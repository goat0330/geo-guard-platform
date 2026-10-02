import json

import pymupdf
import pytest
from fastapi.testclient import TestClient

from app import db
from app.config import settings
from app.main import app


@pytest.mark.parametrize("location_case", ["later_page_box", "unlocated_box", "page_without_box"])
def test_retrieval_keeps_evidence_page_and_bbox_from_one_source_span(tmp_path, monkeypatch, location_case):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "rag_search_backend", "sqlite")
    pdf = pymupdf.open()
    for number in (1, 2):
        page = pdf.new_page()
        page.insert_text((72, 100), f"Slope monitoring evidence on page {number}.")
    bbox = list(pdf[1].get_text("blocks")[0][:4])
    content = pdf.tobytes()
    if location_case == "later_page_box":
        spans = [{"page": 1}, {"page": 2, "bbox": bbox}]
        expected_page, expected_bbox = 2, bbox
    elif location_case == "unlocated_box":
        spans = [{"bbox": bbox}, {"page": 1}]
        expected_page, expected_bbox = 1, None
    else:
        spans = [{"page": 1}]
        expected_page, expected_bbox = 1, None
    client = TestClient(app)
    uploaded = client.post("/api/v1/documents/upload", files={"file": ("source-span-test.pdf", content, "application/pdf")})
    assert uploaded.status_code == 200, uploaded.text
    document_id = uploaded.json()["document_id"]
    with db.connect() as connection:
        connection.execute("UPDATE chunks SET source_spans_json = ? WHERE document_id = ?", (json.dumps(spans), document_id))
    response = client.post("/api/v1/debug/retrieve", json={
        "query": "Slope monitoring", "search_mode": "keyword", "final_top_k": 1,
        "filters": {"document_id": document_id},
    })
    assert response.status_code == 200, response.text
    result = response.json()
    evidence = result["final_evidences"][0]
    assert evidence["page"] == expected_page
    assert evidence["bbox"] == expected_bbox
    assert len(evidence["source_spans"]) == len(spans)
    for stage in ("bm25_candidates", "fusion_candidates"):
        assert result[stage][0]["page"] == expected_page
        assert result[stage][0]["bbox"] == expected_bbox
    if expected_bbox:
        assert "page 2" in pdf[expected_page - 1].get_textbox(expected_bbox)
        assert "第2页" in result["final_context"]
    pdf.close()
