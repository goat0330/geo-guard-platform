# 当前 AI 层

```text
geo-guard-fe (Vue)
  ├── 正式业务页面 → Spring Boot :8007 → 既有 Dify / 业务 API
  ├── 开发模式 AI 工作台 → RAG Studio web :5175
  │                              └── geo-rag-studio :8010
  └── Agent Trace SSE → Spring Boot :8007 → geo-agent-graph :8011
                                               └── RAG retrieve → :8010
```

## 服务职责

- Spring Boot 负责业务鉴权与记录；只将复核需要的隐患点字段传给 Graph。
- RAG Studio 负责 PDF/Text 解析、bbox、chunk、BM25/可选向量、融合、可选 rerank、Evidence 和最终上下文。当前轻量存储是 SQLite + 本地文件。
- LangGraph 负责业务节点编排与 SSE trace；LLM 未配置时返回结构化的待复核结果，不伪造建议。
- RAG Studio web 是学习控制台；正式业务导航只显示原有 Dify 页面。

## 隐患复核图

```text
normalize_input → load_business_context → retrieve_evidence
  → match_candidate_hazard → verify_fields → need_more_evidence?
      ├─ yes → retrieve_more → verify_fields
      └─ no
  → classify_risk → generate_structured_result → human_review
```

Evidence 不足时最多补检一次。字段 evidence 状态使用精确原文包含检查，未命中就保留为待核验；风险等级只接受明确业务输入或有效结构化模型结果，未配置模型时为 `unknown`。Trace 只含 stage、status、summary、Evidence IDs，不包含隐藏思维链。

## 当前能力边界

E 盘 Yuxi 的 chunk preset contract、参数边界、recall_top_k、rerank index-score 对齐、MinerU 请求形状等模式已做小范围重实现。当前数据库仍为本地 SQLite；线上 Embedding、Reranker、MinerU OCR 与 LLM 地址/凭证没有随 ZIP 提供，需要用户配置后才能跑真实的 Vector、Rerank、OCR 和生成结果。
