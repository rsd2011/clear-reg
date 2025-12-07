import { useThemeStore } from '~/stores/theme'
import { useThemeMode } from './useThemeMode'
import { useColorPalette } from './useColorPalette'
import { useSurfacePalette } from './useSurfacePalette'
import { useThemeSelector } from './useThemeSelector'
import { useAccessibility } from './useAccessibility'

/**
 * 테마 설정 Facade Composable
 * 모든 테마 관련 composable을 통합하여 단일 진입점 제공
 */
export function useThemeConfigurator() {
  const themeStore = useThemeStore()

  // Sub-composables
  const themeMode = useThemeMode()
  const colorPalette = useColorPalette()
  const surfacePalette = useSurfacePalette()
  const themeSelector = useThemeSelector()
  const accessibility = useAccessibility()

  /**
   * 설정 초기화
   */
  const resetSettings = () => {
    themeStore.resetAllSettings()
  }

  /**
   * 설정 내보내기 (다운로드)
   */
  const downloadSettings = (filename?: string) => {
    themeStore.downloadSettings(filename)
  }

  /**
   * 설정 가져오기 (파일)
   */
  const importFromFile = () => {
    return themeStore.importFromFile()
  }

  return {
    // Theme Mode
    ...themeMode,

    // Color Palette
    ...colorPalette,

    // Surface Palette
    ...surfacePalette,

    // Theme Selector
    ...themeSelector,

    // Accessibility
    ...accessibility,

    // Actions
    resetSettings,
    downloadSettings,
    importFromFile,
  }
}

// Re-export sub-composables for direct usage
export { useThemeMode } from './useThemeMode'
export { useColorPalette } from './useColorPalette'
export { useSurfacePalette } from './useSurfacePalette'
export { useThemeSelector } from './useThemeSelector'
export { useAccessibility } from './useAccessibility'
