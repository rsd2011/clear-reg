<script setup lang="ts">
import Textarea from 'primevue/textarea'
import type { TextareaProps } from 'primevue/textarea'

interface Props extends /* @vue-ignore */ TextareaProps {
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
  /** 행 수 */
  rows?: number
  /** 열 수 */
  cols?: number
  /** 자동 크기 조절 */
  autoResize?: boolean
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  rows: 3,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <Textarea
    :model-value="props.modelValue ?? ''"
    :placeholder="props.placeholder"
    :disabled="props.disabled"
    :readonly="props.isReadonly"
    :invalid="props.invalid"
    :rows="props.rows"
    :cols="props.cols"
    :auto-resize="props.autoResize"
    :fluid="props.fluid"
    v-bind="$attrs"
    @input="handleInput"
  />
</template>
