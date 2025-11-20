# Rollback Log

형식: `YYYY-MM-DD | migration file | env | rollback 방법 | 결과`

- 기록 예시)
  - 2025-11-20 | 2025-11-19-dw-outbox-retry.sql | staging | psql -f ..._rollback.sql | SUCCESS
