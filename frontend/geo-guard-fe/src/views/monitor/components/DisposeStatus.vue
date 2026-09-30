<template>
  <div class="dispose-status">
    <div ref="chartRef" class="status-chart" />

    <ul class="status-legend">
      <li v-for="item in monitorData.disposeStatus.legend" :key="item.key" class="legend-item">
        <i class="legend-dot" :style="{ background: item.color }" />
        <em class="legend-label">{{ item.label }}</em>
        <em class="legend-value">{{ item.value }}</em>
        <em class="legend-percent">{{ item.percent }}</em>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { monitorData } from '../useMonitorData.js'
import { useEcharts } from '@/hooks/useEcharts.js'

defineOptions({ name: 'DisposeStatus' })

/** 处置率仪表盘：100px 圆环，轨道 rgba(0,28,57,0.08) 宽 10px，进度圆头 */
const getOption = (px) => ({
  series: [
    {
      type: 'gauge',
      radius: '100%',
      center: ['50%', '50%'],
      startAngle: 90,
      endAngle: -270,
      min: 0,
      max: 100,
      pointer: { show: false },
      progress: {
        show: true,
        roundCap: true,
        clip: false,
        width: px(10),
        itemStyle: { color: monitorData.disposeStatus.gaugeColor },
      },
      axisLine: {
        roundCap: false,
        lineStyle: {
          width: px(10),
          color: [[1, 'rgba(0, 28, 57, 0.08)']],
        },
      },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        offsetCenter: [0, '10%'],
        formatter: (value) => `{v|${value}%}\n{n|处置率}`,
        rich: {
          v: {
            fontSize: px(18),
            fontWeight: 700,
            lineHeight: px(24),
            color: '#383C41',
          },
          n: {
            fontSize: px(12),
            lineHeight: px(17),
            color: '#A6ACB8',
          },
        },
      },
      data: [{ value: monitorData.disposeStatus.rate }],
    },
  ],
})

const { chartRef, renderChart } = useEcharts({ getOption })

// 接口数据到达后刷新仪表盘
watch(
  () => monitorData.disposeStatus.rate,
  () => renderChart(),
)
</script>

<style lang="less" scoped>
.dispose-status {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.status-chart {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  margin-left: 20px;
  margin-top: 16px;
}

// 图例：宽 228，两行行距 40，百分比右对齐
.status-legend {
  width: 228px;
  margin: 19px 0 0;
  padding: 0;
  list-style: none;
}

.legend-item {
  display: flex;
  align-items: center;
  height: 22px;

  & + .legend-item {
    margin-top: 18px;
  }
}

.legend-dot {
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: 20px;
  color: #565e73;
}

.legend-value {
  margin-left: 24px;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 800;
  line-height: 22px;
  color: #383c41;
}

.legend-percent {
  flex: 1 1 0;
  text-align: right;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 800;
  line-height: 22px;
  color: #383c41;
}
</style>
