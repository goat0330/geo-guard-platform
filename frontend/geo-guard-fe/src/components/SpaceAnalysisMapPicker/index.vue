<template>
  <el-dialog
    v-model="visible"
    title="选择空间分析点"
    width="min(960px, 90vw)"
    class="space-map-dialog"
    destroy-on-close
    append-to-body
    @closed="resetSelection"
  >
    <div class="map-picker-content">
      <MarsMap
        ref="marsMapRef"
        :show-area-search="true"
        @click="handleMapClick"
      />

      <div class="map-tools">
        <button type="button" title="回到初始位置" @click="marsMapRef?.flyToHome()">
          <i class="iconfont icon-location"></i>
        </button>
        <button type="button" title="放大" @click="marsMapRef?.handleZoomIn()">+</button>
        <button type="button" title="缩小" @click="marsMapRef?.handleZoomOut()">-</button>
      </div>

      <div class="selection-tip">
        <template v-if="selectedPoint">
          经度 {{ selectedPoint.longitude.toFixed(6) }}，纬度 {{ selectedPoint.latitude.toFixed(6) }}
        </template>
        <template v-else>请在地图上单击选择分析点</template>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedPoint" @click="confirmSelection">确定选点</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import * as mars3d from 'mars3d'
import { ElMessage } from 'element-plus'
import MarsMap from '@/components/MarsMap/index.vue'

const emits = defineEmits(['select'])

const visible = ref(false)
const marsMapRef = ref(null)
const selectedPoint = ref(null)

/**
 * 打开空间分析地图。
 * @returns {void}
 */
function open() {
  visible.value = true
  nextTick(() => marsMapRef.value?.resize())
}

/**
 * 将 Cesium 点击位置转换为经纬度并在地图上标记。
 * @param {object} event Mars3D 地图点击事件。
 * @returns {void}
 */
function handleMapClick(event) {
  if (!event?.cartesian) {
    ElMessage.warning('当前点击位置无法获取坐标，请重新选择')
    return
  }
  const point = mars3d.LngLatPoint.fromCartesian(event?.cartesian)
  const longitude = Number(point?.lng)
  const latitude = Number(point?.lat)

  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    ElMessage.warning('当前点击位置无法获取坐标，请重新选择')
    return
  }

  selectedPoint.value = { longitude, latitude }
  // 点击事件已包含地表高程，直接绘制避免首次等待地形服务采样。
  marsMapRef.value?.showRiskPoint(
    { lng: longitude, lat: latitude, alt: Number(point.alt) || 0 },
    { sampleTerrain: false, flyTo: false, alwaysVisible: true },
  )
}

/**
 * 确认当前点位并回传给聊天输入框。
 * @returns {void}
 */
function confirmSelection() {
  if (!selectedPoint.value) return
  emits('select', { ...selectedPoint.value })
  visible.value = false
}

/**
 * 清理点位状态，地图实例由 MarsMap 在弹窗销毁时释放。
 * @returns {void}
 */
function resetSelection() {
  selectedPoint.value = null
  marsMapRef.value = null
}

defineExpose({ open })
</script>

<style lang="less">
.space-map-dialog {
  .el-dialog__body {
    padding: 0 20px;
  }

  .map-picker-content {
    position: relative;
    height: min(620px, 68vh);
    overflow: hidden;
    border-radius: 8px;
  }

  .map-tools {
    position: absolute;
    right: 16px;
    bottom: 24px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border-radius: 6px;
    box-shadow: 0 2px 8px rgba(34, 37, 39, 0.16);

    button {
      width: 36px;
      height: 36px;
      border: 0;
      border-bottom: 1px solid #DCDEE5;
      background: #fff;
      color: #617185;
      font-size: 20px;
      cursor: pointer;

      &:last-child {
        border-bottom: 0;
      }

      &:hover {
        color: #007BFF;
        background: #DCEDFF;
      }
    }
  }

  .selection-tip {
    position: absolute;
    left: 50%;
    bottom: 24px;
    transform: translateX(-50%);
    min-width: 320px;
    padding: 10px 16px;
    border-radius: 6px;
    background: rgba(34, 37, 39, 0.86);
    color: #fff;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-size: 14px;
    font-style: normal;
    text-align: center;
    pointer-events: none;
  }
}
</style>
