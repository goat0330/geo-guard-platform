import { defineStore } from 'pinia'
import { ref, watchEffect } from 'vue'
import { Tools } from '@/components/Cesium/config.js'
import { useLayoutStore } from '@/store/layout.js'
import {
  PRED_RISK_LEVEL_BRAND_TITLE_COLOR,
  PRED_RISK_LEVEL_CARD_BG_COLOR,
  PRED_RISK_LEVEL_COLOR,
  PRED_RISK_LEVEL_LINE_COLOR,
  PRED_RISK_LEVEL_LINK_BTN_COLOR,
  PRED_RISK_LEVEL_TEXT,
  RISK_LEVEL_BRAND_TITLE_COLOR,
  RISK_LEVEL_CARD_BG_COLOR,
  RISK_LEVEL_COLOR,
  RISK_LEVEL_LINE_COLOR,
  RISK_LEVEL_LINK_BTN_COLOR,
  RISK_LEVEL_TEXT,
} from '@/utils/enum.js'

export const useCesiumStore = defineStore('cesium', () => {
  let mapType = ref('588')
  // 当前区域的 Cesium 图层是否已完成绘制。
  let isDrawComplete = ref(false)
  let selectedArea = ref()

  let selectedSlope = ref()
  let statTimeOffset = ref(0) // 统计时间维度-默认为当天

  // 预测类型（1：日 2：周 3：月 4：季 5：年）
  let selectPredType = ref(3)

  // 选择月份
  let searchDate = ref()

  // 地图类型1: 实时，2： 预测
  let selectMapType = ref(1)

  let cesiumTools = ref({
    [Tools.noSearch]: false,
    [Tools.noLayer]: false,
    [Tools.noRiskRank]: false,
    [Tools.noStatistical]: false,
    [Tools.noEqpInfo]: true,
    [Tools.noTool]: false,
    [Tools.noMapSelect]: false,
    [Tools.useMask]: false,
    [Tools.noSelectMapType]: false,
    [Tools.noPredSelect]: false,
    [Tools.noPredLayer]: false,
  })

  function setMapType(type) {
    mapType.value = type
  }

  function setDrawComplete(complete) {
    isDrawComplete.value = complete
  }

  function setSelectedArea(area) {
    selectedArea.value = area
  }

  function setSelectedSlope(slope) {
    selectedSlope.value = slope
  }

  function setStatTimeOffset(offset) {
    statTimeOffset.value = offset
  }

  function setDate(date) {
    searchDate.value = date
  }

  function closeTools(tools) {
    resetCesium(true)
    setTimeout(() => {
      tools.forEach((tool) => (cesiumTools.value[tool] = true))
    }, 0)
  }

  function closeAll() {
    setTimeout(() => {
      const keys = Object.keys(cesiumTools.value)
      keys.forEach((key) => (cesiumTools.value[key] = true))
      cesiumTools.value[Tools.useMask] = false
    }, 0)
  }

  /**
   * 重置cesium
   * @param onlyTools: 仅重置一些面板和操作工具
   * **/
  function resetCesium(onlyTools) {
    const layoutStore = useLayoutStore()
    cesiumTools.value = {
      [Tools.noSearch]: false,
      [Tools.noLayer]: false,
      [Tools.noRiskRank]: false,
      [Tools.noStatistical]: false,
      [Tools.noEqpInfo]: true,
      [Tools.noTool]: false,
      [Tools.noMapSelect]: false,
      [Tools.useMask]: false,
      [Tools.noPredSelect]: false,
      [Tools.noSelectMapType]: false,
      [Tools.noPredLayer]: false,
    }
    if (onlyTools) return
    setSelectedArea(null)
    setSelectedSlope(null)
    setStatTimeOffset(0)
    setMapType('588')
    layoutStore.useCesium = true
  }

  // 重新初始化cesium
  function reInitCesium() {
    const layoutStore = useLayoutStore()
    // 立即作废上一轮绘制完成状态，避免跨页面定位误用旧 Viewer。
    setDrawComplete(false)
    layoutStore.useCesium = false
    setTimeout(() => {
      resetCesium()
    })
  }

  // 获取风险颜色
  function getRiskColor(riskLevel = 0) {
    return selectMapType.value === 2 ? PRED_RISK_LEVEL_COLOR[riskLevel] : RISK_LEVEL_COLOR[riskLevel]
  }

  function getLevelLineColor(riskLevel = 0) {
    return selectMapType.value === 2 ? PRED_RISK_LEVEL_LINE_COLOR[riskLevel] : RISK_LEVEL_LINE_COLOR[riskLevel]
  }

  function getTitleColor(riskLevel = 0) {
    return selectMapType.value === 2
      ? PRED_RISK_LEVEL_BRAND_TITLE_COLOR[riskLevel]
      : RISK_LEVEL_BRAND_TITLE_COLOR[riskLevel]
  }

  function getBtnColor(riskLevel = 0) {
    return selectMapType.value === 2 ? PRED_RISK_LEVEL_LINK_BTN_COLOR[riskLevel] : RISK_LEVEL_LINK_BTN_COLOR[riskLevel]
  }

  function getCardBg(riskLevel = 0) {
    return selectMapType.value === 2 ? PRED_RISK_LEVEL_CARD_BG_COLOR[riskLevel] : RISK_LEVEL_CARD_BG_COLOR[riskLevel]
  }

  function getTitleText(riskLevel = 0) {
    return selectMapType.value === 2 ? PRED_RISK_LEVEL_TEXT[riskLevel] : RISK_LEVEL_TEXT[riskLevel]
  }

  return {
    mapType,
    isDrawComplete,
    selectedArea,
    selectedSlope,
    searchDate,
    setMapType,
    setDrawComplete,
    setSelectedArea,
    setSelectedSlope,
    setStatTimeOffset,
    closeTools,
    cesiumTools,
    resetCesium,
    reInitCesium,
    closeAll,
    selectPredType,
    selectMapType,
    getRiskColor,
    getLevelLineColor,
    getTitleColor,
    getBtnColor,
    getCardBg,
    getTitleText,
    setDate,
  }
})
