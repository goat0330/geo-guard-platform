<template>
  <span class="status-tag" :style="{ color: config.color, background: config.background }">
    <i class="status-dot" :style="{ background: config.color }"></i>{{ status }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { processStatusMap } from '../../config.js'

const props = defineProps({
  /** 处理状态：待复核 / 已复核 / 待入库 / 已入库 */
  status: {
    type: String,
    default: '',
  },
})

/** 未在字典中的状态使用中性配色兜底 */
const fallbackConfig = { color: '#617185', background: '#f1f4f8' }

const config = computed(() => processStatusMap[props.status] || fallbackConfig)
</script>

<style lang="less" scoped>
.status-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
</style>
