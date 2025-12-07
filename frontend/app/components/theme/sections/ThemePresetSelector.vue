<script setup lang="ts">
/**
 * ThemePresetSelector - 테마 프리셋 선택 섹션
 */
import ThemePaletteButton from '../atoms/ThemePaletteButton.vue'
import { useThemeSelector } from '~/composables/theme/useThemeSelector'

const {
  allThemes,
  isSelected,
  selectTheme,
  startPreview,
  cancelPreview,
} = useThemeSelector()
</script>

<template>
  <section>
    <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
      <i class="pi pi-images mr-2" />테마
    </h3>
    <div class="grid grid-cols-3 gap-3">
      <ThemePaletteButton
        v-for="theme in allThemes"
        :key="theme.value"
        :value="theme.value"
        :theme="theme"
        :selected="isSelected(theme.value)"
        @select="selectTheme(theme.value, $event)"
        @preview-start="startPreview(theme.value)"
        @preview-end="cancelPreview"
      />
    </div>
  </section>
</template>
