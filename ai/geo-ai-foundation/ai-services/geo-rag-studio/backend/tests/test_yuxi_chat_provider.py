import asyncio

from fastapi.testclient import TestClient

from app import db
from app.chat import complete_chat
from app.config import settings
from app.main import app
from app.provider_store import save_provider


class _Response:
    def raise_for_status(self):
        return None

    def json(self):
        return {"choices": [{"message": {"content": "已生成的真实响应"}}]}


class _AsyncClient:
    def __init__(self, *, timeout):
        self.timeout = timeout

    async def __aenter__(self):
        return self

    async def __aexit__(self, *_args):
        return None

    async def post(self, url, *, headers, json):
        assert url == "https://chat.example/v1/chat/completions"
        assert headers["Authorization"] == "Bearer test-key"
        assert json["model"] == "chat-model"
        return _Response()


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    return TestClient(app)


def _configure_chat_model():
    save_provider(
        {
            "provider_id": "test-chat",
            "display_name": "Test Chat",
            "provider_type": "openai",
            "base_url": "https://chat.example/v1",
            "api_key": "test-key",
            "capabilities": ["chat"],
            "enabled_models": [{"id": "chat-model", "type": "chat"}],
            "is_enabled": True,
        }
    )
    db.set_system_option("system", {"default_model": "test-chat:chat-model"})


def test_chat_provider_resolves_saved_yuxi_model_spec(tmp_path, monkeypatch):
    _client(tmp_path, monkeypatch)
    _configure_chat_model()
    monkeypatch.setattr("app.chat.httpx.AsyncClient", _AsyncClient)

    content = asyncio.run(complete_chat([{"role": "user", "content": "test"}]))

    assert content == "已生成的真实响应"


def test_yuxi_description_uses_configured_default_chat_model(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    _configure_chat_model()
    monkeypatch.setattr("app.chat.httpx.AsyncClient", _AsyncClient)

    response = client.post("/api/knowledge/generate-description", json={"name": "滑坡隐患库"})

    assert response.status_code == 200, response.text
    assert response.json() == {"description": "已生成的真实响应", "status": "success"}


def test_yuxi_sample_questions_generates_and_persists_model_response(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    _configure_chat_model()
    monkeypatch.setattr(
        "app.chat.httpx.AsyncClient",
        lambda *, timeout: _QuestionClient(timeout=timeout),
    )
    kb = client.post("/api/v1/knowledge-bases", json={"name": "滑坡隐患库"}).json()
    db.create_document(
        "应急预案.pdf", None, "application/pdf", "pymupdf-layout", "general", {},
        {"file_size": 100}, knowledge_base_id=kb["id"],
    )

    response = client.post(
        f"/api/knowledge/databases/{kb['id']}/sample-questions", json={"count": 2}
    )

    assert response.status_code == 200, response.text
    assert response.json()["questions"] == ["降雨量达到多少需要撤离？", "如何复核坡体裂缝？"]
    assert client.get(f"/api/knowledge/databases/{kb['id']}/sample-questions").json()["questions"] == response.json()["questions"]


class _QuestionResponse(_Response):
    def json(self):
        return {"choices": [{"message": {"content": '{"questions":["降雨量达到多少需要撤离？","如何复核坡体裂缝？"]}'}}]}


class _QuestionClient(_AsyncClient):
    async def post(self, url, *, headers, json):
        assert json["model"] == "chat-model"
        return _QuestionResponse()


def test_chat_generation_reports_missing_key_as_unavailable(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    save_provider(
        {
            "provider_id": "test-chat",
            "display_name": "Test Chat",
            "provider_type": "openai",
            "base_url": "https://chat.example/v1",
            "capabilities": ["chat"],
            "enabled_models": [{"id": "chat-model", "type": "chat"}],
            "is_enabled": True,
        }
    )
    db.set_system_option("system", {"default_model": "test-chat:chat-model"})

    response = client.post("/api/knowledge/generate-description", json={"name": "滑坡隐患库"})

    assert response.status_code == 503
    assert "API Key" in response.json()["detail"]
