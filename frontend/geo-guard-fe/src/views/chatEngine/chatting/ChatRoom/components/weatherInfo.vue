<template>
  <div class="weather-info">
    <el-tabs v-model="activeIndex" class="demo-tabs">
      <el-tab-pane v-for="(item, key) in weatherData" :key="key" :label="item.showTime" :name="key"> </el-tab-pane>
    </el-tabs>
    <div class="level-box">
      <div
        v-for="(level, index) in levels"
        :key="index"
        :class="{
          ['level-' + level.value]: true,
          'is-active': selectedLevel === level.value,
          'each-level': true,
        }"
        @click="changeLevel(level)"
      >
        {{ level.label }}
      </div>
    </div>
    <div class="level-detail">
      <div class="each-line">
        <div class="line-label">预警类型：</div>
        <div class="line-value">{{ displayData.type }}</div>
      </div>
      <div class="each-line">
        <div class="line-label">预警发布时间：</div>
        <div class="line-value">{{ displayData.pub_time }}</div>
      </div>
      <div class="each-line">
        <div class="line-label">预警有效期：</div>
        <div class="line-value">{{ displayData.pub_time + ' 至 ' + displayData.range_time }}</div>
      </div>
      <div class="each-line">
        <div class="line-label">影响区域：</div>
        <div class="line-value">{{ displayData.area?.join(',') }}</div>
      </div>
      <div class="each-line">
        <div class="line-label">关键提示：</div>
        <div class="line-value">{{ displayData.tips }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
const props = defineProps({
  weatherData: {
    type: Array,
    required: true,
  },
  data: {
    type: Object,
    default: () => ({}),
  },
})

const activeIndex = ref(0)
const selectedLevel = ref(1)
const displayData = ref({
  type: '地质灾害气象风险预警',
  pub_time: '',
  range_time: '',
  area: [],
  tips: [],
})

const levels = [
  { label: '蓝色风险', value: 1, key: 'blue_level' },
  { label: '黄色风险', value: 2, key: 'yellow_level' },
  { label: '橙色风险', value: 3, key: 'orange_level' },
  { label: '红色风险', value: 4, key: 'red_level' },
]

function changeLevel(level) {
  selectedLevel.value = level.value
}

console.log('天气数据:', props.data)
// watch(
//   () => props.data,
//   (val) => {
//     weatherData.value = val.data.map((item, index) => ({
//       ...item,
//       showTime: dayjs(item.time).format('MM月DD日'),
//       tips: val.key_tips,
//     }))
//   },
//   {
//     immediate: true,
//   },
// )

watch(
  () => [activeIndex.value, selectedLevel.value],
  (val) => {
    console.log('天气数据--------------11', props.data)
    const [index, level] = val
    const weatherData = props.data.data[index]
    // level变化时，这三项目前不变
    displayData.value.pub_time = weatherData.publish_date
    displayData.value.range_time = weatherData.valid_date
    displayData.value.tips = props.data.key_tips

    const targetLevelKey = levels.find((l) => l.value === level)?.key
    displayData.value.area = weatherData.data[targetLevelKey]?.streets || []
    console.log('更新显示数据:', displayData.value)
  },
  {
    immediate: true,
    deep: true,
  },
)
</script>

<style lang="less" scoped>
.level-box {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;

  .each-level {
    width: 88px;
    line-height: 28px;
    text-align: center;
    border-radius: 100px;
    background: #ebeff5;
    border: 1px solid #ebeff5;
    font-size: 14px;
    color: #506073;
    cursor: pointer;
    &.level-1 {
      &:hover,
      &.is-active {
        color: #3561fa;
        background: #3561fa33;
        border: 1px solid #3561fa;
      }
    }
    &.level-2 {
      &:hover,
      &.is-active {
        color: #e8a600;
        background: #e8a60033;
        border: 1px solid #e8a600;
      }
    }
    &.level-3 {
      &:hover,
      &.is-active {
        color: #ff5500;
        background: #ff550033;
        border: 1px solid #ff5500;
      }
    }
    &.level-4 {
      &:hover,
      &.is-active {
        color: #e23030;
        background: #e230301a;
        border: 1px solid #e23030;
      }
    }
  }
}
.level-detail {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  row-gap: 10px;
  color: #222529;
  font-size: 14px;

  .each-line {
    display: flex;
    line-height: 22px;
    .line-label {
      font-weight: 800;
      flex-shrink: 0;
    }
    .line-value {
    }
  }
}
</style>
