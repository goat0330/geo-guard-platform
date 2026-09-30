<template>
  <div class="monitor-page">
    <!-- 地图区域：左侧统计面板与右侧智能体面板都是浮动上层，不挤压地图 -->
    <section class="map-area" :class="{ 'is-left-collapsed': leftCollapsed }"
      aria-label="专业监测地图">
      <!-- 地图内容区：左侧为统计面板让出宽度，右侧智能体面板以覆盖层浮在图上 -->
      <div class="map-content">
        <MarsMap
          ref="marsMapRef"
          @select-area="handleAreaSelect"
          @area-loaded="handleAreaLoaded"
          @risk-point-position="handleRiskPointPosition"
        >
          <!-- 监测点详情气泡：置于 MarsMap 的 overlay 插槽内，与 Cesium 画布同一定位容器，
               坐标由 risk-point-position 持续回抛，随地图平移缩放实时跟随定位点 -->
          <template #overlay>
            <MonitorPointPopup
              v-if="popupVisible && riskPointPosition"
              class="point-popup-layer"
              :style="popupLayerStyle"
              :detail="pointDetail"
              @close="handleClosePopup"
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

      <!-- 左侧面板：与地图并排展示，收起时向左滑出 -->
      <div class="left-panel-wrapper" :class="{ 'is-collapsed': leftCollapsed }">
        <div class="left-panel-stack" :class="{ 'is-collapsed': leftCollapsed }">
          <MonitorOverviewPanel @date-change="handleDateChange" @view-alarm-detail="alarmListVisible = true" />
          <div v-if="alarmListVisible" class="alarm-list-layer">
            <AlarmListPanel @back="handleAlarmListBack" @export="handleExportList" @analyze="handleAnalyze"
              @select="handleSelectAlarm" />
          </div>
        </div>
      </div>

      <!-- 右侧：监测预警智能体分析面板，点击「AI数据分析」从右侧滑入 -->
      <Transition name="right-panel">
        <WarningAgentPanel v-if="agentPanelVisible" class="agent-panel-layer" :detail="agentDetail"
          @close="agentPanelVisible = false" @feedback="openFeedbackDialog" @assign="handleUnavailableAction" />
      </Transition>
    </section>

    <!-- 面板收起/展开按钮：置于 map-area 外层，避免被地图容器裁切 -->
    <button class="collapse-trigger left-trigger" :class="{ 'is-collapsed': leftCollapsed }"
      :title="leftCollapsed ? '展开专业监测概况' : '收起专业监测概况'" @click="leftCollapsed = !leftCollapsed">
      <img :src="leftCollapsed ? rightArrow : leftArrow" alt="" />
    </button>

    <!-- 提交反馈：人工风险等级 + 修正备注，交互与群策群防页面一致 -->
    <FeedbackDialog
      v-model="feedbackVisible"
      :subject="agentAlarm"
      :submitting="feedbackSubmitting"
      @submit="submitFeedback"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import dayjs from 'dayjs'
import { analyzeMonitorWarning, exportWarningDisposalRecord } from '@/api/monitor.js'
import { getDeviceDataDetail } from '@/api/device.js'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import MonitorOverviewPanel from './components/MonitorOverviewPanel.vue'
import MonitorPointPopup from './components/MonitorPointPopup.vue'
import AlarmListPanel from './components/alarmList/index.vue'
import WarningAgentPanel from './components/warningAgent/index.vue'
import FeedbackDialog from '@/components/FeedbackDialog/index.vue'
import { submitDisReportSuggestion } from '@/api/reportDisaster.js'
import { DEFAULT_DYNAMIC_RISK_LEVELS } from '@/utils/riskEvaluation.js'
import { buildRangeParams } from '@/utils/dateRange.js'
import {
  applyWarningAnalysis,
  buildDeviceStatus,
  buildMonitorPointMetrics,
  buildWarningAgentDetail,
  monitorPointDetail,
} from './config.js'
import { fetchMonitorOverview, monitorData } from './useMonitorData.js'
import mapPointIcon from '@/assets/imgs/monitor/icon-alarm-pin.webp'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'

defineOptions({ name: 'MonitorPage' })

const marsMapRef = ref(null)
const leftCollapsed = ref(false)
/** 地图「动态风险」勾选等级：默认只展示极高风险与高风险 */
const dynamicRiskLevels = ref([...DEFAULT_DYNAMIC_RISK_LEVELS])
/** 地图弹窗默认隐藏，点击预警列表项选中后再显示 */
const popupVisible = ref(false)
/** 地图弹窗当前展示的监测点详情（详情字段待接口接入后按选中项请求） */
const pointDetail = ref({ ...monitorPointDetail })

/** 选中监测点在画布中的像素坐标，由 MarsMap 的 risk-point-position 事件持续回抛 */
const riskPointPosition = ref(null)

/**
 * 点位标记尺寸：图标原图 60x68（宽高比 15:17），billboard 会把贴图直接拉伸到 width x height，
 * 所以必须按同一比例等比换算高度，否则图标会被压扁/拉长。弹窗底边按该高度避让图标。
 */
const MARKER_WIDTH = 34
const MARKER_HEIGHT = (MARKER_WIDTH * 68) / 60
const MARKER_GAP = 10

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

/** 左侧面板是否展示预警列表（覆盖统计概况） */
const alarmListVisible = ref(false)
/** 右侧监测预警智能体面板是否展示 */
const agentPanelVisible = ref(false)

/** 右侧面板数据：按点中的预警行字段拼装（列表未返回的字段在拼装层回落「无数据」） */
const agentDetail = ref(null)

/** 右侧面板当前分析的预警行：提交反馈时取其预警主键做归属 */
const agentAlarm = ref(null)

/** 智能体分析请求序号：连续点选预警时只认最后一次响应，避免旧结果盖到新数据上 */
let analysisRequestId = 0

/** 点击「AI数据分析」：先用列表行数据打开面板，曲线描述与分析摘要由接口返回后补上 */
const handleAnalyze = (item) => {
  agentAlarm.value = item
  agentDetail.value = buildWarningAgentDetail(item)
  agentPanelVisible.value = true
  loadWarningAnalysis()
}

/**
 * 拉取监测曲线描述与分析摘要（analyzeMonitorWarning）。
 * 面板已按本地数据先渲染并播放产出节奏，接口结果就地回填，不打断动画；
 * 预警主键或分析时段缺失（列表未返回）时不发请求，文案保持留空，不落 mock
 */
const loadWarningAnalysis = async () => {
  const detail = agentDetail.value
  const warningId = detail?.warningId
  const params = detail?.analysisParams
  if (!warningId || !params) {
    return
  }
  const requestId = ++analysisRequestId
  const data = await analyzeMonitorWarning({ warningId, ...params }).catch(() => null)
  /* 响应回来时已切到别的预警：丢弃本次结果 */
  if (requestId !== analysisRequestId || !data) {
    return
  }
  applyWarningAnalysis(agentDetail.value, data)
}

/** 地图回抛点位像素坐标（点位离开视口时为 null） */
const handleRiskPointPosition = (position) => {
  riskPointPosition.value = position ?? null
}

/** 取预警记录的经纬度：接口返回字符串，统一转数值后再校验 */
const resolveAlarmPoint = (item) => {
  const lng = Number(item?.lon)
  const lat = Number(item?.lat)
  return Number.isFinite(lng) && Number.isFinite(lat) ? { lng, lat } : null
}

/** 设备数据请求序号：连点列表项时只认最后一次响应，避免旧点位数据盖到新点位 */
let deviceRequestId = 0

/**
 * 按设备 ID 补弹窗的设备数据：位移 / 降雨指标与设备状态（两者同源接口，用预警行的 sensorId 当参数）
 * （接口异常、返回为空都保持默认值：指标 '--' 占位、设备状态离线）
 * @param {string} sensorId 预警行的设备 ID
 */
const loadPointDeviceData = async (sensorId) => {
  const requestId = ++deviceRequestId
  if (!sensorId) {
    return
  }
  /* noCheckCode + notUseError 下异常也走 resolve(undefined)，故只按「有没有数据」判断 */
  const data = await getDeviceDataDetail(sensorId).catch(() => null)
  if (requestId !== deviceRequestId || !data) {
    return
  }
  /* 整体替换数组而非改单元格：占位块来自 config 的共享结构，就地改会污染模块状态 */
  const metrics = buildMonitorPointMetrics(data)
  pointDetail.value.displacement = metrics.displacement
  pointDetail.value.rainfall = metrics.rainfall
  /* 设备状态同源返回（deviceStatus：1=在线 / 0=离线）；字段未部署时取不到值，保持离线 */
  pointDetail.value.deviceStatus = buildDeviceStatus(data.deviceStatus)
}

/**
 * 点击预警列表项选中：地图飞行到该监测点打标记，详情弹窗跟随点位展示，
 * 位移 / 降雨指标随后按 sensorId 拉取（未返回前是 '--' 占位）
 */
const handleSelectAlarm = (item) => {
  const point = resolveAlarmPoint(item)
  if (!point) {
    /* 无坐标不伪造位置：清掉上次的标记并提示，等接口补齐经纬度后自动生效 */
    handleClosePopup()
    ElMessage.warning('该预警暂无点位坐标，无法在地图上定位')
    return
  }

  pointDetail.value = {
    /* 指标块来自 monitorPointDetail：按设计稿固定字段常显，先以 '--' 占位，取到数再整体替换 */
    ...monitorPointDetail,
    name: item.name,
    code: item.monitorPointNum,
    /* 接口未返回的字段直接不展示该行，避免出现「设备名称：」这类空值行 */
    info: [
      { label: '地理位置：', value: item.detail?.address ?? '', span: 2 },
      { label: '预警等级：', value: item.level ?? '' },
      { label: '处置状态：', value: item.detail?.processStatus ?? '' },
      { label: '预警时间：', value: item.detail?.publishTime ?? '', span: 2 },
      { label: '设备名称：', value: item.detail?.deviceName ?? '', span: 2 },
    ].filter((row) => row.value),
  }
  popupVisible.value = true
  /* 弹窗先以默认值渲染（指标 '--'、设备状态离线），设备数据取到后再补上 */
  loadPointDeviceData(item.sensorId)
  /* 换点位时先清掉上一个点位的像素坐标，避免弹窗短暂停在旧位置 */
  riskPointPosition.value = null
  /* 标记贴地并常显，避免被三维地形遮挡；宽高必须等比，见 MARKER_WIDTH 注释。
     定位口径与风险评估的斜坡单元定位一致：保留当前航向与俯仰角，1.5s 平滑聚焦，
     不传 keepView 会走默认俯仰 -45°、1.8s 的整视角重置 */
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

/** 关闭监测点弹窗：同步清掉地图标记，避免标记残留在地图上 */
const handleClosePopup = () => {
  popupVisible.value = false
  riskPointPosition.value = null
  marsMapRef.value?.clearRiskPoint()
  /* 弹窗已关闭：让在途的设备数据请求作废，避免响应回来又往详情里写数据 */
  deviceRequestId += 1
}

/** 预警列表返回概况：列表卸载，同步收起地图弹窗与标记 */
const handleAlarmListBack = () => {
  alarmListVisible.value = false
  handleClosePopup()
}

/** 页面卸载：移除 postRender 回调与地图标记，避免内存泄漏 */
onBeforeUnmount(() => {
  handleClosePopup()
})

/** 统计时段切换：按新时段重新拉取概况统计数据（空数组即「全部」，同样要重新取数） */
const handleDateChange = (range) => {
  fetchMonitorOverview(range)
}

/** 地图行政区划选择器（AreaSearch）切换：用选中的区划编码重新拉取概况统计 */
const handleAreaSelect = ({ selected } = {}) => {
  setOverviewRegion(selected?.id)
}

/** 行政区划面板初始化完成：按面板默认区划拉取一次，保证统计区域与地图上展示的区划一致 */
const handleAreaLoaded = ({ root } = {}) => {
  setOverviewRegion(root?.id)
}

/** 记忆选中的区划编码并按其重新取数（编码为空时不动，沿用兜底区划取数） */
const setOverviewRegion = (regionCode) => {
  if (!regionCode) {
    return
  }
  monitorData.regionCode = regionCode
  fetchMonitorOverview()
}
/** 列表筛选的等级文案 → 接口预警等级编码（C1 蓝色 / C2 黄色 / C3 橙色 / C4 红色） */
const WARNING_LEVEL_CODE = { 蓝色告警: 'C1', 黄色告警: 'C2', 橙色告警: 'C3', 红色告警: 'C4' }

/** 导出进行中：避免重复点击产生重复下载 */
const alarmExporting = ref(false)

/**
 * 导出预警处置记录监测列表：按当前筛选条件请求文件流并下载。
 * 接口参数里只有告警等级、时间区间能从筛选条件映射；区划与有效性在列表侧是前端过滤，不传
 */
const handleExportList = async (filters = {}) => {
  if (alarmExporting.value) return
  alarmExporting.value = true
  try {
    const params = {}
    if (WARNING_LEVEL_CODE[filters.level]) params.warningLevel = WARNING_LEVEL_CODE[filters.level]
    /* 时间口径与列表一致：区间两端按 startTime / endTime 下发，未选区间时不传日期 */
    Object.assign(params, buildRangeParams(filters.dateRange))
    const blob = await exportWarningDisposalRecord(params)
    /* 后端异常时会返回 JSON 报文而不是文件流（content-type: application/json），
       先拦掉，避免把一段报错文本存成 .xlsx */
    if (blob?.type?.includes('application/json')) {
      const text = await blob.text()
      console.error('[专业监测] 导出接口返回异常报文:', text)
      ElMessage.error('导出失败，请稍后重试')
      return
    }
    if (!(blob instanceof Blob) || !blob.size) {
      ElMessage.warning('导出内容为空，请调整筛选条件后重试')
      return
    }
    saveAs(blob, `预警处置记录监测列表-${dayjs().format('YYYYMMDD-HHmmss')}.xlsx`)
  } catch (error) {
    console.error('[专业监测] 导出预警处置记录监测列表失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    alarmExporting.value = false
  }
}

/** 面板内「分派给责任人」待接口接入 */
const handleUnavailableAction = () => {
  ElMessage.warning('当前接口未提供该操作')
}

/** 提交反馈弹窗：显示态与提交中态 */
const feedbackVisible = ref(false)
const feedbackSubmitting = ref(false)

/** 右侧面板点「提交反馈」：带上当前预警，弹窗按其现有值回填 */
const openFeedbackDialog = () => {
  if (!agentAlarm.value) {
    ElMessage.warning('请先选择要分析的预警')
    return
  }
  feedbackVisible.value = true
}

/** 提交人工反馈（与群策群防页面同一套接口与成功提示） */
const submitFeedback = async (payload) => {
  feedbackSubmitting.value = true
  try {
    await submitDisReportSuggestion(payload)
    feedbackVisible.value = false
    ElMessage.success('反馈提交成功')
  } finally {
    feedbackSubmitting.value = false
  }
}
</script>

<style lang="less" scoped>
.monitor-page {
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  max-height: 100%;
  min-height: 100%;
  flex: 1 1 auto;
  box-sizing: border-box;
  overflow: visible;
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

/* 地图内容区：左侧为统计面板让出宽度（460 + 12 间距），面板收起时铺满；
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
  bottom: 0;
  left: 0;
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

/* 预警列表：覆盖在统计概况之上，从右滑入 */
.alarm-list-layer {
  position: absolute;
  inset: 0;
  background: #ffffff;
  animation: alarm-layer-in 0.24s ease;
}

@keyframes alarm-layer-in {
  from {
    opacity: 0;
    transform: translateX(24px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 右侧监测预警智能体面板：覆盖层浮在地图与地图控件之上（不挤压地图宽度），滑入效果与其他页面保持一致 */
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

/* 监测点详情气泡：left/top 由地图点位的画布像素坐标驱动，底边贴住点位标记顶部 */
.point-popup-layer {
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
  transform: translateY(-50%) translateX(450px);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s ease;
}

/* 收起：跟随面板左移到内容区左缘外侧，形成骑线效果。
   溢出的 10px 落在左侧菜单的透明内边距上，需页面为 is-fixed-page（`.layout-main` overflow: visible）才不被裁切 */
.left-trigger.is-collapsed {
  transform: translateY(-50%) translateX(-10px);
}
</style>
