<script setup lang="ts">
import Paginator from 'primevue/paginator'
import type { PaginatorProps } from 'primevue/paginator'

interface Props extends /* @vue-ignore */ PaginatorProps {
  /** 총 레코드 수 */
  totalRecords?: number
  /** 페이지당 행 수 */
  rows?: number
  /** 첫 번째 인덱스 */
  first?: number
  /** 페이지당 행 수 옵션 */
  rowsPerPageOptions?: number[]
  /** 페이지 링크 수 */
  pageLinkSize?: number
  /** 현재 페이지 리포트 템플릿 */
  currentPageReportTemplate?: string
  /** 전체 항목 수 표시 */
  showCurrentPageReport?: boolean
  /** 첫 페이지/마지막 페이지 버튼 */
  showFirstLastIcon?: boolean
  /** 점프 to 페이지 */
  showJumpToPageDropdown?: boolean
}

withDefaults(defineProps<Props>(), {
  rows: 10,
  first: 0,
  rowsPerPageOptions: () => [10, 20, 50],
  pageLinkSize: 5,
  currentPageReportTemplate: '{currentPage} / {totalPages}',
})

// PrimeVue PageState 타입과 호환되도록 pageCount를 optional로 정의
interface PageEvent {
  first: number
  rows: number
  page: number
  pageCount?: number
}

defineEmits<{
  'page': [event: PageEvent]
  'update:first': [value: number]
  'update:rows': [value: number]
}>()
</script>

<template>
  <Paginator
    :total-records="totalRecords"
    :rows="rows"
    :first="first"
    :rows-per-page-options="rowsPerPageOptions"
    :page-link-size="pageLinkSize"
    :current-page-report-template="currentPageReportTemplate"
    :show-current-page-report="showCurrentPageReport"
    :show-first-last-icon="showFirstLastIcon"
    :show-jump-to-page-dropdown="showJumpToPageDropdown"
    v-bind="$attrs"
    @page="$emit('page', $event)"
    @update:first="$emit('update:first', $event)"
    @update:rows="$emit('update:rows', $event)"
  />
</template>
