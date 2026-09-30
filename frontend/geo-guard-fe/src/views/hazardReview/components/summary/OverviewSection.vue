<template>
  <div class="overview">
    <!-- 累计接收风险源：总量卡片 -->
    <div class="overview-total">
      <img class="overview-total__icon" :src="sourceOverview.icon" alt="" />
      <div class="overview-total__text">
        <span class="overview-total__label">累计接收风险源</span>
        <span class="overview-total__caption">{{ sourceOverview.caption }}</span>
      </div>
      <p class="overview-total__value">
        <strong><AnimatedNumber :value="sourceOverview.total" /></strong>
        <em>条</em>
      </p>
    </div>

    <!-- 来源拆分：基层智治 / 大排查 / 其他来源 -->
    <ul class="overview-sources">
      <li v-for="item in sourceOverview.items" :key="item.key" class="source-item">
        <img class="source-item__icon" :src="item.icon" alt="" />
        <div class="source-item__text">
          <span class="source-item__label">{{ item.label }}</span>
          <strong class="source-item__value"><AnimatedNumber :value="item.value" /></strong>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { hazardReviewData } from '../../useHazardReviewData.js'

defineOptions({ name: 'ReviewOverviewSection' })

/** 累计接收风险源与来源拆分：接口返回后写入共享数据源，未返回时为空值 */
const sourceOverview = computed(() => hazardReviewData.overview.source)
</script>

<style lang="less" scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overview-total {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 68px;
  padding: 0 16px;
  box-sizing: border-box;
  border: 1px solid #edf6ff;
  border-radius: 10px;
  background: #f5f9fc;
}

.overview-total__icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  object-fit: contain;
}

.overview-total__text {
  min-width: 0;
}

.overview-total__label {
  display: block;
  color: #383c41;
  font-size: 14px;
  font-weight: 600;
  line-height: 14px;
  white-space: nowrap;
}

.overview-total__caption {
  display: block;
  margin-top: 8px;
  color: #a6acb8;
  font-size: 10px;
  line-height: 10px;
  white-space: nowrap;
}

.overview-total__value {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 0 auto;
  color: #222527;

  strong {
    font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
    font-size: 28px;
    font-style: normal;
    font-weight: 700;
    line-height: 26px;
  }

  em {
    font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
    font-size: 20px;
    font-style: normal;
    font-weight: 700;
    line-height: 32px;
  }
}

.overview-sources {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 68px;
  padding: 0 12px;
  box-sizing: border-box;
  border: 1px solid #edf6ff;
  border-radius: 10px;
  background: #f5f9fc;
}

.source-item__icon {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  object-fit: contain;
}

.source-item__text {
  min-width: 0;
}

.source-item__label {
  display: block;
  color: #617185;
  font-size: 12px;
  line-height: 14px;
  white-space: nowrap;
}

.source-item__value {
  display: block;
  margin-top: 6px;
  color: #383c41;
  font-family: 'Alimama FangYuanTi VF', 'AlibabaPuHuiTi', sans-serif;
  font-size: 20px;
  font-weight: 600;
  line-height: 20px;
  white-space: nowrap;
}
</style>
