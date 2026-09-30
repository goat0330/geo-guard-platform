import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'nav-home-solid.png' },
      },
      {
        path: 'warning/risk-eval',
        name: 'RiskEval',
        component: () => import('@/views/riskEvaluation/index.vue'),
        meta: { title: '风险评价' },
      },
      {
        path: 'warning/risk-report',
        name: 'DynamicRiskReport',
        component: () => import('@/views/dynamicRiskReport/index.vue'),
        meta: { title: '动态风险评价一张图' },
      },
      {
        path: 'warning/monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: '专业监测' },
      },
      {
        path: 'warning/group-defense',
        name: 'GroupDefense',
        component: () => import('@/views/groupDefense/index.vue'),
        meta: { title: '群测群防' },
      },
      {
        path: 'warning/hazard-review',
        name: 'HazardReview',
        component: () => import('@/views/hazardReview/index.vue'),
        meta: { title: '隐患复核' },
      },
      {
        path: 'emergency',
        name: 'Emergency',
        component: () => import('@/views/emergency/index.vue'),
        meta: { title: '应急处置' },
      },
      {
        path: 'review',
        name: 'Review',
        component: () => import('@/views/review/index.vue'),
        meta: { title: '复盘优化' },
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agents/index.vue'),
        meta: { title: '智能体广场' },
      },
      ...(import.meta.env.DEV ? [{
        path: 'ai-studio',
        name: 'GeoAiStudio',
        component: () => import('@/views/geoAiStudio/index.vue'),
        meta: { title: 'AI 工作台' },
      }] : []),
      {
        path: 'message-center',
        name: 'MessageCenter',
        component: () => import('@/views/messageCenter/index.vue'),
        meta: { title: '消息中心' },
      },
      {
        path: 'agents/assistant',
        name: 'AgentAssistant',
        component: () => import('@/views/agents/assistant/index.vue'),
        meta: { title: '智能体助手' },
      },
      {
        path: 'agents/space-analysis',
        name: 'SpaceAnalysis',
        component: () => import('@/views/agents/spaceAnalysis/index.vue'),
        meta: { title: '空间分析智能体' },
      },
      {
        path: 'agents/writing',
        name: 'AgentWriting',
        component: () => import('@/views/agents/writing/index.vue'),
        meta: { title: '公文撰写' },
      },
      {
        path: 'agents/conversion',
        name: 'AgentConversion',
        component: () => import('@/views/agents/conversion/index.vue'),
        meta: { title: '格式转换' },
      },
      {
        path: 'agents/meeting',
        name: 'AgentMeeting',
        component: () => import('@/views/agents/meeting/index.vue'),
        meta: { title: '会议纪要' },
      },
      {
        path: 'chat-engine/chatting',
        name: 'ChatEngineChatting',
        component: () => import('@/views/chatEngine/chatting/index.vue'),
        meta: { title: 'AI智能体问答', keepAlive: true },
      },
      {
        path: 'chatting',
        name: 'Chatting',
        component: () => import('@/views/chatEngine/chatting/index.vue'),
        meta: { title: 'AI智能体问答', keepAlive: true },
      },
      {
        path: 'chat-engine/history',
        name: 'ChatEngineHistory',
        component: () => import('@/views/chatEngine/history/index.vue'),
        meta: { title: '历史问答' },
      },
      {
        path: 'chat-engine',
        redirect: '/home',
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
]

import setupPermissionGuard from '@/router/permission.js'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupPermissionGuard(router)

export default router
