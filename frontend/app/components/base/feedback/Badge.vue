<script setup lang="ts">
import Badge from 'primevue/badge'
import type { BadgeProps } from 'primevue/badge'

interface Props extends /* @vue-ignore */ BadgeProps {
  /** 배지에 표시할 값 */
  value?: string | number
  /** 배지 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast' | null
  /** 배지 크기 */
  size?: 'small' | 'large' | 'xlarge' | null
}

defineProps<Props>()
</script>

<template>
  <Badge
    :value="value"
    :severity="severity"
    :size="size"
    v-bind="$attrs"
  />
</template>
