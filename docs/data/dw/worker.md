# DW Worker Guide (Consolidated)

## From: dw-worker-plan.md

# DW Worker 분리/확장 계획

## 1. 목표
- 실시간 API(`dw-gateway`)와 배치/큐 기반 작업(`dw-worker`)을 분리해 스케일링 정책과 장애 영향을 격리한다.
- `DwIngestionJobQueue` + Outbox를 통해 idempotent하게 작업을 전달하고, 향후 SQS/Kafka 등 외부 큐로 전환하기 쉬운 구조를 마련한다.

## 2. 아키텍처 개요
```
[backend/server] --(ports)--> [dw-gateway] --(enqueue)--> DwIngestionJobQueue (DB/Outbox/Redis)
                                                           |
                                                           v
                                                   [dw-worker]
                                                        |
                                                        v
                                             DW staging/storage + audit
```
- `dw-gateway`: REST, 정책/상태 API, 업로드 핸들러, Outbox 기록.
- `dw-worker`: Spring Batch + Quartz(or Spring Scheduling). 큐 레코드를 polling/stream 하여 DW 동기화 실행.
- 공통 모듈: `backend/file-core`, `backend/dw-integration` 재사용.

## 3. dw-worker 모듈 설계
1. **프로젝트 구조**
   - 새 Gradle 모듈 `backend/dw-worker` (Spring Boot CLI).
   - 의존성: `dw-integration`, `file-core`, `platform`, `auth`(AuthContext util) 등.
2. **큐/Outbox 처리**
   - DB Outbox 테이블(`dw_ingestion_outbox`): 상태 `PENDING`, `SENT`, `FAILED`.
   - `DwIngestionJobQueue` 인터페이스 구현: `enqueue(DwIngestionCommand)`는 Outbox insert.
   - `DwIngestionOutboxRelay` (gateway): 트랜잭션 커밋 후 DB→Queue 전달 (초기엔 같은 DB polling, 향후 SQS/Kafka).
3. **Worker 실행 흐름**
   - `@Scheduled` or Spring Batch Job이 Outbox(PENDING) 조회 → 상태 `PROCESSING` → 실제 `DwIngestionService` 실행 → 성공시 `COMPLETED` + 감사 로그, 실패 시 재시도 카운트 증가.
   - 재시도 정책: 5회 Exponential Backoff, 최대 1시간.
4. **구성/모니터링**
   - 별도 `application-worker.yml`: 큐 polling interval, batch size, 재시도 설정.
   - Prometheus 지표: `dw_worker_jobs_total{status=..}`, `dw_worker_lag_seconds`, `dw_worker_retries_total`.
5. **보안/권한**
   - Worker는 내부 서비스 계정만 사용, REST 인증 불필요. DB 접근은 최소 권한(RDS IAM Role)으로 제한.

## 4. 단계별 TODO
1. **Phase 1** (현 단계)
   - `docs/dw/dw-worker-plan.md` 작성 (본 문서).
   - `backend/file-core` + `dw-integration` 재사용 여부 검증.
2. **Phase 2**
   - Gradle 모듈 `backend/dw-worker` 생성, Boot strap class/Config 추가.
   - Outbox 테이블 + JPA 엔티티 정의, `DwIngestionJobQueue` 구현.
3. **Phase 3**
   - Worker Job (`DwWorkerJob`) 구현: 큐 polling → `DwIngestionService` 호출.
   - 상태/재시도/감사 로깅, Runbook 작성.
4. **Phase 4**
   - 인프라: ECS/K8s 배포 템플릿, AutoScaling 정책.
   - 큐 전환 PoC (SQS/Kafka) 및 Feature Flag.

## 5. 리스크 및 대응
- **큐 중복 처리**: Outbox + unique `job_id` + idempotency token으로 보장.
- **장애 감지**: CloudWatch/Grafana alert (lag, failure rate). Runbook에 수동 재처리 절차 기재.
- **배포 복잡도**: GitOps로 worker chart 분리, Blue/Green 롤아웃 적용.

## 6. 산출물/문서
- `docs/dw/dw-worker-plan.md` (본 문서)
- 추후: `backend/dw-worker/README.md`, `docs/runbooks/dw-worker.md`
- Architecture TODO E-1 항목에 본 문서 링크 추가 예정.
\n---\n
## From: dw-worker-deploy.md

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
