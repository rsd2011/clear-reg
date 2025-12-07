<script setup lang="ts">
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import Button from 'primevue/button'
import ThemeConfigurator from '~/components/common/ThemeConfigurator.vue'

// ============================================================================
// State
// ============================================================================

/** 테마 설정 Drawer 표시 여부 */
const themeConfiguratorVisible = ref(false)

// ============================================================================
// Methods
// ============================================================================

/**
 * 테마 설정 패널 열기
 */
function openThemeConfigurator() {
  themeConfiguratorVisible.value = true
}
</script>

<template>
  <div class="min-h-screen bg-[var(--app-bg)] text-[var(--app-text)] transition-colors duration-300">
    <Toast position="top-right" />
    <ConfirmDialog />

    <!-- 메인 콘텐츠 -->
    <slot />

    <!-- ═══════════════════════════════════════════════════════════════════════
         테마 설정 플로팅 버튼 (PrimeVue 공식 사이트 스타일)
         ═══════════════════════════════════════════════════════════════════════ -->
    <Button
      icon="pi pi-palette"
      class="theme-config-fab"
      rounded
      aria-label="테마 설정"
      @click="openThemeConfigurator"
    />

    <!-- 테마 설정 Drawer -->
    <ThemeConfigurator v-model:visible="themeConfiguratorVisible" />
  </div>
</template>

<style scoped>
/**
 * 테마 설정 플로팅 액션 버튼 (FAB)
 * - 우측 하단 고정 위치
 * - 호버 시 회전 애니메이션
 * - PrimeVue 공식 사이트 스타일
 */
.theme-config-fab {
  @apply fixed z-50 shadow-lg;
  right: 1.5rem;
  bottom: 1.5rem;
  width: 3rem;
  height: 3rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.theme-config-fab:hover {
  transform: rotate(15deg) scale(1.1);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
}

/* 다크모드에서 그림자 조정 */
:global(.app-dark) .theme-config-fab {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
}

:global(.app-dark) .theme-config-fab:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
}
</style>
