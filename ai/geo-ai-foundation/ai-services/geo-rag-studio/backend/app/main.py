import math
import copy
import csv
import io
import re
import sqlite3
import time
import uuid
import zipfile
import json
import mimetypes
from pathlib import Path, PurePosixPath
from urllib.parse import urlparse

import httpx
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse, Response

from . import db
from .config import settings
from .embedding import embed_texts, enabled as embedding_enabled, value as embedding_value
from .parser import parse_pdf_layout, parse_pdf_mineru, parse_pdf_mineru_official, render_pdf_page
from .pipeline import index_existing_document, ingest_file, ingest_pdf, ingest_text, parse_existing_document
from .knowledge_features import build_entity_graph, generate_mindmap, indexed_content_fingerprint
from .retrieval import ProviderUnavailable, retrieve, retrieve_debug
from .yuxi_port.rerank import enabled as reranker_enabled, rerank
from .schemas import (
    EmbeddingTestRequest,
    EvaluationRequest,
    EvaluationCase,
    Evidence,
    FolderPayload,
    FolderRenamePayload,
    DocumentBatchPayload,
    DocumentMovePayload,
    MindMapPayload,
    GraphConfigPayload,
    EvaluationRunPayload,
    KnowledgeBasePayload,
    KnowledgeBaseUpdatePayload,
    RetrieveRequest,
    RetrieveResponse,
    RerankerTestRequest,
    TextDocumentRequest,
)
from .yuxi_port.chunk_presets import get_options, normalize_chunk_config, normalize_preset
from .yuxi_port.ragflow_like.nlp import count_tokens

app = FastAPI(title="Geo RAG Studio API", version="0.2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)

ALLOWED_UPLOADS = {
    ".pdf", ".txt", ".md", ".csv", ".json", ".html", ".htm",
    ".docx", ".pptx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png",
    ".bmp", ".tiff", ".tif", ".webp",
}
SUPPORTED_UPLOAD_TYPES = ALLOWED_UPLOADS | {".zip"}
MAX_UPLOAD_BYTES = 100 * 1024 * 1024


def _provider_error(exc: Exception) -> HTTPException:
    if isinstance(exc, ProviderUnavailable):
        return HTTPException(status_code=503, detail=str(exc))
    if isinstance(exc, httpx.HTTPError):
        return HTTPException(status_code=502, detail=f"外部解析或模型服务请求失败：{type(exc).__name__}")
    if isinstance(exc, ValueError):
        return HTTPException(status_code=422, detail=str(exc))
    return HTTPException(status_code=500, detail="RAG pipeline failed")


def _public_document(document: dict) -> dict:
    return {key: value for key, value in document.items() if key != "file_path"}


def _public_knowledge_base(knowledge_base: dict) -> dict:
    result = copy.deepcopy(knowledge_base)
    for section_name in ("parser", "embedding", "reranker", "dify", "notion", "graph"):
        section = result.get("config", {}).get(section_name)
        if not isinstance(section, dict):
            continue
        if section_name == "graph":
            options = section.get("graph_build_config", {}).get("extractor_options")
            if isinstance(options, dict) and "api_key" in options:
                options["api_key_set"] = bool(options.pop("api_key"))
        for key in ("api_key", "token", "dify_token"):
            if key in section:
                section[f"{key}_set"] = bool(section.pop(key))
    return result


def _merge_config(current: dict, incoming: dict) -> dict:
    merged = copy.deepcopy(current or db.default_knowledge_base_config())
    for section, values in incoming.items():
        if isinstance(values, dict) and isinstance(merged.get(section), dict):
            clean_values = {key: value for key, value in values.items() if not key.endswith("_set")}
            updated = {**merged[section], **clean_values}
            for secret in ("api_key", "token", "dify_token"):
                if secret in clean_values and not clean_values[secret] and merged[section].get(secret):
                    updated[secret] = merged[section][secret]
            merged[section] = updated
        else:
            merged[section] = values
    chunking = merged.get("chunking", {})
    merged["chunking"] = {
        "chunk_preset_id": normalize_preset(chunking.get("chunk_preset_id", "general")),
        **normalize_chunk_config(
            {
                "chunk_token_num": chunking.get("chunk_token_num", 512),
                "overlapped_percent": chunking.get("overlapped_percent", 10),
                "delimiter": chunking.get("delimiter", "\\n"),
            }
        ),
    }
    retrieval = merged.get("retrieval", {})
    for name, value in db.default_knowledge_base_config()["retrieval"].items():
        retrieval.setdefault(name, value)
    for name, minimum, maximum in (("recall_top_k", 1, 200), ("final_top_k", 1, 100)):
        value = int(retrieval.get(name, 50 if name == "recall_top_k" else 8))
        if not minimum <= value <= maximum:
            raise ValueError(f"{name} 必须在 {minimum}-{maximum} 之间")
        retrieval[name] = value
    if retrieval.get("search_mode", "hybrid") not in {"hybrid", "keyword", "vector"}:
        raise ValueError("不支持的检索模式")
    for name, default in (("bm25_top_k", 50),):
        value = int(retrieval.get(name, default))
        if not 1 <= value <= 200:
            raise ValueError(f"{name} 必须在 1-200 之间")
        retrieval[name] = value
    for name in ("similarity_threshold", "bm25_drop_ratio_search"):
        value = float(retrieval.get(name, 0.0))
        if not 0 <= value <= 1:
            raise ValueError(f"{name} 必须在 0-1 之间")
        retrieval[name] = value
    retrieval["use_graph_retrieval"] = bool(retrieval.get("use_graph_retrieval", False))
    graph = merged.get("graph", {})
    graph["max_keywords_per_document"] = int(graph.get("max_keywords_per_document", 12))
    if not 1 <= graph["max_keywords_per_document"] <= 50:
        raise ValueError("graph.max_keywords_per_document 必须在 1-50 之间")
    merged["graph"] = graph
    for name in ("vector_weight", "bm25_weight"):
        retrieval[name] = float(retrieval.get(name, 0.7 if name == "vector_weight" else 0.3))
        if not 0 <= retrieval[name] <= 1:
            raise ValueError(f"{name} 必须在 0-1 之间")
    if retrieval["vector_weight"] + retrieval["bm25_weight"] <= 0:
        raise ValueError("Vector 与 BM25 权重不能同时为 0")
    merged["retrieval"] = retrieval
    return merged


def _validate_model_config(config: dict) -> None:
    for section_name in ("embedding", "reranker"):
        section = config.get(section_name, {})
        base_url = str(section.get("base_url") or "").strip()
        if base_url and urlparse(base_url).scheme not in {"http", "https"}:
            raise ValueError(f"{section_name} 服务地址必须是 http 或 https")
    embedding = config.get("embedding", {})
    dimensions = embedding.get("dimensions")
    if dimensions not in (None, "") and int(dimensions) < 1:
        raise ValueError("Embedding dimensions 必须大于 0")
    if embedding.get("batch_size") is not None and not 1 <= int(embedding["batch_size"]) <= 128:
        raise ValueError("Embedding batch_size 必须在 1-128 之间")
    if config.get("reranker", {}).get("protocol", "openai") not in {"openai", "dashscope"}:
        raise ValueError("不支持的 Reranker 协议")
    parser = config.get("parser", {})
    if parser.get("engine", "auto") not in {"auto", "pymupdf-layout", "mineru"}:
        raise ValueError("不支持的 PDF 解析器")
    mineru_url = str(parser.get("mineru_api_uri") or "").strip()
    if mineru_url and urlparse(mineru_url).scheme not in {"http", "https"}:
        raise ValueError("MinerU API URL 必须是 http 或 https")
    dify = config.get("dify", {})
    dify_values = [str(dify.get(key) or "").strip() for key in ("dify_api_url", "dify_token", "dify_dataset_id")]
    if any(dify_values) and not all(dify_values):
        raise ValueError("Dify 连接需要同时填写 API URL、Token 和 Dataset ID")
    if dify_values[0] and not dify_values[0].rstrip("/").endswith("/v1"):
        raise ValueError("Dify API URL 必须以 /v1 结尾")
    notion = config.get("notion", {})
    notion_base = str(notion.get("base_url") or "https://api.notion.com/v1").strip()
    if notion_base and urlparse(notion_base).scheme not in {"http", "https"}:
        raise ValueError("Notion API URL 必须是 http 或 https")


@app.get("/health")
async def health():
    return {"status": "ok", "service": "geo-rag-studio"}


@app.post("/api/v1/providers/embedding/test")
async def test_embedding_provider(request: EmbeddingTestRequest):
    config = request.model_dump(exclude_none=True)
    if not embedding_enabled(config):
        return JSONResponse(
            status_code=503,
            content={"status": "unavailable", "provider": "openai-compatible", "error": "请配置 Embedding Base URL、API Key 和模型"},
        )
    try:
        started = time.perf_counter()
        vectors = await embed_texts(["重庆地质灾害隐患复核 Embedding 连通性测试"], config)
        if not vectors:
            return JSONResponse(status_code=502, content={"status": "error", "provider": "openai-compatible", "error": "模型未返回向量"})
        return {
            "status": "ok",
            "provider": "openai-compatible",
            "model": embedding_value(config, "model", settings.embedding_model),
            "dimensions": len(vectors[0]),
            "latency_ms": round((time.perf_counter() - started) * 1000, 2),
        }
    except httpx.HTTPError as exc:
        raise _provider_error(exc) from exc
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@app.post("/api/v1/providers/parser/test")
async def test_parser_provider(
    file: UploadFile = File(...),
    engine: str = Form("mineru"),
    mineru_api_uri: str | None = Form(None),
    mineru_api_key: str | None = Form(None),
):
    file_name = Path(file.filename or "document.pdf").name
    if Path(file_name).suffix.lower() != ".pdf":
        raise HTTPException(status_code=400, detail="MinerU 测试只接受 PDF 文件")
    content = await file.read()
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="单文件最大 100 MB")
    if not content.startswith(b"%PDF-"):
        raise HTTPException(status_code=422, detail="上传内容不是有效 PDF")
    selected_engine = (engine or "mineru").strip()
    endpoint_uri = (mineru_api_uri or "").strip() or (settings.mineru_api_uri if settings.mineru_enabled else "")
    api_key = (mineru_api_key or "").strip() or settings.mineru_api_key
    if selected_engine == "auto":
        selected_engine = "mineru" if endpoint_uri else "mineru_official" if api_key else "auto"
    if selected_engine == "auto":
        return JSONResponse(
            status_code=503,
            content={"status": "unavailable", "provider": "mineru", "error": "未配置可测试的解析器；请选择 PyMuPDF、MinerU 服务或 MinerU 官方 API"},
        )
    Path(settings.rag_upload_dir).mkdir(parents=True, exist_ok=True)
    temp_path = Path(settings.rag_upload_dir) / f"provider-test-{uuid.uuid4().hex}.pdf"
    temp_path.write_bytes(content)
    try:
        if selected_engine == "pymupdf-layout":
            blocks = parse_pdf_layout(str(temp_path))
        elif selected_engine == "mineru":
            if not endpoint_uri:
                return JSONResponse(
                    status_code=503,
                    content={"status": "unavailable", "provider": "mineru", "error": "MinerU 服务地址未配置"},
                )
            blocks = await parse_pdf_mineru(str(temp_path), endpoint_uri)
        elif selected_engine == "mineru_official":
            if not api_key:
                return JSONResponse(
                    status_code=503,
                    content={"status": "unavailable", "provider": "mineru_official", "error": "MinerU 官方 API Key 未配置"},
                )
            blocks = await parse_pdf_mineru_official(str(temp_path), api_key, endpoint_uri or None)
        else:
            raise HTTPException(status_code=422, detail="不支持的 PDF 解析器")
        return {
            "status": "ok",
            "provider": selected_engine,
            "file_name": file_name,
            "blocks": len(blocks),
            "pages": len({block.get("page") for block in blocks if block.get("page") is not None}),
            "bbox_count": sum(bool(block.get("bbox")) for block in blocks),
            "sample_text": blocks[0]["text"][:300] if blocks else "",
        }
    except httpx.HTTPStatusError as exc:
        return JSONResponse(
            status_code=502,
            content={"status": "error", "provider": selected_engine, "error": f"解析器返回 HTTP {exc.response.status_code}"},
        )
    except httpx.HTTPError as exc:
        return JSONResponse(
            status_code=502,
            content={"status": "error", "provider": selected_engine, "error": f"解析器请求失败：{type(exc).__name__}"},
        )
    except (ValueError, zipfile.BadZipFile) as exc:
        return JSONResponse(status_code=422, content={"status": "error", "provider": selected_engine, "error": str(exc)})
    finally:
        temp_path.unlink(missing_ok=True)


@app.post("/api/v1/providers/reranker/test")
async def test_reranker_provider(request: RerankerTestRequest):
    config = request.model_dump(exclude={"query", "documents"})
    protocol = config.get("protocol") or settings.rerank_protocol
    if any(not document.strip() for document in request.documents):
        raise HTTPException(status_code=422, detail="documents 不能包含空文本")
    if not reranker_enabled(config):
        return JSONResponse(
            status_code=503,
            content={"status": "unavailable", "provider": protocol, "error": "请配置 Reranker Base URL、API Key 和模型"},
        )
    try:
        started = time.perf_counter()
        scores = await rerank(request.query, request.documents, config)
        if scores is None or len(scores) != len(request.documents):
            raise ValueError("Rerank score count does not match the document count")
        return {
            "status": "ok",
            "provider": protocol,
            "model": config["model"] or settings.rerank_model,
            "document_count": len(request.documents),
            "scores": scores,
            "latency_ms": round((time.perf_counter() - started) * 1000, 2),
        }
    except httpx.HTTPError as exc:
        raise _provider_error(exc) from exc
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@app.get("/api/v1/config")
async def config():
    from .yuxi_port.rerank import enabled as rerank_enabled

    return {
        "embedding_enabled": embedding_enabled(),
        "embedding_model": settings.embedding_model or None,
        "reranker_enabled": rerank_enabled(),
        "rerank_model": settings.rerank_model or None,
        "mineru_enabled": settings.mineru_enabled and bool(settings.mineru_api_uri),
        "parser_default": "mineru-file_parse" if settings.mineru_enabled and settings.mineru_api_uri else "pymupdf-layout",
        "storage": "sqlite+local-files",
        "vector_storage": "sqlite-json (local learning mode)",
    }


@app.get("/api/v1/pipeline")
async def pipeline():
    from .yuxi_port.rerank import enabled as rerank_enabled

    vector_status = "configured" if embedding_enabled() else "optional"
    return {
        "stages": [
            {"id": "document", "label": "Document", "status": "ready", "description": "PDF、文本和结构化文本资料"},
            {"id": "parse", "label": "Parse / Layout", "status": "ready", "description": "PyMuPDF 页码与 bbox；可选 MinerU /file_parse"},
            {"id": "chunk", "label": "Chunk", "status": "ready", "description": "Yuxi-derived preset、估算 token 边界与 source spans"},
            {"id": "embedding", "label": "Embedding", "status": vector_status, "description": "可选 OpenAI-compatible Embedding"},
            {"id": "bm25", "label": "BM25", "status": "ready", "description": "本地关键词召回"},
            {"id": "vector", "label": "Vector", "status": vector_status, "description": "仅在真实 Embedding 已配置时运行"},
            {"id": "fusion", "label": "Fusion", "status": "ready", "description": "配置向量时按权重融合 BM25 与 Vector"},
            {"id": "rerank", "label": "Rerank", "status": "configured" if rerank_enabled() else "optional", "description": "OpenAI-like 或 DashScope 协议；显式请求但未配置会报错"},
            {"id": "evidence", "label": "Evidence", "status": "ready", "description": "原文、Evidence ID、PDF page 与 bbox"},
            {"id": "context", "label": "Final Context", "status": "ready", "description": "发给下游 Agent 的最终 Evidence Context"},
        ]
    }


@app.get("/api/v1/chunk-presets")
async def chunk_presets():
    return {"chunk_presets": get_options()}


@app.get("/api/v1/types")
async def knowledge_base_types():
    return {"types": {
        "local": {"name": "Geo RAG 本地向量知识库", "description": "本地文件、SQLite BM25 与可选 Embedding 向量检索", "supports_documents": True, "requires_embedding_model": False},
        "dify": {"name": "Dify Dataset", "description": "只读连接 Dify Dataset Retrieve API", "supports_documents": False, "requires_embedding_model": False},
        "notion": {"name": "Notion", "description": "只读连接 Notion workspace 页面搜索", "supports_documents": False, "requires_embedding_model": False},
    }}


@app.get("/api/v1/files/supported-types")
async def supported_file_types():
    return {"extensions": sorted(SUPPORTED_UPLOAD_TYPES), "max_upload_bytes": MAX_UPLOAD_BYTES}


@app.get("/api/v1/stats")
async def knowledge_statistics():
    knowledge_bases = db.list_knowledge_bases()
    documents = db.list_documents()
    return {
        "knowledge_base_count": len(knowledge_bases),
        "document_count": len(documents),
        "chunk_count": sum(item["chunk_count"] for item in documents),
        "token_count": sum(item["token_count"] for item in documents),
        "status_counts": {status: sum(item["status"] == status for item in documents)
                          for status in ("uploaded", "parsing", "parsed", "chunking", "embedding", "indexed", "failed")},
    }


def _knowledge_base_or_404(knowledge_base_id: str) -> dict:
    knowledge_base = db.get_knowledge_base(knowledge_base_id)
    if not knowledge_base:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    return knowledge_base


@app.get("/api/v1/knowledge-bases")
async def knowledge_bases():
    return [_public_knowledge_base(item) for item in db.list_knowledge_bases()]


@app.post("/api/v1/knowledge-bases")
async def create_knowledge_base(request: KnowledgeBasePayload):
    name = request.name.strip()
    if not name:
        raise HTTPException(status_code=422, detail="知识库名称不能为空")
    try:
        config = _merge_config(db.default_knowledge_base_config(), request.config)
        _validate_model_config(config)
    except (TypeError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    try:
        knowledge_base = db.create_knowledge_base(name, request.description.strip(), request.kb_type)
    except sqlite3.IntegrityError as exc:
        raise HTTPException(status_code=409, detail="知识库名称已存在") from exc
    if request.config:
        knowledge_base = db.update_knowledge_base(
            knowledge_base["id"], knowledge_base["name"], knowledge_base["description"], knowledge_base["kb_type"], config
        )
    return _public_knowledge_base(knowledge_base)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}")
async def get_knowledge_base(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    knowledge_base = next(item for item in db.list_knowledge_bases() if item["id"] == knowledge_base_id)
    return _public_knowledge_base(knowledge_base)


@app.put("/api/v1/knowledge-bases/{knowledge_base_id}")
async def update_knowledge_base(knowledge_base_id: str, request: KnowledgeBaseUpdatePayload):
    current = _knowledge_base_or_404(knowledge_base_id)
    try:
        config = _merge_config(current["config"], request.config)
        _validate_model_config(config)
    except (TypeError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    name = request.name.strip() if request.name else current["name"]
    description = request.description.strip() if request.description is not None else current["description"]
    try:
        updated = db.update_knowledge_base(knowledge_base_id, name, description, current["kb_type"], config)
    except sqlite3.IntegrityError as exc:
        raise HTTPException(status_code=409, detail="知识库名称已存在") from exc
    return _public_knowledge_base(updated)


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}")
async def delete_knowledge_base(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    if not db.delete_knowledge_base(knowledge_base_id):
        raise HTTPException(status_code=409, detail="请先删除知识库中的文件，再删除知识库")
    return {"deleted": True}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/folders")
async def folders(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return db.list_folders(knowledge_base_id)


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/folders")
async def create_folder(knowledge_base_id: str, request: FolderPayload):
    _knowledge_base_or_404(knowledge_base_id)
    if not request.name.strip():
        raise HTTPException(status_code=422, detail="文件夹名称不能为空")
    if request.parent_id and not db.folder_exists(request.parent_id, knowledge_base_id):
        raise HTTPException(status_code=404, detail="Parent folder not found")
    try:
        return db.create_folder(knowledge_base_id, request.name.strip(), request.parent_id)
    except sqlite3.IntegrityError as exc:
        raise HTTPException(status_code=409, detail="同级目录下已存在同名文件夹") from exc


@app.put("/api/v1/knowledge-bases/{knowledge_base_id}/folders/{folder_id}/rename")
async def rename_folder(knowledge_base_id: str, folder_id: str, request: FolderRenamePayload):
    _knowledge_base_or_404(knowledge_base_id)
    try:
        folder = db.rename_folder(folder_id, knowledge_base_id, request.name.strip())
    except sqlite3.IntegrityError as exc:
        raise HTTPException(status_code=409, detail="同级目录下已存在同名文件夹") from exc
    if not folder:
        raise HTTPException(status_code=404, detail="Folder not found")
    return folder


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}/folders/{folder_id}")
async def remove_folder(knowledge_base_id: str, folder_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    if not db.delete_folder(folder_id, knowledge_base_id):
        raise HTTPException(status_code=409, detail="文件夹不存在或仍包含文件/子文件夹")
    return {"deleted": True}


@app.put("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}/move")
async def move_knowledge_base_document(knowledge_base_id: str, document_id: str, request: DocumentMovePayload):
    _knowledge_base_or_404(knowledge_base_id)
    if request.folder_id and not db.folder_exists(request.folder_id, knowledge_base_id):
        raise HTTPException(status_code=404, detail="Folder not found")
    if not db.move_document(document_id, knowledge_base_id, request.folder_id):
        raise HTTPException(status_code=404, detail="Document not found")
    return {"document_id": document_id, "folder_id": request.folder_id}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents")
async def knowledge_base_documents(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return [_public_document(item) for item in db.list_documents(knowledge_base_id)]


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/search")
async def search_knowledge_base_documents(knowledge_base_id: str, q: str = ""):
    _knowledge_base_or_404(knowledge_base_id)
    query = q.strip().casefold()
    items = db.list_documents(knowledge_base_id)
    if query:
        items = [item for item in items if query in item["file_name"].casefold() or query in str(item.get("metadata", {}).get("relative_path", "")).casefold()]
    return [_public_document(item) for item in items]


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/exists")
async def knowledge_base_document_exists(knowledge_base_id: str, filename: str):
    _knowledge_base_or_404(knowledge_base_id)
    exists = any(item["file_name"] == filename or item.get("metadata", {}).get("relative_path") == filename
                 for item in db.list_documents(knowledge_base_id))
    return {"exists": exists, "filename": filename}


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}/documents/batch")
async def batch_delete_knowledge_base_documents(knowledge_base_id: str, request: DocumentBatchPayload):
    _knowledge_base_or_404(knowledge_base_id)
    deleted = []
    missing = []
    upload_root = Path(settings.rag_upload_dir).resolve()
    for document_id in dict.fromkeys(request.document_ids):
        document = db.get_document(document_id)
        if not document or document["knowledge_base_id"] != knowledge_base_id:
            missing.append(document_id)
            continue
        removed = db.delete_document(document_id)
        file_path = Path(removed.get("file_path") or "").resolve()
        if file_path.is_file() and file_path.is_relative_to(upload_root):
            file_path.unlink()
        deleted.append(document_id)
    return {"deleted": deleted, "not_found": missing}


async def _process_document_batch(knowledge_base_id: str, document_ids: list[str], operation: str):
    _knowledge_base_or_404(knowledge_base_id)
    processed = []
    failures = []
    for document_id in dict.fromkeys(document_ids):
        document = db.get_document(document_id)
        if not document or document["knowledge_base_id"] != knowledge_base_id:
            failures.append({"document_id": document_id, "error_message": "Document not found"})
            continue
        try:
            result = await (parse_existing_document(document_id) if operation == "parse" else index_existing_document(document_id))
            processed.append(result)
        except Exception as exc:
            failures.append({"document_id": document_id, "error_message": str(exc)[:500]})
    return {"operation": operation, "processed": processed, "failed": failures}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/documents/parse")
async def parse_knowledge_base_documents(knowledge_base_id: str, request: DocumentBatchPayload):
    return await _process_document_batch(knowledge_base_id, request.document_ids, "parse")


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/documents/parse-pending")
async def parse_pending_knowledge_base_documents(knowledge_base_id: str):
    items = [doc["id"] for doc in db.list_documents(knowledge_base_id) if doc["status"] in {"uploaded", "failed"}]
    return await _process_document_batch(knowledge_base_id, items, "parse")


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/documents/index")
async def index_knowledge_base_documents(knowledge_base_id: str, request: DocumentBatchPayload):
    return await _process_document_batch(knowledge_base_id, request.document_ids, "index")


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/documents/index-pending")
async def index_pending_knowledge_base_documents(knowledge_base_id: str):
    items = [doc["id"] for doc in db.list_documents(knowledge_base_id) if doc["status"] == "parsed"]
    return await _process_document_batch(knowledge_base_id, items, "index")


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}/chunks")
async def knowledge_base_document_chunks(knowledge_base_id: str, document_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    document = db.get_document(document_id)
    if not document or document["knowledge_base_id"] != knowledge_base_id:
        raise HTTPException(status_code=404, detail="Document not found")
    return db.get_chunks(document_id, include_embedding=False)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}/basic")
async def knowledge_base_document_basic(knowledge_base_id: str, document_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    document = db.get_document(document_id)
    if not document or document["knowledge_base_id"] != knowledge_base_id:
        raise HTTPException(status_code=404, detail="Document not found")
    return _public_document(document)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}/content")
async def knowledge_base_document_content(knowledge_base_id: str, document_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    document = db.get_document(document_id)
    if not document or document["knowledge_base_id"] != knowledge_base_id:
        raise HTTPException(status_code=404, detail="Document not found")
    return {"document_id": document_id, "file_name": document["file_name"],
            "blocks": db.get_blocks(document_id), "text": "\n\n".join(block["text"] for block in db.get_blocks(document_id))}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}/download")
async def download_knowledge_base_document(knowledge_base_id: str, document_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    document = db.get_document(document_id)
    if not document or document["knowledge_base_id"] != knowledge_base_id:
        raise HTTPException(status_code=404, detail="Document not found")
    path = Path(document.get("file_path") or "")
    upload_root = Path(settings.rag_upload_dir).resolve()
    if not path.is_file() or not path.resolve().is_relative_to(upload_root):
        raise HTTPException(status_code=404, detail="原始文件不可下载")
    return FileResponse(path, media_type=document.get("mime_type"), filename=document["file_name"])


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/query-params")
async def get_knowledge_base_query_params(knowledge_base_id: str):
    knowledge_base = _knowledge_base_or_404(knowledge_base_id)
    return knowledge_base.get("config", {}).get("retrieval", {})


@app.put("/api/v1/knowledge-bases/{knowledge_base_id}/query-params")
async def update_knowledge_base_query_params(knowledge_base_id: str, retrieval: dict):
    current = _knowledge_base_or_404(knowledge_base_id)
    try:
        config = _merge_config(current["config"], {"retrieval": retrieval})
        _validate_model_config(config)
    except (TypeError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    saved = db.update_knowledge_base(knowledge_base_id, current["name"], current["description"], current["kb_type"], config)
    return saved["config"]["retrieval"]


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/sample-questions")
async def get_sample_questions(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return {"questions": db.list_sample_questions(knowledge_base_id)}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/sample-questions")
async def save_sample_questions(knowledge_base_id: str, payload: dict):
    _knowledge_base_or_404(knowledge_base_id)
    questions = payload.get("questions")
    if not isinstance(questions, list):
        raise HTTPException(status_code=503, detail="问题生成需要配置 LLM；当前 API 只保存显式提供的问题")
    clean = list(dict.fromkeys(str(item).strip() for item in questions if str(item).strip()))
    db.save_sample_questions(knowledge_base_id, clean)
    return {"questions": clean}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/mindmap/files")
async def mindmap_files(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return [_public_document(item) for item in db.list_documents(knowledge_base_id) if item["status"] == "indexed"]


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/mindmap/generate")
async def generate_knowledge_base_mindmap(knowledge_base_id: str, request: MindMapPayload):
    knowledge_base = _knowledge_base_or_404(knowledge_base_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=501, detail="此只读连接器不支持思维导图生成")
    try:
        result = await generate_mindmap(
            knowledge_base,
            request.document_ids or None,
            request.user_prompt,
            request.incremental,
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except httpx.HTTPStatusError as exc:
        raise HTTPException(status_code=502, detail=f"思维导图模型请求失败（HTTP {exc.response.status_code}）") from exc
    except httpx.RequestError as exc:
        raise HTTPException(status_code=503, detail="思维导图模型暂时不可达") from exc
    saved = db.save_knowledge_view(knowledge_base_id, "mindmap", "", result)
    return {**saved["payload"], "created_at": saved["created_at"]}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/mindmap")
async def get_knowledge_base_mindmap(knowledge_base_id: str, document_id: str | None = None):
    _knowledge_base_or_404(knowledge_base_id)
    views = db.list_knowledge_views(knowledge_base_id, "mindmap")
    if document_id:
        views = [item for item in views if document_id in item["payload"].get("source_document_ids", item["source_id"].split(","))]
    if not views:
        return {"status": "not_generated", "mindmap": None}
    return {"status": "ready", **views[0]["payload"], "created_at": views[0]["created_at"]}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/mindmap/diff")
async def knowledge_base_mindmap_diff(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    views = db.list_knowledge_views(knowledge_base_id, "mindmap")
    current_documents = [item for item in db.list_documents(knowledge_base_id) if item["status"] == "indexed"]
    saved = views[0]["payload"] if views else None
    current_by_id = {item["id"]: item for item in current_documents}
    current_ids = set(current_by_id)
    tracked_ids = set((saved or {}).get("source_document_ids", []))
    added_ids = current_ids - tracked_ids
    removed_ids = tracked_ids - current_ids
    added_files = [
        {"file_id": item, "filename": current_by_id[item]["file_name"], "type": current_by_id[item].get("mime_type", "")}
        for item in sorted(added_ids)
    ]
    current_fingerprint = indexed_content_fingerprint(sorted(current_ids))
    has_content_changes = bool(saved and saved.get("source_fingerprint") != current_fingerprint)
    return {
        "has_mindmap": bool(saved),
        "generated": bool(saved),
        "tracked_files": sorted(tracked_ids),
        "current_files": sorted(current_ids),
        "added_files": added_files,
        "removed_file_ids": sorted(removed_ids),
        "unchanged_count": len(current_ids & tracked_ids),
        "needs_update": bool(added_ids or removed_ids or has_content_changes),
        "changed": bool(saved and saved.get("source_fingerprint") != current_fingerprint),
        "current_total": len(current_ids),
        "current_files_truncated": False,
        "current_fingerprint": current_fingerprint,
        "saved_fingerprint": saved.get("source_fingerprint") if saved else None,
        "kb_id": knowledge_base_id,
        "slug": knowledge_base_id,
        "message": "success",
    }


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/graph")
async def get_knowledge_graph(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    saved = db.get_knowledge_view(knowledge_base_id, "graph")
    return {"status": "ready", **saved["payload"], "created_at": saved["created_at"]} if saved else {"status": "not_built", "nodes": [], "edges": []}


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/status")
async def knowledge_graph_status(knowledge_base_id: str):
    knowledge_base = _knowledge_base_or_404(knowledge_base_id)
    saved = db.get_knowledge_view(knowledge_base_id, "graph")
    documents = [item for item in db.list_documents(knowledge_base_id) if item["status"] == "indexed"]
    document_ids = sorted(item["id"] for item in documents)
    total_chunks = sum(len(db.get_chunks(item["id"], include_embedding=False)) for item in documents)
    config = _public_knowledge_base(knowledge_base).get("config", {}).get("graph", {}).get("graph_build_config")
    if not saved:
        return {
            "status": "idle", "locked": bool(config and config.get("locked")), "config": config,
            "node_count": 0, "edge_count": 0, "source_document_count": 0,
            "total_chunks": total_chunks, "pending_chunks": total_chunks, "indexed_chunks": 0,
            "structured_chunks": 0, "entity_count": 0, "relationship_count": 0,
            "build_task_progress": 0,
            "extraction_counts": {"completed": 0, "failed": 0, "pending": total_chunks, "processing": 0},
            "vector_counts": {"supported": False, "completed": 0, "failed": 0, "pending": 0, "processing": 0},
        }
    graph = saved["payload"]
    failed_count = len(graph.get("failed_chunks", []))
    completed_count = max(0, graph.get("processed_chunk_count", 0) - failed_count)
    outdated = (
        sorted(graph.get("source_document_ids", [])) != document_ids
        or graph.get("source_fingerprint") != indexed_content_fingerprint(document_ids)
    )
    pending_count = total_chunks if outdated else failed_count
    entity_count = sum(1 for node in graph.get("nodes", []) if node.get("type") != "Chunk")
    return {
        "status": "outdated" if outdated else graph.get("status", "indexed"),
        "locked": bool(config and config.get("locked")), "config": config,
        "node_count": len(graph["nodes"]), "edge_count": len(graph["edges"]),
        "source_document_count": len(graph["source_document_ids"]),
        "total_chunks": total_chunks, "pending_chunks": pending_count,
        "indexed_chunks": completed_count if not outdated else 0,
        "structured_chunks": completed_count if not outdated else 0,
        "entity_count": entity_count, "relationship_count": len(graph.get("edges", [])),
        "build_task_progress": 100 if not outdated and graph.get("status") == "indexed" else 0,
        "extraction_counts": {"completed": completed_count if not outdated else 0,
                              "failed": failed_count if not outdated else 0,
                              "pending": pending_count, "processing": 0},
        "vector_counts": {"supported": False, "completed": 0, "failed": 0, "pending": 0, "processing": 0},
        "created_at": saved["created_at"], "generation": graph["generation"],
    }


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/failed-chunks")
async def knowledge_graph_failed_documents(knowledge_base_id: str, limit: int = 10):
    _knowledge_base_or_404(knowledge_base_id)
    saved = db.get_knowledge_view(knowledge_base_id, "graph")
    failed = (saved or {}).get("payload", {}).get("failed_chunks", [])
    return {"kb_id": knowledge_base_id, "samples": failed[:max(1, min(limit, 100))]}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/config")
async def configure_knowledge_graph(knowledge_base_id: str, request: GraphConfigPayload):
    current = _knowledge_base_or_404(knowledge_base_id)
    if current["kb_type"] != "local":
        raise HTTPException(status_code=501, detail="此只读连接器不开放图谱构建")
    options = dict(request.extractor_options)
    if options.get("model_params") is not None and not isinstance(options["model_params"], dict):
        raise HTTPException(status_code=422, detail="图谱模型参数必须是 JSON 对象")
    base_url = str(options.get("base_url") or "").strip()
    if base_url and urlparse(base_url).scheme not in {"http", "https"}:
        raise HTTPException(status_code=422, detail="图谱 LLM Base URL 必须是 http 或 https")
    existing = current.get("config", {}).get("graph", {}).get("graph_build_config", {})
    previous_options = existing.get("extractor_options", {})
    if not str(options.get("api_key") or "").strip() and previous_options.get("api_key"):
        options["api_key"] = previous_options["api_key"]
    graph = {
        "max_keywords_per_document": request.max_keywords_per_document,
        "graph_build_config": {
            "locked": True,
            "extractor_type": request.extractor_type,
            "extractor_options": options,
            "created_at": existing.get("created_at") or db.now_iso(),
            **({"updated_at": db.now_iso()} if existing else {}),
        },
    }
    config = dict(current["config"])
    config["graph"] = graph
    updated = db.update_knowledge_base(knowledge_base_id, current["name"], current["description"], current["kb_type"], config)
    public_graph = _public_knowledge_base(updated)["config"]["graph"]
    return {"message": "图谱抽取配置已保存", "status": "success", "config": public_graph["graph_build_config"], "graph": public_graph}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/index")
async def build_knowledge_graph_index(knowledge_base_id: str):
    knowledge_base = _knowledge_base_or_404(knowledge_base_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=501, detail="此只读连接器不开放图谱构建")
    config = knowledge_base.get("config", {}).get("graph", {}).get("graph_build_config", {})
    if not config.get("locked") or config.get("extractor_type") != "llm":
        raise HTTPException(status_code=409, detail="请先配置并保存 LLM 图谱抽取器")
    try:
        graph = await build_entity_graph(knowledge_base)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    saved = db.save_knowledge_view(knowledge_base_id, "graph", "", graph)
    if graph["status"] == "failed" and not graph["nodes"]:
        raise HTTPException(status_code=502, detail=f"图谱抽取失败：{len(graph['failed_chunks'])} 个 Chunk 均未成功")
    return {"status": graph["status"], "node_count": len(graph["nodes"]), "edge_count": len(graph["edges"]),
            "source_document_count": len(graph["source_document_ids"]), "created_at": saved["created_at"],
            "generation": graph["generation"], "failed_chunk_count": len(graph["failed_chunks"]),
            "processed_chunk_count": graph["processed_chunk_count"]}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/reset")
async def reset_knowledge_graph(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return {"reset": db.delete_knowledge_view(knowledge_base_id, "graph")}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/graph-build/reconcile")
async def reconcile_knowledge_graph(knowledge_base_id: str, mode: str = "failed"):
    return await build_knowledge_graph_index(knowledge_base_id)


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/datasets/upload")
async def upload_evaluation_dataset(knowledge_base_id: str, file: UploadFile = File(...)):
    _knowledge_base_or_404(knowledge_base_id)
    content = await file.read()
    if len(content) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="评估集最大 10 MB")
    suffix = Path(file.filename or "").suffix.lower()
    try:
        if suffix == ".json":
            value = json.loads(content.decode("utf-8-sig"))
            cases = value.get("cases", []) if isinstance(value, dict) else value
            name = str(value.get("name") or Path(file.filename or "evaluation-dataset.json").stem) if isinstance(value, dict) else Path(file.filename or "evaluation-dataset.json").stem
        elif suffix == ".csv":
            rows = csv.DictReader(io.StringIO(content.decode("utf-8-sig")))
            cases = []
            for row in rows:
                raw_ids = row.get("relevant_evidence_ids") or row.get("evidence_ids") or ""
                cases.append({"query": row.get("query", ""), "relevant_evidence_ids": [item.strip() for item in re.split(r"[|;；\n]", raw_ids) if item.strip()], "top_k": int(row.get("top_k") or 5)})
            name = Path(file.filename or "evaluation-dataset.csv").stem
        else:
            raise ValueError("只支持 JSON 或 CSV 评估集")
        if not isinstance(cases, list) or not cases:
            raise ValueError("评估集必须包含 cases 数组")
        validated = [EvaluationCase.model_validate(item) for item in cases]
    except (UnicodeDecodeError, json.JSONDecodeError, TypeError, ValueError, KeyError) as exc:
        raise HTTPException(status_code=422, detail=f"评估集格式无效：{exc}") from exc
    if len(validated) > 100:
        raise HTTPException(status_code=422, detail="评估集最多 100 条")
    dataset = db.create_evaluation_dataset(knowledge_base_id, name[:120], [item.model_dump() for item in validated])
    return {
        "id": dataset["id"],
        "knowledge_base_id": dataset["knowledge_base_id"],
        "name": dataset["name"],
        "case_count": len(dataset["cases"]),
        "created_at": dataset["created_at"],
    }


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/datasets")
async def list_knowledge_base_evaluation_datasets(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return db.list_evaluation_datasets(knowledge_base_id)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/datasets/{dataset_id}")
async def get_knowledge_base_evaluation_dataset(knowledge_base_id: str, dataset_id: str, page: int = 1, page_size: int = 50):
    _knowledge_base_or_404(knowledge_base_id)
    dataset = db.get_evaluation_dataset(knowledge_base_id, dataset_id)
    if not dataset:
        raise HTTPException(status_code=404, detail="Evaluation dataset not found")
    page = max(1, page)
    page_size = max(1, min(page_size, 100))
    return {**dataset, "total": len(dataset["cases"]), "page": page, "page_size": page_size,
            "cases": dataset["cases"][(page - 1) * page_size:page * page_size]}


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/datasets/{dataset_id}")
async def delete_knowledge_base_evaluation_dataset(knowledge_base_id: str, dataset_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    if not db.get_evaluation_dataset(knowledge_base_id, dataset_id):
        raise HTTPException(status_code=404, detail="Evaluation dataset not found")
    db.delete_evaluation_dataset(dataset_id)
    return {"deleted": True}


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/runs")
async def run_knowledge_base_evaluation(knowledge_base_id: str, payload: EvaluationRunPayload):
    _knowledge_base_or_404(knowledge_base_id)
    dataset_id = payload.dataset_id
    cases = payload.cases
    if dataset_id:
        dataset = db.get_evaluation_dataset(knowledge_base_id, dataset_id)
        if not dataset:
            raise HTTPException(status_code=404, detail="Evaluation dataset not found")
        cases = [EvaluationCase.model_validate(item) for item in dataset["cases"]]
    if not cases:
        raise HTTPException(status_code=422, detail="请提供 cases 或 dataset_id")
    scoped_cases = [item.model_copy(update={"filters": {**item.filters, "knowledge_base_id": knowledge_base_id}}) for item in cases]
    result = await evaluation(EvaluationRequest(
        cases=scoped_cases, search_mode=payload.search_mode, use_reranker=payload.use_reranker, recall_top_k=payload.recall_top_k
    ))
    return db.create_evaluation_run(knowledge_base_id, dataset_id, result)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/runs")
async def list_knowledge_base_evaluation_runs(knowledge_base_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    return db.list_evaluation_runs(knowledge_base_id)


@app.get("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/runs/{run_id}")
async def get_knowledge_base_evaluation_run(knowledge_base_id: str, run_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    run = db.get_evaluation_run(knowledge_base_id, run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Evaluation run not found")
    return run


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}/evaluation/runs/{run_id}")
async def delete_knowledge_base_evaluation_run(knowledge_base_id: str, run_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    if not db.delete_evaluation_run(knowledge_base_id, run_id):
        raise HTTPException(status_code=404, detail="Evaluation run not found")
    return {"deleted": True}


async def _dify_retrieve(request: RetrieveRequest, knowledge_base: dict) -> RetrieveResponse:
    dify = knowledge_base.get("config", {}).get("dify", {})
    api_url = str(dify.get("dify_api_url") or "").rstrip("/")
    token = str(dify.get("dify_token") or "")
    dataset_id = str(dify.get("dify_dataset_id") or "")
    if not api_url or not token or not dataset_id:
        raise ProviderUnavailable("Dify 连接未完成：需要 API URL、Token 和 Dataset ID")
    retrieval_config = knowledge_base.get("config", {}).get("retrieval", {})
    search_mode = request.search_mode or retrieval_config.get("search_mode", "hybrid")
    search_methods = {"vector": "semantic_search", "keyword": "keyword_search", "hybrid": "hybrid_search"}
    final_top_k = request.final_top_k or request.top_k or retrieval_config.get("final_top_k", 8)
    payload = {
        "query": request.query.strip(),
        "retrieval_model": {
            "search_method": search_methods[search_mode],
            "top_k": final_top_k,
            "reranking_enable": False,
            "score_threshold_enabled": False,
        },
    }
    headers = {"Authorization": f"Bearer {token}"}
    url = f"{api_url}/datasets/{dataset_id}/retrieve"
    started = time.perf_counter()
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(url, headers=headers, json=payload)
        if response.is_error:
            response.raise_for_status()
        data = response.json()
    records = data.get("records", []) if isinstance(data, dict) else []
    if not isinstance(records, list):
        raise ValueError("Dify 返回的 records 格式无效")
    evidences = []
    for index, record in enumerate(records[:final_top_k], start=1):
        if not isinstance(record, dict):
            continue
        segment = record.get("segment") or {}
        document = segment.get("document") or {}
        text = segment.get("content")
        if not isinstance(text, str) or not text.strip():
            continue
        score = float(record.get("score") or 0.0)
        segment_id = str(segment.get("id") or index)
        document_id = str(document.get("id") or dataset_id)
        evidences.append(
            Evidence(
                evidence_id=f"DIFY-{dataset_id}-{segment_id}",
                document_id=document_id,
                chunk_id=segment_id,
                file_name=str(document.get("name") or "Dify Dataset"),
                page=None,
                text=text,
                token_count=count_tokens(text),
                fusion_score=score,
                source_spans=[],
                scores={"bm25": None, "vector": None, "fusion": score, "rerank": None},
                metadata={"rank": index, "provider": "dify", "dataset_id": dataset_id},
            )
        )
    elapsed = round((time.perf_counter() - started) * 1000, 2)
    final_context = "\n\n".join(
        f"[{item.evidence_id}] {item.file_name}\n{item.text}" for item in evidences
    )
    return RetrieveResponse(
        query=request.query.strip(),
        evidences=evidences,
        retrieval={
            "mode": "dify-read-only",
            "search_mode": search_mode,
            "candidate_count": len(records),
            "recall_top_k": request.recall_top_k or final_top_k,
            "final_top_k": final_top_k,
            "use_reranker": False,
            "rerank_status": "not_supported_by_connector",
        },
        timing_ms={"dify": elapsed, "total": elapsed},
        usage={"embedding_enabled": False, "reranker_enabled": False, "context_tokens": sum(item.token_count for item in evidences)},
        final_context=final_context,
    )


def _notion_block_text(block: dict) -> str:
    block_type = block.get("type")
    value = block.get(block_type, {}) if block_type else {}
    rich_text = value.get("rich_text", [])
    if block_type == "table_row":
        rich_text = [item for cell in value.get("cells", []) for item in cell]
    text = "".join(item.get("plain_text", "") for item in rich_text if isinstance(item, dict))
    return text.strip()


async def _notion_page_text(client: httpx.AsyncClient, base_url: str, page: dict, headers: dict) -> tuple[str, str]:
    title = "Notion page"
    for prop in (page.get("properties") or {}).values():
        if isinstance(prop, dict) and prop.get("type") == "title":
            title = "".join(item.get("plain_text", "") for item in prop.get("title", [])) or title
            break
    page_id = str(page.get("id") or "")
    content = []
    cursor = None
    for _ in range(3):
        params = {"page_size": 100}
        if cursor:
            params["start_cursor"] = cursor
        response = await client.get(f"{base_url}/blocks/{page_id}/children", headers=headers, params=params)
        response.raise_for_status()
        data = response.json()
        for block in data.get("results", []):
            text = _notion_block_text(block)
            if text:
                content.append(text)
            if block.get("has_children"):
                child = await client.get(
                    f"{base_url}/blocks/{block['id']}/children", headers=headers, params={"page_size": 100}
                )
                child.raise_for_status()
                for child_block in child.json().get("results", []):
                    child_text = _notion_block_text(child_block)
                    if child_text:
                        content.append(child_text)
        cursor = data.get("next_cursor") if data.get("has_more") else None
        if not cursor:
            break
    return title, "\n".join([title, *content]).strip()


async def _notion_retrieve(request: RetrieveRequest, knowledge_base: dict) -> RetrieveResponse:
    notion = knowledge_base.get("config", {}).get("notion", {})
    token = str(notion.get("token") or "").strip()
    if not token:
        raise ProviderUnavailable("Notion 连接未配置：请填写 Integration Token")
    base_url = str(notion.get("base_url") or "https://api.notion.com/v1").rstrip("/")
    if not base_url.startswith(("https://", "http://")):
        raise ValueError("Notion API URL 必须是 http 或 https")
    retrieval = knowledge_base.get("config", {}).get("retrieval", {})
    top_k = min(request.final_top_k or request.top_k or retrieval.get("final_top_k", 8), 100)
    headers = {"Authorization": f"Bearer {token}", "Notion-Version": notion.get("version", "2022-06-28")}
    started = time.perf_counter()
    evidences = []
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(
            f"{base_url}/search", headers=headers,
            json={"query": request.query.strip(), "page_size": top_k, "filter": {"property": "object", "value": "page"}},
        )
        response.raise_for_status()
        records = response.json().get("results", [])
        for rank, page in enumerate(records[:top_k], start=1):
            title, text = await _notion_page_text(client, base_url, page, headers)
            if not text.strip():
                continue
            page_id = str(page.get("id") or rank)
            evidence_id = f"NOTION-{page_id}"
            evidences.append(
                Evidence(
                    evidence_id=evidence_id,
                    document_id=page_id,
                    chunk_id=evidence_id,
                    file_name=title,
                    text=text,
                    token_count=count_tokens(text),
                    metadata={"provider": "notion", "rank": rank, "url": page.get("url")},
                )
            )
    elapsed = round((time.perf_counter() - started) * 1000, 2)
    final_context = "\n\n".join(f"[{item.evidence_id}] {item.file_name}\n{item.text}" for item in evidences)
    return RetrieveResponse(
        query=request.query.strip(),
        evidences=evidences,
        retrieval={"mode": "notion-search", "search_mode": "keyword", "candidate_count": len(evidences),
                   "recall_top_k": request.recall_top_k or top_k, "final_top_k": top_k,
                   "use_reranker": False, "rerank_status": "not_supported_by_connector"},
        timing_ms={"notion": elapsed, "total": elapsed},
        usage={"embedding_enabled": False, "reranker_enabled": False,
               "context_tokens": sum(item.token_count for item in evidences)},
        final_context=final_context,
    )


@app.post("/api/v1/knowledge-bases/{knowledge_base_id}/connection-test")
async def test_knowledge_base_connection(knowledge_base_id: str):
    knowledge_base = _knowledge_base_or_404(knowledge_base_id)
    config = knowledge_base.get("config", {})
    try:
        if knowledge_base["kb_type"] == "dify":
            result = await _dify_retrieve(
                RetrieveRequest(query="地灾知识库连接测试", final_top_k=1, filters={"knowledge_base_id": knowledge_base_id}),
                knowledge_base,
            )
            return {"ok": True, "provider": "dify", "evidence_count": len(result.evidences)}
        if knowledge_base["kb_type"] == "notion":
            result = await _notion_retrieve(
                RetrieveRequest(query="地质灾害", final_top_k=1, filters={"knowledge_base_id": knowledge_base_id}),
                knowledge_base,
            )
            return {"ok": True, "provider": "notion", "evidence_count": len(result.evidences)}
        if knowledge_base["kb_type"] != "local":
            raise HTTPException(status_code=501, detail="当前版本暂未接入此只读连接器")
        embedding = config.get("embedding", {})
        if not embedding_enabled(embedding):
            raise ProviderUnavailable("尚未配置 Embedding API；本地 BM25 检索仍可使用")
        vectors = await embed_texts(["地灾知识库连接测试"], embedding)
        if not vectors:
            raise HTTPException(status_code=502, detail="Embedding 服务没有返回向量")
        return {"ok": True, "provider": "embedding", "model": embedding.get("model"), "dimensions": len(vectors[0])}
    except (ProviderUnavailable, ValueError, httpx.HTTPError) as exc:
        raise _provider_error(exc) from exc


@app.delete("/api/v1/knowledge-bases/{knowledge_base_id}/documents/{document_id}")
async def delete_knowledge_base_document(knowledge_base_id: str, document_id: str):
    _knowledge_base_or_404(knowledge_base_id)
    document = db.get_document(document_id)
    if not document or document["knowledge_base_id"] != knowledge_base_id:
        raise HTTPException(status_code=404, detail="Document not found")
    deleted = db.delete_document(document_id)
    file_path = Path(deleted.get("file_path") or "").resolve()
    upload_root = Path(settings.rag_upload_dir).resolve()
    if file_path.is_file() and file_path.is_relative_to(upload_root):
        file_path.unlink()
    return {"deleted": True}


@app.get("/api/v1/documents")
async def documents():
    return [_public_document(document) for document in db.list_documents()]


@app.get("/api/v1/documents/{document_id}/chunks")
async def chunks(document_id: str):
    if not db.get_document(document_id):
        raise HTTPException(status_code=404, detail="Document not found")
    return db.get_chunks(document_id, include_embedding=False)


@app.get("/api/v1/documents/{document_id}/blocks")
async def blocks(document_id: str):
    if not db.get_document(document_id):
        raise HTTPException(status_code=404, detail="Document not found")
    return db.get_blocks(document_id)


async def _ingest_upload(
    file: UploadFile,
    chunk_preset_id: str | None,
    chunk_token_num: int | None,
    overlapped_percent: int | None,
    delimiter: str | None,
    knowledge_base_id: str | None = None,
    folder_id: str | None = None,
    relative_path: str | None = None,
):
    file_name = Path(file.filename or "document").name
    suffix = Path(file_name).suffix.lower()
    if suffix == ".zip":
        raise HTTPException(status_code=400, detail="ZIP 文件夹请使用知识库的‘上传文件夹’入口")
    if suffix not in ALLOWED_UPLOADS:
        raise HTTPException(status_code=400, detail=f"不支持的文件类型：{suffix or 'unknown'}")
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="文件内容为空")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="单文件最大 100 MB")
    knowledge_base = _knowledge_base_or_404(knowledge_base_id) if knowledge_base_id else None
    if knowledge_base and knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="只读连接器不能上传本地文件")
    kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
    chunking = kb_config.get("chunking", {})
    selected_preset = chunking.get("chunk_preset_id", "general") if knowledge_base else (chunk_preset_id or "general")
    chunk_settings = (
        {
            "chunk_token_num": chunking.get("chunk_token_num", 512),
            "overlapped_percent": chunking.get("overlapped_percent", 10),
            "delimiter": chunking.get("delimiter", "\\n"),
        }
        if knowledge_base
        else {
            "chunk_token_num": chunk_token_num if chunk_token_num is not None else 512,
            "overlapped_percent": overlapped_percent if overlapped_percent is not None else 10,
            "delimiter": delimiter if delimiter is not None else "\\n",
        }
    )
    if folder_id and (not knowledge_base_id or not db.folder_exists(folder_id, knowledge_base_id)):
        raise HTTPException(status_code=404, detail="Folder not found")
    safe_relative = None
    if relative_path:
        candidate = PurePosixPath(relative_path.replace("\\", "/"))
        if candidate.is_absolute() or ".." in candidate.parts:
            raise HTTPException(status_code=422, detail="文件相对路径无效")
        file_name = candidate.name or file_name
        safe_relative = str(candidate.parent) if str(candidate.parent) != "." else None
    effective_folder_id = folder_id
    if knowledge_base_id and safe_relative:
        effective_folder_id = db.ensure_folder_path(knowledge_base_id, safe_relative, folder_id)
    upload_path = Path(settings.rag_upload_dir) / f"{uuid.uuid4().hex}{suffix}"
    upload_path.parent.mkdir(parents=True, exist_ok=True)
    upload_path.write_bytes(content)
    parser_config = chunk_settings
    metadata = {"file_size": len(content)}
    if relative_path:
        metadata["relative_path"] = relative_path
    embedding_config = kb_config.get("embedding") if knowledge_base else None
    parser_adapter = kb_config.get("parser") if knowledge_base else None
    try:
        mime_type = file.content_type or mimetypes.guess_type(file_name)[0] or "application/octet-stream"
        if suffix == ".pdf":
            return await ingest_pdf(
                str(upload_path), file_name, selected_preset, parser_config, metadata,
                knowledge_base_id, effective_folder_id, embedding_config, parser_adapter,
            )
        return await ingest_file(
            str(upload_path), file_name, mime_type, selected_preset, parser_config,
            {"source_type": suffix[1:], "stored_file": upload_path.name, **metadata},
            knowledge_base_id, effective_folder_id, embedding_config, parser_adapter,
        )
    except (ValueError, UnicodeDecodeError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise _provider_error(exc) from exc


@app.post("/api/v1/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    chunk_preset_id: str | None = Form(None),
    chunk_token_num: int | None = Form(None),
    overlapped_percent: int | None = Form(None),
    delimiter: str | None = Form(None),
    knowledge_base_id: str | None = Form(None),
    folder_id: str | None = Form(None),
    relative_path: str | None = Form(None),
):
    return await _ingest_upload(
        file, chunk_preset_id, chunk_token_num, overlapped_percent, delimiter,
        knowledge_base_id, folder_id, relative_path,
    )


@app.post("/api/v1/documents/pdf")
async def upload_pdf(
    file: UploadFile = File(...),
    chunk_preset_id: str | None = Form(None),
    chunk_token_num: int | None = Form(None),
    overlapped_percent: int | None = Form(None),
    delimiter: str | None = Form(None),
    knowledge_base_id: str | None = Form(None),
    folder_id: str | None = Form(None),
    relative_path: str | None = Form(None),
):
    if Path(file.filename or "").suffix.lower() != ".pdf":
        raise HTTPException(status_code=400, detail="Only PDF is supported")
    return await _ingest_upload(
        file, chunk_preset_id, chunk_token_num, overlapped_percent, delimiter,
        knowledge_base_id, folder_id, relative_path,
    )


@app.post("/api/v1/documents/text")
async def add_text(request: TextDocumentRequest):
    try:
        knowledge_base = _knowledge_base_or_404(request.knowledge_base_id) if request.knowledge_base_id else None
        if knowledge_base and knowledge_base["kb_type"] != "local":
            raise HTTPException(status_code=400, detail="只读连接器不能上传本地文本")
        if request.folder_id and (not request.knowledge_base_id or not db.folder_exists(request.folder_id, request.knowledge_base_id)):
            raise HTTPException(status_code=404, detail="Folder not found")
        kb_config = knowledge_base.get("config", {}) if knowledge_base else {}
        chunking = kb_config.get("chunking", {})
        return await ingest_text(
            request.file_name,
            request.text,
            chunking.get("chunk_preset_id", "general") if knowledge_base else (request.chunk_preset_id or "general"),
            chunking if knowledge_base else request.chunk_parser_config,
            request.metadata,
            request.knowledge_base_id,
            request.folder_id,
            kb_config.get("embedding") if knowledge_base else None,
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise _provider_error(exc) from exc


@app.get("/api/v1/documents/{document_id}/page/{page_no}/image")
async def page_image(document_id: str, page_no: int):
    document = db.get_document(document_id)
    if not document or document.get("mime_type") != "application/pdf" or not document.get("file_path"):
        raise HTTPException(status_code=404, detail="PDF not found")
    try:
        image = render_pdf_page(document["file_path"], page_no)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return Response(content=image, media_type="image/png")


@app.post("/api/v1/retrieve", response_model=RetrieveResponse)
async def retrieve_api(request: RetrieveRequest):
    try:
        knowledge_base = db.get_knowledge_base(str(request.filters.get("knowledge_base_id"))) if request.filters.get("knowledge_base_id") else None
        if knowledge_base and knowledge_base["kb_type"] == "dify":
            return await _dify_retrieve(request, knowledge_base)
        if knowledge_base and knowledge_base["kb_type"] == "notion":
            return await _notion_retrieve(request, knowledge_base)
        return await retrieve(request)
    except (ProviderUnavailable, ValueError, httpx.HTTPError) as exc:
        raise _provider_error(exc) from exc


@app.post("/api/v1/debug/retrieve")
async def debug_retrieve_api(request: RetrieveRequest):
    try:
        knowledge_base = db.get_knowledge_base(str(request.filters.get("knowledge_base_id"))) if request.filters.get("knowledge_base_id") else None
        if knowledge_base and knowledge_base["kb_type"] == "notion":
            result = await _notion_retrieve(request, knowledge_base)
            ranked = [
                {"rank": index, "evidence_id": item.evidence_id, "document_id": item.document_id,
                 "file_name": item.file_name, "text": item.text, "score": None, "score_type": "not_exposed_by_notion"}
                for index, item in enumerate(result.evidences, start=1)
            ]
            return {
                "request": request.model_dump(),
                "pipeline": [
                    {"stage": "query", "status": "completed", "summary": "Query 已发送至 Notion Search API"},
                    {"stage": "notion_search", "status": "completed", "summary": f"Notion 返回 {len(ranked)} 条页面；API 不提供 BM25/Vector 分阶段分数"},
                    {"stage": "final_context", "status": "completed", "summary": f"Final Context {result.usage['context_tokens']} tokens"},
                ],
                "stages": {"notion_search": ranked},
                "bm25_candidates": [], "vector_candidates": [], "fusion_candidates": [], "rerank_candidates": [],
                "final_evidences": [item.model_dump() for item in result.evidences],
                "timing": result.timing_ms,
                "config_snapshot": {"knowledge_base_id": knowledge_base["id"], "provider": "notion",
                                    "retrieval": knowledge_base.get("config", {}).get("retrieval", {})},
                "result": result.model_dump(),
                "evidences": [item.model_dump() for item in result.evidences],
                "final_context": result.final_context,
            }
        if knowledge_base and knowledge_base["kb_type"] == "dify":
            result = await _dify_retrieve(request, knowledge_base)
            ranked = [
                {
                    "rank": index,
                    "evidence_id": item.evidence_id,
                    "chunk_id": item.chunk_id,
                    "document_id": item.document_id,
                    "file_name": item.file_name,
                    "text": item.text,
                    "score": item.scores.get("fusion"),
                    "score_type": "dify_external_score",
                }
                for index, item in enumerate(result.evidences, start=1)
            ]
            return {
                "request": request.model_dump(),
                "pipeline": [
                    {"stage": "query", "status": "completed", "summary": "Query 已发送至 Dify Dataset"},
                    {"stage": "dify_retrieve", "status": "completed", "summary": f"Dify 返回 {len(ranked)} 条证据；连接器不暴露 BM25/Vector 分阶段名次"},
                    {"stage": "final_context", "status": "completed", "summary": f"Final Context 约 {result.usage['context_tokens']} tokens"},
                ],
                "stages": {"dify_retrieve": ranked},
                "result": result.model_dump(),
                "evidences": [item.model_dump() for item in result.evidences],
                "final_context": result.final_context,
            }
        return await retrieve_debug(request)
    except (ProviderUnavailable, ValueError, httpx.HTTPError) as exc:
        raise _provider_error(exc) from exc


@app.post("/api/v1/evaluation/run")
async def evaluation(request: EvaluationRequest):
    cases = []
    for case in request.cases:
        retrieval_request = RetrieveRequest(
            query=case.query,
            search_mode=request.search_mode,
            final_top_k=case.top_k,
            recall_top_k=request.recall_top_k,
            use_reranker=request.use_reranker,
            filters=case.filters,
        )
        try:
            knowledge_base = db.get_knowledge_base(str(case.filters.get("knowledge_base_id"))) if case.filters.get("knowledge_base_id") else None
            if knowledge_base and knowledge_base["kb_type"] == "dify":
                result = await _dify_retrieve(retrieval_request, knowledge_base)
            elif knowledge_base and knowledge_base["kb_type"] == "notion":
                result = await _notion_retrieve(retrieval_request, knowledge_base)
            else:
                result = await retrieve(retrieval_request)
        except (ProviderUnavailable, ValueError, httpx.HTTPError) as exc:
            raise _provider_error(exc) from exc
        relevant = set(case.relevant_evidence_ids)
        returned = [item.evidence_id for item in result.evidences]
        hits = [index for index, evidence_id in enumerate(returned, start=1) if evidence_id in relevant]
        reciprocal_rank = 1 / hits[0] if hits else 0.0
        dcg = sum(1 / math.log2(rank + 1) for rank in hits)
        ideal = sum(1 / math.log2(rank + 1) for rank in range(1, min(len(relevant), case.top_k) + 1))
        cases.append(
            {
                "query": case.query,
                "returned_evidence_ids": returned,
                "relevant_evidence_ids": sorted(relevant),
                "recall_at_k": len(hits) / len(relevant) if relevant else 0.0,
                "precision_at_k": len(hits) / case.top_k,
                "mrr": reciprocal_rank,
                "ndcg_at_k": dcg / ideal if ideal else 0.0,
                "top_k": case.top_k,
            }
        )
    averages = {
        metric: round(sum(case[metric] for case in cases) / len(cases), 6)
        for metric in ("recall_at_k", "precision_at_k", "mrr", "ndcg_at_k")
    }
    return {"case_count": len(cases), "averages": averages, "cases": cases, "judge": "ground-truth evidence IDs; no LLM judge"}


@app.get("/api/v1/evidence/{evidence_id}")
async def evidence(evidence_id: str):
    chunk_id = evidence_id[3:] if evidence_id.startswith("EV-") else evidence_id
    item = db.get_chunk(chunk_id)
    if not item or item.get("document_status") != "indexed":
        raise HTTPException(status_code=404, detail="Evidence not found")
    return {
        "evidence_id": f"EV-{item['id']}",
        "document_id": item["document_id"],
        "chunk_id": item["id"],
        "file_name": item.get("file_name"),
        "text": item["text"],
        "token_count": item["token_count"],
        "source_spans": item["source_spans"],
        "metadata": item["metadata"],
    }


from .yuxi_compat import router as yuxi_compat_router

app.include_router(yuxi_compat_router)
