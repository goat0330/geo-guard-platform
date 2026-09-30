# AI 层学习与联调

## 用 RAG Studio 学习

1. 在 `http://127.0.0.1:5175` 打开 Pipeline Inspector，先确认实际配置状态。
2. 在 Documents 查看本地文档列表；可拖入自己的 PDF/TXT/MD/CSV/JSON，或直接粘贴文本。选择 chunk preset，调整 64–4096 的目标 token 数和 0–80% overlap；观察 Parsed Blocks、页码/bbox 和 Chunks。当前库中的公开应急预案只作回归样本，不代表你的正式知识库。
3. PDF 在 MinerU OCR 配置后走 MinerU/Layout；未配置时使用 PyMuPDF，扫描件没有可提取文字时会报错，不会伪报 OCR 成功。Chunk Inspector 展示平均/中位/最大 token、目标超限数、空块、来源跨度、Embedding 覆盖和完全重复块；token 数是近似估算。
4. 在 Retrieval Inspector 输入问题，依次比较 BM25、Vector、Fusion、Rerank 与 Evidence。可以调整 `search_mode`、`recall_top_k`、`final_top_k`、两路权重和文档过滤。
5. 点 Evidence 查看原文以及 PDF 页的 bbox 红框。未配置的 Vector/Rerank 会报错或标记 skipped，不会填充假分数。
6. 在 RAG Evaluation 手工标记 Relevant Evidence IDs，再运行 Recall@k、Precision@k、MRR、nDCG。评价指标只代表当前输入的标注集。

本地 RAG Studio SQLite 与 Dify Dataset 是两套独立知识库，目前没有自动同步。要比较两者，请在 Dify 的 Dataset 手动上传同一份 PDF，并使用一致的问题与人工相关性标注。

## 在 Dify 手工搭建 Workflow

此步骤用于学习 Dify 画布，不会由代码自动创建或改写 Dify 应用：

1. 从空白创建 **Workflow**，Start 输入设为 `query`、`hazard_id`、`current_record_json`、`historical_record_json)。第一轮学习可以手工粘贴两份 JSON，不必先接业务库。
2. 在 Dify Dataset 新建知识库并手工上传同一份自有地灾 PDF/文本；检查解析文本和 chunk，再用约 512 tokens、10% overlap 做首轮对照。
3. 加 Knowledge Retrieval，选择该 Dataset，将 `query` 与隐患类型/位置等必要字段组织成检索问题；观察返回 chunk、score 和 metadata。此处的 Dify Dataset 与本地 SQLite RAG 库互不自动同步。
4. 加 IF/ELSE：证据为空或待核验字段缺证据时，走一次针对缺失字段的补充 Knowledge Retrieval；随后回到统一的核验分支，不无限循环。
5. 加 LLM，开启结构化输出，字段设为 `candidate_match`、`field_diffs`、`risk_level`、`recommendation`、`evidence_ids`、`review_status`。要求只引用 Retrieval 返回的 Evidence IDs；缺证据输出 `unknown` / `needs_human_review`，不输出隐藏推理过程。
6. 可加 Code 节点校验 JSON 字段和 Evidence IDs 确实来自检索结果；End 输出草稿与 Evidence IDs，并标为待人工确认，不调用正式确认或台账写回接口。
7. Test Run 分别试“证据充分”“缺证据”“调查与历史记录冲突”三种输入，查看各节点输入/输出。确认链路后再 Publish。随后用相同输入对照本地 LangGraph Trace。

画布顺序：`Start → Code(normalize) → Knowledge Retrieval → IF/ELSE → [补充 Retrieval 一次] → LLM(JSON) → Code(validate) → End`。若要自动读取业务库，再把 Start 中手工输入的记录替换成 HTTP Request 调用现有业务接口；先确认该接口路径、鉴权和返回字段，不要在 Dify 客户端保存后端密钥。

Dify 的节点配置、结构化输出与工作流调试步骤见官方 [Workflow Quick Start](https://docs.dify.ai/en/guides/application-orchestrate/creating-an-application)；知识库能力概览见 [Knowledge Base Retrieval](https://docs.dify.ai/guides/knowledge-base/retrieval)。不同 Dify 版本的节点面板可能不同，按当前画布字段配置即可。

## 跑隐患复核 LangGraph

在 Vue 开发模式打开 `/ai-studio` → “隐患复核流程”。页面通过官方 LangGraph Studio 入口打开现成画布，不再自绘图编辑器。按 README 启动 `langgraph dev :2024` 后，在 Studio 选择 `hazard_review`；该开发 API 加载与业务 SSE 服务 `:8011` 相同的 Python graph 定义。官方 Studio Web UI 从 LangChain 站点加载，图执行与 RAG API 仍在本机。

在 Studio 的 Graph 输入框可直接试：

```json
{
  "query": "复核该隐患点当前风险与需要补充的证据",
  "workflow": "hazard_review",
  "hazard_input": {"hazard_id": "LOCAL-DEMO-001"},
  "context": {
    "current_record": {"hazard_name": "示例隐患点", "risk_level": "中风险"},
    "historical_record": {}
  }
}
```

正式前端的“运行复核流程”仍调用 `:8011` 的业务 API；Vite 将 Graph 请求转发到 `:8011`、RAG 请求转发到 `:8010`，无需 Spring Boot `:8007`。该页面的 Trace 来自真实 SSE `running` / `completed` 事件；Structured JSON 显示风险等级、字段差异、人工复核状态和 Evidence IDs。当前没有业务数据库；以后需要连接自己建设的业务 API 才能自动加载真实台账。

工作台默认不调用正式确认、台账写回或新的 Dify API。当前 LLM 没配置时只返回 `llm_not_configured` 状态和人工复核提示。
