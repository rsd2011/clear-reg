<script setup lang="ts">
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import type { AccordionProps } from 'primevue/accordion'

interface AccordionItem {
  value: string
  header: string
  content?: string
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ AccordionProps {
  /** 활성화된 패널 값 */
  value?: string | string[] | null
  /** 다중 선택 허용 */
  multiple?: boolean
  /** 아코디언 아이템 */
  items?: AccordionItem[]
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
})

defineEmits<{
  'update:value': [value: string | string[] | null]
}>()
</script>

<template>
  <Accordion
    :value="props.value ?? null"
    :multiple="props.multiple"
    v-bind="$attrs"
    @update:value="$emit('update:value', $event ?? null)"
  >
    <AccordionPanel
      v-for="item in props.items"
      :key="item.value"
      :value="item.value"
      :disabled="item.disabled"
    >
      <AccordionHeader>{{ item.header }}</AccordionHeader>
      <AccordionContent>
        <slot :name="item.value">
          {{ item.content }}
        </slot>
      </AccordionContent>
    </AccordionPanel>
    <slot />
  </Accordion>
</template>
