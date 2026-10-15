<template>
  <div class="users-page"><div class="page-title"><div><span>OPERATIONS / MEMBERS</span><h1>用户与权限</h1><p>查询账号状态，封禁或恢复违规用户。</p></div></div>
    <div class="panel"><div class="filters"><a-input v-model:value="query.userAccount" placeholder="搜索账号" allow-clear style="width:210px" @press-enter="search" /><a-input v-model:value="query.userName" placeholder="搜索昵称" allow-clear style="width:210px" @press-enter="search" /><a-button type="primary" @click="search">查询</a-button><a-button @click="reset">重置</a-button></div>
      <a-table :columns="columns" :data-source="users" :loading="loading" row-key="id" :scroll="{ x: 850 }" :pagination="{ current: query.current, pageSize: query.pageSize, total, showSizeChanger: false, showTotal: (n: number) => `共 ${n} 位用户` }" @change="onPageChange">
        <template #bodyCell="{ column, record }"><template v-if="column.key === 'user'"><div class="user-cell"><a-avatar :src="record.userAvatar" :size="34">{{ (record.userName || '用').slice(0,1) }}</a-avatar><div><strong>{{ record.userName || '未设置昵称' }}</strong><small>{{ record.userAccount }}</small></div></div></template><template v-else-if="column.key === 'role'"><a-tag :color="record.userRole === 'banned' ? 'red' : record.userRole === 'admin' ? 'blue' : 'green'">{{ record.userRole === 'banned' ? '已封禁' : record.userRole === 'admin' ? '管理员' : '正常' }}</a-tag></template><template v-else-if="column.key === 'date'">{{ formatDate(record.createTime) }}</template><template v-else-if="column.key === 'actions'"><a-popconfirm v-if="record.userRole !== 'admin'" :title="record.userRole === 'banned' ? '确认恢复此账号？' : '确认封禁此账号？'" @confirm="toggleBan(record)"><a-button size="small" :danger="record.userRole !== 'banned'">{{ record.userRole === 'banned' ? '恢复' : '封禁' }}</a-button></a-popconfirm><span v-else class="muted">—</span></template></template>
      </a-table>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listUserVoByPageUsingPost } from '@/api/userController'
import { banUser } from '@/api/adminConsole'
const loading = ref(false); const users = ref<any[]>([]); const total = ref(0)
const query = reactive({ current: 1, pageSize: 10, userAccount: '', userName: '' })
const columns = [{ title: '用户', key: 'user', width: 270 }, { title: '简介', dataIndex: 'userProfile', ellipsis: true }, { title: '状态', key: 'role', width: 110 }, { title: '注册时间', key: 'date', width: 145 }, { title: '操作', key: 'actions', width: 90 }]
function formatDate(value: string) { return value ? new Date(value).toLocaleDateString('zh-CN') : '' }
async function load() { loading.value = true; try { const res: any = await listUserVoByPageUsingPost({ ...query }); if (res.data.code !== 0) throw new Error(res.data.message); users.value = res.data.data?.records || []; total.value = Number(res.data.data?.total || 0) } catch (e: any) { message.error(e?.message || '加载失败') } finally { loading.value = false } }
function search() { query.current = 1; void load() }
function reset() { query.userAccount = ''; query.userName = ''; search() }
function onPageChange(p: any) { query.current = p.current; void load() }
async function toggleBan(user: any) { const banned = user.userRole !== 'banned'; try { const res: any = await banUser({ id: user.id, banned }); if (res.data.code !== 0) throw new Error(res.data.message); message.success(banned ? '账号已封禁' : '账号已恢复'); await load() } catch (e: any) { message.error(e?.message || '操作失败') } }
onMounted(load)
</script>
<style scoped>
.page-title { margin-bottom:24px; }.page-title span { color:#8c9992; font-size:11px; letter-spacing:.14em; }.page-title h1 { margin:5px 0; font-size:26px; color:#25312a; }.page-title p { margin:0; color:#929d95; font-size:13px; }.panel { padding:20px; background:#fff; border:1px solid #e7ebe7; border-radius:8px; }.filters { display:flex; flex-wrap:wrap; gap:10px; margin-bottom:18px; }.user-cell { display:flex; align-items:center; gap:10px; }.user-cell div { display:flex; flex-direction:column; min-width:0; }.user-cell strong { color:#2c382e; font-size:13px; }.user-cell small { color:#9aa39b; overflow:hidden; text-overflow:ellipsis; }.muted { color:#a4aaa5; }
</style>
