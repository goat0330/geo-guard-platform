<template>
  <div class="phone-frame">
    <div class="phone-header">
      <slot name="title"></slot>
    </div>
    <div class="phone-inner">
      <!-- 页面内容 -->
      <div class="phone-content">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped lang="less">
.phone-frame {
  position: relative;
  width: 250px;
  height: 300px;
  background-image: url("@/assets/imgs/chatBox/phone.png");
  background-size: 100% 100%;
  background-repeat: no-repeat;
  /* 根据图片边框厚度调整内边距 */
  padding: 60px 20px 0px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.phone-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
  position: relative;
}

.phone-header {
  font-size: 12px;
  font-weight: 500;
  color: #222529;
  position: absolute;
  top: 39px;
  left: 50%;
  transform: translateX(-50%);
}

.phone-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background-color: #fff;
  
  &::-webkit-scrollbar {
    width: 0;
    display: none;
  }
}
</style>