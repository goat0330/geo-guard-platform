"""Thin Geo RAG adapter over Yuxi's upstream retrieval evaluation metrics."""

from . import _upstream  # noqa: F401
from yuxi.knowledge.eval.metrics import EvaluationMetricsCalculator, RetrievalMetrics


def calculate_retrieval_metrics(evidences, relevant_evidence_ids: list[str], top_k: int) -> dict:
    retrieved_chunk_ids = [str(item.chunk_id) for item in evidences]
    relevant_chunk_ids = [
        evidence_id[3:] if evidence_id.startswith("EV-") else evidence_id
        for evidence_id in relevant_evidence_ids
    ]
    ks = sorted({1, 3, 5, 10, top_k})
    upstream_metrics = EvaluationMetricsCalculator.calculate_retrieval_metrics(
        [{"chunk_id": chunk_id} for chunk_id in retrieved_chunk_ids],
        relevant_chunk_ids,
        k_values=ks,
    )
    return {
        "recall_at_k": RetrievalMetrics.recall_at_k(retrieved_chunk_ids, relevant_chunk_ids, top_k),
        "precision_at_k": RetrievalMetrics.precision_at_k(retrieved_chunk_ids, relevant_chunk_ids, top_k),
        "yuxi_metrics": upstream_metrics,
    }
