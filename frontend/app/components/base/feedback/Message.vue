<script setup lang="ts">
import Message from 'primevue/message'
import type { MessageProps } from 'primevue/message'

interface Props extends /* @vue-ignore */ MessageProps {
  /** 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'error' | 'contrast'
  /** 닫기 버튼 표시 */
  closable?: boolean
  /** 고정 표시 (sticky) */
  sticky?: boolean
  /** 수명 (ms) */
  life?: number
  /** 아이콘 */
  icon?: string
}

withDefaults(defineProps<Props>(), {
  severity: 'info',
  closable: true,
  life: 3000,
})

defineEmits<{
  close: []
}>()
</script>

<template>
  <Message
    :severity="severity"
    :closable="closable"
    :sticky="sticky"
    :life="life"
    :icon="icon"
    v-bind="$attrs"
    @close="$emit('close')"
  >
    <slot />
  </Message>
</template>
