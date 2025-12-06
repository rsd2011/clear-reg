/**
 * RealGrid 키보드 단축키 Composable
 *
 * 키보드 단축키 지원:
 * - Ctrl+C: 복사
 * - Ctrl+V: 붙여넣기
 * - Ctrl+Z: 실행 취소
 * - Delete: 선택 행 삭제
 * - F2: 편집 모드 진입
 * - Escape: 편집 취소
 * - 커스텀 단축키 등록
 */

import type { GridView, LocalDataProvider } from 'realgrid'
import type {
  RealGridKeyboardShortcut,
  RealGridKeyboardOptions,
} from '~/types/realgrid'

// ============================================================================
// 기본 키보드 핸들러
// ============================================================================

/**
 * 클립보드에 선택 영역 복사
 */
const copyToClipboard = async (grid: GridView): Promise<void> => {
  try {
    const selection = grid.getSelection()
    if (!selection) return

    const selectionData = grid.getSelectionData()
    if (!selectionData || selectionData.length === 0) return

    // 탭으로 구분된 텍스트 생성
    const lines: string[] = []
    for (const row of selectionData) {
      const values = Object.entries(row)
        .filter(([key]) => key !== '__rowId')
        .map(([_, value]) => String(value ?? ''))
      lines.push(values.join('\t'))
    }

    const text = lines.join('\n')
    await navigator.clipboard.writeText(text)
  }
  catch (error) {
    console.error('Failed to copy to clipboard:', error)
  }
}

/**
 * 클립보드에서 붙여넣기
 */
const pasteFromClipboard = async (
  grid: GridView,
  provider: LocalDataProvider,
): Promise<void> => {
  try {
    const text = await navigator.clipboard.readText()
    if (!text) return

    const current = grid.getCurrent()
    const dataRow = current.dataRow ?? -1
    if (dataRow < 0) return

    // 탭과 줄바꿈으로 파싱
    const lines = text.split('\n').filter(line => line.trim())
    const columns = grid.getColumns()
    // GridColumn에서 fieldName 속성 접근 (타입 정의에 없을 수 있음)
    const visibleColumns = columns.filter((col) => {
      const fieldName = (col as unknown as { fieldName?: string }).fieldName
      return col.visible && fieldName
    })

    let startRow = dataRow
    const currentColumn = typeof current.column === 'string' ? current.column : (current.column?.name ?? '')
    const startColIndex = visibleColumns.findIndex(col => col.name === currentColumn)

    for (const line of lines) {
      const values = line.split('\t')

      for (let i = 0; i < values.length && startColIndex + i < visibleColumns.length; i++) {
        const column = visibleColumns[startColIndex + i]
        const fieldName = (column as unknown as { fieldName?: string }).fieldName
        if (column && fieldName) {
          provider.setValue(startRow, fieldName, values[i])
        }
      }

      startRow++
      if (startRow >= provider.getRowCount()) {
        break
      }
    }

    grid.refresh()
  }
  catch (error) {
    console.error('Failed to paste from clipboard:', error)
  }
}

/**
 * 선택된 행 삭제
 */
const deleteSelectedRows = (grid: GridView, provider: LocalDataProvider): void => {
  const checkedRows = grid.getCheckedRows()

  if (checkedRows.length > 0) {
    // 체크된 행 삭제 (역순으로)
    for (let i = checkedRows.length - 1; i >= 0; i--) {
      const row = checkedRows[i]
      if (typeof row === 'number') {
        provider.removeRow(row)
      }
    }
  }
  else {
    // 현재 행 삭제
    const current = grid.getCurrent()
    const dataRow = current.dataRow ?? -1
    if (dataRow >= 0) {
      provider.removeRow(dataRow)
    }
  }
}

/**
 * 실행 취소
 * LocalDataProvider의 canUndo/undo는 타입 정의에 없을 수 있음
 */
const undoAction = (provider: LocalDataProvider): void => {
  const p = provider as unknown as { canUndo?: () => boolean, undo?: () => void }
  if (p.canUndo?.()) {
    p.undo?.()
  }
}

/**
 * 다시 실행
 * LocalDataProvider의 canRedo/redo는 타입 정의에 없을 수 있음
 */
const redoAction = (provider: LocalDataProvider): void => {
  const p = provider as unknown as { canRedo?: () => boolean, redo?: () => void }
  if (p.canRedo?.()) {
    p.redo?.()
  }
}

/**
 * 편집 모드 진입
 */
const enterEditMode = (grid: GridView): void => {
  const current = grid.getCurrent()
  if (current.column) {
    grid.showEditor()
  }
}

/**
 * 편집 취소
 */
const cancelEdit = (grid: GridView): void => {
  grid.cancel()
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridKeyboardInput {
  options?: RealGridKeyboardOptions
}

/**
 * RealGrid 키보드 단축키 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { setupKeyboard, registerShortcut, unregisterShortcut } = useRealGridKeyboard({
 *   options: {
 *     enableCopy: true,
 *     enablePaste: true,
 *     enableUndo: true,
 *     enableDelete: true,
 *     enableEdit: true,
 *     customShortcuts: [
 *       {
 *         key: 's',
 *         ctrlKey: true,
 *         action: (grid, provider) => saveData(provider),
 *         description: '저장'
 *       }
 *     ]
 *   }
 * })
 *
 * // 그리드 초기화 후
 * setupKeyboard(gridView, dataProvider)
 * </script>
 * ```
 */
export const useRealGridKeyboard = (input: UseRealGridKeyboardInput = {}) => {
  const defaultOptions: RealGridKeyboardOptions = {
    enableCopy: true,
    enablePaste: true,
    enableUndo: true,
    enableDelete: true,
    enableEdit: true,
    customShortcuts: [],
  }

  const options = ref<RealGridKeyboardOptions>({
    ...defaultOptions,
    ...input.options,
  })

  // 등록된 단축키 맵
  const shortcuts = ref<Map<string, RealGridKeyboardShortcut>>(new Map())

  // 키 조합을 문자열로 변환
  const getShortcutKey = (e: KeyboardEvent): string => {
    const parts: string[] = []
    if (e.ctrlKey || e.metaKey) parts.push('ctrl')
    if (e.shiftKey) parts.push('shift')
    if (e.altKey) parts.push('alt')
    parts.push(e.key.toLowerCase())
    return parts.join('+')
  }

  // 단축키 정의를 키로 변환
  const shortcutToKey = (shortcut: RealGridKeyboardShortcut): string => {
    const parts: string[] = []
    if (shortcut.ctrlKey || shortcut.metaKey) parts.push('ctrl')
    if (shortcut.shiftKey) parts.push('shift')
    if (shortcut.altKey) parts.push('alt')
    parts.push(shortcut.key.toLowerCase())
    return parts.join('+')
  }

  /**
   * 기본 단축키 등록
   */
  const registerDefaultShortcuts = (
    grid: GridView,
    provider: LocalDataProvider,
  ): void => {
    // Ctrl+C: 복사
    if (options.value.enableCopy) {
      shortcuts.value.set('ctrl+c', {
        key: 'c',
        ctrlKey: true,
        action: () => copyToClipboard(grid),
        description: '복사',
      })
    }

    // Ctrl+V: 붙여넣기
    if (options.value.enablePaste) {
      shortcuts.value.set('ctrl+v', {
        key: 'v',
        ctrlKey: true,
        action: () => pasteFromClipboard(grid, provider),
        description: '붙여넣기',
      })
    }

    // Ctrl+Z: 실행 취소
    if (options.value.enableUndo) {
      shortcuts.value.set('ctrl+z', {
        key: 'z',
        ctrlKey: true,
        action: () => undoAction(provider),
        description: '실행 취소',
      })

      // Ctrl+Y 또는 Ctrl+Shift+Z: 다시 실행
      shortcuts.value.set('ctrl+y', {
        key: 'y',
        ctrlKey: true,
        action: () => redoAction(provider),
        description: '다시 실행',
      })

      shortcuts.value.set('ctrl+shift+z', {
        key: 'z',
        ctrlKey: true,
        shiftKey: true,
        action: () => redoAction(provider),
        description: '다시 실행',
      })
    }

    // Delete: 선택 행 삭제
    if (options.value.enableDelete) {
      shortcuts.value.set('delete', {
        key: 'Delete',
        action: () => deleteSelectedRows(grid, provider),
        description: '선택 행 삭제',
      })
    }

    // F2: 편집 모드 진입
    if (options.value.enableEdit) {
      shortcuts.value.set('f2', {
        key: 'F2',
        action: () => enterEditMode(grid),
        description: '편집 모드',
      })

      // Escape: 편집 취소
      shortcuts.value.set('escape', {
        key: 'Escape',
        action: () => cancelEdit(grid),
        description: '편집 취소',
      })
    }

    // 커스텀 단축키 등록
    if (options.value.customShortcuts) {
      for (const shortcut of options.value.customShortcuts) {
        const key = shortcutToKey(shortcut)
        shortcuts.value.set(key, shortcut)
      }
    }
  }

  // 이벤트 핸들러 참조
  let keydownHandler: ((e: KeyboardEvent) => void) | null = null
  let containerRef: HTMLElement | null = null

  /**
   * 키보드 단축키 설정
   */
  const setupKeyboard = (
    grid: GridView,
    provider: LocalDataProvider,
    container?: HTMLElement,
  ): void => {
    // 기본 단축키 등록
    registerDefaultShortcuts(grid, provider)

    // 키다운 핸들러
    keydownHandler = (e: KeyboardEvent) => {
      const key = getShortcutKey(e)
      const shortcut = shortcuts.value.get(key)

      if (shortcut) {
        e.preventDefault()
        e.stopPropagation()
        shortcut.action(grid, provider)
      }
    }

    // 컨테이너 또는 그리드 엘리먼트에 이벤트 등록
    containerRef = container || (grid as unknown as { container: HTMLElement }).container
    if (containerRef) {
      containerRef.addEventListener('keydown', keydownHandler)
    }
  }

  /**
   * 키보드 단축키 해제
   */
  const teardownKeyboard = (): void => {
    if (containerRef && keydownHandler) {
      containerRef.removeEventListener('keydown', keydownHandler)
    }
    keydownHandler = null
    containerRef = null
    shortcuts.value.clear()
  }

  /**
   * 커스텀 단축키 등록
   */
  const registerShortcut = (shortcut: RealGridKeyboardShortcut): void => {
    const key = shortcutToKey(shortcut)
    shortcuts.value.set(key, shortcut)
  }

  /**
   * 단축키 해제
   */
  const unregisterShortcut = (shortcutKey: string): void => {
    shortcuts.value.delete(shortcutKey.toLowerCase())
  }

  /**
   * 등록된 단축키 목록 조회
   */
  const getRegisteredShortcuts = (): RealGridKeyboardShortcut[] => {
    return Array.from(shortcuts.value.values())
  }

  /**
   * 옵션 업데이트
   */
  const updateOptions = (newOptions: Partial<RealGridKeyboardOptions>): void => {
    options.value = { ...options.value, ...newOptions }
  }

  // 컴포넌트 언마운트 시 정리
  onBeforeUnmount(() => {
    teardownKeyboard()
  })

  return {
    // 설정
    setupKeyboard,
    teardownKeyboard,

    // 단축키 관리
    registerShortcut,
    unregisterShortcut,
    getRegisteredShortcuts,

    // 옵션
    updateOptions,
    options: readonly(options),

    // 유틸리티 함수 노출
    copyToClipboard,
    pasteFromClipboard,
    deleteSelectedRows,
    undoAction,
    redoAction,
    enterEditMode,
    cancelEdit,
  }
}

export default useRealGridKeyboard
