<template>
  <Transition name="consultation-bar-slide">
    <div v-if="visible" class="emergency-consultation-bar" role="region" aria-label="应急会商室控制条">
      <!-- 左侧：标识、标题、时钟与信号 -->
      <div class="bar-left">
        <i class="point-title"></i>
        <span class="bar-title">{{ meetingTitle || '应急会商室' }}</span>
        <span class="bar-time num-font">{{ currentTime }}</span>
        <SignalStrength :model-value="4" class="bar-signal" />
      </div>

      <!-- 右侧：参会人员名单、拉人、视频会议按钮(图三)、收起与退出 -->
      <div class="bar-right">
        <!-- 参会人员名单 -->
        <div class="member-summary" :title="allMemberNames">
          <span class="summary-label">会议人员：</span>
          <span class="summary-names">{{ memberNamesDisplay || '暂无成员' }}</span>
        </div>

        <span class="split-line"></span>

        <!-- 中途拉人按钮 -->
        <button
          type="button"
          class="icon-action-btn"
          title="邀请人员加入会商"
          aria-label="邀请人员"
          @click="emit('invite-member')"
        >
          <i class="iconfont icon-add-person"></i>
        </button>

        <span class="split-line"></span>

        <!-- 开启视频会议按钮（图三红框位置：绿色电话方块） -->
        <img
          :src="phoneIcon"
          alt="开启视频会议"
          class="green-square"
          title="开启视频会议"
          role="button"
          tabindex="0"
          @click="emit('open-video')"
          @keydown.enter="emit('open-video')"
        />

        <span class="split-line"></span>

        <!-- 会中转写与会后智能纪要 -->
        <button
          type="button"
          class="record-action-btn"
          title="查看语音转写与智能会议纪要"
          @click="emit('open-record')"
        >
          <i class="iconfont icon-file"></i>
          会议记录
        </button>

        <span class="split-line"></span>

        <button
          type="button"
          class="record-action-btn"
          title="查看历史会议"
          @click="emit('open-history')"
        >
          <i class="iconfont icon-history"></i>
          历史会议
        </button>

        <span class="split-line"></span>

        <!-- 收起按钮 -->
        <button
          type="button"
          class="icon-action-btn"
          title="收起应急会商室"
          aria-label="收起应急会商室"
          @click="emit('fold')"
        >
          <i class="iconfont icon-fold"></i>
        </button>

        <span class="split-line"></span>

        <!-- 结束/退出按钮 -->
        <button
          type="button"
          class="text-action-btn"
          :class="{ 'is-danger': isMainer }"
          :title="isMainer ? '结束全员会议' : '退出会商室'"
          @click="emit('close-meeting')"
        >
          {{ isMainer ? '结束会议' : '退出会商' }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { useNow } from '@/hooks/useNow.js'
import SignalStrength from '@/components/SignalStrength/index.vue'
import phoneIcon from '@/assets/imgs/emergency/phone.png'

const props = defineProps({
  visible: {
    type: Boolean,
    default: true,
  },
  meetingTitle: {
    type: String,
    default: '应急会商室',
  },
  participants: {
    type: Array,
    default: () => [],
  },
  isMainer: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-video', 'open-record', 'open-history', 'invite-member', 'fold', 'close-meeting'])

const { currentTime } = useNow('HH:mm:ss')

// 过滤有效成员
const activeParticipants = computed(() => {
  return (props.participants || []).filter((p) => p.status !== 'REMOVED')
})

const allMemberNames = computed(() => {
  return activeParticipants.value
    .map((p) => p.name || p.displayName || p.username || '成员')
    .join('、')
})

const memberNamesDisplay = computed(() => {
  const names = activeParticipants.value.map((p) => p.name || p.displayName || p.username || '成员')
  if (!names.length) return '暂无成员'
  if (names.length <= 4) return names.join('、')
  return `${names.slice(0, 4).join('、')} 等${names.length}人`
})
</script>

<style lang="less" scoped>
.emergency-consultation-bar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  width: min(1180px, calc(100% - 48px));
  height: 60px;
  padding: 0 24px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 32, 80, 0.12);
  border: 1px solid #eef2f8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.num-font {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}

/* 左侧内容 */
.bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.point-title {
  width: 6px;
  height: 6px;
  background: #007BFF;
  border-radius: 2px;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    width: 100%;
    height: 100%;
    background: rgba(0, 123, 255, 0.25);
    border-radius: 2px;
  }
}

.bar-title {
  font-size: 16px;
  font-weight: 700;
  color: #222527;
}

.bar-time {
  font-size: 16px;
  color: #617185;
}

.bar-signal {
  margin-left: 6px;
}

/* 右侧内容 */
.bar-right {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.member-summary {
  display: flex;
  align-items: center;
  max-width: 420px;
  font-size: 14px;
  color: #617185;
  overflow: hidden;
  white-space: nowrap;
}

.summary-label {
  color: #617185;
  flex-shrink: 0;
}

.summary-names {
  color: #222527;
  overflow: hidden;
  text-overflow: ellipsis;
}

.split-line {
  width: 1px;
  height: 14px;
  background: #e1e6ee;
  flex-shrink: 0;
}

.icon-action-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #617185;
  font-size: 18px;
  transition: all 0.2s;

  &:hover {
    color: #007BFF;
    background: #f0f6ff;
  }

  i {
    font-size: 18px;
  }
}

.record-action-btn {
  display: flex;
  height: 32px;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #617185;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;

  &:hover {
    color: #007BFF;
    background: #E0EEFA;
  }

  i {
    font-size: 16px;
  }
}

/* 图三位置的绿色电话按钮（恩施地灾规范） */
.green-square {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  display: block;
  transition: transform 0.2s, filter 0.2s;

  &:hover {
    transform: scale(1.1);
    filter: brightness(1.06);
  }

  &:active {
    transform: scale(0.95);
  }
}

.text-action-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #e1e6ee;
  background: #ffffff;
  border-radius: 6px;
  color: #617185;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #007BFF;
    border-color: #007BFF;
  }

  &.is-danger {
    color: #ff4d4f;
    border-color: #ffa39e;
    background: #fff1f0;

    &:hover {
      background: #ff4d4f;
      color: #ffffff;
      border-color: #ff4d4f;
    }
  }
}

/* 进出过渡动画 */
.consultation-bar-slide-enter-active,
.consultation-bar-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.consultation-bar-slide-enter-from,
.consultation-bar-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, 20px);
}
</style>
