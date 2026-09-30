<template>
  <div class="device-type">
    <div class="type-chart">
      <div ref="chartRef" class="type-pie" />
      <!-- 环形图内圈与中心文案 -->
      <i class="chart-inner" />
      <i class="chart-core" />
      <span class="chart-label">设备类型</span>
    </div>

    <ul class="type-legend">
      <li v-for="item in monitorData.deviceTypes" :key="item.key" class="legend-item">
        <i class="legend-dot" :style="{ background: item.color }" />
        <em class="legend-label">{{ item.label }}</em>
        <em class="legend-value">{{ item.value }}</em>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { monitorData } from '../useMonitorData.js'
import { useEcharts } from '@/hooks/useEcharts.js'

defineOptions({ name: 'DeviceTypeList' })

/** 环形图：外径 116.57 / 内径 74.29（130 容器内），内圈与中心文案由 DOM 叠加绘制 */
const getOption = () => {
  const data = monitorData.deviceTypes.map((item) => ({
    name: item.label,
    value: Number(String(item.value).replace(/,/g, '')) || 0,
    itemStyle: { color: item.color },
  }))

  return {
    // 起始角对齐设计稿：绿色扇区压在正下方
    startAngle: 200,
    series: [
      // 底圈：铺满整环的底色层，衬托外圈分段缝隙
      {
        type: 'pie',
        radius: ['45%', '55%'],
        center: ['50%', '50%'],
        silent: true,
        z: 1,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderWidth: 0 },
        data: [{ value: 1, itemStyle: { color: '#F3F7FB' } }],
      },
      // 底圈：铺满整环的底色层，衬托外圈分段缝隙
      {
        type: 'pie',
        radius: ['68%', '80%'],
        center: ['50%', '50%'],
        silent: true,
        z: 1,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderWidth: 0 },
        data: [{ value: 1, itemStyle: { color: '#F3F7FB' } }],
      },
      // 外圈：分段扇区，段间留缝隙
      {
        type: 'pie',
        radius: ['80%', '89.7%'],
        center: ['50%', '50%'],
        padAngle: 3,
        z: 2,
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderWidth: 0 },
        data,
      },
    ],
  }
}

const { chartRef, renderChart } = useEcharts({ getOption })

// 接口数据到达后刷新环形图
watch(
  () => monitorData.deviceTypes,
  () => renderChart(),
)
</script>

<style lang="less" scoped>
.device-type {
  display: flex;
  align-items: flex-start;
}

.type-chart {
  position: relative;
  width: 130px;
  height: 130px;
  flex-shrink: 0;
}

.type-pie {
  position: absolute;
  inset: 0;
}

// 内圈（74px 渐变）与核心圆（65px 渐变 + 白描边）
.chart-inner {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 74px;
  height: 74px;
  background: linear-gradient(180deg, #f7f9fc 0%, rgba(228, 230, 238, 0.2) 100%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.chart-core {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 60px;
  height: 60px;
  background: linear-gradient(180deg, #fafbff 41.19%, #e2e5ee 100%);
  border: 1.57px solid #fff;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.chart-label {
  position: absolute;
  top: 50%;
  left: 50%;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  color: #565e73;
  transform: translate(-50%, -50%);
}

// 图例：两列三行，行高 40
.type-legend {
  display: grid;
  flex: 1 1 0;
  min-width: 0;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px 20px;
  align-content: start;
  margin: 14px 0 0;
  padding: 0 0 0 27px;
  list-style: none;
}

.legend-item {
  display: flex;
  align-items: center;
  min-width: 0;
}

.legend-dot {
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: 20px;
  color: #565e73;
}

.legend-value {
  margin-left: 18px;
  overflow: hidden;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 800;
  line-height: 22px;
  color: #383c41;
  white-space: nowrap;
}
</style>
