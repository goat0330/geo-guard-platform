import http from '@/utils/http.js'

// 隐患复核模块接口，字段依据 docs/隐患复核接口联调文档.md
// 响应：普通接口 { code, msg, data }；分页接口 { code, msg, total, rows }

/**
 * 隐患复核总览统计
 * 参数：{ statDate } 或 { beginDate, endDate }（必须成对），regionCode 可选
 */
export const getHazardReviewStatistics = (params) => {
  return http.get('/dizai/hazardPoint/statistics', params)
}

/**
 * 隐患处理分页列表
 * 参数：{ keyword, typeCode, handleStatus, beginDate, endDate, pageNum, pageSize }
 * 分页返回体里 total 与 rows 同级，默认封装只抛内层 data，故用 throwRes 取完整返回体
 */
export const getHazardHandlePage = (params) => {
  return http.get('/dizai/hazardPoint/handle/list', params, { throwRes: true })
}

/** 隐患复核智能体详情：算法结果字段（aiSummary 等）未产出时为 null */
export const getHazardHandleDetail = (id) => {
  return http.get(`/dizai/hazardPoint/handle/${id}/detail`)
}

/** 提交复核反馈：body { version, feedback }，返回更新后的 version */
export const submitHazardFeedback = (id, data) => {
  return http.post(`/dizai/hazardPoint/handle/${id}/feedback`, data)
}

/** 人工确认并生成正式台账：需 reviewAiStatus=SUCCEEDED，body { version, reviewResult, ... } */
export const confirmHazardReview = (id, data) => {
  return http.put(`/dizai/hazardPoint/handle/${id}/confirm`, data)
}

/**
 * 算法接口请求配置：两个算法接口为阻塞模式，Dify 侧可能耗时数十秒，
 * 默认 60s 容易在正常分析中被掐断，这里单独放宽
 */
const AI_REQUEST_CONFIG = { timeout: 180000 }

/**
 * 疑似重复筛查（算法）：当前隐患点是否可能对应已有隐患点
 * 返回 { messageId, conversationId, answer }，answer 为 Markdown 文本，前端按 Markdown 渲染
 * @param {string} id 隐患点主键
 * @param {object} [data] 仅当存在台账外的现场信息时传 { additionalDescription }，最长 20000 字符
 */
export const reviewHazardPoint = (id, data) => {
  return http.post(`/dizai/hazardPoint/handle/${encodeURIComponent(id)}/ai/review`, data, AI_REQUEST_CONFIG)
}

/**
 * 隐患判定建议（算法）：当前隐患点应如何认定和处理
 * 与疑似重复筛查相互独立，judge 不会读取 review 的返回内容，前端也不要串接两者的结果
 * 返回结构与 reviewHazardPoint 一致
 * @param {string} id 隐患点主键
 * @param {object} [data] 请求体同 reviewHazardPoint
 */
export const judgeHazardPoint = (id, data) => {
  return http.post(`/dizai/hazardPoint/handle/${encodeURIComponent(id)}/ai/judge`, data, AI_REQUEST_CONFIG)
}
