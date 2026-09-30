<template>
  <div class="page-wrap">
    <div class="content-wrap">
      <img src="@/assets/imgs/404.png" class="img-404" alt="页面开发中" />
      <div class="tip-text">
        <div class="main-text">页面正在开发中...</div>
        <div class="sub-text">更多功能即将上线，敬请期待</div>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineOptions({ name: 'DevelopmentPage' })

const router = useRouter()

function goHome() {
  router.push('/home')
}
</script>

<style lang="less" scoped>
.page-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 520px;
  background: #fff;

  .content-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 100px;
  }

  .img-404 {
    width: 353px;
    height: 252px;
    margin-right: 66px;
    object-fit: contain;
  }

  .tip-text {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding-top: 48px;
  }

  .main-text {
    margin-bottom: 20px;
    color: #222527;
    font-size: 32px;
    font-weight: 600;
  }

  .sub-text {
    margin-bottom: 60px;
    color: #9096a2;
    font-size: 20px;
  }

  :deep(.el-button) {
    height: 36px;
    padding: 0 16px;
    border-radius: 4px;
  }
}
</style>
