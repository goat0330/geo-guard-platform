<template>
  <div class="mars-map-component">
    <!-- Mars3D 地球挂载容器 -->
    <div ref="mapDomRef" class="mars3d-container"></div>

    <!-- 行政区划下钻与搜索面板（默认固定于地图左下角） -->
    <AreaSearch
      v-if="showAreaSearch && isMapReady"
      ref="areaSearchRef"
      :map="mapInstance"
      :custom-style="areaSearchStyle"
      :default-show-boundary="defaultShowAreaBoundary"
      @select-area="handleAreaSelect"
      @loaded="handleAreaLoaded"
    />

    <!--
      地图覆盖物必须与 Cesium 画布位于同一定位容器内，避免页面级坐标换算产生偏移。
      risk-point-position 的坐标为 viewer.container 内的像素坐标。
    -->
    <slot name="overlay"></slot>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as mars3d from 'mars3d'
import * as Cesium from 'mars3d-cesium'
import CesiumNavigation from 'cesium-navigation-es6'
import AreaSearch from '@/components/AreaSearch/index.vue'
import chongqingBoundary from '@/assets/data/chongqing-boundary.json'
import yellowPoint from '@/assets/imgs/cesium/yellow-point.png'
import redPoint from '@/assets/imgs/cesium/red-point.png'
import { getHazardPointList } from '@/api/common.js'
import {
  DISASTER_BODY_LAYER_CONFIG,
  RISK_AREA_LAYER_CONFIG,
} from '@/utils/geoserver.js'
import 'mars3d/mars3d.css'

defineOptions({ name: 'MarsMap' })

const emit = defineEmits(['onload', 'click', 'risk-point-position', 'select-area', 'area-loaded'])

const props = defineProps({
  // 初始中心坐标，同时作为右下角重新定位的目标视角。
  defaultCenter: {
    type: Object,
    default: () => ({
      lng: 108.25109,
      lat: 29.40199,
      alt: 135257.4,
      pitch: -90,
      heading: 360,
      roll: 0,
    }),
  },
  // 自定义底图配置
  options: {
    type: Object,
    default: () => ({}),
  },
  // 是否展示行政区划搜索与下钻组件（所有 Cesium 地图默认开启）
  showAreaSearch: {
    type: Boolean,
    default: true,
  },
  defaultShowAreaBoundary: {
    type: Boolean,
    default: false,
  },
  // 部分业务页面已有独立定位控件，无需展示 Cesium 罗盘。
  showCompass: {
    type: Boolean,
    default: true,
  },
  // 仅截图场景开启，避免所有地图长期保留 WebGL 绘图缓冲带来额外显存开销。
  preserveDrawingBuffer: {
    type: Boolean,
    default: false,
  },
  // 行政区划组件的自定义样式（默认固定于地图左下角 left: 16px; bottom: 24px）
  areaSearchStyle: {
    type: Object,
    default: () => ({}),
  },
})

const mapDomRef = ref(null)
const areaSearchRef = ref(null)
const isMapReady = ref(false)

const handleAreaSelect = (data) => {
  emit('select-area', data)
}

/** 行政区划面板初始化完成（拿到默认区划）：向上抛出，供页面按默认区划取数 */
const handleAreaLoaded = (data) => {
  emit('area-loaded', data)
}

// 避免使用 Vue 的 ref/reactive 包装 Cesium/Mars3D 实例，杜绝深层 Proxy 劫持导致的性能衰减与内存泄漏
let mapInstance = null
let resizeObserver = null
let navigationInstance = null
let chongqingBoundaryLayer = null
let earthMapLayer = null
let earthMapLabelLayer = null
let vectorMapLayer = null
let vectorMapLabelLayer = null
let cachedTerrainProvider = null
let riskPointLayer = null
let riskPointPostRenderHandler = null
let riskPointRequestId = 0

/**
 * 标记图标预加载缓存：key 为图片地址，value 为加载完成的 HTMLImageElement（失败为 null）。
 * billboard 直接传图片地址时由 Cesium 异步加载，加载完成前标记不会渲染，
 * 表现为「定位后图标迟迟不出现」，故统一先加载成图片对象再打点。
 */
const markerImageCache = new Map()
const hazardClusterImageCache = new Map()

/** 生成隐患点聚合数量图标，并按数量缓存（返回高清 Base64 DataURL）。 */
const getHazardClusterImage = (count) => {
  const displayCount = count > 99 ? '99+' : String(count)
  if (hazardClusterImageCache.has(displayCount)) return hazardClusterImageCache.get(displayCount)

  // 采用 2x 高清画布（size = 56，经 scale: 0.5 呈现为 28px，适中精致）
  const size = 56
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const context = canvas.getContext('2d')
  const center = size / 2
  const radius = center - 4

  // 外圈半透明光晕，增强在复杂卫星影像和起伏山体背景下的对比度
  context.beginPath()
  context.arc(center, center, radius + 2, 0, Math.PI * 2)
  context.fillStyle = 'rgba(228, 91, 91, 0.35)'
  context.fill()

  // 内部主体红色圆
  context.beginPath()
  context.arc(center, center, radius, 0, Math.PI * 2)
  context.fillStyle = '#E45B5B'
  context.fill()

  // 白色边框
  context.lineWidth = 2
  context.strokeStyle = '#ffffff'
  context.stroke()

  // 居中数量文本（字号严格采用双数 18px / 22px）
  context.fillStyle = '#ffffff'
  context.font = `bold ${displayCount.length > 2 ? 18 : 22}px sans-serif`
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.fillText(displayCount, center, center)

  const dataUrl = canvas.toDataURL('image/png', 1)
  hazardClusterImageCache.set(displayCount, dataUrl)
  return dataUrl
}

/**
 * 预加载标记图标，返回已加载的图片对象；加载失败回落到 null，由调用方继续传地址
 * @param {string} src 图片地址
 * @returns {Promise<HTMLImageElement|null>}
 */
const loadMarkerImage = (src) => {
  if (!src) return Promise.resolve(null)
  if (markerImageCache.has(src)) return markerImageCache.get(src)

  const task = new Promise((resolve) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => resolve(null)
    image.src = src
  })
  markerImageCache.set(src, task)
  return task
}

// 承灾体、风险区 WMS 图层，以及接口驱动的隐患点图层状态。
let disasterBodyWmsLayer = null
let hazardPointLayer = null
let riskAreaWmsLayer = null
let isHazardPointActive = false
let hazardPointLoadPromise = null
let hazardPointRequestVersion = 0

const TERRAIN_URL = 'https://application.digitalcq.com/3Dtiles/cq30m2'
const TDT_TOKEN = 'a15c655473ce0c05d18e9aba7506de12'

/**
 * 获取或创建三维切片地形 Provider
 */
const getTerrainProvider = () => {
  if (!cachedTerrainProvider) {
    cachedTerrainProvider = new Cesium.CesiumTerrainProvider({
      url: TERRAIN_URL,
    })
  }
  return cachedTerrainProvider
}

/**
 * 动态开关三维切片地形
 * @param {boolean} visible 是否启用地形
 */
const setTerrainVisible = (visible) => {
  if (!mapInstance || !mapInstance.viewer) return

  if (visible) {
    const provider = getTerrainProvider()
    mapInstance.hasTerrain = true
    if (provider) {
      mapInstance.viewer.terrainProvider = provider
    }
    mapInstance.viewer.scene.globe.depthTestAgainstTerrain = true
  } else {
    const currentProvider = mapInstance.viewer.terrainProvider
    if (currentProvider && !(currentProvider instanceof Cesium.EllipsoidTerrainProvider)) {
      cachedTerrainProvider = currentProvider
    }
    mapInstance.hasTerrain = false
    mapInstance.viewer.terrainProvider = new Cesium.EllipsoidTerrainProvider()
    mapInstance.viewer.scene.globe.depthTestAgainstTerrain = false
  }
}

const createTdtProvider = (layer) => new Cesium.WebMapTileServiceImageryProvider({
  url: `https://t{s}.tianditu.gov.cn/${layer}_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=${layer}&tileMatrixSet=w&TileMatrix={TileMatrix}&TileRow={TileRow}&TileCol={TileCol}&style=default&format=tiles&tk=${TDT_TOKEN}`,
  layer: 'tdtBasicLayer',
  style: 'default',
  format: 'image/jpeg',
  tileMatrixSetID: 'GoogleMapsCompatible',
  subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
  maximumLevel: 18,
})

const removeTdtLayers = () => {
  const imageryLayers = mapInstance?.viewer?.imageryLayers
  if (!imageryLayers) return

  ;[earthMapLayer, earthMapLabelLayer, vectorMapLayer, vectorMapLabelLayer].forEach((layer) => {
    if (layer) imageryLayers.remove(layer)
  })
  earthMapLayer = null
  earthMapLabelLayer = null
  vectorMapLayer = null
  vectorMapLabelLayer = null
}

/**
 * 初始化 Mars3D 地图
 */
const initMarsMap = () => {
  if (!mapDomRef.value) return

  // 合并地图基础配置
  const mapOptions = {
    scene: {
      center: props.defaultCenter,
      showSun: true,
      showMoon: true,
      showSkyBox: true,
      showSkyAtmosphere: true,
      fog: true,
      fxaa: true,
      globe: {
        depthTestAgainstTerrain: true,
        baseColor: '#2b3e4a',
        showGroundAtmosphere: false,
        enableLighting: false,
      },
      cameraController: {
        zoomFactor: 3.0,
        minimumZoomDistance: 10,
        maximumZoomDistance: 50000000,
        enableRotate: true,
        enableTranslate: true,
        enableTilt: true,
        enableZoom: true,
        enableCollisionDetection: true,
      },
      ...(props.options.scene || {}),
      ...(props.preserveDrawingBuffer
        ? {
            contextOptions: {
              ...(props.options.scene?.contextOptions || {}),
              webgl: {
                ...(props.options.scene?.contextOptions?.webgl || {}),
                preserveDrawingBuffer: true,
              },
            },
          }
        : {}),
    },
    // 1. 数字重庆 30米精度三维地形 Terraria 切片服务
    terrain: {
      url: 'https://application.digitalcq.com/3Dtiles/cq30m2',
      show: true,
      ...(props.options.terrain || {}),
    },
    control: {
      defaultContextMenu: false,
      baseLayerPicker: false,
      sceneModePicker: false,
      vrButton: false,
      fullscreenButton: false,
      navigationHelpButton: false,
      homeButton: false,
      geocoder: false,
      clockAnimate: false,
      timeline: false,
      locationBar: false,
      ...(props.options.control || {}),
    },
    basemaps: props.options.basemaps || [
      // 2. 数字重庆 2025卫星影像服务（CGCS2000 经纬度投影）
      {
        id: 10,
        name: '数字重庆卫星影像',
        type: 'xyz',
        url: 'https://application.digitalcq.com/sjzs/service/RES_2025N05MYX_P0P6/1b4d487351be4416b81d7309da374a35/wmts/fwgl-606007de-a132-9a84-7894-2b455251/cgcs2000/{z}/{x}/{y}.png',
        crs: 'EPSG:4490',
        show: true,
      },
      // 3. 数字重庆试点区影像注记服务（CGCS2000 经纬度投影）
      {
        id: 11,
        name: '数字重庆试点区影像注记',
        type: 'xyz',
        url: 'https://application.digitalcq.com/sjzs/service/RES_YXTZJ_PNFA/8fff3321d7254088af409e256c58f094/tile/{z}/{y}/{x}',
        crs: 'EPSG:4490',
        show: false,
      },
      // 备用：高德影像与路网
      {
        id: 20,
        name: '高德卫星影像',
        type: 'gaode',
        layer: 'img_d',
        show: false,
      },
      {
        id: 21,
        name: '高德路网注记',
        type: 'gaode',
        layer: 'img_z',
        show: false,
      },
    ],
    ...props.options,
  }

  // 创建 Mars3D Map 实例
  mapInstance = new mars3d.Map(mapDomRef.value, mapOptions)

  // Cesium 默认可能按 1 倍画布渲染，高分屏下 billboard 会被浏览器放大而发糊。
  // 渲染倍率最高限制为 2，在保证点位图标清晰度的同时控制 GPU 开销。
  if (mapInstance.viewer) {
    mapInstance.viewer.resolutionScale = Math.min(window.devicePixelRatio || 1, 2)
  }

  // 缓存初始化时配置的三维切片地形 Provider
  if (mapInstance.terrainProvider && !(mapInstance.terrainProvider instanceof Cesium.EllipsoidTerrainProvider)) {
    cachedTerrainProvider = mapInstance.terrainProvider
  } else if (mapInstance.viewer?.terrainProvider && !(mapInstance.viewer.terrainProvider instanceof Cesium.EllipsoidTerrainProvider)) {
    cachedTerrainProvider = mapInstance.viewer.terrainProvider
  }

  // 沿用恩施地灾的 Cesium Navigation 罗盘及交互能力。
  navigationInstance = new CesiumNavigation(mapInstance.viewer, {
    defaultResetView: Cesium.Rectangle.fromDegrees(105.28, 28.10, 110.20, 32.22),
    enableCompass: props.showCompass,
    enableZoomControls: false,
    enableDistanceLegend: false,
    enableCompassOuterRing: props.showCompass,
    resetTooltip: '重置视图',
  })

  // 重庆市行政边界保留为地图基础图层，默认隐藏，避免与行政区划边界重复显示。
  chongqingBoundaryLayer = new mars3d.layer.GeoJsonLayer({
    name: '重庆市行政边界',
    data: chongqingBoundary,
    show: false,
    symbol: {
      styleOptions: {
        color: '#007BFF',
        opacity: 0.04,
        outline: true,
        outlineColor: '#DCEDFF',
        outlineWidth: 3,
        clampToGround: true,
      },
    },
  })
  mapInstance.addLayer(chongqingBoundaryLayer)

  // 风险点位使用独立图层，重复定位时只替换当前点位。
  riskPointLayer = new mars3d.layer.GraphicLayer({ name: '斜坡单元定位点' })
  mapInstance.addLayer(riskPointLayer)

  // 挂载到全局方便工具函数以及第三方库直接获取（如 utils/index.js 中的 window._viewer）
  window._map = mapInstance
  window._viewer = mapInstance.viewer
  window._map.handleLayerChange = handleLayerChange

  // 地图单击事件转发
  mapInstance.on(mars3d.EventType.click, (event) => {
    emit('click', event)
  })

  // 监听地形加载错误，降级保障离线或无专网环境下地球正常渲染
  mapInstance.on(mars3d.EventType.terrainLoadError, (event) => {
    console.warn('数字重庆三维地形服务未连通或加载异常，自动降级为标准椭球体:', event)
  })

  // 监听容器尺寸变化（如侧边栏收起展开时自动调整画布）
  if (window.ResizeObserver && mapDomRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (mapInstance && mapInstance.viewer) {
        mapInstance.viewer.resize()
      }
    })
    resizeObserver.observe(mapDomRef.value)
  }

  // 默认启用试点区影像模式（3D地形 + 底层天地图影像 + 试点区高精影像）
  changeBaseMap('pilot')

  isMapReady.value = true

  // 触发就绪回调
  emit('onload', {
    map: mapInstance,
    viewer: mapInstance.viewer,
    mars3d,
  })
}

/**
 * 放大
 */
const handleZoomIn = () => {
  if (mapInstance) {
    mapInstance.zoomIn()
  }
}

/**
 * 缩小
 */
const handleZoomOut = () => {
  if (mapInstance) {
    mapInstance.zoomOut()
  }
}

/**
 * 切换底图模式
 * @param {'pilot'|'default'|'imagery'|'vector'} type 底图类型
 */
const changeBaseMap = (type = 'pilot') => {
  if (!mapInstance || !mapInstance.viewer) return
  removeTdtLayers()

  const layer10 = mapInstance.getLayerById(10)
  const layer11 = mapInstance.getLayerById(11)
  const imageryLayers = mapInstance.viewer.imageryLayers

  if (type === 'pilot' || type === 'default') {
    // 1. 开启数字重庆 3D 切片地形
    setTerrainVisible(true)

    // 2. 默认叠加到天地图上面：底层添加天地图卫星影像作为全局基底（index 0）
    earthMapLayer = imageryLayers.addImageryProvider(createTdtProvider('img'), 0)
    if (earthMapLayer) {
      imageryLayers.lowerToBottom(earthMapLayer)
    }

    // 3. 上层显示数字重庆试点区高精卫星影像，注记由开关按需显示
    if (layer10) layer10.show = true
    raiseWmsLayersToTop()
    return
  }

  // 切换为天地图模式（矢量 / 纯影像）时，关闭 3D 地形（降级为平整椭球体），隐藏试点区专属图层
  setTerrainVisible(false)
  if (layer10) layer10.show = false
  if (layer11) layer11.show = false

  if (type === 'vector') {
    vectorMapLayer = imageryLayers.addImageryProvider(createTdtProvider('vec'))
    vectorMapLabelLayer = imageryLayers.addImageryProvider(createTdtProvider('cva'))
  } else if (type === 'imagery') {
    earthMapLayer = imageryLayers.addImageryProvider(createTdtProvider('img'))
  }
  raiseWmsLayersToTop()
}

/**
 * 开关指定底图的影像注记
 * @param {{ type: 'pilot'|'imagery', show: boolean }} options 注记所属底图及显示状态
 */
const toggleImageryLabel = ({ type, show }) => {
  const imageryLayers = mapInstance?.viewer?.imageryLayers
  if (!imageryLayers) return

  if (type === 'imagery' || type === 'pilot') {
    // 天地图影像与全市影像均叠加天地图 cia 注记，确保重庆全域都有地名标注。
    if (earthMapLabelLayer) {
      imageryLayers.remove(earthMapLabelLayer)
      earthMapLabelLayer = null
    }
    if (show && earthMapLayer) {
      earthMapLabelLayer = imageryLayers.addImageryProvider(createTdtProvider('cia'))
    }
  }

  // 旧的数字重庆注记仅覆盖试点区，全市影像模式不再使用该图层。
  const layer11 = mapInstance.getLayerById(11)
  if (layer11) layer11.show = false
}

/**
 * 重置到初始视角
 */
const flyToHome = () => {
  if (mapInstance) {
    mapInstance.setCameraView(props.defaultCenter, {
      duration: 1.5,
    })
  }
}

/**
 * 定位到指定点位/斜坡单元（供父组件调用）
 * 支持 keepView: true 保持当前相机视角朝向（航向角 heading 与俯仰角 pitch），仅平移聚焦目标
 */
const flyToPoint = (point, options = {}) => {
  if (!mapInstance) return
  const viewer = mapInstance.viewer

  if (options.keepView && viewer?.camera) {
    // 保持用户当前视角（航向角和俯仰角），不重置视角朝向
    const heading = viewer.camera.heading
    const pitch = Cesium.Math.clamp(
      viewer.camera.pitch,
      -Cesium.Math.PI_OVER_TWO + 0.0001,
      Cesium.Math.PI_OVER_TWO - 0.0001,
    )

    // 计算合适观察距离：若处于大尺度宏观俯瞰视角，贴近至标准聚焦视距；若已在有效近景内，尽量保持当前视距
    let range = options.radius
    if (!range) {
      let currentDistance = 1200
      try {
        const canvas = viewer.scene?.canvas
        if (canvas?.clientWidth && canvas?.clientHeight) {
          const centerRay = viewer.camera.getPickRay(
            new Cesium.Cartesian2(canvas.clientWidth / 2, canvas.clientHeight / 2),
          )
          const pickCartesian = centerRay ? viewer.scene.globe?.pick(centerRay, viewer.scene) : null
          if (pickCartesian) {
            currentDistance = Cesium.Cartesian3.distance(viewer.camera.position, pickCartesian)
          } else {
            currentDistance = viewer.camera.positionCartographic?.height || 1200
          }
        }
      } catch {
        currentDistance = viewer.camera.positionCartographic?.height || 1200
      }

      if (currentDistance > 3000) {
        range = 1200
      } else {
        range = Math.max(600, Math.min(currentDistance, 3000))
      }
    }

    let cartesian = null
    if (point instanceof Cesium.Cartesian3) {
      cartesian = point
    } else if (point?.toCartesian) {
      cartesian = point.toCartesian(true)
    } else if (Array.isArray(point)) {
      cartesian = Cesium.Cartesian3.fromDegrees(point[0], point[1], point[2] || 0)
    } else if (point?.lng != null && point?.lat != null) {
      cartesian = Cesium.Cartesian3.fromDegrees(point.lng, point.lat, point.alt || 0)
    }

    if (cartesian) {
      const boundingSphere = new Cesium.BoundingSphere(cartesian, 0)
      viewer.camera.flyToBoundingSphere(boundingSphere, {
        duration: options.duration ?? 1.5,
        offset: new Cesium.HeadingPitchRange(heading, pitch, range),
        complete: () => {
          const targetX = typeof options.screenTargetX === 'function'
            ? options.screenTargetX()
            : options.screenTargetX
          if (Number.isFinite(targetX)) {
            const current = Cesium.SceneTransforms.worldToWindowCoordinates(viewer.scene, cartesian)
            const canvasHeight = viewer.scene.canvas?.clientHeight
            const fovy = viewer.camera.frustum?.fovy
            if (current && canvasHeight && Number.isFinite(fovy)) {
              // 将点位移到目标屏幕横坐标，弹窗仍可保持与标记点水平对齐。
              const metersPerPixel = 2 * range * Math.tan(fovy / 2) / canvasHeight
              viewer.camera.moveRight((current.x - targetX) * metersPerPixel)
            }
          }
          options.complete?.()
        },
        cancel: options.cancel,
      })
      return
    }
  }

  mapInstance.flyToPoint(point, {
    radius: 1200,
    pitch: -45,
    duration: 1.8,
    ...options,
  })
}

/**
 * 展示并聚焦斜坡单元点位
 * 高程优先取地形采样值（缺省开启），配合 clampToGround 保证标记贴在地形表面而非椭球体，
 * 避免出现「定位后在地下」的情况。
 * @param {{ lng: number, lat: number, alt?: number }} point 点位坐标
 * @param {{ sampleTerrain?: boolean, clampToGround?: boolean, flyTo?: boolean, alwaysVisible?: boolean,
 *           image?: string, width?: number, height?: number }} options 定位选项
 *   image/width/height 用于业务方自定义标记图标，缺省沿用内置黄点（30x34）；
 *   image 会先预加载成图片对象再打点（内部按地址缓存），避免图片未就绪导致标记不渲染；
 *   clampToGround 默认 true（贴地），业务方需要按指定高程悬空时显式传 false。
 *   注意：billboard 会把贴图直接拉伸到 width x height，不会等比留白，
 *   所以 width/height 必须与图标原图的宽高比一致，否则图标会被压扁或拉长。
 */
const showRiskPoint = async (point, options = {}) => {
  if (!mapInstance || !riskPointLayer) return

  clearRiskPoint()
  const viewer = mapInstance.viewer
  const requestId = ++riskPointRequestId

  /* 图标加载与地形高程采样并行发起，避免串行等待放大定位延迟；
     图片未就绪时 billboard 不渲染，必须等加载完成再打点 */
  const imageTask = loadMarkerImage(options.image || yellowPoint)
  // 与恩施地灾一致：先取得真实地形高程，标记和信息牌始终使用同一个空间锚点。
  const terrainTask =
    options.sampleTerrain !== false &&
    viewer.terrainProvider &&
    !(viewer.terrainProvider instanceof Cesium.EllipsoidTerrainProvider)
      ? Cesium.sampleTerrainMostDetailed(viewer.terrainProvider, [
          Cesium.Cartographic.fromDegrees(point.lng, point.lat),
        ]).catch((error) => {
          console.warn('斜坡单元点位地形高程采样失败，使用接口高程', error)
          return null
        })
      : null

  const [loadedImage, terrainPositions] = await Promise.all([imageTask, terrainTask])

  // 异步等待期间可能已定位到其他点位或关闭面板，忽略过期请求。
  if (requestId !== riskPointRequestId || !mapInstance || !riskPointLayer) return

  const terrainHeight = terrainPositions?.[0]?.height
  const height = Number.isFinite(terrainHeight) ? terrainHeight : Number(point.alt) || 0

  const position = new mars3d.LngLatPoint(point.lng, point.lat, height)
  const cartesianPosition = position.toCartesian(true)
  riskPointLayer.addGraphic(new mars3d.graphic.BillboardEntity({
    position,
    style: {
      /* 优先用已加载的图片对象，加载失败时回落到地址由 Cesium 自行加载 */
      image: loadedImage || options.image || yellowPoint,
      /* 默认尺寸 30x34 与内置黄点原图 60x68 等比；自定义图标需传入等比后的宽高 */
      width: options.width ?? 30,
      height: options.height ?? 34,
      horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
      verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
      /* 默认贴地：地形开启时标记吸附到地形表面，不会沉到地形以下 */
      clampToGround: options.clampToGround !== false,
      disableDepthTestDistance: options.alwaysVisible ? Number.POSITIVE_INFINITY : 0,
    },
  }))
  const updateScreenPosition = () => {
    const viewer = mapInstance?.viewer
    if (!viewer || riskPointLayer?.graphics?.length === 0) return

    const windowPosition = Cesium.SceneTransforms.worldToWindowCoordinates(
      viewer.scene,
      cartesianPosition,
    )
    if (!windowPosition) {
      emit('risk-point-position', null)
      return
    }

    emit('risk-point-position', {
      x: windowPosition.x,
      y: windowPosition.y,
    })
  }
  riskPointPostRenderHandler = updateScreenPosition
  mapInstance.viewer.scene.postRender.addEventListener(riskPointPostRenderHandler)
  updateScreenPosition()
  if (options.flyTo !== false) {
    flyToPoint(position, {
      keepView: options.keepView,
      radius: options.radius,
      duration: options.duration,
      ...options.flyToOptions,
    })
  }
}

const clearRiskPoint = () => {
  riskPointRequestId += 1
  if (riskPointPostRenderHandler && mapInstance?.viewer?.scene) {
    mapInstance.viewer.scene.postRender.removeEventListener(riskPointPostRenderHandler)
    riskPointPostRenderHandler = null
  }
  riskPointLayer?.clear()
}

/**
 * 触发画布重绘与尺寸更新
 */
const resize = () => {
  if (mapInstance && mapInstance.viewer) {
    mapInstance.viewer.resize()
  }
}

/**
 * 截取当前地图画布。DOM 覆盖控件不属于 WebGL 画布，因此不会进入附件图片。
 * @param {{ type?: string, quality?: number }} options 图片格式与质量
 * @returns {Promise<Blob>} 地图截图
 */
const captureImage = async ({ type = 'image/png', quality = 1 } = {}) => {
  const viewer = mapInstance?.viewer
  const canvas = viewer?.canvas
  if (!canvas) throw new Error('地图尚未初始化')

  viewer.scene?.requestRender?.()
  // 等待相机和斜坡/点位图层完成本帧合成后再读取画布。
  await new Promise((resolve) => requestAnimationFrame(resolve))
  await new Promise((resolve) => requestAnimationFrame(resolve))

  return new Promise((resolve, reject) => {
    try {
      canvas.toBlob((blob) => {
        if (blob) resolve(blob)
        else reject(new Error('地图截图生成失败'))
      }, type, quality)
    } catch (error) {
      reject(error)
    }
  })
}

onMounted(() => {
  initMarsMap()
})

/**
 * 提升 WMS 业务图层至底图图层顶层
 */
const raiseWmsLayersToTop = () => {
  const imageryLayers = mapInstance?.viewer?.imageryLayers
  if (!imageryLayers) return
  if (riskAreaWmsLayer) {
    imageryLayers.raiseToTop(riskAreaWmsLayer)
  }
  if (disasterBodyWmsLayer) {
    imageryLayers.raiseToTop(disasterBodyWmsLayer)
  }
}

/**
 * 切换承灾体（房屋）WMS 图层显示
 * 对应 GeoServer 恩施地灾 pku_user:data_house
 * @param {boolean} visible 是否显示
 */
const toggleDisasterBodyLayer = (visible) => {
  const viewer = mapInstance?.viewer
  if (!viewer) return

  if (!visible) {
    if (disasterBodyWmsLayer) {
      disasterBodyWmsLayer.show = false
    }
    return
  }

  if (disasterBodyWmsLayer) {
    disasterBodyWmsLayer.show = true
    viewer.imageryLayers.raiseToTop(disasterBodyWmsLayer)
    return
  }

  try {
    const provider = new Cesium.WebMapServiceImageryProvider(DISASTER_BODY_LAYER_CONFIG)
    disasterBodyWmsLayer = viewer.imageryLayers.addImageryProvider(provider)
    disasterBodyWmsLayer.show = true
    viewer.imageryLayers.raiseToTop(disasterBodyWmsLayer)
  } catch (error) {
    console.error('加载承灾体 WMS 图层失败:', error)
  }
}

/**
 * 从隐患点列表接口加载并切换点位图层。
 * 接口可能返回数组、分页 rows 或 data 包装，统一在此处收敛。
 * @param {boolean} visible 是否显示
 */
const toggleHazardPointLayer = async (visible) => {
  isHazardPointActive = Boolean(visible)
  if (!visible) {
    hazardPointRequestVersion += 1
    if (hazardPointLayer) hazardPointLayer.show = false
    return
  }
  if (!mapInstance) return
  if (hazardPointLayer) {
    hazardPointLayer.show = true
    return
  }
  if (hazardPointLoadPromise) return hazardPointLoadPromise

  const requestVersion = ++hazardPointRequestVersion
  hazardPointLoadPromise = (async () => {
    try {
      const response = await getHazardPointList({ pageNum: 1, pageSize: 1000 }, { throwRes: true })
      if (!isHazardPointActive || requestVersion !== hazardPointRequestVersion || !mapInstance) return
      const list = Array.isArray(response)
        ? response
        : Array.isArray(response?.rows)
          ? response.rows
          : Array.isArray(response?.data)
            ? response.data
            : Array.isArray(response?.data?.rows)
              ? response.data.rows
              : []
      const markerImage = await loadMarkerImage(redPoint)
      if (!isHazardPointActive || requestVersion !== hazardPointRequestVersion || !mapInstance) return

      hazardPointLayer = new mars3d.layer.GraphicLayer({
        name: '隐患点',
        cluster: {
          enabled: true,
          // 按屏幕像素动态聚合：数量小于 6 时全展开展示单点，>=6 时聚合并显示数量。
          pixelRange: 60,
          minimumClusterSize: 6,
          image: getHazardClusterImage,
          style: {
            scale: 0.5,
            clampToGround: true,
            disableDepthTestDistance: Number.POSITIVE_INFINITY,
            horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
            verticalOrigin: Cesium.VerticalOrigin.CENTER,
          },
        },
      })
      mapInstance.addLayer(hazardPointLayer)

      // 聚合点点击：自动放大视角聚焦并展开聚合簇
      hazardPointLayer.on(mars3d.EventType.click, (event) => {
        const clusterData = event.graphic?._clusterData || event.clusterData
        if (clusterData?.graphics?.length) {
          const positions = clusterData.graphics.map((g) => g.position).filter(Boolean)
          if (positions.length && mapInstance) {
            mapInstance.flyToPositions(positions, { scale: 1.5 })
          }
        }
      })
      list.forEach((item) => {
        const lng = Number(item?.longitude)
        const lat = Number(item?.latitude)
        if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
        hazardPointLayer.addGraphic(new mars3d.graphic.BillboardEntity({
          position: new mars3d.LngLatPoint(lng, lat),
          attr: item,
          style: {
            image: markerImage || redPoint,
            width: 24,
            height: 24,
            horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
            clampToGround: true,
            disableDepthTestDistance: Number.POSITIVE_INFINITY,
          },
        }))
      })
    } catch (error) {
      console.error('加载隐患点图层失败:', error)
    } finally {
      hazardPointLoadPromise = null
    }
  })()
  return hazardPointLoadPromise
}

/**
 * 切换风险区 WMS 图层显示
 * 对应 GeoServer geo_guard:data_risk_zone
 * @param {boolean} visible 是否显示
 */
const toggleRiskAreaLayer = (visible) => {
  const viewer = mapInstance?.viewer
  if (!viewer) return

  if (!visible) {
    if (riskAreaWmsLayer) {
      riskAreaWmsLayer.show = false
    }
    return
  }

  if (riskAreaWmsLayer) {
    riskAreaWmsLayer.show = true
    viewer.imageryLayers.raiseToTop(riskAreaWmsLayer)
    return
  }

  try {
    const provider = new Cesium.WebMapServiceImageryProvider(RISK_AREA_LAYER_CONFIG)
    riskAreaWmsLayer = viewer.imageryLayers.addImageryProvider(provider)
    riskAreaWmsLayer.show = true
    viewer.imageryLayers.raiseToTop(riskAreaWmsLayer)
  } catch (error) {
    console.error('加载风险区 WMS 图层失败:', error)
  }
}

/**
 * 响应图层控制组件勾选切换
 * @param {{ key?: string, label?: string, checked: boolean }} options
 */
const handleLayerChange = (options) => {
  if (!options) return
  const { key, label, checked } = options

  if (key === 'disaster_body' || label === '承灾体') {
    toggleDisasterBodyLayer(checked)
  } else if (key === 'risk_area' || label === '风险区') {
    toggleRiskAreaLayer(checked)
  } else if (key === 'hazard_point' || label === '隐患点' || label === '灾害点') {
    toggleHazardPointLayer(checked)
  }
}

onBeforeUnmount(() => {
  // 清理 ResizeObserver
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  if (navigationInstance) {
    navigationInstance.destroy()
    navigationInstance = null
  }

  // 移除业务图层并使未完成的隐患点请求失效，防止卸载后回写地图。
  if (disasterBodyWmsLayer) {
    try {
      mapInstance?.viewer?.imageryLayers?.remove(disasterBodyWmsLayer)
    } catch (e) {
      console.warn('移除承灾体图层异常:', e)
    }
    disasterBodyWmsLayer = null
  }
  hazardPointRequestVersion += 1
  if (hazardPointLayer && mapInstance) {
    hazardPointLayer.off?.(mars3d.EventType.click)
    mapInstance.removeLayer(hazardPointLayer, true)
  }
  hazardPointLayer = null
  hazardClusterImageCache.clear()
  if (riskAreaWmsLayer) {
    try {
      mapInstance?.viewer?.imageryLayers?.remove(riskAreaWmsLayer)
    } catch (e) {
      console.warn('移除风险区图层异常:', e)
    }
    riskAreaWmsLayer = null
  }

  // 销毁 Mars3D 地图实例，彻底释放 WebGL 上下文与事件监听，杜绝内存泄漏
  if (mapInstance) {
    clearRiskPoint()
    try {
      mapInstance.destroy()
    } catch (e) {
      console.warn('Map destroy warning:', e)
    }
    mapInstance = null
    cachedTerrainProvider = null
    earthMapLayer = null
    earthMapLabelLayer = null
    vectorMapLayer = null
    vectorMapLabelLayer = null
    chongqingBoundaryLayer = null
    riskPointLayer = null
    riskPointPostRenderHandler = null
  }

  // 清除全局变量挂载
  if (window._map) {
    window._map = null
  }
  if (window._viewer) {
    window._viewer = null
  }
})

defineExpose({
  getMap: () => mapInstance,
  getViewer: () => mapInstance?.viewer,
  flyToHome,
  flyToPoint,
  showRiskPoint,
  clearRiskPoint,
  handleZoomIn,
  handleZoomOut,
  changeBaseMap,
  toggleImageryLabel,
  handleLayerChange,
  toggleDisasterBodyLayer,
  toggleHazardPointLayer,
  toggleRiskAreaLayer,
  resize,
  captureImage,
  getAreaSearch: () => areaSearchRef.value,
})
</script>

<style lang="less" scoped>
.mars-map-component {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100%;
  overflow: hidden;
  user-select: none;
  border-radius: 16px;
  -webkit-mask-image: -webkit-radial-gradient(white, black);
  mask-image: radial-gradient(white, black);
  transform: translateZ(0);
}

.mars3d-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-radius: inherit;
  -webkit-mask-image: -webkit-radial-gradient(white, black);
  mask-image: radial-gradient(white, black);
  transform: translateZ(0);
}

/* 隐藏 Cesium 默认版权与组件 */
:deep(.cesium-widget-credits),
:deep(.cesium-credit-logoContainer),
:deep(.cesium-credit-textContainer) {
  display: none !important;
}

/* 强制 Cesium WebGL 硬件加速画布遵循 16px 圆角裁切，防止 macOS/Chrome GPU 穿透直角 */
:deep(.cesium-widget),
:deep(.cesium-widget canvas) {
  border-radius: 16px !important;
  overflow: hidden !important;
}

/* 恩施地灾原生罗盘样式，交互由 cesium-navigation-es6 提供。 */
:deep(.compass) {
  top: auto;
  right: 7px;
  bottom: 196px;
  width: 55px;
  height: 55px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  background: rgba(128, 128, 128, 0.5);
}

:deep(.compass-outer-ring-background) {
  top: 0;
  left: 0;
  width: 15px;
  height: 15px;
  border: 20px solid transparent;
  border-radius: 50%;
}

:deep(.compass-outer-ring) {
  top: 0;
  left: -1px;
  width: 55px;
  height: 55px;
  background-image: url('@/assets/imgs/map-controls/compass.svg'), url('@/assets/imgs/map-controls/compass-border.png');
  background-position: center;
  background-size: 20px 20px, 40px 40px;
  background-repeat: no-repeat;
}

:deep(.compass-gyro),
:deep(.compass-gyro-background) {
  display: none;
}

:deep(.compass-rotation-marker) {
  width: 55px;
  height: 55px;
  fill: #1890ff;
  z-index: 10;
}
</style>
