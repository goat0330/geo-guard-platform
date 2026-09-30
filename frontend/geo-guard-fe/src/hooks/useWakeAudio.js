import { onActivated, onBeforeUnmount, onDeactivated, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import AudioStreamer from '@/utils/audio/AudioStreamer.js'
import { useWakeAssistantStore } from '@/store/wakeAssistant.js'

export function useWakeAudio({ textRef, statusRef, recognizingStatusRef, activeRef, autoStopCb, wakeDetectedCb }) {
  const wakeAssistantStore = useWakeAssistantStore()
  const { enabled, animationPlaying } = storeToRefs(wakeAssistantStore)
  const audio = new AudioStreamer(
    textRef,
    statusRef,
    autoStopCb,
    wakeDetectedCb || wakeAssistantStore.playWakeAnimations,
    recognizingStatusRef,
  )
  // KeepAlive 缓存的页面离开后仍会响应路由变化；仅在页面实际激活时才允许占用麦克风。
  const componentActive = ref(true)

  watch(
    [enabled, activeRef, componentActive],
    async ([wakeEnabled, active, isComponentActive]) => {
      if (wakeEnabled && active && isComponentActive) {
        await audio.startWakeListening()
        return
      }

      audio.stop()
    },
    { immediate: true },
  )

  onActivated(() => {
    componentActive.value = true
  })

  onDeactivated(() => {
    componentActive.value = false
  })

  watch(animationPlaying, (playing, wasPlaying) => {
    if (wasPlaying && !playing) audio.resumeAfterWakeAnimation()
  })

  onBeforeUnmount(() => audio.stop())

  return {
    audio,
    wakeListeningEnabled: enabled,
  }
}
