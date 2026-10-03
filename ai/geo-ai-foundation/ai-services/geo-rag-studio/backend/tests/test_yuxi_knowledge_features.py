import json

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


@pytest.mark.parametrize("operation", ["single", "batch", "folder"])
@pytest.mark.parametrize("content_outdated", [False, True])
def test_deleting_files_prunes_saved_mindmap_without_model(tmp_path, monkeypatch, operation, content_outdated):
    from app import db, knowledge_features

    client = _client(tmp_path, monkeypatch)
    kb_id = client.post("/api/knowledge/databases", json={"database_name": "导图删除测试", "kb_type": "local"}).json()["kb_id"]
    doc_ids = []
    names = ["removed.txt", "retained.txt"]
    for name in names:
        added = client.post("/api/v1/documents/text", json={"file_name": name, "text": "Slope inspection and rainfall.", "knowledge_base_id": kb_id})
        assert added.status_code == 200
        doc_ids.append(added.json()["document_id"])
    fingerprint = "previously-outdated-content" if content_outdated else knowledge_features.indexed_content_fingerprint(doc_ids)
    # This is a saved-tree fixture for deletion tests, not evidence of LLM generation.
    tree = {"content": "Knowledge", "children": [
        {"content": "Empty after removal", "children": [{"content": names[0], "children": []}]},
        {"content": "Preserved", "children": [{"content": names[1], "children": []}]},
    ]}
    db.save_knowledge_view(kb_id, "mindmap", "", {
        "mindmap": tree, "source_document_ids": doc_ids,
        "source_document_names": dict(zip(doc_ids, names)), "source_fingerprint": fingerprint,
    })
    def unexpected_model(*args, **kwargs):
        pytest.fail("Deleting a file must not call a model")
    monkeypatch.setattr(knowledge_features, "_chat_completion", unexpected_model)
    prefix = f"/api/knowledge/databases/{kb_id}"
    if operation == "batch":
        removed = client.request("DELETE", f"{prefix}/documents/batch", json=[doc_ids[0], "missing-fixture"])
        assert removed.status_code == 200
        assert removed.json()["deleted_count"] == 1
    elif operation == "folder":
        folder_id = client.post(f"{prefix}/folders", json={"folder_name": "Remove folder"}).json()["file_id"]
        assert client.put(f"{prefix}/documents/{doc_ids[0]}/move", json={"new_parent_id": folder_id}).status_code == 200
        assert client.delete(f"{prefix}/documents/{folder_id}").status_code == 200
    else:
        assert client.delete(f"{prefix}/documents/{doc_ids[0]}").status_code == 200
    saved = client.get(f"{prefix}/mindmap").json()
    assert saved["mindmap"] == {"content": "Knowledge", "children": [tree["children"][1]]}
    assert saved["source_document_ids"] == [doc_ids[1]]
    assert saved["source_document_names"] == {doc_ids[1]: names[1]}
    diff = client.get(f"{prefix}/mindmap/diff").json()
    assert diff["needs_update"] is content_outdated
    assert db.get_document(doc_ids[1]) is not None


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    monkeypatch.setattr(settings, "embedding_base_url", "")
    monkeypatch.setattr(settings, "embedding_api_key", "")
    monkeypatch.setattr(settings, "embedding_model", "")
    monkeypatch.setattr(settings, "rerank_base_url", "")
    monkeypatch.setattr(settings, "rerank_api_key", "")
    monkeypatch.setattr(settings, "rerank_model", "")
    return TestClient(app)


@pytest.mark.parametrize("add_file", [False, True])
def test_legacy_mindmap_diff_recovers_tracking_from_upstream_leaf_names(tmp_path, monkeypatch, add_file):
    from app import db

    client = _client(tmp_path, monkeypatch)
    kb_id = client.post("/api/knowledge/databases", json={"database_name": "Legacy mindmap", "kb_type": "local"}).json()["kb_id"]
    first = client.post("/api/v1/documents/text", json={"knowledge_base_id": kb_id, "file_name": "tracked.txt", "text": "Rainfall slope inspection."}).json()["document_id"]
    # Saved legacy fixture with no ID map or fingerprint, not model generation.
    db.save_knowledge_view(kb_id, "mindmap", "", {"mindmap": {
        "content": "Legacy", "children": [{"content": "tracked.txt", "children": []}],
    }})
    added_id = None
    if add_file:
        added_id = client.post("/api/v1/documents/text", json={"knowledge_base_id": kb_id, "file_name": "new.txt", "text": "New inspection."}).json()["document_id"]
    diff = client.get(f"/api/knowledge/databases/{kb_id}/mindmap/diff").json()
    assert diff["tracked_files"] == [first]
    assert diff["unchanged_count"] == 1
    assert diff["removed_file_ids"] == []
    assert [item["file_id"] for item in diff["added_files"]] == ([added_id] if add_file else [])
    assert diff["needs_update"] is add_file
    assert client.delete(f"/api/knowledge/databases/{kb_id}/documents/{first}").status_code == 200
    saved = client.get(f"/api/knowledge/databases/{kb_id}/mindmap").json()
    assert saved["mindmap"]["children"] == []
    assert saved["source_document_ids"] == []


def test_native_mindmap_file_list_includes_uploaded_files_and_upstream_envelope(tmp_path, monkeypatch):
    from app import db

    client = _client(tmp_path, monkeypatch)
    kb_id = client.post("/api/knowledge/databases", json={"database_name": "File organization", "kb_type": "local"}).json()["kb_id"]
    document = db.create_document("unparsed.pdf", None, "application/pdf", "pymupdf-layout", "general", {}, {}, knowledge_base_id=kb_id)
    response = client.get(f"/api/knowledge/databases/{kb_id}/mindmap/files")
    assert response.status_code == 200
    payload = response.json()
    assert isinstance(payload, dict), "Yuxi frontend reads response.files, not a bare array"
    assert payload["kb_id"] == kb_id and payload["db_name"] == "File organization"
    assert payload["total"] == 1 and payload["truncated"] is False
    assert payload["files"][0]["file_id"] == document
    assert payload["files"][0]["filename"] == "unparsed.pdf"
    assert payload["files"][0]["type"] == "pdf"
    assert payload["files"][0]["status"] == "uploaded"
    diff = client.get(f"/api/knowledge/databases/{kb_id}/mindmap/diff").json()
    assert diff["added_files"][0]["file_id"] == document


def test_mindmap_generation_uses_upstream_file_list_prompts_and_parser(tmp_path, monkeypatch):
    from app import db, knowledge_features
    from yuxi.knowledge.utils.mindmap_utils import (
        MINDMAP_SYSTEM_PROMPT, MINDMAP_INCREMENTAL_SYSTEM_PROMPT,
        build_mindmap_user_message, build_mindmap_incremental_user_message,
    )

    client = _client(tmp_path, monkeypatch)
    kb_id = client.post("/api/knowledge/databases", json={"database_name": "File organization", "kb_type": "local"}).json()["kb_id"]
    doc = db.create_document("unparsed.pdf", None, "application/pdf", "pymupdf-layout", "general", {}, {}, knowledge_base_id=kb_id)
    tree = {"content": "File organization", "children": [{"content": "unparsed.pdf", "children": []}]}
    calls = []
    async def unit_model(client, options, messages):
        calls.append(messages)
        return "```json\n" + json.dumps(tree) + "\n```"
    monkeypatch.setattr(knowledge_features, "_chat_completion", unit_model)
    generated = client.post(f"/api/knowledge/databases/{kb_id}/mindmap/generate", json={"file_ids": [doc], "user_prompt": "按用途分类"})
    assert generated.status_code == 200, generated.text
    assert calls == [[
        {"role": "system", "content": MINDMAP_SYSTEM_PROMPT},
        {"role": "user", "content": build_mindmap_user_message("File organization", [{"filename": "unparsed.pdf", "type": "pdf"}], "按用途分类")},
    ]]
    assert generated.json()["mindmap"] == tree
    assert generated.json()["file_count"] == 1 and generated.json()["original_file_count"] == 1
    assert generated.json()["truncated"] is False
    previous_tree = json.loads(json.dumps(tree))
    db.create_document("new.txt", None, "text/plain", "text", "general", {}, {}, knowledge_base_id=kb_id)
    tree["children"].append({"content": "new.txt", "children": []})
    updated = client.post(f"/api/knowledge/databases/{kb_id}/mindmap/generate", json={"incremental": True, "user_prompt": "保留分类"})
    assert updated.status_code == 200, updated.text
    assert calls[1] == [
        {"role": "system", "content": MINDMAP_INCREMENTAL_SYSTEM_PROMPT},
        {"role": "user", "content": build_mindmap_incremental_user_message("File organization", previous_tree, [{"filename": "new.txt", "type": "txt"}], "保留分类")},
    ]
    unchanged = client.post(f"/api/knowledge/databases/{kb_id}/mindmap/generate", json={"incremental": True})
    assert unchanged.status_code == 200 and unchanged.json()["no_ai_needed"] is True
    assert unchanged.json()["no_changes"] is True and len(calls) == 2


def test_mindmap_generation_applies_upstream_200_file_limit(tmp_path, monkeypatch):
    import asyncio
    from app import db, knowledge_features
    from yuxi.knowledge.utils.mindmap_utils import MINDMAP_GENERATION_FILE_LIMIT

    _client(tmp_path, monkeypatch)
    documents = [{"id": f"unit-{index}", "file_name": f"file-{index}.txt"} for index in range(MINDMAP_GENERATION_FILE_LIMIT + 1)]
    monkeypatch.setattr(db, "list_documents", lambda kb_id: documents)
    monkeypatch.setattr(knowledge_features, "_fingerprint", lambda ids: "unit-fingerprint")
    async def unit_model(client, options, messages):
        assert "file-199.txt" in messages[1]["content"]
        assert "file-200.txt" not in messages[1]["content"]
        return json.dumps({"content": "Limit", "children": [{"content": item["file_name"], "children": []} for item in documents[:MINDMAP_GENERATION_FILE_LIMIT]]})
    monkeypatch.setattr(knowledge_features, "_chat_completion", unit_model)
    result = asyncio.run(knowledge_features.generate_mindmap({"id": "unit-kb", "name": "Limit"}))
    assert result["file_count"] == 200 and len(result["source_document_ids"]) == 200
    assert result["original_file_count"] == 201 and result["truncated"] is True


@pytest.mark.parametrize("tracked_index", [0, 500])
def test_mindmap_diff_uses_upstream_500_file_window_and_keeps_tracked_files(tmp_path, monkeypatch, tracked_index):
    from app import db

    client = _client(tmp_path, monkeypatch)
    kb_id = client.post("/api/knowledge/databases", json={"database_name": "Mindmap paging", "kb_type": "local"}).json()["kb_id"]
    documents = [{"id": f"DOC-{index}", "file_name": f"file-{index}.txt", "status": "uploaded"} for index in range(501)]
    tracked = documents[tracked_index]
    db.save_knowledge_view(kb_id, "mindmap", "", {
        "mindmap": {"content": "KB", "children": [{"content": tracked["file_name"], "children": []}]},
        "source_document_ids": [tracked["id"]], "source_document_names": {tracked["id"]: tracked["file_name"]},
        "source_fingerprint": "unchanged",
    })
    monkeypatch.setattr(db, "list_documents", lambda _kb_id: documents)
    monkeypatch.setattr("app.main.indexed_content_fingerprint", lambda _ids: "unchanged")
    diff = client.get(f"/api/knowledge/databases/{kb_id}/mindmap/diff").json()
    assert diff["tracked_files"] == [tracked["id"]]
    assert diff["current_total"] == 501
    assert len(diff["current_files"]) == (501 if tracked_index == 500 else 500)
    assert diff["current_files_truncated"] is (tracked_index == 0)
    assert len(diff["added_files"]) == (499 if tracked_index == 0 else 500)


def test_local_knowledge_management_views_retrieval_and_evaluation(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Yuxi 功能迁移验收", "kb_type": "local"},
    ).json()
    kb_id = knowledge_base["id"]

    folder = client.post(f"/api/v1/knowledge-bases/{kb_id}/folders", json={"name": "规范"}).json()
    added = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "重庆地质灾害隐患复核.txt",
            "text": "重庆地质灾害隐患复核需要核对斜坡裂缝、降雨和位移监测记录。雨后应复查斜坡稳定性。",
            "knowledge_base_id": kb_id,
        },
    )
    assert added.status_code == 200
    doc_id = added.json()["document_id"]

    renamed = client.put(f"/api/v1/knowledge-bases/{kb_id}/folders/{folder['id']}/rename", json={"name": "规程"})
    assert renamed.status_code == 200 and renamed.json()["name"] == "规程"
    moved = client.put(
        f"/api/v1/knowledge-bases/{kb_id}/documents/{doc_id}/move", json={"folder_id": folder["id"]}
    )
    assert moved.status_code == 200
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/documents/search", params={"q": "复核"}).json()[0]["id"] == doc_id

    mindmap = client.post(f"/api/v1/knowledge-bases/{kb_id}/mindmap/generate", json={"document_ids": [doc_id]})
    assert mindmap.status_code == 503
    assert "LLM 未配置" in mindmap.json()["detail"]
    current_mindmap = client.get(f"/api/v1/knowledge-bases/{kb_id}/mindmap", params={"document_id": doc_id}).json()
    assert current_mindmap == {"status": "not_generated", "mindmap": None}
    diff = client.get(f"/api/v1/knowledge-bases/{kb_id}/mindmap/diff").json()
    assert diff["needs_update"] is True and diff["added_files"][0]["file_id"] == doc_id

    graph = client.post(f"/api/v1/knowledge-bases/{kb_id}/graph-build/index")
    assert graph.status_code == 409
    graph_status = client.get(f"/api/v1/knowledge-bases/{kb_id}/graph-build/status").json()
    assert graph_status["total_chunks"] > 0
    assert graph_status["pending_chunks"] == graph_status["total_chunks"]
    graph_list = client.get("/api/graph/list")
    assert graph_list.status_code == 200
    assert any(item["id"] == kb_id and item["type"] == "local" for item in graph_list.json()["data"])
    graph_stats = client.get("/api/graph/stats", params={"kb_id": kb_id})
    assert graph_stats.status_code == 200 and graph_stats.json()["data"]["total_nodes"] == 0
    graph_labels = client.get("/api/graph/labels", params={"kb_id": kb_id})
    assert graph_labels.status_code == 200 and graph_labels.json()["success"] is True
    graph_subgraph = client.get("/api/graph/subgraph", params={"kb_id": kb_id, "max_nodes": 10})
    assert graph_subgraph.status_code == 200 and graph_subgraph.json()["data"]["nodes"] == []
    graph_enabled = client.put(
        f"/api/v1/knowledge-bases/{kb_id}/query-params",
        json={"search_mode": "keyword", "use_graph_retrieval": True, "similarity_threshold": 0.0},
    )
    assert graph_enabled.status_code == 200
    debug = client.post(
        "/api/v1/debug/retrieve",
        json={"query": "斜坡裂缝隐患", "filters": {"knowledge_base_id": kb_id}},
    )
    assert debug.status_code == 200
    assert debug.json()["graph_candidates"] == []
    assert debug.json()["final_context"]

    chunk = client.get(f"/api/v1/knowledge-bases/{kb_id}/documents/{doc_id}/chunks").json()[0]
    dataset = {"name": "人工验收集", "cases": [{"query": "斜坡裂缝", "relevant_evidence_ids": [f"EV-{chunk['id']}"]}]}
    uploaded = client.post(
        f"/api/v1/knowledge-bases/{kb_id}/evaluation/datasets/upload",
        files={"file": ("bench.json", json.dumps(dataset, ensure_ascii=False).encode("utf-8"), "application/json")},
    )
    assert uploaded.status_code == 200
    dataset_id = uploaded.json()["id"]
    run = client.post(f"/api/v1/knowledge-bases/{kb_id}/evaluation/runs", json={"dataset_id": dataset_id})
    assert run.status_code == 200
    run_id = run.json()["id"]
    assert run.json()["result"]["case_count"] == 1
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/evaluation/runs/{run_id}").json()["id"] == run_id
    assert client.get(f"/api/v1/knowledge-bases/{kb_id}/evaluation/datasets/{dataset_id}").json()["total"] == 1


def test_notion_connector_reports_unavailable_until_data_source_is_set(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Notion connector", "kb_type": "notion", "config": {"notion": {"base_url": "https://api.notion.com/v1"}}},
    )
    assert created.status_code == 200
    tested = client.post(f"/api/v1/knowledge-bases/{created.json()['id']}/connection-test")
    assert tested.status_code == 503
    assert "notion_data_source_id" in tested.json()["detail"]


def test_graph_config_masks_api_key_and_requires_a_provider_before_indexing(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    knowledge_base = client.post(
        "/api/v1/knowledge-bases",
        json={"name": "Graph provider config", "kb_type": "local"},
    ).json()
    kb_id = knowledge_base["id"]
    configured = client.post(
        f"/api/v1/knowledge-bases/{kb_id}/graph-build/config",
        json={"extractor_type": "llm", "extractor_options": {"base_url": "https://models.example/v1", "model": "test-model", "api_key": "private-test-value"}},
    )
    assert configured.status_code == 200
    assert "private-test-value" not in configured.text
    assert configured.json()["config"]["extractor_options"]["api_key_set"] is True
