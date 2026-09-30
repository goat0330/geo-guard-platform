<template>
  <section class="overview-panel">
    <header class="overview-header">
      <h1>群测群防常态</h1>
      <div class="header-filters">
        <!-- 统计时段：默认全部，选中区间后按「开始 至 结束」展示，重置即回到全部并重新查询 -->
        <DateRangePicker :model-value="reportRange" @change="handleRangeChange" />
      </div>
    </header>

    <div class="overview-scroll">
      <section class="business-section">
        <TitleRow title="群众上报">
          <button class="detail-link" type="button" @click="emit('show-reports')">查看详情<el-icon><ArrowRight /></el-icon></button>
        </TitleRow>
        <article class="total-card">
          <span><img :src="reportTotalIcon" alt="" />累计上报总数</span>
          <strong>{{ numberText(displaySummary.totalCount) }}<small>条</small></strong>
        </article>
        <div class="report-metrics">
          <article v-for="item in reportMetrics" :key="item.label">
            <img :src="item.icon" alt="" />
            <div><span>{{ item.label }}</span><strong>{{ numberText(item.value) }}<small>{{ item.unit }}</small></strong></div>
          </article>
        </div>
      </section>

      <section class="business-section">
        <TitleRow title="人工确认状态"><span class="note">AI评估结果需人工确认</span></TitleRow>
        <div class="ring-layout">
          <GroupDefenseDonutChart class="confirm-chart" :items="confirmChartItems" :center-value="percentText(confirmRate)" center-label="确认率" />
          <div class="confirm-list">
            <article v-for="item in confirmMetrics" :key="item.label">
              <span><i :class="item.tone"></i>{{ item.label }}</span>
              <div><span><strong>{{ numberText(item.value) }}</strong><small>条</small></span><em>{{ percentText(item.rate) }}</em></div>
            </article>
          </div>
        </div>
      </section>

      <section class="business-section">
        <TitleRow title="AI研判结果"><span class="note">基于所选范围自动汇总</span></TitleRow>
        <div class="risk-grid">
          <article v-for="item in riskMetrics" :key="item.label" :class="item.tone">
            <span><i class="iconfont icon-d-defense"></i>{{ item.label }}</span><strong>{{ numberText(item.value) }}</strong>
          </article>
        </div>
      </section>

      <section class="business-section">
        <TitleRow title="异常迹象类别" />
        <div class="ring-layout">
          <GroupDefenseDonutChart class="category-chart" :items="categoryItems" :center-value="numberText(displaySummary.totalCount)" center-label="累计" />
          <div v-if="categoryItems.length" class="legend">
            <div v-for="item in visibleCategoryItems" :key="item.name"><span><i :style="{ background: item.color }"></i>{{ item.name }}</span><strong>{{ numberText(item.value) }}</strong></div>
            <p v-if="categoryPageCount > 1" class="legend-pagination">
              <button type="button" :disabled="categoryPage === 1" aria-label="上一页异常迹象" @click="changeCategoryPage(-1)"><el-icon><ArrowLeft /></el-icon></button>
              <span>{{ categoryPage }} / {{ categoryPageCount }}</span>
              <button type="button" :disabled="categoryPage === categoryPageCount" aria-label="下一页异常迹象" @click="changeCategoryPage(1)"><el-icon><ArrowRight /></el-icon></button>
            </p>
          </div>
          <div v-else class="empty-state">接口暂无异常迹象类别数据</div>
        </div>
      </section>

      <section class="business-section ranking-section">
        <TitleRow title="乡镇报送排行"><span class="note">报送数量</span></TitleRow>
        <div v-if="normalizedRankings.length" class="ranking-list">
          <article v-for="(item, index) in normalizedRankings" :key="item.name">
            <i>{{ index + 1 }}</i><span :title="item.name">{{ item.name }}</span><b><em :style="{ width: item.width }"></em></b><strong>{{ numberText(item.value) }}</strong>
          </article>
        </div>
        <div v-else class="empty-ranking-state">
          <img :src="noDataTip" alt="暂无排行数据" />
          <span>暂无排行数据</span>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, ref, watch } from 'vue'
import DateRangePicker from '@/components/DateRangePicker/index.vue'
import reportTotalIcon from '@/assets/imgs/defense/report-total.png'
import reportUsersIcon from '@/assets/imgs/defense/report-users.png'
import reportTownsIcon from '@/assets/imgs/defense/report-towns.png'
import reportPhotosIcon from '@/assets/imgs/defense/report-photos.png'
import noDataTip from '@/assets/imgs/risk/no-data-tip.png'
import GroupDefenseDonutChart from '@/components/GroupDefenseDonutChart/index.vue'

defineOptions({ name: 'GroupDefenseOverview' })
const props = defineProps({
  /** 统计时段 [开始日期, 结束日期]，空数组表示全部（时段由外层持有，切视图后不丢） */
  reportRange: { type: Array, default: () => [] },
  summary: { type: Object, default: () => ({}) },
  rankings: { type: Array, default: () => [] },
})
const emit = defineEmits(['show-reports', 'date-change'])
/** 时段变化：向上抛出，由外层更新时段并按新时段重新取数 */
const handleRangeChange = (range) => {
  emit('date-change', range)
}
const TitleRow = defineComponent({ props: { title: String }, setup: (p, { slots }) => () => h('div', { class: 'section-title' }, [h('h2', p.title), slots.default?.()]) })

const displaySummary = computed(() => ({
  totalCount: props.summary.totalCount,
  reporterCount: props.summary.reporterCount,
  townCount: props.summary.townCount,
  photoCount: props.summary.photoCount,
  veryHighRisk: props.summary.veryHighRisk,
  highRisk: props.summary.highRisk,
  middleRisk: props.summary.middleRisk,
  lowRisk: props.summary.lowRisk,
}))
const reportMetrics = computed(() => [
  { label: '报灾人数/人', value: displaySummary.value.reporterCount, unit: '', icon: reportUsersIcon },
  { label: '涉及乡镇/个', value: displaySummary.value.townCount, unit: '', icon: reportTownsIcon },
  { label: '含图报送/条', value: displaySummary.value.photoCount, unit: '', icon: reportPhotosIcon },
])
const toFiniteNumber = (value) => value === null || value === undefined || value === '' ? undefined : (Number.isFinite(Number(value)) ? Number(value) : undefined)
const confirmed = computed(() => toFiniteNumber(props.summary.confirmedCount))
const pending = computed(() => toFiniteNumber(props.summary.manualConfirmationPendingCount))
const confirmationTotal = computed(() => confirmed.value === undefined || pending.value === undefined ? undefined : confirmed.value + pending.value)
const rateOf = (value, total) => {
  if (value === undefined || total === undefined || total < 0) return undefined
  if (total === 0) return 0
  return Number((value / total * 100).toFixed(1))
}
const toPercent = (value) => {
  const rate = toFiniteNumber(value)
  if (rate === undefined) return undefined
  return Number((rate <= 1 ? rate * 100 : rate).toFixed(1))
}
const confirmRate = computed(() => confirmationTotal.value === 0 ? 0 : toPercent(props.summary.confirmationRate))
const pendingRate = computed(() => rateOf(pending.value, confirmationTotal.value))
const confirmMetrics = computed(() => [
  { label: '已确认结果', value: confirmed.value, rate: confirmRate.value, tone: 'confirmed' },
  { label: '待人工确认', value: pending.value, rate: pendingRate.value, tone: 'pending' },
])
const riskMetrics = computed(() => [
  { label: '极高风险', value: displaySummary.value.veryHighRisk ?? 0, tone: 'very-high' },
  { label: '高风险', value: displaySummary.value.highRisk ?? 0, tone: 'high' },
  { label: '中风险', value: displaySummary.value.middleRisk ?? 0, tone: 'middle' },
  { label: '低风险', value: displaySummary.value.lowRisk ?? 0, tone: 'low' },
])
const confirmChartItems = computed(() => [
  { name: '已确认', value: confirmed.value, color: '#007BFF' },
  { name: '待确认', value: pending.value, color: '#E0EEFA' },
].filter((item) => item.value !== undefined))
const categoryItems = computed(() => Array.isArray(props.summary.categoryItems) ? props.summary.categoryItems : [])
const categoryPage = ref(1)
const categoryPageSize = 6
const categoryPageCount = computed(() => Math.max(1, Math.ceil(categoryItems.value.length / categoryPageSize)))
const visibleCategoryItems = computed(() => {
  const start = (categoryPage.value - 1) * categoryPageSize
  return categoryItems.value.slice(start, start + categoryPageSize)
})
const changeCategoryPage = (offset) => {
  categoryPage.value = Math.min(Math.max(categoryPage.value + offset, 1), categoryPageCount.value)
}
watch(categoryItems, (items) => {
  const firstNonZeroIndex = items.findIndex((item) => Number(item.value) > 0)
  categoryPage.value = firstNonZeroIndex < 0 ? 1 : Math.floor(firstNonZeroIndex / categoryPageSize) + 1
})
const normalizedRankings = computed(() => {
  const max = Math.max(...props.rankings.map((item) => Number(item.value) || 0), 0)
  return props.rankings.slice(0, 5).map((item) => ({ ...item, width: max > 0 ? `${Math.round(Number(item.value) / max * 100)}%` : '0%' }))
})
const numberText = (value) => value === null || value === undefined || value === '' ? '--' : (Number.isFinite(Number(value)) ? Number(value).toLocaleString('zh-CN') : '--')
const percentText = (value) => value === null || value === undefined || value === '' ? '--' : `${value}%`
</script>

<style lang="less" scoped>
.overview-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  color: #222527;
}
.overview-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 18px;
}
h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  line-height: 26px;
}
.header-filters {
  display: flex;
  align-items: center;
}
.overview-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 24px 20px;
}
.business-section {
  margin-top: 8px;
}
.business-section + .business-section {
  margin-top: 24px;
}
.section-title {
  position: relative;
  display: flex;
  min-height: 24px;
  align-items: center;
  justify-content: space-between;
  padding-left: 18px;
}
.section-title::before {
  position: absolute;
  left: 0;
  width: 8px;
  height: 8px;
  background: url('@/assets/imgs/point.png') center / contain no-repeat;
  content: '';
}
.section-title :deep(h2) {
  margin: 0;
  color: #222527;
  font-size: 16px;
  font-weight: 600;
}
.detail-link,
.note {
  color: #007bff;
  font-size: 12px;
}
.detail-link {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.note {
  color: #a6acb8;
}
.total-card {
  display: flex;
  height: 68px;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 0 16px;
  border: 1px solid #dcedff;
  border-radius: 8px;
  background: #f5f9fc;
}
.total-card > span {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #383c41;
  font-size: 14px;
}
.total-card img {
  width: 34px;
  height: 34px;
}
.total-card strong {
  color: #383c41;
  font-size: 26px;
  font-weight: 600;
}
.total-card strong small {
  color: #383c41;
  font-size: 16px;
  font-weight: 400;
}
small {
  margin-left: 2px;
  font-size: 12px;
}
.report-metrics,
.risk-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}
.report-metrics article {
  display: flex;
  height: 68px;
  align-items: center;
  gap: 6px;
  padding: 8px;
  border: 1px solid #e8edf2;
  border-radius: 8px;
  background: #f5f9fc;
}
.report-metrics img {
  width: 26px;
  height: 26px;
}
.report-metrics span {
  display: block;
  color: #617185;
  font-size: 12px;
}
.report-metrics strong {
  display: block;
  margin-top: 2px;
  color: #383c41;
  font-size: 18px;
  font-weight: 600;
}
.ring-layout {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  margin-top: 12px;
}
.ring {
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  border-radius: 50%;
}
.confirm-ring {
  background: conic-gradient(#007bff var(--rate), #e8edf2 0);
}
.distribution-ring {
  background: conic-gradient(#e45b5b 0 10%, #ff922c 10% 25%, #44b699 25% 50%, #007bff 50%);
}
.ring::before {
  grid-area: 1 / 1;
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: #fff;
  content: '';
}
.ring > div {
  z-index: 1;
  grid-area: 1 / 1;
  text-align: center;
}
.ring strong {
  display: block;
  font-size: 20px;
}
.ring span {
  color: #9096a2;
  font-size: 12px;
}
.confirm-list {
  display: grid;
  gap: 18px;
}
.confirm-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}
.confirm-list article > span,
.legend span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #617185;
}
.confirm-list article > span {
  font-size: 14px;
}
.legend span {
  font-size: 12px;
}
.confirm-list i,
.legend i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.confirm-list article > div {
  display: grid;
  grid-template-columns: 64px 42px;
  align-items: baseline;
  gap: 8px;
  margin-top: 0;
}
.confirm-list article > div > span {
  white-space: nowrap;
}
.confirm-list strong {
  color: #383c41;
  font-size: 18px;
  font-weight: 600;
}
.confirm-list small {
  color: #383c41;
  font-size: 14px;
  font-weight: 400;
}
.confirm-list em {
  color: #383c41;
  font-size: 16px;
  font-style: normal;
}
.confirmed {
  background: #007bff;
}
.pending {
  background: #dcedff;
}
.risk-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 12px;
}
.risk-grid article {
  display: flex;
  height: 82px;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  padding: 10px 4px;
  border-radius: 8px;
  text-align: center;
}
.risk-grid span {
  font-size: 14px;
  white-space: nowrap;
}
.risk-grid strong {
  display: block;
  margin-top: 6px;
  color: #383c41;
  font-size: 20px;
  font-weight: 600;
}
.very-high {
  background: #fff0ee;
  color: #e45b5b;
}
.high {
  background: #fff4e8;
  color: #ff922c;
}
.middle {
  background: #fff8ec;
  color: #f9c568;
}
.low {
  background: #eef6ff;
  color: #007bff;
}
.legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.legend > div {
  display: flex;
  justify-content: space-between;
}
.legend strong {
  color: #383c41;
  font-size: 16px;
  font-weight: 600;
}
.legend-pagination {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: -4px 0 0;
  color: #a6acb8;
  font-size: 12px;
}
.legend-pagination button {
  display: grid;
  place-items: center;
  color: #007bff;
  cursor: pointer;
}
.legend-pagination button:disabled {
  color: #a6acb8;
  cursor: not-allowed;
}
.ranking-list {
  display: grid;
  gap: 14px;
  margin-top: 12px;
}
.ranking-list article {
  display: grid;
  min-height: 28px;
  grid-template-columns: 18px 78px 1fr 32px;
  align-items: center;
  gap: 8px;
}
.ranking-list article > i {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 4px;
  background: #dcedff;
  color: #007bff;
  font-size: 10px;
  font-style: normal;
}
.ranking-list article > span {
  overflow: hidden;
  color: #617185;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ranking-list b {
  height: 4px;
  overflow: hidden;
  border-radius: 2px;
  background: #e8edf2;
}
.ranking-list b em {
  display: block;
  height: 100%;
  background: #007bff;
}
.ranking-list strong {
  color: #383c41;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
}
.empty-state {
  padding: 24px 0;
  color: #a6acb8;
  font-size: 14px;
  text-align: center;
}
.ranking-section {
  display: flex;
  min-height: 300px;
  flex-direction: column;
}
.empty-ranking-state {
  display: flex;
  min-height: 0;
  align-items: center;
  flex-direction: column;
  flex: 1;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: #a6acb8;
  font-size: 14px;
}
.empty-ranking-state img {
  display: block;
  width: 320px;
  height: auto;
}
@media (max-height: 820px) {
  .business-section + .business-section {
    margin-top: 16px;
  }
  .ranking-list {
    gap: 10px;
  }
  .ranking-list article {
    min-height: 26px;
  }
}
</style>
