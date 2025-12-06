# 테마 시스템 아키텍처

> 마지막 업데이트: 2025-12-06

## 개요

이 프로젝트는 **OKLCH 색상 공간**과 **3-Tier Design Token System**을 기반으로 한 다중 테마 시스템을 사용합니다.

## 토큰 계층 구조

```
┌─────────────────────────────────────────────────────────────────┐
│  3️⃣ Component Layer (--app-button-*, --app-card-*, etc.)       │
│      컴포넌트별 토큰 - Semantic 레이어 참조                       │
├─────────────────────────────────────────────────────────────────┤
│  2️⃣ Semantic Layer (--app-color-brand, --app-color-success)    │
│      의미 기반 토큰 - Primitive 레이어 참조                       │
├─────────────────────────────────────────────────────────────────┤
│  1️⃣ Primitive Layer (--oklch-gray-*, --oklch-primary-*)        │
│      OKLCH 12-Step Scale - 원시 색상 값                          │
└─────────────────────────────────────────────────────────────────┘
```

## PrimeVue vs Custom 토큰

### PrimeVue 토큰 (`--p-*`)
PrimeVue 컴포넌트에서 사용하는 토큰으로, `themes/*.ts` 파일에서 `definePreset`으로 정의됩니다.

| 토큰 | 용도 |
|------|------|
| `--p-primary-*` | 브랜드/Primary 색상 스케일 |
| `--p-surface-*` | Surface/배경 색상 |
| `--p-text-*` | 텍스트 색상 |
| `--p-red-*`, `--p-green-*`, etc. | 시맨틱 색상 |

### Custom 토큰 (`--app-*`, `--oklch-*`)
앱 전역 및 커스텀 컴포넌트에서 사용하는 토큰으로, `assets/css/main.css`에서 정의됩니다.

| 토큰 | 용도 |
|------|------|
| `--oklch-gray-*` | 12-Step 그레이스케일 |
| `--oklch-primary-*` | 12-Step Primary 스케일 |
| `--app-color-*` | 시맨틱 색상 (brand, success, etc.) |
| `--app-button-*`, `--app-card-*` | 컴포넌트별 토큰 |

## 토큰 동기화 전략

### 동기화가 필요한 경우

1. **새 테마 추가 시**
   - `themes/[theme-name].ts` 생성 (PrimeVue preset)
   - `themes/index.ts`에 export 및 THEMES 레지스트리 등록
   - `main.css`에 테마 CSS 변수 추가

2. **Primary 색상 변경 시**
   - `themes/[theme].ts`의 `definePreset` 수정
   - `main.css`의 `--oklch-primary-h`, `--oklch-primary-c` 수정

### 동기화 체크리스트

```markdown
□ themes/*.ts의 primary.500 ↔ main.css의 --oklch-primary-9
□ themes/*.ts의 surface colors ↔ main.css의 --oklch-gray-*
□ prefersDark 설정 ↔ .app-dark 클래스 적용 여부
```

## CSS Color Level 5 기능

### light-dark() 함수

라이트/다크 모드에 따라 자동으로 값을 선택합니다:

```css
:root {
  color-scheme: light;
  --app-tooltip-bg: light-dark(var(--oklch-gray-12), var(--oklch-gray-2));
}

.app-dark {
  color-scheme: dark;
  /* light-dark()가 자동으로 두 번째 값 사용 */
}
```

### Relative Color Syntax

기존 색상에서 동적으로 변형 색상을 생성합니다:

```css
--app-color-brand-hover: oklch(from var(--app-color-brand) calc(l + 0.08) c h);
--app-color-brand-translucent: color-mix(in oklch, var(--app-color-brand) 20%, transparent);
```

## 파일 구조

```
frontend/app/
├── themes/
│   ├── index.ts           # 테마 타입, 레지스트리, 유틸리티
│   ├── linear-dark.ts     # Linear Dark 테마 preset
│   ├── github-dark.ts     # GitHub Dark 테마 preset
│   ├── figma-dark.ts      # Figma Dark 테마 preset
│   ├── slack-aubergine.ts # Slack Aubergine 테마 preset
│   ├── koscom-light.ts    # Koscom Light 테마 preset
│   ├── notion-light.ts    # Notion Light 테마 preset
│   └── README.md          # 이 문서
├── stores/
│   └── theme.ts           # Pinia 테마 스토어
└── assets/css/
    └── main.css           # CSS 변수 정의
```

## 브라우저 지원

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| OKLCH | 111+ | 113+ | 15.4+ | 111+ |
| light-dark() | 123+ | 120+ | 17.5+ | 123+ |
| color-mix() | 111+ | 113+ | 16.2+ | 111+ |
| Relative Color | 119+ | 128+ | 16.4+ | 119+ |
| View Transitions | 111+ | ❌ | 18+ | 111+ |

> **참고**: View Transitions API는 Firefox에서 아직 지원되지 않습니다. 폴백으로 CSS transition이 적용됩니다.

## 테마 추가 가이드

### 1. PrimeVue Preset 생성

```typescript
// themes/my-theme.ts
import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

export const MyThemePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '...',
      // ... 500까지
    }
  }
})

export const MY_THEME_META: ThemeConfig = {
  name: 'My Theme',
  description: '테마 설명',
  className: 'theme-my-theme',
  prefersDark: true, // 또는 false
  author: 'Author',
  version: '1.0.0',
  tags: ['modern', 'minimal'],
  accentColors: ['#color1', '#color2', '#color3'],
  fontStyle: 'sans',
}
```

### 2. 레지스트리 등록

```typescript
// themes/index.ts
export { MyThemePreset, MY_THEME_META } from './my-theme'

export const THEMES: Record<ThemeName, ThemeConfig> = {
  // ...
  'my-theme': MY_THEME_META,
}
```

### 3. CSS 변수 추가

```css
/* main.css */
.theme-my-theme {
  --oklch-primary-h: 265;  /* Hue */
  --oklch-primary-c: 0.15; /* Chroma */

  /* 다크 테마인 경우 Gray Scale은 공통 선택자에서 상속됨 */
}
```
