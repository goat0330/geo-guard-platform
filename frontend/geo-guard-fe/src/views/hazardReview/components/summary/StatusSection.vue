<template>
  <div ref="targetRef" class="status">
    <!-- 处理进度：已处理 / 处理率，右侧为处理率进度环 -->
    <div class="status-progress">
      <div class="progress-item">
        <img class="progress-item__icon" :src="reviewStatus.processed.icon" alt="" />
        <div class="progress-item__text">
          <span class="progress-item__label">{{ reviewStatus.processed.label }}</span>
          <strong class="progress-item__value">
            <AnimatedNumber :value="reviewStatus.processed.value" />
          </strong>
        </div>
      </div>

      <i class="status-divider" />

      <div class="progress-item">
        <img class="progress-item__icon" :src="reviewStatus.rate.icon" alt="" />
        <div class="progress-item__text">
          <span class="progress-item__label">{{ reviewStatus.rate.label }}</span>
          <strong class="progress-item__value">
            <AnimatedNumber :value="reviewStatus.rate.value" suffix="%" />
          </strong>
        </div>
      </div>

      <span class="rate-ring" :style="ringStyle" aria-hidden="true" />
    </div>

    <!-- 复核明细：待复核 / 数据异常 / 疑似重复 -->
    <div class="status-detail">
      <div class="detail-pending">
        <img class="detail-pending__icon" :src="reviewStatus.pending.icon" alt="" />
        <div class="detail-pending__text">
          <span class="detail-pending__label">{{ reviewStatus.pending.label }}</span>
          <strong class="detail-pending__value">
            <AnimatedNumber :value="reviewStatus.pending.value" />
          </strong>
        </div>
      </div>

      <!-- 待复核与首个异常项之间的分隔线 -->
      <i class="status-divider" />

      <template v-for="(item, index) in anomalyItems" :key="item.key">
        <!-- 异常项之间同样需要竖分割线 -->
        <i v-if="index > 0" class="status-divider" />

        <div class="detail-anomaly">
          <span class="detail-anomaly__label">
            <i class="detail-anomaly__dot" :style="{ background: item.color }" />
            {{ item.label }}
          </span>
          <p class="detail-anomaly__value">
            <strong><AnimatedNumber :value="item.value" /></strong>
            <em>{{ item.percent }}</em>
          </p>
          <span class="detail-anomaly__bar">
            <i :style="{ width: item.width, background: item.color }" />
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useProgressReveal } from '@/composables/useProgressReveal.js'
import { hazardReviewData } from '../../useHazardReviewData.js'

defineOptions({ name: 'ReviewStatusSection' })

/** 复核状态（处理进度 / 待复核 / 异常占比）：接口返回后写入共享数据源 */
const reviewStatus = computed(() => hazardReviewData.overview.reviewStatus)

/**
 * 进度动画（与首页看板一致）：元素进入视口及统计刷新时，进度环与占比条从 0 涨到目标值。
 * 依赖取已处理 / 处理率 / 待复核与两项异常数，任一变化即重放
 */
const { targetRef, progress } = useProgressReveal(() => [
  reviewStatus.value.processed.value,
  reviewStatus.value.rate.value,
  reviewStatus.value.pending.value,
  reviewStatus.value.anomalies.map((item) => item.value).join(','),
])

/**
 * 处理率进度环：浅色轨道自 12 点方向顺时针铺剩余占比，蓝色进度弧接到 12 点收口
 * （与设计稿一致：进度弧的终点落在 12 点方向，而非起点）
 */
const ringStyle = computed(() => {
  const rate = Math.min(100, Math.max(0, Number(reviewStatus.value.rate.value) || 0)) * progress.value
  const remain = 100 - rate
  return {
    background: `conic-gradient(rgba(0, 28, 57, 0.15) 0 ${remain}%, #699ef5 ${remain}% 100%)`,
  }
})

/**
 * 异常项：占比直接用接口返回的百分比，进度条按两项中的最大值归一；
 * 接口未返回占比时留空，不按总量反推。两者都乘进度系数，与进度环同步增长
 */
const anomalyItems = computed(() => {
  const list = reviewStatus.value.anomalies
  const max = Math.max(...list.map((item) => Number(item.value) || 0), 0)
  const ratio = progress.value
  return list.map((item) => ({
    ...item,
    percent: item.rate === null || item.rate === undefined ? '' : `${(Number(item.rate) * ratio).toFixed(1)}%`,
    width: max ? `${((Number(item.value) || 0) / max) * 100 * ratio}%` : '0%',
  }))
})
</script>

<style lang="less" scoped>
.status {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 已处理 / 处理率 */
.status-progress {
  display: flex;
  align-items: center;
  height: 76px;
  padding: 0 20px 0 16px;
  box-sizing: border-box;
  border: 1px solid #edf6ff;
  border-radius: 10px;
  background: #f5f9fc;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.progress-item__icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  object-fit: contain;
}

.progress-item__text {
  min-width: 0;
}

.progress-item__label {
  display: block;
  color: #617185;
  font-size: 14px;
  line-height: 14px;
  white-space: nowrap;
}

.progress-item__value {
  display: block;
  margin-top: 8px;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 24px;
  font-weight: 700;
  line-height: 26px;
  white-space: nowrap;
}

/* 处理率进度环：6px 圆环，中心用遮罩挖空 */
.rate-ring {
  width: 44px;
  height: 44px;
  margin-left: auto;
  flex-shrink: 0;
  border-radius: 50%;
  -webkit-mask: radial-gradient(circle closest-side, transparent 0 16px, #000 16px);
  mask: radial-gradient(circle closest-side, transparent 0 16px, #000 16px);
}

.status-divider {
  width: 1px;
  height: 36px;
  flex-shrink: 0;
  background: #e4eaef;
}

.status-progress .status-divider {
  margin: 0 20px;
}

/* 待复核 / 数据异常 / 疑似重复：各项之间由分割线等距分开 */
.status-detail {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80px;
  padding: 0 16px;
  box-sizing: border-box;
  border: 1px solid #edf6ff;
  border-radius: 12px;
  background: #f5f9fc;
}

/* 待复核统计项 */
.detail-pending {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.detail-pending__icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  object-fit: contain;
}

.detail-pending__text {
  min-width: 0;
}

.detail-pending__label {
  display: block;
  color: #617185;
  font-size: 14px;
  line-height: 14px;
  white-space: nowrap;
}

.detail-pending__value {
  display: block;
  margin-top: 8px;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 24px;
  font-weight: 700;
  line-height: 26px;
  white-space: nowrap;
}



.detail-anomaly {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.detail-anomaly__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #617185;
  font-size: 14px;
  line-height: 14px;
  white-space: nowrap;
}

.detail-anomaly__dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
}

.detail-anomaly__value {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 0;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 18px;
  font-weight: 600;
  line-height: 18px;

  em {
    font-style: normal;
  }
}

.detail-anomaly__bar {
  display: block;
  width: 66px;
  height: 4px;
  border-radius: 3px;
  background: #e1e6ec;
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
  }
}
</style>
