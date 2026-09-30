<template>
  <div class="alarm-situation">
    <!-- 告警总数卡：412x68 -->
    <div class="total-card">
      <img class="total-icon" :src="monitorData.alarmSituation.totalIcon" alt="" />
      <span class="total-label">仪器告警总数</span>
      <span class="total-count">
        <em class="total-value">{{ monitorData.alarmSituation.total }}</em>
        <em class="total-unit">{{ monitorData.alarmSituation.unit }}</em>
      </span>
    </div>

    <!-- 四级告警卡：每级独立卡片 96x78 -->
    <div class="level-row">
      <div v-for="level in monitorData.alarmSituation.levels" :key="level.key" class="level-card">
        <div class="level-head">
          <i class="level-dot" :style="{ background: level.color }" />
          <span class="level-label">{{ level.label }}</span>
        </div>
        <p class="level-value">{{ level.value }}</p>
        <i class="level-bar">
          <i class="level-bar-fill" :style="{ width: barWidth(level), background: level.color }" />
        </i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { monitorData } from '../useMonitorData.js'

defineOptions({ name: 'AlarmSituation' })

const toNumber = (v) => Number(String(v).replace(/,/g, '')) || 0

/** 等级进度条：按各级数量占告警总数的比例填充（48px 轨道） */
const barWidth = (level) => {
  const total = toNumber(monitorData.alarmSituation.total)
  if (!total) {
    return '6px'
  }
  return `${Math.max(6, Math.round((toNumber(level.value) / total) * 48))}px`
}
</script>

<style lang="less" scoped>
.alarm-situation {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.total-card {
  display: flex;
  align-items: center;
  height: 68px;
  padding: 0 16px;
  box-sizing: border-box;
  background: #f5f9fc;
  border: 1px solid #edf6ff;
  border-radius: 10px;
}

.total-icon {
  width: 44px;
  height: 44px;
  margin-right: 8px;
  object-fit: contain;
}

.total-label {
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  color: #383c41;
}

.total-count {
  display: flex;
  flex: 1 1 0;
  align-items: baseline;
  justify-content: flex-end;
  gap: 2px;
}

.total-value {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 28px;
  font-style: normal;
  font-weight: 800;
  line-height: 26px;
  color: #222527;
}

.total-unit {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 20px;
  font-style: normal;
  font-weight: 800;
  line-height: 32px;
  color: #222527;
}

.level-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.level-card {
  display: flex;
  width: 96px;
  height: 78px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  background: #f5f9fc;
  border: 1px solid #edf6ff;
  border-radius: 12px;
}

.level-head {
  display: flex;
  align-items: center;
  height: 18px;
}

.level-dot {
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
}

.level-label {
  font-size: 14px;
  font-weight: 400;
  line-height: 18px;
  color: #617185;
}

.level-value {
  margin: 6px 0 0;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 800;
  line-height: 18px;
  color: #383c41;
}

.level-bar {
  position: relative;
  width: 48px;
  height: 4px;
  margin-top: 8px;
  overflow: hidden;
  background: #e1e6ec;
  border-radius: 3px;
}

.level-bar-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 3px;
}
</style>
