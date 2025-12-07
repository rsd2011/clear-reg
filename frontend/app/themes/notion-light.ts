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
  primitive: {
    green: {
      50: '#f0f9f4',
      100: '#dcf0e4',
      200: '#a8dbbf',
      300: '#74c69a',
      400: '#56b17f',
      500: '#448361',
      600: '#3a7054',
      700: '#305c46',
      800: '#264838',
      900: '#1c342a',
      950: '#121f19',
    },
    blue: {
      50: '#f0f7fc',
      100: '#dcecf7',
      200: '#a8d4eb',
      300: '#74bcdf',
      400: '#4391bc',
      500: '#337ea9',
      600: '#2a6a8f',
      700: '#215574',
      800: '#18415a',
      900: '#0f2c3f',
      950: '#081820',
    },
    orange: {
      50: '#fef6ed',
      100: '#fcecd5',
      200: '#f7d4a8',
      300: '#f1b570',
      400: '#e68a2e',
      500: '#d9730d',
      600: '#b8600a',
      700: '#974e08',
      800: '#763c06',
      900: '#552b05',
      950: '#341a03',
    },
    red: {
      50: '#fef3f2',
      100: '#fce5e4',
      200: '#f7c4c2',
      300: '#f09d99',
      400: '#e06561',
      500: '#d44c47',
      600: '#b33e3a',
      700: '#92322f',
      800: '#712624',
      900: '#501b19',
      950: '#2f0f0e',
    },
  },
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
        // primary: 토큰 참조로 semantic.primary 팔레트와 연동 (updatePrimaryPalette()가 팔레트 변경 시 자동 반영)
        primary: {
          color: '{primary.500}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.400}',
          activeColor: '{primary.600}',
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
      micro: '0.75rem',
      mini: '0.8125rem',
      small: '0.875rem',
      regular: '1rem',
      large: '1.125rem',
      title1: '1.25rem',
      title2: '1.5rem',
      title3: '1.875rem',
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
