from typing import Any, TypedDict


class AgentState(TypedDict, total=False):
    query: str
    workflow: str
    hazard_input: dict[str, Any]
    context: dict[str, Any]
    business_context: dict[str, Any]
    candidate_match: dict[str, Any]
    comparison: list[dict[str, Any]]
    field_verification: list[dict[str, Any]]
    missing_fields: list[str]
    evidence: list[dict[str, Any]]
    more_evidence_attempted: bool
    needs_more_evidence: bool
    risk_classification: dict[str, Any]
    result: dict[str, Any]
    trace: list[dict[str, Any]]
