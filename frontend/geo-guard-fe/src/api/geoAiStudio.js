const RAG_BASE = '/api/dizai/ai/rag'
const AGENT_BASE = '/geo-ai-agent'

async function request(base, path, options = {}) {
  const tokenKey = import.meta.env.VITE_APP_TOKEN_KEY || 'bwy-token'
  const token = base === RAG_BASE ? localStorage.getItem(tokenKey) : null
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      ...options.headers,
      ...(base === RAG_BASE ? { clientid: import.meta.env.VITE_APP_CLIENT_ID || 'geo-local' } : {}),
      ...(token ? { [tokenKey]: token } : {}),
    },
  })
  const bodyText = await response.text()
  let body = {}
  if (bodyText) {
    try {
      body = JSON.parse(bodyText)
    } catch {
      body = { detail: bodyText }
    }
  }
  if (!response.ok || body?.code >= 400) {
    throw new Error(body?.detail || body?.msg || `请求失败（${response.status}）`)
  }
  return body
}

const jsonOptions = (data) => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data),
})

export const getRagHealth = () => request(RAG_BASE, '/health')
export const getAgentHealth = () => request(AGENT_BASE, '/health')
export const getRagPipeline = () => request(RAG_BASE, '/api/v1/pipeline')
export const getKnowledgeBaseTypes = () => request(RAG_BASE, '/api/v1/types')
export const getRagSupportedFileTypes = () => request(RAG_BASE, '/api/v1/files/supported-types')
export const getRagStatistics = () => request(RAG_BASE, '/api/v1/stats')
export const getDocuments = () => request(RAG_BASE, '/api/v1/documents')
export const getDocumentChunks = (documentId) =>
  request(RAG_BASE, `/api/v1/documents/${encodeURIComponent(documentId)}/chunks`)
export const getChunkPresets = () => request(RAG_BASE, '/api/v1/chunk-presets')
export const getKnowledgeBases = () => request(RAG_BASE, '/api/v1/knowledge-bases')
export const createKnowledgeBase = (payload) =>
  request(RAG_BASE, '/api/v1/knowledge-bases', jsonOptions(payload))
export const updateKnowledgeBase = (id, payload) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}`, {
    ...jsonOptions(payload),
    method: 'PUT',
  })
export const deleteKnowledgeBase = (id) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}`, { method: 'DELETE' })
export const getKnowledgeBaseDocuments = (id) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/documents`)
export const getKnowledgeBaseFolders = (id) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/folders`)
export const renameKnowledgeBaseFolder = (id, folderId, name) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/folders/${encodeURIComponent(folderId)}/rename`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name }),
  })
export const deleteKnowledgeBaseFolder = (id, folderId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/folders/${encodeURIComponent(folderId)}`, { method: 'DELETE' })
export const moveKnowledgeBaseDocument = (id, documentId, folderId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/documents/${encodeURIComponent(documentId)}/move`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ folder_id: folderId }),
  })
export const searchKnowledgeBaseDocuments = (id, query) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/documents/search?q=${encodeURIComponent(query)}`)
export const createKnowledgeBaseFolder = (id, name, parentId = null) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/folders`, jsonOptions({ name, parent_id: parentId }))
export const getKnowledgeBaseChunks = (knowledgeBaseId, documentId) =>
  request(
    RAG_BASE,
    `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents/${encodeURIComponent(documentId)}/chunks`,
  )
export const deleteKnowledgeBaseDocument = (knowledgeBaseId, documentId) =>
  request(
    RAG_BASE,
    `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents/${encodeURIComponent(documentId)}`,
    { method: 'DELETE' },
  )
export const deleteKnowledgeBaseDocuments = (knowledgeBaseId, documentIds) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents/batch`, {
    method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ document_ids: documentIds }),
  })
export const parseKnowledgeBaseDocuments = (knowledgeBaseId, documentIds) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents/parse`, jsonOptions({ document_ids: documentIds }))
export const indexKnowledgeBaseDocuments = (knowledgeBaseId, documentIds) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents/index`, jsonOptions({ document_ids: documentIds }))
export const testKnowledgeBaseConnection = (id) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(id)}/connection-test`, jsonOptions({}))
export const uploadKnowledgeBaseFile = (knowledgeBaseId, file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledge_base_id', knowledgeBaseId)
  if (options.folderId) formData.append('folder_id', options.folderId)
  if (options.relativePath) formData.append('relative_path', options.relativePath)
  return request(RAG_BASE, '/api/v1/documents/upload', { method: 'POST', body: formData })
}
export const retrieveKnowledgeBase = (knowledgeBaseId, payload) =>
  request(RAG_BASE, '/api/v1/debug/retrieve', jsonOptions({
    ...payload,
    filters: { ...(payload.filters || {}), knowledge_base_id: knowledgeBaseId },
  }))
export const evaluateKnowledgeBase = (knowledgeBaseId, payload) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/runs`, jsonOptions(payload))
    .then((run) => ({ ...run.result, run_id: run.id, created_at: run.created_at }))
export const uploadKnowledgeBaseEvaluationDataset = (knowledgeBaseId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/datasets/upload`, { method: 'POST', body: formData })
}
export const getKnowledgeBaseEvaluationDatasets = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/datasets`)
export const getKnowledgeBaseEvaluationDataset = (knowledgeBaseId, datasetId, page = 1, pageSize = 50) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/datasets/${encodeURIComponent(datasetId)}?page=${page}&page_size=${pageSize}`)
export const deleteKnowledgeBaseEvaluationDataset = (knowledgeBaseId, datasetId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/datasets/${encodeURIComponent(datasetId)}`, { method: 'DELETE' })
export const runKnowledgeBaseEvaluation = (knowledgeBaseId, payload) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/runs`, jsonOptions(payload))
export const getKnowledgeBaseEvaluationRuns = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/runs`)
export const getKnowledgeBaseEvaluationRun = (knowledgeBaseId, runId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/evaluation/runs/${encodeURIComponent(runId)}`)
export const getKnowledgeBaseRetrievalConfig = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/query-params`)
export const saveKnowledgeBaseRetrievalConfig = (knowledgeBaseId, config) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/query-params`, { ...jsonOptions(config), method: 'PUT' })
    .then((response) => response)
export const generateKnowledgeBaseMindmap = (knowledgeBaseId, documentIds = []) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/mindmap/generate`, jsonOptions({ document_ids: documentIds }))
export const getKnowledgeBaseMindmap = (knowledgeBaseId, documentId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/mindmap${documentId ? `?document_id=${encodeURIComponent(documentId)}` : ''}`)
export const getKnowledgeBaseMindmapFiles = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/mindmap/files`)
export const getKnowledgeBaseMindmapDiff = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/mindmap/diff`)
export const getKnowledgeBaseGraph = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/graph`)
export const getKnowledgeBaseGraphStatus = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/graph-build/status`)
export const buildKnowledgeBaseGraph = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/graph-build/index`, jsonOptions({}))
export const configureKnowledgeBaseGraph = (knowledgeBaseId, config) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/graph-build/config`, jsonOptions(config))
export const resetKnowledgeBaseGraph = (knowledgeBaseId) =>
  request(RAG_BASE, `/api/v1/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/graph-build/reset`, jsonOptions({}))
export const testEmbeddingProvider = (config) =>
  request(RAG_BASE, '/api/v1/providers/embedding/test', jsonOptions(config))
export const testRerankerProvider = (config) =>
  request(RAG_BASE, '/api/v1/providers/reranker/test', jsonOptions(config))
export const testParserProvider = (file, mineruApiUri) => {
  const formData = new FormData()
  formData.append('file', file)
  if (mineruApiUri) formData.append('mineru_api_uri', mineruApiUri)
  return request(RAG_BASE, '/api/v1/providers/parser/test', { method: 'POST', body: formData })
}
export const getWorkflows = () => request(AGENT_BASE, '/api/v1/workflows')

export const uploadRagFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request(RAG_BASE, '/api/v1/documents/upload', { method: 'POST', body: formData })
}

export const addRagText = (fileName, text) =>
  request(RAG_BASE, '/api/v1/documents/text', jsonOptions({ file_name: fileName, text }))

export const debugRagRetrieval = (query, topK) =>
  request(RAG_BASE, '/api/v1/debug/retrieve', jsonOptions({ query, final_top_k: topK }))

function parseSseEvent(rawEvent) {
  const lines = rawEvent.split(/\r?\n/)
  const event = lines
    .find((line) => line.startsWith('event:'))
    ?.slice(6)
    .trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
    .join('\n')
  return event && data ? { event, payload: JSON.parse(data) } : null
}

export async function runAgentStream(requestBody, onTrace) {
  const response = await fetch(`${AGENT_BASE}/api/v1/run/stream`, {
    ...jsonOptions(requestBody),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body?.detail || `流程运行失败（${response.status}）`)
  }
  if (!response.body) {
    throw new Error('当前浏览器没有可读取的 SSE 响应流')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let result = null
  let finished = false
  while (!finished) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    for (const rawEvent of events) {
      const parsed = parseSseEvent(rawEvent)
      if (parsed?.event === 'trace') onTrace(parsed.payload)
      if (parsed?.event === 'result') result = parsed.payload
      if (parsed?.event === 'error') throw new Error(parsed.payload?.detail || '流程运行失败')
      if (parsed?.event === 'done') finished = true
    }
    if (done) finished = true
  }
  return result
}
