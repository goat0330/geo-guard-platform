from types import SimpleNamespace

from app.yuxi_port.evaluation import calculate_retrieval_metrics


def test_yuxi_retrieval_metrics_are_used_for_chunk_ground_truth():
    result = calculate_retrieval_metrics(
        [SimpleNamespace(chunk_id="chunk-a"), SimpleNamespace(chunk_id="chunk-c")],
        ["EV-chunk-a", "EV-chunk-b"],
        2,
    )

    assert result["recall_at_k"] == 0.5
    assert result["precision_at_k"] == 0.5
    assert result["yuxi_metrics"]["recall@1"] == 0.5
    assert result["yuxi_metrics"]["recall@2"] == 0.5
    assert result["yuxi_metrics"]["f1@2"] == 0.5
