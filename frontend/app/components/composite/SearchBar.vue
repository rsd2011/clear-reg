<script setup lang="ts">
/**
 * SearchBar 복합 컴포넌트
 *
 * FormInput + ActionButton 조합으로 검색 입력 필드 구현
 */

interface Props {
  /** 검색어 */
  modelValue?: string
  /** 플레이스홀더 */
  placeholder?: string
  /** 로딩 상태 */
  loading?: boolean
  /** 비활성화 */
  disabled?: boolean
  /** 버튼 라벨 */
  buttonLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '검색...',
  buttonLabel: '검색',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'search': [query: string]
}>()

function handleSearch() {
  emit('search', props.modelValue ?? '')
}
</script>

<template>
  <div class="flex gap-2">
    <FormInput
      :model-value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled || loading"
      fluid
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="handleSearch"
    />
    <ActionButton
      :label="buttonLabel"
      :loading="loading"
      :disabled="disabled"
      icon="pi pi-search"
      @click="handleSearch"
    />
  </div>
</template>
