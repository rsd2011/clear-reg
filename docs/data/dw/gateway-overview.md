# DW Gateway Separation

## 목적
- 메인 서버(`backend/server`)에서 DW ingestion 관련 REST 엔드포인트를 분리해 전용 Spring Boot 앱(`backend/dw-gateway`)으로 운영한다.
- 서버와 배치/워커는 `backend/dw-gateway-client` 포트를 통해 동일 API를 호출하며, `dw-worker`는 Outbox/큐를 통해 비동기 ingestion을 수행한다.

## 구성 요소
| 모듈 | 역할 |
| --- | --- |
| `backend/dw-gateway` | DW 배치, 정책, 파일 REST 컨트롤러 노출. `DwBatchPort`, `DwIngestionPolicyPort`, `FileManagementPort`를 어댑트한다. |
| `backend/dw-gateway-client` | REST → Port 변환 클라이언트 (`DwBatchPortClient`, `DwOrganizationPortClient`, `DwIngestionPolicyPortClient`, `FileManagementPortClient`). Spring Boot AutoConfiguration(`DwGatewayClientAutoConfiguration`)으로 서버 등에서 자동 주입. |
| `backend/dw-worker` | Outbox relay + ingestion 실행. 큐 메시지는 `dw-gateway`에 의해 생성되며 REST API로 policy 변경/파일 업로드를 담당한다. |
| `backend/server` | API 서버. `dw-gateway-client` 를 통해 DW, 파일, 정책 기능을 호출하며 DW ingestion REST 엔드포인트는 포함하지 않는다. |

## 배치 연계
1. 관리자/운영자는 `dw-gateway` REST API를 호출해 DW 업로드, 정책 변경을 수행한다.
2. `dw-gateway`는 Outbox(`DwIngestionOutbox`)에 작업을 enqueue 하고, `dw-worker`가 큐를 폴링하여 ingestion 을 실행한다.
3. 서버는 `dw-gateway-client` Bean 을 통해 DW 상태/정책/파일 목록 API를 소비하고, 더 이상 `dw-integration` 엔티티를 직접 노출하지 않는다.

## 배포 전략
- `dw-gateway` 는 독립 Boot 애플리케이션으로 빌드/배포한다 (`DwGatewayApplication`).
- 서버와 워커 모두 공통 `DwGatewayClientProperties` 로 Base URI / Service Token 을 구성해 동일한 REST 인터페이스와 재시도 정책을 공유한다.
- GitOps/CI는 `dw-gateway`와 `dw-worker`의 헬스체크를 독립적으로 관찰하며, 장애시 서버 API 영향 범위를 최소화한다.

## 참고 문서
- `docs/data/dw/worker.md`
- `docs/dw/transaction-boundaries.md`
