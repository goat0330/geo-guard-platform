import { nextTick, onBeforeUnmount, ref } from 'vue'

/**
 * 垂直分界线拖动与高度缩放、一键展开/收起管理
 */
export function useVerticalSplitter(options = {}) {
  const {
    minMapHeight = 120,
    minBottomHeight = 160,
    collapseThreshold = 60,
    onResize,
  } = options

  const isMapCollapsed = ref(false)
  const isDragging = ref(false)
  // mapHeight 为 null 表示保持原生原版高度（flex: 1 1 54%; min-height: 360px），拖动后变为具体像素
  const mapHeight = ref(null)
  const lastMapHeight = ref(null)

  let startY = 0
  let startHeight = 0
  let containerRect = null

  const handleMouseMove = (e) => {
    if (!isDragging.value) return
    const deltaY = e.clientY - startY
    const targetHeight = startHeight + deltaY

    // 拖到最上方小于阈值时，自动收起地图
    if (targetHeight <= collapseThreshold) {
      isMapCollapsed.value = true
      mapHeight.value = 0
      onResize?.()
      return
    }

    // 从收起状态向下拖动时，自动恢复展开
    isMapCollapsed.value = false

    let maxHeight = Infinity
    if (containerRect) {
      maxHeight = Math.max(minMapHeight, containerRect.height - minBottomHeight)
    }

    const clampedHeight = Math.min(Math.max(targetHeight, minMapHeight), maxHeight)
    mapHeight.value = clampedHeight
    lastMapHeight.value = clampedHeight
    onResize?.()
  }

  const handleMouseUp = () => {
    if (!isDragging.value) return
    isDragging.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
    onResize?.()
  }

  const startDrag = (e, targetElement, containerElement) => {
    if (e.button !== 0) return
    isDragging.value = true
    startY = e.clientY

    const currentActualHeight = targetElement ? targetElement.getBoundingClientRect().height : 360
    if (!lastMapHeight.value) {
      lastMapHeight.value = currentActualHeight
    }
    startHeight = isMapCollapsed.value ? 0 : (mapHeight.value ?? currentActualHeight)
    containerRect = containerElement ? containerElement.getBoundingClientRect() : null

    document.body.style.cursor = 'row-resize'
    document.body.style.userSelect = 'none'
    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  const toggleCollapse = async (targetElement) => {
    if (isMapCollapsed.value) {
      // 展开地图：恢复原高度
      isMapCollapsed.value = false
      mapHeight.value = lastMapHeight.value ?? null
    } else {
      // 收起地图：记录当前实际 DOM 高度
      const currentActualHeight = targetElement ? targetElement.getBoundingClientRect().height : (mapHeight.value ?? 360)
      if (currentActualHeight > 0) {
        lastMapHeight.value = currentActualHeight
      }
      isMapCollapsed.value = true
      mapHeight.value = 0
    }
    await nextTick()
    onResize?.()
  }

  onBeforeUnmount(() => {
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  })

  return {
    isMapCollapsed,
    isDragging,
    mapHeight,
    startDrag,
    toggleCollapse,
  }
}
