<template>
  <article class="transcript-item" :class="{ 'is-partial': isPartial }">
    <span class="speaker-avatar" :class="{ 'is-speaking': isPartial }">{{ initial }}</span>
    <div class="transcript-item__body">
      <div class="transcript-item__meta">
        <strong>{{ name }}</strong>
        <time>{{ timestamp }}</time>
        <span v-if="isPartial" class="status-tag status-partial">正在说话</span>
        <span v-else-if="isAborted" class="status-tag status-aborted">已中断</span>
      </div>
      <p>
        <span>{{ content }}</span>
        <i v-if="isPartial" class="streaming-cursor" aria-hidden="true"></i>
      </p>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

defineOptions({ name: 'SmartMinutesTranscriptItem' })

const props = defineProps({
  item: { type: Object, default: () => ({}) },
})

const name = computed(() => String(props.item?.speakerName || '未识别发言人'))
const initial = computed(() => name.value.trim().slice(0, 1) || '会')
const timestamp = computed(() => {
  const value = props.item?.startedAt || props.item?.timestamp
  return value && dayjs(value).isValid() ? dayjs(value).format('HH:mm:ss') : ''
})
const content = computed(() => String(props.item?.text || props.item?.content || ''))
const isPartial = computed(() => props.item?.type === 'partial')
const isAborted = computed(() => props.item?.type === 'aborted')
</script>

<style lang="less" scoped>
.transcript-item {
  display: flex;
  gap: 10px;
  transition: background-color 0.2s ease;
}

.speaker-avatar {
  display: flex;
  width: 28px;
  height: 28px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #007bff;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  transition: box-shadow 0.3s ease;

  &.is-speaking {
    box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25);
  }
}

.transcript-item__body {
  min-width: 0;
  flex: 1;
}

.transcript-item__meta {
  display: flex;
  min-height: 14px;
  align-items: center;
  gap: 6px;

  strong,
  time {
    font-size: 10px;
    line-height: 14px;
  }

  strong {
    color: #9096a2;
    font-weight: 600;
  }

  time {
    color: #a6acb8;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-style: normal;
  }
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 14px;
  padding: 0 4px;
  border-radius: 2px;
  font-size: 10px;
  line-height: 14px;

  &.status-partial {
    background: #dcedff;
    color: #007bff;
  }

  &.status-aborted {
    background: #f5f6f8;
    color: #9096a2;
  }
}

p {
  margin: 6px 0 0;
  color: #383c41;
  font-size: 12px;
  font-style: normal;
  line-height: 18px;
  word-break: break-all;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 12px;
  margin-left: 2px;
  vertical-align: -1px;
  background-color: #007bff;
  animation: cursor-blink 0.8s infinite ease-in-out;
}

@keyframes cursor-blink {
  0%, 100% {
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
}
</style>
