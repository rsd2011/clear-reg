#!/usr/bin/env bash
set -euo pipefail

DB_URL="${DB_URL:-postgres://postgres:postgres@localhost:5432/postgres}"
MIGRATION_DIR="${MIGRATION_DIR:-docs/migrations}"

echo "Using DB_URL=${DB_URL}"
echo "Verifying migrations under ${MIGRATION_DIR}"

shopt -s nullglob
migrations=(${MIGRATION_DIR}/*.sql)
shopt -u nullglob

if [ ${#migrations[@]} -eq 0 ]; then
  echo "No migration files found."
  exit 0
fi

for file in "${migrations[@]}"; do
  echo "Applying migration: ${file}"
  psql "${DB_URL}" -v ON_ERROR_STOP=1 -1 -f "${file}"

  echo "Rollback simulation (transactional apply + rollback): ${file}"
  psql "${DB_URL}" -v ON_ERROR_STOP=1 <<SQL
BEGIN;
\\i ${file}
ROLLBACK;
SQL
done

echo "Migration verification completed."
