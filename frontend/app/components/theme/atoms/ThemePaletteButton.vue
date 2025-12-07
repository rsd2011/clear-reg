<script setup lang="ts">
/**
 * ThemePaletteButton - 테마 프리셋 선택 버튼
 */
import Tooltip from 'primevue/tooltip'
import type { ThemeConfig, ThemeName } from '~/themes'

defineProps<{
  /** 테마 값 */
  value: ThemeName
  /** 테마 설정 */
  theme: ThemeConfig
  /** 선택 상태 */
  selected?: boolean
}>()

defineEmits<{
  'select': [event: MouseEvent]
  'preview-start': []
  'preview-end': []
}>()

const vTooltip = Tooltip
</script>

<template>
  <button
    v-tooltip.bottom="theme.name"
    class="theme-palette-btn group"
    :class="{ 'is-selected': selected }"
    @click="$emit('select', $event)"
    @mouseenter="$emit('preview-start')"
    @mouseleave="$emit('preview-end')"
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
      v-if="selected"
      class="absolute -top-1 -right-1 w-5 h-5 bg-primary rounded-full flex items-center justify-center shadow-md"
    >
      <i class="pi pi-check text-white text-xs" />
    </div>
  </button>
</template>

<style scoped>
/* CSS 변수 정의 - 다크모드 자동 전환 */
.theme-palette-btn {
  /* 컴포넌트 레벨 CSS 변수 (라이트 모드 기본값) */
  --btn-bg: var(--p-surface-50);
  --btn-bg-hover: var(--p-surface-100);
  --btn-border: var(--p-surface-200);
  --btn-ring-offset: var(--p-surface-0);

  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem;
  border-radius: 0.75rem;
  background-color: var(--btn-bg);
  border: 1px solid var(--btn-border);
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-palette-btn:hover {
  background-color: var(--btn-bg-hover);
  transform: scale(1.05);
}

.theme-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--p-primary-color);
}

/* Selected state - box-shadow로 ring 효과 구현 */
.theme-palette-btn.is-selected {
  box-shadow:
    0 0 0 2px var(--btn-ring-offset),
    0 0 0 4px var(--p-primary-color);
}

/* 다크모드 - CSS 변수 오버라이드 */
:global(.app-dark) .theme-palette-btn {
  --btn-bg: var(--p-surface-800);
  --btn-bg-hover: var(--p-surface-700);
  --btn-border: var(--p-surface-700);
  --btn-ring-offset: var(--p-surface-900);
}
</style>
