<script setup lang="ts">
/**
 * ThemeModeSelector - 모드 선택 섹션 (Light/Dark/System)
 */
import SelectButton from 'primevue/selectbutton'
import { useThemeMode } from '~/composables/theme/useThemeMode'

const { modeOptions, selectedMode } = useThemeMode()
</script>

<template>
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
</template>
