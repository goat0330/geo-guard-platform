<template>
  <div class="hero-section">
    <!-- 平台主标题 -->
    <div class="title-wrapper">
      <h1 class="main-title">
        <img class="chongqing-logo" :src="chongqingLogo" alt="重庆" />
        <span>地质灾害防治智能体</span>
      </h1>
    </div>

    <!-- 副标题 -->
    <div class="sub-title">
      <span>监测预警</span>
      <span class="dot">·</span>
      <span>会商研判</span>
      <span class="dot">·</span>
      <span>应急处置</span>
      <span class="dot">·</span>
      <span>复盘优化</span>
    </div>

    <!-- AI 搜索提问卡片 -->
    <div class="search-card">
      <!-- 左侧科技点阵装饰 -->
      <div class="tech-dot-grid"></div>

      <!-- 重庆城市地标建筑背景 -->
      <img :src="cqBg" class="cq-city-bg" alt="重庆城市背景" />

      <div
        class="badge-and-mascot"
        :class="{ 'is-wake-hovered': isWakeHovered }"
        @mouseenter="isWakeHovered = true"
        @mouseleave="isWakeHovered = false"
      >
        <!-- 框选区域的绝对定位透明框（hover 触发语音唤醒显示，移出隐藏） -->
        <div class="wake-transparent-box"></div>

        <div class="campaign-badge">
          <span class="badge-tag">{{ `百日攻坚\nAI赋能地质灾害` }}</span>
        </div>
        <div class="mascot-avatar-wrap">
          <WakeAvatarVideo class="mascot-avatar" :show-switch="false" />
        </div>

        <!-- 语音唤醒控制组件（置于最顶层，确保百分之百可点击） -->
        <div
          class="hero-wake-switch-control"
          :class="{ 'is-enabled': isWakeEnabled }"
          @click="toggleWakeStatus"
        >
          <span class="wake-switch-label">
            <i class="iconfont icon-mic"></i>
            语音唤醒
          </span>
          <el-switch
            class="wake-switch"
            :model-value="isWakeEnabled"
            :width="34"
            @change="handleSwitchChange"
          />
        </div>
      </div>
      <textarea
        :value="modelValue"
        class="search-input"
        placeholder="请输入你想问的问题"
        rows="2"
        @input="$emit('update:modelValue', $event.target.value)"
        @keydown.enter.prevent="handleEnter"
      ></textarea>
      <div class="search-bottom-bar">
        <div class="deep-think-pill" :class="{ 'is-active': isDeepThink }" @click="isDeepThink = !isDeepThink">
          <img :src="isDeepThink ? iconSparkleActive : iconSparkle" class="sparkle-icon" alt="深度思考" />
          <span>深度思考</span>
        </div>

        <div class="search-actions-right">
          <button class="voice-btn" title="语音提问" @click="$emit('voice')">
            <img :src="iconVoice" class="voice-icon" alt="语音提问" />
          </button>
          <span class="action-divider"></span>
          <button
            class="send-btn"
            :class="{ 'is-enabled': (modelValue || '').trim() }"
            title="发送"
            @click="handleSend"
          >
            <img :src="iconUp" class="send-icon" alt="发送" />
          </button>
        </div>
      </div>
    </div>

    <QuestionMarquee :questions="prompts" @select="applyPrompt" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import QuestionMarquee from '@/components/QuestionMarquee/index.vue'
import WakeAvatarVideo from '@/components/WakeAvatarVideo/index.vue'
import { useUserStore } from '@/store/user'
import { useWakeAssistantStore } from '@/store/wakeAssistant.js'
import { useWakeAudio } from '@/hooks/useWakeAudio.js'
import { quesPool } from '@/utils/ques_pool'

import iconSparkle from '@/assets/imgs/home/icon-sparkle.png'
import iconSparkleActive from '@/assets/imgs/home/icon-sparkle-active.png'
import iconVoice from '@/assets/imgs/home/voice.svg?url'
import iconUp from '@/assets/imgs/home/up.svg?url'
import chongqingLogo from '@/assets/images/home/brand-chongqing.png'
import cqBg from '@/assets/images/home/hero-city-lines.png'

defineOptions({ name: 'HomeHeroSection' })

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  quickPrompts: {
    type: Array,
    default: () => quesPool,
  },
})

const emit = defineEmits(['update:modelValue', 'search', 'voice'])

const userStore = useUserStore()
const { isDeepThink } = storeToRefs(userStore)
const wakeAssistantStore = useWakeAssistantStore()
const { enabled: isWakeEnabled } = storeToRefs(wakeAssistantStore)

const isWakeHovered = ref(false)
const audioText = ref('')
const isRecording = ref(false)
const isRecognizing = ref(false)
const wakeAudioActive = ref(true)

/**
 * 点击语音唤醒控制卡片切换开启/关闭状态。
 * 如果点击的是内部的 el-switch，则交由 switch 本身的 change 事件处理，避免重复切换。
 * @param {MouseEvent} [event] 点击事件对象
 */
function toggleWakeStatus(event) {
  if (event?.target?.closest('.wake-switch')) return
  if (isWakeEnabled.value) {
    wakeAssistantStore.disable()
  } else {
    wakeAssistantStore.enable()
  }
}

/**
 * 响应 el-switch 状态变更。
 * @param {boolean} status 目标状态
 */
function handleSwitchChange(status) {
  if (status) {
    wakeAssistantStore.enable()
  } else {
    wakeAssistantStore.disable()
  }
}

useWakeAudio({
  textRef: audioText,
  statusRef: isRecording,
  recognizingStatusRef: isRecognizing,
  activeRef: wakeAudioActive,
  autoStopCb: () => {
    if ((props.modelValue || '').trim()) {
      handleSend()
    }
  },
  wakeDetectedCb: () => wakeAssistantStore.playWakeAnimations(),
})

const removeLeadingPunctuation = (text = '') => text.replace(/^[\p{P}\p{Z}\s]+/gu, '')

watch(audioText, (val) => {
  if (val) {
    const normalized = removeLeadingPunctuation(val)
    emit('update:modelValue', normalized)
  }
})

const prompts = computed(() => props.quickPrompts)

function handleEnter() {
  emit('search', props.modelValue)
}

function handleSend() {
  emit('search', props.modelValue)
}

function applyPrompt(text) {
  emit('update:modelValue', text)
  emit('search', text)
}
</script>

<style lang="less" scoped>
.hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  width: 1000px;
  max-width: 100%;
  margin: 100px auto 0;

  /* 顶部背景流云光晕：向外自然晕开消融，色彩深润清透，彻底消除方正截断边缘 */
  &::before {
    content: '';
    position: absolute;
    top: 48px;
    left: 50%;
    transform: translateX(-50%);
    width: 1400px;
    max-width: calc(100vw - 40px);
    height: 450px;
    background:
      /* 1. 右侧建筑与吉祥物周边的柔光（色彩饱满，向外放射晕开） */
      radial-gradient(
        ellipse 55% 55% at 65% 42%,
        rgba(172, 216, 255, 0.72) 0%,
        rgba(206, 235, 255, 0.38) 42%,
        rgba(255, 255, 255, 0) 75%
      ),
      /* 2. 左侧副标题与输入框上方的轻薄云雾 */
      radial-gradient(
          ellipse 52% 50% at 35% 38%,
          rgba(182, 222, 255, 0.65) 0%,
          rgba(214, 238, 255, 0.32) 40%,
          rgba(255, 255, 255, 0) 72%
        ),
      /* 3. 横向大范围漫射主底色（饱满柔和的天空蓝，向四周大范围消融） */
      radial-gradient(
          ellipse 75% 68% at 50% 46%,
          rgba(192, 226, 255, 0.62) 0%,
          rgba(224, 242, 255, 0.3) 48%,
          rgba(255, 255, 255, 0) 82%
        );
    /* 椭圆径向羽化遮罩：使外围所有边缘 100% 柔和消融晕开，杜绝任何方正截断线 */
    mask-image: radial-gradient(ellipse 55% 55% at 50% 50%, #000 20%, rgba(0, 0, 0, 0.85) 52%, transparent 85%);
    -webkit-mask-image: radial-gradient(ellipse 55% 55% at 50% 50%, #000 20%, rgba(0, 0, 0, 0.85) 52%, transparent 85%);
    z-index: 0;
    pointer-events: none;
  }
}

.title-wrapper {
  width: 100%;
  gap: 19px;
  position: relative;
  z-index: 2;
  pointer-events: none;

  .main-title {
    position: relative;
    left: -33px;
    display: flex;
    align-items: center;
    font-family: 'DingTalk JinBuTi', 'DingTalkJinBuTi', sans-serif;
    font-size: 60px;
    font-weight: 800;
    letter-spacing: 0;
    line-height: 1.2;
    margin: 0;
    pointer-events: auto;

    .chongqing-logo {
      flex: 0 0 auto;
      width: 169.91px;
      height: 108px;
      margin-right: 8px;
      object-fit: contain;
    }

    span {
      position: relative;
      left: -25px;
      color: #007bff;
      background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }
}

.sub-title {
  font-family: 'DingTalk JinBuTi', 'DingTalkJinBuTi', sans-serif;
  width: 100%;
  margin-top: 19px;
  margin-bottom: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 400;
  color: #577ba4;
  letter-spacing: 0;
  position: relative;
  z-index: 2;
  pointer-events: none;

  span,
  .dot {
    pointer-events: auto;
  }
}

/* 搜索大输入框 */
.search-card {
  width: 100%;
  max-width: none;
  height: 184px;
  background: #ffffff;
  border: 1px solid #dce1ea;
  border-radius: 12px;
  box-shadow: none;
  padding: 16px;
  box-sizing: border-box;
  position: relative;
  z-index: 3;
  transition: all 0.25s;

  /* 左侧科技网格点阵：贴合卡片左沿，向下延伸，扇形自然淡出，色彩轻浅 */
  .tech-dot-grid {
    position: absolute;
    left: -118px;
    top: 6px;
    width: 120px;
    height: 250px;
    background-image: radial-gradient(rgba(53, 149, 251, 0.22) 1.5px, transparent 1.5px);
    background-size: 10px 10px;
    mask-image: radial-gradient(ellipse 100% 75% at 100% 50%, rgba(0, 0, 0, 0.75) 15%, transparent 85%);
    -webkit-mask-image: radial-gradient(ellipse 100% 75% at 100% 50%, rgba(0, 0, 0, 0.75) 15%, transparent 85%);
    pointer-events: none;
    z-index: 0;
  }

  .cq-city-bg {
    position: absolute;
    right: 0;
    bottom: calc(100% + 6px);
    width: 860px;
    height: 273px;
    object-fit: contain;
    pointer-events: none;
    z-index: 0;
  }

  .badge-and-mascot {
    position: absolute;
    right: -8px;
    bottom: calc(100% + 8px);
    z-index: 10;
    display: flex;
    align-items: center;
    gap: 4px;
    height: 205px;
    pointer-events: auto;

    &:hover,
    &:focus-within,
    &.is-wake-hovered {
      .hero-wake-switch-control {
        opacity: 1;
        visibility: visible;
        transform: translateY(0);
        pointer-events: auto;
      }
    }
  }

  /* 框选热区：绝对定位透明框，覆盖整个标语与吉祥物区域，保证 hover 触发灵敏完整 */
  .wake-transparent-box {
    position: absolute;
    inset: -6px -4px -8px -4px;
    z-index: 10;
    background: transparent;
    cursor: pointer;
    pointer-events: auto;
  }

  .campaign-badge {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    width: 168px;
    height: 88px;
    padding: 0 14px;
    box-sizing: border-box;
    border-radius: 16px 16px 0 16px;
    background: #fffcfa;
    box-shadow: 0 4px 10px 0 #0087ca24;
    pointer-events: none;

    .badge-tag {
      display: block;
      font-family: 'Alimama FangYuanTi VF', sans-serif;
      font-size: 20px;
      line-height: 30px;
      font-style: normal;
      font-weight: Bold-Square;
      white-space: pre-line;
      background: linear-gradient(90deg, #ff6e1a 0%, #ffa600 100%);
      background-clip: text;
      -webkit-background-clip: text;
      color: transparent;
      -webkit-text-fill-color: transparent;
    }
  }

  .mascot-avatar-wrap {
    width: 144px;
    height: 205px;
    position: relative;
    z-index: 2;
    pointer-events: none;
    overflow: visible;

    .mascot-avatar {
      position: relative;
      width: 100%;
      height: 100%;
    }
  }

  /* 语音唤醒开关控制：置于最顶层（z-index: 100），保证 hover 显现后能直接点击交互 */
  .hero-wake-switch-control {
    position: absolute;
    right: 108px;
    bottom: 6px;
    z-index: 100;
    display: flex;
    align-items: center;
    gap: 8px;
    height: 34px;
    padding: 0 10px;
    border: 1px solid rgba(53, 149, 251, 0.24);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.86);
    box-shadow: 0 2px 8px rgba(0, 123, 255, 0.08);
    backdrop-filter: blur(6px);
    opacity: 0;
    visibility: hidden;
    transform: translateY(6px);
    pointer-events: none;
    cursor: pointer;
    user-select: none;
    transition:
      opacity 0.25s ease,
      transform 0.25s ease,
      visibility 0.25s ease,
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      background-color 0.2s ease;

    &:hover,
    &.is-enabled {
      border-color: rgba(0, 123, 255, 0.42);
      background: rgba(255, 255, 255, 0.96);
      box-shadow: 0 3px 10px rgba(0, 123, 255, 0.14);
    }

    .wake-switch-label {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #617185;
      font-size: 12px;
      line-height: 1;
      white-space: nowrap;
      transition: color 0.2s ease;

      i {
        color: #3595fb;
        font-size: 14px;
      }
    }

    &.is-enabled .wake-switch-label {
      color: #007bff;
    }

    :deep(.wake-switch .el-switch__core) {
      --el-switch-on-color: #4394ef;
      --el-switch-off-color: #dce8f5;
    }
  }

  &:focus-within {
    border-color: #b9c8df;
    box-shadow: 0 2px 6px rgba(57, 126, 251, 0.1);
  }

  .search-input {
    width: 100%;
    resize: none;
    border: none;
    outline: none;
    font-size: 14px;
    color: #1a1a1a;
    line-height: 1.6;
    height: 108px;

    &::placeholder {
      color: #878898;
    }
  }

  .search-bottom-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 0;
    padding-top: 0;

    .deep-think-pill {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 34px;
      padding: 0 14px;
      background: #f5f7fb;
      border: 0;
      border-radius: 999px;
      font-size: 14px;
      color: #222529;
      cursor: pointer;
      user-select: none;
      transition: all 0.2s;

      .sparkle-icon {
        width: 16px;
        height: 16px;
      }

      &:hover {
        background: #e9edf5;
      }

      &.is-active {
        background: rgba(53, 97, 250, 0.1);
        color: #3561fa;
        font-weight: 500;
      }
    }

    .search-actions-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .voice-btn {
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        color: #506073;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #f1f5f9;
          color: #2a6ff7;
        }

        .voice-icon {
          width: 20px;
          height: 20px;
        }
      }

      .action-divider {
        width: 1px;
        height: 24px;
        background: #d6d9e5;
      }

      .send-btn {
        width: 44px;
        height: 32px;
        border-radius: 47px;
        background: linear-gradient(130deg, #3561fa 10.99%, #44ceff 117.04%);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        box-shadow: none;
        opacity: 0.5;
        transition: all 0.2s;

        .send-icon {
          width: 20px;
          height: 20px;
        }

        &:hover {
          transform: scale(1.02);
        }

        &.is-enabled {
          opacity: 1;
        }
      }
    }
  }
}
</style>
