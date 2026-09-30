<template>
  <div class="town-risk-chart-container">
    <div ref="chartRef" class="chart-box"></div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { createEchartsScale } from '@/utils/rem.js'
import { debounce } from '@/utils'

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
})

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const renderChart = () => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const { px } = createEchartsScale()

  // 默认取排序前 5 位的乡镇数据
  const list = (props.data || []).slice(0, 5)
  const xData = list.map((item) => item.name || '')
  const veryHighData = list.map((item) => Number(item.veryHighNumber || 0))
  const highData = list.map((item) => Number(item.highNumber || 0))
  const totalData = list.map((item) => Number(item.totalNumber || 0))

  const option = {
    grid: {
      top: px(34),
      right: px(10),
      bottom: px(24),
      left: px(10),
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(0, 123, 255, 0.04)',
        },
      },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E4EAEF',
      borderWidth: 1,
      padding: [px(8), px(12)],
      textStyle: {
        color: '#617185',
        fontSize: px(12),
      },
      extraCssText: `border-radius: ${px(8)}px; box-shadow: 0 ${px(4)}px ${px(12)}px rgba(0, 0, 0, 0.08);`,
      formatter: (params) => {
        if (!params || !params.length) return ''
        const index = params[0].dataIndex
        const town = list[index]
        const townName = town?.name || ''
        const total = town?.totalNumber || 0
        const veryHigh = town?.veryHighNumber || 0
        const high = town?.highNumber || 0

        return `
          <div style="font-weight: 600; color: #222527; margin-bottom: ${px(4)}px;">${townName}</div>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: ${px(12)}px; margin-bottom: ${px(2)}px;">
            <span><i style="display: inline-block; width: ${px(8)}px; height: ${px(8)}px; border-radius: 50%; background: #F08D62; margin-right: ${px(6)}px;"></i>极高风险单元</span>
            <b style="color: #222527;">${veryHigh}</b>
          </div>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: ${px(12)}px; margin-bottom: ${px(2)}px;">
            <span><i style="display: inline-block; width: ${px(8)}px; height: ${px(8)}px; border-radius: 50%; background: #8FB6F6; margin-right: ${px(6)}px;"></i>高风险单元</span>
            <b style="color: #222527;">${high}</b>
          </div>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: ${px(12)}px; margin-top: ${px(4)}px; padding-top: ${px(4)}px; border-top: 1px solid #EDF1F5;">
            <span>总计</span>
            <b style="color: #007BFF;">${total}</b>
          </div>
        `
      },
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: {
        lineStyle: {
          color: '#E7E7E7',
          width: 1,
        },
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#617185',
        fontSize: px(12),
        interval: 0,
        margin: px(10),
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      splitLine: {
        lineStyle: {
          color: '#E7E7E7',
          type: 'solid',
          width: 1,
        },
      },
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#617185',
        fontSize: px(12),
        margin: px(8),
      },
    },
    series: [
      {
        name: '极高风险单元',
        type: 'bar',
        stack: 'townRisk',
        barWidth: px(12),
        itemStyle: {
          color: '#F08D62',
          borderRadius: highData.map((h) => (h === 0 ? [px(2), px(2), 0, 0] : [0, 0, 0, 0])),
        },
        data: veryHighData,
      },
      {
        name: '高风险单元',
        type: 'bar',
        stack: 'townRisk',
        barWidth: px(12),
        itemStyle: {
          color: '#8FB6F6',
          borderRadius: [px(2), px(2), 0, 0],
        },
        label: {
          show: true,
          position: 'top',
          distance: px(4),
          color: '#222527',
          fontFamily: "'Alimama FangYuanTi VF', sans-serif",
          fontSize: px(14),
          fontWeight: 'Bold-Square',
          formatter: (params) => {
            const val = totalData[params.dataIndex]
            return val > 0 ? val : ''
          },
        },
        data: highData,
      },
    ],
  }

  chartInstance.setOption(option, true)
}

const handleResize = debounce(() => {
  if (chartInstance) {
    chartInstance.resize()
    renderChart()
  }
}, 100)

watch(
  () => props.data,
  () => {
    nextTick(() => {
      renderChart()
    })
  },
  { deep: true },
)

onMounted(() => {
  nextTick(() => {
    renderChart()
    if (chartRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        handleResize()
      })
      resizeObserver.observe(chartRef.value)
    }
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style lang="less" scoped>
.town-risk-chart-container {
  width: 100%;
  height: 220px;
  position: relative;
}

.chart-box {
  width: 100%;
  height: 100%;
}
</style>
