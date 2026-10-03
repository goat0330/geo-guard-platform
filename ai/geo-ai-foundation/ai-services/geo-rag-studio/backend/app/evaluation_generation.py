"""Local provider adapter for Yuxi's generated retrieval benchmark contract."""

import random

import json_repair

from . import db
from .chat import complete_chat
from .knowledge_features import indexed_content_fingerprint
from .retrieval import retrieve
from .schemas import RetrieveRequest
from .yuxi_port.graph import rank_chunks_by_ppr, seed_subgraph

GRAPH_SEED_DECAY = 0.9
GRAPH_PPR_DAMPING = 0.85
GRAPH_PPR_MAX_NODES = 10000
DEFAULT_GRAPH_EXPAND_TOP_K = 1
MAX_GRAPH_EXPAND_TOP_K = 3


def _benchmark_prompt(context: list[tuple[str, str]]) -> str:
    context_text = "\n\n".join(f"片段ID={chunk_id}\n{text}" for chunk_id, text in context)
    return (
        "你将基于以下上下文生成一个可由上下文准确回答的问题与标准答案。"
        "仅返回一个JSON对象，不要包含其他文字。"
        "键为 query、gold_answer、gold_chunk_ids。gold_chunk_ids 必须是上述上下文片段的ID子集。\n\n"
        "上下文：\n" + context_text + "\n"
    )


def _graph_entities_by_chunk(graph: dict) -> tuple[dict[str, list[str]], set[str]]:
    nodes = graph.get("nodes") or []
    node_by_id = {str(node.get("id")): node for node in nodes if node.get("id")}
    graph_chunk_ids = {
        str(node.get("chunk_id"))
        for node in nodes
        if node.get("type") == "Chunk" and node.get("chunk_id")
    }
    entities_by_chunk: dict[str, list[str]] = {}
    for edge in graph.get("edges") or []:
        if edge.get("type") != "MENTIONS":
            continue
        source_id = str(edge.get("source") or "")
        target_id = str(edge.get("target") or "")
        source_node = node_by_id.get(source_id) or {}
        target_node = node_by_id.get(target_id) or {}
        if source_node.get("type") == "Chunk" and target_node.get("type") != "Chunk":
            chunk_id = str(source_node.get("chunk_id") or edge.get("chunk_id") or "")
            entity_id = target_id
        elif target_node.get("type") == "Chunk" and source_node.get("type") != "Chunk":
            chunk_id = str(target_node.get("chunk_id") or edge.get("chunk_id") or "")
            entity_id = source_id
        else:
            continue
        if chunk_id and entity_id:
            values = entities_by_chunk.setdefault(chunk_id, [])
            if entity_id not in values:
                values.append(entity_id)
    return entities_by_chunk, graph_chunk_ids


def _current_knowledge_graph(knowledge_base_id: str) -> dict:
    view = db.get_knowledge_view(knowledge_base_id, "graph")
    if not view:
        raise ValueError("图增强评估集生成需要先构建知识图谱")
    graph = view["payload"]
    document_ids = sorted(
        item["id"] for item in db.list_documents(knowledge_base_id) if item.get("status") == "indexed"
    )
    if (
        sorted(str(item) for item in graph.get("source_document_ids", [])) != document_ids
        or graph.get("source_fingerprint") != indexed_content_fingerprint(document_ids)
    ):
        raise ValueError("知识图谱已过期，请重新构建后再生成图增强评估集")
    return graph


def _select_graph_context(
    *,
    chunks: list[dict],
    graph: dict,
    context_count: int,
    graph_expand_top_k: int,
) -> list[dict] | None:
    entities_by_chunk, graph_chunk_ids = _graph_entities_by_chunk(graph)
    chunks_by_id = {str(chunk["id"]): chunk for chunk in chunks}
    graph_anchor_chunks = [
        chunk
        for chunk in chunks
        if str(chunk["id"]) in graph_chunk_ids
        and entities_by_chunk.get(str(chunk["id"]))
        and chunk.get("content")
    ]
    if not graph_anchor_chunks:
        raise ValueError("No graph indexed chunks with entities found in knowledge base")

    anchor = random.choice(graph_anchor_chunks)
    if context_count <= 1:
        return [anchor]

    anchor_id = str(anchor["id"])
    anchor_entity_ids = entities_by_chunk[anchor_id]
    selected = [anchor]
    selected_ids = {anchor_id}
    seed_weights = {entity_id: 1.0 for entity_id in anchor_entity_ids}
    round_index = 1
    while len(selected) < context_count:
        for entity_id in anchor_entity_ids:
            seed_weights[entity_id] = 1.0
        subgraph = seed_subgraph(graph, seed_weights, GRAPH_PPR_MAX_NODES)
        ranked_chunks = rank_chunks_by_ppr(
            subgraph,
            seed_weights,
            top_k=max(context_count * 5, 20),
            damping=GRAPH_PPR_DAMPING,
        )
        if not ranked_chunks:
            return None

        new_chunks = []
        for chunk_id, _ in ranked_chunks:
            chunk_id = str(chunk_id)
            if chunk_id in selected_ids or chunk_id not in chunks_by_id:
                continue
            new_chunks.append(chunks_by_id[chunk_id])
            if len(new_chunks) >= min(graph_expand_top_k, context_count - len(selected)):
                break
        if not new_chunks:
            return None

        new_weight = GRAPH_SEED_DECAY**round_index
        for chunk in new_chunks:
            chunk_id = str(chunk["id"])
            selected.append(chunk)
            selected_ids.add(chunk_id)
            for entity_id in entities_by_chunk.get(chunk_id, []):
                seed_weights[entity_id] = max(seed_weights.get(entity_id, 0.0), new_weight)
        round_index += 1
    return selected


async def generate_benchmark_case(
    *,
    knowledge_base_id: str,
    chunks: list[dict],
    model_spec: str,
    neighbors_count: int,
    generation_mode: str,
    graph_expand_top_k: int = DEFAULT_GRAPH_EXPAND_TOP_K,
) -> dict | None:
    if generation_mode not in {"vector", "graph_enhanced"}:
        raise ValueError("Unsupported benchmark generation mode")
    if not chunks:
        raise ValueError("知识库为空或未解析到 chunks")

    # Yuxi treats neighbors_count as the total context size, including the anchor.
    context_count = max(1, min(int(neighbors_count), 10))
    if generation_mode == "graph_enhanced":
        graph = _current_knowledge_graph(knowledge_base_id)
        context = _select_graph_context(
            chunks=chunks,
            graph=graph,
            context_count=context_count,
            graph_expand_top_k=min(max(int(graph_expand_top_k), 1), MAX_GRAPH_EXPAND_TOP_K),
        )
        if context is None:
            return None
    else:
        anchor = random.choice(chunks)
        context = [anchor]
        if context_count > 1:
            response = await retrieve(
                RetrieveRequest(
                    query=anchor["content"],
                    search_mode="vector",
                    final_top_k=context_count + 2,
                    use_reranker=False,
                    similarity_threshold=0.0,
                    filters={"knowledge_base_id": knowledge_base_id},
                )
            )
            by_id = {str(item["id"]): item for item in chunks}
            for evidence in response.evidences:
                neighbor = by_id.get(str(evidence.chunk_id))
                if neighbor and neighbor["id"] != anchor["id"]:
                    context.append(neighbor)
                if len(context) >= context_count:
                    break

    allowed_ids = {str(item["id"]) for item in context}
    prompt = _benchmark_prompt([(str(item["id"]), item["content"]) for item in context])
    response = await complete_chat([{"role": "user", "content": prompt}], model_spec)
    try:
        value = json_repair.loads(response)
    except Exception as exc:
        raise ValueError(f"评估题生成响应不是有效 JSON：{exc}") from exc
    if not isinstance(value, dict):
        return None
    query = value.get("query")
    answer = value.get("gold_answer")
    gold_chunk_ids = value.get("gold_chunk_ids")
    if not isinstance(query, str) or not query.strip() or not isinstance(answer, str) or not answer.strip():
        return None
    if not isinstance(gold_chunk_ids, list):
        return None
    gold_chunk_ids = list(dict.fromkeys(str(item) for item in gold_chunk_ids if str(item) in allowed_ids))
    if not gold_chunk_ids:
        return None
    return {"query": query.strip(), "gold_chunk_ids": gold_chunk_ids, "gold_answer": answer.strip()}
