import { ref, reactive, toValue } from 'vue'
import { getMeetingTranscripts } from '@/api/meeting.js'
import { mergeSpeechText } from '@/utils/meetingTranscript.js'

export const MEETING_TRANSCRIPT_TOPIC = 'meeting.transcript.v1'

/**
 * 视频会商 LiveKit 实时字幕流接入与历史记录 Composable
 * 基于 LiveKit Text Stream (meeting.transcript.v1)
 *
 * 核心特性：
 * 1. 初次入会调用一次持久化接口获取之前的会议发言历史
 * 2. 实时流驱动发言动态识别（partial 打字机流式呈现，final 定稿）
 * 3. 连续发言智能合并规则：
 *    - 无论上一句是 partial 还是已 final，只要下一句说话的还是同一个人，就继续拼接到上一句后面；
 *    - 当且仅当发言人切换（上一句不是我）时，才新起一行卡片展示。
 * 4. 协议健壮性保障：
 *    - 发送者身份核对：transcriber-{taskId}
 *    - 稳定片段键：${taskId}:${segmentId}
 *    - 防乱序与重复包丢弃：revision <= oldRevision
 *    - 终态保护：final 或 aborted 后拒绝后续迟到的 partial
 *
 * @param {Object} options
 * @param {import('vue').Ref<string> | Function | string} options.meetingId 会议ID
 * @param {Function} [options.getParticipantName] 根据 userId 解析发言人名称的回调
 */
export function useLiveMeetingTranscript(options = {}) {
  const { meetingId, getParticipantName } = options

  // 展示在界面上的段落卡片列表
  const liveTranscripts = ref([])

  // 映射表 Map<segmentKey, { paragraph, segment }>
  // 维护片段与段落卡片的所属关系，用于 O(1) 定位与就地响应式热替换
  const segmentMap = new Map()

  // 历史记录是否已加载过（确保一场会议初次入会只拉取一次）
  let historyLoaded = false
  const historyLoading = ref(false)

  // 当前绑定的 LiveKit Room 实例引用
  let currentRoom = null

  /**
   * 解析参会人显示名称
   * @param {string|number} userId
   * @returns {string}
   */
  const resolveSpeakerName = (userId) => {
    const uid = String(userId || '')
    if (!uid) return '未识别发言人'
    if (typeof getParticipantName === 'function') {
      const name = getParticipantName(uid)
      if (name) return name
    }
    return '参会人'
  }

  /**
   * 追加一条持久化历史记录并应用同人拼接规则
   * @param {Object} record
   */
  const appendHistoricalRecord = (record) => {
    if (!record) return
    const taskId = record.taskId ? String(record.taskId) : 'hist'
    const segmentId = record.segmentId ? String(record.segmentId) : String(record.id || Math.random())
    const segmentKey = `${taskId}:${segmentId}`

    if (segmentMap.has(segmentKey)) return

    const userId = String(record.userId || '')
    const text = String(record.text || record.content || '').trim()
    if (!text) return

    const speakerName = record.speakerName || resolveSpeakerName(userId)

    const newSegment = {
      taskId,
      segmentId,
      segmentKey,
      userId,
      revision: 999999, // 历史记录视为定稿最高版本
      type: 'final',
      text,
      startedAt: record.startedAt,
      endedAt: record.endedAt,
      mock: Boolean(record.mock),
    }

    const lastPara = liveTranscripts.value[liveTranscripts.value.length - 1]
    if (lastPara && String(lastPara.userId) === userId) {
      // 连续同一发言人：拼接到上一句后面
      lastPara.segments.push(newSegment)
      lastPara.text = lastPara.segments.map((s) => s.text).reduce((a, b) => mergeSpeechText(a, b), '')
      lastPara.type = 'final'
      if (newSegment.endedAt) lastPara.endedAt = newSegment.endedAt
      segmentMap.set(segmentKey, { paragraph: lastPara, segment: newSegment })
    } else {
      // 换人或首句：新起一行
      const newPara = reactive({
        id: segmentKey,
        userId,
        speakerName,
        startedAt: record.startedAt || new Date().toISOString(),
        endedAt: record.endedAt || '',
        type: 'final',
        text,
        segments: [newSegment],
      })
      liveTranscripts.value.push(newPara)
      segmentMap.set(segmentKey, { paragraph: newPara, segment: newSegment })
    }
  }

  /**
   * 初次入会拉取一次历史持久化聊天记录
   */
  const loadHistoricalTranscripts = async () => {
    const currentId = String(toValue(meetingId) || '')
    if (!currentId || historyLoaded) return

    historyLoading.value = true
    try {
      const res = await getMeetingTranscripts(currentId, { pageNum: 1, pageSize: 100 })
      const list = Array.isArray(res) ? res : Array.isArray(res?.data) ? res.data : []

      if (list.length > 0) {
        // 按起始时间升序排列，按时间线组织
        const sorted = [...list].sort((a, b) => {
          const tA = a.startedAt ? new Date(a.startedAt).getTime() : 0
          const tB = b.startedAt ? new Date(b.startedAt).getTime() : 0
          return tA - tB
        })

        for (const item of sorted) {
          appendHistoricalRecord(item)
        }
      }
      historyLoaded = true
    } catch (error) {
      console.warn('[LiveTranscript] 初次拉取历史会议记录失败:', error)
    } finally {
      historyLoading.value = false
    }
  }

  /**
   * 处理实时收到的单条字幕流消息
   * @param {Object} msg 字幕流消息体
   * @param {Object} [participantInfo] 发送者 LiveKit 身份信息
   */
  const handleTranscriptMessage = (msg, participantInfo) => {
    if (!msg || typeof msg !== 'object') return

    // 1. 会议 ID 核对
    const currentId = String(toValue(meetingId) || '')
    if (currentId && String(msg.meetingId) !== currentId) {
      return
    }

    // 2. 字段基础校验
    const taskId = String(msg.taskId || '').trim()
    const segmentId = String(msg.segmentId || '').trim()
    const userId = String(msg.userId || '').trim()
    const revision = Number(msg.revision)

    if (!taskId || !segmentId || Number.isNaN(revision)) {
      return
    }

    // 3. 发送者身份校验：LiveKit 提供的 identity 必须等于 transcriber-{taskId}
    if (participantInfo?.identity) {
      const expectedIdentity = `transcriber-${taskId}`
      if (participantInfo.identity !== expectedIdentity) {
        console.warn(
          `[LiveTranscript] 丢弃非法发送者字幕: expected=${expectedIdentity}, actual=${participantInfo.identity}`,
        )
        return
      }
    }

    const segmentKey = `${taskId}:${segmentId}`
    const existing = segmentMap.get(segmentKey)

    if (existing) {
      const { paragraph, segment } = existing

      // 4.1 终态保护：旧值为 final 或 aborted 时拒绝任何后续 partial
      const isTerminal = segment.type === 'final' || segment.type === 'aborted'
      if (isTerminal && msg.type === 'partial') {
        return
      }

      // 4.2 防乱序与重复包丢弃：版本不大于旧值即忽略
      if (revision <= segment.revision) {
        return
      }

      // 4.3 原地热更新片段属性
      segment.revision = revision
      segment.type = msg.type || 'partial'
      segment.text = String(msg.text || '')
      if (msg.startedAt) segment.startedAt = msg.startedAt
      if (msg.endedAt) segment.endedAt = msg.endedAt
      if (typeof msg.mock === 'boolean') segment.mock = msg.mock

      // 4.4 重新计算段落的全部拼接文本与当前状态
      paragraph.text = paragraph.segments.map((s) => s.text).reduce((a, b) => mergeSpeechText(a, b), '')
      const lastSeg = paragraph.segments[paragraph.segments.length - 1]
      paragraph.type = lastSeg.type
      if (lastSeg.endedAt) paragraph.endedAt = lastSeg.endedAt

      // 刷新姓名（若之前为未识别）
      if (!paragraph.speakerName || paragraph.speakerName === '未识别发言人' || paragraph.speakerName === '参会人') {
        paragraph.speakerName = resolveSpeakerName(userId)
      }
    } else {
      // 4.5 新增片段
      const newSegment = {
        taskId,
        segmentId,
        segmentKey,
        userId,
        revision,
        type: msg.type || 'partial',
        text: String(msg.text || ''),
        startedAt: msg.startedAt || new Date().toISOString(),
        endedAt: msg.endedAt || '',
        mock: Boolean(msg.mock),
      }

      const lastPara = liveTranscripts.value[liveTranscripts.value.length - 1]
      if (lastPara && String(lastPara.userId) === userId) {
        // 用户规则：如果下一句说话的还是我，就继续拼接到上一句后面
        lastPara.segments.push(newSegment)
        lastPara.text = lastPara.segments.map((s) => s.text).reduce((a, b) => mergeSpeechText(a, b), '')
        lastPara.type = newSegment.type
        if (newSegment.endedAt) lastPara.endedAt = newSegment.endedAt
        segmentMap.set(segmentKey, { paragraph: lastPara, segment: newSegment })
      } else {
        // 用户规则：上一句不是我才新加一行
        const speakerName = resolveSpeakerName(userId)
        const newPara = reactive({
          id: segmentKey,
          userId,
          speakerName,
          startedAt: msg.startedAt || new Date().toISOString(),
          endedAt: msg.endedAt || '',
          type: msg.type || 'partial',
          text: newSegment.text,
          segments: [newSegment],
        })
        liveTranscripts.value.push(newPara)
        segmentMap.set(segmentKey, { paragraph: newPara, segment: newSegment })
      }
    }
  }

  /**
   * LiveKit 实时短文本流处理器
   * @param {import('livekit-client').TextStreamReader} reader
   * @param {{ identity: string }} participantInfo
   */
  const handleTranscriptStream = async (reader, participantInfo) => {
    try {
      if (!reader || typeof reader.readAll !== 'function') return
      const rawText = await reader.readAll()
      if (!rawText) return

      const msg = JSON.parse(rawText)
      handleTranscriptMessage(msg, participantInfo)
    } catch (err) {
      console.warn('[LiveTranscript] 解析短文本流失败:', err)
    }
  }

  /**
   * 在 LiveKit Room 实例上注册文本流监听并加载历史记录
   * @param {Object} room
   */
  const setupTranscriptStream = (room) => {
    if (!room) return
    currentRoom = room

    // 初次入会拉取一次历史记录
    loadHistoricalTranscripts()

    try {
      if (typeof room.registerTextStreamHandler === 'function') {
        room.registerTextStreamHandler(MEETING_TRANSCRIPT_TOPIC, handleTranscriptStream)
      } else {
        console.warn('[LiveTranscript] 当前 LiveKit Room 不支持 registerTextStreamHandler')
      }
    } catch (e) {
      console.error('[LiveTranscript] 注册实时字幕流监听失败:', e)
    }
  }

  /**
   * 注销 LiveKit 文本流监听
   * @param {Object} [room]
   */
  const cleanupTranscriptStream = (room = currentRoom) => {
    if (!room) return
    try {
      if (typeof room.unregisterTextStreamHandler === 'function') {
        room.unregisterTextStreamHandler(MEETING_TRANSCRIPT_TOPIC)
      }
    } catch (e) {
      console.warn('[LiveTranscript] 注销实时字幕流失败:', e)
    }
    if (currentRoom === room) {
      currentRoom = null
    }
  }

  /**
   * 清空本地字幕记录及历史加载状态
   */
  const clearTranscripts = () => {
    segmentMap.clear()
    liveTranscripts.value = []
    historyLoaded = false
  }

  /**
   * 动态刷新现有段落中的参会人名称
   */
  const refreshSpeakerNames = () => {
    if (!liveTranscripts.value.length) return
    liveTranscripts.value.forEach((para) => {
      const resolved = resolveSpeakerName(para.userId)
      if (resolved && resolved !== para.speakerName) {
        para.speakerName = resolved
      }
    })
  }

  return {
    liveTranscripts,
    historyLoading,
    setupTranscriptStream,
    cleanupTranscriptStream,
    clearTranscripts,
    loadHistoricalTranscripts,
    refreshSpeakerNames,
    handleTranscriptMessage,
  }
}
