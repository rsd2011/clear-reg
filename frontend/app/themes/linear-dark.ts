import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Linear Dark Theme Preset
 * Based on Linear.app design system
 *
 * Primary Color: Indigo
 * - Hex: #5e6ad2
 * - OKLCH: oklch(0.55 0.15 265)
 *
 * @see https://linear.app/design
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 * 테마 전환 시 CSS 변수 --oklch-primary-h, --oklch-primary-c 로 적용됨
 */
export const LINEAR_DARK_OKLCH = {
  primary: {
    h: 265, // Hue: Indigo
    c: 0.15, // Chroma: 중간 채도
    l: 0.55, // Lightness: 메인 색상 (9단계)
  },
  // 12단계 스케일 Hex 값 (OKLCH에서 변환)
  scale: {
    1: '#0d0e18', // oklch(0.15 0.02 265) - 가장 어두운 배경
    2: '#121422', // oklch(0.20 0.03 265)
    3: '#1a1d32', // oklch(0.28 0.06 265)
    4: '#232842', // oklch(0.35 0.09 265)
    5: '#2d3454', // oklch(0.42 0.12 265)
    6: '#3a4268', // oklch(0.50 0.135 265)
    7: '#48527c', // oklch(0.58 0.15 265)
    8: '#5660a0', // oklch(0.65 0.15 265)
    9: '#5e6ad2', // oklch(0.60 0.15 265) - 메인 브랜드 색상
    10: '#6f79e0', // oklch(0.68 0.15 265)
    11: '#8c94ea', // oklch(0.78 0.15 265)
    12: '#c5c9f5', // oklch(0.90 0.15 265) - 가장 밝은 텍스트
  },
} as const
export const LinearDarkPreset = definePreset(Aura, {
  primitive: {
    green: {
      50: '#f0fdf0',
      100: '#dcfadc',
      200: '#92de87',
      300: '#7dd56f',
      400: '#68cc58',
      500: '#5bb84d',
      600: '#4ea342',
      700: '#418937',
      800: '#346e2c',
      900: '#274f20',
      950: '#1a3515',
    },
    sky: {
      50: '#f0f9ff',
      100: '#e0f2fe',
      200: '#90c9fe',
      300: '#6fb8fd',
      400: '#4ea7fc',
      500: '#3d95e8',
      600: '#2b7cc9',
      700: '#1c65a8',
      800: '#144e87',
      900: '#0d3866',
      950: '#072245',
    },
    orange: {
      50: '#fff7ed',
      100: '#ffedd5',
      200: '#fcd9a8',
      300: '#f8bc78',
      400: '#f29949',
      500: '#f08230',
      600: '#e06a1e',
      700: '#ba5317',
      800: '#943f12',
      900: '#6e2e0d',
      950: '#481d08',
    },
    red: {
      50: '#fef2f2',
      100: '#fee2e2',
      200: '#f8b4ae',
      300: '#f28f87',
      400: '#ec6a5e',
      500: '#e74c3c',
      600: '#d43f2f',
      700: '#b33426',
      800: '#8f2a1e',
      900: '#6b2017',
      950: '#47150f',
    },
  },
  semantic: {
    primary: {
      50: '#eef0fb',
      100: '#d5d9f5',
      200: '#bbc2ee',
      300: '#a1abe8',
      400: '#8795e1',
      500: '#5e6ad2',
      600: '#4a54a8',
      700: '#373f7e',
      800: '#232954',
      900: '#10142a',
      950: '#080a15',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f7f8f8',
          100: '#e6e6e6',
          200: '#d0d6e0',
          300: '#8a8f98',
          400: '#62666d',
          500: '#3e3e44',
          600: '#282828',
          700: '#232326',
          800: '#141516',
          900: '#08090a',
          950: '#050506',
        },
        // primary: 토큰 참조로 semantic.primary 팔레트와 연동 (updatePrimaryPalette()가 팔레트 변경 시 자동 반영)
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}',
        },
        success: {
          color: '#68cc58',
          contrastColor: '#ffffff',
          hoverColor: '#5bb84d',
          activeColor: '#4ea342',
        },
        warn: {
          color: '#f29949',
          contrastColor: '#ffffff',
          hoverColor: '#f08230',
          activeColor: '#e06a1e',
        },
        danger: {
          color: '#ec6a5e',
          contrastColor: '#ffffff',
          hoverColor: '#e74c3c',
          activeColor: '#d43f2f',
        },
        info: {
          color: '#4ea7fc',
          contrastColor: '#ffffff',
          hoverColor: '#3d95e8',
          activeColor: '#2b7cc9',
        },
      },
    },
  },
  // 커스텀 디자인 토큰 (typography, spacing 등)
  extend: {
    typography: {
      fontSans: '\'Inter Variable\', \'SF Pro Display\', -apple-system, BlinkMacSystemFont, sans-serif',
      fontMono: '\'Berkeley Mono\', \'SF Mono\', \'Menlo\', monospace',
      micro: '0.6875rem',
      mini: '0.75rem',
      small: '0.8125rem',
      regular: '0.9375rem',
      large: '1.0625rem',
      title1: '1.0625rem',
      title2: '1.3125rem',
      title3: '1.5rem',
      title4: '2rem',
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '8px',
      },
    },
    card: {
      root: {
        borderRadius: '12px',
      },
    },
    inputtext: {
      root: {
        borderRadius: '8px',
      },
    },
    select: {
      root: {
        borderRadius: '8px',
      },
    },
    badge: {
      root: {
        borderRadius: '6px',
      },
    },
    tag: {
      colorScheme: {
        light: {
          success: { background: 'color-mix(in srgb, {green.400} 16%, transparent)', color: '{green.400}' },
          info: { background: 'color-mix(in srgb, {sky.400} 16%, transparent)', color: '{sky.400}' },
          warn: { background: 'color-mix(in srgb, {orange.400} 16%, transparent)', color: '{orange.400}' },
          danger: { background: 'color-mix(in srgb, {red.400} 16%, transparent)', color: '{red.400}' },
        },
        dark: {
          success: { background: 'color-mix(in srgb, {green.400} 16%, transparent)', color: '{green.400}' },
          info: { background: 'color-mix(in srgb, {sky.400} 16%, transparent)', color: '{sky.400}' },
          warn: { background: 'color-mix(in srgb, {orange.400} 16%, transparent)', color: '{orange.400}' },
          danger: { background: 'color-mix(in srgb, {red.400} 16%, transparent)', color: '{red.400}' },
        },
      },
    },
    dialog: {
      root: {
        borderRadius: '12px',
      },
    },
  },
})

// 테마 메타데이터 (HTML 클래스명, 기본 다크모드 선호도)
export const LINEAR_DARK_META = {
  className: 'theme-linear-dark',
  prefersDark: true,
  /** OKLCH 색상 공간 정보 */
  oklch: LINEAR_DARK_OKLCH,
} as const

export default LinearDarkPreset
