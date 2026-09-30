<template>
  <div
    class="map-splitter"
    :class="{ 'is-dragging': isDragging, 'is-collapsed': isCollapsed }"
    title="上下拖动调节高度"
    @mousedown="$emit('drag-start', $event)"
  >
    <!-- 分割线背景条 -->
    <div class="splitter-line"></div>

    <!-- 居中展开/收起控制胶囊按钮 -->
    <button
      type="button"
      class="splitter-toggle-btn"
      :title="isCollapsed ? '展开地图' : '收起地图'"
      :aria-label="isCollapsed ? '展开地图' : '收起地图'"
      @mousedown.stop
      @click.stop="$emit('toggle')"
    >
      <i
        class="iconfont icon-arrow-down arrow-icon"
        :class="{ 'is-reversed': !isCollapsed }"
      ></i>
      <span class="btn-text">{{ isCollapsed ? '展开地图' : '收起地图' }}</span>
    </button>
  </div>
</template>

<script setup>
defineOptions({ name: 'MapSplitter' })

defineProps({
  isCollapsed: {
    type: Boolean,
    default: false,
  },
  isDragging: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['drag-start', 'toggle'])
</script>

<style lang="less" scoped>
.map-splitter {
  position: relative;
  height: 18px;
  margin: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: row-resize;
  user-select: none;
  z-index: 10;
  transition: opacity 0.2s ease;

  &:hover,
  &.is-dragging {
    .splitter-line {
      background: #007bff;
      opacity: 0.8;
      height: 2px;
    }

    .splitter-toggle-btn {
      border-color: #007bff;
      color: #007bff;
      box-shadow: 0 2px 8px rgba(0, 123, 255, 0.2);
    }
  }

  &.is-collapsed {
    margin-top: 4px;
  }
}

.splitter-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: #dee2ec;
  transition: background-color 0.2s ease, height 0.2s ease, opacity 0.2s ease;
}

.splitter-toggle-btn {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 22px;
  padding: 0 12px;
  border: 1px solid #dee2ec;
  border-radius: 11px;
  background: #ffffff;
  color: #617185;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 1px 4px rgba(34, 37, 39, 0.08);

  .arrow-icon {
    font-size: 10px;
    line-height: 1;
    transition: transform 0.24s cubic-bezier(0.4, 0, 0.2, 1);
    transform: rotate(0deg);

    &.is-reversed {
      transform: rotate(180deg);
    }
  }

  .btn-text {
    font-size: 12px;
  }

  &:hover {
    color: #007bff;
    border-color: #007bff;
    background: #e0eefa;
  }
}
</style>
