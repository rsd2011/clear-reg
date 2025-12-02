<script setup lang="ts">
import ConfirmDialog from 'primevue/confirmdialog'
import type { ConfirmDialogProps } from 'primevue/confirmdialog'

interface Props extends /* @vue-ignore */ ConfirmDialogProps {
  /** 그룹 식별자 */
  group?: string
}

defineProps<Props>()
</script>

<template>
  <ConfirmDialog
    :group="group"
    v-bind="$attrs"
  >
    <template
      v-if="$slots.message"
      #message="slotProps"
    >
      <slot
        name="message"
        v-bind="slotProps"
      />
    </template>
    <template
      v-if="$slots.icon"
      #icon
    >
      <slot name="icon" />
    </template>
  </ConfirmDialog>
</template>
