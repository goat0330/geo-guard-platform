import axios from 'axios'

import { ElMessage } from 'element-plus'

import { encryptBase64, encryptWithAes, generateAesKey } from '@/utils/crypto.js'
import { encrypt } from '@/utils/jsencrypt.js'
import { getToken } from '@/utils/index.js'
import { useUserStore } from '@/store/user.js'

// 创建 axios 实例
const instance = axios.create({
  baseURL: '/api',
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
    // clientId为固定值
    // clientId: import.meta.env.VITE_APP_CLIENT_ID,
  },
})

// 请求拦截器：确保每次请求附带最新的 token
instance.interceptors.request.use(
  (config) => {
    const token = getToken()
    const tokenKey = import.meta.env.VITE_APP_TOKEN_KEY || 'bwy-token'
    if (token) {
      config.headers = config.headers || {}
      config.headers[tokenKey] = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

/**
 * 防抖错误提示，确保500ms内只触发一次ElMessage.error，触发后重置计时
 */
let lastErrorTime = 0
const showError = (message) => {
  const now = Date.now()
  if (now - lastErrorTime >= 500) {
    ElMessage.error(message)
    lastErrorTime = now
  }
}

/** 主动取消请求属于正常的请求替换流程，不应作为接口异常提示。 */
const isRequestCanceled = (error) => (
  axios.isCancel(error)
  || error?.name === 'AbortError'
  || error?.name === 'CanceledError'
  || error?.code === 'ERR_CANCELED'
)

/**
 * 发起HTTP请求
 * @param {string} type - 请求类型 (get, post, put, delete, patch)
 * @param {string} url - 请求URL
 * @param {object|null} params - 请求参数
 * @param {object} config - 请求配置
 * @returns {Promise} 返回处理后的响应数据或错误
 */
function request(type, url, params, config = {}) {
  return new Promise(async (resolve, reject) => {
    try {
      instance.defaults.headers[import.meta.env.VITE_APP_TOKEN_KEY] = getToken()

      // 是否需要加密
      const isEncrypt = config?.headers?.isEncrypt === true

      // 开启请求体加密
      if (import.meta.env.VITE_APP_ENCRYPT === 'true' && isEncrypt && (type === 'post' || type === 'put')) {
        // 生成一个 AES 密钥
        const aesKey = generateAesKey()
        instance.defaults.headers[import.meta.env.VITE_APP_ENCRYPT_KEY] = encrypt(encryptBase64(aesKey))
        params =
          typeof params === 'object' ? encryptWithAes(JSON.stringify(params), aesKey) : encryptWithAes(params, aesKey)
      } else {
        delete instance.defaults.headers[import.meta.env.VITE_APP_ENCRYPT_KEY]
      }
      // 发起请求
      let res = await instance({
        method: type,
        url,
        data: type !== 'get' ? params : null,
        params: type === 'get' ? params : null,
        ...config,
      })
      /*****************************响应逻辑*************************************/
      const {
        status,
        data: { code, data, msg },
      } = res
      const successCodes = config.successCodes || [200]
      let success = true
      if ((status !== 200 || !successCodes.includes(code)) && !config.noCheckCode) {
        showError(msg || '请求失败')
        success = false
      }

      // 认证失败 / 登录过期
      if (code == 401 && !config.skipAuthLogout) {
        showError('认证失败或登录已过期，请重新登录')
        try {
          const userStore = useUserStore()
          await userStore.logout({ remote: false })
        } catch (_) {}
      }
      if (config.throwRes) {
        return resolve(res.data)
      } else {
        return success ? resolve(data) : reject(data)
      }
    } catch (error) {
      if (isRequestCanceled(error)) {
        if (config.throwRes) return resolve(error)
        return reject(error)
      }
      const status = error.response?.status
      const code = error.response?.data?.code
      // HTTP 401 或返回体中 code 为 401
      if ((status === 401 || code === 401) && !config.skipAuthLogout) {
        showError('认证失败或登录已过期，请重新登录')
        try {
          const userStore = useUserStore()
          await userStore.logout({ remote: false })
        } catch (_) {}
      } else if (!config.notUseError) {
        showError(error.message || '请求失败')
      }
      if (config.throwRes) {
        return resolve(error)
      }
      return reject(error)
    }
  })
}

const http = {}
;['get', 'post', 'put', 'delete', 'patch'].forEach((type) => {
  http[type] = (url, params, config) => request(type, url, params, config)
})

export default http
