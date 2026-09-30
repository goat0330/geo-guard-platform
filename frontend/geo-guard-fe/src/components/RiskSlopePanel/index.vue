<template>
  <section class="slope-panel">
    <h1>
      <button class="back-button" title="返回风险概览" @click="emit('back')">
        <i class="iconfont icon-arrow-right"></i>
      </button>
      斜坡单元列表
    </h1>

    <div class="filters">
      <div class="filter-head">
        <label class="unit-filter">
          <span>单元编号：</span>
          <el-input v-model="filters.slopeUnitId" placeholder="请输入" clearable @keyup.enter="search" />
        </label>
        <div v-if="!filterExpanded" class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </div>

      <el-collapse-transition>
        <div v-show="filterExpanded" class="filter-more">
          <label>
            <span>风险等级：</span>
            <el-select
              v-model="filters.dynamicRiskLevels"
              multiple
              collapse-tags
              collapse-tags-tooltip
              :suffix-icon="SelectArrowIcon"
              placeholder="请选择风险等级"
              clearable
            >
              <el-option
                v-for="option in DYNAMIC_RISK_LEVEL_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>
          <div class="filter-last-row">
            <label>
              <span>时间选择：</span>
              <el-date-picker
                v-model="filters.assessmentDate"
                class="risk-date-picker"
                type="date"
                value-format="YYYY-MM-DD"
                :prefix-icon="CalendarIcon"
                placeholder="请选择时间"
                :clearable="false"
              />
            </label>
            <div class="filter-actions">
              <el-button type="primary" @click="search">查询</el-button>
              <el-button @click="reset">重置</el-button>
            </div>
          </div>
        </div>
      </el-collapse-transition>

      <div class="filter-divider">
        <span class="divider-line"></span>
        <button
          class="divider-toggle"
          :class="{ 'is-expanded': filterExpanded }"
          type="button"
          @click="filterExpanded = !filterExpanded"
        >
          {{ filterExpanded ? '收起' : '展开' }}<span class="chevron"></span>
        </button>
        <span class="divider-line"></span>
      </div>
    </div>

    <div v-loading="initialLoading" class="slope-list-wrap">
      <RecycleScroller
        v-if="slopeItems.length"
        ref="scrollerRef"
        :key="itemSize"
        class="slope-list"
        :items="slopeItems"
        :item-size="itemSize"
        key-field="virtualId"
        @scroll="handleScroll"
      >
        <template #default="{ item }">
          <div class="slope-list-item">
            <RiskSlopeCard
              :slope="item"
              :active="getRiskSlopeSelectionKey(item) === activeSlopeKey"
              @risk-analysis="emit('risk-analysis', $event)"
              @locate="handleLocate"
            />
          </div>
        </template>
        <template #after>
          <div class="list-status">{{ listStatus }}</div>
        </template>
      </RecycleScroller>
      <el-empty
        v-else-if="!initialLoading"
        :image-size="72"
        :description="listError || '暂无符合条件的斜坡单元'"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, h, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RecycleScroller } from 'vue-virtual-scroller'
import { debounce } from 'lodash-es'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import RiskSlopeCard from './RiskSlopeCard.vue'
import { getRiskAssessmentPage, resolveRiskAssessmentPage } from '@/api/riskEvaluation.js'
import { DYNAMIC_RISK_LEVEL_OPTIONS, formatRiskSlope, getRiskSlopeSelectionKey } from '@/utils/riskEvaluation.js'

defineOptions({ name: 'RiskSlopePanel' })

const emit = defineEmits(['back', 'locate', 'risk-analysis'])
const props = defineProps({
  dataset: {
    type: Object,
    required: true,
  },
})

// Element Plus 图标属性接收 Vue 组件，统一使用项目 iconfont 切图字形。
const SelectArrowIcon = () => h('i', { class: 'iconfont icon-s-arrow' })
const CalendarIcon = () => h('i', { class: 'iconfont icon-a-Calendarrili' })

const scrollerRef = ref(null)
const filterExpanded = ref(false)
const itemSize = ref(102)
const listRows = ref([])
const listLoading = ref(false)
const listError = ref('')
const pageNum = ref(1)
const total = ref(0)
const hasMore = ref(true)
let requestVersion = 0
let listController

const {
  activeSlopeKey,
  filters,
  reset,
  setActiveSlope,
} = props.dataset
const slopeItems = computed(() => listRows.value.map((row) => ({
  ...formatRiskSlope(row),
  virtualId: String(row.id),
})))
const initialLoading = computed(() => listLoading.value && listRows.value.length === 0)
const listStatus = computed(() => {
  if (!slopeItems.value.length) return ''
  if (listLoading.value) return '正在加载更多…'
  if (!hasMore.value) return `没有更多数据了`
  return `已加载 ${slopeItems.value.length} / ${total.value} 个斜坡单元`
})

const loadPage = async ({ resetList = false } = {}) => {
  if ((!resetList && listLoading.value) || (!resetList && !hasMore.value)) return
  const version = resetList ? ++requestVersion : requestVersion
  if (resetList) {
    listController?.abort()
    listController = new AbortController()
    pageNum.value = 1
    listRows.value = []
    total.value = 0
    hasMore.value = true
  }
  if (filters.dynamicRiskLevels.length === 0) {
    listRows.value = []
    total.value = 0
    hasMore.value = false
    listLoading.value = false
    return
  }
  listLoading.value = true
  listError.value = ''
  try {
    const response = await getRiskAssessmentPage({
      ...props.dataset.region.value,
      assessmentDate: filters.assessmentDate,
      dynamicRiskLevels: [...filters.dynamicRiskLevels],
      slopeUnitId: filters.slopeUnitId.trim() || undefined,
      withSlopeUnit: true,
      pageNum: pageNum.value,
      pageSize: 20,
    }, { signal: listController?.signal })
    if (version !== requestVersion) return
    const page = resolveRiskAssessmentPage(response)
    listRows.value.push(...page.rows)
    total.value = page.total
    hasMore.value = listRows.value.length < page.total
    pageNum.value += 1
  } catch {
    if (version === requestVersion) listError.value = '风险数据加载失败，请点击查询重试'
  } finally {
    if (version === requestVersion) listLoading.value = false
  }
}

const search = () => loadPage({ resetList: true })
const handleScroll = (event) => {
  const target = event?.target
  if (target && target.scrollTop + target.clientHeight >= target.scrollHeight - itemSize.value * 2) {
    void loadPage()
  }
}

const updateItemSize = () => {
  const root = parseFloat(getComputedStyle(document.documentElement).fontSize) || 16
  const firstCard = scrollerRef.value?.$el?.querySelector?.('.risk-slope-card')
  const cardHeight = firstCard?.getBoundingClientRect?.()?.height
  // 保持卡片原本的响应式大小，卡片真实高度 + 固定 10px 间距
  const actualCardHeight = (cardHeight && cardHeight > 0) ? cardHeight : ((92 / 16) * root)
  itemSize.value = Math.round(actualCardHeight + 10)
}

let resizeObserver = null

onMounted(() => {
  updateItemSize()
  window.addEventListener('resize', updateItemSize)
  nextTick(() => {
    updateItemSize()
    if (scrollerRef.value?.$el) {
      resizeObserver = new ResizeObserver(updateItemSize)
      resizeObserver.observe(scrollerRef.value.$el)
    }
  })
})

watch(
  () => slopeItems.value.length,
  () => {
    nextTick(updateItemSize)
  },
)

watch([
  () => JSON.stringify(props.dataset.region.value),
  () => filters.assessmentDate,
], () => { void search() }, { immediate: true })

// 与地图全量数据保持一致，连续勾选风险等级后只发起一次分页请求。
const debouncedRiskSearch = debounce(() => { void search() }, 1500)
watch(
  () => [...filters.dynamicRiskLevels].sort((a, b) => a - b).join(','),
  debouncedRiskSearch,
)

onUnmounted(() => {
  debouncedRiskSearch.cancel()
  window.removeEventListener('resize', updateItemSize)
  resizeObserver?.disconnect()
  listController?.abort()
})

const handleLocate = (slope) => {
  // 定位交由父组件处理，父组件确认具备弹窗条件后再高亮
  emit('locate', slope)
}
</script>

<style lang="less" scoped>
.slope-panel {
  height: 100%;
  min-height: 0;
  padding: 24px 0 20px 24px;
  box-sizing: border-box;
  color: #1a1a1a;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

h1 {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin: 0 24px 24px 0;
  font-size: 18px;
  line-height: 26px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1a1a1a;
  cursor: pointer;
}

.back-button i {
  font-size: 16px;
  transform: rotate(180deg);
}

.filters {
  flex-shrink: 0;
  margin: 0 24px 0 0;
}

.filters label {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667382;
  font-size: 14px;
  white-space: nowrap;
}

.filters label > span {
  flex: 0 0 70px;
}

.filters :deep(.el-input),
.filters :deep(.el-select),
.filters :deep(.el-date-editor) {
  width: 100%;
}

.filter-head,
.filter-last-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-head .unit-filter,
.filter-last-row > label {
  flex: 1 1 0;
}

.filter-more {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dfe5ed inset;
}

.filters :deep(.el-select__suffix),
.filters :deep(.risk-date-picker .el-input__prefix) {
  flex: 0 0 20px;
  width: 20px;
  justify-content: center;
}

.filters :deep(.el-select__caret),
.filters :deep(.risk-date-picker .el-input__prefix-inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  color: #222527;
  font-size: 16px;
}

.filters :deep(.risk-date-picker .el-input__prefix) {
  order: 2;
  margin-right: 0;
  margin-left: 6px;
}

.filters :deep(.risk-date-picker .el-input__inner) {
  order: 1;
  text-align: left;
}

.filters :deep(.risk-date-picker .el-input__suffix) {
  order: 3;
}

.filter-actions {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
}

.filter-actions .el-button {
  width: 48px;
  height: 34px;
  margin: 0;
  padding: 0;
  border-radius: 8px;
}

.filter-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 18px;
}

.divider-line {
  flex: 1 1 auto;
  height: 1px;
  background: #e8eef5;
}

.divider-toggle {
  flex-shrink: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: #9096a2;
  font-family: inherit;
  font-size: 12px;
  line-height: 18px;
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: #007bff;
  }

  .chevron {
    width: 14px;
    height: 14px;
    display: block;
    background: url('@/assets/imgs/emergency/icon-chevron-down.png') no-repeat center / contain;
    transition: transform 0.2s ease;
  }

  &.is-expanded .chevron {
    transform: rotate(180deg);
  }
}

.slope-list-wrap {
  flex: 1 1 0;
  min-height: 0;
  padding-right: 20px;
}

.slope-list {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overscroll-behavior: contain;
}

.slope-list-item {
  box-sizing: border-box;
  padding-bottom: 10PX;
}

.list-status {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9096A2;
  font-size: 12px;
}

.slope-list::-webkit-scrollbar {
  width: 4px;
}

.slope-list::-webkit-scrollbar-thumb {
  border-radius: 2px;
  background: #d6dee8;
}

.slope-list-wrap :deep(.el-empty) {
  flex: 1 1 auto;
  padding: 0;
}

@media (max-width: 1366px) {
  .slope-panel {
    padding: 20px 0 16px 16px;
  }
  h1 {
    margin: 0 16px 18px 0;
  }
  .filters {
    margin: 0 16px 16px 0;
  }
  .slope-list-wrap {
    padding-right: 12px;
  }
}
</style>
