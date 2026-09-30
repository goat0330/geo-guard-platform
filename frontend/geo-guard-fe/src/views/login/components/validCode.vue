<template>
  <img v-if="imageInfo" class="code-box" :src="imageInfo" alt="验证码" @click="getStringCaptcha" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getImageCode } from '@/api/sso.js'

const emits = defineEmits(['getUuid'])

const imageInfo = ref('')

/**
 * 获取图形验证码
 */
const getStringCaptcha = async () => {
  try {
    const res = await getImageCode()
    const { img, uuid } = res || {}
    if (img) {
      imageInfo.value = `data:image/gif;base64,${img}`
    }
    if (uuid) {
      emits('getUuid', uuid)
    }
  } catch (e) {
    console.error('获取图形验证码失败', e)
  }
}

onMounted(() => {
  getStringCaptcha()
})

defineExpose({
  getStringCaptcha,
})
</script>

<style scoped lang="less">
.code-box {
  width: 100%;
  height: 100%;
  cursor: pointer;
  object-fit: cover;
}
</style>
