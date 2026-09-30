<template>
  <div class="infinite-scroll-container">
    <!-- 列表内容 -->
    <!-- 置顶 -->
    <div v-if="groupedItems.pinTop.length > 0" class="time-group">
      <div class="group-title">置顶</div>
      <slot name="content" :items="groupedItems.pinTop"></slot>
    </div>

    <!-- 最近7天 -->
    <div v-if="groupedItems.recent7.length > 0" class="time-group">
      <div class="group-title">7天内</div>
      <slot name="content" :items="groupedItems.recent7"></slot>
    </div>

    <!-- 最近30天 -->
    <div v-if="groupedItems.recent30.length > 0" class="time-group">
      <div class="group-title">30天内</div>
      <slot name="content" :items="groupedItems.recent30"></slot>
    </div>

    <!-- 更早 -->
    <div v-if="groupedItems.older.length > 0" class="time-group last-group">
      <div class="group-title">更早</div>
      <slot name="content" :items="groupedItems.older"></slot>
    </div>

    <!-- 加载状态指示器 -->
    <div ref="loader" class="loader">
      <template v-if="loading">
        <div class="loading-spinner">加载中...</div>
      </template>
      <template v-else-if="hasMore">
        <div class="load-more" @click="loadMore">点击加载更多</div>
      </template>
      <template v-else>
        <div class="no-more">没有更多数据了</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick, getCurrentInstance } from 'vue'
import { simpleClone } from '@/utils'

const instance = getCurrentInstance()

// 接收的 props
const props = defineProps({
  // 初始数据
  initialItems: {
    type: Array,
    default: () => [],
  },
  // 加载数据的函数，需要返回 Promise
  loadFunction: {
    type: Function,
    required: true,
  },
  // 是否还有更多数据
  hasMore: {
    type: Boolean,
    default: true,
  },
  // 观察器的根元素
  root: {
    type: HTMLElement,
    default: null,
  },
  // 触发加载的阈值
  threshold: {
    type: Number,
    default: 100,
  },
})

const emits = defineEmits(['load-success', 'load-fail', 'update:items'])

const now = new Date()
const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
const groupedItems = computed(() => {
  const pinTop = []
  const recent7 = []
  const recent30 = []
  const older = []

  items.value.forEach((item) => {
    const itemDate = new Date(item.updateDate).getTime() // 假设字段叫 timestamp
    if (item.pinnedAt) {
      pinTop.push(item)
    } else if (itemDate >= sevenDaysAgo) {
      recent7.push(item)
    } else if (itemDate >= thirtyDaysAgo) {
      recent30.push(item)
    } else {
      older.push(item)
    }
  })

  return {
    pinTop,
    recent7,
    recent30,
    older,
  }
})

// 状态管理
const items = ref([...props.initialItems])
const loading = ref(false)
const loader = ref(null)
let observer = null

// 刷新数据
function refresh() {
  // 清空后自动触发刷新
  clear()
}

// 清空数据
function clear() {
  items.value = []
}
// 加载更多数据
const loadMore = async () => {
  if (loading.value || !props.hasMore) return

  loading.value = true
  try {
    // 调用父组件传入的加载函数
    const newItems = await props.loadFunction()
    emits('load-success')
    if (Array.isArray(newItems)) {
      items.value = [...items.value, ...newItems]
      emits('update:items', items.value)
    }
  } catch (error) {
    emits('load-fail')
  } finally {
    loading.value = false
  }
}

// 置顶以后强制触发已有数据重排
function forceRank(data) {
  let cache = simpleClone(items.value)
  for (let i = 0; i < cache.length; i++) {
    if (cache[i].id === data.id) {
      cache[i].pinnedAt = data.pinnedAt
      break
    }
  }
  items.value = []
  nextTick(() => {
    items.value = cache
  })
}

// 修改所有勾选状态
function forceUpdateCheckStatus(status = true) {
  // 数据引用
  let selected = []
  items.value.forEach((v) => {
    v.isCheck = status
    status && selected.push(v.id)
  })
  return selected
}

// 初始化交叉观察器
const initObserver = () => {
  if (!loader.value) return

  // 创建交叉观察器实例
  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries
      // 当加载指示器可见时，触发加载
      if (entry.isIntersecting && !loading.value && props.hasMore) {
        loadMore()
      }
    },
    {
      root: props.root,
      rootMargin: `${props.threshold}px 0px`,
      threshold: 0,
    },
  )

  // 观察加载指示器元素
  observer.observe(loader.value)
}

// 组件挂载时初始化
onMounted(() => {
  initObserver()
})

// 组件卸载时清理
onUnmounted(() => {
  if (observer && loader.value) {
    observer.unobserve(loader.value)
    observer.disconnect()
    observer = null
  }
})

// 监听hasMore变化，如果有更多数据则重新初始化观察器
watch(
  () => props.hasMore,
  (newVal) => {
    if (newVal && observer) {
      initObserver()
    }
  },
)

// 暴露给父组件的属性和方法
defineExpose({
  loadMore,
  items,
  clear,
  forceRank,
  forceUpdateCheckStatus,
})
</script>

<style scoped>
.infinite-scroll-container {
  width: 100%;
}
.group-title {
  font-size: 12px;
 color: #878898;
  font-weight: 800;
  margin-bottom: 30px;
}

.loader {
  padding: 1rem;
  text-align: center;
  color: #666;
  font-size: 12px;
}

.loading-spinner {
  color: var(--el-color-primary);
}

.load-more {
  color: var(--el-color-primary);

  cursor: pointer;
  transition: color 0.2s;
}

.load-more:hover {
  color: var(--el-color-primary);
}

.no-more {
  color: #999;
}
.time-group {
  margin-bottom: 58px;
}
.last-group {
  margin-bottom: 0;
}
</style>
