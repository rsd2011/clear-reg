<script setup lang="ts">
/**
 * ColorPaletteButton - 색상 팔레트 선택 버튼
 * Primary/Surface 색상 선택에 공용으로 사용
 */
import Tooltip from 'primevue/tooltip'

defineProps<{
  /** 색상 값 (hex) */
  color: string
  /** 색상 이름 (툴팁용) */
  name: string
  /** 선택 상태 */
  selected?: boolean
  /** 툴팁 텍스트 (기본: name) */
  tooltipText?: string
}>()

defineEmits<{
  select: []
}>()

const vTooltip = Tooltip
</script>

<template>
  <button
    v-tooltip.bottom="tooltipText || name"
    class="color-palette-btn"
    :class="{ 'is-selected': selected }"
    :style="{ '--color-main': color }"
    @click="$emit('select')"
  >
    <div
      class="w-6 h-6 rounded-full shadow-sm"
      :style="{ backgroundColor: color }"
    />
    <!-- 선택 체크마크 -->
    <div
      v-if="selected"
      class="absolute inset-0 flex items-center justify-center"
    >
      <i class="pi pi-check text-white text-xs drop-shadow-md" />
    </div>
  </button>
</template>

<style scoped>
/* CSS 변수 정의 - 다크모드 자동 전환 */
.color-palette-btn {
  /* 컴포넌트 레벨 CSS 변수 (라이트 모드 기본값) */
  --btn-bg: var(--p-surface-50);
  --btn-bg-hover: var(--p-surface-100);
  --btn-border: var(--p-surface-200);
  --btn-ring-offset: var(--p-surface-0);
  --ring-color: var(--color-main, var(--p-primary-color));

  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border-radius: 0.5rem;
  background-color: var(--btn-bg);
  border: 1px solid var(--btn-border);
  cursor: pointer;
  transition: all 0.15s ease;
}

.color-palette-btn:hover {
  background-color: var(--btn-bg-hover);
  transform: scale(1.1);
}

.color-palette-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--ring-color);
}

/* Selected state - box-shadow로 ring 효과 구현 */
.color-palette-btn.is-selected {
  box-shadow:
    0 0 0 2px var(--btn-ring-offset),
    0 0 0 4px var(--ring-color);
}

/* 다크모드 - CSS 변수 오버라이드 */
:global(.app-dark) .color-palette-btn {
  --btn-bg: var(--p-surface-800);
  --btn-bg-hover: var(--p-surface-700);
  --btn-border: var(--p-surface-700);
  --btn-ring-offset: var(--p-surface-900);
}
</style>
