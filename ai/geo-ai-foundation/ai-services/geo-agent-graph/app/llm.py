import json

from langchain_openai import ChatOpenAI

from .config import settings


def llm_enabled() -> bool:
    return bool(settings.llm_base_url and settings.llm_api_key and settings.llm_model)


def build_llm() -> ChatOpenAI:
    if not llm_enabled():
        raise RuntimeError("LLM is not configured")
    return ChatOpenAI(model=settings.llm_model, api_key=settings.llm_api_key,
                      base_url=settings.llm_base_url, temperature=0)


async def answer_with_evidence(query: str, evidence: list[dict]) -> dict:
    evidence_ids = [item.get("evidence_id") for item in evidence]
    if not evidence:
        return {"status": "insufficient_evidence", "answer": "未检索到可用于回答的证据。", "evidence_ids": []}
    if not llm_enabled():
        return {"status": "llm_not_configured", "answer": "RAG 已返回证据；LLM 未配置，尚未生成回答。",
                "evidence_ids": evidence_ids}
    context = "\n\n".join(
        f"[{item.get('evidence_id')}] {item.get('file_name')} 第{item.get('page')}页\n{item.get('text')}"
        for item in evidence
    )
    response = await build_llm().ainvoke([
        ("system", "你是地质灾害知识问答助手。仅依据所给证据作答，引用 Evidence ID，不输出隐藏思维链。"),
        ("user", f"问题：{query}\n\n证据：\n{context}"),
    ])
    return {"status": "completed", "answer": response.content, "evidence_ids": evidence_ids}


async def draft_hazard_review(
    query: str,
    hazard_input: dict,
    current_record: dict,
    historical_record: dict,
    differences: list[dict],
    field_verification: list[dict],
    candidate_match: dict,
    risk_classification: dict,
    evidence: list[dict],
) -> dict:
    evidence_ids = [item.get("evidence_id") for item in evidence if item.get("evidence_id")]
    base = {
        "hazard_id": hazard_input.get("hazard_id"),
        "candidate_match": candidate_match,
        "field_comparison": differences,
        "field_verification": field_verification,
        "risk_classification": risk_classification,
        "recommendation": "",
        "evidence_ids": evidence_ids,
        "llm_status": "llm_not_configured",
    }
    if not llm_enabled():
        base["recommendation"] = "LLM 未配置，未生成自动复核建议；请结合字段核验和 Evidence 原文人工判断。"
        return base

    evidence_text = "\n\n".join(
        f"[{item.get('evidence_id')}] {item.get('file_name')} 第{item.get('page')}页\n{item.get('text')}"
        for item in evidence
    ) or "没有检索到证据。"
    system = (
        "你是地灾隐患复核助手。只输出一个 JSON 对象，不输出 Markdown 或思维过程。"
        "字段为 risk_level (low/medium/high/unknown)、recommendation (string)、"
        "review_notes (string[])。只能引用提供的证据和业务字段；信息不足时 risk_level=unknown，"
        "recommendation 说明需要人工核验。不得声称已写入业务台账。"
    )
    user = (
        f"复核问题：{query}\n隐患输入：{json.dumps(hazard_input, ensure_ascii=False)}\n"
        f"当前记录：{json.dumps(current_record, ensure_ascii=False)}\n"
        f"历史记录：{json.dumps(historical_record, ensure_ascii=False)}\n"
        f"字段差异：{json.dumps(differences, ensure_ascii=False)}\n"
        f"字段核验：{json.dumps(field_verification, ensure_ascii=False)}\n"
        f"候选匹配：{json.dumps(candidate_match, ensure_ascii=False)}\n"
        f"风险输入：{json.dumps(risk_classification, ensure_ascii=False)}\n"
        f"Evidence：\n{evidence_text}"
    )
    response = await build_llm().ainvoke([("system", system), ("user", user)])
    try:
        generated = json.loads(response.content)
        if not isinstance(generated, dict):
            raise ValueError("Expected a JSON object")
        level = generated.get("risk_level")
        if level not in {"low", "medium", "high", "unknown"}:
            level = "unknown"
        recommendation = generated.get("recommendation")
        notes = generated.get("review_notes", [])
        if not isinstance(recommendation, str) or not isinstance(notes, list):
            raise ValueError("Invalid structured result fields")
        base.update({"risk_classification": {"level": level, "source": "llm_structured_output",
                                              "evidence_ids": evidence_ids},
                     "recommendation": recommendation,
                     "review_notes": [str(note) for note in notes],
                     "llm_status": "completed"})
    except (TypeError, ValueError, json.JSONDecodeError):
        base["llm_status"] = "invalid_structured_output"
        base["recommendation"] = "LLM 未返回可解析的结构化结果，请人工检查 Evidence 后复核。"
    return base
