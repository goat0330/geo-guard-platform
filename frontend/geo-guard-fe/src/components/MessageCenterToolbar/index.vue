<template>
  <div class="message-toolbar">
    <div class="status-segment" role="tablist" aria-label="消息状态">
      <button
        v-for="item in statusOptions"
        :key="String(item.value)"
        type="button"
        :class="{ 'is-active': status === item.value }"
        @click="$emit('update:status', item.value)"
      >
        {{ item.label }}<span>{{ item.count }}</span>
      </button>
    </div>

    <div class="filter-group">
      <el-date-picker
        :model-value="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        clearable
        @update:model-value="$emit('update:dateRange', $event || [])"
      />
      <el-select
        :model-value="type"
        class="type-select"
        placeholder="全部类型"
        @update:model-value="$emit('update:type', $event)"
      >
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'MessageCenterToolbar' })

const props = defineProps({
  status: { type: [String, Number], default: '' },
  dateRange: { type: Array, default: () => [] },
  type: { type: String, default: '' },
  counts: {
    type: Object,
    default: () => ({ totalCount: 0, readCount: 0, unreadCount: 0 }),
  },
  typeOptions: { type: Array, default: () => [] },
})

defineEmits(['update:status', 'update:dateRange', 'update:type'])

const statusOptions = computed(() => [
  { label: '全部', value: '', count: props.counts.totalCount || 0 },
  { label: '已读', value: 1, count: props.counts.readCount || 0 },
  { label: '未读', value: 0, count: props.counts.unreadCount || 0 },
])
</script>

<style lang="less" scoped>
.message-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.status-segment {
  display: flex;
  height: 40px;
  padding: 4px;
  border-radius: 8px;
  background: #edf1f5;

  button {
    min-width: 88px;
    height: 32px;
    padding: 0 14px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #617185;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s ease, color 0.2s ease;

    span { margin-left: 4px; }
    &:hover { color: #007bff; }
    &.is-active { background: #fff; color: #222527; font-weight: 500; box-shadow: 0 1px 4px rgba(0, 32, 80, 0.08); }
  }
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;

  :deep(.el-date-editor) { width: 280px; }
  .type-select { width: 138px; }
}

@media (max-width: 1100px) {
  .message-toolbar { align-items: flex-start; flex-direction: column; }
}
</style>
