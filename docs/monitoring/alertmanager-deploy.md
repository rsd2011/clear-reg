# Alertmanager 룰 배포 스모크 가이드

## 파일 위치
- `docs/monitoring/audit-alerts.yml`

## 로컬 검증
```bash
promtool check rules docs/monitoring/audit-alerts.yml
```

## k8s 배포 예시
```bash
kubectl -n monitoring create configmap audit-alerts --from-file=docs/monitoring/audit-alerts.yml --dry-run=client -o yaml | kubectl apply -f -
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
