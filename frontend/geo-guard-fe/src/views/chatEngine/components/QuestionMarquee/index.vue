<template>
  <div v-if="questionRows.length" class="question-marquee-list">
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
            <i class="iconfont icon-start"></i>
            <span>{{ question }}</span>
            <i class="iconfont icon-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { quesPool } from '@/utils/ques_pool'

const emit = defineEmits(['select'])

function splitQuestionRows(questions) {
  const middleIndex = Math.ceil(questions.length / 2)
  return [questions.slice(0, middleIndex), questions.slice(middleIndex)].filter((row) => row.length)
}

function getMarqueeDuration(row) {
  return Math.max(row.length * 12, 220)
}

const questionRows = computed(() => splitQuestionRows(quesPool))
</script>

<style lang="less" scoped>
.question-marquee-list {
  width: 926px;
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
  padding-right: 12px;
}

.question-item {
  height: 40px;
  max-width: 280px;
  padding: 0 14px;
  border-radius: 8px;
  background: #fff;
  filter: drop-shadow(0 2px 6px #397efb29);
  color: #506073;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition:
    color 0.2s,
    border-color 0.2s,
    box-shadow 0.2s,
    filter 0.2s,
    background-color 0.2s;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  i {
    flex-shrink: 0;
    color: #3561fa;
    font-size: 14px;
  }

  .icon-arrow-right {
    color: #506073;
  }

  &:hover {
    border-color: #d6e4ff;
    background: #f0f4ff;
    box-shadow: none;
    border: none;
    filter: drop-shadow(0 2px 6px #397efb33);
  }
}

@keyframes question-marquee-right-to-left {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@keyframes question-marquee-left-to-right {
  from {
    transform: translateX(-50%);
  }
  to {
    transform: translateX(0);
  }
}
</style>
