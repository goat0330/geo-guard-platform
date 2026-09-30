import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'

import { getRiskAssessmentPage, resolveRiskAssessmentPage } from '@/api/riskEvaluation.js'
import { formatRiskSlope } from '@/utils/riskEvaluation.js'

const PAGE_SIZE = 20

const createFilters = () => ({
  slopeUnitId: '',
  dynamicRiskLevel: '',
  assessmentDate: dayjs().format('YYYY-MM-DD'),
})

/**
 * 斜坡单元列表数据管理：负责筛选、分页追加、请求竞态处理和卸载保护。
 */
export const useRiskSlopeList = () => {
  const filters = reactive(createFilters())
  const slopeItems = ref([])
  const total = ref(0)
  const initialLoading = ref(false)
  const loadingMore = ref(false)
  const finished = ref(false)
  const activeSlopeUnitId = ref('')
  let nextPage = 1
  let requestVersion = 0
  let isUnmounted = false

  const listStatus = computed(() => {
    if (loadingMore.value) return '加载中...'
    if (slopeItems.value.length && finished.value) return '没有更多数据了'
    return ''
  })

  const buildParams = (pageNum) => {
    const params = { pageNum, pageSize: PAGE_SIZE }

    if (filters.slopeUnitId) params.slopeUnitId = filters.slopeUnitId
    if (filters.assessmentDate) params.assessmentDate = filters.assessmentDate
    if (filters.dynamicRiskLevel !== '' && filters.dynamicRiskLevel !== null) {
      params.dynamicRiskLevel = filters.dynamicRiskLevel
    }

    return params
  }

  const appendRows = (rows) => {
    const formattedRows = rows.map((row, index) => {
      const item = formatRiskSlope(row)
      return {
        ...item,
        // 虚拟列表要求唯一且稳定的键，接口主键优先，斜坡单元编号作为兼容。
        virtualId: String(item.id || item.slopeUnitId || `${nextPage}-${index}`),
      }
    })
    const merged = [...slopeItems.value, ...formattedRows]
    slopeItems.value = Array.from(new Map(merged.map((item) => [item.virtualId, item])).values())
  }

  const loadPage = async (version, pageNum, reset) => {
    if (reset) initialLoading.value = true
    else loadingMore.value = true

    try {
      const response = await getRiskAssessmentPage(buildParams(pageNum))
      const { rows, total: count } = resolveRiskAssessmentPage(response)
      if (isUnmounted || version !== requestVersion) return

      total.value = count
      appendRows(rows)
      nextPage = pageNum + 1
      finished.value = rows.length < PAGE_SIZE || (count > 0 && slopeItems.value.length >= count)
    } catch {
      if (isUnmounted || version !== requestVersion) return
      if (reset) {
        slopeItems.value = []
        total.value = 0
      }
    } finally {
      if (!isUnmounted && version === requestVersion) {
        initialLoading.value = false
        loadingMore.value = false
      }
    }
  }

  const search = () => {
    requestVersion += 1
    nextPage = 1
    slopeItems.value = []
    total.value = 0
    finished.value = false
    activeSlopeUnitId.value = ''
    loadPage(requestVersion, nextPage, true)
  }

  const reset = () => {
    Object.assign(filters, createFilters())
    search()
  }

  const loadMore = () => {
    if (initialLoading.value || loadingMore.value || finished.value) return
    loadPage(requestVersion, nextPage, false)
  }

  const handleScroll = (event) => {
    const target = event.target
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 80) loadMore()
  }

  const setActiveSlope = (slope) => {
    activeSlopeUnitId.value = slope.slopeUnitId
  }

  onMounted(search)

  onUnmounted(() => {
    isUnmounted = true
    requestVersion += 1
  })

  return {
    activeSlopeUnitId,
    filters,
    handleScroll,
    initialLoading,
    listStatus,
    loadingMore,
    reset,
    search,
    setActiveSlope,
    slopeItems,
    total,
  }
}
