// 查询任务派发清单列表
import http from '@/utils/http.js'

// 查询信息报送列表
export function taskDistList(params) {
  return http.get('/dizai/reportDisaster/list', params, {
    throwRes: true,
  })
}

// 处理当前报灾
export function handleDisReport(id) {
  return http.post(`/dizai/reportDisaster/handle/${id}`)
}

// 关闭上报
export function closeDisReport(id) {
  return http.delete(`/dizai/reportDisaster/closeReport/${id}`)
}

// 统计当前报灾
export function getDisStatData(params) {
  return http.post('/dizai/reportDisaster/stat', params)
}

// 查询群众报灾看板
export function getPublicDisasterDashboard(params) {
  return http.get('/dizai/reportDisaster/public/dashboard', params)
}

// 获取当前所有正在处理的报灾
export function getDealingDisaster(params) {
  return http.get('/dizai/reportDisaster/getAllHandling', params)
}

// 获取信息报送详细信息
export function getDisReportDetail(id) {
  return http.get(`/dizai/reportDisaster/${id}`)
}

// 提交报灾记录建议
export function submitDisReportSuggestion(params) {
  // 该接口按文档约定以 code: 0 表示成功，兼容项目通用的 code: 200 约定。
  return http.post('/dizai/reportDisaster/suggestions', params, { successCodes: [0, 200] })
}
