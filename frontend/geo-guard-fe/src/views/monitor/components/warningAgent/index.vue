<template>
  <section class="agent-panel">
    <header class="panel-header">
      <img class="header-icon" :src="headerIcon" alt="" />
      <h1>监测预警智能体</h1>
      <button class="close-button" type="button" title="关闭" @click="emit('close')">
        <img :src="closeIcon" alt="" />
      </button>
    </header>

    <div ref="bodyRef" class="panel-body">
      <div ref="contentRef">
        <!-- 监测点信息卡 -->
        <div class="point-card">
          <span class="point-icon"><img :src="pointIcon" alt="" /></span>
          <div class="point-info">
            <div class="point-title">
              <h2>{{ detail.name }}</h2>
              <span class="level-tag" :style="levelStyle">{{ detail.level }}</span>
            </div>
            <p class="point-meta">
              <i class="iconfont icon-a-Localyidingwei"></i>检测设备：{{ detail.deviceName }}
            </p>
            <p class="point-meta">
              <i class="iconfont icon-a-Frame1"></i>分析时段：{{ detail.period }}
            </p>
          </div>
        </div>

        <p class="agent-summary">{{ detail.summary }}</p>

        <!-- 步骤：按流式节奏逐块出现，当前块展示三点加载（与风险评价智能体面板一致） -->
        <WarningStepSection v-for="step in visibleSteps" :key="step.key" class="message-enter" :step="step"
          :chart="detail.chart" :loading="step.key === loadingKey" :expanded="expandedKeys.includes(step.key)"
          @toggle="toggleStep" />

        <!-- 底部结论：全部步骤产出后再出现 -->
        <div v-if="sequenceComplete" class="agent-footer message-enter">
          <p class="footer-record">
            <span class="record-label">{{ footerRecord.label }}</span>
            <span class="record-value">{{ footerRecord.value }}</span>
          </p>
          <p class="footer-note">{{ detail.footer.note }}</p>
        </div>
      </div>
    </div>

    <footer class="panel-footer">
      <button class="ghost-button" type="button" @click="emit('feedback')">
        <i class="iconfont icon-a-Commentpinglun"></i>提交反馈
      </button>
      <button class="primary-button" type="button" @click="emit('assign')">
        <i class="iconfont icon-add-person"></i>分派给责任人
      </button>
    </footer>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import WarningStepSection from './WarningStepSection.vue'
import { useScroll } from '@/hooks/useScroll.js'
import { alarmLevelMap, warningAgentDetail } from '../../config.js'
import headerIcon from '@/assets/imgs/hazardReview/icon-header-yhfhznt.webp'
import closeIcon from '@/assets/imgs/hazardReview/icon-close.webp'
import pointIcon from '@/assets/imgs/hazardReview/icon-zhlx-bt.webp'

defineOptions({ name: 'WarningAgentPanel' })

const props = defineProps({
  /** 面板数据：由预警列表行拼装（见 config.js 的 buildWarningAgentDetail），缺省用内置 mock */
  detail: {
    type: Object,
    default: () => warningAgentDetail,
  },
})

const emit = defineEmits(['close', 'feedback', 'assign'])

/** 每个步骤块停留加载态的时长（与风险评价智能体面板一致） */
const STEP_LOADING_DURATION = 650

const detail = computed(() => props.detail || warningAgentDetail)

/** 步骤默认展开，可逐项收起 */
const expandedKeys = ref(detail.value.steps.map((step) => step.key))

const toggleStep = (key) => {
  expandedKeys.value = expandedKeys.value.includes(key)
    ? expandedKeys.value.filter((item) => item !== key)
    : [...expandedKeys.value, key]
}

/** 已揭示的步骤数：逐块推进，模拟智能体逐步产出。
 *  接口接入后由流式事件驱动同一套状态，面板与动画无需改动 */
const revealedCount = ref(0)
const visibleSteps = computed(() => detail.value.steps.slice(0, revealedCount.value))
/** 当前正在加载的步骤 key（该块展示三点加载，不可折叠） */
const loadingKey = ref('')
/** 全部步骤产出完毕：底部结论区随后出现 */
const sequenceComplete = ref(false)

let timers = []
let isUnmounted = false

const clearTimers = () => {
  timers.forEach((timer) => window.clearTimeout(timer))
  timers = []
}

const waitLoading = () =>
  new Promise((resolve) => {
    const timer = window.setTimeout(() => {
      timers = timers.filter((item) => item !== timer)
      resolve()
    }, STEP_LOADING_DURATION)
    timers.push(timer)
  })

/** 逐块揭示：每块先出现标题 + 三点加载，加载结束再展开正文 */
const playSequence = async () => {
  clearTimers()
  revealedCount.value = 0
  sequenceComplete.value = false
  for (const [index, step] of detail.value.steps.entries()) {
    if (isUnmounted) return
    revealedCount.value = index + 1
    loadingKey.value = step.key
    await waitLoading()
    if (isUnmounted) return
  }
  loadingKey.value = ''
  sequenceComplete.value = true
}

/* 切换到另一条预警（父级重新拼装 detail）时：重置展开态并按新数据重放一次产出节奏 */
watch(
  () => props.detail,
  () => {
    expandedKeys.value = detail.value.steps.map((step) => step.key)
    void playSequence()
  },
)

onMounted(() => {
  void playSequence()
})

/* 内容变高时自动跟随到底部；用户上滑阅读时不打断 */
const bodyRef = ref(null)
const contentRef = ref(null)
const { isScrollBlocked, scrollBottom } = useScroll(bodyRef, { threshold: 30 })
let contentResizeObserver = null
let scrollFrame = null

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
  contentRef,
  (content) => {
    contentResizeObserver?.disconnect()
    if (!content) return

    contentResizeObserver = new ResizeObserver(scheduleScrollBottom)
    contentResizeObserver.observe(content)
    void nextTick(scheduleScrollBottom)
  },
  { flush: 'post' },
)

onBeforeUnmount(() => {
  isUnmounted = true
  clearTimers()
  contentResizeObserver?.disconnect()
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
})

/** 未在字典中的等级使用中性配色兜底 */
const fallbackLevel = { color: '#617185', background: '#f1f4f8' }

/** 「已有处置记录」兼容两种写法：整句字符串 或 { label, value } */
const footerRecord = computed(() => {
  const record = detail.value.footer.record
  if (typeof record === 'string') return { label: record, value: '' }
  return { label: record?.label ?? '', value: record?.value ?? '' }
})

const levelStyle = computed(() => {
  const config = alarmLevelMap[detail.value.level] || fallbackLevel
  return { color: config.color, background: config.background }
})
</script>

<style lang="less" scoped>
.agent-panel {
  display: flex;
  height: 100%;
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
  /* 头部背景用设计稿切图 */
  background: url('@/assets/imgs/monitor/bg-agent-header.webp') no-repeat center / 100% 100%;
}

.header-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.panel-header h1 {
  flex: 1;
  margin: 0;
  color: #222527;
  font-size: 18px;
  font-weight: bold;
}

.close-button {
  display: flex;
  width: 16px;
  height: 16px;
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
    width: 16px;
    height: 16px;
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
}

/* 监测点信息卡 */
.point-card {
  display: flex;
  gap: 12px;
  padding: 16px;

  width: 412px;
  /* 不低于设计高度，内容变化时不裁切 */
  min-height: 98px;
  border-radius: 12px;
  background: linear-gradient(180deg, #F1F8FF 0%, #FFF 100%);
  box-shadow: 0 2px 6px 0 #1d64b133;

}

.point-icon {
  display: flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #ffffff;

  img {
    width: 40px;
    height: 40px;
    object-fit: contain;
  }
}

.point-info {
  min-width: 0;
}

.point-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.point-title h2 {
  margin: 0;
  color: #007BFF;
  font-size: 16px;
  line-height: 18px;
  font-weight: bold;
  white-space: nowrap;
}

.level-tag {
  flex-shrink: 0;
  height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  color: #9096A2;
}

/* 检测设备 / 分析时段：前置小图标 + 灰蓝文字 */
.point-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 10px 0 0;
  color: #9096A2;
  font-size: 12px;
  line-height: 14px;

  i {
    flex-shrink: 0;
    color: #a6acb8;
    font-size: 12px;
  }
}

.agent-summary {
  margin: 24px 0 12px;
  color: #383C41;
  font-size: 14px;
  line-height: 22px;
}

.agent-footer {
  margin: 24px 0 0;
  color: #9096a2;
  font-size: 14px;
  line-height: 24px;
}

.agent-footer p {
  margin: 0;
}

.agent-footer p + p {
  margin-top: 10px;
}

/* 已有处置记录：label 深色，value 用主色 */
.footer-record .record-label {
  color: #383C41;
}

.footer-record .record-value {
  color: #007bff;
}

.footer-note {
  color: #A6ACB8;
  font-size: 12px;
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

  /* hover 用叠加层过渡，避免渐变切换跳帧 */
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
