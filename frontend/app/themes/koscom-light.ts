import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

/**
 * Koscom Light Theme Preset
 * Based on Koscom.co.kr design system
 * Primary color: Orange #f06e1e
 */
export const KoscomLightPreset = definePreset(Aura, {
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
      fontPrimary: '\'Noto Sans KR\', sans-serif',
      fontSecondary: '\'Nanum Gothic\', \'나눔고딕\', \'맑은고딕\', \'malgun gothic\', sans-serif',
      xs: '13px',
      sm: '14px',
      base: '15px',
      md: '17px',
      lg: '20px',
      xl: '26px',
      xxl: '38px',
      weightNormal: '400',
      weightBold: '700',
      weightExtraBold: '800',
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
} as const

export default KoscomLightPreset
