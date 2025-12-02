<script setup lang="ts">
import { ref } from 'vue'
import { useThemeStore } from '~/stores/theme'
import { useAppToast } from '~/composables/useAppToast'
import type { ThemeName } from '~/themes'

const themeStore = useThemeStore()
const toast = useAppToast()

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
          33개의 PrimeVue 기반 컴포넌트
        </p>
        <div class="flex justify-center gap-2">
          <FeedbackBadge
            value="Base: 28"
            severity="info"
          />
          <FeedbackBadge
            value="Composite: 5"
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
        ]"
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
        <p>33 Components • Base (28) + Composite (5) + Common (3)</p>
        <p class="text-xs">
          3-tier Architecture • Category-based Prefix • PrimeVue 4.4.1
        </p>
      </footer>
    </div>
  </NuxtLayout>
</template>
