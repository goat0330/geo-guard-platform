import { onMounted, ref } from 'vue'
import { getHomeDashboard } from '@/api/common.js'

/**
 * 灾害类型英文枚举到中文标签的统一映射表
 */
export const HAZARD_TYPE_LABEL_MAP = {
  LANDSLIDE: '滑坡',
  COLLAPSE: '崩塌',
  UNSTABLE_SLOPE: '不稳定斜坡',
  SUBSIDENCE: '地面塌陷',
  DEBRIS_FLOW: '泥石流',
}

/**
 * 将后端返回的灾害类型英文枚举格式化为展示中文
 * @param {string} val 原始类型标识
 * @returns {string} 中文标签
 */
export const formatHazardType = (val) => {
  if (!val) return ''
  return HAZARD_TYPE_LABEL_MAP[val] || val
}

/**
 * 安全解析数值：若为 null/undefined/无法解析为有效数字，返回 null
 * @param {*} val 待解析值
 * @returns {number|null}
 */
export const toFiniteNumber = (val) => {
  if (val === null || val === undefined || val === '') return null
  const num = Number(val)
  return Number.isFinite(num) ? num : null
}

/**
 * 首页聚合业务看板 Composable
 */
export const useHomeDashboard = () => {
  const loading = ref(false)
  const cumulative = ref({})
  const today = ref({})
  const riskTrend = ref(null)

  const fetchDashboard = async () => {
    loading.value = true
    try {
      const res = await getHomeDashboard()
      // http.get 默认已在成功时解构返回 res.data
      const data = res?.data || res || {}
      cumulative.value = data?.cumulative || {}
      today.value = data?.today || {}
      riskTrend.value = data?.riskTrend ?? null
    } catch (err) {
      console.error('获取首页业务看板数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    fetchDashboard()
  })

  return {
    cumulative,
    fetchDashboard,
    loading,
    riskTrend,
    today,
  }
}
