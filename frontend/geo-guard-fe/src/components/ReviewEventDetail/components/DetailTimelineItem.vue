<template>
  <article class="timeline-item message-enter" :class="{ 'is-unavailable': isUnavailable }">
    <!-- 时间线左侧轴线与同心圆指示器 -->
    <div class="timeline-rail">
      <div class="indicator-wrap">
        <span class="outer-circle"></span>
        <span class="inner-dot"></span>
      </div>
      <span v-if="!isLast" class="vertical-line"></span>
    </div>

    <!-- 时间线右侧内容 -->
    <div class="timeline-body">
      <!-- 步骤标题与折叠切换 -->
      <div class="node-header">
        <div class="title-wrap">
          <h4 class="node-title">{{ item.name || item.step || item.title || '处置节点' }}</h4>
          <span v-if="isUnavailable" class="gap-tag">待补充</span>
        </div>
        <button
          type="button"
          class="view-detail-btn"
          :aria-expanded="isExpanded"
          @click="$emit('toggle-expand')"
        >
          <span>查看详情</span>
          <i
            class="iconfont icon-arrow-down arrow-icon"
            :class="{ 'is-folded': !isExpanded }"
          ></i>
        </button>
      </div>

      <!-- 可折叠内容卡片 -->
      <el-collapse-transition>
        <div v-show="isExpanded" class="timeline-collapsible-content">
          <div class="timeline-card">
            <!-- 描述文案或无数据缺口提示 -->
            <p v-if="descStr" class="card-desc">
              <template v-if="item.code === 'REPORT_GENERATED' && descBookMatch">
                <span>{{ descBookMatch.before }}</span>
                <a
                  href="javascript:void(0)"
                  class="desc-report-link"
                  :title="`点击下载《${descBookMatch.name}》`"
                  @click="handleDownloadReport(descBookMatch.name)"
                >
                  《{{ descBookMatch.name }}》
                </a>
                <span>{{ descBookMatch.after }}</span>
              </template>
              <template v-else>
                {{ descStr }}
              </template>
            </p>
            <p v-else-if="isUnavailable" class="card-desc is-empty">暂无此环节处置记录（待补充）</p>

            <!-- 任务/报告明细列表 -->
            <div v-if="item.details && item.details.length" class="details-block">
              <div class="block-label">{{ detailsLabel }}</div>
              <ul class="detail-list">
                <li v-for="(detail, dIdx) in item.details" :key="dIdx" class="detail-item">
                  <span class="bullet-dot"></span>
                  <template v-if="item.code === 'REPORT_GENERATED'">
                    <a
                      href="javascript:void(0)"
                      class="detail-report-link"
                      :title="`点击下载《${detail}》`"
                      @click="handleDownloadReport(detail)"
                    >
                      <span class="detail-text">{{ detail }}</span>
                      <el-icon class="download-icon"><Download /></el-icon>
                    </a>
                  </template>
                  <span v-else class="detail-text">{{ detail }}</span>
                </li>
              </ul>
            </div>

            <!-- 操作人与时间栏 -->
            <div v-if="operatorStr || timeStr" class="card-footer-meta">
              <span v-if="operatorStr" class="operator-text">
                {{ item.operatorRole ? `${item.operatorRole}：` : '操作人：' }}{{ operatorStr }}
              </span>
              <span v-if="timeStr" class="time-text">时间：{{ timeStr }}</span>
            </div>

            <!-- 现场照片展示模块 -->
            <div v-if="photoList.length" class="photos-block">
              <div class="block-label">现场照片：</div>
              <div class="photo-grid">
                <div
                  v-for="(photo, pIdx) in photoList"
                  :key="pIdx"
                  class="photo-card"
                >
                  <el-image
                    :src="photo.url"
                    :preview-src-list="previewPhotoUrls"
                    :initial-index="pIdx"
                    preview-teleported
                    class="scene-photo"
                    fit="cover"
                    loading="lazy"
                  />
                  <span v-if="photo.category" class="photo-badge">{{ photo.category }}</span>
                </div>
              </div>
            </div>

            <!-- 反馈摘要 -->
            <div v-if="item.summary" class="summary-block">
              <div class="block-label">反馈摘要：</div>
              <p class="summary-text">{{ item.summary }}</p>
            </div>

            <!-- 现场位置 -->
            <div v-if="locationStr" class="location-block">
              <span class="block-label">现场位置：</span>
              <span class="location-text">{{ locationStr }}</span>
            </div>
          </div>

          <!-- 警示核验胶囊条 (差异提示) -->
          <div v-if="alertStr" class="warning-alert-bar">
            <div class="alert-icon">!</div>
            <span class="alert-text">{{ alertStr }}</span>
          </div>
        </div>
      </el-collapse-transition>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { useReportDownload } from '@/composables/useReportDownload.js'

defineOptions({ name: 'DetailTimelineItem' })

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  index: {
    type: Number,
    required: true,
  },
  isLast: {
    type: Boolean,
    default: false,
  },
  isExpanded: {
    type: Boolean,
    default: true,
  },
})

defineEmits(['toggle-expand'])

const { downloadReport } = useReportDownload()

const isUnavailable = computed(() => props.item?.available === false)
const descStr = computed(() => props.item?.description || props.item?.content || '')

const descBookMatch = computed(() => {
  const text = descStr.value
  if (!text) return null
  const m = text.match(/^(.*?)《(.*?)》(.*?)$/)
  if (!m) return null
  return {
    before: m[1],
    name: m[2],
    after: m[3],
  }
})

const handleDownloadReport = async (reportName) => {
  const reports = props.item?.reports || []
  let target = reports.find(
    (r) => r.fileName === reportName || r.title === reportName || (r.fileName && r.fileName.includes(reportName)),
  )
  if (!target && reports.length > 0) {
    target = reports[0]
  }
  if (!target) {
    target = { fileName: reportName }
  }
  await downloadReport(target)
}
const timeStr = computed(() => props.item?.occurredAt || props.item?.time || '')
const operatorStr = computed(() => props.item?.operator || '')
const locationStr = computed(() => props.item?.location || props.item?.siteLocation || '')
const alertStr = computed(() => props.item?.warning || props.item?.alert || '')

const detailsLabel = computed(() => {
  if (props.item?.code === 'DISPATCH') return '任务明细：'
  if (props.item?.code === 'REPORT_GENERATED') return '关联报告：'
  return '相关明细：'
})

const photoList = computed(() => {
  if (!props.item?.photos || !Array.isArray(props.item.photos)) return []
  return props.item.photos.map((p) => {
    if (typeof p === 'string') {
      return { url: p, category: '' }
    }
    return {
      url: p.url || '',
      category: p.category || '',
      name: p.name || '',
    }
  })
})

const previewPhotoUrls = computed(() => {
  return photoList.value.map((p) => p.url).filter(Boolean)
})
</script>

<style lang="less" scoped>
.timeline-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 8px;
  position: relative;
}

.timeline-rail {
  display: flex;
  flex-direction: column;
  align-items: center;

  .indicator-wrap {
    width: 14px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    flex-shrink: 0;
  }

  .outer-circle {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #007bff;
    opacity: 0.4;
    position: absolute;
    transition: background-color 0.2s ease;
  }

  .inner-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #007bff;
    position: relative;
    z-index: 1;
    transition: background-color 0.2s ease;
  }

  .vertical-line {
    flex: 1;
    width: 1px;
    min-height: 16px;
    background: #dee2ec;
  }
}

.timeline-item.is-unavailable {
  .outer-circle {
    background: #a6acb8;
    opacity: 0.28;
  }

  .inner-dot {
    background: #a6acb8;
  }

  .node-title {
    color: #9096a2;
  }
}

.timeline-body {
  padding-bottom: 16px;
}

.node-header {
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  .title-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .node-title {
    margin: 0;
    color: #383c41;
    font-size: 14px;
    font-weight: 700;
    line-height: 20px;
  }

  .gap-tag {
    display: inline-flex;
    align-items: center;
    height: 18px;
    padding: 0 6px;
    font-size: 10px;
    color: #9096a2;
    background: #eaedf2;
    border-radius: 4px;
    line-height: 18px;
  }

  .view-detail-btn {
    border: 0;
    background: transparent;
    padding: 0;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #007bff;
    font-size: 12px;
    cursor: pointer;

    .arrow-icon {
      display: inline-block;
      font-size: 12px;
      line-height: 1;
      transition: transform 0.2s ease;
      transform: rotate(0deg);

      &.is-folded {
        transform: rotate(-90deg);
      }
    }

    &:hover {
      opacity: 0.8;
    }
  }
}

.timeline-collapsible-content {
  overflow: hidden;
}

.timeline-card {
  border-radius: 12px;
  background: #f5f7fa;
  padding: 12px;
  box-sizing: border-box;

  .card-desc {
    margin: 0;
    color: #617185;
    font-size: 14px;
    line-height: 20px;

    &.is-empty {
      color: #9096a2;
    }
  }

  .desc-report-link {
    color: #007bff;
    font-weight: 500;
    text-decoration: underline;
    text-underline-offset: 3px;
    cursor: pointer;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.8;
    }
  }

  .card-footer-meta {
    margin-top: 8px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    line-height: 20px;
    color: #617185;

    .time-text {
      text-align: right;
    }
  }
}

.details-block {
  margin-top: 8px;

  .block-label {
    color: #617185;
    font-size: 14px;
    margin-bottom: 4px;
  }

  .detail-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .detail-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #383c41;
    line-height: 18px;

    .bullet-dot {
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background: #007bff;
      flex-shrink: 0;
    }
  }
}

.detail-report-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #007bff;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: opacity 0.2s ease;

  .detail-text {
    color: #007bff;
  }

  .download-icon {
    font-size: 14px;
    color: #007bff;
  }

  &:hover {
    opacity: 0.8;
  }
}

.photos-block {
  margin-top: 10px;

  .block-label {
    color: #617185;
    font-size: 14px;
    margin-bottom: 6px;
  }

  .photo-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .photo-card {
    position: relative;
    border-radius: 4px;
    overflow: hidden;
    height: 96px;
    background: #d9d9d9;
  }

  .scene-photo {
    width: 100%;
    height: 100%;
    cursor: pointer;
    display: block;
  }

  .photo-badge {
    position: absolute;
    bottom: 4px;
    left: 4px;
    padding: 2px 6px;
    font-size: 10px;
    line-height: 14px;
    color: #ffffff;
    background: rgba(34, 37, 39, 0.6);
    border-radius: 2px;
    pointer-events: none;
  }
}

.summary-block {
  margin-top: 10px;

  .block-label {
    color: #617185;
    font-size: 14px;
    margin-bottom: 4px;
  }

  .summary-text {
    margin: 0;
    color: #617185;
    font-size: 14px;
    line-height: 20px;
  }
}

.location-block {
  margin-top: 10px;
  font-size: 14px;
  color: #617185;
  line-height: 20px;

  .block-label {
    color: #617185;
  }

  .location-text {
    color: #617185;
  }
}

.warning-alert-bar {
  margin-top: 6px;
  height: 24px;
  padding: 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 4px;
  background: rgba(255, 146, 44, 0.16);

  .alert-icon {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #ff8a1d;
    color: #ffffff;
    font-size: 10px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
  }

  .alert-text {
    color: #ff8a1d;
    font-size: 12px;
  }
}

.message-enter {
  animation: message-enter 0.24s ease both;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
