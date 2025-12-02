<script setup lang="ts">
import Breadcrumb from 'primevue/breadcrumb'
import type { BreadcrumbProps } from 'primevue/breadcrumb'

interface BreadcrumbItem {
  label?: string
  icon?: string
  url?: string
  to?: string
  command?: () => void
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ Omit<BreadcrumbProps, 'model' | 'home'> {
  /** 브레드크럼 아이템 */
  model?: BreadcrumbItem[]
  /** 홈 아이템 */
  home?: BreadcrumbItem
}

withDefaults(defineProps<Props>(), {
  model: () => [],
})
</script>

<template>
  <Breadcrumb
    :model="model"
    :home="home"
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
    <template
      v-if="$slots.separator"
      #separator
    >
      <slot name="separator" />
    </template>
  </Breadcrumb>
</template>
