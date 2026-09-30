<template>
  <div class="emergency-page">
    <!-- 地图区域：智能预案面板浮动在左侧上层 -->
    <section class="map-area" :class="{ 'is-left-collapsed': leftCollapsed }" aria-label="应急处置地图">
      <!-- 地图内容区：左侧为面板预留空间，面板收起时铺满 -->
      <div class="map-content">
        <MarsMap ref="marsMapRef" @onload="handleMapLoaded" @risk-point-position="handleRiskPointPosition">
          <!-- 点位详情弹窗：必须置于 MarsMap 的 overlay 插槽内，与 Cesium 画布同一定位容器，
               坐标取画布像素坐标，随地图平移缩放实时跟随选中的预案点位 -->
          <template #overlay>
            <div v-if="popupVisible && riskPointPosition" class="point-popup-layer" :style="popupLayerStyle">
              <EmergencyPointPopup
                v-if="popupData.metrics"
                :data="popupData"
                @close="handleClosePopup"
                @view-result="handlePopupViewResult"
              />
              <EmergencyGeneratePopup
                v-else
                :data="popupData"
                @close="handleClosePopup"
                @view-result="handlePopupViewResult"
                @view-analysis="handlePopupViewAnalysis"
              />
            </div>
          </template>
        </MarsMap>

        <MapOverlayControls
          v-model:risk-levels="dynamicRiskLevels"
          @zoom-in="marsMapRef?.handleZoomIn()"
          @zoom-out="marsMapRef?.handleZoomOut()"
          @locate-home="marsMapRef?.flyToHome()"
          @change-base-map="marsMapRef?.changeBaseMap($event)"
          @toggle-imagery-label="marsMapRef?.toggleImageryLabel($event)"
        />

        <!-- 勾划范围：绘制工具条（点击列表「去勾划范围」后出现） -->
        <Transition name="draw-toolbar">
          <div v-if="drawingMode" class="draw-toolbar">
            <button
              v-for="tool in drawTools"
              :key="tool.key"
              class="tool-item"
              :class="{ 'is-primary': tool.key === 'draw' }"
              type="button"
              @click="handleDrawTool(tool.key)"
            >
              <el-icon class="tool-icon">
                <component :is="toolIcons[tool.key]" />
              </el-icon>
              {{ tool.label }}
            </button>
          </div>
        </Transition>

        <!-- 勾划范围确认弹窗 -->
        <RangeConfirmPopup
          v-if="confirmVisible"
          class="range-confirm-layer"
          :title="rangeConfirm.title"
          :name="drawPointName"
          :icon="drawIcon"
          :area="rangeArea"
          :area-label="rangeConfirm.areaLabel"
          :tags="rangeConfirm.tags"
          :actions="rangeConfirm.actions"
          @cancel="confirmVisible = false"
          @redraw="handleRedraw"
          @confirm="handleRangeConfirm"
        />
      </div>

      <!-- 左侧面板：智能预案，与地图并排展示，收起时向左滑出 -->
      <div class="left-panel-wrapper" :class="{ 'is-collapsed': leftCollapsed }">
        <div class="left-panel-stack" :class="{ 'is-collapsed': leftCollapsed }">
          <EmergencyPlanPanel
            ref="planPanelRef"
            @select="handleSelectPlan"
            @action="handleCardAction"
            @tab-change="handlePlanTabChange"
          />
        </div>
      </div>

    </section>

    <!-- 面板收起/展开按钮：置于 map-area 外层，避免被地图容器裁切 -->
    <button
      class="collapse-trigger left-trigger"
      :class="{ 'is-collapsed': leftCollapsed }"
      :title="leftCollapsed ? '展开智能预案' : '收起智能预案'"
      @click="leftCollapsed = !leftCollapsed"
    >
      <img :src="leftCollapsed ? rightArrow : leftArrow" alt="" />
    </button>

    <!-- 执行结果：白色遮挡层覆盖整个页面内容区（含会商入口），层内为「结果 + 执行过程」，不挤压地图 -->
    <Transition name="dialog-fade">
      <div v-if="resultVisible" class="execute-layer">
        <div class="execute-mask" @click="closeExecuteResult"></div>
        <div class="execute-content">
          <ExecuteResultDialog
            class="execute-dialog-layer"
            :data="resultData"
            :status="resultStatus"
            :loading-tip="loadingTip"
            @close="closeExecuteResult"
            @action="handleResultAction"
            @capture="handleRouteSnapshot"
          />
          <Transition name="right-panel">
            <ExecuteProcessPanel
              v-if="resultProcessVisible"
              class="execute-process-layer"
              :data="resultProcessData"
              :stream="resultProcessStream"
              @close="handleProcessClose"
              @progress="handleProcessProgress"
              @complete="handleProcessComplete"
            />
          </Transition>
          <!-- 执行过程折叠/展开：镜像左侧面板的收起按钮，收起后可再展开 -->
          <button
            class="process-trigger"
            :class="{ 'is-collapsed': !resultProcessVisible }"
            type="button"
            :title="resultProcessVisible ? '收起执行过程' : '展开执行过程'"
            @click="toggleProcessPanel"
          >
            <img :src="resultProcessVisible ? rightArrow : leftArrow" alt="" />
          </button>
        </div>
      </div>
    </Transition>

    <EvacuationRouteDialog
      :visible="routeDialogVisible"
      :data="evacuationRouteData"
      @close="routeDialogVisible = false"
    />

    <!-- 预案详情查看弹窗：数据取自案件详情接口（hazard / emergency case-detail） -->
    <PlanDetailDialog v-model:visible="planDetailVisible" :data="planDetailData" :loading="planDetailLoading" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, EditPen, RefreshLeft, SwitchButton } from '@element-plus/icons-vue'
import * as Cesium from 'mars3d-cesium'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import EmergencyPlanPanel from './components/EmergencyPlanPanel.vue'
import EmergencyPointPopup from './components/EmergencyPointPopup.vue'
import EmergencyGeneratePopup from './components/EmergencyGeneratePopup.vue'
import RangeConfirmPopup from './components/RangeConfirmPopup.vue'
import ExecuteProcessPanel from './components/ExecuteProcessPanel.vue'
import ExecuteResultDialog from './components/ExecuteResultDialog.vue'
import PlanDetailDialog from './components/PlanDetailDialog.vue'
import EvacuationRouteDialog from '@/components/EvacuationRouteDialog/index.vue'
import {
  PLAN_SCENE,
  PLAN_STATUS,
  fetchLatestRoute,
  fetchPlanCaseDetail,
  generateIntelligentPlan,
  pollLatestRoute,
  resolveCaseDetail,
  resolveRoutePayload,
} from '@/api/intelligentPlan.js'
import { EMPTY_CASE_CARD, buildPlanCaseCard } from './caseDetail.js'
import { buildPlanCardDocx } from './planCardDocx.js'
import {
  buildExecuteProcess,
  buildExecuteResult,
  drawTools,
  EMPTY_ROUTE_RESULT,
  emergencyPointDetail,
  emergencyStatusMap,
  generatedPointDetail,
  hazardAiStatusMap,
  rangeConfirm,
} from './config.js'
import { saveAs } from 'file-saver'
import { formatFileSize } from '@/utils/index.js'
import { DEFAULT_DYNAMIC_RISK_LEVELS } from '@/utils/riskEvaluation.js'
import mapPointUpdate from '@/assets/imgs/emergency/map-point-update.png'
import mapPointCheck from '@/assets/imgs/emergency/map-point-check.png'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'

defineOptions({ name: 'EmergencyPage' })

/** 绘制工具条图标 */
const toolIcons = {
  draw: EditPen,
  undo: RefreshLeft,
  remove: Delete,
  exit: SwitchButton,
}

const marsMapRef = ref(null)
/** mars3d 的 Map 与命名空间对象体量大，且无需响应式，用 shallowRef 存放 */
const mapInstance = shallowRef(null)
const mars3dNs = shallowRef(null)
const leftCollapsed = ref(false)
/** 地图「动态风险」勾选等级：默认只展示极高风险与高风险 */
const dynamicRiskLevels = ref([...DEFAULT_DYNAMIC_RISK_LEVELS])

/** 点位详情弹窗默认隐藏，点击预案卡片选中后再显示 */
const popupVisible = ref(false)
/** 弹窗当前展示的点位详情：带 metrics 走「已有预案更新」弹窗，否则走「突发灾情预案生成」弹窗 */
const popupData = ref({ ...emergencyPointDetail })

/** 选中预案点位在画布中的像素坐标，由 MarsMap 的 risk-point-position 事件持续回抛 */
const riskPointPosition = ref(null)

/** 点位标记尺寸：图标原图 72x72 正方形，等比缩放为 40x40，弹窗底边与图标顶部留 10px */
const MARKER_HEIGHT = 40
const MARKER_GAP = 10

/** 弹窗定位：跟随地图点位，LeftTop 由画布像素坐标换算 */
const popupLayerStyle = computed(() => {
  const position = riskPointPosition.value
  if (!position) {
    return {}
  }
  return {
    left: `${position.x}px`,
    top: `${position.y - MARKER_HEIGHT - MARKER_GAP}px`,
  }
})

/** 「执行结果」遮挡层是否展示（查看更新结果与查看分析过程都走该大弹窗） */
const resultVisible = ref(false)

/** 撤离路线详情弹窗，仅由执行结果中的“查看路线详情”入口打开。 */
const routeDialogVisible = ref(false)

/** 撤离路线详情数据：由 GET /route/latest 返回，暂无数据时保持空列表 */
const evacuationRouteData = ref({ disasterPolygonsWktList: [], resettlementAreas: [], routes: [] })

/** 当前选中的预案来源（场景 + sourceId），撤离路线的生成与查询都依赖它；sourceId 全程字符串 */
const currentSource = ref(null)

/** 当前预案卡片数据（含列表行 raw，用于结果弹窗展示），与 currentSource 同步更新 */
const currentPlan = ref(null)

/** 左侧预案列表面板：生成结束后调用其 refreshList 同步卡片状态 */
const planPanelRef = ref(null)

/** 是否正在调用生成接口（生成 + 轮询期间保持 loading 状态由该标识控制） */
const generating = ref(false)

/** 轮询中止标识：页面卸载时置 true，避免组件销毁后仍在请求 */
const pollingStopped = ref(false)

/** 遮挡层内的「执行过程」面板（结果弹窗的副本，只在遮挡层内排布） */
const resultProcessVisible = ref(false)

/** 遮挡层内展示的结果与执行过程数据（按页签选择：已有预案更新 / 突发险情预案生成） */
const resultData = ref(buildExecuteResult({ isHazard: true }))
const resultProcessData = ref(buildExecuteProcess({ isHazard: true }))

/** 「查看预案详情」弹窗：数据取自案件详情接口（hazard / emergency case-detail） */
const planDetailVisible = ref(false)
const planDetailData = ref(EMPTY_CASE_CARD)
/** 案件详情加载中：接口返回前弹窗保持加载态 */
const planDetailLoading = ref(false)
/** 详情请求序号：连续切换预案时以最后一次点击的结果为准 */
let planDetailSeq = 0

/** 遮挡层内左侧结果的展示阶段：
 *  done 直接全量展示；loading 等待右侧执行过程流式加载完成（查看分析过程 / 生成中） */
const resultStatus = ref('done')

/** 右侧执行过程是否流式加载：仅「查看分析过程 / 生成中」走流式，「查看更新结果」直接全量展示 */
const resultProcessStream = ref(false)

/** 结果数据是否已就绪：生成中（轮询未完成）为 false，其余入口为 true */
const routeReady = ref(true)

/** 右侧执行过程是否已播放完成：与 routeReady 同时满足才切到完成态 */
const processDone = ref(false)

/** 右侧执行过程流式进度：{ revealed, total } */
const processProgress = ref({ revealed: 0, total: 0 })

/** 左侧加载提示：算法计算中提示等待，流式播放中展示执行过程进度 */
const loadingTip = computed(() => {
  if (!routeReady.value) {
    return '正在计算撤离路线，生成过程中可查看右侧执行详情'
  }
  const { revealed, total } = processProgress.value
  return total ? `已完成 ${revealed}/${total} 个步骤，生成过程中可查看右侧执行详情` : ''
})

/** 勾划范围：绘制模式、已绘图形、面积与确认弹窗 */
const drawingMode = ref(false)
const drawnGraphics = shallowRef([])
const rangeGraphic = shallowRef(null)
const confirmVisible = ref(false)
const rangeArea = ref('0km²')
const drawPointName = ref('')
const drawIcon = ref('')

const EARTH_RADIUS = 6371

/** 地图点位标记图标：已有预案更新与突发灾情各用一枚 pin 图 */
const pointMarkerMap = {
  update: mapPointUpdate,
  generate: mapPointCheck,
}

/** 记录当前预案来源与卡片数据：点击卡片主体或直接点卡片操作按钮都会调用，
 *  避免未选中卡片就操作（如直接点「去勾划范围」后确认生成）时来源为空 */
const applyPlanSource = (item) => {
  if (!item?.scene) {
    return
  }
  currentSource.value = { scene: item.scene, sourceId: String(item.sourceId ?? '') }
  currentPlan.value = item
}

/** 取卡片数据的经纬度：接口返回字符串，统一转数值后再校验 */
const resolvePlanPoint = (item) => {
  const lng = Number(item?.longitude)
  const lat = Number(item?.latitude)
  return Number.isFinite(lng) && Number.isFinite(lat) ? { lng, lat } : null
}

/** 地图回抛点位像素坐标（点位离开视口时为 null） */
const handleRiskPointPosition = (position) => {
  riskPointPosition.value = position ?? null
}

/** 数值带单位展示：后端可能返回 null，统一回落为 '--' */
const formatMetric = (value, unit) => {
  const num = Number(value)
  return Number.isFinite(num) && value !== null && value !== undefined && value !== ''
    ? `${num}${unit}`
    : '--'
}

/** 已有隐患点列表行 → 点位详情弹窗数据（结构对齐 EmergencyPointPopup，字段缺失时回落 mock） */
const buildHazardPopupData = (row = {}) => {
  /* 四项指标：接口未返回的项回落 '--'，指标块始终展示，避免出现忽隐忽现的空色块 */
  const metrics = [
    { label: '威胁总人数', value: formatMetric(row.threatenedPopulation, '人') },
    { label: '威胁总资产', value: formatMetric(row.threatenedPropertyValue, '万元') },
    { label: '稳定性现状', value: row.stabilityStatus || '--' },
    { label: '稳定性趋势', value: row.stabilityTrend || '--' },
  ]

  return {
    ...emergencyPointDetail,
    name: row.name || '',
    level: [row.scaleGrade, row.typeName || row.typeCode].filter(Boolean).join('') || emergencyPointDetail.level,
    metrics,
    info: [
      { label: '隐患点编号：', value: row.uniqueDisasterId || '--' },
      { label: '曾发生灾害时间：', value: row.disasterHistoryTime || '--' },
      {
        label: '更新状态：',
        value: row.aiProcessStatusName || hazardAiStatusMap[String(row.aiProcessStatus)] || '--',
        tag: String(row.aiProcessStatus) === PLAN_STATUS.UPDATED ? 'success' : '',
      },
      { label: '更新时间：', value: row.updatedTime || row.createdTime || '--' },
    ],
  }
}

/** 突发灾险情列表行 → 点位详情弹窗数据 */
const buildEmergencyPopupData = (row = {}) => ({
  ...generatedPointDetail,
  name: row.eventName || '',
  level: row.statusName || emergencyStatusMap[Number(row.status)] || generatedPointDetail.level,
  updateTime: row.updateTime || row.occurTime || '',
})

/** 点击预案卡片选中：地图飞行到该点位并打标记，详情弹窗跟随点位展示 */
const handleSelectPlan = (item) => {
  /* 记录来源信息，供后续「去勾划范围 → 生成 → 查路线」链路使用 */
  applyPlanSource(item)

  const point = resolvePlanPoint(item)
  if (!point) {
    /* 无坐标不伪造位置：清掉上次的标记并提示，等接口补齐经纬度后自动生效 */
    handleClosePopup()
    ElMessage.warning('该预案暂无点位坐标，无法在地图上定位')
    return
  }
  if (!mapInstance.value) {
    ElMessage.warning('地图尚未初始化，请稍后再试')
    return
  }

  /* 详情弹窗优先用列表行的真实字段，raw 缺失（如 mock 数据）时回落到默认文案 */
  const raw = item.raw || {}
  popupData.value =
    item.tab === 'generate'
      ? { ...buildEmergencyPopupData(raw), name: raw.eventName || item.name || '' }
      : { ...buildHazardPopupData(raw), name: raw.name || item.name || '' }
  popupVisible.value = true
  /* flyToPoint radius 默认 1200m，足够看清周边威胁范围 */
  marsMapRef.value?.showRiskPoint(point, {
    image: pointMarkerMap[item.tab] || mapPointUpdate,
    /* 图标原图 72x72 正方形，必须等比缩放，宽高不等会拉伸变形 */
    width: 30,
    height: 34,
    /* 取地形真实高程并贴地：高程用 0 时标记会落在三维地形以下，看起来被埋进地下 */
    clampToGround: true,
    alwaysVisible: true,
  })
}

/** 关闭点位弹窗：同步清掉地图标记，避免残留在地图上 */
const handleClosePopup = () => {
  popupVisible.value = false
  riskPointPosition.value = null
  marsMapRef.value?.clearRiskPoint()
}

/** 切换页签：清掉点位弹窗与地图标记，并重置当前预案来源，避免旧页签的来源被误用 */
const handlePlanTabChange = () => {
  handleClosePopup()
  currentSource.value = null
  currentPlan.value = null
}

/** 取多边形可传给后端的 WKT：后端要求合法、闭合的 POLYGON */
const buildRangeWkt = (graphic) => {
  const ring = getRing(graphic)
  if (ring.length < 3) {
    return ''
  }
  const points = ring.map(([lng, lat]) => `${lng} ${lat}`)
  const [firstLng, firstLat] = ring[0]
  const [lastLng, lastLat] = ring[ring.length - 1]
  if (firstLng !== lastLng || firstLat !== lastLat) {
    points.push(`${firstLng} ${firstLat}`)
  }
  return `POLYGON((${points.join(',')}))`
}

/** 查询当前来源的最新撤离路线结果（GET /route/latest），返回解析后的完整载荷 */
const loadLatestRoute = async () => {
  const source = currentSource.value
  if (!source?.sourceId) {
    ElMessage.warning('请先在左侧选择一条预案')
    return null
  }
  const res = await fetchLatestRoute({ scene: source.scene, sourceId: source.sourceId })
  return resolveRoutePayload(res)
}

/** 打开撤离路线详情：用最新查询结果渲染弹窗
 *  生成中 / 生成失败 / 从未生成过分别给出对应提示，避免打开空弹窗 */
const openRouteDialog = async () => {
  try {
    const payload = await loadLatestRoute()
    if (!payload) {
      return
    }
    if (payload.status === PLAN_STATUS.UPDATING) {
      ElMessage.warning('预案仍在生成中，请稍后再试')
      return
    }
    if (payload.status === PLAN_STATUS.FAILED) {
      ElMessage.error('预案生成失败，请重新生成')
      return
    }
    const routeResult = payload.routeResult
    if (!routeResult?.routes?.length) {
      /* handleId 为空表示从未生成过路线，否则是生成完成但无可用路线 */
      ElMessage.warning(routeResult?.handleId ? '该预案暂未生成可用撤离路线' : '该预案尚未生成过撤离路线')
      return
    }
    evacuationRouteData.value = routeResult
    routeDialogVisible.value = true
  } catch (error) {
    console.error('[智能预案] 撤离路线查询失败:', error)
  }
}

/** 生成撤离路线并打开结果：突发灾险情携带勾划范围 WKT，已有隐患点由后端按斜坡单元取范围 */
const handleRangeConfirm = async () => {
  const graphic = drawnGraphics.value[drawnGraphics.value.length - 1]
  const wkt = graphic ? buildRangeWkt(graphic) : ''
  confirmVisible.value = false

  const source = currentSource.value
  if (!source?.sourceId) {
    ElMessage.warning('请先在左侧选择一条突发灾险情')
    return
  }
  if (!wkt) {
    ElMessage.warning('未获取到有效的勾划范围，请重新绘制')
    return
  }

  generating.value = true
  pollingStopped.value = false
  /* 先打开大弹窗进入加载态：算法在后台执行，避免点击后长时间无反馈 */
  openExecuteResult({ tab: 'generate' }, 'generating')
  try {
    const payload = { scene: source.scene, sourceId: source.sourceId }
    if (source.scene === PLAN_SCENE.EMERGENCY) {
      payload.wkt = wkt
    }
    /* generate 只做校验并置为「更新中」，算法在后台执行，返回后立即开始轮询 */
    await generateIntelligentPlan(payload)
    const { payload: latest, status, timeout } = await pollLatestRoute({
      scene: source.scene,
      sourceId: source.sourceId,
      shouldStop: () => pollingStopped.value,
    })
    /* 生成失败或超时：关闭弹窗并刷新列表，让卡片回到「更新失败」 */
    if (timeout || status === PLAN_STATUS.FAILED) {
      ElMessage.error(timeout ? '预案生成超时，请稍后重新生成' : '预案生成失败，请重新生成')
      closeExecuteResult()
      planPanelRef.value?.refreshList()
      return
    }
    /* 生成完成：用真实结果刷新弹窗内容并结束加载态；
       撤离路线示意图由弹窗内的离屏大图在地图渲染完成后出图回抛，不走这里 */
    applyResultPayload(latest, rangeArea.value)
    evacuationRouteData.value = latest.routeResult || { disasterPolygonsWktList: [], resettlementAreas: [], routes: [] }
    routeReady.value = true
    tryFinishResult()
    /* 状态已变为「已更新」，刷新列表同步卡片状态与操作入口 */
    planPanelRef.value?.refreshList()
  } catch (error) {
    /* 业务错误（如隐患点未关联斜坡单元、wkt 不合法）由 http 统一提示，这里只记录 */
    console.error('[智能预案] 撤离路线生成失败:', error)
    closeExecuteResult()
  } finally {
    generating.value = false
  }
}

/** 合并来源信息：接口返回体优先，缺失（null / undefined / ''）的字段回落到列表行。
 *  如 hazardPoint 不含 location / typeName，用列表行补齐，避免弹窗出现「--」或英文编码 */
const mergeSource = (detail, raw = {}) => {
  if (!detail) {
    return raw
  }
  const merged = { ...raw }
  Object.entries(detail).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      merged[key] = value
    }
  })
  return merged
}

/** 当前预案来源标识：截图与文件条目的归属校验用，切预案后旧截图自动失效 */
const sourceKeyOf = (source) => (source?.sourceId ? `${source.scene}:${source.sourceId}` : '')

/** 离屏路线大图截图：{ sourceKey, url, size }，由 RouteSnapshotMap 出图后回填 */
const routeSnapshot = ref(null)

/** 当前来源有效的截图地址：来源不匹配时视为没有，避免切预案后沿用上一份示意图 */
const routeSnapshotImage = computed(() =>
  routeSnapshot.value?.sourceKey === sourceKeyOf(currentSource.value) ? routeSnapshot.value.url : '',
)

/** 把截图写进结果文件条目：resultData 每次重建都会丢掉 src，故统一在这里回填。
 *  注意文件条目挂在 done 下（与 ExecuteResultSummary 取的 data.done.files 同一份引用） */
const applyRouteSnapshot = () => {
  const snapshot = routeSnapshot.value
  const file = resultData.value?.done?.files?.find((item) => item.type === '图片附件')
  if (!snapshot || !file || snapshot.sourceKey !== sourceKeyOf(currentSource.value)) return
  file.src = snapshot.url
  file.size = snapshot.size
}

/** 离屏大图出图完成：记录缩略图与体积（预案详情里的撤离路线示意图也用它） */
const handleRouteSnapshot = ({ url, size }) => {
  if (!url) return
  routeSnapshot.value = {
    sourceKey: sourceKeyOf(currentSource.value),
    url,
    size: formatFileSize(size),
  }
  applyRouteSnapshot()
}

/** 由 GET /route/latest 的真实载荷刷新结果弹窗数据；接口未返回时来源用列表行兜底 */
const applyResultPayload = (payload, area = '') => {
  const isHazard = payload?.hazardPoint ? true : payload?.emergencyEvent ? false : currentPlan.value?.tab !== 'generate'
  const source = mergeSource(payload?.hazardPoint || payload?.emergencyEvent || null, currentPlan.value?.raw || {})
  const routeResult = payload?.routeResult ?? null
  resultData.value = buildExecuteResult({
    isHazard,
    source,
    routeResult,
    area,
  })
  /* 右侧执行过程与左侧结果同源：接口返回后用同一份真实数据重建，避免文案停留在示例值 */
  if (resultProcessVisible.value) {
    resultProcessData.value = buildExecuteProcess({
      isHazard,
      source,
      routeResult,
      area,
    })
  }
  /* 结果对象是重建出来的，已出图的路线示意图要重新写回，否则轮询刷新一次缩略图就没了 */
  applyRouteSnapshot()
}

/** 拉取最新撤离路线并刷新结果弹窗：接口未返回路线时展示空态，不落到 mock */
const refreshExecuteResult = async (area = '') => {
  try {
    const payload = await loadLatestRoute()
    if (!payload) {
      return
    }
    /* 更新失败：路线集合一定是空，先提示重新生成，再渲染空态 */
    if (payload.status === PLAN_STATUS.FAILED) {
      ElMessage.error('预案生成失败，请重新生成')
    }
    applyResultPayload(payload, area)
    evacuationRouteData.value = payload.routeResult || EMPTY_ROUTE_RESULT
  } catch (error) {
    console.error('[智能预案] 执行结果数据加载失败:', error)
  }
}

/** 结果区切完成态：真实结果已就绪且右侧执行过程已播放完毕（或已收起） */
const tryFinishResult = () => {
  if (routeReady.value && processDone.value) {
    resultStatus.value = 'done'
  }
}

/** 打开执行结果遮挡层：按页签选择结果与执行过程数据，并展开层内的执行过程面板
 *  mode: result 结果已就绪，左侧直接全量展示；analysis 左侧先加载，等右侧流式完成后再展示；
 *  generating 生成中，左侧保持加载直到轮询拿到真实结果且右侧执行过程播放完成 */
const openExecuteResult = (item, mode = 'result') => {
  /* 先用当前来源（列表行）渲染，接口返回后立即替换为真实数据 */
  /* 执行过程面板先置为可见，applyResultPayload 才会同步刷新流式内容 */
  resultProcessVisible.value = true
  applyResultPayload(null)
  routeReady.value = mode !== 'generating'
  processDone.value = mode === 'result'
  resultStatus.value = mode === 'result' ? 'done' : 'loading'
  resultProcessStream.value = mode !== 'result'
  processProgress.value = { revealed: 0, total: 0 }
  resultVisible.value = true
  resultProcessVisible.value = true
  /* 非生成中：直接拉最新结果刷新弹窗，不阻塞展示 */
  if (mode !== 'generating') {
    refreshExecuteResult()
  }
}

/** 点位详情弹窗「查看更新结果」：按弹窗数据形态判断页签 */
const handlePopupViewResult = () => {
  openExecuteResult({ tab: popupData.value?.metrics ? 'update' : 'generate' })
}

/** 点位详情弹窗「查看分析过程」：同样打开大弹窗，左侧等待右侧执行过程流式加载完成 */
const handlePopupViewAnalysis = () => {
  openExecuteResult({ tab: popupData.value?.metrics ? 'update' : 'generate' }, 'analysis')
}

/** 右侧执行过程流式进度回抛：驱动左侧加载提示文案 */
const handleProcessProgress = (progress) => {
  processProgress.value = progress
}

/** 右侧执行过程流式加载完成：结果已就绪才切完成态，生成中继续等待 */
const handleProcessComplete = () => {
  processDone.value = true
  tryFinishResult()
}

/** 收起右侧执行过程面板：视作流程已看完，结果就绪即结束加载 */
const handleProcessClose = () => {
  resultProcessVisible.value = false
  processDone.value = true
  tryFinishResult()
}

/** 折叠/展开右侧执行过程面板：收起时结束左侧等待，展开时重新按流式加载 */
const toggleProcessPanel = () => {
  resultProcessVisible.value = !resultProcessVisible.value
  if (!resultProcessVisible.value) {
    processDone.value = true
    tryFinishResult()
  }
}

/** 关闭执行结果遮挡层（遮罩点击、面板关闭都走这里） */
const closeExecuteResult = () => {
  resultVisible.value = false
  resultProcessVisible.value = false
  resultStatus.value = 'done'
  /* 复原就绪态，避免生成中断后下次打开仍停在加载中 */
  routeReady.value = true
  processDone.value = true
}

onBeforeUnmount(() => {
  /* 停止生成结果轮询，避免组件销毁后仍在请求 */
  pollingStopped.value = true
  /* 页面卸载时清掉地图标记，避免 postRender 回调与图层残留 */
  handleClosePopup()
})

/** 卡片操作：去勾划范围 / 查看分析过程 / 查看更新结果，其余动作待接口接入 */
const handleCardAction = ({ item, action }) => {
  /* 卡片操作按钮不触发选中事件，这里补记来源，保证后续生成 / 查询链路有来源 */
  applyPlanSource(item)

  if (action?.event === 'draw-range') {
    drawPointName.value = item.name
    drawIcon.value = item.hazardIcon || ''
    startDraw()
    return
  }
  if (action?.event === 'view-analysis') {
    openExecuteResult(item, 'analysis')
    return
  }
  if (action?.event === 'view-result') {
    openExecuteResult(item)
    return
  }
  handleUnavailableAction()
}

/** 「查看预案详情」：按当前场景调用案件详情接口渲染预案卡，接口未返回时不落到本地拼装数据 */
const openPlanDetail = async () => {
  const source = currentSource.value
  if (!source?.sourceId) {
    ElMessage.warning('请先在左侧选择一条预案')
    return
  }

  const seq = planDetailSeq + 1
  planDetailSeq = seq
  planDetailLoading.value = true
  planDetailVisible.value = true
  try {
    const res = await fetchPlanCaseDetail(source)
    /* 期间又点了别的预案：丢弃本次结果，以最后一次点击为准 */
    if (seq !== planDetailSeq) {
      return
    }
    planDetailData.value = buildPlanCaseCard({
      scene: source.scene,
      detail: resolveCaseDetail(res),
      /* 撤离路线示意图取离屏大图截图；尚未出图时预案卡内展示生成中说明 */
      image: routeSnapshotImage.value,
      /* 已有隐患点的案件详情不含结构化路线，用本次生成的路线结果补齐候选撤离路线表 */
      routes: evacuationRouteData.value?.routes || [],
    })
  } catch (error) {
    /* 业务错误（如突发灾险情不存在、sourceId 非数字）由 http 统一提示，这里只记录 */
    console.error('[智能预案] 案件详情查询失败:', error)
    ElMessage.error('预案详情查询失败，请稍后重试')
    if (seq === planDetailSeq) {
      planDetailVisible.value = false
    }
  } finally {
    if (seq === planDetailSeq) {
      planDetailLoading.value = false
    }
  }
}

/** 预案文档下载：结果弹窗里没有案件详情，先按当前来源查一次，再在前端生成 docx（与预案详情同一套生成逻辑） */
const handleDownloadPlanDoc = async (fileName = '') => {
  const source = currentSource.value
  if (!source?.sourceId) {
    ElMessage.warning('请先在左侧选择一条预案')
    return
  }
  try {
    const res = await fetchPlanCaseDetail(source)
    const card = buildPlanCaseCard({
      scene: source.scene,
      detail: resolveCaseDetail(res),
      /* 撤离路线示意图取离屏大图截图，与预案详情里展示的是同一张 */
      image: routeSnapshotImage.value,
      /* 与预案详情同一份数据源：已有隐患点用本次生成的路线结果补齐候选撤离路线表 */
      routes: evacuationRouteData.value?.routes || [],
    })
    if (!card?.sections?.length) {
      ElMessage.warning('预案详情尚未返回，暂时无法生成文档')
      return
    }
    const blob = await buildPlanCardDocx(card)
    saveAs(blob, fileName || `${card.fileName || '临时处置预案卡'}.docx`)
  } catch (error) {
    console.error('[智能预案] 预案文档生成失败:', error)
    ElMessage.error('预案文档生成失败，请稍后重试')
  }
}

/** 执行结果内操作：撤离路线走真实接口查询，预案详情打开查看弹窗 */
const handleResultAction = (action) => {
  if (action === '查看路线详情') {
    openRouteDialog()
    return
  }
  if (action === '查看预案详情') {
    openPlanDetail()
    return
  }
  /* 预案文档：查一次案件详情后在前端生成 docx，直接触发浏览器下载 */
  if (action?.event === 'download-plan-doc') {
    handleDownloadPlanDoc(action.name)
    return
  }
  /* 图片附件：现场截图由前端生成（dataURL），直接触发浏览器下载 */
  if (action?.src) {
    const anchor = document.createElement('a')
    anchor.href = action.src
    anchor.download = action.name || '图片附件.png'
    anchor.click()
    return
  }
  /* 示意图尚未出图：离屏大图还在绘制 / 等瓦片，给明确提示而不是当成未支持的操作 */
  if (action?.type === '图片附件') {
    ElMessage.info('撤离路线示意图正在生成，请稍候再下载')
    return
  }
  handleUnavailableAction()
}

/** 绘制工具条：绘制 / 撤销 / 删除 / 退出 */
const handleDrawTool = (key) => {
  if (key === 'draw') {
    startDraw()
  } else if (key === 'undo') {
    handleUndoDraw()
  } else if (key === 'remove') {
    handleRemoveDraw()
  } else {
    handleExitDraw()
  }
}

/** 范围多边形样式：橙色描边 + 半透明黄棕填充（贴地） */
const RANGE_STYLE = {
  color: '#ffc53d',
  opacity: 0.5,
  outline: true,
  outlineColor: '#ffa940',
  outlineWidth: 2,
  clampToGround: true,
}

/** 编辑顶点样式：白底圆点 + 橙色描边。
 *  mars3d 的编辑点样式是 DrawUtil 上的全局静态配置，故在地图就绪时设置一次 */
const setupEditPointStyle = (mars3d) => {
  const Control = mars3d?.EditPointType?.Control
  const setStyle = mars3d?.DrawUtil?.setEditPointStyle
  if (Control === undefined || typeof setStyle !== 'function') {
    return
  }
  setStyle.call(mars3d.DrawUtil, Control, {
    pixelSize: 8,
    color: '#ffffff',
    outlineColor: '#ffa940',
    outlineWidth: 2,
  })
}

/** 开始绘制多边形范围：左键依次加点，右键（或双击）结束绘制 */
const startDraw = () => {
  const map = mapInstance.value
  const mars3d = mars3dNs.value
  if (!map?.graphicLayer) {
    ElMessage.warning('地图尚未初始化，请稍后再试')
    return
  }
  drawingMode.value = true
  confirmVisible.value = false
  map.graphicLayer.stopDraw?.()

  /* 绘制完成事件兜底：部分场景 success 回调与 drawCreated 事件会同时触发，用图形引用去重 */
  if (mars3d?.EventType?.drawCreated && typeof map.graphicLayer.on === 'function') {
    map.graphicLayer.off?.(mars3d.EventType.drawCreated, handleDrawCreated)
    map.graphicLayer.on(mars3d.EventType.drawCreated, handleDrawCreated)
  }

  try {
    map.graphicLayer.startDraw({
      type: 'polygon',
      style: RANGE_STYLE,
      success: handleDrawCreated,
    })
    ElMessage.info('请在地图上依次点击绘制范围，右键或双击结束绘制')
  } catch (error) {
    console.error('开启绘制失败:', error)
    ElMessage.error('开启绘制失败，请查看控制台信息')
  }
}

/** 绘制完成（success 回调与 drawCreated 事件共用）：入列、进入编辑态并刷新面积与弹窗 */
const handleDrawCreated = (payload) => {
  /* drawCreated 事件回调收到的是 event 对象，success 回调直接给 graphic */
  const graphic = payload?.graphic || payload
  if (!graphic || drawnGraphics.value.includes(graphic)) {
    return
  }
  console.log('绘制完成', graphic)
  drawnGraphics.value = [...drawnGraphics.value, graphic]

  /* 进入编辑态：保留节点便于拖动微调（与设计稿一致），面积随调整实时刷新 */
  try {
    mapInstance.value?.graphicLayer?.startEditing?.(graphic)
    bindGraphicEditEvents(graphic)
  } catch (error) {
    console.warn('进入编辑态失败:', error)
  }

  updateRange(graphic)
}

/** 节流刷新：拖动节点时高频触发，120ms 合并一次，避免频繁重建标签 */
let rangeRefreshTimer = null
const scheduleRangeRefresh = (graphic) => {
  if (rangeRefreshTimer) {
    return
  }
  rangeRefreshTimer = setTimeout(() => {
    rangeRefreshTimer = null
    updateRange(graphic, false)
  }, 120)
}

/** 绑定编辑事件：拖动节点、删除点、结束编辑后同步面积与浮动标签 */
const bindGraphicEditEvents = (graphic) => {
  const EventType = mars3dNs.value?.EventType
  if (!EventType || typeof graphic?.on !== 'function') {
    return
  }
  const eventTypes = [
    EventType.editMovePoint,
    EventType.editRemovePoint,
    EventType.editStop,
    EventType.change,
  ].filter(Boolean)

  eventTypes.forEach((type) => {
    graphic.on(type, () => scheduleRangeRefresh(graphic))
  })
}

/** 撤销：移除最后一次绘制的范围（尚未绘制时结束当前绘制） */
const handleUndoDraw = () => {
  const list = [...drawnGraphics.value]
  const last = list.pop()
  if (!last) {
    mapInstance.value?.graphicLayer?.stopDraw?.()
    return
  }
  mapInstance.value?.graphicLayer?.removeGraphic?.(last, true)
  drawnGraphics.value = list
  if (list.length) {
    updateRange(list[list.length - 1])
  } else {
    clearRange()
  }
}

/** 删除：清空本次所有绘制范围 */
const handleRemoveDraw = () => {
  if (rangeRefreshTimer) {
    clearTimeout(rangeRefreshTimer)
    rangeRefreshTimer = null
  }
  mapInstance.value?.graphicLayer?.clear?.()
  drawnGraphics.value = []
  clearRange()
}

/** 退出绘制：结束绘制模式并清空本次绘制 */
const handleExitDraw = () => {
  mapInstance.value?.graphicLayer?.stopDraw?.()
  handleRemoveDraw()
  drawingMode.value = false
}

/** 重绘范围：清空当前绘制后重新进入绘制 */
const handleRedraw = () => {
  handleRemoveDraw()
  startDraw()
}

/** 刷新勾划面积与浮动标签；fromDraw 为 true 时同时弹出确认弹窗 */
const updateRange = (graphic, fromDraw = true) => {
  try {
    const ring = getRing(graphic)
    if (ring.length < 3) {
      console.warn('未取到有效的多边形坐标，面积按 0 处理', graphic)
    }
    rangeArea.value = ring.length >= 3 ? `${calcRingArea(ring).toFixed(2)}km²` : '0km²'
    showRangeLabel(graphic)
  } catch (error) {
    console.error('计算勾划面积失败:', error)
  }

  /* 弹窗与标签解耦：即使面积/标签渲染异常，也保证能进入下一步操作 */
  if (fromDraw) {
    confirmVisible.value = true
  }
}

const clearRange = () => {
  confirmVisible.value = false
  rangeArea.value = '0km²'
  removeRangeLabel()
}

/** 单个坐标点转 [经度, 纬度]：兼容经纬度对象与笛卡尔坐标两种形态 */
const toLngLat = (position) => {
  if (!position) {
    return null
  }
  if (position.lng !== undefined) {
    return [position.lng, position.lat]
  }
  const point = mars3dNs.value?.LngLatPoint?.fromCartesian?.(position)
  if (point) {
    return [point.lng, point.lat]
  }
  const cartographic = Cesium.Cartographic.fromCartesian(position)
  return cartographic
    ? [Cesium.Math.toDegrees(cartographic.longitude), Cesium.Math.toDegrees(cartographic.latitude)]
    : null
}

/** 取多边形的经纬度环。
 *  mars3d 的 positionsShow 为实际显示的坐标，绘制/编辑中 positions 可能只是 CallbackProperty */
const getRing = (graphic) => {
  const cartesians = graphic?.positionsShow?.length
    ? graphic.positionsShow
    : (graphic?.positions || graphic?.coordinates || [])
  const ring = cartesians.map(toLngLat).filter(Boolean)
  if (ring.length) {
    return ring
  }

  /* 兜底：从 GeoJSON 导出取坐标 */
  try {
    const geojson = graphic?.toGeoJSON?.({ closure: false, noAlt: true })
    return (geojson?.geometry?.coordinates?.[0] || []).map(([lng, lat]) => [lng, lat])
  } catch (error) {
    console.warn('解析多边形坐标失败:', error)
    return []
  }
}

/** 取多边形中心的经纬度（mars3d 的 center 为笛卡尔坐标） */
const getCenterLngLat = (graphic) => {
  const ring = getRing(graphic)
  if (!ring.length) {
    return null
  }
  const [lng, lat] = toLngLat(graphic?.center) || ring[0]
  return { lng, lat }
}

/** 球面多边形面积（km²） */
const calcRingArea = (ring) => {
  let total = 0
  for (let i = 0; i < ring.length; i += 1) {
    const [lon1, lat1] = ring[i]
    const [lon2, lat2] = ring[(i + 1) % ring.length]
    total += ((lon2 - lon1) * Math.PI / 180)
      * (2 + Math.sin((lat1 * Math.PI) / 180) + Math.sin((lat2 * Math.PI) / 180))
  }
  return Math.abs((total * EARTH_RADIUS * EARTH_RADIUS) / 2)
}

/** 范围浮动标签：显示「本次资料检索范围 x.xxkm²」；DivGraphic 不可用时退回内置 tooltip */
const showRangeLabel = (graphic) => {
  const map = mapInstance.value
  const mars3d = mars3dNs.value
  const text = `${rangeConfirm.label} ${rangeArea.value}`
  const html = `<div style="padding:8px 12px;border-radius:8px;background:rgba(255,255,255,0.96);box-shadow:0 4px 12px rgba(0,32,80,0.16);color:#222527;font-size:14px;line-height:22px;white-space:nowrap;">${rangeConfirm.label}<span style="margin-left:2px;color:#007bff;font-weight:800;font-family:'Alimama FangYuanTi VF',sans-serif;">${rangeArea.value}</span></div>`
  const center = getCenterLngLat(graphic)

  removeRangeLabel()

  if (map && mars3d?.graphic?.DivGraphic && center) {
    try {
      rangeGraphic.value = new mars3d.graphic.DivGraphic({
        position: [center.lng, center.lat, 0],
        style: { html, anchor: [0, -10] },
      })
      map.graphicLayer.addGraphic(rangeGraphic.value)
      return
    } catch (error) {
      console.warn('范围浮动标签渲染失败，改用内置 tooltip:', error)
    }
  }

  /* 兜底：使用 mars3d 内置 tooltip，保证面积始终可见 */
  graphic?.bindTooltip?.(text, { direction: 'top' })
}

const removeRangeLabel = () => {
  if (rangeGraphic.value) {
    mapInstance.value?.graphicLayer?.removeGraphic?.(rangeGraphic.value, true)
    rangeGraphic.value = null
  }
}

/** 查看更新结果 / 查看分析过程 / 进行预案生成等动作待接口接入 */
const handleUnavailableAction = () => {
  ElMessage.warning('当前接口未提供该操作')
}

const handleMapLoaded = ({ map, viewer, mars3d }) => {
  mapInstance.value = map
  mars3dNs.value = mars3d
  setupEditPointStyle(mars3d)
  console.log('Mars3D 地图初始化就绪', map, viewer)
}
</script>

<style lang="less" scoped>
.emergency-page {
  position: relative;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 100%;
  display: flex;
  /* 面板与地图并排，收起按钮在容器外，故不裁切 */
  overflow: visible;
  box-sizing: border-box;
  padding: 0;
  background: transparent;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 地图区域：本身透明，内部内容区左侧为面板预留空间 */
.map-area {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  min-height: 100%;
  box-sizing: border-box;
  background: transparent;
  overflow: visible;
  transform: translateZ(0);
  isolation: isolate;
}

/* 地图内容区：左右分别为两侧面板让出宽度（460 + 12 间距），面板收起/关闭时铺满 */
.map-content {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 472px;
  transition: left 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              right 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.map-area.is-left-collapsed .map-content {
  left: 0;
}

/* 左侧面板裁剪容器：约束在 x >= 0，避免收起时溢出到菜单栏 */
.left-panel-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 10;
  width: 480px;
  max-width: calc(100% - 32px);
  overflow: hidden;
  pointer-events: none;
  visibility: visible;
  transition: visibility 0s linear 0s;

  &.is-collapsed {
    visibility: hidden;
    pointer-events: none;
    transition: visibility 0s linear 0.28s;
  }
}

/* 左侧智能预案面板：与地图并排，收起时向左滑出 */
.left-panel-stack {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 10;
  width: 460px;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  border-radius: 16px;
  background: #ffffff;
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

/* 点位详情弹窗：left/top 由地图点位的画布像素坐标驱动，底边贴住点位标记顶部 */
.point-popup-layer {
  position: absolute;
  z-index: 12;
  transform: translate(-50%, -100%);
}

/* 勾划范围：绘制工具条（地图顶部居中） */
.draw-toolbar {
  position: absolute;
  top: 16px;
  left: 50%;
  z-index: 13;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 16px rgba(0, 32, 80, 0.14);
  transform: translateX(-50%);
}

.tool-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #617185;
  font-family: inherit;
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;

  .tool-icon {
    font-size: 14px;
  }

  &:hover {
    background: #f1f5f9;
    color: #007bff;
  }

  &.is-primary {
    background: #007bff;
    color: #ffffff;

    &:hover {
      background: #3395ff;
      color: #ffffff;
    }
  }
}

.draw-toolbar-enter-active,
.draw-toolbar-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.draw-toolbar-enter-from,
.draw-toolbar-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}

/* 勾划范围确认弹窗：地图区居中偏上（原先贴底，位置太靠下） */
.range-confirm-layer {
  position: absolute;
  top: 70%;
  left: 50%;
  z-index: 14;
  transform: translate(-50%, -50%);
}

.right-panel-enter-active,
.right-panel-leave-active {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;
}

.right-panel-enter-from,
.right-panel-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* 执行结果遮挡层：整层淡入淡出 */
.execute-layer {
  position: absolute;
  inset: 0;
  z-index: 104;
}

/* 遮挡：白色大底，遮住地图与左侧列表 */
.execute-mask {
  position: absolute;
  inset: 0;
  background: #ffffff;
}

/* 遮挡层内容：结果弹窗铺满剩余宽度 + 执行过程 460，整体铺满内容区 */
.execute-content {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 8px;
  box-sizing: border-box;
}

/* 执行结果：铺满剩余宽度，内容 800px 在组件内居中 */
.execute-dialog-layer {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}

/* 执行过程折叠/展开按钮：镜像左侧面板的收起按钮，骑在面板左缘（露出 10px） */
.process-trigger {
  position: absolute;
  top: 50%;
  right: 458px;
  z-index: 2;
  width: 20px;
  height: 72px;
  padding: 0;
  box-sizing: border-box;
  border: 1px solid #cccccc66;
  border-radius: 17px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 32, 80, 0.12);
  cursor: pointer;
  transform: translateY(-50%);
  transition: right 0.28s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 123, 255, 0.16);
  }

  img {
    display: block;
    width: 14px;
    height: 14px;
    margin: 0 auto;
    object-fit: contain;
  }

  /* 收起态：贴到内容区右缘，整体留在遮挡层内，避免溢出页面 */
  &.is-collapsed {
    right: 0;
  }
}

/* 遮挡层内的执行过程面板：与结果弹窗并排，不影响地图 */
.execute-process-layer {
  flex: 0 0 auto;
  width: 460px;
  min-height: 0;
  overflow: hidden;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 10px 32px rgba(0, 32, 80, 0.12);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.24s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

/* 收起/展开按钮 */
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

.left-trigger {
  left: 0;
  /* 高于左侧面板（10）、地图控件与弹窗，避免骑在面板边缘时被遮挡 */
  z-index: 100;
  /* 展开：按钮左缘与面板右缘（460px）对齐，整体露在地图侧 */
  transform: translateY(-50%) translateX(450px);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.2s ease;
}

/* 收起：跟随面板左移到内容区左缘外侧，形成骑线效果。
   溢出的 10px 落在左侧菜单的透明内边距上，需页面为 is-fixed-page（`.layout-main` overflow: visible）才不被裁切 */
.left-trigger.is-collapsed {
  transform: translateY(-50%) translateX(-10px);
}

@media (max-width: 1366px) {
  .left-trigger {
    transform: translateY(-50%) translateX(450px);
  }

  .left-trigger.is-collapsed {
    transform: translateY(-50%) translateX(-10px);
  }
}
</style>
