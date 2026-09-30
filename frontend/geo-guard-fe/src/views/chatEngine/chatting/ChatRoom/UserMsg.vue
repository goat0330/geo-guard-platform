<template>
  <div class="user-msg">
    <div v-if="msg.reqFiles && msg.reqFiles.length > 0" class="msg-file">
      <div v-for="(file, index) in msg.reqFiles" :key="index" class="file-item">
        <UploadFileLoader :file="file" />
      </div>
    </div>
    <div class="user-msg__inner" v-html="msg.content"></div>
  </div>
</template>

<script setup>
import UploadFileLoader from './UploadFileLoader.vue'
const props = defineProps({
  msg: {
    type: Object,
    default: () => ({}),
  },
})
</script>

<style lang="less" scoped>
.user-msg {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;

  .msg-file {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
    flex-wrap: wrap;
  }

  &__inner {
    background: #f5f5f5;
    border-radius: 10px;
    color: #222529;
    // line-height: 17px;
    white-space: pre-line;
    max-width: calc(100% - 50px);
    padding: 9px 20px;
  }
}
</style>
