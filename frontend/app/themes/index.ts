export { LinearDarkPreset, LINEAR_DARK_META } from './linear-dark'
export { KoscomLightPreset, KOSCOM_LIGHT_META } from './koscom-light'

export type ThemeName = 'linear-dark' | 'koscom-light'
export type ThemeMode = 'system' | 'dark' | 'light'

export interface ThemeConfig {
  name: string
  description: string
  className: string
  prefersDark: boolean
}

export const THEMES: Record<ThemeName, ThemeConfig> = {
  'linear-dark': {
    name: 'Linear Dark',
    description: 'Linear.app 스타일 다크 테마',
    className: 'theme-linear-dark',
    prefersDark: true,
  },
  'koscom-light': {
    name: 'Koscom Light',
    description: 'Koscom 스타일 라이트 테마',
    className: 'theme-koscom-light',
    prefersDark: false,
  },
} as const
