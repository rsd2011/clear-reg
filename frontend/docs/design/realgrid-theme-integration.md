# RealGrid OKLCH 테마 통합 설계서

> **문서 버전**: 1.0  
> **작성일**: 2025-12-06  
> **상태**: 검토 대기

---

## 1. 개요

### 1.1 목적
RealGrid 컴포넌트에 앱 테마 시스템(OKLCH 3-Tier Design Token)을 통합하여, 6개 테마 전환 시 RealGrid도 일관된 색상으로 자동 반영되도록 한다.

### 1.2 현재 상태
| 항목 | 상태 | 비고 |
|------|------|------|
| RealGrid | 다크/라이트 전환만 지원 | `realgrid-dark.css` 로드/언로드 |
| 테마 시스템 | 6개 테마, OKLCH 토큰 | `main.css` |
| Dockview | OKLCH 통합 완료 ✅ | 참고 패턴 |

### 1.3 목표 상태
- 테마 전환 시 RealGrid 색상 자동 반영
- Primary 색상이 선택/포커스 상태에 적용
- 아이콘도 테마 색상에 맞게 변경 (CSS mask)

---

## 2. 아키텍처 설계

### 2.1 전략: CSS 변수 오버라이드 (Dockview 동일 패턴)

```
┌─────────────────────────────────────────────────────────┐
│                    main.css 구조                        │
├─────────────────────────────────────────────────────────┤
│ ... 기존 섹션들 ...                                      │
│ ───────────────────────────────────────────────────────│
│ Dockview OKLCH Integration (라인 802-949)              │
│ ───────────────────────────────────────────────────────│
│ RealGrid OKLCH Integration  ← 새로 추가                 │
│   ├── 1️⃣ 공통 변수 레이어                               │
│   ├── 2️⃣ Dark Mode 오버라이드                          │
│   ├── 3️⃣ 테마별 레이아웃 차별화                         │
│   └── 4️⃣ 아이콘 CSS Mask 교체                          │
└─────────────────────────────────────────────────────────┘
```

### 2.2 OKLCH 변수 매핑 설계

```
┌─────────────────────────────────────────────────────────┐
│  RealGrid 영역          →    OKLCH 변수                 │
├─────────────────────────────────────────────────────────┤
│  배경 (Background)                                      │
│  ├── .rg-root           →    --oklch-gray-1            │
│  ├── .rg-header         →    --oklch-gray-2            │
│  ├── .rg-body row       →    --oklch-gray-1            │
│  └── .rg-body alt-row   →    --oklch-gray-2            │
├─────────────────────────────────────────────────────────┤
│  텍스트 (Text)                                          │
│  ├── .rg-root           →    --oklch-gray-12           │
│  ├── .rg-header         →    --oklch-gray-11           │
│  └── summary            →    --oklch-primary-10        │
├─────────────────────────────────────────────────────────┤
│  테두리 (Border)                                        │
│  └── 모든 셀/그리드     →    --oklch-gray-6            │
├─────────────────────────────────────────────────────────┤
│  액센트 (Primary) ⭐                                    │
│  ├── 행 선택            →    --oklch-primary-3         │
│  ├── 셀 포커스          →    --oklch-primary-4         │
│  ├── 포커스 테두리      →    --oklch-primary-9         │
│  └── 체크박스           →    --oklch-primary-9         │
├─────────────────────────────────────────────────────────┤
│  인터랙션 (Interaction)                                 │
│  ├── 호버              →    --oklch-gray-4            │
│  └── 스크롤바          →    --oklch-gray-3~6          │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Phase별 상세 계획

### 3.1 Phase 0 (P0): 기본 색상 통합 - 필수

**목표**: 배경, 텍스트, 테두리 색상을 OKLCH 변수로 통합

**범위**: ~25개 CSS 규칙  
**예상 시간**: 2시간

#### 오버라이드 항목

| # | 셀렉터 | 속성 | 매핑 변수 | 비고 |
|---|--------|------|----------|------|
| 1 | `.rg-root` | background | `--oklch-gray-1` | 전체 배경 |
| 2 | `.rg-root` | color | `--oklch-gray-12` | 전체 텍스트 |
| 3 | `.rg-grid` | border-color | `--oklch-gray-6` | 외곽 테두리 |
| 4 | `.rg-empty-grid` | background | `--oklch-gray-1` | 빈 그리드 |
| 5 | `.rg-empty-grid` | color | `--oklch-gray-12` | 빈 그리드 텍스트 |
| 6 | `.rg-header` | background | `--oklch-gray-2` | 헤더 배경 |
| 7 | `.rg-header` | color | `--oklch-gray-11` | 헤더 텍스트 |
| 8 | `.rg-header-group-cell` | background | `--oklch-gray-3` | 그룹 헤더 |
| 9 | `.rg-header .rg-table tr td` | border-color | `--oklch-gray-6` | 헤더 셀 테두리 |
| 10 | `.rg-body .rg-table tr td` | border-color | `--oklch-gray-6` | 본문 셀 테두리 |
| 11 | `.rg-scrolltrack` | background | `--oklch-gray-3` | 스크롤 트랙 |
| 12 | `.rg-scrollthumb` | background | `--oklch-gray-5` | 스크롤 썸 |
| 13 | `.rg-scrollthumb:hover` | background | `--oklch-gray-6` | 스크롤 호버 |
| 14 | `.rg-header-summary` | background | `--oklch-gray-3` | 서머리 배경 |
| 15 | `.rg-fixed-header-summary` | background | `--oklch-gray-3` | 고정 서머리 |

---

### 3.2 Phase 1 (P1): Primary 색상 통합 - 핵심

**목표**: 선택, 포커스 상태에 테마 Primary 색상 적용

**범위**: ~20개 CSS 규칙  
**예상 시간**: 2시간

#### 오버라이드 항목

| # | 셀렉터 | 속성 | 매핑 변수 | 효과 |
|---|--------|------|----------|------|
| 1 | `.rg-header-select` | background-color | `--oklch-primary-3` | 헤더 선택 |
| 2 | `.rg-header-focus` | background | `--oklch-primary-4` | 헤더 포커스 |
| 3 | `.rg-rowindicator-select` | background-color | `--oklch-primary-3` | 행 인디케이터 선택 |
| 4 | `.rg-rowindicator-focus` | background-color | `--oklch-primary-4` | 행 인디케이터 포커스 |
| 5 | `.rg-statebar-select` | background-color | `--oklch-primary-3` | 상태바 선택 |
| 6 | `.rg-statebar-focus` | background-color | `--oklch-primary-4` | 상태바 포커스 |
| 7 | `.rg-checkbar-select` | background-color | `--oklch-primary-3` | 체크바 선택 |
| 8 | `.rg-checkbar-focus` | background-color | `--oklch-primary-4` | 체크바 포커스 |
| 9 | `.rg-focus` | background-color | `--oklch-primary-4` | 셀 포커스 |
| 10 | `.rg-group-focus` | background-color | `--oklch-primary-4` | 그룹 포커스 |
| 11 | `.rg-deactive-focus` | background-color | `--oklch-primary-3` | 비활성 포커스 |
| 12 | `.rg-focus-line` | border-color | `--oklch-primary-9` | 포커스 테두리 |
| 13 | `.rg-focus-inner` | border-color | `--oklch-primary-8` | 내부 포커스 |
| 14 | `.rg-rowfocus` | background-color | `--oklch-primary-3` | 행 포커스 |
| 15 | `.rg-rowhover` | background-color | `--oklch-gray-4` | 행 호버 |
| 16 | `.rg-selection` | background-color | `--oklch-primary-3` | 셀 선택 영역 |
| 17 | `.rg-selection-cell` | background-color | `--oklch-primary-4` | 선택 셀 |
| 18 | `.rg-header-summary` | color | `--oklch-primary-10` | 서머리 텍스트 |
| 19 | `.rg-fixed-header-summary` | color | `--oklch-primary-10` | 고정 서머리 텍스트 |
| 20 | `input:focus` (에디터) | border-color | `--oklch-primary-7` | 편집 포커스 |

---

### 3.3 Phase 2 (P2): 인터랙션 및 에디터 - 권장

**목표**: 호버, 에디터, 버튼 등 인터랙션 상태 개선

**범위**: ~30개 CSS 규칙  
**예상 시간**: 3시간

#### 오버라이드 항목

| # | 영역 | 셀렉터 | 매핑 변수 |
|---|------|--------|----------|
| 1-3 | 헤더 호버 | `.rg-header .rg-table tr td:hover` | `--oklch-gray-4` |
| 4-6 | 본문 호버 | `.rg-body .rg-table tr td:hover` | `--oklch-gray-4` |
| 7-9 | 스크롤 버튼 | `.rg-scroll-*` | `--oklch-gray-5` |
| 10-12 | 스크롤 버튼 호버 | `.rg-scroll-*:hover` | `--oklch-gray-6` |
| 13-15 | 에디터 배경 | `.rg-editor-*` | `--oklch-gray-2` |
| 16-18 | 에디터 테두리 | `.rg-editor-* input` | `--oklch-gray-6` |
| 19-21 | 버튼 기본 | `.rg-button-*` | `--oklch-gray-5` |
| 22-24 | 버튼 호버 | `.rg-button-*:hover` | `--oklch-gray-6` |
| 25-27 | 버튼 액티브 | `.rg-button-*:active` | `--oklch-primary-7` |
| 28-30 | 필터 패널 | `.rg-filter-panel-*` | `--oklch-gray-2` |

---

### 3.4 Phase 3 (P3): 아이콘 CSS Mask 교체 - 완성도

**목표**: SVG 아이콘을 CSS mask로 교체하여 테마 색상 적용  
**접근법**: **Option C - CSS mask-image 사용** ✅

**범위**: ~45개 CSS 규칙 + SVG 추출  
**예상 시간**: 5시간

#### 아이콘 교체 대상

| # | 아이콘 | 셀렉터 | 색상 변수 |
|---|--------|--------|----------|
| 1 | 정렬 오름차순 | `.rg-header-sort-ascending` | `--oklch-gray-9` |
| 2 | 정렬 내림차순 | `.rg-header-sort-descending` | `--oklch-gray-9` |
| 3 | 정렬 없음 | `.rg-header-sort-none` | `--oklch-gray-9` |
| 4 | 필터 | `.rg-header-filtering` | `--oklch-gray-9` |
| 5 | 필터 활성화 | `.rg-header-filtering.filter-activated` | `--oklch-primary-9` |
| 6 | 팝업 메뉴 | `.rg-header-popup` | `--oklch-gray-9` |
| 7 | 팝업 메뉴 호버 | `.rg-header-popup:hover` | `--oklch-gray-11` |
| 8-10 | 체크박스 (3종) | `.rg-header-check-image.*` | `--oklch-primary-9` |
| 11-14 | 스크롤 화살표 (4종) | `.rg-scroll-*` | `--oklch-gray-9` |
| 15-18 | 트리 확장/축소 | `.rg-tree-*` | `--oklch-gray-9` |
| 19-22 | 그룹 확장/축소 | `.rg-group-*` | `--oklch-gray-9` |
| 23-26 | 에디터 버튼 (4종) | `.rg-button-*` | `--oklch-gray-9` |

#### CSS Mask 구현 패턴

```css
/* 기존 base64 배경 제거 + CSS mask 적용 */
.rg-header-sort-ascending {
  background: none !important;
  background-color: var(--oklch-gray-9) !important;
  -webkit-mask: url("data:image/svg+xml,...") no-repeat center;
  mask: url("data:image/svg+xml,...") no-repeat center;
  -webkit-mask-size: contain;
  mask-size: contain;
}

/* 호버 시 색상 변경 */
.rg-header-sort-ascending:hover {
  background-color: var(--oklch-gray-11) !important;
}

/* 활성화 상태 - Primary 색상 */
.rg-header-filtering.filter-activated {
  background-color: var(--oklch-primary-9) !important;
}
```

#### SVG 추출 작업

1. `realgrid-dark.css`에서 base64 인코딩된 SVG 추출
2. 각 SVG에서 fill 색상을 제거하여 mask용으로 변환
3. 인라인 data URI 또는 외부 파일로 저장

---

## 4. 테마별 레이아웃 차별화

### 4.1 레이아웃 변수 설계

```css
/* Linear Dark - 미니멀 (VS Code 스타일) */
.theme-linear-dark {
  --rg-border-radius: 0px;
  --rg-cell-padding: 4px;
  --rg-header-height: 32px;
  --rg-row-height: 28px;
}

/* GitHub Dark - 클래식 */
.theme-github-dark {
  --rg-border-radius: 6px;
  --rg-cell-padding: 6px;
  --rg-header-height: 36px;
  --rg-row-height: 32px;
}

/* Figma Dark - 컴팩트 */
.theme-figma-dark {
  --rg-border-radius: 4px;
  --rg-cell-padding: 4px;
  --rg-header-height: 30px;
  --rg-row-height: 26px;
}

/* Slack Aubergine - 둥글고 친근 */
.theme-slack-aubergine {
  --rg-border-radius: 8px;
  --rg-cell-padding: 8px;
  --rg-header-height: 40px;
  --rg-row-height: 36px;
}

/* Koscom Light - 기업형 */
.theme-koscom-light {
  --rg-border-radius: 2px;
  --rg-cell-padding: 6px;
  --rg-header-height: 36px;
  --rg-row-height: 32px;
}

/* Notion Light - 미니멀 */
.theme-notion-light {
  --rg-border-radius: 4px;
  --rg-cell-padding: 8px;
  --rg-header-height: 38px;
  --rg-row-height: 34px;
}
```

---

## 5. 접근성 검증 계획

### 5.1 WCAG 대비율 검증

| 테마 | 배경 (L) | 텍스트 (L) | ΔL | 예상 결과 |
|------|----------|-----------|-----|----------|
| Linear Dark | 0.15 | 0.95 | 0.80 | ✅ AAA |
| GitHub Dark | 0.12 | 0.95 | 0.83 | ✅ AAA |
| Figma Dark | 0.13 | 0.95 | 0.82 | ✅ AAA |
| Slack Aubergine | 0.15 | 0.95 | 0.80 | ✅ AAA |
| Koscom Light | 0.97 | 0.20 | 0.77 | ✅ AAA |
| Notion Light | 0.99 | 0.15 | 0.84 | ✅ AAA |

### 5.2 Primary 색상 대비 검증

선택/포커스 상태에서 Primary 색상이 충분한 대비를 제공하는지 검증:

```typescript
// theme-validator.ts 활용
import { checkOklchContrast } from '~/utils/theme-validator'

const themes = ['linear-dark', 'github-dark', 'figma-dark', 
                'slack-aubergine', 'koscom-light', 'notion-light']

themes.forEach(theme => {
  // Primary-3 (선택 배경) vs Gray-12 (텍스트)
  const result = checkOklchContrast(
    getComputedStyle(document.documentElement).getPropertyValue('--oklch-primary-3'),
    getComputedStyle(document.documentElement).getPropertyValue('--oklch-gray-12')
  )
  console.log(`${theme}: ΔL = ${result.lightnessDiff}, sufficient = ${result.sufficient}`)
})
```

### 5.3 E2E 테스트 계획

```typescript
// test/e2e/realgrid-theme.spec.ts
import { test, expect } from '@playwright/test'

const THEMES = [
  'linear-dark', 'github-dark', 'figma-dark',
  'slack-aubergine', 'koscom-light', 'notion-light'
]

test.describe('RealGrid 테마 통합', () => {
  THEMES.forEach(theme => {
    test(`${theme} 테마에서 RealGrid 색상 반영`, async ({ page }) => {
      await page.goto('/demo/realgrid')
      
      // 테마 변경
      await page.evaluate((t) => {
        window.__themeStore__.setTheme(t)
      }, theme)
      
      // RealGrid 루트 배경색 확인
      const rgRoot = page.locator('.rg-root')
      const bgColor = await rgRoot.evaluate(el => 
        getComputedStyle(el).backgroundColor
      )
      
      // OKLCH gray-1 값과 비교 (하드코딩 아님)
      expect(bgColor).not.toBe('rgb(43, 43, 43)') // #2B2B2B
    })

    test(`${theme} 테마에서 선택 행 Primary 색상`, async ({ page }) => {
      await page.goto('/demo/realgrid')
      await page.evaluate((t) => window.__themeStore__.setTheme(t), theme)
      
      // 행 클릭하여 선택
      await page.locator('.rg-body .rg-table tr').first().click()
      
      // 선택 행 배경색에 Primary 틴트 확인
      const selectedRow = page.locator('.rg-rowfocus, .rg-selection')
      const bgColor = await selectedRow.evaluate(el => 
        getComputedStyle(el).backgroundColor
      )
      
      // Primary-3 변수 값과 비교 (하드코딩 아님)
      expect(bgColor).not.toBe('rgb(29, 31, 32)') // #1d1f20
    })
  })

  test('다크/라이트 모드 전환 시 RealGrid 반영', async ({ page }) => {
    await page.goto('/demo/realgrid')
    
    // 라이트 모드로 전환
    await page.evaluate(() => window.__themeStore__.setMode('light'))
    
    const rgRoot = page.locator('.rg-root')
    const lightBg = await rgRoot.evaluate(el => 
      getComputedStyle(el).backgroundColor
    )
    
    // 다크 모드로 전환
    await page.evaluate(() => window.__themeStore__.setMode('dark'))
    
    const darkBg = await rgRoot.evaluate(el => 
      getComputedStyle(el).backgroundColor
    )
    
    // 배경색이 달라야 함
    expect(lightBg).not.toBe(darkBg)
  })
})
```

---

## 6. 파일 변경 요약

| 파일 | 변경 내용 | Phase |
|------|----------|-------|
| `app/assets/css/main.css` | RealGrid OKLCH 섹션 추가 (~200줄) | P0-P3 |
| `plugins/realgrid-theme.client.ts` | 수정 없음 | - |
| `stores/theme.ts` | 수정 없음 | - |
| `test/e2e/realgrid-theme.spec.ts` | 새 파일 생성 | 검증 |

---

## 7. 리스크 및 대응

| 리스크 | 확률 | 영향 | 대응 방안 |
|--------|------|------|----------|
| 셀렉터 우선순위 충돌 | 중 | 중 | `!important` 또는 더 구체적 셀렉터 |
| RealGrid 버전 업데이트 | 저 | 중 | 버전별 셀렉터 테스트 |
| SVG mask 브라우저 호환 | 저 | 저 | 95%+ 지원, fallback 불필요 |
| 라이트 테마 가독성 | 저 | 중 | WCAG 검증으로 사전 방지 |

---

## 8. 일정 요약

| Phase | 범위 | 예상 시간 | 우선순위 |
|-------|------|----------|----------|
| P0 | 기본 색상 | 2시간 | 🔴 필수 |
| P1 | Primary 색상 | 2시간 | 🔴 필수 |
| P2 | 인터랙션 | 3시간 | 🟠 권장 |
| P3 | 아이콘 mask (Option C) | 5시간 | 🟡 선택 |
| 검증 | E2E 테스트 | 2시간 | 🔴 필수 |

**총 예상 시간**: 14시간 (P0-P3 + 검증)  
**권장 우선 구현**: P0 + P1 + 검증 (6시간)

---

## 9. 승인 체크리스트

- [ ] 아키텍처 검토 완료 (CSS 오버라이드 방식)
- [ ] Phase별 범위 확정 (P0-P3)
- [ ] 아이콘 처리 방식 확정 (Option C: CSS mask)
- [ ] 접근성 검증 방식 확정 (WCAG + E2E)
- [ ] RealGrid 버전 업데이트 대응 OK
- [ ] 일정 승인
- [ ] 구현 착수

---

*이 문서는 `/sc:brainstorm` 및 `/sc:design` 명령으로 생성되었습니다.*
