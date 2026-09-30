async function json(url, options={}) {
  const r=await fetch(url,options); if(!r.ok) throw new Error(await r.text());
  return r.json()
}
export const api={
  pipeline:()=>json('/api/v1/pipeline'), config:()=>json('/api/v1/config'), presets:()=>json('/api/v1/chunk-presets'),
  documents:()=>json('/api/v1/documents'), blocks:(id)=>json(`/api/v1/documents/${id}/blocks`), chunks:(id)=>json(`/api/v1/documents/${id}/chunks`),
  debug:(payload)=>json('/api/v1/debug/retrieve',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}),
  evaluation:(payload)=>json('/api/v1/evaluation/run',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}),
  evidence:(id)=>json(`/api/v1/evidence/${id}`),
  upload:async(file,opts)=>{const f=new FormData();f.append('file',file);Object.entries(opts).forEach(([k,v])=>f.append(k,v));return json('/api/v1/documents/upload',{method:'POST',body:f})},
  addText:(payload)=>json('/api/v1/documents/text',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})
}
