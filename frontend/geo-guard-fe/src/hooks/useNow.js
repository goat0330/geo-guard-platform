import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'

/**
 * 响应式当前时间 Hook
 * @param {string} format 时间格式化模版，默认 'YYYY-MM-DD HH:mm:ss'
 */
export const useNow = (format = 'YYYY-MM-DD HH:mm:ss') => {
  const now = ref(dayjs())
  let timer = null

  onMounted(() => {
    timer = setInterval(() => {
      now.value = dayjs()
    }, 1000)
  })

  onBeforeUnmount(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  })

  const currentTime = computed(() => now.value.format(format))

  return {
    currentTime,
  }
}

export default useNow
