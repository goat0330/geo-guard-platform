<template>
  <Teleport to="body">
    <Transition name="picker-fade">
      <div v-if="visible" class="user-picker-mask" @click.self="handleClose">
        <div class="user-picker-card" role="dialog" aria-modal="true">
          <!-- 弹窗右上角关闭按钮 -->
          <button class="close-btn" type="button" aria-label="关闭" @click="handleClose">
            <i class="iconfont icon-close"></i>
          </button>

          <!-- 主体左右双栏 -->
          <div class="picker-body">
            <!-- 左侧：用户库与表格 -->
            <div class="left-section">
              <div class="section-header">
                <div class="title-wrap">
                  <i class="title-point"></i>
                  <span class="main-title">用户库</span>
                  <span class="total-desc">
                    用户总数：<em class="num-font">{{ total }}</em>名
                  </span>
                </div>
                <div class="search-wrap">
                  <el-input
                    v-model="keyword"
                    placeholder="请输入用户姓名"
                    class="search-input"
                    clearable
                    @keyup.enter="handleSearch"
                    @clear="handleSearch"
                  />
                  <el-button type="primary" class="search-btn" @click="handleSearch">
                    搜索
                  </el-button>
                </div>
              </div>

              <!-- 用户表格 -->
              <div class="table-wrap">
                <el-table
                  v-loading="loading"
                  :data="userList"
                  style="width: 100%"
                  height="100%"
                  empty-text="暂无用户数据"
                >
                  <el-table-column label="姓名" min-width="120">
                    <template #default="{ row }">
                      <div class="user-name-cell">
                        <span class="user-status-dot"></span>
                        <span class="name-text">{{ row.displayName || row.username || '--' }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="username" label="账号" min-width="140" />
                  <el-table-column label="操作" width="100" align="center">
                    <template #default="{ row }">
                      <span v-if="isSelf(row)" class="op-tag is-disabled">
                        自己
                      </span>
                      <span v-else-if="isExistingMember(row.userId)" class="op-tag is-disabled">
                        已在会
                      </span>
                      <span v-else-if="isSelected(row.userId)" class="op-link is-remove" @click="removeUser(row.userId)">
                        移除
                      </span>
                      <span v-else class="op-link" @click="addUser(row)">
                        邀请
                      </span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 分页条 -->
              <div class="pagination-bar">
                <div class="page-summary">
                  第 <span class="num-font">{{ pageNum }}</span> 页/共 <span class="num-font">{{ maxPage }}</span> 页，共 <span class="num-font">{{ total }}</span> 条数据
                </div>
                <el-pagination
                  v-model:current-page="pageNum"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50]"
                  small
                  background
                  layout="sizes, prev, pager, next"
                  :total="total"
                  @size-change="handlePageChange"
                  @current-change="handlePageChange"
                />
              </div>
            </div>

            <!-- 右侧：选择用户列表 -->
            <div class="right-section">
              <div class="right-header">
                <i class="title-point"></i>
                <span class="right-title">选择用户列表</span>
                <span v-if="selectedList.length" class="selected-badge num-font">
                  {{ selectedList.length }}
                </span>
              </div>

              <div class="selected-scroll-list">
                <div
                  v-for="item in selectedList"
                  :key="item.userId"
                  class="selected-item"
                >
                  <div class="avatar-circle">
                    {{ (item.displayName || item.username || '用').slice(0, 1) }}
                  </div>
                  <div class="user-info">
                    <span class="info-name" :title="item.displayName || item.username">
                      {{ item.displayName || item.username }}
                    </span>
                    <span class="info-sub">{{ item.username }}</span>
                  </div>
                  <button
                    type="button"
                    class="item-delete"
                    title="移除此用户"
                    @click="removeUser(item.userId)"
                  >
                    <i class="iconfont icon-close"></i>
                  </button>
                </div>

                <div v-if="!selectedList.length" class="empty-placeholder">
                  暂未选择用户，请在左侧列表中点击“邀请”
                </div>
              </div>

              <div class="submit-wrap">
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="submitLoading"
                  @click="handleSubmit"
                >
                  {{ mode === 'invite' ? '确认邀请' : '进入会商室' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingUsers } from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  // 模式：'create' 创建前拉人，'invite' 会中拉人
  mode: {
    type: String,
    default: 'create',
  },
  // 已在会议中的成员 ID 列表（中途拉人时禁用重复邀请）
  existingMemberIds: {
    type: Array,
    default: () => [],
  },
  submitLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:visible', 'confirm'])

const userStore = useUserStore()

const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userList = ref([])
const loading = ref(false)
const selectedList = ref([])

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value) || 1))

const currentUserId = computed(() => {
  return String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
})

const currentUserName = computed(() => {
  return userStore.userInfo?.user?.userName || userStore.userInfo?.userName || ''
})

/**
 * 判断是否为当前登录用户自己
 */
const isSelf = (rowOrId) => {
  const myId = currentUserId.value
  const myName = currentUserName.value
  if (!myId && !myName) return false

  if (typeof rowOrId === 'object' && rowOrId !== null) {
    const rowId = String(rowOrId.userId ?? rowOrId.id ?? '')
    const rowName = rowOrId.username || rowOrId.userName || ''
    if (myId && rowId && myId === rowId) return true
    if (myName && rowName && myName === rowName) return true
    return false
  }

  const idStr = String(rowOrId ?? '')
  return Boolean(myId && idStr && myId === idStr)
}

const isExistingMember = (userId) => {
  return props.existingMemberIds.map(String).includes(String(userId))
}

const isSelected = (userId) => {
  return selectedList.value.some((item) => String(item.userId) === String(userId))
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getMeetingUsers({
      keyword: keyword.value ? keyword.value.trim() : undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    userList.value = Array.isArray(res?.data) ? res.data : []
    total.value = typeof res?.total === 'number' ? res.total : 0
  } catch (err) {
    console.error('获取用户目录失败', err)
    userList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchUsers()
}

const handlePageChange = () => {
  fetchUsers()
}

const addUser = (row) => {
  if (isSelf(row) || isSelected(row.userId) || isExistingMember(row.userId)) return
  selectedList.value.push({
    userId: String(row.userId),
    username: row.username || '',
    displayName: row.displayName || row.username || '',
  })
}

const removeUser = (userId) => {
  selectedList.value = selectedList.value.filter((u) => String(u.userId) !== String(userId))
}

const handleClose = () => {
  emit('update:visible', false)
}

const handleSubmit = () => {
  // 防守过滤自身
  selectedList.value = selectedList.value.filter((u) => !isSelf(u.userId))
  if (props.mode === 'invite' && !selectedList.value.length) {
    ElMessage.warning('请至少选择一位需要邀请的用户')
    return
  }
  emit('confirm', [...selectedList.value])
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      selectedList.value = []
      pageNum.value = 1
      fetchUsers()
    }
  },
  { immediate: true },
)
</script>

<style lang="less" scoped>
.user-picker-mask {
  position: fixed;
  inset: 0;
  z-index: 2100;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.user-picker-card {
  position: relative;
  width: 960px;
  max-width: calc(100vw - 40px);
  height: 560px;
  max-height: calc(100vh - 40px);
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.16);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 18px;
  z-index: 10;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #222527;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: #f0f3f8;
  }
}

.picker-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.title-point {
  width: 6px;
  height: 6px;
  background: #007BFF;
  border-radius: 2px;
  display: inline-block;
  margin-right: 8px;
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}

/* 左侧样式 */
.left-section {
  flex: 1;
  min-width: 0;
  padding: 24px 20px 20px 28px;
  border-right: 1px solid #eef2f7;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.title-wrap {
  display: flex;
  align-items: center;
}

.main-title {
  font-size: 18px;
  font-weight: 600;
  color: #222527;
  margin-right: 14px;
}

.total-desc {
  font-size: 14px;
  color: #617185;

  em {
    color: #222527;
    margin: 0 2px;
  }
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;

  .search-input {
    width: 200px;
  }

  .search-btn {
    background: #007BFF;
    border-color: #007BFF;
    height: 32px;
    padding: 0 16px;
  }
}

.table-wrap {
  flex: 1;
  min-height: 0;
  background: #ffffff;
}

.user-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #007BFF;
}

.name-text {
  font-size: 14px;
  color: #222527;
}

.op-link {
  font-size: 14px;
  color: #007BFF;
  cursor: pointer;
  user-select: none;

  &:hover {
    opacity: 0.8;
  }

  &.is-remove {
    color: #ff4d4f;
  }
}

.op-tag.is-disabled {
  font-size: 14px;
  color: #A6ACB8;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 10px;
}

.page-summary {
  font-size: 12px;
  color: #617185;
}

/* 右侧样式 */
.right-section {
  width: 290px;
  background: #fbfdff;
  padding: 24px 20px 20px;
  display: flex;
  flex-direction: column;
}

.right-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.right-title {
  font-size: 16px;
  font-weight: 600;
  color: #222527;
}

.selected-badge {
  margin-left: 8px;
  padding: 2px 8px;
  background: #DCEDFF;
  color: #007BFF;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1;
}

.selected-scroll-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selected-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #edf2f8;
  border-radius: 8px;
  gap: 10px;
  transition: all 0.2s;

  &:hover {
    border-color: #DCEDFF;
    box-shadow: 0 2px 8px rgba(0, 123, 255, 0.08);
  }
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #DCEDFF;
  color: #007BFF;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.info-name {
  font-size: 14px;
  color: #222527;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-sub {
  font-size: 12px;
  color: #9096A2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-delete {
  border: none;
  background: transparent;
  color: #A6ACB8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    color: #ff4d4f;
    background: #fff1f0;
  }
}

.empty-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px;
  color: #A6ACB8;
  font-size: 14px;
  line-height: 20px;
}

.submit-wrap {
  margin-top: 16px;
}

.submit-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: #007BFF;
  border-color: #007BFF;
}

.picker-fade-enter-active,
.picker-fade-leave-active {
  transition: opacity 0.25s ease;
}

.picker-fade-enter-from,
.picker-fade-leave-to {
  opacity: 0;
}
</style>
