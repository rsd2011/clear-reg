<script setup lang="ts">
/**
 * ErrorBoundary 공통 컴포넌트
 *
 * 에러를 포착하고 표시하는 범용 컴포넌트
 */

interface Props {
  /** 에러 객체 */
  error?: Error | null
  /** 에러 제목 */
  title?: string
  /** 재시도 버튼 표시 */
  showRetry?: boolean
  /** 재시도 버튼 라벨 */
  retryLabel?: string
}

withDefaults(defineProps<Props>(), {
  title: '오류가 발생했습니다',
  showRetry: true,
  retryLabel: '다시 시도',
})

defineEmits<{
  retry: []
}>()
</script>

<template>
  <div
    v-if="error"
    class="flex flex-col items-center justify-center gap-4 p-8 text-center"
  >
    <i class="pi pi-exclamation-triangle text-6xl text-red-500" />

    <div class="flex flex-col gap-2">
      <h3 class="text-xl font-semibold text-red-700">
        {{ title }}
      </h3>
      <p class="text-gray-600">
        {{ error.message }}
      </p>
    </div>

    <ActionButton
      v-if="showRetry"
      :label="retryLabel"
      severity="danger"
      @click="$emit('retry')"
    />

    <slot
      name="error"
      :error="error"
    />
  </div>
  <slot v-else />
</template>
