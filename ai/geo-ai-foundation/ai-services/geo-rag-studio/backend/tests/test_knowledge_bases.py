from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


def test_local_knowledge_base_isolates_documents_and_hides_api_keys(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")

    client = TestClient(app)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "隐患复核资料",
            "description": "真实资料测试",
            "kb_type": "local",
        },
    )
    assert created.status_code == 200
    knowledge_base = created.json()

    other = client.post("/api/v1/knowledge-bases", json={"name": "其他资料", "kb_type": "local"}).json()
    added = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "隐患调查记录.txt",
            "text": "重庆地质灾害隐患调查记录。斜坡稳定性较差，降雨后应复核裂缝与位移变化。",
            "knowledge_base_id": knowledge_base["id"],
        },
    )
    assert added.status_code == 200

    updated = client.put(
        f"/api/v1/knowledge-bases/{knowledge_base['id']}",
        json={"config": {
            "embedding": {"base_url": "https://example.invalid/v1", "model": "bge-m3", "api_key": "secret-test-key"},
            "parser": {"engine": "mineru_official", "api_key": "mineru-debug-secret"},
        }},
    )
    assert updated.status_code == 200
    assert updated.json()["config"]["embedding"]["api_key_set"] is True
    assert "api_key" not in updated.json()["config"]["embedding"]
    assert updated.json()["config"]["parser"]["api_key_set"] is True
    assert "api_key" not in updated.json()["config"]["parser"]

    listed = client.get(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents")
    assert listed.status_code == 200
    assert len(listed.json()) == 1

    retrieval = client.post(
        "/api/v1/debug/retrieve",
        json={
            "query": "斜坡稳定性 降雨 裂缝",
            "search_mode": "keyword",
            "final_top_k": 3,
            "filters": {"knowledge_base_id": knowledge_base["id"]},
        },
    )
    assert retrieval.status_code == 200
    assert retrieval.json()["result"]["evidences"]
    assert retrieval.json()["config_snapshot"]["parser"]["api_key_set"] is True
    assert "api_key" not in retrieval.json()["config_snapshot"]["parser"]
    assert "mineru-debug-secret" not in retrieval.text

    empty_retrieval = client.post(
        "/api/v1/debug/retrieve",
        json={"query": "裂缝", "search_mode": "keyword", "filters": {"knowledge_base_id": other["id"]}},
    )
    assert empty_retrieval.status_code == 200
    assert empty_retrieval.json()["result"]["evidences"] == []


def test_keyword_bm25_does_not_apply_vector_similarity_threshold(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")

    client = TestClient(app)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "BM25 threshold semantics",
            "kb_type": "local",
            "config": {"retrieval": {"search_mode": "keyword", "similarity_threshold": 0.99}},
        },
    )
    kb_id = created.json()["id"]
    added = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "bm25.txt",
            "text": "坡体裂缝复核需要查阅长期降雨、坡面位移、巡查记录和周边排水情况。",
            "knowledge_base_id": kb_id,
        },
    )
    assert added.status_code == 200, added.text

    response = client.post(
        "/api/v1/debug/retrieve",
        json={"query": "坡体裂缝", "filters": {"knowledge_base_id": kb_id}},
    )

    assert response.status_code == 200, response.text
    assert response.json()["result"]["evidences"]


def test_pdf_indexes_retrieves_and_locates_bbox(tmp_path, monkeypatch, pdf_sample):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    Path(settings.rag_upload_dir).mkdir(parents=True, exist_ok=True)
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "mineru_enabled", False)
    monkeypatch.setattr(settings, "mineru_api_uri", "")

    pdf_path = pdf_sample
    client = TestClient(app)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "PDF 样例",
            "kb_type": "local",
            "config": {
                "chunking": {"chunk_preset_id": "general", "chunk_token_num": 256, "overlapped_percent": 10},
                "parser": {"engine": "pymupdf-layout"},
                "retrieval": {"search_mode": "hybrid", "recall_top_k": 12, "final_top_k": 4},
            },
        },
    ).json()

    with pdf_path.open("rb") as pdf:
        upload = client.post(
            "/api/v1/documents/upload",
            data={"knowledge_base_id": knowledge_base["id"], "chunk_token_num": "64"},
            files={"file": (pdf_path.name, pdf, "application/pdf")},
        )
    assert upload.status_code == 200, upload.text
    assert upload.json()["blocks"] > 0
    assert upload.json()["chunks"] > 0

    documents = client.get(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents").json()
    document = documents[0]
    assert document["status"] == "indexed"
    assert document["error_message"] is None
    assert document["chunk_parser_config"]["chunk_token_num"] == 256
    chunks = client.get(
        f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents/{document['id']}/chunks"
    ).json()
    assert chunks
    located_spans = [span for chunk in chunks for span in chunk["source_spans"] if span.get("bbox")]
    assert located_spans
    page = located_spans[0]["page"]
    image = client.get(f"/api/v1/documents/{document['id']}/page/{page}/image")
    assert image.status_code == 200
    assert image.headers["content-type"] == "image/png"

    retrieval = client.post(
        "/api/v1/debug/retrieve",
        json={
            "query": "geological hazard slope cracks rainfall",
            "filters": {"knowledge_base_id": knowledge_base["id"]},
        },
    )
    assert retrieval.status_code == 200
    debug = retrieval.json()
    assert debug["result"]["evidences"]
    assert debug["bm25_candidates"]
    assert debug["fusion_candidates"]
    assert debug["final_evidences"]
    assert debug["final_context"]
    assert debug["timing"]["total"] >= 0
    assert debug["config_snapshot"]["retrieval"]["recall_top_k"] == 4
    assert len(debug["final_evidences"]) <= 4
    evidence = debug["final_evidences"][0]
    assert evidence["evidence_id"] and evidence["document_id"] and evidence["chunk_id"]
    assert evidence["file_name"] == pdf_path.name
    assert evidence["bbox"]
    assert evidence["source_spans"]
    assert evidence["bm25_score"] is not None


def test_provider_test_endpoints_report_unavailable_without_configuration(monkeypatch, pdf_sample):
    from app.config import settings

    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "rerank_base_url", "")
    monkeypatch.setattr(settings, "rerank_api_key", "")
    monkeypatch.setattr(settings, "rerank_model", "")
    monkeypatch.setattr(settings, "mineru_enabled", False)
    monkeypatch.setattr(settings, "mineru_api_uri", "")
    client = TestClient(app)
    embedding = client.post("/api/v1/providers/embedding/test", json={})
    reranker = client.post(
        "/api/v1/providers/reranker/test",
        json={"query": "滑坡隐患", "documents": ["雨后检查裂缝"]},
    )
    pdf_path = pdf_sample
    with pdf_path.open("rb") as pdf:
        parser = client.post(
            "/api/v1/providers/parser/test",
            files={"file": (pdf_path.name, pdf, "application/pdf")},
        )
    assert embedding.status_code == 503
    assert embedding.json()["status"] == "unavailable"
    assert reranker.status_code == 503
    assert reranker.json()["status"] == "unavailable"
    assert reranker.json()["provider"] == "openai"
    assert parser.status_code == 503
    assert parser.json()["status"] == "unavailable"


def test_pdf_parse_failure_is_persisted(monkeypatch, tmp_path, pdf_sample):
    from app.config import settings

    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "mineru_enabled", False)
    monkeypatch.setattr(settings, "mineru_api_uri", "")
    pdf_path = pdf_sample
    client = TestClient(app)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "MinerU 未配置的真实 PDF", "config": {"parser": {"engine": "mineru"}}},
    ).json()
    with pdf_path.open("rb") as pdf:
        upload = client.post(
            "/api/v1/documents/upload",
            data={"knowledge_base_id": knowledge_base["id"]},
            files={"file": (pdf_path.name, pdf, "application/pdf")},
        )
    documents = client.get(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents").json()
    assert upload.status_code == 422
    assert len(documents) == 1
    assert documents[0]["status"] == "failed"
    assert "MinerU OCR is not configured" in documents[0]["error_message"]
