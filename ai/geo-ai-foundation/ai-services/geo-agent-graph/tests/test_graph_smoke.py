import asyncio

from app.graph import COMPILED_GRAPHS, verify_fields


def test_hazard_review_graph_contains_business_nodes():
    graph_nodes = COMPILED_GRAPHS["hazard_review"].get_graph().nodes
    assert {"normalize_input", "load_business_context", "retrieve_evidence", "match_candidate_hazard",
            "verify_fields", "retrieve_more", "classify_risk", "generate_structured_result", "human_review"} <= set(graph_nodes)


def test_field_verification_requests_only_one_additional_retrieval():
    state = {
        "query": "复核隐患",
        "hazard_input": {"fields": {"threatened_population": 10}, "required_fields": ["threatened_population"]},
        "business_context": {},
        "evidence": [],
        "trace": [],
        "more_evidence_attempted": False,
    }
    first = asyncio.run(verify_fields(state))
    assert first["needs_more_evidence"] is True
    second = asyncio.run(verify_fields({**state, **first, "more_evidence_attempted": True}))
    assert second["needs_more_evidence"] is False
