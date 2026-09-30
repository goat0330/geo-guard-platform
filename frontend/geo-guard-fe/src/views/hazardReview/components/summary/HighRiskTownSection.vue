<template>
  <ul ref="targetRef" class="town-list">
    <li v-for="(item, index) in highRiskTowns" :key="item.key" class="town-item">
      <div class="town-item__head">
        <span class="town-item__rank" :class="getRankClass(index)">{{ index + 1 }}</span>
        <span class="town-item__name">{{ item.name }}</span>
        <strong class="town-item__value"><AnimatedNumber :value="item.value" /></strong>
      </div>
      <span class="town-item__bar">
        <i :style="{ width: getBarWidth(item.value) }" />
      </span>
    </li>
  </ul>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useProgressReveal } from '@/composables/useProgressReveal.js'
import { hazardReviewData } from '../../useHazardReviewData.js'

defineOptions({ name: 'ReviewHighRiskTownSection' })

/** 高风险乡镇（A/B 类隐患点数量前五）：接口返回后写入共享数据源 */
const highRiskTowns = computed(() => hazardReviewData.overview.highRiskTowns)

/** 进度条变化效果（与首页看板一致）：进入视口及统计刷新时，条宽从 0 涨到目标比例 */
const { targetRef, progress } = useProgressReveal(() =>
  highRiskTowns.value.map((item) => item.value).join(','),
)

/** 排名底色按名次区分，第 4 名及以后统一使用默认底色 */
const getRankClass = (index) => `is-rank-${Math.min(index + 1, 4)}`

const maxValue = computed(() => Math.max(...highRiskTowns.value.map((item) => Number(item.value) || 0), 0))

/** 条目进度条：按同类最大值归一，再乘进度系数实现增长动画 */
const getBarWidth = (value) => {
  if (!maxValue.value) {
    return '0%'
  }
  return `${((Number(value) || 0) / maxValue.value) * 100 * progress.value}%`
}
</script>

<style lang="less" scoped>
.town-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.town-item__head {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 名次角标：自上而下的渐隐底色 */
.town-item__rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 4px;
  background: linear-gradient(180deg, #edeff5 0%, rgba(237, 239, 245, 0) 100%);
  color: #000000;
  font-size: 10px;
  line-height: 20px;
}

.town-item__rank.is-rank-1 {
  background: linear-gradient(180deg, #f3d5cd 0%, rgba(237, 239, 245, 0) 100%);
}

.town-item__rank.is-rank-2 {
  background: linear-gradient(180deg, #f7e7c5 0%, rgba(237, 239, 245, 0) 100%);
}

.town-item__rank.is-rank-3 {
  background: linear-gradient(180deg, #d2e2f7 0%, rgba(237, 239, 245, 0) 100%);
}

.town-item__name {
  color: #383c41;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  white-space: nowrap;
}

.town-item__value {
  margin-left: auto;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 18px;
  font-weight: 700;
  line-height: 18px;
  white-space: nowrap;
}

.town-item__bar {
  display: block;
  height: 3px;
  margin-top: 6px;
  margin-left: 32px;
  background: #eaeef4;
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    background: linear-gradient(90deg, #81b6f7 0%, #007bff 100%);
  }
}
</style>
