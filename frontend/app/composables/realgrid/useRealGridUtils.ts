/**
 * RealGrid 유틸리티 Composable
 *
 * 순수 유틸리티 함수들을 Composable 패턴으로 래핑하여 제공
 * 실제 구현은 ~/utils/realgrid.ts에 있음
 */

// 순수 함수들을 utils에서 re-export
// 순수 함수들 import
import {
  getMultiLevelColumns,
  setFieldsAndColumns,
  columnsToFields,
  getColumnsToFormModel,
  extractGridData,
  updateGridData,
  toggleColumnVisibility,
  showAllColumns,
  autoFitColumnWidth,
  getCurrentRowData,
  getCheckedRowsData,
  setRowHeight,
} from '~/utils/realgrid'

export {
  getMultiLevelColumns,
  setFieldsAndColumns,
  columnsToFields,
  getColumnsToFormModel,
  extractGridData,
  updateGridData,
  toggleColumnVisibility,
  showAllColumns,
  autoFitColumnWidth,
  getCurrentRowData,
  getCheckedRowsData,
  setRowHeight,
} from '~/utils/realgrid'

// ============================================================================
// Composable Export
// ============================================================================

/**
 * RealGrid 유틸리티 Composable
 *
 * @deprecated 순수 함수를 직접 import하여 사용하는 것을 권장합니다.
 * `import { getMultiLevelColumns, setFieldsAndColumns } from '~/utils/realgrid'`
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * // 권장: 직접 import
 * import { getMultiLevelColumns, setFieldsAndColumns } from '~/utils/realgrid'
 *
 * // 레거시: Composable 사용
 * const {
 *   getMultiLevelColumns,
 *   setFieldsAndColumns,
 *   getColumnsToFormModel,
 *   extractGridData,
 *   setRowHeight
 * } = useRealGridUtils()
 * </script>
 * ```
 */
export const useRealGridUtils = () => {
  return {
    // 멀티레벨 컬럼
    getMultiLevelColumns,

    // 동적 필드/컬럼
    setFieldsAndColumns,
    columnsToFields,

    // 폼 모델
    getColumnsToFormModel,

    // 데이터 유틸리티
    extractGridData,
    updateGridData,

    // 컬럼 유틸리티
    toggleColumnVisibility,
    showAllColumns,
    autoFitColumnWidth,

    // 행 유틸리티
    getCurrentRowData,
    getCheckedRowsData,
    setRowHeight,
  }
}

export default useRealGridUtils
