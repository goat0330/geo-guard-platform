<template>
  <footer class="create-meeting-footer">
    <div class="divider"></div>
    <div class="footer-actions">
      <span class="selected-count">
        <img :src="rightIcon" alt="" />
        已选择<em>{{ count }}</em>人
      </span>
      <button type="button" :disabled="disabled" @click="emit('submit')">
        发起会商并进入会商室
      </button>
    </div>
  </footer>
</template>

<script setup>
import rightIcon from '@/assets/imgs/chatRoom/right.png'

defineOptions({ name: 'CreateMeetingFooter' })

defineProps({
  count: {
    type: Number,
    default: 0,
  },
  disabled: Boolean,
})

const emit = defineEmits(['submit'])
</script>

<style lang="less" scoped>
.create-meeting-footer {
  flex: none;
}

.divider {
  width: 100%;
  height: 1px;
  margin: 16px 0 14px;
  background: #e4eaef;
}

.footer-actions {
  display: flex;
  height: 32px;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;

  > button {
    width: 144px;
    height: 32px;
    border: 0;
    border-radius: 8px;
    background: #007bff;
    color: #ffffff;
    cursor: pointer;
    font-size: 12px;
    line-height: 20px;
    transition: filter 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

    &:hover:not(:disabled) {
      box-shadow: 0 4px 12px rgba(0, 123, 255, 0.35);
      filter: brightness(1.06);
    }

    &:active:not(:disabled) {
      transform: scale(0.98);
      filter: brightness(0.96);
    }

    &:disabled {
      background: #a6acb8;
      cursor: not-allowed;
    }
  }
}

.selected-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #44b699;
  font-size: 12px;
  line-height: 20px;

  img {
    width: 12px;
    height: 12px;
    object-fit: contain;
  }

  em {
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-style: normal;
  }
}
</style>
