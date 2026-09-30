<template>
  <section
    class="space-analysis-page"
    :class="{ 'is-selecting-location': showLocation }"
  >
    <header class="page-nav">
      <span class="page-breadcrumb">
        <span class="breadcrumb-parent">智能体广场 /</span>
        <span class="breadcrumb-current">空间分析智能体</span>
      </span>
      <button v-if="showLocation" type="button" @click="newConversation">
        <i class="iconfont icon-plus" />新建对话
      </button>
      <button type="button" class="history-button" @click="openHistory">
        <i class="iconfont icon-history" />
        历史问答
      </button>
    </header>

    <div v-if="!showLocation" class="welcome-content">
      <div class="hero">
        <img class="hero-contours" :src="heroContours" alt="" />
        <div class="hero-copy">
          <h1>空间分析智能体</h1>
          <p>指一处位置，读懂地上与地下</p>
        </div>
        <div class="hero-visual">
          <span class="campaign-badge"
            ><div>百日攻坚<br />AI赋能地质灾害</div></span
          >
          <WakeAvatarVideo class="hero-mascot" :show-switch="false" />
        </div>
      </div>
      <SpaceAnalysisPrompt
        v-model="question"
        hint="立即进行空间分析"
        :disabled="isStarting"
        @select-location="openLocation"
        @submit="startAnalysis"
      />
      <div class="examples">
        <button type="button" @click="chooseExample('查看周边隐患点，地上下空间分布情况')">
          ✦ 查看周边隐患点、地上下空间分布情况 ›
        </button>
        <button type="button" @click="chooseExample('查看地质构造，地上下空间分布情况')">
          ✦ 查看地质构造、地上下空间分布情况 ›
        </button>
        <button type="button" @click="chooseExample('分析示范区地质灾害风险')">✦ 分析示范区地质灾害风险 ›</button>
      </div>
      <p class="notice">AI生成内容仅供参考，具体信息请以实际调查和专业研判为准。</p>
    </div>

    <div v-else class="analysis-content">
      <div class="analysis-scroll">
        <SpaceAnalysisLocationPanel
          ref="locationPanelRef"
          :submitting="isStarting"
          @cancel="showLocation = false"
          @confirm="confirmLocation"
        />
      </div>
      <SpaceAnalysisPrompt
        v-model="question"
        class="analysis-prompt"
        compact
        location-label="位置/范围"
        hint="立即进行空间分析"
        :disabled="isStarting"
        :selection="selectedLocation"
        @select-location="showLocation = false"
        @remove-selection="selectedLocation = null"
        @submit="startAnalysis"
      />
    </div>
    <HistoryDrawer
      v-model="showHistory"
      :chat-type="CHAT_TYPE.SPACE_ANALYSIS"
      @select="openHistoryConversation"
    />
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SpaceAnalysisLocationPanel from '@/components/SpaceAnalysisLocationPanel/index.vue'
import SpaceAnalysisPrompt from '@/components/SpaceAnalysisPrompt/index.vue'
import WakeAvatarVideo from '@/components/WakeAvatarVideo/index.vue'
import HistoryDrawer from '@/views/chatEngine/components/HistoryDrawer/index.vue'
import { useUserStore } from '@/store/user.js'
import { CHAT_TYPE } from '@/utils/enum.js'
import { buildSpaceAnalysisRequest, createSpaceAnalysisAttachment } from '@/utils/spaceAnalysis.js'
import heroContours from '@/assets/imgs/agents/agent-chat-bg.png'

defineOptions({ name: 'SpaceAnalysisPage' })
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const showLocation = ref(false)
const showHistory = ref(false)
const isStarting = ref(false)
const locationPanelRef = ref(null)
const question = ref(typeof route.query.question === 'string' ? route.query.question : '')
const selectedLocation = ref(null)

function openLocation() {
  showLocation.value = true
}

function chooseExample(text) {
  question.value = text
  openLocation()
}

function newConversation() {
  if (isStarting.value) return
  selectedLocation.value = null
  question.value = ''
  showLocation.value = false
  userStore.userFiles = []
  userStore.userUploadType = ''
  userStore.userCoordinates = null
  userStore.userSpaceSelection = null
}

function openHistory() {
  showHistory.value = true
}

async function openHistoryConversation(id) {
  if (!id) return

  await router.push({
    path: '/chat-engine/chatting',
    query: {
      id,
      type: CHAT_TYPE.SPACE_ANALYSIS,
      _refresh: Date.now(),
    },
  })
}

async function confirmLocation(selection) {
  selectedLocation.value = selection
  await startAnalysis()
}

async function startAnalysis() {
  if (isStarting.value) return
  if (!selectedLocation.value && !question.value) {
    openLocation()
    return
  }
  if (!selectedLocation.value) {
    ElMessage.info('请先选择分析的位置或范围')
    openLocation()
    return
  }
  const location = selectedLocation.value
  isStarting.value = true
  try {
    const screenshot = await createSpaceAnalysisAttachment(locationPanelRef.value, location)
    const request = buildSpaceAnalysisRequest(location, question.value)
    userStore.saveUserQuestion(request.prompt)
    userStore.userFiles = [screenshot]
    userStore.userUploadType = 'img'
    userStore.userCoordinates = request.coordinates
    userStore.userSpaceSelection = location
    await router.push({ path: '/chat-engine/chatting', query: { type: 'space_analysis', _refresh: Date.now() } })
  } catch (error) {
    console.error('空间分析地图截图上传失败', error)
    ElMessage.error('地图截图生成或上传失败，请重试')
  } finally {
    isStarting.value = false
  }
}
</script>

<style lang="less" scoped src="./index.less"></style>
