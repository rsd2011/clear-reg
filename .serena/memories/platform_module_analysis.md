# Platform 모듈 구조 분석 결과

## 1. SPI(Service Provider Interface) 패턴 사용 현황

Platform 모듈에서 **명확한 SPI 패턴**이 구현되어 있습니다.

### 주요 Port/Provider 인터페이스 (SPI)

1. **CurrentUserProvider** (`common/security/`)
   - 현재 사용자 컨텍스트 조회
   - @FunctionalInterface
   - 구현체: `AuthCurrentUserProvider` (admin 모듈)

2. **ScheduledJobPort** (`common/schedule/`)
   - 배치 작업 인터페이스 (중앙 스케줄러 통합용)
   - 메서드: jobId(), trigger(), runOnce(Instant)
   - 구현체: 11개+ (FileScanRescheduler, FileAuditOutboxRelay, DwIngestionOutboxRelay, OutboxDraftAuditRelay, AuditColdArchiveScheduler, AuditMonthlyReportJob, AuditLogRetentionJob, AuditArchiveJob, AuditPartitionScheduler, AuditColdMaintenanceJob)

3. **MaskingStrategy** (`common/masking/`)
   - 마스킹 여부 및 방식 결정
   - 메서드: shouldMask(target), apply(raw, target, maskedValue)
   - 구현체: `PolicyMaskingStrategy` (common 모듈)

4. **UnmaskAuditSink** (`common/masking/`)
   - 언마스킹 감시 이벤트 처리
   - 메서드: handle(UnmaskAuditEvent)
   - 구현체: `JpaUnmaskAuditSink` (audit 모듈)

5. **CacheInvalidationPublisher** (`common/cache/`)
   - 캐시 무효화 이벤트 발행
   - 메서드: publish(CacheInvalidationEvent)
   - 구현체: `RedisCacheInvalidationPublisher` (server 모듈)

6. **RowAccessPolicyProvider** (`common/policy/`)
   - 행 수준 접근 정책 제공
   - 메서드: evaluate(RowAccessQuery)
   - 구현체: `RowAccessPolicyProviderAdapter` (admin 모듈)

7. **PolicySettingsProvider** (`common/policy/`)
   - 정책 토글 설정 제공
   - 메서드: currentSettings(), partitionSettings(), batchJobSchedule()
   - 구현체: `SystemConfigPolicySettingsProvider` (admin 모듈)

8. **MaskingPolicyProvider** (`common/policy/`)
   - 마스킹 정책 제공
   - 메서드: evaluate(MaskingQuery)

## 2. 공통 인터페이스/추상클래스 구조

### 추상 클래스

1. **PrimaryKeyEntity** (`common/jpa/`)
   - UUID 기반 엔티티 기본 클래스
   - Persistable<UUID>, Serializable 구현
   - 모든 JPA 엔티티의 기본

2. **AbstractEnumSetJsonConverter<E>** (`common/jpa/`)
   - Enum Set을 JSON 배열로 저장
   - AttributeConverter 구현
   - 제네릭 기반 구현체 작성 패턴 제공

3. **AbstractIdentifier** (`common/identifier/`)
   - 식별자 공통 기능 (검증, 마스킹)
   - normalizeAndValidate(), mask() 제공
   - 모든 식별자 타입(ResidentRegistrationId, EmailAddress 등)의 기본

### 핵심 인터페이스

1. **ErrorCode** (`common/error/`)
   - 에러 코드 정의 인터페이스

2. **Maskable<T>** (`common/masking/`)
   - 마스킹 가능 객체

## 3. 다른 모듈들이 참조하는 패턴

Platform 모듈은:
- **Port/Provider 제공**: 다른 모듈이 구현하는 인터페이스 제공
- **공통 기반 클래스**: 모든 모듈이 상속받는 기본 클래스 제공
- **Value Objects**: 식별자, 값 객체 공유
- **정책 쿼리 객체**: RowAccessQuery, MaskingQuery 등

## 4. Soft Delete 관련 기존 구현

**검색 결과: 없음**
- soft delete, deleted_at, is_deleted 등의 구현이 없음
- 모든 엔티티는 hard delete 패턴 사용 중

## 5. 패키지 구조

```
backend/platform/src/main/java/com/example/common/
├── annotation/          # @Sensitive 등
├── api/dto/            # ErrorResponse
├── cache/              # Cache invalidation (Publisher SPI)
├── codegroup/          # @ManagedCode, @CodeValue 애너테이션
├── error/              # ErrorCode, BusinessException
├── export/             # Excel, PDF 유틸
├── file/               # FileStatus, FileDownload
├── identifier/         # 식별자 타입들 (AbstractIdentifier 상속)
├── jpa/                # PrimaryKeyEntity, AbstractEnumSetJsonConverter
├── masking/            # Masking 관련 (Strategy SPI, Maskable 인터페이스)
├── orggroup/           # 조직 관련 타입
├── policy/             # Policy Provider SPI들 (RowAccess, Masking, Settings)
├── schedule/           # ScheduledJobPort SPI, BatchJob 관련
├── security/           # CurrentUserProvider SPI, FeatureCode, ActionCode
├── ulid/               # ULID 관련 (Serializer, Deserializer)
├── util/               # KoreanRomanizer
└── value/              # Value Objects (GenderCode, MoneyAmount 등)

testFixtures/
└── testing/bdd/        # BDD 테스트 시나리오
```

## 6. 설계 원칙

1. **Hexagonal Architecture**: Port (인터페이스) 기반 의존성 역전
2. **SPI Pattern**: 확장 포인트를 인터페이스로 정의
3. **Value Objects**: 도메인 값 타입 중앙화
4. **Generic Converters**: 제네릭 기반 JPA 컨버터 (AbstractEnumSetJsonConverter)
5. **ThreadLocal Context**: RowScopeContextHolder, MaskingContextHolder 등으로 컨텍스트 관리
