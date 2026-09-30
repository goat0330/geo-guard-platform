# geo-guard-backend 集成

Spring Boot 是统一业务门面，保留现有 Dify Client、业务 API、登录鉴权和 Dify SSE。新增的 Python 服务不替换这些接口。

| Spring Boot 路由 | Python 上游 | 用途 |
|---|---|---|
| `POST /dizai/ai/rag/retrieve` | RAG `POST /api/v1/retrieve` | Evidence 与 Final Context |
| `POST /dizai/ai/rag/debug/retrieve` | RAG `POST /api/v1/debug/retrieve` | BM25/Vector/Fusion/Rerank 调试 |
| `POST /dizai/ai/graph/run` | Graph `POST /api/v1/run` | 非流式 LangGraph 运行 |
| `POST /dizai/ai/graph/run/stream` | Graph `POST /api/v1/run/stream` | 原样转发真实 SSE 节点状态 |

默认上游地址：RAG `http://127.0.0.1:8010`，Graph `http://127.0.0.1:8011`；可用 `geo.ai.rag.base-url` 和 `geo.ai.graph.base-url` Spring 属性覆盖。React/Vue 开发代理将 `/api` 转发到 Spring Boot `:8007`。

隐患复核请求可以包含 `hazard_input.hazard_id`。如果没有在 `context.current_record` 提供记录，Java 端通过既有 `IHazardPointService.queryById` 读取当前隐患点，并只发送复核所需字段；历史台账需由请求提供。Python 不直接连接业务数据库。Graph 结果始终标记待人工确认，不调用正式确认或写回 API。

新 AI 工作台仅在 Vue 开发构建下出现在侧边栏。正式隐患复核和原有 Dify 工作流继续走现有 Java API。Java HTTP proxy 单测覆盖上游路由、业务记录字段裁剪和 SSE 字节透传。
