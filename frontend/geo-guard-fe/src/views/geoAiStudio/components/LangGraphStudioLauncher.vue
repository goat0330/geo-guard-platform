<template>
  <section class="studio-card">
    <header>
      <div>
        <span class="section-kicker">OFFICIAL LANGGRAPH STUDIO</span>
        <h2>{{ workflow?.title || '隐患复核流程' }}</h2>
      </div>
      <span class="studio-status" :class="`is-${studioStatus}`">{{ statusLabel }}</span>
    </header>

    <p class="studio-description">
      图画布由官方 LangGraph Studio 提供。本地 LangGraph Dev API 加载当前项目中的真实流程定义。
    </p>

    <div class="studio-details">
      <div><span>Graph ID</span><code>hazard_review</code></div>
      <div><span>本地 API</span><code>http://127.0.0.1:2024</code></div>
      <div><span>运行状态</span><b>{{ running ? '业务流程执行中' : `${trace.length} 个 SSE 节点事件` }}</b></div>
    </div>

    <a class="studio-open" :href="studioUrl" target="_blank" rel="noopener noreferrer">
      打开官方 LangGraph Studio <span aria-hidden="true">↗</span>
    </a>

    <p class="studio-footnote">
      画布用于检查和调试图；本页运行按钮仍走项目自己的 SSE 接口并展示业务 Trace。
    </p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

defineOptions({ name: 'LangGraphStudioLauncher' })

defineProps({
  workflow: { type: Object, default: null },
  trace: { type: Array, default: () => [] },
  running: { type: Boolean, default: false },
})

const studioStatus = ref('checking')
const studioUrl = import.meta.env.VITE_LANGGRAPH_STUDIO_URL
  || 'https://smith.langchain.com/studio/?baseUrl=http%3A%2F%2F127.0.0.1%3A2024'
const statusLabel = computed(() => ({
  checking: '检查本地 API',
  ready: '本地 API 已连接',
  unavailable: '本地 API 未启动',
})[studioStatus.value])

onMounted(async () => {
  try {
    const response = await fetch('/geo-langgraph-studio/ok')
    studioStatus.value = response.ok ? 'ready' : 'unavailable'
  } catch {
    studioStatus.value = 'unavailable'
  }
})
</script>

<style lang="less" scoped>
.studio-card {
  display: flex;
  min-height: 300px;
  flex-direction: column;
  padding: 20px;
  border: 1px solid #e4eaf1;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(32, 68, 105, 0.035);
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.section-kicker {
  color: #8b9bae;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
}
h2 {
  margin: 5px 0 0;
  color: #222527;
  font-size: 18px;
  font-weight: 700;
}
.studio-status {
  padding: 6px 8px;
  border-radius: 6px;
  background: #f3f5f8;
  color: #748398;
  font-size: 10px;
  white-space: nowrap;
}
.studio-status.is-ready {
  background: #eef9f4;
  color: #319777;
}
.studio-status.is-unavailable {
  background: #fff5ed;
  color: #bd7230;
}
.studio-description,
.studio-footnote {
  color: #718398;
  font-size: 12px;
  line-height: 1.65;
}
.studio-description {
  margin: 14px 0;
}
.studio-details {
  display: grid;
  gap: 10px;
  padding: 13px;
  border: 1px solid #edf0f4;
  border-radius: 8px;
  background: #fafbfd;
}
.studio-details div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #7c8b9a;
  font-size: 11px;
}
.studio-details code {
  color: #47586b;
  font-size: 11px;
}
.studio-details b {
  color: #47586b;
  font-weight: 500;
}
.studio-open {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: auto;
  padding: 11px 14px;
  border-radius: 7px;
  background: #248aa0;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}
.studio-open:hover {
  background: #19748a;
}
.studio-footnote {
  margin: 10px 0 0;
  font-size: 10px;
}
@media (max-width: 680px) {
  .studio-card {
    min-height: 0;
    gap: 10px;
  }
}
</style>
