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

Yuxi v0.7.3 的 chunk preset、切块器、OCR/parser、embedding/reranker 协议和检索配置数据类已直接迁入并由 Geo API 适配调用；检索配置界面从上游 `MilvusRetrievalConfig` 读取字段和参数边界。Geo 自己的 SQLite/文件层保存文档和 Evidence 定位，并可使用独立 Milvus 索引。Yuxi 的 `MilvusKB` 依赖其 PostgreSQL 仓储、模型注册表和 MinIO 解析文件，当前没有直接作为运行时类使用，因此后端不是全量行为等价。线上 Embedding、Reranker、MinerU OCR 与 LLM 地址/凭证没有随 ZIP 提供；真实 PDF 的 BM25 流程已验证，Vector、Rerank、OCR 与生成能力仍须配置 Provider 后验证。
