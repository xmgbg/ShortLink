<template>
  <div ref="container" class="chart" role="img" :aria-label="label"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ option: Object, label: String })
const container = ref(null)
let chart
let observer
onMounted(() => {
  observer = new ResizeObserver(() => {
    if (!container.value?.clientWidth || !container.value.clientHeight) return
    if (chart) chart.resize()
    else {
      chart = echarts.init(container.value)
      chart.setOption(props.option)
    }
  })
  observer.observe(container.value)
})
watch(() => props.option, option => chart?.setOption(option, true))
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.chart { height: 216px; width: 100%; }
</style>
