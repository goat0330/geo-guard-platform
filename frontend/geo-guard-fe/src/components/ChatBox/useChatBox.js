import { Components, componentsMapper, EVENT_TYPE, slopeDetailString } from '@/components/ChatBox/config.js'
import {
  getRainfallData,
  getRiskAssessmentSteps,
  getSlopeDynimicRiskDetail,
  getSlopeUnitPerson,
  getSlopeUnitRiskDetail,
  saveUserQuestion,
} from '@/api/common.js'
import { markRaw, onBeforeUnmount, onMounted, ref, shallowRef, reactive, nextTick } from 'vue'
import dayjs from 'dayjs'
import { eventBus, EventKey } from '@/utils/eventBus.js'
import {
  getLatestProcessNode,
  getSendSmsList,
  pushTaskDistList,
  taskDistList,
  taskList as taskListAll,
} from '@/api/task.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { LEVEL_COLOR, LEVEL_TEXT, UNIT_RISK_SHOW } from '@/utils/config.js'
import { useRoute, useRouter } from 'vue-router'
import {
  CHAT_TYPE,
  CUSTOM_RECORD_TYPE,
  dateStrMap,
  DISASTER_FIELD_ENUM,
  getLevelText,
  PRED_RISK_LEVEL_COLOR,
  REPORT_SOURCE,
  REPORT_STATUS,
  REPORT_STATUS_TESXT,
  RISK_LEVEL_COLOR,
  RISK_LEVEL_TEXT,
  ROLE_MAP, ROOT_SOURCE_TYPE_TEXT,
  SOURCE_TYPE,
} from '@/utils/enum.js'
import { useChatStore } from '@/store/chat.js'
import { storeToRefs } from 'pinia'
import {
  getAllDealTaskList,
  getDisasterDetail,
  getDisasterRecordDetailByHandleId,
  getRiskAreaStat,
} from '@/api/deal.js'
import { useEmergencyStore } from '@/store/emergency.js'
import { FOLLOW_TIP } from '@/views/emergencyResponse/config.js'
import { debounce, findLastIndex, omit } from 'lodash-es'
import {
  formatDateLabel,
  generateRandomString,
  getCenter,
  getPhotoUrls,
  handleStatus,
  hasAuth,
  sleep,
  toPercent,
  transformAiDataToOptions,
  waitForTasksToComplete,
  waitForTaskToComplete,
  convertToDMS,
  sourceTypeChange,
  hasRole, createLoading, isEmptyValue,
} from '@/utils/index.js'
import { useDefenseStore } from '@/store/defense.js'
import { useCesiumStore } from '@/store/cesium.js'
import { useUserStore } from '@/store/user.js'
import { getDisReportDetail } from '@/api/reportDisaster.js'
import { chat, editChatMsg } from '@/api/chat.js'
import { createMd } from '@/utils/md.js'
import {
  closeReportAction,
  handleReport,
} from '@/views/riskAnalysis/components/disasterReportManage/disasterReportManage.js'
import { decodeJsonStringContent, getHistoryExtra, updateHistoryMsg } from '@/components/ChatBox/chatBoxUtil.js'

export const getUnitData = async (id) => {
  id = (id + '').toString().padStart(4, '0')
  let resRisk = await getSlopeDynimicRiskDetail({
    date: dayjs().format('YYYY-MM-DD 00:00:00'),
    id: id,
  })
  let res = await getSlopeUnitRiskDetail(resRisk?.id)
  res = Object.assign(res, {
    ...res.slopeUnit,
    dynamicRiskLevel: resRisk.dynamicRiskLevel,
    dynamicRiskValue: resRisk.dynamicRiskValue,
    unitId: res.slopeUnitId,
    riskId: resRisk?.id,
  })
  return res
}

const getPointHtml = (html) => {
  return `<div style="display: flex; align-items: baseline; gap: 0.5rem; font-size: 0.875rem; color: #506073">
  <div style="width: 0.25rem; height: 0.25rem; background: #506073; border-radius: 50%; flex-shrink: 0"></div>
  ${html}
</div>`
}

/**
 * 等待动态风险页面完成激活，避免跨页面定位事件被 keep-alive 组件过早消费。
 * @param {number} timeout - 最长等待时间，单位毫秒。
 * @returns {Promise<boolean>} 页面是否在超时前完成激活。
 */
const waitForRiskAnalysisReady = (timeout = 10000) => {
  return new Promise((resolve) => {
    // 页面激活等待的超时定时器。
    let timeoutId = null

    /**
     * 结束等待并释放事件与定时器。
     * @param {boolean} ready - 页面是否已完成激活。
     * @returns {void}
     */
    const finish = (ready) => {
      eventBus.off(EventKey.RISK_ANALYSIS_READY, handleReady)
      window.clearTimeout(timeoutId)
      resolve(ready)
    }

    /**
     * 响应动态风险页面激活事件。
     * @returns {void}
     */
    const handleReady = () => finish(true)

    eventBus.on(EventKey.RISK_ANALYSIS_READY, handleReady)
    timeoutId = window.setTimeout(() => finish(false), timeout)
  })
}

const getPushReceiverText = (res) => {
  const pushName = Array.isArray(res?.pushName) ? res.pushName : []
  const role = Array.isArray(res?.role) ? res.role : []

  if (!pushName.length) {
    return '无'
  }

  return pushName.map((name, index) => `${role[index] || '人员'}为${name}`).join('、')
}

const getTaskSourceName = (taskList = []) => {
  const sourceNameList = [...new Set(
    taskList
      .map((item) => item?.rootSourceType)
      .filter((rootSourceType) => !isEmptyValue(rootSourceType))
      .map((rootSourceType) => ROOT_SOURCE_TYPE_TEXT[rootSourceType] || rootSourceType),
  )]

  return sourceNameList.length ? sourceNameList.join(',') : '全部'
}

const useChatBox = (updateMessageList, submit, scrollBottom, isHistory = false, useListen = false) => {
  const loading = ref(false)
  const isAnswering = ref(false)
  const router = useRouter()
  const route = useRoute()
  const chatStore = useChatStore()
  const { businessId, conversationId, queryInputs, pushTaskQuery, chatProcessIndex } = storeToRefs(chatStore)
  const messageList = isHistory ? ref([]) : storeToRefs(chatStore).messageList
  const emergencyStore = useEmergencyStore()
  const { activeFlowTip, activeFlowIndex } = storeToRefs(emergencyStore)
  const defenseStore = useDefenseStore()
  const { defObj, handleId } = storeToRefs(defenseStore)

  const cesiumStore = useCesiumStore()
  const { selectMapType } = storeToRefs(cesiumStore)

  const userStore = useUserStore()
  const { customRecordTypes } = storeToRefs(userStore)

  const md = createMd({ useBreak: true })

  const pushData = (index, item, delay) => {
    const lastMessage = messageList.value[index]
    return new Promise((resolve) => {
      setTimeout(() => {
        lastMessage.props?.data?.push(item)
        resolve()
      }, delay)
    })
  }

  // 添加提示
  const pushMessageTip = async (text, { type = 1, delay = 0, flowId = '' } = {}) => {
    // 新增提示消息的索引。
    const messageIndex = await pushMessage(
      {
        comp: Components.BUBBLE,
        props: {
          type,
          message: {
            content: text,
          },
        },
      },
      delay,
      { flowId },
    )
    return messageIndex
  }

  // 添加确认框
  const pushConfirmBox = async (
    { confirmShow, title, errorMsg = '', handleFun = () => {} } = {},
    delay = 0,
    flowId = '',
  ) => {
    if (!confirmShow) {
      confirmShow = ref(true)
    }
    return await pushMessage(
      {
        comp: Components.CONFIRM_BOX,
        show: confirmShow,
        props: {
          title: title,
          clickStatus: false,
          confirm: async () => {
            try {
              await handleFun()
            } catch (e) {
              console.error(e)
              if (errorMsg) {
                await pushMessage(
                  {
                    comp: Components.BUBBLE,
                    props: {
                      type: 1,
                      message: {
                        content: errorMsg,
                      },
                    },
                  },
                  0,
                  { flowId },
                )
              }
            } finally {
              confirmShow.value = false
              finishPushMessage()
            }
          },
          cancel: () => {
            confirmShow.value = false
            pushMessage(
              {
                comp: Components.BUBBLE,
                props: {
                  type: 1,
                  message: {
                    content: '已取消~',
                  },
                },
              },
              0,
              { flowId },
            )
          },
        },
      },
      delay,
      { flowId },
    )
  }

  const pushProcessData = async ({ index, item, show = true, key }) => {
    let lastMessage = messageList.value[index]
    if (!index) {
      if (show) {
        item.key = key
        chatProcessIndex.value = await pushMessage(
          {
            comp: Components.PROCESS,
            props: {
              data: [item],
            },
          },
          0,
        )
      }
      return
    }
    if (!show) {
      const innerIndex = lastMessage?.props?.data?.findIndex((me) => me.key === key)
      if (lastMessage.props.data[innerIndex]) {
        lastMessage.props.data[innerIndex].noShow = true
      }
    } else {
      lastMessage.props?.data?.push({
        ...item,
        key: key,
      })
    }
  }

  // 添加消息
  const pushMessage = (item, delay = 0, { flowId = '' } = {}) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        if (item instanceof Array) {
          let lastIndex = -1
          item.map((obj) => {
            const index = findLastIndex(messageList.value, (item) => {
              if (!flowId) {
                return -1
              }
              return item.flowId == flowId
            })
            const addItem = {
              comp: obj.comp,
              components: obj?.components ? shallowRef(markRaw(obj.components)) : componentsMapper[obj.comp],
              props: obj.props,
              show: obj?.show !== false,
              type: obj?.type,
              flowId,
            }
            // 获取当前流程的最后一个index
            if (index !== -1) {
              messageList.value.splice(index + 1, 0, addItem)
              lastIndex = index + 1
            } else {
              messageList.value.push(addItem)
              lastIndex = messageList.value.length - 1
            }
            updateMessageList()
          })
          resolve(lastIndex)
        } else {
          let index = findLastIndex(messageList.value, (item) => {
            if (!flowId) {
              return -1
            }
            return item.flowId == flowId
          })
          const addItem = {
            comp: item.comp,
            components: item?.components ? shallowRef(markRaw(item.components)) : componentsMapper[item.comp],
            props: item.props,
            show: item?.show !== false,
            type: item?.type,
            flowId,
          }
          if (index !== -1) {
            messageList.value.splice(index + 1, 0, addItem)
            resolve(index + 1)
          } else {
            messageList.value.push(addItem)
            resolve(messageList.value.length - 1)
          }
          updateMessageList()
        }
      }, delay)
    })
  }

  const startPushMessage = () => {
    loading.value = true
    isAnswering.value = true
  }

  const finishPushMessage = () => {
    loading.value = false
    isAnswering.value = false
    scrollBottom(true)
  }

  const chooseItem = (item) => {
    submit(item?.content)
  }

  const submitTask = (text) => {
    submit(text)
  }

  // 斜坡单元详情处理逻辑
  const getSlopeDetail = async (data, flowId) => {
    let res = null
    try {
      res = await getUnitData(data?.data?.param)
    } catch (e) {
      console.error(e)
    }
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.QUERY_SLOPE_UNIT, data: res })
    }
    await createUnitDetailData(res, delay, { history, flowId, data })
  }

  const indicatorRiskAnalysis = async (unitId) => {
    // if (!unitId) {
    //   return
    // }
    // let res = null
    // try {
    //   res = await getUnitData(unitId)
    // } catch (e) {
    //   console.error(e)
    // }
    // const flowId = generateRandomString(26)
    // await pushMessageTip(`${unitId}斜坡单元风险详情`, { type: 2 })
    // await createUnitDetailData(res, 1000, { flowId })
    submit(`${unitId}斜坡单元风险详情`)
  }

  const createUnitDetailData = async (unitDetail, delay, { history = false, flowId = '', data }) => {
    startPushMessage()
    try {
      if (!unitDetail) {
        await pushMessageTip('不存在该斜坡单元~', { delay: 0, flowId })
        return
      }
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">好的，我现在将基于「${unitDetail?.name || unitDetail?.slopeUnitName || `斜坡单元编码：${unitDetail?.id}`}」的全量数据，按流程为您推理动态风险等级的计算过程，尽量让每一步的过程都清晰呈现：</div>`,
          },
        },
        history ? 0 : 1000,
        { flowId },
      )
      const steps = await getRiskAssessmentSteps({
        assessmentId: unitDetail.riskId,
      })
      if (steps.length) {
        unitDetail = {
          ...unitDetail,
          ...steps[0],
        }
      }
      const dataObj = slopeDetailString(unitDetail)
      const slopeRawName = unitDetail?.name || unitDetail?.slopeUnitName || (unitDetail?.slopeUnitId ? `${unitDetail.slopeUnitId}号` : '') || ''
      const slopeDisplayName = slopeRawName.endsWith('斜坡') || slopeRawName.endsWith('斜坡单元') ? slopeRawName : `${slopeRawName}斜坡单元`

      let listData = []
      let outData = data
      let content = ''
      if (!history) {
        const analysisItem = reactive({
          title: `已完成调取${slopeDisplayName}的基础信息`,
          contentHtml: '',
          callback: () => {
            return new Promise((resolve) => {
              const { abort } = chat({
                params: {
                  query: '@query_slope_unit',
                  agentType: CHAT_TYPE.NOT_SHOW,
                  inputs: {
                    slope: unitDetail.slopeUnitId,
                  },
                },
                onMessage: async (data) => {
                  if (data?.eventType === 'MESSAGE') {
                    const chunk = data?.answer ?? data?.data?.answer ?? data?.text ?? ''
                    if (typeof chunk === 'string' && chunk) {
                      content += chunk
                      analysisItem.contentHtml = md.render(content)
                      scrollBottom()
                    }
                  } else if (data?.eventType === 'WORKFLOW_FINISHED') {
                    const finishedAnswer =
                      data?.data?.outputs?.answer ??
                      data?.data?.outputs?.text ??
                      data?.data?.outputs?.result ??
                      data?.outputs?.answer ??
                      data?.answer ??
                      null
                    if (typeof finishedAnswer === 'string' && finishedAnswer.trim()) {
                      content = finishedAnswer
                      analysisItem.contentHtml = md.render(content)
                    }
                    // 修改历史记录
                    await updateHistoryMsg(outData, {
                      unitInfo: content,
                    })
                    abort()
                    scrollBottom()
                    setTimeout(() => {
                      resolve()
                    }, 200)
                  }
                },
              })
              scrollBottom()
            })
          },
        })
        listData.push(analysisItem)
      } else {
        const extra = getHistoryExtra(data)
        const content = extra?.unitInfo
        const html = md.render(content)
        const obj = {
          title: `已完成调取${slopeDisplayName}的基础信息`,
          contentHtml: html,
        }
        listData.push(obj)
      }

      listData.push({
        title: `已完成调取${slopeDisplayName}“易发性”结果`,
        contentHtml: getPointHtml(`<div style="">${dataObj?.result?.[0]}</div>`),
      })

      listData.push({
        title: `已完成调取${slopeDisplayName}“危险性”结果`,
        contentHtml: getPointHtml(`<div style="white-space: pre-wrap">${dataObj?.danger?.[0]}</div>`),
      })

      listData.push({
        title: `已完成调取${slopeDisplayName}“易损性”结果`,
        contentHtml: getPointHtml(`<div style="">${dataObj?.damage?.[0]}</div>`),
      })

      listData.push({
        title: `已完成调取${slopeDisplayName}“风险性”结果`,
        contentHtml: getPointHtml(`<div style="">${dataObj?.risk?.[0]}</div>`),
      })

      let finishFLow = false
      await pushMessage(
        {
          comp: Components.PROCESS_DETAIL,
          props: {
            data: listData,
            scrollToBottom: scrollBottom,
            waitTime: delay === 0 ? [0, 0] : [800, 200],
            gap: 20,
            useLocalShow: true,
            finishFuc: () => {
              finishFLow = true
            },
          },
        },
        delay,
        { flowId },
      )

      await waitForTaskToComplete(() => finishFLow === true, 50, 100000)
      await pushMessage(
        [
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-text template-tip-gap">经过调取数据并逐步计算，<span style="font-weight: 800; color: #222529;">${slopeDisplayName}动态风险等级结果为<span style="color: ${UNIT_RISK_SHOW[unitDetail?.dynamicRiskLevel]?.color}">${UNIT_RISK_SHOW[unitDetail?.dynamicRiskLevel]?.text}</span>；</span></div>
        `,
            },
          },
        ],
        delay,
        { flowId },
      )
      scrollBottom?.()
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 动态风险评价“一张图”
  const getSlopeDynamicRisk = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      await waitForTaskToComplete(() => eventBus.hasListener(EventKey.OPEN_ONE_GRAPH_INFERENCE))
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">好的，现在我将基于<span style="color: #3561FA">划定区域的全域地质环境时空数据</span>，清晰拆解AI智能体进行地质灾害<span style="color: #3561FA">动态风险四性评价</span>的思考过程与计算流程：</div>`,
          },
        },
        0,
        { flowId },
      )
      let finishOut = false
      let message = []
      // 获取推理信息
      eventBus.emit(EventKey.OPEN_ONE_GRAPH_INFERENCE, (list) => {
        finishOut = true
        message = list
      })
      await waitForTaskToComplete(() => finishOut)
      await oneGraphInfer(message, flowId, delay)
      if (!history) {
        if (data?.data?.param) {
          let res = null
          try {
            res = await getUnitData(data?.data?.param)
          } catch (e) {
            console.error(e)
          }
          // 获取负责人信息
          data.person = await getSlopeUnitPerson(data?.data?.param)
          data = { ...res, ...data }
        }
        eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.ONE_GRAPH, data })
      }
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">好的，我现在将基于系统实时接入的全量数据，为您生成<span style="color: #3561FA">「斜坡单元动态风险评价图文」~</span></div>`,
          },
        },
        0,
        { flowId },
      )
      const time = dayjs().format('HH:mm')
      await pushMessage(
        {
          comp: Components.DOWNLOAD_CARD,
          props: {
            downloadData: {
              title: '斜坡单元动态风险评价图文',
              createTime: time,
              link: '',
            },
            download: async (isDownload = true) => {
              if (!eventBus.hasListener(EventKey.DOWNLOAD_RISK_ONE_GRAPH)) {
                await router.push('/risk-analysis')
                eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.ONE_GRAPH, data })
                setTimeout(() => {
                  isDownload && eventBus.emit(EventKey.DOWNLOAD_RISK_ONE_GRAPH)
                }, 2000)
              } else {
                isDownload && eventBus.emit(EventKey.DOWNLOAD_RISK_ONE_GRAPH)
              }
            },
          },
        },
        delay,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-text">结合本次风险评价结果，系统已智能生成重点区域的巡查任务更新，可直接推送这些巡查任务，监测员将通过APP收到任务通知。</div>`,
          },
        },
        delay,
        { flowId },
      )

      // await pushMessage(
      //   {
      //     comp: Components.OPERATE_BOX,
      //     props: {
      //       title: '',
      //       operateList: [{ content: '查看巡查任务更新详情' }],
      //       chooseItem,
      //     },
      //   },
      //   delay,
      //   { flowId },
      // )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 一张图推理过程
  const oneGraphInfer = async (data, flowId, delay) => {
    await pushMessage(
      {
        comp: Components.PROCESS_DETAIL,
        props: {
          data,
          scrollToBottom: scrollBottom,
          waitTime: delay === 0 ? [0, 0] : [800, 200],
        },
      },
      delay,
      { flowId },
    )
    await sleep(delay === 0 ? 0 : 5500)
  }

  // 巡查任务详情
  const inspectionDetail = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      let res = null
      try {
        if (businessId.value) {
          res = await getUnitData(businessId.value)
        }
      } catch (e) {
        console.error(e)
      }
      data = { ...res, ...data }
      data.id = businessId.value
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.INSPECT, data })
    }
    startPushMessage()
    try {
      const params = {
        createDate: dayjs(new Date()).format('YYYY-MM-DD 00:00:00'),
        pageNum: 1,
        pageSize: 1,
      }
      if (businessId.value) {
        params.unitId = businessId.value
      }
      const res = await taskDistList(params)
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-text" style="color: #222529">好的，当日任务更新基于<span style="color: #3561FA">「风险等级优先级 + 灾害点对应巡查员」</span>原则，确保高风险单元优先覆盖、任务执行效率最大化。</div>`,
            gap: 10,
          },
        },
        0,
        { flowId },
      )
      if (+res?.total) {
        await pushMessage(
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-text" style="color: #222529">今日已在<span style="color: #3561FA">「调度模式」看板「巡查派发」模块</span>生成${res.total}个巡查任务，任务信息已完整录入，具体内容可点击对应卡片查看。</div>`,
            },
          },
          0,
          { flowId },
        )
        params.status = 1
        const unPush = await taskDistList(params)
        if (+unPush?.total) {
          await pushMessage(
            {
              comp: Components.CONFIRM_BUTTON,
              props: {
                title: '推送重点区域巡查任务',
                confirm: submitTask,
              },
            },
            delay,
            { flowId },
          )
        }
      } else {
        await pushMessage(
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-text" style="color: #222529">经综合评估，当前暂无需新增巡查任务。</div>`,
              gap: 10,
            },
          },
          0,
          { flowId },
        )
      }
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  const pushTask = async (data, flowId) => {
    // 判断是否有按钮权限
    if (!hasAuth(['geo:rwpf:pf']) || !hasRole([ROLE_MAP.DUTY_OFFICER, ROLE_MAP.TOWNSHIP_NATURAL_RESOURCES_DIRECTOR])) {
      await pushMessageTip('暂无权限~')
      return
    }
    startPushMessage()
    const history = data.history
    const delay = history ? 0 : 1000
    let selectParams = null
    let taskList = []
    let filterTaskList = []
    let taskCount = 0
    let uniqueUserIds = 0
    let sourceName = ''
    const normalizeStatusList = (statusList) => {
      if (!Array.isArray(statusList)) return null
      const result = statusList
        .map((status) => Number(status))
        .filter((status) => Number.isFinite(status))
      return result.length ? result : null
    }
    const normalizeCurrentStatusList = (params = {}) => {
      if (Array.isArray(params.currentStatusList)) {
        return normalizeStatusList(params.currentStatusList)
      }
      return isEmptyValue(params.currentStatus) ? null : normalizeStatusList([params.currentStatus])
    }
    if (!history) {
      selectParams = data?.data?.push_task
      // 处置管理单独处理
      if (selectParams.handleId) {
        selectParams = {
          handleId: selectParams.handleId,
        }
        taskList =
          (await getLatestProcessNode({
            handleId: selectParams.handleId,
          })) || []
        if (!hasRole([ROLE_MAP.DUTY_OFFICER, ROLE_MAP.SUPER_ADMIN])) {
          await pushMessageTip('暂无权限推送~')
          return
        }
      } else {
        if (!selectParams || Object.keys(selectParams)?.length === 0) {
          selectParams = {}
          let form = pushTaskQuery.value
          // 区域传参修正
          if (form.mapType == '46') {
            selectParams.pilotArea2 = 1
          } else if (form.mapType == '588') {
            selectParams.pilotArea1 = 1
          }
          // 风险级别
          selectParams.riskLevel = form.riskLevel ? form.riskLevel : null
          if (!form.createDate) {
            form.createDate = dayjs().format('YYYY-MM-DD')
          }
          // 状态处理
          selectParams.currentStatusList = normalizeStatusList(form.currentStatusList)
          selectParams.rootSourceType = (isEmptyValue(form.rootSourceType)) ? null : form.rootSourceType
          selectParams.createDate = form.createDate
            ? dayjs(form.createDate).format('YYYY-MM-DD 00:00:00')
            : dayjs(selectParams.createDate).format('YYYY-MM-DD 00:00:00')
          // 巡查员姓名
          if (form.responsiblePerson) {
            selectParams.responsiblePerson = form.responsiblePerson
          }
          if (form.chainId) {
            selectParams.chainId = form.chainId
            selectParams.chainIds = [form.chainId]
          }
          if (form.unitId) {
            selectParams.unitId = form.unitId
          }
        } else {
          selectParams.currentStatusList = normalizeCurrentStatusList(selectParams)
          delete selectParams.currentStatus
        }
        let chainId = data?.data?.push_task?.chainId
        // 推送单个事件
        if (chainId) {
          selectParams = {}
          selectParams.chainId = chainId
          selectParams.chainIds = [chainId]
        }
        // 推送单个任务
        let taskId = data?.data?.push_task?.taskId
        if (taskId) {
          selectParams = {}
          selectParams.taskId = taskId
        }
        // 推送斜坡单元的全部任务
        if (selectParams.unitId) {
          const res = await getUnitData(selectParams.unitId)
          data = { ...res, ...data }
          data.unitId = selectParams.unitId
        }
        // 获取任务列表
        taskList = await getLatestProcessNode(selectParams)
      }
      taskList = taskList?.data || []
      // 过滤有任务的
      taskList = taskList.filter((item) => {
        taskCount = taskCount + item.pendingPushTaskCount
        return item.pendingPushTaskCount > 0
      })
      taskCount = selectParams.taskId ? 1 : taskCount
      // 根据userId和sourceType聚合，有重复的取第一个
      const groupedTasks = {}

      filterTaskList = await getSendSmsList(selectParams)
      // 算一下列表里面有多少个userId不一样的
      uniqueUserIds = new Set(filterTaskList.map((item) => item.userId)).size
      // 任务来源
      sourceName = getTaskSourceName(taskList)
      // 修改历史记录
      await updateHistoryMsg(data, {
        taskList,
        taskCount,
        uniqueUserIds,
        selectParams,
        filterTaskList,
        sourceName,
      })
    } else {
      const extra = getHistoryExtra(data)
      taskList = extra?.taskList
      selectParams = extra?.selectParams
      taskCount = extra?.taskCount
      uniqueUserIds = extra?.uniqueUserIds
      filterTaskList = extra?.filterTaskList
      sourceName = extra?.sourceName
    }

    if (!taskList?.length) {
      await pushMessageTip('暂无可推送的任务~')
      finishPushMessage()
      return
    }
    try {
      if (!selectParams.id) {
        await pushMessage(
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-title" style="color: #222529">任务推送分析过程：</div>`,
            },
          },
          delay,
          { flowId },
        )
        const processData = [
          {
            title: '确认待推送任务类型和范围',
            contentHtml: getPointHtml(
              `已查询本次任务推送为【${sourceName}】来源${dayjs(selectParams.createDate)?.format('YYYY-MM-DD')}${selectParams.businessId ? `${selectParams.businessId}斜坡单元的` : ''}的所有未推送任务，共计${taskCount}条`,
            ),
          },
          {
            title: '获取每条任务详情及关联人员信息',
            contentHtml: getPointHtml(
              `已获取本次待推送 ${taskCount} 条任务详情，并查询到关联任务接收用户 ${uniqueUserIds || 0}人，已验证所有用户手机号码信息。`,
            ),
          },
          {
            title: '生成任务预览短信及APP端任务表单',
            contentHtml: getPointHtml(
              `已根据任务和人员信息生成 ${filterTaskList?.length || 0} 条短信及${taskCount}条APP端任务，您可以查看下方的预览效果。`,
            ),
          },
        ]
        let finishFLow = false
        await pushMessage(
          {
            comp: Components.PROCESS_DETAIL,
            props: {
              data: processData,
              scrollToBottom: scrollBottom,
              waitTime: delay === 0 ? [0, 0] : [800, 200],
              finishFuc: () => {
                finishFLow = true
              },
            },
          },
          delay,
          { flowId },
        )

        await waitForTaskToComplete(() => finishFLow)

        await pushMessage(
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-text" style="color: #222529">综合上述信息，帮您一键生成<span style="color: #3561FA; font-weight: 800;">「短信及APP任务预览」~</span></div>`,
            },
          },
          delay,
          { flowId },
        )
      } else {
        await pushMessage(
          {
            comp: Components.HTML_TEMPLATE,
            props: {
              templateHtml: `<div class="template-tip-text" style="color: #222529">任务ID为：${selectParams.id}的<span style="color: #3561FA; font-weight: 800;">「短信预览」</span>如下</div>`,
            },
          },
          delay,
          { flowId },
        )
      }

      const dataList = filterTaskList.map((item) => {
        let time = selectParams.createDate
        if (!time) {
          time = dayjs().format('YYYY-MM-DD HH:mm:ss')
        } else if (time.length <= 10 || time.endsWith('00:00:00')) {
          time = dayjs(time).format('YYYY-MM-DD') + ' ' + dayjs().format('HH:mm:ss')
        }
        return {
          smsContent: item.smsContent,
          time,
        }
      })
      await pushMessage(
        {
          comp: Components.SENSITIVE_AREA_PREVIEW,
          props: {
            dataList,
          },
        },
        delay,
        { flowId },
      )

      let confirmShow = ref(true)
      if (!history) {
        const tip = '是否立即执行任务推送?'
        // 获取需要推送的任务列表
        data.messageIndex = await pushMessage(
          {
            comp: Components.CONFIRM_BOX,
            show: confirmShow,
            props: {
              title: tip,
              clickStatus: false,
              confirm: async () => {
                startPushMessage()
                try {
                  const pushParams = {}
                  if (selectParams.chainId) {
                    pushParams.chainId = selectParams.chainId
                    pushParams.chainIds = [selectParams.chainId]
                  } else if (selectParams.handleId) {
                    pushParams.handleId = selectParams.handleId
                  } else if (selectParams.taskId) {
                    pushParams.taskId = selectParams.taskId
                  } else {
                    pushParams.createDate = selectParams.createDate
                      ? dayjs(selectParams.createDate).format('YYYY-MM-DD 00:00:00')
                      : dayjs().format('YYYY-MM-DD 00:00:00')
                    pushParams.rootSourceType = isEmptyValue(selectParams.rootSourceType) ? null : selectParams.rootSourceType
                    pushParams.pushDate = pushParams.createDate
                    pushParams.currentStatusList = normalizeStatusList(selectParams.currentStatusList)
                    pushParams.dynamicRiskLevel = selectParams.dynamicRiskLevel

                    if (selectParams.unitId) {
                      pushParams.unitId = selectParams.unitId
                    }
                    // 巡查员姓名
                    if (selectParams.responsiblePerson) {
                      pushParams.responsiblePerson = selectParams.responsiblePerson
                    }
                    if (selectParams.chainId) {
                      pushParams.chainId = selectParams.chainId
                      pushParams.chainIds = [selectParams.chainId]
                    }
                  }
                  delete pushParams.id
                  const res = await pushTaskDistList(pushParams)
                  if (!res) {
                    await pushMessageTip('推送失败', { delay: 0, flowId })
                  } else {
                    // 通知刷新列表
                    eventBus.emit(EventKey.MODIFY_MESSAGE_LIST)
                    await pushMessage(
                      {
                        comp: Components.HTML_TEMPLATE,
                        props: {
                          templateHtml: `<div class="template-tip-text" style="color: #222529">好的，已通过「短信 + APP 双渠道」向 ${res?.pushName?.length || 0} 名人员下发任务通知，此次推送涵盖${res?.veryHighCount || 0}个极高风险单元 + ${res?.highCount || 0}个高风险单元，接收${getPushReceiverText(res)}。</div>`,
                          gap: 10,
                        },
                      },
                      1000,
                      { flowId },
                    )
                  }
                } catch (e) {
                  await pushMessageTip('推送失败')
                } finally {
                  confirmShow.value = false
                  finishPushMessage()
                }
              },
              cancel: () => {
                confirmShow.value = false
                pushMessageTip('已取消~')
              },
            },
          },
          delay,
          { flowId },
        )
      } else {
        await pushMessageTip('本次操作已处理完成~', { flowId })
      }
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 承灾情况分析
  const analysisOfSituation = async (data, flowId) => {
    const history = data?.history
    const delay = history ? 0 : 1000
    let handleId = queryInputs.value.handleId || route.query.id
    if (!history) {
      // 流程不是在EMERGENCY则禁止
      if (activeFlowTip.value !== FOLLOW_TIP.EMERGENCY) {
        const path = route.path
        const text = path.includes('emergency-response-detail')
          ? '已经分析过了~'
          : '承灾情况分析只支持在处置管理详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.DISASTER_IMPACT_ASSESSMENT, data })
      emergencyStore.setFlowIndex(FOLLOW_TIP.BEFORE_AI_REPORT)
    }
    await nextTick()
    startPushMessage()
    try {
      // 获取详情
      const res = await getDisasterRecordDetailByHandleId(handleId)
      const handleDetail = await getDisasterDetail(handleId)
      // 获取降雨数据
      const rainRes = await getRainfallData(handleDetail?.slopeUnitId)
      // 获取一标三实数据
      const pointRes = await getRiskAreaStat({ handleId })
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title" style="color: #222529">已启动智判承灾，结合GIS信息、应急调查数据深度分析结果如下：</div>`,
            gap: 10,
          },
        },
        delay,
        { flowId },
      )
      const processIndex = await pushMessage(
        {
          comp: Components.PROCESS,
          props: {
            data: [
              {
                title: `已完成威胁范围圈定`,
                list: [
                  {
                    text: `基于滑坡体滑动方向 + 地形坡度，已基于1：2000地图完成威胁范围圈定。`,
                  },
                ],
              },
            ],
          },
        },
        delay,
        { flowId },
      )
      await pushData(
        processIndex,
        {
          title: `已完成承灾体评估`,
          list: [
            {
              text: `已完成承灾体评估：当前评估范围内涉及 ${pointRes?.buildingTotal || 0} 处建筑，覆盖常住人口 ${pointRes?.populationTotal || 0} 人${!pointRes?.age60AndAboveRatio || Number(pointRes?.age60AndAboveRatio) === 0 ? '。' : `，其中 60 岁以上老人占比 ${toPercent(pointRes?.age60AndAboveRatio, 2)}，为本次风险防范重点关注群体。`}`,
            },
          ],
        },
        delay,
      )
      await pushData(
        processIndex,
        {
          title: `已完成发展趋势预测`,
          list: [
            {
              text: `${rainRes?.rainingNow ? '有降雨' : '无降雨'}： ${rainRes?.next24HourRainfallTrend}，最大降雨量为${rainRes?.maxRainfall}mm；`,
            },
          ],
        },
        delay,
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 生成应急报告
  const generateEmergencyReport = async (data, flowId) => {
    const history = data?.history
    const delay = history ? 0 : 1000
    let handleId = queryInputs.value.handleId || route.query.id
    if (!history) {
      // 流程不是在BEFORE_AI_REPORT则禁止
      if (activeFlowTip.value !== FOLLOW_TIP.EMERGENCY) {
        const path = route.path
        let inText = '已经生成过了'
        const text = path.includes('emergency-response-detail') ? inText : '应急报告只支持在处置管理详情页生成哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.GENERATE_EMERGENCY_REPORT, data })
      emergencyStore.setFlowIndex(FOLLOW_TIP.AFTER_AI_REPORT)
    }
    const res = await getDisasterRecordDetailByHandleId(handleId)
    startPushMessage()
    try {
      // 获取详情
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title" style="color: #222529">已启动智判承灾，结合一标三实数据深度分析结果如下：</div>`,
            gap: 10,
          },
        },
        delay,
        { flowId },
      )
      const processIndex = await pushMessage(
        {
          comp: Components.PROCESS,
          props: {
            data: [
              {
                title: `已完成险情任务整合`,
                list: [
                  {
                    text: `涵盖灾害类型（${res?.disasterType || '--'}）、滑体体积、变形特征及风险等级、责任单位关联信息等`,
                  },
                ],
              },
            ],
          },
        },
        delay,
        { flowId },
      )
      await pushData(
        processIndex,
        {
          title: `已生成威胁群众详情`,
          list: [
            { text: `威胁群众${res.threatHouseholds}户， ${res.threatPeople}人，${res.measuresEvacuation}人需撤离` },
          ],
        },
        delay,
      )
      await pushData(
        processIndex,
        {
          title: `已完成基本特征汇总`,
          list: [{ text: `涵盖滑坡地形地貌、规模体量、变形特征及稳定性评估等核心信息` }],
        },
        delay,
      )
      // await pushData(
      //   processIndex,
      //   {
      //     title: `已生成撤离路线图`,
      //     list: [{ text: `明确人员安置、警戒管控、监测巡查、排危除险等 4 项针对性处置措施` }],
      //   },
      //   delay,
      // )
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title" style="color: #222529">综合上述信息，帮您一键生成<span style="color: #3561FA">「应急调查报告」</span>~</div>`,
            gap: 10,
          },
        },
        delay,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 启动专家会审
  const initiateExpertConsultation = async (data, flowId) => {
    const history = data?.history
    if (!history) {
      // 流程不是在BEFORE_AI_REPORT则禁止
      if (activeFlowTip.value !== FOLLOW_TIP.AFTER_AI_REPORT) {
        const path = route.path
        const text = path.includes('emergency-response-detail')
          ? '已经开启过专家会审了~'
          : '专家会审只支持在处置管理详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
      await emergencyStore.nextFlow()
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.INITIATE_EXPERT_CONSULTATION, data })
      emergencyStore.setFlowIndex(FOLLOW_TIP.EXPERT_CONSULTATION)
    }
    // 获取专家列表
    // const res = await getExpertList({})
    // const list = res.data || []
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.BUBBLE,
          props: {
            type: 1,
            message: {
              content: '请选择专家进行连线~',
            },
          },
        },
        0,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  const reset = (cb) => {
    if (isAnswering.value === false) {
      cb?.()
    } else {
      ElMessage.warning('请等待回复完成后新建对话~')
    }
  }

  // 关注斜坡单元
  const focusSlotUnit = async (data) => {
    startPushMessage()
    try {
      // 清除带 ‘type: 'focus_slope',的
      messageList.value = messageList.value.filter((item) => item.type !== 'focus_slope')
      // 预测模式下不做处理
      if (selectMapType.value === 2) {
        return
      }
      const flowId = generateRandomString(26)
      const res = await getSlopeDynimicRiskDetail({
        date: dayjs().format('YYYY-MM-DD 00:00:00'),
        id: data.id,
      })
      const chooseSendItem = async ({ content, type } = {}) => {
        if (!businessId.value) {
          await pushMessage(
            {
              comp: Components.BUBBLE,
              props: {
                type: 1,
                message: {
                  content: '请先选择斜坡单元后点击~',
                },
              },
            },
            0,
          )
          return
        }
        // 获取基础信息
        let appendData = ''
        try {
          if (type === 1) {
            appendData = ''
          } else {
            let unitDetail = await getUnitData(businessId.value)
            const steps = await getRiskAssessmentSteps({
              assessmentId: unitDetail.riskId,
            })
            if (steps.length) {
              unitDetail = {
                ...unitDetail,
                ...steps[0],
              }
              const dataObj = slopeDetailString(unitDetail)
              appendData += dataObj?.baseData?.[0]
              appendData += dataObj?.result?.[0]
              appendData += dataObj?.danger?.[0]
              appendData += dataObj?.damage?.[0]
            }
          }
          submit(content, { otherAppend: appendData })
        } catch (e) {
          console.error(e)
        }
      }
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          type: 'focus_slope',
          props: {
            templateHtml: `<div class="template-tip-title">当前注意到您正在关注 ${data.name || data.slopeUnitName || data.id} 斜坡单元！该单元风险等级为<span style="color: ${RISK_LEVEL_COLOR[res.dynamicRiskLevel]}">【${RISK_LEVEL_TEXT[res.dynamicRiskLevel] || '无'}风险】</span></div>`,
            gap: 10,
          },
        },
        0,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          type: 'focus_slope',
          props: {
            templateHtml: `<div class="template-tip-text">为帮您深入掌握风险细节、精准决策，我梳理了部分关注方向，您可直接选择或自定义提问：</div>`,
          },
        },
        0,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.OPERATE_BOX,
          type: 'focus_slope',
          props: {
            title: '帮我分析',
            icon: new URL('@/assets/imgs/chatBox/operate/analyse.svg', import.meta.url).href,
            operateList: [
              { tip: '研判推理', content: '当前风险等级及核心依据？', type: 1 },
              { tip: '趋势分析', content: '与昨日风险对比有何变化？', type: 1 },
              { tip: '联动剖析', content: '分析该斜坡单元所处乡镇内整体风险情况？', type: 2 },
            ],
            chooseItem: chooseSendItem,
          },
        },
        0,
        { flowId },
      )
      // await pushMessage(
      //   {
      //     comp: Components.OPERATE_BOX,
      //     type: 'focus_slope',
      //     props: {
      //       title: '帮我感知',
      //       icon: new URL('@/assets/imgs/chatBox/operate/setting.svg', import.meta.url).href,
      //       operateList: [{ tip: '动态监控', content: '动态监测指标异常详情？' }],
      //       chooseItem,
      //     },
      //   },
      //   0,
      //   { flowId },
      // )
      await pushMessage(
        {
          comp: Components.OPERATE_BOX,
          type: 'focus_slope',
          props: {
            title: '帮我决策',
            icon: new URL('@/assets/imgs/chatBox/operate/group.svg', import.meta.url).href,
            operateList: [{ tip: '决策助手', content: '需优先采取哪些防控措施？', type: 2 }],
            chooseItem: chooseSendItem,
          },
        },
        0,
        { flowId },
      )
    } finally {
      finishPushMessage()
    }
  }

  // 单元处置流程
  const focusUnitHandleFlow = async (data) => {
    startPushMessage()
    const flowId = generateRandomString(26)
    // 图片总数
    // const total =
    //   (data?.handleDetail?.photoPanorama?.split(',')?.length || 0) +
    //   (data?.handleDetail?.photoDeformation?.split(',')?.length || 0) +
    //   (data?.handleDetail?.photoPanorama?.split(',')?.length || 0)

    const total = (data?.handleDetail?.planePicUrlList?.length || 0) +
      (data?.handleDetail?.sectionalPicUrlList?.length || 0) +
      (data?.handleDetail?.mainPicUrlList?.length || 0)
    await pushMessage(
      {
        comp: Components.HTML_TEMPLATE,
        props: {
          templateHtml: `<div class="template-tip-title">您好！当前进入${data.name || data.slopeUnitName || data.slopeUnitId}斜坡单元处置流程。</div>`,
          gap: 10,
        },
      },
      0,
      { flowId },
    )
    await pushMessage(
      {
        comp: Components.HTML_TEMPLATE,
        props: {
          templateHtml: `<div class="template-tip-point-item">已通知专业技术人员${data?.reporter || '--'}前往现场核查。当前系统全程为本次核查提供数据支撑与后续分析服务！</div>`,
        },
      },
      0,
      { flowId },
    )
    await pushMessage(
      {
        comp: Components.HTML_TEMPLATE,
        props: {
          templateHtml: `<div class="template-tip-title">已接收专业技术人员${data?.handleDetail?.investigator}上报的核实情况，包含现场图像${total || 0}张，基础信息表1份，请进行下一步。</div>`,
          gap: 10,
        },
      },
      0,
      { flowId },
    )
    await pushMessage(
      {
        comp: Components.CONFIRM_BUTTON,
        props: {
          title: '生成应急调查报告',
          confirm: submitTask,
        },
      },
      0,
      { flowId },
    )
    finishPushMessage()
  }

  // 呼叫专家
  const callExpert = async (data, cb, index) => {
    const flowId = generateRandomString(26)
    startPushMessage()
    data = data.filter((item) => item.callType !== 0)
    const consultationLoadingHtml = `<div><span style="color: #3561FA;font-weight: 800;">XX</span>正在实时参与会商...</div>`
    const restoreExpertConsultationLoading = (message) => {
      const processData = message?.props?.data
      if (!Array.isArray(processData)) {
        return
      }
      const consultationItem = processData.find((item) => item?.title === '已完成会商室创建')
      const detail = consultationItem?.list?.find((item) => item?.html?.includes('会话结束') || item?.html?.includes('正在实时参与会商'))
      if (detail) {
        detail.html = consultationLoadingHtml
        detail.pointType = 'loading'
      }
    }
    const callList =
      data?.map((item) => {
        if (item.callType === 1) {
          return { text: `正在呼叫${item?.expertType === 1 ? '水文专家' : '地质专家'}-${item.nickName}...` }
        } else if (item.callType === 3) {
          return { text: `${item?.expertType === 1 ? '水文专家' : '地质专家'}-${item.nickName}连接失败，请重新匹配。` }
        } else if (item.callType === 2) {
          return { text: `${item?.expertType === 1 ? '水文专家' : '地质专家'}-${item.nickName}连接成功。` }
        }
      }) || []
    try {
      // 不存在索引
      if (typeof index !== 'number') {
        const processIndex = await pushMessage(
          {
            comp: Components.PROCESS,
            props: {
              data: [
                {
                  title: `已完成专家组匹配`,
                  showList: true,
                  list: callList,
                },
              ],
            },
          },
          0,
          { flowId },
        )
        await pushData(
          processIndex,
          {
            title: `已完成会商室创建`,
            showList: true,
            list: [
              {
                html: consultationLoadingHtml,
                pointType: 'loading',
              },
            ],
          },
          1000,
        )
        cb(processIndex)
      } else {
        const lastMessage = messageList.value[index]
        lastMessage.props.data[0].list = callList
        restoreExpertConsultationLoading(lastMessage)
        cb(index)
      }
    } finally {
      finishPushMessage()
    }
  }

  const finishExpertConsultation = ({ index } = {}) => {
    const updateMessage = (message) => {
      const processData = message?.props?.data
      if (!Array.isArray(processData)) {
        return false
      }
      let hasUpdate = false
      processData.forEach((item) => {
        item?.list?.forEach((detail) => {
          if (detail?.html?.includes('正在实时参与会商')) {
            detail.html = '<div>会话结束</div>'
            delete detail.pointType
            hasUpdate = true
          }
        })
      })
      return hasUpdate
    }

    if (typeof index === 'number' && messageList.value[index]) {
      const hasUpdate = updateMessage(messageList.value[index])
      if (hasUpdate) {
        updateMessageList()
        return
      }
    }

    const hasUpdate = messageList.value.some((message) => updateMessage(message))
    if (hasUpdate) {
      updateMessageList()
    }
  }

  // 进入处置方案
  const enterIndicatorRiskAnalysis = async ({ delay = 1000, flowId = '' } = {}) => {
    startPushMessage()
    try {
      const processIndex = await pushMessage(
        {
          comp: Components.PROCESS,
          props: {
            data: [
              {
                title: `已完成处置工作方案调用`,
                showList: true,
                list: [{ text: '涵盖灾险点类型、影响范围、风险等级、受威胁人员信息及联系方式等' }],
              },
            ],
          },
        },
        0,
        { flowId },
      )
      await pushData(
        processIndex,
        {
          title: `已生成处置建议清单`,
          showList: true,
          list: [{ text: `明确人员撤离、临时安置、警戒管控、监测巡查、排危除险等针对性处置措施` }],
        },
        delay,
      )
      await pushData(
        processIndex,
        {
          title: `已完成撤离路径规划`,
          showList: true,
          list: [
            {
              text: `基于滑坡体范围圈定需避灾地点与人员，结合村道级路网信息，规划可安置地点与撤离路径，并生成文本化路径描述`,
            },
          ],
        },
        delay,
      )
      await pushData(
        processIndex,
        {
          title: `已生成单点响应处置方案`,
          showList: true,
          list: [{ text: `根据具体风险等级与现场信息，整合处置建议与撤离路线，输出完整的单点响应处置方案文本` }],
        },
        delay,
      )
    } finally {
      finishPushMessage()
    }
  }

  const handlePushMessage = async (item, delay) => {
    startPushMessage()
    try {
      await pushMessage(item, delay)
    } finally {
      finishPushMessage()
    }
  }

  // 开启单点处置方案
  const startIndicatorRiskAnalysis = async (data, flowId) => {
    const history = data?.history
    if (!history) {
      // 流程不是在BEFORE_AI_REPORT则禁止
      if (activeFlowTip.value !== FOLLOW_TIP.EXPERT_CONSULTATION_END) {
        const path = route.path
        const textInfo =
          activeFlowIndex.value > 2 ? '已经执行过了~' : '还未到节点哦，请在方案接入会商结束，等待行政人员审批后进行~'
        const text = path.includes('emergency-response-detail') ? textInfo : '开启处置方案只支持在处置管理详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">确认发送指令后，我将在四级六位的系统中同步任务完成进度。</div>
        `,
          },
        },
        1000,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.CONFIRM_BUTTON,
          props: {
            title: '确认指令发送',
            confirm: submitTask,
          },
        },
        1000,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 确认执行响应方案
  const confirmResponsePlan = async (data, flowId) => {
    const history = data?.history
    if (!history) {
      // 流程不是在BEFORE_AI_REPORT则禁止
      if (activeFlowTip.value !== FOLLOW_TIP.EXPERT_CONSULTATION_END) {
        const path = route.path
        const textInfo =
          activeFlowIndex.value > 2 ? '已经执行过了~' : '还未到节点哦，请在方案接入会商结束，等待行政人员审批后进行~'
        const text = path.includes('emergency-response-detail')
          ? textInfo
          : '确认执行处置方案只支持在处置管理详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
    }
    startPushMessage()
    try {
      const disableCurrentConfirmBox = () => {
        const confirmBox = messageList.value[data.messageIndex]
        if (confirmBox?.comp === Components.CONFIRM_BOX) {
          confirmBox.props.clickStatus = true
          updateMessageList()
        }
      }

      data.messageIndex = await pushMessage(
        {
          comp: Components.CONFIRM_BOX,
          props: {
            title: '确认指令发送后，将开始执行处置方案',
            clickStatus: false,
            confirm: async () => {
              disableCurrentConfirmBox()
              eventBus.emit(EventKey.EXECUTE_SINGLE_DEFENSE_PLAN, async (err) => {
                if (err) {
                  await pushMessage({
                    comp: Components.BUBBLE,
                    props: {
                      type: 1,
                      message: {
                        content: err,
                      },
                    },
                  })
                } else {
                  await pushMessage({
                    comp: Components.BUBBLE,
                    props: {
                      type: 1,
                      message: {
                        content: '指令已下发，正在实时同步各端反馈情况。',
                      },
                    },
                  })
                }
              })
            },
            cancel: () => {
              disableCurrentConfirmBox()
              pushMessage({
                comp: Components.BUBBLE,
                props: {
                  type: 1,
                  message: {
                    content: '已取消~',
                  },
                },
              })
            },
          },
        },
        0,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 生成防御响应方案
  const generateDefenseResponsePlan = async (data, flowId) => {
    const history = data.history
    const delay = 0
    if (!history) {
      // if (defObj.value?.status !== 3) {
      //   const path = route.path
      //   const info = defObj.value?.status < 3 ? '还未到节点哦，会商确认后执行~' : '已经生成过方案了~'
      //   const text = path.includes('defense-response-detail')
      //     ? info
      //     : '生成防御响应方案只支持在防御响应详情页使用哦~'
      //   await pushMessage(
      //     {
      //       comp: Components.BUBBLE,
      //       props: {
      //         type: 1,
      //         message: {
      //           content: text,
      //         },
      //       },
      //     },
      //     0,
      //     { flowId },
      //   )
      //   return
      // }
      // eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.GENERATE_DEFENSIVE_PLAN, data })
    }

    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">已为您生成《${defObj.value?.name || '防御响应方案'}》（${getLevelText(defObj.value?.level)}级）</div>`,
          },
        },
        delay,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.PROCESS,
          props: {
            data: [
              {
                title: `方案已生成完成`,
                list: [
                  {
                    html: `本次方案基于防御响应等级、斜坡单元风险评价及历史险情，您可点击<span style="color: #3561FA; font-weight: 800; cursor: pointer;" data-action="see-detail">「查看详情」</span>预览完整方案，支持大模型自动优化或人工编辑调整。`,
                  },
                ],
              },
            ],
            handleClick: (e) => {
              if (e.target.getAttribute('data-action')) {
                const action = e.target.getAttribute('data-action')
                if (action === 'see-detail') {
                  eventBus.emit(EventKey.VIEW_PLAN, { status: 2 })
                }
              }
            },
          },
        },
        delay,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }
  // 生成专项巡排查任务
  const generateSpecialInspectionTask = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      if (defObj.value?.status !== 4) {
        const path = route.path
        const inPtahText = defObj.value?.status < 4 ? '行政审批后才能生成过专项巡查任务哦' : '已生成过专项巡查任务'
        const text = path.includes('defense-response-detail')
          ? inPtahText
          : '生成专项巡排查任务只支持在防御响应详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.CREATE_INSPECTION_TASKS, data })
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">正在基于当日斜坡单元风险评价生成此次防御响应专项巡查任务</div>`,
          },
        },
        delay,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }
  // 终止响应
  const stopResponse = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      if (defObj.value?.status !== 5) {
        const path = route.path
        const text = path.includes('defense-response-detail')
          ? '还无法结束哦~'
          : '终止响应只支持在防御响应详情页使用哦~'
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: text,
              },
            },
          },
          0,
          { flowId },
        )
        return
      }
      eventBus.emit(EventKey.OPERATE, { type: EVENT_TYPE.TERMINATE_RESPONSE, data })
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">收到防御响应终止通知，此次${defObj.value?.name || '防御响应'}（${getLevelText(defObj.value?.level)} 级）已结束。</div>`,
          },
        },
        delay,
        { flowId },
      )
      // await pushMessage(
      //   {
      //     comp: Components.HTML_TEMPLATE,
      //     props: {
      //       templateHtml: `<div class="template-tip-point-item">全流程数据已自动归档至后台，支持生成<span style="color: #3561FA; font-weight: 800; cursor:pointer;" data-action="response-report">《响应复盘报告》</span>。</div>`,
      //       handleClick: (e) => {
      //         if (e.target.getAttribute('data-action')) {
      //           const action = e.target.getAttribute('data-action')
      //           if (action === 'response-report') {
      //           }
      //         }
      //       },
      //     },
      //   },
      //   delay,
      //   { flowId },
      // )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 右键点击菜单
  const handleRightClick = async (data) => {
    const lng = Number(data?.lng)
    const lat = Number(data?.lat)
    if (Number.isFinite(lng) && Number.isFinite(lat)) {
      const longitude = Number(lng.toFixed(6))
      const latitude = Number(lat.toFixed(6))
      try {
        const inputAfter = async () => {
          await pushMessage(
            {
              comp: Components.BUBBLE,
              props: {
                type: 1,
                message: {
                  content: `正在调取该位置的多维度数据...`,
                },
              },
            },
            1000,
          )
        }
        queryInputs.value = {
          ...(queryInputs.value || {}),
          longitude,
          latitude,
        }
        await submit(`对坐标 [东经：${convertToDMS(longitude)}, 北纬：${convertToDMS(latitude)}] 进行空间分析`, { inputAfter })
        await waitForTasksToComplete()
      } finally {
        // queryInputs.value = omit(queryInputs.value || {}, ['longitude', 'latitude', 'slope', 'regions'])
      }
    }
  }

  // 生成趋势预测报文
  const generateTrendPredictionMessage = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      const path = route.path
      if (!path.includes('prediction')) {
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: '生成趋势预测报文只支持在预测模式中使用哦~',
              },
            },
          },
          0,
          { flowId },
        )
      }
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-text">好的，请问您是否要生成<span style="color: #3561FA; font-weight: 800">「月度」的地质灾害趋势预测报文？</div>`,
          },
        },
        delay,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.CONFIRM_BUTTON,
          props: {
            title: '生成月度趋势预测报文',
            confirm: () => {
              submit('生成月度趋势预测报文')
              // operateBusinessOuter({
              //   text: '生成月度趋势预测报文',
              //   data: {
              //     type: EVENT_TYPE.GENERATE_ANY_TREND_PREDICTION_MESSAGE,
              //     data: {
              //       type: 3,
              //     },
              //   },
              // })
            },
          },
        },
        { flowId },
      )
      // await pushMessage(
      //   {
      //     comp: Components.CONFIRM_BUTTON,
      //     props: {
      //       title: '生成季度趋势预测报文',
      //       confirm: () => {
      //         submit('生成季度趋势预测报文')
      //
      //         // operateBusinessOuter({
      //         //   text: '生成季度趋势预测报文',
      //         //   data: {
      //         //     type: EVENT_TYPE.GENERATE_ANY_TREND_PREDICTION_MESSAGE,
      //         //     data: {
      //         //       type: 4,
      //         //     },
      //         //   },
      //         // })
      //       },
      //     },
      //   },
      //   { flowId },
      // )
      // await pushMessage(
      //   {
      //     comp: Components.CONFIRM_BUTTON,
      //     props: {
      //       title: '生成年度趋势预测报文',
      //       confirm: () => {
      //         submit('生成年度趋势预测报文')
      //
      //         // operateBusinessOuter({
      //         //   text: '生成年度趋势预测报文',
      //         //   data: {
      //         //     type: EVENT_TYPE.GENERATE_ANY_TREND_PREDICTION_MESSAGE,
      //         //     data: {
      //         //       type: 5,
      //         //     },
      //         //   },
      //         // })
      //       },
      //     },
      //   },
      //   { flowId },
      // )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 生成预测报文
  const generatePredictionMessage = async (data, flowId) => {
    const history = data.history
    const delay = history ? 0 : 1000
    if (!history) {
      const path = route.path
      if (!path.includes('prediction')) {
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: '生成预测报文只支持在预测模式中使用哦~',
              },
            },
          },
          0,
          { flowId },
        )
      }
    }
    startPushMessage()
    const type = data.data.type
    let date = data?.data?.prediction_date_str || ''
    date = formatDateLabel(date)
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-text">好的，我将根据趋势预测模型，为您生成<span style="color: #3561FA; font-weight: 800">「${date}地质灾害趋势预测报告」</span>，请稍候～</div>`,
          },
        },
        delay,
        { flowId },
      )
      await eventBus.emitCb(EventKey.GENERATE_ANY_TREND_PREDICTION_MESSAGE, {
        type,
        data,
      })

      await pushMessage(
        {
          comp: Components.PROCESS,
          props: {
            data: [
              {
                title: `报告生成已完成`,
                list: [],
              },
            ],
          },
        },
        delay,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.DOWNLOAD_CARD,
          props: {
            downloadData: {
              title: `${date}地质灾害趋势预测报文`,
              createTime: dayjs().format('YYYY-MM-DD'),
              link: '',
            },
            download: async () => {
              eventBus.emit(EventKey.PRD_DOWNLOAD_RISK_ONE_GRAPH)
            },
          },
        },
        delay,
        { flowId },
      )
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title">请在专家会商后将报文推送至四级六位责任体系相关负责人，做好风险预警与防控部署。</div>`,
          },
        },
        delay,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  // 推送预测报文
  const pushPredictionMessage = async (data, flowId) => {
    const history = data.history
    if (!history) {
      const path = route.path
      if (!path.includes('prediction-detail')) {
        await pushMessage(
          {
            comp: Components.BUBBLE,
            props: {
              type: 1,
              message: {
                content: '推送预测报文支持支在预测模式-预测详情中使用哦~',
              },
            },
          },
          0,
          { flowId },
        )
      }
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.CONFIRM_BOX,
          props: {
            title: '确认指令发送后，将为相关责任人系统发送任务',
            clickStatus: false,
            confirm: async () => {
              startPushMessage()
              try {
                const res = await eventBus.emitCb(EventKey.PUSH_PRD_TASK)
                if (res.length) {
                  // 通知刷新列表
                  await pushMessage(
                    {
                      comp: Components.HTML_TEMPLATE,
                      props: {
                        templateHtml: `<div class="template-tip-text" style="color: #222529">已将${res[0]?.title}地质灾害趋势预测报文同步推送至四级六位责任体系相关责任人系统，本次共发送<span style="color: #3561FA; font-weight: 800">${res[0]?.personCount}</span>人，推送完成！</div>`,
                        gap: 10,
                      },
                    },
                    1000,
                    { flowId },
                  )
                  eventBus.emit(EventKey.PRED_MODIFY_MESSAGE_LIST)
                } else {
                  await pushMessage({
                    comp: Components.BUBBLE,
                    props: {
                      type: 1,
                      message: {
                        content: '推送失败',
                      },
                    },
                  })
                }
              } catch (e) {
                await pushMessage({
                  comp: Components.BUBBLE,
                  props: {
                    type: 1,
                    message: {
                      content: '推送失败',
                    },
                  },
                })
              } finally {
                finishPushMessage()
              }
            },
            cancel: () => {
              pushMessage({
                comp: Components.BUBBLE,
                props: {
                  type: 1,
                  message: {
                    content: '已取消~',
                  },
                },
              })
            },
          },
        },
        0,
        { flowId },
      )
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  const operateBusinessOuter = async ({ text, data } = {}) => {
    const flowId = generateRandomString(26)
    if (text) {
      await pushMessage(
        {
          comp: Components.BUBBLE,
          props: {
            type: 2,
            message: {
              content: text,
            },
          },
        },
        0,
      )
    }
    await operateBusiness(data, flowId)
  }

  // 跳转问答模式、调度模式
  const operateDispatchMode = async (data) => {
    const history = data.history
    if (history) {
      await pushMessageTip('已跳转')
      return
    }
    const type = data.data?.apptype
    const routeMap = {
      [CHAT_TYPE.NORMAL]: '/chat-engine',
      un_normal: '/risk-analysis',
      unnormal: '/risk-analysis',
      defense_response: '/defense-response',
      task_track: '/task-track',
      message_center: '/message-center',
    }
    const targetPath = routeMap[type]
    if (!targetPath) return

    startPushMessage()
    try {
      if (route.path === targetPath) {
        await pushMessageTip('已经在此页面了')
        return
      }
      await router.push(targetPath)
    } finally {
      finishPushMessage()
    }
  }

  // 无权限操作
  const operateNoPermission = async () => {
    await pushMessage(
      {
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '暂无权限操作哦~',
          },
        },
      },
      0,
    )
  }

  // 确认生成复盘报告
  const generateReplayReport = async (data) => {
    if (!data.isEnd) {
      eventBus.emit(EVENT_TYPE.GENERATE_REVIEW_REPORT, { mdText: data.mdText, isEnd: false })
    } else {
      eventBus.emit(EVENT_TYPE.GENERATE_REVIEW_REPORT, { mdText: '', isEnd: true })
    }
  }

  // 生成复盘报告
  const generateReplayReportGet = async (data) => {
    // 判断是否在详情页
    const replayReportRoutePaths = ['defense-response-detail', 'emergency-response-detail', 'task-track']
    const isReplayReportRoute = replayReportRoutePaths.some((path) => route.path.includes(path))

    if (!isReplayReportRoute) {
      await pushMessage({
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '请在响应详情页进行复盘报告生成~',
          },
        },
      })
    } else {
      await pushMessage({
        comp: Components.CONFIRM_BOX,
        props: {
          title: '确认生成复盘报告',
          confirm: async () => {
            queryInputs.value = {
              report_id: route.path.includes('emergency-response-detail') ? route.query.id + '' : handleId.value + '',
            }
            submit('确认生成复盘报告')
          },
          cancel: () => {
            pushMessage({
              comp: Components.BUBBLE,
              props: {
                type: 1,
                message: {
                  content: '已取消~',
                },
              },
            })
          },
        },
      })
    }
  }

  // 报灾AI分析
  const disasterAiAnalysis = async (data, flowId) => {
    const history = data?.history
    const delay = history ? 0 : 1000
    const id = data?.data?.disaster_id
    if (!id) {
      await pushMessageTip('进行报灾事件分析时请带上id~，如执行报灾事件（ID:1234567）AI分析')
      return
    }
    // 获取报灾详情
    const res = await getDisReportDetail(id)
    if (!res) {
      await pushMessageTip('未查询到报灾信息~')
      return
    }
    startPushMessage()
    try {
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title" style="color: #222529">报灾事件AI分析过程：</div>`,
            gap: 10,
          },
        },
        delay,
        { flowId },
      )
      let listData = []
      const photoList = (await getPhotoUrls(res?.photos)) || []
      listData.push({
        title: '图片加载和预处理',
        imgList: photoList,
      })

      let content = ''
      let outData = data
      // 报灾详情已保存的空间分析结果。
      const savedSpaceAnalysis = res?.aiAnalysisData
      if (savedSpaceAnalysis) {
        listData.push({
          title: '上报地点空间分析',
          contentHtml: md.render(decodeJsonStringContent(savedSpaceAnalysis)),
        })
      } else if (!history) {
        const analysisItem = reactive({
          title: '上报地点空间分析',
          contentHtml: '',
          callback: () => {
            return new Promise((resolve) => {
              const { lng, lat } = getCenter(res?.checkCenter)
              let inputs = {
                longitude: lng,
                latitude: lat,
              }
              const { abort } = chat({
                params: {
                  query: `对坐标 [东经：${lng}, 北纬：${lat}] 进行空间分析`,
                  agentType: CHAT_TYPE.NOT_SHOW,
                  inputs: inputs,
                },
                onMessage: async (data) => {
                  if (data?.eventType === 'MESSAGE') {
                    content += data?.answer
                    analysisItem.contentHtml = md.render(content)
                    scrollBottom()
                  } else if (data?.eventType === 'WORKFLOW_FINISHED') {
                    // 修改历史记录
                    await updateHistoryMsg(outData, {
                      areaDetail: content,
                    })
                    abort()
                    scrollBottom()
                    setTimeout(() => {
                      resolve()
                    }, 200)
                  }
                },
              })
            })
          },
        })
        listData.push(analysisItem)
      } else {
        const extra = getHistoryExtra(data)
        const content = extra?.areaDetail
        const html = md.render(content)
        const obj = {
          title: '上报地点空间分析',
          contentHtml: html,
        }
        listData.push(obj)
      }

      // 获取标记
      let arr = transformAiDataToOptions(res?.aiVisionProps, DISASTER_FIELD_ENUM)
      const sortOrder = [
        'imageType',
        'dzStage',
        'deformation',
        'bearing_type',
        'freshEextent',
        'disasterType',
        'riskExtent',
      ]
      arr.sort((a, b) => {
        const indexA = sortOrder.indexOf(a.value)
        const indexB = sortOrder.indexOf(b.value)
        const orderA = indexA === -1 ? sortOrder.length : indexA
        const orderB = indexB === -1 ? sortOrder.length : indexB
        return orderA - orderB
      })
      arr = arr.filter((item) => item?.dataValue) || []
      let html = ''
      if (arr?.length) {
        arr.forEach((info) => {
          html += getPointHtml(
            `<div style="font-weight: 800;">${info?.label}: <span>${info?.dataValue || '--'}</span></div>`,
          )
        })
        listData.push({
          title: '多模态大模型图像识别',
          contentHtml: `<div style="display: flex; flex-direction: column; gap: 5px">${html}</div>`,
        })
      }
      listData.push({
        title: '推理大模型进行风险判级',
        contentHtml: getPointHtml(
          `<div style="">风险等级：<span style="color: ${LEVEL_COLOR[res?.aiRiskLevel]}; font-weight: 800;">${LEVEL_TEXT[res?.aiRiskLevel] || '暂无'}</span></div>`,
        ),
      })
      listData.push({
        title: '输出详细识别报告',
        contentHtml: md.render(res?.aiReportDetail || ''),
      })
      let finishFLow = false
      await pushMessage(
        {
          comp: Components.PROCESS_DETAIL,
          props: {
            data: listData,
            scrollToBottom: scrollBottom,
            waitTime: delay === 0 ? [0, 0] : [800, 200],
            gap: 20,
            useLocalShow: true,
            finishFuc: () => {
              finishFLow = true
            },
          },
        },
        delay,
        { flowId },
      )

      await waitForTaskToComplete(() => finishFLow, 50, 100000)

      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div style="width: 100%; height: 1px; background: #EEF8FF"></div>`,
            gap: 20,
          },
        },
        delay,
        { flowId },
      )

      const statusColor = {
        [REPORT_STATUS.TO_DO]: '#3561fa',
        [REPORT_STATUS.REPORT]: '#ff5500',
        [REPORT_STATUS.DONE]: '#2ca86e',
      }
      await pushMessage(
        {
          comp: Components.HTML_TEMPLATE,
          props: {
            templateHtml: `<div class="template-tip-title" style="color: #222529">该任务状态：<span style="color: ${statusColor[res?.status]}; font-weight: 800;">「${REPORT_STATUS_TESXT[res?.status]}」</span>；</div>`,
            gap: 20,
          },
        },
        0,
        { flowId },
      )

      if (res.status == 1) {
        if (res.sourceType === REPORT_SOURCE.TASK) {
          let showConfirmMsg = ref(true)
          await pushMessage(
            {
              comp: Components.CONFIRM_BUTTON,
              props: {
                title: '帮我进行短信速报',
                size: 'small',
                theme: 'gray',
                show: showConfirmMsg,
                confirm: () => {
                  pushConfirmBox(
                    {
                      title: '短信速报',
                      errorMsg: '操作失败',
                      handleFun: async () => {
                        await handleReport('up', res, () => {
                          eventBus.emit(EventKey.REFRESH_DISASTER_PAGE)
                          showConfirmMsg.value = false
                        })
                      },
                    },
                    delay,
                    flowId,
                  )
                },
                gap: 10,
              },
            },
            0,
            { flowId },
          )
        }

        if (res.sourceType === REPORT_SOURCE.PEOPLE) {
          const showConfirmTask = ref(true)
          await pushMessage(
            {
              comp: Components.CONFIRM_BUTTON,
              props: {
                title: '帮我下发任务',
                size: 'small',
                theme: 'gray',
                show: showConfirmTask,
                confirm: async () => {
                  await pushConfirmBox(
                    {
                      title: '下发任务',
                      errorMsg: '操作失败',
                      handleFun: async () => {
                        await handleReport('down', res, () => {
                          eventBus.emit(EventKey.REFRESH_DISASTER_PAGE)
                          showConfirmTask.value = false
                        })
                      },
                    },
                    delay,
                    flowId,
                  )
                },
                gap: 10,
              },
            },
            0,
            { flowId },
          )
        }

        let showConfirm = ref(true)
        await pushMessage(
          {
            comp: Components.CONFIRM_BUTTON,
            props: {
              title: '帮我设置为非灾险情',
              size: 'small',
              theme: 'gray',
              gap: 10,
              show: showConfirm,
              confirm: async () => {
                await pushConfirmBox(
                  {
                    title: '设置为非灾险情',
                    errorMsg: '操作失败',
                    handleFun: async () => {
                      await closeReportAction(res, () => {
                        eventBus.emit(EventKey.REFRESH_DISASTER_PAGE)
                        showConfirm.value = false
                      })
                    },
                  },
                  delay,
                  flowId,
                )
              },
            },
          },
          0,
          { flowId },
        )

        await pushMessage(
          {
            comp: Components.CONFIRM_BUTTON,
            props: {
              title: '模型误判上报',
              size: 'small',
              theme: 'gray',
              confirm: async () => {
                eventBus.emit(EventKey.REPORT_MODEL_ERROR, res)
              },
            },
          },
          0,
          { flowId },
        )
      }
    } catch (e) {
      console.error(e)
    } finally {
      finishPushMessage()
    }
  }

  /**
   * 定位斜坡单元或灾害点；不在风险评价视图时，确认后跳转并定位。
   * @param {object} data - 定位意图数据。
   * @param {string} data.slope - 斜坡单元 ID。
   * @param {string} data.hazard - 灾害点 ID。
   * @param {string} flowId - 当前操作流程 ID。
   * @returns {Promise<void>}
   */
  const positionRiskTarget = async (data = {}, flowId = '') => {
    // 历史记录只展示原对话，不触发地图定位、页面跳转及关联播报。
    if (isHistory || data.history) {
      return
    }

    // 本次需要定位的斜坡单元或灾害点信息。
    const positionData = {
      slope: data.slope || '',
      hazard: data.hazard || '',
    }
    if (!positionData.slope && !positionData.hazard) {
      await pushMessageTip('未获取到需要定位的斜坡单元或灾害点~', { flowId })
      return
    }

    // 当前风险评价页是否已经直接完成定位。
    const positionResults = await eventBus.emitCb(EventKey.POSITIONING_INTENT, positionData)
    const positionResult = positionResults.find(Boolean)
    if (positionResult) {
      if (typeof positionResult === 'string') {
        await pushMessageTip(positionResult, { flowId })
      }
      return
    }

    // 无地图页面跳转后会新建 Viewer，先作废上一个地图实例的完成状态。
    if (route.meta.notUseMap) {
      cesiumStore.setDrawComplete(false)
    }
    // 跨页面跳转前先订阅激活事件，避免激活信号在 router.push 完成前丢失。
    const riskPageReadyPromise = route.path !== '/risk-analysis' ? waitForRiskAnalysisReady() : Promise.resolve(true)
    if (route.path !== '/risk-analysis') {
      await router.push('/risk-analysis')
    }
    const riskPageReady = await riskPageReadyPromise
    if (!riskPageReady) {
      await pushMessageTip('定位页面加载失败，请稍后重试~', { flowId })
      return
    }
    // 跳转后强制进入风险评价视图并执行定位。
    const jumpPositionResults = await eventBus.emitCb(EventKey.POSITIONING_INTENT, positionData, {
      forceRiskView: true,
    })
    const jumpPositionResult = jumpPositionResults.find(Boolean)
    if (typeof jumpPositionResult === 'string') {
      await pushMessageTip(jumpPositionResult, { flowId })
    } else if (!jumpPositionResult) {
      await pushMessageTip('定位页面加载失败，请稍后重试~', { flowId })
    }
  }

  // 处理业务方法
  const operateBusiness = async (data = {}, flowId = '', { extraData = {} } = {}) => {
    if (data.type === EVENT_TYPE.QUERY_SLOPE_UNIT) {
      businessId.value = data?.data?.param || businessId.value
      await getSlopeDetail(data, flowId)
    } else if (data.type === EVENT_TYPE.ONE_GRAPH) {
      if (!hasAuth(['geo:create:picture']) && !customRecordTypes.value[CUSTOM_RECORD_TYPE.ONE_GRAPH]) {
        await operateNoPermission()
        return
      }
      if (!customRecordTypes.value[CUSTOM_RECORD_TYPE.ONE_GRAPH]) {
        await saveUserQuestion({
          businessType: CUSTOM_RECORD_TYPE.ONE_GRAPH,
          businessContent: true,
          scope: 1,
        })
        customRecordTypes.value[CUSTOM_RECORD_TYPE.ONE_GRAPH] = true
      }
      businessId.value = data?.data?.param || businessId.value
      // 斜坡单元一张图
      if (!data?.history) {
        await router.push('/risk-analysis')
      }
      await getSlopeDynamicRisk(data, flowId)
    } else if (data.type === EVENT_TYPE.INSPECT) {
      // 巡查任务详情
      if (!data?.history) {
        await router.push('/task-track')
      }
      // await inspectionDetail(data, flowId)
    } else if (data.type === EVENT_TYPE.PUSH_TASK) {
      if (!hasAuth(['geo:rwpf:pf'])) {
        await operateNoPermission()
        return
      }
      // 推送巡查任务
      if (!data?.history && !['/task-track', '/emergency-response-detail'].includes(route.path)) {
        await pushMessageTip('请前往调度模式-事件闭环页面进行操作~')
        return
      }
      await nextTick()
      await pushTask(data, flowId)
    } else if (data.type === EVENT_TYPE.DISASTER_IMPACT_ASSESSMENT) {
      // 承灾情况分析
      await analysisOfSituation(data, flowId)
    } else if (data.type === EVENT_TYPE.GENERATE_EMERGENCY_REPORT) {
      // 生成应急报告
      if (!hasAuth(['geo:yjczlc:scdcbg'])) {
        await operateNoPermission()
        return
      }
      await generateEmergencyReport(data, flowId)
    } else if (data.type === EVENT_TYPE.INITIATE_EXPERT_CONSULTATION) {
      if (!hasAuth(['geo:yjczlc:qdzjhsyj'])) {
        await operateNoPermission()
        return
      }
      await initiateExpertConsultation(data, flowId)
    } else if (data.type === EVENT_TYPE.CONFIRM_RESPONSE_PLAN) {
      if (!hasAuth(['geo:yjczlc:fbfa'])) {
        await operateNoPermission()
        return
      }
      // 确认方案并一键发送
      await startIndicatorRiskAnalysis(data, flowId)
    } else if (data.type === EVENT_TYPE.SEND_RESPONSE_PLAN) {
      if (!hasAuth(['geo:yjczlc:fbfa'])) {
        await operateNoPermission()
        return
      }
      // 确认执行响应方案
      await confirmResponsePlan(data, flowId)
    } else if (data.type === EVENT_TYPE.GENERATE_DEFENSIVE_PLAN) {
      // 生成防御方案
      // 检查用户角色，若无权限则提示并返回
      // if (!hasAuth(['geo:fyxy:scfa'])) {
      //   await operateNoPermission()
      //   return
      // }
      await generateDefenseResponsePlan(data, flowId)
    } else if (data.type === EVENT_TYPE.TERMINATE_RESPONSE) {
      // 终止响应
      // 检查用户角色，若无权限则提示并返回
      if (!hasAuth(['geo:fyxy:jsxy'])) {
        await operateNoPermission()
        return
      }
      await stopResponse(data, flowId)
    } else if (data.type === EVENT_TYPE.CREATE_INSPECTION_TASKS) {
      // 生成专项巡排查任务
      // 检查用户角色，若无权限则提示并返回
      if (!hasAuth(['geo:fyxy:sczxxcrw'])) {
        await operateNoPermission()
        return
      }
      await generateSpecialInspectionTask(data, flowId)
    } else if (data.type === EVENT_TYPE.GENERATE_TREND_PREDICTION_MESSAGE) {
      await router.push('/prediction')
      // 检查用户是否具有值班员角色，若无权限则提示并返回
      if (!hasAuth(['geo:qsyc:scbw'])) {
        await operateNoPermission()
        return
      }
      // 生成趋势预测报文
      if (data.data?.time) {
        const typeMapper = {
          month: 3,
          quarter: 4,
          year: 5,
        }
        data.data.type = typeMapper[data.data.time]
        await generatePredictionMessage(data, flowId)
      } else {
        await generateTrendPredictionMessage(data, flowId)
      }
    } else if (data.type === EVENT_TYPE.GENERATE_ANY_TREND_PREDICTION_MESSAGE) {
      // 生成x度趋势预测报文
      await generatePredictionMessage(data, flowId)
    } else if (data.type === EVENT_TYPE.PUSH_ANY_TREND_PREDICTION_MESSAGE) {
      // 推送预测报文
      // 检查用户是否具有值班员角色，若无权限则提示并返回
      if (!hasAuth(['geo:pre:tsycbw'])) {
        await operateNoPermission()
        return
      }
      await pushPredictionMessage(data, flowId)
    } else if (data.type === EVENT_TYPE.SCREEN_TRANSITION) {
      await operateDispatchMode(data)
    } else if (data.type === EVENT_TYPE.GENERATE_REVIEW_REPORT) {
      // 确认生成复盘报告
      await generateReplayReport(data)
    } else if (data.type === EVENT_TYPE.REVIEW_REPORT_GET) {
      // 生成复盘报告
      await generateReplayReportGet(data)
    } else if (data.type === EVENT_TYPE.DISASTER_INTENT) {
      // 报灾AI分析
      await disasterAiAnalysis(data, flowId)
    } else if (data.type === EVENT_TYPE.POSITIONING_INTENT) {
      // 定位到 斜坡单元或者灾害点
      await positionRiskTarget(data, flowId)
    }
  }

  onMounted(() => {
    if (!useListen) {
      return
    }
    eventBus.on(EventKey.ADD_COMPONENT, handlePushMessage)
    eventBus.onQueue(EventKey.FOCUS_UNIT, focusSlotUnit)
    eventBus.on(EventKey.RISK_DEAL_UNIT, focusUnitHandleFlow)
    eventBus.on(EventKey.CALL_EXPERT, callExpert)
    eventBus.on(EventKey.END_EXPERT_CONSULTATION, finishExpertConsultation)
    eventBus.on(EventKey.INDICATOR_RISK_ANALYSIS, indicatorRiskAnalysis)
    eventBus.on(EventKey.ENTER_DISPOSAL_PLAN, enterIndicatorRiskAnalysis)
    eventBus.on(EventKey.MAP_RIGHT_CLICK, handleRightClick)
    eventBus.on(EventKey.OPERATE_BUSINESS, operateBusinessOuter)
    eventBus.on(EventKey.ADD_PROCESS_DATA, pushProcessData)
  })

  onBeforeUnmount(() => {
    eventBus.off(EventKey.ADD_COMPONENT, handlePushMessage)
    eventBus.off(EventKey.FOCUS_UNIT, focusSlotUnit)
    eventBus.off(EventKey.RISK_DEAL_UNIT, focusUnitHandleFlow)
    eventBus.off(EventKey.CALL_EXPERT, callExpert)
    eventBus.off(EventKey.END_EXPERT_CONSULTATION, finishExpertConsultation)
    eventBus.off(EventKey.INDICATOR_RISK_ANALYSIS, indicatorRiskAnalysis)
    eventBus.off(EventKey.ENTER_DISPOSAL_PLAN, enterIndicatorRiskAnalysis)
    eventBus.off(EventKey.MAP_RIGHT_CLICK, handleRightClick)
    eventBus.off(EventKey.OPERATE_BUSINESS, operateBusinessOuter)
    eventBus.off(EventKey.ADD_PROCESS_DATA, pushProcessData)
  })

  return {
    operateBusiness,
    messageList,
    loading,
    isAnswering,
    pushMessage,
    startPushMessage,
    finishPushMessage,
    reset,
    businessId,
    conversationId,
  }
}
export default useChatBox
