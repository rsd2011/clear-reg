import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Figma Dark Theme Preset
 * Based on Figma's design tool interface
 *
 * Primary Color: Figma Blue
 * - Hex: #0d99ff
 * - OKLCH: oklch(0.68 0.18 230)
 *
 * @see https://www.figma.com/blog/illuminating-dark-mode/
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 */
export const FIGMA_DARK_OKLCH = {
  primary: {
    h: 230, // Hue: Blue
    c: 0.18, // Chroma: 높은 채도 (Figma 특유의 선명한 파랑)
    l: 0.68, // Lightness: 메인 색상
  },
  scale: {
    1: '#0a0a0a', // oklch(0.12 0.02 230)
    2: '#121212', // oklch(0.15 0.03 230)
    3: '#1e1e1e', // oklch(0.22 0.06 230)
    4: '#2c2c2c', // oklch(0.30 0.10 230)
    5: '#383838', // oklch(0.38 0.14 230)
    6: '#444444', // oklch(0.45 0.16 230)
    7: '#5285c0', // oklch(0.55 0.18 230)
    8: '#0a7acc', // oklch(0.60 0.18 230)
    9: '#0d99ff', // oklch(0.68 0.18 230) - 메인 브랜드 색상
    10: '#3fafff', // oklch(0.75 0.18 230)
    11: '#6fc3ff', // oklch(0.82 0.18 230)
    12: '#9fd7ff', // oklch(0.88 0.18 230)
  },
} as const
export const FigmaDarkPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#e7f5ff',
      100: '#cfebff',
      200: '#9fd7ff',
      300: '#6fc3ff',
      400: '#3fafff',
      500: '#0d99ff',
      600: '#0a7acc',
      700: '#085c99',
      800: '#053d66',
      900: '#031f33',
      950: '#010f1a',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f5f5f5',
          100: '#e5e5e5',
          200: '#b3b3b3',
          300: '#808080',
          400: '#5c5c5c',
          500: '#444444',
          600: '#383838',
          700: '#2c2c2c',
          800: '#1e1e1e',
          900: '#121212',
          950: '#0a0a0a',
        },
        primary: {
          color: '#0d99ff',
          contrastColor: '#ffffff',
          hoverColor: '#3fafff',
          activeColor: '#0a7acc',
        },
        success: {
          color: '#14ae5c',
          contrastColor: '#ffffff',
          hoverColor: '#1bc76b',
          activeColor: '#0f8a49',
        },
        warn: {
          color: '#ffcd29',
          contrastColor: '#1e1e1e',
          hoverColor: '#ffd54f',
          activeColor: '#e6b800',
        },
        danger: {
          color: '#f24822',
          contrastColor: '#ffffff',
          hoverColor: '#ff6647',
          activeColor: '#d93a18',
        },
        info: {
          color: '#a259ff',
          contrastColor: '#ffffff',
          hoverColor: '#b77fff',
          activeColor: '#8b3fdf',
        },
      },
    },
  },
  extend: {
    typography: {
      fontSans: '\'Inter\', -apple-system, BlinkMacSystemFont, \'Segoe UI\', Roboto, sans-serif',
      fontMono: '\'JetBrains Mono\', \'Fira Code\', \'Source Code Pro\', monospace',
      xs: '11px',
      sm: '12px',
      base: '13px',
      md: '14px',
      lg: '16px',
      xl: '20px',
      xxl: '28px',
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
        borderRadius: '8px',
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
        borderRadius: '4px',
      },
    },
    dialog: {
      root: {
        borderRadius: '12px',
      },
    },
  },
})

export const FIGMA_DARK_META = {
  className: 'theme-figma-dark',
  prefersDark: true,
  oklch: FIGMA_DARK_OKLCH,
} as const

export default FigmaDarkPreset
