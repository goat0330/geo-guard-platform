<template>
  <aside class="report-detail-panel">
    <header>
      <h1><img :src="analysisIcon" alt="" />群治群防智能体</h1>
      <button type="button" title="关闭智能体面板" @click="emit('close')"><el-icon><Close /></el-icon></button>
    </header>

    <div ref="detailScrollRef" v-loading="loading" class="detail-scroll">
      <div v-if="report" ref="detailContentRef" class="detail-content">
        <h2>报灾事件AI分析过程：</h2>

        <section v-for="section in visibleSections" :key="section.key" class="process-section message-enter">
          <button class="section-title" type="button" :disabled="isSectionLoading(section.key)" @click="toggleSection(section.key)">
            <span><el-icon><CircleCheck /></el-icon>{{ section.title }}</span>
            <el-icon v-if="!isSectionLoading(section.key)" class="section-arrow" :class="{ 'is-collapsed': !isExpanded(section.key) }"><ArrowUp /></el-icon>
          </button>
          <Transition name="section-swap" mode="out-in" @after-enter="scheduleScrollBottom">
            <div v-if="isSectionLoading(section.key)" :key="`${section.key}-loading`" class="analysis-loading">
              <i></i><i></i><i></i>
            </div>
            <div v-else-if="isSectionContentVisible(section.key)" :key="`${section.key}-content`" class="section-body" :class="{ 'is-collapsed': !isExpanded(section.key) }">
              <div>
                <template v-if="section.key === 'photos'">
                  <template v-if="visiblePhotos.length">
                    <div class="photo-grid">
                      <img v-for="url in visiblePhotos" :key="url" :src="url" alt="报灾现场图片" />
                    </div>
                    <div v-if="photoPageCount > 1" class="photo-pager">
                      <button type="button" :disabled="photoPage === 0" @click="changePhotoPage(-1)">
                        <el-icon><ArrowLeft /></el-icon>
                        上一组
                      </button>
                      <span>{{ photoPage + 1 }} / {{ photoPageCount }}</span>
                      <button type="button" :disabled="photoPage === photoPageCount - 1" @click="changePhotoPage(1)">
                        下一组
                        <el-icon><ArrowRight /></el-icon>
                      </button>
                    </div>
                  </template>
                  <p v-else class="empty-content">--</p>
                </template>

                <template v-else-if="section.key === 'space'">
                  <div v-if="spaceAnalysisHtml" class="analysis-content markdown-content" v-html="spaceAnalysisHtml"></div>
                  <p v-else class="empty-content">--</p>
                </template>

                <template v-else-if="section.key === 'vision'">
                  <ul v-if="visionItems.length" class="vision-list">
                    <li v-for="item in visionItems" :key="item.label"><b>{{ item.label }}：</b>{{ item.value }}</li>
                  </ul>
                  <p v-else class="empty-content">--</p>
                </template>

                <template v-else-if="section.key === 'risk'">
                  <p class="risk-result">风险等级：<strong :class="riskClass">{{ riskText }}</strong></p>
                </template>

                <template v-else>
                  <p class="analysis-content">{{ reportText }}</p>
                </template>
              </div>
            </div>
          </Transition>
        </section>

        <p v-if="sequenceComplete" class="task-status message-enter">该任务状态：<strong>{{ statusText }}</strong></p>
      </div>
      <div v-else-if="!loading" class="empty-state">暂无报送详情</div>
    </div>

    <footer>
      <button type="button" :disabled="!report || loading" @click="emit('feedback', report)"><el-icon><ChatLineSquare /></el-icon>提交反馈</button>
      <button type="button" :disabled="!report || loading" @click="emit('send', report)"><el-icon><User /></el-icon>发送给责任人</button>
    </footer>
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { REPORT_STATUS_TESXT, RISK_LEVEL_TEXT } from '@/utils/enum.js'
import { createMd } from '@/utils/md.js'
import { useScroll } from '@/hooks/useScroll.js'
import analysisIcon from '@/assets/imgs/fengxian/analysis.png'

defineOptions({ name: 'GroupDefenseReportDetail' })

const props = defineProps({
  report: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  photos: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'feedback', 'send'])
const expandedKeys = ref(new Set(['photos', 'space', 'vision', 'risk', 'report']))
const photoPage = ref(0)
const PHOTO_PAGE_SIZE = 2
const SECTION_REVEAL_DELAY = 720
const SECTION_CONTENT_DELAY = 240
const md = createMd({ useBreak: true })
const detailScrollRef = ref(null)
const detailContentRef = ref(null)
const visibleSectionCount = ref(0)
const visibleContentKeys = ref(new Set())
const loadingSectionKeys = ref(new Set())
const sequenceComplete = ref(false)
const { isScrollBlocked, resumeAutoScroll, scrollBottom } = useScroll(detailScrollRef, { threshold: 48 })
let sequenceTimers = []
let contentResizeObserver = null
let scrollFrame = null

const sections = [
  { key: 'photos', title: '图片加载和预处理' },
  { key: 'space', title: '上报地点空间分析' },
  { key: 'vision', title: '多模态大模型图像识别' },
  { key: 'risk', title: '推理大模型进行风险判级' },
  { key: 'report', title: '输出详细识别报告' },
]
const visibleSections = computed(() => sections.slice(0, visibleSectionCount.value))

const parseObject = (value) => {
  if (!value) return {}
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

const aiVisionProps = computed(() => parseObject(props.report?.aiVisionProps))
const workflowResult = computed(() => parseObject(aiVisionProps.value?.workflowResult))
const visionFieldOptions = [
  { label: '变形迹象', key: '变形迹象' },
  { label: '图像类型', key: '图像类型' },
  { label: '地灾阶段', key: '地灾阶段' },
  { label: '新鲜程度', key: '新鲜程度' },
  { label: '灾害类型', key: '灾害类型' },
  { label: '风险程度', key: '风险程度' },
  { label: '承灾体类型', key: '承灾体类型' },
]
const visionItems = computed(() => visionFieldOptions
  .map((item) => ({ label: item.label, value: workflowResult.value?.[item.key] }))
  .filter((item) => item.value !== null && item.value !== undefined && item.value !== ''))

const photoPageCount = computed(() => Math.ceil(props.photos.length / PHOTO_PAGE_SIZE))
const visiblePhotos = computed(() => {
  const start = photoPage.value * PHOTO_PAGE_SIZE
  return props.photos.slice(start, start + PHOTO_PAGE_SIZE)
})

const changePhotoPage = (offset) => {
  const nextPage = photoPage.value + offset
  if (nextPage >= 0 && nextPage < photoPageCount.value) photoPage.value = nextPage
}

watch(() => props.photos, () => {
  photoPage.value = 0
}, { deep: false })

const clearSectionSequence = () => {
  sequenceTimers.forEach((timer) => clearTimeout(timer))
  sequenceTimers = []
}

const scheduleScrollBottom = () => {
  if (isScrollBlocked.value || scrollFrame) return

  scrollFrame = requestAnimationFrame(async () => {
    scrollFrame = null
    if (!isScrollBlocked.value) await scrollBottom()
  })
}

const startSectionSequence = () => {
  clearSectionSequence()
  resumeAutoScroll()
  visibleSectionCount.value = 0
  visibleContentKeys.value = new Set()
  loadingSectionKeys.value = new Set()
  sequenceComplete.value = false
  expandedKeys.value = new Set(sections.map((section) => section.key))

  void nextTick(() => {
    if (detailScrollRef.value) detailScrollRef.value.scrollTop = 0
  })

  sections.forEach((section, index) => {
    sequenceTimers.push(setTimeout(() => {
      visibleSectionCount.value = index + 1
      const next = new Set(loadingSectionKeys.value)
      next.add(section.key)
      loadingSectionKeys.value = next
      scheduleScrollBottom()
    }, index * SECTION_REVEAL_DELAY))

    sequenceTimers.push(setTimeout(() => {
      const nextLoadingKeys = new Set(loadingSectionKeys.value)
      nextLoadingKeys.delete(section.key)
      loadingSectionKeys.value = nextLoadingKeys

      const nextContentKeys = new Set(visibleContentKeys.value)
      nextContentKeys.add(section.key)
      visibleContentKeys.value = nextContentKeys
      if (index === sections.length - 1) sequenceComplete.value = true
      scheduleScrollBottom()
    }, index * SECTION_REVEAL_DELAY + SECTION_CONTENT_DELAY))
  })
}

const isSectionContentVisible = (key) => visibleContentKeys.value.has(key)
const isSectionLoading = (key) => loadingSectionKeys.value.has(key)

watch(detailContentRef, (content) => {
  contentResizeObserver?.disconnect()
  if (!content) return

  contentResizeObserver = new ResizeObserver(scheduleScrollBottom)
  contentResizeObserver.observe(content)
  scheduleScrollBottom()
}, { flush: 'post' })

watch(() => props.report, (report) => {
  if (report) startSectionSequence()
  else clearSectionSequence()
}, { immediate: true })

onBeforeUnmount(() => {
  clearSectionSequence()
  contentResizeObserver?.disconnect()
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
})

const normalizeText = (value) => {
  if (value === null || value === undefined || value === '') return '--'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return '--'
  }
}

const spaceAnalysisHtml = computed(() => {
  const content = props.report?.aiSpatialAnalysis
  return typeof content === 'string' && content.trim() ? md.render(content) : ''
})
const reportText = computed(() => normalizeText(props.report?.aiReportDetail))
const riskText = computed(() => {
  const text = RISK_LEVEL_TEXT[Number(props.report?.aiRiskLevel)]
  return text ? `${text}风险` : '--'
})
const riskClass = computed(() => `risk-${Number(props.report?.aiRiskLevel)}`)
const statusText = computed(() => REPORT_STATUS_TESXT[props.report?.status] || '--')

const isExpanded = (key) => expandedKeys.value.has(key)
const toggleSection = (key) => {
  const next = new Set(expandedKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedKeys.value = next
}
</script>

<style lang="less" scoped>
.report-detail-panel { display: flex; height: 100%; min-height: 0; flex-direction: column; color: #222527; }
.report-detail-panel > header { display: flex; height: 66px; flex: 0 0 66px; align-items: center; justify-content: space-between; padding: 0 24px; background: linear-gradient(90deg, #f1f7ff 0%, #fff5f5 100%); }
.report-detail-panel > header h1 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 18px; font-weight: 600; }
.report-detail-panel > header h1 img { width: 20px; height: 20px; object-fit: contain; }
.report-detail-panel > header button { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 6px; color: #617185; cursor: pointer; }
.report-detail-panel > header button:hover { background: #dcedff; color: #007bff; }
.detail-scroll { flex: 1 1 0; min-height: 0; overflow: auto; padding: 24px; overscroll-behavior: contain; }
.detail-content > h2 { margin: 0 0 18px; font-size: 16px; font-weight: 600; }
.process-section + .process-section { margin-top: 14px; }
.message-enter { animation: message-enter 0.34s cubic-bezier(0.22, 1, 0.36, 1) both; }
.section-title { display: flex; width: 100%; height: 44px; align-items: center; justify-content: space-between; padding: 0 16px; border: 1px solid #dcedff; border-radius: 8px; background: #f5f9ff; color: #007bff; cursor: pointer; }
.section-title:disabled { cursor: default; }
.section-title > span { display: inline-flex; align-items: center; gap: 8px; font-size: 14px; }
.section-arrow { transition: transform 0.24s ease; }.section-arrow.is-collapsed { transform: rotate(180deg); }
.section-body { display: grid; grid-template-rows: 1fr; opacity: 1; transition: grid-template-rows 0.42s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.3s ease; }
.section-body.is-collapsed { grid-template-rows: 0fr; opacity: 0; }
.section-body > div { min-height: 0; overflow: hidden; }
.analysis-loading { display: flex; height: 38px; align-items: flex-end; gap: 4px; padding: 0 16px 8px; box-sizing: border-box; }
.analysis-loading i { width: 6px; height: 6px; border-radius: 50%; background: #9096a2; animation: analysis-loading 1.2s infinite ease-in-out; }
.analysis-loading i:nth-child(2) { animation-delay: 0.16s; }
.analysis-loading i:nth-child(3) { animation-delay: 0.32s; }
.section-swap-enter-active { transition: opacity 0.34s ease, transform 0.42s cubic-bezier(0.22, 1, 0.36, 1); }
.section-swap-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.section-swap-enter-from { opacity: 0; transform: translateY(-6px); }
.section-swap-leave-to { opacity: 0; transform: translateY(-2px); }
.section-body.section-swap-enter-active { transition: grid-template-rows 0.42s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.34s ease, transform 0.42s cubic-bezier(0.22, 1, 0.36, 1); }
.section-body.section-swap-enter-from { grid-template-rows: 0fr; }
.photo-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; padding: 14px 16px 0; }
.photo-grid img { width: 100%; aspect-ratio: 1.45; border-radius: 6px; object-fit: cover; }
.photo-pager { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 12px 16px 0; color: #617185; font-size: 14px; }
.photo-pager button { display: inline-flex; align-items: center; gap: 4px; padding: 0; color: #007bff; cursor: pointer; }
.photo-pager button:disabled { color: #a6acb8; cursor: not-allowed; }
.analysis-content { margin: 0; padding: 14px 16px 0; color: #617185; font-size: 14px; line-height: 24px; white-space: pre-wrap; word-break: break-word; }
.markdown-content { white-space: normal; }
.markdown-content :deep(p) { margin: 0 0 10px; }
.markdown-content :deep(p:last-child) { margin-bottom: 0; }
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) { margin: 0 0 10px; color: #383c41; font-size: 16px; line-height: 24px; }
.markdown-content :deep(ul),
.markdown-content :deep(ol) { margin: 0 0 10px; padding-left: 20px; }
.markdown-content :deep(li + li) { margin-top: 4px; }
.vision-list { display: grid; gap: 10px; padding: 14px 18px 0; color: #617185; font-size: 14px; line-height: 22px; }
.vision-list li { position: relative; padding-left: 12px; }
.vision-list li::before { position: absolute; top: 9px; left: 0; width: 4px; height: 4px; border-radius: 50%; background: #617185; content: ''; }
.vision-list b { color: #222527; font-weight: 500; }
.risk-result { margin: 0; padding: 14px 16px 0; color: #617185; font-size: 14px; }
.risk-result strong { font-weight: 600; }.risk-result .risk-1 { color: #007bff; }.risk-result .risk-2 { color: #ff922c; }.risk-result .risk-3, .risk-result .risk-4 { color: #ef655a; }
.empty-content { padding: 14px 16px 0; color: #a6acb8; font-size: 14px; }
.task-status { margin: 26px 0 0; color: #617185; font-size: 14px; }.task-status strong { color: #2bb79b; font-weight: 600; }
.empty-state { padding: 80px 0; color: #a6acb8; font-size: 14px; text-align: center; }
footer { display: grid; flex: 0 0 auto; grid-template-columns: 1fr 1fr; gap: 12px; padding: 16px 24px 20px; background: #fff; }
footer button { display: flex; height: 48px; align-items: center; justify-content: center; gap: 8px; border: 1px solid #007bff; border-radius: 6px; color: #007bff; font-size: 16px; cursor: pointer; transition: background-color 0.2s ease, color 0.2s ease; }
footer button:nth-child(2) { background: #007bff; color: #fff; }
footer button:not(:disabled):hover { background: #dcedff; } footer button:nth-child(2):not(:disabled):hover { background: #0069dd; }
footer button:disabled { cursor: not-allowed; opacity: 0.5; }
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
