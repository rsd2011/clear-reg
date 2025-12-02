<script setup lang="ts">
import ProgressBar from 'primevue/progressbar'
import type { ProgressBarProps } from 'primevue/progressbar'

interface Props extends /* @vue-ignore */ ProgressBarProps {
  /** 진행률 (0-100) */
  value?: number
  /** 표시 모드 */
  mode?: 'determinate' | 'indeterminate'
  /** 값 표시 여부 */
  showValue?: boolean
}

withDefaults(defineProps<Props>(), {
  mode: 'determinate',
  showValue: true,
})
</script>

<template>
  <ProgressBar
    :value="value"
    :mode="mode"
    :show-value="showValue"
    v-bind="$attrs"
  />
</template>
