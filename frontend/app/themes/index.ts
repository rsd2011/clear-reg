export { LinearDarkPreset, LINEAR_DARK_META, LINEAR_DARK_OKLCH } from './linear-dark'
export { KoscomLightPreset, KOSCOM_LIGHT_META, KOSCOM_LIGHT_OKLCH } from './koscom-light'
export { GithubDarkPreset, GITHUB_DARK_META, GITHUB_DARK_OKLCH } from './github-dark'
export { NotionLightPreset, NOTION_LIGHT_META, NOTION_LIGHT_OKLCH } from './notion-light'
export { FigmaDarkPreset, FIGMA_DARK_META, FIGMA_DARK_OKLCH } from './figma-dark'
export { SlackAuberginePreset, SLACK_AUBERGINE_META, SLACK_AUBERGINE_OKLCH } from './slack-aubergine'

// Note: DockView/RealGrid 커스텀 테마 제거됨 - 공식 다크/라이트 테마만 사용
// @see plugins/dockview-theme.client.ts - Dockview 공식 테마 전환
// @see plugins/realgrid-theme.client.ts - RealGrid 공식 다크 CSS 로드

// ============================================================================
// Theme Types
// ============================================================================

export type ThemeName
  = | 'linear-dark'
    | 'github-dark'
    | 'figma-dark'
    | 'slack-aubergine'
    | 'koscom-light'
    | 'notion-light'

export type ThemeMode = 'system' | 'dark' | 'light'

export type ThemeTag
  = | 'modern'
    | 'minimal'
    | 'corporate'
    | 'developer'
    | 'warm'
    | 'cool'
    | 'vibrant'
    | 'muted'

export type FontStyle = 'sans' | 'serif' | 'mono'

// ============================================================================
// OKLCH Types (CSS Color Level 4)
// ============================================================================

/**
 * OKLCH 색상 공간의 Primary 색상 정의
 * @see https://oklch.com/
 */
export interface OklchPrimary {
  /** Hue (색상): 0-360도 */
  h: number
  /** Chroma (채도): 0-0.4 */
  c: number
  /** Lightness (밝기): 0-1 */
  l: number
}

/**
 * 12단계 색상 스케일 (Radix UI 패턴)
 *
 * 단계별 용도:
 * - 1-2: 배경 (App background, Subtle background)
 * - 3-5: UI 요소 (Element bg, Hovered, Active)
 * - 6-8: 테두리 (Subtle border, Border, Hovered border)
 * - 9-10: 솔리드 (Solid bg, Hovered solid bg)
 * - 11-12: 텍스트 (Low contrast, High contrast)
 */
export interface OklchScale12 {
  1: string
  2: string
  3: string
  4: string
  5: string
  6: string
  7: string
  8: string
  9: string // 메인 브랜드 색상
  10: string
  11: string
  12: string
}

/**
 * 테마별 OKLCH 색상 정의
 */
export interface ThemeOklch {
  primary: OklchPrimary
  scale: OklchScale12
}

// ============================================================================
// Theme Configuration Interface
// ============================================================================

export interface ThemeConfig {
  /** 테마 표시 이름 */
  name: string
  /** 테마 설명 */
  description: string
  /** HTML에 적용되는 클래스명 */
  className: string
  /** 다크모드 선호 여부 */
  prefersDark: boolean
  /** 테마 제작자/출처 */
  author: string
  /** 테마 버전 */
  version: string
  /** 테마 분류 태그 */
  tags: ThemeTag[]
  /** 대표 색상 (프리뷰용, 3개) */
  accentColors: [string, string, string]
  /** 권장 폰트 스타일 */
  fontStyle: FontStyle
  /** 테마 기본 primary 색상 (테마 변경 시 적용) */
  defaultPrimaryColor: PrimaryColorName
  /** 테마 기본 surface 색상 (테마 변경 시 적용) */
  defaultSurfaceColor: SurfaceColorName
}

// ============================================================================
// Theme Registry (다크/라이트 그룹핑 순서)
// ============================================================================

export const THEMES: Record<ThemeName, ThemeConfig> = {
  // ─────────────────────────────────────────────────────────────────────────
  // Dark Themes
  // ─────────────────────────────────────────────────────────────────────────
  'linear-dark': {
    name: 'Linear Dark',
    description: 'Linear.app 스타일 다크 테마',
    className: 'theme-linear-dark',
    prefersDark: true,
    author: 'Linear',
    version: '1.0.0',
    tags: ['modern', 'minimal', 'developer'],
    accentColors: ['#5e6ad2', '#232326', '#8795e1'],
    fontStyle: 'sans',
    defaultPrimaryColor: 'indigo',
    defaultSurfaceColor: 'slate',
  },
  'github-dark': {
    name: 'GitHub Dark',
    description: 'GitHub Primer 다크 테마',
    className: 'theme-github-dark',
    prefersDark: true,
    author: 'GitHub',
    version: '1.0.0',
    tags: ['developer', 'modern', 'cool'],
    accentColors: ['#58a6ff', '#0d1117', '#238636'],
    fontStyle: 'mono',
    defaultPrimaryColor: 'sky',
    defaultSurfaceColor: 'slate',
  },
  'figma-dark': {
    name: 'Figma Dark',
    description: 'Figma 스타일 다크 테마',
    className: 'theme-figma-dark',
    prefersDark: true,
    author: 'Figma',
    version: '1.0.0',
    tags: ['modern', 'minimal', 'vibrant'],
    accentColors: ['#0d99ff', '#1e1e1e', '#14ae5c'],
    fontStyle: 'sans',
    defaultPrimaryColor: 'sky',
    defaultSurfaceColor: 'zinc',
  },
  'slack-aubergine': {
    name: 'Slack Aubergine',
    description: 'Slack 클래식 Aubergine 다크 테마',
    className: 'theme-slack-aubergine',
    prefersDark: true,
    author: 'Slack',
    version: '1.0.0',
    tags: ['corporate', 'warm', 'vibrant'],
    accentColors: ['#4a154b', '#1a1d21', '#2eb67d'],
    fontStyle: 'sans',
    defaultPrimaryColor: 'purple',
    defaultSurfaceColor: 'neutral',
  },

  // ─────────────────────────────────────────────────────────────────────────
  // Light Themes
  // ─────────────────────────────────────────────────────────────────────────
  'koscom-light': {
    name: 'Koscom Light',
    description: 'Koscom 스타일 라이트 테마',
    className: 'theme-koscom-light',
    prefersDark: false,
    author: 'Koscom',
    version: '1.0.0',
    tags: ['corporate', 'warm', 'muted'],
    accentColors: ['#f06e1e', '#ffffff', '#68cc58'],
    fontStyle: 'sans',
    defaultPrimaryColor: 'orange',
    defaultSurfaceColor: 'stone',
  },
  'notion-light': {
    name: 'Notion Light',
    description: 'Notion 스타일 미니멀 라이트 테마',
    className: 'theme-notion-light',
    prefersDark: false,
    author: 'Notion',
    version: '1.0.0',
    tags: ['minimal', 'modern', 'muted'],
    accentColors: ['#2eaadc', '#ffffff', '#448361'],
    fontStyle: 'serif',
    defaultPrimaryColor: 'cyan',
    defaultSurfaceColor: 'neutral',
  },
} as const

// ============================================================================
// Color Palette (PrimeVue 스타일 색상 선택)
// ============================================================================

export type PrimaryColorName
  = | 'emerald'
    | 'green'
    | 'lime'
    | 'orange'
    | 'amber'
    | 'yellow'
    | 'teal'
    | 'cyan'
    | 'sky'
    | 'blue'
    | 'indigo'
    | 'violet'
    | 'purple'
    | 'fuchsia'
    | 'pink'
    | 'rose'
    | 'slate'
    | 'zinc'
    | 'noir'

export interface ColorPaletteOption {
  /** 색상 이름 */
  name: string
  /** 대표 색상 (500 레벨) */
  color: string
  /** PrimeVue 토큰 이름 */
  token: string
  /** OKLCH 색상 공간 값 (RealGrid 등 CSS 변수 연동용) */
  oklch: { h: number, c: number }
}

/**
 * 색상 팔레트 옵션 (PrimeVue Aura 기준)
 * updatePrimaryPalette에서 `{colorName.level}` 형태로 사용
 */
/**
 * 색상 팔레트 옵션 (PrimeVue Aura 기준)
 * updatePrimaryPalette에서 `{colorName.level}` 형태로 사용
 *
 * OKLCH 값 참고:
 * - h (Hue): 0-360도 색상환
 * - c (Chroma): 0-0.4 채도 (일반적으로 0.12-0.22)
 */
export const COLOR_PALETTES: Record<PrimaryColorName, ColorPaletteOption> = {
  emerald: { name: 'Emerald', color: '#10b981', token: 'emerald', oklch: { h: 165, c: 0.17 } },
  green: { name: 'Green', color: '#22c55e', token: 'green', oklch: { h: 145, c: 0.18 } },
  lime: { name: 'Lime', color: '#84cc16', token: 'lime', oklch: { h: 130, c: 0.18 } },
  orange: { name: 'Orange', color: '#f97316', token: 'orange', oklch: { h: 55, c: 0.19 } },
  amber: { name: 'Amber', color: '#f59e0b', token: 'amber', oklch: { h: 75, c: 0.17 } },
  yellow: { name: 'Yellow', color: '#eab308', token: 'yellow', oklch: { h: 95, c: 0.17 } },
  teal: { name: 'Teal', color: '#14b8a6', token: 'teal', oklch: { h: 175, c: 0.13 } },
  cyan: { name: 'Cyan', color: '#06b6d4', token: 'cyan', oklch: { h: 195, c: 0.14 } },
  sky: { name: 'Sky', color: '#0ea5e9', token: 'sky', oklch: { h: 220, c: 0.15 } },
  blue: { name: 'Blue', color: '#3b82f6', token: 'blue', oklch: { h: 260, c: 0.17 } },
  indigo: { name: 'Indigo', color: '#6366f1', token: 'indigo', oklch: { h: 265, c: 0.15 } },
  violet: { name: 'Violet', color: '#8b5cf6', token: 'violet', oklch: { h: 290, c: 0.17 } },
  purple: { name: 'Purple', color: '#a855f7', token: 'purple', oklch: { h: 300, c: 0.18 } },
  fuchsia: { name: 'Fuchsia', color: '#d946ef', token: 'fuchsia', oklch: { h: 320, c: 0.22 } },
  pink: { name: 'Pink', color: '#ec4899', token: 'pink', oklch: { h: 345, c: 0.18 } },
  rose: { name: 'Rose', color: '#f43f5e', token: 'rose', oklch: { h: 15, c: 0.18 } },
  slate: { name: 'Slate', color: '#64748b', token: 'slate', oklch: { h: 230, c: 0.03 } },
  zinc: { name: 'Zinc', color: '#71717a', token: 'zinc', oklch: { h: 0, c: 0.01 } },
  noir: { name: 'Noir', color: '#09090b', token: 'zinc', oklch: { h: 0, c: 0 } }, // Noir는 zinc 기반
}

/** 색상 팔레트 이름 목록 */
export const PRIMARY_COLOR_NAMES = Object.keys(COLOR_PALETTES) as PrimaryColorName[]

// ============================================================================
// Surface Color Palette (PrimeVue 스타일 중성 배경/테두리 색상)
// ============================================================================

export type SurfaceColorName
  = | 'slate'
    | 'gray'
    | 'zinc'
    | 'neutral'
    | 'stone'

export interface SurfacePaletteOption {
  /** 색상 이름 */
  name: string
  /** 대표 색상 (500 레벨) */
  color: string
  /** 설명 */
  description: string
  /** OKLCH 색상 공간 값 (RealGrid 등 CSS 변수 연동용) */
  oklch: { h: number, c: number }
}

/**
 * Surface 색상 팔레트 옵션
 * 배경, 테두리, 텍스트 등 중성 UI 요소에 사용
 *
 * OKLCH 값 참고:
 * - h (Hue): 색상 방향 (Slate는 푸른빛, Stone은 따뜻한 빛)
 * - c (Chroma): 채도 (Surface는 낮은 채도 사용)
 */
export const SURFACE_PALETTES: Record<SurfaceColorName, SurfacePaletteOption> = {
  slate: { name: 'Slate', color: '#64748b', description: '푸른빛 회색', oklch: { h: 215, c: 0.04 } },
  gray: { name: 'Gray', color: '#6b7280', description: '순수 회색', oklch: { h: 220, c: 0.02 } },
  zinc: { name: 'Zinc', color: '#71717a', description: '따뜻한 회색', oklch: { h: 240, c: 0.01 } },
  neutral: { name: 'Neutral', color: '#737373', description: '중립 회색', oklch: { h: 0, c: 0 } },
  stone: { name: 'Stone', color: '#78716c', description: '베이지 회색', oklch: { h: 35, c: 0.02 } },
}

/** Surface 색상 이름 목록 */
export const SURFACE_COLOR_NAMES = Object.keys(SURFACE_PALETTES) as SurfaceColorName[]

// ============================================================================
// Theme Utilities
// ============================================================================

/** 다크 테마 목록 */
export const DARK_THEMES = Object.entries(THEMES)
  .filter(([_, config]) => config.prefersDark)
  .map(([key]) => key as ThemeName)

/** 라이트 테마 목록 */
export const LIGHT_THEMES = Object.entries(THEMES)
  .filter(([_, config]) => !config.prefersDark)
  .map(([key]) => key as ThemeName)

/** 태그로 테마 필터링 */
export function getThemesByTag(tag: ThemeTag): ThemeName[] {
  return Object.entries(THEMES)
    .filter(([_, config]) => config.tags.includes(tag))
    .map(([key]) => key as ThemeName)
}
