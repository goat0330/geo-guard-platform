import json
import logging

from fastapi import FastAPI
from fastapi.responses import StreamingResponse

from .graph import COMPILED_GRAPHS, WORKFLOW_DEFINITIONS
from .llm import llm_enabled
from .schemas import RunRequest, RunResponse

logger = logging.getLogger(__name__)
app = FastAPI(
    title="Geo Agent Graph API",
    version="0.2.0",
    description="LangGraph orchestration layer for geological-disaster business agents.",
)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "geo-agent-graph", "llm_configured": llm_enabled()}


@app.get("/api/v1/workflows")
async def workflows():
    return {"workflows": [{"id": name, **definition} for name, definition in WORKFLOW_DEFINITIONS.items()]}


def initial_state(req: RunRequest):
    return {
        "query": req.query,
        "workflow": req.workflow,
        "hazard_input": req.hazard_input,
        "context": req.context,
        "business_context": {},
        "candidate_match": {},
        "comparison": [],
        "field_verification": [],
        "missing_fields": [],
        "evidence": [],
        "more_evidence_attempted": False,
        "needs_more_evidence": False,
        "risk_classification": {},
        "result": {},
        "trace": [],
    }


def response_from(final: dict, workflow: str) -> RunResponse:
    return RunResponse(
        workflow=workflow,
        structured_result=final.get("result", {}),
        evidence=final.get("evidence", []),
        trace=final.get("trace", []),
    )


@app.post("/api/v1/run", response_model=RunResponse)
async def run(req: RunRequest):
    final = await COMPILED_GRAPHS[req.workflow].ainvoke(initial_state(req))
    return response_from(final, req.workflow)


@app.post("/api/v1/run/stream")
async def run_stream(req: RunRequest):
    async def events():
        final = {}
        node_titles = {node["id"]: node["title"] for node in WORKFLOW_DEFINITIONS[req.workflow]["nodes"]}
        try:
            async for event in COMPILED_GRAPHS[req.workflow].astream_events(initial_state(req), version="v2"):
                node_id = event.get("metadata", {}).get("langgraph_node")
                if node_id not in node_titles or event.get("name") != node_id:
                    continue
                if event.get("event") == "on_chain_start":
                    payload = {"stage": node_id, "node_id": node_id, "status": "running",
                               "summary": f"正在执行：{node_titles[node_id]}", "data": {}}
                    yield f"event: trace\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"
                elif event.get("event") == "on_chain_end":
                    node_update = event.get("data", {}).get("output")
                    if not isinstance(node_update, dict):
                        continue
                    final.update(node_update)
                    traces = node_update.get("trace", [])
                    trace = traces[-1] if traces else {
                        "stage": node_id, "status": "completed", "summary": f"节点 {node_id} 完成", "data": {}
                    }
                    payload = {**trace, "node_id": node_id}
                    yield f"event: trace\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"
            result = response_from(final, req.workflow).model_dump()
            yield f"event: result\ndata: {json.dumps(result, ensure_ascii=False)}\n\n"
            yield "event: done\ndata: {}\n\n"
        except Exception as exc:
            logger.exception("LangGraph streaming execution failed")
            payload = {"error": "graph_execution_failed", "detail": type(exc).__name__}
            yield f"event: error\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        events(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
