<template>
  <el-dialog v-model="dialogVisible" title="用户协议及免责声明" width="800" :show-close="false">
    <div ref="scrollContainer" class="dialog-content" @scroll="handleScroll">
      <div class="each-graph">{{ texts.text0 }}</div>
      <div>
        <h3>{{ texts.text1.title }}</h3>
        <div v-for="(t, key) in texts.text1.list" :key="'text1-' + key" class="each-detail">{{ t }}</div>
      </div>
      <div>
        <h3>{{ texts.text2.title }}</h3>
        <div v-for="(t, key) in texts.text2.list" :key="'text2-' + key" class="each-detail">{{ t }}</div>
      </div>
      <div ref="contentBottom"></div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button :disabled="!isScrollToBottom" @click="agreedDisclaimer">已阅读并同意</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { onBeforeMount, onMounted, ref } from 'vue'
import { useLocalStorage } from '@vueuse/core'

const dialogVisible = ref(true)
const scrollContainer = ref(null)
const contentBottom = ref(null) // 底部锚点
const isScrollToBottom = ref(false)
const disclaimerRead = useLocalStorage('disclaimerRead', false)

const emits = defineEmits(['agreed-hook'])

const texts = ref({
  text0: ` 欢迎使用地质灾害防治智能体（以下简称“XX”），请在使用前仔细阅读并理解本协议。如您不同意本协议及其条款，请停止使用XX。继续使用则意味着您已理解并接受本协议。用户在本协议中称为“您”。您确认，代表您使用XX的用户已获得充分授权，并有权代表您接受并履行本协议。您明白并同意，XX仅向年满18周岁且具备完全民事行为能力的自然人授权。`,
  text1: {
    title: '1.服务使用和信息内容规范',
    list: [
      `1.1 在使用XX时，您应确保所输入内容拥有知识产权或已获合法授权，不违反法律法规，不侵犯他人权益（包括知识产权及其他权利），并遵守社会公德。请在输入前检查内容，避免敏感或机密信息。`,
      `1.2 您承诺在使用XX时遵守法律法规，不进行违法或违反协议的活动，不发布虚假或不当信息。违反规定的内容将被处理，包括但不限于警告、功能限制、账号封禁等措施。`,
      `1.3 XX提供的所有输出均由AI（人工智能）答复，可能出现错误或遗漏，输出内容为地质灾害防治相关参考建议，仅作辅助决策使用，不作为灾害处置、风险判定、工程实施的唯一法定专业依据。您根据输出的内容所作出的任何判断或者据此作出的后续相关操作行为，所带来的后果和责任均由您自行承担，包括因对输出的内容的真实性、准确性、可靠性、不侵权或满足特定目的的依赖而产生的风险。您应科学理性认识和依法使用生成式人工智能技术。`,
      `1.4 如果您对外发布或传播XX生成的输出，您应当：（1）主动核查输出内容的真实性、准确性，避免传播虚假信息；（2）以显著方式标明该输出内容系由人工智能生成，以向公众提示内容合成的情况；（3）避免发布和传播任何违反本协议使用规范的输出内容。`,
      `1.5 您应确保所有授权用户遵守本协议。`,
    ],
  },
  text2: {
    title: '2.有限责任',
    list: [
      `2.1 XX按现有技术和条件提供服务，尽力提供连贯、安全的服务，但不保证服务的可用性、可靠性或连续性。`,
      `2.2 为提升服务，XX可能不定期更新、维护或暂停服务，无需为此承担责任。`,
      `2.3 您应确保输入内容的授权合法，如引起纠纷或损失，由您自行承担，若给系统造成损失，您应赔偿。`,
      `2.4 若您对输出内容有异议，请及时反馈。系统将重视并处理，但不承担因您传播敏感或歧义内容而引起的侵权、纠纷或损失。`,
      `2.5 系统依法处理违法内容，但不保证及时发现并处理，您应负责输入和输出内容的合法性。`,
      `2.6 对于非故意或重大过失导致的数据问题，系统免责。`,
      `2.7 系统不承担任何间接性、后果性、惩戒性、偶然性或特殊性损害责任。`,
      `2.8 使用XX应遵守相关生成式人工智能服务管理的法律法规。`,
    ],
  },
})

// 滚动监听
const handleScroll = () => {
  const container = scrollContainer.value
  const bottomEl = contentBottom.value

  if (!container || !bottomEl) return

  // 获取容器底部位置
  const containerRect = container.getBoundingClientRect()
  // 获取锚点底部位置
  const bottomRect = bottomEl.getBoundingClientRect()

  // 核心判断：锚点进入可视区域 + 滚动贴近底部（允许 5px 误差，兼容滚动精度问题）
  const isBottom = bottomRect.bottom <= containerRect.bottom + 5

  isScrollToBottom.value = isBottom
}

function openDialog() {
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
}

function agreedDisclaimer() {
  disclaimerRead.value = true
  closeDialog()
  emits('agreed-hook')
}

onBeforeMount(() => {
  if (disclaimerRead.value) {
    closeDialog()
  }
})

defineExpose({
  openDialog,
  closeDialog,
  agreedDisclaimer,
})
</script>

<style lang="less" scoped>
.dialog-content {
  padding: 10px 30px;
  font-size: 16px;
  line-height: 1.5;
  height: 500px;
  overflow-y: auto;
}
.each-graph {
  text-indent: 24px;
}
h3 {
  margin: 18px 0 5px 0;
}
.each-detail {
  padding: 0 8px;
}
</style>
