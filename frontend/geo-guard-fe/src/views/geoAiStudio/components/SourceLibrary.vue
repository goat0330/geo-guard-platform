<template>
  <section class="workspace-card source-library">
    <header class="card-heading">
      <div>
        <span class="section-kicker">SOURCE LIBRARY</span>
        <h2>知识资料</h2>
      </div>
      <button class="icon-button" type="button" title="刷新资料" :disabled="loading" @click="loadDocuments">
        <el-icon><Refresh /></el-icon>
      </button>
    </header>

    <input
      ref="fileInput"
      class="file-input"
      type="file"
      multiple
      accept=".pdf,.txt,.md,.csv,.json"
      @change="handleFileChange"
    />
    <button
      class="upload-zone"
      :class="{ 'is-dragging': dragging, 'is-uploading': uploading }"
      type="button"
      :disabled="uploading"
      @click="fileInput?.click()"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <span class="upload-symbol"
        ><el-icon><UploadFilled /></el-icon
      ></span>
      <span class="upload-copy">
        <b>{{ uploading ? `正在处理 ${uploadProgress}` : '拖入文件，或点击选择' }}</b>
        <small>PDF · TXT · MD · CSV · JSON · 可多选</small>
      </span>
      <span class="upload-arrow"
        ><el-icon><ArrowRight /></el-icon
      ></span>
    </button>
    <p class="parser-note">PDF 使用文本层解析；扫描件和图片 OCR 暂未接入。</p>

    <div class="source-list-head">
      <div>
        <h3>已入库资料</h3>
        <span>{{ documents.length }} 份资料 · {{ totalChunks }} 个文本块</span>
      </div>
      <span class="local-badge">LOCAL</span>
    </div>

    <div v-if="loading" class="list-state">正在读取本地资料…</div>
    <div v-else-if="documents.length" class="document-list">
      <article v-for="doc in documents" :key="doc.document_id" class="document-row">
        <span class="file-kind" :class="fileKind(doc.file_name)">{{ fileExt(doc.file_name) }}</span>
        <div class="document-copy">
          <b :title="doc.file_name">{{ doc.file_name }}</b>
          <small
            >{{ doc.chunk_count }} 个文本块<span v-if="doc.metadata?.pages_total">
              · {{ doc.metadata.pages_total }} 页</span
            ></small
          >
        </div>
        <button class="text-button" type="button" @click="openChunks(doc)">查看切块</button>
      </article>
    </div>
    <div v-else class="list-state empty-state">
      <el-icon><Document /></el-icon>
      <b>知识库还没有资料</b>
      <span>添加 PDF 或文本文件后，这里会显示实际解析结果。</span>
    </div>

    <details class="paste-source">
      <summary>
        <el-icon><EditPen /></el-icon><span>粘贴一段文本入库</span><small>TEXT SOURCE</small>
      </summary>
      <div class="paste-form">
        <el-input v-model.trim="textTitle" placeholder="资料名称" maxlength="160" />
        <el-input
          v-model="textContent"
          type="textarea"
          :rows="4"
          resize="vertical"
          placeholder="粘贴规范条文、调查说明或其他纯文本资料"
        />
        <el-button type="primary" :loading="addingText" @click="addText">加入知识库</el-button>
      </div>
    </details>

    <el-dialog
      v-model="chunksVisible"
      class="chunk-dialog"
      :title="selectedDocument?.file_name || '资料切块'"
      width="min(760px, 92vw)"
    >
      <div class="chunk-dialog-meta">展示前 {{ chunks.length }} 个文本块，原文页码保留在来源标签中。</div>
      <div v-if="chunksLoading" class="list-state">正在读取切块…</div>
      <div v-else class="chunk-list">
        <article v-for="chunk in chunks" :key="chunk.chunk_id" class="chunk-card">
          <header>
            <b>CHUNK {{ String(chunk.ordinal + 1).padStart(3, '0') }}</b
            ><span>{{ chunk.page ? `第 ${chunk.page} 页` : '文本来源' }}</span>
          </header>
          <p>{{ chunk.text }}</p>
        </article>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Document, EditPen, Refresh, UploadFilled } from '@element-plus/icons-vue'
import { addRagText, getDocumentChunks, getDocuments, uploadRagFile } from '@/api/geoAiStudio.js'

defineOptions({ name: 'SourceLibrary' })

const fileInput = ref(null)
const documents = ref([])
const chunks = ref([])
const selectedDocument = ref(null)
const loading = ref(false)
const uploading = ref(false)
const addingText = ref(false)
const chunksLoading = ref(false)
const chunksVisible = ref(false)
const dragging = ref(false)
const uploadProgress = ref('')
const textTitle = ref('')
const textContent = ref('')
const totalChunks = computed(() => documents.value.reduce((sum, doc) => sum + Number(doc.chunk_count || 0), 0))

const loadDocuments = async () => {
  loading.value = true
  try {
    const data = await getDocuments()
    documents.value = Array.isArray(data?.documents) ? data.documents : []
  } catch (error) {
    documents.value = []
    ElMessage.error(error.message || '读取资料失败')
  } finally {
    loading.value = false
  }
}

const processFiles = async (files) => {
  if (!files.length) return
  uploading.value = true
  let succeeded = 0
  const errors = []
  for (const [index, file] of files.entries()) {
    uploadProgress.value = `${index + 1}/${files.length}`
    try {
      await uploadRagFile(file)
      succeeded += 1
    } catch (error) {
      errors.push(`${file.name}：${error.message}`)
    }
  }
  uploading.value = false
  uploadProgress.value = ''
  await loadDocuments()
  if (succeeded) ElMessage.success(`已入库 ${succeeded} 份资料`)
  if (errors.length) ElMessage.warning(errors.slice(0, 2).join('；'))
}

const handleFileChange = async (event) => {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  await processFiles(files)
}

const handleDrop = async (event) => {
  dragging.value = false
  await processFiles(Array.from(event.dataTransfer?.files || []))
}

const addText = async () => {
  if (!textTitle.value || !textContent.value.trim()) {
    ElMessage.warning('请填写资料名称和文本内容')
    return
  }
  addingText.value = true
  try {
    await addRagText(textTitle.value, textContent.value)
    textTitle.value = ''
    textContent.value = ''
    await loadDocuments()
    ElMessage.success('文本资料已入库')
  } catch (error) {
    ElMessage.error(error.message || '文本入库失败')
  } finally {
    addingText.value = false
  }
}

const openChunks = async (doc) => {
  selectedDocument.value = doc
  chunksVisible.value = true
  chunksLoading.value = true
  try {
    const data = await getDocumentChunks(doc.document_id)
    chunks.value = Array.isArray(data?.chunks) ? data.chunks : []
  } catch (error) {
    chunks.value = []
    ElMessage.error(error.message || '读取切块失败')
  } finally {
    chunksLoading.value = false
  }
}

const fileExt = (name = '') => name.split('.').pop()?.toUpperCase() || 'DOC'
const fileKind = (name = '') => `kind-${name.split('.').pop()?.toLowerCase() || 'doc'}`

onMounted(loadDocuments)
</script>

<style lang="less" scoped src="./SourceLibrary.less"></style>
