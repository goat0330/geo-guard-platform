"""Score transforms used by Yuxi's Milvus 2.5.6 WeightedRanker path."""

import math


def activate_score(metric_type: str, score: float) -> float:
    """Port Milvus v2.5.6 weightedScorer activation for Yuxi's COSINE and BM25 routes."""
    metric = metric_type.upper()
    if metric == "COSINE":
        return (1.0 + score) * 0.5
    if metric == "IP":
        return 0.5 + math.atan(score) / math.pi
    if metric == "BM25":
        return 2.0 * math.atan(score) / math.pi
    return 1.0 - 2.0 * math.atan(score) / math.pi


def weighted_hybrid_score(
    *,
    vector_score: float | None,
    bm25_score: float | None,
    vector_weight: float,
    bm25_weight: float,
) -> float:
    """Weighted sum of activated route scores; a missing route contributes zero."""
    score = 0.0
    if vector_score is not None:
        score += vector_weight * activate_score("COSINE", vector_score)
    if bm25_score is not None:
        score += bm25_weight * activate_score("BM25", bm25_score)
    return score
