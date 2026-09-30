<template>
  <header class="agent-page-header">
    <el-button class="back-button" circle :icon="ArrowLeft" aria-label="返回智能体广场" @click="back" />
    <div class="header-content">
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="header-extra"><slot /></div>
  </header>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

defineOptions({ name: 'AgentPageHeader' })

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})

const router = useRouter()

function back() {
  router.push('/agents')
}
</script>

<style lang="less" scoped>
.agent-page-header {
  min-height: 72px;
  padding: 16px 28px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) minmax(40px, auto);
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #edf0f5;
  background: #ffffff;
}

.back-button {
  width: 36px;
  height: 36px;
  color: #617185;
}

.header-content {
  min-width: 0;

  h1 {
    margin: 0;
    color: #222527;
    font-size: 18px;
    line-height: 26px;
    font-weight: 600;
  }

  p {
    margin: 2px 0 0;
    color: #9096a2;
    font-size: 12px;
    line-height: 18px;
  }
}

.header-extra {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .agent-page-header {
    padding: 12px 16px;
  }
}
</style>
