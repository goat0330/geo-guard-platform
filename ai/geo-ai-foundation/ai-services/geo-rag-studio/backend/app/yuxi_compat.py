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
from html.parser import HTMLParser
from pathlib import Path, PurePosixPath
from urllib.parse import quote, unquote, urlsplit

import httpx
from fastapi import APIRouter, Body, File, Form, HTTPException, Query, UploadFile
from fastapi.responses import FileResponse, Response

from . import db, main as rag
from .config import settings
from .schemas import DocumentBatchPayload, DocumentMovePayload, EmbeddingTestRequest, EvaluationRunPayload, GraphConfigPayload, KnowledgeBasePayload, KnowledgeBaseUpdatePayload, MindMapPayload, RerankerTestRequest, RetrieveRequest
from .yuxi_port.chunk_presets import get_options
from .parser import parse_file_markdown

router = APIRouter()
STAGE_URI = "local-stage://"
SHARE_CONFIG = {"version": 2, "read_scope": {"access_level": "global", "department_ids": [], "user_uids": []}, "manage_scope": None}


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
        "status": document.get("status", "uploaded"),
        "parser": document.get("parser"),
        "chunk_preset_id": document.get("chunk_preset_id"),
        "chunk_count": document.get("chunk_count", 0),
        "token_count": document.get("token_count", 0),
        "embedding_chunk_count": document.get("embedding_chunk_count", 0),
        "error_message": document.get("error_message"),
        "created_at": document.get("created_at"),
        "folder_id": document.get("folder_id"),
        "parent_id": document.get("folder_id"),
        "is_folder": False,
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
    total_size = sum(int((item.get("metadata") or {}).get("file_size") or 0) for item in documents)
    status_counts = {status: sum(item.get("status") == status for item in documents) for status in ("uploaded", "parsing", "parsed", "chunking", "embedding", "indexed", "failed")}
    return {
        "file_count": len(documents), "row_count": len(documents), "total_size": total_size,
        "chunk_count": sum(item.get("chunk_count", 0) for item in documents),
        "token_count": sum(item.get("token_count", 0) for item in documents),
        "pending_parse_count": status_counts["uploaded"],
        "processing_count": status_counts["parsing"] + status_counts["chunking"] + status_counts["embedding"],
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
        "share_config": SHARE_CONFIG,
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


@router.get("/api/knowledge/databases")
async def yuxi_list_databases():
    return {"databases": [_yuxi_knowledge_base(item) for item in db.list_knowledge_bases()]}


@router.get("/api/knowledge/databases/accessible")
async def yuxi_list_accessible_databases():
    return {"databases": [_yuxi_knowledge_base(item) for item in db.list_knowledge_bases()]}


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
    raise HTTPException(status_code=503, detail="描述生成需要配置可用的 Chat Completion 模型；当前未生成伪造内容")


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
):
    rag._knowledge_base_or_404(knowledge_base_id)
    documents = db.list_documents(knowledge_base_id)
    if status != "all":
        documents = [item for item in documents if item.get("status") == status]
    if parent_id:
        documents = [item for item in documents if item.get("folder_id") == parent_id]
    if path_prefix:
        documents = [item for item in documents if str((item.get("metadata") or {}).get("relative_path", "")).startswith(path_prefix)]
    folders = db.list_folders(knowledge_base_id)
    if parent_id is not None:
        folders = [item for item in folders if item.get("parent_id") == parent_id]
    elif not recursive:
        folders = [item for item in folders if item.get("parent_id") is None]
    combined = [_yuxi_folder(item) for item in folders] + [_yuxi_document(item) for item in documents]
    total = len(combined)
    page = max(1, page)
    page_size = max(1, min(page_size, 500))
    start = (page - 1) * page_size
    return {"items": combined[start:start + page_size], "page": page, "page_size": page_size, "total": total,
            "has_more": start + page_size < total, "path_prefix": path_prefix, "stats": _kb_stats(knowledge_base_id)}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/search")
async def yuxi_search_documents(knowledge_base_id: str, q: str = "", page: int = 1, page_size: int = 100):
    result = await yuxi_list_documents(knowledge_base_id, page=page, page_size=page_size)
    query = q.casefold().strip()
    if query:
        result["items"] = [item for item in result["items"] if query in item.get("filename", "").casefold()]
    result["total"] = len(result["items"])
    return result


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
    return await rag.remove_folder(knowledge_base_id, folder_id)


@router.put("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/move")
async def yuxi_move_document(knowledge_base_id: str, document_id: str, payload: dict = Body(...)):
    result = await rag.move_knowledge_base_document(knowledge_base_id, document_id, DocumentMovePayload(folder_id=payload.get("new_parent_id")))
    return {**result, "new_parent_id": result.get("folder_id")}


def _staging_paths(stage_id: str) -> tuple[Path, Path]:
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
    data_path, metadata_path = _staging_paths(stage_id)
    data_path.parent.mkdir(parents=True, exist_ok=True)
    data_path.write_bytes(content)
    digest = hashlib.sha256(content).hexdigest()
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
        "has_same_name": False,
        "same_name_files": [],
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


@router.post("/api/knowledge/files/import-workspace")
async def yuxi_import_workspace_files(payload: dict = Body(...)):
    raise HTTPException(status_code=501, detail="Yuxi 个人工作区未迁移；请从本机上传文件到知识库")


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents")
@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/add")
async def yuxi_add_documents(knowledge_base_id: str, payload: dict = Body(...)):
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
    embedding = config.get("embedding") or {}
    processed, failed = [], []
    stage_root = (Path(settings.rag_upload_dir) / ".yuxi_staging").resolve()
    upload_root = Path(settings.rag_upload_dir).resolve()
    for item in items:
        if not isinstance(item, str) or not item.startswith(STAGE_URI):
            failed.append({"item": str(item), "error_message": "该文件不是本地服务上传的暂存文件"})
            continue
        stage_id = item.removeprefix(STAGE_URI)
        data_path, metadata_path = _staging_paths(stage_id)
        if not data_path.is_file() or not metadata_path.is_file() or data_path.resolve().parent != stage_root:
            failed.append({"item": item, "error_message": "上传暂存文件不存在或已过期"})
            continue
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        if metadata.get("knowledge_base_id") not in (None, knowledge_base_id):
            failed.append({"item": item, "error_message": "暂存文件属于另一个知识库"})
            continue
        source_paths = params.get("source_paths") or {}
        source_value = source_paths.get(item, metadata["filename"]) if isinstance(source_paths, dict) else metadata["filename"]
        try:
            source_path = PurePosixPath(str(source_value).replace("\\", "/"))
            if source_path.is_absolute() or any(part in {".", ".."} for part in source_path.parts) or ":" in source_path.parts[0]:
                raise ValueError("文件相对路径无效")
        except (IndexError, ValueError) as exc:
            failed.append({"item": item, "error_message": str(exc) or "文件相对路径无效"})
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
            result = await rag.parse_existing_document(document_id)
            if params.get("auto_index"):
                result = await rag.index_existing_document(document_id)
            processed.append({"document_id": document_id, "file_id": document_id, "result": result})
        except Exception as exc:
            db.update_document_status(document_id, "failed", str(exc)[:500])
            failed.append({"document_id": document_id, "file_id": document_id, "error_message": str(exc)[:500]})
    result_status = "success" if not failed else "error" if not processed else "partial"
    return {"status": result_status, "message": f"已处理 {len(processed)} 个文件，失败 {len(failed)} 个", "processed": processed, "failed": failed}


def _batch_request(payload: dict) -> DocumentBatchPayload:
    file_ids = payload.get("file_ids") or payload.get("document_ids") or []
    return DocumentBatchPayload(document_ids=[str(item) for item in file_ids])


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/parse")
async def yuxi_parse_documents(knowledge_base_id: str, payload: dict = Body(...)):
    result = await rag.parse_knowledge_base_documents(knowledge_base_id, _batch_request(payload))
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"解析完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/parse-pending")
async def yuxi_parse_pending(knowledge_base_id: str, payload: dict = Body(default={} )):
    result = await rag.parse_pending_knowledge_base_documents(knowledge_base_id)
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"解析完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/index")
async def yuxi_index_documents(knowledge_base_id: str, payload: dict = Body(...)):
    result = await rag.index_knowledge_base_documents(knowledge_base_id, _batch_request(payload))
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"入库完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.post("/api/knowledge/databases/{knowledge_base_id}/documents/index-pending")
async def yuxi_index_pending(knowledge_base_id: str, payload: dict = Body(default={} )):
    result = await rag.index_pending_knowledge_base_documents(knowledge_base_id)
    return {**result, "status": "success" if not result["failed"] else "partial", "message": f"入库完成 {len(result['processed'])} 个，失败 {len(result['failed'])} 个"}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/basic")
async def yuxi_get_document_basic(knowledge_base_id: str, document_id: str):
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
    return {"document_id": document_id, "file_id": document_id, "filename": document["file_name"], "blocks": blocks, "chunks": chunks, "text": "\\n\\n".join(item["text"] for item in blocks)}


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}/download")
async def yuxi_download_document(knowledge_base_id: str, document_id: str):
    return await rag.download_knowledge_base_document(knowledge_base_id, document_id)


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
    result = await rag.batch_delete_knowledge_base_documents(knowledge_base_id, DocumentBatchPayload(document_ids=payload))
    return {"deleted_count": len(result["deleted"]), "failed_items": result["not_found"], **result}


@router.delete("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}")
async def yuxi_delete_document(knowledge_base_id: str, document_id: str):
    return await rag.delete_knowledge_base_document(knowledge_base_id, document_id)


@router.get("/api/knowledge/databases/{knowledge_base_id}/documents/{document_id}")
async def yuxi_get_document(knowledge_base_id: str, document_id: str):
    return await yuxi_get_document_basic(knowledge_base_id, document_id)


def _retrieval_options(knowledge_base: dict) -> dict:
    config = (knowledge_base.get("config") or {}).get("retrieval") or {}
    options = [
        {"key": "search_mode", "label": "检索模式", "type": "select", "default": config.get("search_mode", "hybrid"), "options": [
            {"label": "混合检索", "value": "hybrid"}, {"label": "向量检索", "value": "vector"}, {"label": "关键词检索", "value": "keyword"}
        ]},
        {"key": "final_top_k", "label": "最终返回 Chunk 数", "type": "number", "default": config.get("final_top_k", 8), "min": 1, "max": 100},
        {"key": "similarity_threshold", "label": "相似度阈值（0-1）", "type": "number", "default": config.get("similarity_threshold", 0), "min": 0, "max": 1, "step": 0.01},
        {"key": "recall_top_k", "label": "召回数量", "type": "number", "default": config.get("recall_top_k", 50), "min": 1, "max": 200},
        {"key": "bm25_top_k", "label": "BM25 召回数量", "type": "number", "default": config.get("bm25_top_k", 50), "min": 1, "max": 200},
        {"key": "vector_weight", "label": "向量检索权重", "type": "number", "default": config.get("vector_weight", 0.7), "min": 0, "max": 1, "step": 0.05},
        {"key": "bm25_weight", "label": "BM25 权重", "type": "number", "default": config.get("bm25_weight", 0.3), "min": 0, "max": 1, "step": 0.05},
        {"key": "bm25_drop_ratio_search", "label": "BM25 稀疏项丢弃比例", "type": "number", "default": config.get("bm25_drop_ratio_search", 0), "min": 0, "max": 1, "step": 0.01},
        {"key": "use_reranker", "label": "启用重排序", "type": "boolean", "default": config.get("use_reranker", False)},
        {"key": "use_graph_retrieval", "label": "启用图检索", "type": "boolean", "default": config.get("use_graph_retrieval", False)},
    ]
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
    return [
        {"id": item["evidence_id"], "content": item["text"], "text": item["text"],
         "score": item.get("rerank_score") or item.get("fusion_score") or item.get("bm25_score") or item.get("vector_score") or 0,
         "metadata": {"file_id": item["document_id"], "source": item["file_name"], "chunk_id": item["chunk_id"], "page": item.get("page"), "bbox": item.get("bbox")}}
        for item in result.get("evidences", [])
    ]


@router.get("/api/knowledge/databases/{knowledge_base_id}/sample-questions")
async def yuxi_get_sample_questions(knowledge_base_id: str):
    result = await rag.get_sample_questions(knowledge_base_id)
    return result


@router.post("/api/knowledge/databases/{knowledge_base_id}/sample-questions")
async def yuxi_save_sample_questions(knowledge_base_id: str, payload: dict = Body(...)):
    if not isinstance(payload.get("questions"), list):
        raise HTTPException(status_code=503, detail="生成示例问题需要配置可用的 LLM；当前服务不会返回预设问题冒充模型结果")
    return await rag.save_sample_questions(knowledge_base_id, payload)


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
