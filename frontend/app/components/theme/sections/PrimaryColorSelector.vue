<script setup lang="ts">
/**
 * PrimaryColorSelector - Primary 색상 선택 섹션
 */
import ColorPaletteButton from '../atoms/ColorPaletteButton.vue'
import { useColorPalette } from '~/composables/theme/useColorPalette'

const { colorPalettes, isColorSelected, selectColor } = useColorPalette()
</script>

<template>
  <section>
    <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
      <i class="pi pi-palette mr-2" />Primary 색상
    </h3>
    <div class="grid grid-cols-6 gap-2">
      <ColorPaletteButton
        v-for="color in colorPalettes"
        :key="color.value"
        :color="color.color"
        :name="color.name"
        :selected="isColorSelected(color.value)"
        @select="selectColor(color.value)"
      />
    </div>
  </section>
</template>
