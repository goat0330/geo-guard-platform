<template>
  <div class="space-analysis-prompt" :class="{ 'is-compact': compact }">
    <textarea
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="updateValue"
      @keydown="handleKeydown"
    />
    <div class="prompt-actions">
      <div v-if="selectionLabel" class="selection-association">
        <span class="association-prefix">已关联：</span>
        <span class="association-chip">
          <i class="iconfont icon-a-Localyidingwei" />
          <span>{{ selectionLabel }}</span>
          <button
            type="button"
            aria-label="取消关联位置"
            :disabled="disabled || answering"
            @click="$emit('remove-selection')"
          >
            ×
          </button>
        </span>
      </div>
      <button
        type="button"
        class="location-trigger"
        :disabled="disabled || answering"
        @click="$emit('select-location')"
      >
        <i class="iconfont icon-a-Localyidingwei" />{{ locationLabel }}
      </button>
      <span class="prompt-hint">{{ hint }}</span>
      <button
        v-if="!answering"
        type="button"
        class="send-button"
        :disabled="disabled || !canSubmit"
        aria-label="发送问题"
        @click="$emit('submit')"
      >
        <i class="iconfont icon-a-Arrow-upjiantoushang" />
      </button>
      <button v-else type="button" class="stop-button" aria-label="停止生成" @click="$emit('stop')">
        <i class="iconfont icon-stop" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatSpaceAnalysisSelection } from '@/utils/spaceAnalysis.js'

defineOptions({ name: 'SpaceAnalysisPrompt' })

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '想了解哪个地方？先选个位置，或直接告诉我',
  },
  locationLabel: {
    type: String,
    default: '选位置/范围',
  },
  hint: {
    type: String,
    default: '选择位置后开始分析',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  answering: {
    type: Boolean,
    default: false,
  },
  canSubmit: {
    type: Boolean,
    default: true,
  },
  submitOnEnter: {
    type: Boolean,
    default: false,
  },
  selection: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'select-location', 'remove-selection', 'submit', 'stop'])
const selectionLabel = computed(() => formatSpaceAnalysisSelection(props.selection))

function updateValue(event) {
  emit('update:modelValue', event.target.value)
}

function handleKeydown(event) {
  const shouldSubmit = event.key === 'Enter' && !event.shiftKey && (props.submitOnEnter || event.ctrlKey)
  if (!shouldSubmit) return

  event.preventDefault()
  if (!props.disabled && props.canSubmit) emit('submit')
}
</script>

<style lang="less" scoped>
.space-analysis-prompt {
  width: 100%;
  height: 196px;
  min-height: 196px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  border: 1px solid #dce1ea;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 0 10px 0 #0075dc33;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.space-analysis-prompt.is-compact {
  height: 130px;
  min-height: 130px;
  border-color: rgba(0, 123, 255, 0.4);
}

.space-analysis-prompt:focus-within {
  border-color: #b9c8df;
  box-shadow: 0 2px 6px rgba(57, 126, 251, 0.1);
}

textarea {
  width: 100%;
  flex: 1;
  min-height: 0;
  padding: 18px;
  border: 0;
  outline: none;
  resize: none;
  box-sizing: border-box;
  color: #222527;
  background: transparent;
  font: inherit;
  font-size: 14px;
  line-height: 24px;
}

textarea::placeholder {
  color: #a6acb8;
}

textarea:disabled {
  color: #617185;
  cursor: not-allowed;
}

.prompt-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px 12px;
  color: #a6acb8;
  font-size: 14px;
}

.selection-association {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.association-prefix {
  flex: 0 0 auto;
  color: #617185;
}

.association-chip {
  max-width: 320px;
  height: 32px;
  min-width: 0;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-sizing: border-box;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  color: #383c41;
  background: #ffffff;

  > span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .iconfont {
    flex: 0 0 auto;
    font-size: 14px;
  }

  button {
    width: 16px;
    height: 16px;
    flex: 0 0 16px;
    padding: 0;
    border: 0;
    color: #a6acb8;
    background: transparent;
    cursor: pointer;
    font: inherit;
    font-size: 16px;
    line-height: 16px;

    &:hover {
      color: #617185;
    }

    &:disabled {
      cursor: not-allowed;
    }
  }
}

.location-trigger {
  height: 32px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #e4eaef;
  border-radius: 16px;
  color: #383c41;
  background: #ffffff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;

  &:hover {
    border-color: #007bff;
    color: #007bff;
  }

  &:disabled {
    border-color: #e4eaef;
    color: #a6acb8;
    cursor: not-allowed;
  }

  .iconfont {
    width: 14px;
    height: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    line-height: 1;
  }
}

.prompt-hint {
  min-width: 0;
  margin-left: auto;
  overflow: hidden;
  color: #a6acb8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.send-button,
.stop-button {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  cursor: pointer;
}

.send-button {
  background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);

  &:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
}

.stop-button {
  background: #617185;
}

@media (max-width: 720px) {
  .association-prefix {
    display: none;
  }

  .association-chip {
    max-width: 180px;
  }

  .prompt-hint {
    display: none;
  }

  .send-button,
  .stop-button {
    margin-left: auto;
  }
}
</style>
