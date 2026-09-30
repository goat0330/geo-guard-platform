<template>
  <section class="section-container agents-section">
    <div class="section-header">
      <div class="section-title">
        <span class="title-icon">🛒</span>
        <h2>智能体广场</h2>
      </div>
      <div class="more-link" @click="$emit('more')">
        <span>查看更多</span>
        <span class="arrow">&gt;</span>
      </div>
    </div>

    <!-- 6 大智能体推荐卡片 (2列 x 3行) -->
    <div class="agent-cards-grid">
      <div
        v-for="agent in list"
        :key="agent.title"
        class="agent-card"
        tabindex="0"
        @mouseenter="activeAgentTitle = agent.title"
        @mouseleave="activeAgentTitle = ''"
        @focusin="activeAgentTitle = agent.title"
        @focusout="activeAgentTitle = ''"
        @click="$emit('agent-click', agent)"
        @keydown.enter="$emit('agent-click', agent)"
      >
        <div class="agent-icon-wrap">
          <img :src="agent.icon" class="agent-3d-img" :alt="agent.title" />
          <img
            v-if="agent.hoverIcon && activeAgentTitle === agent.title"
            :src="agent.hoverIcon"
            class="agent-3d-img agent-3d-img-hover"
            alt=""
          />
        </div>
        <div class="agent-details">
          <h3 class="agent-name">{{ agent.title }}</h3>
          <p class="agent-desc">{{ agent.description }}</p>
          <div class="agent-actions">
            <button
              v-for="(action, aIndex) in agent.actions"
              :key="aIndex"
              class="agent-action-pill"
              @click.stop="$emit('action-click', { agent, action })"
            >
              {{ action }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

import hazardReviewImg from '@/assets/imgs/agents/agent-business-hazard-review.png'
import hazardReviewHoverImg from '@/assets/imgs/agents/agent-business-hazard-review.gif'
import imageRecognitionImg from '@/assets/imgs/agents/agent-business-image-recognition.png'
import imageRecognitionHoverImg from '@/assets/imgs/agents/agent-business-image-recognition.gif'
import dynamicPlanImg from '@/assets/imgs/agents/agent-business-dynamic-plan.png'
import dynamicPlanHoverImg from '@/assets/imgs/agents/agent-business-dynamic-plan.gif'
import reviewReportImg from '@/assets/imgs/agents/agent-business-review-report.png'
import reviewReportHoverImg from '@/assets/imgs/agents/agent-business-review-report.gif'
import spatialAnalysisImg from '@/assets/imgs/agents/agent-business-spatial-analysis.png'
import spatialAnalysisHoverImg from '@/assets/imgs/agents/agent-business-spatial-analysis.gif'
import riskEvaluationImg from '@/assets/imgs/agents/agent-business-risk-evaluation.png'
import riskEvaluationHoverImg from '@/assets/imgs/agents/agent-business-risk-evaluation.gif'

const props = defineProps({
  agents: {
    type: Array,
    default: () => [
      {
        title: '隐患复核智能体',
        description: '自动比对多源隐患资料，识别新增、重复和字段冲突，生成待审核结论，实现隐患复核一键审核。',
        icon: hazardReviewImg,
        hoverIcon: hazardReviewHoverImg,
        actions: ['复核今日待办', '查看资料差异'],
      },
      {
        title: 'AI识图智能体',
        type: 'image_recognition',
        description: '自动识别并标注裂缝、变形、掉块等异常，快速筛出需重点核查的视频照片和风险对象。',
        icon: imageRecognitionImg,
        hoverIcon: imageRecognitionHoverImg,
        actions: ['上传现场照片', '查看历史识图'],
      },
      {
        title: '动态预案智能体',
        description: '融合最新风险、监测、人员和避险资源，实时动态更新单点应急预案。',
        icon: dynamicPlanImg,
        hoverIcon: dynamicPlanHoverImg,
        actions: ['生成最新预案', '对比预演变化'],
      },
      {
        title: '复盘报告智能体',
        description: '自动归集事件处置全过程，一键生成复盘报告、专家建议和问题改进清单。',
        icon: reviewReportImg,
        hoverIcon: reviewReportHoverImg,
        actions: ['复盘处置事件', '复盘应急演练'],
      },
      {
        title: '空间分析智能体',
        type: 'space_analysis',
        description: '关联隐患、居民、道路和避险资源，快速圈定影响范围，辅助确定避险点和撤离路线。',
        icon: spatialAnalysisImg,
        hoverIcon: spatialAnalysisHoverImg,
        actions: ['空间数据汇聚', '地上地下对齐'],
      },
      {
        title: '风险评价智能体',
        description: '融合调查、监测和气象数据，自动评定滑坡、崩塌风险，锁定高风险区域并生成研判报文。',
        icon: riskEvaluationImg,
        hoverIcon: riskEvaluationHoverImg,
        actions: ['动态风险评价', '生成研判报文'],
      },
    ],
  },
})

defineEmits(['agent-click', 'action-click', 'more'])

const list = computed(() => props.agents)
const activeAgentTitle = ref('')
</script>

<style lang="less" scoped>
.section-container {
  width: 1000px;
  max-width: 100%;
  margin: 48px auto 0;
  padding-bottom: 40px; // 与最底部保持 40px 间隔
  box-sizing: border-box;
  display: flex;
  flex-direction: column;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;

      .title-icon {
        font-size: 18px;
        line-height: 1;
      }

      h2 {
        font-size: 18px;
        font-weight: 700;
        color: #222527;
        margin: 0;
        line-height: 20px;
      }
    }

    .more-link {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #007bff;
      cursor: pointer;
      line-height: 20px;
      transition: opacity 0.2s;

      .arrow {
        font-size: 12px;
        line-height: 1;
      }

      &:hover {
        opacity: 0.8;
      }
    }
  }
}

.agent-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 492px);
  gap: 16px;

  .agent-card {
    width: 492px;
    height: 162px;
    box-sizing: border-box;
    background: linear-gradient(180deg, #f1f9ff 0%, #ffffff 100%);
    border: 1px solid #e6f0fa;
    border-radius: 8px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    cursor: pointer;
    box-shadow: 0 2px 8px 0 rgba(10, 98, 192, 0.12);
    transition: all 0.25s;

    &:hover {
      background: #ffffff;
      border-color: #c2dcf7;
      transform: translateY(-2px);
      box-shadow: 0 4px 16px 0 rgba(10, 98, 192, 0.16);

      .agent-details .agent-name {
        color: #007bff;
      }
    }

    .agent-icon-wrap {
      position: relative;
      width: 98px;
      height: 98px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .agent-3d-img {
        position: absolute;
        inset: 0;
        width: 98px;
        height: 98px;
        object-fit: contain;
        transition: transform 0.25s;
      }

      .agent-3d-img-hover {
        z-index: 1;
      }
    }

    &:hover .agent-3d-img {
      transform: scale(1.05);
    }

    .agent-details {
      flex: 1;
      min-width: 0;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .agent-name {
        font-size: 18px;
        font-weight: 700;
        color: #383c41;
        line-height: 26px;
        margin: 0;
        transition: color 0.2s;
      }

      .agent-desc {
        font-size: 14px;
        color: #9096a2;
        line-height: 20px;
        margin: 0;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .agent-actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;

        .agent-action-pill {
          height: 24px;
          padding: 0 10px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 400;
          color: #3595fb;
          background: #e0eefa;
          border: none;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            background: #d2e6f8;
            color: #007bff;
          }
        }
      }
    }
  }
}
</style>
