import http from '@/utils/http.js'

// 获取用户信息
export const getUserInfo = (config) => {
  return http.get('/system/user/getInfo', config)
}

// 获取路由权限
export const getPersssionRoutes = (params) => {
  return http.get('/system/menu/getRouters', params)
}

// 根据用户id获取行政区划关联
export const getUserArea = (userId) => {
  return http.get(`/dizai/userAdRegion/${userId}`, {}, { notUseError: true, noCheckCode: true })
}

// 获取用户列表
export const getUserList = (params) => {
  return http.get('/system/user/list', params)
}
