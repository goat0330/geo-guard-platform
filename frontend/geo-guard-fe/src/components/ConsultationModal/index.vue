<template>
  <Teleport to="body">
    <Transition name="consultation-modal">
      <div v-if="visible" class="consultation-modal-mask">
        <section class="consultation-modal" role="dialog" aria-modal="true" aria-labelledby="consultation-modal-title">
          <header class="consultation-modal__header">
            <h2 id="consultation-modal-title">会商研判</h2>
            <button class="close-button" type="button" title="关闭" aria-label="关闭" @click="emit('close')">
              <i class="iconfont icon-close"></i>
            </button>
          </header>

          <div class="consultation-modal__workspace">
            <NavTabs :model-value="activeTab" @update:model-value="handleTabChange" />
            <Transition name="panel-fade" mode="out-in">
              <CreateMeetingPanel v-if="activeTab === 'create'" key="create" @submit="emit('create', $event)" />
              <HistoryMeetingList v-else-if="activeTab === 'history'" key="history" />
              <CurrentMeetingPanel v-else key="current" @create="activeTab = 'create'" @enter="emit('enter', $event)" />
            </Transition>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import NavTabs from './components/NavTabs.vue'
import CurrentMeetingPanel from './components/CurrentMeetingPanel.vue'
import HistoryMeetingList from './components/HistoryMeetingList.vue'
import CreateMeetingPanel from './components/CreateMeetingPanel/index.vue'

defineOptions({ name: 'ConsultationModal' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  initialTab: {
    type: String,
    default: 'current',
  },
})

const emit = defineEmits(['close', 'create', 'enter', 'update:initialTab'])
const activeTab = ref(props.initialTab || 'current')

watch(
  () => [props.visible, props.initialTab],
  ([visible, tab]) => {
    if (visible) {
      activeTab.value = ['current', 'create', 'history'].includes(tab) ? tab : 'current'
    }
  },
  { immediate: true },
)

const handleTabChange = (tab) => {
  const nextTab = ['current', 'create', 'history'].includes(tab) ? tab : 'current'
  activeTab.value = nextTab
  emit('update:initialTab', nextTab)
}
</script>

<style lang="less" scoped>
.consultation-modal-mask {
  position: fixed;
  z-index: 2000;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.3);
}

.consultation-modal {
  display: flex;
  width: 1200px;
  height: 670px;
  max-width: calc(100vw - 48px);
  max-height: calc(100vh - 48px);
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.16);
  box-sizing: border-box;
}

.consultation-modal__header {
  display: flex;
  height: 28px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    color: #222527;
    font-size: 18px;
    font-weight: 700;
    line-height: 28px;
  }
}

.close-button {
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #222527;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: #f2f3f5;
  }

  i {
    font-size: 14px;
  }
}

.consultation-modal__workspace {
  display: flex;
  min-height: 0;
  flex: 1;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  background: #f2f3f5;
}

.panel-fade-enter-active,
.panel-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.consultation-modal-enter-active,
.consultation-modal-leave-active {
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  .consultation-modal {
    transition:
      opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.consultation-modal-enter-from,
.consultation-modal-leave-to {
  opacity: 0;

  .consultation-modal {
    opacity: 0;
    transform: scale(0.95);
  }
}

@media (max-width: 900px) {
  .consultation-modal-mask {
    padding: 16px;
  }

  .consultation-modal {
    max-width: calc(100vw - 32px);
    max-height: calc(100vh - 32px);
    padding: 16px;
  }

  .consultation-modal__workspace {
    padding: 12px;
  }
}
</style>
