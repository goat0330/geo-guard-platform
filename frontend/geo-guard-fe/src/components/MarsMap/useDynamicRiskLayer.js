import { onScopeDispose, ref, watch } from 'vue'
import * as Cesium from 'mars3d-cesium'
import { booleanPointInPolygon, point } from '@turf/turf'
import wellknown from 'wellknown'
import { RISK_LEVEL_COLOR } from '@/utils/enum.js'
import { getRiskSlopeSelectionKey } from '@/utils/riskEvaluation.js'

const FILL_ALPHA = 0.3
const BORDER_WIDTH = 2
const HIGHLIGHT_COLOR = '#00FFFF'
const HIGHLIGHT_FILL_ALPHA = 0.35
const HIGHLIGHT_BORDER_WIDTH = 4
const FLY_RANGE_SCALE = 12
const MIN_FLY_RANGE = 3000
const PRIMITIVE_READY_TIMEOUT = 20000

/** 批量贴地图元：按风险等级合批，5000 个面共用一个点击处理器。 */
export const useDynamicRiskLayer = (dataset, onSelect) => {
  const rendering = ref(false)
  let viewer
  let handler
  let generation = 0
  let stopWaitingForReady
  let primitives = []
  let highlightPrimitives = []
  let featureIndex = new Map()
  let slopeIndex = new Map()
  const geometryCache = new WeakMap()
  const getPolygons = (row) => {
    let polygons = geometryCache.get(row)
    if (polygons) return polygons
    try {
      const geometry = wellknown.parse(row?.slopeUnit?.wkt || row?.wkt || '')
      polygons = geometry?.type === 'Polygon' ? [geometry.coordinates] : geometry?.type === 'MultiPolygon' ? geometry.coordinates : []
    } catch {
      polygons = []
    }
    if (row && typeof row === 'object') geometryCache.set(row, polygons)
    return polygons
  }
  const clearHighlight = () => {
    if (viewer && !viewer.isDestroyed()) {
      highlightPrimitives.forEach((primitive) => viewer.scene.groundPrimitives.remove(primitive))
    }
    highlightPrimitives = []
  }
  const clear = () => {
    clearHighlight()
    if (viewer && !viewer.isDestroyed()) primitives.forEach((primitive) => viewer.scene.groundPrimitives.remove(primitive))
    primitives = []
    featureIndex.clear()
    slopeIndex.clear()
  }
  const waitForPrimitivesReady = (targets, current) => new Promise((resolve) => {
    const scene = viewer?.scene
    if (!scene || !targets.length || targets.every((primitive) => primitive.ready)) {
      resolve()
      return
    }

    let timeoutId
    const finish = () => {
      scene.postRender.removeEventListener(checkReady)
      window.clearTimeout(timeoutId)
      if (stopWaitingForReady === finish) stopWaitingForReady = undefined
      resolve()
    }
    const checkReady = () => {
      if (current !== generation || !viewer || viewer.isDestroyed()) {
        finish()
        return
      }
      if (targets.every((primitive) => primitive.ready)) {
        finish()
        return
      }
      scene.requestRender()
    }

    stopWaitingForReady?.()
    stopWaitingForReady = finish
    scene.postRender.addEventListener(checkReady)
    timeoutId = window.setTimeout(finish, PRIMITIVE_READY_TIMEOUT)
    scene.requestRender()
  })
  const hierarchy = (rings) => new Cesium.PolygonHierarchy(
    Cesium.Cartesian3.fromDegreesArray(rings[0].flatMap(([lng, lat]) => [lng, lat])),
    rings.slice(1).map((ring) => new Cesium.PolygonHierarchy(Cesium.Cartesian3.fromDegreesArray(ring.flatMap(([lng, lat]) => [lng, lat])))),
  )
  const createBorderInstance = (ring, color, width = BORDER_WIDTH) => {
    if (!Array.isArray(ring) || ring.length < 3) return null
    const first = ring[0]
    const last = ring[ring.length - 1]
    const points = first[0] === last[0] && first[1] === last[1] ? ring.slice(0, -1) : ring
    if (points.length < 3) return null
    return new Cesium.GeometryInstance({
      geometry: new Cesium.GroundPolylineGeometry({
        positions: Cesium.Cartesian3.fromDegreesArray(points.flatMap(([lng, lat]) => [lng, lat])),
        width,
        loop: true,
      }),
      attributes: {
        color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.fromCssColorString(color)),
      },
    })
  }
  const createFeatureRecord = (item, rings) => {
    const bounds = { west: Infinity, south: Infinity, east: -Infinity, north: -Infinity }
    rings[0].forEach(([lng, lat]) => {
      bounds.west = Math.min(bounds.west, lng)
      bounds.south = Math.min(bounds.south, lat)
      bounds.east = Math.max(bounds.east, lng)
      bounds.north = Math.max(bounds.north, lat)
    })
    return {
      item,
      bounds,
      polygon: { type: 'Polygon', coordinates: rings },
    }
  }
  const getClickCoordinates = (position) => {
    const ray = viewer.camera.getPickRay(position)
    const cartesian = ray && viewer.scene.globe.pick(ray, viewer.scene)
    if (!cartesian) return null
    const cartographic = Cesium.Cartographic.fromCartesian(cartesian)
    return [Cesium.Math.toDegrees(cartographic.longitude), Cesium.Math.toDegrees(cartographic.latitude)]
  }
  const containsCoordinates = (record, coordinates, clickPoint) => {
    const [lng, lat] = coordinates
    const { bounds } = record
    if (lng < bounds.west || lng > bounds.east || lat < bounds.south || lat > bounds.north) return false
    try {
      return booleanPointInPolygon(clickPoint, record.polygon)
    } catch {
      return false
    }
  }
  const findClickedSlope = (position) => {
    const coordinates = getClickCoordinates(position)
    if (!coordinates) return null
    const clickPoint = point(coordinates)
    const pickedIds = viewer.scene.drillPick(position)
      .map((picked) => picked?.id)
      .filter((id) => featureIndex.has(id))
    for (const id of pickedIds) {
      const record = featureIndex.get(id)
      if (containsCoordinates(record, coordinates, clickPoint)) return record.item
    }
    // Cesium 贴地面拾取异常时，通过预计算包围盒缩小范围后再做精确判断。
    for (const record of featureIndex.values()) {
      if (containsCoordinates(record, coordinates, clickPoint)) return record.item
    }
    return null
  }
  const updateHighlight = () => {
    clearHighlight()
    if (!viewer || viewer.isDestroyed()) return
    const activeSlopeKey = String(dataset.activeSlopeKey.value || '')
    const records = activeSlopeKey ? (slopeIndex.get(activeSlopeKey) || []) : []
    const fillColor = Cesium.Color.fromCssColorString(HIGHLIGHT_COLOR).withAlpha(HIGHLIGHT_FILL_ALPHA)
    const fillInstances = records.map((record) => new Cesium.GeometryInstance({
      geometry: new Cesium.PolygonGeometry({
        polygonHierarchy: hierarchy(record.polygon.coordinates),
        vertexFormat: Cesium.PerInstanceColorAppearance.VERTEX_FORMAT,
      }),
      attributes: {
        color: Cesium.ColorGeometryInstanceAttribute.fromColor(fillColor),
      },
    }))
    const borderInstances = records.flatMap((record) => record.polygon.coordinates
      .map((ring) => createBorderInstance(ring, HIGHLIGHT_COLOR, HIGHLIGHT_BORDER_WIDTH))
      .filter(Boolean))
    if (!fillInstances.length) return
    // 填充和轮廓复用同一份 WKT，确保高亮范围完全一致。
    highlightPrimitives.push(viewer.scene.groundPrimitives.add(new Cesium.GroundPrimitive({
      geometryInstances: fillInstances,
      appearance: new Cesium.PerInstanceColorAppearance({ translucent: true, closed: false }),
      asynchronous: true,
      allowPicking: false,
      releaseGeometryInstances: true,
    })))
    if (borderInstances.length) {
      highlightPrimitives.push(viewer.scene.groundPrimitives.add(new Cesium.GroundPolylinePrimitive({
        geometryInstances: borderInstances,
        appearance: new Cesium.PolylineColorAppearance({ translucent: false }),
        asynchronous: true,
        allowPicking: false,
        releaseGeometryInstances: true,
      })))
    }
    viewer.scene.requestRender()
  }
  const draw = async () => {
    const current = ++generation
    stopWaitingForReady?.()
    clear()
    if (!viewer || viewer.isDestroyed()) {
      rendering.value = false
      return
    }
    rendering.value = true
    try {
      const groups = new Map()
      const borderInstances = []
      const index = new Map()
      const nextSlopeIndex = new Map()
      const items = dataset.slopeItems.value
      for (let i = 0; i < items.length; i += 1) {
        if (current !== generation) return
        const item = items[i]
        const row = item.raw
        const polygons = getPolygons(row)
        const color = RISK_LEVEL_COLOR[item.levelValue]
        if (!color) continue
        const instances = groups.get(item.levelValue) || []
        polygons.forEach((rings, part) => {
          try {
            // 接口记录 ID 可能为空或重复，加入数据序号保证 GeometryInstance ID 唯一。
            const id = `dynamic-risk:${item.id || item.slopeUnitId || 'unknown'}:${i}:${part}`
            const baseColor = Cesium.Color.fromCssColorString(color).withAlpha(FILL_ALPHA)
            instances.push(new Cesium.GeometryInstance({
              id,
              geometry: new Cesium.PolygonGeometry({ polygonHierarchy: hierarchy(rings), vertexFormat: Cesium.PerInstanceColorAppearance.VERTEX_FORMAT }),
              attributes: { color: Cesium.ColorGeometryInstanceAttribute.fromColor(baseColor) },
            }))
            const record = createFeatureRecord(item, rings)
            index.set(id, record)
            const slopeKey = getRiskSlopeSelectionKey(item)
            if (slopeKey) {
              const records = nextSlopeIndex.get(slopeKey) || []
              records.push(record)
              nextSlopeIndex.set(slopeKey, records)
            }
            // 所有边界合并到一个贴地图元，避免为每个斜坡创建独立 Entity。
            rings.forEach((ring) => {
              const borderInstance = createBorderInstance(ring, color)
              if (borderInstance) borderInstances.push(borderInstance)
            })
          } catch { /* 无效 WKT 留空，不影响其他斜坡。 */ }
        })
        groups.set(item.levelValue, instances)
        // 分片转换几何，让浏览器在批次间处理滚动、筛选和地图交互。
        if (i % 100 === 99) await new Promise((resolve) => setTimeout(resolve, 0))
      }
      if (current !== generation || viewer.isDestroyed()) return
      featureIndex = index
      slopeIndex = nextSlopeIndex
      groups.forEach((instances) => {
        if (!instances.length) return
        const primitive = viewer.scene.groundPrimitives.add(new Cesium.GroundPrimitive({
          geometryInstances: instances,
          appearance: new Cesium.PerInstanceColorAppearance({ translucent: true, closed: false }),
          asynchronous: true,
          allowPicking: true,
          releaseGeometryInstances: true,
        }))
        primitives.push(primitive)
      })
      if (borderInstances.length) {
        primitives.push(viewer.scene.groundPrimitives.add(new Cesium.GroundPolylinePrimitive({
          geometryInstances: borderInstances,
          appearance: new Cesium.PolylineColorAppearance({ translucent: false }),
          asynchronous: true,
          allowPicking: false,
          releaseGeometryInstances: true,
        })))
      }
      updateHighlight()
      viewer.scene.requestRender()
      // GroundPrimitive 会在 Web Worker 中异步生成，等 ready 后再关闭渲染提示。
      await waitForPrimitivesReady([...primitives], current)
    } finally {
      if (current === generation) rendering.value = false
    }
  }
  const flyToSlope = (slope) => {
    if (!viewer || viewer.isDestroyed()) return false
    const records = slopeIndex.get(getRiskSlopeSelectionKey(slope)) || []
    const polygons = records.length
      ? records.map((record) => record.polygon.coordinates)
      : getPolygons(slope?.raw)
    const positions = polygons.flatMap((rings) => rings[0].map(([lng, lat]) => (
      Cesium.Cartesian3.fromDegrees(lng, lat)
    )))
    if (!positions.length) return false
    const boundingSphere = Cesium.BoundingSphere.fromPoints(positions)
    const range = Math.max(boundingSphere.radius * FLY_RANGE_SCALE, MIN_FLY_RANGE)
    // 高亮定位仅调整观察距离，保留用户当前的航向角和俯仰角。
    const { heading, pitch } = viewer.camera
    viewer.camera.flyToBoundingSphere(boundingSphere, {
      duration: 1.5,
      offset: new Cesium.HeadingPitchRange(
        heading,
        pitch,
        range,
      ),
    })
    return true
  }
  const attach = (nextViewer) => {
    clear()
    handler?.destroy()
    viewer = nextViewer
    handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
    handler.setInputAction(({ position }) => {
      const slope = findClickedSlope(position)
      if (slope) {
        // 由上层业务判断是否有业务弹窗信息后再决定是否高亮，底层不先行设置高亮
        onSelect?.(slope)
      }
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
    void draw()
  }
  watch(dataset.slopeItems, draw)
  watch(dataset.activeSlopeKey, updateHighlight)
  onScopeDispose(() => {
    generation += 1
    stopWaitingForReady?.()
    handler?.destroy()
    clear()
    rendering.value = false
    viewer = null
  })
  return { attach, flyToSlope, rendering }
}
