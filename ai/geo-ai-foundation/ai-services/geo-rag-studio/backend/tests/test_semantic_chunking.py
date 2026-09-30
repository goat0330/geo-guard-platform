from app.yuxi_port.chunking import chunk_blocks
from app.yuxi_port.ragflow_like.parsers.semantic import chunk_markdown
from app.yuxi_port.ragflow_like.utils.table_utils import html_table_to_key_value


def test_semantic_preset_marks_sentence_fallback_when_embedding_is_unconfigured():
    text = "滑坡裂缝持续扩大，坡体位移逐步增加。降雨后坡脚出现渗水，沟谷水位明显上涨。" * 8
    chunks = chunk_blocks(
        [{"id": "SRC-1", "text": text, "page": 1, "bbox": [1, 2, 3, 4]}],
        "semantic",
        {"chunk_token_num": 64},
    )

    assert chunks
    assert len(chunks) >= 2
    assert all(chunk["metadata"]["semantic_fallback"] for chunk in chunks)
    assert all(chunk["token_count"] <= 64 for chunk in chunks)
    assert all(chunk["source_spans"] for chunk in chunks)


def test_semantic_preset_uses_supplied_embeddings_for_long_paragraphs():
    text = "甲类滑坡裂缝不断扩展。甲类坡体位移持续增加。乙类暴雨过程仍在继续。乙类沟谷水位明显上涨。"
    calls = []

    def embed(sentences):
        calls.append(sentences)
        return [[1.0, 0.0] if sentence.startswith("甲") else [0.0, 1.0] for sentence in sentences]

    chunks, fallback = chunk_markdown(text, {"chunk_token_num": 16}, embed_fn=embed)

    assert fallback is False
    assert calls
    assert len(chunks) >= 2
    assert any("甲类" in chunk for chunk in chunks)
    assert any("乙类" in chunk for chunk in chunks)


def test_yuxi_semantic_parser_preserves_heading_and_markdown_table_context():
    markdown = """# 隐患复核

斜坡裂缝持续扩大，雨后坡脚出现渗水。

| 指标 | 监测结果 |
| --- | --- |
| 位移 | 持续增加 |
"""

    chunks, fallback = chunk_markdown(markdown, {"chunk_token_num": 128})

    assert fallback is False
    assert any("隐患复核" in chunk and "斜坡裂缝" in chunk for chunk in chunks)
    assert any("Table" in chunk and "位移" in chunk and "持续增加" in chunk for chunk in chunks)


def test_yuxi_table_chunker_converts_html_tables_to_key_value_rows():
    rows = html_table_to_key_value(
        "<table><tr><th>指标</th><th>结果</th></tr><tr><td>位移</td><td>持续增加</td></tr></table>"
    )

    assert rows == ["指标：位移；结果：持续增加；"]
