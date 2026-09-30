<template>
  <div class="execute-dialog">
    <header class="dialog-header">
      <div class="header-inner">
        <!-- <el-icon class="header-icon"><Finished /></el-icon> -->
        <i class="header-icon" title="返回" @click="emit('close')"></i>

        <h1>{{ data.title }}</h1>
        <!-- <button class="close-button" type="button" title="关闭" @click="emit('close')">
          <img :src="closeIcon" alt="关闭" />
        </button> -->
      </div>
    </header>

    <div class="dialog-body">
      <div class="body-inner">
        <!-- 智能体执行状态 + 参数识别（对应设计稿 800×264 卡片区域） -->
        <section class="result-card">
          <header class="result-head">
            <img class="agent-avatar" :src="agentAvatar" alt="" />
            <div class="agent-text">
              <h2 class="agent-name">{{ data.agent.name }}</h2>
              <p class="agent-desc">{{ data.agent.desc }}</p>
            </div>
          </header>

          <div class="result-body">
            <h3 class="param-title"><span class="dot"></span>{{ data.params.label }}</h3>

            <div class="param-block">
              <img class="param-icon" :src="hazardIcon" alt="" />

              <div class="param-content">
                <div class="param-head">
                  <span class="param-name">{{ data.params.name }}</span>
                  <span class="param-status">{{ data.params.status }}</span>
                </div>

                <!-- 字段两列排布，列表序号为奇数处插入中间竖分隔线 -->
                <div class="param-fields">
                  <template v-for="(field, index) in data.params.fields" :key="field.key">
                    <span v-if="index % 2 === 1" class="field-divider"></span>
                    <p class="param-field">
                      <ParamFieldIcon :type="field.key" />
                      <span class="field-text">{{ field.label }}：{{ field.value }}</span>
                    </p>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 生成中：加载效果（对应设计稿 414×144 区域，接口接入后按真实进度切换） -->
        <ExecuteLoading
          v-if="status === 'loading'"
          class="loading-card"
          :text="data.loading.text"
          :tip="loadingTip || data.loading.tip"
        />

        <!-- 生成完成 -->
        <ExecuteResultSummary v-else :data="data.done" @action="emit('action', $event)" />
      </div>
    </div>

    <footer class="dialog-footer">
      <div class="footer-inner">
        <p class="footer-tip">· {{ data.done.footerTip }}</p>
        <button class="ghost-button" type="button" @click="emit('action', data.done.actions.feedback)">
          <el-icon><ChatDotRound /></el-icon>{{ data.done.actions.feedback }}
        </button>
        <button class="primary-button" type="button" @click="emit('action', data.done.actions.confirm)">
          <el-icon><CircleCheckFilled /></el-icon>{{ data.done.actions.confirm }}
        </button>
      </div>
    </footer>

    <!-- 离屏路线大图：随弹窗挂载（移出视口，不参与布局），路线数据一到就出图并自销毁 -->
    <RouteSnapshotMap :data="routeSnapshotData" @capture="emit('capture', $event)" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ChatDotRound, CircleCheckFilled } from '@element-plus/icons-vue'
import ExecuteResultSummary from './ExecuteResultSummary.vue'
import ExecuteLoading from './ExecuteLoading.vue'
import ParamFieldIcon from './ParamFieldIcon.vue'
import RouteSnapshotMap from './RouteSnapshotMap.vue'
import hazardIcon from '@/assets/imgs/emergency/icon-hazard-red.png'
import agentAvatar from '@/assets/imgs/emergency/agent-avatar.png'

defineOptions({ name: 'ExecuteResultDialog' })

const props = defineProps({
  /** 执行结果数据：{ title, agent, params, loading, done } */
  data: {
    type: Object,
    required: true,
  },
  /** 展示状态：done 完成态 / loading 生成中 */
  status: {
    type: String,
    default: 'done',
  },
  /** 生成中的进度提示：由执行过程的流式进度驱动，缺省时取 data.loading.tip */
  loadingTip: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'action', 'capture'])

/** 撤离路线面板的路线数据：交给离屏大图出图，与卡片预览地图同源 */
const routeSnapshotData = computed(
  () => props.data.done?.panels?.find((panel) => panel.routeData)?.routeData || null,
)
</script>

<style lang="less" scoped>
.execute-dialog {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 10px 32px rgba(0, 32, 80, 0.16);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

/* 头部：图标 + 标题 + 关闭按钮 */
.dialog-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  box-sizing: border-box;
  // border-bottom: 1px solid #f0f3f8;

  .header-inner {
    display: flex;
    width: 100%;
    align-items: center;
    gap: 8px;
    margin: 0 auto;
  }

  /* 返回图标：切图做背景，需要 inline-block 才能撑出宽高 */
  .header-icon {
    flex-shrink: 0;
    display: inline-block;
    width: 16px;
    height: 16px;
    background: url('@/assets/imgs/emergency/icon-left-arrow-btn.png') no-repeat center / contain;
    cursor: pointer;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.72;
    }
  }

  h1 {
    flex: 1 1 auto;
    min-width: 0;
    margin: 0;
    overflow: hidden;
    color: #222527;
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .close-button {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;

    img {
      display: block;
      width: 14px;
      height: 14px;
      object-fit: contain;
    }
  }
}

/* 内容区：外层铺满，内容 800px 居中 */
.dialog-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 24px 28px;
  box-sizing: border-box;
  background: #ffffff;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 2px;
    background: rgba(97, 113, 133, 0.3);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  .body-inner {
    width: 100%;
    max-width: 800px;
    margin: 0 auto;
  }
}

/* 执行结果卡：对应设计稿 800×264 区块（顶部渐变头 + 参数识别） */
.result-card {
  width: 100%;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

/* 顶部渐变头：浅蓝向下渐隐到白，内容 20px 内边距 */
.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  box-sizing: border-box;
  border-radius: 20px 20px 0 0;
  background: linear-gradient(180deg, #deedff 0%, #ffffff 100%);
}

.agent-avatar {
  flex-shrink: 0;
  display: block;
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.agent-text {
  flex: 1 1 auto;
  min-width: 0;
}

.agent-name {
  margin: 0;
  overflow: hidden;
  color: #007bff;
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.agent-desc {
  margin: 4px 0 0;
  color: #80858c;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
}

/* 内容区：左右 40px；距头部 10px、距卡片底部 20px */
.result-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 40px 20px;
}

/* 参数识别标题：蓝点落在内容区左外侧 19px 处（距卡片左缘 21px） */
.param-title {
  position: relative;
  display: flex;
  align-items: center;
  margin: 0;
  color: #222527;
  font-size: 16px;
  font-weight: 500;
  line-height: 22px;

  .dot {
    position: absolute;
    top: 50%;
    left: -19px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #007bff;
    transform: translateY(-50%);
  }
}

/* 参数卡：#FBFBFF 底 + 浅描边，圆角 16，内边距 16 */
.param-block {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  box-sizing: border-box;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  background: #fbfbff;
}

.param-icon {
  flex-shrink: 0;
  display: block;
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.param-content {
  flex: 1 1 auto;
  min-width: 0;
}

.param-head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 24px;
}

.param-name {
  overflow: hidden;
  color: #222527;
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.param-status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  background: rgba(0, 123, 255, 0.26);
  color: #007bff;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
}

/* 字段区：左列 273px + 1px 分隔线 + 右列自适应，行距 8px */
.param-fields {
  display: grid;
  grid-template-columns: 273px 1px minmax(0, 1fr);
  column-gap: 24px;
  row-gap: 8px;
  margin-top: 10px;
}

.param-field {
  display: flex;
  align-items: center;
  /* 图标为设计稿 16×16 Frame（内含约 1px 留白），4px 间距使文字起点落在设计稿位置 */
  gap: 4px;
  min-width: 0;
  margin: 0;
  color: #617185;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;

  .field-text {
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

/* 两列之间的竖分隔线：高 12px，垂直居中于所在行 */
.field-divider {
  align-self: center;
  width: 1px;
  height: 12px;
  background: #e4eaef;
}

/* 生成中：加载块在弹窗内容区水平居中（设计稿 414×144，上下留白由这里给） */
.loading-card {
  margin-top: 24px;
  padding: 48px 0;
}

/* 底部操作栏：内容居中 */
.dialog-footer {
  flex-shrink: 0;
  padding: 14px 24px;
  box-sizing: border-box;
  border-top: 1px solid #f0f3f8;
  background: #ffffff;

  .footer-inner {
    display: flex;
    width: 100%;
    max-width: 800px;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;
    margin: 0 auto;
  }
}

.footer-tip {
  margin: 0 auto 0 0;
  color: #a6b0bd;
  font-size: 14px;
  line-height: 22px;
}

.ghost-button,
.primary-button {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 20px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;

  .el-icon {
    font-size: 16px;
  }
}

/* 提交反馈：白底 + 主色描边 */
.ghost-button {
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
