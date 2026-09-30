import { uploadFile } from '@/api/common.js'

function formatCoordinate(value, positiveHemisphere, negativeHemisphere) {
  const coordinate = Number(value)
  if (!Number.isFinite(coordinate)) return ''
  const hemisphere = coordinate >= 0 ? positiveHemisphere : negativeHemisphere
  return `${Math.abs(coordinate).toFixed(6)}°${hemisphere}`
}

/**
 * 获取空间分析对象在输入框中的关联标签。
 * 坐标采用国际通用的“纬度 N/S，经度 E/W”十进制度格式。
 * @param {object} selection 位置面板返回的点位或斜坡单元。
 * @returns {string}
 */
export function formatSpaceAnalysisSelection(selection) {
  if (selection?.type === 'slope') return selection.slope?.name || ''
  if (selection?.type !== 'point') return ''

  const latitude = formatCoordinate(selection.point?.latitude, 'N', 'S')
  const longitude = formatCoordinate(selection.point?.longitude, 'E', 'W')
  return latitude && longitude ? `${latitude}, ${longitude}` : ''
}

/**
 * 将完整选择对象转换为聊天接口需要的结构化参数。
 * @param {object} selection 位置面板返回的点位或斜坡单元。
 * @returns {object|null}
 */
export function getSpaceAnalysisCoordinates(selection) {
  if (selection?.type === 'point') {
    const longitude = Number(selection.point?.longitude)
    const latitude = Number(selection.point?.latitude)
    return Number.isFinite(longitude) && Number.isFinite(latitude) ? { longitude, latitude } : null
  }
  if (selection?.type === 'slope' && selection.slope?.id !== undefined && selection.slope?.id !== null) {
    return { slope: selection.slope.id }
  }
  return null
}

function getUploadId(response) {
  if (response?.ossId || response?.ossid) return response.ossId || response.ossid
  if (response?.data?.ossId || response?.data?.ossid) return response.data.ossId || response.data.ossid
  if (typeof response?.data === 'string' || typeof response?.data === 'number') return response.data
  if (typeof response === 'string' || typeof response === 'number') return response
  return ''
}

/**
 * 将位置面板的选择结果转换为聊天请求所需的提示词和结构化参数。
 * @param {object} selection 位置面板返回的点位或斜坡单元。
 * @param {string} question 用户补充的问题。
 * @returns {{ prompt: string, coordinates: object }}
 */
export function buildSpaceAnalysisRequest(selection, question = '') {
  const normalizedQuestion = String(question || '').trim()
  const coordinates = getSpaceAnalysisCoordinates(selection)

  if (selection?.type === 'point') {
    if (!coordinates) throw new Error('空间分析坐标无效')
    const { longitude, latitude } = coordinates

    const locationPrompt = `请对坐标点（经度：${longitude.toFixed(6)}，纬度：${latitude.toFixed(6)}）进行空间分析`
    return {
      prompt: `@空间分析智能体 ${locationPrompt}${normalizedQuestion ? `，${normalizedQuestion}` : ''}`,
      coordinates,
    }
  }

  if (selection?.type === 'slope' && selection.slope?.id !== undefined && selection.slope?.id !== null) {
    const locationPrompt = `请对斜坡单元（${selection.slope.name || ''}）进行空间分析`
    return {
      prompt: `@空间分析智能体 ${locationPrompt}${normalizedQuestion ? `，${normalizedQuestion}` : ''}`,
      coordinates,
    }
  }

  throw new Error('空间分析对象无效')
}

/**
 * 截取位置面板地图并上传，返回聊天组件能够直接识别的附件结构。
 * @param {object} panel 位置选择面板组件实例。
 * @param {object} selection 位置面板返回的选择结果。
 * @returns {Promise<object>}
 */
export async function createSpaceAnalysisAttachment(panel, selection) {
  const blob = await panel?.captureSelectionImage?.()
  if (!blob) throw new Error('地图截图为空')

  const objectName = selection?.type === 'slope' ? selection.slope?.name : '坐标点'
  const fileName = `${objectName || '空间分析对象'}地图截图.png`
  const rawFile = new File([blob], fileName, { type: 'image/png', lastModified: Date.now() })
  const formData = new FormData()
  formData.append('file', rawFile)
  const uploadResponse = await uploadFile(formData)
  const ossId = getUploadId(uploadResponse)
  if (!ossId) throw new Error('截图上传结果缺少附件ID')

  return {
    uid: `space-analysis-${Date.now()}`,
    name: rawFile.name,
    size: rawFile.size,
    status: 'success',
    raw: rawFile,
    response: { data: { ossId: String(ossId) } },
  }
}
