# Bounded Context Decoupling Strategy

## 1. 목표
- 12개월 이후를 목표로 Auth, Policy, File, DW Query 컨텍스트를 독립 Spring Boot 서비스 혹은 Modulith slice 로 분리하여 팀별 배포/스케일링을 가능하게 한다.
- 공통 기반(`backend/platform`)을 최소화하고, 각 컨텍스트가 포트/어댑터를 통해 느슨하게 통신하도록 한다.

## 2. 현재 상태 요약
| 컨텍스트 | 모듈 | 주요 의존 | 현 이슈 |
| --- | --- | --- | --- |
| Auth | `backend/auth` | platform | Permission 엔티티가 server와 공유됨 |
| Policy | `backend/policy` | platform, auth | YAML 파서와 서비스 계층 결합 |
| File | `backend/platform`(일부), `backend/server` | policy | DTO 공유 |
| DW Query | `backend/dw-integration`, `backend/dw-gateway` | platform | ingestion/batch 논리 혼재 |

## 3. 목표 아키텍처
- 각 컨텍스트는 `api`(port) + `app` + `infra` 계층을 가진 독립 모듈/서비스.
- 서비스 간 통신은 gRPC/REST + Async Event (Kafka/SQS).
- 공통 타입은 `platform`의 DTO/Value Object 로 제한, 엔티티 공유 금지.
- 배포 단위: Auth & Policy (고가용), File (IO 집중), DW Query (batch friendly).

## 4. 단계별 계획
1. **Phase 1 (0-6M)**
   - `backend/server`에서 인증/정책/파일 API 호출을 기존 port 로 캡슐화 → 이미 완료된 port 기반 유지.
   - Modulith 분석 (`spring-modulith`)으로 패키지 의존 보고서 생성.
   - Cross-module DTO/엔티티 참조 제거.
2. **Phase 2 (6-12M)**
   - `backend/auth`와 `backend/policy` 를 독립 Spring Boot 앱으로 부팅 가능하도록 리팩터링 (별도 `Application` 클래스, config 분리).
   - File/DW Query 는 `dw-gateway`/`dw-worker` 재사용.
   - 내부 통신: Spring Cloud OpenFeign 또는 REST template with client library.
3. **Phase 3 (>12M)**
   - 배포 분리: Auth Service, Policy Service, File Service, DW Query Service (각각 컨테이너).
   - 공통 메시지 버스 (Kafka/EventBridge) 도입, Permission 변경/파일 정책 변경 이벤트 발행.
   - 데이터베이스 분리(스키마 혹은 인스턴스), Data Domain Ownership 정의.

## 5. 기술 고려
- 공통 의존성: `gradle/libs.versions.toml` 에 BOM 유지, 서비스별 `settings.gradle` 구성.
- Observability: OpenTelemetry collector를 통해 cross-service trace.
- 데이터 일관성: Saga/Outbox 패턴 유지.

## 6. 리스크 및 완화
- **팀 리소스 부족** → Modulith 기반 incremental 분리, Feature toggle.
- **배포 복잡도 증가** → GitOps 배포, 환경별 Helm chart 준비.
- **데이터 복제 문제** → CQRS read model로 공통 데이터 공유.

## 7. TODO
- [ ] Modulith 분석 리포트 생성(Sprint별).
- [ ] Auth/Policy용 독립 `Application` 부트스트랩 추가.
- [x] 내부 클라이언트 라이브러리 (port client) 정의 (`backend/dw-gateway-client`).
- [ ] Kafka/EventBridge 도입 PoC.
