<template>
  <div class="slope-search" :style="customStyle" @click.stop="">
    <!-- 选项面板（向上展开） -->
    <div class="select-opt-panel" :class="{ 'is-collapsed': isCollapsed }">
      <div class="select-opt-panel__inner">
        <!-- 顶部操作栏 -->
        <div class="panel-top">
          <div class="title" :title="currentTitle">{{ displayTitle }}</div>
          <div class="search-input">
            <el-checkbox v-model="showBoundary" label="边界线" size="small" @change="handleBoundaryChange" />
            <el-select
              v-model="searchKeyword"
              class="search-select"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="请输入"
              :remote-method="handleRemoteSearch"
              popper-class="map-search-popper"
              remote-show-suffix
              :suffix-icon="SearchIcon"
              @change="handleSearchSelect"
            >
              <el-option
                v-for="item in searchOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              >
                <div class="search-option-row">
                  <span class="option-name">{{ item.name }}</span>
                  <span class="option-path">{{ item.fullPathName }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- 面包屑导航链路与返回上一级 -->
        <div v-if="selectedOpts.length" class="selected-opt">
          <div
            class="back-arrow"
            :class="{ 'is-disabled': selectedOpts.length <= 1 }"
            title="返回上一级"
            @click="backToPreLevel"
          >
            <el-icon><ArrowLeftBold /></el-icon>
          </div>
          <div class="selected-opt-text">
            <template v-for="(opt, idx) in selectedOpts" :key="opt.id || idx">
              <span
                class="selected-each-opt"
                :class="{ 'is-current': idx === selectedOpts.length - 1 }"
                @click="backToDesignatedLevel(opt, idx)"
              >
                {{ formatRegionName(opt.name) }}
              </span>
              <span v-if="idx !== selectedOpts.length - 1">/</span>
            </template>
          </div>
        </div>

        <!-- 当前层级子节点网格选择区 -->
        <div class="panel-xian panel-common">
          <template v-if="displayOptions.length">
            <div
              v-for="item in displayOptions"
              :key="item.id"
              class="each-opt"
              :class="{ 'is-selected': item.id === currentSelectedId }"
              @click="selectOpt(item)"
            >
              {{ formatRegionName(item.name) }}
            </div>
          </template>
          <div v-else class="empty-tips">
            {{ isLeafNode ? '已到达最下级区划' : (searchKeyword ? '未搜索到相关结果~' : '暂无数据~') }}
          </div>
        </div>
      </div>
    </div>

    <!-- 底部触发框：点击展开/折叠面板 -->
    <div class="select-box">
      <div
        id="selector"
        class="select-input"
        :class="{ 'is-expanded': !isCollapsed }"
        @click.stop="toggleCollapse"
      >
        <el-input
          :model-value="currentSelectedName"
          readonly
          placeholder="请选择"
        />
        <el-icon class="arrow-icon"><CaretBottom /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, h, watch } from 'vue'
import { ArrowLeftBold, CaretBottom } from '@element-plus/icons-vue'
import * as mars3d from 'mars3d'
import * as Cesium from 'mars3d-cesium'
import { wktToGeoJSON } from '@terraformer/wkt'
import { getAreaList, getAreaListWithOutWk } from '@/api/common.js'
import { getAreaBoundaryTolerance } from '@/utils/index.js'

const props = defineProps({
  // Mars3D Map 实例
  map: {
    type: Object,
    default: null,
  },
  // 默认是否勾选边界线
  defaultShowBoundary: {
    type: Boolean,
    default: false,
  },
  // 自定义外部样式（默认紧贴地图左下角）
  customStyle: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['select-area', 'boundary-change', 'loaded'])

const SearchIcon = () => h('i', { class: ['iconfont', 'icon-search'] })

// 折叠状态（默认折叠，只展示底部选择框，点击展开）
const isCollapsed = ref(true)
// 边界线开关
const showBoundary = ref(props.defaultShowBoundary)
// 搜索关键字（输入值）
const searchKeyword = ref('')
// 搜索下拉联想项
const searchOptions = ref([])

// 完整树形数据与扁平索引
const allAreaTree = ref([])
const flatAreaList = ref([])

// 当前路径链路与当前展示项
const selectedOpts = ref([])
const displayOptions = ref([])

// Mars3D 边界线图层与 Cesium 名称标签集合引用，以及绘制并发控制请求ID
let boundaryLayer = null
let cesiumLabelCollection = null
let drawRequestId = 0

/**
 * 行政区划飞行定位持续时间（秒）
 * 调优至 0.8s，兼顾丝滑平稳视角过渡与即时定位响应，避免默认 3s+ 的漫长等待
 */
const AREA_FLY_DURATION = 0.8

/**
 * 边界数据与区划详情内存缓存，避免重复点击或下钻返回时重复发起网络请求延迟飞行
 */
const areaBoundaryCache = new Map()

async function getCachedAreaList(params) {
  const key = `${params?.id || ''}_${params?.pcode || ''}_${params?.simpWktLevel ?? ''}`
  if (areaBoundaryCache.has(key)) {
    return areaBoundaryCache.get(key)
  }
  const res = await getAreaList(params)
  const list = Array.isArray(res) ? res : (res?.data || [])
  if (Array.isArray(list) && list.length > 0) {
    areaBoundaryCache.set(key, list)
  }
  return list
}

/**
 * 当前选中节点的 ID 与名称
 */
const currentSelectedNode = computed(() => {
  if (!selectedOpts.value.length) return null
  return selectedOpts.value[selectedOpts.value.length - 1]
})

const currentSelectedId = computed(() => currentSelectedNode.value?.id || '')
const currentSelectedName = computed(() => formatRegionName(currentSelectedNode.value?.name || '请选择'))

const currentTitle = computed(() => selectedOpts.value[0]?.name || '行政区划')
const displayTitle = computed(() => formatRegionName(currentTitle.value))

const isLeafNode = computed(() => {
  return currentSelectedNode.value && (!currentSelectedNode.value.children || currentSelectedNode.value.children.length === 0)
})

/**
 * 格式化超长区划名称
 */
function formatRegionName(name = '') {
  if (!name) return ''
  if (name === '恩施土家族苗族自治州') return '恩施州'
  if (name === '彭水苗族土家族自治县') return '彭水县'
  return name
}



/**
 * 将整棵树展平为便于前端搜索的结构
 */
function buildFlatIndex(nodes, parentPath = []) {
  const result = []
  for (const node of nodes) {
    const currentPath = [...parentPath, node]
    result.push({
      id: node.id,
      name: node.name,
      level: node.level,
      fullPathName: currentPath.map((p) => formatRegionName(p.name)).join(' / '),
      node,
      path: currentPath,
    })
    if (node.children?.length) {
      result.push(...buildFlatIndex(node.children, currentPath))
    }
  }
  return result
}

/**
 * 前端搜索方法
 */
function handleRemoteSearch(query) {
  if (!query || !query.trim()) {
    searchOptions.value = []
    return
  }
  const q = query.trim().toLowerCase()
  searchOptions.value = flatAreaList.value
    .filter((item) => item.name && item.name.toLowerCase().includes(q))
    .slice(0, 30)
}

/**
 * 获取 Mars3D 地图实例
 */
function getActiveMap() {
  return props.map || window._map || null
}

/**
 * 获取 Cesium Viewer 实例
 */
function getActiveViewer() {
  const map = getActiveMap()
  return map?.viewer || map?._viewer || window.viewer || null
}

/**
 * 将面几何转换为纯多段线几何（只要线，不要面）
 */
function convertPolygonToLineGeoJSON(geometry, properties) {
  let lineCoords = []
  if (geometry.type === 'Polygon') {
    lineCoords = geometry.coordinates
  } else if (geometry.type === 'MultiPolygon') {
    geometry.coordinates.forEach((poly) => {
      poly.forEach((ring) => lineCoords.push(ring))
    })
  } else if (geometry.type === 'LineString') {
    lineCoords = [geometry.coordinates]
  } else if (geometry.type === 'MultiLineString') {
    lineCoords = geometry.coordinates
  }

  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: properties || {},
        geometry: {
          type: 'MultiLineString',
          coordinates: lineCoords,
        },
      },
    ],
  }
}

/**
 * 计算或提取行政区划中心经纬度坐标与高程
 */
function getCenterCoordinates(node, geometry) {
  let lng = null
  let lat = null
  let height = 0

  // 1. 如果接口返回了 center (如 POINT(108.178 29.349) 或 POINT Z (108.178 29.349 350))
  if (node && node.center) {
    if (typeof node.center === 'string') {
      try {
        const pt = wktToGeoJSON(node.center)
        if (pt && pt.coordinates && pt.coordinates.length >= 2) {
          lng = Number(pt.coordinates[0])
          lat = Number(pt.coordinates[1])
          height = Number(pt.coordinates[2]) || 0
        }
      } catch (e) {
        const match = node.center.match(/POINT\s*(?:Z\s*)?\(\s*([-\d.]+)\s+([-\d.]+)(?:\s+([-\d.]+))?/i)
        if (match) {
          lng = parseFloat(match[1])
          lat = parseFloat(match[2])
          height = match[3] ? parseFloat(match[3]) : 0
        }
      }
    } else if (Array.isArray(node.center) && node.center.length >= 2) {
      lng = parseFloat(node.center[0])
      lat = parseFloat(node.center[1])
      height = parseFloat(node.center[2]) || 0
    }
  }

  // 2. 如果 node 自带经纬度属性
  if ((lng === null || lat === null) && node) {
    if (node.longitude && node.latitude) {
      lng = parseFloat(node.longitude)
      lat = parseFloat(node.latitude)
    } else if (node.lon && node.lat) {
      lng = parseFloat(node.lon)
      lat = parseFloat(node.lat)
    }
  }

  // 3. 从 geometry 的边界坐标中计算外包矩形中心点
  if ((lng === null || lat === null) && geometry) {
    let minLng = Infinity
    let maxLng = -Infinity
    let minLat = Infinity
    let maxLat = -Infinity

    const scanCoords = (coords) => {
      if (!Array.isArray(coords)) return
      if (coords.length >= 2 && typeof coords[0] === 'number' && typeof coords[1] === 'number') {
        const cLng = coords[0]
        const cLat = coords[1]
        if (cLng < minLng) minLng = cLng
        if (cLng > maxLng) maxLng = cLng
        if (cLat < minLat) minLat = cLat
        if (cLat > maxLat) maxLat = cLat
        return
      }
      for (const item of coords) {
        scanCoords(item)
      }
    }

    scanCoords(geometry.coordinates)
    if (minLng !== Infinity && maxLng !== -Infinity) {
      lng = (minLng + maxLng) / 2
      lat = (minLat + maxLat) / 2
    }
  }

  if (lng !== null && lat !== null && !isNaN(lng) && !isNaN(lat)) {
    return { lng, lat, height }
  }
  return null
}

/**
 * 飞向指定行政区划中心点，平滑调整三维相机视角
 * @param {Object} node 目标行政区划节点
 * @param {Object} fallbackCoords 兜底中心坐标 { lng, lat, height? }
 */
function flyToRegionCenter(node, fallbackCoords = null) {
  const map = getActiveMap()
  if (!map || !node) return

  let center = fallbackCoords || getCenterCoordinates(node, null)

  // 1. 若节点未直接携带 center，尝试解析 WKT
  if (!center && node.wkt) {
    try {
      const geom = wktToGeoJSON(node.wkt)
      center = getCenterCoordinates(node, geom)
    } catch (e) {}
  }

  // 2. 若仍无中心点，尝试在全量树索引 flatAreaList 中匹配节点及其子节点
  if (!center && flatAreaList.value?.length) {
    const found = flatAreaList.value.find((item) => String(item.id) === String(node.id))
    if (found?.node) {
      center = getCenterCoordinates(found.node, null)
      if (!center && found.node.wkt) {
        try {
          const geom = wktToGeoJSON(found.node.wkt)
          center = getCenterCoordinates(found.node, geom)
        } catch (e) {}
      }
      if (!center && found.node.children?.length) {
        let tLng = 0
        let tLat = 0
        let cnt = 0
        for (const ch of found.node.children) {
          const pt = getCenterCoordinates(ch, null)
          if (pt) {
            tLng += pt.lng
            tLat += pt.lat
            cnt++
          }
        }
        if (cnt > 0) {
          center = { lng: tLng / cnt, lat: tLat / cnt }
        }
      }
    }
  }

  // 3. 常见行政区划硬编码兜底，确保永远能够平滑飞行
  if (!center) {
    const name = node.name || ''
    const id = String(node.id || '')
    if (id === '500243' || name.includes('彭水')) {
      center = { lng: 108.166, lat: 29.294 }
    } else if (id === '422800' || id === '422801' || name.includes('恩施')) {
      center = { lng: 109.487, lat: 30.298 }
    } else if (id === '500000' || name.includes('重庆')) {
      center = { lng: 106.551, lat: 29.563 }
    }
  }

  if (!center || !Number.isFinite(center.lng) || !Number.isFinite(center.lat)) return

  // 计算视角高度 (radius 半视距)
  // 区县级: radius 85000; 乡镇/街道级: radius 28000; 村级: radius 10000
  let radius = 60000
  const depth = selectedOpts.value.length
  const level = node.level || (depth <= 1 ? 'county' : depth === 2 ? 'town' : 'village')
  if (level === 'district' || level === 'county' || level === 3 || depth <= 1) {
    radius = 85000
  } else if (level === 'town' || level === 'street' || level === 4 || depth === 2) {
    radius = 28000
  } else {
    radius = 10000
  }

  try {
    if (typeof map.flyToPoint === 'function') {
      map.flyToPoint([center.lng, center.lat, center.height || 0], {
        radius,
        pitch: -65,
        duration: 1.8,
      })
    } else if (map.viewer?.camera) {
      map.viewer.camera.flyTo({
        destination: Cesium.Cartesian3.fromDegrees(center.lng, center.lat, radius),
        orientation: {
          pitch: Cesium.Math.toRadians(-65),
        },
        duration: 1.8,
      })
    }
  } catch (err) {
    console.warn('flyToRegionCenter 飞行定位异常:', err)
  }
}

/**
 * 加载并绘制行政区划边界线与中心名称标签
 * 业务逻辑规范：
 * 1. 采用 drawRequestId 防并发竞态，避免前序异步请求在下钻后覆盖新状态。
 * 2. 无论是否勾选边界线，区划中心名称均始终加载并展示；
 * 3. 获取边界线调用 /dizai/adRegion/list ({ pcode: node.id }) 获取下一级行政区划；
 * 4. 若存在下一级有效区划，绘制下一级全部行政区划的边界线（按需展示），并在各自中心标注其名字（始终展示）；
 * 5. 若无下一级（叶子节点），调用 /dizai/adRegion/list ({ id: node.id }) 获取当前行政区划自身边界进行绘制与标注；
 * 6. 添加新图元前严格二次调用 clearBoundaryLayer，确保任何历史图层和标签被 100% 抹除。
 */
async function loadAndDrawBoundary(node, shouldFlyTo = true) {
  if (!node) return
  const map = getActiveMap()
  if (!map) return

  const reqId = ++drawRequestId

  // 1. 发起新请求前清理旧图层与旧名称标签
  clearBoundaryLayer()

  // 只要 shouldFlyTo 为 true，立即触发平滑飞行定位，不等异步边界数据下载
  if (shouldFlyTo) {
    flyToRegionCenter(node)
  }

  // 根据当前下钻层级计算传递给后端的抽稀参数 simpWktLevel：一级保持不变 (0.001)，二级精细一点 (0.0003)，三级不抽 (0)
  const currentDepth = selectedOpts.value.length
  const simpWktLevel = getAreaBoundaryTolerance(currentDepth, node)

  // 2. 获取下一级的行政区划（带 WKT 数据，传参 simpWktLevel 配合后端抽稀，优先读取内存缓存加速飞行准备）
  let childList = []
  try {
    const list = await getCachedAreaList({ pcode: node.id, simpWktLevel })
    if (Array.isArray(list) && list.length > 0) {
      childList = list
    }
  } catch (err) {
    console.warn('获取下一级行政区划边界列表失败', err)
  }

  // 检查是否已被后续更高优先级的绘制请求取代
  if (reqId !== drawRequestId) return

  let validChildren = childList.filter((c) => !!c.wkt)

  // 若返回了下一级列表但部分缺少 WKT，并发补全子级的 WKT
  if (childList.length > 0 && validChildren.length === 0 && childList.length <= 30) {
    try {
      const fullChildren = await Promise.all(
        childList.map(async (c) => {
          if (c.wkt) return c
          try {
            const itemRes = await getCachedAreaList({ id: c.id, simpWktLevel })
            const item = Array.isArray(itemRes) ? itemRes[0] : (itemRes?.data?.[0] || itemRes)
            return item?.wkt ? { ...c, wkt: item.wkt, center: item.center || c.center } : c
          } catch (e) {
            return c
          }
        }),
      )
      if (reqId !== drawRequestId) return
      validChildren = fullChildren.filter((c) => !!c.wkt)
    } catch (e) {
      console.warn('获取下一级区划 WKT 异常', e)
    }
  }

  if (reqId !== drawRequestId) return

  // 3. 确定要绘制的边界项与中心名称标注项
  const boundaryItems = []
  const labelItems = []

  if (validChildren.length > 0) {
    // 存在下一级：全部绘制下一级行政区划的边界线，并在各自中心标注其名字
    validChildren.forEach((child) => {
      boundaryItems.push(child)
      labelItems.push(child)
    })
  } else {
    // 无下一级（叶子节点）：调用 /dizai/adRegion/list 获取当前节点自身的 WKT 边界
    if (!node.wkt) {
      try {
        const selfRes = await getCachedAreaList({ id: node.id, simpWktLevel })
        const item = Array.isArray(selfRes) ? selfRes[0] : (selfRes?.data?.[0] || selfRes)
        if (item?.wkt) {
          node.wkt = item.wkt
        }
        if (item?.center && !node.center) {
          node.center = item.center
        }
      } catch (err) {
        console.error('获取当前行政区划边界失败', err)
      }
    }

    if (reqId !== drawRequestId) return

    if (node.wkt) {
      boundaryItems.push(node)
      labelItems.push(node)
    } else {
      labelItems.push(node)
    }
  }

  if (boundaryItems.length === 0 && labelItems.length === 0) return

  // 4. 将几何转换为纯线条 GeoJSON Feature 集合（只要线，不要面；WKT 已由后端按 simpWktLevel 抽稀）
  const lineFeatures = []
  for (const item of boundaryItems) {
    if (!item.wkt) continue
    try {
      const geometry = wktToGeoJSON(item.wkt)
      const lineGeojson = convertPolygonToLineGeoJSON(geometry, {
        id: item.id,
        name: item.name,
        level: item.level,
      })
      lineFeatures.push(...lineGeojson.features)
    } catch (err) {
      console.warn('解析行政区划边界几何失败', item.name, err)
    }
  }

  if (reqId !== drawRequestId) return

  // 5. 添加新图层前再次彻底清空旧图层与旧文字标签，确保无任何历史图元残留
  clearBoundaryLayer()

  // 6. 动态添加 Mars3D 纯线条图层（设置固定 ID 方便彻底检索清除）
  if (lineFeatures.length > 0) {
    boundaryLayer = new mars3d.layer.GeoJsonLayer({
      id: 'AREA_SEARCH_BOUNDARY_LAYER',
      name: `行政边界_${node.name}`,
      data: {
        type: 'FeatureCollection',
        features: lineFeatures,
      },
      symbol: {
        styleOptions: {
          color: '#007BFF',
          width: 3,
          clampToGround: true,
        },
      },
      show: showBoundary.value,
    })
    map.addLayer(boundaryLayer)
  }

  // 6. 只要 shouldFlyTo 为 true，直接调用 flyToPoint 飞向行政区中心，保证 100% 平滑飞行
  if (shouldFlyTo) {
    let computedCenter = null
    if (labelItems.length > 0) {
      let totalLng = 0
      let totalLat = 0
      let count = 0
      for (const item of labelItems) {
        const pt = getCenterCoordinates(item, null)
        if (pt) {
          totalLng += pt.lng
          totalLat += pt.lat
          count++
        }
      }
      if (count > 0) {
        computedCenter = { lng: totalLng / count, lat: totalLat / count }
      }
    }
    flyToRegionCenter(node, computedCenter)
  }

  // 7. 使用 Cesium.LabelCollection 渲染中心文字（无背景色，白字黑边，穿透地形深度）
  // 无论是否显示边界线，区划中心名称均始终展示
  const viewer = getActiveViewer()
  if (viewer && viewer.scene && viewer.scene.primitives && labelItems.length > 0) {
    cesiumLabelCollection = new Cesium.LabelCollection()
    cesiumLabelCollection._isAreaSearchLabels = true
    viewer.scene.primitives.add(cesiumLabelCollection)

    for (const item of labelItems) {
      let geom = null
      if (item.wkt) {
        try {
          geom = wktToGeoJSON(item.wkt)
        } catch (e) {}
      }

      const centerPoint = getCenterCoordinates(item, geom)
      if (centerPoint) {
        let { lng, lat, height = 0 } = centerPoint

        // 若三维地球当前已加载地形瓦片，动态同步地表实际高程
        if (viewer.scene.globe) {
          const carto = Cesium.Cartographic.fromDegrees(lng, lat)
          const sampledHeight = viewer.scene.globe.getHeight(carto)
          if (Number.isFinite(sampledHeight) && sampledHeight > 0) {
            height = sampledHeight
          }
        }

        const regionName = formatRegionName(item.name)

        cesiumLabelCollection.add({
          position: Cesium.Cartesian3.fromDegrees(lng, lat, height),
          text: regionName,
          font: 'bold 16px "Alimama FangYuanTi VF", "Microsoft YaHei", sans-serif',
          fillColor: Cesium.Color.WHITE,
          outlineColor: Cesium.Color.BLACK,
          outlineWidth: 3,
          style: Cesium.LabelStyle.FILL_AND_OUTLINE,
          horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
          verticalOrigin: Cesium.VerticalOrigin.CENTER,
          scaleByDistance: new Cesium.NearFarScalar(100000, 1.0, 1000000, 0.6),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          eyeOffset: new Cesium.Cartesian3(0, 0, -100),
        })
      }
    }
  }

  emit('boundary-change', {
    show: showBoundary.value,
    node,
    layer: boundaryLayer,
    labelCollection: cesiumLabelCollection,
    items: labelItems,
  })
}

/**
 * 彻底清除已绘制的边界线与名称标签图层，杜绝任何历史残留
 */
function clearBoundaryLayer() {
  const map = getActiveMap()
  const viewer = getActiveViewer()

  // 1. 彻底清除 Cesium 原生 LabelCollection（支持通过引用与全局特征标记识别清除）
  if (viewer?.scene?.primitives) {
    try {
      const prims = viewer.scene.primitives
      for (let i = prims.length - 1; i >= 0; i--) {
        const p = prims.get(i)
        if (p && (p === cesiumLabelCollection || p._isAreaSearchLabels)) {
          p.removeAll?.()
          prims.remove(p)
          if (!p.isDestroyed?.()) {
            p.destroy()
          }
        }
      }
    } catch (e) {
      console.warn('移除 Cesium 标签集异常', e)
    }
    cesiumLabelCollection = null
  }

  // 2. 彻底清除 Mars3D 边界纯线条图层（通过对象引用、固定 ID 与遍历图层多重保障）
  if (map) {
    try {
      if (boundaryLayer) {
        boundaryLayer.clear?.()
        map.removeLayer(boundaryLayer, true)
      }
      const existing = map.getLayerById('AREA_SEARCH_BOUNDARY_LAYER')
      if (existing) {
        existing.clear?.()
        map.removeLayer(existing, true)
      }
      if (map.eachLayer) {
        map.eachLayer((layer) => {
          if (layer && (layer.id === 'AREA_SEARCH_BOUNDARY_LAYER' || layer.name?.startsWith('行政边界_'))) {
            layer.clear?.()
            map.removeLayer(layer, true)
          }
        })
      }
    } catch (e) {
      console.warn('移除边界图层异常', e)
    }
    boundaryLayer = null
  }
}

/**
 * 响应边界线复选框变化
 * 勾选/取消勾选仅控制边界线的显示与隐藏，区划名称标签保持展示
 */
function handleBoundaryChange(val) {
  if (boundaryLayer) {
    boundaryLayer.show = val
  } else if (val && currentSelectedNode.value) {
    loadAndDrawBoundary(currentSelectedNode.value, false)
  }
  emit('boundary-change', {
    show: val,
    node: currentSelectedNode.value,
    layer: boundaryLayer,
    labelCollection: cesiumLabelCollection,
  })
}

/**
 * 点击某个子区划（下钻）
 */
async function selectOpt(item) {
  selectedOpts.value.push(item)
  if (item.children?.length) {
    displayOptions.value = item.children
  } else {
    displayOptions.value = []
  }

  emit('select-area', {
    selected: item,
    path: selectedOpts.value,
  })

  await loadAndDrawBoundary(item, true)
}

/**
 * 返回上一级
 */
async function backToPreLevel() {
  if (selectedOpts.value.length <= 1) return
  selectedOpts.value.pop()
  const current = selectedOpts.value[selectedOpts.value.length - 1]
  displayOptions.value = current.children || []

  emit('select-area', {
    selected: current,
    path: selectedOpts.value,
  })

  await loadAndDrawBoundary(current, true)
}

/**
 * 返回指定层级（点击面包屑节点）
 */
async function backToDesignatedLevel(opt, idx) {
  if (idx === selectedOpts.value.length - 1) return
  selectedOpts.value = selectedOpts.value.slice(0, idx + 1)
  displayOptions.value = opt.children || []

  emit('select-area', {
    selected: opt,
    path: selectedOpts.value,
  })

  await loadAndDrawBoundary(opt, true)
}

/**
 * 前端搜索选择回调，支持传入区划 id 或节点对象
 */
async function handleSearchSelect(targetIdOrNode) {
  if (!targetIdOrNode) return
  const targetId = typeof targetIdOrNode === 'object' ? targetIdOrNode.id : targetIdOrNode
  const target = flatAreaList.value.find((item) => String(item.id) === String(targetId))
  const targetNode = target?.node || (typeof targetIdOrNode === 'object' ? targetIdOrNode : null)
  if (!targetNode) return

  if (target?.path) {
    selectedOpts.value = [...target.path]
  } else if (!selectedOpts.value.some((item) => String(item.id) === String(targetNode.id))) {
    selectedOpts.value = [targetNode]
  }

  if (targetNode.children?.length) {
    displayOptions.value = targetNode.children
  } else {
    displayOptions.value = []
  }

  emit('select-area', {
    selected: targetNode,
    path: selectedOpts.value,
  })

  await loadAndDrawBoundary(targetNode, true)
  searchKeyword.value = ''
  searchOptions.value = []
}

/**
 * 切换面板展开/折叠
 */
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}


/**
 * 初始化加载行政区划树
 * 数据完全来源于 /dizai/adRegion/tree (getAreaListWithOutWk)
 * 组件面板展示、层级下钻与前端搜索联想全依赖此树
 */
async function initAreaTree() {
  let tree = []
  try {
    const res = await getAreaListWithOutWk()
    const list = Array.isArray(res) ? res : (res?.data || [])
    if (Array.isArray(list) && list.length > 0) {
      tree = list
    }
  } catch (e) {
    console.error('getAreaListWithOutWk 获取行政区划树失败', e)
  }

  if (!tree || tree.length === 0) return

  allAreaTree.value = tree
  flatAreaList.value = buildFlatIndex(tree)

  // 确定初始根节点（例如彭水县 500243 或恩施市 422801）
  let root = tree[0]
  if (root.id === '422800' && root.children?.length) {
    const esShi = root.children.find((c) => c.id === '422801')
    if (esShi) root = esShi
  } else if ((root.id === '500000' || root.name === '重庆市') && root.children?.length) {
    const pengShui = root.children.find((c) => c.id === '500243' || c.name?.includes('彭水'))
    if (pengShui) root = pengShui
  }

  selectedOpts.value = [root]
  displayOptions.value = root.children || []

  emit('loaded', { root, tree })

  // 无论是否勾选边界线，初始化时均绘制行政区划（内部按 showBoundary 决定边界线是否显示，名称始终显示）
  await loadAndDrawBoundary(root, false)
}

watch(
  () => props.map,
  (newMap) => {
    // 仅在地图重载且树已就绪而图元未绘制时兜底调用
    if (newMap && currentSelectedNode.value && allAreaTree.value.length > 0 && !cesiumLabelCollection) {
      loadAndDrawBoundary(currentSelectedNode.value, false)
    }
  },
)

onMounted(async () => {
  await initAreaTree()
})

onBeforeUnmount(() => {
  clearBoundaryLayer()
})

defineExpose({
  handleSearchSelect,
  selectOpt,
  backToPreLevel,
  backToDesignatedLevel,
  loadAndDrawBoundary,
  clearBoundaryLayer,
  toggleCollapse,
  flyToRegionCenter,
  getSelectedArea: () => currentSelectedNode.value,
})
</script>

<style lang="less" scoped>
.slope-search {
  position: absolute;
  z-index: 10;
  bottom: 24px;
  left: 16px;
  color: #ffffff;
  font-size: 12px;
  pointer-events: auto;
  user-select: none;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  :deep(.el-input__wrapper) {
    background: #8080804d;
    backdrop-filter: blur(10px);
    border-radius: 6px;
    border: 1px solid #ffffff80;
    box-shadow: none !important;
    padding: 0 12px;
    line-height: 34px;
    height: 34px;
  }
}

.select-opt-panel {
  backdrop-filter: blur(10px);
  width: 358px;
  margin-bottom: 6px;
  max-height: 240px;
  overflow: hidden;
  transition: max-height 0.3s ease-in-out, margin-bottom 0.3s ease-in-out, opacity 0.2s ease;
  display: flex;
  flex-direction: column;
  opacity: 1;

  &.is-collapsed {
    max-height: 0 !important;
    margin-bottom: 0 !important;
    opacity: 0;
    pointer-events: none;
  }

  .select-opt-panel__inner {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    border: 1px solid #ffffff80;
    padding: 14px;
    border-radius: 6px;
    background: #8080804d;
    backdrop-filter: blur(10px);
  }
}

.select-box {
  display: flex;
  align-items: center;
  column-gap: 4px;

  :deep(.el-input__inner) {
    color: #ffffff;
    cursor: pointer;
    font-size: 12px;
  }

  .select-input {
    position: relative;
    width: 140px;
    cursor: pointer;

    .arrow-icon {
      position: absolute;
      top: 0;
      right: 10px;
      height: 100%;
      display: flex;
      align-items: center;
      transition: transform 0.3s ease;
      color: #ffffff;
    }

    &.is-expanded {
      .arrow-icon {
        transform: rotate(180deg);
      }
    }
  }
}

.selected-opt {
  padding: 10px 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ffffff33;
  font-size: 12px;
  color: #ffffff;

  .back-arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    border-radius: 4px;
    background: #ffffff2e;
    margin-right: 8px;
    cursor: pointer;

    &:hover {
      background: #ffffffb3;
      color: var(--el-color-primary, #007bff);
    }

    &.is-disabled {
      opacity: 0.5;
      pointer-events: none;
    }
  }

  .selected-opt-text {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
  }

  .selected-each-opt {
    cursor: pointer;

    &:hover {
      font-weight: bold;
    }

    &.is-current {
      cursor: default;

      &:hover {
        font-weight: normal;
      }
    }
  }
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #ffffff33;

  .title {
    font-size: 12px;
    font-weight: bold;
    color: #ffffff;
    max-width: 110px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .search-input {
    display: flex;
    align-items: center;
    width: 200px;
    height: 24px;

    :deep(.el-select__wrapper) {
      flex: 1;
      min-width: 0;
      height: 24px;
      line-height: 24px;
      font-size: 12px;
      background: #ffffff33;
      border: 1px solid #ffffff80;
      box-shadow: none !important;
      min-height: auto;
      border-radius: 4px;
      padding: 0 8px;

      .el-input__inner {
        color: #ffffffb3;
      }

      .el-select__selected-item {
        color: #ffffff;
      }

      .el-select__input {
        color: #ffffff;
      }

      .el-select__placeholder {
        color: #ffffff;
      }
    }

    :deep(.el-checkbox) {
      display: flex;
      margin-right: 12px;

      &.is-checked {
        .el-checkbox__inner {
          border: 1px solid #ffffff;
          background: #007bff;

          &::after {
            border-width: 2px;
            border-color: #ffffff;
          }
        }
      }

      .el-checkbox__label {
        color: #ffffffbf;
        font-size: 10px;
      }

      .el-checkbox__input {
        .el-checkbox__inner {
          border: 1px solid #ffffff80;
          background: #8080804d;
        }
      }
    }
  }
}

.panel-common {
  flex: 1;
  min-height: 0;
  max-height: 140px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  column-gap: 10px;
  row-gap: 6px;
  padding: 10px 0;

  .each-opt {
    background: #8080804d;
    backdrop-filter: blur(10px);
    border-radius: 4px;
    padding: 6px 10px;
    white-space: nowrap;
    cursor: pointer;
    font-size: 12px;
    color: #ffffff;
    transition: all 0.2s ease;

    &:hover {
      color: var(--el-color-primary, #007bff);
      background: #ffffffb3;
    }

    &.is-selected {
      color: var(--el-color-primary, #007bff);
      background: #ffffffb3;
    }
  }
}

.empty-tips {
  margin-top: 10px;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

:deep(.el-select__caret) {
  color: #ffffff;
}

.search-select {
  :deep(.is-reverse) {
    transform: rotate(0deg) !important;
  }
}
</style>

<style lang="less">
.el-popper.map-search-popper {
  background: transparent;
  color: #ffffff;
}

.map-search-popper {
  background: #8080804d !important;
  backdrop-filter: blur(10px) !important;
  border-radius: 6px !important;
  border: 1px solid #ffffff80 !important;
  color: #ffffff !important;

  .el-select-dropdown__item {
    font-size: 12px;
    line-height: 26px;
    height: 26px;
    color: #ffffff;

    &.is-hovering {
      background: #8080804d !important;
      color: var(--el-color-primary, #007bff);
    }
  }

  .search-option-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;

    .option-name {
      color: #ffffff;
    }

    .option-path {
      font-size: 10px;
      color: rgba(255, 255, 255, 0.6);
      max-width: 140px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .el-popper__arrow {
    display: none;
  }
}
</style>
