<template>
  <div class="generate-popup">
    <header class="popup-header">
      <span class="popup-title" :title="data.name">{{ data.name }}</span>
      <span class="status-tag">
        <el-icon class="status-icon"><CircleCheckFilled /></el-icon>{{ data.level }}
      </span>
      <button class="close-button" type="button" title="关闭" @click="emit('close')">
        <img :src="closeIcon" alt="关闭" />
      </button>
    </header>

    <!-- 更新时间：单行 -->
    <p class="update-row">
      <span class="update-label">更新时间：</span>
      <span class="update-value">{{ data.updateTime }}</span>
    </p>

    <!-- 底部入口：通栏横线分隔，居中，多个入口之间用竖线分隔 -->
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

defineOptions({ name: 'EmergencyGeneratePopup' })

defineProps({
  /** 突发灾情预案生成点位详情：{ name, level, updateTime, links[] } */
  data: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'view-result', 'view-analysis'])
</script>

<style lang="less" scoped>
/* 突发灾情预案生成：窄弹窗，仅状态 + 更新时间 + 入口 */
.generate-popup {
  position: relative;
  width: 344px;
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

/* 状态标签：浅绿底 + 绿字 + 对勾 */
.status-tag {
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
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;

  .status-icon {
    font-size: 14px;
  }
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

.update-row {
  display: flex;
  align-items: center;
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 22px;
}

.update-label {
  flex-shrink: 0;
  color: #617185;
}

.update-value {
  color: #222527;
}

.popup-footer {
  display: flex;
  justify-content: center;
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

  /* 多个入口之间以竖线分隔 */
  & + .result-link {
    position: relative;
    margin-left: 21px;

    &::before {
      position: absolute;
      top: 50%;
      left: -11px;
      width: 1px;
      height: 16px;
      background: #e9ecf3;
      content: '';
      transform: translateY(-50%);
    }
  }
}
</style>
