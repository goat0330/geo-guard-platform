<template>
  <section class="panel">
    <div class="panel-title"><div><h2>RAG Pipeline</h2><p>类似 LangGraph Studio 的观察方式，但这里观察的是检索链路。</p></div></div>
    <div class="pipeline">
      <template v-for="(s,i) in stages" :key="s.id">
        <button :class="['stage',s.status]" @click="selected=s">
          <span class="stage-index">{{String(i+1).padStart(2,'0')}}</span>
          <b>{{s.label}}</b><small>{{s.description}}</small>
        </button>
        <span v-if="i<stages.length-1" class="arrow">→</span>
      </template>
    </div>
    <div v-if="selected" class="detail-card"><b>{{selected.label}}</b><p>{{selected.description}}</p><code>{{selected.id}}</code></div>
    <div class="architecture-note">
      <b>当前轻量模式</b>
      <span>SQLite + 本地文件；Embedding/Rerank 走远程 API。后续正式部署可把存储 Adapter 换成 Milvus / PostgreSQL，不改上层 API。</span>
    </div>
  </section>
</template>
<script setup>
import {onMounted,ref} from 'vue';import {api} from '../api'
const stages=ref([]),selected=ref(null)
onMounted(async()=>{stages.value=(await api.pipeline()).stages;selected.value=stages.value[0]})
</script>
