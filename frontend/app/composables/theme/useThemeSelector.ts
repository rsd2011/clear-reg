import { computed } from 'vue'
import { useThemeStore } from '~/stores/theme'
import { THEMES, type ThemeName } from '~/themes'

/**
 * 테마 프리셋 선택 및 프리뷰 로직
 */
export function useThemeSelector() {
  const themeStore = useThemeStore()

  /** 전체 테마 목록 */
  const allThemes = computed(() =>
    Object.entries(THEMES).map(([key, config]) => ({
      value: key as ThemeName,
      ...config,
    })),
  )

  /** 다크 테마 목록 */
  const darkThemes = computed(() => themeStore.darkThemes)

  /** 라이트 테마 목록 */
  const lightThemes = computed(() => themeStore.lightThemes)

  /** 현재 선택된 테마 */
  const selectedTheme = computed(() => themeStore.themeName)

  /** 프리뷰 로딩 상태 */
  const isPreviewLoading = computed(() => themeStore.isPreviewLoading)

  /** 프리뷰 에러 */
  const previewError = computed(() => themeStore.previewError)

  /**
   * 테마가 현재 선택되었는지 확인
   */
  const isSelected = (themeName: ThemeName): boolean => {
    return selectedTheme.value === themeName
  }

  /**
   * 테마 선택
   */
  const selectTheme = (themeName: ThemeName, event?: MouseEvent) => {
    themeStore.setTheme(themeName, event)
  }

  /**
   * 테마 프리뷰 시작 (hover)
   */
  const startPreview = (themeName: ThemeName) => {
    themeStore.startPreview(themeName)
  }

  /**
   * 테마 프리뷰 취소 (hover 종료)
   */
  const cancelPreview = () => {
    themeStore.cancelPreview()
  }

  /**
   * 프리뷰 중인 테마 확정
   */
  const confirmPreview = () => {
    themeStore.confirmPreview()
  }

  return {
    allThemes,
    darkThemes,
    lightThemes,
    selectedTheme,
    isPreviewLoading,
    previewError,
    isSelected,
    selectTheme,
    startPreview,
    cancelPreview,
    confirmPreview,
  }
}
