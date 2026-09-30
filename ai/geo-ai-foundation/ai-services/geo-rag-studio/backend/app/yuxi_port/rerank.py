"""Rerank wire protocols adapted from Yuxi's model adapter."""

import math

import httpx

from ..config import settings


def _value(config: dict | None, key: str, fallback):
    configured = (config or {}).get(key)
    return configured if configured not in (None, "") else fallback


def enabled(config: dict | None = None) -> bool:
    return bool(
        _value(config, "base_url", settings.rerank_base_url)
        and _value(config, "api_key", settings.rerank_api_key)
        and _value(config, "model", settings.rerank_model)
    )


def _payload(query: str, documents: list[str], protocol: str, model: str) -> dict:
    if protocol.lower() == "dashscope":
        return {
            "model": model,
            "input": {"query": query, "documents": documents},
            "parameters": {"top_n": len(documents), "return_documents": False},
        }
    return {
        "model": model,
        "query": query,
        "documents": documents,
        "top_n": len(documents),
    }


def _results(data: dict, protocol: str) -> list[dict]:
    if protocol.lower() == "dashscope":
        return list(data.get("output", {}).get("results", []))
    return list(data.get("results", []))


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
    if not documents or not enabled(config):
        return None
    base_url = _value(config, "base_url", settings.rerank_base_url)
    api_key = _value(config, "api_key", settings.rerank_api_key)
    model = _value(config, "model", settings.rerank_model)
    protocol = _value(config, "protocol", settings.rerank_protocol)
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    async with httpx.AsyncClient(timeout=45) as client:
        response = await client.post(base_url, headers=headers, json=_payload(query, documents, protocol, model))
        response.raise_for_status()
    return _scores_in_input_order(_results(response.json(), protocol), len(documents))
