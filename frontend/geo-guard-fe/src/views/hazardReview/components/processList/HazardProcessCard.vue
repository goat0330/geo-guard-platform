<template>
  <article class="process-card" :class="{ 'is-active': active }" @click="emit('select', data)">
    <!-- 标题 + 状态标签 + AI 处理入口 -->
    <div class="card-head">
      <h3 class="hazard-name" :title="data.name">{{ data.name }}</h3>
      <HazardStatusTag :status="data.status" />
      <button class="process-button" type="button" @click.stop="emit('process', data)">
        AI处理<img class="btn-icon" src="@/assets/imgs/hazardReview/icon-arrow-right-btn.webp" alt="" />
      </button>
    </div>

    <!-- 隐患点位置 / 类型 -->
    <ul class="meta-list">
      <li class="meta-row">
        <el-icon class="meta-icon">
          <Location />
        </el-icon>
        <span>隐患点位置：{{ data.location }}</span>
      </li>
      <li class="meta-row">
        <img class="meta-icon" src="@/assets/imgs/hazardReview/icon-time.webp" alt="" />
        <span>隐患点类型：{{ data.hazardType }}</span>
      </li>
    </ul>

    <!-- 处理结论：变化（橙）/ 处理结果（蓝）；接口未返回摘要时整块（含分隔线）不渲染，避免留下空白分隔线 -->
    <template v-if="data.result">
      <span class="line"></span>

      <p class="result-row" :class="data.result.tone">
        {{ data.result.label }}：{{ data.result.text }}
      </p>
    </template>
  </article>
</template>

<script setup>
import HazardStatusTag from './HazardStatusTag.vue'

defineProps({
  /** 单条隐患数据：{ id, name, status, location, hazardType, result } */
  data: {
    type: Object,
    required: true,
  },
  /** 是否为当前选中项（选中后地图上展示该隐患点详情） */
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['process', 'select'])
</script>

<style lang="less" scoped>
.process-card {
  flex-shrink: 0;
  padding: 14px;
  box-sizing: border-box;
  border: 1px solid #e8eef5;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 32, 80, 0.04);
  font-family: 'AlibabaPuHuiTi', sans-serif;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, filter 0.2s ease;
}

/* 选中态：浅蓝底 + 主色描边 + 投影，与监测预警列表的选中反馈一致 */
.process-card.is-active {
  background: #f1f8ff;
  border-color: #007bff;
  filter: drop-shadow(0 2px 10px #1d64b133);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hazard-name {
  margin: 0;
  max-width: 150px;
  overflow: hidden;
  color: #222527;
  font-size: 14px;
  font-weight: 800;
  line-height: 22px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.process-button {
  position: relative;
  z-index: 0;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 74px;
  height: 26px;
  border-radius: 8px;
  margin-left: auto;
  padding: 0 10px;
  border: 0;
  background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
  color: #ffffff;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;

  /* hover 渐变层：渐变无法参与 transition 插值，直接替换会跳帧闪烁，
     改用叠加层过渡 opacity，得到平滑切换效果 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: -1;
    border-radius: inherit;
    background: linear-gradient(90deg, #3395ff 12.5%, #40c4ff 100%);
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover::before {
    opacity: 1;
  }

  .btn-icon {
    display: block;
    width: 10px;
    height: 10px;
    /* 图标不参与命中测试与拖拽，避免鼠标移到图片上时抖动/拖影 */
    pointer-events: none;
    user-select: none;
    -webkit-user-drag: none;
  }
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 10px 0 0;
  padding: 0;
  list-style: none;


}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #9096A2;
  font-size: 12px;
  line-height: 18px;

  .meta-icon {
    flex-shrink: 0;
    color: #a6b0bd;
    font-size: 12px;
  }

  /* 图片图标固定 12px，避免被 flex 压缩 */
  img.meta-icon {
    width: 12px;
    height: 12px;
    object-fit: contain;
  }
}

.line {
  display: block;
  width: 380px;
  height: 1px;
  margin: 10px 0 0;
  background: #dee2ec;
}

.result-row {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;

  &.warning {
    color: #ff922c;
  }

  &.info {
    color: #007bff;
  }
}
</style>
