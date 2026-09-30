import hashlib
import asyncio
import json
import re
from urllib.parse import urlparse

import httpx

from . import db


def _fingerprint(document_ids: list[str]) -> str:
    chunks = [chunk for document_id in document_ids for chunk in db.get_chunks(document_id, include_embedding=False)]
    value = "|".join(sorted([*document_ids, *(chunk["id"] for chunk in chunks)]))
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def indexed_content_fingerprint(document_ids: list[str]) -> str:
    return _fingerprint(document_ids)


async def build_entity_graph(knowledge_base: dict) -> dict:
    knowledge_base_id = knowledge_base["id"]
    options = (
        (knowledge_base.get("config") or {})
        .get("graph", {})
        .get("graph_build_config", {})
        .get("extractor_options", {})
    )
    base_url = str(options.get("base_url") or "").strip().rstrip("/")
    model = str(options.get("model") or options.get("model_spec") or "").strip()
    api_key = str(options.get("api_key") or "").strip()
    if not base_url or not model or not api_key:
        raise RuntimeError("图谱抽取 LLM 未配置；请填写 OpenAI-Compatible Base URL、模型和 API Key")
    if urlparse(base_url).scheme not in {"http", "https"}:
        raise ValueError("图谱 LLM Base URL 必须是 http 或 https")

    endpoint = base_url if base_url.endswith("/chat/completions") else f"{base_url}/chat/completions"
    schema = str(options.get("schema") or "").strip()
    system_prompt = (
        "请从地灾知识片段中抽取实体和实体关系，只返回严格 JSON，不要输出解释。"
        '格式：{"entities":[{"text":"实体","label":"类型","attributes":[{"text":"值","label":"属性"}]}],'
        '"relations":[{"source":"实体文本","target":"实体文本","text":"关系说明","label":"关系类型"}]}。'
        "不得补充片段中不存在的事实。"
    )
    if schema:
        system_prompt += f"\n抽取 Schema 约束：\n{schema}"

    documents = [doc for doc in db.list_documents(knowledge_base_id) if doc["status"] == "indexed"]
    if not documents:
        raise ValueError("没有已入库文件可构建知识图谱")

    nodes_by_id: dict[str, dict] = {}
    edges_by_id: dict[str, dict] = {}
    failed_chunks = []
    chunk_items = [
        (document, chunk)
        for document in documents
        for chunk in db.get_chunks(document["id"], include_embedding=False)
    ]
    processed_chunk_count = len(chunk_items)
    try:
        concurrency = max(1, min(int(options.get("concurrency_count") or 5), 32))
    except (TypeError, ValueError):
        raise ValueError("图谱抽取 concurrency_count 必须是整数")
    semaphore = asyncio.Semaphore(concurrency)
    async with httpx.AsyncClient(timeout=180.0) as client:
        async def process_chunk(document: dict, chunk: dict) -> None:
            async with semaphore:
                try:
                    response = await client.post(
                        endpoint,
                        headers={"Authorization": f"Bearer {api_key}"},
                        json={
                            **(options.get("model_params") if isinstance(options.get("model_params"), dict) else {}),
                            "model": model,
                            "messages": [
                                {"role": "system", "content": system_prompt},
                                {"role": "user", "content": chunk["text"]},
                            ],
                        },
                    )
                    response.raise_for_status()
                    content = response.json()["choices"][0]["message"]["content"]
                    content = re.sub(r"^\s*```(?:json)?|```\s*$", "", str(content), flags=re.IGNORECASE).strip()
                    extracted = json.loads(content)
                    if not isinstance(extracted, dict) or not isinstance(extracted.get("entities", []), list) or not isinstance(extracted.get("relations", []), list):
                        raise ValueError("模型图谱响应必须包含 entities 和 relations 数组")
                    entity_map: dict[str, str] = {}
                    chunk_node_id = f"CHUNK-{chunk['id']}"
                    span = next((item for item in chunk.get("source_spans", []) if item.get("page") is not None), {})
                    nodes_by_id[chunk_node_id] = {
                        "id": chunk_node_id,
                        "name": f"{document['file_name']} · {chunk['chunk_index'] + 1}",
                        "type": "Chunk",
                        "document_id": document["id"],
                        "file_name": document["file_name"],
                        "chunk_id": chunk["id"],
                        "page": span.get("page"),
                        "content_preview": chunk["text"][:300],
                        "normalized": {"type": "Chunk"},
                    }
                    raw_entities = list(extracted.get("entities", []))
                    for relation in extracted.get("relations", []):
                        if isinstance(relation, dict):
                            raw_entities.extend(endpoint for endpoint in (relation.get("source"), relation.get("target")) if isinstance(endpoint, dict))
                    for entity in raw_entities:
                        if not isinstance(entity, dict) or not str(entity.get("text") or "").strip():
                            continue
                        name = str(entity["text"]).strip()
                        kind = str(entity.get("label") or "Entity").strip() or "Entity"
                        entity_key = f"{kind.casefold()}:{' '.join(name.casefold().split())}"
                        entity_id = f"ENT-{hashlib.sha256(entity_key.encode('utf-8')).hexdigest()[:16]}"
                        entity_map[name] = entity_id
                        existing = nodes_by_id.get(entity_id, {})
                        attributes = list(existing.get("attributes", []))
                        raw_attributes = entity.get("attributes", [])
                        if isinstance(raw_attributes, list):
                            attributes.extend(item for item in raw_attributes if isinstance(item, dict))
                        nodes_by_id[entity_id] = {
                            **existing,
                            "id": entity_id,
                            "name": name,
                            "text": name,
                            "type": kind,
                            "label": kind,
                            "attributes": attributes,
                            "document_id": document["id"],
                            "file_name": document["file_name"],
                            "normalized": {"type": kind},
                        }
                        mention_id = f"MENTIONS-{chunk['id']}-{entity_id}"
                        edges_by_id[mention_id] = {
                            "id": mention_id,
                            "source": chunk_node_id,
                            "target": entity_id,
                            "type": "MENTIONS",
                            "text": "提及",
                            "document_id": document["id"],
                            "chunk_id": chunk["id"],
                            "normalized": {"type": "MENTIONS"},
                        }
                    for relation in extracted.get("relations", []):
                        if not isinstance(relation, dict):
                            continue
                        source_name = relation.get("source")
                        target_name = relation.get("target")
                        if isinstance(source_name, dict):
                            source_name = source_name.get("text")
                        if isinstance(target_name, dict):
                            target_name = target_name.get("text")
                        source_id = entity_map.get(str(source_name or ""))
                        target_id = entity_map.get(str(target_name or ""))
                        if not source_id or not target_id:
                            continue
                        relation_type = str(relation.get("label") or "RELATED_TO").strip() or "RELATED_TO"
                        edge_key = f"{chunk['id']}:{source_id}:{relation_type}:{target_id}"
                        edge_id = f"REL-{hashlib.sha256(edge_key.encode('utf-8')).hexdigest()[:16]}"
                        edges_by_id[edge_id] = {
                            "id": edge_id,
                            "source": source_id,
                            "target": target_id,
                            "type": relation_type,
                            "text": str(relation.get("text") or relation_type),
                            "document_id": document["id"],
                            "chunk_id": chunk["id"],
                            "normalized": {"type": relation_type},
                        }
                except (httpx.HTTPError, KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
                    failed_chunks.append({"chunk_id": chunk["id"], "document_id": document["id"], "file_name": document["file_name"], "error": str(exc)[:500]})

        for offset in range(0, len(chunk_items), 100):
            await asyncio.gather(*(process_chunk(document, chunk) for document, chunk in chunk_items[offset:offset + 100]))

    fingerprint = _fingerprint([doc["id"] for doc in documents])
    return {
        "knowledge_base_id": knowledge_base_id,
        "source_document_ids": [doc["id"] for doc in documents],
        "source_fingerprint": fingerprint,
        "nodes": list(nodes_by_id.values()),
        "edges": list(edges_by_id.values()),
        "generation": "openai-compatible-llm-entity-relations",
        "generated_from": "indexed chunks; each relationship retains its source chunk id",
        "status": "failed" if failed_chunks else "indexed",
        "failed_chunks": failed_chunks,
        "processed_chunk_count": processed_chunk_count,
    }


async def _chat_completion(client: httpx.AsyncClient, options: dict, messages: list[dict]) -> str:
    base_url = str(options.get("base_url") or "").strip().rstrip("/")
    model = str(options.get("model") or options.get("model_spec") or "").strip()
    api_key = str(options.get("api_key") or "").strip()
    if not base_url or not model or not api_key:
        raise RuntimeError("LLM 未配置；请填写 OpenAI-Compatible Base URL、模型和 API Key")
    endpoint = base_url if base_url.endswith("/chat/completions") else f"{base_url}/chat/completions"
    response = await client.post(
        endpoint,
        headers={"Authorization": f"Bearer {api_key}"},
        json={"model": model, "temperature": 0, "messages": messages},
    )
    response.raise_for_status()
    return str(response.json()["choices"][0]["message"]["content"])


def _parse_json_response(content: str) -> dict:
    cleaned = re.sub(r"^\s*```(?:json)?|```\s*$", "", content, flags=re.IGNORECASE).strip()
    parsed = json.loads(cleaned)
    if not isinstance(parsed, dict):
        raise ValueError("模型响应必须是 JSON 对象")
    return parsed


async def generate_mindmap(knowledge_base: dict, document_ids: list[str] | None = None,
                           user_prompt: str = "", incremental: bool = False) -> dict:
    knowledge_base_id = knowledge_base["id"]
    options = (
        (knowledge_base.get("config") or {})
        .get("graph", {})
        .get("graph_build_config", {})
        .get("extractor_options", {})
    )
    all_documents = [doc for doc in db.list_documents(knowledge_base_id) if doc["status"] == "indexed"]
    if document_ids:
        wanted = set(document_ids)
        documents = [doc for doc in all_documents if doc["id"] in wanted]
        missing_ids = wanted - {doc["id"] for doc in documents}
        if missing_ids:
            raise ValueError(f"所选文件不存在或尚未入库：{len(missing_ids)} 个")
    else:
        documents = all_documents
    if not documents and not incremental:
        raise ValueError("没有已入库文件可生成思维导图")

    saved_views = db.list_knowledge_views(knowledge_base_id, "mindmap")
    saved_payload = saved_views[0]["payload"] if saved_views else None
    if incremental and not saved_payload:
        raise ValueError("知识库没有现有思维导图，请先全量生成")

    document_names = {doc["id"]: doc["file_name"] for doc in all_documents}
    current_ids = [doc["id"] for doc in documents]
    previous_ids = set((saved_payload or {}).get("source_document_ids", []))
    current_id_set = set(current_ids)
    added_ids = [item for item in current_ids if item not in previous_ids]
    removed_ids = previous_ids - current_id_set
    current_fingerprint = _fingerprint(current_ids)
    content_changed = bool(saved_payload and saved_payload.get("source_fingerprint") != current_fingerprint)
    if incremental and saved_payload and not added_ids and not removed_ids and not content_changed:
        return {**saved_payload, "no_ai_needed": True, "no_changes": True}

    existing_mindmap = (saved_payload or {}).get("mindmap")
    if incremental and existing_mindmap and removed_ids:
        previous_names = (saved_payload or {}).get("source_document_names", {})
        removed_names = {previous_names.get(item, "") for item in removed_ids}

        def prune(node: dict, root: bool = False) -> dict | None:
            if not isinstance(node, dict):
                return None
            content = str(node.get("content") or "")
            original_children = node.get("children") or []
            if content in removed_names and not original_children and not root:
                return None
            children = [child for item in original_children if (child := prune(item)) is not None]
            if original_children and not children and not root:
                return None
            return {**node, "children": children}

        existing_mindmap = prune(existing_mindmap, root=True)
    refresh_all = incremental and content_changed and not added_ids and not removed_ids
    generation_docs = [
        doc for doc in documents
        if not incremental or doc["id"] in added_ids or refresh_all
    ]
    source_texts = []
    for document in generation_docs:
        chunks = db.get_chunks(document["id"], include_embedding=False)
        excerpt = "\n".join(item["text"] for item in chunks[:8])[:5000]
        source_texts.append(f"文件：{document['file_name']}\n内容摘录：\n{excerpt}")
    source_text = "\n\n".join(source_texts)

    system_prompt = (
        "你是知识库整理助手。只根据给出的文件名和内容摘录生成 2-4 层思维导图，"
        "只返回严格 JSON 对象，结构为 {\"content\":\"知识库名称\",\"children\":[...] }。"
        "所有节点使用 content 和 children 字段；叶子节点必须是文件名，且每个文件名只出现一次。"
    )
    if incremental and existing_mindmap:
        user_message = (
            f"知识库：{knowledge_base['name']}\n现有思维导图：{json.dumps(existing_mindmap, ensure_ascii=False)}\n"
            f"新增文件内容：\n{source_text}\n用户补充说明：{user_prompt}\n"
            "保留原分类，将新增文件放入最合适位置；返回完整的新思维导图 JSON。"
        )
    else:
        user_message = (
            f"知识库：{knowledge_base['name']}\n文件内容：\n{source_text}\n"
            f"用户补充说明：{user_prompt}\n每个文件名只能在叶子节点出现一次，不得遗漏。"
        )
    if generation_docs:
        async with httpx.AsyncClient(timeout=180.0) as client:
            mindmap = _parse_json_response(await _chat_completion(client, options, [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message},
            ]))
    else:
        mindmap = existing_mindmap or {"content": knowledge_base["name"], "children": []}
    def validate_tree(node: object) -> bool:
        if not isinstance(node, dict) or not str(node.get("content") or "").strip():
            return False
        children = node.get("children", [])
        return isinstance(children, list) and all(validate_tree(child) for child in children)

    if not validate_tree(mindmap):
        raise ValueError("模型思维导图必须使用 content 字符串和 children 数组")

    def leaf_names(node: dict) -> list[str]:
        children = node.get("children") or []
        return [str(node.get("content") or "")] if not children else [name for child in children for name in leaf_names(child)]

    expected_names = [document_names[item] for item in current_ids]
    generated_names = leaf_names(mindmap)
    missing = [name for name in expected_names if name not in generated_names]
    duplicate = sorted({name for name in generated_names if generated_names.count(name) > 1 and name in expected_names})
    if missing or duplicate:
        raise ValueError(f"思维导图文件叶节点校验失败；缺少 {len(missing)} 个，重复 {len(duplicate)} 个")

    return {
        "knowledge_base_id": knowledge_base_id,
        "source_document_ids": current_ids,
        "source_document_names": {doc["id"]: doc["file_name"] for doc in documents},
        "source_fingerprint": current_fingerprint,
        "mindmap": mindmap,
        "generation": "openai-compatible-llm-mindmap",
        "no_ai_needed": False,
        "no_changes": False,
    }
