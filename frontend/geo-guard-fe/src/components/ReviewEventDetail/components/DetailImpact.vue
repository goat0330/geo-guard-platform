<template>
  <section class="detail-section impact-section">
    <div class="section-title">
      <img :src="pointIcon" class="point-icon" alt="" />
      <h3>影响情况</h3>
    </div>

    <!-- 假 Loading 骨架屏 -->
    <div v-if="loading" class="impact-skeleton">
      <div class="skeleton-table shimmer">
        <div v-for="n in 5" :key="n" class="skeleton-row"></div>
      </div>
    </div>

    <!-- 真实内容展示 -->
    <div v-else class="impact-content message-enter">
      <div class="info-table-card">
        <!-- 威胁对象 -->
        <div class="table-row">
          <div class="cell-label">威胁对象</div>
          <div class="cell-value" :title="info.threatObject">
            {{ info.threatObject }}
          </div>
        </div>

        <!-- 双列：撤离户数 / 撤离人数 -->
        <div class="table-row split-row">
          <div class="half-col">
            <div class="cell-label">撤离户数</div>
            <div class="cell-value">{{ info.evacuatedHouseholds }}</div>
          </div>
          <div class="half-col">
            <div class="cell-label wide-label border-left">撤离人数</div>
            <div class="cell-value">{{ info.evacuatedPeople }}</div>
          </div>
        </div>

        <!-- 双列：受伤人数 / 失踪人数 -->
        <div class="table-row split-row">
          <div class="half-col">
            <div class="cell-label">受伤人数</div>
            <div class="cell-value">{{ info.injuredPeople }}</div>
          </div>
          <div class="half-col">
            <div class="cell-label wide-label border-left">失踪人数</div>
            <div class="cell-value">{{ info.missingPeople }}</div>
          </div>
        </div>

        <!-- 双列：死亡人数 / 直接经济损失 -->
        <div class="table-row split-row">
          <div class="half-col">
            <div class="cell-label">死亡人数</div>
            <div class="cell-value">{{ info.deadPeople }}</div>
          </div>
          <div class="half-col">
            <div class="cell-label wide-label border-left">直接经济损失</div>
            <div class="cell-value">{{ info.directLoss }}</div>
          </div>
        </div>

        <!-- 事件资料 -->
        <div class="table-row">
          <div class="cell-label">事件资料</div>
          <div class="cell-value link-group">
            <a
              href="javascript:void(0)"
              class="doc-link"
              :class="{ 'is-disabled': photoCount === 0 }"
              @click="handleViewPhotos"
            >
              图片资料({{ photoCount }})
            </a>
            <div class="report-link-wrap">
              <a
                href="javascript:void(0)"
                class="doc-link"
                :class="{ 'is-disabled': reportCount === 0 }"
                @click="handleViewReport"
              >
                调查报告({{ reportCount }})
              </a>
              <button
                v-if="reportCount > 0"
                type="button"
                class="quick-download-btn"
                title="下载现场调查报告"
                :disabled="downloading"
                @click.stop="handleDirectDownloadReport"
              >
<!--                <el-icon><Download /></el-icon>-->
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import pointIcon from '@/assets/imgs/point.png'
import { useReportDownload } from '@/composables/useReportDownload.js'

defineOptions({ name: 'DetailImpact' })

const props = defineProps({
  impactInfo: {
    type: Object,
    default: () => ({}),
  },
  materialInfo: {
    type: Object,
    default: () => ({}),
  },
  event: {
    type: Object,
    default: () => ({}),
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['preview-photo', 'preview-report'])

const { downloading, downloadReport } = useReportDownload()

const formatNum = (val, unit) => {
  if (val === null || val === undefined || val === '') return '--'
  return `${val}${unit}`
}

const info = computed(() => {
  const imp = props.impactInfo || {}
  const e = props.event || {}

  return {
    threatObject: imp.threatObject || e.threatObject || '--',
    evacuatedHouseholds: formatNum(imp.evacuatedHouseholds, '户'),
    evacuatedPeople: formatNum(imp.evacuatedPeople, '人'),
    injuredPeople: formatNum(
      imp.injuredPeople !== undefined && imp.injuredPeople !== null
        ? imp.injuredPeople
        : (e.casualties !== undefined && e.casualties !== null ? e.casualties : null),
      '人',
    ),
    missingPeople: formatNum(imp.missingPeople, '人'),
    deadPeople: formatNum(imp.deadPeople, '人'),
    directLoss: formatNum(imp.directLoss, '万元'),
  }
})

const photoCount = computed(() => {
  const m = props.materialInfo || {}
  const e = props.event || {}
  if (m.photoCount !== undefined && m.photoCount !== null) return Number(m.photoCount) || 0
  if (Array.isArray(m.photos)) return m.photos.length
  if (e.scenePhotoCount !== undefined && e.scenePhotoCount !== null) return Number(e.scenePhotoCount) || 0
  return 0
})

const reportCount = computed(() => {
  const m = props.materialInfo || {}
  const e = props.event || {}
  if (m.investigationReportCount !== undefined && m.investigationReportCount !== null) {
    return Number(m.investigationReportCount) || 0
  }
  if (Array.isArray(m.investigationReports)) return m.investigationReports.length
  if (e.investigationReportCount !== undefined && e.investigationReportCount !== null) {
    return Number(e.investigationReportCount) || 0
  }
  return 0
})

const handleViewPhotos = () => {
  if (photoCount.value === 0) {
    ElMessage.info('当前事件暂无现场图片')
    return
  }
  emit('preview-photo', props.event)
}

const handleViewReport = () => {
  if (reportCount.value === 0) {
    ElMessage.info('当前事件暂未关联调查报告')
    return
  }
  emit('preview-report', props.event)
}

const handleDirectDownloadReport = async () => {
  const reports = props.materialInfo?.investigationReports || []
  if (reports.length === 1) {
    await downloadReport(reports[0])
  } else {
    emit('preview-report', props.event)
  }
}
</script>

<style lang="less" scoped>
.impact-section {
  margin-top: 18px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .point-icon {
    width: 8px;
    height: 8px;
    flex-shrink: 0;
    object-fit: contain;
  }

  h3 {
    margin: 0;
    color: #222527;
    font-size: 14px;
    font-weight: 700;
    line-height: 16px;
  }
}

.info-table-card {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #fbfcff;
  overflow: hidden;
}

.table-row {
  height: 38px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e5e8ec;
  box-sizing: border-box;

  &:last-child {
    border-bottom: 0;
  }
}

.cell-label {
  flex: 0 0 91px;
  width: 91px;
  height: 100%;
  padding: 0 12px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  background: #f4f9ff;
  border-right: 1px solid #e4eaef;
  color: #878898;
  font-size: 14px;
  font-weight: 400;
  white-space: nowrap;

  &.wide-label {
    flex: 0 0 107px;
    width: 107px;
  }

  &.border-left {
    border-left: 1px solid #e4eaef;
  }
}

.cell-value {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
  color: #383c41;
  font-size: 14px;
  font-weight: 400;
  line-height: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.link-group {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.doc-link {
  color: #007bff;
  font-size: 14px;
  font-weight: 400;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }

  &.is-disabled {
    color: #a6acb8;
    cursor: not-allowed;
    text-decoration: none;

    &:hover {
      opacity: 1;
    }
  }
}

.report-link-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.quick-download-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #007bff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #e0eefa;
    color: #007bff;
  }

  &:disabled {
    color: #a6acb8;
    cursor: not-allowed;
  }

  :deep(.el-icon) {
    font-size: 14px;
  }
}

.split-row {
  display: flex;

  .half-col {
    flex: 1;
    min-width: 0;
    height: 100%;
    display: flex;
    align-items: center;
  }
}

/* 假 loading 骨架屏 */
.impact-skeleton {
  width: 100%;
}

.skeleton-table {
  width: 100%;
  height: 190px;
  border-radius: 12px;
  border: 1px solid #e4eaef;
  background: #fbfcff;
  padding: 8px 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}

.skeleton-row {
  width: 100%;
  height: 24px;
  border-radius: 4px;
  background: #edf1f6;
}

.shimmer {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.6) 50%,
      rgba(255, 255, 255, 0) 100%
    );
    animation: shimmer-swipe 1.4s infinite;
  }
}

@keyframes shimmer-swipe {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.message-enter {
  animation: message-enter 0.24s ease both;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
