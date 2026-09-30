<template>
  <div class="signal-wrapper" :class="className">
    <div
      v-for="n in 4"
      :key="n"
      class="signal-bar"
      :class="getClass(n)"
      @click="setLevel(n)"
    ></div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Number,
    default: 4,
  },
  className: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const setLevel = (val) => {
  emit('update:modelValue', val)
}

const getClass = (index) => {
  if (index > props.modelValue) return 'inactive'
  if (props.modelValue <= 1) return 'weak'
  if (props.modelValue <= 3) return 'medium'
  return 'strong'
}
</script>

<style lang="less" scoped>
.signal-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 16px;
  margin-left: 10px;
}

.signal-bar {
  width: 4px;
  border-radius: 2px;
  background: #e5e5e5;
  transition: all 0.3s;
  cursor: pointer;
}

.signal-bar:nth-child(1) {
  height: 40%;
}

.signal-bar:nth-child(2) {
  height: 60%;
}

.signal-bar:nth-child(3) {
  height: 80%;
}

.signal-bar:nth-child(4) {
  height: 100%;
}

.weak {
  background: #ff4d4f;
}

.medium {
  background: #FF922C;
}

.strong {
  background: #01FC94;
}

.inactive {
  background: #e5e5e5;
}
</style>
