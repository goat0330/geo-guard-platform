<template>
  <Transition name="agent-panel">
  <aside v-if="visible" class="risk-analysis-agent">
    <header class="agent-header">
      <div><img :src="analysisIcon" alt="" />风险评价智能体</div>
      <button title="关闭" @click="emit('close')"><i class="iconfont icon-close"></i></button>
    </header>
    <div ref="agentBodyRef" v-loading="loading" class="agent-body">
      <div ref="agentContentRef">
        <p class="intro">好的，我现在将基于「{{ unitName }}」的全量数据，按流程为您推理动态风险等级的计算过程，尽量让每一步的过程都清晰呈现：</p>
        <article v-for="item in visibleAnalysisItems" :key="item.title" class="analysis-item message-enter">
          <button class="analysis-title" :disabled="item.loading" @click="toggleItem(item)">
            <span>
              <i class="iconfont icon-circle-right"></i>
              {{ item.title }}
            </span>
            <i v-if="!item.loading" class="iconfont icon-arrow-down" :class="{ 'is-folded': !item.open }"></i>
          </button>
          <div v-if="item.loading && !item.contentHtml" class="analysis-loading">
            <i></i><i></i><i></i>
          </div>
          <div v-else-if="item.open" class="analysis-detail">
            <div v-if="item.contentHtml" class="agent-content" v-dompurify-html="item.contentHtml"></div>
            <p v-for="text in item.content" :key="text">{{ text }}</p>
            <div v-if="item.loading" class="analysis-loading inline-loading">
              <i></i><i></i><i></i>
            </div>
          </div>
        </article>
        <el-empty v-if="!loading && loadFailed" :image-size="72" description="暂无该斜坡单元的风险分析数据" />
        <p v-if="sequenceComplete" class="result message-enter">经过调取数据并逐步计算，<strong>{{ unitName }}动态风险等级结果为<b :style="{ color: dynamicRiskColor }">{{ dynamicRiskLabel }}</b>；</strong></p>
      </div>
    </div>
  </aside>
  </Transition>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

import analysisIcon from '@/assets/imgs/fengxian/analysis.png'
import { useScroll } from '@/hooks/useScroll.js'
import { useRiskAnalysisAgent } from './useRiskAnalysisAgent.js'

defineOptions({ name: 'RiskAnalysisAgent' })

const props = defineProps({ visible: Boolean, slope: { type: Object, default: () => ({}) } })
const emit = defineEmits(['close'])
const agentBodyRef = ref(null)
const agentContentRef = ref(null)
const { isScrollBlocked, scrollBottom } = useScroll(agentBodyRef, { threshold: 30 })
let contentResizeObserver = null
let scrollFrame = null

// 新内容仅在用户仍停留在底部时自动跟随，用户上滑阅读时不打断阅读。
const scheduleScrollBottom = () => {
  if (isScrollBlocked.value || scrollFrame) return

  scrollFrame = requestAnimationFrame(async () => {
    scrollFrame = null
    if (!isScrollBlocked.value) {
      await scrollBottom()
    }
  })
}

watch(
  agentContentRef,
  (content) => {
    contentResizeObserver?.disconnect()
    if (!content) return

    contentResizeObserver = new ResizeObserver(scheduleScrollBottom)
    contentResizeObserver.observe(content)
    void nextTick(scheduleScrollBottom)
  },
  { flush: 'post' },
)

watch(
  () => props.visible,
  (visible) => {
    if (visible) void nextTick(scheduleScrollBottom)
  },
)

onBeforeUnmount(() => {
  contentResizeObserver?.disconnect()
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
})

const {
  dynamicRiskColor,
  dynamicRiskLabel,
  loadFailed,
  loading,
  sequenceComplete,
  toggleItem,
  unitCode,
  unitName,
  visibleAnalysisItems,
} = useRiskAnalysisAgent(props)
</script>

<style lang="less" scoped>
.risk-analysis-agent { position: absolute; top: 0; right: 0; bottom: 0; z-index: 20; width: 480px; border-radius: 16px; background: #fff; color: #617185; overflow: hidden; }
.agent-header { width: 100%; height: 57px; padding: 0; box-sizing: border-box; padding: 0 20px; display: flex; align-items: center; justify-content: space-between; background: url('@/assets/imgs/fengxian/fxpj-bg.png') center / 120% 100% no-repeat; color: #222527; font-family: 'AlibabaPuHuiTi', sans-serif; font-size: 18px; font-style: normal; font-weight: 700; line-height: 18px; }
.agent-header div { display: flex; align-items: center; gap: 8px; }.agent-header img { width: 20px; height: 20px; object-fit: contain; }.agent-header button { border: 0; background: transparent; cursor: pointer; color: #222527; }.agent-header i { font-size: 16px; }
.agent-body { height: calc(100% - 67px); padding: 20px; box-sizing: border-box; overflow-y: auto; font-size: 14px; line-height: 24px; }.intro { margin: 0 0 16px; color: #383c41; }.intro strong, .result b { color: #007bff; }
.analysis-item { margin-bottom: 12px; }.analysis-title { width: 100%; min-height: 46px; padding: 0 12px; display: flex; align-items: center; justify-content: space-between; border: 1px solid #dce3eb; border-radius: 10px; background: #f4f8ff; color: #007bff; font-size: 14px; cursor: pointer; text-align: left; }.analysis-title span { display: flex; align-items: center; gap: 8px; }.analysis-title span i { font-size: 16px; }.analysis-title > i { transition: transform .2s ease; }.analysis-title > i.is-folded { transform: rotate(180deg); }.analysis-detail { padding: 10px 12px 0; color: #9096a2; }.analysis-detail p { position: relative; margin: 0 0 8px; padding-left: 12px; }.analysis-detail p::before { content: ''; position: absolute; top: 10px; left: 0; width: 4px; height: 4px; border-radius: 50%; background: #a6acb8; }.result { margin: 20px 0 0; color: #617185; }.result b { color: #ff922c; }
.analysis-detail p {
  white-space: pre-line;
}
.analysis-title:disabled {
  cursor: default;
}

.analysis-loading {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 4px;
  width: 100%;
  height: 36px;
  padding: 0 0 8px 12px;
  box-sizing: border-box;

  &.inline-loading {
    height: 24px;
    padding: 4px 0 4px 0;
  }
}

.analysis-loading i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9096A2;
  animation: analysis-loading 1.2s infinite ease-in-out;
}

.analysis-loading i:nth-child(2) {
  animation-delay: 0.16s;
}

.analysis-loading i:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes analysis-loading {
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

.agent-content {
  color: #617185;
  font-size: 14px;
  line-height: 24px;
  word-break: break-word;

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 8px;
    padding-left: 20px;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 8px 0;
    font-size: 12px;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #dce3eb;
    padding: 6px 8px;
    text-align: left;
  }

  :deep(th) {
    background: #f4f8ff;
    color: #383c41;
  }
}

.message-enter {
  animation: message-enter 0.24s ease both;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.agent-panel-enter-active, .agent-panel-leave-active { transition: transform .24s ease, opacity .24s ease; }.agent-panel-enter-from, .agent-panel-leave-to { opacity: 0; transform: translateX(24px); }
</style>
