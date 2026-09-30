import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 监听元素首次进入可视区，适合驱动一次性的数字、图表和进度动画。
 * @param {Object} [options] IntersectionObserver 配置
 * @param {number} [options.threshold=0.2] 元素可见比例
 * @param {string} [options.rootMargin='0px'] 观察区域边距
 * @returns {{ targetRef: import('vue').Ref<HTMLElement | null>, hasEntered: import('vue').Ref<boolean> }}
 */
export function useEnterView(options = {}) {
  const { threshold = 0.2, rootMargin = '0px' } = options
  const targetRef = ref(null)
  const hasEntered = ref(false)
  let observer = null

  onMounted(() => {
    if (!targetRef.value || typeof IntersectionObserver === 'undefined') {
      hasEntered.value = true
      return
    }

    observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry?.isIntersecting) return

        hasEntered.value = true
        observer?.disconnect()
        observer = null
      },
      { threshold, rootMargin },
    )
    observer.observe(targetRef.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })

  return {
    targetRef,
    hasEntered,
  }
}
