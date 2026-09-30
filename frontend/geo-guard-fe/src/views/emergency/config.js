import iconHazardRed from '@/assets/imgs/emergency/icon-hazard-red.png'
import iconHazardOrange from '@/assets/imgs/emergency/icon-hazard-orange.png'
import iconStatHouse from '@/assets/imgs/emergency/icon-stat-house.svg'
import iconStatPeople from '@/assets/imgs/emergency/icon-stat-people.svg'
import iconStatAlert from '@/assets/imgs/emergency/icon-stat-alert.svg'

/** 智能预案 - 页签（隐患点 / 灾险情）
 *  keywordLabel：该页签下关键字检索框的标签名
 *  scene：对应后端生成/查询路线的场景枚举（PLAN_SCENE） */
export const planTabs = [
  { key: 'update', label: '已有预案更新', keywordLabel: '隐患点名称', scene: 'EXISTING_HAZARD' },
  { key: 'generate', label: '突发灾情预案生成', keywordLabel: '灾险情名称', scene: 'EMERGENCY_EVENT' },
]

/** 智能预案 - 筛选区（展开态）处理状态选项，按页签分别下发
 *  已有隐患点对应 handleStatus 枚举，突发灾险情对应 status 枚举（0/1/2） */
export const planFilterOptions = {
  hazardStatuses: [
    { label: '待入库', value: 'PENDING_ARCHIVE' },
    { label: '待复核', value: 'PENDING_REVIEW' },
    { label: '已入库', value: 'ARCHIVED' },
    { label: '已复核', value: 'REVIEWED' },
  ],
  emergencyStatuses: [
    { label: '待勾划范围', value: 0 },
    { label: '更新中', value: 1 },
    { label: '已更新', value: 2 },
    { label: '更新失败', value: 3 },
  ],
}

/** 智能预案 - 已有隐患点的 AI 处理状态文案（与后端 aiProcessStatus 一致：0待更新、1更新中、2已更新、3更新失败）
 *  列表直接返回 aiProcessStatusName，本表用于该字段缺失时的兜底展示 */
export const hazardAiStatusMap = {
  0: '待更新',
  1: '更新中',
  2: '已更新',
  3: '更新失败',
}

/** 智能预案 - 突发灾险情状态文案（与后端 status 一致），statusName 缺失时兜底 */
export const emergencyStatusMap = {
  0: '待勾划范围',
  1: '更新中',
  2: '已更新',
  3: '更新失败',
}

/** 智能预案 - 灾害类型图标（接口返回的 typeCode 为「滑坡」「崩塌」等中文编码） */
export const hazardIconMap = {
  崩塌: iconHazardRed,
  滑坡: iconHazardOrange,
}

/** 智能预案 - 状态标签配色（小胶囊 + 图标 + 文字） */
export const planStatusMap = {
  已更新: { color: '#21A366', background: '#E8F8EF' },
  更新中: { color: '#007BFF', background: '#EAF2FE' },
  待勾划范围: { color: '#FF922C', background: '#FFF4E8' },
  已核验: { color: '#21A366', background: '#E8F8EF' },
  较昨日减少5项: { color: '#DD4739', background: '#FEE4E2' },
  待更新: { color: '#9096A2', background: '#F2F5F9' },
  待入库: { color: '#FF922C', background: '#FFF4E8' },
  待复核: { color: '#007BFF', background: '#EAF2FE' },
  已入库: { color: '#21A366', background: '#E8F8EF' },
  已复核: { color: '#21A366', background: '#E8F8EF' },
  更新失败: { color: '#DD4739', background: '#FEE4E2' },
}

/** 智能预案 - 已有预案更新列表（Mock，接口就绪后替换为 @/api 返回值）
 *  已更新：本次更新完成，展示「已更新N项」；更新中：展示执行进度
 *  卡片操作按钮默认「查看更新结果 + 查看分析过程」 */
export const planList = [
  {
    id: 'plan-001',
    name: '示例村四组崩塌',
    status: '已更新',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    updateTime: '2026-09-07 10:26',
    updatedItems: ['威胁信息', '威胁范围', '活动历史'],
  },
  {
    id: 'plan-002',
    name: '示例村四组滑坡',
    status: '更新中',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    updateTime: '2026-09-07 10:26',
    progress: 90,
  },
  {
    id: 'plan-003',
    name: '示例村四组滑坡',
    status: '更新中',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    updateTime: '2026-09-07 10:26',
    progress: 65,
  },
  {
    id: 'plan-004',
    name: '示例村四组滑坡',
    status: '已更新',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    updateTime: '2026-09-07 10:26',
    updatedItems: ['威胁信息', '威胁范围', '活动历史'],
  },
  {
    id: 'plan-005',
    name: '示例村四组崩塌',
    status: '更新中',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    updateTime: '2026-09-07 10:26',
    progress: 80,
  },
  {
    id: 'plan-006',
    name: '示例村四组滑坡',
    status: '已更新',
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    updateTime: '2026-09-07 10:26',
    updatedItems: ['威胁信息', '威胁范围', '活动历史'],
  },
]

/** 智能预案 - 突发灾情预案生成列表（Mock，接口就绪后替换为 @/api 返回值）
 *  三种状态：待勾划范围（去勾划范围）/ 更新中（查看分析过程）/ 已更新（查看更新结果 + 查看分析过程）
 *  第三行 meta 为「发生时间」，卡片不带底部状态行 */
export const planListGenerated = [
  {
    id: 'gen-001',
    name: '示例村四组崩塌',
    status: '待勾划范围',
    actions: ['range'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-002',
    name: '示例村四组滑坡',
    status: '更新中',
    actions: ['analysis'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-003',
    name: '示例村四组崩塌',
    status: '已更新',
    actions: ['result', 'analysis'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-004',
    name: '示例村四组滑坡',
    status: '待勾划范围',
    actions: ['range'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-005',
    name: '示例村四组崩塌',
    status: '更新中',
    actions: ['analysis'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-006',
    name: '示例村四组滑坡',
    status: '已更新',
    actions: ['result', 'analysis'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-007',
    name: '示例村四组崩塌',
    status: '待勾划范围',
    actions: ['range'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '崩塌',
    hazardIcon: iconHazardRed,
    occurTime: '2026-09-07 10:26',
  },
  {
    id: 'gen-008',
    name: '示例村四组滑坡',
    status: '更新中',
    actions: ['analysis'],
    location: '重庆市-彭水县-柳池村-龙溪镇',
    hazardType: '滑坡',
    hazardIcon: iconHazardOrange,
    occurTime: '2026-09-07 10:26',
  },
]

/** 地图点位详情弹窗 - 已有预案更新（Mock，接口就绪后按选中预案请求） */
export const emergencyPointDetail = {
  name: '重庆市舞阳坝窑湾社区老鹰坝组危岩体崩塌',
  level: '中型崩塌',
  /** 四项指标：一行四列展示 */
  metrics: [
    { label: '威胁总人数', value: '18人' },
    { label: '威胁总资产', value: '500万元' },
    { label: '稳定性现状', value: '不稳定' },
    { label: '稳定性趋势', value: '不稳定' },
  ],
  /** 信息行：两列排布；tag 为带图标的胶囊标签 */
  info: [
    { label: '隐患点编号：', value: '422801020416' },
    { label: '曾发生灾害时间：', value: '2025-09-14' },
    { label: '更新状态：', value: '已更新', tag: 'success' },
    { label: '更新时间：', value: '2026-09-10 10:23' },
  ],
  /** 底部入口：文案 + 触发事件名 */
  links: [{ key: 'result', label: '查看更新结果', event: 'view-result' }],
}

/** 地图点位详情弹窗 - 突发灾情预案生成（Mock，无指标卡，仅状态 + 更新时间 + 双入口） */
export const generatedPointDetail = {
  name: '示例组一组',
  level: '已更新',
  updateTime: '2026-09-10 10:23',
  links: [
    { key: 'result', label: '查看更新结果', event: 'view-result' },
    { key: 'analysis', label: '查看分析过程', event: 'view-analysis' },
  ],
}

/** 去勾划范围：地图绘制工具条按钮 */
export const drawTools = [
  { key: 'draw', label: '绘制' },
  { key: 'undo', label: '撤销' },
  { key: 'remove', label: '删除' },
  { key: 'exit', label: '退出' },
]

/** 勾划范围确认弹窗（Mock：确认后可获取的资料项，接口就绪后由后端返回） */
export const rangeConfirm = {
  title: '确认本次资料检索范围',
  /** 地图上范围浮动标签的文案前缀 */
  label: '本次资料检索范围',
  areaLabel: '勾划面积',
  /** 确认后可获取的资料项 */
  tags: [
    '一标三实',
    '路网',
    '避险安置点',
    '地质信息',
    '雨量',
    '隐患点',
    '今日风险评价',
    '行政区划与责任体系',
  ],
  /** 底部按钮文案 */
  actions: {
    cancel: '取消',
    redraw: '重绘范围',
    confirm: '确认范围并生成',
  },
}

/** 执行过程（Mock：右侧「执行过程」面板内容，接口就绪后按预案 id 请求）
 *  每步小结按「前缀 + 片段数组」组织，片段 tone: 'warn' 为需人工跟进的橙色提示 */
/** 查看更新结果：执行结果弹窗（Mock，接口就绪后按预案 id 请求） */
export const executeResult = {
  title: '钟家湾东崩塌执行结果',
  /** 智能体执行状态卡 */
  agent: {
    name: '预案更新智能体',
    desc: '正在调用预案更新能力',
  },
  /** 参数识别 */
  params: {
    label: '参数识别',
    name: '钟家湾东崩塌',
    status: '已有隐患点',
    fields: [
      { key: 'location', label: '位置', value: '重庆市彭水县龙村5组316国道内侧' },
      { key: 'hazard', label: '隐患类型', value: '崩塌' },
      { key: 'source', label: '触发来源', value: '隐患复核结果' },
      { key: 'time', label: '更新时间', value: '2016-09-10 16:20' },
    ],
  },
  /** 生成中提示（接口就绪后按真实进度展示） */
  loading: {
    text: '正在汇聚最新威胁信息、监测记录与人员数据...',
    tip: '已完成2/6个步骤，生成过程中可查看左侧执行详情',
  },
  /** 生成完成 */
  done: {
    title: '智能预案已更新完成',
    desc: '综上所述，动态预案智能体已为您准备好了更新后的预案：',
    panels: [
      {
        index: '01',
        title: '预案更新稿',
        desc: '自动汇聚基础信息、应急安排与属地责任体系',
        items: [
          { label: '威胁户数', from: '18户', to: '12户', icon: iconStatHouse },
          { label: '威胁人数', from: '52人', to: '36人', icon: iconStatPeople },
          { label: '隐患变化', to: '新增坡脚落石记录', highlight: true, icon: iconStatAlert },
        ],
        link: '查看预案详情',
      },
      {
        index: '02',
        title: '最新撤离路线',
        desc: '结合路网、风险评价及安置点形成候选路线',
        link: '查看路线详情',
        /** 路线示意图待设计提供，当前以占位块 + 图例展示 */
        caption: 'XX人XX条路线',
      },
    ],
    notice: '1项内容待专业核实',
    filesTitle: '结果文件：',
    files: [
      { name: '钟家湾东崩塌防灾预案(V3.0).docx', type: 'word', size: '4.5MB' },
      { name: '钟家湾东崩塌撤离路线图.png', type: 'word', size: '4.5MB' },
    ],
    footerTip: '可修改预案内容，保留本次生成版本',
    actions: { feedback: '提交反馈', confirm: '确认采用' },
  },
}

export const executeProcess = {
  title: '执行过程',
  intro: '隐患复核结果已确认，智能体已自动匹配原预案并形成更新稿。',
  steps: [
    {
      key: 'receive',
      title: '已接收隐患复核结果',
      label: '本步结论：',
      summary: [
        { text: '已接收“示例村一组崩塌”复核结果，识别到威胁户数人口及落石活动发生变化。' },
      ],
    },
    {
      key: 'match',
      title: '已匹配已有隐患点与原预案',
      label: '本步结论：',
      summary: [
        {
          text: '已匹配该隐患点现有台账，位于示例乡镇示例村一组；隐患体长约 38米、宽约 16米、高约 21米。已获取原预案 V1.0，进入更新流程。',
        },
      ],
    },
    {
      key: 'basis',
      title: '已汇算变化依据',
      label: '本步结论：',
      summary: [
        {
          text: '经搬迁台账核验，原威胁 8户22人中有 3户8人完成搬迁，现威胁 5户14人；巡查新增 1处落石记录，',
        },
        { text: '待专业核实', tone: 'warn' },
      ],
    },
    {
      key: 'plan',
      title: '已更新结构化预案',
      label: '本步结论：',
      summary: [
        {
          text: '已将威胁户数由 8户更新为 5户、威胁人口由 22人更新为 14人，并补充落石活动记录；险情等级与稳定性结论暂沿用原值。',
        },
      ],
    },
    {
      key: 'route',
      title: '已生成最新撤离路线规划',
      label: '本步结论：',
      summary: [
        {
          text: '已结合最新威胁范围、村组路网、避险点及安置点，生成 1条主撤离路线和 1条备用路线，主路线已避开落石影响区；同步匹配群测群防员、地质工程师、副乡镇长和区县地环站 4类责任人员，路线与任务待确认后更新至后台数据库。',
        },
      ],
    },
  ],
}

/** 撤离路线演示数据，字段保持与 geo-guard-ge 撤离方案接口一致。 */
export const evacuationRouteData = {
  disasterPolygonsWktList: [
    'POLYGON((108.2501 29.4024,108.2525 29.4024,108.2525 29.4040,108.2501 29.4040,108.2501 29.4024))',
    'POLYGON((108.2476 29.3982,108.2500 29.3982,108.2500 29.3998,108.2476 29.3998,108.2476 29.3982))',
  ],
  resettlementAreas: [
    {
      name: '临时安置点A（村委会）',
      areaWkt: 'POLYGON((108.2562 29.4062,108.2572 29.4062,108.2572 29.4070,108.2562 29.4070,108.2562 29.4062))',
    },
    {
      name: '临时安置点B（村小学）',
      areaWkt: 'POLYGON((108.2420 29.3954,108.2430 29.3954,108.2430 29.3962,108.2420 29.3962,108.2420 29.3954))',
    },
  ],
  routes: [
    {
      id: 'route-1',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 2, childrenCount: 1 },
      distanceKm: 0.85,
      estimatedTimeMinutes: 15,
      evacuationDirection: '沿村道向北直行，经岔路口后右转继续直行200米到达安置点。',
      evacuationAreaToRoad: 'LINESTRING(108.2531 29.4040,108.2534 29.4044)',
      evacuationRoad:
        'LINESTRING(108.2534 29.4044,108.2537 29.4050,108.2536 29.4058,108.2542 29.4065,108.2552 29.4067,108.2560 29.4065)',
      roadToResettlement: 'LINESTRING(108.2560 29.4065,108.2566 29.4066)',
    },
    {
      id: 'route-2',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 1, childrenCount: 2 },
      distanceKm: 0.92,
      estimatedTimeMinutes: 18,
      evacuationDirection: '沿居民点东侧便道向北撤离，绕开危险区后接入村道，向东到达临时安置点A。',
      evacuationAreaToRoad: 'LINESTRING(108.2531 29.4038,108.2538 29.4040)',
      evacuationRoad:
        'LINESTRING(108.2538 29.4040,108.2542 29.4047,108.2545 29.4053,108.2550 29.4059,108.2558 29.4062)',
      roadToResettlement: 'LINESTRING(108.2558 29.4062,108.2566 29.4066)',
    },
    {
      id: 'route-3',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 3, childrenCount: 0 },
      distanceKm: 1.1,
      estimatedTimeMinutes: 22,
      evacuationDirection: '从居民点南侧道路向西撤离，经安全通道折向北侧村道，再前往村委会临时安置点。',
      evacuationAreaToRoad: 'LINESTRING(108.2452 29.3958,108.2457 29.3962)',
      evacuationRoad:
        'LINESTRING(108.2457 29.3962,108.2463 29.3960,108.2467 29.3964,108.2467 29.3971,108.2475 29.3972)',
      roadToResettlement: 'LINESTRING(108.2475 29.3972,108.2480 29.3976)',
    },
    {
      id: 'route-4',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 2, childrenCount: 1 },
      distanceKm: 1.3,
      estimatedTimeMinutes: 25,
      evacuationDirection: '沿西侧机耕道向北，避开崩塌影响区后进入安全通道，按现场指引前往安置点。',
      evacuationAreaToRoad: 'LINESTRING(108.2424 29.3990,108.2430 29.3992)',
      evacuationRoad:
        'LINESTRING(108.2430 29.3992,108.2438 29.3986,108.2446 29.3988,108.2452 29.3995,108.2462 29.3997)',
      roadToResettlement: 'LINESTRING(108.2462 29.3997,108.2468 29.4001)',
    },
    {
      id: 'route-5',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 1, childrenCount: 1 },
      distanceKm: 1.5,
      estimatedTimeMinutes: 28,
      evacuationDirection: '由村道向西北方向疏散，沿风险区外缘通行，再转入通往村委会的安全道路。',
      evacuationAreaToRoad: 'LINESTRING(108.2440 29.4010,108.2445 29.4012)',
      evacuationRoad:
        'LINESTRING(108.2445 29.4012,108.2451 29.4017,108.2458 29.4020,108.2464 29.4025,108.2471 29.4028)',
      roadToResettlement: 'LINESTRING(108.2471 29.4028,108.2476 29.4032)',
    },
    {
      id: 'route-6',
      evacuationArea: '村委会旁居民点',
      resettlementPoint: '临时安置点A（村委会）',
      residentsInfo: { totalCount: 12, elderlyCount: 2, childrenCount: 2 },
      distanceKm: 1.6,
      estimatedTimeMinutes: 30,
      evacuationDirection: '从居民点西侧出口撤离，沿乡道外侧向北通行，全程避开红色危险区域。',
      evacuationAreaToRoad: 'LINESTRING(108.2483 29.4010,108.2488 29.4013)',
      evacuationRoad:
        'LINESTRING(108.2488 29.4013,108.2493 29.4018,108.2498 29.4022,108.2503 29.4027,108.2508 29.4033)',
      roadToResettlement: 'LINESTRING(108.2508 29.4033,108.2512 29.4038)',
    },
  ],
}

/** 数值带单位展示：null/undefined/空串统一回落 '--'（接口未返回时不伪造数值） */
const formatCount = (value, unit) => {
  const num = Number(value)
  return value === null || value === undefined || value === '' || !Number.isFinite(num) ? '--' : `${num}${unit}`
}

/**
 * 行政归属：突发灾险情列表已删除 administrativeArea，改为分级行政区字段，
 * 按省、市、区县、乡镇、村顺序拼接；案件详情仍直接返回 administrativeArea，故优先取该字段
 * @param {object} source 来源信息（eventBasicInfo / hazardPoint / emergencyEvent / 列表行）
 * @returns {string} 拼接结果，字段全空时返回空串
 */
export const formatAdminArea = (source = {}) =>
  source.administrativeArea ||
  [source.province, source.city, source.county, source.street, source.village].filter(Boolean).join('')

/** 隐患点类型英文编码 → 中文名：/route/latest 的 hazardPoint 仅返回 typeCode 时兜底展示 */
export const hazardTypeTextMap = {
  UNSTABLE_SLOPE: '不稳定斜坡',
  LANDSLIDE: '滑坡',
  COLLAPSE: '崩塌',
  DEBRIS_FLOW: '泥石流',
  GROUND_FISSURE: '地裂缝',
  GROUND_SUBSIDENCE: '地面塌陷',
}

/** 空路线结果的稳定引用：结果卡预览地图以此判断数据引用，未变化时不重建地图、不触发重复截图 */
export const EMPTY_ROUTE_RESULT = Object.freeze({ disasterPolygonsWktList: [], resettlementAreas: [], routes: [] })

/**
 * 用 GET /route/latest 的真实返回体构建「执行结果」大弹窗数据
 * @param {object} options
 * @param {boolean} options.isHazard true 已有隐患点 / false 突发灾险情
 * @param {object} options.source 来源信息：hazardPoint 或 emergencyEvent（缺失时用列表行兜底）
 * @param {object|null} options.routeResult 撤离路线结果，从未生成过时为 null
 * @param {string} options.area 勾划面积文案（突发灾险情由前端按绘制范围计算）
 */
export const buildExecuteResult = ({ isHazard, source = {}, routeResult = null, area = '' } = {}) => {
  const routes = Array.isArray(routeResult?.routes) ? routeResult.routes : []
  const name = isHazard ? source.name : source.eventName
  const time = isHazard ? source.updatedTime || source.createdTime : source.updateTime || source.occurTime
  /* 位置：location 缺失时按行政区划拼接（hazardPoint 不返回 location 字段） */
  const location = source.location ||
    [source.province, source.city, source.county, source.street, source.village].filter(Boolean).join('') || '--'
  /* 隐患类型：typeName 缺失时按常见英文编码转中文（如 UNSTABLE_SLOPE），未识别的编码原样展示 */
  const hazardType = source.typeName || hazardTypeTextMap[source.typeCode] || source.typeCode || '--'

  /* 人员统计按路线汇总：算法跳过承灾点时只返回成功路线，故以实际返回为准 */
  const sumResidents = (key) => routes.reduce((sum, route) => sum + (Number(route?.residentsInfo?.[key]) || 0), 0)
  const totalPeople = sumResidents('totalCount')

  return {
    title: name ? `${name}执行结果` : '智能预案执行结果',
    agent: isHazard
      ? { name: '预案更新智能体', desc: '正在调用预案更新能力' }
      : { name: '突发险情预案生成智能体', desc: '正在调用突发险情预案生成能力' },
    params: {
      label: '参数识别',
      name: name || '--',
      status: isHazard ? '已有隐患点' : '突发灾险情',
      fields: isHazard
        ? [
            { key: 'location', label: '位置', value: location },
            { key: 'hazard', label: '隐患类型', value: hazardType },
            { key: 'people', label: '威胁人数', value: formatCount(source.threatenedPopulation, '人') },
            { key: 'time', label: '更新时间', value: time || '--' },
          ]
        : [
            { key: 'code', label: '编号', value: source.eventCode || '--' },
            { key: 'area', label: '勾划面积', value: area || '--' },
            { key: 'owner', label: '行政归属', value: formatAdminArea(source) || '--' },
            { key: 'time', label: '生成时间', value: time || '--' },
          ],
    },
    loading: {
      text: isHazard
        ? '正在汇聚最新威胁信息、监测记录与人员数据...'
        : '正在汇算勾划范围内的基础数据与责任体系...',
      tip: '生成过程中可查看右侧执行详情',
    },
    done: {
      title: '智能预案已更新完成',
      desc: isHazard
        ? '综上所述，动态预案智能体已为您准备好了更新后的预案：'
        : '已根据手动勾划范围，为您生成本次突发险情的预案草案：',
      panels: [
        {
          index: '01',
          title: isHazard ? '预案更新稿' : '结构化应急预案',
          desc: '自动汇聚基础信息、应急安排与属地责任体系',
          /* 两种卡片格式：已有隐患点为「原值 → 新值」更新对比；突发灾险情为单值汇总。
             威胁户数、隐患变化、责任体系暂无接口字段，先占位 '--'，接口就绪后替换 */
          items: isHazard
            ? [
                { label: '威胁户数', from: '--', to: '--', icon: iconStatHouse },
                {
                  label: '威胁人数',
                  from: formatCount(source.threatenedPopulation, '人'),
                  to: routes.length ? `${totalPeople}人` : '--',
                  icon: iconStatPeople,
                },
                { label: '隐患变化', to: '--', highlight: true, icon: iconStatAlert },
              ]
            : [
                { label: '范围内核查对象', to: routes.length ? `${totalPeople}人` : '--', icon: iconStatPeople },
                { label: '匹配责任体系', to: '--', icon: iconStatAlert },
                { label: '编制依据', to: `${rangeConfirm.tags.length}类数据`, icon: iconStatHouse },
              ],
          /* 预案详情入口：点击后打开预案详情查看弹窗（详情接口开发中，先展示列表行数据） */
          link: '查看预案详情',
        },
        {
          index: '02',
          title: '最新撤离路线',
          desc: '结合路网、风险评价及安置点形成候选路线',
          link: routes.length ? '查看路线详情' : '',
          caption: routes.length ? `${totalPeople}人${routes.length}条路线` : '暂未生成可用撤离路线',
          /* 预览地图用真实路线数据；未生成时用稳定空引用，避免每次重建数据导致地图反复重建 */
          routeData: routeResult || EMPTY_ROUTE_RESULT,
        },
      ],
      notice: '',
      /* 结果文件固定两项：预案文档 + 撤离路线示意图。
         示意图由 RouteSnapshotMap 用离屏大图截图生成，出图后回填 src 与体积（见 index.vue 的 handleRouteSnapshot） */
      filesTitle: '结果文件：',
      files: [
        {
          name: `${name || '智能预案'}预案(V0.1).docx`,
          type: 'word',
          /* 体积由下载接口返回后补充，未知时不编造 */
          size: '',
          event: 'download-plan-doc',
        },
        {
          name: `${name || '预案'}撤离路线图.png`,
          type: '图片附件',
          /* src 与体积在离屏截图出图后回填，未出图时文件卡只展示类型图标 */
          size: '',
        },
      ],
      footerTip: '可修改预案内容，保留本次生成版本',
      actions: { feedback: '提交反馈', confirm: '确认采用' },
    },
  }
}

/**
 * 构建「预案详情」弹窗的临时处置预案卡（结构对应设计稿「临时处置预案卡」）
 * 数据取自列表行与撤离路线结果，接口未返回的字段统一展示「待核实 / --」，不编造内容
 * @param {object} options
 * @param {boolean} options.isHazard true 已有隐患点 / false 突发灾险情
 * @param {object} options.source 来源信息：hazardPoint 或 emergencyEvent
 * @param {object|null} options.routeResult 撤离路线结果
 * @param {string} options.area 勾划面积文案
 * @param {string} options.image 撤离路线示意图（截取自结果弹窗内的路线地图）
 */
export const buildPlanDetailCard = ({
  isHazard = true,
  source = {},
  routeResult = null,
  area = '',
  image = '',
} = {}) => {
  const routes = Array.isArray(routeResult?.routes) ? routeResult.routes : []
  const resettlementAreas = Array.isArray(routeResult?.resettlementAreas) ? routeResult.resettlementAreas : []
  const name = (isHazard ? source.name : source.eventName) || '--'
  const time = (isHazard ? source.updatedTime || source.createdTime : source.updateTime || source.occurTime) || '--'
  const adminArea = formatAdminArea(source)
  const location = source.location || adminArea || '--'
  const hazardType = source.typeName || hazardTypeTextMap[source.typeCode] || source.typeCode || '--'
  const lng = Number(source.longitude)
  const lat = Number(source.latitude)
  const coordinate = Number.isFinite(lng) && Number.isFinite(lat) ? `${lng.toFixed(6)}°，${lat.toFixed(6)}°` : '--'
  const rangeText = isHazard ? '待现场核定' : area || '待现场核定'
  const sumResidents = (key) => routes.reduce((sum, route) => sum + (Number(route?.residentsInfo?.[key]) || 0), 0)
  const hasRoute = routes.length > 0
  const peopleText = hasRoute ? `${sumResidents('totalCount')}人` : '--'
  const focusText = hasRoute ? `老人${sumResidents('elderlyCount')}人 / 儿童${sumResidents('childrenCount')}人` : '--'
  const statusText = source.statusName || source.status || '待现场核实'
  const settlementName = resettlementAreas[0]?.name || resettlementAreas[0]?.resettlementPoint || '--'

  return {
    fileName: name === '--' ? '临时处置预案卡' : `${name}临时处置预案卡`,
    title: name === '--' ? '临时处置预案卡' : `“${name}”临时处置预案卡`,
    subtitle: '仅用于现场核实前的先期处置 · 表中数据取自接口返回，未返回项标注待核实',
    stage: '现场核实前 · 不作为正式隐患认定、分类分级或入库依据',
    /* 撤离路线示意图：截取自执行结果弹窗内的路线地图，未生成时不展示图片 */
    image,
    sections: [
      {
        title: '事件基本信息',
        note: '名称、时间、地点与范围取自列表与勾画结果',
        rows: [
          { label: '事件名称', value: name, span: 6, mark: { text: '列表数据', type: 'db' } },
          { label: '预案编号', value: source.eventCode || '--', span: 2, center: true },
          { label: isHazard ? '更新时间' : '发生时间', value: time, span: 4, center: true },
          { label: '当前状态', value: statusText, span: 4, center: true, type: 'pending' },
          { label: '发生地点', value: location, span: 6 },
          { label: '中心坐标', value: coordinate, span: 2, center: true, mark: { text: '定位', type: 'gis' } },
          { label: '事件范围', value: rangeText, span: 6 },
          { label: '所属行政区', value: adminArea || location, span: 4 },
        ],
      },
      {
        title: '快速态势',
        note: '按事件范围自动叠加，均需现场复核',
        rows: [
          { label: isHazard ? '隐患类型' : '疑似灾险情类型', value: hazardType, span: 2, center: true },
          { label: '灾害规模', value: source.scaleGrade || '待现场测定', span: 2, center: true, type: 'pending' },
          { label: 'A/B/C/D分类', value: '暂不判定', span: 2, center: true, type: 'pending' },
          { label: '风险等级', value: source.riskGrade || '暂不判定', span: 2, center: true, type: 'pending' },
          {
            label: '威胁人数',
            value: formatCount(source.threatenedPopulation, '人'),
            span: 2,
            center: true,
          },
          { label: '先期处置原则', value: '从严核查、提前避险', span: 2, center: true, type: 'warning' },
          { label: '范围内居民', value: peopleText, span: 2, center: true },
          { label: '重点关注人员', value: focusText, span: 2, center: true },
          { label: '涉及道路', value: '待接口返回', span: 2, center: true, type: 'pending' },
          { label: '周边风险源', value: '待接口返回', span: 4, type: 'pending' },
          { label: '周边敏感目标', value: '待接口返回', span: 4, type: 'pending' },
          { label: '气象与监测摘要', value: '监测与气象摘要待接口返回', span: 10, type: 'long pending' },
          {
            label: '临时风险提示',
            value: '范围内人数是空间叠加结果，不等于潜在亡人人数；现场情况核实前不套用分类分级结论。',
            span: 10,
            type: 'long warning',
            mark: { text: '规则校验', type: 'ai' },
          },
        ],
      },
      {
        title: '先期处置安排',
        note: '按标准处置流程编排，可随现场反馈动态更新',
        rows: [
          {
            label: '分阶段行动',
            span: 10,
            mark: { text: '流程编排', type: 'ai' },
            steps: [
              { title: '0—15分钟', text: '联系属地核实，提醒范围内居民做好撤离准备，临时管控临坡道路。' },
              { title: '15—30分钟', text: '逐户清点人员，优先转移重点关注人员，核验安置点和路线。' },
              { title: '30—60分钟', text: '专业人员到场研判，确认灾种、范围和等级，更新临时预案版本。' },
            ],
          },
          {
            label: '临时管控范围',
            value: '以当前勾画范围为先期警戒参考；具体警戒边界由现场指挥人员根据地形、变形迹象和专家意见确定。',
            span: 10,
            type: 'long pending',
          },
          {
            label: '人员避险建议',
            value: '优先组织疑似影响范围内居民和重点关注人员转移；无法立即确认危险边界时，采用扩大避让、人员先撤的方式处置。',
            span: 10,
            type: 'long',
          },
          {
            label: '现场核查重点',
            value: '核实是否出现裂缝、掉块、房屋变形、地面隆起或持续位移；确认实际受威胁户数人数、道路通行、安置资源可用性并上传现场图片。',
            span: 10,
            type: 'long',
          },
        ],
      },
      {
        title: '临时撤离与资源调度',
        note: '均为候选方案，使用前必须核实',
        rows: [
          { label: '候选安置点', value: settlementName, span: 4, mark: { text: '资源匹配', type: 'gis' } },
          { label: '当前可用性', value: '待电话核实', span: 4, center: true, type: 'pending' },
          {
            label: '候选撤离路线',
            span: 10,
            mark: { text: '道路网计算', type: 'gis' },
            routes: routes.length
              ? routes.map((route, index) => ({
                  plan: index === 0 ? '主路线' : `备选路线${index}`,
                  /* 路径优先用算法返回的完整导航文本，缺失时回落撤离方向，再缺省才用起止点拼接 */
                  path:
                    route.navigationText ||
                    route.evacuationDirection ||
                    `${route.evacuationArea || '--'} → ${route.resettlementPoint || '--'}`,
                  distance: route.distanceKm ? `${route.distanceKm}千米` : '--',
                  duration: route.estimatedTimeMinutes ? `${route.estimatedTimeMinutes}分钟` : '--',
                }))
              : [{ plan: '--', path: '暂未生成可用撤离路线', distance: '--', duration: '--' }],
          },
          {
            label: '撤离路线示意图',
            span: 10,
            /* 图片取执行结果弹窗内路线地图的截图，未生成时给出说明而不是占位空图 */
            image,
            imageEmpty: '路线示意图生成中，请先生成撤离路线',
          },
          {
            label: '路线风险提示',
            value: '候选路线由路网计算得出，强降雨、塌方等条件下通行性需由现场人员确认后再组织转移。',
            span: 10,
            type: 'long warning',
          },
          { label: '预警与联络方式', value: '电话、广播、敲锣、逐户通知', span: 4 },
          { label: '候选医疗资源', value: '待接口返回', span: 4, type: 'pending' },
        ],
      },
      {
        title: '现场责任人与任务',
        note: '姓名、电话由后端按行政区划责任体系回填',
        rows: [
          {
            label: '责任分工',
            span: 10,
            duties: [
              { role: '村级责任人', name: '待接口回填', phone: '--', task: '组织村级力量；设置警戒、集合清点，并向乡镇报告现场情况。' },
              { role: '群测群防员', name: '待接口回填', phone: '--', task: '巡查裂缝、掉块等异常迹象；持续监测并第一时间上报现场变化。' },
              { role: '网格员', name: '待接口回填', phone: '--', task: '逐户通知并清点人数；协助老弱病残等重点人员优先撤离。' },
              { role: '驻守地质工程师', name: '待接口回填', phone: '--', task: '划定危险区域；研判风险并给出撤离、监测和排险建议。' },
            ],
          },
        ],
      },
      {
        title: '待核实与版本信息',
        rows: [
          {
            label: '必须补充的信息',
            value: '①实际灾险情类型与变形迹象；②实际受威胁户数、人数；③现场危险边界；④撤离路线通行条件；⑤安置点可用性与容量；⑥现场责任人员及联系电话。',
            span: 10,
            type: 'long pending',
          },
          { label: '当前版本', value: 'V0.1', span: 2, center: true },
          { label: '生成时间', value: time, span: 2, center: true },
          { label: '有效状态', value: '至现场反馈或新版本生成', span: 2, center: true },
          {
            label: '下一版本触发',
            value: '收到现场图片、调查结果、人员清点、路线核查或专家研判中的任一新增信息后，重新生成临时预案卡并保留历史版本。',
            span: 10,
            type: 'long',
          },
        ],
      },
    ],
    note: '说明：临时预案卡用于信息不足时快速形成先期处置参考。系统只自动获取和组织已有数据，不会把范围内人口直接认定为潜在亡人人数，也不会在缺少现场证据时强行确定灾种、等级或正式入库结论。',
    footerInfo: `撤离路线${routes.length}条 · 避险安置点${resettlementAreas.length}处 · 责任人信息待接口回填`,
  }
}

/**
 * 用真实来源数据构建右侧「执行过程」流式面板内容，文案全部取自来源与路线结果，
 * 接口未返回的字段统一展示 '--'，不再使用写死的示例文案
 * @param {object} options 与 buildExecuteResult 同源
 */
export const buildExecuteProcess = ({ isHazard, source = {}, routeResult = null, area = '' } = {}) => {
  const routes = Array.isArray(routeResult?.routes) ? routeResult.routes : []
  const resettlementAreas = Array.isArray(routeResult?.resettlementAreas) ? routeResult.resettlementAreas : []
  const sumResidents = (key) => routes.reduce((sum, route) => sum + (Number(route?.residentsInfo?.[key]) || 0), 0)
  const hasRoute = routes.length > 0
  const totalPeople = hasRoute ? `${sumResidents('totalCount')}人` : '--'
  const name = (isHazard ? source.name : source.eventName) || '--'
  const adminArea = formatAdminArea(source)
  const location = source.location || adminArea || '--'
  const hazardType = source.typeName || hazardTypeTextMap[source.typeCode] || source.typeCode || '--'
  /* 撤离人数与安置点：路线结果为空时不猜数字，统一 '--' */
  const routeText = hasRoute
    ? `生成${routes.length}条撤离路线，覆盖撤离${totalPeople}（老人${sumResidents('elderlyCount')}人/儿童${sumResidents('childrenCount')}人），避险安置点${resettlementAreas.length}处`
    : '暂未生成可用撤离路线'

  if (isHazard) {
    return {
      title: '执行过程',
      intro: '隐患复核结果已确认，智能体已按该隐患点最新数据更新预案。',
      steps: [
        {
          key: 'receive',
          title: '已接收隐患复核结果',
          label: '本步结论：',
          summary: [
            {
              text: `已接收“${name}”复核结果，隐患类型${hazardType}，当前威胁人口${formatCount(source.threatenedPopulation, '人')}、威胁财产${formatCount(source.threatenedPropertyValue, '万元')}。`,
            },
          ],
        },
        {
          key: 'match',
          title: '已匹配已有隐患点与原预案',
          label: '本步结论：',
          summary: [
            {
              text: `已匹配该隐患点现有台账，位于${location}；隐患体长约${formatCount(source.lengthM, '米')}、宽约${formatCount(source.widthM, '米')}、高约${formatCount(source.heightM, '米')}，规模等级${source.scaleGrade || '--'}，险情等级${source.riskGrade || '--'}。`,
            },
          ],
        },
        {
          key: 'basis',
          title: '已汇算变化依据',
          label: '本步结论：',
          summary: [
            {
              text: `稳定性结论：${source.stabilityAnalysis || source.stabilityTrend || source.stabilityStatus || '--'}；引发因素：${source.triggerFactors || '--'}；曾发生灾害时间：${source.disasterHistoryTime || '--'}。`,
            },
          ],
        },
        {
          key: 'plan',
          title: '已更新结构化预案',
          label: '本步结论：',
          summary: [
            {
              text: `已将威胁人口${formatCount(source.threatenedPopulation, '人')}、威胁财产${formatCount(source.threatenedPropertyValue, '万元')}写入预案底表，稳定性与险情等级沿用${source.riskGrade || '--'}。`,
            },
          ],
        },
        {
          key: 'route',
          title: '已生成最新撤离路线规划',
          label: '本步结论：',
          summary: [{ text: `已结合威胁范围与避险安置点，${routeText}。` }],
          open: true,
        },
      ],
    }
  }

  return {
    title: '执行过程',
    intro: '已按手动勾划范围生成预案要素，请核对后采用。',
    steps: [
      {
        key: 'range',
        title: '已接收手动勾划范围',
        label: '本步结论：',
        summary: [
          {
            text: `已圈定本次手动勾划范围并完成校验，勾划面积${area || '--'}，行政归属${adminArea || '--'}。`,
          },
        ],
      },
      {
        key: 'identify',
        title: '已识别范围与行政归属',
        label: '本步结论：',
        summary: [
          {
            text: `已圈定位于${adminArea || '--'}，关联灾险情${name}（编号${source.eventCode || '--'}）。`,
          },
        ],
      },
      {
        key: 'summary',
        title: '已汇总范围内基础数据',
        label: '本步结论：',
        summary: [
          {
            text: `已汇总范围内基础数据，识别到撤离对象${totalPeople}，避险安置点${hasRoute ? `${resettlementAreas.length}处` : '--'}，均已录入结构化预案底表。`,
          },
        ],
      },
      {
        key: 'plan',
        title: '已生成结构化预案',
        label: '本步结论：',
        summary: [{ text: `已按${name}的灾情信息生成结构化应急预案底表，更新时间${source.updateTime || source.occurTime || '--'}。` }],
      },
      {
        key: 'route',
        title: '已识别撤离路线',
        label: '本步结论：',
        summary: [{ text: `已结合范围内路网与安置点，${routeText}。` }],
        open: true,
      },
      {
        key: 'system',
        title: '已匹配责任体系',
        label: '本步结论：',
        summary: [
          {
            text: `已按行政归属${adminArea || '--'}匹配属地责任体系，责任人员清单待接口返回后展示。`,
            tone: 'warn',
          },
        ],
      },
    ],
  }
}

/** 查看更新结果：突发险情预案生成弹窗（Mock，接口就绪后按预案 id 请求） */
export const generateExecuteResult = {
  title: '示例村二组突发险情生成结果',
  /** 智能体执行状态卡 */
  agent: {
    name: '突发险情预案生成智能体',
    desc: '正在调用突发险情预案生成能力',
  },
  /** 参数识别 */
  params: {
    label: '参数识别',
    name: '示例村二组突发险情',
    status: '复盘归档',
    fields: [
      { key: 'code', label: '编号', value: 'Y-20260911-001' },
      { key: 'area', label: '勾划面积', value: '0.36km2' },
      { key: 'owner', label: '行政归属', value: '乐西乡-乐西村二组' },
      { key: 'time', label: '生成时间', value: '2026-09-11-10:18' },
    ],
  },
  /** 生成中提示（接口就绪后按真实进度展示） */
  loading: {
    text: '正在汇算勾划范围内的基础数据与责任体系...',
    tip: '已完成 3/6个步骤，生成过程中可查看右侧执行详情',
  },
  /** 生成完成 */
  done: {
    title: '智能预案已更新完成',
    desc: '已根据手动勾划范围，为您生成本次突发险情的预案草案：',
    panels: [
      {
        index: '01',
        title: '结构化应急预案',
        desc: '已匹配已知危险对象、危险对象与信息处置措施',
        items: [
          { label: '范围内危险对象', to: '12户36人' },
          { label: '应急责任体系', to: '4类责任角色' },
          { label: '措施依据', to: '8类依据' },
        ],
        link: '查看预案详情',
      },
      {
        index: '02',
        title: '撤离路线',
        desc: '结合撤离路线、风险情况优化撤离路线图，避开崩塌影响范围',
        /** 与已有预案更新结果保持一致：撤离路线卡入口固定为「查看路线详情」 */
        link: '查看路线详情',
        /** 路线示意图待设计提供，当前以占位块 + 图例展示 */
      },
    ],
    notice: '',
    filesTitle: '结果文件：',
    files: [
      { name: '示例村二组_应急预案_v1.0.docx', type: 'word', size: '4.5MB' },
      { name: '示例村二组_撤离路线图.png', type: 'word', size: '4.5MB' },
    ],
    footerTip: '可修改预案内容，保留本次生成版本',
    actions: { feedback: '提交反馈', confirm: '确认采用' },
  },
}

/** 执行过程 - 突发险情预案生成（Mock：遮挡层内的执行过程面板内容） */
export const generateProcess = {
  title: '执行过程',
  intro: '已保障勾划范围生成预案要素，请核对后采用。',
  steps: [
    {
      key: 'receive',
      title: '已接收手动勾划范围',
      label: '本步结论：',
      summary: [{ text: '已圈定本次手动勾划范围并完成校验。' }],
    },
    {
      key: 'identify',
      title: '已识别范围与行政归属',
      label: '本步结论：',
      summary: [{ text: '已圈定位于乐西乡示例村二组，已关联村、乡镇及区县责任归属。' }],
      open: true,
    },
    {
      key: 'summary',
      title: '已汇总范围内基础数据',
      label: '本步结论：',
      summary: [
        { text: '已汇总5类资料，排查到12户36人，1处安置点以及2处隐患点，均已录入结构化预案底表。' },
      ],
      open: true,
    },
    {
      key: 'plan',
      title: '已生成结构化预案',
      label: '本步结论：',
      summary: [{ text: '已生成结构化应急预案底表。' }],
    },
    {
      key: 'route',
      title: '已识别撤离路线',
      label: '本步结论：',
      summary: [
        { text: '已调用V1.0模板，范围内对象要作为核查对象，灾情类型为，实施威胁人数及影响撤离路线。' },
      ],
      open: true,
    },
    {
      key: 'system',
      title: '已匹配责任体系',
      label: '本步结论：',
      summary: [
        { text: '已自行行政归属匹配群测群防员、地质工程师、副乡镇长和区县地环站，供预案确认时排布。' },
      ],
      open: true,
    },
  ],
}
