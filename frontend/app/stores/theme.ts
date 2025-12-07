import { defineStore } from 'pinia'
import { usePreset, updatePrimaryPalette, updateSurfacePalette } from '@primeuix/themes'
import type { Preset, PaletteDesignToken } from '@primeuix/themes/types'
import type { ThemeName, ThemeMode, PrimaryColorName, SurfaceColorName } from '~/themes'
import { THEMES, COLOR_PALETTES, SURFACE_PALETTES } from '~/themes'
import { runThemeValidation } from '~/utils/theme-validator'
import {
  getPrimaryPaletteFromCssVars,
  getSurfacePaletteFromCssVars,
} from '~/utils/color-utils'

// ============================================================================
// 🆕 Dynamic Theme Preset Loader
// ============================================================================

/**
 * 테마 프리셋 동적 로더 맵
 * - 초기 번들 크기 최적화: 사용하는 테마만 로드
 * - 코드 스플리팅: 각 테마가 별도 청크로 분리됨
 */
const THEME_PRESET_LOADERS: Record<ThemeName, () => Promise<unknown>> = {
  'linear-dark': () => import('~/themes/linear-dark').then(m => m.LinearDarkPreset),
  'github-dark': () => import('~/themes/github-dark').then(m => m.GithubDarkPreset),
  'figma-dark': () => import('~/themes/figma-dark').then(m => m.FigmaDarkPreset),
  'slack-aubergine': () => import('~/themes/slack-aubergine').then(m => m.SlackAuberginePreset),
  'koscom-light': () => import('~/themes/koscom-light').then(m => m.KoscomLightPreset),
  'notion-light': () => import('~/themes/notion-light').then(m => m.NotionLightPreset),
}

/** 로드된 프리셋 캐시 (중복 로드 방지) */
const presetCache = new Map<ThemeName, unknown>()

/**
 * 테마 프리셋 로드 (캐싱 적용)
 * @param name - 테마 이름
 * @returns PrimeVue 프리셋
 */
async function loadPreset(name: ThemeName): Promise<unknown> {
  if (!presetCache.has(name)) {
    const preset = await THEME_PRESET_LOADERS[name]()
    presetCache.set(name, preset)
  }
  return presetCache.get(name)!
}

/**
 * 초기 테마 프리셋 즉시 로드 (FOUC 방지)
 * nuxt.config.ts의 인라인 스크립트와 함께 사용
 */
async function _preloadInitialTheme(name: ThemeName): Promise<void> {
  await loadPreset(name)
}

// ============================================================================
// Types
// ============================================================================

export interface ThemeSchedule {
  enabled: boolean
  lightTheme: ThemeName
  darkTheme: ThemeName
  /** 라이트 테마 시작 시간 (HH:MM) */
  sunriseTime: string
  /** 다크 테마 시작 시간 (HH:MM) */
  sunsetTime: string
}

export interface AccessibilityOptions {
  /** 고대비 모드 */
  highContrast: boolean
  /** 애니메이션 줄이기 */
  reducedMotion: boolean
}

// ============================================================================
// Constants
// ============================================================================

/**
 * 🆕 pinia-plugin-persistedstate 영속화 키
 * - 기존 STORAGE_KEYS와 하위 호환성 유지를 위해 동일한 prefix 사용
 */
const PERSIST_KEY = 'app-theme'

/**
 * @deprecated pinia-plugin-persistedstate로 대체됨
 * 하위 호환성을 위해 마이그레이션 로직에서만 사용
 */
const LEGACY_STORAGE_KEYS = {
  themeName: 'app-theme-name',
  themeMode: 'app-theme-mode',
  schedule: 'app-theme-schedule',
  accessibility: 'app-theme-a11y',
} as const

/** 테마 설정 내보내기 포맷 */
export interface ThemeExportData {
  version: 1
  exportedAt: string
  settings: {
    themeName: ThemeName
    themeMode: ThemeMode
    schedule: ThemeSchedule
    accessibility: AccessibilityOptions
  }
}

/** 모든 테마 클래스명 목록 (제거용) */
const ALL_THEME_CLASSES = Object.values(THEMES).map(t => t.className)

/** 기본 스케줄 설정 */
const DEFAULT_SCHEDULE: ThemeSchedule = {
  enabled: false,
  lightTheme: 'notion-light',
  darkTheme: 'linear-dark',
  sunriseTime: '07:00',
  sunsetTime: '19:00',
}

/** 기본 접근성 설정 */
const DEFAULT_ACCESSIBILITY: AccessibilityOptions = {
  highContrast: false,
  reducedMotion: false,
}

// ============================================================================
// Theme Store
// ============================================================================

export const useThemeStore = defineStore('theme', {
  state: () => ({
    themeName: 'linear-dark' as ThemeName,
    themeMode: 'system' as ThemeMode,
    primaryColor: 'indigo' as PrimaryColorName,
    surfaceColor: 'slate' as SurfaceColorName,
    isDark: false,
    isInitialized: false,
    // Phase 2: 프리뷰 모드 (Enhanced)
    isPreviewMode: false,
    previewThemeName: null as ThemeName | null,
    isPreviewLoading: false,
    previewError: null as string | null,
    previewDebounceTimer: null as ReturnType<typeof setTimeout> | null,
    // Phase 2: 시간대별 자동 전환
    schedule: { ...DEFAULT_SCHEDULE } as ThemeSchedule,
    scheduleTimerId: null as ReturnType<typeof setInterval> | null,
    // Phase 2: 접근성 옵션
    accessibility: { ...DEFAULT_ACCESSIBILITY } as AccessibilityOptions,
  }),

  getters: {
    /** 현재 테마 설정 정보 */
    currentTheme: state => THEMES[state.themeName],

    /** 실제 표시되는 테마 (프리뷰 모드 고려) */
    displayTheme: (state) => {
      const name = state.isPreviewMode && state.previewThemeName
        ? state.previewThemeName
        : state.themeName
      return THEMES[name]
    },

    /** 선택 UI용 테마 목록 (다크/라이트 그룹핑) */
    availableThemes: () => Object.entries(THEMES).map(([key, value]) => ({
      value: key as ThemeName,
      label: value.name,
      description: value.description,
      prefersDark: value.prefersDark,
      tags: value.tags,
      accentColors: value.accentColors,
    })),

    /** 다크 테마 목록 */
    darkThemes: () => Object.entries(THEMES)
      .filter(([_, v]) => v.prefersDark)
      .map(([key, value]) => ({
        value: key as ThemeName,
        label: value.name,
        accentColors: value.accentColors,
      })),

    /** 라이트 테마 목록 */
    lightThemes: () => Object.entries(THEMES)
      .filter(([_, v]) => !v.prefersDark)
      .map(([key, value]) => ({
        value: key as ThemeName,
        label: value.name,
        accentColors: value.accentColors,
      })),

    /** 줄임 모션 활성 여부 (시스템 설정 + 사용자 설정) */
    shouldReduceMotion: (state) => {
      return state.accessibility.reducedMotion
        || window.matchMedia('(prefers-reduced-motion: reduce)').matches
    },
  },

  actions: {
    // ─────────────────────────────────────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 테마 스토어 초기화
     */
    init() {
      if (this.isInitialized) return

      // 🆕 레거시 localStorage에서 마이그레이션 (pinia-plugin-persistedstate가 자동 복원 후 호출됨)
      this.migrateLegacyStorage()

      // 접근성 설정 적용
      this.applyAccessibility()

      // 테마 적용
      this.applyTheme()

      // Primary 색상 적용
      this.applyPrimaryColor()

      // Surface 색상 적용
      this.applySurfaceColor()

      // 시스템 테마 변경 감지
      this.watchSystemTheme()

      // 시스템 고대비 설정 감지
      this.watchPrefersContrast()

      // 스케줄 활성화 시 타이머 시작
      if (this.schedule.enabled) {
        this.startScheduleTimer()
      }

      // 개발 모드에서 테마 접근성 검증 실행
      if (import.meta.dev) {
        runThemeValidation()
      }

      this.isInitialized = true
    },

    /**
     * 레거시 localStorage에서 신규 형식으로 마이그레이션
     * - pinia-plugin-persistedstate가 자동으로 상태를 복원하므로
     *   레거시 키가 있는 경우에만 마이그레이션 수행
     */
    migrateLegacyStorage() {
      // 이미 신규 형식이 있으면 마이그레이션 불필요
      if (localStorage.getItem(PERSIST_KEY)) {
        return
      }

      let needsMigration = false

      // 레거시 테마 이름
      const savedTheme = localStorage.getItem(LEGACY_STORAGE_KEYS.themeName) as ThemeName | null
      if (savedTheme && savedTheme in THEMES) {
        this.themeName = savedTheme
        needsMigration = true
      }

      // 레거시 테마 모드
      const savedMode = localStorage.getItem(LEGACY_STORAGE_KEYS.themeMode) as ThemeMode | null
      if (savedMode && ['system', 'dark', 'light'].includes(savedMode)) {
        this.themeMode = savedMode
        needsMigration = true
      }

      // 레거시 스케줄 설정
      const savedSchedule = localStorage.getItem(LEGACY_STORAGE_KEYS.schedule)
      if (savedSchedule) {
        try {
          const parsed = JSON.parse(savedSchedule)
          this.schedule = { ...DEFAULT_SCHEDULE, ...parsed }
          needsMigration = true
        }
        catch { /* ignore */ }
      }

      // 레거시 접근성 설정
      const savedA11y = localStorage.getItem(LEGACY_STORAGE_KEYS.accessibility)
      if (savedA11y) {
        try {
          const parsed = JSON.parse(savedA11y)
          this.accessibility = { ...DEFAULT_ACCESSIBILITY, ...parsed }
          needsMigration = true
        }
        catch { /* ignore */ }
      }

      // 마이그레이션 완료 후 레거시 키 정리
      if (needsMigration) {
        Object.values(LEGACY_STORAGE_KEYS).forEach(key => localStorage.removeItem(key))
        if (import.meta.dev) {
          console.log('[Theme] Migrated from legacy storage keys to persistedstate')
        }
      }
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 테마 변경
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 테마 변경
     * @param name - 테마 이름
     * @param event - 클릭 이벤트 (View Transition 애니메이션 시작점 계산용)
     */
    setTheme(name: ThemeName, event?: MouseEvent) {
      // 클릭 위치 저장 (방사형 확산 애니메이션 시작점)
      this.setTransitionOrigin(event)

      this.themeName = name
      // 🆕 pinia-plugin-persistedstate가 자동으로 localStorage 동기화

      const theme = THEMES[name]

      // 테마 선택 시 해당 테마의 prefersDark에 따라 다크모드 자동 설정
      if (this.themeMode !== 'system') {
        this.themeMode = theme.prefersDark ? 'dark' : 'light'
      }

      // 🆕 테마 변경 시 해당 테마의 기본 Primary/Surface 색상으로 리셋
      this.primaryColor = theme.defaultPrimaryColor
      this.surfaceColor = theme.defaultSurfaceColor

      this.applyThemeWithTransition()

      // Primary/Surface 색상 적용
      this.applyPrimaryColor()
      this.applySurfaceColor()
    },

    /**
     * View Transition 애니메이션 시작점 설정
     * @param event - 마우스 이벤트 (클릭 위치)
     */
    setTransitionOrigin(event?: MouseEvent) {
      if (!event) {
        // 이벤트 없으면 화면 중앙에서 시작
        document.documentElement.style.setProperty('--theme-toggle-x', '50%')
        document.documentElement.style.setProperty('--theme-toggle-y', '50%')
        return
      }

      const x = (event.clientX / window.innerWidth) * 100
      const y = (event.clientY / window.innerHeight) * 100
      document.documentElement.style.setProperty('--theme-toggle-x', `${x}%`)
      document.documentElement.style.setProperty('--theme-toggle-y', `${y}%`)
    },

    /**
     * 다크/라이트/시스템 모드 변경
     */
    setMode(mode: ThemeMode) {
      this.themeMode = mode
      // 🆕 pinia-plugin-persistedstate가 자동으로 localStorage 동기화
      this.applyThemeWithTransition()
    },

    /**
     * 다크모드 토글
     */
    toggleDarkMode() {
      this.setMode(this.isDark ? 'light' : 'dark')
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 프리뷰 모드 (Phase 2 - Enhanced)
    // ─────────────────────────────────────────────────────────────────────────

    /** 프리뷰 디바운스 딜레이 (ms) */
    PREVIEW_DEBOUNCE_DELAY: 150,

    /**
     * 테마 프리뷰 시작 (hover 시) - 디바운스 적용
     * @param themeName 프리뷰할 테마 이름
     */
    startPreview(themeName: ThemeName) {
      // 이전 디바운스 타이머 취소
      if (this.previewDebounceTimer) {
        clearTimeout(this.previewDebounceTimer)
        this.previewDebounceTimer = null
      }

      // 에러 상태 초기화
      this.previewError = null

      // 디바운스 적용: 150ms 후 프리뷰 실행
      this.previewDebounceTimer = setTimeout(async () => {
        try {
          this.isPreviewLoading = true
          this.isPreviewMode = true
          this.previewThemeName = themeName
          await this.applyThemeInternal(themeName, false)
        }
        catch (error) {
          this.previewError = error instanceof Error
            ? error.message
            : '테마 프리뷰 로드 중 오류가 발생했습니다.'
          console.error('[ThemeStore] Preview error:', error)
        }
        finally {
          this.isPreviewLoading = false
        }
      }, this.PREVIEW_DEBOUNCE_DELAY)
    },

    /**
     * 테마 프리뷰 취소 (hover 종료 시)
     */
    async cancelPreview() {
      // 대기 중인 디바운스 타이머 취소
      if (this.previewDebounceTimer) {
        clearTimeout(this.previewDebounceTimer)
        this.previewDebounceTimer = null
      }

      if (!this.isPreviewMode) return

      try {
        this.isPreviewLoading = true
        this.isPreviewMode = false
        this.previewThemeName = null
        this.previewError = null
        await this.applyTheme()
      }
      catch (error) {
        console.error('[ThemeStore] Cancel preview error:', error)
      }
      finally {
        this.isPreviewLoading = false
      }
    },

    /**
     * 프리뷰 중인 테마 확정
     */
    confirmPreview() {
      // 대기 중인 디바운스 타이머 취소
      if (this.previewDebounceTimer) {
        clearTimeout(this.previewDebounceTimer)
        this.previewDebounceTimer = null
      }

      if (this.previewThemeName) {
        this.setTheme(this.previewThemeName)
      }
      this.isPreviewMode = false
      this.previewThemeName = null
      this.previewError = null
    },

    /**
     * 프리뷰 에러 초기화
     */
    clearPreviewError() {
      this.previewError = null
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 시간대별 자동 전환 (Phase 2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 스케줄 설정 업데이트
     */
    setSchedule(schedule: Partial<ThemeSchedule>) {
      this.schedule = { ...this.schedule, ...schedule }
      // 🆕 pinia-plugin-persistedstate가 자동으로 localStorage 동기화

      // 타이머 재시작
      if (this.schedule.enabled) {
        this.startScheduleTimer()
        this.checkSchedule() // 즉시 확인
      }
      else {
        this.stopScheduleTimer()
      }
    },

    /**
     * 스케줄 타이머 시작
     */
    startScheduleTimer() {
      this.stopScheduleTimer()
      // 1분마다 스케줄 확인
      this.scheduleTimerId = setInterval(() => {
        this.checkSchedule()
      }, 60 * 1000)
    },

    /**
     * 스케줄 타이머 중지
     */
    stopScheduleTimer() {
      if (this.scheduleTimerId) {
        clearInterval(this.scheduleTimerId)
        this.scheduleTimerId = null
      }
    },

    /**
     * 현재 시간에 따라 테마 자동 전환
     */
    checkSchedule() {
      if (!this.schedule.enabled) return

      const now = new Date()
      const currentMinutes = now.getHours() * 60 + now.getMinutes()

      const sunriseParts = this.schedule.sunriseTime.split(':').map(Number)
      const sunsetParts = this.schedule.sunsetTime.split(':').map(Number)

      const sunriseHour = sunriseParts[0] ?? 0
      const sunriseMin = sunriseParts[1] ?? 0
      const sunsetHour = sunsetParts[0] ?? 0
      const sunsetMin = sunsetParts[1] ?? 0

      const sunriseMinutes = sunriseHour * 60 + sunriseMin
      const sunsetMinutes = sunsetHour * 60 + sunsetMin

      // 현재 시간이 일출~일몰 사이면 라이트 테마
      const shouldBeLight = currentMinutes >= sunriseMinutes && currentMinutes < sunsetMinutes

      const targetTheme = shouldBeLight
        ? this.schedule.lightTheme
        : this.schedule.darkTheme

      if (this.themeName !== targetTheme) {
        this.setTheme(targetTheme)

        if (import.meta.dev) {
          console.log('[Theme Schedule]', {
            time: `${now.getHours()}:${now.getMinutes()}`,
            shouldBeLight,
            targetTheme,
          })
        }
      }
    },

    // ─────────────────────────────────────────────────────────────────────────
    // Primary Color 변경 (PrimeVue Palette)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Primary 색상 팔레트 변경
     * @param colorName - 색상 이름 (emerald, blue, indigo 등)
     */
    setPrimaryColor(colorName: PrimaryColorName) {
      this.primaryColor = colorName
      this.applyPrimaryColor()
    },

    /**
     * Primary 색상 팔레트 적용
     * PrimeVue의 updatePrimaryPalette API를 사용하여 동적으로 색상 변경
     * OKLCH CSS 변수도 함께 업데이트하여 RealGrid 등 외부 컴포넌트에 반영
     */
    applyPrimaryColor() {
      const palette = COLOR_PALETTES[this.primaryColor]
      if (!palette) return

      // 🆕 OKLCH CSS 변수 업데이트 (RealGrid 등 외부 컴포넌트용)
      const html = document.documentElement
      html.style.setProperty('--oklch-primary-h', String(palette.oklch.h))
      html.style.setProperty('--oklch-primary-c', String(palette.oklch.c))

      // PrimeVue palette() 함수를 사용하여 팔레트 생성
      // token 값을 기반으로 PrimeVue의 내장 팔레트 사용
      const paletteToken = palette.token

      // PrimeVue Aura 테마의 색상 팔레트 정의
      const colorPalettes: Record<string, PaletteDesignToken> = {
        emerald: { 50: '#ecfdf5', 100: '#d1fae5', 200: '#a7f3d0', 300: '#6ee7b7', 400: '#34d399', 500: '#10b981', 600: '#059669', 700: '#047857', 800: '#065f46', 900: '#064e3b', 950: '#022c22' },
        green: { 50: '#f0fdf4', 100: '#dcfce7', 200: '#bbf7d0', 300: '#86efac', 400: '#4ade80', 500: '#22c55e', 600: '#16a34a', 700: '#15803d', 800: '#166534', 900: '#14532d', 950: '#052e16' },
        lime: { 50: '#f7fee7', 100: '#ecfccb', 200: '#d9f99d', 300: '#bef264', 400: '#a3e635', 500: '#84cc16', 600: '#65a30d', 700: '#4d7c0f', 800: '#3f6212', 900: '#365314', 950: '#1a2e05' },
        orange: { 50: '#fff7ed', 100: '#ffedd5', 200: '#fed7aa', 300: '#fdba74', 400: '#fb923c', 500: '#f97316', 600: '#ea580c', 700: '#c2410c', 800: '#9a3412', 900: '#7c2d12', 950: '#431407' },
        amber: { 50: '#fffbeb', 100: '#fef3c7', 200: '#fde68a', 300: '#fcd34d', 400: '#fbbf24', 500: '#f59e0b', 600: '#d97706', 700: '#b45309', 800: '#92400e', 900: '#78350f', 950: '#451a03' },
        yellow: { 50: '#fefce8', 100: '#fef9c3', 200: '#fef08a', 300: '#fde047', 400: '#facc15', 500: '#eab308', 600: '#ca8a04', 700: '#a16207', 800: '#854d0e', 900: '#713f12', 950: '#422006' },
        teal: { 50: '#f0fdfa', 100: '#ccfbf1', 200: '#99f6e4', 300: '#5eead4', 400: '#2dd4bf', 500: '#14b8a6', 600: '#0d9488', 700: '#0f766e', 800: '#115e59', 900: '#134e4a', 950: '#042f2e' },
        cyan: { 50: '#ecfeff', 100: '#cffafe', 200: '#a5f3fc', 300: '#67e8f9', 400: '#22d3ee', 500: '#06b6d4', 600: '#0891b2', 700: '#0e7490', 800: '#155e75', 900: '#164e63', 950: '#083344' },
        sky: { 50: '#f0f9ff', 100: '#e0f2fe', 200: '#bae6fd', 300: '#7dd3fc', 400: '#38bdf8', 500: '#0ea5e9', 600: '#0284c7', 700: '#0369a1', 800: '#075985', 900: '#0c4a6e', 950: '#082f49' },
        blue: { 50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd', 400: '#60a5fa', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8', 800: '#1e40af', 900: '#1e3a8a', 950: '#172554' },
        indigo: { 50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc', 400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca', 800: '#3730a3', 900: '#312e81', 950: '#1e1b4b' },
        violet: { 50: '#f5f3ff', 100: '#ede9fe', 200: '#ddd6fe', 300: '#c4b5fd', 400: '#a78bfa', 500: '#8b5cf6', 600: '#7c3aed', 700: '#6d28d9', 800: '#5b21b6', 900: '#4c1d95', 950: '#2e1065' },
        purple: { 50: '#faf5ff', 100: '#f3e8ff', 200: '#e9d5ff', 300: '#d8b4fe', 400: '#c084fc', 500: '#a855f7', 600: '#9333ea', 700: '#7e22ce', 800: '#6b21a8', 900: '#581c87', 950: '#3b0764' },
        fuchsia: { 50: '#fdf4ff', 100: '#fae8ff', 200: '#f5d0fe', 300: '#f0abfc', 400: '#e879f9', 500: '#d946ef', 600: '#c026d3', 700: '#a21caf', 800: '#86198f', 900: '#701a75', 950: '#4a044e' },
        pink: { 50: '#fdf2f8', 100: '#fce7f3', 200: '#fbcfe8', 300: '#f9a8d4', 400: '#f472b6', 500: '#ec4899', 600: '#db2777', 700: '#be185d', 800: '#9d174d', 900: '#831843', 950: '#500724' },
        rose: { 50: '#fff1f2', 100: '#ffe4e6', 200: '#fecdd3', 300: '#fda4af', 400: '#fb7185', 500: '#f43f5e', 600: '#e11d48', 700: '#be123c', 800: '#9f1239', 900: '#881337', 950: '#4c0519' },
        slate: { 50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1', 400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155', 800: '#1e293b', 900: '#0f172a', 950: '#020617' },
        zinc: { 50: '#fafafa', 100: '#f4f4f5', 200: '#e4e4e7', 300: '#d4d4d8', 400: '#a1a1aa', 500: '#71717a', 600: '#52525b', 700: '#3f3f46', 800: '#27272a', 900: '#18181b', 950: '#09090b' },
      }

      const selectedPalette = colorPalettes[paletteToken] ?? colorPalettes.indigo
      if (selectedPalette) {
        updatePrimaryPalette(selectedPalette)
      }

      if (import.meta.dev && selectedPalette) {
        console.log('[Theme] Primary color changed:', {
          colorName: this.primaryColor,
          token: paletteToken,
          sample500: selectedPalette[500],
          oklch: palette.oklch,
        })
      }
    },

    // ─────────────────────────────────────────────────────────────────────────
    // Surface Color 변경 (PrimeVue Palette)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Surface 색상 팔레트 변경
     * @param colorName - 색상 이름 (slate, gray, zinc, neutral, stone)
     */
    setSurfaceColor(colorName: SurfaceColorName) {
      this.surfaceColor = colorName
      this.applySurfaceColor()
    },

    /**
     * Surface 색상 팔레트 적용
     * PrimeVue의 updateSurfacePalette API를 사용하여 동적으로 색상 변경
     * OKLCH CSS 변수도 함께 업데이트하여 확장성 확보
     */
    applySurfaceColor() {
      const palette = SURFACE_PALETTES[this.surfaceColor]
      if (!palette) return

      // 🆕 OKLCH CSS 변수 업데이트 (향후 확장용)
      const html = document.documentElement
      html.style.setProperty('--oklch-surface-h', String(palette.oklch.h))
      html.style.setProperty('--oklch-surface-c', String(palette.oklch.c))

      // PrimeVue Surface 색상 팔레트 정의 (Tailwind CSS 기준)
      const surfacePalettes: Record<string, PaletteDesignToken> = {
        slate: { 0: '#ffffff', 50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1', 400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155', 800: '#1e293b', 900: '#0f172a', 950: '#020617' },
        gray: { 0: '#ffffff', 50: '#f9fafb', 100: '#f3f4f6', 200: '#e5e7eb', 300: '#d1d5db', 400: '#9ca3af', 500: '#6b7280', 600: '#4b5563', 700: '#374151', 800: '#1f2937', 900: '#111827', 950: '#030712' },
        zinc: { 0: '#ffffff', 50: '#fafafa', 100: '#f4f4f5', 200: '#e4e4e7', 300: '#d4d4d8', 400: '#a1a1aa', 500: '#71717a', 600: '#52525b', 700: '#3f3f46', 800: '#27272a', 900: '#18181b', 950: '#09090b' },
        neutral: { 0: '#ffffff', 50: '#fafafa', 100: '#f5f5f5', 200: '#e5e5e5', 300: '#d4d4d4', 400: '#a3a3a3', 500: '#737373', 600: '#525252', 700: '#404040', 800: '#262626', 900: '#171717', 950: '#0a0a0a' },
        stone: { 0: '#ffffff', 50: '#fafaf9', 100: '#f5f5f4', 200: '#e7e5e4', 300: '#d6d3d1', 400: '#a8a29e', 500: '#78716c', 600: '#57534e', 700: '#44403c', 800: '#292524', 900: '#1c1917', 950: '#0c0a09' },
      }

      const selectedPalette = surfacePalettes[this.surfaceColor] ?? surfacePalettes.slate
      if (selectedPalette) {
        updateSurfacePalette(selectedPalette)
      }

      if (import.meta.dev && selectedPalette) {
        console.log('[Theme] Surface color changed:', {
          colorName: this.surfaceColor,
          sample500: selectedPalette[500],
          oklch: palette.oklch,
        })
      }
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 접근성 옵션 (Phase 2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 접근성 설정 업데이트
     */
    setAccessibility(options: Partial<AccessibilityOptions>) {
      this.accessibility = { ...this.accessibility, ...options }
      // 🆕 pinia-plugin-persistedstate가 자동으로 localStorage 동기화
      this.applyAccessibility()
    },

    /**
     * 접근성 설정 적용
     */
    applyAccessibility() {
      const html = document.documentElement

      // 고대비 모드
      html.classList.toggle('high-contrast', this.accessibility.highContrast)

      // 줄임 모션 (CSS 변수로 제어)
      html.classList.toggle('reduce-motion', this.accessibility.reducedMotion)
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 테마 적용 (핵심 로직)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * View Transition API를 활용한 부드러운 테마 전환
     */
    async applyThemeWithTransition() {
      // View Transition API 지원 확인
      if (!document.startViewTransition) {
        await this.applyTheme()
        return
      }

      // 줄임 모션 선호 시 즉시 적용
      if (this.shouldReduceMotion) {
        await this.applyTheme()
        return
      }

      try {
        const transition = document.startViewTransition(async () => {
          await this.applyTheme()
        })
        await transition.ready
      }
      catch {
        await this.applyTheme()
      }
    },

    /**
     * 테마 적용 (현재 테마)
     */
    async applyTheme() {
      await this.applyThemeInternal(this.themeName, true)
    },

    /**
     * 테마 적용 내부 로직 (🆕 동적 프리셋 로딩)
     * @param themeName - 적용할 테마 이름
     * @param updateDarkMode - 다크모드 상태 업데이트 여부
     */
    async applyThemeInternal(themeName: ThemeName, updateDarkMode: boolean) {
      // 1. 다크모드 결정
      if (updateDarkMode) {
        if (this.themeMode === 'system') {
          this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        }
        else {
          this.isDark = this.themeMode === 'dark'
        }
      }

      // 2. HTML 클래스 업데이트 (즉시 적용 - FOUC 방지)
      const html = document.documentElement
      const theme = THEMES[themeName]

      html.classList.remove(...ALL_THEME_CLASSES)
      html.classList.add(theme.className)

      // 🆕 하이브리드 FOUC 방지: app-dark/app-light 클래스로 color-scheme 강제
      // - 'system' 모드: 현재 시스템 설정에 따라 클래스 추가 (PrimeVue 호환)
      // - 'dark'/'light' 모드: 해당 클래스 추가
      html.classList.remove('app-dark', 'app-light')
      if (this.isDark) {
        html.classList.add('app-dark')
      }
      else {
        html.classList.add('app-light')
      }

      // 3. PrimeVue 프리셋 동적 로드 및 적용
      try {
        const preset = await loadPreset(themeName) as Preset
        usePreset(preset)

        // 4. 🆕 CSS 변수 → PrimeVue 팔레트 동기화
        // CSS 변수가 적용된 후 팔레트 생성 (requestAnimationFrame으로 레이아웃 완료 대기)
        requestAnimationFrame(() => {
          this.syncPaletteFromCssVars()
        })
      }
      catch (error) {
        console.error('[Theme] Failed to load preset:', themeName, error)
      }

      // 디버깅용 로그
      if (import.meta.dev) {
        console.log('[Theme]', {
          themeName,
          themeMode: this.themeMode,
          isDark: this.isDark,
          isPreview: this.isPreviewMode,
          cached: presetCache.has(themeName),
        })
      }
    },

    /**
     * 🆕 CSS 변수에서 PrimeVue 팔레트 동기화
     * OKLCH 색상 변수를 읽어 PrimeVue의 Primary/Surface 팔레트 업데이트
     */
    syncPaletteFromCssVars() {
      try {
        // Primary 팔레트 동기화
        const primaryPalette = getPrimaryPaletteFromCssVars()
        if (primaryPalette) {
          updatePrimaryPalette(primaryPalette)

          if (import.meta.dev) {
            console.log('[Theme] Primary palette synced:', {
              sample500: primaryPalette[500],
              isDark: this.isDark,
            })
          }
        }

        // Surface 팔레트 동기화
        const surfacePalette = getSurfacePaletteFromCssVars()
        if (surfacePalette) {
          // Surface 팔레트 업데이트 (현재 모드에 맞는 팔레트가 이미 생성됨)
          updateSurfacePalette(surfacePalette as PaletteDesignToken)

          if (import.meta.dev) {
            console.log('[Theme] Surface palette synced:', {
              sample500: surfacePalette[500],
              isDark: this.isDark,
            })
          }
        }
      }
      catch (error) {
        console.error('[Theme] Failed to sync palette from CSS vars:', error)
      }
    },

    /**
     * 시스템 테마 변경 감지
     */
    watchSystemTheme() {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', () => {
        if (this.themeMode === 'system') {
          this.applyThemeWithTransition()
        }
      })
    },

    /**
     * 시스템 고대비(prefers-contrast) 설정 감지
     * 사용자가 명시적으로 설정하지 않은 경우에만 시스템 설정을 따름
     */
    watchPrefersContrast() {
      const mediaQuery = window.matchMedia('(prefers-contrast: more)')

      // 🆕 persistedstate 사용: 저장된 데이터 확인
      const savedData = localStorage.getItem(PERSIST_KEY)
      const hasSavedAccessibility = savedData ? JSON.parse(savedData).accessibility !== undefined : false

      // 초기 로드 시 시스템 설정 반영 (저장된 접근성 설정이 없는 경우)
      if (!hasSavedAccessibility && mediaQuery.matches) {
        this.setAccessibility({ highContrast: true })

        if (import.meta.dev) {
          console.log('[Theme] System prefers-contrast: more detected, enabling high contrast')
        }
      }

      // 시스템 설정 변경 감지
      mediaQuery.addEventListener('change', (e) => {
        // 사용자가 명시적으로 설정하지 않은 경우에만 자동 적용
        const currentSaved = localStorage.getItem(PERSIST_KEY)
        const wasExplicitlySet = currentSaved ? JSON.parse(currentSaved).accessibility !== undefined : false

        if (!wasExplicitlySet || confirm('시스템 고대비 설정이 변경되었습니다. 적용하시겠습니까?')) {
          this.setAccessibility({ highContrast: e.matches })

          if (import.meta.dev) {
            console.log('[Theme] System prefers-contrast changed:', e.matches ? 'more' : 'no-preference')
          }
        }
      })
    },

    // ─────────────────────────────────────────────────────────────────────────
    // 테마 내보내기/가져오기 (Phase 3-2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 현재 테마 설정을 JSON으로 내보내기
     * @returns 내보내기 데이터 객체
     */
    exportSettings(): ThemeExportData {
      return {
        version: 1,
        exportedAt: new Date().toISOString(),
        settings: {
          themeName: this.themeName,
          themeMode: this.themeMode,
          schedule: { ...this.schedule },
          accessibility: { ...this.accessibility },
        },
      }
    },

    /**
     * 테마 설정을 JSON 파일로 다운로드
     * @param filename - 파일명 (기본값: theme-settings.json)
     */
    downloadSettings(filename = 'theme-settings.json') {
      const data = this.exportSettings()
      const json = JSON.stringify(data, null, 2)
      const blob = new Blob([json], { type: 'application/json' })
      const url = URL.createObjectURL(blob)

      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    },

    /**
     * JSON 데이터에서 테마 설정 가져오기
     * @param data - 가져올 데이터 (파싱된 JSON 또는 문자열)
     * @returns 성공 여부와 메시지
     */
    importSettings(data: ThemeExportData | string): { success: boolean, message: string } {
      try {
        // 문자열인 경우 파싱
        const parsed: ThemeExportData = typeof data === 'string' ? JSON.parse(data) : data

        // 버전 확인
        if (parsed.version !== 1) {
          return { success: false, message: `지원하지 않는 버전입니다: ${parsed.version}` }
        }

        // 필수 필드 확인
        if (!parsed.settings) {
          return { success: false, message: '설정 데이터가 없습니다.' }
        }

        const { settings } = parsed

        // 테마 이름 유효성 검사
        if (settings.themeName && !(settings.themeName in THEMES)) {
          return { success: false, message: `알 수 없는 테마입니다: ${settings.themeName}` }
        }

        // 테마 모드 유효성 검사
        if (settings.themeMode && !['system', 'dark', 'light'].includes(settings.themeMode)) {
          return { success: false, message: `잘못된 테마 모드입니다: ${settings.themeMode}` }
        }

        // 설정 적용
        // 🆕 pinia-plugin-persistedstate가 상태 변경 시 자동 localStorage 동기화
        if (settings.themeName) {
          this.themeName = settings.themeName
        }

        if (settings.themeMode) {
          this.themeMode = settings.themeMode
        }

        if (settings.schedule) {
          this.setSchedule(settings.schedule)
        }

        if (settings.accessibility) {
          this.setAccessibility(settings.accessibility)
        }

        // 테마 적용
        this.applyThemeWithTransition()

        return { success: true, message: '테마 설정을 성공적으로 가져왔습니다.' }
      }
      catch (error) {
        const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류'
        return { success: false, message: `가져오기 실패: ${errorMessage}` }
      }
    },

    /**
     * 파일에서 테마 설정 가져오기 (파일 선택 다이얼로그)
     * @returns Promise<성공 여부와 메시지>
     */
    async importFromFile(): Promise<{ success: boolean, message: string }> {
      return new Promise((resolve) => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = '.json,application/json'

        input.onchange = async (event) => {
          const file = (event.target as HTMLInputElement).files?.[0]
          if (!file) {
            resolve({ success: false, message: '파일이 선택되지 않았습니다.' })
            return
          }

          try {
            const text = await file.text()
            const result = this.importSettings(text)
            resolve(result)
          }
          catch (error) {
            const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류'
            resolve({ success: false, message: `파일 읽기 실패: ${errorMessage}` })
          }
        }

        input.oncancel = () => {
          resolve({ success: false, message: '파일 선택이 취소되었습니다.' })
        }

        input.click()
      })
    },

    /**
     * 모든 테마 설정 초기화
     */
    resetAllSettings() {
      // 🆕 persistedstate는 자동으로 localStorage 동기화
      // 레거시 키 정리 (마이그레이션 완료 후 제거 가능)
      Object.values(LEGACY_STORAGE_KEYS).forEach(key => localStorage.removeItem(key))
      localStorage.removeItem(PERSIST_KEY)

      // 상태 초기화
      this.themeName = 'linear-dark'
      this.themeMode = 'system'
      this.primaryColor = 'indigo'
      this.surfaceColor = 'slate'
      this.schedule = { ...DEFAULT_SCHEDULE }
      this.accessibility = { ...DEFAULT_ACCESSIBILITY }

      // 타이머 정리
      this.stopScheduleTimer()

      // 적용
      this.applyAccessibility()
      this.applyThemeWithTransition()
      this.applyPrimaryColor()
      this.applySurfaceColor()
    },
  },

  // ============================================================================
  // 🆕 Pinia Persistedstate 설정
  // ============================================================================
  persist: {
    key: PERSIST_KEY,
    storage: typeof window !== 'undefined' ? localStorage : undefined,
    pick: ['themeName', 'themeMode', 'primaryColor', 'surfaceColor', 'schedule', 'accessibility'],
    // afterRestore: 레거시 마이그레이션 및 초기 테마 적용은 init()에서 처리
  },
})
