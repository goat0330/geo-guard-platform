<template>
  <div ref="mapContainerRef" class="dynamic-risk-map"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import maplibregl from 'maplibre-gl'
import wellknown from 'wellknown'
import { bbox, centerOfMass } from '@turf/turf'
import chongqingBoundary from '@/assets/data/chongqing-boundary.json'
import { RISK_LEVEL_COLOR, RISK_LEVEL_TEXT } from '@/utils/enum.js'
import 'maplibre-gl/dist/maplibre-gl.css'

defineOptions({ name: 'DynamicRiskMap' })

const props = defineProps({
  units: {
    type: Array,
    default: () => [],
  },
  preserveDrawingBuffer: Boolean,
  interactive: {
    type: Boolean,
    default: true,
  },
  attributionControl: {
    type: Boolean,
    default: true,
  },
  levelField: {
    type: String,
    default: 'dynamicRiskLevel',
  },
  levelColors: {
    type: Object,
    default: () => RISK_LEVEL_COLOR,
  },
  riskTypeName: {
    type: String,
    default: '风险',
  },
  activeLevels: {
    type: Array,
    default: () => [4, 3, 2, 1],
  },
})

const emit = defineEmits(['ready'])
const mapContainerRef = ref(null)
let map = null
let popup = null
let hoveredId = null
let resizeObserver = null
let resizeFrame = null

const SOURCE_ID = 'dynamic-risk-units'
const FILL_LAYER_ID = 'dynamic-risk-units-fill'
const LINE_LAYER_ID = 'dynamic-risk-units-line'
const LABEL_LAYER_ID = 'dynamic-risk-units-label'
const TDT_TOKEN = 'a15c655473ce0c05d18e9aba7506de12'

const toFeatureCollection = (units, config = {}) => {
  const levelField = config.levelField ?? props.levelField
  const levelColors = config.levelColors ?? props.levelColors
  const activeLevels = config.activeLevels ?? props.activeLevels

  return {
    type: 'FeatureCollection',
    features: units.map((item, index) => {
    let geometry = null
    try {
      geometry = wellknown.parse(item?.wkt || '')
    } catch {
      geometry = null
    }
    if (!geometry) return null
    const riskLevel = Number(item?.[levelField])
    if (!activeLevels.includes(riskLevel)) return null
    return {
      type: 'Feature',
      id: String(item?.id ?? item?.slopeUnitId ?? index),
      geometry,
      properties: {
        id: String(item?.id ?? item?.slopeUnitId ?? ''),
        name: String(item?.name || item?.slopeUnitName || item?.slopeName || '').trim(),
        street: item?.street ?? '',
        village: item?.village ?? '',
        area: item?.area ?? '',
        riskLevel,
        riskLevelText: RISK_LEVEL_TEXT[riskLevel] || '--',
        fillColor: levelColors[riskLevel] || '#A6ACB8',
      },
    }
  }).filter(Boolean),
  }
}

const formatArea = (value) => {
  const area = Number(value)
  if (!Number.isFinite(area)) return '--'
  return area >= 1000000 ? `${(area / 1000000).toFixed(2)} km²` : `${area.toFixed(2)} m²`
}

const createPopupRow = (label, value) => {
  const row = document.createElement('div')
  row.className = 'risk-popup-row'
  const labelElement = document.createElement('span')
  labelElement.textContent = label
  const valueElement = document.createElement('strong')
  valueElement.textContent = value || '--'
  row.append(labelElement, valueElement)
  return row
}

const createPopupContent = (properties) => {
  const wrapper = document.createElement('div')
  wrapper.className = 'risk-popup'
  const header = document.createElement('div')
  header.className = 'risk-popup-header'
  const title = document.createElement('strong')
  const unitTitle = properties.name || (properties.id ? `${properties.id}号` : '') || '--'
  const formattedTitle = unitTitle.endsWith('斜坡单元') || unitTitle.endsWith('斜坡')
    ? unitTitle
    : `${unitTitle}斜坡单元`
  title.textContent = `${formattedTitle}${props.riskTypeName}详情`
  const tag = document.createElement('span')
  tag.textContent = `${properties.riskLevelText || '--'}${props.riskTypeName}`
  tag.style.backgroundColor = properties.fillColor || '#A6ACB8'
  header.append(title, tag)
  wrapper.append(
    header,
    createPopupRow('斜坡面积', formatArea(properties.area)),
    createPopupRow('所属乡镇', properties.street),
    createPopupRow('所属村', properties.village),
  )
  return wrapper
}

const showPopup = (event) => {
  const feature = event.features?.[0]
  if (!feature) return
  map.getCanvas().style.cursor = 'pointer'
  const center = centerOfMass(feature).geometry.coordinates
  popup.setLngLat(center).setDOMContent(createPopupContent(feature.properties)).addTo(map)
}

const clearHover = () => {
  if (hoveredId !== null) map.setFeatureState({ source: SOURCE_ID, id: hoveredId }, { hover: false })
  hoveredId = null
  map.getCanvas().style.cursor = ''
  popup?.remove()
}

const updateHover = (event) => {
  const feature = event.features?.[0]
  if (!feature) return
  if (hoveredId !== null) map.setFeatureState({ source: SOURCE_ID, id: hoveredId }, { hover: false })
  hoveredId = feature.id
  map.setFeatureState({ source: SOURCE_ID, id: hoveredId }, { hover: true })
  showPopup(event)
}

const drawUnits = (config, units = props.units) => {
  if (!map?.isStyleLoaded()) return
  const resolvedConfig = (config && !Array.isArray(config) && typeof config === 'object') ? config : {}
  const targetUnits = Array.isArray(units) ? units : props.units
  const data = toFeatureCollection(targetUnits, resolvedConfig)
  const source = map.getSource(SOURCE_ID)
  if (source) {
    source.setData(data)
  } else {
    map.addSource(SOURCE_ID, { type: 'geojson', data, promoteId: 'id' })
    map.addLayer({
      id: FILL_LAYER_ID,
      type: 'fill',
      source: SOURCE_ID,
      paint: {
        'fill-color': ['get', 'fillColor'],
        'fill-opacity': ['case', ['boolean', ['feature-state', 'hover'], false], 0.95, 0.72],
      },
    })
    map.addLayer({
      id: LINE_LAYER_ID,
      type: 'line',
      source: SOURCE_ID,
      paint: {
        'line-color': '#ffffff',
        'line-opacity': ['case', ['boolean', ['feature-state', 'hover'], false], 1, 0.28],
        'line-width': ['case', ['boolean', ['feature-state', 'hover'], false], 2, 1],
      },
    })
    map.addLayer({
      id: LABEL_LAYER_ID,
      type: 'symbol',
      source: SOURCE_ID,
      minzoom: 13,
      layout: {
        'text-field': [
          'case',
          ['!=', ['get', 'name'], ''],
          ['get', 'name'],
          ['concat', ['get', 'id'], '斜坡'],
        ],
        'text-size': 14,
        'text-anchor': 'center',
      },
      paint: { 'text-color': '#ffffff', 'text-halo-color': '#383C41', 'text-halo-width': 1 },
    })
    map.on('mousemove', FILL_LAYER_ID, updateHover)
    map.on('mouseleave', FILL_LAYER_ID, clearHover)
  }
  if (data.features.length) {
    const bounds = bbox(data)
    map.fitBounds([[bounds[0], bounds[1]], [bounds[2], bounds[3]]], { padding: 36, duration: 0, maxZoom: 13 })
  }
}

const exportImage = async () => {
  if (!map) return ''
  // setData 后 map.loaded() 仍可能为 true；必须等待 source 解析并等待 idle，确保矢量斜坡面已实际绘制到画布。
  await new Promise((resolve) => {
    let settled = false
    let timeout = null
    const finish = () => {
      if (settled) return
      settled = true
      window.clearTimeout(timeout)
      map?.off('idle', onIdle)
      map?.off('sourcedata', onSourceData)
      resolve()
    }

    const onSourceData = (event) => {
      if (event?.sourceId === SOURCE_ID && event?.isSourceLoaded) {
        map?.triggerRepaint()
      }
    }

    const onIdle = () => {
      if (!map?.getSource(SOURCE_ID) || map?.isSourceLoaded(SOURCE_ID)) {
        finish()
      }
    }

    timeout = window.setTimeout(finish, 3500)
    map.on('idle', onIdle)
    map.on('sourcedata', onSourceData)
    map.triggerRepaint()
  })
  return map.getCanvas().toDataURL('image/png')
}

const renderAndExport = async (config, units = props.units) => {
  map?.resize()
  drawUnits(config, units)
  return exportImage()
}

const showUnit = (unit) => {
  if (!map || !unit) return
  const feature = toFeatureCollection([unit]).features[0]
  if (!feature) return
  const center = centerOfMass(feature).geometry.coordinates
  popup.setLngLat(center).setDOMContent(createPopupContent(feature.properties)).addTo(map)
  map.flyTo({ center, zoom: Math.max(map.getZoom(), 13), duration: 800 })
}

const scheduleResize = () => {
  if (resizeFrame) return
  resizeFrame = requestAnimationFrame(() => {
    resizeFrame = null
    map?.resize()
  })
}

onMounted(() => {
  map = new maplibregl.Map({
    container: mapContainerRef.value,
    center: [107.88, 30.06],
    zoom: 7,
    interactive: props.interactive,
    attributionControl: props.attributionControl,
    canvasContextAttributes: { preserveDrawingBuffer: props.preserveDrawingBuffer, antialias: false },
    style: {
      version: 8,
      sources: {
        'tdt-imagery': {
          type: 'raster',
          // 浏览器直连天地图官方服务（支持 HTTPS 与 CORS 跨域），多节点并发轮询加速，避免内网 Nginx 502
          tiles: [
            `https://t0.tianditu.gov.cn/img_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=img&tileMatrixSet=w&TileMatrix={z}&TileRow={y}&TileCol={x}&style=default&format=tiles&tk=${TDT_TOKEN}`,
            `https://t1.tianditu.gov.cn/img_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=img&tileMatrixSet=w&TileMatrix={z}&TileRow={y}&TileCol={x}&style=default&format=tiles&tk=${TDT_TOKEN}`,
            `https://t2.tianditu.gov.cn/img_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=img&tileMatrixSet=w&TileMatrix={z}&TileRow={y}&TileCol={x}&style=default&format=tiles&tk=${TDT_TOKEN}`,
            `https://t3.tianditu.gov.cn/img_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=img&tileMatrixSet=w&TileMatrix={z}&TileRow={y}&TileCol={x}&style=default&format=tiles&tk=${TDT_TOKEN}`,
          ],
          tileSize: 256,
          maxzoom: 18,
        },
        'chongqing-boundary': { type: 'geojson', data: chongqingBoundary },
      },
      layers: [
        { id: 'background', type: 'background', paint: { 'background-color': '#617185' } },
        { id: 'tdt-imagery', type: 'raster', source: 'tdt-imagery', paint: { 'raster-opacity': 1 } },
        { id: 'chongqing-boundary-fill', type: 'fill', source: 'chongqing-boundary', paint: { 'fill-color': '#007BFF', 'fill-opacity': 0.04 } },
        { id: 'chongqing-boundary-line', type: 'line', source: 'chongqing-boundary', paint: { 'line-color': '#DCEDFF', 'line-width': 2 } },
      ],
    },
  })
  resizeObserver = new ResizeObserver(scheduleResize)
  resizeObserver.observe(mapContainerRef.value)
  popup = new maplibregl.Popup({ className: 'dynamic-risk-popup', closeButton: false, closeOnClick: false, maxWidth: 'none' })
  map.on('load', () => {
    drawUnits()
    scheduleResize()
    emit('ready', map)
  })
})

watch(
  () => [props.units, props.levelField, props.levelColors, props.activeLevels],
  () => drawUnits(),
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  if (resizeFrame) cancelAnimationFrame(resizeFrame)
  popup?.remove()
  if (map?.getLayer(FILL_LAYER_ID)) {
    map.off('mousemove', FILL_LAYER_ID, updateHover)
    map.off('mouseleave', FILL_LAYER_ID, clearHover)
  }
  map?.remove()
  popup = null
  map = null
})

defineExpose({ exportImage, renderAndExport, showUnit, resize: () => map?.resize() })
</script>

<style lang="less" scoped>
.dynamic-risk-map { width: 100%; height: 100%; }
:deep(.maplibregl-ctrl-bottom-left), :deep(.maplibregl-ctrl-bottom-right) { display: none; }
</style>

<style lang="less">
.dynamic-risk-popup .maplibregl-popup-content { min-width: 276px; padding: 14px; border: 1px solid #ffffff; border-radius: 6px; background: rgba(241, 243, 252, 0.9); backdrop-filter: blur(4px); }
.dynamic-risk-popup .maplibregl-popup-tip { border-top-color: rgba(241, 243, 252, 0.9); }
.risk-popup { color: #617185; font-size: 12px; }
.risk-popup-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px solid rgba(255, 255, 255, 0.7); }
.risk-popup-header strong { color: #222527; font-size: 14px; font-weight: 500; }
.risk-popup-header span { flex: 0 0 auto; padding: 4px 6px; border-radius: 4px; color: #ffffff; }
.risk-popup-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 24px; }
.risk-popup-row strong { color: #383C41; font-weight: 500; text-align: right; }
</style>
