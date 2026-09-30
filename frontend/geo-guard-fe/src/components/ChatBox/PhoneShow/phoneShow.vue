<template>
  <div class="phone-show-container">
    <div class="phones-wrapper" v-if="displayList.length > 0">
      <!-- 左侧手机：短信预览 -->
      <PhoneFrame>
        <template #title>短信预览</template>
        <div class="sms-content-wrapper">
          <div class="sms-time">{{ formatTime(currentItem.time) }}</div>
          <div class="sms-bubble">
            {{ currentItem.smsContent || '暂无' }}
          </div>
          <div style="height: 1rem"></div>
        </div>
      </PhoneFrame>
    </div>

    <!-- 底部翻页 -->
    <div class="pagination" v-if="displayList.length > 0">
      <div 
        class="page-btn left-btn" 
        :class="{ disabled: currentIndex === 0 }" 
        @click="prevPage"
      >
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none"><path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </div>
      <div class="page-info">
        <span class="current">{{ currentIndex + 1 }}</span>/<span class="total">{{ displayList.length }}</span>
      </div>
      <div 
        class="page-btn right-btn" 
        :class="{ disabled: currentIndex === displayList.length - 1 }" 
        @click="nextPage"
      >
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none"><path d="M9 18L15 12L9 6" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import PhoneFrame from './PhoneFrame.vue';
import dayjs from 'dayjs'

const props = defineProps({
  dataList: {
    type: Array,
    default: () => []
  }
});

/**
 * displayList 数据项结构说明：
 * {
 *   smsContent: string, // 短信内容
 *   time: string        // 时间
 * }
 */
const displayList = computed(() => props.dataList || []);

const currentIndex = ref(0);

const currentItem = computed(() => {
  return displayList.value[currentIndex.value] || {};
});

const formatTime = (time) => {
  if (!time) return '--';
  const target = dayjs(time);
  if (!target.isValid()) return '--';
  if (target.isSame(dayjs(), 'day')) {
    return `今天 ${target.format('HH:mm')}`;
  }
  return target.format('YYYY-MM-DD HH:mm');
};

const prevPage = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
  }
};

const nextPage = () => {
  if (currentIndex.value < displayList.value.length - 1) {
    currentIndex.value++;
  }
};
</script>

<style scoped lang="less">
.phone-show-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 20px;
  user-select: none;
}

.phones-wrapper {
  display: flex;
  justify-content: center;
  gap: 20px;
  background-color: #f4f6f9;
  padding: 30px 30px 0;
  border-radius: 20px;
  margin-bottom: 12px;
  width: 100%;
}

/* 短信预览样式 */
.sms-content-wrapper {
  background-color: #fff;
  height: 100%;
  box-sizing: border-box;
  padding-bottom: 10px;
  margin-top: 5px;
}

.sms-time {
  text-align: center;
  color: #999;
  margin-bottom: 5px;
  font-size: 10px;
}

.sms-bubble {
  background-color: #f2f4f7;
  border-radius: 15px;
  padding: 10px;
  width: 100%;
  font-size: 12px;
  line-height: 16px;
}

/* 巡查任务样式 */
.task-content-wrapper {
  background-color: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.task-field {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: flex-start;
  
  .label {
    width: 90px;
    font-size: 12px;
    color: #222529;
    font-weight: 800;
    flex-shrink: 0;
    margin-right: 10px;
  }
  
  .value {
    flex: 1;
    font-size: 12px;
    color: #878898;
    line-height: 16px;
  }
}

.task-footer {
  margin-top: auto;
  padding: 20px 0;
  text-align: right;
  
  .char-count {
    font-size: 12px;
    color: #b0b0b0;
  }
}

/* 底部翻页样式 */
.pagination {
  display: flex;
  align-items: center;
  gap: 20px;
  
  .page-btn {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s;
    color: #fff;
    
    &.left-btn {
      background-color: #1890ff;
    }
    
    &.right-btn {
      background-color: #1890ff;
    }
    
    &.disabled {
      background-color: #c0c4cc;
      cursor: not-allowed;
      opacity: 0.8;
    }
  }
  
  .page-info {
    font-size: 14px;
    font-weight: 500;
    color: #000000;
    letter-spacing: 1px;
    
    .current {
      font-size: 14px;
    }
    .total {
      font-size: 14px;
      color: #000000;
    }
  }
}
</style>