import http from '@/utils/http.js'
import { createStreamRequest } from '@/utils/sse.js'
import { ROLE_MAP as RoleMap } from '@/utils/enum.js'
import { noticeUsers } from '@/api/chat.js'
import { NOTICE_KEY } from '@/utils/eventBus.js'

// 查询灾害处置列表
export const getDiasterDealList = (params, config) => {
  return http.get('/dizai/taskHandle/list', params, config)
}

export const getDiasterDealListSpecial = (params, config) => {
  return getDiasterDealList(params, {
    throwRes: true,
  })
}

// 删除灾害处置
export const deleteDisasterTask = (ids) => {
  return http.delete(`/dizai/taskHandle/${ids}`)
}
// 处置详情
export const getDisasterDetail = (id) => {
  return http.get(`/dizai/taskHandle/${id}`)
}

// 获取处置管理现场记录列表
export const getDisasterRecordList = (params) => {
  return http.get('/dizai/taskHandleSceneRecord/list', params)
}

// 通过handleId获取处置管理现场记录列表
export const getDisasterRecordDetailByHandleId = (handleId) => {
  return http.get(`/dizai/taskHandleSceneRecord/getByHandleId/${handleId}`)
}

// 新增灾害处置
export const createNewDisasterDealTask = (params) => {
  return http.post('/dizai/taskHandle', params)
}
// ai生成应急报告
// export const getAiEmergencyReport = (data) => {
//   return http.post('/dizai/taskHandle/aiModifyReport', data)
// }

// ai生成应急报告（流式）
export function getAiEmergencyReport({ params, onMessage }) {
  return createStreamRequest({
    url: '/api/dizai/taskHandle/aiModifyReportStream',
    onMessage,
    body: params,
  })
}

// 下一步流程
export const nextProcess = (id) => {
  return http.post(`/dizai/taskHandle/processNext/${id}`)
}

// 获取专家列表
export const getRoleList = (params) => {
  return http.get('/dizai/role/expert', params, { throwRes: true })
}

// 获取专家列表
export const getExpertList = (params) => {
  const data = Object.assign(params, {
    roleKeys: [
      RoleMap.EXPERT,
      RoleMap.DEPUTY_TOWNSHIP_HEAD,
      RoleMap.DEPUTY_COUNTY_HEAD,
      RoleMap.COUNTY_NATURAL_RESOURCES_LEADER,
      RoleMap.TOWNSHIP_NATURAL_RESOURCES_DIRECTOR
    ]
  })
  return getRoleList(data)
}

// 开启会议室
export const openMeeting = (data) => {
  return http.post('/dizai/meeting/startMeeting', data)
}

// 关闭会议室
export const closeMeeting = (meetingId) => {
  return http.post(`/dizai/meeting/closeMeeting/${meetingId}`)
}

// 退出会议室
export const exitMeeting = (meetingId) => {
  return http.post(`/dizai/meeting/exitMeeting/${meetingId}`, {}, { notUseError: true, noCheckCode: true })
}

// 参加会议
export const joinMeeting = (meetingId) => {
  return http.post(`/dizai/meeting/joinMeeting/${meetingId}`)
}

// 获取灾害处置详情
export const getDisasterDealDetail = (id) => {
  return http.post(`/dizai/taskHandle/detail/${id}`)
}

// 获取会议信息
export const getMeetingRoomInfo = (meetingId) => {
  return http.post(`/dizai/meeting/getMeetingInfo/${meetingId}`)
}

// 获取会商信息
export const getMeetingInfo = (handleId, process) => {
  return http.post(`/dizai/meeting/getMeetingId/${handleId}/${process}`)
}

// 获取会商全部人员
export const getChatUsers = async({ handleId, process, hasMainer = false } = {}) => {
  let meetingId = await getMeetingInfo(handleId, process)
  if (meetingId) {
    const data = await getMeetingRoomInfo(meetingId)
    // 提取会议参与者用户ID列表
    let userIds = Object.values(data?.participantsMap || [])?.map((item) => item.userId) || []
    const mainerId = data?.initiator
    userIds = userIds?.filter(item => item != mainerId) || []
    if (hasMainer) {
      return [mainerId, ...userIds]
    } else {
      return userIds
    }
  }
  return []
}

// 专家认定列表
export const getExpertConfirmList = (meetingId) => {
  return http.post(`/dizai/meeting/expertJudgeList/${meetingId}`)
}

// 专家认定打钩
export const expertConfirm = (data) => {
  return http.post('/dizai/meeting/expertJudgeForMe', data)
}

// 移除参与者
export const removeParticipant = (data) => {
  return http.post('/dizai/meeting/removeParticipants', data)
}

// 呼叫参与者
export const callParticipant = (data) => {
  return http.post('/dizai/meeting/recallParticipants', data)
}

// 添加参与者
export const addParticipant = (data) => {
  return http.post('/dizai/meeting/addParticipants', data)
}

// 修改处置报告
export const modifyDisasterDealReport = (data) => {
  return http.post('/dizai/taskHandle/modifyReport', data)
}

// 获取方案地理数据-包括风险区、避难区和撤离路线
export const getPlanGeoInfo = (params) => {
  return http.get('/dizai/evacuationPlan/list', params)
}

// 获取所有撤离路线--最新
export const getEvacuateInfo = (id) => {
  return http.get(`/dizai/evacuationPlan/routes/${id}`)
}

// 查询配置信息
export const getRetreatPlanConfig = (params) => {
  return http.get('/dizai/taskHandleDetail/handleId', params)
}

// 获取任务详情列表-全部
export const getAllDealTaskList = (params) => {
  return http.get('/dizai/taskDistList/listAll', params)
}

// 统计处置数据
export const getDealStatData = (params) => {
  return http.post('/dizai/taskHandle/stat', params)
}

// 获取短信触达
export const getEmailSendInfo = (id) => {
  return http.get(`/dizai/taskHandle/status?handleId=${id}`)
}

// 任务处置审批列表
export const getTaskHandleApplyList = (params) => {
  return http.get('/dizai/taskHandleApproval/list', params)
}

// 新增任务审批
export const createTaskHandleApply = (data) => {
  return http.post('/dizai/taskHandleApproval', data)
}

// 修改任务审批
export const modifyTaskHandleApply = (data) => {
  return http.put('/dizai/taskHandleApproval', data)
}

// 行政人员审批
export const adminApproval = (handleId, meetingType, handleProcess) => {
  return http.post(
    `/dizai/taskHandleApproval/executive?handleId=${handleId}&meetingType=${meetingType}&handleProcess=${handleProcess}`,
  )
}

// 修改报告
export const editReport = (data) => {
  return http.put('/dizai/taskHandleDetail/edit', data)
}

// 获取报告
export const getReport = (params) => {
  return http.get('/dizai/taskHandleDetail/defId', params)
}

// 单点处置方案生成接口
export const createSolution = (data) => {
  return http.post('/dizai/taskHandleDetail/generateEvacuationPlan', data)
}

// 撤离方案重新生成
export const reCreateSolution = (data) => {
  return http.post('/dizai/taskHandleDetail/regenerate', data)
}

// 撤离方案重新生成（流式）
export function reCreateSolutionStream({ params, onMessage }) {
  return createStreamRequest({
    url: '/api/dizai/taskHandleDetail/generateEvacuationPlan/stream',
    onMessage,
    body: params,
  })
}

// 仅分布任务
export const onlyDistribution = (id) => {
  return http.post(`/dizai/taskHandle/publish/skip/${id}`)
}

// 获取报告历史
// export const getEmergencyReportHistoryList = (params) => {
//   return http.get(`/dizai/taskHandleDetailHistory/list`, params)
// }
export const getEmergencyReportHistoryList = (params) => {
  return http.get(`/dizai/taskHandleDetail/list`, params)
}

// 查询当前所有执行中的处置事件
export const getAllDealingTask = (params) => {
  return http.get('/dizai/taskHandle/getAllHandling', params)
}

// 查询符合条件的最新一条处置详情内容
export const getLastDealDetail = (params) => {
  return http.get(`/dizai/taskHandleDetail/latest`, params)
}

// 根据用户关联区域,展示实时风险概况
export const getRiskOverview = (params) => {
  return http.get(`/dizai/taskHandle/riskOverview`, params)
}

// 新增处置管理详情内容
export const createDealDetail = (data) => {
  return http.post(`/dizai/taskHandleDetail`, data)
}

// 新增处置管理详情内容
export const updateDealDetail = (data) => {
  return http.put(`/dizai/taskHandleDetail`, data)
}


// 上报
export const reportItem = (data) => {
  return http.post(`/dizai/taskDistListAdd`, data)
}

// 获取全部上报内容
export const getAllReport = (params) => {
  return http.get(`/dizai/taskDistListAdd/list`, params)
}

// 编辑
export const editItem = (data) => {
  return http.post(`/dizai/taskDistListRemark`, data)
}

// 获取全部编辑备注
export const getAllEditRemark = (params) => {
  return http.get(`/dizai/taskDistListRemark/listByTaskId`, params)
}

// 生成疏散路线
export const generateEvacuationRoute = (params) => {
  return http.post('/dizai/taskHandleDetail/generateEvacuationRoute', params)
}

// 将指定详情内容设置为最新
export const setLatest = (id) => {
  return http.put(`/dizai/taskHandleDetail/latest?id=${id}`)
}

export const getRiskAreaStat = (params) => {
  return http.get('/dizai/taskHandle/riskAreaStat', params)
}
