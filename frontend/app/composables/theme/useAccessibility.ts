import { computed } from 'vue'
import { useThemeStore } from '~/stores/theme'

/**
 * 접근성 옵션 로직
 */
export function useAccessibility() {
  const themeStore = useThemeStore()

  /** 고대비 모드 (v-model 바인딩용) */
  const highContrast = computed({
    get: () => themeStore.accessibility.highContrast,
    set: (value: boolean) => themeStore.setAccessibility({ highContrast: value }),
  })

  /** 애니메이션 줄이기 (v-model 바인딩용) */
  const reducedMotion = computed({
    get: () => themeStore.accessibility.reducedMotion,
    set: (value: boolean) => themeStore.setAccessibility({ reducedMotion: value }),
  })

  /** 줄임 모션 활성 여부 (시스템 설정 + 사용자 설정) */
  const shouldReduceMotion = computed(() => themeStore.shouldReduceMotion)

  return {
    highContrast,
    reducedMotion,
    shouldReduceMotion,
  }
}
