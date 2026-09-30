<template>
  <div class="review-report-inner">
    <!-- 左侧主体区：事件详情生成结果 (loading 态) / 复盘报告 Markdown 正文 (完成态) -->
    <div class="report-left-container">
      <Transition name="page-fade" mode="out-in">
        <ReportLeftSection
          v-if="!isDocumentReady"
          key="loading"
          :event="event"
          @back="emit('back')"
        />
        <ReportDocumentSection
          v-else
          key="document"
          :event="event"
          @back="emit('back')"
        />
      </Transition>
    </div>

    <!-- 右侧面板区：复盘报告智能体 (与左侧无重叠并排) -->
    <div
      class="report-right-container"
      :class="{ 'is-collapsed': agentCollapsed }"
    >
      <ReportAgentPanel
        :event="event"
        :detail="detailData"
        :step1-loading="step1Loading"
        :step1-open="step1Open"
        :step1-visible="step1Visible"
        :step2-loading="step2Loading"
        :step2-open="step2Open"
        :step2-visible="step2Visible"
        :step3-open="step3Open"
        :step3-visible="step3Visible"
        :summary-visible="summaryVisible"
        @close="agentCollapsed = true"
        @toggle-step="toggleStep"
      />
    </div>

    <!-- 左右分栏展开/收起悬浮控制按钮 -->
    <button
      type="button"
      class="agent-collapse-trigger"
      :class="{ 'is-collapsed': agentCollapsed }"
      :title="agentCollapsed ? '展开智能体' : '收起智能体'"
      @click="agentCollapsed = !agentCollapsed"
    >
      <img :src="agentCollapsed ? leftArrow : rightArrow" alt="" />
    </button>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import ReportLeftSection from './components/ReportLeftSection.vue'
import ReportDocumentSection from './components/ReportDocumentSection.vue'
import ReportAgentPanel from './components/ReportAgentPanel.vue'
import { useReportFlow } from './useReportFlow.js'
import { getReviewEventDetail } from '@/api/review.js'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'

defineOptions({ name: 'ReviewReportInner' })

const props = defineProps({
  event: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['back'])

// 右侧智能体是否收起（默认展开）
const agentCollapsed = ref(false)
const detailData = ref(null)

const {
  step1Loading,
  step1Open,
  step1Visible,
  step2Loading,
  step2Open,
  step2Visible,
  step3Open,
  step3Visible,
  summaryVisible,
  isDocumentReady,
  startFlow,
  toggleStep,
  destroyFlow,
} = useReportFlow()

const loadDetail = async (id) => {
  if (!id) {
    detailData.value = null
    return
  }
  try {
    const res = await getReviewEventDetail(id)
    detailData.value = res?.data || res || null
  } catch (err) {
    console.warn('获取复盘事件详情失败，降级使用列表数据', err)
    detailData.value = null
  }
}

watch(
  () => props.event?.id,
  (newId) => {
    void loadDetail(newId)
    void startFlow()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  destroyFlow()
})
</script>

<style lang="less" scoped src="./style.less"></style>
