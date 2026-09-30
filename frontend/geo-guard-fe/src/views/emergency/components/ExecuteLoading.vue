<template>
  <div class="execute-loading">
    <!-- 加载指示：8 瓣顺时针渐隐，整帧按 45° 步进旋转（设计稿导出件） -->
    <span class="loading-spinner"></span>

    <p class="loading-text">{{ text }}</p>

    <p v-if="tip" class="loading-tip">{{ tip }}</p>
  </div>
</template>

<script setup>
defineOptions({ name: 'ExecuteLoading' })

defineProps({
  /** 主文案：正在汇聚最新威胁信息、监测记录与人员数据... */
  text: {
    type: String,
    default: '',
  },
  /** 进度提示：已完成2/6个步骤，生成过程中可查看左侧执行详情 */
  tip: {
    type: String,
    default: '',
  },
})
</script>

<style lang="less" scoped>
.execute-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 加载指示：40×40；8 瓣对应 8 步，每步 45° */
.loading-spinner {
  display: block;
  width: 40px;
  height: 40px;
  background: url('@/assets/imgs/emergency/loading-spinner.svg') no-repeat center / contain;
  animation: loading-spin 0.8s steps(8, end) infinite;
}

@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 主文案：20/28 · 700 · 主色，距指示器 24px */
.loading-text {
  margin: 24px 0 0;
  color: #007bff;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
}

/* 进度胶囊：主色 16% 底、全圆角，文字 14/20 主色，距主文案 16px */
.loading-tip {
  margin: 16px 0 0;
  padding: 8px 24px;
  border-radius: 96px;
  background: rgba(0, 123, 255, 0.16);
  color: #007bff;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  text-align: center;
}
</style>
