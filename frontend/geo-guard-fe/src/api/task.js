import http from '@/utils/http.js'

// 获取任务统计信息
export const getTaskStat = async (params) =>
  http.post('/dizai/taskDistList/stat-status', params)

// 获取任务来源统计信息
export const getTaskSourceStat = async (params) =>
  http.post('/dizai/taskDistList/stat-source-type', params)

// 查询任务派发清单列表
export function taskList(params) {
  return http.get('/dizai/taskDistList/list', params)
}
// 查询任务派发清单列表
export function taskDistList(params) {
  return http.get('/dizai/taskDistList/list', params, {
    throwRes: true
  })
}

// 查询任务派发清单列表（含最新流程节点）
export function taskDistListLastProcess(params) {
  return http.get('/dizai/taskDistList/list/latest-process-node', params, {
    throwRes: true
  })
}

// 导出任务派发清单列表
export function exportTaskDistList(data) {
  return http.post('/dizai/taskDistList/export', data, {
    responseType: 'blob',
    noCheckCode: true,
    throwRes: true
  })
}


// 获取任务派发清单详细信息
export function getTaskDetail(id) {
  return http.get(`/dizai/taskDistList/${id}`)
}

// 推送任务
export function pushTaskDistList(data) {
  return http.post(`/dizai/taskDistList/push`, data)
}

// 催办任务
export function promoteTaskList(id) {
  return http.post(`/dizai/taskDistList/remind/${id}`)
}

//统计不同角色的数量
export function getTaskInfoWithRole(params) {
  return http.post(`/dizai/role/stat`, params)
}

// 统计每日任务数量
export function getStatDay(params) {
  return http.get(`/dizai/taskDistList/stat-day`, params)
}

// 新增任务派发清单
export function addTaskDistList(data) {
  return http.post(`/dizai/taskDistList`, data)
}

// 修改任务派发清单
export function updateTaskDistList(data) {
  return http.put(`/dizai/taskDistList`, data)
}

// 删除任务派发清单
export function deleteTaskDistList(ids) {
  return http.delete(`/dizai/taskDistList/${ids}`)
}

// 获取发送任务的短信列表
export function getSendSmsList(params) {
  return http.post(`/dizai/taskDistList/push/preview-sms`, params)
}

// 获取自动托管状态
export function getAutoTaskStatus() {
  return http.get(`/dizai/autoMode/status`)
}

// 开启自动托管
export function openAutoTask() {
  return http.post(`/dizai/autoMode/open`)
}

// 关闭自动托管
export function closeAutoTask() {
  return http.post(`/dizai/autoMode/close`)
}

// 通过任务 chainId 查询整条流程链路
export function getTaskProcess(chainId) {
  return http.get(`/dizai/taskDistList/process-chain/${chainId}`)
}

// 通过任务 chainId 查询 AI 分析与推理链。
export function getTaskInference(chainId) {
  return http.get(`/dizai/taskDistList/process-chain/ai/${chainId}`)
}

// 查询 AI 托管概览
export function getAiOverview() {
  return http.get(`/dizai/autoMode/ai-hosting/overview`)
}

// 查询 AI 托管历史记录。
export function getAiHistory(params) {
  return http.get(`/dizai/autoMode/ai-hosting/records`, params)
}

// 获取现场反馈内容
export function getFeedbackContent(chainId) {
  return http.get(`/dizai/taskDistList/process-chain/histories`, { chainId })
}

// 获取能力
export function getAbility(chainId) {
  return http.get(`/dizai/taskDistList/process-chain/capabilities/${chainId}`)
}

// 通过历史记录 id 查询任务提交历史。
export function getTaskHistory(id) {
  return http.get(`/dizai/taskDistList/histories/${id}`)
}

// 查询每条可展示主链的最新流程节点。
export function getLatestProcessNode(params) {
  return http.get(`/dizai/taskProcessChainNode/list/latest-process-node`, params, {
    throwRes: true
  })
}

// 通过chainId催办
export function promoteTaskProcess(data) {
  return http.post(`dizai/taskDistList/remind/by-chain-scope`, data)
}

// 通过chainId删除
export function deleteTaskProcess(data) {
  return http.delete(`/dizai/taskDistList/by-chain-scope`, data)
}