#!/usr/bin/env bash
# 예시: 월별 파티션을 S3 Object Lock으로 아카이브 후 삭제
# 사용 전에 PG_URL, S3_BUCKET, S3_PREFIX 환경변수를 설정하세요.
# (배치 런처에서 audit.archive.command 실행 시 환경 그대로 전달됨. SLACK_WEBHOOK이 설정되어 있으면 실패 알림 전송.)
# - PG_URL 예: "postgresql://user:pass@db:5432/audit"
# - Object Lock: COMPLIANCE 모드, 기본 5년 보존 (RETENTION_YEARS로 조정)
# - 실패 시 리트라이(MAX_RETRY, 기본 3회); 실 운영 적용 전 드라이런 필수

set -euo pipefail

PART="${1:-}"
if [[ -z "$PART" ]]; then
  echo "Usage: $0 yyyy_MM (e.g. 2024_10)" >&2
  exit 1
fi

TABLE="audit_log_${PART}"
TMP="/tmp/audit_${PART}.dump"
RETENTION_YEARS="${RETENTION_YEARS:-5}"
MAX_RETRY="${MAX_RETRY:-3}"
SLACK_WEBHOOK="${SLACK_WEBHOOK:-}"

echo "[1/4] Dump ${TABLE}"
pg_dump "$PG_URL" --table="$TABLE" -Fc -f "$TMP"

echo "[2/4] Upload with Object Lock"
aws s3 cp "$TMP" "s3://${S3_BUCKET}/${S3_PREFIX}${TABLE}.dump" \
  --object-lock-mode COMPLIANCE \
  --object-lock-retain-until-date "$(date -d "+${RETENTION_YEARS} years" -Ins)" \
  --metadata "sha256=$(sha256sum "$TMP" | cut -d' ' -f1)"

echo "[3/4] Verify checksum"
LOCAL_SUM="$(sha256sum "$TMP" | cut -d' ' -f1)"
REMOTE_SUM="$(aws s3api head-object --bucket "$S3_BUCKET" --key "${S3_PREFIX}${TABLE}.dump" --query 'Metadata.sha256' --output text || true)"
if [[ -n "$REMOTE_SUM" && "$REMOTE_SUM" != "$LOCAL_SUM" ]]; then
  echo "Checksum mismatch: local=$LOCAL_SUM remote=$REMOTE_SUM" >&2
  exit 1
fi

echo "[4/4] Drop partition"
for attempt in $(seq 1 "$MAX_RETRY"); do
  if psql "$PG_URL" -c "DROP TABLE IF EXISTS ${TABLE};"; then
    rm -f "$TMP"
    break
  fi
  echo "DROP failed (attempt $attempt/$MAX_RETRY). Retrying..."
  sleep $((attempt * 5))
done

if [[ -n "$SLACK_WEBHOOK" ]]; then
  curl -s -X POST -H 'Content-type: application/json' --data \
    "{\"text\":\"[audit-archive] partition ${TABLE} archived with Object Lock ${RETENTION_YEARS}y\"}" \
    "$SLACK_WEBHOOK" || true
fi

echo "Done. (Optional) Move to Glacier:"
echo "  aws s3 cp s3://${S3_BUCKET}/${S3_PREFIX}${TABLE}.dump s3://${S3_BUCKET}/${S3_PREFIX}${TABLE}.dump --storage-class DEEP_ARCHIVE"
