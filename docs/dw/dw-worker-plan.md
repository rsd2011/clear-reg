# DW Worker 분리/확장 계획

## 1. 목표
- 실시간 API(`dw-gateway`)와 배치/큐 기반 작업(`dw-worker`)을 분리해 스케일링 정책과 장애 영향을 격리한다.
- `DwIngestionJobQueue` + Outbox를 통해 idempotent하게 작업을 전달하고, 향후 SQS/Kafka 등 외부 큐로 전환하기 쉬운 구조를 마련한다.

## 2. 아키텍처 개요
```
[backend/server] --(ports)--> [dw-gateway] --(enqueue)--> DwIngestionJobQueue (DB/Outbox/Redis)
                                                           |
                                                           v
                                                   [dw-worker]
                                                        |
                                                        v
                                             DW staging/storage + audit
```
- `dw-gateway`: REST, 정책/상태 API, 업로드 핸들러, Outbox 기록.
- `dw-worker`: Spring Batch + Quartz(or Spring Scheduling). 큐 레코드를 polling/stream 하여 DW 동기화 실행.
- 공통 모듈: `backend/file-core`, `backend/dw-integration` 재사용.

## 3. dw-worker 모듈 설계
1. **프로젝트 구조**
   - 새 Gradle 모듈 `backend/dw-worker` (Spring Boot CLI).
   - 의존성: `dw-integration`, `file-core`, `platform`, `auth`(AuthContext util) 등.
2. **큐/Outbox 처리**
   - DB Outbox 테이블(`dw_ingestion_outbox`): 상태 `PENDING`, `SENT`, `FAILED`.
   - `DwIngestionJobQueue` 인터페이스 구현: `enqueue(DwIngestionCommand)`는 Outbox insert.
   - `DwIngestionOutboxRelay` (gateway): 트랜잭션 커밋 후 DB→Queue 전달 (초기엔 같은 DB polling, 향후 SQS/Kafka).
3. **Worker 실행 흐름**
   - `@Scheduled` or Spring Batch Job이 Outbox(PENDING) 조회 → 상태 `PROCESSING` → 실제 `DwIngestionService` 실행 → 성공시 `COMPLETED` + 감사 로그, 실패 시 재시도 카운트 증가.
   - 재시도 정책: 5회 Exponential Backoff, 최대 1시간.
4. **구성/모니터링**
   - 별도 `application-worker.yml`: 큐 polling interval, batch size, 재시도 설정.
   - Prometheus 지표: `dw_worker_jobs_total{status=..}`, `dw_worker_lag_seconds`, `dw_worker_retries_total`.
5. **보안/권한**
   - Worker는 내부 서비스 계정만 사용, REST 인증 불필요. DB 접근은 최소 권한(RDS IAM Role)으로 제한.

## 4. 단계별 TODO
1. **Phase 1** (현 단계)
   - `docs/dw/dw-worker-plan.md` 작성 (본 문서).
   - `backend/file-core` + `dw-integration` 재사용 여부 검증.
2. **Phase 2**
   - Gradle 모듈 `backend/dw-worker` 생성, Boot strap class/Config 추가.
   - Outbox 테이블 + JPA 엔티티 정의, `DwIngestionJobQueue` 구현.
3. **Phase 3**
   - Worker Job (`DwWorkerJob`) 구현: 큐 polling → `DwIngestionService` 호출.
   - 상태/재시도/감사 로깅, Runbook 작성.
4. **Phase 4**
   - 인프라: ECS/K8s 배포 템플릿, AutoScaling 정책.
   - 큐 전환 PoC (SQS/Kafka) 및 Feature Flag.

## 5. 리스크 및 대응
- **큐 중복 처리**: Outbox + unique `job_id` + idempotency token으로 보장.
- **장애 감지**: CloudWatch/Grafana alert (lag, failure rate). Runbook에 수동 재처리 절차 기재.
- **배포 복잡도**: GitOps로 worker chart 분리, Blue/Green 롤아웃 적용.

## 6. 산출물/문서
- `docs/dw/dw-worker-plan.md` (본 문서)
- 추후: `backend/dw-worker/README.md`, `docs/runbooks/dw-worker.md`
- Architecture TODO E-1 항목에 본 문서 링크 추가 예정.
