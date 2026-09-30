<template>
  <section class="agents-page">
    <div class="agents-content">
      <header class="page-title">
        <h1>智能体广场</h1>
        <p>汇聚地灾业务与办公能力，助力每一项任务</p>
      </header>

      <nav class="primary-tabs" role="tablist" aria-label="智能体类型">
        <button
          v-for="section in AGENT_SECTIONS"
          :key="section.value"
          type="button"
          class="primary-tab"
          :class="{ 'is-active': activeSection === section.value }"
          role="tab"
          :aria-selected="activeSection === section.value"
          @click="activeSection = section.value"
        >
          {{ section.label }}
        </button>
      </nav>

      <Transition name="tab-content" mode="out-in">
        <section :key="activeSection" class="agent-section">
          <div v-if="activeSection === 'general'" class="toolbar">
            <div class="category-tabs" role="tablist" aria-label="办公助手分类">
              <button
                v-for="category in AGENT_CATEGORIES"
                :key="category.value"
                type="button"
                class="category-button"
                :class="{ 'is-active': activeCategory === category.value }"
                @click="activeCategory = category.value"
              >
                {{ category.label }}
              </button>
            </div>
            <el-input
              v-model.trim="keyword"
              class="search-input"
              placeholder="搜索AI工具"
              clearable
              :prefix-icon="Search"
            />
          </div>

          <template v-if="activeTools.length">
            <div class="tool-grid">
              <AgentToolCard v-for="tool in activeTools" :key="tool.title" :tool="tool" @open="openTool(tool)" />
            </div>
          </template>
          <el-empty v-else description="未找到匹配的智能体" />
        </section>
      </Transition>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import AgentToolCard from '@/components/AgentToolCard/index.vue'
import { useUserStore } from '@/store/user.js'
import { AGENT_CATEGORIES, AGENT_SECTIONS, AGENT_TOOLS } from '@/utils/agentTools.js'

defineOptions({ name: 'AgentSquarePage' })

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const activeCategory = ref('all')
const activeSection = ref('business')

const filteredGeneralTools = computed(() => {
  const searchValue = keyword.value.toLocaleLowerCase()
  return AGENT_TOOLS.filter((tool) => tool.section === 'general').filter((tool) => {
    const matchesCategory = activeCategory.value === 'all' || tool.category === activeCategory.value
    const matchesKeyword = !searchValue || `${tool.title}${tool.description}`.toLocaleLowerCase().includes(searchValue)
    return matchesCategory && matchesKeyword
  })
})

const activeTools = computed(() =>
  activeSection.value === 'general'
    ? filteredGeneralTools.value
    : AGENT_TOOLS.filter((tool) => tool.section === activeSection.value),
)

function openTool(tool) {
  if (tool.route) {
    router.push(tool.route)
    return
  }
  userStore.saveUserQuestion(tool.prompt)
  router.push({ path: '/chat-engine/chatting', query: { _refresh: Date.now() } })
}
</script>

<style lang="less" scoped>
.agents-page {
  flex: 1;
  min-height: 0;
  overflow: auto;
  color: #222527;
  background-color: #ffffff;
}

.agents-content {
  width: min(1004px, calc(100% - 64px));
  margin: 0 auto;
  padding: 36px 0 48px;
}

.page-title {
  text-align: center;

  h1 {
    margin: 0;
    color: #222527;
    font-family: 'Alimama FangYuanTi VF', sans-serif;
    font-size: 28px;
    line-height: 40px;
    font-style: normal;
    font-weight: 800;
  }

  p {
    margin: 4px 0 0;
    color: #9096a2;
    font-size: 14px;
    line-height: 22px;
  }
}

.primary-tabs {
  height: 48px;
  margin-top: 28px;
  display: flex;
  align-items: stretch;
  justify-content: flex-start;
  border-bottom: 1px solid #e0eefa;
}

.primary-tab {
  position: relative;
  min-width: 160px;
  padding: 0 24px;
  border: 0;
  color: #617185;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 16px;
  line-height: 48px;
  transition: color 0.2s ease, background-color 0.2s ease;

  &::after {
    position: absolute;
    right: 24px;
    bottom: 0;
    left: 24px;
    height: 3px;
    border-radius: 3px 3px 0 0;
    background: #007bff;
    content: '';
    opacity: 0;
    transform: scaleX(0.5);
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  &:hover {
    color: #007bff;
    background: linear-gradient(180deg, transparent 0%, rgba(220, 237, 255, 0.5) 100%);
  }

  &.is-active {
    color: #007bff;
    font-weight: 600;

    &::after {
      opacity: 1;
      transform: scaleX(1);
    }
  }
}

.agent-section {
  padding-top: 24px;
}

.tab-content-enter-active,
.tab-content-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.tab-content-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.tab-content-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.toolbar {
  margin: 12px 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-button {
  min-height: 32px;
  padding: 6px 14px;
  border: 1px solid #d9e0e8;
  border-radius: 8px;
  color: #617185;
  background: transparent;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;

  &:hover {
    color: #007bff;
    border-color: #007bff;
    background: #dcedff;
  }

  &.is-active {
    color: #007bff;
    border-color: #dcedff;
    background: #dcedff;
  }
}

.search-input {
  width: 220px;
  flex: 0 0 auto;

  :deep(.el-input__wrapper) {
    border-radius: 4px;
    background: #f4f7fa;
    box-shadow: 0 0 0 1px transparent inset;

    &.is-focus {
      background: #ffffff;
      box-shadow: 0 0 0 1px #007bff inset;
    }
  }
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1080px) {
  .tool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar {
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .agents-content {
    width: calc(100% - 32px);
    padding-top: 28px;
  }

  .toolbar {
    flex-direction: column;
  }

  .primary-tab {
    min-width: 0;
    flex: 1;
    padding: 0 12px;

    &::after {
      right: 12px;
      left: 12px;
    }
  }

  .search-input {
    width: 100%;
  }

  .tool-grid {
    grid-template-columns: 1fr;
  }
}
</style>
