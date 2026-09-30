<template>
  <div class="weather-report">
    <div v-if="!hasDone" class="parsing-text">解析中</div>
    <div v-else>
      <div class="report-title">【气象风险预警报告解译结果】</div>
      <div class="report-content">
        <!-- 静态数据面板 -->
        <weatherInfo :data="jsonData" :weatherData="weatherData"></weatherInfo>

        <div v-auth="'geo:ai:sql'">
          <div class="spliter"></div>

          <div v-if="isSubmitted" class="submit-tip">
            <el-icon><SuccessFilled /></el-icon
            >已调用数据入库接口，将本次预警关键信息<strong>同步至系统数据库</strong>，后续将<strong>自动关联对应区域隐患台账</strong>，为风险研判提供支撑。
          </div>
          <template v-else-if="!isHistory">
            <!-- 入库提示 -->
            <div v-if="!isModifyMode" class="sql-tip">
              您可以点击下方“报告入库”按钮，将本次预警关键信息<strong>同步至系统数据库</strong>，后续将<strong>自动关联对应区域隐患台账</strong>，为风险研判提供支撑，如您已保存请忽略～
              <br />
              <el-button type="primary" @click="handleSave" class="operate-btn"
                >报告入库<el-icon><Right /></el-icon
              ></el-button>
            </div>

            <!-- 入库操作表单 -->
            <div v-else class="weather-form-box">
              <weatherForm
                :data="jsonData"
                :weatherData="weatherData"
                :extParams="extParams"
                @cancel="isModifyMode = false"
                @submit-success="handleSubmitSuccess"
              ></weatherForm>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { watch, ref, nextTick } from 'vue'
import dayjs from 'dayjs'
import weatherInfo from './components/weatherInfo.vue'
import weatherForm from './components/weatherForm.vue'
import { hasAuth } from '@/utils/directive.js'
import { ElMessage } from 'element-plus'

const props = defineProps({
  hasDone: {
    type: Boolean,
    default: false,
  },
  data: {
    type: [null, String],
    default: null,
  },
  // conversationId: {
  //   type: String,
  //   default: '',
  // },
  hasRegister: {
    type: Boolean,
    default: false,
  },
  extParams: {
    type: Object,
    default: () => ({}),
  },
  isHistory: {
    type: Boolean,
    default: false,
  }
})

const emits = defineEmits(['scroll-to-bottom'])
let jsonData = ref({})
const weatherData = ref([])
const activeIndex = ref(0)
const isModifyMode = ref(false)
const isSubmitted = ref(props.hasRegister)

watch(
  () => props.hasDone,
  (newV) => {
    console.log('extParams---:', props.extParams)
    newV && parseData(props.data)
  },
  { immediate: true },
)

function parseData(string) {
  console.log('原始字符串:', string)
  try {
    // 预处理字符串：匹配双引号内的内容，将其中的真实换行、回车、制表符进行安全转义
    // 避免打断外部 JSON 结构，并确保内部文本在 JSON.parse 时不会抛出异常
    let safeString = string.replace(/"([^"\\]*(?:\\.[^"\\]*)*)"/g, function (match) {
      return match.replace(/\n/g, '\\n').replace(/\r/g, '\\r').replace(/\t/g, '\\t')
    })

    jsonData.value = JSON.parse(safeString)
    // 空格切割，过滤空字符串，重新用顿号连接
    if (jsonData.value.key_tips && typeof jsonData.value.key_tips === 'string') {
      jsonData.value.key_tips = jsonData.value.key_tips.split(/\s+/).filter(Boolean).join('、')
    }
    weatherData.value = jsonData.value.data.map((item, index) => {
      console.log('item---:', item)
      return {
      ...item,
      showTime: item.time ? dayjs(item.time).format('MM月DD日') : '数据缺失',
      tips: jsonData.value.key_tips,
    }
    })
  } catch (e) {
    console.error('解析数据失败:', e)
    jsonData.value = {}
  }
  console.log('jsonData:', jsonData.value)
}

function handleSave() {
  if (!hasAuth('geo:ai:sql')) {
    return ElMessage.error('您没有权限执行此操作')
  }
  isModifyMode.value = true
  nextTick(() => {
    emits('scroll-to-bottom')
  })
}

function handleSubmitSuccess() {
  isSubmitted.value = true
}
</script>

<style lang="less" scoped>
.parsing-text {
  display: flex;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #878898;
  min-width: 60px;
  &::after {
    content: '...';
    display: block;
    animation: ellipsis-animation 1s linear infinite;
  }
}
@keyframes ellipsis-animation {
  0% {
    content: '.';
  }
  33% {
    content: '..';
  }
  66%,
  100% {
    content: '...';
  }
}

.report-content {

  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.report-title {
  font-size: 20px;
  color: #3561fa;
  font-weight: 800;
  margin-bottom: 18px;
  line-height: 24px;
}

.spliter {
  width: 100%;
  height: 1px;
  background: #dfe4f0;
  margin: 20px 0;
}

.sql-tip {
  color: var(--el-color-primary);

  strong {
    font-weight: 800;
  }

  .operate-btn {
    margin-top: 10px;
    :deep(&.el-button) {
      border-radius: 10px;
    }
  }
}

.submit-tip {
  color: #2ca86e;
  font-size: 14px;
  display: flex;
  align-items: center;
  i {
    margin-right: 6px;
  }
  strong {
    font-weight: 800;
  }
}

.weather-form-box {
  min-width: 400px;
}
</style>
