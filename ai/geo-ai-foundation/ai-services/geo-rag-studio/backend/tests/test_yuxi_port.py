import asyncio

import pytest

from app.yuxi_port import _upstream as _yuxi_vendor_path
from app.yuxi_port.chunk_presets import normalize_chunk_config, normalize_preset
from app.yuxi_port.chunking import chunk_blocks
from app.yuxi_port import rerank as rerank_module
from app.yuxi_port.rerank import _scores_in_input_order
from yuxi.knowledge.chunking.ragflow_like.dispatcher import chunk_markdown as yuxi_chunk_markdown
from yuxi.knowledge.chunking.ragflow_like.parsers.semantic import chunk_markdown as yuxi_semantic_chunk_markdown


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


def test_long_input_follows_vendored_yuxi_chunk_limits():
    long_text = "地质灾害应急响应启动条件需要根据灾情等级和现场情况综合确定。" * 30
    blocks = [{"id": "b1", "page": 1, "bbox": [1, 1, 100, 20], "text": f"Q: {long_text}\nA: {long_text}"}]
    for preset in ("general", "qa"):
        config = {"chunk_token_num": 64, "overlapped_percent": 80}
        chunks = chunk_blocks(blocks, preset, config)
        expected = [
            record["content"]
            for record in yuxi_chunk_markdown(
                markdown_content=blocks[0]["text"],
                file_id="expected-long",
                filename="document.md",
                processing_params={"chunk_preset_id": preset, "chunk_parser_config": config},
            )
        ]
        assert [chunk["text"] for chunk in chunks] == expected


def test_oversized_qa_chunks_keep_page_level_source_spans_local():
    blocks = [
        {"id": "question", "page": 1, "bbox": [10, 10, 100, 30], "text": "Q: 如何巡查？"},
        {"id": "answer-1", "page": 2, "bbox": [10, 40, 100, 60], "text": "A: " + "雨后检查坡体裂缝。" * 500},
        {"id": "answer-2", "page": 3, "bbox": [10, 70, 100, 90], "text": "继续检查排水和监测数据。" * 500},
    ]
    chunks = chunk_blocks(blocks, "qa", {"chunk_token_num": 64, "overlapped_percent": 0})
    assert len(chunks) > 1
    assert all(len({span["page"] for span in chunk["source_spans"]}) <= 2 for chunk in chunks)
    assert all(len(chunk["text"]) <= 4000 for chunk in chunks)


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


@pytest.mark.parametrize(
    ("protocol", "response", "expected_payload"),
    [
        (
            "openai",
            {"results": [{"index": 1, "relevance_score": 0.2}, {"index": 0, "relevance_score": 0.9}]},
            {"model": "rerank-v1", "query": "query", "documents": ["a", "b"], "max_chunks_per_doc": 512},
        ),
        (
            "dashscope",
            {"output": {"results": [{"index": 1, "relevance_score": 0.2}, {"index": 0, "relevance_score": 0.9}]}},
            {
                "model": "rerank-v1",
                "input": {"query": "query", "documents": ["a", "b"]},
                "parameters": {"top_n": 2, "return_documents": False, "instruct": "geology"},
            },
        ),
    ],
)
def test_rerank_calls_vendored_yuxi_protocol_and_aligns_scores(
    monkeypatch, protocol, response, expected_payload
):
    request = {}

    class FakeResponse:
        def raise_for_status(self):
            return None

        def json(self):
            return response

    class FakeClient:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def post(self, url, *, headers, json):
            request.update(url=url, headers=headers, payload=json)
            return FakeResponse()

    monkeypatch.setattr(rerank_module.httpx, "AsyncClient", lambda **_kwargs: FakeClient())
    result = asyncio.run(
        rerank_module.rerank(
            "query",
            ["a", "b"],
            {
                "model": "rerank-v1",
                "base_url": "https://models.example/v1/rerank",
                "api_key": "test-key",
                "protocol": protocol,
                "parameters": {"instruct": "geology"} if protocol == "dashscope" else {},
            },
        )
    )

    assert request["url"] == "https://models.example/v1/rerank"
    assert request["payload"] == expected_payload
    assert request["headers"]["Authorization"] == "Bearer test-key"
    assert result == pytest.approx([0.7109495, 0.549834])


def test_rerank_uses_yuxi_batch_size_and_keeps_scores_aligned(monkeypatch):
    requests = []

    class FakeResponse:
        def __init__(self, documents):
            self.result = {
                "results": [
                    {"index": index, "relevance_score": index + 0.1}
                    for index in reversed(range(len(documents)))
                ]
            }

        def raise_for_status(self):
            return None

        def json(self):
            return self.result

    class FakeClient:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *_args):
            return None

        async def post(self, _url, *, headers, json):
            requests.append(json)
            return FakeResponse(json["documents"])

    monkeypatch.setattr(rerank_module.httpx, "AsyncClient", lambda **_kwargs: FakeClient())
    documents = [f"chunk-{index}" for index in range(33)]
    result = asyncio.run(
        rerank_module.rerank(
            "query",
            documents,
            {"model": "rerank-v1", "base_url": "https://models.example/v1/rerank", "api_key": "test-key"},
        )
    )

    assert [len(request["documents"]) for request in requests] == [32, 1]
    expected = [rerank_module.sigmoid(index + 0.1) for index in range(32)]
    expected.append(rerank_module.sigmoid(0.1))
    assert result == pytest.approx(expected)


@pytest.mark.parametrize("preset", ["general", "qa", "book", "laws", "semantic", "separator"])
def test_chunk_text_matches_vendored_yuxi_parser(preset):
    config = {"chunk_token_num": 64, "overlapped_percent": 10, "delimiter": "\\n"}
    markdown = "# 第一章 总则\n\n第一节 适用范围\n本规范适用于地质灾害隐患复核。\n\n问题：雨后检查什么？\t回答：检查裂缝和排水。"
    blocks = [{"id": "b1", "page": 2, "bbox": [10, 20, 100, 40], "text": markdown}]
    if preset == "semantic":
        expected = yuxi_semantic_chunk_markdown(markdown, config, embed_fn=None)
    else:
        expected = [
            record["content"]
            for record in yuxi_chunk_markdown(
                markdown_content=markdown,
                file_id="expected",
                filename="source.md",
                processing_params={"chunk_preset_id": preset, "chunk_parser_config": config},
            )
        ]
    actual = chunk_blocks(blocks, preset, config, "source.md")
    assert [chunk["text"] for chunk in actual] == expected
    assert all(chunk["metadata"]["chunk_engine_version"].startswith("ragflow_like_v1") for chunk in actual)
