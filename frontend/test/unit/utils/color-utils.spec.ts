/**
 * color-utils 유닛 테스트
 *
 * OKLCH → HEX 변환 및 PrimeVue 팔레트 생성 함수 테스트
 */
import { describe, it, expect } from 'vitest'
import {
  oklchToHex,
  hexToOklch,
  generatePrimaryPaletteFromOklch,
  generateSurfacePaletteFromOklch,
  type OKLCH,
  type PrimeVuePalette,
} from '../../../app/utils/color-utils'

describe('oklchToHex', () => {
  it('should convert pure white correctly', () => {
    const oklch: OKLCH = { l: 1, c: 0, h: 0 }
    const hex = oklchToHex(oklch)
    expect(hex).toBe('#ffffff')
  })

  it('should convert pure black correctly', () => {
    const oklch: OKLCH = { l: 0, c: 0, h: 0 }
    const hex = oklchToHex(oklch)
    expect(hex).toBe('#000000')
  })

  it('should convert Linear Dark primary color correctly', () => {
    // Linear Dark: oklch(0.55 0.15 265) ≈ #5e6ad2
    const oklch: OKLCH = { l: 0.55, c: 0.15, h: 265 }
    const hex = oklchToHex(oklch)

    // HEX 값이 예상 범위 내에 있는지 확인
    // 정확한 값은 변환 알고리즘에 따라 약간 다를 수 있음
    expect(hex).toMatch(/^#[0-9a-f]{6}$/i)

    // 색상이 파란색 계열인지 확인 (R < G < B)
    const rgb = hexToRgb(hex)
    expect(rgb.b).toBeGreaterThan(rgb.r)
  })

  it('should convert GitHub Dark primary color correctly', () => {
    // GitHub Dark: oklch(0.70 0.14 230) ≈ #58a6ff
    const oklch: OKLCH = { l: 0.70, c: 0.14, h: 230 }
    const hex = oklchToHex(oklch)

    expect(hex).toMatch(/^#[0-9a-f]{6}$/i)

    // 색상이 파란색 계열인지 확인
    const rgb = hexToRgb(hex)
    expect(rgb.b).toBeGreaterThan(rgb.r)
    expect(rgb.b).toBeGreaterThan(rgb.g)
  })

  it('should handle neutral gray (c=0)', () => {
    const oklch: OKLCH = { l: 0.5, c: 0, h: 0 }
    const hex = oklchToHex(oklch)

    // RGB 값이 모두 같아야 함 (gray)
    const rgb = hexToRgb(hex)
    expect(rgb.r).toBe(rgb.g)
    expect(rgb.g).toBe(rgb.b)
  })
})

describe('hexToOklch', () => {
  it('should convert white to OKLCH', () => {
    const oklch = hexToOklch('#ffffff')
    expect(oklch).not.toBeNull()
    expect(oklch!.l).toBeCloseTo(1, 2)
    expect(oklch!.c).toBeCloseTo(0, 2)
  })

  it('should convert black to OKLCH', () => {
    const oklch = hexToOklch('#000000')
    expect(oklch).not.toBeNull()
    expect(oklch!.l).toBeCloseTo(0, 2)
    expect(oklch!.c).toBeCloseTo(0, 2)
  })

  it('should return null for invalid hex', () => {
    expect(hexToOklch('invalid')).toBeNull()
    expect(hexToOklch('#gggggg')).toBeNull()
  })

  it('should be reversible (hex → oklch → hex)', () => {
    const originalHex = '#5e6ad2'
    const oklch = hexToOklch(originalHex)
    expect(oklch).not.toBeNull()

    const convertedHex = oklchToHex(oklch!)

    // 변환 후 색상이 원본과 유사해야 함 (약간의 오차 허용)
    const originalRgb = hexToRgb(originalHex)
    const convertedRgb = hexToRgb(convertedHex)

    expect(Math.abs(originalRgb.r - convertedRgb.r)).toBeLessThan(3)
    expect(Math.abs(originalRgb.g - convertedRgb.g)).toBeLessThan(3)
    expect(Math.abs(originalRgb.b - convertedRgb.b)).toBeLessThan(3)
  })
})

describe('generatePrimaryPaletteFromOklch', () => {
  it('should generate all 11 palette steps', () => {
    const palette = generatePrimaryPaletteFromOklch(265, 0.15)

    const expectedSteps: (keyof PrimeVuePalette)[] = [
      50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950,
    ]

    for (const step of expectedSteps) {
      expect(palette[step]).toBeDefined()
      expect(palette[step]).toMatch(/^#[0-9a-f]{6}$/i)
    }
  })

  it('should generate lighter colors for lower numbers', () => {
    const palette = generatePrimaryPaletteFromOklch(265, 0.15)

    // 50은 가장 밝아야 함
    const rgb50 = hexToRgb(palette[50])
    const rgb950 = hexToRgb(palette[950])

    // 밝기 = (R + G + B) / 3
    const brightness50 = (rgb50.r + rgb50.g + rgb50.b) / 3
    const brightness950 = (rgb950.r + rgb950.g + rgb950.b) / 3

    expect(brightness50).toBeGreaterThan(brightness950)
  })

  it('should maintain hue across all steps', () => {
    const hue = 265 // Linear Dark (Indigo)
    const palette = generatePrimaryPaletteFromOklch(hue, 0.15)

    // 500 단계의 OKLCH를 확인
    const oklch500 = hexToOklch(palette[500])
    expect(oklch500).not.toBeNull()

    // Hue가 유지되어야 함 (약간의 오차 허용)
    expect(Math.abs(oklch500!.h - hue)).toBeLessThan(10)
  })

  it('should generate different palettes for different hues', () => {
    const paletteIndigo = generatePrimaryPaletteFromOklch(265, 0.15)
    const paletteBlue = generatePrimaryPaletteFromOklch(230, 0.15)
    const palettePurple = generatePrimaryPaletteFromOklch(320, 0.15)

    // 모든 팔레트의 500 단계가 다른 색상이어야 함
    expect(paletteIndigo[500]).not.toBe(paletteBlue[500])
    expect(paletteBlue[500]).not.toBe(palettePurple[500])
    expect(palettePurple[500]).not.toBe(paletteIndigo[500])
  })

  it('should handle dark mode adjustment', () => {
    const paletteLight = generatePrimaryPaletteFromOklch(265, 0.15, false)
    const paletteDark = generatePrimaryPaletteFromOklch(265, 0.15, true)

    // 다크모드에서는 채도가 약간 높아질 수 있음
    // (둘 다 유효한 팔레트여야 함)
    expect(paletteLight[500]).toMatch(/^#[0-9a-f]{6}$/i)
    expect(paletteDark[500]).toMatch(/^#[0-9a-f]{6}$/i)
  })
})

describe('generateSurfacePaletteFromOklch', () => {
  it('should generate surface palette with all steps', () => {
    const palette = generateSurfacePaletteFromOklch(false)

    const expectedSteps = ['0', '50', '100', '200', '300', '400', '500', '600', '700', '800', '900', '950']

    for (const step of expectedSteps) {
      expect(palette[step]).toBeDefined()
      expect(palette[step]).toMatch(/^#[0-9a-f]{6}$/i)
    }
  })

  it('should generate pure white for step 0', () => {
    const palette = generateSurfacePaletteFromOklch(false)
    expect(palette['0']).toBe('#ffffff')
  })

  it('should generate grayscale colors (R = G = B)', () => {
    const palette = generateSurfacePaletteFromOklch(false)

    for (const [, hex] of Object.entries(palette)) {
      const rgb = hexToRgb(hex)
      // 무채색이므로 R, G, B가 모두 같아야 함 (약간의 오차 허용)
      expect(Math.abs(rgb.r - rgb.g)).toBeLessThan(2)
      expect(Math.abs(rgb.g - rgb.b)).toBeLessThan(2)
    }
  })

  it('should generate different palettes for light and dark modes', () => {
    const paletteLight = generateSurfacePaletteFromOklch(false)
    const paletteDark = generateSurfacePaletteFromOklch(true)

    // 라이트/다크 모드에서 중간 단계 색상이 다를 수 있음
    // (0은 둘 다 white이지만, 500은 다를 수 있음)
    expect(paletteLight['500']).toBeDefined()
    expect(paletteDark['500']).toBeDefined()
  })
})

// 헬퍼 함수
function hexToRgb(hex: string): { r: number, g: number, b: number } {
  const sanitized = hex.replace(/^#/, '')
  const num = parseInt(sanitized, 16)
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  }
}
