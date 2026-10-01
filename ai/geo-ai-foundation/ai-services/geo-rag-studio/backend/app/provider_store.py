"""SQLite registry compatible with Yuxi's model provider contracts."""

import importlib.util
import json
import os
import re
from functools import lru_cache
from pathlib import Path
from typing import Any

from . import db

_PROVIDER_ID = re.compile(r"^[a-zA-Z0-9][a-zA-Z0-9_-]{1,99}$")
_MODEL_TYPES = {"chat", "embedding", "rerank"}
_PROVIDER_TYPES = {"openai", "anthropic", "gemini", "openrouter"}
_CAPABILITIES = _MODEL_TYPES
_BUILTIN_FILE = (
    Path(__file__).resolve().parents[1]
    / "vendor"
    / "yuxi"
    / "package"
    / "yuxi"
    / "models"
    / "providers"
    / "builtin.py"
)


@lru_cache(maxsize=1)
def _builtin_providers() -> list[dict[str, Any]]:
    spec = importlib.util.spec_from_file_location("geo_yuxi_builtin_providers", _BUILTIN_FILE)
    if spec is None or spec.loader is None:
        raise RuntimeError("Vendored Yuxi provider definitions are unavailable")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.BUILTIN_PROVIDERS


def _get_payload(provider_id: str) -> dict[str, Any] | None:
    with db.connect() as connection:
        row = connection.execute(
            "SELECT payload_json FROM model_providers WHERE provider_id=?", (provider_id,)
        ).fetchone()
    return json.loads(row["payload_json"]) if row else None


def ensure_builtin_providers() -> None:
    """Insert the upstream provider templates once without overwriting user settings."""
    with db.connect() as connection:
        seeded = connection.execute(
            "SELECT 1 FROM model_provider_meta WHERE key='yuxi_builtin_providers_seeded'"
        ).fetchone()
        if seeded:
            return
        for definition in _builtin_providers():
            provider_id = definition["provider_id"]
            existing = connection.execute(
                "SELECT 1 FROM model_providers WHERE provider_id=?", (provider_id,)
            ).fetchone()
            if existing:
                continue
            payload = {
                **definition,
                "capabilities": definition.get("capabilities") or ["chat"],
                "enabled_models": definition.get("enabled_models") or [],
                "headers_json": definition.get("headers_json") or {},
                "extra_json": definition.get("extra_json") or {},
                "is_enabled": provider_id == "siliconflow-cn",
                "is_builtin": True,
            }
            timestamp = db.now_iso()
            connection.execute(
                "INSERT INTO model_providers(provider_id,payload_json,created_at,updated_at) VALUES(?,?,?,?)",
                (provider_id, json.dumps(payload, ensure_ascii=False), timestamp, timestamp),
            )
        connection.execute(
            "INSERT INTO model_provider_meta(key,value) VALUES('yuxi_builtin_providers_seeded','true')"
        )


def list_providers() -> list[dict[str, Any]]:
    ensure_builtin_providers()
    with db.connect() as connection:
        rows = connection.execute("SELECT payload_json FROM model_providers").fetchall()
    providers = [public_provider(json.loads(row["payload_json"])) for row in rows]
    return sorted(providers, key=lambda item: (not item.get("is_enabled", False), item["provider_id"]))


def get_provider(provider_id: str) -> dict[str, Any] | None:
    ensure_builtin_providers()
    return _get_payload(provider_id)


def save_provider(payload: dict[str, Any], provider_id: str | None = None) -> dict[str, Any]:
    ensure_builtin_providers()
    existing = get_provider(provider_id) if provider_id else None
    if provider_id and existing is None:
        raise KeyError(provider_id)
    merged = {**(existing or {}), **payload}
    if existing and payload.get("api_key") in (None, ""):
        merged["api_key"] = existing.get("api_key")
    normalized = normalize_provider(merged)
    if provider_id and normalized["provider_id"] != provider_id:
        raise ValueError("provider_id 不可修改")
    timestamp = db.now_iso()
    with db.connect() as connection:
        if existing:
            connection.execute(
                "UPDATE model_providers SET payload_json=?,updated_at=? WHERE provider_id=?",
                (json.dumps(normalized, ensure_ascii=False), timestamp, provider_id),
            )
        else:
            connection.execute(
                "INSERT INTO model_providers(provider_id,payload_json,created_at,updated_at) VALUES(?,?,?,?)",
                (normalized["provider_id"], json.dumps(normalized, ensure_ascii=False), timestamp, timestamp),
            )
    return public_provider(normalized)


def delete_provider(provider_id: str) -> bool:
    ensure_builtin_providers()
    with db.connect() as connection:
        cursor = connection.execute("DELETE FROM model_providers WHERE provider_id=?", (provider_id,))
    return cursor.rowcount > 0


def normalize_provider(payload: dict[str, Any]) -> dict[str, Any]:
    data = dict(payload)
    provider_id = str(data.get("provider_id") or "").strip()
    if not _PROVIDER_ID.fullmatch(provider_id):
        raise ValueError("provider_id 只能包含字母、数字、下划线和中划线，长度 2-100")
    display_name = str(data.get("display_name") or "").strip()
    base_url = str(data.get("base_url") or "").strip()
    if not display_name or not base_url:
        raise ValueError("display_name 和 base_url 不能为空")
    provider_type = data.get("provider_type") or "openai"
    if provider_type not in _PROVIDER_TYPES:
        raise ValueError(f"provider_type 必须是 {', '.join(sorted(_PROVIDER_TYPES))} 之一")
    capabilities = data.get("capabilities") or []
    if not isinstance(capabilities, list) or set(capabilities) - _CAPABILITIES:
        raise ValueError("capabilities 只能包含 chat、embedding、rerank")
    enabled_models = data.get("enabled_models") or []
    if not isinstance(enabled_models, list):
        raise ValueError("enabled_models 必须是对象列表")
    normalized_models = []
    seen = set()
    for item in enabled_models:
        if not isinstance(item, dict):
            raise ValueError("enabled_models 必须是对象列表")
        model = dict(item)
        model_id = str(model.get("id") or "").strip()
        model_type = str(model.get("type") or "unknown").strip()
        if not model_id or model_type not in _MODEL_TYPES:
            raise ValueError("每个模型必须包含 id，type 必须是 chat、embedding 或 rerank")
        if model_id in seen:
            raise ValueError(f"模型 id 重复: {model_id}")
        if capabilities and model_type not in capabilities:
            raise ValueError(f"模型 {model_id} 的 type={model_type} 不在 provider 能力 {sorted(capabilities)} 内")
        seen.add(model_id)
        model["id"] = model_id
        model["type"] = model_type
        model["source"] = model.get("source") if model.get("source") in {"manual", "remote"} else "manual"
        model["display_name"] = str(model.get("display_name") or model_id)
        model["extra"] = model.get("extra") if isinstance(model.get("extra"), dict) else {}
        overrides = model.get("request_body_overrides") or {}
        allowed_overrides = {"enable_thinking", "reasoning", "reasoning_effort", "thinking", "thinking_budget"}
        if not isinstance(overrides, dict) or set(overrides) - allowed_overrides:
            raise ValueError(f"模型 {model_id} 的 request_body_overrides 包含不支持的字段")
        if overrides and (provider_type not in {"openai", "openrouter"} or model_type != "chat"):
            raise ValueError("request_body_overrides 仅支持 OpenAI 兼容供应商的 chat 模型")
        if model_type == "embedding":
            for key in ("dimension", "batch_size"):
                if model.get(key) not in (None, ""):
                    model[key] = int(model[key])
        normalized_models.append(model)
    data.update(
        provider_id=provider_id,
        display_name=display_name,
        provider_type=provider_type,
        base_url=base_url,
        capabilities=capabilities,
        enabled_models=normalized_models,
        headers_json=data.get("headers_json") if isinstance(data.get("headers_json"), dict) else {},
        extra_json=data.get("extra_json") if isinstance(data.get("extra_json"), dict) else {},
        is_enabled=bool(data.get("is_enabled", True)),
        is_builtin=bool(data.get("is_builtin", False)),
    )
    return data


def public_provider(provider: dict[str, Any]) -> dict[str, Any]:
    result = dict(provider)
    env_key = os.getenv(str(result.get("api_key_env") or "")) if result.get("api_key_env") else None
    has_key = bool(result.get("api_key") or env_key)
    result["api_key_set"] = has_key
    result["credential_status"] = "ok" if not result.get("is_enabled") or has_key else "warning"
    result["api_key"] = ""
    return result


def resolve_model_spec(spec: str, model_type: str) -> dict[str, Any] | None:
    if ":" not in (spec or ""):
        return None
    provider_id, model_id = spec.split(":", 1)
    provider = get_provider(provider_id)
    if not provider or not provider.get("is_enabled"):
        return None
    model = next(
        (
            item
            for item in provider.get("enabled_models", [])
            if item.get("id") == model_id and item.get("type") == model_type and item.get("enabled", True)
        ),
        None,
    )
    if not model:
        return None
    base_url_key = {"embedding": "embedding_base_url", "rerank": "rerank_base_url"}.get(model_type)
    base_url = model.get("base_url_override") or provider.get(base_url_key) or provider.get("base_url")
    api_key = provider.get("api_key") or (os.getenv(provider.get("api_key_env", "")) if provider.get("api_key_env") else "")
    extra = dict(provider.get("extra_json") or {})
    extra.update(model.get("extra") or {})
    return {
        "model": model_id,
        "base_url": base_url,
        "api_key": api_key or "",
        "dimensions": model.get("dimension"),
        "batch_size": model.get("batch_size", 40),
        "protocol": extra.get("rerank_protocol") or extra.get("protocol") or provider.get("default_protocol") or "openai",
        "headers": provider.get("headers_json") or {},
        "provider": provider.get("provider_id"),
        "provider_type": provider.get("provider_type"),
    }


def resolve_runtime_config(config: dict | None, model_type: str) -> dict:
    config = dict(config or {})
    spec = str(config.get("model") or "")
    resolved = resolve_model_spec(spec, model_type)
    if not resolved:
        return config
    # Yuxi model selectors persist provider_id:model_id; the provider record supplies
    # the endpoint and credentials while the pipeline sends the upstream model id.
    for key, value in config.items():
        if key in {"model", "base_url", "api_key"} or value in (None, ""):
            continue
        resolved[key] = value
    resolved["model"] = spec.split(":", 1)[1]
    resolved["base_url"] = config.get("base_url") or resolved["base_url"]
    resolved["api_key"] = config.get("api_key") or resolved["api_key"]
    return resolved
