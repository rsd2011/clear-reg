/**
 * RealGrid 내보내기 Composable
 *
 * 다양한 형식으로 그리드 데이터 내보내기:
 * - Excel (.xlsx)
 * - CSV
 * - JSON
 * - 클립보드
 */

import type { GridView, LocalDataProvider } from 'realgrid'
import type {
  RealGridExportOptions,
  ExportFormat,
} from '~/types/realgrid'

// ============================================================================
// 내보내기 유틸리티
// ============================================================================

/**
 * 그리드 데이터를 2D 배열로 변환
 */
const gridDataToArray = (
  grid: GridView,
  provider: LocalDataProvider,
  options: RealGridExportOptions,
): { headers: string[], data: unknown[][] } => {
  // 컬럼 정보
  const columns = grid.getColumns()
  const visibleColumns = options.onlyVisible
    ? columns.filter(col => col.visible)
    : columns

  // 헤더
  const headers = visibleColumns.map(col => col.header?.text || col.name || '')

  // 데이터 행
  let rowCount = provider.getRowCount()
  let startRow = 0

  if (options.onlySelected) {
    const selection = grid.getSelection()
    if (selection) {
      startRow = selection.startRow || 0
      rowCount = (selection.endRow || rowCount - 1) + 1
    }
  }

  const data: unknown[][] = []
  for (let i = startRow; i < rowCount; i++) {
    const row: unknown[] = []
    for (const col of visibleColumns) {
      // GridColumn에서 fieldName 속성 접근 (타입 정의에 없을 수 있음)
      const fieldName = (col as unknown as { fieldName?: string }).fieldName || col.name
      if (fieldName) {
        row.push(provider.getValue(i, fieldName))
      }
    }
    data.push(row)
  }

  return { headers, data }
}

/**
 * Excel 내보내기
 */
const exportToExcel = (
  grid: GridView,
  options: RealGridExportOptions,
): void => {
  const fileName = options.fileName || `export-${Date.now()}.xlsx`
  const sheetName = options.sheetName || 'Sheet1'

  // RealGrid exportGrid 옵션 (타입 정의에 없는 속성 포함)
  grid.exportGrid({
    type: 'excel',
    target: 'local',
    fileName,
    showProgress: true,
    progressMessage: '내보내기 중...',
    allItems: !options.onlySelected,
    indicator: 'hidden',
    header: options.includeHeader !== false ? 'visible' : 'hidden',
    footer: options.includeFooter ? 'visible' : 'hidden',
    lookupDisplay: true,
    exportOptions: {
      sheetName,
    },
  } as Parameters<typeof grid.exportGrid>[0])
}

/**
 * CSV 내보내기
 */
const exportToCsv = (
  grid: GridView,
  provider: LocalDataProvider,
  options: RealGridExportOptions,
): void => {
  const { headers, data } = gridDataToArray(grid, provider, options)
  const delimiter = options.delimiter || ','

  // CSV 문자열 생성
  const lines: string[] = []

  // 헤더 추가
  if (options.includeHeader !== false) {
    lines.push(headers.map(h => escapeCSVValue(h, delimiter)).join(delimiter))
  }

  // 데이터 추가
  for (const row of data) {
    lines.push(row.map(v => escapeCSVValue(String(v ?? ''), delimiter)).join(delimiter))
  }

  const csvContent = lines.join('\n')

  // BOM 추가 (UTF-8)
  const bom = options.encoding === 'utf-8' ? '\uFEFF' : ''
  const blob = new Blob([bom + csvContent], {
    type: `text/csv;charset=${options.encoding || 'utf-8'}`,
  })

  // 다운로드
  const fileName = options.fileName || `export-${Date.now()}.csv`
  downloadBlob(blob, fileName)
}

/**
 * CSV 값 이스케이프
 */
const escapeCSVValue = (value: string, delimiter: string): string => {
  if (value.includes(delimiter) || value.includes('"') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

/**
 * JSON 내보내기
 */
const exportToJson = (
  grid: GridView,
  provider: LocalDataProvider,
  options: RealGridExportOptions,
): void => {
  const { headers, data } = gridDataToArray(grid, provider, options)

  // 객체 배열로 변환
  const jsonData = data.map((row) => {
    const obj: Record<string, unknown> = {}
    for (let i = 0; i < headers.length; i++) {
      const header = headers[i]
      if (header) {
        obj[header] = row[i]
      }
    }
    return obj
  })

  const jsonString = JSON.stringify(jsonData, null, 2)
  const blob = new Blob([jsonString], { type: 'application/json' })

  const fileName = options.fileName || `export-${Date.now()}.json`
  downloadBlob(blob, fileName)
}

/**
 * 클립보드로 복사
 */
const exportToClipboard = async (
  grid: GridView,
  provider: LocalDataProvider,
  options: RealGridExportOptions,
): Promise<boolean> => {
  try {
    const { headers, data } = gridDataToArray(grid, provider, options)
    const delimiter = '\t'

    const lines: string[] = []

    // 헤더 추가
    if (options.includeHeader !== false) {
      lines.push(headers.join(delimiter))
    }

    // 데이터 추가
    for (const row of data) {
      lines.push(row.map(v => String(v ?? '')).join(delimiter))
    }

    const text = lines.join('\n')
    await navigator.clipboard.writeText(text)
    return true
  }
  catch (error) {
    console.error('Failed to copy to clipboard:', error)
    return false
  }
}

/**
 * Blob 다운로드
 */
const downloadBlob = (blob: Blob, fileName: string): void => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridExportInput {
  defaultOptions?: Partial<RealGridExportOptions>
  onExportStart?: (format: ExportFormat) => void
  onExportComplete?: (format: ExportFormat, success: boolean) => void
}

/**
 * RealGrid 내보내기 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { exportGrid, exportExcel, exportCsv, exportJson, copyToClipboard } = useRealGridExport({
 *   defaultOptions: {
 *     includeHeader: true,
 *     onlyVisible: true
 *   },
 *   onExportComplete: (format, success) => {
 *     if (success) {
 *       toast.success(`${format} 내보내기 완료`)
 *     }
 *   }
 * })
 *
 * // 내보내기 실행
 * exportGrid(gridView, dataProvider, { type: 'excel', fileName: 'report.xlsx' })
 * </script>
 * ```
 */
export const useRealGridExport = (input: UseRealGridExportInput = {}) => {
  const defaultOptions = ref<Partial<RealGridExportOptions>>(
    input.defaultOptions || {
      includeHeader: true,
      includeFooter: false,
      onlySelected: false,
      onlyVisible: true,
      encoding: 'utf-8',
      delimiter: ',',
    },
  )

  // 내보내기 상태
  const isExporting = ref(false)

  /**
   * 그리드 내보내기 (통합)
   */
  const exportGrid = async (
    grid: GridView,
    provider: LocalDataProvider,
    options: RealGridExportOptions,
  ): Promise<boolean> => {
    const mergedOptions: RealGridExportOptions = {
      ...defaultOptions.value,
      ...options,
    }

    isExporting.value = true
    input.onExportStart?.(options.type)

    try {
      switch (options.type) {
        case 'excel':
          exportToExcel(grid, mergedOptions)
          break
        case 'csv':
          exportToCsv(grid, provider, mergedOptions)
          break
        case 'json':
          exportToJson(grid, provider, mergedOptions)
          break
        case 'clipboard':
          return await exportToClipboard(grid, provider, mergedOptions)
        default:
          console.warn('Unknown export type:', options.type)
          return false
      }

      input.onExportComplete?.(options.type, true)
      return true
    }
    catch (error) {
      console.error('Export failed:', error)
      input.onExportComplete?.(options.type, false)
      return false
    }
    finally {
      isExporting.value = false
    }
  }

  /**
   * Excel 내보내기 (단축)
   */
  const exportExcel = (
    grid: GridView,
    provider: LocalDataProvider,
    options?: Partial<RealGridExportOptions>,
  ): Promise<boolean> => {
    return exportGrid(grid, provider, { ...options, type: 'excel' })
  }

  /**
   * CSV 내보내기 (단축)
   */
  const exportCsv = (
    grid: GridView,
    provider: LocalDataProvider,
    options?: Partial<RealGridExportOptions>,
  ): Promise<boolean> => {
    return exportGrid(grid, provider, { ...options, type: 'csv' })
  }

  /**
   * JSON 내보내기 (단축)
   */
  const exportJson = (
    grid: GridView,
    provider: LocalDataProvider,
    options?: Partial<RealGridExportOptions>,
  ): Promise<boolean> => {
    return exportGrid(grid, provider, { ...options, type: 'json' })
  }

  /**
   * 클립보드 복사 (단축)
   */
  const copyToClipboard = (
    grid: GridView,
    provider: LocalDataProvider,
    options?: Partial<RealGridExportOptions>,
  ): Promise<boolean> => {
    return exportGrid(grid, provider, { ...options, type: 'clipboard' })
  }

  /**
   * 기본 옵션 업데이트
   */
  const updateDefaultOptions = (options: Partial<RealGridExportOptions>): void => {
    defaultOptions.value = { ...defaultOptions.value, ...options }
  }

  return {
    // 내보내기
    exportGrid,
    exportExcel,
    exportCsv,
    exportJson,
    copyToClipboard,

    // 상태
    isExporting: readonly(isExporting),

    // 옵션
    updateDefaultOptions,
    defaultOptions: readonly(defaultOptions),
  }
}

export default useRealGridExport
