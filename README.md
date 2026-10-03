# 重庆地灾智能体

本仓库把地灾前端、独立 Java 后端与本地 AI 层放在一个可构建项目中，供本机开发和后续迁移学习。

## 项目结构

- `frontend/geo-guard-fe`：Vue 3 地灾前端与 AI Studio。
- `frontend/yuxi-web`：从 Yuxi v0.7.3 迁入的知识库界面，嵌入 AI Studio 的 `/yuxi/`。
- `backend/geo-guard-backend`：独立 Maven reactor、Spring Boot 业务源码、本地认证与 RAG 门面。
- `ai/geo-ai-foundation`：独立 Python RAG Studio 与 LangGraph Agent。

## Yuxi 知识库迁移

知识库界面基于本地 Yuxi v0.7.3 源码，保留文件管理、知识库配置、检索测试、知识图谱、思维导图、评估、文件夹和上传解析/索引工作流。嵌入 AI Studio 后复用宿主登录，不再显示第二个 Yuxi 登录页。迁移来源、保留的上游算法及 Geo 适配边界见 [UPSTREAM.md](ai/geo-ai-foundation/ai-services/geo-rag-studio/backend/vendor/yuxi/UPSTREAM.md)。

RAG 后端直接复用并调用迁入的 Yuxi chunk、parser、embedding、reranker、Dify/Notion connector、图谱/思维导图和评测实现；Geo 用 SQLite、本地文件和单 worker 队列提供独立运行时。所有知识库的 parser、embedding、chunk、retrieval、reranker 配置会保存并用于各自的入库和查询。知识库界面可填 MinerU、OpenAI-compatible Embedding 和 Reranker 服务地址/API Key，并实际测试连接；密钥保存在本机运行数据中，不提交到仓库。

Geo RAG 不依赖 `E:\Yuxi` 的进程、数据库、容器或数据卷。`:8010` 可用 SQLite 执行 PDF/文本解析、分块、BM25 检索、融合、Evidence 和 Final Context；Embedding、Reranker、MinerU 与 LLM 未配置时会显示 unavailable，不伪造成功。项目自有 Milvus/SeaweedFS 是可选检索栈；启用后使用 Yuxi 同款 BM25、向量字段和 Milvus WeightedRanker。当前不是 Yuxi 整个平台或基础设施的逐文件复制：Neo4j、Yuxi 的 PostgreSQL/Redis 分布式队列与 Docker Harness 未迁入。77 个上游知识库相关方法/路径均有本地适配路由，路径覆盖不等于对每个上游部署环境的运行时完全等价。

## 本地启动

1. 安装前端依赖并启动：

   ```powershell
   cd frontend/geo-guard-fe
   npm install
   npm run dev
   ```

   构建命令会先构建 `frontend/yuxi-web`，再构建主前端。开发服务器默认使用 `:5174`。

2. 按 [AI 层 README](ai/geo-ai-foundation/README.md) 启动 RAG Studio `:8010`。轻量模式只需 Python 与本地 SQLite/文件；Embedding、Reranker、MinerU、LLM 和连接型知识库需要使用者自己的服务地址与密钥。真实密钥只放入被 Git 忽略的 `.env` 或本机知识库设置。

3. Java reactor 不依赖组织内 `bwy-project` 父 POM 或私有 Maven 制品。Windows x64 的 JDK 21、Maven 3.9.16 与构建所需依赖缓存由 GitHub Release 的离线构建包提供。将该 zip 下载到仓库外或本机目录后，在 `backend/geo-guard-backend` 运行：

   ```powershell
   .\build-offline.ps1 -KitArchive 'D:\path\to\geo-guard-offline-build-kit.zip'
   ```

   构建脚本使用包内工具并强制 Maven `-o`，不需要预装 Java/Maven 或访问 Maven Central。完整业务 Spring Boot 服务还需要项目独立 PostgreSQL/PostGIS 和 Redis；本地认证/RAG 门面可单独运行，详见各自 README。

## 最近验证（2026-10-03）

- RAG Python：`compileall` 通过，122 项测试通过。
- Java：使用离线工具包执行 `mvn -o clean verify`，33 个模块、394 项测试通过。
- 前端：`npm run build` 成功，Yuxi 与主 Vue 应用均完成生产构建；构建有大型 JS chunk 提示。
- 本机服务：前端 `:5174` → Java 本地认证/RAG 门面 `:8007` → 当前源码 RAG `:8010`。`:8010/health` 返回 200；Java 门面能读取已保存知识库，直连与门面 debug 检索结果一致。当前跑在 `:8007` 的是轻量开发门面；完整业务 Spring Boot 的编译已离线验证，最近运行检查时项目 Redis `:26379` 未监听，因此这次没有验收其完整业务 API。
- 真实 PDF 检索：49 页重庆地灾应急预案，BM25 38 条候选、Vector 0 条、Fusion 5 条、Rerank 0 条、Evidence 5 条、Final Context 3330 字符。5 条 Evidence 都有页码和 bbox；嵌入式 Yuxi 页面实际显示 5 个检索结果，定位原文打开第 17/49 页，红框覆盖匹配的 PDF 原文。
- 外部 Provider：本机尚未配置 Embedding、Reranker、MinerU 或 LLM API，因此 Vector/Rerank/OCR/LLM 的真实模型调用尚未验收；配置后需用工作台里的测试按钮实际请求服务。

## 许可与发布

项目原创部分采用 Apache License 2.0。Yuxi 前端及迁入模块保留上游 MIT 许可；其他第三方许可和署名见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。本仓库不包含本机 `.env`、API Key、用户数据、上传 PDF、SQLite 数据库或 Yuxi Docker 数据。
