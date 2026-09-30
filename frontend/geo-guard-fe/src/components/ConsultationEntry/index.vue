<template>
  <div class="consultation-entry">
    <Transition name="consultation-float">
      <button
        class="meeting-floating-btn"
        :class="{ 'is-dragging': isMeetingDragging }"
        :style="meetingButtonStyle"
        type="button"
        title="会商研判"
        aria-label="打开会商研判"
        @pointerdown="startMeetingDrag"
        @keydown.enter="handleOpenModal"
      >
        <img :src="meetingIcon" alt="" />
      </button>
    </Transition>

    <ConsultationModal
      :visible="modalVisible"
      v-model:initial-tab="modalInitialTab"
      @close="handleCloseModal"
      @create="handleCreateMeeting"
      @enter="handleEnterMeeting"
    />

    <LiveRoom
      v-model:live-room-show="liveRoomShow"
      :meeting-id="meetingId"
      :mainer-id="mainerId"
      :participants="currentParticipants"
      @meeting-closed="handleMeetingClosed"
    />

    <!-- 收到纪要分享通知时直接弹出的纪要查看弹窗 -->
    <ViewMinutesModal
      v-model:visible="minutesModalVisible"
      :meeting="minutesMeeting"
      :summary="minutesSummary"
      :readonly="true"
      @back="minutesModalVisible = false"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ConsultationModal from '@/components/ConsultationModal/index.vue'
import LiveRoom from '@/components/LiveRoom/index.vue'
import ViewMinutesModal from '@/components/ViewMinutesModal/index.vue'
import { useConsultation } from '@/composables/useConsultation.js'
import { getMeetingDetail } from '@/api/meeting.js'
import { eventBus } from '@/utils/eventBus.js'
import meetingIcon from '@/assets/imgs/meeting.png'

defineOptions({ name: 'ConsultationEntry' })

const modalVisible = ref(false)
const modalInitialTab = ref('current')
const meetingPosition = ref({ left: null, top: null })
/** 拖动中：用于临时关闭位移动画，保证跟手 */
const isMeetingDragging = ref(false)

const minutesModalVisible = ref(false)
const minutesMeeting = ref(null)
const minutesSummary = ref(null)

const {
  meetingId,
  mainerId,
  currentParticipants,
  liveRoomShow,
  startOrJoinConsultation,
  handleMeetingClosed,
} = useConsultation()

const meetingButtonStyle = computed(() => {
  const style = {
    left: `${meetingPosition.value.left ?? 16}px`,
  }
  if (meetingPosition.value.top === null) {
    style.bottom = '20px'
  } else {
    style.top = `${meetingPosition.value.top}px`
  }
  return style
})

/**
 * 处理浮动图标拖拽移动
 */
const startMeetingDrag = (event) => {
  const button = event.currentTarget
  if (event.button !== 0) return
  event.preventDefault()

  /* 定位基准取「最近的已定位祖先」而非父节点：
     组件外层是 display: contents（不生成盒模型），若以它为基准坐标全为0，
     offsetParent 指向 layout-main */
  const container = button.offsetParent
  if (!container) return

  const containerRect = container.getBoundingClientRect()
  const buttonRect = button.getBoundingClientRect()
  const startX = event.clientX
  const startY = event.clientY
  const scrollLeft = container.scrollLeft || 0
  const scrollTop = container.scrollTop || 0
  const initialLeft = (buttonRect.left - containerRect.left) + scrollLeft
  const initialTop = (buttonRect.top - containerRect.top) + scrollTop
  let moved = false

  /* 拖拽期间关闭过渡，避免 left/top 被动画追赶产生顿挫 */
  isMeetingDragging.value = true
  button.setPointerCapture(event.pointerId)

  const move = (moveEvent) => {
    const dx = moveEvent.clientX - startX
    const dy = moveEvent.clientY - startY
    moved ||= Math.abs(dx) > 3 || Math.abs(dy) > 3

    const curScrollLeft = container.scrollLeft || 0
    const curScrollTop = container.scrollTop || 0
    const maxLeft = container.clientWidth - buttonRect.width + curScrollLeft
    const maxTop = container.clientHeight - buttonRect.height + curScrollTop

    meetingPosition.value = {
      left: Math.max(curScrollLeft, Math.min(maxLeft, initialLeft + dx)),
      top: Math.max(curScrollTop, Math.min(maxTop, initialTop + dy)),
    }
  }

  const end = (endEvent) => {
    isMeetingDragging.value = false
    button.removeEventListener('pointermove', move)
    button.removeEventListener('pointerup', end)
    button.removeEventListener('pointercancel', end)

    // 位移小于阈值判定为单击，触发弹窗显示
    if (!moved) {
      handleOpenModal()
    }

    if (button.hasPointerCapture(endEvent.pointerId)) {
      button.releasePointerCapture(endEvent.pointerId)
    }
  }

  button.addEventListener('pointermove', move)
  button.addEventListener('pointerup', end)
  button.addEventListener('pointercancel', end)
}

const handleOpenModal = () => {
  modalInitialTab.value = 'current'
  modalVisible.value = true
}

const handleCloseModal = () => {
  modalVisible.value = false
  modalInitialTab.value = 'current'
}

const handleCreateMeeting = async (payload = {}) => {
  await startOrJoinConsultation({
    title: payload.title,
    participantIds: payload.participantIds,
    transcriptionEnabled: payload.transcriptionEnabled,
    openVideo: true,
  })
  modalVisible.value = false
}

const handleEnterMeeting = async (meeting = {}) => {
  if (!meeting?.id) return
  await startOrJoinConsultation({ meetingId: String(meeting.id), openVideo: true })
  modalVisible.value = false
}

const handleOpenMinutesFromNotice = async ({ meetingId: targetMeetingId } = {}) => {
  if (!targetMeetingId) return
  try {
    const res = await getMeetingDetail(targetMeetingId)
    const detail = res?.meeting || res?.data?.meeting || {}
    const participants = res?.participants || res?.data?.participants || []
    const summary = res?.summary || res?.data?.summary || null

    minutesMeeting.value = { ...detail, id: targetMeetingId, participants }
    minutesSummary.value = summary
    minutesModalVisible.value = true
  } catch (err) {
    console.error('获取通知对应的会议纪要失败', err)
    ElMessage.error(err?.message || '获取会议纪要详情失败')
  }
}

const handleOpenMeetingFromNotice = async (payload = {}) => {
  // 点击会商研判邀请通知后，打开会商研判弹窗（支持指定初始 Tab，如 history）
  if (payload?.tab) {
    modalInitialTab.value = payload.tab
    modalVisible.value = true
    return
  }

  if (payload?.meetingId) {
    try {
      const detail = await getMeetingDetail(payload.meetingId, { notUseError: true })
      if (detail?.meeting?.status && detail.meeting.status !== 'ACTIVE') {
        ElMessage.info('该会议已结束，已为您跳转至历史会议')
        modalInitialTab.value = 'history'
        modalVisible.value = true
        return
      }
    } catch (e) {
      console.warn('获取会议详情失败', e)
    }
  }

  modalInitialTab.value = 'current'
  modalVisible.value = true
}

onMounted(() => {
  eventBus.on('OPEN_MINUTES_FROM_NOTICE', handleOpenMinutesFromNotice)
  eventBus.on('OPEN_MEETING_FROM_NOTICE', handleOpenMeetingFromNotice)
})

onBeforeUnmount(() => {
  eventBus.off('OPEN_MINUTES_FROM_NOTICE', handleOpenMinutesFromNotice)
  eventBus.off('OPEN_MEETING_FROM_NOTICE', handleOpenMeetingFromNotice)
})
</script>

<style lang="less" scoped>
.consultation-entry {
  display: contents;
}

.meeting-floating-btn {
  position: absolute;
  z-index: 1001;
  width: 80px;
  height: 84px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: grab;
  touch-action: none;
  user-select: none;
  transition: transform 0.2s ease, filter 0.2s ease;

  &:hover {
    filter: drop-shadow(0 6px 16px rgba(0, 123, 255, 0.46));
    transform: translateY(-2px) scale(1.05);
  }

  &:active {
    cursor: grabbing;
    transform: scale(0.96);
  }

  /* 拖动中：关闭过渡与悬浮位移，置于顶层保证跟手不顿挫 */
  &.is-dragging {
    z-index: 1002;
    transition: none;
    transform: none;
    filter: none;
  }

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
    pointer-events: none;
  }
}

.consultation-float-enter-active,
.consultation-float-leave-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.consultation-float-enter-from,
.consultation-float-leave-to {
  opacity: 0;
  transform: scale(0.66);
}
</style>

