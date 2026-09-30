import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  ImageRun,
  AlignmentType,
  Table,
  TableRow,
  TableCell,
  WidthType,
  BorderStyle
} from "docx"

// px → fontSize
const pxToHalfPoint = (px) => Math.round(px * 1.5)

// px → docx spacing（twip）
const pxToTwip = (px) => Math.round(px * 15)

// rgb 转 hex
const rgbToHex = (color) => {
  if (!color) return undefined

  if (color.startsWith('#')) {
    const hex = color.replace('#', '').trim()

    if (hex.length === 3) {
      return hex.split('').map(c => c + c).join('')
    }

    if (hex.length === 6) return hex

    return undefined
  }

  const match = color.match(/\d+/g)
  if (!match || match.length < 3) return undefined

  const hex = match
    .slice(0, 3)
    .map(n => {
      const num = Number(n)
      return isNaN(num) ? '00' : num.toString(16).padStart(2, '0')
    })
    .join('')

  return hex.length === 6 ? hex : undefined
}

// 解析 style
const parseStyle = (style) => {
  const obj = {}
  style.split(';').forEach(item => {
    const [key, val] = item.split(':').map(s => s && s.trim())
    if (!key) return
    obj[key] = val
  })
  return obj
}

// 对齐
const getAlign = (align) => {
  switch (align) {
    case 'center': return AlignmentType.CENTER
    case 'right': return AlignmentType.RIGHT
    default: return AlignmentType.LEFT
  }
}

// margin 解析（支持简写）
const parseMargin = (style) => {
  const result = { top: 0, right: 0, bottom: 0, left: 0 }

  if (style.margin) {
    const vals = style.margin.split(' ').map(v => parseFloat(v))

    if (vals.length === 1) {
      result.top = result.right = result.bottom = result.left = vals[0]
    } else if (vals.length === 2) {
      result.top = result.bottom = vals[0]
      result.right = result.left = vals[1]
    } else if (vals.length === 3) {
      result.top = vals[0]
      result.right = result.left = vals[1]
      result.bottom = vals[2]
    } else if (vals.length === 4) {
      [result.top, result.right, result.bottom, result.left] = vals
    }
  }

  // 单独属性覆盖
  if (style['margin-top']) result.top = parseFloat(style['margin-top'])
  if (style['margin-bottom']) result.bottom = parseFloat(style['margin-bottom'])
  if (style['margin-left']) result.left = parseFloat(style['margin-left'])
  if (style['margin-right']) result.right = parseFloat(style['margin-right'])

  // 确保 margin 值不为 0，若为 0 则设置为最小值 5
  if (result.top === 0) result.top = 5
  if (result.right === 0) result.right = 5
  if (result.bottom === 0) result.bottom = 5
  if (result.left === 0) result.left = 5

  return result
}

// 图片 base64
const base64ToUint8Array = (base64) => {
  const base64Data = base64.includes(',') ? base64.split(',')[1] : base64
  const binary = atob(base64Data)
  const len = binary.length
  const buffer = new Uint8Array(len)
  for (let i = 0; i < len; i++) {
    buffer[i] = binary.charCodeAt(i)
  }
  return buffer
}

const blobToBase64 = (blob) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

const imageSrcToBase64 = async (src) => {
  if (src.startsWith('data:')) return src

  const response = await fetch(src)
  if (!response.ok) {
    throw new Error(`Image load failed: ${response.status} ${response.statusText}`)
  }

  return blobToBase64(await response.blob())
}

// 用于记录当前有序列表序号
let listCounters = []

const createCellBorders = () => ({
  top: { style: BorderStyle.SINGLE, size: 6, color: '333333' },
  bottom: { style: BorderStyle.SINGLE, size: 6, color: '333333' },
  left: { style: BorderStyle.SINGLE, size: 6, color: '333333' },
  right: { style: BorderStyle.SINGLE, size: 6, color: '333333' },
})

const parseTableCell = async (cellNode, parentStyle = {}) => {
  const children = []
  let currentRuns = []

  for (let child of cellNode.childNodes) {
    const sub = await parseNode(child, parentStyle)

    for (let item of sub) {
      if (item instanceof TextRun || item instanceof ImageRun) {
        currentRuns.push(item)
      } else if (item instanceof Paragraph || item instanceof Table) {
        if (currentRuns.length) {
          children.push(new Paragraph({ children: currentRuns }))
          currentRuns = []
        }
        children.push(item)
      }
    }
  }

  if (currentRuns.length) {
    children.push(new Paragraph({ children: currentRuns }))
  }

  if (!children.length) {
    children.push(new Paragraph({ children: [new TextRun('')] }))
  }

  return new TableCell({
    children,
    borders: createCellBorders(),
    margins: {
      top: pxToTwip(6),
      bottom: pxToTwip(6),
      left: pxToTwip(8),
      right: pxToTwip(8),
    },
  })
}

const parseTableRows = async (node, parentStyle = {}) => {
  const rows = []

  for (let child of node.children || []) {
    if (child.tagName === 'TR') {
      const cells = []

      for (let cellNode of child.children || []) {
        if (cellNode.tagName === 'TD' || cellNode.tagName === 'TH') {
          const cellStyle = {
            ...parentStyle,
            bold: parentStyle.bold || cellNode.tagName === 'TH',
          }
          cells.push(await parseTableCell(cellNode, cellStyle))
        }
      }

      if (cells.length) {
        rows.push(new TableRow({ children: cells }))
      }
    } else if (child.tagName === 'THEAD' || child.tagName === 'TBODY' || child.tagName === 'TFOOT') {
      rows.push(...await parseTableRows(child, parentStyle))
    }
  }

  return rows
}

const parseNode = async (node, parentStyle = {}, { imgAlignment = AlignmentType.CENTER } = {}) => {
  const paragraphs = []

  // ✅ 文本节点
  if (node.nodeType === 3) {
    const text = node.nodeValue
    if (!text || text.trim() === '') return []

    return [
      new TextRun({
        text,
        size: parentStyle?.fontSize,
        color: parentStyle?.color || '000000',
        bold: !!parentStyle?.bold,
      }),
    ]
  }

  if (node.nodeType !== 1) return []

  const tag = node.tagName

  // ✅ br
  if (tag === 'BR') {
    return [new TextRun({ break: 1 })]
  }

  const rawStyle = parseStyle(node.getAttribute('style') || '')

  const textStyle = {
    fontSize: rawStyle['font-size']
      ? pxToHalfPoint(parseFloat(rawStyle['font-size']))
      : parentStyle.fontSize,
    color: rgbToHex(rawStyle.color) || parentStyle.color,
    bold:
      rawStyle['font-weight'] === 'bold' ||
      Number(rawStyle['font-weight']) >= 600 ||
      parentStyle.bold ||
      tag === 'STRONG' ||
      tag === 'B',
  }

  const margin = parseMargin(rawStyle)

  if (tag === 'TABLE') {
    const rows = await parseTableRows(node, textStyle)
    if (!rows.length) return []

    return [
      new Table({
        rows,
        width: {
          size: 100,
          type: WidthType.PERCENTAGE,
        },
      }),
    ]
  }

  // ✅ 图片（inline）
  if (tag === 'IMG') {
    const src = node.getAttribute('src')
    if (!src) return []

    const width = node.getAttribute('width') || 600
    const base64 = await imageSrcToBase64(src)

    return [
      new ImageRun({
        data: base64ToUint8Array(base64),
        transformation: {
          width: Number(width),
          height: Number(width) * 0.6,
        },
      }),
    ]
  }

  // ✅ inline 标签
  if (tag === 'SPAN' || tag === 'STRONG' || tag === 'B') {
    let runs = []
    for (let child of node.childNodes) {
      const sub = await parseNode(child, textStyle)
      runs.push(...sub)
    }
    return runs
  }

  // ✅ li（保持 block）
  if (tag === 'LI') {
    let childrenRuns = []

    for (let child of node.childNodes) {
      const sub = await parseNode(child, textStyle)
      childrenRuns.push(...sub)
    }

    let prefix = ''
    const parentTag = node.parentNode?.tagName

    if (parentTag === 'OL') {
      if (!listCounters[listCounters.length - 1]) listCounters.push(1)
      prefix = listCounters[listCounters.length - 1] + '. '
      listCounters[listCounters.length - 1]++
    } else if (parentTag === 'UL') {
      prefix = '• '
    }

    return [
      new Paragraph({
        children: [
          new TextRun({
            text: prefix,
            size: textStyle.fontSize,
            color: textStyle.color,
            bold: !!textStyle.bold,
          }),
          ...childrenRuns,
        ],
        spacing: {
          before: pxToTwip(margin.top),
          after: pxToTwip(margin.bottom),
        },
        indent: {
          left: pxToTwip(20),
        },
      }),
    ]
  }

  // ol
  if (tag === 'OL') listCounters.push(1)

  // ✅ ⭐ 核心：顺序流式构建
  let currentRuns = []

  for (let child of node.childNodes) {
    const sub = await parseNode(child, textStyle)

    for (let item of sub) {
      // 👉 inline
      if (item instanceof TextRun || item instanceof ImageRun) {
        currentRuns.push(item)
      }
      // 👉 block
      else if (item instanceof Paragraph || item instanceof Table) {
        // 先收束 inline
        if (currentRuns.length) {
          paragraphs.push(
            new Paragraph({
              children: currentRuns,
              alignment: getAlign(rawStyle['text-align']),
              spacing: {
                before: pxToTwip(margin.top),
                after: pxToTwip(margin.bottom),
              },
            })
          )
          currentRuns = []
        }

        paragraphs.push(item)
      }
    }
  }

  // 收尾
  if (currentRuns.length) {
    paragraphs.push(
      new Paragraph({
        children: currentRuns,
        alignment: getAlign(rawStyle['text-align']),
        spacing: {
          before: pxToTwip(margin.top),
          after: pxToTwip(margin.bottom),
        },
        indent: {
          left: pxToTwip(margin.left),
          right: pxToTwip(margin.right),
        },
      })
    )
  }

  if (tag === 'OL') listCounters.pop()

  return paragraphs
}

// 导出
export const exportDomToDocx = async (dom, imgAlignment = AlignmentType.CENTER) => {
  const children = await parseNode(dom, {},{ imgAlignment })

  const doc = new Document({
    sections: [{ children }]
  })

  return await Packer.toBlob(doc)
}
