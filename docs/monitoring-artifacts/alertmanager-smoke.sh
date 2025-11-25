#!/usr/bin/env bash
# 간단한 Alertmanager 룰 배포/검증 스크립트 (로컬/CI 스모크용)
set -euo pipefail

RULE_FILE="docs/monitoring-artifacts/audit-alerts.yml"

echo "[1/3] promtool rule check"
if ! command -v promtool >/dev/null 2>&1; then
  echo "promtool not found; install Prometheus toolchain" >&2
  exit 1
fi
promtool check rules "${RULE_FILE}"

echo "[2/3] push test metric to Prometheus Pushgateway (needs PGW_URL env)"
if [ -n "${PGW_URL:-}" ]; then
  echo "audit_archive_failure_total 1" | curl -s -XPOST "${PGW_URL}/metrics/job/audit_smoke" --data-binary @-
else
  echo "PGW_URL not set; skip pushgateway smoke."
fi

echo "[3/3] remind: patch Alertmanager configmap with audit-alerts.yml"
echo "kubectl -n monitoring create configmap audit-alerts --from-file=${RULE_FILE} --dry-run=client -o yaml | kubectl apply -f -"
echo "kubectl -n monitoring patch alertmanager main --type merge -p '{\"spec\":{\"configMap\":\"audit-alerts\"}}'"
