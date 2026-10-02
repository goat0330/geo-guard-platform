import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import vm from 'node:vm'

const root = new URL('../', import.meta.url)

async function loadModule(path, imports, development = false) {
  const context = vm.createContext({ console })
  const module = new vm.SourceTextModule(await readFile(new URL(path, root), 'utf8'), {
    context,
    initializeImportMeta(meta) { meta.env = { DEV: development, PROD: !development } },
  })
  await module.link((name) => {
    const exports = imports[name]
    assert.ok(exports, `Unexpected dependency: ${name}`)
    return new vm.SyntheticModule(Object.keys(exports), function () {
      for (const [key, value] of Object.entries(exports)) this.setExport(key, value)
    }, { context })
  })
  await module.evaluate()
  return module.namespace.default
}

test('production router registers the AI Studio workspace', async () => {
  const router = await loadModule('src/router/index.js', {
    'vue-router': { createRouter: (options) => options, createWebHistory: () => ({}) },
    '@/router/permission.js': { default: () => {} },
  })
  const route = router.routes.find((item) => item.path === '/').children.find((item) => item.path === 'ai-studio')
  assert.ok(route)
  assert.equal(route.name, 'GeoAiStudio')
  assert.equal(typeof route.component, 'function')
})

for (const development of [false, true]) {
  test(`AI Studio uses host authentication in ${development ? 'development' : 'production'}`, async () => {
    let guard
    let connections = 0
    const user = { token: '', userInfo: { user: {} },
      startConnection() { connections++ }, startHeartbeat() {} }
    const setup = await loadModule('src/router/permission.js', {
      '@/store/user': { useUserStore: () => user }, '@/utils/index.js': { getToken: () => '' },
    }, development)
    setup({ beforeEach(handler) { guard = handler } })
    const target = { path: '/ai-studio', fullPath: '/ai-studio' }
    let redirected
    await guard(target, {}, (path) => { redirected = path })
    assert.equal(redirected, '/login?redirect=%2Fai-studio')
    assert.equal(connections, 0)
    user.token = 'test-host-session'
    await guard(target, {}, (path) => { redirected = path })
    assert.equal(redirected, undefined)
    assert.equal(connections, 1)
  })
}
