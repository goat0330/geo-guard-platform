<template>
  <section class="files-panel">
    <template v-if="knowledgeBase.kb_type === 'local'">
      <input ref="fileInput" class="hidden-input" type="file" multiple accept=".pdf,.txt,.md,.csv,.json" @change="handleFileSelect" />
      <input ref="folderInput" class="hidden-input" type="file" multiple webkitdirectory directory @change="handleFolderSelect" />
      <div class="file-toolbar">
        <el-dropdown @command="handleAction">
          <el-button type="primary"><el-icon><Upload /></el-icon>上传<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="files"><el-icon><DocumentAdd /></el-icon>上传文件</el-dropdown-item>
              <el-dropdown-item command="folder"><el-icon><FolderAdd /></el-icon>上传文件夹</el-dropdown-item>
              <el-dropdown-item command="new-folder"><el-icon><Folder /></el-icon>新建文件夹</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="toolbar-right">
          <el-select v-model="mindMapDocumentId" class="mindmap-select" placeholder="选择文件生成思维导图" clearable>
          <el-option v-for="document in indexedDocuments" :key="document.id" :label="document.file_name" :value="document.id" />
          </el-select>
          <el-button :disabled="!mindMapDocumentId" @click="openMindMap"><el-icon><Share /></el-icon>思维导图</el-button>
          <el-input v-model="search" class="file-search" placeholder="搜索文件" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button text circle :loading="loading" title="刷新" @click="load"><el-icon><Refresh /></el-icon></el-button>
        </div>
      </div>

      <div class="file-stats">
        <span>当前目录：<b>{{ currentFolderName }}</b></span>
        <span>{{ documents.length }} 文件</span>
        <span>{{ totalChunks }} Chunks</span>
        <span>{{ totalTokens }} Tokens</span>
        <span v-if="documents.length" class="quality-note">质量指标基于已解析文本与真实向量状态</span>
      </div>

      <div class="table-tools">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item><button type="button" @click="currentFolderId = null">全部文件</button></el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentFolderId">{{ currentFolderName }}</el-breadcrumb-item>
        </el-breadcrumb>
        <span v-if="uploading" class="upload-progress">正在处理 {{ uploadProgress }}</span>
      </div>

      <div class="table-wrap" @dragover.prevent="dragging = true" @dragleave.prevent="dragging = false" @drop.prevent="handleDrop">
        <div v-if="dragging" class="drop-mask">松开以导入到“{{ currentFolderName }}”</div>
        <el-table v-loading="loading" :data="visibleRows" row-key="id" empty-text="暂无文件">
          <el-table-column label="文件名" min-width="260">
            <template #default="{ row }">
              <div v-if="row.kind === 'folder'" class="resource-name folder-row">
                <button class="folder-name" type="button" @click="currentFolderId = row.id"><el-icon><Folder /></el-icon>{{ row.name }}<span class="row-count">{{ row.child_count }}</span></button>
                <el-dropdown @command="(command) => handleFolderCommand(command, row)"><el-button link><el-icon><MoreFilled /></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="rename">重命名</el-dropdown-item><el-dropdown-item command="delete" divided>删除空文件夹</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
              </div>
              <div v-else class="resource-name">
                <span class="file-extension" :class="`ext-${extension(row.file_name)}`">{{ extension(row.file_name) }}</span>
                <span class="filename" :title="row.file_name">{{ row.file_name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="内容量" width="190">
            <template #default="{ row }">
              <span v-if="row.kind === 'folder'" class="muted">文件夹</span>
              <span v-else>{{ row.chunk_count || 0 }} Chunks · {{ row.token_count || 0 }} Tokens</span>
            </template>
          </el-table-column>
          <el-table-column label="创建人" width="125"><template #default="{ row }">{{ row.kind === 'folder' ? '本地用户' : '本地导入' }}</template></el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag v-if="row.kind === 'folder'" size="small" effect="plain" type="info">目录</el-tag>
              <el-tooltip v-else-if="row.status === 'failed' && row.error_message" :content="row.error_message" placement="top">
                <el-tag size="small" :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
              </el-tooltip>
              <el-tag v-else-if="row.kind !== 'folder'" size="small" :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="175"><template #default="{ row }">{{ formatTime(row.created_at) }}</template></el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div v-if="row.kind !== 'folder'" class="row-actions">
                <el-button link type="primary" @click="openChunks(row)">查看分块</el-button>
                <el-button v-if="row.status === 'uploaded' || row.status === 'failed'" link type="warning" @click="parseDocument(row)">重新解析</el-button>
                <el-button v-else-if="row.status === 'parsed'" link type="success" @click="indexDocument(row)">构建索引</el-button>
                <el-button v-else-if="row.status === 'indexed'" link type="success" @click="indexDocument(row)">重建索引</el-button>
                <el-button link type="danger" @click="removeDocument(row)">删除</el-button>
              </div>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="selectedDocument && selectedChunks.length" class="quality-strip">
        <b>{{ selectedDocument.file_name }} 切块质量</b>
        <span>{{ selectedChunks.length }} chunks</span>
        <span>平均 {{ averageCharacters }} 字符</span>
        <span>超出配置上限 {{ oversizedChunks }} 块</span>
        <span>已向量化 {{ selectedDocument.embedding_chunk_count || 0 }}/{{ selectedDocument.chunk_count || 0 }}</span>
      </div>
    </template>
    <div v-else class="readonly-state">
      <el-icon><Connection /></el-icon>
      <strong>{{ knowledgeBase.kb_type === 'dify' ? 'Dify Dataset 为只读知识库' : 'Notion Workspace 为只读连接' }}</strong>
      <p>{{ knowledgeBase.kb_type === 'dify' ? '文件与分块由 Dify 管理。请在“检索测试”运行真实查询，或在“配置”中更新连接信息。' : '页面由 Notion 管理；请在“检索测试”按真实 Notion API 搜索。' }}</p>
    </div>

    <el-dialog v-model="folderDialogVisible" title="新建文件夹" width="min(460px, 92vw)">
      <el-input v-model.trim="newFolderName" maxlength="120" placeholder="文件夹名称" @keyup.enter="createFolder" />
      <template #footer><el-button @click="folderDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingFolder" @click="createFolder">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="chunksVisible" :title="selectedDocument?.file_name || '文件分块'" width="min(1160px, 96vw)" class="chunks-dialog">
      <div class="chunk-layout">
        <div class="chunk-column">
          <div class="quality-strip compact">
            <span>{{ selectedChunks.length }} chunks</span><span>已向量化 {{ selectedDocument?.embedding_chunk_count || 0 }}</span><span>平均 {{ averageCharacters }} 字符</span>
          </div>
          <el-scrollbar height="min(62vh, 650px)">
            <button v-for="chunk in selectedChunks" :key="chunk.id" type="button" class="chunk-card" :class="{ selected: activeChunk?.id === chunk.id }" @click="activeChunk = chunk">
              <div><b>CHUNK {{ String(chunk.chunk_index + 1).padStart(3, '0') }}</b><span>{{ chunk.token_count }} tokens</span><span>{{ chunk.source_spans?.[0]?.page ? `第 ${chunk.source_spans[0].page} 页` : '文本' }}</span></div>
              <p>{{ chunk.text }}</p>
            </button>
          </el-scrollbar>
        </div>
        <div v-if="activeSpan && selectedDocument?.mime_type === 'application/pdf'" class="pdf-preview">
          <div class="preview-heading"><b>原文定位</b><span>第 {{ activeSpan.page }} 页</span></div>
          <div class="pdf-frame">
            <img :src="pageImageUrl(selectedDocument, activeSpan.page)" alt="PDF 原文页面" />
            <span v-if="bboxStyle(activeSpan)" class="bbox-highlight" :style="bboxStyle(activeSpan)"></span>
          </div>
          <p v-if="!activeSpan.bbox">该证据保留了页码，但当前解析器没有返回有效 bbox。</p>
        </div>
        <div v-else class="pdf-empty"><el-icon><Document /></el-icon><span>选择一个分块查看原文页码与定位框</span></div>
      </div>
    </el-dialog>

    <el-dialog v-model="mindMapVisible" title="思维导图" width="min(820px, 94vw)">
      <div v-if="mindMapLoading" class="mindmap-empty">正在读取实际分块与页码关系…</div>
      <div v-else-if="mindMapTree" class="mindmap-canvas">
        <div class="mind-root">{{ mindMapTree.label }}</div>
        <div v-for="documentNode in mindMapTree.children || []" :key="documentNode.id" class="mind-document">
          <strong>{{ documentNode.label }}</strong>
          <div v-for="page in documentNode.children || []" :key="page.id" class="mind-page">
            <b>{{ page.label }}</b>
            <ul><li v-for="chunk in page.children || []" :key="chunk.id"><button type="button" @click="openChunks(mindMapDocument, chunk)">{{ chunk.label }} · {{ chunk.text.slice(0, 90) }}{{ chunk.text.length > 90 ? '…' : '' }}</button></li></ul>
          </div>
        </div>
        <p>数据由后端 Mindmap API 从已索引 Chunk 与原文页码生成并持久化；这是来源大纲，不是实体关系图。</p>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Connection, Document, DocumentAdd, Folder, FolderAdd, MoreFilled, Refresh, Search, Share, Upload } from '@element-plus/icons-vue'
import {
  createKnowledgeBaseFolder,
  deleteKnowledgeBaseDocument,
  generateKnowledgeBaseMindmap,
  indexKnowledgeBaseDocuments,
  getKnowledgeBaseChunks,
  getKnowledgeBaseDocuments,
  getKnowledgeBaseFolders,
  parseKnowledgeBaseDocuments,
  renameKnowledgeBaseFolder,
  deleteKnowledgeBaseFolder,
  uploadKnowledgeBaseFile,
} from '@/api/geoAiStudio.js'

const props = defineProps({ knowledgeBase: { type: Object, required: true } })
const emit = defineEmits(['stats'])
const fileInput = ref(null)
const folderInput = ref(null)
const documents = ref([])
const folders = ref([])
const chunksVisible = ref(false)
const folderDialogVisible = ref(false)
const mindMapVisible = ref(false)
const loading = ref(false)
const uploading = ref(false)
const savingFolder = ref(false)
const mindMapLoading = ref(false)
const dragging = ref(false)
const uploadProgress = ref('')
const search = ref('')
const currentFolderId = ref(null)
const newFolderName = ref('')
const selectedDocument = ref(null)
const selectedChunks = ref([])
const activeChunk = ref(null)
const mindMapDocumentId = ref('')
const mindMapDocument = ref(null)
const mindMapTree = ref(null)
const indexedDocuments = computed(() => documents.value.filter((item) => item.status === 'indexed'))

const currentFolderName = computed(() => folders.value.find((item) => item.id === currentFolderId.value)?.name || '根目录')
const totalChunks = computed(() => documents.value.reduce((sum, item) => sum + Number(item.chunk_count || 0), 0))
const totalTokens = computed(() => documents.value.reduce((sum, item) => sum + Number(item.token_count || 0), 0))
const visibleRows = computed(() => {
  const query = search.value.trim().toLowerCase()
  const childFolders = folders.value
    .filter((item) => item.parent_id === currentFolderId.value)
    .map((item) => ({ ...item, kind: 'folder', child_count: documents.value.filter((doc) => doc.folder_id === item.id).length }))
  const childDocuments = documents.value
    .filter((item) => item.folder_id === currentFolderId.value)
    .filter((item) => !query || `${item.file_name} ${item.metadata?.relative_path || ''}`.toLowerCase().includes(query))
    .map((item) => ({ ...item, kind: 'document' }))
  return [...childFolders, ...childDocuments]
})
const averageCharacters = computed(() => selectedChunks.value.length
  ? Math.round(selectedChunks.value.reduce((sum, item) => sum + item.text.length, 0) / selectedChunks.value.length)
  : 0)
const oversizedChunks = computed(() => {
  const limit = props.knowledgeBase.config?.chunking?.chunk_token_num || 512
  return selectedChunks.value.filter((item) => item.token_count > limit).length
})
const activeSpan = computed(() => activeChunk.value?.source_spans?.find((span) => span.page) || null)
const load = async () => {
  loading.value = true
  try {
    const [docResult, folderResult] = await Promise.all([
      getKnowledgeBaseDocuments(props.knowledgeBase.id),
      getKnowledgeBaseFolders(props.knowledgeBase.id),
    ])
    documents.value = Array.isArray(docResult) ? docResult : []
    folders.value = Array.isArray(folderResult) ? folderResult : []
    emit('stats', {
      document_count: documents.value.length,
      chunk_count: totalChunks.value,
      token_count: totalTokens.value,
    })
  } catch (error) {
    ElMessage.error(error.message || '读取文件失败')
  } finally {
    loading.value = false
  }
}

const processFiles = async (files, keepPaths = false) => {
  if (!files.length) return
  uploading.value = true
  const failures = []
  let indexed = 0
  let accepted = 0
  for (const [index, file] of files.entries()) {
    uploadProgress.value = `${index + 1}/${files.length}`
    try {
      const relativePath = keepPaths ? (file.webkitRelativePath || file.name) : ''
      const result = await uploadKnowledgeBaseFile(props.knowledgeBase.id, file, {
        folderId: currentFolderId.value,
        relativePath,
      })
      accepted += 1
      if (result.status === 'indexed') indexed += 1
      else if (result.status === 'failed') failures.push(`${file.name}：${result.error_message || '处理失败'}`)
    } catch (error) {
      failures.push(`${file.name}：${error.message}`)
    }
  }
  uploading.value = false
  uploadProgress.value = ''
  await load()
  if (indexed) ElMessage.success(`已完成解析与索引 ${indexed} 份资料`)
  else if (accepted && !failures.length) ElMessage.info(`${accepted} 份资料已上传，处理状态请查看列表`)
  if (failures.length) ElMessage.warning(failures.slice(0, 3).join('；'))
}

const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  await processFiles(files)
}
const handleFolderSelect = async (event) => {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  await processFiles(files, true)
}
const handleDrop = async (event) => {
  dragging.value = false
  await processFiles(Array.from(event.dataTransfer?.files || []))
}
const handleAction = (action) => {
  if (action === 'files') fileInput.value?.click()
  if (action === 'folder') folderInput.value?.click()
  if (action === 'new-folder') {
    newFolderName.value = ''
    folderDialogVisible.value = true
  }
}

const createFolder = async () => {
  if (!newFolderName.value.trim()) return ElMessage.warning('请输入文件夹名称')
  savingFolder.value = true
  try {
    await createKnowledgeBaseFolder(props.knowledgeBase.id, newFolderName.value.trim(), currentFolderId.value)
    folderDialogVisible.value = false
    await load()
    ElMessage.success('文件夹已创建')
  } catch (error) {
    ElMessage.error(error.message || '创建文件夹失败')
  } finally {
    savingFolder.value = false
  }
}

const handleFolderCommand = async (command, folder) => {
  if (command === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('请输入新的文件夹名称', '重命名文件夹', {
        inputValue: folder.name, inputPattern: /.+/, inputErrorMessage: '文件夹名称不能为空',
      })
      await renameKnowledgeBaseFolder(props.knowledgeBase.id, folder.id, value.trim())
      await load()
      ElMessage.success('文件夹已重命名')
    } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '重命名失败') }
  }
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`删除空文件夹“${folder.name}”？`, '删除文件夹', { type: 'warning' })
      await deleteKnowledgeBaseFolder(props.knowledgeBase.id, folder.id)
      await load()
      ElMessage.success('文件夹已删除')
    } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败') }
  }
}

const openChunks = async (document, preferredChunk = null) => {
  selectedDocument.value = document
  chunksVisible.value = true
  selectedChunks.value = []
  activeChunk.value = null
  try {
    const result = await getKnowledgeBaseChunks(props.knowledgeBase.id, document.id)
    selectedChunks.value = Array.isArray(result) ? result : []
    activeChunk.value = preferredChunk ? selectedChunks.value.find((item) => item.id === preferredChunk.id) : selectedChunks.value[0]
  } catch (error) {
    ElMessage.error(error.message || '读取分块失败')
  }
}

const parseDocument = async (document) => {
  try {
    await parseKnowledgeBaseDocuments(props.knowledgeBase.id, [document.id])
    await load()
    ElMessage.success(`已按该知识库解析配置重新处理 ${document.file_name}`)
  } catch (error) { ElMessage.error(error.message || '解析失败') }
}

const indexDocument = async (document) => {
  try {
    await indexKnowledgeBaseDocuments(props.knowledgeBase.id, [document.id])
    await load()
    ElMessage.success(`已按该知识库 Embedding 与分块配置处理 ${document.file_name}`)
  } catch (error) { ElMessage.error(error.message || '索引失败') }
}

const removeDocument = async (document) => {
  try {
    await ElMessageBox.confirm(`删除“${document.file_name}”及其切块和本地文件？`, '删除文件', { type: 'warning' })
    await deleteKnowledgeBaseDocument(props.knowledgeBase.id, document.id)
    await load()
    ElMessage.success('文件已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除失败')
  }
}

const openMindMap = async () => {
  const document = documents.value.find((item) => item.id === mindMapDocumentId.value)
  if (!document) return
  mindMapDocument.value = document
  mindMapVisible.value = true
  mindMapLoading.value = true
  try {
    mindMapTree.value = null
    const result = await generateKnowledgeBaseMindmap(props.knowledgeBase.id, [document.id])
    mindMapTree.value = result.root || null
  } catch (error) {
    mindMapTree.value = null
    ElMessage.error(error.message || '读取思维导图数据失败')
  } finally {
    mindMapLoading.value = false
  }
}

const extension = (name = '') => name.split('.').pop()?.slice(0, 5).toUpperCase() || 'DOC'
const statusLabel = (status) => ({ uploaded: '已上传', parsing: '解析中', parsed: '已解析', chunking: '切块中', embedding: '向量化中', indexed: '已索引', failed: '失败' })[status] || status || '未知'
const statusType = (status) => ({ indexed: 'success', parsed: 'success', parsing: 'warning', chunking: 'warning', embedding: 'warning', failed: 'danger' })[status] || 'info'
const formatTime = (value) => value ? new Date(value).toLocaleString() : '—'
const pageImageUrl = (document, page) => `/geo-ai-rag/api/v1/documents/${encodeURIComponent(document.id)}/page/${page}/image`
const bboxStyle = (span) => {
  if (!span?.bbox || !span.page_width || !span.page_height) return null
  const [x0, y0, x1, y1] = span.bbox
  return {
    left: `${(x0 / span.page_width) * 100}%`,
    top: `${(y0 / span.page_height) * 100}%`,
    width: `${((x1 - x0) / span.page_width) * 100}%`,
    height: `${((y1 - y0) / span.page_height) * 100}%`,
  }
}

watch(() => props.knowledgeBase.id, () => {
  currentFolderId.value = null
  load()
})
onMounted(load)
</script>

<style lang="less" scoped src="./KnowledgeBaseFileManager.less"></style>
