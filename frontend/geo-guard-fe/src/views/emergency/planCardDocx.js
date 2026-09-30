/**
 * 预案卡 → Word 文档
 *
 * 预案卡在弹窗里是 12 列 CSS 网格（.form-grid），DOM 中并没有 <table>，
 * 走通用的 exportDomToDocx 会把「标签 / 取值」逐个拍平成普通段落，表格线与字段并排全部丢失。
 * 因此这里直接按预案卡数据结构生成 Word。
 *
 * 表格结构：正文只有一张表，用 36 列网格——标签固定 6 列、取值按 row.span×3 列，
 * 与预览的 12 列网格比例一致，同时让内容块的子列能整除分配。
 * 候选撤离路线 / 责任分工 / 分阶段行动这类内容块**不使用嵌套表格**，而是展开成同一张表的多行，
 * 标签列纵向合并跨行；否则 Word 里会出现表格套表格（外框套内框，观感很差）。
 */
import {
  AlignmentType,
  BorderStyle,
  Document,
  ImageRun,
  Packer,
  Paragraph,
  Table,
  TableCell,
  TableRow,
  TableLayoutType,
  TextRun,
  VerticalAlign,
  WidthType,
} from 'docx'

/* 正文 36 列网格：标签 6 列，取值每 span 占 3 列（如 span=10 即 30 列，加标签正好 36 列） */
const GRID_COLUMNS = 36
const LABEL_COLUMNS = 6
const SPAN_UNIT = 3
const DEFAULT_VALUE_SPAN = 10
/* A4 正文宽度（11906 - 左右各 1440 twip）均分为 36 列，配合固定布局保证列宽与预览一致 */
const COLUMN_WIDTH = 250
const COLUMN_WIDTHS = Array.from({ length: GRID_COLUMNS }, () => COLUMN_WIDTH)

/* 内容块子列：各列列宽 + 对齐方式（比例对应预览里的子表） */
const ROUTE_COLUMNS = {
  head: ['方案', '路径', '距离', '预计用时'],
  spans: [6, 12, 6, 6],
  aligns: ['center', 'left', 'center', 'center'],
}
const DUTY_COLUMNS = {
  head: ['责任身份', '姓名', '联系电话', '点到点任务'],
  spans: [6, 3, 6, 15],
  aligns: ['center', 'center', 'center', 'left'],
}
const STEP_COLUMNS = {
  spans: [10, 10, 10],
  aligns: ['left', 'left', 'left'],
}

/* 表格线：与预览的 #232323 细线一致 */
const LINE = { style: BorderStyle.SINGLE, size: 4, color: '232323' }
const CELL_BORDERS = { top: LINE, bottom: LINE, left: LINE, right: LINE }
const TABLE_BORDERS = { ...CELL_BORDERS, insideHorizontal: LINE, insideVertical: LINE }
const CELL_MARGINS = { top: 60, bottom: 60, left: 60, right: 60 }

/* 字号为 Word 半磅值：21=10.5pt（五号）、18=9pt、24=12pt、32=16pt */
const FONT_FAMILY = '宋体'
const FONT_BODY = 21
const FONT_SMALL = 18
const FONT_SECTION = 24
const FONT_TITLE = 32

/* 字段类型 → 底色 / 字色，与预览的 pending / warning 一致 */
const TYPE_STYLE = {
  pending: { fill: 'FFF6E9', color: 'B86807' },
  warning: { fill: 'FFF0F0', color: 'BC3434' },
}

/* 来源标记 → 底色 / 字色，与预览的 .mark 一致 */
const MARK_STYLE = {
  input: { fill: 'E9F4FF', color: '1071D0' },
  db: { fill: 'EAF8F2', color: '087E59' },
  gis: { fill: 'E9F7FF', color: '087EA8' },
  ai: { fill: 'F0ECFF', color: '6652CC' },
}

const text = (value, options = {}) =>
  new TextRun({ text: String(value ?? ''), font: FONT_FAMILY, size: FONT_BODY, ...options })

/** 来源标记：预览里是行尾的小标签，Word 里用带底色的文本呈现 */
const markRuns = (mark) => {
  if (!mark?.text) return []
  const style = MARK_STYLE[mark.type] || { fill: 'F2F4F7', color: '5B6672' }
  return [text(mark.text, { size: FONT_SMALL, color: style.color, shading: { fill: style.fill } })]
}

const blobToBase64 = (blob) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })

/** 图片地址（dataURL 或 http 地址）→ docx 需要的 { type, data } */
const toImageData = async (src) => {
  const matched = /^data:image\/(\w+)/.exec(src)?.[1] || /\.(jpe?g|png|gif|bmp)(\?|$)/i.exec(src)?.[1] || 'png'
  const raw = matched.toLowerCase() === 'jpeg' ? 'jpg' : matched.toLowerCase()
  const type = ['jpg', 'png', 'gif', 'bmp'].includes(raw) ? raw : 'png'
  const base64 = src.startsWith('data:') ? src : await blobToBase64(await (await fetch(src)).blob())
  const binary = atob(base64.split(',')[1] || '')
  const data = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    data[i] = binary.charCodeAt(i)
  }
  return { type, data }
}

/** 取值单元格：普通字段（文本）与图片用，内容块不再走这里 */
const valueCell = async (row, span) => {
  const children = []

  if (row.image) {
    const { type, data } = await toImageData(row.image)
    children.push(
      new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [
          new ImageRun({
            type,
            data,
            /* 出图为 16:9，宽度按取值区可用宽度给，避免超出单元格 */
            transformation: { width: 540, height: 304 },
          }),
        ],
      }),
    )
  } else {
    const style = TYPE_STYLE[row.type] || {}
    const alignment = row.center
      ? AlignmentType.CENTER
      : row.type === 'long'
        ? AlignmentType.JUSTIFIED
        : AlignmentType.LEFT
    children.push(
      new Paragraph({
        alignment,
        children: [
          text(row.value || row.imageEmpty || '--', { color: style.color }),
          ...markRuns(row.mark),
        ],
      }),
    )
  }

  return new TableCell({
    columnSpan: span * SPAN_UNIT,
    borders: CELL_BORDERS,
    shading: TYPE_STYLE[row.type] ? { fill: TYPE_STYLE[row.type].fill } : undefined,
    verticalAlign: row.center ? VerticalAlign.CENTER : VerticalAlign.TOP,
    margins: CELL_MARGINS,
    children,
  })
}

/** 标签单元格：内容块的标签纵向合并跨行（rowSpan > 1 时才写合并） */
const labelCell = (label, { mark = null, rowSpan = 1 } = {}) =>
  new TableCell({
    columnSpan: LABEL_COLUMNS,
    ...(rowSpan > 1 ? { rowSpan } : {}),
    borders: CELL_BORDERS,
    shading: { fill: 'FAFAFA' },
    verticalAlign: VerticalAlign.CENTER,
    margins: CELL_MARGINS,
    children: [
      new Paragraph({ alignment: AlignmentType.CENTER, children: [text(label)] }),
      ...(mark ? [new Paragraph({ alignment: AlignmentType.CENTER, children: markRuns(mark) })] : []),
    ],
  })

const emptyCell = (span) =>
  new TableCell({
    columnSpan: span,
    borders: CELL_BORDERS,
    margins: CELL_MARGINS,
    children: [new Paragraph({ children: [] })],
  })

/** 内容块的子单元格：按子列列宽跨列，表头带底色 */
const blockCell = ({ value, columnSpan, align = 'left', head = false }) =>
  new TableCell({
    columnSpan,
    borders: CELL_BORDERS,
    shading: head ? { fill: 'F2F6F9' } : undefined,
    verticalAlign: VerticalAlign.CENTER,
    margins: CELL_MARGINS,
    children: [
      new Paragraph({
        alignment: align === 'center' ? AlignmentType.CENTER : AlignmentType.LEFT,
        children: [text(value, { bold: head, size: head ? FONT_BODY : FONT_SMALL })],
      }),
    ],
  })

/**
 * 内容块展开成正文表格的多行：第一行是子表头（标签纵向合并跨全部数据行），其余是数据行。
 * 这样候选撤离路线 / 责任分工等仍是「一张表」的一部分，不会在单元格里再套一张表。
 */
const blockRows = (row, { head, spans, aligns }, items) => {
  const rows = [
    new TableRow({
      tableHeader: true,
      children: [
        labelCell(row.label, { mark: row.mark, rowSpan: items.length + 1 }),
        ...head.map((value, index) =>
          blockCell({ value, columnSpan: spans[index], align: aligns[index], head: true }),
        ),
      ],
    }),
  ]

  items.forEach((values) => {
    rows.push(
      new TableRow({
        children: values.map((value, index) =>
          blockCell({ value, columnSpan: spans[index], align: aligns[index] }),
        ),
      }),
    )
  })

  return rows
}

/** 内容块行 → 多行；普通字段行不在这里处理 */
const buildBlockRows = (row) => {
  if (row.routes?.length) {
    return blockRows(row, ROUTE_COLUMNS, row.routes.map((item) => [item.plan, item.path, item.distance, item.duration]))
  }
  if (row.duties?.length) {
    return blockRows(row, DUTY_COLUMNS, row.duties.map((item) => [item.role, item.name, item.phone, item.task]))
  }
  if (row.steps?.length) {
    /* 分阶段行动：阶段名做子表头，动作做成一行，与预览的「标题在上、内容在下」一致 */
    return blockRows(
      row,
      { ...STEP_COLUMNS, head: row.steps.map((item) => item.title) },
      [row.steps.map((item) => item.text)],
    )
  }
  return []
}

const isBlockRow = (row) => Boolean(row.routes?.length || row.duties?.length || row.steps?.length)

/** 把字段按「标签 6 列 + 取值 span×3 列」装进 36 列：装不下换行；内容块独占若干行 */
const packLines = (rows = []) => {
  const lines = []
  let fields = []
  let used = 0

  const flush = () => {
    if (fields.length) {
      lines.push({ fields, used })
      fields = []
      used = 0
    }
  }

  rows.forEach((row) => {
    if (isBlockRow(row)) {
      flush()
      lines.push({ block: row })
      return
    }
    const span = Number(row.span) || DEFAULT_VALUE_SPAN
    const width = LABEL_COLUMNS + span * SPAN_UNIT
    if (fields.length && used + width > GRID_COLUMNS) {
      flush()
    }
    fields.push({ row, span })
    used += width
  })

  flush()
  return lines
}

const sectionRow = (section) =>
  new TableRow({
    children: [
      new TableCell({
        columnSpan: GRID_COLUMNS,
        borders: CELL_BORDERS,
        shading: { fill: 'EDF4FA' },
        verticalAlign: VerticalAlign.CENTER,
        margins: CELL_MARGINS,
        children: [
          new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [
              text(section.title, { bold: true, size: FONT_SECTION, color: '2B5278' }),
              ...(section.note ? [text(`  ${section.note}`, { size: FONT_SMALL, color: '6E7F90' })] : []),
            ],
          }),
        ],
      }),
    ],
  })

const buildFieldRow = async ({ fields, used }) => {
  const cells = []

  for (const { row, span } of fields) {
    cells.push(labelCell(row.label))
    cells.push(await valueCell(row, span))
  }

  /* 一行没铺满 36 列时补空格子，避免 Word 里表格右侧出现缺口 */
  if (used < GRID_COLUMNS) {
    cells.push(emptyCell(GRID_COLUMNS - used))
  }

  return new TableRow({ children: cells })
}

/**
 * 生成预案卡 Word 文档
 * @param {object} data 预案卡数据（PlanDetailDialog 的 data：{ title, subtitle, stage, sections, note }）
 * @returns {Promise<Blob>} docx 文件
 */
export const buildPlanCardDocx = async (data = {}) => {
  const children = []

  children.push(
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 120 },
      children: [text(data.title || '临时处置预案卡', { bold: true, size: FONT_TITLE })],
    }),
  )
  if (data.subtitle) {
    children.push(
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 80 },
        children: [text(data.subtitle, { size: FONT_SMALL, color: '778696' })],
      }),
    )
  }
  if (data.stage) {
    children.push(
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 160 },
        shading: { fill: 'FFF0F0' },
        children: [text(data.stage, { bold: true, color: 'C43636' })],
      }),
    )
  }

  const rows = []
  for (const section of data.sections || []) {
    rows.push(sectionRow(section))
    for (const line of packLines(section.rows)) {
      if (line.block) {
        rows.push(...buildBlockRows(line.block))
      } else {
        rows.push(await buildFieldRow(line))
      }
    }
  }

  if (rows.length) {
    children.push(
      new Table({
        rows,
        width: { size: 100, type: WidthType.PERCENTAGE },
        columnWidths: COLUMN_WIDTHS,
        layout: TableLayoutType.FIXED,
        borders: TABLE_BORDERS,
      }),
    )
  }

  if (data.note) {
    children.push(
      new Paragraph({
        spacing: { before: 200 },
        children: [text(data.note, { size: FONT_SMALL, color: '728294' })],
      }),
    )
  }

  const doc = new Document({
    styles: { default: { document: { run: { font: FONT_FAMILY, size: FONT_BODY } } } },
    sections: [{ children }],
  })

  return Packer.toBlob(doc)
}
