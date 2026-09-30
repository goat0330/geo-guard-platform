<template>
  <div
    class="refresh"
    :style="[
      { 'margin-bottom': `${gap}px` }
    ]"
    @click="refresh"
  >
    <img src="@/assets/imgs/chatBox/refresh/refresh.svg?url" />
    <span>{{ title }}</span>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: '换一批'
  },
  gap: {
    type: Number,
    default: 20
  },
  refresh: {
    type: Function,
    default: () => {
    }
  }
})

const refresh = () => {
  props.refresh()
}
</script>

<style scoped lang="less">
.refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  cursor: pointer;
  user-select: none;

  img {
    width: 14px;
    height: 14px;
  }
}
</style>