// eventBus.js
import { EVENT_TYPE } from '@/components/ChatBox/config.js'

class EventBus {
  constructor() {
    this.events = new Map()
  }

  // 订阅
  on(event, callback) {
    if (!this.events.has(event)) {
      this.events.set(event, new Set())
    }
    this.events.get(event).add(callback)
  }

  // 取消订阅
  off(event, callback) {
    if (!this.events.has(event)) return

    if (!callback) {
      // 移除该事件的所有订阅
      this.events.delete(event)
    } else {
      const fns = this.events.get(event)
      if (fns.has(callback)) {
        fns.delete(callback)
      } else {
        // 处理被包装的 callback（如 once 或 onQueue）
        for (const fn of fns) {
          if (fn.originalCallback === callback) {
            fns.delete(fn)
            break
          }
        }
      }
    }
  }

  // 只订阅一次
  once(event, callback) {
    const wrapper = (...args) => {
      callback(...args)
      this.off(event, wrapper)
    }
    wrapper.originalCallback = callback
    this.on(event, wrapper)
  }

  // 队列执行的订阅（多次触发时，按顺序等待前一个执行完毕）
  onQueue(event, callback) {
    let queue = Promise.resolve()
    const wrapper = (...args) => {
      queue = queue.then(async () => {
        try {
          await callback(...args)
        } catch (err) {
          console.error(`[EventBus onQueue error] event: ${event}`, err)
        }
      })
      return queue
    }
    wrapper.originalCallback = callback
    this.on(event, wrapper)
  }

  // 发布
  emit(event, ...args) {
    if (!this.events.has(event)) return // 拷贝一份，防止 emit 中 off 造成遍历问题
    ;[...this.events.get(event)].forEach((fn) => fn(...args))
  }

  /**
   * 带回调的 emit
   * 返回所有监听器的返回值
   */
  async emitCb(event, ...args) {
    if (!this.events.has(event)) return []

    const tasks = [...this.events.get(event)].map((fn) => fn(...args))
    return Promise.all(tasks)
  }

  // 清空所有事件
  clear() {
    this.events.clear()
  }

  // 是否存在监听器
  hasListener(event) {
    return this.events.has(event) && this.events.get(event).size > 0
  }
}

// 单例导出
export const eventBus = new EventBus()

// 全局sse自定义key
export const NOTICE_KEY = {
  // 修改意见汇总
  MODIFY_OPINION_SUMMARY: 'NOTICE_KEY_modify_opinion_summary',
  // 专家打钩
  EXPERT_CHECK: 'NOTICE_KEY_expert_check',
  // 修改报告
  MODIFY_REPORT: 'NOTICE_KEY_modify_report',
  // 重新生成报告
  REGENERATE_REPORT: 'NOTICE_KEY_regenerate_report',
  // 审核成功
  AUDIT_SUCCESS: 'NOTICE_KEY_audit_success',
  // 远程静音
  REMOTE_MUTE: 'NOTICE_KEY_remote_mute',
  // 关闭会议室
  CLOSE_MEETING: 'NOTICE_KEY_close_meeting',
  // 通知刷新页面
  REFRESH_PAGE: 'NOTICE_KEY_refresh_page',
  // 通知刷新等级
  REFRESH_LEVEL: 'NOTICE_KEY_refresh_level',
  // 刷新ai管理通知列表
  REFRESH_AI_MANAGE_NOTICE: 'ai-hosting-overview-changed',
}

// 业务意图对应的语音播报文案；未配置的意图不播报。
export const EVENT_TTS_TEXT_MAP = Object.freeze({
  [EVENT_TYPE.QUERY_SLOPE_UNIT]: '斜坡单元详情已查询完成。',
  [EVENT_TYPE.ONE_GRAPH]: '风险研判报文已生成。',
  [EVENT_TYPE.PUSH_TASK]: '巡查任务如下。',
  [EVENT_TYPE.REPORT_DETAIL]: '信息报送详情已查询完成。',
  [EVENT_TYPE.EQP_DETAIL]: '预警详情已查询完成。',
  [EVENT_TYPE.DISASTER_IMPACT_ASSESSMENT]: '承灾情况分析已完成。',
  [EVENT_TYPE.GENERATE_EMERGENCY_REPORT]: '应急报告已生成。',
  [EVENT_TYPE.INITIATE_EXPERT_CONSULTATION]: '会商已启动。',
  [EVENT_TYPE.CONFIRM_RESPONSE_PLAN]: '响应方案已确认。',
  [EVENT_TYPE.SEND_RESPONSE_PLAN]: '响应方案已发送。',
  [EVENT_TYPE.GENERATE_DEFENSIVE_PLAN]: '防御响应方案已生成。',
  [EVENT_TYPE.CREATE_INSPECTION_TASKS]: '专项巡排查任务已生成。',
  [EVENT_TYPE.TERMINATE_RESPONSE]: '已终止响应。',
  [EVENT_TYPE.GENERATE_TREND_PREDICTION_MESSAGE]: '趋势预测报文已生成。',
  [EVENT_TYPE.GENERATE_ANY_TREND_PREDICTION_MESSAGE]: '指定周期的趋势预测报文已生成。',
  [EVENT_TYPE.PUSH_ANY_TREND_PREDICTION_MESSAGE]: '趋势预测报文已推送。',
  [EVENT_TYPE.SCREEN_TRANSITION]: '页面已跳转。',
  [EVENT_TYPE.GENERATE_REVIEW_REPORT]: '复盘报告已生成。',
  [EVENT_TYPE.REVIEW_REPORT_GET]: '复盘报告相关信息已获取。',
  [EVENT_TYPE.DISASTER_INTENT]: '灾害AI分析已完成。',
})

export const EventKey = {
  // 提交
  SUBMIT: 'SUBMIT',
  // 选择cesium模型
  SELECT_CESIUM_MODEL: 'SELECT_CESIUM_MODEL',
  // 关注斜坡单元
  FOCUS_UNIT: 'FOCUS_UNIT',
  // 重置页面
  RESET_PAGE: 'RESET_PAGE',
  // 动态风险页面已激活，可接收跨页面定位意图
  RISK_ANALYSIS_READY: 'RISK_ANALYSIS_READY',
  // 操作消息
  OPERATE: 'OPERATE',
  // 全局sse消息key
  CHAT_MSG: 'CHAT_MSG',
  // 下载动态风险一张图
  DOWNLOAD_RISK_ONE_GRAPH: 'DOWNLOAD_RISK_ONE_GRAPH',
  // 分监测员重点区域任务更新清单导出
  DOWNLOAD_TASK_DIST_LIST: 'DOWNLOAD_TASK_DIST_LIST',
  // 修改业务ID
  CHANGE_BUSINESS_ID: 'CHANGE_BUSINESS_ID',
  // 重置对话
  RESET_DIALOG: 'RESET_DIALOG',
  // 重置地图
  RESET_MAP: 'RESET_MAP',
  // 险情核实单元详情
  RISK_DEAL_UNIT: 'RISK_DEAL_UNIT',
  // 呼叫专家
  CALL_EXPERT: 'CALL_EXPERT',
  // 结束专家会商
  END_EXPERT_CONSULTATION: 'END_EXPERT_CONSULTATION',
  // 指示牌风险分析
  INDICATOR_RISK_ANALYSIS: 'INDICATOR_RISK_ANALYSIS',
  // 进入处置方案
  ENTER_DISPOSAL_PLAN: 'enter_disposal_plan',
  // 外部添加组件
  ADD_COMPONENT: 'ADD_COMPONENT',
  // 查看方案
  VIEW_PLAN: 'VIEW_PLAN',
  // 任务推送成功刷新列表
  MODIFY_MESSAGE_LIST: 'MODIFY_MESSAGE_LIST',
  // 地图右键分析坐标
  MAP_RIGHT_CLICK: 'MAP_RIGHT_CLICK',
  // 右键点击
  MAP_RIGHT_CLICK_ANALYSIS: 'MAP_RIGHT_CLICK_ANALYSIS',
  // 右键关闭
  MAP_RIGHT_CLICK_CLOSE: 'MAP_RIGHT_CLICK_CLOSE',
  // 操作业务事件
  OPERATE_BUSINESS: 'OPERATE_BUSINESS',
  // 下载预测动态风险一张图
  PRD_DOWNLOAD_RISK_ONE_GRAPH: 'PRD_DOWNLOAD_RISK_ONE_GRAPH',
  // 推送预测任务
  PUSH_PRD_TASK: 'PUSH_PRD_TASK',
  // 预测任务推送成功刷新列表
  PRED_MODIFY_MESSAGE_LIST: 'PRED_MODIFY_MESSAGE_LIST',
  // 单点模拟获取路径
  GET_SINGLE_PATH: 'GET_SINGLE_PATH',
  // 添加processData
  ADD_PROCESS_DATA: 'ADD_PROCESS_DATA',
  // 执行单点防御处置方案
  EXECUTE_SINGLE_DEFENSE_PLAN: 'EXECUTE_SINGLE_DEFENSE_PLAN',
  // 操作天气预警历史记录-打开/关闭
  OPERATE_WEATHER_WARNING_HISTORY: 'OPERATE_WEATHER_WARNING_HISTORY',
  SELECT_HISTORY_RECORD: 'SELECT_HISTORY_RECORD',
  // 开启一张图推理过程
  OPEN_ONE_GRAPH_INFERENCE: 'OPEN_ONE_GRAPH_INFERENCE',
  // 展示一张图弹窗
  SHOW_ONE_GRAPH_WINDOW: 'SHOW_ONE_GRAPH_WINDOW',
  // 发送区域选择变化数据
  SEND_AREA_CHANGE_DATA: 'send_area_change_data',
  // 设置区域组件数据
  SET_AREA_DATA: 'set_area_data',
  // 通知报灾页面刷新
  REFRESH_DISASTER_PAGE: 'refresh_disaster_page',
  // 通知防御模式页面刷新
  REFRESH_DEFENSE_MODE_PAGE: 'refresh_defense_mode_page',
  // 信息报送-模型误判上报
  REPORT_MODEL_ERROR: 'report_model_error',
  // 防御响应-选择上传预警报告
  CHOOSE_WARNING_REPORT_FILE: 'choose_warning_report_file',
  // 任务推送短信预览
  PUSH_TASK_SMS_PREVIEW: 'push_task_sms_preview',
  // 事件Key
  ...EVENT_TYPE,
  // 自定义全局key
  ...NOTICE_KEY,
}
