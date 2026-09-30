<template>
  <div class="chat-layout" :class="{ 'is-human-mode': isHumanMode }">
    <aside v-if="isHumanMode" class="human-avatar-panel" aria-label="数字人播报状态">
      <div class="human-avatar-video">
        <WakeAvatarVideo :is-speaking="isTtsPlaying" :show-switch="false" mirror />
      </div>
      <div class="human-tts-status">
        <TtsPlaybackStatus :session-id="ttsPlaybackState.sessionId" />
      </div>
    </aside>

    <div class="chat-index" :class="{ 'is-space-analysis': currentChatType === CHAT_TYPE.SPACE_ANALYSIS }">
      <!-- 聊天详情 -->
      <div class="chat-content-wrap">
        <div id="chat-container" ref="chatContainer" class="chat-content">
          <div ref="chatContentRef" class="chat-content-inner">
            <div
              v-for="(msg, key) in msgList"
              v-if="msgList.length"
              :key="msg.id"
              class="each-msg"
              :class="{ 'is-ai-message': msg.type !== 1 }"
            >
              <template v-if="msg.type === 1">
                <UserMsg :msg="msg"></UserMsg>
              </template>
              <template v-else>
                <img
                  v-if="!isHumanMode && hasAiOutputContent(msg)"
                  class="engine-avatar"
                  :src="engineAvatar"
                  alt="XX智能助手"
                />
                <AIMsg
                  :msg="{ ...msg, conversationId }"
                  :isAnswering="isAnswering"
                  :mode="mode"
                  :showRefs="key + 1 == msgList.length"
                  @send="sendMsg"
                  @scroll-to-bottom="scrollChatListToBottom"
                ></AIMsg>
              </template>
            </div>
            <Transition name="inline-location-panel" @after-leave="handleInlineLocationAfterLeave">
              <div v-if="showInlineLocationPicker" class="inline-location-picker">
                <SpaceAnalysisLocationPanel
                  ref="inlineLocationPanelRef"
                  :submitting="isInlineLocationSubmitting"
                  @cancel="closeInlineLocationPicker"
                  @confirm="confirmInlineLocation"
                />
              </div>
            </Transition>
            <QuesAdvisor v-if="showQuestionAdvisor" @send="sendMsg"></QuesAdvisor>

            <div id="msg-bottom"></div>
          </div>
        </div>

        <el-icon v-if="displayToBottom" class="to-bottom" @click="scrollChatListToBottom(true)">
          <DArrowRight />
        </el-icon>
      </div>

      <!-- 输入内容 -->
      <UserInput
        ref="userInputRef"
        :is-answering="isAnswering"
        :human-mode="isHumanMode"
        show-human-mode
        :enable-image-upload="currentChatType === CHAT_TYPE.IMAGE_RECOGNITION"
        :enable-meeting-minutes="currentChatType === CHAT_TYPE.MEETING_MINUTES"
        :enable-map-picker="currentChatType === CHAT_TYPE.SPACE_ANALYSIS"
        :space-selection="spaceAnalysisSelection"
        noAgentSelctor
        @send="sendMsg"
        @stop="stopOutput"
        @open-location-selector="openInlineLocationPicker"
        @remove-space-selection="removeSpaceAnalysisSelection"
        @update:human-mode="changeHumanMode"
      />
      <div class="output-tips">回答内容由AI生成，仅供参考，请仔细甄别</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import UserMsg from './UserMsg.vue'
import UserInput from './UserInput.vue'
import AIMsg from './AIMsg.vue'
// import testMd from './mock/test.md?raw'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ElMessage } from 'element-plus'
// import texmath from 'markdown-it-texmath'
// import katex from 'katex'

// // 必须引入 KaTeX 样式
// import 'katex/dist/katex.min.css'
// // 建议引入 texmath 样式以获得更好的公式间距
// import 'markdown-it-texmath/css/texmath.css'
// import markdownItKatex from '@iktakahiro/markdown-it-katex'
import { getChatMsg, getReportParseResult } from '@/api/chat.js'
import dayjs from 'dayjs'
import { generateRandomString, getToken, replaceArticleTag } from '@/utils/index.js'
import { CHAT_TYPE } from '@/utils/enum'
import QuesAdvisor from './QuesAdvisor.vue'
import { hasAuth } from '@/utils/directive.js'
// import { useSessionStorage } from '@vueuse/core'

// const msgDbStorage = useSessionStorage('msg-db-info', {})

import { useRoute } from 'vue-router'
import useChatBox from '@/components/ChatBox/useChatBox.js'
import {
  decodeJsonStringContent,
  parseAnswerPayload,
  parseBusinessAnswer,
  parseHistoryReferenceRecords,
  parseLeadingJsonObject,
  parseThinkStreamChunk,
} from '@/components/ChatBox/chatBoxUtil.js'
import { ttsPlaybackState, ttsStreamPlayer } from '@/utils/audio/TtsStreamPlayer.js'
import { useScroll } from '@/hooks/useScroll.js'
import { useUserStore } from '@/store/user.js'
import WakeAvatarVideo from '@/components/WakeAvatarVideo/index.vue'
import TtsPlaybackStatus from '@/components/TtsPlaybackStatus/index.vue'
import SpaceAnalysisLocationPanel from '@/components/SpaceAnalysisLocationPanel/index.vue'
import {
  buildSpaceAnalysisRequest,
  createSpaceAnalysisAttachment,
  getSpaceAnalysisCoordinates,
} from '@/utils/spaceAnalysis.js'
import engineAvatar from '@/assets/imgs/chatBox/risk-evaluation-agent-avatar.png'
const route = useRoute()
const userStore = useUserStore()
const { isHumanMode } = storeToRefs(userStore)
const isTtsPlaying = computed(() => ttsPlaybackState.active)
const currentChatType = computed(() => {
  const supportedTypes = [CHAT_TYPE.IMAGE_RECOGNITION, CHAT_TYPE.MEETING_MINUTES, CHAT_TYPE.SPACE_ANALYSIS]
  return supportedTypes.includes(route.query.type) ? route.query.type : CHAT_TYPE.NORMAL
})
// 后端会议纪要聊天应用使用短横线形式的 appType。
const requestAppType = computed(() =>
  currentChatType.value === CHAT_TYPE.MEETING_MINUTES ? 'meeting-minutes' : currentChatType.value,
)
const showQuestionAdvisor = computed(
  () =>
    !msgList.value.length &&
    route.query.type &&
    // 空间分析有独立入口流程，不再展示通用的会话欢迎卡片。
    ![CHAT_TYPE.IMAGE_RECOGNITION, CHAT_TYPE.MEETING_MINUTES, CHAT_TYPE.SPACE_ANALYSIS].includes(
      currentChatType.value,
    ),
)

function changeHumanMode(enabled) {
  isHumanMode.value = enabled
}

const props = defineProps({
  // 助手模式，chat为一般会话模式
  mode: {
    type: String,
    default: 'chat',
  },
  noAgentSelctor: {
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits(['get-msg-detail', 'msg-change'])

const chatContainer = ref(null)
const chatContentRef = ref(null)
const { isScrollBlocked, scrollBottom: scrollChatListToBottom } = useScroll(chatContainer, { threshold: 30 })
const { operateBusiness } = useChatBox([], () => {}, scrollChatListToBottom)

// 内容区高度变化时触发的滚动帧，避免图片批量加载时重复滚动。
let contentResizeFrame = null
// 监听图片、流程图等异步渲染造成的内容高度变化。
let contentResizeObserver = null

/**
 * 判断 AI 消息是否已经产生可展示内容。
 *
 * @param {object} message AI 消息。
 * @returns {boolean} 已输出正文、思考内容或业务结果时返回 true。
 */
function hasAiOutputContent(message = {}) {
  const textContents = [message.content, message.originalContent, message.think, message.originalThink]
  const hasTextContent = textContents.some((content) => typeof content === 'string' && content.trim())

  return (
    hasTextContent ||
    Boolean(message.imageRecognitionResult || message.hasChartTask || message.pureJson || message.isOperateJson)
  )
}

/**
 * 在当前处于自动跟随状态时，将异步撑高后的内容滚动到底部。
 * @returns {void}
 */
function scheduleContentResizeScroll() {
  if (isScrollBlocked.value || contentResizeFrame) return

  contentResizeFrame = requestAnimationFrame(() => {
    contentResizeFrame = null
    if (!isScrollBlocked.value) {
      void scrollChatListToBottom()
    }
  })
}

// let test = `\n\n根据知识库检索可知，滑坡稳定性系数（K）的计算核心是**抗滑力与下滑力的比值**，其结果用于判定滑坡稳定性状态（K>1稳定，K=1极限平衡，K<1不稳定）。具体计算方法和公式如下：\n\n---\n\n### **1. 基础计算原理**\n- **核心公式**：  \n  $$\n  K = \\frac{\\text{抗滑力}}{\\text{下滑力}}\n  $$\n- **关键参数**：  \n  滑体重量（W）、滑面倾角（α）、内摩擦角（φ）、凝聚力（c）、地下水深度（h）等。\n\n---\n\n### **2. 常用计算方法**\n#### **(1) 水平投影法**  \n- **适用场景**：滑面为圆弧形或折线形时，将滑体分块计算。  \n- **步骤**：  \n  1. 将滑体沿滑面分割为 **n** 个条块；  \n  2. 计算每块的抗滑力（$B\\tan\\phi + cL$）和下滑力（$W\\sin\\alpha$）；  \n  3. 对所有条块的抗滑力和下滑力进行水平投影求和；  \n  4. 稳定系数公式：  \n     $$\n     K = \\frac{\\sum (B\\tan\\phi + cL)}{\\sum W\\sin\\alpha}\n     $$\n\n#### **(2) 传递系数法**  \n- **适用场景**：滑面为直线或折线时，考虑条块间力的传递。  \n- **公式**：  \n  $$\n  K = \\frac{\\sum_{i=1}^{n-1} \\left[(B_i\\tan\\phi_i + c_iL_i\\right) \\prod_{j=i}^{n-1} \\psi_j + R_n}  \n       {\\sum_{i=1}^{n-1} \\left[W_i(\\sin\\alpha_i + A\\cos\\alpha_i) + T_{D_i}\\right] \\prod_{j=i}^{n-1} \\psi_j + T_n}\n  $$  \n  （其中 $\\psi_j$ 为传递系数，$A$ 为地震力系数，$T_{D_i}$ 为地下水力）\n\n#### **(3) 圆弧滑面法（均质土滑坡）**  \n- **公式**：  \n  $$\n  F_s = \\frac{W_2d_2 + c \\cdot l \\cdot R}{W_1d_1}  \n  $$  \n  （$W_1, W_2$ 为下滑与抗滑块体重量，$d_1, d_2$ 为重心至垂线距离，$R$ 为圆弧半径）\n\n---\n\n### **3. 影响因素与敏感性分析**\n- **关键影响因素**：  \n  - 土体物理力学性质（c、φ）  \n  - 地下水位（h）  \n  - 滑面形态（圆弧、直线、折线）  \n  - 外部荷载（地震、降雨）  \n- **敏感性系数公式**：  \n  $$\n  S = \\left(\\frac{\\Delta F_s / F_{os}}{|\\Delta X| / (X_{\\max} - X_{\\min})}\\right) \\times 100\\%\n  $$  \n  （用于量化参数变化对稳定性的影响程度）\n\n---\n\n### **4. 方法对比与选择**\n| **方法**         | **适用滑面类型** | **是否考虑条块间力传递** | **复杂度** |\n|------------------|------------------|--------------------------|------------|\n| 水平投影法       | 圆弧形、折线形   | 否                       | 中等       |\n| 传递系数法       | 直线形、折线形   | 是                       | 高         |\n| 圆弧滑面法       | 圆弧形           | 否                       | 低         |\n\n---\n\n### **5. 注意事项**\n- **参数选取**：需通过现场勘察或试验获取滑带土的 **c、φ** 等力学参数；  \n- **动态修正**：需结合地下水位、降雨等动态因素调整计算模型；  \n- **方法适配**：复杂滑坡建议采用 **有限元法** 等数值模拟方法（考虑应力-应变关系）。`

// console.log('md -----', md.render(test))
// onMounted(() => {
//   document.getElementById('chat-container').innerHTML = md.render(test)
// })

let chatController = new AbortController()
let ttsSessionId = 0

const msgList = ref([
  // {
  //   type: 1,
  //   content: '你好',
  //   // reqFiles: [
  //   //   {
  //   //     type: 'image',
  //   //     transferMethod: 'local_file',
  //   //     uploadFileId: '2054875818900795393',
  //   //   },
  //   // ],
  // },
  //   {
  //     type: 2,
  //     id: 'f3288291-5335-4493-bd02-1a366b721f62',
  //     content: md.render(fixKatexSpaces('<think>\n好的，用户问的是“风险调查技术要求”，我需要根据提供的知识库内容来回答。首先，我得仔细看看知识库里的资料，特别是那个《地质灾害风险调查评价技术要求（1:50000）》的文件。里面提到了范围、规范性引用文件、术语定义、总则、设计书编写、调查内容、基本调查方法、地质灾害风险评价、成果编制与验收等部分。\n\n用户可能想知道进行地质灾害风险调查时需要遵循哪些技术要求，包括调查的内容、方法、评价方法以及成果的编制等。根据知识库中的信息，技术要求主要分为几个部分：范围适用于崩塌、滑坡、泥石流等地质灾害，规范性文件引用了相关标准，总则里提到了目的任务、部署原则和总体要求。调查内容包括孕灾地质条件、地质灾害调查、隐患调查和承灾体调查。基本方法有资料收集、遥感调查、地面调查、物探、钻探等。风险评价部分涉及易发性、危险性、易损性和风险性评价，以及管控建议。成果需要编制图件和报告，并建立数据库。\n\n需要注意用户可能关心的是具体的技术步骤和要求，比如调查区的划分（一般和重点调查区）、比例尺要求（一般1:50000，重点1:10000）、使用的调查方法等。另外，风险评价的模型和公式，比如人口风险和经济损失风险的计算公式，也需要提及。但用户的问题是关于技术要求，所以重点应放在流程、方法、标准上，而不是具体的计算公式，除非用户特别要求。\n\n需要确保回答覆盖所有关键点：调查内容、方法、评价方法、成果要求，以及适用范围。同时，要引用知识库中的具体条款，比如设计书的编写、图件的比例尺、风险区划图的编制等。还要注意术语的正确使用，比如“孕灾地质条件”、“承灾体调查”等，确保符合专业术语的定义。\n\n可能还需要提到风险评价的模型，如相乘模型R=H×V，以及不同风险等级的划分标准。此外，成果验收和数据库建设也是技术要求的一部分，需要包含在内。要避免添加知识库以外的信息，确保所有内容都基于提供的资料，不进行推测或杜撰。\n</think>\n\n地质灾害风险调查技术要求依据《地质灾害风险调查评价技术要求（1:50000）》制定，核心内容涵盖调查范围、方法、成果及风险评价体系，具体如下：\n\n---\n\n### **1. 调查范围与部署原则**  \n- **适用对象**：以崩塌、滑坡、泥石流为主，地面塌陷等地质灾害可参照执行。  \n- **区域划分**：按县级行政单元部署，优先选择地质灾害密集区、地质环境复杂区及人口聚集区。  \n- **比例尺要求**：  \n  - 一般调查区比例尺不小于1:50 000；  \n  - 重点调查区（如城镇、重大工程区）比例尺不小于1:10 000。  \n\n---\n\n### **2. 调查内容与方法**  \n#### **（1）基础调查内容**  \n- **孕灾地质条件**：分析岩土体类型、构造特征、水文地质条件及人类活动影响。  \n- **地质灾害调查**：统计灾害类型、分布、规模、活动性及成灾模式。  \n- **隐患调查**：识别潜在不稳定体，评估其变形特征与触发条件。  \n- **承灾体调查**：统计人口密度、建筑物、基础设施等暴露对象的易损性。  \n\n#### **（2）技术方法**  \n- **资料收集与遥感解译**：整合历史调查数据，利用遥感影像识别灾害分布及变化。  \n- **地面调查与山地工程**：开展实地踏勘、钻探、物探（如电阻率法、地质雷达）验证隐患点。  \n- **测试与试验**：取样分析岩土体物理力学参数（如抗剪强度、渗透系数）。  \n\n---\n\n### **3. 风险评价体系**  \n#### **（1）评价层级与模型**  \n- **三级评价框架**：  \n  1. **易发性评价**：基于地质条件与历史灾害数据，划分易发区等级（高、中、低）。  \n  2. **危险性评价**：结合灾害活动性与承灾体暴露度，量化破坏概率（Pli）。  \n  3. **风险性评价**：采用**相乘模型** $ R = H \\times V $，其中：  \n     - $ H $：危险性指数（低-4级赋值1-4）；  \n     - $ V $：综合易损性指数（人口、经济易损性加权）。  \n\n#### **（2）风险预测公式**  \n- **人口风险**：  \n  $$\n  R_{pi} = P_{li} \\times V_{pi} \\times D_{pi}\n  $$  \n  - $ D_{pi} $：人口密度（人/km²）；  \n  - $ V_{pi} $：人口易损性（0-1）。  \n- **经济损失风险**：  \n  $$\n  R_{ei} = P_{li} \\times V_{ei} \\times A_{i}\n  $$  \n  - $ A_{i} $：资产密度（万元/km²）；  \n  - $ V_{ei} $：经济易损性（0-1）。  \n\n---\n\n### **4. 成果要求与验收**  \n#### **（1）图件编制**  \n- **核心图件**：  \n  - 地质灾害分布图、孕灾地质条件图、易发性分区图、危险性与风险评价图。  \n  - 坐标系采用2000国家大地坐标系，图例、比例尺、指北针等要素齐全。  \n- **重点图件**：风险区划图需标注极高、高、中、低风险等级边界。  \n\n#### **（2）报告与数据库**  \n- **报告内容**：  \n  - 调查区概况、地质环境特征、灾害发育规律、风险评价成果及管控建议。  \n  - 单体灾害需提出工程治理、避让距离、监测预警等对策。  \n- **数据库建设**：建立空间数据库，整合灾害点属性、监测数据及风险评价结果。  \n\n#### **（3）成果验收**  \n- **验收标准**：图件与报告需通过专家评审，数据完整性、精度符合设计书要求。  \n- **归档要求**：原始资料、成果文件及数据库按规范归档，确保可追溯性。  \n\n---\n\n### **5. 技术局限性与趋势**  \n- **当前不足**：孕灾机理认知不足、极端条件下的风险评价模型精度有限。  \n- **发展方向**：融合InSAR形变监测、机器学习预测模型，提升隐患识别与风险量化能力。  \n\n以上要求为1:50 000比例尺风险调查提供技术框架，实际应用需结合区域地质条件动态调整。',
  // )),
  //   originalContent: '',
  // think: '',
  //     originalThink: '',
  //     refQ: [],
  //     dbs: [],
  //     loading: false,
  //     done: true,
  //     thinking: false,
  //     hasChartTask: true,
  //     chartDataStr: '',
  //     pureJson: null, // 纯JSON数据，无其他附带文案的情况
  //     // pureJson: {
  //     //   type: 'report',
  //     //   data: `{"token":"[redacted]","type":"report_analysis","key_tips":"组织会商研判 加密隐患点巡查排查 加强重点区域防范 开展风险区监测预警 管控人类工程活动 发布预警信息 组织群众撤离 加强临灾避险宣传 强化值班值守","data":[ { "time": "2026-05-16", "publish_date": "2026-05-16 20:00", "valid_date": "2026-05-17 20:00", "data": { "blue_level": { "streets": ["其他乡镇"] }, "yellow_level": { "streets": [ "六角亭街道", "小渡船街道", "金子坝街道", "舞阳坝街道", "七里坪街道", "龙凤镇" ] }, "orange_level": { "streets": [ "沐抚办事处", "屯堡乡", "白果乡", "盛家坝镇" ] }, "red_level": { "streets": [] } } }, { "time": "2026-05-17", "publish_date": "2026-05-17 20:00", "valid_date": "2026-05-18 20:00", "data": { "blue_level": { "streets": [] }, "yellow_level": { "streets": [ "六角亭街道", "小渡船街道", "金子坝街道", "舞阳坝街道", "七里坪街道", "龙凤镇", "沙地乡", "新塘乡", "红土乡" ] }, "orange_level": { "streets": [ "沐抚办事处", "太阳河乡", "白杨坪镇", "崔家坝镇", "屯堡乡", "白果乡", "芭蕉侗族乡", "三岔镇" ] }, "red_level": { "streets": [] } } }, { "time": "2026-05-18", "publish_date": "2026-05-18 20:00", "valid_date": "2026-05-19 20:00", "data": { "blue_level": { "streets": [] }, "yellow_level": { "streets": [] }, "orange_level": { "streets": [] }, "red_level": { "streets": [] } } } ]}`, // 纯JSON数据，无其他附带文案的情况
  //     // },
  //     noThinkingProgress: true,
  //     date: '2026-04-07 09:18:48',
  //     status: 0,
  //     references: '',
  //   },
])
watch(
  () => msgList.value.length,
  (val) => {
    emits('msg-change', val)
  },
  {
    immediate: true,
  },
)

const isAnswering = ref(false) // 回答中
const userInputRef = ref()
const conversationId = ref()
const showInlineLocationPicker = ref(false)
const isInlineLocationSubmitting = ref(false)
const inlineLocationPanelRef = ref(null)
const inlineLocationQuestion = ref('')
const spaceAnalysisSelection = ref(null)
let inlineLocationLeaveResolver = null

function removeSpaceAnalysisSelection() {
  spaceAnalysisSelection.value = null
}

async function openInlineLocationPicker(question = '') {
  if (isAnswering.value) {
    ElMessage.warning('请等待智能体回复完毕')
    return
  }

  inlineLocationQuestion.value = question
  showInlineLocationPicker.value = true
  await nextTick()
  scrollChatListToBottom(true)
}

function closeInlineLocationPicker() {
  showInlineLocationPicker.value = false
  inlineLocationQuestion.value = ''
}

function handleInlineLocationAfterLeave() {
  inlineLocationLeaveResolver?.()
  inlineLocationLeaveResolver = null
}

function hideInlineLocationPicker() {
  inlineLocationQuestion.value = ''
  if (!showInlineLocationPicker.value) return Promise.resolve()

  return new Promise((resolve) => {
    inlineLocationLeaveResolver = resolve
    showInlineLocationPicker.value = false
  })
}

async function confirmInlineLocation(selection) {
  if (isInlineLocationSubmitting.value) return
  isInlineLocationSubmitting.value = true

  let attachment
  let request
  try {
    attachment = await createSpaceAnalysisAttachment(inlineLocationPanelRef.value, selection)
    request = buildSpaceAnalysisRequest(selection, inlineLocationQuestion.value)
  } catch (error) {
    console.error('空间分析地图截图上传失败', error)
    ElMessage.error('地图截图生成或上传失败，请重试')
    return
  } finally {
    isInlineLocationSubmitting.value = false
  }

  // 先移除位置面板，再进入统一的聊天消息与流式回答流程。
  spaceAnalysisSelection.value = selection
  await hideInlineLocationPicker()
  await sendMsg(request.prompt, [attachment], 'img', request.coordinates, selection)
}

// AI问数-画图，目前仅放在最后
// let chartType
// let chartData
// let chartDataStr = ''
// function resetChartInfo() {
//   chartType = ''
//   chartData = null
//   chartDataStr = ''
// }

function getMalformedJsonField(content = '', fieldName = '') {
  const fieldKey = `"${fieldName}":"`
  const startIndex = content.indexOf(fieldKey)
  if (startIndex === -1) return ''

  const valueStart = startIndex + fieldKey.length
  const knownEndIndex = content.indexOf('","from_variable_selector"', valueStart)
  const fallbackEndIndex = content.indexOf('","', valueStart)
  const valueEnd = knownEndIndex > -1 ? knownEndIndex : fallbackEndIndex

  if (valueEnd === -1) return ''
  return decodeJsonStringContent(content.slice(valueStart, valueEnd))
}

function parseSseMessage(rawData = '') {
  try {
    const res = JSON.parse(rawData) || {}
    return {
      res,
      data: res.answer || '',
      date: res.created_at ? dayjs(new Date(parseFloat(res.created_at + '000'))).format('YYYY-MM-DD HH:mm:ss') : '',
      type: res.eventType,
    }
  } catch (error) {
    return {
      res: {},
      data: getMalformedJsonField(rawData, 'answer') || '',
      date: '',
      type: getMalformedJsonField(rawData, 'eventType'),
    }
  }
}

function syncAnswerContent(curMsg, data = '') {
  const previousContent = typeof curMsg.rawContent === 'string' ? curMsg.rawContent : curMsg.originalContent || ''
  curMsg.rawContent = previousContent + data
  // 基于累计文本解析，避免协议标记跨 SSE 分片时被漏判。
  const parsed = parseAnswerPayload(curMsg.rawContent)
  curMsg.refQ = parsed.refQ
  curMsg.originalContent = parsed.content
  curMsg.content = parsed.content
  curMsg.hasChartTask = parsed.hasChartTask
  curMsg.chartDataStr = parsed.chartDataStr
  curMsg.isOperateJson = parsed.isOperateJson
  curMsg.pureJson = parsed.pureJson
}

function syncImageRecognitionResultChunk(curMsg, response = {}, data = '', eventType = '') {
  if (currentChatType.value !== CHAT_TYPE.IMAGE_RECOGNITION || eventType !== 'MESSAGE') return false
  const selector = response.from_variable_selector
  if (!Array.isArray(selector) || selector.at(-1) !== 'result') return false

  const parsed = parseLeadingJsonObject(data)
  if (parsed.data) curMsg.imageRecognitionResult = parsed.data
  return true
}

function syncImageRecognitionFinalAnswer(curMsg, response = {}) {
  if (currentChatType.value !== CHAT_TYPE.IMAGE_RECOGNITION) return
  const finalAnswer = response.data?.outputs?.answer || ''
  if (!finalAnswer) return

  const parsed = parseLeadingJsonObject(finalAnswer)
  if (parsed.data) curMsg.imageRecognitionResult = parsed.data
  if (!parsed.content) return

  curMsg.rawContent = ''
  syncAnswerContent(curMsg, parsed.content)
}

function getUploadFileId(file) {
  let response = file?.response?.data || {}

  return response.ossId
}

/**
 * 将历史记录中的 inputs 统一为对象，兼容接口将其序列化为 JSON 字符串的情况。
 *
 * @param {object|string} inputs 历史消息输入参数。
 * @returns {object} 可读取的输入参数对象。
 */
function parseHistoryInputs(inputs) {
  if (inputs && typeof inputs === 'object') return inputs
  if (typeof inputs !== 'string') return {}

  try {
    const parsed = JSON.parse(inputs)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    return {}
  }
}

/**
 * 解析历史输入中的文件列表，兼容文件数组被再次 JSON 序列化的情况。
 *
 * @param {Array|string} files 历史文件数据。
 * @returns {Array} 历史文件列表。
 */
function parseHistoryFiles(files) {
  if (Array.isArray(files)) return files
  if (typeof files !== 'string') return []

  try {
    const parsed = JSON.parse(files)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

/**
 * 为历史消息中的智能体提及补充与实时消息一致的加粗样式。
 *
 * @param {string} content 用户提问内容。
 * @returns {string} 可直接回显的消息内容。
 */
function formatAgentMention(content = '') {
  return String(content).replace(/^(@[^@：:\s<>]+?(?:智能体|助手))(?=[：:\s]|$)/, '<strong>$1</strong>')
}

async function sendMsg(msg, files = [], uploadType = '', coordinates = null, selection = null) {
  closeInlineLocationPicker()
  if (selection) spaceAnalysisSelection.value = selection
  const requestCoordinates =
    coordinates ||
    (currentChatType.value === CHAT_TYPE.SPACE_ANALYSIS
      ? getSpaceAnalysisCoordinates(spaceAnalysisSelection.value)
      : null)
  const uploadFiles = [...(files || [])]
  let agent, question, quoteCache
  // 提取开头的智能体提及，名称支持以“智能体”或“助手”结尾。
  const agentMentionMatch = /^(@[^@：:\s<>]+?(?:智能体|助手))(?=[：:\s]|$)([\s\S]*)$/.exec(msg)
  if (agentMentionMatch) [, agent, question] = agentMentionMatch
  else question = msg

  // 识图工作流需要接收完整的 @智能体 提及，不能只提交提及后的描述内容。
  const requestQuestion = currentChatType.value === CHAT_TYPE.IMAGE_RECOGNITION ? msg.trim() : question.trim()

  if (currentChatType.value === CHAT_TYPE.IMAGE_RECOGNITION) {
    if (!requestQuestion) {
      ElMessage.warning('请输入识图内容')
      return
    }
    if (!uploadFiles.some((file) => file.status === 'success' && getUploadFileId(file))) {
      ElMessage.warning('请先上传图片')
      return
    }
  }

  // 用户发起新问题时立即解锁音频，并终止上一轮播报。
  ttsSessionId = ttsStreamPlayer.startSession()

  let reqFiles = uploadFiles
    .map((file) => {
      const uploadFileId = getUploadFileId(file)

      if (!uploadFileId) return null
      return {
        type: file.raw?.type?.startsWith('image/') || uploadType === 'img' ? 'image' : 'document',
        transferMethod: 'local_file',
        uploadFileId,
        name: file.name || '',
        size: file.size || 0,
      }
    })
    .filter(Boolean)

  // 当前消息优先使用独立的本地预览，避免发送后清空上传列表导致原 blob 地址失效。
  const displayFiles = reqFiles.map((file, index) => {
    const rawFile = uploadFiles[index]?.raw
    return rawFile && file.type === 'image'
      ? { ...file, previewUrl: URL.createObjectURL(rawFile), isLocalPreview: true }
      : file
  })
  const agentSpacing = agent && !/^[：:]/.test(question) ? '&nbsp;' : ''

  msgList.value.push({
    type: 1,
    content: (agent ? `<strong>${agent}${agentSpacing}</strong>` : '') + question,
    reqFiles: displayFiles,
  })
  scrollChatListToBottom(true)

  userInputRef.value && userInputRef.value.clearInput()
  msgList.value.push({
    type: 2,
    id: null,
    content: '',
    originalContent: '',
    rawContent: '',
    ttsSessionId,
    think: '',
    originalThink: '',
    refQ: [],
    dbs: [],
    loading: true,
    done: false,
    thinking: false,
    hasChartTask: false,
    chartDataStr: '',
    imageRecognitionResult: null,
    pureJson: null, // 纯JSON数据，无其他附带文案的情况
    pureJsonHasRegister: false, // 纯JSON数据是否已经被注册处理，主要用于一些需要特殊处理的纯JSON数据，比如报告解读类的
    noThinkingProgress: reqFiles && reqFiles.length && reqFiles[0].type === 'document', // 是否需要展示进度条式思考过程
    date: '',
    status: 0,
    references: '',
  })
  emits('get-msg-detail', msgList.value)

  // 如果之前有正在进行的请求，先打断它
  chatController.abort()
  // 对新请求重新实现控制器实例化
  chatController = new AbortController()

  isAnswering.value = true
  await fetchEventSource(`/api/dizai/ai/agent/chat`, {
    method: 'POST',
    openWhenHidden: true,
    headers: {
      Accept: 'text/event-stream',
      [import.meta.env.VITE_APP_TOKEN_KEY]: getToken(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      query: requestQuestion,
      think: currentChatType.value !== CHAT_TYPE.IMAGE_RECOGNITION && Boolean(userStore.isDeepThink),
      agentType: currentChatType.value,
      // appId: props.mode,
      conversationId: conversationId.value ? conversationId.value + '' : '',
      inputs: {
        appType: requestAppType.value,
        files: reqFiles || [],
        ...(requestCoordinates ? requestCoordinates : {}),
      },
      ...(reqFiles.length > 0 ? { files: reqFiles } : {}),
    }),
    signal: chatController.signal,
    onmessage(msg) {
      const len = msgList.value.length - 1
      let curMsg = msgList.value[len]

      curMsg.loading = false
      let { res, data, date, type } = parseSseMessage(msg.data)

      if (!conversationId.value && res.conversation_id) {
        conversationId.value = res.conversation_id
      }

      if (type === 'WORKFLOW_FINISHED') {
        // 工作流最终输出包含“识别 JSON + 展示正文”，以完整正文校正流式结果。
        syncImageRecognitionFinalAnswer(curMsg, res)
        isAnswering.value = false
        curMsg.done = true
        curMsg.date = date
        if (res.data?.status === 'failed') {
          curMsg.content = '会话出错，请重试。'
          ttsStreamPlayer.stop(ttsSessionId)
        }
        if (curMsg.isOperateJson) {
          ttsStreamPlayer.stop(ttsSessionId)
          const flowId = generateRandomString(26)
          const backObj = parseBusinessAnswer(res?.data?.outputs?.answer || '')
          // 固定格式 { id: xxx, type: '', data: {} }
          operateBusiness(backObj, flowId)
          scrollChatListToBottom(true)
        } else if (res.data?.status !== 'failed') {
          ttsStreamPlayer.finish(ttsSessionId)
        }
      } else if (type === 'refs') {
        let _refQ
        try {
          _refQ = JSON.parse(data)
        } catch (error) {
          _refQ = []
        }
        curMsg.refQ = _refQ
      } else if (type === 'NODE_FINISHED') {
        const nodeInfo = res?.data || {}
        if (nodeInfo.title === 'Knowledge Showcase') {
          let dbs = res?.data?.outputs?.result
          // try {
          //   dbs = JSON.parse(dbs)
          // } catch (error) {
          //   dbs = {}
          // }
          curMsg.dbs = dbs || []
        }

        // let dbs = res.data?.outputs?.result || []
      } else {
        // result 节点单独展示为识图结论，不混入 Markdown 正文。
        if (syncImageRecognitionResultChunk(curMsg, res, data, type)) {
          if (!curMsg.id) curMsg.id = res.message_id
          scrollChatListToBottom()
          return
        }

        if (!curMsg.id) {
          curMsg.id = res.message_id
        }

        // 拆分思考标签，避免 </think> 后面的正文被误归到思考过程
        data = parseThinkStreamChunk(curMsg, data)

        if (!data) {
          // 思考阶段没有正文，但思考区域仍在增高，需要同步跟随到底部。
          scrollChatListToBottom()
          return
        }

        syncAnswerContent(curMsg, data)
        if (!curMsg.isOperateJson) {
          ttsStreamPlayer.update(curMsg.originalContent, ttsSessionId)
        }
      }
      scrollChatListToBottom()
    },
    onerror(err) {
      ttsStreamPlayer.stop(ttsSessionId)
      isAnswering.value = false
      const len = msgList.value.length - 1
      let temp = msgList.value[len]
      temp.content = temp.originalContent = '会话出错，请重试。'
      temp.loading = false
      temp.done = true
      scrollChatListToBottom()

      throw err
    },
  })
}

async function stopOutput() {
  // 打断连接
  chatController.abort()
  ttsStreamPlayer.stop(ttsSessionId)
  isAnswering.value = false
}

const displayToBottom = computed(() => {
  return isScrollBlocked.value
})

async function initHistoryChat(id) {
  closeInlineLocationPicker()
  removeSpaceAnalysisSelection()
  ttsStreamPlayer.stop(ttsSessionId)
  /**
   * *hasAuth判定是否有权限
   *    -1、当前用户有入库操作权限，根据conversionId查询哪些消息已经入库
   *    -2、当前用户无入库操作权限，只显示消息pureJsonHasRegister为true
   * * 默认都无入库权限
   * * 万一请求入库权限报错，要不影响历史消息的展示
   * ***/

  let registeredMsgIds = []
  const hasSqlAuth = hasAuth('geo:ai:sql')

  if (hasSqlAuth) {
    try {
      const parseRes = await getReportParseResult({ sessionId: id })
      registeredMsgIds = parseRes || []
    } catch (error) {
      console.error('获取入库状态失败：', error)
    }
  }
  // return

  let res = await getChatMsg({ historyId: id, orderByColumn: 'createDate', isAsc: 'asc' })
  conversationId.value = id
  let result = []
  res.forEach((msg) => {
    let originalThink, originalContent, originJsonStr, pureJson, imageRecognitionResult
    // 判断是否包含思考过程，think标签
    if (msg.answer?.startsWith('<think>')) {
      let [think, content] = msg.answer.split('</think>')
      originalThink = think.replace('<think>', '')
      originalContent = content
    } else {
      originalThink = ''
      originalContent = msg.answer || ''
    }

    const historyInputs = parseHistoryInputs(msg.inputs)
    const historyAppType = historyInputs.appType || ''
    if (currentChatType.value === CHAT_TYPE.IMAGE_RECOGNITION || historyAppType === CHAT_TYPE.IMAGE_RECOGNITION) {
      const recognitionAnswer = parseLeadingJsonObject(originalContent)
      imageRecognitionResult = recognitionAnswer.data
      originalContent = recognitionAnswer.content
    }

    // 历史回答与实时回答使用同一协议解析规则。
    const parsedAnswer = parseAnswerPayload(originalContent)
    originalContent = parsedAnswer.content
    originJsonStr = parsedAnswer.chartDataStr
    pureJson = parsedAnswer.pureJson

    if (parsedAnswer.isOperateJson && !pureJson) {
      originalContent = '已跳转'
    }

    const dbs = parseHistoryReferenceRecords(msg.messageMetadata)

    // 处理提问带了图片/文件的情况
    const reqFiles = parseHistoryFiles(historyInputs.files)

    result.push({
      type: 1,
      content: formatAgentMention(msg.query),
      reqFiles: reqFiles,
    })

    // 提问带了图片

    if (originalContent || imageRecognitionResult) {
      let _content = pureJson ? '' : originalContent
      result.push({
        ...msg,
        admireStatus: msg.feedbacks?.[0]?.rating,
        type: 2,
        content: replaceArticleTag(_content, dbs),
        originalContent: _content, // 如果是纯报告数据，则不展示文本内容
        rawContent: originalContent,
        refQ: parsedAnswer.refQ,
        dbs,
        loading: false,
        done: true,
        thinking: false,
        think: originalThink,
        originalThink: originalThink,
        hasChartTask: !!originJsonStr,
        chartDataStr: originJsonStr || '',
        imageRecognitionResult,
        date: msg.updatedAt,
        pureJson: pureJson, // 纯JSON数据，无其他附带文案的情况
        pureJsonHasRegister: pureJson ? (hasSqlAuth ? registeredMsgIds.includes(msg.id) : true) : true,
        noThinkingProgress: reqFiles && reqFiles.length && reqFiles[0].type === 'document', // 是否需要展示进度条式思考过程
        reportHistory: true,
      })
    }
  })
  msgList.value = result

  emits('get-msg-detail', msgList.value)
  await nextTick()
  scrollChatListToBottom()
}

function createNew() {
  if (isAnswering.value) {
    ElMessage.warning('请等待智能体回复完毕')
    return
  }
  closeInlineLocationPicker()
  removeSpaceAnalysisSelection()
  msgList.value = []
}

defineExpose({
  sendMsg,
  createNew,
  initHistoryChat,
})

onBeforeUnmount(() => {
  if (contentResizeFrame) {
    cancelAnimationFrame(contentResizeFrame)
    contentResizeFrame = null
  }
  contentResizeObserver?.disconnect()
  contentResizeObserver = null
  chatController.abort()
  ttsStreamPlayer.stop(ttsSessionId)
  isAnswering.value = false
})

onMounted(() => {
  if (!chatContentRef.value) return

  contentResizeObserver = new ResizeObserver(scheduleContentResizeScroll)
  contentResizeObserver.observe(chatContentRef.value)
})
</script>

<style lang="less" scoped>
.chat-layout {
  width: 50%;
  min-width: 800px;
  height: 100%;
  margin: 0 auto;
}

.chat-index {
  // position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  // align-items: flex-end;

  :deep(.user-input) {
    // width: calc(100% - 50px);
  }
}

.chat-layout.is-human-mode {
  width: min(100%, 1300px);
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 120px;

  .chat-index {
    flex: 1;
    min-width: 0;
  }
}

.human-avatar-panel {
  width: 273px;
  height: 492px;
  flex: 0 0 273px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.human-avatar-video {
  width: 273px;
  height: 450px;
}

// 预留播报状态高度，避免状态出现时挤动数字人视频。
.human-tts-status {
  width: 100%;
  height: 32px;
  display: flex;
  justify-content: center;
}

.chat-content {
  // position: relative;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding-right: 10px;
  // padding: 0 40px;
}

.chat-content-inner {
  min-height: 100%;
}

.chat-index.is-space-analysis {
  .chat-content-wrap {
    height: auto;
    flex: 1;
    min-height: 0;
  }

  :deep(.space-analysis-prompt) {
    flex: 0 0 130px;
  }
}

.inline-location-picker {
  width: 100%;
  margin: 24px 0 32px;
}

.inline-location-panel-enter-active,
.inline-location-panel-leave-active {
  overflow: hidden;
  transition: opacity 0.24s ease, transform 0.24s ease, max-height 0.24s ease;
}

.inline-location-panel-enter-active,
.inline-location-panel-leave-from {
  max-height: 1200px;
}

.inline-location-panel-enter-from,
.inline-location-panel-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(12px);
}

.chat-content-wrap {
  position: relative;
  width: 100%;
  height: calc(100% - 240px);
  margin-bottom: 40px;
}

.to-bottom {
  position: absolute;
  right: 20px;
  bottom: 16px;
  transform: rotate(90deg);
  width: 30px;
  height: 30px;
  background-color: #fff;
  box-shadow: 0px 0px 12px rgba(0, 0, 0, 0.12);
  border-radius: 50%;
  cursor: pointer;
  z-index: 2;

  &:hover {
    color: var(--el-color-primary);
    box-shadow: 0px 0px 12px rgba(0, 0, 0, 0.12);
  }

  // box-shadow: 0px 0px 12px rgba(0, 0, 0, 0.12);
}

.each-msg {
  margin-bottom: 20px;
  line-height: 1.8;
  font-size: 14px;
  width: 100%;

  &:last-child {
    margin-bottom: 0;
  }
}

.each-msg.is-ai-message {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.chat-layout.is-human-mode .each-msg.is-ai-message {
  gap: 0;
}

.engine-avatar {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  object-fit: cover;
  transform: scaleX(-1);
}

.each-msg :deep(.ai-msg) {
  min-width: 0;
  flex: 1;
}

:deep(.quote-text) {
  display: inline-block;
  vertical-align: middle;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background-color: #ebecf1;
  padding: 3px 10px;
  border-radius: 12px;
  color: #848995;
  font-size: 10px;
  font-weight: bold;
  cursor: pointer;
}

:deep(.katex-html) {
  display: none;
}

.output-tips {
  width: 100%;
  font-size: 12px;
  margin-top: 10px;
  color: #a1b1c0;
  text-align: center;
}
</style>
