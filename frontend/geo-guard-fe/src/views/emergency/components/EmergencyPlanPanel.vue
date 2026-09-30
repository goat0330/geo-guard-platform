<template>
  <section class="plan-panel">
    <header class="panel-header">
      <h1>智能预案</h1>
    </header>

    <!-- 页签：已有预案更新 / 突发灾情预案生成 -->
    <div class="plan-tabs">
      <button v-for="tab in planTabs" :key="tab.key" class="plan-tab" :class="{ 'is-active': activeTab === tab.key }"
        type="button" @click="handleTabChange(tab.key)">
        {{ tab.label }}
      </button>
    </div>

    <!-- key 绑定页签：切换页签时整体重载筛选区，避免残留上一页签的条件 -->
    <EmergencyPlanFilters
      :key="activeTab"
      :keyword-label="activeTabMeta.keywordLabel"
      :status-options="activeTabMeta.statusOptions"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 首屏与筛选刷新（回到第一页）时整列进入加载态，避免卡片直接冒出来显得突兀 -->
    <div
      v-loading="initialLoading"
      class="plan-list"
      element-loading-text="预案列表加载中..."
      element-loading-custom-class="plan-list-loading"
      element-loading-background="rgba(255, 255, 255, 0.72)"
      @scroll.passive="handleScroll"
    >
      <EmergencyPlanCard v-for="item in list" :key="item.id" :data="item" :active="activeId === item.id"
        @select="handleSelect" @action="handleAction" />

      <!-- 滚动追加下一页：列表已有内容，只在底部提示 -->
      <p v-if="loading && pageNum > 1 && list.length" class="list-tip">加载中...</p>
      <p v-else-if="!hasMore && list.length" class="list-tip">没有更多了</p>
      <p v-if="!loading && !list.length" class="empty-tip">暂无匹配的预案</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import EmergencyPlanFilters from './EmergencyPlanFilters.vue'
import EmergencyPlanCard from './EmergencyPlanCard.vue'
import {
  buildDateRange,
  fetchEmergencyPlanList,
  fetchHazardPlanList,
  resolvePlanPage,
} from '@/api/intelligentPlan.js'
import {
  emergencyStatusMap,
  formatAdminArea,
  hazardAiStatusMap,
  hazardIconMap,
  planFilterOptions,
  planTabs,
} from '../config.js'
import iconHazardOrange from '@/assets/imgs/emergency/icon-hazard-orange.png'

defineOptions({ name: 'EmergencyPlanPanel' })

const emit = defineEmits(['select', 'action', 'tab-change'])

/** 列表分页大小：与左侧列表可视高度匹配，滚动到底部时自动追加下一页 */
const PAGE_SIZE = 10

const activeTab = ref(planTabs[0].key)
/** 当前选中的预案，用于卡片高亮（点击卡片会在地图上展示对应点位详情） */
const activeId = ref('')

/** 当前页签配置：关键字标签名、状态选项随页签切换 */
const activeTabMeta = computed(() => {
  const tab = planTabs.find((item) => item.key === activeTab.value) ?? planTabs[0]
  return {
    ...tab,
    statusOptions:
      tab.key === planTabs[0].key
        ? planFilterOptions.hazardStatuses
        : planFilterOptions.emergencyStatuses,
  }
})

const isHazardTab = computed(() => activeTab.value === planTabs[0].key)

/** 筛选条件：由筛选区下发，按页签转成对应请求参数 */
const filters = ref({ keyword: '', date: '', status: '' })

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const hasMore = computed(() => list.value.length < total.value)

/** 首屏 / 筛选刷新（回到第一页）的整列加载态；滚动追加只在底部提示，不遮住已有卡片 */
const initialLoading = computed(() => loading.value && pageNum.value === 1)

/** 突发灾险情按 status 决定卡片操作：0 待勾划范围、1 更新中、2 已更新、3 更新失败（重新勾划范围生成） */
const emergencyActionsMap = {
  0: ['range'],
  1: ['analysis'],
  2: ['result', 'analysis'],
  3: ['range'],
}

/** 已有隐患点按 aiProcessStatus 决定卡片操作：0 待更新、1 更新中、2 已更新、3 更新失败 */
const hazardActionsMap = {
  0: ['analysis'],
  1: ['analysis'],
  2: ['result', 'analysis'],
  3: ['analysis'],
}

/** 已有隐患点列表行 → 卡片数据。sourceId 全程保持字符串 */
const mapHazardRow = (row) => {
  /* 列表同时返回 typeCode（编码）与 typeName（中文名），展示与图标匹配统一用中文名 */
  const hazardType = row.typeName || row.typeCode
  const aiStatus = String(row.aiProcessStatus ?? '')
  return {
    id: row.id,
    sourceId: String(row.id),
    name: row.name,
    /* 卡片状态优先展示 AI 更新状态，缺失时回落到处理状态 */
    status: row.aiProcessStatusName || hazardAiStatusMap[aiStatus] || row.handleStatusName || '',
    aiProcessStatus: aiStatus,
    location: row.location,
    hazardType,
    hazardIcon: hazardIconMap[hazardType] || iconHazardOrange,
    /* 卡片展示更新时间：优先 updatedTime，未返回时回落到 createdTime（展示名仍为更新时间） */
    updateTime: row.updatedTime || row.createdTime,
    longitude: row.longitude,
    latitude: row.latitude,
    actions: hazardActionsMap[aiStatus] || ['analysis'],
    raw: row,
  }
}

/** 突发灾险情列表行 → 卡片数据 */
const mapEmergencyRow = (row) => ({
  id: String(row.id),
  sourceId: String(row.id),
  name: row.eventName,
  status: row.statusName || emergencyStatusMap[Number(row.status)] || '',
  statusCode: row.status,
  /* 列表已不再返回 administrativeArea，改按省、市、区县、乡镇、村拼接 */
  location: formatAdminArea(row),
  occurTime: row.occurTime,
  /* 列表已返回 WGS84 中心点经纬度，缺失时保持空值：选中时会提示无法定位 */
  longitude: row.longitude,
  latitude: row.latitude,
  hazardIcon: iconHazardOrange,
  actions: emergencyActionsMap[row.status] ?? ['analysis'],
  raw: row,
})

/** 组装请求参数：两个页签的状态字段名不同（handleStatus / status），时间统一转成起止区间 */
const buildParams = () => {
  const base = {
    pageNum: pageNum.value,
    pageSize: PAGE_SIZE,
    keyword: filters.value.keyword || undefined,
    ...buildDateRange(filters.value.date),
  }
  if (isHazardTab.value) {
    return { ...base, handleStatus: filters.value.status || undefined }
  }
  /* 突发灾险情的 status 为数值枚举，未选择（空串 / clear 后的 null、undefined）时不传 */
  const rawStatus = filters.value.status
  const hasStatus = rawStatus !== '' && rawStatus !== null && rawStatus !== undefined
  return { ...base, status: hasStatus ? Number(rawStatus) : undefined }
}

/** 加载列表：append 为 true 时追加到当前列表（滚动分页） */
const loadList = async (append = false) => {
  if (loading.value) {
    return
  }
  loading.value = true
  const request = isHazardTab.value ? fetchHazardPlanList : fetchEmergencyPlanList
  const mapRow = isHazardTab.value ? mapHazardRow : mapEmergencyRow
  try {
    const res = await request(buildParams())
    const page = resolvePlanPage(res)
    const rows = page.rows.map(mapRow)
    list.value = append ? [...list.value, ...rows] : rows
    total.value = page.total
  } catch (error) {
    console.error('[智能预案] 列表加载失败:', error)
    if (!append) {
      list.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

/** 重新查询：回到第一页 */
const refreshList = () => {
  pageNum.value = 1
  loadList()
}

/** 滚动到底部（距底 < 40px）加载下一页，200ms 去抖避免一次滚动多次触发 */
let scrollTimer = null
const handleScroll = (event) => {
  if (!hasMore.value || loading.value || scrollTimer) {
    return
  }
  scrollTimer = setTimeout(() => {
    scrollTimer = null
  }, 200)
  const el = event.target
  if (el.scrollHeight - el.scrollTop - el.clientHeight > 40) {
    return
  }
  pageNum.value += 1
  loadList(true)
}

const handleTabChange = (key) => {
  if (activeTab.value === key) {
    return
  }
  activeTab.value = key
  activeId.value = ''
  /* 筛选区因 :key 重建已重置 UI，父级持有的条件需同步清空，避免旧页签的关键字/状态混入新页签的请求 */
  filters.value = { keyword: '', date: '', status: '' }
  /* 页签切换后列表整体换源：通知外部清掉地图标记与点位弹窗，并自动重新查询，无需手动点查询 */
  emit('tab-change', key)
  refreshList()
}

const handleSearch = (payload) => {
  filters.value = { ...payload }
  activeId.value = ''
  refreshList()
}

const handleReset = () => {
  activeId.value = ''
}

/** 点击卡片：标记选中并向外抛出（带页签与场景，供地图点位与路线查询使用） */
const handleSelect = (item) => {
  activeId.value = item.id
  emit('select', {
    ...item,
    tab: activeTab.value,
    scene: activeTabMeta.value.scene,
  })
}

/** 点击卡片操作按钮：按钮阻止了冒泡，这里先补上选中态（高亮）再抛出动作，
 *  保证直接点「去勾划范围 / 查看分析过程 / 查看更新结果」时父级就能拿到这条预案的数据 */
const handleAction = ({ item, action }) => {
  activeId.value = item.id
  emit('action', {
    item: { ...item, tab: activeTab.value, scene: activeTabMeta.value.scene },
    action,
  })
}

/* 供父级在生成完成 / 生成失败后刷新列表，让卡片状态与后端保持一致 */
defineExpose({ refreshList })

onMounted(refreshList)

onBeforeUnmount(() => {
  clearTimeout(scrollTimer)
})
</script>

<style lang="less" scoped>
.plan-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: 24px 0 18px 24px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #f0f2ff 0%, #ffffff 96px);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header h1 {
  margin: 0 24px 0 0;
  color: #222527;
  font-size: 18px;
  font-weight: 800;
  line-height: 26px;
}

/* 页签：激活项为浅蓝底 + 主色描边与文字 */
.plan-tabs {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  margin: 14px 24px 0 0;
}

.plan-tab {
  height: 36px;
  padding: 0 20px;
  border: 1px solid #007BFF;
  border-radius: 100px;
  background: #007bff1a;
  color: #007BFF;
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  margin-bottom: 8px;

  &:hover {
    color: #FFFFFF;
    background: #007bffcc;
  }

  &.is-active {
    border-color: #007BFF;
    background: #007BFF;
    color: #FFFFFF;
  }
}

.plan-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 与筛选区的间距由分割线的下边距（24px）提供 */
  margin: 0;
  padding: 0 22px 0 0;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 2px;
    background: rgba(97, 113, 133, 0.3);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

/* 列表加载态：指示器与文案统一用主色，与面板其余加载反馈保持一致 */
:deep(.plan-list-loading .el-loading-spinner .path) {
  stroke: #007bff;
}

:deep(.plan-list-loading .el-loading-text) {
  color: #007bff;
  font-size: 14px;
}

.list-tip {
  flex-shrink: 0;
  margin: 4px 0 8px;
  color: #9096A2;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}

.empty-tip {
  margin: 40px 0 0;
  color: #a6b0bd;
  font-size: 14px;
  text-align: center;
}
</style>
