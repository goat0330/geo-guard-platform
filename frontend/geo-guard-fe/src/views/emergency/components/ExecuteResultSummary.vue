<template>
  <div class="result-summary">
    <!-- 完成提示 -->
    <h3 class="done-title">
      <el-icon class="done-icon">
        <CircleCheckFilled />
      </el-icon>{{ data.title }}
    </h3>
    <p class="done-desc">{{ data.desc }}</p>

    <!-- 两个结果卡片：预案更新稿 / 最新撤离路线 -->
    <div class="panel-grid">
      <div v-for="panel in data.panels" :key="panel.index" class="panel-item">
        <section class="panel-card" >
          <header class="panel-head">
            <span class="panel-index">{{ panel.index }}</span>
            <div class="panel-caption">
              <h4 class="panel-title">{{ panel.title }}</h4>
              <p v-if="panel.desc" class="panel-desc">{{ panel.desc }}</p>
            </div>
          </header>

          <!-- 更新项列表：图标 + 标签 + 原值 → 新值 -->
          <ul v-if="panel.items?.length" class="change-list">
            <li v-for="item in panel.items" :key="item.label" class="change-row">
              <img v-if="item.icon" class="row-icon" :src="item.icon" alt="" />
              <span class="change-label">{{ item.label }}</span>
              <span class="change-value">
                <template v-if="item.from">
                  <strong>{{ item.from }}</strong>
                  <svg class="arrow" viewBox="0 0 18 8" fill="none" aria-hidden="true">
                    <path d="M0 4h15.6M11.9.6 15.6 4l-3.7 3.4" stroke="#007BFF" stroke-width="1.5"
                      stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </template>
                <strong class="is-to" :class="{ 'is-highlight': item.highlight }">{{ item.to }}</strong>
              </span>
            </li>
          </ul>

          <!-- 撤离路线预览：复用详情弹窗的路线地图，但不创建人员动画、不响应点选。
               与上方路线列表并存，不能用 v-else，否则有路线时预览区不渲染 -->
          <div v-if="panel.routeData" class="route-block">
            <div class="route-map-frame">
              <EvacuationRouteMap class="route-map-preview" :data="panel.routeData" :play-animation="false"
                :interactive="false" />
            </div>
          </div>

          <!-- 接口未返回入口文案时不渲染，避免出现只有箭头的空链接 -->
          <footer v-if="panel.link" class="panel-footer">
            <button class="panel-link" type="button" @click="emit('action', panel.link)">
              {{ panel.link }}
              <img class="link-arrow" :src="arrowRightIcon" alt="" />
            </button>
          </footer>
        </section>
      </div>
    </div>

    <!-- 卡片下方说明行：待核实提示 + 各卡片的补充说明，按卡片列对齐 -->
    <div v-if="footnotes.length" class="panel-footnotes">
      <p v-for="item in footnotes" :key="item.text" class="panel-footnote" :class="`is-${item.type}`">
        <img v-if="item.type === 'notice'" class="panel-footnote-icon" :src="noticeAlertIcon" alt="" />
        {{ item.text }}
      </p>
    </div>

    <!-- 结果文件：接口未返回文件时整块不渲染 -->
    <template v-if="data.files?.length">
      <p class="files-title">{{ data.filesTitle }}</p>
      <div class="file-list">
        <div v-for="file in data.files" :key="file.name" class="file-card">
          <!-- 背景光晕：设计稿的蓝色模糊光斑（卡片右下角） -->
          <span class="file-glow"></span>

          <!-- 图片附件：缩略图即截图本身，点击放大预览；其余文件用类型图标 -->
          <el-image
            v-if="file.src"
            class="file-icon file-image"
            :src="file.src"
            :preview-src-list="[file.src]"
            preview-teleported
            hide-on-click-modal
            fit="cover"
          />
          <img v-else class="file-icon" :src="file.icon || fileWordIcon" alt="" />

          <div class="file-text">
            <span class="file-name" :title="file.name">{{ file.name }}</span>
            <span class="file-meta">{{ file.size ? `${file.type} · ${file.size}` : file.type }}</span>
          </div>

          <button class="download-button" type="button" title="下载" @click="emit('action', file)"></button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import arrowRightIcon from '@/assets/imgs/emergency/icon-arrow-right.svg'
import noticeAlertIcon from '@/assets/imgs/emergency/icon-notice-alert.svg'
import fileWordIcon from '@/assets/imgs/emergency/icon-file-word.svg'
import EvacuationRouteMap from '@/components/EvacuationRouteMap/index.vue'

defineOptions({ name: 'ExecuteResultSummary' })

const props = defineProps({
  /** 完成态数据：{ title, desc, panels, notice, filesTitle, files } */
  data: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['action'])

/** 卡片下方说明行：首个为待核实提示，之后按卡片顺序排列各卡片的补充说明。
 *  与上方卡片共用两列网格，因此每一项都与其对应卡片同列对齐 */
const footnotes = computed(() => {
  const list = []
  if (props.data.notice) {
    list.push({ type: 'notice', text: props.data.notice })
  }
  props.data.panels.forEach((panel) => {
    if (panel.caption) {
      list.push({ type: 'note', text: panel.caption })
    }
  })
  return list
})
</script>

<style lang="less" scoped>
.result-summary {
  margin-top: 24px;
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 完成提示：绿色标题 */
.done-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #44B699;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;

  .done-icon {
    font-size: 18px;
  }
}

.done-desc {
  margin: 8px 0 0;
  color: #222527;
  font-size: 16px;
  font-weight: 500;
  line-height: 26px;
}

/* 结果卡片：两列。行内等高（默认 stretch），保证两张卡片底边对齐 */
.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 18px;
}

/* 单个结果卡片（两列网格中的一列）：撑满行高，卡片内部自行分配空间 */
.panel-item {
  display: flex;
  min-width: 0;
}

/* 结果卡片：设计稿 390×324，圆角 20 + 蓝色投影、无描边 */
.panel-card {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  padding: 16px;
  box-sizing: border-box;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #e8e8e8;
  &:hover {
    box-shadow: 0 4px 16px rgba(53, 97, 250, 0.3);
  }
}

/* 撤离路线卡（02）：设计稿为 1px 灰描边、无投影，标题为深色。
   描边用 inset 阴影实现，避免 border 挤占内容宽度（设计稿内容区为 358px） */
.panel-card.is-route {
  box-shadow: inset 0 0 0 1px #e8e8e8;

  .panel-title {
    color: #222529;
  }
}

.panel-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

/* 序号：40×40 主色浅底块，数字居中 */
.panel-index {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: rgba(0, 123, 255, 0.2);
  color: #007bff;
  font-size: 22px;
  font-weight: 700;
  line-height: 24px;
}

.panel-caption {
  flex: 1 1 auto;
  min-width: 0;
}

.panel-title {
  margin: 0;
  color: #007bff;
  font-size: 16px;
  font-weight: 700;
  line-height: 20px;
}

.panel-desc {
  margin: 4px 0 0;
  color: #9096a2;
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
}

/* 更新项：浅灰底块（高 54、圆角 12，块间距 7）。底部留白交给外层等高与入口吸底 */
.change-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 20px 0 24px;
  padding: 0;
  list-style: none;
}

.change-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 54px;
  padding: 0 20px 0 24px;
  box-sizing: border-box;
  border-radius: 12px;
  background: #f5f9fc;
}

.row-icon {
  flex-shrink: 0;
  display: block;
  width: 16px;
  height: 16px;
}

.change-label {
  flex-shrink: 0;
  /* 标签后自动留白，把数值组推到行的右侧 */
  margin-right: auto;
  color: #617185;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
}

.change-value {
  flex-shrink: 0;
  display: flex;
  align-items: center;

  /* 原值 → 新值：18×8 主色箭头，两侧间距 12 / 15（按设计稿，不对称） */
  .arrow {
    flex-shrink: 0;
    display: block;
    width: 18px;
    height: 8px;
    margin: 0 15px 0 12px;
  }

  strong {
    color: #222527;
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    white-space: nowrap;
    font-family: 'Alimama FangYuanTi VF', sans-serif;

    /* 新值：一律主色蓝（设计稿「12户」「36人」均为主色） */
    &.is-to {
      color: #007bff;
    }

    /* 隐患变化：仅字号降为 14，颜色同为主色 */
    &.is-highlight {
      font-size: 14px;
    }
  }
}

/* 撤离路线：静态地图预览，与详情弹窗地图实例相互独立。 */
.route-block {
  margin: 20px 0 24px;
}

/* 预览取景框：按卡片宽度自适应取 2:1。
   取 2:1 而非 16:9，使地图高度约 358×0.5≈176，撤离路线卡总高与预案更新稿卡（324）一致 */
.route-map-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 1;
  overflow: hidden;
  border-radius: 16px;
  background: #617185;
}

/* 地图本体：铺满取景框，尺寸交给容器（组件内部按 100% 撑开） */
.route-map-preview {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 卡片底部入口：吸底对齐（内容不等高时各卡入口仍落在同一条水平线上），
   与上方内容的间距 ≥24px（由 change-list / route-block 的下边距提供），链接距分隔线 12px */
.panel-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #dfe4f0;
}

.panel-link {
  display: inline-flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #007bff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }

  .link-arrow {
    flex-shrink: 0;
    display: block;
    width: 16px;
    height: 16px;
  }
}

/* 卡片下方说明行：与卡片共用两列网格，保证说明与对应卡片同列对齐 */
.panel-footnotes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 20px;
  margin-top: 18px;
}

.panel-footnote {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;

  /* 待核实：橙色提示 */
  &.is-notice {
    color: #ff922c;
    font-weight: 600;
  }

  /* 各卡片补充说明：主色 */
  &.is-note {
    color: #007bff;
  }
}

/* 待核实图标：设计稿导出件（16×16 矢量） */
.panel-footnote-icon {
  flex-shrink: 0;
  display: block;
  width: 16px;
  height: 16px;
}

.files-title {
  margin: 24px 0 0;
  color: #222527;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.file-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 12px;
}

/* 文件卡：设计稿 390×68，白底 + 浅描边，右下角蓝色模糊光晕 */
.file-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 68px;
  padding: 0 24px 0 20px;
  box-sizing: border-box;
  overflow: hidden;
  border: 1px solid #e4eaef;
  border-radius: 10px;
  background: #ffffff;
}

/* 背景光晕：设计稿 Ellipse（127×37、rgba(0,98,255,.2)、blur 50px）贴右下角 */
.file-glow {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 127px;
  height: 37px;
  border-radius: 50%;
  background: rgba(0, 98, 255, 0.2);
  filter: blur(50px);
  pointer-events: none;
}

.file-icon {
  flex-shrink: 0;
  display: block;
  width: 36px;
  height: 36px;
}

/* 图片附件缩略图：与图标同尺寸，圆角裁切，鼠标提示可预览 */
.file-image {
  border-radius: 8px;
  cursor: zoom-in;
}

.file-text {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.file-name {
  overflow: hidden;
  color: #383c41;
  font-size: 14px;
  font-weight: 400;
  line-height: 14px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-meta {
  color: #9096a2;
  font-size: 12px;
  font-weight: 400;
  line-height: 12px;
}

/* 下载按钮：直接用设计稿导出件（28×28，含圆角描边与箭头） */
.download-button {
  position: relative;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: url('@/assets/imgs/emergency/icon-download.svg') no-repeat center / contain;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.75;
  }
}
</style>
