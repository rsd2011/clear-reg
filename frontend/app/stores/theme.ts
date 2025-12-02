import { defineStore } from 'pinia'
import { usePreset } from '@primeuix/themes'
import { LinearDarkPreset } from '~/themes/linear-dark'
import { KoscomLightPreset } from '~/themes/koscom-light'
import type { ThemeName, ThemeMode } from '~/themes'
import { THEMES } from '~/themes'

const STORAGE_KEYS = {
  themeName: 'enterman-theme-name',
  themeMode: 'enterman-theme-mode',
} as const

export const useThemeStore = defineStore('theme', {
  state: () => ({
    themeName: 'linear-dark' as ThemeName,
    themeMode: 'system' as ThemeMode,
    isDark: false,
    isInitialized: false,
  }),

  getters: {
    currentTheme: state => THEMES[state.themeName],
    availableThemes: () => Object.entries(THEMES).map(([key, value]) => ({
      value: key as ThemeName,
      label: value.name,
      description: value.description,
    })),
  },

  actions: {
    init() {
      if (this.isInitialized) return

      // localStorage에서 복원
      const savedTheme = localStorage.getItem(STORAGE_KEYS.themeName) as ThemeName | null
      const savedMode = localStorage.getItem(STORAGE_KEYS.themeMode) as ThemeMode | null

      if (savedTheme && savedTheme in THEMES) {
        this.themeName = savedTheme
      }
      if (savedMode && ['system', 'dark', 'light'].includes(savedMode)) {
        this.themeMode = savedMode
      }

      this.applyTheme()
      this.watchSystemTheme()
      this.isInitialized = true
    },

    setTheme(name: ThemeName) {
      this.themeName = name
      localStorage.setItem(STORAGE_KEYS.themeName, name)

      // 테마 선택 시 해당 테마의 prefersDark에 따라 다크모드 자동 설정
      // (단, system 모드가 아닐 때만)
      if (this.themeMode !== 'system') {
        const theme = THEMES[name]
        this.themeMode = theme.prefersDark ? 'dark' : 'light'
        localStorage.setItem(STORAGE_KEYS.themeMode, this.themeMode)
      }

      this.applyTheme()
    },

    setMode(mode: ThemeMode) {
      this.themeMode = mode
      localStorage.setItem(STORAGE_KEYS.themeMode, mode)
      this.applyTheme()
    },

    toggleDarkMode() {
      // 현재 다크모드 상태를 반대로 전환
      this.setMode(this.isDark ? 'light' : 'dark')
    },

    /**
     * 테마 및 다크모드 적용
     *
     * 순서:
     * 1. 다크모드 상태 결정 (system/dark/light)
     * 2. HTML 클래스 업데이트
     *    - 테마 클래스: .theme-linear-dark 또는 .theme-koscom-light
     *    - 다크모드 클래스: .app-dark (다크일 때만)
     * 3. PrimeVue 프리셋 전환 (CSS 변수 업데이트)
     */
    applyTheme() {
      // 1. 다크모드 결정
      if (this.themeMode === 'system') {
        this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      }
      else {
        this.isDark = this.themeMode === 'dark'
      }

      // 2. HTML 클래스 업데이트
      const html = document.documentElement
      const theme = THEMES[this.themeName]

      // 모든 테마 클래스 제거 후 현재 테마 추가
      html.classList.remove('theme-linear-dark', 'theme-koscom-light')
      html.classList.add(theme.className)

      // 다크모드 클래스 토글
      html.classList.toggle('app-dark', this.isDark)

      // 3. PrimeVue 프리셋 전환 (CSS 변수 업데이트)
      const preset = this.themeName === 'linear-dark'
        ? LinearDarkPreset
        : KoscomLightPreset

      usePreset(preset)

      // 디버깅용 로그 (개발 모드에서만)
      if (import.meta.dev) {
        console.log('[Theme]', {
          themeName: this.themeName,
          themeMode: this.themeMode,
          isDark: this.isDark,
          htmlClasses: html.className,
        })
      }
    },

    watchSystemTheme() {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', () => {
        if (this.themeMode === 'system') {
          this.applyTheme()
        }
      })
    },
  },
})
