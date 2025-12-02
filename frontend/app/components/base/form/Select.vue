<script setup lang="ts" generic="T">
import Select from 'primevue/select'
import type { SelectProps } from 'primevue/select'

interface Props extends /* @vue-ignore */ Omit<SelectProps, 'options' | 'modelValue'> {
  /** 선택된 값 */
  modelValue?: T | null
  /** 옵션 목록 */
  options?: T[]
  /** 옵션 라벨 필드명 */
  optionLabel?: string
  /** 옵션 값 필드명 */
  optionValue?: string
  /** placeholder 텍스트 */
  placeholder?: string
  /** 비활성화 */
  disabled?: boolean
  /** 유효하지 않은 상태 */
  invalid?: boolean
  /** fluid 모드 (전체 너비) */
  fluid?: boolean
  /** 필터링 가능 여부 */
  filter?: boolean
  /** 선택 취소 가능 여부 */
  showClear?: boolean
}

withDefaults(defineProps<Props>(), {
  options: () => [],
})

defineEmits<{
  'update:modelValue': [value: T | null]
}>()
</script>

<template>
  <Select
    :model-value="modelValue"
    :options="options"
    :option-label="optionLabel"
    :option-value="optionValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :invalid="invalid"
    :fluid="fluid"
    :filter="filter"
    :show-clear="showClear"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template
      v-if="$slots.value"
      #value="slotProps"
    >
      <slot
        name="value"
        v-bind="slotProps"
      />
    </template>
    <template
      v-if="$slots.option"
      #option="slotProps"
    >
      <slot
        name="option"
        v-bind="slotProps"
      />
    </template>
  </Select>
</template>
