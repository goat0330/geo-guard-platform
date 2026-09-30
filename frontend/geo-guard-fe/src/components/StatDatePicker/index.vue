<template>
  <div class="date-picker-box">
    <!-- 展示层：蓝色日历图标 + 日期 + 下拉箭头，点击整体唤起面板 -->
    <button type="button" title="统计日期" @click="datePickerRef?.handleOpen()">
      <el-icon><Calendar /></el-icon>
      <span class="date-text">{{ modelValue }}</span>
      <el-icon class="date-arrow"><ArrowDown /></el-icon>
    </button>

    <!-- 真实日期面板：隐藏到 1px，仅用于弹出日历 -->
    <el-date-picker
      ref="datePickerRef"
      :model-value="modelValue"
      class="date-picker-host"
      type="date"
      value-format="YYYY-MM-DD"
      :clearable="false"
      aria-label="统计日期"
      @update:model-value="handleUpdate"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  /** 当前选中日期，格式 YYYY-MM-DD */
  modelValue: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const datePickerRef = ref(null)

const handleUpdate = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style lang="less" scoped>
.date-picker-box {
  position: relative;
  flex-shrink: 0;
}

/* 与群测群防概览的时间筛选保持一致：主色文字 + 小号下拉箭头 */
.date-picker-box button {
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

.date-picker-box button .date-arrow {
  font-size: 10px;
}

.date-text {
  font-family: 'Alimama FangYuanTi VF', AlibabaPuHuiTi, sans-serif;
  font-style: normal;
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
