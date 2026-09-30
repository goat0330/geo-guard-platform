import { debounce } from 'lodash-es'

const DEFAULT_WAIT = 1000
const STORE_KEY = '__debounceClick__'

const getWait = (...values) => {
  const wait = values.find((value) => Number(value) > 0)

  return wait ? Number(wait) : DEFAULT_WAIT
}

const normalizeOptions = (binding) => {
  const value = binding.value
  const modifiers = binding.modifiers || {}
  const listenerOptions = {
    capture: !!modifiers.capture,
    once: !!modifiers.once,
    passive: !!modifiers.passive,
  }

  if (typeof value === 'function') {
    return {
      args: [],
      handler: value,
      listenerOptions,
      modifiers,
      wait: getWait(binding.arg),
      options: {
        leading: true,
        trailing: false,
      },
    }
  }

  if (value && typeof value === 'object') {
    const isArrayValue = Array.isArray(value)

    return {
      args: isArrayValue ? value.slice(1) : value.args || [],
      handler: isArrayValue ? value[0] : value.handler,
      listenerOptions,
      modifiers,
      wait: getWait(value.wait, value.delay, binding.arg),
      options: {
        leading: value.leading !== false,
        trailing: value.trailing === true,
      },
    }
  }

  return {
    args: [],
    handler: null,
    listenerOptions,
    modifiers,
    wait: DEFAULT_WAIT,
    options: {
      leading: true,
      trailing: false,
    },
  }
}

const clearDebounceClick = (el) => {
  const cache = el[STORE_KEY]

  if (!cache) return

  el.removeEventListener('click', cache.listener, cache.listenerOptions)
  cache.debounced.cancel()
  delete el[STORE_KEY]
}

/**
 * 用法示例：
 * v-d-click="submit"
 * v-d-click:1500="submit"
 * v-d-click:1200.stop="submit"
 * v-d-click.stop="{ handler: handleSubmit, args: [row] }"
 * v-d-click.stop="[handleSubmit, row]"
 *
 * 不要写成 v-d-click="handleSubmit(row)"。
 * Vue 会在指令拿到 binding.value 之前先执行 handleSubmit(row)，导致页面渲染时就触发。
 */
const bindDebounceClick = (el, binding) => {
  const { args, handler, listenerOptions, modifiers, wait, options } = normalizeOptions(binding)
  const cache = el[STORE_KEY]
  const optionsKey = JSON.stringify({
    debounce: options,
    listener: listenerOptions,
    modifiers,
  })

  if (typeof handler !== 'function') {
    clearDebounceClick(el)
    return
  }

  if (cache && cache.wait === wait && cache.optionsKey === optionsKey) {
    cache.args = args
    cache.handler = handler
    cache.instance = binding.instance
    return
  }

  clearDebounceClick(el)

  const debounced = debounce(
    (event) => {
      const cache = el[STORE_KEY]

      cache?.handler?.call(cache.instance, ...cache.args, event)
    },
    wait,
    options,
  )
  const listener = (event) => {
    if (modifiers.self && event.target !== el) return
    if (modifiers.stop) event.stopPropagation()
    if (modifiers.prevent) event.preventDefault()

    debounced(event)
  }

  el.addEventListener('click', listener, listenerOptions)
  el[STORE_KEY] = {
    args,
    debounced,
    handler,
    instance: binding.instance,
    listener,
    listenerOptions,
    optionsKey,
    wait,
  }
}

const debounceClick = {
  mounted: bindDebounceClick,
  updated: bindDebounceClick,
  unmounted: clearDebounceClick,
}

export default debounceClick
export { debounceClick }
