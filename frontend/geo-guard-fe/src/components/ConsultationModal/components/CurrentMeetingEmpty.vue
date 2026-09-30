<template>
  <main class="meeting-content" :class="{ 'is-embedded': embedded }">
    <h3 v-if="!embedded">当前会议</h3>
    <div class="empty-meeting">
      <img class="empty-meeting__image" :src="emptyMeetingImage" alt="" />
      <p class="empty-meeting__title">当前暂无进行中的会议～</p>
      <p class="empty-meeting__description">点击“发起会议”，邀请专家后进入应急会商室。</p>
      <button class="start-meeting-button" type="button" @click="emit('create')">
        <img :src="createMeetingIcon" alt="" />
        <span>发起会商</span>
      </button>
    </div>
  </main>
</template>

<script setup>
import createMeetingIcon from '@/assets/imgs/chatRoom/faqihuishang.png'
import emptyMeetingImage from '@/assets/imgs/chatRoom/wuhuiyi.png'

defineOptions({ name: 'CurrentMeetingEmpty' })

defineProps({
  embedded: Boolean,
})

const emit = defineEmits(['create'])
</script>

<style lang="less" scoped>
.meeting-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding: 20px 24px;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  background: #ffffff;

  h3 {
    margin: 0;
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 18px;
  }
}

.meeting-content.is-embedded {
  padding: 0;
  border: 0;
}

.empty-meeting {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 18px;
  text-align: center;

  &__image {
    width: 100px;
    height: 110px;
    margin-bottom: 22px;
    object-fit: contain;
  }

  &__title,
  &__description {
    margin: 0;
  }

  &__title {
    margin-bottom: 8px;
    color: #383c41;
    font-size: 16px;
    font-weight: 400;
    line-height: 20px;
  }

  &__description {
    margin-bottom: 16px;
    color: #a6acb8;
    font-size: 14px;
    font-weight: 400;
    line-height: 20px;
  }
}

.start-meeting-button {
  display: inline-flex;
  width: 98px;
  height: 36px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
  color: #ffffff;
  cursor: pointer;
  transition: filter 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 123, 255, 0.35);
    filter: brightness(1.06);
  }

  &:active {
    transform: scale(0.98);
    filter: brightness(0.96);
  }

  img {
    width: 12px;
    height: 12px;
    object-fit: contain;
  }

  span {
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
  }
}
</style>
