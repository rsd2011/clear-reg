# Read Model Overview (Consolidated)

## From: read-model-plan.md

# Read Model Materialization Plan

## 1. 목적 및 범위
- 조직 트리, 메뉴, 권한 메뉴(read-only navigation) 조회를 밀리초 단위로 응답하기 위해 CQRS 기반 읽기 모델을 별도 저장소(Redis/ElastiCache/MemoryDB)에 구축한다.
- `backend/server`는 기존 port(`PolicyAdminPort`, `DwOrganizationPort`)를 통해 쓰기 모델과 상호작용하고, 읽기 트래픽은 새로운 `ReadModelPort` 계층을 통해 캐시를 직접 조회한다.
- 배치/실시간 동기화를 모두 지원하되, 99% 요청에서 캐시 HIT를 보장하는 것을 목표로 한다.

## 2. 비기능 요구사항
1. **지연 시간**: 조직/메뉴 조회 API p95 < 50ms (현재 180~220ms).
2. **일관성**: 정책 변경 후 1분 이내 최종 일관성, 중요 액션은 관리자가 즉시 재생성 트리거 가능.
3. **가용성**: Redis Multi-AZ 또는 MemoryDB 사용, 장애 시 fallback 으로 RDB 조회 가능해야 함.
4. **감사/추적**: 각 read model 스냅샷은 버전/해시로 저장하여 변경 감사 가능하게 한다.

## 3. 대상 Read Model 정의
| 모델 | 키 스키마 | 페이로드 | 소스 | 조회 주체 |
| --- | --- | --- | --- | --- |
| OrganizationTreeRM | `org:{tenantId}` | 계층 트리(압축 JSON) + RowScope 메타 | `dw-integration` 조직 스냅샷 + `policy` RowScope | 서버, 배치 |
| MenuRM | `menu:{tenantId}:{locale}` | 메뉴 노드 + Feature/Action 권한 태그 | `policy` Feature/Action + 템플릿 | 서버 |
| PermissionMenuRM | `perm-menu:{tenantId}:{principal}` | 사용자별 메뉴 + masking tag | `auth` 권한 + `policy` | 서버/프론트 |

## 4. 시스템 구성
### 4.1 이벤트/변경 소스
- `permission-groups.yml`/정책 변경 → GitOps 파이프라인 → `PolicyAdminPort`가 `ReadModelRebuildRequested` 이벤트 발행.
- DW 조직 스냅샷 완료 → `DwIngestionOutboxRelay`가 `OrganizationSnapshotCommitted` 이벤트 발행.
- 사용자 권한 변경 → `AuthPermissionService`가 `PermissionSetChanged` 이벤트 발행.

### 4.2 동기화 파이프라인
1. 이벤트는 모듈 내부 `ApplicationEventPublisher` 또는 Outbox-Queue(Kafka/SQS)로 push.
2. `read-model-worker` (신규 Spring profile 또는 모듈) 이 큐를 구독하여 Aggregate Loader 호출.
3. Loader는 RDB (PostgreSQL) 에서 필요한 데이터를 projection(SQL/Querydsl DTO)으로 읽어 Redis에 bulk 저장한다.
4. 저장 시 Lua 스크립트 또는 `SET key value EX ttl NX` 형태로 버전 충돌을 방지하고, `hash:{model}` 로 메타(버전, 생성 시각)를 기록한다.

### 4.3 조회 경로
- `backend/server` controller → 서비스 → `ReadModelPort` (`OrganizationReadModelPort`, `MenuReadModelPort`).
- 캐시 MISS 시 fallback 으로 기존 서비스 호출 + 동기 rebuild(서킷 브레이커/비율 제한 포함).
- RowScope 적용 API 는 read model에 미리 계산된 scope 를 포함하고, 요청 시 principal scope 와 AND 연산만 수행.

### 4.4 운영/관측
- Redis keyspace hit/miss, TTL 만료율, rebuild latency 를 Prometheus exporter 로 노출.
- Runbook: TTL 만료 후 MISS 폭증 시 `read-model-worker` 재시작 & 강제 rebuild 스크립트 제공.

## 5. 단계별 실행 (연동 docs/architecture-todo.md > Section E)
1. **Phase 0 (현재)**: 설계/문서 완료, 샘플 데이터셋/계약 확정.
2. **Phase 1 (4주)**: `ReadModelPort` 인터페이스 + Redis client (Lettuce) 설정, OrganizationTreeRM 부터 적용, 통합 테스트 작성.
3. **Phase 2 (6주)**: MenuRM/PermissionMenuRM 확장, Outbox → Queue 연동, `read-model-worker` profile 추가.
4. **Phase 3 (6주)**: 멀티-리전/멀티 AZ replication, TTL 정책 최적화, fallback 모니터링 & 알람 연동.

## 6. 리스크 및 대응
- **이벤트 누락**: Outbox → Queue 재시도, 이벤트 재생 스크립트 제공.
- **캐시 폭주**: Lua 기반 rate limit, 백오프 포함한 rebuild queue.
- **데이터 정합성**: Projection SQL + ArchUnit 테스트로 read 모델 DTO 의존성 검증.
- **운영 복잡도**: `docs/runbooks/read-model-rebuild.md` 작성 예정, Terraform module 으로 Redis 배포 자동화.

## 7. 후속 작업
- `backend/server`에 `OrganizationReadModelPort` 등 추가 및 기존 서비스 주입선 정리.
- `read-model-worker` 모듈 생성 검토 (또는 `dw-gateway` profile 확장) → Section E Task 1과 연계.
- `docs/architecture-todo.md` E-2 항목 업데이트 및 링크.
\n---\n
## From: organization-read-model.md

# Organization Read Model (Redis PoC)

## 목적
- 조직 트리 조회 API의 p95 지연 시간을 50ms 이하로 줄이기 위해 Redis에 미리 전개된 Read Model을 제공한다.
- `DwOrganizationQueryService`는 Redis Snapshot(`OrganizationReadModelPort`)을 우선 조회하고, 없을 경우 기존 JPA 스냅샷으로 폴백한다.

## 데이터 모델
- Redis Key: `rm:org:{tenantId}` (기본 `tenantId=default`, `keyPrefix=rm:org`)
- 값: `OrganizationTreeReadModel` JSON
  - `version`: SHA-256(`organizationCode:version`) 기반 해시
  - `generatedAt`: UTC 생성 시각
  - `nodes`: `DwOrganizationNode` 리스트 (RowScope 계산을 위한 모든 메타 포함)
- TTL: 기본 30분 (`readmodel.organization.ttl`)

## 동작 플로우
1. `RedisOrganizationReadModelPort.rebuild()`  
   - `DwOrganizationTreeService.snapshot()` → flatten → SHA 해시 계산 → Redis `SET key value EX ttl`.
2. `load()`  
   - Redis 조회 성공 시 DTO 반환. MISS 발생 시 `refreshOnMiss=true`일 때 자동 재생성.
3. `CacheMaintenanceService`  
   - `/api/admin/policies/caches/clear` 요청에 `ORGANIZATION_READ_MODEL` 포함 시 `evict()` → `rebuild()` 수행.

## 설정 (server `application.yml`)
```yaml
readmodel:
  organization:
    enabled: true
    key-prefix: rm:org
    tenant-id: default
    ttl: 30m
    refresh-on-miss: true
```

## 테스트 전략
- `RedisOrganizationReadModelPortTest`는 Embedded Redis 로드 후 `rebuild/load/evict` 흐름을 검증.
- `CacheMaintenanceServiceTest`에 ORG Read Model 타겟이 포함되었음을 확인.
\n---\n
## From: read-model-worker-plan.md

# Read Model Worker Plan (Org/Menu/Permission)

## 목표
- 조직/메뉴/퍼미션 메뉴 Read Model을 Redis에 재생성하는 워커 프로파일을 제공하고, API/이벤트 트리거 기반으로 재빌드할 수 있게 한다.

## 구현 체크리스트
- [ ] profile `read-model-worker` 활성화 시: OrganizationReadModelPort/MenuReadModelPort/PermissionMenuReadModelPort 빈이 필요(현재 organization/menu 소스는 placeholder).
- [x] PermissionMenuReadModelSource: PermissionGroup 기반 생성(`PermissionMenuReadModelSourceImpl`).
- [x] Worker/controller: `/internal/read-model/**/rebuild` 엔드포인트, worker bean 생성(readmodel.worker.enabled=true).
- [ ] Organization/Menu ReadModelSource를 실제 데이터로 교체 (현재 Default*Source placeholder).
- [ ] E2E: principal로 rebuild 호출 → Redis 키/값 확인 (embedded Redis) + 권한 그룹 변경 이벤트 후 read model 갱신 확인.

## 운영 플래그 예시
```yaml
readmodel:
  worker:
    enabled: true
  organization:
    enabled: true
  menu:
    enabled: true
  permission-menu:
    enabled: true
```

## 후속
- Organization/Menu 소스 구현 후, CacheInvalidation/PermissionSetChanged 이벤트와 연계해 자동 재빌드 E2E 테스트 추가.
\n---\n
## From: cache-invalidation-plan.md

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
