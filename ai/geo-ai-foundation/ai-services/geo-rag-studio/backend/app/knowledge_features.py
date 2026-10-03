import hashlib
import asyncio
import json
import re
from pathlib import Path
from urllib.parse import urlparse

import httpx

from .chat import complete_chat
from .embedding import embed_batched, enabled as embedding_enabled

from . import db
from .yuxi_port import _upstream  # noqa: F401
from yuxi.knowledge.utils.mindmap_utils import (
    MINDMAP_GENERATION_FILE_LIMIT,
    MINDMAP_FILE_PAGE_SIZE,
    MINDMAP_SYSTEM_PROMPT,
    MINDMAP_INCREMENTAL_SYSTEM_PROMPT,
    build_mindmap_user_message,
    build_mindmap_incremental_user_message,
    collect_mindmap_files,
    detect_mindmap_changes,
    parse_mindmap_content,
    remove_files_from_mindmap,
)


def _fingerprint(document_ids: list[str]) -> str:
    chunks = [chunk for document_id in document_ids for chunk in db.get_chunks(document_id, include_embedding=False)]
    value = "|".join(sorted([*document_ids, *(chunk["id"] for chunk in chunks)]))
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def indexed_content_fingerprint(document_ids: list[str]) -> str:
    return _fingerprint(document_ids)


def mindmap_file_map(documents: list[dict]) -> dict:
    return {
        doc["id"]: {"filename": doc["file_name"], "type": Path(doc["file_name"]).suffix.lstrip(".").lower()}
        for doc in documents
    }


def mindmap_document_page(documents: list[dict], tracked_ids: set[str] | None = None) -> tuple[list[dict], int]:
    current = list(documents[:MINDMAP_FILE_PAGE_SIZE])
    included = {document["id"] for document in current}
    current.extend(
        document for document in documents[MINDMAP_FILE_PAGE_SIZE:]
        if document["id"] in (tracked_ids or set()) - included
    )
    return current, len(documents)


def remove_document_from_mindmaps(document: dict) -> None:
    """Prune stored trees using Yuxi's helper; preserve prior content staleness."""
    knowledge_base_id = document["knowledge_base_id"]
    for view in db.list_knowledge_views(knowledge_base_id, "mindmap"):
        payload = view["payload"]
        tree = payload.get("mindmap")
        if not tree:
            continue
        ids = payload.get("source_document_ids", view["source_id"].split(",") if view["source_id"] else [])
        names = payload.get("source_document_names") or {}
        if not ids and not names:
            current_files = mindmap_file_map(db.list_documents(knowledge_base_id))
            ids = detect_mindmap_changes(tree, None, current_files)["tracked_files"]
            names = {item: current_files[item]["filename"] for item in ids}
        if document["id"] not in ids:
            continue
        removed_name = names.get(document["id"], document["file_name"])
        remaining = [item for item in ids if item != document["id"]]
        updated = {
            **payload,
            "mindmap": remove_files_from_mindmap(tree, {removed_name}),
            "source_document_ids": remaining,
            "source_document_names": {key: value for key, value in names.items() if key != document["id"]},
        }
        if payload.get("source_fingerprint") == _fingerprint(ids):
            updated["source_fingerprint"] = _fingerprint(remaining)
        db.save_knowledge_view(knowledge_base_id, "mindmap", view["source_id"], updated)


async def index_knowledge_graph_vectors(knowledge_base: dict, graph: dict) -> dict:
    """Index entity and relation vectors in local SQLite, matching Yuxi's graph-vector records."""
    knowledge_base_id = knowledge_base["id"]
    embedding_config = (knowledge_base.get("config") or {}).get("embedding") or {}
    model = str(embedding_config.get("model") or "")
    fingerprint = str(graph.get("source_fingerprint") or "")
    if not embedding_enabled(embedding_config):
        db.replace_knowledge_graph_embeddings(knowledge_base_id, fingerprint, model, [])
        return {"status": "unavailable", "model": model or None, "record_count": 0,
                "error": "Embedding provider 未配置，图结构已保存但图向量检索不可用"}

    nodes = graph.get("nodes") or []
    node_by_id = {str(node.get("id")): node for node in nodes if node.get("id")}
    records = []
    for node in nodes:
        name = " ".join(str(node.get("name") or node.get("text") or "").strip().lower().split())
        if node.get("type") != "Chunk" and name:
            records.append({"record_type": "entity", "record_id": str(node["id"]), "content": name})
    for edge in graph.get("edges") or []:
        if edge.get("type") == "MENTIONS":
            continue
        source = node_by_id.get(str(edge.get("source") or ""), {})
        target = node_by_id.get(str(edge.get("target") or ""), {})
        source_name = " ".join(str(source.get("name") or source.get("text") or "").strip().lower().split())
        target_name = " ".join(str(target.get("name") or target.get("text") or "").strip().lower().split())
        if source_name and target_name and edge.get("id"):
            relation_type = str(edge.get("type") or "RELATED_TO")
            records.append({
                "record_type": "triple",
                "record_id": str(edge["id"]),
                "content": f"{source_name} → {relation_type} → {target_name}",
            })

    if not records:
        db.replace_knowledge_graph_embeddings(knowledge_base_id, fingerprint, model, [])
        return {"status": "indexed", "model": model, "record_count": 0}
    try:
        vectors = await embed_batched([record["content"] for record in records], embedding_config)
    except (httpx.HTTPError, ValueError) as exc:
        db.replace_knowledge_graph_embeddings(knowledge_base_id, fingerprint, model, [])
        return {"status": "error", "model": model, "record_count": 0,
                "error": f"Embedding provider 请求失败：{type(exc).__name__}"}
    if vectors is None or len(vectors) != len(records):
        db.replace_knowledge_graph_embeddings(knowledge_base_id, fingerprint, model, [])
        return {"status": "error", "model": model, "record_count": 0,
                "error": "Embedding provider 未返回与实体/三元组数量对齐的向量"}
    indexed = [{**record, "embedding": vector} for record, vector in zip(records, vectors, strict=True)]
    count = db.replace_knowledge_graph_embeddings(knowledge_base_id, fingerprint, model, indexed)
    return {"status": "indexed", "model": model, "record_count": count}


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
    if not api_key and ":" in model:
        return await complete_chat(messages, model, timeout=180.0)
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


async def generate_mindmap(knowledge_base: dict, document_ids: list[str] | None = None,
                           user_prompt: str = "", incremental: bool = False) -> dict:
    knowledge_base_id = knowledge_base["id"]
    options = (
        (knowledge_base.get("config") or {})
        .get("graph", {})
        .get("graph_build_config", {})
        .get("extractor_options", {})
    )
    all_documents = db.list_documents(knowledge_base_id)
    saved_views = db.list_knowledge_views(knowledge_base_id, "mindmap")
    saved_payload = saved_views[0]["payload"] if saved_views else None
    original_count = len(document_ids) if document_ids and not incremental else len(all_documents)
    if document_ids and not incremental:
        selected_ids = document_ids[:MINDMAP_GENERATION_FILE_LIMIT]
        wanted = set(selected_ids)
        documents = [doc for doc in all_documents if doc["id"] in wanted]
        missing_ids = wanted - {doc["id"] for doc in documents}
        if missing_ids:
            raise ValueError(f"所选文件不存在：{len(missing_ids)} 个")
    else:
        if incremental:
            tracked_ids = set((saved_payload or {}).get("source_document_ids", []))
            if not tracked_ids and saved_payload:
                tracked_ids = set(detect_mindmap_changes(
                    saved_payload.get("mindmap"), None, mindmap_file_map(all_documents)
                )["tracked_files"])
            documents, _ = mindmap_document_page(all_documents, tracked_ids)
        else:
            documents = all_documents[:MINDMAP_GENERATION_FILE_LIMIT]
    if not documents and not incremental:
        raise ValueError("知识库中没有文件")

    if incremental and not saved_payload:
        raise ValueError("知识库没有现有思维导图，请先全量生成")

    document_names = {doc["id"]: doc["file_name"] for doc in all_documents}
    current_ids = [doc["id"] for doc in documents]
    current_files = mindmap_file_map(documents)
    previous_names = (saved_payload or {}).get("source_document_names") or {
        item: document_names.get(item, "") for item in (saved_payload or {}).get("source_document_ids", [])
    }
    changes = detect_mindmap_changes((saved_payload or {}).get("mindmap"), previous_names, current_files)
    previous_ids = set(changes["tracked_files"])
    current_id_set = set(current_ids)
    added_ids = [item for item in current_ids if item not in previous_ids]
    removed_ids = previous_ids - current_id_set
    current_fingerprint = _fingerprint(current_ids)
    if incremental and saved_payload and not changes["needs_update"]:
        return {**saved_payload, "no_ai_needed": True, "no_changes": True}

    existing_mindmap = (saved_payload or {}).get("mindmap")
    if incremental and existing_mindmap and removed_ids:
        removed_names = {previous_names.get(item, "") for item in removed_ids}

        existing_mindmap = remove_files_from_mindmap(existing_mindmap, removed_names)
    generation_docs = [
        doc for doc in documents
        if not incremental or doc["id"] in added_ids
    ]
    files_info = collect_mindmap_files(current_files, [doc["id"] for doc in generation_docs])
    if incremental and existing_mindmap:
        system_prompt = MINDMAP_INCREMENTAL_SYSTEM_PROMPT
        user_message = build_mindmap_incremental_user_message(knowledge_base["name"], existing_mindmap, files_info, user_prompt)
    else:
        system_prompt = MINDMAP_SYSTEM_PROMPT
        user_message = build_mindmap_user_message(knowledge_base["name"], files_info, user_prompt)
    if generation_docs:
        async with httpx.AsyncClient(timeout=180.0) as client:
            mindmap = parse_mindmap_content(await _chat_completion(client, options, [
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
        "file_count": len(documents),
        "original_file_count": original_count,
        "truncated": len(documents) < original_count,
        "no_ai_needed": not generation_docs,
        "no_changes": False,
    }
