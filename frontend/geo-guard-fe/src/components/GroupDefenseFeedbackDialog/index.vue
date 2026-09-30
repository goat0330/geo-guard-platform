<template>
  <el-dialog
    :model-value="modelValue"
    title="提交反馈"
    width="440px"
    destroy-on-close
    @close="closeDialog"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="反馈内容" prop="suggestionContent">
        <el-input
          v-model="form.suggestionContent"
          type="textarea"
          :rows="4"
          placeholder="请输入反馈内容"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="closeDialog">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">提交反馈</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'

defineOptions({ name: 'GroupDefenseFeedbackDialog' })

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  report: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'submit'])
const formRef = ref(null)
const form = reactive({ suggestionContent: '' })
const rules = {
  suggestionContent: [
    {
      validator: (_, value, callback) => {
        if (!value?.trim()) {
          callback(new Error('请输入反馈内容'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

watch(() => props.modelValue, (visible) => {
  if (!visible) return
  form.suggestionContent = ''
})

const closeDialog = () => {
  if (props.submitting) return
  emit('update:modelValue', false)
}

const submitForm = async () => {
  // 接口仅接收报灾记录 ID 和反馈内容。
  if (props.report?.id === null || props.report?.id === undefined || props.report?.id === '' || props.submitting) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', {
    reportId: props.report.id,
    suggestionContent: form.suggestionContent.trim(),
  })
}
</script>

<style lang="less" scoped>
:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__title) {
  color: #222527;
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-form-item__label) {
  color: #617185;
}
</style>
