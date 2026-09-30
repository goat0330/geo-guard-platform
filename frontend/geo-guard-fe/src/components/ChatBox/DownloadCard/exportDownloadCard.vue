<template>
  <div class="export-download" :style="[
     { 'margin-bottom': `${gap}px` }
  ]">
    <div class="icon" :class="`${type}-icon`" />
    <div class="title">{{ title }}</div>
    <div class="create-time">创建时间：{{ createTime }}</div>
    <img class="download" src="@/assets/imgs/chatBox/downloadCard/download-white.png" @click="download" />
  </div>
</template>

<script setup>
defineProps({
  gap: {
    type: Number,
    default: 20
  },
  type: {
    type: String,
    default: 'book'
  },
  title: {
    type: String,
    default: ''
  },
  createTime: {
    type: String,
    default: ''
  },
  download: {
    type: Function,
    default: () => {}
  }
})
</script>

<style scoped lang="less">
.export-download {
  position: relative;
  padding: 14px 60px 14px 20px;
  border-radius: 12px;
  border: 1px solid #E7E7EA;
  background: linear-gradient(98deg, #FFF -2.92%, #F5F8FF 97.73%);
  width: max-content;

  .icon {
    width: 22px;
    height: 22px;
    margin-bottom: 12px;
  }

  .title {
    font-size: 14px;
    font-weight: 500;
    color: #222529;
    margin-bottom: 8px;
  }

  .create-time {
    font-size: 12px;
    color: #878898;
  }

  .download {
    position: absolute;
    right: 20px;
    bottom: 18px;
    width: 24px;
    height: 24px;
    cursor: pointer;
  }
}

.book-icon {
  background-image: url("@/assets/imgs/chatBox/downloadCard/book.png");
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}
</style>