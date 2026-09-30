<template>
  <!-- 字段图标：设计稿导出件（Frame 16×16，#617185），按字段类型映射 -->
  <img class="field-icon" :src="iconMap[type] || iconMap.location" alt="" />
</template>

<script setup>
import iconFieldLocation from '@/assets/imgs/emergency/icon-field-location.svg'
import iconFieldHazard from '@/assets/imgs/emergency/icon-field-hazard.svg'
import iconFieldSource from '@/assets/imgs/emergency/icon-field-source.svg'
import iconFieldTime from '@/assets/imgs/emergency/icon-field-time.svg'

defineOptions({ name: 'ParamFieldIcon' })

defineProps({
  /** 字段类型：location 位置 / hazard 隐患类型 / source 触发来源 / time 更新时间 */
  type: {
    type: String,
    default: 'location',
  },
})

/** 字段类型 → 图标切图 */
const iconMap = {
  location: iconFieldLocation,
  hazard: iconFieldHazard,
  source: iconFieldSource,
  time: iconFieldTime,
}
</script>

<style lang="less" scoped>
/* 图标用设计稿的 16×16 Frame 容器（内含留白），与文字间距由父级 gap 控制 */
.field-icon {
  flex-shrink: 0;
  display: block;
  width: 16px;
  height: 16px;
}
</style>
