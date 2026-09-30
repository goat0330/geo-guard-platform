<template>
  <h2 class="section-title">
    <span class="title-with-dot"><img :src="dotIcon" alt="" />{{ title }}</span>
    <slot name="extra" />
  </h2>
</template>

<script setup>
import dotIcon from '@/assets/imgs/hazardReview/icon-header-dot.webp'

defineOptions({ name: 'PanelSectionTitle' })

defineProps({
  /** 区块标题文案 */
  title: {
    type: String,
    default: '',
  },
})
</script>

<style lang="less" scoped>
.section-title {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 800;
  line-height: 22px;
}

.title-with-dot {
  display: inline-flex;
  align-items: center;
  font-size: 16px;
  font-weight: 800;

  img {
    width: 8px;
    height: 8px;
    margin-right: 9px;
    object-fit: contain;
  }
}
</style>
