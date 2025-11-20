# Draft/Approval Module TODO

## Scope
- Module: draft + approval + template + reference/notification flows
- Source baseline: draft domain/services in `backend/draft`, REST in `backend/server/web/DraftController`, migrations `2024-09-24`~`2024-09-27`
- Goal: meet electronic approval requirements (업무별 기안, 템플릿, 결재선, 참조, 알림, 감사)
 - Frontend: Nuxt4로 전환 예정 → 서버 측 Thymeleaf 템플릿/뷰 컨트롤러 및 관련 레이아웃/정적 리소스 제거 완료, JSON API만 유지

## Gaps (요약)
- 업무↔템플릿 매핑과 기본 결재선 자동 추천 미비
- 참조자/열람 제어, 결재자 검증·위임·회수/재상신 흐름 부재
- 알림/이벤트(outbox), 감사/감사로그, 열람권한 세분화 미구현
- 템플릿/그룹 CRUD·버전/활성 관리 UI·API 없음, 뷰(Thymeleaf) 부재
- 검색/필터 단순, 인덱스/리포트/읽기 모델 미정
## Backlog
| ID | Category | Title | Priority | Notes / Dependencies |
| --- | --- | --- | --- | --- |
| T1 | 도메인/DB | 업무-템플릿 기본 매핑 엔티티/마이그레이션 | High | ✅ 완료. `business_template_mappings` 테이블 및 도메인/리포지토리 추가 |
| T2 | 도메인/DB | 참조자(DraftReference) 모델 추가 | High | ✅ 완료. `draft_references` 테이블 및 도메인/리포지토리 추가 |
| T3 | 도메인/DB | 결재 그룹 멤버십/조건식 확장 | High | ✅ 완료. `approval_group_members` 테이블 및 도메인/리포지토리 추가 |
| T4 | 서비스 | 결재자 검증 및 단계 잠금 | High | ✅ 완료. 승인/반려 시 그룹 멤버 검증, draft_approval_steps version 컬럼으로 낙관락 적용 |
| T5 | 서비스 | 회수/재상신/전달 플로우 추가 | High | ✅ 회수/재상신/위임(Delegation) 완료. 위임 대상 기록 및 이력 추가 |
| T6 | 서비스 | 기본 결재선 자동 선택 | Medium | ✅ 조직/업무별 매핑 우선→글로벌 매핑 폴백 선택 로직 적용(`DraftApplicationService.selectTemplates`), 기본 템플릿 조회 API `/api/drafts/templates/default` 제공 |
| T7 | 서비스 | 참조자 등록 및 알림 트리거 | High | T2 선행; 생성/상신 시 참조자 등록 및 이벤트 발행 |
| T8 | API/컨트롤러 | 템플릿/그룹 CRUD 및 활성/버전 관리 | Medium | ✅ DraftTemplateAdminController JSON API 추가 (그룹/결재선/양식 CRUD) |
| T9 | API/컨트롤러 | 이력/참조 조회 API | Medium | ✅ REST 존재(`/api/drafts/{id}/history`,`/references`); OpenAPI 예시 추가 |
| T10 | 프런트/계약 | Nuxt4 전환 대비 UI 계약서(작성/조회) 정의 | High | ✅ OpenAPI 초안 작성(`docs/draft/openapi-draft-approval.md`); SSR 미사용 Nuxt4 SPA 기준 |
| T11 | API/컨트롤러 | 뷰 제거 및 JSON 전용 엔드포인트 정돈 | High | ✅ Thymeleaf 컨트롤러/템플릿 삭제, `/drafts` 라우팅 제거, OpenAPI로 응답 스키마 고정 |
| T12 | 인프라/운영 | 알림 이벤트 퍼블리셔(outbox) | Medium | ✅ 이벤트/로그/Kafka 퍼블리셔 플러그형 구현(`draft.notification.publisher`), Outbox 이벤트 리스너 연계 |
| T13 | 테스트 | 통합/리포지토리/동시성 테스트 확장 | High | 진행: 기본/글로벌 템플릿 자동 선택·예외 케이스 단위 테스트 추가(`DraftApplicationServiceTest`), 커버리지 유지. 다음: 컨트롤러 통합 및 동시성(락) 시나리오 보강 |
| T14 | 보안 | 열람/권한 세분화 | High | ✅ RowScope 기본 ORG/OWN승격, 열람 권한(작성자/결재선/위임/참조/AUDIT) 검증, API 호출자 username 기반 체크, 테스트 통과 |
| T15 | 데이터/성능 | 검색 필터·인덱스 보강 | Medium | ✅ 목록 조회 필터(status/business/createdBy/title) 추가 및 OpenAPI 반영 |
| T16 | 감사 | 감사 로그/감사 이벤트 | Medium | ✅ 감사 이벤트 퍼블리셔 + 감사 이력 API(`/api/drafts/{id}/audit` 필터: action/actor/from/to), IP/UA 필드 포함, `draft.audit.publisher=event|kafka|siem|outbox` 지원, outbox 테이블/릴레이, 신뢰 프록시 헤더 검증, 보존/TTL 정책 명시 |
| T17 | 파일 | 첨부 다운로드/삭제 정책 강화 | Medium | 진행: 스캔 포트/NoOp 스캐너/비활성 스캐너 추가, 메타 필드·마이그레이션 완료, 정책 문서화(`docs/file/file-attachment-security.md`). 다음: 다운로드 권한(기안 맥락) 확장, 감사 outbox/Kafka/SIEM 연계, 설정 프로퍼티 및 재시도/TTL 문서화 |
## Document Follow-ups
- `docs/permissions.md`: 결재자/참조자 권한, 상태 전이 정책 추가 (T4, T5, T14)
- `docs/architecture-todo.md`: 아키텍처 전반 TODO에 본 모듈 링크 추가, Nuxt4 프런트 전환 계획 반영
- `docs/observability.md` + OTEL 설정: 결재 리드타임/반려율 등 비즈니스 메트릭 노출 (T12, T16)
- 프런트 전환 가이드: Nuxt4 기반 UI 계약/폼 검증/i18n 전략 정리 (T10, T11) — OpenAPI 초안 `docs/draft/openapi-draft-approval.md`
