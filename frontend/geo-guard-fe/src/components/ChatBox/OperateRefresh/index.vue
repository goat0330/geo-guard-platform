<template>
  <div>
    <Operate :title="title" :operate-list="operateList" :icon="icon" :choose-item="chooseItem" />
    <Refresh :refresh="refresh" :gap="gap" />
  </div>
</template>

<script setup>
import Operate from '../Operate/index.vue'
import Refresh from '../Refresh/index.vue'
import { onMounted, ref } from 'vue'
import { getSlopeUnitRisk } from '@/api/common.js'
import dayjs from 'dayjs'
import { fillTemplate, createRandomPicker } from '@/utils/index.js'

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  icon: {
    type: String,
    default: '',
  },
  gap: {
    type: Number,
    default: 20,
  },
  questions: {
    type: Array,
    default: () => [],
  },
  chooseItem: {
    type: Function,
    default: () => {},
  },
})

const operateList = ref([])
const params = ref({})
const { pickRandomItems } = createRandomPicker()
const { pickRandomItems: pickRandomUnit } = createRandomPicker()


const refresh = () => {
  const questions = props.questions || []
  let pickQuestion = pickRandomItems(questions, 3)
  params.value.unit = pickRandomUnit(params.value.unitList, 1)?.[0]?.slopeUnitId || 1
  operateList.value = [
    { tip: '智能查询', content: fillTemplate(pickQuestion[0], params.value) },
    { tip: '智能查询', content: fillTemplate(pickQuestion[1], params.value) },
    { tip: '智能查询', content: fillTemplate(pickQuestion[2], params.value) },
  ]
}

const init = async () => {
  const questions = props.questions || []
  let res = await getSlopeUnitRisk({
    orderByColumn: 'dynamicRiskValue',
    isAsc: 'desc',
  })
  params.value = {
    unitList: res.slice(0, 5) || [],
    start: dayjs().startOf('month').format('YYYY-MM-DD'),
    end: dayjs().endOf('month').format('YYYY-MM-DD'),
  }
  params.value.unit = pickRandomUnit(params.value.unitList, 1)?.[0]?.slopeUnitId || 1
  let pickQuestion = pickRandomItems(questions, 3)
  operateList.value = [
    { tip: '智能查询', content: fillTemplate(pickQuestion[0], params.value) },
    { tip: '智能查询', content: fillTemplate(pickQuestion[1], params.value) },
    { tip: '智能查询', content: fillTemplate(pickQuestion[2], params.value) },
  ]
}

onMounted(() => {
  init()
})
</script>

<style scoped lang="less"></style>