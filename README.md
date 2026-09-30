# 重庆地灾智能体：前端、业务服务与 AI 层

这是本地独立运行与学习候选，包含：

- `frontend/geo-guard-fe`：Vue 3 地灾前端与 AI Studio。
- `frontend/yuxi-web`：Yuxi 原生知识库/扩展前端，通过 `/yuxi/` 嵌入 AI Studio。
- `backend/geo-guard-backend`：独立 Maven reactor、Spring Boot 业务源码与本地认证/RAG 门面。
- `ai/geo-ai-foundation`：独立 Python RAG Studio 与 LangGraph Agent。

## 本地验证状态

截至 2026-10-01，Yuxi 原生 Vue 界面已纳入 `frontend/yuxi-web`，AI Studio 的知识库页用 iframe 加载 `/yuxi/index.html#/extensions`；`npm run build` 会先构建 Yuxi，再构建地灾主前端。候选前端使用 `:5174`，不依赖单独运行的 Yuxi `:5173`/`:5175` 开发服务器。

RAG Studio 用 SQLite 和本地文件提供 Yuxi 知识库 API 兼容层，支持 PDF/文本和 ZIP 文件夹入库、六类分块策略、BM25/Vector/Fusion/Rerank、Evidence 页码/bbox、实体图谱、LLM 思维导图和 Final Context。前端知识库调用经 Java :8007 到 RAG :8010。Semantic 在知识库配置 Embedding Provider 时执行聚类，未配置时明确标记 fallback。当前本机未配置 Embedding、Reranker、MinerU 或 LLM Provider，因此对应真实外部模型/OCR调用仍是 unavailable，不能标成已通过。知识图谱使用已配置的 OpenAI-Compatible LLM 抽取并保存在 SQLite；图谱专用向量索引、Yuxi 多用户权限、Milvus、Neo4j 和分布式任务尚未迁入。

Yuxi 前端知识库模块已迁入，但不是 Yuxi 全产品或全后端的逐文件移植。当前本地服务实现文件、知识库配置、检索、图谱/思维导图和人工标注评测工作流；模型驱动的描述/评测集/示例问题生成在未配置 LLM 时返回 unavailable。Dify/Notion 是可配置只读连接器，真实连接需要用户自己的 token。其余智能体、MCP/Skills、Yuxi 用户管理、图谱向量索引、Milvus、Neo4j、分布式任务和 Docker Harness 未迁入。不能据此声称 Yuxi 全功能 100% 等价。

Cesium Ion 的第三方默认访问令牌已从随仓库打包的 Mars3D/Cesium 文件中移除。如业务地图确实使用 Cesium Ion，请在本地 `.env` 设置 `VITE_CESIUM_ION_TOKEN`；该字段为空时不启用 Ion 资源。

## 本地开发

1. 前端进入 `frontend/geo-guard-fe`，安装依赖后运行 `npm run dev`。AI Studio 使用已构建到 `public/yuxi` 的 Yuxi 知识库前端；修改 Yuxi 源码后运行 `npm run build:yuxi` 刷新其静态产物，完整发布构建运行 `npm run build`。
2. 按 `ai/geo-ai-foundation/README.md` 启动 `geo-rag-studio`（8010）和 `geo-agent-graph`（8011）。Provider/LLM 密钥只写入各自被 Git 忽略的 `.env`。
3. Java Maven reactor 不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品；框架源码在仓库内。进入 `backend/geo-guard-backend` 执行 `mvn -B clean verify`。构建需要 JDK 21 和 Maven，不需要模型 API Key。
4. 本地验证码由 `backend/geo-guard-backend/local-auth-service/run-local.ps1` 提示设置开发账号后启动；账号密码不写入仓库。它提供本地登录与 RAG API 门面，不是完整业务数据库服务。

完整业务 Spring Boot 应用的运行还需要 PostgreSQL、Redis、数据库结构和部署配置；该环境尚未随源码提供，也未在本机启动验证。当前 `:8007` 本地服务只承载登录/验证码和 RAG 门面，不能据此宣称整个业务后端已联调完成。

## 发布清理

发布候选不包含本机 `.env`、私钥、部署证书、业务环境配置、上传 PDF、SQLite 数据库、用户管理 CSV 或 Yuxi Docker 数据。RAGFlow-style Yuxi 派生切块代码保留其随代码附带的 MIT 许可声明。

发布范围是上述当前前端、Spring Boot 后端和 AI 层，不包含旧的重复后端仓库。项目原创部分采用 Apache License 2.0，详见根目录 `LICENSE`；Yuxi 前端、RuoYi-Vue-Plus 框架和 RAGFlow-style parser 的第三方许可与署名见 `THIRD_PARTY_NOTICES.md`。第三方字体二进制因公开再分发权限未确认而从候选中移除，前端使用系统字体栈。

公开仓库地址为 `https://github.com/goat0330/geo-guard-platform`。完整 Spring Boot 业务服务运行仍缺 PostgreSQL/Redis 实例、业务 schema 与初始化数据；这些不影响独立 Maven 构建，但意味着业务后端的本机运行验收尚未通过。外部 Provider 也需在本机配置后再做真实模型验收。

## 当前验证

- RAG Studio：`python -m compileall -q app tests` 通过，`python -m pytest -q` 为 28 passed。候选目录自己的 SQLite 有 3 个知识库、3 个真实 PDF 和 187 个 Chunk；PyMuPDF 实测解析 49 页、1008 个带 bbox 的版面块。经 Java :8007 → RAG :8010 的真实检索返回 46 个 BM25 候选、0 个 Vector 候选、30 条 Fusion 候选、5 条 Evidence 和 3,377 字符 Final Context；Evidence 位于 PDF 第 28 页，bbox 页图返回有效 PNG。Embedding、Reranker 和 MinerU 测试均明确返回 unavailable，Vector/Rerank/OCR 未验收。
- LangGraph：Python smoke 测试与编译通过；当前没有 LLM 凭据，未验证真实模型生成。
- 前端：主前端/Yuxi 联合生产构建通过；`frontend/yuxi-web` 单元测试 317 项通过。构建有大 JS chunk 警告。验证码经 `:5174 → :8007` 返回有效 GIF（HTTP 200），本机登录、用户信息和 RAG API 代理已实测通过。
- 本地认证服务：Maven 编译成功，当前没有测试用例。
- Spring Boot Maven reactor：JDK 21 下在隔离源码副本执行独立 `mvn -B clean verify` 成功；48 个测试套件、387 项，失败/错误/跳过均为 0；生成了包含 Spring Boot launcher 的业务可执行 JAR。Java 本地 RAG 门面 `/dizai/ai/rag/retrieve` 与 `/debug/retrieve` 已通过备用端口实测。完整业务服务运行期仍需要 PostgreSQL、Redis 和数据库结构；当前 `:8007` 运行的是本地认证/RAG 门面，不代表全部业务 API 已连接数据库。
