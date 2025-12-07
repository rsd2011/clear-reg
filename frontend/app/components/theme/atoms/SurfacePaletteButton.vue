<script setup lang="ts">
/**
 * SurfacePaletteButton - Surface 색상 팔레트 선택 버튼
 */
import Tooltip from 'primevue/tooltip'

defineProps<{
  /** 색상 값 (hex) */
  color: string
  /** 색상 이름 */
  name: string
  /** 색상 설명 */
  description: string
  /** 선택 상태 */
  selected?: boolean
}>()

defineEmits<{
  select: []
}>()

const vTooltip = Tooltip
</script>

<template>
  <button
    v-tooltip.bottom="`${name} - ${description}`"
    class="surface-palette-btn"
    :class="{ 'is-selected': selected }"
    @click="$emit('select')"
  >
    <!-- 그라데이션 프리뷰 (밝음 → 어두움) -->
    <div class="flex flex-col h-8 w-full rounded-md overflow-hidden shadow-sm">
      <div
        class="flex-1"
        :style="{ backgroundColor: color }"
      />
    </div>
    <span class="text-[10px] text-surface-500 dark:text-surface-400 mt-1">
      {{ name }}
    </span>
    <!-- 선택 체크마크 -->
    <div
      v-if="selected"
      class="absolute -top-1 -right-1 w-4 h-4 bg-primary rounded-full flex items-center justify-center shadow-md"
    >
      <i class="pi pi-check text-white text-[8px]" />
    </div>
  </button>
</template>

<style scoped>
/* CSS 변수 정의 - 다크모드 자동 전환 */
.surface-palette-btn {
  /* 컴포넌트 레벨 CSS 변수 (라이트 모드 기본값) */
  --btn-bg: var(--p-surface-50);
  --btn-bg-hover: var(--p-surface-100);
  --btn-border: var(--p-surface-200);
  --btn-ring-offset: var(--p-surface-0);

  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.375rem;
  border-radius: 0.5rem;
  background-color: var(--btn-bg);
  border: 1px solid var(--btn-border);
  cursor: pointer;
  transition: all 0.15s ease;
}

.surface-palette-btn:hover {
  background-color: var(--btn-bg-hover);
  transform: scale(1.05);
}

.surface-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--p-primary-color);
}

/* Selected state - box-shadow로 ring 효과 구현 */
.surface-palette-btn.is-selected {
  box-shadow:
    0 0 0 2px var(--btn-ring-offset),
    0 0 0 4px var(--p-primary-color);
}

/* 다크모드 - CSS 변수 오버라이드 */
:global(.app-dark) .surface-palette-btn {
  --btn-bg: var(--p-surface-800);
  --btn-bg-hover: var(--p-surface-700);
  --btn-border: var(--p-surface-700);
  --btn-ring-offset: var(--p-surface-900);
}
</style>
