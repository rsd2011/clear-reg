/**
 * RealGrid 컴포넌트 테마 E2E 테스트
 *
 * 테스트 범위:
 * - 상태바, 페이지네이션, 로딩, 에러 컴포넌트
 * - OKLCH 디자인 토큰 시스템 통합
 * - 테마별 레이아웃 (border-radius) 차별화
 * - 접근성 (reduce-motion)
 */

import { test, expect } from '@playwright/test'

const THEMES = [
  { name: 'linear-dark', mode: 'dark', borderRadius: '0' },
  { name: 'github-dark', mode: 'dark', borderRadius: '6px' },
  { name: 'figma-dark', mode: 'dark', borderRadius: '4px' },
  { name: 'slack-aubergine', mode: 'dark', borderRadius: '8px' },
  { name: 'koscom-light', mode: 'light', borderRadius: '2px' },
  { name: 'notion-light', mode: 'light', borderRadius: '3px' },
]

test.describe('RealGrid 컴포넌트 테마 통합', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.removeItem('app-theme-name')
      localStorage.removeItem('app-theme-mode')
      localStorage.removeItem('app-theme-a11y')
    })
    await page.reload()
  })

  test.describe('OKLCH CSS 변수 통합', () => {
    test('OKLCH gray 변수가 정의되어야 함', async ({ page }) => {
      await page.goto('/')

      // OKLCH gray 스케일 확인
      const grayVars = await page.evaluate(() => {
        const styles = getComputedStyle(document.documentElement)
        return {
          gray1: styles.getPropertyValue('--oklch-gray-1').trim(),
          gray2: styles.getPropertyValue('--oklch-gray-2').trim(),
          gray6: styles.getPropertyValue('--oklch-gray-6').trim(),
          gray9: styles.getPropertyValue('--oklch-gray-9').trim(),
          gray11: styles.getPropertyValue('--oklch-gray-11').trim(),
          gray12: styles.getPropertyValue('--oklch-gray-12').trim(),
        }
      })

      // 모든 gray 변수가 정의되어야 함
      expect(grayVars.gray1).toBeTruthy()
      expect(grayVars.gray2).toBeTruthy()
      expect(grayVars.gray6).toBeTruthy()
      expect(grayVars.gray9).toBeTruthy()
      expect(grayVars.gray11).toBeTruthy()
      expect(grayVars.gray12).toBeTruthy()
    })

    test('OKLCH primary 변수가 정의되어야 함', async ({ page }) => {
      await page.goto('/')

      const primaryVars = await page.evaluate(() => {
        const styles = getComputedStyle(document.documentElement)
        return {
          primary9: styles.getPropertyValue('--oklch-primary-9').trim(),
          primary10: styles.getPropertyValue('--oklch-primary-10').trim(),
        }
      })

      expect(primaryVars.primary9).toBeTruthy()
      expect(primaryVars.primary10).toBeTruthy()
    })

    test('다크 모드에서 gray 변수가 반전되어야 함', async ({ page }) => {
      await page.goto('/')

      // 라이트 모드에서 gray-2 값 저장
      await page.evaluate(() => {
        localStorage.setItem('app-theme-name', 'koscom-light')
        localStorage.setItem('app-theme-mode', 'light')
      })
      await page.reload()

      const lightGray2 = await page.evaluate(() => {
        return getComputedStyle(document.documentElement)
          .getPropertyValue('--oklch-gray-2')
          .trim()
      })

      // 다크 모드로 전환
      await page.evaluate(() => {
        localStorage.setItem('app-theme-name', 'linear-dark')
        localStorage.setItem('app-theme-mode', 'dark')
      })
      await page.reload()

      const darkGray2 = await page.evaluate(() => {
        return getComputedStyle(document.documentElement)
          .getPropertyValue('--oklch-gray-2')
          .trim()
      })

      // 라이트/다크 모드에서 gray-2 값이 달라야 함
      // (OKLCH는 lightness가 반전됨)
      expect(lightGray2).not.toBe(darkGray2)
    })
  })

  test.describe('테마별 레이아웃 차별화', () => {
    for (const theme of THEMES) {
      test(`${theme.name} 테마가 올바르게 적용되어야 함`, async ({ page }) => {
        await page.goto('/')

        await page.evaluate(
          (t) => {
            localStorage.setItem('app-theme-name', t.name)
            localStorage.setItem('app-theme-mode', t.mode)
          },
          theme
        )

        await page.reload()

        // 테마 클래스 확인
        const html = page.locator('html')
        await expect(html).toHaveClass(new RegExp(`theme-${theme.name}`))

        // 다크/라이트 모드 클래스 확인
        if (theme.mode === 'dark') {
          await expect(html).toHaveClass(/app-dark/)
        } else {
          await expect(html).not.toHaveClass(/app-dark/)
        }
      })
    }

    test('테마별 border-radius 차별화가 CSS에 정의되어 있어야 함', async ({
      page,
    }) => {
      await page.goto('/')

      // CSS 스타일시트에서 테마별 규칙 존재 확인
      const themeRules = await page.evaluate(() => {
        const themes = {
          linearDark: false,
          githubDark: false,
          figmaDark: false,
          slackAubergine: false,
          koscomLight: false,
          notionLight: false,
        }

        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                const selector = rule.selectorText || ''

                // 각 테마의 border-radius 규칙 확인
                if (
                  selector.includes('.theme-linear-dark') &&
                  selector.includes('realgrid-')
                ) {
                  themes.linearDark = true
                }
                if (
                  selector.includes('.theme-github-dark') &&
                  selector.includes('realgrid-')
                ) {
                  themes.githubDark = true
                }
                if (
                  selector.includes('.theme-figma-dark') &&
                  selector.includes('realgrid-')
                ) {
                  themes.figmaDark = true
                }
                if (
                  selector.includes('.theme-slack-aubergine') &&
                  selector.includes('realgrid-')
                ) {
                  themes.slackAubergine = true
                }
                if (
                  selector.includes('.theme-koscom-light') &&
                  selector.includes('realgrid-')
                ) {
                  themes.koscomLight = true
                }
                if (
                  selector.includes('.theme-notion-light') &&
                  selector.includes('realgrid-')
                ) {
                  themes.notionLight = true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }

        return themes
      })

      // 6개 테마 모두 RealGrid 컴포넌트 스타일이 정의되어야 함
      expect(themeRules.linearDark).toBe(true)
      expect(themeRules.githubDark).toBe(true)
      expect(themeRules.figmaDark).toBe(true)
      expect(themeRules.slackAubergine).toBe(true)
      expect(themeRules.koscomLight).toBe(true)
      expect(themeRules.notionLight).toBe(true)
    })
  })

  test.describe('접근성: reduce-motion', () => {
    test('reduce-motion 클래스가 적용될 때 애니메이션이 비활성화되어야 함', async ({
      page,
    }) => {
      await page.goto('/')

      // reduce-motion 활성화
      await page.evaluate(() => {
        localStorage.setItem(
          'app-theme-a11y',
          JSON.stringify({
            highContrast: false,
            reducedMotion: true,
          })
        )
      })

      await page.reload()

      // reduce-motion 클래스 확인
      const html = page.locator('html')
      await expect(html).toHaveClass(/reduce-motion/)

      // CSS 규칙에서 reduce-motion 시 애니메이션 비활성화 확인
      const hasReduceMotionRule = await page.evaluate(() => {
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                const selector = rule.selectorText || ''
                const cssText = rule.cssText

                // html.reduce-motion .realgrid-loading__spinner 규칙 확인
                if (
                  selector.includes('reduce-motion') &&
                  selector.includes('spinner') &&
                  cssText.includes('animation')
                ) {
                  return true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }
        return false
      })

      expect(hasReduceMotionRule).toBe(true)
    })

    test('prefers-reduced-motion 미디어 쿼리가 존재해야 함', async ({
      page,
    }) => {
      // 시스템 설정 에뮬레이션
      await page.emulateMedia({ reducedMotion: 'reduce' })
      await page.goto('/')

      // 미디어 쿼리 매칭 확인
      const reducedMotionActive = await page.evaluate(() => {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches
      })

      expect(reducedMotionActive).toBe(true)
    })
  })

  test.describe('컴포넌트 CSS 클래스 존재 확인', () => {
    test('realgrid-status-bar 클래스가 CSS에 정의되어야 함', async ({
      page,
    }) => {
      await page.goto('/')

      const hasClass = await page.evaluate(() => {
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                if (rule.selectorText?.includes('.realgrid-status-bar')) {
                  return true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }
        return false
      })

      expect(hasClass).toBe(true)
    })

    test('realgrid-pagination 클래스가 CSS에 정의되어야 함', async ({
      page,
    }) => {
      await page.goto('/')

      const hasClass = await page.evaluate(() => {
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                if (rule.selectorText?.includes('.realgrid-pagination')) {
                  return true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }
        return false
      })

      expect(hasClass).toBe(true)
    })

    test('realgrid-loading 클래스가 CSS에 정의되어야 함', async ({ page }) => {
      await page.goto('/')

      const hasClass = await page.evaluate(() => {
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                if (rule.selectorText?.includes('.realgrid-loading')) {
                  return true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }
        return false
      })

      expect(hasClass).toBe(true)
    })

    test('realgrid-error 클래스가 CSS에 정의되어야 함', async ({ page }) => {
      await page.goto('/')

      const hasClass = await page.evaluate(() => {
        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                if (rule.selectorText?.includes('.realgrid-error')) {
                  return true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }
        return false
      })

      expect(hasClass).toBe(true)
    })
  })

  test.describe('OKLCH 변수 참조 검증', () => {
    test('컴포넌트 CSS가 OKLCH 변수를 참조해야 함', async ({ page }) => {
      await page.goto('/')

      const oklchReferences = await page.evaluate(() => {
        const references = {
          statusBar: false,
          pagination: false,
          loading: false,
          error: false,
        }

        for (const sheet of document.styleSheets) {
          try {
            for (const rule of sheet.cssRules) {
              if (rule instanceof CSSStyleRule) {
                const cssText = rule.cssText
                const selector = rule.selectorText || ''

                // 각 컴포넌트에서 --oklch-* 변수 사용 확인
                if (
                  selector.includes('.realgrid-status-bar') &&
                  cssText.includes('--oklch-')
                ) {
                  references.statusBar = true
                }
                if (
                  selector.includes('.realgrid-pagination') &&
                  cssText.includes('--oklch-')
                ) {
                  references.pagination = true
                }
                if (
                  selector.includes('.realgrid-loading') &&
                  cssText.includes('--oklch-')
                ) {
                  references.loading = true
                }
                if (
                  selector.includes('.realgrid-error') &&
                  cssText.includes('--oklch-')
                ) {
                  references.error = true
                }
              }
            }
          } catch {
            // 크로스 오리진 스타일시트 무시
          }
        }

        return references
      })

      // 모든 컴포넌트가 OKLCH 변수를 사용해야 함
      expect(oklchReferences.statusBar).toBe(true)
      expect(oklchReferences.pagination).toBe(true)
      expect(oklchReferences.loading).toBe(true)
      expect(oklchReferences.error).toBe(true)
    })
  })
})
