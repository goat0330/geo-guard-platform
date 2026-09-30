/**
 * 日期区间（概览类面板的统计时段）通用工具。
 * 约定：区间为空表示「全部」，即不限时段，取数时不向后端传日期字段。
 */

/** 空区间（全部）时的展示文案 */
export const ALL_RANGE_TEXT = '全部'

/**
 * 是否为空区间：非「[开始, 结束] 且两端都有值」一律视为全部，
 * 兼容 null / undefined / [] / ['', ''] 等接口或组件回传的中间态
 * @param {Array} range 日期区间
 * @returns {boolean}
 */
export const isAllRange = (range) =>
  !(Array.isArray(range) && range.length === 2 && range[0] && range[1])

/**
 * 区间 → 展示文案：全部 /「开始 至 结束」
 * @param {Array} range 日期区间
 * @returns {string}
 */
export const formatRangeText = (range) =>
  isAllRange(range) ? ALL_RANGE_TEXT : `${range[0]} 至 ${range[1]}`

/**
 * 区间 → 接口参数：全部时返回空对象（不带日期字段），有区间时按字段名展开。
 * 字段名集中在这里，后端若用其它命名只需改这一处
 * @param {Array} range 日期区间
 * @param {{ startKey?: string, endKey?: string }} [options] 接口字段名
 * @returns {object}
 */
export const buildRangeParams = (range, options = {}) => {
  const { startKey = 'startDate', endKey = 'endDate' } = options
  return isAllRange(range) ? {} : { [startKey]: range[0], [endKey]: range[1] }
}
