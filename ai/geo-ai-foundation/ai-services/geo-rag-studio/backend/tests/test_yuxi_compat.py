import io
import json
import zipfile
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app import db, main as rag
from app.config import settings
from app.main import app


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "rag_workspace_dir", str(tmp_path / "workspace"))
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


def test_yuxi_workspace_tree_create_and_reject_traversal(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    workspace = Path(settings.rag_workspace_dir)
    workspace.mkdir(parents=True)
    (workspace / "资料").mkdir()
    (workspace / "资料" / "滑坡记录.txt").write_text("雨后巡查", encoding="utf-8")

    root = client.get("/api/workspace/tree")
    assert root.status_code == 200, root.text
    assert root.json()["entries"][0]["name"] == "资料"
    assert root.json()["entries"][0]["is_dir"] is True

    files = client.get("/api/workspace/tree", params={"path": "/资料", "files_only": True})
    assert files.status_code == 200, files.text
    assert files.json()["entries"][0]["path"] == "/资料/滑坡记录.txt"

    recursive_files = client.get("/api/workspace/tree", params={"recursive": True, "files_only": True})
    assert recursive_files.status_code == 200, recursive_files.text
    assert [entry["path"] for entry in recursive_files.json()["entries"]] == ["/资料/滑坡记录.txt"]

    created = client.post(
        "/api/workspace/directory",
        json={"parent_path": "/", "name": "待整理"},
    )
    assert created.status_code == 200, created.text
    assert created.json()["entry"]["path"] == "/待整理/"

    traversal = client.get("/api/workspace/tree", params={"path": "/../"})
    assert traversal.status_code == 403


def test_yuxi_document_search_matches_frontend_contract_and_paginates_all_folders(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/knowledge/databases", json={"database_name": "文件搜索验收", "kb_type": "local"}).json()
    kb_id = kb["kb_id"]
    folder = client.post(f"/api/knowledge/databases/{kb_id}/folders", json={"folder_name": "计划"}).json()
    ids = [db.create_document(
        name, str(pdf_sample), "application/pdf", "pymupdf-layout", "general", {}, {},
        knowledge_base_id=kb_id, folder_id=folder["file_id"] if index else None,
    ) for index, name in enumerate(("PLAN_100%.pdf", "plan-two.pdf", "unrelated.pdf"))]
    before_update = db.get_document(ids[0])["updated_at"]
    db.update_document_status(ids[0], "indexed")
    assert db.get_document(ids[0])["updated_at"] > before_update

    url = f"/api/knowledge/databases/{kb_id}/documents/search"
    first = client.get(url, params={"query": " PLAN ", "offset": 0, "limit": 1})
    assert first.status_code == 200, first.text
    assert set(first.json()) == {"files", "total", "offset", "limit", "has_more"}
    assert first.json()["total"] == 2
    assert first.json()["has_more"] is True
    assert first.json()["files"][0]["file_id"] == ids[0]
    assert first.json()["files"][0]["kb_name"] == kb["name"]
    assert first.json()["files"][0]["updated_at"] == db.get_document(ids[0])["updated_at"]
    second = client.get(url, params={"query": "plan", "offset": 1, "limit": 1}).json()
    assert second["files"][0]["file_id"] == ids[1]
    assert second["files"][0]["parent_id"] == folder["file_id"]
    assert second["total"] == 2 and second["has_more"] is False
    assert client.get(url, params={"query": "_100%"}).json()["total"] == 1
    assert client.get(url, params={"query": "missing", "offset": 0, "limit": 1}).json()["total"] == 0
    assert client.get(url, params={"query": " ", "offset": 7, "limit": 2}).json() == {
        "files": [], "total": 0, "offset": 0, "limit": 2, "has_more": False,
    }
    assert client.get(url, params={"offset": -1}).status_code == 422
    assert client.get(url, params={"limit": 501}).status_code == 422
    assert client.get("/api/knowledge/databases/missing/documents/search", params={"query": "plan"}).status_code == 404


def test_document_updated_at_migrates_without_inventing_legacy_update_time(tmp_path, monkeypatch, pdf_sample):
    _client(tmp_path, monkeypatch)
    doc_id = db.create_document("legacy.pdf", str(pdf_sample), "application/pdf", "auto", "general", {}, {})
    with db.connect() as connection:
        connection.execute("ALTER TABLE documents DROP COLUMN updated_at")
    assert db.get_document(doc_id)["updated_at"] is None
    db.update_document_status(doc_id, "failed", error="Parser unavailable")
    document = db.get_document(doc_id)
    assert document["updated_at"]
    assert document["error_message"] == "Parser unavailable"


def test_yuxi_workspace_pdf_import_runs_real_document_pipeline(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    source = Path(settings.rag_workspace_dir) / pdf_sample.name
    source.parent.mkdir(parents=True, exist_ok=True)
    source.write_bytes(pdf_sample.read_bytes())

    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "个人空间 PDF 导入", "kb_type": "local"},
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]

    imported = client.post(
        "/api/knowledge/files/import-workspace",
        json={"kb_id": kb_id, "paths": [f"/{pdf_sample.name}"]},
    )
    assert imported.status_code == 200, imported.text
    item = imported.json()["items"][0]
    assert item["file_path"].endswith(f"/{pdf_sample.name}")
    assert item["content_hash"]

    added = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/add",
        json={"items": [item["file_path"]], "params": {"auto_index": True}},
    )
    assert added.status_code == 200, added.text
    assert added.json()["status"] == "success"
    document_id = added.json()["processed"][0]["document_id"]
    document = next(row for row in client.get(f"/api/knowledge/databases/{kb_id}/documents").json()["items"] if row["file_id"] == document_id)
    assert document["status"] == "indexed"
    assert document["chunk_count"] > 0
    assert document["has_original_file"] is True
    assert document["has_parsed_markdown"] is True

    preview = client.get("/api/workspace/knowledge/file", params={"kb_id": kb_id, "file_id": document_id})
    assert preview.status_code == 200, preview.text
    assert preview.content == pdf_sample.read_bytes()
    assert preview.headers["content-type"] == "application/pdf"
    assert preview.headers["x-yuxi-preview-type"] == "pdf"
    assert preview.headers["x-yuxi-preview-filename"] == pdf_sample.name

    tree = client.get("/api/workspace/knowledge/tree", params={"kb_id": kb_id})
    assert tree.status_code == 200, tree.text
    entry = next(row for row in tree.json()["entries"] if row["file_id"] == document_id)
    assert entry["readonly"] is True
    assert entry["has_original_file"] is True
    assert entry["path"] == f"/knowledge/{kb_id}/file/{document_id}"
    assert client.get("/api/workspace/knowledge/tree", params={"kb_id": kb_id, "page": 0}).status_code == 422

    original = client.get("/api/workspace/knowledge/download", params={"kb_id": kb_id, "file_id": document_id})
    assert original.content == pdf_sample.read_bytes()
    parsed = client.get("/api/workspace/knowledge/download", params={"kb_id": kb_id, "file_id": document_id, "variant": "parsed"})
    assert parsed.status_code == 200, parsed.text
    assert "Slope cracks" in parsed.text
    assert "\\n\\n" not in parsed.text
    assert client.get("/api/workspace/knowledge/file", params={"kb_id": "wrong-kb", "file_id": document_id}).status_code == 404
    assert client.get("/api/workspace/knowledge/download", params={"kb_id": kb_id, "file_id": document_id, "variant": "other"}).status_code == 422

    folder = client.post(f"/api/knowledge/databases/{kb_id}/folders", json={"folder_name": "归档"}).json()
    folder_id = folder["file_id"]
    moved = client.put(f"/api/v1/knowledge-bases/{kb_id}/documents/{document_id}/move", json={"folder_id": folder_id})
    assert moved.status_code == 200, moved.text
    root_entries = client.get("/api/workspace/knowledge/tree", params={"kb_id": kb_id}).json()["entries"]
    assert all(row["file_id"] != document_id for row in root_entries)
    folder_entries = client.get("/api/workspace/knowledge/tree", params={"kb_id": kb_id, "parent_id": folder_id}).json()["entries"]
    assert [row["file_id"] for row in folder_entries] == [document_id]

    duplicate = client.post(
        "/api/knowledge/files/import-workspace",
        json={"kb_id": kb_id, "paths": [f"/{pdf_sample.name}"]},
    )
    assert duplicate.status_code == 409


def test_yuxi_database_config_persists_per_knowledge_base_and_masks_secrets(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    share_config = {
        "version": 2,
        "read_scope": {"access_level": "department", "department_ids": [17], "user_uids": []},
        "manage_scope": None,
    }
    created = client.post(
        "/api/knowledge/databases",
        json={
            "database_name": "Yuxi 配置验收",
            "kb_type": "local",
            "share_config": share_config,
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
    assert created.json()["share_config"] == share_config
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
    assert updated.json()["share_config"] == share_config


@pytest.mark.parametrize("upload_mime_type", ["application/pdf", "application/octet-stream"])
def test_yuxi_local_file_pipeline_and_retrieval_use_pdf(tmp_path, monkeypatch, pdf_sample, upload_mime_type):
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
            files={"file": (pdf_path.name, pdf, upload_mime_type)},
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

    assert document["mime_type"] == "application/pdf"
    page_image = client.get(f"/api/v1/documents/{document_id}/page/1/image")
    assert page_image.status_code == 200, page_image.text
    assert page_image.headers["content-type"] == "image/png"
    assert page_image.content.startswith(b"\x89PNG\r\n\x1a\n")

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
        json={"query": "geological hazard slope cracks rainfall", "meta": {"search_mode": "keyword", "top_k": 3}},
    )
    assert results.status_code == 200, results.text
    assert results.json()
    assert all(item["metadata"]["file_id"] == document_id for item in results.json())
    result = results.json()[0]
    assert result["metadata"]["chunk_index"] >= 0
    assert result["score"] == result["fusion_score"] == result["bm25_score"]
    assert result["distance"] == result["bm25_score"]

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
        json={"dataset_id": dataset_id, "search_mode": "keyword"},
    )
    assert evaluation.status_code == 200, evaluation.text
    assert evaluation.json()["result"]["case_count"] == 1


def test_yuxi_upload_rejects_duplicate_content_and_reports_same_name_versions(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/knowledge/databases", json={"database_name": "版本与去重", "kb_type": "local"}).json()
    kb_id = kb["kb_id"]
    content = "雨后核查裂缝和位移。".encode()
    uploaded = client.post("/api/knowledge/files/upload", params={"kb_id": kb_id}, files={"file": ("记录.txt", content, "text/plain")})
    assert uploaded.status_code == 200, uploaded.text
    added = client.post(f"/api/knowledge/databases/{kb_id}/documents", json={"items": [uploaded.json()["file_path"]], "params": {"auto_index": True}})
    assert added.json()["status"] == "success", added.text
    file_id = added.json()["processed"][0]["document_id"]
    duplicate = client.post("/api/knowledge/files/upload", params={"kb_id": kb_id}, files={"file": ("改名.txt", content, "text/plain")})
    assert duplicate.status_code == 409, duplicate.text
    assert "same content" in duplicate.json()["detail"]
    version = client.post("/api/knowledge/files/upload", params={"kb_id": kb_id}, files={"file": ("记录.txt", "雨后裂缝已扩大。".encode(), "text/plain")})
    assert version.status_code == 200, version.text
    assert version.json()["has_same_name"] is True
    same = version.json()["same_name_files"]
    assert len(same) == 1 and same[0]["file_id"] == file_id
    assert same[0]["content_hash"] == uploaded.json()["content_hash"]
    assert same[0]["size"] == len(content) and same[0]["created_at"]
    other = client.post("/api/knowledge/databases", json={"database_name": "另一个知识库", "kb_type": "local"}).json()
    separate = client.post("/api/knowledge/files/upload", params={"kb_id": other["kb_id"]}, files={"file": ("记录.txt", content, "text/plain")})
    assert separate.status_code == 200, separate.text
    assert separate.json()["has_same_name"] is False


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
        json={
            "top_k": 5,
            "recall_top_k": 24,
            "use_reranker": True,
            "reranker_model": "siliconflow-cn:Pro/BAAI/bge-reranker-v2-m3",
        },
    )
    assert saved.status_code == 200, saved.text
    assert saved.json()["final_top_k"] == 5
    assert saved.json()["recall_top_k"] == 24
    assert saved.json()["use_reranker"] is True
    query_params = client.get(f"/api/knowledge/databases/{kb_id}/query-params").json()
    assert query_params["final_top_k"] == 5
    options = {item["key"]: item for item in query_params["params"]["options"]}
    assert options["search_mode"]["default"] == "vector"
    assert options["final_top_k"]["default"] == 5
    assert options["recall_top_k"]["depend_on"] == ["use_reranker", True]
    assert options["reranker_model"]["type"] == "select"
    assert options["reranker_model"]["depend_on"] == ["use_reranker", True]
    assert options["reranker_model"]["default"] == "siliconflow-cn:Pro/BAAI/bge-reranker-v2-m3"
    assert any(item["value"] == "siliconflow-cn:Pro/BAAI/bge-reranker-v2-m3" for item in options["reranker_model"]["options"])
    unavailable = client.post(
        f"/api/knowledge/databases/{kb_id}/query",
        json={"query": "滑坡隐患", "meta": {}},
    )
    assert unavailable.status_code == 503
    assert "API key" in unavailable.json()["detail"]
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
    documents = client.get(f"/api/knowledge/databases/{kb_id}/documents", params={"recursive": True, "status": "indexed"})
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


def test_yuxi_knowledge_dashboard_stats_aggregates_local_database_and_files(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={"database_name": "知识统计验收", "kb_type": "local", "additional_params": {}},
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["kb_id"]

    content = "地质灾害预警响应需要核查监测数据与巡查记录。".encode()
    uploaded = client.post(
        "/api/knowledge/files/upload",
        params={"kb_id": kb_id},
        files={"file": ("应急预案.txt", content, "text/plain")},
    )
    assert uploaded.status_code == 200, uploaded.text
    added = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/add",
        json={"items": [uploaded.json()["file_path"]]},
    )
    assert added.status_code == 200, added.text
    assert added.json()["status"] == "success"

    response = client.get("/api/dashboard/stats/knowledge")

    assert response.status_code == 200, response.text
    assert response.json() == {
        "total_databases": 1,
        "total_files": 1,
        "total_nodes": 0,
        "total_storage_size": len(content),
        "databases_by_type": {"local": 1},
        "file_type_distribution": {"文本文件": 1},
    }


def test_yuxi_query_preserves_scores_chunk_metadata_and_optional_distance(monkeypatch):
    captured = {}

    async def fake_retrieve(request):
        captured["request"] = request
        return {
            "retrieval": {"search_mode": "hybrid", "use_graph_retrieval": True},
            "evidences": [
                {
                    "evidence_id": "EV-chunk-1",
                    "document_id": "file-1",
                    "chunk_id": "chunk-1",
                    "file_name": "预案.pdf",
                    "page": 2,
                    "bbox": [10, 20, 80, 90],
                    "text": "坡体裂缝扩大时应立即复核。",
                    "bm25_score": 0.0,
                    "vector_score": 0.42,
                    "fusion_score": 0.0,
                    "rerank_score": 0.0,
                    "scores": {
                        "bm25": 0.0,
                        "vector": 0.42,
                        "fusion": 0.0,
                        "rerank": 0.0,
                        "hybrid": 0.67,
                        "graph": 0.03,
                        "distance": 0.67,
                    },
                    "metadata": {"chunk_index": 7, "custom": "preserved"},
                }
            ],
        }

    monkeypatch.setattr(rag, "retrieve_api", fake_retrieve)
    client = TestClient(app)
    response = client.post(
        "/api/knowledge/databases/kb-test/query-test",
        json={"query": "坡体裂缝", "meta": {"top_k": 3, "include_distances": False}},
    )

    assert response.status_code == 200, response.text
    assert captured["request"].include_distances is False
    item = response.json()[0]
    assert item["score"] == 0.0
    assert item["rerank_score"] == 0.0
    assert item["bm25_score"] == 0.0
    assert item["vector_score"] == 0.42
    assert item["hybrid_score"] == 0.67
    assert item["graph_score"] == 0.03
    assert item["fusion_score"] == 0.0
    assert "distance" not in item
    assert item["metadata"] == {
        "chunk_index": 7,
        "custom": "preserved",
        "file_id": "file-1",
        "source": "预案.pdf",
        "chunk_id": "chunk-1",
        "page": 2,
        "bbox": [10, 20, 80, 90],
    }
