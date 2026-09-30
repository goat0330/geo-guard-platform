<template>
  <section class="alarm-panel">
    <header class="panel-header">
      <button class="back-button" type="button" title="返回专业监测概况" @click="emit('back')">
        <img class="back-icon" :src="backIcon" alt="" />
      </button>
      <h1>专业监测告警列表</h1>
      <button class="export-button" type="button" @click="handleExport">
        <i class="iconfont icon-export"></i>导出列表
      </button>
    </header>

    <AlarmFilterBar @change="handleFilterChange" />

    <!-- 滚动到底部自动加载下一页（每页 20 条） -->
    <div ref="listRef" v-loading="loading && pageNum === 1" class="alarm-list" @scroll="handleScroll">
      <AlarmWarningItem
        v-for="item in filteredList"
        :key="item.id"
        :data="item"
        :expanded="expandedIds.includes(item.id)"
        :active="activeId === item.id"
        @toggle="handleToggle"
        @analyze="emit('analyze', $event)"
      />
      <p v-if="!filteredList.length && !loading" class="empty-tip">暂无匹配的告警记录</p>
      <p v-else-if="loading" class="list-tip">加载中…</p>
      <p v-else-if="finished" class="list-tip">已加载全部告警</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AlarmFilterBar from './AlarmFilterBar.vue'
import AlarmWarningItem from './AlarmWarningItem.vue'
import { ALARM_PAGE_SIZE, fetchAlarmList, monitorData } from '../../useMonitorData.js'
import backIcon from '@/assets/imgs/hazardReview/icon-left-arrow.webp'

defineOptions({ name: 'AlarmListPanel' })

const emit = defineEmits(['back', 'export', 'analyze', 'select'])

/**
 * 列表查询条件：区划 / 等级 / 有效性 / 时间区间由筛选栏提供；
 * state 为接口的告警数据状态，缺省为空即不下发（后端按其默认口径返回）；
 * dateRange 为空数组时取数回落到「当月至今」
 */
const filters = ref({ region: '', level: '', validity: '', dateRange: [], state: '' })

/** 默认展开第一条，便于查看详情结构 */
const expandedIds = ref([])
const loading = ref(false)

const listRef = ref(null)
/** 当前页码：首屏为 1，触底加载后自增 */
const pageNum = ref(1)
/** 已无更多数据（接口返回不足一页）：用于停止触底请求并给出提示 */
const finished = ref(false)

/** 加载第一页：筛选条件变化时重置列表 */
const loadFirstPage = async () => {
  pageNum.value = 1
  finished.value = false
  loading.value = true
  try {
    const count = await fetchAlarmList({
      range: filters.value.dateRange,
      state: filters.value.state,
      pageNum: 1,
      pageSize: ALARM_PAGE_SIZE,
    })
    finished.value = count < ALARM_PAGE_SIZE
  } finally {
    loading.value = false
  }
}

/** 触底加载下一页：距底部 40px 内触发，无数据或正在请求时不再发起 */
const handleScroll = async () => {
  const element = listRef.value
  if (!element || loading.value || finished.value) {
    return
  }
  if (element.scrollTop + element.clientHeight < element.scrollHeight - 40) {
    return
  }
  loading.value = true
  try {
    const count = await fetchAlarmList({
      range: filters.value.dateRange,
      state: filters.value.state,
      pageNum: pageNum.value + 1,
      pageSize: ALARM_PAGE_SIZE,
    })
    pageNum.value += 1
    finished.value = count < ALARM_PAGE_SIZE
  } finally {
    loading.value = false
  }
}

const handleFilterChange = (value) => {
  /* 合并而非整体替换：筛选栏暂未提供的条件（如 state）不会被清掉 */
  filters.value = { ...filters.value, ...value }
  loadFirstPage()
}

/** 导出列表：把当前筛选条件一并抛出，由外层按同一口径请求导出接口 */
const handleExport = () => {
  emit('export', { ...filters.value })
}

// 进入列表页即加载第一页
onMounted(() => {
  loadFirstPage()
})

/** 列表首次加载后默认展开第一条 */
watch(
  () => monitorData.alarmList,
  (list) => {
    if (list.length && !expandedIds.value.length) {
      expandedIds.value = [list[0].id]
    }
  },
)

const toggleItem = (id) => {
  expandedIds.value = expandedIds.value.includes(id)
    ? expandedIds.value.filter((item) => item !== id)
    : [...expandedIds.value, id]
}

/** 当前选中项：点击卡片即选中，同时切换展开态 */
const activeId = ref('')

const handleToggle = (id) => {
  activeId.value = id
  toggleItem(id)
  /* 选中列表项后通知外层显示地图弹窗；弹窗关闭后再点同一项仍会重新显示 */
  const target = filteredList.value.find((item) => item.id === id)
  if (target) {
    emit('select', target)
  }
}

/** 列表筛选：等级 / 有效预警按文案匹配，行政区划按地址模糊匹配（预警列表接口无筛选参数） */
const filteredList = computed(() => monitorData.alarmList.filter((item) => {
  const { level, region, validity } = filters.value
  const matchLevel = !level || item.level === level
  const matchRegion = !region || String(item.detail?.address ?? '').includes(region)
  const matchValidity = !validity || item.detail?.validWarning === validity
  return matchLevel && matchRegion && matchValidity
}))
</script>

<style lang="less" scoped>
.alarm-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: 22px 0 18px 24px;
  box-sizing: border-box;
  color: #1a1a1a;
  background: linear-gradient(180deg, #f0f2ff 0%, #ffffff 96px);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  margin: 0 24px 16px 0;
}

.panel-header h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  line-height: 26px;
  white-space: nowrap;
}

.back-button {
  display: inline-flex;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;

  .back-icon {
    display: block;
    width: 18px;
    height: 18px;
    object-fit: contain;
  }
}

.alarm-list {
  flex: 1 1 0;
  min-height: 0;
  gap: 12px;
  padding-right: 24px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

/* 导出列表：标题行右侧主按钮 */
.export-button {
  display: inline-flex;
  height: 34px;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: #007bff;
  color: #ffffff;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s ease;

  i {
    font-size: 14px;
  }

  &:hover {
    background: #3395ff;
  }
}

.alarm-list::-webkit-scrollbar {
  width: 4px;
}

.alarm-list::-webkit-scrollbar-thumb {
  border-radius: 2px;
  background: #d6dee8;
}

.empty-tip {
  margin: 40px 0 0;
  color: #a6b0bd;
  font-size: 14px;
  text-align: center;
}

/* 触底加载 / 全部加载完的提示行 */
.list-tip {
  margin: 12px 0;
  color: #a6b0bd;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
</style>
