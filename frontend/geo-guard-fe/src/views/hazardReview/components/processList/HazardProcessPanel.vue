<template>
  <section class="process-panel">
    <header class="panel-header">
      <button class="back-button" type="button" title="返回复核概况" @click="emit('back')">
        <img class="back-icon" :src="backIcon" alt="" />
      </button>
      <h1>隐患处理列表</h1>
    </header>

    <HazardProcessFilters @change="handleFilterChange" />

    <!-- 滚动到底部自动加载下一页（接口分页，与其它页面列表一致） -->
    <div ref="listRef" class="process-list" @scroll="handleScroll">
      <HazardProcessCard
        v-for="item in hazardReviewData.hazardList"
        :key="item.id"
        :data="item"
        :active="activeId === item.id"
        @process="emit('process', $event)"
        @select="handleSelect"
      />
      <p v-if="!loading && !hazardReviewData.hazardList.length" class="empty-tip">暂无匹配的隐患点</p>
      <p v-else-if="loading" class="list-tip">加载中…</p>
      <p v-else-if="finished" class="list-tip">已加载全部隐患点</p>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import HazardProcessCard from './HazardProcessCard.vue'
import HazardProcessFilters from './HazardProcessFilters.vue'
import { HAZARD_PAGE_SIZE, fetchHazardList, hazardReviewData } from '../../useHazardReviewData.js'
import backIcon from '@/assets/imgs/hazardReview/icon-left-arrow.webp'

const emit = defineEmits(['back', 'process', 'select'])

const listRef = ref(null)
/** 列表查询条件：与 HazardProcessFilters 的输出结构一致，均为接口参数 */
const filters = ref({ keyword: '', typeCode: '', handleStatus: '', dateRange: [] })
/** 当前选中的隐患点，用于卡片高亮（点击卡片会在地图上展示其详情） */
const activeId = ref('')
/** 正在请求：避免触底时重复发起 */
const loading = ref(false)
/** 已无更多数据（本次返回不足一页）：停止触底请求并给出提示 */
const finished = ref(false)
/** 当前页码：首屏为 1，触底加载后自增 */
const pageNum = ref(1)

/** 点击卡片：标记选中并向外抛出，由页面在地图上显示详情弹窗 */
const handleSelect = (item) => {
  activeId.value = item.id
  emit('select', item)
}

/**
 * 补足首屏：内容未撑满容器时没有滚动条，触底事件永远不会触发，
 * 因此一页加载完后若仍有可滚动空间且还有数据，就继续请求下一页。
 * 容器不可见（高度为 0）时不做判断，避免空内容下无限追加
 */
const fillViewport = async () => {
  await nextTick()
  const element = listRef.value
  if (!element || loading.value || finished.value) {
    return
  }
  if (element.clientHeight > 0 && element.scrollHeight <= element.clientHeight + 1) {
    void loadMore()
  }
}

/** 加载第一页：筛选条件变化时重置列表 */
const loadFirstPage = async () => {
  pageNum.value = 1
  finished.value = false
  loading.value = true
  try {
    const count = await fetchHazardList({
      filters: filters.value,
      pageNum: 1,
      pageSize: HAZARD_PAGE_SIZE,
    })
    finished.value = count < HAZARD_PAGE_SIZE
  } catch {
    /* 接口异常已由 http 层统一提示，列表保持原状 */
  } finally {
    loading.value = false
  }
  void fillViewport()
}

/** 触底加载下一页：接口按页码请求，结果追加到列表尾部 */
const loadMore = async () => {
  if (loading.value || finished.value) {
    return
  }
  loading.value = true
  try {
    const count = await fetchHazardList({
      filters: filters.value,
      pageNum: pageNum.value + 1,
      pageSize: HAZARD_PAGE_SIZE,
    })
    pageNum.value += 1
    finished.value = count < HAZARD_PAGE_SIZE
  } catch {
    /* 接口异常已由 http 层统一提示，保留已加载数据 */
  } finally {
    loading.value = false
  }
  void fillViewport()
}

/** 触底加载下一页：距底部 40px 内触发，请求中或已加载完不再发起 */
const handleScroll = () => {
  const element = listRef.value
  if (!element || loading.value || finished.value) {
    return
  }
  if (element.scrollTop + element.clientHeight < element.scrollHeight - 40) {
    return
  }
  void loadMore()
}

const handleFilterChange = (value) => {
  filters.value = { ...filters.value, ...value }
  /* 筛选条件变化后回到第一页并滚回顶部，避免停留在越界位置出现空列表 */
  if (listRef.value) {
    listRef.value.scrollTop = 0
  }
  void loadFirstPage()
}

/* 进入列表即加载第一页 */
onMounted(() => {
  void loadFirstPage()
})

onBeforeUnmount(() => {
  /* 列表放在共享数据源里，离开时清空，避免下次进入先闪出上一次的结果 */
  hazardReviewData.hazardList = []
  hazardReviewData.hazardTotal = 0
})

/* 供页面在确认 / 反馈成功后刷新列表 */
defineExpose({ refresh: loadFirstPage })
</script>

<style lang="less" scoped>
.process-panel {
  height: 100%;
  min-height: 0;
  padding: 22px 0 18px 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #1a1a1a;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0 24px 16px 0;

  h1{
    font-size: 18px;
    font-weight: 800;
  }
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #222527;
  cursor: pointer;

  .back-icon {
    display: block;
    width: 18px;
    height: 18px;
    object-fit: contain;
  }
}

h1 {
  margin: 0;
  font-size: 18px;
  line-height: 26px;
  font-weight: 600;
  white-space: nowrap;
}

.process-list {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 22px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
}

.process-list::-webkit-scrollbar {
  width: 4px;
}

.process-list::-webkit-scrollbar-thumb {
  border-radius: 2px;
  background: #d6dee8;
}

.empty-tip {
  margin: 40px 0 0;
  color: #a6b0bd;
  font-size: 14px;
  text-align: center;
}

/* 触底加载 / 全部加载完的提示行（与其它页面列表一致） */
.list-tip {
  margin: 12px 0;
  color: #a6b0bd;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
</style>
