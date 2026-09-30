<template>
  <div ref="chartRef" class="base-chart-container" :style="{ width, height }"></div>
</template>

<script setup>
import { watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts.js'

const props = defineProps({
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '100%',
  },
  // 动态 option 获取函数：(px) => Option
  getOption: {
    type: Function,
    default: null,
  },
  // 静态 option 对象
  option: {
    type: Object,
    default: null,
  },
})

const { chartRef, chartInstance, px, renderChart, setOption, resize, dispose } = useEcharts({
  getOption: props.getOption,
  autoInit: true,
})

watch(
  () => props.getOption,
  (newGetter) => {
    if (newGetter) {
      renderChart(newGetter)
    }
  },
  { deep: true },
)

watch(
  () => props.option,
  (newOpt) => {
    if (newOpt) {
      setOption(newOpt)
    }
  },
  { deep: true },
)

defineExpose({
  chartInstance,
  px,
  resize,
  renderChart,
  dispose,
})
</script>

<style lang="less" scoped>
.base-chart-container {
  min-width: 0;
  min-height: 0;
  position: relative;
}
</style>
