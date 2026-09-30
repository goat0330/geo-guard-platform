<template>
  <el-dialog
    v-model="dialogVisible"
    class="review-photo-dialog"
    title="现场照片"
    width="720px"
    top="10vh"
    append-to-body
    destroy-on-close
    @close="handleClose"
  >
    <div class="photo-dialog-header">
      <h3 :title="eventTitle">{{ eventTitle || '事件现场照片' }}</h3>
      <span class="photo-total-badge">共 {{ photos.length }} 张照片</span>
    </div>

    <!-- 照片分类过滤标签 -->
    <div v-if="categories.length > 1" class="category-tabs">
      <button
        v-for="cat in categories"
        :key="cat.name"
        type="button"
        class="category-tab"
        :class="{ 'is-active': activeCategory === cat.name }"
        @click="activeCategory = cat.name"
      >
        {{ cat.name }} ({{ cat.count }})
      </button>
    </div>

    <!-- 照片展示网格 -->
    <div v-loading="loading" class="photo-grid-wrap">
      <div v-if="filteredPhotos.length > 0" class="photo-grid">
        <div
          v-for="(photo, index) in filteredPhotos"
          :key="photo.fileId || index"
          class="photo-card"
        >
          <div class="image-wrapper">
            <el-image
              class="photo-img"
              :src="photo.url"
              :preview-src-list="previewList"
              :initial-index="getPreviewIndex(photo)"
              preview-teleported
              fit="cover"
              loading="lazy"
            >
              <template #error>
                <div class="photo-fallback">
                  <el-icon><Picture /></el-icon>
                  <span>暂无预览</span>
                </div>
              </template>
            </el-image>
            <span v-if="photo.category" class="category-tag">
              {{ photo.category }}
            </span>
          </div>
          <div v-if="photo.name || photo.description" class="photo-caption">
            <span :title="photo.name || photo.description">
              {{ photo.name || photo.description }}
            </span>
          </div>
        </div>
      </div>

      <el-empty
        v-else-if="!loading"
        :image-size="72"
        description="暂无相关现场照片"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { getReviewEventDetail } from '@/api/review.js'
import { getImageUrlById } from '@/api/common.js'
import sitePhoto1 from '@/assets/imgs/fupan/site-photo-1.png'
import sitePhoto2 from '@/assets/imgs/fupan/site-photo-2.png'

defineOptions({ name: 'PhotoDialog' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  eventId: { type: [String, Number], default: '' },
  eventTitle: { type: String, default: '' },
})

const emit = defineEmits(['update:visible', 'close'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const loading = ref(false)
const photos = ref([])
const activeCategory = ref('全部')

// 开发联调 Mock 备用图片映射
const MOCK_PHOTO_MAP = {
  'mock-oss-panorama-001': sitePhoto1,
  'mock-oss-deformation-001': sitePhoto2,
  'mock-oss-damage-001': sitePhoto1,
  'mock-task-photo-001': sitePhoto2,
  'mock-task-photo-002': sitePhoto1,
}

// 提取分类列表
const categories = computed(() => {
  const map = {}
  photos.value.forEach((p) => {
    const cat = p.category || '其他'
    map[cat] = (map[cat] || 0) + 1
  })
  const list = [{ name: '全部', count: photos.value.length }]
  Object.keys(map).forEach((cat) => {
    list.push({ name: cat, count: map[cat] })
  })
  return list
})

// 根据选中的分类过滤
const filteredPhotos = computed(() => {
  if (activeCategory.value === '全部') {
    return photos.value
  }
  return photos.value.filter((p) => (p.category || '其他') === activeCategory.value)
})

// 全屏预览列表
const previewList = computed(() => {
  return filteredPhotos.value.map((p) => p.url).filter(Boolean)
})

const getPreviewIndex = (photo) => {
  const idx = previewList.value.indexOf(photo.url)
  return idx >= 0 ? idx : 0
}

/**
 * 加载当前事件关联的现场照片
 */
const loadPhotos = async () => {
  if (!props.eventId) return
  loading.value = true
  activeCategory.value = '全部'

  try {
    const detail = await getReviewEventDetail(props.eventId)
    const rawPhotos = detail?.materialInfo?.photos || []

    // 收集 fileId 用于换取真实 OSS 链接
    const fileIds = rawPhotos.map((p) => p.fileId).filter(Boolean)
    const urlMap = {}

    if (fileIds.length > 0) {
      try {
        const resUrls = await getImageUrlById({ ids: fileIds })
        if (Array.isArray(resUrls)) {
          fileIds.forEach((id, idx) => {
            const rawUrl = resUrls[idx]
            if (rawUrl) {
              try {
                const urlObj = new URL(rawUrl)
                urlMap[id] = '/oss-service' + urlObj.pathname + urlObj.search
              } catch {
                urlMap[id] = rawUrl
              }
            }
          })
        }
      } catch (err) {
        console.warn('换取真实 OSS 图片地址失败，回退开发环境备用图', err)
      }
    }

    photos.value = rawPhotos.map((p, idx) => {
      const fallbackUrl = MOCK_PHOTO_MAP[p.fileId] || (idx % 2 === 0 ? sitePhoto1 : sitePhoto2)
      return {
        ...p,
        url: urlMap[p.fileId] || (p.fileId?.startsWith('http') ? p.fileId : fallbackUrl),
      }
    })
  } catch (error) {
    console.error('获取现场照片失败', error)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      void loadPhotos()
    } else {
      photos.value = []
    }
  },
)

const handleClose = () => {
  emit('close')
}
</script>

<style lang="less" scoped>
.review-photo-dialog {
  :deep(.el-dialog__header) {
    margin-right: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f4;
  }

  :deep(.el-dialog__title) {
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 24px;
  }

  :deep(.el-dialog__body) {
    padding: 16px 20px;
  }
}

.photo-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    min-width: 0;
    margin: 0;
    overflow: hidden;
    color: #222527;
    font-size: 14px;
    font-weight: 700;
    line-height: 22px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .photo-total-badge {
    flex-shrink: 0;
    color: #617185;
    font-size: 12px;
    line-height: 18px;
  }
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.category-tab {
  padding: 4px 12px;
  border: 1px solid #e4eaef;
  border-radius: 4px;
  background: #f8fafc;
  color: #617185;
  font-family: inherit;
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #007bff;
    color: #007bff;
  }

  &.is-active {
    border-color: #007bff;
    background: #007bff;
    color: #ffffff;
  }
}

.photo-grid-wrap {
  min-height: 240px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.photo-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e4eaef;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 123, 255, 0.12);
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 136px;
  background: #f5f7fa;
}

.photo-img {
  width: 100%;
  height: 100%;
  display: block;
}

.category-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 2;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(34, 37, 39, 0.68);
  color: #ffffff;
  font-size: 12px;
  line-height: 16px;
}

.photo-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #9096a2;
  font-size: 12px;

  :deep(.el-icon) {
    font-size: 24px;
  }
}

.photo-caption {
  padding: 8px 10px;
  font-size: 12px;
  line-height: 18px;
  color: #617185;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
