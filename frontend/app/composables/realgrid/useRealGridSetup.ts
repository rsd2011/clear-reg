/**
 * RealGrid Setup Composable
 *
 * RealGrid 초기화, 프리셋 적용, 이벤트 바인딩을 오케스트레이션합니다.
 * 모든 기능 composable을 조합하여 통합된 인터페이스를 제공합니다.
 */

import type { GridView, LocalDataProvider, DataFieldObject } from 'realgrid'
import type {
  RealGridColumn,
  RealGridEventsExtended,
  RealGridInstance,
  RealGridCellClickData,
  RealGridOptionsExtended,
  RealGridColumnValidation,
  RealGridPaginationOptions,
  RealGridInfiniteScrollOptions,
  InfiniteScrollLoadFn,
  RealGridPreset,
} from '~/types/realgrid'

import { initializeRealGrid } from '~/plugins/realgrid.client'
import {
  PRESET_CONFIGS,
  applyRecommendedOptions,
  applyPresetConfig,
  isUndoEnabled,
} from '~/config/realgrid-presets'

import {
  useRealGridContextMenu,
  useRealGridKeyboard,
  useRealGridPersistence,
  useRealGridExport,
  useRealGridSelection,
  useRealGridValidation,
  useRealGridPagination,
  useRealGridInfiniteScroll,
  type UseRealGridPaginationOptions,
} from '~/composables/realgrid'

// ============================================================================
// Types
// ============================================================================

export interface UseRealGridSetupOptions {
  /** 컬럼 정의 */
  columns: RealGridColumn[]
  /** 그리드 옵션 */
  options?: RealGridOptionsExtended
  /** 그리드 이벤트 핸들러 */
  events?: RealGridEventsExtended
  /** 그리드 프리셋 */
  preset?: RealGridPreset

  /** 기능 활성화 옵션 */
  features?: {
    /** 컨텍스트 메뉴 활성화 */
    contextMenu?: boolean
    /** 키보드 단축키 활성화 */
    keyboard?: boolean
    /** 상태 저장 활성화 (storageKey 필요) */
    persistence?: boolean | string
    /** 선택 요약 활성화 */
    selection?: boolean
    /** 유효성 검사 규칙 */
    validations?: RealGridColumnValidation[]
  }

  /** 스크롤 옵션 */
  scroll?: {
    /** 스크롤 모드 */
    mode?: 'none' | 'pagination' | 'infinite'
    /** 페이지네이션 옵션 */
    pagination?: RealGridPaginationOptions & UseRealGridPaginationOptions
    /** 무한 스크롤 옵션 */
    infiniteScroll?: RealGridInfiniteScrollOptions
    /** 데이터 로드 함수 (무한 스크롤용) */
    loadFn?: InfiniteScrollLoadFn
  }

  /** 이벤트 콜백 */
  callbacks?: {
    onReady?: (instance: RealGridInstance) => void
    onSelectionSummaryChange?: (summary: {
      sum: number
      count: number
      average: number
      min: number
      max: number
    }) => void
    onValidationError?: (errors: { row: number, column: string, message: string }[]) => void
    onPageChange?: (page: number) => void
    onLoadMore?: () => void
  }
}

export interface UseRealGridSetupReturn {
  // Refs
  gridContainer: Ref<HTMLDivElement | null>
  gridInstance: Ref<RealGridInstance | null>
  initError: Ref<Error | null>
  isRetrying: Ref<boolean>

  // Methods
  initGrid: () => Promise<void>
  retryInit: () => Promise<void>
  updateData: (data: Record<string, unknown>[]) => void
  getGridInstance: () => RealGridInstance | null

  // Export methods
  exportExcel: (fileName?: string) => void
  exportCsv: (fileName?: string) => void
  exportJson: (fileName?: string) => void
  copyToClipboard: () => Promise<boolean>

  // Validation methods
  validateAll: () => { valid: boolean, errors: { row: number, column: string, value: unknown, message: string }[] }
  goToFirstError: () => void

  // State methods
  saveState: () => void
  loadState: () => boolean
  clearState: () => void

  // Pagination methods
  goToPage: (page: number) => Promise<void>
  setTotalItems: (total: number) => void

  // Infinite scroll methods
  loadMore: () => Promise<void>
  resetInfiniteScroll: () => Promise<void>

  // Composable references (for expose)
  contextMenu: ReturnType<typeof useRealGridContextMenu>
  keyboard: ReturnType<typeof useRealGridKeyboard>
  persistence: ReturnType<typeof useRealGridPersistence> | null
  exportComposable: ReturnType<typeof useRealGridExport>
  selection: ReturnType<typeof useRealGridSelection>
  validation: ReturnType<typeof useRealGridValidation> | null
  pagination: ReturnType<typeof useRealGridPagination> | null
  infiniteScroll: ReturnType<typeof useRealGridInfiniteScroll> | null
}

// ============================================================================
// Composable
// ============================================================================

export function useRealGridSetup(options: UseRealGridSetupOptions): UseRealGridSetupReturn {
  const {
    columns,
    options: gridOptions = {},
    events = {},
    preset = 'default',
    features = {},
    scroll = {},
    callbacks = {},
  } = options

  // ========================================
  // Refs
  // ========================================

  const gridContainer = ref<HTMLDivElement | null>(null)
  const gridInstance = ref<RealGridInstance | null>(null)
  const initError = ref<Error | null>(null)
  const isRetrying = ref(false)

  // ========================================
  // Composables 초기화
  // ========================================

  // 컨텍스트 메뉴
  const contextMenu = useRealGridContextMenu({
    options: gridOptions.contextMenu,
    onExport: (exportOptions) => {
      if (gridInstance.value) {
        exportComposable.exportGrid(
          gridInstance.value.gridView,
          gridInstance.value.dataProvider,
          exportOptions,
        )
      }
    },
  })

  // 키보드
  const keyboard = useRealGridKeyboard({
    options: gridOptions.keyboard,
  })

  // 상태 저장
  const storageKey = typeof features.persistence === 'string'
    ? features.persistence
    : ''
  const persistence = features.persistence && storageKey
    ? useRealGridPersistence({
        options: {
          storageKey,
          ...gridOptions.persistence,
        },
      })
    : null

  // 내보내기
  const exportComposable = useRealGridExport({
    defaultOptions: {
      includeHeader: true,
      onlyVisible: true,
    },
  })

  // 선택 요약
  const selection = useRealGridSelection({
    onSummaryChange: (summary) => {
      callbacks.onSelectionSummaryChange?.(summary)
    },
  })

  // 유효성 검사
  const validation = features.validations && features.validations.length > 0
    ? useRealGridValidation({
        validations: features.validations,
        options: gridOptions.validation,
        onValidationError: (result) => {
          callbacks.onValidationError?.(result.errors)
        },
      })
    : null

  // 페이지네이션
  const pagination = scroll.mode === 'pagination'
    ? useRealGridPagination({
        ...scroll.pagination,
        onPageChange: async (page) => {
          callbacks.onPageChange?.(page)
          if (scroll.pagination?.onPageChange) {
            await scroll.pagination.onPageChange(page)
          }
        },
      })
    : null

  // 무한 스크롤
  const infiniteScroll = scroll.mode === 'infinite' && scroll.loadFn
    ? useRealGridInfiniteScroll({
        ...scroll.infiniteScroll,
        loadFn: scroll.loadFn,
      })
    : null

  // ========================================
  // Grid 초기화
  // ========================================

  const initGrid = async () => {
    initError.value = null

    if (!gridContainer.value) {
      console.error('[RealGrid] Container not found')
      initError.value = new Error('그리드 컨테이너를 찾을 수 없습니다.')
      return
    }

    // 🔬 개발 모드 성능 모니터링
    const startTime = import.meta.env.DEV ? performance.now() : 0

    // 컨테이너 크기 검증
    const rect = gridContainer.value.getBoundingClientRect()
    if (import.meta.env.DEV) {
      console.log('[RealGrid] Container size:', rect.width, 'x', rect.height)
    }

    if (rect.height < 50) {
      console.warn('[RealGrid] Container height too small:', rect.height, 'px')
    }

    try {
      // 🚀 RealGrid 지연 초기화
      await initializeRealGrid()

      const RealGrid = await import('realgrid')

      // LocalDataProvider 생성 (Undo 설정)
      const enableUndo = isUndoEnabled(preset)
      const dataProvider = new RealGrid.LocalDataProvider(enableUndo)
      const gridView = new RealGrid.GridView(gridContainer.value)
      gridView.setDataSource(dataProvider)

      // undoable 설정
      ;(gridView as unknown as { undoable: boolean }).undoable = enableUndo
      ;(dataProvider as unknown as { undoable: boolean }).undoable = enableUndo

      // ========================================
      // RealGrid 추천 설정 적용
      // ========================================
      applyRecommendedOptions(gridView)

      // ========================================
      // 프리셋 기반 설정 적용
      // ========================================
      applyPresetConfig(gridView, dataProvider, preset)

      // ========================================
      // 사용자 옵션 적용 (기본 설정 override)
      // ========================================
      if (gridOptions) {
        if (gridOptions.display) {
          Object.assign(gridView.displayOptions, gridOptions.display)
        }
        if (gridOptions.edit) {
          Object.assign(gridView.editOptions, gridOptions.edit)
        }
        if (gridOptions.checkBar) {
          Object.assign(gridView.checkBar, gridOptions.checkBar)
        }
        const topLevelKeys = ['columnMovable', 'columnResizable', 'sortMode', 'filterMode', 'hideDeletedRows']
        topLevelKeys.forEach((key) => {
          if (key in gridOptions) {
            ;(gridView as unknown as Record<string, unknown>)[key] = gridOptions[key]
          }
        })
      }

      // 필드 설정
      const fields: DataFieldObject[] = columns.map((col) => ({
        fieldName: col.fieldName || col.name,
        dataType: col.type === 'number' ? 'number' : 'text',
      } as DataFieldObject))
      dataProvider.setFields(fields)

      // 컬럼 설정
      gridView.setColumns(columns)

      // ========================================
      // 이벤트 핸들러 등록
      // ========================================
      if (events.onReady) {
        events.onReady(gridView, dataProvider)
      }
      if (events.onCellClicked) {
        gridView.onCellClicked = (_grid, clickData) => {
          events.onCellClicked?.(gridView, clickData as RealGridCellClickData)
        }
      }
      if (events.onCellDblClicked) {
        gridView.onCellDblClicked = (_grid, clickData) => {
          events.onCellDblClicked?.(gridView, clickData as RealGridCellClickData)
        }
      }
      if (events.onDataCellClicked) {
        gridView.onCellClicked = (_grid, clickData) => {
          events.onDataCellClicked?.(gridView, clickData as RealGridCellClickData)
        }
      }
      if (events.onCurrentRowChanged) {
        gridView.onCurrentRowChanged = (_grid, oldRow, newRow) => {
          events.onCurrentRowChanged?.(gridView, oldRow, newRow)
        }
      }

      // ========================================
      // 확장 기능 설정
      // ========================================

      // 컨텍스트 메뉴
      if (features.contextMenu !== false) {
        contextMenu.setupContextMenu(gridView)
        gridView.onContextMenuItemClicked = (grid, menuItem, clickData) => {
          const gv = grid as GridView
          contextMenu.handleContextMenuClick(gv, menuItem as unknown as import('~/types/realgrid').RealGridContextMenuClickData, clickData)
          events.onContextMenuClick?.(gv, menuItem as unknown as import('~/types/realgrid').RealGridContextMenuClickData, clickData)
        }
      }

      // 키보드 단축키
      if (features.keyboard !== false) {
        keyboard.setupKeyboard(gridView, dataProvider, gridContainer.value)
      }

      // 상태 저장
      if (persistence) {
        persistence.loadState(gridView)
        persistence.setupAutoSave(gridView)
      }

      // 선택 요약
      if (features.selection !== false) {
        selection.setupSelection(gridView)
      }

      // 유효성 검사
      if (validation) {
        validation.setupValidation(gridView, dataProvider)
      }

      // 무한 스크롤
      if (infiniteScroll) {
        infiniteScroll.setupInfiniteScroll(gridView, dataProvider)
      }

      // ========================================
      // 인스턴스 저장
      // ========================================
      const instance: RealGridInstance = {
        gridView,
        dataProvider,
        destroy: () => {
          if (features.keyboard !== false) {
            keyboard.teardownKeyboard()
          }
          if (persistence) {
            persistence.teardownAutoSave(gridView)
          }
          if (features.selection !== false) {
            selection.teardownSelection(gridView)
          }
          if (validation) {
            validation.teardownValidation(gridView)
          }
          if (infiniteScroll) {
            infiniteScroll.teardownInfiniteScroll()
          }

          gridView.destroy()
          dataProvider.destroy()
        },
      }
      gridInstance.value = instance

      // 🔬 개발 모드 성능 모니터링 완료
      if (import.meta.env.DEV) {
        const endTime = performance.now()
        const initTime = endTime - startTime
        console.log(`[RealGrid] ✅ 초기화 완료: ${initTime.toFixed(2)}ms`)
        console.log(`[RealGrid] 📊 프리셋: ${preset}`)
      }

      // ready 콜백
      callbacks.onReady?.(instance)
    }
    catch (error) {
      const err = error instanceof Error ? error : new Error(String(error))
      console.error('[RealGrid] ❌ 초기화 실패:', err.message)
      initError.value = err
    }
  }

  const retryInit = async () => {
    isRetrying.value = true
    try {
      await initGrid()
    }
    finally {
      isRetrying.value = false
    }
  }

  // ========================================
  // Data Methods
  // ========================================

  const updateData = (newData: Record<string, unknown>[]) => {
    if (gridInstance.value) {
      gridInstance.value.dataProvider.setRows(newData)
    }
  }

  const getGridInstance = () => gridInstance.value

  // ========================================
  // Export Methods
  // ========================================

  const exportExcel = (fileName?: string) => {
    if (gridInstance.value) {
      exportComposable.exportExcel(
        gridInstance.value.gridView,
        gridInstance.value.dataProvider,
        { fileName },
      )
    }
  }

  const exportCsv = (fileName?: string) => {
    if (gridInstance.value) {
      exportComposable.exportCsv(
        gridInstance.value.gridView,
        gridInstance.value.dataProvider,
        { fileName },
      )
    }
  }

  const exportJson = (fileName?: string) => {
    if (gridInstance.value) {
      exportComposable.exportJson(
        gridInstance.value.gridView,
        gridInstance.value.dataProvider,
        { fileName },
      )
    }
  }

  const copyToClipboard = () => {
    if (gridInstance.value) {
      return exportComposable.copyToClipboard(
        gridInstance.value.gridView,
        gridInstance.value.dataProvider,
      )
    }
    return Promise.resolve(false)
  }

  // ========================================
  // Validation Methods
  // ========================================

  const validateAll = () => {
    if (validation && gridInstance.value) {
      return validation.validateAll(gridInstance.value.dataProvider)
    }
    return { valid: true, errors: [] }
  }

  const goToFirstError = () => {
    if (validation && gridInstance.value) {
      validation.goToFirstError(gridInstance.value.gridView)
    }
  }

  // ========================================
  // State Methods
  // ========================================

  const saveState = () => {
    if (persistence && gridInstance.value) {
      persistence.saveState(gridInstance.value.gridView)
    }
  }

  const loadState = () => {
    if (persistence && gridInstance.value) {
      return persistence.loadState(gridInstance.value.gridView)
    }
    return false
  }

  const clearState = () => {
    if (persistence) {
      persistence.clearState()
    }
  }

  // ========================================
  // Pagination Methods
  // ========================================

  const goToPage = (page: number) => {
    if (pagination) {
      return pagination.goToPage(page)
    }
    return Promise.resolve()
  }

  const setTotalItems = (total: number) => {
    if (pagination) {
      pagination.setTotalItems(total)
    }
  }

  // ========================================
  // Infinite Scroll Methods
  // ========================================

  const loadMore = () => {
    if (infiniteScroll) {
      callbacks.onLoadMore?.()
      return infiniteScroll.loadMore()
    }
    return Promise.resolve()
  }

  const resetInfiniteScroll = () => {
    if (infiniteScroll) {
      return infiniteScroll.reset()
    }
    return Promise.resolve()
  }

  return {
    // Refs
    gridContainer,
    gridInstance,
    initError,
    isRetrying,

    // Methods
    initGrid,
    retryInit,
    updateData,
    getGridInstance,

    // Export
    exportExcel,
    exportCsv,
    exportJson,
    copyToClipboard,

    // Validation
    validateAll,
    goToFirstError,

    // State
    saveState,
    loadState,
    clearState,

    // Pagination
    goToPage,
    setTotalItems,

    // Infinite Scroll
    loadMore,
    resetInfiniteScroll,

    // Composable references
    contextMenu,
    keyboard,
    persistence,
    exportComposable,
    selection,
    validation,
    pagination,
    infiniteScroll,
  }
}

export default useRealGridSetup
