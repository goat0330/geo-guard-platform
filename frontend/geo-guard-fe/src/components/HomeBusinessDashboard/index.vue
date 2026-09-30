<template>
  <section class="business-dashboard">
    <!-- 顶部业务看板标签切换 -->
    <div class="dashboard-tabs">
      <button
        type="button"
        :class="['dashboard-tab', { 'is-active': activeTab === 'total' }]"
        @click="activeTab = 'total'"
      >
        📈 累计业务看板
      </button>
      <button
        type="button"
        :class="['dashboard-tab', { 'is-active': activeTab === 'today' }]"
        @click="activeTab = 'today'"
      >
        今日业务看板
      </button>
    </div>

    <Transition name="dashboard-fade" mode="out-in">
      <TodayBusinessPanel v-if="activeTab === 'today'" key="today" :data="today" />

      <!-- 顶部 3 个指标概览卡片 -->
      <div v-else key="total" class="overview-grid">
        <article v-for="item in overviewItems" :key="item.label" class="overview-card">
          <div class="overview-card-main">
            <img :src="item.icon" class="overview-icon" :alt="item.label" />
            <div class="overview-content">
              <span class="overview-label">{{ item.label }}</span>
              <strong class="overview-num">
                <AnimatedNumber :value="item.value" />
              </strong>
            </div>
          </div>
          <p class="overview-note">{{ item.note }}</p>
        </article>
      </div>
    </Transition>

    <!-- 6 大业务模块详情（行编排：前两行 2:1，第三行 1:1，今日与累计看板公共展示） -->
    <div class="detail-rows">
      <!-- 第一行：监测预警详情 (2/3) + 群众报灾详情 (1/3) -->
      <div class="detail-row row-2-1">
        <MonitorWarningDetail class="col-wide" :data="cumulative" />
        <GroupReportDetail
          class="col-narrow"
          :data="cumulative.publicReportDisasterTypes"
          :total="cumulative.publicReportCount"
        />
      </div>

      <!-- 第二行：风险等级概况 (2/3) + 已复核风险点详情 (1/3) -->
      <div class="detail-row row-2-1">
        <RiskLevelOverview class="col-wide" :data="riskTrend" />
        <HazardReviewDetail
          class="col-narrow"
          :data="cumulative.reviewedHazardTypes"
          :all-types="cumulative.hazardTypes"
        />
      </div>

      <!-- 第三行：动态预案详情 (1/2) + 复盘任务详情 (1/2) -->
      <div class="detail-row row-1-1">
        <DynamicPlanDetail class="col-half" :data="cumulative" />
        <ReviewTaskDetail class="col-half" :data="cumulative" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import monitorWarningIcon from '@/assets/imgs/home/icon-warning.png'
import hazardReviewIcon from '@/assets/imgs/home/overview-hazard-review.png'
import groupReportIcon from '@/assets/imgs/home/overview-group-report.png'

import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import TodayBusinessPanel from '@/components/TodayBusinessPanel/index.vue'
import MonitorWarningDetail from './components/MonitorWarningDetail.vue'
import GroupReportDetail from './components/GroupReportDetail.vue'
import RiskLevelOverview from './components/RiskLevelOverview.vue'
import HazardReviewDetail from './components/HazardReviewDetail.vue'
import DynamicPlanDetail from './components/DynamicPlanDetail.vue'
import ReviewTaskDetail from './components/ReviewTaskDetail.vue'
import { useHomeDashboard } from './useHomeDashboard.js'

defineOptions({
  name: 'HomeBusinessDashboard',
})

const activeTab = ref('total')
const { cumulative, loading, riskTrend, today } = useHomeDashboard()

const overviewItems = computed(() => [
  {
    label: '监测设备累计告警数',
    value: cumulative.value?.monitorAlarmCount ?? '--',
    icon: monitorWarningIcon,
    note: '· 该数据来源于危岩地灾风险管控系统实时数据',
  },
  {
    label: '累计复核风险点',
    value: cumulative.value?.reviewedHazardCount ?? '--',
    icon: hazardReviewIcon,
    note: '· 该数据来源于危岩地灾风险管控系统实时数据',
  },
  {
    label: '群众报灾累计处理',
    value: cumulative.value?.publicReportCompletedCount ?? '--',
    icon: groupReportIcon,
    note: '· 该数据来源于渝快办实时数据',
  },
])
</script>

<style lang="less" scoped>
.business-dashboard {
  width: 1000px;
  max-width: 100%;
  margin: 80px auto 0;
  color: #222527;
}

.dashboard-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 18px;
}

.dashboard-tab {
  font-family: 'AlibabaPuHuiTi', sans-serif;
  padding: 0;
  border: 0;
  background: transparent;
  color: #9096a2;
  font-size: 16px;
  font-style: normal;
  font-weight: 600;
  line-height: 24px;
  cursor: pointer;
  transition: color 0.2s ease;

  &.is-active {
    color: #222527;
  }
}

.dashboard-fade-enter-active,
.dashboard-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.dashboard-fade-enter-from,
.dashboard-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.overview-card {
  min-height: 112px;
  box-sizing: border-box;
  overflow: hidden;
  background: #f5f8fc;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.08);
}

.overview-card-main {
  min-height: 108px;
  padding: 14px 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(135deg, #f7fbff, #ffffff);
  border: 1px solid #e6f0fa;
  border-radius: 10px;
}

.overview-icon {
  width: 58px;
  height: 58px;
  object-fit: contain;
  flex-shrink: 0;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-label {
  color: #617185;
  font-size: 14px;
  line-height: 18px;
}

.overview-num {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 28px;
  line-height: 32px;
  font-style: normal;
  font-weight: Bold-Square;
}

.overview-note {
  height: 24px;
  padding: 5px 20px;
  box-sizing: border-box;
  margin: 0;
  color: #a6acb8;
  font-size: 10px;
  line-height: 14px;
}

.detail-rows {
  display: flex;
  flex-direction: column;
  gap: 28px;
  margin-top: 28px;
}

.detail-row {
  display: grid;
  gap: 24px;

  &.row-2-1 {
    grid-template-columns: 2fr 1fr;
  }

  &.row-1-1 {
    grid-template-columns: 1fr 1fr;
  }
}

.col-wide,
.col-narrow,
.col-half {
  min-width: 0;
}

@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .detail-row.row-2-1,
  .detail-row.row-1-1 {
    grid-template-columns: 1fr;
  }

  .business-dashboard {
    margin-top: 88px;
  }
}
</style>
