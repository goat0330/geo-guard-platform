<template>
  <div class="input-box">
    <div v-if="fileList.length" class="upload-preview-list">
      <div
        v-for="file in fileList"
        :key="file.uid || file.uploadFileId || file.name"
        class="upload-preview-item is-file"
        :class="{
          'is-uploading': file.status === 'uploading',
          'is-error': file.status === 'fail',
        }"
      >
        <img :src="file.status === 'uploading' ? fileUploadingIcon : fileUploadedIcon" class="upload-file-icon" />

        <div class="upload-file-info">
          <div class="upload-file-name" :title="file.name">
            {{ file.name }}
          </div>

          <div class="upload-file-status">
            <el-icon v-if="file.status === 'fail'" class="upload-error-icon">
              <WarningFilled />
            </el-icon>

            <template v-else>
              {{ getUploadStatusText(file) }}
            </template>
          </div>
        </div>

        <button class="upload-remove-btn" type="button" @click="removeUploadFile(file)">
          <i class="iconfont icon-close" />
        </button>
      </div>
    </div>

    <el-input
      v-model="val"
      type="textarea"
      resize="none"
      placeholder="点击左侧地图中任意斜坡单元，我会帮您分析该单元的风险等级、致险因素及处置建议~"
      @keydown="keydown"
      @paste="handlePaste"
    />
    <div class="foot-operate">
<!--      <button-->
<!--        v-if="useParseFile && hasAuth(['geo:fyxy:scyjbg'])"-->
<!--        class="upload-pdf-btn"-->
<!--        type="button"-->
<!--        @click="chooseFile"-->
<!--      >-->
<!--        <i class="iconfont icon-file"></i>-->
<!--        上传预警报告-->
<!--      </button>-->
      <input
        ref="fileInputRef"
        class="hidden-file-input"
        type="file"
        accept=".pdf,.doc,.docx"
        @change="handleFileSelect"
      />
      <div style="flex: 1"></div>
<!--      <div class="deep-think" :class="{ 'deep-think-active': deepThink }" @click="deepThink = !deepThink">-->
<!--        <i class="iconfont icon-deep-think"></i>-->
<!--        深度思考-->
<!--      </div>-->
      <div v-if="isVoice" class="live-voice">
        <div class="live-voice-inner"></div>
        <img class="live-voice-img voice" src="@/assets/imgs/chatBox/inputBox/voice-white.svg?url" @click="shutdownMike" />
      </div>
      <div v-if="!isVoice && mikeLauncing" class="is-loading"></div>
      <div v-if="!isVoice && !mikeLauncing" class="normal-voice">
        <img class="voice" src="@/assets/imgs/chatBox/inputBox/voice.svg?url" @click="openMike" />
      </div>
      <div class="split-line"></div>
      <div v-if="isChat && isAnswering" class="submit" @click="stop">
        <img src="@/assets/imgs/chatBox/inputBox/stop.png" />
      </div>
      <div v-else class="submit" @click="submit">
        <img src="@/assets/imgs/chatBox/inputBox/up.svg?url" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { useWakeAudio } from '@/hooks/useWakeAudio.js'
import { useChatStore } from '@/store/chat.js'
import { storeToRefs } from 'pinia'
import { uploadFile } from '@/api/common.js'
import fileUploadingIcon from '@/assets/imgs/chatEngine/file-uploading.png'
import fileUploadedIcon from '@/assets/imgs/chatEngine/file-uploaded.png'
import { eventBus, EventKey } from '@/utils/eventBus.js'
import { hasAuth } from '@/utils/index.js'

const val = ref('')
const fileInputRef = ref()
const route = useRoute()

const props = defineProps({
  isAnswering: {
    type: Boolean,
    default: false
  },
  isChat: {
    type: Boolean,
    default: false
  }
})

const emits = defineEmits(['submit', 'stop'])
const audioText = ref('')

const isVoice = ref(false)
const wakeAudioActive = computed(() => route.meta.notUseChat !== true && !props.isAnswering)
const { audio } = useWakeAudio({
  textRef: audioText,
  statusRef: isVoice,
  activeRef: wakeAudioActive,
  autoStopCb: () => submit(),
})

const removeLeadingPunctuation = (text = '') => text.replace(/^[\p{P}\p{Z}\s]+/gu, '')

const chatStore = useChatStore()
const { useParseFile, uploadFiles: fileList } = storeToRefs(chatStore)

const submit = () => {
  if (props.isAnswering) return

  const hasUploading = fileList.value.some((file) => file.status === 'uploading')
  if (hasUploading) {
    ElMessage.warning('文件上传中')
    return
  }

  const hasFail = fileList.value.some((file) => file.status === 'fail')
  if (hasFail) {
    ElMessage.warning('请删除上传失败的文件')
    return
  }

  const files = fileList.value.map((file) => getSubmitFile(file)).filter(Boolean)
  audio.stop()
  emits('submit', val.value, { files })
  audio.clearRecognizedText()
  val.value = ''
  fileList.value = []
}

const stop = () => {
  emits('stop')
}

const keydown = (e) => {
  if (e.keyCode === 13 && !e.shiftKey) {
    e.preventDefault()
    submit()
  }
}

const chooseFile = () => {
  fileInputRef.value?.click()
}

eventBus.on(EventKey.CHOOSE_WARNING_REPORT_FILE, chooseFile)

const handleFileSelect = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    handlePdfFile(file)
  }
  event.target.value = ''
}

const handlePaste = (event) => {
  if (!useParseFile.value) {
    return
  }
  const files = Array.from(event.clipboardData?.files || [])
  if (!files.length) return

  event.preventDefault()
  const file = files[0]
  handlePdfFile(file)
}

const allowedFileExtensions = ['pdf', 'doc', 'docx']
const allowedFileTypes = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]

const getFileExtension = (file) => {
  const name = file?.name || ''
  return name.includes('.') ? name.split('.').pop().toLowerCase() : ''
}

const isSupportedDocumentFile = (file) => {
  const extension = getFileExtension(file)
  return allowedFileTypes.includes(file?.type) || allowedFileExtensions.includes(extension)
}

const getSubmitFile = (file) => {
  if (file?.fileInfo) return file.fileInfo
  if (file?.uploadFileId) return file
  return null
}

const getOssId = (res) => {
  if (!res) return ''
  if (res.ossId || res.ossid) return res.ossId || res.ossid
  if (res.data?.ossId || res.data?.ossid) return res.data.ossId || res.data.ossid
  if (typeof res.data === 'string' || typeof res.data === 'number') return res.data
  return ''
}

const updateUploadFile = (uid, patch) => {
  fileList.value = fileList.value.map((item) => (item.uid === uid ? { ...item, ...patch } : item))
}

const handlePdfFile = async (file) => {
  if (!isSupportedDocumentFile(file)) {
    ElMessage.warning('只能上传 PDF、DOC、DOCX 文件')
    return
  }

  if (fileList.value.length) {
    ElMessage.warning('只能上传一个文件')
    return
  }

  const uploadFileItem = {
    uid: `${Date.now()}_${Math.random()}`,
    name: file.name,
    size: file.size,
    status: 'uploading',
    percentage: 0,
    raw: file,
  }

  fileList.value = [uploadFileItem]

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadFile(formData)
    const ossId = getOssId(res)

    if (!ossId) {
      throw new Error('ossId is empty')
    }

    updateUploadFile(uploadFileItem.uid, {
      status: 'success',
      percentage: 100,
      ossId,
      fileInfo: {
        type: 'document',
        transferMethod: 'local_file',
        uploadFileId: String(ossId),
        name: file.name,
        size: file.size,
      },
    })
  } catch (error) {
    console.error(error)
    updateUploadFile(uploadFileItem.uid, {
      status: 'fail',
    })
    ElMessage.error('文件上传失败')
  }
}

const removeUploadFile = (file) => {
  fileList.value = fileList.value.filter((item) => item.uid !== file.uid)
}

const formatFileSize = (size) => {
  if (!size) return ''

  if (size < 1024) {
    return `${size}B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

const getUploadStatusText = (file) => {
  if (file.status === 'success' || file.uploadFileId) {
    return `${getFileExtension(file) || '文件'} · ${formatFileSize(file.size)}`
  }

  if (file.status === 'fail') {
    return '上传失败'
  }

  return '上传中...'
}

const mikeLauncing = ref(false)
async function openMike() {
  mikeLauncing.value = true
  await audio.open()
  audioText.value = val.value
  // 连接动画过渡
  setTimeout(() => {
    mikeLauncing.value = false
    audio.startManualInput()
  }, 200)
}

async function shutdownMike() {
  audio.deactivateInput()
}

watch(
  () => audioText.value,
  (value) => {
    if (!props.isAnswering) val.value = value
  },
)

watch(val, (value) => {
  const normalizedValue = removeLeadingPunctuation(value)
  if (normalizedValue !== value) val.value = normalizedValue
  if (audioText.value !== normalizedValue) audioText.value = normalizedValue
})

onBeforeUnmount(() => {
  eventBus.off(EventKey.CHOOSE_WARNING_REPORT_FILE, chooseFile)
})
</script>

<style scoped lang="less">
.input-box {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  border: 1px solid #4D8FF2;
  padding: 12px;
  color: #878898;
  box-shadow: 0 2px 6px 0 #0048ff3d;

  .upload-preview-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 8px;
  }

  .upload-preview-item {
    position: relative;
  }

  .upload-preview-item.is-file {
    width: 13.75rem;
    height: 3.375rem;
    border-radius: 0.625rem;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.875rem;
    box-sizing: border-box;
    overflow: visible;
  }

  .upload-file-icon {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }

  .upload-file-info {
    flex: 1;
    min-width: 0;
  }

  .upload-file-name {
    font-size: 14px;
    color: #222529;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .upload-file-status {
    margin-top: 6px;
    font-size: 14px;
    color: #8a93a6;
    display: flex;
    align-items: center;
  }

  .upload-error-icon {
    color: #f56c6c;
  }

  .upload-remove-btn {
    position: absolute;
    top: -3px;
    right: -3px;
    width: 14px;
    height: 14px;
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.65);
    color: #fff;
    cursor: pointer;
    opacity: 0;
    transition: 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;

    i {
      font-size: 10px;
    }
  }

  .upload-preview-item:not(.is-uploading):hover .upload-remove-btn {
    opacity: 1;
  }

  .foot-operate {
    display: flex;
    align-items: center;
    margin-top: 10px;

    .upload-pdf-btn {
      height: 26px;
      padding: 0 10px;
      border: none;
      border-radius: 999px;
      background: #f5f7fb;
      color: #506073;
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;
      font-size: 12px;

      &:hover {
        background: #e9edf5;
      }

      i {
        font-size: 12px;
      }
    }

    .hidden-file-input {
      display: none;
    }

    .deep-think {
      padding: 2px 6px;
      border-radius: 10px;
      font-size: 12px;
      color: #222529;
      display: flex;
      align-items: center;
      gap: 4px;
      margin-right: auto;
      cursor: pointer;

      i {
        color: #222529;
        font-size: 12px;
      }

      &:hover {
        background: #d6d7d8;
      }
    }

    .deep-think-active {
      color: #3561FA;
      i {
        color: #3561FA;
      }
    }

    .live-voice {
      width: 26px;
      height: 26px;
      background: rgba(44, 168, 110, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      position: relative;

      .live-voice-inner {
        width: 20px;
        height: 20px;
        background: #2CA86E;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        animation: voice 1.5s ease-in-out infinite alternate;
      }

      .live-voice-img {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
      }
    }

    .normal-voice {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 26px;
      height: 26px;
    }

    .voice {
      width: 14px;
      height: 14px;
      cursor: pointer;
    }

    .split-line {
      width: 1px;
      height: 16px;
      background: #D9D9D9;
      margin: 0 8px 0 12px;
    }

    .submit {
      width: 32px;
      height: 24px;
      border-radius: 12px;
      background: linear-gradient(130deg, #3561FA 10.99%, #44CEFF 117.04%);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      img {
        width: 14px;
        height: 14px;
      }
    }
  }
}

:deep(.el-textarea__inner) {
  box-shadow: unset;
  font-size: 12px;
  color: #222529;
  padding: 0;

  &::placeholder {
    color: #878898;
  }
}

.is-loading {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-block;
  border-top: 3px solid #506073;
  border-right: 3px solid transparent;
  box-sizing: border-box;
  animation: rotation 1.2s linear infinite;
}

@keyframes rotation {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes voice {
  0% {
    transform: scale(1);
  }

  100% {
    transform: scale(0.8);
  }
}
</style>
