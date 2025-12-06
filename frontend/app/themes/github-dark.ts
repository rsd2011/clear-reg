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
        primary: {
          color: '#58a6ff',
          contrastColor: '#ffffff',
          hoverColor: '#79b8ff',
          activeColor: '#1f6feb',
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
      xs: '12px',
      sm: '14px',
      base: '16px',
      md: '18px',
      lg: '20px',
      xl: '24px',
      xxl: '32px',
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
