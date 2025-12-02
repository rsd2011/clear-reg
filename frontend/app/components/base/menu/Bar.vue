<script setup lang="ts">
import Menubar from 'primevue/menubar'
import type { MenubarProps } from 'primevue/menubar'

interface MenuItem {
  label?: string
  icon?: string
  items?: MenuItem[]
  url?: string
  to?: string
  command?: () => void
  separator?: boolean
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ Omit<MenubarProps, 'model'> {
  /** 메뉴 아이템 */
  model?: MenuItem[]
}

withDefaults(defineProps<Props>(), {
  model: () => [],
})
</script>

<template>
  <Menubar
    :model="model"
    v-bind="$attrs"
  >
    <template
      v-if="$slots.start"
      #start
    >
      <slot name="start" />
    </template>
    <template
      v-if="$slots.end"
      #end
    >
      <slot name="end" />
    </template>
    <template
      v-if="$slots.item"
      #item="slotProps"
    >
      <slot
        name="item"
        v-bind="slotProps"
      />
    </template>
  </Menubar>
</template>
