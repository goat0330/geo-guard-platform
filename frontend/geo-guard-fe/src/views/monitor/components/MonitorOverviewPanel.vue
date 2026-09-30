<template>
  <section class="monitor-panel">
    <header class="panel-header">
      <h1>专业监测概览</h1>
      <div class="header-filters">
        <!-- 统计时段：默认全部，选中区间后按「开始 至 结束」展示，重置即回到全部并重新查询 -->
        <DateRangePicker v-model="selectedRange" @change="handleRangeChange" />
      </div>
    </header>

    <div class="panel-body">
      <PanelSectionTitle class="mt-0" title="设备概况" />
      <DeviceOverview />

      <PanelSectionTitle class="mt-16" title="设备类型" level="sub" />
      <DeviceTypeList />

      <PanelSectionTitle class="mt-28" title="仪器告警情况">
        <template #extra>
          <button type="button" class="detail-link" @click="emit('view-alarm-detail')">
            查看详情
            <svg class="detail-arrow" width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M5.5 4L9 7L5.5 10" stroke="#007BFF" stroke-width="1.2" stroke-linecap="round" />
            </svg>
          </button>
        </template>
      </PanelSectionTitle>
      <AlarmSituation />

      <PanelSectionTitle class="mt-20" title="处置状态" level="sub" />
      <DisposeStatus />

      <PanelSectionTitle class="mt-20" title="告警信息" level="sub" />
      <WarningMessageList />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchMonitorOverview } from '../useMonitorData.js'
import DateRangePicker from '@/components/DateRangePicker/index.vue'
import DeviceOverview from './DeviceOverview.vue'
import AlarmSituation from './AlarmSituation.vue'
import DisposeStatus from './DisposeStatus.vue'
import DeviceTypeList from './DeviceTypeList.vue'
import WarningMessageList from './WarningMessageList.vue'
import PanelSectionTitle from '@/components/PanelSectionTitle/index.vue'

defineOptions({ name: 'MonitorOverviewPanel' })

const emit = defineEmits(['date-change', 'view-alarm-detail'])

/** 统计时段，默认空数组 = 全部 */
const selectedRange = ref([])

/** 时段切换：通知外层按新时段重新拉取统计数据 */
const handleRangeChange = (range) => {
  emit('date-change', range)
}

// 面板挂载即拉取概况数据：设备概况 + 设备类型 + 告警情况 + 处置状态 + 最新告警
onMounted(() => {
  fetchMonitorOverview(selectedRange.value)
})
</script>

<style lang="less" scoped>
// 面板容器：460x1048，圆角 20 + 2px 白描边（描边内缩，内容区宽度 = 460 - 4 - 22*2 = 412）
.monitor-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: 22px 22px 0;
  box-sizing: border-box;
  overflow: hidden;
  background: #fff;
  border: 2px solid #fff;
  border-radius: 20px;
  color: #222527;
}

.panel-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 28px;
}

h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  line-height: 18px;
  white-space: nowrap;
  color: #222527;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #007bff;
  font-size: 12px;
  font-weight: 400;
  line-height: 14px;
  cursor: pointer;
}

.detail-arrow {
  margin-left: 4px;
}

.panel-body {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  flex-direction: column;
  /* 滚动条落到面板留白外侧（与风险情况概览一致）：滚动区向右借出 16px 使滚动条贴面板边缘，
     再用等量右内边距补回，内容宽度与原来一致 */
  margin-right: -16px;
  padding-right: 16px;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.mt-0 {
  margin-top: 0;
}

.mt-16 {
  margin-top: 16px;
}

.mt-20 {
  margin-top: 20px;
}

.mt-28 {
  margin-top: 28px;
}

.panel-body::-webkit-scrollbar {
  width: 4px;
}

.panel-body::-webkit-scrollbar-thumb {
  border-radius: 2px;
  background: #d6dee8;
}
</style>
