<template>
  <div class="hazard-review-page">
    <!-- 地图区域：左侧概览面板与右侧智能体面板都是浮动上层，不挤压地图 -->
    <section
      class="map-area"
      :class="{ 'is-left-collapsed': leftCollapsed }"
      aria-label="地图区域"
    >
      <!-- 地图内容区：左侧为概览面板让出宽度，右侧智能体面板以覆盖层浮在图上 -->
      <div class="map-content">
        <MarsMap
          ref="marsMapRef"
          @onload="handleMapLoaded"
          @risk-point-position="handleRiskPointPosition"
        >
          <!-- 隐患点详情气泡：置于 MarsMap 的 overlay 插槽内，与 Cesium 画布同一定位容器，
               坐标由 risk-point-position 持续回抛，随地图平移缩放实时跟随定位点 -->
          <template #overlay>
            <HazardDetailPopup
              v-if="popupVisible && riskPointPosition"
              class="hazard-popup-layer"
              :style="popupLayerStyle"
              :data="popupData"
              @close="closeHazardPopup"
            />
          </template>
        </MarsMap>

        <MapOverlayControls
          v-model:risk-levels="dynamicRiskLevels"
          @zoom-in="marsMapRef?.handleZoomIn()"
          @zoom-out="marsMapRef?.handleZoomOut()"
          @locate-home="marsMapRef?.flyToHome()"
          @change-base-map="marsMapRef?.changeBaseMap($event)"
          @toggle-imagery-label="marsMapRef?.toggleImageryLabel($event)"
          @change-layer="marsMapRef?.handleLayerChange($event)"
        />
      </div>

      <!-- 右侧：隐患复核智能体处理面板，点击卡片「AI处理」从右侧滑入 -->
      <Transition name="right-panel">
        <AgentProcessPanel
          v-if="agentPanelVisible"
          class="agent-panel-layer"
          :data="agentTarget"
          :panel="agentPanel"
          @close="closeAgentPanel"
          @feedback="openFeedbackDialog"
          @confirm="handleConfirm"
          @retry-step="handleRetryStep"
          @skip-step="handleSkipStep"
        />
      </Transition>

      <!-- 左侧面板：与地图并排展示，收起时向左滑出 -->
      <div class="left-panel-wrapper" :class="{ 'is-collapsed': leftCollapsed }">
        <div class="left-panel-stack" :class="{ 'is-collapsed': leftCollapsed }">
          <ReviewSummaryPanel @view-process-detail="processListVisible = true" />
          <div v-if="processListVisible" class="process-list-layer">
            <HazardProcessPanel
              ref="processPanelRef"
              @back="processListVisible = false"
              @process="handleProcess"
              @select="handleSelect"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 左侧面板收起/展开按钮：置于 map-area 外层，避免被地图容器裁切 -->
    <button
      class="collapse-trigger left-trigger"
      :class="{ 'is-collapsed': leftCollapsed }"
      :title="leftCollapsed ? '展开隐患复核情况' : '收起隐患复核情况'"
      @click="toggleLeftCollapse"
    >
      <img :src="leftCollapsed ? rightArrow : leftArrow" alt="" />
    </button>

    <!-- 提交反馈：人工风险等级 + 修正备注，交互与群策群防页面一致 -->
    <FeedbackDialog
      v-model="feedbackVisible"
      :subject="agentTarget"
      :submitting="feedbackSubmitting"
      @submit="submitFeedback"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import ReviewSummaryPanel from './components/ReviewSummaryPanel.vue'
import HazardProcessPanel from './components/processList/HazardProcessPanel.vue'
import HazardDetailPopup from './components/HazardDetailPopup.vue'
import AgentProcessPanel from './components/agentProcess/index.vue'
import FeedbackDialog from '@/components/FeedbackDialog/index.vue'
import { confirmHazardReview, getHazardHandleDetail, submitHazardFeedback } from '@/api/hazardReview.js'
import { DEFAULT_DYNAMIC_RISK_LEVELS } from '@/utils/riskEvaluation.js'
import {
  AI_STEP_KEY,
  REVIEW_AI_STATUS,
  buildHazardAgentPanel,
  fetchHazardOverview,
  runHazardAiStep,
  skipHazardAiStep,
} from './useHazardReviewData.js'
import { hazardDetail } from './config.js'
import mapPointIcon from '@/assets/imgs/hazardReview/map-point-zhd.webp'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'

defineOptions({ name: 'HazardReview' })

const marsMapRef = ref(null)
const leftCollapsed = ref(false)
/** 地图「动态风险」勾选等级：默认只展示极高风险与高风险 */
const dynamicRiskLevels = ref([...DEFAULT_DYNAMIC_RISK_LEVELS])
/** 地图弹窗默认隐藏，点击列表卡片选中后再显示 */
const popupVisible = ref(false)
/** 地图弹窗当前展示的隐患点详情（详情字段待接口接入后按选中项请求） */
const popupData = ref({ ...hazardDetail })

/**
 * 点位标记尺寸：图标原图 60x60（正方形），billboard 会把贴图直接拉伸到传入的宽高，
 * 宽高必须与图标等比（与专业监测的 60x68 图标同口径换算），否则图标会被压扁或拉长
 */
const MARKER_WIDTH = 34
const MARKER_HEIGHT = 34
/** 弹窗底边与图标顶部的间距 */
const MARKER_GAP = 10

/** 选中隐患点在画布中的像素坐标，由 MarsMap 的 risk-point-position 事件持续回抛 */
const riskPointPosition = ref(null)

/** 弹窗定位：跟随地图点位，left/top 由画布像素坐标换算 */
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

/** 地图回抛点位像素坐标（点位离开视口时为 null） */
const handleRiskPointPosition = (position) => {
  riskPointPosition.value = position ?? null
}

/** 左侧面板是否展示隐患处理列表（覆盖复核概况） */
const processListVisible = ref(false)
/** 右侧智能体处理面板是否展示 */
const agentPanelVisible = ref(false)
/** 右侧面板当前处理的隐患点（列表行，详情返回前先用它展示名称与状态） */
const agentTarget = ref(null)
/** 右侧面板数据：详情接口的装配结果（见 useHazardReviewData 的 buildHazardAgentPanel） */
const agentPanel = ref(null)
/** 详情请求序号：连续点选隐患点时只认最后一次响应 */
let agentDetailSeq = 0
/** 隐患处理列表：确认 / 反馈成功后刷新列表 */
const processPanelRef = ref(null)

const toggleLeftCollapse = () => {
  leftCollapsed.value = !leftCollapsed.value
}

/** 点击列表卡片的「AI处理」：先打开面板，再按 id 拉取智能体详情 */
const handleProcess = (item) => {
  /* 同一隐患点再次打开：沿用已展示的算法建议、只为刷新详情，避免重复提交耗时分析 */
  const reuse = agentPanel.value?.id === item.id
  agentTarget.value = item
  if (!reuse) {
    agentPanel.value = null
  }
  agentPanelVisible.value = true
  void loadAgentDetail({ analyze: !reuse })
}

/** 面板是否仍属于当前选中的隐患点：切换点位或关闭面板后返回 false，用于丢弃在途响应 */
const isCurrentAgentPanel = (panel, seq) => seq === agentDetailSeq && agentPanel.value === panel

/**
 * 串行执行单个算法步骤（疑似重复筛查 → 隐患判定建议）。
 * 文档要求先复核后判定：复核未出结果时停在当前步骤等人工处理（重试 / 跳过），不自动继续
 * @param {string} stepKey 步骤 key（AI_STEP_KEY.REVIEW / AI_STEP_KEY.JUDGE）
 * @param {number} [seq] 详情请求序号，切换隐患点后与当前序号不一致即丢弃结果
 * @returns {Promise<boolean>} 该步骤是否拿到分析结果
 */
const runAgentAiStep = async (stepKey, seq = agentDetailSeq) => {
  const panel = agentPanel.value
  if (!panel?.id) {
    return false
  }
  const shouldApply = () => isCurrentAgentPanel(panel, seq)
  const ok = await runHazardAiStep(panel, stepKey, { shouldApply })
  /* 复核成功后接着做判定；复核失败或人工跳过则等用户处理 */
  if (ok && stepKey === AI_STEP_KEY.REVIEW) {
    await runHazardAiStep(panel, AI_STEP_KEY.JUDGE, { shouldApply })
  }
  return ok
}

/** 步骤失败后点「重试」：重试复核成功后会自动继续判定 */
const handleRetryStep = (stepKey) => {
  void runAgentAiStep(stepKey)
}

/** 人工跳过疑似重复筛查：标记结果不完整后继续调用判定接口 */
const handleSkipStep = (stepKey) => {
  const panel = agentPanel.value
  if (!panel?.id || stepKey !== AI_STEP_KEY.REVIEW) {
    return
  }
  const seq = agentDetailSeq
  skipHazardAiStep(panel, stepKey)
  void runHazardAiStep(panel, AI_STEP_KEY.JUDGE, { shouldApply: () => isCurrentAgentPanel(panel, seq) })
}

/** 刷新详情时沿用已展示的算法结果：确认 / 反馈后不必重跑两个耗时的算法接口 */
const carryOverAiResult = (from, to) => {
  from?.steps?.forEach((step) => {
    const target = to.steps.find((item) => item.key === step.key)
    if (target?.ai && step.ai) {
      target.ai = { ...step.ai }
    }
  })
}

/**
 * 拉取智能体详情：接口异常已由 http 层统一提示，面板保持提示文案
 * @param {object} [options]
 * @param {boolean} [options.analyze] 是否随后串行调用两个算法接口（默认 true；仅刷新详情时传 false）
 */
const loadAgentDetail = async ({ analyze = true } = {}) => {
  const id = agentTarget.value?.id
  if (!id) {
    return
  }
  const seq = ++agentDetailSeq
  const detail = await getHazardHandleDetail(id).catch(() => null)
  /* 响应回来时已切到别的隐患点：丢弃本次结果 */
  if (seq !== agentDetailSeq || !detail) {
    return
  }
  const previous = agentPanel.value
  agentPanel.value = buildHazardAgentPanel(detail, agentTarget.value)
  if (analyze) {
    /* 面板先按本地数据渲染并播放产出节奏，算法结果返回后就地回填 */
    void runAgentAiStep(AI_STEP_KEY.REVIEW, seq)
  } else {
    carryOverAiResult(previous, agentPanel.value)
  }
}

/** 取隐患点经纬度：接口可能返回字符串，统一转数值后再校验 */
const resolveHazardPoint = (item) => {
  const lng = Number(item?.lng)
  const lat = Number(item?.lat)
  return Number.isFinite(lng) && Number.isFinite(lat) ? { lng, lat } : null
}

/** 点击列表卡片选中隐患点：地图飞行到该点位并打标记，详情弹窗跟随点位展示 */
const handleSelect = (item) => {
  const point = resolveHazardPoint(item)
  if (!point) {
    /* 无坐标不伪造位置：清掉上次的标记并提示，等接口补齐经纬度后自动生效 */
    closeHazardPopup()
    ElMessage.warning('该隐患点暂无点位坐标，无法在地图上定位')
    return
  }

  /* 弹窗内容用列表接口返回的真实字段；风险等级接口未提供，置空后由弹窗隐藏该标签 */
  popupData.value = { ...hazardDetail, name: item.name, level: '', ...(item.detail || {}) }
  popupVisible.value = true
  /* 换点位时先清掉上一个点位的像素坐标，避免弹窗短暂停在旧位置 */
  riskPointPosition.value = null
  /* 定位口径与风险评估的斜坡单元定位一致：保留当前航向与俯仰角，1.5s 平滑聚焦 */
  marsMapRef.value?.showRiskPoint(point, {
    image: mapPointIcon,
    width: MARKER_WIDTH,
    height: MARKER_HEIGHT,
    clampToGround: true,
    alwaysVisible: true,
    keepView: true,
    duration: 1.5,
  })
}

/** 关闭隐患点弹窗：同步清掉地图标记，避免标记残留在地图上 */
const closeHazardPopup = () => {
  popupVisible.value = false
  riskPointPosition.value = null
  marsMapRef.value?.clearRiskPoint()
}

const closeAgentPanel = () => {
  agentPanelVisible.value = false
  /* 面板已关：让在途的详情请求作废，避免响应回来又写入面板数据 */
  agentDetailSeq += 1
}

/**
 * 面板内「确认复核结果」：需算法状态为 SUCCEEDED，确认结论取详情里的复核建议草稿。
 * 确认成功后状态与版本都会变化，故重新拉取详情，并刷新列表与总览统计
 */
const handleConfirm = async () => {
  const detail = agentPanel.value?.raw
  if (!detail?.id) {
    ElMessage.warning('请先选择要处理的隐患点')
    return
  }
  if (detail.reviewAiStatus !== REVIEW_AI_STATUS.SUCCEEDED) {
    ElMessage.warning('AI 复核尚未完成，暂不能确认复核结果')
    return
  }
  const reviewResult = detail.reviewSuggestion || detail.aiSummary
  if (!reviewResult) {
    ElMessage.warning('暂无可确认的复核结论')
    return
  }
  try {
    await confirmHazardReview(detail.id, { version: detail.version, reviewResult })
    ElMessage.success('已确认复核结果并生成正式台账')
    /* 只刷新状态与版本：算法建议已展示过，不因确认动作再跑一次耗时分析 */
    await loadAgentDetail({ analyze: false })
    processPanelRef.value?.refresh()
    void fetchHazardOverview().catch(() => {})
  } catch {
    /* 版本冲突等业务错误已由 http 层统一提示 */
  }
}

/** 提交反馈弹窗：显示态与提交中态 */
const feedbackVisible = ref(false)
const feedbackSubmitting = ref(false)

/** 右侧流程面板点「提交反馈」：带上当前隐患点，弹窗按其现有值回填 */
const openFeedbackDialog = () => {
  if (!agentTarget.value) {
    ElMessage.warning('请先选择要处理的隐患点')
    return
  }
  feedbackVisible.value = true
}

/** 提交人工反馈：隐患复核接口只收 version 与反馈内容，取弹窗的「人工修正备注」 */
const submitFeedback = async (payload) => {
  const detail = agentPanel.value?.raw
  if (!detail?.id) {
    ElMessage.warning('请先选择要处理的隐患点')
    return
  }
  feedbackSubmitting.value = true
  try {
    const version = await submitHazardFeedback(detail.id, {
      version: detail.version,
      feedback: payload.manualRiskRemark,
    })
    feedbackVisible.value = false
    ElMessage.success('反馈提交成功')
    /* 接口返回最新版本号：更新本地版本，避免再次提交时版本冲突 */
    if (version !== null && version !== undefined) {
      detail.version = version
    }
  } catch {
    /* 版本冲突等业务错误已由 http 层统一提示 */
  } finally {
    feedbackSubmitting.value = false
  }
}

const handleMapLoaded = ({ map, viewer }) => {
  console.log('Mars3D 地图初始化就绪', map, viewer)
}

/** 页面卸载：清掉地图标记与 postRender 回调，避免内存泄漏 */
onBeforeUnmount(() => {
  closeHazardPopup()
})

</script>

<style lang="less" scoped>
.hazard-review-page {
  position: relative;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 100%;
  display: flex;
  // 面板与地图并排，收起按钮在容器外，故不裁切
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

/* 地图内容区：左侧为概览面板让出宽度（460 + 12 间距），面板收起时铺满；
   右侧智能体面板是覆盖层，不参与地图宽度计算 */
.map-content {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 472px;
  transition: left 0.28s cubic-bezier(0.4, 0, 0.2, 1);
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
    transition: visibility 0s linear 0.28s;
  }
}

/* 左侧统计面板：与地图并排，收起时向左滑出 */
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
  border-radius: 20px;
  background: #ffffff;
  transform: translateX(0);
  opacity: 1;
  pointer-events: auto;
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-collapsed {
    transform: translateX(-100%);
    opacity: 0;
    pointer-events: none;
  }
}

/* 隐患点详情气泡：left/top 由地图点位的画布像素坐标驱动，底边贴住点位标记顶部 */
.hazard-popup-layer {
  position: absolute;
  z-index: 12;
  transform: translate(-50%, -100%);
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
  /* 高于左侧面板（10）、地图控件与右侧面板，避免骑在面板边缘时被遮挡 */
  z-index: 100;
  /* 展开：按钮左缘与面板右缘（460px）对齐，整体露在地图侧 */
  transform: translateY(-50%) translateX(448px);
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
    transform: translateY(-50%) translateX(448px);
  }
  .left-trigger.is-collapsed {
    transform: translateY(-50%) translateX(-10px);
  }
}

/* 隐患处理列表：覆盖在复核概况之上，从右滑入 */
.process-list-layer {
  position: absolute;
  inset: 0;
  background: #ffffff;
  animation: process-layer-in 0.24s ease;
}

@keyframes process-layer-in {
  from {
    opacity: 0;
    transform: translateX(24px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 右侧智能体处理面板：覆盖层浮在地图与地图控件之上（不挤压地图宽度），滑入效果与群测群防的详情面板保持一致 */
.agent-panel-layer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 12;
  width: 460px;
  max-width: calc(100% - 32px);
  overflow: hidden;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: -4px 0 20px rgba(0, 32, 80, 0.12);
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

/* 地图控件随 .map-content 一起收缩，无需单独让位 */
</style>
