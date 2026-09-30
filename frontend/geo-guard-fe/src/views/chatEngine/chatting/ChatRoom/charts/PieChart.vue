<template>
  <div class="chart-wrapper">
    <RTitle v-if="title" :title="title">
      <div v-if="titleLegend" class="tiele-lengend">
        <div v-for="item in data" :key="item.name" class="item">
          <div class="symbol" :style="{ background: item.color }"></div>
          <span>{{ item.name }}</span>
        </div>
      </div>
    </RTitle>
    <div ref="chartRef" class="chart-box"></div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import RTitle from '@/components/RTitle/index.vue'
import { useResizeObserver } from '@vueuse/core'
import { debounce } from '@/utils'
import {
  COlOR_FUN,
  COLORS,
} from '@/views/chatEngine/chatting/ChatRoom/charts/coommon.js'

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  showLegend: {
    type: Boolean,
    default: true,
  },
  // 标题后面显示图例
  titleLegend: {
    type: Boolean,
    default: false,
  },
  smooth: {
    type: Boolean,
    default: false,
  },
  xData: {
    type: Array,
    default: [],
  },
  xName: {
    type: String,
    default: '',
  },
  gird: {
    type: Object,
    default: {
      top: 40,
      left: 5,
      right: 10,
      bottom: 1,
      containLabel: true,
    },
  },
  yName: {
    type: String,
    default: '',
  },
  data: {
    type: Array,
    default: [],
  },
  seriesData: {
    type: Array,
    default: () => [],
  },
})

watch(
  () => [props.xData, props.data],
  () => {
    drawChart()
  },
  {
    deep: true,
  },
)

const chartRef = ref()
const resizeCb = debounce(() => {
  // drawChart()
  myChart && myChart.resize()
})
useResizeObserver(chartRef, (entry, observer) => {
  resizeCb(entry, observer)
})
// 绘制条形图
let myChart = null
async function drawChart() {
  // let chartData =
  //   props.data?.map((v) => {
  //     let total = v.data.reduce((total, cur) => total + cur, 0)
  //     return { name: v.name, value: total }
  //   }) || []
  await nextTick()

  if (!myChart) {
    myChart = echarts.init(chartRef.value)
  }
  // 绘制图表
  const option = {
    color: COlOR_FUN(props.seriesData),
    tooltip: {
      trigger: 'item',
    },
    legend: {
      top: '5%',
      left: 'center',
    },
    series: [
      {
        // name: 'Access From',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        padAngle: 2,
        itemStyle: {
          borderRadius: 10,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        // data: chartData,
        data: props.seriesData,
      },
    ],
  }
  myChart.setOption(option, true)
}

function getChartDataURL() {
  return myChart.getDataURL({
    type: 'png',
    pixelRation: 2,
    backgroundColor: '#fff',
  })
}

onMounted(() => {
  drawChart()
})

defineExpose({
  getChartDataURL,
})
</script>

<style lang="less" scoped>
.chart-wrapper {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.tiele-lengend {
  display: flex;
  align-items: center;
  justify-content: end;
  gap: 10px;
  .item {
    color: #878898;
    font-family: 'MiSans';
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 5px;
    .symbol {
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }
  }
}
.chart-box {
  flex: 1;
  min-height: 0;
  min-width: 0;
  backdrop-filter: blur(2px);
}
</style>
