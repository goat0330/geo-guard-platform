<template>
  <section class="writing-page">
    <AgentPageHeader title="公文撰写" description="专业级公文写作助手，确保内容准确、格式规范" />

    <div class="writing-content">
      <Transition name="content-switch" mode="out-in">
        <div v-if="!selectedType" key="types" class="type-panel">
          <img src="@/assets/imgs/agents/writter.png" alt="公文撰写" class="writer-avatar" />
          <h2>选择公文类型</h2>
          <p>根据使用场景填写关键信息，生成任务将交由当前平台智能问答完成。</p>
          <div class="type-grid">
            <button v-for="item in documentTypes" :key="item.value" type="button" @click="selectType(item.value)">
              <span class="type-icon" :style="{ color: item.color, background: item.background }">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </button>
          </div>
        </div>

        <div v-else key="form" class="form-panel">
          <div class="form-title-row">
            <div>
              <h2>新建{{ selectedTypeLabel }}</h2>
              <p>请填写公文生成所需信息</p>
            </div>
            <el-button text :icon="RefreshLeft" @click="selectedType = ''">重新选择</el-button>
          </div>

          <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
            <el-form-item label="标题" prop="title">
              <el-input v-model.trim="formData.title" placeholder="请输入标题" />
            </el-form-item>
            <el-form-item label="公文号" prop="documentNumber">
              <el-input v-model.trim="formData.documentNumber" placeholder="请输入公文号" />
            </el-form-item>
            <el-form-item label="主题" prop="summary">
              <el-input v-model.trim="formData.summary" type="textarea" :rows="5" placeholder="请输入主题和关键要求" />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="发布时间" prop="publishDate">
                <el-date-picker v-model="formData.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择发布时间" />
              </el-form-item>
              <el-form-item label="署名单位" prop="publisher">
                <el-input v-model.trim="formData.publisher" placeholder="请输入单位名称" />
              </el-form-item>
            </div>
            <div class="form-actions">
              <el-button @click="resetForm">重置</el-button>
              <el-button type="primary" :icon="Promotion" @click="generateDocument">生成公文</el-button>
            </div>
          </el-form>
        </div>
      </Transition>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ChatLineSquare, Document, Promotion, RefreshLeft, Tickets } from '@element-plus/icons-vue'
import AgentPageHeader from '@/components/AgentPageHeader/index.vue'
import { useUserStore } from '@/store/user.js'

defineOptions({ name: 'AgentWritingPage' })

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const selectedType = ref('')

const documentTypes = [
  { value: '通知', label: '撰写通知', description: '发布需要相关单位周知或执行的事项。', icon: Bell, color: '#007BFF', background: '#DCEDFF' },
  { value: '报告', label: '撰写报告', description: '向上级汇报工作、反映情况或回复询问。', icon: Document, color: '#27A59D', background: '#E8F7F5' },
  { value: '函', label: '撰写函', description: '用于单位间商洽、询问、请求或答复事项。', icon: ChatLineSquare, color: '#FF922C', background: '#FFF4E8' },
  { value: '请示', label: '撰写请示', description: '向上级单位请求指示或批准。', icon: Tickets, color: '#666FD8', background: '#F0F0FF' },
]

const selectedTypeLabel = computed(() => selectedType.value)
const formData = reactive({
  title: '',
  documentNumber: '',
  summary: '',
  publishDate: '',
  publisher: '',
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  documentNumber: [{ required: true, message: '请输入公文号', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入主题', trigger: 'blur' }],
  publishDate: [{ required: true, message: '请选择发布时间', trigger: 'change' }],
  publisher: [{ required: true, message: '请输入署名单位', trigger: 'blur' }],
}

function selectType(type) {
  selectedType.value = type
}

function resetForm() {
  formRef.value?.resetFields()
}

async function generateDocument() {
  await formRef.value.validate()
  const question = [
    `请根据以下信息撰写一份规范的${selectedType.value}：`,
    `标题：${formData.title}`,
    `公文号：${formData.documentNumber}`,
    `主题：${formData.summary}`,
    `发布时间：${formData.publishDate}`,
    `署名单位：${formData.publisher}`,
  ].join('\n')
  userStore.saveUserQuestion(question)
  router.push({ path: '/chat-engine/chatting', query: { _refresh: Date.now() } })
}
</script>

<style lang="less" scoped>
.writing-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f6fbff 0, #ffffff 64%);
}

.writing-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 36px 20px 48px;
}

.type-panel,
.form-panel {
  width: min(620px, 100%);
  margin: 0 auto;
}

.type-panel {
  text-align: center;

  h2 {
    margin: 16px 0 6px;
    color: #222527;
    font-size: 20px;
    line-height: 28px;
  }

  > p {
    margin: 0;
    color: #617185;
    font-size: 14px;
    line-height: 22px;
  }
}

.writer-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
}

.type-grid {
  margin-top: 32px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;

  button {
    min-height: 148px;
    padding: 18px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    border: 1px solid #e1e6ee;
    border-radius: 8px;
    color: #222527;
    background: #ffffff;
    text-align: left;
    cursor: pointer;
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      border-color: #9bc8ff;
      box-shadow: 0 8px 20px rgba(0, 64, 140, 0.08);
    }

    strong { margin: 14px 0 6px; font-size: 16px; line-height: 22px; }
    > span:last-child { color: #9096a2; font-size: 12px; line-height: 20px; }
  }
}

.type-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  font-size: 20px;
}

.form-panel {
  padding: 28px 32px 32px;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 28px rgba(0, 64, 140, 0.06);
}

.form-title-row {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;

  h2 { margin: 0; color: #222527; font-size: 20px; line-height: 28px; }
  p { margin: 4px 0 0; color: #9096a2; font-size: 12px; line-height: 18px; }
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  :deep(.el-date-editor) { width: 100%; }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.content-switch-enter-active,
.content-switch-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.content-switch-enter-from { opacity: 0; transform: translateY(8px); }
.content-switch-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 620px) {
  .type-grid,
  .form-row { grid-template-columns: 1fr; }
  .form-panel { padding: 20px 16px 24px; }
}
</style>
