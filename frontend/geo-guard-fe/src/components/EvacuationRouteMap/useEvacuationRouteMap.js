import * as mars3d from 'mars3d'
import * as Cesium from 'mars3d-cesium'
import { wktToGeoJSON } from '@terraformer/wkt'
import disasterMarker from '@/assets/imgs/cesium/disaster.png'

const ROUTE_COLOR = '#00F118'
const DANGER_COLOR = '#FF4747'
const SAFE_COLOR = '#00F118'

function getLineCoordinates(wkt) {
  if (!wkt) return []
  const geometry = wktToGeoJSON(wkt)
  if (geometry.type === 'LineString') return geometry.coordinates
  if (geometry.type === 'MultiLineString') return geometry.coordinates.flat(1)
  return []
}

/** 取要素（含面）的坐标集合：取景时危险区/安置区多边形也要一起入画，取外环即可 */
function getFeatureCoordinates(wkt) {
  if (!wkt) return []
  const geometry = wktToGeoJSON(wkt)
  if (geometry.type === 'LineString') return geometry.coordinates
  if (geometry.type === 'MultiLineString') return geometry.coordinates.flat(1)
  if (geometry.type === 'Polygon') return geometry.coordinates[0] || []
  if (geometry.type === 'MultiPolygon') return geometry.coordinates.flatMap((polygon) => polygon[0] || [])
  return []
}

function createNumberMarker(number) {
  const canvas = document.createElement('canvas')
  canvas.width = 80
  canvas.height = 80
  const context = canvas.getContext('2d')
  context.fillStyle = '#199D1F'
  context.beginPath()
  context.arc(40, 40, 36, 0, Math.PI * 2)
  context.fill()
  context.lineWidth = 4
  context.strokeStyle = '#ffffff'
  context.stroke()
  context.fillStyle = '#ffffff'
  context.font = 'bold 36px Arial'
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.fillText(String(number), 40, 42)
  return canvas
}

function routeCenter(wkt) {
  const coordinates = getLineCoordinates(wkt)
  if (!coordinates.length) return null
  const middle = coordinates[Math.floor(coordinates.length / 2)]
  return Cesium.Cartesian3.fromDegrees(middle[0], middle[1], 24)
}

/**
 * 取景范围 → Cesium 矩形（四周可按 margin 比例外扩）。
 *
 * 相机高度不在这里算：交给 Cesium 的 flyTo(Rectangle) 按当前视锥反算。
 * 曾经手写「垂直可视高度 ≈ 1.15 × 相机高度」来估高度，但 Cesium 的 fov 在容器宽 > 高时
 * 是**水平**视场角，宽屏下实际可视高度只有该估算的 1/aspect，导致内容上下被裁。
 * @param {Array<string>} wkts 参与取景的要素 wkt（线或面）
 * @param {number} margin 外扩比例，0.15 表示四周各留 15%
 */
function getExtentRectangle(wkts, margin = 0) {
  /* 取景以路线为主；无路线时传入的可能是面要素，统一用 getFeatureCoordinates 提取外环坐标 */
  const coordinates = wkts.flatMap((wkt) => getFeatureCoordinates(wkt))
  if (!coordinates.length) {
    /* 兜底：停在默认中心附近做一个近景，避免没有要素时镜头还在初始高度 */
    return Cesium.Rectangle.fromDegrees(108.2526 - 0.008, 29.4026 - 0.006, 108.2526 + 0.008, 29.4026 + 0.006)
  }

  const longitudes = coordinates.map(([longitude]) => longitude)
  const latitudes = coordinates.map(([, latitude]) => latitude)
  const west = Math.min(...longitudes)
  const east = Math.max(...longitudes)
  const south = Math.min(...latitudes)
  const north = Math.max(...latitudes)
  const paddingLng = (east - west) * margin
  const paddingLat = (north - south) * margin
  return Cesium.Rectangle.fromDegrees(west - paddingLng, south - paddingLat, east + paddingLng, north + paddingLat)
}

export function createEvacuationRouteMap(container, data, onSelect, options = {}) {
  /* viewScale：取景留白系数，> 1 时镜头整体拉远，保证路线四周有边距
     preserveDrawingBuffer：截图需要保留绘图缓存（WebGL 默认每帧后清空），
     只给需要离屏出图的地图实例开启，普通地图开启会白白多一份显存拷贝 */
  const {
    playAnimation = true,
    interactive = true,
    viewScale = 1.1,
    preserveDrawingBuffer = false,
    /* fitAll：取景时把路线与危险区、安置区合并成一份要素集再取包围盒。
       交互弹窗保持 false（主体是路线，面要素完整入画会把路线拉远看不清走向），
       离屏出图必须开，否则截图会缺内容 */
    fitAll = false,
  } = options
  let destroyed = false
  let resolveDrawn = null
  /* 绘制完成信号：面要素加载 + 相机定焦后 resolve，供离屏截图方判断何时开始等瓦片 */
  const drawnPromise = new Promise((resolve) => {
    resolveDrawn = resolve
  })
  let resizeObserver = null
  let clickHandler = null
  let fitTimer = null
  const routeGroups = []
  const dataSources = []
  const animationStart = Cesium.JulianDate.fromDate(new Date())
  const stopTimes = []

  const map = new mars3d.Map(container, {
    scene: {
      center: { lng: 108.2526, lat: 29.4026, alt: 1400, pitch: -90, heading: 0 },
      showSun: false,
      showMoon: false,
      showSkyBox: false,
      showSkyAtmosphere: false,
      fog: false,
      globe: { depthTestAgainstTerrain: false, enableLighting: false },
    },
    terrain: { show: false },
    control: {
      baseLayerPicker: false,
      sceneModePicker: false,
      navigationHelpButton: false,
      fullscreenButton: false,
      homeButton: false,
      geocoder: false,
      timeline: false,
      animation: false,
      locationBar: false,
      defaultContextMenu: false,
    },
    basemaps: [{ name: '高德卫星影像', type: 'gaode', layer: 'img_d', show: true }],
    /* 截图取 canvas 需要保留绘图缓存，默认不开，只给离屏出图的地图实例开启 */
    ...(preserveDrawingBuffer ? { contextOptions: { webgl: { preserveDrawingBuffer: true } } } : {}),
  })
  const viewer = map.viewer
  viewer.clock.shouldAnimate = false

  if (!interactive) {
    const cameraController = viewer.scene.screenSpaceCameraController
    cameraController.enableInputs = false
  }

  function remember(entity, group) {
    group?.push(entity)
    return entity
  }

  async function drawArea(items, options) {
    const features = (items || [])
      .filter((item) => item.wkt)
      .map((item) => ({
        type: 'Feature',
        properties: { name: item.name || options.name },
        geometry: wktToGeoJSON(item.wkt),
      }))
    if (!features.length) return

    const source = await Cesium.GeoJsonDataSource.load({ type: 'FeatureCollection', features }, { clampToGround: true })
    if (destroyed || viewer.isDestroyed()) return
    source.entities.values.forEach((entity) => {
      const hierarchy = entity.polygon?.hierarchy?.getValue(Cesium.JulianDate.now())
      entity.polygon.material = Cesium.Color.fromCssColorString(options.fill).withAlpha(options.opacity)
      if (hierarchy?.positions?.length) {
        remember(
          viewer.entities.add({
            polyline: {
              positions: [...hierarchy.positions, hierarchy.positions[0]],
              width: 2,
              clampToGround: true,
              material: Cesium.Color.fromCssColorString(options.stroke),
            },
          }),
        )
      }
    })
    dataSources.push(source)
    viewer.dataSources.add(source)
  }

  function drawRouteSegment(wkt, group, options = {}) {
    const coordinates = getLineCoordinates(wkt)
    if (coordinates.length < 2) return
    const positions = Cesium.Cartesian3.fromDegreesArray(coordinates.flat())
    remember(
      viewer.entities.add({
        polyline: {
          positions,
          width: 8,
          clampToGround: true,
          material: Cesium.Color.fromCssColorString(options.color).withAlpha(0.88),
        },
      }),
      group,
    )

    // 设计稿中的虚线位于绿色路线内部，作为行进导向，而不是额外的红绿接驳线。
    remember(
      viewer.entities.add({
        polyline: {
          positions,
          width: 2,
          clampToGround: true,
          material: new Cesium.PolylineDashMaterialProperty({
            color: Cesium.Color.WHITE.withAlpha(0.92),
            gapColor: Cesium.Color.TRANSPARENT,
            dashLength: 14,
          }),
        },
      }),
      group,
    )

    if (!options.animated) return
    const position = new Cesium.SampledPositionProperty()
    let elapsed = 0
    coordinates.forEach((coordinate, index) => {
      const current = Cesium.Cartesian3.fromDegrees(coordinate[0], coordinate[1], 0)
      if (index > 0) {
        const previous = coordinates[index - 1]
        elapsed += Cesium.Cartesian3.distance(Cesium.Cartesian3.fromDegrees(previous[0], previous[1], 0), current) / 10
      }
      position.addSample(Cesium.JulianDate.addSeconds(animationStart, elapsed, new Cesium.JulianDate()), current)
    })
    const stop = Cesium.JulianDate.addSeconds(animationStart, elapsed, new Cesium.JulianDate())
    stopTimes.push(stop)
    remember(
      viewer.entities.add({
        availability: new Cesium.TimeIntervalCollection([new Cesium.TimeInterval({ start: animationStart, stop })]),
        position,
        orientation: new Cesium.VelocityOrientationProperty(position),
        billboard: {
          // 与 geo-guard-ge 一致，使用人员撤离广告牌而不是通用方向箭头。
          image: disasterMarker,
          width: 32,
          height: 32,
          heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
          verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
      }),
      group,
    )
  }

  function drawRoutes() {
    ;(data.routes || []).forEach((route, index) => {
      const group = []
      drawRouteSegment(route.evacuationRoad, group, { color: ROUTE_COLOR, animated: playAnimation })
      drawRouteSegment(route.evacuationAreaToRoad, group, { color: ROUTE_COLOR })
      drawRouteSegment(route.roadToResettlement, group, { color: ROUTE_COLOR })
      const center = routeCenter(route.evacuationRoad)
      if (center) {
        remember(
          viewer.entities.add({
            position: center,
            billboard: {
              image: createNumberMarker(index + 1),
              width: 34,
              height: 34,
              heightReference: Cesium.HeightReference.RELATIVE_TO_GROUND,
              disableDepthTestDistance: Number.POSITIVE_INFINITY,
            },
            properties: { routeIndex: index },
          }),
          group,
        )
      }
      routeGroups.push(group)
    })
  }

  function configureClock() {
    if (!playAnimation || !stopTimes.length) return
    const stop = stopTimes.reduce((latest, current) =>
      Cesium.JulianDate.greaterThan(current, latest) ? current : latest,
    )
    viewer.clock.startTime = animationStart.clone()
    viewer.clock.currentTime = animationStart.clone()
    viewer.clock.stopTime = stop.clone()
    viewer.clock.clockRange = Cesium.ClockRange.LOOP_STOP
    viewer.clock.multiplier = 2
    viewer.clock.shouldAnimate = true
  }

  /** 路线 + 危险区 + 安置区的全部 wkt：合并后就是一份完整的要素集（等价于一个 GeoJSON FeatureCollection） */
  function collectAllWkts(routes) {
    return [
      ...routes.flatMap((route) => [route.evacuationAreaToRoad, route.evacuationRoad, route.roadToResettlement]),
      ...(data.disasterPolygonsWktList || []),
      ...(data.resettlementAreas || []).map((item) => item.areaWkt),
    ].filter(Boolean)
  }

  /** 收集参与取景的 wkt。
   *  默认（交互弹窗）只按路线取景：画面主体是撤离路线，危险区是否完整入画不要求，
   *  否则整条路线被拉得很远、看不清走向；没有路线时才退回面要素，避免镜头停在默认中心点。
   *  fitAll（离屏出图）：把路线、危险区、安置区合并成一份要素集统一取包围盒，保证截图内容不缺 */
  function collectExtentWkts(routes) {
    const routeWkts = routes
      .flatMap((route) => [route.evacuationAreaToRoad, route.evacuationRoad, route.roadToResettlement])
      .filter(Boolean)
    if (fitAll) {
      return collectAllWkts(routes)
    }
    if (routeWkts.length) {
      return routeWkts
    }
    return [
      ...(data.disasterPolygonsWktList || []),
      ...(data.resettlementAreas || []).map((item) => item.areaWkt),
    ]
  }

  function focusWkts(wkts, duration = 0.7) {
    /* 交给 Cesium 按视锥（含容器宽高比、镜头朝向）反算相机高度，保证矩形完整入画 */
    const rectangle = getExtentRectangle(wkts, Math.max(viewScale - 1, 0))
    viewer.camera.flyTo({
      destination: rectangle,
      orientation: { heading: 0, pitch: Cesium.Math.toRadians(-90), roll: 0 },
      duration,
    })
  }

  function selectRoute(index) {
    const activeIndex = Number.isInteger(index) ? index : -1
    routeGroups.forEach((group, routeIndex) => {
      const visible = activeIndex < 0 || activeIndex === routeIndex
      group.forEach((entity) => {
        entity.show = visible
      })
    })
    if (playAnimation && viewer.clock.startTime) {
      viewer.clock.currentTime = viewer.clock.startTime.clone()
      viewer.clock.shouldAnimate = true
    }
    const selectedRoute = data.routes?.[activeIndex]
    /* 未选中时全量取景；选中单条路线时聚焦该路线（面要素不参与，保持近景） */
    focusWkts(collectExtentWkts(selectedRoute ? [selectedRoute] : data.routes || []))
  }

  /* 危险区 / 安置区是面要素，GeoJsonDataSource 异步加载，绘制完成后取景才能把它们算进去 */
  const areaTasks = [
    drawArea(
      (data.disasterPolygonsWktList || []).map((wkt, index) => ({ wkt, name: `危险区${index + 1}` })),
      { name: '危险区', fill: DANGER_COLOR, stroke: '#FF0000', opacity: 0.35 },
    ),
    drawArea(
      (data.resettlementAreas || []).map((item) => ({ wkt: item.areaWkt, name: item.name })),
      { name: '安置区', fill: SAFE_COLOR, stroke: SAFE_COLOR, opacity: 0.5 },
    ),
  ]
  drawRoutes()
  configureClock()

  if (interactive) {
    clickHandler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
    clickHandler.setInputAction((event) => {
      const picked = viewer.scene.pick(event.position)
      const index = Number(picked?.id?.properties?.routeIndex?.getValue?.())
      if (Number.isInteger(index)) onSelect?.(index)
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
  }

  resizeObserver = new ResizeObserver(() => viewer.resize())
  resizeObserver.observe(container)
  fitTimer = window.setTimeout(async () => {
    if (destroyed) return
    /* 等面要素绘制完成后再取景，此时画面上路线、危险区、安置区都已就位；
       面要素加载异常也不影响取景，按已绘制内容定镜头即可 */
    try {
      await Promise.all(areaTasks)
    } catch (error) {
      console.warn('[撤离路线地图] 面要素加载失败，按已绘制内容取景:', error)
    }
    if (destroyed) return
    /* 初始全景取景：只按路线包围盒定镜头，危险区允许只露一部分 */
    focusWkts(collectExtentWkts(data.routes || []), 0)
    resolveDrawn()
  }, 0)

  return {
    selectRoute,
    viewer,
    /* 绘制完成（面要素加载 + 相机定焦）：离屏截图方 await 它之后再去等瓦片 */
    whenDrawn: () => drawnPromise,
    destroy() {
      destroyed = true
      /* 地图被提前销毁时唤醒等待方，避免截图流程悬挂 */
      resolveDrawn()
      window.clearTimeout(fitTimer)
      resizeObserver?.disconnect()
      clickHandler?.destroy()
      dataSources.forEach((source) => viewer.dataSources.remove(source, true))
      map.destroy()
    },
  }
}
