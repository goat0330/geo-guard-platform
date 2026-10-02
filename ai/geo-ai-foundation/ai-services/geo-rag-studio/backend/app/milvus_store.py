"""Geo-owned Milvus index using Yuxi v0.7.3's collection and query contract."""

import asyncio
import hashlib
import json
import threading
import time
from typing import Any

from .config import settings
from .provider_store import resolve_runtime_config

_ALIAS = "geo_rag_milvus"
_CONNECT_LOCK = threading.Lock()
_LOADED: set[str] = set()
_CONTENT_SPARSE_FIELD = "content_sparse"


class MilvusUnavailable(RuntimeError):
    pass


def enabled() -> bool:
    return bool(settings.milvus_uri)


def collection_name(knowledge_base_id: str) -> str:
    digest = hashlib.sha256(knowledge_base_id.encode("utf-8")).hexdigest()[:24]
    return f"{settings.milvus_collection_prefix}_{digest}"


def _connect():
    from pymilvus import connections, db

    if not settings.milvus_uri:
        raise MilvusUnavailable("MILVUS_URI is not configured")
    with _CONNECT_LOCK:
        if not connections.has_connection(_ALIAS):
            connections.connect(
                alias=_ALIAS,
                uri=settings.milvus_uri,
                token=settings.milvus_token,
            )
            databases = db.list_database(using=_ALIAS)
            if settings.milvus_database not in databases:
                db.create_database(settings.milvus_database, using=_ALIAS)
            db.using_database(settings.milvus_database, using=_ALIAS)
    return _ALIAS


def _resolved_embedding(embedding_config: dict | None) -> tuple[str, int | None]:
    config = resolve_runtime_config(embedding_config, "embedding")
    raw_dimension = config.get("dimensions")
    dimension = int(raw_dimension) if raw_dimension not in (None, "") else None
    model = str((embedding_config or {}).get("model") or config.get("model") or "")
    return model, dimension


def _get_or_create_collection(knowledge_base_id: str, embedding_config: dict | None, vectors: list[list[float]]):
    from pymilvus import (
        Collection,
        CollectionSchema,
        DataType,
        FieldSchema,
        Function,
        FunctionType,
        utility,
    )

    alias = _connect()
    name = collection_name(knowledge_base_id)
    model, configured_dimension = _resolved_embedding(embedding_config)
    vector_dimensions = {len(vector) for vector in vectors}
    if len(vector_dimensions) > 1:
        raise ValueError("Embedding vectors have inconsistent dimensions")
    dimension = next(iter(vector_dimensions), configured_dimension)
    if configured_dimension and dimension and configured_dimension != dimension:
        raise ValueError(f"Embedding returned dimension {dimension}; configured dimension is {configured_dimension}")

    if utility.has_collection(name, using=alias):
        collection = Collection(name=name, using=alias)
        schema_fields = {field.name: field for field in collection.schema.fields}
        existing_dimension = (
            int(schema_fields["embedding"].params["dim"]) if "embedding" in schema_fields else None
        )
        if collection.description != _description(knowledge_base_id, model, existing_dimension):
            raise MilvusUnavailable(
                "知识库的 Embedding 模型或维度已变化；请先对该知识库全部文件重新索引"
            )
        if dimension and existing_dimension != dimension:
            raise MilvusUnavailable("Milvus 向量维度与当前 Embedding 不一致；请重新索引整个知识库")
    else:
        fields = [
            FieldSchema(name="id", dtype=DataType.VARCHAR, max_length=100, is_primary=True),
            FieldSchema(
                name="content",
                dtype=DataType.VARCHAR,
                max_length=65535,
                enable_analyzer=True,
                analyzer_params={"type": "chinese"},
            ),
            FieldSchema(name="chunk_id", dtype=DataType.VARCHAR, max_length=100),
            FieldSchema(name="file_id", dtype=DataType.VARCHAR, max_length=100),
            FieldSchema(name="chunk_index", dtype=DataType.INT64),
            FieldSchema(name="has_embedding", dtype=DataType.BOOL),
        ]
        if dimension:
            fields.append(FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=dimension))
        fields.append(FieldSchema(name=_CONTENT_SPARSE_FIELD, dtype=DataType.SPARSE_FLOAT_VECTOR))
        schema = CollectionSchema(
            fields=fields,
            description=_description(knowledge_base_id, model, dimension),
            functions=[
                Function(
                    name="content_bm25",
                    input_field_names=["content"],
                    output_field_names=[_CONTENT_SPARSE_FIELD],
                    function_type=FunctionType.BM25,
                )
            ],
        )
        collection = Collection(name=name, schema=schema, using=alias)
        if dimension:
            collection.create_index(
                "embedding",
                {"metric_type": "COSINE", "index_type": "IVF_FLAT", "params": {"nlist": 1024}},
            )
        collection.create_index(
            _CONTENT_SPARSE_FIELD,
            {
                "metric_type": "BM25",
                "index_type": "SPARSE_INVERTED_INDEX",
                "params": {"inverted_index_algo": "DAAT_MAXSCORE"},
            },
        )
    _load_collection(collection, name)
    return collection


def _description(knowledge_base_id: str, model: str, dimension: int | None) -> str:
    return json.dumps(
        {"knowledge_base_id": knowledge_base_id, "embedding_model": model, "dimension": dimension},
        ensure_ascii=False,
        sort_keys=True,
    )


def _load_collection(collection, name: str) -> None:
    if name not in _LOADED:
        collection.load()
        _LOADED.add(name)


def _upsert_document_sync(
    knowledge_base_id: str,
    document_id: str,
    chunks: list[dict],
    embedding_config: dict | None,
) -> dict:
    collection = _get_or_create_collection(
        knowledge_base_id,
        embedding_config,
        [item["embedding"] for item in chunks if item.get("embedding")],
    )
    name = collection_name(knowledge_base_id)
    collection.delete(expr=f'file_id == {json.dumps(document_id)}')
    schema_fields = {field.name: field for field in collection.schema.fields}
    dimension = int(schema_fields["embedding"].params["dim"]) if "embedding" in schema_fields else None
    rows = []
    for chunk in chunks:
        vector = chunk.get("embedding")
        row = {
            "id": chunk["id"],
            "content": chunk["text"],
            "chunk_id": chunk["id"],
            "file_id": document_id,
            "chunk_index": int(chunk["chunk_index"]),
            "has_embedding": vector is not None,
        }
        if dimension:
            if vector is None:
                vector = [0.0] * dimension
            if len(vector) != dimension:
                raise ValueError(f"Chunk {chunk['id']} vector dimension does not match Milvus schema")
            row["embedding"] = vector
        elif vector is not None:
            raise MilvusUnavailable("知识库还没有向量索引 schema；请启用 Embedding 后重新索引")
        rows.append(row)
    for start in range(0, len(rows), 200):
        collection.insert(rows[start : start + 200])
    collection.flush()
    _load_collection(collection, name)
    return {"collection": name, "indexed_chunks": len(rows), "embedded_chunks": sum(row["has_embedding"] for row in rows)}


async def upsert_document(
    knowledge_base_id: str,
    document_id: str,
    chunks: list[dict],
    embedding_config: dict | None,
) -> dict:
    return await asyncio.to_thread(
        _upsert_document_sync, knowledge_base_id, document_id, chunks, embedding_config
    )


def _delete_document_sync(knowledge_base_id: str, document_id: str) -> None:
    from pymilvus import Collection, utility

    alias = _connect()
    name = collection_name(knowledge_base_id)
    if utility.has_collection(name, using=alias):
        collection = Collection(name=name, using=alias)
        collection.delete(expr=f"file_id == {json.dumps(document_id)}")
        collection.flush()


async def delete_document(knowledge_base_id: str, document_id: str) -> None:
    await asyncio.to_thread(_delete_document_sync, knowledge_base_id, document_id)


def _search_sync(
    knowledge_base_id: str,
    query: str,
    search_mode: str,
    recall_top_k: int,
    bm25_top_k: int,
    drop_ratio_search: float,
    similarity_threshold: float,
    vector_weight: float,
    bm25_weight: float,
    document_ids: list[str],
    query_vector: list[float] | None,
) -> dict[str, Any]:
    from pymilvus import AnnSearchRequest, Collection, WeightedRanker, utility

    alias = _connect()
    name = collection_name(knowledge_base_id)
    if not utility.has_collection(name, using=alias):
        raise MilvusUnavailable("知识库尚未写入 Milvus；请重新索引知识库文件")
    collection = Collection(name=name, using=alias)
    _load_collection(collection, name)
    schema_fields = {field.name for field in collection.schema.fields}
    expression = None
    if document_ids:
        expression = "file_id in [" + ",".join(json.dumps(item) for item in sorted(set(document_ids))) + "]"

    def rows(results, key: str) -> list[dict]:
        if not results:
            return []
        output = []
        for rank, hit in enumerate(results[0], 1):
            score = float(hit.distance)
            if key != "bm25" and score < similarity_threshold:
                continue
            output.append({"chunk_id": str(hit.entity.get("chunk_id")), key: score, "rank": rank})
        return output

    bm25: list[dict] = []
    vector: list[dict] = []
    fusion: list[dict] = []
    timing = {"bm25": 0.0, "vector": 0.0, "fusion": 0.0}
    if search_mode != "vector":
        mark = time.perf_counter()
        results = collection.search(
            data=[query],
            anns_field=_CONTENT_SPARSE_FIELD,
            param={"metric_type": "BM25", "params": {"drop_ratio_search": drop_ratio_search}},
            limit=bm25_top_k,
            expr=expression,
            output_fields=["chunk_id", "file_id", "chunk_index"],
        )
        bm25 = rows(results, "bm25")
        timing["bm25"] = round((time.perf_counter() - mark) * 1000, 2)

    if search_mode != "keyword" and query_vector is not None:
        if "embedding" not in schema_fields:
            raise MilvusUnavailable("知识库没有向量索引 schema；请重新索引知识库文件")
        vector_expr = "has_embedding == true"
        if expression:
            vector_expr = f"({expression}) and ({vector_expr})"
        mark = time.perf_counter()
        vector_results = collection.search(
            data=[query_vector],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 10}},
            limit=recall_top_k,
            expr=vector_expr,
            output_fields=["chunk_id", "file_id", "chunk_index"],
        )
        vector = rows(vector_results, "vector")
        timing["vector"] = round((time.perf_counter() - mark) * 1000, 2)
        if search_mode == "vector":
            mark = time.perf_counter()
            fusion = [{**item, "fusion": item["vector"]} for item in vector]
            timing["fusion"] = round((time.perf_counter() - mark) * 1000, 2)
        else:
            mark = time.perf_counter()
            vector_request = AnnSearchRequest(
                data=[query_vector],
                anns_field="embedding",
                param={"metric_type": "COSINE", "params": {"nprobe": 10}},
                limit=recall_top_k,
                expr=vector_expr,
            )
            bm25_request = AnnSearchRequest(
                data=[query],
                anns_field=_CONTENT_SPARSE_FIELD,
                param={"metric_type": "BM25", "params": {"drop_ratio_search": drop_ratio_search}},
                limit=bm25_top_k,
                expr=expression,
            )
            hybrid_results = collection.hybrid_search(
                reqs=[vector_request, bm25_request],
                rerank=WeightedRanker(vector_weight, bm25_weight),
                limit=recall_top_k,
                output_fields=["chunk_id", "file_id", "chunk_index"],
            )
            bm25_by_id = {item["chunk_id"]: item["bm25"] for item in bm25}
            vector_by_id = {item["chunk_id"]: item["vector"] for item in vector}
            fusion = [
                {
                    "chunk_id": item["chunk_id"],
                    "bm25": bm25_by_id.get(item["chunk_id"]),
                    "vector": vector_by_id.get(item["chunk_id"]),
                    "fusion": item["fusion"],
                    "hybrid": item["fusion"],
                    "rank": rank,
                }
                for rank, item in enumerate(rows(hybrid_results, "fusion"), 1)
            ]
            timing["fusion"] = round((time.perf_counter() - mark) * 1000, 2)
    elif search_mode != "keyword":
        mark = time.perf_counter()
        fusion = [{**item, "fusion": item["bm25"]} for item in bm25[:recall_top_k]]
        timing["fusion"] = round((time.perf_counter() - mark) * 1000, 2)

    if search_mode == "keyword":
        mark = time.perf_counter()
        fusion = [{**item, "fusion": item["bm25"]} for item in bm25[:recall_top_k]]
        timing["fusion"] = round((time.perf_counter() - mark) * 1000, 2)
    return {
        "bm25": bm25,
        "vector": vector,
        "fusion": fusion,
        "timing": timing,
        "embedding_query": query_vector is not None,
        "vector_skip_reason": None if query_vector is not None or search_mode == "keyword" else "embedding_not_configured_or_empty",
    }


async def search(
    knowledge_base_id: str,
    query: str,
    search_mode: str,
    recall_top_k: int,
    bm25_top_k: int,
    drop_ratio_search: float,
    similarity_threshold: float,
    vector_weight: float,
    bm25_weight: float,
    document_ids: list[str],
    embedding_config: dict | None,
) -> dict[str, Any]:
    from .embedding import embed_texts, enabled as embedding_enabled

    query_vector = None
    embedding_query_ms = 0.0
    if search_mode != "keyword" and embedding_enabled(embedding_config):
        mark = time.perf_counter()
        vectors = await embed_texts([query], embedding_config)
        query_vector = vectors[0] if vectors else None
        embedding_query_ms = round((time.perf_counter() - mark) * 1000, 2)
    result = await asyncio.to_thread(
        _search_sync,
        knowledge_base_id,
        query,
        search_mode,
        recall_top_k,
        bm25_top_k,
        drop_ratio_search,
        similarity_threshold,
        vector_weight,
        bm25_weight,
        document_ids,
        query_vector,
    )
    result["timing"]["vector"] = round(result["timing"]["vector"] + embedding_query_ms, 2)
    return result
