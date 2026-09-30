<template>
  <div class="plan-filters">
    <!-- 关键字：收起态即展示；展开后输入框占满整行，「查询」移到第二行末尾，「重置」始终在最后一行末尾 -->
    <div class="filter-row">
      <span class="filter-label">{{ keywordLabel }}：</span>
      <el-input
        v-model="keyword"
        class="keyword-input"
        placeholder="请输入关键字"
        @keyup.enter="handleSearch"
      />
      <template v-if="!expanded">
        <button class="query-button" type="button" @click="handleSearch">查询</button>
        <button class="reset-button" type="button" @click="handleReset">重置</button>
      </template>
    </div>

    <!-- 展开区：时间选择 / 处理状态（折叠高度动画由 el-collapse-transition 提供） -->
    <el-collapse-transition>
      <div v-show="expanded" class="filter-more">
        <div class="filter-row">
          <span class="filter-label">时间选择：</span>
          <el-date-picker
            v-model="date"
            class="filter-control"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择"
          />
          <button class="query-button" type="button" @click="handleSearch">查询</button>
        </div>

        <div class="filter-row">
          <span class="filter-label">处理状态：</span>
          <el-select v-model="status" class="filter-control" placeholder="请选择" clearable>
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
defineOptions({ name: 'EmergencyPlanFilters' })

const props = defineProps({
  /** 关键字输入框的标签名，随页签切换（隐患点名称 / 灾险情名称） */
  keywordLabel: {
    type: String,
    default: '隐患点名称',
  },
  /** 处理状态下拉选项：[{ label, value }]，由父级按页签下发（隐患点走 handleStatus，灾险情走 status） */
  statusOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['search', 'reset', 'toggle-expand'])

const keyword = ref('')
const date = ref('')
const status = ref('')
/** 展开态：收起时仅展示关键字，展开后追加「时间选择 / 处理状态」 */
const expanded = ref(false)

/* 切换页签时清空关键字，与父级列表的清空保持一致，避免输入框残留上一页签的检索词 */
watch(
  () => props.keywordLabel,
  () => {
    keyword.value = ''
  },
)

/* 两个页签的状态枚举不同（隐患点为 handleStatus 字符串，灾险情为 status 数字），切换后清空旧值 */
watch(
  () => props.statusOptions,
  () => {
    status.value = ''
  },
)

/** 对外统一的条件结构，由父级转成对应页签的请求参数 */
const getFilters = () => ({
  keyword: keyword.value.trim(),
  date: date.value,
  status: status.value,
})

const handleSearch = () => {
  emit('search', getFilters())
}

const handleReset = () => {
  keyword.value = ''
  date.value = ''
  status.value = ''
  emit('search', getFilters())
  emit('reset')
}

const toggleExpanded = () => {
  expanded.value = !expanded.value
  emit('toggle-expand', expanded.value)
}
</script>

<style lang="less" scoped>
.plan-filters {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  margin: 16px 22px 0 0;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 标签固定宽度，「隐患点名称：」「灾险情名称：」等长，保证三行控件左边缘对齐 */
.filter-label {
  flex-shrink: 0;
  width: 88px;
  color: #617185;
  font-size: 14px;
  line-height: 20px;
  white-space: nowrap;
}

/* 展开区两行，与首行间距一致 */
.filter-more {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
}

/* 首行关键字输入框：白底无描边，聚焦 / 悬浮转蓝边 */
.keyword-input {
  flex: 1 1 0;
  min-width: 0;
}

.keyword-input :deep(.el-input__wrapper) {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  transition: box-shadow 0.2s ease;

  &:hover,
  &.is-focus {
    box-shadow: 0 0 0 1px #bcd6f7 inset;
  }
}

.keyword-input :deep(.el-input__inner) {
  color: #222527;
  font-size: 14px;
}

.keyword-input :deep(.el-input__inner)::placeholder {
  color: #a6b0bd;
}

/* 时间 / 状态控件：白底 + 浅描边（.filter-control 打不到 el-date-picker，见下方 :deep 规则） */
.filter-control {
  flex: 1 1 0;
  min-width: 0;
}

/* el-date-editor 自带默认宽度（220px），且 class 会落到内层 input 上，
   所以上面 .filter-control 那套样式打不到它，统一从父级 :deep 命中 */
.filter-more :deep(.el-date-editor) {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}

/* 可视白框是内层的 .el-input__wrapper，圆角 / 描边必须加在这里才生效 */
.filter-more :deep(.el-date-editor .el-input__wrapper) {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  transition: box-shadow 0.2s ease;
}

.filter-more :deep(.el-date-editor .el-input__wrapper:hover),
.filter-more :deep(.el-date-editor .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.filter-control :deep(.el-input__wrapper) {
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  transition: box-shadow 0.2s ease;
}

.filter-control :deep(.el-input__wrapper:hover),
.filter-control :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.filter-control :deep(.el-input__inner) {
  color: #222527;
  font-size: 14px;
}

.filter-control :deep(.el-input__inner)::placeholder {
  color: #a6b0bd;
}

/* 日历图标：设计稿为深色 */
.filter-control :deep(.el-input__icon) {
  color: #222527;
  font-size: 14px;
}

.filter-control :deep(.el-select__wrapper) {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e8ee inset;
  font-size: 14px;
  transition: box-shadow 0.2s ease;
}

.filter-control :deep(.el-select__wrapper:hover),
.filter-control :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bcd6f7 inset;
}

.filter-control :deep(.el-select__placeholder) {
  color: #222527;
}

.filter-control :deep(.el-select__placeholder.is-transparent) {
  color: #a6b0bd;
}

/* 下拉箭头改用设计稿图标，并隐藏 Element Plus 自带箭头 */
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
  flex-shrink: 0;
  width: 56px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
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
  color: #617185;

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
