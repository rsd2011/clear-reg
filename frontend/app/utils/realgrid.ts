/**
 * RealGrid 순수 유틸리티 함수
 *
 * Vue 상태와 무관한 순수 함수들
 * - 멀티레벨 컬럼 생성
 * - 동적 필드/컬럼 설정
 * - 폼 모델 변환
 * - 데이터 추출/업데이트
 * - 컬럼/행 조작
 */

import type { GridView, LocalDataProvider, DataField } from 'realgrid'
import type {
  MultiLevelColumnGroup,
  MultiLevelColumnOptions,
  ColumnLayoutItem,
  DynamicColumnInfo,
  RealGridFormModel,
  FormModelItem,
  RealGridColumn,
} from '~/types/realgrid'

// ============================================================================
// 수학 유틸리티 (내부용)
// ============================================================================

/**
 * 최대공약수 (GCD) 계산
 */
const gcd = (a: number, b: number): number => {
  while (b !== 0) {
    const temp = a % b
    a = b
    b = temp
  }
  return a
}

/**
 * 최소공배수 (LCM) 계산
 */
const lcm = (a: number, b: number): number => {
  return (a * b) / gcd(a, b)
}

/**
 * 배열의 최소공배수 계산
 */
const leastCommonMultiple = (arr: number[]): number => {
  if (arr.length === 0) return 1
  if (arr.length === 1) return arr[0] ?? 1

  let result = lcm(arr[0] ?? 1, arr[1] ?? 1)
  for (let i = 2; i < arr.length; i++) {
    result = lcm(result, arr[i] ?? 1)
  }
  return result
}

// ============================================================================
// 멀티레벨 컬럼 유틸리티
// ============================================================================

/**
 * 멀티레벨 컬럼 레이아웃 생성
 *
 * 여러 행에 걸친 컬럼 그룹핑을 위한 레이아웃을 생성합니다.
 * 각 행의 컬럼 수가 다를 경우 LCM을 사용하여 cellSpan을 계산합니다.
 *
 * @param colNames - 각 행별 컬럼명 배열 (예: [['A', 'B'], ['C', 'D', 'E']])
 * @param options - 레이아웃 옵션
 * @returns 멀티레벨 컬럼 그룹 배열
 *
 * @example
 * ```ts
 * const layout = getMultiLevelColumns([
 *   ['year', 'quarter'],
 *   ['jan', 'feb', 'mar']
 * ])
 * gridView.setColumnLayout(layout)
 * ```
 */
export const getMultiLevelColumns = (
  colNames: string[][],
  options: MultiLevelColumnOptions = {},
): MultiLevelColumnGroup[] => {
  const { defaultWidth = 50, headerVisible = false } = options

  if (colNames.length === 0) {
    return []
  }

  // 각 행의 컬럼 수 배열
  const arrLen = colNames.map(row => row.length)

  // 최소공배수 계산
  const lcmValue = leastCommonMultiple(arrLen)

  // 부모 그룹 생성 (수직 방향)
  const parentGroup: MultiLevelColumnGroup = {
    direction: 'vertical',
    items: [],
    header: { visible: headerVisible },
  }

  // 각 행별 그룹 생성
  for (let i = 0; i < colNames.length; i++) {
    const rowGroup: MultiLevelColumnGroup = {
      direction: 'horizontal',
      items: [],
      header: { visible: headerVisible },
    }

    const rowCols = colNames[i] ?? []
    const rowColCount = rowCols.length
    const cellSpan = lcmValue / rowColCount

    for (let j = 0; j < rowColCount; j++) {
      // 컬럼 아이템 추가
      const columnItem: ColumnLayoutItem = {
        column: rowCols[j] ?? '',
        cellSpan,
        width: defaultWidth,
      }
      rowGroup.items.push(columnItem)

      // cellSpan - 1 만큼 너비값 추가 (병합을 위한 placeholder)
      for (let k = 0; k < cellSpan - 1; k++) {
        rowGroup.items.push(defaultWidth)
      }
    }

    parentGroup.items.push(rowGroup as unknown as ColumnLayoutItem)
  }

  return [parentGroup]
}

// ============================================================================
// 동적 필드/컬럼 유틸리티
// ============================================================================

/**
 * 동적으로 필드와 컬럼을 설정
 *
 * columnInfo 배열에서 필드와 컬럼 정보를 추출하여 설정합니다.
 * 그룹 컬럼(items가 있는 경우)은 필드 생성에서 제외됩니다.
 *
 * @param provider - LocalDataProvider 인스턴스
 * @param grid - GridView 인스턴스
 * @param columnInfo - 컬럼 정보 배열
 *
 * @example
 * ```ts
 * setFieldsAndColumns(dataProvider, gridView, [
 *   { name: 'id', tag: { dataType: 'number' } },
 *   { name: 'name', header: { text: '이름' } },
 *   { name: 'amount', tag: { dataType: 'number' } }
 * ])
 * ```
 */
export const setFieldsAndColumns = (
  provider: LocalDataProvider,
  grid: GridView,
  columnInfo: DynamicColumnInfo[],
): void => {
  const fields: DataField[] = []
  const columns: RealGridColumn[] = []

  for (const col of columnInfo) {
    // 기본값 설정
    const column: RealGridColumn = {
      ...col,
      name: col.name,
      fieldName: col.fieldName || col.name,
      header: col.header || { text: col.name },
    }
    columns.push(column)

    // 그룹 컬럼이 아닌 경우에만 필드 생성
    if (!col.items) {
      const field: DataField = {
        fieldName: col.name,
      }

      // dataType 설정 (타입 호환성을 위해 as 사용)
      if (col.tag?.dataType) {
        field.dataType = col.tag.dataType as DataField['dataType']
      }

      fields.push(field)
    }
  }

  provider.setFields(fields)
  grid.setColumns(columns)
}

/**
 * 컬럼 정보를 필드 정보로 변환
 *
 * @param columns - RealGrid 컬럼 배열
 * @returns DataField 배열
 */
export const columnsToFields = (columns: RealGridColumn[]): DataField[] => {
  return columns
    .filter(col => !('items' in col))
    .map((col): DataField => ({
      fieldName: col.fieldName || col.name,
      dataType: (col.type || 'text') as DataField['dataType'],
    }))
}

// ============================================================================
// 폼 모델 유틸리티
// ============================================================================

/**
 * 컬럼 정보를 폼 모델로 변환
 *
 * 그리드의 컬럼 정보를 폼 뷰에서 사용할 수 있는 모델로 변환합니다.
 *
 * @param grid - GridView 인스턴스
 * @param popupMenu - 폼 푸터에 표시할 팝업 메뉴 ID
 * @returns RealGridFormModel
 *
 * @example
 * ```ts
 * const formModel = getColumnsToFormModel(gridView)
 * gridView.setFormView(formModel)
 * ```
 */
export const getColumnsToFormModel = (
  grid: GridView,
  popupMenu = 'menuForm',
): RealGridFormModel => {
  const columns = grid.getColumns()
  const items: FormModelItem[] = []

  for (const column of columns) {
    if (column.header?.text && column.name) {
      items.push({
        header: column.header.text,
        column: column.name,
      })
    }
  }

  return {
    footer: { popupMenu },
    items,
  }
}

// ============================================================================
// 데이터 유틸리티
// ============================================================================

/**
 * 그리드 데이터를 JSON 배열로 추출
 *
 * @param provider - LocalDataProvider 인스턴스
 * @param options - 추출 옵션
 * @returns 데이터 배열
 */
export const extractGridData = (
  provider: LocalDataProvider,
  options: {
    startRow?: number
    endRow?: number
    fields?: string[]
  } = {},
): Record<string, unknown>[] => {
  const { startRow = 0, endRow = -1, fields } = options
  const rowCount = provider.getRowCount()
  const actualEndRow = endRow < 0 ? rowCount - 1 : Math.min(endRow, rowCount - 1)

  const data: Record<string, unknown>[] = []

  for (let i = startRow; i <= actualEndRow; i++) {
    if (fields) {
      const row: Record<string, unknown> = {}
      for (const field of fields) {
        row[field] = provider.getValue(i, field)
      }
      data.push(row)
    }
    else {
      data.push(provider.getJsonRow(i))
    }
  }

  return data
}

/**
 * 그리드에 데이터 일괄 업데이트
 *
 * @param provider - LocalDataProvider 인스턴스
 * @param data - 업데이트할 데이터 배열
 * @param options - 업데이트 옵션
 */
export const updateGridData = (
  provider: LocalDataProvider,
  data: Record<string, unknown>[],
  options: {
    append?: boolean
    clear?: boolean
  } = {},
): void => {
  const { append = false, clear = false } = options

  if (clear) {
    provider.clearRows()
  }

  if (append) {
    provider.addRows(data)
  }
  else {
    provider.setRows(data)
  }
}

// ============================================================================
// 컬럼 유틸리티
// ============================================================================

/**
 * 컬럼 표시/숨김 토글
 *
 * @param grid - GridView 인스턴스
 * @param columnName - 컬럼명
 * @param visible - 표시 여부 (생략시 토글)
 */
export const toggleColumnVisibility = (
  grid: GridView,
  columnName: string,
  visible?: boolean,
): void => {
  const column = grid.columnByName(columnName)
  if (column) {
    const newVisible = visible !== undefined ? visible : !column.visible
    grid.setColumnProperty(columnName, 'visible', newVisible)
  }
}

/**
 * 모든 컬럼 표시
 *
 * @param grid - GridView 인스턴스
 */
export const showAllColumns = (grid: GridView): void => {
  const columns = grid.getColumns()
  for (const column of columns) {
    if (column.name) {
      grid.setColumnProperty(column.name, 'visible', true)
    }
  }
}

/**
 * 컬럼 너비 자동 조정
 *
 * @param grid - GridView 인스턴스
 * @param columnName - 컬럼명 (생략시 모든 컬럼)
 * @param maxWidth - 최대 너비 제한
 */
export const autoFitColumnWidth = (
  grid: GridView,
  columnName?: string,
  maxWidth = 500,
): void => {
  // fitColumnWidth는 타입 정의에 없을 수 있음
  const g = grid as unknown as Record<string, unknown>
  if (typeof g.fitColumnWidth !== 'function') return

  if (columnName) {
    g.fitColumnWidth(columnName, maxWidth, true)
  }
  else {
    const columns = grid.getColumns()
    for (const column of columns) {
      if (column.name) {
        g.fitColumnWidth(column.name, maxWidth, true)
      }
    }
  }
}

// ============================================================================
// 행 유틸리티
// ============================================================================

/**
 * 현재 선택된 행의 데이터 가져오기
 *
 * @param grid - GridView 인스턴스
 * @param provider - LocalDataProvider 인스턴스
 * @returns 현재 행 데이터 또는 null
 */
export const getCurrentRowData = (
  grid: GridView,
  provider: LocalDataProvider,
): Record<string, unknown> | null => {
  const current = grid.getCurrent()
  const dataRow = current.dataRow ?? -1
  if (dataRow >= 0) {
    return provider.getJsonRow(dataRow)
  }
  return null
}

/**
 * 체크된 행들의 데이터 가져오기
 *
 * @param grid - GridView 인스턴스
 * @param provider - LocalDataProvider 인스턴스
 * @returns 체크된 행 데이터 배열
 */
export const getCheckedRowsData = (
  grid: GridView,
  provider: LocalDataProvider,
): Record<string, unknown>[] => {
  const checkedRows = grid.getCheckedRows()
  return checkedRows.map(rowIndex => provider.getJsonRow(rowIndex))
}

/**
 * 행 높이 설정
 *
 * @param grid - GridView 인스턴스
 * @param height - 행 높이 ('small' | 'normal' | 'large' | number)
 */
export const setRowHeight = (
  grid: GridView,
  height: 'small' | 'normal' | 'large' | number,
): void => {
  const heightMap: Record<string, number> = {
    small: 20,
    normal: 28,
    large: 36,
  }

  const actualHeight = typeof height === 'number' ? height : heightMap[height]
  grid.displayOptions.rowHeight = actualHeight
}
