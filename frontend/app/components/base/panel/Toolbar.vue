<script setup lang="ts">
import Toolbar from 'primevue/toolbar'
import type { ToolbarProps } from 'primevue/toolbar'

interface Props extends /* @vue-ignore */ ToolbarProps {
  /** 스타일 클래스 */
  class?: string
}

defineProps<Props>()
</script>

<template>
  <Toolbar
    v-bind="$attrs"
    :class="$props.class"
  >
    <template
      v-if="$slots.start"
      #start
    >
      <slot name="start" />
    </template>
    <template
      v-if="$slots.center"
      #center
    >
      <slot name="center" />
    </template>
    <template
      v-if="$slots.end"
      #end
    >
      <slot name="end" />
    </template>
  </Toolbar>
</template>
