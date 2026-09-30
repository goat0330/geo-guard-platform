<template>
  <div class="history-route">
    <ChatEngineHome :initial-history-visible="true" @history-select="markSelected" @history-closed="backToHome" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ChatEngineHome from '../index.vue'

const router = useRouter()
const selectedHistory = ref(false)

function backToHome() {
  if (selectedHistory.value) return
  router.replace('/chat-engine')
}

function markSelected() {
  selectedHistory.value = true
}
</script>

<style lang="less" scoped>
.history-route {
  width: 100%;
  height: 100%;
}
</style>
