<template>
  <div class="grid two">
    <section class="panel">
      <div class="panel-title"><div><h2>文档入库</h2><p>PDF 与文本资料进入真实 Parse → Chunk → Embedding 流程。</p></div></div>
      <div class="form-grid">
        <label>Chunk Preset<select v-model="form.chunk_preset_id"><option v-for="p in presets" :value="p.value" :key="p.value">{{p.label}}{{p.experimental?' · experimental':''}}</option></select></label>
        <label>Target tokens<input type="number" v-model.number="form.chunk_token_num" min="64" max="4096"></label>
        <label>Overlap %<input type="number" v-model.number="form.overlapped_percent" min="0" max="80"></label>
        <label>Delimiter<input v-model="form.delimiter"></label>
      </div>
      <p class="hint">{{presetDesc}}</p>
      <div :class="['drop-zone',{dragging}]" @dragover.prevent="dragging=true" @dragleave.prevent="dragging=false" @drop.prevent="drop">
        <input ref="fileInput" class="file-input" type="file" accept=".pdf,.txt,.md,.csv,.json,application/pdf,text/plain,text/markdown,text/csv,application/json" @change="pick">
        <b>{{file?.name||'将 PDF 或文本文件拖到这里'}}</b>
        <small>支持 PDF、TXT、MD、CSV、JSON；单文件最大 100 MB。</small>
        <button type="button" class="file-button" @click="fileInput?.click()">选择文件</button>
      </div>
      <button class="primary" :disabled="!file||uploading" @click="upload">{{uploading?'处理中…':'解析并索引文件'}}</button>
      <p class="hint">{{parserHint}}</p>
      <p v-if="message" class="message">{{message}}</p>
      <details class="text-entry">
        <summary>直接粘贴文本</summary>
        <label>文档名<input v-model="textName" maxlength="255"></label>
        <label>文本内容<textarea v-model="textDraft" rows="6" placeholder="粘贴需要解析和切块的文本"></textarea></label>
        <button class="primary" :disabled="uploading||!textDraft.trim()" @click="addText">{{uploading?'处理中…':'添加文本并切块'}}</button>
      </details>
    </section>

    <section class="panel">
      <div class="panel-title"><div><h2>Documents</h2><p>{{docs.length}} 个本地文档</p></div><button @click="loadDocs">刷新</button></div>
      <p v-if="!docs.length" class="empty-docs">还没有文档。拖入 PDF 或文本文件后会显示在这里。</p>
      <div class="doc-list">
        <button v-for="d in docs" :key="d.id" :class="['doc-row',{selected:selected?.id===d.id}]" @click="open(d)">
          <span><b>{{d.file_name}}</b><small>{{d.parser}} · {{d.chunk_preset_id}} · {{d.status}}</small></span>
          <em>{{d.chunk_count}} chunks · {{d.embedding_chunk_count}} embedded</em>
        </button>
      </div>
    </section>

    <section class="panel span-2" v-if="selected">
      <div class="panel-title">
        <div><h2>{{viewMode==='blocks'?'Parse Inspector':'Chunk / Embedding Inspector'}}</h2><p>{{selected.file_name}} · {{selected.parser}}</p></div>
        <div class="segmented"><button :class="{active:viewMode==='blocks'}" @click="viewMode='blocks'">Parsed Blocks</button><button :class="{active:viewMode==='chunks'}" @click="viewMode='chunks'">Chunks</button><button @click="$emit('open-retrieval')">去检索测试</button></div>
      </div>

      <div v-if="viewMode==='blocks'" class="chunk-list">
        <article v-for="b in blocks" :key="b.id" class="chunk-card">
          <div class="chunk-meta"><b>Block #{{b.ordinal+1}}</b><span>{{b.page?'P'+b.page:'Text'}}</span><code>{{b.id}}</code></div>
          <p>{{b.text}}</p>
          <small v-if="b.bbox" class="mono">bbox [{{b.bbox.map(x=>Number(x).toFixed(1)).join(', ')}}]</small>
        </article>
      </div>

      <template v-else>
        <div class="quality-grid">
          <div class="quality-stat"><small>Chunks</small><b>{{chunkQuality.count}}</b></div>
          <div class="quality-stat"><small>平均 / 中位 token</small><b>{{chunkQuality.average}} / {{chunkQuality.median}}</b></div>
          <div class="quality-stat"><small>最大 / 目标 token</small><b>{{chunkQuality.max}} / {{chunkQuality.target||'—'}}</b></div>
          <div class="quality-stat"><small>超出目标 / 空块</small><b>{{chunkQuality.overTarget}} / {{chunkQuality.empty}}</b></div>
          <div class="quality-stat"><small>含来源 Span</small><b>{{chunkQuality.located}} / {{chunkQuality.count}}</b></div>
          <div class="quality-stat"><small>Embedding 覆盖</small><b>{{chunkQuality.embedded}} / {{chunkQuality.count}}</b></div>
          <div class="quality-stat"><small>完全重复块</small><b>{{chunkQuality.duplicates}}</b></div>
        </div>
        <p class="hint quality-hint">token 数是当前近似估算；来源跨度用于追溯原文。可在 RAG Evaluation 用人工标注 Evidence IDs 比较检索质量。</p>
        <div class="chunk-list">
          <article v-for="c in chunks" :key="c.id" class="chunk-card">
            <div class="chunk-meta"><b>#{{c.chunk_index+1}}</b><span>{{c.token_count}} tokens</span><span>{{c.source_spans?.[0]?.page?'P'+c.source_spans[0].page:'Text'}}</span><span>Embedding {{c.embedding_dim?c.embedding_dim+'D':'未生成'}}</span><code>{{c.id}}</code></div>
            <p>{{c.text}}</p>
          </article>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import {computed,onMounted,reactive,ref} from 'vue';import {api} from '../api'
defineEmits(['open-retrieval'])
const docs=ref([]),blocks=ref([]),chunks=ref([]),selected=ref(null),presets=ref([]),file=ref(null),fileInput=ref(null),uploading=ref(false),message=ref(''),viewMode=ref('blocks'),dragging=ref(false)
const textName=ref('pasted-notes.txt'),textDraft=ref('')
const form=reactive({chunk_preset_id:'laws',chunk_token_num:512,overlapped_percent:10,delimiter:'\n'})
const presetDesc=computed(()=>presets.value.find(x=>x.value===form.chunk_preset_id)?.description||'')
const parserHint=computed(()=>file.value?.name.toLowerCase().endsWith('.pdf')
  ?'PDF：MinerU 已配置时调用 OCR/Layout；否则使用 PyMuPDF。扫描件没有可提取文本时会明确报错。'
  :'文本文件：按当前 preset 解析与切块；Embedding 未配置时会保留 BM25 检索并标记向量未生成。')
const chunkQuality=computed(()=>{
  const counts=chunks.value.map(c=>Number(c.token_count)||0).sort((a,b)=>a-b)
  const count=chunks.value.length,target=Number(selected.value?.chunk_parser_config?.chunk_token_num)||0
  const middle=Math.floor(counts.length/2)
  const median=counts.length?(counts.length%2?counts[middle]:Math.round((counts[middle-1]+counts[middle])/2)):0
  const located=chunks.value.filter(c=>Array.isArray(c.source_spans)&&c.source_spans.length).length
  const embedded=chunks.value.filter(c=>Number(c.embedding_dim)>0).length
  const texts=chunks.value.map(c=>(c.text||'').trim()).filter(Boolean)
  return {count,average:count?Math.round(counts.reduce((sum,n)=>sum+n,0)/count):0,median,max:counts.at(-1)||0,
    target,overTarget:target?counts.filter(n=>n>target).length:0,empty:chunks.value.filter(c=>!(c.text||'').trim()).length,
    located,embedded,duplicates:texts.length-new Set(texts).size}
})
async function loadDocs(){docs.value=await api.documents()}
async function open(d){selected.value=d;[blocks.value,chunks.value]=await Promise.all([api.blocks(d.id),api.chunks(d.id)])}
function acceptFile(next){
  const allowed=['.pdf','.txt','.md','.csv','.json'],name=(next?.name||'').toLowerCase()
  const extension=allowed.find(value=>name.endsWith(value))
  if(!extension){file.value=null;message.value='文件格式不支持。请选择 PDF、TXT、MD、CSV 或 JSON。';return}
  if(next.size>100*1024*1024){file.value=null;message.value='单文件不能超过 100 MB。';return}
  file.value=next;message.value=''
}
function pick(event){acceptFile(event.target.files?.[0])}
function drop(event){dragging.value=false;acceptFile(event.dataTransfer?.files?.[0])}
function configPayload(){return {chunk_preset_id:form.chunk_preset_id,chunk_token_num:form.chunk_token_num,overlapped_percent:form.overlapped_percent,delimiter:form.delimiter}}
async function showIndexed(result){
  message.value='完成：'+result.parser+' · '+result.blocks+' parsed blocks / '+result.chunks+' chunks / '+(result.embedded_chunks||0)+' embedded'
  await loadDocs();const doc=docs.value.find(item=>item.id===result.document_id);if(doc)await open(doc)
}
async function upload(){
  if(!file.value)return
  uploading.value=true;message.value=''
  try{const result=await api.upload(file.value,configPayload());file.value=null;if(fileInput.value)fileInput.value.value='';await showIndexed(result)}
  catch(error){message.value='失败：'+error.message}
  finally{uploading.value=false}
}
async function addText(){
  if(!textDraft.value.trim())return
  uploading.value=true;message.value=''
  try{const result=await api.addText({file_name:textName.value.trim()||'pasted-notes.txt',text:textDraft.value,chunk_preset_id:form.chunk_preset_id,chunk_parser_config:configPayload()});textDraft.value='';await showIndexed(result)}
  catch(error){message.value='失败：'+error.message}
  finally{uploading.value=false}
}
onMounted(async()=>{presets.value=(await api.presets()).chunk_presets;await loadDocs()})
</script>

<style scoped>
.drop-zone{display:grid;gap:7px;justify-items:center;margin:14px 0;padding:22px 14px;border:1px dashed #aab8c8;border-radius:9px;background:#fafcff;color:#34445a;text-align:center}
.drop-zone.dragging{border-color:#1677ff;background:#edf5ff}
.drop-zone small{color:#7b889b}.file-input{display:none}
.file-button{padding:7px 11px;border:1px solid #dce3ec;border-radius:7px;background:#fff;color:#42536a;cursor:pointer}
.text-entry{margin-top:14px;border-top:1px solid #edf0f4;padding-top:12px;color:#526176}
.text-entry summary{cursor:pointer;font-size:13px;font-weight:600}
.text-entry label{display:grid;gap:5px;margin:10px 0;font-size:12px}
.text-entry input,.text-entry textarea{width:100%;border:1px solid #dce3ec;border-radius:7px;padding:9px;font:inherit}
.empty-docs{padding:18px 8px;color:#7b889b;font-size:13px}
.quality-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:8px;margin-top:14px}
.quality-stat{display:grid;gap:5px;border:1px solid #e6ebf2;border-radius:8px;padding:10px;background:#fafcff}
.quality-stat small{color:#718096}.quality-stat b{color:#34445a;font-size:16px}
.quality-hint{margin-top:10px}
</style>
