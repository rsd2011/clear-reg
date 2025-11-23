# 중앙 감사(Audit) 모듈 설계 초안

## 1) 요구사항·법적 전제 정리
- 대상: Java 21 / Spring Boot 3.3.x 멀티모듈(backend/platform, auth, data-integration, policy, server, batch).
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
- 의존 방향: 상위 모듈(server, batch, auth, data-integration)이 audit 포트를 의존, audit은 platform 공용 타입 최소 참조.

## 3) 기존 멀티모듈 분석 및 마이그레이션 전략
- 예상 책임 위치
  - `auth`: 로그인/세션/비밀번호 변경 로그, RowScope 활용.
  - `server`: MVC 필터/인터셉터 기반 접속기록.
  - `data-integration`: 배치·대량 조회 로깅, HR 데이터 접근.
  - `policy`: 권한·정책 변경 이력.
  - `platform`: 공통 보안 유틸(RowScope 등) → AuditContext에서 재사용.
- 단계적 플랜
  1) `audit-core` 도입, 공통 `AuditEvent`/`AuditPolicySnapshot` 정의(레거시 로그 유지).
  2) Dual-write: 새 포트 호출 + 기존 로깅 병행, 정책 캐시 적용.
  3) 모듈별 전환: auth → server → data-integration → batch, 직접 DB insert 제거.
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
- server 전역 파일럿: **HandlerInterceptor 기반 레거시를 제거**하고 `HttpAuditAspect`로 컨트롤러 전역 HTTP 감사 처리(`/api/**`), `AuditPort`로 기록. 기존 필터(`SensitiveApiFilter`, `DataPolicyMaskingFilter`)는 AOP 전/후로 동작.
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
- [x] (P2) `auth` 로그인/비밀번호 변경/권한 감사 → AuditPort 전환(dual-write 레거시 제거)
  - [x] (P2) `data-integration` 배치/대량 조회 로깅 → AuditPort 사용, 직접 DB insert 제거 **(배치 목록/최신 + 조직/직원 조회 + outbox enqueue/claim/sent/retry/dead-letter AuditPort 전환 완료, ExportAuditService/OutputMaskingAdapter 멀티포맷 헬퍼·테스트 완료, HR 직원 CSV + 조직 Excel/PDF 엔드포인트 연동 완료, 향후 신규 대량 export 잡은 동일 패턴으로 추가만 남음)**  
  - [x] data-integration 내 export 엔드포인트에 `ExportService/OutputMaskingAdapter` 실연동 (server `HrEmployeeExportController` CSV 예시)  
  - [x] export 결과 직렬화 단계에서 `OutputMaskingAdapter` 적용(Excel/CSV/PDF/JSON 공통) → `ExportMaskingHelper`, `ExcelMaskingAdapter`, `PdfMaskingAdapter` + 멀티포맷 무누출 테스트 추가  
  - [x] ExportCommand에 reason/legalBasis 전달 · Audit 메타 기록 · API 파라미터 검증(사유/법적근거) 연결  
  - [x] export 실패 경로에서도 AuditMode=STRICT/ASYNC_FALLBACK 정책에 맞게 결과 코드/에러를 기록하고 DLQ 재처리 여부 점검.  
    - ExportFailureEvent/Notifier 추가로 실패 메타(파일명/건수/결과코드) 기록 및 알림 훅 제공(현재 NOOP → 알림 시스템 연동 시 구현).  
    - AuditEvent.resultCode에 예외 클래스명 저장, AuditMode 전달(STRICT/ASYNC_FALLBACK) 유지.  
- [x] (P2) `policy` 변경 이력 → AuditEvent(policy-change)로 남기기
- [ ] (P3) 불필요한 기존 로그 테이블/코드 제거 및 문서 업데이트 — `dw_*_log` 레거시 테이블 삭제 계획 수립, 마이그레이션 스크립트 작성 필요

### 테스트
- [x] Policy 미정의 시 기본 ON/사유 필수 동작 스모크(필터 + 문서/Playwright)
- [x] Strict/forceUnmask → UnmaskAudit 적재 e2e 테스트
- [~] (P4) Kafka DLQ/재처리 시나리오 검증(카프카 옵셔널) — 브로커 준비 시 env `AUDIT_KAFKA_BOOTSTRAP` 기반 스모크(AuditKafkaSmokeTest) 실행, DLQ 재처리 리스너(AuditDlqReprocessor) 추가 완료(프로퍼티로 토글), 재처리 알람/지표는 브로커 마련 후 확장
  - [x] (P2) 마스킹/summary에 원문 포함 여부 커버리지 확대 — OutputMaskingAdapter 경로별 샘플 테스트(Excel/CSV/PDF/JSON) 추가 완료

### 운영
- [x] (P2) 보존기간별 파티션/아카이브 배치 스케줄링 — 월 단위 파티션 사전 생성 스케줄러(`AuditPartitionScheduler`) 구현 및 테스트 완료. **후속 세부 작업**  
  - [~] HOT/COLD 테이블스페이스 분리 적용  
    - 프로퍼티 추가 완료: `audit.partition.tablespace.hot`, `audit.partition.tablespace.cold`, `audit.partition.hot-months`, `audit.partition.cold-months` (기본 hot/cold 미지정 시 동일 TS).  
    - SQL 예시: `ALTER TABLE audit_log ATTACH PARTITION audit_log_2025_05 FOR VALUES FROM ('2025-05-01') TO ('2025-06-01') TABLESPACE :hot;`  
      7개월 경과 시 `ALTER TABLE audit_log_2024_10 SET TABLESPACE :cold; REINDEX TABLE audit_log_2024_10; ALTER TABLE ... SET (toast.compress='zstd'); VACUUM ANALYZE audit_log_2024_10;`  
    - 운영 주기: HOT→COLD 이동 월 1회, 장기 파티션 VACUUM/REINDEX 주 1회.  
  - [x] S3 Object Lock/Glacier 배치 스크립트 예시 추가(`docs/audit/hot-cold-archive-example.sh`): 파티션별 dump → S3 Object Lock(5년) 업로드 → 체크섬 검증 후 DROP, Glacier 이동 옵션 포함.  
    - [x] 배치 런처에서 스크립트 호출 파라미터화(`PG_URL`, `S3_BUCKET`, `S3_PREFIX`, `RETENTION_YEARS`, `MAX_RETRY`, `SLACK_WEBHOOK`) 및 실패 리트라이/알림 연동.  
  - [~] AuditPartitionScheduler 정책 연동·배치 통합  
    - [x] PolicyToggleSettings/Policy YAML에 `auditPartitionEnabled`, `auditPartitionCron`, `auditPartitionPreloadMonths`, `auditPartitionTablespaceHot/Cold`, `auditPartitionHotMonths/ColdMonths` 필드 저장 (UI 노출은 추후).  
    - [ ] 정책 변경 이벤트 수신 시 스케줄러 Cron/enable/preloadMonths 동기화 e2e 검증(Policy API 호출 포함)  
- [x] (P2) 월간 접속기록 점검 리포트 — `AuditMonthlyReportJob`을 정책 기반 동적 cron으로 전환 (`auditMonthlyReportEnabled`, `auditMonthlyReportCron`), 기본 cron=0 0 4 1 * *  

### 정리/마이그레이션
  - [x] (P3) 레거시 `dw_*_log` 테이블 정리: 사용 중지 경로 파악 → `docs/migrations/2025-11-23-remove-dw-log-tables.sql` 추가 → 배포 전 백업 및 드라이런 계획 수립  

### 운영 설계 보강 (HOT/COLD + Object Lock)
  - [ ] HOT/COLD 분리 실행 플랜: 최근 6개월 파티션은 `audit_hot` 테이블스페이스 + ZSTD 압축 OFF, 이후 파티션은 `audit_cold` + ZSTD 압축 ON, 주 1회 VACUUM/REINDEX 스케줄.  
  - [x] S3 Object Lock/Glacier 배치 스크립트 예시 추가(`docs/audit/hot-cold-archive-example.sh`): 파티션별 dump → S3 Object Lock(5년) 업로드 → 체크섬 검증 후 DROP, Glacier 이동 옵션 포함.  
  - [ ] 모니터링/알림 룰: HOT IOPS, COLD 비용, export 성공/실패, Object Lock 지연 알림.
    - HOT IOPS: pg_stat_io 기반 HOT TS read/write IOPS 95퍼센타일이 평시 대비 50%↑ 시 Slack 경고.
    - COLD 비용: 월별 table_size×단가 추정 비용 증가율>20% 시 경고.
    - Object Lock 지연: 업로드/검증/삭제 elapsed_ms가 `audit.archive.alert.delay-threshold-ms` 초과 시 경고, 3회 연속 시 PagerDuty 알림.
    - 파티션 이동 실패: `ATTACH/DETACH/SET TABLESPACE` 실패 시 즉시 Slack/Webhook 알림(파티션명, 오류코드, 재시도횟수) + 지수백오프 3회 재시도.
    - 실패 시 rollback: drop 전에 checksum 검증(md5/sha256) 및 S3 ObjectLock 헤더 확인.
    - 지표 수집/알림 파이프라인 제안: Prometheus exporter로 HOT/COLD IOPS·압축비·ObjectLock 지연(ms)·S3 PUT/Glacier 비용 메트릭 노출 → Alertmanager 룰 적용.
    - Alertmanager 샘플 룰 추가: `docs/monitoring/audit-alerts.yml` (archive 실패/지연, HOT IOPS 스파이크, COLD 비용 상승, Object Lock 지연).
  - [ ] 운영 파라미터화: 보존일수/파티션 프리로드 개월수/env 기반 DataSource 분리 설정 추가.  
    - `audit.partition.preload-months=2`, `audit.partition.tablespace.hot/cold`, `audit.partition.hot-months=6`, `audit.partition.cold-months=60`, `audit.archive.command=/path/to/hot-cold-archive-example.sh`, `audit.archive.retry=3` 프로퍼티를 `AuditPartitionScheduler`/`AuditColdArchiveScheduler`/`AuditArchiveJob`에 주입. (스크립트 샘플은 `docs/audit/hot-cold-archive-example.sh` 참고)  
  - [ ] 정책 이벤트 연동: `PolicyChangedEvent(security.policy)` 발생 시 AuditPartitionScheduler/AuditColdArchiveScheduler가 즉시 새 설정을 반영하고, 다음 cron에서 HOT/COLD 생성·아카이브를 재계산하도록 EventListener 추가(e2e 검증 필요).  
    - application.yml 예시  
      ```yaml
      audit:
        partition:
          preload-months: 2        # 미래 파티션 미리 생성
          tablespace:
            hot: audit_hot
            cold: audit_cold
        retention:
          hot-months: 6            # HOT 보존
          cold-months: 60          # COLD 보존
        archive:
          s3:
            bucket: audit-archive
            prefix: audit-log/
            object-lock-years: 5
          enabled: true
      ```
  - [ ] 운영 점검: 파티션 생성/아카이브 실패 알림(Slack/Webhook) 및 리트라이 정책 정의.  
    - Retry: 최대 3회 지수백오프(5s→30s→2m), 알림에는 파티션명·에러코드 포함.  
    - 알림 채널: Slack Webhook 예) `audit.alert.webhook`, 장애 시 이메일 백업.
    - 배치 스크립트 예시(개요)
      ```bash
      part=$1 # e.g. 2024_10
      pg_dump "$PG_URL" --table="audit_log_${part}" -Fc -f "/tmp/audit_${part}.dump"
      aws s3 cp "/tmp/audit_${part}.dump" "s3://$S3_BUCKET/$S3_PREFIX/audit_${part}.dump" \
        --object-lock-mode COMPLIANCE \
        --object-lock-retain-until-date "$(date -d '+5 years' -Ins)"
      # checksum 확인 후 DROP
      psql "$PG_URL" -c "DROP TABLE IF EXISTS audit_log_${part};"
      ```
- [~] (P2) 월간 접속기록 점검 리포트 및 알림 대시보드 연동 — `AuditMonthlyReportJob` 스켈레톤으로 월 1회 집계 로그 추가, 향후 기간별 count/리포트 export/SIEM 연계로 확장 (Grafana/Loki 혹은 SIEM 쿼리 템플릿 정의 예정)  
  - [x] 월간 배치 Cron 설정(yaml/properties) 예시 추가: `0 0 3 1 * *` (매월 1일 03시).  
  - [x] 집계 지표 정의: 총 접속 수, 실패 비율, 심야(00-06시) 조회 건수, DRM/다운로드 시도, unmask 요청 건수.  
  - [x] 결과 저장 스키마: `audit_monthly_summary` (year_month PK, total_count, created_at) — 엔티티/리포지토리/배치 저장 구현 완료.  
  - [~] 알림 훅: Slack/Webhook 또는 이메일로 top-3 이상 징후 전송(채널·템플릿 설계 필요).  
  - [~] 대시보드 연계: Grafana/Loki 또는 SIEM 쿼리 템플릿을 별도 MD로 제공하고 링크 첨부(`docs/monitoring/audit-monthly-report-grafana.md`).  
  - [x] e2e 스모크: H2/pg 테스트에서 지난달 샘플 데이터 삽입 후 배치 실행 → summary row 생성 검증.
- [x] (P2) 감사 로그 조회 권한 최소화, 조회 행위 자체 감사 기록 자동화 (AuditLogAccessAspect, allowed-roles)
- [~] (P3) SIEM/외부 보안시스템 연동 및 전송 암호화 확인 — TLS/서명 채널, 전송 필드 마스킹 매핑 표 작성  
  - [x] 전송 필드 매핑·샘플 페이로드 문서화(`docs/siem/audit-siem-payload.md`)  
  - [ ] 수집/전송 파이프라인(OTLP/syslog) 구현 및 암호화·서명 적용

### SIEM/외부 연동 필드 매핑(초안)
- 전송 방식: TLS + 서명(Optional) syslog/OTLP/JSON. 개인정보/신용정보 필드는 마스킹/토큰화 후 전송.
- 필드 매핑 예시 (→ SIEM)
  - event_time → @timestamp
  - event_type/module/action → labels.event_type/module/action
  - actor_id/actor_type/actor_role/actor_dept → subject.user.id/type/role/dept
  - subject_type/subject_key → resource.type/id (마스킹된 키 사용)
  - client_ip/user_agent/device_id/channel → source.ip/ua/device/channel
  - reason_code/reason_text/legal_basis_code → audit.reason.code/text/legal_basis
  - risk_level → labels.risk_level
  - before_summary/after_summary → fields.before/after (민감값 마스킹)
  - hash_chain → integrity.hash_chain

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
- [ ] 포맷별 Writer에 공통 헬퍼 삽입
- [ ] forceUnmask 역할·사유 검증
- [ ] 대량/스트리밍 성능 검증(SXSSF 등)
- [ ] 다운로드/내보내기 AuditEvent 기록
- [ ] e2e 스모크: 마스킹 적용 여부 확인(Playwright 등)
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
