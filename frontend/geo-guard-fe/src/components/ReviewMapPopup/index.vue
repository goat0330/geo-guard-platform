<template>
  <Transition name="review-popup">
    <section v-if="visible" class="review-map-popup" :style="popupStyle" aria-label="复盘事件地图详情">
      <header class="popup-header">
        <div class="title-row">
          <el-tooltip
            :content="eventTitle"
            placement="top"
            effect="dark"
            :show-after="100"
          >
            <h2>{{ eventTitle }}</h2>
          </el-tooltip>
          <span v-if="eventType" class="event-tag danger">{{ eventType }}</span>
          <span v-if="eventScale" class="event-tag scale">{{ eventScale }}</span>
          <span v-if="eventStatus" class="event-tag status"><i></i>{{ eventStatus }}</span>
        </div>
        <button class="close-button" type="button" title="关闭" @click="emit('close')">
          <i class="iconfont icon-close"></i>
        </button>
      </header>

      <div class="address" :title="eventAddress">
        <Location class="address-icon" />
        <span class="address-label">详细地址：</span>
        <span class="address-value">{{ eventAddress }}</span>
      </div>

      <h3>基础信息：</h3>
      <div class="base-info">
        <div v-for="item in baseInfo" :key="item.label" class="info-item">
          <span>
            <i class="iconfont" :class="item.icon"></i>
            {{ item.label }}
          </span>
          <strong :title="item.value">{{ item.value || '--' }}</strong>
        </div>
      </div>

      <h3>处置概况：</h3>
      <p class="disposal-summary" :title="disposalSummary">{{ disposalSummary || '--' }}</p>
      <div v-if="disposalResult" class="disposal-result">
        <CircleCheckFilled />
        <span :title="disposalResult">{{ disposalResult }}</span>
      </div>

      <div class="material-row">
        <span>现场图片：</span>
        <button
          type="button"
          :class="{ 'is-disabled': photoCount === 0 }"
          title="点击查看现场照片"
          @click="handleOpenPhotos"
        >
          图片共{{ photoCount }}张
        </button>
      </div>
      <div class="material-row">
        <span>调查报告：</span>
        <button
          type="button"
          :class="{ 'is-disabled': reportCount === 0 }"
          title="点击查看现场调查报告"
          @click="handleOpenReport"
        >
          调查报告共{{ reportCount }}份
        </button>
      </div>

      <button class="detail-button" type="button" @click="emit('detail')">查看/生成复盘</button>
    </section>
  </Transition>

  <!-- 现场照片弹窗 -->
  <PhotoDialog
    v-model:visible="photoDialogVisible"
    :event-id="event?.id"
    :event-title="eventTitle"
  />

  <!-- 调查报告弹窗 -->
  <ReportDialog
    v-model:visible="reportDialogVisible"
    :event-id="event?.id"
    :event-title="eventTitle"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { CircleCheckFilled, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PhotoDialog from './components/PhotoDialog.vue'
import ReportDialog from './components/ReportDialog.vue'

defineOptions({ name: 'ReviewMapPopup' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  event: { type: Object, default: () => ({}) },
  position: { type: Object, default: null },
})
const emit = defineEmits(['close', 'detail'])

const MARKER_HEIGHT = 30
const MARKER_GAP = 14

const popupStyle = computed(() => {
  if (!props.position) return {}
  // 屏幕坐标位于图标底部，弹窗底边与图标顶边固定间隔 14px
  return {
    left: `${props.position.x}px`,
    top: `${props.position.y - MARKER_HEIGHT - MARKER_GAP}px`,
  }
})

const eventTitle = computed(() => props.event?.eventName || props.event?.title || '--')
const eventType = computed(() => props.event?.eventTypeName || props.event?.disasterType || props.event?.type || '')
const eventScale = computed(() => props.event?.scaleLevel || props.event?.eventLevelName || props.event?.scale || '')
const eventStatus = computed(() => props.event?.reviewStatusName || props.event?.reviewStatus || '')
const eventAddress = computed(() => props.event?.displayAddress || props.event?.detailedAddress || props.event?.location || '--')

const eventTime = computed(() => {
  const value = props.event?.occurrenceTime || props.event?.time
  if (!value) return '--'
  const str = String(value)
  return str.length >= 16 ? `${str.slice(5, 10)} ${str.slice(11, 16)}` : str
})

// 基础信息按照接口调试规范，空值仅做容错不伪造字段
const baseInfo = computed(() => {
  const e = props.event || {}
  const casualtyVal =
    e.casualties !== null && e.casualties !== undefined && e.casualties !== ''
      ? `${e.casualties}人`
      : '--'

  // 人口底数：接口列表未包含此字段，按规范无映射填 '--'
  const populationVal = '--'

  // 威胁对象：列表接口为 threatObject
  const threatVal = e.threatObject || '--'

  return [
    { label: '发生时间', value: eventTime.value, icon: 'icon-a-Frame1' },
    {
      label: '人口底数',
      value: populationVal,
      icon: 'icon-a-Frame2',
    },
    {
      label: '威胁对象',
      value: threatVal,
      icon: 'icon-weixie',
    },
    {
      label: '人员伤亡',
      value: casualtyVal,
      icon: 'icon-a-Frame5',
    },
  ]
})

const disposalSummary = computed(() => props.event?.disposalOverview || props.event?.description || '--')

const disposalResult = computed(() => {
  const e = props.event || {}
  if (Array.isArray(e.disposalMeasures) && e.disposalMeasures.length > 0) {
    return e.disposalMeasures.join('、')
  }
  return ''
})

const photoCount = computed(() => {
  const e = props.event || {}
  if (e.scenePhotoCount !== null && e.scenePhotoCount !== undefined) {
    return Number(e.scenePhotoCount) || 0
  }
  return 0
})

const reportCount = computed(() => {
  const e = props.event || {}
  if (e.investigationReportCount !== null && e.investigationReportCount !== undefined) {
    return Number(e.investigationReportCount) || 0
  }
  return 0
})

const photoDialogVisible = ref(false)
const reportDialogVisible = ref(false)

const handleOpenPhotos = () => {
  if (photoCount.value === 0) {
    ElMessage.info('当前事件暂无现场照片')
    return
  }
  photoDialogVisible.value = true
}

const handleOpenReport = () => {
  if (reportCount.value === 0) {
    ElMessage.info('当前事件暂未关联调查报告')
    return
  }
  reportDialogVisible.value = true
}
</script>

<style lang="less" scoped>
.review-map-popup {
  position: absolute;
  z-index: 12;
  width: 462px;
  min-height: 357px;
  padding: 14px 16px 16px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 4px 12px rgba(0, 135, 202, 0.16);
  color: #222527;
  backdrop-filter: blur(4px);
  transform: translate(-50%, -100%);
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.title-row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

h2 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
  color: #222527;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.event-tag {
  flex: 0 0 auto;
  min-width: 36px;
  height: 20px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;

  &.danger {
    background: rgba(228, 91, 91, 0.16);
    color: #e45b5b;
  }

  &.scale {
    background: rgba(255, 146, 44, 0.16);
    color: #ff922c;
  }

  &.status {
    gap: 4px;
    background: rgba(0, 123, 255, 0.16);
    color: #007bff;

    i {
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background: #007bff;
    }
  }
}

.close-button {
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #383c41;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }

  i {
    font-size: 16px;
  }
}

.address {
  min-width: 0;
  margin-top: 6px;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  color: #617185;
  font-size: 12px;
  line-height: 20px;

  .address-icon {
    flex: 0 0 14px;
    width: 14px;
    height: 14px;
    margin-top: 3px;
    color: #617185;
  }

  .address-label {
    flex-shrink: 0;
    white-space: nowrap;
    color: #617185;
  }

  .address-value {
    flex: 1;
    min-width: 0;
    word-break: break-all;
    white-space: normal;
    color: #617185;
  }
}

h3 {
  margin: 10px 0 6px;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  color: #222527;
}

.base-info {
  height: 68px;
  padding: 8px 4px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  box-sizing: border-box;
  border: 1px solid #8fc8ff;
  border-radius: 8px;
  background: linear-gradient(180deg, #DEF0FF 0%, #ffffff80 100%);
}

.info-item {
  min-width: 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  & + .info-item {
    border-left: 1px solid #8fc8ff;
  }

  span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #617185;
    font-size: 12px;
    line-height: 18px;
    white-space: nowrap;

    i.iconfont {
      font-size: 10px;
      color: #617185;
      line-height: 1;
    }
  }

  strong {
    display: block;
    width: 100%;
    margin-top: 2px;
    overflow: hidden;
    color: #007bff;
    font-size: 14px;
    font-weight: 700;
    line-height: 24px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

.disposal-summary {
  margin: 0;
  overflow: hidden;
  color: #617185;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.disposal-result {
  height: 28px;
  margin-top: 6px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.62);
  color: #44b699;
  font-size: 12px;
  line-height: 28px;

  svg {
    flex: 0 0 16px;
    width: 16px;
    height: 16px;
  }

  span {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

.material-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  color: #617185;
  font-size: 12px;
  line-height: 20px;

  button {
    padding: 0;
    border: 0;
    border-bottom: 1px solid #007bff;
    background: transparent;
    color: #007bff;
    font: inherit;
    font-size: 12px;
    cursor: pointer;
    line-height: 18px;
    transition: opacity 0.2s;

    &:hover {
      opacity: 0.8;
    }

    &.is-disabled {
      border-bottom-color: #a6acb8;
      color: #a6acb8;
      cursor: not-allowed;

      &:hover {
        opacity: 1;
      }
    }
  }
}

.detail-button {
  width: 100%;
  margin-top: 12px;
  padding: 12px 0 0;
  border: 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
  color: #007bff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.review-popup-enter-active,
.review-popup-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.review-popup-enter-from,
.review-popup-leave-to {
  opacity: 0;
  transform: translate(-50%, calc(-100% + 8px));
}
</style>
