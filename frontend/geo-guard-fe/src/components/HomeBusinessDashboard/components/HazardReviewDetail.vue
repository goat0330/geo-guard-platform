<template>
  <div class="hazard-review-detail">
    <div class="section-header">
      <span class="section-bullet"></span>
      <h3 class="section-title">已复核风险点详情</h3>
    </div>

    <div class="hazard-card">
      <template v-if="typeData.length">
        <!-- 左侧：ECharts 环形图 -->
        <div class="chart-box">
          <div ref="chartRef" class="donut-chart"></div>
        </div>

        <!-- 右侧：图例与统计 -->
        <div class="type-list">
          <div v-for="item in typeData" :key="item.label" class="type-item">
            <div class="type-info">
              <i class="type-dot" :style="{ background: item.color }"></i>
              <span class="type-label">{{ item.label }}</span>
            </div>
            <strong class="type-count"><AnimatedNumber :value="item.num" /></strong>
            <strong class="type-percent"><AnimatedNumber :value="item.percent" suffix="%" /></strong>
          </div>
        </div>
      </template>
      <el-empty v-else class="empty-box" :image-size="50" description="暂无复核风险点分类数据" />
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useEcharts } from '@/hooks/useEcharts.js'
import { createHomeDonutSeries } from '@/utils/homeDonut.js'
import { formatHazardType } from '@/components/HomeBusinessDashboard/useHomeDashboard.js'

defineOptions({
  name: 'HazardReviewDetail',
})

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
})

const COLORS = ['#7EA8FF', '#44B699', '#FF922C', '#F8D35E', '#D7DDE7', '#9096A2']

const typeData = computed(() => {
  const list = Array.isArray(props.data) ? props.data : []
  const total = list.reduce((sum, item) => sum + (Number(item?.count) || 0), 0)
  return list.map((item, idx) => {
    const num = Number(item?.count) || 0
    const percent = total > 0 ? Number(((num / total) * 100).toFixed(1)) : 0
    return {
      label: formatHazardType(item?.value),
      value: num.toLocaleString('zh-CN'),
      num,
      percent,
      color: COLORS[idx % COLORS.length],
    }
  })
})

const { chartRef, px, setOption } = useEcharts({
  initOnVisible: true,
  getOption: (px) => ({
    animationDuration: 1200,
    animationEasing: 'cubicOut',
    title: {
      text: '风险点\n详情分类',
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
    series: createHomeDonutSeries(typeData.value, px),
  }),
})

watch(typeData, () => {
  if (typeData.value.length && setOption) {
    setOption({
      series: createHomeDonutSeries(typeData.value, px),
    })
  }
})
</script>

<style lang="less" scoped>
.hazard-review-detail {
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

.hazard-card {
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

.type-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-item {
  display: grid;
  grid-template-columns: 46px 28px 48px;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  line-height: 20px;
}

.type-info {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.type-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.type-label {
  color: #617185;
  font-size: 14px;
}

.type-count,
.type-percent {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: Bold-Square;
  white-space: nowrap;
}

.type-percent {
  text-align: right;
}

@media (max-width: 768px) {
  .hazard-card {
    flex-direction: column;
  }
}
</style>
