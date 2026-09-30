<template>
  <section class="knowledge-workspace">
    <iframe
      class="knowledge-workspace__frame"
      :key="userStore.token || 'signed-out'"
      src="/yuxi/index.html#/extensions?tab=knowledge"
      title="Yuxi 知识库工作台"
      loading="lazy"
    />
  </section>
</template>

<script setup>
import { watch } from 'vue'
import { useUserStore } from '@/store/user.js'

defineOptions({ name: 'KnowledgeWorkspace' })

const userStore = useUserStore()

watch(
  () => userStore.token,
  (token) => {
    if (token) {
      localStorage.setItem('user_token', token)
    } else {
      localStorage.removeItem('user_token')
    }
  },
  { immediate: true },
)
</script>

<style lang="less" scoped>
.knowledge-workspace {
  min-height: 680px;
  height: calc(100vh - 220px);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.knowledge-workspace__frame {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 680px;
  border: 0;
}
</style>
