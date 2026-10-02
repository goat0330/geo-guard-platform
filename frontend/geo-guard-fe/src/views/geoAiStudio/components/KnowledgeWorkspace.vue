<template>
  <section class="knowledge-workspace">
    <iframe
      class="knowledge-workspace__frame"
      :src="embeddedRagUrl"
      title="Yuxi 知识库工作台"
      loading="lazy"
    />
  </section>
</template>

<script setup>
defineOptions({ name: 'KnowledgeWorkspace' })

const embeddedRagUrl = `/yuxi/index.html?embed=rag&v=${Date.now()}#/extensions?tab=knowledge`
</script>

<style lang="less" scoped>
.knowledge-workspace {
  min-height: 680px;
  height: calc(100vh - 220px);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.knowledge-workspace__frame {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 680px;
  border: 0;
}
</style>
