<template>
  <div class="markdown-container" v-bind="$attrs">
    <!-- 主内容区域 -->
    <div class="markdown-content" ref="contentContainerRef" v-html="renderedContent"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import mermaid from 'mermaid'
import { brToNewlineInP, createMd } from '@/utils/md.js'
// import { marked } from 'marked'

// 接收父组件传入的SSE数据
const props = defineProps({
  sseData: {
    type: String,
    default: '',
  },
  isComplete: {
    // 传输完成的信号（由父组件控制）
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits(['scroll', 'scroll-to-bottom'])

// 状态管理
const contentContainerRef = ref(null)
const rawMarkdown = ref('') // 累积完整的markdown内容
const renderedContent = ref('') // 渲染的内容（传输中不含图片）
const imageQueue = ref([]) // 暂存的图片信息队列
const isLoading = ref(true)
const md = createMd({ wrapTable: true })

mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'loose',
  flowchart: {
    useMaxWidth: false,
    htmlLabels: false,
  },
})

/**
 * 在 Markdown 内容挂载完成后渲染当前回答中的 Mermaid 图表。
 * @returns {Promise<void>}
 */
const renderMermaid = async () => {
  await nextTick()
  const mermaidNodes = contentContainerRef.value?.querySelectorAll('.mermaid')
  if (!mermaidNodes?.length) return

  await mermaid.run({ nodes: mermaidNodes })
}

function escapeHtml(str) {
  if (!str) return ''
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function getFenceMarker(line) {
  const match = line.match(/^ {0,3}(```|~~~)/)
  return match?.[1] || ''
}

function normalizeInlineMath(markdown) {
  let inFence = false
  let fenceMarker = ''

  return markdown
    .split(/(\n)/)
    .map((part) => {
      if (part === '\n') return part

      const marker = getFenceMarker(part)
      if (marker && (!inFence || marker === fenceMarker)) {
        inFence = !inFence
        fenceMarker = inFence ? marker : ''
        return part
      }

      if (inFence) return part

      // KaTeX 插件不识别 "$ D $"，这里只规范渲染副本，不修改原始消息。
      return part.replace(/(^|[^\\$])\$\s*([^\n$]*?\S[^\n$]*?)\s*\$(?!\$)/g, (_, prefix, formula) => {
        return `${prefix}$${formula}$`
      })
    })
    .join('')
}

function findUnclosedBlockMathStart(markdown) {
  let inFence = false
  let fenceMarker = ''
  let blockStart = -1
  let offset = 0
  const lines = markdown.split(/(\n)/)

  for (const part of lines) {
    if (part === '\n') {
      offset += part.length
      continue
    }

    const marker = getFenceMarker(part)
    if (marker && (!inFence || marker === fenceMarker)) {
      inFence = !inFence
      fenceMarker = inFence ? marker : ''
      offset += part.length
      continue
    }

    if (!inFence) {
      let index = part.indexOf('$$')
      while (index !== -1) {
        blockStart = blockStart === -1 ? offset + index : -1
        index = part.indexOf('$$', index + 2)
      }
    }

    offset += part.length
  }

  return blockStart
}

function protectStreamingMarkdown(markdown) {
  const normalized = normalizeInlineMath(markdown)
  if (props.isComplete) return normalized

  const unclosedBlockStart = findUnclosedBlockMathStart(normalized)
  if (unclosedBlockStart === -1) return normalized

  const stableContent = normalized.slice(0, unclosedBlockStart)
  const pendingMath = normalized.slice(unclosedBlockStart)
  return `${stableContent}<div class="math-pending"><pre>${escapeHtml(pendingMath)}</pre></div>`
}

/**
 * 为旧消息中已预渲染的 HTML 表格补充滚动容器。
 * @param {string} html - Markdown 渲染后的 HTML。
 * @returns {string} 带表格容器的 HTML。
 */
function wrapLegacyTables(html = '') {
  if (html.includes('markdown-table-wrapper')) return html

  return html.replace(/<table\b[\s\S]*?<\/table>/gi, (tableHtml) => {
    return `<div class="markdown-table-wrapper">${tableHtml}</div>`
  })
}

// 过滤图片标签的渲染函数
function renderWithoutImages(markdown) {
  imageQueue.value = []
  const html = wrapLegacyTables(md.render(protectStreamingMarkdown(markdown)))

  // 提取图片信息并暂存，同时替换为占位符
  const imgRegex = /<img[^>]+src="([^">]+)"[^>]+alt="([^">]*)"[^>]*>/gi
  const filteredHtml = html.replace(imgRegex, (match, src, alt, index, width, height) => {
    // let {match, src, alt, index, width, height} = args
    // console.log(match, src, alt, index, width, height)
    // src = src.replace(/http|https/, window)
    // 存储图片信息（包含位置标记）
    imageQueue.value.push({
      src: src.replace(/https?:\/\/example\.com/, '/images-service'),
      alt,
      positionMarker: `__IMAGE_MARKER_${imageQueue.value.length}__`,
      dom: match.replace(/https?:\/\/example\.com/, '/images-service'),
    })
    // console.log(imageQueue.value)
    // 返回占位标记
    const newMarker = imageQueue.value[imageQueue.value.length - 1].positionMarker
    return `<div id="${newMarker}" class="img-placer"></div>`
    // return imgNode.zhuanyi
  })

  return brToNewlineInP(filteredHtml)
}
// 传输完成后替换图片占位符
async function loadAllImages() {
  if (!contentContainerRef.value || imageQueue.value.length === 0) return

  let finalHtml = renderedContent.value

  // 替换所有图片占位符为实际img标签
  imageQueue.value.forEach((img) => {
    finalHtml = finalHtml.replace(
      `<div id="${img.positionMarker}" class="img-placer"></div>`,
      `<div class="img-wrap">${img.dom}</div>`,
      // `<img src="${img.src}" alt="${img.alt}" width="${img.width}" height="${img.height}" class="lazy-loaded">`,
    )
  })

  // 渲染最终内容（包含图片）
  renderedContent.value = finalHtml
  imageQueue.value = [] // 清空队列

  await nextTick()
  const images = contentContainerRef.value.querySelectorAll('img')
  if (images.length > 0) {
    const promiseArray = Array.from(images).map((img) => {
      if (img.complete) return Promise.resolve() // 如果图片已经缓存加载完了
      return new Promise((resolve) => {
        img.onload = resolve
        img.onerror = resolve // 即使加载失败也继续，防止死等
      })
    })

    // 4. 等待所有图片加载完成
    // await Promise.all(promiseArray)
    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
  }

  scrollChatListToBottom()
}

function scrollChatListToBottom(flag) {
  setTimeout(() => {
    emits('scroll')
    emits('scroll-to-bottom', flag)
  }, 300)
}

function scrollWithObserver() {
  const container = contentContainerRef.value
  if (!container) return

  const resizeObserver = new ResizeObserver(() => {
    // 只要容器高度发生变化（图片加载、文字渲染），就触发滚动
    scrollChatListToBottom()
  })

  // 开始监听
  resizeObserver.observe(container)

  // 3秒后停止监听（或者在图片加载完后停止），防止性能损耗
  setTimeout(() => {
    resizeObserver.disconnect()
  }, 3000)
}

// 监听SSE数据更新
watch(
  () => props.sseData,
  (newVal) => {
    if (newVal) {
      rawMarkdown.value = newVal
      // 传输中只渲染文本，不渲染图片
      renderedContent.value = renderWithoutImages(newVal)
      scrollChatListToBottom()
    } else {
      rawMarkdown.value = ''
      renderedContent.value = ''
      imageQueue.value = []
    }
  },
  {
    immediate: true,
  },
)

// 监听传输完成信号
watch(
  () => props.isComplete,
  async (isComplete) => {
    if (isComplete) {
      isLoading.value = false
      renderedContent.value = renderWithoutImages(rawMarkdown.value)
      // 传输完成后加载所有图片
      await Promise.all([loadAllImages(), renderMermaid()])
      setTimeout(() => {
        scrollChatListToBottom()
      }, 1000)
    }
  },
  { immediate: true },
)

// 因为mermaid的引入，显示的html还会再被转换一次
function getContentHtml() {
  return contentContainerRef.value.innerHTML
}

// 首次挂载时渲染已完成的历史消息。
onMounted(async () => {
  await Promise.all([loadAllImages(), renderMermaid()])
  scrollChatListToBottom(true)
})

function manualControllMemain() {
  renderMermaid()
}

defineExpose({
  getContentHtml,
  manualControllMemain,
})
</script>

<style lang="less" scoped>
// @import 'katex/dist/katex.min.css';
.markdown-container {
  /* max-width: 800px; */
  :deep(.markdown-content) {
    ul,
    ol {
      margin: 6px 0 12px;
      padding-left: 20px !important;
      list-style-position: outside;
    }

    ul {
      list-style-type: disc !important;
    }

    ul ul {
      list-style-type: circle !important;
    }

    ul ul ul {
      list-style-type: square !important;
    }

    ol {
      list-style: decimal !important;
    }

    ol ol {
      list-style-type: lower-alpha !important;
    }

    p {
      white-space: pre-wrap;
    }

    p:has(+ *) {
      margin-bottom: 5px;
    }
    hr {
      border: none; /* 去掉原生阴影和边框 */
      height: 1px; /* 设置你想要的高度 */
      background-color: #e5e8ec; /* 设置颜色 */
      margin: 12px 0;
    }

    hr {
      border: none; /* 去掉原生阴影和边框 */
      height: 1px; /* 设置你想要的高度 */
      background-color: #e5e8ec; /* 设置颜色 */
      margin: 12px 0;
    }

    .loading-indicator {
      color: #666;
      padding: 10px;
      border: 1px dashed #ccc;
      margin-bottom: 10px;
    }

    .markdown-content {
      line-height: 1.8;
    }

    .image-placeholder {
      display: inline-block;
      width: 200px;
      height: 200px;
      background: #f0f0f0;
      border-radius: 4px;
      margin: 0 2px;
      vertical-align: middle;
    }

    .lazy-loaded.loading {
      opacity: 0.5;
    }

    .img-placer {
      width: 480px;
      height: 300px;
      margin-top: 20px;

      background: #f5f7fa;
    }
    .img-wrap {
      display: inline-block;
      padding: 20px;
      background: #f5f7fa;
      margin-top: 20px;
    }
    .math-pending {
      margin: 8px 0;

      pre {
        margin: 0;
        padding: 8px 10px;
        white-space: pre-wrap;
        word-break: break-word;
        color: #606266;
        background: #f7f8fa;
        border-radius: 4px;
      }
    }
    a {
      color: var(--el-color-primary);
      text-decoration: underline;
    }

    .markdown-table-wrapper {
      width: 100%;
      margin: 12px 0;
      overflow-x: auto;
      border: 1px solid #e1e4eb;
      border-radius: 12px;
      background: #fafbfe;
      box-sizing: border-box;

      table {
        width: max-content;
        min-width: 100%;
        margin: 0;
        border: 0;
        border-collapse: separate;
        border-spacing: 0;
        background: #fafbfe;
      }

      th,
      td {
        min-width: 80px;
        padding: 10px 14px;
        border: 0;
        border-right: 1px solid #e1e4eb;
        border-bottom: 1px solid #e1e4eb;
        box-sizing: border-box;
        color: #30343b;
        font-size: 14px;
        font-weight: 400;
        line-height: 20px;
        text-align: left;
        white-space: nowrap;
        background: #fafbfe;
      }

      th {
        font-weight: 600;
        color: #000;
      }

      th:last-child,
      td:last-child {
        border-right: 0;
      }

      tbody tr:last-child td {
        border-bottom: 0;
      }
    }
  }
}
</style>
