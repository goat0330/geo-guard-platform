import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import {
  getEmbeddedRagAuthHeaders,
  isEmbeddedRagApi,
  isEmbeddedRagApiPath,
  isEmbeddedRagMode,
  isEmbeddedRagRoute
} from '../../src/utils/embeddedRag.js'

const originalWindow = globalThis.window

afterEach(() => {
  if (originalWindow === undefined) delete globalThis.window
  else globalThis.window = originalWindow
})

test('embedded RAG mode is enabled by the explicit embed query', () => {
  const embeddedWindow = { location: { search: '?embed=rag', origin: 'http://localhost' } }
  embeddedWindow.parent = embeddedWindow
  globalThis.window = embeddedWindow

  assert.equal(isEmbeddedRagMode(), true)
  assert.equal(isEmbeddedRagApi('/api/knowledge/databases'), true)

  embeddedWindow.location.search = ''
  assert.equal(isEmbeddedRagMode(), false)
  assert.equal(isEmbeddedRagApi('/api/knowledge/databases'), false)
})

test('embedded knowledge routes remain login-free when an older iframe URL lacks the embed query', () => {
  const embeddedWindow = {
    location: { search: '', hash: '#/extensions?tab=knowledge', origin: 'http://localhost' }
  }
  embeddedWindow.parent = {}
  globalThis.window = embeddedWindow

  assert.equal(isEmbeddedRagMode(), true)
  assert.equal(isEmbeddedRagApi('/api/knowledge/databases'), true)

  embeddedWindow.location.hash = '#/extensions?tab=skill'
  assert.equal(isEmbeddedRagMode(), false)
  embeddedWindow.location.hash = '#/login?redirect=/extensions'
  assert.equal(isEmbeddedRagMode(), false)
})

test('embedded RAG mode permits knowledge routes and rejects unrelated pages', () => {
  assert.equal(isEmbeddedRagRoute('/extensions'), true)
  assert.equal(isEmbeddedRagRoute('/extensions/knowledgebase/kb_123'), true)
  assert.equal(isEmbeddedRagRoute('/extensions/knowledgebase/kb_123/evaluation/ds_1'), true)
  assert.equal(isEmbeddedRagRoute('/login'), false)
  assert.equal(isEmbeddedRagRoute('/agent'), false)
  assert.equal(isEmbeddedRagRoute('/extensions/skill/example'), false)
})

test('embedded RAG API allowlist excludes unrelated Yuxi APIs', () => {
  assert.equal(isEmbeddedRagApiPath('/api/knowledge/databases?limit=10'), true)
  assert.equal(isEmbeddedRagApiPath('/api/evaluation/runs'), true)
  assert.equal(isEmbeddedRagApiPath('/api/workspace/knowledge/tree'), true)
  assert.equal(isEmbeddedRagApiPath('/api/workspace/tree?path=/'), true)
  assert.equal(isEmbeddedRagApiPath('/api/workspace/directory'), true)
  assert.equal(isEmbeddedRagApiPath('/api/workspace/tree/private'), false)
  assert.equal(isEmbeddedRagApiPath('/api/workspace/file'), false)
  assert.equal(isEmbeddedRagApiPath('/api/knowledge-admin/users'), false)
  assert.equal(isEmbeddedRagApiPath('/api/agent'), false)
  assert.equal(isEmbeddedRagApiPath('https://other.example/api/knowledge/databases'), false)
})

test('embedded RAG requests use the existing host session and client ID', () => {
  const storage = { getItem: (key) => key === 'bwy-token' ? 'host-session' : null }
  assert.deepEqual(getEmbeddedRagAuthHeaders(storage), {
    'bwy-token': 'host-session', clientid: 'geo-local'
  })
  assert.deepEqual(getEmbeddedRagAuthHeaders({ getItem: () => null }), { clientid: 'geo-local' })
  assert.deepEqual(getEmbeddedRagAuthHeaders({ getItem: () => 'custom-session' }, 'custom-client', 'custom-token'), {
    'custom-token': 'custom-session', clientid: 'custom-client'
  })
})
