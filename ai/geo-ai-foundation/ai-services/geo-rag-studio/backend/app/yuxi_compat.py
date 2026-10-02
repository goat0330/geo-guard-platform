"""Yuxi Knowledge Base API compatibility for the local SQLite RAG service."""

import hashlib
import csv
import io
import ipaddress
import json
import mimetypes
import re
import shutil
import socket
import stat
import tempfile
import uuid
import zipfile
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path, PurePosixPath
from urllib.parse import quote, unquote, urlsplit

import httpx
from fastapi import APIRouter, Body, File, Form, HTTPException, Query, UploadFile
from fastapi.responses import FileResponse, Response
from pydantic import BaseModel, Field

from . import db, main as rag
from .chat import ChatProviderUnavailable, complete_chat
from .config import settings
from .schemas import DocumentBatchPayload, DocumentMovePayload, EmbeddingTestRequest, EvaluationRunPayload, GraphConfigPayload, KnowledgeBasePayload, KnowledgeBaseUpdatePayload, MindMapPayload, RerankerTestRequest, RetrieveRequest
from .yuxi_port.chunk_presets import get_options
from .parser import parse_file_markdown
from .pipeline import document_processing_params
from yuxi.knowledge.utils.kb_utils import calculate_content_hash, params_for_uploaded_document

router = APIRouter()
STAGE_URI = "local-stage://"
DEFAULT_SHARE_CONFIG = {"version": 2, "read_scope": {"access_level": "global", "department_ids": [], "user_uids": []}, "manage_scope": None}


def _yuxi_document_status(document: dict) -> str:
    status = document.get("status", "uploaded")
    if status in {"chunking", "embedding"}:
        return "indexing"
    if status == "failed":
        phase = (document.get("metadata") or {}).get("failure_stage")
        return {"parse": "error_parsing", "index": "error_indexing"}.get(phase, status)
    return status


def _yuxi_document(document: dict) -> dict:
    metadata = document.get("metadata") or {}
    file_path = Path(document.get("file_path") or "")
    try:
        file_size = int(metadata.get("file_size") or file_path.stat().st_size)
    except (OSError, TypeError, ValueError):
        file_size = 0
    file_name = document.get("file_name") or ""
    return {
        "file_id": document["id"],
        "document_id": document["id"],
        "filename": file_name,
        "file_name": file_name,
        "file_type": file_path.suffix.lower().lstrip("."),
        "mime_type": document.get("mime_type"),
        "file_size": file_size,
        "size": file_size,
        "status": _yuxi_document_status(document),
        "parser": document.get("parser"),
        "chunk_preset_id": document.get("chunk_preset_id"),
        "processing_params": metadata.get("processing_params") or {
            "chunk_preset_id": document.get("chunk_preset_id"),
            "chunk_parser_config": document.get("chunk_parser_config") or {},
        },
        "chunk_count": document.get("chunk_count", 0),
        "token_count": document.get("token_count", 0),
        "embedding_chunk_count": document.get("embedding_chunk_count", 0),
        "error_message": document.get("error_message"),
        "created_at": document.get("created_at"),
        "updated_at": document.get("updated_at"),
        "folder_id": document.get("folder_id"),
        "parent_id": document.get("folder_id"),
        "is_folder": False,
        "has_original_file": file_path.is_file() and file_path.resolve().is_relative_to(Path(settings.rag_upload_dir).resolve()),
        "has_parsed_markdown": bool(db.get_blocks(document["id"])),
        "metadata": metadata,
    }


def _yuxi_folder(folder: dict) -> dict:
    return {
        "file_id": folder["id"],
        "filename": folder["name"],
        "file_name": folder["name"],
        "is_folder": True,
        "status": "indexed",
        "parent_id": folder.get("parent_id"),
        "folder_id": folder.get("parent_id"),
        "created_at": folder.get("created_at"),
    }


def _kb_stats(knowledge_base_id: str) -> dict:
    documents = db.list_documents(knowledge_base_id)
    folders = db.list_folders(knowledge_base_id)
    total_size = sum(int((item.get("metadata") or {}).get("file_size") or 0) for item in documents)
    status_counts = {status: sum(_yuxi_document_status(item) == status for item in documents)
                     for status in ("uploaded", "parsing", "parsed", "indexing", "indexed", "error_parsing", "error_indexing", "failed")}
    return {
        "file_count": len(documents), "folder_count": len(folders), "row_count": len(documents) + len(folders), "total_size": total_size,
        "chunk_count": sum(item.get("chunk_count", 0) for item in documents),
        "token_count": sum(item.get("token_count", 0) for item in documents),
        "pending_parse_count": status_counts["uploaded"],
        "pending_index_count": status_counts["parsed"] + status_counts["error_indexing"],
        "processing_count": status_counts["parsing"] + status_counts["indexing"],
        "status_counts": status_counts,
    }


def _yuxi_knowledge_base(knowledge_base: dict, include_files: bool = False) -> dict:
    public = rag._public_knowledge_base(knowledge_base)
    config = public.get("config") or {}
    embedding = config.get("embedding") or {}
    chunking = config.get("chunking") or {}
    parser = config.get("parser") or {}
    reranker = config.get("reranker") or {}
    dify = config.get("dify") or {}
    notion = config.get("notion") or {}
    additional_params = {
        "chunk_preset_id": chunking.get("chunk_preset_id", "general"),
        "chunk_token_num": chunking.get("chunk_token_num", 512),
        "overlapped_percent": chunking.get("overlapped_percent", 10),
        "delimiter": chunking.get("delimiter", "\\n"),
        "embedding_base_url": embedding.get("base_url", ""),
        "embedding_model": embedding.get("model", ""),
        "embedding_dimensions": embedding.get("dimensions"),
        "embedding_api_key_set": embedding.get("api_key_set", False),
        "parser_engine": parser.get("engine", "auto"),
        "mineru_api_uri": parser.get("mineru_api_uri", ""),
        "mineru_api_key_set": parser.get("api_key_set", False),
        "reranker_base_url": reranker.get("base_url", ""),
        "reranker_model": reranker.get("model", ""),
        "reranker_protocol": reranker.get("protocol", "openai"),
        "reranker_api_key_set": reranker.get("api_key_set", False),
        "dify_api_url": dify.get("dify_api_url", ""),
        "dify_token_set": dify.get("dify_token_set", False),
        "dify_dataset_id": dify.get("dify_dataset_id", ""),
        "notion_data_source_id": notion.get("data_source_id", ""),
        "notion_version": notion.get("version", "2026-03-11"),
        "notion_token_set": notion.get("token_set", False),
    }
    result = {
        **public,
        "kb_id": public["id"],
        "embedding_model_spec": embedding.get("model") or "",
        "additional_params": additional_params,
        "share_config": config.get("share_config") or DEFAULT_SHARE_CONFIG,
        "can_manage": True,
        "row_count": public.get("document_count", 0),
        "stats": _kb_stats(public["id"]),
    }
    if include_files:
        result["files"] = {item["id"]: _yuxi_document(item) for item in db.list_documents(public["id"])}
    return result


def _config_from_yuxi(payload: dict, current: dict | None = None) -> dict:
    config = dict(current or db.default_knowledge_base_config())
    additional = payload.get("additional_params") or {}
    embedding = dict(config.get("embedding") or {})
    for source, target in (("embedding_base_url", "base_url"), ("embedding_model", "model"), ("embedding_api_key", "api_key"), ("embedding_dimensions", "dimensions")):
        if source in additional:
            embedding[target] = additional[source]
    if payload.get("embedding_model_spec"):
        embedding["model"] = payload["embedding_model_spec"]
    config["embedding"] = embedding

    chunking = dict(config.get("chunking") or {})
    for source, target in (("chunk_preset_id", "chunk_preset_id"), ("chunk_token_num", "chunk_token_num"), ("overlapped_percent", "overlapped_percent"), ("delimiter", "delimiter")):
        if source in additional:
            chunking[target] = additional[source]
    if payload.get("chunk_preset_id"):
        chunking["chunk_preset_id"] = payload["chunk_preset_id"]
    config["chunking"] = chunking

    parser = dict(config.get("parser") or {})
    for source, target in (("parser_engine", "engine"), ("mineru_api_uri", "mineru_api_uri"), ("mineru_api_key", "api_key")):
        if source in additional:
            parser[target] = additional[source]
    config["parser"] = parser

    reranker = dict(config.get("reranker") or {})
    for source, target in (("reranker_base_url", "base_url"), ("reranker_model", "model"), ("reranker_api_key", "api_key"), ("reranker_protocol", "protocol")):
        if source in additional:
            reranker[target] = additional[source]
    config["reranker"] = reranker

    dify = dict(config.get("dify") or {})
    for source, target in (("dify_api_url", "dify_api_url"), ("dify_token", "dify_token"), ("dify_dataset_id", "dify_dataset_id")):
        if source in additional:
            dify[target] = additional[source]
    config["dify"] = dify

    notion = dict(config.get("notion") or {})
    for source, target in (("notion_token", "token"), ("notion_data_source_id", "data_source_id"), ("notion_version", "version")):
        if source in additional:
            notion[target] = additional[source]
    config["notion"] = notion
    if "share_config" in payload:
        share_config = payload["share_config"]
        if not isinstance(share_config, dict):
            raise HTTPException(status_code=422, detail="share_config 必须是对象")
        config["share_config"] = share_config
    return rag._merge_config(config, {})


@router.get("/api/knowledge/types")
async def yuxi_knowledge_types():
    return {"kb_types": {
        "local": {"name": "本地向量知识库", "description": "SQLite 与本地文件存储；支持 PDF、文本、分块与混合检索", "supports_documents": True, "requires_embedding_model": False, "create_params": {"options": []}},
        "dify": {"name": "Dify Dataset", "description": "连接 Dify Dataset Retrieve API", "supports_documents": False, "requires_embedding_model": False, "create_params": {"options": [
            {"key": "dify_api_url", "label": "Dify API URL", "type": "text", "required": True},
            {"key": "dify_token", "label": "Dify Token", "type": "password", "required": True},
            {"key": "dify_dataset_id", "label": "Dataset ID", "type": "text", "required": True},
        ]}},
        "notion": {"name": "Notion", "description": "连接 Notion Data Source 搜索 API", "supports_documents": False, "requires_embedding_model": False, "create_params": {"options": [
            {"key": "notion_token", "label": "Notion Token", "type": "password", "required": True},
            {"key": "notion_data_source_id", "label": "Data Source ID", "type": "text", "required": True},
            {"key": "notion_version", "label": "Notion API Version", "type": "text", "required": False, "default": "2026-03-11"},
        ]}},
    }}


@router.get("/api/knowledge/chunk-presets")
async def yuxi_chunk_presets():
    options = get_options()
    return {"chunk_presets": options, "presets": options}


@router.get("/api/knowledge/files/supported-types")
async def yuxi_supported_file_types():
    return {"message": "success", "file_types": sorted(rag.SUPPORTED_UPLOAD_TYPES)}


@router.post("/api/knowledge/files/markdown")
async def yuxi_parse_file_markdown(
    file: UploadFile = File(...),
    kb_id: str | None = Form(None),
    engine: str | None = Form(None),
    mineru_api_uri: str | None = Form(None),
    mineru_api_key: str | None = Form(None),
):
    file_name = Path(file.filename or "").name
    suffix = Path(file_name).suffix.lower()
    if not file_name or suffix not in rag.ALLOWED_UPLOADS:
        raise HTTPException(status_code=415, detail=f"不支持的文件类型：{suffix or 'unknown'}")
    content = await file.read()
    if not content:
        raise HTTPException(status_code=422, detail="上传文件为空")
    if len(content) > rag.MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="文件超过 100 MB")
    parser_options = {}
    if kb_id:
        knowledge_base = rag._knowledge_base_or_404(kb_id)
        parser_options = dict((knowledge_base.get("config") or {}).get("parser") or {})
        parser_options["knowledge_base_id"] = kb_id
    if engine:
        parser_options["engine"] = engine
    if mineru_api_uri:
        parser_options["mineru_api_uri"] = mineru_api_uri
    if mineru_api_key:
        parser_options["api_key"] = mineru_api_key
    Path(settings.rag_upload_dir).mkdir(parents=True, exist_ok=True)
    handle = tempfile.NamedTemporaryFile(delete=False, suffix=suffix, dir=settings.rag_upload_dir)
    temp_path = Path(handle.name)
    try:
        with handle:
            handle.write(content)
        markdown_content = await parse_file_markdown(str(temp_path), parser_options)
        return {"markdown_content": markdown_content, "message": "success"}
    except (ValueError, httpx.HTTPError) as exc:
        raise HTTPException(status_code=422, detail=f"文档解析失败：{exc}") from exc
    finally:
        temp_path.unlink(missing_ok=True)


@router.get("/api/knowledge/stats")
async def yuxi_knowledge_stats():
    stats = await rag.knowledge_statistics()
    return {**stats, "databases_count": stats["knowledge_base_count"], "files_count": stats["document_count"]}


@router.get("/api/dashboard/stats/knowledge")
async def yuxi_knowledge_dashboard_stats():
    from yuxi.services.knowledge_dashboard_service import DATABASE_TYPE_MAPPING, FILE_TYPE_MAPPING

    databases_by_type = {}
    for database in db.list_knowledge_bases():
        kb_type = (database.get("kb_type") or "unknown").lower()
        display_type = DATABASE_TYPE_MAPPING.get(kb_type, kb_type or "未知类型")
        databases_by_type[display_type] = databases_by_type.get(display_type, 0) + 1

    file_type_distribution = {}
    total_storage_size = 0
    total_nodes = 0
    documents = db.list_documents()
    for document in documents:
        extension = Path(document.get("file_name") or "").suffix.lower().lstrip(".")
        display_type = FILE_TYPE_MAPPING.get(
            extension,
            extension.upper() + "文件" if extension and extension != "unknown" else "其他",
        )
        file_type_distribution[display_type] = file_type_distribution.get(display_type, 0) + 1
        metadata = document.get("metadata") or {}
        file_path = document.get("file_path")
        file_size = metadata.get("file_size")
        if file_size is None:
            file_size = Path(file_path).stat().st_size if file_path and Path(file_path).is_file() else 0
        total_storage_size += int(file_size)
        total_nodes += int(document.get("chunk_count") or 0)

    return {
        "total_databases": sum(databases_by_type.values()),
        "total_files": len(documents),
        "total_nodes": total_nodes,
        "total_storage_size": total_storage_size,
        "databases_by_type": databases_by_type,
        "file_type_distribution": file_type_distribution,
    }


@router.get("/api/knowledge/databases")
async def yuxi_list_databases():
    return {"databases": [_yuxi_knowledge_base(item) for item in db.list_knowledge_bases()]}


@router.get("/api/knowledge/databases/accessible")
async def yuxi_list_accessible_databases():
    return {"databases": [_yuxi_knowledge_base(item) for item in db.list_knowledge_bases()]}


class ExternalRetrieveRequest(BaseModel):
    query: str
    file_name: str | None = None
    options: dict | None = None


class ExternalFindRequest(BaseModel):
    patterns: list[str]
    use_regex: bool = False
    case_sensitive: bool = False
    max_windows: int = Field(default=5, ge=1, le=20)
    window_size: int = Field(default=80, ge=1, le=200)


@router.get("/api/knowledge/databases/external")
async def yuxi_list_external_databases():
    return {
        "databases": [
            {
                "kb_id": item["id"],
                "name": item["name"],
                "description": item.get("description") or "",
                "kb_type": item["kb_type"],
                "supports_documents": item["kb_type"] == "local",
            }
            for item in db.list_knowledge_bases()
        ]
    }


@router.get("/api/knowledge/databases/external/{kb_id}/files")
async def yuxi_list_external_files(
    kb_id: str,
    query: str | None = None,
    offset: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=500),
    status: str = "all",
):
    knowledge_base = rag._knowledge_base_or_404(kb_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail=f"{knowledge_base['name']} 只支持检索，不支持文档查看")
    normalized_query = (query or "").strip().casefold()
    accepted_statuses = {
        "indexed": {"indexed", "done"},
    }.get(status, None if status == "all" else {status})
    documents = db.list_documents(kb_id)
    files = []
    for document in documents:
        if normalized_query and normalized_query not in document["file_name"].casefold():
            continue
        if accepted_statuses is not None and _yuxi_document_status(document) not in accepted_statuses:
            continue
        public_document = _yuxi_document(document)
        files.append(
            {
                "kb_id": kb_id,
                "kb_name": knowledge_base["name"],
                "file_id": document["id"],
                "filename": document["file_name"],
                "file_type": public_document["file_type"],
                "status": public_document["status"],
                "created_at": document.get("created_at"),
                "updated_at": document.get("updated_at"),
                "file_size": public_document["file_size"],
                "is_folder": False,
                "parent_id": document.get("folder_id"),
            }
        )
    return {
        "files": files[offset : offset + limit],
        "total": len(files),
        "offset": offset,
        "limit": limit,
        "has_more": offset + limit < len(files),
    }


@router.post("/api/knowledge/databases/external/{kb_id}/retrieve")
async def yuxi_retrieve_external(kb_id: str, payload: ExternalRetrieveRequest):
    knowledge_base = rag._knowledge_base_or_404(kb_id)
    query = payload.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query is required")
    options = payload.options or {}
    filters = {"knowledge_base_id": kb_id}
    if payload.file_name:
        filters["file_name"] = payload.file_name
    request = RetrieveRequest(
        query=query,
        search_mode=options.get("search_mode"),
        top_k=options.get("top_k"),
        final_top_k=options.get("final_top_k"),
        recall_top_k=options.get("recall_top_k"),
        use_reranker=options.get("use_reranker"),
        similarity_threshold=options.get("similarity_threshold"),
        filters=filters,
    )
    response = await rag.retrieve_api(request)
    from yuxi.knowledge.base import KnowledgeBase

    results = [
        {
            "id": evidence.chunk_id,
            "content": evidence.text,
            "score": evidence.fusion_score,
            "metadata": {
                "file_id": evidence.document_id,
                "chunk_id": evidence.chunk_id,
                "file_name": evidence.file_name,
                "page": evidence.page,
                "bbox": evidence.bbox,
            },
        }
        for evidence in response.evidences
    ]
    return KnowledgeBase.build_search_output(kb_id, results)


def _external_document_text(kb_id: str, file_id: str) -> str:
    knowledge_base = rag._knowledge_base_or_404(kb_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail=f"{knowledge_base['name']} 只支持检索，不支持文档查看")
    document = db.get_document(file_id)
    if not document or document.get("knowledge_base_id") != kb_id:
        raise HTTPException(status_code=404, detail=f"文件不存在: {file_id}")
    blocks = db.get_blocks(file_id)
    if not blocks:
        raise HTTPException(status_code=400, detail=f"文件 {file_id} 没有解析后的 Markdown 内容")
    return "\n\n".join(block["text"] for block in blocks)


@router.get("/api/knowledge/databases/external/{kb_id}/files/{file_id}/open")
async def yuxi_open_external_file(
    kb_id: str,
    file_id: str,
    offset: int = Query(0, ge=0),
    limit: int = Query(200, ge=1, le=1800),
):
    content = _external_document_text(kb_id, file_id)
    from yuxi.knowledge.base import KnowledgeBase

    window = KnowledgeBase._build_open_file_window(None, content, offset=offset, limit=limit)
    return {"kb_id": kb_id, "file_id": file_id, **window}


@router.post("/api/knowledge/databases/external/{kb_id}/files/{file_id}/find")
async def yuxi_find_external_file(kb_id: str, file_id: str, payload: ExternalFindRequest):
    if not payload.patterns:
        raise HTTPException(status_code=400, detail="patterns 不能为空")
    content = _external_document_text(kb_id, file_id)
    from yuxi.knowledge.base import KnowledgeBase

    try:
        result = KnowledgeBase._build_find_file_windows(
            content,
            patterns=payload.patterns,
            use_regex=payload.use_regex,
            case_sensitive=payload.case_sensitive,
            max_windows=payload.max_windows,
            window_size=payload.window_size,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except re.error as exc:
        raise HTTPException(status_code=400, detail=f"正则表达式无效: {exc}") from exc
    return {"kb_id": kb_id, "file_id": file_id, **result}


@router.post("/api/knowledge/databases")
async def yuxi_create_database(payload: dict = Body(...)):
    name = str(payload.get("database_name") or payload.get("name") or "").strip()
    if not name:
        raise HTTPException(status_code=422, detail="知识库名称不能为空")
    kb_type = str(payload.get("kb_type") or "local")
    if kb_type not in {"local", "dify", "notion"}:
        raise HTTPException(status_code=422, detail="当前本地服务不支持该知识库类型")
    config = _config_from_yuxi(payload)
    request = KnowledgeBasePayload(name=name, description=str(payload.get("description") or ""), kb_type=kb_type, config=config)
    created = await rag.create_knowledge_base(request)
    return _yuxi_knowledge_base(created, include_files=True)


@router.get("/api/knowledge/databases/{knowledge_base_id}")
async def yuxi_get_database(knowledge_base_id: str):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    return _yuxi_knowledge_base(knowledge_base, include_files=True)


@router.get("/api/knowledge/databases/{knowledge_base_id}/export")
async def yuxi_export_database(
    knowledge_base_id: str,
    format: str = Query("csv", pattern="^(csv|xlsx|md|txt)$"),
    include_vectors: bool = False,
):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    documents = [item for item in db.list_documents() if item.get("knowledge_base_id") == knowledge_base_id]
    all_chunks = db.get_chunks(include_embedding=include_vectors)
    chunks_by_document: dict[str, list[dict]] = {}
    for chunk in all_chunks:
        if chunk.get("knowledge_base_id") == knowledge_base_id:
            chunks_by_document.setdefault(chunk["document_id"], []).append(chunk)
    rows = []
    for document in documents:
        for chunk in chunks_by_document.get(document["id"], []):
            rows.append({
                "document_id": document["id"],
                "file_name": document["file_name"],
                "chunk_id": chunk["id"],
                "chunk_index": chunk["chunk_index"],
                "page": chunk.get("metadata", {}).get("page"),
                "text": chunk["text"],
                "embedding": json.dumps(chunk.get("embedding"), ensure_ascii=False) if include_vectors else "",
            })
    filename = f"{knowledge_base['name']}-knowledge-base.{format}"
    if format in {"md", "txt"}:
        content = "\n\n".join(
            f"# {document['file_name']}\n\n" + "\n\n".join(chunk["text"] for chunk in chunks_by_document.get(document["id"], []))
            for document in documents
        )
        media_type = "text/markdown; charset=utf-8" if format == "md" else "text/plain; charset=utf-8"
        payload = content.encode("utf-8")
    elif format == "csv":
        buffer = io.StringIO(newline="")
        columns = ["document_id", "file_name", "chunk_id", "chunk_index", "page", "text"]
        if include_vectors:
            columns.append("embedding")
        writer = csv.DictWriter(buffer, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
        payload = buffer.getvalue().encode("utf-8-sig")
        media_type = "text/csv; charset=utf-8"
    else:
        from openpyxl import Workbook

        buffer = io.BytesIO()
        workbook = Workbook()
        worksheet = workbook.active
        worksheet.title = "Chunks"
        columns = ["document_id", "file_name", "chunk_id", "chunk_index", "page", "text"]
        if include_vectors:
            columns.append("embedding")
        worksheet.append(columns)
        for row in rows:
            worksheet.append([row.get(column) for column in columns])
        workbook.save(buffer)
        payload = buffer.getvalue()
        media_type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    return Response(
        payload,
        media_type=media_type,
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{quote(filename)}"},
    )


@router.put("/api/knowledge/databases/{knowledge_base_id}")
async def yuxi_update_database(knowledge_base_id: str, payload: dict = Body(...)):
    current = rag._knowledge_base_or_404(knowledge_base_id)
    config = _config_from_yuxi(payload, current.get("config"))
    request = KnowledgeBaseUpdatePayload(
        name=payload.get("name") or payload.get("database_name"),
        description=payload.get("description"),
        config=config,
    )
    updated = await rag.update_knowledge_base(knowledge_base_id, request)
    return _yuxi_knowledge_base(updated, include_files=True)


@router.delete("/api/knowledge/databases/{knowledge_base_id}")
async def yuxi_delete_database(knowledge_base_id: str):
    result = await rag.delete_knowledge_base(knowledge_base_id)
    return {**result, "message": "知识库已删除"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/stats/repair")
async def yuxi_repair_database_stats(knowledge_base_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    return {
        "status": "not_required",
        "message": "本地 SQLite 统计由文档和分块记录实时计算，没有需要回填的冗余统计字段。",
        "updated_token_files": 0,
        "updated_chunk_files": 0,
        "stats": _kb_stats(knowledge_base_id),
    }


@router.get("/api/knowledge/databases/{knowledge_base_id}/virtual-folders/detect")
async def yuxi_detect_virtual_folders(knowledge_base_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    return {"has_virtual_folders": False, "count": 0, "status": "ready"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/virtual-folders/migrate")
async def yuxi_start_virtual_folder_migration(knowledge_base_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    raise HTTPException(status_code=409, detail="本地知识库使用真实目录结构；没有可迁移的虚拟目录任务。")


@router.get("/api/knowledge/databases/{knowledge_base_id}/virtual-folders/migrations/{task_id}/events")
async def yuxi_virtual_folder_migration_events(knowledge_base_id: str, task_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    raise HTTPException(status_code=404, detail="本地知识库没有该迁移任务。")


@router.post("/api/knowledge/generate-description")
async def yuxi_generate_description(payload: dict = Body(...)):
    name = str(payload.get("name") or "").strip()
    if not name:
        raise HTTPException(status_code=422, detail="知识库名称不能为空")
    file_list = payload.get("file_list") or []
    if not isinstance(file_list, list):
        raise HTTPException(status_code=422, detail="file_list 必须是数组")
    files_text = "\n".join(f"- {str(item)}" for item in file_list[:50])
    if len(file_list) > 50:
        files_text += f"\n... (还有 {len(file_list) - 50} 个文件)"
    current_description = str(payload.get("current_description") or "暂无描述")
    if files_text:
        current_description = f"{current_description}\n\n知识库包含的文件:\n{files_text}"
    prompt = (
        "请帮我优化以下知识库的描述。\n\n"
        f"知识库名称: {name}\n当前描述: {current_description}\n\n"
        "要求:\n1. 描述将作为智能体工具的描述使用\n"
        "2. 清晰说明知识库包含什么内容、适合解答什么类型的问题\n"
        "3. 简洁有力，通常 2-4 句话\n4. 不要使用 Markdown 格式\n"
        f"{'5. 请参考提供的文件列表准确概括知识库内容' if files_text else ''}\n"
        "请直接输出优化后的描述，不要有任何前缀说明。"
    )
    try:
        description = await complete_chat([{"role": "user", "content": prompt}])
    except ChatProviderUnavailable as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except (httpx.HTTPError, ValueError) as exc:
        raise HTTPException(status_code=502, detail=f"描述生成失败: {str(exc)[:300]}") from exc
    return {"description": description, "status": "success"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/providers/embedding/test")
async def yuxi_test_embedding(knowledge_base_id: str, payload: dict = Body(default={})):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    saved = knowledge_base.get("config", {}).get("embedding", {})
    values = {
        key: payload.get(key) if payload.get(key) not in (None, "") else saved.get(key, "")
        for key in ("base_url", "api_key", "model", "dimensions")
    }
    values["api_key"] = values["api_key"] or ""
    return await rag.test_embedding_provider(EmbeddingTestRequest.model_validate(values))


@router.post("/api/knowledge/databases/{knowledge_base_id}/providers/parser/test")
async def yuxi_test_parser(
    knowledge_base_id: str,
    file: UploadFile = File(...),
    engine: str | None = Form(None),
    mineru_api_uri: str | None = Form(None),
    mineru_api_key: str | None = Form(None),
):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    saved = knowledge_base.get("config", {}).get("parser", {})
    return await rag.test_parser_provider(
        file,
        engine or saved.get("engine", "auto"),
        mineru_api_uri or saved.get("mineru_api_uri"),
        mineru_api_key or saved.get("api_key"),
    )


@router.post("/api/knowledge/databases/{knowledge_base_id}/providers/reranker/test")
async def yuxi_test_reranker(knowledge_base_id: str, payload: dict = Body(...)):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    saved = knowledge_base.get("config", {}).get("reranker", {})
    values = {
        **payload,
        **{
            key: payload.get(key) if payload.get(key) not in (None, "") else saved.get(key)
            for key in ("base_url", "api_key", "model", "protocol")
        },
    }
    values["api_key"] = values.get("api_key") or ""
    values["base_url"] = values.get("base_url") or ""
    values["model"] = values.get("model") or ""
    values["protocol"] = values.get("protocol") or "openai"
    return await rag.test_reranker_provider(RerankerTestRequest.model_validate(values))


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents")
async def yuxi_list_documents(
    knowledge_base_id: str,
    page: int = 1,
    page_size: int = 100,
    status: str = "all",
    parent_id: str | None = None,
    recursive: bool = False,
    path_prefix: str = "",
    files_only: bool = False,
):
    rag._knowledge_base_or_404(knowledge_base_id)
    folders = db.list_folders(knowledge_base_id)
    if parent_id and not any(item["id"] == parent_id for item in folders):
        raise HTTPException(status_code=404, detail="父文件夹不存在")
    # Yuxi only searches all directory levels when a status filter is active.
    recursive = recursive and status != "all"
    documents = db.list_documents(knowledge_base_id)
    if status != "all":
        documents = [item for item in documents if _yuxi_document_status(item) == status]
    if parent_id and not recursive:
        documents = [item for item in documents if item.get("folder_id") == parent_id]
    elif not recursive:
        documents = [item for item in documents if item.get("folder_id") is None]
    if path_prefix:
        documents = [item for item in documents if str((item.get("metadata") or {}).get("relative_path", "")).startswith(path_prefix)]
    if files_only or status != "all":
        folders = []
    elif parent_id is not None:
        folders = [item for item in folders if item.get("parent_id") == parent_id]
    elif not recursive:
        folders = [item for item in folders if item.get("parent_id") is None]
    combined = [_yuxi_folder(item) for item in folders] + [_yuxi_document(item) for item in documents]
    combined.sort(key=lambda item: (not item["is_folder"], item["filename"].casefold()))
    total = len(combined)
    page = max(1, page)
    page_size = max(1, min(page_size, 500))
    start = (page - 1) * page_size
    return {"items": combined[start:start + page_size], "page": page, "page_size": page_size, "total": total,
            "has_more": start + page_size < total, "path_prefix": path_prefix, "stats": _kb_stats(knowledge_base_id)}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/search")
async def yuxi_search_documents(
    knowledge_base_id: str,
    query: str = "",
    offset: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=500),
):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="连接型知识库仅支持检索，不支持文档搜索")
    normalized_query = query.strip()
    if not normalized_query:
        return {"files": [], "total": 0, "offset": 0, "limit": limit, "has_more": False}
    documents, total = db.search_documents(knowledge_base_id, normalized_query, offset, limit)
    files = []
    for document in documents:
        item = _yuxi_document(document)
        files.append({
            "kb_id": knowledge_base_id,
            "kb_name": knowledge_base["name"],
            **{key: item.get(key) for key in (
                "file_id", "filename", "file_type", "status", "created_at", "updated_at", "file_size", "parent_id"
            )},
        })
    return {"files": files, "total": total, "offset": offset, "limit": limit, "has_more": offset + limit < total}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/exists")
async def yuxi_document_exists(knowledge_base_id: str, filename: str):
    result = await rag.knowledge_base_document_exists(knowledge_base_id, filename)
    return {**result, "exists": result["exists"]}


@router.post("/api/knowledge/databases/{knowledge_base_id}/folders")
async def yuxi_create_folder(knowledge_base_id: str, payload: dict = Body(...)):
    folder = await rag.create_folder(knowledge_base_id, rag.FolderPayload(name=str(payload.get("folder_name") or payload.get("name") or ""), parent_id=payload.get("parent_id")))
    return _yuxi_folder(folder)


@router.put("/api/knowledge/databases/{knowledge_base_id}/folders/{folder_id}/rename")
async def yuxi_rename_folder(knowledge_base_id: str, folder_id: str, payload: dict = Body(...)):
    folder = await rag.rename_folder(knowledge_base_id, folder_id, rag.FolderRenamePayload(name=str(payload.get("folder_name") or payload.get("name") or "")))
    return _yuxi_folder(folder)


@router.delete("/api/knowledge/databases/{knowledge_base_id}/folders/{folder_id}")
async def yuxi_delete_folder(knowledge_base_id: str, folder_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    if not db.folder_exists(folder_id, knowledge_base_id):
        raise HTTPException(status_code=404, detail="文件夹不存在")
    # Match KnowledgeBase.delete_folder: remove descendants before the folder.
    for folder in db.list_folders(knowledge_base_id):
        if folder.get("parent_id") == folder_id:
            await yuxi_delete_folder(knowledge_base_id, folder["id"])
    for document in db.list_documents(knowledge_base_id):
        if document.get("folder_id") == folder_id:
            await rag.delete_knowledge_base_document(knowledge_base_id, document["id"])
    await rag.remove_folder(knowledge_base_id, folder_id)
    return {"message": "文件夹删除成功"}


@router.put("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/move")
async def yuxi_move_document(knowledge_base_id: str, document_id: str, payload: dict = Body(...)):
    result = await rag.move_knowledge_base_document(knowledge_base_id, document_id, DocumentMovePayload(folder_id=payload.get("new_parent_id")))
    return {**result, "new_parent_id": result.get("folder_id")}


def _staging_paths(stage_id: str) -> tuple[Path, Path]:
    stage_id = stage_id.split("/", 1)[0]
    if not re.fullmatch(r"[a-f0-9]{32}", stage_id):
        raise HTTPException(status_code=400, detail="上传临时标识无效")
    root = (Path(settings.rag_upload_dir) / ".yuxi_staging").resolve()
    return root / f"{stage_id}.data", root / f"{stage_id}.json"


def _stage_file(
    content: bytes,
    file_name: str,
    mime_type: str | None,
    knowledge_base_id: str | None,
    source_url: str | None = None,
) -> dict:
    stage_id = uuid.uuid4().hex
    if not mime_type or mime_type == "application/octet-stream":
        mime_type = mimetypes.guess_type(file_name)[0] or "application/octet-stream"
    data_path, metadata_path = _staging_paths(stage_id)
    data_path.parent.mkdir(parents=True, exist_ok=True)
    data_path.write_bytes(content)
    digest = hashlib.sha256(content).hexdigest()
    same_name_files = [
        {**_yuxi_document(document), "content_hash": document.get("metadata", {}).get("sha256") or ""}
        for document in (db.list_documents(knowledge_base_id) if knowledge_base_id else [])
        if document["file_name"].casefold() == file_name.casefold()
    ]
    metadata_path.write_text(
        json.dumps(
            {
                "filename": file_name,
                "mime_type": mime_type or mimetypes.guess_type(file_name)[0],
                "size": len(content),
                "sha256": digest,
                "knowledge_base_id": knowledge_base_id,
                "source_url": source_url,
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    return {
        "file_path": f"{STAGE_URI}{stage_id}",
        "filename": file_name,
        "content_hash": digest,
        "size": len(content),
        "status": "uploaded",
        "has_same_name": bool(same_name_files),
        "same_name_files": same_name_files,
    }


def _public_host(host: str, port: int) -> bool:
    try:
        addresses = {item[4][0] for item in socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)}
    except OSError:
        return False
    return bool(addresses) and all(ipaddress.ip_address(address).is_global for address in addresses)


class _PageText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.title_parts = []
        self._hidden = 0
        self._title = False

    def handle_starttag(self, tag, attrs):
        if tag in {"script", "style", "noscript", "svg"}:
            self._hidden += 1
        elif tag == "title":
            self._title = True
        elif tag in {"p", "div", "br", "li", "tr", "h1", "h2", "h3", "section"}:
            self.parts.append("\n")

    def handle_endtag(self, tag):
        if tag in {"script", "style", "noscript", "svg"} and self._hidden:
            self._hidden -= 1
        elif tag == "title":
            self._title = False
        elif tag in {"p", "div", "br", "li", "tr", "h1", "h2", "h3", "section"}:
            self.parts.append("\n")

    def handle_data(self, data):
        if self._hidden:
            return
        text = data.strip()
        if text:
            self.parts.append(text)
            self.parts.append("\n")
            if self._title:
                self.title_parts.append(text)


@router.post("/api/knowledge/files/upload")
async def yuxi_upload_file(file: UploadFile = File(...), kb_id: str | None = None):
    file_name = Path(file.filename or "").name
    suffix = Path(file_name).suffix.lower()
    if suffix not in rag.ALLOWED_UPLOADS:
        raise HTTPException(status_code=415, detail=f"不支持的文件类型：{suffix or 'unknown'}")
    content = await file.read()
    if not content or len(content) > rag.MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="上传文件为空或超过 100 MB")
    if kb_id:
        knowledge_base = rag._knowledge_base_or_404(kb_id)
        if knowledge_base["kb_type"] != "local":
            raise HTTPException(status_code=400, detail="Dify/Notion 知识库为只读连接器")
        content_hash = await calculate_content_hash(content)
        if any(document.get("metadata", {}).get("sha256") == content_hash for document in db.list_documents(kb_id)):
            raise HTTPException(status_code=409, detail="数据库中已经存在了相同内容文件，File with the same content already exists in this database")
    return _stage_file(content, file_name, file.content_type, kb_id)


@router.post("/api/knowledge/files/upload-folder")
async def yuxi_upload_folder(file: UploadFile = File(...), kb_id: str = Query(..., min_length=1)):
    file_name = Path(file.filename or "").name
    if Path(file_name).suffix.lower() != ".zip":
        raise HTTPException(status_code=415, detail="文件夹上传仅支持 ZIP 压缩包")
    knowledge_base = rag._knowledge_base_or_404(kb_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="Dify/Notion 知识库为只读连接器")
    content = await file.read()
    if not content or len(content) > rag.MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="ZIP 文件为空或超过 100 MB")
    if not zipfile.is_zipfile(io.BytesIO(content)):
        raise HTTPException(status_code=422, detail="上传文件不是有效的 ZIP 压缩包")
    return _stage_file(content, file_name, "application/zip", kb_id)


@router.post("/api/knowledge/files/process-folder")
async def yuxi_process_folder(payload: dict = Body(...)):
    file_path = str(payload.get("file_path") or "")
    kb_id = str(payload.get("kb_id") or "")
    content_hash = str(payload.get("content_hash") or "")
    if not file_path.startswith(STAGE_URI) or not kb_id:
        raise HTTPException(status_code=422, detail="缺少本地暂存 ZIP 或知识库 ID")
    knowledge_base = rag._knowledge_base_or_404(kb_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="Dify/Notion 知识库为只读连接器")
    stage_id = file_path.removeprefix(STAGE_URI)
    data_path, metadata_path = _staging_paths(stage_id)
    stage_root = (Path(settings.rag_upload_dir) / ".yuxi_staging").resolve()
    if data_path.resolve().parent != stage_root or not data_path.is_file() or not metadata_path.is_file():
        raise HTTPException(status_code=404, detail="ZIP 暂存文件不存在或已过期")
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    if metadata.get("knowledge_base_id") != kb_id:
        raise HTTPException(status_code=400, detail="ZIP 暂存文件属于另一个知识库")
    actual_hash = hashlib.sha256(data_path.read_bytes()).hexdigest()
    if not content_hash or actual_hash != content_hash or actual_hash != metadata.get("sha256"):
        raise HTTPException(status_code=409, detail="ZIP 内容校验失败，请重新上传")

    staged_items: list[str] = []
    source_paths: dict[str, str] = {}
    skipped: list[str] = []
    total_size = 0
    try:
        with zipfile.ZipFile(data_path) as archive:
            members = [entry for entry in archive.infolist() if not entry.is_dir()]
            if len(members) > 2000:
                raise HTTPException(status_code=413, detail="ZIP 文件数量超过 2000")
            seen_paths: set[str] = set()
            for entry in members:
                relative = PurePosixPath(entry.filename.replace("\\", "/"))
                if (
                    relative.is_absolute()
                    or not relative.parts
                    or any(part in {".", ".."} for part in relative.parts)
                    or ":" in relative.parts[0]
                ):
                    raise HTTPException(status_code=422, detail="ZIP 内含有不安全的文件路径")
                mode = entry.external_attr >> 16
                if stat.S_ISLNK(mode):
                    raise HTTPException(status_code=422, detail="ZIP 内不允许包含符号链接")
                relative_name = relative.as_posix()
                key = relative_name.casefold()
                if key in seen_paths:
                    raise HTTPException(status_code=422, detail="ZIP 内存在重复文件路径")
                seen_paths.add(key)
                if Path(relative.name).suffix.lower() not in rag.ALLOWED_UPLOADS:
                    skipped.append(relative_name)
                    continue
                if entry.file_size <= 0 or total_size + entry.file_size > rag.MAX_UPLOAD_BYTES:
                    raise HTTPException(status_code=413, detail="ZIP 解压内容为空或总量超过 100 MB")
                content = archive.read(entry)
                total_size += len(content)
                if not content or total_size > rag.MAX_UPLOAD_BYTES:
                    raise HTTPException(status_code=413, detail="ZIP 解压内容为空或总量超过 100 MB")
                staged = _stage_file(
                    content,
                    relative.name,
                    mimetypes.guess_type(relative.name)[0],
                    kb_id,
                )
                staged_uri = staged["file_path"]
                staged_items.append(staged_uri)
                source_paths[staged_uri] = relative_name
    except HTTPException:
        for staged_uri in staged_items:
            staged_id = staged_uri.removeprefix(STAGE_URI)
            staged_data, staged_metadata = _staging_paths(staged_id)
            staged_data.unlink(missing_ok=True)
            staged_metadata.unlink(missing_ok=True)
        raise
    except (zipfile.BadZipFile, RuntimeError) as exc:
        for staged_uri in staged_items:
            staged_id = staged_uri.removeprefix(STAGE_URI)
            staged_data, staged_metadata = _staging_paths(staged_id)
            staged_data.unlink(missing_ok=True)
            staged_metadata.unlink(missing_ok=True)
        raise HTTPException(status_code=422, detail="ZIP 文件损坏或加密") from exc
    finally:
        data_path.unlink(missing_ok=True)
        metadata_path.unlink(missing_ok=True)

    if not staged_items:
        raise HTTPException(status_code=422, detail="ZIP 中没有受支持的 PDF 或文本文件")
    try:
        result = await yuxi_add_documents(
            kb_id,
            {"items": staged_items, "params": {"auto_index": True, "source_paths": source_paths}},
        )
    finally:
        for staged_uri in staged_items:
            staged_id = staged_uri.removeprefix(STAGE_URI)
            staged_data, staged_metadata = _staging_paths(staged_id)
            staged_data.unlink(missing_ok=True)
            staged_metadata.unlink(missing_ok=True)
    return {**result, "skipped_files": skipped, "total_uncompressed_bytes": total_size}


@router.post("/api/knowledge/files/fetch-url")
async def yuxi_fetch_url(payload: dict = Body(...)):
    raw_url = str(payload.get("url") or "").strip()
    parsed = urlsplit(raw_url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username or parsed.password:
        raise HTTPException(status_code=422, detail="仅支持公开 HTTP(S) 文档或网页 URL")
    port = parsed.port or (443 if parsed.scheme == "https" else 80)
    if not _public_host(parsed.hostname, port):
        raise HTTPException(status_code=422, detail="URL 主机不可公开访问")
    kb_id = payload.get("kb_id")
    if kb_id:
        knowledge_base = rag._knowledge_base_or_404(str(kb_id))
        if knowledge_base["kb_type"] != "local":
            raise HTTPException(status_code=400, detail="Dify/Notion 知识库为只读连接器")

    try:
        async with httpx.AsyncClient(timeout=45, follow_redirects=False) as client:
            async with client.stream("GET", raw_url, headers={"Accept": "application/pdf,text/plain,text/markdown,text/html,application/json,text/csv"}) as response:
                if 300 <= response.status_code < 400:
                    raise HTTPException(status_code=422, detail="URL 跳转被拒绝，请直接填写最终文档地址")
                response.raise_for_status()
                chunks = []
                total = 0
                async for chunk in response.aiter_bytes():
                    total += len(chunk)
                    if total > rag.MAX_UPLOAD_BYTES:
                        raise HTTPException(status_code=413, detail="URL 文件超过 100 MB")
                    chunks.append(chunk)
                content = b"".join(chunks)
                content_type = response.headers.get("content-type", "").split(";", 1)[0].lower()
                disposition = response.headers.get("content-disposition", "")
    except HTTPException:
        raise
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"URL 下载失败：{type(exc).__name__}") from exc

    candidate_name = re.search(r"filename\*?=(?:UTF-8''|\")?([^;\"]+)", disposition, re.IGNORECASE)
    file_name = unquote(candidate_name.group(1).strip().strip('"')) if candidate_name else PurePosixPath(unquote(parsed.path)).name
    suffix = Path(file_name).suffix.lower()
    if content_type == "text/html":
        page = _PageText()
        page.feed(content.decode("utf-8", errors="replace"))
        title = re.sub(r"[^\w\-. ()\u4e00-\u9fff]", "", " ".join(page.title_parts)).strip()
        file_name = f"{title[:100] or 'web-page'}.txt"
        content = " ".join(part.strip() for part in page.parts if part.strip()).encode("utf-8")
        content_type = "text/plain"
    elif suffix not in rag.ALLOWED_UPLOADS and content_type.startswith("text/"):
        file_name = f"{Path(file_name).stem or 'web-document'}.txt"
        content_type = "text/plain"
    elif suffix not in rag.ALLOWED_UPLOADS and content_type == "application/pdf":
        file_name = f"{Path(file_name).stem or 'web-document'}.pdf"
    elif suffix not in rag.ALLOWED_UPLOADS and content_type == "application/json":
        file_name = f"{Path(file_name).stem or 'web-document'}.json"
    elif suffix not in rag.ALLOWED_UPLOADS and content_type == "text/csv":
        file_name = f"{Path(file_name).stem or 'web-document'}.csv"
    elif suffix not in rag.ALLOWED_UPLOADS and content_type == "text/markdown":
        file_name = f"{Path(file_name).stem or 'web-document'}.md"
    if Path(file_name).suffix.lower() not in rag.ALLOWED_UPLOADS:
        raise HTTPException(status_code=415, detail="URL 内容类型不受支持；支持 PDF、TXT、Markdown、CSV 和 JSON")
    if not content:
        raise HTTPException(status_code=422, detail="URL 内容为空")
    return _stage_file(content, Path(file_name).name, content_type, str(kb_id) if kb_id else None, raw_url)


def _workspace_target(path: str) -> tuple[Path, str]:
    raw_path = str(path or "/").replace("\\", "/")
    parts = [part for part in raw_path.split("/") if part]
    if any(part in {".", ".."} or ":" in part or "\x00" in part for part in parts):
        raise HTTPException(status_code=403, detail="工作区路径无效")

    root = Path(settings.rag_workspace_dir).resolve()
    root.mkdir(parents=True, exist_ok=True)
    target = root
    for part in parts:
        target = target / part
        try:
            info = target.lstat()
        except FileNotFoundError:
            continue
        attributes = getattr(info, "st_file_attributes", 0)
        reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400)
        if stat.S_ISLNK(info.st_mode) or attributes & reparse_flag:
            raise HTTPException(status_code=403, detail="工作区不允许符号链接或重解析点")

    resolved = target.resolve()
    if resolved != root and root not in resolved.parents:
        raise HTTPException(status_code=403, detail="工作区路径超出允许目录")
    return resolved, "/" + "/".join(parts)


def _workspace_entry(path: Path, virtual_path: str) -> dict:
    info = path.stat()
    is_dir = stat.S_ISDIR(info.st_mode)
    display_path = virtual_path.rstrip("/") + ("/" if is_dir and virtual_path != "/" else "")
    return {
        "path": display_path or "/",
        "virtual_path": f"local-workspace:{display_path or '/'}",
        "name": path.name or "工作区",
        "is_dir": is_dir,
        "size": 0 if is_dir else info.st_size,
        "modified_at": datetime.fromtimestamp(info.st_mtime, timezone.utc).isoformat(),
    }


def _workspace_children(path: Path, virtual_path: str, files_only: bool) -> list[dict]:
    entries = []
    for child in path.iterdir():
        try:
            info = child.lstat()
            attributes = getattr(info, "st_file_attributes", 0)
            reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400)
            if stat.S_ISLNK(info.st_mode) or attributes & reparse_flag:
                continue
            is_dir = stat.S_ISDIR(info.st_mode)
            if files_only and is_dir:
                continue
            child_virtual_path = f"{virtual_path.rstrip('/')}/{child.name}"
            entries.append(_workspace_entry(child, child_virtual_path))
        except OSError:
            continue
    return sorted(entries, key=lambda entry: (not entry["is_dir"], entry["name"].casefold()))


@router.get("/api/workspace/tree")
async def yuxi_workspace_tree(
    path: str = Query("/"),
    recursive: bool = False,
    files_only: bool = False,
    include_unbound_project_dirs: bool = False,
):
    del include_unbound_project_dirs  # Geo local workspace has no Yuxi project binding layer.
    target, virtual_path = _workspace_target(path)
    if not target.exists():
        return {"entries": []}
    if not target.is_dir():
        raise HTTPException(status_code=400, detail="当前路径不是目录")

    if not recursive:
        return {"entries": _workspace_children(target, virtual_path, files_only)}

    entries = []
    pending = [(target, virtual_path)]
    pending_index = 0
    while pending_index < len(pending) and len(entries) < 5000:
        current, current_virtual_path = pending[pending_index]
        pending_index += 1
        children = _workspace_children(current, current_virtual_path, False)
        visible_children = [entry for entry in children if not files_only or not entry["is_dir"]]
        entries.extend(visible_children[: 5000 - len(entries)])
        pending.extend(
            (current / entry["name"], entry["path"].rstrip("/"))
            for entry in children
            if entry["is_dir"]
        )
    return {"entries": entries}


@router.post("/api/workspace/directory")
async def yuxi_create_workspace_directory(payload: dict = Body(...)):
    name = str(payload.get("name") or "").strip()
    if not name or name in {".", ".."} or any(char in name for char in ("/", "\\", ":", "\x00")):
        raise HTTPException(status_code=422, detail="文件夹名称无效")
    parent, parent_virtual_path = _workspace_target(str(payload.get("parent_path") or "/"))
    if not parent.exists():
        raise HTTPException(status_code=404, detail="目标目录不存在")
    if not parent.is_dir():
        raise HTTPException(status_code=400, detail="目标路径不是目录")
    child = parent / name
    try:
        child.mkdir()
    except FileExistsError as exc:
        raise HTTPException(status_code=409, detail="同名文件或文件夹已存在") from exc
    except OSError as exc:
        raise HTTPException(status_code=400, detail="无法创建文件夹") from exc
    virtual_path = f"{parent_virtual_path.rstrip('/')}/{name}"
    return {"success": True, "entry": _workspace_entry(child, virtual_path)}


@router.post("/api/knowledge/files/import-workspace")
async def yuxi_import_workspace_files(payload: dict = Body(...)):
    knowledge_base_id = str(payload.get("kb_id") or "").strip()
    paths = payload.get("paths") or []
    if not knowledge_base_id:
        raise HTTPException(status_code=400, detail="kb_id is required")
    if not isinstance(paths, list) or not paths:
        raise HTTPException(status_code=400, detail="请选择至少一个工作区文件")
    if len(paths) > 50:
        raise HTTPException(status_code=400, detail="一次最多导入 50 个文件")
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="只读连接器不能导入本地文件")

    known_documents = db.list_documents(knowledge_base_id)
    known_hashes = {
        document.get("metadata", {}).get("sha256")
        for document in known_documents
        if document.get("metadata", {}).get("sha256")
    }
    imported_hashes = set()
    results = []
    for path in paths:
        source, virtual_path = _workspace_target(str(path))
        if not source.exists():
            raise HTTPException(status_code=404, detail="工作区文件不存在")
        if not source.is_file():
            raise HTTPException(status_code=400, detail="只能导入文件")
        file_name = source.name.lower()
        if Path(file_name).suffix.lower() not in rag.ALLOWED_UPLOADS:
            raise HTTPException(status_code=415, detail=f"不支持的文件类型：{Path(file_name).suffix or 'unknown'}")
        if source.stat().st_size > rag.MAX_UPLOAD_BYTES:
            raise HTTPException(status_code=413, detail="工作区文件超过 100 MB")
        content = source.read_bytes()
        if not content:
            raise HTTPException(status_code=422, detail="工作区文件为空")
        if len(content) > rag.MAX_UPLOAD_BYTES:
            raise HTTPException(status_code=413, detail="工作区文件超过 100 MB")
        digest = hashlib.sha256(content).hexdigest()
        if digest in known_hashes or digest in imported_hashes:
            raise HTTPException(status_code=409, detail=f"知识库中已经存在相同内容文件：{file_name}")
        imported_hashes.add(digest)

        staged = _stage_file(
            content,
            file_name,
            mimetypes.guess_type(file_name)[0],
            knowledge_base_id,
        )
        same_name_files = [
            {"file_id": document["id"], "filename": document["file_name"]}
            for document in known_documents
            if document["file_name"].casefold() == file_name.casefold()
        ]
        stage_ref = staged["file_path"].removeprefix(STAGE_URI)
        staged.update({
            "message": "Workspace file successfully imported",
            "file_path": f"{STAGE_URI}{stage_ref}/{quote(file_name, safe='')}",
            "minio_path": f"{STAGE_URI}{stage_ref}/{quote(file_name, safe='')}",
            "kb_id": knowledge_base_id,
            "filename": file_name,
            "original_filename": Path(file_name).stem,
            "workspace_path": virtual_path,
            "same_name_files": same_name_files,
            "has_same_name": bool(same_name_files),
        })
        results.append(staged)
    return {"status": "success", "items": results}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents")
async def yuxi_add_documents(knowledge_base_id: str, payload: dict = Body(...)):
    return await _add_documents(knowledge_base_id, payload, parse_after_upload=True)


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/add")
async def yuxi_add_uploaded_documents(knowledge_base_id: str, payload: dict = Body(...)):
    result = await _add_documents(knowledge_base_id, payload, parse_after_upload=False)
    items = []
    for entry in result["processed"]:
        document = db.get_document(entry["file_id"])
        items.append({"index": entry["index"], "item": entry["item"], "file_id": entry["file_id"],
                      "status": document["status"], "file_meta": _yuxi_document(document)})
    failed_items = [{"index": entry["index"], "item": entry["item"], "status": "failed",
                     "error": f"添加记录失败: {entry['error_message']}", "error_type": "add_failed"}
                    for entry in result["failed"]]
    added, failed = len(items), len(failed_items)
    status = "success" if not failed else "failed" if not added else "partial_failed"
    message = (f"已添加 {added} 个文件" if not failed else
               f"文件添加失败，失败 {failed} 个" if not added else f"已添加 {added} 个文件，失败 {failed} 个")
    return {"message": message, "status": status, "items": items, "failed_items": failed_items,
            "added": added, "failed": failed}


async def _add_documents(knowledge_base_id: str, payload: dict, parse_after_upload: bool):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=400, detail="只读连接器不能添加本地文件")
    items = payload.get("items") or []
    if not isinstance(items, list) or not items:
        raise HTTPException(status_code=422, detail="请先上传文件")
    params = payload.get("params") or {}
    config = knowledge_base.get("config") or {}
    chunking = config.get("chunking") or {}
    parser = config.get("parser") or {}
    processed, failed = [], []
    stage_root = (Path(settings.rag_upload_dir) / ".yuxi_staging").resolve()
    upload_root = Path(settings.rag_upload_dir).resolve()
    for index, item in enumerate(items):
        if not isinstance(item, str) or not item.startswith(STAGE_URI):
            failed.append({"index": index, "item": str(item), "error_message": "该文件不是本地服务上传的暂存文件"})
            continue
        stage_id = item.removeprefix(STAGE_URI)
        data_path, metadata_path = _staging_paths(stage_id)
        if not data_path.is_file() or not metadata_path.is_file() or data_path.resolve().parent != stage_root:
            failed.append({"index": index, "item": item, "error_message": "上传暂存文件不存在或已过期"})
            continue
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        if metadata.get("knowledge_base_id") not in (None, knowledge_base_id):
            failed.append({"index": index, "item": item, "error_message": "暂存文件属于另一个知识库"})
            continue
        source_paths = params.get("source_paths") or {}
        source_value = source_paths.get(item, metadata["filename"]) if isinstance(source_paths, dict) else metadata["filename"]
        try:
            source_path = PurePosixPath(str(source_value).replace("\\", "/"))
            if source_path.is_absolute() or any(part in {".", ".."} for part in source_path.parts) or ":" in source_path.parts[0]:
                raise ValueError("文件相对路径无效")
        except (IndexError, ValueError) as exc:
            failed.append({"index": index, "item": item, "error_message": str(exc) or "文件相对路径无效"})
            continue
        relative_path = source_path.as_posix() if len(source_path.parts) > 1 else None
        file_name = source_path.name or metadata["filename"]
        stored_path = upload_root / f"{uuid.uuid4().hex}{Path(file_name).suffix.lower()}"
        shutil.move(str(data_path), str(stored_path))
        metadata_path.unlink(missing_ok=True)
        folder_id = params.get("parent_id")
        if relative_path:
            folder_id = db.ensure_folder_path(knowledge_base_id, PurePosixPath(relative_path).parent.as_posix(), folder_id)
        document_id = db.create_document(
            file_name, str(stored_path), metadata.get("mime_type") or mimetypes.guess_type(file_name)[0] or "application/octet-stream",
            parser.get("engine", "auto"), chunking.get("chunk_preset_id", "general"), chunking,
            {
                "file_size": metadata.get("size", 0),
                "sha256": metadata.get("sha256"),
                "relative_path": relative_path,
                "source_url": metadata.get("source_url"),
            },
            knowledge_base_id, folder_id,
        )
        try:
            processing_params = document_processing_params(
                db.get_document(document_id), config, params_for_uploaded_document(item, params))
            db.update_document_processing_params(document_id, processing_params)
            result = {"document_id": document_id, "status": "uploaded"}
            if parse_after_upload:
                result = await rag.parse_existing_document(document_id)
                if params.get("auto_index"):
                    result = await rag.index_existing_document(document_id)
            processed.append({"index": index, "item": item, "document_id": document_id, "file_id": document_id, "result": result})
        except Exception as exc:
            db.update_document_status(document_id, "failed", str(exc)[:500])
            failed.append({"index": index, "item": item, "document_id": document_id, "file_id": document_id, "error_message": str(exc)[:500]})
    result_status = "success" if not failed else "error" if not processed else "partial"
    return {"status": result_status, "message": f"已处理 {len(processed)} 个文件，失败 {len(failed)} 个", "processed": processed, "failed": failed}


def _batch_request(payload: dict) -> DocumentBatchPayload:
    file_ids = payload.get("file_ids") or payload.get("document_ids") or []
    return DocumentBatchPayload(document_ids=[str(item) for item in file_ids], params=payload.get("params") or {})


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/parse")
async def yuxi_parse_documents(knowledge_base_id: str, payload: dict = Body(...)):
    result = await rag.parse_knowledge_base_documents(knowledge_base_id, _batch_request(payload))
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"解析完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/parse-pending")
async def yuxi_parse_pending(knowledge_base_id: str, payload: dict = Body(default={} )):
    result = await rag.parse_pending_knowledge_base_documents(knowledge_base_id, payload.get("params") or {})
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"解析完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/index")
async def yuxi_index_documents(knowledge_base_id: str, payload: dict = Body(...)):
    result = await rag.index_knowledge_base_documents(knowledge_base_id, _batch_request(payload))
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"入库完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/index-pending")
async def yuxi_index_pending(knowledge_base_id: str, payload: dict = Body(default={} )):
    result = await rag.index_pending_knowledge_base_documents(knowledge_base_id, payload.get("params") or {})
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"入库完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/basic")
async def yuxi_get_document_basic(knowledge_base_id: str, document_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    folder = next((item for item in db.list_folders(knowledge_base_id) if item["id"] == document_id), None)
    if folder:
        return _yuxi_folder(folder)
    document = db.get_document(document_id)
    if not document or document.get("knowledge_base_id") != knowledge_base_id:
        raise HTTPException(status_code=404, detail="文件不存在")
    return _yuxi_document(document)


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/content")
async def yuxi_get_document_content(knowledge_base_id: str, document_id: str):
    document = db.get_document(document_id)
    if not document or document.get("knowledge_base_id") != knowledge_base_id:
        raise HTTPException(status_code=404, detail="文件不存在")
    blocks = db.get_blocks(document_id)
    chunks = db.get_chunks(document_id, include_embedding=False)
    return {"document_id": document_id, "file_id": document_id, "filename": document["file_name"], "blocks": blocks, "chunks": chunks, "text": "\n\n".join(item["text"] for item in blocks)}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/download")
async def yuxi_download_document(knowledge_base_id: str, document_id: str):
    return await rag.download_knowledge_base_document(knowledge_base_id, document_id)


def _workspace_knowledge_entry(kb_id: str, item: dict) -> dict:
    # Entry contract from Yuxi server/routers/workspace_router.py (v0.7.3).
    is_dir = bool(item.get("is_folder"))
    is_virtual_folder = bool(item.get("is_virtual_folder"))
    file_id = item.get("file_id")
    path_prefix = item.get("path_prefix") or ""
    if is_virtual_folder:
        path = f"/knowledge/{kb_id}/virtual/{quote(path_prefix, safe='')}"
    elif is_dir:
        path = f"/knowledge/{kb_id}/folder/{file_id}/"
    else:
        path = f"/knowledge/{kb_id}/file/{file_id}"
    return {
        "source": "knowledge", "kb_id": kb_id, "file_id": file_id,
        "parent_id": item.get("parent_id"), "path": path, "virtual_path": path,
        "name": item.get("filename") or file_id, "is_dir": is_dir,
        "size": 0 if is_dir else int(item.get("file_size") or 0),
        "modified_at": item.get("updated_at") or item.get("created_at") or "",
        "readonly": True, "status": item.get("status") or "done",
        "has_original_file": bool(item.get("has_original_file")),
        "has_parsed_markdown": bool(item.get("has_parsed_markdown")),
        "is_virtual_folder": is_virtual_folder, "path_prefix": path_prefix,
    }


@router.get("/api/workspace/knowledge/tree")
async def yuxi_workspace_knowledge_tree(
    kb_id: str, parent_id: str | None = None, path_prefix: str = "",
    page: int = Query(1, ge=1), page_size: int = Query(200, ge=1, le=500),
    recursive: bool = False, files_only: bool = False,
):
    result = await yuxi_list_documents(
        kb_id, page=page, page_size=page_size, parent_id=parent_id,
        recursive=recursive, path_prefix=path_prefix, files_only=files_only,
    )
    return {
        "kb_id": kb_id, "readonly": True,
        "entries": [_workspace_knowledge_entry(kb_id, item) for item in result["items"]
                    if not files_only or not item.get("is_folder")],
        "page": result["page"], "page_size": result["page_size"], "total": result["total"],
        "has_more": result["has_more"], "parent_id": parent_id, "path_prefix": path_prefix,
    }


@router.get("/api/workspace/knowledge/file")
async def yuxi_workspace_knowledge_preview(kb_id: str, file_id: str):
    from .yuxi_port import _upstream as _upstream  # noqa: F401
    from yuxi.utils.filepreview import (
        MAX_BINARY_PREVIEW_SIZE_BYTES, OfficePreviewConversionError, convert_office_to_pdf,
        is_office_pdf_preview_file, preview_too_large, render_preview,
    )

    original = await rag.download_knowledge_base_document(kb_id, file_id)
    document = db.get_document(file_id)
    filename = document["file_name"]
    metadata = {"source": "knowledge", "kb_id": kb_id, "file_id": file_id,
                "filename": filename, "readonly": True}
    path = Path(original.path)
    with path.open("rb") as stream:
        content = stream.read(MAX_BINARY_PREVIEW_SIZE_BYTES + 1)
    if len(content) > MAX_BINARY_PREVIEW_SIZE_BYTES:
        return {**metadata, **preview_too_large().payload()}

    if is_office_pdf_preview_file(filename):
        cache = Path(settings.rag_upload_dir) / ".previews" / f"{file_id}.pdf"
        try:
            if not cache.is_file():
                converted = await convert_office_to_pdf(filename, content)
                cache.parent.mkdir(parents=True, exist_ok=True)
                cache.write_bytes(converted)
            content = cache.read_bytes()
        except OfficePreviewConversionError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        filename = f"{Path(filename).stem}.pdf"

    preview = render_preview(filename, content)
    if isinstance(preview.content, bytes):
        return Response(
            content=preview.content, media_type=preview.media_type,
            headers={"Content-Disposition": f"inline; filename*=UTF-8''{quote(filename)}",
                     "X-Yuxi-Preview-Type": preview.preview_type,
                     "X-Yuxi-Preview-Filename": quote(filename)},
        )
    return {**metadata, **preview.payload()}


@router.get("/api/workspace/knowledge/download")
async def yuxi_workspace_knowledge_download(kb_id: str, file_id: str, variant: str = "original"):
    if variant == "original":
        return await rag.download_knowledge_base_document(kb_id, file_id)
    if variant != "parsed":
        raise HTTPException(status_code=422, detail="variant 必须是 original 或 parsed")
    content = await rag.knowledge_base_document_content(kb_id, file_id)
    if not content["blocks"]:
        raise HTTPException(status_code=404, detail="文件尚无解析内容")
    filename = f"{Path(content['file_name']).stem}.md"
    return Response(
        content=content["text"], media_type="text/markdown; charset=utf-8",
        headers={"Content-Disposition": f"attachment; filename*=UTF-8''{quote(filename)}"},
    )


@router.get("/api/knowledge/databases/{knowledge_base_id}/images/{object_path:path}")
async def yuxi_get_kb_image(knowledge_base_id: str, object_path: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    normalized = PurePosixPath(object_path.replace("\\", "/"))
    if not object_path.startswith("kb-images/") or normalized.is_absolute() or ".." in normalized.parts:
        raise HTTPException(status_code=400, detail="非法的知识库图片路径")
    relative = PurePosixPath(*normalized.parts[1:])
    root = (Path(settings.rag_upload_dir) / ".kb-images" / knowledge_base_id).resolve()
    target = (root / Path(*relative.parts)).resolve()
    try:
        target.relative_to(root)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="非法的知识库图片路径") from exc
    if not target.is_file():
        raise HTTPException(status_code=404, detail="图片不存在")
    return FileResponse(target, media_type=mimetypes.guess_type(target.name)[0] or "application/octet-stream", headers={"Cache-Control": "private, max-age=3600"})


@router.delete("/api/knowledge/databases/{knowledge_base_id}/documents/batch")
async def yuxi_batch_delete_documents(knowledge_base_id: str, payload: list[str] = Body(...)):
    rag._knowledge_base_or_404(knowledge_base_id)
    deleted_count = 0
    failed_items = []
    for document_id in payload:
        try:
            await yuxi_delete_document(knowledge_base_id, document_id)
            deleted_count += 1
        except HTTPException as exc:
            failed_items.append({"doc_id": document_id, "error": str(exc.detail)})
        except Exception as exc:
            failed_items.append({"doc_id": document_id, "error": str(exc)[:500]})
    if failed_items:
        if deleted_count == 0:
            raise HTTPException(status_code=400, detail=f"批量删除失败: 所有 {len(failed_items)} 个文件均未删除。")
        return {"message": f"部分删除成功: 已删除 {deleted_count} 个文件，失败 {len(failed_items)} 个",
                "deleted_count": deleted_count, "failed_items": failed_items}
    return {"message": f"批量删除成功: 已删除 {deleted_count} 个文件", "deleted_count": deleted_count}


@router.delete("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}")
async def yuxi_delete_document(knowledge_base_id: str, document_id: str):
    rag._knowledge_base_or_404(knowledge_base_id)
    if db.folder_exists(document_id, knowledge_base_id):
        return await yuxi_delete_folder(knowledge_base_id, document_id)
    document = db.get_document(document_id)
    if not document or document.get("knowledge_base_id") != knowledge_base_id:
        raise HTTPException(status_code=400, detail="文件不存在")
    await rag.delete_knowledge_base_document(knowledge_base_id, document_id)
    return {"message": "删除成功"}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}")
async def yuxi_get_document(knowledge_base_id: str, document_id: str):
    return await yuxi_get_document_basic(knowledge_base_id, document_id)


def _retrieval_options(knowledge_base: dict) -> dict:
    from dataclasses import MISSING, fields

    from .provider_api import _model_rows
    from .yuxi_port import _upstream as _upstream  # noqa: F401
    from yuxi.knowledge.implementations.milvus import MilvusRetrievalConfig

    config = (knowledge_base.get("config") or {}).get("retrieval") or {}
    rerank_models = [
        {"label": model["display_name"], "value": model["spec"]}
        for provider in _model_rows("rerank").values()
        for model in provider["models"]
    ]
    options = []
    for field in fields(MilvusRetrievalConfig):
        metadata = dict(field.metadata)
        options_provider = metadata.pop("options_provider", None)
        default = None if field.default is MISSING else field.default
        option = {"key": field.name, "default": config.get(field.name, default), **metadata}
        if options_provider == "rerank_models":
            option["options"] = rerank_models
        options.append(option)
    return {"params": {"options": options}, **config}


@router.get("/api/knowledge/databases/{knowledge_base_id}/query-params")
async def yuxi_get_query_params(knowledge_base_id: str):
    return _retrieval_options(rag._knowledge_base_or_404(knowledge_base_id))


@router.put("/api/knowledge/databases/{knowledge_base_id}/query-params")
async def yuxi_update_query_params(knowledge_base_id: str, payload: dict = Body(...)):
    current = rag._knowledge_base_or_404(knowledge_base_id)
    values = dict(payload)
    aliases = {"top_k": "final_top_k", "vector_search_weight": "vector_weight", "keyword_search_weight": "bm25_weight", "use_graph": "use_graph_retrieval"}
    retrieval = dict(current.get("config", {}).get("retrieval", {}))
    for key, value in values.items():
        retrieval[aliases.get(key, key)] = value
    saved = await rag.update_knowledge_base_query_params(knowledge_base_id, retrieval)
    return {"message": "success", "params": saved, **saved}


def _retrieve_request(knowledge_base_id: str, query: str, meta: dict | None = None) -> RetrieveRequest:
    values = dict(meta or {})
    aliases = {"top_k": "final_top_k", "vector_search_weight": "vector_weight", "keyword_search_weight": "bm25_weight", "use_graph": "use_graph_retrieval"}
    values = {aliases.get(key, key): value for key, value in values.items()}
    values["query"] = query
    values["filters"] = {**(values.get("filters") or {}), "knowledge_base_id": knowledge_base_id}
    return RetrieveRequest.model_validate(values)


@router.post("/api/knowledge/databases/{knowledge_base_id}/query")
@router.post("/api/knowledge/databases/{knowledge_base_id}/query-test")
async def yuxi_query(knowledge_base_id: str, payload: dict = Body(...)):
    request = _retrieve_request(knowledge_base_id, str(payload.get("query") or ""), payload.get("meta"))
    result = await rag.retrieve_api(request)
    result = result.model_dump() if hasattr(result, "model_dump") else result
    search_results = []
    for item in result.get("evidences", []):
        scores = item.get("scores") or {}
        metadata = dict(item.get("metadata") or {})
        file_name = item.get("file_name") or metadata.get("source") or ""
        metadata.update(
            {
                "file_id": item["document_id"],
                "source": file_name,
                "chunk_id": item["chunk_id"],
                "chunk_index": metadata.get("chunk_index", item.get("chunk_index")),
                "page": item.get("page"),
                "bbox": item.get("bbox"),
            }
        )
        fusion_score = item.get("fusion_score")
        if fusion_score is None:
            fusion_score = scores.get("fusion")
        score = fusion_score
        if score is None:
            for key in ("hybrid_score", "bm25_score", "vector_score"):
                if item.get(key) is not None:
                    score = item[key]
                    break
            else:
                score = 0.0
        search_result = {
            "id": item["evidence_id"],
            "content": item["text"],
            "text": item["text"],
            "score": score,
            "metadata": metadata,
        }
        score_fields = {
            "bm25_score": item.get("bm25_score", scores.get("bm25")),
            "vector_score": item.get("vector_score", scores.get("vector")),
            "hybrid_score": item.get("hybrid_score", scores.get("hybrid")),
            "graph_score": item.get("graph_score", scores.get("graph")),
            "fusion_score": fusion_score,
            "rerank_score": item.get("rerank_score", scores.get("rerank")),
        }
        search_result.update({key: value for key, value in score_fields.items() if value is not None})
        distance = item.get("distance", scores.get("distance"))
        if request.include_distances is not False and distance is not None:
            search_result["distance"] = distance
        search_results.append(search_result)
    return search_results


@router.get("/api/knowledge/databases/{knowledge_base_id}/sample-questions")
async def yuxi_get_sample_questions(knowledge_base_id: str):
    result = await rag.get_sample_questions(knowledge_base_id)
    return result


@router.post("/api/knowledge/databases/{knowledge_base_id}/sample-questions")
async def yuxi_save_sample_questions(knowledge_base_id: str, payload: dict = Body(...)):
    knowledge_base = rag._knowledge_base_or_404(knowledge_base_id)
    supplied = payload.get("questions")
    if supplied is not None:
        if not isinstance(supplied, list):
            raise HTTPException(status_code=422, detail="questions 必须是数组")
        return await rag.save_sample_questions(knowledge_base_id, payload)

    count = payload.get("count", 10)
    if isinstance(count, bool) or not isinstance(count, int) or not 1 <= count <= 50:
        raise HTTPException(status_code=422, detail="count 必须是 1 到 50 的整数")
    documents = db.list_documents(knowledge_base_id)
    if not documents:
        raise HTTPException(status_code=400, detail="知识库中没有文件")
    file_lines = "\n".join(
        f"- {item.get('file_name') or item.get('filename') or ''} ({Path(item.get('file_name') or '').suffix.lstrip('.')})"
        for item in documents[:20]
    )
    system_prompt = (
        "你是一个专业的知识库问答测试专家。根据知识库文件列表生成有价值的检索测试问题。"
        "问题要具体、多样，涵盖事实查询、概念解释和操作指导，长度控制在10-30字。"
        '只返回 JSON 对象：{"questions":["问题1？"]}，不要其他说明。'
    )
    user_prompt = f'请为知识库"{knowledge_base["name"]}"生成{count}个测试问题。\n文件列表：\n{file_lines}'
    try:
        content = await complete_chat([
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ])
        content = re.sub(r"^\s*```(?:json)?|```\s*$", "", content, flags=re.IGNORECASE).strip()
        parsed = json.loads(content)
        questions = parsed.get("questions") if isinstance(parsed, dict) else None
        if not isinstance(questions, list) or not questions or any(not isinstance(item, str) or not item.strip() for item in questions):
            raise ValueError("模型响应的 questions 必须是非空字符串数组")
    except ChatProviderUnavailable as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except (httpx.HTTPError, ValueError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=502, detail=f"示例问题生成失败: {str(exc)[:300]}") from exc
    saved = await rag.save_sample_questions(knowledge_base_id, {"questions": questions})
    return {**saved, "count": len(questions), "kb_id": knowledge_base_id, "db_name": knowledge_base["name"]}


@router.get("/api/knowledge/mindmap/databases")
async def yuxi_mindmap_databases():
    return [_yuxi_knowledge_base(item) for item in db.list_knowledge_bases() if item["kb_type"] == "local"]


@router.get("/api/knowledge/databases/{knowledge_base_id}/mindmap/files")
async def yuxi_mindmap_files(knowledge_base_id: str):
    return [_yuxi_document(item) for item in await rag.mindmap_files(knowledge_base_id)]


@router.post("/api/knowledge/databases/{knowledge_base_id}/mindmap/generate")
async def yuxi_generate_mindmap(knowledge_base_id: str, payload: dict = Body(default={} )):
    request = MindMapPayload(
        document_ids=payload.get("file_ids") or payload.get("document_ids") or [],
        user_prompt=payload.get("user_prompt") or "",
        incremental=bool(payload.get("incremental", False)),
    )
    result = await rag.generate_knowledge_base_mindmap(knowledge_base_id, request)
    return {**result, "generation_mode": result.get("generation", "openai-compatible-llm-mindmap")}


@router.get("/api/knowledge/databases/{knowledge_base_id}/mindmap")
async def yuxi_get_mindmap(knowledge_base_id: str, document_id: str | None = None):
    return await rag.get_knowledge_base_mindmap(knowledge_base_id, document_id)


@router.get("/api/knowledge/databases/{knowledge_base_id}/mindmap/diff")
async def yuxi_mindmap_diff(knowledge_base_id: str):
    return await rag.knowledge_base_mindmap_diff(knowledge_base_id)


@router.get("/api/knowledge/databases/{knowledge_base_id}/graph")
async def yuxi_get_graph(knowledge_base_id: str):
    return await rag.get_knowledge_graph(knowledge_base_id)


@router.get("/api/knowledge/databases/{knowledge_base_id}/graph-build/status")
async def yuxi_get_graph_status(knowledge_base_id: str):
    status = await rag.knowledge_graph_status(knowledge_base_id)
    return {
        **status,
        "build_task_status": "completed" if status.get("status") == "indexed" else ("failed" if status.get("status") == "failed" else status.get("status", "idle")),
        "note": "本地 SQLite 保存实体、关系与来源 Chunk；图谱抽取使用已配置的 OpenAI-Compatible LLM。",
    }


@router.get("/api/knowledge/databases/{knowledge_base_id}/graph-build/failed-chunks")
async def yuxi_failed_graph_chunks(knowledge_base_id: str, limit: int = 10):
    return await rag.knowledge_graph_failed_documents(knowledge_base_id, limit)


@router.post("/api/knowledge/databases/{knowledge_base_id}/graph-build/config")
async def yuxi_graph_config(knowledge_base_id: str, payload: dict = Body(...)):
    request = GraphConfigPayload.model_validate(payload)
    return await rag.configure_knowledge_graph(knowledge_base_id, request)


@router.post("/api/knowledge/databases/{knowledge_base_id}/graph-build/index")
async def yuxi_build_graph(knowledge_base_id: str):
    result = await rag.build_knowledge_graph_index(knowledge_base_id)
    return {**result, "build_task_status": result.get("status", "completed"), "message": "本地实体关系图谱构建完成"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/graph-build/reset")
async def yuxi_reset_graph(knowledge_base_id: str, payload: dict = Body(default={} )):
    return await rag.reset_knowledge_graph(knowledge_base_id)


@router.post("/api/knowledge/databases/{knowledge_base_id}/graph-build/reconcile")
async def yuxi_reconcile_graph(knowledge_base_id: str, payload: dict = Body(default={} )):
    return await rag.reconcile_knowledge_graph(knowledge_base_id, mode=str(payload.get("mode") or "failed"))


def _graph_view(knowledge_base_id: str) -> dict:
    knowledge_base = db.get_knowledge_base(knowledge_base_id)
    if not knowledge_base:
        raise HTTPException(status_code=404, detail="Knowledge base not found")
    if knowledge_base["kb_type"] != "local":
        raise HTTPException(status_code=404, detail="Graph API only supports local knowledge bases")
    saved = db.get_knowledge_view(knowledge_base_id, "graph")
    return saved["payload"] if saved else {"nodes": [], "edges": []}


def _graph_node(node: dict) -> dict:
    return {
        **node,
        "name": node.get("name") or node.get("label") or node.get("text") or node.get("id"),
        "normalized": {"type": node.get("type") or node.get("label_type") or "Entity"},
    }


@router.get("/api/graph/list")
async def yuxi_graph_list():
    graphs = [
        {
            "id": item["id"],
            "name": item["name"],
            "type": item["kb_type"],
            "description": item["description"],
            "status": "已连接",
            "created_at": item["created_at"],
            "metadata": rag._public_knowledge_base(item),
        }
        for item in db.list_knowledge_bases()
        if item["kb_type"] == "local"
    ]
    return {"success": True, "data": graphs}


@router.get("/api/graph/subgraph")
async def yuxi_graph_subgraph(
    kb_id: str,
    node_label: str = "*",
    max_depth: int = Query(default=2, ge=1, le=5),
    max_nodes: int = Query(default=100, ge=1, le=1000),
    exclude_chunk: bool = False,
):
    graph = _graph_view(kb_id)
    nodes = [_graph_node(item) for item in graph.get("nodes", [])]
    edges = graph.get("edges", [])
    if exclude_chunk:
        nodes = [node for node in nodes if node.get("type") != "Chunk"]
    by_id = {str(node["id"]): node for node in nodes}
    term = node_label.strip().casefold()
    seeds = list(by_id) if not term or term == "*" else [
        node_id for node_id, node in by_id.items()
        if term in str(node.get("name", "")).casefold()
        or term in str(node.get("type", "")).casefold()
    ]
    adjacency: dict[str, list[tuple[str, dict]]] = {node_id: [] for node_id in by_id}
    for edge in edges:
        source, target = str(edge.get("source", "")), str(edge.get("target", ""))
        if source in by_id and target in by_id:
            adjacency[source].append((target, edge))
            adjacency[target].append((source, edge))
    selected: dict[str, dict] = {}
    selected_edges: dict[str, dict] = {}
    frontier = seeds
    for depth in range(max_depth + 1):
        next_frontier = []
        for node_id in frontier:
            if node_id in selected or len(selected) >= max_nodes:
                continue
            selected[node_id] = by_id[node_id]
            if depth == max_depth:
                continue
            for neighbor, edge in adjacency[node_id]:
                edge_id = str(edge.get("id") or f"{edge.get('source')}:{edge.get('relation', edge.get('type', ''))}:{edge.get('target')}")
                selected_edges[edge_id] = {
                    **edge,
                    "id": edge_id,
                    "type": edge.get("type") or edge.get("relation") or edge.get("label") or "RELATED_TO",
                    "text": edge.get("text") or edge.get("relation") or edge.get("label") or "",
                }
                if neighbor not in selected:
                    next_frontier.append(neighbor)
        frontier = list(dict.fromkeys(next_frontier))
        if not frontier or len(selected) >= max_nodes:
            break
    selected_edges = {
        key: edge for key, edge in selected_edges.items()
        if str(edge.get("source")) in selected and str(edge.get("target")) in selected
    }
    return {"success": True, "data": {"nodes": list(selected.values()), "edges": list(selected_edges.values())}}


@router.get("/api/graph/stats")
async def yuxi_graph_stats(kb_id: str):
    graph = _graph_view(kb_id)
    nodes = graph.get("nodes", [])
    counts: dict[str, int] = {}
    for node in nodes:
        node_type = str(node.get("type") or node.get("label_type") or "Entity")
        if node_type not in {"document", "page", "chunk", "keyword"}:
            counts[node_type] = counts.get(node_type, 0) + 1
    return {"success": True, "data": {
        "total_nodes": len(nodes),
        "total_edges": len(graph.get("edges", [])),
        "entity_types": [{"type": key, "count": value} for key, value in sorted(counts.items())],
    }}


@router.get("/api/graph/labels")
async def yuxi_graph_labels(kb_id: str):
    graph = _graph_view(kb_id)
    labels = sorted({
        str(node.get("type") or node.get("label_type") or "Entity")
        for node in graph.get("nodes", [])
        if node.get("type") not in {"document", "page", "chunk", "keyword"}
    })
    return {"success": True, "data": {"labels": labels}}


@router.post("/api/evaluation/databases/{knowledge_base_id}/datasets/upload")
async def yuxi_upload_eval_dataset(knowledge_base_id: str, file: UploadFile = File(...), name: str = Form(""), description: str = Form("")):
    return await rag.upload_evaluation_dataset(knowledge_base_id, file)


@router.get("/api/evaluation/databases/{knowledge_base_id}/datasets")
async def yuxi_list_eval_datasets(knowledge_base_id: str):
    return await rag.list_knowledge_base_evaluation_datasets(knowledge_base_id)


@router.get("/api/evaluation/databases/{knowledge_base_id}/datasets/{dataset_id}")
async def yuxi_get_eval_dataset(knowledge_base_id: str, dataset_id: str, page: int = 1, page_size: int = 50):
    return await rag.get_knowledge_base_evaluation_dataset(knowledge_base_id, dataset_id, page, page_size)


@router.delete("/api/evaluation/datasets/{dataset_id}")
async def yuxi_delete_eval_dataset(dataset_id: str):
    for item in db.list_knowledge_bases():
        if db.get_evaluation_dataset(item["id"], dataset_id):
            return await rag.delete_knowledge_base_evaluation_dataset(item["id"], dataset_id)
    raise HTTPException(status_code=404, detail="评估集不存在")


@router.get("/api/evaluation/datasets/{dataset_id}/download")
async def yuxi_download_eval_dataset(dataset_id: str):
    for item in db.list_knowledge_bases():
        dataset = db.get_evaluation_dataset(item["id"], dataset_id)
        if dataset:
            content = json.dumps(dataset, ensure_ascii=False, indent=2).encode("utf-8")
            return Response(content=content, media_type="application/json", headers={"Content-Disposition": f'attachment; filename="{dataset_id}.json"'})
    raise HTTPException(status_code=404, detail="评估集不存在")


@router.post("/api/evaluation/databases/{knowledge_base_id}/datasets/generate")
@router.post("/api/evaluation/databases/{knowledge_base_id}/datasets/{dataset_id}/resume")
async def yuxi_generate_eval_dataset(knowledge_base_id: str, dataset_id: str | None = None, payload: dict = Body(default={} )):
    raise HTTPException(status_code=503, detail="评估集生成需要配置可用的 LLM；本地服务不会生成伪造数据")


@router.post("/api/evaluation/databases/{knowledge_base_id}/runs")
async def yuxi_run_eval(knowledge_base_id: str, payload: dict = Body(...)):
    request = EvaluationRunPayload.model_validate(payload)
    return await rag.run_knowledge_base_evaluation(knowledge_base_id, request)


@router.get("/api/evaluation/databases/{knowledge_base_id}/runs")
async def yuxi_list_eval_runs(knowledge_base_id: str):
    return await rag.list_knowledge_base_evaluation_runs(knowledge_base_id)


@router.get("/api/evaluation/databases/{knowledge_base_id}/runs/{run_id}")
async def yuxi_get_eval_run(knowledge_base_id: str, run_id: str, page: int = 1, page_size: int = 50, result_filter: str | None = None, error_only: bool | None = None):
    return await rag.get_knowledge_base_evaluation_run(knowledge_base_id, run_id)


@router.delete("/api/evaluation/databases/{knowledge_base_id}/runs/{run_id}")
async def yuxi_delete_eval_run(knowledge_base_id: str, run_id: str):
    return await rag.delete_knowledge_base_evaluation_run(knowledge_base_id, run_id)
