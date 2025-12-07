<script setup lang="ts">
/**
 * ThemeSelector - 테마 선택 및 설정 컴포넌트
 *
 * 기능:
 * - 테마 목록 표시 (다크/라이트 그룹핑)
 * - 호버 프리뷰
 * - 테마 모드 전환 (시스템/다크/라이트)
 * - 접근성 옵션
 * - 스케줄 설정
 * - 내보내기/가져오기
 */

import { useThemeStore } from '~/stores/theme'
import type { ThemeName, ThemeMode } from '~/themes'

// ============================================================================
// Props & Emits
// ============================================================================

interface Props {
  /** 컴팩트 모드 (드롭다운용) */
  compact?: boolean
  /** 프리뷰 활성화 여부 */
  enablePreview?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
  enablePreview: true,
})

const emit = defineEmits<{
  /** 테마 변경 시 */
  change: [themeName: ThemeName]
}>()

// ============================================================================
// Store & State
// ============================================================================

const themeStore = useThemeStore()

const showScheduleDialog = ref(false)
const showAccessibilityDialog = ref(false)

// 스케줄 폼 상태
const scheduleForm = ref({
  enabled: false,
  lightTheme: 'notion-light' as ThemeName,
  darkTheme: 'linear-dark' as ThemeName,
  sunriseTime: '07:00',
  sunsetTime: '19:00',
})

// ============================================================================
// Computed
// ============================================================================

const darkThemes = computed(() => themeStore.darkThemes)
const lightThemes = computed(() => themeStore.lightThemes)
const currentThemeName = computed(() => themeStore.themeName)
const currentMode = computed(() => themeStore.themeMode)
const isPreviewLoading = computed(() => themeStore.isPreviewLoading)
const previewError = computed(() => themeStore.previewError)
const previewThemeName = computed(() => themeStore.previewThemeName)

const modeOptions = [
  { value: 'system', label: '시스템', icon: 'pi pi-desktop' },
  { value: 'dark', label: '다크', icon: 'pi pi-moon' },
  { value: 'light', label: '라이트', icon: 'pi pi-sun' },
]

// ============================================================================
// Methods
// ============================================================================

function selectTheme(themeName: ThemeName, event?: MouseEvent) {
  themeStore.setTheme(themeName, event)
  emit('change', themeName)
}

function setMode(mode: ThemeMode) {
  themeStore.setMode(mode)
}

function handleThemeHover(themeName: ThemeName) {
  if (props.enablePreview) {
    themeStore.startPreview(themeName)
  }
}

function handleThemeLeave() {
  if (props.enablePreview) {
    themeStore.cancelPreview()
  }
}

// 스케줄 다이얼로그
function openScheduleDialog() {
  scheduleForm.value = { ...themeStore.schedule }
  showScheduleDialog.value = true
}

function saveSchedule() {
  themeStore.setSchedule(scheduleForm.value)
  showScheduleDialog.value = false
}

// 접근성 다이얼로그
function openAccessibilityDialog() {
  showAccessibilityDialog.value = true
}

// 내보내기/가져오기
function exportSettings() {
  themeStore.downloadSettings()
}

async function importSettings() {
  const result = await themeStore.importFromFile()
  if (!result.success) {
    // TODO: 토스트 알림 표시
    console.error(result.message)
  }
}

function resetSettings() {
  if (confirm('모든 테마 설정을 초기화하시겠습니까?')) {
    themeStore.resetAllSettings()
  }
}
</script>

<template>
  <div :class="['theme-selector', { 'theme-selector--compact': compact }]">
    <!-- 테마 모드 선택 -->
    <div class="theme-selector__mode">
      <span class="theme-selector__label">테마 모드</span>
      <SelectButton
        :model-value="currentMode"
        :options="modeOptions"
        option-label="label"
        option-value="value"
        :allow-empty="false"
        @update:model-value="setMode"
      >
        <template #option="{ option }">
          <i :class="option.icon" />
          <span
            v-if="!compact"
            class="ml-2"
          >{{ option.label }}</span>
        </template>
      </SelectButton>
    </div>

    <!-- 프리뷰 에러 메시지 -->
    <div
      v-if="previewError"
      class="theme-selector__error"
    >
      <i class="pi pi-exclamation-triangle" />
      <span>{{ previewError }}</span>
      <button
        class="theme-selector__error-close"
        @click="themeStore.clearPreviewError()"
      >
        <i class="pi pi-times" />
      </button>
    </div>

    <!-- 다크 테마 목록 -->
    <div class="theme-selector__group">
      <span class="theme-selector__group-label">
        <i class="pi pi-moon" />
        다크 테마
        <i
          v-if="isPreviewLoading && darkThemes.some(t => t.value === previewThemeName)"
          class="pi pi-spin pi-spinner theme-selector__loading"
        />
      </span>
      <div class="theme-selector__list">
        <button
          v-for="theme in darkThemes"
          :key="theme.value"
          :class="[
            'theme-selector__item',
            { 'theme-selector__item--active': currentThemeName === theme.value },
          ]"
          @click="selectTheme(theme.value, $event)"
          @mouseenter="handleThemeHover(theme.value)"
          @mouseleave="handleThemeLeave"
        >
          <span class="theme-selector__colors">
            <span
              v-for="(color, index) in theme.accentColors"
              :key="index"
              class="theme-selector__color"
              :style="{ backgroundColor: color }"
            />
          </span>
          <span class="theme-selector__name">{{ theme.label }}</span>
          <i
            v-if="currentThemeName === theme.value"
            class="pi pi-check theme-selector__check"
          />
        </button>
      </div>
    </div>

    <!-- 라이트 테마 목록 -->
    <div class="theme-selector__group">
      <span class="theme-selector__group-label">
        <i class="pi pi-sun" />
        라이트 테마
        <i
          v-if="isPreviewLoading && lightThemes.some(t => t.value === previewThemeName)"
          class="pi pi-spin pi-spinner theme-selector__loading"
        />
      </span>
      <div class="theme-selector__list">
        <button
          v-for="theme in lightThemes"
          :key="theme.value"
          :class="[
            'theme-selector__item',
            { 'theme-selector__item--active': currentThemeName === theme.value },
          ]"
          @click="selectTheme(theme.value, $event)"
          @mouseenter="handleThemeHover(theme.value)"
          @mouseleave="handleThemeLeave"
        >
          <span class="theme-selector__colors">
            <span
              v-for="(color, index) in theme.accentColors"
              :key="index"
              class="theme-selector__color"
              :style="{ backgroundColor: color }"
            />
          </span>
          <span class="theme-selector__name">{{ theme.label }}</span>
          <i
            v-if="currentThemeName === theme.value"
            class="pi pi-check theme-selector__check"
          />
        </button>
      </div>
    </div>

    <!-- 추가 설정 버튼 -->
    <div
      v-if="!compact"
      class="theme-selector__actions"
    >
      <Button
        label="스케줄"
        icon="pi pi-clock"
        severity="secondary"
        text
        size="small"
        @click="openScheduleDialog"
      />
      <Button
        label="접근성"
        icon="pi pi-eye"
        severity="secondary"
        text
        size="small"
        @click="openAccessibilityDialog"
      />
      <Button
        icon="pi pi-download"
        severity="secondary"
        text
        size="small"
        title="설정 내보내기"
        @click="exportSettings"
      />
      <Button
        icon="pi pi-upload"
        severity="secondary"
        text
        size="small"
        title="설정 가져오기"
        @click="importSettings"
      />
      <Button
        icon="pi pi-refresh"
        severity="secondary"
        text
        size="small"
        title="설정 초기화"
        @click="resetSettings"
      />
    </div>

    <!-- 스케줄 설정 다이얼로그 -->
    <Dialog
      v-model:visible="showScheduleDialog"
      header="테마 스케줄 설정"
      :modal="true"
      :style="{ width: '400px' }"
    >
      <div class="schedule-form">
        <div class="schedule-form__field">
          <label>자동 전환 활성화</label>
          <ToggleSwitch v-model="scheduleForm.enabled" />
        </div>

        <template v-if="scheduleForm.enabled">
          <div class="schedule-form__field">
            <label>라이트 테마</label>
            <Select
              v-model="scheduleForm.lightTheme"
              :options="lightThemes"
              option-label="label"
              option-value="value"
              placeholder="라이트 테마 선택"
            />
          </div>

          <div class="schedule-form__field">
            <label>다크 테마</label>
            <Select
              v-model="scheduleForm.darkTheme"
              :options="darkThemes"
              option-label="label"
              option-value="value"
              placeholder="다크 테마 선택"
            />
          </div>

          <div class="schedule-form__row">
            <div class="schedule-form__field">
              <label>
                <i class="pi pi-sun" />
                라이트 시작
              </label>
              <InputText
                v-model="scheduleForm.sunriseTime"
                type="time"
              />
            </div>
            <div class="schedule-form__field">
              <label>
                <i class="pi pi-moon" />
                다크 시작
              </label>
              <InputText
                v-model="scheduleForm.sunsetTime"
                type="time"
              />
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <Button
          label="취소"
          severity="secondary"
          text
          @click="showScheduleDialog = false"
        />
        <Button
          label="저장"
          @click="saveSchedule"
        />
      </template>
    </Dialog>

    <!-- 접근성 설정 다이얼로그 -->
    <Dialog
      v-model:visible="showAccessibilityDialog"
      header="접근성 설정"
      :modal="true"
      :style="{ width: '350px' }"
    >
      <div class="a11y-form">
        <div class="a11y-form__field">
          <div class="a11y-form__info">
            <label>고대비 모드</label>
            <small>색상 대비를 높여 가독성을 향상시킵니다</small>
          </div>
          <ToggleSwitch
            :model-value="themeStore.accessibility.highContrast"
            @update:model-value="themeStore.setAccessibility({ highContrast: $event })"
          />
        </div>

        <div class="a11y-form__field">
          <div class="a11y-form__info">
            <label>애니메이션 줄이기</label>
            <small>움직임에 민감한 경우 애니메이션을 최소화합니다</small>
          </div>
          <ToggleSwitch
            :model-value="themeStore.accessibility.reducedMotion"
            @update:model-value="themeStore.setAccessibility({ reducedMotion: $event })"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="닫기"
          @click="showAccessibilityDialog = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.theme-selector {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
}

.theme-selector--compact {
  padding: 0.5rem;
  gap: 0.75rem;
}

.theme-selector__label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
  display: block;
}

.theme-selector__mode {
  display: flex;
  flex-direction: column;
}

.theme-selector__group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.theme-selector__group-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.theme-selector__list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.theme-selector__item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid transparent;
  border-radius: var(--p-border-radius);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
}

.theme-selector__item:hover {
  background: var(--p-surface-100);
  border-color: var(--p-surface-200);
}

.app-dark .theme-selector__item:hover {
  background: var(--p-surface-700);
  border-color: var(--p-surface-600);
}

.theme-selector__item--active {
  background: var(--p-primary-50);
  border-color: var(--p-primary-200);
}

.app-dark .theme-selector__item--active {
  background: color-mix(in srgb, var(--p-primary-color) 15%, transparent);
  border-color: var(--p-primary-700);
}

.theme-selector__colors {
  display: flex;
  gap: 2px;
}

.theme-selector__color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.theme-selector__name {
  flex: 1;
  font-size: 0.875rem;
  color: var(--p-text-color);
}

.theme-selector__check {
  color: var(--p-primary-color);
  font-size: 0.75rem;
}

.theme-selector__loading {
  margin-left: auto;
  font-size: 0.625rem;
  color: var(--p-primary-color);
}

/* 프리뷰 에러 메시지 */
.theme-selector__error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--p-red-50);
  border: 1px solid var(--p-red-200);
  border-radius: var(--p-border-radius);
  font-size: 0.75rem;
  color: var(--p-red-700);
}

.app-dark .theme-selector__error {
  background: color-mix(in srgb, var(--p-red-500) 15%, transparent);
  border-color: var(--p-red-800);
  color: var(--p-red-300);
}

.theme-selector__error-close {
  margin-left: auto;
  padding: 0.125rem;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  border-radius: var(--p-border-radius-sm);
  opacity: 0.7;
  transition: opacity 0.15s ease;
}

.theme-selector__error-close:hover {
  opacity: 1;
}

.theme-selector__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--p-surface-200);
}

.app-dark .theme-selector__actions {
  border-color: var(--p-surface-700);
}

/* Schedule Form */
.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.schedule-form__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.schedule-form__field label {
  font-size: 0.875rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.schedule-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* Accessibility Form */
.a11y-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.a11y-form__field {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.75rem;
  background: var(--p-surface-50);
  border-radius: var(--p-border-radius);
}

.app-dark .a11y-form__field {
  background: var(--p-surface-800);
}

.a11y-form__info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.a11y-form__info label {
  font-size: 0.875rem;
  font-weight: 500;
}

.a11y-form__info small {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}
</style>
