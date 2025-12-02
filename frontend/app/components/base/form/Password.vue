<script setup lang="ts">
import Password from 'primevue/password'
import type { PasswordProps } from 'primevue/password'

interface Props extends /* @vue-ignore */ PasswordProps {
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
  /** 강도 피드백 표시 */
  feedback?: boolean
  /** 토글 마스크 버튼 표시 */
  toggleMask?: boolean
  /** 미터 표시 */
  showMeter?: boolean
  /** input ID */
  inputId?: string
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  feedback: true,
  toggleMask: true,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function handleInput(value: string) {
  emit('update:modelValue', value)
}
</script>

<template>
  <Password
    :model-value="props.modelValue ?? ''"
    :placeholder="props.placeholder"
    :disabled="props.disabled"
    :readonly="props.isReadonly"
    :invalid="props.invalid"
    :feedback="props.feedback"
    :toggle-mask="props.toggleMask"
    :input-id="props.inputId"
    :fluid="props.fluid"
    v-bind="$attrs"
    @update:model-value="handleInput"
  >
    <template
      v-if="$slots.header"
      #header
    >
      <slot name="header" />
    </template>
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </Password>
</template>
