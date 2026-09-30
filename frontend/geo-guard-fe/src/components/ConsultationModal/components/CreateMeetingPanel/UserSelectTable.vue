<template>
  <section class="user-select-table">
    <div class="section-title">
      <img :src="pointIcon" alt="" />
      <strong>选择参会人员</strong>
      <span>（共<em>{{ total }}</em>人）</span>
    </div>

    <div class="search-row">
      <el-input
        v-model="keyword"
        placeholder="请输入姓名、单位或职务"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleClear"
      />
      <button type="button" @click="handleSearch">搜索</button>
    </div>

    <div class="table-header">
      <div class="col col-name">姓名</div>
      <div class="col col-unit">单位</div>
      <div class="col col-title">职务职称</div>
      <div class="col col-phone">电话</div>
      <div class="col col-action">操作</div>
    </div>

    <div v-loading="loading" class="table-body-wrap">
      <div
        v-if="users.length"
        class="people-scroller"
        @scroll="handleScroll"
      >
        <div
          v-for="(item, index) in users"
          :key="item.userId"
          class="table-row"
          :class="{ 'is-even': index % 2 === 1 }"
        >
          <div class="col col-name" :title="item.displayName || item.nickName || item.username || ''">
            {{ item.displayName || item.nickName || item.username || '' }}
          </div>
          <div class="col col-unit" :title="item.unit || item.orgName || item.deptName || ''">
            {{ item.unit || item.orgName || item.deptName || '' }}
          </div>
          <div class="col col-title" :title="item.title || item.post || ''">
            {{ item.title || item.post || '' }}
          </div>
          <div class="col col-phone num-font" :title="item.phone || item.phonenumber || ''">
            {{ item.phone || item.phonenumber || '' }}
          </div>
          <div class="col col-action">
            <button
              class="invite-button"
              type="button"
              :disabled="isSelf(item) || isSelected(item)"
              @click="emit('select', item)"
            >
              {{ isSelf(item) ? '本人' : isSelected(item) ? '已添加' : '邀请' }}
            </button>
          </div>
        </div>
        <div v-if="loadingMore || isEnd" class="scroll-status">
          {{ loadingMore ? '加载中...' : '没有更多人员了' }}
        </div>
      </div>
      <div v-else-if="!loading" class="empty-placeholder">暂无人员数据</div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import pointIcon from '@/assets/imgs/chatRoom/point.png'

defineOptions({ name: 'UserSelectTable' })

const props = defineProps({
  users: {
    type: Array,
    default: () => [],
  },
  total: {
    type: Number,
    default: 0,
  },
  loading: Boolean,
  loadingMore: Boolean,
  isEnd: Boolean,
  isSelected: {
    type: Function,
    required: true,
  },
  currentUserId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['search', 'load-more', 'select'])
const keyword = ref('')

const isSelf = (user) => {
  const targetId = String(user?.userId ?? user?.id ?? '')
  return Boolean(props.currentUserId && targetId === props.currentUserId)
}

const handleSearch = () => {
  emit('search', keyword.value)
}

const handleClear = () => {
  keyword.value = ''
  emit('search', '')
}

const handleScroll = (event) => {
  const target = event.target || event.currentTarget
  if (!target) return
  const { scrollTop, clientHeight, scrollHeight } = target
  if (scrollTop + clientHeight >= scrollHeight - 32) {
    emit('load-more')
  }
}
</script>

<style lang="less" scoped>
.user-select-table {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.section-title {
  display: flex;
  height: 20px;
  align-items: center;
  margin-bottom: 12px;

  img {
    width: 8px;
    height: 8px;
    margin-right: 8px;
    object-fit: contain;
  }

  strong {
    color: #383c41;
    font-size: 14px;
    font-weight: 700;
    line-height: 20px;
  }

  span {
    margin-left: 4px;
    color: #a6acb8;
    font-size: 12px;
    line-height: 20px;
  }

  em {
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-style: normal;
  }
}

.search-row {
  display: flex;
  height: 32px;
  gap: 8px;
  margin-bottom: 12px;

  :deep(.el-input) {
    width: auto;
    flex: 1;
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

  button {
    width: 48px;
    height: 32px;
    flex: none;
    border: 0;
    border-radius: 8px;
    background: #007bff;
    color: #ffffff;
    cursor: pointer;
    font-size: 12px;
    transition: filter 0.2s ease;

    &:hover {
      filter: brightness(1.06);
    }
  }
}

.table-header {
  display: flex;
  height: 36px;
  align-items: center;
  margin-bottom: 2px;
  padding: 0 4px;
  border-radius: 6px;
  background: #f4f6f9;
  color: #9096a2;
  font-size: 14px;
  font-weight: 500;
  box-sizing: border-box;
}

.table-body-wrap {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.people-scroller {
  min-height: 0;
  flex: 1;
  width: 100%;
  overflow-y: auto;
}

.table-row {
  display: flex;
  height: 36px;
  align-items: center;
  margin-bottom: 2px;
  padding: 0 4px;
  color: #222527;
  font-size: 14px;
  box-sizing: border-box;
  transition: background-color 0.15s ease;

  &.is-even {
    border-radius: 8px;
    background: #f4f7f9;
  }

  &:hover {
    border-radius: 8px;
    background: #edf3f8;
  }
}

.col {
  overflow: hidden;
  padding: 0 8px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
}

.col-name {
  width: 80px;
  flex: none;
}

.col-unit {
  min-width: 110px;
  flex: 1;
}

.col-title {
  width: 96px;
  flex: none;
}

.col-phone {
  width: 104px;
  flex: none;
}

.col-action {
  width: 50px;
  flex: none;
  text-align: right;
}

.invite-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #007bff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  white-space: nowrap;

  &:hover:not(:disabled) {
    opacity: 0.8;
  }

  &:disabled {
    color: #a6acb8;
    cursor: default;
  }
}

.scroll-status {
  display: flex;
  height: 32px;
  align-items: center;
  justify-content: center;
  color: #9096a2;
  font-size: 12px;
}

.empty-placeholder {
  display: flex;
  min-height: 160px;
  flex: 1;
  align-items: center;
  justify-content: center;
  color: #9096a2;
  font-size: 14px;
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}
</style>
