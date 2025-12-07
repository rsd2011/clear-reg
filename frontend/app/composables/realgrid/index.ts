/**
 * RealGrid Composables 모음
 *
 * RealGrid2 그리드 라이브러리를 위한 Vue 3 Composition API 유틸리티
 */

// ============================================================================
// 통합 Composable (권장)
// ============================================================================
export { useRealGrid } from './useRealGrid'
export type { UseRealGridOptions, UseRealGridReturn } from './useRealGrid'

// ============================================================================
// Setup Composable (RealGrid 컴포넌트용)
// ============================================================================
export { useRealGridSetup } from './useRealGridSetup'
export type { UseRealGridSetupOptions, UseRealGridSetupReturn } from './useRealGridSetup'

// ============================================================================
// 컨텍스트 메뉴
// ============================================================================
export { useRealGridContextMenu } from './useRealGridContextMenu'
export type { UseRealGridContextMenuOptions } from './useRealGridContextMenu'

// ============================================================================
// 엑셀/CSV 내보내기
// ============================================================================
export { useRealGridExport } from './useRealGridExport'
export type { UseRealGridExportInput } from './useRealGridExport'

// ============================================================================
// 키보드 단축키
// ============================================================================
export { useRealGridKeyboard } from './useRealGridKeyboard'
export type { UseRealGridKeyboardInput } from './useRealGridKeyboard'

// ============================================================================
// 페이지네이션 (단일 책임 원칙에 따라 분리)
// ============================================================================
export { useRealGridPagination } from './useRealGridPagination'
export type { UseRealGridPaginationOptions } from './useRealGridPagination'

// ============================================================================
// 무한 스크롤
// ============================================================================
export { useRealGridInfiniteScroll } from './useRealGridInfiniteScroll'
export type { UseRealGridInfiniteScrollOptions } from './useRealGridInfiniteScroll'

// ============================================================================
// 통합 스크롤 (페이지네이션 + 무한 스크롤)
// ============================================================================
export { useRealGridScroll } from './useRealGridScroll'
export type { UseRealGridScrollOptions, ScrollMode } from './useRealGridScroll'

// ============================================================================
// 상태 저장
// ============================================================================
export { useRealGridPersistence } from './useRealGridPersistence'
export type { UseRealGridPersistenceInput } from './useRealGridPersistence'

// ============================================================================
// 선택 영역 관리
// ============================================================================
export { useRealGridSelection } from './useRealGridSelection'
export type { UseRealGridSelectionInput } from './useRealGridSelection'

// ============================================================================
// 유틸리티 (순수 함수는 ~/utils/realgrid에서 직접 import 권장)
// ============================================================================
export {
  useRealGridUtils,
  // 순수 함수들도 re-export (하위 호환성)
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
} from './useRealGridUtils'

// ============================================================================
// 유효성 검사
// ============================================================================
export { useRealGridValidation } from './useRealGridValidation'
export type { UseRealGridValidationInput } from './useRealGridValidation'
