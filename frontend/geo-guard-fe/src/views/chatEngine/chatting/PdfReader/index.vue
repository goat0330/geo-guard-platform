<template>
  <div class="pdf-reader" v-loading="loading">
    <div v-if="!loading" class="tips">
      <el-icon><WarningFilled /></el-icon>
      <div>
        注意：此处仅展示引用部分，若您需要查看全文，可点击<span class="check-full" @click="openNewTab">查看全文</span>
      </div>
    </div>
    <div class="pdf-content">
      <VuePdfEmbed
        :annotation-layer="false"
        :text-layer="false"
        :source="{
          cMapUrl: '/cmaps/', // 必须，否则会有概率出现中文丢失的情况-
          url: url,
        }"
        :page="pages"
        @rendered="handleFirstPageRendered"
        @error="handleError"
      ></VuePdfEmbed>
    </div>
    <div v-if="showErrorText">加载失败</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import VuePdfEmbed from 'vue-pdf-embed'
import { simpleClone } from '@/utils/index'
const props = defineProps({
  info: {
    type: Object,
    required: true,
  },
})

const pdfCanvas = ref(null)
const url = ref()

let bboxes = []
const pages = ref(null)

const loading = ref(true)
const showErrorText = ref(false)
function handleFirstPageRendered() {
  loading.value = false
  setTimeout(() => {
    let contents = document.querySelectorAll('.vue-pdf-embed__page canvas')
    bboxes.forEach((pos, key) => {
      const canvas = contents[key]
      let domWidth = canvas.width
      let domHeight = canvas.height
      const ctx = canvas.getContext('2d')

      const rectX = domWidth * pos.bbox[0]
      const rectY = domHeight * pos.bbox[1]
      const rectWidth = domWidth * pos.bbox[2] - rectX
      const rectHeight = domHeight * pos.bbox[3] - rectY

      ctx.beginPath()
      ctx.rect(rectX, rectY, rectWidth, rectHeight)
      ctx.lineWidth = 3
      ctx.strokeStyle = 'red'
      ctx.stroke()
    })
  }, 300)
}

function handleError() {
  loading.value = false
  showErrorText.value = true
}

function openNewTab() {
  window.open(url.value)
}

onMounted(() => {
  bboxes = simpleClone(props.info.bboxes)
  let refPages = bboxes.map((v) => v.page + 1)
  pages.value = refPages && refPages.length ? refPages : null
  url.value = props.info.source_url
})
</script>

<style lang="less" scoped>
.pdf-reader {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #282828;
}
.tips {
  position: absolute;
  z-index: 10;
  top: 0;
  width: 100%;
  display: flex;
  align-items: center;
  font-size: 12px;
  line-height: 24px;
  color: #e6a23c;
  background-color: #fdf6ec;
  i {
    margin-right: 4px;
  }
  .check-full {
    color: red;
    cursor: pointer;
    margin-left: 2px;
    font-size: 12px;
    &:hover {
      text-decoration: underline;
    }
  }
}
.pdf-content {
  width: 70%;
  margin: 0 auto;
  padding-top: 24px;
  overflow: auto;
}
</style>
