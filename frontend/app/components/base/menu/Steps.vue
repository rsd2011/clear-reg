<script setup lang="ts">
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist'
import Step from 'primevue/step'
import StepPanels from 'primevue/steppanels'
import StepPanel from 'primevue/steppanel'
import type { StepperProps } from 'primevue/stepper'

interface StepItem {
  value: string
  label: string
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ StepperProps {
  /** 활성화된 스텝 */
  value?: string
  /** 스텝 아이템 */
  items?: StepItem[]
  /** 선형 진행 모드 */
  linear?: boolean
}

withDefaults(defineProps<Props>(), {
  items: () => [],
  linear: true,
})

defineEmits<{
  'update:value': [value: string]
}>()
</script>

<template>
  <Stepper
    :value="value"
    :linear="linear"
    v-bind="$attrs"
    @update:value="$emit('update:value', $event)"
  >
    <StepList>
      <Step
        v-for="item in items"
        :key="item.value"
        :value="item.value"
        :disabled="item.disabled"
      >
        {{ item.label }}
      </Step>
    </StepList>
    <StepPanels>
      <StepPanel
        v-for="item in items"
        :key="item.value"
        :value="item.value"
      >
        <slot :name="item.value" />
      </StepPanel>
    </StepPanels>
  </Stepper>
</template>
