# Audit 로그 HOT/COLD 분리 및 Object Lock/Glacier 전송 실행안 (초안)

## 1. 파티션/HOT·COLD 테이블스페이스 이동
```sql
-- 월 단위 파티션 사전 생성 (매월 1일 기준)
CREATE TABLE IF NOT EXISTS audit_log_2025_12 PARTITION OF audit_log
  FOR VALUES FROM ('2025-12-01') TO ('2026-01-01')
  TABLESPACE audit_hot;

-- 7개월 경과 파티션 COLD로 이동 + 압축
ALTER TABLE audit_log_2025_05 SET TABLESPACE audit_cold;
ALTER TABLE audit_log_2025_05 SET (toast.compress = 'zstd');
REINDEX TABLE audit_log_2025_05;
VACUUM (ANALYZE) audit_log_2025_05;
```

## 2. Object Lock(Compliance) + Glacier 딥아카이브
```bash
PART=2025_05
aws s3 cp s3://audit-hot/audit_${PART}.parquet s3://audit-archive/${PART}/ --metadata object-lock-mode=COMPLIANCE --metadata-directive REPLACE --object-lock-retain-until-date "$(date -d '+5 years' --iso-8601=seconds)"
# (선택) Glacier Deep Archive 이동
aws s3 cp s3://audit-archive/${PART}/audit_${PART}.parquet s3://audit-glacier/${PART}/ --storage-class DEEP_ARCHIVE
```
- 사전 조건: S3 버킷 Object Lock(Compliance) 활성화, IAM 권한 제한.
- 실패 시 재시도 기록을 AuditEvent로 적재.

## 3. 배치 스케줄링 제안
- 월 1회: `AuditPartitionScheduler`로 신규 파티션 생성 (이미 구현됨).
- 월 1회: 파티션 7개월차 → COLD 이동 + 압축 + REINDEX.
- 분기 1회: COLD 파티션 → S3 Object Lock export + 선택적 Glacier 이동.
- 모니터링: export 성공/실패 카운터, S3 전송 지연, Object Lock 만료까지 남은 기간, COLD 스토리지 비용.

## 4. 롤백/DR 계획
- export 전 Parquet/CSV를 로컬 스냅샷 보관(최소 30일).
- Object Lock 설정 후 삭제 불가이므로, 테스트 버킷에서 드라이런 필수.
- 배치 실패 시 DLQ 테이블(`audit_export_dlq`)에 적재 후 재시도.
```
CREATE TABLE IF NOT EXISTS audit_export_dlq (
  id bigserial primary key,
  partition_key text not null,
  failed_at timestamptz not null default now(),
  error text,
  payload jsonb
);
```
