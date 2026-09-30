import { computed, onScopeDispose, reactive, shallowRef, ref, watch, provide } from 'vue'
import dayjs from 'dayjs'
import { debounce } from 'lodash-es'
import { getRiskAssessmentList, resolveRiskAssessmentPage } from '@/api/riskEvaluation.js'
import { formatRiskSlope, getRiskSlopeSelectionKey } from '@/utils/riskEvaluation.js'
import { SLOPE_UNIT_KEY } from '@/utils/enum.js'

// Symbol.for 在 Vite 热更新后仍保持同一标识，避免兄弟面板取不到共享数据。
export const RISK_DATASET_KEY = Symbol.for('geo-guard:risk-dataset')

// 与恩施一致：行政级别直接映射参数名，不根据显示名称推测级别。
export const riskRegionParams = (selected) => {
  const key = SLOPE_UNIT_KEY[selected?.level]
  if (!key || !selected?.name) return {}
  return { [key]: selected.name }
}

export const useRiskDataset = () => {
  const filters = reactive({ slopeUnitId: '', dynamicRiskLevels: [4, 3], assessmentDate: dayjs().format('YYYY-MM-DD') })
  const region = shallowRef({ county: '彭水苗族土家族自治县' })
  const rows = shallowRef([])
  const initialLoading = ref(false)
  const loadError = ref('')
  const activeSlopeUnitId = ref('')
  const activeSlopeKey = ref('')
  let version = 0
  let controller
  const formattedRows = computed(() =>
    rows.value.map((row) => ({ ...formatRiskSlope(row), virtualId: String(row.id) })),
  )
  const slopeItems = computed(() => {
    if (!filters.dynamicRiskLevels.length) return []

    return formattedRows.value.filter(
      (item) =>
        filters.dynamicRiskLevels.includes(item.levelValue) &&
        (!filters.slopeUnitId || String(item.slopeUnitId).includes(filters.slopeUnitId.trim())),
    )
  })
  const search = async () => {
    const requestVersion = ++version
    controller?.abort()
    controller = new AbortController()
    const signal = controller.signal
    initialLoading.value = true
    loadError.value = ''
    rows.value = []
    if (filters.dynamicRiskLevels.length === 0) {
      initialLoading.value = false
      return
    }
    try {
      // 该接口不传分页参数时会返回当天全量评估数据；pageSize=10000 会让服务端超时。
      const params = {
        ...region.value,
        assessmentDate: filters.assessmentDate,
        dynamicRiskLevels: [...filters.dynamicRiskLevels],
        withSlopeUnit: true,
      }
      // riskAssessment/list 的数组参数必须序列化为同名重复参数，不能使用 axios 默认的 [] 形式。
      const response = await getRiskAssessmentList(params, { signal })
      if (signal.aborted || requestVersion !== version) return
      const allRows = Array.isArray(response) ? response : resolveRiskAssessmentPage(response).rows
      if (requestVersion === version) rows.value = allRows
    } catch (error) {
      if (requestVersion === version && !signal.aborted) {
        loadError.value = '风险数据加载失败，请点击查询重试'
        console.error('风险图层获取失败', error)
      }
    } finally {
      if (requestVersion === version) initialLoading.value = false
    }
  }
  const setRegion = (selected) => {
    const next = riskRegionParams(selected)
    if (JSON.stringify(next) !== JSON.stringify(region.value)) region.value = next
  }
  const reset = () => {
    filters.slopeUnitId = ''
    filters.dynamicRiskLevels = [4, 3, 2, 1]
    filters.assessmentDate = dayjs().format('YYYY-MM-DD')
  }
  watch([region, () => filters.assessmentDate], search, { immediate: true })
  // 连续勾选风险等级时合并请求，避免每次选择都重新加载全量数据。
  const debouncedRiskSearch = debounce(search, 1500)
  watch(() => [...filters.dynamicRiskLevels].sort((a, b) => a - b).join(','), debouncedRiskSearch)
  onScopeDispose(() => {
    debouncedRiskSearch.cancel()
    version += 1
    controller?.abort()
  })
  const dataset = {
    filters,
    region,
    rows,
    slopeItems,
    initialLoading,
    loadError,
    activeSlopeUnitId,
    activeSlopeKey,
    search,
    reset,
    setRegion,
    setActiveSlope: (slope) => {
      activeSlopeUnitId.value = slope?.slopeUnitId ?? ''
      activeSlopeKey.value = getRiskSlopeSelectionKey(slope)
    },
    handleScroll: () => {},
    listStatus: computed(() => (slopeItems.value.length ? `共 ${slopeItems.value.length} 个斜坡单元` : '')),
  }
  provide(RISK_DATASET_KEY, dataset)
  return dataset
}
