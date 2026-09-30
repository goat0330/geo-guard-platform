<template>
  <article class="alarm-card" :class="{ 'is-expanded': expanded, 'is-active': active }">
    <button class="card-head" type="button" @click="emit('toggle', data.id)">
      <i class="head-arrow"></i>
      <span class="hazard-name" :title="data.name">{{ data.name }}</span>
      <span class="level-tag" :style="levelStyle">{{ data.level }}</span>
      <span class="analysis-button" @click.stop="emit('analyze', data)">
        AI数据分析<span
          class="analysis-icon"
          :style="{ maskImage: `url(${arrowIcon})`, WebkitMaskImage: `url(${arrowIcon})` }"
        ></span>
      </span>
    </button>

    <!-- 详情：展开后显示，两列表格（详细地址/处置类型、设备名称/处置人、发布时间/处置时间、有效预警整行）；
         接口未返回的字段统一显示「无数据」 -->
    <div v-if="data.detail" class="card-detail" :class="{ 'is-collapsed': !expanded }">
      <div class="detail-inner">
        <span class="detail-divider"></span>

        <div class="detail-grid">
          <p class="detail-row is-wrap">
            <span class="detail-label">详细地址：</span>{{ displayText(data.detail.address) }}
          </p>
          <p class="detail-row is-wrap">
            <span class="detail-label">处置类型：</span>{{ displayText(data.detail.disposeType) }}
          </p>
          <p class="detail-row is-wrap">
            <span class="detail-label">设备名称：</span>
            <!-- 设备名多为长英文标识、无法折行，单行省略 + title 提示，避免溢出盖住右侧字段 -->
            <span class="detail-value" :title="data.detail.deviceName || ''">
              {{ displayText(data.detail.deviceName) }}
            </span>
          </p>
          <p class="detail-row">
            <span class="detail-label">处置人：</span>{{ displayText(data.detail.disposeUser) }}
          </p>
          <p class="detail-row">
            <span class="detail-label">发布时间：</span>
            <!-- 时间列窄（右列取值区约 104px），带秒会折行：展示到分钟，秒数放 title -->
            <span class="detail-value" :title="data.detail.publishTime || ''">
              {{ displayMinute(data.detail.publishTime) }}
            </span>
          </p>
          <p class="detail-row">
            <span class="detail-label">处置时间：</span>
            <span class="detail-value" :title="data.detail.disposeTime || ''">
              {{ displayMinute(data.detail.disposeTime) }}
            </span>
          </p>
          <p class="detail-row is-full">
            <span class="detail-label">有效告警：</span>
            <span class="valid-tag">{{ displayText(data.detail.validWarning) }}</span>
          </p>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { alarmLevelMap } from '../../config.js'
import arrowIcon from '@/assets/imgs/monitor/icon-arrow-right-btn.webp'

defineOptions({ name: 'AlarmWarningItem' })

const props = defineProps({
  /** 单条预警：{ id, name, level, detail? } */
  data: {
    type: Object,
    required: true,
  },
  expanded: {
    type: Boolean,
    default: false,
  },
  /** 是否为当前选中项 */
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['toggle', 'analyze'])

/** 接口未返回的字段统一显示「无数据」（与设计稿一致） */
const EMPTY_TEXT = '无数据'
const displayText = (value) => (value === null || value === undefined || value === '' ? EMPTY_TEXT : value)

/** 时间取值：接口返回带秒（2026-09-20 08:52:26）时右列取值区约 104px 放不下会折行，
 *  按设计稿精度展示到分钟（设计稿示例即 2026-09-06 15:03），秒数与毫秒放在 title 里可查 */
const displayMinute = (value) => {
  const text = displayText(value)
  const matched = /^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}):\d{2}(?:\.\d+)?$/.exec(text)
  return matched ? `${matched[1]} ${matched[2]}` : text
}

/** 未在字典中的等级使用中性配色兜底 */
const fallbackLevel = { color: '#617185', background: '#f1f4f8' }

const levelStyle = computed(() => {
  const config = alarmLevelMap[props.data.level] || fallbackLevel
  return { color: config.color, background: config.background }
})
</script>

<style lang="less" scoped>
/* 卡片：设计稿 412×202，圆角 12、1px #E4EAEF 描边、#F5F8FA 底 */
.alarm-card {
  margin-bottom: 10px;
  border: 1px solid #e4eaef;
  border-radius: 12px;
  background: #f5f8fa;
  overflow: hidden;
}

/* 展开态卡片内部透明，靠描边区分 */
.alarm-card.is-expanded {
  background: transparent;
}

/* 选中态：浅蓝底 + 主色描边 + 投影（放在展开态之后，展开且选中时以选中态为准） */
.alarm-card.is-active {
  background: #f1f8ff;
  border-color: #007bff;
  filter: drop-shadow(0 2px 10px #1d64b133);
}

.alarm-card.is-active .analysis-button {
  background: #007bff;
  color: #ffffff;
}

/* 头部：设计稿高 50px，左侧 16px 内边距，箭头 16×16，箭头与标题间距 4px */
.card-head {
  display: flex;
  width: 100%;
  height: 50px;
  align-items: center;
  gap: 4px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

/* 折叠箭头：设计稿的 4×8 细箭头，收起朝右、展开朝下 */
.head-arrow {
  position: relative;
  flex-shrink: 0;
  width: 16px;
  height: 16px;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 4px;
    width: 6px;
    height: 6px;
    border-top: 1.5px solid #a6acb8;
    border-right: 1.5px solid #a6acb8;
    transform: translateY(-50%) rotate(45deg);
    transition: transform 0.24s ease;
  }
}

.alarm-card.is-expanded .head-arrow::before {
  transform: translateY(-50%) rotate(135deg);
}

.hazard-name {
  min-width: 0;
  overflow: hidden;
  color: #222527;
  font-size: 14px;
  font-weight: 700;
  line-height: 18px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 等级标签：设计稿 56×18、圆角 4、10px 半粗 */
.level-tag {
  flex-shrink: 0;
  height: 18px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 10px;
  line-height: 18px;
  white-space: nowrap;
}

/* AI 数据分析：设计稿 92×24、圆角 4、#D9EBFF 底，文字与 14×14 箭头居中（间距 2px） */
.analysis-button {
  display: inline-flex;
  width: 92px;
  height: 24px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-left: auto;
  border-radius: 4px;
  background: #d9ebff;
  color: #007bff;
  font-size: 12px;
  line-height: 16px;
  transition: background 0.2s ease;

  .analysis-icon {
    display: block;
    width: 14px;
    height: 14px;
    flex-shrink: 0;
    /* 用遮罩染色，颜色跟随按钮文字：默认蓝、选中白 */
    background: currentColor;
    -webkit-mask-repeat: no-repeat;
    -webkit-mask-position: center;
    -webkit-mask-size: contain;
    mask-repeat: no-repeat;
    mask-position: center;
    mask-size: contain;
  }

  &:hover {
    background: #dbeafe;
  }
}

/* 详情：grid 行高过渡展开收起 */
.card-detail {
  display: grid;
  grid-template-rows: 1fr;
  transition: grid-template-rows 0.24s ease;
}

.card-detail.is-collapsed {
  grid-template-rows: 0fr;
}

.detail-inner {
  min-height: 0;
  overflow: hidden;
}

/* 分割线：设计稿 380×1 #DEE2EC，左右各内缩 16px */
.detail-divider {
  display: block;
  height: 1px;
  margin: 0 16px;
  background: #dee2ec;
}

/* 详情两列：设计稿左列 182 + 右列 162，标签列 60px。卡片 1px 描边会吃掉 2px 内容宽，
   若照搬 380 总宽会让「2026-09-06 15:03」这类取值差 3px 折行，故标签收 2px、列间距收 2px，
   把宽度让给取值列（行高 32px 与设计稿一致） */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 182fr) minmax(0, 162fr);
  column-gap: 34px;
  padding: 12px 16px;
}

.detail-row {
  display: flex;
  min-width: 0;
  align-items: center;
  margin: 0;
  color: #617185;
  font-size: 12px;
  line-height: 32px;
}

.detail-row.is-full {
  grid-column: 1 / -1;
}

/* 长文案行（详细地址）：顶部对齐，多行时标签不跟着居中 */
.detail-row.is-wrap {
  align-items: flex-start;
}

/* 标签宽 58px（设计稿 60px，收 2px 让给取值列），文案固定不换行：避免临界时标签折成两行 */
.detail-label {
  flex-shrink: 0;
  width: 58px;
  color: #a6acb8;
  white-space: nowrap;
}

/* 取值：可收缩并单行省略（完整值由 title 兜底），不挤压、不覆盖右侧字段。
   设备名称与时间取值都用它：设备名是长英文无折行点、时间列窄，靠它保证行高恒为 32px */
.detail-value {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 有效预警：设计稿 48×24 胶囊、12px 字（含 1px 描边，用 border-box 收进 48×24） */
.valid-tag {
  display: inline-flex;
  width: 48px;
  height: 24px;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border: 1px solid #6f86a3;
  border-radius: 100px;
  background: #d5dee9;
  color: #58718f;
  font-size: 12px;
  line-height: 22px;
}
</style>
