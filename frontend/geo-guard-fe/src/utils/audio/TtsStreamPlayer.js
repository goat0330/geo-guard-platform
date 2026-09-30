import { synthesizeTts } from '@/api/tts.js'
import { reactive, readonly } from 'vue'

const DEFAULT_OPTIONS = Object.freeze({
  // TTS 服务使用的默认音色 ID（IndexTTS v2 支持 'man'、'women' 或已注册声音）。
  voiceId: 'man',
  // 单次 TTS 请求文本最大 token 数。
  maxTokens: 120,
  // 同时进行语音合成的最大请求数（IndexTTS 默认并发为 1，单并发避免服务端 429 冲突）。
  concurrency: 1,
  // 播放当前片段时，最多提前合成的后续片段数量。
  preloadCount: 3,
  // 只有达到该长度的句子才会优先提交合成，避免请求过于零碎。
  minSegmentLength: 20,
  // 超过最小长度后，优先在该长度范围内寻找逗号、空格等弱断句点。
  preferredSegmentLength: 60,
  // 文本缓冲区达到该长度时强制断句，避免单段语音过长。
  maxSegmentLength: 120,
  // 流式回答暂停多久后，尝试将当前缓冲文本提前提交合成。
  idleFlushDelay: 800,
})

const STRONG_PUNCTUATION_REG = /[。！？；\n]/
const SOFT_PUNCTUATION_REG = /[，、：,;；\s]/
const MARKDOWN_TABLE_SEPARATOR_REG = /^[ \t]*\|?[ \t]*:?-{3,}:?(?:[ \t]*(?:\|[ \t]*|[ \t]+):?-{3,}:?)*[ \t]*\|?[ \t]*$/gm
const FENCED_CODE_BLOCK_REG = /(```|~~~)[\s\S]*?(?:\1|$)/g

// 播放器内部响应式状态，仅允许通过播放器生命周期修改。
const mutablePlaybackState = reactive({
  sessionId: 0,
  active: false,
})

// 消息组件订阅的只读播放状态。
export const ttsPlaybackState = readonly(mutablePlaybackState)

/**
 * 移除 Markdown 围栏代码块，未收到结束标记时也屏蔽后续内容。
 * @param {string} text - 当前累计的完整回答。
 * @returns {string} 移除 Mermaid 等围栏代码块后的文本。
 */
function removeFencedCodeBlocks(text = '') {
  return String(text).replace(FENCED_CODE_BLOCK_REG, ' ')
}

/**
 * 移除不应参与语音播报的图片内容，兼容流式传输中尚未闭合的图片标记。
 * @param {string} text - 当前累计的完整回答。
 * @returns {string} 已移除图片内容的文本。
 */
function removeImageContent(text = '') {
  return String(text)
    .replace(/<img\b[^>]*>/gi, ' ')
    .replace(/<img\b[^>]*$/gi, ' ')
    .replace(/!\[[\s\S]*?]\([^)]*\)/g, ' ')
    .replace(/!\[[^\]]*(?:]\([^)]*)?$/g, ' ')
    .replace(/\{(?:disaster_image|deformation_photo|damage_photo|remote_sensing_image)}/gi, ' ')
}

/**
 * 移除 Markdown 列表前缀，避免 TTS 将列表点或编号读出来。
 * @param {string} text - 当前累计的完整回答。
 * @returns {string} 已移除列表前缀的文本。
 */
function removeMarkdownListMarkers(text = '') {
  return String(text)
    .replace(/^[ \t]*[-+*•◦▪][ \t]+/gm, '')
    .replace(/^[ \t]*\d+[.)、．][ \t]+/gm, '')
}

/**
 * 移除流式回答中不应进入语音队列的内容。
 * @param {string} text - 当前累计的完整回答。
 * @returns {string} 可用于计算新增播报文本的内容。
 */
function getSpeechSourceText(text = '') {
  return removeMarkdownListMarkers(removeImageContent(removeFencedCodeBlocks(text)))
}

/**
 * 清理不适合直接播报的 Markdown、链接、引用和代码内容。
 * @param {string} text - 原始回答片段。
 * @returns {string} 可交给 TTS 的纯文本。
 */
export function cleanTextForSpeech(text = '') {
  return getSpeechSourceText(text)
    // Markdown 表格对齐分隔行只用于排版，不传给 TTS。
    .replace(MARKDOWN_TABLE_SEPARATOR_REG, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[(\d+,?)+]\((@ref|@re|@r|@)\)?/g, ' ')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/https?:\/\/\S+/g, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\$\$[\s\S]*?\$\$/g, ' ')
    .replace(/\$([^$]+)\$/g, '$1')
    .replace(/^[#>*|~]+/gm, ' ')
    .replace(/[|_*~]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * 从指定范围内倒序查找适合断句的位置。
 * @param {string} text - 待查找文本。
 * @param {number} start - 最小查找下标。
 * @param {number} end - 最大查找下标。
 * @returns {number} 断句位置，不存在时返回 -1。
 */
function findSoftBoundary(text, start, end) {
  for (let index = Math.min(end, text.length - 1); index >= start; index -= 1) {
    if (SOFT_PUNCTUATION_REG.test(text[index])) {
      return index + 1
    }
  }
  return -1
}

/**
 * 管理一轮回答中的文本断句、TTS 预加载和音频顺序播放。
 */
export class TtsStreamPlayer {
  /**
   * 创建公共流式播报器，初始化文本断句、并发合成和顺序播放所需的状态。
   * @param {object} [options] - 播放器配置，未传字段使用 DEFAULT_OPTIONS 中的默认值。
   * @returns {TtsStreamPlayer} 流式播报器实例。
   * @example
   * const player = new TtsStreamPlayer({ voiceId: 'woman', concurrency: 2 })
   */
  constructor(options = {}) {
    // 合并默认值后的播放器配置。
    this.options = { ...DEFAULT_OPTIONS, ...options }
    // 是否允许创建播报会话并播放音频。
    this.enabled = true
    // 当前播报会话标识，用于隔离不同提问产生的异步任务。
    this.sessionId = 0
    // 当前会话是否仍允许接收新增回答文本。
    this.acceptingText = false
    // 上一次接收到的完整回答正文，用于计算本次新增文本。
    this.lastFullText = ''
    // 上一次已排除流程图、代码块的累计播报源文本。
    this.lastSpeechFullText = ''
    // 尚未达到断句条件、等待继续拼接的文本。
    this.textBuffer = ''
    // 当前会话的有序语音合成任务列表。
    this.segments = []
    // 下一段需要播放的任务下标。
    this.playCursor = 0
    // 当前正在执行的语音合成请求数量。
    this.activeSynthesisCount = 0
    // 当前正在播放的 Web Audio 音频源节点。
    this.currentSource = null
    // 复用的浏览器 Web Audio 上下文。
    this.audioContext = null
    // 回答暂停后触发提前断句的定时器标识。
    this.idleFlushTimer = null
    // 是否正在异步启动下一段音频，防止重复播放。
    this.playStarting = false
    // 当前会话播放完成后的 Promise。
    this.sessionFinishedPromise = Promise.resolve()
    // 用于结束当前会话完成 Promise 的方法。
    this.resolveSessionFinished = null
  }

  /**
   * 启用或关闭自动播报。
   * @param {boolean} enabled - 是否启用播报。
   * @returns {void}
   */
  setEnabled(enabled) {
    this.enabled = Boolean(enabled)
    if (!this.enabled) {
      this.stop()
    }
  }

  /**
   * 获取可复用的 AudioContext，不主动触发播放权限申请。
   * @returns {AudioContext|null} 浏览器音频上下文。
   */
  ensureAudioContext() {
    if (typeof window === 'undefined') return null

    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return null

    if (!this.audioContext || this.audioContext.state === 'closed') {
      this.audioContext = new AudioContextClass()
    }
    return this.audioContext
  }

  /**
   * 在用户交互期间创建并恢复 AudioContext，降低自动播放被浏览器拦截的概率。
   * @returns {Promise<boolean>} 是否成功取得音频播放能力。
   */
  async unlock() {
    if (!this.enabled) return false

    const audioContext = this.ensureAudioContext()
    if (!audioContext) return false

    if (audioContext.state === 'suspended') {
      await audioContext.resume()
    }
    return audioContext.state === 'running'
  }

  /**
   * 开始一轮新的回答播报，并使上一轮请求和音频立即失效。
   * @returns {number} 当前播报会话 ID。
   */
  startSession() {
    this.stop()
    this.sessionId += 1
    this.acceptingText = this.enabled
    mutablePlaybackState.sessionId = this.sessionId
    mutablePlaybackState.active = false
    this.sessionFinishedPromise = new Promise((resolve) => {
      this.resolveSessionFinished = resolve
    })
    void this.unlock().catch(() => false)
    return this.sessionId
  }

  /**
   * 播放一段完整文本，并等待音频自然结束或被中断。
   * @param {string} text - 需要播报的文本。
   * @returns {Promise<void>}
   */
  async speakAndWait(text = '') {
    // 清理后的有效播报文本。
    const speechText = cleanTextForSpeech(text)
    if (!this.enabled || !speechText) return

    // 本次完整文本播报的会话 ID。
    const sessionId = this.startSession()
    // 防止新会话覆盖实例上的完成 Promise。
    const finishedPromise = this.sessionFinishedPromise
    this.update(speechText, sessionId)
    this.finish(sessionId)
    await finishedPromise
  }

  /**
   * 接收当前回答的完整正文，只消费相对上一次新增的文本。
   * @param {string} fullText - 当前累计的完整回答正文。
   * @param {number} sessionId - 调用方持有的播报会话 ID。
   * @returns {void}
   */
  update(fullText = '', sessionId = this.sessionId) {
    if (!this.isCurrentSession(sessionId) || !this.acceptingText) return

    // 当前累计的原始回答。
    const nextFullText = String(fullText || '')
    // 当前排除完整或未闭合围栏代码块后的播报源文本。
    const nextSpeechFullText = getSpeechSourceText(nextFullText)
    if (!nextSpeechFullText.startsWith(this.lastSpeechFullText)) {
      // 协议标记可能让可展示正文回退，先撤销尚未提交的尾部文本。
      let commonLength = 0
      const compareLength = Math.min(this.lastSpeechFullText.length, nextSpeechFullText.length)
      while (commonLength < compareLength && this.lastSpeechFullText[commonLength] === nextSpeechFullText[commonLength]) {
        commonLength += 1
      }
      const rollbackLength = this.lastSpeechFullText.length - commonLength
      if (rollbackLength && rollbackLength <= this.textBuffer.length) {
        this.textBuffer = this.textBuffer.slice(0, -rollbackLength)
      }
      this.lastFullText = nextFullText
      this.lastSpeechFullText = nextSpeechFullText
      return
    }

    // 本次真正需要进入断句队列的新增自然语言文本。
    const appendText = nextSpeechFullText.slice(this.lastSpeechFullText.length)
    this.lastFullText = nextFullText
    this.lastSpeechFullText = nextSpeechFullText
    if (!appendText) return

    this.textBuffer += appendText
    this.extractCompletedSegments()
    this.scheduleIdleFlush(sessionId)
  }

  /**
   * 标记文本接收结束，并将没有结束标点的剩余内容加入合成队列。
   * @param {number} sessionId - 调用方持有的播报会话 ID。
   * @returns {void}
   */
  finish(sessionId = this.sessionId) {
    if (!this.isCurrentSession(sessionId)) return

    this.acceptingText = false
    this.clearIdleFlushTimer()
    this.extractCompletedSegments()
    this.enqueueBufferedText(true)
    void this.playNext(sessionId)
  }

  /**
   * 停止指定会话的请求与播放；不传会话 ID 时强制停止当前会话。
   * @param {number} [sessionId] - 需要停止的播报会话 ID。
   * @returns {void}
   */
  stop(sessionId) {
    if (sessionId !== undefined && sessionId !== this.sessionId) return

    this.clearIdleFlushTimer()
    this.acceptingText = false
    this.lastFullText = ''
    this.lastSpeechFullText = ''
    this.textBuffer = ''
    this.playCursor = 0
    this.activeSynthesisCount = 0
    this.playStarting = false
    mutablePlaybackState.active = false
    this.segments.forEach((segment) => segment.controller?.abort())
    this.segments = []

    if (this.currentSource) {
      this.currentSource.onended = null
      try {
        this.currentSource.stop()
      } catch {}
      this.currentSource.disconnect()
      this.currentSource = null
    }
    this.finishSession(this.sessionId)
  }

  /**
   * 结束指定会话的等待状态。
   * @param {number} sessionId - 已完成或被中断的播报会话 ID。
   * @returns {void}
   */
  finishSession(sessionId) {
    if (sessionId !== this.sessionId || !this.resolveSessionFinished) return

    mutablePlaybackState.active = false
    // 当前会话对应的完成方法。
    const resolve = this.resolveSessionFinished
    this.resolveSessionFinished = null
    resolve()
  }

  /**
   * 判断调用是否仍属于当前有效播报会话。
   * @param {number} sessionId - 待判断的播报会话 ID。
   * @returns {boolean} 是否为当前会话。
   */
  isCurrentSession(sessionId) {
    return this.enabled && sessionId === this.sessionId
  }

  /**
   * 从缓冲区持续提取完整句子或达到最大长度的片段。
   * @returns {void}
   */
  extractCompletedSegments() {
    const { minSegmentLength, preferredSegmentLength, maxSegmentLength } = this.options

    while (this.textBuffer) {
      let boundary = -1
      let searchOffset = 0

      while (searchOffset < this.textBuffer.length) {
        const relativeIndex = this.textBuffer.slice(searchOffset).search(STRONG_PUNCTUATION_REG)
        if (relativeIndex === -1) break

        boundary = searchOffset + relativeIndex + 1
        if (boundary >= minSegmentLength) break
        searchOffset = boundary
      }

      if (boundary >= minSegmentLength) {
        this.enqueueSegment(this.textBuffer.slice(0, boundary))
        this.textBuffer = this.textBuffer.slice(boundary)
        continue
      }

      if (this.textBuffer.length >= maxSegmentLength) {
        const softBoundary = findSoftBoundary(this.textBuffer, minSegmentLength, preferredSegmentLength)
        const maxBoundary = softBoundary > -1 ? softBoundary : maxSegmentLength
        this.enqueueSegment(this.textBuffer.slice(0, maxBoundary))
        this.textBuffer = this.textBuffer.slice(maxBoundary)
        continue
      }

      break
    }
  }

  /**
   * 在回答短暂停顿时尽早提交一段足够长的文本，降低首播延迟。
   * @param {number} sessionId - 当前播报会话 ID。
   * @returns {void}
   */
  flushIdleSegment(sessionId) {
    if (!this.isCurrentSession(sessionId) || !this.acceptingText) return

    const { minSegmentLength, preferredSegmentLength } = this.options
    if (this.textBuffer.length < minSegmentLength) return

    const boundary = findSoftBoundary(this.textBuffer, minSegmentLength, preferredSegmentLength)
    if (boundary === -1) return

    this.enqueueSegment(this.textBuffer.slice(0, boundary))
    this.textBuffer = this.textBuffer.slice(boundary)
  }

  /**
   * 安排回答停顿后的延迟断句任务。
   * @param {number} sessionId - 当前播报会话 ID。
   * @returns {void}
   */
  scheduleIdleFlush(sessionId) {
    this.clearIdleFlushTimer()
    this.idleFlushTimer = window.setTimeout(() => {
      this.idleFlushTimer = null
      this.flushIdleSegment(sessionId)
    }, this.options.idleFlushDelay)
  }

  /**
   * 清除等待中的延迟断句任务。
   * @returns {void}
   */
  clearIdleFlushTimer() {
    if (this.idleFlushTimer) {
      window.clearTimeout(this.idleFlushTimer)
      this.idleFlushTimer = null
    }
  }

  /**
   * 将缓冲区文本整体加入合成队列。
   * @param {boolean} force - 是否忽略最小长度限制。
   * @returns {void}
   */
  enqueueBufferedText(force = false) {
    if (!this.textBuffer) return
    if (!force && this.textBuffer.length < this.options.minSegmentLength) return

    this.enqueueSegment(this.textBuffer)
    this.textBuffer = ''
  }

  /**
   * 清理文本并创建一个有序语音合成任务。
   * @param {string} text - 待合成的回答片段。
   * @returns {void}
   */
  enqueueSegment(text) {
    const speakableText = cleanTextForSpeech(text)
    if (!speakableText) return

    this.segments.push({
      sequence: this.segments.length,
      text: speakableText,
      status: 'pending',
      audioBuffer: null,
      controller: null,
    })
    this.pumpSynthesis()
  }

  /**
   * 按并发上限启动等待中的语音合成任务。
   * @returns {void}
   */
  pumpSynthesis() {
    const maxPreloadSequence = this.playCursor + this.options.preloadCount - 1
    while (this.activeSynthesisCount < this.options.concurrency) {
      const segment = this.segments.find((item) => item.status === 'pending' && item.sequence <= maxPreloadSequence)
      if (!segment) return

      segment.status = 'synthesizing'
      this.activeSynthesisCount += 1
      void this.synthesizeSegment(segment, this.sessionId)
    }
  }

  /**
   * 合成单个文本片段并解码为浏览器可播放的 AudioBuffer。
   * @param {object} segment - 有序合成任务。
   * @param {number} sessionId - 任务所属播报会话 ID。
   * @returns {Promise<void>}
   */
  async synthesizeSegment(segment, sessionId) {
    const controller = new AbortController()
    segment.controller = controller

    try {
      const audioContext = this.ensureAudioContext()
      if (!this.isCurrentSession(sessionId)) return
      if (!audioContext) {
        segment.status = 'error'
        return
      }

      const audioData = await synthesizeTts({
        text: segment.text,
        voiceId: this.options.voiceId,
        maxTokens: this.options.maxTokens || 120,
        signal: controller.signal,
      })
      if (!this.isCurrentSession(sessionId)) return

      segment.audioBuffer = await audioContext.decodeAudioData(audioData.slice(0))
      segment.status = 'ready'
    } catch (error) {
      segment.status = 'error'
      const isCanceled =
        error?.name === 'AbortError' || error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED'
      if (!isCanceled && this.isCurrentSession(sessionId)) {
        console.warn('TTS 语音合成失败：', error)
      }
    } finally {
      // 合成成功后保留控制器，待音频自然播放结束时统一中断并释放。
      if (segment.status !== 'ready') {
        segment.controller = null
      }
      if (this.isCurrentSession(sessionId)) {
        this.activeSynthesisCount = Math.max(0, this.activeSynthesisCount - 1)
        this.pumpSynthesis()
        void this.playNext(sessionId)
      }
    }
  }

  /**
   * 按文本序号播放下一段已完成合成的音频。
   * @param {number} sessionId - 当前播报会话 ID。
   * @returns {Promise<void>}
   */
  async playNext(sessionId) {
    if (!this.isCurrentSession(sessionId) || this.currentSource || this.playStarting) return

    let segment = this.segments[this.playCursor]
    while (segment?.status === 'error') {
      this.playCursor += 1
      segment = this.segments[this.playCursor]
    }
    this.pumpSynthesis()
    if (!segment) {
      if (!this.acceptingText && !this.activeSynthesisCount) {
        this.finishSession(sessionId)
      }
      return
    }
    if (segment.status !== 'ready') return

    this.playStarting = true
    let source = null
    try {
      const unlocked = await this.unlock()
      if (!this.isCurrentSession(sessionId)) return
      if (!unlocked) {
        segment.status = 'error'
        return
      }

      source = this.audioContext.createBufferSource()
      source.buffer = segment.audioBuffer
      source.connect(this.audioContext.destination)
      segment.audioBuffer = null
      segment.status = 'playing'
      this.currentSource = source

      source.onended = () => {
        if (this.currentSource !== source) return
        source.disconnect()
        this.currentSource = null
        // 音频自然播放结束后，统一中断并释放该片段的请求控制器。
        segment.controller?.abort()
        segment.controller = null
        segment.status = 'played'
        this.playCursor += 1
        this.pumpSynthesis()
        void this.playNext(sessionId)
      }
      source.start()
      mutablePlaybackState.active = true
    } catch (error) {
      mutablePlaybackState.active = false
      if (this.currentSource === source) {
        this.currentSource = null
      }
      source?.disconnect()
      segment.controller?.abort()
      segment.controller = null
      segment.status = 'error'
      console.warn('TTS 音频播放失败：', error)
    } finally {
      this.playStarting = false
      if (segment.status === 'error' && this.isCurrentSession(sessionId)) {
        this.playCursor += 1
        this.pumpSynthesis()
        void this.playNext(sessionId)
      }
    }
  }
}

export const ttsStreamPlayer = new TtsStreamPlayer()
