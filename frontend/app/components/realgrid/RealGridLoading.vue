<script setup lang="ts">
/**
 * RealGrid 로딩 컴포넌트
 *
 * 무한 스크롤 데이터 로딩 중 표시되는 인디케이터입니다.
 */

interface Props {
  /** 로딩 메시지 */
  message?: string
}

withDefaults(defineProps<Props>(), {
  message: '데이터 로딩 중...',
})
</script>

<template>
  <div class="realgrid-loading">
    <span class="realgrid-loading__spinner" />
    <span>{{ message }}</span>
  </div>
</template>

<style scoped>
@import './styles/realgrid-loading.css';
</style>
