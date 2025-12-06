/**
 * RealGrid 통합 스크롤 Composable
 *
 * 페이지네이션과 무한 스크롤을 하나의 인터페이스로 통합
 * - 모드 전환 지원
 * - 동적 스크롤 방식 변경
 */

import type { InfiniteScrollLoadFn } from '~/types/realgrid'
import {
  useRealGridPagination,
  type UseRealGridPaginationOptions,
} from './useRealGridPagination'
import {
  useRealGridInfiniteScroll,
  type UseRealGridInfiniteScrollOptions,
} from './useRealGridInfiniteScroll'

// ============================================================================
// Types
// ============================================================================

export type ScrollMode = 'pagination' | 'infinite' | 'none'

export interface UseRealGridScrollOptions<T = Record<string, unknown>> {
  /** 스크롤 모드 */
  mode: ScrollMode
  /** 페이지네이션 옵션 */
  pagination?: UseRealGridPaginationOptions
  /** 무한 스크롤 옵션 (loadFn 제외) */
  infiniteScroll?: Omit<UseRealGridInfiniteScrollOptions<T>, 'loadFn'>
  /** 데이터 로드 함수 (무한 스크롤용) */
  loadFn?: InfiniteScrollLoadFn<T>
}

// ============================================================================
// Composable
// ============================================================================

/**
 * RealGrid 스크롤 통합 Composable
 *
 * 페이지네이션과 무한 스크롤을 하나의 인터페이스로 통합
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { mode, pagination, infiniteScroll, switchMode } = useRealGridScroll({
 *   mode: 'infinite',
 *   loadFn: async (offset, limit) => {
 *     return fetchData(offset, limit)
 *   }
 * })
 *
 * // 모드 전환
 * const toggleMode = () => {
 *   switchMode(mode.value === 'infinite' ? 'pagination' : 'infinite')
 * }
 * </script>
 * ```
 */
export const useRealGridScroll = <T = Record<string, unknown>>(
  options: UseRealGridScrollOptions<T>,
) => {
  const mode = ref<ScrollMode>(options.mode)

  // 페이지네이션 초기화
  const paginationComposable = options.mode !== 'none' && options.pagination
    ? useRealGridPagination(options.pagination)
    : null

  // 무한 스크롤 초기화 (loadFn이 있을 때만)
  const infiniteScrollComposable = options.mode !== 'none' && options.loadFn
    ? useRealGridInfiniteScroll({
        ...options.infiniteScroll,
        loadFn: options.loadFn,
      })
    : null

  /**
   * 스크롤 모드 전환
   */
  const switchMode = async (newMode: ScrollMode): Promise<void> => {
    if (mode.value === newMode) {
      return
    }

    // 기존 모드 정리
    if (mode.value === 'infinite' && infiniteScrollComposable) {
      infiniteScrollComposable.teardownInfiniteScroll()
    }

    mode.value = newMode

    // 새 모드 초기화
    if (newMode === 'pagination' && paginationComposable) {
      paginationComposable.reset()
    }
    else if (newMode === 'infinite' && infiniteScrollComposable) {
      await infiniteScrollComposable.reset()
    }
  }

  return {
    mode: readonly(mode),
    pagination: paginationComposable,
    infiniteScroll: infiniteScrollComposable,
    switchMode,
  }
}

export default useRealGridScroll
