<template>
  <section class="process-section">
    <!-- 折叠效果与隐患复核智能体面板一致：胶囊标题 + 箭头旋转 + 行高过渡；
         加载中的块不可折叠也不显示箭头，避免误导成「可以点开」 -->
    <button class="section-title" type="button" :disabled="loading" @click="emit('toggle', step.key)">
      <span>
        <el-icon>
          <CircleCheck />
        </el-icon>{{ step.title }}
      </span>
      <el-icon v-if="!loading" class="section-arrow" :class="{ 'is-collapsed': !expanded }">
        <ArrowUp />
      </el-icon>
    </button>

    <!-- 加载中：三点波浪（节奏与风险评价智能体面板一致），加载结束再展开正文 -->
    <div v-if="loading" class="section-loading">
      <i></i><i></i><i></i>
    </div>

    <div v-else class="section-body" :class="{ 'is-collapsed': !expanded }">
      <div>
        <p v-if="step.subtitle" class="step-subtitle">{{ step.subtitle }}</p>

        <DisplacementChart v-if="step.chart" class="step-chart" :chart="chart" />

        <!-- 说明列表 -->
        <ul v-if="step.points && step.points.length" class="point-list">
          <li v-for="(point, pointIndex) in step.points" :key="pointIndex">{{ toText(point) }}</li>
        </ul>

        <p v-if="step.tip" class="tip-note">{{ step.tip }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import DisplacementChart from './DisplacementChart.vue'

defineOptions({ name: 'WarningStepSection' })

defineProps({
  /** 单个步骤：{ key, title, subtitle, points, chart, tip } */
  step: {
    type: Object,
    required: true,
  },
  expanded: {
    type: Boolean,
    default: true,
  },
  /** 曲线数据，仅 chart 步骤使用 */
  chart: {
    type: Object,
    default: () => ({ times: [], values: [] }),
  },
  /** 该块正在产出：展示三点加载态，正文暂不渲染 */
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['toggle'])

/**
 * 兼容两种写法并统一输出纯文本：
 * 1. '一行文案'
 * 2. ['前缀', { text: '强调片段' }, '后缀']
 */
const toText = (point) => {
  if (typeof point === 'string') return point
  if (Array.isArray(point)) {
    return point
      .map((fragment) => (typeof fragment === 'string' ? fragment : fragment?.text ?? ''))
      .join('')
  }
  return point?.text ?? ''
}
</script>

<style lang="less" scoped>
.process-section+.process-section {
  margin-top: 14px;
}

.section-title {
  display: flex;
  width: 100%;
  height: 44px;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border: 1px solid #dcedff;
  border-radius: 8px;
  background: #f5f9ff;
  color: #007bff;
  font-family: inherit;
  cursor: pointer;
}

.section-title>span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.section-arrow {
  transition: transform 0.24s ease;
}

.section-title:disabled {
  cursor: default;
}

/* 加载态：三点波浪占位，左对齐与正文一致 */
.section-loading {
  display: flex;
  height: 36px;
  align-items: flex-end;
  gap: 4px;
  padding: 0 16px 8px;
}

.section-loading i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9096a2;
  animation: section-loading 1.2s infinite ease-in-out;
}

.section-loading i:nth-child(2) {
  animation-delay: 0.16s;
}

.section-loading i:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes section-loading {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.section-arrow.is-collapsed {
  transform: rotate(180deg);
}

.section-body {
  display: grid;
  grid-template-rows: 1fr;
  opacity: 1;
  transition: grid-template-rows 0.28s ease, opacity 0.2s ease;
}

.section-body.is-collapsed {
  grid-template-rows: 0fr;
  opacity: 0;
}

.section-body>div {
  min-height: 0;
  overflow: hidden;
}

.step-subtitle {
  margin: 0;
  padding: 14px 16px 0;
  color: #383C41;
  font-size: 12px;
  line-height: 24px;
  font-weight: bold;
}

.step-chart {
  margin: 6px 16px 0;
}

.point-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 14px 16px 0;
  list-style: none;
}

.point-list li {
  position: relative;
  padding-left: 12px;
  color: #9096A2;
  font-size: 12px;
  line-height: 22px;
}

/* 文案中的强调片段 */
.point-list li b {
  color: #222527;
  font-weight: 600;
}

.point-list li::before {
  position: absolute;
  top: 9px;
  left: 0;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #617185;
  content: '';
}

/* 分析摘要：浅蓝底 + 左侧主色竖条。
   高度按内容自适应：处理结果这类文案长度不固定，写死高度会让第 3 行及之后
   被 .section-body 的 overflow: hidden 裁掉，故用 min-height 兜住设计稿的 64 高度 */
.tip-note {
  position: relative;
  margin: 14px 0px 0;
  padding: 12px 16px ;
  font-size: 12px;
  line-height: 20px;
  font-weight: 400;
  color: #153A59;
  width: 412px;
  min-height: 64px;
  border-radius: 6px;
  border-left: 2px solid #007BFF;
  background: #F1F7FF;
}

.tip-note::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 4px;
  border-radius: 8px 0 0 8px;
  background: #007bff;
  content: '';
}
</style>
