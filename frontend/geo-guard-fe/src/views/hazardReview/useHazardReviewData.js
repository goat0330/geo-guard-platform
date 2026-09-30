import { reactive } from 'vue'
import {
  getHazardHandlePage,
  getHazardReviewStatistics,
  judgeHazardPoint,
  reviewHazardPoint,
} from '@/api/hazardReview.js'
import { isAllRange } from '@/utils/dateRange.js'
import { DEFAULT_CLASSIFICATION_BASIS } from './config.js'
import iconSummarySource from '@/assets/imgs/hazardReview/icon-summary-source.png'
import iconSourceCommunity from '@/assets/imgs/hazardReview/icon-source-community.png'
import iconSourceInspection from '@/assets/imgs/hazardReview/icon-source-inspection.png'
import iconSourceOther from '@/assets/imgs/hazardReview/icon-source-other.png'
import iconStatusProcessed from '@/assets/imgs/hazardReview/icon-status-processed.png'
import iconStatusRate from '@/assets/imgs/hazardReview/icon-status-rate.png'
import iconStatusPending from '@/assets/imgs/hazardReview/icon-status-pending.png'

/**
 * 隐患复核页面共享数据源（模块级单例）：
 * 统计概览 / 隐患处理列表 / 智能体详情共用同一份状态，接口返回后在此统一装配。
 * 接口未返回的字段保持空值（0 / [] / null），不落 mock，便于联调时暴露空数据。
 * 字段依据 docs/隐患复核接口联调文档.md
 */

/** 风险等级配色：接口只回等级与数量，颜色按等级固定映射 */
const RISK_LEVEL_COLOR = { A: '#E18282', B: '#F7AE81', C: '#F7D777', D: '#81B6F7' }
const RISK_LEVEL_FALLBACK_COLOR = '#B8C6E3'
/** 风险等级展示顺序：接口已补齐四级，这里兜底顺序与百分比分段起点 */
const RISK_LEVEL_ORDER = ['A', 'B', 'C', 'D']

/** 处理状态：UI 名称 ↔ 接口枚举 */
export const HANDLE_STATUS_OPTIONS = [
  { label: '待复核', value: 'PENDING_REVIEW' },
  { label: '待入库', value: 'PENDING_ARCHIVE' },
  { label: '已复核', value: 'REVIEWED' },
  { label: '已入库', value: 'ARCHIVED' },
]

/** AI 复核状态：仅 SUCCEEDED 可人工确认 */
export const REVIEW_AI_STATUS = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
  CONFIRMED: 'CONFIRMED',
}

/** 隐患处理列表每页条数（滚动加载按该页大小继续请求） */
export const HAZARD_PAGE_SIZE = 10

/** 算法步骤状态：idle 未开始 / loading 分析中 / success 已出结果 / error 失败待重试 / skipped 人工跳过 */
export const AI_STEP_STATUS = {
  IDLE: 'idle',
  LOADING: 'loading',
  SUCCESS: 'success',
  ERROR: 'error',
  SKIPPED: 'skipped',
}

/**
 * 算法步骤 key：
 * REVIEW 落在「已读资料并对比台账」（疑似重复筛查），JUDGE 落在「已生成复核建议草稿」（隐患判定建议）
 */
export const AI_STEP_KEY = {
  REVIEW: 'materials',
  JUDGE: 'draft',
}

export const hazardReviewData = reactive({
  /** 统计概览加载态 */
  overviewLoading: false,
  /** 统计时段 [开始日期, 结束日期]，空数组表示全部（累计口径，不传时间参数） */
  statRange: [],
  overview: {
    /** 累计接收风险源与来源拆分 */
    source: {
      total: 0,
      caption: '含基层智治、大排查及其他来源',
      icon: iconSummarySource,
      items: [
        { key: 'community', label: '基层智治', value: 0, icon: iconSourceCommunity },
        { key: 'inspection', label: '大排查', value: 0, icon: iconSourceInspection },
        { key: 'other', label: '其他来源', value: 0, icon: iconSourceOther },
      ],
    },
    /** 复核状态：处理进度 + 待复核 / 异常占比 */
    reviewStatus: {
      processed: { key: 'processed', label: '已处理', value: 0, icon: iconStatusProcessed },
      rate: { key: 'rate', label: '处理率', value: 0, icon: iconStatusRate },
      pending: { key: 'pending', label: '待复核', value: 0, icon: iconStatusPending },
      anomalies: [
        { key: 'abnormal', label: '数据异常', value: 0, rate: null, color: '#E18282' },
        { key: 'duplicate', label: '疑似重复', value: 0, rate: null, color: '#AEA8FC' },
      ],
    },
    riskLevels: [],
    hazardTypes: [],
    highRiskTowns: [],
  },
  /** 隐患处理列表（列表组件自行维护加载态与页码） */
  hazardList: [],
  hazardTotal: 0,
})

const toCount = (value) => Number(value ?? 0)

/** 百分比字段：接口未返回时保留 null，展示层按空处理 */
const toRate = (value) => (value === null || value === undefined || value === '' ? null : Number(value))

/** 风险等级：按 A→D 固定顺序补齐，label 拼上潜在亡人说明 */
const toRiskLevels = (list) => {
  const items = Array.isArray(list) ? list : []
  const byValue = new Map(items.map((item) => [String(item?.value ?? ''), item]))
  return RISK_LEVEL_ORDER.map((level) => {
    const item = byValue.get(level)
    const description = item?.description ? `（${item.description}）` : ''
    return {
      key: level,
      label: `${item?.label || level}${description}`,
      value: toCount(item?.count),
      color: RISK_LEVEL_COLOR[level] || RISK_LEVEL_FALLBACK_COLOR,
    }
  })
}

const toHazardTypes = (list) =>
  (Array.isArray(list) ? list : []).map((item) => ({
    key: item?.value ?? item?.label ?? '',
    label: item?.label || item?.value || '',
    value: toCount(item?.count),
    /* 非地灾用绿色强调（与设计稿一致） */
    highlight: item?.value === 'NON_GEOLOGICAL',
  }))

const toHighRiskTowns = (list) =>
  (Array.isArray(list) ? list : []).map((item) => ({
    key: item?.value ?? item?.label ?? '',
    name: item?.label || item?.value || '',
    value: toCount(item?.count),
  }))

/** 统计接口返回 → 概览面板数据 */
const applyOverview = (data = {}) => {
  /* 联调期打印原始返回，便于核对字段名与空值（接口稳定后可移除） */
  console.info('[隐患复核] 总览统计返回:', data)
  const overview = hazardReviewData.overview
  const source = overview.source
  source.total = toCount(data.totalReceivedCount)
  source.items[0].value = toCount(data.communitySourceCount)
  source.items[1].value = toCount(data.inspectionSourceCount)
  source.items[2].value = toCount(data.otherSourceCount)

  const status = overview.reviewStatus
  status.processed.value = toCount(data.processedCount)
  status.rate.value = Number(data.processingRate ?? 0)
  status.pending.value = toCount(data.pendingReviewCount)
  status.anomalies[0].value = toCount(data.abnormalCount)
  status.anomalies[0].rate = toRate(data.abnormalRate)
  status.anomalies[1].value = toCount(data.duplicateCount)
  status.anomalies[1].rate = toRate(data.duplicateRate)

  overview.riskLevels = toRiskLevels(data.riskLevels)
  overview.hazardTypes = toHazardTypes(data.hazardTypes)
  overview.highRiskTowns = toHighRiskTowns(data.highRiskTowns)
}

/**
 * 隐患复核总览统计：时段为空时按接口默认口径（累计到当天）统计
 * @param {Array} [range] 统计时段 [开始日期, 结束日期]，空数组表示全部
 */
export async function fetchHazardOverview(range = hazardReviewData.statRange) {
  const selected = Array.isArray(range) ? range : []
  hazardReviewData.statRange = selected
  /* 接口的时间范围用 ISO 日期时间且首尾都包含，故结束日补到 23:59:59 */
  const params = isAllRange(selected)
    ? {}
    : { beginDate: `${selected[0]}T00:00:00`, endDate: `${selected[1]}T23:59:59` }
  hazardReviewData.overviewLoading = true
  try {
    const data = await getHazardReviewStatistics(params)
    applyOverview(data)
  } finally {
    hazardReviewData.overviewLoading = false
  }
}

/** 地址展示：过滤空值后按 省-市-区县-乡镇-村-组 拼接 */
const buildAddress = (row = {}) =>
  [row.province, row.city, row.county, row.street, row.village, row.gridGroup].filter(Boolean).join('-')

/** 列表摘要：CHANGE 为「变化」、RESULT 为「处理结果」，无摘要时不展示该行 */
const buildReviewResult = (row = {}) => {
  if (!row.reviewSummary) {
    return null
  }
  return row.reviewSummaryType === 'CHANGE'
    ? { label: '变化', text: row.reviewSummary, tone: 'warning' }
    : { label: '处理结果', text: row.reviewSummary, tone: 'info' }
}

/** 带单位展示：接口未返回时留空，不拼出「--人」这类残缺文案 */
const withUnit = (value, unit) =>
  value === null || value === undefined || value === '' ? '' : `${value}${unit}`

/** 列表行 → 卡片数据（经纬度供地图定位使用；detail 供地图弹窗直接展示，免再请求一次详情） */
const buildHazardRow = (row = {}) => ({
  id: row.id,
  name: row.name || '',
  status: row.handleStatusName || '',
  handleStatus: row.handleStatus || '',
  location: buildAddress(row),
  hazardType: row.typeName || '',
  date: row.createdTime || '',
  lng: row.longitude,
  lat: row.latitude,
  result: buildReviewResult(row),
  detail: {
    threatPeople: withUnit(row.threatenedPopulation, '人'),
    threatProperty: withUnit(row.threatenedPropertyValue, '万元'),
    stabilityNow: row.stabilityStatus || '',
    stabilityForecast: row.stabilityTrend || '',
    code: row.uniqueDisasterId || '',
    disasterTime: row.disasterHistoryTime || '',
  },
})

/**
 * 隐患处理分页列表
 * @param {object} options
 * @param {object} [options.filters] 查询条件：{ keyword, typeCode, handleStatus, date }
 * @param {number} [options.pageNum] 页码，从 1 开始；大于 1 时结果追加到列表尾部
 * @param {number} [options.pageSize] 每页条数
 * @returns {Promise<number>} 本次返回的原始条数（调用方据此判断是否还有下一页）
 */
export async function fetchHazardList({ filters = {}, pageNum = 1, pageSize = HAZARD_PAGE_SIZE } = {}) {
  const params = { pageNum, pageSize }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.typeCode) params.typeCode = filters.typeCode
  if (filters.handleStatus) params.handleStatus = filters.handleStatus
  /* 时间筛选为区间：按开始日 00:00:00 - 结束日 23:59:59 下发，首尾都包含；区间为空则不限时间 */
  const range = Array.isArray(filters.dateRange) ? filters.dateRange : []
  if (!isAllRange(range)) {
    params.beginDate = `${range[0]}T00:00:00`
    params.endDate = `${range[1]}T23:59:59`
  }
  const body = await getHazardHandlePage(params)
  /* 分页返回体：文档为 { total, rows }，实际接口与项目其它分页一致用 { total, data }，两种都兼容 */
  const rows = Array.isArray(body?.rows) ? body.rows : Array.isArray(body?.data) ? body.data : []
  if (!Array.isArray(body?.rows) && !Array.isArray(body?.data)) {
    /* 返回体结构与预期不符（分页体里找不到列表）时给出线索，避免列表静默为空 */
    console.warn('[隐患复核] 隐患处理列表返回体结构异常:', body)
  }
  /* 联调期打印首行原始数据，便于核对卡片底部「变化 / 处理结果」等字段名（接口稳定后可移除） */
  if (rows[0]) {
    console.info('[隐患复核] 隐患处理列表首行:', rows[0])
  }
  hazardReviewData.hazardTotal = toCount(body?.total)
  const list = rows.map(buildHazardRow)
  /* 首页替换、后续页追加，滚动加载时列表不被清空 */
  hazardReviewData.hazardList = pageNum > 1 ? [...hazardReviewData.hazardList, ...list] : list
  return rows.length
}

/** 分类分级表：接口 classificationItems → 表格结构 */
const buildClassifyTable = (items) => ({
  columns: ['分类项', '当前结果', '依据'],
  rows: items.map((item) => [item?.label || '', item?.currentResult || '', item?.basis || '']),
})

/** 威胁指标：接口未返回时单位与值都留空，不拼装残缺文案 */
const buildThreatMetrics = (detail = {}) => {
  const toMetric = (label, value, unit) => {
    const empty = value === null || value === undefined || value === ''
    return empty ? { label, value: '', unit: '' } : { label, value: String(value), unit }
  }
  return [
    toMetric('威胁总人数', detail.threatenedPopulation, '人'),
    toMetric('威胁总资产', detail.threatenedPropertyValue, '万元'),
    toMetric('面积', detail.areaSqm, 'm²'),
    toMetric('体积', detail.volumeCbm, 'm³'),
  ]
}

/** 算法步骤初始态：接口结果到达前不展示任何结论 */
const buildAiState = () => ({
  status: AI_STEP_STATUS.IDLE,
  /** 算法返回的 Markdown 文本 */
  answer: '',
  /** 算法侧消息标识，联调排查用 */
  messageId: '',
  /** 失败提示 */
  error: '',
})

/**
 * 智能体详情（接口）→ 右侧面板数据
 * 算法结果字段（aiSummary / comparisonSummary / classificationBasis / reviewSuggestion）
 * 当前可能为 null，对应步骤留空，不编造分析结论；
 * 疑似重复筛查与隐患判定建议两个算法接口的结果由 runHazardAiStep 回填到步骤的 ai 字段
 * @param {object} detail 详情接口返回的 data
 * @param {object} [row] 列表行，用于详情缺字段时兜底展示
 */
export const buildHazardAgentPanel = (detail = {}, row = {}) => {
  const items = Array.isArray(detail.classificationItems) ? detail.classificationItems : []
  const address = buildAddress(detail)
  const operationTypeText = detail.operationType === '1' ? '新增隐患点' : detail.operationType === '2' ? '已有复核点' : ''

  return {
    id: detail.id || row.id || '',
    name: detail.name || row.name || '',
    status: detail.handleStatusName || row.status || '',
    /** 原始详情：确认与反馈需要取 version / reviewAiStatus */
    raw: detail,
    brief: {
      relation: [operationTypeText, address].filter(Boolean).join(' · '),
      materials: `调查表${toCount(detail.surveyFormCount)}份 · 现场图片${toCount(detail.onsitePhotoCount)}张`,
      summary: detail.aiSummary || '',
      footer: { text: '当前未更新台账，', highlight: '待人工确认。' },
    },
    steps: [
      {
        key: AI_STEP_KEY.REVIEW,
        title: '已读资料并对比台账',
        /* 历史台账比对摘要未产出时留空，不写占位结论 */
        points: detail.comparisonSummary ? [detail.comparisonSummary] : [],
        /* 疑似重复筛查结果（Markdown） */
        ai: buildAiState(),
      },
      {
        key: 'classify',
        title: '已提取分类结果',
        subtitle: '当前分类结果：',
        classify: buildClassifyTable(items),
        metrics: buildThreatMetrics(detail),
      },
      {
        key: 'standard',
        title: '分类分级依据待接入',
        /* 依据未产出时用默认进度说明，不空着也不编造分类结论 */
        points: [detail.classificationBasis || DEFAULT_CLASSIFICATION_BASIS],
      },
      {
        key: AI_STEP_KEY.JUDGE,
        title: '已生成复核建议草稿',
        draftNote: detail.reviewSuggestion || '',
        /* 隐患判定建议结果（Markdown） */
        ai: buildAiState(),
      },
    ],
  }
}

/** 算法步骤 → 接口：疑似重复筛查 / 隐患判定建议 */
const AI_STEP_REQUEST = {
  [AI_STEP_KEY.REVIEW]: reviewHazardPoint,
  [AI_STEP_KEY.JUDGE]: judgeHazardPoint,
}

/**
 * 执行单个算法步骤并把结果就地写入面板的步骤对象。
 * 只改字段不替换 panel 引用：面板 watch 监听引用变化，整体替换会重放产出动画
 * @param {object} panel buildHazardAgentPanel 产出的面板数据
 * @param {string} stepKey 步骤 key（AI_STEP_KEY.REVIEW / AI_STEP_KEY.JUDGE）
 * @param {object} [options]
 * @param {() => boolean} [options.shouldApply] 结果落地校验：已切换隐患点或关闭面板时返回 false 丢弃结果
 * @returns {Promise<boolean>} 是否拿到分析结果（失败与被丢弃都返回 false）
 */
export async function runHazardAiStep(panel, stepKey, { shouldApply } = {}) {
  const request = AI_STEP_REQUEST[stepKey]
  const step = panel?.steps?.find((item) => item.key === stepKey)
  if (!request || !step?.ai || !panel?.id) {
    return false
  }
  /* 分析中忽略重复触发：连点重试不该造成重复提交 */
  if (step.ai.status === AI_STEP_STATUS.LOADING) {
    return false
  }
  const keep = typeof shouldApply === 'function' ? shouldApply : () => true
  step.ai.status = AI_STEP_STATUS.LOADING
  step.ai.error = ''
  try {
    const data = await request(panel.id)
    if (!keep()) {
      return false
    }
    step.ai.status = AI_STEP_STATUS.SUCCESS
    step.ai.answer = data?.answer || ''
    step.ai.messageId = data?.messageId || ''
    /* 联调期保留算法侧标识，便于与后端核对（接口稳定后可移除） */
    if (step.ai.messageId) {
      console.info(`[隐患复核] 算法步骤 ${stepKey} messageId:`, step.ai.messageId)
    }
    return true
  } catch (error) {
    if (!keep()) {
      return false
    }
    step.ai.status = AI_STEP_STATUS.ERROR
    /* 业务错误提示由 http 层统一弹出；这里兜底写入面板，避免失败原因缺失时步骤无信息 */
    step.ai.error = error?.msg || error?.message || '算法调用失败，请稍后重试'
    return false
  }
}

/**
 * 人工跳过疑似重复筛查：结果不完整必须在面板上明确标记（文档 skipped 状态），
 * 是否继续调用隐患判定建议由调用方决定
 */
export const skipHazardAiStep = (panel, stepKey) => {
  const step = panel?.steps?.find((item) => item.key === stepKey)
  if (!step?.ai || step.ai.status === AI_STEP_STATUS.LOADING) {
    return
  }
  step.ai.status = AI_STEP_STATUS.SKIPPED
  step.ai.error = ''
}
