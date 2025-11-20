# DW Worker 배포 가이드 (컨테이너 + HPA)

## 1) 이미지 빌드/푸시
- 사전: `./gradlew :backend:dw-worker:bootJar`
- Docker 빌드: `docker build -t <registry>/dw-worker:<tag> backend/dw-worker`
- 푸시: `docker push <registry>/dw-worker:<tag>`

## 2) 런타임 환경 변수
- 공통
  - `SPRING_PROFILES_ACTIVE=worker`
  - `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
  - `DW_INGESTION_OUTBOX_PUBLISHER_TYPE=kafka` (브로커 사용 시), `dw.ingestion.kafka.enabled=true`
  - `DW_INGESTION_OUTBOX_PUBLISHER_KAFKA_TOPIC=dw-ingestion-jobs`
  - `DW_INGESTION_KAFKA_GROUP_ID=dw-worker`
  - `DW_INGESTION_WORKER_EXECUTOR_CORE_POOL` / `MAX_POOL` / `QUEUE_CAPACITY` (스레드 풀 튜닝)
- 관측성
  - `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus`
  - `MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true`

## 3) Kubernetes 배포 예시 (Deployment + Service + HPA)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dw-worker
spec:
  replicas: 1
  selector:
    matchLabels: { app: dw-worker }
  template:
    metadata:
      labels: { app: dw-worker }
    spec:
      containers:
        - name: dw-worker
          image: <registry>/dw-worker:<tag>
          imagePullPolicy: IfNotPresent
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "worker"
            - name: SPRING_DATASOURCE_URL
              valueFrom: { secretKeyRef: { name: dw-worker-db, key: url } }
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom: { secretKeyRef: { name: dw-worker-db, key: username } }
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom: { secretKeyRef: { name: dw-worker-db, key: password } }
            - name: DW_INGESTION_OUTBOX_PUBLISHER_TYPE
              value: "kafka"
            - name: DW_INGESTION_OUTBOX_PUBLISHER_KAFKA_TOPIC
              value: "dw-ingestion-jobs"
            - name: DW_INGESTION_KAFKA_ENABLED
              value: "true"
            - name: DW_INGESTION_KAFKA_GROUP_ID
              value: "dw-worker"
          ports:
            - containerPort: 8080
              name: http
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            initialDelaySeconds: 30
            periodSeconds: 30
          resources:
            requests: { cpu: "200m", memory: "512Mi" }
            limits: { cpu: "1", memory: "1Gi" }
---
apiVersion: v1
kind: Service
metadata:
  name: dw-worker
spec:
  selector: { app: dw-worker }
  ports:
    - name: http
      port: 80
      targetPort: 8080
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: dw-worker
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: dw-worker
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

## 4) 알람/모니터링 체크리스트
- Prometheus 스크랩 대상에 `dw-worker` 추가, 주요 지표: `dw_ingestion_outbox_pending`, `dw_ingestion_queue_running_jobs`, Kafka consumer lag.
- Alert 예시: pending outbox 증가, dead-letter 발생 증가, Kafka lag 지속, Pod 재시작 횟수 증가.
- Runbook: `docs/runbooks/dw-worker.md` (필요 시 추가) 참고하도록 링크 예정.

## 5) CI/CD 파이프라인 힌트
- 빌드 스텝: `./gradlew :backend:dw-worker:bootJar` → `docker build` → `docker push`.
- 배포 스텝: `kubectl apply -f k8s/dw-worker.yaml` 또는 Helm chart로 관리.
- 태그 전략: 브랜치/PR는 `-rc` 태그, main 릴리스는 `vX.Y.Z` 태그 + 이미지 고정.
