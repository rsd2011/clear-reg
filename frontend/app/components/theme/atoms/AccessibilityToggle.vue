<script setup lang="ts">
/**
 * AccessibilityToggle - 접근성 토글 스위치 + 라벨 조합
 */
import ToggleSwitch from 'primevue/toggleswitch'

defineProps<{
  /** 라벨 텍스트 */
  label: string
  /** 입력 ID */
  inputId: string
}>()

const modelValue = defineModel<boolean>({ required: true })
</script>

<template>
  <div class="flex items-center justify-between">
    <label
      :for="inputId"
      class="text-sm text-surface-700 dark:text-surface-200"
    >
      {{ label }}
    </label>
    <ToggleSwitch
      v-model="modelValue"
      :input-id="inputId"
    />
  </div>
</template>
