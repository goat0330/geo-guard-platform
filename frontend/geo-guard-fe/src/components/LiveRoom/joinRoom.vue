<template>
  <div v-show="visible" class="overlay">
    <div class="join-card">
      <!-- 关闭按钮 -->
      <div class="card-header">
        加入会议
        <i class="iconfont icon-close" @click="handleClose"></i>
      </div>

      <el-form :model="form" label-position="top">
        <!-- 摄像头预览区域 -->
        <div
          v-loading="loading"
          class="avatar-box"
          element-loading-text="开启摄像头中"
          element-loading-custom-class="loading-class"
          element-loading-background="rgba(0, 0, 0, 0.2)"
        >
          <video
            v-show="videoTrack"
            ref="videoRef"
            autoplay
            playsinline
          ></video>
          <el-icon v-show="!videoTrack && !loading" size="80" color="#999">
            <User />
          </el-icon>
        </div>
        <audio ref="audioRef" autoplay></audio>

        <!-- 控制区 -->
        <div class="control-row">
          <div class="control-row-title">麦克风：</div>
          <el-dropdown placement="bottom-end" trigger="click" @command="handleMicDevice">
            <el-button class="control-btn">
              <div class="control-btn-box">
                <div class="left-control-btn">
                  <el-icon @click.stop="toggleMic">
                    <i v-if="form.micOn" class="iconfont icon-micro-on"></i>
                    <i v-else class="iconfont icon-micro-off"></i>
                  </el-icon>
                  <span>{{ form.micDevice?.label ? form.micDevice?.label : '请选择麦克风' }}</span>
                </div>
                <el-icon class="arrow">
                  <ArrowDown />
                </el-icon>
              </div>
            </el-button>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in micList" :key="item.deviceId" :command="item">
                  {{ item?.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <div class="control-row-title">摄像头：</div>
          <el-dropdown placement="bottom-end" trigger="click" @command="handleCameraDevice">
            <el-button type="primary" class="control-btn">
              <div class="control-btn-box">
                <div class="left-control-btn">
                  <el-icon @click.stop="toggleCamera">
                    <i v-if="form.cameraOn" class="iconfont icon-camara-on"></i>
                    <i v-else class="iconfont icon-camare-off"></i>
                  </el-icon>
                  <span>{{ form.cameraDevice?.label ? form.cameraDevice?.label : '请选择摄像头' }}</span>
                </div>
                <el-icon class="arrow">
                  <ArrowDown />
                </el-icon>
              </div>
            </el-button>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in cameraList" :key="item.deviceId" :command="item">
                  {{ item?.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <!-- 加入会议操作按钮 -->
        <el-button type="primary" class="join-btn" @click="joinMeeting"> 加入会议 </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { inject, ref, watch } from 'vue'
import { ArrowDown, User } from '@element-plus/icons-vue'
import { createLocalAudioTrack, createLocalVideoTrack } from 'livekit-client'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  cameraList: {
    type: Array,
    default: () => [],
  },
  micList: {
    type: Array,
    default: () => [],
  },
})

const emits = defineEmits(['joinMeeting', 'closeDialog', 'refreshDevices'])

const form = inject('form')
const loading = ref(false)

// 开关
const toggleMic = () => {
  form.value.micOn = !form.value.micOn
  if (form.value.micOn) {
    startMic()
  } else {
    stopMic()
  }
}

const toggleCamera = () => {
  form.value.cameraOn = !form.value.cameraOn
  if (form.value.cameraOn) {
    startCamera()
  } else {
    stopCamera()
  }
}

// 设备选择
const handleMicDevice = (val) => {
  form.value.micDevice = val
  startMic()
}

const handleCameraDevice = (val) => {
  form.value.cameraDevice = val
  startCamera()
}

const handleClose = () => {
  emits('closeDialog')
}

// 进行预览
const videoRef = ref(null)
const audioRef = ref(null)
const videoTrack = ref(null)
const audioTrack = ref(null)

const startCamera = async () => {
  loading.value = true
  form.value.cameraOn = true
  if (videoTrack.value) {
    destroyCamera()
  }

  try {
    videoTrack.value = await createLocalVideoTrack({
      deviceId: form.value.cameraDevice?.deviceId,
    })
    videoTrack.value.attach(videoRef.value)
    emits('refreshDevices', () => {
      if (!form.value.cameraDevice && props.cameraList?.length) {
        form.value.cameraDevice = props.cameraList[0]
      }
    })
  } catch (err) {
    console.error('startCamera error:', err)
  }

  loading.value = false
  if (!props.visible) {
    destroyCamera()
  }
}

const stopCamera = () => {
  form.value.cameraOn = false
  destroyCamera()
}

const startMic = async () => {
  form.value.micOn = true
  if (audioTrack.value) {
    destroyMic()
  }
  try {
    audioTrack.value = await createLocalAudioTrack({
      deviceId: form.value.micDevice?.deviceId,
    })

    audioTrack.value.attach(audioRef.value)
    emits('refreshDevices', () => {
      if (!form.value.micDevice && props.micList?.length) {
        form.value.micDevice = props.micList[0]
      }
    })
  } catch (err) {
    console.error('startMic error:', err)
  }

  if (!props.visible) {
    destroyMic()
  }
}

const stopMic = () => {
  form.value.micOn = false
  if (!audioTrack.value) return
  destroyMic()
}

const destroyMic = () => {
  if (!audioTrack.value) return
  audioTrack.value?.mediaStreamTrack?.stop()
  audioTrack.value.stop()
  audioTrack.value.detach()
  audioTrack.value = null
}

const destroyCamera = () => {
  if (!videoTrack.value) return
  videoTrack.value?.mediaStreamTrack?.stop()
  videoTrack.value.stop()
  videoTrack.value.detach()
  videoTrack.value = null
}

const joinMeeting = () => {
  destroyMic()
  destroyCamera()
  emits('joinMeeting')
}

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      await startCamera()
      await startMic()
    } else {
      destroyMic()
      destroyCamera()
    }
  },
  { deep: true },
)
</script>

<style scoped lang="less">
:deep(.loading-class .el-loading-spinner) {
  top: 35% !important;
}

.overlay {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10001;
}

.join-card {
  padding: 40px;
  width: 542px;
  background: #ffffff;
  border-radius: 12px;
  box-sizing: border-box;
  position: relative;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #222527;
    font-weight: 700;
    font-size: 18px;
    margin-bottom: 20px;

    .icon-close {
      cursor: pointer;
      font-size: 18px;
      color: #617185;
      transition: color 0.2s;

      &:hover {
        color: #222527;
      }
    }
  }
}

.avatar-box {
  height: 200px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  overflow: hidden;
  background: #000000;

  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.control-row {
  display: flex;
  gap: 12px;
  margin-bottom: 40px;
  flex-direction: column;

  .control-row-title {
    color: #222527;
    font-size: 14px;
    font-weight: 500;
  }
}

.control-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  color: #617185;
  display: flex;
  justify-content: flex-start;
  border: 1px solid #d4d8dd;
  background: #ffffff;

  .iconfont {
    color: #007BFF;
  }

  :deep(& > span) {
    flex: 1;
  }

  .control-btn-box {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .left-control-btn {
      display: flex;
      align-items: center;
      gap: 4px;

      span {
        width: 376px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        text-align: left;
      }
    }

    .right-control-btn {
      width: 40px;
      height: 100%;
    }
  }
}

.join-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #007BFF;
  border-color: #007BFF;

  &:hover {
    background: #0069d9;
    border-color: #0069d9;
  }

  :deep(& > span) {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
