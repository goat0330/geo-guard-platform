import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'
import { fileURLToPath, URL } from 'node:url'
import { mars3dPlugin } from 'vite-plugin-mars3d'
import postCssPxToRem from 'postcss-pxtorem'
import { codeInspectorPlugin } from 'code-inspector-plugin'
import svgLoader from 'vite-svg-loader'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const useDevHttps = env.VITE_DEV_HTTPS !== 'false'

  return {
    plugins: [
      vue(),
      ...(useDevHttps ? [basicSsl()] : []),
      svgLoader({ defaultImport: 'url' }),
      codeInspectorPlugin({
        bundler: 'vite',
      }),
      // Mars3D 本地包为 UMD 格式。生产构建时使用插件的静态加载模式，
      // 避免 Rollup 将其构造函数裁剪为 undefined。
      mars3dPlugin({ useStatic: true }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    css: {
      postcss: {
        plugins: [
          postCssPxToRem({
            rootValue: 16, // 基准大小，对应 1920px 设计稿
            propList: ['*'],
            unitPrecision: 4,
            selectorBlackList: ['.norem', 'html'],
          }),
        ],
      },
    },
    server: {
      port: 5174,
      host: '127.0.0.1',
      proxy: {
        '/geo-ai-rag': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/geo-ai-rag/, '/dizai/ai/rag'),
        },
        '/geo-ai-agent': {
          target: env.VITE_GEO_AGENT_SERVICE_URL || 'http://127.0.0.1:8011',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/geo-ai-agent/, ''),
        },
        '/geo-langgraph-studio': {
          target: 'http://127.0.0.1:2024',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/geo-langgraph-studio/, ''),
        },
        '^/api/knowledge': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => `/dizai/ai/rag${path}`,
        },
        '^/api/evaluation': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => `/dizai/ai/rag${path}`,
        },
        '^/api/workspace': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => `/dizai/ai/rag${path}`,
        },
        '^/api/graph': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api\/graph/, '/dizai/ai/rag/api/graph'),
        },
        '/geoserver': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          secure: false,
        },
        '/api': {
          target: env.VITE_APP_SERVER_URL || 'http://127.0.0.1:8007',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
        '/tts': {
          target: 'http://127.0.0.1:6006',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/tts/, ''),
        },
        '/oss-service': {
          target: 'http://127.0.0.1:9000',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/oss-service/, ''),
        },
        '/images-service': {
          target: 'http://127.0.0.1:10099',
          changeOrigin: true,
          rewrite: (path) => {
            // console.log('-----', path.replace(/^\/images-service/, ''))
            return path.replace(/^\/images-service/, '')
          },
        },
        '/files-service': {
          target: 'http://127.0.0.1:10099',
          changeOrigin: true,
          rewrite: (path) => {
            // console.log('-----', path.replace(/^\/files-service/, ''))
            return path.replace(/^\/files-service/, '')
          },
        },
        // 与恩施地灾保持一致，转发天地图 WMTS 请求。
        '/t_map': {
          target: 'http://t0.tianditu.com',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/t_map/, ''),
        },
        '/api-ws': {
          target: 'wss://127.0.0.1:8012',
          ws: true,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api-ws/, ''),
        },
      },
    },
  }
})
