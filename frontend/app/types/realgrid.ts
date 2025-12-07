// RealGrid 관련 TypeScript 타입 정의
import type { GridView, LocalDataProvider } from 'realgrid'

// ============================================================================
// 프리셋 타입 정의
// ============================================================================

/**
 * 그리드 프리셋 타입
 * - default: 기본 설정 (편집 가능, 추천 설정 적용)
 * - editable: 편집 최적화 (셀 단위 커밋, 소프트 삭제, 복사/붙여넣기 강화)
 * - readonly: 읽기 전용 (편집 비활성화, 선택/복사만 허용)
 * - search: 대용량 데이터 검색용 (필터 패널, 성능 최적화, Undo 비활성화)
 */
export type RealGridPreset = 'default' | 'editable' | 'readonly' | 'search'

/**
 * 프리셋별 설정 값
 */
export interface RealGridPresetConfig {
  /** 편집 가능 여부 */
  editable: boolean
  /** 소프트 삭제 사용 여부 */
  softDeleting: boolean
  /** 삭제된 행 숨김 여부 */
  hideDeletedRows: boolean
  /** 복사 옵션 */
  copyOptions: {
    lookupDisplay: boolean
  }
  /** 붙여넣기 옵션 */
  pasteOptions: {
    enabled: boolean
    convertLookupLabel: boolean
    checkDomainOnly: boolean
    checkReadOnly: boolean
  }
  /** 고정 옵션 (reporting용) */
  fixedOptions?: {
    colCount: number
    rowCount: number
  }
  /** 병합셀 편집 (reporting용) */
  editItemMerging?: boolean
  /** 필터 패널 옵션 (search용) */
  filterPanel?: {
    visible: boolean
    height?: number
    /** 입력 후 필터 적용 지연 시간 (ms) - 대용량 데이터 시 성능 향상 */
    filterDelay?: number
    /** 대소문자 무시 */
    ignoreCase?: boolean
    /** 공백 제거 */
    trimData?: boolean
  }
  /** Undo/Redo 비활성화 (대용량 데이터 성능 최적화) */
  undoable?: boolean
  /** 스크롤 성능 최적화 모드 */
  refreshMode?: 'auto' | 'recycle'
}

// ============================================================================
// 기본 타입 정의
// ============================================================================

/**
 * RealGrid 셀 클릭 이벤트 인자 타입
 * (realgrid 모듈에서 CellClickEventArgs가 export되지 않아 직접 정의)
 */
export interface RealGridCellClickData {
  cellIndex: number
  column: string
  dataRow: number
  fieldIndex: number
  fieldName: string
  itemIndex: number
  [key: string]: unknown
}

/**
 * RealGrid 컬럼 정의
 */
export interface RealGridColumn {
  name: string
  fieldName: string
  type?: 'text' | 'number' | 'datetime' | 'boolean'
  width?: number
  editable?: boolean
  header?: {
    text?: string
  }
  // RealGrid 라이브러리의 확장 속성을 위한 인덱스 시그니처
  [key: string]: unknown
}

/**
 * RealGrid 옵션 설정
 */
export interface RealGridOptions {
  edit?: Record<string, unknown>
  display?: Record<string, unknown>
  checkBar?: Record<string, unknown>
  [key: string]: unknown
}

/**
 * RealGrid 이벤트 콜백 타입
 */
export interface RealGridEvents {
  onReady?: (grid: GridView, provider: LocalDataProvider) => void
  onCellClicked?: (grid: GridView, clickData: RealGridCellClickData) => void
  onCellDblClicked?: (grid: GridView, clickData: RealGridCellClickData) => void
  onDataCellClicked?: (grid: GridView, clickData: RealGridCellClickData) => void
  onCurrentRowChanged?: (grid: GridView, oldRow: number, newRow: number) => void
}

/**
 * RealGrid 인스턴스 인터페이스
 */
export interface RealGridInstance {
  gridView: GridView
  dataProvider: LocalDataProvider
  destroy: () => void
}

// ============================================================================
// 멀티레벨 컬럼 관련 타입
// ============================================================================

/**
 * 컬럼 레이아웃 아이템
 */
export interface ColumnLayoutItem {
  column: string
  cellSpan: number
  width: number
}

/**
 * 멀티레벨 컬럼 그룹 설정
 */
export interface MultiLevelColumnGroup {
  direction: 'vertical' | 'horizontal'
  items: (ColumnLayoutItem | number)[]
  header: { visible: boolean }
}

/**
 * 멀티레벨 컬럼 설정 옵션
 */
export interface MultiLevelColumnOptions {
  defaultWidth?: number
  headerVisible?: boolean
}

// ============================================================================
// 컨텍스트 메뉴 관련 타입
// ============================================================================

/**
 * 컨텍스트 메뉴 아이템
 */
export interface RealGridContextMenuItem {
  label: string
  tag?: string
  type?: 'check' | 'normal'
  checked?: boolean
  enabled?: boolean
  children?: RealGridContextMenuItem[]
}

/**
 * 컨텍스트 메뉴 클릭 데이터
 */
export interface RealGridContextMenuClickData {
  label: string
  tag?: string
  checked?: boolean
  parent?: {
    label: string
    tag?: string
  }
}

/**
 * 컨텍스트 메뉴 액션 핸들러
 */
export type ContextMenuActionHandler = (
  grid: GridView,
  data: RealGridContextMenuClickData,
  cell: RealGridCellClickData,
) => void

/**
 * 컨텍스트 메뉴 액션 맵
 */
export type ContextMenuActionMap = Record<string, ContextMenuActionHandler>

/**
 * 컨텍스트 메뉴 옵션
 */
export interface RealGridContextMenuOptions {
  showFixedMenu?: boolean
  showColumnMenu?: boolean
  showRowHeightMenu?: boolean
  showExportMenu?: boolean
  showFilterMenu?: boolean
  customMenuItems?: RealGridContextMenuItem[]
  customActions?: ContextMenuActionMap
}

// ============================================================================
// 페이지네이션 관련 타입
// ============================================================================

/**
 * 페이지네이션 상태
 */
export interface RealGridPaginationState {
  currentPage: number
  totalItems: number
  itemsPerPage: number
  totalPages: number
  pageGroup: number
  pagesPerGroup: number
}

/**
 * 페이지네이션 옵션
 */
export interface RealGridPaginationOptions {
  itemsPerPage?: number
  pagesPerGroup?: number
  showFirstLast?: boolean
  showPrevNext?: boolean
}

/**
 * 무한 스크롤 상태
 */
export interface RealGridInfiniteScrollState {
  isLoading: boolean
  hasMore: boolean
  currentOffset: number
  pageSize: number
  totalItems: number
}

/**
 * 무한 스크롤 옵션
 */
export interface RealGridInfiniteScrollOptions {
  pageSize?: number
  threshold?: number // 스크롤 위치 (0-1, 하단에서의 비율)
  loadingDelay?: number
}

/**
 * 데이터 로드 함수 타입
 */
export type InfiniteScrollLoadFn<T = Record<string, unknown>> = (
  offset: number,
  limit: number,
) => Promise<{ data: T[], hasMore: boolean, total?: number }>

// ============================================================================
// 선택 요약 관련 타입
// ============================================================================

/**
 * 선택 영역 요약 정보
 */
export interface RealGridSelectionSummary {
  sum: number
  count: number
  average: number
  min: number
  max: number
  numericCount: number
}

/**
 * 선택 요약 옵션
 */
export interface RealGridSelectionOptions {
  includeHidden?: boolean
  numericOnly?: boolean
}

// ============================================================================
// 내보내기 관련 타입
// ============================================================================

/**
 * 내보내기 형식
 */
export type ExportFormat = 'excel' | 'csv' | 'json' | 'clipboard'

/**
 * 내보내기 옵션
 */
export interface RealGridExportOptions {
  type: ExportFormat
  fileName?: string
  sheetName?: string
  includeHeader?: boolean
  includeFooter?: boolean
  onlySelected?: boolean
  onlyVisible?: boolean
  encoding?: 'utf-8' | 'euc-kr'
  delimiter?: string // CSV용
}

// ============================================================================
// 키보드 단축키 관련 타입
// ============================================================================

/**
 * 키보드 단축키 정의
 */
export interface RealGridKeyboardShortcut {
  key: string
  ctrlKey?: boolean
  shiftKey?: boolean
  altKey?: boolean
  metaKey?: boolean
  action: (grid: GridView, provider: LocalDataProvider) => void
  description?: string
}

/**
 * 키보드 옵션
 */
export interface RealGridKeyboardOptions {
  enableCopy?: boolean
  enablePaste?: boolean
  enableUndo?: boolean
  enableDelete?: boolean
  enableEdit?: boolean
  customShortcuts?: RealGridKeyboardShortcut[]
}

// ============================================================================
// 상태 저장 관련 타입
// ============================================================================

/**
 * 그리드 저장 상태
 */
export interface RealGridPersistedState {
  columns: {
    name: string
    width: number
    visible: boolean
    sortOrder?: number
  }[]
  fixedOptions?: {
    colCount: number
    rowCount: number
  }
  filters?: Record<string, unknown>
  sorting?: {
    column: string
    direction: 'asc' | 'desc'
  }[]
  rowHeight?: number
  timestamp: number
}

/**
 * 상태 저장 옵션
 */
export interface RealGridPersistenceOptions {
  storageKey: string
  storage?: 'localStorage' | 'sessionStorage'
  saveColumns?: boolean
  saveFilters?: boolean
  saveSorting?: boolean
  saveFixedOptions?: boolean
  saveRowHeight?: boolean
  autoSave?: boolean
  autoSaveDelay?: number
}

// ============================================================================
// 유효성 검사 관련 타입
// ============================================================================

/**
 * 유효성 검사 규칙
 */
export interface RealGridValidationRule {
  type: 'required' | 'min' | 'max' | 'range' | 'pattern' | 'custom'
  value?: unknown
  message: string
  validator?: (value: unknown, row: Record<string, unknown>) => boolean
}

/**
 * 컬럼별 유효성 검사 설정
 */
export interface RealGridColumnValidation {
  column: string
  rules: RealGridValidationRule[]
}

/**
 * 유효성 검사 결과
 */
export interface RealGridValidationResult {
  valid: boolean
  errors: {
    row: number
    column: string
    value: unknown
    message: string
  }[]
}

/**
 * 유효성 검사 옵션
 */
export interface RealGridValidationOptions {
  validateOnEdit?: boolean
  validateOnCommit?: boolean
  showErrorTooltip?: boolean
  errorClassName?: string
}

// ============================================================================
// 조건부 서식 관련 타입
// ============================================================================

/**
 * 조건부 서식 규칙
 */
export interface RealGridConditionalFormatRule {
  column: string
  condition: (value: unknown, row: Record<string, unknown>) => boolean
  style: {
    background?: string
    foreground?: string
    fontWeight?: string
    fontStyle?: string
    icon?: string
  }
}

// ============================================================================
// 동적 필드/컬럼 관련 타입
// ============================================================================

/**
 * 동적 컬럼 정보
 */
export interface DynamicColumnInfo extends Partial<RealGridColumn> {
  name: string
  tag?: {
    dataType?: 'text' | 'number' | 'datetime' | 'boolean'
    [key: string]: unknown
  }
  items?: DynamicColumnInfo[]
}

/**
 * 폼 모델 아이템
 */
export interface FormModelItem {
  header: string
  column: string
}

/**
 * 폼 모델
 */
export interface RealGridFormModel {
  footer: {
    popupMenu?: string
  }
  items: FormModelItem[]
}

// ============================================================================
// 확장된 이벤트 타입
// ============================================================================

/**
 * 확장된 RealGrid 이벤트 콜백 타입
 */
export interface RealGridEventsExtended extends RealGridEvents {
  /** 컨텍스트 메뉴 클릭 이벤트 (RealGrid 시그니처: grid, item, clickData) */
  onContextMenuClick?: (
    grid: GridView,
    data: RealGridContextMenuClickData,
    clickData?: unknown,
  ) => void
  onSelectionChanged?: (grid: GridView, selection: unknown) => void
  onScrollEnd?: (grid: GridView) => void
  onFilterChanged?: (grid: GridView, column: string, filters: unknown[]) => void
  onColumnResized?: (grid: GridView, column: string, width: number) => void
  onColumnMoved?: (grid: GridView, column: string, newIndex: number) => void
  onSortingChanged?: (grid: GridView, columns: string[]) => void
  onEditCommit?: (grid: GridView, index: number, field: string, newValue: unknown) => void
  onValidationError?: (grid: GridView, error: RealGridValidationResult) => void
}

// ============================================================================
// 확장된 옵션 타입
// ============================================================================

/**
 * 확장된 RealGrid 옵션
 */
export interface RealGridOptionsExtended extends RealGridOptions {
  contextMenu?: RealGridContextMenuOptions
  keyboard?: RealGridKeyboardOptions
  persistence?: RealGridPersistenceOptions
  validation?: RealGridValidationOptions
  pagination?: RealGridPaginationOptions
  infiniteScroll?: RealGridInfiniteScrollOptions
  conditionalFormat?: RealGridConditionalFormatRule[]
}
