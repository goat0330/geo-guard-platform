<template>
  <div ref="sectionRef" class="monitor-warning-detail">
    <div class="section-header">
      <span class="section-bullet"></span>
      <h3 class="section-title">监测预警详情</h3>
    </div>

    <div class="detail-cards">
      <!-- 卡片一：仪器告警处置数 -->
      <div class="alarm-card">
        <div class="alarm-stats-grid">
          <!-- 主指标：处置数 -->
          <div class="stat-col main-stat">
            <span class="col-label">仪器告警处置数</span>
            <div class="col-value">
              <strong class="count-num"><AnimatedNumber :value="props.data?.alarmDisposalTotal ?? '--'" /></strong>
              <span v-if="props.data?.alarmDisposalTotal != null" class="count-unit">条</span>
            </div>
          </div>

          <!-- 子指标：有效处置 -->
          <div class="stat-col sub-stat">
            <span class="col-label valid"> <i class="dot green"></i>有效处置 </span>
            <strong class="col-num green">
              <AnimatedNumber
                :value="props.data?.validAlarmDisposalCount ?? '--'"
                :suffix="props.data?.validAlarmDisposalCount != null ? '条' : ''"
              />
            </strong>
          </div>

          <!-- 子指标：无效处置 -->
          <div class="stat-col sub-stat">
            <span class="col-label invalid"> <i class="dot gray"></i>无效处置 </span>
            <strong class="col-num gray">
              <AnimatedNumber
                :value="props.data?.invalidAlarmDisposalCount ?? '--'"
                :suffix="props.data?.invalidAlarmDisposalCount != null ? '条' : ''"
              />
            </strong>
          </div>
        </div>

        <div class="card-divider"></div>

        <div class="rate-row">
          <span class="rate-label">处置率</span>
          <AnimatedNumber
            class="rate-value"
            :value="formattedDisposalRate"
            :suffix="formattedDisposalRate !== '--' ? '%' : ''"
          />
        </div>
        <div class="rate-track">
          <div
            class="rate-bar"
            :class="{ 'is-animated': sectionHasEntered }"
            :style="{ width: progressDisposalWidth }"
          ></div>
        </div>
      </div>

      <!-- 卡片二：设备在线率与监测点 -->
      <div class="device-card">
        <div class="device-main">
          <!-- 环形图 -->
          <div class="chart-wrapper">
            <div ref="chartRef" class="device-chart"></div>
            <div class="chart-center">
              <strong class="percent">
                <AnimatedNumber
                  :value="formattedOnlineRate"
                  :suffix="formattedOnlineRate !== '--' ? '%' : ''"
                />
              </strong>
              <span class="text">设备在线率</span>
            </div>
          </div>

          <!-- 图例统计 -->
          <div class="device-legend">
            <div class="legend-row">
              <i class="legend-dot blue"></i>
              <span class="legend-name">在线数</span>
              <strong class="legend-value"><AnimatedNumber :value="props.data?.monitorDeviceOnlineCount ?? '--'" /></strong>
            </div>
            <div class="legend-row">
              <i class="legend-dot gray"></i>
              <span class="legend-name">设备总数</span>
              <strong class="legend-value"><AnimatedNumber :value="props.data?.monitorDeviceCount ?? '--'" /></strong>
            </div>
          </div>
        </div>

        <!-- 底部条：监测点个数 -->
        <div class="point-bar">
          <div class="point-left">
            <img src="@/assets/imgs/home/icon-monitor-point.svg" class="point-icon" alt="" />
            <span>监测点个数</span>
          </div>
          <strong class="point-count">
            <AnimatedNumber
              :value="props.data?.monitorPointCount ?? '--'"
              :suffix="props.data?.monitorPointCount != null ? '个' : ''"
            />
          </strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useChartProgressAnimation } from '@/composables/useChartProgressAnimation.js'
import { useEnterView } from '@/composables/useEnterView.js'
import { useEcharts } from '@/hooks/useEcharts.js'

defineOptions({
  name: 'MonitorWarningDetail',
})

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})

const { targetRef: sectionRef, hasEntered: sectionHasEntered } = useEnterView()

const { chartRef, setOption } = useEcharts({
  autoInit: false,
})

const formattedDisposalRate = computed(() => {
  const rate = props.data?.alarmDisposalRate
  if (rate == null || rate === '') return '--'
  const num = Number(rate)
  if (!Number.isFinite(num)) return '--'
  const percent = num <= 1 && num > 0 ? num * 100 : num
  return Number(percent.toFixed(1))
})

const progressDisposalWidth = computed(() => {
  if (!sectionHasEntered.value || formattedDisposalRate.value === '--') return '0%'
  return `${formattedDisposalRate.value}%`
})

const formattedOnlineRate = computed(() => {
  const rate = props.data?.monitorOnlineRate
  if (rate == null || rate === '') return '--'
  const num = Number(rate)
  if (!Number.isFinite(num)) return '--'
  const percent = num <= 1 && num > 0 ? num * 100 : num
  return Number(percent.toFixed(1))
})

function createDeviceChartOption(progress) {
  const targetRate = formattedOnlineRate.value !== '--' ? Number(formattedOnlineRate.value) : 0
  const onlineValue = targetRate * progress

  return {
    animation: false,
    series: [
      {
        type: 'pie',
        radius: ['74%', '90%'],
        center: ['50%', '50%'],
        silent: true,
        label: {
          show: false,
        },
        data: [
          {
            value: onlineValue,
            itemStyle: {
              color: '#007BFF',
            },
          },
          {
            value: Math.max(0, 100 - onlineValue),
            itemStyle: {
              color: '#E7EBF2',
            },
          },
        ],
      },
    ],
  }
}

useChartProgressAnimation({
  active: sectionHasEntered,
  duration: 1800,
  render: (progress) => {
    setOption(createDeviceChartOption(progress), true, true)
  },
})
</script>

<style lang="less" scoped>
.monitor-warning-detail {
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

.detail-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.alarm-card,
.device-card {
  box-sizing: border-box;
  background: #f9fbfe;
  border-radius: 10px;
}

.alarm-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

.alarm-stats-grid {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.stat-col {
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  &.main-stat {
    flex: 1.3;
  }

  &.sub-stat {
    flex: 1;
  }
}

.col-label {
  color: #617185;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
  margin-bottom: 8px;

  &.valid,
  &.invalid {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
  }

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;

    &.green {
      background: #44b699;
    }

    &.gray {
      background: #9096a2;
    }
  }
}

.col-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.count-num {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 34px;
  font-style: normal;
  font-weight: Bold-Square;
  line-height: 36px;
}

.count-unit {
  color: #383c41;
  font-size: 20px;
  line-height: 24px;
}

.col-num {
  font-size: 20px;
  font-style: normal;
  font-weight: Bold-Square;
  line-height: 24px;
  white-space: nowrap;

  &.green {
    color: #44b699;
  }

  &.gray {
    color: #617185;
  }
}

.card-divider {
  border-top: 1px dashed #e0eefa;
  margin: 10px 0;
}

.rate-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.rate-label {
  color: #617185;
  font-size: 12px;
}

.rate-value {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 14px;
  font-style: normal;
  font-weight: Bold-Square;
}

.rate-track {
  width: 100%;
  height: 6px;
  border-radius: 4px;
  background: #e0eefa;
  overflow: hidden;
}

.rate-bar {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: 4px;
  background: linear-gradient(90deg, #007bff 0%, #3595fb 100%);
  transition: width 1.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.78) 50%, transparent 100%);
    transform: translateX(-100%);
  }

  &.is-animated::after {
    animation: rate-bar-shimmer 1.4s ease 0.45s 1 forwards;
  }
}

@keyframes rate-bar-shimmer {
  from {
    transform: translateX(-100%);
  }

  to {
    transform: translateX(100%);
  }
}

.device-card {
  padding: 14px 18px 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.device-main {
  display: flex;
  align-items: center;
  justify-content: space-around;
  min-height: 104px;
}

.chart-wrapper {
  position: relative;
  width: 96px;
  height: 96px;
  flex-shrink: 0;
}

.device-chart {
  width: 100%;
  height: 100%;
}

.chart-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;

  .percent {
    color: #383c41;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-size: 22px;
    font-style: normal;
    font-weight: Bold-Square;
    line-height: 24px;
  }

  .text {
    color: #9096a2;
    font-size: 12px;
    line-height: 16px;
  }
}

.device-legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;

  &.blue {
    background: #007bff;
  }

  &.gray {
    background: #a6acb8;
  }
}

.legend-name {
  color: #617185;
  font-size: 14px;
  min-width: 60px;
}

.legend-value {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: Bold-Square;
}

.point-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #eef4fc;
  border-radius: 6px;
  margin-top: 8px;
}

.point-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #617185;
  font-size: 14px;
}

.point-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.point-count {
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 14px;
  font-style: normal;
  font-weight: Bold-Square;
}

@media (max-width: 768px) {
  .detail-cards {
    grid-template-columns: 1fr;
  }
}
</style>
