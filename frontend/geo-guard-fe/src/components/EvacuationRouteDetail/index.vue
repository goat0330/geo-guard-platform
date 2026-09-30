<template>
  <aside class="route-detail-panel">
    <div v-if="!routes.length" class="empty-state">暂无撤离路线</div>
    <article
      v-for="(route, index) in routes"
      :key="route.id || index"
      class="route-item"
      :class="{ expanded: expandedIndex === index }"
    >
      <button class="route-header" type="button" @click="emit('toggle', index)">
        <img class="route-icon" :src="routeIcon" alt="撤离路线" />
        <strong>
          {{
            expandedIndex === index ? `撤离路线${index + 1}–详情信息` : `撤离路线${index + 1}（共${route.peopleCount}）`
          }}
        </strong>
        <span class="expand-action">
          {{ expandedIndex === index ? '收起' : '展开' }}
          <el-icon><ArrowUp v-if="expandedIndex === index" /><ArrowDown v-else /></el-icon>
        </span>
      </button>

      <Transition name="route-info">
        <div v-if="expandedIndex === index" class="route-information">
          <dl class="route-info-grid">
            <div>
              <dt><i class="iconfont icon-a-Localyidingwei1"></i>撤离起点</dt>
              <dd>{{ route.evacuationArea }}</dd>
            </div>
            <div>
              <dt><i class="iconfont icon-username"></i>人员数量</dt>
              <dd class="number-text">{{ route.peopleCount }}</dd>
            </div>
            <div>
              <dt><i class="iconfont icon-a-Localyidingwei1"></i>撤离目标点</dt>
              <dd>{{ route.resettlementPoint }}</dd>
            </div>
            <div>
              <dt><i class="iconfont icon-a-Wheelchairlunyi"></i>特殊人群</dt>
              <dd>{{ route.specialPeople }}</dd>
            </div>
            <div>
              <dt><i class="iconfont icon-a-Rulerchizi"></i>距离</dt>
              <dd class="number-text">{{ route.distance }}</dd>
            </div>
            <div>
              <dt><i class="iconfont icon-knock"></i>预计时间（步行）</dt>
              <dd class="number-text">{{ route.walkTime }}</dd>
            </div>
          </dl>
          <div class="route-description">
            <span><i class="iconfont icon-a-Map-distanceditujuli"></i>路线描述</span>
            <p>{{ route.evacuationDirection }}</p>
          </div>
        </div>
        <div v-else class="route-summary">
          <span class="route-place"><i class="iconfont icon-a-Localyidingwei1"></i>{{ route.evacuationArea }}</span>
          <span class="route-connector" aria-hidden="true"><i></i><b></b></span>
          <span class="route-place"><i class="iconfont icon-a-Localyidingwei1"></i>{{ route.resettlementPoint }}</span>
        </div>
      </Transition>
    </article>
  </aside>
</template>

<script setup>
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import routeIcon from '@/assets/imgs/emergency/evacuation-route-icon.png'

defineOptions({ name: 'EvacuationRouteDetail' })

defineProps({
  routes: { type: Array, default: () => [] },
  expandedIndex: { type: Number, default: -1 },
})

const emit = defineEmits(['toggle'])
</script>

<style lang="less" scoped>
.route-detail-panel {
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-color: #dcedff transparent;
  scrollbar-width: thin;
}

.route-item {
  overflow: hidden;
  margin-bottom: 12px;
  border: 1px solid #e0eefa;
  border-radius: 14px;
  background: #ffffff;
  transition:
    border-color 0.22s ease,
    box-shadow 0.22s ease;

  &.expanded {
    border-color: #007bff;
    box-shadow: 0 4px 14px rgba(0, 123, 255, 0.08);
  }
}

.route-header {
  display: flex;
  width: 100%;
  min-height: 62px;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 0;
  background: transparent;
  color: #222527;
  cursor: pointer;
  text-align: left;

  strong {
    min-width: 0;
    flex: 1;
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.expanded .route-header strong,
.expanded .expand-action {
  color: #007bff;
}

.route-icon {
  display: block;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
}

.expand-action {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
  color: #007bff;
  font-size: 12px;
  line-height: 20px;
}

.route-information {
  padding: 0 18px 16px;
  border-top: 1px solid #e0eefa;
}

.route-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;

  > div {
    min-width: 0;
    padding: 14px 16px 0 0;

    &:nth-child(even) {
      padding-right: 0;
      padding-left: 24px;
      border-left: 1px dashed #dcedff;
    }
  }

  dt {
    display: flex;
    align-items: center;
    gap: 5px;
    color: #9096a2;
    font-size: 12px;
    line-height: 20px;

    .iconfont {
      color: #a6acb8;
      font-size: 14px;
    }
  }

  dd {
    margin: 3px 0 0;
    overflow: hidden;
    color: #383c41;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.number-text {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
}

.route-description {
  padding-top: 10px;

  span {
    display: flex;
    align-items: center;
    gap: 5px;
    color: #9096a2;
    font-size: 12px;
    line-height: 20px;

    .iconfont {
      color: #a6acb8;
      font-size: 14px;
    }
  }

  p {
    margin: 3px 0 0;
    color: #383c41;
    font-size: 14px;
    line-height: 22px;
  }
}

.route-summary {
  display: flex;
  height: 32px;
  align-items: center;
  gap: 8px;
  padding: 0 18px 12px 68px;
  box-sizing: content-box;
  color: #9096a2;
  font-size: 12px;

  .route-place {
    display: inline-flex;
    min-width: 0;
    max-width: 40%;
    align-items: center;
    gap: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    .iconfont {
      color: #a6acb8;
      font-size: 14px;
    }
  }

  .route-connector {
    display: flex;
    min-width: 18px;
    flex: 1;
    align-items: center;

    i {
      width: 100%;
      border-top: 1px dashed #007bff;
    }

    b {
      width: 0;
      height: 0;
      margin-left: -1px;
      border-top: 3px solid transparent;
      border-bottom: 3px solid transparent;
      border-left: 5px solid #007bff;
    }
  }
}

.route-info-enter-active,
.route-info-leave-active {
  overflow: hidden;
  transition:
    opacity 0.2s ease,
    transform 0.22s ease;
}

.route-info-enter-from,
.route-info-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.empty-state {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #9096a2;
  font-size: 14px;
}
</style>
