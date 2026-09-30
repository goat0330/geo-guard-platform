<template>
  <div class="report-map-image">
    <img v-if="src" :src="src" :alt="`${title}结果图`" />
    <div v-else class="image-loading">{{ loading ? '评价图生成中...' : '暂无评价图数据' }}</div>
    <div v-if="src" class="image-actions">
      <button type="button" @click="openPreview">
        <i class="iconfont icon-fangda"></i>查看
      </button>
      <button type="button" @click="downloadImage">
        <i class="iconfont icon-xaizai"></i>下载
      </button>
    </div>
  </div>

  <el-dialog
    v-model="previewVisible"
    class="risk-map-dialog"
    :title="title"
    width="min(1280px, calc(100vw - 64px))"
    append-to-body
    destroy-on-close
    @opened="handlePreviewOpened"
  >
    <div class="preview-map">
      <DynamicRiskMap
        ref="previewMapRef"
        :units="units"
        :level-field="levelField"
        :level-colors="levelColors"
        :risk-type-name="riskTypeName"
        :active-levels="activeLevels"
      />
      <div class="map-legend">
        <strong>图例</strong>
        <button
          v-for="level in levels"
          :key="level"
          type="button"
          :class="{ 'is-disabled': !activeLevels.includes(level) }"
          @click="toggleLevel(level)"
        >
          <i :style="{ backgroundColor: levelColors[level] }"></i>
          {{ RISK_LEVEL_TEXT[level] }}{{ riskTypeName }}
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import DynamicRiskMap from '@/components/DynamicRiskMap/index.vue'
import { RISK_LEVEL_TEXT } from '@/utils/enum.js'

defineOptions({ name: 'RiskReportMapImage' })

const props = defineProps({
  src: { type: String, default: '' },
  title: { type: String, required: true },
  units: { type: Array, default: () => [] },
  levelField: { type: String, required: true },
  levelColors: { type: Object, required: true },
  riskTypeName: { type: String, required: true },
  loading: Boolean,
})

const previewVisible = ref(false)
const previewMapRef = ref(null)
const levels = [4, 3, 2, 1]
const activeLevels = ref([...levels])

const toggleLevel = (level) => {
  if (activeLevels.value.includes(level)) {
    if (activeLevels.value.length === 1) return
    activeLevels.value = activeLevels.value.filter((item) => item !== level)
  } else {
    activeLevels.value = [...activeLevels.value, level]
  }
}

const openPreview = () => {
  previewVisible.value = true
}

// 弹窗动画结束后再重算地图尺寸，避免地图按缩放前的容器尺寸初始化。
const handlePreviewOpened = async () => {
  await nextTick()
  requestAnimationFrame(() => previewMapRef.value?.resize())
}

const downloadImage = () => {
  if (!props.src) return
  const link = document.createElement('a')
  link.href = props.src
  link.download = `${props.title}结果图.png`
  document.body.appendChild(link)
  link.click()
  link.remove()
}
</script>

<style lang="less" scoped>
.report-map-image { position: relative; width: 200px; height: 102px; margin: 8px 0 4px 12px; overflow: hidden; border: 1px solid #DCEDFF; border-radius: 4px; background: #617185; }
.report-map-image > img { display: block; width: 100%; height: 100%; object-fit: cover; }
.image-loading { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; color: #ffffff; font-size: 12px; }
.image-actions { position: absolute; bottom: 4px; left: 50%; display: flex; width: 112px; height: 26px; align-items: center; padding: 3px 4px; border-radius: 4px; background: rgba(97, 113, 133, 0.78); opacity: 0; transform: translateX(-50%); transition: opacity 0.2s ease; }
.report-map-image:hover .image-actions { opacity: 1; }
.image-actions button { display: flex; width: 50%; height: 20px; align-items: center; justify-content: center; gap: 2px; padding: 0; border: 0; border-radius: 4px; background: transparent; color: #ffffff; font-size: 12px; cursor: pointer; }
.image-actions button:hover { background: rgba(34, 37, 39, 0.4); }
.image-actions i { font-size: 12px; }
.preview-map { position: relative; width: 100%; height: min(680px, calc(100vh - 190px)); min-height: 400px; overflow: hidden; border-radius: 8px; background: #617185; }
.map-legend {
  position: absolute;
  right: 20px;
  bottom: 20px;
  z-index: 5;
  display: flex;
  min-width: 124px;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 8px;
  background: rgba(34, 37, 39, 0.28);
  box-shadow: 0 8px 24px rgba(34, 37, 39, 0.18);
  backdrop-filter: blur(16px) saturate(140%);
  -webkit-backdrop-filter: blur(16px) saturate(140%);
}
.map-legend strong { color: #ffffff; font-size: 12px; }
.map-legend button { display: flex; align-items: center; gap: 10px; padding: 0; border: 0; background: transparent; color: #ffffff; font-size: 12px; cursor: pointer; transition: opacity 0.2s ease; }
.map-legend button.is-disabled { opacity: 0.45; }
.map-legend button i { width: 32px; height: 14px; border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 2px; }
</style>

<style lang="less">
.risk-map-dialog { border-radius: 8px; }
.risk-map-dialog .el-dialog__header { margin: 0; padding: 20px 24px 16px; color: #222527; font-family: 'Alimama FangYuanTi VF', sans-serif; font-size: 18px; font-style: normal; font-weight: Bold-Square; }
.risk-map-dialog .el-dialog__body { padding: 0 24px 24px; }
</style>
