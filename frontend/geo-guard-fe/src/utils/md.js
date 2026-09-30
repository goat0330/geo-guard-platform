import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
// import markdownItKatex from 'markdown-it-katex'
import markdownItKatex from '@traptitech/markdown-it-katex'
import anchor from 'markdown-it-anchor'
import { generateRandomString } from '@/utils/index.js'

/**
 * 修正 Mermaid 中与普通节点重名的子图 ID，避免渲染失败。
 * @param {string} code - Mermaid 源码。
 * @returns {string} 可安全渲染的 Mermaid 源码。
 */
const normalizeMermaidSubgraphIds = (code = '') => {
  // 子图声明不参与普通节点 ID 的收集，避免将自身误判为冲突节点。
  const codeWithoutSubgraphs = code.replace(/^\s*subgraph\s+[^\n]+$/gm, '')
  const nodeIds = new Set(
    [...codeWithoutSubgraphs.matchAll(/\b([A-Za-z][\w-]*)\s*(?:\[|\{|\()/g)].map(([, id]) => id),
  )

  return code.replace(/^(\s*subgraph\s+)([A-Za-z][\w-]*)(\s*\[)/gm, (match, prefix, id, suffix) => {
    return nodeIds.has(id) ? `${prefix}${id}_GROUP${suffix}` : match
  })
}

export const createMd = ({ useBreak = false, wrapTable = false } = {}) => {
  const md = MarkdownIt({
    html: true,
    linkify: true,
    breaks: useBreak,
    // typographer: true,
    highlight: function (str, lang) {
      if (lang === 'mermaid') {
        // 这里不要高亮 mermaid，而是交给 fence 渲染器处理
        // 所以实际上 highlight 不会收到 'mermaid'（因为 fence 优先）
        // 但为了安全兜底，也可以返回原始内容
        return '<pre><code>' + md.utils.escapeHtml(str) + '</code></pre>'
      }
      if (lang && hljs.getLanguage(lang)) {
        try {
          return (
            '<pre class="code-box"><code class="hljs">' +
            hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
            '</code></pre>'
          )
        } catch (_) {
          // 高亮失败时走下方普通代码块兜底。
        }
      }

      return '<pre><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>'
    },
  })

  md.use(markdownItKatex, {
    // 可选：自定义 KaTeX 配置
    errorColor: '#cc0000',
    enableMathInlineInHtml: true,
    enableInlineMath: true,
  })

  // 渲染器重写，处理 mermaid 代码块
  const defaultFenceRenderer = md.renderer.rules.fence
  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const lang = token.info ? token.info.trim() : ''

    // 如果是 mermaid 代码块，特殊处理
    if (lang === 'mermaid') {
      const code = normalizeMermaidSubgraphIds(token.content.trim())
      // 输出给前端 Mermaid 渲染的容器
      return `<div class="mermaid">${md.utils.escapeHtml(code)}</div>`
    }

    // 其他语言走默认逻辑（会触发 highlight）
    if (defaultFenceRenderer) {
      return defaultFenceRenderer(tokens, idx, options, env, self)
    }
    // fallback
    return self.renderToken(tokens, idx, options)
  }

  if (wrapTable) {
    // 让聊天中的宽表格拥有独立滚动区域，避免撑开整个消息气泡。
    const defaultTableOpenRenderer = md.renderer.rules.table_open || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))
    const defaultTableCloseRenderer = md.renderer.rules.table_close || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))
    md.renderer.rules.table_open = (tokens, idx, options, env, self) => {
      return `<div class="markdown-table-wrapper">${defaultTableOpenRenderer(tokens, idx, options, env, self)}`
    }
    md.renderer.rules.table_close = (tokens, idx, options, env, self) => {
      return `${defaultTableCloseRenderer(tokens, idx, options, env, self)}</div>`
    }
  }

  const defaultLinkOpenRenderer =
    md.renderer.rules.link_open ||
    function (tokens, idx, options, env, self) {
      return self.renderToken(tokens, idx, options)
    }
  md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
    // 查找 target 属性的索引
    const aIndex = tokens[idx].attrIndex('target')

    if (aIndex < 0) {
      // 如果没有 target 属性，则添加
      tokens[idx].attrPush(['target', '_blank'])
    } else {
      // 如果已有 target 属性，则强制改为 _blank
      tokens[idx].attrs[aIndex][1] = '_blank'
    }

    // 建议出于安全考虑同时添加 rel="noopener noreferrer"
    const relIndex = tokens[idx].attrIndex('rel')
    if (relIndex < 0) {
      tokens[idx].attrPush(['rel', 'noopener noreferrer'])
    }

    // 调用默认渲染器
    return defaultLinkOpenRenderer(tokens, idx, options, env, self)
  }

  return md
}

export const createMdWithImg = () => {
  const md = createMd({ useBreak: true })
  // 重写图片渲染规则
  const defaultRender =
    md.renderer.rules.image ||
    function (tokens, idx, options, env, self) {
      return self.renderToken(tokens, idx, options)
    }
  md.renderer.rules.image = (tokens, idx, options, env, self) => {
    const token = tokens[idx]

    // 设置样式（统一控制大小）
    token.attrSet('style', 'width:300px;height:auto;')
    // 加属性
    token.attrSet('width', '300')

    // 你也可以按条件控制
    // token.attrSet('style', src.includes('xxx') ? 'width:400px' : 'width:200px')

    return defaultRender(tokens, idx, options, env, self)
  }
  return md
}

export const insertPicture = (md = '', { disasterImage = [], deformationPhoto = [], damagePhoto = [], remoteSensingPhoto = [] } = {}) => {
  if (!md || typeof md !== 'string') return ''

  const genImgStr = (list = []) => {
    if (!Array.isArray(list) || list.length === 0) return ''

    // 只有一张 → 不加序号
    if (list.length === 1) {
      const url = list[0]?.url || list[0]
      return `![图片](${url})`
    }

    // 多张 → 加序号
    return list
      .filter((item) => item)
      .map((item, index) => {
        const url = item?.url || item
        return `![图片${index + 1}](${url})`
      })
      .join('\n')
  }

  return md
    .replace(/\{disaster_image\}/g, genImgStr(disasterImage))
    .replace(/\{deformation_photo\}/g, genImgStr(deformationPhoto))
    .replace(/\{damage_photo\}/g, genImgStr(damagePhoto))
    .replace(/\{remote_sensing_image\}/g, genImgStr(remoteSensingPhoto))
}

/**
 * 将 Markdown 文本中的图片标记替换为指定的占位符标记
 * 
 * @param {string} md - 原始 Markdown 字符串
 * @param {Object} options - 配置选项
 * @param {Array<Object|string>} options.disasterImage - 灾害图片列表，用于替换 {disaster_image} 占位符对应的图片
 * @param {Array<Object|string>} options.deformationPhoto - 形变照片列表，用于替换 {deformation_photo} 占位符对应的图片
 * @returns {string} 替换后的 Markdown 字符串，图片被还原为占位符标记
 */
export const restorePicture = (md = '', { disasterImage = [], deformationPhoto = [], damagePhoto = [] } = {}) => {
  if (!md || typeof md !== 'string') return ''

  /**
   * 生成用于匹配图片的正则表达式模式
   * @param {Array} list - 图片列表
   * @returns {Array<string>} 匹配模式数组
   */
  const genImgPatterns = (list = []) => {
    if (!Array.isArray(list) || list.length === 0) return []

    return list
      .filter((item) => item)
      .map((item, index) => {
        const url = item?.url || item
        // 转义 URL 中的特殊字符以构建正则
        const escapedUrl = url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        
        // 单张图片匹配: ![图片](url) 或 ![图片1](url) (如果原逻辑是多张加序号，这里需要兼容)
        // 考虑到 insertPicture 中单张是 ![图片](url)，多张是 ![图片N](url)
        // 我们需要匹配这两种情况
        if (list.length === 1) {
          return `\\!\\[图片\\]\\(${escapedUrl}\\)`
        } else {
          return `\\!\\[图片${index + 1}\\]\\(${escapedUrl}\\)`
        }
      })
  }

  let result = md

  // 替换灾害图片
  const disasterPatterns = genImgPatterns(disasterImage)
  if (disasterPatterns.length > 0) {
    // 构建组合正则，匹配任意一个图片标记
    const regex = new RegExp(disasterPatterns.join('|'), 'g')
    result = result.replace(regex, '{disaster_image}')
  }

  // 替换形变照片
  const deformationPatterns = genImgPatterns(deformationPhoto)
  if (deformationPatterns.length > 0) {
    const regex = new RegExp(deformationPatterns.join('|'), 'g')
    result = result.replace(regex, '{deformation_photo}')
  }

  // 替换损伤照片
  const damagePatterns = genImgPatterns(damagePhoto)
  if (damagePatterns.length > 0) {
    const regex = new RegExp(damagePatterns.join('|'), 'g')
    result = result.replace(regex, '{damage_photo}')
  }

  return result
}

// 对html 过滤p标签内部的br
export function brToNewlineInP(html) {
  return html?.replace(/<p>([\s\S]*?)<\/p>/g, (match, inner) => {
    return `<p>${inner.replace(/<br\s*\/?>/gi, '')}</p>`
  }) || ''
}

const slugify = (str) =>
  str
    .trim()
    .toLowerCase()
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')

export const createTopPicMd = () => {
  const md = createMd({ useBreak: true })

  md.use(anchor, {
    slugify,
    permalink: false
  })

  md.core.ruler.push('extract_toc', (state) => {
    const tocList = []
    const tokens = state.tokens

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i]

      if (token.type === 'heading_open') {
        const level = Number(token.tag.slice(1))
        const content = tokens[i + 1].content

        const baseId = generateRandomString(20, false)

        let id = baseId
        let index = 1

        while (tocList.find(t => t.id === id)) {
          id = `${baseId}-${index++}`
        }

        token.attrSet('id', id)

        tocList.push({
          id,
          title: content,
          level
        })
      }
    }

    state.env.tocList = tocList
  })

  return {
    md
  }
}

export function tocToTree(list) {
  const result = []
  const stack = []

  for (const item of list) {
    const node = {
      id: item.id,
      title: item.title,
      level: item.level,
      children: []
    }

    // 找到当前节点应该挂载的位置
    while (
      stack.length &&
      stack[stack.length - 1].level >= node.level
      ) {
      stack.pop()
    }

    if (stack.length === 0) {
      result.push(node)
    } else {
      const parent = stack[stack.length - 1]
      parent.children.push(node)
    }

    stack.push(node)
  }

  return result
}
