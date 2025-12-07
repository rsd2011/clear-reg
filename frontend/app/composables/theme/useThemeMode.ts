import { computed } from 'vue'
import { useThemeStore } from '~/stores/theme'
import type { ThemeMode } from '~/themes'

/**
 * 테마 모드(Light/Dark/System) 선택 로직
 */
export function useThemeMode() {
  const themeStore = useThemeStore()

  /** 모드 선택 옵션 */
  const modeOptions = [
    { value: 'light' as ThemeMode, icon: 'pi pi-sun', label: '라이트' },
    { value: 'dark' as ThemeMode, icon: 'pi pi-moon', label: '다크' },
    { value: 'system' as ThemeMode, icon: 'pi pi-desktop', label: '시스템' },
  ]

  /** 현재 선택된 모드 (v-model 바인딩용) */
  const selectedMode = computed({
    get: () => themeStore.themeMode,
    set: (value: ThemeMode) => themeStore.setMode(value),
  })

  /** 현재 다크모드 여부 */
  const isDark = computed(() => themeStore.isDark)

  /** 다크모드 토글 */
  const toggleDarkMode = () => {
    themeStore.toggleDarkMode()
  }

  return {
    modeOptions,
    selectedMode,
    isDark,
    toggleDarkMode,
  }
}
