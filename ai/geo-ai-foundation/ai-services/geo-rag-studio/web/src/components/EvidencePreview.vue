<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-head"><div><b>Evidence Source</b><small>{{evidence?.file_name}} · {{evidence?.chunk_id}}</small></div><button @click="$emit('close')">×</button></div>
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="evidence" class="evidence-layout">
        <div class="preview-wrap" v-if="activeSpan?.page">
          <img :src="imageUrl" @load="imgLoaded=true">
          <div v-if="imgLoaded&&activeSpan?.bbox" class="bbox" :style="bboxStyle"></div>
        </div>
        <div class="source-text"><h3>Chunk 文本</h3><p>{{evidence.text}}</p><h3>Source spans</h3><button v-for="(s,i) in evidence.source_spans" :key="i" @click="spanIndex=i">P{{s.page}} · {{s.bbox?.map(x=>x.toFixed(1)).join(', ')}}</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import {computed,onMounted,ref,watch} from 'vue';import {api} from '../api'
const props=defineProps({id:String});defineEmits(['close']);const evidence=ref(null),loading=ref(true),spanIndex=ref(0),imgLoaded=ref(false)
const activeSpan=computed(()=>evidence.value?.source_spans?.[spanIndex.value])
const imageUrl=computed(()=>activeSpan.value?.page?`/api/v1/documents/${evidence.value.document_id}/page/${activeSpan.value.page}/image`:null)
const bboxStyle=computed(()=>{const s=activeSpan.value;if(!s?.bbox||!s.page_width||!s.page_height)return{};const [x0,y0,x1,y1]=s.bbox;return{left:(x0/s.page_width*100)+'%',top:(y0/s.page_height*100)+'%',width:((x1-x0)/s.page_width*100)+'%',height:((y1-y0)/s.page_height*100)+'%'}})
watch(imageUrl,()=>{imgLoaded.value=false})
onMounted(async()=>{try{evidence.value=await api.evidence(props.id)}finally{loading.value=false}})
</script>
