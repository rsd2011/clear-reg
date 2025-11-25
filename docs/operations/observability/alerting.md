# Alerting & Alertmanager Guide (Consolidated)

## From: monitoring/alertmanager-ci.md

# Alertmanager CI 배포/스모크 파이프라인 제안

## 단계
1) promtool 룰 검증  
   ```bash
   promtool check rules docs/monitoring-artifacts/audit-alerts.yml
   ```
2) ConfigMap 생성/패치  
   ```bash
   kubectl -n monitoring create configmap audit-alerts --from-file=docs/monitoring-artifacts/audit-alerts.yml --dry-run=client -o yaml | kubectl apply -f -
   kubectl -n monitoring patch alertmanager main --type merge -p '{"spec":{"configMap":"audit-alerts"}}'
   ```
3) 테스트 메트릭 주입 (Pushgateway)  
   ```bash
   echo 'audit_archive_failure_total 1' | curl -s -XPOST $PGW_URL/metrics/job/audit_smoke --data-binary @-
   ```
4) Alert firing 확인 스크립트 (Alertmanager API)  
   ```bash
   curl -s http://alertmanager:9093/api/v2/alerts | jq '.[] | select(.labels.alertname=="AuditArchiveFailure")'
   ```
5) Slack/PD 수신기까지 전달 확인(옵션): webhook mock 또는 blackhole receiver로 CI에서 수신 여부 점검.

## GitHub Actions 예시
- `.github/workflows/alertmanager-smoke.yml` 참조:  
  - promtool 체크 → ConfigMap 패치 → Pushgateway 테스트 메트릭 주입 → Alertmanager API로 firing 확인.  
  - 필요한 시크릿: `KUBE_CONFIG_DATA`, `PGW_URL`, `ALERTMANAGER_URL`.

## 주의
- CI 클러스터 접근 권한 필요(kubeconfig/SA).
- PGW_URL/Alertmanager endpoint는 환경변수로 주입.
- 실패 시 CI 실패 처리로 alert 룰 회귀 방지.
\n---\n
## From: monitoring/alertmanager-deploy.md

# Alertmanager 룰 배포 스모크 가이드

## 파일 위치
- `docs/monitoring-artifacts/audit-alerts.yml`

## 로컬 검증
```bash
promtool check rules docs/monitoring-artifacts/audit-alerts.yml
```

## k8s 배포 예시
```bash
kubectl -n monitoring create configmap audit-alerts --from-file=docs/monitoring-artifacts/audit-alerts.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl -n monitoring patch alertmanager main --type merge -p '{
  "spec": { "configMap": "audit-alerts" }
}'
```

## 스모크
1. Prometheus에 테스트 메트릭 주입  
   ```bash
   echo 'audit_archive_failure_total 1' | curl -s -XPOST http://prometheus:9091/metrics/job/audit_smoke
   ```
2. Alertmanager UI에서 `AuditArchiveFailure` firing 확인.

## 파이프라인 메모
- CI에서 promtool 체크 → 배포 파이프라인에서 ConfigMap 생성/롤아웃.
- Slack/PagerDuty 수신기 설정은 Alertmanager global 설정에 위임.
