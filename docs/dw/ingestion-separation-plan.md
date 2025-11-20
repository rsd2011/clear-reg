# DW Ingestion Service Separation Plan

이 문서는 `backend/server`에 탑재된 DW ingestion REST 엔드포인트를 전용 서비스(`dw-gateway`)와 배치 워커(`dw-worker`)로 분리하기 위한 단계별 설계를 정리한다.

## 목표
1. 온라인 API 서버(backend/server)의 부담을 줄이고 배치/파일 업로드 트래픽을 격리한다.
2. 배치 실패 시 재시작/스케일 아웃을 독립적으로 수행할 수 있다.
3. 향후 S3/SQS/Kafka 기반 확장 시 서비스 단위로 배포/롤백 가능하게 한다.

## 현재 구조
- `backend/server`에서 `/api/dw/batches/*`, `/api/admin/dw-ingestion/policy` 등 REST 엔드포인트 제공.
- `backend/batch-app`는 Quartz + Spring Batch로 ingest 작업 실행.
- 두 모듈 모두 동일한 DB와 캐시를 참조하며 인증/권한 컨텍스트 공유.

## 제안 구조
```
[dw-gateway]  --(REST/GraphQL)--> ingest commands (create batch, upload metadata)
   |
   +--> SQS/Kafka queue (ingest jobs)
            |
            v
       [dw-worker] ----> DW ingestion DB / storage
```
- **dw-gateway**: Spring Boot REST 서비스. 기능: 정책 조회/갱신, 배치 상태 조회, 업로드 pre-signed URL 생성 등.
- **dw-worker**: 현재 `backend/batch-app`을 확장/대체. 큐 소비 + Spring Batch 실행 (`DwIngestionJobQueue` -> 현재는 in-memory, 향후 SQS/Kafka 교체 예정).

## 단계별 로드맵
1. **Phase 1** – 코드 구조화
   - `dw-integration` 모듈에서 사용 중인 REST DTO/서비스를 interface화 (예: `DwIngestionPolicyPort`, `DwBatchPort` 이미 도입).
   - gateway/worker 공통 코드 패키지화 (storage, DTO, policy service 등).
2. **Phase 2** – 새로운 서비스 생성
   - `backend/dw-gateway` (새 Gradle 모듈) → Spring Boot, security 설정 최소화.
   - 기존 `/api/dw/*` 컨트롤러를 gateway 모듈로 이동.
3. **Phase 3** – 메시지/큐 도입 준비
   - `dw-worker` 에서 큐/Outbox 소비 구조 설계 (Kafka/SQS). 초기에는 in-memory queue (`DwIngestionJobQueue`, `InMemoryDwIngestionJobQueue`) + DB polling fallback.
   - 인프라 준비 시 queue 연결.
4. **Phase 4** – 데이터 경계 정리
   - gateway는 파일 메타데이터만 저장, 실제 ingestion은 worker가 수행.
   - 정책/캐시 엔드포인트는 gateway에서만 제공하고 worker는 내부 API/queue로만 접근.

## 고려 사항
- 인증/권한: gateway만 외부 노출, worker는 내부 서비스 계정으로 동작.
- 배포: docker image 2개 (gateway, worker). Helm chart 분리.
- 공통 설정: `dw.in` 네임스페이스 공유, `policy` 모듈과 동일.
- 관측성: 각 서비스에 Actuator + Prometheus 스크랩 추가.

## 차후 작업
- S3 업로드/Pre-signed URL 기능 추가 시 gateway에서 처리.
- worker에 Idempotent checkpoint 테이블 추가 → Outbox/CDC 연동 기반.
