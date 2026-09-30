import io
import json
import zipfile
from pathlib import Path

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
    monkeypatch.setattr(settings, "mineru_enabled", False)
    monkeypatch.setattr(settings, "mineru_api_uri", "")
    monkeypatch.setattr(settings, "mineru_api_key", "")
    return TestClient(app)


def test_yuxi_database_config_persists_per_knowledge_base_and_masks_secrets(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={
            "database_name": "Yuxi 配置验收",
            "kb_type": "local",
            "additional_params": {
                "chunk_preset_id": "general",
                "chunk_token_num": 256,
                "overlapped_percent": 15,
                "parser_engine": "pymupdf-layout",
                "mineru_api_uri": "https://mineru.example/v1",
                "mineru_api_key": "mineru-secret",
                "embedding_base_url": "https://embedding.example/v1",
                "embedding_model": "BAAI/bge-m3",
                "embedding_api_key": "embedding-secret",
                "embedding_dimensions": 1024,
                "reranker_base_url": "https://reranker.example/v1/rerank",
                "reranker_model": "BAAI/bge-reranker-v2-m3",
                "reranker_api_key": "reranker-secret",
            },
        },
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]
    settings_payload = created.json()["additional_params"]
    assert settings_payload["embedding_api_key_set"] is True
    assert settings_payload["reranker_api_key_set"] is True
    assert settings_payload["mineru_api_key_set"] is True
    assert "embedding_api_key" not in settings_payload
    assert "reranker_api_key" not in settings_payload
    assert "mineru_api_key" not in settings_payload

    updated = client.put(
        f"/api/knowledge/databases/{kb_id}",
        json={"additional_params": {"chunk_token_num": 384}},
    )
    assert updated.status_code == 200, updated.text
    config = updated.json()["config"]
    assert config["chunking"]["chunk_token_num"] == 384
    assert config["embedding"]["model"] == "BAAI/bge-m3"
    assert config["embedding"]["api_key_set"] is True
    assert config["parser"]["mineru_api_uri"] == "https://mineru.example/v1"
    assert config["parser"]["api_key_set"] is True
    assert "api_key" not in config["parser"]
    assert config["reranker"]["model"] == "BAAI/bge-reranker-v2-m3"


def test_yuxi_local_file_pipeline_and_retrieval_use_pdf(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    pdf_path = pdf_sample

    created = client.post(
        "/api/knowledge/databases",
        json={
            "database_name": "Yuxi PDF 实际入库",
            "kb_type": "local",
            "additional_params": {
                "parser_engine": "pymupdf-layout",
                "chunk_token_num": 256,
                "overlapped_percent": 10,
            },
        },
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]

    with pdf_path.open("rb") as pdf:
        upload = client.post(
            "/api/knowledge/files/upload",
            params={"kb_id": kb_id},
            files={"file": (pdf_path.name, pdf, "application/pdf")},
        )
    assert upload.status_code == 200, upload.text
    stage_uri = upload.json()["file_path"]
    added = client.post(
        f"/api/knowledge/databases/{kb_id}/documents",
        json={"items": [stage_uri], "params": {"auto_index": True}},
    )
    assert added.status_code == 200, added.text
    assert added.json()["status"] == "success"
    document_id = added.json()["processed"][0]["document_id"]

    listed = client.get(f"/api/knowledge/databases/{kb_id}/documents")
    assert listed.status_code == 200
    document = next(row for row in listed.json()["items"] if row["file_id"] == document_id)
    assert document["status"] == "indexed"
    assert document["chunk_count"] > 0

    content = client.get(f"/api/knowledge/databases/{kb_id}/documents/{document_id}/content")
    assert content.status_code == 200
    located = [
        span
        for chunk in content.json()["chunks"]
        for span in chunk["source_spans"]
        if span.get("bbox")
    ]
    assert located

    results = client.post(
        f"/api/knowledge/databases/{kb_id}/query-test",
        json={"query": "geological hazard slope cracks rainfall", "meta": {"top_k": 3}},
    )
    assert results.status_code == 200, results.text
    assert results.json()
    assert all(item["metadata"]["file_id"] == document_id for item in results.json())

    stats = client.post(f"/api/knowledge/databases/{kb_id}/stats/repair", json={})
    assert stats.status_code == 200
    assert stats.json()["status"] == "not_required"
    assert stats.json()["stats"]["chunk_count"] == document["chunk_count"]
    folders = client.get(f"/api/knowledge/databases/{kb_id}/virtual-folders/detect")
    assert folders.status_code == 200 and folders.json()["has_virtual_folders"] is False
    migration = client.post(f"/api/knowledge/databases/{kb_id}/virtual-folders/migrate", json={})
    assert migration.status_code == 409

    mindmap = client.post(
        f"/api/knowledge/databases/{kb_id}/mindmap/generate",
        json={"file_ids": [document_id]},
    )
    assert mindmap.status_code == 503, mindmap.text
    assert "LLM 未配置" in mindmap.json()["detail"]
    mindmap_diff = client.get(f"/api/knowledge/databases/{kb_id}/mindmap/diff").json()
    assert mindmap_diff["needs_update"] is True
    assert mindmap_diff["added_files"][0]["file_id"] == document_id
    graph = client.post(f"/api/knowledge/databases/{kb_id}/graph-build/index", json={})
    assert graph.status_code == 409
    assert client.get(f"/api/knowledge/databases/{kb_id}/graph").json()["nodes"] == []

    relevant_id = results.json()[0]["id"]
    dataset_payload = {
        "name": "Yuxi 检索评估",
        "cases": [{"query": "重庆市地质灾害应急预案", "relevant_evidence_ids": [relevant_id], "top_k": 3}],
    }
    dataset_upload = client.post(
        f"/api/evaluation/databases/{kb_id}/datasets/upload",
        data={"name": "Yuxi 检索评估", "description": "真实 PDF 用例"},
        files={"file": ("evaluation.json", json.dumps(dataset_payload, ensure_ascii=False), "application/json")},
    )
    assert dataset_upload.status_code == 200, dataset_upload.text
    dataset_id = dataset_upload.json()["id"]
    evaluation = client.post(
        f"/api/evaluation/databases/{kb_id}/runs",
        json={"dataset_id": dataset_id},
    )
    assert evaluation.status_code == 200, evaluation.text
    assert evaluation.json()["result"]["case_count"] == 1


def test_yuxi_provider_test_routes_are_explicit_when_keys_are_missing(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "Provider test", "kb_type": "local"},
    )
    kb_id = created.json()["kb_id"]
    embedding = client.post(f"/api/knowledge/databases/{kb_id}/providers/embedding/test", json={})
    reranker = client.post(
        f"/api/knowledge/databases/{kb_id}/providers/reranker/test",
        json={"query": "滑坡隐患", "documents": ["雨后检查裂缝"]},
    )
    assert embedding.status_code == 503
    assert embedding.json()["status"] == "unavailable"
    assert reranker.status_code == 503
    assert reranker.json()["status"] == "unavailable"

    pdf_path = pdf_sample
    with pdf_path.open("rb") as pdf:
        parser = client.post(
            f"/api/knowledge/databases/{kb_id}/providers/parser/test",
            files={"file": (pdf_path.name, pdf, "application/pdf")},
        )
    assert parser.status_code == 503
    assert parser.json()["status"] == "unavailable"

    official_parser = client.post(
        f"/api/knowledge/databases/{kb_id}/providers/parser/test",
        data={"engine": "mineru_official"},
        files={"file": (pdf_path.name, pdf_path.read_bytes(), "application/pdf")},
    )
    assert official_parser.status_code == 503
    assert official_parser.json()["status"] == "unavailable"
    assert official_parser.json()["provider"] == "mineru_official"

    local_parser = client.post(
        f"/api/knowledge/databases/{kb_id}/providers/parser/test",
        data={"engine": "pymupdf-layout"},
        files={"file": (pdf_path.name, pdf_path.read_bytes(), "application/pdf")},
    )
    assert local_parser.status_code == 200, local_parser.text
    assert local_parser.json()["provider"] == "pymupdf-layout"
    assert local_parser.json()["blocks"] > 0


def test_yuxi_query_config_and_mindmap_are_persisted(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "配置视图", "kb_type": "local"},
    )
    kb_id = created.json()["kb_id"]
    saved = client.put(
        f"/api/knowledge/databases/{kb_id}/query-params",
        json={"top_k": 5, "recall_top_k": 24, "use_reranker": True},
    )
    assert saved.status_code == 200, saved.text
    assert saved.json()["final_top_k"] == 5
    assert saved.json()["recall_top_k"] == 24
    assert saved.json()["use_reranker"] is True
    assert client.get(f"/api/knowledge/databases/{kb_id}/query-params").json()["final_top_k"] == 5
    assert client.get("/api/knowledge/types").json()["kb_types"]["local"]["supports_documents"] is True


def test_yuxi_url_import_rejects_local_network_targets(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    response = client.post(
        "/api/knowledge/files/fetch-url",
        json={"url": "http://127.0.0.1/private.pdf"},
    )
    assert response.status_code == 422
    assert response.json()["detail"] == "URL 主机不可公开访问"


def test_yuxi_zip_folder_upload_indexes_supported_files_and_rejects_zip_slip(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "ZIP 文件夹导入", "kb_type": "local"},
    )
    kb_id = created.json()["kb_id"]

    archive_bytes = io.BytesIO()
    with zipfile.ZipFile(archive_bytes, "w") as archive:
        archive.writestr("应急预案/巡查说明.txt", "雨后巡查坡体裂缝、渗水和落石，记录现场变化。")
        archive.writestr("应急预案/流程.md", "# 处置流程\n\n发现变形迹象后立即上报并设置警戒。")
        archive.writestr("应急预案/忽略.bin", b"unsupported")
    upload = client.post(
        "/api/knowledge/files/upload-folder",
        params={"kb_id": kb_id},
        files={"file": ("应急预案.zip", archive_bytes.getvalue(), "application/zip")},
    )
    assert upload.status_code == 200, upload.text
    processed = client.post(
        "/api/knowledge/files/process-folder",
        json={"file_path": upload.json()["file_path"], "content_hash": upload.json()["content_hash"], "kb_id": kb_id},
    )
    assert processed.status_code == 200, processed.text
    assert processed.json()["status"] == "success"
    assert len(processed.json()["processed"]) == 2
    assert processed.json()["skipped_files"] == ["应急预案/忽略.bin"]
    documents = client.get(f"/api/knowledge/databases/{kb_id}/documents")
    assert documents.status_code == 200
    rows = documents.json()["items"]
    assert len([item for item in rows if not item["is_folder"]]) == 2
    assert all(item["status"] == "indexed" for item in rows)

    unsafe_bytes = io.BytesIO()
    with zipfile.ZipFile(unsafe_bytes, "w") as archive:
        archive.writestr("../outside.txt", "不得写出上传目录")
    unsafe_upload = client.post(
        "/api/knowledge/files/upload-folder",
        params={"kb_id": kb_id},
        files={"file": ("unsafe.zip", unsafe_bytes.getvalue(), "application/zip")},
    )
    assert unsafe_upload.status_code == 200, unsafe_upload.text
    rejected = client.post(
        "/api/knowledge/files/process-folder",
        json={"file_path": unsafe_upload.json()["file_path"], "content_hash": unsafe_upload.json()["content_hash"], "kb_id": kb_id},
    )
    assert rejected.status_code == 422
    assert "不安全的文件路径" in rejected.json()["detail"]
