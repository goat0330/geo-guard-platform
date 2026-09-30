import { defineStore } from 'pinia'
import { ref } from 'vue'
import { FOLLOW_TIP, FOLLOW_TIP_FLOW_MAPPER } from '@/views/emergencyResponse/config.js'
import { nextProcess } from '@/api/deal.js'

export const useEmergencyStore = defineStore('emergency', () => {
  const handleId = ref('')
  // 流程执行位置
  const activeFlowIndex = ref(0)
  // 当前流程位置
  const currentFlowIndex = ref(0)
  // 流程执行tip
  const activeFlowTip = ref('')

  const resetStore = (initIndex = 0, initFlowTip = '') => {
    handleId.value = ''
    activeFlowIndex.value = initIndex
    currentFlowIndex.value = initIndex
    activeFlowTip.value = ''
  }

  // 下一步流程
  const nextFlow = async() => {
    if (handleId.value) {
      await nextProcess(handleId.value)
    }
  }

  // 设置流程节点
  const setFlowIndex = (tip) => {
    activeFlowIndex.value = FOLLOW_TIP_FLOW_MAPPER[tip]
    currentFlowIndex.value = FOLLOW_TIP_FLOW_MAPPER[tip]
    activeFlowTip.value = tip
  }

  return {
    handleId,
    activeFlowIndex,
    currentFlowIndex,
    activeFlowTip,
    resetStore,
    nextFlow,
    setFlowIndex
  }
},
  {
    persist: true,
  },
)
