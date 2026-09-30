import { onScopeDispose } from 'vue'
import * as mars3d from 'mars3d'
import { wktToGeoJSON } from '@terraformer/wkt'

/**
 * 空间分析页的斜坡单元选择图层。
 * 图层只保留当前行政区划的数据，切换区域或离开页面时立即释放。
 */
export function useSlopeUnitLayer(onSelect) {
  let map = null
  let slopeLayer = null
  let selectedSlopeLayer = null
  let selectedSlopeId = ''
  let unitsById = new Map()

  function removeLayer(layer) {
    if (!layer || !map) return
    map.removeLayer(layer, true)
  }

  function clearSelection() {
    removeLayer(selectedSlopeLayer)
    selectedSlopeLayer = null
    selectedSlopeId = ''
  }

  /** 取消选中后恢复当前行政区划下的全部斜坡单元。 */
  function restoreAll() {
    clearSelection()
    if (slopeLayer) slopeLayer.show = true
  }

  function clear() {
    clearSelection()
    removeLayer(slopeLayer)
    slopeLayer = null
    unitsById = new Map()
  }

  function toFeature(unit) {
    if (unit?.id === null || unit?.id === undefined || !unit?.wkt) return null
    try {
      const geometry = wktToGeoJSON(unit.wkt)
      if (!['Polygon', 'MultiPolygon'].includes(geometry?.type)) return null
      return {
        type: 'Feature',
        properties: { id: String(unit.id) },
        geometry,
      }
    } catch {
      return null
    }
  }

  function showSelection(unit) {
    clearSelection()
    const feature = toFeature(unit)
    if (!feature || !map) return

    selectedSlopeId = feature.properties.id
    // 已确定分析对象时，隐藏其余斜坡，避免地图信息干扰确认。
    if (slopeLayer) slopeLayer.show = false
    selectedSlopeLayer = new mars3d.layer.GeoJsonLayer({
      name: '空间分析已选斜坡单元',
      data: { type: 'FeatureCollection', features: [feature] },
      symbol: {
        styleOptions: {
          color: '#007BFF',
          opacity: 0.42,
          outline: true,
          outlineColor: '#007BFF',
          outlineWidth: 3,
          clampToGround: true,
        },
      },
    })
    map.addLayer(selectedSlopeLayer)
    // 图层范围即为当前斜坡，使用图层定位可确保其居于视图中心。
    void selectedSlopeLayer.flyTo({
      duration: 0.6,
      scale: 1.5,
      minHeight: 500,
    })
  }

  function draw(units = []) {
    clear()
    if (!map) return 0

    const features = []
    const nextUnitsById = new Map()
    for (const unit of units) {
      const feature = toFeature(unit)
      if (!feature) continue
      features.push(feature)
      nextUnitsById.set(feature.properties.id, unit)
    }
    unitsById = nextUnitsById
    if (!features.length) return 0

    slopeLayer = new mars3d.layer.GeoJsonLayer({
      name: '空间分析斜坡单元',
      data: { type: 'FeatureCollection', features },
      symbol: {
        styleOptions: {
          color: '#3595FB',
          opacity: 0.22,
          outline: true,
          outlineColor: '#007BFF',
          outlineWidth: 2,
          clampToGround: true,
        },
      },
    })
    slopeLayer.on(mars3d.EventType.click, (event) => {
      const slope = unitsById.get(String(event?.graphic?.attr?.id || ''))
      if (!slope) return
      showSelection(slope)
      onSelect?.(slope)
    })
    map.addLayer(slopeLayer)
    return features.length
  }

  function attach(nextMap) {
    map = nextMap || null
  }

  /**
   * 判断地图点击是否仍落在当前选中的斜坡上。
   * @param {object} event Mars3D 地图点击事件
   * @returns {boolean} 是否点击当前斜坡
   */
  function isSelectedEvent(event) {
    return Boolean(selectedSlopeId) && String(event?.graphic?.attr?.id || '') === selectedSlopeId
  }

  onScopeDispose(() => {
    clear()
    map = null
  })

  return { attach, clear, clearSelection, draw, showSelection, restoreAll, isSelectedEvent }
}
