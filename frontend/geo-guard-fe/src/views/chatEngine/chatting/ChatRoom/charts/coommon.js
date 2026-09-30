export const COLORS = ['#8FB6F6', '#F9C568', '#FF7C3A', '#E23030']

export const COlOR_FUN = (data) => {
  return data.map(item => {
    const name = item.name || ''
    if (name.includes('极高风险')) return COLORS[3]
    if (name.includes('高风险')) return COLORS[2]
    if (name.includes('中风险')) return COLORS[1]
    if (name.includes('低风险')) return COLORS[0]
    return COLORS[0]
  })
}