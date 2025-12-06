import { defineStore } from 'pinia'
import { usePreset } from '@primeuix/themes'
import type { ThemeName, ThemeMode } from '~/themes'
import { THEMES } from '~/themes'
import { runThemeValidation } from '~/utils/theme-validator'

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
const PERSIST_KEY = 'enterman-theme'

/**
 * @deprecated pinia-plugin-persistedstate로 대체됨
 * 하위 호환성을 위해 마이그레이션 로직에서만 사용
 */
const LEGACY_STORAGE_KEYS = {
  themeName: 'enterman-theme-name',
  themeMode: 'enterman-theme-mode',
  schedule: 'enterman-theme-schedule',
  accessibility: 'enterman-theme-a11y',
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
    isDark: false,
    isInitialized: false,
    // Phase 2: 프리뷰 모드
    isPreviewMode: false,
    previewThemeName: null as ThemeName | null,
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
     * 🆕 레거시 localStorage에서 신규 형식으로 마이그레이션
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

      // 테마 선택 시 해당 테마의 prefersDark에 따라 다크모드 자동 설정
      if (this.themeMode !== 'system') {
        const theme = THEMES[name]
        this.themeMode = theme.prefersDark ? 'dark' : 'light'
      }

      this.applyThemeWithTransition()
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
    // 프리뷰 모드 (Phase 2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 테마 프리뷰 시작 (hover 시)
     */
    async startPreview(themeName: ThemeName) {
      this.isPreviewMode = true
      this.previewThemeName = themeName
      await this.applyThemeInternal(themeName, false)
    },

    /**
     * 테마 프리뷰 취소 (hover 종료 시)
     */
    async cancelPreview() {
      if (!this.isPreviewMode) return
      this.isPreviewMode = false
      this.previewThemeName = null
      await this.applyTheme()
    },

    /**
     * 프리뷰 중인 테마 확정
     */
    confirmPreview() {
      if (this.previewThemeName) {
        this.setTheme(this.previewThemeName)
      }
      this.isPreviewMode = false
      this.previewThemeName = null
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

      const [sunriseHour, sunriseMin] = this.schedule.sunriseTime.split(':').map(Number)
      const [sunsetHour, sunsetMin] = this.schedule.sunsetTime.split(':').map(Number)

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
      html.classList.toggle('app-dark', this.isDark)

      // 3. PrimeVue 프리셋 동적 로드 및 적용
      try {
        const preset = await loadPreset(themeName)
        usePreset(preset)
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
      this.schedule = { ...DEFAULT_SCHEDULE }
      this.accessibility = { ...DEFAULT_ACCESSIBILITY }

      // 타이머 정리
      this.stopScheduleTimer()

      // 적용
      this.applyAccessibility()
      this.applyThemeWithTransition()
    },
  },

  // ============================================================================
  // 🆕 Pinia Persistedstate 설정
  // ============================================================================
  persist: {
    key: PERSIST_KEY,
    storage: typeof window !== 'undefined' ? localStorage : undefined,
    pick: ['themeName', 'themeMode', 'schedule', 'accessibility'],
    // afterRestore: 레거시 마이그레이션 및 초기 테마 적용은 init()에서 처리
  },
})
