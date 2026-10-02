from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from app.provider_store import resolve_runtime_config


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setenv("SILICONFLOW_API_KEY", "")
    return TestClient(app)


def test_yuxi_builtin_embedding_selector_resolves_upstream_spec(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)

    providers = client.get("/api/system/model-providers")
    assert providers.status_code == 200
    siliconflow = next(item for item in providers.json()["data"] if item["provider_id"] == "siliconflow-cn")
    assert siliconflow["is_enabled"] is True
    assert siliconflow["credential_status"] == "warning"
    assert siliconflow["api_key"] == ""

    models = client.get("/api/system/model-providers/models/v2?model_type=embedding")
    assert models.status_code == 200
    spec = "siliconflow-cn:BAAI/bge-m3"
    model = next(item for item in models.json()["data"]["siliconflow-cn"]["models"] if item["spec"] == spec)
    assert model["dimension"] == 1024

    runtime = resolve_runtime_config({"model": spec}, "embedding")
    assert runtime["model"] == "BAAI/bge-m3"
    assert runtime["base_url"] == "https://api.siliconflow.cn/v1/embeddings"
    assert runtime["dimensions"] == 1024
    assert runtime["api_key"] == ""
    monkeypatch.setattr(settings, "embedding_api_key", "unit-global-secret")
    from app.embedding import enabled as embedding_enabled
    assert embedding_enabled({"model": spec}) is False

    reranker_spec = "siliconflow-cn:Pro/BAAI/bge-reranker-v2-m3"
    reranker = resolve_runtime_config({"model": reranker_spec}, "rerank")
    assert reranker["model"] == "Pro/BAAI/bge-reranker-v2-m3"
    assert reranker["base_url"] == "https://api.siliconflow.cn/v1/rerank"
    assert reranker["api_key"] == ""
    monkeypatch.setattr(settings, "rerank_api_key", "unit-global-secret")
    from app.yuxi_port.rerank import enabled as reranker_enabled
    assert reranker_enabled({"model": reranker_spec}) is False

    status = client.get("/api/system/model-providers/models/status", params={"spec": spec})
    assert status.status_code == 200
    assert status.json()["status"] == "error"
    assert status.json()["message"] == "Provider API Key 未配置"


def test_provider_api_key_is_persisted_but_never_returned(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    secret = "provider-test-secret"
    payload = {
        "provider_id": "custom-embed",
        "display_name": "Custom Embedding",
        "provider_type": "openai",
        "base_url": "https://models.example/v1",
        "embedding_base_url": "https://models.example/v1/embeddings",
        "api_key": secret,
        "capabilities": ["embedding"],
        "enabled_models": [{"id": "embed-v1", "type": "embedding", "dimension": 8}],
        "is_enabled": True,
    }

    created = client.post("/api/system/model-providers", json=payload)
    assert created.status_code == 200, created.text
    assert created.json()["data"]["api_key"] == ""
    assert created.json()["data"]["api_key_set"] is True
    assert secret not in created.text

    listed = client.get("/api/system/model-providers").json()["data"]
    custom = next(item for item in listed if item["provider_id"] == "custom-embed")
    assert custom["api_key"] == ""
    assert custom["api_key_set"] is True

    runtime = resolve_runtime_config({"model": "custom-embed:embed-v1"}, "embedding")
    assert runtime["model"] == "embed-v1"
    assert runtime["base_url"] == "https://models.example/v1/embeddings"
    assert runtime["api_key"] == secret
