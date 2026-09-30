<template>
  <!-- 高度必须通过 prop 传入：BaseChart 会把 width/height 写成内联样式，优先级高于外部 CSS 类 -->
  <BaseChart class="displacement-chart" height="150px" :get-option="getOption" />
</template>

<script setup>
import * as echarts from 'echarts'
import BaseChart from '@/components/BaseChart/index.vue'

defineOptions({ name: 'DisplacementChart' })

const props = defineProps({
  /** 位移曲线：{ times: [], values: [] } */
  chart: {
    type: Object,
    default: () => ({ times: [], values: [] }),
  },
})

/** 用项目 px 函数做 rem 适配 */
const getOption = (px) => ({
  animation: false,
  grid: {
    top: px(24),
    right: px(10),
    bottom: px(4),
    left: px(28),
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'line', lineStyle: { color: '#a6acb8', width: px(1) } },
    backgroundColor: '#ffffff',
    borderWidth: 0,
    padding: [px(8), px(12)],
    textStyle: { color: '#617185', fontSize: px(12) },
    extraCssText: `border-radius:${px(8)}px;box-shadow:0 ${px(4)}px ${px(10)}px #1d64b124;`,
    formatter: (params) => {
      const item = params?.[0]
      if (!item) return ''
      return `${item.axisValue}&nbsp;&nbsp;<b style="color:#FF922C">${item.data}mm</b>`
    },
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: props.chart.times,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e4eaef' } },
    axisLabel: {
      color: '#9096a2',
      fontSize: px(10),
      interval: 3,
      margin: px(8),
    },
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 18,
    interval: 6,
    name: '(mm)',
    nameLocation: 'end',
    nameGap: px(10),
    nameTextStyle: { color: '#9096a2', fontSize: px(10), align: 'left' },
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#9096a2', fontSize: px(10) },
    splitLine: { lineStyle: { color: '#f0f3f7' } },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.chart.values,
      lineStyle: { color: '#ff922c', width: px(1.5) },
      itemStyle: { color: '#ff922c' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255, 146, 44, 0.18)' },
          { offset: 1, color: 'rgba(255, 146, 44, 0)' },
        ]),
      },
    },
  ],
})
</script>

<style lang="less" scoped>
/* 高度由 BaseChart 的 height prop 控制（内联样式），这里只约束宽度，避免两处写高度打架 */
.displacement-chart {
  width: 100%;
}
</style>
