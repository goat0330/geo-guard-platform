# 重庆地灾智能体：前端、业务服务与 AI 层

这是本地独立运行与学习候选，包含：

- `frontend/geo-guard-fe`：Vue 3 地灾前端与 AI Studio。
- `frontend/yuxi-web`：Yuxi 原生知识库/扩展前端，通过 `/yuxi/` 嵌入 AI Studio。
- `backend/geo-guard-backend`：独立 Maven reactor、Spring Boot 业务源码与本地认证/RAG 门面。
- `ai/geo-ai-foundation`：独立 Python RAG Studio 与 LangGraph Agent。

## 本地验证状态

截至 2026-10-01，Yuxi 原生 Vue 界面已纳入 `frontend/yuxi-web`，AI Studio 的知识库页用 iframe 加载 `/yuxi/index.html#/extensions`；`npm run build` 会先构建 Yuxi，再构建地灾主前端。候选前端使用 `:5174`，不依赖单独运行的 Yuxi `:5173`/`:5175` 开发服务器。

RAG Studio 用 SQLite/本地文件保存知识库配置、文档和证据定位；搜索可选择 SQLite fallback，或启用本项目独立的 Milvus 2.5.6 + SeaweedFS S3 栈，使用 Yuxi 同款中文 BM25 Function、向量字段和 Milvus WeightedRanker。后者已用真实重庆应急预案 PDF 在独立测试端口 `:8013` 完成解析、索引和 BM25 检索。SQLite fallback 方便轻量开发，但不承诺和 Milvus BM25 得分相同。前端知识库调用的正式链路仍是 Java :8007 → RAG :8010。当前本机未配置 Embedding、Reranker、MinerU 或 LLM Provider，真实 Vector、Rerank、OCR 与 LLM 请求仍是 unavailable，不能标成已通过。图谱向量索引、Yuxi 多用户权限、Neo4j 和分布式任务尚未迁入。

这不是 Yuxi 全产品或全后端的逐文件移植。知识库前端直接复制自 Yuxi v0.7.3 commit `caff3208c9db9128aa0b277bc5c669141383ddbd`：384 个上游跟踪文件中有 374 个 SHA-256 完全一致，另外 10 个只为接入宿主登录、API 路由和嵌入视图做了修改。后端直接迁入并调用上游 `DifyKB`、`NotionKB`、`OtherEmbedding`、解析器、切块器、Reranker 和评测代码；知识库、models、permissions 目录分别有 59/62、10/10、2/2 个文件与上游哈希一致，3 个 knowledge 文件含小范围本地补丁，详见 `ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/vendor/yuxi/UPSTREAM.md`。FastAPI 路由是适配层，不是原样复制的 Yuxi 路由模块；目前核对了 77 个上游接口路径，但路径覆盖不等于行为完全等价。Geo 使用自己的 SQLite/文件元数据层和独立 Milvus 2.5.6 索引，不连接 Yuxi 数据库、向量库或 Docker 数据卷；当前真实 PDF 测试覆盖 BM25-only，向量请求仍需用户配置 Embedding Provider。模型驱动的描述/评测集/示例问题生成在未配置 LLM 时返回 unavailable；连接 Dify/Notion 和外部模型需要用户自己的 token/key。Yuxi 智能体、MCP/Skills、用户管理、图谱向量索引、Neo4j、分布式任务和 Docker Harness 未迁入。

Cesium Ion 的第三方默认访问令牌已从随仓库打包的 Mars3D/Cesium 文件中移除。如业务地图确实使用 Cesium Ion，请在本地 `.env` 设置 `VITE_CESIUM_ION_TOKEN`；该字段为空时不启用 Ion 资源。

## 本地开发

1. 前端进入 `frontend/geo-guard-fe`，安装依赖后运行 `npm run dev`。AI Studio 使用已构建到 `public/yuxi` 的 Yuxi 知识库前端；修改 Yuxi 源码后运行 `npm run build:yuxi` 刷新其静态产物，完整发布构建运行 `npm run build`。
2. 按 `ai/geo-ai-foundation/README.md` 启动 Geo 独立 Milvus/SeaweedFS Compose，再启动 `geo-rag-studio`（8010）和 `geo-agent-graph`（8011）。Provider/LLM 密钥只写入各自被 Git 忽略的 `.env`。
3. Java Maven reactor 不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品；框架源码在仓库内。Windows x64 的 JDK 21、Maven 3.9.16 和验证所需 Maven 依赖缓存随 GitHub Releases 离线构建包提供。进入 `backend/geo-guard-backend` 执行 `.\build-offline.ps1 -KitArchive .\geo-guard-offline-build-kit.zip`，构建使用包内 JDK 并强制 Maven `-o`，不访问 Maven Central，也不要求预装 Java 或 Maven。
4. 本地验证码由 `backend/geo-guard-backend/local-auth-service/run-local.ps1` 提示设置开发账号后启动；账号密码不写入仓库。它提供本地登录与 RAG API 门面，不是完整业务数据库服务。

完整业务 Spring Boot 应用的运行还需要 PostgreSQL、Redis、数据库结构和部署配置；该环境尚未随源码提供，也未在本机启动验证。当前 `:8007` 本地服务只承载登录/验证码和 RAG 门面，不能据此宣称整个业务后端已联调完成。

## 发布清理

发布候选不包含本机 `.env`、私钥、部署证书、业务环境配置、上传 PDF、SQLite 数据库、用户管理 CSV 或 Yuxi Docker 数据。RAGFlow-style Yuxi 派生切块代码保留其随代码附带的 MIT 许可声明。

发布范围是上述当前前端、Spring Boot 后端和 AI 层，不包含旧的重复后端仓库。项目原创部分采用 Apache License 2.0，详见根目录 `LICENSE`；Yuxi 前端、RuoYi-Vue-Plus 框架和 RAGFlow-style parser 的第三方许可与署名见 `THIRD_PARTY_NOTICES.md`。第三方字体二进制因公开再分发权限未确认而从候选中移除，前端使用系统字体栈。

公开仓库地址为 `https://github.com/goat0330/geo-guard-platform`。完整 Spring Boot 业务服务运行仍缺 PostgreSQL/Redis 实例、业务 schema 与初始化数据；这些不影响独立 Maven 构建，但意味着业务后端的本机运行验收尚未通过。外部 Provider 也需在本机配置后再做真实模型验收。

## 当前验证

- RAG Studio：Python 锁定环境通过 `compileall`，最近一次全量测试为 65 passed。此前旧运行实例 `:8010` 经 Java :8007 的真实 PDF 测试有 3 个本地知识库、3 个 PDF、187 个 Chunk。最新 Geo 源码在独立 `:8013`、独立 Milvus 2.5.6 + SeaweedFS 数据栈上重新上传同一份真实预案：49 页解析为 1,008 个版面块、74 个 Chunk；BM25 48 条、Vector 0 条、Fusion 20 条、Evidence 5 条，Evidence 含 bbox，Final Context 1,936 字符。Embedding/Reranker/MinerU/LLM 凭据未配置；其真实调用仍未验收。现有 `:8010` 进程尚未切换到这份最新源码。
- LangGraph：Python smoke 测试与编译通过；当前没有 LLM 凭据，未验证真实模型生成。
- 前端：主前端/Yuxi 联合生产构建通过；`frontend/yuxi-web` 单元测试 317 项通过。构建有大 JS chunk 警告。验证码经 `:5174 → :8007` 返回有效 GIF（HTTP 200），本机登录、用户信息和 RAG API 代理已实测通过。
- 本地认证服务：Maven 编译成功，当前没有测试用例。
- Spring Boot Maven reactor：使用包含 Temurin JDK 21、Maven 和依赖缓存的离线包，在无 `target` 的临时源码副本中构建；系统 Java/Maven 路径已屏蔽，`mvn -o -B verify` 成功。33 个 reactor 模块成功，48 个测试套件、387 项，失败/错误/跳过均为 0。产物包含 Spring Boot `JarLauncher` 和 299 个嵌套依赖 JAR。Java 本地 RAG 门面 `/dizai/ai/rag/retrieve` 与 `/debug/retrieve` 已通过备用端口实测。完整业务服务运行期仍需要 PostgreSQL、Redis 和数据库结构；当前 `:8007` 运行的是本地认证/RAG 门面，不代表全部业务 API 已连接数据库。
