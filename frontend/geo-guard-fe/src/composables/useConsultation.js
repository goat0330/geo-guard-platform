import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createMeeting,
  getMeetingList,
  getMeetingDetail,
  addMeetingParticipants,
  endMeeting,
  updateMeetingTranscriptionSwitch,
  updateMeetingSummarySwitch,
} from '@/api/meeting.js'
import { eventBus, EventKey } from '@/utils/eventBus.js'
import { useUserStore } from '@/store/user.js'

/**
 * 视频会商研判业务逻辑 Composable
 * 支持会商室控制条、中途拉人、视频会议唤起与状态协同
 */
export function useConsultation() {
  const userStore = useUserStore()
  const meetingId = ref('')
  const mainerId = ref('')
  const currentMeeting = ref(null)
  const currentParticipants = ref([])
  const consultationRoomVisible = ref(false)
  const liveRoomShow = ref(false)
  const isActionLoading = ref(false)

  // 当前用户是否为主持人
  const isMainer = computed(() => {
    const myUserId = String(userStore.userInfo?.user?.userId || '')
    if (!myUserId) return false
    const hostId = currentMeeting.value?.hostUserId || mainerId.value
    if (hostId && String(hostId) === myUserId) return true
    const myParticipant = currentParticipants.value.find(
      (p) => String(p.userId) === myUserId,
    )
    return myParticipant?.role === 'HOST'
  })

  /**
   * 获取并刷新会议详情与参会人员列表
   */
  const fetchMeetingDetail = async (id = meetingId.value) => {
    if (!id) return null
    try {
      const res = await getMeetingDetail(id)
      currentMeeting.value = res?.meeting || null
      currentParticipants.value = Array.isArray(res?.participants) ? res.participants : []
      if (res?.meeting?.hostUserId) {
        mainerId.value = String(res.meeting.hostUserId)
      }
      return res
    } catch (e) {
      console.error('获取会议详情异常', e)
      return null
    }
  }

  /**
   * 检查当前是否有被拉入的进行中会议（若有则直接加入并打开应急会商室）
   * @returns {Promise<boolean>} 是否存在并成功加入活跃会议
   */
  const checkAndJoinActiveMeeting = async () => {
    isActionLoading.value = true
    try {
      const res = await getMeetingList({ pageNum: 1, pageSize: 20 })
      const meetingList = Array.isArray(res?.data) ? res.data : []
      const activeMeeting = meetingList.find((m) => m.status === 'ACTIVE')

      if (activeMeeting) {
        meetingId.value = String(activeMeeting.id)
        mainerId.value = String(activeMeeting.hostUserId || '')
        currentMeeting.value = activeMeeting
        await fetchMeetingDetail(activeMeeting.id)
        consultationRoomVisible.value = true
        ElMessage.success('检测到已有会商研判邀请，已自动加入应急会商室')
        return true
      }
      return false
    } catch (e) {
      console.error('检查受邀会议异常', e)
      return false
    } finally {
      isActionLoading.value = false
    }
  }

  /**
   * 启动或加入会商研判
   * @param {object} options
   * @param {string} [options.meetingId] 指定加入的会议 ID
   * @param {string} [options.title='汛期地质灾害会商研判'] 会议主题
   * @param {string} [options.description] 会议说明
   * @param {string[]} [options.participantIds] 初始受邀系统用户 ID 数组
   * @param {boolean} [options.openVideo=false] 是否同时直接呼起音视频画面
   */
  const startOrJoinConsultation = async (options = {}) => {
    isActionLoading.value = true

    try {
      // 分支 0：指定了会议 ID（如来自系统消息邀请点击）
      if (options.meetingId) {
        const detailRes = await getMeetingDetail(options.meetingId)
        const targetMeeting = detailRes?.meeting
        if (targetMeeting?.status !== 'ACTIVE') {
          ElMessage.warning('该会议已结束或不存在')
          return
        }
        meetingId.value = String(targetMeeting.id)
        mainerId.value = String(targetMeeting.hostUserId || '')
        currentMeeting.value = targetMeeting
        currentParticipants.value = Array.isArray(detailRes?.participants) ? detailRes.participants : []
        consultationRoomVisible.value = true
        if (options.openVideo) {
          liveRoomShow.value = true
        }
        return
      }

      // 1. 查询当前用户的有效会议列表
      const res = await getMeetingList({ pageNum: 1, pageSize: 20 })
      const meetingList = Array.isArray(res?.data) ? res.data : []

      // 2. 检查当前是否存在进行中的会商会议
      const activeMeeting = meetingList.find((m) => m.status === 'ACTIVE')

      if (activeMeeting) {
        // 分支 A：已有进行中会议，加入并展示应急会商室
        const myUserId = String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
        meetingId.value = String(activeMeeting.id)
        mainerId.value = String(activeMeeting.hostUserId || '')
        currentMeeting.value = activeMeeting

        // 若当前用户是主持人且在弹窗中明确指定了开关状态，同步更新转写与纪要开关
        if (
          options.transcriptionEnabled !== undefined &&
          Boolean(options.transcriptionEnabled) !== Boolean(activeMeeting.transcriptionEnabled) &&
          String(activeMeeting.hostUserId || '') === myUserId
        ) {
          try {
            const enableState = Boolean(options.transcriptionEnabled)
            await Promise.all([
              updateMeetingTranscriptionSwitch(activeMeeting.id, enableState),
              updateMeetingSummarySwitch(activeMeeting.id, enableState),
            ])
            activeMeeting.transcriptionEnabled = enableState
            activeMeeting.summaryEnabled = enableState
          } catch (switchErr) {
            console.warn('同步已有会议智能纪要开关失败', switchErr)
          }
        }

        await fetchMeetingDetail(activeMeeting.id)
        consultationRoomVisible.value = true
        if (options.openVideo) {
          liveRoomShow.value = true
        }
        ElMessage.info('检测到当前已有会商进行中，已加入应急会商室')
      } else {
        // 分支 B：创建并开启新会议
        const rawTitle = (options.title || '汛期地质灾害会商研判').trim()
        const titleStr = rawTitle ? rawTitle.slice(0, 100) : '汛期地质灾害会商研判'
        const descStr =
          options.description !== undefined && options.description !== null
            ? String(options.description).slice(0, 2000)
            : null

        const myUserId = String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
        const cleanParticipantIds = Array.isArray(options.participantIds)
          ? options.participantIds.map(String).filter((id) => Boolean(id) && id !== myUserId)
          : []

        const createParams = {
          title: titleStr,
          description: descStr,
          participantIds: cleanParticipantIds,
        }

        const createRes = await createMeeting(createParams)
        const newMeeting = createRes?.meeting || (createRes?.id ? createRes : null)

        if (newMeeting?.id) {
          // 若用户在发起弹窗中指定了智能纪要开关，链式同步调用转写与纪要开关接口
          if (options.transcriptionEnabled !== undefined) {
            try {
              const enableState = Boolean(options.transcriptionEnabled)
              const switchRes = await updateMeetingTranscriptionSwitch(newMeeting.id, enableState)
              await updateMeetingSummarySwitch(newMeeting.id, enableState)
              if (switchRes?.meeting) {
                newMeeting.transcriptionEnabled = switchRes.meeting.transcriptionEnabled
              } else {
                newMeeting.transcriptionEnabled = enableState
              }
              newMeeting.summaryEnabled = enableState
            } catch (switchErr) {
              console.warn('同步切换会议转写与纪要开关失败', switchErr)
            }
          }

          meetingId.value = String(newMeeting.id)
          mainerId.value = String(newMeeting.hostUserId || '')
          currentMeeting.value = newMeeting
          currentParticipants.value = Array.isArray(createRes?.participants) ? createRes.participants : []
          consultationRoomVisible.value = true
          if (options.openVideo) {
            liveRoomShow.value = true
          }
          ElMessage.success('已成功开启应急会商室')
        } else {
          ElMessage.error('开启会议失败：未获取到会议详情')
        }
      }
    } catch (e) {
      console.error('启动或加入会商研判异常', e)
      ElMessage.error(e?.message || '进入会商室失败，请重试')
    } finally {
      isActionLoading.value = false
    }
  }

  /**
   * 中途邀请新成员加入会议
   * @param {string[]} userIds 新邀请的用户 ID 数组
   */
  const inviteNewUsers = async (userIds = []) => {
    const myUserId = String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
    const cleanIds = Array.from(new Set(userIds.map(String).filter((id) => Boolean(id) && id !== myUserId)))
    if (!cleanIds.length) {
      ElMessage.warning('请选择需要邀请的用户')
      return false
    }
    if (!meetingId.value) {
      ElMessage.error('当前无有效会议')
      return false
    }

    try {
      await addMeetingParticipants(meetingId.value, cleanIds)
      ElMessage.success('已成功发送邀请')
      await fetchMeetingDetail(meetingId.value)
      return true
    } catch (err) {
      console.error('中途邀请用户失败', err)
      ElMessage.error(err?.message || '邀请用户失败，请重试')
      return false
    }
  }

  /**
   * 结束当前全员会议（仅主持人）
   */
  const endCurrentMeeting = async () => {
    if (!meetingId.value) return false
    try {
      await endMeeting(meetingId.value)
      ElMessage.success('会议已成功结束')
      handleMeetingClosed()
      return true
    } catch (err) {
      console.error('结束会议失败', err)
      ElMessage.error(err?.message || '结束会议失败，请重试')
      return false
    }
  }

  /**
   * 响应消息通知的入会请求
   */
  const handleOpenFromNotice = (data) => {
    const targetId = data?.meetingId ? String(data.meetingId) : ''
    if (targetId) {
      fetchMeetingDetail(targetId)
    }
  }

  /**
   * 响应会议结束消息
   */
  const handleRemoteMeetingClosed = (data) => {
    if (!data?.meetingId || String(data.meetingId) === String(meetingId.value)) {
      handleMeetingClosed()
    }
  }

  onMounted(() => {
    eventBus.on('OPEN_MEETING_FROM_NOTICE', handleOpenFromNotice)
    eventBus.on(EventKey.CLOSE_MEETING, handleRemoteMeetingClosed)
  })

  onBeforeUnmount(() => {
    eventBus.off('OPEN_MEETING_FROM_NOTICE', handleOpenFromNotice)
    eventBus.off(EventKey.CLOSE_MEETING, handleRemoteMeetingClosed)
  })

  /**
   * 收起/关闭应急会商室面板（最小化）
   */
  const foldConsultationRoom = () => {
    consultationRoomVisible.value = false
  }

  /**
   * 打开应急会商室面板
   */
  const openConsultationRoom = () => {
    if (meetingId.value) {
      consultationRoomVisible.value = true
    }
  }

  /**
   * 打开视频会议房间
   */
  const openLiveRoom = () => {
    if (meetingId.value) {
      liveRoomShow.value = true
    }
  }

  /**
   * 会议结束或退出时的状态彻底清理
   */
  const handleMeetingClosed = () => {
    meetingId.value = ''
    mainerId.value = ''
    currentMeeting.value = null
    currentParticipants.value = []
    consultationRoomVisible.value = false
    liveRoomShow.value = false
  }

  return {
    meetingId,
    mainerId,
    currentMeeting,
    currentParticipants,
    isMainer,
    consultationRoomVisible,
    liveRoomShow,
    isActionLoading,
    startOrJoinConsultation,
    checkAndJoinActiveMeeting,
    inviteNewUsers,
    endCurrentMeeting,
    fetchMeetingDetail,
    foldConsultationRoom,
    openConsultationRoom,
    openLiveRoom,
    handleMeetingClosed,
  }
}

export default useConsultation
