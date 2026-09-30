import http from '@/utils/http.js'

// 获取消息列表
export const getMessageList = async (params) => {
  return http.get('/dizai/msgNotice/list', params, {
    throwRes: true,
  })
}

// 统计业务消息通知状态
export const getMessageNoticeStat = async (params) => {
  return http.get('/dizai/msgNotice/countStat', params)
}

// 删除业务消息通知
export const deleteMessageNotice = async (ids) => {
  return http.delete(`/dizai/msgNotice/${ids}`)
}

// 处理业务消息通知
export const dealMessageNotice = async (params) => {
  return http.post(`/dizai/msgNotice/handle`, params)
}
