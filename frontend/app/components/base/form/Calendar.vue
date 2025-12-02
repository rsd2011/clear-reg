<script setup lang="ts">
import DatePicker from 'primevue/datepicker'
import type { DatePickerProps } from 'primevue/datepicker'

interface Props extends /* @vue-ignore */ DatePickerProps {
  /** 선택된 날짜 */
  modelValue?: Date | Date[] | null
  /** placeholder 텍스트 */
  placeholder?: string
  /** 비활성화 */
  disabled?: boolean
  /** 읽기 전용 */
  isReadonly?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** 선택 모드 */
  selectionMode?: 'single' | 'multiple' | 'range'
  /** 시간 표시 */
  showTime?: boolean
  /** 시간만 표시 */
  timeOnly?: boolean
  /** 날짜 포맷 */
  dateFormat?: string
  /** 인라인 모드 */
  inline?: boolean
  /** 아이콘 표시 */
  showIcon?: boolean
  /** 오늘 버튼 표시 */
  showButtonBar?: boolean
  /** 최소 날짜 */
  minDate?: Date
  /** 최대 날짜 */
  maxDate?: Date
  /** 비활성화할 날짜들 */
  disabledDates?: Date[]
  /** 비활성화할 요일 (0=일, 6=토) */
  disabledDays?: number[]
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectionMode: 'single',
  dateFormat: 'yy-mm-dd',
})

defineEmits<{
  'update:modelValue': [value: Date | Date[] | null]
}>()
</script>

<template>
  <DatePicker
    :model-value="props.modelValue"
    :placeholder="props.placeholder"
    :disabled="props.disabled"
    :readonly="props.isReadonly"
    :invalid="props.invalid"
    :selection-mode="props.selectionMode"
    :show-time="props.showTime"
    :time-only="props.timeOnly"
    :date-format="props.dateFormat"
    :inline="props.inline"
    :show-icon="props.showIcon"
    :show-button-bar="props.showButtonBar"
    :min-date="props.minDate"
    :max-date="props.maxDate"
    :disabled-dates="props.disabledDates"
    :disabled-days="props.disabledDays"
    :fluid="props.fluid"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event as Date | Date[] | null)"
  />
</template>
