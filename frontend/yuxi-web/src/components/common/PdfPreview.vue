<template>
  <div class="pdf-preview-container" ref="containerRef">
    <!-- 加载中状态 -->
    <div v-if="loading" class="pdf-status-view">
      <LoaderCircle class="pdf-spinner" :size="22" />
      <span class="pdf-status-text">正在加载 PDF 文档...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="pdf-status-view pdf-error-view">
      <CircleAlert :size="24" class="pdf-error-icon" />
      <span class="pdf-status-text">{{ error }}</span>
    </div>

    <!-- PDF 页面列表 -->
    <div v-else class="pdf-pages-scroll-wrapper">
      <div
        v-for="pageNum in totalPages"
        :key="pageNum"
        class="pdf-page-card"
        :data-page-number="pageNum"
      >
        <canvas :ref="(el) => setCanvasRef(el, pageNum)" class="pdf-canvas" />
        <div
          v-if="pageNum === evidencePage && evidenceBoxStyle"
          class="pdf-evidence-box"
          :style="evidenceBoxStyle"
          aria-label="检索证据在 PDF 中的位置"
        />
        <div class="pdf-page-number-tag">{{ pageNum }} / {{ totalPages }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { CircleAlert, LoaderCircle } from '@lucide/vue'

const props = defineProps({
  url: {
    type: String,
    required: true
  },
  // 左右保留的安全内边距（px）
  horizontalPadding: {
    type: Number,
    default: 32
  },
  location: {
    type: Object,
    default: null
  }
})

const containerRef = ref(null)
const loading = ref(true)
const error = ref('')
const totalPages = ref(0)
const evidenceBoxStyle = ref(null)

const canvasMap = new Map()
const renderTasks = new Map()
let pdfjsLibInstance = null
let currentPdfDoc = null
let currentLoadingTask = null
let resizeObserver = null
let resizeTimer = null
let lastRenderedWidth = 0

const setCanvasRef = (el, pageNum) => {
  if (el) {
    canvasMap.set(pageNum, el)
  } else {
    canvasMap.delete(pageNum)
  }
}

const evidencePage = computed(() => {
  const page = Number(props.location?.page)
  return Number.isInteger(page) && page > 0 && page <= totalPages.value ? page : null
})

const getEvidenceBbox = () => {
  const bbox = props.location?.bbox
  if (
    !Array.isArray(bbox) ||
    bbox.length !== 4 ||
    bbox.some((value) => value === null || value === '' || !Number.isFinite(Number(value)))
  ) {
    return null
  }
  const [x0, y0, x1, y1] = bbox.map(Number)
  return x1 > x0 && y1 > y0 ? [x0, y0, x1, y1] : null
}

const mapEvidenceBbox = (page, viewport, bbox) => {
  const pageView = page.view
  if (!pageView || pageView.length < 4) return null
  const [x0, y0, x1, y1] = bbox
  const top = pageView[3]
  const left = pageView[0]
  const points = [
    viewport.convertToViewportPoint(left + x0, top - y0),
    viewport.convertToViewportPoint(left + x1, top - y0),
    viewport.convertToViewportPoint(left + x0, top - y1),
    viewport.convertToViewportPoint(left + x1, top - y1)
  ]
  const xs = points.map(([x]) => x)
  const ys = points.map(([, y]) => y)
  const x = Math.min(...xs)
  const y = Math.min(...ys)
  const width = Math.max(...xs) - x
  const height = Math.max(...ys) - y
  if (![x, y, width, height].every(Number.isFinite) || width <= 0 || height <= 0) return null
  return {
    left: `${x}px`,
    top: `${y}px`,
    width: `${width}px`,
    height: `${height}px`
  }
}

const cancelOngoingRenders = () => {
  for (const [, task] of renderTasks) {
    try {
      task.cancel()
    } catch {
      // 忽略已完成或已取消异常
    }
  }
  renderTasks.clear()
}

const getPdfjs = async () => {
  if (pdfjsLibInstance) return pdfjsLibInstance

  const [pdfjs, workerUrlModule] = await Promise.all([
    import('pdfjs-dist'),
    import('pdfjs-dist/build/pdf.worker.min.mjs?url')
  ])

  pdfjs.GlobalWorkerOptions.workerSrc = workerUrlModule.default || workerUrlModule
  pdfjsLibInstance = pdfjs
  return pdfjs
}

// 获取容器可用的实际显示宽度
const getAvailableContainerWidth = () => {
  const container = containerRef.value
  if (!container) {
    return typeof window !== 'undefined' ? window.innerWidth : 800
  }

  // 优先取容器自身或其滚动父容器的实际 clientWidth
  const width =
    container.clientWidth ||
    container.parentElement?.clientWidth ||
    container.getBoundingClientRect().width ||
    800

  return width
}

// 计算当前容器适合的横向 100% 缩放比例
const calculateFitScale = (page) => {
  const containerWidth = getAvailableContainerWidth()
  const availableWidth = Math.max(containerWidth - props.horizontalPadding, 120)
  const unscaledViewport = page.getViewport({ scale: 1.0 })
  return availableWidth / unscaledViewport.width
}

const renderSinglePage = async (pdfDoc, pageNum) => {
  const canvas = canvasMap.get(pageNum)
  if (!canvas || !pdfDoc) return

  try {
    const page = await pdfDoc.getPage(pageNum)
    const fitScale = calculateFitScale(page)
    const viewport = page.getViewport({ scale: fitScale })

    if (pageNum === evidencePage.value) {
      const bbox = getEvidenceBbox()
      evidenceBoxStyle.value = bbox ? mapEvidenceBbox(page, viewport, bbox) : null
    }

    // 支持 HiDPI / Retina 屏幕的高清绘制
    const outputScale = window.devicePixelRatio || 1
    const ctx = canvas.getContext('2d')

    canvas.width = Math.floor(viewport.width * outputScale)
    canvas.height = Math.floor(viewport.height * outputScale)

    // CSS 保持视口物理显示尺寸
    canvas.style.width = `${Math.floor(viewport.width)}px`
    canvas.style.height = `${Math.floor(viewport.height)}px`

    const transform = outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null

    // 取消同一页之前未完成的渲染
    if (renderTasks.has(pageNum)) {
      try {
        renderTasks.get(pageNum).cancel()
      } catch {
        // 忽略
      }
      renderTasks.delete(pageNum)
    }

    const renderTask = page.render({
      canvasContext: ctx,
      transform,
      viewport
    })

    renderTasks.set(pageNum, renderTask)
    await renderTask.promise
    renderTasks.delete(pageNum)
    if (pageNum === evidencePage.value) {
      canvas.closest('.pdf-page-card')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  } catch (err) {
    if (err?.name !== 'RenderingCancelledException') {
      console.error(`渲染 PDF 第 ${pageNum} 页失败:`, err)
    }
  }
}

const renderAllPages = async () => {
  if (!currentPdfDoc) return
  cancelOngoingRenders()

  lastRenderedWidth = getAvailableContainerWidth()

  for (let i = 1; i <= totalPages.value; i++) {
    await renderSinglePage(currentPdfDoc, i)
  }
}

const loadPdf = async () => {
  if (!props.url) {
    loading.value = false
    error.value = '无效的 PDF 链接'
    return
  }

  loading.value = true
  error.value = ''
  totalPages.value = 0
  evidenceBoxStyle.value = null
  cancelOngoingRenders()

  if (currentLoadingTask) {
    try {
      currentLoadingTask.destroy()
    } catch {
      // 忽略
    }
    currentLoadingTask = null
  }

  try {
    const pdfjs = await getPdfjs()
    const loadingTask = pdfjs.getDocument({
      url: props.url,
      cMapUrl: 'https://cdn.jsdelivr.net/npm/pdfjs-dist@' + pdfjs.version + '/cmaps/',
      cMapPacked: true
    })
    currentLoadingTask = loadingTask

    const pdfDoc = await loadingTask.promise
    currentPdfDoc = pdfDoc
    totalPages.value = pdfDoc.numPages
    loading.value = false

    await nextTick()
    await renderAllPages()
  } catch (err) {
    if (err?.name !== 'RenderingCancelledException') {
      console.error('加载 PDF 失败:', err)
      error.value = '无法加载 PDF 文件或文件格式受损'
    }
    loading.value = false
  }
}

const handleResize = () => {
  if (!currentPdfDoc || loading.value || totalPages.value === 0) return

  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    const currentWidth = getAvailableContainerWidth()
    // 当容器宽度变化超过阈值时自适应重绘
    if (Math.abs(currentWidth - lastRenderedWidth) > 8) {
      renderAllPages()
    }
  }, 120)
}

watch(
  () => props.url,
  () => {
    loadPdf()
  }
)

watch(
  () => props.location,
  async () => {
    evidenceBoxStyle.value = null
    if (currentPdfDoc && evidencePage.value) {
      await renderSinglePage(currentPdfDoc, evidencePage.value)
    }
  },
  { deep: true }
)

onMounted(() => {
  loadPdf()

  if (typeof ResizeObserver !== 'undefined' && containerRef.value) {
    resizeObserver = new ResizeObserver(handleResize)
    // 监听父级或自身的变化
    const target = containerRef.value.parentElement || containerRef.value
    resizeObserver.observe(target)
  }
})

onBeforeUnmount(() => {
  clearTimeout(resizeTimer)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  cancelOngoingRenders()
  if (currentLoadingTask) {
    try {
      currentLoadingTask.destroy()
    } catch {
      // 忽略
    }
  }
  currentPdfDoc = null
  canvasMap.clear()
})
</script>

<style scoped>
.pdf-preview-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  background: var(--gray-50, #f8fafc);
  position: relative;
  user-select: text;
  box-sizing: border-box;
}

.pdf-status-view {
  flex: 1;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: var(--gray-600, #64748b);
}

.pdf-spinner {
  animation: pdf-spin 1s linear infinite;
  color: var(--primary-color, #2563eb);
}

.pdf-error-view {
  color: var(--color-danger, #ef4444);
}

.pdf-error-icon {
  opacity: 0.85;
}

.pdf-status-text {
  font-size: 13px;
}

.pdf-pages-scroll-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 16px 12px 28px;
  width: 100%;
  box-sizing: border-box;
}

.pdf-page-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100%;
  box-shadow:
    0 4px 14px rgba(0, 0, 0, 0.07),
    0 1px 3px rgba(0, 0, 0, 0.03);
  background: #ffffff;
  border-radius: 4px;
  transition: box-shadow 0.2s ease;
}

.pdf-page-card:hover {
  box-shadow:
    0 6px 18px rgba(0, 0, 0, 0.1),
    0 2px 5px rgba(0, 0, 0, 0.05);
}

.pdf-evidence-box {
  position: absolute;
  z-index: 2;
  border: 2px solid #dc2626;
  border-radius: 2px;
  background: rgba(239, 68, 68, 0.08);
  box-sizing: border-box;
  pointer-events: none;
}

.pdf-canvas {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  background: #ffffff;
}

.pdf-page-number-tag {
  position: absolute;
  bottom: -18px;
  right: 2px;
  font-size: 11px;
  color: var(--gray-400, #94a3b8);
  user-select: none;
}

@keyframes pdf-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
