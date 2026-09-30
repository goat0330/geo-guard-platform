<template>
  <div class="workflow-workspace">
    <LangGraphStudioLauncher :workflow="workflow" :trace="trace" :running="running" />

    <div class="run-column">
      <section class="workflow-card run-card">
        <header class="run-heading">
          <div>
            <span class="section-kicker">HAZARD REVIEW RUN</span>
            <h2>试跑一次隐患复核</h2>
          </div>
          <span class="review-only">建议草稿 · 人工确认</span>
        </header>

        <label class="field-label" for="review-question">复核问题 <i>必填</i></label>
        <el-input
          id="review-question"
          v-model="query"
          type="textarea"
          :rows="3"
          resize="vertical"
          placeholder="描述本次需要核对或复核的问题"
          :disabled="running"
        />

        <label class="field-label hazard-id-label" for="hazard-id">业务隐患点 ID <span>可选；当前本地流程不连接业务库，请用下方 JSON 提供记录</span></label>
        <el-input id="hazard-id" v-model="hazardId" placeholder="隐患点标识（用于本次检索和候选匹配）" :disabled="running" />

        <div class="record-fields">
          <div class="record-field">
            <label class="field-label" for="current-record">调查记录 <span>可选 JSON</span></label>
            <el-input
              id="current-record"
              v-model="currentRecordText"
              type="textarea"
              :rows="5"
              resize="vertical"
              placeholder="粘贴调查记录 JSON 对象；字段按现有业务数据填写"
              :disabled="running"
            />
          </div>
          <div class="record-field">
            <label class="field-label" for="historical-record">历史台账 <span>可选 JSON</span></label>
            <el-input
              id="historical-record"
              v-model="historicalRecordText"
              type="textarea"
              :rows="5"
              resize="vertical"
              placeholder="粘贴历史台账 JSON 对象；字段按现有业务数据填写"
              :disabled="running"
            />
          </div>
        </div>

        <div class="form-footnote">
          <span class="footnote-mark">i</span>
          <span>只提供调查记录时不执行台账差异对比。当前流程不会调用正式确认或台账写回接口。</span>
        </div>

        <div class="run-actions">
          <span v-if="runError" class="run-error">{{ runError }}</span>
          <span v-else class="run-hint">业务 Trace 只展示节点与状态，不包含模型隐藏思维链。</span>
          <el-button type="primary" :loading="running" :disabled="!query.trim()" @click="runWorkflow">
            <el-icon v-if="!running"><Promotion /></el-icon>
            {{ running ? '正在运行' : '运行复核流程' }}
          </el-button>
        </div>
      </section>

      <section v-if="trace.length || agentResult" class="workflow-card execution-card">
        <header class="execution-heading">
          <div>
            <span class="section-kicker">LIVE BUSINESS TRACE</span>
            <h2>执行记录</h2>
          </div>
          <span class="trace-count">{{ trace.length }} 个节点事件</span>
        </header>

        <ol class="trace-list">
          <li v-for="(item, index) in trace" :key="`${item.stage}-${index}`">
            <span class="trace-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <b>{{ traceTitle(item.stage) }}</b>
              <p>{{ item.summary }}</p>
            </div>
            <span class="trace-status" :class="`status-${item.status}`">{{ traceStatus(item.status) }}</span>
          </li>
        </ol>

        <div v-if="agentResult" class="draft-result">
          <div class="draft-banner">
            <el-icon><WarningFilled /></el-icon>
            <span>复核建议草稿</span>
            <b>待人工确认</b>
          </div>
          <p class="draft-answer">{{ structuredResult.recommendation || '流程完成，但没有返回复核建议。' }}</p>
          <p class="result-status">风险等级：{{ structuredResult.risk_classification?.level || 'unknown' }} · {{ structuredResult.review_status || '待人工确认' }}</p>

          <div v-if="comparison.length" class="comparison-result">
            <h3>
              字段差异 <span>{{ comparison.length }}</span>
            </h3>
            <div class="comparison-head"><span></span><span>调查记录</span><span>历史台账</span></div>
            <div v-for="item in comparison" :key="item.field" class="comparison-row">
              <b>{{ item.field }}</b>
              <span>{{ displayValue(item.current, item.current_present) }}</span>
              <span>{{ displayValue(item.historical, item.historical_present) }}</span>
            </div>
          </div>

          <div v-if="evidence.length" class="workflow-evidence">
            <h3>
              引用证据 <span>{{ evidence.length }}</span>
            </h3>
            <article v-for="item in evidence" :key="item.evidence_id">
              <header>
                <b>{{ item.file_name }}</b
                ><span>{{ item.page ? `第 ${item.page} 页` : item.evidence_id }}</span>
              </header>
              <p>{{ item.text }}</p>
              <small>{{ item.evidence_id }}</small>
            </article>
          </div>
          <details class="structured-json">
            <summary>查看 Structured JSON</summary>
            <pre>{{ JSON.stringify(structuredResult, null, 2) }}</pre>
          </details>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, WarningFilled } from '@element-plus/icons-vue'
import { getWorkflows, runAgentStream } from '@/api/geoAiStudio.js'
import LangGraphStudioLauncher from './LangGraphStudioLauncher.vue'

defineOptions({ name: 'AgentWorkflowWorkspace' })

const workflows = ref([])
const trace = ref([])
const agentResult = ref(null)
const running = ref(false)
const query = ref('')
const currentRecordText = ref('')
const historicalRecordText = ref('')
const runError = ref('')
const hazardId = ref('')
const workflow = computed(() => workflows.value.find((item) => item.id === 'hazard_review') || null)
const comparison = computed(() =>
  Array.isArray(agentResult.value?.structured_result?.field_comparison)
    ? agentResult.value.structured_result.field_comparison
    : [],
)
const structuredResult = computed(() => agentResult.value?.structured_result || {})
const evidence = computed(() => (Array.isArray(agentResult.value?.evidence) ? agentResult.value.evidence : []))

const parseRecord = (text, label) => {
  if (!text.trim()) return null
  let value
  try {
    value = JSON.parse(text)
  } catch {
    throw new Error(`${label}不是有效 JSON`)
  }
  if (!value || Array.isArray(value) || typeof value !== 'object') {
    throw new Error(`${label}需要是 JSON 对象`)
  }
  return value
}

const runWorkflow = async () => {
  if (!query.value.trim() || running.value) return
  let currentRecord
  let historicalRecord
  try {
    currentRecord = parseRecord(currentRecordText.value, '调查记录')
    historicalRecord = parseRecord(historicalRecordText.value, '历史台账')
  } catch (error) {
    runError.value = error.message
    return
  }

  runError.value = ''
  trace.value = []
  agentResult.value = null
  running.value = true
  try {
    agentResult.value = await runAgentStream(
      {
        workflow: 'hazard_review',
        query: query.value.trim(),
        hazard_input: { hazard_id: hazardId.value.trim() || undefined },
        context: { current_record: currentRecord, historical_record: historicalRecord },
      },
      (event) => trace.value.push(event),
    )
    if (!agentResult.value) throw new Error('流程没有返回最终结果')
  } catch (error) {
    runError.value = error.message || '流程运行失败'
    ElMessage.error(runError.value)
  } finally {
    running.value = false
  }
}

const traceTitle = (stage) => workflow.value?.nodes?.find((node) => node.id === stage)?.title || stage
const traceStatus = (status) => ({ completed: '已完成', running: '执行中', skipped: '已跳过', failed: '失败' })[status] || status
const displayValue = (value, present) =>
  !present ? '未提供' : typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '空值')

onMounted(async () => {
  try {
    const data = await getWorkflows()
    workflows.value = Array.isArray(data?.workflows) ? data.workflows : []
  } catch {
    workflows.value = []
  }
})
</script>

<style lang="less" scoped src="./AgentWorkflowWorkspace.less"></style>
