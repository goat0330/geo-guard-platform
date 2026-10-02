# Geo AI Foundation

独立的 Python AI 层，为重庆地灾前端提供可本地运行、可迁移的 RAG 与 LangGraph 能力。知识库工作台经完整 Spring Boot 业务应用 `:8007` 访问 RAG `:8010`，共用宿主登录。Graph 开发入口当前仍连接 Python `:8011`，业务上下文可由用户用 JSON 提供；现有 Dify API 保留。

```text
geo-guard-fe :5174 /ai-studio (embeds the native Yuxi knowledge UI at /yuxi/)
   ├─ /api/dizai/ai/rag + Yuxi /api/knowledge
   │      → local Java facade :8007 → geo-rag-studio :8010
   └─ /geo-ai-agent → geo-agent-graph :8011 → geo-rag-studio :8010

官方 LangGraph Studio 画布 → 本地 `langgraph dev` API :2024 → 同一份 graph 定义

geo-guard-backend/chongqing-geological-disaster-start :8007 provides real local
login and the RAG API proxy, using project-owned PostgreSQL/PostGIS :15432 and
Redis :26379. The older local-auth-service is no longer the running facade.
```

## 当前实现

- `geo-rag-studio`：SQLite + 本地 PDF/文本保存元数据与原文；PyMuPDF layout、页码和 bbox；Yuxi RAGFlow-style General/QA/Book/Laws/Semantic/Separator chunkers；Milvus 2.5.6 + SeaweedFS 可复现栈提供原生 BM25、COSINE 向量与 WeightedRanker，SQLite BM25 是轻量 fallback；Semantic 使用知识库配置的 embedding API 做聚类，没有 Provider 时显式标记 fallback；还包括 OpenAI-compatible embedding/reranker、MinerU 自部署 `/file_parse` 与官方 batch API 适配器、Evidence 原文定位、Final Context 和人工标注 Evidence ID Evaluation。知识图谱与思维导图使用知识库保存的 OpenAI-Compatible LLM 配置实际抽取/生成；未配置时明确返回 unavailable，不产出关键词图或来源轮廓冒充模型结果。
- `geo-agent-graph`：隐患复核图包含输入规范化、业务上下文、证据召回、候选匹配、字段核验、一次补充检索、风险分类、结构化结果和人工复核。Trace 是真实节点的运行/完成状态，不含模型隐藏思维链。
- Spring Boot：`POST /dizai/ai/rag/retrieve`、`/dizai/ai/rag/debug/retrieve`、`/dizai/ai/graph/run`、`/dizai/ai/graph/run/stream`；隐患点 ID 可通过既有业务服务读取基础记录，Dify 原接口保留。
- 前端 AI Studio 的 RAG 请求经 Java :8007 转发到 :8010；Graph 当前开发入口仍经 Vite 代理访问 :8011。当前不读取业务数据库，不把输入 ID 当成已查到的记录。
- Yuxi 原生 Vue 前端已整体复制到 `frontend/yuxi-web`，其知识库/扩展入口作为静态前端嵌入 AI Studio 的 `/yuxi/`。Geo SQLite 服务承载文件管理和 ZIP 文件夹上传，Milvus/SeaweedFS 承载可选搜索索引，并提供可配置 Embedding/Reranker、MinerU 自部署和官方 API、Dify/Notion 只读连接器、LLM 实体关系图谱、LLM 思维导图和 Evidence ID 评测。官方 MinerU API Key 可以按知识库配置，也可以写入被忽略的 `.env`；服务端响应只返回配置状态，解析测试会实际上传所选 PDF。它不是 Yuxi 后端基础设施的逐项复制：Geo 使用独立 Milvus/SeaweedFS 实例，不连接 Yuxi 的服务、数据卷或配置；Neo4j、图谱向量索引、分布式任务和多用户权限不包含；无 LLM 时依赖模型的生成能力明确返回 unavailable。
- Yuxi 知识库上传弹窗的“个人空间”使用 `RAG_WORKSPACE_DIR`（默认 `geo-rag-studio/data/workspace`）作为受限本地目录；把文件放入此目录后，可在知识库文件选择器中浏览并真实导入。它不会浏览项目目录以外的磁盘路径。

Yuxi 上游工作树 commit 为 `caff3208c9db9128aa0b277bc5c669141383ddbd`；v0.2 压缩包记录的 `031e2c...` commit 在本机克隆中不可解析。前端知识库界面按可追溯文件清单直接迁入；后端知识库相关代码直接 vendoring 并由 Geo API/存储适配层调用，不是把 Yuxi 整仓和整套 Docker Harness 原样复制。Yuxi 工作树未修改。

## 本地启动

Python 3.11+、Node.js、Java 21 与 Maven。第一次安装：

```powershell
$AiRoot = 'ai/geo-ai-foundation/ai-services'
& "$AiRoot\.venv\Scripts\python.exe" -m pip install -e "$AiRoot\geo-rag-studio\backend[test]" -e "$AiRoot\geo-agent-graph"
```

在 RAG 后端目录先复制/合并 `.env.example` 中的 Milvus 设置到被忽略的 `.env`，再启动 Geo 独立索引栈。不要覆盖已有 `.env` 里的用户配置。该栈使用本项目自己的 Docker Compose 项目、卷和 `:19531` 端口，不读取或修改 Yuxi 的容器与数据。

```powershell
Set-Location 'ai/geo-ai-foundation/ai-services/geo-rag-studio/backend'
docker compose -f docker-compose.milvus.yml up -d
```

本机文件选择器对应的受限工作区默认是 `ai/geo-ai-foundation/ai-services/geo-rag-studio/data/workspace`；需要导入已有文件时，可把文件放到该目录，或在 `.env` 设置 `RAG_WORKSPACE_DIR` 指向另一个专用目录。

然后启动两个 Python 服务。工作目录很重要：RAG 元数据与文件默认保存在 `geo-rag-studio\data`。

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

已有 `.studio-venv` 或安装了 Studio extra 的 `.venv` 时，也可执行 `start-local-studio.ps1`，后台启动同一个官方 API。脚本只为子进程设置 UTF-8 与无颜色日志，避免 Windows GBK 解码错误和缺少 colorama 的启动失败；不修改图定义。已实测 `/ok` 返回 `{"ok":true}`，助手列表注册了 `hazard_review` 和 `qa`。官方 Studio 网页仍由 LangChain 托管，不能把这个开发入口称为完全离线的画布。

RAG 知识库工作台：

```powershell
Set-Location 'frontend/geo-guard-fe'
npm install
npm run dev
```

打开 `https://127.0.0.1:5174/ai-studio`，首次使用开发自签名证书需要用户手动处理浏览器证书提示。知识库标签页加载迁入的 Yuxi Vue 知识库模块；主前端 `npm run build` 会先生成并打包 `public/yuxi`。Yuxi 知识库 API 与 AI Studio RAG API 都经完整 Java :8007 转发到 :8010；Graph 开发 API 通过 `/geo-ai-agent` 到 :8011。正式隐患复核页的 Dify 调用不变。

`/ai-studio` 路由和侧栏入口同时包含在开发与生产构建中，均要求宿主账号登录。嵌入的 Yuxi 知识库模块共用宿主认证，不增加第二次 Yuxi 登录。

Embedding、Reranker、MinerU 和 LLM 都是可选外部依赖。分别将 `geo-rag-studio/backend/.env.example` 复制为 `backend/.env`、将 `geo-agent-graph/.env.example` 复制为 `geo-agent-graph/.env`，再填入本机服务地址和密钥；也可在本地知识库设置里单独填写 Provider。RAG Provider 可通过 `/api/v1/providers/embedding/test`、`/api/v1/providers/parser/test` 和 `/api/v1/providers/reranker/test` 实测，知识库设置页的解析器测试会要求上传真实 PDF；未配置时会明确返回 unavailable/error，不会把 Vector、Rerank、OCR 或 LLM 标成成功。

每个知识库的 Embedding/Reranker 使用其保存的配置或所选模型供应商记录，不把空地址、空模型或缺失密钥与全局 `.env` 逐字段拼接。空地址/模型表示该知识库未启用该 Provider；空密钥更新仍保留已有密钥以支持页面脱敏编辑。全局 Provider 测试仅在请求省略配置时使用 `.env` 默认值。

RAG 数据目录已被 `.gitignore` 排除。当前库中的公开应急预案只作技术测试样本，不能作为你的正式自有知识资料；正式验收要上传你提供的 PDF/文本。接口、数据流和手工 Dify 学习步骤见 [AI 层学习与联调](docs/AI_LAYER_LEARNING.md)。
