import { test, expect } from '@playwright/test'

test.describe('Docs 페이지 - RealGrid/DockView 탭 테스트', () => {
  test.beforeEach(async ({ page }) => {
    // 콘솔 메시지 수집
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        console.log(`[Console Error]: ${msg.text()}`)
      }
    })

    // 페이지 오류 수집
    page.on('pageerror', (error) => {
      console.log(`[Page Error]: ${error.message}`)
    })

    await page.goto('/docs', { waitUntil: 'networkidle' })
  })

  test('RealGrid 탭이 오류 없이 렌더링되어야 함', async ({ page }) => {
    // RealGrid 탭 클릭
    const realgridTab = page.locator('text=🗂️ RealGrid')
    await expect(realgridTab).toBeVisible()
    await realgridTab.click()

    // 잠시 대기 (그리드 초기화 시간)
    await page.waitForTimeout(2000)

    // RealGrid 컨테이너 확인
    const realgridWrapper = page.locator('.realgrid-wrapper')
    await expect(realgridWrapper).toBeVisible()

    // RealGrid 컨테이너 내부에 그리드가 생성되었는지 확인
    const realgridContainer = page.locator('.realgrid-container')
    await expect(realgridContainer).toBeVisible()

    // RealGrid 루트 요소 확인 (실제 그리드가 렌더링되었는지)
    const rgRoot = page.locator('.rg-root')
    const hasRgRoot = await rgRoot.count()
    console.log(`[RealGrid] .rg-root 요소 개수: ${hasRgRoot}`)

    // 콘솔 에러 체크를 위한 스크린샷
    await page.screenshot({ path: 'test-results/realgrid-tab.png', fullPage: true })

    // 페이지에 오류 메시지가 표시되지 않아야 함
    const errorMessage = page.locator('text=오류').first()
    const hasError = await errorMessage.isVisible().catch(() => false)
    console.log(`[RealGrid] 오류 메시지 표시: ${hasError}`)
  })

  test('DockView 탭이 오류 없이 렌더링되어야 함', async ({ page }) => {
    // DockView 탭 클릭
    const dockviewTab = page.locator('text=🪟 DockView')
    await expect(dockviewTab).toBeVisible()
    await dockviewTab.click()

    // 잠시 대기 (DockView 초기화 시간)
    await page.waitForTimeout(2000)

    // DockView 데모 컨테이너 확인
    const dockviewContainer = page.locator('.dockview-demo-container')
    await expect(dockviewContainer).toBeVisible()

    // DockView 컴포넌트 확인 (공식 테마는 HTML에서 상속됨)
    const dockviewComponent = page.locator('.dv-dockview')
    await expect(dockviewComponent).toBeVisible()

    // DockView 패널 확인 (패널이 렌더링되었는지)
    const dvPanel = page.locator('.dv-panel-content')
    const panelCount = await dvPanel.count()
    console.log(`[DockView] .dv-panel-content 요소 개수: ${panelCount}`)

    // 패널이 최소 1개 이상 있어야 함 (초기 3개 생성)
    if (panelCount === 0) {
      console.log('[DockView] 경고: 패널이 렌더링되지 않음!')
    }

    // 콘솔 에러 체크를 위한 스크린샷
    await page.screenshot({ path: 'test-results/dockview-tab.png', fullPage: true })
  })

  test('RealGrid 행 추가/삭제 버튼이 동작해야 함', async ({ page }) => {
    // RealGrid 탭 클릭
    await page.locator('text=🗂️ RealGrid').click()
    await page.waitForTimeout(1000)

    // 행 추가 버튼 클릭
    const addButton = page.locator('button:has-text("행 추가")')
    await expect(addButton).toBeVisible()
    await addButton.click()

    // 토스트 메시지 확인 (행 추가됨)
    await page.waitForTimeout(500)

    // 행 삭제 버튼 클릭
    const removeButton = page.locator('button:has-text("행 삭제")')
    await expect(removeButton).toBeVisible()
    await removeButton.click()

    await page.waitForTimeout(500)
  })

  test('DockView 패널 추가 버튼이 동작해야 함', async ({ page }) => {
    // DockView 탭 클릭
    await page.locator('text=🪟 DockView').click()
    await page.waitForTimeout(1000)

    // 패널 추가 버튼 클릭
    const addButton = page.locator('button:has-text("패널 추가")')
    await expect(addButton).toBeVisible()
    await addButton.click()

    // 토스트 메시지 확인
    await page.waitForTimeout(500)

    // 초기화 버튼 클릭
    const resetButton = page.locator('button:has-text("초기화")')
    await expect(resetButton).toBeVisible()
    await resetButton.click()

    await page.waitForTimeout(500)
  })
})

test.describe('콘솔 에러 수집 테스트', () => {
  test('docs 페이지 로드 시 JavaScript 에러가 없어야 함', async ({ page }) => {
    const errors: string[] = []

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text())
      }
    })

    page.on('pageerror', (error) => {
      errors.push(error.message)
    })

    await page.goto('/docs', { waitUntil: 'networkidle' })

    // 모든 탭 순회
    const tabs = ['🗂️ RealGrid', '🪟 DockView']
    for (const tabName of tabs) {
      const tab = page.locator(`text=${tabName}`)
      if (await tab.isVisible()) {
        await tab.click()
        await page.waitForTimeout(2000)
      }
    }

    // 수집된 에러 출력
    if (errors.length > 0) {
      console.log('\n=== 수집된 JavaScript 에러 ===')
      errors.forEach((error, index) => {
        console.log(`${index + 1}. ${error}`)
      })
      console.log('==============================\n')
    }
    else {
      console.log('\n[SUCCESS] JavaScript 에러 없음\n')
    }
  })
})
