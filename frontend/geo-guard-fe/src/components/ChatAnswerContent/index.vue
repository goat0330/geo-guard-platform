<template>
  <div class="text-show" @mouseover="hoverMsg">
    <div
      v-if="msg.think"
      :class="{
        'think-content': true,
      }"
    >
      <i v-if="isCollapsed" class="scale-btn iconfont icon-enlarge" @click="enlargeThinkDetail(false)" />
      <i v-else class="scale-btn iconfont icon-shrink" @click="enlargeThinkDetail(true)" />
      <div v-if="msg.thinking" class="status-thinking think-animate">深度思考中</div>
      <div v-else class="status-done">已完成思考</div>
      <div
        :class="{
          'think-content__inner': true,
          'is-thinking': msg.thinking,
          'hideen-content': isCollapsed && !msg.thinking,
        }"
      >
        <div v-if="msg.dbs && msg.dbs.length" class="quote-box">
          <div class="quote-title">找到{{ msg.dbs.length }}篇参考文献：</div>
          <div class="quote-content">
            <div
              v-for="(db, key) in msg.dbs"
              :key="key"
              :class="['each-quote', { 'is-link': canOpenReference(db) }]"
              @click="canOpenReference(db) && checkFile(db)"
              :num="key"
            >
              <span>[{{ key + 1 }}]</span>
              <el-link v-if="db.source_url" type="primary">
                {{ getReferenceTitle(db) }}
              </el-link>
              <div v-else>{{ getReferenceTitle(db) }}</div>
            </div>
          </div>
        </div>
        <HtmlRender
          class="ai-msg__inner"
          :sseData="msg.think"
          :is-complete="isComplete"
          @scroll-to-bottom="scrollBottom"
        ></HtmlRender>
      </div>
    </div>
    <ImageRecognitionResult v-if="msg.imageRecognitionResult" :data="msg.imageRecognitionResult" />
    <HtmlRender
      ref="contentHtmlRenderRef"
      class="ai-msg__inner"
      :sseData="sseData"
      :is-complete="isComplete"
      @scroll="scrollBottom"
      @scroll-to-bottom="scrollBottom"
    />
    <ChartRender ref="chartRenderRef" v-if="isComplete && msg.chartDataStr" :data="msg.chartDataStr"></ChartRender>

    <div v-if="msg.pureJson?.type === 'report'" class="analysis-result">
      <WeatherReport
        :has-done="msg.completed ?? msg.done ?? isComplete"
        :has-register="msg.pureJsonHasRegister"
        :data="msg.pureJson?.data"
        :ext-params="{
          conversationId: msg.conversationId,
          messageId: msg.messageId || msg.id,
        }"
        :isHistory="msg.reportHistory"
        @scroll-to-bottom="scrollBottom"
      ></WeatherReport>
    </div>

    <el-popover
      v-if="showPopover"
      v-model:visible="showPopover"
      placement="bottom"
      :width="400"
      :virtual-ref="triggerDom"
    >
      <div class="popover-quote-title">
        <strong>参考引用：</strong>
        <ElLink type="primary" @click="downloadFile(quoteArticle?.metadata || quoteArticle)">
          {{ getReferenceTitle(quoteArticle) }}
        </ElLink>
        <!-- <strong>{{ quoteArticle.document_name }}</strong> -->
      </div>
      <div class="popover-quote-content">{{ quoteArticle?.content }}</div>
    </el-popover>

    <!-- 关联问题 -->
    <div v-if="showRefs && isComplete && msg.refQ?.length" class="refer-question-box">
      <div v-for="(q, key) in msg.refQ" :key="key" class="each-ques">
        <span @click="emits('send', q)">
          {{ q }}
          <el-icon><ArrowRight /></el-icon>
        </span>
      </div>
    </div>
  </div>
  <!-- 文件阅读器 -->
  <el-dialog v-model="pdfReaderDialogVisible" title="参考文献" top="10vh" width="70%">
    <div class="pdf-content">
      <PdfReader v-if="pdfReaderDialogVisible" :info="dbInfo"></PdfReader>
    </div>
  </el-dialog>
</template>
<script setup>
import HtmlRender from '@/views/chatEngine/chatting/ChatRoom/HtmlRender.vue'
import { ElLink } from 'element-plus'
import { nextTick, ref } from 'vue'
import { downloadFileWithId } from '@/utils/index.js'
import ChartRender from '@/views/chatEngine/chatting/ChatRoom/ChartRender.vue'
import PdfReader from '@/views/chatEngine/chatting/PdfReader/index.vue'
import WeatherReport from '@/views/chatEngine/chatting/ChatRoom/WeatherReport.vue'
import ImageRecognitionResult from '@/components/ImageRecognitionResult/index.vue'

const emits = defineEmits(['scroll', 'send'])
const triggerDom = ref()
const isCollapsed = ref(true)

/**
 * 切换思考过程的展开状态。
 * @param {boolean} flag - 是否折叠思考内容。
 * @returns {void}
 */
function enlargeThinkDetail(flag) {
  isCollapsed.value = flag
}

// 接收父组件传入的SSE数据
const props = defineProps({
  sseData: {
    type: String,
    default: '',
  },
  isComplete: {
    // 传输完成的信号（由父组件控制）
    type: Boolean,
    default: false,
  },
  msg: {
    type: Object,
    default: () => ({}),
  },
  showRefs: {
    type: Boolean,
    default: true,
  },
})

const quoteArticle = ref({})
const showPopover = ref(false)

/**
 * 处理正文引用标签悬停，并展示对应文献摘要。
 * @param {MouseEvent} event - 鼠标悬停事件。
 * @returns {Promise<void>}
 */
async function hoverMsg(event) {
  const dom = event.target

  if (dom?.nodeName !== 'SPAN' || !dom?.className.includes('quote-text')) return
  triggerDom.value = dom
  const referenceIndex = Number.parseInt(dom.attributes.num.value)
  if (!referenceIndex) return
  quoteArticle.value = props.msg.dbs?.[referenceIndex - 1] || {}
  await nextTick()
  showPopover.value = true
}

/**
 * 通知父级将回答区域滚动到底部。
 * @returns {void}
 */
const scrollBottom = () => {
  // 滚动到底部
  emits('scroll')
}

const dbInfo = ref()
const pdfReaderDialogVisible = ref(false)

/**
 * 打开指定引用文献的阅读器。
 * @param {object} db - 引用文献数据。
 * @returns {Promise<void>}
 */
async function checkFile(db) {
  dbInfo.value = db
  await nextTick()
  pdfReaderDialogVisible.value = true
}

/**
 * 获取不同接口引用数据中的统一显示标题。
 * @param {object} db - 引用文献数据。
 * @returns {string} 引用文献标题。
 */
function getReferenceTitle(db = {}) {
  return db.source || db.title || db.document_name || '无标题'
}

/**
 * 判断引用文献是否具备可打开的文件信息。
 * @param {object} db - 引用文献数据。
 * @returns {boolean} 是否允许打开阅读器。
 */
function canOpenReference(db = {}) {
  return Boolean(db.source_url || db.metadata || db.dataset_id || db.document_id)
}

/**
 * 下载引用文献对应的源文件。
 * @param {object} db - 引用文献下载元数据。
 * @returns {Promise<void>}
 */
async function downloadFile(db) {
  if (!db) return
  await downloadFileWithId(db)
}

const contentHtmlRenderRef = ref()
const chartRenderRef = ref()

/**
 * 获取当前回答正文渲染后的 HTML，供导出功能使用。
 * @returns {string} 回答正文 HTML。
 */
function getContentHtml() {
  return contentHtmlRenderRef.value?.getContentHtml?.() || ''
}

/**
 * 获取图表组件导出的图片数据，供文档导出功能使用。
 * @returns {string|undefined} 图表图片的 Base64 数据。
 */
function getChartDataURL() {
  return chartRenderRef.value?.getChartDataURL?.()
}

defineExpose({
  getContentHtml,
  getChartDataURL,
})
</script>
<style scoped lang="less">
.text-show {
}
:deep(.ai-msg__inner) {
  h1,
  h2,
  h3,
  h4,
  h5 {
    line-height: 2;
  }
  h1 {
    font-size: 20px;
  }
  h2 {
    font-size: 18px;
  }
  h3,
  h4,
  h5 {
    font-size: 14px;
  }

  img {
    // max-width: 60%;
  }
  ul,
  ol {
    padding-left: 40px !important;
  }

  hr {
    margin: 5px 0;
  }
  p {
    //margin: 5px 0;
  }
}

:deep(.quote-text) {
  display: inline-block;
  vertical-align: middle;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background-color: #ebecf1;
  padding: 3px 10px;
  border-radius: 12px;
  color: #848995;
  font-size: 10px;
  font-weight: bold;
  cursor: pointer;
}

:deep(.el-poppeer) {
  padding: 20px;
}
.popover-quote-title {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.popover-quote-content {
  white-space: pre-wrap;
  font-size: 12px;
  line-height: 1.5;
  max-height: 300px;
  overflow-y: auto;
}

.think-content {
  position: relative;
  font-size: 14px;
  padding: 18px 16px;
  border: 1px solid #e7e7ea;
  border-radius: 12px;
  margin-bottom: 12px;
  color: #878898;
  background: #fff;
  // height: auto;
  // overflow-y: auto;
  .scale-btn {
    position: absolute;
    right: 16px;
    top: 16px;
    &:hover {
      cursor: pointer;
      color: var(--el-color-primary);
    }
  }
  .think-content__inner {
    overflow: auto;
    max-height: 560px;
    opacity: 1;
    transition:
      max-height 0.3s ease,
      opacity 0.3s ease;
  }
  .is-thinking {
    max-height: 560px;
  }
  .is-thinking.is-collapsed {
    max-height: 298px;
  }
  .is-collapsed {
    // height: 0;
  }
  .hideen-content {
    max-height: 0;
    opacity: 0;
    overflow: hidden;
  }
  .status-done {
    color: #222529;
    font-weight: 600;
    font-size: 14px;
  }
}

.think-animate {
  display: flex;

  &::after {
    content: '...';
    animation: ellipsis-animation 1s linear infinite;
  }
}

@keyframes ellipsis-animation {
  0%,
  100% {
    content: '...';
  }
  33% {
    content: '.. ';
  }
  66% {
    content: '.  ';
  }
}

.quote-box {
  font-size: 12px;
}
.quote-title {
  margin-top: 10px;
  font-weight: bold;
}
.quote-content {
  padding-left: 16px;
}
.each-quote {
  display: flex;
  align-items: center;
  line-height: 2;
  &.is-link {
    cursor: pointer;
  }
  span {
    margin-right: 6px;
    font-weight: 500;
  }
  :deep(.el-link) {
    font-size: 12px;
  }
}

.pdf-content {
  height: 70vh;
}

.refer-question-box {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
}

.each-ques {
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  span {
    display: inline-block;
    width: auto;
    background-color: #e4f0ff;
    border-radius: 8px;
    line-height: 32px;
    padding: 0 12px;
    font-size: 12px;
    color: #3561fa;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: #d7e8ff;
    }
  }

  i {
    margin-left: 16px;
  }
}

.analysis-result {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  overflow-x: hidden;
}
</style>
