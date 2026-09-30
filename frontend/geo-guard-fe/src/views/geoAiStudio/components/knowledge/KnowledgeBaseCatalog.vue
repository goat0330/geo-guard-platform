<template>
  <div class="catalog">
    <header class="catalog-header">
      <div>
        <h2>知识库</h2>
        <p>管理地灾资料、检索配置与评估任务</p>
      </div>
      <el-button type="primary" @click="createVisible = true">
        <el-icon><Plus /></el-icon>新建知识库
      </el-button>
    </header>

    <div class="catalog-tools">
      <el-input v-model="search" class="search" placeholder="搜索知识库..." clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="typeFilter" class="type-filter" placeholder="全部类型">
        <el-option label="全部类型" value="all" />
        <el-option label="本地向量库" value="local" />
        <el-option label="Dify 只读连接" value="dify" />
        <el-option label="Notion 只读连接" value="notion" />
      </el-select>
      <span class="result-count">{{ filteredItems.length }} 个知识库</span>
    </div>

    <div v-if="loading" class="catalog-state">正在读取知识库…</div>
    <div v-else-if="filteredItems.length" class="catalog-grid">
      <button
        v-for="item in filteredItems"
        :key="item.id"
        type="button"
        class="knowledge-card"
        @click="$emit('open', item)"
      >
        <div class="card-top">
          <span class="database-icon" :class="`type-${item.kb_type}`">
            <el-icon><component :is="item.kb_type === 'dify' ? Connection : Coin" /></el-icon>
          </span>
          <span class="card-more" :title="item.id" @click.stop="copyId(item.id)">
            <el-icon><CopyDocument /></el-icon>
          </span>
        </div>
        <strong>{{ item.name }}</strong>
        <span class="card-subtitle">{{ relativeTime(item.created_at) }} · {{ item.document_count || 0 }} 文件</span>
        <p>{{ item.description || '暂无描述' }}</p>
        <div class="card-tags">
          <el-tag size="small" effect="light" :type="item.kb_type === 'dify' ? 'primary' : 'success'">
            {{ item.kb_type === 'dify' ? 'Dify' : item.kb_type === 'notion' ? 'Notion' : 'Geo RAG 本地库' }}
          </el-tag>
          <el-tag v-if="item.kb_type === 'local'" size="small" type="info" effect="plain">
            {{ item.config?.embedding?.model || 'BM25 only' }}
          </el-tag>
          <el-tag v-if="item.kb_type === 'dify'" size="small" type="info" effect="plain">只读连接</el-tag>
          <el-tag v-if="item.kb_type === 'notion'" size="small" type="info" effect="plain">只读连接</el-tag>
        </div>
      </button>
    </div>
    <div v-else class="catalog-empty">
      <span class="empty-icon"><el-icon><Coin /></el-icon></span>
      <strong>{{ search || typeFilter !== 'all' ? '没有匹配的知识库' : '暂无知识库' }}</strong>
      <p>创建知识库后，可以上传 PDF、文本和结构化资料，并配置检索、图谱和评估。</p>
      <el-button type="primary" @click="createVisible = true">创建知识库</el-button>
    </div>

    <el-dialog v-model="createVisible" width="min(860px, 94vw)" destroy-on-close class="create-dialog">
      <div class="wizard-heading">
        <div class="database-icon"><el-icon><Coin /></el-icon></div>
        <strong>新建知识库</strong>
        <span>{{ step + 1 }}/3 · {{ stepLabels[step] }}</span>
      </div>
      <div class="wizard-body">
        <div class="wizard-steps">
          <span v-for="(label, index) in stepLabels" :key="label" :class="{ active: step === index, done: step > index }">
            <i>{{ index + 1 }}</i>{{ label }}
          </span>
        </div>

        <template v-if="step === 0">
          <label class="field-label">知识库名称 <b>*</b></label>
          <el-input v-model.trim="form.name" maxlength="120" show-word-limit placeholder="例如：地灾隐患复核知识库" />
          <label class="field-label type-label">知识库类型 <b>*</b></label>
          <div class="type-options">
            <button type="button" :class="{ selected: form.kb_type === 'local' }" @click="form.kb_type = 'local'">
              <span class="type-heading"><el-icon><Coin /></el-icon><b>本地向量知识库</b></span>
              <p>Geo RAG 自建解析、切块、BM25 与向量检索，文件保存在本机。</p>
              <small>支持文件 · 支持配置 Embedding</small>
            </button>
            <button type="button" :class="{ selected: form.kb_type === 'dify' }" @click="form.kb_type = 'dify'">
              <span class="type-heading"><el-icon><Connection /></el-icon><b>Dify</b></span>
              <p>连接 Dify Dataset 的只读检索知识库，文档由 Dify 管理。</p>
              <small>只读连接 · 需要 Dataset API 配置</small>
            </button>
            <button type="button" :class="{ selected: form.kb_type === 'notion' }" @click="form.kb_type = 'notion'">
              <span class="type-heading"><el-icon><Link /></el-icon><b>Notion</b></span>
              <p>连接 Notion workspace，按页面标题与正文进行只读搜索。</p>
              <small>只读连接 · 需要 Internal Integration Token</small>
            </button>
          </div>
        </template>

        <template v-else-if="step === 1">
          <div v-if="form.kb_type === 'local'" class="form-grid">
            <div class="form-section">
              <label>PDF 解析方式</label>
              <el-select v-model="form.parser.engine">
                <el-option label="自动（MinerU 已配置时使用）" value="auto" />
                <el-option label="PyMuPDF Layout" value="pymupdf-layout" />
                <el-option label="MinerU OCR / Layout" value="mineru" />
              </el-select>
            </div>
            <div v-if="form.parser.engine === 'mineru'" class="form-section">
              <label>MinerU API URL</label>
              <el-input v-model.trim="form.parser.mineru_api_uri" placeholder="http://127.0.0.1:8000" />
            </div>
            <div class="form-section">
              <label>Embedding API Base URL</label>
              <el-input v-model.trim="form.embedding.base_url" placeholder="https://api.siliconflow.cn/v1" />
            </div>
            <div class="form-section">
              <label>Embedding 模型</label>
              <el-input v-model.trim="form.embedding.model" placeholder="BAAI/bge-m3" />
            </div>
            <div class="form-section span-2">
              <label>API Key</label>
              <el-input v-model="form.embedding.api_key" type="password" show-password autocomplete="new-password" placeholder="可先留空，使用 BM25 检索" />
              <small>密钥仅由本地 RAG 服务保存；不会在读取配置时回传。</small>
            </div>
            <div class="form-section">
              <label>分块策略</label>
              <el-select v-model="form.chunking.chunk_preset_id">
                <el-option v-for="option in chunkPresets" :key="option.value" :value="option.value" :label="option.label">
                  <span>{{ option.label }}</span><small v-if="option.experimental">（实验）</small>
                </el-option>
              </el-select>
            </div>
            <div class="form-section">
              <label>Chunk Token 数（64–4096）</label>
              <el-input-number v-model="form.chunking.chunk_token_num" :min="64" :max="4096" :step="64" />
            </div>
            <div class="form-section">
              <label>重叠比例（0–80%）</label>
              <el-input-number v-model="form.chunking.overlapped_percent" :min="0" :max="80" />
            </div>
          </div>
          <div v-else-if="form.kb_type === 'dify'" class="form-grid">
            <div class="form-section span-2"><label>Dify API URL <b>*</b></label><el-input v-model.trim="form.dify.dify_api_url" placeholder="http://127.0.0.1/v1" /></div>
            <div class="form-section span-2"><label>Dify Token <b>*</b></label><el-input v-model="form.dify.dify_token" type="password" show-password autocomplete="new-password" /></div>
            <div class="form-section span-2"><label>Dataset ID <b>*</b></label><el-input v-model.trim="form.dify.dify_dataset_id" /></div>
          </div>
          <div v-else class="form-grid">
            <div class="form-section span-2"><label>Notion API URL</label><el-input v-model.trim="form.notion.base_url" placeholder="https://api.notion.com/v1" /></div>
            <div class="form-section span-2"><label>Internal Integration Token <b>*</b></label><el-input v-model="form.notion.token" type="password" show-password autocomplete="new-password" /></div>
            <div class="form-section"><label>Notion-Version</label><el-input v-model.trim="form.notion.version" placeholder="2022-06-28" /></div>
            <p class="span-2 notice">先在 Notion 将页面共享给该 Integration。Token 留在本机 RAG 服务配置中，不会回传到浏览器。</p>
          </div>
          <div class="form-section description-field"><label>知识库描述</label><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" placeholder="说明资料内容、适用任务和使用限制" /></div>
        </template>

        <template v-else>
          <div class="summary-card">
            <div class="summary-title"><span class="database-icon"><el-icon><component :is="form.kb_type === 'local' ? Coin : form.kb_type === 'dify' ? Connection : Link" /></el-icon></span><strong>{{ form.name }}</strong><el-tag size="small">{{ form.kb_type === 'local' ? '本地向量知识库' : `${form.kb_type === 'dify' ? 'Dify' : 'Notion'} 只读连接` }}</el-tag></div>
            <div v-if="form.kb_type === 'local'" class="summary-grid">
              <span>Embedding 模型</span><b>{{ form.embedding.model || '未配置 · BM25 only' }}</b>
              <span>分块策略</span><b>{{ form.chunking.chunk_preset_id }}</b>
              <span>Chunk Token</span><b>{{ form.chunking.chunk_token_num }} · overlap {{ form.chunking.overlapped_percent }}%</b>
            </div>
            <div v-else-if="form.kb_type === 'dify'" class="summary-grid"><span>Dataset</span><b>{{ form.dify.dify_dataset_id }}</b><span>API</span><b>{{ form.dify.dify_api_url }}</b></div>
            <div v-else class="summary-grid"><span>Provider</span><b>Notion Search API</b><span>API</span><b>{{ form.notion.base_url || 'https://api.notion.com/v1' }}</b></div>
            <p>{{ form.description || '暂无描述' }}</p>
          </div>
          <div class="notice">创建后可在知识库配置中调整检索 top_k、权重和 Reranker。API Key 不会在页面回显。</div>
        </template>
      </div>
      <template #footer>
        <div class="wizard-footer">
          <span>{{ form.name || (form.kb_type === 'local' ? '本地知识库' : `${form.kb_type === 'dify' ? 'Dify' : 'Notion'} 只读知识库`) }}</span>
          <div>
            <el-button @click="step === 0 ? (createVisible = false) : step--">{{ step === 0 ? '取消' : '上一步' }}</el-button>
            <el-button v-if="step < 2" type="primary" @click="nextStep">下一步</el-button>
            <el-button v-else type="primary" :loading="creating" @click="create">创建知识库</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Connection, CopyDocument, Link, Plus, Search } from '@element-plus/icons-vue'
import { createKnowledgeBase, getChunkPresets } from '@/api/geoAiStudio.js'

const props = defineProps({ items: { type: Array, default: () => [] }, loading: Boolean })
const emit = defineEmits(['open', 'created', 'refresh'])
const search = ref('')
const typeFilter = ref('all')
const createVisible = ref(false)
const creating = ref(false)
const step = ref(0)
const chunkPresets = ref([])
const stepLabels = ['类型', '配置', '确认']
const form = reactive(makeForm())
const filteredItems = computed(() => props.items.filter((item) => {
  const query = search.value.trim().toLowerCase()
  return (!query || `${item.name} ${item.description || ''}`.toLowerCase().includes(query)) &&
    (typeFilter.value === 'all' || item.kb_type === typeFilter.value)
}))

function makeForm() {
  return {
    name: '', description: '', kb_type: 'local',
    parser: { engine: 'auto', mineru_api_uri: '' },
    embedding: { base_url: '', api_key: '', model: '', batch_size: 32 },
    chunking: { chunk_preset_id: 'general', chunk_token_num: 512, overlapped_percent: 10, delimiter: '\\n' },
    dify: { dify_api_url: '', dify_token: '', dify_dataset_id: '' },
    notion: { base_url: 'https://api.notion.com/v1', token: '', version: '2022-06-28' },
  }
}

const nextStep = () => {
  if (step.value === 0 && !form.name.trim()) return ElMessage.warning('请输入知识库名称')
  if (step.value === 1 && form.kb_type === 'dify') {
    if (!form.dify.dify_api_url.trim() || !form.dify.dify_token.trim() || !form.dify.dify_dataset_id.trim()) {
      return ElMessage.warning('请填写 Dify API URL、Token 和 Dataset ID')
    }
    if (!form.dify.dify_api_url.trim().replace(/\/$/, '').endsWith('/v1')) return ElMessage.warning('Dify API URL 需要以 /v1 结尾')
  }
  if (step.value === 1 && form.kb_type === 'notion' && !form.notion.token.trim()) return ElMessage.warning('请填写 Notion Integration Token')
  step.value += 1
}

const create = async () => {
  creating.value = true
  try {
    const config = form.kb_type === 'dify'
      ? { dify: { ...form.dify } }
      : form.kb_type === 'notion'
        ? { notion: { ...form.notion } }
        : { embedding: { ...form.embedding }, chunking: { ...form.chunking }, parser: { ...form.parser } }
    const result = await createKnowledgeBase({
      name: form.name.trim(), description: form.description.trim(), kb_type: form.kb_type, config,
    })
    createVisible.value = false
    step.value = 0
    Object.assign(form, makeForm())
    emit('created', result)
    ElMessage.success('知识库已创建')
  } catch (error) {
    ElMessage.error(error.message || '创建知识库失败')
  } finally {
    creating.value = false
  }
}

const relativeTime = (value) => {
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未知'
  const days = Math.floor((Date.now() - date.getTime()) / 86400000)
  return days <= 0 ? '今天创建' : days === 1 ? '昨天创建' : `${days} 天前创建`
}

const copyId = async (value) => {
  await navigator.clipboard.writeText(value)
  ElMessage.success('已复制知识库 ID')
}

onMounted(async () => {
  try {
    const result = await getChunkPresets()
    chunkPresets.value = result?.chunk_presets || []
  } catch (error) {
    ElMessage.error(error.message || '读取分块策略失败')
  }
})
</script>

<style lang="less" scoped src="./KnowledgeBaseCatalog.less"></style>
