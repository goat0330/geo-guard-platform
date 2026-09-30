<template>
  <el-form ref="formRef" :model="form" :rules="rules">
    <el-form-item prop="phonenumber">
      <el-input v-model="form.phonenumber" placeholder="请输入手机号码" />
    </el-form-item>
    <el-form-item prop="smsCode">
      <div class="code-box">
        <el-input v-model="form.smsCode" placeholder="请输入验证码" />
        <div class="send-btn" :class="{ disabled: isSendDisabled }" @click="getCaptcha">
          {{ countDown > 0 ? `${countDown}s` : '获取验证码' }}
        </div>
      </div>
    </el-form-item>
    <el-form-item>
      <div class="check-box">
        <el-checkbox v-model="noLogin">七天免登录</el-checkbox>
      </div>
    </el-form-item>
    <el-form-item>
      <div class="login-btn" :class="{ 'is-loading': isLoggingIn }" @click="login">
        <el-icon v-if="isLoggingIn" class="is-loading"><Loading /></el-icon>
        {{ isLoggingIn ? '登录中' : '登录' }}
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { phoneReg } from '@/utils/validate.js'
import { getPhoneCode } from '@/api/sso.js'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user.js'

const emits = defineEmits(['loginSuccess'])
const userStore = useUserStore()

const formRef = ref()
const form = ref({
  phonenumber: '',
  grantType: 'sms',
  smsCode: '',
})
const noLogin = ref(false)
const isLoggingIn = ref(false)

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!phoneReg.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules = ref({
  phonenumber: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
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

const login = () => {
  if (isLoggingIn.value) return
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        isLoggingIn.value = true
        await userStore.login(form.value)
        ElMessage.success('登录成功')
        emits('loginSuccess')
      } catch (e) {
        console.error(e)
      } finally {
        isLoggingIn.value = false
      }
    }
  })
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
