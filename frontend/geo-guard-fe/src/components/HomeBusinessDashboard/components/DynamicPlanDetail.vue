<template>
  <div class="dynamic-plan-detail">
    <div class="section-header">
      <span class="section-bullet"></span>
      <h3 class="section-title">动态预案详情</h3>
    </div>

    <div class="plan-cards">
      <!-- 动态预案累计更新 -->
      <article class="plan-card">
        <img src="@/assets/imgs/home/icon-plan-update.png" class="plan-icon" alt="" />
        <div class="plan-content">
          <span class="plan-label">动态预案累计更新</span>
          <strong class="plan-num">
            <AnimatedNumber :value="props.data?.dynamicPlanUpdatedCount ?? '--'" />
          </strong>
        </div>
      </article>

      <!-- 动态预案累计生成 -->
      <article class="plan-card">
        <img src="@/assets/imgs/home/icon-plan-generate.png" class="plan-icon" alt="" />
        <div class="plan-content">
          <span class="plan-label">动态预案累计生成</span>
          <strong class="plan-num">
            <AnimatedNumber :value="props.data?.dynamicPlanGeneratedCount ?? '--'" />
          </strong>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'

defineOptions({
  name: 'DynamicPlanDetail',
})

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})
</script>

<style lang="less" scoped>
.dynamic-plan-detail {
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

.plan-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.plan-card {
  box-sizing: border-box;
  background: #f9fbfe;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 96px;
}

.plan-icon {
  width: 48px;
  height: 48px;
  object-fit: contain;
  flex-shrink: 0;
}

.plan-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plan-label {
  color: #617185;
  font-size: 14px;
  line-height: 16px;
}

.plan-num {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 28px;
  font-style: normal;
  font-weight: Bold-Square;
  line-height: 32px;
}

@media (max-width: 768px) {
  .plan-cards {
    grid-template-columns: 1fr;
  }
}
</style>
