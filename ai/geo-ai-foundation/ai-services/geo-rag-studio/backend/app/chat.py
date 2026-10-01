"""OpenAI-compatible chat access through the persisted Yuxi model registry."""

import httpx

from . import db
from .provider_store import resolve_runtime_config


class ChatProviderUnavailable(RuntimeError):
    pass


def _default_model_spec() -> str:
    system = db.get_system_option("system", {}) or {}
    return str(system.get("default_model") or "siliconflow-cn:deepseek-ai/DeepSeek-V4-Flash")


async def complete_chat(messages: list[dict], model_spec: str | None = None, *, timeout: float = 60) -> str:
    selected = str(model_spec or _default_model_spec()).strip()
    config = resolve_runtime_config({"model": selected}, "chat")
    if not config.get("api_key") or not config.get("base_url") or not config.get("model"):
        raise ChatProviderUnavailable("默认 Chat 模型不可用；请配置并启用 OpenAI-Compatible Provider 的 API Key")
    if config.get("provider_type") not in {"openai", "openrouter"}:
        raise ChatProviderUnavailable("当前 Chat 调用仅支持 OpenAI-Compatible Provider")

    base_url = str(config["base_url"]).rstrip("/")
    endpoint = base_url if base_url.endswith("/chat/completions") else f"{base_url}/chat/completions"
    headers = dict(config.get("headers") or {})
    headers.setdefault("Authorization", f"Bearer {config['api_key']}")
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.post(
            endpoint,
            headers=headers,
            json={
                "model": config["model"],
                "messages": messages,
                "temperature": 0,
            },
        )
        response.raise_for_status()
    choices = response.json().get("choices") or []
    if not choices:
        raise ValueError("Chat 响应缺少 choices")
    content = (choices[0].get("message") or {}).get("content")
    if not isinstance(content, str) or not content.strip():
        raise ValueError("Chat 响应缺少文本内容")
    return content.strip()
