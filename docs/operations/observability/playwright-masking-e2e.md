# Playwright e2e 가이드 — forceUnmask 역할/사유 검증

> 현재 프론트엔드(Nuxt4)가 미구현 상태이므로, API 기반 시나리오를 Playwright에서 호출하는 형태의 스켈레톤을 제시한다.

## 전제
- 서버가 로컬 8080에서 구동 중 (`/api/exports/sample`, `/api/exports/orgs/excel` 등).
- 테스트용 토큰 혹은 세션 쿠키를 `AUTH_HEADER`로 주입.

## 설치
```bash
npm install -D playwright @playwright/test
```

## 스켈레톤 테스트 (`tests/masking-force-unmask.spec.ts`)
```ts
import { test, expect } from '@playwright/test';

const BASE = process.env.BASE_URL ?? 'http://localhost:8080';
const AUTH = process.env.AUTH_HEADER ?? 'Bearer dummy';

test('기본 마스킹이 적용된다', async ({ request }) => {
  const res = await request.get(`${BASE}/api/exports/sample`, {
    headers: { Authorization: AUTH },
    params: {
      accountNumber: '1234567890123456',
      reasonCode: 'CS'
    }
  });
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toContain('************3456');
});

test('forceUnmask + 사유 전달 시 원문 노출', async ({ request }) => {
  const res = await request.get(`${BASE}/api/exports/sample`, {
    headers: { Authorization: AUTH },
    params: {
      accountNumber: '1234567890123456',
      reasonCode: 'AUDIT_OVERRIDE',
      reasonText: '승인된 요청',
      forceUnmask: 'true'
    }
  });
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toContain('1234567890123456');
});
```

## 사유/역할 검증 아이디어
- 헤더/쿠키에 `X-ROLE=COMPLIANCE_ADMIN` 등을 넣어야 forceUnmask 허용 → API에서 403 응답이면 실패.
- reasonCode/ reasonText 누락 시 400 반환하는지를 추가 검증.

## 실행
```bash
npx playwright test tests/masking-force-unmask.spec.ts
```

## CI 연동
- BASE_URL, AUTH_HEADER 환경변수 주입.
- `npx playwright install --with-deps chromium` 후 위 테스트 실행.
