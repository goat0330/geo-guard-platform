<template>
  <section class="review-panel">
    <header class="panel-header">
      <h1 class="panel-header__title">隐患复核总览</h1>
      <!-- 统计区划统一由地图上的政区选择器（AreaSearch）控制，面板内不再重复提供 -->
      <div class="panel-header__filters">
        <!-- 统计时段：默认全部，选中区间后按「开始 至 结束」展示，重置即回到全部并重新查询 -->
        <DateRangePicker v-model="selectedRange" @change="handleRangeChange" />
      </div>
    </header>

    <div class="panel-body">
      <div class="panel-block">
        <PanelSectionTitle title="总体概况" />
        <OverviewSection />
      </div>

      <div class="panel-block">
        <PanelSectionTitle title="复核状态">
          <template #extra>
            <button type="button" class="detail-link" @click="emit('view-process-detail')">
              查看详情
              <svg class="detail-arrow" width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M5.5 4L9 7L5.5 10" stroke="#007BFF" stroke-width="1.2" stroke-linecap="round" />
              </svg>
            </button>
          </template>
        </PanelSectionTitle>
        <StatusSection />
      </div>

      <div class="panel-block">
        <PanelSectionTitle title="风险等级" />
        <RiskLevelSection />
      </div>

      <div class="panel-block">
        <PanelSectionTitle title="隐患类型" />
        <HazardTypeSection />
      </div>

      <div class="panel-block">
        <PanelSectionTitle title="高风险乡镇">
          <span class="section-caption">（按A/B类隐患点数量）</span>
        </PanelSectionTitle>
        <HighRiskTownSection />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import DateRangePicker from '@/components/DateRangePicker/index.vue'
import PanelSectionTitle from '@/components/PanelSectionTitle/index.vue'
import { fetchHazardOverview } from '../useHazardReviewData.js'
import OverviewSection from './summary/OverviewSection.vue'
import StatusSection from './summary/StatusSection.vue'
import RiskLevelSection from './summary/RiskLevelSection.vue'
import HazardTypeSection from './summary/HazardTypeSection.vue'
import HighRiskTownSection from './summary/HighRiskTownSection.vue'

defineOptions({ name: 'ReviewSummaryPanel' })

const emit = defineEmits(['date-change', 'view-process-detail'])

/** 统计时段：默认空数组 = 全部；切换后向上抛出，便于后续按时段请求数据 */
const selectedRange = ref([])

const handleRangeChange = (range) => {
  emit('date-change', range)
  /* 时段变化即重新统计（接口异常已由 http 层统一提示） */
  void fetchHazardOverview(range).catch(() => {})
}

/* 面板挂载即按默认时段（全部）拉取总览统计 */
onMounted(() => {
  void fetchHazardOverview(selectedRange.value).catch(() => {})
})
</script>

<style lang="less" scoped>
/* 面板容器：460x1231 设计稿，圆角 20 + 2px 白描边，内容区宽度 = 460 - 4 - 22*2 = 412 */
.review-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: 22px 22px 0;
  box-sizing: border-box;
  overflow: hidden;
  border: 2px solid #ffffff;
  border-radius: 20px;
  background: #ffffff;
  color: #222527;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}

.panel-header__title {
  margin: 0;
  color: #222527;
  font-size: 18px;
  font-weight: 800;
  line-height: 18px;
  white-space: nowrap;
}

.panel-header__filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* 区块标题补充说明：跟在标题文案后，字号更小、颜色更浅 */
.section-caption {
  color: #a6acb8;
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
}

/* 区块标题右侧的文字按钮（与专业监测概览的「查看详情」保持一致） */
.detail-link {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #007bff;
  font-family: inherit;
  font-size: 12px;
  font-weight: 400;
  line-height: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.detail-link:hover {
  color: #3395ff;
}

.detail-arrow {
  margin-left: 4px;
}

/*
 * 滚动容器：负右边距把滚动条推到面板内边距带（内容区之外），
 * 内容用等量右内边距补齐，滚动条不再挤压/遮挡卡片内容；
 * 滚动条样式交给全局 reset.css 统一处理，保证与其它面板一致
 */
.panel-body {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  flex-direction: column;
  gap: 28px;
  margin-right: -22px;
  padding-right: 22px;
  padding-bottom: 22px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

/* 矮屏下压缩留白，保证首屏信息量 */
@media (max-height: 850px) {
  .panel-header {
    margin-bottom: 20px;
  }

  .panel-body {
    gap: 20px;
  }
}
</style>
