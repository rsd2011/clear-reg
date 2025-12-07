import { computed } from 'vue'
import { useThemeStore } from '~/stores/theme'
import { COLOR_PALETTES, PRIMARY_COLOR_NAMES, type PrimaryColorName } from '~/themes'

/**
 * Primary 색상 팔레트 선택 로직
 */
export function useColorPalette() {
  const themeStore = useThemeStore()

  /** 색상 팔레트 목록 */
  const colorPalettes = computed(() =>
    PRIMARY_COLOR_NAMES.map(name => ({
      value: name,
      ...COLOR_PALETTES[name],
    })),
  )

  /** 현재 선택된 Primary 색상 */
  const selectedColor = computed(() => themeStore.primaryColor)

  /**
   * 색상이 현재 선택되었는지 확인
   */
  const isColorSelected = (colorName: PrimaryColorName): boolean => {
    return selectedColor.value === colorName
  }

  /**
   * Primary 색상 선택
   */
  const selectColor = (colorName: PrimaryColorName) => {
    themeStore.setPrimaryColor(colorName)
  }

  return {
    colorPalettes,
    selectedColor,
    isColorSelected,
    selectColor,
  }
}
