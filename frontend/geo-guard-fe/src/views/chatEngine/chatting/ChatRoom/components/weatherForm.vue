<template>
  <div class="modify-box">
    <el-tabs v-model="activeIndex" class="demo-tabs">
      <el-tab-pane v-for="(item, key) in tabWeatherData" :key="key" :label="item.showTime" :name="key"> </el-tab-pane>
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
    <div class="form-box form-weather-form">
      <RForm
        ref="searchFormRef"
        :form-list="formList"
        v-model:form-data="formData"
        :label-width="120 / 16 + 'rem'"
        :defaultColSpan="24"
        noOperateBtn
      />
      <div class="action-btns">
        <el-button @click="handleCancel">取消</el-button>
        <el-button v-if="Number(activeIndex) > 0" type="primary" @click="handlePrev">上一天</el-button>
        <el-button type="primary" v-if="Number(activeIndex) < weatherData.length - 1" @click="handleNext"
          >下一天</el-button
        >
        <el-button type="primary" v-if="Number(activeIndex) === weatherData.length - 1" @click="handleSubmit"
          >提交</el-button
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import dayjs from 'dayjs'
import RForm from '@/components/RForm/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { receiveWeatherWarning } from '@/api/common'
import { getAreaList } from '@/api/common'
import { DEFAULT_AREA_CODE } from '@/utils/enum.js'
import { eventBus, EventKey } from '@/utils/eventBus.js'

const emits = defineEmits(['cancel', 'submit-success'])
const props = defineProps({
  weatherData: {
    type: Array,
    required: true,
  },
  data: {
    type: Object,
    default: () => ({}),
  },
  conversationId: {
    type: String,
    default: '',
  },
  extParams: {
    type: Object,
    default: () => ({}),
  },
})

const levels = [
  { label: '蓝色风险', value: 1, key: 'blue_level' },
  { label: '黄色风险', value: 2, key: 'yellow_level' },
  { label: '橙色风险', value: 3, key: 'orange_level' },
  { label: '红色风险', value: 4, key: 'red_level' },
]

const activeIndex = ref(0)
const searchFormRef = ref()
const areaOptions = ref([])
const selectedLevel = ref(1)
const WARNING_VALID_MAX_HOURS = 24
const WARNING_VALID_MAX_DURATION = WARNING_VALID_MAX_HOURS * 60 * 60 * 1000

// 监听天数切换，自动将风险等级重置为默认的第一级（蓝色风险）
watch(activeIndex, (newIndex, oldIndex) => {
  if (oldIndex !== undefined && newIndex !== oldIndex) {
    syncCurrentFormToLocalData(Number(oldIndex), selectedLevel.value)
  }
  selectedLevel.value = levels[0].value
})

function changeLevel(level) {
  if (selectedLevel.value === level.value) return
  syncCurrentFormToLocalData(Number(activeIndex.value), selectedLevel.value)
  selectedLevel.value = level.value
}

function disabledWarningRangeDate(date) {
  return dayjs(date).isBefore(dayjs().startOf('day'), 'day')
}

function getWarningRangeError(range) {
  if (!Array.isArray(range) || range.length !== 2 || !range[0] || !range[1]) {
    return '请选择预警有效期'
  }

  const start = dayjs(range[0])
  const end = dayjs(range[1])
  if (!start.isValid() || !end.isValid()) {
    return '预警有效期格式不正确'
  }
  if (start.isBefore(dayjs().startOf('day')) || end.isBefore(dayjs().startOf('day'))) {
    return '预警有效期不能早于今天'
  }
  if (end.isBefore(start)) {
    return '预警有效期结束时间不能早于开始时间'
  }
  if (end.valueOf() - start.valueOf() > WARNING_VALID_MAX_DURATION) {
    return `预警有效期不能超过 ${WARNING_VALID_MAX_HOURS} 小时`
  }

  return ''
}

function validateWarningRangeRule(rule, value, callback) {
  const error = getWarningRangeError(value)
  if (error) {
    callback(new Error(error))
    return
  }
  callback()
}

// 本地表单绑定的核心数据，初始化默认的预警类型
const formData = ref({
  type: '地质灾害气象风险预警',
})

// 当前天存在于识别结果、但不在系统区域字典中的区域；回写时保留，避免非标准名称丢失
const extAreaList = ref([])
const OTHER_TOWNS = '其他乡镇'

const formList = computed(() => [
  {
    title: '预警信息来源',
    type: 'Input',
    key: 'type',
    props: {
      disabled: true,
    },
  },
  {
    title: '发布时间',
    type: 'DatePicker',
    key: 'publish_date',
    props: {
      type: 'datetime',
      format: 'YYYY-MM-DD HH:mm',
      'value-format': 'YYYY-MM-DD HH:mm',
    },
    rules: [{ required: true, message: '请选择发布时间', trigger: 'change' }],
  },
  {
    title: '预警有效期',
    type: 'DatePicker',
    key: 'range_date',
    props: {
      type: 'datetimerange',
      format: 'YYYY-MM-DD HH:mm',
      'value-format': 'YYYY-MM-DD HH:mm',
      'disabled-date': disabledWarningRangeDate,
    },
    rules: [
      { required: true, message: '请选择预警有效期', trigger: 'change' },
      { validator: validateWarningRangeRule, trigger: 'change' },
    ],
  },
  {
    title: '影响区域',
    type: 'Select',
    key: 'area',
    options: areaOptions.value,
    props: {
      placeholder: '请选择影响区域',
      multiple: true,
      filterable: true,
    },
  },
  {
    title: '关键提示',
    type: 'Input',
    key: 'tips',
    props: {
      type: 'textarea',
      placeholder: '请输入',
    },
  },
])

const validAreaNames = ref(null)

const localData = ref(JSON.parse(JSON.stringify(props.data)))
const editedRangeDateMap = ref({})
const draftRangeDateMap = ref({})
let isUpdating = false

function getLevelKey(levelValue = selectedLevel.value) {
  return levels.find((level) => level.value === levelValue)?.key
}

function hasOwnValue(obj, key) {
  return Object.prototype.hasOwnProperty.call(obj, key)
}

function getRangeDateForDay(dayData, index) {
  if (hasOwnValue(draftRangeDateMap.value, index)) {
    return draftRangeDateMap.value[index]
  }

  const start = dayData.validStartDate ?? dayData.publish_date
  const end = dayData.validEndDate ?? dayData.valid_date
  return start || end ? [start, end] : []
}

function syncCurrentFormToLocalData(index = Number(activeIndex.value), levelValue = selectedLevel.value) {
  if (isUpdating || !localData.value.data) return

  const weatherData = localData.value.data[index]
  const levelKey = getLevelKey(levelValue)
  if (!weatherData || !levelKey) return

  weatherData.publishDate = formData.value.publish_date

  const rangeDate = Array.isArray(formData.value.range_date) ? [...formData.value.range_date] : []
  draftRangeDateMap.value = {
    ...draftRangeDateMap.value,
    [index]: rangeDate,
  }

  if (!getWarningRangeError(rangeDate)) {
    weatherData.validStartDate = rangeDate[0]
    weatherData.validEndDate = rangeDate[1]
    delete weatherData.valid_date
    editedRangeDateMap.value = {
      ...editedRangeDateMap.value,
      [index]: true,
    }
  } else if (!rangeDate.length || (!rangeDate[0] && !rangeDate[1])) {
    editedRangeDateMap.value = {
      ...editedRangeDateMap.value,
      [index]: false,
    }
  }

  delete weatherData.publish_date

  if (!weatherData.data) {
    weatherData.data = {}
  }
  if (!weatherData.data[levelKey]) {
    weatherData.data[levelKey] = { streets: [] }
  }
  weatherData.data[levelKey].key_tips = formData.value.tips

  // Select 只承载字典内区域，字典外区域从 extAreaList 补回原始 streets
  const areaArray = formData.value.area || []
  weatherData.data[levelKey].streets = Array.from(new Set([...areaArray, ...(extAreaList.value || [])]))
}

const tabWeatherData = computed(() =>
  props.weatherData.map((item, index) => {
    const validStartDate = localData.value.data?.[index]?.validStartDate
    const showTime =
      editedRangeDateMap.value[index] && validStartDate && dayjs(validStartDate).isValid()
        ? dayjs(validStartDate).format('MM月DD日')
        : item.showTime

    return {
      ...item,
      showTime,
    }
  }),
)

// 核心方法：根据当前选中的天数（activeIndex）和等级（selectedLevel），更新表单回显数据
const updateFormData = async () => {
  isUpdating = true
  const index = Number(activeIndex.value)
  const weatherData = localData.value.data[index]
  if (!weatherData) {
    isUpdating = false
    return
  }

  // 回填“发布时间”和“预警有效期”范围数组
  const nextFormData = {
    ...formData.value,
    publish_date: weatherData.publishDate ?? weatherData.publish_date,
    range_date: getRangeDateForDay(weatherData, index),
  }

  // 回填“关键提示”，优先取当前等级独立维护的 tips，如果没有则取外层全局的 tips 兜底
  const levelKey = getLevelKey()
  nextFormData.tips = weatherData.data?.[levelKey]?.key_tips ?? localData.value.key_tips
  const streets = weatherData.data?.[levelKey]?.streets || []

  // 字典加载后，表单只展示可选区域；未知区域暂存在 extAreaList，随当前等级一并回写
  if (validAreaNames.value) {
    const isOtherTowns = streets.length === 1 && streets[0] === OTHER_TOWNS
    extAreaList.value = isOtherTowns ? [] : streets.filter((street) => !validAreaNames.value.has(street))

    // “其他乡镇”表示当天未被其他等级命中的剩余区域，只按当天各等级做排除
    if (isOtherTowns) {
      nextFormData.area = getRemainingAreaNames(weatherData)
    } else {
      nextFormData.area = streets.filter((street) => validAreaNames.value.has(street))
    }
  } else {
    nextFormData.area = streets
    extAreaList.value = []
  }

  formData.value = nextFormData
  await nextTick()
  isUpdating = false
}

// 监听表单数据的变化，将用户的修改实时同步写回 localData 本地深拷贝数据源中
watch(
  () => formData.value,
  () => {
    // 如果是由 updateFormData 方法脚本触发的赋值，跳过本次回写避免死循环
    if (isUpdating) return
    syncCurrentFormToLocalData()
  },
  { deep: true },
)

watch(
  () => [activeIndex.value, selectedLevel.value],
  (val) => {
    updateFormData()
  },
  {
    immediate: true,
    deep: true,
  },
)

// 收集某一天所有等级中已明确出现过的有效区域，用于展开“其他乡镇”
function getAppearedAreaNames(dayData = {}) {
  const appearedAreaNames = new Set()
  levels.forEach((level) => {
    const streets = dayData.data?.[level.key]?.streets
    if (!Array.isArray(streets)) return

    streets.forEach((street) => {
      if (street && street !== OTHER_TOWNS && validAreaNames.value?.has(street)) {
        appearedAreaNames.add(street)
      }
    })
  })
  return appearedAreaNames
}

// “其他乡镇”对应全量有效区域减去当天已出现区域
function getRemainingAreaNames(dayData = {}) {
  if (!validAreaNames.value) return []
  const appearedAreaNames = getAppearedAreaNames(dayData)
  return Array.from(validAreaNames.value).filter((areaName) => !appearedAreaNames.has(areaName))
}

// 提交前兜底展开仍保留为占位值的“其他乡镇”，避免把占位文案传给接口
function normalizeWeatherData(dataList = []) {
  const normalizedData = JSON.parse(JSON.stringify(dataList))
  normalizedData.forEach((dayData) => {
    // 提交入参统一使用后端字段，兼容旧数据回填后去掉冗余字段。
    dayData.publishDate = dayData.publishDate ?? dayData.publish_date
    dayData.validStartDate = dayData.validStartDate ?? dayData.publish_date
    dayData.validEndDate = dayData.validEndDate ?? dayData.valid_date
    delete dayData.publish_date
    delete dayData.valid_date

    levels.forEach((level) => {
      const levelData = dayData.data?.[level.key]
      if (!levelData) return

      const streets = levelData.streets || []
      if (streets.length === 1 && streets[0] === OTHER_TOWNS) {
        levelData.streets = getRemainingAreaNames(dayData)
      }
    })
  })
  return normalizedData
}

// 递归提取全量区域树结构中的所有区域名称，存入 Set 集合中以便进行高效（O(1)）匹配验证
function extractAreaNames(list, nameSet = new Set()) {
  if (!list || !Array.isArray(list)) return nameSet
  list.forEach((item) => {
    if (item.name) nameSet.add(item.name)
    if (item.children) extractAreaNames(item.children, nameSet)
  })
  return nameSet
}

function handleCancel() {
  emits('cancel')
}

// 自定义校验器：检查某一天的发布时间、有效期等跨页数据是否完整
function validateDayData(index) {
  const dayData = localData.value.data[index]
  const dayLabel = props.weatherData[index]?.showTime || `第${index + 1}天`

  if (!formData.value.type) {
    ElMessage.warning(`请填写 ${dayLabel} 的预警信息来源`)
    return false
  }
  if (!dayData.publishDate && !dayData.publish_date) {
    ElMessage.warning(`请填写 ${dayLabel} 的发布时间`)
    return false
  }
  const warningRange = getRangeDateForDay(dayData, index)
  if ((!warningRange[0] || !warningRange[1]) && !dayData.valid_date) {
    ElMessage.warning(`请填写 ${dayLabel} 的预警有效期`)
    return false
  }
  const warningRangeError = getWarningRangeError(warningRange)
  if (warningRangeError) {
    ElMessage.warning(`${dayLabel} 的${warningRangeError}`)
    return false
  }

  return true
}

function handlePrev() {
  const current = Number(activeIndex.value)
  if (current > 0) {
    syncCurrentFormToLocalData(current, selectedLevel.value)
    activeIndex.value = current - 1
  }
}

async function handleNext() {
  // 1. 触发 ElementPlus 的视图层基础校验
  const isValid = await searchFormRef.value?.validate().catch(() => false)
  if (!isValid) return

  // 2. 触发该天数据的深层逻辑校验
  const current = Number(activeIndex.value)
  if (!validateDayData(current)) return

  if (current < props.weatherData.length - 1) {
    syncCurrentFormToLocalData(current, selectedLevel.value)
    activeIndex.value = current + 1
  }
}

async function handleSubmit() {
  syncCurrentFormToLocalData(Number(activeIndex.value), selectedLevel.value)

  // 1. 提交前确保当前页表单必填通过
  const isValid = await searchFormRef.value?.validate().catch(() => false)
  if (!isValid) return

  // 2. 循环对所有天数做终态校验，避免未切到的页漏校验
  for (let i = 0; i < props.weatherData.length; i++) {
    if (!validateDayData(i)) return
  }

  try {
    await ElMessageBox.confirm('是否确认完成入库？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch (error) {
    return // 用户取消
  }

  // 提交前展开“其他乡镇”等占位区域
  let params = {}
  params.type = props.data.type
  params.token = props.data.token
  params.id = props.extParams?.conversationId || ''
  params.message_id = props.extParams?.messageId || ''
  params.data = normalizeWeatherData(localData.value.data)

  try {
    await receiveWeatherWarning(params)
    ElMessage.success('入库成功')
    emits('submit-success')
    eventBus.emit(EventKey.REFRESH_DEFENSE_MODE_PAGE)
  } catch (err) {
    console.error('数据入库失败:', err)
  }
}

// 首次加载：请求后端返回的全量行政区域信息
const fetchAreaList = async () => {
  try {
    const res = await getAreaList({
      pcode: DEFAULT_AREA_CODE,
    })

    // 1. 提取全量有效的区域名
    validAreaNames.value = extractAreaNames(res)
    areaOptions.value = Array.from(validAreaNames.value).map((name) => ({ label: name, value: name }))
    // 2. 更新表单区域数据的显示
    updateFormData()
  } catch (error) {
    console.error('获取区域全量数据失败:', error)
  }
}
fetchAreaList()
</script>

<style lang="less" scoped>
.modify-box {
  width: 67%;
  min-width: 500px;
  border-radius: 12px;
  border: 1px solid #d4d8dd;
  box-shadow: 0 4px 14px 0 #0000000f;
  padding: 18px 14px;
}

.level-box {
  display: flex;
  gap: 10px;
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

.form-box {
  margin-top: 22px;
  :deep(.el-date-editor .el-range-input) {
    color: #222529;
  }
  :deep(.el-textarea__inner) {
    color: #222529;
  }
  :deep(.el-input.is-disabled .el-input__inner) {
    color: #222529;
    -webkit-text-fill-color: #222529;
  }
}

.action-btns {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;

  :deep(.el-button) {
    border-radius: 8px;
  }
}
</style>
