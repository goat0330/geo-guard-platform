<template>
  <teleport to="body">
    <div v-show="liveRoomShow" class="live-room" :style="{ zIndex: zIndex }" @click="leaveShow = false">
      <div class="live-room-stage" :class="{ 'is-fullscreen': isFullScreen }">
        <template v-if="hasJoinRoom">
          <div
            ref="roomRef"
            class="live-list"
            :class="{
              'live-list-full': isFullScreen,
              'with-smart-minutes': smartMinutesVisible && !isFullScreen,
            }"
            v-loading="meetingLoading"
            element-loading-custom-class="live-list-loading"
            element-loading-text="正在连接会议，请稍候..."
            element-loading-background="rgba(255, 255, 255, 0.8)"
            @click.stop
          >
          <!-- 会议头部 -->
          <div class="live-header">
            <div class="header-title">
              应急会商室
              <div class="header-time">{{ currentTime }}</div>
              <SignalStrength :model-value="4" class-name="header-signal" />
            </div>
            <div class="content"></div>
            <div class="operate-list">
              <!-- 点开直接多选的加人功能（宫格布局左侧） -->
              <InviteMemberPopover
                :meeting-id="meetingId"
                :existing-member-ids="existingMemberIds"
                :host-user-id="hostUserId"
                :is-host="isMainer"
                @success="handleInviteSuccess"
              />

              <div class="operate-select">
                <div
                  class="operate-select-item"
                  :class="{ 'operate-select-item-active': layer === 1 }"
                  @click="layer = 1"
                >
                  宫格布局
                </div>
                <div
                  class="operate-select-item"
                  :class="{ 'operate-select-item-active': layer === 2 }"
                  @click="layer = 2"
                >
                  右侧人员布局
                </div>
              </div>
              <div class="folder-icon" @click="folderRoom">
                <i class="iconfont icon-a-Full-screen-playquanpingbofang"></i>
                收起
              </div>
              <div class="full-screen" @click.stop="fullScreen">
                <i
                  class="iconfont"
                  :class="{ 'icon-a-Full-screenquanjufangda': !isFullScreen, 'icon-fold': isFullScreen }"
                ></i>
              </div>
            </div>
          </div>

          <!-- 说话提示 -->
          <div v-if="speakingNames.length" class="say-box">
            正在说话：{{ speakingNames.join('、') }}
          </div>

          <!-- 布局 1：宫格平铺 -->
          <div v-if="layer === 1" class="live-room-list">
            <div
              v-for="p in participantList"
              :key="p.identity"
              v-loading="p._loading"
              element-loading-custom-class="camara-loading"
              element-loading-text="开启摄像头中"
              class="live-room-item"
              :class="{
                'live-room-item-active': p.identity === activeUserId,
                'live-room-item-video': p._hasVideo,
                'live-room-item-share': p._hasShare,
              }"
              @click="chooseUser(p)"
            >
              <div class="avatar">{{ (getParticipantName(p)).slice(0, 1) }}</div>
              <div class="room-video" :ref="(el) => setVideoRef(el, p.identity)"></div>
              <div class="live-room-item-foot">
                <SignalStrength :model-value="4" class-name="signal-green" />
                <div class="expert-name">
                  <i
                    class="iconfont"
                    :class="p._hasAudio ? 'icon-micro-on' : 'icon-micro-off'"
                    :style="{ color: p._speaking ? '#01FC94' : undefined }"
                    @click.stop="toggleRemoteMic(p)"
                  ></i>
                  {{ getParticipantName(p) }}
                </div>
              </div>
            </div>
          </div>

          <!-- 布局 2：主画面 + 右侧人员列表 -->
          <div v-else class="right-live-list">
            <div class="live-screen">
              <div v-show="selectUserPlay && selectUser" ref="leftVideoRef" class="live-screen-video"></div>
              <div v-if="selectUser" class="live-screen-name">{{ getParticipantName(selectUser) }}</div>
              <div v-if="!selectUserPlay && selectUser" class="screen-avatar">
                {{ (getParticipantName(selectUser)).slice(0, 1) }}
              </div>
            </div>
            <div class="live-user-list">
              <div
                v-for="p in participantList"
                :key="p.identity"
                v-loading="p._loading"
                element-loading-custom-class="side-camera-loading"
                element-loading-text="开启摄像头中"
                class="live-room-item"
                :class="{
                  'live-room-item-active': p.identity === activeUserId,
                  'live-room-item-video': p._hasVideo,
                  'live-room-item-share': p._hasShare,
                }"
                @click="chooseUser(p)"
              >
                <div class="avatar">{{ (getParticipantName(p)).slice(0, 1) }}</div>
                <div class="room-video" :ref="(el) => setVideoRef(el, p.identity)"></div>
                <div class="live-room-item-foot">
                  <SignalStrength :model-value="4" class-name="signal-green" />
                  <div class="expert-name">
                    <i
                      class="iconfont"
                      :class="p._hasAudio ? 'icon-micro-on' : 'icon-micro-off'"
                      :style="{ color: p._speaking ? '#01FC94' : undefined }"
                      @click.stop="toggleRemoteMic(p)"
                    ></i>
                    {{ getParticipantName(p) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部控制栏 -->
          <div class="live-foot">
            <div class="live-foot-list">
              <!-- 麦克风控制 -->
              <el-dropdown class="foot-device-dropdown" placement="top" trigger="click" @command="handleMicDevice">
                <div class="foot-device-control">
                  <div class="foot-btn" :title="form.micDevice?.label || '请选择麦克风'" @click.stop="toggleMic">
                    <i class="iconfont" :class="form.micOn ? 'icon-micro-on' : 'icon-micro-off'"></i>
                    {{ form.micOn ? '静音' : '解除静音' }}
                  </div>
                  <div class="foot-device-arrow">
                    <el-icon>
                      <ArrowDown />
                    </el-icon>
                  </div>
                </div>

                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="(item, index) in micList"
                      :key="item.deviceId || index"
                      :command="item"
                    >
                      {{ getDeviceLabel(item, index, '麦克风') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="!micList.length" disabled>暂无麦克风设备</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- 摄像头控制 -->
              <el-dropdown class="foot-device-dropdown" placement="top" trigger="click" @command="handleCameraDevice">
                <div class="foot-device-control">
                  <div class="foot-btn" :title="form.cameraDevice?.label || '请选择摄像头'" @click.stop="toggleCamera">
                    <i class="iconfont" :class="form.cameraOn ? 'icon-camara-on' : 'icon-camare-off'"></i>
                    {{ form.cameraOn ? '停止视频' : '开启视频' }}
                  </div>
                  <div class="foot-device-arrow">
                    <el-icon>
                      <ArrowDown />
                    </el-icon>
                  </div>
                </div>

                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="(item, index) in cameraList"
                      :key="item.deviceId || index"
                      :command="item"
                    >
                      {{ getDeviceLabel(item, index, '摄像头') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="!cameraList.length" disabled>暂无摄像头设备</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- 屏幕共享 -->
              <div class="foot-btn" @click.stop="toggleScreenShare">
                <i class="iconfont icon-a-Laptop-computerbijibendiannao"></i>
                {{ isScreenSharing ? '停止共享' : '分享屏幕' }}
              </div>

              <!-- 音量调节 -->
              <div class="foot-btn" @click.stop="toggleVoiceChange">
                <VoiceChange
                  v-if="showVoiceChange"
                  v-model:model-value="voiceNum"
                  @change="changeVoice"
                />
                <i class="iconfont" :class="form.voiceOn ? 'icon-a-Volume-noticeshengyin-da' : 'icon-a-Volume-noticeshengyin-da1'"></i>
                音量
              </div>

              <!-- 智能纪要与右侧抽屉联动 -->
              <button
                type="button"
                class="foot-btn toolbar-minutes"
                :class="{ active: smartMinutesVisible }"
                title="智能纪要"
                @click.stop="smartMinutesVisible = !smartMinutesVisible"
              >
                <img :src="minutesIcon" alt="" />
                智能纪要
              </button>
            </div>

            <!-- 离开/结束会议 -->
            <div class="leave-room">
              <div class="foot-btn" @click.stop="beforeLeave">
                <div v-show="leaveShow" class="leave-btn" @click.stop="leaveMeeting">
                  <el-button type="danger" class="inner-box">离开会议</el-button>
                </div>
                <i class="iconfont icon-a-Logouttuichu"></i>
                离开会议
              </div>
              <div v-if="isMainer" class="foot-btn" @click.stop="endingMeeting">
                <i class="iconfont icon-ETL-jieshu"></i>
                结束会议
              </div>
            </div>
          </div>
        </div>
      </template>

        <SmartMinutesDrawer
          v-model:visible="smartMinutesVisible"
          :meeting-id="meetingId"
          :mainer-id="mainerId"
          :is-full-screen="isFullScreen"
          :live-transcripts="liveTranscripts"
          :load-history="loadHistoricalTranscripts"
          @click.stop
        />
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createLocalAudioTrack, createLocalVideoTrack, Room, RoomEvent } from 'livekit-client'
import { getMeetingToken, endMeeting, getMeetingDetail, addMeetingParticipants } from '@/api/meeting.js'
import { noticeUsers } from '@/api/chat.js'
import { useUserStore } from '@/store/user.js'
import { storeToRefs } from 'pinia'
import { eventBus, EventKey, NOTICE_KEY } from '@/utils/eventBus.js'
import { useNow } from '@/hooks/useNow.js'

import SignalStrength from '@/components/SignalStrength/index.vue'
import VoiceChange from './VoiceChange.vue'
import SmartMinutesDrawer from '@/components/SmartMinutesDrawer/index.vue'
import InviteMemberPopover from '@/components/InviteMemberPopover/index.vue'
import { useLiveMeetingTranscript } from '@/composables/useLiveMeetingTranscript.js'
import minutesIcon from '@/assets/imgs/chatRoom/zhineng.svg?url'

const props = defineProps({
  meetingId: {
    type: String,
    default: '',
  },
  mainerId: {
    type: [String, Number],
    default: '',
  },
  liveRoomShow: {
    type: Boolean,
    default: false,
  },
  participants: {
    type: Array,
    default: () => [],
  },
  zIndex: {
    type: [Number, String],
    default: 1000,
  },
})

const emits = defineEmits(['update:liveRoomShow', 'meetingClosed', 'meetingEnded', 'open-record'])

const REMOTE_AUDIO = 'audio-remote'
const { currentTime } = useNow('HH:mm:ss')
const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const roomRef = ref()
const room = ref(null)
const liveKitInfo = ref(null)
const hasJoinRoom = ref(false)
const meetingLoading = ref(false)
const isJoining = ref(false)
const isManualLeaving = ref(false)
const currentMeetingDetail = ref(null)
const isFullScreen = ref(false)
const layer = ref(2)
const activeUserId = ref(null)
const participantMap = ref(new Map())
const leftVideoRef = ref(null)
const mainAttachedTracks = ref([])
const isScreenSharing = ref(false)
const isCloseMeeting = ref(false)
const showVoiceChange = ref(false)
const smartMinutesVisible = ref(false)

// 发言人姓名动态解析
const getParticipantDisplayName = (userId) => {
  const uid = String(userId || '')
  if (!uid) return '未识别发言人'
  const p = participantMap.value?.get(uid)
  if (p?.name) return p.name
  const detailUser = currentMeetingDetail.value?.participants?.find(
    (item) => String(item.userId) === uid,
  )
  if (detailUser?.name) return detailUser.name
  const myId = String(userInfo.value?.user?.userId || userInfo.value?.userId || '')
  if (uid === myId) {
    return userInfo.value?.user?.userName || userInfo.value?.userName || '我'
  }
  return '参会人'
}

// 接入实时字幕流
const {
  liveTranscripts,
  setupTranscriptStream,
  cleanupTranscriptStream,
  clearTranscripts,
  loadHistoricalTranscripts,
  refreshSpeakerNames,
} = useLiveMeetingTranscript({
  meetingId: computed(() => props.meetingId),
  getParticipantName: getParticipantDisplayName,
})

// 当会议详情参会人更新时，刷新已有字幕的发言人姓名
watch(
  () => currentMeetingDetail.value?.participants,
  () => {
    refreshSpeakerNames()
  },
  { deep: true },
)

// 中途邀请人员现有成员过滤（仅保留 status === 'ACTIVE' 的有效在会成员）
const existingMemberIds = computed(() => {
  const ids = new Set()
  const list = currentMeetingDetail.value?.participants || props.participants || []
  if (Array.isArray(list)) {
    list.forEach((p) => {
      if (p?.userId && p.status === 'ACTIVE') {
        ids.add(String(p.userId))
      }
    })
  }
  return Array.from(ids)
})

const hostUserId = computed(() => {
  return String(currentMeetingDetail.value?.meeting?.hostUserId || props.mainerId || '')
})

const handleInviteSuccess = async () => {
  if (props.meetingId) {
    const detailRes = await getMeetingDetail(props.meetingId)
    if (detailRes) {
      currentMeetingDetail.value = detailRes
    }
  }
}

const form = ref({
  micOn: true,
  cameraOn: true,
  micDevice: null,
  cameraDevice: null,
  voiceOn: true,
})
provide('form', form)

const cameraList = ref([])
const micList = ref([])
let audioTrack = null
let videoTrack = null
const remoteAudioElements = new Map()

// 转写服务会以 LiveKit 机器人身份入会，仅用于采集音频，不应作为真实参会人展示。
const isTranscriberBot = (participant) => {
  const identity = String(participant?.identity || '').toLowerCase()
  return identity === 'meeting-transcriber' || identity.includes('transcriber')
}

const getLocalParticipant = () => {
  return participantMap.value.get(String(userInfo.value?.user?.userId || ''))
}

const syncLocalParticipantAudioState = () => {
  const participant = getLocalParticipant()
  if (participant) {
    updateParticipantVideoState(participant)
  }
}

const voiceNum = ref(100)
const changeVoice = (val) => {
  voiceNum.value = val
  form.value.voiceOn = val > 0
  syncRemoteAudioVolume()
}

const toggleVoiceChange = () => {
  showVoiceChange.value = !showVoiceChange.value
}

const syncRemoteAudioVolume = () => {
  const volume = Math.min(1, Math.max(0, voiceNum.value / 100))
  remoteAudioElements.forEach((audioList) => {
    audioList.forEach((audioEl) => {
      audioEl.volume = volume
      audioEl.muted = volume === 0
    })
  })
}

const getDeviceLabel = (device, index, prefix) => {
  return device?.label || `${prefix}${index + 1}`
}

const isMainer = computed(() => {
  const myUserId = String(userStore.userInfo?.user?.userId || '')
  if (!myUserId) return false
  const hostId = currentMeetingDetail.value?.meeting?.hostUserId || props.mainerId
  if (hostId && String(hostId) === myUserId) return true
  const myParticipant = currentMeetingDetail.value?.participants?.find(
    (p) => String(p.userId) === myUserId,
  )
  return myParticipant?.role === 'HOST'
})

const getParticipantName = (p) => {
  if (p?.name) return p.name
  const matched = currentMeetingDetail.value?.participants?.find(
    (item) => String(item.userId) === String(p?.identity),
  )
  if (matched?.name) return matched.name
  if (String(p?.identity) === String(userStore.userInfo?.user?.userId)) {
    return userStore.userInfo?.user?.nickName || userStore.userInfo?.user?.userName || '我'
  }
  return '参会人'
}

const participantList = computed(() => {
  return Array.from(participantMap.value.values())
})

const speakingNames = computed(() => {
  return participantList.value
    .filter((p) => p._speaking)
    .map((p) => getParticipantName(p))
    .filter(Boolean)
})

const selectUser = computed(() => {
  return participantMap.value?.get(String(activeUserId.value || '')) || null
})

const selectUserPlay = computed(() => {
  return Array.from(selectUser.value?.trackPublications?.values() || []).some((pub) => {
    return pub.kind === 'video' && !pub.isMuted
  })
})

const chooseUser = (p) => {
  activeUserId.value = String(p.identity)
  attachMainVideo(String(p.identity))
}

const folderRoom = async () => {
  emits('update:liveRoomShow', false)
}

const fullScreen = async () => {
  isFullScreen.value = !isFullScreen.value
}

watch(
  () => props.liveRoomShow,
  async (visible) => {
    if (!visible || hasJoinRoom.value || isJoining.value) return
    // 不展示设备预览，直接由浏览器/系统选择默认摄像头和麦克风。
    try {
      await getDevices()
    } catch (error) {
      console.warn('获取默认音视频设备失败，将直接尝试加入会议', error)
    }
    await joinVMeeting()
  },
)

watch(layer, async (val) => {
  if (val === 1) {
    destroyMainVideo()
  }

  if (val === 2 && activeUserId.value) {
    await nextTick()
    attachMainVideo(activeUserId.value)
  }
})

const toggleMic = async () => {
  form.value.micOn = !form.value.micOn

  if (!room.value) return

  try {
    if (!audioTrack && form.value.micOn) {
      audioTrack = await createLocalAudioTrack({
        deviceId: form.value.micDevice?.deviceId || undefined,
      })
      await room.value.localParticipant.publishTrack(audioTrack)
    }

    if (!audioTrack) return

    if (form.value.micOn) {
      await audioTrack.unmute()
    } else {
      await audioTrack.mute()
    }

    syncLocalParticipantAudioState()
  } catch (e) {
    console.warn('切换麦克风异常', e)
    form.value.micOn = false
    ElMessage.warning('麦克风操作失败，请检查权限和设备；仍可观看会议')
  }
}

const toggleCamera = async () => {
  form.value.cameraOn = !form.value.cameraOn
  if (!room.value) return

  const myId = String(userInfo.value?.user?.userId || '')
  const participant = participantMap.value.get(myId)
  if (participant && form.value.cameraOn) {
    participant._loading = true
  }

  try {
    await room.value.localParticipant.setCameraEnabled(form.value.cameraOn)
  } catch (e) {
    console.warn('切换摄像头异常', e)
    form.value.cameraOn = false
    ElMessage.warning('摄像头操作失败，请检查权限和设备；仍可观看会议')
  } finally {
    if (participant) {
      participant._loading = false
      updateParticipantVideoState(participant)
    }
  }
}

const toggleRemoteMic = async (participant) => {
  const userId = String(participant.identity)
  const myUserId = String(userInfo.value?.user?.userId || '')

  if (userId === myUserId) {
    await toggleMic()
    return
  }

  if (myUserId !== String(props.mainerId) || !participant._hasAudio) {
    return
  }

  await noticeUsers({
    userIds: [userId],
    type: NOTICE_KEY.REMOTE_MUTE,
    data: {
      type: REMOTE_AUDIO,
      value: false,
      meetingId: String(props.meetingId),
      userId,
    },
  })
}

const handleMicDevice = async (val) => {
  form.value.micDevice = val

  if (!room.value) return

  if (audioTrack) {
    await room.value.localParticipant.unpublishTrack(audioTrack)
    audioTrack.stop()
    audioTrack = null
  }

  try {
    audioTrack = await createLocalAudioTrack({
      deviceId: form.value.micDevice?.deviceId || undefined,
    })

    if (!form.value.micOn) {
      await audioTrack.mute()
    }

    await room.value.localParticipant.publishTrack(audioTrack)

    if (!form.value.micOn) {
      await audioTrack.mute()
    }

    syncLocalParticipantAudioState()
  } catch (e) {
    console.warn('切换麦克风设备失败', e)
    form.value.micOn = false
    ElMessage.warning('麦克风设备操作失败，请检查权限')
  }
}

const handleCameraDevice = async (val) => {
  form.value.cameraDevice = val

  if (!room.value) return

  const myId = String(userInfo.value?.user?.userId || '')
  const participant = participantMap.value.get(myId)
  if (participant && form.value.cameraOn) {
    participant._loading = true
  }

  if (videoTrack) {
    await room.value.localParticipant.unpublishTrack(videoTrack)
    videoTrack.stop()
    videoTrack = null
  }

  try {
    videoTrack = await createLocalVideoTrack({
      deviceId: form.value.cameraDevice?.deviceId,
    })
    await room.value.localParticipant.publishTrack(videoTrack)

    if (!form.value.cameraOn || isScreenSharing.value) {
      await room.value.localParticipant.setCameraEnabled(false)
    }
  } catch (e) {
    console.warn('切换摄像头设备失败', e)
    form.value.cameraOn = false
    ElMessage.warning('摄像头设备操作失败，请检查权限')
  } finally {
    if (participant) {
      participant._loading = false
      updateParticipantVideoState(participant)
    }
  }
}

const checkMediaPermission = async () => {
  try {
    const cameraPermission = await navigator.permissions.query({
      name: 'camera',
    })
    const micPermission = await navigator.permissions.query({
      name: 'microphone',
    })
    return {
      camera: cameraPermission.state,
      microphone: micPermission.state,
    }
  } catch (e) {
    return {
      camera: 'prompt',
      microphone: 'prompt',
    }
  }
}

const getDevices = async (cb) => {
  const { camera, microphone } = await checkMediaPermission()

  if (camera === 'denied') {
    ElMessage.warning('摄像头权限被拒绝')
  }
  if (microphone === 'denied') {
    ElMessage.warning('麦克风权限被拒绝')
  }

  const devices = await navigator.mediaDevices.enumerateDevices()
  cameraList.value = devices.filter((d) => d.kind === 'videoinput')
  micList.value = devices.filter((d) => d.kind === 'audioinput')

  if (cameraList.value.length && !form.value.cameraDevice) {
    form.value.cameraDevice = cameraList.value[0]
  }
  if (micList.value.length && !form.value.micDevice) {
    form.value.micDevice = micList.value[0]
  }

  if (cb && typeof cb === 'function') {
    cb()
  }
}

const joinVMeeting = async () => {
  if (!props.meetingId) {
    ElMessage.error('未获取到会议信息')
    return
  }
  if (isJoining.value) return
  isJoining.value = true
  meetingLoading.value = true

  try {
    // 1. 入会前先查验会议详情与状态
    try {
      const detailRes = await getMeetingDetail(props.meetingId)
      currentMeetingDetail.value = detailRes
      if (detailRes?.meeting?.status === 'ENDED') {
        ElMessage.warning('该会议已结束')
        folderRoom()
        return
      }
    } catch (e) {
      console.warn('获取会议详情失败，尝试直接获取凭证', e)
    }

    // 2. 调用 /meeting/meetings/{meetingId}/token 获取 LiveKit 凭证
    const tokenRes = await getMeetingToken(props.meetingId)
    const token = tokenRes?.token
    const url = tokenRes?.url
    if (!token || !url) {
      ElMessage.error('获取入会凭证失败')
      return
    }

    // 仅在当前组件内存中使用，不持久化或写入路由
    liveKitInfo.value = {
      token,
      url,
      roomName: tokenRes.roomName,
    }
    hasJoinRoom.value = true
    isManualLeaving.value = false

    // 3. 连接 LiveKit 会议室
    await connectMeeting()
  } catch (e) {
    console.error('加入视频会议失败', e)
    ElMessage.error(e?.message || '加入视频会议失败，请检查网络或会议状态')
  } finally {
    isJoining.value = false
    meetingLoading.value = false
  }
}

const leaveShow = ref(false)
const beforeLeave = () => {
  leaveShow.value = !leaveShow.value
}

const leaveMeeting = async (isPassive = false) => {
  isManualLeaving.value = !isPassive
  hasJoinRoom.value = false
  participantMap.value.clear()
  activeUserId.value = null
  leaveShow.value = false
  smartMinutesVisible.value = false
  await leaveRoom()
  removeEvents()
  folderRoom()
  emits('meetingClosed')
}

const endingMeeting = async () => {
  try {
    await ElMessageBox.confirm(
      '确定结束全员会议吗？结束后所有参会人员将自动退出。',
      '结束会议确认',
      {
        confirmButtonText: '确定结束',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    // 1. 调用后端结束会议接口
    const endedMeeting = await endMeeting(props.meetingId)
    ElMessage.success('会议已成功结束')

    emits('meetingEnded', {
      id: String(props.meetingId),
      title: endedMeeting?.meeting?.title || currentMeetingDetail.value?.meeting?.title || '',
      status: 'ENDED',
    })

    // 2. 发送自定义通知提醒其他参会人退出
    const userIds = props.participants?.map((item) => String(item.userId)) || []
    if (userIds.length) {
      noticeUsers({
        userIds,
        type: NOTICE_KEY.CLOSE_MEETING,
        data: {
          meetingId: String(props.meetingId),
        },
      }).catch((err) => console.warn('通知其他人员结束会议异常', err))
    }

    // 3. 后端成功后再清理并退出；leaveMeeting 统一通知父组件清理会议状态
    await leaveMeeting(true)
  } catch (e) {
    console.error('结束会议失败', e)
    ElMessage.error(e?.message || '结束会议失败，请重试')
  }
}

const connectMeeting = async () => {
  room.value = new Room({
    adaptiveStream: true,
    dynacast: true,
  })

  // 在 room.connect 之前挂载实时字幕短文本流监听
  setupTranscriptStream(room.value)

  const { url, token } = liveKitInfo.value
  await room.value.connect(url, token, {
    autoSubscribe: true,
  })

  // 摄像头：若开启则采集；权限被拒绝时保留仅订阅入会能力
  if (form.value.cameraOn) {
    try {
      videoTrack = await createLocalVideoTrack({
        deviceId: form.value.cameraDevice?.deviceId || undefined,
      })
      await room.value.localParticipant.publishTrack(videoTrack)
    } catch (e) {
      form.value.cameraOn = false
      console.warn('摄像头权限或设备异常', e)
      ElMessage.warning('摄像头操作失败，请检查权限和设备；仍可观看会议')
    }
  }

  // 麦克风：若开启则采集；权限被拒绝时保留仅订阅入会能力
  if (form.value.micOn) {
    try {
      audioTrack = await createLocalAudioTrack({
        deviceId: form.value.micDevice?.deviceId || undefined,
      })
      await room.value.localParticipant.publishTrack(audioTrack)
    } catch (e) {
      form.value.micOn = false
      console.warn('麦克风权限或设备异常', e)
      ElMessage.warning('麦克风操作失败，请检查权限和设备；仍可观看会议')
    }
  }

  if (!form.value.micOn && audioTrack) {
    await audioTrack.mute()
  }
  if (!form.value.cameraOn) {
    await room.value.localParticipant.setCameraEnabled(false)
  }

  // 解除浏览器自动播放限制
  try {
    await room.value.startAudio()
  } catch (e) {
    console.warn('LiveKit startAudio 提示', e)
  }

  addParticipant(room.value.localParticipant)
  room.value.remoteParticipants?.forEach((p) => {
    addParticipant(p)
  })

  // 注册事件
  room.value.on(RoomEvent.TrackSubscribed, handleVideoStream)
  room.value.on(RoomEvent.TrackUnsubscribed, handleVideoStreamRemove)
  room.value.on(RoomEvent.TrackMuted, startCameraListener)
  room.value.on(RoomEvent.TrackUnmuted, stopCameraListener)
  room.value.on(RoomEvent.LocalTrackPublished, handleLocalTrackPublished)
  room.value.on(RoomEvent.LocalTrackUnpublished, handleLocalTrackUnpublished)
  room.value.on(RoomEvent.ParticipantConnected, handlePersonIn)
  room.value.on(RoomEvent.ParticipantDisconnected, handlePersonOut)
  room.value.on(RoomEvent.ActiveSpeakersChanged, handleVoiceWave)
  room.value.on(RoomEvent.SignalReconnecting, handleSignalReconnecting)
  room.value.on(RoomEvent.Reconnecting, handleReconnecting)
  room.value.on(RoomEvent.Reconnected, handleReconnected)
  room.value.on(RoomEvent.Disconnected, handleDisconnected)
}

const handleSignalReconnecting = () => {
  ElMessage.info('会议网络信号波动，正在重新连接...')
}

const handleReconnecting = () => {
  ElMessage.info('网络连接中断，正在尝试重新连接会议...')
}

const handleReconnected = () => {
  ElMessage.success('会议连接已恢复')
}

const handleDisconnected = async () => {
  cleanUpMedia()
  if (!isManualLeaving.value && hasJoinRoom.value) {
    try {
      const detailRes = await getMeetingDetail(props.meetingId)
      if (detailRes?.meeting?.status === 'ENDED') {
        ElMessage.warning('会议已被主持人结束')
        await leaveMeeting(true)
        return
      }
      const myUserId = String(userInfo.value?.user?.userId || '')
      const myP = detailRes?.participants?.find((p) => String(p.userId) === myUserId)
      if (myP && myP.status === 'REMOVED') {
        ElMessage.warning('您已被移出该会议')
        await leaveMeeting(true)
        return
      }
      ElMessage.warning('与会议服务器断开连接，请检查网络')
    } catch (e) {
      ElMessage.warning('与会议服务器断开连接')
    }
    await leaveMeeting(true)
  }
}

const addParticipant = (participant) => {
  if (isTranscriberBot(participant)) return
  const identity = String(participant.identity)
  updateParticipantVideoState(participant, true)
  refreshSpeakerNames()
  if (!activeUserId.value) {
    activeUserId.value = identity
    nextTick(() => attachMainVideo(identity))
  }
}

const removeParticipant = (participant) => {
  if (isTranscriberBot(participant)) return
  const identity = String(participant.identity)
  const newMap = new Map(participantMap.value)
  newMap.delete(identity)
  participantMap.value = newMap

  const tracks = attachedMap.get(identity) || []
  tracks.forEach((track) => {
    try {
      track.detach()
    } catch (e) {
      console.log('解绑离会人员轨道失败', e)
    }
  })
  attachedMap.delete(identity)
  videoRefs.delete(identity)

  const audioList = remoteAudioElements.get(identity) || []
  audioList.forEach((audioEl) => audioEl.remove())
  remoteAudioElements.delete(identity)

  if (String(activeUserId.value) === identity) {
    destroyMainVideo()
    activeUserId.value = participantList.value[0]?.identity || null
    nextTick(() => {
      if (activeUserId.value) attachMainVideo(activeUserId.value)
    })
  }
}

const handlePersonIn = (participant) => {
  addParticipant(participant)
}

const handlePersonOut = (participant) => {
  removeParticipant(participant)
}

const handleVideoStream = (track, pub, participant) => {
  if (isTranscriberBot(participant)) return
  attachParticipantTracks(participant.identity)
  updateParticipantVideoState(participant)
}

const handleVideoStreamRemove = (track, pub, participant) => {
  if (isTranscriberBot(participant)) return
  track.detach()

  const idStr = String(participant.identity)
  const existing = attachedMap.get(idStr) || []
  attachedMap.set(
    idStr,
    existing.filter((t) => t !== track),
  )

  setTimeout(() => {
    updateParticipantVideoState(participant)
  }, 100)
}

const startCameraListener = async (publication, participant) => {
  if (publication.kind === 'video' || publication.kind === 'audio') {
    updateParticipantVideoState(participant)
  }
}

const stopCameraListener = async (publication, participant) => {
  if (publication.kind === 'video' || publication.kind === 'audio') {
    updateParticipantVideoState(participant)
  }
}

const startShareScreen = async (publication, participant) => {
  if (publication.source === 'screen_share') {
    updateParticipantVideoState(participant)
  }
}

const stopShareScreen = async (publication, participant) => {
  if (publication.source === 'screen_share') {
    if (isScreenSharing.value) {
      isScreenSharing.value = false
      await room.value?.localParticipant?.setScreenShareEnabled(false)
      if (form.value.cameraOn) {
        await room.value?.localParticipant?.setCameraEnabled(true)
      }
      updateParticipantVideoState(participant)
    }
  }
}

const handleLocalTrackPublished = (publication, participant) => {
  attachParticipantTracks(participant.identity)
  startShareScreen(publication, participant)
}

const handleLocalTrackUnpublished = (publication, participant) => {
  if (publication.track) {
    publication.track.detach()
    const idStr = String(participant.identity)
    const existing = attachedMap.get(idStr) || []
    attachedMap.set(
      idStr,
      existing.filter((t) => t !== publication.track),
    )
  }
  stopShareScreen(publication, participant)
}

const updateParticipantVideoState = (participant, allowInsert = false) => {
  if (isTranscriberBot(participant)) return
  const publications = Array.from(participant.trackPublications.values())
  const myUserId = String(userInfo.value?.user?.userId || '')
  const isLocalParticipant = String(participant.identity) === myUserId
  const hasVideo = publications.some(
    (pub) => pub.kind === 'video' && pub.source === 'camera' && !pub.isMuted,
  )
  const hasShare = publications.some(
    (pub) => pub.kind === 'video' && pub.source === 'screen_share' && !pub.isMuted,
  )
  const hasMicPublication = publications.some(
    (pub) => pub.kind === 'audio' && pub.source === 'microphone',
  )
  const hasAudio = isLocalParticipant
    ? form.value.micOn && hasMicPublication
    : publications.some(
        (pub) => pub.kind === 'audio' && pub.source === 'microphone' && !pub.isMuted,
      )

  participant._hasVideo = hasVideo
  participant._loading = false
  participant._hasShare = hasShare
  participant._hasAudio = hasAudio
  participant._speaking = participant._speaking || false

  const identity = String(participant.identity)
  if (!allowInsert && !participantMap.value.has(identity)) {
    return
  }

  const newMap = new Map(participantMap.value)
  newMap.set(identity, participant)
  participantMap.value = newMap

  if (String(activeUserId.value) === identity) {
    nextTick(() => attachMainVideo(identity))
  }
}

const videoRefs = new Map()
const attachedMap = new Map()

const setVideoRef = (el, identity) => {
  if (!el) return

  const idStr = String(identity)
  const oldEl = videoRefs.get(idStr)
  if (oldEl && oldEl !== el) {
    const tracks = attachedMap.get(idStr) || []
    tracks.forEach((track) => track.detach())
    attachedMap.set(idStr, [])
  }

  videoRefs.set(idStr, el)
  attachParticipantTracks(idStr)
}

const attachParticipantTracks = (identity) => {
  const idStr = String(identity)
  const participant = participantMap.value.get(idStr)
  const container = videoRefs.get(idStr)

  if (!participant || !container) return

  const existing = attachedMap.get(idStr) || []
  const myUserId = String(userInfo.value?.user?.userId || '')

  participant.trackPublications.forEach((pub) => {
    const track = pub.track
    if (!track || existing.includes(track)) return

    if (track.kind === 'video' && track.source === 'camera') {
      const el = track.attach()
      el.className = 'video-track'
      el.style.width = '100%'
      el.style.height = '100%'
      el.style.objectFit = 'cover'
      el.style.position = 'absolute'
      el.style.top = 0
      el.style.left = 0
      container.appendChild(el)
    }

    if (track.kind === 'video' && track.source === 'screen_share') {
      const el = track.attach()
      el.className = 'share-track'
      el.style.width = '100%'
      el.style.height = '100%'
      el.style.objectFit = 'cover'
      el.style.position = 'absolute'
      el.style.top = 0
      el.style.left = 0
      container.appendChild(el)
    }

    if (track.kind === 'audio') {
      if (idStr !== myUserId) {
        const el = track.attach()
        el.volume = Math.min(1, Math.max(0, voiceNum.value / 100))
        el.muted = voiceNum.value === 0

        const audioList = remoteAudioElements.get(idStr) || []
        audioList.push(el)
        remoteAudioElements.set(idStr, audioList)
      }
    }

    existing.push(track)
  })

  attachedMap.set(idStr, existing)
}

const attachMainVideo = (identity) => {
  const idStr = String(identity)
  const participant = participantMap.value.get(idStr)
  const container = leftVideoRef.value

  if (!participant || !container) return

  const publications = Array.from(participant.trackPublications.values())
  const videoPub = publications.find((pub) => {
    return pub.kind === 'video' && pub.track && pub.source === 'camera' && !pub.isMuted
  })
  const sharePub = publications.find((pub) => {
    return pub.kind === 'video' && pub.track && pub.source === 'screen_share' && !pub.isMuted
  })

  if (!videoPub?.track && !sharePub?.track) {
    container.innerHTML = ''
    return
  }

  container.innerHTML = ''
  const track = sharePub?.track || videoPub?.track
  const el = track.attach()
  el.style.width = '100%'
  el.style.height = '100%'
  el.style.objectFit = sharePub?.track ? 'contain' : 'cover'
  el.style.display = 'block'
  container.appendChild(el)
  mainAttachedTracks.value = [track]
}

const destroyMainVideo = () => {
  const container = leftVideoRef.value
  if (container) {
    container.innerHTML = ''
  }
  mainAttachedTracks.value = []
}

const toggleScreenShare = async () => {
  if (!room.value) return

  try {
    if (!isScreenSharing.value) {
      await room.value.localParticipant.setScreenShareEnabled(true)
      isScreenSharing.value = true
      if (form.value.cameraOn) {
        await room.value.localParticipant.setCameraEnabled(false)
      }
    } else {
      await room.value.localParticipant.setScreenShareEnabled(false)
      isScreenSharing.value = false
      if (form.value.cameraOn) {
        await room.value.localParticipant.setCameraEnabled(true)
      }
    }
  } catch (e) {
    console.warn('屏幕共享异常或取消', e)
    ElMessage.info('未开启屏幕共享，请检查浏览器权限或重新选择')
    isScreenSharing.value = false
  }

  const myId = String(userInfo.value?.user?.userId || '')
  const participant = participantMap.value.get(myId)
  if (participant) updateParticipantVideoState(participant)
}

const cleanUpMedia = () => {
  remoteAudioElements.forEach((audioList) => {
    audioList.forEach((audioEl) => audioEl.remove())
  })
  remoteAudioElements.clear()

  attachedMap.forEach((tracks) => {
    tracks.forEach((track) => {
      try {
        track.detach()
      } catch (e) {}
    })
  })
  attachedMap.clear()
  videoRefs.clear()
  destroyMainVideo()
}

const leaveRoom = async () => {
  if (!room.value) return

  try {
    await room.value?.localParticipant?.setMicrophoneEnabled(false)
  } catch (e) {}
  try {
    await room.value?.localParticipant?.setCameraEnabled(false)
  } catch (e) {}
  try {
    await room.value?.localParticipant?.setScreenShareEnabled(false)
  } catch (e) {}

  if (audioTrack) {
    try {
      audioTrack.mediaStreamTrack?.stop()
      audioTrack.stop()
    } catch (e) {}
    audioTrack = null
  }

  if (videoTrack) {
    try {
      videoTrack.mediaStreamTrack?.stop()
      videoTrack.stop()
    } catch (e) {}
    videoTrack = null
  }

  room.value?.localParticipant?.trackPublications.forEach((publication) => {
    const track = publication.track
    track?.mediaStreamTrack?.stop()
    track?.stop()
    publication.unpublish?.()
  })

  // 注销实时字幕流监听并清空记录
  cleanupTranscriptStream(room.value)
  clearTranscripts()

  try {
    await room.value?.disconnect()
  } catch (e) {}

  cleanUpMedia()
  room.value = null
}

const removeEvents = () => {
  if (room.value) {
    cleanupTranscriptStream(room.value)
    room.value.off(RoomEvent.ParticipantConnected, handlePersonIn)
    room.value.off(RoomEvent.ParticipantDisconnected, handlePersonOut)
    room.value.off(RoomEvent.TrackSubscribed, handleVideoStream)
    room.value.off(RoomEvent.TrackUnsubscribed, handleVideoStreamRemove)
    room.value.off(RoomEvent.TrackMuted, startCameraListener)
    room.value.off(RoomEvent.TrackUnmuted, stopCameraListener)
    room.value.off(RoomEvent.LocalTrackPublished, handleLocalTrackPublished)
    room.value.off(RoomEvent.LocalTrackUnpublished, handleLocalTrackUnpublished)
    room.value.off(RoomEvent.ActiveSpeakersChanged, handleVoiceWave)
    room.value.off(RoomEvent.SignalReconnecting, handleSignalReconnecting)
    room.value.off(RoomEvent.Reconnecting, handleReconnecting)
    room.value.off(RoomEvent.Reconnected, handleReconnected)
    room.value.off(RoomEvent.Disconnected, handleDisconnected)
  }
}

const remoteMute = async (data) => {
  const { meetingId } = data || {}
  if (String(meetingId) === String(props.meetingId)) {
    ElMessage.warning('您已被主持人静音~')
    form.value.micOn = false
    await audioTrack?.mute()
    syncLocalParticipantAudioState()
  }
}

const handleVoiceWave = (speakers) => {
  const speakingIds = speakers.map((p) => String(p.identity))
  const newMap = new Map(participantMap.value)
  newMap.forEach((p) => {
    p._speaking = speakingIds.includes(String(p.identity))
  })
  participantMap.value = newMap
}

const closeMeeting = (data) => {
  const { meetingId } = data || {}
  if (String(meetingId) === String(props.meetingId)) {
    ElMessage.warning('视频会议已结束~')
    isCloseMeeting.value = true
    leaveMeeting(true)
  }
}

onBeforeUnmount(async () => {
  await leaveMeeting()
  eventBus.off(EventKey.REMOTE_MUTE, remoteMute)
  eventBus.off(EventKey.CLOSE_MEETING, closeMeeting)
})

onMounted(() => {
  eventBus.on(EventKey.REMOTE_MUTE, remoteMute)
  eventBus.on(EventKey.CLOSE_MEETING, closeMeeting)
  if (props.liveRoomShow && !hasJoinRoom.value) {
    void (async () => {
      try {
        await getDevices()
      } catch (error) {
        console.warn('获取默认音视频设备失败，将直接尝试加入会议', error)
      }
      await joinVMeeting()
    })()
  }
})

defineExpose({
  handleMicDevice,
  handleCameraDevice,
  leaveMeeting,
})
</script>

<style scoped lang="less">
.live-room {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  overflow: hidden;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;

  .live-room-stage {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    max-width: calc(100vw - 40px);
    max-height: calc(100vh - 40px);
    transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);

    &.is-fullscreen {
      width: 100vw;
      height: 100vh;
      max-width: none;
      max-height: none;
      gap: 0;
      position: relative;
      display: block;
    }
  }

  .live-list {
    width: 1200px;
    max-width: calc(100vw - 40px);
    height: 794px;
    max-height: calc(100vh - 40px);
    background: #ffffff;
    border-radius: 20px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    position: relative;
    flex-shrink: 0;
    transition: max-width 0.3s cubic-bezier(0.25, 1, 0.5, 1);

    &.with-smart-minutes {
      max-width: calc(100vw - 321px - 56px);
    }

    .live-header {
      padding: 20px 40px;
      display: flex;
      align-items: center;

      .header-title {
        display: flex;
        align-items: center;
        color: #222527;
        font-size: 18px;
        font-weight: 700;

        .header-time {
          color: #617185;
          font-family: 'Alimama FangYuanTi VF', sans-serif;
          font-size: 16px;
          margin-left: 10px;
        }

        .header-signal {
          position: relative;
          top: -3px;
          margin-left: 14px;
          height: 16px;
        }
      }

      .content {
        flex: 1;
      }

      .operate-list {
        display: flex;
        align-items: center;
        gap: 16px;

        .operate-select {
          height: 34px;
          border-radius: 6px;
          background: #E4EBF3;
          backdrop-filter: blur(2px);
          padding: 3px 6px;
          display: flex;
          align-items: center;
          gap: 10px;

          .operate-select-item {
            height: 100%;
            border-radius: 6px;
            padding: 4px 10px;
            color: #222527;
            font-size: 12px;
            font-weight: 500;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s ease;
          }

          .operate-select-item-active {
            background: #007BFF;
            color: #ffffff;
          }
        }

        .folder-icon {
          display: flex;
          align-items: center;
          gap: 6px;
          color: #222527;
          font-size: 16px;
          cursor: pointer;
          user-select: none;

          &:hover {
            color: #007BFF;
          }
        }

        .full-screen {
          cursor: pointer;
          color: #222527;

          &:hover {
            color: #007BFF;
          }
        }
      }
    }

    .live-room-list {
      border-top: 1px solid #DFE4F0;
      border-bottom: 1px solid #DFE4F0;
      display: flex;
      align-items: center;
      align-content: center;
      justify-content: center;
      flex-wrap: wrap;
      gap: 30px;
      padding: 30px;
      overflow-y: auto;
      flex: 1;
      background: #F7F7F7;
    }

    .live-room-item {
      position: relative;
      width: 200px;
      height: 110px;
      border-radius: 4px;
      border: 2px solid transparent;
      background: #EDEDED;
      cursor: pointer;
      overflow: hidden;
      flex-shrink: 0;

      .avatar {
        position: absolute;
        top: 16px;
        left: 50%;
        transform: translate(-50%, 0);
        width: 60px;
        height: 60px;
        background: #007BFF;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        color: #ffffff;
        font-weight: 700;
      }

      .room-video {
        display: none;
        position: relative;
        width: 100%;
        height: 100%;
      }

      .live-room-item-foot {
        position: absolute;
        left: 0;
        bottom: 0;
        border-radius: 2px;
        background: rgba(0, 0, 0, 0.6);
        min-width: 0;
        max-width: 100%;
        height: 24px;
        overflow: hidden;
        color: #ffffff;
        display: flex;
        align-items: center;
        z-index: 10;

        .signal-green {
          width: 24px;
          height: 24px;
          border-radius: 2px;
          background: #2CA86E;
          margin-left: 0;
          padding: 5px;
          flex-shrink: 0;

          :deep(.signal-bar) {
            background: #ffffff !important;
          }
        }

        .expert-name {
          padding: 0 8px;
          font-size: 12px;
          color: #e1e2e3;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          .iconfont {
            font-size: 12px;
          }
        }
      }
    }

    .live-room-item-active {
      border-color: #007BFF;
    }

    .live-room-item-video,
    .live-room-item-share {
      .avatar {
        display: none;
      }

      .room-video {
        display: block;

        .video-track {
          z-index: 1;
        }

        .share-track {
          z-index: 0;
        }
      }
    }

    .live-room-item-share {
      .room-video {
        .video-track {
          z-index: 0;
        }

        .share-track {
          z-index: 1;
        }
      }
    }

    .right-live-list {
      display: flex;
      gap: 6px;
      flex: 1;
      min-height: 0;
      overflow: hidden;

      .live-screen {
        position: relative;
        flex: 1;
        border-top: 1px solid #DFE4F0;
        border-bottom: 1px solid #DFE4F0;
        background: #111111;
        overflow: hidden;

        .live-screen-video {
          width: 100%;
          height: 100%;
        }

        .live-screen-name {
          position: absolute;
          left: 0;
          bottom: 0;
          width: 100%;
          height: 40px;
          background: rgba(30, 30, 30, 0.5);
          backdrop-filter: blur(6px);
          color: #ffffff;
          font-size: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 10;
        }

        .screen-avatar {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: 100px;
          height: 100px;
          border-radius: 50%;
          background: #007BFF;
          color: #ffffff;
          font-size: 40px;
          font-weight: 700;
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }

      .live-user-list {
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 6px;
        width: 200px;
        height: 100%;
        overflow-y: auto;
        border-top: 1px solid #DFE4F0;
        border-bottom: 1px solid #DFE4F0;
        background: #F7F7F7;
      }
    }

    .live-foot {
      display: flex;
      align-items: center;
      height: 72px;
      padding: 0 50px;

      .iconfont {
        font-size: 26px;
      }

      .live-foot-list {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 28px;
        flex: 1;
        color: #617185;
      }

      .leave-room {
        display: flex;
        gap: 18px;
        color: #EB5A4A;
      }

      .foot-btn {
        position: relative;
        width: 54px;
        display: flex;
        flex-direction: column;
        align-items: center;
        cursor: pointer;
        font-size: 12px;
        font-weight: 500;
        gap: 6px;
        white-space: nowrap;
        padding: 4px 8px;
        user-select: none;
        transition: background 0.2s;

        &:hover {
          background: #E5E5E5;
          border-radius: 4px;
        }

        .leave-btn {
          position: absolute;
          bottom: 66px;
          width: 224px;
          height: 74px;
          border-radius: 10px;
          border: 1px solid #DFE4F0;
          background: #ffffff;
          box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
          z-index: 10;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;

          .inner-box {
            width: 188px;
            height: 38px;
            border-radius: 6px;
            color: #ffffff;
            font-size: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      }

      .toolbar-minutes {
        width: 60px;
        height: 55px;
        justify-content: center;
        gap: 6px;
        padding: 0;
        border: 0;
        background: transparent;
        color: #007BFF;

        img {
          width: 32px;
          height: 32px;
          object-fit: contain;
        }

        &.active {
          border-radius: 4px;
          background: rgba(0, 123, 255, 0.16);
          color: #007bff;
        }
      }

      .foot-device-dropdown {
        color: inherit;
      }

      .foot-device-control {
        display: flex;
        align-items: center;
        gap: 4px;
        outline: none;

        .foot-btn {
          min-width: 54px;
        }
      }

      .foot-device-arrow {
        width: 16px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        color: #617185;
        cursor: pointer;

        &:hover {
          background: #E4EBF3;
        }

        .el-icon {
          font-size: 14px;
        }
      }
    }

    .say-box {
      position: absolute;
      top: 80px;
      left: 50%;
      transform: translate(-50%, 0);
      border-radius: 8px;
      background: rgba(0, 0, 0, 0.6);
      padding: 12px 20px;
      min-height: 46px;
      color: #ffffff;
      font-size: 14px;
      font-weight: 500;
      z-index: 10;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .live-list-full {
    width: 100vw;
    max-width: none !important;
    height: 100vh;
    max-height: none !important;
    border-radius: 0;
    position: absolute;
    top: 0;
    left: 0;
    z-index: 1001;
  }
}

:deep(.camara-loading .el-loading-spinner) {
  top: 35% !important;
}

:deep(.side-camera-loading .el-loading-spinner) {
  top: 20% !important;
}

:deep(.side-camera-loading .el-loading-text) {
  margin-top: 6px;
  font-size: 12px !important;
}

:deep(.live-list-loading .el-loading-spinner) {
  top: 50% !important;
}
</style>
