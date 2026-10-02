const RAG_API_PREFIXES = ['/api/knowledge', '/api/evaluation', '/api/workspace/knowledge']
const RAG_WORKSPACE_PATHS = ['/api/workspace/tree', '/api/workspace/directory']

export const EMBEDDED_RAG_ROUTE = '/extensions?tab=knowledge'

export function isEmbeddedRagMode() {
  if (typeof window === 'undefined') return false
  if (new URLSearchParams(window.location?.search || '').get('embed') === 'rag') return true
  if (window.parent === window) return false

  const [path, query = ''] = (window.location?.hash || '').replace(/^#/, '').split('?')
  return (
    /^\/extensions\/knowledgebase(?:\/|$)/.test(path) ||
    (path === '/extensions' && new URLSearchParams(query).get('tab') === 'knowledge')
  )
}

export function isEmbeddedRagRoute(path) {
  return path === '/extensions' || /^\/extensions\/knowledgebase(?:\/|$)/.test(path)
}

export function isEmbeddedRagApiPath(url, origin = 'http://localhost') {
  let pathname
  try {
    const target = new URL(url, origin)
    if (target.origin !== new URL(origin).origin) return false
    pathname = target.pathname
  } catch {
    return false
  }

  return RAG_WORKSPACE_PATHS.includes(pathname) ||
    RAG_API_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))
}

export function isEmbeddedRagApi(url) {
  return isEmbeddedRagMode() && isEmbeddedRagApiPath(url, window.location.origin)
}

export function getEmbeddedRagAuthHeaders(storage, clientId = 'geo-local', tokenKey = 'bwy-token') {
  const token = storage.getItem(tokenKey)
  return token ? { [tokenKey]: token, clientid: clientId } : { clientid: clientId }
}
