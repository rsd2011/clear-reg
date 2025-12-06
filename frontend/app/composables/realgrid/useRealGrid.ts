/**
 * RealGrid 통합 Composable
 *
 * 모든 RealGrid 기능을 하나의 인터페이스로 제공하는 Facade Composable
 * - 그리드 초기화 및 정리
 * - 선택 기능 (useRealGridSelection)
 * - 키보드 단축키 (useRealGridKeyboard)
 * - 컨텍스트 메뉴 (useRealGridContextMenu)
 * - 내보내기 (useRealGridExport)
 * - 유효성 검사 (useRealGridValidation)
 * - 상태 저장 (useRealGridPersistence)
 * - 스크롤/페이지네이션 (useRealGridScroll)
 */

import type { GridView, LocalDataProvider, DataField } from 'realgrid'
import type { RealGridColumn, InfiniteScrollLoadFn } from '~/types/realgrid'

import { useRealGridSelection, type UseRealGridSelectionInput } from './useRealGridSelection'
import { useRealGridKeyboard, type UseRealGridKeyboardInput } from './useRealGridKeyboard'
import { useRealGridContextMenu, type UseRealGridContextMenuOptions } from './useRealGridContextMenu'
import { useRealGridExport, type UseRealGridExportInput } from './useRealGridExport'
import { useRealGridValidation, type UseRealGridValidationInput } from './useRealGridValidation'
import { useRealGridPersistence, type UseRealGridPersistenceInput } from './useRealGridPersistence'
import { useRealGridScroll, type ScrollMode } from './useRealGridScroll'
import { useRealGridPagination, type UseRealGridPaginationOptions } from './useRealGridPagination'
import { useRealGridInfiniteScroll, type UseRealGridInfiniteScrollOptions } from './useRealGridInfiniteScroll'

// ============================================================================
// Types
// ============================================================================

export interface UseRealGridOptions<T = Record<string, unknown>> {
  /** 그리드 고유 ID (상태 저장에 사용) */
  gridId?: string

  /** 필드 정의 */
  fields?: DataField[]

  /** 컬럼 정의 */
  columns?: RealGridColumn[]

  /** 선택 기능 옵션 */
  selection?: UseRealGridSelectionInput

  /** 키보드 단축키 옵션 */
  keyboard?: UseRealGridKeyboardInput

  /** 컨텍스트 메뉴 옵션 */
  contextMenu?: UseRealGridContextMenuOptions

  /** 내보내기 옵션 */
  export?: UseRealGridExportInput

  /** 유효성 검사 옵션 */
  validation?: UseRealGridValidationInput

  /** 상태 저장 옵션 */
  persistence?: UseRealGridPersistenceInput

  /** 스크롤 모드 */
  scrollMode?: ScrollMode

  /** 페이지네이션 옵션 (scrollMode가 'pagination'일 때) */
  pagination?: UseRealGridPaginationOptions

  /** 무한 스크롤 옵션 (scrollMode가 'infinite'일 때) */
  infiniteScroll?: Omit<UseRealGridInfiniteScrollOptions<T>, 'loadFn'>

  /** 데이터 로드 함수 (무한 스크롤용) */
  loadFn?: InfiniteScrollLoadFn<T>

  /** 그리드 초기화 후 콜백 */
  onReady?: (grid: GridView, provider: LocalDataProvider) => void

  /** 에러 발생 시 콜백 */
  onError?: (error: Error) => void
}

export interface UseRealGridReturn<T = Record<string, unknown>> {
  // 그리드 인스턴스 참조
  gridView: Ref<GridView | null>
  dataProvider: Ref<LocalDataProvider | null>

  // 상태
  isReady: Ref<boolean>
  isLoading: Ref<boolean>

  // 초기화
  initialize: (containerEl: HTMLDivElement | string) => Promise<void>
  destroy: () => void

  // 데이터 조작
  setData: (data: T[]) => void
  addData: (data: T[]) => void
  clearData: () => void
  getData: () => T[]

  // 개별 composable 접근
  selection: ReturnType<typeof useRealGridSelection> | null
  keyboard: ReturnType<typeof useRealGridKeyboard> | null
  contextMenu: ReturnType<typeof useRealGridContextMenu> | null
  exportFeature: ReturnType<typeof useRealGridExport> | null
  validation: ReturnType<typeof useRealGridValidation> | null
  persistence: ReturnType<typeof useRealGridPersistence> | null
  scroll: ReturnType<typeof useRealGridScroll> | null
  pagination: ReturnType<typeof useRealGridPagination> | null
  infiniteScroll: ReturnType<typeof useRealGridInfiniteScroll> | null
}

// ============================================================================
// Composable
// ============================================================================

/**
 * RealGrid 통합 Composable
 *
 * 모든 RealGrid 기능을 하나의 인터페이스로 통합하여 제공합니다.
 * 필요한 기능만 선택적으로 활성화할 수 있습니다.
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const {
 *   gridView,
 *   dataProvider,
 *   isReady,
 *   initialize,
 *   destroy,
 *   setData,
 *   selection,
 *   pagination
 * } = useRealGrid({
 *   gridId: 'my-grid',
 *   fields: [{ fieldName: 'id' }, { fieldName: 'name' }],
 *   columns: [
 *     { name: 'id', fieldName: 'id', header: { text: 'ID' } },
 *     { name: 'name', fieldName: 'name', header: { text: '이름' } }
 *   ],
 *   selection: { options: { numericOnly: true } },
 *   scrollMode: 'pagination',
 *   pagination: {
 *     itemsPerPage: 20,
 *     onPageChange: async (page) => {
 *       const data = await fetchData(page)
 *       setData(data)
 *     }
 *   }
 * })
 *
 * const containerRef = ref<HTMLDivElement>()
 *
 * onMounted(async () => {
 *   if (containerRef.value) {
 *     await initialize(containerRef.value)
 *   }
 * })
 *
 * onUnmounted(() => {
 *   destroy()
 * })
 * </script>
 *
 * <template>
 *   <div ref="containerRef" class="grid-container" />
 *   <div v-if="pagination" class="pagination">
 *     <button @click="pagination.prevPage" :disabled="!pagination.canGoPrev.value">이전</button>
 *     <span>{{ pagination.paginationState.currentPage }} / {{ pagination.totalPages.value }}</span>
 *     <button @click="pagination.nextPage" :disabled="!pagination.canGoNext.value">다음</button>
 *   </div>
 * </template>
 * ```
 */
export const useRealGrid = <T = Record<string, unknown>>(
  options: UseRealGridOptions<T> = {},
): UseRealGridReturn<T> => {
  // 그리드 인스턴스 참조
  const gridView = ref<GridView | null>(null)
  const dataProvider = ref<LocalDataProvider | null>(null)

  // 상태
  const isReady = ref(false)
  const isLoading = ref(false)

  // 개별 composable 인스턴스 (미리 생성 - setup 메서드 패턴)
  const selectionComposable = options.selection
    ? useRealGridSelection(options.selection)
    : null

  const keyboardComposable = options.keyboard
    ? useRealGridKeyboard(options.keyboard)
    : null

  const contextMenuComposable = options.contextMenu
    ? useRealGridContextMenu(options.contextMenu)
    : null

  const exportComposable = options.export
    ? useRealGridExport(options.export)
    : null

  const validationComposable = options.validation
    ? useRealGridValidation(options.validation)
    : null

  const persistenceComposable = options.persistence
    ? useRealGridPersistence(options.persistence)
    : null

  const paginationComposable = options.scrollMode === 'pagination' && options.pagination
    ? useRealGridPagination(options.pagination)
    : null

  const infiniteScrollComposable = options.scrollMode === 'infinite' && options.loadFn
    ? useRealGridInfiniteScroll({
        ...options.infiniteScroll,
        loadFn: options.loadFn,
      })
    : null

  const scrollComposable = options.scrollMode && options.scrollMode !== 'none' && options.loadFn
    ? useRealGridScroll({
        mode: options.scrollMode,
        pagination: options.pagination,
        infiniteScroll: options.infiniteScroll,
        loadFn: options.loadFn,
      })
    : null

  /**
   * 그리드 초기화
   */
  const initialize = async (containerEl: HTMLDivElement | string): Promise<void> => {
    try {
      isLoading.value = true

      // RealGrid 모듈 동적 import
      const { GridView: GridViewClass, LocalDataProvider: LocalDataProviderClass } = await import('realgrid')

      // DataProvider 생성
      const provider = new LocalDataProviderClass(false)
      dataProvider.value = provider

      // GridView 생성
      const grid = new GridViewClass(containerEl)
      grid.setDataSource(provider)
      gridView.value = grid

      // 필드 설정
      if (options.fields) {
        provider.setFields(options.fields)
      }

      // 컬럼 설정
      if (options.columns) {
        grid.setColumns(options.columns)
      }

      // 선택 기능 설정
      if (selectionComposable) {
        selectionComposable.setupSelection(grid)
      }

      // 키보드 단축키 설정
      if (keyboardComposable) {
        keyboardComposable.setupKeyboard(grid, provider)
      }

      // 컨텍스트 메뉴 설정
      if (contextMenuComposable) {
        contextMenuComposable.setupContextMenu(grid)
        // 타입 캐스팅: RealGrid 내부 타입과 Composable 타입 호환
        grid.onContextMenuItemClicked = contextMenuComposable.handleContextMenuClick as typeof grid.onContextMenuItemClicked
      }

      // 유효성 검사 설정
      if (validationComposable) {
        validationComposable.setupValidation(grid, provider)
      }

      // 상태 저장 설정
      if (persistenceComposable) {
        persistenceComposable.loadState(grid)
        persistenceComposable.setupAutoSave(grid)
      }

      // 무한 스크롤 설정
      if (infiniteScrollComposable) {
        infiniteScrollComposable.setupInfiniteScroll(grid, provider)
      }

      isReady.value = true

      // 준비 완료 콜백
      if (options.onReady) {
        options.onReady(grid, provider)
      }
    }
    catch (error) {
      console.error('Failed to initialize RealGrid:', error)
      if (options.onError) {
        options.onError(error as Error)
      }
      throw error
    }
    finally {
      isLoading.value = false
    }
  }

  /**
   * 그리드 정리
   */
  const destroy = (): void => {
    // 무한 스크롤 정리
    if (infiniteScrollComposable) {
      infiniteScrollComposable.teardownInfiniteScroll()
    }

    // 통합 스크롤 정리
    if (scrollComposable?.infiniteScroll) {
      scrollComposable.infiniteScroll.teardownInfiniteScroll()
    }

    // GridView 정리
    if (gridView.value) {
      gridView.value.destroy()
      gridView.value = null
    }

    // DataProvider 정리
    if (dataProvider.value) {
      dataProvider.value.destroy()
      dataProvider.value = null
    }

    isReady.value = false
  }

  /**
   * 데이터 설정
   */
  const setData = (data: T[]): void => {
    if (dataProvider.value) {
      dataProvider.value.setRows(data as Record<string, unknown>[])
    }
  }

  /**
   * 데이터 추가
   */
  const addData = (data: T[]): void => {
    if (dataProvider.value) {
      dataProvider.value.addRows(data as Record<string, unknown>[])
    }
  }

  /**
   * 데이터 초기화
   */
  const clearData = (): void => {
    if (dataProvider.value) {
      dataProvider.value.clearRows()
    }
  }

  /**
   * 데이터 조회
   */
  const getData = (): T[] => {
    if (!dataProvider.value) {
      return []
    }

    const rowCount = dataProvider.value.getRowCount()
    const data: T[] = []

    for (let i = 0; i < rowCount; i++) {
      data.push(dataProvider.value.getJsonRow(i) as T)
    }

    return data
  }

  // 컴포넌트 언마운트 시 자동 정리
  onBeforeUnmount(() => {
    destroy()
  })

  return {
    // 그리드 인스턴스 참조
    gridView: gridView as Ref<GridView | null>,
    dataProvider: dataProvider as Ref<LocalDataProvider | null>,

    // 상태
    isReady: readonly(isReady) as Ref<boolean>,
    isLoading: readonly(isLoading) as Ref<boolean>,

    // 초기화
    initialize,
    destroy,

    // 데이터 조작
    setData,
    addData,
    clearData,
    getData,

    // 개별 composable 접근
    selection: selectionComposable,
    keyboard: keyboardComposable,
    contextMenu: contextMenuComposable,
    exportFeature: exportComposable,
    validation: validationComposable,
    persistence: persistenceComposable,
    scroll: scrollComposable,
    pagination: paginationComposable,
    infiniteScroll: infiniteScrollComposable,
  }
}

export default useRealGrid
