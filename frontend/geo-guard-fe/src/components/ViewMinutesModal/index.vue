<template>
  <Teleport to="body">
    <Transition name="view-minutes-fade">
      <div v-if="visible" class="view-minutes-mask" @click.self="handleBack">
        <div class="view-minutes-layout">
          <!-- 主窗口 -->
          <section
            class="view-minutes-modal"
            :class="{ 'is-readonly': isReadonly }"
            role="dialog"
            aria-modal="true"
            aria-labelledby="view-minutes-title"
          >
            <!-- 顶部导航栏 -->
            <header class="modal-header">
              <button
                type="button"
                class="back-btn"
                title="返回"
                aria-label="返回"
                @click="handleBack"
              >
                <img :src="backIcon" alt="返回" />
              </button>

              <h2 id="view-minutes-title" class="meeting-title" :title="meetingTitle">
                {{ meetingTitle }}
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

            <!-- 生成过程横幅卡片（可配置隐藏） -->
            <ProcessBanner
              v-if="showProcess"
              :duration="generationDuration"
              @finish="handleProcessFinish"
            />

            <!-- 纪要正文容器 -->
            <MinutesContent
              :title="meetingTitle"
              :summary="summary"
              :loading="isContentLoading"
            />

            <!-- 底部操作栏（可配置隐藏） -->
            <footer v-if="showFooter" class="modal-footer">
              <div class="footer-tip">
                <span class="tip-icon">ⓘ</span>
                <span>可将智能纪要分享给参会人员</span>
              </div>

              <div class="footer-actions">
                <button
                  type="button"
                  class="btn-primary"
                  :class="{ 'is-active': drawerVisible }"
                  @click="drawerVisible = !drawerVisible"
                >
                  分享纪要
                </button>
              </div>
            </footer>
          </section>

          <!-- 右侧抽屉：发送给责任人 -->
          <ShareDrawer
            v-if="showFooter"
            v-model:visible="drawerVisible"
            :meeting-id="meeting?.id"
            :participants="meeting?.participants"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import backIcon from '@/assets/imgs/chatRoom/back.png'
import ProcessBanner from './components/ProcessBanner.vue'
import MinutesContent from './components/MinutesContent.vue'
import ShareDrawer from './components/ShareDrawer.vue'

defineOptions({ name: 'ViewMinutesModal' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  meeting: {
    type: Object,
    default: () => ({}),
  },
  summary: {
    type: Object,
    default: () => null,
  },
  generationDuration: {
    type: [Number, String],
    default: null,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  hideProcess: {
    type: Boolean,
    default: false,
  },
  hideFooter: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:visible', 'back'])

const drawerVisible = ref(false)
const isProcessFinished = ref(false)

const isReadonly = computed(() => Boolean(props.readonly))
const showProcess = computed(() => !props.readonly && !props.hideProcess)
const showFooter = computed(() => !props.readonly && !props.hideFooter)

// 若展示进度条，正文必须等待进度条执行满；若不展示进度条，则直接就绪
const isContentLoading = computed(() => {
  return showProcess.value && !isProcessFinished.value
})

const handleProcessFinish = () => {
  isProcessFinished.value = true
}

const meetingTitle = computed(() => {
  return props.meeting?.title || '应急会商'
})

const hostName = computed(() => {
  return props.meeting?.hostName || props.meeting?.creatorName || ''
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

const meetingTimeText = computed(() => {
  return formatDateTime(props.meeting?.createdAt, props.meeting?.endedAt)
})

const formatDateTime = (start, end) => {
  if (!start) return ''
  const startDate = dayjs(start)
  const endDate = end ? dayjs(end) : null
  const datePart = startDate.format('YYYY年M月D日')
  const startTimePart = startDate.format('HH:mm')
  const endTimePart = endDate ? endDate.format('HH:mm') : ''
  return endTimePart ? `${datePart}${startTimePart}-${endTimePart}` : `${datePart} ${startTimePart}`
}

const handleBack = () => {
  emit('update:visible', false)
  emit('back')
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      drawerVisible.value = false
      isProcessFinished.value = !showProcess.value
    } else {
      isProcessFinished.value = false
    }
  },
  { immediate: true },
)
</script>

<style lang="less" scoped>
.view-minutes-mask {
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

.view-minutes-layout {
  display: flex;
  align-items: stretch;
  gap: 12px;
  max-width: calc(100vw - 48px);
  max-height: calc(100vh - 48px);
}

.view-minutes-modal {
  width: 1200px;
  max-width: calc(100vw - 48px);
  height: 794px;
  max-height: calc(100vh - 48px);
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.16);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;

  &.is-readonly {
    :deep(.minutes-container) {
      padding-bottom: 24px;
    }
  }
}

// 顶部 Header
.modal-header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
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
    max-width: 420px;
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
    line-height: 16px;
    white-space: nowrap;

    .num-font {
      font-style: normal;
    }

    .meta-divider {
      margin: 0 8px;
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

// 底部操作栏
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  border-top: 1px solid #e4eaef;
  background: #ffffff;
  flex-shrink: 0;

  .footer-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #a6acb8;
    font-size: 12px;
    line-height: 16px;

    .tip-icon {
      font-size: 14px;
      line-height: 14px;
    }
  }

  .footer-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .btn-secondary {
      width: 84px;
      height: 32px;
      padding: 0;
      border: 1px solid #007bff;
      border-radius: 6px;
      background: #ffffff;
      color: #007bff;
      font-size: 12px;
      font-weight: 400;
      line-height: 30px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #f0f7ff;
      }

      &.is-active {
        background: #e6f2ff;
        font-weight: 700;
      }
    }

    .btn-primary {
      width: 84px;
      height: 32px;
      padding: 0;
      border: 0;
      border-radius: 6px;
      background: #007bff;
      color: #ffffff;
      font-size: 12px;
      font-weight: 400;
      line-height: 32px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #006ce3;
      }

      &.is-confirmed {
        background: #44b699;
        cursor: default;
      }
    }
  }
}

.num-font {
  font-style: normal;
}

// 动画
.view-minutes-fade-enter-active,
.view-minutes-fade-leave-active {
  transition: opacity 0.25s ease;

  .view-minutes-modal {
    transition: transform 0.25s cubic-bezier(0.25, 1, 0.5, 1);
  }
}

.view-minutes-fade-enter-from,
.view-minutes-fade-leave-to {
  opacity: 0;

  .view-minutes-modal {
    transform: scale(0.96);
  }
}
</style>
