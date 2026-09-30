<template>
  <main class="create-meeting-panel">
    <header class="create-meeting-panel__header">
      <h3>发起会商</h3>
      <span>（填写主题，邀请参会人员后即可进入会商室）</span>
    </header>

    <div class="form-row">
      <label for="consultation-subject"><i>*</i>会商主题：</label>
      <el-input
        id="consultation-subject"
        v-model="subject"
        maxlength="100"
        placeholder="请输入会商主题"
      />
      <button
        class="transcription-switch"
        :class="{ 'is-active': smartSummaryEnabled }"
        type="button"
        role="switch"
        :aria-checked="smartSummaryEnabled"
        @click="smartSummaryEnabled = !smartSummaryEnabled"
      >
        <span></span>
      </button>
      <strong>开启智能纪要</strong>
      <small>（会后自动生成智能纪要）</small>
    </div>

    <div class="divider"></div>

    <section class="people-selector">
      <UserSelectTable
        :users="users"
        :total="total"
        :loading="initialLoading"
        :loading-more="loadingMore"
        :is-end="isEnd"
        :is-selected="isSelected"
        :current-user-id="currentUserId"
        @search="handleSearch"
        @load-more="handleLoadMore"
        @select="selectUser"
      />
      <div class="vertical-divider"></div>
      <SelectedExpertsList :users="selectedUsers" @remove="removeUser" />
    </section>

    <CreateMeetingFooter
      :count="selectedUsers.length"
      :disabled="!canSubmit"
      @submit="emit('submit', createPayload)"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMeetingUsers } from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'
import CreateMeetingFooter from './CreateMeetingFooter.vue'
import SelectedExpertsList from './SelectedExpertsList.vue'
import UserSelectTable from './UserSelectTable.vue'

defineOptions({ name: 'CreateMeetingPanel' })

const emit = defineEmits(['submit'])

const userStore = useUserStore()
const currentUserId = computed(() => {
  return String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
})

const subject = ref('')
const smartSummaryEnabled = ref(false)
const users = ref([])
const selectedUsers = ref([])
const total = ref(0)
const initialLoading = ref(false)
const loadingMore = ref(false)
const isEnd = ref(false)
const pageNum = ref(1)
const pageSize = 20
const currentKeyword = ref('')

// 只要输入会商主题即可开会，不限制是否邀请他人
const canSubmit = computed(() => Boolean(subject.value.trim()))

const createPayload = computed(() => ({
  title: subject.value.trim(),
  participantIds: selectedUsers.value
    .map((user) => String(user.userId))
    .filter((id) => Boolean(id) && id !== currentUserId.value),
  transcriptionEnabled: smartSummaryEnabled.value,
}))

const isSelected = (user) => {
  const targetId = String(user?.userId ?? user?.id ?? '')
  return selectedUsers.value.some((item) => String(item.userId ?? item.id ?? '') === targetId)
}

const fetchUsers = async (refresh = false) => {
  if (loadingMore.value || (isEnd.value && !refresh)) return
  if (refresh) {
    pageNum.value = 1
    isEnd.value = false
    initialLoading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const response = await getMeetingUsers({
      keyword: currentKeyword.value || undefined,
      pageNum: pageNum.value,
      pageSize,
    })

    // 兼容各种返回结构：顶层纯数组、data、rows、data.records、data.rows、list
    let rawList = []
    if (Array.isArray(response)) {
      rawList = response
    } else if (Array.isArray(response?.data)) {
      rawList = response.data
    } else if (Array.isArray(response?.rows)) {
      rawList = response.rows
    } else if (Array.isArray(response?.data?.records)) {
      rawList = response.data.records
    } else if (Array.isArray(response?.data?.rows)) {
      rawList = response.data.rows
    } else if (Array.isArray(response?.list)) {
      rawList = response.list
    }

    const pageItems = rawList
      .map((u) => {
        const uid = String(u.userId ?? u.id ?? u.user_id ?? '')
        return {
          ...u,
          userId: uid,
          id: uid,
          displayName: u.displayName || u.nickName || u.username || u.name || '',
          unit: u.unit || u.orgName || u.deptName || u.dept?.deptName || '',
          title: u.title || u.post || u.postName || '',
          phone: u.phone || u.phonenumber || u.mobile || '',
        }
      })
      .filter((u) => Boolean(u.userId))

    const source = refresh ? pageItems : [...users.value, ...pageItems]
    users.value = Array.from(
      new Map(source.filter((item) => item?.userId).map((item) => [String(item.userId), item])).values(),
    )

    const numTotal = Number(response?.total ?? response?.data?.total)
    if (Number.isFinite(numTotal) && numTotal >= 0) {
      total.value = numTotal
      isEnd.value = !pageItems.length || users.value.length >= total.value
    } else {
      // 纯数组未提供总数时，以当前累计条数为准；若单页返回条数小于 pageSize 则判定已无更多
      total.value = users.value.length
      isEnd.value = !pageItems.length || pageItems.length < pageSize
    }
    if (!isEnd.value) {
      pageNum.value += 1
    }
  } catch (error) {
    console.error('获取参会人员失败', error)
    if (refresh) {
      users.value = []
      total.value = 0
    }
  } finally {
    initialLoading.value = false
    loadingMore.value = false
  }
}

const handleSearch = (keyword) => {
  currentKeyword.value = (keyword || '').trim()
  fetchUsers(true)
}

const handleLoadMore = () => {
  if (!loadingMore.value && !isEnd.value && !initialLoading.value) {
    fetchUsers(false)
  }
}

const selectUser = (user) => {
  const userId = String(user?.userId ?? user?.id ?? '')
  if (!userId || (currentUserId.value && userId === currentUserId.value) || isSelected(user)) return
  selectedUsers.value.push({
    ...user,
    userId,
    id: userId,
  })
}

const removeUser = (userId) => {
  selectedUsers.value = selectedUsers.value.filter((user) => String(user.userId ?? user.id) !== String(userId))
}

onMounted(() => {
  fetchUsers(true)
})
</script>

<style lang="less" scoped>
.create-meeting-panel {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding: 20px 24px 16px;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  background: #ffffff;
}

.create-meeting-panel__header {
  display: flex;
  height: 18px;
  align-items: center;

  h3 {
    margin: 0;
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 18px;
  }

  span {
    margin-left: 8px;
    color: #9096a2;
    font-size: 12px;
    line-height: 14px;
  }
}

.form-row {
  display: flex;
  height: 32px;
  align-items: center;
  margin-top: 20px;

  label {
    flex: none;
    margin-right: 8px;
    color: #617185;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;

    i {
      color: #dd4739;
      font-style: normal;
    }
  }

  :deep(.el-input) {
    width: 419px;
    height: 32px;
  }

  :deep(.el-input__wrapper) {
    min-height: 32px;
    padding: 0 12px;
    border-radius: 8px;
    box-shadow: 0 0 0 1px #e4e5eb inset;
  }

  :deep(.el-input__inner) {
    color: #383c41;
    font-size: 14px;
  }

  :deep(.el-input__count) {
    color: #a6acb8;
    font-size: 12px;
  }

  strong {
    margin-left: 10px;
    color: #007bff;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
  }

  small {
    margin-left: 4px;
    color: #a6acb8;
    font-size: 12px;
    line-height: 14px;
  }
}

.transcription-switch {
  position: relative;
  width: 36px;
  height: 18px;
  flex: none;
  margin-left: 16px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #d7e2f3;
  cursor: pointer;
  transition: background-color 0.2s ease;

  span {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #ffffff;
    transition: transform 0.2s ease;
  }

  &.is-active {
    background: #007bff;

    span {
      transform: translateX(18px);
    }
  }
}

.divider {
  width: 100%;
  height: 1px;
  flex: none;
  margin: 16px 0;
  background: #e4eaef;
}

.people-selector {
  display: flex;
  min-height: 0;
  flex: 1;
  gap: 19px;
  padding-right: 19px;
  box-sizing: border-box;
}

.vertical-divider {
  width: 1px;
  flex: none;
  background: #e4eaef;
}

</style>
