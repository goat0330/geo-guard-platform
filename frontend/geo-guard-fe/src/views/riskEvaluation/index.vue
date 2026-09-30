<template>
  <div class="risk-evaluation-page">
    <!-- 风险概览与地图并排展示，左侧面板收起后地图自动扩展。 -->
    <section
      class="map-area"
      :class="{
        'is-left-collapsed': leftCollapsed,
        'is-right-panel-open': rightPanelExpanded,
      }"
      aria-label="地图区域"
    >
      <MarsMap
        ref="marsMapRef"
        @onload="handleMapLoaded"
        @risk-point-position="handleRiskPointPosition"
        @select-area="handleMapAreaSelect"
      >
        <template #overlay>
          <Transition name="risk-layer-status">
            <div
              v-if="mapLoadingText"
              class="risk-layer-loading"
              role="status"
              aria-live="polite"
            >
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>{{ mapLoadingText }}</span>
            </div>
          </Transition>
          <RiskSlopeMapPopup
            :visible="Boolean(selectedSlope && riskPointPosition)"
            :slope="selectedSlope || {}"
            :position="riskPointPosition"
            @close="closeSlopePopup"
            @risk-analysis="openRiskAnalysis"
          />
          <MapOverlayControls
            class="risk-map-controls"
            :class="{
              'is-detail-open': rightPanelExpanded,
            }"
            v-model:risk-levels="riskDataset.filters.dynamicRiskLevels"
            @zoom-in="marsMapRef?.handleZoomIn()"
            @zoom-out="marsMapRef?.handleZoomOut()"
            @locate-home="marsMapRef?.flyToHome()"
            @change-base-map="marsMapRef?.changeBaseMap($event)"
            @toggle-imagery-label="marsMapRef?.toggleImageryLabel($event)"
            @change-layer="marsMapRef?.handleLayerChange($event)"
          />
        </template>
      </MarsMap>

      <div class="left-panel-wrapper" :class="{ 'is-collapsed': leftCollapsed }">
        <div class="left-panel-stack" :class="{ 'is-collapsed': leftCollapsed }">
          <RiskOverviewPanel
            ref="riskOverviewPanelRef"
            class="stack-panel overview-panel-layer"
            :dataset="riskDataset"
            @show-detail="showSlopeList"
            @open-risk-report="openRiskReport"
            @select-area="handleOverviewAreaSelect"
            @date-change="handleOverviewDateChange"
          />
          <RiskSlopePanel
            class="stack-panel slope-panel-layer"
            :class="{ 'is-visible': panelMode === 'slope-list' }"
            :dataset="riskDataset"
            @back="showOverview"
            @locate="handleSlopeLocate"
            @risk-analysis="openRiskAnalysis"
          />
        </div>
      </div>

      <div
        class="right-panel-clip"
        :class="{ 'is-collapsed': rightPanelCollapsed }"
      >
        <RiskAnalysisAgent
          class="right-agent-panel"
          :class="{ 'is-collapsed': riskAnalysisCollapsed }"
          :visible="riskAnalysisVisible"
          :slope="selectedSlope || {}"
          @close="closeRiskAnalysis"
        />
        <RiskReportAgent
          class="right-agent-panel"
          :class="{ 'is-collapsed': riskReportCollapsed }"
          :visible="riskReportVisible"
          @close="closeRiskReport"
          @complete="openDynamicRiskReport"
        />
      </div>

    </section>

    <!-- 概览面板展开/收起按钮：置于 map-area 外层，避免被地图容器裁切，使按钮在收起时可居中跨越分界线 -->
    <button
      class="collapse-trigger left-trigger"
      :class="{ 'is-collapsed': leftCollapsed }"
      :title="leftCollapsed ? `展开${activePanelName}` : `收起${activePanelName}`"
      @click="toggleLeftCollapse"
    >
      <img :src="leftCollapsed ? rightArrow : leftArrow" alt="" />
    </button>

    <button
      v-if="rightPanelVisible"
      class="collapse-trigger right-trigger"
      :class="{ 'is-collapsed': rightPanelCollapsed }"
      type="button"
      :title="rightTriggerTitle"
      @click="toggleRightPanel"
    >
      <img :src="rightPanelCollapsed ? leftArrow : rightArrow" alt="" />
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import RiskOverviewPanel from '@/components/RiskOverviewPanel/index.vue'
import RiskSlopePanel from '@/components/RiskSlopePanel/index.vue'
import RiskSlopeMapPopup from '@/components/RiskSlopeMapPopup/index.vue'
import RiskAnalysisAgent from '@/components/RiskAnalysisAgent/index.vue'
import RiskReportAgent from '@/components/RiskReportAgent/index.vue'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'
import { useRiskDataset } from '@/components/RiskSlopePanel/useRiskDataset.js'
import { useDynamicRiskLayer } from '@/components/MarsMap/useDynamicRiskLayer.js'

defineOptions({ name: 'RiskEvaluation' })

const riskDataset = useRiskDataset()
const riskLayer = useDynamicRiskLayer(riskDataset, (slope) => handleSlopeLocate(slope))

const leftCollapsed = ref(false)
const panelMode = ref('overview')
const marsMapRef = ref(null)
const riskOverviewPanelRef = ref(null)
const selectedSlope = ref(null)
const riskPointPosition = ref(null)
const riskAnalysisVisible = ref(false)
const riskAnalysisCollapsed = ref(false)
const riskReportVisible = ref(false)
const riskReportCollapsed = ref(false)
const router = useRouter()

let isSyncingFromOverview = false

const handleOverviewAreaSelect = async (areaResult) => {
  const selected = areaResult?.selected
  if (!selected) return
  closeSlopePopup()
  closeRiskAnalysis()
  riskDataset.setRegion(selected)

  isSyncingFromOverview = true
  // 联动地图：通过 AreaSearch 飞行定位并重绘行政区划
  try {
    const areaSearch = marsMapRef.value?.getAreaSearch()
    if (areaSearch) {
      if (typeof areaSearch.handleSearchSelect === 'function') {
        await areaSearch.handleSearchSelect(selected.id || selected)
      } else if (typeof areaSearch.loadAndDrawBoundary === 'function') {
        await areaSearch.loadAndDrawBoundary(selected, true)
      }
      // 双重保障：确保平滑飞行定位立即响应
      if (typeof areaSearch.flyToRegionCenter === 'function') {
        areaSearch.flyToRegionCenter(selected)
      }
    }
  } catch (err) {
    console.warn('联动地图行政区划失败:', err)
  } finally {
    setTimeout(() => {
      isSyncingFromOverview = false
    }, 200)
  }
}

const handleMapAreaSelect = (data) => {
  if (data?.selected) {
    closeSlopePopup()
    closeRiskAnalysis()
    riskDataset.setRegion(data.selected)
  }
  if (isSyncingFromOverview) return
  // 图上选择了地区后，概览面板同步展示对应地区并重新调用接口获取数据
  if (data?.selected) {
    riskOverviewPanelRef.value?.selectArea(data)
  }
}

const handleOverviewDateChange = (date) => {
  if (date) {
    closeSlopePopup()
    closeRiskAnalysis()
    riskDataset.filters.assessmentDate = date
  }
}

const activePanelName = computed(() => panelMode.value === 'slope-list' ? '斜坡单元列表' : '风险概览')
const rightPanelVisible = computed(() => riskAnalysisVisible.value || riskReportVisible.value)
const rightPanelCollapsed = computed(() => (
  riskAnalysisVisible.value ? riskAnalysisCollapsed.value : riskReportCollapsed.value
))
const rightPanelExpanded = computed(() => rightPanelVisible.value && !rightPanelCollapsed.value)
const rightTriggerTitle = computed(() => {
  const panelName = riskAnalysisVisible.value ? '风险评价智能体' : '今日风险分析'
  return rightPanelCollapsed.value ? `展开${panelName}` : `收起${panelName}`
})
const mapLoadingText = computed(() => {
  if (riskDataset.initialLoading.value) return '正在获取动态风险数据...'
  if (riskLayer.rendering.value) return '正在渲染动态风险图层...'
  return ''
})


const toggleLeftCollapse = () => {
  leftCollapsed.value = !leftCollapsed.value
}

const showSlopeList = () => {
  leftCollapsed.value = false
  panelMode.value = 'slope-list'
  // 进入斜坡单元列表时：风险等级默认全选
  riskDataset.filters.dynamicRiskLevels = [4, 3, 2, 1]
}

const showOverview = () => {
  panelMode.value = 'overview'
  // 返回风险情况概览时：动态风险只选择极高和高
  riskDataset.filters.dynamicRiskLevels = [4, 3]
  closeSlopePopup()
  closeRiskAnalysis()
  selectedSlope.value = null
  riskDataset.setActiveSlope(null)
}

const handleSlopeLocate = (slope) => {
  const lng = Number(slope?.lng)
  const lat = Number(slope?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    closeSlopePopup()
    ElMessage.warning('当前斜坡单元暂无点位坐标')
    return
  }

  // 坐标有效且能够展示业务弹窗时，才设置选中并高亮
  selectedSlope.value = slope
  riskDataset.setActiveSlope(slope)
  riskPointPosition.value = null
  const fittedToSlope = riskLayer.flyToSlope(slope)
  marsMapRef.value?.showRiskPoint(
    { lng, lat, alt: Number(slope.alt) || 0 },
    {
      flyTo: !fittedToSlope,
      width: 30,
      height: 34,
      // 高亮面贴地绘制时关闭定位标记的深度检测，避免图标被覆盖。
      alwaysVisible: true,
    },
  )
}

const closeSlopePopup = () => {
  riskPointPosition.value = null
  marsMapRef.value?.clearRiskPoint()
  // 若右侧风险分析智能体弹窗未展开，则取消斜坡单元高亮
  if (!riskAnalysisVisible.value) {
    selectedSlope.value = null
    riskDataset.setActiveSlope(null)
  }
}

const openRiskAnalysis = (slope) => {
  if (slope) {
    selectedSlope.value = slope
    // 展开业务智能体分析弹窗时高亮该斜坡
    riskDataset.setActiveSlope(slope)
    riskLayer.flyToSlope(slope)
  }
  closeRiskReport()
  riskAnalysisVisible.value = true
  riskAnalysisCollapsed.value = false
}

const closeRiskAnalysis = () => {
  riskAnalysisVisible.value = false
  riskAnalysisCollapsed.value = false
  // 若地图气泡弹窗也未显示，关闭智能体弹窗时同步取消高亮
  if (!riskPointPosition.value) {
    selectedSlope.value = null
    riskDataset.setActiveSlope(null)
  }
}

const openRiskReport = () => {
  closeRiskAnalysis()
  riskReportVisible.value = true
  riskReportCollapsed.value = false
}

const closeRiskReport = () => {
  riskReportVisible.value = false
  riskReportCollapsed.value = false
}

const toggleRightPanel = () => {
  if (riskAnalysisVisible.value) {
    riskAnalysisCollapsed.value = !riskAnalysisCollapsed.value
    return
  }
  riskReportCollapsed.value = !riskReportCollapsed.value
}

const openDynamicRiskReport = () => {
  router.push({
    name: 'DynamicRiskReport',
    query: { date: dayjs().format('YYYY-MM-DD') },
  })
}

const handleRiskPointPosition = (position) => {
  if (!position) {
    riskPointPosition.value = null
    return
  }
  riskPointPosition.value = position
}

const handleMapLoaded = ({ viewer }) => {
  riskLayer.attach(viewer)
}</script>

<style lang="less" scoped>
.risk-evaluation-page {
  position: relative;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 100%;
  display: flex;
  overflow: visible;
  box-sizing: border-box;
  padding: 0;
  background: transparent;
  font-family: inherit;
}

.risk-evaluation-page :deep(input) {
  font-family: inherit;
}

/* 左侧面板与地图保留 12px 透明间距，露出底层背景图。 */
.map-area {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  min-height: 100%;
  padding-left: 472px;
  background: transparent;
  overflow: visible;
  box-sizing: border-box;
  transform: translateZ(0);
  isolation: isolate;
  transition: padding-left 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              padding-right 0.28s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-left-collapsed {
    padding-left: 0;
  }

  &.is-right-panel-open {
    padding-right: 0;

    :deep(.compass) {
      right: 487px;
    }
  }

  :deep(.compass) {
    transition: right 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.risk-layer-loading {
  position: absolute;
  top: 20px;
  left: 50%;
  z-index: 24;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #DCEDFF;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 16px rgba(34, 37, 39, 0.12);
  color: #222527;
  font-size: 14px;
  line-height: 20px;
  pointer-events: none;
  transform: translateX(-50%);

  .el-icon {
    width: 18px;
    height: 18px;
    color: #007BFF;
    font-size: 18px;
  }
}

.risk-layer-status-enter-active,
.risk-layer-status-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.risk-layer-status-enter-from,
.risk-layer-status-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}

/* 左侧面板裁剪容器：严格约束在 x >= 0 范围，彻底防止向左滑动收起时溢出覆盖菜单栏 */
.left-panel-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 480px;
  max-width: calc(100% - 32px);
  overflow: hidden;
  pointer-events: none;
  z-index: 10;
  visibility: visible;
  transition: visibility 0s linear 0s;

  &.is-collapsed {
    visibility: hidden;
    pointer-events: none;
    transition: visibility 0s linear 0.28s;
  }
}

/* 风险情况概览独立占据左侧，不再覆盖地图。 */
.left-panel-stack {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 10;
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-sizing: border-box;
  transform: translateX(0);
  opacity: 1;
  pointer-events: auto;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-collapsed {
    transform: translateX(-100%);
    opacity: 0;
    pointer-events: none;
  }
}

.stack-panel {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  background: #ffffff;
}

.overview-panel-layer {
  z-index: 1;
}

.slope-panel-layer {
  z-index: 2;
  opacity: 0;
  transform: translateX(-100%);
  pointer-events: none;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;

  &.is-visible {
    opacity: 1;
    transform: translateX(0);
    pointer-events: auto;
  }
}

/* 收起/展开通用按钮 */
.collapse-trigger {
  position: absolute;
  top: 50%;
  width: 20px;
  height: 72px;
  padding: 0;
  box-sizing: border-box;
  border: 1px solid #cccccc66;
  border-radius: 17px;
  background: #ffffff;
  cursor: pointer;
  transform: translateY(-50%);
}

.collapse-trigger:hover {
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.16);
}

.collapse-trigger img {
  display: block;
  width: 14px;
  height: 14px;
  margin: 0 auto;
  object-fit: contain;
}

/* 左侧概览面板收起按钮：使用与面板完全一致的 GPU transform 贝塞尔曲线，杜绝延迟脱节 */
.left-trigger {
  left: 0;
  z-index: 100;
  transform: translateY(-50%) translateX(450px);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.2s ease;
}

.left-trigger.is-collapsed {
  transform: translateY(-50%) translateX(-10px);
}

/* 右侧面板裁剪容器：盖在地图上，超出部分进行裁剪，防止页面产生横向滚动条 */
.right-panel-clip {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 500px;
  z-index: 15;
  overflow: hidden;
  pointer-events: none;
  visibility: visible;
  transition: visibility 0s linear 0s;

  &.is-collapsed {
    visibility: hidden;
    transition: visibility 0s linear 0.28s;
  }
}

/* 右侧智能体面板盖在地图上，交互与复盘详情保持一致，左侧圆角与阴影独立浮层展示 */
.right-agent-panel {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 480px;
  box-sizing: border-box;
  background: #ffffff;
  border-radius: 20px 0 0 20px !important;
  border: 1px solid #d5dbe0 !important;
  border-right: none !important;
  box-shadow: -4px 0 24px rgba(47, 95, 186, 0.12) !important;
  pointer-events: auto;
  opacity: 1;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.28s ease,
              visibility 0s linear 0s;

  :deep(.agent-header) {
    border-radius: 20px 0 0 0;
  }

  &.is-collapsed {
    visibility: hidden;
    opacity: 0;
    transform: translateX(100%);
    pointer-events: none;
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
                opacity 0.28s ease,
                visibility 0s linear 0.28s;
  }
}

.right-trigger {
  right: 0;
  z-index: 100;
  transform: translateY(-50%) translateX(-470px);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.2s ease;
}

.right-trigger.is-collapsed {
  transform: translateY(-50%) translateX(10px);
}

/* 地图右侧组件避让：当右侧弹窗打开时向左自适应平移，避免被右侧弹窗盖住 */
:deep(.risk-map-controls) {
  .filter-groups,
  .map-tools {
    transition: right 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &.is-detail-open {
    .filter-groups,
    .map-tools {
      right: 496px;
    }
  }
}

@media (max-width: 1366px) {
  .left-panel-wrapper {
    width: 480px;
  }

  .left-panel-stack {
    width: 460px;
  }

  .left-trigger {
    transform: translateY(-50%) translateX(450px);
  }

  .left-trigger.is-collapsed {
    transform: translateY(-50%) translateX(-10px);
  }

  .right-panel-clip {
    width: 460px;
  }

  .right-agent-panel {
    width: 440px !important;
  }

  .right-trigger {
    transform: translateY(-50%) translateX(-430px);

    &.is-collapsed {
      transform: translateY(-50%) translateX(10px);
    }
  }

  :deep(.risk-map-controls.is-detail-open) {
    .filter-groups,
    .map-tools {
      right: 456px;
    }
  }

  .map-area.is-right-panel-open {
    :deep(.compass) {
      right: 447px;
    }
  }
}
</style>
