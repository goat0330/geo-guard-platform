<template>
  <div class="risk-level">
    <div class="risk-donut">
      <!-- ECharts 饼图：环宽与外径按设计稿 150 盒换算为百分比半径 -->
      <div ref="chartRef" class="risk-donut__chart" />

      <!-- 中心圆盘：设计为两层渐变叠出微微凸起的圆盘，最上层带白色描边 -->
      <span class="risk-donut__hole" aria-hidden="true" />
      <span class="risk-donut__core">
        <em>风险等级</em>
        <em>占比</em>
      </span>
    </div>

    <!-- 图例：等级说明 + 数量 -->
    <ul class="risk-legend">
      <li v-for="item in riskLevelShare" :key="item.key" class="risk-legend__item">
        <i class="risk-legend__dot" :style="{ background: item.color }" />
        <span class="risk-legend__label">{{ item.label }}</span>
        <strong class="risk-legend__value"><AnimatedNumber :value="item.value" /></strong>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useEcharts } from '@/hooks/useEcharts.js'
import { hazardReviewData } from '../../useHazardReviewData.js'

defineOptions({ name: 'ReviewRiskLevelSection' })

/** 风险等级分布（A/B/C/D）：接口返回后写入共享数据源 */
const riskLevelShare = computed(() => hazardReviewData.overview.riskLevels)

/** 环形图分段顺序：设计稿自 12 点顺时针为 C → D → A → B（图例仍按 A→D 展示） */
const RING_ORDER = ['C', 'D', 'A', 'B']

/** 环形图数据：按设计稿顺序排列，保证分段起点与配色一致 */
const ringItems = computed(() =>
  RING_ORDER.map((key) => riskLevelShare.value.find((item) => item.key === key))
    .filter(Boolean)
    .map((item) => ({
      name: item.label,
      value: Number(item.value) || 0,
      itemStyle: { color: item.color },
    })),
)

const getOption = (px) => ({
  /* 与首页看板一致：环形图分段展开动画，数据重绘时重新播放 */
  animation: true,
  animationDuration: 1400,
  animationEasing: 'cubicOut',
  tooltip: { show: false },
  series: [
    {
      type: 'pie',
      // 细环：150 盒内设计稿环带为 r 60 → 67.5，换算成相对 75 的百分比即 80% → 90%
      radius: ['80%', '90%'],
      center: ['50%', '50%'],
      startAngle: 90,
      clockwise: true,
      silent: true,
      label: { show: false },
      labelLine: { show: false },
      emphasis: { disabled: true },
      // 细白描边消除相邻分段之间的锯齿缝
      itemStyle: { borderColor: '#FFFFFF', borderWidth: px(0.5) },
      data: ringItems.value,
    },
  ],
})

const { chartRef, renderChart } = useEcharts({ getOption })

/** 统计返回后重绘环形图（notMerge 重建 + 动画参数，形成变化效果） */
watch(riskLevelShare, () => renderChart())
</script>

<style lang="less" scoped>
.risk-level {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.risk-donut {
  position: relative;
  width: 150px;
  height: 150px;
  flex-shrink: 0;
}

.risk-donut__chart {
  width: 100%;
  height: 100%;
}

/* 底盘：填满环内孔洞，向下略微变暗 */
.risk-donut__hole {
  position: absolute;
  inset: 32.14px;
  border-radius: 50%;
  background: linear-gradient(180deg, #f7f9fc 0%, rgba(228, 230, 238, 0.2) 100%);
  pointer-events: none;
}

/* 中心盘：白色描边叠出层次，承载文案 */
.risk-donut__core {
  position: absolute;
  inset: 37.5px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border: 1.57px solid #ffffff;
  border-radius: 50%;
  background: linear-gradient(180deg, #fafbff 41.19%, #e2e5ee 100%);
  pointer-events: none;

  em {
    color: #565e73;
    font-size: 12px;
    font-style: normal;
    font-weight: 700;
    line-height: 18px;
    white-space: nowrap;
  }
}

.risk-legend {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.risk-legend__item {
  display: flex;
  /* D 级说明文案会折行，顶部对齐让圆点与数量都跟文案首行齐平 */
  align-items: flex-start;
  gap: 8px;
}

.risk-legend__dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  /* 首行行高 20px，8px 圆点下移 6px 与文字视觉居中 */
  margin-top: 6px;
  border-radius: 50%;
}

.risk-legend__label {
  flex: 1 1 auto;
  min-width: 0;
  color: #565e73;
  font-size: 14px;
  line-height: 20px;
}

.risk-legend__value {
  flex-shrink: 0;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 16px;
  font-weight: 600;
  line-height: 20px;
  white-space: nowrap;
}
</style>
