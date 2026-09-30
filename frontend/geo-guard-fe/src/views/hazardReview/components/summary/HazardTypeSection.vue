<template>
  <div ref="targetRef" class="type-chart">
    <!-- 纵轴刻度：与网格线共用同一套比例 -->
    <div class="type-chart__axis">
      <span
        v-for="(tick, index) in ticks"
        :key="tick"
        class="type-chart__tick"
        :style="{ top: getTickOffset(index) }"
      >
        {{ tick }}
      </span>
    </div>

    <div class="type-chart__plot">
      <div class="type-chart__grid">
        <i
          v-for="(tick, index) in ticks"
          :key="tick"
          class="type-chart__grid-line"
          :style="{ top: getTickOffset(index) }"
        />

        <div class="type-chart__bars">
          <div v-for="item in hazardTypeStats" :key="item.key" class="type-bar">
            <span class="type-bar__value" :class="{ 'is-highlight': item.highlight }">
              <AnimatedNumber :value="item.value" />
            </span>
            <i
              class="type-bar__shape"
              :class="{ 'is-highlight': item.highlight }"
              :style="{ height: getBarHeight(item.value) }"
            />
          </div>
        </div>
      </div>

      <div class="type-chart__labels">
        <span v-for="item in hazardTypeStats" :key="item.key" class="type-chart__label">
          {{ item.label }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useProgressReveal } from '@/composables/useProgressReveal.js'
import { hazardReviewData } from '../../useHazardReviewData.js'

defineOptions({ name: 'ReviewHazardTypeSection' })

/** 隐患类型分布（六类）：接口返回后写入共享数据源 */
const hazardTypeStats = computed(() => hazardReviewData.overview.hazardTypes)

/** 柱高变化效果（与首页看板一致）：进入视口及统计刷新时，柱子从 0 长到目标高度 */
const { targetRef, progress } = useProgressReveal(() =>
  hazardTypeStats.value.map((item) => item.value).join(','),
)

/** 网格线区间数：4 个区间对应 5 条刻度线 */
const TICK_INTERVAL_COUNT = 4

/** 纵轴步长候选，用于把轴上限收敛到规整数值 */
const TICK_STEPS = [
  10, 20, 25, 50, 100, 150, 200, 250, 300, 400, 500, 600, 800,
  1000, 1500, 2000, 2500, 3000, 4000, 5000, 8000, 10000,
]

const maxValue = computed(() => Math.max(...hazardTypeStats.value.map((item) => Number(item.value) || 0), 0))

/** 纵轴上限：向上取规整步长，最高柱不贴顶 */
const axisMax = computed(() => {
  const need = maxValue.value / TICK_INTERVAL_COUNT
  const step = TICK_STEPS.find((item) => item >= need) ?? Math.ceil(need / 1000) * 1000
  return step * TICK_INTERVAL_COUNT
})

/** 纵轴刻度值，自上而下由大到小 */
const ticks = computed(() => {
  const step = axisMax.value / TICK_INTERVAL_COUNT
  return Array.from({ length: TICK_INTERVAL_COUNT + 1 }, (_, index) => axisMax.value - step * index)
})

/** 刻度线 / 刻度文字的纵向位置，与柱高共用同一比例 */
const getTickOffset = (index) => `${(index / TICK_INTERVAL_COUNT) * 100}%`

/** 柱高：按纵轴上限换算为百分比，再乘进度系数实现生长动画 */
const getBarHeight = (value) => {
  if (!axisMax.value) {
    return '0%'
  }
  return `${((Number(value) || 0) / axisMax.value) * 100 * progress.value}%`
}
</script>

<style lang="less" scoped>
.type-chart {
  display: flex;
  column-gap: 4px;
  padding-top: 10px;
}

/* 纵轴：固定与绘图区等高，刻度文字以网格线为中心上下溢出 */
.type-chart__axis {
  position: relative;
  width: 28px;
  height: 128px;
  flex-shrink: 0;
  color: #b2b3bd;
  font-size: 12px;
  line-height: 20px;
}

.type-chart__tick {
  position: absolute;
  right: 0;
  transform: translateY(-50%);
  white-space: nowrap;
}

.type-chart__plot {
  flex: 1 1 0;
  min-width: 0;
}

.type-chart__grid {
  position: relative;
  height: 128px;
}

.type-chart__grid-line {
  position: absolute;
  right: 0;
  left: 0;
  height: 1px;
  background: #e7e7e7;
}

.type-chart__bars {
  position: absolute;
  inset: 0;
  display: flex;
}

.type-bar {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}

.type-bar__value {
  margin-bottom: 4px;
  color: #007bff;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 14px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;

  &.is-highlight {
    color: #81cbb8;
  }
}

.type-bar__shape {
  display: block;
  width: 12px;
  background: #81b6f7;

  &.is-highlight {
    background: #81cbb8;
  }
}

.type-chart__labels {
  display: flex;
  margin-top: 5px;
}

.type-chart__label {
  flex: 1 1 0;
  min-width: 0;
  color: #383c41;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  /* 类目名保持单行，避免「危岩/崩塌」在窄列内折行 */
  white-space: nowrap;
}
</style>
