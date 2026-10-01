from pathlib import Path

from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from app.ocr_api import _upstream  # noqa: F401
from app.ocr_api import _build_processor
from app import ocr_api
from yuxi.knowledge.parser.capabilities import PARSER_CAPABILITIES


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.delenv("MINERU_API_KEY", raising=False)
    monkeypatch.delenv("MINERU_API_URI", raising=False)
    monkeypatch.delenv("SILICONFLOW_API_KEY", raising=False)
    monkeypatch.delenv("PADDLEOCR_API_TOKEN", raising=False)
    return TestClient(app)


def test_yuxi_ocr_options_are_from_vendored_capabilities(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    response = client.get("/api/system/ocr/options")

    assert response.status_code == 200
    data = response.json()
    assert data["default_engine"] == "rapid_ocr"
    assert [item["engine_id"] for item in data["engines"]] == list(PARSER_CAPABILITIES)


def test_ocr_option_config_persists_and_masks_keys(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    secret = "dummy-mineru-test-key"

    saved = client.put(
        "/api/system/config/options/mineru_official_api_opts",
        json={"value": {"api_key": secret}},
    )
    assert saved.status_code == 200
    option = saved.json()["option"]
    assert option["value"]["api_key"] == ""
    assert option["sensitive_state"]["api_key"]["source"] == "database"
    assert secret not in saved.text
    assert _build_processor("mineru_official").api_key == secret

    updated = client.post("/api/system/config/update", json={"default_ocr_engine": "mineru_ocr"})
    assert updated.status_code == 200
    assert updated.json()["default_ocr_engine"] == "mineru_ocr"
    assert client.get("/api/system/config").json()["default_ocr_engine"] == "mineru_ocr"


def test_yuxi_document_upload_uses_selected_upstream_ocr_engine(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    knowledge_base = client.post(
        "/api/knowledge/databases",
        json={"database_name": "OCR source migration", "kb_type": "local"},
    ).json()
    kb_id = knowledge_base["kb_id"]
    with Path(pdf_sample).open("rb") as pdf:
        uploaded = client.post(
            "/api/knowledge/files/upload",
            params={"kb_id": kb_id},
            files={"file": (Path(pdf_sample).name, pdf, "application/pdf")},
        )
    assert uploaded.status_code == 200, uploaded.text

    calls = []

    class Parser:
        def process_file(self, file_path, params):
            calls.append((Path(file_path).suffix, params))
            return "Yuxi OCR parser output: slope crack field inspection."

    monkeypatch.setattr(ocr_api, "_build_processor", lambda engine: Parser())
    added = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/add",
        json={
            "items": [uploaded.json()["file_path"]],
            "params": {"ocr_engine": "pp_structure_v3_ocr"},
        },
    )

    assert added.status_code == 200, added.text
    assert calls == [(".pdf", {})]
    document = added.json()["processed"][0]
    assert document["result"]["parser"] == "pp_structure_v3_ocr"
    assert document["result"]["status"] == "parsed"
