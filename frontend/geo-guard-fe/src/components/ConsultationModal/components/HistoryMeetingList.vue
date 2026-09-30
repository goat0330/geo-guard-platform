<template>
  <main v-loading="initialLoading" class="history-meeting-list">
    <h3>历史会议</h3>
    <div v-if="meetings.length" class="history-list-wrap">
      <DynamicScroller
        :items="meetings"
        :min-item-size="160"
        key-field="id"
        class="history-list"
        @scroll.native="handleScroll"
        @touchstart.native="handleTouchStart"
        @touchend.native="handleTouchEnd"
      >
        <template #default="{ item, index, active }">
          <DynamicScrollerItem :item="item" :active="active" :data-index="index" :size-dependencies="[item.title, item.participants?.length]">
            <div class="history-item">
              <article class="history-card">
              <div class="history-date">
                <strong class="num-font">{{ formatDate(item.createdAt) }}</strong
                ><span>{{ formatWeek(item.createdAt) }}</span>
              </div>
              <div class="history-divider"></div>
              <div class="history-detail">
                <div>
                  <h4 :title="item.title || ''">{{ item.title || '' }}</h4>
                  <b>已结束</b>
                </div>
                <p class="num-font meeting-time">{{ formatTimeRange(item) }}</p>
                <p class="meta-row">
                  <em>发起人</em><span>{{ item.hostName || '' }}</span>
                </p>
                <p class="meta-row">
                  <em>参会人</em><span :title="getParticipantNames(item)">{{ getParticipantNames(item) }}</span>
                  <small v-if="getParticipantCount(item)" class="num-font">共{{ getParticipantCount(item) }}人</small>
                </p>
              </div>
              <div
                class="summary-card"
                :class="[summaryState(item), { 'is-loading': summaryLoadingId === item.id }]"
                :title="summaryCardTitle(item)"
                @click="handleSummaryClick(item)"
              >
                <img :src="summaryIcon" alt="" />
                <div>
                  <p>
                    <span>智能纪要</span>
                    <b>{{ summaryLabel(item) }}</b>
                  </p>
                  <span class="summary-desc">{{ summaryDescription(item) }}</span>
                </div>
              </div>
              </article>
            </div>
          </DynamicScrollerItem>
        </template>
        <template #after
          ><div class="list-status">{{ listStatus }}</div></template
        >
      </DynamicScroller>
    </div>
    <div v-else-if="!initialLoading" class="history-empty">暂无历史会议</div>

    <!-- 生成纪要弹窗 -->
    <GenerateMinutesModal
      v-model:visible="generateModalVisible"
      :meeting="selectedMeeting"
      @back="generateModalVisible = false"
      @completed="handleGenerateCompleted"
    />

    <!-- 查看纪要弹窗 -->
    <ViewMinutesModal
      v-model:visible="viewModalVisible"
      :meeting="selectedMeeting"
      :summary="selectedSummary"
      :generation-duration="generationDuration"
      @back="viewModalVisible = false"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { getMeetingList, getMeetingDetail } from '@/api/meeting.js'
import summaryIcon from '@/assets/imgs/chatRoom/zhinengjiyao.png'
import GenerateMinutesModal from '@/components/GenerateMinutesModal/index.vue'
import ViewMinutesModal from '@/components/ViewMinutesModal/index.vue'

defineOptions({ name: 'HistoryMeetingList' })

const meetings = ref([])
const initialLoading = ref(false)
const loadingMore = ref(false)
const total = ref(0)
const pageNum = ref(1)
const isEnd = ref(false)
const touchStartY = ref(0)
const pageSize = 20
const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const listStatus = computed(() => (loadingMore.value ? '加载中...' : isEnd.value ? '没有更多数据了' : ''))

const formatDate = (value) => (value ? dayjs(value).format('M月D日') : '')
const formatWeek = (value) => (value ? weeks[dayjs(value).day()] : '')
const formatTimeRange = (item) => {
  if (!item?.createdAt || !item?.endedAt) return ''
  const start = dayjs(item.createdAt)
  const end = dayjs(item.endedAt)
  return `${start.format('HH:mm')} - ${end.format('HH:mm')} · 会议时长${Math.max(0, end.diff(start, 'minute'))}分钟`
}

const getActiveParticipants = (item) => {
  if (Array.isArray(item?.participants)) {
    return item.participants.filter((p) => p?.status === 'ACTIVE' || !p?.status)
  }
  return []
}

const getParticipantNames = (item) => {
  if (item?.participantNames) return item.participantNames
  const active = getActiveParticipants(item)
  if (active.length) {
    return active
      .map((p) => p?.name || p?.displayName || p?.username)
      .filter(Boolean)
      .join('、')
  }
  return item?.hostName || ''
}

const getParticipantCount = (item) => {
  if (typeof item?.participantCount === 'number') return item.participantCount
  const active = getActiveParticipants(item)
  if (active.length) return active.length
  return item?.hostName ? 1 : 0
}

// 区分开启/未开启状态
const isSummaryEnabled = (item) => {
  if (typeof item?.summaryEnabled === 'boolean') return item.summaryEnabled
  if (typeof item?.transcriptionEnabled === 'boolean') return item.transcriptionEnabled
  return Boolean(item?.transcriptionStatus)
}

const summaryState = (item) => (isSummaryEnabled(item) ? 'is-enabled' : 'is-disabled')
const summaryLabel = (item) => (isSummaryEnabled(item) ? '已开启' : '未开启')
const summaryDescription = (item) =>
  isSummaryEnabled(item) ? '点击查看智能纪要' : '本次会议未开启智能纪要'
const summaryCardTitle = (item) => (isSummaryEnabled(item) ? '点击查看纪要' : '')

// 纪要弹窗状态
const generateModalVisible = ref(false)
const viewModalVisible = ref(false)
const selectedMeeting = ref(null)
const selectedSummary = ref(null)
const generationDuration = ref(null)
const summaryLoadingId = ref(null)

const isSummaryReady = (sum) => {
  return Boolean(sum && sum.status === 'COMPLETED' && sum.content)
}

const handleSummaryClick = async (item) => {
  if (!isSummaryEnabled(item) || summaryLoadingId.value === item.id) return

  summaryLoadingId.value = item.id
  try {
    const res = await getMeetingDetail(item.id, { notUseError: true })
    const detail = res?.meeting || res?.data?.meeting || item
    const summary = res?.summary || res?.data?.summary || null
    const participants = res?.participants || res?.data?.participants || []
    selectedMeeting.value = { ...item, ...detail, participants }
    selectedSummary.value = summary
    generationDuration.value = null

    // 根据后端真实纪要状态决定展示：未就绪则展示生成中 Loading 页面持续轮询
    if (isSummaryReady(summary)) {
      viewModalVisible.value = true
    } else {
      generateModalVisible.value = true
    }
  } catch (error) {
    console.error('加载会议详情及纪要失败', error)
    selectedMeeting.value = item
    selectedSummary.value = null
    generationDuration.value = null
    generateModalVisible.value = true
  } finally {
    summaryLoadingId.value = null
  }
}

const handleGenerateCompleted = (payload) => {
  generateModalVisible.value = false
  selectedMeeting.value = payload.meeting
  selectedSummary.value = payload.summary
  generationDuration.value = payload.generationDuration || null
  viewModalVisible.value = true
}


const loadMeetings = async (refresh = false) => {
  if (loadingMore.value || (isEnd.value && !refresh)) return
  if (refresh) {
    pageNum.value = 1
    isEnd.value = false
    initialLoading.value = true
  }
  loadingMore.value = true
  try {
    const response = await getMeetingList({ pageNum: pageNum.value, pageSize })
    const pageItems = Array.isArray(response?.data) ? response.data.filter((item) => item?.status === 'ENDED') : []

    // 补充参会人详情（若列表未直接包含 participants）
    await Promise.allSettled(
      pageItems.map(async (item) => {
        if (!Array.isArray(item.participants)) {
          try {
            const detailRes = await getMeetingDetail(item.id, { notUseError: true })
            const pList = detailRes?.participants || detailRes?.data?.participants
            if (Array.isArray(pList)) {
              item.participants = pList
            }
          } catch (e) {
            // 静默容错
          }
        }
      }),
    )

    const source = refresh ? pageItems : [...meetings.value, ...pageItems]
    meetings.value = Array.from(
      new Map(source.filter((item) => item?.id).map((item) => [String(item.id), item])).values(),
    )
    total.value = Number.isFinite(response?.total) ? response.total : 0
    isEnd.value = !response?.data?.length || pageNum.value * pageSize >= total.value
    if (!isEnd.value) pageNum.value += 1
  } catch (error) {
    console.error('获取历史会议失败', error)
  } finally {
    initialLoading.value = false
    loadingMore.value = false
  }
}

const handleScroll = (event) => {
  const target = event.target
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 48) loadMeetings()
}
const handleTouchStart = (event) => {
  touchStartY.value = event.currentTarget.scrollTop <= 0 ? event.touches[0]?.clientY || 0 : 0
}
const handleTouchEnd = (event) => {
  if (touchStartY.value && (event.changedTouches[0]?.clientY || 0) - touchStartY.value >= 56) loadMeetings(true)
  touchStartY.value = 0
}

onMounted(() => loadMeetings(true))
</script>

<style lang="less" scoped>
.history-meeting-list {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding: 20px 24px 16px;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  background: #fff;
  h3 {
    margin: 0 0 20px;
    color: #222527;
    font-size: 16px;
    line-height: 18px;
  }
}
.history-list-wrap {
  min-height: 0;
  flex: 1;
  margin-right: -24px;
}
.history-list {
  height: 100%;
  padding-right: 24px;
  box-sizing: border-box;

  :deep(.vue-recycle-scroller__item-wrapper) {
    overflow: visible;
  }
}
.history-card {
  display: flex;
  min-height: 148px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #ffffff;
  box-sizing: border-box;
}

.history-item {
  padding: 1px 2px 12px 1px;
  box-sizing: border-box;
}
.history-date {
  width: 88px;
  flex: none;
  strong,
  span {
    display: block;
  }
  strong {
    color: #617185;
    font-size: 16px;
    line-height: 24px;
  }
  span {
    margin-top: 4px;
    color: #9096a2;
    font-size: 12px;
  }
}
.history-divider {
  width: 1px;
  height: 102px;
  background: #dee2ec;
}
.history-detail {
  min-width: 0;
  flex: 1;
  padding-left: 16px;
  > div {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  h4 {
    overflow: hidden;
    margin: 0;
    color: #383c41;
    font-size: 18px;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  b {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 20px;
    flex: none;
    box-sizing: border-box;
    border-radius: 4px;
    background: rgba(68, 182, 153, 0.16);
    color: #44b699;
    font-size: 10px;
    line-height: 1;
  }
  p {
    margin: 4px 0 0;
    color: #617185;
    font-size: 14px;
    line-height: 20px;
  }
  .meeting-time {
    margin-bottom: 6px;
    color: #9096a2;
    font-size: 16px;
    line-height: 24px;
  }
  .meta-row {
    display: flex;
    align-items: center;

    em {
      width: 42px;
      flex: none;
      margin-right: 12px;
      color: #a6acb8;
      font-style: normal;
    }
    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: #617185;
    }
    small {
      margin-left: 12px;
      color: #a6acb8;
      font-size: 14px;
      white-space: nowrap;
      flex: none;
    }
  }
}
.summary-card {
  display: flex;
  width: 206px;
  height: 72px;
  flex: none;
  align-items: center;
  margin-left: 16px;
  padding: 0 12px;
  border-radius: 12px;
  background: #f1f4f7;
  box-sizing: border-box;
  img {
    width: 32px;
    height: 32px;
    margin-right: 8px;
    opacity: 0.55;
  }
  p {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 4px;
    color: #617185;
    font-size: 16px;
    line-height: 22px;
    white-space: nowrap;
  }
  b {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 20px;
    flex: none;
    box-sizing: border-box;
    border-radius: 4px;
    background: rgba(97, 113, 133, 0.16);
    font-size: 10px;
    line-height: 1;
  }
  .summary-desc {
    display: block;
    color: #9096a2;
    font-size: 12px;
    white-space: nowrap;
  }
  &.is-confirmed,
  &.is-pending {
    background: #f5fbff;
    img {
      opacity: 1;
    }
    p {
      color: #383c41;
    }
  }
  &.is-enabled,
  &.is-confirmed,
  &.is-pending {
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: #e8f4ff;
      box-shadow: 0 2px 8px rgba(68, 182, 153, 0.12);
    }

    b {
      color: #44b699;
      background: rgba(68, 182, 153, 0.16);
    }
  }

  &.is-loading {
    opacity: 0.7;
    cursor: wait !important;
    pointer-events: none;
  }
}
.history-empty,
.list-status {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a6acb8;
  font-size: 14px;
}
.history-empty {
  flex: 1;
}
.list-status {
  height: 28px;
  font-size: 12px;
}
.num-font {
  font-style: normal;
}
</style>
