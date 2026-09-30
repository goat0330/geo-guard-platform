<template>
  <section v-if="items.length" class="recognition-result" aria-label="AI识图结果">
    <p v-for="item in items" :key="item.label" class="recognition-result__item">
      <span>{{ item.label }}：</span>
      <span :class="{ 'is-high-risk': item.label === '风险程度' && item.value === '高' }">
        {{ item.value }}
      </span>
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'ImageRecognitionResult' })

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})

const FIELD_ORDER = [
  '图像类型',
  '灾害类型',
  '变形迹象',
  '承灾体类型',
  '承载体类型',
  '新鲜程度',
  '地灾阶段',
  '风险程度',
  'AI标签',
]

const items = computed(() => {
  const entries = Object.entries(props.data).filter(
    ([, value]) => value !== null && value !== undefined && value !== '',
  )
  const fieldIndex = new Map(FIELD_ORDER.map((field, index) => [field, index]))

  return entries
    .sort(
      ([left], [right]) => (fieldIndex.get(left) ?? FIELD_ORDER.length) - (fieldIndex.get(right) ?? FIELD_ORDER.length),
    )
    .map(([label, value]) => ({
      label,
      value: typeof value === 'object' ? JSON.stringify(value) : String(value),
    }))
})
</script>

<style scoped lang="less">
.recognition-result {
  margin: 0 0 20px;
  color: #222527;
}

.recognition-result__item {
  margin: 0 0 8px;
  font-size: 16px;
  line-height: 24px;
  overflow-wrap: anywhere;
}

.recognition-result__item:last-child {
  margin-bottom: 0;
}

.is-high-risk {
  color: #e45b5b;
  font-weight: 600;
}
</style>
