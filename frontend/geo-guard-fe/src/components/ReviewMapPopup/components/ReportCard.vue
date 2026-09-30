<template>
  <article class="report-card">
    <header class="card-header">
      <div class="header-main">
        <h2 class="report-title" :title="fileName">
          {{ fileName }}
        </h2>
        <div class="report-meta-row">
          <span v-if="report.roundNo" class="meta-tag">
            第 {{ report.roundNo }} 轮调查
          </span>
          <span v-if="report.attachmentTypeName || report.category" class="meta-tag tag-type">
            {{ report.attachmentTypeName || '调查报告' }}
          </span>
          <span class="meta-item">
            调查人：{{ report.generatedBy || report.operator || '--' }}
          </span>
          <span class="meta-item">
            调查时间：{{ report.generatedAt || report.createdAt || '--' }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <el-button
          type="primary"
          :icon="Download"
          :loading="downloading"
          @click="$emit('download', report)"
        >
          下载报告
        </el-button>
      </div>
    </header>

    <div class="card-body">
      <!-- 文件附件展示卡片 -->
      <div class="file-attachment-card">
        <div class="file-icon-box" :class="fileTypeClass">
          <span class="file-type-badge">{{ fileExt }}</span>
          <el-icon class="file-icon"><Document /></el-icon>
        </div>
        <div class="file-info">
          <div class="file-name" :title="fileName">
            {{ fileName }}
          </div>
          <div class="file-meta">
            <span v-if="fileSize" class="meta-spec">
              大小：{{ fileSize }}
            </span>
            <span v-if="report.sourceSystem" class="meta-spec">
              来源：{{ report.sourceSystem }}
            </span>
            <span v-if="report.createdAt" class="meta-spec">
              生成时间：{{ report.createdAt }}
            </span>
          </div>
        </div>
        <div class="file-actions">
          <el-button
            type="primary"
            link
            :icon="View"
            :loading="previewing"
            @click="$emit('preview', report)"
          >
            预览
          </el-button>
          <el-button
            type="primary"
            link
            :icon="Download"
            :loading="downloading"
            @click="$emit('download', report)"
          >
            下载
          </el-button>
        </div>
      </div>

      <!-- 报告摘要或描述说明 -->
      <div
        v-if="report.content || report.description"
        class="report-desc-section"
      >
        <div class="desc-label">报告摘要 / 描述说明</div>
        <div class="report-text-content">
          {{ report.content || report.description }}
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { Document, Download, View } from '@element-plus/icons-vue'

defineOptions({ name: 'ReportCard' })

const props = defineProps({
  report: {
    type: Object,
    required: true,
  },
  downloading: {
    type: Boolean,
    default: false,
  },
  previewing: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['download', 'preview'])

const fileName = computed(() => {
  const r = props.report
  let name = r.fileName || r.title || r.name || '现场调查报告'
  if (!name.includes('.') && r.fileExtension) {
    name = `${name}.${r.fileExtension}`
  } else if (!name.includes('.')) {
    name = `${name}.pdf`
  }
  return name
})

const fileExt = computed(() => {
  const r = props.report
  if (r.fileExtension) return String(r.fileExtension).toUpperCase()
  const name = r.fileName || r.title || ''
  const parts = name.split('.')
  return parts.length > 1 ? parts.pop().toUpperCase() : 'PDF'
})

const fileTypeClass = computed(() => {
  const ext = fileExt.value.toLowerCase()
  if (ext === 'pdf') return 'is-pdf'
  if (ext === 'doc' || ext === 'docx') return 'is-word'
  return 'is-generic'
})

const fileSize = computed(() => {
  const bytes = props.report?.fileSize
  if (!bytes || isNaN(bytes)) return ''
  const b = Number(bytes)
  if (b < 1024) return `${b} B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
  return `${(b / (1024 * 1024)).toFixed(2)} MB`
})
</script>

<style lang="less" scoped>
.report-card {
  border: 1px solid #e4eaef;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #edf0f4;
  background: #fbfcfe;

  .header-main {
    min-width: 0;
    flex: 1;
  }

  .header-actions {
    flex-shrink: 0;
  }
}

.report-title {
  margin: 0 0 10px;
  color: #222527;
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.report-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #617185;
  font-size: 12px;
  line-height: 18px;
}

.meta-tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: #e0eefa;
  color: #007bff;
  font-weight: 500;

  &.tag-type {
    background: #edf0f4;
    color: #617185;
  }
}

.meta-item {
  color: #617185;
}

.card-body {
  padding: 20px;
}

.file-attachment-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid #e4eaef;
  border-radius: 6px;
  background: #f8fafc;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: #007bff;
    box-shadow: 0 2px 8px rgba(0, 123, 255, 0.08);
  }
}

.file-icon-box {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 48px;
  border-radius: 4px;
  background: #e0eefa;
  color: #007bff;
  flex-shrink: 0;

  .file-icon {
    font-size: 24px;
  }

  .file-type-badge {
    position: absolute;
    bottom: 2px;
    left: 2px;
    right: 2px;
    background: #007bff;
    color: #ffffff;
    font-size: 10px;
    line-height: 12px;
    font-weight: 700;
    text-align: center;
    border-radius: 2px;
    transform: scale(0.85);
  }

  &.is-pdf {
    background: #fee2e2;
    color: #e45b5b;

    .file-type-badge {
      background: #e45b5b;
    }
  }

  &.is-word {
    background: #e0eefa;
    color: #007bff;

    .file-type-badge {
      background: #007bff;
    }
  }
}

.file-info {
  min-width: 0;
  flex: 1;

  .file-name {
    color: #222527;
    font-size: 14px;
    font-weight: 600;
    line-height: 22px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 4px;
    color: #9096a2;
    font-size: 12px;
    line-height: 18px;
  }
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.report-desc-section {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 6px;
  background: #fbfcfe;
  border: 1px dashed #e4eaef;

  .desc-label {
    margin-bottom: 6px;
    color: #617185;
    font-size: 12px;
    font-weight: 600;
    line-height: 18px;
  }
}

.report-text-content {
  color: #383c41;
  font-size: 14px;
  line-height: 24px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
