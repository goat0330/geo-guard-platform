import http from '@/utils/http.js'
import dayjs from 'dayjs'
import {
  RISK_ASSESSMENT_CACHE_COUNTY,
  readRiskAssessmentCache,
  replaceRiskAssessmentCache,
} from '@/utils/riskAssessmentCache.js'

const CACHE_QUERY_IGNORED_KEYS = new Set([
  'assessmentDate',
  'withSlopeUnit',
  'pageNum',
  'pageSize',
  'orderByColumn',
  'isAsc',
])
const REGION_KEYS = ['province', 'city', 'county', 'street', 'village']
const REGION_CODE_KEYS = ['provinceCode', 'cityCode', 'countyCode', 'streetCode', 'villageCode']
let memoryDatasetDate = ''
let todayDatasetPromise

const getToday = () => dayjs().format('YYYY-MM-DD')

const getRowField = (row, key) => row?.[key] ?? row?.slopeUnit?.[key]

const matchesText = (source, target) => String(source ?? '').includes(String(target).trim())

const filterRiskAssessmentRows = (rows, params = {}) => {
  const dynamicRiskLevels = Array.isArray(params.dynamicRiskLevels)
    ? params.dynamicRiskLevels.map(Number)
    : params.dynamicRiskLevel === undefined || params.dynamicRiskLevel === null || params.dynamicRiskLevel === ''
      ? null
      : [Number(params.dynamicRiskLevel)]

  const filteredRows = rows.filter((row) => {
    if (dynamicRiskLevels?.length && !dynamicRiskLevels.includes(Number(row?.dynamicRiskLevel))) return false
    if (params.slopeUnitId && !matchesText(row?.slopeUnitId, params.slopeUnitId)) return false

    const regionMatched = [...REGION_KEYS, ...REGION_CODE_KEYS].every((key) => (
      !params[key] || String(getRowField(row, key) ?? '') === String(params[key])
    ))
    if (!regionMatched) return false

    return Object.entries(params).every(([key, value]) => {
      if (CACHE_QUERY_IGNORED_KEYS.has(key)) return true
      if (key === 'dynamicRiskLevels' || key === 'dynamicRiskLevel' || key === 'slopeUnitId') return true
      if (REGION_KEYS.includes(key) || REGION_CODE_KEYS.includes(key)) return true
      if (value === undefined || value === null || value === '') return true
      return String(getRowField(row, key) ?? '') === String(value)
    })
  })

  if (!params.orderByColumn) return filteredRows
  const direction = String(params.isAsc).toLowerCase() === 'asc' ? 1 : -1
  return [...filteredRows].sort((left, right) => {
    const leftValue = getRowField(left, params.orderByColumn)
    const rightValue = getRowField(right, params.orderByColumn)
    const leftNumber = Number(leftValue)
    const rightNumber = Number(rightValue)
    if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
      return (leftNumber - rightNumber) * direction
    }
    return String(leftValue ?? '').localeCompare(String(rightValue ?? '')) * direction
  })
}

const canUseTodayCache = (params = {}) => {
  const assessmentDate = params.assessmentDate || getToday()
  const county = params.county
  return assessmentDate === getToday() && (!county || county === RISK_ASSESSMENT_CACHE_COUNTY)
}

const fetchTodayDataset = async () => {
  const assessmentDate = getToday()
  const response = await http.get('/dizai/riskAssessment/list', {
    county: RISK_ASSESSMENT_CACHE_COUNTY,
    assessmentDate,
    withSlopeUnit: true,
  }, { paramsSerializer: serializeQueryParams })
  const rows = Array.isArray(response) ? response : resolveRiskAssessmentPage(response).rows

  // 写入是独立后台任务，页面拿到接口数据后无需等待 IndexedDB。
  void replaceRiskAssessmentCache(assessmentDate, rows).catch((error) => {
    console.warn('风险评价数据写入 IndexedDB 失败', error)
  })
  return rows
}

/** 保证当天全量数据只读取或请求一次，并发页面共用同一 Promise。 */
export const ensureTodayRiskAssessmentDataset = async () => {
  const assessmentDate = getToday()
  if (memoryDatasetDate !== assessmentDate) {
    memoryDatasetDate = assessmentDate
    todayDatasetPromise = undefined
  }
  if (todayDatasetPromise) return todayDatasetPromise

  todayDatasetPromise = (async () => {
    try {
      const cachedRows = await readRiskAssessmentCache(assessmentDate)
      if (cachedRows) return cachedRows
    } catch (error) {
      console.warn('风险评价数据读取 IndexedDB 失败', error)
    }
    return fetchTodayDataset()
  })()

  try {
    return await todayDatasetPromise
  } catch (error) {
    todayDatasetPromise = undefined
    throw error
  }
}

/** 在首屏渲染后的浏览器空闲时间预取当天数据。 */
export const scheduleTodayRiskAssessmentPrefetch = () => {
  const run = () => {
    void ensureTodayRiskAssessmentDataset().catch((error) => {
      console.warn('风险评价当天数据预取失败', error)
    })
  }

  if ('requestIdleCallback' in window) {
    const idleId = window.requestIdleCallback(run, { timeout: 3000 })
    return () => window.cancelIdleCallback(idleId)
  }
  const timerId = window.setTimeout(run, 1000)
  return () => window.clearTimeout(timerId)
}

/**
 * 自定义查询参数序列化。
 * 后端 dynamicRiskLevels 约定使用同名重复参数（dynamicRiskLevels=2&dynamicRiskLevels=3），
 * axios 默认会序列化成 dynamicRiskLevels[]=2，因此这里统一改写。
 */
const serializeQueryParams = (params) => {
  const searchParams = new URLSearchParams()

  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item === undefined || item === null || item === '') return
        searchParams.append(key, item)
      })
      return
    }

    searchParams.append(key, value)
  })

  return searchParams.toString()
}

/**
 * 斜坡单元分页列表
 * GET /dizai/riskAssessment/list
 * 返回 TableDataInfo：{ code, msg, total, rows }
 * @param {{ assessmentDate?: string, slopeUnitId?: string, dynamicRiskLevel?: number, dynamicRiskLevels?: number[], pageNum?: number, pageSize?: number }} params
 */
export const getRiskAssessmentPage = async (params, config = {}) => {
  if (canUseTodayCache(params)) {
    const rows = filterRiskAssessmentRows(await ensureTodayRiskAssessmentDataset(), params)
    const pageNum = Math.max(Number(params?.pageNum) || 1, 1)
    const pageSize = Math.max(Number(params?.pageSize) || rows.length || 1, 1)
    const start = (pageNum - 1) * pageSize
    return {
      code: 200,
      msg: '',
      rows: rows.slice(start, start + pageSize),
      total: rows.length,
    }
  }
  return http.get('/dizai/riskAssessment/list', params, {
    throwRes: true,
    paramsSerializer: serializeQueryParams,
    ...config,
  })
}

/**
 * 斜坡单元全量列表。地图仅在行政区、日期或风险等级变化时调用，避免分页限制截断 WKT 图层。
 */
export const getRiskAssessmentList = async (params, config = {}) => {
  if (canUseTodayCache(params)) {
    return filterRiskAssessmentRows(await ensureTodayRiskAssessmentDataset(), params)
  }
  return http.get('/dizai/riskAssessment/list', params, {
    paramsSerializer: serializeQueryParams,
    ...config,
  })
}

/**
 * 解析分页结果，兼容 rows/total 位于顶层（TableDataInfo）与被 data 包裹两种返回形态。
 * @returns {{ rows: object[], total: number }}
 */
export const resolveRiskAssessmentPage = (res) => {
  const body = Array.isArray(res?.rows)
    ? res
    : Array.isArray(res?.data?.rows)
      ? res.data
      : Array.isArray(res?.data)
        ? { rows: res.data, total: res.total }
        : null
  if (!body) return { rows: [], total: 0 }

  const total = Number(body.total)
  return {
    rows: body.rows || [],
    total: Number.isFinite(total) ? total : 0,
  }
}
