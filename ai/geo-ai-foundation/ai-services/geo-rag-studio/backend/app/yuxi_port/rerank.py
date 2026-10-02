"""SQLite-configured adapter over Yuxi's upstream reranker implementations."""

import math

import httpx

from . import _upstream  # noqa: F401
from ..config import settings
from ..provider_store import resolve_runtime_config
from yuxi.models.rerank import DashscopeReranker, OpenAIReranker, sigmoid


def _value(config: dict | None, key: str, fallback):
    return config[key] if config is not None and key in config else fallback


def enabled(config: dict | None = None) -> bool:
    config = resolve_runtime_config(config, "rerank")
    return bool(
        _value(config, "base_url", settings.rerank_base_url)
        and _value(config, "api_key", settings.rerank_api_key)
        and _value(config, "model", settings.rerank_model)
    )


def _scores_in_input_order(results: list[dict], document_count: int) -> list[float]:
    if len(results) != document_count:
        raise ValueError(
            f"Rerank returned {len(results)} scores for {document_count} documents"
        )
    scores: list[float | None] = [None] * document_count
    for result in results:
        index = result.get("index")
        if type(index) is not int or not 0 <= index < document_count or scores[index] is not None:
            raise ValueError("Rerank response contains an invalid or duplicate index")
        value = result.get("relevance_score")
        try:
            score = float(value)
        except (TypeError, ValueError) as exc:
            raise ValueError("Rerank response contains a non-numeric score") from exc
        if not math.isfinite(score):
            raise ValueError("Rerank response contains a non-finite score")
        scores[index] = score
    if any(score is None for score in scores):
        raise ValueError("Rerank response omitted one or more input indexes")
    return [float(score) for score in scores]


async def rerank(query: str, documents: list[str], config: dict | None = None) -> list[float] | None:
    config = resolve_runtime_config(config, "rerank")
    if not documents or not enabled(config):
        return None
    protocol = _value(config, "protocol", settings.rerank_protocol)
    reranker_class = DashscopeReranker if protocol.lower() == "dashscope" else OpenAIReranker
    reranker = reranker_class(
        model_name=_value(config, "model", settings.rerank_model),
        api_key=_value(config, "api_key", settings.rerank_api_key),
        base_url=_value(config, "base_url", settings.rerank_base_url),
        parameters=config.get("parameters") or {},
    )
    headers = {**reranker.headers, **(config.get("headers") or {})}
    all_scores = []
    async with httpx.AsyncClient(timeout=30) as client:
        for start in range(0, len(documents), 32):
            batch = documents[start : start + 32]
            payload = reranker._build_payload(query, batch, max_length=int(config.get("max_length", 512)))
            response = await client.post(reranker.url, headers=headers, json=payload)
            response.raise_for_status()
            raw_scores = _scores_in_input_order(reranker._extract_results(response.json()), len(batch))
            all_scores.extend(float(sigmoid(score)) for score in raw_scores)
    if len(all_scores) != len(documents):
        raise ValueError(f"Rerank returned {len(all_scores)} scores for {len(documents)} documents")
    return all_scores
