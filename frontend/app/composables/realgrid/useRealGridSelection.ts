/**
 * RealGrid 선택 영역 관리 Composable
 *
 * 선택된 셀들의 요약 정보:
 * - 합계, 평균, 최대, 최소, 개수
 * - 실시간 업데이트
 * - 숫자 컬럼 자동 감지
 */

import type { GridView } from 'realgrid'
import type {
  RealGridSelectionSummary,
  RealGridSelectionOptions,
} from '~/types/realgrid'

// ============================================================================
// 선택 요약 유틸리티
// ============================================================================

/**
 * 선택된 데이터에서 숫자값 추출
 */
const extractNumericValues = (
  grid: GridView,
  selectionData: Record<string, unknown>[],
): number[] => {
  const values: number[] = []

  for (const row of selectionData) {
    for (const [key, value] of Object.entries(row)) {
      // __rowId 제외
      if (key === '__rowId') continue

      // 컬럼이 숫자 타입인지 확인
      const column = grid.columnByName(key)
      if (column?.valueType === 'number' || typeof value === 'number') {
        const numValue = typeof value === 'number' ? value : Number(value)
        if (!Number.isNaN(numValue)) {
          values.push(numValue)
        }
      }
    }
  }

  return values
}

/**
 * 숫자 배열에서 요약 통계 계산
 */
const calculateSummary = (values: number[]): RealGridSelectionSummary => {
  if (values.length === 0) {
    return {
      sum: 0,
      count: 0,
      average: 0,
      min: 0,
      max: 0,
      numericCount: 0,
    }
  }

  const sum = values.reduce((acc, val) => acc + val, 0)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const average = sum / values.length

  return {
    sum,
    count: values.length,
    average,
    min,
    max,
    numericCount: values.length,
  }
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridSelectionInput {
  options?: RealGridSelectionOptions
  onSummaryChange?: (summary: RealGridSelectionSummary) => void
}

/**
 * RealGrid 선택 영역 관리 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { setupSelection, summary, getSummary, formatSummary } = useRealGridSelection({
 *   options: { numericOnly: true },
 *   onSummaryChange: (s) => console.log('Selection changed:', s)
 * })
 *
 * // 그리드 초기화 후
 * setupSelection(gridView)
 * </script>
 *
 * <template>
 *   <div class="status-bar">
 *     합계: {{ summary.sum.toLocaleString() }} |
 *     평균: {{ summary.average.toFixed(2) }} |
 *     개수: {{ summary.count }}
 *   </div>
 * </template>
 * ```
 */
export const useRealGridSelection = (input: UseRealGridSelectionInput = {}) => {
  const options = ref<RealGridSelectionOptions>(
    input.options || {
      includeHidden: false,
      numericOnly: true,
    },
  )

  // 현재 요약 상태
  const summary = reactive<RealGridSelectionSummary>({
    sum: 0,
    count: 0,
    average: 0,
    min: 0,
    max: 0,
    numericCount: 0,
  })

  // 선택된 셀 수
  const selectedCellCount = ref(0)

  // 선택된 행 수
  const selectedRowCount = ref(0)

  // 그리드 참조
  let gridRef: GridView | null = null

  /**
   * 요약 업데이트
   */
  const updateSummary = (grid: GridView): void => {
    const selectionData = grid.getSelectionData()

    if (!selectionData || selectionData.length === 0) {
      // 선택 없음 - 초기화
      Object.assign(summary, {
        sum: 0,
        count: 0,
        average: 0,
        min: 0,
        max: 0,
        numericCount: 0,
      })
      selectedCellCount.value = 0
      selectedRowCount.value = 0
      return
    }

    // 숫자값 추출 및 요약 계산
    const values = extractNumericValues(grid, selectionData)
    const newSummary = calculateSummary(values)

    // 상태 업데이트
    Object.assign(summary, newSummary)
    selectedRowCount.value = selectionData.length

    // 셀 개수 계산
    let cellCount = 0
    for (const row of selectionData) {
      cellCount += Object.keys(row).filter(k => k !== '__rowId').length
    }
    selectedCellCount.value = cellCount

    // 콜백 호출
    if (input.onSummaryChange) {
      input.onSummaryChange(newSummary)
    }
  }

  /**
   * 선택 이벤트 설정
   */
  const setupSelection = (grid: GridView): void => {
    gridRef = grid

    // 선택 변경 이벤트
    grid.onSelectionChanged = () => {
      updateSummary(grid)
    }

    // 선택 해제 이벤트
    grid.onSelectionEnded = () => {
      updateSummary(grid)
    }
  }

  /**
   * 선택 이벤트 해제
   */
  const teardownSelection = (grid: GridView): void => {
    grid.onSelectionChanged = undefined
    grid.onSelectionEnded = undefined
    gridRef = null
  }

  /**
   * 현재 요약 가져오기 (수동 호출)
   */
  const getSummary = (grid?: GridView): RealGridSelectionSummary => {
    const targetGrid = grid || gridRef
    if (targetGrid) {
      updateSummary(targetGrid)
    }
    return { ...summary }
  }

  /**
   * 레거시 호환: 합계만 반환
   * (기존 getSelectionSummary 함수와 호환)
   */
  const getSelectionSum = (grid?: GridView): number => {
    return getSummary(grid).sum
  }

  /**
   * 요약 포맷팅 (표시용)
   */
  const formatSummary = (
    format: 'full' | 'compact' | 'sum-only' = 'compact',
  ): string => {
    if (summary.numericCount === 0) {
      return ''
    }

    switch (format) {
      case 'sum-only':
        return `합계: ${summary.sum.toLocaleString()}`

      case 'compact':
        return `합: ${summary.sum.toLocaleString()} | 평균: ${summary.average.toFixed(2)} | 개수: ${summary.count}`

      case 'full':
        return [
          `합계: ${summary.sum.toLocaleString()}`,
          `평균: ${summary.average.toFixed(2)}`,
          `최대: ${summary.max.toLocaleString()}`,
          `최소: ${summary.min.toLocaleString()}`,
          `개수: ${summary.count}`,
        ].join(' | ')

      default:
        return ''
    }
  }

  /**
   * 선택된 데이터 가져오기
   */
  const getSelectedData = <T = Record<string, unknown>>(grid?: GridView): T[] => {
    const targetGrid = grid || gridRef
    if (!targetGrid) {
      return []
    }
    return (targetGrid.getSelectionData() || []) as T[]
  }

  /**
   * 선택된 행 인덱스 가져오기
   */
  const getSelectedRows = (grid?: GridView): number[] => {
    const targetGrid = grid || gridRef
    if (!targetGrid) {
      return []
    }

    const selection = targetGrid.getSelection()
    if (!selection) {
      return []
    }

    const rows: number[] = []
    for (let i = selection.startRow || 0; i <= (selection.endRow || 0); i++) {
      rows.push(i)
    }
    return rows
  }

  /**
   * 선택 클리어
   */
  const clearSelection = (grid?: GridView): void => {
    const targetGrid = grid || gridRef
    if (targetGrid) {
      targetGrid.clearSelection()
      updateSummary(targetGrid)
    }
  }

  /**
   * 전체 선택
   */
  const selectAll = (grid?: GridView): void => {
    const targetGrid = grid || gridRef
    if (targetGrid) {
      const rowCount = targetGrid.getItemCount()
      const columns = targetGrid.getColumns()
      const colCount = columns.length
      if (rowCount > 0 && colCount > 0) {
        const firstCol = columns[0]?.name ?? ''
        const lastCol = columns[colCount - 1]?.name ?? ''
        targetGrid.setSelection({
          style: 'block' as const,
          startRow: 0,
          startColumn: firstCol,
          endRow: rowCount - 1,
          endColumn: lastCol,
        } as Parameters<typeof targetGrid.setSelection>[0])
        updateSummary(targetGrid)
      }
    }
  }

  /**
   * 컬럼 전체 선택
   */
  const selectColumn = (columnName: string, grid?: GridView): void => {
    const targetGrid = grid || gridRef
    if (targetGrid) {
      const column = targetGrid.columnByName(columnName)
      if (column) {
        const rowCount = targetGrid.getItemCount()
        targetGrid.setSelection({
          style: 'columns' as const,
          startRow: 0,
          startColumn: columnName,
          endRow: rowCount - 1,
          endColumn: columnName,
        } as Parameters<typeof targetGrid.setSelection>[0])
        updateSummary(targetGrid)
      }
    }
  }

  /**
   * 행 전체 선택
   */
  const selectRow = (rowIndex: number, grid?: GridView): void => {
    const targetGrid = grid || gridRef
    if (targetGrid) {
      const columns = targetGrid.getColumns()
      const colCount = columns.length
      if (colCount > 0) {
        const firstCol = columns[0]?.name ?? ''
        const lastCol = columns[colCount - 1]?.name ?? ''
        targetGrid.setSelection({
          style: 'rows' as const,
          startRow: rowIndex,
          startColumn: firstCol,
          endRow: rowIndex,
          endColumn: lastCol,
        } as Parameters<typeof targetGrid.setSelection>[0])
        updateSummary(targetGrid)
      }
    }
  }

  /**
   * 옵션 업데이트
   */
  const updateOptions = (newOptions: Partial<RealGridSelectionOptions>): void => {
    options.value = { ...options.value, ...newOptions }
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    gridRef = null
  })

  return {
    // 설정
    setupSelection,
    teardownSelection,

    // 요약
    summary: readonly(summary),
    getSummary,
    getSelectionSum, // 레거시 호환
    formatSummary,

    // 선택 정보
    selectedCellCount: readonly(selectedCellCount),
    selectedRowCount: readonly(selectedRowCount),
    getSelectedData,
    getSelectedRows,

    // 선택 조작
    clearSelection,
    selectAll,
    selectColumn,
    selectRow,

    // 옵션
    updateOptions,
    options: readonly(options),
  }
}

export default useRealGridSelection
