import http from '@/utils/http.js'

// 查询风险预测列表
export function riskPredictionList(params) {
  return http.get('/dizai/riskPrediction/list', params)
}

export function riskPredictionListPage(params) {
  return http.get('/dizai/riskPrediction/list', params, { throwRes: true })
}

// 统计风险预测数据
export function riskPredictionStat(params) {
  return http.post('/dizai/riskPrediction/stat', params)
}

// 新增地灾风险预测推送
export function addRiskPrediction(data) {
  return http.post('/dizai/riskPredictionPush', data)
}

// 获取地灾风险预测推送详细信息
export function getRiskPredictionDetail(id) {
  return http.get(`/dizai/riskPredictionPush/${id}`)
}

// 推送当前报告
export function pushRiskPrediction(id) {
  return http.post(`/dizai/riskPredictionPush/pushReport/${id}`)
}

// 查询地灾风险预测推送列表
export const getPredictionList = async (params) => {
  return http.get('/dizai/riskPredictionPush/list', params, { throwRes: true })
}

// 统计风险预测banner
export function getHeaderBanner(data) {
  return http.post('/dizai/riskPrediction/statBanner', data)
}
