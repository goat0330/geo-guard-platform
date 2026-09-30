<template>
  <div class="history-drawer-host">
    <Transition name="history-slide" @after-leave="handleAfterLeave">
      <div v-show="modelValue" class="history-drawer-panel">
        <div class="history-header">
          <div class="history-title">
            <el-icon><Clock /></el-icon>
            历史对话
          </div>
          <el-icon class="close-btn" @click="closeDrawer"><Close /></el-icon>
        </div>

        <div ref="scrollRootRef" class="history-list" @scroll="handleScroll">
          <GroupChatHistory
            ref="GroupChatHistoryRef"
            :load-function="getList"
            :has-more="hasMore"
            :threshold="120"
            @update:items="handleItemsUpdate"
            @load-success="pageNum++"
          >
            <template #content="{ items }">
              <div v-for="(item, key) in items" :key="item.id" class="each-chat" @click.stop="getChatDetail(item)">
                <div class="each-content">
                  <div v-if="!item.isModifying" class="chat-title" :title="item.title">{{ item.title || '未命名对话' }}</div>
                  <div v-else class="modify-box" @click.stop>
                    <ElInput
                      ref="modifyInputRefs"
                      :model-value="item.title"
                      maxlength="50"
                      @blur="confirmModifyMsgName(item)"
                      @input="(val) => inputTitle(item, val)"
                      @keydown="(event) => handleKeydown(event, item)"
                    />
                  </div>
                </div>

                <el-popover
                  v-if="!modifyInputRefIndex"
                  placement="left-start"
                  trigger="click"
                  popper-class="history-operate-poper"
                >
                  <template #reference>
                    <div class="more-btn" @click.stop>
                      <i class="iconfont icon-more" />
                    </div>
                  </template>
                  <div class="each-operate" @click.stop="pinTop(item)">
                    <i class="iconfont icon-pin-top" />{{ item.pinnedAt ? '取消置顶' : '置顶' }}
                  </div>
                  <div class="each-operate" @click.stop="modifyMsgName(item, key)">
                    <i class="iconfont icon-edit" />重命名
                  </div>
                  <div class="each-operate delete-btn" @click.stop="deleteSingleMsg(item)">
                    <i class="iconfont icon-Delete" />删除
                  </div>
                </el-popover>
              </div>
            </template>
          </GroupChatHistory>
        </div>

        <ConfirmDialog v-model="dialogVisible" @confirm="deleteSelecteMsg" />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Close, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GroupChatHistory from '@/components/GroupChatHistory/index.vue'
import ConfirmDialog from '@/components/ConfirmDialog/index.vue'
import { deleteHistory, getHistoryListOwn, modifyChatMsg } from '@/api/chat.js'
import { CHAT_TYPE } from '@/utils/enum'
import { useRoute } from 'vue-router'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  // 智能体入口页没有会话类型路由参数时，由调用方显式指定历史类型。
  chatType: {
    type: String,
    default: '',
  },
})
const route = useRoute()
const historyType = computed(() => {
  if (props.chatType) return props.chatType

  const supportedTypes = [CHAT_TYPE.IMAGE_RECOGNITION, CHAT_TYPE.SPACE_ANALYSIS]
  return supportedTypes.includes(route.query.type) ? route.query.type : CHAT_TYPE.NORMAL
})

const emits = defineEmits(['update:modelValue', 'select', 'closed'])

const GroupChatHistoryRef = ref()
const scrollRootRef = ref()
const historys = ref([])
const totalChatCount = ref(0)
const pageNum = ref(1)
const inited = ref(false)
const dialogVisible = ref(false)
const modifyInputRefs = ref()
const modifyInputRefIndex = ref()
const pageSize = 50
let delId = ''

const hasMore = computed(() => {
  if (!inited.value) return true
  return totalChatCount.value > historys.value.length
})

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      refresh()
    }
  },
)

onMounted(() => {
  if (props.modelValue) {
    refresh()
  }
})

function closeDrawer() {
  emits('update:modelValue', false)
}

function handleAfterLeave() {
  emits('closed')
}

function handleItemsUpdate(newItems) {
  historys.value = newItems
}

async function getList(page, size) {
  const res = await getHistoryListOwn({
    pageSize: size || pageSize,
    pageNum: page || pageNum.value,
    type: historyType.value,
    orderByColumn: 'pinnedAt,updateDate',
    isAsc: 'desc',
  })
  inited.value = true
  totalChatCount.value = res.total

  return (res.data || []).map((item) => ({ ...item, isModifying: false }))
}

function refresh() {
  pageNum.value = 1
  inited.value = false
  totalChatCount.value = 0
  historys.value = []
  nextTick(() => {
    GroupChatHistoryRef.value?.clear()
    GroupChatHistoryRef.value?.loadMore()
  })
}

function handleScroll() {
  const el = scrollRootRef.value
  if (!el || !hasMore.value) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distanceToBottom <= 120) {
    GroupChatHistoryRef.value?.loadMore()
  }
}

async function pinTop(item) {
  const data = {
    id: item.id,
    pinnedAt: item.pinnedAt ? 0 : 1,
  }
  await modifyChatMsg(data)
  ElMessage.success('操作成功')
  GroupChatHistoryRef.value?.forceRank(data)
}

function deleteSingleMsg(msg) {
  delId = msg.id
  dialogVisible.value = true
}

async function deleteSelecteMsg() {
  if (!delId) return
  await deleteHistory(delId)
  ElMessage.success('删除成功')
  delId = ''
  refresh()
}

async function modifyMsgName(msg, key) {
  msg.isModifying = true
  modifyInputRefIndex.value = `${key}`
  await nextTick()
  setTimeout(() => {
    const inputRef = Array.isArray(modifyInputRefs.value) ? modifyInputRefs.value[key] : modifyInputRefs.value
    inputRef?.focus()
  }, 0)
}

function inputTitle(item, val) {
  item.title = val
}

async function confirmModifyMsgName(item) {
  const title = item.title?.trim()
  if (!title) {
    ElMessage.warning('对话标题不能为空')
    return
  }
  await modifyChatMsg({
    id: item.id,
    title,
  })
  item.title = title
  item.isModifying = false
  modifyInputRefIndex.value = null
  ElMessage.success('修改成功')
}

function handleKeydown(event, msg) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    confirmModifyMsgName(msg)
  }
}

function getChatDetail(item) {
  if (item.isModifying) return
  emits('select', item.id)
  closeDrawer()
}
</script>

<style lang="less" scoped>
.history-drawer-host {
  position: absolute;
  inset: 0;
  z-index: 20;
  pointer-events: none;
  overflow: hidden;
  border-radius: inherit;
}

.history-drawer-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 360px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e6ebf5;
  pointer-events: auto;
}

.history-slide-enter-active,
.history-slide-leave-active {
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.history-slide-enter-from,
.history-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.history-header {
  height: 60px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  // border-bottom: 1px solid #edf1f7;
  flex-shrink: 0;
}

.history-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 800;
  color: #222529;
}

.close-btn {
  color: #878898;
  cursor: pointer;
  font-size: 18px;

  &:hover {
    color: var(--el-color-primary);
  }
}

.history-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 12px 18px 18px;
}

.each-chat {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 34px 10px 10px;
  margin-bottom: 4px;
  border-radius: 10px;
  cursor: pointer;
  &:hover {
    background: #f4f6fa;

    .more-btn {
      opacity: 1;
    }
  }
}

.each-content {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 14px;
  line-height: 20px;
  color: #222529;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.modify-box {
  :deep(.el-input__wrapper) {
    box-shadow: none;
    border: 1px solid #dfe4f0;
  }
}

.more-btn {
  position: absolute;
  top: 9px;
  right: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #878898;
  opacity: 0;
  border-radius: 8px;

  &:hover {
    background: #e8ebf0;
  }
}

.each-operate {
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  line-height: 28px;
  padding: 0 8px;
  border-radius: 6px;
  user-select: none;

  &:hover {
    background: #f2f4f8;
  }

  i {
    margin-right: 6px;
  }
}

.delete-btn {
  color: #e23030;
}

:deep(.group-title) {
  margin: 14px 0 10px;
  font-size: 14px;
  color: #878898;
  font-weight: 800;
}

:deep(.time-group) {
  margin-bottom: 20px;
}
</style>

<style lang="less">
.el-popper.history-operate-poper {
  max-width: 100px !important;
  min-width: auto;
  border-radius: 8px;
  padding: 8px 6px;

  .el-popper__arrow {
    display: none;
  }
}
</style>
