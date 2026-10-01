"""Import shim for Yuxi's optional Redis-backed model cache.

Geo RAG Studio resolves model credentials from its SQLite provider registry and
does not start Redis. Calling Yuxi's Redis cache directly therefore fails clearly.
"""

from contextlib import contextmanager


@contextmanager
def sync_redis_client():
    raise RuntimeError("Yuxi Redis model cache is not configured; use the Geo RAG SQLite provider registry")
    yield None


async def get_async_redis_client():
    raise RuntimeError("Yuxi Redis cache is not configured; use the Geo RAG SQLite provider registry")
