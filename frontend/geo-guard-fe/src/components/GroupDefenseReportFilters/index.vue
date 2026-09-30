<template>
  <section class="report-filters">
    <div class="filter-row">
      <span class="filter-label">上报地点：</span>
      <el-input
        v-model="filters.checkCenterLocation"
        class="filter-control"
        clearable
        placeholder="请输入上报地点"
        @keyup.enter="handleSearch"
      />
      <template v-if="!expanded">
        <button class="query-button" type="button" @click="handleSearch">查询</button>
        <button class="reset-button" type="button" @click="handleReset">重置</button>
      </template>
    </div>

    <el-collapse-transition>
      <div v-show="expanded" class="filter-more">
        <div class="filter-pair-row">
          <div class="filter-item">
            <span class="filter-label">风险等级：</span>
            <el-select v-model="filters.aiRiskLevel" class="filter-control" clearable placeholder="请选择">
              <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>

          <div class="filter-item">
            <span class="filter-label">处理状态：</span>
            <el-select v-model="filters.status" class="filter-control" clearable placeholder="请选择">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>

        <div class="filter-row">
          <span class="filter-label">时间选择：</span>
          <el-date-picker
            v-model="filters.checkDate"
            class="filter-control"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择"
          />
          <button class="query-button" type="button" @click="handleSearch">查询</button>
          <button class="reset-button" type="button" @click="handleReset">重置</button>
        </div>
      </div>
    </el-collapse-transition>

    <div class="filter-divider">
      <span class="divider-line"></span>
      <button class="divider-toggle" :class="{ 'is-expanded': expanded }" type="button" @click="toggleExpanded">
        {{ expanded ? '收起' : '展开' }}
        <span class="chevron"></span>
      </button>
      <span class="divider-line"></span>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'

defineOptions({ name: 'GroupDefenseReportFilters' })

defineProps({
  riskOptions: { type: Array, default: () => [] },
  statusOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['search'])
const expanded = ref(false)
const filters = reactive({ checkCenterLocation: '', aiRiskLevel: null, status: null, checkDate: '' })

const getFilters = () => ({
  checkCenterLocation: filters.checkCenterLocation.trim(),
  aiRiskLevel: filters.aiRiskLevel,
  status: filters.status,
  checkDate: filters.checkDate,
})

const handleSearch = () => {
  emit('search', getFilters())
}

const handleReset = () => {
  filters.checkCenterLocation = ''
  filters.aiRiskLevel = null
  filters.status = null
  filters.checkDate = ''
  handleSearch()
}

const toggleExpanded = () => {
  expanded.value = !expanded.value
}
</script>

<style lang="less" scoped>
.report-filters {
  flex: 0 0 auto;
  padding: 0 24px;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-pair-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.filter-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.filter-pair-row .filter-label {
  width: auto;
  flex-basis: 76px;
}
.filter-label {
  width: 76px;
  flex: 0 0 76px;
  color: #617185;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
}
.filter-control {
  min-width: 0;
  flex: 1 1 0;
}
.filter-more {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
}
.filter-more :deep(.el-date-editor) {
  width: auto;
}
.filter-control :deep(.el-input__wrapper),
.filter-control :deep(.el-select__wrapper) {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  transition: box-shadow 0.2s ease;
}
.filter-control :deep(.el-input__wrapper:hover),
.filter-control :deep(.el-input__wrapper.is-focus),
.filter-control :deep(.el-select__wrapper:hover),
.filter-control :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}
.filter-control :deep(.el-input__inner) {
  color: #222527;
  font-size: 14px;
}
.filter-control :deep(.el-input__inner)::placeholder,
.filter-control :deep(.el-select__placeholder.is-transparent) {
  color: #a6acb8;
}
.filter-control :deep(.el-input__icon) {
  color: #222527;
  font-size: 14px;
}
.filter-control :deep(.el-select__caret) {
  width: 16px;
  height: 16px;
  margin-left: 4px;
  background: url('@/assets/imgs/hazardReview/icon-arrow-down-select.webp') no-repeat center / contain;
}
.filter-control :deep(.el-select__caret svg) {
  display: none;
}
.query-button,
.reset-button {
  width: 56px;
  height: 32px;
  flex: 0 0 56px;
  padding: 0;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
}
.query-button {
  border: 0;
  background: #007bff;
  color: #ffffff;
}
.query-button:hover {
  background: #3595fb;
}
.reset-button {
  border: 0;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  color: #617185;
}
.reset-button:hover {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
  color: #007bff;
}
.filter-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 24px;
}
.divider-line {
  height: 1px;
  flex: 1 1 auto;
  background: #e8eef5;
}
.divider-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #9096a2;
  font-size: 12px;
  line-height: 18px;
  cursor: pointer;
}
.divider-toggle:hover {
  color: #007bff;
}
.chevron {
  display: block;
  width: 14px;
  height: 14px;
  background: url('@/assets/imgs/emergency/icon-chevron-down.png') no-repeat center / contain;
  transition: transform 0.2s ease;
}
.divider-toggle.is-expanded .chevron {
  transform: rotate(180deg);
}
</style>
