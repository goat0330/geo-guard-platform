import dayjs from 'dayjs'

const DATABASE_NAME = 'geo-guard-risk-assessment'
const DATABASE_VERSION = 1
const META_STORE = 'dataset-meta'
const CHUNK_STORE = 'dataset-chunks'
const CHUNK_SIZE = 100

export const RISK_ASSESSMENT_CACHE_COUNTY = '彭水苗族土家族自治县'

let databasePromise

const requestToPromise = (request) => new Promise((resolve, reject) => {
  request.onsuccess = () => resolve(request.result)
  request.onerror = () => reject(request.error)
})

const transactionToPromise = (transaction) => new Promise((resolve, reject) => {
  transaction.oncomplete = () => resolve()
  transaction.onerror = () => reject(transaction.error)
  transaction.onabort = () => reject(transaction.error)
})

const openDatabase = () => {
  if (!('indexedDB' in window)) return Promise.reject(new Error('IndexedDB is not supported'))
  if (databasePromise) return databasePromise

  databasePromise = new Promise((resolve, reject) => {
    const request = window.indexedDB.open(DATABASE_NAME, DATABASE_VERSION)

    request.onupgradeneeded = () => {
      const database = request.result
      if (!database.objectStoreNames.contains(META_STORE)) {
        database.createObjectStore(META_STORE, { keyPath: 'key' })
      }
      if (!database.objectStoreNames.contains(CHUNK_STORE)) {
        database.createObjectStore(CHUNK_STORE, { keyPath: 'key' })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => {
      databasePromise = undefined
      reject(request.error)
    }
    request.onblocked = () => {
      databasePromise = undefined
      reject(new Error('IndexedDB upgrade is blocked'))
    }
  })

  return databasePromise
}

const yieldToMainThread = () => new Promise((resolve) => {
  window.setTimeout(resolve, 0)
})

export const getRiskAssessmentCacheKey = (assessmentDate = dayjs().format('YYYY-MM-DD')) => (
  `${assessmentDate}|${RISK_ASSESSMENT_CACHE_COUNTY}`
)

/**
 * 读取当天风险评价全量缓存。数据分块读取，避免单次结构化克隆长时间占用主线程。
 * @param {string} assessmentDate 评估日期
 * @returns {Promise<object[]|null>} 命中返回全量数据，未命中返回 null
 */
export const readRiskAssessmentCache = async (assessmentDate) => {
  const database = await openDatabase()
  const cacheKey = getRiskAssessmentCacheKey(assessmentDate)
  const metaTransaction = database.transaction(META_STORE, 'readonly')
  const metadata = await requestToPromise(metaTransaction.objectStore(META_STORE).get(cacheKey))
  if (!metadata || metadata.assessmentDate !== assessmentDate) return null

  const rows = []
  for (let index = 0; index < metadata.chunkCount; index += 1) {
    const transaction = database.transaction(CHUNK_STORE, 'readonly')
    const chunkKey = `${cacheKey}|${String(index).padStart(6, '0')}`
    const chunk = await requestToPromise(transaction.objectStore(CHUNK_STORE).get(chunkKey))
    if (!Array.isArray(chunk?.rows)) return null
    rows.push(...chunk.rows)
    await yieldToMainThread()
  }

  return rows
}

/**
 * 替换风险评价缓存。写入前清空所有历史数据，元数据最后写入以避免读到不完整数据。
 * @param {string} assessmentDate 评估日期
 * @param {object[]} rows 当天全量风险评价数据
 * @returns {Promise<void>}
 */
export const replaceRiskAssessmentCache = async (assessmentDate, rows) => {
  if (!Array.isArray(rows)) return

  const database = await openDatabase()
  const cacheKey = getRiskAssessmentCacheKey(assessmentDate)
  const clearTransaction = database.transaction([META_STORE, CHUNK_STORE], 'readwrite')
  clearTransaction.objectStore(META_STORE).clear()
  clearTransaction.objectStore(CHUNK_STORE).clear()
  await transactionToPromise(clearTransaction)

  const chunkCount = Math.ceil(rows.length / CHUNK_SIZE)
  for (let index = 0; index < chunkCount; index += 1) {
    const transaction = database.transaction(CHUNK_STORE, 'readwrite')
    transaction.objectStore(CHUNK_STORE).put({
      key: `${cacheKey}|${String(index).padStart(6, '0')}`,
      rows: rows.slice(index * CHUNK_SIZE, (index + 1) * CHUNK_SIZE),
    })
    await transactionToPromise(transaction)
    await yieldToMainThread()
  }

  const metaTransaction = database.transaction(META_STORE, 'readwrite')
  metaTransaction.objectStore(META_STORE).put({
    key: cacheKey,
    assessmentDate,
    county: RISK_ASSESSMENT_CACHE_COUNTY,
    chunkCount,
    rowCount: rows.length,
    updatedAt: Date.now(),
  })
  await transactionToPromise(metaTransaction)
}
