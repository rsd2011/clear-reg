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
