import { PRED_RISK_LEVEL } from '@/utils/enum.js'


// 风险助手target
export const targetSelector = '#chat-container'

// layout header
export const layoutHeader = '#left-content-header'

// layout-center-left
export const layoutCenterLeft = '#content-layout-left'

// layout-center-right
export const layoutCenterRight = '#content-layout-right'

// layout foot
export const layoutFoot = '#left-content-foot'

// chat-box-btn
export const chatBoxBtn = '#chat-left-btn'

export const RISK_WEEK_TYPE = {
  STATIC_RISK: 'static-risk',
  HIDDEN_RISK: 'static-hidden-risk',
}

export const riskWeekOptions = [
  {
    label: '本周风险情况统计',
    value: RISK_WEEK_TYPE.STATIC_RISK,
  },
  {
    label: '本周隐患情况统计',
    value: RISK_WEEK_TYPE.HIDDEN_RISK,
  },
]

export const TAB_MAP = {
  RISK: 'dynamic-risk',
  SOURCE: 'source-emit',
  TASK_SEND: 'task-send',
  DEEL_RISK: 'deel-risk',
  DISASTER_REPORT: 'disaster-report',
}

export const tabOptions = [
  {
    label: '动态风险',
    value: TAB_MAP.RISK,
  },
  {
    label: '监测预警',
    value: TAB_MAP.SOURCE,
  },
  {
    label: '任务派发',
    value: TAB_MAP.TASK_SEND,
  },
  {
    label: '应急处置',
    value: TAB_MAP.DEEL_RISK,
  },
  {
    label: '信息报送',
    value: TAB_MAP.DISASTER_REPORT,
  },
]

export const DATE_RANGE_MAP = {
  TODAY: '0',
  WEEK: '7',
  MONTH: '30',
  YEAR: '365',
}

export const dateRangeOptions = [
  {
    label: '今日',
    value: DATE_RANGE_MAP.TODAY,
  },
  {
    label: '近七天',
    value: DATE_RANGE_MAP.WEEK,
  },
  {
    label: '近一月',
    value: DATE_RANGE_MAP.MONTH,
  },
  {
    label: '近一年',
    value: DATE_RANGE_MAP.YEAR,
  },
]

export const LEVEL_MAP = {
  SUPER_HIGH: 4,
  HIGH: 3,
  MIDDLE: 2,
  LOW: 1,
  VERY_LOW: -1,
  NO: 0,
}
export const LEVEL_TEXT = {
  [LEVEL_MAP.SUPER_HIGH]: '极高风险',
  [LEVEL_MAP.HIGH]: '高风险',
  [LEVEL_MAP.MIDDLE]: '中风险',
  [LEVEL_MAP.LOW]: '低风险',
}
export const LEVEL_COLOR = {
  [LEVEL_MAP.SUPER_HIGH]: '#E23030',
  [LEVEL_MAP.HIGH]: '#FA8043',
  [LEVEL_MAP.MIDDLE]: '#EDBA5D',
  [LEVEL_MAP.LOW]: '#699EF5',
}

export const warnLevelOptions = [
  {
    key: 'superHigh',
    label: '极高风险',
    value: LEVEL_MAP.SUPER_HIGH,
    color: '#E23030',
    icon: new URL('@/assets/imgs/super-high.png', import.meta.url).href,
  },
  {
    key: 'high',
    label: '高风险',
    value: LEVEL_MAP.HIGH,
    color: '#FA8043',
    icon: new URL('@/assets/imgs/high.png', import.meta.url).href,
  },
  {
    key: 'middle',
    label: '中风险',
    value: LEVEL_MAP.MIDDLE,
    color: '#EDBA5D',
    icon: new URL('@/assets/imgs/middle.png', import.meta.url).href,
  },
  {
    key: 'low',
    label: '低风险',
    value: LEVEL_MAP.LOW,
    color: '#699EF5',
    icon: new URL('@/assets/imgs/low.png', import.meta.url).href,
  },
]

// 任务状态枚举
export const TASK_SEND_TYPE_MAP = {
  NOT_SEND: 1,
  NOT_CHECK: 2,
  CHECKING: 3,
  CLOSE: 4,
  HAS_FEEDBACK: 5,
}
export const TASK_STATUS_TEXT_ENUM = {
  [TASK_SEND_TYPE_MAP.NOT_SEND]: '未推送',
  [TASK_SEND_TYPE_MAP.NOT_CHECK]: '未核查',
  [TASK_SEND_TYPE_MAP.CHECKING]: '核查中',
  [TASK_SEND_TYPE_MAP.CLOSE]: '已关闭',
  [TASK_SEND_TYPE_MAP.HAS_FEEDBACK]: '已反馈',
}
export const taskSendTypeOptions = [
  {
    label: '未推送',
    value: TASK_SEND_TYPE_MAP.NOT_SEND,
    color: '#F5D98F',
  },
  {
    label: '未核查',
    value: TASK_SEND_TYPE_MAP.NOT_CHECK,
    color: '#B5DBFF',
  },
  {
    label: '核查中',
    value: TASK_SEND_TYPE_MAP.CHECKING,
    color: '#699EF5',
  },
  {
    label: '已反馈',
    value: TASK_SEND_TYPE_MAP.HAS_FEEDBACK,
    color: '#3371d9',
  },
  {
    label: '已关闭',
    value: TASK_SEND_TYPE_MAP.CLOSE,
    color: '#0052D9',
  },
]

export const RISK_SCALE_MAP = {
  SUPER_HIGH: 4,
  HIGH: 3,
  MIDDLE: 2,
  SMALL: 1,
}

export const riskScaleOptions = [
  {
    label: '特大型',
    value: RISK_SCALE_MAP.SUPER_HIGH,
  },
  {
    label: '大型',
    value: RISK_SCALE_MAP.HIGH,
  },
  {
    label: '中型',
    value: RISK_SCALE_MAP.MIDDLE,
  },
  {
    label: '小型',
    value: RISK_SCALE_MAP.SMALL,
  },
]

export const RISK_DEAL_STATUS_MAP = {
  TALK: 4,
  CHECK: 3,
  DEAL: 2,
  RUN: 1,
}

export const riskDealStatusOptions = [
  {
    label: '核实中',
    value: RISK_DEAL_STATUS_MAP.CHECK,
  },
  {
    label: '会商中',
    value: RISK_DEAL_STATUS_MAP.TALK,
  },
  {
    label: '处置中',
    value: RISK_DEAL_STATUS_MAP.DEAL,
  },
  {
    label: '撤离中',
    value: RISK_DEAL_STATUS_MAP.RUN,
  },
]


export const TASK_DEAL_TYPE_MAP = {
  NOT_DEAL: 0,
  ONE: 1,
  TWO: 2,
  THREE: 3,
  FOUR: 4,
}

export const taskDealTypeOptions = [
  {
    label: '未响应',
    value: TASK_DEAL_TYPE_MAP.NOT_DEAL,
    color: '#878898',
  },
  {
    label: 'I级响应',
    value: TASK_DEAL_TYPE_MAP.FOUR,
    color: '#E23030',
  },
  {
    label: 'II级响应',
    value: TASK_DEAL_TYPE_MAP.THREE,
    color: '#FF7C3A',
  },
  {
    label: 'III级响应',
    value: TASK_DEAL_TYPE_MAP.TWO,
    color: '#F9C568',
  },
  {
    label: 'IV级响应',
    value: TASK_DEAL_TYPE_MAP.ONE,
    color: '#8FB6F6',
  },
]

export const UNIT_RISK_SHOW = {
  [RISK_DEAL_STATUS_MAP.TALK]: {
    text: '红色 - 极高风险',
    color: '#E23030',
  },
  [RISK_DEAL_STATUS_MAP.CHECK]: {
    text: '橙色 - 高风险',
    color: '#FA8043',
  },
  [RISK_DEAL_STATUS_MAP.DEAL]: {
    text: '黄色 - 中风险',
    color: '#EDBA5D',
  },
  [RISK_DEAL_STATUS_MAP.RUN]: {
    text: '蓝色 - 低风险',
    color: '#699EF5',
  },
  [null]: {
    text: '低风险',
    color: '#222529',
  },
}

export const predWarnLevelOptions = [
  {
    level: [PRED_RISK_LEVEL.EXTREME_HIGH],
    key: 'superHigh',
    label: '极高风险',
    value: LEVEL_MAP.SUPER_HIGH,
    color: '#E23030',
    icon: new URL('@/assets/imgs/super-high.png', import.meta.url).href,
    bgColor: '#FFFAFA',
    borderColor: '#ffcccc80',
  },
  {
    level: [PRED_RISK_LEVEL.HIGH],
    key: 'high',
    label: '高风险',
    value: LEVEL_MAP.HIGH,
    color: '#FA8043',
    icon: new URL('@/assets/imgs/high.png', import.meta.url).href,
    bgColor: '#FFFCFA',
    borderColor: '#ffdfcc80',
  },
  {
    level: [PRED_RISK_LEVEL.MIDDLE],
    key: 'middle',
    label: '中风险',
    value: LEVEL_MAP.MIDDLE,
    color: '#EDBA5D',
    icon: new URL('@/assets/imgs/middle.png', import.meta.url).href,
  },
  {
    level: [PRED_RISK_LEVEL.LOW],
    key: 'low',
    label: '低风险',
    value: LEVEL_MAP.LOW,
    color: '#699EF5',
    icon: new URL('@/assets/imgs/low.png', import.meta.url).href,
  },
  // {
  //   level: [PRED_RISK_LEVEL.VERY_LOW],
  //   key: 'veryLow',
  //   label: '低风险',
  //   value: LEVEL_MAP.LOW,
  //   color: '#4AB28A',
  //   icon: new URL('@/assets/imgs/very-low.png', import.meta.url).href,
  // },
]

export const SINGLE_KEY = {
  // 地理位置
  LOCATION: 'LOCATION',
  // 监管情况
  SUPERVISION: 'SUPERVISION',
  // 历史灾害
  HISTORY: 'HISTORY',

  // 临近水系
  NEARBY_WATER_SYSTEM: 'NEARBY_WATER_SYSTEM',
  // 平均降雨
  AVERAGE_RAINFALL: 'AVERAGE_RAINFALL',
  // 斜坡形态
  SLOPE_SHAPE: 'SLOPE_SHAPE',
  // 斜坡高程
  SLOPE_ELEVATION: 'SLOPE_ELEVATION',
  // 斜坡坡向
  SLOPE_SLOPE: 'SLOPE_SLOPE',
  // 地质开挖
  GEOLOGICAL_EXCAVATION: 'GEOLOGICAL_EXCAVATION',

  // 灾害模拟演示
  SIMULATE_START: 'SIMULATE_START',
  // 降雨
  RAIN: 'RAIN',
  // 水位上涨
  FLOOD: 'FLOOD',
  // 滑坡模拟
  SLOPE_SLIDE: 'SLOPE_SLIDE',
  // 滑坡粒子分析
  PARTICLE_ANALYSIS: 'PARTICLE_ANALYSIS',
  // 滑坡AI分析
  AI_ANALYSIS: 'AI_ANALYSIS',

  // 承灾人口
  DISASTER_POPULATION: 'DISASTER_POPULATION',
  // 承灾房屋
  DISASTER_HOUSE: 'DISASTER_HOUSE',
  // 承灾基础设施1
  DISASTER_INFRASTRUCTURE_1: 'DISASTER_INFRASTRUCTURE_1',
  // 承灾基础设施2
  DISASTER_INFRASTRUCTURE_2: 'DISASTER_INFRASTRUCTURE_2',

  // 逃生路线
  ESCAPE_ROUTE: 'ESCAPE_ROUTE',
  // 医疗设施1
  MEDICAL_FACILITY_1: 'MEDICAL_FACILITY_1',
  // 应急安置
  EMERGENCY_SHELTER: 'EMERGENCY_SHELTER',
  // 避难场所
  SHELTER_PLACE: 'SHELTER_PLACE',

  // 复盘报告
  REVIEW_REPORT: 'REVIEW_REPORT',
}

export const SINGLE_TAB = {
  // 总览
  OVERVIEW: 'OVERVIEW',
  // 灾前体检
  PRE_DISASTER_CHECK: 'PRE_DISASTER_CHECK',
  // 灾害模拟
  DISASTER_SIMULATION: 'DISASTER_SIMULATION',
  // 受灾分析
  DISASTER_ANALYSIS: 'DISASTER_ANALYSIS',
  // 避险安置
  LIFE_ENGINEERING: 'LIFE_ENGINEERING',
  // 复盘总结
  REVIEW_SUMMARY: 'REVIEW_SUMMARY',
}

export const SINGLE_TABS = [
  {
    label: '总体概览',
    name: SINGLE_TAB.OVERVIEW,
  },
  {
    label: '灾前体检',
    name: SINGLE_TAB.PRE_DISASTER_CHECK,
  },
  {
    label: '灾害模拟',
    name: SINGLE_TAB.DISASTER_SIMULATION,
  },
  {
    label: '受灾分析',
    name: SINGLE_TAB.DISASTER_ANALYSIS,
  },
  {
    label: '避险安置',
    name: SINGLE_TAB.LIFE_ENGINEERING,
  },
  {
    label: '复盘总结',
    name: SINGLE_TAB.REVIEW_SUMMARY,
  },
]

export const SINGLE_TAB_MAP = {
  [SINGLE_TAB.OVERVIEW]: [
    { key: SINGLE_KEY.LOCATION, value: '地理位置' },
    { key: SINGLE_KEY.SUPERVISION, value: '监管情况' },
  ],
  [SINGLE_TAB.PRE_DISASTER_CHECK]: [
    { key: SINGLE_KEY.NEARBY_WATER_SYSTEM, value: '临近水系' },
    { key: SINGLE_KEY.AVERAGE_RAINFALL, value: '{date}平均降雨' },
    { key: SINGLE_KEY.SLOPE_SHAPE, value: '斜坡形态' },
    { key: SINGLE_KEY.SLOPE_ELEVATION, value: '斜坡高程' },
    { key: SINGLE_KEY.SLOPE_SLOPE, value: '斜坡坡向' },
    { key: SINGLE_KEY.GEOLOGICAL_EXCAVATION, value: '地质开挖' },
  ],
  [SINGLE_TAB.DISASTER_SIMULATION]: [
    { key: SINGLE_KEY.SIMULATE_START, value: '灾害模拟演示', order: 0 },
    { key: SINGLE_KEY.RAIN, value: '降雨', order: 1 },
    { key: SINGLE_KEY.FLOOD, value: '水位上涨', order: 2 },
    { key: SINGLE_KEY.SLOPE_SLIDE, value: '滑坡模拟', order: 3 },
    { key: SINGLE_KEY.PARTICLE_ANALYSIS, value: '滑坡粒子分析', order: 4 },
    { key: SINGLE_KEY.AI_ANALYSIS, value: '滑坡AI分析', order: 5 },
  ],
  [SINGLE_TAB.DISASTER_ANALYSIS]: [
    { key: SINGLE_KEY.DISASTER_POPULATION, value: '承灾人口' },
    { key: SINGLE_KEY.DISASTER_HOUSE, value: '承灾房屋' },
    { key: SINGLE_KEY.DISASTER_INFRASTRUCTURE_1, value: '承灾基础设施-1' },
    { key: SINGLE_KEY.DISASTER_INFRASTRUCTURE_2, value: '承灾基础设施-2' },
  ],
  [SINGLE_TAB.LIFE_ENGINEERING]: [
    { key: SINGLE_KEY.ESCAPE_ROUTE, value: '逃生路线' },
    { key: SINGLE_KEY.MEDICAL_FACILITY_1, value: '医疗设施 -1' },
    { key: SINGLE_KEY.EMERGENCY_SHELTER, value: '应急安置' },
    { key: SINGLE_KEY.SHELTER_PLACE, value: '避难场所' },
  ],
  [SINGLE_TAB.REVIEW_SUMMARY]: [{ key: SINGLE_KEY.REVIEW_REPORT, value: '复盘报告' }],
}
