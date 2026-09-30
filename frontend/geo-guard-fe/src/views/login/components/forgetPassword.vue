<template>
  <el-form ref="formRef" :model="form" :rules="rules">
    <el-form-item prop="phonenumber">
      <el-input v-model="form.phonenumber" placeholder="请输入手机号" />
    </el-form-item>
    <el-form-item prop="smsCode">
      <div class="code-box">
        <el-input v-model="form.smsCode" placeholder="请输入验证码" />
        <div class="send-btn" :class="{ disabled: isSendDisabled }" @click="getCaptcha">
          {{ countDown > 0 ? `${countDown}s` : '获取验证码' }}
        </div>
      </div>
    </el-form-item>
    <el-form-item prop="newPassword">
      <el-input v-model="form.newPassword" placeholder="请输入新密码" type="password" show-password />
    </el-form-item>
    <el-form-item prop="newPasswordCheck">
      <el-input v-model="form.newPasswordCheck" placeholder="请确认新密码" type="password" show-password />
    </el-form-item>
    <el-form-item>
      <div class="login-btn" @click="resetPassword">重置密码</div>
    </el-form-item>
    <el-form-item>
      <div class="login-btn normal-btn" @click="backLogin">返回登录</div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPhoneCode, resetPasswordByPhone } from '@/api/sso'
import { phoneReg } from '@/utils/validate.js'

const emits = defineEmits(['backLogin'])

const formRef = ref()
const form = ref({
  phonenumber: '',
  newPassword: '',
  newPasswordCheck: '',
  smsCode: '',
})

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!phoneReg.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const validatePasswordCheck = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认新密码'))
  } else if (value !== form.value.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = ref({
  phonenumber: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度为5到20个字符' },
  ],
  newPasswordCheck: [{ required: true, validator: validatePasswordCheck, trigger: 'blur' }],
})

const countDown = ref(0)
const isSending = ref(false)
const isSendDisabled = computed(() => countDown.value > 0 || isSending.value)
let countDownTimer = null

function clearCountDown() {
  if (countDownTimer) {
    clearInterval(countDownTimer)
    countDownTimer = null
  }
}

function startCountDown() {
  clearCountDown()
  countDown.value = 60

  countDownTimer = setInterval(() => {
    if (countDown.value > 1) {
      countDown.value--
    } else {
      clearCountDown()
      countDown.value = 0
    }
  }, 1000)
}

const validatePhoneField = () => {
  return new Promise((resolve) => {
    formRef.value.validateField('phonenumber', (valid) => {
      resolve(valid)
    })
  })
}

const getCaptcha = async () => {
  if (isSendDisabled.value) return

  const isPhoneValid = await validatePhoneField()
  if (!isPhoneValid) return

  try {
    isSending.value = true
    await getPhoneCode({ phonenumber: form.value.phonenumber })
    ElMessage.success('验证码发送成功')
    startCountDown()
  } catch (e) {
    console.error(e)
  } finally {
    isSending.value = false
  }
}

const resetPassword = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      await resetPasswordByPhone({
        phonenumber: form.value.phonenumber,
        smsCode: form.value.smsCode,
        password: form.value.newPassword,
      })
      ElMessage.success('密码重置成功')
      clearCountDown()
      countDown.value = 0
      formRef.value.resetFields()
    } catch (e) {
      console.error(e)
    }
  })
}

const backLogin = () => {
  emits('backLogin')
}

onBeforeUnmount(() => {
  clearCountDown()
})
</script>

<style scoped lang="less">
.send-btn.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
