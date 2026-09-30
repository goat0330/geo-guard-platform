<template>
  <div class="layout-wrapper">
    <!-- 左侧侧边栏 -->
    <Aside />

    <!-- 右侧主体内容容器 -->
    <main class="layout-main" :class="{ 'is-fixed-page': isMapWorkspace }">
      <div class="page-container" :class="{ 'is-fixed-page': isMapWorkspace }">
        <RouterView />
      </div>
      <!-- 会商入口由布局统一维护，全页面（含首页）统一提供入口 -->
      <ConsultationEntry />
    </main>
  </div>
</template>

<script setup>
import Aside from './aside.vue'
import { RouterView, useRoute } from 'vue-router'
import { computed, onMounted, onUnmounted } from 'vue'
import ConsultationEntry from '@/components/ConsultationEntry/index.vue'
import { useUserStore } from '@/store/user.js'
import { scheduleTodayRiskAssessmentPrefetch } from '@/api/riskEvaluation.js'

defineOptions({ name: 'AppLayout' })

const route = useRoute()
const userStore = useUserStore()
const isLocalAiStudio = import.meta.env.DEV && route.name === 'GeoAiStudio'
let cancelRiskAssessmentPrefetch

// 确保布局容器挂载时，全局 SSE 与心跳持续运行（内部有单例防重）
onMounted(() => {
  if (isLocalAiStudio) return
  userStore.startConnection()
  userStore.startHeartbeat()
  // 让出首屏渲染后再预取当天全量风险评价数据。
  cancelRiskAssessmentPrefetch = scheduleTodayRiskAssessmentPrefetch()
})

onUnmounted(() => {
  cancelRiskAssessmentPrefetch?.()
})

// 地图类页面：主体不参与外层滚动与白底卡片，由页面自身铺满容器
const isMapWorkspace = computed(() => {
  return (
    route.name === 'RiskEval'
    || route.name === 'DynamicRiskReport'
    || route.name === 'Monitor'
    || route.name === 'HazardReview'
    || route.name === 'GroupDefense'
    || route.name === 'Emergency'
    || route.name === 'Review'
    || route.path.includes('risk-eval')
  )
})
</script>

<style lang="less" scoped>
.layout-wrapper {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: url('@/assets/imgs/bg.png') no-repeat center / cover;
}

.layout-main {
  flex: 1;
  min-width: 0;
  height: calc(100vh - 28px);
  margin: 14px 16px 14px 0;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 4px 24px rgba(0, 32, 80, 0.04);
  /* layout-main 保持固定视口不滚动，使会商研判等浮动控件在滚动页面中不产生坐标偏移与跳动 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;

  &.is-fixed-page {
    overflow: visible;
    background: transparent;
    box-shadow: none;
  }
}

.page-container {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  /* 页面级滚动由 page-container 独立承载，与外层浮动控件解耦 */
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;

  &.is-fixed-page {
    overflow: visible;
  }

  /* 优雅滚动条 */
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(148, 163, 184, 0.3);
    border-radius: 4px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background-color: rgba(148, 163, 184, 0.5);
  }
}
</style>
