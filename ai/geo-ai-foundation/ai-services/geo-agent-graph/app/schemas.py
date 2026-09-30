from typing import Any, Literal

from pydantic import BaseModel, Field


class RunRequest(BaseModel):
    query: str = Field(min_length=1, max_length=4000)
    workflow: Literal["qa", "hazard_review"] = "hazard_review"
    hazard_input: dict[str, Any] = Field(default_factory=dict)
    context: dict[str, Any] = Field(default_factory=dict)


class RunResponse(BaseModel):
    workflow: str
    structured_result: dict[str, Any]
    evidence: list[dict[str, Any]]
    trace: list[dict[str, Any]]
