<script setup lang="ts">
import ToggleSwitch from 'primevue/toggleswitch'
import type { ToggleSwitchProps } from 'primevue/toggleswitch'

interface Props extends /* @vue-ignore */ ToggleSwitchProps {
  /** 스위치 상태 */
  modelValue?: boolean
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  readonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** input ID */
  inputId?: string
  /** 라벨 텍스트 */
  label?: string
}

const props = defineProps<Props>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>

<template>
  <div class="flex items-center gap-2">
    <ToggleSwitch
      :model-value="props.modelValue"
      :disabled="props.disabled"
      :readonly="props.readonly"
      :invalid="props.invalid"
      :input-id="props.inputId"
      v-bind="$attrs"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <label
      v-if="props.label"
      :for="props.inputId"
      class="cursor-pointer"
    >{{ props.label }}</label>
  </div>
</template>
