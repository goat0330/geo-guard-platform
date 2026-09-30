<template>
  <div class="knowledge-detail">
    <header class="detail-header">
      <div class="breadcrumbs">
        <button type="button" @click="$emit('back')">知识库</button><span>›</span><strong>{{ knowledgeBase.name }}</strong>
      </div>
      <div class="detail-actions">
        <el-button v-if="knowledgeBase.kb_type === 'dify'" :loading="testing" @click="testConnection">测试连接</el-button>
        <el-button @click="settingsVisible = true"><el-icon><Setting /></el-icon>配置</el-button>
      </div>
    </header>

    <nav class="detail-tabs" aria-label="知识库功能">
      <button v-for="tab in tabs" :key="tab.id" type="button" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
        {{ tab.label }}
      </button>
    </nav>

    <div class="detail-stats">
      <div><el-icon><Document /></el-icon><b>{{ stats.document_count }}</b><span>文件</span></div>
      <div><el-icon><Coin /></el-icon><b>{{ stats.chunk_count }}</b><span>Chunks</span></div>
      <div><el-icon><Coin /></el-icon><b>{{ stats.token_count }}</b><span>Tokens</span></div>
      <span class="storage-tag">{{ knowledgeBase.kb_type === 'dify' ? 'Dify 只读连接' : '本地资料与索引' }}</span>
    </div>

    <KnowledgeBaseFileManager
      v-if="activeTab === 'files'"
      :knowledge-base="knowledgeBase"
      @stats="updateStats"
    />
    <KnowledgeBaseRetrievalTest v-else-if="activeTab === 'retrieval'" :knowledge-base="knowledgeBase" />
    <KnowledgeBaseKnowledgeGraph v-else-if="activeTab === 'graph'" :knowledge-base="knowledgeBase" />
    <KnowledgeBaseEvaluation v-else :knowledge-base="knowledgeBase" />

    <KnowledgeBaseSettingsDialog
      v-model="settingsVisible"
      :knowledge-base="knowledgeBase"
      @updated="$emit('updated', $event)"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Document, Setting } from '@element-plus/icons-vue'
import { testKnowledgeBaseConnection } from '@/api/geoAiStudio.js'
import KnowledgeBaseEvaluation from './KnowledgeBaseEvaluation.vue'
import KnowledgeBaseFileManager from './KnowledgeBaseFileManager.vue'
import KnowledgeBaseKnowledgeGraph from './KnowledgeBaseKnowledgeGraph.vue'
import KnowledgeBaseRetrievalTest from './KnowledgeBaseRetrievalTest.vue'
import KnowledgeBaseSettingsDialog from './KnowledgeBaseSettingsDialog.vue'

const props = defineProps({ knowledgeBase: { type: Object, required: true } })
defineEmits(['back', 'updated', 'deleted'])

const activeTab = ref('files')
const settingsVisible = ref(false)
const testing = ref(false)
const stats = ref({
  document_count: props.knowledgeBase.document_count || 0,
  chunk_count: props.knowledgeBase.chunk_count || 0,
  token_count: props.knowledgeBase.token_count || 0,
})
const tabs = [
  { id: 'files', label: '文件管理' },
  { id: 'retrieval', label: '检索测试' },
  { id: 'graph', label: '知识图谱' },
  { id: 'evaluation', label: '评估' },
]

const testConnection = async () => {
  testing.value = true
  try {
    const result = await testKnowledgeBaseConnection(props.knowledgeBase.id)
    ElMessage.success(`Dify 连接正常，返回 ${result.evidence_count} 条测试证据`)
  } catch (error) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    testing.value = false
  }
}

const updateStats = (newStats) => {
  Object.assign(stats.value, newStats)
}

watch(() => props.knowledgeBase.id, () => {
  stats.value = {
    document_count: props.knowledgeBase.document_count || 0,
    chunk_count: props.knowledgeBase.chunk_count || 0,
    token_count: props.knowledgeBase.token_count || 0,
  }
})
</script>

<style lang="less" scoped>
.knowledge-detail {
  min-height: 680px;
  color: var(--gg-text-primary, #222527);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
}

.breadcrumbs,
.detail-actions,
.detail-stats,
.detail-stats > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.breadcrumbs {
  button {
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--gg-text-secondary, #617185);
    font-size: 14px;
    cursor: pointer;
  }

  span {
    color: #a6acb8;
  }

  strong {
    font-size: 16px;
  }
}

.detail-tabs {
  display: flex;
  justify-content: center;
  gap: 4px;
  padding: 0 20px;
  border-bottom: 1px solid #e8edf2;

  button {
    position: relative;
    padding: 12px 14px;
    border: 0;
    background: transparent;
    color: var(--gg-text-secondary, #617185);
    font-size: 14px;
    cursor: pointer;
  }

  button.active {
    color: var(--gg-primary-500, #007bff);
    font-weight: 700;

    &::after {
      position: absolute;
      right: 12px;
      bottom: 0;
      left: 12px;
      height: 2px;
      background: var(--gg-primary-500, #007bff);
      content: '';
    }
  }
}

.detail-stats {
  padding: 14px 20px;
  border-bottom: 1px solid #edf0f3;

  > div {
    min-width: 105px;
    padding: 7px 10px;
    border: 1px solid #e7ebf0;
    border-radius: 7px;
    color: var(--gg-text-muted, #9096a2);
    font-size: 12px;
  }

  .el-icon {
    color: #1685a5;
    font-size: 16px;
  }

  b {
    color: var(--gg-text-primary, #222527);
    font-weight: 600;
  }
}

.storage-tag {
  margin-left: auto;
  color: var(--gg-text-muted, #9096a2);
  font-size: 12px;
}

@media (max-width: 700px) {
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-tabs {
    justify-content: flex-start;
    overflow: auto;
  }

  .detail-stats {
    overflow-x: auto;
  }
}
</style>
