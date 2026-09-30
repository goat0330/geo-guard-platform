<template>
  <!-- 未开始时不占位：避免空容器在不该有间距的步骤里留出空白 -->
  <div v-if="!isIdle" class="algo-result">
    <!-- 分析中：阻塞式算法可能耗时数十秒，明确提示避免用户重复提交 -->
    <div v-if="isLoading" class="algo-loading">
      <span class="algo-loading__dots"><i></i><i></i><i></i></span>
      <span class="algo-loading__text">算法分析中，可能需数十秒，请勿重复提交</span>
    </div>

    <!-- 失败：展示后端提示并提供重试；疑似重复筛查允许人工跳过后继续判定 -->
    <div v-else-if="isError" class="algo-error">
      <p class="algo-error__text">{{ state.error || '算法调用失败，请稍后重试' }}</p>
      <div class="algo-error__actions">
        <button type="button" class="algo-button" @click="emit('retry')">重试</button>
        <button v-if="skippable" type="button" class="algo-button is-plain" @click="emit('skip')">
          跳过并继续判定
        </button>
      </div>
    </div>

    <!-- 人工跳过：结果不完整，面板上必须留痕 -->
    <div v-else-if="isSkipped" class="algo-skipped">
      <p class="algo-skipped__text">已跳过疑似重复筛查，后续判定建议未参考重复筛查结果。</p>
      <button type="button" class="algo-button is-plain" @click="emit('retry')">补做重复筛查</button>
    </div>

    <!-- 成功：Markdown 正文 -->
    <div v-else-if="html" class="algo-answer" v-dompurify-html="html"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { createMd } from '@/utils/md.js'
import { AI_STEP_STATUS } from '../../useHazardReviewData.js'

defineOptions({ name: 'AgentAlgoResult' })

const props = defineProps({
  /** 步骤算法态：{ status, answer, messageId, error }，见 useHazardReviewData 的 buildAiState */
  state: {
    type: Object,
    required: true,
  },
  /** 是否允许「跳过并继续判定」：仅疑似重复筛查步骤开启 */
  skippable: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['retry', 'skip'])

/** 与项目其它 AI 文本一致：软换行按换行渲染 */
const md = createMd({ useBreak: true })

const isIdle = computed(() => props.state.status === AI_STEP_STATUS.IDLE)
const isLoading = computed(() => props.state.status === AI_STEP_STATUS.LOADING)
const isError = computed(() => props.state.status === AI_STEP_STATUS.ERROR)
const isSkipped = computed(() => props.state.status === AI_STEP_STATUS.SKIPPED)
/** 算法返回的是 Markdown 文本，按 Markdown 渲染后再净化注入 */
const html = computed(() => (props.state.answer ? md.render(props.state.answer) : ''))
</script>

<style lang="less" scoped>
.algo-result {
  padding: 14px 16px 0;
}

/* 分析中：小三点波浪 + 说明文案 */
.algo-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9096a2;
  font-size: 12px;
  line-height: 18px;
}

.algo-loading__dots {
  display: inline-flex;
  align-items: flex-end;
  gap: 4px;
  height: 12px;

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #9096a2;
    animation: algo-loading 1.2s infinite ease-in-out;

    &:nth-child(2) {
      animation-delay: 0.16s;
    }

    &:nth-child(3) {
      animation-delay: 0.32s;
    }
  }
}

@keyframes algo-loading {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.algo-error {
  padding: 10px 12px;
  border-left: 4px solid #dd4739;
  border-radius: 6px;
  background: #fef4f4;
}

.algo-error__text {
  margin: 0;
  color: #dd4739;
  font-size: 13px;
  line-height: 20px;
}

.algo-error__actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.algo-skipped {
  padding: 10px 12px;
  border-left: 4px solid #ffb84e;
  border-radius: 6px;
  background: #fff8ec;
}

.algo-skipped__text {
  margin: 0 0 10px;
  color: #b06f00;
  font-size: 13px;
  line-height: 20px;
}

.algo-button {
  height: 28px;
  padding: 0 14px;
  border: 0;
  border-radius: 6px;
  background: #007bff;
  color: #ffffff;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #3395ff;
  }

  &.is-plain {
    border: 1px solid #007bff;
    background: #eff6ff;
    color: #007bff;

    &:hover {
      background: #e2efff;
    }
  }
}

/* 算法 Markdown 正文：与面板其它说明文字同字号行高 */
.algo-answer {
  color: #617185;
  font-size: 14px;
  line-height: 22px;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 0 0 6px;
    color: #383c41;
    font-size: 14px;
    font-weight: 700;
    line-height: 22px;
  }

  :deep(h1:not(:first-child)),
  :deep(h2:not(:first-child)),
  :deep(h3:not(:first-child)),
  :deep(h4:not(:first-child)) {
    margin-top: 12px;
  }

  :deep(p) {
    margin: 0 0 6px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 6px;
    padding-left: 18px;
  }

  :deep(li + li) {
    margin-top: 4px;
  }

  :deep(strong) {
    color: #383c41;
  }

  :deep(code) {
    padding: 1px 4px;
    border-radius: 4px;
    background: #f2f5f9;
    font-size: 12px;
  }

  :deep(pre) {
    margin: 0 0 6px;
    padding: 10px 12px;
    overflow-x: auto;
    border-radius: 6px;
    background: #f2f5f9;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 0 0 6px;
    font-size: 13px;
  }

  :deep(th),
  :deep(td) {
    padding: 6px 8px;
    border: 1px solid #e9ecf3;
    text-align: left;
  }

  :deep(th) {
    background: #f5f6fa;
    color: #9096a2;
    font-weight: 400;
  }
}
</style>
