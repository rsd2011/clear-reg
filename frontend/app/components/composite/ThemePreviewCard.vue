<script setup lang="ts">
/**
 * ThemePreviewCard - 테마 프리뷰 카드 컴포넌트
 *
 * 기능:
 * - 테마 미리보기 카드 표시
 * - accentColors 3색 미리보기 바
 * - hover 시 프리뷰 모드 활성화
 * - 클릭 시 테마 선택
 */

import type { ThemeConfig, ThemeName, ThemeTag } from '~/themes'

// ============================================================================
// Props & Emits
// ============================================================================

interface Props {
  /** 테마 ID */
  themeName: ThemeName
  /** 테마 설정 */
  theme: ThemeConfig
  /** 선택 상태 */
  selected?: boolean
  /** 프리뷰 중인 상태 */
  previewing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  previewing: false,
})

const emit = defineEmits<{
  /** 테마 선택 시 */
  'select': [themeName: ThemeName, event: MouseEvent]
  /** 프리뷰 시작 */
  'preview-start': [themeName: ThemeName]
  /** 프리뷰 종료 */
  'preview-end': []
}>()

// ============================================================================
// Methods
// ============================================================================

function handleClick(event: MouseEvent) {
  emit('select', props.themeName, event)
}

function handleMouseEnter() {
  emit('preview-start', props.themeName)
}

function handleMouseLeave() {
  emit('preview-end')
}

/** 태그 색상 매핑 */
function getTagSeverity(tag: ThemeTag): 'info' | 'success' | 'warn' | 'secondary' {
  const tagColors: Record<ThemeTag, 'info' | 'success' | 'warn' | 'secondary'> = {
    modern: 'info',
    minimal: 'secondary',
    corporate: 'warn',
    developer: 'success',
    warm: 'warn',
    cool: 'info',
    vibrant: 'success',
    muted: 'secondary',
  }
  return tagColors[tag] || 'secondary'
}
</script>

<template>
  <div
    :class="[
      'theme-preview-card',
      {
        'theme-preview-card--selected': selected,
        'theme-preview-card--previewing': previewing,
      },
    ]"
    role="button"
    tabindex="0"
    @click="handleClick"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <!-- 색상 미리보기 바 -->
    <div class="theme-preview-card__colors">
      <div
        v-for="(color, index) in theme.accentColors"
        :key="index"
        class="theme-preview-card__color"
        :style="{ backgroundColor: color }"
      />
    </div>

    <!-- 테마 정보 -->
    <div class="theme-preview-card__content">
      <div class="theme-preview-card__header">
        <h4 class="theme-preview-card__name">
          {{ theme.name }}
        </h4>
        <i
          :class="[
            'theme-preview-card__mode-icon',
            theme.prefersDark ? 'pi pi-moon' : 'pi pi-sun',
          ]"
        />
      </div>

      <p class="theme-preview-card__description">
        {{ theme.description }}
      </p>

      <div class="theme-preview-card__tags">
        <Tag
          v-for="tag in theme.tags.slice(0, 2)"
          :key="tag"
          :value="tag"
          :severity="getTagSeverity(tag)"
          class="theme-preview-card__tag"
        />
      </div>
    </div>

    <!-- 선택 표시 -->
    <div
      v-if="selected"
      class="theme-preview-card__check"
    >
      <i class="pi pi-check" />
    </div>
  </div>
</template>

<style scoped>
.theme-preview-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0;
  border: 2px solid var(--p-surface-200);
  border-radius: var(--p-border-radius-lg);
  background: var(--p-surface-0);
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
}

.app-dark .theme-preview-card {
  border-color: var(--p-surface-700);
  background: var(--p-surface-800);
}

.theme-preview-card:hover {
  border-color: var(--p-primary-300);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.app-dark .theme-preview-card:hover {
  border-color: var(--p-primary-600);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.theme-preview-card--selected {
  border-color: var(--p-primary-500);
  background: var(--p-primary-50);
}

.app-dark .theme-preview-card--selected {
  border-color: var(--p-primary-500);
  background: color-mix(in srgb, var(--p-primary-color) 10%, var(--p-surface-800));
}

.theme-preview-card--previewing {
  border-color: var(--p-primary-400);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--p-primary-color) 20%, transparent);
}

.theme-preview-card:focus-visible {
  outline: 2px solid var(--p-primary-500);
  outline-offset: 2px;
}

/* 색상 미리보기 바 */
.theme-preview-card__colors {
  display: flex;
  height: 8px;
}

.theme-preview-card__color {
  flex: 1;
}

.theme-preview-card__color:first-child {
  border-top-left-radius: calc(var(--p-border-radius-lg) - 2px);
}

.theme-preview-card__color:last-child {
  border-top-right-radius: calc(var(--p-border-radius-lg) - 2px);
}

/* 콘텐츠 영역 */
.theme-preview-card__content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
}

.theme-preview-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.theme-preview-card__name {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.theme-preview-card__mode-icon {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.theme-preview-card__description {
  margin: 0;
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-preview-card__tags {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.theme-preview-card__tag {
  font-size: 0.625rem;
  padding: 0.125rem 0.375rem;
}

/* 선택 표시 */
.theme-preview-card__check {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 1.25rem;
  height: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--p-primary-500);
  border-radius: 50%;
  color: white;
  font-size: 0.625rem;
}
</style>
