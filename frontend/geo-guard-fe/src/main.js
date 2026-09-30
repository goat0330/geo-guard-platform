import { createApp } from 'vue'
import './utils/cesiumIon.js'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VueDOMPurifyHTML from 'vue-dompurify-html'
import { auth } from '@/utils/directive.js'
import 'katex/dist/katex.min.css'
import '@/assets/iconfont/iconfont.css'
import '@/assets/iconfont/iconfont.js'
import './utils/rem.js'
import './assets/styles/font.css'
import './assets/styles/reset.css'
import './assets/styles/global.less'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(VueDOMPurifyHTML)
app.directive('auth', auth)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
