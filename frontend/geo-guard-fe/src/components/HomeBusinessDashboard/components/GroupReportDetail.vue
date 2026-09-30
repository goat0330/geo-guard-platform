<template>
  <div ref="sectionRef" class="group-report-detail">
    <div class="section-header">
      <span class="section-bullet"></span>
      <h3 class="section-title">{{ title }}</h3>
    </div>

    <div class="report-card">
      <template v-if="categoryData.length">
        <!-- 左侧：ECharts 环形图 -->
        <div class="chart-box">
          <div ref="chartRef" class="donut-chart"></div>
        </div>

        <!-- 右侧：图例与数值统计 -->
        <div class="category-list">
          <div v-for="item in categoryData" :key="item.label" class="category-item">
            <div class="category-info">
              <i class="category-dot" :style="{ background: item.color }"></i>
              <span class="category-label">{{ item.label }}</span>
            </div>
            <strong class="category-value"><AnimatedNumber :value="item.num" /></strong>
          </div>
        </div>
      </template>
      <el-empty v-else class="empty-box" :image-size="50" description="暂无灾种分类数据" />
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useChartProgressAnimation } from '@/composables/useChartProgressAnimation.js'
import { useEnterView } from '@/composables/useEnterView.js'
import { useEcharts } from '@/hooks/useEcharts.js'
import { createHomeDonutSeries } from '@/utils/homeDonut.js'
import { formatHazardType } from '@/components/HomeBusinessDashboard/useHomeDashboard.js'

defineOptions({
  name: 'GroupReportDetail',
})

const props = defineProps({
  title: {
    type: String,
    default: '群众报灾详情',
  },
  data: {
    type: Array,
    default: null,
  },
})

const COLORS = ['#3595FB', '#44B699', '#FF922C', '#E45B5B', '#9096A2']

const categoryData = computed(() => {
  if (!Array.isArray(props.data) || props.data.length === 0) return []
  return props.data.map((item, idx) => ({
    label: formatHazardType(item.value),
    value: item.count?.toLocaleString?.('zh-CN') ?? String(item.count ?? ''),
    num: Number(item.count) || 0,
    color: COLORS[idx % COLORS.length],
  }))
})

const { targetRef: sectionRef, hasEntered: sectionHasEntered } = useEnterView()

const { chartRef, px, setOption } = useEcharts({
  autoInit: false,
})

function createChartOption(progress) {
  if (!categoryData.value.length) return {}
  return {
    animation: false,
    title: {
      text: '群众报灾\n分类占比',
      left: 'center',
      top: 'center',
      textStyle: {
        color: '#617185',
        fontSize: px(12),
        fontWeight: 700,
        lineHeight: px(18),
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    series: createHomeDonutSeries(categoryData.value, px, progress),
  }
}

useChartProgressAnimation({
  active: sectionHasEntered,
  duration: 1800,
  render: (progress) => {
    if (categoryData.value.length) {
      setOption(createChartOption(progress), true, true)
    }
  },
})
</script>

<style lang="less" scoped>
.group-report-detail {
  width: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.section-bullet {
  width: 8px;
  height: 8px;
  background: url('@/assets/imgs/point.png') center / contain no-repeat;
  flex-shrink: 0;
}

.section-title {
  margin: 0;
  color: #383c41;
  font-size: 16px;
  font-weight: 600;
  line-height: 20px;
}

.report-card {
  box-sizing: border-box;
  background: #ffffff;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 154px;

  .empty-box {
    width: 100%;
    padding: 8px 0;
  }
}

.chart-box {
  width: 116px;
  height: 116px;
  flex-shrink: 0;
}

.donut-chart {
  width: 100%;
  height: 100%;
}

.category-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  line-height: 20px;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.category-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.category-label {
  color: #617185;
  font-size: 14px;
}

.category-value {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: Bold-Square;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .report-card {
    flex-direction: column;
  }
}
</style>
