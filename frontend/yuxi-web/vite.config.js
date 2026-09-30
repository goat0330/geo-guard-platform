import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { providers } from '@opencode-ai/models/snapshot'

// 快照只在构建进程读取，浏览器只接收展示字段；模型覆盖随锁定依赖更新。
const modelMetadataPlugin = {
  name: 'model-display-metadata',
  resolveId(id) {
    if (id === 'virtual:model-display-metadata') return '\0' + id
  },
  load(id) {
    if (id !== '\0virtual:model-display-metadata') return
    const catalog = Object.fromEntries(
      Object.entries(providers).map(([providerId, provider]) => [
        providerId,
        {
          models: Object.fromEntries(
            Object.entries(provider.models).map(([modelId, model]) => [
              modelId,
              {
                modalities: { input: model.modalities?.input },
                limit: { context: model.limit?.context },
                cost: model.cost
              }
            ])
          )
        }
      ])
    )
    return `export const providers = ${JSON.stringify(catalog)}`
  }
}

export default defineConfig(({ mode }) => {
  // eslint-disable-next-line no-undef
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: '/yuxi/',
    plugins: [vue(), modelMetadataPlugin],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5175,
      host: '127.0.0.1',
      proxy: {
        '^/api/knowledge': {
          target: env.VITE_API_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          rewrite: (path) => `/dizai/ai/rag${path}`
        },
        '^/api/evaluation': {
          target: env.VITE_API_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          rewrite: (path) => `/dizai/ai/rag${path}`
        },
        '^/api': {
          target: env.VITE_API_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '^/minio/public/': {
          target: env.VITE_MINIO_URL || 'http://127.0.0.1:8010',
          changeOrigin: true
        }
      },
      watch: {
        usePolling: true,
        ignored: ['**/node_modules/**', '**/dist/**']
      }
    },
    build: {
      outDir: fileURLToPath(new URL('../geo-guard-fe/public/yuxi', import.meta.url)),
      emptyOutDir: true
    }
  }
})
