<script setup lang="ts">
/**
 * ThemeActions - 하단 액션 버튼 섹션
 */
import Button from 'primevue/button'

defineEmits<{
  reset: []
  export: []
}>()
</script>

<template>
  <section class="flex gap-2">
    <Button
      label="설정 초기화"
      icon="pi pi-refresh"
      severity="secondary"
      variant="outlined"
      size="small"
      class="flex-1"
      @click="$emit('reset')"
    />
    <Button
      label="내보내기"
      icon="pi pi-download"
      severity="secondary"
      variant="outlined"
      size="small"
      class="flex-1"
      @click="$emit('export')"
    />
  </section>
</template>
