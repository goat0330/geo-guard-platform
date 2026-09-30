import http from '@/utils/http.js'
import { getRiskAssessmentList, getRiskAssessmentPage } from '@/api/riskEvaluation.js'

// 获取行政区划树形结构
export const getAreaList = async (params) => {
  return http.get('/dizai/adRegion/list', params)
}

// 获取行政区划树形结构
export const getAreaListWithOutWk = async (params) => {
  return http.get('/dizai/adRegion/tree', {
    withWkt: false,
    ...params,
  })
}

// 获取斜坡单元数据
export const getSlopeUnitList = async (params) => {
  return http.get('/dizai/slopeUnit/list', params)
}

// 获取斜坡单元数据分页
export const getSlopeUnitListPage = async (params) => {
  return http.get('/dizai/slopeUnit/list', params, { throwRes: true })
}

// 获取斜坡单元详细信息
export const getSlopeUnitDetail = async (params, config = {}) => {
  return http.get(`/dizai/slopeUnit/${params}`, null, config)
}

// 获取灾害点数据
export const getHazardPointList = async (params, config = {}) => {
  return http.get('/dizai/hazardPoint/list', params, config)
}

// 获取雨量数据
export const getRainDataList = async (params) => {
  return http.get('/dizai/dzRainfallRasterUnit/list/current/day', params)
}

// 获取雨量数据
export const getRainDataLive = async (params) => {
  return http.get('/dizai/warningData/getLiveRain', params)
}

// 获取今日雨量数据（按照街道级行政区划聚合）
export const getTodayRainfall = async (params) => {
  return http.get('/dizai/rainfallLogSlopeUnitStatistic/listTodayRainfall', params)
}

// 获取斜坡单元风险评估
export const getSlopeUnitRisk = async (params, config = {}) => {
  return getRiskAssessmentList(params, config)
}

// 获取斜坡单元风险评估
export const getSlopeUnitRiskPage = async (params) => {
  return getRiskAssessmentPage(params)
}

// 查询制定斜坡单元的风险评估详情
export const getSlopeUnitRiskDetail = async (id) => {
  return http.get(`/dizai/riskAssessment/${id}`)
}

// 获取行政区划的信息
export const getAreaStatistic = async (params) => {
  return http.post('/dizai/adRegion/stat', params)
}
// 根据行政区划id查询信息
export const getAreaInfoById = async (params) => {
  return http.get(`/dizai/adRegion/getInfo/${params}`)
}

// 获取监测点数据
export const getMonitorPointList = async (params) => {
  return http.get('/dizai/monitorPoint/list', params)
}

// 获取监测点基本情况详细信息
export const getMonitorDetailById = async (id) => {
  return http.get(`/dizai/monitorPoint/${id}`)
}

// 统计风险评估数据
export const getRiskStatistic = async (params) => {
  return http.post('/dizai/riskAssessment/stat', params)
}

// 根据斜坡单元Id和时间查询斜坡单元风险评估详情
export const getSlopeDynimicRiskDetail = async (params) => {
  return http.post('/dizai/riskAssessment/unitId', params)
}

//统计斜坡单元危险性近n天的数据
export const getSlopeRangeDangerInfo = async (params) => {
  return http.post('/dizai/riskAssessment/statHazard', params)
}

// 统计聊天横幅数据
export const getChatBannerStat = async (params = {}) => {
  return http.post('/dizai/riskAssessment/statChatBanner', params)
}

// 获取风险区信息
export const getRiskAreaInfo = async (params) => {
  return http.get('/dizai/riskZone/list', params)
}

// 获取监测仪器情况
export const getMonitorInfo = async (params) => {
  return http.post('/dizai/monitorDevice/stat', params)
}

// 获取监测仪器地区情况
export const getMonitorStatByArea = async (params) => {
  return http.post('/dizai/monitorDevice/statByArea', params)
}

// 查询地质灾害预测与易发性评价数据列表
export const getRiskAssessmentSteps = async (params) => {
  return http.get('/dizai/riskAssessmentSteps/list', params)
}

// 根据斜坡单元Id查询斜坡单元的负责人
export const getSlopeUnitPerson = async (id, config = {}) => {
  return http.get(`/dizai/slopeUnit/getSlopeUnitPerson/${id}`, null, config)
}

// 根据ossid获取图片url
export const getImageUrlById = async (params) => {
  return http.post(`/dizai/oss/apply-url`, params)
}

// 修改防御响应状态(0:未开启，1:已开启)
export const updateDefenseStatus = async (status) => {
  return http.post(`/dizai/defRespPlan/status/${status}`)
}

// 查询防御响应方案状态(0:未开启，1:已开启)
export const getDefenseStatus = async () => {
  return http.post(`/dizai/defRespPlan/getStatus`)
}

// 查询最新的一条日志
export const getLatestLog = async () => {
  return http.get(`/dizai/operLog/latest`)
}

// 上传文件到oss
export const uploadFile = async (params) => {
  return http.post(`/dizai/oss/upload`, params, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 通过ossId下载文件
export const downloadFile = async (id) => {
  return http.post(
    `/dizai/oss/down-url/${id}`,
    {},
    {
      responseType: 'blob',
      noCheckCode: true,
      throwRes: true,
    },
  )
}

// 获取今日气象预警
// export const getTodayWeatherWarning = async (params) => {
//   return http.get(`/dizai/dataAlarm/Stats`, params)
// }

// 获取降雨量
export const getRainfall = async (slopeUnitId) => {
  return http.get(`/dizai/dzRainfallRasterUnit/slopeUnit/${slopeUnitId}`)
}

// 获取降雨数据
export const getRainfallData = async (slopeUnitId) => {
  return http.get(`/dizai/dzRainfallRasterUnit/slopeUnit/summary/${slopeUnitId}`)
}

// 记录当前登录用户当天已处理过的业务标记
export const saveUserQuestion = async (question) => {
  return http.post(`/dizai/common/dzCache/record`, question)
}

// 查询当前登录用户当天是否已存在业务标记
export const queryUserQuestion = async (question) => {
  return http.post(`/dizai/common/dzCache/query`, question)
}

// 查询斜坡单元列表，不包含WKT
export const getSlopeUnitListWithOutWkt = async (params) => {
  return http.get(`/dizai/slopeUnit/list_no_wkt`, params)
}

// 获取天气预测
export const getWeatherInfo = (params) => {
  return http.get('/dizai/dataAlarm/Stats', params)
}

// 获取天气预测历史记录
export const getWeatherInfoHistory = (params) => {
  return http.get('/dizai/dataAlarm/list', params)
}

// 根据斜坡单元ID查询当天24小时雨量序列。
export const getRainfallSequence = async (params) => {
  return http.get(`/dizai/rainfallLogSlopeUnitStatistic/todayHourlyBySlopeUnit`, params, {
    notUseError: true,
    noCheckCode: true,
  })
}

// 根据地图类型和区域获取统计面板数据
export const getStatData = (params) => {
  return http.get('/dizai/person/stat', params)
}

// 根据范围WKT查询房屋列表
export const getHouseListByWkt = (params) => {
  return http.post('/dizai/dataHouse/queryByWkt', params)
}

// 根据斜坡单元ID查询道路列表
export const getRoadListBySlopeUnitId = (slopeUnitId) => {
  return http.get(`/dizai/dataRoad/bySlopeUnitId/${slopeUnitId}`)
}
// 根据坐标经纬度查询承灾体相关信息
export const getBearBuildingInfo = (params) => {
  return http.get('/dizai/person/building/profile', params)
}

// 接收算法侧解析天气预测预警
export const receiveWeatherWarning = (params) => {
  return http.post('/dizai/algorithm-receive/reportAnalysis', params)
}

// 查询承灾体
export const getOverloadBuilding = (params) => {
  return http.get('/dizai/warningData/listHouseGeometry', params)
}

// 修改预警记录
export const updateWarningData = (params) => {
  return http.put(`/dizai/dataAlarm`, params)
}

// 获取风险等级有升级风险的斜坡单元
export const getUpgradeRiskSlopeUnit = (params) => {
  return http.get('/dizai/riskAssessment/todayTemDynamicRiskList', params)
}

// 按乡镇名称批量查询乡镇基础信息
export const getTownInfoByName = (params) => {
  return http.post('/dizai/adRegion/town/basicInfo', params)
}

// 获取首页聚合业务看板
export const getHomeDashboard = (params) => {
  return http.get('/dizai/home/dashboard', params)
}
