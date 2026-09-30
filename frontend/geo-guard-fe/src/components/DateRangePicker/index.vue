<template>
  <div class="date-range-picker">
    <!-- 展示层：蓝色日历图标 + 区间文案 + 下拉箭头，点击整体唤起面板 -->
    <button class="range-trigger" type="button" title="统计时段" @click="openPicker">
      <el-icon><Calendar /></el-icon>
      <span class="range-text">{{ displayText }}</span>
      <el-icon class="range-arrow"><ArrowDown /></el-icon>
    </button>

    <!-- 重置：清空区间回到「全部」并立即查询一次 -->
    <button class="reset-button" type="button" title="重置为全部" @click="handleReset">重置</button>

    <!-- 真实日期面板：隐藏到 1px，仅用于弹出区间日历 -->
    <el-date-picker
      ref="datePickerRef"
      :model-value="pickerValue"
      class="date-picker-host"
      type="daterange"
      value-format="YYYY-MM-DD"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      range-separator="至"
      :clearable="false"
      aria-label="统计时段"
      @update:model-value="handleUpdate"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { formatRangeText, isAllRange } from '@/utils/dateRange.js'

defineOptions({ name: 'DateRangePicker' })

const props = defineProps({
  /** 当前选中区间 [开始日期, 结束日期]，格式 YYYY-MM-DD；空数组表示全部 */
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const datePickerRef = ref(null)

/** 展示文案：全部 /「开始 至 结束」 */
const displayText = computed(() => formatRangeText(props.modelValue))

/** 面板值：全部时传 null，避免区间面板把空数组当成无效值 */
const pickerValue = computed(() => (isAllRange(props.modelValue) ? null : [...props.modelValue]))

const openPicker = () => {
  datePickerRef.value?.handleOpen()
}

/** 选中区间：同步 v-model 并通知外层按新区间查询 */
const handleUpdate = (value) => {
  const range = Array.isArray(value) ? value.filter(Boolean) : []
  emit('update:modelValue', range)
  emit('change', range)
}

/** 重置：切回「全部」并触发一次查询 */
const handleReset = () => {
  if (isAllRange(props.modelValue)) {
    /* 已是全部时也要重新查询，保证按钮始终有反馈 */
    emit('change', [])
    return
  }
  emit('update:modelValue', [])
  emit('change', [])
}
</script>

<style lang="less" scoped>
.date-range-picker {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

/* 与群测群防概览的时间筛选保持一致：主色文字 + 小号下拉箭头 */
.range-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #007bff;
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.range-trigger .range-arrow {
  font-size: 10px;
}

.range-text {
  font-family: 'Alimama FangYuanTi VF', AlibabaPuHuiTi, sans-serif;
  font-style: normal;
}

/* 重置：与列表筛选的重置按钮同款（白底描边 + 灰字，hover 转主色） */
.reset-button {
  height: 24px;
  padding: 0 8px;
  border: 1px solid #dcedff;
  border-radius: 4px;
  background: #ffffff;
  color: #617185;
  font-family: inherit;
  font-size: 12px;
  line-height: 22px;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.reset-button:hover {
  border-color: #007bff;
  color: #007bff;
}

/* 隐藏 Element Plus 输入框本体，仅保留弹出的日期面板。
   注意：el-date-picker 属于子组件，必须用 :deep() 才能命中其根节点 */
:deep(.date-picker-host) {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 1px !important;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}
</style>
