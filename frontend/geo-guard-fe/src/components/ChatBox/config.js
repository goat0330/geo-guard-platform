import { shallowRef } from 'vue'
import Bubble from '@/components/ChatBox/Bubble/index.vue'
import Refresh from '@/components/ChatBox/Refresh/index.vue'
import OperateBox from '@/components/ChatBox/Operate/index.vue'
import Process from '@/components/ChatBox/Process/index.vue'
import DownloadCard from '@/components/ChatBox/DownloadCard/index.vue'
import HtmlTemplate from '@/components/ChatBox/HtmlTemplate/index.vue'
import ConfirmButton from '@/components/ChatBox/ComfirmBtn/index.vue'
import ConfirmBox from '@/components/ChatBox/ConfirmBox/index.vue'
import Table from '@/components/ChatBox/Table/index.vue'
import ExportDownloadCard from '@/components/ChatBox/DownloadCard/exportDownloadCard.vue'
import OperateRefresh from '@/components/ChatBox/OperateRefresh/index.vue'
import { RISK_LEVEL_TEXT, RISK_OCCUR } from '@/utils/enum.js'
import { toFixed, formatMetersToKm } from '@/utils/index.js'
import dayjs from 'dayjs'
import FlowEngine from './FlowEngine/index.vue'
import PhoneShow from '@/components/ChatBox/PhoneShow/phoneShow.vue'

export const Components = {
  // 对话
  BUBBLE: 'BUBBLE',
  // 刷新
  REFRESH: 'REFRESH',
  // 操作列表
  OPERATE_BOX: 'OPERATE_BOX',
  // 推理列表抽屉
  PROCESS: 'PROCESS',
  // 下载卡片
  DOWNLOAD_CARD: 'DOWNLOAD_CARD',
  // 导出下载卡片
  EXPORT_DOWNLOAD_CARD: 'EXPORT_DOWNLOAD_CARD',
  // html模板
  HTML_TEMPLATE: 'HTML_TEMPLATE',
  // 确认按钮
  CONFIRM_BUTTON: 'CONFIRM_BUTTON',
  // 确认框
  CONFIRM_BOX: 'CONFIRM_BOX',
  // 表格
  TABLE: 'TABLE',
  // 查询列表
  OPERATE_REFRESH: 'OPERATE_REFRESH',
  // 推理流程
  PROCESS_DETAIL: 'PROCESS_DETAIL',
  // 短信预览
  SENSITIVE_AREA_PREVIEW: 'SENSITIVE_AREA_PREVIEW',
}

export const componentsMapper = {
  [Components.BUBBLE]: shallowRef(Bubble),
  [Components.REFRESH]: shallowRef(Refresh),
  [Components.OPERATE_BOX]: shallowRef(OperateBox),
  [Components.PROCESS]: shallowRef(Process),
  [Components.DOWNLOAD_CARD]: shallowRef(DownloadCard),
  [Components.EXPORT_DOWNLOAD_CARD]: shallowRef(ExportDownloadCard),
  [Components.HTML_TEMPLATE]: shallowRef(HtmlTemplate),
  [Components.CONFIRM_BUTTON]: shallowRef(ConfirmButton),
  [Components.CONFIRM_BOX]: shallowRef(ConfirmBox),
  [Components.TABLE]: shallowRef(Table),
  [Components.OPERATE_REFRESH]: shallowRef(OperateRefresh),
  [Components.PROCESS_DETAIL]: shallowRef(FlowEngine),
  [Components.SENSITIVE_AREA_PREVIEW]: shallowRef(PhoneShow),
}

export const EVENT_TYPE = {
  // 斜坡单元详情
  QUERY_SLOPE_UNIT: 'query_slope_unit',
  // 动态风险评价“一张图”
  ONE_GRAPH: 'one_graph',
  // 巡查任务
  INSPECT: 'inspect',
  // 推送重点区域巡查任务
  PUSH_TASK: 'push_task',
  // 斜坡单元风险程度详单
  CHECK_RISK_DETAIL: 'check_risk_detail',
  // 信息报送详情
  REPORT_DETAIL: 'report_detail',
  // 预警详情
  EQP_DETAIL: 'eqp_detail',
  // 承灾情况分析
  DISASTER_IMPACT_ASSESSMENT: 'disaster_impact_assessment',
  // 生成应急报告
  GENERATE_EMERGENCY_REPORT: 'generate_emergency_report',
  // 启动专家会商
  INITIATE_EXPERT_CONSULTATION: 'initiate_expert_consultation',
  // 确认响应方案
  CONFIRM_RESPONSE_PLAN: 'confirm_response_plan',
  // 发送响应方案
  SEND_RESPONSE_PLAN: 'send_response_plan',
  // 生成防御响应方案
  GENERATE_DEFENSIVE_PLAN: 'generate_defensive_plan',
  // 生成专项巡排查任务
  CREATE_INSPECTION_TASKS: 'create_inspection_tasks',
  // 终止响应
  TERMINATE_RESPONSE: 'terminate_response',
  // 生成趋势预测报文
  GENERATE_TREND_PREDICTION_MESSAGE: 'geo_gen_trend',
  // 生成x度趋势预测报文
  GENERATE_ANY_TREND_PREDICTION_MESSAGE: 'generate_any_trend_prediction_message',
  // 推送X月/第X季度/X年地质灾害趋势预测报文
  PUSH_ANY_TREND_PREDICTION_MESSAGE: 'geo_push_trend',
  // 跳转页面
  SCREEN_TRANSITION: 'screen_transition',
  // cesium初始化绘制完毕
  CESIUM_DRAW_COMPLETE: 'cesium_draw_complete',
  // 确认生成复盘报告
  GENERATE_REVIEW_REPORT: 'retrospective_report',
  // 生成复盘报告
  REVIEW_REPORT_GET: 'retrospective_report_intent',
  // 报灾AI分析
  DISASTER_INTENT: 'disaster_intent',
  // 定位 斜坡，隐患点
  POSITIONING_INTENT: 'positioning_intent',
}

// 分隔符
export const splitTip = '(@data)'

export const initMessageMapper = {
  '/risk-analysis': async ({ chooseItem }) => {
    return [
      {
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '您好，我是XX，请问有什么可以帮您？',
          },
          gap: 30,
        },
      },
      {
        comp: Components.OPERATE_REFRESH,
        props: {
          title: '帮我查询',
          icon: new URL('@/assets/imgs/chatBox/operate/search.png', import.meta.url).href,
          questions: questionList.select || [],
          chooseItem,
        },
      },
    ]
  },
  '/defense-response': async ({ chooseItem }) => {
    return [
      {
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '您好，我是XX，请问有什么可以帮您？',
          },
          gap: 30,
        },
      },
      {
        comp: Components.OPERATE_BOX,
        props: {
          title: '帮我查询',
          icon: new URL('@/assets/imgs/chatBox/operate/search.png', import.meta.url).href,
          operateList: [
            { tip: '智能查询', content: 'Ⅲ级响应需落实哪些措施？' },
            { tip: '智能查询', content: '如何新增关联单点响应？' },
            { tip: '智能查询', content: '什么是防御响应？' },
          ],
          chooseItem,
        },
      },
      {
        comp: Components.PROCESS_DETAIL,
      },
    ]
  },
  '/defense-response-detail': async ({ chooseItem }) => {
    return [
      {
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '您好，我是XX，请问有什么可以帮您？',
          },
          gap: 30,
        },
      },
      {
        comp: Components.OPERATE_BOX,
        props: {
          title: '帮我查询',
          icon: new URL('@/assets/imgs/chatBox/operate/search.png', import.meta.url).href,
          operateList: [
            { tip: '智能查询', content: 'Ⅲ级响应需落实哪些措施？' },
            { tip: '智能查询', content: '如何新增关联单点响应？' },
            { tip: '智能查询', content: '什么是防御响应？' },
          ],
          chooseItem,
        },
      },
      {
        comp: Components.PROCESS_DETAIL,
      },
    ]
  },
  '/task-track': async ({ chooseItem }) => {
    return [
      {
        comp: Components.BUBBLE,
        props: {
          type: 1,
          message: {
            content: '您好，我是XX，请问有什么可以帮您？',
          },
          gap: 30,
        },
      },
      {
        comp: Components.OPERATE_REFRESH,
        props: {
          title: '帮我查询',
          icon: new URL('@/assets/imgs/chatBox/operate/search.png', import.meta.url).href,
          questions: questionList.select || [],
          chooseItem,
        },
      },
    ]
  },
}

// 斜坡单元详细数据
export const slopeDetailString = (data) => {
  console.log('data ====== ', data)
  let sevenRain = ''
  data?.rainfallPast7Daily?.map((item) => {
    // const result = dayjs(item?.date, 'YYYYMMDD HH:mm:ss').format('YYYY-MM-DD HH:mm:ss')
    const key = Object.keys(item)?.[0]
    sevenRain += `${key}: ${toFixed(item[key]) || 0}mm\n`
  })
  data.vulnerabilityDisplayJson = data?.vulnerabilityDisplayJson || {}
  const slopeRawName = data?.name || data?.slopeUnitName || (data?.slopeUnitId ? `${data.slopeUnitId}号` : '') || ''
  const slopeDisplayName = slopeRawName.endsWith('斜坡') || slopeRawName.endsWith('斜坡单元') ? slopeRawName : `${slopeRawName}斜坡单元`
  return {
    baseData: [
      `${slopeDisplayName}的基本信息如下：斜坡位于${data?.province}${data?.city}${data?.county}${data?.street}${data?.village}，为${data?.adjacentWater}的${data?.unitMorphology || '--'}，坡高${Number(data?.elevationDiff || 0).toFixed(2)}m，坡度${data?.slopeMean}°，坡向${Number(data?.aspectMean || 0).toFixed(2)}°，为${data?.lithologyDesc || '--'}。`,
    ],
    result: [
      `按照滑坡易发性评价指标体系，智能体将提取地质条件、地形条件、植被条件、水文条件四类指标，调取数据如下：该斜坡为${data?.adjacentWater || '--'}斜坡；斜坡形态为${data?.unitMorphology || '--'}；坡度为${data?.slopeMean || '--'}°；地形起伏${data?.elevationDiff || ''}；工程切坡${data?.vulnerabilityDisplayJson?.road_house_perimeter || '0'}m；斜坡结构为${data?.slopeStructure || '--'}；工程岩组为${data?.lithologyDesc || '--'}；最近断层距离为${data?.tectonicDist || 0}km；植被覆盖率${data?.vegetationCover || 0}。综合分析，该斜坡易发性等级为${RISK_LEVEL_TEXT[data?.susceptibilityLevel]}。`,
    ],
    danger: [
      // `${data?.slopeUnitId}斜坡单元的危险性等级为${RISK_LEVEL_TEXT[data?.hazardLevel]}危险性，易发性等级为${RISK_LEVEL_TEXT[data?.susceptibilityLevel]}易发，\n过去七天的降雨量\n${sevenRain}预测未来一天累计降雨量${toFixed(data?.rainfallForecast) || 0}mm`,
      `根据近7天降雨数据显示，该斜坡\n${sevenRain}\n前期有效降雨为${toFixed(data?.rainfallPast7, 2) || 0}mm，预测24小时降雨量为${toFixed(data?.rainfallForecast) || 0}mm；综合诱发性系数为${data?.combinedTimeProb || '--'}。叠加易发性等级，该斜坡危险性等级为${RISK_LEVEL_TEXT[data?.hazardLevel]}。`,
    ],
    damage: [
      `基于一标三实等承灾体数据，对斜坡易损性进行分析。该斜坡面积为${data?.vulnerabilityDisplayJson.area_m2 || '--'}㎡，常住人口数量为${data?.vulnerabilityDisplayJson?.pop_count || '--'}人，内有建筑物${data?.vulnerabilityDisplayJson?.house_count || 0}栋。综合分析，该斜坡易损性等级为${RISK_LEVEL_TEXT[data?.vulnerabilityLevel]}。`,
    ],
    risk: [
      `${slopeDisplayName}的风险性等级为${RISK_LEVEL_TEXT[data?.riskLevel]}风险性，该斜坡危险性等级为${RISK_LEVEL_TEXT[data?.hazardLevel]}危险性，易损性等级为${RISK_LEVEL_TEXT[data?.vulnerabilityLevel]}易损性，依据地质灾害风险等级矩阵分析，综合判定该斜坡单元的风险等级为${RISK_LEVEL_TEXT[data?.riskLevel]}风险性。`,
    ],
  }
}

// 问题列表
export const questionList = {
  // select: [
  //   '恩施市近3天的高风险斜坡单元有多少？',
  //   `龙凤镇{start}-{end}的中风险斜坡单元数量是多少？`,
  //   '金子坝街道昨日的低风险斜坡单元有多少个？',
  //   '{unit}斜坡单元今天的风险等级是多少？',
  //   // '{unit}斜坡单元今日的降雨量是多少？',
  //   '{unit}斜坡单元近10年来发生过几起灾害？',
  //   '恩施市今年的防御响应事件有哪些？',
  //   '{unit}斜坡单元本周派发的巡查任务有哪些？',
  //   '恩施市本月的处置事件有多少？',
  //   // '今年恩施市发生过几次气象风险预警？',
  //   '试点区近一个月发生的滑坡事件有多少？',
  //   // '恩施大峡谷风景管理处近10年来发生的灾害事件有多少？',
  //   '恩施市发生过滑坡的斜坡单元数量有多少？',
  // ],
  select: [
    '试点区极高风险斜坡单元数量是多少？',
    '示范区极高风险斜坡单元数量是多少？',
    '试点区高风险斜坡单元数量是多少？',
    '示范区高风险斜坡单元数量是多少？',
    '今日各乡镇/街道高风险斜坡单元分布情况如何？',
    '各乡镇/街道高风险斜坡单元数量分别是多少？',
    '示范区高风险斜坡单元对应的斜坡名称有哪些？',
    '恩施市各乡镇高风险斜坡单元数量最多的是哪个乡镇？',
    '恩施市各乡镇高风险斜坡单元数量最少的是哪个乡镇？',
    '恩施市高风险斜坡单元数量最多的乡镇及数量是多少？',
    '示范区内面积最大的高风险斜坡单元中心点坐标在哪里？',
    '试点区内面积最大的高风险斜坡单元面积是多少？',
    '示范区内高风险斜坡单元总面积是多少？',
    '各乡镇高风险斜坡单元总面积最大的是哪个乡镇？',
    '屯堡乡与红土乡高风险斜坡单元总面积哪个更大？',
  ],
}

export const ReasoningProcess = ({ unitNum, urlMapper }) => {
  return [
    {
      loading: true,
      title: '全域时空数据基座加载',
      contentText: `风险评价智能体从天空地数据基座，批量调取全域地形地貌、地质构造、生态水文等全量时空数据，完成 ${unitNum} 个斜坡单元的基础地形指标计算、要素叠加、多源数据匹配与校准，构建基于高精度数据的计算基底，支撑全域动态风险四性评价。`,
    },
    {
      loading: true,
      title: '区域「地质灾害易发性」评价',
      contentText: `智能体基于地形地貌、地质构造、生态水文等评价因子，并使用SMOTE过采样和类权重策略优化数据类型分布，对全域斜坡单元进行基于AutoML+Stacking集成模型的多分类训练，加权求和得到易发性概率并反向阈值映射得到最终的易发性等级，针对示范区，采用级联LightGBM模型进行预测纠偏。使用训练好的模型进行合并全量推断，完成易发性评价。`,
      imgList: [urlMapper['susceptibilityLevel']],
      type: 1,
    },
    {
      loading: true,
      title: '区域「地质灾害危险性」评价',
      contentText: `智能体耦合降雨等级（实况降雨、预报降雨）、人类工程活动（道路工程扰动、建筑工程扰动）等时间诱发因子，加权计算得到诱发概率，并阈值划分得到诱发等级。基于危险性判断矩阵，综合易发性等级（作为纵向因子）和诱发等级（作为横向因子）两个维度进行动态危险性等级的判断。`,
      imgList: [urlMapper['hazardLevel']],
      type: 2,
    },
    {
      loading: true,
      title: '区域「地质灾害易损性」评价',
      contentText: `智能体综合考量人口样本（居民）和单位样本（企业、个体户等），基于斜坡单元面积与人口样本数量计算得到人口密度、汇总单位样本的注册资本作为经济强度，归一化后按权重合成得到易损性得分，最后采用Jenks分级得到各个斜坡单元的易损性等级，用以评估承载体暴露所可能导致的人员与财产损失风险。`,
      imgList: [urlMapper['riskLevel']],
      type: 4,
    },
    {
      loading: true,
      title: '区域「地质灾害风险性」评价',
      contentText: `智能体耦合危险性与易损性结果，基于风险性判断矩阵，综合危险性等级（作为纵向因子）和易损性等级（作为横向因子）两个维度进行动态风险性等级的判断，并基于危险性指数、人口暴露和经济暴露计算得到综合风险指数，进行风险性分级排序。`,
      imgList: [urlMapper['vulnerabilityLevel']],
      type: 3,
    },
  ]
}
