<template>
  <article class="plan-card" :class="{ 'is-active': active }" @click="emit('select', data)">
    <div class="card-top">
      <!-- 灾害类型图标 -->
      <img class="hazard-icon" :src="data.hazardIcon" alt="" />

      <div class="card-body">
        <!-- 名称 + 状态标签 -->
        <div class="card-head">
          <h3 class="plan-name" :title="data.name">{{ data.name }}</h3>
          <span class="status-tag" :style="statusStyle">
            <el-icon v-if="statusIcon" class="status-icon">
              <component :is="statusIcon" />
            </el-icon>
            {{ data.status }}
          </span>
        </div>

        <!-- 位置 / 时间（更新时间、发生时间或发布时间） -->
        <ul class="meta-list">
          <li class="meta-row">
            <el-icon class="meta-icon">
              <Location />
            </el-icon>
            <span>{{ data.location }}</span>
          </li>
          <li class="meta-row">
            <img class="meta-icon" :src="iconTime" alt="" />
            <span>{{ timeLabel }}：{{ timeValue }}</span>
          </li>
        </ul>
      </div>

      <!-- 操作按钮：与内容区之间以竖线分隔，上下排列；数量由数据决定 -->
      <div v-if="actions.length" class="card-actions">
        <button v-for="action in actions" :key="action.key" class="action-button" type="button"
          @click.stop="emit('action', { item: data, action })">
          {{ action.label }}
        </button>
      </div>
    </div>

    <!-- 底部状态行：与上方内容以通栏横线分隔
         依赖 updatedItems（已更新项）/ progress（执行进度）字段，
         列表接口暂未返回这两项，此处保持不展示，待接口补齐后自动生效 -->
    <div v-if="data.updatedItems || data.progress" class="card-footer">
      <!-- 已更新完成：本次更新项 -->
      <p v-if="data.updatedItems" class="updated-row">
        已更新{{ data.updatedItems.length }}项：{{ data.updatedItems.join('、') }}
      </p>

      <!-- 更新中：文案 + 进度条 + 百分比同行 -->
      <div v-else class="progress-row">
        <span class="progress-label">执行进度</span>
        <span class="progress-bar"><i :style="{ width: `${data.progress}%` }"></i></span>
        <strong class="progress-value">{{ data.progress }}<em>%</em></strong>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled, Crop, Refresh } from '@element-plus/icons-vue'
import { planStatusMap } from '../config.js'
import iconTime from '@/assets/imgs/emergency/icon-time.png'

defineOptions({ name: 'EmergencyPlanCard' })

const props = defineProps({
  /** 单条预案：{ id, name, status, location, hazardIcon, updateTime|occurTime|publishTime, updatedItems?, progress?, actions? } */
  data: {
    type: Object,
    required: true,
  },
  /** 是否为当前选中项（选中后地图上展示该点位详情） */
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'action'])

/** 按钮字典：不同状态取用不同组合 */
const actionMap = {
  result: { key: 'result', label: '查看更新结果', event: 'view-result' },
  analysis: { key: 'analysis', label: '查看分析过程', event: 'view-analysis' },
  range: { key: 'range', label: '去勾划范围', event: 'draw-range' },
}

/** 默认提供两个入口，数据里给了 actions 则按 actions 渲染 */
const actions = computed(() =>
  (props.data.actions || ['result', 'analysis'])
    .map((key) => actionMap[key])
    .filter(Boolean),
)

/** 状态标签图标：已完成类为实心对勾，更新中为环形箭头，待勾划范围用框选图标 */
const statusIconMap = {
  已更新: CircleCheckFilled,
  已核验: CircleCheckFilled,
  更新中: Refresh,
  待勾划范围: Crop,
}

const statusIcon = computed(() => statusIconMap[props.data.status] || null)

const statusStyle = computed(() => {
  const tone = planStatusMap[props.data.status] || planStatusMap.待更新
  return { color: tone.color, background: tone.background }
})

/** 时间行：卡片统一展示更新时间，突发灾险情保留发生时间/发布时间兜底 */
const timeLabel = computed(() => {
  if (props.data.updateTime) {
    return '更新时间'
  }
  return props.data.occurTime ? '发生时间' : '发布时间'
})

const timeValue = computed(
  () => props.data.updateTime || props.data.occurTime || props.data.publishTime || '',
)
</script>

<style lang="less" scoped>
.plan-card {
  flex-shrink: 0;
  padding: 14px;
  box-sizing: border-box;
  border: 1px solid #e8eef5;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 32, 80, 0.04);
  /* 通栏分隔线需跟随卡片圆角 */
  overflow: hidden;
  font-family: 'AlibabaPuHuiTi', sans-serif;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, filter 0.2s ease;
}

/* 选中态：与隐患处理、监测预警列表的选中反馈保持一致 */
.plan-card.is-active {
  background: #f1f8ff;
  border-color: #007bff;
  filter: drop-shadow(0 2px 10px #1d64b133);
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.hazard-icon {
  flex-shrink: 0;
  display: block;
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.card-body {
  flex: 1 1 auto;
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-name {
  margin: 0;
  max-width: 150px;
  overflow: hidden;
  color: #222527;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 状态标签：小胶囊 + 图标 */
.status-tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  font-weight: 700;

  .status-icon {
    font-size: 12px;
  }
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #9096A2;
  font-size: 12px;
  line-height: 14px;

  .meta-icon {
    flex-shrink: 0;
    display: block;
    width: 12px;
    height: 12px;
    font-size: 12px;
    color: #a6b0bd;
    object-fit: contain;
  }
}

/* 操作按钮：左侧竖线与内容区分隔，上下两行右对齐 */
.card-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid #e8eef5;
}

.action-button {
  height: 24px;
  padding: 0 12px;
  border: 0;
  border-radius: 4px;
  background: #DCEDFF;
  color: #007BFF;
  font-family: inherit;
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.1s ease, color 0.1s ease;

  &:hover {
    background: #007BFF;
    color: #ffffff;
  }
}

/* 底部状态行：通栏横线分隔 */
.card-footer {
  margin: 12px -14px 0;
  padding: 12px 14px 0;
  border-top: 1px solid #e8eef5;
}

/* 已更新项：主色文字 */
.updated-row {
  margin: 0;
  color: #007BFF;
  font-size: 12px;
  line-height: 14px;
  font-weight: 600;
}

/* 执行进度：文案 + 进度条 + 百分比同行 */
.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-label {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.progress-bar {
  flex: 1 1 auto;
  min-width: 0;
  height: 4px;
  border-radius: 3px;
  background: #edf1f5;
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
    transition: width 0.28s ease;
  }
}

.progress-value {
  flex-shrink: 0;
  color: #222527;
  font-size: 14px;
  font-weight: 600;
  font-family: 'Alimama FangYuanTi VF', sans-serif;

  em {
    margin-left: 2px;
    font-size: 12px;
    font-style: normal;
    font-weight: 400;
  }
}
</style>
