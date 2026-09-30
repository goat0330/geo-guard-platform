export const template = {
  0: (text) => {
    return `
    <div class="template-tip-text">${text}</div>
  `
  },
  1: (data) => {
    const riskMap = {
      1: {
        color: '#E23030',
        text: '红色极高风险',
      },
      2: {
        color: '',
        text: '',
      },
      3: {
        color: '',
        text: '',
      },
    }
    return `
      <div class="template-title">
         当前注意到您正在关注 ${data?.name || data?.slopeUnitName || data?.id} 斜坡单元！该单元风险等级为<span style="color: ${riskMap[data?.level]?.color}">【${riskMap[data?.level]?.text}】</span>，核心信息如下：
       </div>
       <div class="template-tip-list">
         <div class="template-tip-item">
          <div class="point"></div>
          <div>位于 XX 镇 XX 村 X 组，涉及 387 名群众</div>
        </div>
         <div class="template-tip-item">
          <div class="point"></div>
          <div>当前有X个灾害点，该斜坡属于XX风险区，近期裂缝位移累计 3.2mm、土壤饱和度 78%（接近警戒值），雷达图已完成数据融合分析</div>
         </div>
       </div>
    `
  },
  2: (data) => {
    return `
      <div class="template-title">您好！\n已接收到【州级地灾防治中心】2025-11-29 11:38发布的地质灾害气象风险预警预报：</div>
      <div class="template-tip-list" style="margin-bottom: 20px">
        <div class="template-tip-item">
          <div class="point"></div>
          <div>预计未来24小时XXX地区累计<span class="text-bold text-blue">降雨≥80mm</span>，预警等级为<span class="text-orange text-bold">橙色预警</span></div>
        </div>
        <div class="template-tip-item">
          <div class="point"></div>
          <div>影响试点区域东北部，涉及<span class="text-blue text-bold">XXX个斜坡单元</span></div>
        </div>
      </div>
      <div class="template-title" style="margin-bottom: 0">系统将启动III级防御响应，是否确认？</div>
    `
  },
}
