<template>
  <div class="chart-title">
    <div class="left"><img src="@/assets/imgs/title-pre.png" alt="" /> {{ title }}</div>

    <div class="right"><slot /></div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: '',
  },
})
</script>

<style lang="less" scoped>
.chart-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  .left {
    display: flex;
    align-items: center;
    gap: 9px;
    font-size: 16px;
    line-height: 24px;
    color: #222529;
    font-weight: 800;
    img {
      height: 8px;
      width: 8px;
    }
  }
  .right {
    flex: 1;
    min-width: 0;
  }
}
</style>
