<template>
  <section class="overview-panel">
    <!-- 顶部标题与行政区/日期标签 -->
    <header class="overview-header">
      <h1 class="panel-title">风险情况概览</h1>
      <div class="header-tags">
        <!-- 日期选择：纯净无框 el-date-picker -->
        <div class="tag-item date-item" title="切换日期" @click="openDatePicker">
          <el-date-picker
            ref="datePickerRef"
            v-model="currentDate"
            type="date"
            value-format="YYYY-MM-DD"
            :clearable="false"
            :editable="false"
            class="borderless-date-picker"
            popper-class="overview-date-popper"
            @change="handleDateChange"
          >
            <template #prefix>
              <i class="iconfont icon-a-Calendarrili"></i>
            </template>
          </el-date-picker>
          <i class="iconfont icon-arrow-down"></i>
        </div>
      </div>
    </header>

    <!-- 中间滚动内容区 -->
    <div class="overview-scroll-area">
      <div class="overview-content">
        <!-- 模块一：总体范围 -->
        <div class="section-title">
          <span>总体范围</span>
          <span v-if="updatedTime" class="updated">
            <i class="iconfont icon-a-Frame1"></i>
            更新 {{ updatedTime }}
          </span>
        </div>

        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.label" class="summary-card">
            <img :src="item.icon" :alt="item.label" class="card-icon" />
            <div class="card-text">
              <span class="card-label">{{ item.label }}</span>
              <strong class="card-value">{{ item.value || '0' }}</strong>
            </div>
          </div>
        </div>

        <!-- 模块二：动态风险情况 -->
        <div class="section-title">
          <div class="title-left">
            <span>动态风险情况</span>
            <el-dropdown trigger="click" @command="handlePeriodChange">
              <button class="today-filter" type="button">
                {{ currentPeriodLabel }}
                <i class="iconfont icon-arrow-down"></i>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in periodOptions"
                    :key="item.value"
                    :command="item.value"
                    :class="{ 'is-active': selectedPeriod === item.value }"
                  >
                    {{ item.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <button class="detail-button" type="button" @click="emit('show-detail')">
            查看详情
            <i class="iconfont icon-arrow-right"></i>
          </button>
        </div>

        <div class="risk-levels">
          <div v-for="item in riskLevels" :key="item.label" class="risk-level-item">
            <img :src="item.icon" :alt="item.label" class="level-icon" />
            <span class="level-name">{{ item.label }}</span>
            <span class="level-bar">
              <i :style="{ width: item.width, background: item.color }"></i>
            </span>
            <div class="level-meta">
              <strong class="level-count">{{ item.count }}</strong>
              <span class="level-divider"></span>
              <b class="level-percent">{{ item.percent }}</b>
              <div class="level-trend">
                <span class="trend-tag">{{ currentPeriodLabel }}</span>
                <em v-if="item.trendText === '0 -' || item.trendText === '0 —'" class="trend-val zero">0 —</em>
                <em v-else :class="['trend-val', item.trendClass]">
                  {{ item.trendText }}
                  <img v-if="item.trendIcon" :src="item.trendIcon" alt="" class="trend-icon" />
                </em>
              </div>
            </div>
          </div>
        </div>

        <!-- 模块三：乡镇风险对象排序 -->
        <div class="section-title ranking-title">
          <span>乡镇风险对象排序</span>
          <div class="chart-legend">
            <span class="legend-item">
              <i class="legend-bar extreme"></i>
              <span>极高风险单元</span>
            </span>
            <span class="legend-item">
              <i class="legend-bar high"></i>
              <span>高风险单元</span>
            </span>
          </div>
        </div>

        <TownRiskBarChart v-if="towns.length" :data="towns" />
        <el-empty v-else class="town-empty" :image-size="60" description="暂无数据" />
      </div>
    </div>

    <!-- 底部操作按钮 -->
    <footer class="overview-footer">
      <button class="report-button" type="button" @click="emit('open-risk-report')">
        <i class="iconfont icon-a-Frame21"></i>
        <span>今日风险评价报文</span>
      </button>
    </footer>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

import hazardOrange from '@/assets/imgs/fengxian/hazard-orange.png'
import hazardRed from '@/assets/imgs/fengxian/hazard-red.png'
import coverageArea from '@/assets/imgs/fengxian/coverage-area.png'
import affectedPeople from '@/assets/imgs/fengxian/affected-people.png'
import affectedBuildings from '@/assets/imgs/fengxian/affected-buildings.png'
import affectedTowns from '@/assets/imgs/fengxian/affected-towns.png'
import TownRiskBarChart from '@/components/TownRiskBarChart/index.vue'
import { useRiskOverview } from './useRiskOverview.js'

const props = defineProps({
  dataset: {
    type: Object,
    required: true,
  },
})

const datePickerRef = ref(null)

const openDatePicker = () => {
  datePickerRef.value?.focus()
}

const periodOptions = [
  { label: '今日', value: 'today' },
  { label: '近3日', value: '3days' },
  { label: '近7日', value: '7days' },
]

const currentPeriodLabel = computed(() => {
  return periodOptions.find((item) => item.value === selectedPeriod.value)?.label || '今日'
})

const handlePeriodChange = (val) => {
  changePeriod(val)
  emit('period-change', val)
}

const {
  changeDate,
  changePeriod,
  changeRegion,
  currentDate,
  riskLevels,
  selectedPeriod,
  summaryValues,
  towns,
  updatedTime,
} = useRiskOverview(props.dataset)

const handleDateChange = (val) => {
  if (!val) return
  changeDate(val)
  emit('date-change', val)
}

const summaryItems = computed(() => [
  { label: '斜坡单元(个)', value: summaryValues.value.riskSlopeCount, icon: hazardOrange },
  { label: '崩塌隐患点(个)', value: summaryValues.value.riskZoneCount, icon: hazardRed },
  { label: '覆盖面积(km²)', value: summaryValues.value.coveredAreaKm2, icon: coverageArea },
  { label: '涉及人数(人)', value: summaryValues.value.affectedPopulationCount, icon: affectedPeople },
  { label: '影响房屋(栋)', value: summaryValues.value.affectedBuildingCount, icon: affectedBuildings },
  { label: '覆盖乡镇(个)', value: summaryValues.value.coveredTownCount, icon: affectedTowns },
])

const emit = defineEmits([
  'show-detail',
  'open-risk-report',
  'period-change',
  'select-area',
  'date-change',
])

defineExpose({
  selectArea: (val) => changeRegion(val),
})
</script>

<style lang="less" scoped>
.overview-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 24px 24px 20px;
  color: #222527;
  box-sizing: border-box;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  margin-bottom: 24px;
}

.panel-title {
  flex-shrink: 0;
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 18px;
  color: #222527;
}

.header-tags {
  display: flex;
  align-items: center;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #007bff;
  font-size: 14px;
  font-weight: 500;
  line-height: 16px;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }

  i {
    font-size: 14px;
  }

  .icon-arrow-down {
    font-size: 10px;
  }
}

:deep(.borderless-date-picker) {
  width: auto !important;
  display: inline-flex !important;
  align-items: center;
  vertical-align: middle;

  .el-input__wrapper {
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    padding: 0 !important;
    height: auto !important;
    line-height: 16px !important;
    cursor: pointer;
  }

  .el-input__inner {
    width: 86px !important;
    height: 16px !important;
    line-height: 16px !important;
    color: #007bff !important;
    font-size: 14px !important;
    font-weight: 500 !important;
    cursor: pointer !important;
    padding: 0 !important;
    border: none !important;
  }

  .el-input__prefix {
    color: #007bff !important;
    margin-right: 4px;
    font-size: 14px !important;

    i {
      font-size: 14px;
    }
  }

  .el-input__suffix {
    display: none !important;
  }
}

.overview-scroll-area {
  flex: 1 1 0;
  min-height: 0;
  margin-right: -16px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 2px;
    background: #d6dee8;
  }
}

.overview-content {
  padding-right: 16px;
  box-sizing: border-box;
}

.section-title {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-left: 18px;
  font-size: 16px;
  font-weight: 700;
  color: #222529;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 0;
    width: 8px;
    height: 8px;
    transform: translateY(-50%);
    background: url('@/assets/imgs/point.png') center / contain no-repeat;
  }
}

.ranking-title {
  margin-bottom: 8px;
}

.title-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.today-filter {
  display: inline-flex;
  height: 24px;
  align-items: center;
  gap: 4px;
  padding: 0 10px;
  border: 1px solid #e4e8ee;
  border-radius: 4px;
  background: #ffffff;
  color: #383c41;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #b9c8df;
    color: #007bff;
  }

  i {
    font-size: 10px;
  }
}

.detail-button {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #007bff;
  font-size: 12px;
  cursor: pointer;

  i {
    font-size: 12px;
  }
}

.updated {
  color: #8d98a7;
  font-size: 12px;
  font-weight: 400;

  i {
    margin-right: 4px;
    font-size: 14px;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 12px;
  margin-bottom: 24px;
}

.summary-card {
  display: flex;
  align-items: center;
  height: 76px;
  padding: 12px 14px;
  box-sizing: border-box;
  border: 1px solid #edf6ff;
  border-radius: 10px;
  background: #f5f9fc;
  gap: 10px;
}

.card-icon {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
}

.card-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.card-label {
  display: block;
  color: #617185;
  font-size: 14px;
  line-height: 14px;
  white-space: nowrap;
}

.card-value {
  display: block;
  margin-top: 6px;
  color: #222527;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 22px;
  font-style: normal;
  font-weight: Bold-Square;
  line-height: 24px;
  white-space: nowrap;
}

.risk-levels {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.risk-level-item {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f5f9fc;
  box-sizing: border-box;
}

.level-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  object-fit: contain;
  flex-shrink: 0;
}

.level-name {
  width: 28px;
  margin-right: 10px;
  color: #617185;
  font-size: 14px;
  flex-shrink: 0;
}

.level-bar {
  flex: 1;
  height: 5px;
  margin-right: 12px;
  border-radius: 3px;
  background: #e3e9ef;
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
    transition: width 0.3s ease;
  }
}

.level-meta {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.level-count {
  width: 44px;
  color: #222527;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 14px;
  font-style: normal;
  font-weight: Bold-Square;
  text-align: right;
  white-space: nowrap;
  flex-shrink: 0;
}

.level-divider {
  display: inline-block;
  width: 1px;
  height: 10px;
  margin: 0 6px;
  background: #e4eaef;
  flex-shrink: 0;
}

.level-percent {
  width: 44px;
  color: #222527;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 14px;
  font-style: normal;
  font-weight: Bold-Square;
  text-align: left;
  white-space: nowrap;
  flex-shrink: 0;
}

.level-trend {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  width: 74px;
  margin-left: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.trend-tag {
  color: #a6acb8;
  font-size: 12px;
  margin-right: 4px;
}

.trend-val {
  display: inline-flex;
  align-items: center;
  font-style: normal;
  font-size: 12px;

  &.zero {
    color: #617185;
  }

  &.up {
    color: #dd4739;
  }

  &.down {
    color: #44b699;
  }
}

.trend-icon {
  width: 8px;
  height: 10px;
  margin-left: 2px;
  flex-shrink: 0;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #617185;
  font-size: 12px;
}

.legend-bar {
  display: inline-block;
  width: 12px;
  height: 3px;
  border-radius: 2px;

  &.extreme {
    background: #f08d62;
  }

  &.high {
    background: #8fb6f6;
  }
}

.town-empty {
  padding: 24px 0 12px;
}

.overview-footer {
  margin-top: 16px;
  flex-shrink: 0;
}

.report-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  border: 1px solid #007bff;
  border-radius: 8px;
  background: #edf6ff;
  color: #007bff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  i {
    font-size: 16px;
    color: #007bff;
  }

  &:hover {
    background: #dcedff;
    border-color: #007bff;
  }
}

@media (max-height: 850px) {
  .overview-panel {
    padding: 16px 16px 12px;
  }

  .overview-header {
    margin-bottom: 16px;
  }

  .summary-card {
    height: 68px;
  }

  .risk-level-item {
    height: 42px;
  }

  .report-button {
    height: 40px;
    font-size: 14px;
  }
}
</style>
