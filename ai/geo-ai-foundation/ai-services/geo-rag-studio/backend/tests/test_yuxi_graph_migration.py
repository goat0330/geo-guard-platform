import asyncio
import math
from pathlib import Path

import pytest

from app import db
from app.config import settings
from app import knowledge_features
from app import retrieval
from app.main import app
from app.retrieval import tokenize
from app.yuxi_port.graph import fuse_chunk_rankings, rank_chunks_by_ppr, seed_subgraph
from app.yuxi_port.milvus_ranker import activate_score, weighted_hybrid_score
from fastapi.testclient import TestClient


def test_yuxi_graph_ppr_uses_seed_personalization_and_chunk_nodes():
    graph = {
        "nodes": [
            {"id": "E1", "type": "Hazard"},
            {"id": "E2", "type": "Location"},
            {"id": "CHUNK-C1", "type": "Chunk", "chunk_id": "C1"},
            {"id": "CHUNK-C2", "type": "Chunk", "chunk_id": "C2"},
        ],
        "edges": [
            {"source": "E1", "target": "CHUNK-C1"},
            {"source": "E1", "target": "E2"},
            {"source": "E2", "target": "CHUNK-C2"},
        ],
    }
    subgraph = seed_subgraph(graph, {"E1": 1.0}, max_nodes=100)

    ranked = rank_chunks_by_ppr(subgraph, {"E1": 1.0}, top_k=10, damping=0.85)

    assert [chunk_id for chunk_id, _ in ranked] == ["C1", "C2"]
    assert ranked[0][1] > ranked[1][1] > 0


def test_yuxi_graph_fusion_uses_weighted_reciprocal_rank_k_60():
    fused = fuse_chunk_rankings(
        [{"id": "C1", "text": "base one"}, {"id": "C2", "text": "base two"}],
        [{"id": "C2", "graph": 0.8}, {"id": "C3", "graph": 0.7}],
        graph_weight=2.0,
    )

    assert [item["id"] for item in fused] == ["C2", "C3", "C1"]
    assert fused[0]["fusion"] == 1 / 62 + 2 / 61
    assert fused[1]["fusion"] == 2 / 62
    assert fused[0]["fusion_sources"] == ["chunk", "graph"]


@pytest.mark.parametrize("fresh_snapshot", [True, False])
def test_deleting_document_prunes_graph_and_keeps_shared_entities(
    fresh_snapshot: bool, tmp_path: Path, monkeypatch
):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "documents"))
    monkeypatch.setattr(settings, "rag_workspace_dir", str(tmp_path / "workspace"))
    monkeypatch.setattr(settings, "rag_search_backend", "sqlite")
    Path(settings.rag_upload_dir).mkdir()
    Path(settings.rag_workspace_dir).mkdir()

    with TestClient(app) as client:
        knowledge_base = client.post(
            "/api/v1/knowledge-bases",
            json={"name": "Graph deletion parity", "config": {"embedding": {"model": "test-embedding"}}},
        ).json()
        documents = []
        for name, text in (
            ("source-a.txt", "Shared hazard and unique slope."),
            ("source-b.txt", "Shared hazard and river."),
        ):
            response = client.post(
                "/api/v1/documents/text",
                json={"file_name": name, "text": text, "knowledge_base_id": knowledge_base["id"]},
            )
            assert response.status_code == 200, response.text
            documents.append(db.get_document(response.json()["document_id"]))

        first, second = documents
        first_chunk = db.get_chunks(first["id"], include_embedding=False)[0]
        second_chunk = db.get_chunks(second["id"], include_embedding=False)[0]
        document_ids = [first["id"], second["id"]]
        fingerprint = knowledge_features.indexed_content_fingerprint(document_ids)
        saved_fingerprint = fingerprint if fresh_snapshot else "stale-graph-snapshot"
        graph = {
            "status": "indexed",
            "generation": "test",
            "source_document_ids": document_ids,
            "source_fingerprint": saved_fingerprint,
            "processed_chunk_count": 2,
            "failed_chunks": [],
            "vector_index": {"status": "indexed", "model": "test-embedding", "record_count": 5},
            "nodes": [
                {
                    "id": f"CHUNK-{first_chunk['id']}", "type": "Chunk", "chunk_id": first_chunk["id"],
                    "document_id": first["id"], "file_name": first["file_name"],
                },
                {
                    "id": f"CHUNK-{second_chunk['id']}", "type": "Chunk", "chunk_id": second_chunk["id"],
                    "document_id": second["id"], "file_name": second["file_name"],
                },
                {
                    "id": "E-SHARED", "name": "shared hazard", "type": "Hazard",
                    "document_id": first["id"], "file_name": first["file_name"],
                    "source_document_ids": document_ids,
                },
                {"id": "E-ONLY", "name": "unique slope", "type": "Hazard", "document_id": second["id"],
                 "file_name": second["file_name"]},
                {"id": "E-KEEP", "name": "river", "type": "Location", "document_id": second["id"],
                 "file_name": second["file_name"], "source_document_ids": [second["id"]]},
                {"id": "E-RELATION", "name": "shared fault", "type": "Hazard", "document_id": first["id"],
                 "file_name": first["file_name"]},
            ],
            "edges": [
                {"id": "M-A-SHARED", "source": f"CHUNK-{first_chunk['id']}", "target": "E-SHARED",
                 "type": "MENTIONS", "document_id": first["id"]},
                {"id": "M-A-ONLY", "source": f"CHUNK-{first_chunk['id']}", "target": "E-ONLY",
                 "type": "MENTIONS", "document_id": first["id"]},
                {"id": "M-B-SHARED", "source": f"CHUNK-{second_chunk['id']}", "target": "E-SHARED",
                 "type": "MENTIONS", "document_id": second["id"]},
                {"id": "M-B-KEEP", "source": f"CHUNK-{second_chunk['id']}", "target": "E-KEEP",
                 "type": "MENTIONS", "document_id": second["id"]},
                {"id": "R-A", "source": "E-SHARED", "target": "E-ONLY", "type": "RELATED_TO",
                 "document_id": first["id"], "chunk_id": first_chunk["id"]},
                {"id": "R-B", "source": "E-SHARED", "target": "E-KEEP", "type": "RELATED_TO",
                 "document_id": second["id"], "chunk_id": second_chunk["id"]},
                {"id": "R-B-RELATION", "source": "E-RELATION", "target": "E-KEEP", "type": "RELATED_TO",
                 "document_id": second["id"], "chunk_id": second_chunk["id"]},
            ],
        }
        db.save_knowledge_view(knowledge_base["id"], "graph", "", graph)
        db.replace_knowledge_graph_embeddings(
            knowledge_base["id"], saved_fingerprint, "test-embedding",
            [
                {"record_type": "entity", "record_id": record_id, "content": record_id, "embedding": [1.0, 0.0]}
                for record_id in ("E-SHARED", "E-ONLY", "E-KEEP", "E-RELATION")
            ] + [
                {"record_type": "triple", "record_id": record_id, "content": record_id, "embedding": [0.0, 1.0]}
                for record_id in ("R-A", "R-B", "R-B-RELATION")
            ],
        )

        deleted = client.delete(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents/{first['id']}")
        assert deleted.status_code == 200, deleted.text
        saved = db.get_knowledge_view(knowledge_base["id"], "graph")
        assert saved is not None
        pruned = saved["payload"]
        node_ids = {item["id"] for item in pruned["nodes"]}
        edge_ids = {item["id"] for item in pruned["edges"]}
        assert node_ids == {f"CHUNK-{second_chunk['id']}", "E-SHARED", "E-KEEP", "E-RELATION"}
        assert edge_ids == {"M-B-SHARED", "M-B-KEEP", "R-B", "R-B-RELATION"}
        shared = next(item for item in pruned["nodes"] if item["id"] == "E-SHARED")
        assert shared["source_document_ids"] == [second["id"]]
        assert shared["document_id"] == second["id"]
        assert shared["file_name"] == second["file_name"]
        relation_entity = next(item for item in pruned["nodes"] if item["id"] == "E-RELATION")
        assert relation_entity["document_id"] == second["id"]
        assert relation_entity["file_name"] == second["file_name"]
        assert pruned["source_document_ids"] == [second["id"]]
        assert pruned["source_fingerprint"] == (
            knowledge_features.indexed_content_fingerprint([second["id"]])
            if fresh_snapshot else saved_fingerprint
        )
        status = client.get(f"/api/v1/knowledge-bases/{knowledge_base['id']}/graph-build/status")
        assert status.status_code == 200, status.text
        assert status.json()["status"] == ("indexed" if fresh_snapshot else "outdated")
        vectors = db.get_knowledge_graph_embeddings(
            knowledge_base["id"], pruned["source_fingerprint"], "test-embedding"
        )
        assert {(item["record_type"], item["record_id"]) for item in vectors} == {
            ("entity", "E-SHARED"), ("entity", "E-KEEP"), ("entity", "E-RELATION"),
            ("triple", "R-B"), ("triple", "R-B-RELATION")
        }
        assert pruned["vector_index"]["record_count"] == 5

        deleted_last = client.delete(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents/{second['id']}")
        assert deleted_last.status_code == 200, deleted_last.text
        assert db.get_knowledge_view(knowledge_base["id"], "graph") is None


def test_yuxi_hybrid_score_matches_milvus_256_weighted_ranker():
    cosine_score = activate_score("COSINE", 0.8)
    bm25_score = activate_score("BM25", 5.0)

    assert cosine_score == 0.9
    assert bm25_score == 2.0 * math.atan(5.0) / math.pi
    assert weighted_hybrid_score(
        vector_score=0.8,
        bm25_score=5.0,
        vector_weight=0.7,
        bm25_weight=0.3,
    ) == 0.7 * cosine_score + 0.3 * bm25_score
    assert weighted_hybrid_score(
        vector_score=None,
        bm25_score=5.0,
        vector_weight=0.7,
        bm25_weight=0.3,
    ) == 0.3 * bm25_score


def test_yuxi_chinese_analyzer_uses_jieba_search_tokens():
    tokens = tokenize("Milvus 是一个高性能、可扩展的向量数据库！")

    assert tokens == [
        "Milvus",
        "是",
        "一个",
        "高性",
        "性能",
        "高性能",
        "可",
        "扩展",
        "的",
        "向量",
        "数据",
        "据库",
        "数据库",
    ]


def test_yuxi_graph_vectors_are_persisted_and_scoped_by_source_and_model(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    records = [
        {
            "record_type": "entity",
            "record_id": "E1",
            "content": "裂缝",
            "embedding": [1.0, 0.0],
        }
    ]

    assert db.replace_knowledge_graph_embeddings("KB-1", "fp-1", "bge-m3", records) == 1
    assert db.get_knowledge_graph_embeddings("KB-1", "fp-1", "bge-m3") == records
    assert db.get_knowledge_graph_embeddings("KB-1", "fp-2", "bge-m3") == []
    assert db.get_knowledge_graph_embeddings("KB-1", "fp-1", "other-model") == []


def test_graph_vector_index_uses_yuxi_entity_and_triple_content(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(knowledge_features, "embedding_enabled", lambda config: True)

    async def embed(texts, config):
        return [[float(index + 1), 0.0] for index, _ in enumerate(texts)]

    monkeypatch.setattr(knowledge_features, "embed_batched", embed)
    result = asyncio.run(
        knowledge_features.index_knowledge_graph_vectors(
            {"id": "KB-1", "config": {"embedding": {"model": "provider:bge-m3"}}},
            {
                "source_fingerprint": "fp-1",
                "nodes": [
                    {"id": "E1", "name": "裂缝", "type": "Hazard"},
                    {"id": "E2", "name": "斜坡", "type": "Location"},
                    {"id": "CHUNK-C1", "chunk_id": "C1", "type": "Chunk"},
                ],
                "edges": [
                    {"id": "R1", "source": "E1", "target": "E2", "type": "LOCATED_ON"},
                    {"id": "M1", "source": "CHUNK-C1", "target": "E1", "type": "MENTIONS"},
                ],
            },
        )
    )

    vectors = db.get_knowledge_graph_embeddings("KB-1", "fp-1", "provider:bge-m3")
    assert result == {"status": "indexed", "model": "provider:bge-m3", "record_count": 3}
    assert [(item["record_type"], item["content"]) for item in vectors] == [
        ("entity", "裂缝"),
        ("entity", "斜坡"),
        ("triple", "裂缝 → LOCATED_ON → 斜坡"),
    ]


def test_yuxi_graph_retrieval_runs_through_local_debug_pipeline(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(retrieval, "embedding_enabled", lambda config=None: True)

    async def embed_query(texts, config):
        assert texts == ["斜坡裂缝" ]
        return [[1.0, 0.0] for _ in texts]

    monkeypatch.setattr(retrieval, "embed_texts", embed_query)
    client = TestClient(app)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "Yuxi 图检索运行链路",
            "config": {
                "embedding": {"model": "test-embedding"},
                "retrieval": {"search_mode": "keyword", "use_graph_retrieval": True, "recall_top_k": 3, "final_top_k": 1},
            },
        },
    ).json()
    added = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "隐患复核.txt",
            "text": "斜坡裂缝在强降雨后扩大，需要复核边坡稳定性。",
            "knowledge_base_id": knowledge_base["id"],
        },
    )
    assert added.status_code == 200
    document_id = added.json()["document_id"]
    chunk = db.get_chunks(document_id, include_embedding=False)[0]
    fingerprint = knowledge_features.indexed_content_fingerprint([document_id])
    graph = {
        "status": "indexed",
        "source_document_ids": [document_id],
        "source_fingerprint": fingerprint,
        "vector_index": {"status": "indexed"},
        "nodes": [
            {"id": "E-SLOPE", "name": "斜坡裂缝", "type": "Hazard"},
            {"id": f"CHUNK-{chunk['id']}", "chunk_id": chunk["id"], "type": "Chunk"},
        ],
        "edges": [
            {"id": "M-1", "source": f"CHUNK-{chunk['id']}", "target": "E-SLOPE", "type": "MENTIONS"}
        ],
    }
    db.save_knowledge_view(knowledge_base["id"], "graph", "", graph)
    db.replace_knowledge_graph_embeddings(
        knowledge_base["id"],
        fingerprint,
        "test-embedding",
        [{"record_type": "entity", "record_id": "E-SLOPE", "content": "斜坡裂缝", "embedding": [1.0, 0.0]}],
    )

    response = client.post(
        "/api/v1/debug/retrieve",
        json={"query": "斜坡裂缝", "filters": {"knowledge_base_id": knowledge_base["id"]}},
    )

    assert response.status_code == 200, response.text
    debug = response.json()
    assert debug["result"]["retrieval"]["graph_status"] == "completed"
    assert debug["config_snapshot"]["retrieval"]["recall_top_k"] == 3
    assert debug["graph_candidates"][0]["chunk_id"] == chunk["id"]
    assert debug["fusion_candidates"][0]["score_type"] == "yuxi_weighted_rrf_k60"
    assert debug["final_evidences"][0]["chunk_id"] == chunk["id"]
