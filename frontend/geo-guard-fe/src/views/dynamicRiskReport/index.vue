<template>
  <div class="dynamic-risk-report-workspace">
    <main ref="pageRef" class="dynamic-risk-report-page">
      <header class="page-header">
        <button type="button" class="back-button" title="返回风险情况概览" @click="goBack">
          <i class="iconfont icon-title-back"></i>
        </button>
        <h1>动态风险评价“一张图”</h1>
        <button
          type="button"
          class="download-button"
          :disabled="downloading || !mapReady"
          :title="downloading ? '报告生成中' : '下载报告'"
          aria-label="下载报告"
          @click="downloadReport"
        >
          <i class="iconfont icon-download"></i>
        </button>
      </header>

      <section
        ref="mapSectionRef"
        v-loading="loading"
        element-loading-text="正在加载地图数据..."
        class="map-section"
        :class="{ 'is-dragging': isDragging, 'is-collapsed': isMapCollapsed }"
        :style="mapSectionStyle"
      >
        <DynamicRiskMap ref="mapRef" :units="units" preserve-drawing-buffer @ready="mapReady = true" />
        <div class="legend-panel">
          <strong>图例</strong>
          <div v-for="item in legends" :key="item.level"><i :style="{ background: item.color }"></i>{{ item.label }}风险</div>
        </div>
        <div v-if="!loading && !units.length" class="empty-state">暂无斜坡单元动态风险数据</div>
      </section>

      <!-- 地图与报告内容区域可拖拽分界线及一键展开/收起按钮 -->
      <MapSplitter
        :is-collapsed="isMapCollapsed"
        :is-dragging="isDragging"
        @drag-start="startDrag($event, mapSectionRef, pageRef)"
        @toggle="toggleCollapse(mapSectionRef)"
      />

      <!-- 下方报告内容滚动区：上下预留留白 padding 并配合消隐遮罩，防止贴边 -->
      <section
        class="report-section"
        :class="{
          'is-custom-height': mapHeight !== null,
          'is-expanded': isMapCollapsed,
        }"
      >
        <div class="report-scroll-wrapper">
          <DynamicRiskReportContent :units="units" :date="targetDate" @choose-unit="focusUnit" />
        </div>
      </section>

      <div ref="exportRef" class="export-content">
        <h1>动态风险评价“一张图”</h1>
        <img v-if="exportMapImage" :src="exportMapImage" alt="动态风险评价地图" />
        <DynamicRiskReportContent :units="units" :date="targetDate" is-download />
      </div>
    </main>

    <aside class="agent-slot" :class="{ 'is-collapsed': agentCollapsed }">
      <button
        class="agent-collapse-trigger"
        type="button"
        :title="agentCollapsed ? '展开风险评价智能体' : '收起风险评价智能体'"
        @click="agentCollapsed = !agentCollapsed"
      >
        <img :src="agentCollapsed ? leftArrow : rightArrow" alt="" />
      </button>
      <RiskReportAgent
        class="result-agent"
        :class="{ 'is-collapsed': agentCollapsed }"
        visible
        embedded
        mode="result"
        @close="agentCollapsed = true"
      />
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import dayjs from 'dayjs'
import DynamicRiskMap from '@/components/DynamicRiskMap/index.vue'
import DynamicRiskReportContent from '@/components/DynamicRiskReportContent/index.vue'
import RiskReportAgent from '@/components/RiskReportAgent/index.vue'
import MapSplitter from './components/MapSplitter.vue'
import { useVerticalSplitter } from './useVerticalSplitter.js'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'
import { getRiskAssessmentList, resolveRiskAssessmentPage } from '@/api/riskEvaluation.js'
import { RISK_ASSESSMENT_CACHE_COUNTY } from '@/utils/riskAssessmentCache.js'
import { exportDomToDocx } from '@/utils/docx.js'
import { RISK_LEVEL_COLOR, RISK_LEVEL_TEXT } from '@/utils/enum.js'

defineOptions({ name: 'DynamicRiskReport' })

const router = useRouter()
const route = useRoute()
const targetDate = computed(() => {
  const queryDate = route.query.date
  const date = dayjs(queryDate)
  return date.isValid() ? date.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')
})
const pageRef = ref(null)
const mapSectionRef = ref(null)
const mapRef = ref(null)
const exportRef = ref(null)
const units = ref([])
const loading = ref(false)
const downloading = ref(false)
const mapReady = ref(false)
const exportMapImage = ref('')
const agentCollapsed = ref(false)
const legends = [4, 3, 2, 1].map((level) => ({ level, label: RISK_LEVEL_TEXT[level], color: RISK_LEVEL_COLOR[level] }))

// 地图与内容分界线拖拽和一键展开/收起管理
const {
  isMapCollapsed,
  isDragging,
  mapHeight,
  startDrag,
  toggleCollapse,
} = useVerticalSplitter({
  minMapHeight: 160,
  minBottomHeight: 180,
  collapseThreshold: 60,
  onResize: () => {
    mapRef.value?.resize?.()
  },
})

// 地图容器响应式样式（初始保持和原来一致，仅在折叠或拖动后应用特定尺寸）
const mapSectionStyle = computed(() => {
  if (isMapCollapsed.value) {
    return {
      height: '0px',
      minHeight: '0px',
      flex: '0 0 0px',
      marginBottom: '0px',
      opacity: '0',
      pointerEvents: 'none',
    }
  }
  if (mapHeight.value === null) {
    return {}
  }
  return {
    height: `${mapHeight.value}px`,
    minHeight: '160px',
    flex: `0 0 ${mapHeight.value}px`,
    opacity: '1',
  }
})

const loadUnits = async () => {
  loading.value = true
  try {
    const params = {
      county: RISK_ASSESSMENT_CACHE_COUNTY,
      assessmentDate: targetDate.value,
      dynamicRiskLevels: [4, 3, 2, 1],
      withSlopeUnit: true,
    }
    // 优先读取 IndexedDB 当天全量缓存（有缓存拿缓存），未命中时请求接口并异步写入缓存
    const response = await getRiskAssessmentList(params)
    const rows = Array.isArray(response) ? response : resolveRiskAssessmentPage(response).rows

    // 格式化风险评估行，并扁平化关联的斜坡单元空间与属性数据供地图与报告使用
    units.value = rows.map((row) => {
      const slopeUnit = row?.slopeUnit || {}
      const level = Number(row?.dynamicRiskLevel)
      const slopeName =
        slopeUnit?.name ||
        row?.name ||
        slopeUnit?.slopeName ||
        row?.slopeName ||
        slopeUnit?.slopeUnitName ||
        row?.slopeUnitName ||
        ''
      return {
        ...slopeUnit,
        ...row,
        id: String(row?.id || row?.slopeUnitId || slopeName || ''),
        assessmentId: row?.id,
        slopeUnitId: row?.slopeUnitId || slopeUnit?.id || '',
        name: slopeName || row?.slopeUnitId || '',
        wkt: slopeUnit?.wkt || row?.wkt || '',
        dynamicRiskLevel: Number.isFinite(level) ? level : 0,
        area: slopeUnit?.area ?? row?.area ?? '',
        county: slopeUnit?.county || row?.county || RISK_ASSESSMENT_CACHE_COUNTY,
        street: slopeUnit?.street || row?.street || '',
        village: slopeUnit?.village || row?.village || '',
        center: slopeUnit?.center || row?.center || '',
        slopeUnit,
        raw: row,
      }
    })
  } catch (error) {
    console.error('加载动态风险评价数据失败', error)
    units.value = []
  } finally {
    loading.value = false
  }
}

const focusUnit = (unit) => {
  const target = units.value.find((item) => String(item?.id) === String(unit?.id))
  if (!target) return
  mapRef.value?.showUnit(target)
}

const downloadReport = async () => {
  if (downloading.value || !mapReady.value) return
  downloading.value = true
  try {
    exportMapImage.value = await mapRef.value?.exportImage()
    const blob = await exportDomToDocx(exportRef.value)
    saveAs(blob, '动态风险评价一张图.docx')
  } catch (error) {
    console.error('下载动态风险评价报告失败', error)
    ElMessage.error('报告生成失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

const goBack = () => router.push({ name: 'RiskEval' })

watch(() => route.query.date, () => {
  loadUnits()
})

onMounted(loadUnits)
</script>

<style lang="less" scoped>
.dynamic-risk-report-workspace {
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  overflow: visible;
  background: transparent;
}
.dynamic-risk-report-page {
  display: flex;
  width: auto;
  min-width: 0;
  min-height: 100%;
  flex: 1 1 auto;
  flex-direction: column;
  overflow: hidden;
  border-radius: 16px;
  background: #ffffff;
  color: #222527;
}
.agent-slot {
  position: relative;
  width: 480px;
  min-width: 480px;
  height: 100%;
  margin-left: 12px;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              min-width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              margin-left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.agent-slot.is-collapsed {
  width: 0;
  min-width: 0;
  margin-left: 0;
}
.result-agent {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.2s ease,
              visibility 0s linear 0s;
}
.result-agent.is-collapsed {
  visibility: hidden;
  opacity: 0;
  transform: translateX(calc(100% + 12px));
  pointer-events: none;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.2s ease,
              visibility 0s linear 0.28s;
}
.agent-collapse-trigger {
  position: absolute;
  top: 50%;
  left: -10px;
  z-index: 30;
  width: 20px;
  height: 72px;
  padding: 0;
  border: 1px solid #cccccc66;
  border-radius: 17px;
  background: #ffffff;
  cursor: pointer;
  transform: translateY(-50%);
  transition: box-shadow 0.2s ease;
}
.agent-collapse-trigger:hover {
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.16);
}
.agent-collapse-trigger img {
  display: block;
  width: 14px;
  height: 14px;
  margin: 0 auto;
  object-fit: contain;
}
.page-header {
  position: relative;
  display: flex;
  height: 64px;
  flex: 0 0 64px;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-sizing: border-box;

  h1 {
    position: absolute;
    left: 50%;
    margin: 0;
    transform: translateX(-50%);
    color: #222527;
    font-size: 24px;
    line-height: 34px;
  }
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 1px solid #D4D8DD;
  border-radius: 4px;
  background: #FFFFFF;
  color: #878898;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.2s, color 0.2s, background-color 0.2s;

  &:hover {
    border-color: #007BFF;
    color: #007BFF;
    background: #F5F9FF;
  }

  .icon-title-back {
    font-size: 14px;
    line-height: 1;
  }
}

.download-button {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  padding: 0;
  border: 1px solid #DCEDFF;
  border-radius: 4px;
  background: #ffffff;
  color: #9096A2;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;

  &:not(:disabled):hover {
    border-color: #007BFF;
    background: #E0EEFA;
    color: #007BFF;
  }

  &:disabled {
    border-color: #E0EEFA;
    background: #ffffff;
    color: #A6ACB8;
    cursor: not-allowed;
  }

  i {
    width: 16px;
    height: 16px;
    font-size: 16px;
  }
}
/* 地图和报告共用页面横向边距，保证上下内容两端齐平。 */
.map-section {
  position: relative;
  min-height: 360px;
  flex: 1 1 54%;
  margin: 0 24px;
  overflow: hidden;
  border-radius: 8px;
  transition: height 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              flex 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.2s ease;

  &.is-dragging {
    transition: none;
  }

  &.is-collapsed {
    height: 0 !important;
    min-height: 0 !important;
    flex: 0 0 0 !important;
    margin-top: 0 !important;
    margin-bottom: 0 !important;
    opacity: 0 !important;
    pointer-events: none !important;
  }
}

.legend-panel {
  position: absolute;
  right: 20px;
  bottom: 20px;
  z-index: 5;
  display: flex;
  min-width: 112px;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 8px;
  background: rgba(34, 37, 39, 0.28);
  box-shadow: 0 8px 24px rgba(34, 37, 39, 0.18);
  backdrop-filter: blur(16px) saturate(140%);
  -webkit-backdrop-filter: blur(16px) saturate(140%);
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;

  strong {
    color: #ffffff;
    font-size: 12px;
  }

  div {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  i {
    width: 32px;
    height: 14px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-radius: 2px;
  }
}

.empty-state {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 37, 39, 0.2);
  color: #ffffff;
  font-size: 16px;
}

.report-section {
  position: relative;
  height: 280px;
  flex: 0 0 280px;
  margin: 0 24px 16px;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  transition: height 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              flex 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              margin-top 0.24s ease;

  &.is-custom-height,
  &.is-expanded {
    height: auto;
    flex: 1 1 auto;
  }

  &.is-expanded {
    margin-top: 6px;
  }
}

.report-scroll-wrapper {
  flex: 1 1 auto;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 18px 28px 24px;
  mask-image: linear-gradient(
    to bottom,
    transparent 0,
    #000000 16px,
    #000000 calc(100% - 16px),
    transparent 100%
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0,
    #000000 16px,
    #000000 calc(100% - 16px),
    transparent 100%
  );
}

.export-content {
  position: fixed;
  top: 0;
  left: -10000px;
  width: 800px;
  padding: 20px;
  background: #ffffff;
  box-sizing: border-box;

  > h1 {
    margin: 0 0 16px;
    color: #222527;
    font-size: 24px;
    text-align: center;
  }

  > img {
    display: block;
    width: 760px;
    height: auto;
    margin-bottom: 18px;
  }
}

@media (max-height: 800px) {
  .report-section {
    min-height: 140px;
  }
}

@media (max-width: 768px) {
  .page-header,
  .map-section,
  .report-section {
    margin-right: 16px;
    margin-left: 16px;
  }

  .page-header {
    padding: 0;

    h1 {
      font-size: 20px;
    }
  }

  .report-section {
    padding-right: 16px;
    padding-left: 16px;
  }
}
</style>
