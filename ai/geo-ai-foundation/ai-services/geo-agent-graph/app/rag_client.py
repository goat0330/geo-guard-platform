import httpx

from .config import settings


async def retrieve(query: str, top_k: int | None = None) -> dict:
    payload = {
        "query": query,
        "search_mode": "hybrid",
        "final_top_k": top_k or settings.rag_top_k,
        "recall_top_k": max(settings.rag_top_k, top_k or settings.rag_top_k),
    }
    async with httpx.AsyncClient(timeout=60) as client:
        response = await client.post(f"{settings.rag_base_url.rstrip('/')}/api/v1/retrieve", json=payload)
        response.raise_for_status()
        return response.json()
