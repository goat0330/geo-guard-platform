import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const messageList = ref([])
  const businessId = ref(0)
  const conversationId = ref('')
  const showHistory = ref(false)
  const showNew = ref(true)
  const deepThink = ref(false)
  const queryInputs = ref({
    // 处置id
    handleId: ''
  })
  // 是否可以上传文件
  const useParseFile = ref(false)
  // 上传文件列表
  const uploadFiles = ref([])
  const pushTaskQuery = ref({})
  // 专家会商右侧processIndex
  const chatProcessIndex = ref(null)

  return {
    messageList,
    businessId,
    conversationId,
    queryInputs,
    showHistory,
    showNew,
    deepThink,
    pushTaskQuery,
    chatProcessIndex,
    uploadFiles,
    useParseFile
  }
})
