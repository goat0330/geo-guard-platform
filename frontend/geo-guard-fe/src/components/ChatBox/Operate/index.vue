<template>
  <div
    class="operate-box"
    :style="[
      { 'margin-bottom': `${gap}px` }
    ]"
  >
    <div v-if="title" class="operate-title">
      <img :src="icon" />
      <span>{{ title }}</span>
    </div>
    <div class="operate-list">
      <div
        v-for="(item, index) in operateList"
        class="operate-item"
        :key="index"
        @click="chooseItem(item)"
      >
        <div v-if="item?.tip" class="tips">#{{ item?.tip }}</div>
        <div class="content">{{ item?.content }}</div>
        <img src="@/assets/imgs/chatBox/operate/right.svg?url" />
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  // { tip: '', content: '' }
  operateList: {
    type: Array,
    default: () => [
    ]
  },
  gap: {
    type: Number,
    default: 20
  },
  chooseItem: {
    type: Function,
    default: () => {}
  }
})

const chooseItem = (item) => {
  props.chooseItem(item)
}
</script>

<style scoped lang="less">
.operate-box {
  width: 100%;
  display: flex;
  flex-direction: column;

  .operate-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    font-size: 14px;
    color: #222529;

    img {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      overflow: hidden;
    }
  }

  .operate-list {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .operate-item {
      width: max-content;
      display: flex;
      align-items: center;
      padding: 10px 20px 10px 10px;
      background: #F5F7FA;
      border-radius: 8px;
      cursor: pointer;

      .tips {
        font-size: 12px;
        color: #3561FA;
        margin-right: 8px;
      }

      .content {
        font-size: 12px;
        color: #878898;
        flex: 1;
        margin-right: 8px;
      }

      img {
        width: 12px;
        height: 12px;
      }
    }
  }
}
</style>