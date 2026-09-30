<template>
  <div class="history-list-box" :class="{ 'history-list-box-active': show }">
    <div class="history-header">
      <img src="@/assets/imgs/chatBox/history/history.png" />
      <div style="flex: 1">历史预警</div>
      <img src="@/assets/imgs/chatBox/history/delete.png" style="cursor: pointer" @click="closeHistory" />
    </div>
    <!-- <template v-if="showType === 1"> -->
    <DynamicScroller :items="historyList" :min-item-size="60" class="history-list" @scroll.native="onScroll">
      <template v-slot="{ item, index, active }">
        <DynamicScrollerItem
          :item="item"
          :active="active"
          :size-dependencies="[item.content]"
          :data-index="index"
          @click="selectDetail(item)"
        >
          <div v-if="mode === 'chat'" class="history-item" :class="selectedItemId === item?.id ? 'is-active' : ''">
            <div class="history-item-header">
              <div class="item-title">{{ removeAfterAt(item?.title) }}</div>
            </div>
            <div class="item-content">{{ item?.message }}</div>
          </div>
          <div class="split"></div>
        </DynamicScrollerItem>
      </template>
      <template #after>
        <div v-if="loading && !isEnd" class="loading">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          加载中
        </div>
        <div v-if="isEnd" class="loading">没有更多数据了~</div>
      </template>
    </DynamicScroller>
    <!-- </template> -->
    <!-- <template v-else>
      <HistoryDetail :detail-data="historyDetail" />
    </template> -->
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getHistoryListOwn, getChatMsg } from '@/api/chat.js'
import { getWeatherInfoHistory, getWeatherInfo } from '@/api/common.js'
import { CHAT_TYPE } from '@/utils/enum.js'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { Loading } from '@element-plus/icons-vue'
import { debounce } from 'lodash-es'
// import HistoryDetail from './historyDetail.vue'
import { removeAfterAt, removeAtBlock } from '@/utils/index.js'
import dayjs from 'dayjs'
import { eventBus, EventKey } from '../../../utils/eventBus'

const props = defineProps({
  mode: {
    type: String,
    default: 'chat',
  },
})

const showType = ref(1)
const historyList = ref([])
const selectedItemId = ref(null)

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
    let data = await getList()
    historyList.value = historyList.value.concat(data)
  }
}, 200)

const page = ref({
  pageNum: 1,
  pageSize: 20,
  total: 0,
})

const refresh = async (options = {}) => {
  page.value.pageNum = 1
  historyList.value = await getList()
  if (options.emitSelected) {
    emitSelectedHistoryRecord()
  }
}

const getList = async () => {
  const params = {
    pageNum: page.value.pageNum,
    pageSize: page.value.pageSize,
    orderByColumn: 'validStartDate',
    isAsc: 'desc',
  }
  try {
    loading.value = true
    const res = await getWeatherInfoHistory(params)
    page.value.total = res.total || 0
    isEnd.value = page.value.total <= page.value.pageNum * page.value.pageSize
    const today = dayjs().startOf('day')
    let todayIndex = -1
    let result = (res.data || []).map((v, index) => {
      const validStartDate = dayjs(v?.validStartDate)
      const isToday = validStartDate.isSame(today, 'day')
      if (todayIndex === -1 && isToday) {
        todayIndex = index
      }
      return {
        ...v,
        isActive: isToday,
        title: validStartDate.format('YYYY-MM-DD') + ' 气象预警',
      }
    })
    if (page.value.pageNum === 1) {
      if (todayIndex > -1) {
        selectedItemId.value = result[todayIndex].id
      } else {
        const insertIndex = result.findIndex((item) => {
          const validStartDate = dayjs(item?.validStartDate)
          return validStartDate.isValid() && validStartDate.startOf('day').isBefore(today)
        })
        selectedItemId.value = 'custom'
        result.splice(insertIndex === -1 ? result.length : insertIndex, 0, {
          title: today.format('YYYY-MM-DD') + ' 气象预警',
          id: 'custom',
          message: '无预警信息~',
          isActive: true,
        })
      }
    }
    return result
  } finally {
    loading.value = false
  }
}

const show = ref(false)
const openDialog = async () => {
  show.value = true
  await refresh({ emitSelected: true })
}
const closeDialog = () => {
  show.value = false
}
const refreshSelectedHistory = async () => {
  await refresh({ emitSelected: true })
}

const closeHistory = () => {
  if (showType.value === 1) {
    closeDialog()
    eventBus.emit(EventKey.OPERATE_WEATHER_WARNING_HISTORY, false)
  } else {
    showType.value = 1
  }
}

const historyDetail = ref({})
const emitSelectedHistoryRecord = () => {
  const selectedItem = historyList.value.find((item) => item?.id === selectedItemId.value)
  if (selectedItem) {
    eventBus.emit(EventKey.SELECT_HISTORY_RECORD, selectedItem)
  }
}

const selectDetail = async (item) => {
  selectedItemId.value = item.id
  console.log('--selectDetail--', item)
  eventBus.emit(EventKey.SELECT_HISTORY_RECORD, item)
  // const res = await getChatMsg({ historyId: item?.id })
  // historyDetail.value = {
  //   id: item?.id,
  //   data: res,
  // }
  // showType.value = 2
}

onMounted(() => {
  refresh()
})

defineExpose({
  openDialog,
  closeDialog,
  refreshSelectedHistory,
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

  .history-list {
    flex: 1;
    height: 100%;
    overflow: auto;
    display: flex;
    flex-direction: column;
    padding-right: 8px;

    .history-item {
      padding: 20px;
      border-radius: 10px;
      border: 1px solid #d4d8dd;
      cursor: pointer;
      &:hover {
        border-color: #3561FA;
      }
      &.is-active {
        border-color: #3561FA;
        box-shadow: 0 4px 6px 0 #3561fa33;
        .item-title {
          color: #3561FA !important;
        }
      }

      .history-item-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        color: #222529;
        font-size: 14px;
        font-weight: 500;
        //margin-bottom: 14px;

        .item-title {
          color: #222529;
          font-size: 14px;
          font-weight: 500;
        }
        .date-time {
          font-size: 12px;
          color: #878898;
        }
      }

      .item-content {
        margin-top: 14px;
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
