<template>
  <section class="agent-panel">
    <header class="panel-header">
      <img class="header-icon" :src="headerIcon" alt="" />
      <h1>隐患复核智能体</h1>
      <button class="close-button" type="button" title="关闭" @click="emit('close')">
        <img :src="closeIcon" alt="" />
      </button>
    </header>

    <div class="panel-body">
      <!-- 隐患点摘要 -->
      <div class="hazard-card">
        <div class="card-title">
          <span class="pin-wrap"><i class="iconfont icon-a-Localyidingwei"></i></span>
          <h2>{{ hazardName }}</h2>
          <span v-if="hazardStatus" class="status-tag">{{ hazardStatus }}</span>
        </div>
        <p class="card-meta"><i class="iconfont icon-a-Localyidingwei"></i>{{ brief.relation }}</p>
        <p class="card-meta"><i class="iconfont icon-file"></i>{{ brief.materials }}</p>
      </div>

      <!-- 摘要：算法结果（aiSummary）未产出时留空，不写占位结论 -->
      <p v-if="brief.summary" class="agent-summary">{{ brief.summary }}</p>

      <p v-if="!panel" class="panel-tip">正在调取该隐患点的复核数据…</p>

      <!-- 步骤：展示节奏由智能体返回驱动，当前块先出「分析中」占位，返回后再出下一块；
           步骤区自带与上方卡片的间距，摘要未产出（aiSummary 为 null）时也不会贴在一起 -->
      <div v-else class="step-list">
        <AgentStepSection
          v-for="step in visibleSteps"
          :key="step.key"
          class="message-enter"
          :step="step"
          :loading="isStepLoading(step)"
          :expanded="expandedKeys.includes(step.key)"
          @toggle="toggleStep"
          @retry="emit('retry-step', $event)"
          @skip="emit('skip-step', $event)"
        />

        <!-- 底部结论：全部步骤结束后再出现 -->
        <p v-if="allStepsSettled" class="agent-footer message-enter">
          {{ brief.footer.text }}<em>{{ brief.footer.highlight }}</em>
        </p>
      </div>
    </div>

    <footer class="panel-footer">
      <button class="ghost-button" type="button" @click="emit('feedback')">
        <i class="iconfont icon-a-Commentpinglun"></i>提交反馈
      </button>
      <button class="primary-button" type="button" @click="emit('confirm')">
        <i class="iconfont icon-check"></i>确认复核结果
      </button>
    </footer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AgentStepSection from './AgentStepSection.vue'
import { AI_STEP_STATUS } from '../../useHazardReviewData.js'
import headerIcon from '@/assets/imgs/hazardReview/icon-header-yhfhznt.webp'
import closeIcon from '@/assets/imgs/hazardReview/icon-close.webp'

defineOptions({ name: 'AgentProcessPanel' })

const props = defineProps({
  /** 当前处理的隐患点：隐患处理列表中的条目（详情未返回前先展示其名称与状态） */
  data: {
    type: Object,
    default: null,
  },
  /** 详情接口的装配结果（见 useHazardReviewData 的 buildHazardAgentPanel），未返回时为 null */
  panel: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'feedback', 'confirm', 'retry-step', 'skip-step'])

/** 卡片与摘要的空值兜底：详情未返回时不展示伪数据 */
const EMPTY_BRIEF = { relation: '', materials: '', summary: '', footer: { text: '', highlight: '' } }

const brief = computed(() => props.panel?.brief || EMPTY_BRIEF)
const steps = computed(() => props.panel?.steps || [])
const hazardName = computed(() => props.panel?.name || props.data?.name || '')
const hazardStatus = computed(() => props.panel?.status || props.data?.status || '')

/** 处理步骤默认全部展开，可逐项收起 */
const expandedKeys = ref([])

const toggleStep = (key) => {
  expandedKeys.value = expandedKeys.value.includes(key)
    ? expandedKeys.value.filter((item) => item !== key)
    : [...expandedKeys.value, key]
}

/** 本地数据块（无 ai）天然结束；算法块要拿到结果、失败或人工跳过才算结束 */
const isStepSettled = (step) =>
  !step.ai
  || step.ai.status === AI_STEP_STATUS.SUCCESS
  || step.ai.status === AI_STEP_STATUS.ERROR
  || step.ai.status === AI_STEP_STATUS.SKIPPED

/** 该块算法分析中：不可折叠，进度由块内的算法结果区展示 */
const isStepLoading = (step) => step.ai?.status === AI_STEP_STATUS.LOADING

/**
 * 展示节奏由智能体返回驱动：顺序展示到第一个未结束的块（含该块），
 * 该块先以「分析中」占位，返回结果后再接着显示下一块，不再用定时器模拟流式
 */
const visibleSteps = computed(() => {
  const list = steps.value
  const pendingIndex = list.findIndex((step) => !isStepSettled(step))
  return pendingIndex === -1 ? list : list.slice(0, pendingIndex + 1)
})

/** 全部步骤结束：底部结论随后出现 */
const allStepsSettled = computed(() => steps.value.length > 0 && steps.value.every(isStepSettled))

/**
 * 详情就绪或切换到另一个隐患点（父级重新传入 panel）时重置展开态；
 * 步骤出现时机交给算法返回，这里不再重放产出节奏
 */
const resetExpanded = () => {
  expandedKeys.value = steps.value.map((step) => step.key)
}

watch(() => props.panel, resetExpanded)

onMounted(resetExpanded)
</script>

<style lang="less" scoped>
.agent-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
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
}

.header-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.panel-header h1 {
  flex: 1;
  margin: 0;
  color: #222527;
  font-size: 16px;
  font-weight: 600;
}

.close-button {
  display: flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #eef2f7;
  }

  img {
    width: 14px;
    height: 14px;
    object-fit: contain;
  }
}

.panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 20px 20px;

  /* 子项保持自然高度：否则内容超高时会被 flex 压扁，压扁的部分落在内部 overflow:hidden 里被裁掉 */
  > * {
    flex-shrink: 0;
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 3px;
    background: #dfe4ec;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #c6cedb;
  }
}

.hazard-card {
  padding: 14px;
  border: 1px solid #eef1f6;
  border-radius: 12px;
  background: linear-gradient(180deg, #F1F8FF 0%, #FFF 100%);
  box-shadow: 0 2px 6px 0 #1d64b133;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pin-wrap {
  display: flex;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff0e8;
  color: #ff922c;
  font-size: 14px;
}

.card-title h2 {
  margin: 0;
  color: #222527;
  font-size: 16px;
  font-weight: 600;
}

.status-tag {
  flex-shrink: 0;
  padding: 0 12px;
  width: 72px;
  height: 24px;
  border-radius: 100px;
  background: #007bff29;
  color: #007BFF;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &::before {
    content: "";
    display: block;
    width: 4px;
    height: 4px;
    border-radius: 100%;
    background-color: #007BFF;
  }
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0 0;
  color: #9096a2;
  font-size: 12px;
  line-height: 18px;

  i {
    font-size: 14px;
  }
}

.agent-summary {
  margin: 16px 0 16px;
  color: #222527;
  font-size: 14px;
  line-height: 22px;

}

/* 详情未返回时的提示行 */
.panel-tip {
  margin: 16px 0 0;
  color: #9096a2;
  font-size: 14px;
  line-height: 22px;
}

/* 步骤区：与上方卡片/摘要拉开间距（摘要存在时与其下边距折叠，不会翻倍） */
.step-list {
  margin-top: 16px;
}

.agent-footer {
  margin: 18px 0 0;
  color: #222527;
  font-size: 14px;
  line-height: 22px;
}

/* 「待人工确认」用主色强调 */
.agent-footer em {
  color: #007bff;
  font-style: normal;
}

/* 新产出的块：轻微上移淡入，与风险评价智能体面板一致 */
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

.panel-footer {
  flex-shrink: 0;
  display: flex;
  gap: 12px;
  padding: 12px 20px 20px;
  border-top: 1px solid #f2f5f9;
}

.ghost-button,
.primary-button {
  position: relative;
  z-index: 0;
  display: inline-flex;
  flex: 1;
  height: 44px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 10px;
  font-family: inherit;
  font-size: 16px;
  cursor: pointer;

  i {
    font-size: 18px;
  }
}

.ghost-button {
  border: 1px solid #007bff;
  background: #eff6ff;
  color: #007bff;
  transition: background 0.2s ease;

  &:hover {
    background: #e2efff;
  }
}

.primary-button {
  background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
  color: #ffffff;

  /* hover 用叠加层过渡，避免渐变背景切换时的跳帧闪烁 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: -1;
    border-radius: inherit;
    background: linear-gradient(90deg, #3395ff 12.5%, #40c4ff 100%);
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover::before {
    opacity: 1;
  }
}
</style>
