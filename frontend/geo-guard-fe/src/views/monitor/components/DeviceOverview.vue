<template>
  <div class="device-overview">
    <!-- 上排：设备总数 / 设备在线率，高 76 -->
    <div class="overview-row">
      <div v-for="item in monitorData.deviceOverview" :key="item.key" class="overview-item">
        <img class="item-icon" :src="item.icon" alt="" />
        <div class="item-text">
          <p class="item-label">{{ item.label }}</p>
          <p class="item-value">{{ item.value }}</p>
        </div>
      </div>
    </div>

    <!-- 下排：在线 / 离线数 + 进度条，高 52 -->
    <div class="status-row">
      <span v-for="status in monitorData.deviceStatuses" :key="status.key" class="status-item">
        <i class="status-dot" :style="{ background: status.color }" />
        <em class="status-label">{{ status.label }}</em>
        <em class="status-value" :style="{ color: status.color }">{{ status.value }}</em>
      </span>
    </div>

    <div class="status-bar">
      <i class="bar-fill" :style="{ width: onlinePercent }" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { monitorData } from '../useMonitorData.js'

defineOptions({ name: 'DeviceOverview' })

/** 在线占比：在线数 / (在线数 + 离线数)，用于底部进度条宽度 */
const onlinePercent = computed(() => {
  const total = monitorData.deviceStatuses.reduce((sum, item) => sum + item.value, 0)
  if (!total) {
    return '0%'
  }
  const online = monitorData.deviceStatuses.find((item) => item.key === 'online')
  return `${(((online?.value || 0) / total) * 100).toFixed(1)}%`
})
</script>

<style lang="less" scoped>
.device-overview {
  padding: 12px 16px;
  box-sizing: border-box;
  background: #f5f9fc;
  border: 1px solid #edf6ff;
  border-radius: 10px;
}

.overview-row {
  display: flex;
  align-items: center;
  height: 76px;
  margin: -12px -16px 0;
  padding: 0 16px;
}

.overview-item {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  align-items: center;
}

// 两列之间的竖向分割线（36px 高，位于列中间）
.overview-item + .overview-item {
  position: relative;
  padding-left: 24px;

  &::before {
    position: absolute;
    top: 50%;
    left: 0;
    width: 1px;
    height: 36px;
    background: #e4eaef;
    transform: translateY(-50%);
    content: '';
  }
}

.item-icon {
  width: 44px;
  height: 44px;
  margin-right: 14px;
  object-fit: contain;
}

.item-text {
  min-width: 0;
}

.item-label {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  line-height: 14px;
  color: #617185;
}

.item-value {
  margin: 8px 0 0;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 24px;
  font-style: normal;
  font-weight: 800;
  line-height: 26px;
  color: #383c41;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-top: 1px solid #e4eaef;
}

.status-item {
  display: inline-flex;
  align-items: center;
}

.status-dot {
  width: 6px;
  height: 6px;
  margin-right: 8px;
  border-radius: 50%;
}

.status-label {
  margin-right: 8px;
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: 20px;
  color: #9096a2;
}

.status-value {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 800;
  line-height: 20px;
}

.status-bar {
  height: 4px;
  overflow: hidden;
  background: #c3cad7;
  border-radius: 3px;
}

.bar-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #5aa7ff 0%, #007bff 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}
</style>
