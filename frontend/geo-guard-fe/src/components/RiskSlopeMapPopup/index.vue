<template>
  <Transition name="risk-popup">
    <section v-if="visible" class="risk-popup" :style="popupStyle" aria-label="斜坡单元详情">
      <header class="popup-header">
        <div class="title-row">
          <h2>{{ slopeTitle }}</h2>
          <span class="risk-tag" :class="riskLevelClass">{{ slope.level }}</span>
        </div>
        <button class="close-button" title="关闭" @click="emit('close')">
          <i class="iconfont icon-close"></i>
        </button>
      </header>

      <div class="address" :title="slopeAddress"><i class="iconfont icon-address"></i>{{ slopeAddress }}</div>

      <h3>基础信息：</h3>
      <div class="base-info">
        <div v-for="item in baseInfoItems" :key="item.key" class="info-item">
          <span><i :class="['iconfont', item.icon]"></i>{{ item.label }}</span>
          <strong :title="item.tooltip || item.value">
            {{ item.value }}<small v-if="item.unit && item.value !== '--'">{{ item.unit }}</small>
          </strong>
        </div>
      </div>

      <div class="rainfall-section">
        <h3>近4小时降雨量：</h3>
        <BaseChart ref="rainfallChartRef" class="rainfall-chart" :get-option="getRainfallChartOption" />
      </div>

      <button class="analysis-button" @click="emit('risk-analysis', slope)">
        <i class="iconfont icon-a-Chart-linezhexiantu"></i>风险分析
      </button>
    </section>
  </Transition>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, ref, watch } from 'vue'
import dayjs from 'dayjs'
import BaseChart from '@/components/BaseChart/index.vue'
import { getRainfallSequence, getSlopeUnitDetail, getSlopeUnitPerson } from '@/api/common.js'
import { formatSlopeAddress, formatSlopeName, toKm2 } from '@/utils/riskEvaluation.js'

defineOptions({ name: 'RiskSlopeMapPopup' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  slope: {
    type: Object,
    default: () => ({}),
  },
  position: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'risk-analysis'])

const MARKER_HEIGHT = 30
const MARKER_GAP = 17
const RAINFALL_HOUR_COUNT = 4

const rainfallChartRef = ref(null)
const rainfallTrend = ref([])
let rainfallRequestSequence = 0

const extraBaseInfo = ref({
  populationCount: null,
  buildingCount: null,
  contactName: '',
  contactPhone: '',
  area: '',
  address: '',
})
let baseInfoRequestSequence = 0

const targetSlopeUnitId = computed(() => {
  return (
    props.slope?.slopeUnitId ||
    props.slope?.raw?.slopeUnitId ||
    props.slope?.raw?.slopeUnit?.id ||
    props.slope?.id ||
    props.slope?.raw?.id ||
    ''
  )
})

const slopeTitle = computed(
  () =>
    formatSlopeName(
      props.slope.name ||
        props.slope.slopeUnitName ||
        props.slope.slopeName ||
        props.slope.code ||
        props.slope.slopeUnitId,
    ) || '未知斜坡单元',
)

const slopeAddress = computed(() => {
  if (props.slope.address && props.slope.address !== '--') {
    return props.slope.address
  }
  return extraBaseInfo.value.address || props.slope.address || '--'
})

const popupStyle = computed(() => {
  if (!props.position) return {}
  // 点位坐标位于图片底部，面板底部与图片顶部保持 17px 间隔。
  return {
    left: `${props.position.x}px`,
    top: `${props.position.y - MARKER_HEIGHT - MARKER_GAP}px`,
  }
})

const riskLevelClass = computed(() => {
  const levelClassMap = {
    无风险: 'is-none',
    极高风险: 'is-extreme',
    高风险: 'is-high',
    中风险: 'is-medium',
    低风险: 'is-low',
  }
  return levelClassMap[props.slope.level]
})

const baseInfoItems = computed(() => {
  // 地理范围（面积）
  const areaVal = props.slope.area || extraBaseInfo.value.area
  const hasArea = areaVal !== null && areaVal !== undefined && areaVal !== ''

  // 人口底数
  const popVal =
    extraBaseInfo.value.populationCount ??
    props.slope.populationCount ??
    (props.slope.population !== '' && props.slope.population != null ? props.slope.population : null)
  const hasPop = popVal !== null && popVal !== undefined && popVal !== ''

  // 建筑资源（建筑数量）
  const bldVal =
    extraBaseInfo.value.buildingCount ??
    props.slope.buildingCount ??
    (props.slope.buildings !== '' && props.slope.buildings != null ? props.slope.buildings : null)
  const hasBld = bldVal !== null && bldVal !== undefined && bldVal !== ''

  // 联系人（负责人）
  const contactName = extraBaseInfo.value.contactName || props.slope.contact || ''
  const contactPhone = extraBaseInfo.value.contactPhone || ''
  let contactVal = '--'
  let contactTooltip = ''
  if (contactName && contactPhone) {
    contactVal = contactName
    contactTooltip = `${contactName} (${contactPhone})`
  } else if (contactName) {
    contactVal = contactName
    contactTooltip = contactName
  } else if (contactPhone) {
    contactVal = contactPhone
    contactTooltip = contactPhone
  }

  return [
    {
      key: 'area',
      label: '地理范围',
      value: hasArea ? areaVal : '--',
      unit: hasArea ? 'km²' : '',
      tooltip: hasArea ? `${areaVal} km²` : '',
      icon: 'icon-a-Frame4',
    },
    {
      key: 'population',
      label: '人口底数',
      value: hasPop ? popVal : '--',
      unit: hasPop ? '人' : '',
      tooltip: hasPop ? `${popVal} 人` : '',
      icon: 'icon-a-Frame5',
    },
    {
      key: 'buildings',
      label: '建筑资源',
      value: hasBld ? bldVal : '--',
      unit: hasBld ? '栋' : '',
      tooltip: hasBld ? `${bldVal} 栋` : '',
      icon: 'icon-a-Frame6',
    },
    {
      key: 'contact',
      label: '联系人',
      value: contactVal,
      unit: '',
      tooltip: contactTooltip,
      icon: 'icon-a-Frame71',
    },
  ]
})

const defaultRainfallHours = () => {
  const now = dayjs()
  return Array.from(
    { length: RAINFALL_HOUR_COUNT },
    (_, index) => now.subtract(RAINFALL_HOUR_COUNT - index - 1, 'hour').format('HH:00'),
  )
}

/**
 * 按恩施地灾的 hourTime、rainfall 字段取值，并整理为最近4小时数据。
 * @param {Array} list 当天逐小时雨量序列
 * @returns {Array<{time: string, value: number, current: boolean}>}
 */
const formatRecentRainfall = (list = []) => {
  const rainfallByHour = new Map()

  list.forEach((item) => {
    if (!item?.hourTime) return

    const hour = dayjs(item.hourTime)
    if (!hour.isValid()) return

    rainfallByHour.set(hour.format('YYYY-MM-DD HH'), Number(item.rainfall) || 0)
  })

  const now = dayjs()
  return Array.from({ length: RAINFALL_HOUR_COUNT }, (_, index) => {
    const offset = RAINFALL_HOUR_COUNT - index - 1
    const hour = now.subtract(offset, 'hour')
    return {
      time: hour.format('HH:00'),
      value: rainfallByHour.get(hour.format('YYYY-MM-DD HH')) ?? 0,
      current: offset === 0,
    }
  })
}

const renderRainfallChart = async () => {
  await nextTick()
  rainfallChartRef.value?.renderChart(getRainfallChartOption)
}

const getRainfallChartOption = (px) => {
  const hasData = rainfallTrend.value.length > 0
  const times = hasData ? rainfallTrend.value.map((item) => item.time) : defaultRainfallHours()
  const values = hasData ? rainfallTrend.value.map((item) => Number(item.value)) : []
  const currentTime = hasData
    ? rainfallTrend.value.find((item) => item.current)?.time
    : times[times.length - 1]
  const dataMax = Math.max(...values, 0)
  const yAxisInterval = dataMax === 0 ? 5 : (Math.ceil(dataMax / 3 / 5) * 5 || 5)

  return {
    animation: false,
    grid: {
      top: px(20),
      right: 0,
      bottom: 0,
      left: px(26),
    },
    graphic: !hasData ? [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: {
        text: '暂无降雨数据',
        fill: '#9096A2',
        fontSize: px(12),
      },
    }] : [],
    tooltip: hasData ? {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#A6ACB8', width: px(1) } },
      backgroundColor: '#FFFFFF',
      borderWidth: 0,
      padding: [px(10), px(14)],
      textStyle: { color: '#617185', fontSize: px(12) },
      extraCssText: `border-radius:${px(8)}px;box-shadow:0 ${px(4)}px ${px(10)}px #0087ca24;`,
      formatter: (params) => {
        const item = params?.[0]
        if (!item) return ''
        return `<span style="display:inline-block;width:${px(12)}px;height:${px(2)}px;margin-right:${px(8)}px;vertical-align:middle;background:#42CFF5;"></span>${item.axisValue}降雨量&nbsp;&nbsp;<b>${item.data}mm</b>`
      },
    } : { show: false },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times,
      axisLine: { lineStyle: { color: '#C8D2DC', width: px(1) } },
      axisTick: { show: false },
      axisLabel: {
        color: '#9096A2',
        fontSize: px(10),
        margin: px(8),
        formatter: (value) => value === currentTime ? `{current|${value}}` : value,
        rich: { current: { color: '#DD4739', fontSize: px(10) } },
      },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: dataMax === 0 ? 15 : yAxisInterval * 3,
      interval: yAxisInterval,
      name: '(mm)',
      nameTextStyle: { color: '#A6ACB8', fontSize: px(10), padding: [0, 0, 0, px(-20)] },
      axisLabel: { color: '#A6ACB8', fontSize: px(10) },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#E0EEFA', width: px(1) } },
    },
    series: [{
      type: 'line',
      data: values,
      smooth: false,
      symbol: 'none',
      lineStyle: { color: '#42CFF5', width: px(2) },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(66, 207, 245, 0.42)' },
          { offset: 1, color: 'rgba(66, 207, 245, 0.02)' },
        ]),
      },
      markLine: (hasData && currentTime) ? {
        silent: true,
        symbol: 'none',
        lineStyle: { color: '#DD4739', width: px(1) },
        label: { show: false },
        data: [{ xAxis: currentTime }],
      } : undefined,
    }],
  }
}

watch(
  [() => props.visible, () => targetSlopeUnitId.value],
  async ([visible, unitId], _, onCleanup) => {
    const requestSequence = ++baseInfoRequestSequence
    let cancelled = false
    onCleanup(() => {
      cancelled = true
    })

    extraBaseInfo.value = {
      populationCount: null,
      buildingCount: null,
      contactName: '',
      contactPhone: '',
      area: '',
      address: '',
    }

    if (!visible || !unitId) return

    try {
      const [detailRes, personRes] = await Promise.allSettled([
        getSlopeUnitDetail(unitId, { noCheckCode: true, notUseError: true }),
        getSlopeUnitPerson(unitId, { noCheckCode: true, notUseError: true }),
      ])
      if (cancelled || requestSequence !== baseInfoRequestSequence) return

      const detail = detailRes.status === 'fulfilled' ? detailRes.value : null
      const person = personRes.status === 'fulfilled' ? personRes.value : null

      extraBaseInfo.value = {
        populationCount: detail?.populationCount ?? null,
        buildingCount: detail?.buildingCount ?? null,
        contactName: person?.userName ?? '',
        contactPhone: person?.phoneNumber ?? '',
        area: detail?.area != null ? toKm2(detail.area) : '',
        address: formatSlopeAddress(detail),
      }
    } catch (error) {
      if (cancelled || requestSequence !== baseInfoRequestSequence) return
      console.warn('获取斜坡单元详情或人员失败:', error)
    }
  },
  { immediate: true },
)

watch(
  [() => props.visible, () => targetSlopeUnitId.value],
  async ([visible, slopeUnitId], _, onCleanup) => {
    const requestSequence = ++rainfallRequestSequence
    let cancelled = false
    onCleanup(() => {
      cancelled = true
    })

    rainfallTrend.value = []
    await renderRainfallChart()
    if (!visible || !slopeUnitId) return

    try {
      const res = await getRainfallSequence({ slopeUnitId })
      if (cancelled || requestSequence !== rainfallRequestSequence) return

      rainfallTrend.value = formatRecentRainfall(Array.isArray(res?.hourlySeries) ? res.hourlySeries : [])
      await renderRainfallChart()
    } catch (error) {
      if (cancelled || requestSequence !== rainfallRequestSequence) return
      console.warn('获取斜坡单元雨量序列失败:', error)
    }
  },
  { immediate: true },
)
</script>

<style lang="less" scoped>
.risk-popup {
  position: absolute;
  z-index: 12;
  width: 378px;
  //height: 364px;
  padding: 14px 20px;
  box-sizing: border-box;
  border: 2px solid #ffffff;
  border-radius: 10px;
  background: #ffffffdb;
  box-shadow: 0 4px 10px #0087ca24;
  color: #222527;
  backdrop-filter: blur(2px);
  transform: translate(-50%, -100%);
}

.popup-header,
.title-row,
.address,
.analysis-button {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.popup-header {
  justify-content: space-between;
}

.title-row {
  min-width: 0;
  gap: 16px;
}

h2 {
  overflow: hidden;
  margin: 0;
  font-size: 18px;
  line-height: 30px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.risk-tag {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background: #ff922c;
  color: #ffffff;
  font-size: 10px;
  width: 46px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.risk-tag.is-extreme,
.risk-tag.is-high {
  background: #ef4c46;
}

.risk-tag.is-low {
  background: #007bff;
}

.risk-tag.is-none {
  background: #878898;
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #222527;
  cursor: pointer;
}

.close-button i {
  font-size: 16px;
}

.address {
  gap: 4px;
  margin-top: 6px;
  color: #617185;
  font-size: 14px;
  line-height: 22px;
}

.address i {
  font-size: 14px;
}

h3 {
  margin: 12px 0 4px;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
}

.base-info {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  //min-height: 80px;
  padding: 4px 8px;
  box-sizing: border-box;
  border: 1px solid #8fc8ff;
  border-radius: 8px;
  background: rgba(220, 237, 255, 0.45);
}

.info-item {
  min-width: 0;
  padding: 0 8px;
  text-align: center;
  border-right: 1px solid #8fc8ff;
}

.info-item:last-child {
  border-right: 0;
}

.info-item span {
  display: block;
  color: #617185;
  font-size: 10px;
  line-height: 18px;
  white-space: nowrap;
}

.info-item span i {
  margin-right: 4px;
  font-size: 10px;
}

.info-item strong {
  display: block;
  overflow: hidden;
  color: #007bff;
  font-size: 14px;
  line-height: 26px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.info-item small,
.rainfall-value small {
  margin-left: 2px;
  font-size: 12px;
}

.rainfall-section h3 {
  margin-top: 12px;
  margin-bottom: 0;
  font-size: 14px;
  line-height: 20px;
}

.rainfall-chart {
  width: 100%;
  height: 88px;
}

.analysis-button {
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 14px;
  padding: 14px 0 0;
  border: 0;
  border-top: 1px solid #ffffff99;
  background: transparent;
  color: #007bff;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  font-size: 14px;
  font-style: normal;
  font-weight: 500;
  line-height: 14px;
  cursor: pointer;
}

.analysis-button i {
  font-size: 16px;
}

.risk-popup-enter-active,
.risk-popup-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.risk-popup-enter-from,
.risk-popup-leave-to {
  opacity: 0;
  transform: translate(-50%, calc(-100% + 12px));
}

@media (max-width: 560px) {
  .risk-popup {
    width: min(378px, calc(100vw - 24px));
  }
}
</style>
