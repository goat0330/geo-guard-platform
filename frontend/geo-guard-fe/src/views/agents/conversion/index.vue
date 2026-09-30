<template>
  <section class="conversion-page">
    <AgentPageHeader title="格式转换" description="支持配置 PDF、Word、Excel 与图片格式转换" />
    <div class="conversion-content">
      <div class="intro-row">
        <div><h2>上传文件</h2><p>一次仅处理同一种类型的文件</p></div>
        <img src="@/assets/imgs/agents/con-bg.png" alt="格式转换" />
      </div>

      <div class="conversion-panel">
        <el-upload v-if="!fileList.length" v-model:file-list="fileList" class="upload-area" drag multiple :auto-upload="false" :show-file-list="false">
          <img src="@/assets/imgs/agents/upload.png" alt="上传" />
          <p><span>点击选择</span>或将文件拖拽至此处</p>
          <small>支持 PDF、Word、Excel 与常用图片格式</small>
        </el-upload>

        <Transition name="file-panel">
          <div v-if="fileList.length" class="file-config">
            <div class="file-column">
              <div class="section-title"><span>待转换文件</span><el-button text :icon="Plus" @click="uploadRef?.$el?.querySelector('input')?.click()">继续添加</el-button></div>
              <el-upload ref="uploadRef" v-model:file-list="fileList" multiple :auto-upload="false" :show-file-list="false" class="hidden-upload" />
              <ul>
                <li v-for="file in fileList" :key="file.uid">
                  <el-icon><Document /></el-icon>
                  <div><strong :title="file.name">{{ file.name }}</strong><span>{{ formatFileSize(file.size) }}</span></div>
                  <el-button text circle :icon="Delete" aria-label="删除文件" @click="removeFile(file.uid)" />
                </li>
              </ul>
            </div>
            <div class="config-column">
              <label>转换为</label>
              <el-radio-group v-model="targetFormat">
                <el-radio-button v-for="format in targetFormats" :key="format" :value="format">{{ format }}</el-radio-button>
              </el-radio-group>
              <label>图片合并</label>
              <el-segmented v-model="mergeImages" :options="mergeOptions" />
              <el-button type="primary" :disabled="!targetFormat" @click="confirmConversion">确认转换</el-button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { Delete, Document, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AgentPageHeader from '@/components/AgentPageHeader/index.vue'

defineOptions({ name: 'AgentConversionPage' })

const fileList = ref([])
const uploadRef = ref()
const targetFormat = ref('')
const mergeImages = ref(false)
const targetFormats = ['Word', 'PDF', 'Excel']
const mergeOptions = [{ label: '合并', value: true }, { label: '不合并', value: false }]

function formatFileSize(size) {
  if (!Number.isFinite(size)) return ''
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function removeFile(uid) {
  fileList.value = fileList.value.filter((file) => file.uid !== uid)
}

function confirmConversion() {
  ElMessage.warning('格式转换接口尚未配置，当前无法提交转换任务')
}
</script>

<style lang="less" scoped>
.conversion-page { flex: 1; min-height: 0; display: flex; flex-direction: column; background: #f7faff; }
.conversion-content { width: min(760px, calc(100% - 32px)); margin: 0 auto; padding: 36px 0 48px; }
.intro-row { min-height: 112px; padding: 0 20px; display: flex; align-items: center; justify-content: space-between; }
.intro-row h2 { margin: 0 0 8px; color: #222527; font-size: 24px; line-height: 32px; }
.intro-row p { margin: 0; color: #617185; font-size: 14px; }
.intro-row img { width: 156px; height: 88px; object-fit: contain; }
.conversion-panel { margin-top: 20px; padding: 24px; border-radius: 8px; background: #ffffff; box-shadow: 0 8px 28px rgba(0, 64, 140, 0.08); }
.upload-area {
  :deep(.el-upload) { width: 100%; }
  :deep(.el-upload-dragger) { width: 100%; height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 8px; background: #f8fbff; }
  img { width: 56px; height: 42px; object-fit: contain; }
  p { margin: 18px 0 6px; color: #222527; font-size: 14px; }
  p span { color: #007bff; }
  small { color: #9096a2; font-size: 12px; }
}
.file-config { display: grid; grid-template-columns: minmax(0, 1fr) 224px; gap: 24px; }
.section-title { height: 36px; display: flex; align-items: center; justify-content: space-between; color: #222527; font-size: 14px; font-weight: 600; }
.hidden-upload { display: none; }
ul { margin: 10px 0 0; padding: 12px; max-height: 286px; overflow: auto; list-style: none; border: 1px dashed #d8e0ea; border-radius: 8px; background: #f8fbff; }
li { min-height: 54px; padding: 8px; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #e8edf3; }
li:last-child { border-bottom: 0; }
li > .el-icon { color: #007bff; font-size: 24px; }
li div { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
li strong { overflow: hidden; color: #222527; font-size: 14px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
li span { color: #9096a2; font-family: 'Alimama FangYuanTi VF', sans-serif; font-size: 12px; }
.config-column { display: flex; flex-direction: column; gap: 14px; }
.config-column label { color: #617185; font-size: 12px; }
.config-column :deep(.el-radio-group) { display: grid; grid-template-columns: repeat(3, 1fr); }
.config-column :deep(.el-radio-button__inner) { width: 100%; padding: 8px 4px; }
.config-column > .el-button { margin-top: auto; }
.file-panel-enter-active,
.file-panel-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.file-panel-enter-from,
.file-panel-leave-to { opacity: 0; transform: translateY(8px); }
@media (max-width: 720px) {
  .file-config { grid-template-columns: 1fr; }
  .intro-row { padding: 0; }
  .intro-row img { width: 124px; }
}
</style>
