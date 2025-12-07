<script setup lang="ts">
/**
 * ThemeConfigurator - PrimeVue 스타일 테마 설정 패널
 *
 * PrimeVue 공식 사이트의 우측 상단 테마 설정 패널을 참고하여 구현.
 * - 모드 선택 (Light/Dark/System)
 * - Primary 색상 선택
 * - Surface 색상 선택
 * - 테마 프리셋 선택
 * - 접근성 옵션
 *
 * @see https://primevue.org (테마 설정 패널 참고)
 */
import Drawer from 'primevue/drawer'
import Divider from 'primevue/divider'

// Section Components
import ThemeModeSelector from './sections/ThemeModeSelector.vue'
import PrimaryColorSelector from './sections/PrimaryColorSelector.vue'
import SurfaceColorSelector from './sections/SurfaceColorSelector.vue'
import ThemePresetSelector from './sections/ThemePresetSelector.vue'
import AccessibilityOptions from './sections/AccessibilityOptions.vue'
import ThemeActions from './sections/ThemeActions.vue'

// Composables
import { useThemeConfigurator } from '~/composables/theme/useThemeConfigurator'

// ============================================================================
// Props & Emits
// ============================================================================

const props = defineProps<{
  /** Drawer 표시 여부 (v-model:visible) */
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

// ============================================================================
// Composables
// ============================================================================

const {
  isPreviewLoading,
  resetSettings,
  downloadSettings,
} = useThemeConfigurator()

// ============================================================================
// Computed
// ============================================================================

/** Drawer visible 바인딩 */
const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})
</script>

<template>
  <Drawer
    v-model:visible="drawerVisible"
    position="right"
    header="테마 설정"
    class="theme-configurator-drawer !w-80 lg:!w-96"
  >
    <div class="flex flex-col gap-6">
      <!-- 모드 선택 (Light / Dark / System) -->
      <ThemeModeSelector />

      <Divider />

      <!-- Primary 색상 선택 -->
      <PrimaryColorSelector />

      <Divider />

      <!-- Surface 색상 선택 -->
      <SurfaceColorSelector />

      <Divider />

      <!-- 테마 프리셋 선택 -->
      <ThemePresetSelector />

      <Divider />

      <!-- 접근성 옵션 -->
      <AccessibilityOptions />

      <Divider />

      <!-- 하단 액션 버튼 -->
      <ThemeActions
        @reset="resetSettings"
        @export="downloadSettings()"
      />
    </div>

    <!-- 프리뷰 로딩 인디케이터 -->
    <div
      v-if="isPreviewLoading"
      class="absolute inset-0 bg-surface-0/50 dark:bg-surface-900/50 flex items-center justify-center"
    >
      <i class="pi pi-spin pi-spinner text-2xl text-primary" />
    </div>
  </Drawer>
</template>

<style scoped>
/* Drawer 커스텀 스타일 */
:deep(.p-drawer-content) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.p-drawer-header) {
  border-bottom: 1px solid var(--p-surface-200);
}

:global(.app-dark) :deep(.p-drawer-header) {
  border-bottom-color: var(--p-surface-700);
}
</style>
