# RowScope/Organization Cache Invalidation Channel Plan

## 1. 목표
- `AuthContextHolder`, `PermissionEvaluator`, `DwOrganizationQueryService` 등이 유지하는 RowScope/조직 트리 캐시를 중앙 Pub/Sub 채널로 동기화하여 stale 데이터로 인한 권한 오류를 최소화한다.
- 캐시 무효화는 최소 500ms 내에 전 모듈로 전파되도록 하며, 이벤트 유실 시 재동기화(backfill) 수단을 제공한다.

## 2. 범위
- 대상 캐시: `RowScopeCache`, `PermissionMenuCache`, `OrganizationTreeCache`, `SensitiveDataMaskingRuleCache`.
- 소비자: `backend/server`, `backend/dw-gateway`, 향후 `dw-worker`, `batch-app`.
- 인프라: Redis Pub/Sub (AWS Elasticache / MemoryDB). 향후 Kafka 전환 가능하도록 추상화.

## 3. 아키텍처
1. **Channel Topics**
   - `cache-invalidation.rowscope`
   - `cache-invalidation.permission-menu`
   - `cache-invalidation.organization`
   - `cache-invalidation.masking`
   각 메시지는 `{ "type": "RowScope", "tenantId": "foo", "scopeId": "bar", "version": 12 }` 구조.
2. **Publisher**
   - 정책 변경 플로우 (`PolicyAdminPort`, GitOps Webhook) 와 DW 스냅샷 커밋 (`DwIngestionOutboxRelay`).
   - Spring `CacheInvalidationPublisher` → Redis `ReactiveStringRedisTemplate.convertAndSend`.
   - 실패 시 Outbox 테이블에 저장 후 재시도.
3. **Subscriber**
   - 각 모듈에 `CacheInvalidationListener` (Spring @EventListener + RedisMessageListenerContainer) 구성.
   - 메시지 수신 시 캐시 키 pattern 삭제/버전 비교 후 재로딩.
4. **Fallback**
   - `cache-reconcile` Scheduled job이 Redis Stream/DB 스냅샷과 로컬 캐시 버전을 비교해 누락 이벤트를 보완.

## 4. 데이터 흐름 예시
```
permission update -> PolicyAdminService -> publish(CacheInvalidationEvent)
  -> Outbox insert -> OutboxRelay -> Redis Pub/Sub
    -> server CacheInvalidationListener -> RowScopeCache.evict(tenantId/scopeId)
```

## 5. 비기능 요구사항
- **전파 지연**: 99% < 500ms (Redis subnet 내).
- **신뢰성**: Outbox+재시도, 구독자 Sidecar health 체크.
- **관측성**: `cache_invalidation_published_total`, `cache_invalidation_processed_total`, `invalidation_latency_histogram` Prometheus 지표.
- **보안**: Redis ACL, TLS, VPC 내 통신.

## 6. 단계별 실행
1. 인터페이스 정의: `CacheInvalidationPublisher`, `CacheInvalidationListener`, `CacheInvalidationEvent` (`backend/platform`).
2. Redis Pub/Sub 빈 구성 (`platform` auto-config) + 통합 테스트.
3. `policy`, `auth`, `dw-integration` 모듈에서 정책 변경과 DW 스냅샷 완료 시 이벤트 발행.
4. `server`, `dw-gateway`, `batch-app` 의 캐시 레이어에서 구독자 적용.
5. 재동기화 job 및 runbook (`docs/runbooks/cache-invalidation.md`) 작성.

## 7. 리스크 및 대응
- **이중 발행**: 이벤트 ID + deduplication set 관리.
- **Subscriber 다운**: Health check 실패 시 알람, 재기동 후 Outbox replay 수행.
- **Redis 장애**: Multi-AZ + 자동 failover, 임시 fallback 으로 캐시 TTL을 짧게 설정.

## 8. 후속 TODO
- [ ] Outbox 스키마에 cache invalidation event 타입 추가.
- [ ] `CacheInvalidationPublisher`/`Listener` 구현.
- [ ] Redis connection 설정 및 IaC 업데이트.
- [ ] Runbook/알람 룰 추가.
