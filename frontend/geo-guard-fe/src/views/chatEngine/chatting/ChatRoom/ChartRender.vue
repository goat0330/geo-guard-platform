<script lang="jsx">
import { h, onMounted, ref } from 'vue'
import LineChart from './charts/LineChart.vue'
import PieChart from './charts/PieChart.vue'
import BarChart from './charts/BarChart.vue'

export default {
  props: {
    data: {
      type: String,
      default: '',
    },
  },
  setup(props, { expose }) {
    const chartRef = ref()
    const chartCompEnum = {
      line: LineChart,
      pie: PieChart,
      bar: BarChart,
    }
    const xData = ref([])
    const yData = ref([])
    const xName = ref('')
    const yName = ref('')
    const chartType = ref('')
    const chartData = ref({})
    const openDataZoom = ref(false)
    // 返回顺续不稳定，通过权重排序
    const riskTypeWeight = {
      极高风险: 4,
      高风险: 3,
      中风险: 2,
      低风险: 1,
    }

    function parseChartJson(str = '') {
      const chartJson = str.replace('(@json)', '').trim()
      try {
        return JSON.parse(chartJson)
      } catch (error) {
        return JSON.parse(chartJson.replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\'))
      }
    }

    function dealChartStr(str) {
      let result
      try {
        result = parseChartJson(str)
        // let { gtype, data } = result[0] || {}
        chartType.value = result[0].gtype
        chartData.value = result[0] || {}
        xName.value = chartData.value.xAxisUnit || ''
        yName.value = chartData.value.yAxisUnit || ''

        /***
         * 数据的数量超过10条时，默认开启dataZoom
         * series本身数量过多，series成员最大数量过多
         * ***/
        if (chartData.value?.series?.length > 10 || chartData.value?.series?.some((s) => s.data?.length > 10)) {
          openDataZoom.value = true
        } else {
          openDataZoom.value = false
        }
      } catch (error) {
        result = []
      } finally {
        return result
      }
    }

    function getChartDataURL() {
      return chartRef.value.getChartDataURL()
    }

    function renderChart() {
      if (!chartCompEnum[chartType.value]) return
      return chartType.value
        ? h(chartCompEnum[chartType.value], {
            ref: chartRef,
            openDataZoom: openDataZoom.value,
            xData: chartData.value?.xAxis || [],
            xName: xName.value,
            yName: yName.value,
            // data: yData.value,
            seriesData: chartData.value?.series || chartData.value?.data || [],
          })
        : null
    }

    expose({
      getChartDataURL,
    })

    onMounted(() => {
      dealChartStr(props.data)
    })
    return () => (
      <div
        class="msg-chart-wrap"
        style={{
          height: chartType.value === 'no' ? '0' : '260px',
        }}
      >
        {renderChart()}
      </div>
    )
  },
}
</script>

<style lang="less" scoped>
.msg-chart-wrap {
  margin: 16px 0;
  width: 50%;
  min-width: 400px;
  max-width: 600px;
  height: 260px;
}
</style>
