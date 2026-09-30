import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { riskRegionParams } from '@/components/RiskSlopePanel/useRiskDataset.js'
import dayjs from 'dayjs'

import { getAreaListWithOutWk, getChatBannerStat, getRiskStatistic } from '@/api/common.js'
import riskExtreme from '@/assets/imgs/fengxian/risk-extreme.png'
import riskHigh from '@/assets/imgs/fengxian/risk-high.png'
import riskLow from '@/assets/imgs/fengxian/risk-low.png'
import riskMedium from '@/assets/imgs/fengxian/risk-medium.png'
import trendDown from '@/assets/imgs/fengxian/trend-down.png'
import trendUp from '@/assets/imgs/fengxian/trend-up.png'

const DEFAULT_COUNTY = '彭水苗族土家族自治县'

const RISK_LEVEL_META = [
  { level: 4, label: '极高', icon: riskExtreme, color: '#E45B5B' },
  { level: 3, label: '高', icon: riskHigh, color: '#FF922C' },
  { level: 2, label: '中', icon: riskMedium, color: '#FFBD63' },
  { level: 1, label: '低', icon: riskLow, color: '#007BFF' },
]

const PERIOD_OFFSET_MAP = {
  today: 1,
  '3days': 3,
  '7days': 7,
}

const toFiniteNumber = (value) => {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

const formatCount = (value, fallback = '') => {
  const number = toFiniteNumber(value)
  return number === null ? fallback : number.toLocaleString('zh-CN')
}

const formatDecimal = (value, fallback = '') => {
  const number = toFiniteNumber(value)
  return number === null ? fallback : number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const formatUpdatedTime = (date) => {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

/**
 * 格式化超长或规范行政区名称
 */
export const formatRegionName = (name = '') => {
  if (!name) return ''
  if (name === '恩施土家族苗族自治州') return '恩施州'
  return name
}

/**
 * 根据级联链路推导请求入参
 */
const extractRegionParams = (pathNodes) => {
  const params = {}
  if (!Array.isArray(pathNodes)) return params

  pathNodes.forEach((node) => Object.assign(params, riskRegionParams(node)))

  return params
}

/**
 * 风险概览数据编排：统一管理概览、等级变化和乡镇排序，支持行政区与日期联动。
 */
export const useRiskOverview = (dataset = null) => {
  const overview = ref(null)
  const statistic = ref(null)
  const selectedPeriod = ref('today')
  const updatedTime = ref('')
  let requestVersion = 0
  let overviewVersion = 0
  let isUnmounted = false

  // 行政区划树相关状态
  const regionTree = ref([])
  const regionNodeMap = new Map()
  const selectedRegionValue = ref([])
  const selectedRegionName = ref(DEFAULT_COUNTY)
  const currentRegionParams = ref({ county: DEFAULT_COUNTY })

  // 日期状态
  const currentDate = ref(dayjs().format('YYYY-MM-DD'))

  const summaryValues = computed(() => ({
    riskSlopeCount: formatCount(overview.value?.riskSlopeCount, '0'),
    riskZoneCount: formatCount(overview.value?.riskZoneCount, '0'),
    coveredAreaKm2: formatDecimal(overview.value?.coveredAreaKm2, '0'),
    affectedPopulationCount: formatCount(overview.value?.affectedPopulationCount, '0'),
    affectedBuildingCount: formatCount(overview.value?.affectedBuildingCount, '0'),
    coveredTownCount: formatCount(overview.value?.coveredTownCount, '0'),
  }))

  const riskLevels = computed(() => {
    const hasStatistic = statistic.value !== null
    const countMap = new Map(
      (Array.isArray(statistic.value?.stat) ? statistic.value.stat : []).map((item) => [
        Number(item?.dynamicRiskLevel),
        toFiniteNumber(item?.count) ?? 0,
      ]),
    )
    const total = RISK_LEVEL_META.reduce((sum, item) => sum + (countMap.get(item.level) ?? 0), 0)

    return RISK_LEVEL_META.map((item) => {
      const count = countMap.get(item.level) ?? 0
      const diff = toFiniteNumber(statistic.value?.diffRisk?.[String(item.level)]) ?? 0
      const percent = total > 0 ? (count / total) * 100 : 0

      return {
        ...item,
        count: hasStatistic ? formatCount(count) : '',
        percent: hasStatistic ? `${Number(percent.toFixed(1))}%` : '',
        width: hasStatistic ? `${percent}%` : '0%',
        trendText: hasStatistic ? (diff > 0 ? `+${diff}` : diff < 0 ? String(diff) : '0 -') : '',
        trendIcon: hasStatistic && diff > 0 ? trendUp : hasStatistic && diff < 0 ? trendDown : '',
        trendClass: hasStatistic && diff > 0 ? 'up' : hasStatistic && diff < 0 ? 'down' : '',
      }
    })
  })

  const towns = computed(() => {
    const list = Array.isArray(overview.value?.townRiskRanking) ? overview.value.townRiskRanking : []
    return list.map((town) => {
      const veryHigh = toFiniteNumber(town?.veryHighRiskCount) ?? 0
      const high = toFiniteNumber(town?.highRiskCount) ?? 0
      // 图表只展示极高和高风险单元，总数必须与两组柱状数据保持同一口径。
      const total = veryHigh + high

      return {
        key: town?.townCode || town?.townName || town?.rank,
        rank: toFiniteNumber(town?.rank),
        name: town?.townName ?? '',
        total: formatCount(total),
        totalNumber: total,
        veryHighNumber: veryHigh,
        highNumber: high,
        highest: total > 0 ? `${(veryHigh / total) * 100}%` : '0%',
        high: total > 0 ? `${(high / total) * 100}%` : '0%',
      }
    })
  })

  const loadOverview = async () => {
    const version = ++overviewVersion
    overview.value = null
    // 总体范围按行政区划统计最新快照，不传递日期参数
    const data = await getChatBannerStat({
      ...currentRegionParams.value,
    })
    if (isUnmounted || version !== overviewVersion) return
    overview.value = data ?? null
    updatedTime.value = formatUpdatedTime(new Date())
  }

  const loadRiskStatistic = async () => {
    const version = ++requestVersion
    statistic.value = null
    const data = await getRiskStatistic({
      ...currentRegionParams.value,
      assessmentDate: currentDate.value,
      date: currentDate.value,
      offset: PERIOD_OFFSET_MAP[selectedPeriod.value],
    })
    // 下拉快速切换时，仅保留最后一次请求结果，避免旧响应覆盖新周期。
    if (isUnmounted || version !== requestVersion) return
    statistic.value = data ?? null
  }

  const changePeriod = (period) => {
    if (!Object.hasOwn(PERIOD_OFFSET_MAP, period) || period === selectedPeriod.value) return
    selectedPeriod.value = period
    loadRiskStatistic().catch(() => {})
  }

  /**
   * 初始化行政区划树
   */
  const initRegionTree = async () => {
    try {
      const res = await getAreaListWithOutWk()
      const list = Array.isArray(res) ? res : (res?.data || [])
      if (!Array.isArray(list) || list.length === 0) return

      let defaultTargetNode = null
      let defaultPath = []

      // 递归处理树结构以适应 el-cascader，并建立索引
      const sanitizeTree = (nodes, parentPath = []) => {
        return nodes.map((node) => {
          const currentPath = [...parentPath, node]
          regionNodeMap.set(String(node.id), { node, path: currentPath })

          // 匹配彭水县作为默认选中国家
          if (!defaultTargetNode && (String(node.id) === '500243' || node.name?.includes('彭水'))) {
            defaultTargetNode = node
            defaultPath = currentPath
          }

          const hasChildren = Array.isArray(node.children) && node.children.length > 0
          return {
            id: String(node.id),
            name: node.name,
            displayName: formatRegionName(node.name),
            level: node.level,
            children: hasChildren ? sanitizeTree(node.children, currentPath) : undefined,
          }
        })
      }

      regionTree.value = sanitizeTree(list)

      if (defaultTargetNode) {
        selectedRegionValue.value = defaultPath.map((item) => String(item.id))
        selectedRegionName.value = formatRegionName(defaultTargetNode.name)
        currentRegionParams.value = extractRegionParams(defaultPath)
      } else if (list[0]) {
        selectedRegionValue.value = [String(list[0].id)]
        selectedRegionName.value = formatRegionName(list[0].name)
        currentRegionParams.value = extractRegionParams([list[0]])
      }
    } catch (e) {
      console.error('获取行政区划树失败:', e)
    }
  }

  /**
   * 切换行政区划
   * @param {string[]|string|object} val 选中的节点 id 链路、id，或包含 { selected, path } 的对象
   */
  const changeRegion = (val) => {
    let targetNode = null
    let targetPath = []

    if (val && typeof val === 'object' && val.selected) {
      targetNode = val.selected
      targetPath = Array.isArray(val.path) && val.path.length > 0 ? val.path : [val.selected]
    } else {
      const targetId = String(Array.isArray(val) ? val[val.length - 1] : (val?.id || val || ''))
      if (!targetId) return null
      const info = regionNodeMap.get(targetId)
      if (info) {
        targetNode = info.node
        targetPath = info.path
      } else if (val && typeof val === 'object' && val.name) {
        targetNode = val
        targetPath = [val]
      }
    }

    if (!targetNode) return null

    selectedRegionValue.value = targetPath.map((item) => String(item.id))
    selectedRegionName.value = formatRegionName(targetNode.name)
    currentRegionParams.value = extractRegionParams(targetPath)
    dataset?.setRegion(targetNode)
    if (dataset) currentRegionParams.value = dataset.region.value

    // 选择后立即获取数据
    Promise.allSettled([loadOverview(), loadRiskStatistic()])

    return {
      selected: targetNode,
      path: targetPath,
      params: currentRegionParams.value,
    }
  }

  /**
   * 切换日期：选择后立即获取数据
   */
  const changeDate = (newDate) => {
    if (!newDate) return
    currentDate.value = newDate
    if (dataset) dataset.filters.assessmentDate = newDate
    Promise.allSettled([loadOverview(), loadRiskStatistic()])
  }

  onMounted(async () => {
    await initRegionTree()
    if (dataset) currentRegionParams.value = dataset.region.value
    Promise.allSettled([loadOverview(), loadRiskStatistic()])
  })

  // 地图、列表改日期或行政区时，看板同样使用当前筛选条件。
  if (dataset) watch([dataset.region, () => dataset.filters.assessmentDate], ([region, date]) => {
    if (currentDate.value === date && JSON.stringify(currentRegionParams.value) === JSON.stringify(region)) return
    currentRegionParams.value = region
    currentDate.value = date
    Promise.allSettled([loadOverview(), loadRiskStatistic()])
  })

  onUnmounted(() => {
    isUnmounted = true
    requestVersion += 1
  })

  return {
    changeDate,
    changePeriod,
    changeRegion,
    countyName: selectedRegionName,
    currentDate,
    regionTree,
    riskLevels,
    selectedPeriod,
    selectedRegionName,
    selectedRegionValue,
    summaryValues,
    towns,
    updatedTime,
  }
}
