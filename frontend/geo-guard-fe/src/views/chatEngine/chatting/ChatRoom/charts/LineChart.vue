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
      left: 15,
      right: 90,
      bottom: 1,
      containLabel: true,
    },
  },
  openDataZoom: {
    type: Boolean,
    default: false,
  },
  yName: {
    name: '',
    // show: true,
    // type: String,
    // default: '',
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
  await nextTick()

  if (!myChart) {
    myChart = echarts.init(chartRef.value)
  }

  // 调整grid
  let gridBottom = props.gird.bottom || 10
  if (props.openDataZoom) {
    if (!props.grid || props.grid.bottom < 30) {
      gridBottom = 30
    }
  }

  // 绘制图表
  const option = {
    color: COlOR_FUN(props.seriesData),
    tooltip: {
      trigger: 'axis',
      borderColor: 'transparent',
      // borderWidth: 0,
    },
    grid: {
      ...props.gird,
      containLabel: true,
      bottom: gridBottom,
    },
    legend: {
      show: props.showLegend,
      top: 0,
      right: 0,
      textStyle: {
        fontSize: '0.75rem',
        color: '#878898',
        fontWeight: 500,
      },
    },
    ...(props.openDataZoom
      ? {
          dataZoom: [
            {
              show: true,
              realtime: true,
              start: 0,
              end: 40,
              xAxisIndex: [0, 1],
            },
            {
              type: 'inside',
              realtime: true,
              start: 0,
              end: 40,
              xAxisIndex: [0, 1],
            },
          ],
        }
      : {}),
    xAxis: [
      {
        type: 'category',
        name: props.xName,
        nameGap: 30,
        nameLocation: 'end',
        nameTextStyle: {
          color: '#506073',
          fontSize: 12,
          align: 'center',
          verticalAlign: 'bottom',
          padding: [0, 14, 0, 0],
        },
        data: props.xData,
        // 色块区域
        splitArea: {
          show: false,
          areaStyle: {
            color: ['#092348', '#061840'],
            opacity: 0.3,
          },
        },
        splitLine: {
          show: false,
          interval: 'auto',
        },
        axisTick: {
          show: false,
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#DFE4F0',
          },
        },
        axisLabel: {
          align: 'center',
          color: '#B2B3BD',
          margin: 5,
          fontSize: '0.75rem',
        },
      },
    ],
    yAxis: [
      {
        type: 'value',
        // backgroundColor: '#547cb6',
        name: props.yName,
        position: 'left',
        nameTextStyle: {
          color: '#506073',
          fontSize: 12,
          align: 'center',
          verticalAlign: 'bottom',
          padding: [0, 14, 0, 0],
        },
        nameLocation: 'end',
        minInterval: 1,
        splitLine: {
          show: true, // 显示第一个Y轴网格线
          lineStyle: {
            color: ['#E7E7E7'],
            shadowColor: 'rgba(83,144,255,0.5)',
            shadowBlur: 0,
            opacity: 1,
            width: 1,
            type: [6, 3],
          },
        },
        axisLabel: {
          align: 'right',
          color: '#B2B3BD',
          margin: 20,
          fontSize: '0.75rem',
        },
      },
    ],
    series: props.seriesData.map((d, key) => ({
      // name: d.name,
      type: 'line',
      smooth: true,
      symbol: 'emptyCircle',
      symbolSize: 8,
      yAxisIndex: 0,
      // itemStyle: {
      //   // 折线拐点标志的样式
      //   borderColor: COLORS[key],
      //   borderWidth: 2,
      //   color: '#fff',
      // },
      lineStyle: {
        color: COLORS[key],
        width: 3,
      },
      tooltip: {
        textStyle: {
          color: '#fff',
        },
        borderColor: 'transparent',
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
      },
      areaStyle: {
        //填充线条下面的面积区域颜色。（areaStyle只是锦上添花）
        opacity: 0.2,
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0,
              color: COLORS[key], // 上处的颜色
            },
            {
              offset: 1,
              color: '#FEFEFF', // 下处的颜色
            },
          ],
          global: false, // 缺省为 false
        },
      },
      // data: d.data,
      ...d,
    })),
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
