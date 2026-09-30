<template>
  <span ref="numberRef" :aria-label="finalText">{{ displayText }}</span>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useEnterView } from '@/composables/useEnterView.js'

defineOptions({
  name: 'AnimatedNumber',
})

const props = defineProps({
  value: {
    type: [Number, String],
    required: true,
  },
  duration: {
    type: Number,
    default: 1400,
  },
  decimals: {
    type: Number,
    default: undefined,
  },
  prefix: {
    type: String,
    default: '',
  },
  suffix: {
    type: String,
    default: '',
  },
  useGrouping: {
    type: Boolean,
    default: true,
  },
})

const { targetRef: numberRef, hasEntered } = useEnterView({ threshold: 0.4 })
const displayValue = ref(0)
let animationFrameId = 0

const numericValue = computed(() => {
  const normalizedValue =
    typeof props.value === 'string' ? props.value.replace(/,/g, '').replace(/%/g, '') : props.value
  const parsedValue = Number(normalizedValue)
  return Number.isFinite(parsedValue) ? parsedValue : null
})

const precision = computed(() => {
  if (Number.isInteger(props.decimals) && props.decimals >= 0) return props.decimals
  const rawValue = String(props.value).replace(/,/g, '').replace(/%/g, '')
  return rawValue.includes('.') ? rawValue.split('.')[1].length : 0
})

function formatValue(value) {
  if (numericValue.value === null) return ''

  return Number(value).toLocaleString('zh-CN', {
    useGrouping: props.useGrouping,
    minimumFractionDigits: precision.value,
    maximumFractionDigits: precision.value,
  })
}

const fallbackText = computed(() => String(props.value ?? ''))
const displayText = computed(() =>
  numericValue.value === null ? fallbackText.value : `${props.prefix}${formatValue(displayValue.value)}${props.suffix}`,
)
const finalText = computed(() =>
  numericValue.value === null ? fallbackText.value : `${props.prefix}${formatValue(numericValue.value)}${props.suffix}`,
)

function startAnimation() {
  cancelAnimationFrame(animationFrameId)

  if (numericValue.value === null) {
    displayValue.value = 0
    return
  }

  const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  if (reduceMotion || props.duration <= 0) {
    displayValue.value = numericValue.value
    return
  }

  const startValue = displayValue.value
  const targetValue = numericValue.value
  const startTime = performance.now()

  // easeOutCubic 让数字前快后慢，结束时更自然。
  const updateValue = (currentTime) => {
    const progress = Math.min((currentTime - startTime) / props.duration, 1)
    const easedProgress = 1 - Math.pow(1 - progress, 3)
    displayValue.value = startValue + (targetValue - startValue) * easedProgress

    if (progress < 1) {
      animationFrameId = requestAnimationFrame(updateValue)
    } else {
      displayValue.value = targetValue
    }
  }

  animationFrameId = requestAnimationFrame(updateValue)
}

/* 进入视口播放（首页看板行为） */
watch(
  hasEntered,
  (entered) => {
    if (entered) startAnimation()
  },
  { immediate: true },
)

/* 数据变化时兜底播放：在滚动容器内视口回调可能不触发，
   此时数值从 0 变为真实值时也要滚动出来，否则会一直停在 0 */
watch(numericValue, () => {
  if (hasEntered.value || numericValue.value) startAnimation()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationFrameId)
})
</script>

<style lang="less" scoped>
span {
  font-variant-numeric: tabular-nums;
}
</style>
