import { createMd } from '@/utils/md.js'

/**
 * 0920 复盘报告内页2 示例 Markdown 正文数据
 */
export const DEMO_REPORT_MARKDOWN = `
# 一、事件概况

2026年4月11日16时35分，彭水县高谷镇陈家村6组G319国道2270处发生岩质边坡垮塌。现场垮塌方量约25立方米，堆积体占压道路并造成双向交通中断。

经现场核查，本次事件未造成人员伤亡，也未发现人员被困。

事发边坡为道路人工切坡，坡体以中厚层灰岩为主，层面及裂隙较发育。调查时坡面仍分布约100立方米破碎岩体，连续降雨、施工扰动或车辆振动可能诱发二次崩塌。

事件发生后，高谷镇、县规划自然资源局、交通部门及公安交巡警先后开展信息核实、现场警戒、交通管制、专业调查和清障处置。现有材料能够支撑事件事实与专业分析，但道路恢复、危害验收和响应终止记录仍需补充。

> 📑 **复盘总体结论**  
> 本次处置总体及时，现场警戒和交通管制有效避免了次生伤亡，综合评价82.3分，等级“良好”。

# 二、处置过程回顾

## 1. 险情发现与快速核实
16时35分，陈家村网格员发现落石阻断道路并完成首报；16时42分，高谷镇值班室核对位置、道路影响及人员伤亡情况；16时48分，县应急联动专班同步派发现场警戒、交通管制和专业调查任务。

## 2. 现场管控与专业研判
16时55分，交巡警与属地人员实施双向管制；17时12分，驻地地质队到达现场，对坡体结构、垮塌规模和残余风险开展调查；17时36分形成降雨诱发的顺向岩质边坡局部崩塌判断。

## 3. 排险清障与阶段恢复
19时05分起，交通部门组织清理路面堆积体；20时20分单幅道路临时放行。恢复时间及依据为示例补全，仍需责任单位提供正式回用和验收等相关意见。

# 三、地质专业分析

事发边坡走向近东西向，坡高约28米，坡角约55°。坡体岩性为寒武系中统娄山关组白云质灰岩，呈薄至中厚层状产出。受区域构造裂隙切割及前期持续强降水入渗影响，岩体抗剪强度显著软化降低，最终导致沿节理裂隙面产生楔形滑塌失稳。

当前坡体上方尚存危岩单体约100立方米，坡脚坡面虽已临时清理，但仍处于基本欠稳定状态，需加强降雨期动态监测与专业支护加固。

# 四、综合评价

本次崩滑险情从网格员首报到现场实施双向封闭管控耗时20分钟，响应速度迅速，各联动部门协同紧密，应急处置闭环效率总体良好。

- **信息报送时效性**：优秀（首报耗时7分钟，快于规定时限）
- **现场管控果断性**：良好（交巡警与镇村人员迅速建立安全隔离区）
- **专家研判精准性**：良好（定性准确，有效指导现场清理施工）
- **档案归集完整性**：待提升（尚缺单幅放行后复测及应急终止归档凭据）

# 五、问题与改进

1. **监测预警短板**：该切坡地段此前未纳入自动化微芯桩或雨量位移联动监测，缺乏降雨阈值自动触发预警机制。
2. **多源数据协同**：调查报告中记载发生时间与系统记录存在5分钟偏差，需完善网格端与业务平台的时间戳校验机制。
3. **闭环回执跟踪**：现场清障后的边坡安全评估与响应终止流程需在系统内补充数字化回执，形成闭环管理。

# 六、复盘结论

综上所述，本次高谷镇G319国道边坡垮塌事件处置及时得当，未造成次生安全事故。建议后续将该路段纳入重点监测点位库，实施主动防护网加固工程，并在防汛期强化网格化巡查与雨前雨中雨后“三查”制度，全面筑牢地灾防线。
`.trim()

/**
 * 从 Markdown 源码中提取标题大纲（TOC）
 * @param {string} markdown
 * @returns {Array<{ id: string, title: string, level: number }>}
 */
export function extractTocFromMarkdown(markdown = '') {
  const lines = markdown.split('\n')
  const toc = []
  let chapterIndex = 0

  for (const line of lines) {
    const headingMatch = line.match(/^(#{1,3})\s+(.+)$/)
    if (headingMatch) {
      const level = headingMatch[1].length
      const rawTitle = headingMatch[2].trim()
      chapterIndex += 1
      const id = `report-heading-${chapterIndex}`
      toc.push({
        id,
        title: rawTitle,
        level,
      })
    }
  }

  return toc
}

/**
 * 解析并生成带锚点 ID 的 HTML 及目录大纲
 * @param {string} markdown
 * @returns {{ html: string, toc: Array<{ id: string, title: string, level: number }> }}
 */
export function parseReportMarkdown(markdown = '') {
  const toc = extractTocFromMarkdown(markdown)
  const md = createMd({ useBreak: true, wrapTable: true })

  let headingCounter = 0
  const originalHeadingOpen =
    md.renderer.rules.heading_open ||
    ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))

  md.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
    headingCounter += 1
    const id = `report-heading-${headingCounter}`
    tokens[idx].attrSet('id', id)
    tokens[idx].attrJoin('class', 'report-doc-heading')
    return originalHeadingOpen(tokens, idx, options, env, self)
  }

  const html = md.render(markdown)

  return {
    html,
    toc,
  }
}
