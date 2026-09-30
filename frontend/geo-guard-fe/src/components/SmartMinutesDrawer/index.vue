<template>
  <Transition name="smart-minutes">
    <aside
      v-if="visible"
      class="smart-minutes-drawer"
      :class="{ 'is-fullscreen': isFullScreen }"
      aria-label="智能纪要"
    >
      <div class="drawer-inner">
        <header class="drawer-header">
          <div class="drawer-title">
            <img :src="minutesIcon" alt="" />
            <strong>智能纪要</strong>
          </div>
          <button type="button" title="关闭智能纪要" aria-label="关闭智能纪要" @click="emit('update:visible', false)">
            <i class="iconfont icon-close"></i>
          </button>
        </header>
        <section class="switch-card">
          <div class="switch-row">
            <strong>智能纪要</strong>
            <button
              class="summary-switch"
              :class="{ 'is-off': !enabled, 'is-loading': switchLoading, 'is-disabled': !canToggleSwitch }"
              type="button"
              role="switch"
              :aria-checked="enabled"
              :disabled="switchLoading || !canToggleSwitch"
              :title="switchTooltip"
              @click="handleToggleSwitch"
            >
              <i class="switch-handle"></i>
            </button>
          </div>
          <p>会后将自动生成智能纪要，可在历史会议中查看。</p>
        </section>
        <div v-if="enabled" class="recording-status">
          <span class="sound-waves"><i v-for="item in 5" :key="item"></i></span>
          <span>正在记录会议内容...</span>
        </div>
        <p v-else class="disabled-tip">本次会议未开启智能纪要</p>
        <div ref="scrollRef" class="transcript-list" @scroll="handleScroll">
          <TranscriptItem v-for="item in displayTranscripts" :key="item.id" :item="item" />
          <div v-if="!loading && !displayTranscripts.length" class="empty-state">暂无实时发言记录</div>
        </div>
      </div>
    </aside>
  </Transition>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMeetingTranscriptionStatus,
  updateMeetingTranscriptionSwitch,
  updateMeetingSummarySwitch,
} from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'
import minutesIcon from '@/assets/imgs/chatRoom/zhineng.png'
import TranscriptItem from './TranscriptItem.vue'

defineOptions({ name: 'SmartMinutesDrawer' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  meetingId: { type: String, default: '' },
  mainerId: { type: [String, Number], default: '' },
  isFullScreen: { type: Boolean, default: false },
  liveTranscripts: { type: Array, default: () => [] },
  loadHistory: { type: Function, default: null },
})

const emit = defineEmits(['update:visible'])

const userStore = useUserStore()
const currentUserId = computed(() => {
  return String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
})
const isHost = computed(() => {
  if (!props.mainerId) return true
  return Boolean(currentUserId.value && String(props.mainerId) === currentUserId.value)
})
const canToggleSwitch = computed(() => isHost.value)
const switchTooltip = computed(() => {
  if (!canToggleSwitch.value) return '仅主持人可开启或关闭智能纪要'
  return enabled.value ? '点击关闭智能纪要' : '点击开启智能纪要'
})

// 视频会议期间完全由实时流驱动展示
const displayTranscripts = computed(() => {
  return Array.isArray(props.liveTranscripts) ? props.liveTranscripts : []
})

const enabled = ref(true)
const loading = ref(false)
const switchLoading = ref(false)
const scrollRef = ref(null)
const shouldStickBottom = ref(true)

const handleScroll = () => {
  const element = scrollRef.value
  if (!element) return
  shouldStickBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 32
}

const scrollToBottom = async () => {
  if (!shouldStickBottom.value) return
  await nextTick()
  scrollRef.value?.scrollTo({ top: scrollRef.value.scrollHeight, behavior: 'smooth' })
}

/**
 * 拉取智能纪要开关状态（无需轮询持久化记录）
 */
const fetchTranscriptionStatus = async () => {
  if (!props.meetingId) return
  loading.value = true
  try {
    const statusResult = await getMeetingTranscriptionStatus(props.meetingId)
    let newEnabled = null
    if (typeof statusResult?.enabled === 'boolean') {
      newEnabled = statusResult.enabled
    } else if (typeof statusResult?.data?.enabled === 'boolean') {
      newEnabled = statusResult.data.enabled
    }

    if (typeof newEnabled === 'boolean') {
      enabled.value = newEnabled
    }
  } catch (error) {
    console.warn('获取智能纪要开关状态异常', error)
  } finally {
    loading.value = false
  }
}

const handleToggleSwitch = async () => {
  if (switchLoading.value) return
  if (!canToggleSwitch.value) {
    ElMessage.warning('仅主持人可开启或关闭智能纪要')
    return
  }
  if (!props.meetingId) {
    ElMessage.warning('未获取到有效会议信息')
    return
  }

  const targetState = !enabled.value
  switchLoading.value = true

  try {
    if (targetState) {
      await Promise.all([
        updateMeetingTranscriptionSwitch(props.meetingId, true),
        updateMeetingSummarySwitch(props.meetingId, true),
      ])
      enabled.value = true
      ElMessage.success('已开启智能纪要')
    } else {
      await updateMeetingTranscriptionSwitch(props.meetingId, false)
      enabled.value = false
      ElMessage.success('已关闭智能纪要')
    }
    await fetchTranscriptionStatus()
  } catch (error) {
    console.error('切换智能纪要失败', error)
    ElMessage.error(error?.msg || error?.message || '切换智能纪要失败，请重试')
  } finally {
    switchLoading.value = false
  }
}

watch(
  () => [props.visible, props.meetingId],
  async ([visible, meetingId]) => {
    if (!visible || !meetingId) return
    shouldStickBottom.value = true
    await Promise.allSettled([
      fetchTranscriptionStatus(),
      typeof props.loadHistory === 'function' ? props.loadHistory() : Promise.resolve(),
    ])
    scrollToBottom()
  },
  { immediate: true },
)

// 监听实时流更新，自动平滑滚到底部
watch(
  () => {
    const list = displayTranscripts.value
    const last = list[list.length - 1]
    return [list.length, last?.text, last?.type]
  },
  () => {
    scrollToBottom()
  },
)
</script>

<style lang="less" scoped>
.smart-minutes-drawer {
  width: 321px;
  height: 794px;
  max-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(34, 37, 39, 0.12);
  box-sizing: border-box;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;

  &.is-fullscreen {
    position: fixed;
    top: 50%;
    right: 20px;
    transform: translateY(-50%);
    z-index: 1002;
  }
}

.drawer-inner {
  width: 289px;
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-sizing: border-box;
}

.drawer-header,
.drawer-title,
.switch-card > div,
.recording-status {
  display: flex;
  align-items: center;
}

.drawer-header {
  height: 18px;
  justify-content: space-between;
}

.drawer-title {
  gap: 4px;

  img {
    width: 18px;
    height: 18px;
    object-fit: contain;
  }

  strong {
    color: #222527;
    font-size: 14px;
    font-weight: 500;
    line-height: 18px;
  }
}

.drawer-header > button {
  display: flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: #383c41;
  cursor: pointer;

  i {
    font-size: 14px;
  }
}

.switch-card {
  height: 72px;
  margin-top: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f1f8ff;
  box-sizing: border-box;

  > div {
    justify-content: space-between;
  }

  strong {
    color: #222527;
    font-size: 14px;
    font-weight: 500;
    line-height: 18px;
  }

  p {
    margin: 8px 0 0;
    color: #9096a2;
    font-size: 12px;
    line-height: 18px;
  }
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-switch {
  position: relative;
  width: 26px;
  height: 14px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #007bff;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s ease, opacity 0.2s ease;

  .switch-handle {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #ffffff;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &.is-off {
    background: #a6acb8;

    .switch-handle {
      transform: translateX(-12px);
    }
  }

  &.is-loading {
    opacity: 0.7;
    cursor: wait;
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.recording-status {
  height: 20px;
  margin-top: 12px;
  gap: 4px;

  > span:last-child {
    background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
    background-clip: text;
    color: transparent;
    font-size: 12px;
    line-height: 20px;
  }
}

.sound-waves {
  display: flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  gap: 2px;

  i {
    width: 2px;
    border-radius: 2px;
    background: #007bff;
    animation: sound-pulse 1.2s ease-in-out infinite;
  }

  i:nth-child(1),
  i:nth-child(5) { height: 4px; }
  i:nth-child(2),
  i:nth-child(4) { height: 8px; animation-delay: 0.15s; }
  i:nth-child(3) { height: 12px; animation-delay: 0.3s; }
}

.disabled-tip,
.empty-state {
  color: #9096a2;
  font-size: 12px;
  line-height: 18px;
}

.disabled-tip {
  margin: 12px 0 0;
}

.transcript-list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
  overflow-y: auto;
  padding-right: 4px;
}

.empty-state {
  margin: auto 0;
  text-align: center;
}

.smart-minutes-enter-active,
.smart-minutes-leave-active {
  transition: width 0.3s cubic-bezier(0.25, 1, 0.5, 1),
              padding 0.3s cubic-bezier(0.25, 1, 0.5, 1),
              margin 0.3s cubic-bezier(0.25, 1, 0.5, 1),
              opacity 0.25s ease;
}

.smart-minutes-enter-from,
.smart-minutes-leave-to {
  width: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  margin-left: -16px !important;
  opacity: 0;
}

.smart-minutes-drawer.is-fullscreen {
  &.smart-minutes-enter-active,
  &.smart-minutes-leave-active {
    transition: transform 0.3s cubic-bezier(0.25, 1, 0.5, 1),
                opacity 0.25s ease;
  }

  &.smart-minutes-enter-from,
  &.smart-minutes-leave-to {
    width: 321px !important;
    padding: 16px !important;
    margin-left: 0 !important;
    opacity: 0;
    transform: translate(40px, -50%);
  }
}

@keyframes sound-pulse {
  0%, 100% {
    transform: scaleY(0.4);
  }
  50% {
    transform: scaleY(1);
  }
}
</style>
