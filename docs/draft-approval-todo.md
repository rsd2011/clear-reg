# Draft/Approval Module TODO

## Scope
- Module: draft + approval + template + reference/notification flows
- Source baseline: draft domain/services in `backend/draft`, REST in `backend/server/web/DraftController`, migrations `2024-09-24`~`2024-09-27`
- Goal: meet electronic approval requirements (업무별 기안, 템플릿, 결재선, 참조, 알림, 감사)

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
| T6 | 서비스 | 기본 결재선 자동 선택 | Medium | T1 선행; 조직/업무별 기본 템플릿 조회, 제한적 커스터마이즈 |
| T7 | 서비스 | 참조자 등록 및 알림 트리거 | High | T2 선행; 생성/상신 시 참조자 등록 및 이벤트 발행 |
| T8 | API/컨트롤러 | 템플릿/그룹 CRUD 및 활성/버전 관리 | Medium | ApprovalGroup/LineTemplate/FormTemplate CRUD, UI/REST |
| T9 | API/컨트롤러 | 이력/참조 조회 API | Medium | DraftHistory, 참조자 목록/열람 로그 제공; T2 이후 |
| T10 | 뷰/Thymeleaf | 기안 작성/조회 화면 | Medium | ✅ 목록/작성/상세 뷰, 레이아웃 적용, 상태 배지 i18n 완료 |
| T11 | 뷰/Thymeleaf | 결재 진행 화면 | Medium | ✅ 상태별 액션 버튼(승인/반려/회수/재상신) 및 참조/이력 표시 완료 |
| T12 | 인프라/운영 | 알림 이벤트 퍼블리셔(outbox) | Medium | Kafka/RabbitMQ 어댑터, 이메일/푸시 플러그인; T7 |
| T13 | 테스트 | 통합/리포지토리/동시성 테스트 확장 | High | 상태 전이, 락, 컨트롤러, 커버리지 ≥80% 유지 |
| T14 | 보안 | 열람/권한 세분화 | High | 작성자/결재자/참조자/관리자 액션 검증, RowScope 확장; T2 |
| T15 | 데이터/성능 | 검색 필터·인덱스 보강 | Medium | 상태/업무/기간/결재자/참조자 필터, 인덱스 추가 |
| T16 | 감사 | 감사 로그/감사 이벤트 | Medium | 액터/IP/UA 기록, 조회 API, 알림과 연계; T4 |
| T17 | 파일 | 첨부 다운로드/삭제 정책 강화 | Medium | file-core 연동, 권한, 바이러스 스캔 후크 |
## Document Follow-ups
- `docs/permissions.md`: 결재자/참조자 권한, 상태 전이 정책 추가 (T4, T5, T14)
- `docs/architecture-todo.md`: 아키텍처 전반 TODO에 본 모듈 링크 추가
- `docs/observability.md` + OTEL 설정: 결재 리드타임/반려율 등 비즈니스 메트릭 노출 (T12, T16)
- 새 뷰 가이드: Thymeleaf 레이아웃/fragment 규칙 정리 (T10, T11)
