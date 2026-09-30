import { getImageUrlById } from '@/api/common.js'
import { getCenter, getUrl, toBase64List } from '@/utils/index.js'
import { getDisasterDetail, getDisasterRecordDetailByHandleId, getLastDealDetail } from '@/api/deal.js'
import { insertPicture } from '@/utils/md.js'

export const FOLLOW_TIP = {
  // 事件详情开始
  EMERGENCY: 'emergency',
  // ai生成报告之前
  BEFORE_AI_REPORT: 'before_ai_report',
  // ai生成报告之后
  AFTER_AI_REPORT: 'after_ai_report',
  // 开始专家会商
  EXPERT_CONSULTATION: 'expert_consultation',
  // 专家会商中
  EXPERT_CONSULTATION_ING: 'expert_consultation_ing',
  // 方案接入专家会商前（拉人）
  SCHEME_INTEGRATION: 'scheme_integration',
  // 开启专家会商后（会议室）
  EXPERT_CONSULTATION_AFTER: 'expert_consultation_after',
  // 专家会商结束（会议结束等待审批员审批）
  EXPERT_CONSULTATION_END: 'expert_consultation_end',
  // 响应执行
  RESPONSE_EXECUTION: 'response_execution',
  // 归档
  ARCHIVE: 'archive',
}

export const PAGE_TYPE_MAPPER = {
  [FOLLOW_TIP.EMERGENCY]: 'emergency',
  [FOLLOW_TIP.BEFORE_AI_REPORT]: 'bear',
  [FOLLOW_TIP.AFTER_AI_REPORT]: 'bear',
  [FOLLOW_TIP.EXPERT_CONSULTATION]: 'personList',
  [FOLLOW_TIP.EXPERT_CONSULTATION_ING]: 'unanimously',
  [FOLLOW_TIP.SCHEME_INTEGRATION]: 'solution',
  [FOLLOW_TIP.EXPERT_CONSULTATION_AFTER]: 'solution',
  [FOLLOW_TIP.EXPERT_CONSULTATION_END]: 'solution',
  [FOLLOW_TIP.RESPONSE_EXECUTION]: 'solution-success',
  [FOLLOW_TIP.ARCHIVE]: 'solution-success',
}

export const FOLLOW_TIP_FLOW_MAPPER = {
  [FOLLOW_TIP.EMERGENCY]: 0,
  [FOLLOW_TIP.BEFORE_AI_REPORT]: 0,
  [FOLLOW_TIP.AFTER_AI_REPORT]: 0,
  [FOLLOW_TIP.EXPERT_CONSULTATION]: 1,
  [FOLLOW_TIP.EXPERT_CONSULTATION_ING]: 1,
  [FOLLOW_TIP.SCHEME_INTEGRATION]: 2,
  [FOLLOW_TIP.EXPERT_CONSULTATION_AFTER]: 2,
  [FOLLOW_TIP.EXPERT_CONSULTATION_END]: 2,
  [FOLLOW_TIP.RESPONSE_EXECUTION]: 3,
  [FOLLOW_TIP.ARCHIVE]: 4
}

export const handlePhoto = (photo) => {
  if (Array.isArray(photo)) {
    return photo
      .map(item => item?.photo)
      .filter(item => item != undefined && item != null && item != '') || []
  }

  if (typeof photo === 'string') {
    const value = photo.trim()
    if (value.startsWith('[')) {
      try {
        return handlePhoto(JSON.parse(value))
      } catch {
        // Invalid JSON falls back to comma-separated photo IDs.
      }
    }

    return value
      .split(',')
      .map(item => item.trim())
      .filter(Boolean) || []
  }

  return []
}

// 获取图片（已支持 base64）
export const getPictures = async (data = {}) => {
  let mainPicList = []
  let planePicList = []
  let sectionalPicList = []
  let remoteSensingPicList = []

  let mainPicUrlList = []
  let planePicUrlList = []
  let sectionalPicUrlList = []
  let remoteSensingPicUrlList = []

  const photoPanorama = handlePhoto(data.photoPanorama)
  if (photoPanorama.length) {
    const res = await getImageUrlById({ ids: photoPanorama })
    if (res?.length) {
      const urls = getUrl(res)
      mainPicUrlList = urls
      mainPicList = await toBase64List(urls)
    }
  }

  const photoDeformation = handlePhoto(data.photoDeformation)
  if (photoDeformation.length) {
    const res = await getImageUrlById({ ids: photoDeformation })
    if (res?.length) {
      const urls = getUrl(res)
      planePicUrlList = urls
      planePicList = await toBase64List(urls)
    }
  }

  const photoDamage = handlePhoto(data.photoDamage)
  if (photoDamage.length) {
    const res = await getImageUrlById({ ids: photoDamage })
    if (res?.length) {
      const urls = getUrl(res)
      sectionalPicUrlList = urls
      sectionalPicList = await toBase64List(urls)
    }
  }

  const remoteSensingPhoto = handlePhoto(data.remoteSensingPhoto)
  if (remoteSensingPhoto.length) {
    const res = await getImageUrlById({ ids: remoteSensingPhoto })
    if (res?.length) {
      const urls = getUrl(res)
      remoteSensingPicUrlList = urls
      remoteSensingPicList = await toBase64List(urls)
    }
  }

  return {
    mainPicList,
    planePicList,
    sectionalPicList,
    remoteSensingPicList,
    mainPicUrlList,
    planePicUrlList,
    sectionalPicUrlList,
    remoteSensingPicUrlList
  }
}


export const getHandleDetail = async(id) => {
  const res = await getDisasterDetail(id)
  if (res) {
    res.handleDetail = await getDisasterRecordDetailByHandleId(id)
    const { lng, lat, height } = getCenter(res.handleDetail?.disasterCoordinates)
    res.handleDetail.longitude = lng
    res.handleDetail.latitude = lat
    res.handleDetail.altitude = height

    // 获取处置报告
    const resData = await getLastDealDetail({
      bizId: id,
      bizType: 1,
      contentType: 1,
    })

    const json = JSON.parse(resData?.planContentJson || '{}')
    const remoteSensingPhoto =json?.['{remote_sensing_image}'] || []
    res.handleDetail.remoteSensingPhoto = remoteSensingPhoto?.join(',')

    const {
      mainPicList,
      planePicList,
      sectionalPicList,
      mainPicUrlList,
      planePicUrlList,
      sectionalPicUrlList ,
      remoteSensingPicList,
      remoteSensingPicUrlList
    } = await getPictures(res.handleDetail)
    res.handleDetail.mainPicList = mainPicList
    res.handleDetail.planePicList = planePicList
    res.handleDetail.sectionalPicList = sectionalPicList
    res.handleDetail.mainPicUrlList = mainPicUrlList
    res.handleDetail.planePicUrlList = planePicUrlList
    res.handleDetail.sectionalPicUrlList = sectionalPicUrlList
    res.handleDetail.remoteSensingPicList = remoteSensingPicList
    res.handleDetail.remoteSensingPicUrlList = remoteSensingPicUrlList

    res.aiReport = insertPicture(resData?.planContent, {
      disasterImage: res?.handleDetail?.mainPicList,
      deformationPhoto: res?.handleDetail?.planePicList,
      damagePhoto: res?.handleDetail?.sectionalPicList,
      remoteSensingPhoto: res?.handleDetail?.remoteSensingPicList
    })
  }
  return res
}