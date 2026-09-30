import assistantImg from '@/assets/imgs/agents/assistant.png'
import dispatcherImg from '@/assets/imgs/agents/distritor.png'
import lawImg from '@/assets/imgs/agents/law.png'
import writerImg from '@/assets/imgs/agents/writter.png'
import hazardReviewImg from '@/assets/imgs/agents/agent-business-hazard-review.png'
import hazardReviewHoverImg from '@/assets/imgs/agents/agent-business-hazard-review.gif'
import imageRecognitionBusinessImg from '@/assets/imgs/agents/agent-business-image-recognition.png'
import imageRecognitionBusinessHoverImg from '@/assets/imgs/agents/agent-business-image-recognition.gif'
import dynamicPlanImg from '@/assets/imgs/agents/agent-business-dynamic-plan.png'
import dynamicPlanHoverImg from '@/assets/imgs/agents/agent-business-dynamic-plan.gif'
import reviewReportImg from '@/assets/imgs/agents/agent-business-review-report.png'
import reviewReportHoverImg from '@/assets/imgs/agents/agent-business-review-report.gif'
import spatialAnalysisImg from '@/assets/imgs/agents/agent-business-spatial-analysis.png'
import spatialAnalysisHoverImg from '@/assets/imgs/agents/agent-business-spatial-analysis.gif'
import riskEvaluationImg from '@/assets/imgs/agents/agent-business-risk-evaluation.png'
import riskEvaluationHoverImg from '@/assets/imgs/agents/agent-business-risk-evaluation.gif'
import qaDataImg from '@/assets/imgs/agents/agent-business-qa-data.png'
import qaDataHoverImg from '@/assets/imgs/agents/agent-business-qa-data.gif'
import meetingMinutesImg from '@/assets/imgs/agents/agent-business-meeting-minutes.png'
import meetingMinutesHoverImg from '@/assets/imgs/agents/agent-business-meeting-minutes.gif'
import writingImg from '@/assets/imgs/agents/agent-document-writing.png'
import outlineImg from '@/assets/imgs/agents/agent-writing-outline.png'
import pptImg from '@/assets/imgs/agents/agent-ppt.png'
import excelImg from '@/assets/imgs/agents/agent-excel.png'
import polishImg from '@/assets/imgs/agents/agent-text-polish.png'
import proofreadImg from '@/assets/imgs/agents/agent-proofread.png'
import weeklyImg from '@/assets/imgs/agents/agent-weekly-report.png'
import monthlyImg from '@/assets/imgs/agents/agent-monthly-report.png'
import planImg from '@/assets/imgs/agents/agent-work-plan.png'
import conversionImg from '@/assets/imgs/agents/agent-format-conversion.png'
import translateImg from '@/assets/imgs/agents/agent-translation.png'
import imageRecognitionImg from '@/assets/imgs/agents/agent-image-recognition.png'
import toneImg from '@/assets/imgs/agents/agent-tone.png'
import newBadgeImg from '@/assets/imgs/agents/badge-new.png'

export const AGENT_CATEGORIES = [
  { label: '全部分类', value: 'all' },
  { label: '文档助手', value: 'document' },
  { label: '日常办公', value: 'office' },
  { label: '实用工具', value: 'utility' },
  { label: '其他', value: 'other' },
]

export const AGENT_SECTIONS = [
  { label: '专属业务智能体', value: 'business' },
  { label: '通用办公助手', value: 'general' },
]

const AGENT_CATEGORY_LABELS = Object.fromEntries(
  AGENT_CATEGORIES.filter((category) => category.value !== 'all').map((category) => [category.value, category.label]),
)

export const AGENT_PROFILES = {
  dispatcher: {
    title: '调度助手',
    description: '帮助您规划任务分配、协调人员安排并跟进工作进度。',
    image: dispatcherImg,
    questions: ['如何合理安排今天的巡查任务？', '多部门联合处置时如何分工？', '帮我整理一份任务跟进清单'],
  },
  government: {
    title: '政务助手',
    description: '提供政务办理指南、材料梳理、流程说明与时限提醒。',
    image: writerImg,
    questions: ['地质灾害隐患点认定需要哪些材料？', '群测群防员工作流程是什么？', '帮我梳理地灾项目申报流程'],
  },
  law: {
    title: '法规助手',
    description: '查询法律法规条文、解读政策要求并梳理合规要点。',
    image: lawImg,
    questions: ['地质灾害防治条例有哪些核心要求？', '隐患排查责任如何划分？', '应急避险转移有哪些法规依据？'],
  },
  newcomer: {
    title: '新人助手',
    description: '快速了解岗位职责、业务流程、平台操作与常见问题。',
    image: assistantImg,
    questions: ['日常巡查需要重点关注什么？', '如何上报新的地质灾害隐患？', '预警信息发布后需要做哪些工作？'],
  },
  statistic: {
    title: '地灾统计',
    description: '汇总地质灾害业务数据，辅助生成日报、周报和月报。',
    image: assistantImg,
    questions: ['帮我生成地质灾害统计日报', '帮我生成地质灾害统计周报', '帮我生成地质灾害统计月报'],
  },
}

const businessTool = (tool) => ({
  section: 'business',
  category: 'business',
  source: '业务专属',
  usage: '116人使用过',
  ...tool,
})

const generalTool = (tool) => ({
  section: 'general',
  usage: '116人使用过',
  ...tool,
  source: AGENT_CATEGORY_LABELS[tool.category] || '',
})

export const AGENT_TOOLS = [
  businessTool({
    title: '隐患复核智能体',
    description: '比对历次调查资料，辅助隐患复核。',
    image: hazardReviewImg,
    hoverImage: hazardReviewHoverImg,
    action: '立即使用',
    route: '/warning/hazard-review',
  }),
  businessTool({
    title: 'AI识图智能体',
    description: '识别现场照片，辅助标出异常。',
    image: imageRecognitionBusinessImg,
    hoverImage: imageRecognitionBusinessHoverImg,
    route: '/chat-engine/chatting?type=image_recognition',
  }),
  businessTool({
    title: '动态预案智能体',
    description: '汇聚最新数据，生成更新预案。',
    image: dynamicPlanImg,
    hoverImage: dynamicPlanHoverImg,
    route: '/emergency',
  }),
  businessTool({
    title: '复盘报告智能体',
    description: '复盘处置过程，输出改进建议。',
    image: reviewReportImg,
    hoverImage: reviewReportHoverImg,
    prompt: '@复盘报告智能体 请协助我复盘本次处置过程。',
  }),
  businessTool({
    title: '空间分析智能体',
    description: '关联地上地下，汇聚空间信息。',
    image: spatialAnalysisImg,
    hoverImage: spatialAnalysisHoverImg,
    prompt: '@空间分析智能体 请协助我进行空间分析。',
    route: '/agents/space-analysis',
  }),
  businessTool({
    title: '风险评价智能体',
    description: '综合监测气象，开展风险评价。',
    image: riskEvaluationImg,
    hoverImage: riskEvaluationHoverImg,
    prompt: '@风险评价智能体 请协助我开展风险评价。',
  }),
  businessTool({
    title: '问答问数智能体',
    description: '查询地灾知识，统计业务数据。',
    image: qaDataImg,
    hoverImage: qaDataHoverImg,
    prompt: '@问答问数智能体 请回答我的地灾业务问题。',
  }),
  businessTool({
    title: '会商纪要智能体',
    description: '上传会议材料，智能生成结构化纪要。',
    image: meetingMinutesImg,
    hoverImage: meetingMinutesHoverImg,
    route: '/chat-engine/chatting?type=meeting_minutes',
  }),
  generalTool({
    title: '公文撰写',
    description: '万能格式转换神器',
    category: 'document',
    tag: 'NEW',
    badgeImage: newBadgeImg,
    image: writingImg,
    route: '/agents/writing',
  }),
  generalTool({
    title: '写作大纲',
    description: '创建清晰的写作结构',
    category: 'document',
    image: outlineImg,
    prompt: '请帮我创建一份结构清晰的写作大纲。',
  }),
  generalTool({
    title: 'PPT制作',
    description: '一句话生成PPT',
    category: 'document',
    image: pptImg,
    prompt: '请根据我的需求生成一份PPT内容大纲。',
  }),
  generalTool({
    title: 'Excel函数大师',
    description: '各种事项办事指南',
    category: 'document',
    image: excelImg,
    prompt: '请帮助我解决Excel函数或数据处理问题。',
  }),
  generalTool({
    title: '文本润色大师',
    description: '文本润色，下笔如有神',
    category: 'document',
    image: polishImg,
    prompt: '请对我接下来提供的文本进行润色。',
  }),
  generalTool({
    title: '内容校对',
    description: '校对内容，保证消息无误',
    category: 'document',
    image: proofreadImg,
    prompt: '请校对我接下来提供的内容，并指出需要修改的地方。',
  }),
  generalTool({
    title: '格式转换',
    description: '万能格式转换神器',
    category: 'utility',
    image: conversionImg,
    route: '/agents/conversion',
  }),
  generalTool({
    title: '图片处理',
    description: '快速处理图片',
    category: 'utility',
    image: imageRecognitionImg,
    route: '/chat-engine/chatting?type=image_recognition',
  }),
  generalTool({
    title: '工作周报',
    description: '总结一周工作成果',
    category: 'office',
    image: weeklyImg,
    prompt: '请根据我提供的工作内容生成一份工作周报。',
  }),
  generalTool({
    title: '工作月报',
    description: '全面回顾每月工作',
    category: 'office',
    image: monthlyImg,
    prompt: '请根据我提供的工作内容生成一份工作月报。',
  }),
  generalTool({
    title: '工作计划',
    description: '量身定制工作计划',
    category: 'office',
    image: planImg,
    prompt: '请根据我的目标制定一份可执行的工作计划。',
  }),
  generalTool({
    title: 'AI翻译',
    description: '熟练掌握翻译技巧',
    category: 'utility',
    image: translateImg,
    prompt: '请翻译我接下来提供的内容，并保留原文语义和格式。',
  }),
  generalTool({
    title: '调整语气',
    description: '调整语气适配不同场合',
    category: 'other',
    image: toneImg,
    prompt: '请根据我说明的使用场景，调整文本语气。',
  }),
]
