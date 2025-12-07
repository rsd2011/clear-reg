<script setup lang="ts">
/**
 * AccessibilityOptions - 접근성 옵션 섹션
 */
import AccessibilityToggle from '../atoms/AccessibilityToggle.vue'
import { useAccessibility } from '~/composables/theme/useAccessibility'

const { highContrast, reducedMotion } = useAccessibility()
</script>

<template>
  <section>
    <h3 class="text-sm font-semibold text-surface-500 dark:text-surface-400 mb-3">
      <i class="pi pi-eye mr-2" />접근성
    </h3>
    <div class="flex flex-col gap-4">
      <AccessibilityToggle
        v-model="highContrast"
        label="고대비 모드"
        input-id="high-contrast"
      />
      <AccessibilityToggle
        v-model="reducedMotion"
        label="애니메이션 줄이기"
        input-id="reduced-motion"
      />
    </div>
  </section>
</template>
