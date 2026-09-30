<template>
  <div class="history-list-box" :class="{ 'history-list-box-active': show }">
    <div class="history-header">
      <img src="@/assets/imgs/chatBox/history/history.png" />
      <div style="flex: 1">
        {{ showType == 1 ? '历史对话' : removeAtBlock(historyDetail?.data?.[0]?.query) }}
      </div>
      <img src="@/assets/imgs/chatBox/history/delete.png" style="cursor: pointer" @click="closeHistory" />
    </div>
    <template v-if="showType === 1">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索历史会话，支持标题、问题、答案、关键字"
          clearable
          @input="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="history-list" @scroll="onScroll">
        <div v-if="groupedHistoryList.last7Days.length > 0" class="group-title">近7天</div>
        <div v-for="(item, index) in groupedHistoryList.last7Days" :key="'7-' + index" class="history-item-wrapper" @click="selectDetail(item)">
          <div v-if="mode === 'chat'" class="history-item">
            <div class="history-item-header">
              <div>{{ removeAfterAt(item?.title) }}</div>
              <div class="date-time">{{ item?.createDate }}</div>
            </div>
            <div class="item-content">{{ getCleanAnswer(item?.lastDetail?.answer || '') }}</div>
          </div>
          <div class="split"></div>
        </div>

        <div v-if="groupedHistoryList.last30Days.length > 0" class="group-title">近30天</div>
        <div v-for="(item, index) in groupedHistoryList.last30Days" :key="'30-' + index" class="history-item-wrapper" @click="selectDetail(item)">
          <div v-if="mode === 'chat'" class="history-item">
            <div class="history-item-header">
              <div>{{ removeAfterAt(item?.title) }}</div>
              <div class="date-time">{{ item?.createDate }}</div>
            </div>
            <div class="item-content">{{ getCleanAnswer(item?.lastDetail?.answer || '') }}</div>
          </div>
          <div class="split"></div>
        </div>
        
        <div v-if="loading && !isEnd" class="loading">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          加载中
        </div>
        <div v-if="isEnd" class="loading">没有更多数据了~</div>
      </div>
    </template>
    <template v-else>
      <HistoryDetail :detail-data="historyDetail" />
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { getHistoryListOwn, getChatMsg } from '@/api/chat.js'
import { CHAT_TYPE } from '@/utils/enum.js'
import { Loading, Search } from '@element-plus/icons-vue'
import { debounce } from 'lodash-es'
import HistoryDetail from './historyDetail.vue'
import { removeAfterAt, removeAtBlock } from '@/utils/index.js'
import dayjs from 'dayjs'
import { useRoute } from 'vue-router'

const props = defineProps({
  mode: {
    type: String,
    default: 'chat',
  },
})

const showType = ref(1)
const historyList = ref([])
const searchQuery = ref('')

const threshold = 1
const isEnd = ref(false)
const loading = ref(false)
const onScroll = (e) => {
  const el = e.target
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - threshold) {
    loadMore()
  }
}

const loadMore = debounce(async () => {
  if (!isEnd.value) {
    page.value.pageNum += 1
    const data = await getList()
    historyList.value = historyList.value.concat(data)
  }
}, 200)

const page = ref({
  pageNum: 1,
  pageSize: 20,
  total: 0,
})

const refresh = async () => {
  page.value.pageNum = 1
  historyList.value = await getList()
}

const onSearch = debounce(() => {
  refresh()
}, 300)

const route = useRoute()
const getAgentType = () => {
  const path = route.path
  if (path.includes('defense-response')) {
    return CHAT_TYPE.DEFENSE
  } else if (path.includes('disaster-response') || path.includes('emergency-response-detail')) {
    return CHAT_TYPE.DISPOSAL
  } else if (path.includes('prediction')){
    return CHAT_TYPE.PREDICT
  } else {
    return CHAT_TYPE.ASSISTANT
  }
}

const getList = async () => {
  const params = {
    pageNum: page.value.pageNum,
    pageSize: page.value.pageSize,
    type: getAgentType(),
    orderByColumn: 'updateDate',
    isAsc: 'desc',
    title: searchQuery.value || undefined,
  }
  try {
    loading.value = true
    const res = await getHistoryListOwn(params)
    page.value.total = res.total || 0
    isEnd.value = page.value.total <= page.value.pageNum * page.value.pageSize
    return res.data || []
  } finally {
    loading.value = false
  }
}

const show = ref(false)
const openDialog = () => {
  show.value = true
  refresh()
}
const closeDialog = () => {
  show.value = false
}

const closeHistory = () => {
  if (showType.value === 1) {
    closeDialog()
  } else {
    showType.value = 1
  }
}

const historyDetail = ref({})
const selectDetail = async (item) => {
  const res = await getChatMsg({ historyId: item?.id })
  historyDetail.value = {
    id: item?.id,
    data: res,
  }
  showType.value = 2
}

const getCleanAnswer = (answer) => {
  if (!answer) return ''
  // Remove <think>...</think> blocks if present
  let cleanStr = answer.replace(/<think>[\s\S]*?<\/think>/g, '')
  cleanStr = cleanStr.trim()

  const appTypeBlockMatch = cleanStr.match(/^\(@apptype\)\[[\s\S]*?\]/)
  if (appTypeBlockMatch) {
    return '系统流程类型' + cleanStr.substring(appTypeBlockMatch[0].length)
  }
  
  if (cleanStr.startsWith('{')) {
    let braceCount = 0
    let matchEndIndex = -1
    for (let i = 0; i < cleanStr.length; i++) {
      if (cleanStr[i] === '{') braceCount++
      else if (cleanStr[i] === '}') braceCount--
      
      if (braceCount === 0) {
        matchEndIndex = i
        break
      }
    }
    
    if (matchEndIndex !== -1) {
      cleanStr = '系统流程类型' + cleanStr.substring(matchEndIndex + 1)
    }
  }
  
  return cleanStr || ''
}

const groupedHistoryList = computed(() => {
  const now = dayjs()
  const result = {
    last7Days: [],
    last30Days: []
  }
  
  historyList.value.forEach(item => {
    if (!item.updateDate) return
    const updateTime = dayjs(item.updateDate)
    const diffDays = now.diff(updateTime, 'day')
    
    if (diffDays <= 7) {
      result.last7Days.push(item)
    } else if (diffDays <= 30) {
      result.last30Days.push(item)
    }
  })
  
  return result
})

onMounted(() => {
  refresh()
})

defineExpose({
  openDialog,
  closeDialog,
})
</script>

<style scoped lang="less">
.history-list-box {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 100;
  background: #fff;
  padding: 20px;
  display: flex;
  flex-direction: column;
  transform: translateY(100%);
  transition: transform 0.3s ease-in-out;

  .history-header {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 6px;
    color: #222529;
    font-weight: 800;
    flex-shrink: 0;

    img {
      width: 20px;
      height: 20px;
    }
  }

  .search-box {
    margin-bottom: 16px;
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px #d4d8dd inset;
      border-radius: 6px;
      height: 36px;
    }
    :deep(.el-input__inner::placeholder) {
      font-size: 14px;
      color: #c3c9cf;
    }
  }

  .history-list {
    flex: 1;
    height: 100%;
    overflow: auto;
    display: flex;
    flex-direction: column;
    padding-right: 8px;
    
    .group-title {
      font-size: 14px;
      color: #878898;
      margin-bottom: 12px;
      font-weight: 800;
    }

    .history-item {
      padding: 20px;
      border-radius: 10px;
      border: 1px solid #d4d8dd;
      cursor: pointer;

      .history-item-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        color: #222529;
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 14px;

        .date-time {
          font-size: 12px;
          color: #878898;
        }
      }

      .item-content {
        font-size: 12px;
        color: #506073;
        line-height: 18px;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        text-align: justify;
      }
    }

    .split {
      height: 12px;
    }
  }

  .loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 8px;
  }
}

.history-list-box-active {
  transform: translateY(0);
}
</style>
