<template>
  <section class="detail-section timeline-section">
    <div class="section-title">
      <h3>📍 处置时间线</h3>
      <span v-if="displayNodeCount" class="node-count">{{ displayNodeCount }} 个节点</span>
    </div>

    <div class="timeline">
      <DetailTimelineItem
        v-for="(item, index) in items"
        :key="item.code || `${item.step || item.title || index}-${item.occurredAt || item.time}`"
        :item="item"
        :index="index"
        :is-last="index === items.length - 1"
        :is-expanded="isExpanded(item, index)"
        @toggle-expand="toggleExpand(item, index)"
      />

      <!-- 时间线加载中提示动画 -->
      <div v-if="loading" class="analysis-loading" aria-label="正在整理处置时间线">
        <i></i>
        <i></i>
        <i></i>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DetailTimelineItem from './DetailTimelineItem.vue'

defineOptions({ name: 'DetailTimeline' })

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  totalNodesCount: {
    type: Number,
    default: 0,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const displayNodeCount = computed(() => props.totalNodesCount || props.items?.length || 0)

// 控制节点展开/收起状态（记录每个节点的展开状态）
const expandedMap = ref({})

/**
 * 判断节点是否展开：
 * 1. 优先使用用户点击记录的值
 * 2. 其次使用数据自带的 open 属性（若有）
 * 3. 默认全部节点展开
 */
const isExpanded = (item, index) => {
  if (expandedMap.value[index] !== undefined) {
    return expandedMap.value[index]
  }
  if (item.open !== undefined) {
    return item.open
  }
  return true
}

const toggleExpand = (item, index) => {
  expandedMap.value[index] = !isExpanded(item, index)
}

// 切换事件列表时重置展开状态映射
watch(
  () => props.items,
  () => {
    expandedMap.value = {}
  },
)
</script>

<style lang="less" scoped>
.timeline-section {
  margin-top: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    color: #383c41;
    font-size: 16px;
    font-weight: 700;
    line-height: 20px;
  }

  .node-count {
    color: #9096a2;
    font-size: 10px;
  }
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.analysis-loading {
  height: 34px;
  padding-left: 22px;
  display: flex;
  align-items: center;
  gap: 4px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #9096a2;
    animation: analysis-loading 1.2s infinite ease-in-out;
  }

  i:nth-child(2) {
    animation-delay: 0.16s;
  }

  i:nth-child(3) {
    animation-delay: 0.32s;
  }
}

@keyframes analysis-loading {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}
</style>
