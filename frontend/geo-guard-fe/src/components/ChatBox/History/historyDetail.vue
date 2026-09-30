<template>
  <div class="history-detail">
<!--    <div class="history-detail-title">正在查看历史对话记录（ID:{{ detailData?.id }}）</div>-->
    <div class="model-send">
      <div v-for="(item, index) in messageList" :key="index">
        <component v-show="item.show" :is="item.components" v-bind="item.props" :key="index" @scroll="scrollBottom" />
      </div>
<!--      <div v-show="loading" class="bubble-loading">-->
<!--        <span v-for="(dot, index) in 3" :key="index" class="dot" :style="{ animationDelay: `${index * 0.2}s` }">.</span>-->
<!--      </div>-->
      <div ref="bottomRef"></div>
    </div>
  </div>
</template>

<script setup>
import useChatBox from '@/components/ChatBox/useChatBox.js'
import { splitTip } from '@/components/ChatBox/config.js'
import { isJsonString, safeFixJsonString } from '@/utils/index.js'
import { ref, shallowRef, watch } from 'vue'
import Bubble from '@/components/ChatBox/Bubble/index.vue'
import { useScroll } from '@/hooks/useScroll.js'
import { parseRecommendQuestions, parseThinkStreamChunk } from '@/components/ChatBox/chatBoxUtil.js'
import { hasAuth } from '@/utils/directive.js'
import { getReportParseResult } from '@/api/chat.js'

const props = defineProps({
  detailData: {
    type: Object,
    default: () => ({})
  }
})


const bottomRef = ref()
const scrollBottom = () => {
  setTimeout(() => {
    bottomRef.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'end'
    })
  }, 50)
}

const updateMessageList = () => {}
const submit = () => {}

const {
  operateBusiness,
  messageList,
  loading,
} = useChatBox(updateMessageList, submit, scrollBottom, true)

const initDetail = async() => {
  messageList.value = []
  const data = props.detailData?.data || []
  const hasSqlAuth = hasAuth('geo:ai:sql')
  let registeredMsgIds = []
  if (hasSqlAuth) {
    try {
      const parseRes = await getReportParseResult({ sessionId: props.detailData.id })
      registeredMsgIds = parseRes || []
    } catch (error) {
      console.error('获取入库状态失败：', error)
    }
  }
  for (const item of data) {
    messageList.value.push({
      components: shallowRef(Bubble),
      props: {
        type: 2,
        message: {
          content: item?.query?.replace(/斜坡单元id为:\d+/g, ''),
        },
      },
      show: true
    })
    if (item?.answer?.startsWith('{') ||item?.answer?.startsWith('(@apptype)') ) {
      let backObj = {}
      const str = item?.answer
      let jsonString = str
      // 判断是否有后续内容  后续内容为(@data)[text]
      const splits = str?.split(splitTip)
      // 去除空格和换行符
      jsonString = safeFixJsonString(splits[0]?.replace(/\s+/g, ''))
      // 主要json内容
      if (isJsonString(jsonString)) {
        backObj = JSON.parse(jsonString)
      }
      if (backObj instanceof Array) {
        backObj = backObj[0]
      }
      // 附加内容判断
      if (splits.length > 1) {
        backObj.followText = splits[1]?.slice(1, -1)
      }
      backObj.history = true
      backObj.historyData = item
      await operateBusiness(backObj)
    } else {
      let originalThink, originalContent, originJsonStr, pureJson
      // 判断是否包含思考过程，think标签
      if (item.answer?.startsWith('<think>')) {
        let [think, content] = item.answer.split('</think>')
        originalThink = think.replace('<think>', '')
        originalContent = content
      } else {
        originalThink = ''
        originalContent = item.answer
      }

      const recommendParsed = parseRecommendQuestions(originalContent)
      originalContent = recommendParsed.content

      const JSON_SPLITER = '(@json)'
      if (originalContent.includes(JSON_SPLITER)) {
        let [content, json] = originalContent.split('(@json)')
        originalContent = content
        originJsonStr = json
      }

      const REPORT_SPLITER = '(@report)'
      if (originalContent.includes(REPORT_SPLITER)) {
        pureJson = {
          type: 'report',
          data: originalContent.replace('(@report)', ''),
        }
      } else {
        pureJson = null
      }

      let dbs = []
      try {
        let refs = JSON.parse(item.messageMetadata)
        dbs = (refs?.retriever_resources || []).map((v) => ({
          title: v.document_name,
          content: v.content,
          metadata: { ...v },
        }))
      } catch (error) {
        console.log('知识库转换出错：', error)
      }

      // 处理提问带了图片/文件的情况
      let reqFiles = []
      if (item.inputs.files && item.inputs.files.length > 0) {
        reqFiles = item.inputs.files
      }

      originalContent = originalContent.replace(
        /https?:\/\/example\.com/g,
        '/images-service'
      )

      let _content = pureJson ? '' : originalContent
      messageList.value.push({
        components: shallowRef(Bubble),
        props: {
          type: 1,
          message: {
            message: _content,
            // 保留原始 Markdown，由 ChatAnswerContent 统一渲染公式和 Mermaid 流程图。
            content: _content,
            dbs: dbs,
            pureJson,
            pureJsonHasRegister: pureJson ? (hasSqlAuth ? registeredMsgIds.includes(item.id) : true) : true,
            completed: true,
            conversationId: props.detailData.id,
            messageId: item.id,
            reportHistory: true
          },
          completed: true
        },
        show: true
      })
    }
  }
}

watch(
  () => props.detailData,
  () => {
    initDetail()
  },
  { immediate: true, deep: true }
)

</script>

<style scoped lang="less">
.history-detail {
  width: 100%;
  height: 100%;
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  align-items: center;

  .history-detail-title {
    width: max-content;
    padding: 10px 20px;
    border-radius: 8px;
    background: rgba(53, 97, 250, .1);
    font-size: 12px;
    color: #3561FA;
  }

  .model-send {
    margin-top: 20px;
    width: 100%;
    padding-right: 8px;
  }
}
</style>
