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
  primitive: {
    green: {
      50: '#edfaf2',
      100: '#d1f4e0',
      200: '#7ae4a8',
      300: '#47d684',
      400: '#1bc76b',
      500: '#14ae5c',
      600: '#0f8a49',
      700: '#0b6837',
      800: '#074525',
      900: '#032313',
      950: '#02110a',
    },
    purple: {
      50: '#faf5ff',
      100: '#f3e8ff',
      200: '#dfc4ff',
      300: '#c9a0ff',
      400: '#b77fff',
      500: '#a259ff',
      600: '#8b3fdf',
      700: '#7030b5',
      800: '#54238a',
      900: '#38185c',
      950: '#1c0c2e',
    },
    yellow: {
      50: '#fffef0',
      100: '#fffde0',
      200: '#fff8b3',
      300: '#ffef7a',
      400: '#ffd54f',
      500: '#ffcd29',
      600: '#e6b800',
      700: '#b38f00',
      800: '#806600',
      900: '#4d3d00',
      950: '#261f00',
    },
    red: {
      50: '#fff5f2',
      100: '#ffe5de',
      200: '#ffb9a8',
      300: '#ff8b71',
      400: '#ff6647',
      500: '#f24822',
      600: '#d93a18',
      700: '#ab2d12',
      800: '#7d200d',
      900: '#4f1408',
      950: '#280a04',
    },
  },
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
        // primary: 토큰 참조로 semantic.primary 팔레트와 연동 (updatePrimaryPalette()가 팔레트 변경 시 자동 반영)
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}',
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
      micro: '0.6875rem',
      mini: '0.75rem',
      small: '0.8125rem',
      regular: '0.875rem',
      large: '1rem',
      title1: '1.25rem',
      title2: '1.75rem',
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
    tag: {
      colorScheme: {
        light: {
          success: { background: 'color-mix(in srgb, {green.500} 16%, transparent)', color: '{green.500}' },
          info: { background: 'color-mix(in srgb, {purple.500} 16%, transparent)', color: '{purple.500}' },
          warn: { background: 'color-mix(in srgb, {yellow.500} 16%, transparent)', color: '{yellow.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
        },
        dark: {
          success: { background: 'color-mix(in srgb, {green.500} 16%, transparent)', color: '{green.500}' },
          info: { background: 'color-mix(in srgb, {purple.500} 16%, transparent)', color: '{purple.500}' },
          warn: { background: 'color-mix(in srgb, {yellow.500} 16%, transparent)', color: '{yellow.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
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

export const FIGMA_DARK_META = {
  className: 'theme-figma-dark',
  prefersDark: true,
  oklch: FIGMA_DARK_OKLCH,
} as const

export default FigmaDarkPreset
