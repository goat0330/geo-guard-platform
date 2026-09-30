import http from '@/utils/http.js'

// 设备概况统计：监测点数 / 设备总数 / 在线数 / 在线率
export const getMonitorDeviceStat = () => {
  return http.post('/dizai/monitorDevice/stat')
}

// 统计已发布的监测设备预警数据（处理状态分布 + 预警等级分布），body: { offset }
export const getMonitorWarningStat = (params) => {
  return http.post('/dizai/warningData/statMonitorWarning', params)
}

// 获取对应时间段的已发布气象预警数据列表（state 必传，默认传 1）
// 返回体里 total 与 data 同级（{ total, data, code, msg }），默认封装只抛内层 data 会丢掉 total，
// 分页需要同时拿到两者，故用 throwRes 取完整返回体
export const getWeatherWarningPage = (params) => {
  return http.get('/dizai/warningData/getWeatherWarning', params, { throwRes: true })
}

// 获取对应时间段的已发布的监测设备预警数据列表（专业监测告警列表的数据源）
// 字段见 docs/获取对应时间段的已发布的监测设备预警数据列表.md；
// state（数据状态）由调用方按查询条件传入，不传则走后端默认口径
// 与气象预警一致：total 与 data 同级，用 throwRes 取完整返回体以支持分页
export const getMonitorWarningPage = (params) => {
  return http.get('/dizai/warningData/getMonitorWarning', params, { throwRes: true })
}

// 导出预警处置记录监测列表（返回文件流）
// 字段见 docs/导出预警处置记录监测列表.md：参数都在 query 上，故走 config.params；
// 返回体是文件流，用 responseType: 'blob' 并按 throwRes 直接拿 Blob（noCheckCode 跳过 JSON 校验）
export const exportWarningDisposalRecord = (params) => {
  return http.post('/dizai/warningDisposalRecordMonitor/export', null, {
    params,
    responseType: 'blob',
    noCheckCode: true,
    throwRes: true,
  })
}

// 监测设备详情
export const getMonitorDeviceDetail = (id) => {
  return http.get(`/dizai/monitorDevice/${id}`)
}

// 监测设备监测数据树
export const getMonitorDataTree = (id) => {
  return http.get(`/dizai/monitorDevice/getMonitorData/${id}`)
}


/**
 * 专业监测概览统计：设备概况 / 设备类型 / 告警情况 / 处置状态 / 最新预警
 * body: { regionCode, startDate, endDate }，时段为「全部」时不传 startDate / endDate
 */
export const getMonitorOverview = (data) => {
  return http.post('/dizai/warningData/professionalMonitoringOverview', data)
}

// 基于本地同步的预警、设备及曲线调用专业监测智能体：返回监测曲线描述与分析摘要
// 字段见 docs/基于本地同步的预警、设备及曲线调用专业监测智能体。.md
// body: { warningId, startTime, endTime }，时间为毫秒时间戳
export const analyzeMonitorWarning = (data) => {
  return http.post('/dizai/warningData/analyzeMonitorWarning', data)
}