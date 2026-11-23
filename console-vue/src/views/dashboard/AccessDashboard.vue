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
          <el-button :disabled="groupLoading || loading || !gid || !!error || !!rankError" @click="exportReport">导出统计</el-button>
        </div>
      </header>
      <div v-if="error" class="notice panel" role="alert">
        {{ error }} <el-button link type="primary" @click="retry">重试</el-button>
      </div>
      <div v-else-if="!groupLoading && !groups.length" class="notice panel">暂无短链分组</div>
      <div v-else class="report" v-loading="groupLoading || loading">
        <div class="metrics">
          <div v-for="[key, title] in metrics" :key="key" class="panel metric">
            <span>{{ title }}</span><strong>{{ loading || groupLoading ? '—' : format(stats?.[key]) }}</strong>
          </div>
        </div>
        <div class="charts">
          <section class="panel"><h2>访问趋势</h2>
            <DashboardChart v-if="stats?.daily?.length" :option="trend" label="按日期统计的访问次数折线图"
              @marker-click="peakDate = $event" />
            <p v-else class="empty">该时段暂无访问记录</p>
            <div v-if="spikes.length" class="peak-days">
              <span title="有短链的访问次数较前一天至少增加 10 次，且达到前一天的两倍">流量突增</span>
              <el-button v-for="day in spikes" :key="day.date" link type="primary"
                @click="peakDate = day.date">{{ day.date.slice(5) }} · {{ day.pv }} 次</el-button>
            </div>
            <div v-if="peakDate" class="contributors" aria-live="polite">
              <strong>{{ peakDate }} 当日贡献</strong>
              <el-button link @click="peakDate = ''">收起</el-button>
              <p v-if="peakLoading">正在查询…</p>
              <p v-else-if="peakError">{{ peakError }} <el-button link @click="peakRetry++">重试</el-button></p>
              <ol v-else-if="contributors.length"><li v-for="item in contributors" :key="item.fullShortUrl">
                {{ item.title || item.fullShortUrl }} · {{ format(item.pv) }} 次
              </li></ol>
              <p v-else>当天暂无访问记录</p>
            </div>
          </section>
          <section class="panel"><h2>设备分布</h2>
            <DashboardChart v-if="devices.length" :option="deviceChart" label="各类设备访问占比" />
            <p v-else class="empty">该时段暂无设备数据</p>
          </section>
        </div>
        <section class="panel"><h2>热门短链 <small>按访问次数取前 5 条</small></h2>
          <p v-if="rankError" role="alert">{{ rankError }} <el-button link @click="retry">重试</el-button></p>
          <el-table v-else :data="ranking" size="small" empty-text="该时段暂无访问记录" @row-click="detail = $event">
            <el-table-column type="index" label="排名" width="54" />
            <el-table-column prop="title" label="短链名称" min-width="130" show-overflow-tooltip />
            <el-table-column prop="fullShortUrl" label="短链地址" min-width="150" show-overflow-tooltip />
            <el-table-column prop="pv" label="访问次数" width="84" align="right" />
            <el-table-column label="操作" width="92" align="center">
              <template #default="{ row }"><el-button link type="primary" @click.stop="detail = row">查看统计</el-button></template>
            </el-table-column>
          </el-table>
        </section>
        <el-collapse v-model="expanded" class="panel details">
          <el-collapse-item title="访问时段" name="hours">
            <DashboardChart v-if="stats?.hourStats?.length && expanded.includes('hours')" :option="hourChart" label="按小时统计的访问次数柱状图" />
            <p v-else class="pending">该时段暂无访问记录</p>
          </el-collapse-item>
          <el-collapse-item title="地区分布" name="regions">
            <el-table v-if="regions.length" :data="regions" size="small">
              <el-table-column prop="locale" label="省份" />
              <el-table-column prop="cnt" label="访问次数" align="right" />
            </el-table>
            <p v-else class="pending">该时段暂无地区数据</p>
          </el-collapse-item>
        </el-collapse>
      </div>
      <RankedLinkDetail v-if="detail" :key="detail.fullShortUrl" :link="detail" :gid="gid" :range="range" @closed="detail = null" />
    </section>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, inject, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import groupApi from '@/api/modules/group'
import { getUsername } from '@/core/auth'
import DashboardChart from './DashboardChart.vue'
import { reportCsv } from './report'

const RankedLinkDetail = defineAsyncComponent(() => import('./RankedLinkDetail.vue'))
const detail = ref(null)
const ranking = ref([])
const rankError = ref('')
const peakDate = ref('')
const peakRetry = ref(0)
const peakLoading = ref(false)
const peakError = ref('')
const contributors = ref([])

const gid = inject('activeLinkGroup')
const username = getUsername()
const groups = ref([])
const groupLoading = ref(true)
const loading = ref(false)
const error = ref('')
const stats = ref(null)
const expanded = ref([])
const period = ref('7')
const refresh = ref(0)
const metrics = [['pv', '访问次数（PV）'], ['uv', '独立访客（UV）'], ['uip', '独立 IP 数']]
const format = value => Number(value || 0).toLocaleString('zh-CN')
const isFuture = date => dayjs(date).isAfter(dayjs(), 'day')
function recent(days) {
  const today = dayjs()
  return [today.subtract(days - 1, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
}
const customRange = ref(recent(7))
const range = computed(() => period.value === 'custom' ? customRange.value : recent(Number(period.value)))

async function loadRanking(group, dates) {
  const { data } = await groupApi.queryGroupRanking({
    gid: group, startDate: dates[0] + ' 00:00:00', endDate: dates[1] + ' 23:59:59'
  })
  if (!data.success) throw Error(data.message || '无法加载短链排行')
  return data.data || []
}

watch([peakDate, peakRetry], async ([date], _, onCleanup) => {
  if (!date) return
  let active = true
  onCleanup(() => { active = false })
  peakLoading.value = true
  peakError.value = ''
  try {
    const rows = await loadRanking(gid.value, [date, date])
    if (active) contributors.value = rows
  } catch (e) {
    if (active) peakError.value = e.message || '无法加载当日贡献'
  } finally {
    if (active) peakLoading.value = false
  }
})
function exportReport() {
  const group = groups.value.find(item => item.gid === gid.value).name
  const csv = reportCsv(group, range.value, stats.value?.daily || [], ranking.value)
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = [group, ...range.value, '访问统计.csv'].join('_')
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

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
function retry() {
  if (groups.value.length) refresh.value++
  else loadGroups()
}
watch([gid, range, groupLoading, refresh], async ([group, dates, waiting], _, onCleanup) => {
  if (waiting || !groups.value.length || !group || !dates?.length) return
  let active = true
  onCleanup(() => { active = false })
  loading.value = true
  error.value = ''
  stats.value = null
  ranking.value = []
  rankError.value = ''
  detail.value = null
  peakDate.value = ''
  const rankRequest = loadRanking(group, dates).then(rows => {
    if (active) ranking.value = rows
  }).catch(e => {
    if (active) rankError.value = e.message || '无法加载短链排行'
  })
  try {
    const { data } = await groupApi.queryGroupStats({
      gid: group, startDate: dates[0] + ' 00:00:00', endDate: dates[1] + ' 23:59:59'
    })
    if (!data.success) throw new Error(data.message || '无法加载访问统计')
    if (active) stats.value = data.data
  } catch (e) {
    if (active) error.value = e.message || '无法加载访问统计'
  } finally {
    await rankRequest
    if (active) loading.value = false
  }
})

const blue = '#2c85ec'
function timeChart(labels, values, type) {
  return {
    color: [blue], tooltip: { trigger: 'axis', confine: true },
    grid: { top: 16, left: 48, right: 24, bottom: 28 },
    xAxis: { type: 'category', data: labels, boundaryGap: type === 'bar', axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#ebeef5' } } },
    series: [{ name: '访问次数', type, data: values, barMaxWidth: 22,
      areaStyle: type === 'line' ? { opacity: 0.08 } : undefined }]
  }
}
const spikes = computed(() => {
  const dates = new Set(stats.value?.spikeDates || [])
  return (stats.value?.daily || []).filter(day => dates.has(day.date))
})
const trend = computed(() => {
  const daily = stats.value?.daily || []
  const option = timeChart(daily.map(day => day.date), daily.map(day => day.pv), 'line')
  option.xAxis.axisLabel.formatter = date => date.slice(5)
  option.series[0].markPoint = {
    symbol: 'circle', symbolSize: 13, label: { show: false }, itemStyle: { color: '#df891b' },
    data: spikes.value.map(day => ({ name: day.date, coord: [day.date, day.pv], value: day.pv }))
  }
  return option
})
const hourChart = computed(() => {
  const hours = stats.value?.hourStats || []
  return timeChart(hours.map((_, i) => i + '时'), hours, 'bar')
})
const devices = computed(() => (stats.value?.deviceStats || []).filter(item => item.cnt > 0).map(item => ({
  name: ({ PC: '电脑', Mobile: '手机' })[item.device] || item.device, value: item.cnt
})))
const deviceChart = computed(() => ({
  color: [blue, '#86bcfa', '#a8b2c0'], tooltip: { trigger: 'item', confine: true },
  legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '62%'], center: ['50%', '43%'],
    label: { formatter: '{d}%' }, data: devices.value }]
}))
const regions = computed(() => [...(stats.value?.localeCnStats || [])].sort((a, b) => b.cnt - a.cnt))
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
h2 small, .peak-days { font-size: 12px; font-weight: 400; color: #606266; }
.peak-days { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.contributors { margin-top: 8px; padding: 8px 12px; background: #f7f8fa; font-size: 13px; }
.contributors ol { padding-left: 18px; }
.contributors li { list-style: decimal; }
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
