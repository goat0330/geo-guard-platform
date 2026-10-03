import json
import time

from fastapi.testclient import TestClient

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
    return TestClient(app)


def _indexed_document(client, kb_id):
    response = client.post(
        "/api/v1/documents/text",
        json={
            "file_name": "重庆隐患复核.txt",
            "text": "重庆山区连续降雨后，应复核坡体裂缝、位移监测和斜坡稳定状态。",
            "knowledge_base_id": kb_id,
        },
    )
    assert response.status_code == 200, response.text
    document_id = response.json()["document_id"]
    chunks = client.get(f"/api/v1/knowledge-bases/{kb_id}/documents/{document_id}/chunks")
    assert chunks.status_code == 200, chunks.text
    return document_id, chunks.json()[0]


def _wait_for_task(client, task_id):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        response = client.get(f"/api/tasks/{task_id}")
        assert response.status_code == 200, response.text
        task = response.json()["task"]
        if task["status"] in {"success", "failed", "cancelled"}:
            return task
        time.sleep(0.02)
    raise AssertionError(f"Task {task_id} did not finish")


def test_yuxi_evaluation_dataset_uses_jsonl_and_success_envelopes(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "评测知识库", "kb_type": "local"}).json()
    document_id, chunk = _indexed_document(client, kb["id"])
    payload = {"query": "连续降雨后复核什么？", "gold_chunk_ids": [chunk["id"]], "gold_answer": "复核坡体裂缝、位移和斜坡稳定状态。"}

    uploaded = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/upload",
        data={"name": "雨后复核评测", "description": "本地 JSONL 验收"},
        files={"file": ("雨后复核.jsonl", json.dumps(payload, ensure_ascii=False), "application/x-ndjson")},
    )
    assert uploaded.status_code == 200, uploaded.text
    assert uploaded.json()["message"] == "success"
    dataset = uploaded.json()["data"]
    assert dataset["dataset_id"].startswith("dataset_")
    assert dataset["item_count"] == 1
    assert dataset["has_gold_chunks"] and dataset["has_gold_answers"]
    assert dataset["build_metadata"]["filename"] == "雨后复核.jsonl"

    rejected = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/upload",
        data={"name": "错误格式"},
        files={"file": ("evaluation.json", "{}", "application/json")},
    )
    assert rejected.status_code == 400

    listed = client.get(f"/api/evaluation/databases/{kb['id']}/datasets")
    assert listed.json()["message"] == "success"
    assert listed.json()["data"][0]["dataset_id"] == dataset["dataset_id"]

    detail = client.get(f"/api/evaluation/databases/{kb['id']}/datasets/{dataset['dataset_id']}", params={"page": 1, "page_size": 1})
    assert detail.json()["message"] == "success"
    assert detail.json()["data"]["items"][0]["gold_chunk_ids"] == [chunk["id"]]
    assert detail.json()["data"]["pagination"]["total_items"] == 1

    download = client.get(f"/api/evaluation/datasets/{dataset['dataset_id']}/download")
    assert download.status_code == 200
    assert download.headers["content-type"].startswith("application/x-ndjson")
    assert "filename*=UTF-8''" in download.headers["content-disposition"]
    assert json.loads(download.text) == payload

    deleted = client.delete(f"/api/evaluation/datasets/{dataset['dataset_id']}")
    assert deleted.json() == {"message": "success", "data": None}
    assert client.get(f"/api/evaluation/databases/{kb['id']}/datasets").json()["data"] == []
    assert document_id


def test_yuxi_evaluation_run_applies_selected_models_and_returns_paginated_results(tmp_path, monkeypatch):
    from app import main

    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "问答评测知识库", "kb_type": "local"}).json()
    _, chunk = _indexed_document(client, kb["id"])
    dataset_payload = {
        "query": "连续降雨后要复核哪些内容？",
        "gold_chunk_ids": [chunk["id"]],
        "gold_answer": "复核坡体裂缝、位移监测和斜坡稳定状态。",
    }
    uploaded = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/upload",
        data={"name": "问答基准"},
        files={"file": ("qa.jsonl", json.dumps(dataset_payload, ensure_ascii=False), "application/x-ndjson")},
    ).json()["data"]
    calls = []

    async def fake_complete_chat(messages, model_spec=None, **_kwargs):
        prompt = messages[0]["content"]
        calls.append((model_spec, prompt))
        if "公正的评判者" in prompt:
            return '{"score":1.0,"reasoning":"事实一致"}'
        return "雨后复核坡体裂缝、位移监测和斜坡稳定状态。"

    monkeypatch.setattr(main, "complete_chat", fake_complete_chat)
    started = client.post(
        f"/api/evaluation/databases/{kb['id']}/runs",
        json={
            "dataset_id": uploaded["dataset_id"],
            "name": "雨后复核模型评测",
            "model_config": {
                "search_mode": "keyword",
                "answer_llm": "local:answer-model",
                "judge_llm": "local:judge-model",
            },
        },
    )
    assert started.status_code == 200, started.text
    assert started.json()["message"] == "success"
    run_id = started.json()["data"]["run_id"]
    assert run_id.startswith("run_")
    assert len(calls) == 2
    assert [call[0] for call in calls] == ["local:answer-model", "local:judge-model"]

    listed = client.get(f"/api/evaluation/databases/{kb['id']}/runs")
    assert listed.json()["message"] == "success"
    run = listed.json()["data"][0]
    assert run["run_id"] == run_id
    assert run["name"] == "雨后复核模型评测"
    assert run["status"] == "completed"
    assert run["overall_score"] == 1.0

    details = client.get(
        f"/api/evaluation/databases/{kb['id']}/runs/{run_id}",
        params={"page": 1, "page_size": 1},
    )
    assert details.status_code == 200, details.text
    data = details.json()["data"]
    assert data["items"][0]["generated_answer"].startswith("雨后复核")
    assert data["items"][0]["metrics"]["score"] == 1.0
    assert data["items"][0]["retrieved_chunks"][0]["metadata"]["chunk_id"] == chunk["id"]
    assert data["pagination"]["total"] == 1

    errors = client.get(
        f"/api/evaluation/databases/{kb['id']}/runs/{run_id}",
        params={"result_filter": "answer_errors"},
    )
    assert errors.json()["data"]["pagination"]["total"] == 0
    assert client.delete(f"/api/evaluation/databases/{kb['id']}/runs/{run_id}").json() == {
        "message": "success", "data": None
    }


def test_yuxi_dataset_generation_persists_progress_and_resume_is_idempotent(tmp_path, monkeypatch):
    import asyncio

    from app import task_runtime

    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "生成评测知识库", "kb_type": "local"}).json()
    _, chunk = _indexed_document(client, kb["id"])
    generated_count = 0
    generation_params = []

    async def fake_generate(**kwargs):
        nonlocal generated_count
        await asyncio.sleep(0.02)
        generated_count += 1
        generation_params.append(kwargs)
        return {
            "query": f"自动评测问题 {generated_count}",
            "gold_chunk_ids": [chunk["id"]],
            "gold_answer": "雨后复核坡体裂缝与位移。",
        }

    monkeypatch.setattr(task_runtime, "generate_benchmark_case", fake_generate)
    submitted = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/generate",
        json={
            "name": "自动评测集",
            "count": 2,
            "concurrency_count": 2,
            "llm_model_spec": "local:test-model",
            "generation_mode": "graph_enhanced",
            "graph_expand_top_k": 2,
        },
    )
    assert submitted.status_code == 200, submitted.text
    submission = submitted.json()["data"]
    task = _wait_for_task(client, submission["task_id"])
    assert task["status"] == "success"
    assert task["result"]["item_count"] == 2
    assert generated_count == 2
    assert all(item["generation_mode"] == "graph_enhanced" for item in generation_params)
    assert all(item["graph_expand_top_k"] == 2 for item in generation_params)

    datasets = client.get(f"/api/evaluation/databases/{kb['id']}/datasets").json()["data"]
    dataset = next(item for item in datasets if item["dataset_id"] == submission["dataset_id"])
    assert dataset["item_count"] == 2
    assert dataset["build_metadata"]["status"] == "completed"
    assert dataset["build_metadata"]["progress"] == 100

    resumed = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/{submission['dataset_id']}/resume",
        json={},
    )
    assert resumed.status_code == 200, resumed.text
    assert resumed.json()["data"]["message"] == "数据集已完成生成"
    assert "task_id" not in resumed.json()["data"]


def test_yuxi_graph_enhanced_generation_expands_real_local_graph_with_ppr(tmp_path, monkeypatch):
    import asyncio

    from app import db, evaluation_generation, knowledge_features

    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "图增强评测知识库", "kb_type": "local"}).json()
    document_id, _ = _indexed_document(client, kb["id"])
    indexed_chunks = db.get_chunks(document_id, include_embedding=False)
    anchor_id = indexed_chunks[0]["id"]
    neighbor_id = "CHUNK-GRAPH-NEIGHBOR"
    db.replace_chunks(document_id, [
        {
            "id": anchor_id,
            "text": "锚点片段记录了连续降雨后的坡体裂缝变化。",
            "token_count": 16,
            "source_spans": [],
            "metadata": {},
        },
        {
            "id": neighbor_id,
            "text": "关联片段说明应同步核验位移监测和斜坡稳定状态。",
            "token_count": 18,
            "source_spans": [],
            "metadata": {},
        },
    ])
    chunks = [
        {
            "id": chunk["id"],
            "content": chunk["text"],
            "file_id": chunk["document_id"],
            "chunk_index": chunk["chunk_index"],
        }
        for chunk in db.get_chunks(document_id, include_embedding=False)
    ]
    anchor_node_id = f"CHUNK-{anchor_id}"
    neighbor_node_id = f"CHUNK-{neighbor_id}"
    document_ids = [document_id]
    db.save_knowledge_view(kb["id"], "graph", "", {
        "source_document_ids": document_ids,
        "source_fingerprint": knowledge_features.indexed_content_fingerprint(document_ids),
        "nodes": [
            {"id": anchor_node_id, "type": "Chunk", "chunk_id": anchor_id},
            {"id": "ENT-HAZARD", "type": "Hazard"},
            {"id": neighbor_node_id, "type": "Chunk", "chunk_id": neighbor_id},
            {"id": "ENT-LOCATION", "type": "Location"},
        ],
        "edges": [
            {"source": anchor_node_id, "target": "ENT-HAZARD", "type": "MENTIONS", "chunk_id": anchor_id},
            {"source": "ENT-HAZARD", "target": "ENT-LOCATION", "type": "RELATED_TO"},
            {"source": neighbor_node_id, "target": "ENT-LOCATION", "type": "MENTIONS", "chunk_id": neighbor_id},
        ],
    })

    monkeypatch.setattr(evaluation_generation.random, "choice", lambda values: values[0])
    prompts = []

    async def fake_complete_chat(messages, _model_spec):
        prompts.append(messages[0]["content"])
        return json.dumps({
            "query": "雨后还要核验哪些指标？",
            "gold_answer": "同步核验位移监测和斜坡稳定状态。",
            "gold_chunk_ids": [anchor_id, neighbor_id],
        }, ensure_ascii=False)

    monkeypatch.setattr(evaluation_generation, "complete_chat", fake_complete_chat)
    generated = asyncio.run(evaluation_generation.generate_benchmark_case(
        knowledge_base_id=kb["id"],
        chunks=chunks,
        model_spec="local:test-model",
        neighbors_count=2,
        generation_mode="graph_enhanced",
        graph_expand_top_k=1,
    ))

    assert generated["gold_chunk_ids"] == [anchor_id, neighbor_id]
    assert len(prompts) == 1
    assert "锚点片段记录了连续降雨后的坡体裂缝变化。" in prompts[0]
    assert "关联片段说明应同步核验位移监测和斜坡稳定状态。" in prompts[0]


def test_yuxi_graph_enhanced_generation_rejects_missing_graph_without_vector_fallback(tmp_path, monkeypatch):
    import asyncio

    import pytest

    from app import evaluation_generation

    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "缺图谱评测知识库", "kb_type": "local"}).json()
    _, chunk = _indexed_document(client, kb["id"])

    with pytest.raises(ValueError, match="先构建知识图谱"):
        asyncio.run(evaluation_generation.generate_benchmark_case(
            knowledge_base_id=kb["id"],
            chunks=[{
                "id": chunk["id"],
                "content": chunk["text"],
                "file_id": chunk["document_id"],
                "chunk_index": chunk["chunk_index"],
            }],
            model_spec="local:test-model",
            neighbors_count=1,
            generation_mode="graph_enhanced",
        ))


def test_yuxi_dataset_generation_reports_unconfigured_chat_provider(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb = client.post("/api/v1/knowledge-bases", json={"name": "无模型评测知识库", "kb_type": "local"}).json()
    _indexed_document(client, kb["id"])
    submitted = client.post(
        f"/api/evaluation/databases/{kb['id']}/datasets/generate",
        json={"count": 1, "concurrency_count": 1, "llm_model_spec": "missing:model"},
    )
    assert submitted.status_code == 200, submitted.text
    submission = submitted.json()["data"]
    task = _wait_for_task(client, submission["task_id"])
    assert task["status"] == "failed"
    assert "配置并启用" in task["error"]
    datasets = client.get(f"/api/evaluation/databases/{kb['id']}/datasets").json()["data"]
    dataset = next(item for item in datasets if item["dataset_id"] == submission["dataset_id"])
    assert dataset["build_metadata"]["status"] == "failed"
    assert dataset["build_metadata"]["error_message"] == task["error"]
