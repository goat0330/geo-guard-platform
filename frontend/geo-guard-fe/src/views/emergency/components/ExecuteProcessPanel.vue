<template>
  <section class="execute-panel">
    <header class="panel-header">
      <img class="header-icon" :src="headerIcon" alt="" />
      <h1>{{ data.title }}</h1>
      <button class="close-button" type="button" title="收起" @click="emit('close')">
        <img :src="closeIcon" alt="收起" />
      </button>
    </header>

    <div ref="bodyRef" class="panel-body">
      <!-- 流式：顶部说明与步骤依次出现，与风险评价智能体面板保持一致的节奏 -->
      <p v-if="visibleCount >= 1" class="panel-intro message-enter">{{ data.intro }}</p>

      <ExecuteStepSection
        v-for="step in visibleSteps"
        :key="step.key"
        class="message-enter"
        :step="step"
        :expanded="expandedKeys.includes(step.key)"
        @toggle="toggleStep(step.key)"
      />

      <!-- 思考中：三点跳动，位于已展示内容下方，全部展示完毕后消失 -->
      <div v-if="loading" class="thinking"><i></i><i></i><i></i></div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ExecuteStepSection from './ExecuteStepSection.vue'
import headerIcon from '@/assets/imgs/hazardReview/icon-header-yhfhznt.webp'
import closeIcon from '@/assets/imgs/emergency/icon-close.png'

defineOptions({ name: 'ExecuteProcessPanel' })

const props = defineProps({
  /** 执行过程数据：{ title, intro, steps: [{ key, title, summary, open? }] } */
  data: {
    type: Object,
    required: true,
  },
  /** 是否流式加载：步骤逐个出现（接口接入后由真实执行进度驱动），关闭则一次性全量展示 */
  stream: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['close', 'progress', 'complete'])

/** 流式加载每步间隔，与风险评价智能体面板保持一致 */
const STEP_INTERVAL = 720

const bodyRef = ref(null)
/** 已出现的消息数：0 空、1 说明、2 起为第 1~N 步 */
const visibleCount = ref(0)
const loading = ref(false)
const expandedKeys = ref([])
let sequenceTimer = null

const steps = computed(() => props.data?.steps || [])
/** 消息总数 = 顶部说明 + 步骤数 */
const totalMessages = computed(() => steps.value.length + 1)
const visibleSteps = computed(() => steps.value.slice(0, Math.max(0, visibleCount.value - 1)))

/** 自动滚动到底部：内容超出面板高度时始终展示最新一步 */
const scrollToBottom = async () => {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

const clearSequence = () => {
  if (sequenceTimer) {
    window.clearInterval(sequenceTimer)
    sequenceTimer = null
  }
}

/** 上报进度，供左侧结果区的加载提示展示「已完成 x/y 个步骤」 */
const reportProgress = () => {
  emit('progress', { revealed: visibleSteps.value.length, total: steps.value.length })
}

/** 新出现的步骤默认展开（step.open === false 时保持收起），用户手动折叠后不再自动展开 */
watch(visibleCount, () => {
  visibleSteps.value.forEach((step) => {
    if (step.open !== false && !expandedKeys.value.includes(step.key)) {
      expandedKeys.value = [...expandedKeys.value, step.key]
    }
  })
  reportProgress()
})

/** 启动流式加载：每 STEP_INTERVAL 出现一条，全部出现后关闭思考动画并上报完成 */
const startSequence = () => {
  clearSequence()
  if (!props.stream) {
    visibleCount.value = totalMessages.value
    loading.value = false
    reportProgress()
    emit('complete')
    return
  }
  visibleCount.value = 1
  expandedKeys.value = []
  loading.value = true
  reportProgress()
  sequenceTimer = window.setInterval(() => {
    visibleCount.value += 1
    void scrollToBottom()
    if (visibleCount.value >= totalMessages.value) {
      clearSequence()
      loading.value = false
      emit('complete')
    }
  }, STEP_INTERVAL)
}

const toggleStep = (key) => {
  expandedKeys.value = expandedKeys.value.includes(key)
    ? expandedKeys.value.filter((item) => item !== key)
    : [...expandedKeys.value, key]
}

onMounted(startSequence)

onBeforeUnmount(clearSequence)
</script>

<style lang="less" scoped>
.execute-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #ffffff;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 57px;
  padding: 0 20px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eef1ff 0%, #f6f8ff 46%, #ffffff 100%);

  .header-icon {
    flex-shrink: 0;
    display: block;
    width: 22px;
    height: 22px;
    object-fit: contain;
  }

  h1 {
    flex: 1 1 auto;
    min-width: 0;
    margin: 0;
    overflow: hidden;
    color: #222527;
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .close-button {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;

    img {
      display: block;
      width: 14px;
      height: 14px;
      object-fit: contain;
    }
  }
}

.panel-body {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 4px 20px 24px;
  box-sizing: border-box;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 2px;
    background: rgba(97, 113, 133, 0.3);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

/* 顶部说明：统一 14px / 行高 24 */
.panel-intro {
  margin: 0 0 16px;
  color: #383c41;
  font-size: 14px;
  font-weight: 500;
  line-height: 24px;
}

/* 流式进场：新消息淡入并轻微上移 */
.message-enter {
  animation: message-in 0.28s ease both;
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 思考中：三点依次跳动 */
.thinking {
  display: flex;
  gap: 5px;
  padding: 12px 2px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #007bff;
    animation: thinking 1s infinite ease-in-out;
  }

  i:nth-child(2) {
    animation-delay: 0.16s;
  }

  i:nth-child(3) {
    animation-delay: 0.32s;
  }
}

@keyframes thinking {
  0%,
  60%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-4px);
  }
}
</style>
