<template>
  <Teleport to="body">
    <Transition name="record-panel">
      <div v-if="visible" class="record-mask" @click.self="emit('close')">
        <aside class="record-panel" role="dialog" aria-modal="true" aria-label="会议记录">
          <header class="panel-header">
            <div>
              <span class="header-label">会商研判</span>
              <h3>{{ meetingTitle || '会议记录' }}</h3>
            </div>
            <button type="button" class="icon-btn" title="关闭" aria-label="关闭" @click="emit('close')">
              <i class="iconfont icon-close"></i>
            </button>
          </header>

          <nav class="panel-tabs" aria-label="会议记录分类">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              type="button"
              :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              {{ tab.label }}
              <span v-if="tab.value === 'transcript'" class="count-num">{{ transcriptList.length }}</span>
            </button>
          </nav>

          <section v-if="activeTab === 'transcript'" class="panel-content transcript-content">
            <div class="status-row">
              <span class="status-dot" :class="`is-${statusTone}`"></span>
              <span>{{ transcriptionText }}</span>
              <span v-if="transcriptionStatus.updatedAt" class="update-time">
                更新于 {{ formatDateTime(transcriptionStatus.updatedAt) }}
              </span>
            </div>

            <div v-if="transcriptionStatus.error" class="error-tip">
              {{ transcriptionStatus.error }}
            </div>

            <div v-loading="loading" class="transcript-list">
              <div v-for="item in displayTranscripts" :key="item.id" class="transcript-item">
                <div class="speaker-avatar">{{ speakerInitial(item.speakerName) }}</div>
                <div class="transcript-main">
                  <div class="transcript-meta">
                    <strong>{{ item.speakerName || '未识别发言人' }}</strong>
                    <span>{{ formatTimeRange(item.startedAt, item.endedAt) }}</span>
                    <em v-if="item.mock">模拟转写</em>
                  </div>
                  <p>{{ item.text || '' }}</p>
                </div>
              </div>
              <div v-if="!loading && !displayTranscripts.length" class="empty-state">
                <i class="iconfont icon-file"></i>
                <strong>暂无语音转写</strong>
                <span>{{ emptyTranscriptText }}</span>
              </div>
            </div>
          </section>

          <section v-else class="panel-content summary-content">
            <div v-loading="loading" class="summary-wrap">
              <template v-if="summary?.content">
                <div class="summary-status">
                  <span class="status-dot" :class="`is-${summaryTone}`"></span>
                  <strong>{{ summaryText }}</strong>
                  <span v-if="summary.updatedAt">{{ formatDateTime(summary.updatedAt) }}</span>
                </div>
                <div class="summary-body md-box-show" v-dompurify-html="summaryHtml"></div>
                <div v-if="summary.error" class="error-tip">{{ summary.error }}</div>
              </template>
              <div v-else class="empty-state summary-empty">
                <i class="iconfont icon-file"></i>
                <strong>{{ summaryText }}</strong>
                <span>{{ summaryEmptyText }}</span>
                <div v-if="summary?.error" class="error-tip">{{ summary.error }}</div>
              </div>
            </div>
          </section>

          <footer class="panel-footer">
            <span>系统仅展示已完成并保存的转写内容</span>
            <button type="button" class="refresh-btn" :disabled="loading" @click="refreshAll(true)">
              <i class="iconfont icon-refresh"></i>刷新
            </button>
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  getMeetingDetail,
  getMeetingTranscripts,
  getMeetingTranscriptionStatus,
} from '@/api/meeting.js'
import { createMd } from '@/utils/md.js'
import { mergeConsecutiveTranscripts } from '@/utils/meetingTranscript.js'

defineOptions({ name: 'MeetingRecordPanel' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  meetingId: { type: String, default: '' },
  meetingTitle: { type: String, default: '' },
  meetingStatus: { type: String, default: 'ACTIVE' },
  initialTab: { type: String, default: 'transcript' },
  autoWaitSummary: { type: Boolean, default: true },
})

const emit = defineEmits(['close'])
const tabs = [
  { label: '语音转写', value: 'transcript' },
  { label: '智能纪要', value: 'summary' },
]
const activeTab = ref('transcript')
const loading = ref(false)
const transcriptList = ref([])
// 同一人且相差 20ms 以内的连续发言合并展示
const displayTranscripts = computed(() => mergeConsecutiveTranscripts(transcriptList.value, 20))
const transcriptionStatus = ref({})
const summary = ref(null)
const resolvedMeetingStatus = ref('ACTIVE')
let pollTimer = null
let requestVersion = 0

const md = createMd({ useBreak: true })
const summaryHtml = computed(() => {
  const content = summary.value?.content
  if (!content) return ''
  return md.render(content)
})

const statusMap = {
  NOT_STARTED: ['尚未开始', 'muted'],
  STARTING: ['转写启动中', 'pending'],
  RUNNING: ['语音转写中', 'running'],
  STOPPING: ['正在整理尾句', 'pending'],
  COMPLETED: ['语音转写已完成', 'success'],
  FAILED: ['语音转写失败', 'danger'],
}

const summaryMap = {
  RUNNING: ['智能纪要生成中', 'running'],
  COMPLETED: ['智能纪要已生成', 'success'],
  FAILED: ['智能纪要生成失败', 'danger'],
  SKIPPED: ['未生成智能纪要', 'muted'],
}

const transcriptionText = computed(() => {
  if (transcriptionStatus.value.enabled === false) return '语音转写未启用'
  if (transcriptionStatus.value.enabled === true && transcriptionStatus.value.available === false) return '转写服务暂不可用'
  return statusMap[transcriptionStatus.value.status]?.[0] || '正在获取转写状态'
})

const statusTone = computed(() => {
  if (transcriptionStatus.value.enabled === false) return 'muted'
  if (transcriptionStatus.value.enabled === true && transcriptionStatus.value.available === false) return 'danger'
  return statusMap[transcriptionStatus.value.status]?.[1] || 'muted'
})

const summaryText = computed(() => {
  if (summary.value?.status) return summaryMap[summary.value.status]?.[0] || '智能纪要状态未知'
  return resolvedMeetingStatus.value === 'ENDED' ? '智能纪要准备中' : '会议结束后生成智能纪要'
})

const summaryTone = computed(() => summaryMap[summary.value?.status]?.[1] || 'muted')
const emptyTranscriptText = computed(() => {
  if (transcriptionStatus.value.status === 'RUNNING') return '开始发言后，识别完成的内容会显示在这里'
  return '当前会议还没有可展示的转写内容'
})
const summaryEmptyText = computed(() => {
  if (resolvedMeetingStatus.value !== 'ENDED') return '会议结束并完成语音转写后，系统将自动生成会议纪要'
  if (summary.value?.status === 'FAILED') return '可稍后刷新会议详情查看最新结果'
  if (summary.value?.status === 'SKIPPED') return '本次会议没有可用于生成纪要的转写内容'
  return '系统正在整理完整转写并生成会议纪要，请稍候'
})

const speakerInitial = (name) => String(name || '会').trim().slice(0, 1)
const formatDateTime = (value) => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : ''
const formatTimeRange = (start, end) => {
  if (!start) return ''
  const startText = dayjs(start).format('HH:mm:ss')
  return end ? `${startText} - ${dayjs(end).format('HH:mm:ss')}` : startText
}

const mergeTranscripts = (list) => {
  const recordMap = new Map(transcriptList.value.map((item) => [String(item.id), item]))
  list.forEach((item) => recordMap.set(String(item.id), item))
  transcriptList.value = Array.from(recordMap.values()).sort((a, b) => {
    const timeDiff = dayjs(a.startedAt).valueOf() - dayjs(b.startedAt).valueOf()
    return timeDiff || String(a.id).localeCompare(String(b.id))
  })
}

const fetchTranscripts = async (version) => {
  const pageSize = 100
  const isInitialLoad = transcriptList.value.length === 0
  const firstPage = isInitialLoad ? 1 : Math.floor(transcriptList.value.length / pageSize) + 1
  const res = await getMeetingTranscripts(props.meetingId, { pageNum: firstPage, pageSize })
  if (version !== requestVersion) return
  const firstList = Array.isArray(res?.data) ? res.data : []
  mergeTranscripts(firstList)

  // 已结束的长会议也需要一次性回看完整记录，不能因停止轮询只留下第一页。
  const total = Number(res?.total)
  const pageCount = Number.isFinite(total) ? Math.ceil(total / pageSize) : 1
  if (!isInitialLoad || pageCount <= 1) return
  const remainingResults = await Promise.all(
    Array.from({ length: pageCount - 1 }, (_, index) => (
      getMeetingTranscripts(props.meetingId, { pageNum: index + 2, pageSize })
    )),
  )
  if (version !== requestVersion) return
  remainingResults.forEach((result) => {
    mergeTranscripts(Array.isArray(result?.data) ? result.data : [])
  })
}

const refreshAll = async (showError = false) => {
  if (!props.meetingId || loading.value) return
  const version = ++requestVersion
  loading.value = true
  try {
    const [statusResult, detailResult] = await Promise.allSettled([
      getMeetingTranscriptionStatus(props.meetingId),
      getMeetingDetail(props.meetingId, { notUseError: true }),
      fetchTranscripts(version),
    ])
    if (version !== requestVersion) return
    if (statusResult.status === 'fulfilled') transcriptionStatus.value = statusResult.value || {}
    if (detailResult.status === 'fulfilled') {
      resolvedMeetingStatus.value = detailResult.value?.meeting?.status || props.meetingStatus
      summary.value = detailResult.value?.summary || null
    }
    if (showError && [statusResult, detailResult].every((result) => result.status === 'rejected')) {
      ElMessage.error('会议记录刷新失败，请稍后重试')
    }
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

const shouldPoll = computed(() => {
  const transcriptionPending = ['STARTING', 'RUNNING', 'STOPPING'].includes(transcriptionStatus.value.status)
  const summaryPending = props.autoWaitSummary
    && resolvedMeetingStatus.value === 'ENDED'
    && (!summary.value || summary.value.status === 'RUNNING')
  return props.visible && (transcriptionPending || summaryPending)
})

const stopPolling = () => {
  window.clearInterval(pollTimer)
  pollTimer = null
}

const syncPolling = () => {
  stopPolling()
  if (shouldPoll.value) pollTimer = window.setInterval(() => refreshAll(false), 3000)
}

watch(() => [props.visible, props.meetingId, props.initialTab], ([visible, meetingId, initialTab]) => {
  stopPolling()
  requestVersion += 1
  if (!visible || !meetingId) return
  activeTab.value = initialTab === 'summary' ? 'summary' : 'transcript'
  transcriptList.value = []
  transcriptionStatus.value = {}
  summary.value = null
  resolvedMeetingStatus.value = props.meetingStatus
  void refreshAll(false)
}, { immediate: true })

watch(shouldPoll, syncPolling)
onBeforeUnmount(() => {
  requestVersion += 1
  stopPolling()
})
</script>

<style lang="less" scoped>
.record-mask { position: fixed; inset: 0; z-index: 3000; background: rgba(34, 37, 39, 0.18); }
.record-panel { position: absolute; top: 0; right: 0; display: flex; width: min(520px, 100vw); height: 100%; flex-direction: column; overflow: hidden; background: #fff; box-shadow: -8px 0 28px rgba(34, 37, 39, 0.12); color: #222527; }
.panel-header { display: flex; min-height: 82px; align-items: center; justify-content: space-between; padding: 16px 20px; box-sizing: border-box; border-bottom: 1px solid #E0EEFA; background: linear-gradient(110deg, #F4F9FF, #FFFFFF); }
.header-label { color: #007BFF; font-size: 12px; }
.panel-header h3 { max-width: 420px; margin: 6px 0 0; overflow: hidden; color: #222527; font-size: 18px; text-overflow: ellipsis; white-space: nowrap; }
.icon-btn { display: grid; width: 32px; height: 32px; place-items: center; padding: 0; border: 0; border-radius: 6px; background: transparent; color: #617185; cursor: pointer; }
.icon-btn:hover { background: #E0EEFA; color: #007BFF; }
.panel-tabs { display: flex; height: 52px; padding: 0 20px; border-bottom: 1px solid #E0EEFA; }
.panel-tabs button { position: relative; display: flex; min-width: 112px; align-items: center; justify-content: center; gap: 8px; padding: 0 12px; border: 0; background: transparent; color: #617185; font-size: 14px; cursor: pointer; }
.panel-tabs button::after { position: absolute; right: 12px; bottom: 0; left: 12px; height: 3px; border-radius: 3px 3px 0 0; background: #007BFF; content: ''; opacity: 0; transform: scaleX(0.5); transition: opacity 0.2s ease, transform 0.2s ease; }
.panel-tabs button.active { color: #007BFF; font-weight: 600; }
.panel-tabs button.active::after { opacity: 1; transform: scaleX(1); }
.count-num { min-width: 20px; height: 20px; padding: 0 5px; border-radius: 10px; background: #E0EEFA; color: #007BFF; font-family: 'Alimama FangYuanTi VF', sans-serif; font-size: 12px; font-style: normal; line-height: 20px; box-sizing: border-box; }
.panel-content { min-height: 0; flex: 1; overflow: hidden; }
.transcript-content { display: flex; flex-direction: column; padding: 16px 20px 0; }
.status-row, .summary-status { display: flex; min-height: 38px; align-items: center; gap: 8px; color: #617185; font-size: 12px; }
.update-time { margin-left: auto; color: #A6ACB8; }
.status-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #A6ACB8; }
.status-dot.is-running { background: #44B699; box-shadow: 0 0 0 4px rgba(68, 182, 153, 0.14); animation: status-pulse 1.6s ease infinite; }
.status-dot.is-pending { background: #FF922C; }.status-dot.is-success { background: #44B699; }.status-dot.is-danger { background: #E45B5B; }
.error-tip { margin: 8px 0; padding: 10px 12px; border-radius: 6px; background: rgba(228, 91, 91, 0.08); color: #E45B5B; font-size: 12px; line-height: 20px; }
.transcript-list { min-height: 160px; flex: 1; overflow-y: auto; padding: 8px 4px 20px 0; }
.transcript-item { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px solid #EDF1F5; }
.speaker-avatar { display: grid; width: 36px; height: 36px; flex: 0 0 36px; place-items: center; border-radius: 50%; background: #DCEDFF; color: #007BFF; font-size: 14px; font-weight: 600; }
.transcript-main { min-width: 0; flex: 1; }.transcript-meta { display: flex; align-items: center; gap: 8px; min-height: 22px; }
.transcript-meta strong { color: #383C41; font-size: 14px; }.transcript-meta span { color: #A6ACB8; font-family: 'Alimama FangYuanTi VF', sans-serif; font-size: 12px; font-style: normal; }
.transcript-meta em { padding: 1px 6px; border-radius: 4px; background: rgba(255, 146, 44, 0.1); color: #FF922C; font-size: 12px; font-style: normal; }
.transcript-main p { margin: 6px 0 0; color: #617185; font-size: 14px; line-height: 24px; white-space: pre-wrap; word-break: break-word; }
.summary-content { overflow-y: auto; padding: 20px; box-sizing: border-box; }.summary-wrap { min-height: 100%; }
.summary-status { padding-bottom: 14px; border-bottom: 1px solid #EDF1F5; }.summary-status strong { color: #383C41; font-size: 14px; }.summary-status span:last-child { margin-left: auto; color: #A6ACB8; }
.summary-body { padding: 16px 0; color: #383C41; word-break: break-word; }
.empty-state { display: flex; min-height: 280px; align-items: center; justify-content: center; flex-direction: column; color: #A6ACB8; text-align: center; }
.empty-state i { margin-bottom: 12px; color: #DCEDFF; font-size: 42px; }.empty-state strong { margin-bottom: 8px; color: #617185; font-size: 16px; }.empty-state span { max-width: 320px; font-size: 12px; line-height: 20px; }
.summary-empty { min-height: 420px; }.summary-empty .error-tip { max-width: 360px; }
.panel-footer { display: flex; min-height: 56px; align-items: center; justify-content: space-between; padding: 0 20px; border-top: 1px solid #EDF1F5; color: #A6ACB8; font-size: 12px; }
.refresh-btn { display: flex; height: 32px; align-items: center; gap: 6px; padding: 0 12px; border: 1px solid #DCEDFF; border-radius: 6px; background: #fff; color: #007BFF; cursor: pointer; }.refresh-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.record-panel-enter-active, .record-panel-leave-active { transition: background 0.24s ease; }.record-panel-enter-active .record-panel, .record-panel-leave-active .record-panel { transition: transform 0.28s ease; }
.record-panel-enter-from, .record-panel-leave-to { background: transparent; }.record-panel-enter-from .record-panel, .record-panel-leave-to .record-panel { transform: translateX(100%); }
@keyframes status-pulse { 0%, 100% { box-shadow: 0 0 0 3px rgba(68, 182, 153, 0.1); } 50% { box-shadow: 0 0 0 6px rgba(68, 182, 153, 0.04); } }
</style>
