import http from '@/utils/http.js'
import { createStreamRequest } from '@/utils/sse.js'

/********************对话相关--自有库版本***********************/
// 获取历史会话列表
export function getHistoryListOwn(params) {
  return http.get('/dizai/ai/agent/chatHistory/list', params, { throwRes: true })
}

// 获取会话详情
export function getChatMsg(params) {
  return http.get('/dizai/ai/agent/chatHistoryDetail/list', params)
}

// 修改会话历史
export function modifyChatMsg(params) {
  return http.put('/dizai/ai/agent/chatHistory', params)
}

// 修改会话历史 chatbox
export function editChatMsg(params) {
  return http.put('/dizai/ai/agent/chatHistoryDetail', params)
}

// 点赞
export function updateLike(params) {
  return http.post(`/dizai/ai/agent/feedbacks/handle`, params)
}

/*******************************************/

// 获取历史会话列表
export function getHistoryList(params) {
  return http.get('/dizai/ai/agent/history/list-conversation', params, { throwRes: true })
}

// 获取会话详情
// export function getChatMsg(params) {
//   return http.get('/dizai/ai/agent/history/list-message', params)
// }

export function sseControl({ onMessage }) {
  return createStreamRequest({
    url: '/api/dizai/sse/connect',
    method: 'GET',
    onMessage,
    useRetry: true,
    longConnection: true,
  })
}

// 删除回话历史
export function deleteHistory(ids) {
  return http.delete(`/dizai/ai/agent/chatHistory/${ids}`)
}

// 对话接口
export function chat({ params, onMessage }) {
  return createStreamRequest({
    url: '/api/dizai/ai/agent/chat',
    onMessage,
    body: params,
  })
}

// llm任务处理
export function replyHandle(params) {
  return http.post('/dizai/llm/reply/handle', params)
}

// 通知指定用户信息
export const notifyUser = (data) => {
  return http.post('/dizai/sse/notifyByUserIds', data)
}

// 通知指定用户信息
export const noticeUsers = ({ userIds, type, data } = {}) => {
  const params = {
    userIds: userIds,
    content: {
      type,
      data,
    },
  }
  return http.post('/dizai/sse/notifyByUserIds', params)
}

//根据会话id查询哪些报告解析的数据已经入库
export const getReportParseResult = (params) => {
  return http.get('/dizai/dataAlarm/codesBySessionId', params)
}
