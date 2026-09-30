<template>
  <div class="donut-wrap">
    <div ref="chartRef" class="donut-chart"></div>
    <div class="donut-center"><strong>{{ centerValue }}</strong><span>{{ centerLabel }}</span></div>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts.js'

defineOptions({ name: 'GroupDefenseDonutChart' })
const props = defineProps({
  items: { type: Array, default: () => [] },
  centerValue: { type: [String, Number], default: '--' },
  centerLabel: { type: String, default: '' },
})
const getOption = (px) => ({
  animation: false,
  tooltip: { show: false },
  series: [{
    type: 'pie',
    radius: ['72%', '88%'],
    center: ['50%', '50%'],
    silent: true,
    label: { show: false },
    itemStyle: { borderColor: '#FFFFFF', borderWidth: px(2), borderRadius: px(2) },
    data: props.items.map((item) => ({ value: item.value, name: item.name, itemStyle: { color: item.color } })),
  }],
})
const { chartRef, renderChart } = useEcharts({ getOption })
watch(() => props.items, () => renderChart(), { deep: true })
</script>

<style lang="less" scoped>
.donut-wrap {
  position: relative;
  width: 100px;
  height: 100px;
}
.donut-chart {
  width: 100%;
  height: 100%;
}
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  pointer-events: none;
}
.donut-center strong {
  color: #383c41;
  font-size: 18px;
  font-weight: 600;
}
.donut-center span {
  margin-top: 2px;
  color: #9096a2;
  font-size: 12px;
}
</style>
