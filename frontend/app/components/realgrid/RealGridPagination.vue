<script setup lang="ts">
/**
 * RealGrid 페이지네이션 컴포넌트
 *
 * 그리드 데이터의 페이지 네비게이션 UI를 제공합니다.
 */

interface Props {
  /** 현재 페이지 번호 */
  currentPage: number
  /** 전체 페이지 수 */
  totalPages: number
  /** 표시할 페이지 번호 배열 */
  pageNumbers: number[]
  /** 이전 페이지 이동 가능 여부 */
  canGoPrev: boolean
  /** 다음 페이지 이동 가능 여부 */
  canGoNext: boolean
  /** 전체 아이템 수 */
  totalItems: number
}

defineProps<Props>()

const emit = defineEmits<{
  first: []
  prev: []
  next: []
  last: []
  goToPage: [page: number]
}>()
</script>

<template>
  <div class="realgrid-pagination">
    <button
      class="realgrid-pagination__btn"
      :disabled="!canGoPrev"
      @click="emit('first')"
    >
      |&lt;
    </button>
    <button
      class="realgrid-pagination__btn"
      :disabled="!canGoPrev"
      @click="emit('prev')"
    >
      &lt;
    </button>

    <button
      v-for="page in pageNumbers"
      :key="page"
      class="realgrid-pagination__btn"
      :class="{ 'realgrid-pagination__btn--active': page === currentPage }"
      @click="emit('goToPage', page)"
    >
      {{ page }}
    </button>

    <button
      class="realgrid-pagination__btn"
      :disabled="!canGoNext"
      @click="emit('next')"
    >
      &gt;
    </button>
    <button
      class="realgrid-pagination__btn"
      :disabled="!canGoNext"
      @click="emit('last')"
    >
      &gt;|
    </button>

    <span class="realgrid-pagination__info">
      {{ currentPage }} / {{ totalPages }} 페이지
      (총 {{ totalItems.toLocaleString() }}건)
    </span>
  </div>
</template>

<style scoped>
@import './styles/realgrid-pagination.css';
</style>
