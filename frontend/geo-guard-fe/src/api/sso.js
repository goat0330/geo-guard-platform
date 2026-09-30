// 获取SSO地址，重定向值认证中心
import http from '@/utils/http.js'

export const getSSOUrl = (params) => {
  return http.get('/sso/getSsoAuthUrl', params)
}

// 根据ticket值登录
export const loginByTicket = (params, config) => {
  return http.get('/sso/doLoginByTicket', params, config)
}

// 登出
export const logout = (params, config = {}) => {
  return http.post('/auth/logout', params, config)
}

// 获取SSO服务器地址
export const getSSOServerUrl = (params) => {
  return http.get('/auth/sso-server', params)
}

// 生成图片验证码
export const getImageCode = (params) => {
  return http.get('/auth/code', params)
}

// 获取手机验证码
export const getPhoneCode = (params) => {
  return http.get('/resource/sms/code', params)
}

// 手机号重置密码
export const resetPasswordByPhone = (params, config = {}) => {
  return http.post('/auth/resetPwd/mobile', params, {
    ...config,
    headers: {
      isEncrypt: true,
      ...config.headers,
    },
    // headers: {
    //   'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
    // }
  })
}

// 登录
export const login = (data, config = {}) => {
  return http.post(
    '/auth/login',
    {
      ...data,
      clientId: import.meta.env.VITE_APP_CLIENT_ID,
    },
    {
      ...config,
      headers: {
        isEncrypt: true,
        ...config.headers,
      },
      // headers: {
      //   'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
      // }
    },
  )
}

// 心跳
export const heartbeat = (params) => {
  return http.post('/dizai/online/heartbeat', params, { notUseError: true })
}
