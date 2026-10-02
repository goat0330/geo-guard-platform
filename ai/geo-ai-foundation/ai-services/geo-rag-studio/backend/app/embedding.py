import math

from .config import settings
from .provider_store import resolve_runtime_config
from .yuxi_port import _upstream  # noqa: F401
from yuxi.models.embed import OtherEmbedding


def value(config: dict | None, key: str, fallback):
    return config[key] if config is not None and key in config else fallback


def enabled(config: dict | None = None) -> bool:
    config = resolve_runtime_config(config, "embedding")
    return bool(
        value(config, "base_url", settings.embedding_base_url)
        and value(config, "api_key", settings.embedding_api_key)
        and value(config, "model", settings.embedding_model)
    )


def _embedding_url(config: dict | None = None) -> str:
    base = value(config, "base_url", settings.embedding_base_url).rstrip("/")
    return base if base.endswith("/embeddings") else f"{base}/embeddings"


def _validated_vectors(vectors: list, expected_count: int, expected_dimensions: int | None = None) -> list[list[float]]:
    if len(vectors) != expected_count:
        raise ValueError(f"Embedding returned {len(vectors)} vectors for {expected_count} inputs")
    if not vectors:
        return []

    normalized: list[list[float]] = []
    for vector in vectors:
        if not isinstance(vector, list) or not vector:
            raise ValueError("Embedding response contains an empty vector")
        try:
            values = [float(value) for value in vector]
        except (TypeError, ValueError) as exc:
            raise ValueError("Embedding response contains a non-numeric vector") from exc
        if not all(math.isfinite(value) for value in values):
            raise ValueError("Embedding response contains a non-finite vector value")
        normalized.append(values)

    dimensions = {len(vector) for vector in normalized}
    if len(dimensions) != 1:
        raise ValueError("Embedding response vectors have inconsistent dimensions")
    if expected_dimensions and dimensions != {expected_dimensions}:
        raise ValueError(f"Embedding returned dimensions {next(iter(dimensions))}; expected {expected_dimensions}")
    return normalized


def _vectors_in_input_order(data: list[dict], expected_count: int) -> list[list[float]]:
    if len(data) != expected_count:
        raise ValueError(f"Embedding returned {len(data)} vectors for {expected_count} inputs")
    vectors: list[list[float] | None] = [None] * expected_count
    for item in data:
        if not isinstance(item, dict):
            raise ValueError("Embedding response contains an invalid item")
        index = item.get("index")
        if type(index) is not int or not 0 <= index < expected_count or vectors[index] is not None:
            raise ValueError("Embedding response contains an invalid or duplicate index")
        vectors[index] = item.get("embedding")
    if any(vector is None for vector in vectors):
        raise ValueError("Embedding response omitted one or more input indexes")
    return _validated_vectors(vectors, expected_count)


class _YuxiEmbedding(OtherEmbedding):
    """Keep Yuxi's request/retry behavior and validate its response at the local API boundary."""

    @staticmethod
    def _extract_embeddings(result: dict) -> list[list[float]]:
        if not isinstance(result, dict) or not isinstance(result.get("data"), list):
            raise ValueError("Embedding response has an invalid format")
        data = result["data"]
        has_index = [isinstance(item, dict) and "index" in item for item in data]
        if any(has_index):
            if not all(has_index):
                raise ValueError("Embedding response contains a partial set of indexes")
            return _vectors_in_input_order(data, len(data))
        if any(not isinstance(item, dict) for item in data):
            raise ValueError("Embedding response contains an invalid item")
        return _validated_vectors([item.get("embedding") for item in data], len(data))


def _model(config: dict | None = None) -> tuple[_YuxiEmbedding, int | None]:
    config = resolve_runtime_config(config, "embedding")
    dimensions = value(config, "dimensions", settings.embedding_dimensions)
    expected_dimensions = int(dimensions) if dimensions not in (None, "") else None
    model = _YuxiEmbedding(
        model=value(config, "model", settings.embedding_model),
        base_url=_embedding_url(config),
        api_key=value(config, "api_key", settings.embedding_api_key),
        dimension=expected_dimensions,
        batch_size=int(value(config, "batch_size", settings.embedding_batch_size)),
    )
    model.headers.update(config.get("headers") or {})
    return model, expected_dimensions


async def embed_texts(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    config = resolve_runtime_config(config, "embedding")
    if not texts or not enabled(config):
        return None
    model, expected_dimensions = _model(config)
    vectors = await model.aencode(texts)
    return _validated_vectors(vectors, len(texts), expected_dimensions)


async def embed_batched(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    config = resolve_runtime_config(config, "embedding")
    if not texts or not enabled(config):
        return None
    model, expected_dimensions = _model(config)
    vectors = await model.abatch_encode(texts)
    return _validated_vectors(vectors, len(texts), expected_dimensions)


def embed_texts_sync(texts: list[str], config: dict | None = None) -> list[list[float]] | None:
    config = resolve_runtime_config(config, "embedding")
    if not texts or not enabled(config):
        return None
    model, expected_dimensions = _model(config)
    vectors = model.batch_encode(texts)
    return _validated_vectors(vectors, len(texts), expected_dimensions)
