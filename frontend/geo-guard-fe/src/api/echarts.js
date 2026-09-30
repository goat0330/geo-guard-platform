import http from '@/utils/http.js'

// 统计风险评估数据
export function getRiskAssessment(data) {
  return http.post('/dizai/riskAssessment/stat', data)
}

// 查询监测设备基本情况列表
export function getMonitorDeviceList(params) {
  return http.get('/dizai/monitorDevice/list', params)
}

// 获取监测设备基本情况详细信息
export function getMonitorDevice(id) {
  return http.get(`/dizai/monitorDevice/${id}`)
}

// 获取专业预警情况
export function getWarningData(params) {
  return http.post(`/dizai/warningData/statMonitorWarning`, params)
}
