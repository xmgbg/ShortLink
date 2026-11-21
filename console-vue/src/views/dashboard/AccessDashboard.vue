<template>
  <div class="dashboard">
    <aside aria-label="短链分组">
      <div class="group-title">短链分组 <small>共 {{ groups.length }} 组</small></div>
      <button v-for="group in groups" :key="group.gid" :aria-pressed="group.gid === gid"
        @click="gid = group.gid"><span>{{ group.name }}</span><span>{{ group.shortLinkCount ?? 0 }}</span></button>
    </aside>
    <section class="workspace">
      <header class="toolbar panel">
        <div class="heading"><h1>访问看板</h1><span>欢迎您，{{ username }}！</span></div>
        <div class="filters">
          <el-select v-model="gid" aria-label="选择分组" placeholder="选择分组" :disabled="!groups.length">
            <el-option v-for="group in groups" :key="group.gid" :value="group.gid" :label="group.name" />
          </el-select>
          <el-radio-group v-model="period" aria-label="统计周期">
            <el-radio-button label="7">近 7 天</el-radio-button>
            <el-radio-button label="30">近 30 天</el-radio-button>
            <el-radio-button label="custom">自定义日期</el-radio-button>
          </el-radio-group>
          <el-date-picker v-if="period === 'custom'" v-model="customRange" type="daterange"
            value-format="YYYY-MM-DD" :clearable="false" :disabled-date="isFuture"
            start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" />
        </div>
      </header>
      <div v-if="error" class="notice panel" role="alert">
        {{ error }} <el-button link type="primary" @click="loadGroups">重试</el-button>
      </div>
      <div v-else-if="!groupLoading && !groups.length" class="notice panel">暂无短链分组</div>
      <div v-else class="report" v-loading="groupLoading">
        <div class="metrics">
          <div v-for="title in metrics" :key="title" class="panel metric">
            <span>{{ title }}</span><strong>—</strong>
          </div>
        </div>
        <div class="charts">
          <section class="panel"><h2>访问趋势</h2>
            <p class="empty">该时段暂无访问记录</p>
          </section>
          <section class="panel"><h2>设备分布</h2>
            <p class="empty">该时段暂无设备数据</p>
          </section>
        </div>
        <section class="panel"><h2>热门短链</h2>
          <div class="rank-head"><span>短链名称</span><span>短链地址</span><span>访问次数</span></div>
          <p class="pending">所选时段的排行暂未开放</p>
        </section>
        <el-collapse v-model="expanded" class="panel details">
          <el-collapse-item title="访问时段" name="hours">
            <p class="pending">该时段暂无访问记录</p>
          </el-collapse-item>
          <el-collapse-item title="地区分布" name="regions">
            <p class="pending">该时段暂无地区数据</p>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import groupApi from '@/api/modules/group'
import { getUsername } from '@/core/auth'

const gid = inject('activeLinkGroup')
const username = getUsername()
const groups = ref([])
const groupLoading = ref(true)
const error = ref('')
const expanded = ref([])
const period = ref('7')
const metrics = ['访问次数（PV）', '独立访客（UV）', '独立 IP 数']
const isFuture = date => dayjs(date).isAfter(dayjs(), 'day')
function recent(days) {
  const today = dayjs()
  return [today.subtract(days - 1, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
}
const customRange = ref(recent(7))

async function loadGroups() {
  groupLoading.value = true
  error.value = ''
  try {
    const { data } = await groupApi.queryGroup()
    if (!data.success) throw new Error(data.message || '无法加载分组')
    groups.value = [...data.data].reverse()
    if (!groups.value.some(group => group.gid === gid.value)) gid.value = groups.value[0]?.gid || ''
  } catch (e) {
    error.value = e.message || '无法加载分组'
  } finally {
    groupLoading.value = false
  }
}
onMounted(loadGroups)
</script>

<style scoped>
.dashboard { display: flex; min-height: 100%; background: #eef0f5; color: #303133; }
aside { flex: 0 0 190px; background: white; border-right: 1px solid #dcdfe6; }
.group-title { padding: 16px 10px; border-bottom: 1px solid #ebeef5; font-size: 15px; font-weight: 600; }
.group-title small { font-size: 12px; font-weight: normal; }
aside button { display: flex; justify-content: space-between; width: 100%; padding: 12px; gap: 6px; border: 0; background: white; color: inherit; cursor: pointer; }
aside button span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
aside button[aria-pressed="true"] { color: #3464e0; background: #ebeffa; font-weight: 600; }
.workspace { min-width: 0; flex: 1; padding: 16px; }
.panel { background: white; border: 1px solid #e4e7ed; border-radius: 4px; padding: 14px 16px; }
.toolbar, .filters, .heading { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.toolbar { justify-content: space-between; margin-bottom: 12px; }
.heading span, .metric span, .empty, .pending { font-size: 13px; color: #606266; }
h1 { font-size: 19px; } h2 { font-size: 15px; margin: 0 0 12px; }
.filters .el-select { width: 150px; }
.report { display: grid; gap: 12px; }
.metrics, .charts { display: grid; gap: 12px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.metric strong { display: block; font-size: 23px; color: #2c85ec; font-variant-numeric: tabular-nums; }
.charts { grid-template-columns: minmax(0, 2.3fr) minmax(220px, 1fr); }
.empty, .pending { text-align: center; padding: 18px 0; }
.empty { height: 216px; align-content: center; }
.rank-head { display: grid; grid-template-columns: 1fr 1fr auto; background: #f7f8fa; padding: 7px 12px; font-size: 13px; }
.details { padding-block: 0; }
@media (max-width: 760px) {
  aside { display: none; }
  .workspace { padding: 10px; }
  .charts { grid-template-columns: minmax(0, 1fr); }
  .metrics { gap: 6px; }
  .metric { padding: 10px; }
  .filters { max-width: 100%; }
  .filters :deep(.el-date-editor) { max-width: 100%; }
}
</style>
