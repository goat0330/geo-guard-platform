import pytest

from app.yuxi_port.chunk_presets import normalize_chunk_config, normalize_preset
from app.yuxi_port.chunking import chunk_blocks
from app.yuxi_port.rerank import _scores_in_input_order


def test_chunk_parameter_boundaries():
    assert normalize_chunk_config({"chunk_token_num": 64, "overlapped_percent": 80})["chunk_token_num"] == 64
    assert normalize_chunk_config({"chunk_token_num": 4096, "overlapped_percent": 0})["chunk_token_num"] == 4096
    for config in (
        {"chunk_token_num": 63}, {"chunk_token_num": 4097},
        {"chunk_token_num": True}, {"overlapped_percent": 81},
        {"delimiter": "x" * 65},
    ):
        with pytest.raises(ValueError):
            normalize_chunk_config(config)
    with pytest.raises(ValueError, match="Unknown chunk preset"):
        normalize_preset("not-a-yuxi-preset")


def test_qa_chunks_keep_original_question_and_answer_locations():
    blocks = [
        {"id": "b1", "page": 2, "bbox": [10, 10, 80, 20], "text": "Q: 滑坡隐患如何巡查？"},
        {"id": "b2", "page": 3, "bbox": [10, 30, 90, 50], "text": "A: 雨后应重点检查坡体裂缝和排水情况。"},
    ]
    chunks = chunk_blocks(blocks, "qa", {"chunk_token_num": 64})
    assert len(chunks) == 1
    assert "坡体裂缝" in chunks[0]["text"]
    assert {span["page"] for span in chunks[0]["source_spans"]} == {2, 3}
    assert chunks[0]["metadata"]["question"] == "滑坡隐患如何巡查？"


def test_chunk_token_target_is_respected_for_long_qa_and_general_text():
    long_text = "地质灾害应急响应启动条件需要根据灾情等级和现场情况综合确定。" * 30
    blocks = [{"id": "b1", "page": 1, "bbox": [1, 1, 100, 20], "text": f"Q: {long_text}\nA: {long_text}"}]
    for preset in ("general", "qa"):
        chunks = chunk_blocks(blocks, preset, {"chunk_token_num": 64, "overlapped_percent": 80})
        assert chunks
        assert all(chunk["token_count"] <= 64 for chunk in chunks)


def test_oversized_qa_chunks_keep_page_level_source_spans_local():
    blocks = [
        {"id": "question", "page": 1, "bbox": [10, 10, 100, 30], "text": "Q: 如何巡查？"},
        {"id": "answer-1", "page": 2, "bbox": [10, 40, 100, 60], "text": "A: 雨后检查坡体裂缝。" * 18},
        {"id": "answer-2", "page": 3, "bbox": [10, 70, 100, 90], "text": "继续检查排水和监测数据。" * 18},
    ]
    chunks = chunk_blocks(blocks, "qa", {"chunk_token_num": 64, "overlapped_percent": 0})
    assert len(chunks) > 1
    assert all(len({span["page"] for span in chunk["source_spans"]}) <= 2 for chunk in chunks)
    assert all(chunk["token_count"] <= 64 for chunk in chunks)


def test_rerank_score_order_and_alignment_validation():
    scores = _scores_in_input_order([
        {"index": 1, "relevance_score": 0.2},
        {"index": 0, "relevance_score": 0.9},
    ], 2)
    assert scores == [0.9, 0.2]
    for results in (
        [{"index": 0, "relevance_score": 0.9}],
        [{"index": 0, "relevance_score": 0.9}, {"index": 0, "relevance_score": 0.2}],
        [{"index": 0, "relevance_score": float("nan")}, {"index": 1, "relevance_score": 0.2}],
    ):
        with pytest.raises(ValueError):
            _scores_in_input_order(results, 2)
