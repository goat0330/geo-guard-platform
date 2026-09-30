<template>
  <section class="panel">
    <div class="panel-title"><div><h2>Retrieval Inspector</h2><p>观察 BM25、Vector、Fusion、Rerank 和最终 Evidence；未配置的阶段会如实跳过。</p></div></div>
    <div class="query-bar"><input v-model="query" @keyup.enter="run" placeholder="例如：地质灾害应急响应启动条件"><button class="primary" :disabled="running||!query.trim()" @click="run">{{running?'检索中…':'运行'}}</button></div>
    <div class="retrieval-config">
      <label>Search mode<select v-model="searchMode"><option value="hybrid">Hybrid</option><option value="keyword">BM25 / Keyword</option><option value="vector">Vector</option></select></label>
      <label>Recall top-k<input v-model.number="recallTopK" type="number" min="1" max="200"></label>
      <label>Final top-k<input v-model.number="finalTopK" type="number" min="1" max="100"></label>
      <label>Vector weight<input v-model.number="vectorWeight" type="number" min="0" max="1" step="0.05"></label>
      <label>BM25 weight<input v-model.number="bm25Weight" type="number" min="0" max="1" step="0.05"></label>
      <label>Document ID<input v-model="documentId" placeholder="可选，限定文档"></label>
      <label class="rerank-toggle"><input v-model="useReranker" type="checkbox"> Use Reranker</label>
    </div>
    <p v-if="error" class="error-message">{{error}}</p>
    <template v-if="result">
      <div class="metrics"><span>模式 <b>{{result.result.retrieval.mode}}</b></span><span>Recall <b>{{result.result.retrieval.recall_top_k}}</b></span><span>Final <b>{{result.result.retrieval.final_top_k}}</b></span><span>Rerank <b>{{result.result.retrieval.rerank_status}}</b></span><span>总耗时 <b>{{result.result.timing_ms.total}} ms</b></span><span>Context <b>{{result.result.usage.context_tokens}} tokens</b></span></div>
      <p v-if="result.result.retrieval.vector_skip_reason" class="hint">Vector：{{result.result.retrieval.vector_skip_reason}}</p>
      <div class="rank-grid">
        <RankColumn title="BM25" :items="result.stages.bm25" />
        <RankColumn title="Vector" :items="result.stages.vector" />
        <RankColumn title="Fusion" :items="result.stages.fusion" />
        <RankColumn title="Rerank" :items="result.stages.rerank" />
      </div>
      <div class="final-section">
        <h2>Final Evidence / Context</h2>
        <article v-for="e in result.evidences" :key="e.evidence_id" class="evidence-card" @click="preview=e.evidence_id">
          <div><b>[{{e.metadata.rank}}] {{e.file_name}}</b><span>{{e.page?'P'+e.page:'Text'}} · {{e.token_count}} tokens</span></div>
          <p>{{e.text}}</p><code>{{e.evidence_id}}</code>
        </article>
      </div>
    </template>
    <EvidencePreview v-if="preview" :id="preview" @close="preview=null" />
  </section>
</template>

<script setup>
import {defineComponent,h,ref} from 'vue'
import {api} from '../api'
import EvidencePreview from './EvidencePreview.vue'

const query=ref(''),running=ref(false),useReranker=ref(false),result=ref(null),preview=ref(null),error=ref('')
const searchMode=ref('hybrid'),recallTopK=ref(20),finalTopK=ref(6),vectorWeight=ref(0.7),bm25Weight=ref(0.3),documentId=ref('')
async function run(){
  running.value=true;error.value=''
  const filters=documentId.value.trim()?{document_id:documentId.value.trim()}:{}
  try{result.value=await api.debug({query:query.value,search_mode:searchMode.value,use_reranker:useReranker.value,recall_top_k:recallTopK.value,final_top_k:finalTopK.value,vector_weight:vectorWeight.value,bm25_weight:bm25Weight.value,filters})}
  catch(e){error.value='检索失败：'+e.message;result.value=null}
  finally{running.value=false}
}
const RankColumn=defineComponent({props:{title:String,items:Array},setup(p){return()=>h('div',{class:'rank-col'},[h('h3',p.title),...(p.items||[]).slice(0,10).map(x=>h('div',{class:'rank-item'},[h('b','#'+x.rank),h('span',x.file_name),h('em',Number(x.score??0).toFixed(4)),h('small',x.text.slice(0,72))]))])}})
</script>

<style scoped>
.retrieval-config{display:flex;align-items:end;gap:12px;flex-wrap:wrap;margin:10px 0 14px}
.retrieval-config label{display:grid;gap:5px;color:#607086;font-size:12px}
.retrieval-config select,.retrieval-config input[type=number]{border:1px solid #dce3ec;border-radius:7px;padding:8px;background:#fff;min-width:115px}
.retrieval-config .rerank-toggle{display:flex;align-items:center;gap:6px}
.error-message{color:#bd3131;white-space:pre-wrap}
</style>
