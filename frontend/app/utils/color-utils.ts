/**
 * Color Utility Functions for Theme Customization
 *
 * Provides color manipulation utilities including:
 * - Color format conversions (HEX, RGB, HSL, OKLCH)
 * - Palette generation from primary color
 * - WCAG contrast ratio calculations
 * - OKLCH-based 12-step scale generation
 * - Color validation
 */

// ============================================================================
// Types
// ============================================================================

export interface RGB {
  r: number
  g: number
  b: number
}

export interface HSL {
  h: number
  s: number
  l: number
}

/**
 * OKLCH 색상 공간
 * @property l - Lightness (0-1)
 * @property c - Chroma (0-0.4, 일반적으로 0-0.2)
 * @property h - Hue (0-360)
 */
export interface OKLCH {
  l: number
  c: number
  h: number
}

/**
 * Oklab 색상 공간 (OKLCH 변환용 중간 형식)
 */
export interface Oklab {
  L: number
  a: number
  b: number
}

export interface ColorPalette {
  50: string
  100: string
  200: string
  300: string
  400: string
  500: string
  600: string
  700: string
  800: string
  900: string
  950: string
}

/**
 * 12단계 색상 스케일 (Radix UI 패턴)
 * 1-2: Backgrounds, 3-5: UI elements, 6-8: Borders, 9-12: Text/Solid
 */
export interface ColorScale12 {
  1: string
  2: string
  3: string
  4: string
  5: string
  6: string
  7: string
  8: string
  9: string
  10: string
  11: string
  12: string
}

// ============================================================================
// Color Format Conversions
// ============================================================================

/**
 * Convert HEX color to RGB
 * @param hex - HEX color string (with or without #)
 * @returns RGB object or null if invalid
 */
export function hexToRgb(hex: string): RGB | null {
  const sanitized = hex.replace(/^#/, '')

  if (!/^[0-9A-Fa-f]{6}$/.test(sanitized)) {
    return null
  }

  const num = parseInt(sanitized, 16)
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  }
}

/**
 * Convert RGB to HEX color
 * @param rgb - RGB object
 * @returns HEX color string with #
 */
export function rgbToHex(rgb: RGB): string {
  const toHex = (n: number) => {
    const clamped = Math.max(0, Math.min(255, Math.round(n)))
    return clamped.toString(16).padStart(2, '0')
  }

  return `#${toHex(rgb.r)}${toHex(rgb.g)}${toHex(rgb.b)}`
}

/**
 * Convert RGB to HSL
 * @param rgb - RGB object (values 0-255)
 * @returns HSL object (h: 0-360, s: 0-100, l: 0-100)
 */
export function rgbToHsl(rgb: RGB): HSL {
  const r = rgb.r / 255
  const g = rgb.g / 255
  const b = rgb.b / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const l = (max + min) / 2

  if (max === min) {
    return { h: 0, s: 0, l: l * 100 }
  }

  const d = max - min
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min)

  let h: number
  switch (max) {
    case r:
      h = ((g - b) / d + (g < b ? 6 : 0)) / 6
      break
    case g:
      h = ((b - r) / d + 2) / 6
      break
    default:
      h = ((r - g) / d + 4) / 6
  }

  return {
    h: Math.round(h * 360),
    s: Math.round(s * 100),
    l: Math.round(l * 100),
  }
}

/**
 * Convert HSL to RGB
 * @param hsl - HSL object (h: 0-360, s: 0-100, l: 0-100)
 * @returns RGB object (values 0-255)
 */
export function hslToRgb(hsl: HSL): RGB {
  const h = hsl.h / 360
  const s = hsl.s / 100
  const l = hsl.l / 100

  if (s === 0) {
    const gray = Math.round(l * 255)
    return { r: gray, g: gray, b: gray }
  }

  const hueToRgb = (p: number, q: number, t: number): number => {
    let tt = t
    if (tt < 0) tt += 1
    if (tt > 1) tt -= 1
    if (tt < 1 / 6) return p + (q - p) * 6 * tt
    if (tt < 1 / 2) return q
    if (tt < 2 / 3) return p + (q - p) * (2 / 3 - tt) * 6
    return p
  }

  const q = l < 0.5 ? l * (1 + s) : l + s - l * s
  const p = 2 * l - q

  return {
    r: Math.round(hueToRgb(p, q, h + 1 / 3) * 255),
    g: Math.round(hueToRgb(p, q, h) * 255),
    b: Math.round(hueToRgb(p, q, h - 1 / 3) * 255),
  }
}

/**
 * Convert HEX to HSL
 * @param hex - HEX color string
 * @returns HSL object or null if invalid
 */
export function hexToHsl(hex: string): HSL | null {
  const rgb = hexToRgb(hex)
  if (!rgb) return null
  return rgbToHsl(rgb)
}

/**
 * Convert HSL to HEX
 * @param hsl - HSL object
 * @returns HEX color string
 */
export function hslToHex(hsl: HSL): string {
  return rgbToHex(hslToRgb(hsl))
}

// ============================================================================
// Palette Generation
// ============================================================================

/**
 * Generate a full color palette from a single primary color
 * Creates 11 shades (50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950)
 *
 * @param primaryHex - Primary color in HEX format
 * @returns ColorPalette object with all shades
 */
export function generatePalette(primaryHex: string): ColorPalette | null {
  const hsl = hexToHsl(primaryHex)
  if (!hsl) return null

  // Lightness values for each shade (Tailwind CSS inspired)
  const lightnessMap: Record<keyof ColorPalette, number> = {
    50: 97,
    100: 94,
    200: 86,
    300: 76,
    400: 62,
    500: 50, // Base - will be adjusted to match input
    600: 42,
    700: 34,
    800: 26,
    900: 18,
    950: 10,
  }

  // Saturation adjustments (reduce saturation at extremes)
  const saturationMultiplier: Record<keyof ColorPalette, number> = {
    50: 0.3,
    100: 0.4,
    200: 0.6,
    300: 0.8,
    400: 0.95,
    500: 1.0,
    600: 1.0,
    700: 0.95,
    800: 0.85,
    900: 0.7,
    950: 0.5,
  }

  const palette = {} as ColorPalette
  const shades = Object.keys(lightnessMap) as (keyof ColorPalette)[]

  for (const shade of shades) {
    const adjustedHsl: HSL = {
      h: hsl.h,
      s: Math.min(100, hsl.s * saturationMultiplier[shade]),
      l: lightnessMap[shade],
    }
    palette[shade] = hslToHex(adjustedHsl)
  }

  return palette
}

/**
 * Generate a palette optimized for dark themes
 * Produces colors that work well on dark backgrounds
 *
 * @param primaryHex - Primary color in HEX format
 * @returns ColorPalette object optimized for dark mode
 */
export function generateDarkPalette(primaryHex: string): ColorPalette | null {
  const hsl = hexToHsl(primaryHex)
  if (!hsl) return null

  // Adjusted lightness for dark theme (lighter shades are more vibrant)
  const lightnessMap: Record<keyof ColorPalette, number> = {
    50: 95,
    100: 90,
    200: 82,
    300: 70,
    400: 58,
    500: 48,
    600: 40,
    700: 32,
    800: 24,
    900: 16,
    950: 8,
  }

  // Higher saturation for dark mode visibility
  const saturationMultiplier: Record<keyof ColorPalette, number> = {
    50: 0.4,
    100: 0.5,
    200: 0.7,
    300: 0.85,
    400: 1.0,
    500: 1.0,
    600: 0.95,
    700: 0.85,
    800: 0.7,
    900: 0.5,
    950: 0.3,
  }

  const palette = {} as ColorPalette
  const shades = Object.keys(lightnessMap) as (keyof ColorPalette)[]

  for (const shade of shades) {
    const adjustedHsl: HSL = {
      h: hsl.h,
      s: Math.min(100, Math.max(20, hsl.s * saturationMultiplier[shade])),
      l: lightnessMap[shade],
    }
    palette[shade] = hslToHex(adjustedHsl)
  }

  return palette
}

// ============================================================================
// Contrast & Accessibility
// ============================================================================

/**
 * Calculate relative luminance of a color
 * Based on WCAG 2.1 specification
 *
 * @param rgb - RGB color object
 * @returns Relative luminance (0-1)
 */
export function getRelativeLuminance(rgb: RGB): number {
  const sRGB = [rgb.r / 255, rgb.g / 255, rgb.b / 255]

  const luminance = sRGB.map((c) => {
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  })

  return 0.2126 * luminance[0] + 0.7152 * luminance[1] + 0.0722 * luminance[2]
}

/**
 * Calculate WCAG contrast ratio between two colors
 *
 * @param color1 - First color in HEX format
 * @param color2 - Second color in HEX format
 * @returns Contrast ratio (1-21)
 */
export function getContrastRatio(color1: string, color2: string): number | null {
  const rgb1 = hexToRgb(color1)
  const rgb2 = hexToRgb(color2)

  if (!rgb1 || !rgb2) return null

  const l1 = getRelativeLuminance(rgb1)
  const l2 = getRelativeLuminance(rgb2)

  const lighter = Math.max(l1, l2)
  const darker = Math.min(l1, l2)

  return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Check if color combination meets WCAG AA standard
 * AA requires 4.5:1 for normal text, 3:1 for large text
 *
 * @param foreground - Foreground color in HEX
 * @param background - Background color in HEX
 * @param isLargeText - Whether text is large (18pt+ or 14pt+ bold)
 * @returns Whether combination meets AA standard
 */
export function meetsWcagAA(
  foreground: string,
  background: string,
  isLargeText = false,
): boolean {
  const ratio = getContrastRatio(foreground, background)
  if (ratio === null) return false
  return isLargeText ? ratio >= 3 : ratio >= 4.5
}

/**
 * Check if color combination meets WCAG AAA standard
 * AAA requires 7:1 for normal text, 4.5:1 for large text
 *
 * @param foreground - Foreground color in HEX
 * @param background - Background color in HEX
 * @param isLargeText - Whether text is large
 * @returns Whether combination meets AAA standard
 */
export function meetsWcagAAA(
  foreground: string,
  background: string,
  isLargeText = false,
): boolean {
  const ratio = getContrastRatio(foreground, background)
  if (ratio === null) return false
  return isLargeText ? ratio >= 4.5 : ratio >= 7
}

/**
 * Find the best contrasting color (black or white) for given background
 *
 * @param backgroundColor - Background color in HEX
 * @returns '#000000' or '#ffffff'
 */
export function getContrastingTextColor(backgroundColor: string): string {
  const rgb = hexToRgb(backgroundColor)
  if (!rgb) return '#000000'

  const luminance = getRelativeLuminance(rgb)
  return luminance > 0.179 ? '#000000' : '#ffffff'
}

// ============================================================================
// Color Validation
// ============================================================================

/**
 * Validate if string is a valid HEX color
 *
 * @param color - Color string to validate
 * @returns Whether color is valid HEX format
 */
export function isValidHex(color: string): boolean {
  return /^#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$/.test(color)
}

/**
 * Normalize HEX color to 6-digit format with #
 *
 * @param hex - HEX color (3 or 6 digit, with or without #)
 * @returns Normalized HEX color or null if invalid
 */
export function normalizeHex(hex: string): string | null {
  if (!isValidHex(hex)) return null

  let sanitized = hex.replace(/^#/, '')

  // Expand 3-digit hex to 6-digit
  if (sanitized.length === 3) {
    sanitized = sanitized
      .split('')
      .map(c => c + c)
      .join('')
  }

  return `#${sanitized.toLowerCase()}`
}

// ============================================================================
// Color Manipulation
// ============================================================================

/**
 * Lighten a color by percentage
 *
 * @param hex - HEX color
 * @param amount - Percentage to lighten (0-100)
 * @returns Lightened HEX color
 */
export function lighten(hex: string, amount: number): string | null {
  const hsl = hexToHsl(hex)
  if (!hsl) return null

  const newL = Math.min(100, hsl.l + amount)
  return hslToHex({ ...hsl, l: newL })
}

/**
 * Darken a color by percentage
 *
 * @param hex - HEX color
 * @param amount - Percentage to darken (0-100)
 * @returns Darkened HEX color
 */
export function darken(hex: string, amount: number): string | null {
  const hsl = hexToHsl(hex)
  if (!hsl) return null

  const newL = Math.max(0, hsl.l - amount)
  return hslToHex({ ...hsl, l: newL })
}

/**
 * Adjust saturation of a color
 *
 * @param hex - HEX color
 * @param amount - Amount to adjust (-100 to 100)
 * @returns Adjusted HEX color
 */
export function saturate(hex: string, amount: number): string | null {
  const hsl = hexToHsl(hex)
  if (!hsl) return null

  const newS = Math.max(0, Math.min(100, hsl.s + amount))
  return hslToHex({ ...hsl, s: newS })
}

/**
 * Mix two colors together
 *
 * @param color1 - First HEX color
 * @param color2 - Second HEX color
 * @param weight - Weight of first color (0-1, default 0.5)
 * @returns Mixed HEX color
 */
export function mix(color1: string, color2: string, weight = 0.5): string | null {
  const rgb1 = hexToRgb(color1)
  const rgb2 = hexToRgb(color2)

  if (!rgb1 || !rgb2) return null

  const w = Math.max(0, Math.min(1, weight))

  return rgbToHex({
    r: Math.round(rgb1.r * w + rgb2.r * (1 - w)),
    g: Math.round(rgb1.g * w + rgb2.g * (1 - w)),
    b: Math.round(rgb1.b * w + rgb2.b * (1 - w)),
  })
}

/**
 * Create a transparent version of a color (returns rgba string)
 *
 * @param hex - HEX color
 * @param alpha - Alpha value (0-1)
 * @returns RGBA color string
 */
export function withAlpha(hex: string, alpha: number): string | null {
  const rgb = hexToRgb(hex)
  if (!rgb) return null

  const a = Math.max(0, Math.min(1, alpha))
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${a})`
}

// ============================================================================
// OKLCH Color Space Conversions
// ============================================================================

/**
 * Linear RGB → Oklab 변환 행렬
 */
function linearRgbToOklab(r: number, g: number, b: number): Oklab {
  // sRGB to linear RGB
  const toLinear = (c: number): number => {
    return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  }

  const lr = toLinear(r)
  const lg = toLinear(g)
  const lb = toLinear(b)

  // Linear RGB to LMS
  const l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
  const m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
  const s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)

  // LMS to Oklab
  return {
    L: 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
    a: 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
    b: 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
  }
}

/**
 * Oklab → Linear RGB 변환
 */
function oklabToLinearRgb(L: number, a: number, b: number): RGB {
  // Oklab to LMS
  const l = Math.pow(L + 0.3963377774 * a + 0.2158037573 * b, 3)
  const m = Math.pow(L - 0.1055613458 * a - 0.0638541728 * b, 3)
  const s = Math.pow(L - 0.0894841775 * a - 1.2914855480 * b, 3)

  // LMS to linear RGB
  const lr = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
  const lg = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
  const lb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

  // Linear RGB to sRGB
  const toSrgb = (c: number): number => {
    const clamped = Math.max(0, Math.min(1, c))
    return clamped <= 0.0031308
      ? clamped * 12.92
      : 1.055 * Math.pow(clamped, 1 / 2.4) - 0.055
  }

  return {
    r: Math.round(toSrgb(lr) * 255),
    g: Math.round(toSrgb(lg) * 255),
    b: Math.round(toSrgb(lb) * 255),
  }
}

/**
 * Oklab → OKLCH 변환
 */
function oklabToOklch(lab: Oklab): OKLCH {
  const c = Math.sqrt(lab.a * lab.a + lab.b * lab.b)
  let h = Math.atan2(lab.b, lab.a) * (180 / Math.PI)
  if (h < 0) h += 360

  return {
    l: lab.L,
    c,
    h: c < 0.0001 ? 0 : h, // 무채색인 경우 hue는 0
  }
}

/**
 * OKLCH → Oklab 변환
 */
function oklchToOklab(lch: OKLCH): Oklab {
  const hRad = lch.h * (Math.PI / 180)
  return {
    L: lch.l,
    a: lch.c * Math.cos(hRad),
    b: lch.c * Math.sin(hRad),
  }
}

/**
 * HEX → OKLCH 변환
 * @param hex - HEX 색상 문자열
 * @returns OKLCH 객체 또는 null
 */
export function hexToOklch(hex: string): OKLCH | null {
  const rgb = hexToRgb(hex)
  if (!rgb) return null

  const oklab = linearRgbToOklab(rgb.r / 255, rgb.g / 255, rgb.b / 255)
  return oklabToOklch(oklab)
}

/**
 * OKLCH → HEX 변환
 * @param oklch - OKLCH 색상 객체
 * @returns HEX 색상 문자열
 */
export function oklchToHex(oklch: OKLCH): string {
  const oklab = oklchToOklab(oklch)
  const rgb = oklabToLinearRgb(oklab.L, oklab.a, oklab.b)
  return rgbToHex(rgb)
}

/**
 * OKLCH → CSS oklch() 문자열 변환
 * @param oklch - OKLCH 색상 객체
 * @param precision - 소수점 자릿수 (기본 3)
 * @returns CSS oklch() 문자열
 */
export function oklchToCss(oklch: OKLCH, precision = 3): string {
  const l = oklch.l.toFixed(precision)
  const c = oklch.c.toFixed(precision)
  const h = oklch.h.toFixed(1)
  return `oklch(${l} ${c} ${h})`
}

/**
 * CSS oklch() 문자열 파싱
 * @param css - CSS oklch() 문자열
 * @returns OKLCH 객체 또는 null
 */
export function parseOklchCss(css: string): OKLCH | null {
  const match = css.match(/oklch\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*\)/)
  if (!match) return null

  return {
    l: parseFloat(match[1]),
    c: parseFloat(match[2]),
    h: parseFloat(match[3]),
  }
}

/**
 * HEX → CSS oklch() 문자열 변환 (편의 함수)
 * @param hex - HEX 색상 문자열
 * @returns CSS oklch() 문자열 또는 null
 */
export function hexToOklchCss(hex: string): string | null {
  const oklch = hexToOklch(hex)
  if (!oklch) return null
  return oklchToCss(oklch)
}

// ============================================================================
// OKLCH-based Palette Generation
// ============================================================================

/**
 * OKLCH 기반 12단계 색상 스케일 생성 (Radix UI 패턴)
 *
 * 스케일 역할:
 * - 1-2: 배경색 (가장 밝음/어두움)
 * - 3-5: UI 요소 배경
 * - 6-8: 테두리
 * - 9-12: 텍스트 및 솔리드 배경
 *
 * @param hue - 색조 (0-360)
 * @param chroma - 채도 (0-0.4, 일반적으로 0.1-0.2)
 * @param isDark - 다크모드 여부
 * @returns 12단계 OKLCH CSS 문자열 배열
 */
export function generateOklchScale12(
  hue: number,
  chroma: number,
  isDark = false,
): ColorScale12 {
  // 라이트모드: 밝음(1) → 어두움(12)
  // 다크모드: 어두움(1) → 밝음(12)
  const lightModeLightness = [
    0.99, // 1: App background
    0.97, // 2: Subtle background
    0.94, // 3: UI element bg
    0.91, // 4: Hovered UI element
    0.88, // 5: Active UI element
    0.84, // 6: Subtle borders
    0.77, // 7: UI borders
    0.65, // 8: Hovered borders
    0.55, // 9: Solid backgrounds
    0.50, // 10: Hovered solid
    0.40, // 11: Low-contrast text
    0.20, // 12: High-contrast text
  ]

  const darkModeLightness = [
    0.12, // 1: App background
    0.15, // 2: Subtle background
    0.18, // 3: UI element bg
    0.21, // 4: Hovered UI element
    0.24, // 5: Active UI element
    0.28, // 6: Subtle borders
    0.35, // 7: UI borders
    0.45, // 8: Hovered borders
    0.55, // 9: Solid backgrounds
    0.60, // 10: Hovered solid
    0.75, // 11: Low-contrast text
    0.93, // 12: High-contrast text
  ]

  // 단계별 chroma 조정 (밝거나 어두운 색상은 채도 감소)
  const chromaMultiplier = [
    0.05, 0.08, 0.15, 0.25, 0.35,
    0.45, 0.60, 0.80, 1.00, 0.95,
    0.70, 0.30,
  ]

  const lightness = isDark ? darkModeLightness : lightModeLightness
  const scale = {} as ColorScale12

  for (let i = 0; i < 12; i++) {
    const step = (i + 1) as keyof ColorScale12
    const l = lightness[i]
    const c = chroma * chromaMultiplier[i]
    scale[step] = oklchToCss({ l, c, h: hue })
  }

  return scale
}

/**
 * Gray 스케일 생성 (무채색)
 * @param isDark - 다크모드 여부
 * @returns 12단계 OKLCH CSS 문자열 배열
 */
export function generateGrayScale12(isDark = false): ColorScale12 {
  return generateOklchScale12(0, 0, isDark)
}

/**
 * 기존 HEX 색상에서 OKLCH 12단계 스케일 생성
 * @param hex - 기준 HEX 색상 (보통 primary-500에 해당)
 * @param isDark - 다크모드 여부
 * @returns 12단계 OKLCH CSS 문자열 배열
 */
export function generateOklchScaleFromHex(hex: string, isDark = false): ColorScale12 | null {
  const oklch = hexToOklch(hex)
  if (!oklch) return null

  return generateOklchScale12(oklch.h, oklch.c, isDark)
}

// ============================================================================
// OKLCH Contrast & Accessibility
// ============================================================================

/**
 * OKLCH 기반 대비율 추정
 * OKLCH의 L(lightness)은 지각적으로 균일하므로,
 * ΔL ≥ 0.40 ≈ APCA Lc ≥ 60 (권장 대비)
 *
 * @param color1 - 첫 번째 OKLCH 색상
 * @param color2 - 두 번째 OKLCH 색상
 * @returns Lightness 차이 (0-1)
 */
export function getOklchLightnessDiff(color1: OKLCH, color2: OKLCH): number {
  return Math.abs(color1.l - color2.l)
}

/**
 * OKLCH 기반 접근성 대비 체크
 * @param foreground - 전경색 (HEX 또는 OKLCH)
 * @param background - 배경색 (HEX 또는 OKLCH)
 * @param isLargeText - 큰 텍스트 여부
 * @returns 대비 충분 여부
 */
export function checkOklchContrast(
  foreground: string | OKLCH,
  background: string | OKLCH,
  isLargeText = false,
): { sufficient: boolean, lightnessDiff: number, recommendation: string } {
  const fg = typeof foreground === 'string' ? hexToOklch(foreground) : foreground
  const bg = typeof background === 'string' ? hexToOklch(background) : background

  if (!fg || !bg) {
    return { sufficient: false, lightnessDiff: 0, recommendation: '유효하지 않은 색상' }
  }

  const diff = getOklchLightnessDiff(fg, bg)

  // APCA 기준 근사값
  // 일반 텍스트: ΔL ≥ 0.40
  // 큰 텍스트: ΔL ≥ 0.30
  const threshold = isLargeText ? 0.30 : 0.40
  const sufficient = diff >= threshold

  let recommendation = ''
  if (!sufficient) {
    const needed = threshold - diff
    const lighterFg = fg.l < bg.l
    if (lighterFg) {
      recommendation = `전경색 L을 ${(fg.l - needed).toFixed(2)}로 낮추거나 배경색 L을 ${(bg.l + needed).toFixed(2)}로 높이세요`
    }
    else {
      recommendation = `전경색 L을 ${(fg.l + needed).toFixed(2)}로 높이거나 배경색 L을 ${(bg.l - needed).toFixed(2)}로 낮추세요`
    }
  }

  return { sufficient, lightnessDiff: diff, recommendation }
}

/**
 * 테마 전환용 OKLCH Lightness 조정
 * 다크모드 전환 시 L값만 조정하여 일관된 색상 유지
 *
 * @param oklch - 원본 OKLCH 색상
 * @param targetL - 목표 Lightness (0-1)
 * @returns 조정된 OKLCH CSS 문자열
 */
export function adjustOklchLightness(oklch: OKLCH, targetL: number): string {
  return oklchToCss({
    l: Math.max(0, Math.min(1, targetL)),
    c: oklch.c,
    h: oklch.h,
  })
}

/**
 * 다크모드 색상 자동 생성
 * 라이트모드 색상을 받아 다크모드용으로 L값 반전
 *
 * @param lightModeOklch - 라이트모드 OKLCH 색상
 * @returns 다크모드용 OKLCH CSS 문자열
 */
export function toDarkMode(lightModeOklch: OKLCH): string {
  // L값 반전: 밝은 색 → 어두운 색, 어두운 색 → 밝은 색
  const darkL = 1 - lightModeOklch.l

  // 다크모드에서는 채도를 약간 높여 가독성 향상
  const adjustedC = Math.min(0.3, lightModeOklch.c * 1.1)

  return oklchToCss({
    l: darkL,
    c: adjustedC,
    h: lightModeOklch.h,
  })
}
