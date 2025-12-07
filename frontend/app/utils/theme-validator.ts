/**
 * Theme Accessibility Validator
 *
 * 테마 색상 조합의 WCAG 접근성 기준 준수 여부를 검증합니다.
 * - WCAG AA: 일반 텍스트 4.5:1, 큰 텍스트 3:1
 * - WCAG AAA: 일반 텍스트 7:1, 큰 텍스트 4.5:1
 *
 * OKLCH 기반 검증:
 * - APCA (Advanced Perceptual Contrast Algorithm) 근사
 * - ΔL (밝기 차이) ≥ 0.40 → WCAG AA 수준 (Lc ≈ 60)
 * - ΔL ≥ 0.55 → WCAG AAA 수준 (Lc ≈ 75)
 */

import {
  getContrastRatio,
  meetsWcagAA,
  meetsWcagAAA,
  hexToRgb,
  hexToOklch,
  // getOklchLightnessDiff, // 현재 미사용
  checkOklchContrast,
  adjustOklchLightness,
} from './color-utils'
import type { ThemeName, ThemeOklch } from '~/themes'
import {
  THEMES,
  LINEAR_DARK_OKLCH,
  GITHUB_DARK_OKLCH,
  FIGMA_DARK_OKLCH,
  SLACK_AUBERGINE_OKLCH,
  KOSCOM_LIGHT_OKLCH,
  NOTION_LIGHT_OKLCH,
  ELONSOFT_LIGHT_OKLCH,
} from '~/themes'

// ============================================================================
// Types
// ============================================================================

export interface ContrastCheckResult {
  pair: string
  foreground: string
  background: string
  ratio: number
  wcagAA: boolean
  wcagAAA: boolean
  isLargeText: boolean
}

export interface ThemeValidationResult {
  themeName: ThemeName
  valid: boolean
  score: number // 0-100
  checks: ContrastCheckResult[]
  warnings: string[]
  errors: string[]
}

export interface OklchContrastCheckResult {
  pair: string
  foreground: string
  background: string
  lightnessDiff: number
  meetsAA: boolean // ΔL ≥ 0.40
  meetsAAA: boolean // ΔL ≥ 0.55
  recommendation: string
}

export interface OklchValidationResult {
  themeName: ThemeName
  valid: boolean
  score: number // 0-100
  checks: OklchContrastCheckResult[]
  scaleConsistency: {
    hasUniformSteps: boolean
    averageStep: number
    maxDeviation: number
  }
  warnings: string[]
  errors: string[]
}

// ============================================================================
// Constants
// ============================================================================

/** 다크 테마 기본 배경색 (검증용) */
const DARK_BACKGROUNDS = ['#0d1117', '#1a1a1a', '#1e1e1e', '#121212']

/** 라이트 테마 기본 배경색 (검증용) */
const LIGHT_BACKGROUNDS = ['#ffffff', '#f7f6f3', '#fafafa', '#f5f5f5']

/** 테마별 OKLCH 데이터 맵 */
const THEME_OKLCH_MAP: Record<ThemeName, ThemeOklch> = {
  'linear-dark': LINEAR_DARK_OKLCH,
  'github-dark': GITHUB_DARK_OKLCH,
  'figma-dark': FIGMA_DARK_OKLCH,
  'slack-aubergine': SLACK_AUBERGINE_OKLCH,
  'koscom-light': KOSCOM_LIGHT_OKLCH,
  'notion-light': NOTION_LIGHT_OKLCH,
  'elonsoft-light': ELONSOFT_LIGHT_OKLCH,
}

/** OKLCH 밝기 차이 임계값 */
const OKLCH_CONTRAST_THRESHOLDS = {
  AA: 0.40, // WCAG AA 수준 (Lc ≈ 60)
  AAA: 0.55, // WCAG AAA 수준 (Lc ≈ 75)
  OPTIMAL: 0.50, // 최적 가독성
}

// ============================================================================
// Validation Functions
// ============================================================================

/**
 * 단일 색상 쌍의 대비율 검사
 */
export function checkContrast(
  foreground: string,
  background: string,
  pairName: string,
  isLargeText = false,
): ContrastCheckResult {
  const ratio = getContrastRatio(foreground, background)

  return {
    pair: pairName,
    foreground,
    background,
    ratio: ratio ?? 0,
    wcagAA: meetsWcagAA(foreground, background, isLargeText),
    wcagAAA: meetsWcagAAA(foreground, background, isLargeText),
    isLargeText,
  }
}

/**
 * 테마의 접근성 검증
 */
export function validateTheme(themeName: ThemeName): ThemeValidationResult {
  const theme = THEMES[themeName]
  const checks: ContrastCheckResult[] = []
  const warnings: string[] = []
  const errors: string[] = []

  // 배경색 결정
  const backgrounds = theme.prefersDark ? DARK_BACKGROUNDS : LIGHT_BACKGROUNDS
  const primaryBg = backgrounds[0]!

  // 1. Primary 색상 vs 배경 검사
  const primaryColor = theme.accentColors[0]!
  const primaryCheck = checkContrast(primaryColor, primaryBg, 'Primary vs Background')
  checks.push(primaryCheck)

  if (!primaryCheck.wcagAA) {
    errors.push(`Primary 색상(${primaryColor})이 배경(${primaryBg})과 WCAG AA 기준을 충족하지 않습니다. (대비율: ${primaryCheck.ratio.toFixed(2)}:1)`)
  }
  else if (!primaryCheck.wcagAAA) {
    warnings.push(`Primary 색상이 WCAG AAA 기준을 충족하지 않습니다. (대비율: ${primaryCheck.ratio.toFixed(2)}:1)`)
  }

  // 2. Secondary 색상 vs 배경 검사 (있는 경우)
  const secondaryColor = theme.accentColors[1]
  if (secondaryColor) {
    const secondaryCheck = checkContrast(secondaryColor, primaryBg, 'Secondary vs Background')
    checks.push(secondaryCheck)

    if (!secondaryCheck.wcagAA) {
      warnings.push(`Secondary 색상(${secondaryColor})이 WCAG AA 기준을 충족하지 않습니다.`)
    }
  }

  // 3. Tertiary 색상 vs 배경 검사 (있는 경우)
  const tertiaryColor = theme.accentColors[2]
  if (tertiaryColor) {
    const tertiaryCheck = checkContrast(tertiaryColor, primaryBg, 'Tertiary vs Background')
    checks.push(tertiaryCheck)

    if (!tertiaryCheck.wcagAA) {
      warnings.push(`Tertiary 색상(${tertiaryColor})이 WCAG AA 기준을 충족하지 않습니다.`)
    }
  }

  // 4. 텍스트 색상 검사 (일반적인 텍스트 색상 가정)
  const textColor = theme.prefersDark ? '#ffffff' : '#000000'
  const textCheck = checkContrast(textColor, primaryBg, 'Text vs Background')
  checks.push(textCheck)

  // 5. Primary 색상 위의 텍스트 검사
  const textOnPrimaryCheck = checkContrast(
    theme.prefersDark ? '#ffffff' : '#000000',
    primaryColor,
    'Text on Primary',
  )
  checks.push(textOnPrimaryCheck)

  if (!textOnPrimaryCheck.wcagAA) {
    warnings.push(`Primary 색상 위의 텍스트가 읽기 어려울 수 있습니다. (대비율: ${textOnPrimaryCheck.ratio.toFixed(2)}:1)`)
  }

  // 점수 계산 (모든 검사의 AA 통과 비율)
  const aaPassCount = checks.filter(c => c.wcagAA).length
  const score = Math.round((aaPassCount / checks.length) * 100)

  return {
    themeName,
    valid: errors.length === 0,
    score,
    checks,
    warnings,
    errors,
  }
}

/**
 * 모든 테마 검증
 */
export function validateAllThemes(): Map<ThemeName, ThemeValidationResult> {
  const results = new Map<ThemeName, ThemeValidationResult>()

  for (const themeName of Object.keys(THEMES) as ThemeName[]) {
    results.set(themeName, validateTheme(themeName))
  }

  return results
}

/**
 * 개발 모드에서 테마 검증 결과 콘솔 출력
 */
export function logThemeValidation(result: ThemeValidationResult): void {
  const icon = result.valid ? '✅' : '❌'
  const _scoreColor = result.score >= 80 ? 'green' : result.score >= 60 ? 'orange' : 'red'

  console.group(`${icon} Theme: ${result.themeName} (Score: ${result.score}/100)`)

  if (result.errors.length > 0) {
    console.group('❌ Errors')
    result.errors.forEach(e => console.error(e))
    console.groupEnd()
  }

  if (result.warnings.length > 0) {
    console.group('⚠️ Warnings')
    result.warnings.forEach(w => console.warn(w))
    console.groupEnd()
  }

  console.group('📊 Contrast Checks')
  console.table(result.checks.map(c => ({
    'Pair': c.pair,
    'Ratio': `${c.ratio.toFixed(2)}:1`,
    'WCAG AA': c.wcagAA ? '✅' : '❌',
    'WCAG AAA': c.wcagAAA ? '✅' : '❌',
  })))
  console.groupEnd()

  console.groupEnd()
}

/**
 * 개발 모드에서 모든 테마 검증 실행
 */
export function runThemeValidation(): void {
  if (!import.meta.dev) return

  console.group('🎨 Theme Accessibility Validation')

  const results = validateAllThemes()
  let totalScore = 0
  let validCount = 0

  results.forEach((result) => {
    logThemeValidation(result)
    totalScore += result.score
    if (result.valid) validCount++
  })

  const avgScore = Math.round(totalScore / results.size)
  console.log(`\n📈 Summary: ${validCount}/${results.size} themes valid, Average score: ${avgScore}/100`)
  console.groupEnd()
}

/**
 * 색상 추천 (대비율 개선)
 */
export function suggestAccessibleColor(
  originalColor: string,
  backgroundColor: string,
  targetRatio = 4.5,
): string | null {
  const rgb = hexToRgb(originalColor)
  if (!rgb) return null

  // 밝기 조정으로 대비율 개선 시도
  const bgRgb = hexToRgb(backgroundColor)
  if (!bgRgb) return null

  // 배경이 어두우면 색상을 밝게, 밝으면 어둡게
  const bgLuminance = (bgRgb.r * 299 + bgRgb.g * 587 + bgRgb.b * 114) / 1000
  const shouldLighten = bgLuminance < 128

  // 10% 단위로 조정하며 적절한 대비율 찾기
  for (let i = 1; i <= 10; i++) {
    const factor = shouldLighten ? 1 + (i * 0.1) : 1 - (i * 0.1)
    const adjusted = {
      r: Math.min(255, Math.max(0, Math.round(rgb.r * factor))),
      g: Math.min(255, Math.max(0, Math.round(rgb.g * factor))),
      b: Math.min(255, Math.max(0, Math.round(rgb.b * factor))),
    }

    const hex = `#${adjusted.r.toString(16).padStart(2, '0')}${adjusted.g.toString(16).padStart(2, '0')}${adjusted.b.toString(16).padStart(2, '0')}`
    const ratio = getContrastRatio(hex, backgroundColor)

    if (ratio && ratio >= targetRatio) {
      return hex
    }
  }

  return null
}

// ============================================================================
// OKLCH Validation Functions
// ============================================================================

/**
 * 테마의 OKLCH 데이터 가져오기
 */
export function getThemeOklch(themeName: ThemeName): ThemeOklch | null {
  return THEME_OKLCH_MAP[themeName] ?? null
}

/**
 * 12단계 스케일의 밝기 일관성 검사
 * Radix UI 패턴: 각 단계 간 밝기 차이가 균일해야 함
 */
export function checkScaleConsistency(scale: Record<number, string>): {
  hasUniformSteps: boolean
  averageStep: number
  maxDeviation: number
  steps: number[]
} {
  const lightnesses: number[] = []

  // 각 단계의 OKLCH 밝기 추출
  for (let i = 1; i <= 12; i++) {
    const colorValue = scale[i as keyof typeof scale]
    if (colorValue) {
      const oklch = hexToOklch(colorValue)
      if (oklch) {
        lightnesses.push(oklch.l)
      }
    }
  }

  if (lightnesses.length < 12) {
    return {
      hasUniformSteps: false,
      averageStep: 0,
      maxDeviation: 1,
      steps: [],
    }
  }

  // 인접 단계 간 밝기 차이 계산
  const steps: number[] = []
  for (let i = 1; i < lightnesses.length; i++) {
    const curr = lightnesses[i]!
    const prev = lightnesses[i - 1]!
    steps.push(Math.abs(curr - prev))
  }

  const averageStep = steps.reduce((a, b) => a + b, 0) / steps.length
  const maxDeviation = Math.max(...steps.map(s => Math.abs(s - averageStep)))

  // 편차가 평균의 50% 이내면 균일하다고 판단
  const hasUniformSteps = maxDeviation <= averageStep * 0.5

  return {
    hasUniformSteps,
    averageStep,
    maxDeviation,
    steps,
  }
}

/**
 * OKLCH 기반 테마 검증
 */
export function validateThemeOklch(themeName: ThemeName): OklchValidationResult {
  const oklchData = getThemeOklch(themeName)
  const theme = THEMES[themeName]
  const checks: OklchContrastCheckResult[] = []
  const warnings: string[] = []
  const errors: string[] = []

  if (!oklchData) {
    return {
      themeName,
      valid: false,
      score: 0,
      checks: [],
      scaleConsistency: {
        hasUniformSteps: false,
        averageStep: 0,
        maxDeviation: 1,
      },
      warnings: [],
      errors: [`테마 ${themeName}의 OKLCH 데이터가 없습니다.`],
    }
  }

  const { scale } = oklchData
  const _isDark = theme.prefersDark

  // 1. 배경 (1-2) vs 텍스트 (11-12) 대비 검사
  const bgColor = scale[1]
  const textColor = scale[12]
  const bgTextContrast = checkOklchContrast(textColor, bgColor)

  checks.push({
    pair: 'Text vs Background (12 vs 1)',
    foreground: textColor,
    background: bgColor,
    lightnessDiff: bgTextContrast.lightnessDiff,
    meetsAA: bgTextContrast.sufficient,
    meetsAAA: bgTextContrast.lightnessDiff >= OKLCH_CONTRAST_THRESHOLDS.AAA,
    recommendation: bgTextContrast.recommendation,
  })

  if (!bgTextContrast.sufficient) {
    errors.push(`텍스트(${textColor})와 배경(${bgColor})의 대비가 부족합니다. ΔL: ${bgTextContrast.lightnessDiff.toFixed(2)}`)
  }

  // 2. 브랜드 색상 (9) vs 배경 (1) 대비 검사
  const brandColor = scale[9]
  const brandBgContrast = checkOklchContrast(brandColor, bgColor)

  checks.push({
    pair: 'Brand vs Background (9 vs 1)',
    foreground: brandColor,
    background: bgColor,
    lightnessDiff: brandBgContrast.lightnessDiff,
    meetsAA: brandBgContrast.lightnessDiff >= 0.30, // 브랜드 색상은 약간 낮은 기준
    meetsAAA: brandBgContrast.lightnessDiff >= OKLCH_CONTRAST_THRESHOLDS.AA,
    recommendation: brandBgContrast.recommendation,
  })

  if (brandBgContrast.lightnessDiff < 0.25) {
    warnings.push(`브랜드 색상(${brandColor})이 배경과 잘 구분되지 않습니다.`)
  }

  // 3. 보조 텍스트 (11) vs 배경 (1) 대비 검사
  const mutedText = scale[11]
  const mutedBgContrast = checkOklchContrast(mutedText, bgColor)

  checks.push({
    pair: 'Muted Text vs Background (11 vs 1)',
    foreground: mutedText,
    background: bgColor,
    lightnessDiff: mutedBgContrast.lightnessDiff,
    meetsAA: mutedBgContrast.lightnessDiff >= 0.35, // 보조 텍스트는 약간 낮은 기준
    meetsAAA: mutedBgContrast.sufficient,
    recommendation: mutedBgContrast.recommendation,
  })

  // 4. UI 요소 (3-5) vs 배경 (1) 검사
  const uiElement = scale[4]
  const uiBgContrast = checkOklchContrast(uiElement, bgColor)

  checks.push({
    pair: 'UI Element vs Background (4 vs 1)',
    foreground: uiElement,
    background: bgColor,
    lightnessDiff: uiBgContrast.lightnessDiff,
    meetsAA: uiBgContrast.lightnessDiff >= 0.08, // UI 요소는 낮은 기준
    meetsAAA: uiBgContrast.lightnessDiff >= 0.12,
    recommendation: uiBgContrast.lightnessDiff < 0.08 ? 'UI 요소가 배경과 더 구분되어야 합니다.' : '✓',
  })

  // 5. 테두리 (6-8) vs 배경 (1) 검사
  const border = scale[7]
  const borderBgContrast = checkOklchContrast(border, bgColor)

  checks.push({
    pair: 'Border vs Background (7 vs 1)',
    foreground: border,
    background: bgColor,
    lightnessDiff: borderBgContrast.lightnessDiff,
    meetsAA: borderBgContrast.lightnessDiff >= 0.15,
    meetsAAA: borderBgContrast.lightnessDiff >= 0.25,
    recommendation: borderBgContrast.lightnessDiff < 0.15 ? '테두리가 더 눈에 띄어야 합니다.' : '✓',
  })

  // 6. 스케일 일관성 검사
  const scaleConsistency = checkScaleConsistency(scale as unknown as Record<number, string>)

  if (!scaleConsistency.hasUniformSteps) {
    warnings.push(`스케일 단계 간 밝기 차이가 불균일합니다. 최대 편차: ${scaleConsistency.maxDeviation.toFixed(3)}`)
  }

  // 점수 계산
  const aaPassCount = checks.filter(c => c.meetsAA).length
  const score = Math.round((aaPassCount / checks.length) * 100)

  return {
    themeName,
    valid: errors.length === 0,
    score,
    checks,
    scaleConsistency: {
      hasUniformSteps: scaleConsistency.hasUniformSteps,
      averageStep: scaleConsistency.averageStep,
      maxDeviation: scaleConsistency.maxDeviation,
    },
    warnings,
    errors,
  }
}

/**
 * 모든 테마의 OKLCH 검증
 */
export function validateAllThemesOklch(): Map<ThemeName, OklchValidationResult> {
  const results = new Map<ThemeName, OklchValidationResult>()

  for (const themeName of Object.keys(THEMES) as ThemeName[]) {
    results.set(themeName, validateThemeOklch(themeName))
  }

  return results
}

/**
 * OKLCH 검증 결과 콘솔 출력
 */
export function logOklchValidation(result: OklchValidationResult): void {
  const icon = result.valid ? '✅' : '❌'

  console.group(`${icon} OKLCH Theme: ${result.themeName} (Score: ${result.score}/100)`)

  if (result.errors.length > 0) {
    console.group('❌ Errors')
    result.errors.forEach(e => console.error(e))
    console.groupEnd()
  }

  if (result.warnings.length > 0) {
    console.group('⚠️ Warnings')
    result.warnings.forEach(w => console.warn(w))
    console.groupEnd()
  }

  console.group('📊 OKLCH Contrast Checks')
  console.table(result.checks.map(c => ({
    Pair: c.pair,
    ΔL: c.lightnessDiff.toFixed(3),
    AA: c.meetsAA ? '✅' : '❌',
    AAA: c.meetsAAA ? '✅' : '❌',
    Note: c.recommendation,
  })))
  console.groupEnd()

  console.group('📏 Scale Consistency')
  console.log(`Uniform: ${result.scaleConsistency.hasUniformSteps ? '✅' : '❌'}`)
  console.log(`Avg Step: ${result.scaleConsistency.averageStep.toFixed(3)}`)
  console.log(`Max Deviation: ${result.scaleConsistency.maxDeviation.toFixed(3)}`)
  console.groupEnd()

  console.groupEnd()
}

/**
 * 개발 모드에서 모든 테마 OKLCH 검증 실행
 */
export function runOklchValidation(): void {
  if (!import.meta.dev) return

  console.group('🎨 OKLCH Theme Accessibility Validation')

  const results = validateAllThemesOklch()
  let totalScore = 0
  let validCount = 0

  results.forEach((result) => {
    logOklchValidation(result)
    totalScore += result.score
    if (result.valid) validCount++
  })

  const avgScore = Math.round(totalScore / results.size)
  console.log(`\n📈 Summary: ${validCount}/${results.size} themes valid, Average score: ${avgScore}/100`)
  console.groupEnd()
}

/**
 * OKLCH 기반 접근 가능한 색상 추천
 */
export function suggestAccessibleOklchColor(
  originalColor: string,
  backgroundColor: string,
  targetLightnessDiff = OKLCH_CONTRAST_THRESHOLDS.AA,
): string | null {
  const fgOklch = hexToOklch(originalColor)
  const bgOklch = hexToOklch(backgroundColor)

  if (!fgOklch || !bgOklch) return null

  // 배경이 어두우면 밝게, 밝으면 어둡게 조정
  const targetL = bgOklch.l < 0.5
    ? Math.min(1, bgOklch.l + targetLightnessDiff)
    : Math.max(0, bgOklch.l - targetLightnessDiff)

  return adjustOklchLightness(fgOklch, targetL)
}
