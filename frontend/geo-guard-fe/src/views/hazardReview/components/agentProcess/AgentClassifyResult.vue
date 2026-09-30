<template>
  <div class="classify-result">
    <!-- 分类分级结果表 -->
    <div class="classify-table">
      <div class="table-row is-head">
        <span v-for="column in table.columns" :key="column">{{ column }}</span>
      </div>
      <div v-for="row in table.rows" :key="row[0]" class="table-row">
        <span v-for="(cell, index) in row" :key="index" :class="{ 'is-value': index === 1 }">{{ cell }}</span>
      </div>
    </div>

    <!-- 威胁指标：整块粉底，列间用细线分隔 -->
    <div class="threat-metrics">
      <div v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span class="metric-label">{{ metric.label }}</span>
        <p class="metric-value">
          <span class="num-font">{{ metric.value }}</span><span class="metric-unit">{{ metric.unit }}</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'AgentClassifyResult' })

defineProps({
  /** 分类表：{ columns: [], rows: [[]] } */
  table: {
    type: Object,
    default: () => ({ columns: [], rows: [] }),
  },
  /** 威胁指标：[{ label, value, unit }] */
  metrics: {
    type: Array,
    default: () => [],
  },
})
</script>

<style lang="less" scoped>
.classify-table {
  overflow: hidden;
  border: 1px solid #e9ecf3;
  border-radius: 10px;
  background: #ffffff;
}

.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  align-items: center;
  height: 44px;
  color: #383c41;
  font-size: 14px;

  &:not(.is-head) {
    border-top: 1px solid #eef1f6;
  }

  span {
    text-align: center;

    &:first-child {
      padding-left: 16px;
      text-align: left;
    }
  }

  /* 当前结果列取深色，便于和依据区分 */
  .is-value {
    color: #222527;
    font-weight: 500;
  }
}

.table-row.is-head {
  background: #f5f6fa;
  color: #9096a2;
}

/* 威胁指标：整块浅粉底（左侧略深向右过渡），与设计稿一致 */
.threat-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 12px;
  border-radius: 12px;
  background: linear-gradient(90deg, #fceef0 0%, #fdf3f3 55%, #fef8f7 100%);
}

.metric-card {
  padding: 14px 4px;
  text-align: center;
}

.metric-card + .metric-card {
  border-left: 1px solid #f7e2e2;
}

.metric-label {
  display: block;
  color: #b09a9a;
  font-size: 12px;
  line-height: 18px;
}

.metric-value {
  margin: 6px 0 0;
  color: #dd4739;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;
}

.metric-unit {
  font-size: 12px;
  font-weight: 400;
}

/* 数字使用项目数字字体 */
.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}
</style>
