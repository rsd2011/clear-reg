<script setup lang="ts">
import { DockviewVue } from 'dockview-vue'
import type { DockviewReadyEvent } from 'dockview-core'

interface Props {
  // DockView 기본 props 전달
  [key: string]: unknown
}

const props = defineProps<Props>()
const emit = defineEmits<{
  ready: [event: DockviewReadyEvent]
}>()

function handleReady(event: DockviewReadyEvent) {
  emit('ready', event)
}
</script>

<template>
  <div class="themable-dockview-wrapper">
    <DockviewVue
      v-bind="props"
      class="dockview-theme-enterman"
      @ready="handleReady"
    >
      <slot />
    </DockviewVue>
  </div>
</template>

<style scoped>
.themable-dockview-wrapper {
  width: 100%;
  height: 100%;
}

/* 테마 전환 트랜지션 */
.dockview-theme-enterman {
  transition: background-color 0.3s ease, border-radius 0.3s ease;
}
</style>
