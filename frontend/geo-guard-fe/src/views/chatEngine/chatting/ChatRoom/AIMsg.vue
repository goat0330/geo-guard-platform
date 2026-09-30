<template>
  <div class="ai-msg">
    <div class="ai-msg-wrap">
      <TtsPlaybackStatus v-if="!isHumanMode" :session-id="msg.ttsSessionId" />
      <div v-if="showLoadingProgress">
        <AILoading ref="AILoadingRef" v-if="!msg.noThinkingProgress"></AILoading>
      </div>
      <template v-else>
        <ChatAnswerContent
          ref="answerContentRef"
          :style="{ 'padding-bottom': showExport ? '10px' : '0' }"
          :sse-data="msg.content"
          :is-complete="msg.done"
          :msg="msg"
          :show-refs="showRefs"
          @send="(question) => emits('send', question)"
          @scroll="(flag) => emits('scroll-to-bottom', flag)"
        />
        <el-button v-if="showExport && !msg.loading" type="text" class="export-btn" @click="exportPdf($event, msg)"
          >导出为pdf</el-button
        >

        <div v-if="msg.done" class="tool-btn">
          <SvgCopy @click="copyText(msg.originalContent)"></SvgCopy>
          <SvgWord @click="exportToDocx(msg.content)"></SvgWord>

          <SvgAdmire v-if="admireStatus !== 1" @click="ChangeAdmireStatus(1)"></SvgAdmire>
          <SvgAdmireActive
            v-if="admireStatus == 1"
            class="active-icon"
            @click="ChangeAdmireStatus(0)"
          ></SvgAdmireActive>
          <SvgDevalue v-if="admireStatus !== 2" @click="ChangeAdmireStatus(2)"></SvgDevalue>
          <!-- <SvgAdmire @click="ChangeAdmireStatus(true)"></SvgAdmire> -->
          <SvgDevalueActive
            v-if="admireStatus == 2"
            class="active-icon"
            @click="ChangeAdmireStatus(0)"
          ></SvgDevalueActive>
          <span class="msg-date">{{ msg.date }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { storeToRefs } from 'pinia'
// import MarkdownIt from 'vue3-markdown-it'
import { watch, inject } from 'vue'
// import html2pdf, { f } from 'html2pdf.js'
import { ElMessage, ElMessageBox, tourEmits } from 'element-plus'
import SvgCopy from '@/assets/svg/home/copy.svg?component'
import SvgWord from '@/assets/svg/home/word.svg?component'
import SvgAdmire from '@/assets/svg/home/admire.svg?component'
import SvgDevalue from '@/assets/svg/home/devalue.svg?component'
import SvgAdmireActive from '@/assets/svg/home/admire_active.svg?component'
import SvgDevalueActive from '@/assets/svg/home/devalue_active.svg?component'
import AILoading from '@/components/AILoading/index.vue'
import ChatAnswerContent from '@/components/ChatAnswerContent/index.vue'
import TtsPlaybackStatus from '@/components/TtsPlaybackStatus/index.vue'

import { saveAs } from 'file-saver'
// import { getDownloadUrl, updateLike } from '@/api'
import { updateLike } from '@/api/chat'
// import axios from 'axios'
import { copyToClipboard } from '@/utils/index'
import * as docx from 'docx'
// import { AGENT_TOOLS } from '@/utils/enum'
import dayjs from 'dayjs'
import domtoimage from 'dom-to-image'
import * as htmlToImage from 'html-to-image'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  msg: {
    type: Object,
    default: () => ({}),
  },
  mode: {
    type: String,
    default: 'chat',
  },
  isAnswering: {
    type: Boolean,
    default: false,
  },
  showRefs: {
    type: Boolean,
    default: false,
  },
})
const emits = defineEmits(['send', 'scroll-to-bottom'])

const userStore = useUserStore()

const showExport = inject('showExport', false)

const isHumanMode = storeToRefs(userStore).isHumanMode

const admireStatus = ref(props.msg.admireStatus)

const answerContentRef = ref()

const showLoadingProgress = ref(true)
const AILoadingRef = ref()
watch(
  () => props.msg.loading,
  async (val) => {
    if (!val) {
      // await nextTick()
      // AILoadingRef.value?.setAnimateEnd()
      // setTimeout(() => {
      showLoadingProgress.value = false
      // }, 300)
    } else {
      showLoadingProgress.value = true
    }
  },
  {
    immediate: true,
  },
)

// const STATUS_ENUM = {
//   0: null,
//   1: 'like',
//   '-1': 'dislike',
// }
async function ChangeAdmireStatus(status) {
  await updateLike({
    detailId: props.msg.id,
    rating: status,
  })
  admireStatus.value = status
}

async function copyText(text) {
  try {
    await copyToClipboard(text)
    ElMessage.success('复制成功')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
const exportToDocx = async () => {
  let html = answerContentRef.value?.getContentHtml() || ''

  await ElMessageBox.confirm('确认导出为docx文件？', '提示')
  try {
    const result = html.replace(
      /<span[^>]*\bclass\s*=\s*["'][^"']*?\bquote-text\b[^"']*?["'][^>]*>([^<]*)<\/span>/gi,
      '[$1]',
    )
    // 创建新的文档
    const doc = new docx.Document({
      sections: [
        {
          properties: {},
          children: await convertHtmlToDocx(result),
        },
      ],
    })
    // 生成 Blob 并下载
    const blob = await docx.Packer.toBlob(doc)
    saveAs(blob, `XX大模型-${dayjs().format('YYYY-MM-DD HH:mm:ss')}.docx`)
  } catch (error) {
    console.error(error)
  }
}

const MAX_A4_WIDTH = 580
const MAX_A4_HEIGHT = 760
async function convertSvgToPngBase64(svgElement) {
  const serializer = new XMLSerializer()
  // 确保所有 CSS 变量和样式被包含（Mermaid 依赖大量 CSS 变量）
  let svgStr = serializer.serializeToString(svgElement)

  // 如果没有 xmlns 属性则手动添加
  if (!svgStr.match(/^<svg[^>]+xmlns="http\:\/\/www\.w3\.org\/2000\/svg"/)) {
    svgStr = svgStr.replace(/^<svg/, '<svg xmlns="http://www.w3.org/2000/svg"')
  }

  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')
  const img = new Image()

  // 获取尺寸
  const width = svgElement.viewBox?.baseVal?.width || 800
  const height = svgElement.viewBox?.baseVal?.height || 450
  canvas.width = width * 2 // 2倍分辨率防止模糊
  canvas.height = height * 2

  return new Promise((resolve) => {
    img.onload = () => {
      context.fillStyle = 'white'
      context.fillRect(0, 0, canvas.width, canvas.height)
      context.drawImage(img, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/png'))
    }
    img.onerror = () => resolve(null)
    // 处理中文乱码：使用 unescape + encodeURIComponent
    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgStr)))
  })
}

// 辅助函数，将base64-转ArrayBuffer
function base64ToArrayBuffer(base64) {
  const binaryString = atob(base64.split(',')[1])
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes.buffer
}

// SVG 元素转为 PNG Base64
// async function convertSvgToPngBase64(svgElement) {
//   // 临时设置 SVG 的 width/height（避免模糊）
//   const clone = svgElement.cloneNode(true)
//   clone.setAttribute('width', '800')
//   clone.setAttribute('height', '400')
//   clone.style.width = '800px'
//   clone.style.height = '400px'

//   // 创建一个临时容器
//   const tempContainer = document.createElement('div')
//   tempContainer.style.position = 'absolute'
//   tempContainer.style.left = '-9999px'
//   tempContainer.appendChild(clone)
//   document.body.appendChild(tempContainer)

//   try {
//     // 使用 dom-to-image 渲染为 PNG
//     const dataUrl = await domtoimage.toPng(tempContainer, {
//       width: 800,
//       height: 400,
//       style: { transform: 'scale(1)', transformOrigin: 'top left' },
//       filter: (node) => !(node instanceof HTMLScriptElement), // 忽略 script
//     })
//     return dataUrl
//   } finally {
//     // 清理临时元素
//     document.body.removeChild(tempContainer)
//   }
// }

const svgMap = new Map() // 用于存放 { placeholderId: paragraphInstance }
// 将 HTML 转换为 docx 元素
const convertHtmlToDocx = async (html) => {
  // 创建一个临时 DOM 元素来解析 HTML
  svgMap.clear()
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = html

  const elements = [
    // new Paragraph({
    //   children: [
    //     Media.addImage()
    //   ]
    // })
  ]

  // 先处理所有 SVG 元素：替换为占位符 + 异步转图
  const svgPromises = []
  const svgPlaceholders = []
  const allSvgs = tempDiv.querySelectorAll('svg')
  allSvgs.forEach((svg, index) => {
    // 创建唯一占位符
    const placeholderId = `__SVG_PLACEHOLDER_${index}__`
    const wrapper = document.createElement('div')
    wrapper.className = 'svg-placeholder'
    wrapper.dataset.placeholder = placeholderId
    svg.replaceWith(wrapper)
    // 记录转换 Promise
    const promise = convertSvgToPngBase64(svg).then((base64) => ({
      placeholderId,
      base64,
      width: svg.width?.baseVal?.value || 700,
      height: svg.height?.baseVal?.value || 600,
    }))
    svgPromises.push(promise)
    svgPlaceholders.push(placeholderId)
  })

  parseHtmlElement(tempDiv.children, elements, null)

  if (svgPromises.length > 0) {
    const svgImages = await Promise.all(svgPromises)

    // 替换占位符为图片段落
    svgImages.forEach(({ placeholderId, base64, width, height }) => {
      const targetPara = svgMap.get(placeholderId)
      if (targetPara) {
        // 如果图片过宽，则按比例缩放
        let finalWidth = width
        let finalHeight = height
        const widthRatio = MAX_A4_WIDTH / width
        const heightRatio = MAX_A4_HEIGHT / height
        // 2. 如果图片超过了任一限制
        if (width > MAX_A4_WIDTH || height > MAX_A4_HEIGHT) {
          // 取较小的缩放比，确保宽和高都能缩进 A4 范围内
          const ratio = Math.min(widthRatio, heightRatio)
          finalWidth = width * ratio
          finalHeight = height * ratio
        }

        Object.assign(
          targetPara,
          new docx.Paragraph({
            children: [
              new docx.ImageRun({
                data: base64ToArrayBuffer(base64),
                type: 'png',
                transformation: { width: finalWidth, height: finalHeight },
              }),
            ],
            spacing: { after: 200 },
          }),
        )
      }
    })
  }

  if (props.msg.chartDataStr) {
    // 获取图表图片
    const base64Image = answerContentRef.value?.getChartDataURL?.()
    if (!base64Image) return elements
    elements.push(
      new docx.Paragraph({
        children: [
          new docx.ImageRun({
            data: base64ToArrayBuffer(base64Image),
            type: 'png',
            transformation: {
              width: 500, // 图片宽度，精准控制
              height: 260, // 图片高度，精准控制
            },
          }),
        ],
      }),
    )
  }

  return elements
}

function parseInlineElements(node, currentStyle = {}) {
  let runs = []
  const childNodes = node.childNodes
  childNodes.forEach(async (child) => {
    // 合并当前层级的样式
    const newStyle = { ...currentStyle }

    if (child.nodeType === Node.TEXT_NODE) {
      const text = child.textContent
      if (text) {
        // 只有在最终的文本节点才实例化 TextRun
        runs.push(
          new docx.TextRun({
            text: text,
            ...newStyle,
          }),
        )
      }
    } else if (child.nodeType === Node.ELEMENT_NODE) {
      const tagName = child.tagName.toLowerCase()

      // 根据标签更新样式对象
      if (tagName === 'strong' || tagName === 'b') newStyle.bold = true
      if (tagName === 'em' || tagName === 'i') newStyle.italics = true
      if (tagName === 'u') newStyle.underline = {}
      if (tagName === 'span') {
        if (child.style.color) newStyle.color = child.style.color
        if (child.style.fontSize) {
          // 简单的字号处理，docx size 单位是半磅 (pt * 2)
          const pt = parseInt(child.style.fontSize)
          if (!isNaN(pt)) newStyle.size = pt * 2
        }
      }
      if (tagName === 'code') {
        newStyle.font = 'Courier New'
        newStyle.shading = { fill: 'F0F0F0' }
      }

      // 递归调用，将当前样式往子节点传递
      const nestedRuns = parseInlineElements(child, newStyle)
      runs.push(...nestedRuns)
    }
  })
  return runs
}

async function convertKatexToPng(el) {
  // 1. 创建克隆，确保不影响页面显示
  const clone = el.cloneNode(true)

  // 2. 关键：手动把页面上 KaTeX 的 CSS 注入到克隆节点的 Style 属性中
  // 或者确保 html-to-image 能够抓取到样式
  document.body.appendChild(clone) // 暂时挂载以获取样式上下文

  try {
    const dataUrl = await htmlToImage.toPng(clone, {
      backgroundColor: '#ffffff',
      pixelRatio: 3, // 提高公式清晰度，公式小符号多，倍率要高
      skipAutoScale: true,
      // 这里的 fontEmbedCSS 是解决空白的关键
      fontEmbedCSS: true,
    })
    return dataUrl
  } finally {
    document.body.removeChild(clone)
  }
}

function parseContentContainer(containerNode, result, listType, listMarker) {
  const childNodes = Array.from(containerNode.childNodes)

  // 收集连续的行内元素，直到遇到块级元素
  let inlineBuffer = []

  const flushBuffer = () => {
    if (inlineBuffer.length > 0) {
      const pOptions = { children: [...inlineBuffer], spacing: { after: 200 } }

      // 如果是列表的第一行，加上列表样式
      if (listType === 'bullet') {
        pOptions.bullet = { level: 0 }
      } else if (listType === 'number' && listMarker) {
        // 如果没有配置 docx 的 numbering 实例，直接在 TextRun 前加前缀
        pOptions.children.unshift(new docx.TextRun({ text: listMarker }))
      }

      result.push(new docx.Paragraph(pOptions))
      inlineBuffer = []
      // 标记已处理，后续行不再加列表头
      listType = null
      listMarker = null
    }
  }

  childNodes.forEach(async (node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      if (node.textContent.trim()) {
        inlineBuffer.push(new docx.TextRun({ text: node.textContent }))
      }
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const tagName = node.tagName.toLowerCase()
      const isBlock = ['p', 'ul', 'ol', 'table', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(tagName)

      if (isBlock) {
        flushBuffer() // 先把之前的行内文字刷成一段
        parseHtmlElement([node], result, null) // 递归处理块级
      } else {
        inlineBuffer.push(...parseInlineElements(node))

        // if (tagName === 'span' && node.classList.contains('katex')) {
        //   const dataUrl = await convertKatexToPng(node) // 异步转图

        //   inlineBuffer.push(
        //     new docx.Paragraph({
        //       children: [
        //         new docx.ImageRun({
        //           data: base64ToArrayBuffer(dataUrl),
        //           type: 'png',
        //           transformation: { width: 200, height: 80 },
        //         }),
        //       ],
        //     }),
        //   )
        // } else {
        //   inlineBuffer.push(...parseInlineElements(node))
        // }
        // 行内元素 (span, b, i, img 等)
      }
    }
  })

  flushBuffer() // 处理剩余的行内元素
}

async function parseHtmlElement(list, result, order) {
  for (let i = 0; i < list.length; i++) {
    let child = list[i]
    const tagName = child.tagName.toLowerCase()

    switch (tagName) {
      case 'h1':
      case 'h2':
      case 'h3':
      case 'h4':
      case 'h5':
      case 'h6':
        result.push(
          new docx.Paragraph({
            heading: docx.HeadingLevel[`HEADING_${tagName.charAt(1)}`],
            children: parseInlineElements(child),
            spacing: { after: 200 },
          }),
        )
        break

      case 'p':
        const pChildren = parseInlineElements(child)
        // 处理有序列表传递过来的序号
        if (order && i === 0) {
          pChildren.unshift(new docx.TextRun({ text: `${order}. `, bold: true }))
        }
        result.push(
          new docx.Paragraph({
            children: pChildren,
            spacing: { after: 200 },
          }),
        )
        break

      case 'ul':
      case 'ol':
        Array.from(child.children).forEach((li, index) => {
          if (li.tagName.toLowerCase() === 'li') {
            // 无论内部有什么，我们都把 li 当作一个容器来解析
            // 传入 bullet/numbering 信息，让子元素的第一行带上列表标记
            const listType = tagName === 'ul' ? 'bullet' : 'number'
            const listMarker = tagName === 'ol' ? `${index + 1}. ` : null

            parseContentContainer(li, result, listType, listMarker, svgMap)
          }
        })

        // Array.from(child.children).forEach((li, index) => {
        //   if (li.tagName.toLowerCase() === 'li') {
        //     // 关键：检查 LI 内部是否有块级元素（如 LI 里面嵌套了 P 或另一个 UL）
        //     const hasBlockElement = Array.from(li.children).some((el) =>
        //       ['p', 'ul', 'ol', 'table', 'div'].includes(el.tagName.toLowerCase()),
        //     )

        //     if (hasBlockElement) {
        //       // 如果有嵌套块级元素，递归处理其子节点
        //       parseHtmlElement(li.children, result, tagName === 'ol' ? index + 1 : null)
        //     } else {
        //       // 如果只是纯文本或行内元素
        //       result.push(
        //         new docx.Paragraph({
        //           children: parseInlineElements(li),
        //           bullet: tagName === 'ul' ? { level: 0 } : undefined,
        //           numbering: tagName === 'ol' ? { reference: 'my-crazy-numbering', level: 0 } : undefined, // 注意：OL 最好配合 docx 的 numbering 配置
        //           // 如果不配置 numbering，可以使用你之前的手动加序号方式：
        //           text: tagName === 'ol' ? `${index + 1}. ${li.textContent}` : undefined,
        //         }),
        //       )
        //     }
        //   }
        // })
        break

      case 'table':
        const rows = Array.from(child.querySelectorAll('tr')).map((tr) => {
          const cells = Array.from(tr.querySelectorAll('td, th')).map((td) => {
            const cellContent = []
            // 表格单元格也可以使用通用容器解析
            parseContentContainer(td, cellContent, null, null, svgMap)
            return new docx.TableCell({ children: cellContent })
          })
          return new docx.TableRow({ children: cells })
        })
        result.push(
          new docx.Table({
            width: { size: 100, type: docx.WidthType.PERCENTAGE },
            rows: rows,
          }),
        )
        // const rows = Array.from(child.querySelectorAll('tr')).map((tr) => {
        //   const cells = Array.from(tr.querySelectorAll('td, th')).map((td) => {
        //     // 表格单元格内部也需要递归处理
        //     const cellContent = []
        //     parseHtmlElement(td.children, cellContent)
        //     // 如果单元格是纯文本没有子标签
        //     if (cellContent.length === 0) {
        //       cellContent.push(new docx.Paragraph({ text: td.textContent }))
        //     }
        //     return new docx.TableCell({ children: cellContent })
        //   })
        //   return new docx.TableRow({ children: cells })
        // })
        // result.push(
        //   new docx.Table({
        //     width: { size: 100, type: docx.WidthType.PERCENTAGE },
        //     rows: rows,
        //   }),
        // )
        break

      case 'div':
        if (child.classList.contains('svg-placeholder')) {
          const placeholderId = child.dataset.placeholder
          // 创建一个空的段落作为“槽位”
          const placeholderPara = new docx.Paragraph({
            children: [new docx.TextRun({ text: placeholderId })],
          })
          // 【关键】将这个对象引用存入我们的数组，而不是存 ID 字符串
          svgMap.set(placeholderId, placeholderPara)
          result.push(placeholderPara)
        } else {
          // div 作为一个容器，继续递归解析它的子元素
          parseHtmlElement(child.children, result)
        }
        break

      default:
        if (child.children.length > 0) {
          parseHtmlElement(child.children, result)
        } else if (child.textContent.trim()) {
          result.push(new docx.Paragraph({ text: child.textContent, spacing: { after: 200 } }))
        }
        break
    }
  }
  return result
}

function exportRichHtmlToWord(element, filename = 'document') {
  const parser = new DOMParser()
  const doc = parser.parseFromString(element, 'text/html')

  // // 构建完整的 HTML 内容（包含 Word 特定标记和样式）
  // const fullHtml = `
  //   <html xmlns:o="urn:schemas-microsoft-com:office:office"
  //         xmlns:w="urn:schemas-microsoft-com:office:word"
  //         xmlns="http://www.w3.org/TR/REC-html40">
  //     <head>
  //       <meta charset="UTF-8">
  //       <!-- Word页面设置 -->
  //       <xml>
  //         <w:WordDocument>
  //           <w:View>Print</w:View>
  //           <w:Zoom>100</w:Zoom>
  //           <w:DoNotOptimizeForBrowser/>
  //         </w:WordDocument>
  //       </xml>
  //       <style>
  //         @page {
  //           /* A4尺寸：21cm × 29.7cm */
  //           size: 21cm 29.7cm;
  //           margin: 1in; /* 1英寸边距 */
  //           mso-page-orientation: portrait; /* 纵向 */
  //         }
  //         body {
  //           font-family: SimSun, "宋体", serif;
  //           font-size: 12pt;
  //           mso-font-alt: "Times New Roman";
  //         }
  //         /* 强制Word识别页面设置 */
  //         @media print {
  //           @page {
  //             size: A4 portrait;
  //           }
  //         }
  //       </style>
  //     </head>
  //     <body>
  //       ${element}
  //     </body>
  //   </html>
  // `
  // // 创建 Blob 并下载
  // const blob = new Blob([fullHtml], {
  //   type: 'application/msword;charset=utf-8',
  // })
  // const url = URL.createObjectURL(blob)
  // const a = document.createElement('a')
  // a.href = url
  // a.download = `${filename}.doc`
  // document.body.appendChild(a)
  // a.click()
  // // 清理
  // setTimeout(() => {
  //   document.body.removeChild(a)
  //   URL.revokeObjectURL(url)
  // }, 0)
}

// // 使用示例
// document.getElementById('convertBtn').addEventListener('click', () => {
//   const html = `
//     <div>
//       <h1>HTML 转 Word 示例</h1>
//       <p>这是一段包含 <strong>粗体</strong> 和 <em>斜体</em> 的文本。</p>
//       <ul>
//         <li>列表项 1</li>
//         <li>列表项 2</li>
//       </ul>
//       <table border="1">
//         <tr><th>表头1</th><th>表头2</th></tr>
//         <tr><td>单元格1</td><td>单元格2</td></tr>
//       </table>
//     </div>
//   `;

//   convertHtmlToWord(html, 'my-document');
// });

const foldStatus = ref(true)
function switchFold() {
  foldStatus.value = !foldStatus.value
}

const exportPdf = (e) => {
  // if (props.isAnswering) {
  //   ElMessage.warning('请等待智能体回复完毕')
  //   return
  // }
  // const source = e?.target?.parentNode?.parentNode?.children[0]
  // if (source) {
  //   const opt = {
  //     margin: 1,
  //     filename: '结果.pdf',
  //     image: { type: 'jpeg', quality: 0.98 },
  //     html2canvas: { scale: 2 },
  //     jsPDF: {
  //       unit: 'in',
  //       format: 'letter',
  //       orientation: 'portrait',
  //     },
  //   }
  //   html2pdf().set(opt).from(source).save()
  // }
}

// const displayContent = ref('')

onMounted(() => {
  // displayContent.value = md.render(testMd)
  console.log(props.msg)
})
</script>

<style lang="less" scoped>
.ai-msg {
  width: 100%;
  position: relative;
  display: flex;
  justify-content: flex-start;
  :deep(.ai-msg__inner) {
    h1,
    h2,
    h3,
    h4,
    h5 {
      line-height: 2;
    }
    h1 {
      font-size: 20px;
    }
    h2 {
      font-size: 18px;
    }
    h3,
    h4,
    h5 {
      font-size: 14px;
    }

    img {
      // max-width: 60%;
    }
    ul,
    ol {
      padding-left: 40px !important;
    }

    table {
      border-collapse: collapse; /* 合并边框 */
      border: 1px solid #d6d9e5;
      min-width: 550px;
    }
    thead tr {
      background-color: #ebecf1;
    }
    tr {
      // display: flex;
      // width: 100%;
    }
    th,
    td {
      // flex: 1;
      font-size: 12px;
      border: 1px solid #d6d9e5;
      padding: 4px 6px; /* 增加内边距让内容更美观 */
      text-align: center;
    }
  }
}
@msg_ml: 50px;
.ai-msg-wrap {
  border-radius: 10px;
  background: #fff;
  width: calc(100% - 50px);
}
.ai-msg__inner {
  width: auto;
}
.ai-avatar-wrap {
  position: absolute;
  left: 0px;
  top: 0;
  background-color: #fff;

  width: 30px;
  height: 30px;
  border-radius: 50%;
}
.ai-avatar {
  margin-right: 20px;
}
.each-db-content {
  white-space: pre-wrap;
}

.ai-thinking {
  position: relative;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  perspective: 800px;
  top: 6px;
  left: 12px;
}

.inner {
  position: absolute;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.inner.one {
  left: 0%;
  top: 0%;
  animation: rotate-one 1s linear infinite;
  border-bottom: 3px solid #c0c4cc;
}

.inner.two {
  right: 0%;
  top: 0%;
  animation: rotate-two 1s linear infinite;
  border-right: 3px solid #c0c4cc;
}

.inner.three {
  right: 0%;
  bottom: 0%;
  animation: rotate-three 1s linear infinite;
  border-top: 3px solid #c0c4cc;
}

// .export-pdf-btn {
//   padding: 5px 10px;
//   // border-radius:;
// }
.export-btn {
  position: absolute;
  top: 12px;
  right: 20px;
}

.dbs-box {
  // position: absolute;
  bottom: 0;
  margin-top: 20px;
  background: #fafafb;
  overflow: hidden;
  .db-title {
    color: #30384a;
    font-size: 12px;
    border-radius: 6px;
    background: #f2f3f7;
    line-height: 20px;
    padding: 8px 10px;
    display: flex;
    position: relative;
    cursor: pointer;
    user-select: none;

    span {
      color: #1957e4;
      font-weight: bold;
      display: flex;
      align-items: center;
    }
    svg {
      width: 14px;
      height: 14px;
      margin-right: 2px;
      position: relative;
      top: 1px;
    }
    .fold-btn {
      position: absolute;
      right: 10px;
      top: 11px;
      cursor: pointer;

      svg {
        transition: transform 0.3s linear;
      }
    }
  }

  .db-list {
    // padding: 10px;
    font-size: 12px;
    color: #848995;
    max-height: 400px;
    overflow: auto;
  }
  .list-inner {
    padding: 10px;
  }
  .each-db {
    margin-bottom: 14px;
    &:last-child {
      margin-bottom: 0;
    }
  }
  .each-db-title {
    color: #30384a;
    // margin-bottom: 10px;
    // margin-top: 10px;
    display: flex;
    align-items: center;
    span {
      font-weight: bold;
    }
    .el-link {
      font-size: 12px;
      margin-left: 10px;
    }
  }
  .each-db-content {
    padding: 10px;
    background: #fff;
    border-radius: 6px;
    margin-top: 10px;
  }
}

.tool-btn {
  margin-top: 16px;
  display: flex;
  position: relative;
  svg {
    width: 16px;
    height: 16px;
    margin-right: 12px;
    cursor: pointer;
  }
  svg + .is-active {
    :deep(path) {
      stroke: var(--el-color-primary) !important;
      fill: #fff;
    }
  }
  .msg-date {
    position: absolute;
    top: 0;
    font-size: 12px;
    line-height: 16px;
    right: 0;
    color: #666666;
  }
  .active-icon {
    width: 15px;
    height: 15px;
  }
}

.output-tip {
  text-align: right;
  font-size: 12px;
}

@keyframes rotate-one {
  0% {
    transform: rotateX(35deg) rotateY(-45deg) rotateZ(0deg);
  }
  100% {
    transform: rotateX(35deg) rotateY(-45deg) rotateZ(360deg);
  }
}

@keyframes rotate-two {
  0% {
    transform: rotateX(50deg) rotateY(10deg) rotateZ(0deg);
  }
  100% {
    transform: rotateX(50deg) rotateY(10deg) rotateZ(360deg);
  }
}

@keyframes rotate-three {
  0% {
    transform: rotateX(35deg) rotateY(55deg) rotateZ(0deg);
  }
  100% {
    transform: rotateX(35deg) rotateY(55deg) rotateZ(360deg);
  }
}

.think-animate {
  display: flex;
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-left: 12px;
  &::after {
    content: '...'; /* 省略号内容 */
    display: block;
    animation: ellipsis-animation 1s linear infinite; /* 动画名称、持续时间和循环次数 */
  }
}
@keyframes ellipsis-animation {
  0% {
    content: '...';
  }
  33% {
    content: '.. ';
  }
  66% {
    content: '.  ';
  }
  100% {
    content: '...';
  }
}
</style>
