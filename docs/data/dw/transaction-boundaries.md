# DW Ingestion Transaction Boundaries

## 목적
DW ingestion 로직은 `backend/dw-integration` 과 `backend/batch-app`에서 JPA 트랜잭션을 사용해 스테이징 → 스냅샷 테이블을 업데이트한다. 본 문서는 각 단계의 커밋 순서, 롤백 절차, 운영 중 주의 사항을 정리한다.

## 현재 흐름 요약
1. **Feed 선택**: `DwIngestionService`가 `DataFeedConnector`로부터 `DwFeedIngestionTemplate` 에 필요한 데이터를 조회한다.
2. **검증 & 스테이징**: CSV/DB payload를 파싱하여 `dw_*_staging` 테이블에 저장 (`@Transactional` 서비스). 오류는 `dw_import_errors`에 기록.
3. **스냅샷 반영**: 스테이징 데이터를 `dw_*` 스냅샷 테이블(DwEmployees, DwOrganizations 등)에 반영하고 버전/유효기간을 계산.
4. **메타 업데이트**: `dw_import_batches`에 상태/오류/건수 기록, 캐시 무효화 후 종료.

## 트랜잭션 경계 정의
| 단계 | 트랜잭션 | 커밋 시점 | 롤백 행동 |
| --- | --- | --- | --- |
| Feed 선택 및 상태 변경 (`dw_import_batches.status=PENDING→PROCESSING`) | 짧은 단일 트랜잭션 | feed 잠금 직후 | 상태를 원래 값으로 복원하거나 `FAILED` 기록 |
| 스테이징 적재 | 커넥터별 서비스 @Transactional | 스테이징 insert 완료 시 | 스테이징 레코드 및 오류 레코드 삭제 |
| 스냅샷 반영 (DwEmployees, DwOrganizations 등) | 엔티티별 @Transactional | upsert + 버전행 생성 후 커밋 | 기존 상태 유지, 스테이징 레코드 롤백 |
| 메타데이터/캐시 | 단일 트랜잭션 | `dw_import_batches` 업데이트 + 캐시 무효화 | 실패 시 배치 상태를 `FAILED`로 업데이트 |

## 권장 사항
- **단일 배치 단위 트랜잭션**: 스테이징과 스냅샷 반영을 하나의 `REQUIRES_NEW` 트랜잭션 블록으로 묶으면, 실패 시 스테이징/스냅샷 동시 롤백이 가능하다.
- **Idempotent 체크포인트**: `dw_import_batches`에 `last_step` 필드를 추가해 재시작 시 중복 실행을 피한다.
- **캐시 무효화 순서**: 스냅샷 커밋 → 캐시 무효화 → 배치 상태 `COMPLETED` 순서를 문서화하고, 실패 시 전체 캐시를 건드리지 않도록 한다.

## 롤백 절차 (운영 가이드)
1. 배치 실패 시 `dw_import_batches`에서 대상 파일/배치를 식별한다.
2. 스테이징 테이블을 truncate 또는 `batch_id` 기준 삭제한다.
3. 스냅샷 테이블은 버전 기반이므로 추가 레코드가 생겼다면 스냅샷 롤백 스크립트(`docs/migrations`에 추가 예정)를 실행한다.
4. 캐시(`DW_EMPLOYEES`, `DW_ORG_TREE`)를 수동으로 비우고 재처리한다.

## 차기 작업
- `DwIngestionService`에 단계별 `TransactionTemplate`/`REQUIRES_NEW` 적용.
- Outbox 테이블을 추가해 worker/gateway 간 신뢰성 전달.
- 문서에 실제 DB 커밋/롤백 스크립트 추가.
