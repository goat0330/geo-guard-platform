<template>
  <section class="detail-section basic-info-section">
    <div class="section-title">
      <h3>📄 基本信息</h3>
    </div>

    <!-- 假 Loading 骨架屏 -->
    <div v-if="loading" class="basic-skeleton">
      <div class="skeleton-tags">
        <span class="skeleton-tag shimmer"></span>
        <span class="skeleton-tag shimmer"></span>
        <span class="skeleton-tag wide shimmer"></span>
        <span class="skeleton-tag wide shimmer"></span>
        <span class="skeleton-tag wide shimmer"></span>
      </div>
      <div class="skeleton-table shimmer">
        <div v-for="n in 7" :key="n" class="skeleton-row"></div>
      </div>
    </div>

    <!-- 真实内容展示 -->
    <div v-else class="basic-content message-enter">
      <!-- 属性标签栏：只渲染后端真实返回的标签，绝不编造假标签 -->
      <div v-if="tagList.length > 0" class="tag-list">
        <span
          v-for="(tag, idx) in tagList"
          :key="idx"
          class="tag-item"
          :class="getTagClass(idx)"
        >
          {{ tag }}
        </span>
      </div>

      <!-- 结构化网格信息表格卡片 -->
      <div class="info-table-card">
        <div class="table-row">
          <div class="cell-label">灾险情名称</div>
          <div class="cell-value" :title="info.disasterName">{{ info.disasterName }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">灾险情编号</div>
          <div class="cell-value">{{ info.disasterCode }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">灾害类型</div>
          <div class="cell-value">{{ info.disasterType }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">诱因</div>
          <div class="cell-value" :title="info.cause">{{ info.cause }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">发生时间</div>
          <div class="cell-value">{{ info.occurrenceTime }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">所属区县</div>
          <div class="cell-value">{{ info.county }}</div>
        </div>

        <div class="table-row">
          <div class="cell-label">详细地址</div>
          <div class="cell-value" :title="info.detailedAddress">{{ info.detailedAddress }}</div>
        </div>

        <!-- 双列行：灾险情等级 / 稳定状态 -->
        <div class="table-row split-row">
          <div class="half-col">
            <div class="cell-label">灾险情等级</div>
            <div class="cell-value">{{ info.disasterLevel }}</div>
          </div>
          <div class="half-col">
            <div class="cell-label border-left">稳定状态</div>
            <div class="cell-value">{{ info.stabilityStatus }}</div>
          </div>
        </div>

        <!-- 双列行：时间类型 / 隐患点名称 -->
        <div class="table-row split-row">
          <div class="half-col">
            <div class="cell-label">时间类型</div>
            <div class="cell-value">{{ info.timeType }}</div>
          </div>
          <div class="half-col">
            <div class="cell-label border-left">隐患点名称</div>
            <div class="cell-value">{{ info.hiddenDangerName }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'DetailBasicInfo' })

const props = defineProps({
  basicInfo: {
    type: Object,
    default: () => ({}),
  },
  tags: {
    type: Array,
    default: () => [],
  },
  event: {
    type: Object,
    default: () => ({}),
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const formatVal = (v) => {
  if (v === null || v === undefined || v === '') return '--'
  return String(v)
}

const info = computed(() => {
  const b = props.basicInfo || {}
  const e = props.event || {}
  return {
    disasterName: formatVal(b.disasterName || e.eventName || e.title),
    disasterCode: formatVal(b.disasterCode || e.id),
    disasterType: formatVal(b.disasterType || e.eventTypeName || e.disasterType || e.type),
    cause: formatVal(b.cause || e.cause),
    occurrenceTime: formatVal(b.occurrenceTime || e.occurrenceTime || e.time),
    county: formatVal(b.county || e.county),
    detailedAddress: formatVal(b.detailedAddress || e.displayAddress || e.detailedAddress || e.location),
    disasterLevel: formatVal(b.disasterLevel || e.scaleLevel || e.eventLevelName || e.scale),
    stabilityStatus: formatVal(b.stabilityStatus),
    timeType: formatVal(b.timeType),
    hiddenDangerName: formatVal(b.hiddenDangerName),
  }
})

const TAG_CLASSES = ['tag-danger', 'tag-warning', 'tag-success', 'tag-orange']
const getTagClass = (idx) => TAG_CLASSES[idx % TAG_CLASSES.length]

const tagList = computed(() => {
  if (Array.isArray(props.tags) && props.tags.length > 0) {
    return props.tags.filter(Boolean)
  }
  const list = []
  const b = props.basicInfo || {}
  const e = props.event || {}
  const type = b.disasterType || e.eventTypeName || e.disasterType || e.type
  const level = b.disasterLevel || e.scaleLevel || e.eventLevelName || e.scale
  const sus = b.susceptibilityLevel
  const risk = b.riskLevelName
  if (type) list.push(type)
  if (level) list.push(level)
  if (sus) list.push(sus)
  if (risk) list.push(risk)
  return list
})
</script>

<style lang="less" scoped>
.basic-info-section {
  margin-top: 18px;
}

.section-title {
  margin-bottom: 10px;

  h3 {
    margin: 0;
    color: #383c41;
    font-size: 16px;
    font-weight: 700;
    line-height: 20px;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-item {
  height: 18px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  line-height: 10px;
  white-space: nowrap;

  &.tag-danger {
    background: rgba(226, 48, 48, 0.16);
    color: #dd4739;
  }

  &.tag-warning {
    background: rgba(255, 184, 78, 0.16);
    color: #ffae00;
  }

  &.tag-success {
    background: rgba(68, 182, 153, 0.16);
    color: #44b699;
  }

  &.tag-orange {
    background: rgba(255, 146, 44, 0.16);
    color: #ff8a1d;
  }
}

.info-table-card {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #fbfcff;
  overflow: hidden;
}

.table-row {
  height: 38px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e5e8ec;
  box-sizing: border-box;

  &:last-child {
    border-bottom: 0;
  }
}

.cell-label {
  flex: 0 0 91px;
  width: 91px;
  height: 100%;
  padding: 0 12px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  background: #f4f9ff;
  border-right: 1px solid #e4eaef;
  color: #878898;
  font-size: 14px;
  font-weight: 400;
  white-space: nowrap;

  &.border-left {
    border-left: 1px solid #e4eaef;
  }
}

.cell-value {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
  color: #383c41;
  font-size: 14px;
  font-weight: 400;
  line-height: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.split-row {
  display: flex;

  .half-col {
    flex: 1;
    min-width: 0;
    height: 100%;
    display: flex;
    align-items: center;
  }
}

/* 假 loading 骨架屏 */
.basic-skeleton {
  width: 100%;
}

.skeleton-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;

  .skeleton-tag {
    width: 44px;
    height: 18px;
    border-radius: 4px;
    background: #edf1f6;

    &.wide {
      width: 64px;
    }
  }
}

.skeleton-table {
  width: 100%;
  height: 342px;
  border-radius: 12px;
  border: 1px solid #e4eaef;
  background: #fbfcff;
  padding: 8px 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}

.skeleton-row {
  width: 100%;
  height: 24px;
  border-radius: 4px;
  background: #edf1f6;
}

.shimmer {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.6) 50%,
      rgba(255, 255, 255, 0) 100%
    );
    animation: shimmer-swipe 1.4s infinite;
  }
}

@keyframes shimmer-swipe {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.message-enter {
  animation: message-enter 0.24s ease both;
}

@keyframes message-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
