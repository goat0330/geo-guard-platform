<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-dot"></span><div><b>Geo RAG Studio</b><small>Yuxi-derived RAG Lab</small></div></div>
      <button v-for="item in nav" :key="item.id" :class="['nav', {active:tab===item.id}]" @click="tab=item.id">{{item.label}}</button>
      <div class="sidebar-foot">
        <div class="status-line"><span :class="['status-dot',config.embedding_enabled&&'ok']"></span>Embedding {{config.embedding_enabled?'已接入':'未配置'}}</div>
        <div class="status-line"><span :class="['status-dot',config.reranker_enabled&&'ok']"></span>Reranker {{config.reranker_enabled?'已接入':'未配置'}}</div>
        <div class="status-line"><span :class="['status-dot',config.mineru_enabled&&'ok']"></span>MinerU / OCR {{config.mineru_enabled?'已接入':'未配置 · PDF 使用 PyMuPDF'}}</div>
      </div>
    </aside>
    <main class="main">
      <header><div><h1>{{title}}</h1><p>观察 Document → Evidence 的真实 RAG 后端过程，不承载 Agent Harness。</p></div></header>
      <PipelineInspector v-if="tab==='pipeline'" />
      <DocumentsPanel v-if="tab==='documents'" @open-retrieval="tab='retrieval'" />
      <RetrievalInspector v-if="tab==='retrieval'" />
      <EvaluationPanel v-if="tab==='evaluation'" />
    </main>
  </div>
</template>
<script setup>
import {computed,onMounted,ref} from 'vue'
import {api} from './api'
import PipelineInspector from './components/PipelineInspector.vue'
import DocumentsPanel from './components/DocumentsPanel.vue'
import RetrievalInspector from './components/RetrievalInspector.vue'
import EvaluationPanel from './components/EvaluationPanel.vue'
const tab=ref('pipeline'); const config=ref({})
const nav=[{id:'pipeline',label:'Pipeline Inspector'},{id:'documents',label:'Documents & Chunks'},{id:'retrieval',label:'Retrieval Inspector'},{id:'evaluation',label:'RAG Evaluation'}]
const title=computed(()=>nav.find(x=>x.id===tab.value)?.label||'Geo RAG Studio')
onMounted(async()=>{try{config.value=await api.config()}catch(e){console.error(e)}})
</script>
