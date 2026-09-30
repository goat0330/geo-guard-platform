<template>
  <el-dialog
    :model-value="visible"
    width="920px"
    top="22px"
    align-center
    append-to-body
    class="plan-detail-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-head">
        <div class="head-text">
          <b>{{ data.cardName || '预案卡' }}</b>
          <span class="head-meta">{{ data.title }} · {{ data.version || 'V0.1' }}</span>
        </div>
      </div>
    </template>

    <!-- 预案卡正文：仅用于预览；导出 docx 走 planCardDocx（按数据结构生成，保证表格格式） -->
    <div v-loading="loading" element-loading-text="预案详情加载中..." class="paper-wrap">
      <article v-if="!loading" class="paper">
        <h1 class="paper-title">{{ data.title || '临时处置预案卡' }}</h1>
        <p v-if="data.subtitle" class="paper-subtitle">{{ data.subtitle }}</p>
        <p v-if="data.stage" class="paper-stage">{{ data.stage }}</p>

        <div class="form-grid">
          <template v-for="section in data.sections" :key="section.title">
            <div class="grid-section">
              {{ section.title }}
              <span v-if="section.note" class="section-note">{{ section.note }}</span>
            </div>

            <template v-for="(row, index) in section.rows" :key="`${section.title}-${row.label}-${index}`">
              <div class="grid-label">{{ row.label }}</div>

              <div
                class="grid-value"
                :class="[row.type, { 'is-center': row.center }]"
                :style="{ gridColumn: `span ${row.span || 10}` }"
              >
                <!-- 多列并排的字段：label 紧跟 value，需要让后续字段接在同一行 -->
                <template v-if="row.steps">
                  <div class="steps">
                    <div v-for="step in row.steps" :key="step.title" class="step">
                      <b>{{ step.title }}</b>
                      <span>{{ step.text }}</span>
                    </div>
                  </div>
                </template>

                <template v-else-if="row.routes">
                  <div class="route-row is-head">
                    <div>方案</div>
                    <div>路径</div>
                    <div>距离</div>
                    <div>预计用时</div>
                  </div>
                  <div v-for="route in row.routes" :key="route.plan" class="route-row">
                    <div>{{ route.plan }}</div>
                    <div>{{ route.path }}</div>
                    <div>{{ route.distance }}</div>
                    <div>{{ route.duration }}</div>
                  </div>
                </template>

                <template v-else-if="row.duties">
                  <div class="duty-table">
                    <div class="duty-cell is-head">责任身份</div>
                    <div class="duty-cell is-head">姓名</div>
                    <div class="duty-cell is-head">联系电话</div>
                    <div class="duty-cell is-head">点到点任务</div>
                    <template v-for="duty in row.duties" :key="duty.role">
                      <div class="duty-cell is-role">{{ duty.role }}</div>
                      <div class="duty-cell is-name">{{ duty.name }}</div>
                      <div class="duty-cell is-phone">{{ duty.phone }}</div>
                      <div class="duty-cell is-task">{{ duty.task }}</div>
                    </template>
                  </div>
                </template>

                <!-- 撤离路线示意图：截取自执行结果弹窗内的路线地图 -->
                <template v-else-if="row.image">
                  <img class="route-image" :src="row.image" alt="撤离路线示意图" />
                </template>

                <template v-else>
                  <span>{{ row.value || (row.imageEmpty && !row.value ? row.imageEmpty : '--') }}</span>
                </template>

                <span v-if="row.mark" class="mark" :class="row.mark.type">{{ row.mark.text }}</span>
              </div>
            </template>
          </template>
        </div>

        <p v-if="data.note" class="paper-note">{{ data.note }}</p>
      </article>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <span class="footer-info">{{ data.footerInfo }}</span>
        <div class="footer-actions">
          <button class="footer-button" type="button" @click="handleDownloadImage">图片下载</button>
          <button class="footer-button is-primary" type="button" @click="handleDownloadPlan">预案下载</button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { buildPlanCardDocx } from '../planCardDocx.js'

defineOptions({ name: 'PlanDetailDialog' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  /** 预案卡数据：caseDetail 产物，{ cardName, title, subtitle, stage, image, sections, note, footerInfo } */
  data: {
    type: Object,
    required: true,
  },
  /** 案件详情查询中：正文未就绪时展示加载态 */
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:visible'])

/** 预案下载：优先用接口下发的文档地址，否则按预案卡数据在前端生成 docx（带表格格式） */
const handleDownloadPlan = async () => {
  if (props.data.docUrl) {
    const anchor = document.createElement('a')
    anchor.href = props.data.docUrl
    anchor.download = `${props.data.fileName || '临时处置预案卡'}.docx`
    anchor.click()
    return
  }
  try {
    const blob = await buildPlanCardDocx(props.data)
    saveAs(blob, `${props.data.fileName || '临时处置预案卡'}.docx`)
  } catch (error) {
    console.error('导出预案卡失败', error)
    ElMessage.error('预案导出失败，请稍后重试')
  }
}

/** 图片下载：撤离路线示意图（结果弹窗内路线地图的截图） */
const handleDownloadImage = () => {
  if (!props.data.image) {
    ElMessage.warning('路线示意图尚未生成，请先生成撤离路线')
    return
  }
  const anchor = document.createElement('a')
  anchor.href = props.data.image
  anchor.download = `${props.data.fileName || '临时处置预案卡'}撤离路线图.png`
  anchor.click()
}
</script>

<style lang="less" scoped>
.plan-detail-dialog {
  :deep(.el-dialog) {
    display: flex;
    /* 最高高度：不超过视口 - 44px，大屏上最多 900px，超出后正文内部滚动，头部与底部按钮固定 */
    max-height: min(calc(100vh - 44px), 900px);
    flex-direction: column;
    overflow: hidden;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 22px 76px rgba(20, 39, 60, 0.36);
  }

  :deep(.el-dialog__header) {
    display: flex;
    height: 62px;
    flex: 0 0 62px;
    align-items: center;
    padding: 0 24px;
    margin: 0;
    border-bottom: 1px solid #e2e8ee;
    box-sizing: border-box;
  }

  :deep(.el-dialog__headerbtn) {
    top: 18px;
    right: 20px;
  }

  :deep(.el-dialog__body) {
    flex: 1;
    min-height: 0;
    padding: 24px 34px 34px;
    overflow-y: auto;
  }

  /* 底部操作区不参与高度压缩，保证「预案下载 / 图片下载」始终可见 */
  :deep(.el-dialog__footer) {
    flex: 0 0 66px;
    padding: 0;
  }
}

.dialog-head {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
}

.head-text {
  color: #22272b;
  font-size: 15px;
}

.head-meta {
  margin-left: 12px;
  color: #7d8996;
  font-size: 12px;
}

/* 加载态用 aLoading 遮罩，需给容器留出高度 */
.paper-wrap {
  min-height: 320px;
}

.paper {
  max-width: 810px;
  margin: 0 auto;
}

.paper-title {
  margin: 0 0 7px;
  color: #232323;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1.4;
  text-align: center;
}

.paper-subtitle {
  margin: 0 0 12px;
  color: #778696;
  font-size: 12px;
  text-align: center;
}

.paper-stage {
  margin: 0 0 16px;
  padding: 7px;
  color: #c43636;
  font-weight: 700;
  letter-spacing: 1px;
  text-align: center;
  border: 1px solid #efb2b2;
  background: #fff0f0;
}

/* 12 列网格：与预案卡表格一致，label 2 列 + value 按字段长度跨列 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  border: 2px solid #232323;
}

.grid-section {
  grid-column: span 12;
  padding: 7px;
  color: #2b5278;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 2px;
  text-align: center;
  background: #edf4fa;
  border-bottom: 1px solid #232323;
}

.section-note {
  margin-left: 8px;
  color: #6e7f90;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0;
}

.grid-label {
  grid-column: span 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 9px 8px;
  color: #232323;
  font-weight: 500;
  line-height: 1.75;
  text-align: center;
  background: #fafafa;
  border: 1px solid #232323;
}

.grid-value {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 8px;
  color: #232323;
  font-weight: 600;
  line-height: 1.75;
  word-break: break-word;
  border: 1px solid #232323;

  &.is-center {
    align-items: center;
    text-align: center;
  }

  &.long {
    font-weight: 500;
    line-height: 1.95;
    text-align: justify;
  }

  &.pending {
    background: #fff6e9;
    color: #b86807;
  }

  &.warning {
    background: #fff0f0;
    color: #bc3434;
  }
}

.mark {
  display: inline-flex;
  align-self: flex-start;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  white-space: nowrap;

  &.input {
    background: #e9f4ff;
    color: #1071d0;
  }

  &.db {
    background: #eaf8f2;
    color: #087e59;
  }

  &.gis {
    background: #e9f7ff;
    color: #087ea8;
  }

  &.ai {
    background: #f0ecff;
    color: #6652cc;
  }
}

/* 分阶段行动：三列等宽 */
.steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.step {
  padding: 9px;
  border: 1px solid #cfd9e3;
  border-radius: 4px;
  background: #fff;

  b {
    display: block;
    margin-bottom: 4px;
    color: #15588f;
  }

  span {
    color: #46596d;
    font-size: 12px;
    font-weight: 500;
  }
}

/* 候选撤离路线表头与数据行 */
.route-row {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) 82px 92px;
  border: 1px solid #9aa9b7;

  & + .route-row {
    border-top: 0;
  }

  > div {
    padding: 6px 7px;
    border-right: 1px solid #9aa9b7;

    &:last-child {
      border-right: 0;
    }
  }

  &.is-head {
    background: #f2f6f9;
    font-weight: 600;
    text-align: center;
  }
}

/* 撤离路线示意图：结果弹窗内路线地图的截图 */
.route-image {
  display: block;
  width: 100%;
  border: 1px solid #d8e3ee;
  border-radius: 4px;
}

/* 现场责任人与任务：四列等宽表 */
.duty-table {
  display: grid;
  grid-template-columns: 1.3fr 0.85fr 1.3fr 3.15fr;
  font-size: 11px;
  line-height: 1.65;
}

.duty-cell {
  display: flex;
  align-items: center;
  padding: 8px 7px;
  font-weight: 500;
  border-right: 1px solid #9aa9b7;
  border-bottom: 1px solid #9aa9b7;

  &:nth-child(4n) {
    border-right: 0;
  }

  &.is-head {
    justify-content: center;
    background: #f2f6f9;
    font-weight: 700;
  }

  &.is-role {
    justify-content: center;
    color: #174d77;
    font-weight: 700;
    text-align: center;
    background: #f9fcff;
  }

  &.is-name,
  &.is-phone {
    justify-content: center;
    font-weight: 700;
    text-align: center;
  }

  &.is-task {
    color: #334f69;
  }
}

.paper-note {
  margin: 13px 0 0;
  color: #728294;
  font-size: 11px;
  line-height: 1.8;
}

.dialog-footer {
  display: flex;
  height: 66px;
  align-items: center;
  justify-content: space-between;
  padding: 0 25px;
  border-top: 1px solid #e1e8ef;
  background: #f8fafc;
  box-sizing: border-box;
}

.footer-info {
  color: #68798b;
  font-size: 12px;
}

.footer-actions {
  display: flex;
  gap: 9px;
}

.footer-button {
  padding: 9px 16px;
  border: 1px solid #ccd9e6;
  border-radius: 7px;
  background: #fff;
  color: #46596d;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    border-color: #6eb0ef;
    color: #087cf0;
  }

  &.is-primary {
    border-color: #087cf0;
    background: #087cf0;
    color: #fff;

    &:hover {
      color: #fff;
      opacity: 0.88;
    }
  }
}
</style>
