<template>
  <div
    class="process-box"
    :style="[
      { 'margin-bottom': `${gap}px` }
    ]"
  >
    <div
      v-for="(item, index) in data"
      :key="index"
      class="process-item"
    >
      <template v-if="!item.noShow">
        <div class="process-item-box" :class="{ 'is-expanded': !!item.showList }" @click="showDetail(item)">
          <img class="header-icon" src="@/assets/imgs/chatBox/process/right.svg?url" />
          <div class="content">{{ item?.title }}</div>
          <img v-if="item?.list?.length" class="control-icon" :class="{ 'control-icon-active': !!item.showList }" src="@/assets/imgs/chatBox/process/down.svg?url" />
        </div>
        <Transition v-if="item?.list?.length" name="process">
          <div v-if="item?.showList" class="process-content">
            <div
              v-for="(detail, idx) in item?.list"
              class="process-content-item"
              :key="idx"
              @click="handleClick($event)"
            >
              <Loading v-if="detail.pointType === 'loading'" class-name="loading" :size="10" color-start="#3561FA" color-mid="rgba(53, 97, 250, 0.6)" color-end="#EEF8FF" />
              <div v-else class="point"></div>
              <div v-if="detail?.text" class="process-content-text">{{ detail?.text }}</div>
              <div v-else-if="detail?.html" class="process-content-text" v-dompurify-html="detail?.html"></div>
            </div>
          </div>
        </Transition>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Loading from '@/components/Icon/Loading/index.vue'

const props = defineProps({
  gap: {
    type: Number,
    default: 20
  },
  // { title: '', list: [ { text: '', html: '', pointType: '' } ], loading: true }
  data: {
    type: Array,
    default: () => ([])
  },
  handleClick: {
    type: Function,
    default: () => {}
  },
  pointType: {
    type: String,
    default: 'circle'
  }
})

const showDetail = (item) => {
  if (item.showList === 'undefined') {
    item.showList = false
  }
  item.showList = !item.showList
}
</script>

<style scoped lang="less">
.process-box {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .process-item {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .process-item-box {
      position: relative;
      border-radius: 12px;
      border: 1px solid #E7E7EA;
      background: #FBFBFF;
      display: flex;
      align-items: center;
      padding: 12px 20px 12px 12px;
      width: max-content;
      cursor: pointer;

      .header-icon {
        width: 14px;
        height: 14px;
        margin-right: 12px;
      }

      .content {
        flex: 1;
        font-size: 14px;
        text-align: justify;
        margin-right: 20px;
      }

      .control-icon {
        width: 12px;
        height: 12px;
        cursor: pointer;
      }

      .control-icon-active {
        transform: rotate(180deg);
      }

      &:hover, &.is-expanded {
        .content {
          color: #3561FA;
        }
      }
    }

    .process-content {
      margin-left: 26px;
      display: flex;
      flex-direction: column;

      .process-content-item {
        display: flex;
        gap: 11px;

        .point {
          width: 4px;
          height: 4px;
          background: #506073;
          flex-shrink: 0;
          margin-top: 10px;
          border-radius: 50%;
        }

        .process-content-text {
          color: #506073;
          text-align: justify;
          font-size: 14px;
          line-height: 22px;
          white-space: pre-wrap;
        }
      }
    }
  }
}

.process-enter-active,
.process-leave-active {
  transition: all 0.2s ease;
}

.process-enter-from,
.process-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.loading {
  position: relative;
  top: 11px;
}
</style>