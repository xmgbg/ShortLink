<template>
  <ChartsInfo ref="dialog" :title="link.title || link.fullShortUrl" :origin-url="link.originUrl"
    :favicon="link.favicon" :info="info" :table-info="table" @change-time="setDates"
    @change-page="page = { ...$event }" @closed="$emit('closed')" />
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import linkApi from '@/api/modules/smallLinkPage'
import ChartsInfo from '@/views/mySpace/components/chartsInfo/ChartsInfo.vue'

const props = defineProps({ link: Object, gid: String, range: Array })
defineEmits(['closed'])
const dialog = ref(null)
const dates = ref(null)
const page = ref({ current: 1, size: 10 })
const info = ref(null)
const table = ref(null)
onMounted(() => dialog.value.isVisible(props.range))
function setDates(value) {
  dates.value = value || props.range
}
function params() {
  return { gid: props.gid, fullShortUrl: props.link.fullShortUrl, enableStatus: 0,
    startDate: dates.value[0] + ' 00:00:00', endDate: dates.value[1] + ' 23:59:59' }
}
watch(dates, async (_, __, onCleanup) => {
  let active = true
  onCleanup(() => { active = false })
  info.value = null
  try {
    const { data } = await linkApi.queryLinkStats(params())
    if (!data.success) throw Error(data.message)
    if (active) info.value = data.data
  } catch (e) { if (active) ElMessage.error(e.message || '无法加载短链统计') }
})
watch([dates, page], async (_, __, onCleanup) => {
  if (!dates.value) return
  let active = true
  onCleanup(() => { active = false })
  table.value = null
  try {
    const response = await linkApi.queryLinkTable({ ...params(), ...page.value })
    if (!response.data.success) throw Error(response.data.message)
    if (active) table.value = response
  } catch (e) { if (active) ElMessage.error(e.message || '无法加载访问记录') }
})
</script>
