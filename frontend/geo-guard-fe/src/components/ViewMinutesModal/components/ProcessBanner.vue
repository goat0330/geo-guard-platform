<template>
  <section class="process-banner">
    <!-- 左侧标题区 -->
    <div class="banner-left">
      <div class="doc-icon">
        <svg viewBox="0 0 24 28" width="24" height="28" fill="none">
          <defs>
            <linearGradient id="bannerDocGrad" x1="0" y1="0" x2="24" y2="0" gradientUnits="userSpaceOnUse">
              <stop offset="12.5%" stop-color="#007BFF" />
              <stop offset="100%" stop-color="#00B2FF" />
            </linearGradient>
          </defs>
          <path
            d="M2 3C2 1.34315 3.34315 0 5 0H16L24 8V25C24 26.6569 22.6569 28 21 28H5C3.34315 28 2 26.6569 2 25V3Z"
            fill="url(#bannerDocGrad)"
          />
          <path d="M16 0V8H24" fill="rgba(255,255,255,0.4)" />
          <line x1="6" y1="12" x2="15" y2="12" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" />
          <line x1="6" y1="17" x2="18" y2="17" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" />
        </svg>
      </div>
      <div class="title-wrap">
        <h4 class="banner-title">智能纪要生成过程</h4>
        <p class="banner-desc">系统已根据会议转写完成内容整理</p>
      </div>
    </div>

    <!-- 垂直分割线 -->
    <div class="banner-divider"></div>

    <!-- 中间 4 阶段步骤流（带模拟推进过程） -->
    <div class="step-workflow">
      <template v-for="(step, index) in steps" :key="step">
        <div
          class="step-node"
          :class="{
            'is-done': index < currentStep,
            'is-active': index === currentStep && !isAllDone,
            'is-pending': index > currentStep,
          }"
        >
          <span class="node-icon">
            <!-- 已完成状态：蓝底白勾 -->
            <svg v-if="index < currentStep || isAllDone" viewBox="0 0 12 12" width="10" height="10" fill="none">
              <path
                d="M2.5 6.2L4.8 8.5L9.5 3.5"
                stroke="#FFFFFF"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- 正在处理状态：微光脉冲小圆点 -->
            <span v-else-if="index === currentStep" class="active-dot"></span>
            <!-- 未开始状态：灰线空心勾 -->
            <svg v-else viewBox="0 0 12 12" width="10" height="10" fill="none">
              <path
                d="M2.5 6.2L4.8 8.5L9.5 3.5"
                stroke="#A6ACB8"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="node-text">{{ step }}</span>
        </div>

        <!-- 节点间连接线 -->
        <div
          v-if="index < steps.length - 1"
          class="step-line"
          :class="{ 'is-filled': index < currentStep || isAllDone }"
        >
          <div class="line-fill"></div>
        </div>
      </template>
    </div>

    <!-- 垂直分割线 -->
    <div class="banner-divider"></div>

    <!-- 右侧状态标记 -->
    <div class="banner-right">
      <span v-if="isAllDone" class="finish-text">
        <template v-if="duration">已完成 · 用时<em class="num-font">{{ duration }}</em>秒</template>
        <template v-else>已完成</template>
      </span>
      <span v-else class="processing-text">
        <template v-if="animatedSeconds">正在整理中 · 用时<em class="num-font">{{ animatedSeconds }}</em>秒</template>
        <template v-else>正在整理中...</template>
      </span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

defineOptions({ name: 'ProcessBanner' })

const props = defineProps({
  duration: {
    type: [Number, String],
    default: null,
  },
  steps: {
    type: Array,
    default: () => [
      '读取原始转写',
      '理解会议内容',
      '梳理议题与结论',
      '生成纪要草稿',
    ],
  },
})

const emit = defineEmits(['finish'])

const currentStep = ref(0)
const isAllDone = ref(false)
const animatedSeconds = ref(0)

let timerIds = []

const clearAllTimers = () => {
  timerIds.forEach((id) => clearTimeout(id))
  timerIds = []
}

// 推进过程节点
const startFakeProgress = () => {
  clearAllTimers()
  currentStep.value = 0
  isAllDone.value = false
  animatedSeconds.value = props.duration ? Math.min(6, Number(props.duration)) : 0

  // 阶段 1：读取原始转写
  timerIds.push(
    setTimeout(() => {
      currentStep.value = 1
      if (props.duration) animatedSeconds.value = Math.min(15, Number(props.duration))
    }, 450),
  )

  // 阶段 2：理解会议内容
  timerIds.push(
    setTimeout(() => {
      currentStep.value = 2
      if (props.duration) animatedSeconds.value = Math.min(24, Number(props.duration))
    }, 950),
  )

  // 阶段 3：梳理议题与结论
  timerIds.push(
    setTimeout(() => {
      currentStep.value = 3
      if (props.duration) animatedSeconds.value = Math.min(32, Number(props.duration))
    }, 1450),
  )

  // 阶段 4：生成纪要草稿与全部完成
  timerIds.push(
    setTimeout(() => {
      currentStep.value = 4
      animatedSeconds.value = Number(props.duration) || 0
      isAllDone.value = true
      emit('finish')
    }, 1950),
  )
}

onMounted(() => {
  startFakeProgress()
})

watch(
  () => props.duration,
  () => {
    startFakeProgress()
  },
)

onBeforeUnmount(() => {
  clearAllTimers()
})
</script>

<style lang="less" scoped>
.process-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  margin: 0 24px 12px;
  padding: 0 20px;
  background: #f1f8ff;
  border-radius: 12px;
  box-sizing: border-box;
  flex-shrink: 0;

  .banner-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .doc-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .title-wrap {
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .banner-title {
    margin: 0;
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 20px;
  }

  .banner-desc {
    margin: 4px 0 0;
    color: #9096a2;
    font-size: 12px;
    font-weight: 400;
    line-height: 16px;
  }

  .banner-divider {
    width: 1px;
    height: 36px;
    background: rgba(0, 123, 255, 0.2);
    flex-shrink: 0;
  }

  .step-workflow {
    display: flex;
    align-items: center;
  }

  .step-node {
    display: flex;
    align-items: center;
    gap: 8px;
    transition: all 0.3s ease;

    .node-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      box-sizing: border-box;
      flex-shrink: 0;
      transition: all 0.3s ease;
    }

    .node-text {
      color: #617185;
      font-size: 14px;
      font-weight: 400;
      line-height: 20px;
      white-space: nowrap;
      transition: color 0.3s ease;
    }

    // 已完成状态
    &.is-done {
      .node-icon {
        border: 2px solid #007bff;
        background: #007bff;
      }

      .node-text {
        color: #383c41;
        font-weight: 500;
      }
    }

    // 正在处理状态
    &.is-active {
      .node-icon {
        border: 2px solid #007bff;
        background: #ebf4ff;

        .active-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #007bff;
          animation: pulseNode 1s infinite ease-in-out;
        }
      }

      .node-text {
        color: #007bff;
        font-weight: 700;
      }
    }

    // 未开始状态
    &.is-pending {
      .node-icon {
        border: 2px solid #a6acb8;
        background: #f2f9ff;
      }

      .node-text {
        color: #617185;
      }
    }
  }

  .step-line {
    position: relative;
    width: 46px;
    height: 2px;
    margin: 0 10px;
    background: #dee5ed;
    border-radius: 2px;
    overflow: hidden;
    flex-shrink: 0;

    .line-fill {
      width: 0%;
      height: 100%;
      background: #007bff;
      transition: width 0.4s ease;
    }

    &.is-filled .line-fill {
      width: 100%;
    }
  }

  .banner-right {
    display: flex;
    align-items: center;

    .finish-text {
      color: #44b699;
      font-size: 14px;
      font-weight: 700;
      line-height: 20px;
      white-space: nowrap;

      .num-font {
        font-family: 'Alimama FangYuanTi VF', sans-serif;
        font-style: normal;
      }
    }

    .processing-text {
      color: #007bff;
      font-size: 14px;
      font-weight: 500;
      line-height: 20px;
      white-space: nowrap;

      .num-font {
        font-family: 'Alimama FangYuanTi VF', sans-serif;
        font-style: normal;
      }
    }
  }
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}

@keyframes pulseNode {
  0%, 100% {
    transform: scale(0.85);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
  }
}
</style>
