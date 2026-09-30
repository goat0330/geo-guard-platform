<template>
  <div class="process-filters">
    <!-- 关键字：收起态即展示；展开后按钮移到展开区末行（折叠高度动画由 el-collapse-transition 提供） -->
    <div class="search-row">
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="隐患点名称关键字"
        clearable
        @keyup.enter="notify"
      >
        <template #prefix>
          <i class="iconfont icon-search"></i>
        </template>
      </el-input>
      <template v-if="!expanded">
        <button class="query-button" type="button" @click="notify">查询</button>
        <button class="reset-button" type="button" @click="handleReset">重置</button>
      </template>
    </div>

    <el-collapse-transition>
      <div v-show="expanded" class="filter-more">
        <!-- 灾害类型 / 处理状态 -->
        <div class="filter-row">
          <label class="filter-cell">
            <span>灾害类型：</span>
            <!-- 选项与编码取自总览统计的隐患类型分布，选中的 value 直接作为接口 typeCode 下发 -->
            <el-select v-model="typeCode" placeholder="请选择" clearable>
              <el-option
                v-for="item in typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
          <label class="filter-cell">
            <span>处理状态：</span>
            <!-- 处理状态用接口枚举（PENDING_REVIEW 等），不再按名称匹配 -->
            <el-select v-model="handleStatus" placeholder="请选择" clearable>
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
        </div>

        <!-- 时间范围 / 查询 / 重置：按钮常驻展开区末行末尾 -->
        <div class="time-row">
          <label class="time-cell">
            <span>时间选择：</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              range-separator="至"
              unlink-panels
            />
          </label>
          <button class="query-button" type="button" @click="notify">查询</button>
          <button class="reset-button" type="button" @click="handleReset">重置</button>
        </div>
      </div>
    </el-collapse-transition>

    <!-- 分割线：距上方 12px、下方 24px，中间为「展开」入口 -->
    <div class="filter-divider">
      <span class="divider-line"></span>
      <button
        class="divider-toggle"
        :class="{ 'is-expanded': expanded }"
        type="button"
        @click="toggleExpanded"
      >
        {{ expanded ? '收起' : '展开' }}<span class="chevron"></span>
      </button>
      <span class="divider-line"></span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { HANDLE_STATUS_OPTIONS, hazardReviewData } from '../../useHazardReviewData.js'

const emit = defineEmits(['change'])

const keyword = ref('')
/** 灾害类型编码：接口 typeCode */
const typeCode = ref('')
/** 处理状态：接口 handleStatus 枚举 */
const handleStatus = ref('')
/** 创建时间范围：接口 beginDate / endDate，空数组表示不限 */
const dateRange = ref([])
/** 展开态：收起时仅展示关键字，展开后追加「灾害类型 / 处理状态 / 时间选择」 */
const expanded = ref(false)

/** 类型选项：与总览统计的隐患类型同源，value 即接口编码 */
const typeOptions = computed(() =>
  hazardReviewData.overview.hazardTypes.map((item) => ({ label: item.label, value: item.key })),
)
const statusOptions = HANDLE_STATUS_OPTIONS

const getFilters = () => ({
  keyword: keyword.value.trim(),
  typeCode: typeCode.value,
  handleStatus: handleStatus.value,
  dateRange: dateRange.value,
})

const notify = () => emit('change', getFilters())

/** 条件变化即时筛选，「查询」按钮用于手动触发同一逻辑 */
watch([keyword, typeCode, handleStatus, dateRange], notify)

const handleReset = () => {
  keyword.value = ''
  typeCode.value = ''
  handleStatus.value = ''
  dateRange.value = []
  notify()
}

const toggleExpanded = () => {
  expanded.value = !expanded.value
}
</script>

<style lang="less" scoped>
.process-filters {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  margin: 0 24px 0 0;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 搜索行 */
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  flex: 1 1 0;
  min-width: 0;

  /* 与应急筛选一致：白底 + 浅描边，聚焦 / 悬浮转蓝边 */
  :deep(.el-input__wrapper) {
    height: 32px;
    padding: 0 12px;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 0 0 1px #e4e8ee inset;
    transition: box-shadow 0.2s ease;
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #bcd6f7 inset;
  }

  :deep(.el-input__prefix) {
    color: #a6b0bd;
    font-size: 14px;
  }

  :deep(.el-input__inner) {
    color: #222527;
    font-size: 14px;
  }
}

/* 展开区：两行，行间距与首行一致 */
.filter-more {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
}

.query-button {
  flex-shrink: 0;
  width: 56px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: #007bff;
  color: #ffffff;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #3395ff;
  }
}

/* 灾害类型 / 处理状态 */
.filter-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.filter-cell,
.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #5c6673;
  font-size: 14px;
  white-space: nowrap;
}

.filter-cell :deep(.el-select),
.time-cell :deep(.el-date-editor) {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}

.filter-cell :deep(.el-select__wrapper) {
  min-height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  font-size: 14px;
  transition: box-shadow 0.2s ease;
}

.filter-cell :deep(.el-select__wrapper:hover),
.filter-cell :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.filter-cell :deep(.el-select__placeholder) {
  color: #222527;
}

.filter-cell :deep(.el-select__placeholder.is-transparent) {
  color: #a6b0bd;
}

/* 下拉箭头改用设计稿图标，并隐藏 Element Plus 自带箭头 */
.filter-cell :deep(.el-select__caret) {
  width: 16px;
  height: 16px;
  margin-left: 4px;
  background: url('@/assets/imgs/hazardReview/icon-arrow-down-select.webp') no-repeat center / contain;
}

.filter-cell :deep(.el-select__caret svg) {
  display: none;
}

/* 时间范围行：区间选择器比单日宽，空间不足时整行换行，查询 / 重置按钮保持一行 */
.time-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.time-cell {
  flex: 1 1 220px;
  min-width: 0;
}

/* 区间选择器内部的日期与分隔符收小到 12px，避免两个日期互相挤压 */
.time-cell :deep(.el-range-input),
.time-cell :deep(.el-range-separator) {
  font-size: 12px;
}

.time-cell :deep(.el-input__wrapper) {
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  transition: box-shadow 0.2s ease;
}

.time-cell :deep(.el-input__wrapper:hover),
.time-cell :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.time-cell :deep(.el-input__inner) {
  color: #222527;
  font-size: 14px;
}

/* 日期图标：设计稿为深色 */
.time-cell :deep(.el-input__icon) {
  color: #222527;
  font-size: 14px;
}

.reset-button {
  flex-shrink: 0;
  width: 56px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  color: #5c6673;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    color: #007bff;
    box-shadow: 0 0 0 1px #bcd6f7 inset;
  }
}

/* 分割线：上 12px、下 24px，中间留出「展开」入口 */
.filter-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 24px;
}

.divider-line {
  flex: 1 1 auto;
  height: 1px;
  background: #e8eef5;
}

.divider-toggle {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
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
    display: block;
    width: 14px;
    height: 14px;
    background: url('@/assets/imgs/emergency/icon-chevron-down.png') no-repeat center / contain;
    transition: transform 0.2s ease;
  }

  &.is-expanded .chevron {
    transform: rotate(180deg);
  }
}
</style>
