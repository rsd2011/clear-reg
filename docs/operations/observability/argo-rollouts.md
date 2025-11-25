# Argo Rollouts / Blue-Green PoC 계획

## 1) 목표
- API/dw-worker 배포 시 무중단/점진 롤아웃을 실험하고, 실패 자동 롤백 + 알림 경로를 확보한다.

## 2) 적용 대상
- 1차: `backend/server` (API)
- 2차: `backend/dw-worker` (워커)

## 3) Argo Rollouts 매니페스트 예시 (canary→blue-green 전환 가능)
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: server
spec:
  replicas: 3
  strategy:
    blueGreen:
      activeService: server-svc
      previewService: server-preview
      autoPromotionEnabled: false
      autoPromotionSeconds: 300
  selector:
    matchLabels: { app: server }
  template:
    metadata:
      labels: { app: server }
    spec:
      containers:
        - name: server
          image: <registry>/server:<tag>
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 30
            periodSeconds: 30
```

## 4) 자동 롤백 조건/알람
- Metric provider: Prometheus
  - 5xx rate > 2% for 2m
  - p95 latency > 800ms for 2m
- Rollout analysis template 예시
```yaml
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: server-rollout-analysis
spec:
  metrics:
    - name: error-rate
      interval: 60s
      count: 2
      failureLimit: 1
      provider:
        prometheus:
          address: http://prometheus.monitoring:9090
          query: sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) / sum(rate(http_server_requests_seconds_count[1m]))
    - name: latency-p95
      interval: 60s
      count: 2
      failureLimit: 1
      provider:
        prometheus:
          address: http://prometheus.monitoring:9090
          query: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```
- Alerting: Argo Rollouts notifications → Slack/Webhook (`onRollback`, `onPaused`, `onAnalysisRunFailed`).

## 5) 단계별 PoC 플랜
1. Dev 클러스터에 Argo Rollouts 컨트롤러 설치 (Helm)
2. `server` Deployment → Rollout 변환 (preview svc 추가)
3. Prometheus metric provider 설정 + 간단한 AnalysisTemplate 연결
4. Canary/Blue-Green 실험: 강제 실패를 주입해 자동 롤백 확인
5. Slack 알림 Hook 추가
6. dw-worker에도 동일 패턴 적용 (readiness/queue lag 기준)

## 6) 후속 작업
- Helm chart 값 템플릿화 (`charts/server/values-rollout.yaml` 등)
- GitOps 연계(Argo CD)로 롤아웃 리소스 관리
- Runbook 작성: 강제 롤백, 수동 프로모션, 분석 실패 시 대응

