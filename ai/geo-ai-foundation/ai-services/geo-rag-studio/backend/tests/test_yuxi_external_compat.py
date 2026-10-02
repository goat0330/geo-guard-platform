from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


def test_yuxi_external_database_routes_use_indexed_local_content(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    client = TestClient(app)

    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "外部知识库兼容", "kb_type": "local"},
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]
    indexed = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "slope-monitoring.txt",
            "text": "After rainfall, inspect slope cracks and monitoring instruments in Chongqing.",
            "knowledge_base_id": kb_id,
        },
    )
    assert indexed.status_code == 200, indexed.text
    document_id = indexed.json()["document_id"]

    databases = client.get("/api/knowledge/databases/external")
    assert databases.status_code == 200
    assert any(item["kb_id"] == kb_id and item["supports_documents"] for item in databases.json()["databases"])

    files = client.get(
        f"/api/knowledge/databases/external/{kb_id}/files",
        params={"query": "slope", "offset": 0, "limit": 10},
    )
    assert files.status_code == 200, files.text
    assert files.json()["total"] == 1
    assert files.json()["files"][0]["file_id"] == document_id

    retrieved = client.post(
        f"/api/knowledge/databases/external/{kb_id}/retrieve",
        json={"query": "slope cracks rainfall", "options": {"search_mode": "keyword", "top_k": 3}},
    )
    assert retrieved.status_code == 200, retrieved.text
    result = retrieved.json()["results"][0]
    assert result["kb_id"] == kb_id
    assert result["file_id"] == document_id
    assert "slope cracks" in result["content"]

    opened = client.get(f"/api/knowledge/databases/external/{kb_id}/files/{document_id}/open")
    assert opened.status_code == 200, opened.text
    assert opened.json()["total_lines"] > 0
    assert "monitoring instruments" in opened.json()["content"]

    found = client.post(
        f"/api/knowledge/databases/external/{kb_id}/files/{document_id}/find",
        json={"patterns": ["rainfall"], "window_size": 10},
    )
    assert found.status_code == 200, found.text
    assert found.json()["total_matches"] == 1
    assert found.json()["kb_id"] == kb_id
    assert found.json()["file_id"] == document_id


def test_yuxi_external_file_routes_reject_read_only_connectors(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    client = TestClient(app)
    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "Dify 只读源", "kb_type": "dify"},
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]

    files = client.get(f"/api/knowledge/databases/external/{kb_id}/files")
    assert files.status_code == 400
    assert "只支持检索" in files.json()["detail"]
