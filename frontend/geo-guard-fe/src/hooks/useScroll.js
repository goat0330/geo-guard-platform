import { nextTick, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue'

const DEFAULT_THRESHOLD = 16
const AUTO_SCROLL_OFFSET = 4
const USER_SCROLL_INTENT_DURATION = 160

const getTargetElement = (target) => {
  const rawTarget = unref(target)

  if (!rawTarget) {
    return null
  }

  if (rawTarget instanceof HTMLElement) {
    return rawTarget
  }

  if (typeof rawTarget !== 'string') {
    return null
  }

  if (rawTarget.startsWith('#') || rawTarget.startsWith('.')) {
    return document.querySelector(rawTarget)
  }

  return document.getElementById(rawTarget) || document.querySelector(`.${rawTarget}`) || document.querySelector(rawTarget)
}

export const useScroll = (target, options = {}) => {
  const threshold = options.threshold ?? DEFAULT_THRESHOLD
  const isScrollBlocked = ref(false)
  let targetEl = null
  let isProgrammaticScrolling = false
  let scrollUnlockTimer = null
  let stopTargetWatch = null
  let userScrollIntentUntil = 0

  const isAtBottom = (el) => {
    if (!el) {
      return false
    }

    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
  }

  const updateScrollState = () => {
    if (!targetEl) {
      return
    }

    if (isProgrammaticScrolling) {
      return
    }

    if (isAtBottom(targetEl)) {
      isScrollBlocked.value = false
      return
    }

    if (performance.now() > userScrollIntentUntil) {
      return
    }

    isScrollBlocked.value = !isAtBottom(targetEl)
  }

  const markUserScrollIntent = () => {
    userScrollIntentUntil = performance.now() + USER_SCROLL_INTENT_DURATION
  }

  const handlePointerDown = (event) => {
    if (!targetEl) {
      return
    }

    const scrollbarWidth = targetEl.offsetWidth - targetEl.clientWidth
    const bounds = targetEl.getBoundingClientRect()
    if (scrollbarWidth > 0 && event.clientX >= bounds.right - scrollbarWidth) {
      markUserScrollIntent()
    }
  }

  const handleKeyDown = (event) => {
    if (['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End'].includes(event.key)) {
      markUserScrollIntent()
    }
  }

  const removeTargetListeners = (element) => {
    if (!element) {
      return
    }

    element.removeEventListener('scroll', updateScrollState)
    element.removeEventListener('wheel', markUserScrollIntent)
    element.removeEventListener('touchmove', markUserScrollIntent)
    element.removeEventListener('pointerdown', handlePointerDown)
    element.removeEventListener('keydown', handleKeyDown)
  }

  const addTargetListeners = (element) => {
    element.addEventListener('scroll', updateScrollState, { passive: true })
    element.addEventListener('wheel', markUserScrollIntent, { passive: true })
    element.addEventListener('touchmove', markUserScrollIntent, { passive: true })
    element.addEventListener('pointerdown', handlePointerDown, { passive: true })
    element.addEventListener('keydown', handleKeyDown)
  }

  const resumeAutoScroll = () => {
    isScrollBlocked.value = false
    userScrollIntentUntil = 0
  }

  const scrollBottom = async(force = false) => {
    await nextTick()

    targetEl = getTargetElement(target)
    if (!targetEl) {
      return false
    }

    if (isScrollBlocked.value && !force) {
      return false
    }

    // 自动跟随保留微小余量，避免子像素取整触发浏览器向上回夹。
    const maxScrollTop = Math.max(0, targetEl.scrollHeight - targetEl.clientHeight)
    const nextScrollTop = force ? maxScrollTop : Math.max(0, maxScrollTop - AUTO_SCROLL_OFFSET)
    if (!force && nextScrollTop <= targetEl.scrollTop) {
      return true
    }

    isProgrammaticScrolling = true
    targetEl.scrollTo({
      top: nextScrollTop,
      behavior: 'auto',
    })

    if (scrollUnlockTimer) {
      cancelAnimationFrame(scrollUnlockTimer)
    }

    scrollUnlockTimer = requestAnimationFrame(() => {
      isScrollBlocked.value = false
      isProgrammaticScrolling = false
      scrollUnlockTimer = null
    })
    return true
  }

  onMounted(() => {
    stopTargetWatch = watch(
      () => getTargetElement(target),
      (nextTarget, previousTarget) => {
        removeTargetListeners(previousTarget)
        targetEl = nextTarget
        if (!targetEl) return

        resumeAutoScroll()
        addTargetListeners(targetEl)
      },
      { immediate: true, flush: 'post' },
    )
  })

  onBeforeUnmount(() => {
    stopTargetWatch?.()
    if (scrollUnlockTimer) {
      cancelAnimationFrame(scrollUnlockTimer)
      scrollUnlockTimer = null
    }

    if (!targetEl) {
      return
    }

    removeTargetListeners(targetEl)
  })

  return {
    isScrollBlocked,
    resumeAutoScroll,
    scrollBottom,
  }
}
