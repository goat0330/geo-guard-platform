import { getToken } from '@/utils/index.js'
import { useUserStore } from '@/store/user.js'
import { fetchEventSource } from '@microsoft/fetch-event-source'

export const createStreamRequest = ({
                                      url,
                                      method = 'POST',
                                      headers = {},
                                      body,
                                      onMessage,
                                      onOpen,
                                      useRetry = false,
                                      longConnection = false,
                                    }) => {
  let controller = null
  let retryCount = 0
  const MAX_RETRY = 5

  let closedByUser = false
  let reconnectTimer = null
  let reconnecting = false

  const abort = () => {
    closedByUser = true
    reconnecting = false

    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    controller?.abort()
  }

  const connect = async () => {
    if (closedByUser) return

    const currentToken = getToken()
    // 长连接场景下若无 Token，不应发起请求并停止重试
    if (longConnection && !currentToken) {
      abort()
      return
    }

    controller = new AbortController()

    try {
      await fetchEventSource(url, {
        method,

        headers: {
          'Content-Type': 'application/json',
          [import.meta.env.VITE_APP_TOKEN_KEY]: currentToken,
          ...headers,
        },

        body: body ? JSON.stringify(body) : undefined,

        signal: controller.signal,

        openWhenHidden: true,

        // 禁用内置重试，由外部精确控制重试与状态
        retry: 0,

        async onopen(res) {
          // 401/403 鉴权失败，立即终止重试，避免死循环
          if (res.status === 401 || res.status === 403) {
            abort()
            throw new Error(`HTTP ${res.status} Unauthorized`)
          }

          if (res.status !== 200) {
            throw new Error(`HTTP ${res.status}`)
          }

          // 连接成功，重置重试次数
          retryCount = 0

          onOpen?.()
        },

        onmessage(msg) {
          parseSSEEvent(msg?.data, onMessage)
        },

        onclose() {
          // 服务端关闭连接
          throw new Error('SSE closed')
        },

        onerror(err) {
          // 抛出去走 catch
          throw err
        },
      })
    } catch (err) {
      if (closedByUser) return

      if (useRetry || longConnection) {
        retry()
      }
    }
  }

  const retry = () => {
    if (closedByUser || reconnecting) return

    // 长连接重连前检查 Token，若已退出登录则停止重连
    if (longConnection && !getToken()) {
      abort()
      return
    }

    if (!longConnection && retryCount >= MAX_RETRY) {
      return
    }

    reconnecting = true
    retryCount++

    controller?.abort()

    reconnectTimer = setTimeout(() => {
      reconnecting = false
      connect()
    }, 3000)
  }

  connect()

  return {
    abort,
    isClosed: () => closedByUser,
  }
}

export const parseSSEEvent = (dataStr, onMessage) => {
  try {
    onMessage?.(JSON.parse(dataStr))
  } catch {
    onMessage?.(dataStr)
  }
}
