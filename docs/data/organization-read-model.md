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
