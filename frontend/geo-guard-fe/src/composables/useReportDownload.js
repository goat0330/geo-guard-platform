import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { downloadFile, getImageUrlById } from '@/api/common.js'

/**
 * 现场调查报告下载及在线预览 composable
 */
export function useReportDownload() {
  const downloading = ref(false)
  const downloadingAll = ref(false)
  const previewing = ref(false)

  /**
   * 获取报告文件名（含安全后缀）
   */
  const getFilename = (report) => {
    if (!report) return '现场调查报告.pdf'
    let name = report.fileName || report.title || report.name || '现场调查报告'
    if (!name.includes('.') && report.fileExtension) {
      name = `${name}.${report.fileExtension}`
    } else if (!name.includes('.')) {
      name = `${name}.pdf`
    }
    return name
  }

  /**
   * 获取文件后缀（大写）
   */
  const getFileExt = (report) => {
    if (!report) return 'PDF'
    if (report.fileExtension) return String(report.fileExtension).toUpperCase()
    const name = report.fileName || report.title || ''
    const parts = name.split('.')
    return parts.length > 1 ? parts.pop().toUpperCase() : 'PDF'
  }

  /**
   * 格式化文件大小
   */
  const formatFileSize = (bytes) => {
    if (!bytes || isNaN(bytes)) return ''
    const b = Number(bytes)
    if (b < 1024) return `${b} B`
    if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
    return `${(b / (1024 * 1024)).toFixed(2)} MB`
  }

  /**
   * 下载单个调查报告文件
   * @param {object} report - 调查报告附件数据
   */
  const downloadReport = async (report) => {
    if (!report) return
    downloading.value = true
    const filename = getFilename(report)

    try {
      const ossId = report.ossId || (/^\d+$/.test(String(report.fileId)) ? report.fileId : null)
      let downloaded = false

      // 1. 优先调用系统统一 OSS 下载接口获取二进制 Blob
      if (ossId) {
        try {
          const res = await downloadFile(ossId)
          if (res instanceof Blob && res.size > 0) {
            // 容错检查：后端是否将错误 JSON 包装成了 Blob
            if (res.type && res.type.includes('application/json')) {
              const text = await res.text()
              try {
                const json = JSON.parse(text)
                if (json && (json.code !== 200 || json.status >= 400)) {
                  throw new Error(json.msg || '文件下载接口返回错误')
                }
              } catch (jsonErr) {
                if (jsonErr.message !== 'Unexpected end of JSON input') {
                  throw jsonErr
                }
              }
            }
            saveAs(res, filename)
            ElMessage.success('下载成功')
            downloaded = true
          }
        } catch (ossErr) {
          console.warn('OSS 二进制下载接口异常，尝试回退备用下载方式:', ossErr)
        }
      }

      if (downloaded) return

      // 2. 尝试使用直链或通过 apply-url 换取临时直链
      let targetUrl = report.fileUrl || report.url || report.externalUrl
      if (!targetUrl && (ossId || report.fileId)) {
        try {
          const urlList = await getImageUrlById({ ids: [ossId || report.fileId] })
          if (Array.isArray(urlList) && urlList[0]) {
            targetUrl = urlList[0]
          }
        } catch (urlErr) {
          console.warn('换取临时下载链接失败:', urlErr)
        }
      }

      if (targetUrl) {
        try {
          const resp = await fetch(targetUrl)
          if (resp.ok) {
            const blob = await resp.blob()
            saveAs(blob, filename)
            ElMessage.success('下载成功')
            return
          }
        } catch {
          // fetch 跨域受阻时通过 a 标签原生下载
          const a = document.createElement('a')
          a.href = targetUrl
          a.download = filename
          a.target = '_blank'
          document.body.appendChild(a)
          a.click()
          document.body.removeChild(a)
          ElMessage.success('已发起下载')
          return
        }
      }

      // 3. 若无二进制文件但有结构化文本，生成文本文件容错下载
      if (report.content || report.description) {
        const textBlob = new Blob([report.content || report.description], {
          type: 'text/plain;charset=utf-8',
        })
        const textFilename = filename.endsWith('.pdf') ? filename.replace(/\.pdf$/, '.txt') : filename
        saveAs(textBlob, textFilename)
        ElMessage.success('下载成功')
        return
      }

      ElMessage.warning('未获取到可供下载的调查报告文件')
    } catch (error) {
      console.error('下载调查报告失败:', error)
      ElMessage.error(error.message || '下载调查报告失败，请稍后重试')
    } finally {
      downloading.value = false
    }
  }

  /**
   * 在线预览调查报告
   * @param {object} report - 调查报告附件数据
   */
  const previewReport = async (report) => {
    if (!report) return
    previewing.value = true

    try {
      let directUrl = report.fileUrl || report.url || report.externalUrl
      const ossId = report.ossId || (/^\d+$/.test(String(report.fileId)) ? report.fileId : null)
      if (!directUrl && (ossId || report.fileId)) {
        const urlList = await getImageUrlById({ ids: [ossId || report.fileId] })
        if (Array.isArray(urlList) && urlList[0]) {
          directUrl = urlList[0]
        }
      }

      if (directUrl) {
        window.open(directUrl, '_blank')
      } else {
        ElMessage.info('该调查报告暂无在线预览地址，请点击下载查看')
      }
    } catch (err) {
      console.warn('获取预览地址失败:', err)
      ElMessage.error('获取预览地址失败，请直接下载查看')
    } finally {
      previewing.value = false
    }
  }

  /**
   * 批量下载所有轮次报告
   * @param {Array} reportList
   */
  const downloadAllReports = async (reportList) => {
    if (!Array.isArray(reportList) || reportList.length === 0) return
    downloadingAll.value = true

    try {
      for (let i = 0; i < reportList.length; i += 1) {
        await downloadReport(reportList[i])
        if (i < reportList.length - 1) {
          await new Promise((resolve) => setTimeout(resolve, 600))
        }
      }
    } finally {
      downloadingAll.value = false
    }
  }

  return {
    downloading,
    downloadingAll,
    previewing,
    getFilename,
    getFileExt,
    formatFileSize,
    downloadReport,
    previewReport,
    downloadAllReports,
  }
}
