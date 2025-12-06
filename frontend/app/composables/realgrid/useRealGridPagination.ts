/**
 * RealGrid 페이지네이션 Composable
 *
 * 레거시 paging 함수를 Vue 3 반응형으로 현대화
 * - 전통적인 페이지네이션 지원
 * - 서버 사이드 페이징 통합
 */

import type { RealGridPaginationState } from '~/types/realgrid'

// ============================================================================
// Types
// ============================================================================

export interface UseRealGridPaginationOptions {
  /** 페이지당 아이템 수 (기본값: 20) */
  itemsPerPage?: number
  /** 페이지 그룹당 페이지 수 (기본값: 10) */
  pagesPerGroup?: number
  /** 페이지 변경 시 호출되는 콜백 */
  onPageChange?: (page: number) => void | Promise<void>
}

// ============================================================================
// Composable
// ============================================================================

/**
 * RealGrid 페이지네이션 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const {
 *   paginationState,
 *   goToPage,
 *   nextPage,
 *   prevPage,
 *   setTotalItems,
 *   pageNumbers
 * } = useRealGridPagination({
 *   itemsPerPage: 20,
 *   pagesPerGroup: 10,
 *   onPageChange: async (page) => {
 *     const data = await fetchData(page)
 *     dataProvider.setRows(data)
 *   }
 * })
 * </script>
 *
 * <template>
 *   <div class="pagination">
 *     <button @click="goToFirst" :disabled="!canGoPrev">|&lt;</button>
 *     <button @click="prevPage" :disabled="!canGoPrev">&lt;</button>
 *     <button
 *       v-for="page in pageNumbers"
 *       :key="page"
 *       @click="goToPage(page)"
 *       :class="{ active: page === paginationState.currentPage }"
 *     >
 *       {{ page }}
 *     </button>
 *     <button @click="nextPage" :disabled="!canGoNext">&gt;</button>
 *     <button @click="goToLast" :disabled="!canGoNext">&gt;|</button>
 *   </div>
 * </template>
 * ```
 */
export const useRealGridPagination = (options: UseRealGridPaginationOptions = {}) => {
  const { itemsPerPage = 20, pagesPerGroup = 10, onPageChange } = options

  // 페이지네이션 상태
  const paginationState = reactive<RealGridPaginationState>({
    currentPage: 1,
    totalItems: 0,
    itemsPerPage,
    totalPages: 0,
    pageGroup: 1,
    pagesPerGroup,
  })

  // 로딩 상태
  const isLoading = ref(false)

  // Computed 속성들
  const totalPages = computed(() =>
    Math.ceil(paginationState.totalItems / paginationState.itemsPerPage),
  )

  const pageGroup = computed(() =>
    Math.ceil(paginationState.currentPage / paginationState.pagesPerGroup),
  )

  const firstPageInGroup = computed(() =>
    (pageGroup.value - 1) * paginationState.pagesPerGroup + 1,
  )

  const lastPageInGroup = computed(() =>
    Math.min(pageGroup.value * paginationState.pagesPerGroup, totalPages.value),
  )

  const pageNumbers = computed(() => {
    const pages: number[] = []
    for (let i = firstPageInGroup.value; i <= lastPageInGroup.value; i++) {
      pages.push(i)
    }
    return pages
  })

  const canGoPrev = computed(() => paginationState.currentPage > 1)
  const canGoNext = computed(() => paginationState.currentPage < totalPages.value)
  const canGoPrevGroup = computed(() => pageGroup.value > 1)
  const canGoNextGroup = computed(() => lastPageInGroup.value < totalPages.value)

  // 페이지 정보 (offset, limit)
  const offset = computed(() =>
    (paginationState.currentPage - 1) * paginationState.itemsPerPage,
  )
  const limit = computed(() => paginationState.itemsPerPage)

  // 상태 동기화
  watch(totalPages, (newTotal) => {
    paginationState.totalPages = newTotal
  })

  watch(pageGroup, (newGroup) => {
    paginationState.pageGroup = newGroup
  })

  /**
   * 특정 페이지로 이동
   */
  const goToPage = async (page: number): Promise<void> => {
    if (page < 1 || page > totalPages.value || page === paginationState.currentPage) {
      return
    }

    isLoading.value = true
    paginationState.currentPage = page

    try {
      if (onPageChange) {
        await onPageChange(page)
      }
    }
    finally {
      isLoading.value = false
    }
  }

  /**
   * 다음 페이지로 이동
   */
  const nextPage = async (): Promise<void> => {
    if (canGoNext.value) {
      await goToPage(paginationState.currentPage + 1)
    }
  }

  /**
   * 이전 페이지로 이동
   */
  const prevPage = async (): Promise<void> => {
    if (canGoPrev.value) {
      await goToPage(paginationState.currentPage - 1)
    }
  }

  /**
   * 첫 페이지로 이동
   */
  const goToFirst = async (): Promise<void> => {
    await goToPage(1)
  }

  /**
   * 마지막 페이지로 이동
   */
  const goToLast = async (): Promise<void> => {
    await goToPage(totalPages.value)
  }

  /**
   * 다음 페이지 그룹으로 이동
   */
  const nextGroup = async (): Promise<void> => {
    if (canGoNextGroup.value) {
      await goToPage(lastPageInGroup.value + 1)
    }
  }

  /**
   * 이전 페이지 그룹으로 이동
   */
  const prevGroup = async (): Promise<void> => {
    if (canGoPrevGroup.value) {
      await goToPage(firstPageInGroup.value - 1)
    }
  }

  /**
   * 총 아이템 수 설정
   */
  const setTotalItems = (total: number): void => {
    paginationState.totalItems = total
    // 현재 페이지가 총 페이지 수를 초과하면 조정
    if (paginationState.currentPage > totalPages.value && totalPages.value > 0) {
      paginationState.currentPage = totalPages.value
    }
  }

  /**
   * 페이지당 아이템 수 변경
   */
  const setItemsPerPage = async (count: number): Promise<void> => {
    paginationState.itemsPerPage = count
    // 첫 페이지로 이동
    await goToPage(1)
  }

  /**
   * 페이지네이션 초기화
   */
  const reset = (): void => {
    paginationState.currentPage = 1
    paginationState.totalItems = 0
  }

  return {
    // 상태
    paginationState: readonly(paginationState),
    isLoading: readonly(isLoading),

    // Computed
    totalPages,
    pageGroup,
    firstPageInGroup,
    lastPageInGroup,
    pageNumbers,
    canGoPrev,
    canGoNext,
    canGoPrevGroup,
    canGoNextGroup,
    offset,
    limit,

    // 액션
    goToPage,
    nextPage,
    prevPage,
    goToFirst,
    goToLast,
    nextGroup,
    prevGroup,
    setTotalItems,
    setItemsPerPage,
    reset,
  }
}

export default useRealGridPagination
