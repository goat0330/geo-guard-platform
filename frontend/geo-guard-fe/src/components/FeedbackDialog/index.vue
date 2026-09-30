<template>
  <el-dialog
    :model-value="modelValue"
    title="提交人工反馈"
    width="440px"
    destroy-on-close
    @close="closeDialog"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="人工风险等级" prop="manualRiskLevel">
        <el-select v-model="form.manualRiskLevel" placeholder="请选择风险等级">
          <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="人工修正备注" prop="manualRiskRemark">
        <el-input
          v-model="form.manualRiskRemark"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入人工核查结论或修正原因"
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
import { RISK_LEVEL_TEXT } from '@/utils/enum.js'

/**
 * 人工反馈弹窗：人工风险等级 + 人工修正备注，交互与群策群防页面的提交反馈一致，
 * 供各页面的「提交反馈」按钮复用（群策群防、隐患复核、监测预警）
 */
defineOptions({ name: 'FeedbackDialog' })

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 反馈对象：{ id, manualRiskLevel, aiRiskLevel, manualRiskRemark }，打开时用其现有值回填 */
  subject: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'submit'])
const formRef = ref(null)
const form = reactive({ manualRiskLevel: null, manualRiskRemark: '' })
const rules = {
  manualRiskLevel: [{ required: true, message: '请选择人工风险等级', trigger: 'change' }],
  manualRiskRemark: [{ required: true, message: '请输入人工修正备注', trigger: 'blur' }],
}
/* 风险等级去掉了「无」，保留低 / 中 / 高 / 极高 */
const riskOptions = Object.entries(RISK_LEVEL_TEXT)
  .filter(([value]) => Number(value) > 0)
  .map(([value, label]) => ({ value: Number(value), label: `${label}风险` }))

watch(() => props.modelValue, (visible) => {
  if (!visible) return
  form.manualRiskLevel = props.subject?.manualRiskLevel ?? props.subject?.aiRiskLevel ?? null
  form.manualRiskRemark = props.subject?.manualRiskRemark ?? ''
})

const closeDialog = () => {
  if (props.submitting) return
  emit('update:modelValue', false)
}

const submitForm = async () => {
  /* 反馈对象缺 id 时不提交，避免发出无法归属的记录 */
  if (!props.subject?.id || props.submitting) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', {
    id: props.subject.id,
    manualRiskLevel: form.manualRiskLevel,
    manualRiskRemark: form.manualRiskRemark.trim(),
  })
}
</script>

<style lang="less" scoped>
:deep(.el-select) {
  width: 100%;
}

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
