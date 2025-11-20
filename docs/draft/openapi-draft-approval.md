# Draft/Approval OpenAPI 계약 (Nuxt4 SPA 전용, SSR 미사용)

## 전제
- 클라이언트: Nuxt4 SPA (SSR 비활성), Axios/Fetch 사용.
- 인증: 서버 측 Spring Security 세션 또는 Bearer 토큰(현행 정책 준수). 모든 엔드포인트는 `Authorization` 헤더 필요.
- 응답 포맷: JSON only. 문제 발생 시 `application/problem+json` 형태로 반환.
- 타임존: `OffsetDateTime`(ISO-8601, 예: `2025-11-20T10:20:30Z`).
- 권한: FeatureCode/ActionCode 기반. RowScope에 따라 조직 필터가 적용됨.

## 공통 스키마
- Pagination 응답
```json
{
  "content": [ { /* item */ } ],
  "totalElements": 120,
  "totalPages": 12,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

- Problem 응답 (권장)
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "결재 권한이 없습니다.",
  "instance": "/api/drafts/{id}/approve"
}
```
- Validation 오류 예시
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "유효성 검증에 실패했습니다.",
  "invalidParams": [
    { "name": "title", "reason": "must not be blank" },
    { "name": "formPayload", "reason": "invalid JSON" }
  ]
}
```

## 표준 응답/에러 규칙
- 성공: 200 OK (조회/액션), 201 Created(필요 시), 204 No Content(삭제성 액션 시 사용 가능).
- 실패: 400(검증), 401/403(인증/인가), 404(리소스 없음), 409(상태 불일치/동시성), 422(도메인 규칙 위반 시 선택적으로 사용), 500(예상치 못한 오류).
- `Content-Type`은 항상 `application/json` 또는 `application/problem+json`.
- 동시성 충돌 시 `409` + Problem detail에 기대 상태/현재 상태 명시.

## 엔드포인트

### 1) 기안 목록 조회
- `GET /api/drafts?page=0&size=10&status=IN_REVIEW&businessFeature=LEAVE`
- 권한: `DRAFT_READ`
- 응답: `Page<DraftResponse>`

### 2) 기안 생성
- `POST /api/drafts`
- 권한: `DRAFT_CREATE`
- Body: `DraftCreateRequest`
```json
{
  "title": "휴가 신청",
  "content": "연차 3일",
  "businessFeatureCode": "LEAVE",
  "templateCode": "DEFAULT",
  "formTemplateCode": "LEAVE_FORM_V1",
  "formPayload": "{ \"from\": \"2025-11-25\", \"to\": \"2025-11-27\" }",
  "references": ["userA", "userB"]
}
```
- 응답: `DraftResponse`

### 3) 기안 단건 조회
- `GET /api/drafts/{id}`
- 권한: `DRAFT_READ`
- 응답: `DraftResponse`
- 예시
```json
{
  "id": "c2c3c0e1-9c35-4a5b-8c63-8c6c6f1d8e11",
  "title": "휴가 신청",
  "content": "연차 3일",
  "businessFeatureCode": "LEAVE",
  "organizationCode": "ORG-A",
  "status": "IN_REVIEW",
  "templateCode": "DEFAULT",
  "formTemplateCode": "LEAVE_FORM_V1",
  "formTemplateVersion": 1,
  "formTemplateSnapshot": "{...json schema...}",
  "formPayload": "{ \"from\": \"2025-11-25\", \"to\": \"2025-11-27\" }",
  "createdAt": "2025-11-20T10:00:00Z",
  "updatedAt": "2025-11-20T10:10:00Z",
  "submittedAt": "2025-11-20T10:05:00Z",
  "completedAt": null,
  "cancelledAt": null,
  "withdrawnAt": null,
  "approvalSteps": [
    {
      "id": "9c7ba40a-9f17-42d8-9c38-3c9853d2d9ae",
      "approvalGroupCode": "TEAM_LEAD",
      "state": "IN_PROGRESS",
      "actedBy": "lead1",
      "delegatedTo": null,
      "comment": null,
      "startedAt": "2025-11-20T10:05:00Z",
      "completedAt": null,
      "lockedVersion": 3
    },
    {
      "id": "a5f0c2bb-1e2f-4d5f-8c1b-5e1a0f8b2c3d",
      "approvalGroupCode": "DEPT_HEAD",
      "state": "WAITING",
      "actedBy": null,
      "delegatedTo": null,
      "comment": null,
      "startedAt": null,
      "completedAt": null,
      "lockedVersion": 0
    }
  ],
  "attachments": []
}
```

### 4) 상태 액션
- 상신: `POST /api/drafts/{id}/submit` (`DRAFT_SUBMIT`)
- 승인: `POST /api/drafts/{id}/approve` (`DRAFT_APPROVE`)
```json
{ "stepId": "UUID", "comment": "승인합니다." }
```
- 반려: `POST /api/drafts/{id}/reject` (`DRAFT_APPROVE`)
- 회수: `POST /api/drafts/{id}/withdraw` (`DRAFT_WITHDRAW`)
- 재상신: `POST /api/drafts/{id}/resubmit` (`DRAFT_RESUBMIT`)
- 위임: `POST /api/drafts/{id}/delegate?delegatedTo=userX` (`DRAFT_DELEGATE`)
```json
{ "stepId": "UUID", "comment": "부재중 위임" }
```
- 취소: `POST /api/drafts/{id}/cancel` (`DRAFT_CANCEL`)
- 응답: `DraftResponse`

### 5) 이력/참조 조회
- `GET /api/drafts/{id}/history` → `[DraftHistoryResponse]`
- `GET /api/drafts/{id}/references` → `[DraftReferenceResponse]`
- 권한: `DRAFT_READ`
- 이력 응답 예시
```json
[
  {
    "eventType": "SUBMITTED",
    "actor": "userA",
    "details": "기안 상신",
    "occurredAt": "2025-11-20T10:05:00Z"
  },
  {
    "eventType": "APPROVED_STEP",
    "actor": "lead1",
    "details": "결재 그룹 TEAM_LEAD 승인",
    "occurredAt": "2025-11-20T10:06:30Z"
  }
]
```
- 참조 응답 예시
```json
[
  {
    "referencedUserId": "userB",
    "referencedAt": "2025-11-20T10:05:00Z"
  }
]
```

### 6) 기본 템플릿 추천
- `GET /api/drafts/templates/default?businessFeature=LEAVE`
- 권한: `DRAFT_CREATE`
- 응답: `DraftTemplateSuggestionResponse`

### 7) 템플릿/그룹 관리 (Admin, Nuxt 콘솔 연동)
- `POST /api/draft-admin/approval-groups` / `PUT /{id}` / `GET` (조직별 그룹 관리)
- `POST /api/draft-admin/approval-line-templates` / `PUT /{id}` / `GET` (결재선 템플릿 + 스텝)
- `POST /api/draft-admin/form-templates` / `PUT /{id}` / `GET` (폼 스키마/버전 관리)
- 권한: `DRAFT_AUDIT` (관리자용)

## 주요 스키마 요약
- `DraftResponse`
  - `id: UUID`
  - `title, content`
  - `businessFeatureCode, organizationCode, templateCode`
  - `formTemplateCode, formTemplateVersion, formTemplateSnapshot, formPayload`
  - `status: [DRAFT, IN_REVIEW, APPROVED, REJECTED, CANCELLED, WITHDRAWN]`
  - `createdAt, updatedAt, submittedAt, completedAt, cancelledAt, withdrawnAt`
  - `approvalSteps: DraftApprovalStepResponse[]`
  - `attachments: DraftAttachmentResponse[]`

- `DraftApprovalStepResponse`
  - `id, approvalGroupCode, state[WAITING/IN_PROGRESS/APPROVED/REJECTED/SKIPPED]`
  - `actedBy, delegatedTo, comment, startedAt, completedAt, lockedVersion`

- `DraftHistoryResponse`
  - `eventType, actor, details, occurredAt`

- `DraftReferenceResponse`
  - `referencedUserId, referencedAt`

- `DraftTemplateSuggestionResponse`
  - `approvalTemplateId, formTemplateId, source` (organization/business 매핑 여부)

## 밸리데이션/비즈니스 규칙(요약)
- `title` 1~255, `businessFeatureCode`/`templateCode` 필수.
- `formPayload` 는 JSON 문자열, 폼 스키마(`formTemplateSnapshot`)와 클라이언트 측에서 사전 검증 권장.
- 결재/반려/회수/재상신/위임은 상태/권한 검증 후 처리. 종료 상태에서는 수정 불가.
- 조직 접근 제한: RowScope=SELF 조직만 허용, AUDIT 권한 시 전체 조직 접근.
- 위임(delegate): `delegatedTo`는 동일 그룹 멤버 제한(도메인 검증 수행).

## Nuxt4 연동 메모 (SSR 미사용)
- Axios 기본 설정: `baseURL=/api`, 타임아웃/재시도 설정.
- 공통 에러 핸들러: `application/problem+json` 파싱하여 토스트/다이얼로그 표준화.
- i18n: 상태/액션 라벨은 FE 번역 리소스에서 관리 (서버 메시지 의존 없음).
- 캐싱: 목록 캐시 시 상태별 invalidation(상신/승인/반려/회수/재상신 직후 무효화).
- Form Builder: `formTemplateSnapshot`(JSON schema) 기반 동적 폼 구성, 제출 전 클라이언트 스키마 검증.
