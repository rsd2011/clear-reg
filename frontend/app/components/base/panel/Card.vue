<script setup lang="ts">
import Card from 'primevue/card'

interface Props {
  /** 카드 제목 */
  title?: string
  /** 카드 부제목 */
  subtitle?: string
}

defineProps<Props>()
</script>

<template>
  <Card v-bind="$attrs">
    <template
      v-if="title || $slots.title"
      #title
    >
      <slot name="title">
        {{ title }}
      </slot>
    </template>
    <template
      v-if="subtitle || $slots.subtitle"
      #subtitle
    >
      <slot name="subtitle">
        {{ subtitle }}
      </slot>
    </template>
    <template
      v-if="$slots.header"
      #header
    >
      <slot name="header" />
    </template>
    <template #content>
      <slot />
    </template>
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </Card>
</template>
