<template>
  <aside class="event-detail-panel">
    <!-- 顶部 Header 标题栏 -->
    <header class="detail-header">
      <div class="header-title-wrap">
        <h1 :title="headerTitle">{{ headerTitle }}</h1>
        <span v-if="reviewStatusText" class="review-status">
          <i class="status-dot"></i>
          {{ reviewStatusText }}
        </span>
      </div>
      <button
        type="button"
        class="close-btn"
        title="关闭事件详情"
        @click="emit('close')"
      >
        <i class="iconfont icon-close"></i>
      </button>
    </header>

    <!-- 主体滚动内容区 -->
    <div ref="contentRef" class="detail-content">
      <Transition name="detail-section">
        <DetailOverview
          v-if="visibleSectionCount >= 1"
          :description="detailData?.eventOverview || props.event?.disposalOverview || props.event?.description || ''"
          :loading="detailLoading"
        />
      </Transition>

      <Transition name="detail-section">
        <DetailBasicInfo
          v-if="visibleSectionCount >= 2"
          :basic-info="detailData?.basicInfo"
          :tags="detailData?.tags"
          :event="props.event"
          :loading="detailLoading"
        />
      </Transition>

      <Transition name="detail-section">
        <DetailImpact
          v-if="visibleSectionCount >= 3"
          :impact-info="detailData?.impactInfo"
          :material-info="detailData?.materialInfo"
          :event="props.event"
          :loading="detailLoading"
          @preview-photo="handleOpenPhotos"
          @preview-report="handleOpenReports"
        />
      </Transition>

      <Transition name="detail-section">
        <DetailTimeline
          v-if="visibleSectionCount >= 4"
          :items="timelineItems"
          :total-nodes-count="timelineItems.length"
          :loading="timelineLoading"
        />
      </Transition>

      <Transition name="detail-loading">
        <div
          v-if="visibleSectionCount < 4"
          class="analysis-loading"
          aria-label="正在整理事件详情"
        >
          <i></i>
          <i></i>
          <i></i>
        </div>
      </Transition>
    </div>

    <!-- 底部操作按钮栏 (对齐设计稿 48px 大按钮) -->
    <footer class="detail-footer">
      <button
        type="button"
        class="btn-action btn-secondary"
        @click="emit('feedback')"
      >
        <FolderAdd class="btn-icon" />
        <span>补充资料</span>
      </button>
      <button
        type="button"
        class="btn-action btn-primary"
        :disabled="reportLoading"
        @click="handleGenerateReport"
      >
        <Loading v-if="reportLoading" class="btn-icon is-loading" />
        <Document v-else class="btn-icon" />
        <span>{{ reportLoading ? '正在生成...' : '生成复盘报告' }}</span>
      </button>
    </footer>

    <!-- 现场照片弹窗 -->
    <PhotoDialog
      v-model:visible="photoDialogVisible"
      :event-id="props.event?.id"
      :event-title="headerTitle"
    />

    <!-- 调查报告弹窗 -->
    <ReportDialog
      v-model:visible="reportDialogVisible"
      :event-id="props.event?.id"
      :event-title="headerTitle"
      :initial-reports="detailData?.materialInfo?.investigationReports"
    />
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Document, FolderAdd, Loading } from '@element-plus/icons-vue'
import DetailOverview from './components/DetailOverview.vue'
import DetailBasicInfo from './components/DetailBasicInfo.vue'
import DetailImpact from './components/DetailImpact.vue'
import DetailTimeline from './components/DetailTimeline.vue'
import PhotoDialog from '@/components/ReviewMapPopup/components/PhotoDialog.vue'
import ReportDialog from '@/components/ReviewMapPopup/components/ReportDialog.vue'
import { getReviewEventDetail } from '@/api/review.js'
import { getImageUrlById } from '@/api/common.js'
import sitePhoto1 from '@/assets/imgs/fupan/site-photo-1.png'
import sitePhoto2 from '@/assets/imgs/fupan/site-photo-2.png'

defineOptions({ name: 'ReviewEventDetail' })

const props = defineProps({
  event: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'feedback', 'report'])

const contentRef = ref(null)
const reportLoading = ref(false)
const timelineLoading = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailTimeline = ref([])
const visibleSectionCount = ref(0)
const timers = new Map()

const photoDialogVisible = ref(false)
const reportDialogVisible = ref(false)

const headerTitle = computed(() => {
  return (
    detailData.value?.basicInfo?.disasterName ||
    props.event?.eventName ||
    props.event?.title ||
    (props.event?.id ? `事件详情${props.event.id}` : '--')
  )
})

const reviewStatusText = computed(() => {
  return (
    detailData.value?.reviewStatusName ||
    props.event?.reviewStatusName ||
    props.event?.reviewStatus ||
    ''
  )
})

// 开发联调备用图片映射
const MOCK_PHOTO_MAP = {
  'mock-oss-panorama-001': sitePhoto1,
  'mock-oss-deformation-001': sitePhoto2,
  'mock-oss-damage-001': sitePhoto1,
  'mock-task-photo-001': sitePhoto2,
  'mock-task-photo-002': sitePhoto1,
}

const clearTimers = () => {
  timers.forEach((resolve, timer) => {
    window.clearTimeout(timer)
    resolve(false)
  })
  timers.clear()
}

const wait = (duration) =>
  new Promise((resolve) => {
    const timer = window.setTimeout(() => {
      timers.delete(timer)
      resolve(true)
    }, duration)
    timers.set(timer, resolve)
  })

const scrollToBottom = async () => {
  await nextTick()
  if (contentRef.value) {
    contentRef.value.scrollTo({
      top: contentRef.value.scrollHeight,
      behavior: 'smooth',
    })
  }
}

let playVersion = 0

// 四个业务区块按顺序出现
const playSectionSequence = async (version) => {
  visibleSectionCount.value = 0

  for (let section = 1; section <= 4; section += 1) {
    const finished = await wait(300)
    if (!finished || version !== playVersion) return
    visibleSectionCount.value = section
    await scrollToBottom()
  }
}

/**
 * 加载并格式化真实事件详情数据及处置时间线
 */
const loadDetailData = async (id, version) => {
  if (!id) return
  detailLoading.value = true
  timelineLoading.value = true

  try {
    const res = await getReviewEventDetail(id)
    if (version !== playVersion) return
    const detail = res?.data || res || {}
    detailData.value = detail
    const rawTimeline = detail?.timeline || []

    // 收集所有关联照片的 fileId
    const fileIds = []
    rawTimeline.forEach((node) => {
      if (Array.isArray(node.photos)) {
        node.photos.forEach((p) => {
          if (p?.fileId) fileIds.push(p.fileId)
        })
      }
    })

    // 批量换取 OSS 地址
    const urlMap = {}
    if (fileIds.length > 0) {
      try {
        const resUrls = await getImageUrlById({ ids: fileIds })
        if (Array.isArray(resUrls)) {
          fileIds.forEach((fileId, idx) => {
            const rawUrl = resUrls[idx]
            if (rawUrl) {
              try {
                const urlObj = new URL(rawUrl)
                urlMap[fileId] = '/oss-service' + urlObj.pathname + urlObj.search
              } catch {
                urlMap[fileId] = rawUrl
              }
            }
          })
        }
      } catch (err) {
        console.warn('换取时间线照片 OSS 地址失败，回退本地开发备用图', err)
      }
    }

    // 结构化节点并注入照片 URL
    const processed = rawTimeline.map((node) => {
      const photos = (node.photos || []).map((p, pIdx) => {
        if (typeof p === 'string') return p
        const fallback = MOCK_PHOTO_MAP[p.fileId] || (pIdx % 2 === 0 ? sitePhoto1 : sitePhoto2)
        return {
          ...p,
          url: urlMap[p.fileId] || (p.fileId?.startsWith('http') ? p.fileId : fallback),
        }
      })

      const reports = detail?.materialInfo?.investigationReports || []
      return {
        ...node,
        photos,
        reports: node.code === 'REPORT_GENERATED' ? reports : [],
      }
    })

    detailTimeline.value = processed
  } catch (err) {
    console.error('获取事件详情失败', err)
  } finally {
    if (version === playVersion) {
      detailLoading.value = false
      timelineLoading.value = false
    }
  }
}

const timelineItems = computed(() => {
  const reports = detailData.value?.materialInfo?.investigationReports || []
  if (detailTimeline.value.length > 0) {
    return detailTimeline.value.map((node) => ({
      ...node,
      reports: node.code === 'REPORT_GENERATED' ? reports : (node.reports || []),
    }))
  }
  const raw = props.event.timeline || []
  return raw.map((node) => ({
    ...node,
    reports: node.code === 'REPORT_GENERATED' ? reports : [],
  }))
})

const handleOpenPhotos = () => {
  photoDialogVisible.value = true
}

const handleOpenReports = () => {
  reportDialogVisible.value = true
}

watch(
  () => props.event.id,
  (id) => {
    playVersion += 1
    clearTimers()
    detailData.value = null
    detailTimeline.value = []
    reportLoading.value = false
    void playSectionSequence(playVersion)
    void loadDetailData(id, playVersion)
  },
  { immediate: true },
)

const handleGenerateReport = async () => {
  if (reportLoading.value) return
  reportLoading.value = true
  const finished = await wait(850)
  if (!finished) return
  reportLoading.value = false
  emit('report', props.event)
}

onBeforeUnmount(() => {
  playVersion += 1
  clearTimers()
})
</script>

<style lang="less" scoped src="./style.less"></style>

<style lang="less" scoped>
.detail-section-enter-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.detail-section-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.analysis-loading {
  height: 34px;
  padding-left: 22px;
  display: flex;
  align-items: center;
  gap: 4px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #9096A2;
    animation: analysis-loading 1.2s infinite ease-in-out;
  }

  i:nth-child(2) {
    animation-delay: 0.16s;
  }

  i:nth-child(3) {
    animation-delay: 0.32s;
  }
}

.detail-loading-leave-active {
  transition: opacity 0.2s ease;
}

.detail-loading-leave-to {
  opacity: 0;
}

@keyframes analysis-loading {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}
</style>
