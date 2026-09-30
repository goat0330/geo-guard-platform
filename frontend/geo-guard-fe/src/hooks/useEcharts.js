import { ref, shallowRef, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useResizeObserver } from '@vueuse/core'
import { createEchartsScale } from '@/utils/rem.js'

/**
 * 防抖函数
 */
function debounce(fn, delay = 200) {
  let timer = null
  const debounced = function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      timer = null
      fn.apply(this, args)
    }, delay)
  }
  debounced.cancel = () => {
    if (timer) clearTimeout(timer)
    timer = null
  }
  return debounced
}

/**
 * ECharts rem 适配组合式函数 (Composition API)
 * 对齐恩施地灾项目的 ECharts 自适应方案，同时强化容器尺寸监听与内存泄漏防护
 *
 * @param {Object} [options] 配置项
 * @param {Function} [options.getOption] 动态获取 option 的函数，入参为 (px)，返回最新 echarts option
 * @param {boolean} [options.autoInit=true] 是否在 mounted 时自动初始化
 * @param {boolean} [options.initOnVisible=false] 是否等图表首次进入可视区再初始化
 * @returns {Object} { chartRef, chartInstance, px, setOption, renderChart, resize, dispose }
 */
export function useEcharts(options = {}) {
  const { getOption = null, autoInit = true, initOnVisible = false } = options
  const chartRef = ref(null)
  const chartInstance = shallowRef(null)
  const { px } = createEchartsScale()

  let currentGetOption = getOption
  let visibilityObserver = null
  let canRender = !initOnVisible

  // 尺寸调整防抖处理
  const resize = debounce(() => {
    if (chartInstance.value && !chartInstance.value.isDisposed()) {
      chartInstance.value.resize()
    }
  }, 100)

  // 渲染/重新渲染图表
  const renderChart = async (customGetOption) => {
    if (!canRender) return

    await nextTick()
    if (!chartRef.value) return

    if (!chartInstance.value || chartInstance.value.isDisposed()) {
      chartInstance.value = echarts.init(chartRef.value)
    }

    const getter = customGetOption || currentGetOption
    if (getter && typeof getter === 'function') {
      currentGetOption = getter
      const opt = getter(px)
      if (opt) {
        chartInstance.value.setOption(opt, { notMerge: true })
      }
    }
  }

  // 设置静态或动态 Option
  const setOption = (opt, notMerge = true, lazyUpdate = false) => {
    if (!chartInstance.value || chartInstance.value.isDisposed()) {
      if (chartRef.value) {
        chartInstance.value = echarts.init(chartRef.value)
      } else {
        return
      }
    }
    chartInstance.value.setOption(opt, { notMerge, lazyUpdate })
  }

  // 监听窗口尺寸改变 (更新 rem 比例同时重新渲染 option，保证字号边距按 rem 比例无失真缩放)
  const handleWindowResize = debounce(() => {
    if (currentGetOption && typeof currentGetOption === 'function') {
      renderChart(currentGetOption)
    } else {
      resize()
    }
  }, 150)

  // 监听容器 DOM 尺寸改变 (例如侧边栏展开/折叠、面板拖拽调整)
  useResizeObserver(chartRef, () => {
    resize()
  })

  onMounted(() => {
    window.addEventListener('resize', handleWindowResize)
    if (autoInit && currentGetOption) {
      if (initOnVisible && typeof IntersectionObserver !== 'undefined' && chartRef.value) {
        visibilityObserver = new IntersectionObserver(
          ([entry]) => {
            if (!entry?.isIntersecting) return

            canRender = true
            renderChart()
            visibilityObserver?.disconnect()
            visibilityObserver = null
          },
          { threshold: 0.2 },
        )
        visibilityObserver.observe(chartRef.value)
      } else {
        canRender = true
        renderChart()
      }
    }
  })

  // 内存泄漏防护：组件卸载前严格销毁实例、移除事件监听、清空引用
  const dispose = () => {
    window.removeEventListener('resize', handleWindowResize)
    visibilityObserver?.disconnect()
    visibilityObserver = null
    handleWindowResize.cancel()
    resize.cancel()
    if (chartInstance.value && !chartInstance.value.isDisposed()) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }
  }

  onBeforeUnmount(() => {
    dispose()
  })

  return {
    chartRef,
    chartInstance,
    px,
    setOption,
    renderChart,
    resize,
    dispose,
  }
}
