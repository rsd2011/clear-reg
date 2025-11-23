# Playwright 기반 정책/마스킹 E2E 스모크

## 전제
- Node.js, `npx playwright` 사용 가능해야 함.
- 백엔드 서버 기동 상태(`BASE_URL`, 기본 http://localhost:8080).
- `ADMIN_TOKEN`: POLICY/NOTICE READ 권한이 있는 JWT 설정.

## 실행
```bash
BASE_URL=http://localhost:8080 \
ADMIN_TOKEN="<jwt>" \
npx playwright test docs/audit/playwright/policy-debug.spec.ts
```

## 시나리오 요약
1) `/api/admin/policies/effective` 호출로 정책 토글 + DataPolicyMatch 노출 확인
2) 공지 목록 `/api/notices` 호출 시 마스킹된 필드가 응답되는지 기본 동작 확인 (secure-by-default)

필요 시 추가 케이스
- 특정 permGroup/orgGroup/policy 설정 후 `maskRule` 변화를 비교
- SENSITIVE API에 reason 누락 시 400 여부 확인
