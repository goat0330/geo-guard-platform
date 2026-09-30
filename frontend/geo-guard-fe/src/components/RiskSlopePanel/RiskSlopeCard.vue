<template>
  <article class="risk-slope-card" :class="{ 'is-active': active }" @click="handleLocate">
    <img :src="levelIcon" alt="斜坡单元" />
    <div class="slope-info">
      <div class="slope-name" :title="slopeTitle">
        {{ slopeTitle }}
        <span v-if="slope.level" :style="{ background: levelColor }">{{ slope.level }}</span>
      </div>
      <div class="meta" :title="slope.address || '--'">
        <i class="iconfont icon-address"></i>{{ slope.address || '--' }}
      </div>
      <div class="meta"><i class="iconfont icon-a-Frame1"></i>更新时间{{ slope.updateTime || '暂无' }}</div>
    </div>
    <div class="card-actions">
      <button type="button" @click.stop="emit('risk-analysis', slope)">风险分析</button>
      <button type="button" @click.stop="handleLocate">地图定位</button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import riskBlueIcon from '@/assets/imgs/risk/blue.svg'
import riskOrangeIcon from '@/assets/imgs/risk/orange.svg'
import riskRedIcon from '@/assets/imgs/risk/red.svg'
import riskYellowIcon from '@/assets/imgs/risk/yellow.svg'
import { RISK_LEVEL } from '@/utils/enum.js'
import { getDynamicRiskLevelColor, formatSlopeName } from '@/utils/riskEvaluation.js'

const props = defineProps({
  slope: {
    type: Object,
    default: () => ({}),
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['risk-analysis', 'locate'])

const slopeTitle = computed(
  () =>
    formatSlopeName(
      props.slope.name ||
        props.slope.slopeUnitName ||
        props.slope.slopeName ||
        props.slope.code ||
        props.slope.slopeUnitId,
    ) || '未知斜坡单元',
)

// 风险等级图标严格按极高、高、中、低映射为红、橘、黄、蓝。
const LEVEL_ICON_MAP = {
  [RISK_LEVEL.EXTREME_HIGH]: riskRedIcon,
  [RISK_LEVEL.HIGH]: riskOrangeIcon,
  [RISK_LEVEL.MIDDLE]: riskYellowIcon,
  [RISK_LEVEL.LOW]: riskBlueIcon,
  [RISK_LEVEL.NONE]: riskBlueIcon,
}

const levelIcon = computed(() => LEVEL_ICON_MAP[props.slope.levelValue] ?? riskBlueIcon)

const levelColor = computed(() => getDynamicRiskLevelColor(props.slope.levelValue))

const handleLocate = () => {
  emit('locate', props.slope)
}
</script>

<style lang="less" scoped>
.risk-slope-card {
  flex: 0 0 92px;
  min-height: 92px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 92px;
  gap: 10px;
  align-items: center;
  padding: 14px;
  box-sizing: border-box;
  border: 1px solid #dce3eb;
  border-radius: 8px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    border-color: #9ac7fb;
    background: #f8fbff;
  }

  &.is-active {
    border-color: #087df5;
    background: #f1f7ff;
    box-shadow: 0 2px 8px rgba(0, 123, 255, 0.08);
  }
}

.risk-slope-card > img {
  width: 38px;
  height: 28px;
  object-fit: contain;
}

.slope-info {
  min-width: 0;
}

.slope-name {
  overflow: hidden;
  color: #252b33;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.slope-name span {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 7px;
  border-radius: 3px;
  color: #ffffff;
  font-size: 10px;
  font-weight: 400;
  vertical-align: middle;
}

.meta {
  overflow: hidden;
  margin-top: 7px;
  color: #9aa5b2;
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.meta i {
  margin-right: 5px;
  font-size: 12px;
}

.card-actions {
  display: grid;
  gap: 8px;
  padding-left: 14px;
  border-left: 1px solid #e4eaf0;
}

.card-actions button {
  height: 26px;
  border: 0;
  border-radius: 4px;
  background: #e5f2ff;
  color: #087df5;
  font-size: 12px;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;

  &:hover {
    background-color: #007bff;
    color: #ffffff;
  }
}

@media (max-width: 1366px) {
  .risk-slope-card {
    grid-template-columns: 30px minmax(0, 1fr) 74px;
    padding-inline: 10px;
  }
  .risk-slope-card > img {
    width: 30px;
  }
  .card-actions {
    padding-left: 8px;
  }
}
</style>
