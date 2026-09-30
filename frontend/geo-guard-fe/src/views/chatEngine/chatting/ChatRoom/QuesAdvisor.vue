<template>
  <div class="question-box">
    <div class="intro-bg"></div>

    <div class="question-box__inner">
      <div class="agent-intro">
        <img :src="agentAvatar" alt="" class="avatar" />
        <div class="agent-des">
          <div>
            你好，我是<span class="agent-type">{{ agentInfo.name }}小助手</span>
          </div>
          <div>欢迎随时向我提问哦~</div>
        </div>
      </div>

      <div>
        <div class="ques-title">{{ agentInfo.selfIntro }}</div>
        <div class="ques-info">
          <div v-for="q in questions" :key="q" class="each-info" @click="sendQuestion(q)">
            <div class="each-info__inner">
              <i class="iconfont icon-start" />{{ q }}<i class="iconfont icon-arrow-right" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeMount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ASSISTANT_LIST, CHAT_TYPE } from '@/utils/enum'
import defaultAgentAvatar from '@/assets/imgs/chatBox/avatar-half.png'
import spaceAnalysisAvatar from '@/assets/imgs/ai-avatar.png'
const route = useRoute()

const agentInfo = ref({})
const questions = ref([])
const agentAvatar = computed(() =>
  route.query.type === CHAT_TYPE.SPACE_ANALYSIS ? spaceAnalysisAvatar : defaultAgentAvatar,
)

const emits = defineEmits(['send'])

function sendQuestion(q) {
  emits('send', q)
}

// 随机抽取数组元素
function getRandomElInArray(arr) {
  if (arr.length < 3) {
    return arr
  }
  const shuffled = [...arr]
  const result = []
  for (let i = 0; i < 3; i++) {
    const randomIndex = Math.floor(Math.random() * (shuffled.length - i)) + i
    ;[shuffled[i], shuffled[randomIndex]] = [shuffled[randomIndex], shuffled[i]]
    result.push(shuffled[i])
  }
  return result
}

onBeforeMount(() => {
  const { type } = route.query
  if (type) {
    const target = ASSISTANT_LIST.find((v) => v.type === type)
    agentInfo.value = target || {
      name: '智能',
      selfIntro: '请描述您的问题，我会尽力协助您。',
    }
    questions.value = getRandomElInArray(target?.questions || [])
  }
})
</script>

<style lang="less" scoped>
.question-box {
  position: absolute;
  width: 100%;
  // top: -30px;
  display: flex;
  padding-left: 50px;
  z-index: 10;
  .question-box__inner {
    // width: 75%;
  }

  .agent-intro {
    // width: 100%;
    display: flex;
    height: 105px;
    align-items: flex-end;
  }
  .agent-des {
    font-family: 'Alimama FangYuanTi VF';
    font-size: 22px;
    color: #222529;
    font-weight: 800;
    height: 90px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
  .agent-type {
    color: var(--el-color-primary);
  }
  .avatar {
    width: 80px;
    height: 104px;
    margin-right: 20px;
    object-fit: contain;
    object-position: center bottom;
  }
  .ques-title {
    margin: 40px 0 20px 0;
    color: #878898;
    font-size: 14px;
  }
  .each-info {
    display: flex;
    line-height: 42px;
    margin-bottom: 13px;
    cursor: pointer;
    align-items: center;
    &:hover {
      color: var(--el-color-primary);
    }
    &__inner {
      background: #f5f7fa;
      padding-left: 10px;
      padding-right: 20px;
      border-radius: 8px;
    }
    .iconfont {
      color: #3561fa;
      margin-right: 8px;
    }
    .arrow-icon {
      margin-top: 5px;
      margin-left: 20px;
    }
  }
}


.each-info__inner {
  //display: flex;
  //align-items: center;
}
</style>
