const pad = (n) => `${n}`.padStart(2, '0')
const now = new Date()

/** 统计日期（默认当天） */
export const monitorDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`

/** 概况统计的默认行政区划：彭水苗族土家族自治县。
 *  概况面板先于地图政区选择器就绪取数，此时用该编码兜底，保证统计区域与地图展示的区划一致 */
export const DEFAULT_REGION_CODE = '500243'

/** 专业监测告警列表 - 等级配色（三方接口 C1 蓝色 / C2 黄色 / C3 橙色 / C4 红色） */
export const alarmLevelMap = {
  红色告警: { color: '#DD4739', background: '#FED8D8' },
  橙色告警: { color: '#FF922C', background: '#FEE6D8' },
  黄色告警: { color: '#FFB84E', background: '#FEEFD8' },
  蓝色告警: { color: '#007BFF', background: '#DCEDFF' },
  无告警: { color: '#007bff', background: '#eaf2fe' },
}

/** 专业监测告警列表 - 筛选选项（区划选项来自行政区划树，见 useMonitorData） */
export const alarmFilterOptions = {
  levels: ['红色告警', '橙色告警', '黄色告警', '蓝色告警', '无告警'],
  /* 与卡片「有效告警」胶囊取值保持一致 */
  validities: ['有效', '无效'],
}

/** 监测预警智能体 - 面板数据（Mock，接口就绪后替换为 @/api 返回值） */
export const warningAgentDetail = {
  name: '土地堂组斜坡',
  level: '红色告警',
  deviceName: '土地堂组斜坡_GNSS02',
  period: '09-07 10:00-09-08 10:00',
  summary: '已读取该设备近24小时监测数据，结合曲线变化与已有处置记录整理监测数据档案。',
  /** steps 中的 points 为说明文字数组，每项一行 */
  steps: [
    {
      key: 'data',
      title: '已读取监测数据与预警记录',
      points: [
        '已读取该设备近24小时监测数据与监测详情',
        '监测详情包含设备信息、预警记录与处置记录',
      ],
    },
    {
      key: 'curve',
      title: '已完成监测曲线描述',
      subtitle: '累计位移变化：',
      chart: true,
      points: [
        '曲线整体波动较小，末段出现一次短时升高，随后回落至此前水平；当前时段未见连续上升趋势。',
      ],
    },
    {
      key: 'summary',
      title: '已生成分析摘要',
      tip: '本阶段以平稳波动为主，存在单次累计位移，建议结合后现场记录与设备情况进一步核查。',
    },
  ],
  /** 累计位移曲线（近24小时，每 2 小时一个点） */
  chart: {
    times: ['10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00', '00:00', '02:00', '04:00', '06:00', '08:00', '10:00'],
    values: [3.5, 3.6, 4.8, 10.8, 11.4, 11.2, 10.3, 9.4, 8.2, 7.4, 7.6, 7.8, 11.5],
  },
  footer: {
    /** 已有处置记录：label 深色，value 用主色 */
    record: {
      label: '已有处置记录：',
      value: '数据异常导致误报。',
    },
    note: '以上为监测数据描述，预警有效性以人工核查结果为准。',
  },
}

/** 分析时段窗口长度：预警列表只给预警时刻，面板展示与智能体取数统一按预警时刻往前推 24 小时 */
const ANALYSIS_WINDOW = 24 * 60 * 60 * 1000

/**
 * 解析分析时段：把「预警时刻」还原成面板展示的起止文案与智能体接口需要的毫秒时间戳。
 * 两处共用同一份计算，保证面板上的分析时段与接口实际分析的区间一致
 * @param {string} warningTime 预警时刻
 * @returns {{ startTime: number, endTime: number, period: string } | null} 时间无效时返回 null
 */
const resolveAnalysisWindow = (warningTime) => {
  const end = new Date(String(warningTime || '').replace(/-/g, '/'))
  if (Number.isNaN(end.getTime())) {
    return null
  }
  const start = new Date(end.getTime() - ANALYSIS_WINDOW)
  const stamp = (date) => `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  return {
    startTime: start.getTime(),
    endTime: end.getTime(),
    period: `${stamp(start)}-${stamp(end)}`,
  }
}

/**
 * 预警列表行 → 监测预警智能体面板数据
 * 文案按列表行返回的真实字段拼接；列表未返回的字段显示「无数据」，不编造分析结论；
 * 曲线描述与分析摘要由 analyzeMonitorWarning 返回后经 applyWarningAnalysis 回填，未返回前留空
 * @param {object} row 预警列表项：{ id, name, level, monitorPointNum, detail: { deviceName, address, publishTime, disposeType, processStatus, processResult } }
 */
export const buildWarningAgentDetail = (row = {}) => {
  const detail = row.detail || {}
  const name = row.name || warningAgentDetail.name
  const level = row.level || warningAgentDetail.level
  const deviceName = detail.deviceName || '无数据'
  const pointNum = row.monitorPointNum || '无数据'
  const warningTime = detail.publishTime || ''
  const processStatus = detail.disposeType || detail.processStatus || '无数据'
  const processResult = detail.processResult || ''
  const window = resolveAnalysisWindow(warningTime)

  return {
    ...warningAgentDetail,
    /* 智能体接口主键：列表未返回时留空，调用方据此判断不发请求 */
    warningId: row.id ? String(row.id) : '',
    /* 智能体取数区间：与面板展示的分析时段同源 */
    analysisParams: window ? { startTime: window.startTime, endTime: window.endTime } : null,
    name,
    level,
    deviceName,
    period: window ? window.period : '无数据',
    summary: `已读取「${name}」近24小时监测数据，结合曲线变化与已有处置记录整理监测数据档案。`,
    steps: [
      {
        key: 'data',
        title: '已读取监测数据与预警记录',
        points: [
          `监测点：${name}（${pointNum}）`,
          `预警时间：${warningTime || '无数据'}`,
          '监测详情包含设备信息、预警记录与处置记录',
        ],
      },
      {
        key: 'curve',
        title: '已完成监测曲线描述',
        subtitle: '累计位移变化：',
        chart: true,
        /* 由接口 description 填充 */
        points: [],
      },
      {
        key: 'summary',
        title: '已生成分析摘要',
        /* 由接口 summary 填充 */
        tip: '',
      },
    ],
    footer: {
      record: { label: '已有处置记录：', value: processResult || processStatus },
      note: warningAgentDetail.footer.note,
    },
  }
}

/** 接口返回的长文本 → 面板说明列表：按换行拆行，无换行时整体作为一条 */
const toPointList = (text) =>
  String(text ?? '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

/**
 * 智能体接口返回 → 面板内容：description 落到「监测曲线描述」，summary 落到「分析摘要」。
 * 就地写入 detail 的对应字段而不替换对象引用：父级 watch 监听的是引用变化，
 * 整体替换会触发产出动画重放，导致已展示的内容闪回
 * @param {object} detail buildWarningAgentDetail 产出的面板数据
 * @param {{ description?: string, summary?: string }} [data] 接口返回的 data
 */
export const applyWarningAnalysis = (detail, data = {}) => {
  const curveStep = detail?.steps?.find((step) => step.key === 'curve')
  const summaryStep = detail?.steps?.find((step) => step.key === 'summary')
  if (curveStep) {
    curveStep.points = toPointList(data?.description)
  }
  if (summaryStep) {
    summaryStep.tip = data?.summary ?? ''
  }
}

/**
 * 位移 / 降雨指标块：按设计稿固定三格 × 两块，字段名对应设备接口
 * /dizai/warningData/getDeviceRainfallAndDisplacement（依据 docs/根据设备ID获取雨量与位移统计.md）。
 * 该接口要设备 ID，列表行目前没有，故先只固定字段、取值给 '--' 占位，块本身常显；
 * 接入时把接口返回体直接传给 buildMonitorPointMetrics 即可，模板无需改动。
 */
const MONITOR_METRIC_FIELDS = {
  displacement: [
    { key: 'd1', label: '近1天位移', field: 'last1DayDisplacement', unit: 'mm' },
    { key: 'd3', label: '近3天位移', field: 'last3DaysDisplacement', unit: 'mm' },
    { key: 'd7', label: '近7天位移', field: 'last7DaysDisplacement', unit: 'mm' },
  ],
  rainfall: [
    { key: 'h1', label: '近1小时降雨', caption: '雨量', field: 'last1HourRainfall', unit: 'mm' },
    { key: 'h24', label: '近24小时降雨', caption: '雨量', field: 'last24HoursRainfall', unit: 'mm' },
    { key: 'd7', label: '近7天降雨', caption: '雨量', field: 'last7DaysRainfall', unit: 'mm' },
  ],
}

/**
 * 由设备接口返回体拼装指标块；缺值时只保留 '--'
 * @param {object} [data] 接口返回的雨量与位移统计，缺省即全部占位
 * @returns {{ displacement: Array, rainfall: Array }}
 */
export const buildMonitorPointMetrics = (data = {}) => {
  const toCells = (fields) =>
    fields.map(({ field, label, key, unit, caption }) => {
      const value = data?.[field]
      const empty = value === null || value === undefined || value === ''
      if (empty) {
        /* 无数据时不带单位与「雨量」前缀，避免出现「--mm」「雨量--」这类残缺文案 */
        return { key, label, value: '--', unit: '' }
      }
      return caption ? { key, label, caption, value, unit } : { key, label, value, unit }
    })

  return {
    displacement: toCells(MONITOR_METRIC_FIELDS.displacement),
    rainfall: toCells(MONITOR_METRIC_FIELDS.rainfall),
  }
}

/** 设备在线状态项：在线的圆点与文字走主色绿色（见 MonitorPointPopup 的 .status-item.online） */
const ONLINE_DEVICE_STATUS = [{ key: 'online', label: '在线', tone: 'online' }]
/** 离线状态项：字段缺失或非 1 时使用（弹窗也用它兜底，保证状态行常显） */
export const OFFLINE_DEVICE_STATUS = [{ key: 'offline', label: '离线' }]

/**
 * 设备状态：字段与雨量与位移接口同源（deviceStatus，1=在线，0=离线）。
 * 该字段接口尚未部署，取不到值（undefined/空）时一律按离线展示
 * @param {number|string} [deviceStatus] 1=在线，0=离线
 * @returns {Array} 状态项列表
 */
export const buildDeviceStatus = (deviceStatus) =>
  Number(deviceStatus) === 1 ? ONLINE_DEVICE_STATUS : OFFLINE_DEVICE_STATUS

/** 监测点详情初始结构：指标块按设计稿固定字段常显，其余字段随选中项填充 */
export const monitorPointDetail = {
  name: '',
  code: '',
  ...buildMonitorPointMetrics(),
  info: [],
  /* 接口返回前的默认状态：离线（字段未部署时也保持离线） */
  deviceStatus: buildDeviceStatus(),
}
