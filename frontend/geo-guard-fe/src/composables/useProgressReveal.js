import { nextTick, onMounted, ref, watch } from 'vue'
import { useChartProgressAnimation } from './useChartProgressAnimation.js'
import { useEnterView } from './useEnterView.js'

/**
 * 数据变化效果：把 0 → 1 的进度动画作用到图表 / 进度环 / 进度条上，
 * 与首页看板的动效一致（easeOutCubic，默认 1400ms）。
 * 触发时机有三处兜底：元素进入视口、依赖数据刷新、组件挂载，
 * 避免概览面板在滚动容器内时视口回调不触发、动画一直停在 0。
 *
 * @param {Function} deps getter，返回用于判断数据变化的响应式数据
 * @param {number} [duration=1400] 动画时长
 * @returns {{ targetRef: import('vue').Ref<HTMLElement|null>, progress: import('vue').Ref<number> }}
 */
export function useProgressReveal(deps, duration = 1400) {
  const { targetRef, hasEntered } = useEnterView()
  /** 动画开关：先复位再启动，确保每次数据刷新都能重放 */
  const active = ref(false)
  const progress = ref(0)

  useChartProgressAnimation({
    active,
    duration,
    render: (value) => {
      progress.value = value
    },
  })

  /** 复位再启动：保证每次都能看到从 0 到目标值的过程 */
  const play = async () => {
    active.value = false
    await nextTick()
    active.value = true
  }

  /* 进入视口播放一次（与首页看板一致） */
  watch(hasEntered, (entered) => {
    if (entered) void play()
  })

  /* 数据到达也播放：不依赖视口回调，避免数据已就绪时图形仍为空白 */
  watch(deps, () => {
    void play()
  })

  onMounted(() => {
    void play()
  })

  return { targetRef, progress }
}
