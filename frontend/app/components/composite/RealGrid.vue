<script setup lang="ts">
/**
 * RealGrid 확장 컴포넌트
 *
 * 레거시 realgrid-utils.js를 TypeScript/Vue 3로 현대화하여 통합
 * - 컨텍스트 메뉴 (고정, 컬럼 표시/숨김, 행 높이, 내보내기)
 * - 키보드 단축키 (복사, 붙여넣기, 실행 취소, 삭제)
 * - 상태 저장 (컬럼, 필터, 정렬)
 * - 선택 영역 요약 (합계, 평균, 최대, 최소)
 * - 유효성 검사
 * - 페이지네이션 / 무한 스크롤
 */

import type { GridView, DataFieldObject } from 'realgrid'
import { useThemeStore } from '~/stores/theme'
import { initializeRealGrid } from '~/plugins/realgrid.client'
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
} from '~/types/realgrid'

// Composables
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
// Props 정의
// ============================================================================

interface Props {
  /** 컬럼 정의 */
  columns: RealGridColumn[]
  /** 그리드 데이터 */
  data?: Record<string, unknown>[]
  /** 그리드 옵션 (확장) */
  options?: RealGridOptionsExtended
  /** 그리드 이벤트 핸들러 */
  events?: RealGridEventsExtended
  /** 그리드 높이 */
  height?: string

  // 기능 활성화 옵션
  /** 컨텍스트 메뉴 활성화 */
  enableContextMenu?: boolean
  /** 키보드 단축키 활성화 */
  enableKeyboard?: boolean
  /** 상태 저장 활성화 (storageKey 필요) */
  enablePersistence?: boolean
  /** 상태 저장 키 */
  storageKey?: string
  /** 선택 요약 활성화 */
  enableSelectionSummary?: boolean
  /** 유효성 검사 규칙 */
  validations?: RealGridColumnValidation[]

  // 스크롤 모드
  /** 스크롤 모드 ('none' | 'pagination' | 'infinite') */
  scrollMode?: 'none' | 'pagination' | 'infinite'
  /** 페이지네이션 옵션 */
  paginationOptions?: RealGridPaginationOptions & UseRealGridPaginationOptions
  /** 무한 스크롤 옵션 */
  infiniteScrollOptions?: RealGridInfiniteScrollOptions
  /** 데이터 로드 함수 (무한 스크롤용) */
  loadFn?: InfiniteScrollLoadFn
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  options: () => ({}),
  events: () => ({}),
  height: '400px',
  enableContextMenu: true,
  enableKeyboard: true,
  enablePersistence: false,
  storageKey: '',
  enableSelectionSummary: true,
  validations: () => [],
  scrollMode: 'none',
  paginationOptions: () => ({}),
  infiniteScrollOptions: () => ({}),
})

// ============================================================================
// Emits 정의
// ============================================================================

const emit = defineEmits<{
  ready: [instance: RealGridInstance]
  selectionSummaryChange: [summary: {
    sum: number
    count: number
    average: number
    min: number
    max: number
  }]
  validationError: [errors: { row: number, column: string, message: string }[]]
  pageChange: [page: number]
  loadMore: []
}>()

// ============================================================================
// Refs
// ============================================================================

const gridContainer = ref<HTMLDivElement | null>(null)
const gridInstance = ref<RealGridInstance | null>(null)

// 테마 스토어 (다크/라이트 모드 감지용)
const themeStore = useThemeStore()

// ============================================================================
// Composables 초기화
// ============================================================================

// 컨텍스트 메뉴
const contextMenu = useRealGridContextMenu({
  options: props.options?.contextMenu,
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
  options: props.options?.keyboard,
})

// 상태 저장
const persistence = props.enablePersistence && props.storageKey
  ? useRealGridPersistence({
      options: {
        storageKey: props.storageKey,
        ...props.options?.persistence,
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
    emit('selectionSummaryChange', summary)
  },
})

// 유효성 검사
const validation = props.validations && props.validations.length > 0
  ? useRealGridValidation({
      validations: props.validations,
      options: props.options?.validation,
      onValidationError: (result) => {
        emit('validationError', result.errors)
      },
    })
  : null

// 페이지네이션
const pagination = props.scrollMode === 'pagination'
  ? useRealGridPagination({
      ...props.paginationOptions,
      onPageChange: async (page) => {
        emit('pageChange', page)
        if (props.paginationOptions?.onPageChange) {
          await props.paginationOptions.onPageChange(page)
        }
      },
    })
  : null

// 무한 스크롤
const infiniteScroll = props.scrollMode === 'infinite' && props.loadFn
  ? useRealGridInfiniteScroll({
      ...props.infiniteScrollOptions,
      loadFn: props.loadFn,
    })
  : null

// ============================================================================
// 그리드 초기화
// ============================================================================

const initGrid = async () => {
  if (!gridContainer.value) {
    console.error('[RealGrid] Container not found')
    return
  }

  // 컨테이너 크기 검증
  const rect = gridContainer.value.getBoundingClientRect()
  console.log('[RealGrid] Container size:', rect.width, 'x', rect.height)

  if (rect.height < 50) {
    console.warn('[RealGrid] Container height too small:', rect.height, 'px. Expected:', props.height)
  }

  try {
    // 🚀 RealGrid 지연 초기화 (필요 시점에 로드)
    await initializeRealGrid()

    // 동적 import로 RealGrid 모듈 로드
    const RealGrid = await import('realgrid')

    // RealGrid 인스턴스 생성
    // Note: RealGrid 라이브러리의 TypeScript 타입 정의가 모든 옵션을 포함하지 않으므로 타입 단언 사용
    const gridOptions = {
      // 기본 레이아웃 옵션
      columnMovable: true,
      columnResizable: true,
      defaultColumnWidth: 100,
      fitStyle: 'fill',
      header: { height: 32 },
      // 사용자 옵션 (override 가능)
      ...props.options,
    } as Parameters<typeof RealGrid.default.createGrid>[1]

    // Undo/Redo 지원을 위해 LocalDataProvider와 GridView를 직접 생성
    // Note: LocalDataProvider(true)로 생성해야 undo 이력이 저장됨
    // createGrid()는 내부적으로 LocalDataProvider()를 기본값으로 생성하므로 undo가 작동하지 않음
    // 참고: https://docs.realgrid.com/guides/editing/undo
    const dataProvider = new RealGrid.LocalDataProvider(true) // true: undo 이력 저장
    const gridView = new RealGrid.GridView(gridContainer.value)
    gridView.setDataSource(dataProvider)

    // undoable 설정 (GridView와 DataProvider 모두 필요)
    ;(gridView as unknown as { undoable: boolean }).undoable = true
    ;(dataProvider as unknown as { undoable: boolean }).undoable = true

    // 행 높이 설정 (displayOptions를 통해 설정)
    gridView.displayOptions.rowHeight = 28

    // 필드 설정 (CSV/JSON 내보내기를 위해 필수)
    // 컬럼 정의에서 필드 정보 추출
    const fields: DataFieldObject[] = props.columns.map((col) => ({
      fieldName: col.fieldName || col.name,
      dataType: col.type === 'number' ? 'number' : 'text',
    } as DataFieldObject))
    dataProvider.setFields(fields)

    // 컬럼 설정
    gridView.setColumns(props.columns)

    // 데이터 로드
    if (props.data && props.data.length > 0) {
      dataProvider.setRows(props.data)
    }

    // ========================================
    // 기본 이벤트 핸들러 등록
    // ========================================

    if (props.events.onReady) {
      props.events.onReady(gridView, dataProvider)
    }
    if (props.events.onCellClicked) {
      gridView.onCellClicked = (_grid, clickData) => {
        props.events.onCellClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onCellDblClicked) {
      gridView.onCellDblClicked = (_grid, clickData) => {
        props.events.onCellDblClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onDataCellClicked) {
      gridView.onCellClicked = (_grid, clickData) => {
        props.events.onDataCellClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onCurrentRowChanged) {
      gridView.onCurrentRowChanged = (_grid, oldRow, newRow) => {
        props.events.onCurrentRowChanged?.(gridView, oldRow, newRow)
      }
    }

    // ========================================
    // 확장 기능 설정
    // ========================================

    // 컨텍스트 메뉴
    if (props.enableContextMenu) {
      contextMenu.setupContextMenu(gridView)
      // RealGrid 이벤트 시그니처: (grid: GridBase, item: PopupMenuItem, clickData: ClickData)
      gridView.onContextMenuItemClicked = (grid, menuItem, clickData) => {
        // GridBase → GridView 타입 캐스팅 (GridView는 GridBase를 확장)
        const gv = grid as GridView
        contextMenu.handleContextMenuClick(gv, menuItem as unknown as import('~/types/realgrid').RealGridContextMenuClickData, clickData)
        props.events.onContextMenuClick?.(gv, menuItem as unknown as import('~/types/realgrid').RealGridContextMenuClickData, clickData)
      }
    }

    // 키보드 단축키
    if (props.enableKeyboard) {
      keyboard.setupKeyboard(gridView, dataProvider, gridContainer.value)
    }

    // 상태 저장
    if (persistence) {
      // 저장된 상태 로드
      persistence.loadState(gridView)
      // 자동 저장 설정
      persistence.setupAutoSave(gridView)
    }

    // 선택 요약
    if (props.enableSelectionSummary) {
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
        // 정리
        if (props.enableKeyboard) {
          keyboard.teardownKeyboard()
        }
        if (persistence) {
          persistence.teardownAutoSave(gridView)
        }
        if (props.enableSelectionSummary) {
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

    // ready 이벤트 발생
    emit('ready', instance)
  }
  catch (error) {
    console.error('[RealGrid] Failed to initialize:', error)
    console.error('[RealGrid] Container element:', gridContainer.value)
  }
}

// ============================================================================
// 그리드 데이터 업데이트
// ============================================================================

const updateData = (newData: Record<string, unknown>[]) => {
  if (gridInstance.value) {
    gridInstance.value.dataProvider.setRows(newData)
  }
}

// ============================================================================
// 외부 노출 메서드
// ============================================================================

const getGridInstance = () => gridInstance.value

// 내보내기 메서드
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

// 유효성 검사 메서드
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

// 상태 저장 메서드
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

// 페이지네이션 메서드
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

// 무한 스크롤 메서드
const loadMore = () => {
  if (infiniteScroll) {
    emit('loadMore')
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

// ============================================================================
// Lifecycle hooks
// ============================================================================

onMounted(() => {
  // DOM이 완전히 렌더링된 후 초기화
  nextTick(() => {
    initGrid()
  })
})

onBeforeUnmount(() => {
  if (gridInstance.value) {
    gridInstance.value.destroy()
    gridInstance.value = null
  }
})

// ============================================================================
// Watch
// ============================================================================

// Watch data changes
watch(
  () => props.data,
  (newData) => {
    if (newData && newData.length > 0) {
      updateData(newData)
    }
  },
  { deep: true },
)

// Watch theme changes - 다크/라이트 CSS 전환 시 그리드 새로고침
watch(
  () => themeStore.isDark,
  () => {
    if (gridInstance.value) {
      // 공식 다크/라이트 CSS 전환 후 그리드 새로고침
      gridInstance.value.gridView.refresh()
    }
  },
)

// ============================================================================
// Expose
// ============================================================================

defineExpose({
  // 인스턴스
  getGridInstance,
  updateData,

  // 내보내기
  exportExcel,
  exportCsv,
  exportJson,
  copyToClipboard,

  // 유효성 검사
  validateAll,
  goToFirstError,
  validationErrors: validation?.errors,
  isValid: validation?.isValid,

  // 선택 요약
  selectionSummary: selection.summary,
  getSelectionSum: selection.getSelectionSum,

  // 상태 저장
  saveState,
  loadState,
  clearState,

  // 페이지네이션
  pagination: pagination
    ? {
        state: pagination.paginationState,
        pageNumbers: pagination.pageNumbers,
        canGoPrev: pagination.canGoPrev,
        canGoNext: pagination.canGoNext,
        goToPage,
        nextPage: pagination.nextPage,
        prevPage: pagination.prevPage,
        goToFirst: pagination.goToFirst,
        goToLast: pagination.goToLast,
        setTotalItems,
      }
    : null,

  // 무한 스크롤
  infiniteScroll: infiniteScroll
    ? {
        state: infiniteScroll.state,
        loadMore,
        reset: resetInfiniteScroll,
      }
    : null,

  // Composables (고급 사용자용)
  composables: {
    contextMenu,
    keyboard,
    persistence,
    export: exportComposable,
    selection,
    validation,
    pagination,
    infiniteScroll,
  },
})
</script>

<template>
  <div class="realgrid-wrapper">
    <!-- 그리드 컨테이너 -->
    <div
      ref="gridContainer"
      class="realgrid-container"
      :style="{ height: props.height, minHeight: '200px' }"
      tabindex="0"
    />

    <!-- 선택 요약 상태바 (선택적) -->
    <div
      v-if="enableSelectionSummary && selection.summary.numericCount > 0"
      class="realgrid-status-bar"
    >
      <span class="status-item">
        <strong>합계:</strong> {{ selection.summary.sum.toLocaleString() }}
      </span>
      <span class="status-item">
        <strong>평균:</strong> {{ selection.summary.average.toFixed(2) }}
      </span>
      <span class="status-item">
        <strong>최대:</strong> {{ selection.summary.max.toLocaleString() }}
      </span>
      <span class="status-item">
        <strong>최소:</strong> {{ selection.summary.min.toLocaleString() }}
      </span>
      <span class="status-item">
        <strong>개수:</strong> {{ selection.summary.count }}
      </span>
    </div>

    <!-- 페이지네이션 UI (선택적) -->
    <div
      v-if="scrollMode === 'pagination' && pagination"
      class="realgrid-pagination"
    >
      <button
        class="pagination-btn"
        :disabled="!pagination.canGoPrev.value"
        @click="pagination.goToFirst()"
      >
        |&lt;
      </button>
      <button
        class="pagination-btn"
        :disabled="!pagination.canGoPrev.value"
        @click="pagination.prevPage()"
      >
        &lt;
      </button>

      <button
        v-for="page in pagination.pageNumbers.value"
        :key="page"
        class="pagination-btn"
        :class="{ active: page === pagination.paginationState.currentPage }"
        @click="pagination.goToPage(page)"
      >
        {{ page }}
      </button>

      <button
        class="pagination-btn"
        :disabled="!pagination.canGoNext.value"
        @click="pagination.nextPage()"
      >
        &gt;
      </button>
      <button
        class="pagination-btn"
        :disabled="!pagination.canGoNext.value"
        @click="pagination.goToLast()"
      >
        &gt;|
      </button>

      <span class="pagination-info">
        {{ pagination.paginationState.currentPage }} / {{ pagination.totalPages.value }} 페이지
        (총 {{ pagination.paginationState.totalItems.toLocaleString() }}건)
      </span>
    </div>

    <!-- 무한 스크롤 로딩 표시 (선택적) -->
    <div
      v-if="scrollMode === 'infinite' && infiniteScroll?.state.isLoading"
      class="realgrid-loading"
    >
      <span class="loading-spinner" />
      <span>데이터 로딩 중...</span>
    </div>
  </div>
</template>

<style scoped>
.realgrid-wrapper {
  width: 100%;
  /* height 제거: 부모 높이 없으면 0px 되는 문제 방지 */
  /* flex 레이아웃 제거: 자식 컨텐츠에 따라 자동 조정 */
}

.realgrid-container {
  width: 100%;
  /* flex: 1 제거: inline style height와 충돌 방지 */
  min-height: 200px; /* 최소 높이 보장 */
}

/* 상태바 스타일 */
.realgrid-status-bar {
  display: flex;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: var(--color-surface-secondary, #f5f5f5);
  border-top: 1px solid var(--color-border, #e0e0e0);
  font-size: 0.875rem;
}

.status-item {
  display: inline-flex;
  gap: 0.25rem;
}

/* 페이지네이션 스타일 */
.realgrid-pagination {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  background: var(--color-surface-secondary, #f5f5f5);
  border-top: 1px solid var(--color-border, #e0e0e0);
}

.pagination-btn {
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border, #e0e0e0);
  background: var(--color-surface, #fff);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: var(--color-surface-hover, #e8e8e8);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-btn.active {
  background: var(--color-primary, #1976d2);
  color: white;
  border-color: var(--color-primary, #1976d2);
  font-weight: bold;
}

.pagination-info {
  margin-left: 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary, #666);
}

/* 로딩 스타일 */
.realgrid-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--color-surface-secondary, #f5f5f5);
  border-top: 1px solid var(--color-border, #e0e0e0);
}

.loading-spinner {
  width: 1rem;
  height: 1rem;
  border: 2px solid var(--color-border, #e0e0e0);
  border-top-color: var(--color-primary, #1976d2);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 다크 모드 지원 */
:root.dark .realgrid-status-bar,
:root.dark .realgrid-pagination,
:root.dark .realgrid-loading {
  background: var(--color-surface-secondary, #2a2a2a);
  border-color: var(--color-border, #444);
}

:root.dark .pagination-btn {
  background: var(--color-surface, #333);
  border-color: var(--color-border, #444);
  color: var(--color-text, #fff);
}

:root.dark .pagination-btn:hover:not(:disabled) {
  background: var(--color-surface-hover, #444);
}
</style>
