# Alertmanager CI 배포/스모크 파이프라인 제안

## 단계
1) promtool 룰 검증  
   ```bash
   promtool check rules docs/monitoring/audit-alerts.yml
   ```
2) ConfigMap 생성/패치  
   ```bash
   kubectl -n monitoring create configmap audit-alerts --from-file=docs/monitoring/audit-alerts.yml --dry-run=client -o yaml | kubectl apply -f -
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

## 주의
- CI 클러스터 접근 권한 필요(kubeconfig/SA).
- PGW_URL/Alertmanager endpoint는 환경변수로 주입.
- 실패 시 CI 실패 처리로 alert 룰 회귀 방지.
