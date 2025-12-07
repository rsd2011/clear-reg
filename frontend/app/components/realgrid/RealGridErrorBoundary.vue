<script setup lang="ts">
/**
 * RealGrid 에러 바운더리 컴포넌트
 *
 * 그리드 초기화 실패 시 에러 UI를 표시하고 재시도 기능을 제공합니다.
 */

interface Props {
  /** 에러 객체 */
  error: Error | null
  /** 재시도 중 여부 */
  isRetrying?: boolean
  /** 컨테이너 높이 */
  height?: string
}

withDefaults(defineProps<Props>(), {
  isRetrying: false,
  height: '400px',
})

const emit = defineEmits<{
  retry: []
}>()

const handleRetry = () => {
  emit('retry')
}
</script>

<template>
  <div
    v-if="error"
    class="realgrid-error"
    :style="{ height, minHeight: '200px' }"
  >
    <div class="realgrid-error__content">
      <span class="realgrid-error__icon">⚠️</span>
      <h3 class="realgrid-error__title">
        그리드 초기화 실패
      </h3>
      <p class="realgrid-error__message">
        {{ error.message }}
      </p>
      <button
        class="realgrid-error__retry-btn"
        :disabled="isRetrying"
        @click="handleRetry"
      >
        {{ isRetrying ? '재시도 중...' : '🔄 다시 시도' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
@import './styles/realgrid-error.css';
</style>
