<template>
  <div class="table-box" :style="{ 'margin-bottom': `${gap}px` }">
    <component :is="loadComp(config[tableId]?.component)" :gap="gap" :table-data="tableData" />
  </div>
</template>

<script setup>

import { defineAsyncComponent } from 'vue'

const config = {
  1: {
    component: () => import('@/components/ChatBox/Table/MessageTable.vue')
  }
}

const loadComp = (path) => {
  if (!path) return ''
  return defineAsyncComponent(path)
}

defineProps({
  tableId: {
    type: Number,
    default: 1
  },
  gap: {
    type: Number,
    default: 20
  },
  tableData: {
    type: Array,
    default: () => ([])
  }
})
</script>

<style scoped lang="less">
.table-box {
  width: 100%;
}
:deep(.el-table) {
  th.el-table__cell {
    background: #F4F6F9;
    line-height: 20px;
    color: #506073;
    font-size: 14px;
    font-weight: 500;
  }

  .el-table__cell {
    font-size: 14px !important;
    color: #222529;
    line-height: 20px;
  }

  .el-table__row--striped .el-table__cell {
    background: #F4F6F9 !important;
  }
}
</style>