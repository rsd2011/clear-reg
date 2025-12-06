# CSS 변수 네이밍 가이드라인

> 프론트엔드 테마 시스템의 CSS Custom Properties 명명 규칙

## 목차

1. [3-Tier Design Token System](#3-tier-design-token-system)
2. [네이밍 컨벤션](#네이밍-컨벤션)
3. [계층별 상세 규칙](#계층별-상세-규칙)
4. [OKLCH 색상 시스템](#oklch-색상-시스템)
5. [Relative Color Syntax](#relative-color-syntax)
6. [접근성 고려사항](#접근성-고려사항)

---

## 3-Tier Design Token System

CSS 변수는 3단계 계층 구조를 따릅니다:

```
┌─────────────────────────────────────────────────────────────┐
│  1️⃣ Primitive Layer (원시 계층)                              │
│  - OKLCH 12단계 스케일                                       │
│  - L/C/H 컴포넌트 분리                                       │
│  예: --oklch-gray-1, --oklch-primary-h                      │
├─────────────────────────────────────────────────────────────┤
│  2️⃣ Semantic Layer (의미 계층)                               │
│  - 용도 기반 명명                                            │
│  - Primitive 참조                                           │
│  예: --app-color-brand, --app-color-text-primary            │
├─────────────────────────────────────────────────────────────┤
│  3️⃣ Component Layer (컴포넌트 계층)                          │
│  - 컴포넌트별 토큰                                           │
│  - Semantic 참조                                            │
│  예: --app-button-primary-bg, --app-card-shadow             │
└─────────────────────────────────────────────────────────────┘
```

---

## 네이밍 컨벤션

### 기본 패턴

```
--{namespace}-{category}-{property}[-{variant}][-{state}]
```

| 요소 | 설명 | 예시 |
|------|------|------|
| `namespace` | 소유권 구분 | `app`, `oklch`, `p` (PrimeVue) |
| `category` | 계층/용도 | `color`, `button`, `card` |
| `property` | 속성 | `bg`, `text`, `border` |
| `variant` | 변형 (선택) | `primary`, `secondary`, `subtle` |
| `state` | 상태 (선택) | `hover`, `active`, `disabled` |

### 네임스페이스 규칙

| Prefix | 용도 | 예시 |
|--------|------|------|
| `--oklch-*` | OKLCH 원시 색상값 | `--oklch-gray-1`, `--oklch-primary-h` |
| `--app-*` | 애플리케이션 토큰 | `--app-color-brand`, `--app-button-bg` |
| `--p-*` | PrimeVue 토큰 | `--p-surface-900`, `--p-primary-500` |
| `--dv-*` | DockView 토큰 | `--dv-background-color` |

---

## 계층별 상세 규칙

### 1️⃣ Primitive Layer

OKLCH 12단계 스케일 (Radix UI 패턴):

```
단계    용도
────────────────────────────────
1-2     배경 (App bg, Subtle bg)
3-5     UI 요소 (Element bg, Hovered, Active)
6-8     테두리 (Subtle border, Border, Hovered border)
9-10    솔리드 (Solid bg, Hovered solid bg)
11-12   텍스트 (Low contrast, High contrast)
```

**L/C/H 컴포넌트 분리:**

```css
/* ✅ 권장: 컴포넌트 분리로 동적 조합 가능 */
--oklch-primary-h: 265;      /* Hue */
--oklch-primary-c: 0.15;     /* Chroma */
--oklch-primary-l-9: 0.55;   /* Lightness (단계별) */

/* 조합 시 */
--oklch-primary-9: oklch(var(--oklch-primary-l-9) var(--oklch-primary-c) var(--oklch-primary-h));
```

### 2️⃣ Semantic Layer

**카테고리별 명명:**

| 카테고리 | 패턴 | 예시 |
|----------|------|------|
| 브랜드 | `--app-color-brand[-variant]` | `--app-color-brand-subtle` |
| 피드백 | `--app-color-{type}[-variant]` | `--app-color-success-hover` |
| 텍스트 | `--app-color-text-{level}` | `--app-color-text-primary` |
| 배경 | `--app-color-bg-{type}` | `--app-color-bg-elevated` |
| 서피스 | `--app-color-surface-{state}` | `--app-color-surface-hovered` |
| 테두리 | `--app-color-border-{level}` | `--app-color-border-emphasis` |

**피드백 색상 타입:**
- `success`: 성공, 완료
- `warning`: 경고, 주의
- `danger`: 오류, 위험
- `info`: 정보, 안내

**텍스트 레벨:**
- `primary`: 가장 중요한 텍스트 (제목, 본문)
- `secondary`: 보조 텍스트 (부제목, 레이블)
- `tertiary`: 덜 중요한 텍스트 (힌트, 비활성)
- `disabled`: 비활성 상태
- `inverse`: 반전 배경용
- `on-brand`: 브랜드 색상 위

**배경 타입:**
- `primary`: 기본 페이지 배경
- `secondary`: 보조 영역 배경
- `tertiary`: 3차 영역
- `elevated`: 떠 있는 요소 (모달, 드롭다운)
- `sunken`: 움푹 들어간 영역 (인풋 필드)
- `overlay`: 반투명 오버레이

### 3️⃣ Component Layer

**패턴:**

```
--app-{component}-{property}[-{variant}][-{state}]
```

**예시:**

```css
/* Button */
--app-button-primary-bg
--app-button-primary-bg-hover
--app-button-secondary-border
--app-button-disabled-text

/* Card */
--app-card-bg
--app-card-bg-hover
--app-card-shadow
--app-card-shadow-hover

/* Input */
--app-input-bg
--app-input-border-focus
--app-input-error-border

/* Navigation */
--app-nav-item-active
--app-nav-item-text-active
```

---

## OKLCH 색상 시스템

### 왜 OKLCH인가?

1. **지각적 균일성**: 밝기/채도 변화가 인간 시각에 균일하게 적용
2. **Hue 안정성**: 색상 회전 시 채도가 유지됨 (HSL 대비 개선)
3. **접근성**: 대비율 계산 정확도 향상
4. **표준화**: CSS Color Level 4 표준

### 문법

```css
/* 기본 형식 */
oklch(L C H)
oklch(L C H / alpha)

/* L: Lightness (0-1) */
/* C: Chroma (0-0.4) */
/* H: Hue (0-360) */
```

### 테마별 Hue 값

| 테마 | Primary Hue | 색상 |
|------|-------------|------|
| Linear Dark | 265 | Indigo |
| GitHub Dark | 230 | Blue |
| Figma Dark | 230 | Blue |
| Slack Aubergine | 320 | Purple |
| Koscom Light | 55 | Orange |
| Notion Light | 200 | Teal |

---

## Relative Color Syntax

### 개요

CSS Color Level 5의 Relative Color Syntax를 사용하여 런타임에 색상 변형을 동적으로 계산합니다.

### 브라우저 지원

- Chrome 111+ ✅
- Safari 15.4+ ✅
- Firefox 128+ ✅
- 지원률: ~93%+

### 기본 패턴

```css
/* 밝기 조정 */
--app-color-brand-hover: oklch(from var(--app-color-brand) calc(l + 0.08) c h);
--app-color-brand-active: oklch(from var(--app-color-brand) calc(l - 0.05) c h);

/* 채도 조정 */
--app-color-brand-subtle-auto: oklch(from var(--app-color-brand) calc(l + 0.35) calc(c * 0.4) h);
```

### 투명도 변형 (color-mix)

```css
/* 투명도 적용 */
--app-color-brand-translucent: color-mix(in oklch, var(--app-color-brand) 20%, transparent);
--app-color-brand-ghost: color-mix(in oklch, var(--app-color-brand) 10%, transparent);
```

### 다크모드 조정

```css
/* 라이트 모드 */
--app-color-brand-hover: oklch(from var(--app-color-brand) calc(l + 0.08) c h);

/* 다크 모드 - 밝기 변화 방향 조정 */
.app-dark {
  --app-color-brand-hover: oklch(from var(--app-color-brand) calc(l + 0.1) c h);
}
```

---

## 접근성 고려사항

### 대비율 확보

- 텍스트-배경 대비: 최소 4.5:1 (WCAG AA)
- 대형 텍스트: 최소 3:1
- UI 컴포넌트: 최소 3:1

### 고대비 모드

```css
html.high-contrast {
  --app-contrast-boost: 1.15;
}

/* 포커스 링 강화 */
html.high-contrast :focus-visible {
  outline: 3px solid var(--p-primary-500) !important;
  outline-offset: 2px !important;
}
```

### 줄임 모션

```css
/* 사용자 설정 */
html.reduce-motion * {
  animation-duration: 0.01ms !important;
  transition-duration: 0.01ms !important;
}

/* 시스템 설정 */
@media (prefers-reduced-motion: reduce) {
  /* ... */
}
```

---

## Quick Reference

### 자주 사용하는 토큰

```css
/* 텍스트 */
color: var(--app-color-text-primary);
color: var(--app-color-text-secondary);

/* 배경 */
background: var(--app-color-bg-primary);
background: var(--app-color-bg-elevated);

/* 브랜드 */
background: var(--app-color-brand);
background: var(--app-color-brand-hover);

/* 테두리 */
border-color: var(--app-color-border-default);
border-color: var(--app-color-border-subtle);

/* 컴포넌트 */
background: var(--app-button-primary-bg);
box-shadow: var(--app-card-shadow);
```

### 새 토큰 추가 체크리스트

- [ ] 올바른 계층(Primitive/Semantic/Component) 선택
- [ ] 네이밍 컨벤션 준수
- [ ] 다크모드 오버라이드 필요 여부 확인
- [ ] OKLCH 스케일 참조 (하드코딩 X)
- [ ] 접근성 대비율 검증
