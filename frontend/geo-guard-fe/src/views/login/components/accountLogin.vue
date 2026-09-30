<template>
  <el-form ref="formRef" :model="form" :rules="rules">
    <el-form-item prop="username">
      <el-input v-model="form.username" placeholder="请输入账号">
        <template #prefix>
          <i class="iconfont icon-username input-icon" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item prop="password">
      <el-input v-model="form.password" placeholder="请输入密码" type="password">
        <template #prefix>
          <i class="iconfont icon-password input-icon" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item prop="code">
      <div class="code-box">
        <el-input v-model="form.code" placeholder="请输入验证码" @keydown="keydown">
          <template #prefix>
            <i class="iconfont icon-secret input-icon" />
          </template>
        </el-input>
        <div class="send-btn">
          <ValidCode ref="validRef" @getUuid="getUuid" />
        </div>
      </div>
    </el-form-item>
    <el-form-item>
      <div class="check-box">
        <el-checkbox v-model="noLogin">七天免登录</el-checkbox>
        <div class="forget" @click="changePassword">忘记密码</div>
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
import { ref } from 'vue'
import ValidCode from '@/views/login/components/validCode.vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user.js'

const emits = defineEmits(['changePassword', 'loginSuccess'])
const userStore = useUserStore()

const form = ref({
  username: '',
  password: '',
  grantType: 'password',
  code: '',
  uuid: '',
})
const formRef = ref()
const validRef = ref()
const rules = ref({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
})
const noLogin = ref(false)
const isLoggingIn = ref(false)

const getUuid = (uuid) => {
  form.value.uuid = uuid
}

const login = () => {
  if (isLoggingIn.value) return
  formRef.value.validate(async (valid) => {
    if (valid) {
      const params = Object.assign(
        {
          name: form.value.username,
          pwd: form.value.password,
        },
        form.value,
      )
      try {
        isLoggingIn.value = true
        await userStore.login(params)
        ElMessage.success('登录成功')
        emits('loginSuccess')
      } catch (e) {
        console.error(e)
        await validRef.value?.getStringCaptcha?.()
        form.value.code = ''
        form.value.password = ''
        formRef.value.validateField('password')
      } finally {
        isLoggingIn.value = false
      }
    }
  })
}

const keydown = (e) => {
  if (e.keyCode === 13) {
    e.preventDefault()
    login()
  }
}

const changePassword = () => {
  emits('changePassword')
}
</script>

<style scoped lang="less">
.input-icon {
  color: #617185;
  margin-right: 12px;
}
</style>
