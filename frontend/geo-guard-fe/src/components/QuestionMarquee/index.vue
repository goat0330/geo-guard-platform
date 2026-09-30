<template>
  <div class="question-marquee-list">
    <div
      v-for="(row, rowIndex) in questionRows"
      :key="rowIndex"
      class="question-marquee"
      :class="rowIndex === 0 ? 'is-left-to-right' : 'is-right-to-left'"
      :style="{ '--duration': `${getMarqueeDuration(row)}s` }"
    >
      <div class="question-track">
        <div v-for="groupIndex in 2" :key="groupIndex" class="question-group">
          <button
            v-for="question in row"
            :key="`${groupIndex}-${question}`"
            class="question-item"
            type="button"
            :title="question"
            @click="emit('select', question)"
          >
            <img src="@/assets/imgs/home/icon-sparkle-active.png" alt="" />
            <span>{{ question }}</span>
            <i class="question-arrow iconfont icon-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  questions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['select'])

const questionRows = computed(() => {
  const middleIndex = Math.ceil(props.questions.length / 2)
  return [props.questions.slice(0, middleIndex), props.questions.slice(middleIndex)].filter(
    (row) => row.length,
  )
})

function getMarqueeDuration(row) {
  return Math.max(row.length * 12, 220)
}
</script>

<style lang="less" scoped>
.question-marquee-list {
  width: 100%;
  margin: 8px -6px -6px;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.question-marquee {
  width: 100%;
  margin: -8px 0;
  padding: 8px 0;
  overflow: hidden;
}

.question-track {
  display: flex;
  width: max-content;
  animation-duration: var(--duration);
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}

.question-marquee:hover .question-track {
  animation-play-state: paused;
}

.is-left-to-right .question-track {
  animation-name: question-marquee-left-to-right;
}

.is-right-to-left .question-track {
  animation-name: question-marquee-right-to-left;
}

.question-group {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-right: 20px;
}

.question-item {
  height: 40px;
  max-width: 280px;
  padding: 0 14px;
  border-radius: 8px;
  background: #ffffff;
  filter: drop-shadow(0 2px 6px rgba(57, 126, 251, 0.16));
  color: #506073;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s, filter 0.2s, background-color 0.2s;

  img {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }

  span:not(.question-arrow) {
    min-width: 0;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: left;
  }

  .question-arrow {
    flex-shrink: 0;
    width: 16px;
    height: 16px;
    color: #506073;
    font-size: 16px;
    line-height: 16px;
  }

  &:hover {
    color: #3561fa;
    background: #f0f4ff;
    filter: drop-shadow(0 2px 6px rgba(57, 126, 251, 0.2));
  }
}

@keyframes question-marquee-right-to-left {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@keyframes question-marquee-left-to-right {
  from { transform: translateX(-50%); }
  to { transform: translateX(0); }
}

@media (prefers-reduced-motion: reduce) {
  .question-track {
    animation-play-state: paused;
  }
}
</style>
