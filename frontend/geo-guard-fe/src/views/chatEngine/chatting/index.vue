<template>
  <!-- 空间分析直接渲染正式会话背景，避免历史消息初始化时闪现旧页面。 -->
  <div
    class="chat-room"
    :class="{
      'no-img-bg': isSpaceAnalysis || !(chatIsEmpty && route.query.type && !isAgentInputType),
      'is-space-analysis': isSpaceAnalysis,
    }"
  >
    <div v-if="isSpaceAnalysis" class="space-agent-header">
      <span class="space-agent-breadcrumb">
        <span class="breadcrumb-parent">智能体广场 /</span>
        <span class="breadcrumb-current">空间分析智能体</span>
      </span>
      <button type="button" @click="createNewChat"><i class="iconfont icon-plus" />新建对话</button>
    </div>
    <div v-else class="operate-btn">
      <div class="btn-common" @click="backToHome">
        <i class="iconfont icon-fanhui" />
        返回
      </div>
      <div class="spliter"></div>
      <div class="btn-common" @click="createNewChat">
        <i class="iconfont icon-plus" />
        新建对话
      </div>
    </div>
    <div class="his-btn btn-common" @click="openHistoryDrawer">
      <i class="iconfont icon-history" />
      历史问答
    </div>
    <!-- <QuesAdvisor v-if="chatIsEmpty"></QuesAdvisor> -->
    <ChatRoom v-if="showChat" ref="chatRoomRef" @msg-change="computedMsgIsEmpty"></ChatRoom>
    <HistoryDrawer v-model="showHistory" @select="getChatDetail"></HistoryDrawer>

    <!-- 免责声明 -->
    <Disclaimers ref="DisclaimersRef" @agreed-hook="handleChatEntry"></Disclaimers>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, nextTick, onActivated, watch } from 'vue'
import ChatRoom from './ChatRoom/index.vue'
import HistoryDrawer from '../components/HistoryDrawer/index.vue'
import { useUserStore } from '@/store/user'
import { useRoute, useRouter } from 'vue-router'
import Disclaimers from './Dsiclaimers.vue'
import { useLocalStorage } from '@vueuse/core'
import { CHAT_TYPE } from '@/utils/enum.js'

defineOptions({
  name: 'ChatEngineChatting',
})

const disclaimerRead = useLocalStorage('disclaimerRead', false)

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isAgentInputType = computed(() =>
  [CHAT_TYPE.IMAGE_RECOGNITION, CHAT_TYPE.MEETING_MINUTES].includes(route.query.type),
)
const isSpaceAnalysis = computed(() => route.query.type === CHAT_TYPE.SPACE_ANALYSIS)
// 从智能体广场进入时保留真实来路；直接打开聊天链接时不使用浏览器回退。
const shouldGoBackToAgentSquare = computed(() => String(window.history.state?.back || '').startsWith('/agents'))

const DisclaimersRef = ref()

const showHistory = ref(false)

const chatRoomRef = ref()
async function getChatDetail(id, shouldSyncRoute = true) {
  if (!id) return
  showHistory.value = false

  if (shouldSyncRoute && route.query.id !== `${id}`) {
    await router.replace({
      path: '/chat-engine/chatting',
      query: {
        id,
        ...(route.query.type ? { type: route.query.type } : {}),
        _refresh: Date.now(),
      },
    })
    return
  }

  chatRoomRef.value.initHistoryChat(id)
}

function openHistoryDrawer() {
  showHistory.value = true
}

const showChat = ref(true)
async function createNewChat() {
  if (route.query.type === CHAT_TYPE.SPACE_ANALYSIS) {
    router.push('/agents/space-analysis')
    return
  }
  if (isAgentInputType.value) {
    router.push('/agents')
    return
  }
  await rebuildChat()
  if (route.query.type) {
    router.push({
      path: '/chat-engine/chatting',
      query: {
        type: route.query.type,
        _refresh: Date.now(),
      },
    })
  } else {
    router.push('/chat-engine')
  }
}

async function rebuildChat() {
  return new Promise(async (resolve, reject) => {
    showChat.value = false
    await nextTick()
    showChat.value = true
    resolve()
  })
}

function backToHome() {
  if (shouldGoBackToAgentSquare.value) {
    router.go(-1)
    return
  }
  router.push(isAgentInputType.value ? '/agents' : '/chat-engine')
}

const chatIsEmpty = ref(true)
function computedMsgIsEmpty(val) {
  chatIsEmpty.value = !val
}

function mountedHook() {
  if (route.query.id) {
    getChatDetail(route.query.id, false)
    return
  }

  let question = userStore.userQuestion
  let files = userStore.userFiles || []
  let uploadType = userStore.userUploadType || ''

  if (question || files.length > 0) {
    chatRoomRef.value.sendMsg(
      question,
      files,
      uploadType,
      userStore.userCoordinates,
      userStore.userSpaceSelection,
    )
    userStore.clearUserQuestion()
    userStore.userFiles = []
    userStore.userUploadType = ''
    userStore.userCoordinates = null
    userStore.userSpaceSelection = null
  }
}

function hasPendingChatMessage() {
  return !!userStore.userQuestion || !!userStore.userFiles?.length
}

function getChatEntryKey() {
  return [route.query.id || '', route.query.type || '', route.query._refresh || ''].join('|')
}

const activeChatEntryKey = ref('')
const chatEntryInited = ref(false)
let chatEntryHandlingPromise = null

async function handleChatEntry(forceReset = false) {
  if (chatEntryHandlingPromise) return chatEntryHandlingPromise

  chatEntryHandlingPromise = doHandleChatEntry(forceReset).finally(() => {
    chatEntryHandlingPromise = null
  })
  return chatEntryHandlingPromise
}

async function doHandleChatEntry(forceReset = false) {
  if (!disclaimerRead.value) {
    DisclaimersRef.value.openDialog()
    return
  }

  const entryKey = getChatEntryKey()
  const shouldReset =
    forceReset ||
    route.query.id ||
    route.query._refresh ||
    hasPendingChatMessage() ||
    (!chatEntryInited.value && route.query.type)

  if (chatEntryInited.value && activeChatEntryKey.value === entryKey && !hasPendingChatMessage() && !forceReset) return

  if (shouldReset) {
    await rebuildChat()
    chatIsEmpty.value = true
  }

  activeChatEntryKey.value = entryKey
  chatEntryInited.value = true
  await nextTick()
  mountedHook()
}

onMounted(() => {
  handleChatEntry()
})
onActivated(() => {
  handleChatEntry()
})

watch(
  () => route.fullPath,
  () => {
    if (route.path === '/chat-engine/chatting') {
      handleChatEntry()
    }
  },
)
</script>

<style lang="less" scoped>
.chat-room {
  position: relative;
  height: 100%;
  background-color: #fff;
  border-radius: 18px 0 0 18px;
  padding: 18px;
  padding-top: 0;
  background-image: url('@/assets/imgs/chatBox/advisor-bg.png');
  background-size: auto 155px;
  background-repeat: no-repeat;
  background-position: center 0;

  :deep(.chat-content) {
    padding-top: 50px;
  }
}
.chat-room.no-img-bg {
  background: #fff !important;
  padding-top: 50px;
  :deep(.chat-content) {
    padding-top: 0;
  }
}
.chat-room.no-img-bg.is-space-analysis {
  padding-top: 96px;
}
.space-agent-header {
  position: absolute;
  top: 28px;
  left: 28px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
  color: #617185;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
}
.space-agent-breadcrumb {
  display: inline-flex;
  gap: 8px;
}
.space-agent-breadcrumb .breadcrumb-parent {
  color: #617185;
}
.space-agent-breadcrumb .breadcrumb-current {
  color: #383c41;
}
.space-agent-header button {
  padding: 0;
  border: 0;
  color: #617185;
  background: transparent;
  cursor: pointer;
  font: inherit;
}
.space-agent-header button:hover {
  color: #007bff;
}
.space-agent-header .iconfont {
  margin-right: 6px;
  font-size: 12px;
}
.operate-btn {
  display: flex;
  position: absolute;
  top: 20px;
  left: 36px;
  column-gap: 16px;
}
.spliter {
  width: 1px;
  height: 16px;
  background: #f3f3f3;
}
.his-btn {
  position: absolute;
  top: 20px;
  right: 36px;
}
.chat-room.is-space-analysis .his-btn {
  top: 28px;
  right: 28px;
  line-height: 20px;
}
.btn-common {
  font-size: 14px;
  color: #878898;
  cursor: pointer;
  i {
    font-size: 12px;
  }
  &:hover {
    color: var(--el-color-primary);
  }
}
</style>
