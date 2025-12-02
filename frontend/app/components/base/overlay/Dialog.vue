<script setup lang="ts">
import Dialog from 'primevue/dialog'
import type { DialogProps } from 'primevue/dialog'

interface Props extends /* @vue-ignore */ DialogProps {
  /** 다이얼로그 표시 여부 */
  visible?: boolean
  /** 다이얼로그 헤더 제목 */
  header?: string
  /** 모달 모드 */
  modal?: boolean
  /** 닫기 버튼 표시 */
  closable?: boolean
  /** 드래그 가능 여부 */
  draggable?: boolean
  /** 다이얼로그 스타일 */
  style?: Record<string, string>
}

withDefaults(defineProps<Props>(), {
  modal: true,
  closable: true,
  draggable: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

function handleHide() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="header"
    :modal="modal"
    :closable="closable"
    :draggable="draggable"
    :style="style"
    v-bind="$attrs"
    @update:visible="$emit('update:visible', $event)"
    @hide="handleHide"
  >
    <template
      v-if="$slots.header"
      #header
    >
      <slot name="header" />
    </template>
    <slot />
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </Dialog>
</template>
