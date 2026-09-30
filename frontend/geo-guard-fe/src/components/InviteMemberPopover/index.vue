<template>
  <el-popover
    v-model:visible="popoverVisible"
    trigger="click"
    placement="bottom-end"
    :width="300"
    popper-class="invite-member-popper"
    :show-arrow="true"
    @show="handlePopoverShow"
  >
    <template #reference>
      <button
        type="button"
        class="invite-person-btn"
        :class="{ 'is-active': popoverVisible }"
        title="邀请/管理参会人员"
        aria-label="邀请/管理参会人员"
      >
        <i class="iconfont icon-add-person"></i>
      </button>
    </template>

    <div class="invite-popover-content">
      <!-- 顶部标题与当前统计 -->
      <div class="popover-header">
        <span class="header-title">参会人员管理</span>
        <span class="selected-count">
          已选 <em class="num-font">{{ selectedIds.length }}</em> 人
        </span>
      </div>

      <!-- 搜索输入框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索姓名或账号"
          clearable
          size="small"
          @input="handleSearch"
          @clear="handleClear"
        >
          <template #prefix>
            <i class="iconfont icon-search"></i>
          </template>
        </el-input>
      </div>

      <!-- 人员多选虚拟滚动列表 -->
      <div v-loading="loading" class="user-check-list">
        <DynamicScroller
          v-if="userList.length"
          :items="userList"
          :min-item-size="38"
          key-field="userId"
          class="member-scroller"
          @scroll.native="handleScroll"
        >
          <template #default="{ item: user, index, active }">
            <DynamicScrollerItem :item="user" :active="active" :data-index="index">
              <div class="user-check-item-wrap">
                <div
                  class="user-check-item"
                  :class="{
                    'is-disabled': isUserDisabled(user),
                    'is-checked': isUserChecked(user),
                  }"
                  @click="toggleUser(user)"
                >
                  <el-checkbox
                    :model-value="isUserChecked(user)"
                    :disabled="isUserDisabled(user)"
                    @click.stop
                    @change="() => toggleUser(user)"
                  />

                  <div class="user-avatar">
                    {{ (user.displayName || user.username || '用').slice(0, 1) }}
                  </div>

                  <div class="user-names">
                    <span class="display-name" :title="user.displayName || user.username">
                      {{ user.displayName || user.username }}
                    </span>
                    <span v-if="user.displayName && user.username" class="username-sub">
                      ({{ user.username }})
                    </span>
                  </div>

                  <!-- 状态标签 -->
                  <span v-if="isHostUser(user)" class="status-tag is-host">主持人</span>
                  <span v-else-if="isSelf(user)" class="status-tag is-self">自己</span>
                  <span v-else-if="isInitialExisting(user)" class="status-tag is-existing">已在会</span>
                </div>
              </div>
            </DynamicScrollerItem>
          </template>
          <template #after>
            <div v-if="loadingMore" class="scroll-status">加载中...</div>
            <div v-else-if="isEnd && userList.length" class="scroll-status">没有更多人员了</div>
          </template>
        </DynamicScroller>

        <div v-else-if="!loading" class="empty-tip">
          {{ searchKeyword ? '未搜索到匹配人员' : '暂无可邀请人员' }}
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div class="popover-footer">
        <el-button size="small" @click="popoverVisible = false">取消</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="submitting"
          :disabled="!hasChanges"
          @click="handleConfirm"
        >
          确定
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import {
  getMeetingUsers,
  addMeetingParticipants,
  removeMeetingParticipant,
  getMeetingDetail,
} from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'

defineOptions({ name: 'InviteMemberPopover' })

const props = defineProps({
  meetingId: {
    type: [String, Number],
    required: true,
  },
  existingMemberIds: {
    type: Array,
    default: () => [],
  },
  hostUserId: {
    type: [String, Number],
    default: '',
  },
  isHost: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['success'])

const userStore = useUserStore()
const popoverVisible = ref(false)
const searchKeyword = ref('')
const userList = ref([])
const selectedIds = ref([])
const initialExistingIds = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const isEnd = ref(false)
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const meetingHostUserId = ref('')
let searchTimer = null

const currentUserId = computed(() => {
  return String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
})

const currentUserName = computed(() => {
  return userStore.userInfo?.user?.userName || userStore.userInfo?.userName || ''
})

const hostUserIdStr = computed(() => {
  return String(meetingHostUserId.value || props.hostUserId || '')
})

// 是否为主持人用户
const isHostUser = (user) => {
  const uid = String(user?.userId ?? user?.id ?? '')
  return Boolean(hostUserIdStr.value && uid && hostUserIdStr.value === uid)
}

// 是否为登录用户自己
const isSelf = (user) => {
  const uid = String(user?.userId ?? '')
  const uname = user?.username || user?.userName || ''
  if (currentUserId.value && uid && currentUserId.value === uid) return true
  if (currentUserName.value && uname && currentUserName.value === uname) return true
  return false
}

// 是否原本就在会
const isInitialExisting = (user) => {
  const uid = String(user?.userId ?? '')
  return initialExistingIds.value.includes(uid)
}

// 是否勾选
const isUserChecked = (user) => {
  const uid = String(user?.userId ?? '')
  return selectedIds.value.includes(uid)
}

// 禁用规则：
// 1. 主持人不可移出会议
// 2. 当前登录用户如果是已有在会人员，不可在浮层中取消自己（走挂断/离开会议）
// 3. 非主持人无权移除在会人员
const isUserDisabled = (user) => {
  if (isHostUser(user)) return true
  if (isSelf(user) && isInitialExisting(user)) return true
  if (isInitialExisting(user) && !props.isHost) return true
  return false
}

// 变更统计
const toAddList = computed(() => {
  return selectedIds.value.filter((id) => !initialExistingIds.value.includes(id))
})

const toRemoveList = computed(() => {
  return initialExistingIds.value.filter((id) => {
    // 排除主持人与自己，不能被移除
    if (hostUserIdStr.value && id === hostUserIdStr.value) return false
    if (currentUserId.value && id === currentUserId.value) return false
    return !selectedIds.value.includes(id)
  })
})

const hasChanges = computed(() => {
  return toAddList.value.length > 0 || toRemoveList.value.length > 0
})

const actionSummaryText = computed(() => {
  const addCount = toAddList.value.length
  const delCount = toRemoveList.value.length
  if (addCount && delCount) {
    return `(+${addCount},-${delCount})`
  }
  if (addCount) {
    return `(+${addCount})`
  }
  if (delCount) {
    return `(-${delCount})`
  }
  return ''
})

const toggleUser = (user) => {
  if (isUserDisabled(user)) {
    if (isHostUser(user)) {
      ElMessage.warning('会议主持人不可移出会议')
    } else if (isSelf(user) && isInitialExisting(user)) {
      ElMessage.warning('无法在浮层中将自己移出会议')
    } else if (isInitialExisting(user) && !props.isHost) {
      ElMessage.warning('仅主持人可取消邀请或移除参会人员')
    }
    return
  }

  const idStr = String(user?.userId ?? user?.id ?? '')
  if (!idStr) return
  const index = selectedIds.value.indexOf(idStr)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(idStr)
  }
}

const fetchUsers = async (isReset = false) => {
  if (isReset) {
    pageNum.value = 1
    isEnd.value = false
    userList.value = []
  } else {
    if (loadingMore.value || isEnd.value) return
    loadingMore.value = true
  }

  try {
    const res = await getMeetingUsers({
      pageNum: pageNum.value,
      pageSize,
      keyword: searchKeyword.value.trim() || undefined,
    })

    const rawList = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
    total.value = typeof res?.total === 'number' ? res.total : rawList.length

    if (isReset) {
      userList.value = rawList
    } else {
      const existingIds = new Set(userList.value.map((u) => String(u.userId || u.id)))
      const toAppend = rawList.filter((u) => !existingIds.has(String(u.userId || u.id)))
      userList.value = [...userList.value, ...toAppend]
    }

    if (rawList.length < pageSize || userList.value.length >= total.value) {
      isEnd.value = true
    }
  } catch (err) {
    console.error('加载参会候选人员失败', err)
  } finally {
    loadingMore.value = false
  }
}

const handleScroll = (event) => {
  const target = event?.target || event?.currentTarget
  if (!target || loading.value || loadingMore.value || isEnd.value) return
  const { scrollTop, clientHeight, scrollHeight } = target
  if (scrollTop + clientHeight >= scrollHeight - 32) {
    pageNum.value += 1
    fetchUsers(false)
  }
}

const handleSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchUsers(true)
  }, 300)
}

const handleClear = () => {
  searchKeyword.value = ''
  fetchUsers(true)
}

const fetchDetailAndUsers = async () => {
  loading.value = true
  try {
    if (props.meetingId) {
      const detailRes = await getMeetingDetail(props.meetingId, { notUseError: true })
      const participants = detailRes?.participants || detailRes?.data?.participants || []
      const activeParticipants = participants.filter((p) => p?.status === 'ACTIVE')
      const activeUserIds = activeParticipants.map((p) => String(p.userId)).filter(Boolean)
      const hostId = String(
        detailRes?.meeting?.hostUserId ||
        detailRes?.data?.meeting?.hostUserId ||
        participants.find((p) => p.role === 'HOST')?.userId ||
        props.hostUserId ||
        '',
      )
      if (hostId) meetingHostUserId.value = hostId
      initialExistingIds.value = [...activeUserIds]
      selectedIds.value = [...activeUserIds]
    }
    await fetchUsers(true)
  } catch (err) {
    console.error('加载参会人员数据失败', err)
    initialExistingIds.value = (props.existingMemberIds || []).map(String).filter(Boolean)
    selectedIds.value = [...initialExistingIds.value]
  } finally {
    loading.value = false
  }
}

const handlePopoverShow = async () => {
  searchKeyword.value = ''
  await fetchDetailAndUsers()
}

const handleConfirm = async () => {
  if (!props.meetingId) {
    ElMessage.error('当前会议 ID 不存在')
    return
  }

  const toAdd = toAddList.value
  const toRemove = toRemoveList.value

  if (!toAdd.length && !toRemove.length) {
    popoverVisible.value = false
    return
  }

  submitting.value = true
  try {
    const tasks = []
    // 1. 新增邀请人员：POST /meeting/meetings/{meetingId}/participants
    if (toAdd.length > 0) {
      tasks.push(addMeetingParticipants(props.meetingId, toAdd))
    }
    // 2. 取消邀请/移除在会成员：DELETE /meeting/meetings/{meetingId}/participants/{userId}
    if (toRemove.length > 0) {
      toRemove.forEach((removeId) => {
        tasks.push(removeMeetingParticipant(props.meetingId, removeId))
      })
    }

    await Promise.all(tasks)

    let msg = '参会人员已更新'
    if (toAdd.length && toRemove.length) {
      msg = `已邀请 ${toAdd.length} 人，已取消邀请/移除 ${toRemove.length} 人`
    } else if (toAdd.length) {
      msg = `已成功邀请 ${toAdd.length} 人`
    } else if (toRemove.length) {
      msg = `已成功取消邀请/移除 ${toRemove.length} 人`
    }
    ElMessage.success(msg)

    popoverVisible.value = false
    emit('success', { added: toAdd, removed: toRemove })
  } catch (err) {
    console.error('更新参会人员失败', err)
    ElMessage.error(err?.message || '操作失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="less">
.invite-member-popper.el-popover.el-popper {
  padding: 0;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  border: 1px solid #e4eaef;
  background: #ffffff;
}
</style>

<style lang="less" scoped>
.invite-person-btn {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: #e4ebf3;
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #222527;
  transition: all 0.2s ease;

  &:hover,
  &.is-active {
    background: #d8e4f0;
    color: #007bff;
  }

  &:active {
    transform: scale(0.96);
  }

  .iconfont {
    font-size: 18px;
    line-height: 1;
  }
}

.invite-popover-content {
  display: flex;
  flex-direction: column;
  padding: 14px 16px 12px;
  box-sizing: border-box;
}

.popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  .header-title {
    color: #222527;
    font-size: 14px;
    font-weight: 700;
    line-height: 20px;
  }

  .selected-count {
    color: #007bff;
    font-size: 12px;
    line-height: 18px;

    em {
      font-style: normal;
      font-weight: 700;
    }
  }
}

.search-box {
  margin-bottom: 8px;

  :deep(.el-input__wrapper) {
    border-radius: 6px;
    background: #f7f8f9;
    box-shadow: none;
    border: 1px solid #e4eaef;
  }
}

.user-check-list {
  height: 240px;
  margin: 0 -4px;
  padding: 0 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.member-scroller {
  height: 100%;
  overflow-y: auto;
}

.user-check-item-wrap {
  padding-bottom: 4px;
  box-sizing: border-box;
}

.scroll-status {
  padding: 6px 0 8px;
  text-align: center;
  color: #a6acb8;
  font-size: 10px;
  line-height: 16px;
}

.user-check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover:not(.is-disabled) {
    background: #f0f7ff;
  }

  &.is-checked:not(.is-disabled) {
    background: #e6f2ff;
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }

  .user-avatar {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: #007bff;
    color: #ffffff;
    font-size: 12px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .user-names {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 4px;
    overflow: hidden;

    .display-name {
      color: #222527;
      font-size: 14px;
      line-height: 20px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .username-sub {
      color: #9096a2;
      font-size: 12px;
      line-height: 16px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .status-tag {
    flex-shrink: 0;
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 10px;
    line-height: 16px;

    &.is-host {
      background: rgba(255, 146, 44, 0.16);
      color: #ff922c;
    }

    &.is-self {
      background: #f2f4f7;
      color: #9096a2;
    }

    &.is-existing {
      background: #e6f7ff;
      color: #007bff;
    }
  }
}

.empty-tip {
  padding: 24px 0;
  text-align: center;
  color: #a6acb8;
  font-size: 12px;
}

.popover-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}
</style>
