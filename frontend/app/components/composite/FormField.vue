<script setup lang="ts">
/**
 * FormField 복합 컴포넌트
 *
 * 라벨 + 입력 필드 + 에러 메시지를 포함한 완전한 폼 필드
 */

interface Props {
  /** 필드 라벨 */
  label?: string
  /** 필수 입력 표시 */
  required?: boolean
  /** 에러 메시지 */
  error?: string
  /** 도움말 텍스트 */
  help?: string
}

defineProps<Props>()
</script>

<template>
  <div class="flex flex-col gap-2">
    <label
      v-if="label"
      class="font-medium"
    >
      {{ label }}
      <span
        v-if="required"
        class="text-red-500"
      >*</span>
    </label>

    <slot />

    <small
      v-if="help && !error"
      class="text-gray-500"
    >
      {{ help }}
    </small>

    <small
      v-if="error"
      class="text-red-500"
    >
      {{ error }}
    </small>
  </div>
</template>
