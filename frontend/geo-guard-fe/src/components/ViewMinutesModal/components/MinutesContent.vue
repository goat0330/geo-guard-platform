<template>
  <main class="minutes-container">
    <div
      class="minutes-content-scroll"
      v-loading="loading"
      element-loading-text="正在提取会议要点并整理纪要..."
      element-loading-background="rgba(255, 255, 255, 0.85)"
    >
      <!-- 进度执行中骨架屏占位 -->
      <div v-if="loading" class="minutes-skeleton-wrap">
        <div class="skeleton-badge"></div>
        <div class="skeleton-title"></div>
        <div class="skeleton-paragraph">
          <div class="skeleton-line is-full"></div>
          <div class="skeleton-line is-long"></div>
          <div class="skeleton-line is-mid"></div>
          <div class="skeleton-line is-short"></div>
        </div>
        <div class="skeleton-paragraph">
          <div class="skeleton-line is-mid"></div>
          <div class="skeleton-line is-full"></div>
          <div class="skeleton-line is-long"></div>
          <div class="skeleton-line is-full"></div>
        </div>
      </div>

      <!-- 进度完成淡入展示纪要正文 -->
      <Transition v-else name="content-fade">
        <div class="minutes-real-content">
          <!-- AI 提示胶囊 -->
          <div v-if="renderedMarkdown" class="ai-badge">AI生成，请核对内容</div>

          <!-- 纪要大标题 -->
          <h3 class="document-title">{{ title }}纪要</h3>

          <!-- 正文内容：若有 Markdown 则解析渲染 -->
          <div v-if="renderedMarkdown" class="minutes-markdown" v-html="renderedMarkdown"></div>

          <!-- 空状态与状态提示：根据后端真实状态展示 -->
          <div v-else class="minutes-empty">
            <div class="empty-icon">
              <svg viewBox="0 0 48 48" width="48" height="48" fill="none">
                <path
                  d="M10 6C8.89543 6 8 6.89543 8 8V40C8 41.1046 8.89543 42 10 42H38C39.1046 42 40 41.1046 40 40V18L28 6H10Z"
                  stroke="#A6ACB8"
                  stroke-width="2.5"
                  stroke-linejoin="round"
                />
                <path d="M28 6V18H40" stroke="#A6ACB8" stroke-width="2.5" stroke-linejoin="round" />
                <line x1="16" y1="26" x2="32" y2="26" stroke="#A6ACB8" stroke-width="2.5" stroke-linecap="round" />
                <line x1="16" y1="32" x2="26" y2="32" stroke="#A6ACB8" stroke-width="2.5" stroke-linecap="round" />
              </svg>
            </div>
            <p class="empty-text">{{ emptyTipText }}</p>
          </div>
        </div>
      </Transition>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { createMd } from '@/utils/md.js'

defineOptions({ name: 'MinutesContent' })

const props = defineProps({
  title: {
    type: String,
    default: '应急会商',
  },
  summary: {
    type: Object,
    default: () => null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const mdParser = createMd({ useBreak: true, wrapTable: true })

const renderedMarkdown = computed(() => {
  const content = props.summary?.content
  if (!content || typeof content !== 'string') return ''
  try {
    return mdParser.render(content)
  } catch (e) {
    console.error('Markdown 渲染错误', e)
    return ''
  }
})

const emptyTipText = computed(() => {
  const status = props.summary?.status
  if (status === 'RUNNING') {
    return '智能纪要正在生成中，请稍候...'
  }
  if (status === 'FAILED') {
    return props.summary?.error ? `纪要生成失败：${props.summary.error}` : '智能纪要生成失败'
  }
  if (status === 'SKIPPED') {
    return '本次会议未开启转写或无可用转写内容，未生成纪要'
  }
  return '暂无会议纪要内容'
})
</script>

<style lang="less" scoped>
.minutes-container {
  flex: 1;
  min-height: 0;
  width: 100%;
  margin: 0;
  padding: 16px 24px;
  background: #f7f8f9;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.minutes-content-scroll {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 24px 32px;
  background: #ffffff;
  border-radius: 16px;
  overflow-y: auto;
  box-sizing: border-box;

  .ai-badge {
    display: table;
    margin: 0 auto 16px;
    padding: 2px 14px;
    border-radius: 4px;
    background: rgba(114, 199, 255, 0.26);
    color: #007bff;
    font-size: 12px;
    font-weight: 400;
    line-height: 20px;
    text-align: center;
  }

  .document-title {
    margin: 0 0 24px;
    color: #222527;
    font-size: 22px;
    font-weight: 700;
    line-height: 32px;
    text-align: center;
  }
}

// 骨架屏占位
.minutes-skeleton-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding-top: 8px;

  .skeleton-badge {
    width: 140px;
    height: 24px;
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f4f8 25%, #e0eefa 37%, #f0f4f8 63%);
    background-size: 400% 100%;
    animation: skeletonShimmer 1.4s ease infinite;
    margin-bottom: 16px;
  }

  .skeleton-title {
    width: 280px;
    height: 32px;
    border-radius: 6px;
    background: linear-gradient(90deg, #f0f4f8 25%, #e0eefa 37%, #f0f4f8 63%);
    background-size: 400% 100%;
    animation: skeletonShimmer 1.4s ease infinite;
    margin-bottom: 32px;
  }

  .skeleton-paragraph {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
    margin-bottom: 24px;
  }

  .skeleton-line {
    height: 18px;
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f4f8 25%, #e8f1fa 37%, #f0f4f8 63%);
    background-size: 400% 100%;
    animation: skeletonShimmer 1.4s ease infinite;

    &.is-full {
      width: 100%;
    }

    &.is-long {
      width: 88%;
    }

    &.is-mid {
      width: 72%;
    }

    &.is-short {
      width: 46%;
    }
  }
}

@keyframes skeletonShimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.content-fade-enter-active,
.content-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.content-fade-enter-from,
.content-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.minutes-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;

  .empty-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    opacity: 0.6;
  }

  .empty-text {
    margin: 0;
    color: #9096a2;
    font-size: 14px;
    line-height: 22px;
    text-align: center;
  }
}

.minutes-markdown {
  color: #383c41;
  font-size: 16px;
  line-height: 28px;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 18px 0 8px;
    color: #222527;
    font-weight: 700;
  }

  :deep(h1) {
    font-size: 20px;
  }

  :deep(h2) {
    font-size: 18px;
  }

  :deep(h3) {
    font-size: 16px;
  }

  :deep(p) {
    margin: 0 0 10px;
    text-align: justify;
  }

  :deep(ol),
  :deep(ul) {
    margin: 0 0 12px;
    padding-left: 24px;
  }

  :deep(li) {
    margin-bottom: 4px;
  }
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}
</style>
