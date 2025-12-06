/**
 * RealGrid 컨텍스트 메뉴 Composable
 *
 * 레거시 setContextMenu/onContextMenuClick을 TypeScript/Vue 3로 현대화
 * - 동적 컨텍스트 메뉴 생성
 * - 핸들러 맵 기반 액션 처리
 * - 확장 가능한 커스텀 메뉴
 */

import type { GridView } from 'realgrid'
import type {
  RealGridContextMenuItem,
  RealGridContextMenuClickData,
  RealGridContextMenuOptions,
  ContextMenuActionHandler,
  ContextMenuActionMap,
  RealGridExportOptions,
} from '~/types/realgrid'

// ============================================================================
// 기본 메뉴 아이템 생성자
// ============================================================================

/**
 * 고정(Freeze) 메뉴 아이템 생성
 */
const createFixedMenuItems = (
  grid: GridView,
  currentRow: number,
  currentColumnText: string,
): RealGridContextMenuItem => {
  const fixedOptions = grid.fixedOptions
  const hasFixed = (fixedOptions.rightCount || 0) + (fixedOptions.colCount || 0) + (fixedOptions.rowCount || 0) > 0

  return {
    label: '고정',
    children: [
      { label: '행 1개', tag: 'fix:row:1' },
      { label: '행 2개', tag: 'fix:row:2' },
      { label: `현재 행까지 (${currentRow})`, tag: 'fix:row:current' },
      { label: '-' },
      { label: '첫번째 컬럼', tag: 'fix:col:1' },
      { label: '두번째 컬럼', tag: 'fix:col:2' },
      { label: `현재 컬럼까지 (${currentColumnText})`, tag: 'fix:col:current' },
      { label: '-' },
      { label: '고정 취소', tag: 'fix:cancel', enabled: hasFixed },
    ],
  }
}

/**
 * 컬럼 표시/숨김 메뉴 아이템 생성
 */
const createColumnVisibilityMenuItems = (grid: GridView): RealGridContextMenuItem => {
  const columnNames = grid.getColumnNames()
  const visibleMenuItems: RealGridContextMenuItem[] = []

  for (const name of columnNames) {
    const column = grid.columnByName(name)
    if (column?.fieldName) {
      visibleMenuItems.push({
        label: column.header?.text || name,
        tag: `column:toggle:${name}`,
        type: 'check',
        checked: column.visible,
      })
    }
  }

  visibleMenuItems.push(
    { label: '-' },
    { label: '컬럼 모두 보기', tag: 'column:showAll' },
    { label: '-' },
    { label: '현재 컬럼 필터 걸기', tag: 'column:autoFilter' },
  )

  return {
    label: '컬럼',
    tag: 'columnMenu',
    children: visibleMenuItems,
  }
}

/**
 * 행 높이 메뉴 아이템 생성
 */
const createRowHeightMenuItems = (): RealGridContextMenuItem => {
  return {
    label: '행 높이',
    children: [
      { label: '보통 (28px)', tag: 'rowHeight:normal' },
      { label: '좁게 (20px)', tag: 'rowHeight:small' },
      { label: '넓게 (36px)', tag: 'rowHeight:large' },
    ],
  }
}

/**
 * 내보내기 메뉴 아이템 생성
 */
const createExportMenuItems = (): RealGridContextMenuItem => {
  return {
    label: '내보내기',
    children: [
      { label: 'Excel 내보내기', tag: 'export:excel' },
      { label: 'CSV 내보내기', tag: 'export:csv' },
      { label: 'JSON 내보내기', tag: 'export:json' },
      { label: '-' },
      { label: '클립보드로 복사', tag: 'export:clipboard' },
    ],
  }
}

// ============================================================================
// 기본 액션 핸들러
// ============================================================================

/**
 * 기본 컨텍스트 메뉴 액션 맵
 */
const createDefaultActions = (
  exportHandler?: (options: RealGridExportOptions) => void,
): ContextMenuActionMap => {
  return {
    // 행 고정
    'fix:row:1': grid => grid.setFixedOptions({ rowCount: 1 }),
    'fix:row:2': grid => grid.setFixedOptions({ rowCount: 2 }),
    'fix:row:current': (grid, _data, cell) => {
      grid.setFixedOptions({ rowCount: cell.itemIndex + 1 })
    },

    // 컬럼 고정
    'fix:col:1': grid => grid.setFixedOptions({ colCount: 1 }),
    'fix:col:2': grid => grid.setFixedOptions({ colCount: 2 }),
    'fix:col:current': (grid, _data, cell) => {
      const column = grid.columnByName(cell.column)
      if (column && typeof column.index === 'number') {
        grid.setFixedOptions({ colCount: column.index + 1 })
      }
    },

    // 고정 취소
    'fix:cancel': grid => grid.setFixedOptions({ colCount: 0, rowCount: 0 }),

    // 컬럼 모두 보기
    'column:showAll': (grid) => {
      const columns = grid.getColumns()
      for (const column of columns) {
        if (column.name) {
          grid.setColumnProperty(column.name, 'visible', true)
        }
      }
    },

    // 현재 컬럼 자동 필터
    'column:autoFilter': (grid, _data, cell) => {
      const column = grid.columnByName(cell.column)
      if (column) {
        column.autoFilter = true
        grid.refresh()
      }
    },

    // 행 높이 설정
    'rowHeight:normal': (grid) => {
      grid.displayOptions.rowHeight = 28
    },
    'rowHeight:small': (grid) => {
      grid.displayOptions.rowHeight = 20
    },
    'rowHeight:large': (grid) => {
      grid.displayOptions.rowHeight = 36
    },

    // 내보내기
    'export:excel': (grid) => {
      if (exportHandler) {
        exportHandler({ type: 'excel' })
      }
      else {
        grid.exportGrid({ type: 'excel' })
      }
    },
    'export:csv': (grid) => {
      if (exportHandler) {
        exportHandler({ type: 'csv' })
      }
      else {
        grid.exportGrid({ type: 'csv' })
      }
    },
    'export:json': (_grid) => {
      if (exportHandler) {
        exportHandler({ type: 'json' })
      }
    },
    'export:clipboard': (_grid) => {
      if (exportHandler) {
        exportHandler({ type: 'clipboard' })
      }
    },
  }
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridContextMenuOptions {
  options?: RealGridContextMenuOptions
  onExport?: (options: RealGridExportOptions) => void
}

/**
 * RealGrid 컨텍스트 메뉴 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { setupContextMenu, handleContextMenuClick } = useRealGridContextMenu({
 *   options: {
 *     showFixedMenu: true,
 *     showColumnMenu: true,
 *     showExportMenu: true,
 *     customMenuItems: [
 *       { label: '사용자 정의', tag: 'custom:action' }
 *     ],
 *     customActions: {
 *       'custom:action': (grid, data, cell) => {
 *         console.log('Custom action!', cell)
 *       }
 *     }
 *   }
 * })
 *
 * // 그리드 초기화 후
 * setupContextMenu(gridView)
 * gridView.onContextMenuItemClicked = handleContextMenuClick
 * </script>
 * ```
 */
export const useRealGridContextMenu = (composableOptions: UseRealGridContextMenuOptions = {}) => {
  const menuOptions = ref<RealGridContextMenuOptions>(
    composableOptions.options || {
      showFixedMenu: true,
      showColumnMenu: true,
      showRowHeightMenu: true,
      showExportMenu: true,
      showFilterMenu: true,
    },
  )

  // 액션 맵 초기화
  const actionMap = ref<ContextMenuActionMap>({
    ...createDefaultActions(composableOptions.onExport),
    ...composableOptions.options?.customActions,
  })

  /**
   * 컨텍스트 메뉴 설정
   */
  const setupContextMenu = (grid: GridView): void => {
    // 메뉴 빌드 및 설정을 위한 이벤트 핸들러 등록
    grid.onContextMenuPopup = (g) => {
      const menu = buildContextMenu(g as GridView)
      g.setContextMenu(menu)
      return true
    }
  }

  /**
   * 동적 컨텍스트 메뉴 빌드
   */
  const buildContextMenu = (grid: GridView): RealGridContextMenuItem[] => {
    const current = grid.getCurrent()
    const currentRow = (current.itemIndex ?? 0) + 1
    const columnName = typeof current.column === 'string' ? current.column : (current.column?.name ?? '')
    const column = columnName ? grid.columnByName(columnName) : null
    const currentColumnText = column?.header?.text || columnName

    const menu: RealGridContextMenuItem[] = []

    // 고정 메뉴
    if (menuOptions.value.showFixedMenu) {
      menu.push(createFixedMenuItems(grid, currentRow, currentColumnText))
    }

    // 컬럼 표시/숨김 메뉴
    if (menuOptions.value.showColumnMenu) {
      menu.push(createColumnVisibilityMenuItems(grid))
    }

    // 행 높이 메뉴
    if (menuOptions.value.showRowHeightMenu) {
      menu.push(createRowHeightMenuItems())
    }

    // 구분선
    if (menu.length > 0 && (menuOptions.value.showExportMenu || menuOptions.value.customMenuItems?.length)) {
      menu.push({ label: '-' })
    }

    // 내보내기 메뉴
    if (menuOptions.value.showExportMenu) {
      menu.push(createExportMenuItems())
    }

    // 사용자 정의 메뉴
    if (menuOptions.value.customMenuItems?.length) {
      menu.push({ label: '-' })
      menu.push(...menuOptions.value.customMenuItems)
    }

    return menu
  }

  /**
   * 컨텍스트 메뉴 클릭 핸들러
   * RealGrid 이벤트 시그니처: (grid, item: PopupMenuItem, clickData: ClickData)
   */
  const handleContextMenuClick = (
    grid: GridView,
    data: RealGridContextMenuClickData,
    _clickData?: unknown,
  ): void => {
    const cell = grid.getCurrent()

    // 컬럼 토글 처리 (동적 태그)
    if (data.tag?.startsWith('column:toggle:')) {
      const columnName = data.tag.replace('column:toggle:', '')
      grid.setColumnProperty(columnName, 'visible', data.checked)
      return
    }

    // 액션 맵에서 핸들러 찾기
    const tag = data.tag || ''
    const handler = actionMap.value[tag]

    if (handler) {
      // CellIndex → RealGridCellClickData 변환 (타입 안전하게)
      const columnName = typeof cell.column === 'string' ? cell.column : (cell.column?.name ?? '')
      handler(grid, data, {
        cellIndex: (cell as unknown as { cellIndex?: number }).cellIndex ?? 0,
        column: columnName,
        dataRow: cell.dataRow ?? 0,
        fieldIndex: (cell as unknown as { fieldIndex?: number }).fieldIndex ?? 0,
        fieldName: (cell as unknown as { fieldName?: string }).fieldName ?? columnName,
        itemIndex: cell.itemIndex ?? 0,
      })
    }
  }

  /**
   * 커스텀 액션 등록
   */
  const registerAction = (tag: string, handler: ContextMenuActionHandler): void => {
    actionMap.value[tag] = handler
  }

  /**
   * 커스텀 액션 제거
   */
  const unregisterAction = (tag: string): void => {
    const { [tag]: _, ...rest } = actionMap.value
    actionMap.value = rest
  }

  /**
   * 메뉴 옵션 업데이트
   */
  const updateMenuOptions = (newOptions: Partial<RealGridContextMenuOptions>): void => {
    menuOptions.value = { ...menuOptions.value, ...newOptions }
  }

  /**
   * 커스텀 메뉴 아이템 추가
   */
  const addCustomMenuItem = (item: RealGridContextMenuItem): void => {
    if (!menuOptions.value.customMenuItems) {
      menuOptions.value.customMenuItems = []
    }
    menuOptions.value.customMenuItems.push(item)
  }

  /**
   * 커스텀 메뉴 아이템 제거
   */
  const removeCustomMenuItem = (tag: string): void => {
    if (menuOptions.value.customMenuItems) {
      menuOptions.value.customMenuItems = menuOptions.value.customMenuItems.filter(
        item => item.tag !== tag,
      )
    }
  }

  return {
    // 설정 및 핸들러
    setupContextMenu,
    buildContextMenu,
    handleContextMenuClick,

    // 액션 관리
    registerAction,
    unregisterAction,

    // 메뉴 옵션 관리
    updateMenuOptions,
    addCustomMenuItem,
    removeCustomMenuItem,

    // 상태 접근
    menuOptions: readonly(menuOptions),
    actionMap: readonly(actionMap),
  }
}

export default useRealGridContextMenu
