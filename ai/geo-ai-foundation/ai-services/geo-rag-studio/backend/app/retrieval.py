import math
import re
import time
from collections import Counter

from . import db
from .config import settings
from .embedding import embed_texts, enabled as embedding_enabled
from .knowledge_features import indexed_content_fingerprint as _source_fingerprint
from .schemas import Evidence, RetrieveRequest, RetrieveResponse, SourceSpan
from .yuxi_port.rerank import enabled as rerank_enabled, rerank

TOKEN_RE = re.compile(r"[\w\u4e00-\u9fff]+", re.UNICODE)


class ProviderUnavailable(RuntimeError):
    pass


def tokenize(text: str) -> list[str]:
    tokens = TOKEN_RE.findall((text or "").lower())
    output = []
    for token in tokens:
        if any("\u4e00" <= char <= "\u9fff" for char in token) and len(token) > 1:
            output.extend(token[index : index + 2] for index in range(len(token) - 1))
        output.append(token)
    return output


def bm25_scores(query: str, documents: list[str], k1: float = 1.5, b: float = 0.75, drop_ratio_search: float = 0.0) -> list[float]:
    if not documents:
        return []
    tokenized = [tokenize(text) for text in documents]
    query_tokens = tokenize(query)
    count = len(documents)
    average_length = sum(map(len, tokenized)) / max(count, 1)
    document_frequency = Counter(term for terms in tokenized for term in set(terms))
    query_terms = list(dict.fromkeys(query_tokens))
    drop_count = min(len(query_terms), int(len(query_terms) * max(0.0, min(1.0, drop_ratio_search))))
    if drop_count:
        query_terms.sort(key=lambda term: math.log(1 + (count - document_frequency[term] + 0.5) / (document_frequency[term] + 0.5)))
        query_terms = query_terms[drop_count:]
        query_tokens = [term for term in query_tokens if term in set(query_terms)]
    scores = []
    for terms in tokenized:
        term_frequency = Counter(terms)
        length = len(terms)
        score = 0.0
        for term in query_tokens:
            frequency = term_frequency.get(term, 0)
            if not frequency:
                continue
            inverse_frequency = math.log(1 + (count - document_frequency[term] + 0.5) / (document_frequency[term] + 0.5))
            denominator = frequency + k1 * (1 - b + b * length / max(average_length, 1e-9))
            score += inverse_frequency * (frequency * (k1 + 1)) / denominator
        scores.append(score)
    return scores


def cosine(left: list[float] | None, right: list[float] | None) -> float | None:
    if not left or not right:
        return None
    if len(left) != len(right):
        raise ValueError("Stored and query embedding dimensions differ")
    dot = sum(x * y for x, y in zip(left, right, strict=True))
    left_norm = math.sqrt(sum(x * x for x in left))
    right_norm = math.sqrt(sum(y * y for y in right))
    return dot / (left_norm * right_norm) if left_norm and right_norm else 0.0


def _normalize(values: list[float | None]) -> list[float | None]:
    present = [value for value in values if value is not None]
    if not present:
        return values
    low, high = min(present), max(present)
    if high - low < 1e-12:
        return [1.0 if value is not None else None for value in values]
    return [((value - low) / (high - low)) if value is not None else None for value in values]


def _stage_row(chunk: dict, score: float, rank: int, stage_score: str) -> dict:
    spans = chunk.get("source_spans", [])
    primary_span = next((span for span in spans if span.get("page") is not None), {})
    bbox_span = next((span for span in spans if span.get("bbox")), primary_span)
    return {
        "rank": rank,
        "evidence_id": f"EV-{chunk['id']}",
        "chunk_id": chunk["id"],
        "document_id": chunk["document_id"],
        "file_name": chunk.get("file_name"),
        "page": primary_span.get("page"),
        "bbox": bbox_span.get("bbox"),
        "text": chunk["text"],
        "score": round(score, 8),
        "score_type": stage_score,
    }


def _filtered_chunks(filters: dict) -> list[dict]:
    chunks = [chunk for chunk in db.get_chunks() if chunk.get("document_status") == "indexed"]
    document_ids = filters.get("document_ids")
    if isinstance(document_ids, list):
        keep = {str(value) for value in document_ids}
        chunks = [chunk for chunk in chunks if chunk["document_id"] in keep]
    document_id = filters.get("document_id")
    if document_id:
        chunks = [chunk for chunk in chunks if chunk["document_id"] == str(document_id)]
    knowledge_base_id = filters.get("knowledge_base_id")
    if knowledge_base_id:
        chunks = [chunk for chunk in chunks if chunk.get("knowledge_base_id") == str(knowledge_base_id)]
    file_name = filters.get("file_name")
    if file_name:
        chunks = [chunk for chunk in chunks if chunk.get("file_name") == file_name]
    return chunks


def _rank(chunks: list[dict], scores: list[float | None], top_k: int, label: str) -> list[dict]:
    pairs = [(index, score) for index, score in enumerate(scores) if score is not None]
    pairs.sort(key=lambda item: item[1], reverse=True)
    if label == "bm25":
        pairs = [pair for pair in pairs if pair[1] > 0]
    return [
        {**chunks[index], label: score, "rank": rank}
        for rank, (index, score) in enumerate(pairs[:top_k], start=1)
    ]


async def _retrieve(req: RetrieveRequest) -> tuple[RetrieveResponse, dict]:
    started = time.perf_counter()
    query = req.query.strip()
    if not query:
        raise ValueError("query must not be blank")

    knowledge_base = db.get_knowledge_base(str(req.filters.get("knowledge_base_id"))) if req.filters.get("knowledge_base_id") else None
    kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
    retrieval_config = kb_config.get("retrieval", {})
    embedding_config = kb_config.get("embedding", {})
    reranker_config = kb_config.get("reranker", {})
    search_mode = req.search_mode or retrieval_config.get("search_mode", "hybrid")
    use_reranker = req.use_reranker if req.use_reranker is not None else bool(retrieval_config.get("use_reranker", False))
    final_top_k = req.final_top_k or req.top_k or retrieval_config.get("final_top_k") or settings.default_final_top_k
    requested_recall = req.recall_top_k or retrieval_config.get("recall_top_k")
    recall_top_k = max(final_top_k, requested_recall or (settings.default_recall_top_k if use_reranker else final_top_k))
    bm25_top_k = max(final_top_k, req.bm25_top_k or retrieval_config.get("bm25_top_k", 50))
    bm25_drop_ratio_search = req.bm25_drop_ratio_search if req.bm25_drop_ratio_search is not None else retrieval_config.get("bm25_drop_ratio_search", 0.0)
    similarity_threshold = req.similarity_threshold if req.similarity_threshold is not None else retrieval_config.get("similarity_threshold", 0.0)
    use_graph = req.use_graph_retrieval if req.use_graph_retrieval is not None else bool(retrieval_config.get("use_graph_retrieval", False))
    graph_weight = float(retrieval_config.get("graph_weight", 1.0))
    if use_reranker and not rerank_enabled(reranker_config):
        raise ProviderUnavailable("Reranker requested but RERANK_BASE_URL, RERANK_API_KEY, and RERANK_MODEL are not configured")
    if search_mode == "vector" and not embedding_enabled(embedding_config):
        raise ProviderUnavailable("Vector retrieval requires an Embedding API configuration")

    chunks = _filtered_chunks(req.filters)
    if not chunks:
        empty = RetrieveResponse(
            query=query,
            evidences=[],
            retrieval={"mode": "empty", "search_mode": search_mode, "candidate_count": 0, "recall_top_k": recall_top_k, "final_top_k": final_top_k, "use_reranker": use_reranker},
            timing_ms={"total": round((time.perf_counter() - started) * 1000, 2)},
            usage={"embedding_enabled": embedding_enabled(embedding_config), "reranker_enabled": rerank_enabled(reranker_config), "context_tokens": 0},
            final_context="",
        )
        return empty, {"bm25": [], "vector": [], "graph": [], "fusion": [], "rerank": [], "final": []}

    docs = [chunk["text"] for chunk in chunks]
    times = {}
    stages: dict[str, list[dict]] = {"bm25": [], "vector": [], "graph": [], "fusion": [], "rerank": []}
    has_stored_vectors = any(chunk.get("embedding") for chunk in chunks)
    if search_mode == "vector" and not has_stored_vectors:
        raise ProviderUnavailable("No indexed document embeddings are available; ingest documents with Embedding first")

    mark = time.perf_counter()
    bm25_raw = bm25_scores(query, docs, drop_ratio_search=bm25_drop_ratio_search) if search_mode != "vector" else [None] * len(chunks)
    bm25_top = _rank(chunks, bm25_raw, bm25_top_k, "bm25")
    times["bm25"] = round((time.perf_counter() - mark) * 1000, 2)
    stages["bm25"] = [_stage_row(item, item["bm25"], rank, "bm25_raw") for rank, item in enumerate(bm25_top, 1)]

    mark = time.perf_counter()
    vector_raw: list[float | None] = [None] * len(chunks)
    query_vector = None
    vector_skip_reason = None
    if search_mode != "keyword" and embedding_enabled(embedding_config) and has_stored_vectors:
        query_vectors = await embed_texts([query], embedding_config)
        if query_vectors:
            query_vector = query_vectors[0]
            active_embedding_model = (embedding_config or {}).get("model") or settings.embedding_model
            for index, chunk in enumerate(chunks):
                if chunk.get("embedding"):
                    if chunk.get("embedding_model") != active_embedding_model:
                        raise ProviderUnavailable("Stored vectors use a different embedding model; re-index these documents first")
                    vector_raw[index] = cosine(query_vector, chunk["embedding"])
        else:
            vector_skip_reason = "embedding_api_returned_no_vector"
    elif search_mode != "keyword":
        vector_skip_reason = "no_indexed_vectors" if embedding_enabled(embedding_config) else "embedding_not_configured"
    vector_top = _rank(chunks, vector_raw, recall_top_k, "vector")
    times["vector"] = round((time.perf_counter() - mark) * 1000, 2)
    stages["vector"] = [_stage_row(item, item["vector"], rank, "cosine_similarity") for rank, item in enumerate(vector_top, 1)]

    mark = time.perf_counter()
    bm25_norm = _normalize(bm25_raw)
    vector_norm = _normalize(vector_raw)
    vector_weight = req.vector_weight if req.vector_weight is not None else retrieval_config.get("vector_weight", settings.vector_weight)
    bm25_weight = req.bm25_weight if req.bm25_weight is not None else retrieval_config.get("bm25_weight", settings.bm25_weight)
    if vector_weight + bm25_weight <= 0:
        raise ValueError("vector_weight and bm25_weight cannot both be zero")
    graph_raw: list[float | None] = [None] * len(chunks)
    graph_top = []
    graph_status = "skipped"
    if use_graph:
        graph_view = db.get_knowledge_view(str(req.filters.get("knowledge_base_id") or ""), "graph")
        if graph_view:
            graph = graph_view["payload"]
            current_document_ids = sorted({chunk["document_id"] for chunk in chunks})
            graph_is_current = (
                sorted(graph.get("source_document_ids", [])) == current_document_ids
                and graph.get("source_fingerprint") == _source_fingerprint(current_document_ids)
            )
            if graph_is_current:
                query_folded = query.casefold()
                matching_entities = {
                    node["id"] for node in graph.get("nodes", [])
                    if node.get("type") != "Chunk"
                    and str(node.get("name") or node.get("text") or "").strip()
                    and str(node.get("name") or node.get("text") or "").casefold() in query_folded
                }
                graph_chunk_hits = Counter()
                for edge in graph.get("edges", []):
                    if edge.get("type") == "MENTIONS" and edge.get("target") in matching_entities:
                        graph_chunk_hits[str(edge.get("chunk_id") or "")] += 1
                    elif (edge.get("source") in matching_entities or edge.get("target") in matching_entities) and edge.get("chunk_id"):
                        graph_chunk_hits[str(edge["chunk_id"])] += 1
                for index, chunk in enumerate(chunks):
                    score = float(graph_chunk_hits.get(chunk["id"], 0))
                    if score:
                        graph_raw[index] = score
                graph_top = _rank(chunks, graph_raw, recall_top_k, "graph")
                graph_status = "completed"
            else:
                graph_status = "outdated"
        else:
            graph_status = "not_built"
    graph_norm = _normalize(graph_raw)
    stages["graph"] = [_stage_row(item, item["graph"], rank, "entity_graph_match_count") for rank, item in enumerate(graph_top, 1)]
    candidates = {}
    for item in bm25_top + vector_top + graph_top:
        candidates[item["id"]] = {**candidates.get(item["id"], {}), **item}
    index_by_id = {chunk["id"]: index for index, chunk in enumerate(chunks)}
    fused = []
    for item in candidates.values():
        index = index_by_id[item["id"]]
        active = []
        if search_mode == "keyword" and item.get("bm25") is not None:
            active.append((1.0, bm25_norm[index] or 0.0))
        elif search_mode == "vector" and item.get("vector") is not None:
            active.append((1.0, vector_norm[index] or 0.0))
        elif search_mode == "hybrid" and item.get("bm25") is not None:
            active.append((bm25_weight, bm25_norm[index] or 0.0))
        if search_mode == "hybrid" and item.get("vector") is not None:
            active.append((vector_weight, vector_norm[index] or 0.0))
        if use_graph and item.get("graph") is not None:
            active.append((graph_weight, graph_norm[index] or 0.0))
        total = sum(weight for weight, _ in active)
        if total:
            score = sum(weight * value for weight, value in active) / total
            fused.append((item, score))
    fused.sort(key=lambda item: item[1], reverse=True)
    fusion_all = [{**item, "fusion": score} for item, score in fused]
    fusion_all = [item for item in fusion_all if item["fusion"] >= similarity_threshold]
    fusion_top = fusion_all[:recall_top_k]
    times["fusion"] = round((time.perf_counter() - mark) * 1000, 2)
    stages["fusion"] = [_stage_row(item, item["fusion"], rank, "weighted_normalized_score") for rank, item in enumerate(fusion_top, 1)]

    mark = time.perf_counter()
    rerank_status = "skipped"
    rerank_error = None
    final_candidates = fusion_top
    if use_reranker and fusion_top:
        scores = await rerank(query, [item["text"] for item in fusion_top], reranker_config)
        if scores is None or len(scores) != len(fusion_top):
            raise ValueError("Rerank score count does not match the candidate count")
        final_candidates = [
            {**item, "rerank": score}
            for item, score in zip(fusion_top, scores, strict=True)
        ]
        final_candidates.sort(key=lambda item: item["rerank"], reverse=True)
        rerank_status = "completed"
        stages["rerank"] = [_stage_row(item, item["rerank"], rank, "provider_relevance_score") for rank, item in enumerate(final_candidates, 1)]
    times["rerank"] = round((time.perf_counter() - mark) * 1000, 2)

    selected = final_candidates[:final_top_k]
    evidences = []
    for rank, chunk in enumerate(selected, start=1):
        spans = [SourceSpan.model_validate(span) for span in chunk.get("source_spans", [])]
        primary_span = next((span for span in spans if span.page is not None), None)
        bbox_span = next((span for span in spans if span.bbox), primary_span)
        primary_page = primary_span.page if primary_span else None
        scores = {
            "bm25": round(chunk["bm25"], 8) if chunk.get("bm25") is not None else None,
            "vector": round(chunk["vector"], 8) if chunk.get("vector") is not None else None,
            "fusion": round(chunk["fusion"], 8) if chunk.get("fusion") is not None else None,
            "rerank": round(chunk["rerank"], 8) if chunk.get("rerank") is not None else None,
        }
        evidences.append(
            Evidence(
                evidence_id=f"EV-{chunk['id']}",
                document_id=chunk["document_id"],
                chunk_id=chunk["id"],
                file_name=chunk.get("file_name") or "",
                page=primary_page,
                text=chunk["text"],
                token_count=chunk["token_count"],
                bbox=bbox_span.bbox if bbox_span else None,
                bm25_score=scores["bm25"],
                vector_score=scores["vector"],
                fusion_score=scores["fusion"],
                rerank_score=scores["rerank"],
                source_spans=spans,
                scores=scores,
                metadata={**chunk.get("metadata", {}), "rank": rank},
            )
        )
    times["total"] = round((time.perf_counter() - started) * 1000, 2)
    mode = "hybrid" if search_mode == "hybrid" and query_vector is not None and has_stored_vectors else ("bm25-only" if search_mode == "hybrid" else search_mode)
    final_context = "\n\n".join(
        f"[{item.evidence_id}] {item.file_name}{f' 第{item.page}页' if item.page else ''}\n{item.text}"
        for item in evidences
    )
    response = RetrieveResponse(
        query=query,
        evidences=evidences,
        retrieval={
            "mode": mode,
            "search_mode": search_mode,
            "candidate_count": len(chunks),
            "recall_top_k": recall_top_k,
            "final_top_k": final_top_k,
            "use_reranker": use_reranker,
            "reranker": (reranker_config or {}).get("model") or settings.rerank_model or None,
            "rerank_status": rerank_status,
            "graph_status": graph_status,
            "use_graph_retrieval": use_graph,
            "vector_skip_reason": vector_skip_reason,
            "vector_weight": vector_weight,
            "bm25_weight": bm25_weight,
            "bm25_top_k": bm25_top_k,
            "bm25_drop_ratio_search": bm25_drop_ratio_search,
            "similarity_threshold": similarity_threshold,
            "rerank_error": rerank_error,
        },
        timing_ms=times,
        usage={
            "embedding_enabled": embedding_enabled(embedding_config),
            "reranker_enabled": rerank_enabled(reranker_config),
            "context_tokens": sum(item.token_count for item in evidences),
        },
        final_context=final_context,
    )
    return response, stages


async def retrieve(req: RetrieveRequest) -> RetrieveResponse:
    response, _ = await _retrieve(req)
    return response


async def retrieve_debug(req: RetrieveRequest) -> dict:
    response, stages = await _retrieve(req)
    search_mode = response.retrieval.get("search_mode", "hybrid")
    use_reranker = response.retrieval.get("use_reranker", False)
    vector_status = "skipped" if response.retrieval.get("vector_skip_reason") or search_mode == "keyword" else "completed"
    rerank_status = response.retrieval.get("rerank_status", "skipped")
    pipeline = [
        {"stage": "query", "status": "completed", "summary": "Query 已规范化"},
        {"stage": "bm25", "status": "skipped" if search_mode == "vector" else "completed", "summary": f"BM25 返回 {len(stages['bm25'])} 条候选"},
        {"stage": "vector", "status": vector_status, "summary": response.retrieval.get("vector_skip_reason") or f"Vector 返回 {len(stages['vector'])} 条候选"},
        {"stage": "graph", "status": response.retrieval.get("graph_status", "skipped"), "summary": f"实体图检索返回 {len(stages['graph'])} 条候选" if response.retrieval.get("graph_status") != "not_built" else "图检索已开启但尚未构建实体关系图谱"},
        {"stage": "fusion", "status": "completed", "summary": f"Fusion 保留 {len(stages['fusion'])} 条候选"},
        {"stage": "rerank", "status": rerank_status, "summary": "Rerank 已执行" if rerank_status == "completed" else ("Rerank 未产生候选" if use_reranker else "未请求 Rerank")},
        {"stage": "evidence", "status": "completed", "summary": f"已生成 {len(response.evidences)} 条 Evidence"},
        {"stage": "final_context", "status": "completed", "summary": f"Final Context 约 {response.usage['context_tokens']} tokens"},
    ]
    knowledge_base = db.get_knowledge_base(str(req.filters.get("knowledge_base_id"))) if req.filters.get("knowledge_base_id") else None
    kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
    model_config = {}
    for section in ("embedding", "reranker"):
        values = dict(kb_config.get(section, {}))
        secret = values.pop("api_key", None)
        global_key = settings.embedding_api_key if section == "embedding" else settings.rerank_api_key
        values["api_key_set"] = bool(secret or global_key)
        values["enabled"] = embedding_enabled(kb_config.get(section, {})) if section == "embedding" else rerank_enabled(kb_config.get(section, {}))
        if section == "embedding":
            values.setdefault("provider", "openai-compatible")
            values["model"] = values.get("model") or settings.embedding_model or None
        else:
            values["model"] = values.get("model") or settings.rerank_model or None
            values["protocol"] = values.get("protocol") or settings.rerank_protocol
        model_config[section] = values
    config_snapshot = {
        "knowledge_base_id": knowledge_base.get("id") if knowledge_base else None,
        "parser": kb_config.get("parser", {}),
        "chunking": kb_config.get("chunking", {}),
        "retrieval": {
            **kb_config.get("retrieval", {}),
            "search_mode": search_mode,
            "recall_top_k": response.retrieval.get("recall_top_k"),
            "final_top_k": response.retrieval.get("final_top_k"),
            "use_reranker": use_reranker,
            "vector_weight": response.retrieval.get("vector_weight"),
            "bm25_weight": response.retrieval.get("bm25_weight"),
            "bm25_top_k": response.retrieval.get("bm25_top_k"),
            "bm25_drop_ratio_search": response.retrieval.get("bm25_drop_ratio_search"),
            "similarity_threshold": response.retrieval.get("similarity_threshold"),
            "use_graph_retrieval": response.retrieval.get("use_graph_retrieval"),
            "graph_status": response.retrieval.get("graph_status"),
        },
        "models": model_config,
    }
    return {
        "request": req.model_dump(),
        "pipeline": pipeline,
        "stages": stages,
        "bm25_candidates": stages["bm25"],
        "vector_candidates": stages["vector"],
        "graph_candidates": stages["graph"],
        "fusion_candidates": stages["fusion"],
        "rerank_candidates": stages["rerank"],
        "final_evidences": [item.model_dump() for item in response.evidences],
        "timing": response.timing_ms,
        "config_snapshot": config_snapshot,
        "result": response.model_dump(),
        "evidences": [item.model_dump() for item in response.evidences],
        "final_context": response.final_context,
    }
