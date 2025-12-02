<script setup lang="ts">
import Tag from 'primevue/tag'
import type { TagProps } from 'primevue/tag'

interface Props extends /* @vue-ignore */ TagProps {
  /** 태그 내용 */
  value?: string
  /** 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'
  /** 아이콘 */
  icon?: string
  /** 둥근 모서리 */
  rounded?: boolean
}

withDefaults(defineProps<Props>(), {
  severity: 'secondary',
})
</script>

<template>
  <Tag
    :value="value"
    :severity="severity"
    :icon="icon"
    :rounded="rounded"
    v-bind="$attrs"
  >
    <slot />
  </Tag>
</template>
