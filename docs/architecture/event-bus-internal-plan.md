# 내부망 도메인 이벤트 버스 계획 (Kafka on-prem 기준)

## 목표
- 외부망 없이 권한/정책/배치 완료 이벤트를 Kafka 토픽으로 발행·구독해 캐시 무효화, 감사, 후속 워크플로를 트리거한다.

## 우선 PoC 범위
- 이벤트: `PermissionChanged`, `DwIngestionCompleted`, `FilePolicyUpdated`
- 토픽(예시): `auth.events`, `dw.ingestion.events`, `file.policy.events`
- 파티션: 초기 3 (throughput 낮을 경우 1), replication factor 2~3 (클러스터 크기 따라)
- 메시지 형식: JSON + 스키마 버전 필드 `"version":1`

## Publish/Subscribe 설계
- Producer: Spring Kafka 직접 사용 (의존성 이미 추가됨) → `KafkaTemplate`
- Consumer: 워커/서버에서 `@KafkaListener`로 구독, idempotency는 outbox ID/이벤트 ID 기반
- 실패 처리: 기본 재시도 + dead-letter 토픽(`*.dlt`)

## 최소 DTO 예시
```json
{
  "eventId": "uuid",
  "occurredAt": "2025-11-20T00:00:00Z",
  "type": "PermissionChanged",
  "payload": {
    "principalId": "user1",
    "groupCode": "AUDITOR",
    "source": "policy-gitops"
  },
  "version": 1
}
```

## 클러스터/보안
- 브로커: `kafka.infra.svc.cluster.local:9092`
- 인증: SASL/PLAIN 또는 mTLS (내부망이라도 운영 시 mTLS 권장)
- ACL: topic 기준 producer/consumer 권한 제한

## 운영 체크리스트
- 모니터링: lag(consumer), error rate(DLT), throughput(m/s)
- 알림: DLT 메시지 발생 시 Slack/Webhook
- 재처리: DLT/아카이브 토픽에서 재생 스크립트 제공

## 후속 작업
- 실제 Producer/Listener 구현 스켈레톤 추가
- 토픽 이름/파티션 수 확정
- CI 환경에서 Kafka 테스트컨테이너로 contract 테스트 추가
