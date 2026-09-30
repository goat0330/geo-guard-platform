<template>
  <div
    class="voice-change"
    :class="{ 'is-dragging': isDragging }"
    @click.stop
    @pointerdown.stop
  >
    <div class="voice-panel">
      <div
        ref="trackRef"
        class="voice-track-wrap"
        @pointerdown="handlePointerDown"
      >
        <div class="voice-track">
          <div class="voice-track-active" :style="{ height: `${currentValue}%` }"></div>
          <div class="voice-thumb" :style="{ bottom: `${currentValue}%` }"></div>
        </div>
      </div>
      <div class="voice-value">{{ currentValue }}%</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Number,
    default: 45,
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const trackRef = ref(null)
const innerValue = ref(normalizeValue(props.modelValue))
const isDragging = ref(false)

const currentValue = computed(() => normalizeValue(innerValue.value))

watch(
  () => props.modelValue,
  (val) => {
    if (!isDragging.value) {
      innerValue.value = normalizeValue(val)
    }
  },
)

function normalizeValue(value) {
  if (!Number.isFinite(value)) return 0
  return Math.min(100, Math.max(0, Math.round(value)))
}

function updateValue(clientY) {
  if (!trackRef.value) return

  const rect = trackRef.value.getBoundingClientRect()
  const nextValue = ((rect.bottom - clientY) / rect.height) * 100
  const normalizedValue = normalizeValue(nextValue)

  if (normalizedValue === innerValue.value) return

  innerValue.value = normalizedValue
  emit('update:modelValue', normalizedValue)
  emit('change', normalizedValue)
}

function handlePointerDown(event) {
  isDragging.value = true
  updateValue(event.clientY)
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
  window.addEventListener('pointercancel', handlePointerUp)
}

function handlePointerMove(event) {
  updateValue(event.clientY)
}

function handlePointerUp() {
  isDragging.value = false
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  window.removeEventListener('pointercancel', handlePointerUp)
}

onBeforeUnmount(() => {
  handlePointerUp()
})
</script>

<style scoped lang="less">
.voice-change {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 14px);
  z-index: 10;
  pointer-events: auto;
  transform: translateX(-50%) translateY(6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.voice-panel {
  width: 48px;
  padding: 20px 0 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-radius: 10px;
  cursor: default;
  user-select: none;
  box-shadow: 0 0 4px 0 rgba(0, 0, 0, 0.1), 0 4px 10px 0 rgba(0, 0, 0, 0.1);
}

.voice-track-wrap {
  position: relative;
  width: 28px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  touch-action: none;
}

.voice-track {
  position: relative;
  width: 6px;
  height: 88px;
  border-radius: 10px;
  background: #d9d9d9;
}

.voice-track-active {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  border-radius: 999px;
  background: #007BFF;
}

.voice-thumb {
  position: absolute;
  left: 50%;
  width: 10px;
  height: 10px;
  border: 1px solid #007BFF;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.35);
  transform: translate(-50%, 50%);
}

.voice-value {
  margin-top: 10px;
  font-size: 12px;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  color: #617185;
}
</style>
