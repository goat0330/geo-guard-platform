<template>
  <section class="active-meeting">
    <article class="meeting-card">
      <div class="meeting-date">
        <strong>{{ dateLabel }}</strong>
        <span class="num-font">{{ dateValue }}</span>
      </div>
      <div class="card-divider"></div>
      <div class="meeting-detail">
        <h4 :title="meeting.title || ''">{{ meeting.title || '' }}</h4>
        <p class="meeting-time num-font">{{ timeLabel }}</p>
        <p>
          <em>发起人</em><span>{{ meeting.hostName || '' }}</span>
        </p>
        <p>
          <em>参会人</em><span>{{ participantNames }}</span
          ><small class="num-font">共{{ activeParticipants.length }}人</small>
        </p>
      </div>
      <div class="meeting-action">
        <div class="meeting-status">
          <i><b></b><b></b><b></b><b></b><b></b></i>进行中
        </div>
        <button type="button" @click="emit('enter', meeting)">进入会商室<img :src="enterIcon" alt="" /></button>
      </div>
    </article>
    <div v-if="transcriptionStatus.enabled !== false" class="summary-banner">
      <img :src="summaryIcon" alt="" />
      <strong>智能纪要记录中</strong>
      <span>（已开启实时记录，会后生成纪要草稿并由人工确认）</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import enterIcon from '@/assets/imgs/chatRoom/enter.png'
import summaryIcon from '@/assets/imgs/chatRoom/zhineng.png'

defineOptions({ name: 'CurrentMeetingActive' })

const props = defineProps({
  meeting: { type: Object, required: true },
  participants: { type: Array, default: () => [] },
  transcriptionStatus: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['enter'])

const startTime = computed(() => dayjs(props.meeting.createdAt))
const dateLabel = computed(() => (startTime.value.isSame(dayjs(), 'day') ? '今天' : startTime.value.format('M月D日')))
const dateValue = computed(() => (startTime.value.isValid() ? startTime.value.format('M月D日') : ''))
const timeLabel = computed(() => {
  if (!startTime.value.isValid()) return ''
  const minutes = Math.max(0, dayjs().diff(startTime.value, 'minute'))
  return `${startTime.value.format('HH:mm')} 开始 · 已进行${minutes}分钟`
})
const activeParticipants = computed(() => props.participants.filter((item) => item?.status === 'ACTIVE'))
const participantNames = computed(() =>
  activeParticipants.value
    .map((item) => item?.name)
    .filter(Boolean)
    .join('、'),
)
</script>

<style lang="less" scoped>
.active-meeting {
  position: relative;
  margin-top: 20px;
}

.meeting-card {
  position: relative;
  z-index: 1;
  display: flex;
  height: 148px;
  padding: 18px 20px;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #ffffff;
  box-sizing: border-box;
}
.meeting-date {
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
    line-height: 16px;
  }
}
.card-divider {
  width: 1px;
  height: 102px;
  flex: none;
  background: #dee2ec;
}
.meeting-detail {
  min-width: 0;
  flex: 1;
  padding-left: 16px;
  h4 {
    overflow: hidden;
    margin: 0;
    color: #383c41;
    font-size: 18px;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  p {
    display: flex;
    margin: 4px 0 0;
    color: #617185;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
  }
  .meeting-time {
    margin-bottom: 8px;
    color: #9096a2;
    font-size: 16px;
    font-weight: 400;
    line-height: 24px;
  }
  em {
    width: 42px;
    flex: none;
    color: #a6acb8;
    font-style: normal;
    margin-right: 8px;
  }
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  small {
    margin-left: 12px;
    color: #a6acb8;
    font-size: 14px;
    white-space: nowrap;
  }
}
.meeting-action {
  display: flex;
  width: 120px;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  .meeting-status {
    display: flex;
    align-items: center;
    color: #44b699;
    font-size: 14px;
    font-weight: 500;
  }
  i {
    display: flex;
    height: 20px;
    align-items: center;
    gap: 2px;
    margin-right: 4px;
  }
  b {
    width: 2px;
    border-radius: 2px;
    background: #44b699;
    animation: sound-wave 1.2s ease-in-out infinite;
  }
  b:nth-child(1),
  b:nth-child(5) {
    height: 4px;
  }
  b:nth-child(2),
  b:nth-child(4) {
    height: 8px;
    animation-delay: 0.15s;
  }
  b:nth-child(3) {
    height: 14px;
    animation-delay: 0.3s;
  }
  button {
    display: flex;
    width: 100px;
    height: 32px;
    align-items: center;
    justify-content: center;
    gap: 4px;
    border: 0;
    border-radius: 8px;
    background: #007bff;
    color: #fff;
    cursor: pointer;
    font-size: 12px;
  }
  img {
    width: 12px;
    height: 12px;
    object-fit: contain;
  }
}
.summary-banner {
  position: relative;
  z-index: 0;
  display: flex;
  height: 52px;
  margin-top: -14px;
  padding: 14px 20px 0;
  border-radius: 0 0 12px 12px;
  background: #f5fbff;
  align-items: center;
  box-sizing: border-box;

  img {
    width: 14px;
    height: 14px;
  }

  strong {
    margin-left: 8px;
    background: linear-gradient(90deg, #007bff, #00b2ff);
    -webkit-background-clip: text;
    color: transparent;
    font-size: 12px;
  }

  span {
    margin-left: 4px;
    color: #9096a2;
    font-size: 12px;
  }
}
.num-font {
  font-style: normal;
}
@keyframes sound-wave {
  50% {
    transform: scaleY(0.55);
  }
}
</style>
