<template>
  <article class="map-report-popup">
    <header>
      <h2>报灾信息</h2>
      <button type="button" title="关闭报灾信息" @click="emit('close')"><el-icon><Close /></el-icon></button>
    </header>
    <div class="popup-body">
      <div class="meta-row">
        <span>报灾人：{{ textValue(report?.userName) }}</span>
        <span>报灾时间：{{ textValue(report?.checkTime || report?.createDate) }}</span>
      </div>
      <p>填报信息：{{ textValue(report?.sceneTextRecord) }}</p>
      <img v-if="photos.length" :src="photos[0]" alt="报灾现场" />
      <div v-else class="photo-empty"><el-icon><Picture /></el-icon><span>暂无现场图片</span></div>
    </div>
  </article>
</template>

<script setup>
defineOptions({ name: 'GroupDefenseMapPopup' })

defineProps({
  report: { type: Object, default: null },
  photos: { type: Array, default: () => [] },
})

const emit = defineEmits(['close'])
const textValue = (value) => value ?? '--'
</script>

<style lang="less" scoped>
.map-report-popup {
  position: relative;
  width: 400px;
  max-width: calc(100% - 32px);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8px 24px rgba(0, 20, 48, 0.2);
  color: #222527;
  backdrop-filter: blur(12px);
}

header { display: flex; height: 48px; align-items: center; justify-content: space-between; padding: 0 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.86); }
h2 { margin: 0; font-size: 16px; font-weight: 600; }
header button { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 6px; color: #617185; font-size: 18px; cursor: pointer; }
header button:hover { background: #dcedff; color: #007bff; }
.popup-body { padding: 12px 16px 16px; }
.meta-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 14px; color: #617185; font-size: 12px; }
.meta-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.popup-body p { margin: 10px 0; overflow: hidden; color: #617185; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.popup-body img, .photo-empty { width: 100%; height: 210px; border-radius: 8px; }
.popup-body img { object-fit: cover; }
.photo-empty { display: flex; align-items: center; flex-direction: column; justify-content: center; gap: 8px; background: rgba(220, 237, 255, 0.72); color: #9096a2; font-size: 14px; }
.photo-empty .el-icon { color: #007bff; font-size: 30px; }
</style>
