<script setup lang="ts">
import RealGrid from 'realgrid'
import { useThemeStore } from '~/stores/theme'
import type {
  RealGridColumn,
  RealGridOptions,
  RealGridEvents,
  RealGridInstance,
  RealGridCellClickData,
} from '~/types/realgrid'

// Props 정의
interface Props {
  /** 컬럼 정의 */
  columns: RealGridColumn[]
  /** 그리드 데이터 */
  data?: Record<string, unknown>[]
  /** 그리드 옵션 */
  options?: RealGridOptions
  /** 그리드 이벤트 핸들러 */
  events?: RealGridEvents
  /** 그리드 높이 */
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  options: () => ({}),
  events: () => ({}),
  height: '400px',
})

// Emits 정의
const emit = defineEmits<{
  ready: [instance: RealGridInstance]
}>()

// Refs
const gridContainer = ref<HTMLDivElement | null>(null)
const gridInstance = ref<RealGridInstance | null>(null)

// 테마 스토어 구독
const themeStore = useThemeStore()

// 그리드 초기화
const initGrid = () => {
  if (!gridContainer.value) {
    console.error('Grid container not found')
    return
  }

  try {
    // RealGrid 인스턴스 생성
    const { gridView, dataProvider } = RealGrid.createGrid(gridContainer.value, {
      ...props.options,
    })

    // 컬럼 설정
    gridView.setColumns(props.columns)

    // 데이터 로드
    if (props.data && props.data.length > 0) {
      dataProvider.setRows(props.data)
    }

    // 이벤트 핸들러 등록
    if (props.events.onReady) {
      props.events.onReady(gridView, dataProvider)
    }
    if (props.events.onCellClicked) {
      gridView.onCellClicked = (grid, clickData) => {
        props.events.onCellClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onCellDblClicked) {
      gridView.onCellDblClicked = (grid, clickData) => {
        props.events.onCellDblClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onDataCellClicked) {
      gridView.onCellClicked = (grid, clickData) => {
        props.events.onDataCellClicked?.(gridView, clickData as RealGridCellClickData)
      }
    }
    if (props.events.onCurrentRowChanged) {
      gridView.onCurrentRowChanged = (grid, oldRow, newRow) => {
        props.events.onCurrentRowChanged?.(gridView, oldRow, newRow)
      }
    }

    // 인스턴스 저장
    const instance: RealGridInstance = {
      gridView,
      dataProvider,
      destroy: () => {
        gridView.destroy()
        dataProvider.destroy()
      },
    }
    gridInstance.value = instance

    // ready 이벤트 발생
    emit('ready', instance)
  }
  catch (error) {
    console.error('Failed to initialize RealGrid:', error)
  }
}

// 그리드 데이터 업데이트
const updateData = (newData: Record<string, unknown>[]) => {
  if (gridInstance.value) {
    gridInstance.value.dataProvider.setRows(newData)
  }
}

// 그리드 인스턴스 노출
const getGridInstance = () => gridInstance.value

// Lifecycle hooks
onMounted(() => {
  initGrid()
})

onBeforeUnmount(() => {
  if (gridInstance.value) {
    gridInstance.value.destroy()
    gridInstance.value = null
  }
})

// Watch data changes
watch(
  () => props.data,
  (newData) => {
    if (newData && newData.length > 0) {
      updateData(newData)
    }
  },
  { deep: true },
)

// Watch theme changes
watch(
  () => [themeStore.themeName, themeStore.isDark],
  () => {
    if (gridInstance.value) {
      // 테마 변경 시 그리드 새로고침
      gridInstance.value.gridView.refresh()
    }
  },
  { deep: true },
)

// 컴포넌트 메서드 노출
defineExpose({
  getGridInstance,
  updateData,
})
</script>

<template>
  <div class="realgrid-wrapper">
    <div
      ref="gridContainer"
      class="realgrid-container"
      :style="{ height: props.height }"
    />
  </div>
</template>

<style scoped>
.realgrid-wrapper {
  width: 100%;
  height: 100%;
}

.realgrid-container {
  width: 100%;
}
</style>
