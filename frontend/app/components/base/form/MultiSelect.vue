<script setup lang="ts" generic="T">
import MultiSelect from 'primevue/multiselect'
import type { MultiSelectProps } from 'primevue/multiselect'

interface Props extends /* @vue-ignore */ Omit<MultiSelectProps, 'options' | 'modelValue'> {
  /** 선택된 값 배열 */
  modelValue?: T[] | null
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
  /** 최대 표시 라벨 수 */
  maxSelectedLabels?: number
  /** 표시 모드 */
  display?: 'comma' | 'chip'
  /** 선택 제한 */
  selectionLimit?: number
  /** 전체 선택 체크박스 표시 */
  selectAll?: boolean
}

withDefaults(defineProps<Props>(), {
  options: () => [],
  maxSelectedLabels: 3,
  display: 'comma',
})

defineEmits<{
  'update:modelValue': [value: T[] | null]
}>()
</script>

<template>
  <MultiSelect
    :model-value="modelValue"
    :options="options"
    :option-label="optionLabel"
    :option-value="optionValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :invalid="invalid"
    :fluid="fluid"
    :filter="filter"
    :max-selected-labels="maxSelectedLabels"
    :display="display"
    :selection-limit="selectionLimit"
    :select-all="selectAll"
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
    <template
      v-if="$slots.chip"
      #chip="slotProps"
    >
      <slot
        name="chip"
        v-bind="slotProps"
      />
    </template>
  </MultiSelect>
</template>
