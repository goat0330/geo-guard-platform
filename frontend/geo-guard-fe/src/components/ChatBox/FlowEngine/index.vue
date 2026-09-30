<template>
  <div
    class="flow-engine"
    :style="[
      { 'margin-bottom': `${gap}px` }
    ]"
  >
    <div
      class="engine-block"
      v-for="(item, index) in listData"
      :key="index"
      v-show="index < visibleTitleCount"
    >
      <div
        class="title-engine"
        :class="{ 'is-expanded': index < visibleContentCount && isContentExpanded(item) }"
        @click="toggleContent(item, index)"
      >
        <Loading v-if="item.loading" class-name="loading" :size="14" color-start="#3561FA" color-mid="rgba(53, 97, 250, 0.6)" color-end="#EEF8FF" />
        <img v-else class="header-icon" src="@/assets/imgs/chatBox/process/right.svg?url" />
        <div class="title-text">{{ item.title }}</div>
        <img
          v-if="index < visibleContentCount && hasContent(item)"
          class="control-icon"
          :class="{ 'control-icon-active': isContentExpanded(item) }"
          src="@/assets/imgs/chatBox/process/down.svg?url"
        />
      </div>
      <Transition name="flow-content" @after-enter="scrollToLatest">
        <div v-if="index < visibleContentCount && isContentExpanded(item)" class="content-engine">
          <div v-show="item.contentHtml" class="md-box-show" v-dompurify-html="item.contentHtml"></div>
          <div v-show="item.contentText" style="white-space: pre-wrap">{{ item.contentText }}</div>
          <div v-show="item.imgList?.length" class="img-list">
            <div v-for="(imgUrl, imgIndex) in item.imgList" :key="imgIndex" class="img-item">
              <img :src="imgUrl" />
              <div class="img-operate">
                <div class="img-operate-item" @click.stop="seeDetail(item.imgList, imgIndex, item.type)">
                  <i class="iconfont icon-fangda"></i>
                  查看
                </div>
                <div class="img-operate-item" @click.stop="download(imgUrl, imgIndex)">
                  <i class="iconfont icon-xaizai"></i>
                  下载
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <el-image-viewer
      v-if="showPreview"
      :url-list="photoShowList"
      show-progress
      :initial-index="showIndex"
      @close="showPreview = false"
    />
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, computed } from 'vue'
import Loading from '@/components/Icon/Loading/index.vue'
import { sleep } from '@/utils/index.js'
import { eventBus, EventKey } from '@/utils/eventBus.js'

const props = defineProps({
  // 传入的数据格式为：
  // [{
  //   title: '标题',
  //   loading: true, // 是否显示加载状态
  //   contentHtml: 'html内容', // html内容（可选）
  //   contentText: '纯文本内容', // 纯文本内容（可选，如果提供html内容则不显示）
  //   imgList: ['图片url'], // 图片列表（可选）
  //   type: '类型', // 类型（可选，查看图片时用到）
  //   callback: () => {} // 回调函数（可选，在当前步骤展示完后调用）
  // }]
  data: {
    type: Array,
    default: () => []
  },
  gap: {
    type: Number,
    default: 20
  },
  // 第一个是正文延迟， 第二个是标题延迟 (ms)
  waitTime: {
    type: Array,
    default: () => [800, 200]
  },
  scrollToBottom: {
    type: Function,
    default: () => {}
  },
  // 是否使用本地图片查看
  useLocalShow: {
    type: Boolean,
    default: false
  },
  // 流程结束的回调
  finishFuc: {
    type: Function,
    default: () => {}
  }
})

// 直接使用 computed 保持引用和深层响应性
const listData = computed(() => props.data || [])

// 标题显示后，等待多久再显示正文（毫秒）
const TITLE_DELAY_MS = props.waitTime?.[0] || 0
// 正文显示后，等待多久再进入下一条（毫秒）
const CONTENT_DELAY_MS = props.waitTime?.[1] || 0

// 控制当前已展示到第几个标题/正文
const visibleTitleCount = ref(0)
const visibleContentCount = ref(0)

let cancelled = false

const hasContent = (item) => {
  return !!(item?.contentHtml || item?.contentText || item?.imgList?.length)
}

const isContentExpanded = (item) => {
  return item?.showContent !== false
}

const toggleContent = (item, index) => {
  if (index >= visibleContentCount.value || !hasContent(item)) return
  item.showContent = !isContentExpanded(item)
}

/**
 * 滚动到最新流程内容底部。
 * @returns {Promise<void>}
 */
const scrollToLatest = async () => {
  await nextTick()
  if (cancelled) return
  await props.scrollToBottom()
}

// 按“标题 -> 正文 -> 下一条”的顺序逐条展示
const runFlow = async () => {
  for (let i = 0; i < listData.value.length; i += 1) {
    if (cancelled) return

    if (listData.value[i]) {
      listData.value[i].loading = true
    }

    visibleTitleCount.value = i + 1
    await scrollToLatest()
    await sleep(TITLE_DELAY_MS)
    if (cancelled) return

    visibleContentCount.value = i + 1
    if (listData.value[i]) {
      listData.value[i].loading = false
      if (listData.value[i].showContent === undefined) {
        listData.value[i].showContent = true
      }
    }
    // 正文首次挂载后先滚动；展开动画结束后会由 after-enter 再次对齐底部。
    await scrollToLatest()

    if (listData.value[i]?.callback && typeof listData.value[i].callback === 'function') {
      if (!listData.value[i]._hasRun) {
        listData.value[i]._hasRun = true
        await Promise.all([sleep(CONTENT_DELAY_MS), listData.value[i].callback()])
      } else {
        await sleep(CONTENT_DELAY_MS)
      }
    } else {
      await sleep(CONTENT_DELAY_MS)
    }

    if (cancelled) return
  }

  // 通知结束
  props?.finishFuc?.()
}

const showPreview = ref(false)
const photoShowList = ref([])
const showIndex = ref(0)

const seeDetail = (imgList, index, type) => {
  if (props.useLocalShow) {
    photoShowList.value = imgList
    showIndex.value = index
    showPreview.value = true
  } else {
    eventBus.emit(EventKey.SHOW_ONE_GRAPH_WINDOW, type)
  }
}

const getFileNameFromUrl = (url, index) => {
  const cleanUrl = (url || '').split('?')[0]
  const rawName = cleanUrl.split('/').pop()
  return rawName || `flow-image-${index + 1}.png`
}

const download = async (imgUrl, index) => {
  const fileName = getFileNameFromUrl(imgUrl, index)
  try {
    const response = await fetch(imgUrl)
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  } catch (error) {
    // 跨域或网络异常时兜底：新窗口打开原图，用户可手动保存
    window.open(imgUrl, '_blank')
  }
}

onMounted(() => {
  runFlow()
})

// 组件卸载时中断后续异步流程，避免更新已卸载组件
onUnmounted(() => {
  cancelled = true
})

</script>

<style scoped lang="less">
.flow-engine {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .engine-block {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .title-engine {
      width: 380px;
      height: 42px;
      display: flex;
      align-items: center;
      gap: 10px;
      border-radius: 12px;
      border: 1px solid #E7E7EA;
      background: #FBFBFF;
      padding: 0 20px;
      cursor: pointer;

      .loading {
        position: relative;
        top: 6px;
      }

      .header-icon {
        width: 16px;
        height: 16px;
      }

      .title-text {
        flex: 1;
        min-width: 0;
        font-size: 14px;
        color: #3561fa;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .control-icon {
        width: 12px;
        height: 12px;
        flex-shrink: 0;
        cursor: pointer;
        transition: transform 0.2s ease;
      }

      .control-icon-active {
        transform: rotate(180deg);
      }

      &:hover,
      &.is-expanded {
        .title-text {
          color: #3561FA;
        }
      }
    }

    .content-engine {
      color: #878898;
      text-align: justify;
      font-size: 14px;
      padding: 0 20px;
      line-height: 22px;

      .img-list {
        display: grid;
        grid-template-columns: repeat(2, 177px);
        margin-top: 5px;
        grid-gap: 10px;

        .img-item {
          width: 177px;
          height: 90px;
          border-radius: 4px;
          border: 1px solid #D4D8DD;
          position: relative;
          cursor: pointer;

          img {
            width: 100%;
            height: 100%;
          }

          .img-operate {
            position: absolute;
            bottom: 4px;
            left: 50%;
            transform: translateX(-50%);
            display: none;
            align-items: center;
            gap: 0;
            width: 112px;
            height: 26px;
            border-radius: 4px;
            background: #949494b3;
            backdrop-filter: blur(2px);
            padding: 3px 4px;

            .img-operate-item {
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 2px;
              width: 50px;
              height: 20px;
              border-radius: 4px;
              color: #FFFFFF;
              font-size: 12px;
              cursor: pointer;

              i {
                font-size: 12px;
              }

              &:hover {
                background: #0000004d;
              }
            }
          }

          &:hover {
            .img-operate {
              display: flex;
            }
          }
        }
      }
    }
  }
}

.flow-content-enter-active,
.flow-content-leave-active {
  transition: all 0.2s ease;
}

.flow-content-enter-from,
.flow-content-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
