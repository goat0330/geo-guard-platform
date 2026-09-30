<template>
  <h2 class="section-title" :class="`is-${level}`">
    <span class="title-with-dot">
      <!-- 一级标题：双层错位方块装饰点 -->
      <i v-if="level === 'main'" class="title-squares">
        <i class="square square-front" />
        <i class="square square-back" />
      </i>
      <!-- 二级标题：圆形装饰点 -->
      <i v-else class="title-dot" />
      {{ title }}<slot />
    </span>
    <slot name="extra" />
  </h2>
</template>

<script setup>
defineOptions({ name: 'PanelSectionTitle' })

defineProps({
  /** 区块标题文案 */
  title: {
    type: String,
    default: '',
  },
  /** 标题层级：main 一级标题（16px 加粗），sub 二级标题（14px Medium） */
  level: {
    type: String,
    default: 'main',
  },
})
</script>

<style lang="less" scoped>
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 12px;
}

.section-title.is-sub {
  margin-bottom: 8px;
}

.title-with-dot {
  display: inline-flex;
  align-items: center;
}

// 一级标题：16px Bold + 双层方块点
.is-main .title-with-dot {
  font-size: 16px;
  font-weight: 800;
  line-height: 20px;
  color: #222527;
}

.title-squares {
  position: relative;
  width: 8px;
  height: 8px;
  margin-right: 9px;
}

.square {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #007bff;
}

.square-front {
  top: 0;
  left: 0;
}

.square-back {
  top: 2px;
  left: 2px;
  opacity: 0.2;
}

// 二级标题：14px Medium + 圆点
.is-sub .title-with-dot {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: #383c41;
}

.title-dot {
  width: 5px;
  height: 5px;
  margin-right: 4px;
  background: #007bff;
  border-radius: 50%;
}
</style>
