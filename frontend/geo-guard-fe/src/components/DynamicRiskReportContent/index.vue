<template>
  <div class="report-content" :class="{ 'is-download': isDownload }">
    <h2>{{ reportDate }}地质灾害风险程度预测</h2>
    <p>基于地质环境条件、实时降雨量、监测点情况、承灾体分布及抗灾能力等，现发布今日地质灾害风险预测结果。</p>
    <p>
      全域共 <strong class="number-text">{{ total }}</strong> 个斜坡单元，
      极高风险 <strong class="number-text extreme">{{ counts[4] }}</strong> 处，
      高风险 <strong class="number-text high">{{ counts[3] }}</strong> 处，
      中风险 <strong class="number-text middle">{{ counts[2] }}</strong> 处，
      低风险 <strong class="number-text low">{{ counts[1] }}</strong> 处。明细附后。
    </p>

    <section v-for="group in visibleGroups" :key="group.level">
      <h3 :style="{ color: group.color }">{{ group.order }}、{{ group.label }}风险区{{ group.count }}处</h3>
      <p v-for="area in group.areas" :key="area.name">
        <strong>{{ area.name }}</strong>
        （<button v-for="(unit, index) in area.units" :key="unit.id" type="button" @click="emit('choose-unit', unit)">{{ getUnitDisplayName(unit) }}{{ index < area.units.length - 1 ? '、' : '' }}</button>）
      </p>
    </section>

    <p v-if="!visibleGroups.length">【当前暂无有效斜坡单元数据】</p>
    <p>请上述区域相关责任人员按照地质灾害防范要求及辖区防御响应方案、应急预案要求，开展巡查、排查、核查，结合实际开展会商研判、避险撤离，及时报送灾（险）情信息，并做好各类预警处置。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import { RISK_LEVEL_COLOR, RISK_LEVEL_TEXT } from '@/utils/enum.js'

defineOptions({ name: 'DynamicRiskReportContent' })

const props = defineProps({
  units: { type: Array, default: () => [] },
  isDownload: Boolean,
  date: { type: String, default: '' },
})
const emit = defineEmits(['choose-unit'])
const reportDate = computed(() => {
  const d = dayjs(props.date)
  return (d.isValid() ? d : dayjs()).format('YYYY年MM月DD日')
})

const total = computed(() => props.units.length)
const counts = computed(() => props.units.reduce((result, item) => {
  const level = Number(item?.dynamicRiskLevel)
  if (result[level] !== undefined) result[level] += 1
  return result
}, { 1: 0, 2: 0, 3: 0, 4: 0 }))

const visibleGroups = computed(() => {
  const orderText = ['一', '二', '三', '四']
  return [4, 3, 2, 1].map((level) => {
    const levelUnits = props.units.filter((item) => Number(item?.dynamicRiskLevel) === level)
    const areaMap = new Map()
    levelUnits.forEach((item) => {
      const name = [item?.county, item?.street, item?.village].filter(Boolean).join('') || '未明确区域'
      if (!areaMap.has(name)) areaMap.set(name, [])
      areaMap.get(name).push(item)
    })
    return {
      level,
      label: RISK_LEVEL_TEXT[level],
      color: RISK_LEVEL_COLOR[level],
      count: levelUnits.length,
      areas: Array.from(areaMap, ([name, units]) => ({ name, units })),
    }
  }).filter((group) => group.count).map((group, index) => ({ ...group, order: orderText[index] }))
})

const getUnitDisplayName = (unit) => {
  const raw = String(unit?.name || unit?.slopeUnitName || unit?.slopeName || unit?.id || '').trim()
  if (!raw) return '--'
  if (raw.endsWith('斜坡') || raw.endsWith('斜坡单元')) return raw
  return `${raw}斜坡`
}
</script>

<style lang="less" scoped>
.report-content {
  color: #383c41;
  font-size: 16px;
  line-height: 26px;
  word-break: break-word;

  &:not(.is-download) {
    padding-top: 4px;
    padding-bottom: 20px;
  }

  h2 {
    margin: 0 0 4px;
    color: #222527;
    font-size: 24px;
    line-height: 34px;
    text-align: left;
  }

  h3 {
    margin: 8px 0 0;
    font-size: 16px;
    line-height: 26px;
  }

  p {
    margin: 0;
  }

  h2 + p {
    color: #222527;
    font-weight: 500;
  }

  h2 + p + p {
    margin-top: 2px;
  }

  button {
    padding: 0;
    border: 0;
    background: transparent;
    color: #007bff;
    font: inherit;
    text-decoration: underline;
    cursor: pointer;
  }
}

.number-text {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}

.extreme {
  color: #e45b5b;
}

.high {
  color: #ff922c;
}

.middle {
  color: #f9c568;
}

.low {
  color: #007bff;
}

.is-download {
  width: 760px;
  padding: 20px;
  background: #ffffff;
  box-sizing: border-box;

  button {
    color: #222527;
    text-decoration: none;
  }
}
</style>
