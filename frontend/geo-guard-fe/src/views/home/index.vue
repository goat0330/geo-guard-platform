<template>
  <div class="home-page">
    <!-- 右上角：历史问答入口 -->
    <div class="top-action-bar">
      <div class="history-btn" @click="handleHistoryClick">
        <img src="@/assets/imgs/home/icon-history.png" class="history-icon" alt="历史问答" />
        <span>历史问答</span>
      </div>
    </div>

    <!-- 顶部 Hero 区域（主标题、AI搜索卡片、快捷提问走马灯） -->
    <HomeHeroSection
      v-model="searchQuery"
      @search="handleSearch"
      @voice="handleVoiceInput"
    />

    <!-- 模块一：今日业务看板（指标 2x2 与 4列分类摘要） -->
    <HomeBusinessDashboard />

    <!-- 模块二：智能体广场（6大智能体推荐卡片与快捷操作） -->
    <HomeAgentSquare
      @agent-click="handleAgentClick"
      @action-click="handleAgentAction"
      @more="handleMoreAgents"
    />

    <!-- 历史对话抽屉 -->
    <HistoryDrawer v-model="showHistory" @select="getChatDetail" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HomeHeroSection from '@/components/HomeHeroSection/index.vue'
import HomeBusinessDashboard from '@/components/HomeBusinessDashboard/index.vue'
import HomeAgentSquare from '@/components/HomeAgentSquare/index.vue'
import HistoryDrawer from '@/views/chatEngine/components/HistoryDrawer/index.vue'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'HomePage' })

const router = useRouter()
const userStore = useUserStore()

const searchQuery = ref('')
const showHistory = ref(false)

/**
 * 发起搜索提问并跳转至二级问答页面
 * @param {string} [customQuery] 可选自定义提问内容
 */
function handleSearch(customQuery) {
  const query = typeof customQuery === 'string' ? customQuery : searchQuery.value
  if (!query || !query.trim()) {
    ElMessage.warning('请输入你想问的问题')
    return
  }

  // 将提问内容存入全局状态
  userStore.saveUserQuestion(query.trim())
  userStore.userFiles = []
  userStore.userUploadType = ''

  router.push({
    path: '/chat-engine/chatting',
    query: {
      _refresh: Date.now(),
    },
  })
}

function handleVoiceInput() {
  ElMessage.info('正在开启语音输入模式...')
  router.push({
    path: '/chat-engine/chatting',
    query: {
      voice: '1',
      _refresh: Date.now(),
    },
  })
}

function handleHistoryClick() {
  showHistory.value = true
}

function getChatDetail(id) {
  showHistory.value = false
  router.push({
    path: '/chat-engine/chatting',
    query: {
      id,
      _refresh: Date.now(),
    },
  })
}

function handleMoreAgents() {
  router.push('/agents')
}

function handleAgentClick(agent) {
  if (agent.type === 'space_analysis') {
    router.push('/agents/space-analysis')
    return
  }
  if (agent.type) {
    router.push({ path: '/chat-engine/chatting', query: { type: agent.type } })
    return
  }
  handleSearch(`@${agent.title} 你好，请介绍一下你的核心功能与应用场景`)
}

function handleAgentAction({ agent, action }) {
  if (agent.type === 'image_recognition') {
    router.push({ path: '/chat-engine/chatting', query: { type: agent.type } })
    return
  }
  if (agent.type === 'space_analysis') {
    router.push({ path: '/agents/space-analysis', query: { question: action } })
    return
  }
  handleSearch(`@${agent.title} ${action}`)
}
</script>

<style lang="less" scoped>
.home-page {
  padding: 22px 42px 0;
  box-sizing: border-box;
  background: #ffffff;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 右上角历史按钮 */
.top-action-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0;

  .history-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #666666;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: 8px;
    transition: all 0.2s;

    .history-icon {
      display: none;
    }

    &:hover {
      color: #2a6ff7;
      background: #f1f5f9;
    }
  }
}
</style>
