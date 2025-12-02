<script setup lang="ts">
import type { GridView, LocalDataProvider } from 'realgrid'
import type { RealGridColumn, RealGridInstance, RealGridCellClickData } from '~/types/realgrid'
import { useThemeStore } from '~/stores/theme'

// 페이지 메타데이터
definePageMeta({
  title: 'RealGrid Demo',
})

// 테마 스토어 사용
const themeStore = useThemeStore()

// 그리드 컬럼 정의
const columns: RealGridColumn[] = [
  {
    name: 'id',
    fieldName: 'id',
    type: 'text',
    width: 80,
    header: { text: 'ID' },
  },
  {
    name: 'name',
    fieldName: 'name',
    type: 'text',
    width: 150,
    header: { text: '이름' },
  },
  {
    name: 'email',
    fieldName: 'email',
    type: 'text',
    width: 200,
    header: { text: '이메일' },
  },
  {
    name: 'department',
    fieldName: 'department',
    type: 'text',
    width: 120,
    header: { text: '부서' },
  },
  {
    name: 'position',
    fieldName: 'position',
    type: 'text',
    width: 100,
    header: { text: '직급' },
  },
  {
    name: 'salary',
    fieldName: 'salary',
    type: 'number',
    width: 120,
    header: { text: '급여' },
  },
  {
    name: 'joinDate',
    fieldName: 'joinDate',
    type: 'datetime',
    width: 120,
    header: { text: '입사일' },
  },
]

// 샘플 데이터
const gridData = ref([
  {
    id: '1',
    name: '김철수',
    email: 'kim.cs@example.com',
    department: '개발팀',
    position: '과장',
    salary: 55000000,
    joinDate: '2020-03-15',
  },
  {
    id: '2',
    name: '이영희',
    email: 'lee.yh@example.com',
    department: '기획팀',
    position: '차장',
    salary: 65000000,
    joinDate: '2018-07-01',
  },
  {
    id: '3',
    name: '박민수',
    email: 'park.ms@example.com',
    department: '개발팀',
    position: '대리',
    salary: 45000000,
    joinDate: '2021-01-10',
  },
  {
    id: '4',
    name: '최지현',
    email: 'choi.jh@example.com',
    department: '인사팀',
    position: '사원',
    salary: 38000000,
    joinDate: '2022-06-20',
  },
  {
    id: '5',
    name: '정수연',
    email: 'jung.sy@example.com',
    department: '마케팅팀',
    position: '부장',
    salary: 75000000,
    joinDate: '2017-03-25',
  },
])

// 그리드 인스턴스 참조
const gridWrapperRef = ref<{ getGridInstance: () => RealGridInstance | null } | null>(null)

// 그리드 준비 완료 핸들러
const onGridReady = (grid: GridView, provider: LocalDataProvider) => {
  console.log('Grid is ready!', grid, provider)
}

// 셀 클릭 핸들러
const onCellClicked = (_grid: GridView, clickData: RealGridCellClickData) => {
  console.log('Cell clicked:', clickData)
}

// 행 추가
const addRow = () => {
  const newId = (gridData.value.length + 1).toString()
  const today = new Date().toISOString().split('T')[0] as string
  gridData.value.push({
    id: newId,
    name: `신규 직원 ${newId}`,
    email: `new${newId}@example.com`,
    department: '미정',
    position: '사원',
    salary: 35000000,
    joinDate: today,
  })
}

// 마지막 행 삭제
const removeLastRow = () => {
  if (gridData.value.length > 0) {
    gridData.value.pop()
  }
}

// 그리드 새로고침
const refreshGrid = () => {
  const instance = gridWrapperRef.value?.getGridInstance()
  if (instance) {
    instance.gridView.refresh()
  }
}
</script>

<template>
  <div class="realgrid-demo-page p-6">
    <!-- 페이지 헤더 -->
    <div class="mb-6">
      <h1 class="text-3xl font-bold mb-2">
        RealGrid 데모
      </h1>
      <p class="text-gray-600">
        확장 가능한 테마 시스템을 적용한 RealGrid 래핑 컴포넌트 데모
      </p>
    </div>

    <!-- 테마 컨트롤 -->
    <div class="mb-6 p-4 bg-white rounded-lg shadow">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h2 class="text-xl font-semibold mb-1">
            현재 테마: {{ themeStore.themeName }}
          </h2>
          <p class="text-sm text-gray-600">
            {{ themeStore.currentTheme.description }}
          </p>
        </div>
        <div class="flex gap-3">
          <button
            class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            :disabled="themeStore.themeName === 'linear-dark'"
            @click="themeStore.setTheme('linear-dark')"
          >
            Linear Dark
          </button>
          <button
            class="px-4 py-2 bg-orange-600 text-white rounded hover:bg-orange-700 transition-colors"
            :disabled="themeStore.themeName === 'koscom-light'"
            @click="themeStore.setTheme('koscom-light')"
          >
            Koscom Light
          </button>
        </div>
      </div>

      <!-- 다크/라이트 모드 전환 -->
      <div class="flex items-center gap-3">
        <span class="text-sm font-medium">모드:</span>
        <button
          class="px-4 py-2 bg-gray-700 text-white rounded hover:bg-gray-800 transition-colors"
          @click="themeStore.toggleDarkMode"
        >
          {{ themeStore.isDark ? '🌙 Dark' : '☀️ Light' }}
        </button>
        <span class="text-sm text-gray-600">
          ({{ themeStore.isDark ? '다크' : '라이트' }} 모드 활성)
        </span>
      </div>
    </div>

    <!-- 그리드 컨트롤 -->
    <div class="mb-6 p-4 bg-white rounded-lg shadow">
      <h2 class="text-xl font-semibold mb-4">
        그리드 조작
      </h2>
      <div class="flex gap-3">
        <button
          class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition-colors"
          @click="addRow"
        >
          행 추가
        </button>
        <button
          class="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors"
          @click="removeLastRow"
        >
          마지막 행 삭제
        </button>
        <button
          class="px-4 py-2 bg-purple-600 text-white rounded hover:bg-purple-700 transition-colors"
          @click="refreshGrid"
        >
          새로고침
        </button>
      </div>
    </div>

    <!-- RealGrid 컴포넌트 -->
    <div class="p-4 bg-white rounded-lg shadow">
      <h2 class="text-xl font-semibold mb-4">
        직원 목록 그리드
      </h2>
      <RealGrid
        ref="gridWrapperRef"
        :columns="columns"
        :data="gridData"
        :height="'500px'"
        :events="{
          onReady: onGridReady,
          onCellClicked: onCellClicked,
        }"
      />
    </div>

    <!-- 테마 통합 안내 -->
    <div class="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
      <h3 class="text-lg font-semibold mb-2 text-blue-900">
        💡 프로젝트 테마 시스템 통합
      </h3>
      <div class="text-sm text-blue-800 space-y-2">
        <p>RealGrid가 프로젝트의 전역 테마 시스템과 자동으로 연동됩니다:</p>
        <ul class="list-disc list-inside space-y-1 ml-2">
          <li>
            <strong>테마 스토어 구독</strong>: HTML 클래스 (<code class="bg-white px-2 py-0.5 rounded">.theme-linear-dark</code>, <code class="bg-white px-2 py-0.5 rounded">.theme-koscom-light</code>)로 자동 적용
          </li>
          <li>
            <strong>다크/라이트 모드</strong>: <code class="bg-white px-2 py-0.5 rounded">.app-dark</code> 클래스로 자동 전환
          </li>
          <li>
            <strong>4가지 조합</strong>: Linear Dark/Light + Koscom Light/Dark 모두 지원
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.realgrid-demo-page {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
