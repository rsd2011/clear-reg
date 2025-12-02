<script setup lang="ts">
import Checkbox from 'primevue/checkbox'
import type { CheckboxProps } from 'primevue/checkbox'

interface Props extends /* @vue-ignore */ CheckboxProps {
  /** 체크 상태 */
  modelValue?: boolean | unknown
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  isReadonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** 바이너리 모드 (true/false) */
  binary?: boolean
  /** 체크 시 값 */
  trueValue?: unknown
  /** 미체크 시 값 */
  falseValue?: unknown
  /** input ID */
  inputId?: string
  /** 라벨 텍스트 */
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  binary: true,
})

defineEmits<{
  'update:modelValue': [value: boolean | unknown]
}>()
</script>

<template>
  <div class="flex items-center gap-2">
    <Checkbox
      :model-value="props.modelValue"
      :disabled="props.disabled"
      :readonly="props.isReadonly"
      :invalid="props.invalid"
      :binary="props.binary"
      :true-value="props.trueValue"
      :false-value="props.falseValue"
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
