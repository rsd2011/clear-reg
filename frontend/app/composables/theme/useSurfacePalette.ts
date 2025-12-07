import { computed } from 'vue'
import { useThemeStore } from '~/stores/theme'
import { SURFACE_PALETTES, SURFACE_COLOR_NAMES, type SurfaceColorName } from '~/themes'

/**
 * Surface 색상 팔레트 선택 로직
 */
export function useSurfacePalette() {
  const themeStore = useThemeStore()

  /** Surface 색상 팔레트 목록 */
  const surfacePalettes = computed(() =>
    SURFACE_COLOR_NAMES.map(name => ({
      value: name,
      ...SURFACE_PALETTES[name],
    })),
  )

  /** 현재 선택된 Surface 색상 */
  const selectedSurface = computed(() => themeStore.surfaceColor)

  /**
   * Surface 색상이 현재 선택되었는지 확인
   */
  const isSurfaceSelected = (colorName: SurfaceColorName): boolean => {
    return selectedSurface.value === colorName
  }

  /**
   * Surface 색상 선택
   */
  const selectSurface = (colorName: SurfaceColorName) => {
    themeStore.setSurfaceColor(colorName)
  }

  return {
    surfacePalettes,
    selectedSurface,
    isSurfaceSelected,
    selectSurface,
  }
}
