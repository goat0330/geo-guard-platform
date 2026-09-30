<template>
  <div class="point-popup">
    <header class="popup-header">
      <span class="popup-title" :title="data.name">{{ data.name }}</span>
      <span class="level-tag">{{ data.level }}</span>
      <button class="close-button" type="button" title="关闭" @click="emit('close')">
        <img :src="closeIcon" alt="关闭" />
      </button>
    </header>

    <!-- 四项指标：一行四列；整块无指标时不渲染空色块（突发灾险情弹窗本就没有指标） -->
    <div v-if="data.metrics?.length" class="stat-grid">
      <div v-for="metric in data.metrics" :key="metric.label" class="stat-cell">
        <span class="stat-label">{{ metric.label }}</span>
        <strong class="stat-value">{{ metric.value }}</strong>
      </div>
    </div>

    <!-- 信息行：两列 -->
    <div class="info-grid">
      <div v-for="row in data.info" :key="row.label" class="info-row">
        <span class="info-label">{{ row.label }}</span>
        <span v-if="row.tag" class="info-tag">
          <el-icon class="tag-icon"><CircleCheckFilled /></el-icon>{{ row.value }}
        </span>
        <span v-else class="info-value">{{ row.value }}</span>
      </div>
    </div>

    <!-- 底部入口 -->
    <footer class="popup-footer">
      <button
        v-for="link in data.links"
        :key="link.key"
        class="result-link"
        type="button"
        @click="emit(link.event, data)"
      >
        {{ link.label }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { CircleCheckFilled } from '@element-plus/icons-vue'
import closeIcon from '@/assets/imgs/emergency/icon-close.png'

defineOptions({ name: 'EmergencyPointPopup' })

defineProps({
  /** 已有预案更新点位详情：{ name, level, metrics[], info[], links[] } */
  data: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'view-result', 'view-analysis'])
</script>

<style lang="less" scoped>
/* 已有预案更新：宽弹窗，含四项指标与两列信息 */
.point-popup {
  position: relative;
  width: 520px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 16px;
  border: 2px solid #ffffff;
  border-radius: 10px;
  background: rgba(240, 244, 255, 0.9);
  -webkit-backdrop-filter: blur(2px);
  backdrop-filter: blur(2px);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 标题行：标题 + 等级标签在左，关闭按钮靠右 */
.popup-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.popup-title {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #1f2430;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 等级标签：橙红底白字 */
.level-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: #dd4739;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;
}

.close-button {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;

  img {
    display: block;
    width: 16px;
    height: 16px;
    object-fit: contain;
  }
}

/* 四项指标：浅红渐变底 + 竖向分隔线 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 14px;
  padding: 18px 0;
  box-sizing: border-box;
  border: 1px solid rgba(221, 71, 57, 0.22);
  border-radius: 8px;
  background: linear-gradient(180deg, #fdeded 0%, #fff7f7 100%);
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;

  & + .stat-cell {
    border-left: 1px solid rgba(221, 71, 57, 0.18);
  }
}

.stat-label {
  color: #617185;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.stat-value {
  color: #dd4739;
  font-size: 16px;
  line-height: 20px;
  font-weight: 800;
  white-space: nowrap;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
}

/* 信息行：两列，数值左对齐 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px 20px;
  margin-top: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  color: #617185;
  font-size: 14px;
  line-height: 22px;
}

.info-label {
  flex-shrink: 0;
}

.info-value {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #222527;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 更新状态：带对勾的胶囊标签 */
.info-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: #e8f8ef;
  color: #21a366;
  font-size: 12px;
  line-height: 22px;
  white-space: nowrap;

  .tag-icon {
    font-size: 12px;
  }
}

/* 底部入口：通栏横线分隔，居中 */
.popup-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin: 16px -16px 0;
  padding: 14px 16px 0;
  border-top: 1px solid #e9ecf3;
}

.result-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #007bff;
  font-family: inherit;
  font-size: 14px;
  line-height: 22px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}
</style>
