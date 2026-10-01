import httpx
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from yuxi.knowledge.implementations.dify import DifyKB
from yuxi.knowledge.implementations.notion import NotionAPIError, NotionKB


def _client(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "rag_db_path", str(tmp_path / "rag.sqlite3"))
    monkeypatch.setattr(settings, "rag_upload_dir", str(tmp_path / "uploads"))
    return TestClient(app)


def _create_dify_kb(client):
    response = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "Yuxi Dify migration",
            "kb_type": "dify",
            "config": {
                "dify": {
                    "dify_api_url": "https://dify.example/v1",
                    "dify_token": "test-secret",
                    "dify_dataset_id": "dataset-123",
                },
                "retrieval": {"search_mode": "hybrid", "final_top_k": 4},
            },
        },
    )
    assert response.status_code == 200
    return response.json()["id"]


def test_dify_retrieve_calls_vendored_yuxi_connector(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb_id = _create_dify_kb(client)
    requests = []

    async def fake_request(self, client_payload, request_url, headers):
        requests.append((client_payload, request_url, headers))
        return {
            "records": [
                {
                    "score": 0.91,
                    "segment": {
                        "id": "chunk-7",
                        "position": 7,
                        "content": "坡体裂缝扩展，需要核查降雨和位移记录。",
                        "document": {"id": "file-4", "name": "隐患复核指南.pdf"},
                    },
                }
            ]
        }

    monkeypatch.setattr(DifyKB, "_request_dify", fake_request)
    response = client.post(
        "/api/v1/retrieve",
        json={"query": "坡体裂缝", "final_top_k": 3, "filters": {"knowledge_base_id": kb_id}},
    )

    assert response.status_code == 200, response.text
    assert requests[0][0]["retrieval_model"]["search_method"] == "hybrid_search"
    assert requests[0][0]["retrieval_model"]["top_k"] == 3
    assert requests[0][1] == "https://dify.example/v1/datasets/dataset-123/retrieve"
    assert requests[0][2]["Authorization"] == "Bearer test-secret"
    evidence = response.json()["evidences"][0]
    assert evidence["document_id"] == "file-4"
    assert evidence["chunk_id"] == "chunk-7"
    assert evidence["file_name"] == "隐患复核指南.pdf"
    assert evidence["fusion_score"] == 0.91
    assert "test-secret" not in response.text


def test_dify_connection_error_is_not_reported_as_empty_success(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    kb_id = _create_dify_kb(client)
    attempts = 0

    async def failed_request(self, client_payload, request_url, headers):
        nonlocal attempts
        attempts += 1
        raise httpx.ConnectError("connection refused")

    monkeypatch.setattr(DifyKB, "_request_dify", failed_request)
    response = client.post(
        "/api/v1/knowledge-bases/" + kb_id + "/connection-test",
    )

    assert attempts == 2
    assert response.status_code == 502
    assert response.json()["detail"] == "外部解析或模型服务请求失败：ConnectError"


def test_yuxi_frontend_dify_flow_uses_upstream_connector_and_masks_token(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/knowledge/databases",
        json={
            "database_name": "Dify from Yuxi UI",
            "kb_type": "dify",
            "additional_params": {
                "dify_api_url": "https://dify.example/v1",
                "dify_token": "ui-flow-secret",
                "dify_dataset_id": "dataset-ui",
            },
        },
    )
    assert created.status_code == 200, created.text
    kb_id = created.json()["id"]
    details = client.get(f"/api/knowledge/databases/{kb_id}")
    assert details.status_code == 200
    assert "ui-flow-secret" not in details.text
    assert details.json()["additional_params"]["dify_token_set"] is True

    requests = []

    async def fake_request(self, client_payload, request_url, headers):
        requests.append(client_payload)
        return {
            "records": [
                {
                    "score": 0.84,
                    "segment": {
                        "id": "segment-ui",
                        "content": "滑坡隐患复核应综合核对裂缝和监测数据。",
                        "document": {"id": "document-ui", "name": "地灾复核指南.pdf"},
                    },
                }
            ]
        }

    monkeypatch.setattr(DifyKB, "_request_dify", fake_request)
    query = client.post(
        f"/api/knowledge/databases/{kb_id}/query-test",
        json={"query": "滑坡隐患", "meta": {"search_mode": "keyword", "final_top_k": 2}},
    )

    assert query.status_code == 200, query.text
    assert requests[0]["retrieval_model"]["search_method"] == "keyword_search"
    assert query.json()[0]["metadata"]["chunk_id"] == "segment-ui"
    assert query.json()[0]["content"] == "滑坡隐患复核应综合核对裂缝和监测数据。"


def test_notion_retrieve_uses_vendored_yuxi_connector(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "Yuxi Notion migration",
            "kb_type": "notion",
            "config": {
                "notion": {
                    "token": "notion-test-secret",
                    "data_source_id": "source-123",
                    "version": "2026-03-11",
                }
            },
        },
    )
    assert created.status_code == 200
    kb_id = created.json()["id"]
    captured = {}

    async def fake_aquery(self, query_text, requested_kb_id, *, config, **kwargs):
        captured["query"] = query_text
        captured["kb_id"] = requested_kb_id
        captured["config"] = config
        return [
            {
                "content": "斜坡裂缝变化应结合监测记录复核。",
                "score": 8.5,
                "metadata": {
                    "source": "隐患复核记录",
                    "file_id": "page-9",
                    "chunk_id": "page-9:3",
                    "notion_url": "https://notion.so/page-9",
                },
            }
        ]

    monkeypatch.setattr(NotionKB, "aquery", fake_aquery)
    response = client.post(
        "/api/v1/retrieve",
        json={"query": "斜坡裂缝", "filters": {"knowledge_base_id": kb_id}},
    )

    assert response.status_code == 200, response.text
    assert captured["query"] == "斜坡裂缝"
    assert captured["kb_id"] == kb_id
    assert captured["config"].additional_params["notion_data_source_id"] == "source-123"
    evidence = response.json()["evidences"][0]
    assert evidence["document_id"] == "page-9"
    assert evidence["chunk_id"] == "page-9:3"
    assert evidence["fusion_score"] == 8.5
    assert "notion-test-secret" not in response.text


def test_notion_api_error_is_not_reported_as_empty_success(tmp_path, monkeypatch):
    client = _client(tmp_path, monkeypatch)
    created = client.post(
        "/api/v1/knowledge-bases",
        json={
            "name": "Notion error propagation",
            "kb_type": "notion",
            "config": {
                "notion": {"token": "notion-test-secret", "data_source_id": "source-123"}
            },
        },
    )
    kb_id = created.json()["id"]

    async def fail_search(*args, **kwargs):
        raise NotionAPIError("test API unavailable")

    monkeypatch.setattr(NotionKB, "_search_candidate_pages", fail_search)
    response = client.post(f"/api/v1/knowledge-bases/{kb_id}/connection-test")

    assert response.status_code == 502
    assert response.json()["detail"] == "Notion API 请求失败"
    assert "notion-test-secret" not in response.text
