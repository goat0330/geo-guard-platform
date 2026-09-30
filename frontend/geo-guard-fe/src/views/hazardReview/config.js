/**
 * 隐患复核页面静态数据
 * 说明：后端接口（内网）暂不可访问，此处先以 Mock 数据驱动页面，
 *      接口就绪后只需把本文件导出的数据替换为 @/api 返回值即可。
 */

import iconSummarySource from '@/assets/imgs/hazardReview/icon-summary-source.png'
import iconSourceCommunity from '@/assets/imgs/hazardReview/icon-source-community.png'
import iconSourceInspection from '@/assets/imgs/hazardReview/icon-source-inspection.png'
import iconSourceOther from '@/assets/imgs/hazardReview/icon-source-other.png'
import iconStatusProcessed from '@/assets/imgs/hazardReview/icon-status-processed.png'
import iconStatusRate from '@/assets/imgs/hazardReview/icon-status-rate.png'
import iconStatusPending from '@/assets/imgs/hazardReview/icon-status-pending.png'

/** 统计日期 */
export const reviewDate = '2026-09-08'

/**
 * 总体概况 —— 累计接收风险源
 * items 为累计量的来源拆分，与 total 同口径
 */
export const sourceOverview = {
  total: 23113,
  caption: '含基层智治、大排查及其他来源',
  icon: iconSummarySource,
  items: [
    { key: 'community', label: '基层智治', value: 12486, icon: iconSourceCommunity },
    { key: 'inspection', label: '大排查', value: 9421, icon: iconSourceInspection },
    { key: 'other', label: '其他来源', value: 1206, icon: iconSourceOther },
  ],
}

/**
 * 复核状态 —— 智能体处理进度与异常占比
 * 处理率 rate 取百分比数值（58.4 表示 58.4%），同时驱动文案与进度环
 * anomalies 的占比分母与总体概况的累计接收风险源一致，由组件按 sourceOverview.total 换算
 */
export const reviewStatus = {
  processed: { key: 'processed', label: '已处理', value: 13503, icon: iconStatusProcessed },
  rate: { key: 'rate', label: '处理率', value: 58.4, icon: iconStatusRate },
  pending: { key: 'pending', label: '待复核', value: 301, icon: iconStatusPending },
  anomalies: [
    { key: 'abnormal', label: '数据异常', value: 84, color: '#E18282' },
    { key: 'duplicate', label: '疑似重复', value: 217, color: '#AEA8FC' },
  ],
}

/** 风险等级 —— 环形图与图例（A/B/C/D 为按潜在亡人数划分的等级） */
export const riskLevelShare = [
  { key: 'A', label: 'A（潜在亡人≥10人）', value: 23, color: '#E18282' },
  { key: 'B', label: 'B（3 人≤潜在亡人＜10人）', value: 543, color: '#F7AE81' },
  { key: 'C', label: 'C（1 人≤潜在亡人＜3 人）', value: 678, color: '#F7D777' },
  { key: 'D', label: 'D（无潜在亡人风险、造成财产损失）', value: 1235, color: '#81B6F7' },
]

/** 隐患类型 —— 分类柱状图，highlight 项使用绿色强调（非地灾） */
export const hazardTypeStats = [
  { key: 'rockfall', label: '危岩/崩塌', value: 1269 },
  { key: 'landslide', label: '滑坡', value: 862 },
  { key: 'debrisFlow', label: '泥石流', value: 22 },
  { key: 'groundCollapse', label: '地面塌陷', value: 50 },
  { key: 'groundSubsidence', label: '地面沉降', value: 49 },
  { key: 'nonGeological', label: '非地灾', value: 98, highlight: true },
]

/** 高风险乡镇 —— 按 A/B 类隐患点数量降序 */
export const highRiskTowns = [
  { key: 'shuanghe', name: '双河镇', value: 9 },
  { key: 'hongshan', name: '洪山镇', value: 7 },
  { key: 'zhongling', name: '中岭乡', value: 6 },
  { key: 'taihe', name: '太和镇', value: 4 },
  { key: 'yueshuihe', name: '越水河镇', value: 3 },
]

/** 地图点位弹窗详情（默认展示的隐患点） */
export const hazardDetail = {
  name: '郁山镇米阳坝村3组滑坡',
  address: '重庆市彭水苗族土家族自治县郁山镇',
  level: '中风险',
  threatPeople: '18人',
  threatProperty: '500万元',
  stabilityNow: '不稳定',
  stabilityForecast: '不稳定',
  code: 'CQ-PS-2026-0137',
  disasterTime: '2026-07-19 14:20',
}

/** 隐患处理列表 - 状态标签配色（小圆点 + 文字） */
export const processStatusMap = {
  待复核: { color: '#FF922C', background: '#FFF4E8' },
  待入库: { color: '#FF922C', background: '#FFF4E8' },
  已复核: { color: '#21A366', background: '#E8F8EF' },
  已入库: { color: '#21A366', background: '#E8F8EF' },
}

/** 隐患处理列表 - 筛选选项 */
export const processFilterOptions = {
  hazardTypes: ['滑坡', '崩塌', '泥石流', '地面塌陷'],
  statuses: ['待复核', '已复核', '待入库', '已入库'],
}

/** 隐患处理列表 - 滚动加载每批条数（接口就绪后作为分页 pageSize） */
export const processLoadSize = 4

/** 隐患处理列表（Mock，接口就绪后替换为 @/api 返回值；lng / lat 为地图定位坐标，同样来自接口） */
export const processList = [
  {
    id: 'hd-001',
    name: '马岩危岩',
    status: '待复核',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-08',
    lng: 108.1675,
    lat: 29.2942,
    result: { label: '变化', text: '规模、威胁财产', tone: 'warning' },
  },
  {
    id: 'hd-002',
    name: '新增调查点A',
    status: '待入库',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-08',
    lng: 108.1721,
    lat: 29.2884,
    result: { label: '处理结果', text: '待补充位置', tone: 'info' },
  },
  {
    id: 'hd-003',
    name: '马鞍溪滑坡',
    status: '已复核',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-07',
    lng: 108.1826,
    lat: 29.3011,
    result: { label: '处理结果', text: '复核无变化', tone: 'info' },
  },
  {
    id: 'hd-004',
    name: '调查点B',
    status: '待复核',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-07',
    lng: 108.1588,
    lat: 29.2773,
    result: { label: '变化', text: '分类分级建议', tone: 'warning' },
  },
  {
    id: 'hd-005',
    name: '调查点C',
    status: '已入库',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-06',
    lng: 108.1904,
    lat: 29.3095,
    result: { label: '处理结果', text: '已更新台账', tone: 'info' },
  },
  {
    id: 'hd-006',
    name: '新增调查点A',
    status: '待入库',
    location: '重庆市-彭水县-柳村-土地堂组',
    hazardType: '崩塌',
    date: '2026-09-06',
    lng: 108.1743,
    lat: 29.2831,
    result: null,
  },
]

/** 智能体处理面板 - 地点与资料来源摘要（Mock，接口就绪后替换为 @/api 返回值） */
export const agentProcessBrief = {
  relation: '已有复核点 · 联合乡-回联村-5组',
  materials: '调查表1份 · 现场图片未提供',
  summary: '已整理调查资料并比对比历史台账，分类分级结果待正式标准校验。',
  /** 底部状态提示：text 常规文案，highlight 用主色强调 */
  footer: {
    text: '当前未更新台账，',
    highlight: '待人工确认。',
  },
}

/** 智能体处理面板 - 分类分级结果表（Mock） */
export const agentClassifyTable = {
  columns: ['分类项', '当前结果', '依据'],
  rows: [
    ['灾害类型', '崩塌', '现场调查'],
    ['稳定性', '差', '现场调查'],
    ['规模等级', '特大型', '原台账'],
    ['险情等级', '大型', '原台账'],
    ['防治等级', '一级', '原台账'],
  ],
}

/** 智能体处理面板 - 威胁指标（Mock，底板依次对应 icon-flfjjg-bg1~4） */
export const agentThreatMetrics = [
  { label: '威胁总人数', value: '907', unit: '人' },
  { label: '威胁总资产', value: '6369', unit: '万元' },
  { label: '面积', value: '0.5', unit: '万m²' },
  { label: '体积', value: '2652', unit: '万m³' },
]

/** 分类分级依据缺省文案：详情接口未返回 classificationBasis 时展示的当前进度说明 */
export const DEFAULT_CLASSIFICATION_BASIS = '百日攻坚分类标准待印发，接入后逐项匹配条款，完成分级校验。'

/** 智能体处理面板 - 处理步骤（Mock） */
export const agentProcessSteps = [
  {
    key: 'materials',
    title: '已读资料并对比台账',
    points: ['已关联历史台账，发现规模、威胁财产变化；无图片时依据调查表继续处理'],
  },
  {
    key: 'classify',
    title: '已提取分类结果',
    subtitle: '当前分类结果：',
    classify: agentClassifyTable,
    metrics: agentThreatMetrics,
  },
  {
    key: 'standard',
    title: '分类分级依据待接入',
    points: [DEFAULT_CLASSIFICATION_BASIS],
  },
  {
    key: 'draft',
    title: '已生成复核建议草稿',
    draftNote: '已整理崩塌类隐患信息，建议核实现规模与威胁财产变化，完成标准校验后提交人工确认。',
  },
]
