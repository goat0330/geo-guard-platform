<template>
  <section class="process-section">
    <!-- 胶囊标题：对勾图标 + 标题 + 展开箭头 -->
    <button
      class="section-title"
      :class="{ 'is-collapsed': !expanded }"
      type="button"
      @click="emit('toggle')"
    >
      <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
      <span class="title-text">{{ step.title }}</span>
      <el-icon class="arrow-icon" :class="{ 'is-collapsed': !expanded }">
        <ArrowDown />
      </el-icon>
    </button>

    <div class="section-body" :class="{ 'is-collapsed': !expanded }">
      <div class="section-inner">
        <!-- 小结：圆点 + 深色结论前缀 + 浅灰正文（待跟进片段为橙色） -->
        <ul class="summary-list">
          <li class="summary-item">
            <span v-if="step.label" class="summary-label">{{ step.label }}</span>
            <span
              v-for="(segment, index) in step.summary"
              :key="index"
              class="summary-text"
              :class="{ 'is-warn': segment.tone === 'warn' }"
            >{{ segment.text }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
defineOptions({ name: 'ExecuteStepSection' })

defineProps({
  /** 单个步骤：{ key, title, label?, summary: [{ text, tone? }] } */
  step: {
    type: Object,
    required: true,
  },
  /** 是否展开 */
  expanded: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['toggle'])
</script>

<style lang="less" scoped>
.process-section {
  flex-shrink: 0;
}

.process-section + .process-section {
  margin-top: 14px;
}

/* 胶囊标题：浅蓝底 + 主色描边与文字（与其它页面折叠块一致） */
.section-title {
  display: flex;
  width: 100%;
  height: 44px;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  box-sizing: border-box;
  border: 1px solid #dcedff;
  border-radius: 8px;
  background: #f5f9ff;
  color: #007bff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;

  &:hover {
    background: #eaf2fe;
  }

  .check-icon {
    flex-shrink: 0;
    font-size: 16px;
  }

  .title-text {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .arrow-icon {
    flex-shrink: 0;
    font-size: 12px;
    color: #007bff;
    transition: transform 0.24s ease;

    &.is-collapsed {
      transform: rotate(180deg);
    }
  }
}

/* 展开动画：grid-template-rows 过渡，无需测量高度 */
.section-body {
  display: grid;
  grid-template-rows: 1fr;
  opacity: 1;
  transition: grid-template-rows 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;

  &.is-collapsed {
    grid-template-rows: 0fr;
    opacity: 0;
  }
}

.section-inner {
  min-height: 0;
  overflow: hidden;
}

/* 小结列表：统一 14px / 行高 24 */
.summary-list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.summary-item {
  position: relative;
  padding-left: 18px;
  color: #9096a2;
  font-size: 14px;
  font-weight: 500;
  line-height: 24px;

  &::before {
    position: absolute;
    top: 0;
    left: 6px;
    color: #9096a2;
    content: '•';
  }
}

/* 结论前缀：深色加粗 */
.summary-label {
  margin-right: 4px;
  color: #383c41;
  font-weight: 600;
}

/* 需人工跟进的片段：橙色加粗 */
.summary-text.is-warn {
  color: #ff922c;
  font-weight: 600;
}
</style>
