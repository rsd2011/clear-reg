import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * ELONSOFT Light Theme Preset
 * Based on ELONSOFT brand design system
 *
 * 금융 컴플라이언스 전문 기업의 신뢰감 있는 라이트 테마
 *
 * Primary Color: Navy Blue
 * - Hex: #182A88
 * - OKLCH: oklch(0.35 0.18 265)
 *
 * Accent Color: Orange
 * - Hex: #F39800
 * - OKLCH: oklch(0.75 0.18 70)
 *
 * @see https://www.elonsoft.co.kr/
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 * 라이트 테마: 밝은 배경에서 시작, 어두운 텍스트로 종료
 */
export const ELONSOFT_LIGHT_OKLCH = {
  primary: {
    h: 265, // Hue: Navy Blue
    c: 0.18, // Chroma: 선명한 채도 (브랜드 네이비)
    l: 0.35, // Lightness: 메인 색상 (어두운 네이비)
  },
  scale: {
    1: '#ffffff', // oklch(0.99 0 0) - 가장 밝은 배경
    2: '#fafafa', // oklch(0.98 0 0)
    3: '#f5f5f5', // oklch(0.96 0.005 265)
    4: '#eeeeee', // oklch(0.94 0.01 265)
    5: '#e0e0e0', // oklch(0.90 0.015 265)
    6: '#bdbdbd', // oklch(0.78 0.02 265)
    7: '#9e9e9e', // oklch(0.68 0.025 265)
    8: '#757575', // oklch(0.53 0.03 265)
    9: '#182A88', // oklch(0.35 0.18 265) - 메인 브랜드 색상
    10: '#14236d', // oklch(0.30 0.16 265)
    11: '#424242', // oklch(0.35 0.01 265) - 본문 텍스트
    12: '#302927', // oklch(0.28 0.02 50) - 가장 어두운 텍스트 (브랜드 텍스트)
  },
} as const

export const ElonsoftLightPreset = definePreset(Aura, {
  primitive: {
    green: {
      50: '#f0fdf4',
      100: '#dcfce7',
      200: '#bbf7d0',
      300: '#86efac',
      400: '#4ade80',
      500: '#22c55e',
      600: '#16a34a',
      700: '#15803d',
      800: '#166534',
      900: '#14532d',
      950: '#052e16',
    },
    blue: {
      50: '#eef1fa',
      100: '#d4dbf2',
      200: '#a9b7e5',
      300: '#7e93d8',
      400: '#536fcb',
      500: '#182A88',
      600: '#14236d',
      700: '#101c52',
      800: '#0c1537',
      900: '#080e1c',
      950: '#04070e',
    },
    orange: {
      50: '#fff8ed',
      100: '#ffeed4',
      200: '#ffd9a8',
      300: '#ffbf70',
      400: '#f9a035',
      500: '#F39800',
      600: '#d47f00',
      700: '#b06600',
      800: '#8c4e00',
      900: '#683800',
      950: '#3d2100',
    },
    red: {
      50: '#fef2f2',
      100: '#fee2e2',
      200: '#fecaca',
      300: '#fca5a5',
      400: '#f87171',
      500: '#ef4444',
      600: '#dc2626',
      700: '#b91c1c',
      800: '#991b1b',
      900: '#7f1d1d',
      950: '#450a0a',
    },
  },
  semantic: {
    primary: {
      50: '#eef1fa',
      100: '#d4dbf2',
      200: '#a9b7e5',
      300: '#7e93d8',
      400: '#536fcb',
      500: '#182A88',
      600: '#14236d',
      700: '#101c52',
      800: '#0c1537',
      900: '#080e1c',
      950: '#04070e',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#fafafa',
          100: '#f5f5f5',
          200: '#eeeeee',
          300: '#e0e0e0',
          400: '#bdbdbd',
          500: '#9e9e9e',
          600: '#757575',
          700: '#616161',
          800: '#424242',
          900: '#302927',
          950: '#1a1817',
        },
        // primary: 토큰 참조로 semantic.primary 팔레트와 연동
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}',
        },
        success: {
          color: '#22c55e',
          contrastColor: '#ffffff',
          hoverColor: '#4ade80',
          activeColor: '#16a34a',
        },
        warn: {
          color: '#F39800', // ELONSOFT Orange
          contrastColor: '#ffffff',
          hoverColor: '#f9a035',
          activeColor: '#d47f00',
        },
        danger: {
          color: '#ef4444',
          contrastColor: '#ffffff',
          hoverColor: '#f87171',
          activeColor: '#dc2626',
        },
        info: {
          color: '#3b82f6',
          contrastColor: '#ffffff',
          hoverColor: '#60a5fa',
          activeColor: '#2563eb',
        },
      },
    },
  },
  extend: {
    typography: {
      fontSans: '\'Pretendard\', \'Noto Sans KR\', -apple-system, BlinkMacSystemFont, \'Segoe UI\', sans-serif',
      fontMono: '\'JetBrains Mono\', \'Fira Code\', Consolas, monospace',
      micro: '0.75rem',
      mini: '0.8125rem',
      small: '0.875rem',
      regular: '0.9375rem',
      large: '1.125rem',
      title1: '1.25rem',
      title2: '1.5rem',
      title3: '1.875rem',
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '2px',
      },
    },
    card: {
      root: {
        borderRadius: '2px',
      },
    },
    inputtext: {
      root: {
        borderRadius: '2px',
      },
    },
    select: {
      root: {
        borderRadius: '2px',
      },
    },
    badge: {
      root: {
        borderRadius: '2px',
      },
    },
    tag: {
      colorScheme: {
        light: {
          success: { background: 'color-mix(in srgb, {green.500} 16%, transparent)', color: '{green.500}' },
          info: { background: 'color-mix(in srgb, {blue.500} 16%, transparent)', color: '{blue.500}' },
          warn: { background: 'color-mix(in srgb, {orange.500} 16%, transparent)', color: '{orange.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
        },
        dark: {
          success: { background: 'color-mix(in srgb, {green.500} 16%, transparent)', color: '{green.500}' },
          info: { background: 'color-mix(in srgb, {blue.500} 16%, transparent)', color: '{blue.500}' },
          warn: { background: 'color-mix(in srgb, {orange.500} 16%, transparent)', color: '{orange.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
        },
      },
    },
    dialog: {
      root: {
        borderRadius: '2px',
      },
    },
    panel: {
      root: {
        borderRadius: '2px',
      },
    },
    toast: {
      root: {
        borderRadius: '2px',
      },
    },
    tooltip: {
      root: {
        borderRadius: '2px',
      },
    },
  },
})

// 테마 메타데이터 (HTML 클래스명, 기본 다크모드 선호도)
export const ELONSOFT_LIGHT_META = {
  className: 'theme-elonsoft-light',
  prefersDark: false,
  oklch: ELONSOFT_LIGHT_OKLCH,
} as const

export default ElonsoftLightPreset
