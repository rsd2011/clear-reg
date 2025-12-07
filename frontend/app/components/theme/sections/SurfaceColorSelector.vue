<script setup lang="ts">
/**
 * SurfaceColorSelector - Surface 색상 선택 섹션
 */
import SurfacePaletteButton from '../atoms/SurfacePaletteButton.vue'
import { useSurfacePalette } from '~/composables/theme/useSurfacePalette'

const { surfacePalettes, isSurfaceSelected, selectSurface } = useSurfacePalette()
</script>

<template>
  <section>
    <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
      <i class="pi pi-th-large mr-2" />Surface 색상
    </h3>
    <div class="grid grid-cols-5 gap-2">
      <SurfacePaletteButton
        v-for="surface in surfacePalettes"
        :key="surface.value"
        :color="surface.color"
        :name="surface.name"
        :description="surface.description"
        :selected="isSurfaceSelected(surface.value)"
        @select="selectSurface(surface.value)"
      />
    </div>
  </section>
</template>
