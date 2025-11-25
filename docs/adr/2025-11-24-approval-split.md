# ADR: Split Draft and Approval Bounded Contexts

**Date:** 2025-11-24

## Status
Accepted — initial scaffolding committed; further extraction pending.

## Context
Draft 기능이 결재 라인/상태를 직접 소유하고 있어 결합도가 높고, 조직/권한 정책을 해석하는 책임이 Draft에 남아 있었다. 결재 흐름을 별도 모듈로 분리해 API/이벤트로 통합하고, 순환 참조(FK) 없이 ID 기반으로만 연결해야 한다.

## Decision
- 신규 Gradle 서브모듈 `backend:approval`을 추가하고, 결재 도메인/포트를 이 모듈에 위치시킨다.
- Draft → Approval 단방향 의존성만 유지한다. Approval은 Draft의 세부 도메인에 직접 의존하지 않는다.
- Approval 포트(`ApprovalFacade`)는 결재 요청/상태/액션을 ID 기반으로 노출한다. Draft는 결재 ID(`approvalRequestId`)만 저장하고 상태는 Approval API로 조회한다.
- 이벤트/동기 포트 모두 지원 가능하도록 요청/완료 이벤트 페이로드를 Approval 쪽에서 정의한다(향후 추가 예정).

## Consequences
- Draft 모듈은 결재 엔티티를 더 이상 직접 소유하지 않고, Approval 모듈 포트를 통해서만 결재를 요청/조회하게 리팩터링해야 한다.
- Approval 모듈은 조직/권한 정책 해석 시 `backend/policy`의 서비스/클라이언트에만 의존하며 Draft 도메인은 알지 않는다.
- DB 스키마는 approval_* 테이블을 Approval 모듈로 논리 분리하고, FK 없이 인덱스+도메인 서비스로 무결성을 보장한다.
- 테스트는 Draft/Approval 각각 분리하고, 모듈 간 계약은 계약 테스트/포트 테스트로 검증한다.

## Work in Progress
- `backend:approval` 모듈 초기화 및 인메모리 포트 구현 완료 (`InMemoryApprovalFacade`).
- 아직 Draft 도메인에 남아있는 `Approval*`/`DraftApproval*` 엔티티와 컨트롤러는 추후 이동/삭제 필요.
- Approval 완료 이벤트 발행, Draft 제출 이벤트 구독, 스키마 분리는 다음 단계에서 진행한다.

## Next Steps
1. Draft 도메인에서 결재 엔티티 의존을 제거하고 `approvalRequestId` 필드로 대체.
2. `DraftApplicationService`가 `ApprovalFacade`를 사용하도록 어댑터 추가 (동기/비동기 선택).
3. 기존 결재 엔티티(`ApprovalGroup*`, `ApprovalLineTemplate*`, `DraftApproval*`)를 Approval 모듈로 이동하고 JPA 매핑 재구성.
4. API 계층 분리: Draft API는 결재 작업을 Approval API로 위임, Approval API는 결재 로그/상태 제공.
5. DB 마이그레이션 스크립트 작성: approval_* 테이블 생성, Draft 테이블 FK 제거 및 인덱스 추가.
6. 계약/통합 테스트 추가 및 기능 토글로 점진적 롤아웃.
