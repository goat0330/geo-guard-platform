<template>
  <div class="range-confirm">
    <h2 class="confirm-title">{{ title }}</h2>

    <!-- 险情信息 + 勾划面积 -->
    <div class="point-block">
      <div class="point-info">
        <img v-if="icon" class="point-icon" :src="icon" alt="" />
        <span class="point-name" :title="name">{{ name }}</span>
      </div>
      <p class="area-info">
        {{ areaLabel }}<strong>{{ area }}</strong>
      </p>
    </div>

    <p class="tags-title">确认后将获取：</p>
    <div class="tag-list">
      <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
    </div>

    <footer class="confirm-footer">
      <button class="ghost-button" type="button" @click="emit('cancel')">{{ actionText.cancel }}</button>
      <button class="outline-button" type="button" @click="emit('redraw')">{{ actionText.redraw }}</button>
      <button class="primary-button" type="button" @click="emit('confirm')">{{ actionText.confirm }}</button>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'RangeConfirmPopup' })

const props = defineProps({
  /** 弹窗标题 */
  title: {
    type: String,
    default: '确认本次资料检索范围',
  },
  /** 险情名称 */
  name: {
    type: String,
    default: '',
  },
  /** 灾害类型图标 */
  icon: {
    type: String,
    default: '',
  },
  /** 勾划面积（含单位，如 0.36km²） */
  area: {
    type: String,
    default: '0km²',
  },
  /** 面积前缀文案 */
  areaLabel: {
    type: String,
    default: '勾划面积',
  },
  /** 确认后可获取的资料项 */
  tags: {
    type: Array,
    default: () => [],
  },
  /** 底部按钮文案 */
  actions: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['cancel', 'redraw', 'confirm'])

const actionText = computed(() => ({
  cancel: '取消',
  redraw: '重绘范围',
  confirm: '确认范围并生成',
  ...props.actions,
}))
</script>

<style lang="less" scoped>
.range-confirm {
  width: 480px;
  max-width: 100%;
  box-sizing: border-box;
  padding: 24px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 10px 32px rgba(0, 32, 80, 0.18);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.confirm-title {
  margin: 0;
  color: #1f2430;
  font-size: 18px;
  font-weight: 800;
  line-height: 26px;
}

/* 险情信息块：浅灰底卡片 */
.point-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 10px;
  background: #f5f7fa;
}

.point-info {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.point-icon {
  flex-shrink: 0;
  display: block;
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.point-name {
  min-width: 0;
  overflow: hidden;
  color: #222527;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.area-info {
  flex-shrink: 0;
  margin: 0;
  color: #9096a2;
  font-size: 12px;
  line-height: 22px;
  white-space: nowrap;

  strong {
    margin-left: 4px;
    color: #007bff;
    font-size: 16px;
    font-weight: 800;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
  }
}

.tags-title {
  margin: 16px 0 0;
  color: #617185;
  font-size: 12px;
  line-height: 18px;
}

/* 资料项标签：浅蓝底 + 主色文字 */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 6px;
  background: #eaf2fe;
  color: #007bff;
  font-size: 12px;
  line-height: 28px;
  white-space: nowrap;
}

.confirm-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.ghost-button,
.outline-button,
.primary-button {
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.ghost-button {
  margin-left: auto;
  border: 1px solid #e4e8ee;
  background: #ffffff;
  color: #617185;

  &:hover {
    border-color: #c8d2e0;
    color: #222527;
  }
}

.outline-button {
  border: 1px solid #007bff;
  background: #ffffff;
  color: #007bff;

  &:hover {
    background: #eff6ff;
  }
}

.primary-button {
  border: 0;
  background: #007bff;
  color: #ffffff;

  &:hover {
    background: #3395ff;
  }
}
</style>
