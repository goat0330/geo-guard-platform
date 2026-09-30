// 两类首页环图共用半径和分段，保持外环与浅色内环严格对齐。
export function createHomeDonutSeries(data, px, progress = 1) {
  const ring = {
    type: 'pie',
    center: ['50%', '50%'],
    startAngle: 90,
    label: { show: false },
    labelLine: { show: false },
    emphasis: { disabled: true },
  }
  const normalizedProgress = Math.min(Math.max(progress, 0), 1)
  const total = data.reduce((sum, item) => sum + item.num, 0)
  const segments = data.map((item) => ({
    name: item.label,
    value: item.num * normalizedProgress,
    itemStyle: { color: item.color },
  }))
  const remainingSegment = {
    name: '',
    value: total * (1 - normalizedProgress),
    itemStyle: { color: 'transparent', borderWidth: 0 },
    tooltip: { show: false },
  }

  return [
    {
      ...ring,
      radius: ['72%', '84%'],
      itemStyle: { borderColor: '#ffffff', borderWidth: px(3) },
      data: [...segments, remainingSegment],
      z: 3,
    },
    {
      ...ring,
      radius: ['59%', '71%'],
      silent: true,
      tooltip: { show: false },
      itemStyle: { opacity: 0.16, borderColor: '#ffffff', borderWidth: px(3) },
      data: [...segments, remainingSegment],
      z: 2,
    },
    {
      ...ring,
      radius: ['51%', '57%'],
      silent: true,
      tooltip: { show: false },
      data: [{ value: 1, itemStyle: { color: '#F4F6FA' } }],
      z: 1,
    },
    {
      ...ring,
      radius: ['0%', '50%'],
      silent: true,
      tooltip: { show: false },
      data: [
        {
          value: 1,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0.7,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#FAFBFE' },
                { offset: 0.55, color: '#F4F6FA' },
                { offset: 1, color: '#DFE3EE' },
              ],
            },
          },
        },
      ],
      z: 0,
    },
  ]
}
