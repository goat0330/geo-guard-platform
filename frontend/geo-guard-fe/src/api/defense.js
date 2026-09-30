import http from '@/utils/http.js'

// 查询防御响应方案列表
export const defRespPlanPageList = (params) => {
  return http.get('/dizai/defRespPlan/list', params, {
    throwRes: true,
    notUseError: true,
    noCheckCode: true
  })
}

export const defRespPlanList = (params) => {
  return http.get('/dizai/defRespPlan/list', params)
}

// 获取防御响应方案详细信息
export const defRespPlanDetail = (id) => {
  return http.get(`/dizai/defRespPlan/${id}`)
}

// 修改防御响应方案
export const updateDefRespPlan = (data) => {
  return http.put('/dizai/defRespPlan/edit', data)
}

// 防御响应下一步
export const nextDefRespPlan = (id) => {
  return http.post(`/dizai/defRespPlan/processNext/${id}`)
}

// 获取防御响应流程进度
export const getDefRespPlanProgress = (params) => {
  return http.get(`/dizai/processProgress/def`, params)
}

// 获取总数
export const getDefRespPlanTotal = (params) => {
  return http.get(`/dizai/defRespPlan/tree`, params)
}

// 获取已启动和拟启动区域
export const getDefenseArea = (params) => {
  return http.get('/dizai/defRespPlan/getDefenseRespRange', params)
}
// 开启单点防御
export const startDefenseFlow = (data) => {
  return http.post(`/dizai/defRespPlan/startSingle?handleId=${data.handleId}`)
}

// 查询关联信息
export const getRelationInfo = (params) => {
  return http.get(`/dizai/defRespPlan/getRelationInfo`, params)
}

// 获取区域防御的范围
export const getDefenseAreaRange = (defId) => {
  return http.get(`/dizai/defRespPlan/rangeSlopeUnits/${defId}`)
}

// 查询子节点XX建议。
export const getSubSuggestions = (params) => {
  return http.get(`/dizai/defRespPlan/childGeoAdvice`, params)
}

// 按乡镇/街道名称查询当前等级和XX建议。
export const getGeoAdviceByTown = (params) => {
  return http.get(`/dizai/defRespPlan/streetGeoAdvice`, params)
}

// 批量调整乡镇级区域防御响应与县级区域防御响应的关联关系
export const batchAdjustRelation = (data) => {
  return http.post(`/dizai/defRespPlan/batchRelateTownRegion`, data)
}

// 获取待处置预警列表
export const dataAlarmList = (params) => {
  return http.get('/dizai/dataAlarm/list', params)
}

// 按指定时间匹配气象预警列表
export const dataAlarmDetail = (params) => {
  return http.get('/dizai/dataAlarm/matchList', params)
}

// 预启动预警信息
export const defenseResponsePlan = (params) => {
  return http.post(`/dizai/defRespPlan/syncConsultationFromAlarm`, params)
}

// 判断提交乡镇是否有变化
export const hasChangeStreets = (data) => {
  return http.post(`/dizai/defRespPlan/matchCirculatingTownLevels`, data)
}

export const approvalStatus = (data) => {
  return http.post(`/dizai/defRespPlan/approvalStatus`, data)
}

// 查询当前流转中乡镇级防御响应统计。
export const getTownRegionStatistics = (params) => {
  return http.get(`/dizai/defRespPlan/circulatingTownStats`, params)
}
