<template>
  <ul v-if="monitorData.warningMessages.length" class="warning-list">
    <li v-for="item in monitorData.warningMessages" :key="item.id" class="warning-item">
      <span class="level-tag" :style="tagStyle(item.level)">
        <em>{{ item.level.slice(0, 1) }}{{ item.level.slice(1) }}</em>
        <em>告警</em>
      </span>
      <p class="warning-text">{{ item.content }}</p>
    </li>
  </ul>

  <!-- 无数据占位：与群策群防「乡镇报送排行」的空态一致（同款占位图 + 同款文案样式） -->
  <div v-else class="empty-state">
    <img :src="noDataTip" alt="暂无告警数据" />
    <span>暂无告警数据</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { monitorData } from '../useMonitorData.js'
import noDataTip from '@/assets/imgs/risk/no-data-tip.png'

defineOptions({ name: 'WarningMessageList' })

/** 告警等级与配色的映射：角标底色 16% 透明度、文字 80% 透明度 */
const levelColorMap = {
  红色: '#E45B5B',
  橙色: '#FF922C',
  黄色: '#F0B429',
  蓝色: '#007BFF',
}

const tagStyle = computed(() => (level) => {
  const color = levelColorMap[level] || levelColorMap.蓝色
  return {
    color: `${color}CC`,
    background: `${color}29`,
  }
})
</script>

<style lang="less" scoped>
.warning-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 无数据占位：样式对齐群策群防「乡镇报送排行」的空态（同一张 no-data-tip 图） */
.empty-state {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: #a6acb8;
  font-size: 14px;
}

.empty-state img {
  display: block;
  width: 320px;
  height: auto;
}

.warning-item {
  display: flex;
  gap: 8px;
  padding: 12px;
  box-sizing: border-box;
  background: #f5f9fc;
  border-radius: 8px;
}

.level-tag {
  display: flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 400;
  line-height: 14px;

  em {
    font-style: normal;
  }
}

.warning-text {
  display: -webkit-box;
  flex: 1 1 0;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  color: #9096a2;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>
