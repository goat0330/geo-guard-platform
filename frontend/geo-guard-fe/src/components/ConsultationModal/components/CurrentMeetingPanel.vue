<template>
  <main v-loading="loading" class="current-meeting-panel">
    <h3>当前会议</h3>
    <CurrentMeetingActive
      v-if="meeting"
      :meeting="meeting"
      :participants="participants"
      :transcription-status="transcriptionStatus"
      @enter="emit('enter', $event)"
    />
    <CurrentMeetingEmpty v-else-if="!loading" embedded @create="emit('create')" />
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMeetingDetail, getMeetingList, getMeetingTranscriptionStatus } from '@/api/meeting.js'
import CurrentMeetingActive from './CurrentMeetingActive.vue'
import CurrentMeetingEmpty from './CurrentMeetingEmpty.vue'

defineOptions({ name: 'CurrentMeetingPanel' })

const emit = defineEmits(['create', 'enter'])
const loading = ref(false)
const meeting = ref(null)
const participants = ref([])
const transcriptionStatus = ref({})

const loadCurrentMeeting = async () => {
  loading.value = true
  try {
    const listResult = await getMeetingList({ pageNum: 1, pageSize: 20 })
    const activeMeeting = Array.isArray(listResult?.data)
      ? listResult.data.find((item) => item?.status === 'ACTIVE')
      : null

    if (!activeMeeting?.id) return

    meeting.value = activeMeeting
    const [detailResult, statusResult] = await Promise.allSettled([
      getMeetingDetail(activeMeeting.id),
      getMeetingTranscriptionStatus(activeMeeting.id),
    ])
    if (detailResult.status === 'fulfilled' && detailResult.value?.meeting) {
      meeting.value = detailResult.value.meeting
      participants.value = Array.isArray(detailResult.value.participants) ? detailResult.value.participants : []
    }
    if (statusResult.status === 'fulfilled') transcriptionStatus.value = statusResult.value || {}
  } catch (error) {
    console.error('获取当前会议失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(loadCurrentMeeting)
</script>

<style lang="less" scoped>
.current-meeting-panel {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding: 20px 24px;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  background: #ffffff;

  > h3 {
    margin: 0;
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 18px;
  }
}
</style>
