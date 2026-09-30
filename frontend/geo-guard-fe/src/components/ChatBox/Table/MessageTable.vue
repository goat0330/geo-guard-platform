<template>
  <el-table stripe :data="tableData" border style="width: 100%">
    <el-table-column prop="send" label="发送对象" />
    <el-table-column prop="count" label="人数" />
    <el-table-column prop="channel" label="发送渠道" />
  </el-table>
</template>

<script setup>
defineProps({
  tableData: {
    type: Array,
    default: () => ([])
  }
})
</script>

<style scoped lang="less">

</style>