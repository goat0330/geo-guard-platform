from pathlib import Path
import importlib

from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from app.ocr_api import _upstream  # noqa: F401
from app.ocr_api import _build_processor
from app import ocr_api
from yuxi.knowledge.parser.capabilities import PARSER_CAPABILITIES


def test_rapidocr_onnx_runtime_is_available_for_migrated_parser():
    # Importing the concrete engine must work before model download or PDF parsing.
    # This catches missing runtime wheels without making tests depend on network models.
    importlib.import_module("rapidocr.inference_engine.onnxruntime.main")


def test_rapidocr_health_reports_missing_runtime(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    import_module = ocr_api.importlib.import_module

    def without_onnxruntime(module_name, *args, **kwargs):
        if module_name == "rapidocr.inference_engine.onnxruntime.main":
            raise ModuleNotFoundError("No module named 'onnxruntime'", name="onnxruntime")
        return import_module(module_name, *args, **kwargs)

    monkeypatch.setattr(ocr_api.importlib, "import_module", without_onnxruntime)
    health = client.get("/api/system/ocr/health").json()["health"]["rapid_ocr"]

    assert health["status"] == "unavailable"
    assert "onnxruntime" in health["message"]


def test_rapidocr_pdf_upload_preserves_page_and_bbox(tmp_path, monkeypatch, pdf_sample):
    client = _client(tmp_path, monkeypatch)
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "OCR 页面定位", "config": {"parser": {"engine": "rapid_ocr"}}},
    ).json()

    class Result:
        boxes = [[(144, 144), (600, 144), (600, 220), (144, 220)]]
        txts = ["Slope crack after rainfall"]

    class Processor:
        def _load_model(self):
            pass

        def _create_temp_image_file(self, image):
            image_path = tmp_path / "ocr-page.png"
            image.save(image_path)
            return str(image_path)

        def ocr(self, image_path):
            assert Path(image_path).is_file()
            return Result()

    monkeypatch.setattr(ocr_api, "_build_processor", lambda _engine: Processor())
    with Path(pdf_sample).open("rb") as pdf:
        uploaded = client.post(
            "/api/v1/documents/upload",
            data={"knowledge_base_id": knowledge_base["id"]},
            files={"file": (Path(pdf_sample).name, pdf, "application/pdf")},
        )
    assert uploaded.status_code == 200, uploaded.text

    document = client.get(f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents").json()[0]
    assert document["status"] == "indexed"
    blocks = client.get(
        f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents/{document['id']}/content"
    ).json()["blocks"]
    assert blocks[0]["page"] == 1
    assert blocks[0]["bbox"] == [72.0, 72.0, 300.0, 110.0]
    chunks = client.get(
        f"/api/v1/knowledge-bases/{knowledge_base['id']}/documents/{document['id']}/chunks"
    ).json()
    span = chunks[0]["source_spans"][0]
    assert span["page"] == 1
    assert span["bbox"] == [72.0, 72.0, 300.0, 110.0]


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
    assert calls == []
    file_id = added.json()["items"][0]["file_id"]
    assert added.json()["items"][0]["file_meta"]["processing_params"]["ocr_engine"] == "pp_structure_v3_ocr"
    parsed = client.post(f"/api/knowledge/databases/{kb_id}/documents/parse", json={"file_ids": [file_id]})
    assert parsed.status_code == 200, parsed.text
    assert parsed.json()["failed"] == []
    assert calls == [(".pdf", {})]
    document = parsed.json()["processed"][0]
    assert document["parser"] == "pp_structure_v3_ocr"
    assert document["status"] == "parsed"
