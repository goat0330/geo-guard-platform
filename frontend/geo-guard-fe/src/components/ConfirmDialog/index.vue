<template>
  <el-dialog
    v-model="dialogVisible"
    title="Tips"
    width="500"
    top="35vh"
    modal-class="r-confirm-dialog"
    :before-close="beforeClose"
  >
    <template #header="{ close, titleId, titleClass }">
      <div class="dialog-header"><i class="iconfont icon-waring" />{{ title || '确定删除对话？' }}</div>
    </template>
    <div class="dialog-content">{{ content || '删除后，聊天记录将不可恢复。' }}</div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button v-if="deleteMode" type="danger" @click="handleConfirm">删除</el-button>
        <el-button v-else="deleteMode" type="primary" @click="handleConfirm">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  deleteMode: {
    type: Boolean,
    default: true,
  },
  title: {
    type: String,
    default: '',
  },
  content: {
    type: String,
    default: '',
  },
})
const emits = defineEmits(['update:modelValue', 'cancel', 'confirm'])
const dialogVisible = ref(false)
watch(
  () => props.modelValue,
  (val) => {
    dialogVisible.value = val
  },
  {
    immediate: true,
  },
)
function closeDialog() {
  dialogVisible.value = false
  emits('update:modelValue', false)
}
function handleCancel() {
  closeDialog()
  emits('cancel')
}

function handleConfirm() {
  closeDialog()
  emits('confirm')
}

function beforeClose(done) {
  emits('update:modelValue', false)
  done()
}
</script>

<style lang="less" scoped>
.r-confirm-dialog {
  .dialog-header {
    font-size: 16px;
    color: #222529;
    font-weight: 800;
    .iconfont {
      color: #ffa600;
      margin-right: 10px;
    }
  }

  .dialog-content {
    color: #878898;
    font-size: 14px;
  }

  .dialog-footer {
    padding-top: 4px;
    :deep(.el-button) {
      width: 84px;
      padding: 0;
      height: 32px;
      border-radius: 8px;
    }
    :deep(.el-button:first-child) {
      border: 1px solid #d4d8dd;
    }
  }
}
</style>

<style lang="less">
.r-confirm-dialog {
  .el-dialog {
    border-radius: 8px;
    padding: 20px;
  }
}
</style>
