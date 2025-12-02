<script setup lang="ts">
import InputNumber from 'primevue/inputnumber'
import type { InputNumberProps } from 'primevue/inputnumber'

interface Props extends /* @vue-ignore */ InputNumberProps {
  /** 입력값 */
  modelValue?: number | null
  /** placeholder 텍스트 */
  placeholder?: string
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  isReadonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** 모드 (decimal, currency) */
  mode?: 'decimal' | 'currency'
  /** 통화 코드 */
  currency?: string
  /** 로케일 */
  locale?: string
  /** 최솟값 */
  min?: number
  /** 최댓값 */
  max?: number
  /** 소수점 최소 자릿수 */
  minFractionDigits?: number
  /** 소수점 최대 자릿수 */
  maxFractionDigits?: number
  /** 증감 버튼 표시 */
  showButtons?: boolean
  /** 버튼 레이아웃 */
  buttonLayout?: 'stacked' | 'horizontal' | 'vertical'
  /** 증가 단위 */
  step?: number
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
  /** 접두사 */
  prefix?: string
  /** 접미사 */
  suffix?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'decimal',
  step: 1,
})

defineEmits<{
  'update:modelValue': [value: number | null]
}>()
</script>

<template>
  <InputNumber
    :model-value="props.modelValue"
    :placeholder="props.placeholder"
    :disabled="props.disabled"
    :readonly="props.isReadonly"
    :invalid="props.invalid"
    :mode="props.mode"
    :currency="props.currency"
    :locale="props.locale"
    :min="props.min"
    :max="props.max"
    :min-fraction-digits="props.minFractionDigits"
    :max-fraction-digits="props.maxFractionDigits"
    :show-buttons="props.showButtons"
    :button-layout="props.buttonLayout"
    :step="props.step"
    :fluid="props.fluid"
    :prefix="props.prefix"
    :suffix="props.suffix"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>
