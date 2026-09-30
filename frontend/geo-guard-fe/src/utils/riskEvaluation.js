import { getCenter } from '@/utils/index.js'
import { RISK_LEVEL, RISK_LEVEL_COLOR } from './enum.js'

/**
 * 动态风险等级（对应接口 dynamicRiskLevel）
 * 0 无风险 / 1 低风险 / 2 中风险 / 3 高风险 / 4 极高风险
 */
export const DYNAMIC_RISK_LEVEL_OPTIONS = [
  { label: '极高风险', value: RISK_LEVEL.EXTREME_HIGH },
  { label: '高风险', value: RISK_LEVEL.HIGH },
  { label: '中风险', value: RISK_LEVEL.MIDDLE },
  { label: '低风险', value: RISK_LEVEL.LOW },
  { label: '无风险', value: RISK_LEVEL.NONE },
]

/** 地图「动态风险」默认勾选等级：极高风险 + 高风险 */
export const DEFAULT_DYNAMIC_RISK_LEVELS = [RISK_LEVEL.EXTREME_HIGH, RISK_LEVEL.HIGH]

const DYNAMIC_RISK_LEVEL_LABEL_MAP = Object.fromEntries(
  DYNAMIC_RISK_LEVEL_OPTIONS.map((item) => [item.value, item.label]),
)

/** 动态风险等级 -> 文案，未匹配返回空串 */
export const getDynamicRiskLevelLabel = (level) => {
  return DYNAMIC_RISK_LEVEL_LABEL_MAP[Number(level)] ?? ''
}

/** 动态风险等级 -> 主题色，统一复用项目风险色板 */
export const getDynamicRiskLevelColor = (level) => {
  return RISK_LEVEL_COLOR[Number(level)] ?? RISK_LEVEL_COLOR[RISK_LEVEL.NONE]
}

/** 评价记录优先使用记录 ID，缺失时退回斜坡单元 ID。 */
export const getRiskSlopeSelectionKey = (slope) => {
  const recordId = String(slope?.id ?? '').trim()
  return recordId || String(slope?.slopeUnitId ?? '').trim()
}

const SQUARE_METERS_PER_KM2 = 1_000_000

/** 斜坡单元面积：库内单位为平方米，UI 展示 km² */
export const toKm2 = (area) => {
  const value = Number(area)
  if (!Number.isFinite(value)) return ''
  return Number((value / SQUARE_METERS_PER_KM2).toFixed(2))
}

/** 坐标字段容错：非法值返回 undefined，保证下游 Number.isFinite 判断生效 */
const toCoordinate = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

/**
 * 格式化斜坡单元名称：在编号/名称后追加“斜坡单元”，若已有则不重复追加
 * @param {string|number} name
 * @returns {string}
 */
export const formatSlopeName = (name) => {
  const raw = String(name ?? '').trim()
  if (!raw) return ''
  if (raw.endsWith('斜坡单元') || raw.endsWith('斜坡')) return raw
  return `${raw}斜坡单元`
}

/**
 * 格式化斜坡单元地址：优先取 village-street，单字段存在时取该字段，无数据时返回 '--'
 * @param {object} slopeUnit
 * @returns {string}
 */
export const formatSlopeAddress = (slopeUnit) => {
  const village = slopeUnit?.village?.trim?.() || ''
  const street = slopeUnit?.street?.trim?.() || ''
  if (village && street) return `${village}-${street}`
  return village || street || '--'
}

/**
 * 风险评价列表行 -> 列表卡片 / 地图弹窗视图模型
 * 接口未返回的字段一律留空，不做数据伪造。
 * @param {object} row DzRiskAssessmentVo
 */
export const formatRiskSlope = (row) => {
  const slopeUnit = row?.slopeUnit ?? null
  const level = Number(row?.dynamicRiskLevel)
  const center = getCenter(slopeUnit?.center)
  const rawName =
    slopeUnit?.name ||
    row?.name ||
    slopeUnit?.slopeName ||
    row?.slopeName ||
    slopeUnit?.slopeUnitName ||
    row?.slopeUnitName ||
    ''
  const rawCode = rawName || row?.slopeUnitId || ''

  return {
    // 评价记录ID，后续接入评价过程时作为入参
    id: row?.id ?? '',
    slopeUnitId: row?.slopeUnitId ?? '',
    name: rawName || row?.slopeUnitId || '',
    slopeUnitName: rawName || row?.slopeUnitId || '',
    code: formatSlopeName(rawCode) || '未知斜坡单元',
    rawCode,
    level: getDynamicRiskLevelLabel(level),
    levelValue: Number.isFinite(level) ? level : null,
    street: slopeUnit?.street || '',
    village: slopeUnit?.village || '',
    address: formatSlopeAddress(slopeUnit),
    updateTime: row?.createDate || '',
    area: toKm2(slopeUnit?.area),
    // 若列表项带有人口底数与建筑数量则直接映射，其余由弹窗详情接口补充
    population: slopeUnit?.populationCount != null ? slopeUnit.populationCount : '',
    buildings: slopeUnit?.buildingCount != null ? slopeUnit.buildingCount : '',
    contact: '',
    rainfallTrend: [],
    lng: toCoordinate(center?.lng),
    lat: toCoordinate(center?.lat),
    alt: toCoordinate(center?.height) ?? 0,
    raw: row ?? {},
  }
}
