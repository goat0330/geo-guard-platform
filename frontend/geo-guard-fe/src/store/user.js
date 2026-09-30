import { defineStore } from 'pinia'
import { ref, watchEffect } from 'vue'
import { useLocalStorage } from '@vueuse/core'
import { heartbeat, login as loginApi, logout as logoutApi } from '@/api/sso.js'
import { getMessageNoticeStat } from '@/api/notify.js'
import router from '@/router'
import { getPersssionRoutes, getUserArea, getUserInfo } from '@/api/user.js'
import { sseControl } from '@/api/chat.js'
import { eventBus, EventKey, NOTICE_KEY } from '@/utils/eventBus.js'
import { debounce } from 'lodash-es'
import { ElNotification } from 'element-plus'
import { getDefenseStatus as getDefenseStatusFun, queryUserQuestion, updateDefenseStatus } from '@/api/common.js'
import { createAdvancedHeartbeat, getToken } from '@/utils/index.js'
import { defRespPlanPageList } from '@/api/defense.js'
import { CUSTOM_RECORD_TYPE } from '@/utils/enum.js'
import { getDisasterDetail } from '@/api/deal.js'
import { useTaskStore } from '@/store/task.js'

export const useUserStore = defineStore('user', () => {
  const tokenLocalStorage = useLocalStorage(import.meta.env.VITE_APP_TOKEN_KEY, '')
  const userLocalStorage = useLocalStorage(import.meta.env.VITE_APP_USER_INFO_KEY, {})
  const token = ref(tokenLocalStorage.value || null)

  const userQuestion = ref(null)
  const userFiles = ref([])
  const userUploadType = ref('')
  const userCoordinates = ref(null)
  const userSpaceSelection = ref(null)
  const userInfo = ref(userLocalStorage.value || {})
  const buttonAuths = ref([])
  const routeAuths = ref([])
  const menuRoutes = ref([])
  const userArea = ref({})
  // 数字人模式持久化到本地，刷新或重新进入聊天页后保留用户选择。
  const isHumanMode = useLocalStorage('dizai-human-mode-enabled', false)
  // 深度思考模式持久化到本地，默认开启，刷新或重新进入聊天页后保留用户选择。
  const isDeepThink = useLocalStorage('dizai-deep-think-enabled-v2', true)
  // 自定义记录类型
  const customRecordTypes = ref({
    // 是否生成过一张图
    [CUSTOM_RECORD_TYPE.ONE_GRAPH]: false,
  })

  const sse = ref(null)
  // 是否开启防御响应
  const defenseStatus = ref(false)

  const heartbeatInterval = ref(null)

  const notice = ref(null)
  const noticeCount = ref(0)

  watchEffect(() => {
    if (tokenLocalStorage.value) {
      token.value = tokenLocalStorage.value
    }
    if (
      userLocalStorage.value &&
      typeof userLocalStorage.value === 'object' &&
      Object.keys(userLocalStorage.value).length > 0
    ) {
      userInfo.value = userLocalStorage.value
    }
  })

  function normalizeRoutePath(path = '', parentPath = '') {
    if (!path) return parentPath || '/'
    if (/^https?:\/\//.test(path)) return path
    if (path.startsWith('/')) return path
    return `${parentPath}/${path}`.replace(/\/+/g, '/')
  }

  function getDizaiRoutes(routes = []) {
    const routeList = Array.isArray(routes) ? routes : []
    const dizaiRoutes = routeList.find((item) => item.path === '/dizai')
    return dizaiRoutes?.children || routeList
  }

  function isHiddenRoute(route) {
    return route.hidden === true || route.hidden === 1 || route.hidden === 'true'
  }

  function collectRoutePaths(routes = [], parentPath = '') {
    return routes.reduce((paths, route) => {
      const fullPath = normalizeRoutePath(route.path, parentPath)
      paths.push(fullPath)
      if (route.children?.length) {
        paths.push(...collectRoutePaths(route.children, fullPath))
      }
      return paths
    }, [])
  }

  function buildMenuRoutes(routes = [], parentPath = '') {
    return routes
      .filter((route) => !isHiddenRoute(route))
      .map((route) => {
        const fullPath = normalizeRoutePath(route.path, parentPath)
        const children = buildMenuRoutes(route.children || [], fullPath)
        return {
          ...route,
          path: fullPath,
          children,
        }
      })
  }

  async function getMessageNoticeUrl(type, handleId) {
    const noticeType = Number(type)
    if (noticeType === 1) {
      const detail = await getDisasterDetail(handleId)
      return detail?.handleProcess < 3
        ? `/emergency-response-front?id=${handleId}`
        : `/emergency-response-detail?id=${handleId}`
    }
    const urlMapper = {
      1: `/emergency-response-detail?id=${handleId}`,
      2: `/defense-response-detail?id=${handleId}`,
      3: `prediction?reportId=${handleId}`,
      4: `/defense-response?isMessage=${handleId}`,
    }
    return urlMapper[noticeType]
  }

  async function setUserInfo(val) {
    const taskStore = useTaskStore()
    userInfo.value = val || {}
    userLocalStorage.value = val || {}
    if (val) {
      localStorage.setItem(import.meta.env.VITE_APP_USER_INFO_KEY, JSON.stringify(val))
    }
    const { permissions, routes } = val || {}
    buttonAuths.value = permissions || []
    const permissionRoutes = getDizaiRoutes(routes)
    routeAuths.value = collectRoutePaths(permissionRoutes)
    menuRoutes.value = buildMenuRoutes(permissionRoutes)
    // 获取防御响应状态
    try {
      await getDefenseStatus()
    } catch (e) {
      console.warn('获取防御响应状态异常', e)
    }
    // 获取用户关联行政区划信息
    try {
      if (val?.user?.userId) {
        userArea.value = await getUserArea(val?.user?.userId)
      }
    } catch (e) {
      console.error(e)
    }
    // 获取是否开生成风险研判报文
    try {
      customRecordTypes.value[CUSTOM_RECORD_TYPE.ONE_GRAPH] =
        (await queryUserQuestion({
          businessType: CUSTOM_RECORD_TYPE.ONE_GRAPH,
          scope: 1,
        })) || false
    } catch (e) {
      console.error(e)
    }
  }

  function removeUserInfo() {
    userInfo.value = {}
    userLocalStorage.value = {}
    localStorage.removeItem(import.meta.env.VITE_APP_USER_INFO_KEY)
    routeAuths.value = []
    menuRoutes.value = []
    buttonAuths.value = []
    userArea.value = {}
    noticeCount.value = 0
  }

  function setToken(val) {
    token.value = val
    tokenLocalStorage.value = val || ''
    if (val) {
      localStorage.setItem(import.meta.env.VITE_APP_TOKEN_KEY, val)
    } else {
      localStorage.removeItem(import.meta.env.VITE_APP_TOKEN_KEY)
    }
  }

  function removeToken() {
    token.value = null
    tokenLocalStorage.value = ''
    localStorage.removeItem(import.meta.env.VITE_APP_TOKEN_KEY)
  }

  function clearLoginState() {
    stopConnection()
    stopHeartbeat()
    removeToken()
    removeUserInfo()
  }

  function saveUserQuestion(question) {
    userQuestion.value = question
  }

  function clearUserQuestion() {
    userQuestion.value = null
  }

  // 获取消息统计--非必要接口，捕获异常避免影响用户体验
  async function getUnreadCount() {
    try {
      let res = await getMessageNoticeStat()
      noticeCount.value = res.unreadCount || 0
    } catch (error) {
      console.error('获取消息通知统计失败', error)
      noticeCount.value = 0
    }
  }

  async function updateUserInfo() {
    // 获取用户信息
    let [userRes, routeRes] = await Promise.all([getUserInfo(), getPersssionRoutes()])
    await setUserInfo({
      ...userRes,
      routes: routeRes,
    })
  }

  async function login(params) {
    const res = await loginApi(params)
    const tokenVal = typeof res === 'string' ? res : res?.access_token || res?.token
    setToken(tokenVal)
    try {
      await updateUserInfo()
    } catch (e) {
      console.warn('获取用户信息异常', e)
    }
    return res
  }

  async function logout(options = {}) {
    const { remote = true } = options
    try {
      if (remote) {
        await logoutApi(
          {},
          {
            notUseError: true,
            noCheckCode: true,
            skipAuthLogout: true,
          },
        )
      }
    } catch (error) {
      console.error('退出登录接口调用失败', error)
    } finally {
      clearLoginState()
    }
    await router.push('/login')
  }

  // 全局调用一次消息推送sse
  const onMessage = (data) => {
    eventBus.emit(EventKey.CHAT_MSG, data)
    const obj = data?.data

    // 会议邀请与纪要分享统一使用 notify-custom 推送。
    if (data?.type === 'notify-custom' && ['meeting-invited', 'meeting-summary-shared'].includes(obj?.type)) {
      const targetMeetingId = obj?.meetingId ? String(obj.meetingId) : ''
      getUnreadCount()
      eventBus.emit('MESSAGE_CENTER_CHANGED')
      if (obj.type === 'meeting-invited') {
        eventBus.emit('MEETING_INVITED', { meetingId: targetMeetingId })
      }
      if (targetMeetingId) {
        if (notice.value) {
          notice.value.close()
        }
        const isSummary = obj.type === 'meeting-summary-shared'
        const elNotice = ElNotification({
          title: isSummary ? '会商纪要' : '会商研判邀请',
          message: isSummary ? '您收到一份新的会商纪要，点击查看' : '您收到一个新的会商研判视频会议邀请，点击加入',
          type: 'success',
          position: 'top-right',
          duration: 0,
          onClick: () => {
            elNotice.close()
            if (isSummary) {
              router.push({
                name: 'MessageCenter',
                query: { m_type: 'meeting-summary', meeting_id: targetMeetingId },
              })
            } else {
              eventBus.emit('OPEN_MEETING_FROM_NOTICE', { meetingId: targetMeetingId })
            }
          },
        })
        notice.value = elNotice
      }
      return
    }

    // 适配纪要分享提醒：type 为 notify-custom，data.type 为 meeting-summary-shared
    if (data?.type === 'notify-custom' && obj?.type === 'meeting-summary-shared') {
      const targetMeetingId = obj?.meetingId ? String(obj.meetingId) : ''
      if (targetMeetingId) {
        if (notice.value) {
          notice.value.close()
        }
        const elNotice = ElNotification({
          title: '会商纪要提醒',
          message: '您收到一份会商纪要分享，点击查看',
          type: 'success',
          position: 'top-right',
          duration: 0,
          onClick: () => {
            elNotice.close()
            eventBus.emit('OPEN_MINUTES_FROM_NOTICE', { meetingId: targetMeetingId })
          },
        })
        notice.value = elNotice
      }
      return
    }

    const myKeys = Object.values(NOTICE_KEY)
    if (myKeys.includes(obj?.type)) {
      // 自定义key处置
      const data = obj?.data || null
      eventBus.emit(obj.type, data)
    } else if (data.type == NOTICE_KEY.REFRESH_AI_MANAGE_NOTICE) {
      eventBus.emit(NOTICE_KEY.REFRESH_AI_MANAGE_NOTICE, data)
    } else {
      messageNotice(obj, data?.type)
    }
  }

  // 消息弹出通知
  function messageNotice(data, itemType) {
    let obj = data?.bizData || data
    if (typeof data?.bizData === 'string') {
      try {
        obj = JSON.parse(data?.bizData)
      } catch (e) {
        console.warn('解析 bizData 异常', e)
      }
    }
    if (obj) {
      if (notice.value) {
        notice.value.close()
      }

      // 业务消息中心记录：meeting-summary
      if (itemType === 'meeting-summary' || obj?.type === 'meeting-summary-shared') {
        const targetMeetingId = obj?.meetingId ? String(obj.meetingId) : ''
        if (targetMeetingId) {
          const elNotice = ElNotification({
            title: '会商纪要提醒',
            message: '您收到一份会商纪要分享，点击查看',
            type: 'success',
            position: 'top-right',
            duration: 0,
            onClick: () => {
              elNotice.close()
              eventBus.emit('OPEN_MINUTES_FROM_NOTICE', { meetingId: targetMeetingId })
            },
          })
          notice.value = elNotice
          return
        }
      }

      // 业务消息中心记录的 type 为 meeting
      if (itemType === 'meeting' || obj?.meetingId) {
        const targetMeetingId = obj?.meetingId ? String(obj.meetingId) : ''
        if (targetMeetingId) {
          const elNotice = ElNotification({
            title: '会商会议通知',
            message: '您有一条会商会议消息，点击查看或加入',
            type: 'success',
            position: 'top-right',
            duration: 0,
            onClick: () => {
              elNotice.close()
              eventBus.emit('OPEN_MEETING_FROM_NOTICE', { meetingId: targetMeetingId })
            },
          })
          notice.value = elNotice
          return
        }
      }

      const handleId = obj?.handleId
      if (handleId) {
        const type = obj?.meetingType || 1
        // 审批类型 1：专家 2：行政
        const approvalType = obj?.approvalType || 1
        const elNotice = ElNotification({
          title: approvalType == 1 ? '会议邀请' : '审批',
          message: approvalType == 1 ? '请点击加入会议' : '请点击跳转审批页面',
          type: 'success',
          position: 'top-right',
          duration: 0,
          onClick: async () => {
            const url = await getMessageNoticeUrl(type, handleId)
            await router.push(url)
            elNotice.close()
            window.location.reload()
          },
        })
        notice.value = elNotice
      }
    }
  }

  // 处理消息通知
  async function messageHandle(bizData, itemType) {
    let data = bizData || {}
    if (typeof bizData === 'string') {
      try {
        data = JSON.parse(bizData)
      } catch (e) {
        console.warn('解析 bizData 异常', e)
      }
    }

    // 业务消息中心点击处理：meeting-summary
    if (itemType === 'meeting-summary' || data?.type === 'meeting-summary-shared') {
      const targetMeetingId = data?.meetingId ? String(data.meetingId) : ''
      if (targetMeetingId) {
        eventBus.emit('OPEN_MINUTES_FROM_NOTICE', { meetingId: targetMeetingId })
      }
      return
    }

    // 业务消息中心点击处理
    if (itemType === 'meeting' || data?.meetingId) {
      const targetMeetingId = data?.meetingId ? String(data.meetingId) : ''
      if (targetMeetingId) {
        eventBus.emit('OPEN_MEETING_FROM_NOTICE', { meetingId: targetMeetingId })
      }
      return
    }

    const handleId = data?.handleId
    const type = data?.meetingType || 1
    await router.push(await getMessageNoticeUrl(type, handleId))
  }

  // 开启全局sse（单例保护：全局仅维护一个连接）
  function startConnection() {
    const currentToken = token.value || getToken()
    // 未登录或无有效 Token 时不建立连接
    if (!currentToken) return

    // 如果已有存活的 sse 连接且未被关闭，直接复用，不重复创建
    if (sse.value && !sse.value.isClosed?.()) {
      return
    }

    stopConnection()
    sse.value = sseControl({ onMessage })
  }

  // 关闭sse
  function stopConnection() {
    if (sse.value) {
      sse.value?.abort()
      sse.value = null
    }
  }

  async function heartbeats() {
    if (token.value) {
      await heartbeat()
      getUnreadCount()
    }
  }

  // 开启心跳（单例保护）
  function startHeartbeat() {
    const currentToken = token.value || getToken()
    if (!currentToken) return

    // 如果心跳已在运行，避免重复重建
    if (heartbeatInterval.value) {
      return
    }
    heartbeatInterval.value = createAdvancedHeartbeat(heartbeats, { interval: 10000, retry: 1 })
    heartbeatInterval.value?.start()
  }

  // 关闭心跳
  function stopHeartbeat() {
    if (heartbeatInterval.value) {
      heartbeatInterval.value?.stop()
      heartbeatInterval.value = null
    }
  }

  // 获取防御响应状态
  async function getDefenseStatus() {
    // 存在开启的防御响应就变状态
    const res = await defRespPlanPageList({
      process: [1, 2, 3, 4, 5],
    })
    const data = res.data || []
    defenseStatus.value = !!data.length
    const status = await getDefenseStatusFun()
    if (!!status !== defenseStatus.value) {
      await toggleDefenseStatus(!!defenseStatus.value)
    }
  }

  // 开启/关闭防御响应状态
  async function toggleDefenseStatus(status) {
    await updateDefenseStatus(status ? 1 : 0)
  }

  return {
    token,
    userQuestion,
    userFiles,
    userUploadType,
    userCoordinates,
    userSpaceSelection,
    saveUserQuestion,
    clearUserQuestion,
    setToken,
    removeToken,
    clearLoginState,
    logout,
    login,
    setUserInfo,
    removeUserInfo,
    userInfo,
    noticeCount,
    updateUserInfo,
    startConnection,
    stopConnection,
    buttonAuths,
    routeAuths,
    menuRoutes,
    defenseStatus,
    getDefenseStatus,
    toggleDefenseStatus,
    startHeartbeat,
    stopHeartbeat,
    messageHandle,
    messageNotice,
    userArea,
    isHumanMode,
    isDeepThink,
    customRecordTypes,
  }
})
