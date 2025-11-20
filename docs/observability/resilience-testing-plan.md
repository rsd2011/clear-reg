# Resilience Testing & Zero-downtime Deployment Plan

## 1. 목표
- 주요 모듈(API, DW ingestion, batch)이 장애나 배포 중에도 서비스 연속성을 유지하도록, chaos 실험과 롤링 배포 도구를 도입한다.
- Argo Rollouts 또는 Spinnaker 기반으로 블루-그린/카나리 배포를 자동화하고, 실패 시 즉시 롤백할 수 있는 체계를 구축한다.

## 2. 전략 개요
1. **Chaos Experiments**
   - 대상: Redis, Kafka, Postgres, 서버 인스턴스, DW 배치 작업.
   - 도구: LitmusChaos (Kubernetes) 또는 AWS Fault Injection Simulator.
   - 시나리오: 네트워크 지연, Pod 강제 종료, 읽기 Replica Failover, Outbox Relay 지연.
2. **Zero-downtime Deployments**
   - Argo Rollouts with canary steps (traffic split, metric checks) or Spinnaker pipeline.
   - Feature flag / config toggle 로 backward compatibility 확인.
3. **Observability Gates**
   - 배포 중 SLO (error rate, latency) 감시, 실패 시 자동 중단.

## 3. 단계별 실행
1. **Preparation (0-3M)**
   - Kubernetes manifest/Helm chart 를 Argo Rollouts compatible template 로 전환.
   - Chaos namespace 구축, 기본 실험 템플릿 작성.
   - Runbook: chaos 실험 절차 및 안전가드 정의.
2. **Pilot (3-6M)**
   - 서버 API 에 대해 카나리 배포 → 10% traffic 전환 → metrics 검증 후 100%.
   - Redis/Kafka chaos 테스트 주 1회 스케줄링.
3. **Scale-out (6-12M)**
   - DW ingestion/batch-app 배포 플로우도 Argo Rollouts/Spinnaker 로 통합.
   - Chaos pipeline 을 GitOps와 연결하여 PR마다 시뮬레이션.

## 4. 기술 선택
- GitOps: Argo CD + Rollouts (K8s). Spinnaker 는 멀티 클라우드 필요 시 선택.
- Chaos: LitmusChaos + CRD. AWS 환경은 FIS 연동.
- Monitoring: Prometheus + Alertmanager; Rollouts analysis templates referencing PromQL.

## 5. 리스크 및 대응
- **실험 중 실제 고객 영향** → Non-prod → Staging → Prod 순서, blast radius 제한.
- **배포 파이프라인 복잡도 증가** → 템플릿화, Runbook 제공.
- **Argo Rollouts 학습 비용** → 워크샵/가이드 문서 제공.

## 6. TODO
- [ ] Helm/manifest 를 Rollouts compatible 구조로 리팩터링.
- [ ] Chaos 템플릿 (Redis down, Kafka partition, Postgres failover) 작성.
- [ ] Argo Rollouts/Spinnaker pipeline PoC → metrics 기반 canary 구성.
- [ ] Rollouts 실패시 자동 롤백/알람 연동.
- [ ] Runbook (`docs/runbooks/resilience-testing.md`) 작성.
