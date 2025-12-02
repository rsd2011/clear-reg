<script setup lang="ts">
/**
 * Loading 공통 컴포넌트
 *
 * 로딩 상태를 표시하는 범용 컴포넌트
 */

interface Props {
  /** 로딩 중 여부 */
  loading?: boolean
  /** 메시지 */
  message?: string
  /** 크기 */
  size?: 'small' | 'medium' | 'large'
  /** 전체 화면 오버레이 */
  fullscreen?: boolean
}

withDefaults(defineProps<Props>(), {
  message: '로딩 중...',
  size: 'medium',
})

const sizeClasses = {
  small: 'w-4 h-4',
  medium: 'w-8 h-8',
  large: 'w-12 h-12',
}
</script>

<template>
  <div
    v-if="loading"
    :class="[
      'flex flex-col items-center justify-center gap-4',
      fullscreen && 'fixed inset-0 bg-black/50 z-50',
    ]"
  >
    <i
      class="pi pi-spin pi-spinner"
      :class="sizeClasses[size]"
    />
    <p v-if="message">
      {{ message }}
    </p>
  </div>
  <slot v-else />
</template>
