<template>
  <div class="retrieval-page">
    <div class="query-panel">
      <div class="panel-heading"><div><b>检索测试</b><p>对当前知识库执行真实召回，查看各阶段排序、Evidence 与 Final Context。</p></div><el-tag :type="knowledgeBase.kb_type === 'dify' ? 'primary' : 'success'">{{ modeLabel }}</el-tag></div>
      <el-input v-model="query" type="textarea" :rows="3" resize="vertical" placeholder="例如：调查记录中的稳定性描述与历史台账不一致时，应核对哪些依据？" @keydown.ctrl.enter="run" />
      <div class="query-options">
        <label>检索模式<el-select v-model="searchMode"><el-option label="混合检索" value="hybrid" /><el-option label="BM25 关键词" value="keyword" /><el-option label="Vector 向量" value="vector" /></el-select></label>
        <label>最终 top_k<el-input-number v-model="finalTopK" :min="1" :max="100" /></label>
        <label>recall_top_k<el-input-number v-model="recallTopK" :min="1" :max="200" /></label>
        <label>BM25 top_k<el-input-number v-model="bm25TopK" :min="1" :max="200" /></label>
        <label>相似度阈值<el-input-number v-model="similarityThreshold" :min="0" :max="1" :step="0.05" /></label>
        <label>BM25 稀疏词丢弃<el-input-number v-model="bm25DropRatio" :min="0" :max="1" :step="0.05" /></label>
        <el-checkbox v-model="useReranker" :disabled="knowledgeBase.kb_type !== 'local'">使用 Reranker</el-checkbox>
        <el-checkbox v-model="useGraphRetrieval" :disabled="knowledgeBase.kb_type !== 'local'">图谱候选</el-checkbox>
        <el-button type="primary" :loading="loading" :disabled="!query.trim()" @click="run"><el-icon><Search /></el-icon>运行检索</el-button>
      </div>
    </div>

    <template v-if="debugResult">
      <div class="result-metrics">
        <div><b>{{ result?.evidences?.length || 0 }}</b><span>条最终证据</span></div>
        <div><b>{{ result?.retrieval?.candidate_count ?? 0 }}</b><span>个候选</span></div>
        <div><b>{{ result?.timing_ms?.total ?? '—' }}<small> ms</small></b><span>检索耗时</span></div>
        <div><b>{{ result?.retrieval?.mode || '—' }}</b><span>实际模式</span></div>
        <div><b>{{ result?.usage?.embedding_enabled ? '已启用' : '未启用' }}</b><span>Embedding</span></div>
      </div>

      <div class="pipeline-status">
        <div v-for="stage in debugResult.pipeline || []" :key="stage.stage" class="pipeline-stage" :class="`stage-${stage.status}`">
          <span class="stage-dot"></span><div><b>{{ stageLabel(stage.stage) }}</b><small>{{ stage.summary }}</small></div>
        </div>
      </div>

      <section v-if="result?.evidences?.length" class="evidence-section">
        <header><b>Evidence</b><span>{{ result.evidences.length }} 条 · 按最终排序</span></header>
        <article v-for="item in result.evidences" :key="item.evidence_id" class="evidence-card">
          <div class="evidence-head"><span class="rank">{{ String(item.metadata?.rank || 0).padStart(2, '0') }}</span><b>{{ item.file_name }}</b><span v-if="item.page" class="page-label">第 {{ item.page }} 页</span><button type="button" @click="copyEvidenceId(item.evidence_id)">{{ item.evidence_id }}</button></div>
          <p>{{ item.text }}</p>
          <div class="scores"><span>BM25 {{ score(item.scores?.bm25) }}</span><span>Vector {{ score(item.scores?.vector) }}</span><span>Fusion {{ score(item.scores?.fusion) }}</span><span v-if="item.scores?.rerank !== null">Rerank {{ score(item.scores?.rerank) }}</span></div>
        </article>
      </section>
      <div v-else class="empty-result">没有召回证据。检查当前知识库是否已成功索引。</div>

      <section class="context-section"><header><b>Final Context</b><span>{{ result?.usage?.context_tokens || 0 }} tokens</span></header><pre>{{ result?.final_context || '（空）' }}</pre></section>

      <details class="raw-debug"><summary>查看各阶段排名、计时和配置快照</summary><pre>{{ JSON.stringify({ stages: debugResult.stages, timing: debugResult.timing, config_snapshot: debugResult.config_snapshot }, null, 2) }}</pre></details>
    </template>
    <div v-else class="retrieval-empty"><el-icon><Search /></el-icon><b>输入问题后运行检索</b><span>该页面使用当前知识库的真实索引；模型未配置时会显示 BM25-only。</span></div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { retrieveKnowledgeBase } from '@/api/geoAiStudio.js'

const props = defineProps({ knowledgeBase: { type: Object, required: true } })
const query = ref('')
const loading = ref(false)
const debugResult = ref(null)
const retrievalConfig = computed(() => props.knowledgeBase.config?.retrieval || {})
const searchMode = ref(retrievalConfig.value.search_mode || 'hybrid')
const finalTopK = ref(retrievalConfig.value.final_top_k || 8)
const recallTopK = ref(retrievalConfig.value.recall_top_k || 50)
const bm25TopK = ref(retrievalConfig.value.bm25_top_k || 50)
const similarityThreshold = ref(retrievalConfig.value.similarity_threshold || 0)
const bm25DropRatio = ref(retrievalConfig.value.bm25_drop_ratio_search || 0)
const useReranker = ref(Boolean(retrievalConfig.value.use_reranker))
const useGraphRetrieval = ref(Boolean(retrievalConfig.value.use_graph_retrieval))
const result = computed(() => debugResult.value?.result)
const modeLabel = computed(() => props.knowledgeBase.kb_type === 'dify'
  ? 'Dify Dataset' : (props.knowledgeBase.config?.embedding?.model || 'BM25 only'))

const run = async () => {
  if (!query.value.trim()) return
  loading.value = true
  try {
    debugResult.value = await retrieveKnowledgeBase(props.knowledgeBase.id, {
      query: query.value.trim(),
      search_mode: searchMode.value,
      final_top_k: finalTopK.value,
      recall_top_k: recallTopK.value,
      bm25_top_k: bm25TopK.value,
      similarity_threshold: similarityThreshold.value,
      bm25_drop_ratio_search: bm25DropRatio.value,
      use_reranker: useReranker.value,
      use_graph_retrieval: useGraphRetrieval.value,
      vector_weight: retrievalConfig.value.vector_weight,
      bm25_weight: retrievalConfig.value.bm25_weight,
    })
  } catch (error) {
    debugResult.value = null
    ElMessage.error(error.message || '检索失败')
  } finally {
    loading.value = false
  }
}

const stageLabel = (value) => ({ query: 'Query', bm25: 'BM25', vector: 'Vector', fusion: 'Fusion', graph: 'Graph', rerank: 'Rerank', evidence: 'Evidence', final_context: 'Final Context', dify_retrieve: 'Dify Retrieve', notion_search: 'Notion Search' })[value] || value
const score = (value) => value === null || value === undefined ? '—' : Number(value).toFixed(4)
const copyEvidenceId = async (value) => {
  await navigator.clipboard.writeText(value)
  ElMessage.success('Evidence ID 已复制')
}

watch(() => props.knowledgeBase.id, () => { debugResult.value = null })
watch(() => props.knowledgeBase.config?.retrieval, (config) => {
  if (!config) return
  searchMode.value = config.search_mode || 'hybrid'
  finalTopK.value = config.final_top_k || 8
  recallTopK.value = config.recall_top_k || 50
  bm25TopK.value = config.bm25_top_k || 50
  similarityThreshold.value = config.similarity_threshold || 0
  bm25DropRatio.value = config.bm25_drop_ratio_search || 0
  useReranker.value = Boolean(config.use_reranker)
  useGraphRetrieval.value = Boolean(config.use_graph_retrieval)
})
</script>

<style lang="less" scoped>
.retrieval-page {
  padding: 18px 20px 24px;
}

.query-panel,
.evidence-section,
.context-section {
  border: 1px solid #e5ebf2;
  border-radius: 8px;
  background: #ffffff;
}

.query-panel {
  padding: 16px;
}

.panel-heading,
.evidence-section header,
.context-section header,
.evidence-head,
.scores {
  display: flex;
  align-items: center;
}

.panel-heading {
  justify-content: space-between;
  margin-bottom: 12px;

  b {
    font-size: 16px;
  }

  p {
    margin: 4px 0 0;
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
  }
}

.query-options {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  margin-top: 14px;
  flex-wrap: wrap;

  label {
    display: flex;
    flex-direction: column;
    gap: 6px;
    color: var(--gg-text-secondary, #617185);
    font-size: 12px;
  }

  :deep(.el-select),
  :deep(.el-input-number) {
    width: 140px;
  }
}

.query-options :deep(.el-checkbox) {
  margin: 0 0 9px;
}

.query-options .el-button {
  margin-left: auto;
}

.result-metrics {
  display: flex;
  gap: 10px;
  margin: 14px 0;
  flex-wrap: wrap;

  > div {
    display: flex;
    min-width: 130px;
    padding: 10px 13px;
    flex-direction: column;
    border: 1px solid #e7ecf2;
    border-radius: 8px;
    background: #fafcff;
  }

  b {
    color: var(--gg-text-strong, #383c41);
    font-size: 16px;
  }

  span {
    margin-top: 4px;
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
  }
}

.pipeline-status {
  display: flex;
  gap: 8px;
  margin: 14px 0;
  overflow-x: auto;
}

.pipeline-stage {
  display: flex;
  min-width: 155px;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e8edf2;
  border-radius: 8px;
  background: #ffffff;

  b,
  small {
    display: block;
  }

  b {
    font-size: 12px;
  }

  small {
    margin-top: 4px;
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
    line-height: 16px;
  }
}

.stage-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  margin-top: 3px;
  border-radius: 50%;
  background: #a6acb8;
}

.stage-completed .stage-dot {
  background: #34a46f;
}

.stage-skipped .stage-dot {
  background: #ff922c;
}

.evidence-section,
.context-section {
  margin-top: 14px;
  padding: 14px;

  header {
    justify-content: space-between;
    margin-bottom: 10px;

    b {
      font-size: 16px;
    }

    span {
      color: var(--gg-text-muted, #9096a2);
      font-size: 12px;
    }
  }
}

.evidence-card {
  padding: 12px 0;
  border-top: 1px solid #edf0f3;

  p {
    margin: 10px 0;
    color: var(--gg-text-strong, #383c41);
    font-size: 14px;
    line-height: 21px;
    white-space: pre-wrap;
  }
}

.evidence-head {
  gap: 9px;
  flex-wrap: wrap;

  b {
    font-size: 14px;
  }

  button {
    margin-left: auto;
    padding: 3px 7px;
    border: 0;
    border-radius: 4px;
    background: #f0f5fa;
    color: #617185;
    font-size: 12px;
    cursor: pointer;
  }
}

.rank {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 5px;
  background: #eff6ff;
  color: #007bff;
  font-size: 12px;
  font-weight: 700;
}

.page-label,
.scores span {
  padding: 3px 7px;
  border-radius: 4px;
  background: #f4f6f8;
  color: var(--gg-text-secondary, #617185);
  font-size: 12px;
}

.scores {
  gap: 7px;
  flex-wrap: wrap;
}

.context-section pre,
.raw-debug pre {
  max-height: 350px;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border-radius: 6px;
  background: #f7f9fc;
  color: #383c41;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
}

.raw-debug {
  margin-top: 14px;
  color: #617185;
  font-size: 12px;

  summary {
    padding: 10px 0;
    cursor: pointer;
  }
}

.empty-result,
.retrieval-empty {
  padding: 36px 14px;
  color: var(--gg-text-muted, #9096a2);
  text-align: center;
  font-size: 14px;
}

.retrieval-empty {
  display: flex;
  min-height: 300px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;

  .el-icon {
    color: #1685a5;
    font-size: 28px;
  }

  b {
    color: var(--gg-text-primary, #222527);
  }
}
</style>
