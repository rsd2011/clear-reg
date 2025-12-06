/**
 * RealGrid 무한 스크롤 Composable
 *
 * 스크롤 위치 감지 및 자동 데이터 로드
 * - 임계값 도달 시 자동 로드
 * - 디바운스된 로딩
 * - 상태 관리
 */

import type { GridView, LocalDataProvider } from 'realgrid'
import type {
  RealGridInfiniteScrollState,
  InfiniteScrollLoadFn,
} from '~/types/realgrid'

// ============================================================================
// Types
// ============================================================================

export interface UseRealGridInfiniteScrollOptions<T = Record<string, unknown>> {
  /** 페이지 크기 (기본값: 50) */
  pageSize?: number
  /** 스크롤 임계값 (0-1, 하단에서의 비율, 기본값: 0.8) */
  threshold?: number
  /** 로딩 지연 시간 (ms, 기본값: 100) */
  loadingDelay?: number
  /** 데이터 로드 함수 */
  loadFn: InfiniteScrollLoadFn<T>
}

// ============================================================================
// Composable
// ============================================================================

/**
 * RealGrid 무한 스크롤 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { setupInfiniteScroll, loadMore, reset, state } = useRealGridInfiniteScroll({
 *   pageSize: 50,
 *   threshold: 0.8,
 *   loadFn: async (offset, limit) => {
 *     const response = await api.getItems({ offset, limit })
 *     return {
 *       data: response.items,
 *       hasMore: response.hasMore,
 *       total: response.total
 *     }
 *   }
 * })
 *
 * // 그리드 초기화 후
 * setupInfiniteScroll(gridView, dataProvider)
 *
 * // 초기 데이터 로드
 * await loadMore()
 * </script>
 * ```
 */
export const useRealGridInfiniteScroll = <T = Record<string, unknown>>(
  options: UseRealGridInfiniteScrollOptions<T>,
) => {
  const {
    pageSize = 50,
    threshold = 0.8,
    loadingDelay = 100,
    loadFn,
  } = options

  // 무한 스크롤 상태
  const state = reactive<RealGridInfiniteScrollState>({
    isLoading: false,
    hasMore: true,
    currentOffset: 0,
    pageSize,
    totalItems: 0,
  })

  // 스크롤 감지 핸들러 참조
  let scrollHandler: ((grid: GridView) => void) | null = null
  let gridRef: GridView | null = null
  let providerRef: LocalDataProvider | null = null

  // 디바운스 타이머
  let loadDebounceTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 다음 데이터 로드
   */
  const loadMore = async (): Promise<void> => {
    if (state.isLoading || !state.hasMore) {
      return
    }

    state.isLoading = true

    try {
      const result = await loadFn(state.currentOffset, state.pageSize)

      if (providerRef && result.data.length > 0) {
        // 기존 데이터에 추가
        providerRef.addRows(result.data as Record<string, unknown>[])
        state.currentOffset += result.data.length
      }

      state.hasMore = result.hasMore
      if (result.total !== undefined) {
        state.totalItems = result.total
      }
    }
    catch (error) {
      console.error('Failed to load more data:', error)
      state.hasMore = false
    }
    finally {
      state.isLoading = false
    }
  }

  /**
   * 디바운스된 로드
   */
  const debouncedLoadMore = (): void => {
    if (loadDebounceTimer) {
      clearTimeout(loadDebounceTimer)
    }

    loadDebounceTimer = setTimeout(() => {
      loadMore()
    }, loadingDelay)
  }

  /**
   * 스크롤 위치 확인 및 로드 트리거
   */
  const checkScrollPosition = (grid: GridView): void => {
    if (!state.hasMore || state.isLoading) {
      return
    }

    // RealGrid의 스크롤 정보 가져오기 (타입 정의에 없을 수 있음)
    const g = grid as unknown as { getScrollInfo?: () => { vscrollPosition: number, vscrollSize: number, viewHeight: number } }
    if (!g.getScrollInfo) return
    const scrollInfo = g.getScrollInfo()
    const { vscrollPosition, vscrollSize, viewHeight } = scrollInfo

    // 스크롤 가능한 총 높이
    const totalScrollable = vscrollSize - viewHeight
    if (totalScrollable <= 0) {
      // 스크롤이 필요 없는 경우 (데이터가 적음)
      return
    }

    // 현재 스크롤 비율
    const scrollRatio = vscrollPosition / totalScrollable

    // 임계값 도달 시 로드
    if (scrollRatio >= threshold) {
      debouncedLoadMore()
    }
  }

  /**
   * 무한 스크롤 설정
   */
  const setupInfiniteScroll = (grid: GridView, provider: LocalDataProvider): void => {
    gridRef = grid
    providerRef = provider

    // 스크롤 이벤트 핸들러 등록
    scrollHandler = (g: GridView) => {
      checkScrollPosition(g)
    }

    // onScrolled는 타입 정의에 없을 수 있음
    ;(grid as unknown as { onScrolled: typeof scrollHandler }).onScrolled = scrollHandler
  }

  /**
   * 무한 스크롤 해제
   */
  const teardownInfiniteScroll = (): void => {
    if (gridRef && scrollHandler) {
      ;(gridRef as unknown as { onScrolled: null }).onScrolled = null
    }

    if (loadDebounceTimer) {
      clearTimeout(loadDebounceTimer)
    }

    scrollHandler = null
    gridRef = null
    providerRef = null
  }

  /**
   * 상태 초기화 및 새로 로드
   */
  const reset = async (): Promise<void> => {
    state.isLoading = false
    state.hasMore = true
    state.currentOffset = 0
    state.totalItems = 0

    if (providerRef) {
      providerRef.clearRows()
    }

    await loadMore()
  }

  /**
   * 데이터 새로고침 (처음부터 다시 로드)
   */
  const refresh = async (): Promise<void> => {
    await reset()
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    teardownInfiniteScroll()
  })

  return {
    // 상태
    state: readonly(state),

    // 설정
    setupInfiniteScroll,
    teardownInfiniteScroll,

    // 액션
    loadMore,
    reset,
    refresh,
  }
}

export default useRealGridInfiniteScroll
