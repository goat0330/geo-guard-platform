# 重庆地灾智能体：前端、业务服务与 AI 层

这是本地独立运行与学习候选，包含：

- `frontend/geo-guard-fe`：Vue 3 地灾前端与 AI Studio。
- `frontend/yuxi-web`：Yuxi 原生知识库/扩展前端，通过 `/yuxi/` 嵌入 AI Studio。
- `backend/geo-guard-backend`：独立 Maven reactor、Spring Boot 业务源码与本地认证/RAG 门面。
- `ai/geo-ai-foundation`：独立 Python RAG Studio 与 LangGraph Agent。

## 本地验证状态

截至 2026-10-03，Yuxi 原生 Vue 界面已纳入 `frontend/yuxi-web`，AI Studio 的知识库页用 iframe 加载 `/yuxi/index.html?embed=rag#/extensions?tab=knowledge`；`npm run build` 会先构建 Yuxi，再构建地灾主前端。AI Studio 的路由和侧栏入口同时包含在开发与生产构建中，均要求宿主登录。嵌入的知识库使用宿主服务门面，不再要求第二次 Yuxi 登录。候选前端使用 `:5174`，不依赖单独运行的 Yuxi `:5173`/`:5175` 开发服务器。

RAG Studio 用 SQLite/本地文件保存知识库配置、文档和证据定位；搜索可选择 SQLite fallback，或启用本项目独立的 Milvus 2.5.6 + SeaweedFS S3 栈，使用 Yuxi 同款中文 BM25 Function、向量字段和 Milvus WeightedRanker。后者此前已用真实重庆应急预案 PDF 在独立测试端口 `:8013` 完成解析、索引和 BM25 检索。SQLite fallback 方便轻量开发，但不承诺和 Milvus BM25 得分相同。当前 Java :8007 → RAG :8010 已重新用真实 PDF 验证入库、检索、原文预览与 bbox 定位。当前本机未配置 Embedding、Reranker、MinerU 或 LLM Provider，真实 Vector、Rerank、OCR 与 LLM 请求仍是 unavailable，不能标成已通过。Yuxi 多用户权限、Neo4j 和分布式任务尚未完成迁移；SQLite 图谱向量实现已有测试，但真实外部 Embedding 未验收。

这不是 Yuxi 全产品或全后端的逐文件移植。知识库前端直接复制自 Yuxi v0.7.3 commit `caff3208c9db9128aa0b277bc5c669141383ddbd`，接入修改集中在宿主登录、API 路由和嵌入视图。后端直接迁入并调用上游 `DifyKB`、`NotionKB`、`OtherEmbedding`、解析器、切块器、Reranker 和评测代码；来源和本地补丁见 `ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/vendor/yuxi/UPSTREAM.md`。FastAPI 路由是适配层，不是原样复制的 Yuxi 路由模块；此前核对了 77 个上游接口路径，但路径覆盖不等于行为完全等价。Geo 使用自己的 SQLite/文件元数据层和独立 Milvus 2.5.6 索引，不连接 Yuxi 数据库、向量库或 Docker 数据卷；当前真实 PDF 测试覆盖 BM25-only，向量请求仍需用户配置 Embedding Provider。模型驱动的描述/评测集/示例问题生成在未配置 LLM 时返回 unavailable；连接 Dify/Notion 和外部模型需要用户自己的 token/key。Yuxi 智能体、MCP/Skills、用户管理、Neo4j、分布式任务和 Docker Harness 未迁入。

Cesium Ion 的第三方默认访问令牌已从随仓库打包的 Mars3D/Cesium 文件中移除。如业务地图确实使用 Cesium Ion，请在本地 `.env` 设置 `VITE_CESIUM_ION_TOKEN`；该字段为空时不启用 Ion 资源。

## 本地开发

1. 前端进入 `frontend/geo-guard-fe`，安装依赖后运行 `npm run dev`。AI Studio 使用已构建到 `public/yuxi` 的 Yuxi 知识库前端；修改 Yuxi 源码后运行 `npm run build:yuxi` 刷新其静态产物，完整发布构建运行 `npm run build`。
2. 按 `ai/geo-ai-foundation/README.md` 启动 Geo 独立 Milvus/SeaweedFS Compose，再启动 `geo-rag-studio`（8010）和 `geo-agent-graph`（8011）。Provider/LLM 密钥只写入各自被 Git 忽略的 `.env`。
3. Java Maven reactor 不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品；框架源码在仓库内。Windows x64 的 JDK 21、Maven 3.9.16 和验证所需 Maven 依赖缓存随 GitHub Releases 离线构建包提供。进入 `backend/geo-guard-backend` 执行 `.\build-offline.ps1 -KitArchive .\geo-guard-offline-build-kit.zip`，构建使用包内 JDK 并强制 Maven `-o`，不访问 Maven Central，也不要求预装 Java 或 Maven。
4. 在 `backend/geo-guard-backend` 按其 README 初始化独立 PostgreSQL/PostGIS、Redis 和本地管理员，然后运行 `.\start-local-business.ps1`。完整业务 JAR 提供 `:8007` 登录、真实验证码和 RAG 门面；密码、JWT 密钥保存在被忽略的本地文件。旧 `local-auth-service` 保留为早期开发源码，当前运行链路已使用完整业务应用。

完整业务 Spring Boot 应用已用包内 JDK 21 在 `:8007` 启动，连接项目自己的 PostgreSQL/PostGIS `:15432` 和 Redis `:26379`。`backend/geo-guard-backend/database/` 提供不含旧业务记录的 103 个表、11 个业务视图及索引、约束；初始化脚本已在全新独立集群实际执行，重复运行保留已有数据库。新增脚本支持独立 Redis、随机密码管理员、JWT 密钥和完整业务服务启动；真实认证的 19 项断言通过，详见后端 README。统一门面的 multipart、ASYNC 上下文和 PDF 响应类型问题已修复。主前端、Yuxi 普通知识请求、原生上传和 Markdown 图片读取共用宿主认证；独立 Yuxi 非嵌入页面仍保留其原有认证。新增本地监测视图的 PostgreSQL 测试与 10 个 Java 列表接口通过；本项目菜单、完整业务 API 和真实浏览器端到端验收尚未完成。

## 发布清理

发布候选不包含本机 `.env`、私钥、部署证书、业务环境配置、上传 PDF、SQLite 数据库、用户管理 CSV 或 Yuxi Docker 数据。RAGFlow-style Yuxi 派生切块代码保留其随代码附带的 MIT 许可声明。

发布范围是上述当前前端、Spring Boot 后端和 AI 层，不包含旧的重复后端仓库。项目原创部分采用 Apache License 2.0，详见根目录 `LICENSE`；Yuxi 前端、RuoYi-Vue-Plus 框架和 RAGFlow-style parser 的第三方许可与署名见 `THIRD_PARTY_NOTICES.md`。第三方字体二进制因公开再分发权限未确认而从候选中移除，前端使用系统字体栈。

公开仓库地址为 `https://github.com/goat0330/geo-guard-platform`。本地 PostgreSQL/PostGIS、103 个业务/系统表和 11 个业务视图、独立 Redis 和管理员已建立，完整 Spring Boot 业务服务在 `:8007` 通过真实认证与 RAG 门面验收；完整业务联调、菜单、浏览器验收和全部 Yuxi 知识库行为对齐仍待完成。外部 Provider 需配置后做真实模型验收。本地后续修改尚未全部推送。

## 当前验证

- RAG Studio：2026-10-03 最新源码通过 `compileall`，全量测试为 105 passed。Java :8007 → RAG :8010 已实测真实 PDF 上传、文件配置及分块/BM25检索：49 页、1,008 个版面块；文件级 256 tokens / 0% overlap 覆盖知识库默认 512 tokens，产生 67 个 Chunk，重建仍为 67 块，所有文本与直接调用迁入的 Yuxi dispatcher 一致。BM25 50 条、Vector 0 条、Fusion 5 条、Rerank 0 条、Evidence 5 条、Final Context 1,840 字符。5 条证据的页码/bbox 与保存的来源配对一致，框内可提取实际 PDF 原文。真实失败与重试验证了 `error_parsing`、`error_indexing`、待入库统计及成功后的错误清除；嵌套文件夹移动和删除实测通过，循环移动返回 400，删除后没有残留行、块或检索证据。思维导图文件清单、旧树变更检测、生成提示词、JSON 解析、文件上限和删除同步已直接复用迁入的 Yuxi 函数，并由 105 项全量测试覆盖。最新直连 `:8010` 的真实 PDF 请求再次解析为 49 页、1,008 块，入库后检索返回 38 BM25 候选、3 条 Evidence 和 2,000 字符上下文；PyMuPDF Parser Provider 返回 200，Embedding/Reranker 返回 503 unavailable（尚未配置 API）。LLM 思维导图真实生成、Java 门面最新代码的端到端重测、Vector/Rerank 与 Milvus 删除验收仍未完成。此前独立 Milvus `:8013` 测试仅覆盖 BM25，不能代替这些验收。
- LangGraph：Python smoke 测试与编译通过；当前没有 LLM 凭据，未验证真实模型生成。
- 前端：主前端/Yuxi 联合生产构建通过，AI Studio 生产路由及开发/生产宿主鉴权的 3 项测试通过；`frontend/yuxi-web` 此前全量单元测试 325 项通过，认证/上传/Markdown 相关测试 24 项通过。构建有大 JS chunk 警告。此前验证码经 `:5174 → :8007` 返回有效 GIF（HTTP 200），本机登录、用户信息和 RAG API 代理已实测通过。本轮前端启动使用默认开发 HTTPS，浏览器仍需要用户处理本地证书信任，尚未完成最新界面的浏览器验收。
- 本地认证服务：Maven 编译成功，当前没有测试用例。
- Spring Boot Maven reactor：此前在无 `target` 的临时源码副本用离线包构建，屏蔽系统 Java/Maven 路径，`mvn -o -B verify` 的 33 个模块、387 项测试全部通过。2026-10-03 最新 `.\build-offline.ps1` 再次用包内 JDK、Maven 和缓存完成 33 模块离线构建；394 项测试、零失败/错误/跳过。完整业务 JAR 此前运行在 `:8007` 并通过真实认证与 RAG 门面验收；本次复查发现项目 Redis `:26379` 未运行，Java 无法启动。继续验证 `:8007` 需先恢复项目 Redis。已通过 `mvn -o -B verify` 证明离线 Maven reactor 构建无需网络，不能据此推断全部业务 API 已验收。

2026-10-02 最新完整业务 Java 门面验收：1,528,947 字节 PDF 在 `:8007 → :8010` 保持原文一致，49 页、1008 个 bbox、38 个分块；BM25 38 条候选、5 条 Evidence、Final Context 3342 字符，5 个证据框内均可提取实际原文。Yuxi 前端使用的暂存上传、解析和索引三个接口也分别实测，状态为 `uploaded → parsed → indexed`；其 PDF 页预览返回有效 PNG。当前未配置模型，Vector/Rerank 未执行，三个外部 Provider 测试明确返回 unavailable。Yuxi 最新全量前端测试 325 项通过，最新认证/上传/Markdown 相关测试 24 项通过，Yuxi 静态前端重建成功；最新浏览器验收被本地自签名 HTTPS 证书阻断，需要用户手动处理。
