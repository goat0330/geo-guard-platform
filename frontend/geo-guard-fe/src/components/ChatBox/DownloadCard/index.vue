<template>
  <div
    class="download-card"
    :style="[
      { 'margin-bottom': `${gap}px` }
    ]"
    @click.stop="download(false)"
  >
    <div class="icon" :class="`${type}-icon`"></div>
    <div class="download-content">
      <div class="download-title">{{ downloadData?.title }}</div>
      <div class="create-time">创建时间  {{ downloadData?.createTime }}</div>
    </div>
    <img class="download-icon" src="@/assets/imgs/chatBox/downloadCard/download.svg?url" @click.stop="download(true)" />
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
    default: 'docx'
  },
  // { title: '', createTime: '', link: '' }
  downloadData: {
    type: Object,
    default: () => ({})
  },
  download: {
    type: Function,
    default: () => () => {}
  }
})
</script>

<style scoped lang="less">
.download-card {
  border-radius: 12px;
  border: 1px solid #E7E7EA;
  background: #FBFBFF;
  padding: 12px 20px 12px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  width: max-content;

  .icon {
    width: 39px;
    height: 39px;
  }

  .download-content {
    display: flex;
    flex-direction: column;
    gap: 5px;
    flex: 1;

    .download-title {
      font-size: 12px;
      font-weight: 800;
    }

    .create-time {
      font-size: 12px;
    }
  }

  .download-icon {
    width: 16px;
    height: 16px;
    cursor: pointer;
  }
}

.docx-icon {
  background-image: url("@/assets/imgs/chatBox/downloadCard/docx.png");
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}
</style>