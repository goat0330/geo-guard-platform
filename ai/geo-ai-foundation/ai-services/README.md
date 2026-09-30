# AI Services

## geo-rag-studio :8010

- PDF（数字文本层）、TXT、MD、CSV、JSON 和粘贴文本入库；PDF 保留页码和可用 bbox。
- SQLite 保存资料、切块与 Embedding；支持 BM25 和可选 OpenAI-compatible 向量检索。
- 配置 MinerU endpoint 时实际调用 `/file_parse`；配置 reranker endpoint 时按候选 index/score 对齐返回重排结果。未配置时 provider 测试返回 unavailable。
- 提供资料/切块列表、Yuxi API 兼容层、检索调试、Evidence 原文定位、关键词图谱、思维导图和人工标注 RAG Evaluation。

## geo-agent-graph :8011

支持知识问答与隐患复核两条图：

```text
normalize_input → compare_records → retrieve_review_evidence
               → draft_review → mark_pending_review
```

隐患复核生成待人工确认草稿，不调用正式确认或台账写回接口。`:8011` 输出的 Trace 是业务节点和状态摘要，不是隐藏思维链。

## UI

Yuxi 原生 Vue 项目位于 `frontend/yuxi-web`，作为 AI Studio 的 `/yuxi/` 页面嵌入；原 Geo RAG Studio 调试工作台仍在 `geo-guard-fe/src/views/geoAiStudio`。两者均调用当前本地后端，不依赖 Yuxi Docker Harness。
