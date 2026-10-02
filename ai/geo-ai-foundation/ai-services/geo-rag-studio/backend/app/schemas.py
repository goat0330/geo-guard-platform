from typing import Any, Literal

from pydantic import BaseModel, Field


class SourceSpan(BaseModel):
    block_id: str | None = None
    page: int | None = None
    bbox: list[float] | None = None
    page_width: float | None = None
    page_height: float | None = None


class Evidence(BaseModel):
    evidence_id: str
    document_id: str
    chunk_id: str
    file_name: str
    page: int | None = None
    text: str
    token_count: int
    bbox: list[float] | None = None
    bm25_score: float | None = None
    vector_score: float | None = None
    fusion_score: float | None = None
    rerank_score: float | None = None
    source_spans: list[SourceSpan] = Field(default_factory=list)
    scores: dict[str, float | None] = Field(default_factory=dict)
    metadata: dict[str, Any] = Field(default_factory=dict)


class RetrieveRequest(BaseModel):
    query: str = Field(min_length=1)
    search_mode: Literal["hybrid", "keyword", "vector"] | None = None
    include_distances: bool | None = None
    top_k: int | None = Field(default=None, ge=1, le=100)
    final_top_k: int | None = Field(default=None, ge=1, le=100)
    recall_top_k: int | None = Field(default=None, ge=1, le=200)
    use_reranker: bool | None = None
    reranker_model: str | None = Field(default=None, max_length=200)
    vector_weight: float | None = Field(default=None, ge=0, le=1)
    bm25_weight: float | None = Field(default=None, ge=0, le=1)
    bm25_top_k: int | None = Field(default=None, ge=1, le=200)
    bm25_drop_ratio_search: float | None = Field(default=None, ge=0, le=1)
    similarity_threshold: float | None = Field(default=None, ge=0, le=1)
    use_graph_retrieval: bool | None = None
    graph_entity_top_k: int | None = Field(default=None, ge=1, le=100)
    graph_triple_top_k: int | None = Field(default=None, ge=1, le=100)
    graph_max_nodes: int | None = Field(default=None, ge=100, le=50000)
    graph_top_k: int | None = Field(default=None, ge=1, le=200)
    graph_weight: float | None = Field(default=None, ge=0, le=5)
    ppr_damping: float | None = Field(default=None, ge=0.1, le=0.99)
    filters: dict[str, Any] = Field(default_factory=dict)


class RetrieveResponse(BaseModel):
    query: str
    evidences: list[Evidence]
    retrieval: dict[str, Any]
    timing_ms: dict[str, float]
    usage: dict[str, Any]
    final_context: str = ""


class TextDocumentRequest(BaseModel):
    file_name: str = Field(min_length=1, max_length=255)
    text: str = Field(min_length=1)
    chunk_preset_id: str | None = None
    chunk_parser_config: dict[str, Any] = Field(default_factory=dict)
    metadata: dict[str, Any] = Field(default_factory=dict)
    knowledge_base_id: str | None = None
    folder_id: str | None = None


class EvaluationCase(BaseModel):
    query: str = Field(min_length=1)
    relevant_evidence_ids: list[str] = Field(min_length=1)
    top_k: int = Field(default=5, ge=1, le=100)
    filters: dict[str, Any] = Field(default_factory=dict)


class EvaluationRequest(BaseModel):
    cases: list[EvaluationCase] = Field(min_length=1, max_length=100)
    search_mode: Literal["hybrid", "keyword", "vector"] | None = None
    use_reranker: bool | None = None
    recall_top_k: int | None = Field(default=None, ge=1, le=200)


class KnowledgeBasePayload(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    description: str = Field(default="", max_length=1000)
    kb_type: Literal["local", "dify", "notion"] = "local"
    config: dict[str, Any] = Field(default_factory=dict)


class KnowledgeBaseUpdatePayload(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    config: dict[str, Any] = Field(default_factory=dict)


class EmbeddingTestRequest(BaseModel):
    base_url: str = ""
    api_key: str = ""
    model: str = ""
    dimensions: int | None = Field(default=None, ge=1)


class RerankerTestRequest(BaseModel):
    query: str = Field(min_length=1)
    documents: list[str] = Field(min_length=1, max_length=100)
    base_url: str = ""
    api_key: str = ""
    model: str = ""
    protocol: Literal["openai", "dashscope"] | None = None


class FolderPayload(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    parent_id: str | None = None


class FolderRenamePayload(BaseModel):
    name: str = Field(min_length=1, max_length=120)


class DocumentMovePayload(BaseModel):
    folder_id: str | None = None


class DocumentBatchPayload(BaseModel):
    document_ids: list[str] = Field(min_length=1, max_length=500)
    params: dict[str, Any] = Field(default_factory=dict)


class MindMapPayload(BaseModel):
    document_ids: list[str] = Field(default_factory=list, max_length=5000)
    user_prompt: str = Field(default="", max_length=4000)
    incremental: bool = False


class GraphConfigPayload(BaseModel):
    max_keywords_per_document: int = Field(default=12, ge=1, le=50)
    extractor_type: Literal["llm"] = "llm"
    extractor_options: dict = Field(default_factory=dict)


class EvaluationRunPayload(BaseModel):
    dataset_id: str | None = None
    cases: list[EvaluationCase] | None = Field(default=None, min_length=1, max_length=100)
    search_mode: Literal["hybrid", "keyword", "vector"] | None = None
    use_reranker: bool | None = None
    recall_top_k: int | None = Field(default=None, ge=1, le=200)
