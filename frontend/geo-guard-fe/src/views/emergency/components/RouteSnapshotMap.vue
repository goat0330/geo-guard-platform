<template>
  <!-- 离屏路线大图：挂在执行结果弹窗内，弹窗出现即挂载、截图完成即销毁。
       结果卡里的预览地图只有卡片大小（358×179），截出来太糊，故用这张移出视口的大图单独出图 -->
  <div class="route-snapshot" aria-hidden="true">
    <div
      ref="containerRef"
      class="route-snapshot-canvas"
      :style="{ width: `${SNAPSHOT_WIDTH}px`, height: `${SNAPSHOT_HEIGHT}px` }"
    ></div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createEvacuationRouteMap } from '@/components/EvacuationRouteMap/useEvacuationRouteMap.js'
import 'mars3d/mars3d.css'

defineOptions({ name: 'RouteSnapshotMap' })

const props = defineProps({
  /** 撤离路线结果，与「查看路线详情」弹窗同源：{ handleId, routes, disasterPolygonsWktList, resettlementAreas }。
   *  没有 routes 时不建图（生成中/无可用路线），数据一到就出图，不阻塞右侧执行过程 */
  data: { type: Object, default: null },
})

const emit = defineEmits(['capture'])

/* 出图尺寸：16:9、1280×720，兼顾清晰度与 dataURL 体积 */
const SNAPSHOT_WIDTH = 1280
const SNAPSHOT_HEIGHT = 720
/* 瓦片兜底时长：超时也照截，拿到已加载的部分影像，不无限等下去 */
const TILE_TIMEOUT = 8000
/* 贴地折线的几何是异步准备的：地球瓦片就绪时路线可能还没画上（画面只有编号点），
   先留一段固定准备时间，再按「画面里是否出现路线绿」判定，缺路线就继续等 */
const SETTLE_DELAY = 1000
const ROUTE_RETRY_TIMES = 4
const ROUTE_RETRY_DELAY = 600
/* 路线绿像素下限：编号点为深绿(#199D1F)不计入，实测有线约 1000+、无线约 0 */
const ROUTE_PIXEL_MIN = 100
/* 结构指纹缓存：同一份路线数据只出一次图，弹窗关闭重开时直接复用，避免重复建图 */
const snapshotCache = new Map()
const CACHE_LIMIT = 4

const containerRef = ref(null)
let routeMap = null
let cancelTileWait = null
/* 已出图/已尝试的指纹：结果数据被接口刷新重建时不再重复出图 */
let capturedKey = ''
let capturing = false

/**
 * 结构指纹：轮询刷新会重建 routeResult 对象，只能按内容判断是否同一份数据。
 * handleId 是后端给的路线标识，最稳；缺失时退回各段路线 WKT 的长度组合
 */
const snapshotKey = (data) => {
  const routes = Array.isArray(data?.routes) ? data.routes : []
  if (!routes.length) return ''
  if (data.handleId) return String(data.handleId)
  return routes
    .map((route) =>
      [route.evacuationAreaToRoad, route.evacuationRoad, route.roadToResettlement]
        .map((wkt) => (wkt ? String(wkt).length : 0))
        .join('-'),
    )
    .join('|')
}

/** dataURL 体积：base64 每 4 字符对应 3 字节，用于文件卡展示真实大小 */
const dataUrlSize = (url) => {
  const base64 = url.slice(url.indexOf(',') + 1)
  return Math.round((base64.length * 3) / 4)
}

const sleep = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms))

/** 等两帧：瓦片加载完成后画面还要再合成一帧，立刻取 canvas 可能拿到上一帧 */
const nextFrames = (count = 2) =>
  new Promise((resolve) => {
    const step = (remaining) => {
      if (remaining <= 0) {
        resolve()
        return
      }
      window.requestAnimationFrame(() => step(remaining - 1))
    }
    step(count)
  })

/**
 * 统计出图里的路线绿像素：路线为 #00F118 高不透明度，编号点是深绿 #199D1F、
 * 危险区是红、安置区虽同为绿色但只有 0.5 透明度，都不满足该阈值，故可作为「路线是否画上」的判据
 */
const countRoutePixels = async (url) => {
  const image = new Image()
  image.src = url
  await image.decode()
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 360
  const context = canvas.getContext('2d')
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  const { data } = context.getImageData(0, 0, canvas.width, canvas.height)
  let count = 0
  for (let i = 0; i < data.length; i += 4) {
    if (data[i] < 100 && data[i + 1] > 200 && data[i + 2] < 100) count += 1
  }
  return count
}

/** 等地球瓦片加载完：相机定焦后影像才开始请求，队列归零才算画面完整 */
const waitForTiles = (viewer) =>
  new Promise((resolve) => {
    const globe = viewer?.scene?.globe
    if (!globe || globe.tilesLoaded) {
      resolve()
      return
    }
    let timer = null
    let removeListener = null
    const finish = () => {
      window.clearTimeout(timer)
      removeListener?.()
      removeListener = null
      cancelTileWait = null
      resolve()
    }
    removeListener = globe.tileLoadProgressEvent.addEventListener((queuedTileCount) => {
      if (queuedTileCount === 0) finish()
    })
    timer = window.setTimeout(finish, TILE_TIMEOUT)
    /* 弹窗提前关闭时由 destroyMap 直接结束等待，不留悬挂的定时器 */
    cancelTileWait = finish
  })

const destroyMap = () => {
  cancelTileWait?.()
  cancelTileWait = null
  routeMap?.destroy()
  routeMap = null
}

/** 出图：绘制 → 等绘制完成 → 等瓦片 → 取帧 → 回抛 dataURL → 销毁地图释放 WebGL 上下文 */
const capture = async () => {
  const container = containerRef.value
  const key = snapshotKey(props.data)
  const cached = snapshotCache.get(key)
  if (cached) {
    emit('capture', { ...cached, key })
    return
  }

  const map = createEvacuationRouteMap(container, props.data, null, {
    playAnimation: false,
    interactive: false,
    preserveDrawingBuffer: true,
    /* 截图必须把路线、危险区、安置区一起框进画面，否则会缺内容（交互弹窗保持只按路线取景）。
       viewScale 是四周留白比例：1.15 表示内容外各留 15% 边距 */
    fitAll: true,
    viewScale: 1.15,
  })
  routeMap = map
  try {
    await map.whenDrawn()
    /* 相机是瞬移过去的，先等两帧让 Cesium 处理相机变更、发出新视野的瓦片请求；
       否则 read 到的 tilesLoaded 还是上一个视野的状态，会截到没影像的空图 */
    await nextFrames(2)
    await waitForTiles(map.viewer)
    /* 瓦片就绪不代表路线已画上：贴地折线的几何还在异步准备，先留出准备时间 */
    await sleep(SETTLE_DELAY)
    let url = ''
    for (let attempt = 0; attempt <= ROUTE_RETRY_TIMES; attempt += 1) {
      /* 取帧前等两帧，确保最后一帧已完成合成 */
      await nextFrames(2)
      /* 等待期间弹窗已关闭：地图已销毁，不再碰 viewer */
      if (!routeMap) return
      url = map.viewer.canvas.toDataURL('image/png') || ''
      if (!url) return
      if ((await countRoutePixels(url)) >= ROUTE_PIXEL_MIN) break
      /* 画面里还没有路线：再等一会儿重取一帧，避免截到只有编号点的图 */
      await sleep(ROUTE_RETRY_DELAY)
    }
    const payload = { url, size: dataUrlSize(url) }
    snapshotCache.set(key, payload)
    if (snapshotCache.size > CACHE_LIMIT) {
      snapshotCache.delete(snapshotCache.keys().next().value)
    }
    emit('capture', { ...payload, key })
  } finally {
    /* 无论成功失败都要销毁，否则 WebGL 上下文与瓦片请求会一直留着 */
    destroyMap()
  }
}

const runCapture = async () => {
  if (capturing || !containerRef.value) return
  const key = snapshotKey(props.data)
  if (!key || key === capturedKey) return
  capturing = true
  /* 先记指纹：失败不自动重试，避免同一份数据反复建图拖垮页面 */
  capturedKey = key
  try {
    await capture()
  } catch (error) {
    console.warn('[智能预案] 撤离路线示意图截图失败，文件卡保持默认图标:', error)
  } finally {
    capturing = false
  }
}

const snapshotKeyRef = computed(() => snapshotKey(props.data))

onMounted(runCapture)

/* 弹窗打开时路线数据可能还没到：等数据到位（指纹由空变有）再出图，指纹相同则跳过 */
watch(snapshotKeyRef, runCapture)

onBeforeUnmount(destroyMap)
</script>

<style lang="less" scoped>
/* 隐藏方式必须是「移出视口」而不是 display:none / visibility:hidden——
   容器尺寸为 0 时 Cesium 的 canvas 也是 0×0，截出来是空图 */
.route-snapshot {
  position: fixed;
  top: 0;
  left: -20000px;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}
</style>
