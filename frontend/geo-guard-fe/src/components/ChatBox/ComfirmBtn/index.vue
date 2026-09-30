<template>
  <div v-if="show" class="confirm-btn"
       :class="[`btn-${size}`, `theme-${theme}`]"
       :style="{ 'margin-bottom': `${gap}px` }"
       @click="confirm(title)"
  >
    <div class="confirm-btn-text">{{ title }}</div>
    <i class="iconfont icon-arrow-right"></i>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  confirm: {
    type: Function,
    default: () => {}
  },
  gap: {
    type: Number,
    default: 20
  },
  size: {
    type: String,
    default: 'normal'
  },
  theme: {
    type: String,
    default: 'normal'
  },
  show: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped lang="less">
.confirm-btn {
  display: flex;
  align-items: center;
  border-radius: 12px;
  cursor: pointer;
  width: max-content;

  .confirm-btn-text {
    line-height: 26px;
    margin-right: 10px;
  }
  
  img {
    width: 14px;
    height: 14px;
  }
}

.btn-normal {
  padding: 8px 20px;
  font-size: 14px;

  .icon-arrow-right {
    font-size: 14px;
  }
}

.btn-small {
  padding: 3px 10px;
  font-size: 12px;

  .icon-arrow-right {
    font-size: 12px;
  }
}

.theme-normal {
  color: #3561FA;
  background: #E4F0FF;
}

.theme-gray {
  color: #878898;
  background: #F5F7FA;
}
</style>