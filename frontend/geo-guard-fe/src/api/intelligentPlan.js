import http from '@/utils/http.js'

/** 接口前缀 */
const BASE = '/dizai/intelligentPlan'

/** 生成场景枚举：与后端保持一致 */
export const PLAN_SCENE = {
  HAZARD: 'EXISTING_HAZARD',
  EMERGENCY: 'EMERGENCY_EVENT',
}

/** 来源更新状态：隐患点取 aiProcessStatus，突发灾险情取 status，枚举一致
 *  0 待更新/待勾划范围、1 更新中、2 已更新、3 更新失败 */
export const PLAN_STATUS = {
  PENDING: '0',
  UPDATING: '1',
  UPDATED: '2',
  FAILED: '3',
}

/**
 * 从响应体里取出行数组，兼容常见分页壳：rows / list / records / data
 * @param {object} body 响应体或其 data 层
 * @returns {object[]|null}
 */
const pickRows = (body) => {
  if (!body || typeof body !== 'object') {
    return null
  }
  const hit = ['rows', 'list', 'records', 'data'].find((key) => Array.isArray(body[key]))
  return hit ? body[hit] : null
}

/**
 * 解析分页结果。
 * 实际返回体形如 { code, msg, total, data: [...] }，data 本身即行数组、total 与 data 平级；
 * 同时兼容 { rows, total } 与 { data: { rows, total } } 两种壳。
 * http 的 throwRes 把整个返回体原样抛出，统一在此取值
 * @param {object} res 接口返回体
 * @returns {{ rows: object[], total: number }}
 */
export const resolvePlanPage = (res) => {
  const inner = res?.data && typeof res.data === 'object' ? res.data : null
  const rows = pickRows(res) ?? pickRows(inner) ?? []
  const total = Number(res?.total ?? inner?.total)
  return {
    rows,
    total: Number.isFinite(total) ? total : rows.length,
  }
}

/**
 * 单日筛选转起止时间。
 * 后端 beginDate / endDate 为 yyyy-MM-dd HH:mm:ss，且包含边界值
 * @param {string} date YYYY-MM-DD
 * @returns {{ beginDate?: string, endDate?: string }}
 */
export const buildDateRange = (date) => {
  if (!date) {
    return {}
  }
  return {
    beginDate: `${date} 00:00:00`,
    endDate: `${date} 23:59:59`,
  }
}

/**
 * 已有隐患点候选列表
 * GET /dizai/intelligentPlan/hazard/list
 * @param {{ pageNum?: number, pageSize?: number, keyword?: string, typeCode?: string, handleStatus?: string, beginDate?: string, endDate?: string }} params
 */
export const fetchHazardPlanList = (params) => http.get(`${BASE}/hazard/list`, params, { throwRes: true })

/**
 * 突发灾险情候选列表
 * GET /dizai/intelligentPlan/emergency/list
 * @param {{ pageNum?: number, pageSize?: number, keyword?: string, status?: number, beginDate?: string, endDate?: string }} params
 */
export const fetchEmergencyPlanList = (params) =>
  http.get(`${BASE}/emergency/list`, params, { throwRes: true })

/**
 * 生成撤离路线（同步接口，未完成前前端保持 loading）
 * POST /dizai/intelligentPlan/generate
 * @param {{ scene: string, sourceId: string, wkt?: string }} data sourceId 全程按字符串传递
 */
export const generateIntelligentPlan = (data) => http.post(`${BASE}/generate`, data)

/**
 * 查询当前撤离路线（含来源完整信息与最新路线结果）
 * GET /dizai/intelligentPlan/route/latest
 * @param {{ scene: string, sourceId: string }} params
 */
export const fetchLatestRoute = (params) => http.get(`${BASE}/route/latest`, params)

/**
 * 解析撤离路线查询结果。
 * 返回体形如 { code, msg, data: { scene, sourceId, hazardPoint, emergencyEvent, routeResult } }，
 * http 默认只抛出 data 层，这里同时兼容直接拿到整个返回体的情况
 * @param {object} res 接口返回体
 * @returns {{ scene: string, sourceId: string, hazardPoint: object|null, emergencyEvent: object|null, routeResult: object|null, status: string }}
 */
export const resolveRoutePayload = (res) => {
  const data = res?.data && res.data.routeResult !== undefined ? res.data : res ?? {}
  const status = data.hazardPoint?.aiProcessStatus ?? data.emergencyEvent?.status
  return {
    scene: data.scene ?? '',
    sourceId: data.sourceId ?? '',
    hazardPoint: data.hazardPoint ?? null,
    emergencyEvent: data.emergencyEvent ?? null,
    routeResult: data.routeResult ?? null,
    /* 隐患点返回字符串枚举，突发灾险情返回数字，统一转成字符串比较 */
    status: status === undefined || status === null ? '' : String(status),
  }
}

/**
 * 已有隐患点案件详情（字段与「地质灾害隐患点防灾预案表」一致）
 * GET /dizai/intelligentPlan/hazard/{sourceId}/case-detail
 * @param {string} sourceId 隐患点列表 ID（字符串）
 */
export const fetchHazardCaseDetail = (sourceId) =>
  http.get(`${BASE}/hazard/${encodeURIComponent(sourceId)}/case-detail`)

/**
 * 突发灾险情案件详情
 * GET /dizai/intelligentPlan/emergency/{sourceId}/case-detail
 * @param {string} sourceId 灾险情列表 ID（数字内容的字符串）
 */
export const fetchEmergencyCaseDetail = (sourceId) =>
  http.get(`${BASE}/emergency/${encodeURIComponent(sourceId)}/case-detail`)

/**
 * 按场景查询案件详情：突发灾险情走 emergency 分支，其余（含已有隐患点）走 hazard 分支
 * @param {{ scene: string, sourceId: string }} params
 */
export const fetchPlanCaseDetail = ({ scene, sourceId }) =>
  scene === PLAN_SCENE.EMERGENCY ? fetchEmergencyCaseDetail(sourceId) : fetchHazardCaseDetail(sourceId)

/**
 * 解析案件详情响应体。
 * http 默认抛出 data 层（即详情本体），这里同时兼容直接拿到完整返回体的情况
 * @param {object} res 接口返回体
 * @returns {object|null} 案件详情
 */
export const resolveCaseDetail = (res) => {
  if (!res || typeof res !== 'object') {
    return null
  }
  /* 完整返回体形如 { code, msg, data }，data 中带 title 或 sourceId */
  const isEnvelope = res.sourceId === undefined && res.title === undefined && res.data !== undefined
  return isEnvelope ? res.data ?? null : res
}

/** 等待指定毫秒 */
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * 轮询查询撤离路线，直到来源状态变为「已更新 / 更新失败」或超时。
 * POST /generate 已改为异步，算法在后台执行，前端按后端建议每 2~3 秒轮询一次
 * @param {object} options
 * @param {string} options.scene 场景枚举
 * @param {string} options.sourceId 来源 ID（字符串）
 * @param {number} [options.interval] 轮询间隔，默认 2000ms
 * @param {number} [options.timeout] 超时时间，默认 30 分钟（与后端算法超时一致）
 * @param {() => boolean} [options.shouldStop] 外部中止判定（组件卸载 / 用户取消），返回 true 立即停止
 * @param {(payload: object) => void} [options.onTick] 每次轮询结果回调，便于实时刷新进度
 * @returns {Promise<{ payload: object, status: string, timeout: boolean }>}
 */
export const pollLatestRoute = async ({
  scene,
  sourceId,
  interval = 2000,
  timeout = 30 * 60 * 1000,
  shouldStop = () => false,
  onTick = null,
}) => {
  const startedAt = Date.now()
  let payload = { scene, sourceId, hazardPoint: null, emergencyEvent: null, routeResult: null, status: '' }

  while (!shouldStop()) {
    const res = await fetchLatestRoute({ scene, sourceId })
    payload = resolveRoutePayload(res)
    onTick?.(payload)

    /* 已更新 / 更新失败：停止轮询 */
    if (payload.status === PLAN_STATUS.UPDATED || payload.status === PLAN_STATUS.FAILED) {
      return { payload, status: payload.status, timeout: false }
    }
    /* 超时保护：算法默认 30 分钟超时，超时后按失败处理，避免无限轮询 */
    if (Date.now() - startedAt >= timeout) {
      return { payload, status: payload.status, timeout: true }
    }
    await sleep(interval)
  }

  return { payload, status: payload.status, timeout: false }
}
