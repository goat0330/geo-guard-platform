import asyncio
import time
from pathlib import Path
from threading import Event

import pytest
from fastapi.testclient import TestClient

from app import db
from app.config import settings
from app.main import app


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "rag_workspace_dir", str(tmp_path / "workspace"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "rerank_base_url", "")
    monkeypatch.setattr(settings, "rerank_api_key", "")
    monkeypatch.setattr(settings, "rerank_model", "")
    monkeypatch.setattr(settings, "mineru_enabled", False)
    monkeypatch.setattr(settings, "mineru_api_uri", "")
    monkeypatch.setattr(settings, "mineru_api_key", "")
    return TestClient(app)


def _knowledge_base(client, name="任务 API 验收"):
    response = client.post(
        "/api/knowledge/databases",
        json={"database_name": name, "kb_type": "local"},
    )
    assert response.status_code == 200, response.text
    return response.json()["kb_id"]


def _document(tmp_path: Path, knowledge_base_id: str, name: str = "巡查记录.txt"):
    path = tmp_path / name
    path.write_text("坡体裂缝巡查记录，连续降雨后应复核位移。", encoding="utf-8")
    return db.create_document(
        name,
        str(path),
        "text/plain",
        "plain-text",
        "general",
        {},
        {},
        knowledge_base_id=knowledge_base_id,
    )


def _wait_for_terminal(client, task_id: str, timeout: float = 5):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        response = client.get(f"/api/tasks/{task_id}")
        assert response.status_code == 200, response.text
        task = response.json()["task"]
        if task["status"] in {"success", "failed", "cancelled"}:
            return task
        time.sleep(0.02)
    pytest.fail(f"Task {task_id} did not finish before timeout")


def test_parse_task_runs_and_matches_yuxi_task_api(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb_id = _knowledge_base(client)
    document_id = _document(tmp_path, kb_id)

    submitted = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/parse",
        json={"file_ids": [document_id, document_id]},
    )
    assert submitted.status_code == 200, submitted.text
    submission = submitted.json()
    assert submission["status"] == "queued"
    assert submission["queued_count"] == 1

    task = _wait_for_terminal(client, submission["task_id"])
    assert task["type"] == "knowledge_parse"
    assert task["status"] == "success"
    assert task["progress"] == 100
    assert task["result"]["processed"][0]["document_id"] == document_id
    assert db.get_document(document_id)["status"] == "parsed"

    listing = client.get("/api/tasks", params={"status": "success", "limit": 10}).json()
    assert listing["summary"]["status_counts"]["success"] == 1
    assert listing["summary"]["type_counts"]["knowledge_parse"] == 1
    assert listing["tasks"][0]["id"] == submission["task_id"]
    assert "payload" not in listing["tasks"][0]
    assert client.delete(f"/api/tasks/{submission['task_id']}").status_code == 200
    assert client.get(f"/api/tasks/{submission['task_id']}").status_code == 404


def test_running_task_cancel_stops_before_next_document(tmp_path, monkeypatch):
    from app import task_runtime

    client = _client(tmp_path, monkeypatch)
    kb_id = _knowledge_base(client, "任务取消验收")
    document_ids = [
        _document(tmp_path, kb_id, "第一份.txt"),
        _document(tmp_path, kb_id, "第二份.txt"),
    ]
    started = Event()

    async def slow_parse(document_id, _params):
        started.set()
        await asyncio.sleep(0.15)
        db.update_document_status(document_id, "parsed")
        return {"document_id": document_id, "status": "parsed"}

    monkeypatch.setattr(task_runtime, "parse_existing_document", slow_parse)
    submitted = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/parse",
        json={"file_ids": document_ids},
    ).json()
    assert started.wait(3)
    cancel = client.post(f"/api/tasks/{submitted['task_id']}/cancel")
    assert cancel.status_code == 200, cancel.text
    task = _wait_for_terminal(client, submitted["task_id"])
    assert task["status"] == "cancelled"
    assert len(task["result"]["processed"]) == 1
    assert db.get_document(document_ids[1])["status"] == "uploaded"


def test_task_runner_failure_is_visible_and_terminal_tasks_are_deletable(tmp_path, monkeypatch):
    from app import task_runtime

    client = _client(tmp_path, monkeypatch)
    kb_id = _knowledge_base(client, "任务失败验收")
    document_id = _document(tmp_path, kb_id)

    async def fail_runner(_task_id, _payload):
        raise RuntimeError("fixture parser failure")

    monkeypatch.setattr(task_runtime, "_process_documents", fail_runner)
    submitted = client.post(
        f"/api/knowledge/databases/{kb_id}/documents/parse",
        json={"file_ids": [document_id]},
    ).json()
    task = _wait_for_terminal(client, submitted["task_id"])
    assert task["status"] == "failed"
    assert "fixture parser failure" in task["error"]
    assert client.post(f"/api/tasks/{submitted['task_id']}/cancel").status_code == 400
    assert client.delete(f"/api/tasks/{submitted['task_id']}").status_code == 200


def test_task_management_requires_yuxi_admin_role(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    task_id = "task-not-found"
    user_headers = {"X-Geo-User-UID": "42", "X-Geo-User-Role": "user"}

    responses = [
        client.get("/api/tasks", headers=user_headers),
        client.get(f"/api/tasks/{task_id}", headers=user_headers),
        client.post(f"/api/tasks/{task_id}/cancel", headers=user_headers),
        client.delete(f"/api/tasks/{task_id}", headers=user_headers),
    ]
    assert [response.status_code for response in responses] == [403, 403, 403, 403]
    assert all(response.json()["detail"] == "需要管理员权限" for response in responses)

    admin_headers = {"X-Geo-User-UID": "7", "X-Geo-User-Role": "admin"}
    assert client.get("/api/tasks", headers=admin_headers).status_code == 200
    assert client.get(f"/api/tasks/{task_id}", headers=admin_headers).status_code == 404
