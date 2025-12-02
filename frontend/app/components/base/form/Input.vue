<script setup lang="ts">
import InputText from 'primevue/inputtext'
import type { InputTextProps } from 'primevue/inputtext'

interface Props extends /* @vue-ignore */ InputTextProps {
  /** 입력값 */
  modelValue?: string | null
  /** placeholder 텍스트 */
  placeholder?: string
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  isReadonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** 입력 크기 */
  size?: 'small' | 'large'
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <InputText
    :model-value="props.modelValue ?? ''"
    :placeholder="props.placeholder"
    :disabled="props.disabled"
    :readonly="props.isReadonly"
    :invalid="props.invalid"
    :size="props.size"
    :fluid="props.fluid"
    v-bind="$attrs"
    @input="handleInput"
  />
</template>
