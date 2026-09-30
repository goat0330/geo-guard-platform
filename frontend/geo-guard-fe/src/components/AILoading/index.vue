<template>
  <div class="loading-progress">
    <div
      v-for="(step, key) in steps"
      :key="step.value"
      class="each-step"
      :class="activeIndex >= key ? 'is-active' : ''"
    >
      <span v-if="key == activeIndex + 1" class="loader"></span>
      <i v-else class="iconfont icon-check" />
      {{ step.label }}
      <div v-if="key < steps.length - 1" class="spliter"></div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeMount, onBeforeUnmount, onMounted, ref } from 'vue'
const activeIndex = ref(-1)

const DURATION = 2

const steps = ref([
  { label: '安全校验', value: 'securityCheck' },
  { label: '提问分析', value: 'questionAnalysis' },
  { label: '知识检索', value: 'knowledgeRetrieval' },
  { label: '知识阅读', value: 'knowledgeReading' },
  { label: '准备输出', value: 'prepareOutput' },
])

let timer = null
onMounted(() => {
  timer = setInterval(() => {
    if (activeIndex.value < steps.value.length - 1) {
      activeIndex.value++
    } else {
      clearInterval(timer)
    }
  }, 600)
})

function setAnimateEnd() {
  clearInterval(timer)
  activeIndex.value = steps.value.length - 1
}

onBeforeUnmount(() => {
  clearInterval(timer)
})

defineExpose({
  setAnimateEnd,
})
</script>

<style lang="less" scoped>
.loading-progress {
  display: flex;
  color: #878898;
  align-items: center;
  font-size: 14px;
  .each-step {
    display: flex;
    align-items: center;
    font-size: 14px;
    i {
      margin-right: 5px;
      font-size: 24px;
    }
    .spliter {
      width: 60px;
      height: 3px;
      background-color: #878898;
      border-radius: 2px;
      margin: 0 8px;
      overflow: hidden;
      &__inner {
        width: 0;
        height: 100%;
        background-color: var(--el-color-primary);
        transition: width 2s linear;
      }
    }

    &.is-active {
      color: #222529;
      font-weight: bold;
      color: var(--el-color-primary);
      i {
        color: var(--el-color-primary);
      }
      .spliter {
        background-color: var(--el-color-primary);
      }
    }
  }
}

.loader {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-block;
  border-top: 3px solid rgba(53, 97, 250, 0.8);
  border-right: 3px solid transparent;
  box-sizing: border-box;
  animation: rotation 1s linear infinite;
  margin-left: 2px;
  margin-right: 8px;
}

@keyframes rotation {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
