<template>
  <aside class="report-agent-panel" aria-label="复盘报告智能体执行过程">
    <!-- 头部：标题与关闭折叠按钮（和风险评价智能体保持一致） -->
    <header class="panel-header">
      <div class="header-brand">
        <img :src="agentIcon" alt="智能体图标" class="brand-avatar" />
        <h2 class="brand-title">复盘报告智能体</h2>
      </div>
      <div class="header-actions">
        <button
          type="button"
          class="close-btn"
          title="收起智能体"
          @click="emit('close')"
        >
          <i class="iconfont icon-close"></i>
        </button>
      </div>
    </header>

    <!-- 滚动交互区 -->
    <div class="panel-content">
      <!-- 开场白 -->
      <p class="intro-bubble">
        好的，我将基于<span class="highlight-title">「{{ eventTitle }}」</span>事件资料，逐步还原处置经过、核验证据并形成复盘报告:
      </p>

      <!-- 步骤 1：已完成识别事件与复盘范围（框样式与风险评价智能体一致） -->
      <article v-if="step1Visible" class="analysis-item message-enter">
        <button
          type="button"
          class="analysis-title"
          :disabled="step1Loading"
          @click="emit('toggle-step', 1)"
        >
          <span>
            <el-icon class="step-check-icon"><CircleCheck /></el-icon>
            已完成识别事件与复盘范围
          </span>
          <i
            v-if="!step1Loading"
            class="iconfont icon-arrow-down"
            :class="{ 'is-folded': !step1Open }"
          ></i>
        </button>

        <!-- 假 loading 动效（参考事件分析） -->
        <div v-if="step1Loading" class="analysis-loading" aria-label="正在分析...">
          <i></i><i></i><i></i>
        </div>

        <!-- 展开明细 -->
        <div v-else-if="step1Open" class="analysis-detail">
          <p
            v-for="(item, index) in step1Items"
            :key="index"
          >
            <span class="detail-label">{{ item.label }}：</span>
            <span class="detail-value">{{ item.value }}</span>
          </p>
        </div>
      </article>

      <!-- 步骤 2：已完成汇集并核验复盘资料 -->
      <article v-if="step2Visible" class="analysis-item message-enter">
        <button
          type="button"
          class="analysis-title"
          :disabled="step2Loading"
          @click="emit('toggle-step', 2)"
        >
          <span>
            <el-icon class="step-check-icon"><CircleCheck /></el-icon>
            已完成汇集并核验复盘资料
          </span>
          <i
            v-if="!step2Loading"
            class="iconfont icon-arrow-down"
            :class="{ 'is-folded': !step2Open }"
          ></i>
        </button>

        <!-- 假 loading 动效 -->
        <div v-if="step2Loading" class="analysis-loading" aria-label="正在核验资料...">
          <i></i><i></i><i></i>
        </div>

        <!-- 展开明细 -->
        <div v-else-if="step2Open" class="analysis-detail">
          <p
            v-for="(item, index) in step2Items"
            :key="index"
          >
            <span class="detail-label">{{ item.label }}：</span>
            <span class="detail-value">{{ item.value }}</span>
          </p>
        </div>
      </article>

      <!-- 步骤 3：已汇聚变化依据 -->
      <article v-if="step3Visible" class="analysis-item message-enter">
        <button
          type="button"
          class="analysis-title"
          @click="emit('toggle-step', 3)"
        >
          <span>
            <el-icon class="step-check-icon"><CircleCheck /></el-icon>
            已汇聚变化依据
          </span>
          <i
            class="iconfont icon-arrow-down"
            :class="{ 'is-folded': !step3Open }"
          ></i>
        </button>

        <!-- 展开明细 -->
        <div v-if="step3Open" class="analysis-detail">
          <p>
            <span class="detail-label">本步结论：</span>
            <span class="detail-value">{{ step3Conclusion }}</span>
          </p>
        </div>
      </article>

      <!-- 底部高亮总结提示 -->
      <Transition name="fade">
        <div v-if="summaryVisible" class="summary-notice">
          <p class="summary-text">
            综上所述，当前已锁定<strong>1起复盘事件</strong>，核验<strong>{{ summaryInfo.materialsText }}</strong>，识别出<strong>{{ summaryInfo.diffText }}</strong>和<span class="summary-alert">{{ summaryInfo.gapsText }}。</span>
          </p>
        </div>
      </Transition>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import agentIcon from '@/assets/imgs/fupan/header.png'

defineOptions({ name: 'ReportAgentPanel' })

const props = defineProps({
  event: {
    type: Object,
    default: () => ({}),
  },
  detail: {
    type: Object,
    default: () => ({}),
  },
  step1Loading: Boolean,
  step1Open: Boolean,
  step1Visible: Boolean,
  step2Loading: Boolean,
  step2Open: Boolean,
  step2Visible: Boolean,
  step3Open: Boolean,
  step3Visible: Boolean,
  summaryVisible: Boolean,
})

const emit = defineEmits(['close', 'toggle-step'])

/**
 * 格式化发生时间为“YYYY年M月D日H时m分”
 * @param {string} timeStr - 原始时间字符串
 * @returns {string} 格式化时间或 --
 */
const formatOccurrenceTime = (timeStr) => {
  if (!timeStr) return '--'
  const match = String(timeStr).match(/^(\d{4})-(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{1,2})/)
  if (match) {
    const [, year, month, day, hour, minute] = match
    return `${year}年${Number(month)}月${Number(day)}日${Number(hour)}时${minute}分`
  }
  return String(timeStr)
}

/**
 * 格式化人员伤亡情况
 * @param {object} impactInfo - 影响情况
 * @param {object} event - 备用事件对象
 * @returns {string} 伤亡描述
 */
const formatCasualties = (impactInfo, event) => {
  const injured = impactInfo?.injuredPeople
  const dead = impactInfo?.deadPeople
  const casualties = event?.casualties

  if ((injured === 0 && dead === 0) || casualties === 0) {
    return '无人员伤亡'
  }
  if (
    (injured !== undefined && injured !== null && injured > 0) ||
    (dead !== undefined && dead !== null && dead > 0)
  ) {
    const parts = []
    if (injured > 0) parts.push(`伤${injured}人`)
    if (dead > 0) parts.push(`亡${dead}人`)
    return parts.join('，')
  }
  if (casualties !== undefined && casualties !== null && casualties > 0) {
    return `伤亡${casualties}人`
  }
  return '--'
}

// 事件标题
const eventTitle = computed(() => {
  return (
    props.detail?.basicInfo?.disasterName ||
    props.event?.eventName ||
    props.event?.title ||
    '--'
  )
})

// 步骤 1 明细数据
const step1Items = computed(() => {
  // 1. 事件对象
  // 模板："2026年4月11日16时35分，彭水县高谷镇陈家村6组G319国道2270处发生小型岩质边坡崩塌。"
  const occurrenceTimeRaw =
    props.detail?.basicInfo?.occurrenceTime || props.event?.occurrenceTime
  const formattedTime = formatOccurrenceTime(occurrenceTimeRaw)

  const address =
    props.detail?.basicInfo?.detailedAddress ||
    props.event?.displayAddress ||
    props.event?.detailedAddress ||
    props.event?.location ||
    '--'

  const level =
    props.detail?.basicInfo?.disasterLevel ||
    props.event?.scaleLevel ||
    props.event?.eventLevelName ||
    ''

  const nature = props.detail?.impactInfo?.disasterNature || ''

  const type =
    props.detail?.basicInfo?.disasterType ||
    props.event?.disasterType ||
    props.event?.eventTypeName ||
    '--'

  const disasterDesc = `${level}${nature ? `${nature}边坡` : ''}${type}`
  const eventObjectValue = `${formattedTime}，${address}发生${disasterDesc}。`

  // 2. 影响情况
  // 模板："约25立方米堆积体阻断道路，无人员伤亡;坡面仍有约100立方米破碎岩体。"
  const debrisVol = props.detail?.impactInfo?.debrisVolume
  const debrisVolText =
    debrisVol !== null && debrisVol !== undefined
      ? `约${debrisVol}立方米堆积体`
      : '约--堆积体'

  const threat =
    props.detail?.impactInfo?.threatObject || props.event?.threatObject
  const traffic = props.detail?.impactInfo?.trafficStatus

  let threatDesc = '阻断道路'
  if (threat) {
    threatDesc = threat.startsWith('威胁') ? threat : `威胁${threat}`
  } else if (traffic) {
    threatDesc = traffic
  }

  const casualtiesDesc = formatCasualties(props.detail?.impactInfo, props.event)

  const residualVol = props.detail?.impactInfo?.residualVolume
  const residualVolText =
    residualVol !== null && residualVol !== undefined
      ? `坡面仍有约${residualVol}立方米破碎岩体。`
      : '坡面仍有约--破碎岩体。'

  const impactValue = `${debrisVolText}${threatDesc}，${casualtiesDesc};${residualVolText}`

  // 3. 复盘范围
  // 模板："覆盖险情上报、任务派发、现场管控、专业调查、会商研判、排险恢复和闭环归档。"
  let reviewScopeValue = props.detail?.reviewScope
  if (!reviewScopeValue) {
    const timelineNodes = (props.detail?.timeline || [])
      .map((item) => item?.name)
      .filter(Boolean)
    reviewScopeValue =
      timelineNodes.length > 0 ? `覆盖${timelineNodes.join('、')}。` : '--'
  }

  return [
    { label: '事件对象', value: eventObjectValue },
    { label: '影响情况', value: impactValue },
    { label: '复盘范围', value: reviewScopeValue },
  ]
})

// 步骤 2 明细数据
const step2Items = computed(() => {
  // 1. 已读取
  // 模板："现场调查报告、3张现场图片、任务流转记录、当日风险评价及相关雨量信息，共12项资料。"
  const materialInfo = props.detail?.materialInfo || {}
  const reportCount =
    materialInfo.investigationReportCount ?? props.event?.investigationReportCount
  const photoCount =
    materialInfo.photoCount ?? props.event?.scenePhotoCount
  const taskCount = materialInfo.taskRecordCount
  const riskCount = materialInfo.riskAssessmentCount

  const reportPart =
    reportCount !== undefined && reportCount !== null
      ? `${reportCount}份现场调查报告`
      : '现场调查报告(--)'

  const photoPart =
    photoCount !== undefined && photoCount !== null
      ? `${photoCount}张现场图片`
      : '现场图片(--)'

  const taskPart =
    taskCount !== undefined && taskCount !== null
      ? `${taskCount}项任务流转记录`
      : '任务流转记录(--)'

  const riskPart =
    riskCount !== undefined && riskCount !== null
      ? `${riskCount}项当日风险评价`
      : '当日风险评价(--)'

  const attachmentsCount =
    materialInfo.totalEvidenceCount ??
    materialInfo.attachments?.length ??
    (reportCount != null || photoCount != null
      ? Number(reportCount || 0) +
        Number(photoCount || 0) +
        Number(taskCount || 0) +
        Number(riskCount || 0)
      : null)

  const totalCountText =
    attachmentsCount !== null && attachmentsCount !== undefined
      ? `共${attachmentsCount}项资料`
      : '共--项资料'

  const readValue = `${reportPart}、${photoPart}、${taskPart}、${riskPart}及相关雨量信息(--)，${totalCountText}。`

  // 2. 来源分级
  // 模板："区分调查报告、系统记录、图片辅助判读和示例补全;推测内容不直接作为正式事实。"
  const sourceGradingValue = props.detail?.sourceClassification || '--'

  // 3. 发现差异
  // 模板："调查报告记载发生时间为16时30分，系统记录为16时35分08秒，需人工核对。"
  const diffs = props.detail?.evidenceDifferences || []
  const diffConclusions = diffs.map((d) => d?.conclusion).filter(Boolean)
  let diffValue = '--'
  if (diffConclusions.length > 0) {
    diffValue = diffConclusions.join('；')
  } else {
    const timeline = props.detail?.timeline || []
    const warningList = timeline.map((item) => item?.warning).filter(Boolean)
    if (warningList.length > 0) {
      diffValue = warningList.join('；')
    }
  }

  // 4. 资料缺口
  // 模板："道路恢复、残余危岩验收和响应终止记录尚未形成完整回执。"
  const lifecycleGaps = props.detail?.lifecycleGaps
  const gaps =
    Array.isArray(lifecycleGaps) && lifecycleGaps.length > 0
      ? lifecycleGaps
      : props.detail?.dataGaps || []
  const gapsValue = gaps.length > 0 ? `${gaps.join('、')}尚未形成完整回执。` : '--'

  return [
    { label: '已读取', value: readValue },
    { label: '来源分级', value: sourceGradingValue },
    { label: '发现差异', value: diffValue },
    { label: '资料缺口', value: gapsValue },
  ]
})

// 步骤 3 本步结论
// 模板："正在关联上报、签收、到场、会商、清障和恢复记录..."
const step3Conclusion = computed(() => {
  const customConclusion = props.detail?.stepConclusions?.changeBasisConclusion
  if (customConclusion) {
    return customConclusion
  }
  const timelineNodes = (props.detail?.timeline || [])
    .map((item) => item?.name)
    .filter(Boolean)

  if (timelineNodes.length > 0) {
    return `正在关联${timelineNodes.join('、')}记录...`
  }
  return '--'
})

// 底部高亮总结数据
// 模板："综上所述，当前已锁定1起复盘事件，核验12项资料，识别出1处时间差异和3类关键资料缺口。"
const summaryInfo = computed(() => {
  const materialInfo = props.detail?.materialInfo || {}
  const reportCount =
    materialInfo.investigationReportCount ?? props.event?.investigationReportCount
  const photoCount =
    materialInfo.photoCount ?? props.event?.scenePhotoCount

  const attachmentsCount =
    materialInfo.attachments?.length ??
    (reportCount != null || photoCount != null
      ? Number(reportCount || 0) + Number(photoCount || 0)
      : null)

  const materialsText =
    attachmentsCount !== null && attachmentsCount !== undefined
      ? `${attachmentsCount}项资料`
      : '--'

  const timeline = props.detail?.timeline || []
  const warningCount = timeline.filter((item) => Boolean(item?.warning)).length
  const diffText = warningCount > 0 ? `${warningCount}处时间差异` : '--'

  const gaps = props.detail?.dataGaps || []
  const gapsText = gaps.length > 0 ? `${gaps.length}类关键资料缺口` : '--'

  return {
    materialsText,
    diffText,
    gapsText,
  }
})
</script>

<style lang="less" scoped src="./ReportAgentPanel.less"></style>
