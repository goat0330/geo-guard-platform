import { ref } from 'vue'

export function useReportFlow() {
  // 定时器映射，便于统一清理防泄漏
  const timers = new Map()
  let executionVersion = 0

  // 步骤 1：已完成识别事件与复盘范围
  const step1Loading = ref(true)
  const step1Open = ref(true)
  const step1Visible = ref(false)

  // 步骤 2：已完成汇集并核验复盘资料
  const step2Loading = ref(false)
  const step2Open = ref(true)
  const step2Visible = ref(false)

  // 步骤 3：已汇聚变化依据
  const step3Visible = ref(false)
  const step3Open = ref(true)

  // 底部综合高亮总结
  const summaryVisible = ref(false)

  // 是否已加载完成并展示报告 Markdown 文档正文（3s 后完成）
  const isDocumentReady = ref(false)

  const clearTimers = () => {
    timers.forEach((resolve, timer) => {
      window.clearTimeout(timer)
      resolve(false)
    })
    timers.clear()
  }

  const wait = (duration) =>
    new Promise((resolve) => {
      const timer = window.setTimeout(() => {
        timers.delete(timer)
        resolve(true)
      }, duration)
      timers.set(timer, resolve)
    })

  // 模拟智能体渐进式推理与假 Loading 动效流
  const startFlow = async () => {
    executionVersion += 1
    const currentVersion = executionVersion
    clearTimers()

    // 初始重置
    isDocumentReady.value = false
    step1Visible.value = true
    step1Loading.value = true
    step1Open.value = true

    step2Visible.value = false
    step2Loading.value = false
    step2Open.value = true

    step3Visible.value = false
    step3Open.value = true

    summaryVisible.value = false

    // 步骤 1 模拟执行 420ms
    const step1Done = await wait(420)
    if (!step1Done || currentVersion !== executionVersion) return
    step1Loading.value = false

    // 开启步骤 2，模拟执行 520ms
    step2Visible.value = true
    step2Loading.value = true
    const step2Done = await wait(520)
    if (!step2Done || currentVersion !== executionVersion) return
    step2Loading.value = false

    // 开启步骤 3
    const step3Done = await wait(300)
    if (!step3Done || currentVersion !== executionVersion) return
    step3Visible.value = true

    // 呈现底部总结
    const summaryDone = await wait(260)
    if (!summaryDone || currentVersion !== executionVersion) return
    summaryVisible.value = true

    // 智能体完成推理 3s 倒计时后自动切换到报告文档正文视图
    const documentDone = await wait(1500)
    if (!documentDone || currentVersion !== executionVersion) return
    isDocumentReady.value = true
  }

  const toggleStep = (stepNumber) => {
    if (stepNumber === 1 && !step1Loading.value) {
      step1Open.value = !step1Open.value
    } else if (stepNumber === 2 && !step2Loading.value) {
      step2Open.value = !step2Open.value
    } else if (stepNumber === 3) {
      step3Open.value = !step3Open.value
    }
  }

  const destroyFlow = () => {
    executionVersion += 1
    clearTimers()
  }

  return {
    step1Loading,
    step1Open,
    step1Visible,
    step2Loading,
    step2Open,
    step2Visible,
    step3Open,
    step3Visible,
    summaryVisible,
    isDocumentReady,
    startFlow,
    toggleStep,
    destroyFlow,
  }
}
