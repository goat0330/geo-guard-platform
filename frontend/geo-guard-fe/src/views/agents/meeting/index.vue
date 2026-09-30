<template>
  <div></div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CHAT_TYPE } from '@/utils/enum.js'

defineOptions({ name: 'AgentMeetingPage' })

const router = useRouter()

onMounted(() => {
  router.replace({
    path: '/chat-engine/chatting',
    query: { type: CHAT_TYPE.MEETING_MINUTES, _refresh: Date.now() },
  })
})
</script>
