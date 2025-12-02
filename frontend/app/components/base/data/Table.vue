<script setup lang="ts" generic="T">
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import type { DataTableProps } from 'primevue/datatable'

interface ColumnDef {
  field: string
  header: string
  sortable?: boolean
  style?: string
  headerStyle?: string
}

interface Props extends /* @vue-ignore */ Omit<DataTableProps, 'value'> {
  /** 테이블 데이터 */
  value?: T[]
  /** 컬럼 정의 */
  columns?: ColumnDef[]
  /** 선택된 행 */
  selection?: T | T[] | null
  /** 선택 모드 */
  selectionMode?: 'single' | 'multiple'
  /** 로딩 상태 */
  loading?: boolean
  /** 페이지네이션 */
  paginator?: boolean
  /** 페이지당 행 수 */
  rows?: number
  /** 페이지당 행 수 옵션 */
  rowsPerPageOptions?: number[]
  /** 정렬 모드 */
  sortMode?: 'single' | 'multiple'
  /** 스트라이프 */
  stripedRows?: boolean
  /** 행 호버 효과 */
  rowHover?: boolean
  /** 그리드 라인 */
  showGridlines?: boolean
  /** 반응형 레이아웃 */
  responsiveLayout?: 'scroll' | 'stack'
  /** 스크롤 가능 */
  scrollable?: boolean
  /** 스크롤 높이 */
  scrollHeight?: string
}

withDefaults(defineProps<Props>(), {
  value: () => [],
  columns: () => [],
  rows: 10,
  rowsPerPageOptions: () => [10, 20, 50],
  sortMode: 'single',
  responsiveLayout: 'scroll',
})

// DataTable 이벤트 타입 정의
interface SortEvent {
  sortField: string | null
  sortOrder: number | null
}

defineEmits<{
  'update:selection': [value: T | T[] | null]
  'row-select': [event: { data: T }]
  'row-unselect': [event: { data: T }]
  'page': [event: { first: number, rows: number, page: number }]
  'sort': [event: SortEvent]
}>()
</script>

<template>
  <DataTable
    :value="value"
    :selection="selection"
    :selection-mode="selectionMode"
    :loading="loading"
    :paginator="paginator"
    :rows="rows"
    :rows-per-page-options="rowsPerPageOptions"
    :sort-mode="sortMode"
    :striped-rows="stripedRows"
    :row-hover="rowHover"
    :show-gridlines="showGridlines"
    :responsive-layout="responsiveLayout"
    :scrollable="scrollable"
    :scroll-height="scrollHeight"
    v-bind="$attrs"
    @update:selection="$emit('update:selection', $event)"
    @row-select="$emit('row-select', $event)"
    @row-unselect="$emit('row-unselect', $event)"
    @page="$emit('page', $event)"
    @sort="$emit('sort', $event as SortEvent)"
  >
    <Column
      v-for="col in columns"
      :key="col.field"
      :field="col.field"
      :header="col.header"
      :sortable="col.sortable"
      :style="col.style"
      :header-style="col.headerStyle"
    />
    <slot />
  </DataTable>
</template>
