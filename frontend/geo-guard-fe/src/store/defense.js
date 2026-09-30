import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDefenseStore = defineStore('defense', () => {
  const defId = ref('')
  const handleId = ref('')
  const defObj = ref(null)

  const resetStore = () => {
    handleId.value = ''
    defId.value = ''
    defObj.value = null
  }

  return {
    defId,
    handleId,
    defObj,
    resetStore
  }
})
