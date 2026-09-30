import { useUserStore } from '@/store/user.js'
import {
  AREA_BOUNDARY_SIMPLIFY_TOLERANCE,
  AREA_LEVEL,
  ROLE_MAP,
  SOURCE_TYPE,
  TASK_TRACK_STATUS,
  TASK_TRACK_STATUS_NUM,
} from './enum'
import axios from 'axios'
import { parse, convert } from 'terraformer-wkt-parser'
import dayjs from 'dayjs'
import { round } from 'lodash-es'
import { diffWords } from 'diff'
import heic2any from 'heic2any'
import * as turf from '@turf/turf'
import { storeToRefs } from 'pinia'
import { ElLoading } from 'element-plus'
import * as Cesium from 'mars3d-cesium'
import { getImageUrlById } from '@/api/common.js'
import { jsonrepair } from 'jsonrepair'

export const isDev = () => import.meta.env.DEV

export const formatNumber = (num, precision = 0) => {
  num = parseFloat(num)
  if (isNaN(num)) return
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: precision,
    maximumFractionDigits: precision,
  }).format(num)
}

export const formatFileSize = (fileOrSize) => {
  const size = Number(fileOrSize?.size ?? fileOrSize)
  if (!Number.isFinite(size) || size < 0) return ''

  if (size < 1024) {
    return `${size}B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

/**
 * 平方米转换为平方公里。
 * @param {number|string} value 平方米数值
 * @param {number} precision 保留小数位数
 * @returns {number}
 */
export const squareMetersToSquareKilometers = (value, precision = 2) => {
  const area = Number(value)
  if (!Number.isFinite(area)) return 0
  return Number((area / 1000000).toFixed(precision))
}

export const simpleClone = (obj) => JSON.parse(JSON.stringify(obj))

export const isEmptyValue = (value) => value === null || value === undefined || value === ''

// 防抖函数
export const debounce = (func, delay = 1000, immediate = false) => {
  let timer = null
  return function (...args) {
    if (immediate && !timer) {
      func.apply(this, args)
    }
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      func.apply(this, args)
    }, delay)
  }
}

export const getRandomInt = (min, max) => {
  min = Math.ceil(min)
  max = Math.floor(max)
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// 复制文本-兼容降级
export function copyToClipboard(text) {
  // 检测是否支持 Clipboard API
  if (navigator.clipboard) {
    return navigator.clipboard.writeText(text) // 现代浏览器用 API
  } else {
    // 降级方案：创建隐藏 textarea
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed' // 避免滚动影响
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select() // 选中内容
    document.execCommand('copy') // 执行复制（旧 API，部分浏览器已废弃）
    document.body.removeChild(textarea)
    return Promise.resolve()
  }
}

export function getToken() {
  return localStorage.getItem(import.meta.env.VITE_APP_TOKEN_KEY) || ''
}

export function removeToken() {
  const userStore = useUserStore()

  userStore.removeToken()
  localStorage.removeItem(import.meta.env.VITE_APP_TOKEN_KEY)
}

export function isJsonString(str) {
  if (typeof str !== 'string') return false // 先确保是字符串
  try {
    const obj = JSON.parse(str)
    // 可以进一步判断解析结果是不是对象或数组
    return typeof obj === 'object' && obj !== null
  } catch (e) {
    console.error('Error parsing JSON:', e, str)
    return false
  }
}

/**
 * 生成随机字符串
 * @param {number} length - 生成的字符串长度
 * @returns {string}
 */
export const generateRandomString = (length = 8, useNum = true) => {
  const chars = `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz${useNum ? '0123456789' : ''}`
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

// 处理需要替换的标签
export function replaceArticleTag(content, dbs) {
  return content.replace(/\[(\d,?)+\](\((@ref|@re|@r|@)\)?)+/g, function (...[, article_num]) {
    article_num = parseInt(article_num)
    return `<span class="quote-text" num="${article_num}">${dbs[article_num - 1]?.title || '无标题'}</span>`
  })
}

export async function downloadFileWithId(db) {
  const response = await fetch(
    `/api/dify/datasets/${encodeURIComponent(db.dataset_id)}/documents/${encodeURIComponent(db.document_id)}/upload-file`,
  )
  if (!response.ok) {
    throw new Error(`Dify 文件下载失败 (${response.status})`)
  }

  const data = await response.json()
  if (!data.download_url) {
    throw new Error('Dify 未返回文件下载地址')
  }

  const downloadUrl = /^https?:\/\//i.test(data.download_url)
    ? data.download_url
    : `${import.meta.env.VITE_APP_DIFY_URL}${data.download_url}`
  window.open(downloadUrl, '_blank', 'noopener')
}

export function parseWktMultiPolygon(wkt) {
  // 去除 MULTIPOLYGON 和多余括号
  const match = wkt.trim().match(/^MULTIPOLYGON\s*\(\s*\(\s*\((.+)\)\s*\)\s*\)$/i)
  if (!match) throw new Error('Invalid WKT MultiPolygon')

  const coordsStr = match[1]
  // 按逗号分割点（注意：坐标内部也有空格，不能 split(',') 粗暴处理）
  // 更健壮的方式：用正则匹配数字对
  const points = coordsStr.match(/(-?\d+\.?\d*)\s+(-?\d+\.?\d*)/g).map((pair) => {
    const [x, y] = pair.trim().split(/\s+/).map(Number)
    return [x, y] // [lon, lat]
  })

  return {
    type: 'MultiPolygon',
    coordinates: [[points]], // 注意四层嵌套
  }
}

// 根据后端数据拼接出完整地址信息，级别从小到大
export function formatAddress(data, endPoint = 'city') {
  if (!data) return []
  let result = []
  if (data.village) {
    result.push({
      type: 'village',
      name: data.village,
      id: data.villageCode,
      level: AREA_LEVEL.VILLAGE,
      pcode: data.streetCode,
    })
    if (endPoint === 'village') return result
  }
  if (data.street) {
    result.push({
      type: 'street',
      name: data.street,
      id: data.streetCode,
      level: AREA_LEVEL.STREET,
      pcode: data.cityCode,
    })
    if (endPoint === 'street') return result
  }
  if (data.county) {
    result.push({
      type: 'county',
      name: data.county,
      id: data.countyCode,
      level: AREA_LEVEL.COUNTY,
      pcode: data.provinceCode,
    })
    if (endPoint === 'county') return result
  }
  if (data.city) {
    result.push({ type: 'city', name: data.city, id: data.cityCode, level: AREA_LEVEL.CITY, pcode: data.provinceCode })
    if (endPoint === 'city') return result
  }
  return result
}

export function formatAddressName(data, endPoint = 'city') {
  return formatAddress(data, endPoint)
    .map((v) => v.name)
    .reverse()
    .join('')
}

export function parseSseJson(data) {
  if (!data) return null

  if (typeof data === 'object') return data // 已经是对象

  try {
    let obj = JSON.parse(data)

    // 如果解析后还是字符串（双重转义），再 parse 一次
    if (typeof obj === 'string') obj = JSON.parse(obj)

    return obj
  } catch (e) {
    console.error('SSE JSON 解析失败', e, data)
    return null
  }
}

export function removeBlackBackground(base64Image) {
  return new Promise((resolve) => {
    const img = new Image()
    img.src = base64Image

    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')

      ctx.drawImage(img, 0, 0)

      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const data = imageData.data

      for (let i = 0; i < data.length; i += 4) {
        const r = data[i]
        const g = data[i + 1]
        const b = data[i + 2]
        const a = data[i + 3]

        // 如果是黑色或者接近黑色，就把 alpha 置 0
        if (r < 10 && g < 10 && b < 10) {
          data[i + 3] = 0 // alpha 透明
        }
      }

      ctx.putImageData(imageData, 0, 0)
      resolve(canvas)
    }
  })
}

/**
 * 下载 blob 数据
 * @param {Blob} blob - 后端返回的 blob 或自己构造的 Blob
 * @param {string} filename - 下载的文件名（含后缀）
 */
export const downloadFile = async (url, params, filenamename = '未命名' + new Date().getTime() + '.xlsx') => {
  let response = await axios({
    url: '/api' + url,
    method: 'POST',
    data: params,
    responseType: 'blob',
    headers: {
      'Content-Type': 'application/json',
    },
  })
  const blob = new Blob([response.data], { type: 'application/octet-stream' })
  downloadBlob(blob, filenamename)
}
export const downloadBlob = (blob, filename) => {
  if (!(blob instanceof Blob)) {
    throw new Error('downloadBlob: 参数 blob 必须是 Blob 类型')
  }

  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')

  a.style.display = 'none'
  a.href = url
  a.download = filename

  document.body.appendChild(a)
  a.click()

  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/**
 * @param {Function} fn 需要执行的函数（必须返回 Promise）
 * @param {number} maxRetry 最大重试次数
 * @param {number} delay 重试间隔(ms)
 */
export const retryFun = async (fn, maxRetry = 3, delay = 2000) => {
  let attempt = 0

  const run = async () => {
    try {
      return await fn()
    } catch (err) {
      attempt++
      if (attempt >= maxRetry) {
        throw err
      }
      await new Promise((resolve) => setTimeout(resolve, delay))
      return run()
    }
  }

  return run()
}

/**
 * 安全修复 JSON 字符串
 * @param {string} input 原始 json 字符串
 * @returns {string} 可安全 JSON.parse 的字符串，失败返回 "{}"
 */
export const safeFixJsonString = (input) => {
  if (typeof input !== 'string') return '{}'

  let str = input.trim()
  if (!str) return '{}'

  // 0️⃣ 去掉 ```json 包裹
  str = str.replace(/```json|```/gi, '').trim()

  // 1️⃣ 双层 JSON 脱壳
  if (str.startsWith('"') && str.endsWith('"')) {
    try {
      const parsed = JSON.parse(str)
      if (typeof parsed === 'string') {
        str = parsed.trim()
      }
    } catch {}
  }

  // 2️⃣ 去尾逗号
  str = str.replace(/,\s*([}\]])/g, '$1')

  // 3️⃣ 直接尝试
  if (isValidJson(str)) return str

  // 4️⃣ 提取 JSON 主体
  str = extractJsonBody(str)
  if (isValidJson(str)) return str

  // 5️⃣ 补缺失的右括号
  const openBraces = (str.match(/{/g) || []).length
  const closeBraces = (str.match(/}/g) || []).length
  const openBrackets = (str.match(/\[/g) || []).length
  const closeBrackets = (str.match(/]/g) || []).length

  if (openBraces > closeBraces) {
    str += '}'.repeat(openBraces - closeBraces)
  }

  if (openBrackets > closeBrackets) {
    str += ']'.repeat(openBrackets - closeBrackets)
  }

  if (isValidJson(str)) return str

  // 6️⃣ 删除尾部多余括号
  let repaired = str
  let count = 0
  const MAX_REPAIR = 20

  while (count < MAX_REPAIR && /[}\]]$/.test(repaired)) {
    repaired = repaired.slice(0, -1).trim()
    if (isValidJson(repaired)) return repaired
    count++
  }

  return '{}'
}

export const autoFixJson = (str) => {
  if (typeof str !== 'string') return null

  let s = str.trim()

  const stack = []
  const pairs = { '{': '}', '[': ']' }

  for (let char of s) {
    if (pairs[char]) {
      stack.push(pairs[char])
    } else if (char === '}' || char === ']') {
      if (stack.length && stack[stack.length - 1] === char) {
        stack.pop()
      } else {
        // 多余的右括号，删除
        s = s.replace(char, '')
      }
    }
  }

  // 补齐缺少的右括号
  while (stack.length) {
    s += stack.pop()
  }

  try {
    return s
  } catch {
    return null
  }
}

// ---------------- helpers ----------------

const isValidJson = (str) => {
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

const extractJsonBody = (str) => {
  const first = str.search(/[{\[]/)
  const last = Math.max(str.lastIndexOf('}'), str.lastIndexOf(']'))

  if (first === -1 || last === -1 || last <= first) return str

  return str.slice(first, last + 1)
}

/**
 * 填充 {name} 格式占位符
 * @param {string} template
 * @param {Object} data
 * @returns {string}
 */
export const fillTemplate = (template = '', data = {}) => {
  return template.replace(/\{(\w+)\}/g, (_, key) => {
    return data[key] ?? ''
  })
}

// 创建一个随机选择器（带记忆）
export const createRandomPicker = () => {
  let lastSet = new Set()

  const pickRandomItems = (arr, count = 3) => {
    if (!arr?.length) return []

    // ① 去重（关键）
    const uniqueArr = [...new Set(arr)]

    // ② 限制最大返回数量
    const maxCount = Math.min(count, uniqueArr.length)

    // ③ 过滤掉上一次选过的
    let candidates = uniqueArr.filter((item) => !lastSet.has(item))

    // ⚠️ 如果不够选了，就允许重新参与（避免选不出来）
    if (candidates.length < maxCount) {
      candidates = uniqueArr
    }

    // ④ 随机打乱
    const shuffled = candidates
      .map((item) => ({ item, sort: Math.random() }))
      .sort((a, b) => a.sort - b.sort)
      .map(({ item }) => item)

    // ⑤ 取前 N 个
    const result = shuffled.slice(0, maxCount)

    // ⑥ 记录本次结果
    lastSet = new Set(result)

    return result
  }

  return {
    pickRandomItems,
  }
}

/**
 * 小数转百分比字符串
 * @param {number} value - 小数值，如 0.256
 * @param {number} decimals - 保留小数位数，默认 2
 * @returns {string} 百分比字符串，如 "25.60%"，null/undefined/NaN 返回 "0%"
 */
export function toPercent(value, decimals = 2) {
  // 如果不是有效数字，默认使用 0
  if (typeof value !== 'number' || isNaN(value)) {
    value = 0
  }
  const percent = value * 100
  return percent.toFixed(decimals) + '%' // 固定小数位
}

export const toFixed = (value, digits = 2) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return 0
  return round(num, digits)
}

export function formatDistanceSafe(squareMeter, digits = 3) {
  const value = Number(squareMeter)
  if (Number.isNaN(value)) return '0.00'
  return (value / 1_000_000).toFixed(digits)
}

export function formatMetersToKm(meters, digits = 2) {
  const value = Number(meters)
  if (Number.isNaN(value)) return '0.00'
  return (value / 1000).toFixed(digits)
}

// 获取中心点坐标
export function getCenter(center) {
  if (!center) return {}

  try {
    const geojson = parse(center)

    if (geojson.type !== 'Point') return null

    const [lng = 0, lat = 0, height = 0] = geojson.coordinates

    return {
      lng,
      lat,
      height,
    }
  } catch (err) {
    console.error('WKT解析失败:', err)
    return {}
  }
  // const match = center?.match(/POINT(?:\s+Z)?\s*\(\s*([-\d.]+)\s+([-\d.]+)(?:\s+([-\d.]+))?\s*\)/i)
  //
  // if (!match) return null
  //
  // const [, lng, lat, z] = match
  //
  // return {
  //   lng: +lng,
  //   lat: +lat,
  //   height: z ? +z : 0,
  // }
}

// 根据枚举生成选项数组
export function generateOptions(enums) {
  let options = Object.keys(enums).map((val) => ({ label: enums[val], value: val }))
  return options
}

/**
 * 高级心跳轮询封装
 * @param {Function} callback 心跳请求函数（可返回 Promise）
 * @param {Object} options 配置项
 *   - interval: 心跳间隔，单位 ms，默认 3000
 *   - retry: 请求失败重试次数，默认 3
 */
export function createAdvancedHeartbeat(callback, options = {}) {
  const { interval = 3000, retry = 3 } = options
  let active = false
  let timeoutId = null

  const run = async () => {
    if (!active) return

    let attempts = 0
    const tryHeartbeat = async () => {
      try {
        await callback()
        attempts = 0 // 成功后重置重试次数
      } catch (err) {
        console.error('心跳异常:', err)
        attempts++
        if (attempts <= retry) {
          console.log(`重试第 ${attempts} 次`)
          return tryHeartbeat() // 失败重试
        }
      }
    }

    // 启动心跳请求（异步安全，不累积调用栈）
    Promise.resolve()
      .then(tryHeartbeat)
      .finally(() => {
        if (active) {
          timeoutId = setTimeout(run, interval)
        }
      })
  }

  return {
    start() {
      if (!active) {
        active = true
        run()
      }
    },
    stop() {
      active = false
      if (timeoutId) clearTimeout(timeoutId)
    },
  }
}

// 防重提交
export const antiResubmission = (fn) => {
  let running = false

  return async function (...args) {
    if (running) return
    running = true

    try {
      await Promise.resolve(fn.apply(this, args))
      await Promise.resolve() // 等待微任务队列
    } finally {
      running = false
    }
  }
}

// 等待所有任务完成
export function waitForTasksToComplete() {
  return new Promise((resolve) => {
    function check() {
      queueMicrotask(() => {
        setTimeout(resolve, 0)
      })
    }
    // 启动检查流程
    check()
  })
}

// 等待任务完成，超时后返回 false，避免条件不满足时无限等待。
export const waitForTaskToComplete = (conditionFn, interval = 50, maxWait = 10000) => {
  return new Promise((resolve, reject) => {
    const startTime = Date.now()
    let timer = null

    const cleanup = () => {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
    }

    const check = () => {
      let result
      try {
        result = conditionFn()
      } catch (error) {
        cleanup()
        reject(error)
        return
      }

      if (result) {
        cleanup()
        resolve(result)
        return
      }

      if (Number.isFinite(maxWait) && Date.now() - startTime >= maxWait) {
        cleanup()
        resolve(false)
        return
      }

      timer = setTimeout(check, interval)
    }

    check()
  })
}

// rem 转 px
export const rem2px = (rem) => rem * parseFloat(getComputedStyle(document.documentElement).fontSize)

export const handleStatus = (params) => {
  if (params.statusNum) {
    switch (+params.statusNum) {
      case TASK_TRACK_STATUS_NUM.DONE:
        params.statusList = [TASK_TRACK_STATUS.CLOSED, TASK_TRACK_STATUS.FEEDBACK]
        break
      // 未完成 -- 未核查 核查中
      case TASK_TRACK_STATUS_NUM.UNFINISHED:
        params.statusList = [TASK_TRACK_STATUS.UN_CHECK, TASK_TRACK_STATUS.CHECKING]
        break
      // 未推送
      case TASK_TRACK_STATUS_NUM.UN_SEND:
        params.status = TASK_TRACK_STATUS.UN_SEND
        break
      // 未核查
      case TASK_TRACK_STATUS_NUM.UN_CHECK:
        params.status = TASK_TRACK_STATUS.UN_CHECK
        break
      // 核查中
      case TASK_TRACK_STATUS_NUM.CHECKING:
        params.status = TASK_TRACK_STATUS.CHECKING
        break
      // 已反馈
      case TASK_TRACK_STATUS_NUM.FEEDBACK:
        params.status = TASK_TRACK_STATUS.FEEDBACK
        break
      // 已关闭
      case TASK_TRACK_STATUS_NUM.CLOSED:
        params.status = TASK_TRACK_STATUS.CLOSED
        break
      // 已过期
      case TASK_TRACK_STATUS_NUM.EXPIRED:
        params.status = TASK_TRACK_STATUS.EXPIRED
        break
      // 申请技术协查
      case TASK_TRACK_STATUS_NUM.TECH_ASSISTANCE:
        params.status = TASK_TRACK_STATUS.TECH_ASSISTANCE
        break
      default:
        break
    }
  }
  return params.status
}

// 来源处理
export const sourceTypeChange = (params) => {
  if (params.sourceType == -1) {
    params.sourceType = null
    params.sourceTypeList = [
      SOURCE_TYPE.ADD_BY_USER,
      SOURCE_TYPE.SYSTEM_ASSESSMENT,
      SOURCE_TYPE.PUBLIC_REPORT,
      SOURCE_TYPE.DEFENSE_RESPONSE,
      SOURCE_TYPE.EMERGENCY_DISPOSAL,
      SOURCE_TYPE.MONITORING_WARNING,
      SOURCE_TYPE.TECHNICAL_ASSISTANCE,
    ]
    // params.isEmergency = 1
  } else if (params.sourceType == 4) {
    // params.planType = 3
  }
}

/**
 * 根据type返回日期文本
 * 3：月  4：季  5：年
 */
export const getDateLabel = (type) => {
  const now = dayjs()
  const year = now.year()
  const month = now.month() + 1

  switch (type) {
    case 3:
      return `${year}年${month}月`

    case 4: {
      const quarter = Math.ceil(month / 3)
      const startMonth = (quarter - 1) * 3 + 1
      const endMonth = quarter * 3
      return `${year}年第${quarter}季度（${startMonth}月-${endMonth}月）`
    }

    case 5:
      return `${year}年`

    default:
      return ''
  }
}

export const removeDiffSpan = (str = '') => {
  return (
    str
      // 删除 remove-text 整段
      ?.replace(/<span class="remove-text">[\s\S]*?<\/span>/g, '')
      // 去掉 add-text 标签但保留内容
      ?.replace(/<span class="add-text">/g, '')
      ?.replace(/<\/span>/g, '') || ''
  )
}

const splitListSpan = (html = '') => {
  return html.replace(/<span class="(add-text|remove-text)">([\s\S]*?)<\/span>/g, (match, cls, content) => {
    // 匹配换行 + 数字列表（\n4. / \n10.）
    const reg = /\n(\d+\.\s*)/g

    if (!reg.test(content)) return match

    let result = ''
    let lastIndex = 0

    content.replace(reg, (m, listMark, index) => {
      // 前一段（保留在 span 内）
      const prev = content.slice(lastIndex, index)
      if (prev) {
        result += `<span class="${cls}">${prev}</span>`
      }

      // 列表标记（放到 span 外）
      result += `\n${listMark}`

      lastIndex = index + m.length
    })

    // 最后一段
    const rest = content.slice(lastIndex)
    if (rest) {
      result += `<span class="${cls}">${rest}</span>`
    }

    return result
  })
}

const cleanStr = (str = '') => {
  return str.replace(/[\u200B-\u200D\uFEFF]/g, '')
}

/**
 * 比对文本，输出 diff 后的 HTML 字符串
 * @param {string} oldText - 旧文本
 * @param {string} newText - 新文本
 * @param {string[]} ignoreWords - 忽略比对的字符串数组，这些内容不会标记为添加或删除
 * @returns {string} 包含 diff 标记的 HTML 字符串
 */
export const compareText = (oldText, newText, ignoreWords = []) => {
  oldText = cleanStr(oldText)
  newText = cleanStr(newText)
  const protectMarkdownImageToken = (text = '') => {
    const tokenMap = {}
    let index = 0

    const protectedText = text.replace(/!?\[[^\]]*图片[^\]]*\]\([^)]+\)/g, (match) => {
      const token = `__MD_IMAGE_TOKEN_${index}__`
      tokenMap[token] = match
      index += 1
      return token
    })

    return {
      text: protectedText,
      tokenMap,
    }
  }

  const restoreMarkdownImageToken = (text = '', tokenMap = {}) => {
    return text.replace(/__MD_IMAGE_TOKEN_\d+__/g, (token) => tokenMap[token] || token)
  }

  // 移除已有的 diff 标签
  oldText = removeDiffSpan(oldText)

  const oldProtected = protectMarkdownImageToken(oldText)
  const newProtected = protectMarkdownImageToken(newText)

  // 执行单词级别比对
  const diffData = diffWords(oldProtected.text, newProtected.text)

  // 构建忽略匹配的正则表达式（转义特殊字符）
  const ignorePatterns = ignoreWords.map((word) => {
    // 转义正则特殊字符
    const escapedWord = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    return new RegExp(escapedWord, 'g')
  })

  /**
   * 检查内容是否属于忽略列表
   * @param {string} content - 待检查的内容
   * @returns {boolean} 是否应忽略
   */
  const shouldIgnore = (content) => {
    if (!ignorePatterns.length) return false
    return ignorePatterns.some((pattern) => pattern.test(content))
  }

  const html = diffData
    .map((part) => {
      const partValue = restoreMarkdownImageToken(
        part.value,
        part.added
          ? newProtected.tokenMap
          : part.removed
            ? oldProtected.tokenMap
            : { ...oldProtected.tokenMap, ...newProtected.tokenMap },
      )

      // 如果是添加的部分
      if (part.added) {
        // 检查是否在忽略列表中
        if (shouldIgnore(partValue)) {
          return partValue
        }
        return `<span class="add-text">${partValue}</span>`
      }
      // 如果是删除的部分
      if (part.removed) {
        // 检查是否在忽略列表中
        if (shouldIgnore(partValue)) {
          return partValue
        }
        return `<span class="remove-text">${partValue}</span>`
      }
      // 未变化的部分直接返回
      return partValue
    })
    .join('')

  return splitListSpan(html)
}

export const removeAtBlock = (str = '') => {
  return str.replace(/@[a-zA-Z]+\[[\s\S]*?\]/g, '')
}

export const removeAfterAt = (str = '') => {
  return str.replace(/@[a-zA-Z]+[\s\S]*$/, '')
}

export const formatDateLabel = (str = '') => {
  if (!str) return ''

  // 2026-02 → 2026年02月
  if (/^\d{4}-\d{2}$/.test(str)) {
    const [year, month] = str.split('-')
    return `${year}年${month}月`
  }

  // 2026-Q1 → 2026年Q1
  if (/^\d{4}-Q[1-4]$/.test(str)) {
    const [year, quarter] = str.split('-')
    return `${year}年${quarter}`
  }

  // 2025 → 2025年
  if (/^\d{4}$/.test(str)) {
    return `${str}年`
  }

  // 兜底：原样返回
  return str
}

// url → base64
export const toBase64 = (url) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'

    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0)
      resolve(canvas.toDataURL('image/png'))
    }

    img.onerror = () => resolve('') // 防止某张图挂了导致整体失败

    img.src = url
  })
}

// 批量转 base64
export const toBase64List = async (list = []) => {
  if (!Array.isArray(list) || !list.length) return []
  const res = await Promise.all(list.map((url) => toBase64(url)))
  return res.filter(Boolean)
}

/**
 * 判断是否 HEIC 并转换成 PNG
 * @param {string | Blob} source - 图片 URL 或 Blob
 * @returns {Promise<string | null>} - 返回 Base64 URL，如果失败返回 null
 */
export async function convertHeicIfNeeded(source) {
  let isHeic = false
  let blob

  // 如果传入的是 URL，先 fetch 成 Blob
  if (typeof source === 'string') {
    const response = await fetch(source)
    blob = await response.blob()
  } else {
    blob = source
  }

  // 判断 MIME 类型
  const mime = blob.type.toLowerCase()
  if (mime === 'image/heic' || mime === 'image/heif') {
    isHeic = true
  } else {
    // 有些 HEIC 没设置 MIME，可以通过文件名后缀判断
    if (typeof source === 'string' && source.match(/\.heic$/i)) {
      isHeic = true
    }
  }

  // 如果是 HEIC，需要转换
  if (isHeic) {
    try {
      blob = await heic2any({ blob, toType: 'image/png' })
    } catch (err) {
      console.error('HEIC 转换失败:', err)
      return null
    }
  }

  // 将 Blob 转成 Base64
  return await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result) // Base64 URL
    reader.onerror = (err) => reject(err)
    reader.readAsDataURL(blob)
  })
}

export const getUrl = (res) => {
  let urls = res || []
  return urls.map((url) => {
    const { pathname, search } = new URL(url)
    return '/oss-service' + pathname + search
  })
}

// 判断是否在中国（避免国外误差）
const outOfChina = (lng, lat) => {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

const transformLat = (lng, lat) => {
  let ret = -100 + 2 * lng + 3 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += ((20 * Math.sin(6 * lng * Math.PI) + 20 * Math.sin(2 * lng * Math.PI)) * 2) / 3
  ret += ((20 * Math.sin(lat * Math.PI) + 40 * Math.sin((lat / 3) * Math.PI)) * 2) / 3
  ret += ((160 * Math.sin((lat / 12) * Math.PI) + 320 * Math.sin((lat * Math.PI) / 30)) * 2) / 3
  return ret
}

const transformLng = (lng, lat) => {
  let ret = 300 + lng + 2 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += ((20 * Math.sin(6 * lng * Math.PI) + 20 * Math.sin(2 * lng * Math.PI)) * 2) / 3
  ret += ((20 * Math.sin(lng * Math.PI) + 40 * Math.sin((lng / 3) * Math.PI)) * 2) / 3
  ret += ((150 * Math.sin((lng / 12) * Math.PI) + 300 * Math.sin((lng / 30) * Math.PI)) * 2) / 3
  return ret
}

// 高德坐标转WGS84
export const gcj02ToWgs84 = (lng, lat) => {
  if (outOfChina(lng, lat)) return [lng, lat]

  const a = 6378245.0
  const ee = 0.00669342162296594323

  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)

  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)

  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)

  const mgLat = lat + dLat
  const mgLng = lng + dLng

  return [lng * 2 - mgLng, lat * 2 - mgLat]
}

/**
 * 根据行政区划下钻层级/级别获取边界线抽稀容差 (单位: 度)
 * 规则：
 * - 一级：保持不变（0.001，约 100~110 米）
 * - 二级：精细一点（0.0003，约 30~33 米）
 * - 三级：不抽稀（0，保持原始精度）
 *
 * @param {number} depth - 导航下钻深度（1: 一级, 2: 二级, >=3: 三级）
 * @param {object} [item] - 区划项（支持 item.level）
 * @returns {number} 容差数值，0 表示不抽稀
 */
export function getAreaBoundaryTolerance(depth = 1, item = null) {
  // 若区划明确为村级(5)或导航深度>=3，完全不抽稀
  if (
    item?.level === AREA_LEVEL.VILLAGE ||
    item?.level === 5 ||
    item?.level === 'village' ||
    depth >= 3
  ) {
    return AREA_BOUNDARY_SIMPLIFY_TOLERANCE.LEVEL_3
  }

  // 二级：精细一点（0.0003）
  if (
    depth === 2 ||
    item?.level === AREA_LEVEL.STREET ||
    item?.level === 4 ||
    item?.level === 'street'
  ) {
    return AREA_BOUNDARY_SIMPLIFY_TOLERANCE.LEVEL_2
  }

  // 一级：保持不变（0.001）
  return AREA_BOUNDARY_SIMPLIFY_TOLERANCE.LEVEL_1
}

export function simplifyWKT(wkt, tolerance = AREA_BOUNDARY_SIMPLIFY_TOLERANCE.LEVEL_1) {
  if (!wkt) return ''
  // 容差小于等于 0 时不抽稀，直接返回原始 WKT
  if (!tolerance || tolerance <= 0) return wkt

  // 1️⃣ WKT → GeoJSON
  const geojson = parse(wkt)

  // 2️⃣ 抽稀（Douglas-Peucker）
  const simplified = turf.simplify(geojson, {
    tolerance,
    highQuality: true,
  })

  // 3️⃣ GeoJSON → WKT
  return convert(simplified)
}


export function parseMixedJson(str) {
  if (!str || typeof str !== 'string') return null

  const start = str.indexOf('[')
  const end = str.lastIndexOf(']')

  if (start === -1 || end === -1) return null

  const arrStr = str.slice(start, end + 1)

  try {
    const arr = JSON.parse(arrStr)

    // 如果是数组，取第一个对象
    if (Array.isArray(arr)) {
      return arr[0] ?? null
    }

    return null
  } catch (e) {
    console.error('解析失败:', e, arrStr)
    return null
  }
}

/**
 * 判断右侧数组中是否至少有一个元素存在于左侧数组中
 * @param {Array} leftArr - 左侧数组（被包含的集合）
 * @param {Array} rightArr - 右侧数组（待检查的集合）
 * @param {string} passMark - 有则直接返回 true
 * @returns {boolean} 如果左侧数组中有任何一个元素在左侧数组中存在，或者右侧数组包含指定的超级管理员角色，则返回 true，否则返回 false
 */
export const hasIntersection = (leftArr = [], rightArr = [], passMark = '') => {
  if (!Array.isArray(leftArr) || !Array.isArray(rightArr)) {
    return false
  }

  if (leftArr.includes(passMark)) {
    return true
  }

  // 使用 Set 优化查找性能，时间复杂度从 O(n*m) 降低到 O(n+m)
  const leftSet = new Set(leftArr)

  return rightArr.some((item) => leftSet.has(item))
}

export const hasRole = (ros) => {
  const userStore = useUserStore()
  const { userInfo } = storeToRefs(userStore)
  const roles = userInfo.value?.roles || []
  return hasIntersection(ros, roles, ROLE_MAP.SUPER_ADMIN)
}

// 判断是否存在权限
export const hasAuth = (auths) => {
  const userStore = useUserStore()
  const { buttonAuths } = storeToRefs(userStore)
  return hasIntersection(buttonAuths.value, auths, '*')
}

export const createLoading = ({
  target = document.body,
  text = '加载中...',
  textColor = '#fff',
  background = 'rgba(0,0,0,0.7)',
  spinnerColor = '#fff',
} = {}) => {
  // 生成唯一 class
  const customClass = `custom-loading-${Date.now()}`

  // 动态插入样式
  const style = document.createElement('style')
  style.innerHTML = `
    .${customClass} {
      background: ${background} !important;
    }
    .${customClass} .el-loading-text {
      color: ${textColor} !important;
    }
    .${customClass} .el-loading-spinner .path {
      stroke: ${spinnerColor} !important;
    }
  `
  document.head.appendChild(style)

  return ElLoading.service({
    lock: true,
    target,
    text,
    customClass,
  })
}

// 获取单个坐标点的Z值（高度）
export const getHeightFromPoint = async (point) => {
  let viewer = window._viewer // 假设 Cesium Viewer 实例挂在全局
  if (!viewer) return
  let [targetLon, targetLat] = point
  const cartographic = Cesium.Cartographic.fromDegrees(targetLon, targetLat)

  const sampledPositions = await Cesium.sampleTerrainMostDetailed(viewer.scene.terrainProvider, [cartographic])

  return sampledPositions[0].height
}

export const sleep = (ms) =>
  new Promise((resolve) => {
    setTimeout(resolve, ms)
  })

export const getPhotoUrls = async (ids, needBase64 = false) => {
  if (!ids || (Array.isArray(ids) && !ids.length)) return []
  const idArray = Array.isArray(ids) ? ids : String(ids).split(',')

  const resPhotos = (await getImageUrlById({ ids: idArray })) || []
  const transPhoto = getUrl(resPhotos)

  if (needBase64) {
    return await Promise.all(
      transPhoto.map(async (item) => {
        return await convertHeicIfNeeded(item)
      }),
    )
  }

  return transPhoto
}

/**
 * 将 AI 识别对象转换为 label/value 展示结构
 * @param {Object} data AI原始数据对象
 * @param {Array} enumList 枚举配置
 * @returns {Array} 转换后的数组
 */
export function transformAiDataToOptions(data, enumList) {
  if (!Array.isArray(enumList)) return []
  if (!data) return enumList

  return enumList.map((item) => ({
    label: item.label,
    value: item.value,
    dataValue: data?.[item.value] ?? null,
  }))
}

// 经纬度格式化
export function convertToDMS(coordinate, fixedSeconds = 2) {
  if (isNaN(coordinate) || coordinate === null) {
    return ''
  }
  const absolute = Math.abs(coordinate)
  const degrees = Math.floor(absolute)
  const minutesNotTruncated = (absolute - degrees) * 60
  const minutes = Math.floor(minutesNotTruncated)
  const seconds = ((minutesNotTruncated - minutes) * 60).toFixed(fixedSeconds)
  return `${degrees}°${minutes}'${seconds}"`
}
