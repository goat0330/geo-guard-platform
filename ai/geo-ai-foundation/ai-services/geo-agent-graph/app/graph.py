from collections.abc import Mapping
from typing import Any

from langgraph.graph import END, START, StateGraph

from .llm import answer_with_evidence, draft_hazard_review
from .rag_client import retrieve
from .state import AgentState

HAZARD_REVIEW_NODES = [
    {"id": "normalize_input", "title": "规范化输入", "description": "整理复核问题和隐患字段"},
    {"id": "load_business_context", "title": "加载业务上下文", "description": "读取请求中由业务端提供的记录"},
    {"id": "retrieve_evidence", "title": "检索证据", "description": "从 RAG Studio 召回判定依据"},
    {"id": "match_candidate_hazard", "title": "匹配候选隐患", "description": "按显式隐患编号匹配业务记录"},
    {"id": "verify_fields", "title": "核验字段", "description": "比较字段并标注证据覆盖情况"},
    {"id": "retrieve_more", "title": "补充检索", "description": "对未覆盖字段进行一次扩展召回"},
    {"id": "classify_risk", "title": "分类风险", "description": "仅采用明确输入或模型结构化输出"},
    {"id": "generate_structured_result", "title": "生成结构化结果", "description": "生成待审核 JSON 和 Evidence IDs"},
    {"id": "human_review", "title": "人工复核", "description": "标记待人工确认，不写入正式台账"},
]

WORKFLOW_DEFINITIONS = {
    "qa": {
        "title": "地灾知识问答",
        "description": "检索知识依据后生成有来源的回答。",
        "nodes": [
            {"id": "normalize_input", "title": "规范化输入", "description": "清理问题文本"},
            {"id": "retrieve_evidence", "title": "检索证据", "description": "调用 RAG 召回依据"},
            {"id": "business_analysis", "title": "生成回答", "description": "结合证据组织业务回答"},
        ],
        "edges": [["normalize_input", "retrieve_evidence"], ["retrieve_evidence", "business_analysis"]],
    },
    "hazard_review": {
        "title": "隐患复核",
        "description": "比对业务记录、核验证据，输出待人工确认的结构化结果。",
        "nodes": HAZARD_REVIEW_NODES,
        "edges": [
            ["normalize_input", "load_business_context"], ["load_business_context", "retrieve_evidence"],
            ["retrieve_evidence", "match_candidate_hazard"], ["match_candidate_hazard", "verify_fields"],
            ["verify_fields", "need_more_evidence?"], ["need_more_evidence?", "retrieve_more", "yes"],
            ["need_more_evidence?", "classify_risk", "no"], ["retrieve_more", "verify_fields"],
            ["classify_risk", "generate_structured_result"], ["generate_structured_result", "human_review"],
        ],
    },
}


def add_trace(state: AgentState, stage: str, status: str, summary: str, data: dict | None = None) -> list[dict]:
    trace = list(state.get("trace", []))
    trace.append({"stage": stage, "status": status, "summary": summary, "data": data or {}})
    return trace


def flatten_record(value: Any, prefix: str = "") -> dict[str, Any]:
    if not isinstance(value, Mapping):
        return {prefix: value} if prefix else {}
    flattened = {}
    for key, item in value.items():
        field = f"{prefix}.{key}" if prefix else str(key)
        if isinstance(item, Mapping) and item:
            flattened.update(flatten_record(item, field))
        elif isinstance(item, Mapping):
            flattened[field] = {}
        else:
            flattened[field] = item
    return flattened


def _record_pair(state: AgentState) -> tuple[dict, dict]:
    context = state.get("business_context", {})
    hazard = state.get("hazard_input", {})
    current = context.get("current_record") or hazard.get("current_record") or {}
    historical = context.get("historical_record") or hazard.get("historical_record") or {}
    return (current if isinstance(current, dict) else {}, historical if isinstance(historical, dict) else {})


async def normalize_input(state: AgentState) -> AgentState:
    query = " ".join(state.get("query", "").split())
    return {**state, "query": query,
            "trace": add_trace(state, "normalize_input", "completed", "已规范化复核问题和输入字段")}


async def load_business_context(state: AgentState) -> AgentState:
    supplied = state.get("context", {})
    context = supplied if isinstance(supplied, dict) else {}
    return {
        **state,
        "business_context": context,
        "trace": add_trace(
            state, "load_business_context", "completed" if context else "skipped",
            f"已载入业务记录，来源：{context.get('business_source', 'request_context')}" if context else "请求未携带业务台账记录，未读取外部数据库",
            {"current_record_present": isinstance(context.get("current_record"), dict),
             "historical_record_present": isinstance(context.get("historical_record"), dict)},
        ),
    }


def _retrieval_query(state: AgentState, fields: list[str] | None = None) -> str:
    hazard = state.get("hazard_input", {})
    values = [state.get("query", ""), str(hazard.get("hazard_id", "")),
              str(hazard.get("hazard_name", "")), str(hazard.get("hazard_type", ""))]
    if fields:
        values.extend(fields)
    return " ".join(value for value in values if value).strip()


async def retrieve_evidence(state: AgentState) -> AgentState:
    response = await retrieve(_retrieval_query(state))
    evidence = response.get("evidences", [])
    return {**state, "evidence": evidence,
            "trace": add_trace(state, "retrieve_evidence", "completed", f"RAG 返回 {len(evidence)} 条证据", {
                "mode": response.get("retrieval", {}).get("mode"),
                "evidence_ids": [item.get("evidence_id") for item in evidence],
            })}


async def match_candidate_hazard(state: AgentState) -> AgentState:
    hazard = state.get("hazard_input", {})
    candidates = state.get("business_context", {}).get("candidate_hazards", [])
    hazard_id = str(hazard.get("hazard_id") or "").strip()
    matches = [item for item in candidates if isinstance(item, dict)
               and str(item.get("hazard_id") or item.get("id") or "") == hazard_id] if hazard_id else []
    if len(matches) == 1:
        match = {"status": "matched", "hazard_id": hazard_id, "candidate": matches[0]}
    elif len(matches) > 1:
        match = {"status": "multiple_candidates", "hazard_id": hazard_id, "candidate_count": len(matches)}
    elif hazard_id and not candidates:
        match = {"status": "unverified", "hazard_id": hazard_id, "reason": "business_context_has_no_candidate_list"}
    elif hazard_id:
        match = {"status": "not_found", "hazard_id": hazard_id}
    else:
        match = {"status": "insufficient_input", "reason": "hazard_id_not_supplied"}
    return {**state, "candidate_match": match,
            "trace": add_trace(state, "match_candidate_hazard", "completed", f"候选隐患匹配状态：{match['status']}", {
                "hazard_id": hazard_id or None})}


def _verify(state: AgentState) -> tuple[list[dict], list[str], list[dict]]:
    current, historical = _record_pair(state)
    current_fields, historical_fields = flatten_record(current), flatten_record(historical)
    differences = [
        {"field": key, "current": current_fields.get(key), "historical": historical_fields.get(key),
         "current_present": key in current_fields, "historical_present": key in historical_fields}
        for key in sorted(current_fields.keys() | historical_fields.keys())
        if current_fields.get(key) != historical_fields.get(key)
    ]
    hazard = state.get("hazard_input", {})
    supplied = hazard.get("fields") if isinstance(hazard.get("fields"), dict) else current
    supplied_fields = flatten_record(supplied)
    required = hazard.get("required_fields", [])
    if not isinstance(required, list):
        required = []
    required = [str(field) for field in required] or sorted(supplied_fields)
    evidence = state.get("evidence", [])
    verification, missing = [], []
    for field in required:
        value = supplied_fields.get(field)
        matches = [item.get("evidence_id") for item in evidence if value is not None
                   and str(value).casefold() in str(item.get("text", "")).casefold()]
        status = "missing_input" if value is None or value == "" else ("evidence_text_match" if matches else "not_verified_by_evidence")
        if status != "evidence_text_match":
            missing.append(field)
        verification.append({"field": field, "value": value, "status": status, "evidence_ids": matches})
    return differences, missing, verification


async def verify_fields(state: AgentState) -> AgentState:
    differences, missing, verification = _verify(state)
    needs_more = bool(missing) and not state.get("more_evidence_attempted", False)
    return {
        **state,
        "comparison": differences,
        "missing_fields": missing,
        "field_verification": verification,
        "needs_more_evidence": needs_more,
        "trace": add_trace(state, "verify_fields", "completed", f"字段核验 {len(verification)} 项，待补证 {len(missing)} 项", {
            "difference_count": len(differences), "missing_fields": missing,
        }),
    }


def route_more_evidence(state: AgentState) -> str:
    return "yes" if state.get("needs_more_evidence") else "no"


async def retrieve_more(state: AgentState) -> AgentState:
    response = await retrieve(_retrieval_query(state, state.get("missing_fields", [])), top_k=15)
    existing = {item.get("evidence_id") for item in state.get("evidence", [])}
    evidence = list(state.get("evidence", []))
    evidence.extend(item for item in response.get("evidences", []) if item.get("evidence_id") not in existing)
    return {**state, "evidence": evidence, "more_evidence_attempted": True,
            "trace": add_trace(state, "retrieve_more", "completed", f"扩展召回后共 {len(evidence)} 条证据", {
                "new_evidence_count": len(evidence) - len(existing), "missing_fields": state.get("missing_fields", []),
            })}


async def classify_risk(state: AgentState) -> AgentState:
    current, _ = _record_pair(state)
    hazard = state.get("hazard_input", {})
    value = hazard.get("risk_level") or current.get("risk_level") or current.get("riskLevel") or current.get("riskGrade")
    allowed = {"low", "medium", "high", "unknown", "低", "中", "较高", "高", "一般", "中等"}
    classification = {"level": value if isinstance(value, str) and value in allowed else "unknown",
                      "source": "business_input" if isinstance(value, str) and value in allowed else "not_provided",
                      "evidence_ids": []}
    return {**state, "risk_classification": classification,
            "trace": add_trace(state, "classify_risk", "completed",
                                f"风险等级：{classification['level']}（{classification['source']}）")}


async def generate_structured_result(state: AgentState) -> AgentState:
    current, historical = _record_pair(state)
    result = await draft_hazard_review(
        state.get("query", ""), state.get("hazard_input", {}), current, historical,
        state.get("comparison", []), state.get("field_verification", []),
        state.get("candidate_match", {}), state.get("risk_classification", {}), state.get("evidence", []),
    )
    return {**state, "result": result,
            "trace": add_trace(state, "generate_structured_result", "completed", "已生成结构化复核 JSON", {
                "llm_status": result.get("llm_status"), "evidence_ids": result.get("evidence_ids", []),
            })}


async def human_review(state: AgentState) -> AgentState:
    result = dict(state.get("result", {}))
    result.update({"review_status": "pending_human_confirmation", "needs_human_review": True,
                   "formal_record_updated": False,
                   "evidence_ids": [item.get("evidence_id") for item in state.get("evidence", [])]})
    return {**state, "result": result,
            "trace": add_trace(state, "human_review", "completed", "结果待人工确认，未写入正式台账")}


async def business_analysis(state: AgentState) -> AgentState:
    result = await answer_with_evidence(state["query"], state.get("evidence", []))
    return {**state, "result": result,
            "trace": add_trace(state, "business_analysis", "completed", "已完成基于 Evidence 的知识问答", {
                "evidence_ids": result.get("evidence_ids", []),
            })}


def build_graph(workflow: str):
    graph = StateGraph(AgentState)
    if workflow == "hazard_review":
        for name, node in [
            ("normalize_input", normalize_input), ("load_business_context", load_business_context),
            ("retrieve_evidence", retrieve_evidence), ("match_candidate_hazard", match_candidate_hazard),
            ("verify_fields", verify_fields), ("retrieve_more", retrieve_more),
            ("classify_risk", classify_risk), ("generate_structured_result", generate_structured_result),
            ("human_review", human_review),
        ]:
            graph.add_node(name, node)
        graph.add_edge(START, "normalize_input")
        graph.add_edge("normalize_input", "load_business_context")
        graph.add_edge("load_business_context", "retrieve_evidence")
        graph.add_edge("retrieve_evidence", "match_candidate_hazard")
        graph.add_edge("match_candidate_hazard", "verify_fields")
        graph.add_conditional_edges("verify_fields", route_more_evidence, {"yes": "retrieve_more", "no": "classify_risk"})
        graph.add_edge("retrieve_more", "verify_fields")
        graph.add_edge("classify_risk", "generate_structured_result")
        graph.add_edge("generate_structured_result", "human_review")
        graph.add_edge("human_review", END)
    else:
        graph.add_node("normalize_input", normalize_input)
        graph.add_node("retrieve_evidence", retrieve_evidence)
        graph.add_node("business_analysis", business_analysis)
        graph.add_edge(START, "normalize_input")
        graph.add_edge("normalize_input", "retrieve_evidence")
        graph.add_edge("retrieve_evidence", "business_analysis")
        graph.add_edge("business_analysis", END)
    return graph.compile()


COMPILED_GRAPHS = {name: build_graph(name) for name in WORKFLOW_DEFINITIONS}
hazard_review_graph = COMPILED_GRAPHS["hazard_review"]
qa_graph = COMPILED_GRAPHS["qa"]
