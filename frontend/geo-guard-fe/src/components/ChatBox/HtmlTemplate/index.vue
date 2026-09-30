<template>
  <div class="html-template-box" :style="{ 'margin-bottom': `${gap}px` }" v-html="tempLateHtml" @click="handleClick($event)"></div>
</template>

<script setup>
import { computed } from 'vue'
import { template } from './template.js'

const props = defineProps({
  templateId: {
    type: Number,
    default: 1
  },
  templateHtml: {
    type: String,
    default: ''
  },
  data: {
    type: Object,
    default: () => ({})
  },
  gap: {
    type: Number,
    default: 20
  },
  handleClick: {
    type: Function,
    default: () => {}
  }
})

const tempLateHtml = computed(() => {
  if (props.templateHtml) return props.templateHtml
  return template[+props.templateId](props?.data) || ''
})
</script>

<style scoped lang="less">
.html-template-box {

}
</style>
<style lang="less">
@import "styles";
</style>