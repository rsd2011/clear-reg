import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Slack Aubergine Theme Preset
 * Based on Slack's classic Aubergine design system
 *
 * Primary Color: Aubergine (Purple)
 * - Hex: #4a154b / #611f63
 * - OKLCH: oklch(0.35 0.15 320)
 *
 * @see https://slack.design/
 * @see https://brandpalettes.com/slack-logo-color-codes/
 */

/**
 * OKLCH 색상 정의 (CSS 변수와 동기화)
 */
export const SLACK_AUBERGINE_OKLCH = {
  primary: {
    h: 320, // Hue: Purple/Magenta
    c: 0.15, // Chroma: 중간 채도
    l: 0.35, // Lightness: 어두운 메인 색상
  },
  scale: {
    1: '#0a090c', // oklch(0.10 0.02 320)
    2: '#121016', // oklch(0.15 0.03 320)
    3: '#1a1d21', // oklch(0.22 0.045 320)
    4: '#222529', // oklch(0.28 0.06 320)
    5: '#2e2d31', // oklch(0.35 0.075 320)
    6: '#3d3c40', // oklch(0.42 0.09 320)
    7: '#4a154b', // oklch(0.35 0.15 320) - 브랜드 색상
    8: '#611f63', // oklch(0.40 0.15 320)
    9: '#7a2b7c', // oklch(0.48 0.15 320) - UI 액센트
    10: '#9a3e9c', // oklch(0.55 0.15 320)
    11: '#c266c3', // oklch(0.68 0.15 320)
    12: '#ebcceb', // oklch(0.88 0.10 320)
  },
} as const
export const SlackAuberginePreset = definePreset(Aura, {
  primitive: {
    green: {
      50: '#edfdf6',
      100: '#d4f9e8',
      200: '#87eec0',
      300: '#50e39f',
      400: '#36d395',
      500: '#2eb67d',
      600: '#239762',
      700: '#1a7249',
      800: '#114d31',
      900: '#082918',
      950: '#04140c',
    },
    cyan: {
      50: '#f0fbff',
      100: '#e0f7ff',
      200: '#a8e8fc',
      300: '#7adff8',
      400: '#5ed3f5',
      500: '#36c5f0',
      600: '#1fa8d4',
      700: '#1785a8',
      800: '#0f627c',
      900: '#083f50',
      950: '#042028',
    },
    yellow: {
      50: '#fffdf0',
      100: '#fffbe0',
      200: '#fff3b3',
      300: '#ffe680',
      400: '#f5c754',
      500: '#ecb22e',
      600: '#d9a020',
      700: '#a87a18',
      800: '#775510',
      900: '#463108',
      950: '#231904',
    },
    red: {
      50: '#fef2f6',
      100: '#fde5ed',
      200: '#f9b5cc',
      300: '#f586ab',
      400: '#f03b72',
      500: '#e01e5a',
      600: '#b8174a',
      700: '#8f1239',
      800: '#660d28',
      900: '#3d0818',
      950: '#1f040c',
    },
  },
  semantic: {
    primary: {
      50: '#f5e6f5',
      100: '#ebcceb',
      200: '#d699d7',
      300: '#c266c3',
      400: '#ae33af',
      500: '#4a154b',
      600: '#611f63',
      700: '#4a154b',
      800: '#330e33',
      900: '#1c081c',
      950: '#0e040e',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f8f8f8',
          100: '#d1d2d3',
          200: '#ababad',
          300: '#868689',
          400: '#616061',
          500: '#3d3c40',
          600: '#2e2d31',
          700: '#222529',
          800: '#1a1d21',
          900: '#121016',
          950: '#0a090c',
        },
        primary: {
          color: '#4a154b',
          contrastColor: '#ffffff',
          hoverColor: '#611f63',
          activeColor: '#330e33',
        },
        success: {
          color: '#2eb67d',
          contrastColor: '#ffffff',
          hoverColor: '#36d395',
          activeColor: '#239762',
        },
        warn: {
          color: '#ecb22e',
          contrastColor: '#1a1d21',
          hoverColor: '#f5c754',
          activeColor: '#d9a020',
        },
        danger: {
          color: '#e01e5a',
          contrastColor: '#ffffff',
          hoverColor: '#f03b72',
          activeColor: '#b8174a',
        },
        info: {
          color: '#36c5f0',
          contrastColor: '#1a1d21',
          hoverColor: '#5ed3f5',
          activeColor: '#1fa8d4',
        },
      },
    },
  },
  extend: {
    typography: {
      fontSans: '\'Slack-Lato\', \'Lato\', -apple-system, BlinkMacSystemFont, sans-serif',
      fontMono: '\'Monaco\', \'Menlo\', \'Ubuntu Mono\', \'Consolas\', monospace',
      micro: '0.75rem',
      mini: '0.8125rem',
      small: '0.875rem',
      regular: '0.9375rem',
      large: '1rem',
      title1: '1.125rem',
      title2: '1.375rem',
      title3: '1.75rem',
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
        borderRadius: '8px',
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
          info: { background: 'color-mix(in srgb, {cyan.500} 16%, transparent)', color: '{cyan.500}' },
          warn: { background: 'color-mix(in srgb, {yellow.500} 16%, transparent)', color: '{yellow.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
        },
        dark: {
          success: { background: 'color-mix(in srgb, {green.500} 16%, transparent)', color: '{green.500}' },
          info: { background: 'color-mix(in srgb, {cyan.500} 16%, transparent)', color: '{cyan.500}' },
          warn: { background: 'color-mix(in srgb, {yellow.500} 16%, transparent)', color: '{yellow.500}' },
          danger: { background: 'color-mix(in srgb, {red.500} 16%, transparent)', color: '{red.500}' },
        },
      },
    },
    dialog: {
      root: {
        borderRadius: '8px',
      },
    },
  },
})

export const SLACK_AUBERGINE_META = {
  className: 'theme-slack-aubergine',
  prefersDark: true,
  oklch: SLACK_AUBERGINE_OKLCH,
} as const

export default SlackAuberginePreset
