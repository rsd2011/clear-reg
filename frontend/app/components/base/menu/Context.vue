<script setup lang="ts">
import ContextMenu from 'primevue/contextmenu'
import type { ContextMenuProps } from 'primevue/contextmenu'

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

interface Props extends /* @vue-ignore */ Omit<ContextMenuProps, 'model'> {
  /** 메뉴 아이템 */
  model?: MenuItem[]
  /** 대상 요소 참조 */
  target?: HTMLElement | string
  /** 글로벌 모드 */
  global?: boolean
}

withDefaults(defineProps<Props>(), {
  model: () => [],
})
</script>

<template>
  <ContextMenu
    :model="model"
    :target="target"
    :global="global"
    v-bind="$attrs"
  >
    <template
      v-if="$slots.item"
      #item="slotProps"
    >
      <slot
        name="item"
        v-bind="slotProps"
      />
    </template>
  </ContextMenu>
</template>
