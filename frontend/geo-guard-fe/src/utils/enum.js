import { generateOptions } from './index.js'

export const CHAT_TYPE = {
  NORMAL: 'normal',
  // AI 识图助手
  IMAGE_RECOGNITION: 'image_recognition',
  // 会议纪要智能体
  MEETING_MINUTES: 'meeting_minutes',
  // 空间分析智能体
  SPACE_ANALYSIS: 'space_analysis',
  // 调度模式
  ASSISTANT: 'assistant',
  // 防御响应模式
  DEFENSE: 'defense',
  // 处置管理
  DISPOSAL: 'disposal',
  // 预测模式
  PREDICT: 'predict',
  // 任务推送
  TASK_SEND: 'task_send',
  // 不显示
  NOT_SHOW: 'not_show',
}

export const AREA_LEVEL = {
  PROVINCE: 1, // 省
  CITY: 2, // 市
  COUNTY: 3, // 县/区
  STREET: 4, // 镇
  VILLAGE: 5, // 村
}

// 斜坡单元对应查询参数键名
export const SLOPE_UNIT_KEY = {
  [AREA_LEVEL.PROVINCE]: 'province',
  [AREA_LEVEL.CITY]: 'city',
  [AREA_LEVEL.COUNTY]: 'county',
  [AREA_LEVEL.STREET]: 'street',
  [AREA_LEVEL.VILLAGE]: 'village',
}

export const LAST_AREA_LEVEL = AREA_LEVEL.VILLAGE

/**
 * 行政区划边界线各级抽稀容差配置（单位：度）
 * 一级：保持不变 (0.001，约 100~110 米)
 * 二级：精细一点 (0.0003，约 30~33 米)
 * 三级：不抽稀 (0，保持原始精度)
 */
export const AREA_BOUNDARY_SIMPLIFY_TOLERANCE = {
  LEVEL_1: 0.0001,
  LEVEL_2: 0.00005,
  LEVEL_3: 0,
}

// 斜坡单元风险等级4为最高，其他的等级1是最高,保持统一颜色定义的基础上，将此处反着写
export const NORMAL_LEVEL = {
  NONE: 0,
  EXTREME_HIGH: 1,
  HIGH: 2,
  MIDDLE: 3,
  LOW: 4,
}
export const NORMAL_LEVEL_TEXT = {
  [NORMAL_LEVEL.NONE]: '无',
  [NORMAL_LEVEL.EXTREME_HIGH]: '极高',
  [NORMAL_LEVEL.HIGH]: '高',
  [NORMAL_LEVEL.MIDDLE]: '中',
  [NORMAL_LEVEL.LOW]: '低',
}
export const NORMAL_LEVEL_COLOR = {
  [NORMAL_LEVEL.NONE]: '#878898', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [NORMAL_LEVEL.LOW]: '#3561FA', // 实为最低
  [NORMAL_LEVEL.MIDDLE]: '#F9C568',
  [NORMAL_LEVEL.HIGH]: '#FF7C3A',
  [NORMAL_LEVEL.EXTREME_HIGH]: '#E23030', // 实为最高
}

// 区域默认根节点code 恩施州
export const DEFAULT_ROOT_AREA_CODE = 422800
// 恩施市
export const DEFAULT_AREA_CODE = 422801

// 报灾风险等级
export const DISASTER_LEVEL_RISK = {
  LOW: 1,
  MIDDLE: 2,
  HIGH: 3,
  EXTREME_HIGH: 4,
}

// 风险研判指数等级
export const RISK_LEVEL = {
  NONE: 0,
  LOW: 1,
  MIDDLE: 2,
  HIGH: 3,
  EXTREME_HIGH: 4,
}
export const RISK_LEVEL_TEXT = {
  [RISK_LEVEL.NONE]: '无',
  [RISK_LEVEL.LOW]: '低',
  [RISK_LEVEL.MIDDLE]: '中',
  [RISK_LEVEL.HIGH]: '高',
  [RISK_LEVEL.EXTREME_HIGH]: '极高',
}

// 任务颜色映射
export const colorMapper = {
  null: '#878898',
  [RISK_LEVEL.NONE]: '#878898',
  [RISK_LEVEL.LOW]: '#8FB6F6',
  [RISK_LEVEL.MIDDLE]: '#F9C568',
  [RISK_LEVEL.HIGH]: '#FA8043',
  [RISK_LEVEL.EXTREME_HIGH]: '#E23030',
}

//斜坡单元风险
export const RISK_LEVEL_COLOR = {
  [RISK_LEVEL.NONE]: '#878898', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [RISK_LEVEL.LOW]: '#3561FA',
  [RISK_LEVEL.MIDDLE]: '#F9C568',
  [RISK_LEVEL.HIGH]: '#FF7C3A',
  [RISK_LEVEL.EXTREME_HIGH]: '#E23030',
}

export const RESPONSE_LEVEL_COLOR = {
  4: '#E23030',
  3: '#FF7C3A',
  2: '#F9C568',
  1: '#3561FA',
}

export const RiskLevelOptions = [
  {
    key: 'superHigh',
    label: '极高风险',
    value: RISK_LEVEL.EXTREME_HIGH,
    color: '#E23030',
    icon: new URL('@/assets/imgs/super-high.png', import.meta.url).href,
  },
  {
    key: 'high',
    label: '高风险',
    value: RISK_LEVEL.HIGH,
    color: '#FA8043',
    icon: new URL('@/assets/imgs/high.png', import.meta.url).href,
  },
  {
    key: 'middle',
    label: '中风险',
    value: RISK_LEVEL.MIDDLE,
    color: '#EDBA5D',
    icon: new URL('@/assets/imgs/middle.png', import.meta.url).href,
  },
  {
    key: 'low',
    label: '低风险',
    value: RISK_LEVEL.LOW,
    color: '#699EF5',
    icon: new URL('@/assets/imgs/low.png', import.meta.url).href,
  },
]

export const RISK_LEVEL_LINE_COLOR = {
  [RISK_LEVEL.NONE]: 'linear-gradient(180deg, #FFF 0%, #fff 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [RISK_LEVEL.LOW]: 'linear-gradient(180deg, #FFF 0%, #3561FA 100%)',
  [RISK_LEVEL.MIDDLE]: 'linear-gradient(180deg, #FFF 0%, #F9C568 100%)',
  [RISK_LEVEL.HIGH]: 'linear-gradient(180deg, #FFF 0%, #FF7C3A 100%)',
  [RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(180deg, #FFF 0%, #E23030 100%)',
}

export const RISK_LEVEL_BRAND_TITLE_COLOR = {
  [RISK_LEVEL.NONE]: 'linear-gradient(180deg, #8788984d 0%, #878898 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [RISK_LEVEL.LOW]: 'linear-gradient(180deg, #3561FA4d 0%, #3561FA 100%)',
  [RISK_LEVEL.MIDDLE]: 'linear-gradient(180deg, #F9C5684d 0%, #F9C568 100%)',
  [RISK_LEVEL.HIGH]: 'linear-gradient(180deg, #FF7C3A4d 0%, #FF7C3A 100%)',
  [RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(180deg, #eb2c2c4d 0%, #EB2C2C 100%)',
}

export const RISK_LEVEL_LINK_BTN_COLOR = {
  [RISK_LEVEL.NONE]: 'linear-gradient(90deg, #878898b3 0%, #87889833 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [RISK_LEVEL.LOW]: 'linear-gradient(90deg, #3561FAb3 0%, #3561FA33 100%)',
  [RISK_LEVEL.MIDDLE]: 'linear-gradient(90deg, #F9C568b3 0%, #F9C56833 100%)',
  [RISK_LEVEL.HIGH]: 'linear-gradient(90deg, #FF7C3Ab3 0%, #FF7C3A33 100%)',
  [RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(90deg, #eb2c2cb3 0%, #eb2c2c33 100%)',
}

export const RISK_LEVEL_CARD_BG_COLOR = {
  [RISK_LEVEL.NONE]: '#f0f1ff', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  [RISK_LEVEL.LOW]: '#e9effa',
  [RISK_LEVEL.MIDDLE]: '#fff9f1',
  [RISK_LEVEL.HIGH]: '#f8ece6',
  [RISK_LEVEL.EXTREME_HIGH]: '#ede3e0',
}

// 易发性
export const RISK_OCCUR = {
  [RISK_LEVEL.NONE]: '不易',
  [RISK_LEVEL.LOW]: '不易',
  [RISK_LEVEL.MIDDLE]: '较易',
  [RISK_LEVEL.HIGH]: '易',
  [RISK_LEVEL.EXTREME_HIGH]: '极易',
}

// 图层枚举
export const LAYER_ENUM = {
  SINGLE_SLOPE_UNIT: 0, // 单个斜坡单元
  RISK_INDEX: 1, // 风险研判指数,
  MONITOR_POINT: 2, // 监测点
  HAZARD_POINT: 3, // 灾害点
  ALARM_POINT: 4, // 报灾点
  RISK_AREA: 5, // 风险区
  DISASTER_OBJ: 6, // 承灾体
  RAIN_DISTRI: 7, // 雨量分布
  DEAL_POINT: 8, // 处置点
}

// 风险归因分析
export const RISK_ATTRIBUTION_ENUM = {
  SUSCEPTIBILITY: 1,
  DANGE: 2,
  VULNERABILITY: 3,
  DYNAMIC_RISK: 4,
}
export const RISK_ATTRIBUTION_TEXT_ENUM = {
  [RISK_ATTRIBUTION_ENUM.SUSCEPTIBILITY]: '自身易发',
  [RISK_ATTRIBUTION_ENUM.DANGE]: '降雨超阈值',
  [RISK_ATTRIBUTION_ENUM.VULNERABILITY]: '承灾密度大',
  [RISK_ATTRIBUTION_ENUM.DYNAMIC_RISK]: '出现灾险情',
}

export const KEY_MAP = {
  // 斜坡单元 entity name标识
  KEY_LABEL: 'KEY_LABEL_IDENTIFICATION',
}

// 报灾状态
export const REPORT_STATUS = {
  TO_DO: 1,
  REPORT: 2,
  DONE: 3,
}

export const REPORT_STATUS_TESXT = {
  [REPORT_STATUS.TO_DO]: '待处理',
  [REPORT_STATUS.REPORT]: '已报送',
  [REPORT_STATUS.DONE]: '已处理',
}

// 报灾来源
export const REPORT_SOURCE = {
  TASK: 1,
  PEOPLE: 2,
}

export const REPORT_SOURCE_TEXT = {
  [REPORT_SOURCE.TASK]: '任务反馈',
  [REPORT_SOURCE.PEOPLE]: '群众报灾',
}

// 根来源类型
export const ROOT_SOURCE_TYPE = {
  UNKNOWN: 0,
  TASK_FEEDBACK: 1,
  PUBLIC_REPORT: 2,
  WARNING_INFO: 3,
  MONITOR_WARNING: 4,
  SYSTEM_EVALUATION: 5,
  MANUAL_ADD: 6,
  TECH_ASSISTANCE: 7,
  DISPOSAL_MANAGEMENT: 8,
  DEFENSE_RESPONSE: 9,
  EMERGENCY_DISPOSAL: 10,
}
export const ROOT_SOURCE_TYPE_TEXT = {
  [ROOT_SOURCE_TYPE.UNKNOWN]: '未知',
  [ROOT_SOURCE_TYPE.TASK_FEEDBACK]: '任务反馈',
  [ROOT_SOURCE_TYPE.PUBLIC_REPORT]: '群众报灾',
  [ROOT_SOURCE_TYPE.WARNING_INFO]: '预警信息',
  [ROOT_SOURCE_TYPE.MONITOR_WARNING]: '监测预警',
  [ROOT_SOURCE_TYPE.SYSTEM_EVALUATION]: '每日评价',
  [ROOT_SOURCE_TYPE.MANUAL_ADD]: '手动添加',
  [ROOT_SOURCE_TYPE.TECH_ASSISTANCE]: '技术协查',
  [ROOT_SOURCE_TYPE.DISPOSAL_MANAGEMENT]: '处置管理',
  [ROOT_SOURCE_TYPE.DEFENSE_RESPONSE]: '防御响应',
  [ROOT_SOURCE_TYPE.EMERGENCY_DISPOSAL]: '应急处置',
}
export const ROOT_SOURCE_TYPE_OPTIONS = generateOptions(ROOT_SOURCE_TYPE_TEXT)

export const ASSISTANT_LIST = [
  {
    title: 'AI识图智能体',
    desc: '识别现场图片中的地灾迹象',
    type: CHAT_TYPE.IMAGE_RECOGNITION,
    name: 'AI识图',
    selfIntro: '上传现场照片后，我可以识别裂缝、变形及其他异常迹象。',
    questions: [],
  },
  {
    title: '空间分析智能体',
    desc: '基于坐标点汇聚地上地下一体化信息',
    type: CHAT_TYPE.SPACE_ANALYSIS,
    name: '空间分析',
    selfIntro: '在地图上选择坐标点后，我可以为您汇聚并分析周边空间信息。',
    questions: [],
  },
  {
    title: '制度规范',
    desc: '提供合规与技术依据',
    type: 'number',
    icon: new URL('@/assets/imgs/chatEngine/card-4.png', import.meta.url).href,
    cardTitle: '明确责任边界，规范技术查询',
    cardDesc: '提供法律法规、规范文件、行政管理制度、专业技术标准等。',
    name: '制度规范',
    selfIntro: '专注提供合规依据与技术支撑，助力精准防控，你可以这样问我：',
    questions: [
      '汛期地质灾害巡排查工作有哪些内容？',
      '如何开展地质灾害双控建设？',
      '网格化管理职责有哪些？',
      // '试点区本月的处置事件有多少？',
      // '龙凤镇近 3 天的高风险斜坡单元有多少？',
      // '恩施大峡谷风景管理处近10年来发生的灾害事件有多少？',
      // '金子坝街道本周派发的巡查任务有哪些？',
    ],
  },
  {
    title: '地灾科普',
    desc: '普及实用知识',
    type: 'education',
    icon: new URL('@/assets/imgs/chatEngine/card-6.png', import.meta.url).href,
    cardTitle: '普及实用知识',
    cardDesc: '提供地灾防治基础知识、群众防灾避险应知应会。',
    name: '科普宣传员',
    selfIntro: '普及地灾识别与避险知识，筑牢群防群治基础，你可以这样问我：',
    questions: [
      '滑坡地质灾害一般是如何发生的？',
      '遇到地质灾害如何避险？',
      '如何识别地质灾害？',
      '地质灾害防治措施有哪些？',
    ],
  },
  {
    title: '解决方案',
    desc: '提供全流程防治支撑',
    type: 'technical',
    icon: new URL('@/assets/imgs/chatEngine/card-5.png', import.meta.url).href,
    cardTitle: '一体化防治方案',
    cardDesc: '提供规划计划、巡查排查、预警响应、演练培训等常用方案。',
    name: '解决方案',
    selfIntro: '专注提供全流程防治支撑，助力科学决策与应急处置，你可以这样问我：',
    questions: [
      '对于一处已出现裂缝的小型滑坡，有哪些经济有效的应急治理措施？',
      '如何制定一份实用的地质灾害群测群防年度工作方案？',
      '在山区公路边坡防护中，如何选择“挡土墙”还是“锚杆格构”方案？',
    ],
  },

  // {
  //   title: '法规制度',
  //   desc: '提供合规依据',
  //   type: 'law',
  //   icon: new URL('@/assets/imgs/chatEngine/card-4.png', import.meta.url).href,
  //   cardTitle: '明确防控责任边界',
  //   cardDesc: '相关法律法规、分级管理制度等',
  //   name: '合规顾问',
  //   selfIntro: '能够解读地灾防治法规政策，明晰责任边界与，你可以这样问我：',
  //   questions: [
  //     '我省地灾防治工作主要有哪些？',
  //     '及时上报地质灾害有没有奖励？',
  //     '地灾防治工作各部门职责是什么？',
  //     '如何认定新的灾害点？',
  //     '地质灾害有几个预警响应等级？',
  //   ],
  // },
  // {
  //   title: '规划方案',
  //   desc: '明确防治方向',
  //   type: 'plan',
  //   icon: new URL('@/assets/imgs/chatEngine/card-2.png', import.meta.url).href,
  //   cardTitle: '明确防控重点任务',
  //   cardDesc: '区域防治专项规划、年度工作方案等',
  //   name: '规划参谋',
  //   selfIntro: '能够梳理地灾防治规划目标与重点部署，提供清晰实施指引，你可以这样问我：',
  //   disabled: true,
  //   questions: [
  //     '我省地质灾害防治规划目标及任务是什么？',
  //     '我省地质灾害重点防治区在哪？',
  //     '地质灾害重点防范期集中在哪几个月？',
  //     '哪几个县（市、区）正在开展双控体系建设工作？',
  //   ],
  // },
  // {
  //   title: '应急处置',
  //   desc: '提供清晰指导',
  //   type: 'emergency',
  //   icon: new URL('@/assets/imgs/chatEngine/card-3.png', import.meta.url).href,
  //   cardTitle: '指引处置流程',
  //   cardDesc: '防御响应流程、预警处置规范',
  //   name: '应急调度',
  //   selfIntro: '聚焦地灾应急响应与处置流程，提供实战应对指引，你可以这样问我：',
  //   disabled: true,
  //   questions: [
  //     '我省地质灾害防御响应工作职责如何划分？',
  //     '地质灾害防御响应分几个等级，其应对措施是什么？',
  //     '2025年我省发生了哪些重大地质灾害，损失如何？',
  //     '恩施州地质灾害应急处置概况',
  //   ],
  // },
]
// 事件类型
export const EVENT_TYPE = {
  HP: 1,
  BT: 2,
  DMTX: 3,
  NSL: 4,
  WY: 5,
  OTHER: 100,
}
export const EVENT_TYPE_TEXT = {
  [EVENT_TYPE.HP]: '滑坡',
  [EVENT_TYPE.BT]: '崩塌',
  [EVENT_TYPE.DMTX]: '地面塌陷',
  [EVENT_TYPE.NSL]: '泥石流',
  [EVENT_TYPE.WY]: '危岩',
  [EVENT_TYPE.OTHER]: '其他',
}
export const EVENT_TYPE_OPTIONS = [
  { label: '滑坡', value: 1 },
  { label: '崩塌', value: 2 },
  { label: '地面塌陷', value: 3 },
  { label: '泥石流', value: 4 },
  { label: '危岩', value: 5 },
  { label: '其他', value: 100 },
]

// 复盘状态
export const REVIEW_STATUS = {
  PENDING: 0, // 待复盘
  GENERATING: 1, // 生成中
  CONFIRMING: 2, // 待确认
  CONFIRMED: 3, // 已确认
  ARCHIVED: 4, // 已归档
}
export const REVIEW_STATUS_TEXT = {
  [REVIEW_STATUS.PENDING]: '待复盘',
  [REVIEW_STATUS.GENERATING]: '生成中',
  [REVIEW_STATUS.CONFIRMING]: '待确认',
  [REVIEW_STATUS.CONFIRMED]: '已确认',
  [REVIEW_STATUS.ARCHIVED]: '已归档',
}
export const REVIEW_STATUS_OPTIONS = [
  { label: '待复盘', value: REVIEW_STATUS.PENDING },
  { label: '生成中', value: REVIEW_STATUS.GENERATING },
  { label: '待确认', value: REVIEW_STATUS.CONFIRMING },
  { label: '已确认', value: REVIEW_STATUS.CONFIRMED },
  { label: '已归档', value: REVIEW_STATUS.ARCHIVED },
]

// 处置进度
export const DEAL_STATUS = {
  RESEARCH: 1,
  MEETING: 2,
  PLAN: 3,
  RES: 4,
  DONE: 5,
}
export const DEAL_STATUS_TEXT = {
  [DEAL_STATUS.RESEARCH]: '应急调查',
  [DEAL_STATUS.MEETING]: '会商研判',
  [DEAL_STATUS.PLAN]: '方案接入',
  [DEAL_STATUS.RES]: '响应执行',
  [DEAL_STATUS.DONE]: '闭环归档',
}
export const DEAL_STATUS_OPTIONS = generateOptions(DEAL_STATUS_TEXT)

// 易发原因
export const EASY_REASON = {
  SELF: 1,
  RAIN: 2,
  PERSON: 3,
  RISK: 4,
}

export const EASY_REASON_MAPPER = {
  [EASY_REASON.SELF]: '自身易发',
  [EASY_REASON.RAIN]: '降雨超阈值',
  [EASY_REASON.PERSON]: '人口密度大',
  [EASY_REASON.RISK]: '出现灾险情',
}

// 发展趋势
export const DEVELOP_TREND = {
  1: '即将失稳破坏',
  2: '可能继续变形',
  3: '趋于稳定',
}

// 方案类型
export const PLAN_TYPE = {
  RETREAT: 1, // 人员安置
  ALARM: 2, // 警示防护
  INSPECT: 3, // 监测巡查（群测群防）
  RISK: 4, // 排危除险
  GOVERN: 5, // 工程治理
  PROMOTION: 6, // 宣传告知
  TRAFFIC: 7, // 交通管制
  OTHER: 8, // 其他建议
  INSTRUMENT_INSPECT: 9, // 监测巡查（仪器监测）
}
export const PLAN_TYPE_TEXT = {
  [PLAN_TYPE.RETREAT]: '人员安置',
  [PLAN_TYPE.ALARM]: '警示防护',
  [PLAN_TYPE.INSPECT]: '监测巡查（群测群防）',
  [PLAN_TYPE.RISK]: '排危除险',
  [PLAN_TYPE.GOVERN]: '工程治理',
  [PLAN_TYPE.PROMOTION]: '宣传告知',
  [PLAN_TYPE.TRAFFIC]: '交通管制',
  [PLAN_TYPE.OTHER]: '其他建议',
  [PLAN_TYPE.INSTRUMENT_INSPECT]: '监测巡查（仪器监测）',
}
// 方案状态
export const PLAN_STATUS = {
  DRAFT: 0,
  TO_DO: 1,
  DOING: 2,
  DONE: 3,
}

// 任务闭环状态
export const TASK_TRACK_STATUS = {
  UN_SEND: 1, // 待派发
  UN_CHECK: 2, // 待核查
  CHECKING: 3, // 核查中
  CLOSED: 4, // 已关闭
  FEEDBACK: 5, // 已反馈
  TECH_ASSISTANCE: 6, // 技术协查中
  EXPIRED: 7, // 已逾期
}

// 任务闭环状态筛选值
export const TASK_TRACK_STATUS_NUM = {
  ALL: 1, // 全部
  DONE: 2, // 已完成
  UNFINISHED: 3, // 未完成
  UN_SEND: 4, // 未推送
  UN_CHECK: 5, // 未核查
  CHECKING: 6, // 核查中
  FEEDBACK: 7, // 已反馈
  CLOSED: 8, // 已关闭
  EXPIRED: 9, // 已过期
  TECH_ASSISTANCE: 10, // 申请技术协查
}

export const TASK_TRACK_STATUS_TEXT = {
  [TASK_TRACK_STATUS.UN_SEND]: '待派发',
  [TASK_TRACK_STATUS.UN_CHECK]: '待核查',
  [TASK_TRACK_STATUS.CHECKING]: '核查中',
  [TASK_TRACK_STATUS.CLOSED]: '已关闭',
  [TASK_TRACK_STATUS.FEEDBACK]: '已反馈',
  [TASK_TRACK_STATUS.TECH_ASSISTANCE]: '技术协查中',
  [TASK_TRACK_STATUS.EXPIRED]: '已逾期',
}

// 当前流程状态
export const CURRENT_STATUS = {
  CIRCULATING: 1,
  EXECUTING: 2,
  FEEDBACK: 3,
  TECH_ASSISTANCE: 4,
  EXPIRED: 5,
  FINISHED: 6,
}
export const CURRENT_STATUS_TEXT = {
  [CURRENT_STATUS.CIRCULATING]: '流转中',
  [CURRENT_STATUS.EXECUTING]: '执行中',
  [CURRENT_STATUS.FEEDBACK]: '已反馈',
  [CURRENT_STATUS.TECH_ASSISTANCE]: '技术协查',
  [CURRENT_STATUS.EXPIRED]: '已过期',
  [CURRENT_STATUS.FINISHED]: '已结束',
}
export const CURRENT_STATUS_OPTIONS = generateOptions(CURRENT_STATUS_TEXT)

// 方案状态-调整
export const PLAN_PROCESS = TASK_TRACK_STATUS
export const PLAN_PROCESS_TEXT = TASK_TRACK_STATUS_TEXT

// 风险预测推送状态
export const PREDICTION_PUSH_STATUS = {
  NOT_PUSH: 0,
  PUSHED: 1,
}
export const PREDICTION_PUSH_STATUS_TEXT = {
  [PREDICTION_PUSH_STATUS.NOT_PUSH]: '未推送',
  [PREDICTION_PUSH_STATUS.PUSHED]: '已推送',
}
export const PREDICTION_PUSH_STATUS_OPTIONS = generateOptions(PREDICTION_PUSH_STATUS_TEXT)

// 风险预测类型
export const PREDICTION_TYPE = {
  // DAY: 1, // 日
  // WEEK: 2, // 周
  MONTH: 3, // 月
  QUARTER: 4, // 季度
  YEAR: 5, // 年
}
export const PREDICTION_TYPE_TEXT = {
  // [PREDICTION_TYPE.DAY]: '日',
  // [PREDICTION_TYPE.WEEK]: '周',
  [PREDICTION_TYPE.MONTH]: '月度',
  [PREDICTION_TYPE.QUARTER]: '季度',
  [PREDICTION_TYPE.YEAR]: '年度',
}

export const PREDICTION_TYPE_OPTIONS = generateOptions(PREDICTION_TYPE_TEXT)

// 预测风险等级
export const PRED_RISK_LEVEL = {
  // NONE: 0,
  // VERY_LOW: 0,
  LOW: 1,
  MIDDLE: 2,
  HIGH: 3,
  EXTREME_HIGH: 4,
}

export const PRED_RISK_LEVEL_TEXT = {
  // [PRED_RISK_LEVEL.NONE]: '无明显',
  // [PRED_RISK_LEVEL.VERY_LOW]: '低',
  [PRED_RISK_LEVEL.LOW]: '较低',
  [PRED_RISK_LEVEL.MIDDLE]: '中',
  [PRED_RISK_LEVEL.HIGH]: '高',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: '极高',
}

export const PRED_RISK_LEVEL_COLOR = {
  // [PRED_RISK_LEVEL.NONE]: '#878898', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  // [PRED_RISK_LEVEL.VERY_LOW]: '#50fba4',
  [PRED_RISK_LEVEL.LOW]: '#3561FA',
  [PRED_RISK_LEVEL.MIDDLE]: '#F9C568',
  [PRED_RISK_LEVEL.HIGH]: '#FF7C3A',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: '#E23030',
}

export const PRED_RISK_LEVEL_LINE_COLOR = {
  // [PRED_RISK_LEVEL.NONE]: 'linear-gradient(180deg, #FFF 0%, #fff 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  // [PRED_RISK_LEVEL.VERY_LOW]: 'linear-gradient(180deg, #FFF 0%, #50fba4 100%)',
  [PRED_RISK_LEVEL.LOW]: 'linear-gradient(180deg, #FFF 0%, #3561FA 100%)',
  [PRED_RISK_LEVEL.MIDDLE]: 'linear-gradient(180deg, #FFF 0%, #F9C568 100%)',
  [PRED_RISK_LEVEL.HIGH]: 'linear-gradient(180deg, #FFF 0%, #FF7C3A 100%)',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(180deg, #FFF 0%, #E23030 100%)',
}

export const PRED_RISK_LEVEL_BRAND_TITLE_COLOR = {
  // [PRED_RISK_LEVEL.NONE]: 'linear-gradient(180deg, #8788984d 0%, #878898 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  // [PRED_RISK_LEVEL.VERY_LOW]: 'linear-gradient(180deg, #50fba44d 0%, #50fba4 100%)',
  [PRED_RISK_LEVEL.LOW]: 'linear-gradient(180deg, #3561FA4d 0%, #3561FA 100%)',
  [PRED_RISK_LEVEL.MIDDLE]: 'linear-gradient(180deg, #F9C5684d 0%, #F9C568 100%)',
  [PRED_RISK_LEVEL.HIGH]: 'linear-gradient(180deg, #FF7C3A4d 0%, #FF7C3A 100%)',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(180deg, #eb2c2c4d 0%, #EB2C2C 100%)',
}

export const PRED_RISK_LEVEL_LINK_BTN_COLOR = {
  // [PRED_RISK_LEVEL.NONE]: 'linear-gradient(90deg, #878898b3 0%, #87889833 100%)', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  // [PRED_RISK_LEVEL.VERY_LOW]: 'linear-gradient(180deg, #50fba4b3 0%, #50fba433 100%)',
  [PRED_RISK_LEVEL.LOW]: 'linear-gradient(90deg, #3561FAb3 0%, #3561FA33 100%)',
  [PRED_RISK_LEVEL.MIDDLE]: 'linear-gradient(90deg, #F9C568b3 0%, #F9C56833 100%)',
  [PRED_RISK_LEVEL.HIGH]: 'linear-gradient(90deg, #FF7C3Ab3 0%, #FF7C3A33 100%)',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: 'linear-gradient(90deg, #eb2c2cb3 0%, #eb2c2c33 100%)',
}

export const PRED_RISK_LEVEL_CARD_BG_COLOR = {
  // [PRED_RISK_LEVEL.NONE]: '#f0f1ff', // 容错，一般不会显示，如果显示肯定是后端返回的数据出错
  // [PRED_RISK_LEVEL.VERY_LOW]: '#e5f6ef',
  [PRED_RISK_LEVEL.LOW]: '#e9effa',
  [PRED_RISK_LEVEL.MIDDLE]: '#fff9f1',
  [PRED_RISK_LEVEL.HIGH]: '#f8ece6',
  [PRED_RISK_LEVEL.EXTREME_HIGH]: '#ede3e0',
}

export const dateStrMap = {
  3: '月度',
  4: '季度',
  5: '年度',
}

// 会议类型

// 自定义记录类型
export const CUSTOM_RECORD_TYPE = {
  // 是否生成过一张图
  ONE_GRAPH: 'one_graph',
}

// 角色类型
export const ROLE_MAP = {
  // 超级管理员
  SUPER_ADMIN: 'superadmin',

  // 值班员
  DUTY_OFFICER: 'dz_zby',

  // 分管县长
  DEPUTY_COUNTY_HEAD: 'dz_fgxianzhang',

  // 县长
  COUNTY_HEAD: 'dz_xz',

  // 县自规局领导
  COUNTY_NATURAL_RESOURCES_LEADER: 'dz_zgjld',

  // 乡自规所所长
  TOWNSHIP_NATURAL_RESOURCES_DIRECTOR: 'dz_zgssz',

  // 分管乡长
  DEPUTY_TOWNSHIP_HEAD: 'dz_fgxiangzhang',

  // 村支书
  VILLAGE_SECRETARY: 'dz_czs',

  // 监测员
  MONITOR: 'dz_yhdjcy',

  // 技术支撑单位
  TECH_SUPPORT_UNIT: 'dz_jszcry',

  // 专家
  EXPERT: 'dz_zj',

  // 专业技术人员
  ASSISTANT: 'dz_xgy',

  // 群众
  PUBLIC: 'dz_qz',

  // 风险区巡查员
  RISK_AREA_INSPECTOR: 'dz_fxqxcy',
}

export const StoreKey = {
  LARGE_FONT_SIZE: 'LARGE_FONT_SIZE',
  FONT_SIZE: 'LOCATION_FONT_SIZE',
}

// 灾害类型枚举
export const DISASTER_TYPE = {
  SLOPE: '00',
  SLIDE: '01',
  COLLAPSE: '02',
  LANDSLIDE: '03',
  SINK: '04',
}
export const DISASTER_TYPE_TEXT = {
  [DISASTER_TYPE.SLOPE]: '斜坡',
  [DISASTER_TYPE.SLIDE]: '滑坡',
  [DISASTER_TYPE.COLLAPSE]: '崩塌',
  [DISASTER_TYPE.LANDSLIDE]: '泥石流',
  [DISASTER_TYPE.SINK]: '地面塌陷',
}

// 灾害等级枚举
export const DISASTER_LEVEL = {
  LOW: 'A',
  MID: 'B',
  HUGE: 'C',
  EXTREAM: 'D',
  UNKNOWN: 'E',
}
export const DISASTER_LEVEL_TEXT = {
  [DISASTER_LEVEL.LOW]: '小型',
  [DISASTER_LEVEL.MID]: '中型',
  [DISASTER_LEVEL.HUGE]: '大型',
  [DISASTER_LEVEL.EXTREAM]: '特大型',
  [DISASTER_LEVEL.UNKNOWN]: '未知',
}

// 灾害稳定行状态
export const STABLE_STAUTS = {
  A: 'A',
  B: 'B',
  C: 'C',
}
export const STABLE_STAUTS_TEXT = {
  [STABLE_STAUTS.A]: '稳定',
  [STABLE_STAUTS.B]: '基本稳定',
  [STABLE_STAUTS.C]: '不稳定',
}

// 设备预警等级
export const WARNING_LEVEL = {
  RED: 4,
  ORANGE: 3,
  YELLOW: 2,
  BLUE: 1,
}
export const WARNING_LEVEL_TEXT = {
  [WARNING_LEVEL.RED]: '红色预警',
  [WARNING_LEVEL.ORANGE]: '橙色预警',
  [WARNING_LEVEL.YELLOW]: '黄色预警',
  [WARNING_LEVEL.BLUE]: '蓝色预警',
}
export const WARNING_LEVEL_OPTIONS = generateOptions(WARNING_LEVEL_TEXT)

// 处置类型
export const DISPOSAL_TYPE = {
  DATA_ERROR: 0,
  DEVICE_MAINTENANCE: 1,
  DEVICE_DESTROYED: 2,
  NORMAL_WARNING: 3,
  MODEL_TO_OPTIMIZE: 4,
}
export const DISPOSAL_TYPE_TEXT = {
  [DISPOSAL_TYPE.DATA_ERROR]: '数据异常导致误报',
  [DISPOSAL_TYPE.DEVICE_MAINTENANCE]: '设备维护导致误报',
  [DISPOSAL_TYPE.DEVICE_DESTROYED]: '设备遭到破坏',
  [DISPOSAL_TYPE.NORMAL_WARNING]: '正常预警',
  [DISPOSAL_TYPE.MODEL_TO_OPTIMIZE]: '预警模型待优化',
}
export const DISPOSAL_TYPE_OPTIONS = generateOptions(DISPOSAL_TYPE_TEXT)

// 是否有效预警
export const VALID_WARNING = {
  INVALID: 0,
  VALID: 1,
}
export const VALID_WARNING_TEXT = {
  [VALID_WARNING.INVALID]: '无效',
  [VALID_WARNING.VALID]: '有效',
}
export const VALID_WARNING_OPTIONS = generateOptions(VALID_WARNING_TEXT)

// 处置状态
export const DISPOSAL_STATUS = {
  UNDONE: 0,
  DONE: 1,
}
export const DISPOSAL_STATUS_TEXT = {
  [DISPOSAL_STATUS.UNDONE]: '未处置',
  [DISPOSAL_STATUS.DONE]: '已处置',
}
export const DISPOSAL_STATUS_OPTIONS = generateOptions(DISPOSAL_STATUS_TEXT)

// 设备传感器类型枚举
export const SENSOR_TYPES = {
  QJ: '倾角',
  JS: '加速度',
  GP: '地表位移',
}

/**
 * 来源类型枚举
 */
export const SOURCE_TYPE = {
  ALL: -1, // 全部
  ADD_BY_USER: 0, // 手动新增
  SYSTEM_ASSESSMENT: 1, // 系统评估
  PUBLIC_REPORT: 2, // 群众上报
  DEFENSE_RESPONSE: 3, // 防御响应专项
  EMERGENCY_DISPOSAL: 4, // 应急处置专项
  MONITORING_WARNING: 5, // 监测预警
  TECHNICAL_ASSISTANCE: 6, // 技术协查
}

/**
 * 来源类型选项配置
 */
export const sourceTypeOptions = [
  { label: '全部', value: SOURCE_TYPE.ALL },
  { label: '手动新增', value: SOURCE_TYPE.ADD_BY_USER },
  { label: '每日评价', value: SOURCE_TYPE.SYSTEM_ASSESSMENT },
  { label: '群众报灾', value: SOURCE_TYPE.PUBLIC_REPORT },
  { label: '区域防御响应', value: SOURCE_TYPE.DEFENSE_RESPONSE },
  { label: '单点防御响应', value: SOURCE_TYPE.EMERGENCY_DISPOSAL },
  { label: '监测预警', value: SOURCE_TYPE.MONITORING_WARNING },
  { label: '技术人员调查', value: SOURCE_TYPE.TECHNICAL_ASSISTANCE },
]

/**
 * 业务类型
 */
export const BUSINESS_TYPE = {
  // 未知
  UNKNOWN: 0,
  // 任务
  TASK: 1,
  // 报灾/报告
  REPORT: 2,
  // 处置管理
  DISPOSAL: 3,
  // 防御响应
  DEFENSE_RESPONSE: 4,
  // 预警信息
  WARNING_INFO: 5,
  // 监测预警
  MONITORING_WARNING: 6,
}

/**
 * 业务类型展示映射
 */
export const BUSINESS_TYPE_TEXT = {
  [BUSINESS_TYPE.UNKNOWN]: '未知',
  [BUSINESS_TYPE.TASK]: '任务',
  [BUSINESS_TYPE.REPORT]: '报灾/报告',
  [BUSINESS_TYPE.DISPOSAL]: '处置管理',
  [BUSINESS_TYPE.DEFENSE_RESPONSE]: '防御响应',
  [BUSINESS_TYPE.WARNING_INFO]: '预警信息',
  [BUSINESS_TYPE.MONITORING_WARNING]: '监测预警',
}

export const BUSINESS_TYPE_OPTIONS = generateOptions(BUSINESS_TYPE_TEXT)

/**
 * 任务流程链路业务类型
 */
export const TASK_PROCESS_CHAIN_BIZ_TYPE = {
  // 未知业务类型
  UNKNOWN: 0,
  // 任务
  TASK: 1,
  // 报灾/报告
  REPORT: 2,
  // 处置管理
  DISPOSAL: 3,
  // 防御响应
  DEFENSE_RESPONSE: 4,
  // 预警信息
  WARNING_INFO: 5,
  // 监测预警
  MONITORING_WARNING: 6,
}

/**
 * 任务流程链路业务类型展示映射
 */
export const TASK_PROCESS_CHAIN_BIZ_TYPE_TEXT = {
  [TASK_PROCESS_CHAIN_BIZ_TYPE.UNKNOWN]: '未知',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.TASK]: '任务',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.REPORT]: '报灾/报告',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.DISPOSAL]: '处置管理',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.DEFENSE_RESPONSE]: '防御响应',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.WARNING_INFO]: '预警信息',
  [TASK_PROCESS_CHAIN_BIZ_TYPE.MONITORING_WARNING]: '监测预警',
}

export const TASK_PROCESS_CHAIN_BIZ_TYPE_OPTIONS = generateOptions(TASK_PROCESS_CHAIN_BIZ_TYPE_TEXT)

/**
 * 任务流程链路来源
 */
export const TASK_PROCESS_CHAIN_SOURCE_TYPE = {
  // 未知链路来源
  UNKNOWN: 0,
  // 任务反馈
  TASK_FEEDBACK: 1,
  // 群众报灾
  PUBLIC_REPORT: 2,
  // 手动添加
  MANUAL_ADD: 3,
  // 系统评估
  SYSTEM_ASSESSMENT: 4,
  // 防御响应
  DEFENSE_RESPONSE: 5,
  // 应急处置
  EMERGENCY_DISPOSAL: 6,
  // 监测预警
  MONITORING_WARNING: 7,
  // 技术协查
  TECHNICAL_ASSISTANCE: 8,
  // 预警信息
  WARNING_INFO: 9,
  // 处置管理
  DISPOSAL: 10,
  // 群众报灾报告
  PUBLIC_REPORT_REPORT: 11,
  // 任务反馈报告
  TASK_FEEDBACK_REPORT: 12,
}

/**
 * 任务流程链路来源展示映射
 */
export const TASK_PROCESS_CHAIN_SOURCE_TYPE_TEXT = {
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.UNKNOWN]: '未知',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.TASK_FEEDBACK]: '任务反馈',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.PUBLIC_REPORT]: '群众报灾',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.MANUAL_ADD]: '手动添加',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.SYSTEM_ASSESSMENT]: '系统评估',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.DEFENSE_RESPONSE]: '防御响应',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.EMERGENCY_DISPOSAL]: '应急处置',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.MONITORING_WARNING]: '监测预警',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.TECHNICAL_ASSISTANCE]: '技术协查',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.WARNING_INFO]: '预警信息',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.DISPOSAL]: '处置管理',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.PUBLIC_REPORT_REPORT]: '群众报灾报告',
  [TASK_PROCESS_CHAIN_SOURCE_TYPE.TASK_FEEDBACK_REPORT]: '任务反馈报告',
}

export const TASK_PROCESS_CHAIN_SOURCE_TYPE_OPTIONS = generateOptions(TASK_PROCESS_CHAIN_SOURCE_TYPE_TEXT)

/**
 * 任务流程链路关键节点类别
 */
export const TASK_PROCESS_CHAIN_NODE_CATEGORY = {
  // 任务开始核查
  TASK_CHECK_START: 1,
  // 任务反馈
  TASK_FEEDBACK: 2,
  // 申请技术协查
  APPLY_TECHNICAL_ASSISTANCE: 3,
  // 开启处置管理
  START_DISPOSAL: 4,
  // 开启单点防御响应
  START_SINGLE_DEFENSE_RESPONSE: 5,
  // 区域防御响应启动
  START_AREA_DEFENSE_RESPONSE: 6,
  // 批量下发处置任务
  BATCH_DISTRIBUTE_DISPOSAL_TASK: 7,
  // 任务关闭
  TASK_CLOSED: 8,
  // 任务过期
  TASK_EXPIRED: 9,
  // 群众报灾
  PUBLIC_REPORT: 11,
  // 任务反馈
  TASK_FEEDBACK_REPORT: 12,
  // 下发AI险情核实任务
  APPLY_AI_DANGER_VERIFY_TASK: 15,
}

/**
 * 任务流程链路关键节点类别展示映射
 */
export const TASK_PROCESS_CHAIN_NODE_CATEGORY_TEXT = {
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.TASK_CHECK_START]: '任务开始核查',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.TASK_FEEDBACK]: '任务反馈',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.APPLY_TECHNICAL_ASSISTANCE]: '申请技术协查',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.START_DISPOSAL]: '开启处置管理',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.START_SINGLE_DEFENSE_RESPONSE]: '开启单点防御响应',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.START_AREA_DEFENSE_RESPONSE]: '区域防御响应启动',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.BATCH_DISTRIBUTE_DISPOSAL_TASK]: '批量下发处置任务',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.TASK_CLOSED]: '任务关闭',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.TASK_EXPIRED]: '任务过期',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.PUBLIC_REPORT]: '群众报灾报告',
  [TASK_PROCESS_CHAIN_NODE_CATEGORY.TASK_FEEDBACK_REPORT]: '任务反馈报告',
}

export const TASK_PROCESS_CHAIN_NODE_CATEGORY_OPTIONS = generateOptions(TASK_PROCESS_CHAIN_NODE_CATEGORY_TEXT)

export const GraphColor = {
  // 易发性
  EASY: {
    4: '#F19E84',
    3: '#EFC66D',
    2: '#F5EAC7',
    1: '#E0FEA4',
  },
  // 危险性
  DANGER: {
    4: '#EA3323',
    3: '#E8B080',
    2: '#E6E64B',
    1: '#9BCB62',
  },
  // 风险性
  RISK: {
    4: '#A92218',
    3: '#E2AC87',
    2: '#F8F653',
    1: '#4F8B36',
  },
  // 易损性
  VULNERABILITY: {
    4: '#E83323',
    3: '#F1AE3D',
    2: '#B9DC48',
    1: '#58A631',
  },
}

export const DISASTER_FIELD_ENUM = [
  // { label: "风险程度", value: "riskExtent" },
  { label: '新鲜程度', value: 'freshEextent' },
  { label: '地灾阶段', value: 'dzStage' },
  { label: '灾害类型', value: 'disasterType' },
  { label: '图像类型', value: 'imageType' },
  { label: '承灾体类型', value: 'bearing_type' },
  { label: '变形迹象', value: 'deformation' },
]

export const DISASTER_FIELD_ENUM_ALL = [{ label: '风险程度', value: 'riskExtent' }, ...DISASTER_FIELD_ENUM]

export const POINT_TYPE = {
  // 风险点
  HAZARD_POINT: 'hazard_point',
  // 监测点
  MONITOR_POINT: 'monitor_point',
  // 警报点
  ALARM_POINT: 'alarm_point',
}

// 会议类型
export const MEETING_TYPE_MAP = {
  '': '全部类型',
  meeting: '会商邀请',
  'meeting-summary': '会商纪要',
}

export const getLevelText = (level) => {
  const map = {
    4: 'I',
    3: 'II',
    2: 'III',
    1: 'IV',
  }
  return map[level] || ''
}

export const RESPONSE_LIST_ID = '10000000'
