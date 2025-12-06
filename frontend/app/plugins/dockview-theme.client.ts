/**
 * Dockview Theme Dynamic Switcher Plugin
 *
 * 공식 Dockview 테마를 isDark 상태에 따라 동적 전환
 * - 다크 모드: dockview-theme-abyss (VS Code 스타일)
 * - 라이트 모드: dockview-theme-light
 *
 * HTML 요소에 테마 클래스를 추가하면 Dockview 컴포넌트가 상속받음
 *
 * @see https://dockview.dev/docs/overview/getStarted/theme/
 */

import { useThemeStore } from '~/stores/theme'

/** Dockview 빌트인 테마 클래스 목록 (제거 대상) */
const DOCKVIEW_BASE_THEMES = [
  'dockview-theme-dark',
  'dockview-theme-light',
  'dockview-theme-abyss',
  'dockview-theme-dracula',
  'dockview-theme-vs',
  'dockview-theme-replit',
] as const

export default defineNuxtPlugin(() => {
  const themeStore = useThemeStore()

  // Dockview 기본 테마 클래스를 HTML에 동적 적용
  watchEffect(() => {
    const html = document.documentElement

    // 다크모드: abyss (VS Code 스타일), 라이트모드: light
    const baseTheme = themeStore.isDark
      ? 'dockview-theme-abyss'
      : 'dockview-theme-light'

    // 기존 Dockview 기본 테마 클래스 모두 제거
    html.classList.remove(...DOCKVIEW_BASE_THEMES)

    // 새 기본 테마 클래스 추가
    html.classList.add(baseTheme)

    if (import.meta.dev) {
      console.log('[Dockview Theme]', {
        isDark: themeStore.isDark,
        baseTheme,
      })
    }
  })
})
