<script setup lang="ts">
/**
 * ThemeConfigurator - PrimeVue 스타일 테마 선택기
 *
 * PrimeVue 공식 사이트의 우측 상단 테마 설정 패널을 참고하여 구현.
 * - 모드 선택 (Light/Dark/System)
 * - 테마 선택 (프리셋)
 * - 팔레트 선택 (색상 그리드)
 * - 접근성 옵션
 */
import Drawer from 'primevue/drawer'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import Divider from 'primevue/divider'
import ToggleSwitch from 'primevue/toggleswitch'
import Tooltip from 'primevue/tooltip'
import { useThemeStore } from '~/stores/theme'
import { THEMES, COLOR_PALETTES, PRIMARY_COLOR_NAMES, SURFACE_PALETTES, SURFACE_COLOR_NAMES, type ThemeName, type ThemeMode, type PrimaryColorName, type SurfaceColorName } from '~/themes'

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

const themeStore = useThemeStore()
const vTooltip = Tooltip

// ============================================================================
// State
// ============================================================================

/** 모드 선택 옵션 */
const modeOptions = [
  { value: 'light' as ThemeMode, icon: 'pi pi-sun', label: '라이트' },
  { value: 'dark' as ThemeMode, icon: 'pi pi-moon', label: '다크' },
  { value: 'system' as ThemeMode, icon: 'pi pi-desktop', label: '시스템' },
]

/** 전체 테마 목록 */
const allThemes = computed(() =>
  Object.entries(THEMES)
    .map(([key, config]) => ({
      value: key as ThemeName,
      ...config,
    })),
)

// ============================================================================
// Computed
// ============================================================================

/** Drawer visible 바인딩 */
const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

/** 현재 선택된 모드 */
const selectedMode = computed({
  get: () => themeStore.themeMode,
  set: (value: ThemeMode) => themeStore.setMode(value),
})

/** 현재 선택된 테마 */
const selectedTheme = computed(() => themeStore.themeName)

/** 현재 선택된 Primary 색상 */
const selectedColor = computed(() => themeStore.primaryColor)

/** 색상 팔레트 목록 */
const colorPalettes = computed(() =>
  PRIMARY_COLOR_NAMES.map(name => ({
    value: name,
    ...COLOR_PALETTES[name],
  })),
)

/** 현재 선택된 Surface 색상 */
const selectedSurface = computed(() => themeStore.surfaceColor)

/** Surface 색상 팔레트 목록 */
const surfacePalettes = computed(() =>
  SURFACE_COLOR_NAMES.map(name => ({
    value: name,
    ...SURFACE_PALETTES[name],
  })),
)

/** 고대비 모드 */
const highContrast = computed({
  get: () => themeStore.accessibility.highContrast,
  set: (value: boolean) => themeStore.setAccessibility({ highContrast: value }),
})

/** 애니메이션 줄이기 */
const reducedMotion = computed({
  get: () => themeStore.accessibility.reducedMotion,
  set: (value: boolean) => themeStore.setAccessibility({ reducedMotion: value }),
})

// ============================================================================
// Methods
// ============================================================================

/**
 * 테마 선택
 */
function selectTheme(themeName: ThemeName, event?: MouseEvent) {
  themeStore.setTheme(themeName, event)
}

/**
 * 테마 프리뷰 시작 (hover)
 */
function startPreview(themeName: ThemeName) {
  themeStore.startPreview(themeName)
}

/**
 * 테마 프리뷰 취소 (hover 종료)
 */
function cancelPreview() {
  themeStore.cancelPreview()
}

/**
 * 테마가 현재 선택되었는지 확인
 */
function isSelected(themeName: ThemeName): boolean {
  return selectedTheme.value === themeName
}

/**
 * 색상이 현재 선택되었는지 확인
 */
function isColorSelected(colorName: PrimaryColorName): boolean {
  return selectedColor.value === colorName
}

/**
 * Primary 색상 선택
 */
function selectColor(colorName: PrimaryColorName) {
  themeStore.setPrimaryColor(colorName)
}

/**
 * Surface 색상이 현재 선택되었는지 확인
 */
function isSurfaceSelected(colorName: SurfaceColorName): boolean {
  return selectedSurface.value === colorName
}

/**
 * Surface 색상 선택
 */
function selectSurface(colorName: SurfaceColorName) {
  themeStore.setSurfaceColor(colorName)
}

/**
 * 설정 초기화
 */
function resetSettings() {
  themeStore.resetAllSettings()
}
</script>

<template>
  <Drawer
    v-model:visible="drawerVisible"
    position="right"
    :header="'테마 설정'"
    class="theme-configurator-drawer !w-80 lg:!w-96"
  >
    <div class="flex flex-col gap-6">
      <!-- ═══════════════════════════════════════════════════════════════════
           모드 선택 (Light / Dark / System)
           ═══════════════════════════════════════════════════════════════════ -->
      <section>
        <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
          <i class="pi pi-sun mr-2" />모드
        </h3>
        <SelectButton
          v-model="selectedMode"
          :options="modeOptions"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          class="w-full"
        >
          <template #option="{ option }">
            <div class="flex items-center gap-2 px-1">
              <i :class="option.icon" />
              <span class="text-sm">{{ option.label }}</span>
            </div>
          </template>
        </SelectButton>
      </section>

      <Divider />

      <!-- ═══════════════════════════════════════════════════════════════════
           Primary 색상 선택 (PrimeVue 스타일 색상 그리드)
           ═══════════════════════════════════════════════════════════════════ -->
      <section>
        <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
          <i class="pi pi-palette mr-2" />Primary 색상
        </h3>
        <div class="grid grid-cols-6 gap-2">
          <button
            v-for="color in colorPalettes"
            :key="color.value"
            v-tooltip.bottom="color.name"
            class="color-palette-btn"
            :class="{ 'ring-2 ring-offset-2 ring-offset-surface-0 dark:ring-offset-surface-900': isColorSelected(color.value) }"
            :style="{ '--color-main': color.color }"
            @click="selectColor(color.value)"
          >
            <div
              class="w-6 h-6 rounded-full shadow-sm"
              :style="{ backgroundColor: color.color }"
            />
            <!-- 선택 체크마크 -->
            <div
              v-if="isColorSelected(color.value)"
              class="absolute inset-0 flex items-center justify-center"
            >
              <i class="pi pi-check text-white text-xs drop-shadow-md" />
            </div>
          </button>
        </div>
      </section>

      <Divider />

      <!-- ═══════════════════════════════════════════════════════════════════
           Surface 색상 선택 (배경/테두리용 중성 색상)
           ═══════════════════════════════════════════════════════════════════ -->
      <section>
        <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
          <i class="pi pi-th-large mr-2" />Surface 색상
        </h3>
        <div class="grid grid-cols-5 gap-2">
          <button
            v-for="surface in surfacePalettes"
            :key="surface.value"
            v-tooltip.bottom="`${surface.name} - ${surface.description}`"
            class="surface-palette-btn"
            :class="{ 'ring-2 ring-primary ring-offset-2 ring-offset-surface-0 dark:ring-offset-surface-900': isSurfaceSelected(surface.value) }"
            @click="selectSurface(surface.value)"
          >
            <!-- 그라데이션 프리뷰 (밝음 → 어두움) -->
            <div class="flex flex-col h-8 w-full rounded-md overflow-hidden shadow-sm">
              <div
                class="flex-1"
                :style="{ backgroundColor: surface.color }"
              />
            </div>
            <span class="text-[10px] text-surface-500 dark:text-surface-400 mt-1">
              {{ surface.name }}
            </span>
            <!-- 선택 체크마크 -->
            <div
              v-if="isSurfaceSelected(surface.value)"
              class="absolute -top-1 -right-1 w-4 h-4 bg-primary rounded-full flex items-center justify-center shadow-md"
            >
              <i class="pi pi-check text-white text-[8px]" />
            </div>
          </button>
        </div>
      </section>

      <Divider />

      <!-- ═══════════════════════════════════════════════════════════════════
           테마 팔레트 (통합)
           ═══════════════════════════════════════════════════════════════════ -->
      <section>
        <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
          <i class="pi pi-images mr-2" />테마
        </h3>
        <div class="grid grid-cols-3 gap-3">
          <button
            v-for="theme in allThemes"
            :key="theme.value"
            v-tooltip.bottom="theme.name"
            class="theme-palette-btn group"
            :class="{ 'ring-2 ring-primary ring-offset-2 ring-offset-surface-0 dark:ring-offset-surface-900': isSelected(theme.value) }"
            @click="selectTheme(theme.value, $event)"
            @mouseenter="startPreview(theme.value)"
            @mouseleave="cancelPreview"
          >
            <!-- 색상 팔레트 미리보기 -->
            <div
              class="flex h-10 w-full rounded-lg overflow-hidden shadow-sm"
              :class="{ 'border border-surface-200': !theme.prefersDark }"
            >
              <div
                class="flex-1"
                :style="{ backgroundColor: theme.accentColors[1] }"
              />
              <div
                class="flex-1"
                :style="{ backgroundColor: theme.accentColors[0] }"
              />
              <div
                class="flex-1"
                :style="{ backgroundColor: theme.accentColors[2] }"
              />
            </div>
            <!-- 테마 이름 + 다크/라이트 아이콘 -->
            <div class="flex items-center gap-1 mt-2">
              <i
                :class="theme.prefersDark ? 'pi pi-moon' : 'pi pi-sun'"
                class="text-[10px] text-surface-400"
              />
              <span class="text-xs text-surface-600 dark:text-surface-300 group-hover:text-primary transition-colors truncate">
                {{ theme.name.replace(' Dark', '').replace(' Light', '') }}
              </span>
            </div>
            <!-- 선택 체크마크 -->
            <div
              v-if="isSelected(theme.value)"
              class="absolute -top-1 -right-1 w-5 h-5 bg-primary rounded-full flex items-center justify-center shadow-md"
            >
              <i class="pi pi-check text-white text-xs" />
            </div>
          </button>
        </div>
      </section>

      <Divider />

      <!-- ═══════════════════════════════════════════════════════════════════
           접근성 옵션
           ═══════════════════════════════════════════════════════════════════ -->
      <section>
        <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
          <i class="pi pi-eye mr-2" />접근성
        </h3>
        <div class="flex flex-col gap-4">
          <!-- 고대비 모드 -->
          <div class="flex items-center justify-between">
            <label
              for="high-contrast"
              class="text-sm text-surface-700 dark:text-surface-200"
            >
              고대비 모드
            </label>
            <ToggleSwitch
              v-model="highContrast"
              input-id="high-contrast"
            />
          </div>
          <!-- 애니메이션 줄이기 -->
          <div class="flex items-center justify-between">
            <label
              for="reduced-motion"
              class="text-sm text-surface-700 dark:text-surface-200"
            >
              애니메이션 줄이기
            </label>
            <ToggleSwitch
              v-model="reducedMotion"
              input-id="reduced-motion"
            />
          </div>
        </div>
      </section>

      <Divider />

      <!-- ═══════════════════════════════════════════════════════════════════
           하단 액션 버튼
           ═══════════════════════════════════════════════════════════════════ -->
      <section class="flex gap-2">
        <Button
          label="설정 초기화"
          icon="pi pi-refresh"
          severity="secondary"
          variant="outlined"
          size="small"
          class="flex-1"
          @click="resetSettings"
        />
        <Button
          label="내보내기"
          icon="pi pi-download"
          severity="secondary"
          variant="outlined"
          size="small"
          class="flex-1"
          @click="themeStore.downloadSettings()"
        />
      </section>
    </div>

    <!-- 프리뷰 로딩 인디케이터 -->
    <div
      v-if="themeStore.isPreviewLoading"
      class="absolute inset-0 bg-surface-0/50 dark:bg-surface-900/50 flex items-center justify-center"
    >
      <i class="pi pi-spin pi-spinner text-2xl text-primary" />
    </div>
  </Drawer>
</template>

<style scoped>
/* Color palette button */
.color-palette-btn {
  --ring-color: var(--color-main, var(--p-primary-color));
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border-radius: 0.5rem;
  background-color: var(--p-surface-50);
  border: 1px solid var(--p-surface-200);
  cursor: pointer;
  transition: all 0.15s ease;
}

.color-palette-btn:hover {
  background-color: var(--p-surface-100);
  transform: scale(1.1);
}

.color-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--color-main, var(--p-primary-color));
}

/* Selected state - use CSS variable for dynamic ring color */
.color-palette-btn.ring-2 {
  --tw-ring-color: var(--color-main) !important;
}

:global(.app-dark) .color-palette-btn {
  background-color: var(--p-surface-800);
  border-color: var(--p-surface-700);
}

:global(.app-dark) .color-palette-btn:hover {
  background-color: var(--p-surface-700);
}

/* Surface palette button */
.surface-palette-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.375rem;
  border-radius: 0.5rem;
  background-color: var(--p-surface-50);
  border: 1px solid var(--p-surface-200);
  cursor: pointer;
  transition: all 0.15s ease;
}

.surface-palette-btn:hover {
  background-color: var(--p-surface-100);
  transform: scale(1.05);
}

.surface-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--p-primary-color);
}

:global(.app-dark) .surface-palette-btn {
  background-color: var(--p-surface-800);
  border-color: var(--p-surface-700);
}

:global(.app-dark) .surface-palette-btn:hover {
  background-color: var(--p-surface-700);
}

/* Theme palette button */
.theme-palette-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem;
  border-radius: 0.75rem;
  background-color: var(--p-surface-50);
  border: 1px solid var(--p-surface-200);
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-palette-btn:hover {
  background-color: var(--p-surface-100);
  transform: scale(1.05);
}

.theme-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--p-primary-color);
}

/* 다크모드 */
:global(.app-dark) .theme-palette-btn {
  background-color: var(--p-surface-800);
  border-color: var(--p-surface-700);
}

:global(.app-dark) .theme-palette-btn:hover {
  background-color: var(--p-surface-700);
}

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
