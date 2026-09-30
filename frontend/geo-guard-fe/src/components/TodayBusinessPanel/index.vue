<template>
  <section class="section-container">
    <!-- 看板指标卡片 (2x2) -->
    <div class="metrics-grid">
      <!-- 指标 1：今日重点风险数量 -->
      <div class="metric-card">
        <div class="card-left">
          <img src="@/assets/imgs/home/card-banner-red.png" class="metric-icon" alt="风险数量" />
          <div class="metric-info">
            <div class="metric-label">今日重点风险数量</div>
            <div class="metric-number-row">
              <AnimatedNumber class="metric-num" :value="props.data?.keyRiskTotal ?? '--'" />
              <span v-if="props.data?.keyRiskTotal != null" class="metric-unit">处</span>
            </div>
          </div>
        </div>
        <div class="card-divider"></div>
        <div class="card-right">
          <div class="breakdown-row">
            <span class="breakdown-label">极高风险</span>
            <AnimatedNumber
              class="breakdown-val text-red"
              :value="props.data?.veryHighRiskCount ?? '--'"
              :suffix="props.data?.veryHighRiskCount != null ? '处' : ''"
            />
            <span
              v-if="props.data?.veryHighRiskTrend != null"
              :class="['trend-value', props.data.veryHighRiskTrend >= 0 ? 'is-up' : 'is-down']"
            >
              <AnimatedNumber
                :value="Math.abs(props.data.veryHighRiskTrend)"
                :prefix="props.data.veryHighRiskTrend >= 0 ? '+' : '-'"
              />
              <img :src="props.data.veryHighRiskTrend >= 0 ? trendUp : trendDown" alt="" />
            </span>
          </div>
          <div class="breakdown-row">
            <span class="breakdown-label">高风险</span>
            <AnimatedNumber
              class="breakdown-val text-orange"
              :value="props.data?.highRiskCount ?? '--'"
              :suffix="props.data?.highRiskCount != null ? '处' : ''"
            />
            <span
              v-if="props.data?.highRiskTrend != null"
              :class="['trend-value', props.data.highRiskTrend >= 0 ? 'is-up' : 'is-down']"
            >
              <AnimatedNumber
                :value="Math.abs(props.data.highRiskTrend)"
                :prefix="props.data.highRiskTrend >= 0 ? '+' : '-'"
              />
              <img :src="props.data.highRiskTrend >= 0 ? trendUp : trendDown" alt="" />
            </span>
          </div>
        </div>
      </div>

      <!-- 指标 2：待复核风险点 -->
      <div class="metric-card">
        <div class="card-left">
          <img src="@/assets/imgs/home/card-banner-blue.png" class="metric-icon" alt="待复核风险点" />
          <div class="metric-info">
            <div class="metric-label">待复核风险点</div>
            <div class="metric-number-row">
              <AnimatedNumber class="metric-num" :value="props.data?.pendingHazardCount ?? '--'" />
              <span v-if="props.data?.pendingHazardCount != null" class="metric-unit">项</span>
            </div>
          </div>
        </div>
        <div class="card-divider"></div>
        <div class="card-right review-counts">
          <div class="breakdown-row">
            <span class="breakdown-label">需优先处理项</span>
            <AnimatedNumber
              class="breakdown-val text-blue"
              :value="props.data?.priorityPendingCount ?? '--'"
              :suffix="props.data?.priorityPendingCount != null ? '项' : ''"
            />
            <span
              v-if="props.data?.priorityPendingTrend != null"
              :class="['trend-value', props.data.priorityPendingTrend >= 0 ? 'is-up' : 'is-down']"
            >
              <AnimatedNumber
                :value="Math.abs(props.data.priorityPendingTrend)"
                :prefix="props.data.priorityPendingTrend >= 0 ? '+' : '-'"
              />
              <img :src="props.data.priorityPendingTrend >= 0 ? trendUp : trendDown" alt="" />
            </span>
          </div>
          <div class="sub-tags-row">
            <template v-if="pendingHazardTypesList.length">
              <span v-for="t in pendingHazardTypesList" :key="t.label" class="sub-item">
                <span class="sub-label">{{ t.label }}</span>
                <AnimatedNumber class="sub-val text-red" :value="t.count" suffix="项" />
              </span>
            </template>
            <span v-else class="sub-item empty">
              <span class="sub-label">暂无待复核类型</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 指标 3：监测设备在线率 -->
      <div class="metric-card">
        <div class="card-left">
          <img src="@/assets/imgs/home/card-banner-purple.png" class="metric-icon" alt="设备在线率" />
          <div class="metric-info">
            <div class="metric-label">监测设备在线率</div>
            <div class="metric-number-row">
              <AnimatedNumber
                class="metric-num"
                :value="formattedOnlineRate"
                :suffix="props.data?.monitorOnlineRate != null ? '%' : ''"
              />
            </div>
          </div>
        </div>
        <div class="card-divider"></div>
        <div class="card-right device-counts">
          <div class="breakdown-row">
            <span class="breakdown-label">在线数量</span>
            <AnimatedNumber
              class="breakdown-val text-green"
              :value="props.data?.monitorDeviceOnlineCount ?? '--'"
              :suffix="props.data?.monitorDeviceOnlineCount != null ? '台' : ''"
            />
          </div>
          <div class="breakdown-row">
            <span class="breakdown-label">离线数量</span>
            <AnimatedNumber
              class="breakdown-val text-red"
              :value="props.data?.monitorDeviceOfflineCount ?? '--'"
              :suffix="props.data?.monitorDeviceOfflineCount != null ? '台' : ''"
            />
          </div>
        </div>
      </div>

      <!-- 指标 4：监测设备告警数 -->
      <div class="metric-card">
        <div class="card-left">
          <img src="@/assets/imgs/home/card-banner-orange.png" class="metric-icon" alt="设备告警数" />
          <div class="metric-info">
            <div class="metric-label">监测设备告警数</div>
            <div class="metric-number-row">
              <AnimatedNumber class="metric-num" :value="props.data?.monitorAlarmCount ?? '--'" />
              <span v-if="props.data?.monitorAlarmCount != null" class="metric-unit">条</span>
            </div>
          </div>
        </div>
        <div class="card-divider"></div>
        <div class="card-right alert-counts">
          <div class="alert-count-item">
            <span>当前有效</span>
            <strong class="text-blue">
              <AnimatedNumber
                :value="props.data?.validAlarmCount ?? '--'"
                :suffix="props.data?.validAlarmCount != null ? '条' : ''"
              />
            </strong>
          </div>
          <div class="alert-count-item">
            <span>已处置</span>
            <strong class="text-green">
              <AnimatedNumber
                :value="props.data?.alarmCompletedCount ?? '--'"
                :suffix="props.data?.alarmCompletedCount != null ? '条' : ''"
              />
            </strong>
          </div>
          <div class="alert-count-item">
            <span>未处置</span>
            <strong class="text-gray">
              <AnimatedNumber
                :value="props.data?.alarmUnprocessedCount ?? '--'"
                :suffix="props.data?.alarmUnprocessedCount != null ? '条' : ''"
              />
            </strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 看板分类摘要卡片 (4列) -->
    <div class="summary-cards-grid">
      <!-- 摘要 1：群测群防 -->
      <div class="summary-card-item">
        <div class="summary-card-header">
          <div class="feature-icon-box">
            <img src="@/assets/imgs/home/feature-chat.png" class="feature-icon" alt="群测群防" />
          </div>
          <span class="summary-title">群测群防</span>
        </div>
        <div class="summary-card-body">
          <div class="summary-main-val">
            <span class="sum-label">今日上报数</span>
            <div class="sum-val-wrap">
              <AnimatedNumber class="sum-number" :value="props.data?.publicReportCount ?? '--'" />
              <span v-if="props.data?.publicReportCount != null" class="sum-unit">条</span>
            </div>
          </div>
          <div class="summary-sub-stats">
            <span class="stat-item">
              <span class="stat-label">已核验</span>
              <AnimatedNumber
                class="stat-val text-green"
                :value="props.data?.publicReportCompletedCount ?? '--'"
                :suffix="props.data?.publicReportCompletedCount != null ? '条' : ''"
              />
            </span>
            <span class="stat-divider"></span>
            <span class="stat-item">
              <span class="stat-label">待核验</span>
              <AnimatedNumber
                class="stat-val text-red"
                :value="props.data?.publicReportPendingCount ?? '--'"
                :suffix="props.data?.publicReportPendingCount != null ? '条' : ''"
              />
            </span>
          </div>
          <div class="summary-divider-line"></div>
          <div class="summary-footer-info">
            <img src="@/assets/imgs/home/icon-image.png" class="info-icon" alt="" />
            <span>
              现场照片
              <AnimatedNumber
                :value="props.data?.publicReportPhotoCount ?? '--'"
                :suffix="props.data?.publicReportPhotoCount != null ? '份' : ''"
              />
            </span>
          </div>
        </div>
      </div>

      <!-- 摘要 2：动态预案 -->
      <div class="summary-card-item">
        <div class="summary-card-header">
          <div class="feature-icon-box">
            <img src="@/assets/imgs/home/feature-lightbulb.png" class="feature-icon" alt="动态预案" />
          </div>
          <span class="summary-title">动态预案</span>
        </div>
        <div class="summary-card-body">
          <div class="summary-main-val">
            <span class="sum-label">待更新数</span>
            <div class="sum-val-wrap">
              <AnimatedNumber class="sum-number" :value="props.data?.dynamicPlanPendingUpdateCount ?? '--'" />
              <span v-if="props.data?.dynamicPlanPendingUpdateCount != null" class="sum-unit">份</span>
            </div>
          </div>
          <div class="summary-sub-stats">
            <span class="stat-item">
              <span class="stat-label">新增风险点</span>
              <AnimatedNumber
                class="stat-val text-red"
                :value="props.data?.newHazardCount ?? '--'"
                :suffix="props.data?.newHazardCount != null ? '处' : ''"
              />
            </span>
          </div>
          <div class="summary-divider-line"></div>
          <div class="summary-footer-info">
            <img src="@/assets/imgs/home/icon-history.png" class="info-icon" alt="" />
            <span>最近更新时间 {{ props.data?.dynamicPlanLastUpdateTime || '--' }}</span>
          </div>
        </div>
      </div>

      <!-- 摘要 3：复盘任务 -->
      <div class="summary-card-item">
        <div class="summary-card-header">
          <div class="feature-icon-box">
            <img src="@/assets/imgs/home/feature-list.png" class="feature-icon" alt="复盘任务" />
          </div>
          <span class="summary-title">复盘任务</span>
        </div>
        <div class="summary-card-body">
          <div class="summary-main-val">
            <span class="sum-label">待生成任务数</span>
            <div class="sum-val-wrap">
              <AnimatedNumber class="sum-number" :value="props.data?.reviewTaskPendingGenerateCount ?? '--'" />
              <span v-if="props.data?.reviewTaskPendingGenerateCount != null" class="sum-unit">起</span>
            </div>
          </div>
          <div class="summary-sub-stats">
            <span class="stat-item">
              <span class="stat-label">已处置</span>
              <AnimatedNumber
                class="stat-val text-green"
                :value="props.data?.reviewTaskProcessedCount ?? '--'"
                :suffix="props.data?.reviewTaskProcessedCount != null ? '起' : ''"
              />
            </span>
            <span class="stat-divider"></span>
            <span class="stat-item">
              <span class="stat-label">未处置</span>
              <AnimatedNumber
                class="stat-val text-red"
                :value="props.data?.reviewTaskUnprocessedCount ?? '--'"
                :suffix="props.data?.reviewTaskUnprocessedCount != null ? '起' : ''"
              />
            </span>
          </div>
          <div class="summary-divider-line"></div>
          <div class="summary-footer-info">
            <img src="@/assets/imgs/home/icon-document.png" class="info-icon" alt="" />
            <span>
              本周已完成
              <AnimatedNumber
                :value="props.data?.weekCompletedReviewCount ?? '--'"
                :suffix="props.data?.weekCompletedReviewCount != null ? '份' : ''"
              />
            </span>
          </div>
        </div>
      </div>

      <!-- 摘要 4：今日业务进度 -->
      <div ref="progressSectionRef" class="summary-card-item progress-card">
        <div class="summary-card-header">
          <div class="feature-icon-box">
            <img src="@/assets/imgs/home/feature-trend.png" class="feature-icon" alt="今日业务进度" />
          </div>
          <span class="summary-title">今日业务进度</span>
        </div>
        <div class="summary-card-body">
          <div class="progress-list">
            <div class="progress-item">
              <div class="progress-info">
                <span class="prog-label">隐患复核</span>
                <AnimatedNumber
                  class="prog-percent"
                  :value="props.data?.hazardReviewRate ?? '--'"
                  :suffix="props.data?.hazardReviewRate != null ? '%' : ''"
                />
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill" :style="{ width: progressWidth(props.data?.hazardReviewRate) }"></div>
              </div>
            </div>
            <div class="progress-item">
              <div class="progress-info">
                <span class="prog-label">告警核查</span>
                <AnimatedNumber
                  class="prog-percent"
                  :value="props.data?.alarmCheckRate ?? '--'"
                  :suffix="props.data?.alarmCheckRate != null ? '%' : ''"
                />
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill" :style="{ width: progressWidth(props.data?.alarmCheckRate) }"></div>
              </div>
            </div>
            <div class="progress-item">
              <div class="progress-info">
                <span class="prog-label">巡查汇总</span>
                <AnimatedNumber
                  class="prog-percent"
                  :value="props.data?.patrolSummaryRate ?? '--'"
                  :suffix="props.data?.patrolSummaryRate != null ? '%' : ''"
                />
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill" :style="{ width: progressWidth(props.data?.patrolSummaryRate) }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import AnimatedNumber from '@/components/AnimatedNumber/index.vue'
import { useEnterView } from '@/composables/useEnterView.js'
import trendUp from '@/assets/imgs/fengxian/trend-up.png'
import trendDown from '@/assets/imgs/fengxian/trend-down.png'
import { formatHazardType } from '@/components/HomeBusinessDashboard/useHomeDashboard.js'

defineOptions({
  name: 'TodayBusinessPanel',
})

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
})

const { targetRef: progressSectionRef, hasEntered: progressHasEntered } = useEnterView()

/**
 * 进入可视区前保持 0，进入后过渡到业务目标进度。
 * @param {number|null} value 目标百分比
 * @returns {string} CSS 宽度值
 */
function progressWidth(value) {
  if (value == null || !Number.isFinite(Number(value))) return '0%'
  return progressHasEntered.value ? `${value}%` : '0%'
}

// 待复核类型分类列表
const pendingHazardTypesList = computed(() => {
  const list = props.data?.pendingHazardTypes
  if (!Array.isArray(list)) return []
  return list.map((item) => ({
    label: formatHazardType(item.value),
    count: item.count,
  }))
})

// 在线率安全转换
const formattedOnlineRate = computed(() => {
  const rate = props.data?.monitorOnlineRate
  if (rate == null || rate === '') return '--'
  const num = Number(rate)
  if (!Number.isFinite(num)) return '--'
  const percent = num <= 1 && num > 0 ? num * 100 : num
  return Number(percent.toFixed(1))
})
</script>

<style lang="less" scoped>
/* 通用模块结构 */
.section-container {
  width: 1000px;
  max-width: 100%;
  margin: 0;
  display: flex;
  flex-direction: column;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 800;
      color: #1a1a1a;
    }
  }
}

/* 指标 2x2 网格 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 18px;

  .metric-card {
    position: relative;
    background: #f8fafc;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    min-height: 116px;
    padding: 18px 22px;
    display: flex;
    align-items: center;
    transition: all 0.25s;

    &:hover {
      background: #ffffff;
      border-color: #dbeafe;
      box-shadow: 0 6px 20px rgba(42, 111, 247, 0.06);
    }

    .card-left {
      display: flex;
      align-items: center;
      gap: 16px;
      flex: 0 0 50%;
      min-width: 0;

      .metric-icon {
        width: 48px;
        height: 48px;
        object-fit: contain;
        flex-shrink: 0;
      }

      .metric-info {
        display: flex;
        flex-direction: column;

        .metric-label {
          font-size: 14px;
          color: #617185;
          margin-bottom: 8px;
        }

        .metric-number-row {
          display: flex;
          align-items: baseline;
          gap: 4px;

          .metric-num {
            font-family: 'Alimama FangYuanTi VF', sans-serif;
            font-size: 34px;
            font-weight: 800;
            color: #383c41;
            line-height: 1.1;
          }

          .metric-unit {
            font-family: 'Alimama FangYuanTi VF', sans-serif;
            font-weight: 700;
            font-size: 24px;
            color: #383c41;
            margin-left: 5px;
          }
        }
      }
    }

    .card-divider {
      position: absolute;
      left: 50%;
      top: 50%;
      width: 1px;
      height: 44px;
      background: #e2e8f0;
      transform: translate(-50%, -50%);
    }

    .card-right {
      flex: 1;
      min-width: 0;
      margin-left: 42px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      justify-content: center;

      .breakdown-row {
        display: flex;
        align-items: center;
        font-size: 14px;

        .breakdown-label {
          color: #617185;
          width: 80px;
          white-space: nowrap;
        }

        .breakdown-val {
          font-weight: 700;
          font-size: 18px;
          min-width: 48px;
        }

        .trend-value {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          font-weight: 600;
          margin-left: 10px;

          img {
            flex-shrink: 0;
            width: 10px;
            height: 10px;
            object-fit: contain;
          }

          &.is-up {
            color: #dd4739;
          }
          &.is-down {
            color: #44b699;
          }
        }
      }

      .sub-tags-row {
        display: flex;
        align-items: center;
        gap: 16px;
        font-size: 14px;
        line-height: 20px;
        white-space: nowrap;

        .sub-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;

          .sub-label {
            color: #617185;
            font-weight: 400;
          }

          .sub-val {
            color: #617185;
            font-weight: 600;

            &.text-red {
              color: #dd4739;
            }
          }
        }
      }
    }

    &:nth-child(even) .card-right {
      margin-left: 17px;
    }

    .review-counts {
      .breakdown-row {
        .breakdown-label {
          width: auto;
          white-space: nowrap;
          margin-right: 16px;
        }

        .breakdown-val {
          min-width: auto;
        }

        .trend-value {
          margin-left: 31px;
        }
      }
    }

    .device-counts {
      flex-direction: row;
      align-items: center;
      justify-content: flex-start;
      gap: 46px;

      .breakdown-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;

        .breakdown-label {
          width: auto;
        }
      }
    }

    .alert-counts {
      flex-direction: row;
      align-items: center;
      justify-content: flex-start;
      gap: 22px;

      .alert-count-item {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
        min-width: 0;
        color: #617185;
        font-size: 14px;
        white-space: nowrap;

        strong {
          font-family: 'Alimama FangYuanTi VF', sans-serif;
          font-size: 14px;
          line-height: 20px;
          font-weight: 700;
        }
      }
    }
  }
}

/* 看板 4 列摘要卡片 */
.summary-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 24px;

  .summary-card-item {
    display: flex;
    flex-direction: column;

    .summary-card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;

      .feature-icon-box {
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .feature-icon {
          width: 24px;
          height: 24px;
          object-fit: contain;
        }
      }

      .summary-title {
        font-size: 16px;
        font-weight: 700;
        color: #383c41;
        line-height: 20px;
      }
    }

    .summary-card-body {
      height: 142px;
      background: #f5f8fa;
      border: 1px solid #edf2f7;
      border-radius: 8px;
      padding: 16px;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: all 0.25s;

      &:hover {
        background: #ffffff;
        border-color: #dbeafe;
        box-shadow: 0 6px 20px rgba(42, 111, 247, 0.06);
      }

      .summary-main-val {
        display: flex;
        align-items: center;

        .sum-label {
          font-size: 14px;
          color: #617185;
          line-height: 20px;
        }

        .sum-val-wrap {
          display: flex;
          align-items: baseline;
          gap: 4px;

          .sum-number {
            font-family: 'Alimama FangYuanTi VF', sans-serif;
            font-size: 28px;
            font-weight: 700;
            color: #383c41;
            line-height: 1;
            margin: 0 19px 0 23px;
          }

          .sum-unit {
            font-family: 'Alimama FangYuanTi VF', sans-serif;
            font-size: 20px;
            font-weight: 700;
            color: #383c41;
            line-height: 1;
          }
        }
      }

      .summary-sub-stats {
        display: flex;
        align-items: center;
        font-size: 14px;
        line-height: 20px;

        .stat-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;

          .stat-label {
            color: #a6acb8;
            font-weight: 400;
          }

          .stat-val {
            font-weight: 600;

            &.text-green {
              color: #44b699;
            }

            &.text-red {
              color: #e45b5b;
            }
          }
        }

        .stat-divider {
          width: 1px;
          height: 12px;
          background: #e4eaef;
          margin: 0 16px 0 29px;
          flex-shrink: 0;
        }
      }

      .summary-divider-line {
        width: 100%;
        height: 1px;
        background: #e4eaef;
      }

      .summary-footer-info {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #9096a2;
        line-height: 14px;

        .info-icon {
          width: 14px;
          height: 14px;
          flex-shrink: 0;
          object-fit: contain;
        }
      }
    }

    &.progress-card {
      .summary-card-body {
        padding: 16px;

        .progress-list {
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          height: 100%;

          .progress-item {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .progress-info {
              display: flex;
              justify-content: space-between;
              align-items: center;

              .prog-label {
                font-size: 14px;
                color: #617185;
                line-height: 20px;
              }

              .prog-percent {
                font-family: 'Alimama FangYuanTi VF', sans-serif;
                font-size: 16px;
                font-weight: 700;
                color: #383c41;
                line-height: 20px;
              }
            }

            .progress-bar-bg {
              width: 100%;
              height: 4px;
              background: #e1e6ec;
              border-radius: 5px;
              overflow: hidden;

              .progress-bar-fill {
                height: 100%;
                background: linear-gradient(90deg, #007bff 12.5%, #00b2ff 100%);
                border-radius: 5px;
                transition: width 1.2s cubic-bezier(0.22, 1, 0.36, 1);
              }
            }
          }
        }
      }
    }
  }
}

/* 状态通用颜色 */
.text-red {
  color: #dd4739;
}
.text-orange {
  color: #ff922c;
}
.text-blue {
  color: #007bff;
}
.text-green {
  color: #44b699;
}
.text-gray {
  color: #617185;
}
</style>
