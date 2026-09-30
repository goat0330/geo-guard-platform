<template>
  <section class="selected-experts">
    <div class="section-title">
      <img :src="pointIcon" alt="" />
      <strong>已选专家列表</strong>
    </div>

    <div class="selected-experts__list">
      <article v-for="user in users" :key="user.userId" class="expert-card">
        <img class="expert-card__avatar" :src="userIcon" alt="" />
        <div class="expert-card__info">
          <p>
            <strong>{{ user.displayName || user.username || '' }}</strong>
            <span v-if="user.title || user.post">（{{ user.title || user.post }}）</span>
          </p>
          <small :title="user.unit || user.orgName || ''">{{ user.unit || user.orgName || '' }}</small>
        </div>
        <button type="button" title="移除" aria-label="移除" @click="emit('remove', user.userId)">
          <img :src="removeIcon" alt="" />
        </button>
      </article>
      <div v-if="!users.length" class="selected-experts__empty">暂未选择参会人员</div>
    </div>
  </section>
</template>

<script setup>
import pointIcon from '@/assets/imgs/chatRoom/point.png'
import removeIcon from '@/assets/imgs/chatRoom/remove.png'
import userIcon from '@/assets/imgs/chatRoom/user.png'

defineOptions({ name: 'SelectedExpertsList' })

defineProps({
  users: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['remove'])
</script>

<style lang="less" scoped>
.selected-experts {
  display: flex;
  width: 268px;
  min-width: 0;
  flex-direction: column;
}

.section-title {
  display: flex;
  height: 20px;
  align-items: center;
  margin-bottom: 12px;

  img {
    width: 8px;
    height: 8px;
    margin-right: 8px;
    object-fit: contain;
  }

  strong {
    color: #383c41;
    font-size: 14px;
    font-weight: 700;
    line-height: 20px;
  }
}

.selected-experts__list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.expert-card {
  display: flex;
  width: 268px;
  height: 64px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #ffffff;
  box-sizing: border-box;

  &__avatar {
    width: 28px;
    height: 28px;
    flex: none;
    margin-right: 8px;
    object-fit: contain;
  }

  &__info {
    min-width: 0;
    flex: 1;

    p {
      display: flex;
      overflow: hidden;
      margin: 0 0 4px;
      color: #383c41;
      font-size: 14px;
      line-height: 16px;
      white-space: nowrap;
    }

    strong,
    span,
    small {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      flex: none;
      font-weight: 500;
    }

    span,
    small {
      color: #9096a2;
      font-size: 12px;
      font-weight: 400;
      line-height: 16px;
    }

    small {
      display: block;
    }
  }

  button {
    width: 20px;
    height: 20px;
    flex: none;
    margin-left: 8px;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: transparent;
    cursor: pointer;
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.08);
    }

    img {
      display: block;
      width: 20px;
      height: 20px;
    }
  }
}

.selected-experts__empty {
  display: flex;
  min-height: 120px;
  align-items: center;
  justify-content: center;
  color: #a6acb8;
  font-size: 14px;
}
</style>
