# HR 배치 연계 개요

본 문서는 HR 일일 배치 파이프라인의 구조와 운영 가이드를 정리합니다.

## 파이프라인 단계

1. **파일 수신**: HR 시스템이 `employee_YYYYMMDD_seq.csv`, `organization_YYYYMMDD_seq.csv` 두 종류의 파일을 `hr.ingestion.incoming-dir`로 전달합니다.
2. **파일 검증**: 이름 패턴, 중복 여부, SHA-256 해시를 계산해 `hr_import_batches`에 기록하고 피드 타입(직원/조직)을 식별합니다.
3. **CSV 파싱/검증**: 각 피드마다 전용 파서/검증기를 통해 필수 필드와 참조 무결성을 검사하며 오류는 `hr_import_errors`에 적재합니다.
4. **스테이징 적재**: 정합성 검사를 통과한 직원 데이터는 `hr_employee_staging`, 조직 데이터는 `hr_organization_staging`에 저장됩니다.
5. **정규화/버전 관리**: `hr_employees`, `hr_organizations` 테이블에서 각각 SCD2 스냅샷을 유지하며 신규/변경 건수를 배치 엔터티에 집계합니다.
6. **아카이브**: 처리가 완료된 파일은 `archive-dir` 또는 실패 시 `error-dir`로 이동합니다.

## 스케줄 및 운영

- `hr.ingestion.batch-cron`(기본: 오전 2시 30분 KST)에 맞춰 `HrIngestionScheduler`가 실행됩니다.
- `/api/hr/batches/ingest` 엔드포인트로 수동 재처리가 가능하며, `/api/hr/batches`에서 배치 이력을 조회할 수 있습니다.
- `hr.ingestion.enabled=false`로 설정하면 스케줄러와 파일 스캐너가 비활성화됩니다.

## 테이블 요약

| 테이블 | 목적 |
| --- | --- |
| `hr_import_batches` | 파일 메타데이터, 처리 상태, 건수, 오류 메시지를 저장합니다. |
| `hr_import_errors` | 레코드 단위 검증 실패 내용을 기록합니다. |
| `hr_employee_staging` | 정합성 검사를 통과한 직원 HR 원본 스냅샷을 저장합니다. |
| `hr_organization_staging` | 정합성 검사를 통과한 조직 원본 스냅샷을 저장합니다. |
| `hr_employees` | 서비스에서 사용하는 정규화된 직원 이력(SCD2)을 유지합니다. |
| `hr_organizations` | 조직 트리 및 유효기간 기반 이력을 유지합니다. |
| `hr_external_feeds` | 외부 DB/CDC로부터 전달된 원본 페이로드를 큐 형태로 저장합니다. |

## 디렉터리

- `incoming-dir`: HR 팀이 업로드하는 위치 (기본 `build/hr/incoming`).
- `archive-dir`: 성공적으로 처리된 파일 보관 위치.
- `error-dir`: 실패 파일 보관 위치.

각 디렉터리는 애플리케이션 기동 시 자동으로 생성됩니다. 운영 환경에서는 외부 스토리지(S3, NAS 등)와 마운트하여 사용합니다.

## 외부 DB 연계

- `HrFeedConnector` 추상화를 통해 파일(`FileHrFeedConnector`)과 외부 DB(`DatabaseHrFeedConnector`) 커넥터를 동시에 구성합니다.
- 외부 DB 커넥터는 `hr_external_feeds` 큐 테이블에서 `PENDING` 상태의 페이로드를 읽어 `PROCESSING`으로 전환한 뒤 파이프라인에 전달합니다.
- 처리 성공 시 `COMPLETED`, 실패 시 `FAILED`로 상태를 갱신하고 오류 메시지를 기록하여 재처리 및 모니터링을 쉽게 합니다.
- `hr.ingestion.database.enabled=true` 로 설정하면 DB 커넥터가 활성화되며, 배치 크기(`batch-size`) 설정으로 원본 DB 부하를 제어할 수 있습니다.
