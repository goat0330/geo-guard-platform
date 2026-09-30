<template>
  <Teleport to="body">
    <Transition name="route-dialog">
      <div v-if="visible" class="route-dialog-mask" @click.self="emit('close')">
        <section class="route-dialog" role="dialog" aria-modal="true" aria-labelledby="route-dialog-title">
          <header class="dialog-header">
            <h2 id="route-dialog-title">最新撤离路线</h2>
            <button class="close-button" type="button" title="关闭" aria-label="关闭" @click="emit('close')">
              <el-icon><Close /></el-icon>
            </button>
          </header>

          <div class="dialog-content">
            <EvacuationRouteMap
              class="route-map"
              :data="data"
              :active-index="mapActiveIndex"
              @select="handleMapSelect"
            />
            <EvacuationRouteDetail
              class="route-detail"
              :routes="routes"
              :expanded-index="expandedIndex"
              @toggle="handleDetailToggle"
            />
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import EvacuationRouteMap from '@/components/EvacuationRouteMap/index.vue'
import EvacuationRouteDetail from '@/components/EvacuationRouteDetail/index.vue'

defineOptions({ name: 'EvacuationRouteDialog' })

const props = defineProps({
  visible: { type: Boolean, default: false },
  data: {
    type: Object,
    default: () => ({ routes: [], disasterPolygonsWktList: [], resettlementAreas: [] }),
  },
})

const emit = defineEmits(['close'])
const expandedIndex = ref(0)
const mapActiveIndex = ref(-1)

const routes = computed(() =>
  (Array.isArray(props.data?.routes) ? props.data.routes : []).map((route) => {
    const distance = Number(route.distanceKm)
    const residents = route.residentsInfo || {}
    return {
      ...route,
      peopleCount: `${Number(residents.totalCount) || 0}人`,
      specialPeople: `老人${Number(residents.elderlyCount) || 0}人/儿童${Number(residents.childrenCount) || 0}人`,
      distance: Number.isFinite(distance)
        ? distance < 1
          ? `${Math.round(distance * 1000)}米`
          : `${distance}公里`
        : '--',
      walkTime: `${Number(route.estimatedTimeMinutes) || 0}分钟`,
    }
  }),
)

function handleDetailToggle(index) {
  const isClosing = expandedIndex.value === index
  expandedIndex.value = isClosing ? -1 : index
  mapActiveIndex.value = isClosing ? -1 : index
}

function handleMapSelect(index) {
  const isClosing = mapActiveIndex.value === index
  mapActiveIndex.value = isClosing ? -1 : index
  expandedIndex.value = isClosing ? -1 : index
}

function handleKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

watch(
  () => props.visible,
  (visible) => {
    window.removeEventListener('keydown', handleKeydown)
    if (visible) {
      expandedIndex.value = routes.value.length ? 0 : -1
      mapActiveIndex.value = -1
      window.addEventListener('keydown', handleKeydown)
    }
  },
)

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="less" scoped>
.route-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 3100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  box-sizing: border-box;
  background: rgba(34, 37, 39, 0.42);
  font-family: 'AlibabaPuHuiTi', sans-serif;
}

.route-dialog {
  display: flex;
  width: min(1760px, calc(100vw - 64px));
  height: min(864px, calc(100vh - 64px));
  min-height: 560px;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 18px 56px rgba(34, 37, 39, 0.2);
}

.dialog-header {
  display: flex;
  height: 74px;
  flex: 0 0 74px;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px 0 32px;
  border-bottom: 1px solid #e0eefa;
  box-sizing: border-box;

  h2 {
    margin: 0;
    color: #222527;
    font-size: 20px;
    font-weight: 600;
    line-height: 28px;
  }
}

.close-button {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #383c41;
  cursor: pointer;
  font-size: 22px;
  transition:
    background 0.2s ease,
    color 0.2s ease;

  &:hover {
    background: #e0eefa;
    color: #007bff;
  }
}

.dialog-content {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: minmax(0, 1.62fr) minmax(420px, 1fr);
  gap: 24px;
  padding: 24px 28px 28px;
  box-sizing: border-box;
}

.route-map,
.route-detail {
  min-width: 0;
  min-height: 0;
}

.route-dialog-enter-active,
.route-dialog-leave-active {
  transition: opacity 0.22s ease;

  .route-dialog {
    transition:
      transform 0.24s ease,
      opacity 0.2s ease;
  }
}

.route-dialog-enter-from,
.route-dialog-leave-to {
  opacity: 0;

  .route-dialog {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
  }
}

@media (max-width: 1100px) {
  .route-dialog-mask {
    padding: 16px;
  }

  .route-dialog {
    width: calc(100vw - 32px);
    height: calc(100vh - 32px);
    min-height: 0;
  }

  .dialog-content {
    display: flex;
    overflow-y: auto;
    flex-direction: column;
    padding: 16px;
  }

  .route-map {
    min-height: 420px;
    flex: 0 0 52vh;
  }

  .route-detail {
    min-height: 440px;
  }
}
</style>
