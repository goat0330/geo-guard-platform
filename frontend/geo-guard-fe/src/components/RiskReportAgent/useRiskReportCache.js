const cache = {
  key: '',
  unitCount: null,
  riskUnits: null,
  mapImages: {},
}

/**
 * 获取当日风险分析共享缓存，日期或行政区变化时自动清空旧结果。
 *
 * @param {string} key 缓存范围标识，格式为“行政区:日期”。
 * @returns {{ unitCount: number|null, riskUnits: object[]|null, mapImages: Record<string, string> }} 风险分析缓存。
 */
export const getRiskReportCache = (key) => {
  if (cache.key !== key) {
    cache.key = key
    cache.unitCount = null
    cache.riskUnits = null
    cache.mapImages = {}
  }

  return cache
}
