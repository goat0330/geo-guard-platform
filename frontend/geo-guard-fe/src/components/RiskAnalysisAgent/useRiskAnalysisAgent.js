import { computed, onUnmounted, ref, watch } from 'vue'

import { getRiskAssessmentSteps, getSlopeUnitRiskDetail } from '@/api/common.js'
import { chat } from '@/api/chat.js'
import { CHAT_TYPE, RISK_LEVEL_TEXT } from '@/utils/enum.js'
import { toFixed } from '@/utils/index.js'
import { createMd } from '@/utils/md.js'
import { formatSlopeName, getDynamicRiskLevelColor, getDynamicRiskLevelLabel } from '@/utils/riskEvaluation.js'

const EMPTY_TEXT = '--'
const ITEM_LOADING_DURATION = 650
const AGENT_TIMEOUT = 30000
const md = createMd({ useBreak: true })

/**
 * 解析斜坡单元名称，优先取真实 name，末尾规范带“斜坡单元”后缀
 * @param {object} data
 * @returns {string}
 */
const resolveSlopeUnitName = (data) => {
  const rawName =
    data?.name ||
    data?.slopeUnit?.name ||
    data?.slopeUnitName ||
    data?.raw?.slopeUnit?.name ||
    data?.raw?.name ||
    data?.rawCode ||
    data?.code ||
    ''
  const formatted = formatSlopeName(rawName)
  if (formatted) return formatted
  // 兜底回退：若名称全为空，才回退到 slopeUnitId / id
  const fallbackId = data?.slopeUnitId || data?.id || ''
  return fallbackId ? formatSlopeName(fallbackId) : EMPTY_TEXT
}

const formatEffectiveRainfall = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(2) : '0.00'
}

const formatRiskLevel = (level) => RISK_LEVEL_TEXT[Number(level)] || EMPTY_TEXT

const parseObject = (value) => {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value
  if (typeof value !== 'string' || !value.trim()) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

const formatRainfall = (list) => {
  let sevenRain = ''
  if (!Array.isArray(list)) return sevenRain

  list.forEach((item) => {
    if (!item || typeof item !== 'object') return
    const key = Object.keys(item)[0]
    if (key) sevenRain += `${key}: ${toFixed(item[key]) || 0}mm\n`
  })
  return sevenRain
}

const buildAnalysisItems = (data) => {
  const unitName = resolveSlopeUnitName(data)
  const vulnerability = parseObject(data?.vulnerabilityDisplayJson)

  return [
    {
      title: `已完成调取${unitName}的基础信息`,
      open: true,
      content: [],
    },
    {
      title: `已完成调取${unitName}“易发性”结果`,
      open: true,
      content: [
        `按照滑坡易发性评价指标体系，智能体将提取地质条件、地形条件、植被条件、水文条件四类指标，调取数据如下：该斜坡为${data?.adjacentWater || '--'}斜坡；斜坡形态为${data?.unitMorphology || '--'}；坡度为${data?.slopeMean || '--'}°；地形起伏${data?.elevationDiff || ''}；工程切坡${vulnerability?.road_house_perimeter || '0'}m；斜坡结构为${data?.slopeStructure || '--'}；工程岩组为${data?.lithologyDesc || '--'}；最近断层距离为${data?.tectonicDist || 0}km；植被覆盖率${data?.vegetationCover || 0}。综合分析，该斜坡易发性等级为${formatRiskLevel(data?.susceptibilityLevel)}。`,
      ],
    },
    {
      title: `已完成调取${unitName}“危险性”结果`,
      open: false,
      content: [
        `根据近7天降雨数据显示，该斜坡\n${formatRainfall(data?.rainfallPast7Daily)}\n前期有效降雨为${formatEffectiveRainfall(data?.rainfallPast7)}mm，预测24小时降雨量为${toFixed(data?.rainfallForecast) || 0}mm；综合诱发性系数为${data?.combinedTimeProb || '--'}。叠加易发性等级，该斜坡危险性等级为${formatRiskLevel(data?.hazardLevel)}。`,
      ],
    },
    {
      title: `已完成调取${unitName}“易损性”结果`,
      open: false,
      content: [
        `基于一标三实等承灾体数据，对斜坡易损性进行分析。该斜坡面积为${vulnerability?.area_m2 || '--'}㎡，常住人口数量为${vulnerability?.pop_count || '--'}人，内有建筑物${vulnerability?.house_count || 0}栋。综合分析，该斜坡易损性等级为${formatRiskLevel(data?.vulnerabilityLevel)}。`,
      ],
    },
    {
      title: `已完成调取${unitName}“风险性”结果`,
      open: true,
      content: [
        `${unitName}的风险性等级为${formatRiskLevel(data?.riskLevel)}风险性，该斜坡危险性等级为${formatRiskLevel(data?.hazardLevel)}危险性，易损性等级为${formatRiskLevel(data?.vulnerabilityLevel)}易损性，依据地质灾害风险等级矩阵分析，综合判定该斜坡单元的风险等级为${formatRiskLevel(data?.riskLevel)}风险性。`,
      ],
    },
  ]
}

/**
 * 单斜坡风险分析数据编排，复用恩施的评价详情与评价步骤组合方式。
 */
export const useRiskAnalysisAgent = (props) => {
  const loading = ref(false)
  const analysisData = ref(null)
  const analysisItems = ref([])
  const loadFailed = ref(false)
  const sequenceComplete = ref(false)
  const revealedItemCount = ref(0)
  const sequenceTimers = new Map()
  let requestVersion = 0
  let isUnmounted = false
  let agentConnection = null
  let cancelAgentRequest = null
  let lastAnalysisCache = null

  const unitName = computed(() => {
    return (
      resolveSlopeUnitName(analysisData.value) ||
      resolveSlopeUnitName(props.slope) ||
      EMPTY_TEXT
    )
  })
  const unitCode = computed(() => props.slope?.slopeUnitId || props.slope?.id || EMPTY_TEXT)
  const dynamicRiskLabel = computed(() => {
    const level = analysisData.value?.dynamicRiskLevel ?? props.slope?.levelValue
    return getDynamicRiskLevelLabel(level) || EMPTY_TEXT
  })
  const dynamicRiskColor = computed(() => {
    const level = analysisData.value?.dynamicRiskLevel ?? props.slope?.levelValue
    return getDynamicRiskLevelColor(level)
  })
  const visibleAnalysisItems = computed(() => analysisItems.value.slice(0, revealedItemCount.value))

  const clearSequence = () => {
    cancelAgentRequest?.()
    cancelAgentRequest = null
    sequenceTimers.forEach((resolve, timer) => {
      window.clearTimeout(timer)
      resolve(false)
    })
    sequenceTimers.clear()
  }

  const waitForItem = () => {
    return new Promise((resolve) => {
      const timer = window.setTimeout(() => {
        sequenceTimers.delete(timer)
        resolve(true)
      }, ITEM_LOADING_DURATION)
      sequenceTimers.set(timer, resolve)
    })
  }

  const cloneAnalysisItems = (items) => {
    return items.map((item) => ({
      ...item,
      content: [...item.content],
      loading: false,
    }))
  }

/**
 * 提取工作流结束事件中的最终完整文本
 * @param {object} data
 * @returns {string|null}
 */
const getWorkflowFinishedAnswer = (data) => {
  const answer =
    data?.data?.outputs?.answer ??
    data?.data?.outputs?.text ??
    data?.data?.outputs?.result ??
    data?.data?.outputs?.content ??
    data?.outputs?.answer ??
    data?.outputs?.text ??
    data?.outputs?.result ??
    data?.outputs?.content ??
    data?.answer ??
    data?.text ??
    null

  if (typeof answer === 'string' && answer.trim()) {
    return answer
  }
  return null
}

  const loadAgentBaseInfo = (slopeUnitId, item, version) => {
    return new Promise((resolve) => {
      let content = ''
      let settled = false
      const finish = (finalContent) => {
        if (settled) return
        settled = true
        cancelAgentRequest = null
        window.clearTimeout(timeoutTimer)
        agentConnection?.abort()
        agentConnection = null
        if (!isUnmounted && version === requestVersion) {
          // 完结时优先使用工作流最终文本替换，若为空则保留流式累积文本
          const targetContent =
            finalContent !== undefined && finalContent !== null && finalContent !== ''
              ? finalContent
              : content
          item.contentHtml = targetContent ? md.render(targetContent) : ''
          item.content = targetContent ? [] : ['智能体暂未返回该斜坡单元的基础信息']
        }
        resolve()
      }
      const timeoutTimer = window.setTimeout(() => finish(), AGENT_TIMEOUT)
      cancelAgentRequest = () => finish()

      agentConnection = chat({
        params: {
          query: '@query_slope_unit',
          agentType: CHAT_TYPE.NOT_SHOW,
          inputs: { slope: slopeUnitId },
        },
        onMessage: (data) => {
          if (isUnmounted || version !== requestVersion) {
            finish()
            return
          }
          // 读到 message 就实时拼接
          if (data?.eventType === 'MESSAGE') {
            const chunk = data?.answer ?? data?.data?.answer ?? data?.text ?? ''
            if (typeof chunk === 'string' && chunk) {
              content += chunk
              item.contentHtml = md.render(content)
            }
          } else if (data?.eventType === 'WORKFLOW_FINISHED') {
            // 完结的时候再用完结的内容替换
            const finalAnswer = getWorkflowFinishedAnswer(data)
            finish(finalAnswer)
          }
        },
      })
    })
  }

  const playSequence = async (version, assessmentId) => {
    const list = analysisItems.value
    for (let index = 0; index < list.length; index++) {
      if (isUnmounted || version !== requestVersion) return
      const item = list[index]
      revealedItemCount.value = index + 1
      item.loading = true

      if (index === 0) {
        const querySlope =
          analysisData.value?.slopeUnitId ||
          props.slope?.slopeUnitId ||
          props.slope?.raw?.slopeUnitId ||
          props.slope?.id ||
          props.slope?.raw?.id ||
          unitName.value
        await loadAgentBaseInfo(querySlope, item, version)
      } else {
        const completed = await waitForItem()
        if (!completed) return
      }

      if (isUnmounted || version !== requestVersion) return
      item.loading = false
      // 每个板块数据出现时默认展开，后续仍允许用户手动收起。
      item.open = true
    }
    revealedItemCount.value = list.length
    sequenceComplete.value = true
    lastAnalysisCache = {
      assessmentId,
      data: { ...analysisData.value },
      items: cloneAnalysisItems(list),
    }
  }

  const loadAnalysis = async () => {
    const version = ++requestVersion
    clearSequence()
    sequenceComplete.value = false
    revealedItemCount.value = 0
    const assessmentId = props.slope?.id || props.slope?.raw?.id
    if (!props.visible || !assessmentId) {
      loading.value = false
      analysisData.value = null
      analysisItems.value = []
      loadFailed.value = Boolean(props.visible && !assessmentId)
      return
    }

    if (String(lastAnalysisCache?.assessmentId) === String(assessmentId)) {
      analysisData.value = { ...lastAnalysisCache.data }
      analysisItems.value = cloneAnalysisItems(lastAnalysisCache.items)
      revealedItemCount.value = analysisItems.value.length
      sequenceComplete.value = true
      loadFailed.value = false
      loading.value = false
      return
    }

    loading.value = true
    loadFailed.value = false
    try {
      const [detailResult, stepsResult] = await Promise.allSettled([
        getSlopeUnitRiskDetail(assessmentId),
        getRiskAssessmentSteps({ assessmentId }),
      ])
      if (isUnmounted || version !== requestVersion) return

      const detail = detailResult.status === 'fulfilled' ? detailResult.value : null
      const steps = stepsResult.status === 'fulfilled' && Array.isArray(stepsResult.value) ? stepsResult.value : []
      if (!detail && !props.slope?.raw) throw new Error('风险评价详情为空')

      const merged = {
        ...(props.slope?.raw || {}),
        ...(detail?.slopeUnit || props.slope?.raw?.slopeUnit || {}),
        ...(steps[0] || {}),
        // 评价详情中的数值等级为最终结果，不能被斜坡基础信息中的中文等级覆盖。
        ...(detail || {}),
        name: detail?.slopeUnit?.name || props.slope?.raw?.slopeUnit?.name || props.slope?.name || props.slope?.rawCode || '',
        slopeUnitId: detail?.slopeUnit?.id || detail?.slopeUnitId || props.slope?.slopeUnitId || '',
      }
      analysisData.value = merged
      const items = buildAnalysisItems(merged).map((item) => ({
        ...item,
        loading: false,
        contentHtml: '',
      }))
      analysisItems.value = items
      void playSequence(version, assessmentId)
    } catch {
      if (isUnmounted || version !== requestVersion) return
      analysisData.value = null
      analysisItems.value = []
      loadFailed.value = true
    } finally {
      if (!isUnmounted && version === requestVersion) loading.value = false
    }
  }

  const toggleItem = (item) => {
    item.open = !item.open
  }

  watch(
    [
      () => props.visible,
      () => props.slope?.id,
      () => props.slope?.slopeUnitId,
      () => props.slope?.raw?.id,
      () => props.slope?.raw?.slopeUnitId,
    ],
    () => void loadAnalysis(),
    { immediate: true },
  )

  onUnmounted(() => {
    isUnmounted = true
    requestVersion += 1
    clearSequence()
  })

  return {
    analysisItems,
    dynamicRiskColor,
    dynamicRiskLabel,
    loadFailed,
    loading,
    sequenceComplete,
    toggleItem,
    unitCode,
    unitName,
    visibleAnalysisItems,
  }
}
