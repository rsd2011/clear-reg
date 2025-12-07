<script setup lang="ts">
/**
 * RealGrid 확장 컴포넌트
 *
 * Clean Architecture로 리팩터링된 RealGrid 컴포넌트입니다.
 * - 프리셋: config/realgrid-presets.ts
 * - 초기화 로직: composables/realgrid/useRealGridSetup.ts
 * - UI 컴포넌트: RealGridPagination, RealGridStatusBar, RealGridErrorBoundary, RealGridLoading
 */

import { useThemeStore } from '~/stores/theme'
import { useRealGridSetup } from '~/composables/realgrid'
import type {
  RealGridColumn,
  RealGridEventsExtended,
  RealGridInstance,
  RealGridOptionsExtended,
  RealGridColumnValidation,
  RealGridPaginationOptions,
  RealGridInfiniteScrollOptions,
  InfiniteScrollLoadFn,
  RealGridPreset,
} from '~/types/realgrid'
import type { UseRealGridPaginationOptions } from '~/composables/realgrid'

// UI 컴포넌트
import RealGridErrorBoundary from './RealGridErrorBoundary.vue'
import RealGridStatusBar from './RealGridStatusBar.vue'
import RealGridPagination from './RealGridPagination.vue'
import RealGridLoading from './RealGridLoading.vue'

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

  /**
   * 그리드 프리셋
   * - 'default': 기본 설정 (편집 가능, 추천 설정 적용)
   * - 'editable': 편집 최적화 (셀 단위 커밋, 소프트 삭제)
   * - 'readonly': 읽기 전용 (편집 비활성화, 선택/복사만)
   * - 'search': 대용량 검색용 (필터 패널, Undo 비활성화, 스크롤 최적화)
   * - 'reporting': 리포팅용 (고정열/행, 병합셀, 편집 비활성화)
   */
  preset?: RealGridPreset

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
  preset: 'default',
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
// 테마 스토어
// ============================================================================

const themeStore = useThemeStore()

// ============================================================================
// useRealGridSetup 활용
// ============================================================================

const {
  gridContainer,
  gridInstance,
  initError,
  isRetrying,
  initGrid,
  retryInit,
  updateData,
  getGridInstance,
  exportExcel,
  exportCsv,
  exportJson,
  copyToClipboard,
  validateAll,
  goToFirstError,
  saveState,
  loadState,
  clearState,
  goToPage,
  setTotalItems,
  loadMore,
  resetInfiniteScroll,
  // Composables
  contextMenu,
  keyboard,
  persistence,
  exportComposable,
  selection,
  validation,
  pagination,
  infiniteScroll,
} = useRealGridSetup({
  columns: props.columns,
  options: props.options,
  events: props.events,
  preset: props.preset,
  features: {
    contextMenu: props.enableContextMenu,
    keyboard: props.enableKeyboard,
    persistence: props.enablePersistence ? props.storageKey : false,
    selection: props.enableSelectionSummary,
    validations: props.validations,
  },
  scroll: {
    mode: props.scrollMode,
    pagination: props.paginationOptions,
    infiniteScroll: props.infiniteScrollOptions,
    loadFn: props.loadFn,
  },
  callbacks: {
    onReady: (instance) => emit('ready', instance),
    onSelectionSummaryChange: (summary) => emit('selectionSummaryChange', summary),
    onValidationError: (errors) => emit('validationError', errors),
    onPageChange: (page) => emit('pageChange', page),
    onLoadMore: () => emit('loadMore'),
  },
})

// ============================================================================
// Lifecycle hooks
// ============================================================================

onMounted(() => {
  nextTick(async () => {
    await initGrid()
    // 초기화 완료 후 초기 데이터 로드
    if (props.data && props.data.length > 0) {
      updateData(props.data)
    }
  })
})

onBeforeUnmount(() => {
  if (gridInstance.value) {
    gridInstance.value.destroy()
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
  { deep: true, immediate: true },
)

// Watch theme changes
watch(
  () => themeStore.isDark,
  () => {
    if (gridInstance.value) {
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
    <!-- 에러 바운더리 -->
    <RealGridErrorBoundary
      v-if="initError"
      :error="initError"
      :is-retrying="isRetrying"
      :height="height"
      @retry="retryInit"
    />

    <!-- 그리드 컨테이너 -->
    <div
      v-show="!initError"
      ref="gridContainer"
      class="realgrid-container"
      :style="{ height, minHeight: '200px' }"
      tabindex="0"
    />

    <!-- 선택 요약 상태바 -->
    <RealGridStatusBar
      v-if="enableSelectionSummary"
      :summary="selection.summary"
    />

    <!-- 페이지네이션 UI -->
    <RealGridPagination
      v-if="scrollMode === 'pagination' && pagination"
      :current-page="pagination.paginationState.currentPage"
      :total-pages="pagination.totalPages.value"
      :page-numbers="pagination.pageNumbers.value"
      :can-go-prev="pagination.canGoPrev.value"
      :can-go-next="pagination.canGoNext.value"
      :total-items="pagination.paginationState.totalItems"
      @first="pagination.goToFirst()"
      @prev="pagination.prevPage()"
      @next="pagination.nextPage()"
      @last="pagination.goToLast()"
      @go-to-page="pagination.goToPage"
    />

    <!-- 무한 스크롤 로딩 표시 -->
    <RealGridLoading
      v-if="scrollMode === 'infinite' && infiniteScroll?.state.isLoading"
    />
  </div>
</template>

<style scoped>
@import './styles/realgrid-base.css';
</style>
