<script setup lang="ts">
import type { GridView, LocalDataProvider } from 'realgrid'
import { useThemeStore } from '~/stores/theme'
import { useAppToast } from '~/composables/useAppToast'
import type { ThemeName } from '~/themes'
import { THEMES } from '~/themes'
import type {
  RealGridColumn,
  RealGridInstance,
  RealGridCellClickData,
  RealGridColumnValidation,
  InfiniteScrollLoadFn,
} from '~/types/realgrid'

// 🚀 DockView 지연 로딩 (탭 활성화 시에만 로드)
const DockviewVue = defineAsyncComponent(() =>
  import('dockview-vue').then(m => m.DockviewVue),
)

// DockView 테마 (지연 로드)
const dockviewThemes = shallowRef<{ themeLight: unknown, themeAbyss: unknown } | null>(null)
async function loadDockviewThemes() {
  if (!dockviewThemes.value) {
    const { themeLight, themeAbyss } = await import('dockview-core')
    dockviewThemes.value = { themeLight, themeAbyss }
  }
  return dockviewThemes.value
}

// DockviewReadyEvent 타입 (동적 import를 위해)
type DockviewReadyEvent = { api: unknown }

// DockView 로딩 상태
const dockviewLoading = ref(false)

const themeStore = useThemeStore()
const toast = useAppToast()

// Dockview 테마 (다크/라이트에 따라 공식 테마 객체 반환)
const dockviewTheme = computed(() => {
  if (!dockviewThemes.value) return null
  return themeStore.isDark ? dockviewThemes.value.themeAbyss : dockviewThemes.value.themeLight
})

// 테마 프리뷰 상태
const previewingTheme = ref<ThemeName | null>(null)
const selectedThemeForDetail = computed(() => themeStore.themeName)

// 테마 목록 (다크/라이트 분리)
const darkThemeEntries = computed(() =>
  Object.entries(THEMES).filter(([_, config]) => config.prefersDark) as [ThemeName, typeof THEMES[ThemeName]][],
)
const lightThemeEntries = computed(() =>
  Object.entries(THEMES).filter(([_, config]) => !config.prefersDark) as [ThemeName, typeof THEMES[ThemeName]][],
)

// 테마 Select 옵션
interface ThemeSelectOption {
  label: string
  value: ThemeName
}

const themeSelectOptions = computed((): ThemeSelectOption[] =>
  themeStore.availableThemes.map(theme => ({
    label: theme.label,
    value: theme.value,
  })),
)
const selectedThemeOption = computed({
  get: () => themeStore.themeName,
  set: (value: ThemeName) => {
    themeStore.setTheme(value)
    toast.success(`테마 변경: ${THEMES[value].name}`)
  },
})

// Select 옵션 타입
interface SelectOption {
  label: string
  value: string
}

// Form 상태
const inputValue = ref('')
const textareaValue = ref('')
const selectValue = ref<SelectOption | null>(null)
const multiSelectValue = ref<SelectOption[]>([])
const checkboxValue = ref(false)
const radioValue = ref('option1')
const switchValue = ref(false)
const numberValue = ref(10)
const calendarValue = ref<Date | null>(null)
const passwordValue = ref('')
const searchQuery = ref('')

const selectOptions = [
  { label: '옵션 1', value: 'option1' },
  { label: '옵션 2', value: 'option2' },
  { label: '옵션 3', value: 'option3' },
]

// Data 상태
const tableData = ref([
  { id: 1, name: '홍길동', email: 'hong@example.com', status: 'active' },
  { id: 2, name: '김철수', email: 'kim@example.com', status: 'inactive' },
  { id: 3, name: '이영희', email: 'lee@example.com', status: 'pending' },
  { id: 4, name: '박민수', email: 'park@example.com', status: 'active' },
  { id: 5, name: '정수진', email: 'jung@example.com', status: 'active' },
])
const tableColumns = [
  { field: 'name', header: '이름', sortable: true },
  { field: 'email', header: '이메일', sortable: true },
  { field: 'status', header: '상태', sortable: true },
]

const treeData = ref([
  {
    key: '0',
    label: 'Documents',
    icon: 'pi pi-folder',
    children: [
      { key: '0-0', label: 'Work', icon: 'pi pi-folder' },
      { key: '0-1', label: 'Personal', icon: 'pi pi-folder' },
    ],
  },
])

// Panel 상태
const activeTab = ref('form')
const accordionValue = ref<string[]>(['0'])

// 🚀 DockView 탭 활성화 시 테마 지연 로딩
watch(activeTab, async (newTab) => {
  if (newTab === 'dockview' && !dockviewThemes.value) {
    dockviewLoading.value = true
    try {
      await loadDockviewThemes()
    }
    finally {
      dockviewLoading.value = false
    }
  }
})

// Overlay 상태
const dialogVisible = ref(false)
const drawerVisible = ref(false)

// Menu 상태
const menuItems = [
  { label: 'Home', icon: 'pi pi-home' },
  { label: 'Products', icon: 'pi pi-box' },
  { label: 'Contact', icon: 'pi pi-envelope' },
]

const breadcrumbItems = [
  { label: 'Home' },
  { label: 'Products' },
  { label: 'Details' },
]

const stepsItems = [
  { label: '정보 입력' },
  { label: '확인' },
  { label: '완료' },
]
const activeStep = ref(0)

// Handlers
function handleThemeChange(name: ThemeName) {
  themeStore.setTheme(name)
  toast.success(`테마 변경: ${name}`)
}

function handleModeChange(mode: 'system' | 'dark' | 'light') {
  themeStore.setMode(mode)
  const modeLabel = mode === 'system' ? '시스템' : mode === 'dark' ? '다크' : '라이트'
  toast.success(`모드 변경: ${modeLabel}`)
}

function handleThemePreviewStart(themeName: ThemeName) {
  previewingTheme.value = themeName
  themeStore.startPreview(themeName)
}

function handleThemePreviewEnd() {
  previewingTheme.value = null
  themeStore.cancelPreview()
}

function handleThemeSelect(themeName: ThemeName, event: MouseEvent | KeyboardEvent) {
  previewingTheme.value = null
  // KeyboardEvent는 clientX/Y가 없으므로 MouseEvent만 전달
  const mouseEvent = 'clientX' in event ? event as MouseEvent : undefined
  themeStore.setTheme(themeName, mouseEvent)
  toast.success(`테마 변경: ${THEMES[themeName].name}`)
}

function handleSearch(query: string) {
  toast.info(`검색: ${query}`)
}

function handleConfirm() {
  toast.success('확인되었습니다')
}

function handleRefresh() {
  toast.info('새로고침')
}

function handleAdd() {
  toast.success('추가됨')
}

// ============================================================================
// RealGrid Demo State
// ============================================================================

// RealGrid 데모 컬럼 정의
const realgridColumns: RealGridColumn[] = [
  {
    name: 'id',
    fieldName: 'id',
    type: 'text',
    width: 60,
    header: { text: 'ID' },
  },
  {
    name: 'name',
    fieldName: 'name',
    type: 'text',
    width: 120,
    header: { text: '이름' },
  },
  {
    name: 'email',
    fieldName: 'email',
    type: 'text',
    width: 180,
    header: { text: '이메일' },
  },
  {
    name: 'department',
    fieldName: 'department',
    type: 'text',
    width: 100,
    header: { text: '부서' },
  },
  {
    name: 'score',
    fieldName: 'score',
    type: 'number',
    width: 80,
    header: { text: '점수' },
  },
  {
    name: 'status',
    fieldName: 'status',
    type: 'text',
    width: 80,
    header: { text: '상태' },
  },
]

// RealGrid 샘플 데이터
const realgridData = ref([
  { id: '1', name: '김철수', email: 'kim@example.com', department: '개발팀', score: 85, status: 'active' },
  { id: '2', name: '이영희', email: 'lee@example.com', department: '기획팀', score: 92, status: 'inactive' },
  { id: '3', name: '박민수', email: 'park@example.com', department: '인사팀', score: 78, status: 'pending' },
  { id: '4', name: '최지현', email: 'choi@example.com', department: '마케팅팀', score: 95, status: 'active' },
  { id: '5', name: '정수연', email: 'jung@example.com', department: '영업팀', score: 88, status: 'active' },
])

// RealGrid 인스턴스 참조 (RealGrid.vue에서 expose한 타입)
interface RealGridComponentExpose {
  getGridInstance: () => RealGridInstance | null
  updateData: (data: Record<string, unknown>[]) => void
  exportExcel: (fileName?: string) => void
  exportCsv: (fileName?: string) => void
  exportJson: (fileName?: string) => void
  copyToClipboard: () => Promise<boolean>
  validateAll: () => { valid: boolean, errors: { row: number, column: string, message: string }[] }
  goToFirstError: () => void
  validationErrors?: { row: number, column: string, message: string }[]
  isValid?: boolean
  selectionSummary: { sum: number, count: number, average: number, min: number, max: number, numericCount: number }
  getSelectionSum: () => number
  saveState: () => void
  loadState: () => boolean
  clearState: () => void
  pagination?: {
    state: { currentPage: number, totalItems: number, itemsPerPage: number }
    pageNumbers: { value: number[] }
    canGoPrev: { value: boolean }
    canGoNext: { value: boolean }
    goToPage: (page: number) => Promise<void>
    nextPage: () => Promise<void>
    prevPage: () => Promise<void>
    goToFirst: () => Promise<void>
    goToLast: () => Promise<void>
    setTotalItems: (n: number) => void
  }
  infiniteScroll?: {
    state: { isLoading: boolean, hasMore: boolean, currentOffset: number, pageSize: number, totalItems: number }
    loadMore: () => Promise<void>
    reset: () => Promise<void>
  }
}
const realgridRef = ref<RealGridComponentExpose | null>(null)

// RealGrid 이벤트 핸들러
function onRealgridReady(_grid: GridView, _provider: LocalDataProvider) {
  toast.info('RealGrid 초기화 완료')
}

function onRealgridCellClick(_grid: GridView, clickData: RealGridCellClickData) {
  toast.info(`셀 클릭: ${clickData.column} (Row: ${clickData.dataRow})`)
}

// RealGrid 행 추가
function addRealgridRow() {
  const newId = (realgridData.value.length + 1).toString()
  realgridData.value.push({
    id: newId,
    name: `신규 사원 ${newId}`,
    email: `new${newId}@example.com`,
    department: '미정',
    score: Math.floor(Math.random() * 30) + 70, // 70~99 랜덤 점수
    status: 'pending',
  })
  toast.success('행 추가됨')
}

// RealGrid 마지막 행 삭제
function removeRealgridRow() {
  if (realgridData.value.length > 1) {
    realgridData.value.pop()
    toast.success('행 삭제됨')
  }
  else {
    toast.warn('최소 1개 행이 필요합니다')
  }
}

// RealGrid 내보내기 핸들러
function exportRealgridExcel() {
  realgridRef.value?.exportExcel?.('realgrid-demo')
  toast.success('Excel 내보내기 완료')
}

function exportRealgridCsv() {
  realgridRef.value?.exportCsv?.('realgrid-demo')
  toast.success('CSV 내보내기 완료')
}

function exportRealgridJson() {
  realgridRef.value?.exportJson?.('realgrid-demo')
  toast.success('JSON 내보내기 완료')
}

// ============================================================================
// RealGrid 2: 상태 저장 + 유효성 검사 + 페이지네이션 데모
// ============================================================================

// 부서 목록
const departments = ['개발팀', '기획팀', '인사팀', '마케팅팀', '영업팀', '재무팀', '디자인팀', 'QA팀']
const statuses = ['active', 'inactive', 'pending']

// 대용량 샘플 데이터 생성 (100건)
function generateSampleData(count: number) {
  const names = ['김철수', '이영희', '박민수', '최지현', '정수연', '홍길동', '강미나', '윤서준', '임도현', '송하늘']
  const data = []
  for (let i = 1; i <= count; i++) {
    data.push({
      id: String(i),
      name: (names[i % names.length] ?? '사원') + (Math.floor(i / names.length) || ''),
      email: `user${i}@example.com`,
      department: departments[i % departments.length],
      status: statuses[i % statuses.length],
      salary: Math.floor(3000 + Math.random() * 7000) * 10000, // 3000만원 ~ 1억
      hireDate: new Date(2020 + Math.floor(i / 30), i % 12, (i % 28) + 1).toISOString().split('T')[0],
    })
  }
  return data
}

// 그리드 2 컬럼 정의 (급여, 입사일 추가)
const realgrid2Columns: RealGridColumn[] = [
  { name: 'id', fieldName: 'id', type: 'text', width: 60, header: { text: 'ID' } },
  { name: 'name', fieldName: 'name', type: 'text', width: 100, header: { text: '이름' }, editable: true },
  { name: 'email', fieldName: 'email', type: 'text', width: 180, header: { text: '이메일' }, editable: true },
  { name: 'department', fieldName: 'department', type: 'text', width: 100, header: { text: '부서' } },
  { name: 'salary', fieldName: 'salary', type: 'number', width: 120, header: { text: '급여' } },
  { name: 'status', fieldName: 'status', type: 'text', width: 80, header: { text: '상태' } },
]

// 🚀 그리드 2 데이터 (100건 - 지연 생성)
const realgrid2AllData = ref<Record<string, unknown>[]>([])
const realgrid2Data = ref<Record<string, unknown>[]>([])
const realgrid2TotalItems = ref(100)
const realgrid2Ref = ref<RealGridComponentExpose | null>(null)
const realgrid2Initialized = ref(false)

// 유효성 검사 규칙
const realgrid2Validations: RealGridColumnValidation[] = [
  {
    column: 'name',
    rules: [
      { type: 'required', message: '이름은 필수입니다' },
    ],
  },
  {
    column: 'email',
    rules: [
      { type: 'required', message: '이메일은 필수입니다' },
      { type: 'pattern', value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '올바른 이메일 형식이 아닙니다' },
    ],
  },
]

// 그리드 2 이벤트 핸들러
function onRealgrid2Ready(_grid: GridView, _provider: LocalDataProvider) {
  toast.info('RealGrid 2 (페이지네이션) 초기화 완료')

  // 🚀 데이터 지연 생성 (최초 1회)
  if (!realgrid2Initialized.value) {
    realgrid2AllData.value = generateSampleData(100)
    realgrid2Initialized.value = true
  }

  // 페이지네이션 총 아이템 수 설정
  nextTick(() => {
    realgrid2Ref.value?.pagination?.setTotalItems(realgrid2TotalItems.value)
  })
  // 페이지네이션 초기 데이터 로드
  loadPage2Data(1)
}

function onRealgrid2ValidationError(errors: { row: number, column: string, message: string }[]) {
  const firstError = errors[0]
  if (firstError) {
    toast.error(`유효성 검사 오류: ${firstError.message}`)
  }
}

// 페이지네이션 데이터 로드
async function loadPage2Data(page: number) {
  const pageSize = 10
  const start = (page - 1) * pageSize
  const end = start + pageSize
  realgrid2Data.value = realgrid2AllData.value.slice(start, end)
}

// 페이지 변경 핸들러
async function onPage2Change(page: number) {
  toast.info(`페이지 ${page} 로딩 중...`)
  await loadPage2Data(page)
}

// 그리드 2 상태 저장/로드
function saveGrid2State() {
  realgrid2Ref.value?.saveState?.()
  toast.success('그리드 상태 저장됨')
}

function loadGrid2State() {
  const result = realgrid2Ref.value?.loadState?.()
  if (result) {
    toast.success('그리드 상태 복원됨')
  }
  else {
    toast.warn('저장된 상태가 없습니다')
  }
}

function clearGrid2State() {
  realgrid2Ref.value?.clearState?.()
  toast.info('저장된 상태 삭제됨')
}

// 그리드 2 유효성 검사
function validateGrid2() {
  const result = realgrid2Ref.value?.validateAll?.()
  if (result?.valid) {
    toast.success('유효성 검사 통과!')
  }
  else {
    toast.error(`${result?.errors?.length || 0}건의 오류 발견`)
    realgrid2Ref.value?.goToFirstError?.()
  }
}

// ============================================================================
// RealGrid 3: 무한 스크롤 데모
// ============================================================================

const realgrid3Columns: RealGridColumn[] = [
  { name: 'id', fieldName: 'id', type: 'text', width: 60, header: { text: 'ID' } },
  { name: 'name', fieldName: 'name', type: 'text', width: 100, header: { text: '이름' } },
  { name: 'email', fieldName: 'email', type: 'text', width: 180, header: { text: '이메일' } },
  { name: 'department', fieldName: 'department', type: 'text', width: 100, header: { text: '부서' } },
  { name: 'salary', fieldName: 'salary', type: 'number', width: 120, header: { text: '급여' } },
  { name: 'hireDate', fieldName: 'hireDate', type: 'text', width: 100, header: { text: '입사일' } },
]

// 🚀 무한 스크롤 전체 데이터 (500건 - 지연 생성)
let realgrid3AllData: Record<string, unknown>[] = []
const realgrid3Data = ref<Record<string, unknown>[]>([])
const realgrid3Ref = ref<RealGridComponentExpose | null>(null)
const realgrid3Initialized = ref(false)

// 무한 스크롤 데이터 로드 함수
const loadGrid3Data: InfiniteScrollLoadFn = async (offset: number, limit: number) => {
  // 🚀 데이터 지연 생성 (최초 1회)
  if (!realgrid3Initialized.value) {
    realgrid3AllData = generateSampleData(500)
    realgrid3Initialized.value = true
  }

  // 네트워크 지연 시뮬레이션
  await new Promise(resolve => setTimeout(resolve, 500))

  const data = realgrid3AllData.slice(offset, offset + limit)
  const hasMore = offset + limit < realgrid3AllData.length

  return {
    data,
    hasMore,
    total: realgrid3AllData.length,
  }
}

// 그리드 3 이벤트 핸들러
async function onRealgrid3Ready(_grid: GridView, _provider: LocalDataProvider) {
  toast.info('RealGrid 3 (무한 스크롤) 초기화 완료')
  // 초기 데이터 로드 (무한 스크롤 시작)
  await nextTick()
  await realgrid3Ref.value?.infiniteScroll?.loadMore()
}

// 무한 스크롤 리셋
async function resetGrid3InfiniteScroll() {
  await realgrid3Ref.value?.infiniteScroll?.reset?.()
  toast.info('무한 스크롤 초기화됨')
}

// ============================================================================
// DockView Demo State
// ============================================================================

// DockView API 참조
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const dockviewApi = ref<any>(null)

// DockView 패널 카운터
const dockviewPanelCount = ref(3)

// DockView 준비 핸들러
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onDockviewReady(event: DockviewReadyEvent) {
  const api = event.api as any
  dockviewApi.value = api

  // 초기 패널 구성
  api.addPanel({
    id: 'panel1',
    component: 'panelComponent',
    params: { title: '패널 1' },
  })

  api.addPanel({
    id: 'panel2',
    component: 'panelComponent',
    params: { title: '패널 2' },
    position: { referencePanel: 'panel1', direction: 'right' },
  })

  api.addPanel({
    id: 'panel3',
    component: 'panelComponent',
    params: { title: '패널 3' },
    position: { referencePanel: 'panel1', direction: 'below' },
  })

  toast.info('DockView 초기화 완료')
}

// DockView 패널 추가
function addDockviewPanel() {
  if (!dockviewApi.value)
    return

  dockviewPanelCount.value++
  const panelId = `panel${dockviewPanelCount.value}`

  dockviewApi.value.addPanel({
    id: panelId,
    component: 'panelComponent',
    params: { title: `패널 ${dockviewPanelCount.value}` },
  })

  toast.success(`패널 ${dockviewPanelCount.value} 추가됨`)
}

// DockView 모든 패널 리셋
function resetDockviewPanels() {
  if (!dockviewApi.value)
    return

  // 모든 패널 ID 수집 후 제거
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const panelIds = dockviewApi.value.panels.map((p: any) => p.id)
  panelIds.forEach((id: string) => {
    const panel = dockviewApi.value!.getPanel(id)
    if (panel) {
      dockviewApi.value!.removePanel(panel)
    }
  })

  // 초기 상태로 복원
  dockviewPanelCount.value = 3

  dockviewApi.value.addPanel({
    id: 'panel1',
    component: 'panelComponent',
    params: { title: '패널 1' },
  })

  dockviewApi.value.addPanel({
    id: 'panel2',
    component: 'panelComponent',
    params: { title: '패널 2' },
    position: { referencePanel: 'panel1', direction: 'right' },
  })

  dockviewApi.value.addPanel({
    id: 'panel3',
    component: 'panelComponent',
    params: { title: '패널 3' },
    position: { referencePanel: 'panel1', direction: 'below' },
  })

  toast.info('패널 초기화 완료')
}
</script>

<template>
  <NuxtLayout>
    <div class="max-w-7xl mx-auto p-8 space-y-8">
      <!-- 헤더 -->
      <header class="text-center space-y-4">
        <h1 class="text-4xl font-bold">
          컴포넌트 라이브러리
        </h1>
        <p class="text-lg opacity-70">
          35개의 PrimeVue 기반 컴포넌트
        </p>
        <div class="flex justify-center gap-2">
          <FeedbackBadge
            value="Base: 28"
            severity="info"
          />
          <FeedbackBadge
            value="Composite: 7"
            severity="success"
          />
          <FeedbackBadge
            value="Common: 3"
            severity="secondary"
          />
        </div>
      </header>

      <!-- 테마 설정 -->
      <section>
        <h2 class="text-2xl font-semibold mb-4">
          테마 설정
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <PanelCard title="테마 선택">
            <div class="flex gap-2 flex-wrap">
              <ActionButton
                v-for="theme in themeStore.availableThemes"
                :key="theme.value"
                :label="theme.label"
                :severity="themeStore.themeName === theme.value ? 'info' : 'secondary'"
                @click="handleThemeChange(theme.value)"
              />
            </div>
          </PanelCard>

          <PanelCard title="다크모드 설정">
            <div class="space-y-3">
              <div class="flex gap-2 flex-wrap">
                <ActionButton
                  label="시스템"
                  icon="pi pi-desktop"
                  :severity="themeStore.themeMode === 'system' ? 'info' : 'secondary'"
                  @click="handleModeChange('system')"
                />
                <ActionButton
                  label="라이트"
                  icon="pi pi-sun"
                  :severity="themeStore.themeMode === 'light' ? 'info' : 'secondary'"
                  @click="handleModeChange('light')"
                />
                <ActionButton
                  label="다크"
                  icon="pi pi-moon"
                  :severity="themeStore.themeMode === 'dark' ? 'info' : 'secondary'"
                  @click="handleModeChange('dark')"
                />
              </div>
              <div class="text-sm opacity-70">
                <p>현재 모드: <strong>{{ themeStore.themeMode }}</strong></p>
                <p>실제 테마: <strong>{{ themeStore.isDark ? '다크' : '라이트' }}</strong></p>
              </div>
            </div>
          </PanelCard>
        </div>

        <!-- 테마 프리뷰 카드 그리드 -->
        <div class="mt-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold">
              테마 프리뷰
            </h3>
            <div class="w-48">
              <!-- FormSelect의 제네릭 타입이 modelValue에서 추론되어 ThemeName[]을 기대하지만,
                   optionLabel/optionValue 사용 시 객체 배열이 필요하므로 타입 캐스팅 -->
              <FormSelect
                v-model="selectedThemeOption"
                :options="(themeSelectOptions as unknown as ThemeName[])"
                option-label="label"
                option-value="value"
                placeholder="테마 선택"
                fluid
              />
            </div>
          </div>

          <!-- 다크 테마 -->
          <div class="mb-4">
            <p class="text-sm font-medium text-muted-color mb-2 flex items-center gap-2">
              <i class="pi pi-moon" />
              다크 테마
            </p>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
              <ThemePreviewCard
                v-for="[name, config] in darkThemeEntries"
                :key="name"
                :theme-name="name"
                :theme="config"
                :selected="themeStore.themeName === name"
                :previewing="previewingTheme === name"
                @select="handleThemeSelect"
                @preview-start="handleThemePreviewStart"
                @preview-end="handleThemePreviewEnd"
              />
            </div>
          </div>

          <!-- 라이트 테마 -->
          <div class="mb-4">
            <p class="text-sm font-medium text-muted-color mb-2 flex items-center gap-2">
              <i class="pi pi-sun" />
              라이트 테마
            </p>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
              <ThemePreviewCard
                v-for="[name, config] in lightThemeEntries"
                :key="name"
                :theme-name="name"
                :theme="config"
                :selected="themeStore.themeName === name"
                :previewing="previewingTheme === name"
                @select="handleThemeSelect"
                @preview-start="handleThemePreviewStart"
                @preview-end="handleThemePreviewEnd"
              />
            </div>
          </div>

          <!-- 선택된 테마 상세 정보 -->
          <PanelCard class="mt-4">
            <template #title>
              <div class="flex items-center gap-2">
                <span
                  class="w-3 h-3 rounded-full"
                  :style="{ backgroundColor: THEMES[selectedThemeForDetail].accentColors[0] }"
                />
                {{ THEMES[selectedThemeForDetail].name }} 상세 정보
              </div>
            </template>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="space-y-2">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">설명:</span>
                  <span class="text-sm">{{ THEMES[selectedThemeForDetail].description }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">제작자:</span>
                  <span class="text-sm">{{ THEMES[selectedThemeForDetail].author }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">버전:</span>
                  <span class="text-sm">{{ THEMES[selectedThemeForDetail].version }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">폰트:</span>
                  <span class="text-sm">{{ THEMES[selectedThemeForDetail].fontStyle }}</span>
                </div>
              </div>
              <div class="space-y-2">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">모드:</span>
                  <FeedbackTag
                    :value="THEMES[selectedThemeForDetail].prefersDark ? '다크' : '라이트'"
                    :severity="THEMES[selectedThemeForDetail].prefersDark ? 'secondary' : 'info'"
                    :icon="THEMES[selectedThemeForDetail].prefersDark ? 'pi pi-moon' : 'pi pi-sun'"
                  />
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">태그:</span>
                  <div class="flex gap-1 flex-wrap">
                    <FeedbackTag
                      v-for="tag in THEMES[selectedThemeForDetail].tags"
                      :key="tag"
                      :value="tag"
                      severity="secondary"
                    />
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-muted-color w-20">대표색:</span>
                  <div class="flex gap-1">
                    <span
                      v-for="(color, idx) in THEMES[selectedThemeForDetail].accentColors"
                      :key="idx"
                      class="w-6 h-6 rounded border border-surface-200"
                      :style="{ backgroundColor: color }"
                      :title="color"
                    />
                  </div>
                </div>
              </div>
            </div>
          </PanelCard>
        </div>
      </section>

      <!-- 탭 네비게이션 -->
      <PanelTabs
        v-model:value="activeTab"
        :items="[
          { value: 'form', label: '📝 Form (10)', icon: 'pi pi-pencil' },
          { value: 'action', label: '🔘 Action (3)', icon: 'pi pi-bolt' },
          { value: 'data', label: '📊 Data (3)', icon: 'pi pi-table' },
          { value: 'panel', label: '📦 Panel (4)', icon: 'pi pi-box' },
          { value: 'overlay', label: '🪟 Overlay (4)', icon: 'pi pi-window-maximize' },
          { value: 'menu', label: '🧭 Menu (4)', icon: 'pi pi-bars' },
          { value: 'feedback', label: '💬 Feedback (4)', icon: 'pi pi-comment' },
          { value: 'composite', label: '🔗 Composite (5)', icon: 'pi pi-link' },
          { value: 'realgrid', label: '🗂️ RealGrid', icon: 'pi pi-th-large' },
          { value: 'dockview', label: '🪟 DockView', icon: 'pi pi-objects-column' },
        ]"
        scrollable
      >
        <!-- Form 탭 -->
        <template #form>
          <div class="space-y-6">
            <PanelCard title="기본 입력 컴포넌트">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <FormField
                  label="Input"
                  description="기본 텍스트 입력"
                  required
                >
                  <FormInput
                    v-model="inputValue"
                    placeholder="텍스트를 입력하세요"
                    fluid
                  />
                </FormField>

                <FormField
                  label="Textarea"
                  description="여러 줄 텍스트 입력"
                >
                  <FormTextarea
                    v-model="textareaValue"
                    placeholder="내용을 입력하세요"
                    :rows="3"
                  />
                </FormField>

                <FormField
                  label="Select"
                  description="단일 선택"
                >
                  <FormSelect
                    v-model="selectValue"
                    :options="selectOptions"
                    option-label="label"
                    option-value="value"
                    placeholder="옵션을 선택하세요"
                    fluid
                  />
                </FormField>

                <FormField
                  label="MultiSelect"
                  description="다중 선택"
                >
                  <FormMultiSelect
                    v-model="multiSelectValue"
                    :options="selectOptions"
                    option-label="label"
                    option-value="value"
                    placeholder="여러 옵션을 선택하세요"
                    display="chip"
                  />
                </FormField>

                <FormField
                  label="Number"
                  description="숫자 입력"
                >
                  <FormNumber
                    v-model="numberValue"
                    :min="0"
                    :max="100"
                    fluid
                  />
                </FormField>

                <FormField
                  label="Calendar"
                  description="날짜 선택"
                >
                  <FormCalendar
                    v-model="calendarValue"
                    placeholder="날짜를 선택하세요"
                  />
                </FormField>

                <FormField
                  label="Password"
                  description="비밀번호 입력"
                >
                  <FormPassword
                    v-model="passwordValue"
                    placeholder="비밀번호"
                    feedback
                    toggle-mask
                  />
                </FormField>
              </div>
            </PanelCard>

            <PanelCard title="체크박스 & 라디오 & 스위치">
              <div class="space-y-4">
                <FormField
                  label="Checkbox"
                  description="단일 체크박스"
                >
                  <FormCheckbox
                    v-model="checkboxValue"
                    label="이용 약관에 동의합니다"
                    binary
                  />
                </FormField>

                <FormField
                  label="Radio"
                  description="라디오 버튼 그룹"
                >
                  <div class="flex gap-4">
                    <FormRadio
                      v-model="radioValue"
                      value="option1"
                      label="옵션 1"
                    />
                    <FormRadio
                      v-model="radioValue"
                      value="option2"
                      label="옵션 2"
                    />
                    <FormRadio
                      v-model="radioValue"
                      value="option3"
                      label="옵션 3"
                    />
                  </div>
                </FormField>

                <FormField
                  label="Switch"
                  description="토글 스위치"
                >
                  <FormSwitch v-model="switchValue" />
                  <span class="ml-2">{{ switchValue ? '활성화' : '비활성화' }}</span>
                </FormField>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- Action 탭 -->
        <template #action>
          <div class="space-y-6">
            <PanelCard title="Button - 버튼 컴포넌트">
              <div class="space-y-4">
                <div>
                  <h3 class="font-semibold mb-2">
                    Severity
                  </h3>
                  <div class="flex gap-2 flex-wrap">
                    <ActionButton label="Primary" />
                    <ActionButton
                      label="Secondary"
                      severity="secondary"
                    />
                    <ActionButton
                      label="Success"
                      severity="success"
                    />
                    <ActionButton
                      label="Info"
                      severity="info"
                    />
                    <ActionButton
                      label="Warn"
                      severity="warn"
                    />
                    <ActionButton
                      label="Danger"
                      severity="danger"
                    />
                  </div>
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Variants
                  </h3>
                  <div class="flex gap-2 flex-wrap">
                    <ActionButton label="Solid" />
                    <ActionButton
                      label="Outlined"
                      outlined
                    />
                    <ActionButton
                      label="Text"
                      text
                    />
                  </div>
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Icons & Loading
                  </h3>
                  <div class="flex gap-2 flex-wrap">
                    <ActionButton
                      label="Icon"
                      icon="pi pi-check"
                    />
                    <ActionButton icon="pi pi-search" />
                    <ActionButton
                      label="Loading"
                      loading
                    />
                    <ActionButton
                      label="Disabled"
                      disabled
                    />
                  </div>
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Sizes
                  </h3>
                  <div class="flex gap-2 flex-wrap items-center">
                    <ActionButton
                      label="Small"
                      size="small"
                    />
                    <ActionButton label="Default" />
                    <ActionButton
                      label="Large"
                      size="large"
                    />
                  </div>
                </div>
              </div>
            </PanelCard>

            <PanelCard title="SplitButton & SpeedDial">
              <div class="space-y-4">
                <div>
                  <h3 class="font-semibold mb-2">
                    SplitButton
                  </h3>
                  <ActionSplitButton
                    label="저장"
                    :model="[
                      { label: '저장', icon: 'pi pi-save' },
                      { label: '다른 이름으로 저장', icon: 'pi pi-file' },
                      { label: '내보내기', icon: 'pi pi-download' },
                    ]"
                  />
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    SpeedDial
                  </h3>
                  <ActionSpeedDial
                    :model="[
                      { icon: 'pi pi-pencil', label: '수정' },
                      { icon: 'pi pi-trash', label: '삭제' },
                      { icon: 'pi pi-upload', label: '업로드' },
                    ]"
                  />
                </div>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- Data 탭 -->
        <template #data>
          <div class="space-y-6">
            <PanelCard title="Table - 데이터 테이블">
              <DataTable
                :value="tableData"
                :columns="tableColumns"
                paginator
                :rows="3"
                :rows-per-page-options="[3, 5, 10]"
              >
                <template #empty>
                  <div class="text-center py-4">
                    데이터가 없습니다
                  </div>
                </template>
              </DataTable>
            </PanelCard>

            <PanelCard title="Paginator - 페이지네이션">
              <DataPaginator
                :rows="10"
                :total-records="120"
              />
            </PanelCard>

            <PanelCard title="Tree - 트리 구조">
              <DataTree :value="treeData" />
            </PanelCard>
          </div>
        </template>

        <!-- Panel 탭 -->
        <template #panel>
          <div class="space-y-6">
            <PanelCard title="Card - 기본 카드">
              <template #header>
                <div class="w-full h-24 bg-gradient-to-r from-primary-500 to-primary-700 flex items-center justify-center">
                  <span class="text-white text-lg font-semibold">Card Header</span>
                </div>
              </template>
              <p>카드 본문 영역입니다. 다양한 콘텐츠를 배치할 수 있습니다.</p>
              <template #footer>
                <div class="flex gap-2 justify-end">
                  <ActionButton
                    label="취소"
                    severity="secondary"
                  />
                  <ActionButton label="확인" />
                </div>
              </template>
            </PanelCard>

            <PanelCard title="Accordion - 아코디언">
              <PanelAccordion
                v-model:value="accordionValue"
                :items="[
                  { value: '0', header: '섹션 1', content: '섹션 1의 내용입니다.' },
                  { value: '1', header: '섹션 2', content: '섹션 2의 내용입니다.' },
                  { value: '2', header: '섹션 3', content: '섹션 3의 내용입니다.' },
                ]"
              />
            </PanelCard>

            <PanelCard title="Toolbar - 툴바">
              <PanelToolbar>
                <template #start>
                  <ActionButton
                    label="새로 만들기"
                    icon="pi pi-plus"
                  />
                </template>
                <template #center>
                  <span class="font-semibold">문서 편집기</span>
                </template>
                <template #end>
                  <ActionButton
                    icon="pi pi-search"
                    text
                  />
                  <ActionButton
                    icon="pi pi-calendar"
                    text
                  />
                  <ActionButton
                    icon="pi pi-cog"
                    text
                  />
                </template>
              </PanelToolbar>
            </PanelCard>
          </div>
        </template>

        <!-- Overlay 탭 -->
        <template #overlay>
          <div class="space-y-6">
            <PanelCard title="Dialog & Drawer">
              <div class="flex gap-2 flex-wrap">
                <ActionButton
                  label="Dialog 열기"
                  icon="pi pi-external-link"
                  @click="dialogVisible = true"
                />
                <ActionButton
                  label="Drawer 열기"
                  icon="pi pi-bars"
                  @click="drawerVisible = true"
                />
              </div>

              <OverlayDialog
                v-model:visible="dialogVisible"
                header="샘플 Dialog"
                :style="{ width: '30vw' }"
              >
                <p class="mb-4">
                  Dialog 내용입니다. 모달 형태로 표시됩니다.
                </p>
                <template #footer>
                  <ActionButton
                    label="취소"
                    severity="secondary"
                    @click="dialogVisible = false"
                  />
                  <ActionButton
                    label="확인"
                    @click="dialogVisible = false"
                  />
                </template>
              </OverlayDialog>

              <OverlayDrawer
                v-model:visible="drawerVisible"
                header="샘플 Drawer"
                position="right"
              >
                <p>Drawer 내용입니다. 사이드에서 슬라이드됩니다.</p>
              </OverlayDrawer>
            </PanelCard>

            <PanelCard title="Confirm & Tooltip">
              <div class="space-y-4">
                <div>
                  <h3 class="font-semibold mb-2">
                    Confirm Dialog
                  </h3>
                  <OverlayConfirm />
                  <ConfirmButton
                    label="삭제 확인"
                    severity="danger"
                    message="정말 삭제하시겠습니까?"
                    @confirm="handleConfirm"
                  />
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Tooltip
                  </h3>
                  <OverlayTooltip
                    value="이것은 툴팁입니다"
                    position="top"
                  >
                    <ActionButton label="마우스를 올려보세요" />
                  </OverlayTooltip>
                </div>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- Menu 탭 -->
        <template #menu>
          <div class="space-y-6">
            <PanelCard title="MenuBar - 메뉴바">
              <MenuBar :model="menuItems" />
            </PanelCard>

            <PanelCard title="Breadcrumb - 경로 표시">
              <MenuBreadcrumb
                :model="breadcrumbItems"
                :home="{ icon: 'pi pi-home' }"
              />
            </PanelCard>

            <PanelCard title="Steps - 단계 표시">
              <MenuSteps
                :model="stepsItems"
                :active-step="activeStep"
              />
              <div class="flex gap-2 mt-4">
                <ActionButton
                  label="이전"
                  :disabled="activeStep === 0"
                  @click="activeStep--"
                />
                <ActionButton
                  label="다음"
                  :disabled="activeStep === stepsItems.length - 1"
                  @click="activeStep++"
                />
              </div>
            </PanelCard>

            <PanelCard title="ContextMenu - 컨텍스트 메뉴">
              <p class="mb-2">
                오른쪽 클릭으로 메뉴를 열 수 있습니다 (구현 필요)
              </p>
              <MenuContext :model="menuItems" />
            </PanelCard>
          </div>
        </template>

        <!-- Feedback 탭 -->
        <template #feedback>
          <div class="space-y-6">
            <PanelCard title="Badge & Tag">
              <div class="space-y-4">
                <div>
                  <h3 class="font-semibold mb-2">
                    Badge
                  </h3>
                  <div class="flex gap-2 items-center flex-wrap">
                    <FeedbackBadge
                      value="2"
                      severity="success"
                    />
                    <FeedbackBadge
                      value="8"
                      severity="info"
                    />
                    <FeedbackBadge
                      value="4"
                      severity="warn"
                    />
                    <FeedbackBadge
                      value="1"
                      severity="danger"
                    />
                  </div>
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Tag
                  </h3>
                  <div class="flex gap-2 flex-wrap">
                    <FeedbackTag value="Primary" />
                    <FeedbackTag
                      value="Success"
                      severity="success"
                      icon="pi pi-check"
                    />
                    <FeedbackTag
                      value="Info"
                      severity="info"
                      icon="pi pi-info-circle"
                    />
                    <FeedbackTag
                      value="Warn"
                      severity="warn"
                      icon="pi pi-exclamation-triangle"
                    />
                    <FeedbackTag
                      value="Danger"
                      severity="danger"
                      icon="pi pi-times"
                    />
                  </div>
                </div>

                <div>
                  <h3 class="font-semibold mb-2">
                    Status Badge (Composite)
                  </h3>
                  <div class="flex gap-2 flex-wrap">
                    <StatusBadge status="active" />
                    <StatusBadge status="inactive" />
                    <StatusBadge status="pending" />
                    <StatusBadge status="success" />
                    <StatusBadge status="error" />
                    <StatusBadge status="warning" />
                  </div>
                </div>
              </div>
            </PanelCard>

            <PanelCard title="Message - 메시지">
              <div class="space-y-2">
                <FeedbackMessage
                  severity="success"
                  closable
                >
                  성공 메시지입니다
                </FeedbackMessage>
                <FeedbackMessage
                  severity="info"
                  closable
                >
                  정보 메시지입니다
                </FeedbackMessage>
                <FeedbackMessage
                  severity="warn"
                  closable
                >
                  경고 메시지입니다
                </FeedbackMessage>
                <FeedbackMessage
                  severity="error"
                  closable
                >
                  오류 메시지입니다
                </FeedbackMessage>
              </div>
            </PanelCard>

            <PanelCard title="Progress - 진행률">
              <div class="space-y-4">
                <div>
                  <h3 class="font-semibold mb-2">
                    확정 모드
                  </h3>
                  <FeedbackProgress :value="60" />
                </div>
                <div>
                  <h3 class="font-semibold mb-2">
                    불확정 모드
                  </h3>
                  <FeedbackProgress mode="indeterminate" />
                </div>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- Composite 탭 -->
        <template #composite>
          <div class="space-y-6">
            <PanelCard title="FormField - 폼 필드 통합">
              <div class="space-y-4">
                <FormField
                  label="사용자 이름"
                  description="3-20자 영문, 숫자"
                  required
                  error="사용자 이름은 필수입니다"
                >
                  <FormInput
                    placeholder="사용자 이름 입력"
                    fluid
                  />
                </FormField>

                <FormField
                  label="이메일"
                  required
                >
                  <FormInput
                    type="email"
                    placeholder="email@example.com"
                    fluid
                  />
                </FormField>
              </div>
            </PanelCard>

            <PanelCard title="SearchBar - 검색 바">
              <SearchBar
                v-model="searchQuery"
                placeholder="검색어를 입력하세요"
                @search="handleSearch"
              />
            </PanelCard>

            <PanelCard title="DataTableToolbar - 테이블 툴바">
              <DataTableToolbar
                title="사용자 관리"
                show-refresh
                show-add
                show-delete
                @refresh="handleRefresh"
                @add="handleAdd"
              />
            </PanelCard>

            <PanelCard title="ConfirmButton - 확인 버튼">
              <div class="flex gap-2">
                <ConfirmButton
                  label="저장"
                  severity="success"
                  message="변경사항을 저장하시겠습니까?"
                  @confirm="() => toast.success('저장됨')"
                />
                <ConfirmButton
                  label="삭제"
                  severity="danger"
                  message="정말 삭제하시겠습니까?"
                  header="삭제 확인"
                  @confirm="() => toast.success('삭제됨')"
                />
              </div>
            </PanelCard>

            <PanelCard title="StatusBadge - 상태 배지">
              <div class="space-y-2">
                <p class="text-sm opacity-70">
                  상태별 아이콘과 색상이 자동 매핑됩니다
                </p>
                <div class="flex gap-2 flex-wrap">
                  <StatusBadge
                    status="active"
                    label="활성"
                  />
                  <StatusBadge
                    status="inactive"
                    label="비활성"
                  />
                  <StatusBadge
                    status="pending"
                    label="대기 중"
                  />
                  <StatusBadge
                    status="success"
                    label="완료"
                  />
                  <StatusBadge
                    status="error"
                    label="실패"
                  />
                  <StatusBadge
                    status="warning"
                    label="주의"
                  />
                </div>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- RealGrid 탭 -->
        <template #realgrid>
          <div class="space-y-6">
            <!-- ========================================== -->
            <!-- 그리드 1: 기본 기능 + 내보내기 -->
            <!-- ========================================== -->
            <PanelCard title="RealGrid 1 - 기본 기능 데모">
              <template #subtitle>
                컨텍스트 메뉴, 키보드 단축키, 선택 요약, 내보내기 기능
              </template>

              <!-- 기능 안내 -->
              <div class="mb-4 p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <p class="font-medium mb-2 flex items-center gap-2">
                      <i class="pi pi-bars" />
                      컨텍스트 메뉴 (우클릭)
                    </p>
                    <ul class="list-disc list-inside opacity-70 space-y-1">
                      <li>컬럼 고정/해제</li>
                      <li>컬럼 표시/숨김</li>
                      <li>행 높이 조절</li>
                      <li>Excel/CSV 내보내기</li>
                    </ul>
                  </div>
                  <div>
                    <p class="font-medium mb-2 flex items-center gap-2">
                      <i class="pi pi-keyboard" />
                      키보드 단축키
                    </p>
                    <ul class="list-disc list-inside opacity-70 space-y-1">
                      <li><kbd>Ctrl+C</kbd> 복사</li>
                      <li><kbd>Ctrl+V</kbd> 붙여넣기</li>
                      <li><kbd>Ctrl+Z</kbd> 실행 취소</li>
                      <li><kbd>Delete</kbd> 삭제</li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 컨트롤 버튼 -->
              <div class="flex flex-wrap gap-2 mb-4">
                <ActionButton
                  label="행 추가"
                  icon="pi pi-plus"
                  severity="success"
                  @click="addRealgridRow"
                />
                <ActionButton
                  label="행 삭제"
                  icon="pi pi-minus"
                  severity="danger"
                  @click="removeRealgridRow"
                />
                <div class="border-l mx-2" />
                <ActionButton
                  label="Excel"
                  icon="pi pi-file-excel"
                  severity="info"
                  outlined
                  @click="exportRealgridExcel"
                />
                <ActionButton
                  label="CSV"
                  icon="pi pi-file"
                  severity="info"
                  outlined
                  @click="exportRealgridCsv"
                />
                <ActionButton
                  label="JSON"
                  icon="pi pi-code"
                  severity="info"
                  outlined
                  @click="exportRealgridJson"
                />
              </div>

              <!-- RealGrid 컴포넌트 -->
              <RealGrid
                ref="realgridRef"
                :columns="realgridColumns"
                :data="realgridData"
                height="300px"
                :events="{
                  onReady: onRealgridReady,
                  onCellClicked: onRealgridCellClick,
                }"
              />

              <!-- 선택 영역 안내 -->
              <p class="text-xs opacity-50 mt-2">
                💡 셀을 드래그하여 선택하면 하단에 합계/평균/최대/최소가 표시됩니다.
              </p>
            </PanelCard>

            <!-- ========================================== -->
            <!-- 그리드 2: 페이지네이션 + 상태 저장 + 유효성 검사 -->
            <!-- ========================================== -->
            <PanelCard title="RealGrid 2 - 페이지네이션 + 고급 기능">
              <template #subtitle>
                페이지네이션, 상태 저장 (컬럼 너비/순서), 유효성 검사 데모
              </template>

              <!-- 상태 저장 컨트롤 -->
              <div class="mb-4 p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                <p class="font-medium mb-2 flex items-center gap-2">
                  <i class="pi pi-save" />
                  상태 저장 기능
                </p>
                <p class="text-sm opacity-70 mb-3">
                  컬럼 너비, 순서, 고정 상태가 localStorage에 저장됩니다. 컬럼을 드래그하여 순서를 바꾸거나 너비를 조절한 후 저장해보세요.
                </p>
                <div class="flex gap-2">
                  <ActionButton
                    label="상태 저장"
                    icon="pi pi-save"
                    severity="success"
                    size="small"
                    @click="saveGrid2State"
                  />
                  <ActionButton
                    label="상태 복원"
                    icon="pi pi-refresh"
                    severity="info"
                    size="small"
                    @click="loadGrid2State"
                  />
                  <ActionButton
                    label="상태 삭제"
                    icon="pi pi-trash"
                    severity="secondary"
                    size="small"
                    @click="clearGrid2State"
                  />
                </div>
              </div>

              <!-- 유효성 검사 컨트롤 -->
              <div class="mb-4 p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                <p class="font-medium mb-2 flex items-center gap-2">
                  <i class="pi pi-check-circle" />
                  유효성 검사
                </p>
                <p class="text-sm opacity-70 mb-3">
                  이름(필수), 이메일(필수 + 형식) 검사가 적용됩니다. 셀을 더블클릭하여 편집 후 검사해보세요.
                </p>
                <ActionButton
                  label="전체 검사 실행"
                  icon="pi pi-check"
                  severity="warn"
                  size="small"
                  @click="validateGrid2"
                />
              </div>

              <!-- RealGrid 컴포넌트 -->
              <RealGrid
                ref="realgrid2Ref"
                :columns="realgrid2Columns"
                :data="realgrid2Data"
                height="350px"
                enable-persistence
                storage-key="docs-realgrid2-state"
                :validations="realgrid2Validations"
                scroll-mode="pagination"
                :pagination-options="{
                  itemsPerPage: 10,
                  onPageChange: onPage2Change,
                }"
                :events="{
                  onReady: onRealgrid2Ready,
                }"
                @validation-error="onRealgrid2ValidationError"
              />

              <!-- 기능 태그 -->
              <div class="flex flex-wrap gap-2 mt-3">
                <FeedbackTag
                  value="페이지네이션"
                  severity="info"
                  icon="pi pi-list"
                />
                <FeedbackTag
                  value="100건 데이터"
                  severity="secondary"
                  icon="pi pi-database"
                />
                <FeedbackTag
                  value="상태 저장"
                  severity="success"
                  icon="pi pi-save"
                />
                <FeedbackTag
                  value="유효성 검사"
                  severity="warn"
                  icon="pi pi-check"
                />
              </div>
            </PanelCard>

            <!-- ========================================== -->
            <!-- 그리드 3: 무한 스크롤 -->
            <!-- ========================================== -->
            <PanelCard title="RealGrid 3 - 무한 스크롤">
              <template #subtitle>
                스크롤 시 자동으로 데이터를 추가 로딩하는 무한 스크롤 데모 (500건)
              </template>

              <!-- 무한 스크롤 안내 -->
              <div class="mb-4 p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                <p class="font-medium mb-2 flex items-center gap-2">
                  <i class="pi pi-arrow-down" />
                  무한 스크롤 사용법
                </p>
                <p class="text-sm opacity-70 mb-3">
                  그리드를 아래로 스크롤하면 자동으로 다음 데이터가 로딩됩니다.
                  네트워크 지연을 시뮬레이션하기 위해 500ms 딜레이가 있습니다.
                </p>
                <ActionButton
                  label="처음부터 다시 로드"
                  icon="pi pi-refresh"
                  severity="secondary"
                  size="small"
                  @click="resetGrid3InfiniteScroll"
                />
              </div>

              <!-- RealGrid 컴포넌트 -->
              <RealGrid
                ref="realgrid3Ref"
                :columns="realgrid3Columns"
                :data="realgrid3Data"
                height="400px"
                scroll-mode="infinite"
                :infinite-scroll-options="{
                  pageSize: 20,
                  threshold: 0.8,
                }"
                :load-fn="loadGrid3Data"
                :events="{
                  onReady: onRealgrid3Ready,
                }"
              />

              <!-- 기능 태그 -->
              <div class="flex flex-wrap gap-2 mt-3">
                <FeedbackTag
                  value="무한 스크롤"
                  severity="info"
                  icon="pi pi-arrow-down"
                />
                <FeedbackTag
                  value="500건 데이터"
                  severity="secondary"
                  icon="pi pi-database"
                />
                <FeedbackTag
                  value="자동 로딩"
                  severity="success"
                  icon="pi pi-sync"
                />
                <FeedbackTag
                  value="500ms 지연"
                  severity="warn"
                  icon="pi pi-clock"
                />
              </div>
            </PanelCard>

            <!-- ========================================== -->
            <!-- 테마 연동 정보 -->
            <!-- ========================================== -->
            <PanelCard title="테마 연동">
              <div class="space-y-3">
                <p class="text-sm opacity-70">
                  RealGrid는 프로젝트 테마 시스템과 자동 연동됩니다.
                </p>
                <div class="flex flex-wrap gap-2">
                  <FeedbackTag
                    value="HTML 클래스 기반"
                    severity="info"
                    icon="pi pi-code"
                  />
                  <FeedbackTag
                    value="다크/라이트 자동 전환"
                    severity="success"
                    icon="pi pi-sync"
                  />
                  <FeedbackTag
                    value="6개 테마 지원"
                    severity="secondary"
                    icon="pi pi-palette"
                  />
                </div>
                <p class="text-xs opacity-50 mt-2">
                  상단의 테마 설정에서 테마를 변경하면 그리드 스타일이 자동으로 업데이트됩니다.
                </p>
              </div>
            </PanelCard>

            <!-- ========================================== -->
            <!-- 컴포저블 기능 요약 -->
            <!-- ========================================== -->
            <PanelCard title="RealGrid 컴포저블 기능 요약">
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridContextMenu</p>
                  <p class="text-xs opacity-70">컨텍스트 메뉴 (고정, 컬럼, 행높이, 내보내기)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridKeyboard</p>
                  <p class="text-xs opacity-70">키보드 단축키 (복사, 붙여넣기, 실행취소)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridExport</p>
                  <p class="text-xs opacity-70">내보내기 (Excel, CSV, JSON, 클립보드)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridSelection</p>
                  <p class="text-xs opacity-70">선택 요약 (합계, 평균, 최대, 최소)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridValidation</p>
                  <p class="text-xs opacity-70">유효성 검사 (필수, 패턴, 범위, 커스텀)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridPersistence</p>
                  <p class="text-xs opacity-70">상태 저장 (컬럼, 필터, 정렬, 고정)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridPagination</p>
                  <p class="text-xs opacity-70">페이지네이션 (페이지 이동, 총 건수)</p>
                </div>
                <div class="p-3 bg-surface-100 dark:bg-surface-800 rounded-lg">
                  <p class="font-medium mb-2">useRealGridInfiniteScroll</p>
                  <p class="text-xs opacity-70">무한 스크롤 (자동 로딩, 임계값)</p>
                </div>
              </div>
            </PanelCard>
          </div>
        </template>

        <!-- DockView 탭 -->
        <template #dockview>
          <div class="space-y-6">
            <PanelCard title="DockView - 도킹 레이아웃 매니저">
              <template #subtitle>
                VS Code 스타일의 드래그 앤 드롭 패널 레이아웃
              </template>

              <!-- 컨트롤 버튼 -->
              <div class="flex gap-2 mb-4">
                <ActionButton
                  label="패널 추가"
                  icon="pi pi-plus"
                  severity="success"
                  @click="addDockviewPanel"
                />
                <ActionButton
                  label="초기화"
                  icon="pi pi-refresh"
                  severity="secondary"
                  @click="resetDockviewPanels"
                />
              </div>

              <!-- DockView 컨테이너 (제한된 높이) -->
              <div class="dockview-demo-container">
                <!-- 🚀 DockView 지연 로딩: 테마 로드 후 렌더링 -->
                <template v-if="dockviewLoading">
                  <div class="flex items-center justify-center h-full">
                    <Loading />
                    <span class="ml-2 text-sm opacity-70">DockView 로딩 중...</span>
                  </div>
                </template>
                <Suspense v-else-if="dockviewTheme">
                  <!-- eslint-disable-next-line vue/attribute-hyphenation -->
                  <DockviewVue
                    :theme="(dockviewTheme as any)"
                    @ready="onDockviewReady"
                  />
                  <template #fallback>
                    <div class="flex items-center justify-center h-full">
                      <Loading />
                    </div>
                  </template>
                </Suspense>
                <div
                  v-else
                  class="flex items-center justify-center h-full text-sm opacity-70"
                >
                  DockView 초기화 대기 중...
                </div>
              </div>

              <!-- 사용 안내 -->
              <div class="mt-4 p-3 bg-surface-100 dark:bg-surface-800 rounded-lg text-sm">
                <p class="font-medium mb-2">
                  사용 방법:
                </p>
                <ul class="list-disc list-inside space-y-1 opacity-70">
                  <li>탭을 드래그하여 패널 위치 변경</li>
                  <li>패널 경계를 드래그하여 크기 조절</li>
                  <li>탭을 다른 패널로 드롭하여 그룹화</li>
                </ul>
              </div>
            </PanelCard>

            <PanelCard title="테마 연동">
              <div class="space-y-3">
                <p class="text-sm opacity-70">
                  DockView는 프로젝트 테마 시스템과 자동 연동됩니다.
                </p>
                <div class="flex flex-wrap gap-2">
                  <FeedbackTag
                    value="CSS 변수 기반"
                    severity="info"
                    icon="pi pi-code"
                  />
                  <FeedbackTag
                    value="트랜지션 효과"
                    severity="success"
                    icon="pi pi-sparkles"
                  />
                  <FeedbackTag
                    value="반응형 레이아웃"
                    severity="secondary"
                    icon="pi pi-arrows-alt"
                  />
                </div>
              </div>
            </PanelCard>
          </div>
        </template>
      </PanelTabs>

      <!-- Common 컴포넌트 섹션 -->
      <section>
        <h2 class="text-2xl font-semibold mb-4">
          Common 컴포넌트 (3개)
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <PanelCard title="Loading">
            <div class="flex justify-center">
              <Loading />
            </div>
          </PanelCard>

          <PanelCard title="Empty">
            <Empty message="데이터가 없습니다" />
          </PanelCard>

          <PanelCard title="ErrorBoundary">
            <ErrorBoundary>
              <p>에러 발생 시 표시되는 컴포넌트</p>
            </ErrorBoundary>
          </PanelCard>
        </div>
      </section>

      <!-- 푸터 -->
      <footer class="text-center py-8 opacity-50 text-sm space-y-2">
        <p class="font-semibold">
          Enterman Component Library
        </p>
        <p>35 Components • Base (28) + Composite (7) + Common (3)</p>
        <p class="text-xs">
          3-tier Architecture • Category-based Prefix • PrimeVue 4.4.1
        </p>
      </footer>
    </div>
  </NuxtLayout>
</template>

<style scoped>
/* DockView 데모 컨테이너 - 제한된 공간에서 표시 */
.dockview-demo-container {
  height: 400px;
  border: 1px solid var(--p-surface-200);
  border-radius: var(--p-border-radius);
  overflow: hidden;
}

/* ClientOnly 래퍼 및 Dockview가 부모 높이를 상속받도록 설정 */
.dockview-demo-container > * {
  width: 100%;
  height: 100%;
}

/* Dockview 컨테이너 - 공식 테마는 HTML에서 상속됨 */
.dockview-demo-container :deep(.dv-dockview) {
  width: 100%;
  height: 100%;
}

.app-dark .dockview-demo-container {
  border-color: var(--p-surface-700);
}

/* 키보드 단축키 스타일 */
kbd {
  display: inline-block;
  padding: 0.125rem 0.375rem;
  font-size: 0.75rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  background: var(--p-surface-200);
  border: 1px solid var(--p-surface-300);
  border-radius: 4px;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
}

:root.dark kbd,
.app-dark kbd {
  background: var(--p-surface-700);
  border-color: var(--p-surface-600);
}
</style>
