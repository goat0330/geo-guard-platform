import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'

export const useLayoutStore = defineStore('layout', () => {
  const leftContentClass = ref('')
  const leftContentStyle = ref({})
  const useCesium = ref(true)
  const layoutResizeLockKeys = shallowRef(new Set())
  const isLayoutResizeLocked = computed(() => layoutResizeLockKeys.value.size > 0)

  function lockLayoutResize(key) {
    if (!key) return
    const nextKeys = new Set(layoutResizeLockKeys.value)
    nextKeys.add(key)
    layoutResizeLockKeys.value = nextKeys
  }

  function unlockLayoutResize(key) {
    if (!key) return
    const nextKeys = new Set(layoutResizeLockKeys.value)
    nextKeys.delete(key)
    layoutResizeLockKeys.value = nextKeys
  }

  function clearLayoutResizeLocks(filter) {
    if (typeof filter !== 'function') {
      layoutResizeLockKeys.value = new Set()
      return
    }
    const nextKeys = new Set(layoutResizeLockKeys.value)
    nextKeys.forEach((key) => {
      if (filter(key)) {
        nextKeys.delete(key)
      }
    })
    layoutResizeLockKeys.value = nextKeys
  }

  return {
    leftContentClass,
    leftContentStyle,
    useCesium,
    isLayoutResizeLocked,
    lockLayoutResize,
    unlockLayoutResize,
    clearLayoutResizeLocks
  }
})
