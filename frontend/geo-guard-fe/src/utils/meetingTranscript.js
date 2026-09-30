import dayjs from 'dayjs'

/**
 * 语音文本拼接（中文直接连接，英文/数字间以空格分隔）
 * @param {string} text1 第一段文本
 * @param {string} text2 第二段文本
 * @returns {string} 拼接后的文本
 */
export function mergeSpeechText(text1, text2) {
  const t1 = String(text1 || '').trim()
  const t2 = String(text2 || '').trim()
  if (!t1) return t2
  if (!t2) return t1

  const lastChar = t1.slice(-1)
  const firstChar = t2.slice(0, 1)
  if (/[a-zA-Z0-9]/.test(lastChar) && /[a-zA-Z0-9]/.test(firstChar)) {
    return `${t1} ${t2}`
  }
  return `${t1}${t2}`
}

/**
 * 判断相邻两条发言是否属于同一个人且时间相差在阈值以内（默认 20ms）
 * @param {Object} prev 前一条记录
 * @param {Object} curr 后一条记录
 * @param {number} thresholdMs 时间阈值（毫秒），默认 20ms
 * @returns {boolean}
 */
export function isConsecutiveSpeech(prev, curr, thresholdMs = 20) {
  if (!prev || !curr) return false

  // 1. 发言人判断：同一个人（优先以 userId 对比，兜底对比 speakerName）
  const prevUserId = prev.userId ? String(prev.userId) : ''
  const currUserId = curr.userId ? String(curr.userId) : ''
  const prevSpeaker = String(prev.speakerName || '').trim()
  const currSpeaker = String(curr.speakerName || '').trim()

  const sameUser =
    (prevUserId && currUserId && prevUserId === currUserId) ||
    (prevSpeaker && currSpeaker && prevSpeaker === currSpeaker)

  if (!sameUser) return false

  // 2. 时间差判断
  const currStart = curr.startedAt ? dayjs(curr.startedAt).valueOf() : NaN
  const prevEnd = prev.endedAt ? dayjs(prev.endedAt).valueOf() : NaN
  const prevStart = prev.startedAt ? dayjs(prev.startedAt).valueOf() : NaN

  if (Number.isNaN(currStart)) return false

  // 若前一条存在结束时间，比较当前条开始时间与上一条结束时间的间隔
  if (!Number.isNaN(prevEnd)) {
    const gap = currStart - prevEnd
    // 间隔在 20ms 以内（允许微弱音频帧重叠，下限 -200ms）
    if (gap <= thresholdMs && gap >= -200) {
      return true
    }
    // 容错：若两段起始时间几乎相同（<= 20ms）
    if (!Number.isNaN(prevStart) && Math.abs(currStart - prevStart) <= thresholdMs) {
      return true
    }
    return false
  }

  // 若前一条无 endedAt，退化为比较起始时间差
  if (!Number.isNaN(prevStart)) {
    return Math.abs(currStart - prevStart) <= thresholdMs
  }

  return false
}

/**
 * 合并同一个人在 20ms 内因断句分割的连续发言记录
 * @param {Array} list 原始发言列表
 * @param {number} thresholdMs 阈值，默认 20ms
 * @returns {Array} 合并后的发言列表
 */
export function mergeConsecutiveTranscripts(list, thresholdMs = 20) {
  if (!Array.isArray(list) || !list.length) return []

  const result = []

  for (const item of list) {
    if (!item) continue
    const prev = result[result.length - 1]

    if (prev && isConsecutiveSpeech(prev, item, thresholdMs)) {
      // 合并文本并更新结束时间
      prev.text = mergeSpeechText(prev.text, item.text)
      if (item.endedAt) {
        prev.endedAt = item.endedAt
      }
    } else {
      // 保持原始数据不被污染
      result.push({
        ...item,
        text: String(item.text || ''),
      })
    }
  }

  return result
}
