"""HTTP contract used by Yuxi's provider settings and model selectors."""

import time
from typing import Any
from urllib.parse import urlsplit

import httpx
from fastapi import APIRouter, Body, HTTPException, Query

from .provider_store import (
    delete_provider,
    get_provider,
    list_providers,
    public_provider,
    resolve_model_spec,
    resolve_runtime_config,
    save_provider,
)

router = APIRouter(prefix="/api/system/model-providers", tags=["model-providers"])


def _model_rows(model_type: str) -> dict[str, dict[str, Any]]:
    if model_type not in {"chat", "embedding", "rerank"}:
        raise HTTPException(status_code=422, detail="model_type 必须是 chat、embedding 或 rerank")
    grouped: dict[str, dict[str, Any]] = {}
    for provider in list_providers():
        if not provider.get("is_enabled"):
            continue
        models = [
            model
            for model in provider.get("enabled_models", [])
            if model.get("type") == model_type and model.get("enabled", True)
        ]
        if not models:
            continue
        grouped[provider["provider_id"]] = {
            "provider_id": provider["provider_id"],
            "provider_display_name": provider.get("display_name") or provider["provider_id"],
            "models": [
                {
                    "spec": f"{provider['provider_id']}:{model['id']}",
                    "model_id": model["id"],
                    "display_name": model.get("display_name") or model["id"],
                    "dimension": model.get("dimension"),
                    "batch_size": model.get("batch_size", 40),
                }
                for model in models
            ],
        }
    return grouped


@router.get("")
async def get_providers():
    return {"success": True, "data": list_providers()}


@router.get("/models/v2")
async def get_v2_models(model_type: str = Query("chat")):
    return {"success": True, "data": _model_rows(model_type)}


@router.get("/{provider_id}")
async def get_provider_detail(provider_id: str):
    provider = get_provider(provider_id)
    if not provider:
        raise HTTPException(status_code=404, detail=f"供应商 {provider_id} 不存在")
    return {"success": True, "data": public_provider(provider)}


@router.post("/models/cache/refresh")
async def refresh_model_cache():
    # Provider records are read directly from SQLite for each request, so no process cache can go stale.
    model_count = sum(len(item["models"]) for item in _model_rows("chat").values())
    model_count += sum(len(item["models"]) for item in _model_rows("embedding").values())
    model_count += sum(len(item["models"]) for item in _model_rows("rerank").values())
    return {"success": True, "message": "SQLite model registry is current", "model_count": model_count}


@router.get("/models/status")
async def get_model_status(spec: str = Query(...)):
    info = None
    model_type = None
    for kind in ("embedding", "rerank", "chat"):
        info = resolve_model_spec(spec, kind)
        if info:
            model_type = kind
            break
    provider_id = spec.split(":", 1)[0] if ":" in spec else ""
    provider = get_provider(provider_id) if provider_id else None
    model = next(
        (item for item in (provider or {}).get("enabled_models", []) if f"{provider_id}:{item.get('id')}" == spec),
        {},
    )
    if not info or not model_type:
        return {"spec": spec, "status": "error", "message": f"未找到已启用模型: {spec}", "model_type": model_type}
    if not info.get("api_key"):
        return {"spec": spec, "status": "error", "message": "Provider API Key 未配置", "model_type": model_type}
    started = time.perf_counter()
    try:
        config = resolve_runtime_config({"model": spec}, model_type)
        if model_type == "embedding":
            from .embedding import embed_texts

            vectors = await embed_texts(["Yuxi model provider connection test"], config)
            if not vectors:
                raise ValueError("模型未返回 embedding")
            message = f"连接正常，维度 {len(vectors[0])}"
        elif model_type == "rerank":
            from .yuxi_port.rerank import rerank

            scores = await rerank("hazard inspection", ["geological hazard field inspection"], config)
            if scores is None or len(scores) != 1:
                raise ValueError("Reranker 未返回对齐的 relevance score")
            message = "连接正常"
        else:
            base_url = str(config["base_url"]).rstrip("/")
            endpoint = base_url if base_url.endswith("/chat/completions") else f"{base_url}/chat/completions"
            headers = {"Authorization": f"Bearer {config['api_key']}", **(config.get("headers") or {})}
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.post(endpoint, headers=headers, json={
                    "model": config["model"],
                    "messages": [{"role": "user", "content": "Say 1"}],
                    "max_tokens": 4,
                })
                response.raise_for_status()
                if not response.json().get("choices"):
                    raise ValueError("模型响应缺少 choices")
            message = "连接正常"
        return {
            "spec": spec,
            "status": "available",
            "message": message,
            "model_type": model_type,
            "latency_ms": round((time.perf_counter() - started) * 1000, 2),
        }
    except Exception as exc:
        return {
            "spec": spec,
            "status": "error",
            "message": str(exc)[:500],
            "model_type": model_type,
            "latency_ms": round((time.perf_counter() - started) * 1000, 2),
        }


@router.post("")
async def create_provider(payload: dict = Body(...)):
    try:
        return {"success": True, "data": save_provider(payload)}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        if "UNIQUE constraint failed" in str(exc):
            raise HTTPException(status_code=409, detail="provider_id 已存在") from exc
        raise


@router.put("/{provider_id}")
async def update_provider(provider_id: str, payload: dict = Body(...)):
    try:
        return {"success": True, "data": save_provider(payload, provider_id)}
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=f"供应商 {provider_id} 不存在") from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.delete("/{provider_id}")
async def remove_provider(provider_id: str):
    if not delete_provider(provider_id):
        raise HTTPException(status_code=404, detail=f"供应商 {provider_id} 不存在")
    return {"success": True}


def _models_url(base_url: str, endpoint: str | None) -> str:
    if endpoint and urlsplit(endpoint).scheme in {"http", "https"}:
        return endpoint
    return f"{base_url.rstrip('/')}/{str(endpoint or 'models').lstrip('/')}"


@router.get("/{provider_id}/remote-models")
async def get_remote_models(provider_id: str):
    provider = get_provider(provider_id)
    if not provider:
        raise HTTPException(status_code=404, detail=f"供应商 {provider_id} 不存在")
    api_key = provider.get("api_key") or ""
    headers = dict(provider.get("headers_json") or {})
    if api_key:
        headers.setdefault("Authorization", f"Bearer {api_key}")
    endpoints = [(provider.get("models_endpoint"), "chat")]
    if "embedding" in (provider.get("capabilities") or []):
        endpoints.append((provider.get("embedding_models_endpoint"), "embedding"))
    if "rerank" in (provider.get("capabilities") or []) and provider.get("rerank_models_endpoint"):
        endpoints.append((provider.get("rerank_models_endpoint"), "rerank"))
    models = []
    try:
        async with httpx.AsyncClient(timeout=40) as client:
            for endpoint, model_type in endpoints:
                if not endpoint:
                    continue
                response = await client.get(_models_url(provider["base_url"], endpoint), headers=headers)
                response.raise_for_status()
                result = response.json()
                raw_models = result.get("data") if isinstance(result, dict) else result
                if not isinstance(raw_models, list):
                    raise ValueError(f"{endpoint} 响应必须是列表或包含 data 列表")
                for raw in raw_models:
                    if not isinstance(raw, dict) or not str(raw.get("id") or "").strip():
                        continue
                    models.append({
                        **raw,
                        "id": str(raw["id"]).strip(),
                        "type": raw.get("type") if raw.get("type") in {"chat", "embedding", "rerank"} else model_type,
                        "display_name": raw.get("name") or str(raw["id"]).strip(),
                    })
    except httpx.HTTPStatusError as exc:
        status = 502 if exc.response.status_code == 401 else exc.response.status_code
        detail = "远端 API 认证失败，请检查 API Key 配置" if exc.response.status_code == 401 else f"Models 请求失败: {exc.response.text[:300]}"
        raise HTTPException(status_code=status, detail=detail) from exc
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"拉取远端模型失败: {exc}") from exc
    return {"success": True, "data": models}
