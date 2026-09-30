<template>
  <div class="login-wrap">
    <div class="login-wrap__inner">
      <img src="@/assets/imgs/login/login-banner.png" alt="Banner" class="login-banner" />
      <div class="login-box">
        <div class="project-info">
          <div class="logo">
            <img src="@/assets/imgs/logo.png" alt="Logo" />
          </div>
          <div class="title">
            <div class="cn-title">重庆地质灾害防治智能体</div>
          </div>
        </div>
        <div class="login-card">
          <div class="login-title">欢迎登录</div>
          <div class="submit-form">
            <component
              :is="component"
              @changePassword="changePassword"
              @backLogin="backLogin"
              @loginSuccess="loginSuccess"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef } from 'vue'
import AccountLogin from './components/accountLogin.vue'
import PhoneLogin from './components/phoneLogin.vue'
import ForgetPassword from './components/forgetPassword.vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user.js'

const activeTab = ref(1)
const router = useRouter()
const userStore = useUserStore()

const component = shallowRef(AccountLogin)
const left = ref('0px')

const changeTab = (tab) => {
  activeTab.value = tab
  if (activeTab.value === 1) {
    component.value = AccountLogin
    left.value = '0px'
  } else {
    component.value = PhoneLogin
    left.value = '50%'
  }
}

const backLogin = () => {
  activeTab.value = 1
  component.value = AccountLogin
}

const changePassword = () => {
  activeTab.value = 1
  component.value = ForgetPassword
}

const loginSuccess = () => {
  try {
    userStore.startConnection()
    userStore.startHeartbeat()
  } catch (e) {
    console.warn('初始化心跳或SSE连接异常', e)
  }
  const redirect = router.currentRoute.value.query?.redirect || '/'
  router.push(redirect)
}
</script>

<style scoped lang="less">
.login-wrap {
  min-width: 1366px;
  min-height: 768px;
  height: 100vh;
  background-image: url('@/assets/imgs/login/login-bg.png');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;

  &__inner {
    display: flex;
    height: 57.5vh;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 40px 0 rgba(140, 148, 183, 0.1);
    border-radius: 18px;
    overflow: hidden;
  }
}

.login-banner {
  object-fit: cover;
  height: 100%;
}

.login-box {
  height: 100%;
  background-size: cover;
  background-repeat: no-repeat;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  background-color: #ffffff;

  .project-info {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40px;
    overflow: hidden;

    .logo {
      width: 36px;
      height: 36px;
      flex-shrink: 0;
      margin-right: 10px;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 100%;
        height: 100%;
        display: block;
      }
    }

    .title {
      color: #007bff;
      font-size: 26px;
      font-family: 'Alimama FangYuanTi VF', sans-serif;
      font-weight: bold;
      white-space: nowrap;
      line-height: 1.1;
    }

    .en-title {
      margin-bottom: 4px;
    }
  }

  .login-card {
    width: 560px;
    padding: 0 60px;
    border-radius: 18px;
    background: #ffffff;

    .login-title {
      font-size: 24px;
      font-weight: 600;
      color: #222527;
      margin-bottom: 48px;
    }

    .tab-list {
      position: relative;
      display: flex;
      width: 100%;
      border-bottom: 1px solid #dfe4f0;
      padding-bottom: 10px;
      cursor: pointer;

      .tab-item {
        width: 50%;
        font-size: 16px;
        color: #222527;
        text-align: center;
      }

      .tab-item-active {
        color: #007bff;
        font-weight: 600;
      }

      .tab-line {
        position: absolute;
        left: 0;
        top: 100%;
        width: 50%;
        height: 2px;
        background: #007bff;
        transition: all 0.3s ease;
      }
    }

    .submit-form {
      margin-top: 24px;
    }
  }
}

:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 6px;
  border: 1px solid #dfe4f0;
  font-size: 16px;
}
</style>

<style lang="less">
.login-box .submit-form {
  .code-box {
    width: 100%;
    position: relative;

    .el-input__wrapper {
      justify-content: flex-start;
    }

    .el-input__inner {
      max-width: calc(100% - 110px) !important;
    }

    .send-btn {
      cursor: pointer;
      position: absolute;
      right: 8px;
      top: 50%;
      transform: translateY(-50%);
      color: #222527;
      font-size: 14px;
      font-family: 'Alimama FangYuanTi VF', sans-serif;
      width: 100px;
      height: 32px;
      border-radius: 4px;
      background: #d4d4d4;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
  }

  .check-box {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #222527;
    width: 100%;

    .forget {
      color: #007bff;
      margin-left: auto;
      cursor: pointer;
    }
  }

  .login-btn {
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 600;
    color: #ffffff;
    width: 100%;
    height: 44px;
    border-radius: 6px;
    background: linear-gradient(130deg, #007bff 10.99%, #44ceff 117.04%);
    user-select: none;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.9;
    }

    &.is-loading {
      cursor: wait;
      opacity: 0.75;
      pointer-events: none;
    }

    .el-icon {
      margin-right: 8px;
      font-size: 16px;
    }
  }

  .normal-btn {
    border: 1px solid #007bff;
    background: #ffffff;
    color: #007bff;

    &:hover {
      background: #dcedff;
    }
  }
}
</style>
