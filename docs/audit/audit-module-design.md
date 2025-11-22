# 중앙 감사(Audit) 모듈 설계 초안

## 1) 요구사항·법적 전제 정리
- 대상: Java 21 / Spring Boot 3.3.x 멀티모듈(backend/platform, auth, dw-integration, policy, server, batch-app).
- 규제 반영: 접속기록 기본 2년(고위험), 신용정보 처리 3년, 전자금융거래 5년을 정책 기본값으로 설정(정책으로 조정 가능).
- 개인정보 최소수집·마스킹, 위·변조 방지(append-only/hash-chain), 월 1회 점검, 민감 응답 사유 필수.
- 감사 실패 시 트랜잭션 영향은 정책 기반 선택(STRICT=롤백, ASYNC_FALLBACK=비동기 보류).
- 본 문서는 법률 자문이 아니며, 적용 전 내부 컴플라이언스 검증이 필수.

## 2) 전체 아키텍처 및 통합 전략
- 신규 `backend/audit` 모듈을 도입: `audit-core`(API/port + domain), `audit-infra`(JPA/Kafka/hash-chain 저장소), `audit-policy-client`(policy 캐시).
- Policy 기능은 기존 `backend/policy`를 확장하여 감사 토글·보존·마스킹·민감 API·임계치 관리.
- 연동 패턴
  - 동기: `AuditPort.record(event, mode)` 호출. mode=STRICT|ASYNC_FALLBACK.
  - 비동기: Kafka 토픽 `audit.events.v1` 발행 → audit 모듈 consumer 저장.
  - SDK/AOP: `@Audit(action=..., sensitive=true, reasonRequired=true)`로 컨트롤러/서비스 단 공통 처리.
- 의존 방향: 상위 모듈(server, batch-app, auth, dw-integration)이 audit 포트를 의존, audit은 platform 공용 타입 최소 참조.

## 3) 기존 멀티모듈 분석 및 마이그레이션 전략
- 예상 책임 위치
  - `auth`: 로그인/세션/비밀번호 변경 로그, RowScope 활용.
  - `server`: MVC 필터/인터셉터 기반 접속기록.
  - `dw-integration`: 배치·대량 조회 로깅, HR 데이터 접근.
  - `policy`: 권한·정책 변경 이력.
  - `platform`: 공통 보안 유틸(RowScope 등) → AuditContext에서 재사용.
- 단계적 플랜
  1) `audit-core` 도입, 공통 `AuditEvent`/`AuditPolicySnapshot` 정의(레거시 로그 유지).
  2) Dual-write: 새 포트 호출 + 기존 로깅 병행, 정책 캐시 적용.
  3) 모듈별 전환: auth → server → dw-integration → batch-app, 직접 DB insert 제거.
  4) 레거시 로그 테이블/코드 제거, 문서 및 권한 정리.

## 4) 감사 이벤트 종류 및 공통 스키마 정의
- 공통 필드: eventId, eventTime, eventType, systemName, moduleName, action, actor{id,type,role,dept}, subject{type,key}, channel, clientIp, userAgent, deviceId, successYn, resultCode, errorMessage(masked), reasonCode, reasonText, legalBasisCode, riskLevel, tags, beforeSummary, afterSummary, extraJson, hashChain.
- 카테고리: 인증/접속, 개인정보·신용정보 조회/변경, DRM 해제·다운로드, 권한·정책 변경, 배치·대량 처리, 오류·우회 시도.

## 5) 데이터 모델 및 DB 설계 (PostgreSQL 예시)
```sql
CREATE TABLE audit_log (
  id               BIGSERIAL PRIMARY KEY,
  event_time       TIMESTAMPTZ NOT NULL,
  event_type       VARCHAR(64) NOT NULL,
  system_name      VARCHAR(32),
  module_name      VARCHAR(32),
  action           VARCHAR(64),
  actor_id         VARCHAR(64),
  actor_type       VARCHAR(16),
  actor_role       VARCHAR(64),
  actor_dept       VARCHAR(64),
  subject_type     VARCHAR(32),
  subject_key      VARCHAR(128),
  channel          VARCHAR(32),
  client_ip        INET,
  user_agent       VARCHAR(256),
  device_id        VARCHAR(128),
  success_yn       CHAR(1),
  result_code      VARCHAR(32),
  reason_code      VARCHAR(32),
  reason_text      VARCHAR(512),
  legal_basis_code VARCHAR(32),
  risk_level       VARCHAR(8),
  before_summary   VARCHAR(1024),
  after_summary    VARCHAR(1024),
  extra_json       JSONB,
  hash_chain       VARCHAR(128),
  created_at       TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_audit_time ON audit_log(event_time);
CREATE INDEX idx_audit_actor ON audit_log(actor_id, event_time);
CREATE INDEX idx_audit_subject ON audit_log(subject_type, subject_key);
CREATE INDEX idx_audit_type ON audit_log(event_type, module_name);

CREATE TABLE audit_reason (
  reason_code      VARCHAR(32) PRIMARY KEY,
  name             VARCHAR(128),
  description      VARCHAR(512),
  legal_basis_code VARCHAR(32),
  active_yn        CHAR(1),
  created_at       TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE audit_policy (
  policy_id        BIGSERIAL PRIMARY KEY,
  target_type      VARCHAR(16) NOT NULL, -- API|EVENT|MODULE
  target_key       VARCHAR(256) NOT NULL,
  enabled_yn       CHAR(1) NOT NULL,
  sensitive_api_yn CHAR(1),
  reason_required_yn CHAR(1),
  retention_days   INTEGER,
  risk_level       VARCHAR(8),
  mask_profile     VARCHAR(64),
  threshold_config JSONB,
  updated_by       VARCHAR(64),
  updated_at       TIMESTAMPTZ DEFAULT now()
);
```
- 개인정보 최소 수집: 원문 주민번호/계좌번호 저장 금지, 요약/마스킹/해시만 저장.

## 6) 개인정보 조회 사유·DRM 해제·로그인·민감 응답 처리 설계
- SENSITIVE_API: 정책 조회 결과 `sensitive_api_yn=true` → reasonCode/reasonText/legalBasisCode 필수, 누락 시 400 또는 정책상 허용된 시스템 코드만 통과.
- DRM: request/approval/execute 이벤트 분리, assetId·reason·approver·expiry·route 기록, 임계치 초과 시 경보 AuditEvent 추가.
- 로그인/접속: 성공/실패/락 사유·채널/IP/UA 기록, 실패 다수 시 riskLevel 상승 + 알림 훅.

## 7) Policy 모듈 기반 감사 정책 토글 및 SENSITIVE_API 처리
- 캐시: Caffeine 5분 TTL, policy 변경 이벤트 수신 시 무효화.
- 우선순위: endpoint > eventType > module > system default. 미정의 시 Secure-by-default(ON, reasonRequired=true).
- 필수 감사 비활성화 시: COMPLIANCE_ADMIN 이상, 사유 입력, 경고 노출, 변경 자체를 AuditEvent(policy-change)로 기록.

## 8) 보존기간·무결성·보안 설계
- 보존: policy.retention_days로 타입별 관리, 만료 시 purge 배치(append-only 원본은 WORM/S3 Object Lock 가능).
- 무결성: hash_chain(prev_hash+record) 저장, 옵션으로 외부 WORM/서명. DB는 append-only 파티션 권장.
- 암호화: TLS 전송, 디스크/컬럼 AES 암호화, 마스킹/요약 저장.
- 접근통제: 최소 권한, 조회/다운로드 행위도 AuditEvent로 기록, 월 1회 이상 점검/룰 기반 이상탐지.

## 9) API·메시지 설계
- REST
  - `POST /api/audits` : body=AuditEvent, header `x-audit-mode`(strict/async).
  - `GET /api/audits/{id}` : 상세 조회(마스킹 적용).
  - `GET /api/policies/resolve?endpoint=/v1/accounts/{id}` : 정책 조회.
  - `POST /api/policies/{target}/toggle` : 감사 on/off, 민감 API 설정 등.
- Kafka
  - Topic `audit.events.v1`: header(eventType,module,action,riskLevel), payload=공통 스키마 + extraJson.
  - DLQ `audit.events.dlq` 재처리 배치.
- 오류 전략: strict 모드 실패 시 업무 롤백, async 모드 실패 시 DLQ 후 경고.

## 10) Java + Spring 예시 코드 (요약)
```java
// audit-core
@Value @Builder
public class AuditEvent {
  UUID eventId; Instant eventTime; String eventType; String moduleName; String action;
  Actor actor; Subject subject; Channel channel;
  boolean success; String resultCode; String reasonCode; String reasonText; String legalBasisCode;
  RiskLevel riskLevel; String beforeSummary; String afterSummary; Map<String,Object> extra;
}

public interface AuditPort {
  void record(AuditEvent event, AuditMode mode);
  Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType);
}

// audit-infra AOP
@Aspect @Component @RequiredArgsConstructor
public class AuditAspect {
  private final AuditPort auditPort; private final PolicyClient policy;

  @Around("@annotation(audit)")
  public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
    var ctx = AuditContextHolder.current();
    var pol = policy.resolve(audit.endpoint(), audit.eventType());
    validateSensitive(pol, ctx); // reason/legalBasis required
    boolean success = false; String result = "OK";
    try {
      Object ret = pjp.proceed(); success = true; return ret;
    } catch (Exception ex) {
      result = ex.getClass().getSimpleName(); throw ex;
    } finally {
      auditPort.record(AuditEventBuilder.from(ctx, audit, pol)
          .success(success).resultCode(result).build(), pol.mode());
    }
  }
}
```

### 스켈레톤 경로 & 사용 예 (현재 커밋 상태)
- 코드 경로: `backend/audit/` 
  - 도메인: `com.example.audit.AuditEvent`, `AuditPolicySnapshot`, `AuditMode`, `AuditPort`
  - 인프라: `com.example.audit.infra.AuditRecordService`(JPA+Kafka dual-write), `infra.persistence.AuditLogEntity/Repository`
  - 정책 캐시: `com.example.audit.infra.policy.AuditPolicyResolver` (Caffeine, secure-by-default)
- 정책 토글 스키마 확장: `PolicyToggleSettings`에 `auditEnabled/auditReasonRequired/auditSensitiveApiDefaultOn/auditRetentionDays/auditStrictMode/auditRiskLevel` 추가 (기본 ON, reasonRequired=true, retention 730일, STRICT).
- server/auth 파일럿: `AuthService` 로그인·비밀번호 변경 성공 시 `AuditPort`에 ASYNC_FALLBACK 모드 dual-write.
- server 전역 파일럿: `RequestAuditInterceptor`를 `/api/**` 경로에 등록해 HTTP 요청 메타를 dual-write.
- Kafka 토픽 기본값: `audit.kafka.topic=audit.events.v1` (필요 시 프로퍼티로 재정의).
- JPA 테이블: `audit_log` 엔티티 스켈레톤 포함.

### 운영 프로퍼티 예시 (server `application.yml`)
```yaml
audit:
  kafka:
    topic: audit.events.v1
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### Policy API 응답/화면에 노출되는 감사 필드
- `PolicyView` 필드: `auditEnabled`, `auditReasonRequired`, `auditSensitiveApiDefaultOn`, `auditRetentionDays`, `auditStrictMode`, `auditRiskLevel`
- UI/관리자 콘솔에서 위 필드를 편집·표시하도록 반영 필요.

## 11) 테스트·운영·모니터링 전략
- 단위: AuditAspect의 reason 필수 검증, policy 캐시 미스 시 fallback, hash_chain 계산 테스트.
- 통합: Dual-write 경로 검증, strict 모드 롤백 여부, Kafka DLQ 재시도.
- 운영: 월간 접속기록 리포트, 이상행위 룰(심야 대량 조회/DRM 해제 빈발) 알림, SIEM 전송.
- 관측: audit write latency/error, Kafka lag, policy 캐시 hit/miss 메트릭 노출.

## 12) TODO 체크리스트 (우선순위 P1 > P2 > P3 > P4)
### 설계
- [x] `backend/audit` 모듈 구조 확정(core/infra/policy-client)
- [x] 공통 `AuditEvent`·`AuditPolicySnapshot`·`AuditMode` 스키마 문서화
- [x] Policy 스키마에 감사 토글·sensitive_api·retention·mask_profile 추가 정의

### 구현
- [x] AuditPort 구현(JPA 저장 + Kafka publisher 스켈레톤), hash_chain 계산 포함
- [x] `@Audit` 대체용 필터/인터셉터 등록(server)
- [x] PolicyClient + Caffeine 캐시(secure-by-default) 구현
- [x] SENSITIVE_API reason/legalBasis 검증 필터(server)
- [x] (P2) DRM 이벤트 전용 서비스/DTO 추가

### 마이그레이션
- [~] (P2) `auth` 로그인/비밀번호 변경 로그 → AuditPort dual-write 후 레거시 제거
- [~] (P2) `server` 컨트롤러 필터 로깅 → AOP/포트 전환 및 레거시 제거
- [~] (P2) `dw-integration` 배치/대량 조회 로깅 → AuditPort 사용, 직접 DB insert 제거 **(배치 목록/최신 조회는 AuditPort 연동 완료, 추가 대량 처리 전환 필요)**
- [x] (P2) `policy` 변경 이력 → AuditEvent(policy-change)로 남기기
- [ ] (P3) 불필요한 기존 로그 테이블/코드 제거 및 문서 업데이트

### 테스트
- [x] Policy 미정의 시 기본 ON/사유 필수 동작 스모크(필터 + 문서/Playwright)
- [x] Strict/forceUnmask → UnmaskAudit 적재 e2e 테스트
- [ ] (P4) Kafka DLQ/재처리 시나리오 검증(카프카 옵셔널)
- [~] (P2) 마스킹/summary에 원문 포함 여부 커버리지 확대

### 운영
- [ ] (P2) 보존기간별 파티션/아카이브 배치 스케줄링
- [ ] (P2) 월간 접속기록 점검 리포트 및 알림 대시보드 연동
- [x] (P2) 감사 로그 조회 권한 최소화, 조회 행위 자체 감사 기록 자동화 (AuditLogAccessAspect, allowed-roles)
- [ ] (P3) SIEM/외부 보안시스템 연동 및 전송 암호화 확인

## 13) 멀티 포맷(Excel/PDF/XML/Word/CSV/JSON) 출력 마스킹 설계
- 공통 원칙
  - 컨트롤러/필터에서 `DataPolicyMaskingFilter` → `MaskingTarget/MaskingContextHolder` 설정, `maskRule/maskParams`는 `MaskingFunctions.masker(policyMatch)`로 `UnaryOperator<String>` 생성
  - 출력 직전 단일 어댑터(OutputMaskingAdapter)에서 포맷 무관하게 마스킹 적용
  - 승인 역할 + `forceUnmaskFields/forceUnmaskKinds` 조합으로 필드 단위 해제 허용(사유 필수)
- 컴포넌트
  - `OutputMaskingAdapter<T>`: 포맷별 Writer 앞단에서 값→mask→Writer
  - 포맷별 훅: Excel(SXSSF) 셀 쓰기 전, CSV/JSON/XML 직렬화 시, PDF/Word 텍스트 삽입 전 `mask` 호출
  - `MaskRuleProcessor`(NONE/PARTIAL/FULL/HASH/TOKENIZE) + `Maskable` 값객체(RRN/카드/계좌/이름/주소 등)는 `MaskingService.render`
- 흐름
  1) 필터: `DataPolicyMaskingFilter` → `MaskingTarget` ThreadLocal
  2) 서비스: RowScope 적용 데이터 조회
  3) 출력 서비스: `masker = MaskingFunctions.masker(policyMatch)` 준비
  4) Writer: 각 필드/셀/텍스트에 `masker.apply(...)` 또는 `MaskingService.render(...)`
  5) 감사: `AuditEvent(DOWNLOAD_<FORMAT>)` 기록(파일명·행수·reason/legalBasis·rowScope/maskRule 포함)
- 체크리스트
  - 포맷별 Writer에 공통 헬퍼 삽입
  - forceUnmask 역할·사유 검증
  - 대량/스트리밍 성능 검증(SXSSF 등)
  - 다운로드/내보내기 AuditEvent 기록
  - e2e 스모크: 마스킹 적용 여부 확인(Playwright 등)
  - **사용자 요청 기반 마스킹 정책**: 화면/사용자 입력으로 선택된 마스킹 해제·강도 설정도 문서 다운로드(Excel/PDF/Word/XML/CSV 등) 시 동일하게 적용. UI에서 선택된 정책값을 요청 컨텍스트에 태우고, Writer 단계에서 `MaskingTarget.forceUnmaskFields/kinds` 반영.
  - **모든 마스킹 정책 일관 적용**: 서비스/DTO 레이어에서 적용한 마스킹 규칙(민감필드, maskRule, maskParams)과 forceUnmask 여부를 문서 변환(모든 포맷)에도 동일하게 전달·적용해 서버/문서 출력 간 정책 불일치가 없도록 한다.

### 운영 베스트 프랙티스 가이드 (감사 로그 조회 + 보존/파티션)
- **조회 자체를 감사**: 모든 `audit_log` 조회 API/쿼리 결과에 대해 `AUDIT_ACCESS` 이벤트 발행 (actor, 검색조건, 결과 건수, 페이지 번호 포함), ASYNC_FALLBACK 로깅.
- **최소 권한 원칙**: 전용 역할(AUDIT_VIEWER)만 조회 가능, 결과 다운로드/Export는 별도 권한 + 사유 필수.
- **파티션 설계**: 월 단위 파티션(`audit_log_yyyy_mm`), 3/5년 보존 정책을 파티션 DROP/ARCHIVE 배치로 실행. 해시체인/WORM 보존본은 S3 Object Lock(Compliance mode) 또는 외부 WORM 스토리지에 병행 적재.
- **캐시/인덱스**: 조회 키(event_time, actor_id, subject_type/key) 인덱스 유지. 장기 파티션은 HOT(최근 3~6개월) / COLD(이후) 테이블스페이스 분리.
- **성능/비용**: COLD 파티션은 ZSTD 압축, auto-vacuum 튜닝. Export 시 필터링/마스킹된 데이터만 제공.
- **운영 리포트**: 월간 접속기록 점검 작업을 스케줄링(배치)하고 결과를 알림/대시보드로 전송. 이상 패턴(심야 다건 조회 등) 룰을 Prometheus Alert 또는 SIEM 룰로 등록.

**이 설계는 참고용이며, 실제 적용 전 반드시 최신 국내 법령과 감독당국·금융보안원 및 사내 컴플라이언스 규정을 통해 최종 검증해야 한다.**
