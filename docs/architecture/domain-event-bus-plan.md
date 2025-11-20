# Domain Event Bus Adoption Plan

## 1. 목표
- 권한 변경, DW 배치 완료, 파일 정책 업데이트 등의 도메인 이벤트를 Kafka/EventBridge 기반 버스로 발행/구독하여 모듈 해제와 비동기 워크플로우를 지원한다.
- Outbox 패턴과 결합해 최소 1회 전송(at-least-once)을 보장하고, 감사를 위한 이벤트 추적을 추가한다.

## 2. 이벤트 유형
| 이벤트 | 발행 모듈 | 페이로드 핵심 | 구독자 |
| --- | --- | --- | --- |
| `PermissionChanged` | backend/auth | userId, feature/action set, version | server(read model), policy cache |
| `PolicyBundleApplied` | backend/policy | bundleId, gitHash, effectiveAt | server, dw-gateway |
| `DwBatchCompleted` | backend/dw-integration | batchId, orgRange, status | batch-app, read-model-worker |
| `FilePolicyUpdated` | backend/server/file module | fileType, maskingRuleId | masking service |

## 3. 기술 선택
- **Kafka (MSK)**: 고처리량, 파티션 관리 가능. 배치/실시간 모두 적합.
- **EventBridge**: SaaS 통합/라우팅 용이, 운영 간소화.
- 결정: **2단계 접근**
  1. 내부 환경에서는 Kafka(로컬: Redpanda/Bitnami) 사용.
  2. 멀티 계정 연동 필요 시 EventBridge 룰을 통해 팬아웃.

## 4. 아키텍처
1. 각 모듈은 Outbox 테이블(`domain_event_outbox`)에 이벤트 저장 → `OutboxRelay` 가 Kafka topic 으로 publish.
2. Kafka topic 네이밍: `domain.permission-changed`, `domain.dw-batch-completed`, ...
3. Consumer 그룹: 서비스별 하나, Spring Kafka listener 로 처리.
4. 재처리: DLQ topic (`domain.permission-changed.dlq`) + Runbook.

## 5. 단계별 실행
- **Phase 1**: `platform` 에 Outbox 엔티티/Relay 추상화 확장 (`DomainEventPublisher`). 로컬 docker-compose 로 Kafka 구동.
- **Phase 2**: 권한/정책 변경 이벤트부터 Kafka publish, read model worker 가 구독해 캐시 무효화/재계산 수행.
- **Phase 3**: EventBridge 통합, cross-account 구독자 구성, observability (lag, throughput) 대시보드 구축.

## 6. 관측/보안
- Metrics: `domain_events_published_total`, `outbox_backlog`, `consumer_lag` (Prometheus + Grafana).
- Audit: 이벤트 payload 를 S3/Glue catalog 에도 적재.
- 보안: Kafka SASL/SCRAM, PrivateLink. EventBridge 는 IAM policy 제한.

## 7. 리스크/완화
- **이벤트 스키마 변경** → Schema Registry + 호환성 검사.
- **중복 처리** → 이벤트 ID 저장, listener idempotency 확보.
- **운영 비용** → 개발 단계에서는 Redpanda 단일 노드, Prod 는 MSK serverless 고려.

## 8. TODO
- [ ] Outbox 스키마/엔티티 확장 (eventType, payload, headers).
- [ ] Kafka docker-compose + Testcontainers 지원 추가.
- [ ] `DomainEventPublisher` 인터페이스/테스트 구현.
- [ ] 초기 이벤트(`PermissionChanged`, `PolicyBundleApplied`) publish/subscribe 코드 작성.
- [ ] Observability 대시보드/Runbook 추가.
