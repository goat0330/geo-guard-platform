"""Local adapter for Yuxi's graph PPR and graph-result RRF ranking."""

from typing import Any

import networkx as nx


def seed_subgraph(graph_payload: dict[str, Any], seed_weights: dict[str, float], max_nodes: int) -> dict[str, list[dict]]:
    """Build the same two-hop neighborhood used by Yuxi's graph retrieval."""
    nodes = graph_payload.get("nodes") or []
    edges = graph_payload.get("edges") or []
    node_by_id = {str(node.get("id")): node for node in nodes if node.get("id")}
    neighbors: dict[str, set[str]] = {node_id: set() for node_id in node_by_id}
    for edge in edges:
        source, target = str(edge.get("source") or ""), str(edge.get("target") or "")
        if source in node_by_id and target in node_by_id:
            neighbors[source].add(target)
            neighbors[target].add(source)

    selected: list[str] = []
    seen: set[str] = set()
    frontier = [node_id for node_id in seed_weights if node_id in node_by_id]
    for depth in range(3):
        next_frontier: list[str] = []
        for node_id in frontier:
            if node_id in seen:
                continue
            if len(selected) >= max(1, max_nodes):
                break
            seen.add(node_id)
            selected.append(node_id)
            if depth < 2:
                next_frontier.extend(sorted(neighbors[node_id] - seen))
        if len(selected) >= max(1, max_nodes):
            break
        frontier = next_frontier

    selected_ids = set(selected)
    selected_nodes = []
    for node_id in selected:
        node = node_by_id[node_id]
        is_chunk = node.get("type") == "Chunk"
        selected_nodes.append({
            "id": node_id,
            "type": node.get("type"),
            "properties": {
                "entity_id": None if is_chunk else node_id,
                "chunk_id": node.get("chunk_id") if is_chunk else None,
            },
        })
    selected_edges = [
        {"source_id": str(edge.get("source")), "target_id": str(edge.get("target"))}
        for edge in edges
        if str(edge.get("source") or "") in selected_ids and str(edge.get("target") or "") in selected_ids
    ]
    return {"nodes": selected_nodes, "edges": selected_edges}


def rank_chunks_by_ppr(
    subgraph: dict[str, Any], seed_weights: dict[str, float], *, top_k: int, damping: float
) -> list[tuple[str, float]]:
    """Port of Yuxi MilvusGraphService.rank_chunks_by_ppr."""
    nodes = subgraph.get("nodes") or []
    edges = subgraph.get("edges") or []
    if not nodes or top_k <= 0:
        return []

    node_ids = [node["id"] for node in nodes]
    index_by_id = {node_id: index for index, node_id in enumerate(node_ids)}
    edge_indices = [
        (index_by_id[edge["source_id"]], index_by_id[edge["target_id"]])
        for edge in edges
        if edge.get("source_id") in index_by_id and edge.get("target_id") in index_by_id
    ]
    if not edge_indices:
        return []

    graph = nx.Graph()
    graph.add_nodes_from(range(len(nodes)))
    graph.add_edges_from(edge_indices)
    reset = [0.0] * len(nodes)
    chunk_node_indexes: list[tuple[int, str]] = []
    for index, node in enumerate(nodes):
        properties = node.get("properties") or {}
        if node.get("type") == "Chunk" and properties.get("chunk_id"):
            chunk_node_indexes.append((index, properties["chunk_id"]))
            continue
        entity_id = properties.get("entity_id")
        if entity_id in seed_weights:
            reset[index] = seed_weights[entity_id]

    reset_total = sum(reset)
    if reset_total <= 0 or not chunk_node_indexes:
        return []
    reset = [value / reset_total for value in reset]
    personalization = {index: value for index, value in enumerate(reset)}
    scores = nx.pagerank(
        graph,
        alpha=min(max(damping, 0.1), 0.99),
        personalization=personalization,
    )
    ranked = sorted(
        ((chunk_id, float(scores[index])) for index, chunk_id in chunk_node_indexes),
        key=lambda item: item[1],
        reverse=True,
    )
    return ranked[:top_k]


def fuse_chunk_rankings(
    base_chunks: list[dict], graph_chunks: list[dict], graph_weight: float
) -> list[dict]:
    """Port of Yuxi MilvusKB._fuse_chunk_rankings (weighted RRF, k=60)."""
    fused: dict[str, dict] = {}

    def merge(chunk: dict, rank: int, weight: float, source: str) -> None:
        chunk_id = chunk.get("id") or chunk.get("metadata", {}).get("chunk_id")
        if not chunk_id:
            return
        score = weight / (60.0 + rank)
        existing = fused.get(str(chunk_id))
        if existing is None:
            existing = {**chunk, "fusion": 0.0, "fusion_sources": []}
            fused[str(chunk_id)] = existing
        existing["fusion"] += score
        existing["fusion_score"] = existing["fusion"]
        existing["fusion_sources"].append(source)
        if source == "graph" and "graph" in chunk:
            existing["graph"] = chunk["graph"]

    for rank, chunk in enumerate(base_chunks, start=1):
        merge(chunk, rank, 1.0, "chunk")
    for rank, chunk in enumerate(graph_chunks, start=1):
        merge(chunk, rank, max(graph_weight, 0.0), "graph")
    return sorted(fused.values(), key=lambda item: item["fusion"], reverse=True)
