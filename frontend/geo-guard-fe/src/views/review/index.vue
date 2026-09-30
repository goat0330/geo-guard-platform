<template>
  <div class="review-page">
    <!-- 视图一：复盘报告内页1 (无重叠双栏 + 智能体) -->
    <Transition name="view-fade" mode="out-in">
      <ReviewReportInner
        v-if="currentView === 'report' && selectedEvent"
        :event="selectedEvent"
        @back="handleBackToWorkspace"
      />

      <!-- 视图二：复盘地图与列表详情工作台 -->
      <div v-else class="workspace-wrap">
        <section
          class="map-area"
          :class="{
            'is-left-collapsed': leftCollapsed,
            'is-right-open': detailVisible && !rightCollapsed,
          }"
          aria-label="复盘事件地图"
        >
          <MarsMap
            ref="marsMapRef"
            :show-area-search="false"
            @onload="handleMapLoaded"
            @risk-point-position="handleRiskPointPosition"
          >
            <template #overlay>
              <MapOverlayControls
                class="review-map-controls"
                :class="{
                  'is-detail-open': detailVisible && !rightCollapsed,
                }"
                :show-risk="false"
                :layers="reviewLayers"
                @zoom-in="marsMapRef?.handleZoomIn()"
                @zoom-out="marsMapRef?.handleZoomOut()"
                @locate-home="marsMapRef?.flyToHome()"
                @change-base-map="marsMapRef?.changeBaseMap($event)"
                @toggle-imagery-label="marsMapRef?.toggleImageryLabel($event)"
                @change-layer="marsMapRef?.handleLayerChange($event)"
              />

              <ReviewMapPopup
                :visible="popupVisible && Boolean(eventPointPosition) && Boolean(selectedEvent)"
                :event="selectedEvent"
                :position="eventPointPosition"
                @close="closePopup"
                @detail="openSelectedEvent"
              />
            </template>
          </MarsMap>

          <div class="left-panel-clip" :class="{ 'is-collapsed': leftCollapsed }">
            <ReviewEventList
              class="left-panel"
              :class="{ 'is-collapsed': leftCollapsed }"
              :events="eventList"
              :active-id="selectedEvent?.id"
              :loading="listLoading"
              :loading-more="loadingMore"
              :has-more="hasMore"
              :total="eventTotal"
              @select="selectEvent"
              @locate="locateEvent"
              @search="handleSearch"
              @reset="handleReset"
              @load-more="handleLoadMore"
            />
          </div>

          <div v-if="detailVisible && selectedEvent" class="right-panel-clip" :class="{ 'is-collapsed': rightCollapsed }">
            <ReviewEventDetail
              class="right-panel"
              :class="{ 'is-collapsed': rightCollapsed }"
              :event="selectedEvent"
              @close="closeDetail"
              @feedback="showPendingMessage"
              @report="generateReport"
            />
          </div>
        </section>

        <button
          class="collapse-trigger left-trigger"
          :class="{ 'is-collapsed': leftCollapsed }"
          type="button"
          :title="leftCollapsed ? '展开复盘事件列表' : '收起复盘事件列表'"
          @click="leftCollapsed = !leftCollapsed"
        >
          <img :src="leftCollapsed ? rightArrow : leftArrow" alt="" />
        </button>

        <button
          v-if="detailVisible"
          class="collapse-trigger right-trigger"
          :class="{ 'is-collapsed': rightCollapsed }"
          type="button"
          :title="rightCollapsed ? '展开事件详情' : '收起事件详情'"
          @click="rightCollapsed = !rightCollapsed"
        >
          <img :src="rightCollapsed ? leftArrow : rightArrow" alt="" />
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarsMap from '@/components/MarsMap/index.vue'
import MapOverlayControls from '@/components/MapOverlayControls/index.vue'
import ReviewEventList from '@/components/ReviewEventList/index.vue'
import ReviewEventDetail from '@/components/ReviewEventDetail/index.vue'
import ReviewMapPopup from '@/components/ReviewMapPopup/index.vue'
import ReviewReportInner from '@/components/ReviewReportInner/index.vue'
import leftArrow from '@/assets/imgs/left-arr.png'
import rightArrow from '@/assets/imgs/right-arr.png'
import redPoint from '@/assets/imgs/cesium/red-point.png'
import jianceshebeiIcon from '@/assets/imgs/layer/jianceshebei.png'
import zhengzaigengxinIcon from '@/assets/imgs/layer/zhengzaigengxin.png'
import yiwanchenggengxinIcon from '@/assets/imgs/layer/yiwanchenggengxin.png'
import weixieIcon from '@/assets/imgs/layer/weixie.png'
import zaihaitiIcon from '@/assets/imgs/layer/zaihaiti.png'
import huadongfangxiangIcon from '@/assets/imgs/layer/huadongfangxiang.png'
import { getReviewEventList } from '@/api/review.js'

defineOptions({ name: 'ReviewWorkspace' })

const route = useRoute()
const router = useRouter()

const marsMapRef = ref(null)
const pageNum = ref(1)
const pageSize = 10
const eventList = ref([])
const eventTotal = ref(0)
const listLoading = ref(false)
const loadingMore = ref(false)
const hasMore = computed(() => eventList.value.length < eventTotal.value)
const selectedEvent = ref(null)
const leftCollapsed = ref(false)
const detailVisible = ref(false)
const rightCollapsed = ref(false)
const popupVisible = ref(true)
const eventPointPosition = ref(null)
const searchFilters = ref({
  eventName: '',
  reviewStatus: undefined,
  eventType: undefined,
})

// 复盘事件地图专属图层列表
const reviewLayers = ref([
  { key: 'monitor_device', label: '监测设备', image: jianceshebeiIcon, checked: true },
  { key: 'updating', label: '正在更新', image: zhengzaigengxinIcon, checked: false },
  { key: 'updated', label: '已完成更新', image: yiwanchenggengxinIcon, checked: false },
  { key: 'threat_scope', label: '威胁范围', image: weixieIcon, checked: false },
  { key: 'hazard_scope', label: '灾害体范围', image: zaihaitiIcon, checked: false },
  { key: 'sliding_direction', label: '滑动方向', image: huadongfangxiangIcon, checked: false },
])

// 视图切换：'workspace' | 'report'
const currentView = ref(route.query.mode === 'report' ? 'report' : 'workspace')

/**
 * 适配真实后端接口数据结构，保留后端原有字段同时建立前端兼容别名
 */
const normalizeEvent = (item) => {
  if (!item) return null
  return {
    ...item,
    id: String(item.id ?? ''),
    title: item.eventName || item.title || '',
    type: item.eventTypeName || item.disasterType || item.type || '',
    scale: item.scaleLevel || item.eventLevelName || item.scale || '',
    reviewStatus: item.reviewStatusName || (item.reviewStatus != null ? String(item.reviewStatus) : ''),
    reviewStatusValue: item.reviewStatus,
    eventType: item.eventType,
    eventTypeValue: item.eventType,
    location: item.displayAddress || item.detailedAddress || item.location || '',
    time: item.occurrenceTime || item.time || '',
    description: item.disposalOverview || item.description || '',
    materials: item.materialSummary || item.materials || '',
    timeline: item.timeline || [],
  }
}

/**
 * 加载复盘事件列表数据，支持 reset 刷新与下滑滚动分页追加
 */
const loadEventList = async ({ reset = false, filters = searchFilters.value } = {}) => {
  if (reset) {
    pageNum.value = 1
    listLoading.value = true
  } else {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
  }

  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      eventName: filters?.eventName ? filters.eventName.trim() : undefined,
      reviewStatus:
        filters?.reviewStatus !== undefined && filters?.reviewStatus !== '' && filters?.reviewStatus !== null
          ? Number(filters.reviewStatus)
          : undefined,
      eventType:
        filters?.eventType !== undefined && filters?.eventType !== '' && filters?.eventType !== null
          ? Number(filters.eventType)
          : undefined,
    }
    const res = await getReviewEventList(params)
    const rawList = res?.data || res?.rows || []
    const normalized = rawList.map(normalizeEvent)

    if (reset) {
      eventList.value = normalized
    } else {
      const merged = [...eventList.value, ...normalized]
      eventList.value = Array.from(new Map(merged.map((item) => [String(item.id), item])).values())
    }

    eventTotal.value = Number(res?.total ?? (reset ? normalized.length : eventList.value.length))

    if (eventList.value.length > 0) {
      if (reset || !selectedEvent.value) {
        let target = null
        const currentActiveId = route.query.id || selectedEvent.value?.id
        if (currentActiveId) {
          target = eventList.value.find((item) => String(item.id) === String(currentActiveId))
        }
        selectedEvent.value = target || eventList.value[0]
        if (marsMapRef.value) {
          showEventPoint(selectedEvent.value, false)
        }
      }

      if (eventList.value.length < eventTotal.value) {
        pageNum.value += 1
      }
    } else {
      selectedEvent.value = null
      closePopup()
    }
  } catch (err) {
    console.error('获取复盘事件列表失败', err)
    ElMessage.error('获取复盘事件列表失败')
  } finally {
    listLoading.value = false
    loadingMore.value = false
  }
}

const syncRouteEvent = () => {
  if (route.query.id && eventList.value.length > 0) {
    const found = eventList.value.find((item) => String(item.id) === String(route.query.id))
    if (found) {
      selectedEvent.value = found
      showEventPoint(found)
    }
  }
}

watch(
  () => route.query.mode,
  (mode) => {
    currentView.value = mode === 'report' ? 'report' : 'workspace'
  },
)

watch(
  () => route.query.id,
  () => {
    syncRouteEvent()
  },
)

onMounted(() => {
  loadEventList({ reset: true })
})

const showEventPoint = (event, flyTo = true) => {
  if (!event) return
  let lng = Number(event.longitude)
  let lat = Number(event.latitude)

  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    const [cLng, cLat] = String(event.coordinate ?? '').split(',').map(Number)
    lng = cLng
    lat = cLat
  }

  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    eventPointPosition.value = null
    popupVisible.value = false
    ElMessage.warning('当前复盘事件暂无点位坐标')
    return
  }

  eventPointPosition.value = null
  popupVisible.value = true
  marsMapRef.value?.showRiskPoint(
    { lng, lat },
    {
      flyTo,
      keepView: true,
      alwaysVisible: true,
      image: redPoint,
      width: 30,
      height: 30,
    },
  )
}

const selectEvent = (event) => {
  selectedEvent.value = event
  detailVisible.value = true
  rightCollapsed.value = false
  showEventPoint(event)
}

const openSelectedEvent = () => {
  detailVisible.value = true
  rightCollapsed.value = false
  popupVisible.value = true
}

const locateEvent = (event) => {
  selectedEvent.value = event
  showEventPoint(event)
}

const handleSearch = (filterPayload) => {
  if (typeof filterPayload === 'string') {
    searchFilters.value = {
      eventName: filterPayload,
      reviewStatus: undefined,
      eventType: undefined,
    }
  } else if (filterPayload && typeof filterPayload === 'object') {
    searchFilters.value = {
      eventName: filterPayload.eventName || '',
      reviewStatus: filterPayload.reviewStatus,
      eventType: filterPayload.eventType,
    }
  }
  loadEventList({ reset: true, filters: searchFilters.value })
}

const handleReset = () => {
  searchFilters.value = {
    eventName: '',
    reviewStatus: undefined,
    eventType: undefined,
  }
  loadEventList({ reset: true, filters: searchFilters.value })
}

const handleLoadMore = () => {
  if (hasMore.value && !loadingMore.value) {
    loadEventList({ reset: false, filters: searchFilters.value })
  }
}

const closePopup = () => {
  popupVisible.value = false
  eventPointPosition.value = null
  marsMapRef.value?.clearRiskPoint()
}

const handleRiskPointPosition = (position) => {
  eventPointPosition.value = position
}

const closeDetail = () => {
  detailVisible.value = false
  rightCollapsed.value = false
}

// 进入复盘报告内页
const generateReport = (event) => {
  if (event) {
    selectedEvent.value = event
  }
  currentView.value = 'report'
  router.replace({
    query: {
      ...route.query,
      mode: 'report',
      id: selectedEvent.value?.id,
    },
  })
}

// 从报告内页返回工作台
const handleBackToWorkspace = () => {
  currentView.value = 'workspace'
  const nextQuery = { ...route.query }
  delete nextQuery.mode
  router.replace({ query: nextQuery })
}

const showPendingMessage = () => {
  ElMessage.info('补充资料功能待接口接入')
}

const handleMapLoaded = () => {
  if (selectedEvent.value) {
    showEventPoint(selectedEvent.value, false)
  }
}
</script>

<style lang="less" scoped src="./style.less"></style>
