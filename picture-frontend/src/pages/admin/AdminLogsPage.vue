<template>
  <div class="logs-page"><div class="page-title"><div><span>OPERATIONS / AUDIT</span><h1>操作日志</h1><p>审核与后台操作记录</p></div><a-button :loading="loading" @click="load">刷新</a-button></div><div class="table-panel"><a-table :columns="columns" :data-source="records" :loading="loading" row-key="id" :pagination="{ current: page, pageSize: 20, total, showSizeChanger: false }" :scroll="{ x: 800 }" @change="onChange"><template #bodyCell="{ column, record }"><template v-if="column.key === 'date'">{{ record.createTime ? new Date(record.createTime).toLocaleString('zh-CN') : '' }}</template><template v-else-if="column.key === 'detail'"><span class="detail">{{ record.detail }}</span></template></template></a-table></div></div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listOperationLogs } from '@/api/adminConsole'
const loading = ref(false); const records = ref<any[]>([]); const page = ref(1); const total = ref(0)
const columns = [{ title: '时间', key: 'date', dataIndex: 'createTime', width: 200, customRender: ({ text }: any) => (text ? new Date(text).toLocaleString('zh-CN') : '') }, { title: '操作人 ID', dataIndex: 'userId', width: 190 }, { title: '模块', dataIndex: 'module', width: 130 }, { title: '动作', dataIndex: 'action', width: 120 }, { title: '详情', key: 'detail' }, { title: 'IP', dataIndex: 'ip', width: 140 }]
async function load() { loading.value = true; try { const res: any = await listOperationLogs({ current: page.value, pageSize: 20 }); if (res.data.code !== 0) throw new Error(res.data.message); records.value = res.data.data?.records || []; total.value = Number(res.data.data?.total || 0) } catch (e: any) { message.error(e?.message || '日志加载失败') } finally { loading.value = false } }
function onChange(p: any) { page.value = p.current; void load() }
onMounted(load)
</script>
<style scoped>
.page-title { display:flex; align-items:end; justify-content:space-between; margin-bottom:24px; }.page-title span { color:var(--text-3); font-size:11px; letter-spacing:.14em; }.page-title h1 { margin:5px 0; font-size:26px; color:var(--text-1); }.page-title p { margin:0; color:var(--text-3); font-size:13px; }.table-panel { padding:18px; background:#fff; border:1px solid var(--border); border-radius:8px; }.detail { display:block; max-width:440px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
</style>
