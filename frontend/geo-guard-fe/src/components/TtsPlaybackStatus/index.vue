<template>
  <Transition name="tts-status">
    <div v-if="visible" class="tts-playback-status" role="status" aria-live="polite">
      <span class="playing-info">
        <span class="voice-wave" aria-hidden="true">
          <i v-for="index in 4" :key="index" :style="{ animationDelay: `${(index - 1) * 0.12}s` }"></i>
        </span>
        <span>播放中</span>
      </span>
      <span class="divider" aria-hidden="true"></span>
      <button class="stop-button" type="button" aria-label="停止播放" @click="stopPlayback">
        <span class="stop-icon" aria-hidden="true"></span>
        停止播放
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { ttsPlaybackState, ttsStreamPlayer } from '@/utils/audio/TtsStreamPlayer.js'

const props = defineProps({
  sessionId: {
    type: Number,
    default: 0,
  },
})

// 只有当前消息所属的 TTS 会话显示播放状态。
const visible = computed(
  () => props.sessionId > 0 && ttsPlaybackState.sessionId === props.sessionId && ttsPlaybackState.active,
)

/**
 * 停止当前消息对应的语音合成和播放。
 * @returns {void}
 */
function stopPlayback() {
  ttsStreamPlayer.stop(props.sessionId)
}
</script>

<style scoped lang="less">
.tts-playback-status {
  display: flex;
  align-items: center;
  width: fit-content;
  min-height: 32px;
  margin-bottom: 8px;
  padding: 0 8px 0 12px;
  border: 1px solid rgba(25, 87, 228, 0.16);
  border-radius: 16px;
  background: #f2f6ff;
  box-shadow: 0 4px 12px rgba(25, 87, 228, 0.08);
  color: #1957e4;
  box-sizing: border-box;
  font-size: 12px;
  line-height: 20px;
}

.playing-info,
.stop-button {
  display: inline-flex;
  align-items: center;
}

.voice-wave {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 18px;
  height: 16px;
  margin-right: 6px;

  i {
    width: 2px;
    height: 12px;
    border-radius: 2px;
    background: currentColor;
    animation: voice-wave 0.8s ease-in-out infinite alternate;
  }
}

.divider {
  width: 1px;
  height: 12px;
  margin-left: 10px;
  background: rgba(25, 87, 228, 0.2);
}

.stop-button {
  gap: 4px;
  margin-left: 4px;
  padding: 2px 6px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #506073;
  font: inherit;
  line-height: 20px;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;

  &:hover {
    background: rgba(25, 87, 228, 0.1);
    color: #1957e4;
  }

  &:focus-visible {
    outline: 2px solid rgba(25, 87, 228, 0.3);
    outline-offset: 2px;
  }
}

.stop-icon {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: currentColor;
}

.tts-status-enter-active,
.tts-status-leave-active {
  overflow: hidden;
  transition:
    opacity 0.2s ease,
    transform 0.2s ease,
    max-height 0.2s ease,
    margin-bottom 0.2s ease;
}

.tts-status-enter-from,
.tts-status-leave-to {
  max-height: 0;
  margin-bottom: 0;
  opacity: 0;
  transform: translateY(-6px);
}

.tts-status-enter-to,
.tts-status-leave-from {
  max-height: 32px;
  opacity: 1;
  transform: translateY(0);
}

@keyframes voice-wave {
  0% {
    transform: scaleY(0.35);
    opacity: 0.45;
  }

  100% {
    transform: scaleY(1);
    opacity: 1;
  }
}
</style>
