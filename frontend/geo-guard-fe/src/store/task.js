import { defineStore } from 'pinia'
import { ref } from 'vue'
import { closeAutoTask, getAutoTaskStatus, openAutoTask } from '@/api/task.js'

export const useTaskStore = defineStore('task', () => {
  const autoFlow = ref(false)

  const getCurrentAiFlowStatus = async() => {
    const res = await getAutoTaskStatus()
    autoFlow.value = res.enabled
    return autoFlow.value
  }

  const setAiFlowStatus = async(enabled) => {
    if (enabled) {
      await openAutoTask()
    } else {
      await closeAutoTask()
    }
    await getCurrentAiFlowStatus()
  }

  const changeFlow = async() => {
    await getCurrentAiFlowStatus()
    if (autoFlow.value) {
      await closeAutoTask()
    } else {
      await openAutoTask()
    }
    await getCurrentAiFlowStatus()
  }

  return {
    autoFlow,
    getCurrentAiFlowStatus,
    setAiFlowStatus,
    changeFlow
  }
})
