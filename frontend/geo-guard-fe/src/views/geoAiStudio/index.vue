<template>
  <section class="studio-page">
    <header class="studio-header">
      <div class="header-copy">
        <div class="eyebrow"><span></span> GEO AI / LOCAL WORKBENCH</div>
        <h1>地灾智能体工作台</h1>
        <p>把知识资料、检索证据与隐患复核流程放在同一条可追踪链路上。</p>
      </div>
      <div class="service-health" aria-label="AI 服务状态">
        <div class="health-item">
          <span class="health-dot" :class="ragStatus"></span>
          <span
            >RAG <b>{{ serviceLabel(ragStatus) }}</b></span
          >
        </div>
        <div class="health-item">
          <span class="health-dot" :class="agentStatus"></span>
          <span
            >LangGraph <b>{{ serviceLabel(agentStatus) }}</b></span
          >
        </div>
        <div class="health-item llm-item">
          <span class="health-dot" :class="agentStatus === 'online' && llmConfigured ? 'online' : 'idle'"></span>
          <span
            >LLM <b>{{ llmLabel }}</b></span
          >
        </div>
      </div>
    </header>

    <nav class="studio-tabs" aria-label="AI 工作台模块">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        :class="{ 'is-active': activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <span class="tab-index">{{ tab.index }}</span>
        <span>{{ tab.label }}</span>
        <span class="tab-note">{{ tab.note }}</span>
      </button>
    </nav>

    <KeepAlive>
      <component :is="activeTab === 'knowledge' ? KnowledgeWorkspace : AgentWorkflowWorkspace" />
    </KeepAlive>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAgentHealth, getRagHealth } from '@/api/geoAiStudio.js'
import AgentWorkflowWorkspace from './components/AgentWorkflowWorkspace.vue'
import KnowledgeWorkspace from './components/KnowledgeWorkspace.vue'

defineOptions({ name: 'GeoAiStudio' })

const activeTab = ref('knowledge')
const ragStatus = ref('checking')
const agentStatus = ref('checking')
const llmConfigured = ref(false)
const tabs = [
  { id: 'knowledge', index: '01', label: '知识库与检索', note: 'RAG STUDIO' },
  { id: 'workflow', index: '02', label: '隐患复核流程', note: 'LANGGRAPH' },
]

const serviceLabel = (status) =>
  ({
    checking: '检测中',
    online: '已连接',
    offline: '未连接',
  })[status] || '未知'

const llmLabel = computed(() => {
  if (agentStatus.value !== 'online') return '服务未连接'
  return llmConfigured.value ? '已配置' : '未配置'
})

onMounted(async () => {
  const [rag, agent] = await Promise.allSettled([getRagHealth(), getAgentHealth()])
  ragStatus.value = rag.status === 'fulfilled' ? 'online' : 'offline'
  agentStatus.value = agent.status === 'fulfilled' ? 'online' : 'offline'
  llmConfigured.value = agent.status === 'fulfilled' && Boolean(agent.value.llm_configured)
})
</script>

<style lang="less" scoped>
.studio-page {
  --ink: #222527;
  --muted: #617185;
  --line: #e4eaf1;
  --blue: #007bff;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 30px 36px 40px;
  box-sizing: border-box;
  color: var(--ink);
  background: radial-gradient(ellipse at 100% 0%, rgba(220, 237, 255, 0.56), transparent 36%), #f6f8fb;
}

.studio-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  padding: 0 4px 24px;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #617185;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1.2px;

  span {
    width: 20px;
    height: 2px;
    background: #007bff;
  }
}

.header-copy h1 {
  margin: 10px 0 4px;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 28px;
  font-style: normal;
  font-weight: 700;
  line-height: 38px;
}

.header-copy p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 22px;
}

.service-health {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.health-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 11px;
  border: 1px solid rgba(215, 225, 237, 0.88);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;

  b {
    margin-left: 4px;
    color: #383c41;
    font-weight: 600;
  }
}

.health-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #a6acb8;

  &.online {
    background: #44b699;
    box-shadow: 0 0 0 3px rgba(68, 182, 153, 0.12);
  }
  &.offline {
    background: #e45b5b;
  }
  &.checking {
    background: #ff922c;
    animation: pulse 1s infinite alternate;
  }
  &.idle {
    background: #a6acb8;
  }
}

.studio-tabs {
  display: flex;
  gap: 10px;
  padding: 0 4px 18px;

  button {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 202px;
    padding: 10px 13px;
    border: 1px solid var(--line);
    border-radius: 9px;
    background: rgba(255, 255, 255, 0.7);
    color: #617185;
    text-align: left;
    cursor: pointer;
    transition:
      border-color 0.2s ease,
      background 0.2s ease,
      box-shadow 0.2s ease;
  }

  button:hover {
    border-color: #b8d7fb;
    background: #ffffff;
  }

  button.is-active {
    border-color: #a8cffb;
    background: #ffffff;
    box-shadow: 0 5px 18px rgba(0, 65, 140, 0.07);
    color: #222527;
  }
}

.tab-index {
  color: #007bff;
  font-size: 12px;
  font-weight: 700;
}

.tab-note {
  margin-left: auto;
  color: #a6acb8;
  font-size: 10px;
  letter-spacing: 0.6px;
}

@keyframes pulse {
  from {
    opacity: 0.5;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .studio-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .service-health {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .studio-page {
    padding: 22px 16px 30px;
  }
  .studio-tabs {
    flex-direction: column;
  }
  .studio-tabs button {
    width: 100%;
  }
  .llm-item {
    display: none;
  }
}
</style>
