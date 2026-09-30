<template>
  <div class="alarm-filters">
    <!-- 行政区划：收起态即展示；展开后按钮移到展开区末行（折叠高度动画由 el-collapse-transition 提供） -->
    <div class="filter-head">
      <label class="filter-row">
        <span>行政区划：</span>
        <el-select v-model="filters.region" placeholder="请选择" clearable>
          <el-option v-for="item in monitorData.regionOptions" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </label>
      <template v-if="!expanded">
        <button class="query-button" type="button" @click="notify">查询</button>
        <button class="reset-button" type="button" @click="handleReset">重置</button>
      </template>
    </div>

    <el-collapse-transition>
      <div v-show="expanded" class="filter-more">
        <div class="filter-grid">
          <label class="filter-row">
            <span>告警等级：</span>
            <el-select v-model="filters.level" placeholder="请选择" clearable>
              <el-option v-for="item in alarmFilterOptions.levels" :key="item" :label="item" :value="item" />
            </el-select>
          </label>

          <label class="filter-row">
            <span>是否有效：</span>
            <el-select v-model="filters.validity" placeholder="请选择" clearable>
              <el-option v-for="item in alarmFilterOptions.validities" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
        </div>

        <!-- 时间选择（区间）/ 查询 / 重置：按钮常驻展开区末行末尾 -->
        <div class="filter-time">
          <label class="filter-row">
            <span>时间选择：</span>
            <el-date-picker
              v-model="filters.dateRange"
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
import { ref, watch } from 'vue'
import { alarmFilterOptions } from '../../config.js'
import { monitorData } from '../../useMonitorData.js'

defineOptions({ name: 'AlarmFilterBar' })

const emit = defineEmits(['change'])

const filters = ref({ region: '', level: '', validity: '', dateRange: [] })
/** 展开态：收起时仅展示行政区划，展开后追加「告警等级 / 是否有效 / 时间选择（区间）」 */
const expanded = ref(false)

const notify = () => emit('change', { ...filters.value })

/** 条件变化即时筛选，「查询」按钮用于手动触发同一逻辑 */
watch(filters, notify, { deep: true })

const handleReset = () => {
  filters.value = { region: '', level: '', validity: '', dateRange: [] }
  notify()
}

const toggleExpanded = () => {
  expanded.value = !expanded.value
}
</script>

<style lang="less" scoped>
.alarm-filters {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  margin: 0 24px 0 0;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 收起态首行：行政区划 + 查询 / 重置 */
.filter-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-head .filter-row {
  flex: 1 1 0;
}

/* 展开区：两行，行间距与首行一致 */
.filter-more {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
}

.filter-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  color: #617185;
  font-size: 14px;
  white-space: nowrap;
}

.filter-row :deep(.el-select),
.filter-row :deep(.el-date-editor) {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}

.filter-row :deep(.el-select__wrapper),
.filter-row :deep(.el-input__wrapper) {
  min-height: 34px;
  padding: 0 10px;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  font-size: 14px;
  transition: box-shadow 0.2s ease;
}

.filter-row :deep(.el-select__wrapper:hover),
.filter-row :deep(.el-select__wrapper.is-focused),
.filter-row :deep(.el-input__wrapper:hover),
.filter-row :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.filter-row :deep(.el-select__placeholder.is-transparent),
.filter-row :deep(.el-input__inner::placeholder) {
  color: #a6b0bd;
}

/* 下拉箭头与日期图标用设计稿图标 */
.filter-row :deep(.el-select__caret) {
  width: 16px;
  height: 16px;
  margin-left: 4px;
  background: url('@/assets/imgs/hazardReview/icon-arrow-down-select.webp') no-repeat center / contain;
}

.filter-row :deep(.el-select__caret svg) {
  display: none;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

/* 区间选择器比单日输入宽：空间不足时整行换行，查询 / 重置按钮保持一行 */
.filter-time {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-time .filter-row {
  flex: 1 1 240px;
  min-width: 0;
}

/* 区间选择器填满可用宽度，两个日期用 12px 展示，避免日期文字互相挤压 */
.filter-time :deep(.el-range-editor) {
  width: 100%;
  min-width: 0;
}

.filter-time :deep(.el-range-input),
.filter-time :deep(.el-range-separator) {
  font-size: 12px;
}

.query-button,
.reset-button {
  flex-shrink: 0;
  height: 34px;
  padding: 0 16px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.query-button {
  border: 0;
  background: #007bff;
  color: #ffffff;

  &:hover {
    background: #3395ff;
  }
}

.reset-button {
  border: 0;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  color: #5c6673;

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
