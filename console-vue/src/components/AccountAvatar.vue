<template>
  <el-avatar class="account-avatar" :size="size" :src="src || undefined" aria-hidden="true">
    {{ initials }}
  </el-avatar>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  username: String,
  src: String,
  size: { type: Number, default: 28 }
})

const initials = computed(() => Array.from(props.username || '').slice(0, 2).join('').toUpperCase())
</script>

<style scoped>
.account-avatar {
  flex: none;
  background: linear-gradient(140deg, #203657 80%, #4e9ceb 80%);
  color: white;
  font-weight: 600;
}
</style>
