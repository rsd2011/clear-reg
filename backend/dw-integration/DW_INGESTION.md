# DW 통합 배치 개요

본 문서는 DW 통합 모듈의 다중 소스 수집 파이프라인(파일, DB, 향후 SaaS API) 구조와 운영 가이드를 정리합니다. 설계는 Google Cloud Data Fusion/AWS Glue 권장 패턴과 동일하게 **수집 → 정제 → 적재 → 배포** 4단계를 따릅니다.

## 파이프라인 단계

1. **소스 커넥터**: 파일(`employee_YYYYMMDD_seq.csv`, `organization_YYYYMMDD_seq.csv`, `holiday_{COUNTRY}_YYYYMMDD_seq.csv`, `code_{TYPE}_YYYYMMDD_seq.csv`), 외부 DB, 향후 SaaS API가 `dw.ingestion.incoming-dir` 혹은 `dw_source_feeds` 큐로 데이터를 전달합니다. 공통코드 파일은 TYPE(예: `COUNTRY`, `EMPLOYMENT_STATUS`)별 폴더/이름 규칙을 가지며 DW ↔ 시스템간 코드 일관성을 유지합니다.
2. **메타 검증**: 이름 패턴, 중복 여부, SHA-256 해시를 계산해 `dw_import_batches`에 기록하고 피드 타입(직원/조직/기타)을 식별합니다.
3. **포맷 파싱/검증**: 커넥터별 전용 파서/검증기로 필수 필드, 스키마 버전, 참조 무결성을 검사하며 오류는 `dw_import_errors`에 적재합니다.
4. **스테이징 적재**: 정합성 검사를 통과한 데이터는 주제별 스테이징(`dw_employee_staging`, `dw_org_staging`, 추가 주제)에 저장됩니다.
5. **정규화/버전 관리**: 비즈니스용 테이블(e.g., `dw_employees`, `dw_organizations`, `dw_holidays`)에서 SCD2/스냅샷을 유지하고 배치 엔터티에 집계합니다.
6. **아카이브/거버넌스**: 처리 완료 파일은 `archive-dir`, 실패는 `error-dir`로 이동하며, 보관 기간 정책(`PolicyToggleSettings.fileRetentionDays`)을 적용합니다.

## 스케줄 및 운영

- `dw.ingestion.job-schedules`(기본: `DW_INGESTION` 하나) 설정에 따라 Quartz `DwQuartzScheduleManager`가 작업별 Trigger를 빌드합니다. 토글을 끄면 즉시 Job이 unschedule 되고, Cron/Timezone 변경 시 리스케줄됩니다. 각 작업은 `DwFeedIngestionTemplate`에 의해 파싱/검증/동기화 단계가 구성되므로 휴일처럼 새로운 데이터를 추가해도 템플릿만 구현하면 됩니다.
- `/api/dw/batches/ingest` 엔드포인트로 수동 재처리가 가능하며, `/api/dw/batches`에서 배치 이력을 조회할 수 있습니다.
- `dw.ingestion.enabled=false`로 설정하면 스케줄러와 커넥터가 비활성화됩니다.

## 테이블 요약

| 테이블 | 목적 |
| --- | --- |
| `dw_import_batches` | 소스 별 파일/페이로드 메타데이터, 처리 상태, 건수, 오류 메시지를 저장합니다. |
| `dw_import_errors` | 레코드 단위 검증 실패 내용을 기록합니다. |
| `dw_employee_staging` | 정합성 검사를 통과한 직원 원본 스냅샷을 저장합니다. |
| `dw_org_staging` | 조직/기타 주제 원본 스냅샷을 저장합니다. |
| `dw_employees` | 서비스에서 사용하는 정규화된 직원 이력(SCD2)을 유지합니다. |
| `dw_organizations` | 조직 트리 및 유효기간 기반 이력을 유지합니다. |
| `dw_holidays` | 국가별 휴일 스냅샷을 저장합니다. |
| `dw_common_codes` | DW가 제공하는 공통 코드(국가, 직무, 기타 레퍼런스)를 저장하며 시스템 공통코드 서비스와 병합됩니다. |
| `dw_source_feeds` | 외부 DB/CDC/SaaS에서 전달된 원본 페이로드를 큐 형태로 저장합니다. |

## 디렉터리

- `incoming-dir`: 원본 시스템이 업로드하는 위치 (기본 `build/dw/incoming`).
- `archive-dir`: 성공적으로 처리된 파일 보관 위치.
- `error-dir`: 실패 파일 보관 위치.

각 디렉터리는 애플리케이션 기동 시 자동으로 생성됩니다. 운영 환경에서는 외부 스토리지(S3, NAS 등)와 마운트하여 사용합니다.

## 커넥터/파이프라인 모범 사례

- `DataFeedConnector` 추상화로 파일(`FileDataFeedConnector`), DB(`DatabaseDataFeedConnector`), API(`ApiDataFeedConnector` 예정)를 하나의 파이프라인으로 등록합니다. 파일 커넥터는 파일명 패턴으로 피드 타입(직원/조직/휴일)을 판별하고, 필요한 속성(예: 국가 코드)을 attributes에 포함해 템플릿에 전달합니다.
- 커넥터는 `dw_source_feeds` 큐 테이블에서 `PENDING` → `PROCESSING` → `COMPLETED/FAILED`로 상태를 관리합니다.
- Google Cloud Data Fusion, AWS Glue 처럼 **idempotent ingestion + checkpoint 메타데이터** 구조를 사용하여 재시작 시 중복 처리를 방지합니다.
- `dw.ingestion.database.enabled=true` 로 설정하면 DB 커넥터가 활성화되며, 배치 크기(`batch-size`) 설정으로 원본 DB 부하를 제어할 수 있습니다.
- SaaS/API 커넥터는 OAuth 토큰 로테이션, Rate Limit 대응 등을 위한 공통 헬퍼를 사용합니다.

## 캐싱 전략

- 직원: `DwEmployeeDirectoryService`가 `CacheNames.DW_EMPLOYEES` (Caffeine + Redis)로 활성 레코드를 캐싱합니다. 배치 동기화 시 `HrEmployeeSynchronizationService`가 자동으로 캐시를 무효화하여 최신 상태를 유지합니다.
- 조직: `DwOrganizationTreeService`가 전체 조직 트리를 메모리/캐시 계층에 재구성하고, `DwOrganizationQueryService`는 RowScope별 요청을 캐시된 트리에서 바로 필터링합니다. 조직 배치 동기화 및 관리 작업이 완료되면 `DwIngestionService`와 `HrOrganizationSynchronizationService`가 `CacheNames.DW_ORG_TREE`를 비웁니다.
- 캐시 TTL/사이즈는 `cache.*` 프로퍼티로 조정하며, Redis가 켜져 있으면 2단계 캐시(central + local)로 동작합니다.
- 운영자는 `/api/admin/policies/caches/clear` 엔드포인트를 이용해 `DW_EMPLOYEES`, `DW_ORG_TREE`, `ORGANIZATION_ROW_SCOPE`, `LATEST_DW_BATCH` 등 관리형 캐시를 정책 UI에서 즉시 비울 수 있습니다(목록 미전달 시 전체 캐시를 정리).
