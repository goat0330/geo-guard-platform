<template>
  <Teleport to="body">
    <Transition name="minutes-modal-fade">
      <div v-if="visible" class="generate-minutes-mask" @click.self="handleBack">
        <section
          class="generate-minutes-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="minutes-modal-title"
        >
          <!-- 顶部导航栏 -->
          <header class="modal-header">
            <button
              type="button"
              class="back-btn"
              title="返回历史会议"
              aria-label="返回历史会议"
              @click="handleBack"
            >
              <img :src="backIcon" alt="返回" />
            </button>

            <h2 id="minutes-modal-title" class="meeting-title" :title="meeting?.title || '应急会商'">
              {{ meeting?.title || '应急会商' }}
            </h2>

            <div class="meeting-meta">
              <span v-if="meetingTimeText" class="num-font">{{ meetingTimeText }}</span>
              <span v-if="meetingTimeText && durationText" class="meta-divider">｜</span>
              <span v-if="durationText">会议时长<em class="num-font">{{ durationText }}</em></span>
              <span v-if="(meetingTimeText || durationText) && hostName" class="meta-divider">｜</span>
              <span v-if="hostName">发起人{{ hostName }}</span>
              <span v-if="(meetingTimeText || durationText || hostName) && participantCount !== null" class="meta-divider">｜</span>
              <span v-if="participantCount !== null"><em class="num-font">{{ participantCount }}</em>人参会</span>
            </div>

            <button
              type="button"
              class="close-btn"
              title="关闭"
              aria-label="关闭会议纪要"
              @click="handleBack"
            >
              <i class="iconfont icon-close"></i>
            </button>
          </header>

          <!-- 中间 Loading 与生成状态内容区 -->
          <main class="modal-body">
            <div class="loading-wrap">
              <!-- 菊花旋转 Loading -->
              <div class="flower-spinner">
                <span v-for="i in 8" :key="i" :class="`blade-${i}`"></span>
              </div>

              <!-- 主标题 -->
              <h3 class="generating-title">正在生成智能纪要...</h3>

              <!-- 说明胶囊标签 -->
              <div class="generating-badge">
                智能体正在读取会议转写，并持续整理重要结论
              </div>

              <!-- 阶段步骤列表 -->
              <ul class="step-list">
                <li
                  v-for="(step, index) in steps"
                  :key="step"
                  :class="{
                    'is-active': index === currentStep,
                    'is-done': index < currentStep,
                    'is-pending': index > currentStep,
                  }"
                >
                  <i class="step-dot"></i>
                  <span>{{ step }}</span>
                </li>
              </ul>
            </div>
          </main>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import backIcon from '@/assets/imgs/chatRoom/back.png'
import { getMeetingDetail } from '@/api/meeting.js'

defineOptions({ name: 'GenerateMinutesModal' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  meeting: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:visible', 'back', 'completed'])

const steps = [
  '开始生成图文总结',
  '理解会议内容',
  '梳理会议脉络',
  '内容生成中',
]

const currentStep = ref(0)
let timerIds = []
let pollTimer = null
let isCancelled = false

const clearAllTimers = () => {
  timerIds.forEach((id) => clearTimeout(id))
  timerIds = []
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

const formatDateTime = (start, end) => {
  if (!start) return ''
  const startDate = dayjs(start)
  const endDate = end ? dayjs(end) : null
  const datePart = startDate.format('YYYY年M月D日')
  const startTimePart = startDate.format('HH:mm')
  const endTimePart = endDate ? endDate.format('HH:mm') : ''
  return endTimePart ? `${datePart}${startTimePart}-${endTimePart}` : `${datePart} ${startTimePart}`
}

const hostName = computed(() => {
  return props.meeting?.hostName || props.meeting?.creatorName || ''
})

const durationText = computed(() => {
  if (!props.meeting?.createdAt || !props.meeting?.endedAt) {
    return ''
  }
  const start = dayjs(props.meeting.createdAt)
  const end = dayjs(props.meeting.endedAt)
  const totalSeconds = Math.max(0, end.diff(start, 'second'))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes === 0 && seconds === 0) return '0秒'
  return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分钟`
})

const participantCount = computed(() => {
  if (Array.isArray(props.meeting?.participants) && props.meeting.participants.length) {
    return props.meeting.participants.length
  }
  if (Array.isArray(props.meeting?.participantIds) && props.meeting.participantIds.length) {
    return props.meeting.participantIds.length
  }
  return null
})

const meetingTimeText = computed(() => {
  return formatDateTime(props.meeting?.createdAt, props.meeting?.endedAt)
})

const isSummaryReady = (sum) => {
  return Boolean(sum && sum.status === 'COMPLETED' && sum.content)
}

const startGeneration = async () => {
  clearAllTimers()
  isCancelled = false
  currentStep.value = 0

  const startTime = Date.now()
  const meetingId = props.meeting?.id

  // 1. 阶段动画平滑过渡到步骤 3（内容生成中），保持活跃
  timerIds.push(
    setTimeout(() => {
      if (!isCancelled) currentStep.value = 1
    }, 800),
  )
  timerIds.push(
    setTimeout(() => {
      if (!isCancelled) currentStep.value = 2
    }, 1600),
  )
  timerIds.push(
    setTimeout(() => {
      if (!isCancelled) currentStep.value = 3
    }, 2400),
  )

  // 2. 持续轮询接口，直到能够获取到完整的会议纪要
  let fetchedDetail = null
  let fetchedSummary = null
  let fetchedParticipants = []
  const maxPollDuration = 90000 // 最大轮询 90 秒超时保护
  const pollInterval = 1500

  while (!isCancelled) {
    try {
      if (meetingId) {
        const res = await getMeetingDetail(meetingId, { notUseError: true })
        fetchedDetail = res?.meeting || res?.data?.meeting || null
        fetchedSummary = res?.summary || res?.data?.summary || null
        fetchedParticipants = res?.participants || res?.data?.participants || []

        // 如果纪要已经生成完成且有内容，成功退出轮询
        if (isSummaryReady(fetchedSummary)) {
          break
        }

        // 如果后端明确标记失败
        if (fetchedSummary?.status === 'FAILED') {
          ElMessage.error(fetchedSummary?.error || '智能纪要生成失败')
          handleBack()
          return
        }
      }
    } catch (err) {
      console.warn('轮询会议纪要异常', err)
    }

    if (isCancelled) return

    // 超时检测
    if (Date.now() - startTime >= maxPollDuration) {
      ElMessage.warning('会议纪要生成中，用时较长，请稍后在历史会议中查看')
      handleBack()
      return
    }

    // 等待下一次轮询
    await new Promise((resolve) => {
      pollTimer = setTimeout(resolve, pollInterval)
    })
  }

  if (isCancelled) return

  // 3. 获取成功：保底让前序动画平滑衔接，并将第 4 步完成拉满
  const elapsed = Date.now() - startTime
  const remainingWait = Math.max(0, 2600 - elapsed)

  timerIds.push(
    setTimeout(() => {
      if (isCancelled) return
      currentStep.value = 4 // 全部打勾完成
      timerIds.push(
        setTimeout(() => {
          if (isCancelled) return
          const totalDurationSec = Math.max(1, Math.round((Date.now() - startTime) / 1000))
          const mergedMeeting = {
            ...props.meeting,
            ...(fetchedDetail || {}),
            participants: fetchedParticipants.length
              ? fetchedParticipants
              : props.meeting?.participants || [],
          }
          emit('completed', {
            meeting: mergedMeeting,
            detail: fetchedDetail,
            summary: fetchedSummary,
            generationDuration: totalDurationSec,
          })
        }, 300),
      )
    }, remainingWait),
  )
}

const handleBack = () => {
  isCancelled = true
  clearAllTimers()
  emit('update:visible', false)
  emit('back')
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      startGeneration()
    } else {
      isCancelled = true
      clearAllTimers()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  isCancelled = true
  clearAllTimers()
})
</script>

<style lang="less" scoped>
.generate-minutes-mask {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 2010;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
}

.generate-minutes-modal {
  width: 1200px;
  max-width: calc(100vw - 48px);
  height: 794px;
  max-height: calc(100vh - 48px);
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.16);
  display: flex;
  flex-direction: column;
  padding: 24px 32px;
  box-sizing: border-box;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  height: 32px;
  flex-shrink: 0;

  .back-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;
    transition: transform 0.2s ease, opacity 0.2s ease;

    &:hover {
      opacity: 0.8;
      transform: scale(1.05);
    }

    &:active {
      transform: scale(0.95);
    }

    img {
      width: 20px;
      height: 20px;
      object-fit: contain;
      display: block;
    }
  }

  .meeting-title {
    margin: 0 0 0 12px;
    color: #222527;
    font-size: 18px;
    font-weight: 700;
    line-height: 28px;
    max-width: 380px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .meeting-meta {
    display: flex;
    align-items: center;
    margin-left: 16px;
    color: #9096a2;
    font-size: 14px;
    font-weight: 400;
    line-height: 14px;
    white-space: nowrap;

    .num-font {
      font-style: normal;
    }

    .meta-divider {
      margin: 0 6px;
      color: #d8ebff;
    }

    em {
      font-style: normal;
    }
  }

  .close-btn {
    display: inline-flex;
    width: 28px;
    height: 28px;
    flex: 0 0 28px;
    align-items: center;
    justify-content: center;
    margin-left: auto;
    padding: 0;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #222527;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background: #f2f3f5;
    }

    i {
      font-size: 14px;
    }
  }
}

.modal-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

// 8瓣菊花旋转 Loading
.flower-spinner {
  position: relative;
  width: 44px;
  height: 44px;
  animation: spin 1s linear infinite;

  span {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 2.5px;
    height: 9px;
    margin-top: -19px;
    margin-left: -1.25px;
    border-radius: 2px;
    background: #007bff;
    transform-origin: center 19px;
  }

  .blade-1 {
    transform: rotate(0deg);
    opacity: 0.3;
  }

  .blade-2 {
    transform: rotate(45deg);
    opacity: 0.4;
  }

  .blade-3 {
    transform: rotate(90deg);
    opacity: 0.5;
  }

  .blade-4 {
    transform: rotate(135deg);
    opacity: 0.6;
  }

  .blade-5 {
    transform: rotate(180deg);
    opacity: 0.7;
  }

  .blade-6 {
    transform: rotate(225deg);
    opacity: 0.8;
  }

  .blade-7 {
    transform: rotate(270deg);
    opacity: 0.9;
  }

  .blade-8 {
    transform: rotate(315deg);
    opacity: 1;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.generating-title {
  margin: 24px 0 0;
  color: #007bff;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
}

.generating-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  margin-top: 14px;
  padding: 0 24px;
  border-radius: 96px;
  background: rgba(0, 123, 255, 0.16);
  color: #007bff;
  font-size: 14px;
  font-weight: 400;
  line-height: normal;
  text-align: center;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 32px 0 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #a6acb8;
    font-size: 14px;
    font-weight: 400;
    line-height: 16px;
    transition: color 0.3s ease;

    &.is-active {
      color: #007bff;
      font-weight: 500;

      .step-dot {
        width: 6px;
        height: 6px;
        background: #007bff;
        box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25);
        animation: pulseDot 1.2s infinite ease-in-out;
      }
    }

    &.is-done {
      color: #383c41;

      .step-dot {
        width: 6px;
        height: 6px;
        background: #44b699;
      }
    }

    &.is-pending {
      color: #a6acb8;

      .step-dot {
        background: #a6acb8;
      }
    }
  }

  .step-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #a6acb8;
    flex-shrink: 0;
    transition: all 0.3s ease;
  }
}

@keyframes pulseDot {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.2);
  }
  50% {
    transform: scale(1.2);
    box-shadow: 0 0 0 4px rgba(0, 123, 255, 0.35);
  }
}

// 弹窗淡入淡出动画
.minutes-modal-fade-enter-active,
.minutes-modal-fade-leave-active {
  transition: opacity 0.25s ease;

  .generate-minutes-modal {
    transition: transform 0.25s cubic-bezier(0.25, 1, 0.5, 1);
  }
}

.minutes-modal-fade-enter-from,
.minutes-modal-fade-leave-to {
  opacity: 0;

  .generate-minutes-modal {
    transform: scale(0.96);
  }
}
</style>
