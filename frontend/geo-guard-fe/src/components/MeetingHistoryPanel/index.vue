<template>
  <Teleport to="body">
    <Transition name="history-panel">
      <div v-if="visible" class="history-mask" @click.self="emit('close')">
        <aside class="history-panel" role="dialog" aria-modal="true" aria-label="历史会议">
          <header class="history-header">
            <div>
              <span>会商研判</span>
              <h3>历史会议</h3>
            </div>
            <button type="button" class="close-btn" title="关闭" aria-label="关闭" @click="emit('close')">
              <i class="iconfont icon-close"></i>
            </button>
          </header>

          <div class="history-toolbar">
            <el-input v-model.trim="keyword" clearable placeholder="搜索会议主题或主持人">
              <template #prefix><i class="iconfont icon-search"></i></template>
            </el-input>
            <button type="button" class="refresh-btn" :disabled="loading" title="刷新历史会议" @click="fetchMeetings(true)">
              <i class="iconfont icon-refresh"></i>
            </button>
          </div>

          <div v-loading="loading" class="history-content">
            <div v-if="filteredMeetings.length" class="meeting-list">
              <button
                v-for="meeting in filteredMeetings"
                :key="meeting.id"
                type="button"
                class="meeting-row"
                @click="emit('view-record', meeting)"
              >
                <span class="meeting-icon"><i class="iconfont icon-file"></i></span>
                <span class="meeting-main">
                  <strong>{{ meeting.title || '未命名会议' }}</strong>
                  <span class="meeting-meta">
                    <em>{{ meeting.hostName || '主持人信息为空' }}</em>
                    <em>{{ formatDateTime(meeting.endedAt || meeting.createdAt) }}</em>
                  </span>
                </span>
                <span class="view-action">查看纪要<i class="iconfont icon-arrow-right"></i></span>
              </button>
            </div>

            <div v-else-if="!loading" class="empty-state">
              <i class="iconfont icon-history"></i>
              <strong>{{ keyword ? '未找到相关会议' : '暂无历史会议' }}</strong>
              <span>{{ keyword ? '请尝试更换搜索关键词' : '结束后的会商会议会显示在这里' }}</span>
            </div>
          </div>

          <footer class="history-footer">
            共 <span>{{ filteredMeetings.length }}</span> 场历史会议
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting.js'

defineOptions({ name: 'MeetingHistoryPanel' })

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'view-record'])
const keyword = ref('')
const loading = ref(false)
const meetingList = ref([])
let requestVersion = 0

const filteredMeetings = computed(() => {
  const searchText = keyword.value.toLowerCase()
  if (!searchText) return meetingList.value
  return meetingList.value.filter((meeting) => {
    return [meeting.title, meeting.hostName]
      .some((value) => String(value || '').toLowerCase().includes(searchText))
  })
})

const formatDateTime = (value) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : ''

/** 分页读取当前用户的全部会议，再筛选已结束会议。 */
const fetchMeetings = async (showError = false) => {
  const version = ++requestVersion
  const pageSize = 100
  loading.value = true
  try {
    const firstResult = await getMeetingList({ pageNum: 1, pageSize })
    if (firstResult?.code !== 200 || !Array.isArray(firstResult?.data)) {
      throw new Error(firstResult?.msg || '历史会议加载失败')
    }
    if (version !== requestVersion) return
    const total = Number(firstResult?.total)
    const pageCount = Number.isFinite(total) ? Math.ceil(total / pageSize) : 1
    const results = [firstResult]
    for (let pageNum = 2; pageNum <= pageCount; pageNum += 1) {
      const result = await getMeetingList({ pageNum, pageSize })
      if (result?.code !== 200 || !Array.isArray(result?.data)) {
        throw new Error(result?.msg || '历史会议加载失败')
      }
      results.push(result)
      if (version !== requestVersion) return
    }
    meetingList.value = results
      .flatMap((result) => Array.isArray(result?.data) ? result.data : [])
      .filter((meeting) => meeting.status === 'ENDED')
      .sort((a, b) => dayjs(b.endedAt || b.createdAt).valueOf() - dayjs(a.endedAt || a.createdAt).valueOf())
  } catch (error) {
    if (showError) ElMessage.error('历史会议加载失败，请稍后重试')
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => props.visible, (visible) => {
  requestVersion += 1
  if (visible) void fetchMeetings(false)
}, { immediate: true })
</script>

<style lang="less" scoped>
.history-mask { position: fixed; inset: 0; z-index: 2990; background: rgba(34, 37, 39, 0.18); }
.history-panel { position: absolute; top: 0; right: 0; display: flex; width: min(520px, 100vw); height: 100%; flex-direction: column; overflow: hidden; background: #fff; box-shadow: -8px 0 28px rgba(34, 37, 39, 0.12); color: #222527; }
.history-header { display: flex; min-height: 82px; align-items: center; justify-content: space-between; padding: 16px 20px; box-sizing: border-box; border-bottom: 1px solid #E0EEFA; background: linear-gradient(110deg, #F4F9FF, #FFFFFF); }
.history-header span { color: #007BFF; font-size: 12px; }.history-header h3 { margin: 6px 0 0; font-size: 18px; }
.close-btn, .refresh-btn { display: grid; width: 32px; height: 32px; place-items: center; padding: 0; border: 0; border-radius: 6px; background: transparent; color: #617185; cursor: pointer; }
.close-btn:hover, .refresh-btn:hover { background: #E0EEFA; color: #007BFF; }.refresh-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.history-toolbar { display: flex; gap: 10px; padding: 16px 20px; border-bottom: 1px solid #EDF1F5; }.history-toolbar .el-input { flex: 1; }
.history-content { min-height: 200px; flex: 1; overflow-y: auto; padding: 4px 20px 20px; box-sizing: border-box; }
.meeting-list { display: flex; flex-direction: column; }.meeting-row { display: flex; width: 100%; min-height: 82px; align-items: center; gap: 12px; padding: 12px 4px; border: 0; border-bottom: 1px solid #EDF1F5; background: #fff; text-align: left; cursor: pointer; transition: background-color 0.2s ease; }
.meeting-row:hover { background: #F6FAFF; }.meeting-icon { display: grid; width: 40px; height: 40px; flex: 0 0 40px; place-items: center; border-radius: 8px; background: #E0EEFA; color: #007BFF; }.meeting-icon i { font-size: 18px; }
.meeting-main { min-width: 0; flex: 1; }.meeting-main strong { display: block; overflow: hidden; color: #383C41; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }.meeting-meta { display: flex; gap: 12px; margin-top: 8px; color: #9096A2; font-size: 12px; }
.meeting-meta em { overflow: hidden; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }.meeting-meta em:last-child { font-family: 'Alimama FangYuanTi VF', sans-serif; font-style: normal; }
.view-action { display: flex; flex: 0 0 auto; align-items: center; gap: 4px; color: #007BFF; font-size: 12px; }.view-action i { font-size: 14px; }
.empty-state { display: flex; min-height: 420px; align-items: center; justify-content: center; flex-direction: column; color: #A6ACB8; text-align: center; }.empty-state > i { margin-bottom: 12px; color: #DCEDFF; font-size: 44px; }.empty-state strong { margin-bottom: 8px; color: #617185; font-size: 16px; }.empty-state span { font-size: 12px; }
.history-footer { display: flex; min-height: 52px; align-items: center; padding: 0 20px; border-top: 1px solid #EDF1F5; color: #9096A2; font-size: 12px; }.history-footer span { margin: 0 4px; color: #007BFF; font-family: 'Alimama FangYuanTi VF', sans-serif; font-style: normal; }
.history-panel-enter-active, .history-panel-leave-active { transition: background 0.24s ease; }.history-panel-enter-active .history-panel, .history-panel-leave-active .history-panel { transition: transform 0.28s ease; }
.history-panel-enter-from, .history-panel-leave-to { background: transparent; }.history-panel-enter-from .history-panel, .history-panel-leave-to .history-panel { transform: translateX(100%); }
</style>
