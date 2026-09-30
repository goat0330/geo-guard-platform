<template>
  <div class="evacuation-route-map">
    <div ref="mapRef" class="mars3d-container"></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createEvacuationRouteMap } from './useEvacuationRouteMap.js'
import 'mars3d/mars3d.css'

defineOptions({ name: 'EvacuationRouteMap' })

const props = defineProps({
  data: { type: Object, required: true },
  activeIndex: { type: Number, default: -1 },
  /** 结果卡预览不播放人员撤离动画，详情弹窗保持默认播放。 */
  playAnimation: { type: Boolean, default: true },
  /** 预览地图仅展示，不响应路线点选。 */
  interactive: { type: Boolean, default: true },
  /** 取景留白系数：> 1 时镜头整体拉远，避免路线贴边被裁。 */
  viewScale: { type: Number, default: 1.1 },
})

const emit = defineEmits(['select'])
const mapRef = ref(null)
let routeMap = null

/** 创建地图实例：mars3d 会在内部完成绘制与镜头定位 */
const createMap = () => {
  routeMap = createEvacuationRouteMap(mapRef.value, props.data, (index) => emit('select', index), {
    playAnimation: props.playAnimation,
    interactive: props.interactive,
    viewScale: props.viewScale,
  })
  routeMap.selectRoute(props.activeIndex)
}

const destroyMap = () => {
  routeMap?.destroy()
  routeMap = null
}

onMounted(createMap)

/** 路线数据更新：重建地图，保证画面与最新路线一致 */
watch(() => props.data, () => {
  destroyMap()
  createMap()
})

watch(
  () => props.activeIndex,
  (index) => routeMap?.selectRoute(index),
)

onBeforeUnmount(destroyMap)
</script>

<style lang="less" scoped>
.evacuation-route-map {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: #617185;
}

.mars3d-container {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

::deep(.cesium-viewer-bottom),
::deep(.mars3d-logo) {
  display: none !important;
}
</style>
