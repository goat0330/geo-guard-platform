import asyncio
from functools import partial
from pathlib import Path

from . import db
from .config import settings
from .embedding import embed_batched, embed_texts_sync, enabled as embedding_enabled
from .parser import parse_file, parse_pdf_layout, parse_pdf_mineru, parse_pdf_mineru_official, parse_text
from .yuxi_port.chunk_presets import normalize_chunk_config, normalize_preset
from .yuxi_port.chunking import chunk_blocks


def _embedding_model(config: dict | None) -> str:
    return (config or {}).get("model") or settings.embedding_model


async def _chunk_blocks(
    blocks: list[dict],
    preset: str,
    parser_config: dict,
    file_name: str,
    embedding_config: dict | None,
) -> list[dict]:
    if preset == "semantic" and embedding_enabled(embedding_config):
        embed_fn = partial(embed_texts_sync, config=embedding_config)
        return await asyncio.to_thread(chunk_blocks, blocks, preset, parser_config, file_name, embed_fn)
    return chunk_blocks(blocks, preset, parser_config, file_name)


async def _embed_chunks(chunks: list[dict], config: dict | None = None) -> int:
    if not chunks or not embedding_enabled(config):
        return 0
    vectors = await embed_batched([chunk["text"] for chunk in chunks], config)
    if vectors is None or len(vectors) != len(chunks):
        raise ValueError("Embedding vector count does not match chunk count")
    for chunk, vector in zip(chunks, vectors, strict=True):
        db.update_embedding(chunk["id"], vector, _embedding_model(config))
    return len(vectors)


async def _index_document(
    file_name: str,
    file_path: str | None,
    mime_type: str,
    parser_name: str,
    blocks: list[dict],
    preset: str,
    parser_config: dict,
    metadata: dict | None,
    knowledge_base_id: str | None = None,
    folder_id: str | None = None,
    embedding_config: dict | None = None,
    document_id: str | None = None,
) -> dict:
    if document_id is None:
        document_id = db.create_document(
            file_name,
            file_path,
            mime_type,
            parser_name,
            preset,
            parser_config,
            {"source_type": "pdf" if mime_type == "application/pdf" else "text", **(metadata or {})},
            knowledge_base_id,
            folder_id,
        )
    try:
        if not blocks:
            raise ValueError("文档中没有可索引的文本；扫描 PDF 或图片请配置可用的 OCR Parser")
        db.update_document_status(document_id, "chunking", parser=parser_name)
        chunks = await _chunk_blocks(blocks, preset, parser_config, file_name, embedding_config)
        if not chunks:
            raise ValueError("切块结果为空，未创建可检索资料")
        db.replace_blocks(document_id, blocks)
        db.replace_chunks(document_id, chunks)
        if embedding_enabled(embedding_config):
            db.update_document_status(document_id, "embedding")
        embedded = await _embed_chunks(chunks, embedding_config)
        db.update_document_status(document_id, "indexed")
    except Exception as exc:
        db.update_document_status(document_id, "failed", str(exc))
        raise
    return {
        "document_id": document_id,
        "file_name": file_name,
        "parser": parser_name,
        "blocks": len(blocks),
        "chunks": len(chunks),
        "embedded_chunks": embedded,
        "embedding_enabled": embedding_enabled(embedding_config),
        "embedding_model": _embedding_model(embedding_config) if embedded else None,
        "chunk_preset_id": preset,
        "chunk_parser_config": parser_config,
        "status": "indexed",
    }


async def ingest_pdf(
    path: str,
    file_name: str | None = None,
    preset: str = "general",
    parser_config: dict | None = None,
    metadata: dict | None = None,
    knowledge_base_id: str | None = None,
    folder_id: str | None = None,
    embedding_config: dict | None = None,
    parser_adapter: dict | None = None,
) -> dict:
    normalized_preset = normalize_preset(preset)
    normalized_config = normalize_chunk_config(parser_config)
    source_path = str(Path(path).resolve())
    parser_options = parser_adapter or {}
    parser_engine = parser_options.get("engine", "auto")
    mineru_uri = parser_options.get("mineru_api_uri") or None
    use_mineru_official = parser_engine == "mineru_official" or (
        parser_engine == "auto" and not mineru_uri and bool(settings.mineru_api_key)
    )
    use_mineru = parser_engine == "mineru" or (
        parser_engine == "auto" and bool(mineru_uri or (settings.mineru_enabled and settings.mineru_api_uri))
    )
    parser_name = (
        "mineru-official"
        if use_mineru_official
        else "mineru-file_parse"
        if use_mineru
        else "pymupdf-layout"
    )
    name = file_name or Path(path).name
    document_id = db.create_document(
        name, source_path, "application/pdf", parser_name, normalized_preset, normalized_config,
        {"source_type": "pdf", **(metadata or {})}, knowledge_base_id, folder_id,
    )
    try:
        db.update_document_status(document_id, "parsing")
        if use_mineru_official:
            blocks = await parse_pdf_mineru_official(
                source_path,
                parser_options.get("api_key"),
                mineru_uri,
            )
        elif use_mineru:
            blocks = await parse_pdf_mineru(source_path, mineru_uri)
        else:
            blocks = parse_pdf_layout(source_path)
        db.update_document_status(document_id, "parsed", parser=parser_name)
        return await _index_document(
            name,
            source_path,
            "application/pdf",
            parser_name,
            blocks,
            normalized_preset,
            normalized_config,
            metadata,
            knowledge_base_id,
            folder_id,
            embedding_config,
            document_id,
        )
    except Exception as exc:
        db.update_document_status(document_id, "failed", str(exc), parser=parser_name)
        raise


async def ingest_file(
    path: str,
    file_name: str,
    mime_type: str,
    preset: str = "general",
    parser_config: dict | None = None,
    metadata: dict | None = None,
    knowledge_base_id: str | None = None,
    folder_id: str | None = None,
    embedding_config: dict | None = None,
    parser_adapter: dict | None = None,
) -> dict:
    """Parse and index a supported local document while preserving its source file."""
    normalized_preset = normalize_preset(preset)
    normalized_config = normalize_chunk_config(parser_config)
    source_path = str(Path(path).resolve())
    name = Path(file_name).name
    document_id = db.create_document(
        name, source_path, mime_type, "pending", normalized_preset, normalized_config,
        {"source_type": Path(name).suffix.lower().lstrip(".") or "text", **(metadata or {})},
        knowledge_base_id, folder_id,
    )
    try:
        db.update_document_status(document_id, "parsing")
        parser_options = {**(parser_adapter or {}), "knowledge_base_id": knowledge_base_id}
        blocks, parser_name = await parse_file(source_path, parser_options)
        db.update_document_status(document_id, "parsed", parser=parser_name)
        return await _index_document(
            name, source_path, mime_type, parser_name, blocks, normalized_preset,
            normalized_config, metadata, knowledge_base_id, folder_id,
            embedding_config, document_id,
        )
    except Exception as exc:
        db.update_document_status(document_id, "failed", str(exc))
        raise


async def ingest_text(
    file_name: str,
    text: str,
    preset: str = "general",
    parser_config: dict | None = None,
    metadata: dict | None = None,
    knowledge_base_id: str | None = None,
    folder_id: str | None = None,
    embedding_config: dict | None = None,
) -> dict:
    normalized_preset = normalize_preset(preset)
    normalized_config = normalize_chunk_config(parser_config)
    name = Path(file_name).name
    document_id = db.create_document(
        name, None, "text/plain", "plain-text", normalized_preset, normalized_config,
        {"source_type": "text", **(metadata or {})}, knowledge_base_id, folder_id,
    )
    try:
        db.update_document_status(document_id, "parsing")
        blocks = parse_text(text)
        db.update_document_status(document_id, "parsed", parser="plain-text")
        return await _index_document(
            name,
            None,
            "text/plain",
            "plain-text",
            blocks,
            normalized_preset,
            normalized_config,
            metadata,
            knowledge_base_id,
            folder_id,
            embedding_config,
            document_id,
        )
    except Exception as exc:
        db.update_document_status(document_id, "failed", str(exc), parser="plain-text")
        raise


async def parse_existing_document(document_id: str) -> dict:
    document = db.get_document(document_id)
    if not document:
        raise ValueError("Document not found")
    knowledge_base = db.get_knowledge_base(document["knowledge_base_id"]) if document.get("knowledge_base_id") else None
    kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
    parser_config = kb_config.get("parser", {})
    path = document.get("file_path")
    try:
        db.update_document_status(document_id, "parsing")
        if document["mime_type"] == "application/pdf":
            if not path or not Path(path).is_file():
                raise ValueError("PDF 原文件不存在，无法重新解析")
            engine = parser_config.get("engine", "auto")
            mineru_uri = parser_config.get("mineru_api_uri") or None
            use_mineru = engine == "mineru" or (
                engine == "auto" and bool(mineru_uri or (settings.mineru_enabled and settings.mineru_api_uri))
            )
            parser_name = "mineru-file_parse" if use_mineru else "pymupdf-layout"
            blocks = await parse_pdf_mineru(path, mineru_uri) if use_mineru else parse_pdf_layout(path)
        elif path and Path(path).is_file():
            blocks, parser_name = await parse_file(path, parser_config)
        else:
            blocks = db.get_blocks(document_id)
            if not blocks:
                raise ValueError("原始文件或解析文本不存在，无法重新解析")
            parser_name = document.get("parser") or "stored-blocks"
        db.replace_blocks(document_id, blocks)
        db.update_document_status(document_id, "parsed", parser=parser_name)
        return {"document_id": document_id, "status": "parsed", "parser": parser_name, "blocks": len(blocks)}
    except Exception as exc:
        db.update_document_status(document_id, "failed", str(exc))
        raise


async def index_existing_document(document_id: str) -> dict:
    document = db.get_document(document_id)
    if not document:
        raise ValueError("Document not found")
    if document["status"] not in {"parsed", "indexed", "failed"}:
        raise ValueError(f"文件当前状态 {document['status']}，不能开始索引")
    knowledge_base = db.get_knowledge_base(document["knowledge_base_id"]) if document.get("knowledge_base_id") else None
    kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
    chunking = kb_config.get("chunking", {})
    parser_config = {
        "chunk_token_num": chunking.get("chunk_token_num", document["chunk_parser_config"].get("chunk_token_num", 512)),
        "overlapped_percent": chunking.get("overlapped_percent", document["chunk_parser_config"].get("overlapped_percent", 10)),
        "delimiter": chunking.get("delimiter", document["chunk_parser_config"].get("delimiter", "\\n")),
    }
    blocks = db.get_blocks(document_id)
    if not blocks:
        raise ValueError("文件尚未解析，请先执行解析")
    return await _index_document(
        document["file_name"],
        document.get("file_path"),
        document["mime_type"],
        document.get("parser") or "stored-blocks",
        blocks,
        chunking.get("chunk_preset_id", document["chunk_preset_id"] or "general"),
        parser_config,
        document.get("metadata", {}),
        document.get("knowledge_base_id"),
        document.get("folder_id"),
        kb_config.get("embedding"),
        document_id,
    )
