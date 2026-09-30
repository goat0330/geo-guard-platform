<template>
  <Transition name="drawer-slide">
    <aside v-if="visible" class="share-drawer" role="complementary" aria-label="发送给责任人">
      <div class="drawer-header">
        <h3 class="drawer-title">发送给责任人</h3>
        <button
          type="button"
          class="drawer-close"
          title="关闭"
          aria-label="关闭"
          @click="emit('update:visible', false)"
        >
          ✕
        </button>
      </div>

      <div v-loading="loading" class="drawer-body">
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索姓名或账号"
            clearable
            size="small"
            @keyup.enter="handleSearch"
            @clear="handleClear"
          >
            <template #prefix>
              <i class="iconfont icon-search"></i>
            </template>
          </el-input>
        </div>

        <!-- 人员虚拟滚动列表 -->
        <div v-if="userList.length" class="person-list-wrap">
          <DynamicScroller
            :items="userList"
            :min-item-size="72"
            key-field="userId"
            class="person-scroller"
            @scroll.native="handleScroll"
          >
            <template #default="{ item, index, active }">
              <DynamicScrollerItem :item="item" :active="active" :data-index="index">
                <div class="person-card-wrap">
                  <div
                    class="person-card"
                    :class="{ 'is-checked': isUserChecked(item) }"
                    @click="togglePerson(item)"
                  >
                    <div class="person-check" :class="{ 'is-checked': isUserChecked(item) }">
                      <svg v-if="isUserChecked(item)" viewBox="0 0 12 12" width="10" height="10" fill="none">
                        <path
                          d="M2.5 6.2L4.8 8.5L9.5 3.5"
                          stroke="#FFFFFF"
                          stroke-width="1.8"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </div>

                    <img :src="userIcon" class="person-avatar" alt="头像" />

                    <div class="person-info">
                      <div class="person-row">
                        <span class="person-name">{{ item.displayName }}</span>
                        <span v-if="item.title" class="person-tag">（{{ item.title }}）</span>
                      </div>
                      <div v-if="item.unit" class="person-org" :title="item.unit">
                        {{ item.unit }}
                      </div>
                    </div>
                  </div>
                </div>
              </DynamicScrollerItem>
            </template>
            <template #after>
              <div v-if="loadingMore" class="scroll-status">加载中...</div>
              <div v-else-if="isEnd && userList.length" class="scroll-status">没有更多人员了</div>
            </template>
          </DynamicScroller>
        </div>

        <div v-else-if="!loading" class="empty-person-placeholder">
          {{ searchKeyword ? '未搜索到匹配人员' : '暂无可分享人员' }}
        </div>

        <!-- 附言输入框 -->
        <div class="postscript-wrap">
          <div class="postscript-header">
            <label class="postscript-label" for="minutes-postscript">附言：</label>
            <span class="word-count">{{ postscriptText.length }}/500</span>
          </div>
          <textarea
            id="minutes-postscript"
            v-model="postscriptText"
            class="postscript-textarea"
            placeholder="请输入附言内容（可选，最长500字）"
            maxlength="500"
            rows="3"
          ></textarea>
        </div>
      </div>

      <!-- 抽屉底部操作 -->
      <div class="drawer-footer">
        <div class="selected-summary">
          已选择<em class="num-font">{{ selectedCount }}</em>人
        </div>
        <div class="drawer-buttons">
          <button type="button" class="drawer-cancel-btn" @click="emit('update:visible', false)">
            取消
          </button>
          <button
            type="button"
            class="drawer-send-btn"
            :disabled="sending || selectedCount === 0"
            @click="handleSend"
          >
            {{ sending ? '发送中...' : '确认发送' }}
          </button>
        </div>
      </div>
    </aside>
  </Transition>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { getMeetingUsers, shareMeetingSummary } from '@/api/meeting.js'
import { useUserStore } from '@/store/user.js'
import userIcon from '@/assets/imgs/chatRoom/user.png'

defineOptions({ name: 'ShareDrawer' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  meetingId: {
    type: String,
    default: '',
  },
  participants: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:visible', 'send'])

const userStore = useUserStore()
const currentUserId = computed(() => {
  return String(userStore.userInfo?.user?.userId || userStore.userInfo?.userId || '')
})

const loading = ref(false)
const loadingMore = ref(false)
const sending = ref(false)
const isEnd = ref(false)
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const searchKeyword = ref('')
const postscriptText = ref('')
const userList = ref([])
const selectedUserIds = ref(new Set())

const selectedCount = computed(() => {
  return selectedUserIds.value.size
})

const isUserChecked = (person) => {
  return selectedUserIds.value.has(String(person.userId))
}

const togglePerson = (person) => {
  const uid = String(person.userId)
  const newSet = new Set(selectedUserIds.value)
  if (newSet.has(uid)) {
    newSet.delete(uid)
  } else {
    newSet.add(uid)
  }
  selectedUserIds.value = newSet
}

const fetchUsers = async (isReset = false) => {
  if (isReset) {
    pageNum.value = 1
    isEnd.value = false
    userList.value = []
    loading.value = true
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

    // 过滤掉当前登录用户自己
    const mapped = rawList
      .filter((item) => {
        const uid = String(item.userId || item.id || '')
        return uid && uid !== currentUserId.value
      })
      .map((item) => {
        const uid = String(item.userId || item.id)
        return {
          userId: uid,
          displayName: item.displayName || item.name || item.nickName || item.username || '用户',
          title: item.title || item.post || '',
          unit: item.unit || item.orgName || item.deptName || '',
        }
      })

    if (isReset) {
      userList.value = mapped
    } else {
      const existingIds = new Set(userList.value.map((u) => u.userId))
      const toAppend = mapped.filter((u) => !existingIds.has(u.userId))
      userList.value = [...userList.value, ...toAppend]
    }

    if (rawList.length < pageSize || userList.value.length >= total.value) {
      isEnd.value = true
    }
  } catch (err) {
    console.error('获取系统全部人员失败', err)
    ElMessage.error(err?.msg || err?.message || '获取人员列表失败')
  } finally {
    loading.value = false
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
  fetchUsers(true)
}

const handleClear = () => {
  searchKeyword.value = ''
  fetchUsers(true)
}

const handleSend = async () => {
  const userIds = Array.from(selectedUserIds.value)
  if (!userIds.length) {
    ElMessage.warning('请至少选择一位责任人')
    return
  }
  if (!props.meetingId) {
    ElMessage.error('会议标识缺失，无法分享纪要')
    return
  }

  const remark = postscriptText.value.trim() || undefined

  sending.value = true
  try {
    await shareMeetingSummary(props.meetingId, { userIds, remark })
    ElMessage.success('已成功将会议纪要发送给所选责任人')
    emit('send', { userIds, remark })
    emit('update:visible', false)
  } catch (err) {
    console.error('发送会议纪要失败', err)
    ElMessage.error(err?.msg || err?.message || '发送会议纪要失败，请重试')
  } finally {
    sending.value = false
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      selectedUserIds.value = new Set()
      searchKeyword.value = ''
      postscriptText.value = ''
      fetchUsers(true)
    }
  },
  { immediate: true },
)
</script>

<style lang="less" scoped>
.share-drawer {
  width: 321px;
  height: 794px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.16);
  display: flex;
  flex-direction: column;
  padding: 24px 16px 16px;
  box-sizing: border-box;
  flex-shrink: 0;

  .drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .drawer-title {
      margin: 0;
      color: #222527;
      font-size: 16px;
      font-weight: 700;
      line-height: 20px;
    }

    .drawer-close {
      width: 24px;
      height: 24px;
      padding: 0;
      border: 0;
      background: transparent;
      color: #9096a2;
      font-size: 16px;
      cursor: pointer;
      transition: color 0.2s ease;

      &:hover {
        color: #222527;
      }
    }
  }

  .drawer-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow: hidden;
  }

  .search-box {
    flex-shrink: 0;

    :deep(.el-input__wrapper) {
      border-radius: 8px;
      background: #f7f8f9;
      box-shadow: none;
      border: 1px solid #e4eaef;
    }
  }

  .person-list-wrap {
    flex: 1;
    min-height: 0;
    position: relative;
  }

  .person-scroller {
    height: 100%;
    overflow-y: auto;
  }

  .person-card-wrap {
    padding-bottom: 8px;
    box-sizing: border-box;
  }

  .scroll-status {
    padding: 8px 0 12px;
    text-align: center;
    color: #a6acb8;
    font-size: 12px;
    line-height: 16px;
  }

  .person-card {
    display: flex;
    align-items: center;
    height: 64px;
    padding: 0 12px;
    border: 1px solid #e4eaef;
    border-radius: 12px;
    background: #ffffff;
    box-sizing: border-box;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #007bff;
      background: #f8fbff;
    }

    &.is-checked {
      border-color: #007bff;
      background: #f0f7ff;
    }

    .person-check {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 14px;
      height: 14px;
      border: 1px solid #d5dbe0;
      border-radius: 3px;
      background: #ffffff;
      margin-right: 10px;
      flex-shrink: 0;
      transition: all 0.2s ease;

      &.is-checked {
        border-color: #007bff;
        background: #007bff;
      }
    }

    .person-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      margin-right: 8px;
      object-fit: contain;
      flex-shrink: 0;
    }

    .person-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .person-row {
      display: flex;
      align-items: center;
      overflow: hidden;
      white-space: nowrap;

      .person-name {
        color: #383c41;
        font-size: 14px;
        font-weight: 500;
        line-height: 16px;
      }

      .person-tag {
        color: #9096a2;
        font-size: 12px;
        line-height: 16px;
      }
    }

    .person-org {
      margin-top: 4px;
      color: #9096a2;
      font-size: 12px;
      line-height: 16px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .person-status {
      margin-left: 6px;
      flex-shrink: 0;

      .status-badge {
        display: inline-block;
        padding: 1px 4px;
        border-radius: 4px;
        background: rgba(68, 182, 153, 0.16);
        color: #44b699;
        font-size: 10px;
        line-height: 14px;
      }
    }
  }

  .empty-person-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 120px;
    padding: 20px 0;
    color: #9096a2;
    font-size: 14px;
    text-align: center;
  }

  .postscript-wrap {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .postscript-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .postscript-label {
      color: #9096a2;
      font-size: 14px;
      line-height: 16px;
    }

    .word-count {
      color: #a6acb8;
      font-size: 12px;
      line-height: 16px;
      font-family: 'Alimama FangYuanTi VF', sans-serif;
    }

    .postscript-textarea {
      width: 100%;
      height: 80px;
      padding: 10px 12px;
      border: 1px solid #e4e5eb;
      border-radius: 8px;
      background: #ffffff;
      color: #383c41;
      font-size: 12px;
      line-height: 18px;
      box-sizing: border-box;
      resize: none;
      outline: none;
      transition: border-color 0.2s ease;

      &:focus {
        border-color: #007bff;
      }
    }
  }

  .drawer-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 48px;
    margin-top: 12px;
    border-top: 1px solid #f1f4f7;
    padding-top: 10px;
    flex-shrink: 0;

    .selected-summary {
      color: #617185;
      font-size: 12px;
      line-height: 16px;

      em {
        font-style: normal;
        font-weight: 700;
        color: #007bff;
        margin: 0 2px;
      }
    }

    .drawer-buttons {
      display: flex;
      align-items: center;
      gap: 8px;

      .drawer-cancel-btn {
        width: 60px;
        height: 32px;
        padding: 0;
        border: 1px solid #d5dbe0;
        border-radius: 6px;
        background: #ffffff;
        color: #617185;
        font-size: 12px;
        line-height: 30px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #f7f8f9;
        }
      }

      .drawer-send-btn {
        width: 76px;
        height: 32px;
        padding: 0;
        border: 0;
        border-radius: 6px;
        background: #007bff;
        color: #ffffff;
        font-size: 12px;
        line-height: 32px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover:not(:disabled) {
          background: #006ce3;
        }

        &:disabled {
          background: #d5dbe0;
          color: #9096a2;
          cursor: not-allowed;
        }
      }
    }
  }
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.25, 1, 0.5, 1);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
</style>
