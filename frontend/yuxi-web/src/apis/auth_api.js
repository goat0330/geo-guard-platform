/**
 * 认证相关 API
 */

import {
  apiAdminGet,
  apiDelete,
  apiGet,
  apiPost,
  apiPut,
  apiSuperAdminGet,
  apiSuperAdminPost
} from './base'

/**
 * 获取 OIDC 配置
 * @returns {Promise<{enabled: boolean, provider_name?: string}>}
 */
async function getOIDCConfig() {
  return { enabled: false }
}

/**
 * 获取 OIDC 登录 URL
 * @param {string} redirectPath - 登录后的重定向路径
 * @returns {Promise<{login_url: string}>}
 */
async function getOIDCLoginUrl(redirectPath = '/') {
  const params = new URLSearchParams({ redirect_path: redirectPath })
  return apiGet(`/api/auth/oidc/login-url?${params}`, {}, false)
}

/**
 * 使用一次性 code 交换 OIDC 登录结果
 * @param {string} code - 一次性登录 code
 * @returns {Promise<{
 *   access_token: string,
 *   token_type: string,
 *   user_id: number,
 *   username: string,
 *   uid: string,
 *   phone_number: string | null,
 *   avatar: string | null,
 *   role: string,
 *   department_id: number | null,
 *   department_name: string | null
 * }>}
 */
async function getUserAccessOptions() {
  return apiAdminGet('/api/auth/users/access-options')
}

async function exchangeOIDCCode(code) {
  return apiPost('/api/auth/oidc/exchange-code', { code }, {}, false)
}

async function login(credentials) {
  const response = await apiPost(
    '/api/auth/login',
    {
      username: credentials.loginId,
      password: credentials.password,
      code: credentials.code,
      uuid: credentials.uuid
    },
    {},
    false
  )
  if (response?.code !== 200 || !response.data?.access_token) {
    const error = new Error(response?.msg || '本地登录失败')
    error.status = response?.code
    throw error
  }
  return {
    access_token: response.data.access_token,
    token_type: response.data.token_type || 'Bearer',
    user_id: 'local-dev-user',
    username: credentials.loginId,
    uid: credentials.loginId,
    role: 'superadmin',
    phone_number: null,
    avatar: null,
    department_id: null,
    department_name: null
  }
}

async function initialize(admin) {
  return apiPost('/api/auth/initialize', admin, {}, false)
}

async function checkFirstRun() {
  return false
}

async function getUsers({ skip = 0, limit = 100 } = {}) {
  const params = new URLSearchParams({ skip: String(skip), limit: String(limit) })
  return apiGet(`/api/auth/users?${params}`)
}

async function getUsersPage({ offset = 0, limit = 50, search, departmentId, role } = {}) {
  const params = new URLSearchParams({ offset: String(offset), limit: String(limit) })
  if (search) params.set('search', search)
  if (departmentId) params.set('department_id', String(departmentId))
  if (role) params.set('role', role)
  return apiAdminGet(`/api/auth/users/page?${params}`)
}

async function createUser(userData) {
  return apiPost('/api/auth/users', userData)
}

async function updateUser(userId, userData) {
  return apiPut(`/api/auth/users/${encodeURIComponent(userId)}`, userData)
}

async function deleteUser(userId) {
  return apiDelete(`/api/auth/users/${encodeURIComponent(userId)}`)
}

async function validateUsername(username) {
  return apiPost('/api/auth/validate-username', { username })
}

async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiPost('/api/auth/upload-avatar', formData)
}

async function getCurrentUser() {
  const response = await apiGet('/api/system/user/getInfo')
  if (response?.code !== 200 || !response.data?.user) {
    throw new Error(response?.msg || '无法读取本地用户信息')
  }
  const user = response.data.user
  return {
    id: user.userId,
    username: user.userName,
    uid: user.userId,
    avatar: user.avatar || '',
    role: response.data.roles?.includes('local-admin') ? 'superadmin' : 'user',
    phone_number: null,
    department_id: null,
    department_name: ''
  }
}

async function getLocalCaptcha() {
  const response = await apiGet('/api/auth/code', {}, false)
  if (response?.code !== 200 || !response.data?.img || !response.data?.uuid) {
    throw new Error(response?.msg || '验证码服务不可用')
  }
  return response.data
}

async function logout() {
  return apiPost('/api/auth/logout', {})
}

async function updateProfile(profileData) {
  return apiPut('/api/auth/profile', profileData)
}

async function checkUid(uid) {
  return apiSuperAdminGet(`/api/auth/check-uid/${encodeURIComponent(uid)}`)
}

async function impersonateUser(userId) {
  return apiSuperAdminPost(`/api/auth/impersonate/${encodeURIComponent(userId)}`, {})
}

async function getCLIAuthSession(userCode) {
  const encoded = encodeURIComponent(userCode)
  return apiGet(`/api/auth/cli/sessions/${encoded}`)
}

async function approveCLIAuthSession(userCode) {
  const encoded = encodeURIComponent(userCode)
  return apiPost(`/api/auth/cli/sessions/${encoded}/approve`, {})
}

export const authApi = {
  login,
  getLocalCaptcha,
  logout,
  initialize,
  checkFirstRun,
  getUsers,
  getUsersPage,
  createUser,
  updateUser,
  deleteUser,
  validateUsername,
  uploadAvatar,
  getCurrentUser,
  updateProfile,
  checkUid,
  impersonateUser,
  getOIDCConfig,
  getOIDCLoginUrl,
  getUserAccessOptions,
  exchangeOIDCCode,
  getCLIAuthSession,
  approveCLIAuthSession
}
