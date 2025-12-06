/**
 * RealGrid 상태 저장 Composable
 *
 * 그리드 상태를 localStorage/sessionStorage에 저장:
 * - 컬럼 순서, 너비, 표시 상태
 * - 정렬 상태
 * - 필터 상태
 * - 고정(freeze) 설정
 * - 행 높이
 */

import type { GridView } from 'realgrid'
import type {
  RealGridPersistedState,
  RealGridPersistenceOptions,
} from '~/types/realgrid'

// RealGrid 타입 정의에 없는 속성들을 위한 인터페이스
interface SortingColumnInfo {
  column: string
  ascending: boolean
}

// ============================================================================
// 상태 저장/복원 유틸리티
// ============================================================================

/**
 * 현재 그리드 상태 추출
 */
const extractGridState = (
  grid: GridView,
  options: RealGridPersistenceOptions,
): RealGridPersistedState => {
  const state: RealGridPersistedState = {
    columns: [],
    timestamp: Date.now(),
  }

  // 컬럼 상태
  if (options.saveColumns) {
    const columns = grid.getColumns()
    state.columns = columns.map((col, index) => ({
      name: col.name || '',
      width: col.width || 100,
      visible: col.visible !== false,
      sortOrder: index,
    }))
  }

  // 고정 옵션
  if (options.saveFixedOptions) {
    const fixedOptions = grid.fixedOptions
    state.fixedOptions = {
      colCount: fixedOptions.colCount || 0,
      rowCount: fixedOptions.rowCount || 0,
    }
  }

  // 정렬 상태
  if (options.saveSorting) {
    // getSortingColumns는 타입 정의에 없을 수 있음
    const g = grid as unknown as Record<string, unknown>
    if (typeof g.getSortingColumns === 'function') {
      const sortingColumns = g.getSortingColumns() as SortingColumnInfo[]
      if (sortingColumns && sortingColumns.length > 0) {
        state.sorting = sortingColumns.map(col => ({
          column: col.column,
          direction: col.ascending ? 'asc' : 'desc',
        }))
      }
    }
  }

  // 필터 상태
  if (options.saveFilters) {
    // RealGrid 필터 상태 추출
    const columns = grid.getColumns()
    const filters: Record<string, unknown> = {}

    for (const col of columns) {
      if (col.name) {
        const columnFilters = grid.getColumnFilters(col.name)
        if (columnFilters && columnFilters.length > 0) {
          filters[col.name] = columnFilters
        }
      }
    }

    if (Object.keys(filters).length > 0) {
      state.filters = filters
    }
  }

  // 행 높이
  if (options.saveRowHeight) {
    state.rowHeight = grid.displayOptions.rowHeight
  }

  return state
}

/**
 * 저장된 상태를 그리드에 적용
 */
const applyGridState = (
  grid: GridView,
  state: RealGridPersistedState,
  options: RealGridPersistenceOptions,
): void => {
  // 컬럼 상태 복원
  if (options.saveColumns && state.columns.length > 0) {
    for (const colState of state.columns) {
      if (colState.name) {
        grid.setColumnProperty(colState.name, 'width', colState.width)
        grid.setColumnProperty(colState.name, 'visible', colState.visible)
      }
    }

    // 컬럼 순서 복원
    const orderedNames = state.columns
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      .map(c => c.name)
      .filter(Boolean)

    if (orderedNames.length > 0) {
      // setColumnOrder는 타입 정의에 없을 수 있음
      const g = grid as unknown as Record<string, unknown>
      if (typeof g.setColumnOrder === 'function') {
        g.setColumnOrder(orderedNames)
      }
    }
  }

  // 고정 옵션 복원
  if (options.saveFixedOptions && state.fixedOptions) {
    grid.setFixedOptions({
      colCount: state.fixedOptions.colCount,
      rowCount: state.fixedOptions.rowCount,
    })
  }

  // 정렬 상태 복원
  if (options.saveSorting && state.sorting && state.sorting.length > 0) {
    const sortColumns = state.sorting.map(s => s.column)
    const sortDirs = state.sorting.map(s => s.direction === 'asc' ? 'ascending' : 'descending')
    grid.orderBy(sortColumns, sortDirs as Parameters<typeof grid.orderBy>[1])
  }

  // 필터 상태 복원
  if (options.saveFilters && state.filters) {
    for (const [columnName, filters] of Object.entries(state.filters)) {
      if (Array.isArray(filters)) {
        grid.setColumnFilters(columnName, filters)
      }
    }
  }

  // 행 높이 복원
  if (options.saveRowHeight && state.rowHeight) {
    grid.displayOptions.rowHeight = state.rowHeight
  }
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridPersistenceInput {
  options: RealGridPersistenceOptions
}

/**
 * RealGrid 상태 저장 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { saveState, loadState, clearState, setupAutoSave } = useRealGridPersistence({
 *   options: {
 *     storageKey: 'my-grid-state',
 *     storage: 'localStorage',
 *     saveColumns: true,
 *     saveFilters: true,
 *     saveSorting: true,
 *     saveFixedOptions: true,
 *     saveRowHeight: true,
 *     autoSave: true,
 *     autoSaveDelay: 1000
 *   }
 * })
 *
 * // 그리드 초기화 후
 * loadState(gridView) // 저장된 상태 복원
 * setupAutoSave(gridView) // 자동 저장 설정
 * </script>
 * ```
 */
export const useRealGridPersistence = (input: UseRealGridPersistenceInput) => {
  const defaultOptions: Partial<RealGridPersistenceOptions> = {
    storage: 'localStorage',
    saveColumns: true,
    saveFilters: true,
    saveSorting: true,
    saveFixedOptions: true,
    saveRowHeight: true,
    autoSave: false,
    autoSaveDelay: 1000,
  }

  const options = ref<RealGridPersistenceOptions>({
    ...defaultOptions,
    ...input.options,
  })

  // 자동 저장 타이머
  let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

  // 스토리지 접근
  const getStorage = (): Storage => {
    return options.value.storage === 'sessionStorage'
      ? sessionStorage
      : localStorage
  }

  /**
   * 상태 저장
   */
  const saveState = (grid: GridView): void => {
    try {
      const state = extractGridState(grid, options.value)
      const storage = getStorage()
      storage.setItem(options.value.storageKey, JSON.stringify(state))
    }
    catch (error) {
      console.error('Failed to save grid state:', error)
    }
  }

  /**
   * 상태 로드
   */
  const loadState = (grid: GridView): boolean => {
    try {
      const storage = getStorage()
      const savedState = storage.getItem(options.value.storageKey)

      if (!savedState) {
        return false
      }

      const state: RealGridPersistedState = JSON.parse(savedState)
      applyGridState(grid, state, options.value)
      return true
    }
    catch (error) {
      console.error('Failed to load grid state:', error)
      return false
    }
  }

  /**
   * 저장된 상태 삭제
   */
  const clearState = (): void => {
    try {
      const storage = getStorage()
      storage.removeItem(options.value.storageKey)
    }
    catch (error) {
      console.error('Failed to clear grid state:', error)
    }
  }

  /**
   * 저장된 상태 존재 여부 확인
   */
  const hasState = (): boolean => {
    try {
      const storage = getStorage()
      return storage.getItem(options.value.storageKey) !== null
    }
    catch {
      return false
    }
  }

  /**
   * 저장된 상태 조회 (적용하지 않고)
   */
  const getState = (): RealGridPersistedState | null => {
    try {
      const storage = getStorage()
      const savedState = storage.getItem(options.value.storageKey)
      return savedState ? JSON.parse(savedState) : null
    }
    catch {
      return null
    }
  }

  /**
   * 디바운스된 자동 저장
   */
  const debouncedSave = (grid: GridView): void => {
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
    }

    autoSaveTimer = setTimeout(() => {
      saveState(grid)
    }, options.value.autoSaveDelay || 1000)
  }

  /**
   * 자동 저장 설정
   */
  const setupAutoSave = (grid: GridView): void => {
    if (!options.value.autoSave) {
      return
    }

    // 컬럼 리사이즈 이벤트 (타입 정의에 없을 수 있음)
    const g = grid as unknown as Record<string, unknown>
    g.onColumnResized = () => {
      debouncedSave(grid)
    }

    // 컬럼 이동 이벤트
    g.onColumnMoved = () => {
      debouncedSave(grid)
    }

    // 정렬 변경 이벤트
    grid.onSortingChanged = () => {
      debouncedSave(grid)
    }

    // 필터 변경 이벤트
    grid.onFilteringChanged = () => {
      debouncedSave(grid)
    }

    // 컬럼 표시/숨김 변경
    grid.onColumnPropertyChanged = (g, column, property) => {
      if (property === 'visible') {
        debouncedSave(grid)
      }
    }
  }

  /**
   * 자동 저장 해제
   */
  const teardownAutoSave = (grid: GridView): void => {
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
      autoSaveTimer = null
    }

    // 이벤트 핸들러 제거
    const g = grid as unknown as Record<string, unknown>
    g.onColumnResized = undefined
    g.onColumnMoved = undefined
    grid.onSortingChanged = undefined
    grid.onFilteringChanged = undefined
    grid.onColumnPropertyChanged = undefined
  }

  /**
   * 옵션 업데이트
   */
  const updateOptions = (newOptions: Partial<RealGridPersistenceOptions>): void => {
    options.value = { ...options.value, ...newOptions }
  }

  /**
   * 상태 내보내기 (파일 다운로드)
   */
  const exportState = (grid: GridView, fileName = 'grid-state.json'): void => {
    try {
      const state = extractGridState(grid, options.value)
      const blob = new Blob([JSON.stringify(state, null, 2)], {
        type: 'application/json',
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      link.click()
      URL.revokeObjectURL(url)
    }
    catch (error) {
      console.error('Failed to export grid state:', error)
    }
  }

  /**
   * 상태 가져오기 (파일 업로드)
   */
  const importState = async (grid: GridView, file: File): Promise<boolean> => {
    try {
      const text = await file.text()
      const state: RealGridPersistedState = JSON.parse(text)
      applyGridState(grid, state, options.value)
      return true
    }
    catch (error) {
      console.error('Failed to import grid state:', error)
      return false
    }
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
    }
  })

  return {
    // 상태 관리
    saveState,
    loadState,
    clearState,
    hasState,
    getState,

    // 자동 저장
    setupAutoSave,
    teardownAutoSave,

    // 옵션
    updateOptions,
    options: readonly(options),

    // 파일 내보내기/가져오기
    exportState,
    importState,
  }
}

export default useRealGridPersistence
