<template>
  <div class="group-defense-page" :class="{ 'is-process': processPanelVisible }">
    <section
      class="map-stage"
      :class="{
        'is-left-collapsed': !leftPanelVisible,
        'has-right-panel': processPanelVisible,
      }"
      aria-label="群测群防地图"
    >
      <MarsMap
        ref="marsMapRef"
        @onload="handleMapLoaded"
        @risk-point-position="handleRiskPointPosition"
        @select-area="handleAreaSelect"
        @area-loaded="handleAreaLoaded"
      >
        <template #overlay>
          <Transition name="map-popup">
            <GroupDefenseMapPopup
              v-if="selectedReport && mapPopupVisible && reportPopupPosition"
              ref="reportPopupRef"
              class="report-map-popup"
              :class="{ 'is-below': reportPopupBelow }"
              :style="reportPopupStyle"
              :report="selectedReport"
              :photos="photoUrls"
              @close="closeMapPopup"
            />
          </Transition>
        </template>
      </MarsMap>
      <MapOverlayControls
        v-model:risk-levels="riskDataset.filters.dynamicRiskLevels"
        :class="{ 'has-right-panel': processPanelVisible }"
        @zoom-in="marsMapRef?.handleZoomIn()"
        @zoom-out="marsMapRef?.handleZoomOut()"
        @locate-home="marsMapRef?.flyToHome()"
        @change-base-map="marsMapRef?.changeBaseMap($event)"
        @toggle-imagery-label="marsMapRef?.toggleImageryLabel($event)"
        @change-layer="marsMapRef?.handleLayerChange($event)"
      />

      <aside class="left-panel" :class="{ 'is-hidden': !leftPanelVisible }">
        <Transition name="panel-swap" mode="out-in">
          <GroupDefenseOverview
            v-if="activeView === 'overview'"
            key="overview"
            :report-range="dashboardRange"
            :summary="summary"
            :rankings="rankings"
            @date-change="handleDashboardRange"
            @show-reports="switchView('reports')"
          />
          <GroupDefenseReportList
            v-else
            key="reports"
            :reports="reports"
            :total="total"
            :loading="listLoading"
            :loading-more="listLoadingMore"
            :finished="listFinished"
            :selected-id="selectedReport?.id"
            @back="switchView('overview')"
            @query="fetchReports"
            @load-more="loadMoreReports"
            @select-report="selectReport"
            @analyze-report="openProcess"
          />
        </Transition>
      </aside>

      <Transition name="right-panel">
        <GroupDefenseReportDetail
          v-if="processPanelVisible"
          class="detail-panel"
          :report="selectedReport"
          :photos="photoUrls"
          :loading="detailLoading"
          @close="closeProcess"
          @feedback="feedbackDialogVisible = true"
          @send="sendToResponsiblePerson"
        />
      </Transition>
    </section>

    <button
      class="panel-toggle"
      :class="{ 'is-hidden': !leftPanelVisible }"
      type="button"
      :title="leftPanelVisible ? '收起业务面板' : '展开业务面板'"
      @click="leftPanelVisible = !leftPanelVisible"
    >
      <img :src="leftPanelVisible ? leftArrow : rightArrow" alt="" />
    </button>

    <GroupDefenseFeedbackDialog
      v-model="feedbackDialogVisible"
      :report="selectedReport"
      :submitting="feedbackSubmitting"
      @submit="submitFeedback"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import GroupDefenseOverview from '@/components/GroupDefenseOverview/index.vue'
import GroupDefenseReportList from '@/components/GroupDefenseReportList/index.vue'
import GroupDefenseReportDetail from '@/components/GroupDefenseReportDetail/index.vue'
import GroupDefenseMapPopup from '@/components/GroupDefenseMapPopup/index.vue'
import GroupDefenseFeedbackDialog from '@/components/GroupDefenseFeedbackDialog/index.vue'
import { useDynamicRiskLayer } from '@/components/MarsMap/useDynamicRiskLayer.js'
import { useRiskDataset } from '@/components/RiskSlopePanel/useRiskDataset.js'
import { getCenter, getPhotoUrls } from '@/utils/index.js'
import {
  submitDisReportSuggestion,
  getDisReportDetail,
  getPublicDisasterDashboard,
  handleDisReport,
  taskDistList,
} from '@/api/reportDisaster.js'
import { AREA_LEVEL } from '@/utils/enum.js'
import { buildRangeParams } from '@/utils/dateRange.js'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'

defineOptions({ name: 'GroupDefensePage' })

const riskDataset = useRiskDataset()
const riskLayer = useDynamicRiskLayer(riskDataset)
const route = useRoute()
const router = useRouter()
const marsMapRef = ref(null)
const leftPanelVisible = ref(true)
const reports = ref([])
const total = ref(0)
const listLoading = ref(false)
const listLoadingMore = ref(false)
const listFinished = ref(false)
const loadedListPage = ref(1)
const selectedReport = ref(null)
const photoUrls = ref([])
const mapPopupVisible = ref(true)
const reportPopupPosition = ref(null)
const reportPopupRef = ref(null)
const detailLoading = ref(false)
const processPanelVisible = ref(false)
const feedbackDialogVisible = ref(false)
const feedbackSubmitting = ref(false)
let photoRequestId = 0
let dashboardRequestId = 0
let listRequestId = 0
/** 群众报灾看板的统计时段，空数组表示全部 */
const dashboardRange = ref([])
const dashboardRegionParams = ref({})
const summary = ref({})
const rankings = ref([])
const lastListQuery = ref({ pageNum: 1, pageSize: 20, orderByColumn: 'createDate', isAsc: 'desc' })
const activeView = computed(() => {
  const view = route.query.view
  return ['reports', 'process'].includes(view) ? view : 'overview'
})
// 报灾卡片比斜坡卡片高，点位居中时要按地图上下剩余空间选择展示方向。
const popupSpace = computed(() => {
  const y = reportPopupPosition.value?.y
  const height = marsMapRef.value?.getViewer()?.container?.clientHeight
  if (!Number.isFinite(y) || !height) return null
  return {
    above: Math.max(0, y - 38 - 16),
    below: Math.max(0, height - y - 20 - 16),
  }
})
const reportPopupBelow = computed(() => Boolean(
  popupSpace.value && popupSpace.value.above < 420 && popupSpace.value.below > popupSpace.value.above,
))
const getPopupHorizontalBounds = () => {
  const mapContainer = marsMapRef.value?.getViewer()?.container
  const popupWidth = reportPopupRef.value?.$el?.offsetWidth || 400
  const popupHalfWidth = popupWidth / 2
  const mapRect = mapContainer?.getBoundingClientRect()
  const controlsRect = processPanelVisible.value
    ? mapContainer?.closest('.map-stage')?.querySelector('.filter-groups')?.getBoundingClientRect()
    : null
  // 控件会随面板移动，直接使用实际位置，避免 rem 缩放后弹窗压住控件。
  const safeRight = controlsRect && mapRect
    ? controlsRect.left - mapRect.left - 16
    : (mapContainer?.clientWidth || popupWidth + 32) - 16
  const minCenter = popupHalfWidth + 16
  const maxCenter = Math.max(minCenter, safeRight - popupHalfWidth)
  return { minCenter, maxCenter, mapWidth: mapContainer?.clientWidth }
}
const reportPopupStyle = computed(() => {
  const position = reportPopupPosition.value
  if (!position) return undefined
  const below = reportPopupBelow.value
  const { minCenter, maxCenter } = getPopupHorizontalBounds()
  const popupX = Math.min(Math.max(position.x, minCenter), maxCenter)
  return {
    left: `${popupX}px`,
    top: `${position.y + (below ? 20 : -38)}px`,
    maxHeight: popupSpace.value ? `${below ? popupSpace.value.below : popupSpace.value.above}px` : undefined,
  }
})

const handleRiskPointPosition = (position) => {
  if (!position || !Number.isFinite(position.x) || !Number.isFinite(position.y)) {
    reportPopupPosition.value = null
    return
  }
  reportPopupPosition.value = position
}

const handleMapLoaded = ({ viewer }) => {
  riskLayer.attach(viewer)
}

const getDashboardRegionParams = (selected) => {
  const code = selected?.id
  if (code === null || code === undefined || code === '') return {}

  switch (selected?.level) {
    case AREA_LEVEL.COUNTY:
    case 'county':
    case 'district':
      return { countyCode: code }
    case AREA_LEVEL.STREET:
    case 'street':
    case 'town':
      return { streetCode: code }
    case AREA_LEVEL.VILLAGE:
    case 'village':
      return { villageCode: code }
    default:
      return {}
  }
}

// 行政区划选择器的当前层级决定看板接口使用的区域参数。
const updateDashboardRegion = (selected) => {
  dashboardRegionParams.value = getDashboardRegionParams(selected)
  riskDataset.setRegion(selected)
  fetchDashboard()
  fetchReports(lastListQuery.value)
}

const handleAreaSelect = ({ selected } = {}) => {
  updateDashboardRegion(selected)
}

const handleAreaLoaded = ({ root } = {}) => {
  updateDashboardRegion(root)
}

const closeMapPopup = () => {
  mapPopupVisible.value = false
  reportPopupPosition.value = null
  marsMapRef.value?.clearRiskPoint()
}

const switchView = (view, reportId) => {
  const query = { ...route.query }
  delete query.preview
  if (view !== 'process') {
    // 概览和报送列表默认不展示报文面板，点击具体报送记录后再打开。
    selectedReport.value = null
    mapPopupVisible.value = false
    reportPopupPosition.value = null
    marsMapRef.value?.clearRiskPoint()
  }
  if (view === 'overview') {
    delete query.view
    delete query.id
  } else {
    query.view = view
    if (reportId !== null && reportId !== undefined && reportId !== '') query.id = String(reportId)
    else if (view !== 'process') delete query.id
  }
  router.replace({ query })
}

const fetchReports = async (query = lastListQuery.value, { append = false } = {}) => {
  const listQuery = { ...query }
  delete listQuery.countyCode
  delete listQuery.streetCode
  delete listQuery.villageCode
  const pageSize = Number(listQuery.pageSize) || 20
  const pageNum = append ? Number(listQuery.pageNum) || loadedListPage.value + 1 : 1
  listQuery.pageNum = pageNum
  listQuery.pageSize = pageSize
  if (!append) {
    lastListQuery.value = { ...listQuery, pageNum: 1 }
    listFinished.value = false
  }
  const currentRequestId = ++listRequestId
  listLoading.value = true
  listLoadingMore.value = append
  try {
    const response = await taskDistList({
      ...listQuery,
      ...dashboardRegionParams.value,
    })
    if (currentRequestId !== listRequestId) return
    if (!response || !Array.isArray(response.data)) throw new Error(response?.msg || '报送列表返回结构异常')
    const nextReports = append ? [...reports.value, ...response.data] : response.data
    const nextTotal = Number(response.total) || 0
    reports.value = nextReports
    total.value = nextTotal
    loadedListPage.value = pageNum
    listFinished.value = response.data.length < pageSize || (nextTotal > 0 && nextReports.length >= nextTotal)
    if (!append) {
      selectedReport.value = null
      mapPopupVisible.value = false
      reportPopupPosition.value = null
      marsMapRef.value?.clearRiskPoint()
    }
  } catch (error) {
    if (currentRequestId !== listRequestId) return
    if (!append) {
      reports.value = []
      total.value = 0
      loadedListPage.value = 1
      listFinished.value = true
    }
    console.error('获取报送列表失败', error)
  } finally {
    if (currentRequestId === listRequestId) {
      listLoading.value = false
      listLoadingMore.value = false
    }
  }
}

// 仅在上一页成功后推进页码，失败时保留当前列表并允许再次触发。
const loadMoreReports = () => {
  if (listLoading.value || listFinished.value) return
  fetchReports(
    {
      ...lastListQuery.value,
      pageNum: loadedListPage.value + 1,
    },
    { append: true },
  )
}

/** 时段变化：先记录选中区间（供刷新/切换区划时复用），再按新时段取数 */
const handleDashboardRange = (range) => {
  dashboardRange.value = range
  fetchDashboard(range)
}

/**
 * 群众报灾看板取数
 * @param {Array} [range] 统计时段 [开始日期, 结束日期]，空数组表示全部（不传日期）
 */
const fetchDashboard = async (range = dashboardRange.value) => {
  const currentRequestId = ++dashboardRequestId
  try {
    const data = await getPublicDisasterDashboard({
      ...buildRangeParams(range),
      ...dashboardRegionParams.value,
    })
    if (currentRequestId !== dashboardRequestId) return
    summary.value = {
      totalCount: data?.today?.totalCount,
      reporterCount: data?.today?.reporterCount,
      townCount: data?.today?.townCount,
      photoCount: data?.today?.imageReportCount,
      aiPendingCount: data?.today?.aiPendingCount,
      pending: data?.processing?.pendingCount,
      processing: data?.processing?.processingCount,
      completed: data?.processing?.completedCount,
      analyzedCount: data?.ai?.analyzedCount,
      confirmedCount: data?.manualConfirmation?.confirmedCount,
      manualConfirmationPendingCount: data?.manualConfirmation?.pendingCount,
      confirmationRate: data?.manualConfirmation?.confirmationRate,
      veryHighRisk: data?.ai?.veryHighRiskCount,
      highRisk: data?.ai?.highRiskCount,
      middleRisk: data?.ai?.mediumRiskCount,
      lowRisk: data?.ai?.lowRiskCount,
      categoryItems: Array.isArray(data?.deformationSigns)
        ? data.deformationSigns
            .map((item, index) => ({
              name: item?.signName,
              value: item?.reportCount,
              color: ['#91B7FF', '#F3A56E', '#F4D769', '#F2E49F', '#88D1B3', '#78A8F8'][index % 6],
            }))
            .filter((item) => item.name)
        : [],
    }
    rankings.value = Array.isArray(data?.townRankings)
      ? data.townRankings.map((item) => ({ rank: item?.rank, name: item?.townName, value: item?.reportCount }))
      : []
  } catch (error) {
    if (currentRequestId !== dashboardRequestId) return
    summary.value = {}
    rankings.value = []
    console.error('获取群众报灾看板失败', error)
  }
}

const getReportPoint = (checkCenter) => {
  const point = getCenter(checkCenter)
  const lng = Number(point?.lng)
  const lat = Number(point?.lat)
  const alt = Number(point?.height)

  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  if (lng < -180 || lng > 180 || lat < -90 || lat > 90) return null

  return {
    lng,
    lat,
    alt: Number.isFinite(alt) ? alt : 0,
  }
}

const locateReport = async (report) => {
  const point = getReportPoint(report?.checkCenter)
  reportPopupPosition.value = null
  marsMapRef.value?.clearRiskPoint()

  if (!point) {
    ElMessage.warning('该报送记录缺少有效坐标，无法定位')
    return
  }

  try {
    // 与斜坡单元定位一致：保留当前航向和俯仰角，平滑聚焦报灾点。
    await marsMapRef.value?.showRiskPoint(point, {
      alwaysVisible: true,
      keepView: true,
      duration: 1.5,
      flyToOptions: processPanelVisible.value ? {
        screenTargetX: () => {
          const { minCenter, maxCenter, mapWidth } = getPopupHorizontalBounds()
          return Math.min(Math.max((mapWidth || 0) / 2, minCenter), maxCenter)
        },
      } : undefined,
    })
  } catch (error) {
    console.error('定位报送点位失败', error)
    ElMessage.error('地图定位失败，请稍后重试')
  }
}

const selectReport = (report) => {
  if (!report || typeof report !== 'object') return
  selectedReport.value = report
  mapPopupVisible.value = true
  locateReport(report)
}

const openProcess = (report) => {
  if (report?.id === null || report?.id === undefined || report?.id === '') return
  processPanelVisible.value = true
  selectReport(report)
  switchView('process', report.id)
}

const closeProcess = () => {
  processPanelVisible.value = false
  switchView('reports')
}

const fetchReportDetail = async (reportId) => {
  if (reportId === null || reportId === undefined || reportId === '') return
  detailLoading.value = true
  try {
    selectedReport.value = await getDisReportDetail(reportId)
  } catch (error) {
    selectedReport.value = null
    console.error('获取报灾详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

const loadReportPhotos = async (photos) => {
  const currentRequestId = ++photoRequestId
  if (!photos || (Array.isArray(photos) && !photos.length)) {
    photoUrls.value = []
    return
  }
  try {
    const urls = await getPhotoUrls(photos)
    if (currentRequestId === photoRequestId) photoUrls.value = Array.isArray(urls) ? urls : []
  } catch (error) {
    console.error('获取报灾图片失败', error)
    if (currentRequestId === photoRequestId) photoUrls.value = []
  }
}

const refreshCurrentReport = async () => {
  await Promise.all([fetchReports(lastListQuery.value), fetchDashboard()])
  if (route.query.id) await fetchReportDetail(route.query.id)
}

const submitFeedback = async (payload) => {
  feedbackSubmitting.value = true
  try {
    await submitDisReportSuggestion(payload)
    feedbackDialogVisible.value = false
    ElMessage.success('反馈提交成功')
  } finally {
    feedbackSubmitting.value = false
  }
}

const sendToResponsiblePerson = async (report) => {
  if (!report?.id || detailLoading.value) return
  detailLoading.value = true
  try {
    await handleDisReport(report.id)
    ElMessage.success('已发送给责任人')
    await refreshCurrentReport()
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => route.query.id,
  (reportId) => {
    if (processPanelVisible.value) fetchReportDetail(reportId)
  },
  { immediate: true },
)

watch(activeView, (view) => {
  if (view !== 'process') processPanelVisible.value = false
})

watch(() => selectedReport.value?.photos, loadReportPhotos, { immediate: true })
</script>

<style lang="less" scoped>
.group-defense-page {
  --side-panel-width: 460px;
  --map-panel-gap: 12px;
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  overflow: visible;
  background: transparent;
  font-family: inherit;
}
.map-stage {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100%;
  padding-left: calc(var(--side-panel-width) + var(--map-panel-gap));
  overflow: hidden;
  box-sizing: border-box;
  border-radius: 16px;
  background: transparent;
  isolation: isolate;
  transform: translateZ(0);
  transition:
    padding-left 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    padding-right 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.map-stage.is-left-collapsed {
  padding-left: 0;
}
/* 智能体面板以浮层形式覆盖地图，不再为其预留右侧布局空间。 */
.map-stage.has-right-panel {
  padding-right: 0;
}
.left-panel,
.detail-panel {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 10;
  width: var(--side-panel-width);
  max-width: calc(100% - 32px);
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  box-shadow: 4px 0 20px rgba(0, 32, 80, 0.12);
}
.left-panel {
  left: 0;
  transform: translateX(0);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.left-panel.is-hidden {
  pointer-events: none;
  transform: translateX(-100%);
}
.detail-panel {
  right: 0;
  box-shadow: -4px 0 20px rgba(0, 32, 80, 0.12);
}
.panel-toggle {
  position: absolute;
  top: 50%;
  left: 0;
  z-index: 100;
  display: flex;
  width: 20px;
  height: 72px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid rgba(204, 204, 204, 0.4);
  border-radius: 17px;
  background: #fff;
  cursor: pointer;
  transform: translateY(-50%) translateX(450px);
  transition:
    transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.2s ease;
}
.panel-toggle.is-hidden {
  transform: translateY(-50%) translateX(-10px);
}
.panel-toggle:hover {
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.18);
}
.panel-toggle img {
  width: 14px;
  height: 14px;
  object-fit: contain;
}
.report-map-popup {
  position: absolute;
  z-index: 9;
  overflow-y: auto;
  transform: translate(-50%, -100%);
}
.report-map-popup.is-below {
  transform: translate(-50%, 0);
}
:deep(.map-overlay-controls .filter-groups),
:deep(.map-overlay-controls .map-tools) {
  transition: right 0.28s ease;
}
:deep(.map-overlay-controls.has-right-panel .filter-groups),
:deep(.map-overlay-controls.has-right-panel .map-tools) {
  right: 476px;
}
.panel-swap-enter-active,
.panel-swap-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.22s ease;
}
.panel-swap-enter-from {
  opacity: 0;
  transform: translateX(18px);
}
.panel-swap-leave-to {
  opacity: 0;
  transform: translateX(-18px);
}
.right-panel-enter-active,
.right-panel-leave-active {
  transition:
    transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease;
}
.right-panel-enter-from,
.right-panel-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
.map-popup-enter-active,
.map-popup-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.24s ease;
}
.map-popup-enter-from,
.map-popup-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}
.map-popup-enter-from.is-below,
.map-popup-leave-to.is-below {
  transform: translate(-50%, 12px);
}
@media (max-width: 1366px) {
  .report-map-popup {
    width: 340px;
  }
}
</style>
