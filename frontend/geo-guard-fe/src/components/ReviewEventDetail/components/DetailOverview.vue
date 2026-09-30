<template>
  <section class="detail-section overview-section">
    <div class="section-title">
      <h3>📌 事件概述</h3>
    </div>
    <div class="overview-card message-enter">
      <p class="description-text">{{ description || '--' }}</p>
    </div>
  </section>
</template>

<script setup>
defineOptions({ name: 'DetailOverview' })

defineProps({
  description: {
    type: String,
    default: '',
  },
})
</script>

<style lang="less" scoped>
.overview-section {
  margin-top: 4px;
}

.section-title {
  margin-bottom: 10px;

  h3 {
    margin: 0;
    color: #383c41;
    font-size: 16px;
    font-weight: 700;
    line-height: 20px;
  }
}

.overview-card {
  width: 100%;
  padding: 12px;
  box-sizing: border-box;
  border-radius: 6px;
  background: #f5f7fa;

  .description-text {
    margin: 0;
    color: #617185;
    font-size: 14px;
    font-weight: 400;
    line-height: 22px;
  }
}

.message-enter {
  animation: message-enter 0.24s ease both;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
