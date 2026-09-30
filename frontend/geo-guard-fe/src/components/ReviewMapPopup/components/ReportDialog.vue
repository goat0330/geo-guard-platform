<template>
  <el-dialog
    v-model="dialogVisible"
    class="review-report-dialog"
    title="现场调查报告"
    width="760px"
    top="8vh"
    append-to-body
    destroy-on-close
    @close="handleClose"
  >
    <div v-loading="loading" class="report-content-wrap">
      <template v-if="reports.length > 0">
        <!-- 多轮调查报告切换标签 -->
        <div v-if="reports.length > 1" class="report-tabs">
          <button
            v-for="(rep, idx) in reports"
            :key="rep.id || rep.fileId || idx"
            type="button"
            class="report-tab"
            :class="{ 'is-active': activeIndex === idx }"
            @click="activeIndex = idx"
          >
            第 {{ rep.roundNo || idx + 1 }} 轮报告
          </button>
        </div>

        <!-- 调查报告展示卡片 -->
        <ReportCard
          v-if="currentReport"
          :report="currentReport"
          :downloading="downloading"
          :previewing="previewing"
          @download="downloadReport"
          @preview="previewReport"
        />
      </template>

      <el-empty
        v-else-if="!loading"
        :image-size="72"
        description="当前事件暂未关联调查报告"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button
          v-if="reports.length > 1"
          :loading="downloadingAll"
          @click="downloadAllReports(reports)"
        >
          全部下载 ({{ reports.length }})
        </el-button>
        <el-button
          type="primary"
          :icon="Download"
          :disabled="!currentReport"
          :loading="downloading"
          @click="downloadReport(currentReport)"
        >
          下载当前报告
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { getReviewEventDetail } from '@/api/review.js'
import { useReportDownload } from '@/composables/useReportDownload.js'
import ReportCard from './ReportCard.vue'

defineOptions({ name: 'ReportDialog' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  eventId: { type: [String, Number], default: '' },
  eventTitle: { type: String, default: '' },
  initialReports: { type: Array, default: null },
})

const emit = defineEmits(['update:visible', 'close'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const loading = ref(false)
const reports = ref([])
const activeIndex = ref(0)

const {
  downloading,
  downloadingAll,
  previewing,
  downloadReport,
  previewReport,
  downloadAllReports,
} = useReportDownload()

const currentReport = computed(() => reports.value[activeIndex.value] || null)

/**
 * 加载当前事件关联的调查报告
 */
const loadReports = async () => {
  if (props.initialReports && props.initialReports.length > 0) {
    reports.value = props.initialReports
    activeIndex.value = 0
    return
  }

  if (!props.eventId) return
  loading.value = true
  activeIndex.value = 0

  try {
    const res = await getReviewEventDetail(props.eventId)
    const detail = res?.data || res || {}
    const list =
      detail?.materialInfo?.investigationReports ||
      detail?.investigationReports ||
      (detail?.materialInfo?.attachments || detail?.attachments || []).filter(
        (a) => a.attachmentType === 2 || a.category === 'INVESTIGATION_REPORT',
      ) ||
      []
    reports.value = list
  } catch (error) {
    console.error('获取调查报告失败', error)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      void loadReports()
    } else {
      reports.value = []
    }
  },
)

const handleClose = () => {
  emit('close')
}
</script>

<style lang="less" scoped>
.review-report-dialog {
  :deep(.el-dialog__header) {
    margin-right: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #edf0f4;
  }

  :deep(.el-dialog__title) {
    color: #222527;
    font-size: 16px;
    font-weight: 700;
    line-height: 24px;
  }

  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }
}

.report-content-wrap {
  min-height: 280px;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 4px;
}

.report-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.report-tab {
  padding: 4px 14px;
  border: 1px solid #e4eaef;
  border-radius: 4px;
  background: #f8fafc;
  color: #617185;
  font-family: inherit;
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #007bff;
    color: #007bff;
  }

  &.is-active {
    border-color: #007bff;
    background: #007bff;
    color: #ffffff;
  }
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}
</style>
