<template>
  <el-dialog v-model="dialogVisible" title="配置知识库" width="min(820px, 94vw)" destroy-on-close>
    <input ref="parserFileInput" class="hidden-input" type="file" accept=".pdf,application/pdf" @change="testParserFile" />
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础信息" name="basic">
        <el-form label-position="top">
          <el-form-item label="知识库名称"><el-input v-model.trim="form.name" maxlength="120" /></el-form-item>
          <el-form-item label="知识库描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
          <template v-if="knowledgeBase.kb_type === 'local'">
            <div class="subheading">PDF 解析</div>
            <div class="two-columns">
              <el-form-item label="解析器">
                <el-select v-model="form.parser.engine">
                  <el-option label="自动（已配置 MinerU 时使用）" value="auto" />
                  <el-option label="PyMuPDF Layout" value="pymupdf-layout" />
                  <el-option label="MinerU OCR / Layout" value="mineru" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.parser.engine === 'mineru'" label="MinerU API URL">
                <el-input v-model.trim="form.parser.mineru_api_uri" placeholder="http://127.0.0.1:8000" />
              </el-form-item>
            </div>
            <el-button plain :loading="testingParser" @click="parserFileInput?.click()">选 PDF 实测解析</el-button>
            <p class="readonly-note">会把所选 PDF 上传到当前配置的 MinerU 服务。未配置或服务不可达会明确报错。</p>
            <div class="subheading">Embedding 模型</div>
            <el-form-item label="OpenAI-Compatible Base URL"><el-input v-model.trim="form.embedding.base_url" placeholder="https://api.siliconflow.cn/v1" /></el-form-item>
            <div class="two-columns">
              <el-form-item label="模型名称"><el-input v-model.trim="form.embedding.model" placeholder="BAAI/bge-m3" /></el-form-item>
              <el-form-item label="向量维度"><el-input-number v-model="form.embedding.dimensions" :min="1" :controls="false" placeholder="服务端返回" /></el-form-item>
            </div>
            <el-form-item label="API Key">
              <el-input v-model="form.embedding.api_key" type="password" show-password autocomplete="new-password" :placeholder="embeddingKeySet ? '已有密钥，留空保持不变' : '输入服务商 API Key'" />
              <small v-if="embeddingKeySet">本地服务已保存密钥；接口不会回传密钥明文。</small>
            </el-form-item>
            <el-button plain :loading="testingEmbedding" @click="testEmbedding">实测 Embedding API</el-button>
            <p v-if="embeddingResult" class="readonly-note">{{ embeddingResult.provider }} · {{ embeddingResult.model }} · {{ embeddingResult.dimensions }} 维 · {{ embeddingResult.latency_ms }} ms</p>
            <div class="two-columns">
              <el-form-item label="分块策略">
                <el-select v-model="form.chunking.chunk_preset_id">
                  <el-option v-for="preset in chunkPresets" :key="preset.value" :label="preset.label + (preset.experimental ? '（实验）' : '')" :value="preset.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="Chunk Token 数（64–4096）"><el-input-number v-model="form.chunking.chunk_token_num" :min="64" :max="4096" :step="64" /></el-form-item>
            </div>
            <div class="two-columns">
              <el-form-item label="Overlap（0–80%）"><el-input-number v-model="form.chunking.overlapped_percent" :min="0" :max="80" /></el-form-item>
              <el-form-item label="分隔符"><el-input v-model="form.chunking.delimiter" placeholder="换行符" /></el-form-item>
            </div>
            <div class="subheading">Reranker</div>
            <el-form-item label="Rerank API URL"><el-input v-model.trim="form.reranker.base_url" placeholder="https://.../rerank" /></el-form-item>
            <div class="two-columns">
              <el-form-item label="Rerank 模型"><el-input v-model.trim="form.reranker.model" /></el-form-item>
              <el-form-item label="协议"><el-select v-model="form.reranker.protocol"><el-option label="OpenAI-like" value="openai" /><el-option label="DashScope" value="dashscope" /></el-select></el-form-item>
            </div>
            <el-form-item label="Reranker API Key"><el-input v-model="form.reranker.api_key" type="password" show-password autocomplete="new-password" :placeholder="rerankerKeySet ? '已有密钥，留空保持不变' : '可选'" /></el-form-item>
            <el-button plain :loading="testingReranker" @click="testReranker">实测 Reranker API</el-button>
            <p v-if="rerankerResult" class="readonly-note">{{ rerankerResult.provider }} · {{ rerankerResult.model }} · {{ rerankerResult.document_count }} 篇输入 · {{ rerankerResult.scores?.length }} 个对齐分数 · {{ rerankerResult.latency_ms }} ms</p>
          </template>
          <template v-else-if="knowledgeBase.kb_type === 'dify'">
            <div class="subheading">Dify Dataset 连接</div>
            <el-form-item label="Dify API URL"><el-input v-model.trim="form.dify.dify_api_url" placeholder="http://127.0.0.1/v1" /></el-form-item>
            <el-form-item label="Dify Token"><el-input v-model="form.dify.dify_token" type="password" show-password autocomplete="new-password" :placeholder="difyTokenSet ? '已有 Token，留空保持不变' : 'Dataset API Token'" /></el-form-item>
            <el-form-item label="Dataset ID"><el-input v-model.trim="form.dify.dify_dataset_id" /></el-form-item>
            <p class="readonly-note">Dify 知识库只读。文件解析与切块仍由 Dify 自己执行。</p>
          </template>
          <template v-else>
            <div class="subheading">Notion Workspace 连接</div>
            <el-form-item label="Notion API URL"><el-input v-model.trim="form.notion.base_url" placeholder="https://api.notion.com/v1" /></el-form-item>
            <el-form-item label="Internal Integration Token"><el-input v-model="form.notion.token" type="password" show-password autocomplete="new-password" :placeholder="notionTokenSet ? '已有 Token，留空保持不变' : '输入 Integration Token'" /></el-form-item>
            <el-form-item label="Notion-Version"><el-input v-model.trim="form.notion.version" placeholder="2022-06-28" /></el-form-item>
            <p class="readonly-note">只读连接。搜索前请先在 Notion 中将目标页面共享给该 Integration。</p>
            <p v-if="notionTokenSet" class="readonly-note">已保存 Integration Token。可使用窗口底部“保存并测试连接”验证配置。</p>
          </template>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="检索配置" name="retrieval">
        <el-form label-position="top">
          <div class="two-columns">
            <el-form-item label="检索模式"><el-select v-model="form.retrieval.search_mode"><el-option label="混合检索" value="hybrid" /><el-option label="关键词 BM25" value="keyword" /><el-option label="向量检索" value="vector" /></el-select></el-form-item>
            <el-form-item label="最终返回 top_k（1–100）"><el-input-number v-model="form.retrieval.final_top_k" :min="1" :max="100" /></el-form-item>
          </div>
          <div class="two-columns">
            <el-form-item label="召回 recall_top_k（1–200）"><el-input-number v-model="form.retrieval.recall_top_k" :min="1" :max="200" /></el-form-item>
            <el-form-item label="BM25 候选数 bm25_top_k（1–200）"><el-input-number v-model="form.retrieval.bm25_top_k" :min="1" :max="200" /></el-form-item>
          </div>
          <div class="two-columns">
            <el-form-item label="相似度阈值（0–1）"><el-input-number v-model="form.retrieval.similarity_threshold" :min="0" :max="1" :step="0.05" /></el-form-item>
            <el-form-item label="BM25 稀疏项丢弃比例（0–1）"><el-input-number v-model="form.retrieval.bm25_drop_ratio_search" :min="0" :max="1" :step="0.05" /></el-form-item>
          </div>
          <div class="two-columns">
            <el-form-item label="使用 Reranker"><el-switch v-model="form.retrieval.use_reranker" /></el-form-item>
            <el-form-item label="图谱召回"><el-switch v-model="form.retrieval.use_graph_retrieval" /></el-form-item>
          </div>
          <div class="two-columns">
            <el-form-item label="Vector 权重（0–1）"><el-input-number v-model="form.retrieval.vector_weight" :min="0" :max="1" :step="0.05" /></el-form-item>
            <el-form-item label="BM25 权重（0–1）"><el-input-number v-model="form.retrieval.bm25_weight" :min="0" :max="1" :step="0.05" /></el-form-item>
          </div>
          <p class="readonly-note">两种权重不能同时为 0。未配置 Embedding 时，混合模式会真实降级为 BM25-only。图谱召回使用已构建的本地关键词关系图。</p>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="权限配置" name="permissions">
        <div class="permission-note"><el-icon><Lock /></el-icon><div><b>本机学习工作台</b><p>当前 RAG Studio 使用本地 SQLite 与本地文件目录，未配置用户/部门权限系统。</p></div></div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="settings-footer">
        <el-button :loading="saving" @click="save(true)">保存并测试连接</el-button>
        <div><el-button @click="dialogVisible = false">关闭</el-button><el-button type="primary" :loading="saving" @click="save(false)">保存</el-button></div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { getChunkPresets, testEmbeddingProvider, testKnowledgeBaseConnection, testParserProvider, testRerankerProvider, updateKnowledgeBase } from '@/api/geoAiStudio.js'

const props = defineProps({ modelValue: Boolean, knowledgeBase: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue', 'updated'])
const dialogVisible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const activeTab = ref('basic')
const saving = ref(false)
const chunkPresets = ref([])
const embeddingKeySet = ref(false)
const rerankerKeySet = ref(false)
const difyTokenSet = ref(false)
const notionTokenSet = ref(false)
const parserFileInput = ref(null)
const testingParser = ref(false)
const testingEmbedding = ref(false)
const testingReranker = ref(false)
const embeddingResult = ref(null)
const rerankerResult = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return {
    name: '', description: '',
    parser: { engine: 'auto', mineru_api_uri: '' },
    embedding: { base_url: '', model: '', api_key: '', dimensions: null, batch_size: 32 },
    chunking: { chunk_preset_id: 'general', chunk_token_num: 512, overlapped_percent: 10, delimiter: '\\n' },
    retrieval: { search_mode: 'hybrid', recall_top_k: 50, bm25_top_k: 50, final_top_k: 8, similarity_threshold: 0, bm25_drop_ratio_search: 0, vector_weight: 0.7, bm25_weight: 0.3, use_reranker: false, use_graph_retrieval: false },
    reranker: { base_url: '', model: '', api_key: '', protocol: 'openai' },
    dify: { dify_api_url: '', dify_token: '', dify_dataset_id: '' },
    notion: { base_url: 'https://api.notion.com/v1', token: '', version: '2022-06-28' },
  }
}

const fillForm = () => {
  const config = props.knowledgeBase.config || {}
  const blank = emptyForm()
  form.value = {
    ...blank,
    name: props.knowledgeBase.name,
    description: props.knowledgeBase.description || '',
    parser: { ...blank.parser, ...(config.parser || {}) },
    embedding: { ...blank.embedding, ...(config.embedding || {}), api_key: '' },
    chunking: { ...blank.chunking, ...(config.chunking || {}), delimiter: config.chunking?.delimiter || '\\n' },
    retrieval: { ...blank.retrieval, ...(config.retrieval || {}) },
    reranker: { ...blank.reranker, ...(config.reranker || {}), api_key: '' },
    dify: { ...blank.dify, ...(config.dify || {}), dify_token: '' },
    notion: { ...blank.notion, ...(config.notion || {}), token: '' },
  }
  embeddingKeySet.value = Boolean(config.embedding?.api_key_set)
  rerankerKeySet.value = Boolean(config.reranker?.api_key_set)
  difyTokenSet.value = Boolean(config.dify?.dify_token_set || config.dify?.token_set)
  notionTokenSet.value = Boolean(config.notion?.token_set)
  embeddingResult.value = null
  rerankerResult.value = null
}

const save = async (testAfterSave) => {
  saving.value = true
  try {
    const payload = { name: form.value.name, description: form.value.description }
    payload.config = props.knowledgeBase.kb_type === 'dify'
      ? { dify: { ...form.value.dify } }
      : props.knowledgeBase.kb_type === 'notion'
        ? { notion: { ...form.value.notion } }
        : {
          parser: { ...form.value.parser },
          embedding: { ...form.value.embedding },
          chunking: { ...form.value.chunking },
          retrieval: { ...form.value.retrieval },
          reranker: { ...form.value.reranker },
        }
    const updated = await updateKnowledgeBase(props.knowledgeBase.id, payload)
    embeddingKeySet.value = Boolean(updated.config?.embedding?.api_key_set)
    rerankerKeySet.value = Boolean(updated.config?.reranker?.api_key_set)
    difyTokenSet.value = Boolean(updated.config?.dify?.dify_token_set)
    notionTokenSet.value = Boolean(updated.config?.notion?.token_set)
    emit('updated', updated)
    ElMessage.success('知识库配置已保存')
    if (testAfterSave) {
      const test = await testKnowledgeBaseConnection(props.knowledgeBase.id)
      ElMessage.success(`${test.provider} 连接正常${test.evidence_count !== undefined ? `，返回 ${test.evidence_count} 条证据` : ''}`)
    }
  } catch (error) {
    ElMessage.error(error.message || (testAfterSave ? '保存或连接测试失败' : '保存配置失败'))
  } finally {
    saving.value = false
  }
}

const testEmbedding = async () => {
  testingEmbedding.value = true
  try {
    embeddingResult.value = await testEmbeddingProvider(form.value.embedding)
    ElMessage.success('Embedding API 实测通过')
  } catch (error) {
    embeddingResult.value = null
    ElMessage.error(error.message || 'Embedding 测试失败')
  } finally { testingEmbedding.value = false }
}

const testReranker = async () => {
  testingReranker.value = true
  try {
    rerankerResult.value = await testRerankerProvider({
      ...form.value.reranker,
      query: '滑坡隐患稳定性复核需要核对哪些现场依据？',
      documents: ['核查坡体裂缝、变形、排水和威胁对象。', '今天天气晴朗，适合户外活动。'],
    })
    if (rerankerResult.value.scores?.length !== 2) throw new Error('Reranker 返回分数与输入文档数不一致')
    ElMessage.success('Reranker API 实测通过，分数与输入对齐')
  } catch (error) {
    rerankerResult.value = null
    ElMessage.error(error.message || 'Reranker 测试失败')
  } finally { testingReranker.value = false }
}

const testParserFile = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  testingParser.value = true
  try {
    const result = await testParserProvider(file, form.value.parser.mineru_api_uri)
    ElMessage.success(`MinerU 解析完成：${result.pages} 页、${result.blocks} 个内容块、${result.bbox_count} 个 bbox`)
  } catch (error) {
    ElMessage.error(error.message || 'MinerU PDF 测试失败')
  } finally { testingParser.value = false }
}

watch(() => props.modelValue, async (visible) => {
  if (!visible) return
  fillForm()
  try {
    const result = await getChunkPresets()
    chunkPresets.value = result?.chunk_presets || []
  } catch (error) {
    ElMessage.error(error.message || '读取分块策略失败')
  }
})
</script>

<style lang="less" scoped>
.hidden-input { display: none; }

.subheading {
  margin: 12px 0 4px;
  color: var(--gg-text-strong, #383c41);
  font-size: 16px;
  font-weight: 700;
}

.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.two-columns :deep(.el-input-number),
.two-columns :deep(.el-select) {
  width: 100%;
}

.settings-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.settings-footer > div {
  display: flex;
  gap: 8px;
}

.readonly-note,
.el-form-item small {
  color: var(--gg-text-muted, #9096a2);
  font-size: 12px;
  line-height: 18px;
}

.permission-note {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #e6edf3;
  border-radius: 8px;
  background: #f8fbfe;

  .el-icon {
    color: #1685a5;
    font-size: 20px;
  }

  b {
    font-size: 14px;
  }

  p {
    margin: 6px 0 0;
    color: var(--gg-text-secondary, #617185);
    font-size: 14px;
  }
}

@media (max-width: 640px) {
  .two-columns {
    grid-template-columns: 1fr;
  }

  .settings-footer {
    flex-direction: column;
  }

  .settings-footer > div {
    justify-content: flex-end;
  }
}
</style>
