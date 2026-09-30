<template>
  <section class="message-center-page">
    <header class="page-header">
      <div>
        <h1>消息中心</h1>
        <p>集中查看和处理业务通知</p>
      </div>
      <el-dropdown v-if="!batchMode" trigger="click" @command="enterBatchMode">
        <el-button :icon="MoreFilled" circle title="批量操作" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="read" :icon="CircleCheck">批量已读</el-dropdown-item>
            <el-dropdown-item command="delete" :icon="Delete" divided>批量删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <MessageCenterToolbar
      :status="condition.status"
      :date-range="dateRange"
      :type="condition.type"
      :counts="messageCounts"
      :type-options="typeOptions"
      @update:status="setStatus"
      @update:date-range="setDateRange"
      @update:type="setType"
    />

    <MessageCenterList
      :messages="messages"
      :selected-ids="selectedIds"
      :batch-mode="batchMode"
      :loading="loading"
      :type-map="MEETING_TYPE_MAP"
      @handle="handleMessage"
      @toggle="toggleSelected"
    />

    <footer class="page-footer">
      <template v-if="batchMode">
        <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="toggleAll">
          {{ selectedIds.length ? `已选 ${selectedIds.length} 条` : '全选当前页' }}
        </el-checkbox>
        <div class="batch-actions">
          <el-button @click="exitBatchMode">取消</el-button>
          <el-button
            :type="batchType === 'delete' ? 'danger' : 'primary'"
            :disabled="!selectedIds.length"
            @click="dialogVisible = true"
          >
            {{ batchType === 'delete' ? '删除' : '标记已读' }}
          </el-button>
        </div>
      </template>
      <template v-else>
        <span class="total-text"
          >共 <b>{{ total }}</b> 条消息</span
        >
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next"
          background
          :total="total"
          @current-change="loadMessages"
          @size-change="handlePageSizeChange"
        />
      </template>
    </footer>

    <ConfirmDialog
      v-model="dialogVisible"
      :title="batchType === 'delete' ? '确定删除消息？' : '标记消息为已读？'"
      :content="
        batchType === 'delete' ? '删除后消息将无法恢复。' : `确认将选中的 ${selectedIds.length} 条消息标记为已读？`
      "
      :delete-mode="batchType === 'delete'"
      @confirm="confirmBatchAction"
    />

    <!-- 会商纪要查看弹窗（只读模式：隐藏生成过程和底部操作栏及按钮） -->
    <ViewMinutesModal
      v-model:visible="minutesModalVisible"
      :meeting="selectedMeeting"
      :summary="selectedSummary"
      :readonly="true"
      @back="closeMinutesModal"
    />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, Delete, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import MessageCenterToolbar from '@/components/MessageCenterToolbar/index.vue'
import MessageCenterList from '@/components/MessageCenterList/index.vue'
import ConfirmDialog from '@/components/ConfirmDialog/index.vue'
import ViewMinutesModal from '@/components/ViewMinutesModal/index.vue'
import { getMessageList, getMessageNoticeStat, deleteMessageNotice, dealMessageNotice } from '@/api/notify.js'
import { getMeetingDetail } from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'
import { MEETING_TYPE_MAP } from '@/utils/enum.js'
import { eventBus } from '@/utils/eventBus.js'

defineOptions({ name: 'MessageCenter' })

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const messages = ref([])
const messageCounts = ref({ totalCount: 0, readCount: 0, unreadCount: 0 })
const dateRange = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const batchMode = ref(false)
const batchType = ref('')
const selectedIds = ref([])
const dialogVisible = ref(false)
const minutesModalVisible = ref(false)
const selectedMeeting = ref({})
const selectedSummary = ref(null)
const condition = reactive({ status: '', type: '' })

const typeOptions = computed(() => Object.entries(MEETING_TYPE_MAP).map(([value, label]) => ({ value, label })))
const isAllSelected = computed(() => messages.value.length > 0 && selectedIds.value.length === messages.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && !isAllSelected.value)

function buildParams() {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    orderByColumn: 'createDate',
    isAsc: 'desc',
    status: condition.status,
    type: condition.type,
  }
  if (dateRange.value.length === 2) {
    params.params = {
      beginTime: dayjs(dateRange.value[0]).format('YYYY-MM-DD 00:00:00'),
      endTime: dayjs(dateRange.value[1]).format('YYYY-MM-DD 23:59:59'),
    }
  }
  return params
}

async function loadCounts() {
  const result = await getMessageNoticeStat()
  messageCounts.value = result || { totalCount: 0, readCount: 0, unreadCount: 0 }
}

async function loadMessages() {
  loading.value = true
  try {
    const [result] = await Promise.all([getMessageList(buildParams()), loadCounts()])
    messages.value = Array.isArray(result?.data) ? result.data : []
    total.value = Number(result?.total) || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  pageNum.value = 1
  loadMessages()
}

function setStatus(value) {
  condition.status = value
  resetAndLoad()
}
function setType(value) {
  condition.type = value
  resetAndLoad()
}
function setDateRange(value) {
  dateRange.value = value
  resetAndLoad()
}
function handlePageSizeChange() {
  pageNum.value = 1
  loadMessages()
}

function enterBatchMode(type) {
  batchMode.value = true
  batchType.value = type
  selectedIds.value = []
}

function exitBatchMode() {
  batchMode.value = false
  batchType.value = ''
  selectedIds.value = []
}

function toggleSelected(id) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((selectedId) => selectedId !== id)
    : [...selectedIds.value, id]
}

function toggleAll(checked) {
  selectedIds.value = checked ? messages.value.map((item) => item.id) : []
}

async function markAsRead(ids, showTip = true) {
  await dealMessageNotice({ ids })
  const idSet = new Set(ids.map(String))
  messages.value = messages.value.map((item) => (idSet.has(String(item.id)) ? { ...item, status: 1 } : item))
  await loadCounts()
  if (showTip) ElMessage.success('操作成功')
}

async function confirmBatchAction() {
  const ids = [...selectedIds.value]
  dialogVisible.value = false
  if (!ids.length) return
  if (batchType.value === 'delete') {
    await deleteMessageNotice(ids)
    ElMessage.success('删除成功')
  } else {
    await markAsRead(ids)
  }
  exitBatchMode()
  await loadMessages()
}

async function handleMessage(item) {
  if (batchMode.value) {
    toggleSelected(item.id)
    return
  }
  const bizData = parseBizData(item.bizData)
  const meetingId = bizData?.meetingId ? String(bizData.meetingId) : ''
  if (!meetingId) return ElMessage.error('消息数据异常，会议ID缺失')

  let detail
  try {
    detail = await getMeetingDetail(meetingId, { notUseError: true })
  } catch {
    ElMessage.warning('会议已删除或您已不在参会名单中')
    return
  }

  if (!detail?.meeting) {
    ElMessage.warning('会议不存在或暂无访问权限')
    return
  }
  if (item.status !== 1) await markAsRead([item.id], false)

  if (item.type === 'meeting-summary') {
    const detailMeeting = detail.meeting || {}
    const participants = detail.participants || []
    const summary = detail.summary || null
    selectedMeeting.value = {
      ...detailMeeting,
      id: meetingId,
      participants,
    }
    selectedSummary.value = summary
    minutesModalVisible.value = true
    return
  }

  if (item.type === 'meeting') {
    if (detail.meeting.status !== 'ACTIVE') {
      ElMessage.info('该会议已结束，已为您跳转至历史会议')
      eventBus.emit('OPEN_MEETING_FROM_NOTICE', { tab: 'history', meetingId })
      return
    }
    await userStore.messageHandle(bizData, item.type)
  }
}

function parseBizData(value) {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value
  if (typeof value !== 'string' || !value.trim()) return null
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

function closeMinutesModal() {
  minutesModalVisible.value = false
  selectedMeeting.value = {}
  selectedSummary.value = null
}

function syncRouteType() {
  const routeType = typeof route.query.m_type === 'string' ? route.query.m_type : ''
  condition.type = Object.hasOwn(MEETING_TYPE_MAP, routeType) ? routeType : ''
  resetAndLoad()

  const routeMeetingId = typeof route.query.meeting_id === 'string' ? route.query.meeting_id : ''
  if (condition.type === 'meeting-summary' && routeMeetingId) {
    void openMeetingSummary(routeMeetingId)
  } else if (condition.type === 'meeting' && routeMeetingId) {
    void openMeetingNotice(routeMeetingId)
  }
}

async function openMeetingNotice(meetingId) {
  try {
    const detail = await getMeetingDetail(meetingId, { notUseError: true })
    if (!detail?.meeting) {
      ElMessage.warning('会议不存在或暂无访问权限')
      return
    }
    if (detail.meeting.status !== 'ACTIVE') {
      ElMessage.info('该会议已结束，已为您跳转至历史会议')
      eventBus.emit('OPEN_MEETING_FROM_NOTICE', { tab: 'history', meetingId })
      return
    }
    eventBus.emit('OPEN_MEETING_FROM_NOTICE', { meetingId })
  } catch {
    ElMessage.warning('会议已删除或您已不在参会名单中')
  }
}

async function openMeetingSummary(meetingId) {
  try {
    const detail = await getMeetingDetail(meetingId, { notUseError: true })
    if (!detail?.meeting) {
      ElMessage.warning('会议不存在或暂无访问权限')
      return
    }
    const detailMeeting = detail.meeting || {}
    const participants = detail.participants || []
    const summary = detail.summary || null
    selectedMeeting.value = {
      ...detailMeeting,
      id: String(meetingId),
      participants,
    }
    selectedSummary.value = summary
    minutesModalVisible.value = true
  } catch {
    ElMessage.warning('会议已删除或您已不在参会名单中')
  }
}

onMounted(() => {
  eventBus.on('MESSAGE_CENTER_CHANGED', resetAndLoad)
})

watch(() => [route.query.m_type, route.query.meeting_id], syncRouteType, { immediate: true })

onBeforeUnmount(() => {
  eventBus.off('MESSAGE_CENTER_CHANGED', resetAndLoad)
})
</script>

<style lang="less" scoped>
.message-center-page {
  display: flex;
  width: min(1040px, calc(100% - 64px));
  height: 100%;
  min-height: 0;
  margin: 0 auto;
  padding: 32px 0 24px;
  flex: 1;
  flex-direction: column;
  box-sizing: border-box;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;

  h1 {
    margin: 0;
    color: #222527;
    font-size: 24px;
    font-weight: 800;
    line-height: 34px;
  }
  p {
    margin: 4px 0 0;
    color: #9096a2;
    font-size: 14px;
    line-height: 20px;
  }
}

.page-footer {
  display: flex;
  flex: 0 0 auto;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 0;
  border-top: 1px solid #edf1f5;
}

.total-text {
  color: #617185;
  font-size: 14px;
  b {
    color: #383c41;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-weight: 600;
  }
}

.batch-actions {
  display: flex;
  gap: 12px;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #007bff;
}

@media (max-width: 900px) {
  .message-center-page {
    width: calc(100% - 32px);
    padding-top: 24px;
  }
  .page-footer {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
