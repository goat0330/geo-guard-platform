<template>
  <section class="workspace-card retrieval-debugger">
    <header class="card-heading">
      <div>
        <span class="section-kicker">RETRIEVAL DEBUG</span>
        <h2>检索调试</h2>
      </div>
      <span v-if="result?.retrieval?.mode" class="mode-pill">{{ modeLabel }}</span>
    </header>

    <form class="query-form" @submit.prevent="runRetrieval">
      <label for="retrieval-query">检索问题</label>
      <el-input
        id="retrieval-query"
        v-model="query"
        type="textarea"
        :rows="3"
        resize="vertical"
        placeholder="例如：调查记录中的稳定性描述与历史台账不一致时，应核对哪些依据？"
      />
      <div class="query-actions">
        <label class="top-k-control"
          >返回证据
          <el-select v-model="topK" size="small" aria-label="返回证据数量">
            <el-option v-for="value in [3, 5, 8, 10]" :key="value" :label="`${value} 条`" :value="value" />
          </el-select>
        </label>
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="!query.trim()">
          <el-icon><Search /></el-icon>运行检索
        </el-button>
      </div>
    </form>

    <div v-if="debugData" class="retrieval-result">
      <div class="result-summary">
        <div>
          <b>{{ evidenceList.length }}</b
          ><span>条最终证据</span>
        </div>
        <div>
          <b>{{ result?.retrieval?.candidate_count ?? 0 }}</b
          ><span>个候选文本块</span>
        </div>
        <div>
          <b>{{ result?.timing_ms?.total ?? '—' }}<small> ms</small></b
          ><span>检索耗时</span>
        </div>
      </div>

      <div class="trace-strip" aria-label="检索执行状态">
        <div
          v-for="stage in debugData.pipeline || []"
          :key="stage.stage"
          class="trace-stage"
          :class="`state-${stage.status}`"
        >
          <span class="trace-marker"
            ><el-icon><component :is="stage.status === 'completed' ? CircleCheckFilled : Clock" /></el-icon
          ></span>
          <span>{{ stageName(stage.stage) }}</span>
        </div>
      </div>

      <div v-if="evidenceList.length" class="evidence-list">
        <article v-for="item in evidenceList" :key="item.evidence_id" class="evidence-card">
          <header>
            <span class="evidence-rank">{{ String(item.metadata?.rank || 0).padStart(2, '0') }}</span>
            <b>{{ item.file_name }}</b>
            <span v-if="item.page" class="page-tag">第 {{ item.page }} 页</span>
          </header>
          <p>{{ item.text }}</p>
          <footer>
            <span>{{ item.evidence_id }}</span>
            <span>BM25 {{ score(item.scores?.bm25) }}</span>
            <span v-if="item.scores?.vector !== null">Vector {{ score(item.scores?.vector) }}</span>
            <span class="fusion-score"
              ><i :style="{ width: `${scoreWidth(item.scores?.fusion)}%` }"></i>Fusion
              {{ score(item.scores?.fusion) }}</span
            >
          </footer>
        </article>
      </div>
      <div v-else class="no-evidence">
        <el-icon><DocumentDelete /></el-icon>
        <b>没有召回证据</b>
        <span>{{ result?.retrieval?.message || '检查资料是否已入库，或调整检索问题。' }}</span>
      </div>
    </div>
    <div v-else class="debug-empty">
      <div class="empty-orbit"><span></span><span></span><span></span></div>
      <b>等待一次检索</b>
      <span>运行后可查看召回状态、证据来源与排序分数。</span>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Clock, DocumentDelete, Search } from '@element-plus/icons-vue'
import { debugRagRetrieval } from '@/api/geoAiStudio.js'

defineOptions({ name: 'RetrievalDebugger' })

const query = ref('')
const topK = ref(5)
const loading = ref(false)
const debugData = ref(null)
const result = computed(() => debugData.value?.result)
const evidenceList = computed(() => (Array.isArray(result.value?.evidences) ? result.value.evidences : []))
const modeLabel = computed(() => (result.value?.retrieval?.mode === 'hybrid' ? 'BM25 + Vector' : 'BM25 ONLY'))

const stageName = (stage) =>
  ({
    query: '问题',
    bm25: 'BM25',
    vector: 'Vector',
    fusion: '融合',
    rerank: 'Rerank',
    final_context: '证据',
  })[stage] || stage

const score = (value) => (value === null || value === undefined ? '—' : Number(value).toFixed(3))
const scoreWidth = (value) => Math.max(0, Math.min(100, Number(value || 0) * 100))

const runRetrieval = async () => {
  if (!query.value.trim()) return
  loading.value = true
  try {
    debugData.value = await debugRagRetrieval(query.value.trim(), topK.value)
  } catch (error) {
    debugData.value = null
    ElMessage.error(error.message || '检索失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="less" scoped src="./RetrievalDebugger.less"></style>
