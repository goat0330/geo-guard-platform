<template>
  <div class="chat-engin-box">
    <div class="header">
      <div class="header-item" @click="openHistoryDrawer">
        <!-- <i class="iconfont icon-arrow-down" /> -->
        历史问答
      </div>
    </div>
    <div class="slogan-box">
      <div class="slogan-content">
        <div class="home-title">山河智行 人地共安</div>
        <div class="home-desc">
          <div class="home-desc-title">感知地质表象 <span>·</span> 探寻灾害真象 <span>·</span> 把握规律本象 <span>·</span> 守护众生万象</div>
          <div class="home-desc-line"></div>
        </div>
      </div>
      <div class="mascot-box">
        <div class="home-name">我是XX</div>
        <WakeAvatarVideo class="avatar" />
      </div>
    </div>

    <div class="chat-box">
      <UserInput @send="sendMsg"></UserInput>
    </div>
    <QuestionMarquee @select="sendMsg" />
    <div class="recommended-tips">为您推荐一下热门使用~</div>
    <div class="recommended-cards">
      <Card v-for="(item, index) in cardList" :key="index" :data="item" />
    </div>

    <HistoryDrawer v-model="showHistory" @select="getChatDetail" @closed="handleHistoryClosed" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Card from './components/RecommendedCard/index.vue'
import QuestionMarquee from './components/QuestionMarquee/index.vue'
import HistoryDrawer from './components/HistoryDrawer/index.vue'
import UserInput from './chatting/ChatRoom/UserInput.vue'
import WakeAvatarVideo from '@/components/WakeAvatarVideo/index.vue'
import { ASSISTANT_LIST } from '@/utils/enum'

import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const router = useRouter()

const userStore = useUserStore()
const props = defineProps({
  initialHistoryVisible: {
    type: Boolean,
    default: false,
  },
})
const emits = defineEmits(['history-closed', 'history-select'])

const showHistory = ref(props.initialHistoryVisible)

function openHistoryDrawer() {
  showHistory.value = true
}
function getChatDetail(id) {
  showHistory.value = false
  emits('history-select', id)
  router.push({
    path: '/chat-engine/chatting',
    query: {
      id,
      _refresh: Date.now(),
    },
  })
}

function sendMsg(msg, files, uploadType) {
  userStore.saveUserQuestion(msg)
  userStore.userFiles = files
  userStore.userUploadType = uploadType
  router.push({
    path: '/chat-engine/chatting',
    query: {
      _refresh: Date.now(),
    },
  })
}

function handleHistoryClosed() {
  emits('history-closed')
}

const cardList = ref(ASSISTANT_LIST)
</script>

<style lang="less" scoped>
.chat-engin-box {
  position: relative;
  width: 100%;
  height: 100%;
  background-image: url('@/assets/imgs/chatEngine/bg.png');
  background-size: cover;
  background-repeat: no-repeat;
  display: flex;
  flex-direction: column;
  align-items: center;
  // justify-content: center;

  .header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    padding: 23px 36px 0;

    .header-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #878898;
      &:hover {
        cursor: pointer;
        color: var(--el-color-primary);
      }

      img {
        width: 14px;
        height: 14px;
      }
    }
  }

  .slogan-box {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 926px;
    margin-top: 5%;
    padding-right: 60px;

    .slogan-content {
      position: relative;
      z-index: 1;
    }

    .mascot-box {
      position: relative;
      width: 260px;
      height: 188px;
      flex-shrink: 0;
    }

    .home-name {
      position: absolute;
      top: -20px;
      left: -30px;
      width: 183px;
      height: 86px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 15px 8px 0;
      background-image: url('@/assets/imgs/chatEngine/ai_avator_chat.png');
      background-repeat: no-repeat;
      background-size: 183px 86px;
      font-size: 24px;
      line-height: 28px;
      color: #222529;
      font-family: DingTalk JinBuTi;
      font-weight: 400;
      white-space: nowrap;
      box-sizing: border-box;
    }

    .avatar {
      position: absolute;
      top: -45px;
      right: -10px;
      width: 130px;
      height: 230px;
      cursor: pointer;
    }
  }

  .home-title {
    padding-left: 0px;
    font-size: 62px;
    font-weight: 500;
    margin-bottom: 12px;
    line-height: 80px;
    font-family: DingTalk JinBuTi;
    background: linear-gradient(90deg, #3667FA 0.48%, #49AAE8 50.48%, #46C4C0 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .home-desc {
    padding-left: 0px;
    margin-bottom: 32px;

    .home-desc-title {
      color: #62b2ee;
      font-size: 22px;
      line-height: 40px;
      letter-spacing: 1px;
    }

    .home-desc-line {
      width: 590px;
      height: 4px;
      margin: 4px 0 10px;
      background: linear-gradient(90deg, #4394EF 0%, #46c3c200 100%);
    }

    .home-desc-subtitle {
      color: #989aaa;
      font-size: 16px;
      line-height: 30px;
      letter-spacing: 1px;
      white-space: nowrap;

      span {
        margin: 0 5px;
        color: #9ea0ad;
      }
    }
  }

  .chat-box {
    width: 926px;
    height: 180px;
    // background: #fff;
    // padding: 20px;

    .foot-operate {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      margin-top: 10px;

      .voice {
        width: 20px;
        height: 20px;
        cursor: pointer;
      }

      .split-line {
        width: 1px;
        height: 24px;
        background: #d9d9d9;
        margin: 0 12px 0 12px;
      }

      .submit {
        width: 50px;
        height: 36px;
        border-radius: 47px;
        background: linear-gradient(130deg, #3561fa 10.99%, #44ceff 117.04%);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        user-select: none;

        img {
          width: 20px;
          height: 20px;
        }
      }
    }
  }

  .recommended-tips {
    margin-top: 80px;
    font-size: 14px;
    color: #506073;
    width: 926px;
    margin-bottom: 20px;
  }

  .recommended-cards {
    width: 926px;
    display: flex;
    flex-wrap: wrap;
    row-gap: 10px;
    column-gap: 13px;
    justify-content: space-between;
  }
}

:deep(.el-textarea__inner) {
  box-shadow: unset;
  color: #222529;
  padding: 0;
  height: 96px;
  font-size: 16px;

  &::placeholder {
    color: #878898;
  }
}

</style>
