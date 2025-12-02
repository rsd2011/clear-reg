<script setup lang="ts">
import RadioButton from 'primevue/radiobutton'
import type { RadioButtonProps } from 'primevue/radiobutton'

interface Props extends /* @vue-ignore */ RadioButtonProps {
  /** 선택된 값 */
  modelValue?: unknown
  /** 이 라디오의 값 */
  value?: unknown
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  isReadonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** input ID */
  inputId?: string
  /** name 속성 (라디오 그룹) */
  name?: string
  /** 라벨 텍스트 */
  label?: string
}

const props = defineProps<Props>()

defineEmits<{
  'update:modelValue': [value: unknown]
}>()
</script>

<template>
  <div class="flex items-center gap-2">
    <RadioButton
      :model-value="props.modelValue"
      :value="props.value"
      :disabled="props.disabled"
      :readonly="props.isReadonly"
      :invalid="props.invalid"
      :input-id="props.inputId"
      :name="props.name"
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
