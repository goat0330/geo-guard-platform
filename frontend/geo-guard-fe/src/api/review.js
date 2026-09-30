import http from '@/utils/http.js'

/**
 * 查询复盘事件列表
 * @param {object} params - 查询参数 (pageNum, pageSize, eventName, reviewStatus 等)
 * @returns {Promise} 响应体包含 total, data (或 rows)
 */
export const getReviewEventList = (params) => {
  return http.get('/dizai/taskHandle/list', params, { throwRes: true })
}

/**
 * 查询复盘事件详情
 * @param {string|number} id - 事件处置主键 ID
 * @returns {Promise}
 */
export const getReviewEventDetail = (id) => {
  return http.get(`/dizai/taskHandle/reviewDetail/${id}`)
}
