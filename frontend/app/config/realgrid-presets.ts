/**
 * RealGrid 프리셋 설정
 *
 * 그리드 사용 목적에 따른 최적화된 기본 설정을 정의합니다.
 * - default: 기본 설정 (편집 가능, 추천 설정 적용)
 * - editable: 편집 최적화 (셀 단위 커밋, 소프트 삭제, 복사/붙여넣기 강화)
 * - readonly: 읽기 전용 (편집 비활성화, 선택/복사만 허용)
 * - search: 대용량 데이터 검색용 (필터 패널, 성능 최적화, Undo 비활성화)
 */

import type { GridView, LocalDataProvider } from 'realgrid'
import { SortMode, FilterMode, GridFitStyle } from 'realgrid'
import type { RealGridPreset, RealGridPresetConfig } from '~/types/realgrid'

// ============================================================================
// 프리셋 설정 정의
// ============================================================================

/**
 * 프리셋별 기본 설정 정의
 */
export const PRESET_CONFIGS: Record<RealGridPreset, RealGridPresetConfig> = {
  default: {
    editable: true,
    softDeleting: true,
    hideDeletedRows: false,
    copyOptions: { lookupDisplay: true },
    pasteOptions: {
      enabled: true,
      convertLookupLabel: true,
      checkDomainOnly: true,
      checkReadOnly: true,
    },
  },
  editable: {
    editable: true,
    softDeleting: true,
    hideDeletedRows: false,
    copyOptions: { lookupDisplay: true },
    pasteOptions: {
      enabled: true,
      convertLookupLabel: true,
      checkDomainOnly: true,
      checkReadOnly: true,
    },
  },
  readonly: {
    editable: false,
    softDeleting: false,
    hideDeletedRows: true,
    copyOptions: { lookupDisplay: true },
    pasteOptions: {
      enabled: false,
      convertLookupLabel: false,
      checkDomainOnly: false,
      checkReadOnly: true,
    },
    undoable: false, // 편집 불가 → Undo 불필요 (메모리 절약)
  },
  search: {
    editable: false,
    softDeleting: false,
    hideDeletedRows: true,
    copyOptions: { lookupDisplay: true },
    pasteOptions: {
      enabled: false,
      convertLookupLabel: false,
      checkDomainOnly: false,
      checkReadOnly: true,
    },
    // 🔍 대용량 데이터 검색 최적화
    filterPanel: {
      visible: true,
      height: 32,
      filterDelay: 300, // 입력 후 300ms 후 필터 적용 (타이핑 중 과도한 필터링 방지)
      ignoreCase: true, // 대소문자 무시
      trimData: true, // 앞뒤 공백 제거
    },
    undoable: false, // Undo 비활성화 (메모리 절약)
    refreshMode: 'recycle', // 스크롤 성능 최적화
  },
}

// ============================================================================
// 프리셋 적용 함수
// ============================================================================

/**
 * RealGrid 추천 설정 적용
 * @see https://docs.realgrid.com/tutorial/realgrid-recommended-options
 */
export function applyRecommendedOptions(gridView: GridView): void {
  // Phase 1: 핵심 편집 옵션
  // 1-1. 명시적 정렬/필터 모드 (자동 재정렬 방지)
  gridView.sortMode = SortMode.EXPLICIT
  gridView.filterMode = FilterMode.EXPLICIT

  // 1-2. 편집 완료 방식 설정
  gridView.editOptions.commitByCell = true
  gridView.editOptions.commitWhenLeave = true
  gridView.editOptions.crossWhenExitLast = true
  gridView.editOptions.exceptDataClickWhenButton = true

  // Phase 4: UI/UX 개선
  // 4-1. 기본 레이아웃 옵션
  ;(gridView as unknown as { columnMovable: boolean }).columnMovable = true
  ;(gridView as unknown as { columnResizable: boolean }).columnResizable = true
  gridView.displayOptions.defaultColumnWidth = 100
  gridView.displayOptions.fitStyle = GridFitStyle.FILL
  gridView.header.height = 32

  // 4-2. 행 높이 설정 및 조절 활성화
  gridView.displayOptions.rowHeight = 28
  gridView.displayOptions.rowResizable = true
  gridView.displayOptions.eachRowResizable = true
}

/**
 * 프리셋 설정을 GridView와 DataProvider에 적용
 */
export function applyPresetConfig(
  gridView: GridView,
  dataProvider: LocalDataProvider,
  preset: RealGridPreset,
): void {
  const config = PRESET_CONFIGS[preset]

  // Phase 2: 데이터 관리 옵션 (프리셋 기반)
  // 2-1. 편집 가능 여부
  gridView.editOptions.editable = config.editable

  // 2-2. 소프트 삭제
  dataProvider.softDeleting = config.softDeleting

  // 2-3. 삭제된 행 표시 제어
  gridView.hideDeletedRows = config.hideDeletedRows

  // 2-4. 체크바 헤더 동기화
  gridView.checkBar.syncHeadCheck = true

  // Phase 3: 복사/붙여넣기 강화
  // 3-1. 복사 옵션
  gridView.copyOptions.lookupDisplay = config.copyOptions.lookupDisplay

  // 3-2. 붙여넣기 옵션
  if (config.pasteOptions.enabled) {
    gridView.pasteOptions.convertLookupLabel = config.pasteOptions.convertLookupLabel
    gridView.pasteOptions.checkDomainOnly = config.pasteOptions.checkDomainOnly
    gridView.pasteOptions.checkReadOnly = config.pasteOptions.checkReadOnly
    gridView.pasteOptions.numberChars = [',']
  }

  // 검색 프리셋 전용 설정 (대용량 데이터 최적화)
  if (preset === 'search' && config.filterPanel) {
    gridView.filterPanel.visible = config.filterPanel.visible
    if (config.filterPanel.height) {
      gridView.filterPanel.height = config.filterPanel.height
    }
    if (config.filterPanel.filterDelay !== undefined) {
      gridView.filterPanel.filterDelay = config.filterPanel.filterDelay
    }
    if (config.filterPanel.ignoreCase !== undefined) {
      gridView.filterPanel.ignoreCase = config.filterPanel.ignoreCase
    }
    if (config.filterPanel.trimData !== undefined) {
      gridView.filterPanel.trimData = config.filterPanel.trimData
    }
    // 성능 최적화: 스크롤 리프레시 모드
    if (config.refreshMode) {
      ;(gridView.displayOptions as unknown as { refreshMode: string }).refreshMode = config.refreshMode
    }
  }
}

/**
 * 프리셋의 Undo 활성화 여부 확인
 */
export function isUndoEnabled(preset: RealGridPreset): boolean {
  const config = PRESET_CONFIGS[preset]
  return config.undoable !== false
}

/**
 * 프리셋 설정 가져오기
 */
export function getPresetConfig(preset: RealGridPreset): RealGridPresetConfig {
  return PRESET_CONFIGS[preset]
}

export default PRESET_CONFIGS
