import { nextTick, onBeforeUnmount, watch } from 'vue'

/**
 * 在激活后将图表进度从 0 动画到 1，并负责清理动画帧。
 * @param {Object} options 动画配置
 * @param {import('vue').Ref<boolean>} options.active 是否开始动画
 * @param {(progress: number) => void} options.render 按进度更新图表
 * @param {number} [options.duration=1800] 动画时长
 * @returns {{ stop: () => void }} 动画停止方法
 */
export function useChartProgressAnimation({ active, render, duration = 1800 }) {
  let animationFrameId = 0

  function stop() {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = 0
  }

  async function start() {
    stop()
    await nextTick()

    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (reduceMotion || duration <= 0) {
      render(1)
      return
    }

    render(0)
    const startTime = performance.now()

    const update = (currentTime) => {
      const elapsedProgress = Math.min((currentTime - startTime) / duration, 1)
      const easedProgress = 1 - Math.pow(1 - elapsedProgress, 3)
      render(easedProgress)

      if (elapsedProgress < 1) {
        animationFrameId = requestAnimationFrame(update)
      }
    }

    animationFrameId = requestAnimationFrame(update)
  }

  watch(
    active,
    (isActive) => {
      if (isActive) start()
    },
    { immediate: true },
  )

  onBeforeUnmount(stop)

  return { stop }
}
