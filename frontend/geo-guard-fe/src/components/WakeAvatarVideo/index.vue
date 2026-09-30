<template>
  <div class="wake-avatar-video" :class="{ mirrored: mirror }">
    <video :ref="(element) => setVideoRef(element, 0)" :class="{ active: activeVideoIndex === 0 }" autoplay muted playsinline></video>
    <video
      :ref="(element) => setVideoRef(element, 1)"
      :class="{ active: activeVideoIndex === 1 }"
      autoplay
      muted
      playsinline
    ></video>
    <div
      v-if="showSwitch"
      class="wake-switch-control"
      :class="{ 'has-label': switchLabel, 'is-enabled': enabled }"
    >
      <span v-if="switchLabel" class="wake-switch-label">
        <i class="iconfont icon-mic"></i>
        {{ switchLabel }}
      </span>
      <el-switch
        class="wake-switch"
        :model-value="enabled"
        :width="34"
        @change="changeWakeStatus"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useWakeAssistantStore } from '@/store/wakeAssistant.js'

const videoRefs = ref([])
const activeVideoIndex = ref(0)
const wakeAssistantStore = useWakeAssistantStore()
const { enabled, avatarVideoLoop, avatarVideoPlaybackRate, avatarVideoSrc } = storeToRefs(wakeAssistantStore)
const ANSWER_VIDEO = '/video/rw-sh.webm'

const props = defineProps({
  // 是否正处于 TTS 音频播放阶段。
  isSpeaking: {
    type: Boolean,
    default: false,
  },
  // 是否显示语音唤醒开关。
  showSwitch: {
    type: Boolean,
    default: true,
  },
  // 开关文案，为空时保持原有紧凑开关。
  switchLabel: {
    type: String,
    default: '',
  },
  // 是否水平镜像数字人画面。
  mirror: {
    type: Boolean,
    default: false,
  },
})

// 说话状态只临时覆盖展示视频，不影响唤醒流程维护的原始视频状态。
const displayVideoSrc = computed(() => (props.isSpeaking ? ANSWER_VIDEO : avatarVideoSrc.value))
const displayVideoLoop = computed(() => (props.isSpeaking ? true : avatarVideoLoop.value))
const displayVideoPlaybackRate = computed(() => (props.isSpeaking ? 1 : avatarVideoPlaybackRate.value))

function setVideoRef(element, index) {
  videoRefs.value[index] = element
}

/**
 * 切换语音唤醒监听状态。
 * @param {boolean} status - 是否启用语音唤醒。
 * @returns {void}
 */
function changeWakeStatus(status) {
  if (status) wakeAssistantStore.enable()
  else wakeAssistantStore.disable()
}

function waitUntilPlayable(video) {
  if (video.readyState >= HTMLMediaElement.HAVE_FUTURE_DATA) return Promise.resolve()
  return new Promise((resolve) => {
    video.addEventListener('canplay', resolve, { once: true })
    video.addEventListener('error', resolve, { once: true })
  })
}

async function switchVideo() {
  await nextTick()
  const video = videoRefs.value[1 - activeVideoIndex.value]
  if (!video) return
  video.pause()
  video.src = displayVideoSrc.value
  video.loop = displayVideoLoop.value
  video.load()
  await waitUntilPlayable(video)
  video.playbackRate = displayVideoPlaybackRate.value
  await video.play().catch(() => {})

  const previousVideo = videoRefs.value[activeVideoIndex.value]
  activeVideoIndex.value = 1 - activeVideoIndex.value
  window.setTimeout(() => previousVideo?.pause(), 160)
}

onMounted(switchVideo)
watch([displayVideoSrc, displayVideoLoop, displayVideoPlaybackRate], switchVideo)
</script>

<style scoped>
.wake-avatar-video {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: visible;
}

video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  transition: opacity 160ms linear;
}

.wake-avatar-video.mirrored video {
  transform: scaleX(-1);
}

video.active {
  opacity: 1;
}

.wake-switch-control {
  position: absolute;
  right: -36px;
  bottom: 6px;
  z-index: 2;
  display: flex;
  align-items: center;
}

.wake-switch-control.has-label {
  gap: 8px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(53, 149, 251, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.08);
  backdrop-filter: blur(6px);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.wake-switch-control.has-label:hover,
.wake-switch-control.has-label.is-enabled {
  border-color: rgba(0, 123, 255, 0.42);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 3px 10px rgba(0, 123, 255, 0.14);
}

.wake-switch-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #617185;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.wake-switch-label i {
  color: #3595FB;
  font-size: 14px;
}

.wake-switch-control.is-enabled .wake-switch-label {
  color: #007BFF;
}

:deep(.wake-switch .el-switch__core) {
  --el-switch-on-color: #4394ef;
  --el-switch-off-color: #dce8f5;
}
</style>
