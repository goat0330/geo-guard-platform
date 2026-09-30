<template>
  <div class="loading" :class="className" :style="{ width: size + 'px', height: size + 'px' }">
    <span v-for="n in 8" :key="n"
          :style="getSpanStyle(n)"></span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 接收 size 和 color
const props = defineProps({
  size: { type: Number, default: 20 }, // 宽高
  colorStart: { type: String, default: '#494e4b' },
  colorMid: { type: String, default: '#7c7e7c' },
  colorEnd: { type: String, default: '#fff' },
  className: { type: String, default: '' }
})

// 计算每根 span 的动画延迟和旋转角度
const getSpanStyle = (index) => {
  const angle = (index - 1) * 45
  const delay = ((index - 1) / 8) + 's'
  return {
    transform: `rotate(${angle}deg)`,
    animationDelay: delay,
    width: (props.size / 12) + 'px',
    height: (props.size / 3.3) + 'px',
    marginLeft: -(props.size / 12) + 'px',
    marginTop: -(props.size / 1.0) + 'px',
    transformOrigin: `center ${props.size / 2}px`,
    '--color-start': props.colorStart,
    '--color-mid': props.colorMid,
    '--color-end': props.colorEnd,
  }
}
</script>

<style scoped lang="less">
.loading {
  position: relative;
}

.loading span {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 3px;
  animation: fade 1s linear infinite;
}

/* 使用 CSS 变量动态颜色 */
@keyframes fade {
  0% { background: var(--color-start, #494e4b); }
  50% { background: var(--color-mid, #7c7e7c); }
  100% { background: var(--color-end, #fff); }
}
</style>