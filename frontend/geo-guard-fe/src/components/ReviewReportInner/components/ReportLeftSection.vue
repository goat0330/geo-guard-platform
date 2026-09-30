<template>
  <section class="report-left-section" aria-label="事件详情生成结果">
    <!-- 顶部返回与标题栏 -->
    <header class="left-top-bar">
      <div class="header-left">
        <button
          type="button"
          class="back-btn"
          title="返回复盘列表"
          @click="emit('back')"
        >
          <i class="iconfont icon-title-back"></i>
        </button>
        <h1 class="page-title">事件详情生成结果</h1>
      </div>
    </header>

    <!-- 主体内容卡片区 -->
    <div class="left-body-wrap">
      <!-- 智能体状态与事件内嵌卡片 (800px × 260px) -->
      <article class="agent-main-card">
        <!-- 顶部浅蓝渐变条 -->
        <div class="card-header-banner">
          <div class="avatar-wrap">
            <img :src="agentAvatar" alt="复盘报告智能体" class="agent-avatar-img" />
          </div>
          <div class="agent-meta">
            <h2 class="agent-name">复盘报告智能体</h2>
            <p class="agent-action">正在调用预案更新能力</p>
          </div>
        </div>

        <!-- 节点指示器与事件卡片 -->
        <div class="card-flow-body">
          <!-- 节点头部：蓝色实心圆点 + 参数识别 -->
          <div class="step-node-header">
            <i class="flow-dot"></i>
            <span class="flow-title">参数识别</span>
          </div>

          <!-- 节点内容区：左侧连接线 + 右侧内嵌卡片 -->
          <div class="step-node-content">
            <div class="flow-line-track">
              <div class="flow-line"></div>
            </div>

            <!-- 内嵌事件简要卡片 -->
            <div class="nested-event-card">
              <!-- 卡片首行：图标 + 标题及标签 -->
              <div class="event-headline-row">
                <div class="risk-icon-box">
                  <img :src="riskIcon" alt="风险等级图标" class="risk-icon-img" />
                </div>
                <div class="event-title-wrap">
                  <div class="title-status-line">
                    <h3 class="event-title" :title="event.title || '高谷镇G319国道2270处公路岩质边坡垮塌'">
                      {{ event.title || '高谷镇G319国道2270处公路岩质边坡垮塌' }}
                    </h3>
                    <!-- 状态标签（生成中） -->
                    <div class="card-status-badge">
                      <i class="badge-dot"></i>
                      <span>生成中</span>
                    </div>
                  </div>
                  <div class="tags-row">
                    <span class="tag-badge tag-disaster">{{ event.disasterType || '滑坡' }}</span>
                    <span class="tag-badge tag-scale">{{ event.scale || '小型' }}</span>
                    <span class="tag-badge tag-mutation">{{ event.mutationType || '新生突发' }}</span>
                  </div>
                </div>
              </div>

              <!-- 卡片次行：位置与更新时间 -->
              <div class="event-info-row">
                <div class="info-item">
                  <i class="iconfont icon-address"></i>
                  <span>位置：{{ event.location || '重庆市彭水县龙村5组316国道内侧' }}</span>
                </div>
                <div class="info-divider"></div>
                <div class="info-item">
                  <i class="iconfont icon-a-Frame1"></i>
                  <span>更新时间：{{ event.occurTime || '2016-09-10 16:20' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <!-- 下部动态加载状态展示区 -->
      <div class="loading-state-area">
        <!-- 环形旋转加载动画 -->
        <div class="radial-spinner" aria-hidden="true">
          <span
            v-for="index in 8"
            :key="index"
            class="spinner-blade"
            :style="{
              transform: `rotate(${(index - 1) * 45}deg)`,
              opacity: (0.3 + (index - 1) * 0.1).toFixed(2),
            }"
          ></span>
        </div>

        <!-- 提示大文案 -->
        <p class="loading-main-text">正在汇集并核验事件资料、处置记录与现场调查信息...</p>

        <!-- 胶囊进度条 -->
        <div class="capsule-progress-pill">
          <span>已完成2/8个步骤，可在右侧查看智能体执行过程</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import agentAvatar from '@/assets/imgs/chatBox/risk-evaluation-agent-avatar.png'
import redRiskIcon from '@/assets/imgs/risk/red.svg'
import orangeRiskIcon from '@/assets/imgs/risk/orange.svg'
import yellowRiskIcon from '@/assets/imgs/risk/yellow.svg'
import blueRiskIcon from '@/assets/imgs/risk/blue.svg'

defineOptions({ name: 'ReportLeftSection' })

const props = defineProps({
  event: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['back'])

// 根据风险等级动态选取对应的 risk 图标
const riskIcon = computed(() => {
  const level = String(props.event.riskLevel || '').trim()
  if (level.includes('极高') || level.includes('高') || level.includes('红')) {
    return redRiskIcon
  }
  if (level.includes('中') || level.includes('较') || level.includes('橙')) {
    return orangeRiskIcon
  }
  if (level.includes('低') || level.includes('黄')) {
    return yellowRiskIcon
  }
  if (level.includes('微') || level.includes('蓝')) {
    return blueRiskIcon
  }
  return redRiskIcon
})
</script>

<style lang="less" scoped src="./ReportLeftSection.less"></style>
