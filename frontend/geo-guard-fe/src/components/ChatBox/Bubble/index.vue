<template>
  <div
    class="bubble-box"
    :style="[
      type == 2 && { 'justify-content': 'flex-end' },
      { 'margin-bottom': `${gap}px` }
    ]"
  >
    <template v-if="type === 1">
      <img class="avatar" :src="riskEvaluationAgentAvatar" alt="风险评价智能体" />
      <div class="answer-wrap">
        <div class="bubble bubble-answer">
          <template v-if="useDefault">{{ defaultStreamContent }}</template>
          <ChatAnswerContent
            v-else
            :sseData="message?.content"
            :is-complete="message?.completed"
            :msg="message"
            @scroll="scrollBottom"
            @send="(question) => emits('send', question)"
          />
        </div>
      </div>
    </template>
    <template v-else>
      <div class="bubble bubble-ask">
        <div>{{ removeAtBlock(message?.content) }}</div>
        <div v-if="message?.files?.length" class="ask-file-list">
          <div v-for="file in message.files" :key="file.uploadFileId" class="ask-file-item">
            <img src="@/assets/imgs/chatEngine/file-uploaded.png" class="ask-file-icon" />
            <div class="ask-file-info">
              <div class="ask-file-name" :title="file.name">{{ file.name }}</div>
              <div class="ask-file-size">{{ formatFileSize(file.size) }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import ChatAnswerContent from '@/components/ChatAnswerContent/index.vue'
import { onBeforeUnmount, ref, watch } from 'vue'
import { removeAtBlock } from '@/utils/index.js'
import riskEvaluationAgentAvatar from '@/assets/imgs/chatBox/risk-evaluation-agent-avatar.png'

const emits = defineEmits(['scroll', 'send'])

const props = defineProps({
  // 1：回复 2：问题
  type: {
    type: Number,
    default: 2
  },
  message: {
    type: Object,
    default: () => {}
  },
  gap: {
    type: Number,
    default: 20
  },
  // 是否使用默认文本
  useDefault: {
    type: Boolean,
    default: false
  }
})

const scrollBottom = () => {
  emits('scroll')
}

const formatFileSize = (size) => {
  if (!size) return ''

  if (size < 1024) {
    return `${size}B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }

  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

const defaultStreamContent = ref('')
let defaultStreamTimer = null

const clearDefaultStreamTimer = () => {
  if (defaultStreamTimer) {
    clearInterval(defaultStreamTimer)
    defaultStreamTimer = null
  }
}

const startDefaultStream = (content = '') => {
  clearDefaultStreamTimer()
  defaultStreamContent.value = ''

  if (!content) {
    return
  }

  let index = 0
  defaultStreamTimer = setInterval(() => {
    index += 1
    defaultStreamContent.value = content.slice(0, index)
    emits('scroll')
    if (index >= content.length) {
      clearDefaultStreamTimer()
    }
  }, 40)
}

watch(
  () => [props.message, props.type],
  () => {
    if (props.type === 2 || props.message?.completed) {
      emits('scroll', true)
    }
  },
  { deep:true, immediate: true }
)

watch(
  () => [props.useDefault, props.message?.content],
  ([useDefault, content]) => {
    if (useDefault) {
      startDefaultStream(content || '')
    } else {
      clearDefaultStreamTimer()
      defaultStreamContent.value = content || ''
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  clearDefaultStreamTimer()
})

</script>

<style scoped lang="less">
.bubble-box {
  display: flex;
  //align-items: center;

  .avatar {
    width: 40px;
    height: 40px;
  }

  .answer-wrap {
    max-width: calc(100% - 50px);
    min-width: 0;
    margin-left: 10px;
  }

  .bubble {
    padding: 12px 20px;
    font-size: 14px;
    line-height: 20px;
  }

  .bubble-answer {
    background: #D2E5FF;
    border-radius: 0 12px 12px 12px;
    //flex: 1;
    min-width: 0;
    box-sizing: border-box;

    :deep(.ai-msg__inner) {
      max-width: 100%;
      overflow-x: auto;
    }
  }

  .bubble-ask {
    background: #f5f5f5;
    border-radius: 12px 0 12px 12px;
    align-self: flex-end;
  }

  .ai-tip {
    color: #EF8B34;
    margin-top: 8px;
  }

  .ask-file-list {
    margin-top: 10px;
  }

  .ask-file-item {
    width: 220px;
    border-radius: 10px;
    background: #fff;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    box-sizing: border-box;
  }

  .ask-file-icon {
    width: 28px;
    height: 28px;
    flex-shrink: 0;
  }

  .ask-file-info {
    flex: 1;
    min-width: 0;
  }

  .ask-file-name {
    color: #222529;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ask-file-size {
    margin-top: 4px;
    color: #8a93a6;
    font-size: 12px;
  }
}

:deep(.modify-box) {
  width: 100% !important;
  min-width: unset !important;
}

:deep(.weather-report) {
  overflow-x: auto;
}
</style>
