import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Notion Light Theme Preset
 * Based on Notion's minimalist design system
 *
 * Primary Color: Teal/Blue
 * - Hex: #2eaadc
 * - OKLCH: oklch(0.68 0.12 200)
 *
 * @see https://www.notionavenue.co/post/notion-color-code-hex-palette
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 * 라이트 테마: 밝은 배경에서 시작, 어두운 텍스트로 종료
 */
export const NOTION_LIGHT_OKLCH = {
  primary: {
    h: 200, // Hue: Teal/Cyan
    c: 0.12, // Chroma: 낮은 채도 (Notion 특유의 차분한 느낌)
    l: 0.68, // Lightness: 메인 색상
  },
  scale: {
    1: '#ffffff', // oklch(0.99 0 0) - 가장 밝은 배경
    2: '#fbfbfa', // oklch(0.98 0.005 200)
    3: '#f7f6f3', // oklch(0.96 0.01 200)
    4: '#f1f1ef', // oklch(0.94 0.015 200)
    5: '#e9e9e7', // oklch(0.92 0.02 200)
    6: '#d3d3d1', // oklch(0.85 0.025 200)
    7: '#9b9a97', // oklch(0.68 0.03 200)
    8: '#787774', // oklch(0.55 0.035 200)
    9: '#2eaadc', // oklch(0.68 0.12 200) - 메인 브랜드 색상
    10: '#2488b0', // oklch(0.58 0.12 200)
    11: '#37352f', // oklch(0.30 0.02 200) - 본문 텍스트
    12: '#1f1f1e', // oklch(0.20 0.01 200) - 가장 어두운 텍스트
  },
} as const
export const NotionLightPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#e8f6fb',
      100: '#d1edf7',
      200: '#a3dbef',
      300: '#75c9e7',
      400: '#47b7df',
      500: '#2eaadc',
      600: '#2488b0',
      700: '#1b6684',
      800: '#124458',
      900: '#09222c',
      950: '#041116',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#fbfbfa',
          100: '#f7f6f3',
          200: '#f1f1ef',
          300: '#e9e9e7',
          400: '#d3d3d1',
          500: '#9b9a97',
          600: '#787774',
          700: '#5a5955',
          800: '#37352f',
          900: '#1f1f1e',
          950: '#0f0f0e',
        },
        primary: {
          color: '#2eaadc',
          contrastColor: '#ffffff',
          hoverColor: '#47b7df',
          activeColor: '#2488b0',
        },
        success: {
          color: '#448361',
          contrastColor: '#ffffff',
          hoverColor: '#4d9970',
          activeColor: '#3a7054',
        },
        warn: {
          color: '#d9730d',
          contrastColor: '#ffffff',
          hoverColor: '#e68a2e',
          activeColor: '#b8600a',
        },
        danger: {
          color: '#d44c47',
          contrastColor: '#ffffff',
          hoverColor: '#e06561',
          activeColor: '#b33e3a',
        },
        info: {
          color: '#337ea9',
          contrastColor: '#ffffff',
          hoverColor: '#4391bc',
          activeColor: '#2a6a8f',
        },
      },
    },
  },
  extend: {
    typography: {
      fontSerif: '\'Lyon-Text\', Georgia, \'Cambria\', \'Times New Roman\', Times, serif',
      fontSans: '\'Segoe UI\', Helvetica, \'Apple Color Emoji\', Arial, sans-serif',
      fontMono: '\'SFMono-Regular\', Menlo, Consolas, monospace',
      xs: '12px',
      sm: '14px',
      base: '16px',
      md: '18px',
      lg: '20px',
      xl: '24px',
      xxl: '30px',
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '4px',
      },
    },
    card: {
      root: {
        borderRadius: '4px',
      },
    },
    inputtext: {
      root: {
        borderRadius: '4px',
      },
    },
    select: {
      root: {
        borderRadius: '4px',
      },
    },
    badge: {
      root: {
        borderRadius: '3px',
      },
    },
    dialog: {
      root: {
        borderRadius: '4px',
      },
    },
  },
})

export const NOTION_LIGHT_META = {
  className: 'theme-notion-light',
  prefersDark: false,
  oklch: NOTION_LIGHT_OKLCH,
} as const

export default NotionLightPreset
