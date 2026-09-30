<template>
  <nav class="meeting-nav" aria-label="会议导航">
    <button
      v-for="item in tabs"
      :key="item.value"
      class="meeting-nav__item"
      :class="{ 'is-active': modelValue === item.value }"
      type="button"
      @click="emit('update:modelValue', item.value)"
    >
      <img class="meeting-nav__icon" :src="item.icon" alt="" />
      <span class="meeting-nav__text">
        <strong>{{ item.label }}</strong>
        <small>{{ item.description }}</small>
      </span>
    </button>
  </nav>
</template>

<script setup>
import currentMeetingIcon from '@/assets/imgs/chatRoom/dangqianhuiyi.png'
import createMeetingIcon from '@/assets/imgs/chatRoom/tianjiahuiyi.png'
import historyMeetingIcon from '@/assets/imgs/chatRoom/lishihuiyi.png'

defineOptions({ name: 'ConsultationNavTabs' })

defineProps({
  modelValue: {
    type: String,
    default: 'current',
  },
})

const emit = defineEmits(['update:modelValue'])

const tabs = [
  { value: 'current', label: '当前会议', description: '查看正在进行的会商', icon: currentMeetingIcon },
  { value: 'create', label: '发起会议', description: '邀请专家并进入会商室', icon: createMeetingIcon },
  { value: 'history', label: '历史会议', description: '查看已结束的会商记录', icon: historyMeetingIcon },
]
</script>

<style lang="less" scoped>
.meeting-nav {
  display: flex;
  width: 200px;
  flex: none;
  flex-direction: column;
  gap: 8px;
}

.meeting-nav__item {
  display: flex;
  width: 200px;
  height: 72px;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #ffffff;
  color: #383c41;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;

  &:not(.is-active):hover {
    border-color: #dcedff;
    background: #fbfdff;
  }

  &.is-active {
    border-color: #007bff;
    background: #f1f8ff;
    box-shadow: 0 2px 10px rgba(29, 100, 177, 0.2);
  }
}

.meeting-nav__icon {
  display: block;
  width: 32px;
  height: 32px;
  flex: none;
  object-fit: contain;
}

.meeting-nav__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;

  strong {
    font-size: 14px;
    font-weight: 500;
    line-height: 16px;
  }

  small {
    color: #9096a2;
    font-size: 12px;
    font-weight: 400;
    line-height: 16px;
    white-space: nowrap;
  }
}
</style>
