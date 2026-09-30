<template>
  <div v-loading="loading" class="message-list">
    <el-empty v-if="!loading && !messages.length" description="暂无消息" />
    <button
      v-for="item in messages"
      v-else
      :key="item.id"
      type="button"
      class="message-item"
      :class="{ 'is-unread': item.status === 0, 'is-selecting': batchMode }"
      @click="$emit('handle', item)"
    >
      <el-checkbox
        v-if="batchMode"
        class="message-check"
        :model-value="selectedIds.includes(item.id)"
        @click.stop
        @change="$emit('toggle', item.id)"
      />
      <span class="type-icon" :class="item.type">
        <span v-if="item.status === 0" class="unread-dot" />
        {{ getTypeShortName(item.type) }}
      </span>
      <span class="message-main">
        <span class="message-title">{{ item.title || '--' }}</span>
        <span class="message-content">{{ item.content || '--' }}</span>
        <span class="message-date">{{ item.createDate || '--' }}</span>
      </span>
      <span v-if="item.status !== 1 && !batchMode" class="handle-link">立即处理</span>
    </button>
  </div>
</template>

<script setup>
defineOptions({ name: 'MessageCenterList' })

const props = defineProps({
  messages: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
  batchMode: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  typeMap: { type: Object, default: () => ({}) },
})

defineEmits(['handle', 'toggle'])

function getTypeShortName(type) {
  const typeName = props.typeMap[type] || ''
  return typeName.replace(/^会商/, '') || '--'
}
</script>

<style lang="less" scoped>
.message-list {
  /* 仅列表区域滚动，分页栏始终停留在页面底部。 */
  min-height: 0;
  flex: 1 1 auto;
  overflow-y: auto;
}

.message-item {
  display: flex;
  width: 100%;
  min-height: 112px;
  align-items: flex-start;
  padding: 18px 16px;
  border: 0;
  border-bottom: 1px solid #edf1f5;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: #f7faff;
  }
  &.is-unread {
    background: rgba(220, 237, 255, 0.24);
  }
  &.is-unread:hover {
    background: rgba(220, 237, 255, 0.5);
  }
}

.message-check {
  margin: 15px 16px 0 0;
}

.type-icon {
  position: relative;
  display: flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  flex: 0 0 48px;
  margin-right: 16px;
  border-radius: 8px;
  background: #dcedff;
  color: #007bff;
  font-size: 14px;
  font-weight: 500;

  &.meeting-summary {
    background: #fff0e2;
    color: #ff922c;
  }
}

.unread-dot {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #e45b5b;
}

.message-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.message-title {
  margin-bottom: 6px;
  color: #222527;
  font-size: 16px;
  font-weight: 500;
  line-height: 22px;
}
.message-content {
  display: -webkit-box;
  overflow: hidden;
  margin-bottom: 8px;
  color: #617185;
  font-size: 14px;
  line-height: 20px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.message-date {
  color: #a6acb8;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 12px;
  line-height: 18px;
}
.handle-link {
  align-self: center;
  flex: 0 0 auto;
  margin-left: 32px;
  color: #007bff;
  font-size: 14px;
}

@media (max-width: 900px) {
  .message-item {
    padding-right: 10px;
    padding-left: 10px;
  }
  .handle-link {
    margin-left: 12px;
  }
}
</style>
