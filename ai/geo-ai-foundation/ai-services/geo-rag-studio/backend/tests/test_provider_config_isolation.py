import asyncio

import pytest
from fastapi.testclient import TestClient

from app import embedding, main
from app.config import settings
from app.provider_store import resolve_runtime_config
from app.yuxi_port import rerank as rerank_module


@pytest.mark.parametrize("model_type,module,prefix", [
    ("embedding", embedding, "embedding"),
    ("rerank", rerank_module, "rerank"),
])
def test_scoped_provider_does_not_inherit_global_identity(monkeypatch, model_type, module, prefix):
    monkeypatch.setattr(settings, f"{prefix}_base_url", "https://global.example/v1")
    monkeypatch.setattr(settings, f"{prefix}_api_key", "unit-global-secret")
    monkeypatch.setattr(settings, f"{prefix}_model", "global-model")
    assert module.enabled() is True
    assert module.enabled({}) is True
    assert module.enabled({"base_url": "", "model": "", "api_key": "stored-unit-placeholder"}) is False
    assert module.enabled({"base_url": "https://kb.example/v1", "model": "kb-model"}) is False
    runtime = resolve_runtime_config({"base_url": "https://kb.example/v1", "model": "kb-model"}, model_type)
    assert runtime["api_key"] == ""


def test_explicit_unset_dimensions_do_not_inherit_global_dimensions(monkeypatch):
    monkeypatch.setattr(settings, "embedding_dimensions", 1024)
    assert embedding.value({"dimensions": None}, "dimensions", settings.embedding_dimensions) is None


def test_provider_probes_distinguish_omitted_and_explicit_empty_config(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    for prefix in ("embedding", "rerank"):
        monkeypatch.setattr(settings, f"{prefix}_base_url", "https://global.example/v1")
        monkeypatch.setattr(settings, f"{prefix}_api_key", "unit-global-secret")
        monkeypatch.setattr(settings, f"{prefix}_model", "global-model")

    async def embed_probe(texts, config):
        assert config == {}
        return [[1.0, 0.0]]

    async def rerank_probe(query, documents, config):
        assert config == {}
        return [0.75] * len(documents)

    monkeypatch.setattr(main, "embed_texts", embed_probe)
    monkeypatch.setattr(main, "rerank", rerank_probe)
    with TestClient(main.app) as client:
        embedded = client.post("/api/v1/providers/embedding/test", json={})
        assert embedded.status_code == 200
        assert embedded.json()["model"] == "global-model"
        reranked = client.post("/api/v1/providers/reranker/test", json={"query": "rainfall", "documents": ["slope"]})
        assert reranked.status_code == 200
        assert reranked.json()["model"] == "global-model"
        assert client.post("/api/v1/providers/embedding/test", json={"base_url": "", "model": ""}).status_code == 503
        assert client.post("/api/v1/providers/reranker/test", json={"query": "rainfall", "documents": ["slope"], "base_url": "", "model": ""}).status_code == 503


def test_disabled_scoped_provider_skips_model_requests(monkeypatch):
    monkeypatch.setattr(settings, "embedding_base_url", "https://global.example/v1")
    monkeypatch.setattr(settings, "embedding_model", "global-model")
    monkeypatch.setattr(settings, "embedding_api_key", "unit-global-secret")
    def unexpected_model(config):
        pytest.fail("A disabled scoped provider must not construct a model client")

    monkeypatch.setattr(embedding, "_model", unexpected_model)
    assert asyncio.run(embedding.embed_texts(["slope"], {"base_url": "", "model": ""})) is None


def test_registered_provider_probes_report_runtime_model_and_protocol(tmp_path, monkeypatch):
    from app.provider_store import save_provider

    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    with TestClient(main.app) as client:
        save_provider({"provider_id": "unit-probe", "display_name": "Unit probe", "base_url": "https://unit.example/v1",
                       "api_key": "unit-placeholder", "capabilities": ["embedding", "rerank"],
                       "extra_json": {"rerank_protocol": "dashscope"}, "enabled_models": [
                           {"id": "embed-unit", "type": "embedding", "dimension": 2},
                           {"id": "rank-unit", "type": "rerank"}]})

        async def embed_probe(texts, config):
            assert config["model"] == "embed-unit"
            return [[1.0, 0.0]]

        async def rank_probe(query, documents, config):
            assert config["model"] == "rank-unit"
            assert config["protocol"] == "dashscope"
            return [0.75] * len(documents)

        monkeypatch.setattr(main, "embed_texts", embed_probe)
        monkeypatch.setattr(main, "rerank", rank_probe)
        embedded = client.post("/api/v1/providers/embedding/test", json={"model": "unit-probe:embed-unit"})
        assert embedded.status_code == 200
        assert embedded.json()["model"] == "embed-unit"
        ranked = client.post("/api/v1/providers/reranker/test", json={"query": "rainfall", "documents": ["slope"], "model": "unit-probe:rank-unit"})
        assert ranked.status_code == 200
        assert ranked.json()["model"] == "rank-unit"
        assert ranked.json()["provider"] == "dashscope"
