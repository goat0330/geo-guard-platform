<template>
  <section class="graph-page">
    <header class="graph-heading">
      <div><b>知识图谱</b><p>从已索引文本提取关键词，并按真实文件建立共现关系。</p></div>
      <div class="graph-actions">
        <el-tag :type="graph.status === 'indexed' ? 'success' : 'info'">{{ graph.status === 'indexed' ? `已构建 · ${graph.nodes?.length || 0} 节点` : '尚未构建' }}</el-tag>
        <el-input-number v-model="keywordsPerDocument" :min="1" :max="50" title="每份文档最多关键词数" />
        <el-button type="primary" :loading="building" :disabled="knowledgeBase.kb_type !== 'local'" @click="build">构建 / 刷新</el-button>
        <el-button v-if="graph.status === 'indexed'" :loading="building" @click="reset">清除</el-button>
      </div>
    </header>
    <div v-if="knowledgeBase.kb_type !== 'local'" class="graph-empty"><el-icon><Connection /></el-icon><b>{{ knowledgeBase.kb_type === 'dify' ? 'Dify Dataset' : 'Notion' }} 连接器不提供本地图谱</b><span>此连接只支持其原生检索 API。</span></div>
    <div v-else-if="loading" class="graph-empty">正在读取已保存的图谱数据…</div>
    <div v-else-if="graph.status !== 'indexed'" class="graph-empty"><el-icon><Share /></el-icon><b>图谱尚未构建</b><span>构建后展示关键词节点、来源文件和实际共现边。</span></div>
    <div v-else>
      <div ref="chartElement" class="graph-chart"></div>
      <div class="graph-meta">
        <el-tag size="small" type="primary">{{ graph.generation || 'local-keyword-cooccurrence' }}</el-tag>
        <span>{{ graph.source_document_ids?.length || 0 }} 份来源文件</span>
        <span>{{ graph.edges?.length || 0 }} 条真实关系</span>
        <span>生成于 {{ graph.created_at ? new Date(graph.created_at).toLocaleString() : '—' }}</span>
      </div>
      <p class="graph-note">{{ graph.note || '关键词和连接来自当前已索引 Chunk；不是 LLM 生成的实体关系三元组。' }}</p>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Share } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { buildKnowledgeBaseGraph, configureKnowledgeBaseGraph, getKnowledgeBaseGraph, getKnowledgeBaseGraphStatus, resetKnowledgeBaseGraph } from '@/api/geoAiStudio.js'

const props = defineProps({ knowledgeBase: { type: Object, required: true } })
const graph = ref({ status: 'not_built', nodes: [], edges: [] })
const chartElement = ref(null)
const loading = ref(false)
const building = ref(false)
const keywordsPerDocument = ref(props.knowledgeBase.config?.graph?.max_keywords_per_document || 12)
let chart

const render = async () => {
  if (!chartElement.value || graph.value.status !== 'indexed') return
  await nextTick()
  chart ||= echarts.init(chartElement.value)
  const colors = { document: '#1885a5', keyword: '#51a36f' }
  chart.setOption({
    tooltip: { trigger: 'item', formatter: (item) => item.dataType === 'edge' ? `${item.data.relation || 'contains'} · 权重 ${item.data.weight}` : item.data.label },
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true, data: graph.value.nodes.map((node) => ({
        ...node, name: node.label, symbolSize: node.type === 'document' ? 42 : Math.min(32, 16 + (node.document_count || 1) * 3),
        itemStyle: { color: colors[node.type] || '#8b98a8' }, label: { show: true, position: 'right', formatter: '{b}', fontSize: 12 },
      })),
      links: graph.value.edges.map((edge) => ({ ...edge, lineStyle: { width: Math.max(1, Math.min(5, Math.log2((edge.weight || 1) + 1))), opacity: 0.55 } })),
      force: { repulsion: 190, edgeLength: 90 }, emphasis: { focus: 'adjacency', lineStyle: { width: 4 } },
    }],
  })
  chart.resize()
}

const load = async () => {
  if (props.knowledgeBase.kb_type !== 'local') return
  loading.value = true
  try {
    const [status, saved] = await Promise.all([
      getKnowledgeBaseGraphStatus(props.knowledgeBase.id),
      getKnowledgeBaseGraph(props.knowledgeBase.id),
    ])
    graph.value = { ...saved, ...status, status: saved.status === 'ready' ? 'indexed' : status.status }
    await render()
  } catch (error) { ElMessage.error(error.message || '读取图谱失败') }
  finally { loading.value = false }
}

const build = async () => {
  building.value = true
  try {
    await configureKnowledgeBaseGraph(props.knowledgeBase.id, { max_keywords_per_document: keywordsPerDocument.value })
    await buildKnowledgeBaseGraph(props.knowledgeBase.id)
    await load()
    ElMessage.success('已根据当前已索引资料构建关键词关系图')
  } catch (error) { ElMessage.error(error.message || '构建图谱失败') }
  finally { building.value = false }
}

const reset = async () => {
  building.value = true
  try {
    await resetKnowledgeBaseGraph(props.knowledgeBase.id)
    graph.value = { status: 'not_built', nodes: [], edges: [] }
    ElMessage.success('已清除已保存图谱')
  } catch (error) { ElMessage.error(error.message || '清除图谱失败') }
  finally { building.value = false }
}

const resize = () => chart?.resize()
watch(() => props.knowledgeBase.id, () => { graph.value = { status: 'not_built', nodes: [], edges: [] }; load() })
onMounted(() => { load(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); chart?.dispose(); chart = null })
</script>

<style lang="less" scoped>
.graph-page { padding: 18px 20px 24px; }
.graph-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.graph-heading b { font-size: 16px; }
.graph-heading p { margin: 5px 0 0; color: var(--gg-text-muted, #9096a2); font-size: 12px; }
.graph-actions { display: flex; align-items: center; gap: 8px; }
.graph-chart { width: 100%; height: min(64vh, 620px); min-height: 380px; border: 1px solid #e8edf2; border-radius: 8px; background: #fbfcfe; }
.graph-meta { display: flex; align-items: center; gap: 14px; margin-top: 12px; color: var(--gg-text-muted, #9096a2); font-size: 12px; }
.graph-note { color: var(--gg-text-muted, #9096a2); font-size: 12px; line-height: 1.6; }
.graph-empty { display: flex; min-height: 350px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--gg-text-muted, #9096a2); font-size: 14px; text-align: center; }
.graph-empty .el-icon { color: #1685a5; font-size: 30px; }
.graph-empty b { color: var(--gg-text-primary, #222527); font-size: 16px; }
@media (max-width: 760px) { .graph-heading { align-items: flex-start; flex-direction: column; } .graph-actions { flex-wrap: wrap; } }
</style>
