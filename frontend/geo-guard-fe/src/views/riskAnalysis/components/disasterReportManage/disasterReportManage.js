import { ElMessage } from 'element-plus'
import { eventBus, EventKey } from '@/utils/eventBus.js'
import { Components } from '@/components/ChatBox/config.js'
import { createLoading } from '@/utils/index.js'
import { handleDisReport, closeDisReport } from '@/api/reportDisaster.js'
import { REPORT_STATUS } from '@/utils/enum.js'

// 正在处理的报灾记录，防止同一记录被多个组件实例并发提交。
const processingReportIds = new Set()

export async function closeReportAction(selectedRow, refresh) {
  await closeDisReport(selectedRow.id)
  ElMessage.success('关闭成功')
  refresh()
  selectedRow.status = REPORT_STATUS.DONE
}

export async function handleReport(type, selectedRow, refresh) {
  const reportId = selectedRow?.id
  if (!reportId || processingReportIds.has(reportId)) return

  processingReportIds.add(reportId)
  let message = ''
  if (type === 'up') {
    message = `收到！已通过系统通知和短信渠道为您报送【${selectedRow?.checkCenterLocation || '--'}】的异常迹象核查任务给${selectedRow?.street || '--'}自规所${selectedRow?.dispatchTargetName || '--'}。`
  } else {
    message = `收到！已在「调度模式」看板【巡查任务】模块新建 1 个专项任务，任务信息已完整录入，具体内容可在对应卡片查看。 同步通过APP和短信渠道为您下发【${selectedRow?.checkCenterLocation}】的异常迹象核查任务给巡查员${selectedRow?.dispatchTargetName || '--'}。`
  }
  const loading = createLoading({
    lock: true,
    text: `${type === 'up' ? '上报' : '下发'}中`,
    background: 'rgba(0, 0, 0, 0.7)',
  })
  try {
    await handleDisReport(reportId)
    eventBus.emit(EventKey.ADD_COMPONENT, {
      comp: Components.BUBBLE,
      props: {
        type: 1,
        message: {
          content: message,
        },
      },
    })
    ElMessage.success('操作成功')
    refresh()
    selectedRow.status = REPORT_STATUS.DONE
  } finally {
    processingReportIds.delete(reportId)
    loading.close()
  }
}
