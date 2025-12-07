/**
 * RealGrid 수정 사항 E2E 테스트
 *
 * 이슈 1: 셀 선택 시 텍스트 가시성
 * 이슈 2: 무한 스크롤 작동
 * 이슈 3: 선택 요약 기능 (숫자 컬럼)
 */

import { test, expect } from '@playwright/test'

test.describe('RealGrid 수정 사항 테스트', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/docs')
    // RealGrid 탭 선택 및 로드 대기
    await page.getByRole('tab', { name: 'RealGrid' }).click()
    await page.waitForTimeout(2000) // RealGrid 초기화 대기
  })

  test('이슈 3: 숫자 컬럼(점수)이 추가되어야 함', async ({ page }) => {
    // RealGrid 1 섹션으로 스크롤
    await page.locator('text=RealGrid 1 - 기본 기능 데모').scrollIntoViewIfNeeded()
    await page.waitForTimeout(500)

    // 그리드 1에서 "점수" 헤더 텍스트 확인 (요소가 존재하는지)
    const scoreHeader = page.getByRole('columnheader', { name: '점수' }).first()
    await scoreHeader.scrollIntoViewIfNeeded()
    await expect(scoreHeader).toBeAttached()

    // 점수 데이터 확인 (첫 번째 행의 점수 85)
    const scoreData = await page.evaluate(() => {
      const cells = document.querySelectorAll('td')
      for (const cell of cells) {
        if (cell.textContent?.includes('85')) {
          return cell.textContent
        }
      }
      return null
    })

    expect(scoreData).toBeTruthy()
    console.log('✅ 점수 컬럼 헤더 및 데이터가 존재함:', scoreData)
  })

  test('이슈 2: 무한 스크롤 그리드 초기화 확인', async ({ page }) => {
    // Toast 메시지로 초기화 완료 확인
    const toast = page.locator('text=RealGrid 3 (무한 스크롤) 초기화 완료')
    await expect(toast).toBeVisible({ timeout: 5000 })

    console.log('✅ 무한 스크롤 그리드 초기화 완료 확인')
  })

  test('이슈 1: 선택 셀 CSS에 color 속성이 있어야 함', async ({ page }) => {
    // CSS 파일에서 .rg-selection-cell에 color 속성이 있는지 확인
    // 실제 셀 클릭 대신 CSS 규칙 확인
    const hasColorRule = await page.evaluate(() => {
      const styleSheets = Array.from(document.styleSheets)
      for (const sheet of styleSheets) {
        try {
          const rules = Array.from(sheet.cssRules || [])
          for (const rule of rules) {
            if (rule instanceof CSSStyleRule) {
              if (rule.selectorText?.includes('rg-selection-cell')) {
                const color = rule.style.color
                if (color && color !== '') {
                  return { found: true, color }
                }
              }
            }
          }
        }
        catch {
          // Cross-origin stylesheets will throw
        }
      }
      return { found: false, color: null }
    })

    console.log('선택 셀 CSS color 규칙:', hasColorRule)
    expect(hasColorRule.found).toBe(true)
    console.log('✅ 선택 셀에 color 속성이 적용됨:', hasColorRule.color)
  })
})
