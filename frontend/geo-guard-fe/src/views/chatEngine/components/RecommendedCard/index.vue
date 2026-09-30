<template>
  <div class="recommend-card" @click="goUrl" :class="data.disabled ? 'is-disable' : ''">
    <div class="header">
      {{ data?.title }}
      <img class="arrow" src="@/assets/imgs/chatEngine/card-right.png" />
      <img class="arrow-active" src="@/assets/imgs/chatEngine/card-right-hover.png" />
    </div>
    <!-- <div class="desc">{{ data?.desc }}</div> -->
    <div class="message-card">
      <!-- <img :src="data.icon" /> -->
      <div class="message-card-content">
        <!-- <div class="message-card-content-title">{{ data?.cardTitle }}</div> -->
        <div class="message-card-content-desc">{{ data?.cardDesc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})

const goUrl = () => {
  console.log('---goUrl---')
  if (props.data.disabled) return
  router.push({
    path: '/chat-engine/chatting',
    query: {
      type: props.data.type,
      _refresh: Date.now(),
    },
  })
}
</script>

<style scoped lang="less">
.recommend-card {
  padding: 20px;
  width: 300px;
  // height: 137px;
  // background-image: url('@/assets/imgs/chatEngine/recommend-bg.png');
  background: linear-gradient(97deg, #f5f8ff 5.35%, #f9f8ff 97.33%);
  background-size: cover;
  background-repeat: no-repeat;
  cursor: pointer;
  border-radius: 12px;

  .header {
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #222529;
    margin-bottom: 6px;
    font-weight: 800;

    img {
      width: 20px;
      height: 20px;
    }
    .arrow {
      display: block;
    }
    .arrow-active {
      display: none;
    }
  }
  &:hover {
    background: #fff;
    box-shadow: 0 4px 16px 0 #13224e1a;
    & .header {
      color: var(--el-color-primary);
      .arrow-active {
        display: block;
      }
      .arrow {
        display: none;
      }
    }
  }

  .message-card {
    width: 260px;
    border-radius: 8px;
    display: flex;
    gap: 11px;
    img {
      width: 30px;
      height: 30px;
    }
    .message-card-content {
      .message-card-content-title {
        font-size: 12px;
        font-weight: 800;
        color: #222529;
        margin-bottom: 1px;
      }
      .message-card-content-desc {
        font-size: 14px;
        color: #878898;
        line-height: 24px;
      }
    }
  }
}
.is-disable {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
