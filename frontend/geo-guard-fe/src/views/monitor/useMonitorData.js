import { reactive } from 'vue'
import { getAreaListWithOutWk } from '@/api/common.js'
import { getMonitorOverview, getMonitorWarningPage } from '@/api/monitor.js'
import { buildRangeParams, isAllRange } from '@/utils/dateRange.js'
import { DEFAULT_REGION_CODE } from './config.js'
import deviceTotalIcon from '@/assets/imgs/monitor/icon-device-total.webp'
import onlineRateIcon from '@/assets/imgs/monitor/icon-online-rate.webp'

/**
 * 专业监测页面共享数据源（模块级单例）：
 * 概况面板 / 预警列表面板 / 地图弹窗共用同一份状态，接口返回后在此统一装配
 */

/** 三方预警等级 C1-C4 → 页面等级文案 */
const WARNING_LEVEL_TEXT = { C1: '蓝色', C2: '黄色', C3: '橙色', C4: '红色' }

/** 等级文案里出现的色值（从低到高），用于兜底识别中文等级 */
const LEVEL_COLORS = ['红色', '橙色', '黄色', '蓝色']

/** 设备类型 → 环形图配色：优先按后端 code 命中，其次按名称，未识别统一使用默认灰蓝 */
const DEVICE_TYPE_COLOR = {
  GNSS: '#81A4F7',
  CRACK_METER: '#B4DCC0',
  RAIN_GAUGE: '#B0D8EA',
  INCLINOMETER: '#F7AE81',
  DISPLACEMENT: '#B8C6E3',
  OTHER: '#F7D777',
  裂缝计: '#B4DCC0',
  雨量计: '#B0D8EA',
  倾角计: '#F7AE81',
  位移计: '#B8C6E3',
  其他: '#F7D777',
}

/** 三方预警等级 → 页面文案：兼容 C1-C4、1-4 以及直接返回中文色值的写法 */
const toWarningLevelText = (level) => {
  const text = String(level ?? '').trim()
  if (!text) {
    return '无'
  }
  const key = text.toUpperCase()
  if (WARNING_LEVEL_TEXT[key]) {
    return WARNING_LEVEL_TEXT[key]
  }
  /* 直接返回「红色预警」这类中文时按色值命中 */
  const color = LEVEL_COLORS.find((item) => text.includes(item))
  if (color) {
    return color
  }
  return WARNING_LEVEL_TEXT[`C${Number(key.replace(/\D/g, ''))}`] ?? '无'
}

/** 数字千分位展示 */
const formatNumber = (value) => Number(value ?? 0).toLocaleString()

export const monitorData = reactive({
  // 设备概况
  deviceOverview: [
    { key: 'total', label: '设备总数', value: 0, icon: deviceTotalIcon },
    { key: 'rate', label: '设备在线率', value: '0.0%', icon: onlineRateIcon },
  ],
  deviceStatuses: [
    { key: 'online', label: '在线数', value: 0, color: '#007BFF' },
    { key: 'offline', label: '离线数', value: 0, color: '#9096A2' },
  ],
  // 仪器告警情况
  alarmSituation: {
    total: 0,
    unit: '条',
    totalIcon: deviceTotalIcon,
    levels: [
      { key: 'red', label: '红色', value: 0, color: '#E18282' },
      { key: 'orange', label: '橙色', value: 0, color: '#F7AE81' },
      { key: 'yellow', label: '黄色', value: 0, color: '#FFB84E' },
      { key: 'blue', label: '蓝色', value: 0, color: '#56A5FE' },
    ],
  },
  // 处置状态：已处置 / 未处置，rate 为处置率（已处置占比）
  disposeStatus: {
    rate: 0,
    total: '0',
    gaugeColor: '#699EF5',
    legend: [
      { key: 'done', label: '已处置', value: '0条', percent: '0%', color: '#81A4F7' },
      { key: 'todo', label: '未处置', value: '0条', percent: '0%', color: '#EBEDEF' },
    ],
  },
  // 设备类型分布
  deviceTypes: [],
  // 预警信息（取最新 3 条）
  warningMessages: [],
  // 预警列表
  alarmList: [],
  alarmTotal: 0,
  // 预警列表的行政区划筛选项（行政区划树根节点的下级区划）
  regionOptions: [],
  // 概况统计的区划编码：来源是地图上的行政区划选择器（AreaSearch），
  // 未选择时保持为空，取数时由 DEFAULT_REGION_CODE（彭水 500243）兜底
  regionCode: '',
  // 当前统计时段（概况统计入参，空数组表示全部）
  statRange: [],
})

/** 行政区划树：取根节点下级区划作为预警列表的区划筛选项 */
export async function fetchRegionTree() {
  const res = await getAreaListWithOutWk()
  const list = Array.isArray(res) ? res : res?.data ?? []
  if (!list.length) {
    return
  }
  const top = list[0]
  monitorData.regionOptions = (top?.children ?? []).map((c) => ({ id: c?.id, name: c?.name }))
}

/**
 * 专业监测概览：一次请求装配设备概况 / 设备类型 / 告警情况 / 处置状态 / 最新告警
 * @param {Array} [range] 统计时段 [开始日期, 结束日期]，空数组表示全部（不传日期）
 */
export async function fetchOverview(range = monitorData.statRange) {
  const selected = Array.isArray(range) ? range : []
  /* 区划编码来自地图上的行政区划选择器；选择器就绪前为空，此时用彭水（500243）兜底，
     避免不带区划取数导致统计区域与地图展示的区划不一致 */
  const params = {
    regionCode: monitorData.regionCode || DEFAULT_REGION_CODE,
    ...buildRangeParams(selected),
  }
  const res = await getMonitorOverview(params)
  const overview = res ?? {}

  /* 选中的时段以面板持有为准（接口不回显区间）；「全部」时参数里不带日期字段 */
  monitorData.statRange = selected

  // 设备概况：总数 / 在线数 / 离线数 / 在线率
  const device = overview.deviceOverview ?? {}
  const deviceTotal = Number(device.total ?? 0)
  const online = Number(device.online ?? 0)
  // 在线率可能为 0-1 小数或 0-100 百分数，统一换算为百分数
  let onlineRate = Number(device.onlineRate ?? 0)
  if (onlineRate > 0 && onlineRate <= 1) {
    onlineRate *= 100
  }
  monitorData.deviceOverview[0].value = formatNumber(deviceTotal)
  monitorData.deviceOverview[1].value = `${onlineRate.toFixed(1)}%`
  monitorData.deviceStatuses[0].value = online
  monitorData.deviceStatuses[1].value = Number(device.offline ?? Math.max(deviceTotal - online, 0))

  // 设备类型分布：后端返回 code / name / count
  monitorData.deviceTypes = (overview.deviceTypes ?? []).map((item) => ({
    key: item?.code || item?.name || 'OTHER',
    label: item?.name || '其他',
    value: formatNumber(item?.count ?? 0),
    color: DEVICE_TYPE_COLOR[item?.code] || DEVICE_TYPE_COLOR[item?.name] || '#B8C6E3',
  }))

  // 仪器告警情况：按 RED / ORANGE / YELLOW / BLUE 回写各级数量
  const warning = overview.warningOverview ?? {}
  const levels = new Map(
    (warning.levels ?? []).map((item) => [String(item?.code ?? '').toUpperCase(), item]),
  )
  monitorData.alarmSituation.levels.forEach((level) => {
    const matched = levels.get(level.key.toUpperCase())
    level.label = matched?.name || level.label
    level.value = formatNumber(matched?.count ?? 0)
  })
  monitorData.alarmSituation.total = formatNumber(warning.total ?? 0)

  // 处置状态：处置率取已处置占比
  const disposal = overview.disposal ?? {}
  const disposalTotal = Number(disposal.total ?? 0)
  const toLegendItem = (key, label, item, color) => ({
    key,
    label,
    value: `${formatNumber(item?.count ?? 0)}条`,
    percent: `${Number(item?.rate ?? 0).toFixed(1)}%`,
    color,
  })
  monitorData.disposeStatus.rate = Math.round(Number(disposal.completed?.rate ?? 0))
  monitorData.disposeStatus.legend = [
    toLegendItem('done', '已处置', disposal.completed, '#81A4F7'),
    toLegendItem('todo', '未处置', disposal.unprocessed, '#EBEDEF'),
  ]
  monitorData.disposeStatus.total = formatNumber(disposalTotal)

  // 预警信息：接口直接返回最新预警文案所需字段
  monitorData.warningMessages = (overview.latestWarnings ?? []).map((item) => {
    const levelName = item?.levelName || toWarningLevelText(item?.levelCode)
    return {
      id: item?.warningId,
      level: levelName,
      content: `【${item?.location ?? ''}】于${item?.warningTime ?? ''}产生${levelName}告警。`,
    }
  })
}

/** 日期格式化为 YYYY-MM-DD */
const formatDay = (date) => {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 预警列表每页条数：列表滚动到底部时按该页大小继续加载 */
export const ALARM_PAGE_SIZE = 20

/**
 * 预警列表：对应时间段的监测设备告警，按区间查询（接口 startTime / endTime）
 * 字段映射依据 docs/获取对应时间段的已发布的监测设备预警数据列表.md；
 * 该接口未返回的字段留空，由展示层显示「无数据」，不从前一个接口的字段名猜值
 * @param {object} options
 * @param {Array} [options.range] 查询区间 [开始日期, 结束日期] YYYY-MM-DD，空数组时回落到当月至今
 * @param {string} [options.state] 告警数据状态，由查询条件传入；为空时不下发该参数
 * @param {number} [options.pageNum] 页码，从 1 开始；大于 1 时结果追加到列表尾部
 * @param {number} [options.pageSize] 每页条数
 * @returns {Promise<number>} 本次返回的原始条数（调用方据此判断是否还有下一页）
 */
export async function fetchAlarmList({ range = [], state = '', pageNum = 1, pageSize = ALARM_PAGE_SIZE } = {}) {
  /* 区划筛选项取自行政区划树，首次进入列表时补齐（预警列表接口本身不收区划参数） */
  if (!monitorData.regionOptions.length) {
    await fetchRegionTree()
  }
  const today = new Date()
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1)
  /* 未选区间时沿用「当月至今」，避免默认查全量把列表撑满 */
  const [startTime, endTime] = isAllRange(range) ? [formatDay(monthStart), formatDay(today)] : range
  const params = {
    startTime,
    endTime,
    pageNum,
    pageSize,
  }
  /* 状态默认不传：只有查询条件里选中后才带上，避免写死口径 */
  if (state) {
    params.state = state
  }
  /* 分页接口返回体为 { total, data, code, msg }（total 与 data 同级），故取完整 body */
  const body = await getMonitorWarningPage(params)
  const rows = Array.isArray(body?.data) ? body.data : []
  if (!Array.isArray(body?.data)) {
    /* 返回体结构与预期不符（total/data 同级）时给出线索，避免列表静默为空 */
    console.warn('[专业监测] 预警列表返回体结构异常:', body)
  }
  monitorData.alarmTotal = Number(body?.total ?? 0)

  const list = rows.map((item) => ({
    /* 预警主键：卡片 key 与展开态标识 */
    id: item?.warningPrimaryKey || item?.id,
    /* 卡片标题：监测点名称 */
    name: item?.monitorPointName || '',
    /* 告警等级：接口返回 C1-C4（可能带中文后缀），统一转成「红色告警」这类文案 */
    level: `${toWarningLevelText(item?.warningLevel)}告警`,
    /* 地图定位与智能体取数用（选中卡片时会用到） */
    lon: item?.lon,
    lat: item?.lat,
    monitorPointId: item?.monitorPointId,
    monitorPointNum: item?.monitorPointNum,
    sensorId: item?.sensorId,
    warningBullentinPath: item?.warningBullentinPath,
    detail: {
      /* 详细地址 */
      address: item?.address,
      /* 发布时间：预警时间 */
      publishTime: item?.warningTime,
      /* 处置类型 / 地图弹窗处置状态：预警处理状态 */
      disposeType: item?.warningProcessStatus,
      processStatus: item?.warningProcessStatus,
      /* 预警处理结果：留给监测预警智能体面板的「已有处置记录」 */
      processResult: item?.warningProcessResult,
      /* 设备名称 / 处置人 / 处置时间 / 有效预警：本接口未返回，卡片按「无数据」展示 */
    },
  }))
  /* 首页替换、后续页追加，滚动加载时列表不被清空 */
  monitorData.alarmList = pageNum > 1 ? [...monitorData.alarmList, ...list] : list
  return rows.length
}

/**
 * 概况面板数据：按统计时段 + 地图上选择的区划编码拉取概览统计
 * @param {Array} [range] 统计时段 [开始日期, 结束日期]，空数组表示全部
 */
export async function fetchMonitorOverview(range) {
  return Promise.allSettled([fetchOverview(range)])
}
