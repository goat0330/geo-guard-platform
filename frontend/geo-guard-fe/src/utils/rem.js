// 基准大小 (对应 1920px 设计稿)
let baseSize = 16

// 获取客户端宽度
function getClientWidth() {
  return document.documentElement.clientWidth || window.innerWidth
}

// 根据屏幕宽度获取最大字体大小限制
export function getMaxFontSizeByScreen() {
  const width = getClientWidth()
  // 1k: <= 1920, max 20
  // 2k: <= 2560, max 28
  // 4k: > 2560, max 36
  if (width <= 1920) {
    return 20
  } else if (width <= 2560) {
    return 28
  } else {
    return 36
  }
}

// 设置 rem 函数
export function setRem() {
  const fontAppendScale = 0
  const maxSize = getMaxFontSizeByScreen()
  const maxScale = 2
  baseSize = 16
  // 当前页面宽度相对于 1920 宽的缩放比例
  const scale = document.documentElement.clientWidth / 1920
  // 设置页面根节点字体大小
  let fontSize = baseSize * Math.min(scale + fontAppendScale, maxScale)
  fontSize = fontSize < 12 ? 12 : fontSize > maxSize ? maxSize : fontSize
  document.documentElement.style.fontSize = fontSize + 'px'
}

// 初始化
setRem()

// 改变窗口大小时重新设置 rem
window.addEventListener('resize', setRem)

/**
 * ECharts rem 适配缩放工具 (对齐恩施地灾规范)
 * 根据当前 html 根字号动态计算图表属性尺寸 (字体、边距、柱宽、图标等)
 */
export function createEchartsScale() {
  const getRootFontSize = () =>
    parseFloat(getComputedStyle(document.documentElement).fontSize) || 16

  const scalePx = (designPx) => {
    const root = getRootFontSize()
    if (Array.isArray(designPx)) {
      return designPx.map((item) => Number((item * (root / 16)).toFixed(4)))
    }
    return Number((designPx * (root / 16)).toFixed(4))
  }

  return {
    px: scalePx,
    scale: scalePx,
    rootFontSize: getRootFontSize,
  }
}

/**
 * 快捷单次转换工具
 * @param {number|number[]} designPx 1920设计稿下的像素值
 * @returns {number|number[]} 当前屏幕尺寸下计算出的 px 数值
 */
export function echartsPx(designPx) {
  return createEchartsScale().px(designPx)
}
