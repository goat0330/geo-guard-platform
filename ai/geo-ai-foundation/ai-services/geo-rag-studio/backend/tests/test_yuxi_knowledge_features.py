import json

from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "rerank_base_url", "")
    monkeypatch.setattr(settings, "rerank_api_key", "")
    monkeypatch.setattr(settings, "rerank_model", "")
    return TestClient(app)


def test_local_knowledge_management_views_retrieval_and_evaluation(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Yuxi 功能迁移验收", "kb_type": "local"},
    ).json()
    kb_id = knowledge_base["id"]

    folder = client.post(f"/api/v1/knowledge-bases/{kb_id}/folders", json={"name": "规范"}).json()
    added = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "重庆地质灾害隐患复核.txt",
            "text": "重庆地质灾害隐患复核需要核对斜坡裂缝、降雨和位移监测记录。雨后应复查斜坡稳定性。",
            "knowledge_base_id": kb_id,
        },
    )
    assert added.status_code == 200
    doc_id = added.json()["document_id"]

    renamed = client.put(f"/api/v1/knowledge-bases/{kb_id}/folders/{folder['id']}/rename", json={"name": "规程"})
    assert renamed.status_code == 200 and renamed.json()["name"] == "规程"
    moved = client.put(
        f"/api/v1/knowledge-bases/{kb_id}/documents/{doc_id}/move", json={"folder_id": folder["id"]}
    )
    assert moved.status_code == 200
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/documents/search", params={"q": "复核"}).json()[0]["id"] == doc_id

    mindmap = client.post(f"/api/v1/knowledge-bases/{kb_id}/mindmap/generate", json={"document_ids": [doc_id]})
    assert mindmap.status_code == 503
    assert "LLM 未配置" in mindmap.json()["detail"]
    current_mindmap = client.get(f"/api/v1/knowledge-bases/{kb_id}/mindmap", params={"document_id": doc_id}).json()
    assert current_mindmap == {"status": "not_generated", "mindmap": None}
    diff = client.get(f"/api/v1/knowledge-bases/{kb_id}/mindmap/diff").json()
    assert diff["needs_update"] is True and diff["added_files"][0]["file_id"] == doc_id

    graph = client.post(f"/api/v1/knowledge-bases/{kb_id}/graph-build/index")
    assert graph.status_code == 409
    graph_status = client.get(f"/api/v1/knowledge-bases/{kb_id}/graph-build/status").json()
    assert graph_status["total_chunks"] > 0
    assert graph_status["pending_chunks"] == graph_status["total_chunks"]
    graph_list = client.get("/api/graph/list")
    assert graph_list.status_code == 200
    assert any(item["id"] == kb_id and item["type"] == "local" for item in graph_list.json()["data"])
    graph_stats = client.get("/api/graph/stats", params={"kb_id": kb_id})
    assert graph_stats.status_code == 200 and graph_stats.json()["data"]["total_nodes"] == 0
    graph_labels = client.get("/api/graph/labels", params={"kb_id": kb_id})
    assert graph_labels.status_code == 200 and graph_labels.json()["success"] is True
    graph_subgraph = client.get("/api/graph/subgraph", params={"kb_id": kb_id, "max_nodes": 10})
    assert graph_subgraph.status_code == 200 and graph_subgraph.json()["data"]["nodes"] == []
    graph_enabled = client.put(
        f"/api/v1/knowledge-bases/{kb_id}/query-params",
        json={"search_mode": "keyword", "use_graph_retrieval": True, "similarity_threshold": 0.0},
    )
    assert graph_enabled.status_code == 200
    debug = client.post(
        "/api/v1/debug/retrieve",
        json={"query": "斜坡裂缝隐患", "filters": {"knowledge_base_id": kb_id}},
    )
    assert debug.status_code == 200
    assert debug.json()["graph_candidates"] == []
    assert debug.json()["final_context"]

    chunk = client.get(f"/api/v1/knowledge-bases/{kb_id}/documents/{doc_id}/chunks").json()[0]
    dataset = {"name": "人工验收集", "cases": [{"query": "斜坡裂缝", "relevant_evidence_ids": [f"EV-{chunk['id']}"]}]}
    uploaded = client.post(
        f"/api/v1/knowledge-bases/{kb_id}/evaluation/datasets/upload",
        files={"file": ("bench.json", json.dumps(dataset, ensure_ascii=False).encode("utf-8"), "application/json")},
    )
    assert uploaded.status_code == 200
    dataset_id = uploaded.json()["id"]
    run = client.post(f"/api/v1/knowledge-bases/{kb_id}/evaluation/runs", json={"dataset_id": dataset_id})
    assert run.status_code == 200
    run_id = run.json()["id"]
    assert run.json()["result"]["case_count"] == 1
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/evaluation/runs/{run_id}").json()["id"] == run_id
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/evaluation/datasets/{dataset_id}").json()["total"] == 1


def test_notion_connector_reports_unavailable_until_token_is_set(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Notion connector", "kb_type": "notion", "config": {"notion": {"base_url": "https://api.notion.com/v1"}}},
    )
    assert created.status_code == 200
    tested = client.post(f"/api/v1/knowledge-bases/{created.json()['id']}/connection-test")
    assert tested.status_code == 503
    assert "Integration Token" in tested.json()["detail"]


def test_graph_config_masks_api_key_and_requires_a_provider_before_indexing(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Graph provider config", "kb_type": "local"},
    ).json()
    kb_id = knowledge_base["id"]
    configured = client.post(
        f"/api/v1/knowledge-bases/{kb_id}/graph-build/config",
        json={"extractor_type": "llm", "extractor_options": {"base_url": "https://models.example/v1", "model": "test-model", "api_key": "private-test-value"}},
    )
    assert configured.status_code == 200
    assert "private-test-value" not in configured.text
    assert configured.json()["config"]["extractor_options"]["api_key_set"] is True
