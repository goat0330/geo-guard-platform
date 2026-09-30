<template>
  <section class="assistant-page">
    <AgentPageHeader :title="profile.title" description="智能体问答将使用平台现有会话能力" />
    <div class="assistant-content">
      <img :src="profile.image" :alt="profile.title" class="assistant-avatar" />
      <h2>{{ profile.title }}</h2>
      <p class="assistant-description">{{ profile.description }}</p>

      <div class="question-list">
        <button v-for="question in profile.questions" :key="question" type="button" @click="startChat(question)">
          <span>{{ question }}</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <el-button class="start-button" type="primary" round @click="startChat('')">
        <el-icon><ChatLineRound /></el-icon>
        开始新对话
      </el-button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, ChatLineRound } from '@element-plus/icons-vue'
import AgentPageHeader from '@/components/AgentPageHeader/index.vue'
import { AGENT_PROFILES } from '@/utils/agentTools.js'
import { useUserStore } from '@/store/user.js'

defineOptions({ name: 'AgentAssistantPage' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = computed(() => AGENT_PROFILES[route.query.type] || AGENT_PROFILES.newcomer)

function startChat(question) {
  if (question) userStore.saveUserQuestion(question)
  router.push({ path: '/chat-engine/chatting', query: { _refresh: Date.now() } })
}
</script>

<style lang="less" scoped>
.assistant-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f6fbff 0, #ffffff 60%);
}

.assistant-content {
  width: min(520px, calc(100% - 32px));
  margin: auto;
  padding: 44px 0 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.assistant-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 8px 24px rgba(0, 123, 255, 0.14);
}

h2 {
  margin: 18px 0 8px;
  color: #222527;
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
}

.assistant-description {
  margin: 0;
  color: #617185;
  font-size: 14px;
  line-height: 24px;
}

.question-list {
  width: 100%;
  margin-top: 32px;
  display: grid;
  gap: 10px;

  button {
    width: 100%;
    min-height: 48px;
    padding: 10px 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    border: 1px solid #e1e6ee;
    border-radius: 8px;
    color: #222527;
    background: #ffffff;
    font: inherit;
    font-size: 14px;
    text-align: left;
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      color: #007bff;
      border-color: #9bc8ff;
      box-shadow: 0 6px 16px rgba(0, 64, 140, 0.08);
    }
  }
}

.start-button {
  margin-top: 28px;
  min-width: 148px;
}
</style>
