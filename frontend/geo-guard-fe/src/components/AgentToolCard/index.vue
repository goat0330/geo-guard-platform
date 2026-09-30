<template>
  <article
    class="agent-tool-card"
    tabindex="0"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @focusin="isHovered = true"
    @focusout="isHovered = false"
    @click="$emit('open')"
    @keydown.enter="$emit('open')"
  >
    <img v-if="tool.badgeImage" :src="tool.badgeImage" :alt="tool.tag" class="tool-badge-image" />
    <span v-else-if="tool.tag" class="tool-badge" :class="`is-${tool.tag.toLowerCase()}`">{{ tool.tag }}</span>

    <div class="tool-main">
      <span class="tool-icon-wrap">
        <img :src="tool.image" :alt="tool.title" class="tool-icon" />
        <img v-if="tool.hoverImage && isHovered" :src="tool.hoverImage" alt="" class="tool-icon tool-icon-hover" />
      </span>
      <div class="tool-copy">
        <h2>{{ tool.title }}</h2>
        <p>{{ tool.description }}</p>
      </div>
    </div>

    <footer class="tool-footer">
      <span class="tool-source">
        <i class="iconfont icon-foler" aria-hidden="true"></i>
        {{ tool.source }}
      </span>
      <span class="tool-operation">
        <span class="tool-usage">{{ tool.usage }}</span>
        <span class="tool-action">
          {{ tool.action || '立即使用' }}
          <el-icon><Right /></el-icon>
        </span>
      </span>
    </footer>
  </article>
</template>

<script setup>
import { ref } from 'vue'
import { Right } from '@element-plus/icons-vue'

defineOptions({ name: 'AgentToolCard' })

defineProps({
  tool: {
    type: Object,
    required: true,
  },
})

defineEmits(['open'])

const isHovered = ref(false)
</script>

<style lang="less" scoped>
.agent-tool-card {
  position: relative;
  box-sizing: border-box;
  min-width: 0;
  height: 124px;
  padding: 20px 20px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid #d5dbe0;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  overflow: hidden;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover,
  &:focus-visible {
    outline: none;
    border-color: #007bff;
    background-color: #f1f8ff;
    box-shadow: 0 2px 10px rgba(29, 100, 177, 0.2);
  }
}

.tool-badge,
.tool-badge-image {
  position: absolute;
  top: 0;
  right: 0;
  width: 48px;
  height: 20px;
}

.tool-badge {
  display: grid;
  place-items: center;
  border-radius: 0 16px 0 16px;
  color: #ffffff;
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-size: 12px;
  font-style: normal;

  &.is-hot {
    background: linear-gradient(90deg, #ff6e1a 0%, #ffa600 100%);
  }

  &.is-new {
    background: #007bff;
  }
}

.tool-badge-image {
  object-fit: contain;
}

.tool-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.tool-icon-wrap {
  position: relative;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: block;
}

.tool-icon {
  position: absolute;
  inset: 0;
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.tool-icon-hover {
  z-index: 1;
}

.tool-copy {
  min-width: 0;

  h2 {
    margin: 0 0 8px;
    color: #383c41;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-size: 16px;
    line-height: 20px;
    font-style: normal;
    font-weight: Bold-Square;
  }

  p {
    margin: 0;
    color: #9096a2;
    font-size: 14px;
    line-height: 16px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.tool-footer {
  height: 24px;
  flex: 0 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #a6acb8;
  font-size: 12px;
  line-height: 16px;
}

.tool-source {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;

  .icon-foler {
    font-size: 12px;
    line-height: 1;
    transform: translateY(-1px);
  }
}

.tool-usage {
  font-family: 'Alimama FangYuanTi VF', sans-serif;
  font-style: normal;
  opacity: 1;
  visibility: visible;
  transition:
    opacity 0.2s ease,
    visibility 0.2s ease;
}

.tool-operation {
  position: relative;
  min-width: 86px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
}

.tool-action {
  position: absolute;
  top: 0;
  right: 0;
  box-sizing: border-box;
  width: 86px;
  height: 24px;
  flex: 0 0 auto;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
  font-size: 12px;
  line-height: 14px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(4px);
  transition:
    opacity 0.2s ease,
    visibility 0.2s ease,
    transform 0.2s ease;

  .el-icon {
    font-size: 14px;
  }
}

.agent-tool-card:hover .tool-action,
.agent-tool-card:focus-visible .tool-action {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.agent-tool-card:hover .tool-usage,
.agent-tool-card:focus-visible .tool-usage {
  opacity: 0;
  visibility: hidden;
}
</style>
