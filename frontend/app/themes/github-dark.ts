import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * GitHub Dark Theme Preset
 * Based on GitHub Primer Design System
 *
 * Primary Color: Blue
 * - Hex: #58a6ff
 * - OKLCH: oklch(0.70 0.14 230)
 *
 * @see https://primer.style/design/foundations/color/
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 */
export const GITHUB_DARK_OKLCH = {
  primary: {
    h: 230, // Hue: Blue
    c: 0.14, // Chroma: 중간 채도
    l: 0.70, // Lightness: 메인 색상
  },
  scale: {
    1: '#0d1117', // oklch(0.15 0.02 230)
    2: '#161b22', // oklch(0.20 0.028 230)
    3: '#21262d', // oklch(0.28 0.056 230)
    4: '#2a3038', // oklch(0.35 0.084 230)
    5: '#363d48', // oklch(0.42 0.112 230)
    6: '#44505e', // oklch(0.50 0.126 230)
    7: '#5577a0', // oklch(0.58 0.14 230)
    8: '#4892d0', // oklch(0.65 0.14 230)
    9: '#58a6ff', // oklch(0.70 0.14 230) - 메인 브랜드 색상
    10: '#79b8ff', // oklch(0.75 0.14 230)
    11: '#a5d6ff', // oklch(0.85 0.14 230)
    12: '#cae8ff', // oklch(0.92 0.14 230)
  },
} as const
export const GithubDarkPreset = definePreset(Aura, {
  primitive: {
    green: {
      50: '#f0fff4',
      100: '#dcffe4',
      200: '#a7f3bc',
      300: '#7ee89a',
      400: '#56d364',
      500: '#3fb950',
      600: '#238636',
      700: '#196c2e',
      800: '#0f5323',
      900: '#033a16',
      950: '#02260f',
    },
    sky: {
      50: '#f0f8ff',
      100: '#e1f0ff',
      200: '#a5d6ff',
      300: '#79b8ff',
      400: '#58a6ff',
      500: '#1f6feb',
      600: '#1158c7',
      700: '#0d419d',
      800: '#0a2d6e',
      900: '#051937',
      950: '#030d1c',
    },
    orange: {
      50: '#fff8f0',
      100: '#ffeed5',
      200: '#ffe3b3',
      300: '#e3b341',
      400: '#d29922',
      500: '#9e6a03',
      600: '#845306',
      700: '#693e05',
      800: '#4e2c04',
      900: '#331d02',
      950: '#1a0f01',
    },
    red: {
      50: '#fff5f5',
      100: '#ffe2e0',
      200: '#ffc1bd',
      300: '#ff7b72',
      400: '#f85149',
      500: '#da3633',
      600: '#b62324',
      700: '#8e1c1c',
      800: '#661514',
      900: '#3d0d0d',
      950: '#1f0606',
    },
  },
  semantic: {
    primary: {
      50: '#e6f2ff',
      100: '#cce4ff',
      200: '#99c9ff',
      300: '#66adff',
      400: '#3392ff',
      500: '#58a6ff',
      600: '#1f6feb',
      700: '#1158c7',
      800: '#0d419d',
      900: '#0a2d6e',
      950: '#051937',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f0f6fc',
          100: '#c9d1d9',
          200: '#b1bac4',
          300: '#8b949e',
          400: '#6e7681',
          500: '#484f58',
          600: '#30363d',
          700: '#21262d',
          800: '#161b22',
          900: '#0d1117',
          950: '#010409',
        },
        // primary: 토큰 참조로 semantic.primary 팔레트와 연동 (updatePrimaryPalette()가 팔레트 변경 시 자동 반영)
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}',
        },
        success: {
          color: '#3fb950',
          contrastColor: '#ffffff',
          hoverColor: '#56d364',
          activeColor: '#238636',
        },
        warn: {
          color: '#d29922',
          contrastColor: '#ffffff',
          hoverColor: '#e3b341',
          activeColor: '#9e6a03',
        },
        danger: {
          color: '#f85149',
          contrastColor: '#ffffff',
          hoverColor: '#ff7b72',
          activeColor: '#da3633',
        },
        info: {
          color: '#58a6ff',
          contrastColor: '#ffffff',
          hoverColor: '#79b8ff',
          activeColor: '#1f6feb',
        },
      },
    },
  },
  extend: {
    typography: {
      fontSans: '-apple-system, BlinkMacSystemFont, \'Segoe UI\', \'Noto Sans\', Helvetica, Arial, sans-serif',
      fontMono: '\'SFMono-Regular\', Consolas, \'Liberation Mono\', Menlo, monospace',
      micro: '0.75rem',
      mini: '0.8125rem',
      small: '0.875rem',
      regular: '1rem',
      large: '1.125rem',
      title1: '1.25rem',
      title2: '1.5rem',
      title3: '2rem',
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '6px',
      },
    },
    card: {
      root: {
        borderRadius: '6px',
      },
    },
    inputtext: {
      root: {
        borderRadius: '6px',
      },
    },
    select: {
      root: {
        borderRadius: '6px',
      },
    },
    badge: {
      root: {
        borderRadius: '10px',
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

export const GITHUB_DARK_META = {
  className: 'theme-github-dark',
  prefersDark: true,
  oklch: GITHUB_DARK_OKLCH,
} as const

export default GithubDarkPreset
