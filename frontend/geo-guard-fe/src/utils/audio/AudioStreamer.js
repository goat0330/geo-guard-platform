import { ElMessage } from 'element-plus'
import hark from 'hark'
import { pinyin } from 'pinyin-pro'
import { ttsStreamPlayer } from '@/utils/audio/TtsStreamPlayer.js'

// `/chat-engine` 与布局右侧聊天框共用的语音采集、唤醒和识别控制器。
let activeAudioStreamer = null

function toFullPinyin(text) {
  return pinyin(text, { toneType: 'none', type: 'array' }).join('').toLowerCase()
}

export default class AudioStreamer {
  /**
   * 创建语音采集、静音过滤和 Socket 识别的控制器。
   *
   * @param {import('vue').Ref<string>} textRef - 识别文本的响应式引用，识别结果会写入 `textRef.value`。
   * @param {import('vue').Ref<boolean>} connectStatusRef - 输入状态的响应式引用；`true` 表示当前可将识别结果输入文本框。
   * @param {() => void} [autoStopCb] - 普通语音输入超时时的回调，通常用于自动发送已识别文本。
   * @param {() => void} [wakeDetectedCb] - 识别到唤醒词后的回调，用于开始唤醒动画。
   * @param {import('vue').Ref<boolean>} [recognizingStatusRef] - 等待离线最终识别结果的响应式状态。
   * @returns {AudioStreamer} 新创建的语音控制器实例。
   * @example
   * const audio = new AudioStreamer(audioText, isRecording, () => sendMsg())
   */
  constructor(textRef, connectStatusRef, autoStopCb, wakeDetectedCb, recognizingStatusRef = { value: false }) {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    // 确保 URL 格式正确
    this.url = `${protocol}://${window.location.host}/api-ws`
    // 当前语音识别 WebSocket
    this.socket = null
    // Web Audio 音频处理上下文
    this.audioContext = null
    // 采集麦克风 PCM 数据的处理节点
    this.processor = null
    // 麦克风音频流对应的输入节点
    this.input = null
    // 浏览器麦克风媒体流
    this.stream = null
    // 正在进行的麦克风权限申请，避免重复申请
    this.openPromise = null
    // hark 创建的说话/静音检测器
    this.speechEvents = null
    // 当前是否检测到用户正在说话
    this.isSpeaking = false
    // 最近一次检测到声音的时间戳
    this.lastVoiceAt = 0
    // 接收识别结果的响应式文本引用
    this.responseText = textRef
    // 对外同步语音输入开启状态的响应式引用
    this.wsStatus = connectStatusRef
    // 当前是否允许采集并发送音频
    this.isRecording = false
    // 静音超时结束输入后的回调
    this.autoStopCb = autoStopCb
    // 命中唤醒词后的回调
    this.wakeDetectedCb = wakeDetectedCb
    // 对外同步离线结果整理状态，用于衔接录音动画与自动发送。
    this.recognizingStatus = recognizingStatusRef

    // 语音输入静音超时计时器
    this.timeoutTimer = null
    // 连续静音超时时间
    this.IDLE_TIMEOUT = 2.5 * 1000
    // 等待服务端离线最终结果的计时器
    this.offlineResultTimer = null
    // 离线最终结果最长等待时间
    this.OFFLINE_RESULT_TIMEOUT = 4 * 1000

    // 已确认的历史识别文本
    this.historyText = ''
    // 用于暂存当前句子的增量识别结果
    this.onlineText = ''
    // 是否忽略当前 Socket 返回的所有消息
    this.ignoreMessage = false
    // 是否还有已发送但未返回最终结果的音频
    this.hasPendingRecognition = false
    // 是否正在等待最终结果后自动发送消息
    this.isAwaitingAutoSend = false
    this.recognizingStatus.value = false
    // 当前输入会话是否已触发自动发送，避免离线结果、超时和断连重复提交
    this.autoSendTriggered = false
    // 当前会话模式：idle、wake 或 input
    this.sessionMode = 'idle'
    // 会话创建序号，用于淘汰过期异步请求
    this.sessionRequestId = 0
    // 唤醒监听异常断开后的重连计时器
    this.reconnectTimer = null
    // 唤醒词监听总开关
    this.wakeListeningEnabled = false
    // 当前识别结果是否允许写入输入框
    this.isInputActive = false
    // 需要匹配的唤醒词
    this.wakeWords = ['你好地象', '你好重庆']
    this.wakeWord = this.wakeWords[0]
    // 唤醒词的无声调全拼
    this.wakeWordPinyins = this.wakeWords.map((w) => toFullPinyin(w))
    this.wakeWordPinyin = this.wakeWordPinyins[0]
    // 监听 Socket 累计收到的识别文本全拼
    this.wakeTextBuffer = ''
    // 是否正在等待唤醒动画播放结束
    this.isWakeAnimationPending = false
    // 最近一次成功唤醒的时间戳
    this.lastWakeAt = 0
    // 重复唤醒防抖时间
    this.WAKE_DEBOUNCE = 2 * 1000
    // 当前输入会话是否检测到有效语音
    this.hasVoiceInput = false

    // 服务端要求的参数
    // 发送给识别服务的目标采样率
    this.TARGET_SAMPLE_RATE = 16000
    // 发送给识别服务的声道数量
    this.CHANNELS = 1

    // 用于暂存原始音频数据，降低采样频率
    // 等待下采样和发送的原始音频缓存
    this.cacheBuffer = []
    // 说话检测触发前保留的短时音频缓存
    this.preRollBuffer = []
    // 前置音频缓存时长，避免吞掉第一个字
    this.PRE_ROLL_DURATION = 400
    // 单次发送前需要累计的采样点数量
    this.SEND_INTERVAL = 4096 * 4
  }

  /**
   * 请求麦克风权限并保存音频流；已有音频流时不会重复申请。
   *
   * @returns {Promise<void>} 麦克风就绪或权限申请失败后完成；失败信息会通过消息提示展示。
   * @example
   * await audio.open()
   */
  async open() {
    if (this.stream) return
    if (!this.openPromise) {
      this.openPromise = navigator.mediaDevices
        .getUserMedia({ audio: true })
        .then((stream) => {
          this.stream = stream
        })
        .catch((err) => {
          ElMessage.error('无法访问麦克风，请检查权限')
          console.error(err)
        })
        .finally(() => {
          this.openPromise = null
        })
    }
    await this.openPromise
  }

  /**
   * 建立全新的识别 Socket 并开始采集音频；当前 Socket 会先被关闭。
   *
   * @param {object} [options={}] - 启动选项。
   * @param {boolean} [options.requiresWakeWord=false] - 是否必须先识别到唤醒词才允许写入输入框。
   * @param {boolean} [options.persistent=false] - 是否保持长连接；长连接不使用普通语音的超时断开机制。
   * @returns {Promise<void>} Socket 创建请求及麦克风准备完成后返回；实际连接成功由 WebSocket 回调处理。
   * @example
   * await audio.start() // 普通手动语音输入
   * await audio.start({ requiresWakeWord: true, persistent: true }) // 常驻唤醒监听
   */
  async start({ requiresWakeWord = false, persistent = false } = {}) {
    if (persistent) this.wakeListeningEnabled = true
    await this.startSession(requiresWakeWord ? 'wake' : 'input')
  }

  /**
   * 创建一个全新的识别会话。唤醒监听与语音输入绝不复用 Socket，旧 Socket 的回调也不会处理。
   *
   * @param {'wake' | 'input'} mode - 新会话用途：唤醒词监听或语音输入。
   * @returns {Promise<void>} 麦克风准备及 Socket 创建流程完成。
   */
  async startSession(mode) {
    const requestId = ++this.sessionRequestId
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (activeAudioStreamer && activeAudioStreamer !== this) {
      activeAudioStreamer.stop({ immediatelyCloseSocket: true })
    }
    activeAudioStreamer = this

    // 每一种模式都使用独立 Socket，先让旧会话彻底失效。
    this.closeCurrentSocket()

    if (!this.stream) {
      await this.open()
      if (!this.stream) {
        if (activeAudioStreamer === this) activeAudioStreamer = null
        return
      }
    }

    // 等待麦克风授权期间，可能已有其他输入框接管了语音连接。
    if (requestId !== this.sessionRequestId || activeAudioStreamer !== this) {
      if (activeAudioStreamer !== this) this.releaseAudioCapture()
      return
    }

    this.ignoreMessage = false
    this.sessionMode = mode
    this.isInputActive = mode === 'input'
    this.wsStatus.value = false
    this.wakeTextBuffer = ''
    this.historyText = this.responseText.value || ''
    this.onlineText = ''
    this.hasVoiceInput = false
    this.hasPendingRecognition = false
    this.isAwaitingAutoSend = false
    this.recognizingStatus.value = false
    this.autoSendTriggered = false
    this.lastVoiceAt = 0
    this.cacheBuffer = []
    this.preRollBuffer = []

    console.info('[语音识别] 正在创建 Socket', { mode, url: this.url })
    const socket = new WebSocket(this.url)
    this.socket = socket
    socket.binaryType = 'arraybuffer'
    // Socket 建连期间也开始保留前置音频，避免用户过早开口时丢失首字。
    this.isRecording = true
    this.initAudioProcessor()

    socket.onopen = () => {
      if (this.socket !== socket) return
      console.info('[语音识别] Socket 已开启', { mode, url: this.url })
      this.wsStatus.value = mode === 'input'
      // 发送握手配置（必须与服务端 Python 代码中的 args 匹配）
      const config = {
        mode: '2pass',
        chunk_size: [5, 10, 5],
        chunk_interval: 10,
        encoder_chunk_look_back: 4,
        decoder_chunk_look_back: 0,
        wav_name: 'microphone',
        is_speaking: true,
        hotwords: '',
        itn: true,
      }
      socket.send(JSON.stringify(config))
      // 建连期间若已经检测到语音，握手配置发送后立即补发缓存。
      this.flushCachedAudio(this.audioContext?.sampleRate || this.TARGET_SAMPLE_RATE)

      if (mode === 'input') this.resetTimeoutTimer()
    }

    socket.onmessage = (event) => {
      if (this.socket !== socket || this.ignoreMessage) return
      try {
        const data = JSON.parse(event.data)
        console.log(data)

        // 监听 Socket 的所有识别内容只参与唤醒匹配，永远不写入输入框。
        if (mode === 'wake') {
          this.tryActivateByWakeWord(data.text)
          return
        }

        if (!this.isInputActive && !this.isAwaitingAutoSend) return
        const shouldAutoSend = data.mode === '2pass-offline' && this.isAwaitingAutoSend
        if (data.mode === '2pass-offline') this.hasPendingRecognition = false
        if (data.mode === '2pass-online' && data.text) {
          // online 仅作为当前句子的临时预览，不能沉淀到历史文本。
          this.onlineText = data.text
          this.responseText.value = this.historyText + this.onlineText
        }
        if (data.mode === '2pass-offline' && data.text) {
          // offline 是当前句子的最终结果，用它替换 online 预览后再沉淀。
          this.historyText += data.text
          this.onlineText = ''
          this.responseText.value = this.historyText
        }
        if (shouldAutoSend) {
          this.completeInputAndAutoSend()
        }
      } catch {
        console.warn('收到非 JSON 消息:', event.data)
      }
    }

    socket.onerror = (err) => {
      if (this.socket !== socket) return
      console.error('WS Error:', err)
      socket.close()
    }

    socket.onclose = (event) => {
      console.info('[语音识别] Socket 已关闭', {
        mode,
        code: event.code,
        reason: event.reason || '无',
        wasClean: event.wasClean,
      })
      if (this.socket !== socket) return
      const shouldAutoSend = mode === 'input' && String(this.responseText.value || '').trim().length > 0
      if (this.offlineResultTimer) {
        clearTimeout(this.offlineResultTimer)
        this.offlineResultTimer = null
      }
      if (shouldAutoSend) {
        this.completeInputAndAutoSend()
        return
      }

      this.socket = null
      this.sessionMode = 'idle'
      this.isRecording = false
      this.isInputActive = false
      this.isAwaitingAutoSend = false
      this.recognizingStatus.value = false
      this.hasPendingRecognition = false
      this.wsStatus.value = false
      this.cacheBuffer = []
      this.preRollBuffer = []
      this.resumeWakeListeningIfNeeded({ delayed: true })
    }
  }

  /**
   * 启动常驻唤醒监听。监听结果不会写入输入框，识别到“你好XX”后切换为新的输入会话。
   *
   * @returns {Promise<void>} 启动流程完成。
   * @example
   * await audio.startWakeListening()
   */
  async startWakeListening() {
    this.wakeListeningEnabled = true
    if (this.sessionMode === 'input' || this.isWakeAnimationPending) return
    await this.startSession('wake')
  }

  /**
   * 启动手动语音输入，跳过唤醒词校验。
   * 会先关闭已有识别 Socket，再按普通模式新建独立连接。
   *
   * @returns {Promise<void>} 输入状态切换或连接启动完成。
   * @example
   * await audio.startManualInput()
   */
  async startManualInput() {
    this.isWakeAnimationPending = false
    await this.startSession('input')
  }

  /**
   * 立即断开当前识别 Socket，但保留麦克风和 Web Audio 资源。
   * 将 `this.socket` 先置空，使旧连接的异步回调无法影响后续新会话。
   *
   * @returns {void}
   */
  closeCurrentSocket() {
    const socket = this.socket
    if (socket) {
      console.info('[语音识别] 正在关闭 Socket', {
        mode: this.sessionMode,
        readyState: socket.readyState,
      })
    }
    this.socket = null
    this.sessionMode = 'idle'
    this.isRecording = false
    this.isInputActive = false
    this.wsStatus.value = false
    this.isAwaitingAutoSend = false
    this.recognizingStatus.value = false
    this.hasPendingRecognition = false
    this.cacheBuffer = []
    this.preRollBuffer = []
    if (this.timeoutTimer) {
      clearTimeout(this.timeoutTimer)
      this.timeoutTimer = null
    }
    if (this.offlineResultTimer) {
      clearTimeout(this.offlineResultTimer)
      this.offlineResultTimer = null
    }
    if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) socket.close()
  }

  /**
   * 关闭当前会话并创建新的语音输入 Socket。
   *
   * @returns {Promise<void>} 新输入会话创建流程完成。
   * @example
   * audio.activateInput() // 用户主动点击麦克风
   */
  async activateInput() {
    await this.startManualInput()
  }

  /**
   * 关闭当前输入状态。
   * 常驻唤醒模式下回到等待唤醒词；普通模式释放麦克风。
   *
   * @returns {void}
   * @example
   * audio.deactivateInput()
   */
  deactivateInput() {
    if (this.timeoutTimer) {
      clearTimeout(this.timeoutTimer)
      this.timeoutTimer = null
    }
    this.isInputActive = false
    this.wsStatus.value = false
    this.historyText = this.responseText.value || ''
    this.onlineText = ''
    this.wakeTextBuffer = ''
    this.isWakeAnimationPending = false
    this.hasPendingRecognition = false
    this.isAwaitingAutoSend = false
    this.cacheBuffer = []
    this.closeCurrentSocket()
    this.resumeWakeListeningIfNeeded()
  }

  /**
   * 当前输入会话结束后，根据唤醒总开关重新创建独立的监听 Socket。
   *
   * @param {object} [options={}] - 恢复选项。
   * @param {boolean} [options.delayed=false] - 异常断线时是否短暂延迟，避免立即重连形成忙循环。
   * @returns {void}
   */
  resumeWakeListeningIfNeeded({ delayed = false } = {}) {
    if (!this.wakeListeningEnabled || this.isWakeAnimationPending) {
      if (!this.wakeListeningEnabled) {
        this.releaseAudioCapture()
        if (activeAudioStreamer === this) activeAudioStreamer = null
      }
      return
    }

    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    const restart = () => {
      this.reconnectTimer = null
      if (this.wakeListeningEnabled && !this.isWakeAnimationPending && this.sessionMode === 'idle') {
        void this.startSession('wake')
      }
    }

    if (delayed) this.reconnectTimer = setTimeout(restart, 300)
    else restart()
  }

  /**
   * 检查识别文本是否包含唤醒词，并在命中时开启输入状态。
   * 匹配时会忽略空白和标点，将文本转为无声调全拼后，对唤醒前服务端返回的全部识别文本进行比对。
   *
   * @param {string} text - 服务端本次返回的识别文本。
   * @returns {boolean} 是否命中唤醒词。
   * @example
   * const matched = audio.tryActivateByWakeWord('nihaodixiang，查询天气') // true
   */
  tryActivateByWakeWord(text) {
    if (!text) return false
    if (this.isWakeAnimationPending) return false
    const now = Date.now()
    if (now - this.lastWakeAt < this.WAKE_DEBOUNCE) return false
    // 识别服务可能返回中文或全拼，并会在唤醒词中间插入标点；匹配前统一转全拼。
    const normalizedText = text.replace(/[\p{P}\p{Z}\s]/gu, '')
    this.wakeTextBuffer += toFullPinyin(normalizedText)
    const wakeText = this.wakeTextBuffer
    const matchedIndex = this.wakeWordPinyins.findIndex((p) => wakeText.lastIndexOf(p) >= 0)
    if (matchedIndex < 0) return false
    const hitWakeWord = this.wakeWords[matchedIndex]

    // 唤醒词命中后立即关闭监听 Socket；动画结束后会新建语音输入 Socket。
    console.info('[语音识别] 唤醒成功', { wakeWord: hitWakeWord, text })
    this.lastWakeAt = now
    this.isWakeAnimationPending = true
    this.cacheBuffer = []
    this.wakeTextBuffer = ''
    this.closeCurrentSocket()
    // 唤醒词可能被正在播放的回答音频触发，命中后立即停止全局 TTS，避免干扰后续语音输入。
    ttsStreamPlayer.stop()
    this.wakeDetectedCb?.()
    return true
  }

  /**
   * 唤醒动画播放结束后，丢弃动画期间的音频，并建立新的 Socket 开始语音输入。
   *
   * @returns {void}
   */
  async resumeAfterWakeAnimation() {
    if (!this.isWakeAnimationPending) return
    this.cacheBuffer = []
    this.isWakeAnimationPending = false
    await this.startManualInput()
  }

  /**
   * 清空已识别文本及其拼接缓存。
   * 发送消息后调用，避免下一次唤醒把上一轮已发送的内容作为识别前缀。
   *
   * @returns {void} 无返回值；会同步清空绑定的输入文本和内部缓存。
   * @example
   * audio.clearRecognizedText()
   */
  clearRecognizedText() {
    this.responseText.value = ''
    this.historyText = ''
    this.onlineText = ''
  }

  /**
   * 重置普通语音输入的静音超时计时器。
   * 超时后会结束音频流，待服务端返回 `2pass-offline` 最终结果后执行 `autoStopCb`。
   *
   * @returns {void}
   * @example
   * audio.resetTimeoutTimer()
   */
  resetTimeoutTimer() {
    // 先清除之前的计时器
    if (this.timeoutTimer) {
      clearTimeout(this.timeoutTimer)
    }

    // 重新开始计时
    this.timeoutTimer = setTimeout(() => {
      // 语音状态事件可能在连续讲话时短暂抖动；关闭前以最近采集到的语音帧再次确认。
      const silenceDuration = Date.now() - this.lastVoiceAt
      if (this.isSpeaking || (this.lastVoiceAt && silenceDuration < this.IDLE_TIMEOUT)) {
        this.resetTimeoutTimer()
        return
      }
      console.log(`${this.IDLE_TIMEOUT / 1000} 秒未检测到声音输入，开始整理识别结果...`)
      this.finishInputAndWaitForOffline()
    }, this.IDLE_TIMEOUT)
  }

  /**
   * 结束自动语音输入并等待服务端返回本轮离线最终识别结果。
   *
   * @returns {void}
   */
  finishInputAndWaitForOffline() {
    if (!this.isInputActive) return

    const socket = this.socket
    if (socket?.readyState === WebSocket.OPEN) {
      // 先提交尚未达到发送阈值的音频，再通知服务端结束本轮语音。
      this.flushCachedAudio(this.audioContext?.sampleRate || this.TARGET_SAMPLE_RATE)
      socket.send(JSON.stringify({ is_speaking: false }))
    }

    // 本轮是否实际采集到有效语音，用于区分“无需发送”和“结果已提前返回”。
    const hasVoiceInput = this.hasVoiceInput
    // offline 可能早于静音超时返回；仅在仍有音频待确认时继续等待。
    this.isAwaitingAutoSend = hasVoiceInput && this.hasPendingRecognition && socket?.readyState === WebSocket.OPEN
    this.isRecording = false
    this.isInputActive = false
    this.wsStatus.value = false
    this.recognizingStatus.value = this.isAwaitingAutoSend
    this.wakeTextBuffer = ''
    this.isWakeAnimationPending = false

    // 没有有效语音时无需发送，直接结束输入会话并恢复监听。
    if (!hasVoiceInput) {
      this.closeCurrentSocket()
      this.resumeWakeListeningIfNeeded()
      return
    }

    // 最终结果已经提前返回，无需再启动 4 秒兜底等待。
    if (!this.isAwaitingAutoSend) {
      this.completeInputAndAutoSend()
      return
    }

    // 服务端若未返回离线结果，也必须结束本次输入，避免永久占用输入 Socket 而无法恢复唤醒监听。
    this.offlineResultTimer = setTimeout(() => {
      if (this.sessionMode !== 'input' || !this.isAwaitingAutoSend) return
      this.completeInputAndAutoSend()
    }, this.OFFLINE_RESULT_TIMEOUT)
  }

  /**
   * 完成当前输入会话并自动提交已识别文本。
   * 无论由离线结果、等待超时还是 Socket 异常关闭触发，同一会话只会提交一次。
   *
   * @returns {void}
   */
  completeInputAndAutoSend() {
    if (this.autoSendTriggered) return

    // 关闭 Socket 前确认在线识别文本，避免等待 offline 时丢弃可提交内容。
    const recognizedText = String(this.responseText.value || '').trim()
    this.isAwaitingAutoSend = false

    if (!recognizedText) {
      this.closeCurrentSocket()
      this.resumeWakeListeningIfNeeded()
      return
    }

    this.autoSendTriggered = true
    this.closeCurrentSocket()
    // 保持“识别中”状态到发送动作真正执行，避免中间闪回麦克风按钮。
    this.recognizingStatus.value = true
    setTimeout(() => {
      this.recognizingStatus.value = false
      this.autoStopCb?.()
    }, 0)
  }

  /**
   * 初始化 Web Audio 节点、hark 静音检测和音频处理回调。
   * 只有 hark 判定为非静音时才缓存并经 Socket 发送音频；该方法由 Socket 连接成功后内部调用。
   *
   * @returns {void}
   * @example
   * // 通常无需手动调用：audio.start() 连接成功后会自动执行。
   */
  initAudioProcessor() {
    if (!this.stream || this.processor) return
    this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const sourceSampleRate = this.audioContext.sampleRate
    this.input = this.audioContext.createMediaStreamSource(this.stream)

    // 使用 hark 的音量检测事件过滤静音帧，避免无效音频占用 Socket 和后端识别资源。
    this.speechEvents = hark(this.stream, {
      threshold: -68,
      interval: 100,
      play: false,
    })
    this.speechEvents.on('speaking', () => {
      this.isSpeaking = true
      this.lastVoiceAt = Date.now()
      this.hasVoiceInput = true
      // 将检测到说话之前保留的短时音频拼到正文前，避免首个音节被 VAD 截掉。
      if (!this.isWakeAnimationPending && this.sessionMode !== 'idle' && this.preRollBuffer.length) {
        this.cacheBuffer.push(...this.preRollBuffer)
        this.preRollBuffer = []
      }
      if (this.isInputActive && this.timeoutTimer) {
        clearTimeout(this.timeoutTimer)
        this.timeoutTimer = null
      }
    })
    this.speechEvents.on('stopped_speaking', () => {
      this.isSpeaking = false
      this.flushCachedAudio(sourceSampleRate)
      if (this.isInputActive) this.resetTimeoutTimer()
    })

    // 保持 4096，这是浏览器处理的稳定尺寸
    this.processor = this.audioContext.createScriptProcessor(4096, this.CHANNELS, this.CHANNELS)

    this.processor.onaudioprocess = (e) => {
      if (!this.isRecording || this.sessionMode === 'idle') return

      const inputData = e.inputBuffer.getChannelData(0)

      // 唤醒动画和反馈音频播放期间不采集、不上传用户语音。
      if (this.isWakeAnimationPending) return

      // VAD 尚未触发时滚动保留最近一小段音频，触发后作为语音前缀一并发送。
      if (!this.isSpeaking) {
        this.appendPreRollAudio(inputData, sourceSampleRate)
        return
      }

      this.lastVoiceAt = Date.now()
      // 动画结束时用户可能仍在持续说话，此时 hark 不会再次触发 speaking 事件；以实际采集到的语音帧确认本轮有效输入。
      if (this.isInputActive) this.hasVoiceInput = true

      // 暂存数据，达到阈值后再处理。
      this.cacheBuffer.push(...inputData)

      // 只有当暂存的数据达到指定长度时，才执行下采样和发送
      if (this.cacheBuffer.length >= this.SEND_INTERVAL && this.socket?.readyState === WebSocket.OPEN) {
        this.flushCachedAudio(sourceSampleRate)
      }
    }

    this.input.connect(this.processor)
    this.processor.connect(this.audioContext.destination)
  }

  /**
   * 滚动保存说话检测触发前的短时 PCM，只保留配置时长内的最新采样。
   *
   * @param {Float32Array} inputData - 当前音频处理帧。
   * @param {number} sourceSampleRate - 麦克风原始采样率。
   * @returns {void}
   */
  appendPreRollAudio(inputData, sourceSampleRate) {
    this.preRollBuffer.push(...inputData)
    const maxSamples = Math.round((sourceSampleRate * this.PRE_ROLL_DURATION) / 1000)
    if (this.preRollBuffer.length > maxSamples) {
      this.preRollBuffer.splice(0, this.preRollBuffer.length - maxSamples)
    }
  }

  /**
   * 将当前缓存的非静音 PCM 数据下采样、转换为 Int16，并发送到已打开的 Socket。
   * 用于缓存达到阈值、检测到说话结束和主动停止录音时。
   *
   * @param {number} sourceSampleRate - 浏览器 AudioContext 的原始采样率，例如 48000。
   * @returns {void}
   * @example
   * audio.flushCachedAudio(audio.audioContext.sampleRate)
   */
  flushCachedAudio(sourceSampleRate) {
    if (this.isWakeAnimationPending || !this.cacheBuffer.length || this.socket?.readyState !== WebSocket.OPEN) return

    const audioToProcess = new Float32Array(this.cacheBuffer)
    this.cacheBuffer = []
    const resampledData = this.downsample(audioToProcess, sourceSampleRate, this.TARGET_SAMPLE_RATE)
    const int16Array = this.float32ToInt16(resampledData)
    this.socket.send(int16Array.buffer)
    if (this.isInputActive) this.hasPendingRecognition = true
  }

  /**
   * 使用线性聚合将 PCM Float32 数据降采样。
   *
   * @param {Float32Array} buffer - 原始单声道 PCM 浮点采样数据，范围通常为 -1 到 1。
   * @param {number} fromRate - 原始采样率。
   * @param {number} toRate - 目标采样率。
   * @returns {Float32Array} 目标采样率的 PCM 数据；采样率一致时直接返回原数组。
   * @example
   * const pcm16k = audio.downsample(pcm48k, 48000, 16000)
   */
  downsample(buffer, fromRate, toRate) {
    if (fromRate === toRate) return buffer
    const sampleRateRatio = fromRate / toRate
    const newLength = Math.round(buffer.length / sampleRateRatio)
    const result = new Float32Array(newLength)
    let offsetResult = 0
    let offsetBuffer = 0
    while (offsetResult < result.length) {
      const nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio)
      let accum = 0,
        count = 0
      for (let i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
        accum += buffer[i]
        count++
      }
      result[offsetResult] = accum / count
      offsetResult++
      offsetBuffer = nextOffsetBuffer
    }
    return result
  }

  /**
   * 将范围为 -1 到 1 的 Float32 PCM 数据转换为小端 Int16 PCM 数据。
   *
   * @param {Float32Array} float32Array - 浮点格式的 PCM 采样数据。
   * @returns {Int16Array} 可直接通过 WebSocket 发送的 16 位 PCM 数据。
   * @example
   * const pcmInt16 = audio.float32ToInt16(pcm16k)
   */
  float32ToInt16(float32Array) {
    const buffer = new ArrayBuffer(float32Array.length * 2)
    const view = new DataView(buffer)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true) // 小端字节序
    }
    return new Int16Array(buffer)
  }

  /**
   * 释放麦克风和 Web Audio 资源，但保留当前 Socket 供同一输入框下次复用。
   *
   * @returns {void} 无返回值。
   */
  releaseAudioCapture() {
    this.isRecording = false

    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop())
      this.stream = null
    }

    if (this.processor) {
      this.processor.disconnect()
      this.processor.onaudioprocess = null
      this.processor = null
    }

    if (this.speechEvents) {
      this.speechEvents.stop()
      this.speechEvents = null
    }
    this.isSpeaking = false

    if (this.input) {
      this.input.disconnect()
      this.input = null
    }

    if (this.audioContext && this.audioContext.state !== 'closed') this.audioContext.close()
    this.audioContext = null
    this.cacheBuffer = []
    this.preRollBuffer = []
  }

  /**
   * 停止识别并释放 Socket、麦克风轨道、静音检测器及 Web Audio 资源。
   * 常驻唤醒监听若只需退出当前输入状态，应使用 `deactivateInput()`，而不是本方法。
   *
   * @param {object} [options={}] - 停止选项。
   * @param {boolean} [options.clearText=false] - 是否同时清空输入文本及内部识别缓存。
   * @param {boolean} [options.immediatelyCloseSocket=false] - 是否立即关闭 Socket；切换输入入口时使用，避免并存连接。
   * @param {boolean} [options.discardPendingAudio=false] - 是否丢弃尚未发送的音频，不再作为当前识别会话的结束数据上传。
   * @returns {void}
   * @example
   * audio.stop()
   * audio.stop({ clearText: true })
   */
  stop(options = {}) {
    const { clearText = false, immediatelyCloseSocket = false, discardPendingAudio = false } = options
    this.sessionRequestId++
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.offlineResultTimer) {
      clearTimeout(this.offlineResultTimer)
      this.offlineResultTimer = null
    }
    if (clearText) {
      this.responseText.value = ''
      this.historyText = ''
      this.onlineText = ''
      this.ignoreMessage = true
    }

    // 清除计时器
    if (this.timeoutTimer) {
      clearTimeout(this.timeoutTimer)
      this.timeoutTimer = null
    }

    if (this.socket) {
      console.info('[语音识别] 正在关闭 Socket', {
        mode: this.sessionMode,
        readyState: this.socket.readyState,
      })
    }

    this.isRecording = false
    this.sessionMode = 'idle'
    this.wsStatus.value = false
    this.wakeListeningEnabled = false
    this.isInputActive = false
    this.wakeTextBuffer = ''
    this.isWakeAnimationPending = false
    this.hasPendingRecognition = false
    this.isAwaitingAutoSend = false
    this.recognizingStatus.value = false
    this.autoSendTriggered = false

    // 1. 发送结束信号并关闭 Socket
    if (this.socket) {
      if (this.socket.readyState === WebSocket.OPEN) {
        // 停止发生在一句话结束前时，也要先发送已检测到的最后一段非静音音频。
        if (!discardPendingAudio) this.flushCachedAudio(this.audioContext?.sampleRate || this.TARGET_SAMPLE_RATE)
        // 告知服务端音频流已结束，这很重要，能让服务端把最后一段话识别出来
        this.socket.send(JSON.stringify({ is_speaking: false }))
      }
      // 主动结束录音时延迟关闭以送达结束信号；切换入口时立即关闭，确保只有一个 Socket。
      const tempSocket = this.socket
      if (immediatelyCloseSocket) tempSocket.close()
      else setTimeout(() => tempSocket.close(), 500)
      this.socket = null
    }

    this.releaseAudioCapture()
    this.onlineText = ''
    if (activeAudioStreamer === this) activeAudioStreamer = null
  }
}
