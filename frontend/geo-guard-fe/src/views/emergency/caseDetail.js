import { PLAN_SCENE } from '@/api/intelligentPlan.js'
import { formatAdminArea } from './config.js'

/**
 * 智能预案案件详情 → 预案卡展示数据
 * 数据源：
 *  - GET /dizai/intelligentPlan/hazard/{sourceId}/case-detail  已有隐患点（隐患点防灾预案表结构）
 *  - GET /dizai/intelligentPlan/emergency/{sourceId}/case-detail  突发灾险情（临时处置预案卡结构）
 * 接口无可靠数据来源的字段返回空字符串，这里统一展示占位文案，不回填示例值、不伪造字段。
 */

/** 接口未返回数据时的占位文案 */
const EMPTY_TEXT = '--'

/** 详情加载前的空卡：结构完整，避免弹窗首帧取不到 sections 报错 */
export const EMPTY_CASE_CARD = Object.freeze({
  fileName: '',
  cardName: '',
  version: '',
  title: '',
  subtitle: '',
  stage: '',
  image: '',
  sections: [],
  note: '',
  footerInfo: '',
})

/** 取值：null / undefined / 空串统一回落占位文案 */
const displayText = (value) => {
  if (value === null || value === undefined || value === '') {
    return EMPTY_TEXT
  }
  return String(value)
}

/** 常规字段行：label 固定 2 列，value 按 span 跨列（每行列数合计 12 列，避免排布留空） */
const fieldRow = (label, value, { span = 2, center = false, type = '', mark = null } = {}) => ({
  label,
  value: displayText(value),
  span,
  ...(center ? { center: true } : {}),
  ...(type ? { type } : {}),
  ...(mark ? { mark } : {}),
})

/** 整行长文本行：label 2 列 + value 10 列铺满一行 */
const longRow = (label, value, type = 'long') => fieldRow(label, value, { span: 10, type })

/** 取数组：接口约定的列表字段不会返回 null，兼容异常时回落空数组 */
const pickList = (value) => (Array.isArray(value) ? value : [])

/** 距离：接口返回公里数，<1 公里按米展示，与撤离路线详情弹窗口径一致 */
const formatDistance = (km) => {
  const value = Number(km)
  if (!Number.isFinite(value)) return EMPTY_TEXT
  return value < 1 ? `${Math.round(value * 1000)}米` : `${value}公里`
}

/** 预计用时：接口返回分钟数 */
const formatDuration = (minutes) => {
  const value = Number(minutes)
  return Number.isFinite(value) ? `${value}分钟` : EMPTY_TEXT
}

/**
 * 撤离路线结果 → 预案卡的「候选撤离路线」行（方案 / 路径 / 距离 / 预计用时）
 * 路径用「撤离起点 → 撤离目标点」拼接，与撤离路线详情弹窗的展示口径一致
 */
const buildCandidateRoutes = (routes) =>
  pickList(routes).map((route, index) => ({
    plan: `撤离路线${index + 1}`,
    path: `${displayText(route?.evacuationArea)} → ${displayText(route?.resettlementPoint)}`,
    distance: formatDistance(route?.distanceKm),
    duration: formatDuration(route?.estimatedTimeMinutes),
  }))

/**
 * 突发灾险情案件详情 → 临时处置预案卡
 * @param {object} detail GET /emergency/{sourceId}/case-detail 返回体
 * @param {object} options image 撤离路线示意图（截取自结果弹窗内的路线地图）
 */
const buildEmergencyCard = (detail = {}, { image = '' } = {}) => {
  const basic = detail.eventBasicInfo || {}
  const situation = detail.rapidSituation || {}
  const response = detail.initialResponse || {}
  const evacuation = detail.temporaryEvacuation || {}
  const version = detail.verificationVersion || {}
  const responsibilities = pickList(detail.onSiteResponsibilities?.responsibilities)
  const candidateRoutes = pickList(evacuation.candidateRoutes)
  const title = detail.title || '临时处置预案卡'

  const steps = pickList(response.phasedActions).map((item) => ({
    title: displayText(item?.stage),
    text: displayText(item?.action),
  }))

  return {
    cardName: '临时处置预案卡',
    fileName: title,
    version: version.currentVersion || '',
    title,
    subtitle: '仅用于现场核实前的先期处置 · 数据取自案件详情接口，未返回项以“--”展示',
    stage: detail.stageDescription || '',
    image,
    sections: [
      {
        title: '事件基本信息',
        note: '名称、编号、时间、地点与范围取自灾险情台账',
        rows: [
          fieldRow('事件名称', basic.eventName, { span: 10, mark: { text: '列表数据', type: 'db' } }),
          fieldRow('预案编号', basic.eventCode, { span: 2, center: true }),
          fieldRow('发生时间', basic.occurTime, { span: 2, center: true }),
          fieldRow('当前状态', basic.currentStatus, { span: 2, center: true }),
          longRow('发生地点', basic.location),
          fieldRow('中心坐标', basic.centerCoordinate, {
            span: 4,
            center: true,
            mark: { text: '定位', type: 'gis' },
          }),
          fieldRow('所属行政区', basic.administrativeArea || formatAdminArea(basic), { span: 4 }),
          longRow('事件范围', basic.eventRange),
          longRow('事件范围WKT', basic.eventRangeWkt),
        ],
      },
      {
        title: '快速态势',
        note: '按事件范围自动叠加，均需现场复核',
        rows: [
          fieldRow('疑似灾险情类型', situation.suspectedDisasterType, { span: 2, center: true }),
          fieldRow('灾害规模', situation.disasterScale, { span: 2, center: true }),
          fieldRow('A/B/C/D分类', situation.classification, { span: 2, center: true }),
          fieldRow('风险等级', situation.riskLevel, { span: 2, center: true }),
          fieldRow('潜在亡人数', situation.potentialFatalities, { span: 2, center: true }),
          fieldRow('先期处置原则', situation.initialDisposalPrinciple, { span: 2, center: true }),
          fieldRow('范围内居民', situation.residentsInRange, { span: 2, center: true }),
          fieldRow('重点关注人员', situation.keyConcernPersons, { span: 2, center: true }),
          fieldRow('涉及道路', situation.involvedRoads, { span: 2, center: true }),
          fieldRow('周边风险源', situation.surroundingRiskSources, { span: 4 }),
          fieldRow('周边敏感目标', situation.surroundingSensitiveTargets, { span: 4 }),
          longRow('气象与监测摘要', situation.weatherMonitoringSummary),
          longRow('临时风险提示', situation.temporaryRiskWarning, 'long warning'),
        ],
      },
      {
        title: '先期处置安排',
        note: '由后端按灾害类型编排，可随现场反馈动态更新',
        rows: [
          {
            label: '分阶段行动',
            span: 10,
            mark: { text: '流程编排', type: 'ai' },
            steps: steps.length ? steps : [{ title: EMPTY_TEXT, text: '接口未返回分阶段行动' }],
          },
          longRow('临时管控范围', response.temporaryControlArea),
          longRow('人员避险建议', response.evacuationAdvice),
          longRow('现场核查重点', response.fieldInspectionPriorities),
        ],
      },
      {
        title: '临时撤离与资源调度',
        note: '均为候选方案，使用前必须核实',
        rows: [
          fieldRow('候选安置点', evacuation.candidateResettlementPoint, {
            span: 4,
            mark: { text: '资源匹配', type: 'gis' },
          }),
          fieldRow('当前可用性', evacuation.availability, { span: 4, center: true }),
          {
            label: '候选撤离路线',
            span: 10,
            mark: { text: '道路网计算', type: 'gis' },
            routes: candidateRoutes.length
              ? candidateRoutes.map((item) => ({
                  plan: displayText(item?.schemeName),
                  path: displayText(item?.path),
                  distance: displayText(item?.distance),
                  duration: displayText(item?.estimatedTime),
                }))
              : [{ plan: EMPTY_TEXT, path: '暂未生成可用撤离路线', distance: EMPTY_TEXT, duration: EMPTY_TEXT }],
          },
          {
            label: '撤离路线示意图',
            span: 10,
            image,
            imageEmpty: '路线示意图生成中，请先生成撤离路线',
          },
          longRow('路线风险提示', evacuation.routeRiskWarning, 'long warning'),
          fieldRow('预警与联络方式', evacuation.warningContactMethod, { span: 4 }),
          fieldRow('候选医疗资源', evacuation.candidateMedicalResources, { span: 4 }),
        ],
      },
      {
        title: '现场责任人与任务',
        note: '姓名、电话由后端按行政区划责任体系回填',
        rows: [
          {
            label: '责任分工',
            span: 10,
            duties: responsibilities.length
              ? responsibilities.map((item) => ({
                  role: displayText(item?.role),
                  name: displayText(item?.name),
                  phone: displayText(item?.phone),
                  task: displayText(item?.task),
                }))
              : [{ role: EMPTY_TEXT, name: EMPTY_TEXT, phone: EMPTY_TEXT, task: '接口未返回责任人员' }],
          },
        ],
      },
      {
        title: '待核实与版本信息',
        rows: [
          longRow('必须补充的信息', version.requiredInformation),
          fieldRow('当前版本', version.currentVersion, { span: 2, center: true }),
          fieldRow('生成时间', version.generationTime, { span: 2, center: true }),
          fieldRow('有效状态', version.validStatus, { span: 2, center: true }),
          longRow('下一版本触发', version.nextVersionTrigger),
        ],
      },
    ],
    note: detail.notice || '',
    footerInfo: `候选撤离路线${candidateRoutes.length}条 · 现场责任人员${responsibilities.length}人 · 版本${displayText(version.currentVersion)}`,
  }
}

/**
 * 已有隐患点案件详情 → 隐患点防灾预案表
 * @param {object} detail GET /hazard/{sourceId}/case-detail 返回体
 * @param {object} options image 撤离路线示意图
 * @param {Array} options.routes 撤离路线结果（/route/latest 的 routes），用于生成候选撤离路线表
 */
const buildHazardCard = (detail = {}, { image = '', routes = [] } = {}) => {
  const basic = detail.basicInfo || {}
  const monitoring = detail.monitoringWarning || {}
  const evacuation = detail.emergencyEvacuation || {}
  const persons = detail.responsiblePersons || {}
  const compilation = detail.compilationConfirmation || {}
  const title = detail.title || '地质灾害隐患点防灾预案表'
  const candidateRoutes = buildCandidateRoutes(routes)

  return {
    cardName: '地质灾害隐患点防灾预案表',
    fileName: title,
    version: '',
    title,
    subtitle: '数据取自隐患点案件详情接口，未返回项以“--”展示',
    stage: '',
    image,
    sections: [
      {
        title: '基本情况',
        note: '名称、编号、位置与规模取自隐患点台账',
        rows: [
          fieldRow('灾害点名称', basic.hazardName, { span: 10, mark: { text: '台账数据', type: 'db' } }),
          fieldRow('灾害点统一编号', basic.hazardCode, { span: 2, center: true }),
          fieldRow('类型', basic.hazardType, { span: 2, center: true }),
          fieldRow('规模', basic.scale, { span: 2, center: true }),
          longRow('地理位置', basic.location),
          fieldRow('经度', basic.longitude, { span: 2, center: true, mark: { text: '定位', type: 'gis' } }),
          fieldRow('纬度', basic.latitude, { span: 2, center: true, mark: { text: '定位', type: 'gis' } }),
          fieldRow('诱发因素', basic.triggerFactors, { span: 2, center: true }),
          fieldRow('威胁户数（户）', basic.threatenedHouseholds, { span: 2, center: true }),
          fieldRow('威胁人数（人）', basic.threatenedPopulation, { span: 2, center: true }),
          fieldRow('威胁财产（万元）', basic.threatenedProperty, { span: 2, center: true }),
          longRow('威胁对象', basic.threatenedObject),
          longRow('灾害体与周边关系', basic.disasterSurroundingRelationship),
          longRow('变形特征及活动历史', basic.deformationFeatures),
          longRow('潜在危害', basic.potentialHazards),
        ],
      },
      {
        title: '监测预警',
        note: '由隐患点台账与群测群防责任回填',
        rows: [
          fieldRow('监测方式', monitoring.monitoringMode, { span: 2, center: true }),
          fieldRow('监测周期', monitoring.monitoringCycle, { span: 2, center: true }),
          fieldRow('监测手段', monitoring.monitoringMeans, { span: 2, center: true }),
          fieldRow('监测部位', monitoring.monitoringLocation, { span: 4 }),
          fieldRow('预定报警信号', monitoring.alarmSignal, { span: 4 }),
          longRow('监测的主要迹象', monitoring.mainMonitoringSigns),
          longRow('监测的主要手段和方法', monitoring.mainMonitoringMeansAndMethods),
          longRow('临灾预报的判据', monitoring.warningCriteria),
          fieldRow('预警方式', monitoring.warningMethod, { span: 4 }),
          fieldRow('报警人电话', monitoring.alarmPhone, { span: 4 }),
        ],
      },
      {
        title: '应急处置与撤离',
        note: '撤离安排由岗位人员现场执行，使用前需确认通行条件',
        rows: [
          longRow('应急处理措施', evacuation.emergencyMeasures),
          longRow('撤离路线', evacuation.evacuationRoute),
          fieldRow('路线距离', evacuation.routeDistance, { span: 2, center: true }),
          fieldRow('撤离顺序', evacuation.evacuationOrder, { span: 2, center: true }),
          fieldRow('预计用时', evacuation.estimatedTime, { span: 2, center: true }),
          longRow('预定避灾地点', evacuation.resettlementLocation),
          longRow('预定疏散路线', evacuation.plannedEvacuationRoute),
          longRow('路线风险提示', evacuation.routeRiskWarning, 'long warning'),
          {
            label: '候选撤离路线',
            span: 10,
            mark: { text: '道路网计算', type: 'gis' },
            routes: candidateRoutes.length
              ? candidateRoutes
              : [{ plan: EMPTY_TEXT, path: '暂未生成可用撤离路线', distance: EMPTY_TEXT, duration: EMPTY_TEXT }],
          },
          {
            label: '撤离路线示意图',
            span: 10,
            image,
            imageEmpty: '路线示意图生成中，请先生成撤离路线',
          },
        ],
      },
      {
        title: '责任人员',
        note: '姓名、电话由后端按行政区划责任体系回填',
        rows: [
          fieldRow('监测人员', persons.monitorPerson, { span: 2, center: true }),
          fieldRow('监测人员电话', persons.monitorPhone, { span: 2, center: true }),
          fieldRow('报警人电话', persons.alarmPhone, { span: 2, center: true }),
          fieldRow('村长', persons.villageHead, { span: 2, center: true }),
          fieldRow('村长电话', persons.villageHeadPhone, { span: 2, center: true }),
          fieldRow('组长 / 组长电话', persons.groupLeaderAndPhone, { span: 2, center: true }),
          fieldRow('疏散命令发布人', persons.evacuationOrderIssuer, { span: 2, center: true }),
          fieldRow('疏散值班电话', persons.evacuationDutyPhone, { span: 2, center: true }),
          fieldRow('预警信号发布人', persons.warningSignalIssuer, { span: 2, center: true }),
          fieldRow('抢、排险单位、负责人', persons.rescueUnitAndLeader, { span: 2, center: true }),
          fieldRow('抢、排险值班电话', persons.rescueDutyPhone, { span: 2, center: true }),
          fieldRow('治安保卫单位、负责人', persons.securityUnitAndLeader, { span: 2, center: true }),
          fieldRow('治安保卫值班电话', persons.securityDutyPhone, { span: 2, center: true }),
          fieldRow('医疗救护单位、负责人', persons.medicalUnitAndLeader, { span: 2, center: true }),
          fieldRow('医疗救护值班电话', persons.medicalDutyPhone, { span: 2, center: true }),
        ],
      },
      {
        title: '编制与确认',
        rows: [
          fieldRow('预案编制单位', compilation.preparationUnit, { span: 4 }),
          fieldRow('预案批准单位', compilation.approvalUnit, { span: 4 }),
          longRow('生成依据', compilation.generationBasis),
          longRow('待人工确认事项', compilation.manualConfirmationItems),
        ],
      },
    ],
    note: '',
    footerInfo: `避险地点${displayText(evacuation.resettlementLocation)} · 编制单位${displayText(compilation.preparationUnit)}`,
  }
}

/**
 * 按场景把案件详情转成预案卡数据
 * @param {object} options
 * @param {string} options.scene 场景枚举（PLAN_SCENE）
 * @param {object|null} options.detail 案件详情，接口未返回时可传 null
 * @param {string} [options.image] 撤离路线示意图
 * @param {Array} [options.routes] 撤离路线结果（/route/latest 的 routes），已有隐患点卡片用它生成候选撤离路线表
 * @returns {object} PlanDetailDialog 可直接渲染的预案卡数据
 */
export const buildPlanCaseCard = ({ scene, detail = null, image = '', routes = [] } = {}) => {
  if (!detail || typeof detail !== 'object') {
    return EMPTY_CASE_CARD
  }
  return scene === PLAN_SCENE.EMERGENCY
    ? buildEmergencyCard(detail, { image })
    : buildHazardCard(detail, { image, routes })
}
