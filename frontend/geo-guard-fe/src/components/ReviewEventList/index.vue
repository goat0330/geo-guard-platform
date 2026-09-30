<template>
  <section class="event-list-panel">
    <header class="panel-header">
      <h1>复盘事件</h1>
      <span>共 {{ totalCount }} 条</span>
    </header>

    <div class="event-filters">
      <div class="filter-row">
        <label for="review-event-keyword">事件名称：</label>
        <el-input
          id="review-event-keyword"
          v-model="keyword"
          clearable
          placeholder="请输入关键字"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-collapse-transition>
        <div v-show="filterExpanded" class="filter-more">
          <div class="filter-dropdown-row">
            <div class="dropdown-item">
              <label>复盘状态：</label>
              <el-select
                v-model="reviewStatus"
                placeholder="请选择"
                clearable
                @change="handleSelectChange"
              >
                <el-option
                  v-for="item in REVIEW_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <div class="dropdown-item">
              <label>事件类型：</label>
              <el-select
                v-model="eventType"
                placeholder="请选择"
                clearable
                @change="handleSelectChange"
              >
                <el-option
                  v-for="item in EVENT_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
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

    <div v-loading="initialLoading" class="event-list-wrap">
      <DynamicScroller
        v-if="filteredEvents.length"
        ref="scrollerRef"
        :items="filteredEvents"
        :min-item-size="140"
        key-field="id"
        class="event-list"
        @scroll="handleScroll"
      >
        <template #default="{ item, index, active }">
          <DynamicScrollerItem
            :item="item"
            :active="active"
            :data-index="index"
            :size-dependencies="[
              item.eventName,
              item.title,
              item.displayAddress,
              item.materialSummary,
            ]"
          >
            <div class="event-card-item">
              <article
                class="event-card"
                :class="{ 'is-active': String(item.id) === String(activeId) }"
                tabindex="0"
                @click="emit('select', item)"
                @keydown.enter="emit('select', item)"
              >
                <img class="level-icon" :src="getLevelIcon(item)" alt="" />
                <div class="card-content">
                  <el-tooltip :content="item.eventName || item.title || '--'" placement="top" effect="dark" :show-after="100">
                    <h2>{{ item.eventName || item.title || '--' }}</h2>
                  </el-tooltip>
                  <div class="tag-row">
                    <span v-if="item.eventTypeName || item.disasterType || item.type" class="level-tag">
                      {{ item.eventTypeName || item.disasterType || item.type }}
                    </span>
                    <span v-if="item.scaleLevel || item.eventLevelName || item.scale" class="type-tag">
                      {{ item.scaleLevel || item.eventLevelName || item.scale }}
                    </span>
                    <span v-if="item.reviewStatusName || item.reviewStatus" class="status-tag">
                      {{ item.reviewStatusName || item.reviewStatus }}
                    </span>
                  </div>
                  <p class="event-meta" :title="item.displayAddress || item.detailedAddress || item.location || '--'">
                    <Location class="meta-icon" />
                    <span class="meta-text">{{ item.displayAddress || item.detailedAddress || item.location || '--' }}</span>
                  </p>
                  <p class="event-meta">
                    <Clock class="meta-icon" />
                    <span class="meta-text">
                      {{ formatEventDate(item.occurrenceTime || item.time) }}
                      <template v-if="item.materialSummary || item.materials">
                        · {{ item.materialSummary || item.materials }}
                      </template>
                    </span>
                  </p>
                </div>
                <footer>
                  <button type="button" @click.stop="emit('select', item)">查看详情</button>
                  <button type="button" @click.stop="emit('locate', item)">地图定位</button>
                </footer>
              </article>
            </div>
          </DynamicScrollerItem>
        </template>
        <template #after>
          <div v-if="listStatusText" class="list-status">{{ listStatusText }}</div>
        </template>
      </DynamicScroller>

      <el-empty v-else-if="!loading" :image-size="72" description="暂无匹配事件" />
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { Clock, Location } from '@element-plus/icons-vue'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import riskBlueIcon from '@/assets/imgs/risk/blue.svg'
import riskOrangeIcon from '@/assets/imgs/risk/orange.svg'
import riskRedIcon from '@/assets/imgs/risk/red.svg'
import riskYellowIcon from '@/assets/imgs/risk/yellow.svg'
import { EVENT_TYPE_OPTIONS, REVIEW_STATUS_OPTIONS, RISK_LEVEL } from '@/utils/enum.js'

defineOptions({ name: 'ReviewEventList' })

const props = defineProps({
  events: { type: Array, default: () => [] },
  activeId: { type: [String, Number], default: '' },
  loading: { type: Boolean, default: false },
  loadingMore: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
  total: { type: Number, default: undefined },
})

const emit = defineEmits(['select', 'locate', 'search', 'reset', 'load-more'])
const keyword = ref('')
const reviewStatus = ref(undefined)
const eventType = ref(undefined)
const appliedKeyword = ref('')
const appliedReviewStatus = ref(undefined)
const appliedEventType = ref(undefined)
const filterExpanded = ref(false)
const scrollerRef = ref(null)

// 与斜坡单元列表保持一致：极高、高、中、低分别使用红、橙、黄、蓝图标。
const LEVEL_ICON_MAP = {
  [RISK_LEVEL.EXTREME_HIGH]: riskRedIcon,
  [RISK_LEVEL.HIGH]: riskOrangeIcon,
  [RISK_LEVEL.MIDDLE]: riskYellowIcon,
  [RISK_LEVEL.LOW]: riskBlueIcon,
  [RISK_LEVEL.NONE]: riskBlueIcon,
}

const getRiskLevelValue = (item) => {
  if (item.riskLevelValue != null) return item.riskLevelValue
  if (item.riskLevel != null) return item.riskLevel
  const extent = item.riskExtent || ''
  if (extent.includes('极高')) return RISK_LEVEL.EXTREME_HIGH
  if (extent.includes('高')) return RISK_LEVEL.HIGH
  if (extent.includes('中')) return RISK_LEVEL.MIDDLE
  if (extent.includes('低')) return RISK_LEVEL.LOW
  // 容错根据 eventLevel 映射
  if (item.eventLevel === 4) return RISK_LEVEL.EXTREME_HIGH
  if (item.eventLevel === 3) return RISK_LEVEL.HIGH
  if (item.eventLevel === 2) return RISK_LEVEL.MIDDLE
  return RISK_LEVEL.LOW
}

const getLevelIcon = (item) => LEVEL_ICON_MAP[getRiskLevelValue(item)] ?? riskBlueIcon
const formatEventDate = (time) => {
  if (!time) return '--'
  const datePart = String(time).split(' ')[0]
  return datePart || '--'
}

const totalCount = computed(() => (props.total != null ? props.total : filteredEvents.value.length))
const initialLoading = computed(() => props.loading && props.events.length === 0)

const listStatusText = computed(() => {
  if (!filteredEvents.value.length) return ''
  if (props.loadingMore) return '正在加载更多…'
  if (!props.hasMore) return '没有更多数据了'
  return ''
})

const filteredEvents = computed(() => {
  return props.events.filter((item) => {
    // 关键字匹配
    if (appliedKeyword.value) {
      const normalizedKeyword = appliedKeyword.value.toLowerCase()
      const name = String(item.eventName || item.title || '').toLowerCase()
      const id = String(item.id || '').toLowerCase()
      if (!name.includes(normalizedKeyword) && !id.includes(normalizedKeyword)) {
        return false
      }
    }
    // 复盘状态匹配
    if (appliedReviewStatus.value !== undefined && appliedReviewStatus.value !== '' && appliedReviewStatus.value !== null) {
      const targetStatus = Number(appliedReviewStatus.value)
      const statusOption = REVIEW_STATUS_OPTIONS.find((opt) => opt.value === targetStatus)
      const matchesValue =
        (item.reviewStatus != null && Number(item.reviewStatus) === targetStatus) ||
        (item.reviewStatusValue != null && Number(item.reviewStatusValue) === targetStatus)
      const matchesName = Boolean(
        statusOption && (item.reviewStatusName === statusOption.label || item.reviewStatus === statusOption.label),
      )
      if (!matchesValue && !matchesName) {
        return false
      }
    }
    // 事件类型匹配
    if (appliedEventType.value !== undefined && appliedEventType.value !== '' && appliedEventType.value !== null) {
      const targetType = Number(appliedEventType.value)
      const typeOption = EVENT_TYPE_OPTIONS.find((opt) => opt.value === targetType)
      const matchesValue =
        (item.eventType != null && Number(item.eventType) === targetType) ||
        (item.eventTypeValue != null && Number(item.eventTypeValue) === targetType)
      const matchesName = Boolean(
        typeOption &&
          (item.eventTypeName === typeOption.label ||
            item.disasterType === typeOption.label ||
            item.type === typeOption.label),
      )
      if (!matchesValue && !matchesName) {
        return false
      }
    }
    return true
  })
})

const scrollListTop = async () => {
  await nextTick()
  if (scrollerRef.value?.$el) {
    scrollerRef.value.$el.scrollTop = 0
  } else if (scrollerRef.value?.scrollToItem) {
    scrollerRef.value.scrollToItem(0)
  }
}

const handleSelectChange = () => {
  handleSearch()
}

const handleSearch = () => {
  const nameVal = keyword.value ? keyword.value.trim() : ''
  appliedKeyword.value = nameVal
  appliedReviewStatus.value = reviewStatus.value
  appliedEventType.value = eventType.value

  emit('search', {
    eventName: nameVal,
    reviewStatus:
      reviewStatus.value !== undefined && reviewStatus.value !== '' && reviewStatus.value !== null
        ? Number(reviewStatus.value)
        : undefined,
    eventType:
      eventType.value !== undefined && eventType.value !== '' && eventType.value !== null
        ? Number(eventType.value)
        : undefined,
  })
  void scrollListTop()
}

const handleReset = () => {
  keyword.value = ''
  reviewStatus.value = undefined
  eventType.value = undefined
  appliedKeyword.value = ''
  appliedReviewStatus.value = undefined
  appliedEventType.value = undefined

  emit('reset')
  void scrollListTop()
}

const handleScroll = (event) => {
  const target = event?.target
  if (!target) return
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 60) {
    emit('load-more')
  }
}
</script>

<style lang="less" scoped>
.event-list-panel {
  height: 100%;
  padding: 22px 18px 18px 22px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #222527;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  h1 {
    margin: 0;
    font-size: 18px;
    line-height: 26px;
    font-weight: 800;
  }

  span {
    color: #9096a2;
    font-size: 12px;
  }
}

.event-filters {
  flex-shrink: 0;
}

.filter-row {
  display: grid;
  grid-template-columns: auto 217px 48px 48px;
  align-items: center;
  gap: 8px;
}

.filter-row label {
  color: #565e73;
  font-size: 14px;
  font-style: normal;
  font-weight: 400;
  line-height: normal;
  white-space: nowrap;
}

.filter-row :deep(.el-input__wrapper) {
  width: 217px;
  height: 32px;
  min-height: 32px;
  box-sizing: border-box;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #e4e5eb inset;
  transition: box-shadow 0.2s ease;

  &:hover,
  &.is-focus {
    box-shadow: 0 0 0 1px #007bff inset;
  }
}

.filter-row :deep(.el-input__inner) {
  color: #222527;
  font-family: inherit;
  font-size: 14px;
  line-height: normal;

  &::placeholder {
    color: var(--el-text-color-placeholder, #a8abb2);
    font-family: inherit;
    font-size: 14px;
    font-weight: 400;
  }
}

.filter-row :deep(.el-button) {
  width: 48px;
  height: 32px;
  margin-left: 0;
  padding: 0;
  border-radius: 8px;
  font-size: 12px;
}

.filter-row :deep(.el-button--primary) {
  border-color: #007bff;
  background: #007bff;
}

.filter-more {
  min-height: 0;
  padding-top: 10px;
}

.filter-dropdown-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: center;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  label {
    flex-shrink: 0;
    color: #565e73;
    font-size: 14px;
    font-style: normal;
    font-weight: 400;
    line-height: normal;
    white-space: nowrap;
  }

  :deep(.el-select) {
    flex: 1 1 0;
    min-width: 0;
  }

  :deep(.el-select__wrapper) {
    height: 32px;
    min-height: 32px;
    box-sizing: border-box;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 0 0 1px #e4e5eb inset;
    padding: 0 11px;
    font-family: inherit;
    font-size: 14px;
    line-height: normal;
    transition: box-shadow 0.2s ease;
  }

  :deep(.el-select__wrapper.is-focused),
  :deep(.el-select__wrapper:hover) {
    box-shadow: 0 0 0 1px #007bff inset;
  }

  :deep(.el-select__placeholder),
  :deep(.el-select__placeholder.is-transparent) {
    color: var(--el-text-color-placeholder, #a8abb2);
    font-family: inherit;
    font-size: 14px;
    font-weight: 400;
  }

  :deep(.el-select__selected-item) {
    color: #222527;
    font-family: inherit;
    font-size: 14px;
  }
}

.filter-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 16px;
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

.event-list-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  position: relative;
}

.event-list {
  flex: 1;
  height: 100%;
  padding-right: 4px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.event-list::-webkit-scrollbar {
  width: 4px;
}

.event-list::-webkit-scrollbar-thumb {
  border-radius: 2px;
  background: #d6dee8;
}

.event-card-item {
  padding-bottom: 12px;
  box-sizing: border-box;
}

.list-status {
  padding: 8px 0 12px;
  text-align: center;
  color: #9096a2;
  font-size: 12px;
  line-height: 18px;
}

.event-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  column-gap: 10px;
  padding: 16px;
  border: 1px solid #E4EAEF;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

  &:hover,
  &.is-active {
    border-color: #007bff;
    background: #f7fbff;
    box-shadow: 0 4px 14px rgba(0, 123, 255, 0.08);
  }
}

.card-content {
  min-width: 0;
}

.card-content h2 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #222527;
  font-size: 14px;
  line-height: 22px;
  font-weight: 700;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.level-icon {
  width: 38px;
  height: 28px;
  margin-top: 2px;
  object-fit: contain;
}

.tag-row {
  display: flex;
  gap: 6px;
  margin: 10px 0;

  span {
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 12px;
    line-height: 18px;
  }
}

.level-tag {
  color: #e45b5b;
  background: #fff0f0;
}

.type-tag {
  color: #ff922c;
  background: #fff4e8;
}

.status-tag {
  color: #44b699;
  background: #ebf8f4;
}

.event-meta {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 5px 0 0;
  color: #617185;
  font-size: 12px;
  line-height: 18px;

  svg,
  .meta-icon {
    flex: 0 0 14px;
    width: 14px;
    height: 14px;
    margin-top: 2px;
  }

  .meta-text {
    flex: 1;
    min-width: 0;
    word-break: break-all;
  }
}

.event-card footer {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #edf0f4;

  button {
    padding: 0;
    border: 0;
    background: transparent;
    color: #007bff;
    font-size: 12px;
    cursor: pointer;
  }

  button + button {
    border-left: 1px solid #edf0f4;
  }
}
</style>
