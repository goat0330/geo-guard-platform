<template>
  <div ref="sectionRef" class="review-task-detail">
    <div class="section-header">
      <span class="section-bullet"></span>
      <h3 class="section-title">复盘任务详情</h3>
    </div>

    <div class="task-card">
      <!-- 左侧：累计生成 -->
      <div class="task-summary">
        <img src="@/assets/imgs/home/icon-review-generate.png" class="task-icon" alt="" />
        <div class="task-content">
          <span class="task-label">复盘任务累计生成</span>
          <strong class="task-num">
            <AnimatedNumber :value="props.data?.reviewTaskGeneratedCount ?? '--'" />
          </strong>
        </div>
      </div>

      <!-- 右侧：灾情 / 险情进度条 -->
      <div class="task-bars">
        <div class="bar-row">
          <span class="bar-label">灾情</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: progressDisasterWidth }"></div>
          </div>
          <strong class="bar-value">
            <AnimatedNumber :value="props.data?.reviewDisasterCount ?? '--'" />
          </strong>
        </div>

        <div class="bar-row">
          <span class="bar-label">险情</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: progressDangerWidth }"></div>
          </div>
          <strong class="bar-value">
            <AnimatedNumber :value="props.data?.reviewDangerCount ?? '--'" />
          </strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useEnterView } from '@/composables/useEnterView.js'

defineOptions({
  name: 'ReviewTaskDetail',
})

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})

const { targetRef: sectionRef, hasEntered: sectionHasEntered } = useEnterView()

const progressDisasterWidth = computed(() => {
  if (!sectionHasEntered.value || props.data?.reviewDisasterCount == null) return '0%'
  const total = Number(props.data?.reviewTaskGeneratedCount) || 0
  const count = Number(props.data?.reviewDisasterCount) || 0
  if (total <= 0) return '0%'
  return `${Math.min(100, Math.round((count / total) * 100))}%`
})

const progressDangerWidth = computed(() => {
  if (!sectionHasEntered.value || props.data?.reviewDangerCount == null) return '0%'
  const total = Number(props.data?.reviewTaskGeneratedCount) || 0
  const count = Number(props.data?.reviewDangerCount) || 0
  if (total <= 0) return '0%'
  return `${Math.min(100, Math.round((count / total) * 100))}%`
})
</script>

<style lang="less" scoped>
.review-task-detail {
  width: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.section-bullet {
  width: 8px;
  height: 8px;
  background: url('@/assets/imgs/point.png') center / contain no-repeat;
  flex-shrink: 0;
}

.section-title {
  margin: 0;
  color: #383c41;
  font-size: 16px;
  font-weight: 600;
  line-height: 20px;
}

.task-card {
  box-sizing: border-box;
  background: #f9fbfe;
  border-radius: 10px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  min-height: 96px;
}

.task-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.task-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.task-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-label {
  color: #617185;
  font-size: 14px;
  line-height: 16px;
}

.task-num {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 28px;
  font-style: normal;
  font-weight: Bold-Square;
  line-height: 32px;
}

.task-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 32px 1fr 40px;
  align-items: center;
  gap: 12px;
}

.bar-label {
  color: #617185;
  font-size: 14px;
  line-height: 16px;
}

.bar-track {
  height: 6px;
  background: #e0eefa;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #007bff 0%, #3595fb 100%);
  transition: width 1.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.bar-value {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: Bold-Square;
  text-align: right;
  line-height: 16px;
}

@media (max-width: 768px) {
  .task-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
