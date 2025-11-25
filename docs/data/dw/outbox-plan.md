# DW Ingestion Outbox → Message Broker Plan

## 현재 상태 (In-memory Relay)
- `DwIngestionOutboxService` 에서 `dw_ingestion_outbox` 테이블에 Pending 레코드를 적재한다.
- `DwIngestionOutboxRelay`(batch-app) 가 스케줄링되어 `findTop50ByStatusOrderByCreatedAtAsc` 로 PENDING 레코드를 락 없이 조회 → IN_MEMORY `DwIngestionJobQueue` 로 실행 → `markSent`.
- 제한 사항
  - 배치 앱 인스턴스 하나만 사용해야 안전함 (동시 실행 시 동일 레코드 처리 필요).
  - 재시작/다중 인스턴스 대비 재시도/락 전략이 없다.
  - 큐가 메모리 기반이므로 Failover 불가.

## 목표
1. Outbox 테이블 → 외부 메시지 브로커(SQS/Kafka)로 relay, 멱등성 보장.
2. 다중 worker 인스턴스가 메시지를 소비하더라도 중복 처리되지 않도록 설계.

## 설계 제안
### Outbox Schema 확장
- `dw_ingestion_outbox`
  - `id UUID PK`
  - `job_type VARCHAR`
  - `payload JSON` (향후 확장 대비)
  - `status ENUM(PENDING,SENDING,SENT,FAILED)`
  - `available_at TIMESTAMP`
  - `locked_at TIMESTAMP`
  - `locked_by VARCHAR` (optional)

### Relay → SQS 예시 (Kafka도 가능)
1. `DwIngestionOutboxRelay` 가 PENDING 레코드를 `FOR UPDATE SKIP LOCKED` 로 조회.
2. 각 레코드에 대해 `SQSMessage` 생성 (MessageGroupId=job_type, deduplicationId=id).
3. SQS 전송 성공 시 `status=SENT`, 실패 시 `status=FAILED` + 재시도 정책 적용.
4. Worker (`dw-worker` 또는 batch-app) 는 SQS listener → 메시지를 `DwIngestionJobQueue` 로 전달.

### 멱등성 전략
- Outbox `id` 를 메시지 deduplication key 로 사용 (SQS FIFO 혹은 Kafka key).
- Worker는 `DwIngestionJob` 실행 시작 전에 `dw_ingestion_outbox`의 상태를 확인해 이미 처리된 job인지 검증.
- 재시도: `FAILED` 레코드는 별도 스케줄러가 일정 시간 후 `PENDING` 으로 복구.

### 단계별 마이그레이션
1. Outbox 테이블에 `payload`, `locked_*` 컬럼 추가, `claimPending` 로직을 `FOR UPDATE SKIP LOCKED` 쿼리로 교체.
2. `DwIngestionOutboxRelay`를 빈번한 SQS 작성기로 교체 (Spring Cloud AWS 혹은 AWS SDK 사용).
3. `InMemoryDwIngestionJobQueue`는 기본 구현을 유지하되, SQS consumer가 메시지를 받고 `DwIngestionJobQueue`를 호출하도록 분리.
4. 멀티 인스턴스 검증 후 in-memory relay 제거.

## 후속 작업
- SQS/Kafka 통합 라이브러리 선택 및 인프라 Terraform 템플릿 준비.
- Outbox 엔티티에 payload/lock 필드 추가하는 DB migration 작성.
- Worker 측 SQS Consumer 구현 (`@SqsListener` 등) → DwIngestionJobQueue 호출.
- Retry/Alerting 정책 문서화.
