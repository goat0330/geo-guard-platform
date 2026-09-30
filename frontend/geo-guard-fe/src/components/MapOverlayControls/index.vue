<template>
  <div class="map-overlay-controls">
    <div v-if="showFilters" class="filter-groups">
      <section class="filter-group">
        <button class="filter-title" type="button" @click="layerExpanded = !layerExpanded">
          <i class="iconfont icon-layer title-icon"></i>
          <span>图层</span>
          <CaretBottom class="caret" :class="{ 'is-collapsed': !layerExpanded }" />
        </button>
        <div class="filter-content" :class="{ 'is-collapsed': !layerExpanded }">
          <div class="filter-list">
            <label
              v-for="item in activeLayers"
              :key="item.label"
              class="filter-item"
              :class="{ 'is-checked': item.checked }"
            >
              <span class="item-name">
                <img v-if="item.image" :src="item.image" class="item-icon item-icon-img" alt="" />
                <svg v-else class="item-icon" aria-hidden="true">
                  <use :href="`#${item.icon}`"></use>
                </svg>
                {{ item.label }}
              </span>
              <div class="custom-checkbox">
                <input v-model="item.checked" type="checkbox" @change="handleLayerToggle(item)" />
                <span class="custom-checkbox-inner"></span>
              </div>
            </label>
          </div>
        </div>
      </section>

      <section v-if="showRisk" class="filter-group risk-group">
        <button class="filter-title" type="button" @click="riskExpanded = !riskExpanded">
          <span>动态风险</span>
          <CaretBottom class="caret" :class="{ 'is-collapsed': !riskExpanded }" />
        </button>
        <div class="filter-content" :class="{ 'is-collapsed': !riskExpanded }">
          <div class="filter-list">
            <label
              v-for="item in riskOptions"
              :key="item.label"
              class="filter-item"
              :class="{ 'is-checked': isRiskLevelChecked(item.level) }"
            >
              <span class="item-name">
                <svg class="item-icon risk-icon" aria-hidden="true">
                  <use :href="`#${item.icon}`"></use>
                </svg>
                {{ item.label }}
              </span>
              <div class="custom-checkbox">
                <input
                  :checked="isRiskLevelChecked(item.level)"
                  type="checkbox"
                  @change="toggleRisk(item.level, $event.target.checked)"
                />
                <span class="custom-checkbox-inner"></span>
              </div>
            </label>
          </div>
        </div>
      </section>
    </div>

    <div class="map-tools">
      <button
        class="tool-button location-button"
        type="button"
        title="定位到重庆市"
        aria-label="定位到重庆市"
        @click="$emit('locate-home')"
      >
        <img :src="pointIcon" alt="" />
      </button>

      <div class="zoom-controls">
        <button type="button" title="放大" aria-label="放大" @click="$emit('zoom-in')">
          <img :src="plusIcon" alt="" />
        </button>
        <span></span>
        <button type="button" title="缩小" aria-label="缩小" @click="$emit('zoom-out')">
          <img :src="subtractionIcon" alt="" />
        </button>
      </div>

      <button
        v-if="showBaseMapControl"
        class="tool-button"
        :class="{ 'is-active': mapSelectorExpanded }"
        type="button"
        title="图层控制"
        aria-label="图层控制"
        @click="mapSelectorExpanded = !mapSelectorExpanded"
      >
        <i class="iconfont icon-folder-box"></i>
      </button>

      <Transition v-if="showBaseMapControl" name="map-selector">
        <div v-if="mapSelectorExpanded" class="map-selector">
          <div class="map-selector-title">图层控制</div>
          <div class="map-list">
            <div
              v-for="item in mapOptions"
              :key="item.label"
              class="map-item"
              :class="{ 'is-active': activeMap === item.value }"
              @click="handleMapChange(item)"
            >
              <label v-if="['pilot', 'imagery'].includes(item.value)" class="imagery-label" @click.stop>
                <input
                  :checked="getImageryLabelVisible(item.value)"
                  type="checkbox"
                  @change="handleImageryLabelChange(item.value, $event.target.checked)"
                />
                <span>影像注记</span>
              </label>
              <img :src="item.image" alt="" />
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CaretBottom } from '@element-plus/icons-vue'
import pointIcon from '@/assets/imgs/map-controls/point.png'
import plusIcon from '@/assets/imgs/plus-icon.png'
import subtractionIcon from '@/assets/imgs/subtraction-icon.png'
import pilotMap from '@/assets/imgs/map-controls/pilot-map.png'
import imageryMap from '@/assets/imgs/map-controls/imagery-map.png'
import vectorMap from '@/assets/imgs/map-controls/vector-map.png'

const emit = defineEmits([
  'update:riskLevels',
  'zoom-in',
  'zoom-out',
  'locate-home',
  'change-base-map',
  'toggle-imagery-label',
  'change-layer',
])

const layerExpanded = ref(true)
const riskExpanded = ref(true)
const mapSelectorExpanded = ref(false)
const activeMap = ref('pilot')
const showPilotImageryLabel = ref(false)
const showTdtImageryLabel = ref(false)

const layerOptions = ref([
  { key: 'monitor', label: '监测设备', icon: 'icon-a-Frame7', checked: true },
  { key: 'hazard_point', label: '隐患点', icon: 'icon-risk-point', checked: false },
  { key: 'risk_area', label: '风险区', icon: 'icon-lightning-point', checked: false },
  { key: 'disaster_body', label: '承灾体', icon: 'icon-area-disaster-body', checked: false },
  { key: 'rain', label: '实时雨量分布', icon: 'icon-rain-point', checked: false },
])

const props = defineProps({
  // 空间分析等仅需要地图工具的场景可隐藏右上角筛选面板。
  showFilters: {
    type: Boolean,
    default: true,
  },
  // 空间分析只保留定位与缩放，避免额外底图入口干扰选取流程。
  showBaseMapControl: {
    type: Boolean,
    default: true,
  },
  riskLevels: {
    type: Array,
    default: () => [],
  },
  showRisk: {
    type: Boolean,
    default: true,
  },
  layers: {
    type: Array,
    default: null,
  },
  customLayers: {
    type: Array,
    default: null,
  },
})

const activeLayers = computed(() => {
  return props.layers || props.customLayers || layerOptions.value
})
const riskOptions = [
  { label: '极高风险', icon: 'icon-v-high-risk', level: 4 },
  { label: '高风险', icon: 'icon-high-risk', level: 3 },
  { label: '中风险', icon: 'icon-mid-risk', level: 2 },
  { label: '低风险', icon: 'icon-low-risk', level: 1 },
]
const allRiskLevels = riskOptions.map((item) => item.level)

const isRiskLevelChecked = (level) => {
  return Array.isArray(props.riskLevels) && props.riskLevels.includes(level)
}

const toggleRisk = (level, checked) => {
  const currentLevels = Array.isArray(props.riskLevels) ? props.riskLevels : []
  const nextLevels = checked
    ? [...new Set([...currentLevels, level])]
    : currentLevels.filter((value) => value !== level)
  emit('update:riskLevels', nextLevels)
}

const mapOptions = [
  { label: '天地图矢量', value: 'vector', image: vectorMap },
  { label: '全市影像', value: 'pilot', image: pilotMap },
  { label: '天地图影像', value: 'imagery', image: imageryMap },
]

const handleMapChange = (item) => {
  activeMap.value = item.value
  emit('change-base-map', item.value)
  // 底图切换后仅恢复当前底图自己的注记状态。
  if (item.value === 'pilot' || item.value === 'imagery') {
    emit('toggle-imagery-label', {
      type: item.value,
      show: getImageryLabelVisible(item.value),
    })
  }
}

const getImageryLabelVisible = (type) => {
  return type === 'pilot' ? showPilotImageryLabel.value : showTdtImageryLabel.value
}

const handleImageryLabelChange = (type, checked) => {
  if (type === 'pilot') {
    showPilotImageryLabel.value = checked
  } else {
    showTdtImageryLabel.value = checked
  }
  emit('toggle-imagery-label', { type, show: checked })
}

/**
 * 切换图层勾选状态并派发图层变更事件
 * 优先通过 Vue 事件通知父组件，同时兼容挂载在 window._map 上的图层变更处理器
 * @param {Object} item 勾选的图层配置项
 */
const handleLayerToggle = (item) => {
  const payload = {
    key: item.key,
    label: item.label,
    checked: item.checked,
    layers: activeLayers.value,
  }
  emit('change-layer', payload)

  // 兜底直接调用地图实例上的图层变更处理器
  if (typeof window !== 'undefined' && window._map?.handleLayerChange) {
    window._map.handleLayerChange(payload)
  }
}

defineExpose({
  layerOptions: activeLayers,
  getData: () => ({
    layers: activeLayers.value.filter((item) => item.checked).map((item) => item.key),
    layerOptions: activeLayers.value,
  }),
})
</script>

<style lang="less" scoped>
.map-overlay-controls {
  position: absolute;
  inset: 0;
  z-index: 8;
  pointer-events: none;
  color: #ffffff;
}

.filter-groups {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
  pointer-events: auto;
}

.filter-group {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.filter-title,
.filter-list,
.tool-button,
.zoom-controls {
  border: 1px solid #ffffff80;
  background: #8080804d;
  backdrop-filter: blur(10px);
}

.filter-title {
  min-width: 96px;
  height: 36px;
  padding: 0 10px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: #ffffff;
  font-size: 12px;
  cursor: pointer;
  box-sizing: border-box;
  user-select: none;
  transition: all 0.2s ease;

  &:hover {
    background: #80808080;
  }
}

.title-icon,
.caret {
  width: 14px;
  height: 14px;
}

.caret {
  margin-left: 2px;
  transform: rotate(180deg);
  transition: transform 0.3s ease;

  &.is-collapsed {
    transform: rotate(0);
  }
}

.filter-content {
  display: grid;
  grid-template-rows: 1fr;
  margin-top: 8px;
  opacity: 1;
  transition:
    grid-template-rows 0.3s ease,
    opacity 0.2s ease,
    margin-top 0.3s ease;

  &.is-collapsed {
    grid-template-rows: 0fr;
    margin-top: 0;
    opacity: 0;
  }
}

.filter-list {
  width: 142px;
  min-height: 0;
  padding: 8px 10px;
  border-radius: 6px;
  overflow: hidden;
  box-sizing: border-box;
}

.filter-item {
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease;

  &:hover {
    color: #ffffff;
  }

  &.is-checked {
    color: #ffffff;
  }
}

.item-name {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.item-icon {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  overflow: visible;
}

.item-icon-img {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  object-fit: contain;
  display: block;
}

.risk-icon {
  width: 16px;
  height: 16px;
  flex-basis: 16px;
}

.custom-checkbox {
  position: relative;
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  input {
    position: absolute;
    inset: 0;
    opacity: 0;
    margin: 0;
    cursor: pointer;
    z-index: 1;
  }

  .custom-checkbox-inner {
    width: 14px;
    height: 14px;
    border: 1px solid #ffffff80;
    border-radius: 2px;
    background: #8080804d;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    &::after {
      content: '';
      width: 3px;
      height: 7px;
      border: solid #ffffff;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg) scale(0);
      transition: transform 0.15s ease-in-out;
      margin-top: -2px;
    }
  }
}

.filter-item.is-checked .custom-checkbox .custom-checkbox-inner {
  border-color: #ffffff;
  background: #007bff;

  &::after {
    transform: rotate(45deg) scale(1);
  }
}

.map-tools {
  position: absolute;
  right: 16px;
  bottom: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  pointer-events: auto;
}

.tool-button,
.zoom-controls {
  width: 34px;
  border-radius: 6px;
  box-sizing: border-box;
  color: #ffffff;
}

.tool-button {
  height: 34px;
  padding: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #80808080;
  }

  img {
    width: 20px;
    height: 20px;
    display: block;
  }

  .icon-folder-box {
    font-size: 20px;
    line-height: 1;
  }
}

.zoom-controls {
  padding: 7px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.zoom-controls button {
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 16px;
    height: 16px;
    display: block;
  }
}

.zoom-controls span {
  width: 100%;
  height: 1px;
  background: rgba(255, 255, 255, 0.2);
  margin: 7px 0;
}

.map-selector {
  position: absolute;
  right: 52px;
  bottom: 0;
  width: 320px;
  padding: 10px;
  border-radius: 6px;
  box-sizing: border-box;
  background: #8080804d;
  backdrop-filter: blur(10px);
  border: 1px solid #ffffff80;
  color: #ffffff;
  transform-origin: right bottom;
}

.map-selector-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.map-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.map-item {
  position: relative;
  height: 66px;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  box-sizing: border-box;
  cursor: pointer;

  &.is-active {
    border-color: #007bff;
  }

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  > span {
    position: absolute;
    right: 0;
    bottom: 0;
    padding: 2px 6px;
    border-radius: 4px 0 0;
    background: rgba(0, 0, 0, 0.5);
    color: #ffffff;
    font-size: 10px;
  }

  &.is-active > span {
    background: #007bff;
  }
}

.imagery-label {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: 100%;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.45);
  color: #ffffff;
  font-size: 10px;
  cursor: pointer;

  input {
    width: 10px;
    height: 10px;
    margin: 0;
    accent-color: #007bff;
  }
}

.map-selector-enter-active,
.map-selector-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.22s ease;
}

.map-selector-enter-from,
.map-selector-leave-to {
  opacity: 0;
  transform: translateX(12px) scale(0.96);
}
</style>
