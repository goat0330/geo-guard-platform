<template>
  <div class="evaluation-page">
    <section class="dataset-panel">
      <header><div><b>评估数据集</b><p>上传 JSON 或 CSV，数据集与每次运行结果都会保存在本地 RAG 服务中。</p></div><input ref="datasetInput" class="hidden-input" type="file" accept=".json,.csv,application/json,text/csv" @change="uploadDataset" /><el-button :loading="datasetLoading" @click="datasetInput?.click()">上传评估集</el-button></header>
      <div class="dataset-runner">
        <el-select v-model="selectedDatasetId" placeholder="选择已保存的数据集" clearable>
          <el-option v-for="dataset in datasets" :key="dataset.id" :label="`${dataset.name} · ${dataset.case_count} 条`" :value="dataset.id" />
        </el-select>
        <el-button type="primary" :loading="datasetLoading" :disabled="!selectedDatasetId" @click="runDataset">运行数据集</el-button>
        <el-button :disabled="!selectedDatasetId" @click="deleteSelectedDataset">删除数据集</el-button>
      </div>
      <el-table v-if="runs.length" :data="runs" size="small" class="runs-table" @row-click="showRun">
        <el-table-column prop="created_at" label="运行时间" min-width="200"><template #default="{ row }">{{ new Date(row.created_at).toLocaleString() }}</template></el-table-column>
        <el-table-column prop="dataset_id" label="数据集 ID" min-width="170"><template #default="{ row }">{{ row.dataset_id || '手动单例' }}</template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click.stop="showRun(row)">查看结果</el-button></template></el-table-column>
      </el-table>
    </section>

    <section class="eval-form">
      <div class="eval-heading"><div><b>RAG 检索评估</b><p>填写人工标注的相关 Evidence ID，计算 Recall@K、Precision@K、MRR 和 nDCG@K。</p></div><el-tag type="info">Ground Truth IDs</el-tag></div>
      <el-form label-position="top">
        <el-form-item label="测试问题"><el-input v-model="query" type="textarea" :rows="2" placeholder="请输入一个真实检索问题" /></el-form-item>
        <el-form-item label="相关 Evidence IDs（逗号或换行分隔）"><el-input v-model="evidenceIds" type="textarea" :rows="2" placeholder="EV-CHK-..." /></el-form-item>
        <div class="eval-options">
          <label>检索模式<el-select v-model="searchMode"><el-option label="混合检索" value="hybrid" /><el-option label="BM25" value="keyword" /><el-option label="Vector" value="vector" /></el-select></label>
          <label>Top K<el-input-number v-model="topK" :min="1" :max="100" /></label>
          <el-button type="primary" :loading="loading" :disabled="!query.trim() || !parsedEvidenceIds.length" @click="run">运行评估</el-button>
        </div>
      </el-form>
    </section>

    <section v-if="result" class="eval-result">
      <header><b>评估结果</b><span>{{ result.case_count }} 个样本 · {{ result.judge }}</span></header>
      <div class="metric-grid">
        <div v-for="metric in metrics" :key="metric.key"><b>{{ Number(result.averages?.[metric.key] || 0).toFixed(4) }}</b><span>{{ metric.label }}</span></div>
      </div>
      <div v-for="item in result.cases || []" :key="item.query" class="case-result">
        <b>{{ item.query }}</b>
        <span>Recall@{{ item.top_k }} {{ item.recall_at_k }} · Precision@{{ item.top_k }} {{ item.precision_at_k }} · MRR {{ item.mrr }} · nDCG {{ item.ndcg_at_k }}</span>
        <small>标注：{{ item.relevant_evidence_ids.join('、') || '—' }}</small>
        <small>返回：{{ item.returned_evidence_ids.join('、') || '—' }}</small>
      </div>
    </section>
    <div v-else class="eval-empty"><el-icon><DataAnalysis /></el-icon><b>等待评估样本</b><span>从“检索测试”复制 Evidence ID，再填写人工相关性标注。</span></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis } from '@element-plus/icons-vue'
import { deleteKnowledgeBaseEvaluationDataset, evaluateKnowledgeBase, getKnowledgeBaseEvaluationDatasets, getKnowledgeBaseEvaluationRuns, getKnowledgeBaseEvaluationRun, runKnowledgeBaseEvaluation, uploadKnowledgeBaseEvaluationDataset } from '@/api/geoAiStudio.js'

const props = defineProps({ knowledgeBase: { type: Object, required: true } })
const query = ref('')
const evidenceIds = ref('')
const searchMode = ref(props.knowledgeBase.config?.retrieval?.search_mode || 'hybrid')
const topK = ref(props.knowledgeBase.config?.retrieval?.final_top_k || 8)
const loading = ref(false)
const result = ref(null)
const parsedEvidenceIds = computed(() => evidenceIds.value.split(/[\n,，;；]/).map((value) => value.trim()).filter(Boolean))
const datasetInput = ref(null)
const datasetLoading = ref(false)
const datasets = ref([])
const runs = ref([])
const selectedDatasetId = ref('')
const metrics = [
  { key: 'recall_at_k', label: '平均 Recall@K' },
  { key: 'precision_at_k', label: '平均 Precision@K' },
  { key: 'mrr', label: '平均 MRR' },
  { key: 'ndcg_at_k', label: '平均 nDCG@K' },
]

const loadPersistentData = async () => {
  try {
    const [datasetResult, runResult] = await Promise.all([
      getKnowledgeBaseEvaluationDatasets(props.knowledgeBase.id),
      getKnowledgeBaseEvaluationRuns(props.knowledgeBase.id),
    ])
    datasets.value = Array.isArray(datasetResult) ? datasetResult : []
    runs.value = Array.isArray(runResult) ? runResult : []
    if (selectedDatasetId.value && !datasets.value.some((item) => item.id === selectedDatasetId.value)) selectedDatasetId.value = ''
  } catch (error) { ElMessage.error(error.message || '读取评估数据集失败') }
}

const uploadDataset = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  datasetLoading.value = true
  try {
    const dataset = await uploadKnowledgeBaseEvaluationDataset(props.knowledgeBase.id, file)
    selectedDatasetId.value = dataset.id
    await loadPersistentData()
    ElMessage.success(`已保存评估集：${dataset.case_count ?? dataset.cases?.length ?? 0} 条样本`)
  } catch (error) { ElMessage.error(error.message || '上传评估集失败') }
  finally { datasetLoading.value = false }
}

const runDataset = async () => {
  if (!selectedDatasetId.value) return
  datasetLoading.value = true
  try {
    const run = await runKnowledgeBaseEvaluation(props.knowledgeBase.id, {
      dataset_id: selectedDatasetId.value,
      search_mode: searchMode.value,
    })
    result.value = run.result
    await loadPersistentData()
    ElMessage.success(`评估完成：${run.result?.case_count || 0} 条样本`)
  } catch (error) { ElMessage.error(error.message || '评估运行失败') }
  finally { datasetLoading.value = false }
}

const showRun = async (row) => {
  try {
    const run = await getKnowledgeBaseEvaluationRun(props.knowledgeBase.id, row.id)
    result.value = run.result
  } catch (error) { ElMessage.error(error.message || '读取评估结果失败') }
}

const deleteSelectedDataset = async () => {
  const id = selectedDatasetId.value
  if (!id) return
  try {
    await deleteKnowledgeBaseEvaluationDataset(props.knowledgeBase.id, id)
    selectedDatasetId.value = ''
    await loadPersistentData()
    ElMessage.success('数据集已删除')
  } catch (error) { ElMessage.error(error.message || '删除数据集失败') }
}

const run = async () => {
  loading.value = true
  try {
    result.value = await evaluateKnowledgeBase(props.knowledgeBase.id, {
      search_mode: searchMode.value,
      cases: [{ query: query.value.trim(), relevant_evidence_ids: parsedEvidenceIds.value, top_k: topK.value }],
    })
  } catch (error) {
    result.value = null
    ElMessage.error(error.message || '评估失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadPersistentData)
watch(() => props.knowledgeBase.id, loadPersistentData)
</script>

<style lang="less" scoped>
.evaluation-page {
  padding: 18px 20px 24px;
}

.dataset-panel {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #e5ebf2;
  border-radius: 8px;
  background: #fff;

  header,
  .dataset-runner {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  header {
    justify-content: space-between;
    margin-bottom: 12px;
  }

  header p { margin: 5px 0 0; color: var(--gg-text-muted, #9096a2); font-size: 12px; }
  .dataset-runner :deep(.el-select) { width: min(420px, 100%); }
  .runs-table { margin-top: 12px; cursor: pointer; }
}

.hidden-input { display: none; }

.eval-form,
.eval-result {
  padding: 16px;
  border: 1px solid #e5ebf2;
  border-radius: 8px;
  background: #ffffff;
}

.eval-heading,
.eval-result header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  b {
    font-size: 16px;
  }

  p {
    margin: 5px 0 0;
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
  }
}

.eval-options {
  display: flex;
  align-items: flex-end;
  gap: 14px;

  label {
    display: flex;
    flex-direction: column;
    gap: 6px;
    color: var(--gg-text-secondary, #617185);
    font-size: 12px;
  }

  :deep(.el-select),
  :deep(.el-input-number) {
    width: 150px;
  }

  .el-button {
    margin-left: auto;
  }
}

.eval-result {
  margin-top: 14px;

  header span {
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;

  > div {
    padding: 12px;
    border-radius: 8px;
    background: #f7f9fc;
  }

  b,
  span {
    display: block;
  }

  b {
    color: #007bff;
    font-size: 18px;
  }

  span {
    margin-top: 5px;
    color: var(--gg-text-secondary, #617185);
    font-size: 12px;
  }
}

.case-result {
  display: flex;
  margin-top: 14px;
  padding: 12px 0 0;
  flex-direction: column;
  gap: 6px;
  border-top: 1px solid #edf0f3;
  font-size: 14px;

  span,
  small {
    color: var(--gg-text-secondary, #617185);
    font-size: 12px;
  }
}

.eval-empty {
  display: flex;
  min-height: 280px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: var(--gg-text-muted, #9096a2);
  font-size: 14px;

  .el-icon {
    color: #1685a5;
    font-size: 30px;
  }

  b {
    color: var(--gg-text-primary, #222527);
  }
}

@media (max-width: 720px) {
  .eval-options {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
