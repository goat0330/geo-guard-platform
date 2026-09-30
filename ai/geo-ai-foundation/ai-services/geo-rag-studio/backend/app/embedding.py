import math

import httpx

from .config import settings


def value(config: dict | None, key: str, fallback):
    configured = (config or {}).get(key)
    return configured if configured not in (None, "") else fallback


def enabled(config: dict | None = None) -> bool:
    return bool(
        value(config, "base_url", settings.embedding_base_url)
        and value(config, "api_key", settings.embedding_api_key)
        and value(config, "model", settings.embedding_model)
    )


def _embedding_url(config: dict | None = None) -> str:
    base = value(config, "base_url", settings.embedding_base_url).rstrip("/")
    return base if base.endswith("/embeddings") else f"{base}/embeddings"


def _vectors_in_input_order(data: list[dict], expected_count: int) -> list[list[float]]:
    if len(data) != expected_count:
        raise ValueError(f"Embedding returned {len(data)} vectors for {expected_count} inputs")
    vectors: list[list[float] | None] = [None] * expected_count
    for item in data:
        index = item.get("index")
        if type(index) is not int or not 0 <= index < expected_count or vectors[index] is not None:
            raise ValueError("Embedding response contains an invalid or duplicate index")
        vector = item.get("embedding")
        if not isinstance(vector, list) or not vector:
            raise ValueError("Embedding response contains an empty vector")
        try:
            values = [float(value) for value in vector]
        except (TypeError, ValueError) as exc:
            raise ValueError("Embedding response contains a non-numeric vector") from exc
        if not all(math.isfinite(value) for value in values):
            raise ValueError("Embedding response contains a non-finite vector value")
        vectors[index] = values
    if any(vector is None for vector in vectors):
        raise ValueError("Embedding response omitted one or more input indexes")
    dimensions = {len(vector) for vector in vectors if vector is not None}
    if len(dimensions) != 1:
        raise ValueError("Embedding response vectors have inconsistent dimensions")
    return [vector for vector in vectors if vector is not None]


async def embed_texts(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    if not texts or not enabled(config):
        return None
    model = value(config, "model", settings.embedding_model)
    dimensions = value(config, "dimensions", settings.embedding_dimensions)
    api_key = value(config, "api_key", settings.embedding_api_key)
    payload = {"model": model, "input": texts}
    if dimensions:
        payload["dimensions"] = dimensions
    headers = {"Authorization": f"Bearer {api_key}"}
    async with httpx.AsyncClient(timeout=90) as client:
        response = await client.post(_embedding_url(config), headers=headers, json=payload)
        response.raise_for_status()
    return _vectors_in_input_order(response.json().get("data", []), len(texts))


async def embed_batched(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    if not texts or not enabled(config):
        return None
    vectors = []
    batch_size = max(1, min(int(value(config, "batch_size", settings.embedding_batch_size)), 128))
    for start in range(0, len(texts), batch_size):
        result = await embed_texts(texts[start : start + batch_size], config)
        if result is None:
            return None
        vectors.extend(result)
    if len(vectors) != len(texts):
        raise ValueError("Embedding batch count does not match input count")
    return vectors


def embed_texts_sync(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    if not texts or not enabled(config):
        return None
    model = value(config, "model", settings.embedding_model)
    dimensions = value(config, "dimensions", settings.embedding_dimensions)
    api_key = value(config, "api_key", settings.embedding_api_key)
    batch_size = max(1, min(int(value(config, "batch_size", settings.embedding_batch_size)), 128))
    vectors: list[list[float]] = []
    with httpx.Client(timeout=90) as client:
        for start in range(0, len(texts), batch_size):
            batch = texts[start : start + batch_size]
            payload = {"model": model, "input": batch}
            if dimensions:
                payload["dimensions"] = dimensions
            response = client.post(
                _embedding_url(config),
                headers={"Authorization": f"Bearer {api_key}"},
                json=payload,
            )
            response.raise_for_status()
            vectors.extend(_vectors_in_input_order(response.json().get("data", []), len(batch)))
    if len(vectors) != len(texts):
        raise ValueError("Embedding batch count does not match input count")
    return vectors
