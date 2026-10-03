"""SQLite-backed task lifecycle for the embedded Yuxi RAG workbench."""

import asyncio
import json
import logging
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

from . import db
from .evaluation_generation import generate_benchmark_case
from .pipeline import index_existing_document, parse_existing_document

logger = logging.getLogger(__name__)
_executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="geo-rag-task")
_scheduled: set[str] = set()
_schedule_lock = threading.Lock()
_TERMINAL = {"success", "failed", "cancelled"}


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _task(row, *, details: bool = True) -> dict:
    result = dict(row)
    result["payload"] = json.loads(result.pop("payload_json", None) or "{}")
    result_json = result.pop("result_json", None)
    result["result"] = json.loads(result_json) if result_json else None
    result["cancel_requested"] = bool(result["cancel_requested"])
    if not details:
        result.pop("payload", None)
        result.pop("result", None)
    return result


def get_task(task_id: str, *, details: bool = True) -> dict | None:
    with db.connect() as connection:
        row = connection.execute("SELECT * FROM tasks WHERE id=?", (task_id,)).fetchone()
    return _task(row, details=details) if row else None


def list_tasks(status: str | None = None, limit: int = 100) -> dict:
    where = " WHERE status=?" if status else ""
    parameters = (status,) if status else ()
    with db.connect() as connection:
        total = connection.execute("SELECT COUNT(*) FROM tasks").fetchone()[0]
        filtered_total = connection.execute(
            "SELECT COUNT(*) FROM tasks" + where, parameters
        ).fetchone()[0]
        rows = connection.execute(
            "SELECT * FROM tasks" + where + " ORDER BY created_at DESC LIMIT ?",
            (*parameters, limit),
        ).fetchall()
        status_counts = {
            row["status"]: row["count"]
            for row in connection.execute(
                "SELECT status, COUNT(*) AS count FROM tasks" + where + " GROUP BY status",
                parameters,
            )
        }
        type_counts = {
            row["type"]: row["count"]
            for row in connection.execute(
                "SELECT type, COUNT(*) AS count FROM tasks" + where + " GROUP BY type",
                parameters,
            )
        }
    return {
        "tasks": [_task(row, details=False) for row in rows],
        "summary": {
            "total": total,
            "filtered_total": filtered_total,
            "status_counts": status_counts,
            "type_counts": type_counts,
        },
    }


def _create_task(name: str, task_type: str, payload: dict) -> str:
    task_id = uuid.uuid4().hex
    timestamp = _now()
    with db.connect() as connection:
        connection.execute(
            "INSERT INTO tasks(id, name, type, status, progress, message, payload_json, created_at, updated_at) "
            "VALUES (?, ?, ?, 'queued', 0, ?, ?, ?, ?)",
            (
                task_id,
                name,
                task_type,
                "任务已排队",
                json.dumps(payload, ensure_ascii=False),
                timestamp,
                timestamp,
            ),
        )
    return task_id


def _update_task(task_id: str, **values) -> None:
    allowed = {
        "status", "progress", "message", "result", "error", "cancel_requested",
        "started_at", "completed_at",
    }
    assignments = []
    parameters = []
    for key, value in values.items():
        if key not in allowed:
            raise ValueError(f"Unsupported task field: {key}")
        column = {"result": "result_json"}.get(key, key)
        if key == "result":
            value = json.dumps(value, ensure_ascii=False)
        elif key == "cancel_requested":
            value = int(bool(value))
        assignments.append(f"{column}=?")
        parameters.append(value)
    assignments.append("updated_at=?")
    parameters.extend((_now(), task_id))
    with db.connect() as connection:
        connection.execute(
            f"UPDATE tasks SET {', '.join(assignments)} WHERE id=?", parameters
        )


def request_cancel(task_id: str) -> dict | None:
    task = get_task(task_id)
    if not task or task["status"] in _TERMINAL:
        return None
    if task["status"] == "queued":
        _update_task(
            task_id,
            status="cancelled",
            message="任务已取消",
            error="任务已取消",
            cancel_requested=True,
            completed_at=_now(),
        )
    else:
        _update_task(task_id, cancel_requested=True, message="已收到取消请求")
    return get_task(task_id)


def delete_task(task_id: str) -> bool:
    with db.connect() as connection:
        cursor = connection.execute(
            "DELETE FROM tasks WHERE id=? AND status IN ('success', 'failed', 'cancelled')",
            (task_id,),
        )
    return cursor.rowcount > 0


def _schedule(task_id: str) -> None:
    with _schedule_lock:
        if task_id in _scheduled:
            return
        _scheduled.add(task_id)
    _executor.submit(_run_in_thread, task_id)


def _run_in_thread(task_id: str) -> None:
    try:
        asyncio.run(_run_task(task_id))
    except Exception:
        logger.exception("RAG task runner failed: task_id=%s", task_id)
    finally:
        with _schedule_lock:
            _scheduled.discard(task_id)


def submit_document_task(
    knowledge_base_id: str,
    document_ids: list[str],
    operation: str,
    params: dict | None = None,
    auto_index: bool = False,
) -> dict:
    ids = list(dict.fromkeys(str(item) for item in document_ids))
    if operation not in {"parse", "index", "ingest"}:
        raise ValueError("Unsupported document task operation")
    if not ids:
        return {
            "operation": operation,
            "status": "success",
            "message": "没有待处理文件",
            "processed": [],
            "failed": [],
            "queued_count": 0,
        }
    task_type = {
        "parse": "knowledge_parse",
        "index": "knowledge_index",
        "ingest": "knowledge_ingest",
    }[operation]
    label = {"parse": "解析", "index": "入库", "ingest": "导入"}[operation]
    payload = {
        "knowledge_base_id": knowledge_base_id,
        "document_ids": ids,
        "operation": operation,
        "params": params or {},
        "auto_index": bool(auto_index),
    }
    task_id = _create_task(f"文档{label} ({knowledge_base_id})", task_type, payload)
    _schedule(task_id)
    return {
        "operation": operation,
        "status": "queued",
        "task_id": task_id,
        "queued_count": len(ids),
        "message": f"{label}任务已提交，{len(ids)} 个文件进入队列",
    }


def submit_evaluation_dataset_task(
    knowledge_base_id: str,
    params: dict,
    dataset_id: str | None = None,
) -> dict:
    knowledge_base = db.get_knowledge_base(knowledge_base_id)
    if not knowledge_base:
        raise ValueError("知识库不存在")
    if knowledge_base["kb_type"] != "local":
        raise ValueError("仅本地知识库支持自动生成评估集")

    resumed = dataset_id is not None
    if resumed:
        dataset = db.get_evaluation_dataset(knowledge_base_id, dataset_id)
        if not dataset:
            raise ValueError("评估数据集不存在")
        metadata = dataset["build_metadata"]
        if metadata.get("source") != "generated" or not metadata.get("params"):
            raise ValueError("只能恢复有生成参数的自动生成数据集")
        params = metadata["params"]
        if len(dataset["cases"]) >= int(params["count"]):
            metadata.update(status="completed", progress=100, message="完成")
            db.update_evaluation_dataset(knowledge_base_id, dataset_id, build_metadata=metadata)
            return {"dataset_id": dataset_id, "message": "数据集已完成生成"}
        with db.connect() as connection:
            rows = connection.execute(
                "SELECT * FROM tasks WHERE type='dataset_generation' AND status IN ('queued', 'running') ORDER BY created_at DESC"
            ).fetchall()
        for row in rows:
            existing = _task(row)
            if existing["payload"].get("dataset_id") == dataset_id:
                return {
                    "dataset_id": dataset_id,
                    "task_id": existing["id"],
                    "message": "已有进行中的生成任务",
                }
    else:
        dataset_id = f"dataset_{uuid.uuid4().hex[:8]}"

    task_payload = {
        "knowledge_base_id": knowledge_base_id,
        "dataset_id": dataset_id,
        **params,
    }
    task_id = _create_task(
        "继续生成评估数据集" if resumed else "生成评估数据集",
        "dataset_generation",
        task_payload,
    )
    if not resumed:
        dataset = db.create_evaluation_dataset(
            knowledge_base_id,
            str(params["name"]).strip(),
            [],
            description=str(params.get("description") or ""),
            dataset_id=dataset_id,
            build_metadata={"source": "generated", "status": "pending", "progress": 0, "params": params},
        )
    metadata = dict(dataset["build_metadata"])
    metadata.update(status="pending", progress=0, task_id=task_id, message="评估数据集生成任务已提交")
    db.update_evaluation_dataset(knowledge_base_id, dataset_id, build_metadata=metadata)
    _schedule(task_id)
    return {
        "dataset_id": dataset_id,
        "task_id": task_id,
        "message": "评估数据集生成任务已恢复" if resumed else "评估数据集生成任务已提交",
    }


def _cancel_requested(task_id: str) -> bool:
    task = get_task(task_id, details=False)
    return bool(task and task["cancel_requested"])


async def _process_documents(task_id: str, payload: dict) -> dict:
    knowledge_base_id = payload["knowledge_base_id"]
    document_ids = payload["document_ids"]
    operation = payload["operation"]
    params = payload.get("params") or {}
    processed = []
    failed = []
    total = len(document_ids)

    for index, document_id in enumerate(document_ids, 1):
        if _cancel_requested(task_id):
            break
        document = db.get_document(document_id)
        if not document or document["knowledge_base_id"] != knowledge_base_id:
            failed.append({"document_id": document_id, "error_message": "Document not found"})
        else:
            try:
                if operation == "parse":
                    result = await parse_existing_document(document_id, params)
                elif operation == "index":
                    result = await index_existing_document(document_id, params)
                else:
                    parsed = await parse_existing_document(document_id)
                    result = (
                        await index_existing_document(document_id)
                        if payload.get("auto_index")
                        else parsed
                    )
                processed.append(result)
            except Exception as exc:
                failed.append({"document_id": document_id, "error_message": str(exc)[:500]})
        _update_task(
            task_id,
            progress=round(index / total * 95, 1),
            message=f"正在{({'parse': '解析', 'index': '入库', 'ingest': '导入'}[operation])}第 {index}/{total} 个文件",
            result={"operation": operation, "processed": processed, "failed": failed},
        )
    return {
        "operation": operation,
        "processed": processed,
        "failed": failed,
        "cancelled": _cancel_requested(task_id),
    }


async def _process_evaluation_dataset(task_id: str, payload: dict) -> dict:
    knowledge_base_id = payload["knowledge_base_id"]
    dataset_id = payload["dataset_id"]
    dataset = db.get_evaluation_dataset(knowledge_base_id, dataset_id)
    if not dataset:
        raise ValueError("评估数据集不存在")
    params = dataset["build_metadata"].get("params") or payload
    chunks = [
        {
            "id": chunk["id"],
            "content": chunk["text"],
            "file_id": chunk["document_id"],
            "chunk_index": chunk["chunk_index"],
        }
        for chunk in db.get_chunks(include_embedding=False)
        if chunk.get("knowledge_base_id") == knowledge_base_id
        and chunk.get("document_status") == "indexed"
        and chunk.get("text")
    ]
    if not chunks:
        raise ValueError("知识库为空或未解析到 chunks")

    total = int(params["count"])
    cases = list(dataset["cases"])
    attempts = max(total * 5, 50)
    concurrency = max(1, min(int(params.get("concurrency_count", 10)), 20))
    metadata = dict(dataset["build_metadata"])
    metadata.update(status="running", progress=0, message="准备生成评估题")
    db.update_evaluation_dataset(knowledge_base_id, dataset_id, build_metadata=metadata)
    attempt = 0
    while len(cases) < total and attempt < attempts:
        if _cancel_requested(task_id):
            raise InterruptedError("评估集生成任务已取消")
        batch_size = min(concurrency, total - len(cases), attempts - attempt)
        generated = await asyncio.gather(*(
            generate_benchmark_case(
                knowledge_base_id=knowledge_base_id,
                chunks=chunks,
                model_spec=str(params["llm_model_spec"]),
                neighbors_count=int(params.get("neighbors_count", 1)),
                generation_mode=str(params.get("generation_mode", "vector")),
                graph_expand_top_k=int(params.get("graph_expand_top_k", 1)),
            )
            for _ in range(batch_size)
        ), return_exceptions=True)
        attempt += batch_size
        failure = next((item for item in generated if isinstance(item, BaseException)), None)
        for item in generated:
            if isinstance(item, BaseException) or not item or len(cases) >= total:
                continue
            cases.append({
                **item,
                "relevant_evidence_ids": [f"EV-{chunk_id}" for chunk_id in item["gold_chunk_ids"]],
                "top_k": 5,
                "filters": {},
            })
        if cases:
            metadata["progress"] = round(len(cases) * 99 / max(total, 1))
        metadata["message"] = f"已生成 {len(cases)}/{total} 道评估题"
        db.update_evaluation_dataset(knowledge_base_id, dataset_id, cases=cases, build_metadata=metadata)
        _update_task(
            task_id,
            progress=round(len(cases) * 95 / max(total, 1)),
            message=metadata.get("message", "正在生成评估题"),
            result={"dataset_id": dataset_id, "item_count": len(cases)},
        )
        if _cancel_requested(task_id):
            raise InterruptedError("评估集生成任务已取消")
        if failure is not None:
            raise failure

    if len(cases) < total:
        raise ValueError(f"仅生成 {len(cases)}/{total} 道有效评估题")
    metadata.update(status="completed", progress=100, message="完成")
    db.update_evaluation_dataset(knowledge_base_id, dataset_id, cases=cases, build_metadata=metadata)
    return {"dataset_id": dataset_id, "item_count": len(cases), "build_metadata": metadata}


async def _run_task(task_id: str) -> None:
    task = get_task(task_id)
    if not task or task["status"] in _TERMINAL:
        return
    if task["cancel_requested"]:
        _update_task(task_id, status="cancelled", message="任务已取消", error="任务已取消", completed_at=_now())
        return
    started_at = _now()
    _update_task(task_id, status="running", progress=0, message="准备处理文件", started_at=started_at)
    try:
        result = (
            await _process_evaluation_dataset(task_id, task["payload"])
            if task["type"] == "dataset_generation"
            else await _process_documents(task_id, task["payload"])
        )
        cancelled = result.pop("cancelled", False)
        failure_count = len(result.get("failed", []))
        current = get_task(task_id) or task
        _update_task(
            task_id,
            status="cancelled" if cancelled else "success",
            progress=100 if not cancelled else current.get("progress", 0),
            message=(
                "任务已取消" if cancelled
                else "评估数据集生成任务已完成" if task["type"] == "dataset_generation"
                else f"任务已完成，失败 {failure_count} 个"
            ),
            error="任务已取消" if cancelled else None,
            result=result,
            completed_at=_now(),
        )
    except Exception as exc:
        logger.exception("RAG document task failed: task_id=%s", task_id)
        if task["type"] == "dataset_generation":
            payload = task["payload"]
            dataset = db.get_evaluation_dataset(payload["knowledge_base_id"], payload["dataset_id"])
            if dataset:
                metadata = dict(dataset["build_metadata"])
                metadata.update(status="failed", error_message=str(exc)[:1000], message=str(exc)[:1000])
                db.update_evaluation_dataset(payload["knowledge_base_id"], payload["dataset_id"], build_metadata=metadata)
        terminal_status = "cancelled" if isinstance(exc, InterruptedError) else "failed"
        _update_task(
            task_id,
            status=terminal_status,
            message="任务执行失败",
            error=str(exc)[:1000],
            completed_at=_now(),
        )


def _recover_interrupted_task(task: dict) -> None:
    payload = task.get("payload") or {}
    operation = payload.get("operation")
    for document_id in payload.get("document_ids") or []:
        document = db.get_document(document_id)
        if not document:
            continue
        status = document["status"]
        if status == "parsing":
            db.update_document_status(document_id, "uploaded")
        elif status in {"chunking", "embedding"} and db.get_blocks(document_id):
            db.update_document_status(document_id, "parsed")
    _update_task(
        task["id"],
        status="queued",
        progress=0,
        message="服务重启后重新排队",
        started_at=None,
        completed_at=None,
        error=None,
    )


def resume_pending_tasks() -> None:
    with db.connect() as connection:
        rows = connection.execute(
            "SELECT * FROM tasks WHERE status IN ('queued', 'running') ORDER BY created_at"
        ).fetchall()
    for row in rows:
        task = _task(row)
        if task["status"] == "running":
            _recover_interrupted_task(task)
        _schedule(task["id"])


def public_task(task: dict | None) -> dict | None:
    if not task:
        return None

    def redact(value):
        if isinstance(value, dict):
            return {
                key: "********" if any(secret in key.lower() for secret in ("api_key", "token", "secret"))
                else redact(item)
                for key, item in value.items()
            }
        if isinstance(value, list):
            return [redact(item) for item in value]
        return value

    result = dict(task)
    result["payload"] = redact(result.get("payload") or {})
    return result
