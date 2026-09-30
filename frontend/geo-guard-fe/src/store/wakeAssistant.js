import { defineStore } from 'pinia'
import { ref } from 'vue'

const IDLE_VIDEO = '/video/rw-jz.webm'
const WAKE_RESPONSE_VIDEO = '/video/rw-hs.webm'
const ANSWER_AUDIO = '/video/answer-dz.wav'

let animationPromise = null

// 显式预加载资源和首帧，避免浏览器将未挂载节点的媒体请求延后。
const mediaAssets = [IDLE_VIDEO, WAKE_RESPONSE_VIDEO, ANSWER_AUDIO]
mediaAssets.forEach((src) => {
  const link = document.createElement('link')
  link.rel = 'preload'
  link.as = src.endsWith('.wav') ? 'audio' : 'video'
  link.href = src
  document.head.appendChild(link)
})

const preloadedVideos = [IDLE_VIDEO, WAKE_RESPONSE_VIDEO].map((src) => {
  const video = document.createElement('video')
  video.preload = 'auto'
  video.muted = true
  video.src = src
  video.load()
  return video
})

const answerAudioPreload = new Audio(ANSWER_AUDIO)
answerAudioPreload.preload = 'auto'
answerAudioPreload.load()

/**
 * 获取预加载媒体的时长。
 *
 * @param {HTMLMediaElement} media 已创建的音频或视频元素。
 * @returns {Promise<number>} 媒体时长；读取失败时返回 `0`。
 */
function getMediaDuration(media) {
  if (Number.isFinite(media.duration) && media.duration > 0) return Promise.resolve(media.duration)
  if (media.error) return Promise.resolve(0)

  return new Promise((resolve) => {
    const finish = () => resolve(Number.isFinite(media.duration) && media.duration > 0 ? media.duration : 0)
    media.addEventListener('loadedmetadata', finish, { once: true })
    media.addEventListener('error', finish, { once: true })
  })
}

/**
 * 计算视频与回答音频同时结束所需的播放倍率。
 *
 * @returns {Promise<number>} 视频播放倍率；无法读取时长时返回默认倍率 `1`。
 */
async function getWakeResponsePlaybackRate() {
  const [videoDuration, audioDuration] = await Promise.all([
    getMediaDuration(preloadedVideos[1]),
    getMediaDuration(answerAudioPreload),
  ])
  if (!videoDuration || !audioDuration) return 1
  return videoDuration / audioDuration
}

/**
 * 播放回答阶段的音频，结束或播放失败后重置临时音频实例。
 *
 * @returns {Promise<void>} 音频播放结束、出错或无法自动播放时完成。
 *
 * @example
 * await playAnswerAudio()
 */
function playAnswerAudio() {
  return new Promise((resolve) => {
    const audio = new Audio(ANSWER_AUDIO)
    const finish = () => {
      audio.removeEventListener('ended', finish)
      audio.removeEventListener('error', finish)
      audio.pause()
      audio.currentTime = 0
      resolve()
    }

    audio.preload = 'auto'
    audio.addEventListener('ended', finish, { once: true })
    audio.addEventListener('error', finish, { once: true })
    audio.play().catch(finish)
  })
}

export const useWakeAssistantStore = defineStore('wakeAssistant', () => {
  const enabled = ref(false)
  const animationPlaying = ref(false)
  const avatarVideoSrc = ref(IDLE_VIDEO)
  const avatarVideoLoop = ref(true)
  const avatarVideoPlaybackRate = ref(1)

  /**
   * 启用语音唤醒功能。
   *
   * @returns {void} 无返回值；会将 `enabled` 设置为 `true`。
   *
   * @example
   * useWakeAssistantStore().enable()
   */
  function enable() {
    enabled.value = true
  }

  /**
   * 关闭语音唤醒功能。
   *
   * @returns {void} 无返回值；会将 `enabled` 设置为 `false`，由监听组件停止音频采集。
   *
   * @example
   * useWakeAssistantStore().disable()
   */
  function disable() {
    enabled.value = false
  }

  /**
   * 仅播放唤醒后的回答音频，不切换虚拟形象视频。
   * 同一轮播放尚未结束时会复用正在执行的 Promise。
   *
   * @returns {Promise<void>} 回答音频播放结束、出错或无法自动播放后兑现。
   *
   * @example
   * await useWakeAssistantStore().playWakeAudio()
   */
  function playWakeAudio() {
    if (animationPromise) return animationPromise

    animationPlaying.value = true
    animationPromise = (async () => {
      try {
        await playAnswerAudio()
      } finally {
        animationPlaying.value = false
        animationPromise = null
      }
    })()

    return animationPromise
  }

  /**
   * 唤醒后立即播放回答视频与音频，并通过播放倍率使两者同时结束。
   * 同一轮动画尚未结束时会复用正在执行的 Promise，避免重复创建媒体元素。
   *
   * @returns {Promise<void>} 回答音频结束、视频恢复待机状态后兑现。
   *
   * @example
   * await useWakeAssistantStore().playWakeAnimations()
   */
  function playWakeAnimations() {
    if (animationPromise) return animationPromise

    animationPlaying.value = true
    animationPromise = (async () => {
      try {
        avatarVideoPlaybackRate.value = await getWakeResponsePlaybackRate()
        avatarVideoLoop.value = false
        avatarVideoSrc.value = WAKE_RESPONSE_VIDEO
        await playAnswerAudio()
      } finally {
        avatarVideoSrc.value = IDLE_VIDEO
        avatarVideoLoop.value = true
        avatarVideoPlaybackRate.value = 1
        animationPlaying.value = false
        animationPromise = null
      }
    })()

    return animationPromise
  }

  return {
    enabled,
    animationPlaying,
    avatarVideoSrc,
    avatarVideoLoop,
    avatarVideoPlaybackRate,
    enable,
    disable,
    playWakeAudio,
    playWakeAnimations,
  }
})
