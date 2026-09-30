# Geo AI Foundation

独立的 Python AI 层，为重庆地灾前端提供可本地运行、可迁移的 RAG 与 LangGraph 能力。学习/调试工作台直接连接这两个 Python 服务，不依赖 Spring Boot 业务库；当前业务记录由用户用 JSON 提供。正式业务集成时仍可保留既有 Java 门面和 Dify API。

```text
geo-guard-fe :5174 /ai-studio (DEV; embeds the native Yuxi knowledge UI at /yuxi/)
   ├─ /api/dizai/ai/rag + Yuxi /api/knowledge
   │      → local Java facade :8007 → geo-rag-studio :8010
   └─ /geo-ai-agent → geo-agent-graph :8011 → geo-rag-studio :8010

官方 LangGraph Studio 画布 → 本地 `langgraph dev` API :2024 → 同一份 graph 定义

geo-guard-backend/local-auth-service :8007 provides local login and RAG API proxy.
The main Spring Boot business application builds independently, but needs its own
PostgreSQL/Redis configuration and schema before it can run as a business backend.
```

## 当前实现

- `geo-rag-studio`：SQLite + 本地 PDF/文本；PyMuPDF layout、页码和 bbox；Yuxi RAGFlow-style General/QA/Book/Laws/Semantic/Separator chunkers；Semantic 使用知识库配置的 embedding API 做聚类，没有 Provider 时显式标记 fallback；BM25、OpenAI-compatible embedding/reranker、MinerU 自部署 `/file_parse` 与官方 batch API 适配器、Evidence 原文定位、Final Context 和人工标注 Evidence ID Evaluation。知识图谱与思维导图使用知识库保存的 OpenAI-Compatible LLM 配置实际抽取/生成；未配置时明确返回 unavailable，不产出关键词图或来源轮廓冒充模型结果。
- `geo-agent-graph`：隐患复核图包含输入规范化、业务上下文、证据召回、候选匹配、字段核验、一次补充检索、风险分类、结构化结果和人工复核。Trace 是真实节点的运行/完成状态，不含模型隐藏思维链。
- Spring Boot：`POST /dizai/ai/rag/retrieve`、`/dizai/ai/rag/debug/retrieve`、`/dizai/ai/graph/run`、`/dizai/ai/graph/run/stream`；隐患点 ID 可通过既有业务服务读取基础记录，Dify 原接口保留。
- 前端 AI Studio 的 RAG 请求经 Java :8007 转发到 :8010；Graph 当前开发入口仍经 Vite 代理访问 :8011。当前不读取业务数据库，不把输入 ID 当成已查到的记录。
- Yuxi 原生 Vue 前端已整体复制到 `frontend/yuxi-web`，其知识库/扩展入口作为静态前端嵌入 AI Studio 的 `/yuxi/`。本地 SQLite 服务承载文件管理和 ZIP 文件夹上传、BM25/Vector/Fusion/Rerank、可配置 Embedding/Reranker、MinerU 自部署和官方 API、Dify/Notion 只读连接器、LLM 实体关系图谱、LLM 思维导图和 Evidence ID 评测。官方 MinerU API Key 可以按知识库配置，也可以写入被忽略的 `.env`；服务端响应只返回配置状态，解析测试会实际上传所选 PDF。它不是 Yuxi 后端基础设施的逐项复制：Milvus、Neo4j、图谱向量索引、分布式任务和多用户权限不包含；无 LLM 时依赖模型的生成能力明确返回 unavailable。

本地 Yuxi 源码审阅位置：`the upstream Yuxi source tree`。本机工作树 commit 为 `caff3208c9db9128aa0b277bc5c669141383ddbd`；v0.2 压缩包记录的 `031e2c...` commit 在本机克隆中不可解析。因此这里记录为基于本地源码重实现的 Yuxi-derived 部件，不宣称与压缩包引用 commit 做过逐文件 diff。Yuxi 工作树未修改。

## 本地启动

Python 3.11+、Node.js、Java 21 与 Maven。第一次安装：

```powershell
$AiRoot = 'ai/geo-ai-foundation/ai-services'
& "$AiRoot\.venv\Scripts\python.exe" -m pip install -e "$AiRoot\geo-rag-studio\backend[test]" -e "$AiRoot\geo-agent-graph"
```

在两个 PowerShell 窗口分别运行。工作目录很重要：RAG 数据默认保存在 `geo-rag-studio\data`。

```powershell
Set-Location 'ai/geo-ai-foundation/ai-services\geo-rag-studio\backend'
& '..\..\.venv\Scripts\python.exe' -m uvicorn app.main:app --host 127.0.0.1 --port 8010
```

```powershell
Set-Location 'ai/geo-ai-foundation/ai-services\geo-agent-graph'
& '..\.venv\Scripts\python.exe' -m uvicorn app.main:app --host 127.0.0.1 --port 8011
```

要在官方 LangGraph Studio 中查看和调试隐患复核画布，另开 PowerShell 窗口运行：

```powershell
Set-Location 'ai/geo-ai-foundation/ai-services\geo-agent-graph'
$env:LOG_COLOR = 'false'
$env:PYTHONUTF8 = '1'
$env:PYTHONIOENCODING = 'utf-8'
uv run --extra studio langgraph dev --port 2024 --no-browser
```

该命令启动本地开发 API，并打印官方 Studio 链接；图代码与 RAG API 在本机运行。此 `langgraph dev` 是内存态开发服务，正式业务请求仍走 `geo-agent-graph :8011` 和当前本地 SSE 接口。

RAG 知识库工作台：

```powershell
Set-Location 'frontend/geo-guard-fe'
npm install
npm run dev
```

打开 `http://127.0.0.1:5174/ai-studio`。知识库标签页加载迁入的 Yuxi Vue 知识库模块；主前端 `npm run build` 会先生成并打包 `public/yuxi`。Yuxi 知识库 API 与 AI Studio RAG API 都经 Java :8007 转发到 :8010；Graph 开发 API 通过 `/geo-ai-agent` 到 :8011。正式隐患复核页的 Dify 调用不变。

Embedding、Reranker、MinerU 和 LLM 都是可选外部依赖。分别将 `geo-rag-studio/backend/.env.example` 复制为 `backend/.env`、将 `geo-agent-graph/.env.example` 复制为 `geo-agent-graph/.env`，再填入本机服务地址和密钥；也可在本地知识库设置里单独填写 Provider。RAG Provider 可通过 `/api/v1/providers/embedding/test`、`/api/v1/providers/parser/test` 和 `/api/v1/providers/reranker/test` 实测，知识库设置页的解析器测试会要求上传真实 PDF；未配置时会明确返回 unavailable/error，不会把 Vector、Rerank、OCR 或 LLM 标成成功。

RAG 数据目录已被 `.gitignore` 排除。当前库中的公开应急预案只作技术测试样本，不能作为你的正式自有知识资料；正式验收要上传你提供的 PDF/文本。接口、数据流和手工 Dify 学习步骤见 [AI 层学习与联调](docs/AI_LAYER_LEARNING.md)。
