<template>
  <div class="upload-file-loader">
    <template v-if="fileType === 'image'">
      <el-image
        v-if="imageUrl"
        class="preview-img"
        :src="imageUrl"
        :preview-src-list="[imageUrl]"
        fit="cover"
        @error="handleImageError"
      />
      <div v-else-if="imageLoadFailed" class="img-placeholder is-error">图片加载失败</div>
      <div v-else class="img-placeholder" v-loading="true"></div>
    </template>
    <template v-else-if="fileType === 'document'">
      <div class="document-preview">
        <img src="@/assets/imgs/chatEngine/file-uploaded.png" class="upload-file-icon" />

        <div class="upload-file-info">
          <div class="upload-file-name" :title="file.name || '文档'">{{ file.name || '未命名' }}</div>
          <div class="upload-file-status">{{ getUploadStatusText(file) }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount, watch } from 'vue'
import { downloadFile, getImageUrlById } from '@/api/common.js'

const props = defineProps({
  file: {
    type: Object,
    default: () => ({}),
  },
})

const imageUrl = ref('')
const imageLoadFailed = ref(false)
let downloadFallbackTried = false
let downloadedObjectUrl = ''

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'heic', 'heif']

const fileType = computed(() => {
  const type = String(props.file?.type || '').toLowerCase()
  if (type === 'image' || type.startsWith('image/')) return 'image'
  if (type === 'document') return 'document'

  const extension = props.file?.name?.split('.').pop()?.toLowerCase() || ''
  return IMAGE_EXTENSIONS.includes(extension) ? 'image' : 'document'
})

const uploadFileId = computed(
  () => props.file?.uploadFileId || props.file?.ossId || props.file?.ossid || '',
)

function revokeDownloadedObjectUrl() {
  if (!downloadedObjectUrl) return
  URL.revokeObjectURL(downloadedObjectUrl)
  downloadedObjectUrl = ''
}

async function handleImageError() {
  if (!downloadFallbackTried && uploadFileId.value) {
    downloadFallbackTried = true

    try {
      const blob = await downloadFile(uploadFileId.value)
      if (blob instanceof Blob && blob.size > 0) {
        revokeDownloadedObjectUrl()
        downloadedObjectUrl = URL.createObjectURL(blob)
        imageUrl.value = downloadedObjectUrl
        return
      }
    } catch (error) {
      console.error('下载历史图片失败:', error)
    }
  }

  imageUrl.value = ''
  imageLoadFailed.value = true
}

const fetchImageUrl = async () => {
  if (fileType.value !== 'image') return

  revokeDownloadedObjectUrl()
  downloadFallbackTried = false
  imageLoadFailed.value = false

  const existingUrl = props.file.previewUrl || props.file.url
  if (existingUrl) {
    imageUrl.value = existingUrl
    return
  }

  imageUrl.value = ''
  if (uploadFileId.value) {
    try {
      const res = await getImageUrlById({ ids: [uploadFileId.value] })
      if (res && res.length > 0 && res[0]) {
        try {
          const urlObj = new URL(res[0])
          imageUrl.value = '/oss-service' + urlObj.pathname + urlObj.search
        } catch {
          imageUrl.value = res[0]
        }
      } else {
        imageLoadFailed.value = true
      }
    } catch (error) {
      console.error('获取图片地址失败:', error)
      imageLoadFailed.value = true
    }
  } else {
    imageLoadFailed.value = true
  }
}

function formatFileSize(size) {
  if (!size) return ''

  if (size < 1024) {
    return `${size}B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

function getUploadStatusText(file) {
  console.log('file in getUploadStatusText:', file)
  const extension = file.name?.split('.').pop()?.toLowerCase() || ''
  return `${extension} · ${formatFileSize(file.size)}`
}

watch(
  () => props.file,
  () => {
    fetchImageUrl()
  },
  { deep: true, immediate: true },
)

onBeforeUnmount(() => {
  revokeDownloadedObjectUrl()

  if (props.file?.isLocalPreview && props.file.previewUrl) {
    URL.revokeObjectURL(props.file.previewUrl)
  }
})
</script>

<style lang="less" scoped>
.upload-file-loader {
  .preview-img {
    width: 80px;
    height: 80px;
    border-radius: 10px;
    border: 1px solid #d4d8dd;
    cursor: pointer;
    display: block;
  }

  .img-placeholder {
    width: 120px;
    height: 120px;
    border-radius: 10px;
    // border: 1px solid #d4d8dd;
    background: #f6f8fc;

    &.is-error {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #9096a2;
      font-size: 14px;
    }
  }

  .document-preview {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    background: #f5f5f5;
    border-radius: 10px;
    column-gap: 10px;
    width: 220px;
    height: 54px;

    .upload-file-icon {
      color: #3561fa;
      flex-shrink: 0;
      width: 26px;
      height: 26px;
    }

    .upload-file-info {
      min-width: 0;
      flex: 1;
    }

    .upload-file-name {
      color: #222529;
      font-size: 14px;
      line-height: 20px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .upload-file-status {
      font-size: 14px;
      color: #8a93a6;
    }
  }
}
</style>
