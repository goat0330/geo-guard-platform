import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/index.js'

// 白名单路由
const whiteList = ['/login']

export default function setupPermissionGuard(router) {
  router.beforeEach(async (to, from, next) => {
    // AI Studio is a local-only development module backed by the local Python services.
    if (import.meta.env.DEV && to.path === '/ai-studio') {
      next()
      return
    }

    const userStore = useUserStore()
    const hasToken = userStore.token || getToken()

    if (!hasToken) {
      // 未登录状态
      if (whiteList.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      }
    } else {
      // 已登录状态访问登录页，重定向至首页
      if (to.path === '/login') {
        next('/')
      } else {
        // 如果无用户信息，尝试拉取一次
        if (!userStore.userInfo?.user) {
          try {
            await userStore.updateUserInfo()
            // 登录态恢复成功，启动全局 SSE 与心跳（内部有单例防重保护）
            userStore.startConnection()
            userStore.startHeartbeat()
            next()
          } catch (error) {
            console.error('Token失效或获取用户信息失败:', error)
            userStore.clearLoginState()
            next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
            return
          }
        } else {
          // 已有用户信息状态下，确保全局 SSE 与心跳持续运行
          userStore.startConnection()
          userStore.startHeartbeat()
          next()
        }
      }
    }
  })
}
