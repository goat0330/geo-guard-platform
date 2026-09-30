<template>
  <section class="panel">
    <div class="panel-title"><div><h2>RAG Evaluation</h2><p>用人工标注的 Evidence IDs 评估召回；不会调用 LLM Judge。</p></div></div>
    <div class="eval-config">
      <label>Search mode<select v-model="searchMode"><option value="hybrid">Hybrid（无向量时显示 BM25-only）</option><option value="keyword">BM25 / Keyword</option><option value="vector">Vector</option></select></label>
      <label>Recall top-k<input v-model.number="recallTopK" type="number" min="1" max="200"></label>
      <label>Final top-k<input v-model.number="topK" type="number" min="1" max="100"></label>
      <label>限定 Document ID（可选）<input v-model="documentId" placeholder="DOC-…"></label>
      <label class="eval-check"><input v-model="useReranker" type="checkbox"> 使用 Reranker</label>
    </div>
    <article v-for="(item,index) in cases" :key="index" class="eval-case">
      <div class="eval-case-title"><b>Case {{index+1}}</b><button v-if="cases.length>1" @click="cases.splice(index,1)">移除</button></div>
      <label>Query<textarea v-model="item.query" rows="2" placeholder="输入真实业务问题"></textarea></label>
      <label>Relevant Evidence IDs<textarea v-model="item.evidenceIds" rows="2" placeholder="EV-CHK-…，多个 ID 用逗号或换行分隔"></textarea></label>
    </article>
    <div class="eval-actions"><button @click="cases.push({query:'',evidenceIds:''})">添加 Case</button><button class="primary" :disabled="running||!canRun" @click="run">{{running?'评测中…':'运行评测'}}</button></div>
    <p class="hint">标注方式：先从 Retrieval Inspector 查看原文和 PDF 定位，再将确实相关的 Evidence ID 填入。Evaluation 结果只代表当前标注集。</p>
    <p v-if="error" class="eval-error">{{error}}</p>
    <section v-if="result" class="eval-results">
      <div class="metrics"><span>Case 数 <b>{{result.case_count}}</b></span><span v-for="(value,key) in result.averages" :key="key">{{key}} <b>{{Number(value).toFixed(3)}}</b></span></div>
      <article v-for="(item,index) in result.cases" :key="index" class="eval-case">
        <b>Case {{index+1}} · {{item.query}}</b>
        <p>Recall@{{item.top_k}} {{item.recall_at_k.toFixed(3)}} · Precision@{{item.top_k}} {{item.precision_at_k.toFixed(3)}} · MRR {{item.mrr.toFixed(3)}} · nDCG@{{item.top_k}} {{item.ndcg_at_k.toFixed(3)}}</p>
        <small>相关: {{item.relevant_evidence_ids.join(', ')}}</small><br><small>召回: {{item.returned_evidence_ids.join(', ')||'无'}}</small>
      </article>
    </section>
  </section>
</template>

<script setup>
import {computed,ref} from 'vue'
import {api} from '../api'

const cases=ref([{query:'',evidenceIds:''}])
const searchMode=ref('hybrid'),recallTopK=ref(50),topK=ref(5),documentId=ref(''),useReranker=ref(false)
const running=ref(false),error=ref(''),result=ref(null)
const canRun=computed(()=>cases.value.every(item=>item.query.trim()&&item.evidenceIds.trim()))

async function run(){
  running.value=true;error.value='';result.value=null
  const filters=documentId.value.trim()?{document_id:documentId.value.trim()}:{}
  const payload={search_mode:searchMode.value,recall_top_k:recallTopK.value,use_reranker:useReranker.value,
    cases:cases.value.map(item=>({query:item.query.trim(),relevant_evidence_ids:item.evidenceIds.split(/[\s,，]+/).filter(Boolean),top_k:topK.value,filters}))}
  try{result.value=await api.evaluation(payload)}catch(e){error.value='评测失败：'+e.message}finally{running.value=false}
}
</script>

<style scoped>
.eval-config{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:16px 0}
.eval-config label,.eval-case label{display:grid;gap:5px;color:#607086;font-size:12px}
.eval-config input,.eval-config select,.eval-case textarea{width:100%;border:1px solid #dce3ec;border-radius:7px;padding:9px;background:#fff;font:inherit}
.eval-check{display:flex!important;align-items:center;gap:8px!important}.eval-check input{width:auto}
.eval-case{border:1px solid #e6ebf2;border-radius:9px;padding:12px;margin:10px 0;display:grid;gap:9px}
.eval-case-title,.eval-actions{display:flex;justify-content:space-between;align-items:center;gap:10px}
.eval-case p{color:#59697e}.eval-case small{color:#718096;overflow-wrap:anywhere}.eval-error{color:#bd3131;white-space:pre-wrap}.eval-results{margin-top:16px}
@media(max-width:780px){.eval-config{grid-template-columns:1fr 1fr}}
</style>
