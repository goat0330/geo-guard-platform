<template>
  <div class="chat-box" :class="{ 'has-upload-files': hasUploadFiles }" @keydown.capture="disableTabKey">
    <div ref="chatBoxRef" class="model-send">
      <div v-for="(item, index) in messageList" :key="index">
        <component
          v-show="item.show"
          :is="item.components"
          v-bind="item.props"
          :key="index"
          @scroll="scrollBottom"
          @send="submit"
        />
      </div>
      <div class="bubble-loading" :class="{ 'bubble-loading-show': loading }">
        <span v-for="(dot, index) in 3" :key="index" class="dot" :style="{ animationDelay: `${index * 0.2}s` }">.</span>
      </div>
      <div ref="bottomRef"></div>
    </div>

    <div class="model-input">
      <div class="operate-box">
        <div id="chat-left-btn" class="left-btn"></div>
        <div class="operate-spacer"></div>
        <div v-if="showHistory" class="right-btn" @click="openHistory">
          <el-tooltip class="box-item" effect="dark" content="历史记录" placement="bottom">
            <img class="icon-show" src="@/assets/imgs/chatBox/history.png" />
          </el-tooltip>
        </div>
        <div v-if="showNew" class="right-btn" @click="newChat">
          <el-tooltip class="box-item" effect="dark" content="新对话" placement="bottom">
            <img class="icon-show" src="@/assets/imgs/chatBox/newChat.png" />
          </el-tooltip>
        </div>
      </div>
      <InputBox @submit="submit" @stop="stop" :is-answering="isAnswering" :is-chat="isChat" />
      <div class="input-tip">回答内容由AI生成，仅供参考，请仔细甄别</div>
    </div>
    <History ref="historyRef" />
    <WeatherHistory ref="weatherHistoryRef"></WeatherHistory>
  </div>
</template>

<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, provide, ref, shallowRef, watch } from 'vue'
import InputBox from './inputBox/index.vue'
import Bubble from './Bubble/index.vue'
import ConfirmBox from './ConfirmBox/index.vue'
import { componentsMapper, EVENT_TYPE } from '@/components/ChatBox/config.js'
import { chat, replyHandle } from '@/api/chat.js'
import {
  generateRandomString,
  parseMixedJson,
  replaceArticleTag,
} from '@/utils/index.js'
import { ElMessage } from 'element-plus'
import { eventBus, EventKey, EVENT_TTS_TEXT_MAP } from '@/utils/eventBus.js'
import { CHAT_TYPE, LAYER_ENUM } from '@/utils/enum.js'
import History from './History/index.vue'
import WeatherHistory from './WeatherHistory/index.vue'
import useChatBox from '@/components/ChatBox/useChatBox.js'
import { cloneDeep } from 'lodash-es'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/store/chat.js'
import { storeToRefs } from 'pinia'
import { useScroll } from '@/hooks/useScroll.js'
import { debounce } from 'lodash-es'
import {
  parseRecommendQuestionJson,
  parseAnswerPayload,
  parseBusinessAnswer,
  parseThinkStreamChunk,
} from '@/components/ChatBox/chatBoxUtil.js'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const router = useRouter()
const route = useRoute()
const chatBoxRef = ref()
const { scrollBottom } = useScroll(chatBoxRef)

// 存储一份初始数据
const initData = cloneDeep(props.modelValue)
let ttsSessionId = 0

// 内部修改时同步给外部
const updateMessageList = () => {
  emits('update:modelValue', [...messageList.value])
}

/**
 * 关闭指定的对话请求，仅在它仍是当前请求时重置输入状态。
 * @param {Function} abort - 需要关闭的请求中断方法。
 * @returns {void}
 */
const close = (abort = connect.value?.abort) => {
  abort?.()
  if (connect.value?.abort !== abort) return

  connect.value = null
  isAnswering.value = false
  loading.value = false
}

const stop = async () => {
  if (!connect.value) {
    return
  }
  close()
  messageList.value.push({
    components: shallowRef(Bubble),
    props: {
      type: 1,
      message: {
        content: '已停止对话',
        completed: true,
      },
    },
    show: true,
  })
}

const getFileExtension = (file) => {
  const name = file?.name || ''
  return name.includes('.') ? name.split('.').pop().toUpperCase() : ''
}

const getDefaultFileQuestion = (files = []) => {
  const extension = getFileExtension(files[0])
  return `请解读这份${extension || ''}文件`
}

const submit = debounce(async (val, { inputAfter, otherAppend = '', files = [] } = {}) => {
  const reqFiles = Array.isArray(files) ? files : []

  if (!val && !reqFiles.length) {
    return
  }
  if (isAnswering.value) {
    ElMessage.warning('请等待回复完成后发送指令~')
    return
  }
  const question = val || getDefaultFileQuestion(reqFiles)
  const flowId = generateRandomString(26)
  messageList.value.push({
    components: shallowRef(Bubble),
    props: {
      type: 2,
      message: {
        content: question,
        files: reqFiles,
        completed: true,
      },
    },
    show: true,
    flowId,
  })
  if (typeof inputAfter === 'function') {
    await inputAfter()
  }
  updateMessageList()
  scrollBottom()
  isAnswering.value = true
  await nextTick()
  getAnswer(question, flowId, otherAppend, reqFiles)
}, 500, {
  leading: true
})

const {
  operateBusiness,
  messageList,
  loading,
  isAnswering,
  pushMessage,
  startPushMessage,
  finishPushMessage,
  reset,
  businessId,
  conversationId,
} = useChatBox(updateMessageList, submit, scrollBottom, false, true)

const chatStore = useChatStore()
const { queryInputs, showHistory, showNew, uploadFiles } = storeToRefs(chatStore)
const hasUploadFiles = computed(() => uploadFiles.value?.length > 0)

const emits = defineEmits(['update:modelValue'])

const bottomRef = ref(null)

const historyRef = ref()

const weatherHistoryRef = ref()

const disableTabKey = (event) => {
  if (event.key === 'Tab') {
    event.preventDefault()
  }
}

// 监听外部变化
watch(
  () => props.modelValue,
  (value, oldValue) => {
    messageList.value = props.modelValue?.map((item) => {
      return {
        comp: item.comp,
        components: item?.components ? shallowRef(markRaw(item.components)) : componentsMapper[item.comp],
        props: item.props,
        show: item?.show !== false,
        type: item?.type,
        flowId: item?.flowId,
      }
    })
    if (value?.length > oldValue?.length) {
      scrollBottom()
    }
  },
  { deep: true, immediate: true },
)
// 获取操作(每个业务不一样，丢到外面处理)
// 各问答流程独立保存意图流数据，避免新旧请求相互覆盖。
const operationContextMap = new Map()
// 当前问答流程中已经播报过的业务意图，避免流式消息重复播报。
const spokenIntentKeys = new Set()

/**
 * 命中业务意图映射时启动语音播报，并立即执行原业务操作。
 * @param {object} operationData - 模型解析后的意图数据。
 * @param {string} flowId - 当前操作流程 ID。
 * @returns {Promise<void>}
 */
const executeOperation = async (operationData, flowId) => {
  await operateBusiness(operationData, flowId, {})
}

/**
 * 处理模型返回的操作意图，在操作完成事件中只执行一次最终意图。
 * @param {object} data - SSE 返回的操作消息。
 * @param {Function} abort - 当前请求的中断方法。
 * @param {string} flowId - 当前操作流程 ID。
 * @returns {Promise<void>}
 */
const getOperation = async (data, abort, flowId) => {
  // 当前流程的意图流上下文。
  const operationContext = operationContextMap.get(flowId) || {
    tempString: '',
    eventObj: {},
    messageId: '',
  }
  operationContextMap.set(flowId, operationContext)

  if (data.eventType === 'WORKFLOW_FINISHED') {
    const backObj = parseBusinessAnswer(data?.data?.outputs?.answer || '')
    // 固定格式 { id: xxx, type: '', data: {} }
    console.log('sse json ----- ', backObj, data)
    backObj.isEnd = true
    backObj.messageId = operationContext.messageId
    await executeOperation(backObj, flowId)
    // 当前流程已结束，释放播报记录。
    for (const intentKey of spokenIntentKeys) {
      if (intentKey.startsWith(`${flowId}:`)) spokenIntentKeys.delete(intentKey)
    }
    operationContextMap.delete(flowId)
    close(abort)
    scrollBottom()
  } else if (data.eventType === 'MESSAGE') {
    operationContext.messageId = data.message_id
    // 是流式回复
    const businessChunk = parseBusinessAnswer(data.answer || '')
    if (Object.keys(businessChunk).length && data.answer !== '{}') {
      operationContext.eventObj = businessChunk
    } else {
      operationContext.tempString += data.answer
    }
    if (![EVENT_TYPE.GENERATE_REVIEW_REPORT].includes(operationContext.eventObj?.type)) {
      return
    }
    operationContext.eventObj.isEnd = false
    operationContext.eventObj.mdText = operationContext.tempString
    await executeOperation(operationContext.eventObj, flowId)
  }
}

const connect = ref(null)
const isChat = ref(false)

// 页面切换会重置消息列表，需先终止仍引用旧消息索引的请求。
watch(() => route.path, () => close(), { flush: 'sync' })

const getAgentType = () => {
  const path = route.path
  if (path.includes('defense-response') || path.includes('emergency-response-detail')) {
    return CHAT_TYPE.DEFENSE
  } else if (path.includes('disaster-response') || path.includes('emergency-response-front')) {
    return CHAT_TYPE.DISPOSAL
  } else if (path.includes('prediction')){
    return CHAT_TYPE.PREDICT
  } else if (path.includes('task-track')) {
    return CHAT_TYPE.TASK_SEND
  } else {
    return CHAT_TYPE.ASSISTANT
  }
}
// 获取对话回复
const getAnswer = (val, flowId, otherAppend = '', files = []) => {
  isChat.value = true
  // 当前问答请求使用的语音会话 ID。
  const requestTtsSessionId = ttsSessionId
  // 当前问答请求对应的中断方法，避免旧请求关闭新请求。
  let requestAbort
  const reqFiles = Array.isArray(files) ? files : []
  let index = -1
  // 判断是回答还是系统操作
  let messageIndex = -1
  let isAnswer = true
  let once = false
  let getAnswerType = (string) => {
    if ((string?.startsWith('{') || string?.startsWith('(@apptype)')) && !once && messageIndex == 0) {
      once = true
      isAnswer = false
    }
    return isAnswer
  }

  let query = ''
  let appendUnit =
    (businessId.value ? '斜坡单元id为:' + businessId.value + ',' : '') +
    (otherAppend ? otherAppend + '--该流程走知识问答' : '')
  if (appendUnit) {
    query = val + `@append[${appendUnit}]`
  } else {
    query = val
  }
  const params = {
    params: {
      query: query,
      conversationId: conversationId.value ? conversationId.value + '' : '',
      agentType: getAgentType(),
      inputs: {
        ...(queryInputs.value || {}),
        ...(reqFiles.length ? { files: reqFiles } : {}),
      },
      ...(reqFiles.length ? { files: reqFiles } : {}),
    },
    onMessage: async (data) => {
      if (['WORKFLOW_FINISHED', 'MESSAGE'].includes(data?.eventType)) {
        messageIndex += 1
      }
      let message = ''
      if (data?.conversation_id) {
        conversationId.value = data?.conversation_id
      }
      // 判断是否是系统流程消息，打到全局处理里面定制化
      if (data?.data?.prompt) {
        onMessage(data)
        close(requestAbort)
        return
      }
      // 系统操作信息处理
      const answer = data?.answer || data?.outputs?.answer
      if (['WORKFLOW_FINISHED', 'MESSAGE'].includes(data?.eventType) && !getAnswerType(answer)) {
        await getOperation(data, requestAbort, flowId)
        queryInputs.value = {}
        return
      }
      // ai回答
      try {
        if (index === -1) {
          loading.value = false
          messageList.value.push({
            components: shallowRef(Bubble),
            props: {
              type: 1,
              message: {
                message: '',
                content: '',
                rawContent: '',
                ttsSessionId: requestTtsSessionId,
                refQ: [],
                dbs: '',
              },
              completed: false,
            },
            show: false,
          })
          // 记录消息索引
          index = messageList.value.length - 1
        }
        if (data?.eventType === 'refs') {
          const refQ = parseRecommendQuestionJson(data?.answer || '')
          messageList.value[index].props.message.refQ = refQ
        } else if (data?.eventType === 'MESSAGE') {
          message = data?.answer || ''
          messageList.value[index].show = true
          const currentMessage = messageList.value[index].props.message
          const answerContent = parseThinkStreamChunk(currentMessage, message)

          if (answerContent) {
            currentMessage.message += answerContent
            currentMessage.rawContent += answerContent
            // 使用累计文本识别协议标记，避免标记被拆到多个 SSE 分片中。
            const parsed = parseAnswerPayload(currentMessage.rawContent)
            currentMessage.refQ = parsed.refQ
            currentMessage.hasChartTask = parsed.hasChartTask
            currentMessage.chartDataStr = parsed.chartDataStr
            currentMessage.isOperateJson = parsed.isOperateJson
            currentMessage.pureJson = parsed.pureJson

            if (parsed.pureJson) {
              currentMessage.conversationId = conversationId.value
              currentMessage.messageId = data?.message_id
              return
            }
            if (!parsed.isOperateJson) {
              let dbs = currentMessage.dbs || []
              let originalContent = replaceArticleTag(parsed.content, dbs)
              originalContent = originalContent?.replace(/<\/?think>/g, '')
              // 保留原始 Markdown，由 HtmlRender 统一处理表格、公式和 Mermaid。
              currentMessage.content = originalContent
            }
          }
          // 清除确认框（所有录入了TaskId的都隐藏）
          const comps = messageList.value.filter((comp) => comp.taskId)
          if (comps?.length) {
            comps.forEach((comp) => (comp.show = false))
          }
        } else if (data?.eventType === 'NODE_FINISHED') {
          messageList.value[index].props.message.dbs = data.data?.outputs?.result
        } else if (data?.eventType === 'WORKFLOW_FINISHED') {
          const answer = data?.data?.outputs?.answer || ''
          if (answer.includes('(@json)')) {
            const splits = answer.split('(@json)')
            messageList.value[index].props.message.chartDataStr = splits[1]
          }
          messageList.value[index].props.completed = true
          messageList.value[index].props.message.completed = true
          updateMessageList()
          queryInputs.value = {}
          close(requestAbort)
        }
      } finally {
        if (connect.value?.abort === requestAbort) loading.value = false
        await nextTick()
        scrollBottom()
      }
    },
  }
  loading.value = true
  // 当前请求连接。
  const requestConnect = chat(params)
  requestAbort = requestConnect?.abort
  connect.value = requestConnect
}

// 全局消息接收
const onMessage = async (data) => {
  // 获取后端推送
  const obj = data?.data
  if (obj?.prompt === 'add') {
    messageList.value.push({
      taskId: obj?.taskId,
      components: shallowRef(ConfirmBox),
      props: {
        confirm: async () => {
          await replyHandle({
            taskId: obj?.taskId,
            reply: true,
          })
          loading.value = true
          scrollBottom()
        },
        cancel: async () => {
          await replyHandle({
            taskId: obj?.taskId,
            reply: false,
          })
          scrollBottom()
        },
      },
      show: true,
    })
    scrollBottom()
  }
}

const openHistory = () => {
  historyRef.value.openDialog()
}

// 新建对话
const newChat = () => {
  reset(async () => {
    conversationId.value = ''
    messageList.value = cloneDeep(initData)
    updateMessageList()
    if (!businessId.value) {
      eventBus.emit(EventKey.RESET_PAGE)
      await nextTick()
      eventBus.emit(EventKey.RESET_DIALOG)
    } else {
      await nextTick()
      eventBus.emit(EventKey.RESET_DIALOG, () => {
        eventBus.emit(EventKey.FOCUS_UNIT, {
          id: businessId.value,
          type: 'slope',
          unitType: LAYER_ENUM.SINGLE_SLOPE_UNIT,
        })
      })
    }
  })
}

// 挂载方法
const mountFun = () => {
  window.__chatBox__ = {
    submit,
    pushMessage,
    startPushMessage,
    finishPushMessage,
    scrollBottom,
    messageList
  }
}

defineExpose({
  submit,
  pushMessage,
  startPushMessage,
  finishPushMessage,
  scrollBottom,
})

const changeBusinessId = (val) => {
  businessId.value = val
}

const operateHistory = async (flag) => {
  if (typeof flag === 'object' && flag !== null) {
    if (flag.emitSelected) {
      await weatherHistoryRef.value?.refreshSelectedHistory()
    }
    if (flag.open === true) {
      weatherHistoryRef.value?.openDialog()
    } else if (flag.open === false) {
      weatherHistoryRef.value?.closeDialog()
    }
    return
  }
  if (flag) {
    weatherHistoryRef.value.openDialog()
  } else {
    weatherHistoryRef.value.closeDialog()
  }
}

onMounted(() => {
  eventBus.on(EventKey.CHAT_MSG, onMessage)
  eventBus.on(EventKey.OPERATE_WEATHER_WARNING_HISTORY, operateHistory)
  eventBus.on(EventKey.CHANGE_BUSINESS_ID, changeBusinessId)
  eventBus.on(EventKey.SUBMIT, submit)
  mountFun()
})

onBeforeUnmount(() => {
  close()
  eventBus.off(EventKey.CHAT_MSG, onMessage)
  eventBus.off(EventKey.CHANGE_BUSINESS_ID, changeBusinessId)
  eventBus.off(EventKey.SUBMIT, submit)
  eventBus.off(EventKey.OPERATE_WEATHER_WARNING_HISTORY, operateHistory)
  window.__chatBox__ = null
})
</script>

<style scoped lang="less">
@inputHeight: 138px;
@inputHeightWithFiles: 208px;
.chat-box {
  position: relative;
  padding: 30px 20px 20px;
  background: #fff;
  height: 100%;

  .bubble-loading {
    display: inline-flex;
    font-size: 20px;
    font-weight: bold;
    color: #666;
    visibility: hidden;

    .dot {
      display: inline-block;
      margin: 0 2px;
      animation: bounce 1s infinite;
    }
  }

  .bubble-loading-show {
    visibility: visible;
  }

  .model-send {
    height: calc(100% - @inputHeight - 30px);
    overflow-y: auto;
    scrollbar-gutter: stable;
    padding-right: 8px;
    overflow-x: hidden;
  }

  .model-input {
    position: absolute;
    left: 20px;
    bottom: 30px;
    width: calc(100% - 40px);
    height: @inputHeight;
    display: flex;
    flex-direction: column;

    .operate-box {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      width: 100%;

      .left-btn {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
        min-width: 0;
        overflow: hidden;
      }

      .operate-spacer {
        flex: none;
      }

      :deep(.left-btn .text-btn) {
        flex: 0 1 auto;
        min-width: 0;
      }

      .right-btn {
        flex: none;
        width: 32px;
        height: 32px;
        margin-right: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;

        .icon-show {
          width: 20px;
          height: 20px;
        }

        &:hover {
          border-radius: 8px;
          background: rgba(53, 97, 250, 0.1);
        }
      }
    }

    .input-tip {
      font-size: 12px;
      margin-top: 8px;
      color: #a1b1c0;
      margin-left: 5px;
      text-align: center;
    }
  }

  &.has-upload-files {
    .model-send {
      height: calc(100% - @inputHeightWithFiles - 30px);
    }

    .model-input {
      height: @inputHeightWithFiles;
    }
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
    opacity: 0.3;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}

:deep(.submit-tip) {
  display: block;

  i {
    position: relative;
    top: 2px;
  }
}
</style>
