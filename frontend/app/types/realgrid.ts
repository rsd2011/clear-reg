// RealGrid 관련 TypeScript 타입 정의
import type { GridView, LocalDataProvider } from 'realgrid'

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
