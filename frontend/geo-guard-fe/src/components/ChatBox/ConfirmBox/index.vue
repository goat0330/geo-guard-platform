<template>
  <div
    class="confirm-box"
    :style="{ 'margin-bottom': `${gap}px` }"
  >
    <div class="confirm-box-title">{{ title }}</div>
    <div class="btn-list">
      <el-button v-if="useCancel" class="btn-item" @click="selectBtn('cancel')" :disabled="hasClick">{{ cancelBtnText }}</el-button>
      <el-button type="primary" class="btn-item btn-item-active" @click="selectBtn('confirm')" :disabled="hasClick">{{ confirmBtnText }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { once } from 'lodash-es'
import { ref, watch } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: '是否执行？',
  },
  useCancel: {
    type: Boolean,
    default: true,
  },
  cancelBtnText: {
    type: String,
    default: '我再想想',
  },
  confirmBtnText: {
    type: String,
    default: '确认',
  },
  gap: {
    type: Number,
    default: 20,
  },
  confirm: {
    type: Function,
    default: () => {},
  },
  cancel: {
    type: Function,
    default: () => {},
  },
  clickStatus: {
    type: Boolean,
    default: false
  }
})

const hasClick = ref(false)
const selectBtn = once((type) => {
  hasClick.value = true
  type == 'confirm' ? props?.confirm() : props?.cancel()
})

watch(
  () => props.clickStatus,
  (status) => {
    hasClick.value = status
  },
  { deep: true, immediate: true }
)
</script>

<style scoped lang="less">
.confirm-box {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #e7e7ea;

  .confirm-box-title {
    font-size: 14px;
    text-align: center;
    font-weight: 800;
    color: #222529;
    margin-bottom: 12px;
    line-height: 26px;
  }

  .btn-list {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 20px;

    .btn-item {
      width: 220px;
      height: 32px;
      border-radius: 6px;
      line-height: 26px;
      padding: 3px 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: #222529;
    }

    .btn-item-active {
      color: #fff;
    }
  }
}
</style>