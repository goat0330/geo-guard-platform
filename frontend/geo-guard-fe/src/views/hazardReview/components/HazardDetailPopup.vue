<template>
  <div class="hazard-popup">
    <header class="popup-header">
      <span class="popup-title" :title="data.name">{{ data.name }}</span>
      <!-- 风险等级接口未返回时整块隐藏，避免出现空胶囊 -->
      <span v-if="data.level" class="level-tag">{{ data.level }}</span>
      <button class="close-button" type="button" title="关闭" @click="emit('close')">
        <img :src="closeIcon" alt="关闭" />
      </button>
    </header>

    <!-- 威胁与稳定性：一行四列 -->
    <div class="stat-grid">
      <div v-for="stat in stats" :key="stat.label" class="stat-cell">
        <span class="stat-label">{{ stat.label }}</span>
        <strong class="stat-value">{{ stat.value }}</strong>
      </div>
    </div>

    <!-- 隐患点编号 / 曾发生灾害时间 -->
    <div class="info-list">
      <div v-for="row in infoRows" :key="row.label" class="info-row">
        <span class="info-label">{{ row.label }}</span>
        <span class="info-value">{{ row.value }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import closeIcon from '@/assets/imgs/hazardReview/icon-close.webp'

const props = defineProps({
  data: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close'])

const stats = computed(() => [
  { label: '威胁总人数', value: props.data.threatPeople },
  { label: '威胁总资产', value: props.data.threatProperty },
  { label: '稳定性现状', value: props.data.stabilityNow },
  { label: '稳定性趋势', value: props.data.stabilityForecast },
])

const infoRows = computed(() => [
  { label: '隐患点编号：', value: props.data.code },
  { label: '曾发生灾害时间：', value: props.data.disasterTime },
])
</script>

<style lang="less" scoped>
.hazard-popup {
  position: relative;
  width: 402px;
  box-sizing: border-box;
  padding: 14px;
  border: 2px solid #ffffff;
  border-radius: 10px;
  background: rgba(241, 243, 252, 0.85);
  -webkit-backdrop-filter: blur(2px);
  backdrop-filter: blur(2px);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 标题行：标题 + 等级标签在左，关闭按钮靠右 */
.popup-header {
  display: flex;
  align-items: center;
  gap: 5px;
}

.popup-title {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #222529;
  font-size: 14px;
  font-weight: 700;
  line-height: 16px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 等级标签：红色小胶囊 */
.level-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 16px;
  padding: 0 4px;
  border-radius: 4px;
  background: #e23030;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 10px;
  white-space: nowrap;
}

.close-button {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  width: 16px;
  height: 16px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;

  img {
    width: 11px;
    height: 11px;
    display: block;
    object-fit: contain;
  }
}

/* 四项指标卡片：渐变浅红底 + 竖向分隔线 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 12px;
  padding: 16px 0;
  box-sizing: border-box;
  border: 1px solid rgba(226, 48, 48, 0.2);
  border-radius: 6px;
  background: linear-gradient(180deg, #f9e6e6 0%, rgba(241, 193, 193, 0.2) 100%);
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;

  & + .stat-cell {
    border-left: 1px solid rgba(226, 48, 48, 0.2);
  }
}

.stat-label {
  color: #506073;
  font-size: 10px;
  line-height: 14px;
  white-space: nowrap;
}

.stat-value {
  color: #c54040;
  font-size: 14px;
  line-height: 14px;
  font-weight: 700;
  white-space: nowrap;
  font-family: 'Alimama FangYuanTi VF';
}

/* 编号 / 时间：两行文本，值列左对齐 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  color: #506073;
  font-size: 12px;
  line-height: 14px;
}

.info-label {
  flex: 0 0 96px;
}

.info-value {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
