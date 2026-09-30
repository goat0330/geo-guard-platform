<template>
  <section class="location-panel">
    <div class="panel-heading">
      <img :src="agentAvatar" alt="" />
      <div>
        <h2>空间分析智能体</h2>
        <p>指一处位置，读懂地上与地下</p>
      </div>
    </div>

    <div class="panel-content">
      <p class="section-label">找到你关心的地方 <span>定位不改变分析对象</span></p>
      <div class="region-filters">
        <label v-for="(level, index) in regionLevels" :key="level.label">
          {{ level.label }}：
          <el-select
            :model-value="selectedPath[index + 1]?.id ?? ''"
            :placeholder="level.placeholder"
            clearable
            @change="selectRegionLevel(index, $event)"
          >
            <el-option
              v-for="item in optionsAt(index)"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </label>
        <button type="button" class="reset-button" @click="resetRegion">重置</button>
      </div>
      <div class="quick-search">
        <span>快速搜索：</span>
        <el-select
          v-model="searchRegionId"
          filterable
          clearable
          placeholder="搜索你关心的地方"
          @change="searchRegion"
        >
          <el-option
            v-for="item in searchableRegions"
            :key="item.node.id"
            :label="item.path.map((part) => part.name).join(' / ')"
            :value="item.node.id"
          />
        </el-select>
        <el-button class="search-button" type="primary" @click="searchRegion(searchRegionId)">搜索</el-button>
      </div>

      <div class="selection-tabs" role="tablist" aria-label="位置选择方式">
        <button type="button" :class="{ active: mode === 'browse' }" @click="changeMode('browse')">浏览地图</button>
        <button type="button" :class="{ active: mode === 'slope' }" @click="changeMode('slope')">斜坡单元</button>
        <button type="button" :class="{ active: mode === 'point' }" @click="changeMode('point')">选一个点</button>
      </div>
      <div
        class="map-wrap"
        :class="{ 'is-picking': mode === 'point', 'is-selecting-slope': mode === 'slope' }"
      >
        <MarsMap
          ref="mapRef"
          :area-search-style="{ display: 'none' }"
          :default-show-area-boundary="true"
          :preserve-drawing-buffer="true"
          :show-compass="false"
          @area-loaded="handleAreaLoaded"
          @select-area="handleAreaSelected"
          @click="handleMapClick"
        >
          <template #overlay>
            <MapOverlayControls
              :show-filters="false"
              :show-base-map-control="false"
              @locate-home="mapRef?.flyToHome()"
              @zoom-in="mapRef?.handleZoomIn()"
              @zoom-out="mapRef?.handleZoomOut()"
            />
          </template>
        </MarsMap>
      </div>
      <div class="selection-footer">
        <div>
          <strong>{{ selectionTitle }}</strong>
          <p>{{ selectionDescription }}</p>
        </div>
        <div class="footer-actions">
          <el-button :disabled="submitting" @click="$emit('cancel')">取消</el-button>
          <el-button
            type="primary"
            :disabled="!canConfirm || submitting"
            :loading="submitting"
            @click="confirmSelection"
          >
            开始分析
          </el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import * as mars3d from 'mars3d'
import { ElMessage } from 'element-plus'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import { getSlopeUnitList } from '@/api/common.js'
import { SLOPE_UNIT_KEY } from '@/utils/enum.js'
import { useSlopeUnitLayer } from './useSlopeUnitLayer.js'
// 与聊天进行中的智能体头像保持一致。
import agentAvatar from '@/assets/imgs/chatBox/risk-evaluation-agent-avatar.png'

defineOptions({ name: 'SpaceAnalysisLocationPanel' })
defineProps({
  submitting: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['cancel', 'confirm'])
const mapRef = ref(null)
const mode = ref('browse')
const root = ref(null)
const regionTree = ref([])
const selectedPath = ref([])
const searchRegionId = ref('')
const selectedPoint = ref(null)
const selectedSlope = ref(null)
let slopeRequestId = 0
const regionLevels = [
  { label: '县域选择', placeholder: '请选择县域' },
  { label: '乡镇选择', placeholder: '请选择乡镇' },
  { label: '村落选择', placeholder: '请选择村落' },
]

const searchableRegions = computed(() => {
  const items = []
  const visit = (nodes, path) => {
    for (const node of nodes) {
      const nextPath = [...path, node]
      items.push({ node, path: nextPath })
      if (Array.isArray(node.children)) visit(node.children, nextPath)
    }
  }
  visit(regionTree.value, [])
  return items
})
const selectedRegion = computed(() => selectedPath.value.at(-1) || null)
const selectionSummary = computed(() => {
  if (mode.value === 'point') {
    return selectedPoint.value
      ? `${selectedPoint.value.longitude.toFixed(6)}°E，${selectedPoint.value.latitude.toFixed(6)}°N`
      : '请在地图上选一个点'
  }
  if (mode.value === 'slope') return selectedSlope.value?.name || '请在地图上选择斜坡单元'
  return '请选择分析方式'
})
const selectionTitle = computed(() => {
  if (mode.value === 'browse') return '尚未选择新的分析对象'
  if (mode.value === 'slope') return `斜坡单元选择：${selectionSummary.value}`
  return mode.value === 'point'
    ? `所选坐标：${selectionSummary.value}`
    : '尚未选择新的分析对象'
})
const selectionDescription = computed(() => {
  if (mode.value === 'browse') return '尚未选择新的分析对象'
  if (mode.value === 'slope') return '仅以已选择的斜坡单元进行空间分析。'
  return mode.value === 'point'
    ? '在地图中点击选点，确认后即可进行空间分析。'
    : '尚未选择新的分析对象'
})
const canConfirm = computed(() => {
  if (mode.value === 'point') return !!selectedPoint.value
  return mode.value === 'slope' && selectedSlope.value?.id !== undefined && selectedSlope.value?.id !== null
})
const slopeUnitLayer = useSlopeUnitLayer((slope) => {
  selectedSlope.value = slope
})
const optionsAt = (index) => selectedPath.value[index]?.children || []
const findRegionPath = (id) => searchableRegions.value.find(({ node }) => String(node.id) === String(id))?.path || []

function handleAreaLoaded({ root: initialRoot, tree } = {}) {
  regionTree.value = Array.isArray(tree) ? tree : []
  root.value = initialRoot || null
  selectedPath.value = initialRoot ? findRegionPath(initialRoot.id) : []
  slopeUnitLayer.attach(mapRef.value?.getMap())
  if (mode.value === 'slope') void loadSlopeUnits()
}

function handleAreaSelected({ selected } = {}) {
  if (!selected) return
  selectedPath.value = findRegionPath(selected.id)
  if (mode.value === 'slope') void loadSlopeUnits()
}

function selectNode(node, path) {
  if (!node) return
  slopeRequestId += 1
  selectedPath.value = path
  selectedSlope.value = null
  if (mode.value === 'slope') slopeUnitLayer.clear()
  mapRef.value?.getAreaSearch()?.handleSearchSelect(node)
}

function selectRegionLevel(index, id) {
  if (!id) {
    const path = selectedPath.value.slice(0, index + 1)
    selectNode(path.at(-1), path)
    return
  }
  const node = optionsAt(index).find((item) => String(item.id) === String(id))
  if (node) selectNode(node, [...selectedPath.value.slice(0, index + 1), node])
}

function searchRegion(id) {
  const item = searchableRegions.value.find(({ node }) => String(node.id) === String(id))
  if (item) selectNode(item.node, item.path)
}

function resetRegion() {
  searchRegionId.value = ''
  if (root.value) selectNode(root.value, findRegionPath(root.value.id))
}

function changeMode(nextMode) {
  if (mode.value === nextMode) return
  if (mode.value === 'point') {
    selectedPoint.value = null
    mapRef.value?.clearRiskPoint()
  }
  if (mode.value === 'slope') {
    selectedSlope.value = null
    slopeUnitLayer.clear()
  }
  mode.value = nextMode
  if (nextMode === 'slope') void loadSlopeUnits()
}

async function loadSlopeUnits() {
  const region = selectedRegion.value
  const regionKey = SLOPE_UNIT_KEY[region?.level]
  const requestId = ++slopeRequestId
  selectedSlope.value = null
  slopeUnitLayer.clear()

  if (!regionKey || !region?.name) {
    return
  }

  try {
    const response = await getSlopeUnitList({ [regionKey]: region.name })
    if (requestId !== slopeRequestId || mode.value !== 'slope') return
    const slopes = Array.isArray(response) ? response : []
    slopeUnitLayer.draw(slopes)
  } catch (error) {
    if (requestId !== slopeRequestId || mode.value !== 'slope') return
    console.error('加载斜坡单元失败', error)
  }
}

function handleMapClick(event) {
  if (mode.value === 'slope') {
    if (!selectedSlope.value || slopeUnitLayer.isSelectedEvent(event)) return
    // 在已选斜坡以外点击即视为取消，恢复全部候选斜坡。
    selectedSlope.value = null
    slopeUnitLayer.restoreAll()
    return
  }
  if (mode.value !== 'point') return
  if (!event?.cartesian) {
    ElMessage.warning('当前位置无法获取坐标，请重新选择')
    return
  }
  const point = mars3d.LngLatPoint.fromCartesian(event.cartesian)
  const longitude = Number(point?.lng)
  const latitude = Number(point?.lat)
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return
  selectedPoint.value = { longitude, latitude }
  mapRef.value?.showRiskPoint(
    { lng: longitude, lat: latitude, alt: Number(point.alt) || 0 },
    { sampleTerrain: false, flyTo: false, alwaysVisible: true },
  )
}

function confirmSelection() {
  if (!canConfirm.value) return
  emit('confirm', mode.value === 'point'
    ? { type: 'point', point: { ...selectedPoint.value } }
    : { type: 'slope', slope: { id: selectedSlope.value.id, name: selectedSlope.value.name } })
}

function captureSelectionImage() {
  if (!mapRef.value?.captureImage) throw new Error('地图截图能力不可用')
  return mapRef.value.captureImage({ type: 'image/png' })
}

onBeforeUnmount(() => {
  slopeRequestId += 1
})

defineExpose({ captureSelectionImage })
</script>

<style lang="less" scoped src="./index.less"></style>
