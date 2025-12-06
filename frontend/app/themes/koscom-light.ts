import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Koscom Light Theme Preset
 * Based on Koscom.co.kr design system
 *
 * Primary Color: Orange
 * - Hex: #f06e1e
 * - OKLCH: oklch(0.68 0.20 55)
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 * 라이트 테마: 밝은 배경에서 시작, 어두운 텍스트로 종료
 */
export const KOSCOM_LIGHT_OKLCH = {
  primary: {
    h: 55, // Hue: Orange
    c: 0.20, // Chroma: 높은 채도 (Koscom 브랜드의 선명한 오렌지)
    l: 0.68, // Lightness: 메인 색상
  },
  scale: {
    1: '#ffffff', // oklch(0.99 0 0) - 가장 밝은 배경
    2: '#f6f6f6', // oklch(0.97 0 0)
    3: '#f5f5f5', // oklch(0.96 0.01 55)
    4: '#f0f0f0', // oklch(0.94 0.015 55)
    5: '#efefef', // oklch(0.93 0.02 55)
    6: '#d6d6d6', // oklch(0.87 0.025 55)
    7: '#aaaaaa', // oklch(0.72 0.03 55)
    8: '#797575', // oklch(0.55 0.04 55)
    9: '#f06e1e', // oklch(0.68 0.20 55) - 메인 브랜드 색상
    10: '#ea580c', // oklch(0.60 0.20 55)
    11: '#5d6060', // oklch(0.45 0.02 55) - 본문 텍스트
    12: '#3c3c3c', // oklch(0.32 0.01 55) - 가장 어두운 텍스트
  },
} as const
export const KoscomLightPreset = definePreset(Aura, {
  primitive: {
    green: {
      50: '#f0fdf0',
      100: '#dcfadc',
      200: '#a8f0a0',
      300: '#8ae680',
      400: '#78dc68',
      500: '#68cc58',
      600: '#5bb84d',
      700: '#4ea342',
      800: '#3d8034',
      900: '#2c5c26',
      950: '#1b3818',
    },
    blue: {
      50: '#f0f4ff',
      100: '#e0e8ff',
      200: '#b3c4ff',
      300: '#86a0ff',
      400: '#597cff',
      500: '#4368f2',
      600: '#3366cc',
      700: '#2952b8',
      800: '#1f3e8a',
      900: '#152a5c',
      950: '#0a152e',
    },
    orange: {
      50: '#fff7ed',
      100: '#ffedd5',
      200: '#fed7aa',
      300: '#fdba74',
      400: '#fb923c',
      500: '#f06e1e',
      600: '#ea580c',
      700: '#c2410c',
      800: '#9a3412',
      900: '#7c2d12',
      950: '#431407',
    },
    red: {
      50: '#fff5f5',
      100: '#ffe0e0',
      200: '#ffb3b3',
      300: '#ff8080',
      400: '#ff4d4d',
      500: '#ff2a2a',
      600: '#ff1e1e',
      700: '#e61a1a',
      800: '#b31414',
      900: '#800f0f',
      950: '#4d0909',
    },
  },
  semantic: {
    primary: {
      50: '#fff7ed',
      100: '#ffedd5',
      200: '#fed7aa',
      300: '#fdba74',
      400: '#fb923c',
      500: '#f06e1e',
      600: '#ea580c',
      700: '#c2410c',
      800: '#9a3412',
      900: '#7c2d12',
      950: '#431407',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f6f6f6',
          100: '#f5f5f5',
          200: '#f0f0f0',
          300: '#efefef',
          400: '#d6d6d6',
          500: '#aaaaaa',
          600: '#999999',
          700: '#797575',
          800: '#5d6060',
          900: '#3c3c3c',
          950: '#1a1a1a',
        },
        primary: {
          color: '#f06e1e',
          contrastColor: '#ffffff',
          hoverColor: '#ea580c',
          activeColor: '#c2410c',
        },
        success: {
          color: '#68cc58',
          contrastColor: '#ffffff',
          hoverColor: '#5bb84d',
          activeColor: '#4ea342',
        },
        warn: {
          color: '#f06e1e',
          contrastColor: '#ffffff',
          hoverColor: '#ea580c',
          activeColor: '#c2410c',
        },
        danger: {
          color: '#ff2a2a',
          contrastColor: '#ffffff',
          hoverColor: '#ff1e1e',
          activeColor: '#e61a1a',
        },
        info: {
          color: '#4368f2',
          contrastColor: '#ffffff',
          hoverColor: '#3366cc',
          activeColor: '#2952b8',
        },
      },
    },
  },
  // 커스텀 디자인 토큰 (typography, spacing 등)
  extend: {
    typography: {
      fontSans: '\'Noto Sans KR\', sans-serif',
      fontMono: '\'Nanum Gothic\', \'나눔고딕\', \'맑은고딕\', \'malgun gothic\', sans-serif',
      micro: '0.8125rem',
      mini: '0.875rem',
      small: '0.9375rem',
      regular: '1.0625rem',
      large: '1.25rem',
      title1: '1.625rem',
      title2: '2.375rem',
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '0px',
      },
    },
    card: {
      root: {
        borderRadius: '0px',
      },
    },
    inputtext: {
      root: {
        borderRadius: '0px',
      },
    },
    select: {
      root: {
        borderRadius: '0px',
      },
    },
    badge: {
      root: {
        borderRadius: '0px',
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
        borderRadius: '0px',
      },
    },
  },
})

// 테마 메타데이터 (HTML 클래스명, 기본 다크모드 선호도)
export const KOSCOM_LIGHT_META = {
  className: 'theme-koscom-light',
  prefersDark: false,
  oklch: KOSCOM_LIGHT_OKLCH,
} as const

export default KoscomLightPreset
